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
commit + push, run manually whenever you want to save progress).

Usage:
    ./push.py "commit message"
    ./push.py                     # prompts for a commit message
    ./push.py --dry-run "message" # show what would happen, change nothing

Safety:
    - Never uses --force or --no-verify.
    - Never runs `git config` (it only reads config; you fix a missing
      identity yourself).
    - Never runs `git add` (see check_unreviewed_changes()'s docstring): a
      staged path is the operator's record that they've reviewed it and it's
      good, so this script commits the index exactly as it finds it and
      refuses outright -- commits and pushes nothing -- if the working tree
      has any unstaged modification or any untracked, non-gitignored file.
    - Exits non-zero with a clear message on any precondition failure
      (no git identity configured, unreviewed changes present, push
      rejected, etc).
"""

import argparse
import shlex
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


def git_identity_configured(repo_dir):
    """True if `git config user.email` resolves to a non-empty value."""
    result = run(["git", "config", "--get", "user.email"], cwd=repo_dir, check=False, capture=True)
    return bool(result.stdout.strip())


def get_staged_paths(repo_dir):
    """
    List paths currently staged in the index.

    This is the sole definition of "what push.py is about to commit": a staged path is the
    operator's record that they reviewed it and it's good (see check_unreviewed_changes()'s
    docstring), so this script commits the index exactly as it finds it rather than deciding
    what belongs in the commit itself.

    Returns:
        list[str]: staged paths (possibly empty).

    Raises:
        subprocess.CalledProcessError, OSError: if the check itself can't run. Deliberately not
        caught here -- an error running `git diff --cached` almost always means something is
        fundamentally wrong with the repo, and this check exists specifically to protect an
        invariant, so the caller should fail closed (abort) rather than silently proceed.
    """
    result = subprocess.run(
        ["git", "diff", "--cached", "--name-only"],
        cwd=repo_dir,
        capture_output=True,
        text=True,
        check=True,
    )
    return [line for line in result.stdout.splitlines() if line.strip()]


def check_unreviewed_changes(repo_dir):
    """
    List paths that have NOT been staged: tracked files with unstaged modifications/deletions,
    plus untracked files that are not gitignored.

    Staging is how this script knows something has been reviewed (see get_staged_paths()'s
    docstring); this is the other half of that invariant. Its presence alongside staged content
    means the operator's review pass isn't finished, so nothing should be committed or pushed
    yet -- not even the already-staged part -- until the tree is fully resolved one way or
    another (staged, committed separately, stashed, or discarded).

    Gitignored untracked files are deliberately excluded (via `git ls-files --exclude-standard`):
    build output and similar noise was never going to be staged or reviewed, and flagging it
    would make this refusal fire on essentially every real run.

    Returns:
        list[str]: unreviewed paths (possibly empty).

    Raises:
        subprocess.CalledProcessError, OSError: see get_staged_paths() -- same fail-closed
        reasoning applies here.
    """
    unstaged = subprocess.run(
        ["git", "diff", "--name-only"],
        cwd=repo_dir,
        capture_output=True,
        text=True,
        check=True,
    ).stdout.splitlines()
    untracked = subprocess.run(
        ["git", "ls-files", "--others", "--exclude-standard"],
        cwd=repo_dir,
        capture_output=True,
        text=True,
        check=True,
    ).stdout.splitlines()
    return [line for line in (*unstaged, *untracked) if line.strip()]


def current_branch(repo_dir):
    result = run(["git", "rev-parse", "--abbrev-ref", "HEAD"], cwd=repo_dir, capture=True)
    return result.stdout.strip()


def short_sha(repo_dir):
    result = run(["git", "rev-parse", "--short", "HEAD"], cwd=repo_dir, capture=True)
    return result.stdout.strip()


def remote_url(repo_dir, remote="origin"):
    result = run(["git", "remote", "get-url", remote], cwd=repo_dir, check=False, capture=True)
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


def commit_and_push(repo_dir, cli_message, *, dry_run=False):
    """
    Commit whatever is already staged in repo_dir and push, refusing to commit or push
    ANYTHING if the working tree has unreviewed content.

    Extracted out of main() (as a repo_dir-parameterized function, mirroring juneau's/ssc's
    own commit_and_push()) so it's testable against a real temporary git repository without
    needing to fake this module's REPO_DIR constant.

    A staged path is the operator's record that they've reviewed it and it's good -- staging
    as they review, not as a mechanical step right before committing. So this commits the
    index exactly as it finds it (no `git add`, anywhere, ever): it does not decide what
    belongs in the commit, because the index already says. It refuses outright -- commits and
    pushes NOTHING -- if the working tree has any unstaged modification to a tracked file, or
    any untracked, non-gitignored file (see check_unreviewed_changes()), since their presence
    means the review pass isn't finished. This is deliberately strict with no override flag --
    even a flag that names the exact paths being overridden is still a way to push something
    nobody reviewed, which is the one thing this refusal exists to prevent. For genuinely
    partial work (some files reviewed and staged, others still mid-edit), the sanctioned way
    out is `git stash push -- <paths>` before running push.py and `git stash pop` after; the
    refusal message itself prints that command with the actual offending paths already filled
    in.

    A fully CLEAN tree (nothing staged, nothing unreviewed) is a benign no-op, not a refusal:
    there is genuinely nothing to commit, and this repo has no push-only path (unlike juneau's
    ahead-of-upstream case), so there is nothing else to do. The dirty-but-unstaged case is
    still refused (dirty content is, by definition, unreviewed) -- that is the case this
    invariant exists to catch.

    Both gate queries (check_unreviewed_changes(), get_staged_paths()) are read-only, so they
    always run -- including under --dry-run -- rather than being skipped. That is what makes
    --dry-run report the REAL outcome instead of a guess: it can never claim it "would commit"
    a tree the real run would refuse.

    Args:
        repo_dir: Repository root to operate against.
        cli_message: Commit message supplied on the CLI, or None to prompt for one (the prompt
            is only reached once it's established that there is something to actually commit).
        dry_run: If True, report what would happen and change nothing (a refusal is reported
            as "would REFUSE" and exits 0, since --dry-run itself did not fail).

    Returns:
        int: 0 on success (committed+pushed, a genuine no-op on a clean tree, or a --dry-run
             report of either outcome). 1 if refused (unreviewed content present) or the gate
             checks themselves failed to run (fails closed -- see check_unreviewed_changes()'s
             docstring).
    """
    try:
        unreviewed = check_unreviewed_changes(repo_dir)
        staged = get_staged_paths(repo_dir)
    except (subprocess.CalledProcessError, OSError) as e:
        print(f"\n❌ ERROR: Could not check the working tree/index: {e}")
        return 1

    if unreviewed:
        header = "🔍 DRY RUN — would REFUSE" if dry_run else "❌ ERROR"
        print(f"\n{header}: the working tree has unreviewed changes — staging is how this "
              "script knows something has been reviewed, and these paths are not staged:")
        for path in unreviewed:
            print(f"   {path}")
        print("   Why: everything this pushes should be something you've read, not just")
        print("   something that happened to be in the tree.")
        print("\n   Nothing was committed or pushed. Stage each path above if you've reviewed")
        print("   it, or set it aside for this run and restore it after:")
        quoted_paths = " ".join(shlex.quote(path) for path in unreviewed)
        print(f"     git stash push -- {quoted_paths}")
        print("     git stash pop")
        return 0 if dry_run else 1

    if not staged:
        print("⚠ Nothing to commit — working tree is clean. Nothing to do.")
        return 0

    message = resolve_commit_message(cli_message)
    branch = current_branch(repo_dir)

    if dry_run:
        plural = "" if len(staged) == 1 else "s"
        print(f"\n🔍 DRY RUN — no changes will be made. Would commit the staged index "
              f"({len(staged)} path{plural}) and push to origin/{branch}:")
        print(f"   git commit -F- <<< {message!r}")
        print(f"   git push origin {branch}")
        return 0

    print("\n📝 Committing...")
    # Commit the index exactly as it was found -- see check_unreviewed_changes()'s docstring
    # for why there is no `git add` anywhere in this script. Pipe the message via stdin
    # (git commit -F -) instead of -m, so multi-line messages and odd characters round-trip
    # safely with no shell quoting at all.
    run(["git", "commit", "-F", "-"], cwd=repo_dir, input_text=message)

    print(f"🚀 Pushing to origin/{branch}...")
    run(["git", "push", "origin", branch], cwd=repo_dir)

    print("\n" + "=" * 70)
    print("✅ Push complete.")
    print(f"   Branch: {branch}")
    print(f"   Commit: {short_sha(repo_dir)}")
    print(f"   Remote: {remote_url(repo_dir)}")
    print("=" * 70)
    return 0


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
    if not git_identity_configured(REPO_DIR):
        print("❌ ERROR: no git identity configured (git config user.email is unset).")
        print("   This has caused a failed push before on this repo. Fix it with:")
        print('     git config user.email "you@example.com"')
        print('     git config user.name "Your Name"')
        print("   (this script will never set it for you)")
        sys.exit(1)
    print("✅ Git identity configured.")

    sys.exit(commit_and_push(REPO_DIR, args.message, dry_run=args.dry_run))


if __name__ == "__main__":
    main()
