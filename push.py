#!/usr/bin/env python3
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""
Commit and Push Script for juneau-release-manager

A small convenience helper for this personal repo (github.com/jamesbognar) — NOT
the Apache push.py (that one build/test/installs first; this one is just
add + commit + push, run manually whenever you want to save progress).

Usage:
    ./push.py "commit message"
    ./push.py                     # prompts for a commit message
    ./push.py --dry-run "message" # show what would happen, change nothing

Safety:
    - Never uses --force or --no-verify.
    - Never runs `git config` (it only reads config; you fix a missing
      identity yourself).
    - Exits non-zero with a clear message on any precondition failure
      (no git identity configured, nothing to commit, push rejected, etc).
"""

import argparse
import subprocess
import sys
from pathlib import Path

REPO_DIR = Path(__file__).resolve().parent


def run(cmd, cwd=REPO_DIR, check=True, capture=False, input_text=None):
    """Run a command (list form, no shell) and return the CompletedProcess."""
    return subprocess.run(
        cmd,
        cwd=cwd,
        check=check,
        capture_output=capture,
        text=True,
        input=input_text,
    )


def git_identity_configured():
    """True if `git config user.email` resolves to a non-empty value."""
    result = run(["git", "config", "--get", "user.email"], check=False, capture=True)
    return bool(result.stdout.strip())


def has_changes_to_commit():
    """True if `git status --porcelain` reports anything (staged or not)."""
    result = run(["git", "status", "--porcelain"], capture=True)
    return bool(result.stdout.strip())


def current_branch():
    result = run(["git", "rev-parse", "--abbrev-ref", "HEAD"], capture=True)
    return result.stdout.strip()


def short_sha():
    result = run(["git", "rev-parse", "--short", "HEAD"], capture=True)
    return result.stdout.strip()


def remote_url(remote="origin"):
    result = run(["git", "remote", "get-url", remote], check=False, capture=True)
    return result.stdout.strip() if result.returncode == 0 else "(no remote)"


def resolve_commit_message(cli_message):
    """Use the CLI-supplied message, or prompt for one; abort if still empty."""
    if cli_message:
        return cli_message

    try:
        message = input("Commit message: ").strip()
    except (EOFError, KeyboardInterrupt):
        print("\n❌ Aborted: no commit message provided.")
        sys.exit(1)

    if not message:
        print("❌ Aborted: commit message cannot be empty.")
        sys.exit(1)

    return message


def main():
    parser = argparse.ArgumentParser(
        description="Commit and push juneau-release-manager to origin.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  ./push.py "Fix credential validator NPE"
  ./push.py
  ./push.py --dry-run "Would-be commit message"
        """,
    )
    parser.add_argument(
        "message",
        nargs="?",
        default=None,
        help="Commit message. If omitted, you'll be prompted for one.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Show what would be done without making any changes.",
    )
    args = parser.parse_args()

    print("=" * 70)
    print("📦 juneau-release-manager push")
    print("=" * 70)

    # Identity gate — must hold before anything else.
    print("🔐 Checking git identity...")
    if not git_identity_configured():
        print("❌ ERROR: no git identity configured (git config user.email is unset).")
        print("   This has caused a failed push before on this repo. Fix it with:")
        print('     git config user.email "you@example.com"')
        print('     git config user.name "Your Name"')
        print("   (this script will never set it for you)")
        sys.exit(1)
    print("✅ Git identity configured.")

    if not has_changes_to_commit():
        print("⚠ Nothing to commit — working tree is clean. Nothing to do.")
        sys.exit(0)

    message = resolve_commit_message(args.message)
    branch = current_branch()

    if args.dry_run:
        print("\n🔍 DRY RUN — no changes will be made. Would run:")
        print("   git add .")
        print(f"   git commit -F- <<< {message!r}")
        print(f"   git push origin {branch}")
        sys.exit(0)

    print(f"\n📝 Staging all changes in {REPO_DIR}...")
    run(["git", "add", "."])

    print("📝 Committing...")
    # Pipe the message via stdin (git commit -F -) instead of -m, so multi-line
    # messages and odd characters round-trip safely with no shell quoting at all.
    run(["git", "commit", "-F", "-"], input_text=message)

    print(f"🚀 Pushing to origin/{branch}...")
    run(["git", "push", "origin", branch])

    print("\n" + "=" * 70)
    print("✅ Push complete.")
    print(f"   Branch: {branch}")
    print(f"   Commit: {short_sha()}")
    print(f"   Remote: {remote_url()}")
    print("=" * 70)


if __name__ == "__main__":
    main()
