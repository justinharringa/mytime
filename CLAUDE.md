# mytime — notes for agents

An Android app (Java, Gradle modules `:mytime` and `:libraries:timecalc`).
README.md covers the build; VERSIONING.md covers version numbers.

## Every PR needs a `## Release note`

Fill in the `## Release note` section of `.github/pull_request_template.md`.
**It is the app's Google Play "What's new" text, published unedited.** When a
`v*` tag is pushed, `release.yml` collects the notes of the PRs merged since the
previous tag (`scripts/play-release-notes.py`) and uploads them with the bundle.
The `release-note` check fails a PR whose body has no such section.

- Write **one line for a person using the app**: what they will notice.
  "Check-ins one minute apart are no longer skipped as duplicates", not "Treat
  only a check-in in the same minute as a duplicate". It becomes one `•`
  bullet; a trailing period is dropped.
- Write **`none`** when a user would not notice: build, CI, tests, docs,
  refactors, dependency bumps, this file. Most PRs here are `none`, and that is
  the right answer, not a gap: every `none` and Renovate PR in a release is
  summed up in **one** light-hearted "behind-the-scenes" bullet, picked from
  `MAINTENANCE` in `scripts/play-release-notes.py`. Never write a note that
  only says "maintenance" or "updated dependencies"; that is what `none` is for.
- No PR numbers, class names, SDK levels or jargon. Play shows plain text, so
  no Markdown either.
- Keep it short: all the notes of a release share Play's 500-character limit.
- If you change the PR's behaviour after opening it, update the note too.

Renovate PRs (`renovate/*` branches) are exempt from the check; they count
toward the release's single behind-the-scenes bullet.

## Releasing is the owner's

Never push a tag or dispatch `release.yml`. A tag push builds the signed bundle
and uploads it to the Play production track **as a draft**; the owner sends it
for review in the Play Console.
