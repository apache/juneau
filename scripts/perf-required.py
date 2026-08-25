#!/usr/bin/env python3
# ***************************************************************************************************************************
# * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
# * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
# * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
# * with the License.  You may obtain a copy of the License at
# *
# *  http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
# * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
# * specific language governing permissions and limitations under the License.
# ***************************************************************************************************************************
"""
"## Perf section" requirement checker, for FINISHED archive entries.

Mirrors ONE of the two trigger conditions from ~/Project Work/skills/todo-and-waves/SKILL.md
(Juneau FINISHED `## Perf`): "the implementation touches any file in the perf-sensitive
hot-path inventory". It deliberately does NOT attempt the OTHER trigger ("the landing changes the
test count by more than 50 tests") -- that requires comparing against the prior FINISHED archive's
stated test count, which isn't mechanically derivable from a diff. Keep checking that one by hand.

Scope: prints yes/no for whether a `## Perf` block is REQUIRED in the FINISHED archive for the
current changes, based purely on whether any touched file's basename is in HOT_PATH_FILES below
(kept in sync with the skill's inventory: RestContext.java, Settings.java, VarResolver.java,
PropertyVar.java, BeanContext.java, BeanStore.java, MockRestClient.java, ValueResolver.java,
FieldInfo.java, ParameterInfo.java).

Diff scope (which files count as "touched"):
  1. Default: merge-base(HEAD, <upstream default branch>) vs the current working tree (so
     uncommitted changes count too) -- i.e. "everything this landing has changed so far,
     relative to where it branched off". The upstream default branch is auto-detected via
     `git symbolic-ref refs/remotes/origin/HEAD` (falling back to `git remote show origin`).
  2. Fallback: if no upstream default branch can be resolved (no "origin" remote, detached
     checkout, etc.), falls back to `git diff --name-only HEAD` -- i.e. just the working tree's
     uncommitted changes vs the last commit.
  3. --against <ref> overrides auto-detection: diffs merge-base(HEAD, <ref>) vs the working tree.
  4. --files <path...> bypasses git entirely and checks an explicit file list (useful for testing,
     or for checking a specific set of files without touching git).

Exit status doubles as the answer, so this composes in a shell conditional:
  0   Perf section NOT required (no hot-path file touched).
  1   Perf section REQUIRED (at least one hot-path file touched).
  2   Could not determine the diff (git command failed) -- distinct from "no".

Usage:
    ./scripts/perf-required.py
    ./scripts/perf-required.py --against origin/master
    ./scripts/perf-required.py --files juneau-rest/juneau-rest-server/.../RestContext.java
    ./scripts/perf-required.py --list-hot-paths

Options:
    --against <ref>    Compare against merge-base(HEAD, <ref>) instead of auto-detecting the
                        upstream default branch.
    --files <path...>  Check this explicit file list instead of running git diff.
    --list-hot-paths   Print the hot-path filename inventory (one per line) and exit 0.
    --help, -h         Show this help message.
"""

from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent

# Kept in sync with ~/Project Work/skills/todo-and-waves/SKILL.md Juneau FINISHED `## Perf`
# hot-path inventory. Matched by basename only (these class names are unique enough
# across the repo that a full-path match would just add fragility for no benefit).
HOT_PATH_FILES = (
    "RestContext.java",
    "Settings.java",
    "VarResolver.java",
    "PropertyVar.java",
    "BeanContext.java",
    "BeanStore.java",
    "MockRestClient.java",
    "ValueResolver.java",
    "FieldInfo.java",
    "ParameterInfo.java",
)


def git_output(args: list) -> str | None:
    """Run a git command in REPO_ROOT; return stripped stdout, or None on failure."""
    try:
        result = subprocess.run(
            ["git", *args],
            cwd=REPO_ROOT,
            capture_output=True,
            text=True,
            check=True,
        )
        return result.stdout.strip()
    except Exception:
        return None


def detect_upstream_default_branch() -> str | None:
    """Auto-detect the upstream default branch (e.g. "origin/master"), or None if it can't be resolved."""
    ref = git_output(["symbolic-ref", "refs/remotes/origin/HEAD"])
    if ref:
        return ref.removeprefix("refs/remotes/")

    # Older/leaner checkouts may not have origin/HEAD set; `git remote show origin` still works
    # (it just costs a network round-trip in some git versions -- acceptable for this use case).
    info = git_output(["remote", "show", "origin"])
    if info:
        for line in info.splitlines():
            line = line.strip()
            if line.startswith("HEAD branch:"):
                branch = line.split(":", 1)[1].strip()
                if branch and branch != "(unknown)":
                    return f"origin/{branch}"
    return None


def touched_files(against: str | None) -> tuple:
    """
    Returns (files, error):
      - files: list of touched relative paths (possibly empty), or [] on error
      - error: human-readable description of what went wrong, or None on success
    """
    upstream = against or detect_upstream_default_branch()

    if upstream:
        merge_base = git_output(["merge-base", "HEAD", upstream])
        if merge_base:
            diff = git_output(["diff", "--name-only", merge_base])
            if diff is not None:
                return ([line for line in diff.splitlines() if line], None)
            return ([], f"'git diff --name-only {merge_base}' failed")
        # Explicit --against was given but couldn't be resolved -- that's a user error, report it
        # rather than silently falling back to HEAD (which could mask an intended comparison).
        if against:
            return ([], f"could not resolve merge-base(HEAD, {against}) -- check the ref name")

    # No upstream default branch resolvable -- fall back to working-tree-vs-HEAD.
    diff = git_output(["diff", "--name-only", "HEAD"])
    if diff is not None:
        return ([line for line in diff.splitlines() if line], None)
    return ([], "'git diff --name-only HEAD' failed -- not a git repo?")


def hot_path_hits(files: list) -> list:
    """Return the subset of files whose basename is in HOT_PATH_FILES, preserving input order."""
    return [f for f in files if Path(f).name in HOT_PATH_FILES]


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Check whether a FINISHED archive's '## Perf' block is required (hot-path-file trigger only).",
        epilog=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("--against", metavar="REF", help="Compare against merge-base(HEAD, REF) instead of auto-detecting.")
    parser.add_argument("--files", nargs="+", metavar="PATH", help="Check this explicit file list instead of running git diff.")
    parser.add_argument("--list-hot-paths", action="store_true", help="Print the hot-path filename inventory and exit.")
    args = parser.parse_args()

    if args.list_hot_paths:
        for name in HOT_PATH_FILES:
            print(name)
        return 0

    if args.files is not None:
        files, error = args.files, None
    else:
        files, error = touched_files(args.against)

    if error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 2

    hits = hot_path_hits(files)

    if hits:
        print("yes -- ## Perf block IS required (hot-path file(s) touched):")
        for f in hits:
            print(f"  {f}")
        print()
        print("Note: this only checks the hot-path-file trigger. Also check by hand whether the")
        print("test-count-delta trigger (>50 net tests vs the prior FINISHED landing) applies.")
        return 1

    print("no -- ## Perf block is not required by the hot-path-file trigger.")
    print("Note: this only checks the hot-path-file trigger. Also check by hand whether the")
    print("test-count-delta trigger (>50 net tests vs the prior FINISHED landing) applies.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
