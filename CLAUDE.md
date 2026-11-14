# mytime — notes for agents

An Android app (Java, Gradle modules `:mytime` and `:libraries:timecalc`).
README.md covers the build; VERSIONING.md covers version numbers.

## Labels decide the Google Play release notes

When a `v*` tag is pushed, `release.yml` builds the Play "What's new" text from
the PRs merged since the previous tag (`scripts/play-release-notes.py`) and
uploads it, **unedited**, with the bundle. For each PR you open:

- **Users will notice it** (a new feature, a visible fix): add the
  **`enhancement`** or **`bug`** label. Its title becomes one `•` bullet, so
  word the title for a person using the app. If it already reads like a code
  description ("Treat only a check-in in the same minute as a duplicate"),
  keep it and put the user wording in the body's `## Release note` section
  instead ("Check-ins one minute apart are no longer skipped as duplicates").
  That section replaces the title in the notes.
- **Users won't notice it** (build, CI, tests, docs, refactors, dependency
  bumps, this file): **no label, no section**. That is most PRs here, and the
  right answer: all of them together become one light-hearted
  "behind-the-scenes" bullet, picked from `MAINTENANCE` in the script.
- `bug` is for bugs a user can hit. A CI, build or release-notes fix gets no
  label.
- No PR numbers, class names, SDK levels or jargon in a note. Play shows plain
  text, so no Markdown either; a trailing period is dropped. All notes of a
  release share Play's 500-character limit.
- If the change's behaviour moves after you open the PR, update the label,
  title or section to match. They are read at tag time, so they can still be
  fixed after merge, until the tag is pushed.

The `release-note` check's job summary shows how the PR will appear. It only
fails when the note is empty or too long, so a green check does not mean the
label is right. Renovate PRs (`renovate/*`) are always behind-the-scenes.

## Releasing is the owner's

Never push a tag or dispatch `release.yml`. A tag push builds the signed bundle
and uploads it to the Play production track **as a draft**; the owner sends it
for review in the Play Console.
