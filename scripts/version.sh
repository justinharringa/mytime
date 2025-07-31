#!/bin/bash

# Version management script for MyTime Android app
# Usage: ./scripts/version.sh [major|minor|patch] [--dry-run]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

# Get current version from git tags
get_current_version() {
    local latest_tag=$(git describe --tags --abbrev=0 2>/dev/null || echo "")
    if [ -z "$latest_tag" ]; then
        echo "0.0.0"
    else
        # Remove 'v' prefix if present
        echo "${latest_tag#v}"
    fi
}

# Increment version based on type
increment_version() {
    local version=$1
    local increment_type=$2
    
    IFS='.' read -ra VERSION_PARTS <<< "$version"
    local major=${VERSION_PARTS[0]:-0}
    local minor=${VERSION_PARTS[1]:-0}
    local patch=${VERSION_PARTS[2]:-0}
    
    case $increment_type in
        major)
            major=$((major + 1))
            minor=0
            patch=0
            ;;
        minor)
            minor=$((minor + 1))
            patch=0
            ;;
        patch)
            patch=$((patch + 1))
            ;;
        *)
            print_error "Invalid increment type: $increment_type. Use major, minor, or patch."
            exit 1
            ;;
    esac
    
    echo "$major.$minor.$patch"
}

# Calculate version code
calculate_version_code() {
    local version=$1
    IFS='.' read -ra VERSION_PARTS <<< "$version"
    local major=${VERSION_PARTS[0]:-0}
    local minor=${VERSION_PARTS[1]:-0}
    local patch=${VERSION_PARTS[2]:-0}
    
    # Calculate base version code: major * 10000 + minor * 100 + patch
    local base_version_code=$((major * 10000 + minor * 100 + patch))
    
    # Add commit count since the tag
    local latest_tag=$(git describe --tags --abbrev=0 2>/dev/null || echo "")
    local commit_count=0
    if [ -n "$latest_tag" ]; then
        commit_count=$(git rev-list --count "${latest_tag}..HEAD" 2>/dev/null || echo "0")
    fi
    
    echo $((base_version_code + commit_count))
}

# Main script logic
main() {
    local increment_type=${1:-patch}
    local dry_run=false
    
    if [ "$2" = "--dry-run" ]; then
        dry_run=true
    fi
    
    # Check if we're in a git repository
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        print_error "Not in a git repository"
        exit 1
    fi
    
    # Check for uncommitted changes
    if [ "$dry_run" = false ] && [ -n "$(git status --porcelain)" ]; then
        print_warning "You have uncommitted changes. Please commit or stash them before creating a new version."
        git status --short
        exit 1
    fi
    
    local current_version=$(get_current_version)
    local new_version=$(increment_version "$current_version" "$increment_type")
    local version_code=$(calculate_version_code "$new_version")
    
    print_info "Current version: $current_version"
    print_info "New version: $new_version (version code: $version_code)"
    
    if [ "$dry_run" = true ]; then
        print_status "Dry run mode - no changes will be made"
        print_info "To create the new version, run: $0 $increment_type"
        exit 0
    fi
    
    # Create and push the new tag
    local tag_name="v$new_version"
    
    print_status "Creating git tag: $tag_name"
    git tag "$tag_name"
    
    print_status "Pushing tag to remote..."
    git push origin "$tag_name"
    
    print_status "Version $new_version has been created and pushed!"
    print_info "The GitHub Actions workflow will automatically build and release the new version."
}

# Show help if no arguments provided
if [ $# -eq 0 ]; then
    echo "Usage: $0 [major|minor|patch] [--dry-run]"
    echo ""
    echo "Examples:"
    echo "  $0 patch          # Increment patch version (1.1.1 -> 1.1.2)"
    echo "  $0 minor          # Increment minor version (1.1.1 -> 1.2.0)"
    echo "  $0 major          # Increment major version (1.1.1 -> 2.0.0)"
    echo "  $0 patch --dry-run # Show what would happen without making changes"
    echo ""
    echo "This script will:"
    echo "  1. Calculate the new version based on the current git tag"
    echo "  2. Create a new git tag with the version"
    echo "  3. Push the tag to trigger the GitHub Actions release workflow"
    exit 0
fi

main "$@" 