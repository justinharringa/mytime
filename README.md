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
- **Gradle**: 9.8.0 (via the wrapper)
- **Android Gradle Plugin**: 9.4.1

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
- Creates GitHub release with release notes
- Uploads the AAB to release assets
- Only runs when you push a version tag

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
   - Creates GitHub release
   - Uploads the AAB to release assets

3. **Upload the AAB** to Google Play Console.

### 📋 **Workflow Triggers**

- **Push**: Triggers on pushes to `main` branch
- **Pull Request**: Triggers on PRs to `main` branch
- **Manual**: Can be triggered manually via GitHub Actions UI

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
