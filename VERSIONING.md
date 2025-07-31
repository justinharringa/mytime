# Version Management for MyTime Android App

This project uses git tags to automatically manage version numbers for the Android app. The version information is extracted from git tags during the build process.

## How It Works

### Version Format
- **Version Name**: Extracted from git tags (e.g., `v1.2.3` becomes `1.2.3`)
- **Version Code**: Calculated as `major * 10000 + minor * 100 + patch + commit_count`

### Examples
- Tag `v1.1.1` with 5 commits since tag → Version: `1.1.1`, Code: `10101 + 5 = 10106`
- Tag `v2.0.0` with 0 commits since tag → Version: `2.0.0`, Code: `20000 + 0 = 20000`

## Usage

### Using the Version Script

The easiest way to create new versions is using the provided script:

```bash
# Create a patch release (1.1.1 -> 1.1.2)
./scripts/version.sh patch

# Create a minor release (1.1.1 -> 1.2.0)
./scripts/version.sh minor

# Create a major release (1.1.1 -> 2.0.0)
./scripts/version.sh major

# Preview what would happen without making changes
./scripts/version.sh patch --dry-run
```

### Manual Git Tagging

If you prefer to create tags manually:

```bash
# Create a new version tag
git tag v1.1.2

# Push the tag to trigger the release workflow
git push origin v1.1.2
```

## Workflow

1. **Development**: Work on features in the main branch
2. **Version Creation**: Use `./scripts/version.sh` to create a new version
3. **Automatic Release**: GitHub Actions automatically builds and releases when a tag is pushed
4. **Distribution**: The built APK/AAB is uploaded to GitHub Releases

## Version Code Calculation

The version code is calculated using this formula:
```
version_code = (major * 10000) + (minor * 100) + patch + commit_count
```

This ensures:
- Each version has a unique, increasing version code
- Version codes are meaningful and traceable
- Pre-release builds (with commits since tag) have higher version codes

## GitHub Actions Integration

The existing GitHub Actions workflow (`release.yml`) automatically:
- Triggers on `v*` tags
- Builds the signed APK/AAB
- Creates a GitHub Release with release notes
- Uploads the built artifact

## Best Practices

1. **Semantic Versioning**: Use semantic versioning (major.minor.patch)
2. **Tag Naming**: Always prefix tags with `v` (e.g., `v1.1.1`)
3. **Clean Working Directory**: Ensure no uncommitted changes before creating versions
4. **Test Before Release**: Always test builds before creating new versions

## Troubleshooting

### No Tags Found
If no git tags exist, the build will use default values:
- Version Name: `1.0.0`
- Version Code: `1`

### Build Failures
If the build fails due to git commands:
- Ensure you're in a git repository
- Check that git is available in the build environment
- Verify you have proper git permissions

### Version Mismatch
If the version doesn't match expectations:
- Check the latest git tag: `git describe --tags --abbrev=0`
- Verify the tag format (should start with `v`)
- Ensure the tag has been pushed to the remote repository 