#!/usr/bin/env python3
"""Builds the Google Play "What's new" text from the PRs in a release.

Every PR body carries a `## Release note` section (see
.github/pull_request_template.md): one user-facing sentence, or `none`. At tag
time this collects those sentences from the PRs merged since the previous tag,
so the Play notes are written once, by whoever wrote the change, and nobody
edits them at release time.

Usage:
  scripts/play-release-notes.py check
      Validates one PR body, read from $PR_BODY. Used by release-note.yml.
  scripts/play-release-notes.py collect <tag>
      Prints the notes for <tag> (PRs merged since the previous v* tag).
      Reads PR bodies from the GitHub API with $GITHUB_TOKEN and
      $GITHUB_REPOSITORY. Used by release.yml.

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

# Everything that is not user-facing (Renovate bumps, `none` PRs, PRs from
# before the convention) is summed up in ONE of these lines, never one line per
# PR. The pick is seeded by the tag, so a re-run of release.yml produces the
# same text and the GitHub Release and Play agree.
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

# Branches whose PRs never carry a note. Renovate's are dependency bumps and
# its PR bodies do not use the template.
EXEMPT_BRANCH_PREFIXES = ("renovate/",)

NONE_VALUES = {"none", "n/a", "na", "no", "-"}

HEADING = re.compile(r"^#{1,6}\s+(.*?)\s*#*\s*$")
SECTION_TITLE = re.compile(r"release\s+notes?", re.IGNORECASE)
COMMENT = re.compile(r"<!--.*?-->", re.DOTALL)
MERGE_SUBJECT = re.compile(r"^Merge pull request #(\d+) ")
SQUASH_SUBJECT = re.compile(r"\(#(\d+)\)\s*$")


class MissingSection(Exception):
    pass


def extract_note(body):
    """Returns the PR's note, or None when it says `none`.

    Raises MissingSection when the body has no `## Release note` section, or
    the section is empty (the template's instructions are HTML comments, so an
    untouched template counts as empty).
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
        raise MissingSection("no `## Release note` section")

    text = " ".join(line.strip().removeprefix("- ").removeprefix("* ").strip() for line in collected)
    text = re.sub(r"\s+", " ", text).strip()
    if not text:
        raise MissingSection("the `## Release note` section is empty")
    if text.rstrip(".").strip().strip("`").lower() in NONE_VALUES:
        return None
    return text


def maintenance_line(tag):
    return random.Random(tag).choice(MAINTENANCE)


def format_notes(notes, has_maintenance, tag):
    """A bullet per user-facing note, plus at most one maintenance bullet."""
    lines = [note.rstrip(".") for note in notes]
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
        pr = fetch(number)
        branch = pr["head"]["ref"]
        if branch.startswith(EXEMPT_BRANCH_PREFIXES):
            report.append(f"#{number}: maintenance ({branch})")
            maintenance = True
            continue
        try:
            note = extract_note(pr.get("body"))
        except MissingSection as error:
            # A PR merged before the convention, or with the check ignored. Do
            # not block the release on it: say so, and leave it out.
            report.append(f"#{number}: maintenance, no note ({error})")
            maintenance = True
            continue
        if note is None:
            report.append(f"#{number}: maintenance (none)")
            maintenance = True
            continue
        report.append(f"#{number}: {note}")
        notes.append(note)

    report.insert(0, f"Release {tag}, PRs since {previous or 'the first commit'}:")
    return format_notes(notes, maintenance, tag), report


def main(argv):
    if argv[:1] == ["check"]:
        try:
            note = extract_note(os.environ.get("PR_BODY", ""))
        except MissingSection as error:
            print(
                f"::error::PR body: {error}. Add a `## Release note` section with one "
                "user-facing sentence for the Google Play \"What's new\" text, or `none`."
            )
            return 1
        if note is not None and len(note) > PLAY_LIMIT:
            print(f"::error::Release note is {len(note)} characters; Google Play allows {PLAY_LIMIT} for the whole release.")
            return 1
        print(f"Release note: {note}" if note else "Release note: none (not user-facing)")
        return 0

    if len(argv) == 2 and argv[0] == "collect":
        repo, token = os.environ["GITHUB_REPOSITORY"], os.environ["GITHUB_TOKEN"]
        text, report = collect(argv[1], lambda number: fetch_pr(repo, number, token))
        print("\n".join(report), file=sys.stderr)
        if len(text) > PLAY_LIMIT:
            print(
                f"::error::Combined release notes are {len(text)} characters; Google Play allows "
                f"{PLAY_LIMIT}. Shorten the `## Release note` sections of the PRs above.",
                file=sys.stderr,
            )
            return 1
        print(text)
        return 0

    print(__doc__, file=sys.stderr)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
