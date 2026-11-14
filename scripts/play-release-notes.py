#!/usr/bin/env python3
"""Builds the Google Play "What's new" text from the PRs in a release.

A PR is a user-facing note when it is labelled `enhancement` or `bug`: its
title becomes one bullet. An optional `## Release note` section in the PR body
(see .github/pull_request_template.md) replaces the title, for when the title
is worded for developers; writing one also counts as opting in, and `none`
there opts out even with a label. Every other PR (no label, Renovate) is
maintenance, and all of them together add ONE light-hearted bullet.

Labels, titles and bodies are read from the GitHub API at tag time, so any of
them can be fixed after merge, until the tag is pushed.

Usage:
  scripts/play-release-notes.py check
      Previews one PR, from $PR_TITLE, $PR_BODY, $PR_LABELS (a JSON list of
      names) and $PR_BRANCH. Fails only if its note is unusable. Used by
      release-note.yml, which writes the preview to the job summary.
  scripts/play-release-notes.py collect <tag>
      Prints the notes for <tag> (PRs merged since the previous v* tag).
      Reads PRs from the GitHub API with $GITHUB_TOKEN and $GITHUB_REPOSITORY.
      Used by release.yml.

Stdlib only, so it runs on a stock runner with no install step.
"""

import json
import os
import random
import re
import subprocess
import sys
import urllib.request

# Google Play rejects "What's new" text over 500 characters per language.
PLAY_LIMIT = 500

# A PR with one of these labels is a user-facing note. `bug` only for bugs a
# user can hit: a CI or build fix stays unlabelled.
USER_FACING_LABELS = {"enhancement", "bug"}

# Everything that is not user-facing is summed up in ONE of these lines, never
# one line per PR. The pick is seeded by the tag, so a re-run of release.yml
# produces the same text and the GitHub Release and Play agree.
MAINTENANCE = [
    "Behind-the-scenes updates",
    "Some housekeeping under the hood",
    "Oiled the gears and wound the clock",
    "Fresh batteries in the behind-the-scenes clockwork",
    "A little tidying up while nobody was watching the clock",
    "Routine maintenance to keep everything ticking",
    "Tightened a few bolts behind the scenes",
    "Swept the workshop floor behind the scenes",
]

# Branches whose PRs are always maintenance, whatever their labels.
MAINTENANCE_BRANCH_PREFIXES = ("renovate/",)

NONE_VALUES = {"none", "n/a", "na", "no", "-"}

HEADING = re.compile(r"^#{1,6}\s+(.*?)\s*#*\s*$")
SECTION_TITLE = re.compile(r"release\s+notes?", re.IGNORECASE)
COMMENT = re.compile(r"<!--.*?-->", re.DOTALL)
CONVENTIONAL_PREFIX = re.compile(r"^[a-z]+(\([^)]*\))?!?:\s*")
MERGE_SUBJECT = re.compile(r"^Merge pull request #(\d+) ")
SQUASH_SUBJECT = re.compile(r"\(#(\d+)\)\s*$")

# Sentinel: the section says `none`.
NONE = object()


def section_note(body):
    """The `## Release note` section's text, NONE for `none`, or None when absent or empty.

    The template's guidance is in HTML comments, so an untouched template reads
    as empty, i.e. as no override.
    """
    lines = COMMENT.sub("", (body or "").replace("\r\n", "\n")).split("\n")
    collected = None
    for line in lines:
        heading = HEADING.match(line)
        if heading:
            if collected is not None:
                break
            if SECTION_TITLE.fullmatch(heading.group(1).strip()):
                collected = []
            continue
        if collected is not None:
            collected.append(line)
    if collected is None:
        return None

    text = " ".join(line.strip().removeprefix("- ").removeprefix("* ").strip() for line in collected)
    text = re.sub(r"\s+", " ", text).strip()
    if not text:
        return None
    if text.rstrip(".").strip().strip("`").lower() in NONE_VALUES:
        return NONE
    return text


def from_title(title):
    """A title as a bullet: no `type(scope):` prefix, first letter capitalised."""
    text = CONVENTIONAL_PREFIX.sub("", (title or "").strip()).strip()
    return text[:1].upper() + text[1:]


def classify(pr):
    """Returns (note, reason): note is the bullet text, or None for maintenance."""
    branch = pr["head"]["ref"]
    if branch.startswith(MAINTENANCE_BRANCH_PREFIXES):
        return None, f"maintenance ({branch})"
    labels = {label["name"] for label in pr.get("labels", [])}
    section = section_note(pr.get("body"))
    if section is NONE:
        return None, "maintenance (release note says none)"
    if section is not None:
        return section.rstrip("."), "note (from the Release note section)"
    matched = sorted(labels & USER_FACING_LABELS)
    if matched:
        return from_title(pr.get("title")).rstrip("."), f"note (title, labelled {', '.join(matched)})"
    return None, "maintenance (no enhancement/bug label)"


def maintenance_line(tag):
    return random.Random(tag).choice(MAINTENANCE)


def format_notes(notes, has_maintenance, tag):
    """A bullet per user-facing note, plus at most one maintenance bullet."""
    lines = list(notes)
    if has_maintenance or not lines:
        lines.append(maintenance_line(tag))
    return "\n".join(f"• {line}" for line in lines)


def pr_numbers(subjects):
    """PR numbers from first-parent commit subjects, oldest first."""
    numbers = []
    for subject in subjects:
        match = MERGE_SUBJECT.match(subject) or SQUASH_SUBJECT.search(subject)
        if match and int(match.group(1)) not in numbers:
            numbers.append(int(match.group(1)))
    return list(reversed(numbers))


def git(*args):
    return subprocess.run(["git", *args], check=True, capture_output=True, text=True).stdout.strip()


def previous_tag(tag):
    result = subprocess.run(
        ["git", "describe", "--tags", "--abbrev=0", "--match", "v*", f"{tag}^"],
        capture_output=True,
        text=True,
    )
    return result.stdout.strip() if result.returncode == 0 else ""


def fetch_pr(repo, number, token):
    request = urllib.request.Request(
        f"https://api.github.com/repos/{repo}/pulls/{number}",
        headers={
            "Accept": "application/vnd.github+json",
            "Authorization": f"Bearer {token}",
            "X-GitHub-Api-Version": "2022-11-28",
        },
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.load(response)


def collect(tag, fetch):
    """Returns (text, report lines) for the PRs merged between the previous tag and `tag`."""
    previous = previous_tag(tag)
    revisions = f"{previous}..{tag}" if previous else tag
    subjects = git("log", "--first-parent", "--format=%s", revisions).splitlines()

    notes, report, maintenance = [], [], False
    for number in pr_numbers(subjects):
        note, reason = classify(fetch(number))
        if note:
            notes.append(note)
            report.append(f"#{number}: {reason}: {note}")
        else:
            maintenance = True
            report.append(f"#{number}: {reason}")

    report.insert(0, f"Release {tag}, PRs since {previous or 'the first commit'}:")
    return format_notes(notes, maintenance, tag), report


def check(environ):
    """Previews one PR. Returns (exit status, Markdown preview)."""
    pr = {
        "head": {"ref": environ.get("PR_BRANCH", "")},
        "title": environ.get("PR_TITLE", ""),
        "body": environ.get("PR_BODY", ""),
        "labels": [{"name": name} for name in json.loads(environ.get("PR_LABELS") or "[]")],
    }
    note, reason = classify(pr)
    if note is None:
        return 0, (
            f"**Google Play:** this PR counts toward the release's single behind-the-scenes bullet: {reason}.\n\n"
            "If users will notice it, add the `enhancement` or `bug` label (its title becomes the bullet), "
            "or write the bullet in a `## Release note` section."
        )
    preview = f"**Google Play:** this PR adds the bullet\n\n> • {note}\n\n_{reason}_"
    if not note:
        return 1, preview + "\n\n**The note is empty.** Give the PR a title, or fill in `## Release note`."
    if len(note) > PLAY_LIMIT:
        return 1, preview + f"\n\n**The note is {len(note)} characters; Google Play allows {PLAY_LIMIT} for the whole release.**"
    return 0, preview


def main(argv):
    if argv[:1] == ["check"]:
        status, preview = check(os.environ)
        print(preview)
        summary = os.environ.get("GITHUB_STEP_SUMMARY")
        if summary:
            with open(summary, "a", encoding="utf-8") as out:
                out.write(preview + "\n")
        if status:
            print("::error::This PR's Google Play note is unusable; see the job summary.")
        return status

    if len(argv) == 2 and argv[0] == "collect":
        repo, token = os.environ["GITHUB_REPOSITORY"], os.environ["GITHUB_TOKEN"]
        text, report = collect(argv[1], lambda number: fetch_pr(repo, number, token))
        print("\n".join(report), file=sys.stderr)
        if len(text) > PLAY_LIMIT:
            print(
                f"::error::Combined release notes are {len(text)} characters; Google Play allows "
                f"{PLAY_LIMIT}. Shorten the titles or `## Release note` sections of the PRs above.",
                file=sys.stderr,
            )
            return 1
        print(text)
        return 0

    print(__doc__, file=sys.stderr)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
