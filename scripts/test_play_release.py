"""Tests for play-release-notes.py and play-publish.py.

Run from the repo root: python3 -m unittest discover -s scripts -p 'test_*.py'
"""

import http.server
import importlib.util
import json
import os
import tempfile
import threading
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent


def load(name):
    spec = importlib.util.spec_from_file_location(name.replace("-", "_"), SCRIPTS / f"{name}.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


notes = load("play-release-notes")
TEMPLATE = (SCRIPTS.parent / ".github" / "pull_request_template.md").read_text()


class ExtractNote(unittest.TestCase):
    def test_sentence(self):
        body = "## Summary\nStuff.\n\n## Release note\nCheck-ins a minute apart are no longer dropped.\n\n## Testing\nran it"
        self.assertEqual(notes.extract_note(body), "Check-ins a minute apart are no longer dropped.")

    def test_none_variants(self):
        for value in ["none", "None.", "`none`", "N/A", "- none"]:
            with self.subTest(value=value):
                self.assertIsNone(notes.extract_note(f"## Release note\n{value}\n"))

    def test_untouched_template_is_missing(self):
        with self.assertRaises(notes.MissingSection):
            notes.extract_note(TEMPLATE)

    def test_filled_template(self):
        body = TEMPLATE.replace("## Release note\n", "## Release note\nFaster startup.\n", 1)
        self.assertEqual(notes.extract_note(body), "Faster startup.")

    def test_no_section(self):
        with self.assertRaises(notes.MissingSection):
            notes.extract_note("## Summary\nNo note here")
        with self.assertRaises(notes.MissingSection):
            notes.extract_note(None)

    def test_heading_variants_and_crlf(self):
        self.assertEqual(notes.extract_note("### Release notes\r\nFixes a crash.\r\n"), "Fixes a crash.")

    def test_wrapped_lines_join(self):
        self.assertEqual(notes.extract_note("## Release note\n- Fixes a crash\n  on rotation.\n"), "Fixes a crash on rotation.")

    def test_section_ends_at_next_heading(self):
        self.assertEqual(notes.extract_note("## Release note\nA.\n# Other\nB."), "A.")


class FormatAndNumbers(unittest.TestCase):
    def test_user_notes_only(self):
        self.assertEqual(notes.format_notes(["One.", "Two"], False, "v1.0.0"), "• One\n• Two")

    def test_one_maintenance_line_however_many_maintenance_prs(self):
        text = notes.format_notes(["Fixes a crash."], True, "v1.2.3")
        lines = text.split("\n")
        self.assertEqual(lines[0], "• Fixes a crash")
        self.assertEqual(len(lines), 2)
        self.assertIn(lines[1].removeprefix("• "), notes.MAINTENANCE)

    def test_maintenance_only_release(self):
        text = notes.format_notes([], True, "v1.1.8")
        self.assertEqual(text.count("\n"), 0)
        self.assertIn(text.removeprefix("• "), notes.MAINTENANCE)
        self.assertEqual(notes.format_notes([], False, "v1.1.8"), text)

    def test_pick_is_stable_per_tag_and_varies_across_tags(self):
        self.assertEqual(notes.maintenance_line("v1.1.8"), notes.maintenance_line("v1.1.8"))
        self.assertGreater(len({notes.maintenance_line(f"v1.{n}.0") for n in range(40)}), 3)

    def test_every_phrasing_fits_comfortably(self):
        self.assertTrue(all(len(line) <= 80 for line in notes.MAINTENANCE))

    def test_pr_numbers_from_merge_and_squash_subjects_oldest_first(self):
        subjects = [
            "Merge pull request #44 from justinharringa/renovate/github-actions",
            "Fix something (#42)",
            "A direct commit",
            "Merge pull request #29 from justinharringa/same-minute-duplicates",
        ]
        self.assertEqual(notes.pr_numbers(subjects), [29, 42, 44])


class Collect(unittest.TestCase):
    """Replays the v1.1.6..v1.1.7 PRs through collect() in a scratch repo."""

    def setUp(self):
        self.repo = tempfile.TemporaryDirectory()
        self.cwd = os.getcwd()
        os.chdir(self.repo.name)
        run = lambda *a: notes.subprocess.run(["git", *a], check=True, capture_output=True)  # noqa: E731
        run("init", "-q", "-b", "main")
        run("config", "user.email", "t@example.com")
        run("config", "user.name", "t")
        commit = lambda msg: run("commit", "-q", "--allow-empty", "-m", msg)  # noqa: E731
        commit("start")
        run("tag", "v1.1.6")
        for number, branch in [(26, "fix-release-notes"), (27, "modernize-gradle"), (29, "same-minute-duplicates"), (43, "renovate/test-dependencies")]:
            commit(f"Merge pull request #{number} from justinharringa/{branch}")
        run("tag", "v1.1.7")

    def tearDown(self):
        os.chdir(self.cwd)
        self.repo.cleanup()

    def test_only_user_facing_notes(self):
        prs = {
            26: ("fix-release-notes", "## Release note\nnone"),
            27: ("modernize-gradle", "Old PR body, written before the convention"),
            29: ("same-minute-duplicates", "## Release note\nCheck-ins a minute apart are no longer dropped as duplicates."),
            43: ("renovate/test-dependencies", "## Release note\nshould be skipped"),
        }
        fetch = lambda n: {"head": {"ref": prs[n][0]}, "body": prs[n][1]}  # noqa: E731
        text, report = notes.collect("v1.1.7", fetch)
        self.assertEqual(
            text,
            "• Check-ins a minute apart are no longer dropped as duplicates\n• " + notes.maintenance_line("v1.1.7"),
        )
        self.assertIn("#26: maintenance (none)", report)
        self.assertIn("#27: maintenance, no note (no `## Release note` section)", report)
        self.assertIn("#43: maintenance (renovate/test-dependencies)", report)

    def test_a_none_pr_alone_adds_the_maintenance_line(self):
        fetch = lambda n: {"head": {"ref": "b"}, "body": "## Release note\n" + ("none" if n == 26 else f"Change {n}.")}  # noqa: E731
        text, _ = notes.collect("v1.1.7", fetch)
        self.assertEqual(text, "• Change 27\n• Change 29\n• Change 43\n• " + notes.maintenance_line("v1.1.7"))

    def test_no_maintenance_line_when_every_pr_is_user_facing(self):
        fetch = lambda n: {"head": {"ref": "b"}, "body": f"## Release note\nChange {n}."}  # noqa: E731
        text, _ = notes.collect("v1.1.7", fetch)
        self.assertEqual(text, "• Change 26\n• Change 27\n• Change 29\n• Change 43")


class FakePlay(http.server.BaseHTTPRequestHandler):
    calls = []
    fail_on = None

    def log_message(self, *args):
        pass

    def respond(self):
        length = int(self.headers.get("Content-Length") or 0)
        body = self.rfile.read(length) if length else b""
        FakePlay.calls.append((self.command, self.path, body, self.headers.get("Authorization")))
        if FakePlay.fail_on and FakePlay.fail_on in self.path:
            self.send_response(403)
            payload = {"error": {"message": "The caller does not have permission"}}
        else:
            self.send_response(200)
            if self.path.endswith("/edits"):
                payload = {"id": "E1"}
            elif "/bundles" in self.path:
                payload = {"versionCode": 10108, "sha256": "abc"}
            else:
                payload = {}
        self.end_headers()
        self.wfile.write(json.dumps(payload).encode())

    do_POST = do_PUT = do_DELETE = respond


class Publish(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.server = http.server.HTTPServer(("127.0.0.1", 0), FakePlay)
        threading.Thread(target=cls.server.serve_forever, daemon=True).start()
        os.environ["PLAY_API_ROOT"] = f"http://127.0.0.1:{cls.server.server_port}"
        cls.publish = load("play-publish")

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()

    def setUp(self):
        FakePlay.calls, FakePlay.fail_on = [], None
        self.dir = tempfile.TemporaryDirectory()
        d = Path(self.dir.name)
        (d / "app.aab").write_bytes(b"AAB")
        (d / "mapping.txt").write_text("a -> b")
        (d / "notes.txt").write_text("Faster startup.\n")
        os.environ["PLAY_ACCESS_TOKEN"] = "tok"
        self.argv = [
            "--package", "com.harringa.mytime", "--aab", str(d / "app.aab"), "--mapping", str(d / "mapping.txt"),
            "--version-name", "1.1.8", "--notes-file", str(d / "notes.txt"),
        ]

    def tearDown(self):
        self.dir.cleanup()

    def test_draft_on_production(self):
        self.assertEqual(self.publish.main(self.argv), 0)
        base = "/androidpublisher/v3/applications/com.harringa.mytime"
        self.assertEqual(
            [(method, path) for method, path, _, _ in FakePlay.calls],
            [
                ("POST", f"{base}/edits"),
                ("POST", f"/upload{base}/edits/E1/bundles?uploadType=media"),
                ("POST", f"/upload{base}/edits/E1/apks/10108/deobfuscationFiles/proguard?uploadType=media"),
                ("PUT", f"{base}/edits/E1/tracks/production"),
                ("POST", f"{base}/edits/E1:commit"),
            ],
        )
        self.assertTrue(all(auth == "Bearer tok" for *_, auth in FakePlay.calls))
        self.assertEqual(FakePlay.calls[1][2], b"AAB")
        track = json.loads(FakePlay.calls[3][2])
        self.assertEqual(
            track,
            {"track": "production", "releases": [{
                "name": "1.1.8", "versionCodes": ["10108"], "status": "draft",
                "releaseNotes": [{"language": "en-US", "text": "Faster startup."}],
            }]},
        )

    def test_failure_deletes_the_edit_and_never_commits(self):
        FakePlay.fail_on = "/tracks/"
        self.assertEqual(self.publish.main(self.argv), 1)
        methods = [(method, path.rsplit("/", 1)[-1]) for method, path, _, _ in FakePlay.calls]
        self.assertEqual(methods[-1], ("DELETE", "E1"))
        self.assertNotIn("E1:commit", [p for _, p in methods])

    def test_missing_token(self):
        del os.environ["PLAY_ACCESS_TOKEN"]
        self.assertEqual(self.publish.main(self.argv), 1)
        self.assertEqual(FakePlay.calls, [])


if __name__ == "__main__":
    unittest.main()
