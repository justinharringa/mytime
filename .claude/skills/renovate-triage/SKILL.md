---
name: renovate-triage
description: Triage mytime's open Renovate PRs from evidence, not titles. Classify each PR by posture, read the upstream notes (AGP and androidx publish theirs on developer.android.com, not GitHub), require the `build` check green on the current head (nothing on GitHub enforces it: `main` has no ruleset), merge what passes the LOW RISK rubric one PR at a time, and verify each merge commit's run on `main`. Merging deploys nothing; a release is a tag push (which uploads a draft to Play) plus sending that draft for review, both the owner's. Works unattended from a cloud session (GitHub connector or `gh api`). Use when asked to triage, review or clear mytime's Renovate PRs or its Dependency Dashboard, or when the Daily Renovate Triage routine reaches this repo.
---

# Renovate Triage: mytime

An Android app (Java, two Gradle modules: `:mytime` and `:libraries:timecalc`)
built with the Android Gradle Plugin (AGP). `renovate.json` explains every
grouping rule in its `description` fields; read it once before the first
triage. It mirrors untappd-backup's skill, because the shape is the same: a
merge changes `main` and nothing else.

## What a merge does, and what a release does

```
PR merged to main ──(nothing ships)──▶ owner pushes tag vX.Y.Z
   ──▶ release.yml builds the signed, R8-minified App Bundle ──▶ GitHub Release
   ──▶ release.yml uploads it to Play production as a DRAFT
   ──▶ owner sends the draft for review in the Play Console ──▶ users
```

- **Merging is not a deploy.** No user sees a merged bump until the owner tags
  and sends the Play draft for review. Keep `main` releasable all the same: a bad merge waits there for
  whoever cuts the next release.
- **Never push a tag or dispatch a workflow from triage.** Releasing is the
  owner's decision, and `release.yml` signs with the upload key.

## What gates a PR, and what does not

| Check (workflow) | Runs on | Covers |
|---|---|---|
| `build` (Android Build & Test) | every PR and every push to `main` | `./gradlew build -x assembleRelease`, unit tests (Robolectric for `:mytime`, plain JUnit for `timecalc`), `scripts/check-version-info.sh`, JaCoCo reports, `assembleDebug`, `lint`. About 2 minutes |
| `renovate.json (config validator)` (Renovate Config Validation) | PRs and pushes that touch `renovate.json` | `renovate-config-validator --strict` |
| `release-note` (Release Note) | PRs, **skipped on `renovate/*` branches** | Previews the PR's Google Play note. Skipped on every Renovate PR by design (Renovate PRs are always behind-the-scenes); a skip is not a failure |

**Nothing enforces these.** `main` has no ruleset and no classic protection,
and `allow_auto_merge` is off. GitHub will merge a red PR if asked, so
**triage checks `build` itself.**

**Not covered by any pre-merge check:**
- **The release build.** `-x assembleRelease` skips it, so R8 minification and
  resource shrinking (`minifyEnabled`, `shrinkResources` in
  `mytime/build.gradle`) first run on the next tag. R8 ships inside AGP, so
  **every AGP bump changes the shrinker without a pre-merge test**, and a Guava
  bump can change what R8 keeps. A failure surfaces at the next release, in
  front of the owner, or at runtime in a minified build.
- **Signing.** Only `release.yml` decodes the upload keystore.
- **`softprops/action-gh-release` and `google-github-actions/auth`.** Both
  appear only in `release.yml`, which has no `pull_request` trigger. `auth`
  mints the Play upload token, so a broken bump surfaces as a failed Play
  upload at the next tag.
- **The Play upload** (`scripts/play-publish.py`). Its unit tests run in
  `build` against a fake API; the real API is first called at the next tag.

## Phase 0: Inventory

```bash
R=justinharringa/mytime
gh api "repos/$R/pulls?state=open&per_page=50" \
  --jq '.[] | "#\(.number) \(.user.login) [\([.labels[].name]|join(","))] \(.head.sha[0:7]) \(.title)"'
```

The Dependency Dashboard is #39 (`issue_read`, or
`gh api repos/$R/issues/39 --jq .body`).

**Do not filter on the `renovate` label.** Security PRs from
`vulnerabilityAlerts` carry `security` instead. A PR from `dependabot[bot]`
means Dependabot security updates are switched on in the repo settings; report
it to the owner rather than triaging around it, because the two bots duplicate
each other's security PRs (untappd-backup #163 vs #165).

There is no `minimumReleaseAge` here, so nothing waits under *Pending Status
Checks*. If something does, the config changed: read `renovate.json`.

## Phase 1: Classify

The groups come from `renovate.json`. `separateMajorMinor` puts every major on
its own PR, labelled `major-update`.

| Bucket | Recognise by | Triage does |
|---|---|---|
| **Ask first** | any `major-update` PR (all action majors arrive as one grouped `GitHub Actions (major)` PR); the **JDK (setup-java)** group at any version; **Android Gradle Plugin minor** (9.4 → 9.5); any PR that changes the **version** of `softprops/action-gh-release` or `google-github-actions/auth` (release-only, never exercised by a PR); a Renovate config-migration PR (`renovate.json` edits); any PR touching a file other than the ones listed in Phase 3 | Gather the evidence (Phase 2), then notify. Do not merge |
| **Candidate LOW RISK** | **Gradle wrapper** minor or patch; **Android Gradle Plugin** patch; **Guava** minor or patch (always `-android`); **Test dependencies** minor or patch (JUnit, Robolectric, androidx.test); **GitHub Actions** `Pin dependencies` and digest refreshes (the version comment stays the same, so behaviour does not change, including for the release-only action); `security` PRs | Apply the rubric |

Why AGP minors are Ask first while patches are candidates: a minor can raise
the minimum Gradle, deprecate DSL the modules use, and ships a new R8 that no
PR check exercises. A patch keeps all three.

The first wave, opened 2026-11-09 within minutes of `renovate.json` landing,
classifies as:

| PR | Bucket | Why |
|---|---|---|
| #35 Pin dependencies | Candidate | digest pins at the current `@v4`/`@v1`, both workflows |
| #37 Guava 33.8.0 | Candidate | minor, `-android` in both modules |
| #38 Gradle wrapper 9.9.0 | Candidate | minor |
| #36 AGP 9.5.1 | Ask first | AGP minor |
| #34 JDK 25 | Ask first | JDK group |
| #33 GitHub Actions (major) | Ask first | checkout v7, setup-java v6, cache v6, upload-artifact v7, softprops v3 |

The actions are pinned to floating majors (`@v4`), so after #35 their stream is
digest refreshes plus majors; minor and patch never arrive for them. #33 and #35
edit the same `uses:` lines, so whichever merges second gets rebased by
Renovate (`rebaseWhen: auto` rebases a conflicted PR).

## Phase 2: Read the notes

1. The PR body.
2. Where each dependency publishes notes. Renovate attaches none for AGP or
   androidx, because their source URL is googlesource:

   | Dependency | Notes |
   |---|---|
   | Gradle wrapper | `https://docs.gradle.org/<version>/release-notes.html` (also `gradle/gradle` GitHub releases, tag `v<version>`) |
   | Android Gradle Plugin | `https://developer.android.com/build/releases/gradle-plugin`; older releases under `https://developer.android.com/build/releases/past-releases` |
   | AGP ↔ Gradle compatibility | `https://developer.android.com/build/releases/gradle-plugin#updating-gradle` |
   | Guava | `google/guava` GitHub releases, tag `v<version>` (no `-android` suffix) |
   | Robolectric | `robolectric/robolectric` GitHub releases, tag `robolectric-<version>` |
   | androidx.test | `https://developer.android.com/jetpack/androidx/releases/test` |
   | JUnit 4 | `junit-team/junit4` (dormant; a 4.x release would be news in itself) |
   | GitHub Actions | the action's GitHub releases |

3. For GitHub-hosted notes, stop at the first route that answers:
   anonymous `curl -s https://api.github.com/repos/<owner>/<repo>/releases/tags/<tag>`
   (a 403 is the per-IP rate limit, so move on); WebFetch of
   `https://github.com/<owner>/<repo>/releases/tag/<tag>`; `gh api` for the same
   path. In a routine the GitHub connector refuses repos outside the session,
   so it cannot read upstream notes. **"Notes not read" is not an outcome:** if
   every route fails, report "notes unreachable" with each route tried.

4. What to look for, per dependency:
   - **Gradle**: removed or deprecated APIs the build scripts use
     (`providers.exec`, `pluginManager.withPlugin`, `JacocoTaskExtension`), and
     whether the AGP pinned in the root `build.gradle` supports the new Gradle.
   - **AGP**: the minimum Gradle (compare with
     `gradle/wrapper/gradle-wrapper.properties`), the minimum JDK (CI and
     `build.gradle` require 21), R8 or shrinker changes, and DSL removals.
   - **Robolectric**: the supported SDK levels. `compileSdk` in
     `mytime/build.gradle` must be in the set, or every Robolectric test fails
     to boot the framework (the `build` check catches this, so read it to
     explain a red run, not to predict one).
   - **Guava**: removals in the classes the app uses
     (`grep -rn 'com.google.common' mytime/src libraries/timecalc/src`).

## Phase 3: LOW RISK rubric

Merge only if **every** line holds. Otherwise hold or ask, and name the line
that failed.

- [ ] Bucket is *Candidate LOW RISK*
- [ ] Notes read (Phase 2), with nothing breaking that this app uses
- [ ] `build` is **SUCCESS on the current head SHA**. Pending is not success,
      and nothing on GitHub will stop a merge on red
- [ ] For a Gradle wrapper or AGP PR: the notes show the pinned AGP supports
      the new Gradle (or the new AGP supports the pinned Gradle)
- [ ] Files touched are only `gradle/wrapper/gradle-wrapper.properties`
      (and `gradle-wrapper.jar`, `gradlew`, `gradlew.bat` on a wrapper bump),
      the root `build.gradle` plugin line, the `dependencies` blocks of
      `mytime/build.gradle` and `libraries/timecalc/build.gradle`, or `uses:`
      lines in either workflow. A `uses:` change in `release.yml` qualifies
      only when its `# vX` comment is unchanged (a pin or digest refresh); a
      version change there is Ask first. Every change matches the title
- [ ] One commit, authored by Renovate; no hand edits on the branch
- [ ] `main`'s latest `Android Build & Test` push run is green, so a merge
      cannot hide an existing breakage

A wrapper PR from the hosted Renovate edits only
`gradle-wrapper.properties` (#38: one line, `distributionUrl`). It does not
regenerate `gradlew`, `gradlew.bat` or the wrapper jar. That is acceptable: the
jar only downloads and launches the distribution the properties file names, and
`build` ran on the new distribution. If a wrapper PR **does** change the jar,
Ask first: a binary changed by a bot needs a human to confirm it came from
`gradle wrapper`.

**A red `build` is not always the bump's fault.** Read the failing step's log
(`get_job_logs`) before blaming the PR. `Set up Android SDK` downloads from
Google, and `Run lint checks` can pick up new lint rules from a newer AGP only.
A step the PR cannot affect is not the PR's fault, but the PR is still not
mergeable: hold it and say why.

Merge, **one PR at a time**, matching the repo's merge-commit history:

```bash
gh api -X PUT repos/$R/pulls/<n>/merge -f merge_method=merge -f sha=<head-you-reviewed>
```

In a routine, the connector's `merge_pull_request` with `merge_method: "merge"`
and `expectedHeadSha: <head you reviewed>`. The SHA pin refuses the merge if
Renovate pushed after you reviewed. Then run Phase 4 before the next merge:
Guava and the test dependencies both edit `mytime/build.gradle`, so a second
merge on top of an unverified first one loses attribution and can conflict.

## Phase 4: Verify a merge (about 3 minutes later)

A merge to `main` runs `Android Build & Test` on the merge commit. It must
finish green:

```bash
gh api "repos/$R/actions/runs?branch=main&event=push&per_page=4" \
  --jq '.workflow_runs[] | "\(.id) \(.name) \(.head_sha[0:7]) \(.status)/\(.conclusion)"'
```

In a routine: `actions_list` method `list_workflow_runs`. Match the run's
`head_sha` to the merge commit; a green run on an older commit proves nothing.
A red run after a merge means stop merging and notify with the run ID. Do not
fix forward from triage.

**What Phase 4 cannot verify:** the release build (R8, shrinking, signing). For
an AGP or Guava merge, the report says so, and the owner's next release is the
first real test. That is a known gap, not a reason to hold every AGP patch.

## Unattended (routine) mode

The Daily Renovate Triage routine runs this skill with nobody watching, from
the parent of the repo clones, so this file is not auto-loaded; the routine
prompt points here. It has the GitHub connector, `gh api` (REST only: `gh`
porcelain fails with `GraphQL is not available from Claude Code sessions`), no
Android SDK, and no owner on hand.

**May do:** read anything; merge PRs that pass every line of Phase 3, one at a
time; run Phase 4 after each.

**Never:**
- push tags, dispatch or re-run workflows, push commits, or comment on, close
  or edit PRs
- tick Dependency Dashboard boxes (`renovate.json` REBASE POLICY: never
  "rebase all")
- merge anything Ask first, or anything whose `build` is red or pending.
  Never merge on the reasoning "the failure is unrelated"

**Notify (one line per item, inside the routine's single notification), only
when:**
1. A PR needs the owner: Ask first, or held because its own change broke
   `build`. Notify on the day it is opened or its head changes (`updated_at`
   within 24h). A PR that is merely still held stays out of the notification.
2. `main` went red after a merge (Phase 4).
3. A `security`-labelled PR was merged after the newest release tag, so the fix
   reaches no user until the owner tags and sends the Play draft for review. Notify when the merge is
   under 24h old, and again if it is still unreleased 7 days later.
4. A Dependabot PR appeared (see Phase 0).

Everything else is silent: merged LOW RISK PRs, routine holds, "`main` is N
commits ahead of the release". Message shape:
`mytime: <action needed> — #<n> <title>: <one-line evidence>`.

## Known false alarms

| Observation | Why it's fine |
|---|---|
| `main` ahead of the latest release tag | Normal; the owner releases in batches |

## Report

For each PR: number, title, bucket, decision (merged / held / ask), and one or
two lines of evidence. For merges, the Phase 4 run ID, plus "release build not
verified" for AGP and Guava. Finish with how far `main` is ahead of the newest
tag: `gh api repos/$R/compare/<tag>...main --jq .ahead_by`.

## Known characteristics

- **2026-11-09, skill drafted.** `renovate.json` landed in #32 at 01:33Z;
  Renovate closed onboarding PR #31 a second later, opened Dashboard #39 and
  PRs #33–#38 (the table in Phase 1), and the merge commit's push runs were
  green. Latest release v1.1.7 (2026-09-25). The owner merges PRs with merge
  commits. A local dry run on 2026-11-08 caught
  Renovate's default maven versioning proposing Guava `33.8.0-jre` for an
  `-android` pin; `renovate.json`'s Guava rule fixes that, so a Guava PR whose
  new version is not `-android` means that rule stopped matching: Ask first.
