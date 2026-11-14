"""Tests for play-release-notes.py and play-publish.py.

Run from the repo root: python3 -m unittest discover -s scripts -p 'test_*.py'
"""

import contextlib
import http.server
import importlib.util
import io
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


def pr(title="A change", labels=(), body="", branch="feature"):
    return {"head": {"ref": branch}, "title": title, "body": body, "labels": [{"name": n} for n in labels]}


class SectionNote(unittest.TestCase):
    def test_sentence(self):
        body = "## Summary\nStuff.\n\n## Release note\nCheck-ins a minute apart are no longer dropped.\n\n## Testing\nran it"
        self.assertEqual(notes.section_note(body), "Check-ins a minute apart are no longer dropped.")

    def test_none_variants(self):
        for value in ["none", "None.", "`none`", "N/A", "- none"]:
            with self.subTest(value=value):
                self.assertIs(notes.section_note(f"## Release note\n{value}\n"), notes.NONE)

    def test_absent_or_untouched_template_is_no_override(self):
        for body in [TEMPLATE, "## Summary\nNo note here", None, "## Release note\n\n## Testing\n"]:
            with self.subTest(body=body):
                self.assertIsNone(notes.section_note(body))

    def test_filled_template(self):
        body = TEMPLATE.replace("## Release note\n", "## Release note\nFaster startup.\n", 1)
        self.assertEqual(notes.section_note(body), "Faster startup.")

    def test_heading_variants_crlf_and_wrapping(self):
        self.assertEqual(notes.section_note("### Release notes\r\n- Fixes a crash\r\n  on rotation.\r\n"), "Fixes a crash on rotation.")

    def test_section_ends_at_next_heading(self):
        self.assertEqual(notes.section_note("## Release note\nA.\n# Other\nB."), "A.")


class Classify(unittest.TestCase):
    def test_label_makes_the_title_a_note(self):
        for label in ["enhancement", "bug"]:
            with self.subTest(label=label):
                self.assertEqual(notes.classify(pr("Clearer message when a check-in is skipped.", [label]))[0],
                                 "Clearer message when a check-in is skipped")

    def test_conventional_prefix_is_stripped_from_a_title(self):
        self.assertEqual(notes.classify(pr("fix(ui)!: check-ins no longer vanish", ["bug"]))[0], "Check-ins no longer vanish")

    def test_no_label_is_maintenance(self):
        self.assertEqual(notes.classify(pr("Fix generated release notes", ["tests"])),
                         (None, "maintenance (no enhancement/bug label)"))

    def test_section_overrides_the_title_and_opts_in_without_a_label(self):
        body = "## Release note\nCheck-ins one minute apart are no longer skipped as duplicates."
        expected = "Check-ins one minute apart are no longer skipped as duplicates"
        self.assertEqual(notes.classify(pr("Treat only a check-in in the same minute as a duplicate", ["bug"], body))[0], expected)
        self.assertEqual(notes.classify(pr("Treat only a check-in in the same minute as a duplicate", [], body))[0], expected)

    def test_none_opts_out_even_with_a_label(self):
        self.assertIsNone(notes.classify(pr("Speed up the build", ["enhancement"], "## Release note\nnone"))[0])

    def test_renovate_is_maintenance_whatever_it_says(self):
        self.assertIsNone(notes.classify(pr("chore: update x", ["bug"], "## Release note\nhi", "renovate/x"))[0])


class Check(unittest.TestCase):
    def env(self, **kw):
        base = {"PR_TITLE": "A change", "PR_BODY": "", "PR_LABELS": "[]", "PR_BRANCH": "feature"}
        base.update(kw)
        return base

    def test_unlabelled_passes_and_says_behind_the_scenes(self):
        status, preview = notes.check(self.env())
        self.assertEqual(status, 0)
        self.assertIn("behind-the-scenes", preview)

    def test_labelled_previews_the_bullet(self):
        status, preview = notes.check(self.env(PR_TITLE="Faster startup", PR_LABELS='["enhancement"]'))
        self.assertEqual(status, 0)
        self.assertIn("> • Faster startup", preview)

    def test_too_long_fails(self):
        status, _ = notes.check(self.env(PR_TITLE="x" * 501, PR_LABELS='["bug"]'))
        self.assertEqual(status, 1)

    def test_empty_title_note_fails(self):
        status, _ = notes.check(self.env(PR_TITLE="fix: ", PR_LABELS='["bug"]'))
        self.assertEqual(status, 1)


class FormatAndNumbers(unittest.TestCase):
    def test_user_notes_only(self):
        self.assertEqual(notes.format_notes(["One", "Two"], False, "v1.0.0"), "• One\n• Two")

    def test_one_maintenance_line_however_many_maintenance_prs(self):
        lines = notes.format_notes(["Fixes a crash"], True, "v1.2.3").split("\n")
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

    PRS = [(26, "fix-release-notes"), (27, "modernize-gradle"), (29, "same-minute-duplicates"), (43, "renovate/test-dependencies")]

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
        for number, branch in self.PRS:
            commit(f"Merge pull request #{number} from justinharringa/{branch}")
        run("tag", "v1.1.7")

    def tearDown(self):
        os.chdir(self.cwd)
        self.repo.cleanup()

    def test_v1_1_7_with_labels(self):
        prs = {
            26: pr("Fix generated release notes", [], "", "fix-release-notes"),
            27: pr("Upgrade to Gradle 9 / AGP 9", ["gradle"], "", "modernize-gradle"),
            29: pr("Treat only a check-in in the same minute as a duplicate", ["bug"],
                   "## Release note\nCheck-ins one minute apart are no longer skipped as duplicates", "same-minute-duplicates"),
            43: pr("chore: update Test dependencies to v4.18", ["renovate"], "", "renovate/test-dependencies"),
        }
        text, report = notes.collect("v1.1.7", prs.get)
        self.assertEqual(
            text,
            "• Check-ins one minute apart are no longer skipped as duplicates\n• " + notes.maintenance_line("v1.1.7"),
        )
        self.assertIn("#26: maintenance (no enhancement/bug label)", report)
        self.assertIn("#43: maintenance (renovate/test-dependencies)", report)

    def test_a_none_pr_alone_adds_the_maintenance_line(self):
        fetch = lambda n: pr(f"Change {n}", [] if n == 26 else ["enhancement"])  # noqa: E731
        text, _ = notes.collect("v1.1.7", fetch)
        self.assertEqual(text, "• Change 27\n• Change 29\n• Change 43\n• " + notes.maintenance_line("v1.1.7"))

    def test_no_maintenance_line_when_every_pr_is_user_facing(self):
        text, _ = notes.collect("v1.1.7", lambda n: pr(f"Change {n}", ["enhancement"]))
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

    def test_missing_mapping_is_an_annotation_and_deletes_the_edit(self):
        argv = list(self.argv)
        argv[argv.index("--mapping") + 1] += ".gone"
        stderr = io.StringIO()
        with contextlib.redirect_stderr(stderr):
            self.assertEqual(self.publish.main(argv), 1)
        self.assertIn("::error::Google Play upload failed: FileNotFoundError", stderr.getvalue())
        methods = [(method, path.rsplit("/", 1)[-1]) for method, path, _, _ in FakePlay.calls]
        self.assertEqual(methods[-1], ("DELETE", "E1"))
        self.assertNotIn("E1:commit", [p for _, p in methods])

    def test_missing_notes_file_is_an_annotation_before_any_call(self):
        argv = list(self.argv)
        argv[argv.index("--notes-file") + 1] += ".gone"
        stderr = io.StringIO()
        with contextlib.redirect_stderr(stderr):
            self.assertEqual(self.publish.main(argv), 1)
        self.assertIn("::error::Google Play upload failed: FileNotFoundError", stderr.getvalue())
        self.assertEqual(FakePlay.calls, [])

    def test_missing_token(self):
        del os.environ["PLAY_ACCESS_TOKEN"]
        self.assertEqual(self.publish.main(self.argv), 1)
        self.assertEqual(FakePlay.calls, [])


if __name__ == "__main__":
    unittest.main()
