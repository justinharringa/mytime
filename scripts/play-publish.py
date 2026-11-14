#!/usr/bin/env python3
"""Uploads an App Bundle to a Google Play track through the Publisher API.

One edit: upload the .aab, upload the R8 mapping (so Play deobfuscates crash
stack traces), set the track's release with its "What's new" text, commit. Any
failure deletes the edit, so nothing half-uploaded is left behind.

Usage:
  scripts/play-publish.py --package com.harringa.mytime \
      --aab mytime/build/outputs/bundle/release/mytime-1.2.3.aab \
      --mapping mytime/build/outputs/mapping/release/mapping.txt \
      --version-name 1.2.3 --notes-file play-notes.txt \
      [--track production] [--status draft] [--language en-US]

The OAuth access token (scope androidpublisher) comes from $PLAY_ACCESS_TOKEN;
release.yml mints it keylessly with google-github-actions/auth.

Calls the API with urllib instead of a marketplace action, so no third-party
code ever holds the token. Stdlib only.
"""

import argparse
import json
import os
import sys
import urllib.error
import urllib.request

API = os.environ.get("PLAY_API_ROOT", "https://androidpublisher.googleapis.com")


class PlayError(Exception):
    pass


class Play:
    def __init__(self, package, token):
        self.base = f"/androidpublisher/v3/applications/{package}"
        self.token = token

    def call(self, method, path, body=None, upload=None, timeout=60):
        if upload is not None:
            url = f"{API}/upload{self.base}{path}?uploadType=media"
            data, content_type = upload, "application/octet-stream"
        else:
            url = f"{API}{self.base}{path}"
            data = None if body is None else json.dumps(body).encode()
            content_type = "application/json"
        request = urllib.request.Request(url, data=data, method=method)
        request.add_header("Authorization", f"Bearer {self.token}")
        if data is not None:
            request.add_header("Content-Type", content_type)
        try:
            with urllib.request.urlopen(request, timeout=timeout) as response:
                raw = response.read()
        except urllib.error.HTTPError as error:
            detail = error.read().decode(errors="replace")
            try:
                detail = json.loads(detail)["error"]["message"]
            except (ValueError, KeyError, TypeError):
                pass
            raise PlayError(f"{method} {path or '/'}: HTTP {error.code}: {detail}") from None
        return json.loads(raw) if raw else {}


def publish(play, args, notes):
    edit = play.call("POST", "/edits", body={})["id"]
    try:
        with open(args.aab, "rb") as aab:
            bundle = play.call("POST", f"/edits/{edit}/bundles", upload=aab.read(), timeout=600)
        version_code = bundle["versionCode"]
        print(f"Uploaded {args.aab}: versionCode {version_code}, sha256 {bundle.get('sha256', '?')}")

        if args.mapping:
            with open(args.mapping, "rb") as mapping:
                play.call(
                    "POST",
                    f"/edits/{edit}/apks/{version_code}/deobfuscationFiles/proguard",
                    upload=mapping.read(),
                    timeout=300,
                )
            print(f"Uploaded {args.mapping} as the deobfuscation file")

        release = {
            "name": args.version_name,
            "versionCodes": [str(version_code)],
            "status": args.status,
            "releaseNotes": [{"language": args.language, "text": notes}],
        }
        play.call("PUT", f"/edits/{edit}/tracks/{args.track}", body={"track": args.track, "releases": [release]})
        play.call("POST", f"/edits/{edit}:commit")
    except BaseException:
        try:
            play.call("DELETE", f"/edits/{edit}")
        except PlayError as error:
            print(f"(could not delete edit {edit}: {error})", file=sys.stderr)
        raise
    print(f"Committed: {args.version_name} ({version_code}) is a {args.status} release on the {args.track} track")
    return version_code


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--package", required=True)
    parser.add_argument("--aab", required=True)
    parser.add_argument("--mapping")
    parser.add_argument("--version-name", required=True)
    parser.add_argument("--notes-file", required=True)
    parser.add_argument("--track", default="production")
    parser.add_argument("--status", default="draft", choices=["draft", "completed", "halted", "inProgress"])
    parser.add_argument("--language", default="en-US")
    args = parser.parse_args(argv)

    token = os.environ.get("PLAY_ACCESS_TOKEN")
    if not token:
        print("::error::PLAY_ACCESS_TOKEN is not set", file=sys.stderr)
        return 1
    with open(args.notes_file, encoding="utf-8") as notes_file:
        notes = notes_file.read().strip()

    try:
        publish(Play(args.package, token), args, notes)
    except PlayError as error:
        print(f"::error::Google Play: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
