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
Re-pin the downstream consumers (Release Manager, Support Console) to a fresh local Apache
Juneau build.

MECHANISM. Release Manager (~/git/apache/release-manager) and the Sandbox Support Console
(~/git/sandbox/sandbox-support-console) both declare a floating
`<juneau.version>10.0.0-SNAPSHOT</juneau.version>` that Maven resolves from the shared ~/.m2
local repository. There is no version property to bump and no per-project pin file to edit:
the old isolated `.m2-pinned` / `juneau-pinned` mechanism is retired (see the console's
pom.xml comment and its README.md "Dependency status" section -- its `.mvn/maven.config` is
now a 0-byte file, and release-manager has no `.mvn/` at all). "Re-pin" therefore means exactly
one thing: rebuild Juneau's HEAD and `mvn install` it into ~/.m2, so every consumer that
resolves `org.apache.juneau:*:10.0.0-SNAPSHOT` from there picks up the new build the next time
it builds. `-nsu` (no-snapshot-update) matters here: without it, Maven can pull an Apache CI
timestamped SNAPSHOT through the global settings.xml `apache.snapshots` repository and shadow
the local build this script just installed, silently undoing the whole point of running it.

THE GUARD THIS SCRIPT EXISTS FOR. Installing Juneau while its working tree is dirty bakes
unaccepted/uncommitted work into the machine-wide ~/.m2 -- and every concurrent session on this
machine then silently resolves that build, with no indication anything unreviewed rode along.
This is a documented recurring hazard. So before touching ~/.m2, this script verifies the
Juneau tree is completely clean (no modified tracked files, no staged changes, no untracked
non-ignored files -- i.e. `git status --porcelain` is empty) and REFUSES -- nonzero exit,
offending paths printed -- if it is not. This mirrors scripts/push.py's own
unreviewed-changes gate (see check_unreviewed_changes() there): staged-but-uncommitted is not
an exception, since a staged path can still be reverted before commit and is not "the same
thing reviewed" as what the operator is asking this script to publish machine-wide.

There is deliberately no way to skip the guard. A documented --allow-dirty escape hatch was
considered and OMITTED: the one case that flag would ever be reached for is a dirty tree the
operator is not looking straight at, since if they were, the sanctioned move is to `git
stash`, commit, or discard the change -- not teach a script to skip the one check it exists
to enforce. If a genuine scratch-directory use case shows up later, add --allow-dirty
deliberately then; the safe default here is hard refusal.

WHAT THIS SCRIPT DOES, IN ORDER:
  1. Resolve the Juneau repo root (default ~/git/apache/juneau; --juneau-dir overrides).
  2. GUARD: refuse (exit 1) unless `git status --porcelain` is empty in that tree.
  3. Print the commit about to be installed (short hash + subject), and a warning if HEAD
     is not identical to the locally known origin/master (unpushed work is about to be baked
     into ~/.m2 too -- allowed, since "clean" and "pushed" are different questions, but worth
     a loud warning). This does NOT run `git fetch`, so it can miss commits pushed elsewhere
     since the last fetch; the warning notes that.
  4. Run `mvn -B -nsu clean install -DskipTests` in the Juneau tree, streaming output live and
     aborting on a nonzero exit. JAVA_HOME defaults to ~/jdk/default if that path exists, else
     whatever is already in the environment.
  5. Optional --verify-consumers: after a successful install, run
     `mvn -B -nsu clean test-compile` in release-manager and the support console (paths
     overridable via --release-manager-dir / --support-console-dir) and report pass/fail for
     each. This never runs any git command in either consumer tree -- mvn only.
  6. Print a final summary: the installed version (read from Juneau's own pom.xml), the
     commit just installed, and where it landed (~/.m2/repository/org/apache/juneau/).

Usage:
    ./scripts/repin-consumers.py
    ./scripts/repin-consumers.py --verify-consumers
    ./scripts/repin-consumers.py --juneau-dir ~/git/apache/juneau-9.2.1 --verify-consumers
    ./scripts/repin-consumers.py --help

Exit status:
    0   Install succeeded (and, if --verify-consumers was given, both consumers compiled --
        see --verify-consumers above for how a consumer failure is reported instead).
    1   The dirty-tree guard refused. Nothing was built or installed.
    2   Usage error, or the Juneau tree / its pom.xml could not be found or read.
    3   The Maven install itself failed (nonzero exit). ~/.m2 may or may not have been
        partially updated by Maven; fix the build and re-run.
    4   The install succeeded but at least one --verify-consumers check failed to compile.
        The install already happened; this is reported, not treated as an install failure.
"""

from __future__ import annotations

import argparse
import os
import re
import subprocess
import sys
from pathlib import Path

JUNEAU_DIR_DEFAULT = Path.home() / "git" / "apache" / "juneau"
RELEASE_MANAGER_DIR_DEFAULT = Path.home() / "git" / "apache" / "release-manager"
SUPPORT_CONSOLE_DIR_DEFAULT = Path.home() / "git" / "sandbox" / "sandbox-support-console"
JDK_DEFAULT = Path.home() / "jdk" / "default"

MVN_INSTALL_ARGV = ["mvn", "-B", "-nsu", "clean", "install", "-DskipTests"]
MVN_VERIFY_ARGV = ["mvn", "-B", "-nsu", "clean", "test-compile"]


def check_dirty(juneau_dir: Path) -> list[str]:
    """
    Return the `git status --porcelain` lines for juneau_dir -- empty means clean.

    Covers all three things the guard cares about: modified tracked files, staged changes, and
    untracked non-ignored files all show up as porcelain lines, so an empty list is the only
    way this tree is safe to install from.
    """
    result = subprocess.run(
        ["git", "-C", str(juneau_dir), "status", "--porcelain"],
        capture_output=True,
        text=True,
        check=True,
    )
    return [line for line in result.stdout.splitlines() if line.strip()]


def _git_out(repo_dir: Path, argv: list[str]) -> str:
    result = subprocess.run(
        ["git", "-C", str(repo_dir), *argv],
        capture_output=True,
        text=True,
        check=True,
    )
    return result.stdout.strip()


def describe_head(juneau_dir: Path) -> tuple[str, str, bool | None]:
    """
    Return (short_hash, subject, unpushed) for the commit about to be installed.

    unpushed is True if HEAD differs from the locally known origin/master, False if identical,
    or None if origin/master could not be resolved at all (no such remote-tracking ref) --
    reported as a note, never a refusal: this script's guard is about the working TREE being
    clean, not about whether HEAD happens to be pushed yet.
    """
    short_hash = _git_out(juneau_dir, ["rev-parse", "--short", "HEAD"])
    subject = _git_out(juneau_dir, ["log", "-1", "--format=%s"])

    unpushed: bool | None
    try:
        head_sha = _git_out(juneau_dir, ["rev-parse", "HEAD"])
        origin_sha = _git_out(juneau_dir, ["rev-parse", "origin/master"])
        unpushed = head_sha != origin_sha
    except subprocess.CalledProcessError:
        unpushed = None

    return short_hash, subject, unpushed


def read_pom_version(pom_path: Path) -> str | None:
    """
    Best-effort read of the reactor root <version> from pom.xml, without a full XML parse.

    Matches the root project's own artifactId/version pair (`<artifactId>juneau</artifactId>`
    immediately followed by its `<version>`), which is unambiguous in Juneau's own pom.xml
    layout. Returns None (reported as "unknown") rather than guessing if that shape ever
    changes or the file can't be read.
    """
    try:
        text = pom_path.read_text(encoding="utf-8")
    except OSError:
        return None
    match = re.search(r"<artifactId>juneau</artifactId>\s*<version>([^<]+)</version>", text)
    return match.group(1) if match else None


def resolve_java_home() -> str | None:
    """~/jdk/default if it exists, else whatever JAVA_HOME is already set to (or None)."""
    if JDK_DEFAULT.exists():
        return str(JDK_DEFAULT)
    return os.environ.get("JAVA_HOME")


def run_streaming(argv: list[str], cwd: Path, env: dict) -> int:
    """Run argv with output streamed live -- never captured, never shell=True, always an argv list."""
    print(f"$ {' '.join(argv)}   (cwd={cwd})")
    result = subprocess.run(argv, cwd=str(cwd), env=env, check=False)
    return result.returncode


def verify_consumer(label: str, repo_dir: Path, env: dict) -> bool:
    """
    Run MVN_VERIFY_ARGV in repo_dir and report pass/fail.

    Never touches git state in repo_dir -- the only subprocess this runs is mvn.
    """
    print(f"\n--- Verifying {label} ({repo_dir}) ---")
    if not repo_dir.is_dir():
        print(f"SKIP: {repo_dir} does not exist.")
        return False
    returncode = run_streaming(MVN_VERIFY_ARGV, cwd=repo_dir, env=env)
    if returncode == 0:
        print(f"PASS: {label} compiled against the freshly installed Juneau build.")
        return True
    print(f"FAIL: {label} did not compile (mvn exit {returncode}).")
    return False


def main() -> int:
    parser = argparse.ArgumentParser(
        description=(
            "Rebuild Apache Juneau's HEAD and install it into the shared ~/.m2, so the "
            "downstream consumers (Release Manager, Support Console) pick it up on their next "
            "build. Refuses to run if the Juneau tree is not completely clean -- see this "
            "script's module docstring for why there is no override flag."
        ),
        epilog=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument(
        "--juneau-dir",
        type=Path,
        default=JUNEAU_DIR_DEFAULT,
        metavar="PATH",
        help=f"Juneau repo root to build and install (default: {JUNEAU_DIR_DEFAULT}).",
    )
    parser.add_argument(
        "--verify-consumers",
        action="store_true",
        help=(
            "After a successful install, run `mvn -B -nsu clean test-compile` in "
            "release-manager and the support console (see --release-manager-dir / "
            "--support-console-dir) and report pass/fail for each. Never touches either "
            "consumer's git state."
        ),
    )
    parser.add_argument(
        "--release-manager-dir",
        type=Path,
        default=RELEASE_MANAGER_DIR_DEFAULT,
        metavar="PATH",
        help=(
            "Release Manager repo root, used only with --verify-consumers "
            f"(default: {RELEASE_MANAGER_DIR_DEFAULT})."
        ),
    )
    parser.add_argument(
        "--support-console-dir",
        type=Path,
        default=SUPPORT_CONSOLE_DIR_DEFAULT,
        metavar="PATH",
        help=(
            "Support Console repo root, used only with --verify-consumers "
            f"(default: {SUPPORT_CONSOLE_DIR_DEFAULT})."
        ),
    )
    args = parser.parse_args()

    juneau_dir = args.juneau_dir.expanduser().resolve()
    if not juneau_dir.is_dir():
        print(f"ERROR: {juneau_dir} does not exist or is not a directory.", file=sys.stderr)
        return 2
    if not (juneau_dir / ".git").exists():
        print(f"ERROR: {juneau_dir} is not a git working tree (no .git found).", file=sys.stderr)
        return 2

    # THE GUARD. Nothing above this point runs a build; nothing below it runs unless the tree
    # is provably clean. See the module docstring for why there is no override flag.
    try:
        dirty = check_dirty(juneau_dir)
    except subprocess.CalledProcessError as exc:
        print(f"ERROR: could not run `git status` in {juneau_dir}: {exc}", file=sys.stderr)
        return 2
    if dirty:
        print(f"REFUSED: {juneau_dir} is not clean. Installing it now would bake the following", file=sys.stderr)
        print("uncommitted/unreviewed changes into the machine-wide ~/.m2, where every concurrent", file=sys.stderr)
        print("session on this machine would silently pick them up:", file=sys.stderr)
        for line in dirty:
            print(f"  {line}", file=sys.stderr)
        print("", file=sys.stderr)
        print("Commit, stash, or discard the above, then re-run. There is no --allow-dirty escape", file=sys.stderr)
        print("hatch by design -- see this script's module docstring for why.", file=sys.stderr)
        return 1

    try:
        short_hash, subject, unpushed = describe_head(juneau_dir)
    except subprocess.CalledProcessError as exc:
        print(f"ERROR: could not read HEAD in {juneau_dir}: {exc}", file=sys.stderr)
        return 2

    print(f"About to install: {short_hash} {subject}")
    if unpushed is True:
        print("WARNING: HEAD differs from the locally known origin/master -- this commit has NOT")
        print("         been pushed. It will still be installed into ~/.m2 and resolved by every")
        print("         concurrent session on this machine. (No `git fetch` was run, so this may")
        print("         also be stale if origin/master moved since the last fetch.)")
    elif unpushed is None:
        print("NOTE: could not resolve origin/master to compare against HEAD -- skipping that check.")

    java_home = resolve_java_home()
    env = dict(os.environ)
    if java_home:
        env["JAVA_HOME"] = java_home
    print(f"JAVA_HOME={env.get('JAVA_HOME', '(unset -- relying on ambient environment)')}")

    print("\n--- Installing Juneau into ~/.m2 ---")
    returncode = run_streaming(MVN_INSTALL_ARGV, cwd=juneau_dir, env=env)
    if returncode != 0:
        print(f"\nERROR: `{' '.join(MVN_INSTALL_ARGV)}` failed (exit {returncode}).", file=sys.stderr)
        return 3
    print("\nInstall succeeded.")

    consumer_failures: list[str] = []
    if args.verify_consumers:
        print("\n=== Verifying downstream consumers ===")
        if not verify_consumer("release-manager", args.release_manager_dir.expanduser().resolve(), env):
            consumer_failures.append("release-manager")
        if not verify_consumer("sandbox-support-console", args.support_console_dir.expanduser().resolve(), env):
            consumer_failures.append("sandbox-support-console")

    version = read_pom_version(juneau_dir / "pom.xml")
    m2_repo = Path.home() / ".m2" / "repository" / "org" / "apache" / "juneau"

    print("\n=== Summary ===")
    print(f"Installed version: {version or '(could not be determined from pom.xml)'}")
    print(f"Commit:            {short_hash} {subject}")
    print(f"Installed to:      {m2_repo}")
    if args.verify_consumers:
        if consumer_failures:
            print(f"Consumer verify:   FAILED ({', '.join(consumer_failures)})")
        else:
            print("Consumer verify:   PASS (release-manager, sandbox-support-console)")

    if consumer_failures:
        return 4
    return 0


if __name__ == "__main__":
    sys.exit(main())
