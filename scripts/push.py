#!/usr/bin/env python3
#
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
Build and Push Script for Juneau

This script automates the build, test, and deployment workflow:
1. Runs all tests
2. Builds and installs the project
3. Commits changes to Git
4. Pushes to remote repository

Usage: python3 push.py "commit message"
       python3 push.py "commit message" --skip-tests
       python3 push.py "commit message" --sonarqube
       python3 push.py "commit message" --tracker-audit
       python3 push.py "commit message" --docs-only
"""

# Sound file paths
MACOS_SUCCESS_SOUND = "/System/Library/Sounds/Glass.aiff"
MACOS_FAILURE_SOUND = "/System/Library/Sounds/Basso.aiff"
LINUX_SUCCESS_SOUND = "/usr/share/sounds/freedesktop/stereo/complete.oga"
LINUX_FAILURE_SOUND = "/usr/share/sounds/freedesktop/stereo/dialog-error.oga"

import argparse
import os
import platform
import shutil
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path


# ----------------------------------------------------------------------------------------------------------------------
# External Juneau starter repos (TODO-158).
#
# After this script builds + installs the local 10.0.0-SNAPSHOT into ~/.m2, it builds each of these external
# starter repos against that local install to recover the downstream-consumer compile/test guard that
# externalizing the starters would otherwise lose.  Edit this list to match your local checkout locations.
# A path that does not exist is SKIPPED WITH A WARNING (not a failure).  Any PRESENT starter that fails to
# build fails the push.  There is no GitHub Actions CI on these branches; this script is the compile gate.
# Docker image builds are not part of this gate.
# ----------------------------------------------------------------------------------------------------------------------
STARTER_REPO_PATHS = [
    Path.home() / "git" / "apache" / "juneau-microservice-jetty-starter",
    Path.home() / "git" / "apache" / "juneau-microservice-springboot-starter",
    Path.home() / "git" / "apache" / "juneau-microservice-tomcat-starter",
]


def run_command(cmd, description, cwd=None):
    """
    Run a shell command and handle errors.
    
    Args:
        cmd: Command to run (string or list)
        description: Description of the step for output
        cwd: Working directory (defaults to script parent directory)
    
    Returns:
        True if successful, False otherwise
    """
    if cwd is None:
        cwd = Path(__file__).parent.parent
    
    print(f"\n{description}")
    print(f"Running: {' '.join(cmd) if isinstance(cmd, list) else cmd}")
    
    try:
        subprocess.run(
            cmd,
            cwd=cwd,
            shell=isinstance(cmd, str),
            check=True,
            capture_output=False,
            text=True
        )
        print(f"✅ {description} - SUCCESS")
        return True
    except subprocess.CalledProcessError as e:
        print(f"❌ {description} - FAILED (exit code: {e.returncode})")
        return False
    except Exception as e:
        print(f"❌ {description} - FAILED: {e}")
        return False


def play_sound(success=True):  # NOSONAR python:S3776 -- Cognitive complexity is acceptable for this utility function
    """
    Play a system sound to indicate success or failure.
    
    Args:
        success: True for success sound, False for failure sound
    """
    try:
        system = platform.system()
        if system == "Darwin":  # macOS
            if success:
                # Success sound
                sound_path = MACOS_SUCCESS_SOUND
            else:
                # Failure sound
                sound_path = MACOS_FAILURE_SOUND
            
            if os.path.exists(sound_path):
                subprocess.run(
                    ["afplay", sound_path],
                    capture_output=True,
                    timeout=5
                )
        elif system == "Linux":
            # Try to use paplay (PulseAudio) or aplay (ALSA)
            if success:
                # Try to play a beep or use speaker-test
                try:
                    subprocess.run(
                        ["paplay", LINUX_SUCCESS_SOUND],
                        capture_output=True,
                        timeout=5
                    )
                except OSError:
                    # Fallback to speaker-test
                    subprocess.run(
                        ["speaker-test", "-t", "sine", "-f", "1000", "-l", "1"],
                        capture_output=True,
                        timeout=2
                    )
            else:
                try:
                    subprocess.run(
                        ["paplay", LINUX_FAILURE_SOUND],
                        capture_output=True,
                        timeout=5
                    )
                except OSError:
                    # Fallback to speaker-test with lower frequency
                    subprocess.run(
                        ["speaker-test", "-t", "sine", "-f", "400", "-l", "1"],
                        capture_output=True,
                        timeout=2
                    )
        elif system == "Windows":
            # Use winsound module
            import winsound
            if success:
                winsound.MessageBeep(winsound.MB_OK)
            else:
                winsound.MessageBeep(winsound.MB_ICONHAND)
    except Exception:
        # Silently fail if sound can't be played
        pass


def _python310():
    """
    Locate a Python >= 3.10 interpreter, regardless of how push.py itself was launched.

    scripts/sonarqube.py uses 3.10+ syntax (e.g. `dict | None`), so this can't just
    reuse sys.executable when push.py was invoked under an older interpreter (e.g. the
    macOS system /usr/bin/python3, which is 3.9).

    Returns:
        Path to a suitable interpreter (str), or None if none could be found.
    """
    if sys.version_info >= (3, 10):
        return sys.executable

    for name in ("python3.13", "python3.12", "python3.11", "python3.10", "python3"):
        candidate = shutil.which(name)
        if not candidate:
            continue
        try:
            probe = subprocess.run(
                [candidate, "-c", "import sys;print(1 if sys.version_info>=(3,10) else 0)"],
                capture_output=True,
                text=True,
                timeout=5,
                check=False
            )
            if probe.returncode == 0 and probe.stdout.strip() == "1":
                return candidate
        except Exception:
            continue

    fallback = "/opt/homebrew/bin/python3"
    if os.path.exists(fallback):
        return fallback

    return None


def run_sonarqube_gate(juneau_root, step_num):
    """
    Run scripts/sonarqube.py in whole-repo fresh-fetch mode and block the push if
    SonarCloud currently reports ANY issue (all severities) for the branch being
    pushed.

    This is a REPORT gate, not a diff gate: it reflects SonarCloud's last
    CI-analyzed commit for the current branch, not the local working tree — i.e.
    it's a ratchet against that branch's overall issue count. Honors SONAR_TOKEN
    from the environment if set (sonarqube.py reads it directly).

    Branch wiring (TODO-340): the gate resolves the current git branch and passes
    it through as `--branch <current-branch>` so the gate reflects the branch
    actually being pushed rather than always `master`. On `master` (or when the
    branch can't be resolved) it runs exactly as before — no `--branch` argument.
    If SonarCloud has no analysis for the current branch yet (a brand-new branch
    CI hasn't scanned), sonarqube.py exits 3 and this gate degrades to a
    non-blocking warning/skip instead of failing the push.

    Args:
        juneau_root: Repository root (cwd for the subprocess).
        step_num: The current step number (for output formatting).

    Returns:
        One of the status strings:
          "pass"  — SonarCloud reports zero issues for this scope (proceed).
          "fail"  — the gate is blocked by open issues (abort the push).
          "skip"  — no SonarCloud analysis for this branch yet (warn, proceed).
          "error" — could not run the gate (no interpreter / unexpected error);
                    treated as a blocking failure by the caller.
    """
    branch = current_branch(juneau_root)
    scoped_to_branch = branch not in ("master", "unknown")
    scope_desc = f"branch '{branch}'" if scoped_to_branch else "master"

    cmd_suffix = f" --branch {branch}" if scoped_to_branch else ""
    print(f"\n🔎 Step {step_num}: Running SonarQube gate (scripts/sonarqube.py --all --run --fail-on-issues{cmd_suffix})...")
    print(f"   ⚠ Caveat: this reflects SonarCloud's last CI-analyzed commit for {scope_desc}, NOT your local diff (it's a ratchet).")

    py310 = _python310()
    if not py310:
        print(
            "\n❌ Could not find a Python >= 3.10 interpreter to run scripts/sonarqube.py "
            "(requires 3.10+; e.g. the macOS system python3 is too old). Install one "
            "(e.g. `brew install python3`) so it's discoverable as python3.1x/python3 on "
            "PATH, or make sure /opt/homebrew/bin/python3 exists, then retry."
        )
        return "error"

    sonarqube_script = Path(__file__).parent / "sonarqube.py"
    cmd = [py310, str(sonarqube_script), "--all", "--run", "--fail-on-issues"]
    if scoped_to_branch:
        cmd += ["--branch", branch]
    result = subprocess.run(cmd, cwd=juneau_root, check=False)

    # Exit-code contract (see sonarqube.py): 0 clean, 2 issues, 3 no-analysis, else error.
    if result.returncode == 0:
        return "pass"
    if result.returncode == 2:
        return "fail"
    if result.returncode == 3:
        return "skip"
    return "error"


def run_tracker_audit_gate(juneau_root, step_num):
    """
    Run scripts/todo-status-audit.py against this repo's TODO tracker
    (~/Project Work/todos/juneau/) and block the push if it flags any plan file as
    inconsistent — a stale/misplaced status header, or a declared `Counts:` claim
    that no longer re-derives from committed (HEAD) content.

    Unlike the SonarQube gate, this is a pure local check (no network, no external
    service): it re-derives every declared count via `git cat-file --batch` against
    the CURRENT HEAD, so it reflects exactly what's about to be pushed rather than a
    ratchet against a prior CI-analyzed commit.

    Args:
        juneau_root: Repository root (cwd for the subprocess).
        step_num: The current step number (for output formatting).

    Returns:
        One of the status strings:
          "pass"  — no plan file was flagged (proceed).
          "fail"  — at least one plan file was flagged (abort the push).
          "error" — could not run the gate (e.g. the tracker directory doesn't
                    exist, or the script raised unexpectedly); treated as a
                    blocking failure by the caller.
    """
    print(f"\n📋 Step {step_num}: Running tracker audit gate (scripts/todo-status-audit.py)...")

    audit_script = Path(__file__).parent / "todo-status-audit.py"
    result = subprocess.run([sys.executable, str(audit_script)], cwd=juneau_root, check=False)

    # Exit-code contract (see todo-status-audit.py): 0 clean, 1 flagged, 2 hard error
    # (e.g. missing tracker directory), anything else treated as an unexpected error.
    if result.returncode == 0:
        return "pass"
    if result.returncode == 1:
        return "fail"
    return "error"


def maybe_run_tracker_audit_gate(args, juneau_root, step_num):
    """
    Run the tracker-audit gate ONLY when --tracker-audit was passed; otherwise a pure,
    zero-side-effect no-op.

    Extracted as its own function (rather than inlining `if args.tracker_audit: ...`
    directly in main(), as run_sonarqube_gate's call site does) so the off path is
    unit-testable in isolation, without exercising the rest of main()'s
    build/test/commit/push flow: a test can assert this returns None and invokes
    run_tracker_audit_gate zero times when args.tracker_audit is False — the
    strongest available proof that the gate cannot affect push behavior, including
    runtime, when it's off.

    Returns:
        None if the gate is off (nothing ran). Otherwise run_tracker_audit_gate()'s
        status string ("pass" / "fail" / "error").
    """
    if not args.tracker_audit:
        return None
    return run_tracker_audit_gate(juneau_root, step_num)


# Identity guard for --docs-only pushes to the juneau-docs ASF repo. Mirrors the
# same-named constant/function in juneau-docs/scripts/release-docs.py (and
# release-docs-stage.py) so the two repos enforce this check identically.
REQUIRED_GIT_EMAIL = "jamesbognar@apache.org"


def verify_apache_identity(repo_dir):
    """Refuse to proceed unless git is configured with the ASF committer identity."""
    try:
        result = subprocess.run(
            ["git", "config", "--get", "user.email"],
            cwd=repo_dir,
            capture_output=True,
            text=True,
            check=False,
        )
        email = result.stdout.strip()
    except Exception as e:
        print(f"❌ ERROR: Could not read git user.email: {e}")
        return False

    if email != REQUIRED_GIT_EMAIL:
        print("❌ ERROR: Git identity is not the ASF committer identity.")
        print(f"   Found:    user.email = '{email or '(unset)'}'")
        print(f"   Required: user.email = '{REQUIRED_GIT_EMAIL}'")
        print("")
        print("   Fix (this script cannot mutate git config):")
        print(f"     git config user.email {REQUIRED_GIT_EMAIL}")
        print('     git config user.name "James Bognar"')
        return False
    return True


def check_git_status(repo_dir):
    """Check if there are any changes to commit."""
    try:
        result = subprocess.run(
            ["git", "status", "--porcelain"],
            cwd=repo_dir,
            capture_output=True,
            text=True,
            check=True
        )
        return len(result.stdout.strip()) > 0
    except Exception as e:
        print(f"⚠ Warning: Could not check git status: {e}")
        return True


def check_preexisting_staged_changes(repo_dir):
    """
    List paths with changes already staged in the index, before this script's own `git add .`.

    `git commit` commits whatever is in the index regardless of which step staged it, so
    content staged by something else before push.py runs (e.g. a concurrent session sharing
    this working tree) would be folded into this push's commit whether or not `git add .` ran.
    Checking this before add/commit lets the caller refuse rather than silently absorb it.

    Returns:
        list[str]: staged paths (possibly empty). Fails open (returns []) if the check itself
        can't run -- the add/commit steps immediately following will surface any real git
        problem on their own.
    """
    try:
        result = subprocess.run(
            ["git", "diff", "--cached", "--name-only"],
            cwd=repo_dir,
            capture_output=True,
            text=True,
            check=True
        )
        return [line for line in result.stdout.splitlines() if line.strip()]
    except Exception as e:
        print(f"⚠ Warning: Could not check for pre-existing staged changes: {e}")
        return []


def check_upstream_changes(repo_dir):
    """
    Fetch from the remote and compare the local branch to its upstream tracking branch.

    Returns:
        tuple: (ahead, behind, error_message)
        - ahead: commits on the local branch not yet on the upstream branch, or None if there
          is no upstream branch configured
        - behind: commits on the upstream branch not yet on the local branch, or None if there
          is no upstream branch configured
        - error_message: Error message if the check failed, None otherwise

    ahead/behind are None (not 0) when there's no upstream branch configured, so callers can
    tell "nothing to push because there's no remote to compare against" apart from "checked,
    and there's genuinely nothing ahead".
    """
    try:
        # Get current branch name
        result = subprocess.run(
            ["git", "rev-parse", "--abbrev-ref", "HEAD"],
            cwd=repo_dir,
            capture_output=True,
            text=True,
            check=True
        )
        current_branch = result.stdout.strip()
        
        # Fetch latest from remote
        subprocess.run(
            ["git", "fetch"],
            cwd=repo_dir,
            capture_output=True,
            text=True,
            check=True
        )
        
        # Check if there's a remote tracking branch
        result = subprocess.run(
            ["git", "rev-parse", "--abbrev-ref", f"{current_branch}@{{upstream}}"],
            cwd=repo_dir,
            capture_output=True,
            text=True,
            check=False
        )
        
        if result.returncode != 0:
            # No upstream branch configured, skip check
            return (None, None, None)
        
        upstream_branch = result.stdout.strip()
        
        # Get commit counts
        result = subprocess.run(
            ["git", "rev-list", "--left-right", "--count", f"{current_branch}...{upstream_branch}"],
            cwd=repo_dir,
            capture_output=True,
            text=True,
            check=True
        )
        
        # Output format: "X\tY" where X is commits ahead, Y is commits behind
        counts = result.stdout.strip().split('\t')
        if len(counts) != 2:
            return (None, None, "Could not parse upstream comparison")
        
        commits_ahead = int(counts[0])
        commits_behind = int(counts[1])
        return (commits_ahead, commits_behind, None)
        
    except subprocess.CalledProcessError as e:
        return (None, None, f"Git command failed: {e}")
    except Exception as e:
        return (None, None, f"Error checking upstream changes: {e}")


def current_branch(repo_dir):
    """Get current git branch name."""
    try:
        result = subprocess.run(
            ["git", "rev-parse", "--abbrev-ref", "HEAD"],
            cwd=repo_dir,
            capture_output=True,
            text=True,
            check=True
        )
        return result.stdout.strip()
    except Exception:
        return "unknown"


def timing_log_path(repo_dir):
    """Out-of-repo branch-specific timing history location."""
    branch = current_branch(repo_dir).replace("/", "__")
    return Path.home() / ".cache" / "juneau-push-timings" / f"{branch}.jsonl"


def _collect_surefire_stats(juneau_root: Path):
    """Aggregate tests/failures/errors/skipped from all Surefire XML files under juneau-integration-tests."""
    reports = juneau_root / "juneau-integration-tests" / "target" / "surefire-reports"
    totals = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
    found = False
    for xml_path in sorted(reports.rglob("TEST-*.xml")):
        try:
            root = ET.parse(xml_path).getroot()
            for key in totals:
                totals[key] += int(root.attrib.get(key, 0))
            found = True
        except Exception:
            pass
    return (totals["tests"], totals["failures"], totals["errors"], totals["skipped"]) if found else None


def _append_test_run_history(juneau_root: Path, wall_sec: int) -> None:
    """Append one row to juneau-integration-tests/test-run-history.tsv; creates with header if absent. Never raises."""
    try:
        tsv_path = juneau_root / "juneau-integration-tests" / "test-run-history.tsv"
        header = "timestamp\tgit_sha\tbranch\ttests_run\tfailures\terrors\tskipped\twall_sec"
        need_header = not tsv_path.exists() or tsv_path.stat().st_size == 0

        ts = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

        try:
            sha = subprocess.check_output(
                ["git", "rev-parse", "HEAD"], cwd=juneau_root, text=True, stderr=subprocess.DEVNULL
            ).strip()[:12]
        except Exception:
            sha = "?"

        branch = current_branch(juneau_root)

        stats = _collect_surefire_stats(juneau_root)
        if stats is not None:
            tests_run, failures, errors, skipped = stats
        else:
            tests_run = failures = errors = skipped = "?"

        row = "\t".join(str(v) for v in [ts, sha, branch, tests_run, failures, errors, skipped, wall_sec])

        with tsv_path.open("a", encoding="utf-8") as f:
            if need_header:
                f.write(header + "\n")
            f.write(row + "\n")

        print(f"📊 Test metrics appended → juneau-integration-tests/test-run-history.tsv ({tests_run} tests, {wall_sec}s)")
    except Exception as exc:
        print(f"⚠ Warning: Could not append test metrics: {exc}")


def verify_starter_repos(step_num):
    """
    Build each PRESENT external starter repo against the freshly-installed local SNAPSHOT.

    Args:
        step_num: The current step number (for output formatting).

    Returns:
        True if every present starter built successfully (or none are present); False if any
        present starter failed to build.
    """
    print(f"\n🌱 Step {step_num}: Verifying external starter repos against local SNAPSHOT...")
    any_present = False
    for repo in STARTER_REPO_PATHS:
        if not repo.exists():
            print(f"  ⏭️  Skipping missing starter repo: {repo}")
            continue
        any_present = True
        wrapper = repo / ("mvnw.cmd" if platform.system() == "Windows" else "mvnw")
        cmd = [str(wrapper), "-q", "verify"] if wrapper.exists() else ["mvn", "-q", "verify"]
        if not run_command(cmd, f"  Building starter: {repo.name}", cwd=repo):
            print(f"\n❌ Starter repo build FAILED: {repo}")
            return False
    if not any_present:
        print("  ⚠ No starter repos found locally — nothing to verify.")
    return True


def commit_and_push(
    repo_dir,
    message,
    step_num,
    *,
    label="",
    pull_hint="git pull",
    pre_flight_hook=None,
    pre_commit_hook=None,
):
    """
    Commit any local changes and push, treating "clean working tree" and "nothing to push" as
    two different things (the bug this replaces reported success without ever pushing).

    A clean working tree means there is nothing NEW to commit -- it does NOT mean there is
    nothing to push. A prior run could have committed successfully and then been interrupted (or
    failed) before the push step, leaving unpushed commits sitting on the branch; treating a
    clean tree as "nothing to do" in that situation is a false success, since the caller would
    believe their work reached the remote when it did not. So a clean tree here skips the
    COMMIT, not the push: if the branch is already ahead of its upstream, that gets pushed. Only
    a clean tree with nothing already ahead (or no upstream to compare against) is truly a
    no-op, and that is reported with text that is never confusable with an actual push having
    happened.

    Also refuses to stage/commit if the index already has staged changes before this call's own
    `git add .` -- e.g. from a concurrent session sharing this working tree -- since `git commit`
    would fold pre-existing staged content into this push's commit regardless of what `git add .`
    stages in this run. `git add .` itself is deliberately left unconditional for genuinely
    unstaged/untracked changes: that is this script's documented, long-standing behavior (commit
    everything in the tree under one message), and narrowing it further without being asked would
    risk surprising an existing caller just as badly as the bug it would replace.

    Shared by all three sites in this file that make this same commit-or-push decision (the
    default juneau flow, the juneau-docs follow-up in main()'s Step 6, and --docs-only's
    juneau-docs flow) -- this decision is exactly what the false-success bug duplicated three
    times over, so the two hooks below exist to let each site's genuinely different surrounding
    behavior (an identity gate, a pre-commit smoke check) plug into ONE copy of the decision
    logic instead of re-implementing it.

    Args:
        repo_dir: Repository root.
        message: Commit message to use if a commit is made.
        step_num: The current step number (for output formatting).
        label: Optional text identifying which repo this is, appended to output messages (e.g.
            " (juneau-docs)") so a caller running this against more than one repo per invocation
            can tell them apart in the log. Empty by default.
        pull_hint: The `git pull` command shown in the behind-upstream error message. Callers
            operating outside the repo's own directory (e.g. main() running against the
            juneau-docs sibling while cwd is juneau) can pass a `-C <path>`-qualified form.
        pre_flight_hook: Optional zero-arg callable invoked once it's established that there is
            something to commit and/or push (i.e. NOT the nothing_to_do case), before any git
            state is mutated. Must return True to proceed or False to abort (and is responsible
            for printing its own error and playing the failure sound before returning False).
            Used for checks that must run before ANY push, including a push-only run with
            nothing new to commit -- e.g. the juneau-docs Apache-identity gate in main()'s Step 6
            follow-up. None (the default) means no such check is needed.
        pre_commit_hook: Optional zero-arg callable invoked only when there ARE local changes to
            commit, immediately before `git add .`. Same True/False contract as pre_flight_hook.
            Used for checks that have nothing new to validate when nothing changed -- e.g. the
            juneau-docs Docusaurus smoke build, which only needs to run before a commit that
            actually contains new docs content. None (the default) means no such check is needed.

    Returns:
        tuple: (status, step_num)
        - status: "ok" (proceed with the rest of the flow), "nothing_to_do" (nothing was
          committed or pushed -- caller should exit 0 immediately, matching the pre-existing
          no-op shortcut), or "error" (caller should exit 1 immediately).
        - step_num: the input step_num, advanced past whichever of the commit/push steps
          actually ran (unchanged for "nothing_to_do"/"error").
    """
    print(f"\n🔍 Checking for upstream changes{label}...")
    commits_ahead, commits_behind, error_msg = check_upstream_changes(repo_dir)
    if error_msg:
        print(f"\n⚠ Warning: Could not check upstream changes{label}: {error_msg}")
        print("Continuing anyway...")
    elif commits_behind:
        print(f"\n❌ ERROR: Local branch{label} is behind upstream/remote branch.")
        print("Please pull/merge upstream changes before pushing.")
        print(f"Run: {pull_hint}")
        play_sound(success=False)
        return ("error", step_num)

    # A clean tree skips the commit, not the push -- see docstring above.
    has_changes = check_git_status(repo_dir)
    if not has_changes and not commits_ahead:
        if error_msg:
            print(f"\nℹ Nothing to commit locally{label}, and the upstream comparison could not "
                  "be verified (see warning above) -- assuming nothing to push.")
        else:
            print(f"\nℹ Nothing to commit and no unpushed commits{label}.")
        print(f"🎉 Push{label} completed successfully (nothing to commit or push)!")
        play_sound(success=True)
        return ("nothing_to_do", step_num)

    if pre_flight_hook is not None and not pre_flight_hook():
        return ("error", step_num)

    if has_changes:
        preexisting_staged = check_preexisting_staged_changes(repo_dir)
        if preexisting_staged:
            print(f"\n❌ ERROR: The Git index already has staged changes{label} from before this run:")
            for path in preexisting_staged:
                print(f"   {path}")
            print("   Refusing to stage/commit over them -- they may belong to a concurrent")
            print("   session sharing this working tree. Review, commit, or unstage them first,")
            print("   then re-run push.py.")
            play_sound(success=False)
            return ("error", step_num)

        if pre_commit_hook is not None and not pre_commit_hook():
            return ("error", step_num)

        # Step N: Git add and commit
        print(f"\n📝 Step {step_num}: Committing changes to Git{label}...")
        if not run_command(
            ["git", "add", "."],
            f"  {step_num}.1: Staging all changes...",
            repo_dir
        ):
            print(f"\n❌ git add failed{label} -- aborting.")
            play_sound(success=False)
            return ("error", step_num)

        if not run_command(
            ["git", "commit", "-m", message],
            f"  {step_num}.2: Creating commit...",
            repo_dir
        ):
            print(f"\n❌ git commit failed{label} -- aborting.")
            play_sound(success=False)
            return ("error", step_num)
        print(f"✅ Step {step_num}: Git commit completed{label}.")
        step_num += 1
    else:
        print(f"\nℹ No new changes to commit{label} -- {commits_ahead} unpushed commit(s) "
              "already on this branch will be pushed now.")

    # Step N (or N+1): Push to remote
    if not run_command(
        ["git", "push"],
        f"🚀 Step {step_num}: Pushing changes to remote repository{label}...",
        repo_dir
    ):
        print(f"\n❌ git push failed{label}.")
        print(f"⚠ Local commits exist but were not pushed{label}.")
        play_sound(success=False)
        return ("error", step_num)
    print(f"✅ Step {step_num}: Changes pushed to remote{label}.")
    step_num += 1

    return ("ok", step_num)


def run_docs_only(args, juneau_root):  # NOSONAR python:S3776 -- Cognitive complexity is acceptable for this function
    """
    --docs-only mode: operate ONLY on the sibling juneau-docs repo.

    Skips the entire juneau code path (no container-tags/BOM checks, no test run, no
    mvn build/install, no starter-repo verification, no juneau commit/push). Runs the
    same Apache-identity gate and docs-verification gate (Docusaurus smoke build via
    build-docs.py --skip-maven, which runs verify-docs.py internally) that the default
    flow's Step 6 juneau-docs follow-up already runs, then commits and pushes
    juneau-docs — as the ONLY step, rather than a follow-up to a juneau push.

    Args:
        args: Parsed CLI arguments (message, dry_run, etc).
        juneau_root: The juneau repo root (used only to locate the juneau-docs sibling).

    Returns:
        Process exit code (0 success, 1 failure).
    """
    docs_root = juneau_root.parent / "juneau-docs"

    print("=" * 70)
    print("🚀 Juneau Docs-Only Push Script")
    print("=" * 70)
    print(f"Docs directory: {docs_root}")
    print(f"Commit message: '{args.message}'")
    print("📚 DOCS-ONLY MODE (--docs-only) — skipping juneau Java build/test/push entirely.")
    if args.skip_tests or args.sonarqube:
        print("⚠ Note: --skip-tests/--sonarqube only apply to the juneau code path, which "
              "--docs-only skips entirely; ignoring them.")
    if args.dry_run:
        print("🔍 DRY RUN MODE - No actual changes will be made")
    print("=" * 70)

    if not docs_root.exists():
        print(f"\n❌ ERROR: juneau-docs repo not found at {docs_root}")
        print("   Expected it as a sibling of the juneau checkout.")
        play_sound(success=False)
        return 1

    if args.dry_run:
        print("\nSteps that would be executed:")
        print(f"  1. Verify Apache git identity on juneau-docs (user.email == {REQUIRED_GIT_EMAIL})")
        print("  2. Check for upstream changes on juneau-docs (git fetch + compare to upstream)")
        print("  3. Check juneau-docs git status (exit 0 with a no-changes message if clean)")
        print("  4. Run docs verification gate: python3 scripts/build-docs.py --skip-maven (runs verify-docs.py)")
        print(f"  5. Commit changes: git add . && git commit -m \"{args.message}\"  (in juneau-docs)")
        print("  6. Push to remote: git push  (in juneau-docs)")
        print("\nDry run complete. Use without --dry-run to execute.")
        return 0

    # Step 1: Apache identity gate — must hold before any work begins.
    print("\n🔐 Step 1: Verifying git identity (apache.org email) on juneau-docs...")
    if not verify_apache_identity(docs_root):
        play_sound(success=False)
        return 1
    print("✅ Step 1: Git identity verified")

    # Steps 2-6: upstream check, no-op detection, docs smoke check, commit, and push -- all
    # delegated to commit_and_push() (shared with main()'s Step 6 follow-up) so the clean-tree-
    # vs-nothing-to-push fix and the pre-existing-staged-changes guard live in one place. The
    # Docusaurus smoke build only has anything new to validate when there IS a commit about to
    # be made, so it's wired in as commit_and_push()'s pre_commit_hook rather than running
    # unconditionally.
    def _docs_smoke_check():
        print("\n📚 juneau-docs has changes — running Docusaurus smoke check first...")
        docs_build_script = docs_root / "scripts" / "build-docs.py"
        try:
            result = subprocess.run(
                [sys.executable, str(docs_build_script), "--skip-maven"],
                cwd=docs_root,
                check=False
            )
            if result.returncode != 0:
                print("\n❌ Docs smoke check failed — fix the Docusaurus build before pushing juneau-docs.")
                play_sound(success=False)
                return False
            print("✅ Docs smoke check passed")
            return True
        except Exception as e:
            print(f"\n❌ Docs smoke check failed: {e}")
            play_sound(success=False)
            return False

    status, _ = commit_and_push(
        docs_root, args.message, 5,
        label=" (juneau-docs)",
        pull_hint="git -C ../juneau-docs pull",
        pre_commit_hook=_docs_smoke_check,
    )
    if status == "error":
        return 1
    if status == "nothing_to_do":
        return 0

    print("\n" + "=" * 70)
    print("🎉 Docs-only push completed successfully!")
    print(f"📦 Commit message: '{args.message}'")
    print("=" * 70)
    play_sound(success=True)
    return 0


def main():  # NOSONAR python:S3776 -- Cognitive complexity is acceptable for this main function
    parser = argparse.ArgumentParser(
        description="Build, test, and push Juneau project to Git repository",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python3 push.py "Fixed bug in RestClient"
  python3 push.py "Updated documentation" --skip-tests
  python3 push.py "Quick fix" --skip-tests
  python3 push.py "Fixed bug in RestClient" --sonarqube
  python3 push.py "Fixed bug in RestClient" --tracker-audit
  python3 push.py "Updated topic page" --docs-only
        """
    )
    
    parser.add_argument(
        "message",
        help="Git commit message"
    )
    
    parser.add_argument(
        "--skip-tests",
        action="store_true",
        help="Skip running tests (useful for documentation-only changes)"
    )
    
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Show what would be done without actually doing it"
    )

    parser.add_argument(
        "--docs-only",
        action="store_true",
        help=(
            "Operate ONLY on the sibling juneau-docs repo: skip the entire juneau code "
            "path (no container-tags/BOM checks, no tests, no mvn build/install, no "
            "starter-repo verification, no juneau commit/push). Still enforces the "
            "Apache identity gate and the docs verification gate (Docusaurus smoke "
            "build via build-docs.py --skip-maven, which runs verify-docs.py) against "
            "juneau-docs before committing/pushing it. Exits 0 with a no-op message if "
            "juneau-docs has no changes."
        )
    )

    parser.add_argument(
        "--sonarqube", "--sonar",
        action="store_true",
        dest="sonarqube",
        help=(
            "Opt-in gate: run scripts/sonarqube.py in whole-repo fresh-fetch mode and "
            "block the push if SonarCloud currently reports any issue (all severities). "
            "Reflects SonarCloud's last CI-analyzed commit, not the local diff (a ratchet)."
        )
    )

    parser.add_argument(
        "--tracker-audit", "--todo-audit",
        action="store_true",
        dest="tracker_audit",
        help=(
            "Opt-in gate: run scripts/todo-status-audit.py against ~/Project Work/todos/juneau/ "
            "and block the push if it flags any plan file (stale/misplaced status header, or a "
            "declared Counts: claim that no longer re-derives from committed HEAD content)."
        )
    )
    
    args = parser.parse_args()
    
    # Get the Juneau root directory
    script_dir = Path(__file__).parent
    juneau_root = script_dir.parent

    # --docs-only short-circuits into its own self-contained flow (juneau-docs only);
    # everything below this is the unchanged default (full juneau build+push) flow.
    if args.docs_only:
        return run_docs_only(args, juneau_root)

    print("=" * 70)
    print("🚀 Juneau Build and Push Script")
    print("=" * 70)
    print(f"Working directory: {juneau_root}")
    print(f"Commit message: '{args.message}'")
    if args.skip_tests:
        print("⚠ Tests will be SKIPPED")
    if args.sonarqube:
        print("🔎 SonarQube gate ENABLED (--sonarqube)")
    if args.tracker_audit:
        print("📋 Tracker audit gate ENABLED (--tracker-audit)")
    if args.dry_run:
        print("🔍 DRY RUN MODE - No actual changes will be made")
    print("=" * 70)
    
    if args.dry_run:
        print("\nSteps that would be executed:")
        step_num = 1
        if args.sonarqube:
            _branch = current_branch(juneau_root)
            _branch_arg = f" --branch {_branch}" if _branch not in ("master", "unknown") else ""
            print(f"  {step_num}. Run SonarQube gate: python3 scripts/sonarqube.py --all --run --fail-on-issues{_branch_arg} (blocks push if any issues; skips gracefully if this branch has no SonarCloud analysis yet)")
            step_num += 1
        if args.tracker_audit:
            print(f"  {step_num}. Run tracker audit gate: python3 scripts/todo-status-audit.py (blocks push if any plan file is flagged)")
            step_num += 1
        print(f"  {step_num}. Prompt for PGP passphrase (dummy call)")
        step_num += 1
        if not args.skip_tests:
            print(f"  {step_num}. Verify container test tags: python3 scripts/check-container-tags.py")
            step_num += 1
            print(f"  {step_num}. Verify BOM completeness: python3 scripts/check-bom-completeness.py")
            step_num += 1
            print(f"  {step_num}. Run tests with timing capture: python3 scripts/test.py --full --timing-log ~/.cache/juneau-push-timings/<branch>.jsonl")
            step_num += 1
            print(f"  {step_num}. Print timing deltas: python3 scripts/push-timings.py --log ~/.cache/juneau-push-timings/<branch>.jsonl")
            step_num += 1
        print(f"  {step_num}. Build and install: mvn clean package install -DskipTests")
        step_num += 1
        print(f"  {step_num}. Verify external starter repos against local SNAPSHOT (./mvnw -q verify; missing paths skipped)")
        step_num += 1
        print(f"  {step_num}. Commit changes: git add . && git commit -m \"{args.message}\"")
        step_num += 1
        print(f"  {step_num}. Push to remote: git push")
        print("\nDry run complete. Use without --dry-run to execute.")
        return 0

    step_num = 1

    # Identity gate — must hold before the expensive build/test gate and any commit/push.
    # Extended (maintainer-approved) from the --docs-only path to the default flow too;
    # juneau-docs gets its own check further down, right before its Step 6 commit/push,
    # since the two repos can have independent git config user.email.
    print("\n🔐 Verifying git identity (apache.org email) on juneau...")
    if not verify_apache_identity(juneau_root):
        play_sound(success=False)
        return 1
    print("✅ Git identity verified")

    # Step 0 (opt-in, --sonarqube/--sonar): SonarQube report gate. Runs first so it
    # aborts cheaply, before the container-tags/BOM checks, tests, and build.
    if args.sonarqube:
        gate_status = run_sonarqube_gate(juneau_root, step_num)
        if gate_status == "fail":
            print("\n❌ Push aborted: SonarCloud currently reports open issues for this branch.")
            print("   Note: this reflects SonarCloud's last CI-analyzed commit (a ratchet), not your local diff.")
            print("   Resolve/triage the reported issues, or omit --sonarqube to push without this gate.")
            play_sound(success=False)
            return 1
        if gate_status == "error":
            print("\n❌ Push aborted: the SonarQube gate could not be run.")
            print("   Fix the issue reported above (e.g. install a Python >= 3.10 interpreter), "
                  "or omit --sonarqube to push without this gate.")
            play_sound(success=False)
            return 1
        if gate_status == "skip":
            print(f"⚠ Step {step_num}: SonarQube gate skipped — SonarCloud has no analysis for this "
                  "branch yet (a new branch CI hasn't scanned). Continuing without the Sonar gate.")
        else:  # "pass"
            print(f"✅ Step {step_num}: SonarQube gate passed — SonarCloud reports zero issues for this branch.")
        step_num += 1

    # Step 0b (opt-in, --tracker-audit/--todo-audit): TODO tracker audit gate. Also runs
    # before the container-tags/BOM checks, tests, and build, so it aborts cheaply.
    tracker_gate_status = maybe_run_tracker_audit_gate(args, juneau_root, step_num)
    if tracker_gate_status is not None:
        if tracker_gate_status == "fail":
            print("\n❌ Push aborted: the tracker audit flagged at least one plan file "
                  "under ~/Project Work/todos/juneau/.")
            print("   Run `python3 scripts/todo-status-audit.py` for details, or omit "
                  "--tracker-audit to push without this gate.")
            play_sound(success=False)
            return 1
        if tracker_gate_status == "error":
            print("\n❌ Push aborted: the tracker audit gate could not be run.")
            print("   Fix the issue reported above, or omit --tracker-audit to push without this gate.")
            play_sound(success=False)
            return 1
        print(f"✅ Step {step_num}: Tracker audit gate passed — no plan file flagged.")
        step_num += 1

    # Prompt for PGP passphrase early (before any time-consuming operations)
    prompt_script = script_dir / 'prompt-pgp-passphrase.py'
    if prompt_script.exists():
        try:
            subprocess.run(
                [sys.executable, str(prompt_script)],
                check=False  # Don't fail if this doesn't work
            )
        except Exception as e:
            print(f"⚠ Could not run PGP passphrase prompt: {e}")
    
    # Step 1: Run tests (optional)
    if not args.skip_tests:
        check_container_tags = script_dir / "check-container-tags.py"
        if check_container_tags.exists():
            if not run_command(
                [sys.executable, str(check_container_tags)],
                f"🔎 Step {step_num}: Checking container test tags...",
                juneau_root
            ):
                print("\n❌ Build process aborted due to missing container test tags.")
                play_sound(success=False)
                return 1
            step_num += 1

        check_bom = script_dir / "check-bom-completeness.py"
        if check_bom.exists():
            if not run_command(
                [sys.executable, str(check_bom)],
                f"🔎 Step {step_num}: Checking BOM completeness...",
                juneau_root
            ):
                print("\n❌ Build process aborted: juneau-bom is out of sync with the reactor.")
                play_sound(success=False)
                return 1
            step_num += 1

        test_script = script_dir / 'test.py'
        timing_file = timing_log_path(juneau_root)
        if test_script.exists():
            print(f"\n🧪 Step {step_num}: Running tests via test.py...")
            try:
                _test_start = time.time()
                result = subprocess.run(
                    [sys.executable, str(test_script), "--full", "--timing-log", str(timing_file)],
                    cwd=juneau_root,
                    check=False
                )
                _test_wall_sec = int(time.time() - _test_start)
                if result.returncode != 0:
                    print("\n❌ Build process aborted due to test failures.")
                    play_sound(success=False)
                    return 1
                print(f"✅ Step {step_num}: Tests passed")
                _append_test_run_history(juneau_root, _test_wall_sec)
            except Exception as e:
                print(f"\n❌ Error running tests: {e}")
                play_sound(success=False)
                return 1
        else:
            # Fallback to direct mvn test if test.py doesn't exist
            _test_start = time.time()
            _mvn_ok = run_command(
                ["mvn", "test"],
                f"🧪 Step {step_num}: Running tests...",
                juneau_root
            )
            _test_wall_sec = int(time.time() - _test_start)
            if not _mvn_ok:
                print("\n❌ Build process aborted due to test failures.")
                play_sound(success=False)
                return 1
            _append_test_run_history(juneau_root, _test_wall_sec)
        timing_report = script_dir / "push-timings.py"
        if timing_report.exists():
            run_command(
                [sys.executable, str(timing_report), "--log", str(timing_file)],
                f"📊 Step {step_num}: Timing regression report...",
                juneau_root
            )
        step_num += 1
    else:
        print(f"\n⏭️  Step {step_num}: Skipping tests (--skip-tests flag)")
        step_num += 1
    
    # Step 2: Build and install (skip tests - already run in Step 1)
    if not run_command(
        ["mvn", "clean", "package", "install", "-DskipTests"],
        f"🏗️  Step {step_num}: Building and installing project...",
        juneau_root
    ):
        print("\n❌ Build process aborted due to build failure.")
        play_sound(success=False)
        return 1
    step_num += 1

    # Step 3 (TODO-158): Build external starter repos against the freshly-installed local SNAPSHOT (blocking gate)
    if not verify_starter_repos(step_num):
        print("\n❌ Build process aborted due to external starter repo verification failure.")
        play_sound(success=False)
        return 1
    step_num += 1
    
    # Step 4/5: Commit (if there's anything new) and push (if there's anything ahead) --
    # extracted into commit_and_push() so the "clean tree != nothing to push" fix and the
    # pre-existing-staged-changes guard are unit-testable against real git without invoking the
    # mvn build/test steps above.
    push_status, step_num = commit_and_push(juneau_root, args.message, step_num)
    if push_status == "error":
        return 1
    if push_status == "nothing_to_do":
        return 0

    # Step 6 (optional): juneau-docs follow-up — smoke check + commit + push. Guarded only by
    # docs_root existing (NOT by check_git_status(docs_root) as before): a clean juneau-docs
    # tree can still have unpushed commits from an earlier interrupted run, and this follow-up
    # has to run for that case too, or it silently never pushes them (the same false-success
    # shape as the defect commit_and_push() exists to fix). Delegated to the same
    # commit_and_push() as the default juneau flow and --docs-only, via two hooks: the identity
    # gate must run before ANY push (including a push-only run with no new commit), and the
    # smoke check only has anything to validate when there IS a commit about to be made.
    docs_root = juneau_root.parent / "juneau-docs"
    if docs_root.exists():
        def _verify_docs_identity():
            print("\n🔐 Verifying git identity (apache.org email) on juneau-docs...")
            if not verify_apache_identity(docs_root):
                play_sound(success=False)
                return False
            print("✅ Git identity verified")
            return True

        def _docs_smoke_check():
            print(f"\n📚 juneau-docs has changes — running Docusaurus smoke check first...")
            docs_build_script = docs_root / "scripts" / "build-docs.py"
            docs_smoke_start = time.time()
            try:
                result = subprocess.run(
                    [sys.executable, str(docs_build_script), "--skip-maven"],
                    cwd=docs_root,
                    check=False
                )
                docs_smoke_elapsed = time.time() - docs_smoke_start
                if result.returncode != 0:
                    print("\n❌ Docs smoke check failed — fix the Docusaurus build before pushing juneau-docs.")
                    play_sound(success=False)
                    return False
                print(f"✅ Docs smoke check passed ({docs_smoke_elapsed:.1f}s)")
                return True
            except Exception as e:
                print(f"\n❌ Docs smoke check failed: {e}")
                play_sound(success=False)
                return False

        docs_status, step_num = commit_and_push(
            docs_root, args.message, step_num,
            label=" (juneau-docs)",
            pull_hint="git -C ../juneau-docs pull",
            pre_flight_hook=_verify_docs_identity,
            pre_commit_hook=_docs_smoke_check,
        )
        if docs_status == "error":
            return 1
        # "nothing_to_do" here just means juneau-docs itself had nothing going on -- the juneau
        # push above already succeeded, so fall through to the overall success block below.

    # Success!
    print("\n" + "=" * 70)
    print("🎉 All operations completed successfully!")
    print(f"📦 Commit message: '{args.message}'")
    print("=" * 70)
    play_sound(success=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())

