# MyTime Android App

A simple Android time tracking application built with Java and AndroidX.

## Features

- Time tracking and check-in functionality
- List view of check-ins grouped by date
- Modern Android UI with Material Design components
- Compatible with Android API 16+ (Android 4.1+)

## Build Requirements

- **Java**: 17 (LTS)
- **Android SDK**: API 34 (Android 14)
- **Gradle**: 8.4
- **Android Gradle Plugin**: 8.3.0

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

- Builds the project with Java 17
- Runs all tests
- Generates debug APK (for testing)
- Runs lint checks
- Tests compatibility across API levels (16, 21, 29, 34)
- Performs code quality checks

### 🚀 **Release Workflow** (`release.yml`)

**Triggers:** Push tags matching `v*` (e.g., `v1.1.0`)

- Builds signed release APK
- Creates GitHub release with release notes
- Uploads signed APK to release assets
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

1. **Update version** in `mytime/build.gradle`:
   ```gradle
   versionCode 12
   versionName "1.1.1"
   ```

2. **Create and push tag**:
   ```bash
   git tag v1.1.1
   git push origin v1.1.1
   ```

3. **GitHub Actions automatically**:
   - Builds signed release APK
   - Creates GitHub release
   - Uploads APK to release assets

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

- **AndroidX**: Modern Android support libraries
- **Guava**: Google's core Java libraries (immutable collections)
- **Java 8 Time API**: Built-in date/time manipulation (replaced Joda Time)

## Technical Decisions

### Current Architecture
- **ListView with BaseAdapter**: Using the traditional ListView pattern with proper ViewHolder pattern for performance
- **Basic Android Widgets**: Using TextView, Button, TimePicker for minimal method count
- **Multidex**: Enabled to handle 64K method limit while maintaining Guava's immutable properties
- **View Recycling**: Implemented ViewHolder pattern in CheckInAdapter for efficient scrolling

### Future Improvements
- **RecyclerView Migration**: Consider migrating from ListView to RecyclerView for better performance
- **Material Design**: Upgrade to Material Design components for modern UI/UX
- **Note**: These upgrades would add ~9,000 methods but are supported by multidex

## Compliance

This app is compliant with:

- ✅ **Google Play target SDK requirements** (API 34)
- ✅ **Modern Android development practices** (AndroidX)
- ✅ **Java 17 LTS** for optimal performance
- ✅ **Java 8 Time API** support (minSdk 26)
- ✅ **Backward compatibility** (minSdk 26 - Android 8.0+)

## Contributing

1. Fork the repository
2. Create a feature branch from `main`
3. Make your changes
4. Ensure all tests pass (CI/CD will verify)
5. Submit a pull request to `main`

The CI/CD pipeline will automatically test your changes across multiple environments. 