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

This project uses trunk-based development with a comprehensive GitHub Actions workflow that:

### 🏗️ **Build Job**

- Builds the project with Java 17
- Runs all tests
- Generates debug and release APKs
- Uploads build artifacts
- Runs lint checks

### 🔄 **Compatibility Test Job**

- Tests build compatibility across multiple Android API levels (16, 21, 29, 34)
- Ensures backward compatibility with older Android versions

### 📊 **Code Quality Job**

- Runs static code analysis tools
- Performs code quality checks
- Generates build reports

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
- **Material Design**: Google's Material Design components
- **Joda Time**: Date/time manipulation library
- **Guava**: Google's core Java libraries

## Compliance

This app is compliant with:

- ✅ **Google Play target SDK requirements** (API 34)
- ✅ **Modern Android development practices** (AndroidX)
- ✅ **Java 17 LTS** for optimal performance
- ✅ **Backward compatibility** (minSdk 16)

## Contributing

1. Fork the repository
2. Create a feature branch from `main`
3. Make your changes
4. Ensure all tests pass (CI/CD will verify)
5. Submit a pull request to `main`

The CI/CD pipeline will automatically test your changes across multiple environments. 