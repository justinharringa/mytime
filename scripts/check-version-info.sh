#!/bin/bash

# Checks the version name/code that mytime/build.gradle derives in CI:
# - a tag build (what release.yml runs) must use the tag, e.g. v2.3.4 -> 2.3.4 / 20304
# - a pull request build must still get a plain x.y.z name, not the "<n>/merge" ref
# Usage (from the repo root): ./scripts/check-version-info.sh

set -euo pipefail

version_info() {
    env CI=true TAG_NAME= "$@" ./gradlew -q :mytime:outputVersionInfo --console=plain
}

value() {
    grep "^$1=" <<<"$2" | cut -d= -f2
}

fail() {
    echo "FAIL: $1"
    exit 1
}

tag_build=$(version_info GITHUB_REF_TYPE=tag GITHUB_REF_NAME=v2.3.4)
[ "$(value VERSION_NAME "$tag_build")" = "2.3.4" ] || fail "tag build: expected VERSION_NAME=2.3.4, got: $tag_build"
[ "$(value VERSION_CODE "$tag_build")" = "20304" ] || fail "tag build: expected VERSION_CODE=20304, got: $tag_build"
echo "ok: tag v2.3.4 -> $(value VERSION_NAME "$tag_build") ($(value VERSION_CODE "$tag_build"))"

pr_build=$(version_info GITHUB_REF_TYPE=branch GITHUB_REF_NAME=27/merge)
pr_name=$(value VERSION_NAME "$pr_build")
[[ "$pr_name" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || fail "pull request build: expected an x.y.z VERSION_NAME, got: $pr_build"
echo "ok: pull request ref 27/merge -> $pr_name ($(value VERSION_CODE "$pr_build"))"
