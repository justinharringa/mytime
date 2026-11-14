# MyTime Android App

A simple Android time tracking application built with Java.

## Features

- Time tracking and check-in functionality
- List view of check-ins grouped by date
- Simple UI built from standard Android widgets
- Runs on Android 8.0+ (API 26)

## Build Requirements

- **JDK**: 21 (LTS). The build stops with a clear error on older JDKs. Android Studio's bundled JDK works; on the command line, point `JAVA_HOME` at a JDK 21. The app itself still compiles to Java 17 bytecode.
- **Android SDK**: API 36 (Android 16)
- **Gradle**: the version pinned in `gradle/wrapper/gradle-wrapper.properties` (use the wrapper)
- **Android Gradle Plugin**: the version pinned in the root `build.gradle`

Both are bumped by Renovate, which is why this list names the file rather than a number that would go stale on the next merge.

## Local Development

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd mytime
   ```

2. **Build the project**
   ```bash
   ./gradlew build
   ```

3. **Run tests**
   ```bash
   ./gradlew test
   ```
   App module tests use Robolectric, so no emulator or device is needed. For a coverage report, run `./gradlew :mytime:createDebugUnitTestCoverageReport`.

4. **Build debug APK**
   ```bash
   ./gradlew assembleDebug
   ```

5. **Build release APK**
   ```bash
   ./gradlew assembleRelease
   ```

## CI/CD Pipeline

This project uses trunk-based development with comprehensive GitHub Actions workflows:

### 🏗️ **Build & Test Workflow** (`android-build.yml`)

**Triggers:** Push to `main`, Pull Requests, Manual dispatch

- Builds the project with JDK 21
- Runs all unit tests
- Checks CI version numbering (`scripts/check-version-info.sh`)
- Adds a test coverage table to the job summary and uploads the coverage reports
- Generates debug APK (for testing)
- Runs lint checks

### 🚀 **Release Workflow** (`release.yml`)

**Triggers:** Push tags matching `v*` (e.g., `v1.1.0`)

- Builds the signed release App Bundle (AAB)
- Collects the Google Play "What's new" text from the `## Release note` section of each PR merged since the previous tag (`scripts/play-release-notes.py`)
- Creates the GitHub release with those notes plus the commit list, and attaches the AAB
- Uploads the AAB, its R8 mapping and the notes to the Google Play **production track as a draft** (`scripts/play-publish.py`)
- Only runs when you push a version tag

### 📝 **Release Note Check** (`release-note.yml`)

**Triggers:** Pull Requests (opened, edited, synchronized), except Renovate's

- Fails a PR whose body has no `## Release note` section (`.github/pull_request_template.md`). The section holds one user-facing line, or `none`, and becomes a Play "What's new" bullet unedited. `CLAUDE.md` says what makes a good one.

### 🔐 **Signing Configuration**

For release builds, the following environment variables must be set in GitHub Secrets:

- `UPLOAD_KEYSTORE`: Base64-encoded upload keystore file
- `UPLOAD_KEY_ALIAS`: Upload key alias
- `UPLOAD_KEY_PASSWORD`: Upload key password

To set up Play App Signing:
1. Enable "Signing by Google Play" in Google Play Console
2. Generate or use the provided upload keystore
3. Encode your upload keystore: `base64 -i your-upload-keystore.jks`
4. Add the encoded string to GitHub Secrets as `UPLOAD_KEYSTORE`
5. Add your upload key alias and password to the other secrets

### 📋 **Release Process**

1. **Create and push a version tag.** The version name and code come from the tag; see [VERSIONING.md](VERSIONING.md):
   ```bash
   git tag v1.1.1
   git push origin v1.1.1
   ```

2. **GitHub Actions automatically**:
   - Builds the signed release App Bundle (AAB)
   - Creates the GitHub release, with the AAB attached
   - Uploads the AAB and the release notes to Google Play as a draft production release

3. **Send it for review** in the Play Console (Publishing overview, or Production → the draft release). You can still edit the notes there first.

If a PR's note needs fixing after it merged, edit that PR's description before tagging; the notes are read from the PR bodies at tag time. Each user-facing note is one `•` bullet. Everything else in the release (Renovate bumps, `none` PRs) is summed up in **one** extra bullet, a light-hearted take on "Behind-the-scenes updates" picked from `MAINTENANCE` in `scripts/play-release-notes.py` (seeded by the tag, so re-runs agree). For example:

```
• Check-ins one minute apart are no longer skipped as duplicates
• Clearer message when a check-in is skipped
• Tightened a few bolts behind the scenes
```

### 🤖 **Google Play publishing (one-time setup)**

`release.yml` authenticates keylessly: GitHub's OIDC token is exchanged through GCP Workload Identity Federation for a short-lived token of the `play-publisher` service account. No JSON key exists. The GCP side lives in the knuckles repo (`infrastructure/terraform/gcp/play_publisher.tf`), which only accepts tokens from this repository's `release.yml` running on a `v*` tag.

1. Apply the knuckles GCP stack, and note its `play_publisher_wif_provider_name` and `play_publisher_sa_email` outputs.
2. In the Play Console: **Users and permissions → Invite new users**, enter the service account's email, and under **App permissions** add My Time with **Release to production, exclude devices, and use Play App Signing** and **Release apps to testing tracks**.
3. In this repository's **Settings → Secrets and variables → Actions → Variables**, set `PLAY_WIF_PROVIDER` and `PLAY_SERVICE_ACCOUNT` to those two outputs. They are identifiers, not secrets.
4. Optional: set `PLAY_NOTES_LANGUAGE` if the store listing's default language is not `en-US`.

Until both variables are set, the upload step is skipped with a warning and the AAB is only on the GitHub release.

### 📋 **Workflow Triggers**

- **Push**: Triggers on pushes to `main` branch
- **Pull Request**: Triggers on PRs to `main` branch
- **Manual**: Can be triggered manually via GitHub Actions UI

### 🔄 **Renovate Config Validation** (`renovate-validate.yml`)

**Triggers:** Pull Requests and pushes to `main` that touch `renovate.json`

- Runs `renovate-config-validator --strict` against `renovate.json`. A malformed config does not fail loudly on Renovate's side (it just opens no PRs), so it is checked here.

## Dependency Updates

[Renovate](https://docs.renovatebot.com/) (the Mend-hosted GitHub App) opens the dependency PRs; `renovate.json` at the repository root is the config and explains each rule in its `description` fields. In short:

- **Gradle wrapper**, **Android Gradle Plugin**, **Test dependencies** (JUnit, Robolectric, androidx.test), **GitHub Actions** (pinned to commit digests) and the **JDK** that `setup-java` installs each get their own PR, so a bump can be reviewed against the thing it is coupled to (the AGP ↔ Gradle compatibility matrix, Robolectric's supported SDK levels, the JDK guard in `build.gradle`).
- Everything is **manual review**; nothing automerges until `main` has a ruleset requiring the `build` job. The config says how to turn it on.
- No schedule windows and no PR limits, on purpose: an update Renovate holds back is reported identically to one that does not exist. The Dependency Dashboard issue lists everything Renovate sees.
- The daily Renovate triage routine merges the low-risk PRs and notifies about the rest, following `.claude/skills/renovate-triage/SKILL.md`. That file is the authority on what may be merged without a human.

## Development Workflow

This project follows **trunk-based development**:

1. **Create feature branch** from `main`
2. **Make changes** and test locally
3. **Push branch** and create pull request
4. **CI/CD pipeline** automatically tests your changes
5. **Review and merge** to `main` when ready

## Project Structure

```
mytime/
├── .github/workflows/     # GitHub Actions workflows
├── mytime/               # Main Android app module
│   ├── src/main/java/    # Java source code
│   ├── src/main/res/     # Android resources
│   └── build.gradle      # App module build config
├── libraries/timecalc/   # Time calculation library
├── build.gradle          # Root project build config
└── gradle.properties     # Gradle properties
```

## Dependencies

- **Guava**: Google's core Java libraries (immutable collections)
- **Java 8 Time API**: Built-in date/time manipulation (replaced Joda Time)

## Version Management

The project uses a centralized version management system through Gradle tasks:

### Available Tasks

- **`./gradlew showVersion`**: Display current version information including:
  - Version Name and Code
  - Target SDK and Min SDK
  - Java Version
  - Build Type and CI Environment status
  - Git information

- **`./gradlew outputVersionInfo`**: Output version information in CI/CD format:
  ```
  VERSION_NAME=1.1.6
  VERSION_CODE=10106
  TARGET_SDK=36
  MIN_SDK=26
  JAVA_VERSION=17
  ```

### Single Source of Truth

All version information is extracted from the Gradle build configuration, ensuring consistency across:
- Local development
- CI/CD pipelines
- Release generation
- Documentation

This eliminates the need for manual parsing of build.gradle files and reduces the risk of version mismatches.

## Technical Decisions

### Current Architecture
- **ListView with BaseAdapter**: Using the traditional ListView pattern with proper ViewHolder pattern for performance
- **Basic Android Widgets**: Using TextView, Button, TimePicker for minimal method count
- **View Recycling**: Implemented ViewHolder pattern in CheckInAdapter for efficient scrolling

### Future Improvements
- **RecyclerView Migration**: Consider migrating from ListView to RecyclerView for better performance
- **Material Design**: Upgrade to Material Design components for modern UI/UX

## Compliance

This app is compliant with:

- ✅ **Google Play target SDK requirements** (API 36)
- ✅ **JDK 21 (LTS) build**, with the app compiled to Java 17 bytecode
- ✅ **Java 8 Time API** support (minSdk 26)
- ✅ **Backward compatibility** (minSdk 26 - Android 8.0+)

## Contributing

1. Fork the repository
2. Create a feature branch from `main`
3. Make your changes
4. Ensure all tests pass (CI/CD will verify)
5. Submit a pull request to `main`

The CI/CD pipeline will automatically test your changes across multiple environments.
