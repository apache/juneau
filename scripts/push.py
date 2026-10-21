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
0. Prechecks: refuses up front unless git has the ASF committer identity, the tree has no unreviewed
   (unstaged/untracked) changes and the branch isn't behind its upstream -- for juneau and, when it exists, the
   sibling juneau-docs -- before any build or test work (commit_and_push() repeats the last two at the end)
1. Runs all tests
2. Builds and installs the project
3. Commits changes to Git
4. Pushes to remote repository

Usage: python3 push.py "commit message"
       python3 push.py "commit message" --skip-tests
       python3 push.py "commit message" --sonarqube
       python3 push.py "commit message" --tracker-audit
       python3 push.py "commit message" --docs-only
       python3 push.py "commit message" --js-tests      (force the headless-browser JS harness)
       python3 push.py "commit message" --no-js-tests   (never run it)
       python3 push.py --test-only                      (build and test gates only; no commit or push)

JS harness (-Pjs-tests): with neither flag, test.py runs it automatically when a .js/.css/.ftl file
under a src/ tree differs from origin/master, and skips it with a notice if Node/npm are missing.
"""

import argparse
import contextlib
import importlib.util
import os
import platform
import re
import shlex
import shutil
import signal
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path

# The shared loader (_juneau.py) sits beside this script; appended, so no script here shadows an installed module.
if str(Path(__file__).resolve().parent) not in sys.path:
    sys.path.append(str(Path(__file__).resolve().parent))
import _juneau  # noqa: E402


def toolkit(name):
    """A juneau_scripts module (git, run, notify, prompt, proc) from the in-tree toolkit."""
    _juneau.load()
    return importlib.import_module(f"juneau_scripts.{name}")


def say(text="", level="info"):
    """Status text: printed as before, or a filtered note when a console view owns the screen."""
    _juneau.load()[0].say(text, level=level)


def console_active():
    """True when a juneau_run console view draws the terminal."""
    return _juneau.load()[0].console_active()


def run_gate(cmd, cwd, step_id, title, label):
    """
    A gate script's exit code.  Under a console view it runs through run_tool as a row, with its output in the full
    log; otherwise it runs on the terminal as before.  The caller interprets the code (each gate has its own
    exit-code contract).
    """
    run = _juneau.load()[0]
    if not run.console_active():
        return subprocess.run(cmd, cwd=cwd, check=False).returncode
    return run.run_tool(cmd, "generic", step_id, title, n=next_step_number(), cwd=cwd,  # NOSONAR python:S1192 - a parser name, as in run_command
                        label=label).exit


def aborted(text, *hint):
    """
    Stop the push after a failed row: text and the hint lines as error lines, or under a console view (where the
    failed row already says what stopped) only the hint lines, as a hint.  Returns the exit code, 1.
    """
    run = _juneau.load()[0]
    if run.console_active():
        if hint:
            run.hint(list(hint))
    else:
        say(text, "error")
        for line in hint:
            say(line, "error")
    return 1


def next_step_number(count=1):
    """The next top-level step number, from the toolkit's session counter (the one run.step, run.gate and
    run.handoff use); count > 1 reserves a block."""
    return toolkit("run")._next_n(count=count)


@contextlib.contextmanager
def marked_step(step_id, title, label=None):
    """A run-protocol step around a block (run.step): silent unless RUN_MARKERS=1 or a console view is active."""
    with toolkit("run").step(step_id, title, label=label) as step:
        yield step


def run_marked(step_id, title, cmd, description, cwd=None, tool=None, label=None, summarize=None):
    """
    run_command() as the protocol step `step_id`.  With `tool` ("maven", "generic", ...) and markers on, or with a
    console view active, juneau_run runs the command and reports its progress; otherwise the call is just wrapped
    in a step.  summarize(output) becomes the row's text under a console view.
    """
    run = _juneau.load()[0]
    if (tool is not None and run.enabled()) or run.console_active():
        return run_command(cmd, description, cwd, tool=tool, step_id=step_id, title=title, n=next_step_number(),
                           label=label, summarize=summarize)
    with marked_step(step_id, title) as step:
        ok = run_command(cmd, description, cwd, label=label)
        if not ok:
            step.fail()
        return ok


def run_command(cmd, description, cwd=None, tool=None, step_id=None, title=None, n=None, parent=None, *,
                label=None, capture=False, tty=False, summarize=None):
    """
    Run a shell command and handle errors.

    Args:
        cmd: Command to run (string or list)
        description: Description of the step for output
        cwd: Working directory (defaults to script parent directory)
        tool: juneau_run parser name; with it (and a list cmd) the command runs as the protocol step `step_id`
            when markers are on
        step_id, title, n, parent, label: the step fields.  With a console view active, a list cmd with a
            step_id also runs through run_tool (tool defaults to "generic"), so its output stays off the screen
        capture, tty, summarize: passed to run_tool

    Returns:
        True if successful, False otherwise
    """
    if cwd is None:
        cwd = Path(__file__).parent.parent

    run = _juneau.load()[0]
    console = run.console_active()
    say(description.strip() if console else f"\n{description}")
    say(f"Running: {' '.join(cmd) if isinstance(cmd, list) else cmd}")

    try:
        # Only with markers on (and a tool) or a console view does run_tool take over; otherwise the original call
        # below runs unchanged.
        routed = isinstance(cmd, list) and (
            (tool is not None and run.enabled()) or (step_id is not None and console))
        if routed:
            result = run.run_tool(cmd, tool or "generic", step_id, title or description.strip(), n=n, parent=parent,
                                  cwd=cwd, capture=capture, tty=tty, summarize=summarize, label=label)
            if result.exit != 0:
                raise subprocess.CalledProcessError(result.exit, cmd)
        else:
            subprocess.run(
                cmd,
                cwd=cwd,
                shell=isinstance(cmd, str),
                check=True,
                capture_output=False,
                text=True
            )
        say(f"✅ {description} - SUCCESS")
        return True
    except subprocess.CalledProcessError as e:
        say(f"❌ {description} - FAILED (exit code: {e.returncode})", "error")
        return False
    except Exception as e:
        say(f"❌ {description} - FAILED: {e}", "error")
        return False


#: Set to exactly "1" to silence the end-of-run sound and the terminal bell (also honored by test.py).
PUSH_NO_SOUND_ENV = "JUNEAU_PUSH_NO_SOUND"


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
    say(f"\n🔎 Step {step_num}: Running SonarQube gate (scripts/sonarqube.py --all --run --fail-on-issues{cmd_suffix})...")
    say(f"   ⚠ Caveat: this reflects SonarCloud's last CI-analyzed commit for {scope_desc}, NOT your local diff (it's a ratchet).", "warn")

    py310 = _python310()
    if not py310:
        say(
            "\n❌ Could not find a Python >= 3.10 interpreter to run scripts/sonarqube.py "
            "(requires 3.10+; e.g. the macOS system python3 is too old). Install one "
            "(e.g. `brew install python3`) so it's discoverable as python3.1x/python3 on "
            "PATH, or make sure /opt/homebrew/bin/python3 exists, then retry.",
            "error",
        )
        return "error"

    sonarqube_script = Path(__file__).parent / "sonarqube.py"
    cmd = [py310, str(sonarqube_script), "--all", "--run", "--fail-on-issues"]
    if scoped_to_branch:
        cmd += ["--branch", branch]
    # Exit-code contract (see sonarqube.py): 0 clean, 2 issues, 3 no-analysis, else error -- the toolkit's default
    # gate codes.
    return toolkit("run").gate(cmd, title="SonarQube gate", id="sonar", label="Sonar", cwd=juneau_root).status


def run_tracker_audit_gate(juneau_root, step_num):
    """
    Run ~/Project Work/scripts/todo-status-audit.py --project juneau against this repo's
    TODO tracker (~/Project Work/todos/juneau/) and block the push if it flags any plan
    file as inconsistent — a stale/misplaced status header, or a declared `Counts:` claim
    that no longer re-derives from committed (HEAD) content.

    Unlike the SonarQube gate, this is a pure local check (no network, no external
    service): it re-derives every declared count via `git cat-file --batch` against
    the CURRENT HEAD, so it reflects exactly what's about to be pushed rather than a
    ratchet against a prior CI-analyzed commit.

    The audit script and the tracker it scans both live outside this repo (consolidated
    2026-08-30 into ~/Project Work/scripts/, parameterized by --project instead of one
    copy per repo) — so the script path and the scanned directory are both passed
    explicitly here rather than relying on `cwd=juneau_root` to make them resolve.

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
    say(f"\n📋 Step {step_num}: Running tracker audit gate (~/Project Work/scripts/todo-status-audit.py --project juneau)...")

    audit_script = Path.home() / "Project Work" / "scripts" / "todo-status-audit.py"
    todo_dir = Path.home() / "Project Work" / "todos" / "juneau"
    # Exit-code contract (see todo-status-audit.py): 0 clean, 1 flagged, 2 hard error
    # (e.g. missing tracker directory), anything else treated as an unexpected error.
    return toolkit("run").gate(
        [sys.executable, str(audit_script), "--project", "juneau", "--dir", str(todo_dir)],
        title="Tracker audit", id="tracker", label="Tracker", cwd=juneau_root, codes={0: "pass", 1: "fail"},
    ).status


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

#: How the hints reach the juneau-docs sibling from the juneau checkout.
DOCS_GIT = "git -C ../juneau-docs"


def resolve_commit_message(cli_message):
    """Use the CLI-supplied message, or prompt for one; abort if still empty."""
    if cli_message:
        return cli_message

    try:
        message = toolkit("prompt").text("Commit message: ", default="")
    except KeyboardInterrupt:
        message = None
    if message is None:
        print("\n❌ Aborted: no commit message provided.")
        sys.exit(1)

    if not message:
        print("❌ Aborted: commit message cannot be empty.")
        sys.exit(1)

    return message


def get_staged_paths(repo_dir):
    """
    List paths currently staged in the index.

    This is the sole definition of "what push.py is about to commit": a staged path is the
    operator's record that they reviewed it and it's good (see commit_and_push()'s docstring),
    so this script commits the index exactly as it finds it rather than deciding what belongs
    in the commit itself.

    Returns:
        list[str]: staged paths (possibly empty).

    Raises:
        GitError: if the check itself can't run (or the directory isn't a repository). Deliberately not
        caught here -- unlike the ahead/behind upstream check (a network operation that can
        legitimately fail), an error running `git diff --cached` almost always means something
        is fundamentally wrong with the repo, and this check exists specifically to protect an
        invariant, so the caller should fail closed (abort) rather than silently proceed.
    """
    return toolkit("git").staged_paths(repo_dir)


def check_unreviewed_changes(repo_dir):
    """
    List paths that have NOT been staged: tracked files with unstaged modifications/deletions,
    plus untracked files that are not gitignored.

    Staging is how this script knows something has been reviewed (see commit_and_push()'s
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
        GitError: see get_staged_paths() -- same fail-closed reasoning applies here.
    """
    git = toolkit("git")
    return [*git.unstaged_paths(repo_dir), *git.untracked_paths(repo_dir)]


def report_unreviewed(unreviewed_paths, label, outcome, lead="ERROR"):
    """
    Print the unreviewed-changes refusal: the paths, why they matter, `outcome` (what was and wasn't done) and the
    stash hint.  Shared by the pre-flight and by commit_and_push()'s late check so the two read alike.
    """
    say(f"\n❌ {lead}: The working tree{label} has unreviewed changes -- staging is how "
        "this script knows something has been reviewed, and these paths are not staged:", "error")
    for path in unreviewed_paths:
        say(f"   {path}", "error")
    say("   Why: everything this pushes should be something you've read, not just", "error")
    say("   something that happened to be in the tree.", "error")
    say(f"\n   {outcome} Stage each path above if you've reviewed", "error")
    say("   it, or set it aside for this run and restore it after:", "error")
    quoted_paths = " ".join(shlex.quote(path) for path in unreviewed_paths)
    say(f"     git stash push -- {quoted_paths}", "error")
    say("     git stash pop", "error")


def prechecks(juneau_root, *, dry_run=False, docs=True):
    """
    The Prechecks row's checks, which refuse before any build or test gate a push that commit_and_push() would
    only refuse at the end: git's user.email isn't the ASF committer identity, the tree has unreviewed changes, or
    the branch is behind its upstream (no upstream configured passes; a failed fetch or git error fails closed).
    With docs and the juneau-docs sibling present and active -- something staged, or commits ahead of its upstream
    (docs_active) -- the same three for juneau-docs follow (docs_prechecks); an idle juneau-docs is not checked, so
    its dirt doesn't block a juneau push.  The late checks stay (upstream can move during the long build); these
    only save the wait.
    """
    checks = toolkit("checks")
    outcome = "Nothing would be built, committed or pushed." if dry_run else "Nothing was built, committed or pushed."
    listed = [checks.identity(juneau_root, required=REQUIRED_GIT_EMAIL),
              checks.unreviewed(juneau_root, outcome=outcome),
              checks.upstream(juneau_root)]
    docs_root = juneau_root.parent / "juneau-docs"
    if docs and docs_root.exists() and docs_active(docs_root):
        listed += docs_prechecks(docs_root, dry_run=dry_run)
    return listed


def docs_active(docs_root):
    """Whether the juneau-docs follow-up has anything to push: something staged, or commits ahead of its upstream
    (as of the last fetch).  A git error counts as active, so the docs Prechecks run and report it."""
    git = toolkit("git")
    try:
        if git.staged_paths(docs_root):
            return True
        ahead, _, err = git.ahead_behind(docs_root, fetch=False)
    except git.GitError:
        return True
    return bool(ahead) or (err is not None and err != "no upstream")


def docs_prechecks(docs_root, *, dry_run=False):
    """prechecks()' three checks for the juneau-docs sibling, named "docs identity", "docs unreviewed" and "docs
    upstream", with hints that reach it from the juneau checkout."""
    checks = toolkit("checks")
    outcome = "Nothing would be built, committed or pushed." if dry_run else "Nothing was built, committed or pushed."
    return [checks.identity(docs_root, required=REQUIRED_GIT_EMAIL, label="docs identity"),
            checks.unreviewed(docs_root, git_cmd=DOCS_GIT, label="docs unreviewed", outcome=outcome),
            checks.upstream(docs_root, git_cmd=DOCS_GIT, label="docs upstream")]


def current_branch(repo_dir):
    """The current branch name; "HEAD" on a detached HEAD (as `git rev-parse --abbrev-ref HEAD` prints), else
    "unknown"."""
    git = toolkit("git")
    return git.current_branch(repo_dir) or ("HEAD" if git.short_head(repo_dir) else "unknown")


def run_test_script(cmd, cwd, run, env=None):
    """
    Run test.py with env (None inherits ours).  With markers on it runs through proc.run_group in its own session,
    so a signal aimed at this script's group does not reach it unannounced; on cancellation it is sent SIGTERM
    itself (its handler stops Maven and ends the step cleanly) and, if it ignores that for 30 seconds, its whole
    group is killed.  Either way the caller gets a CompletedProcess whose returncode is test.py's exit code.
    """
    if not run.enabled():
        return subprocess.run(cmd, cwd=cwd, check=False, env=env)
    result = toolkit("proc").run_group(cmd, cwd=cwd, env=env)
    return subprocess.CompletedProcess(cmd, result.code)


def load_timings_module(script_dir):
    """push-timings.py as a module (its name has a hyphen, so it can't be imported by name)."""
    spec = importlib.util.spec_from_file_location("push_timings", script_dir / "push-timings.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def run_timing_report(script_dir, juneau_root):
    """
    The Timing row, from test.py's run-history store for juneau_root: slower entries as warn notes and faster ones
    as info, the full table in test-timings.txt beside the store.  The report is advisory, so a failure shows on
    the row and never stops the push.
    """
    run = _juneau.load()[0]
    with run.row("timing", "Timing", label="Timing") as row:
        try:
            timings = load_timings_module(script_dir)
            if timings.history_off():
                row.summary = "run history is off (JUNEAU_RUN_HISTORY=0)"
                return
            records = timings.load_history(repo=juneau_root)
            report = timings.report_path(repo=juneau_root)
            _, reason = timings.latest_run(records)
            if reason:
                row.summary = reason
                return
            entries = timings.analyse(records)
            timings.write_report(entries, report)
        except Exception as e:
            row.summary = f"⚠ report failed: {e}"
            return
        for entry in entries:
            if entry["flag"] is not None:
                run.note("warn" if entry["flag"] == "slower" else "info", entry["line"], step="timing")
        row.summary = f"{timings.counts(entries)} → {report}"


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

        say(f"📊 Test metrics appended → juneau-integration-tests/test-run-history.tsv ({tests_run} tests, {wall_sec}s)")
    except Exception as exc:
        say(f"⚠ Warning: Could not append test metrics: {exc}", "warn")


def build_test_command(test_script, args):
    """The test.py invocation for the test step; forwards --js-tests/--no-js-tests (auto-detect lives in test.py).
    test.py records its own timings in the run-history store, which push-timings.py reads."""
    cmd = [sys.executable, str(test_script), "--full"]
    if getattr(args, "js_tests", False):
        cmd.append("--js-tests")
    if getattr(args, "no_js_tests", False):
        cmd.append("--no-js-tests")
    return cmd


TAGGED_LINE = re.compile(r"\((\d+) container tests tagged\)")
BOM_LINE = re.compile(r"enumerates all (\d+) consumer-facing modules")


def tags_summary(text):
    """The Tags row, `<n> container tests tagged`, from check-container-tags.py's output; None if not recognised."""
    found = TAGGED_LINE.search(text or "")
    return f"{found.group(1)} container tests tagged" if found else None


def bom_summary(text):
    """The BOM row, `<n> modules in juneau-bom`, from check-bom-completeness.py's output; None if not recognised."""
    found = BOM_LINE.search(text or "")
    return f"{found.group(1)} modules in juneau-bom" if found else None


COMMIT_LINE = re.compile(r"^\[.+? ([0-9a-f]{7,40})\] (.*)$", re.MULTILINE)
FILES_CHANGED = re.compile(r"^ (\d+) files? changed", re.MULTILINE)
COMMIT_SUBJECT_WIDTH = 60


def commit_summary(text):
    """The Commit row, `<hash>  "<subject>"  (<n> files)`, from git commit's output; None if it isn't recognised."""
    text = (text or "").replace("\r\n", "\n")   # tty=True output has PTY line endings
    head = COMMIT_LINE.search(text)
    if head is None:
        return None
    sha, subject = head.groups()
    if len(subject) > COMMIT_SUBJECT_WIDTH:
        subject = subject[:COMMIT_SUBJECT_WIDTH - 1] + "…"
    files = FILES_CHANGED.search(text)
    count = f"  ({files.group(1)} file{'' if files.group(1) == '1' else 's'})" if files else ""
    return f'{sha}  "{subject}"{count}'


def upstream_name(repo_dir):
    """The branch's upstream (`origin/master`), or "" when there is none."""
    result = subprocess.run(["git", "rev-parse", "--abbrev-ref", "--symbolic-full-name", "@{upstream}"],
                            cwd=repo_dir, capture_output=True, text=True, check=False)
    return result.stdout.strip() if result.returncode == 0 else ""


def commit_and_push(
    repo_dir,
    message,
    step_num,
    *,
    label="",
    git_cmd="git",
    pre_flight_hook=None,
    pre_commit_hook=None,
    step_prefix="",
    row_labels=("Commit", "Push"),
):
    """
    Commit whatever is already staged and push, treating "nothing staged" and "nothing to push"
    as two different things (the bug this replaces reported success without ever pushing).

    Nothing staged means there is nothing NEW to commit -- it does NOT mean there is nothing to
    push. A prior run could have committed successfully and then been interrupted (or failed)
    before the push step, leaving unpushed commits sitting on the branch; treating a clean index
    as "nothing to do" in that situation is a false success, since the caller would believe their
    work reached the remote when it did not. So an empty index here skips the COMMIT, not the
    push: if the branch is already ahead of its upstream, that gets pushed. Only a clean index
    with nothing already ahead (or no upstream to compare against) is truly a no-op, and that is
    reported with text that is never confusable with an actual push having happened.

    A staged path is the operator's record that they've reviewed it and it's good -- staging as
    they review, not as a mechanical step right before committing. So this function commits the
    index exactly as it finds it (no `git add .`, anywhere, ever): it does not decide what
    belongs in the commit, because the index already says. And it refuses outright -- commits
    and pushes NOTHING -- if the working tree has any unstaged modification or any untracked,
    non-gitignored file, since their presence means the review pass isn't finished, and this
    script has no way to tell "safe to commit around" apart from "someone forgot to stage it."
    That refusal applies even to a push-only run (nothing staged, only pushing prior commits):
    unreviewed content sitting in the tree at push time isn't safe to assume away, even though
    it isn't literally what's being pushed. This is deliberately strict with no override flag --
    even a flag that names the exact paths being overridden is still a way to push something
    nobody reviewed, which is the one thing this refusal exists to prevent. For genuinely partial
    work (some files reviewed and staged, others still mid-edit), the sanctioned way out is
    `git stash push -- <paths>` before running push.py and `git stash pop` after; the refusal
    message itself prints that command with the actual offending paths already filled in, so the
    recovery is discoverable at the moment of refusal rather than something the operator has to
    already know.

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
        git_cmd: The git invocation shown in the behind-upstream hint. Callers operating outside
            the repo's own directory (e.g. main() running against the juneau-docs sibling while cwd
            is juneau) pass a `-C <path>`-qualified form.
        pre_flight_hook: Optional zero-arg callable invoked once it's established that there is
            something to commit and/or push (i.e. NOT the nothing_to_do case) and that the tree
            has nothing unreviewed in it, before any git state is mutated. Must return True to
            proceed or False to abort (and is responsible for printing its own error and playing
            the failure sound before returning False). Used for checks that must run before ANY
            push, including a push-only run with nothing new to commit. None (the default) means no
            such check is needed.
        pre_commit_hook: Optional zero-arg callable invoked only when there ARE staged changes to
            commit, immediately before `git commit`. Same True/False contract as pre_flight_hook.
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
    git = toolkit("git")
    say(f"\n🔍 Checking for upstream changes{label}...")
    upstream = git.upstream_verdict(repo_dir, git_cmd=git_cmd)
    commits_ahead = upstream.ahead
    if upstream.status == "error":
        # Fails closed like the Prechecks: a branch nobody could compare with its upstream is not pushed.
        say(f"\n❌ ERROR: {upstream.reason}{label}. Nothing was committed or pushed.", "error")
        say(f"   Re-run the push once `{git_cmd} fetch` works.", "error")
        return ("error", step_num)
    if not upstream.ok:
        say(f"\n❌ ERROR: Local branch{label} is {upstream.reason}. Nothing was committed or pushed.", "error")
        for line in upstream.hint:
            say(f"   {line}", "error")
        return ("error", step_num)

    try:
        has_staged = bool(get_staged_paths(repo_dir))
    except (subprocess.CalledProcessError, OSError, git.GitError) as e:
        say(f"\n❌ ERROR: Could not check the Git index{label}: {e}", "error")
        return ("error", step_num)

    # Nothing staged skips the commit, not the push -- see docstring above.
    if not has_staged and not commits_ahead:
        say(f"\nℹ Nothing staged and no unpushed commits{label}.")
        say(f"🎉 Push{label} completed successfully (nothing to commit or push)!")
        return ("nothing_to_do", step_num)

    # There is something to commit and/or push -- refuse if any of it (or anything else in the
    # tree) hasn't been reviewed (staged) yet. See docstring above for why this applies even to
    # a push-only run.
    try:
        unreviewed_paths = check_unreviewed_changes(repo_dir)
    except (subprocess.CalledProcessError, OSError, git.GitError) as e:
        say(f"\n❌ ERROR: Could not check for unreviewed changes{label}: {e}", "error")
        return ("error", step_num)
    if unreviewed_paths:
        report_unreviewed(unreviewed_paths, label, "Nothing was committed or pushed.")
        return ("error", step_num)

    if pre_flight_hook is not None and not pre_flight_hook():
        return ("error", step_num)

    if has_staged:
        if pre_commit_hook is not None and not pre_commit_hook():
            return ("error", step_num)

        # Step N: Commit the index as-is -- see docstring above for why there is no `git add .`.
        say(f"\n📝 Step {step_num}: Committing changes to Git{label}...")
        if not run_command(
            ["git", "commit", "-m", message],
            f"  Creating commit{label}...",
            repo_dir,
            step_id=f"{step_prefix}commit", title=f"Commit{label}", label=row_labels[0],
            capture=True, tty=True, summarize=commit_summary,
        ):
            aborted(f"\n❌ git commit failed{label} -- aborting.")
            return ("error", step_num)
        say(f"✅ Step {step_num}: Git commit completed{label}.")
        step_num += 1
    else:
        say(f"\nℹ Nothing staged to commit{label} -- {commits_ahead} unpushed commit(s) "
            "already on this branch will be pushed now.")

    # Step N (or N+1): Push to remote -- to the tracked upstream (git.push_command), never a guessed remote.
    try:
        push_cmd = git.push_command(repo_dir)
    except git.GitError as e:
        say(f"\n❌ ERROR: Could not work out where to push{label}: {e}", "error")
        say(f"⚠ Local commits exist but were not pushed{label}.", "warn")
        return ("error", step_num)
    upstream = upstream_name(repo_dir) if console_active() else ""
    if not run_command(
        push_cmd,
        f"🚀 Step {step_num}: Pushing changes to remote repository{label}...",
        repo_dir,
        step_id=f"{step_prefix}push", title=f"Push{label}", label=row_labels[1],
        tty=True, summarize=lambda _text: f"{upstream} ✓" if upstream else None,
    ):
        aborted(f"\n❌ git push failed{label}.")
        say(f"⚠ Local commits exist but were not pushed{label}.", "warn")
        return ("error", step_num)
    say(f"✅ Step {step_num}: Changes pushed to remote{label}.")
    step_num += 1

    return ("ok", step_num)


def run_docs_followup(juneau_root, message, step_num, *, docs_prechecked=False):
    """
    juneau-docs follow-up to a default-flow juneau push: smoke check + commit + push, via
    commit_and_push() (shared with --docs-only's run_docs_only()).

    Extracted out of main() -- like run_tracker_audit_gate()/commit_and_push() -- so it's
    independently testable without running main()'s mvn build/test steps first.

    Guarded only by the juneau-docs sibling existing, NOT by whether it has local changes: a
    clean juneau-docs tree can still have unpushed commits left over from an earlier
    interrupted run, and this follow-up has to run for that case too or it silently never
    pushes them -- the same false-success shape commit_and_push() exists to fix, one level up.

    The juneau-docs identity is checked by the Prechecks row (docs_prechecks) whenever juneau-docs has
    something to push.  When it was idle then (docs_prechecked False) but has become active during the build, the
    identity is checked here, just before anything is committed, and a mismatch refuses.  The Docusaurus smoke
    check only has anything new to validate when there IS a commit about to be made, so it's commit_and_push()'s
    pre_commit_hook.

    Args:
        juneau_root: The juneau repo root (used only to locate the juneau-docs sibling).
        message: Commit message to use if a juneau-docs commit is made.
        step_num: The current step number (for output formatting).
        docs_prechecked: Whether the Prechecks row already ran docs_prechecks (juneau-docs was active then).

    Returns:
        tuple: (status, step_num)
        - status: "ok" (juneau-docs was pushed, committed-then-pushed, or genuinely had nothing
          to do -- caller should continue), "no_docs_root" (the juneau-docs sibling doesn't
          exist -- a pure skip, not a failure, caller should continue), or "error" (caller
          should exit 1 immediately).
        - step_num: commit_and_push()'s returned step_num, unchanged for "no_docs_root".
    """
    docs_root = juneau_root.parent / "juneau-docs"
    if not docs_root.exists():
        return ("no_docs_root", step_num)

    if not docs_prechecked and docs_active(docs_root):
        checks = toolkit("checks")
        verdict = checks.evaluate(checks.identity(docs_root, required=REQUIRED_GIT_EMAIL, label="docs identity"))
        if verdict.status != "pass":
            aborted(f"\n❌ juneau-docs became active during the build and its identity check failed: "
                    f"{verdict.reason}  Nothing was committed or pushed in juneau-docs.", *verdict.hint)
            return ("error", step_num)

    def _docs_smoke_check():
        say("\n📚 juneau-docs has changes — running Docusaurus smoke check first...")
        docs_build_script = docs_root / "scripts" / "build-docs.py"
        docs_smoke_start = time.time()
        try:
            returncode = run_gate(
                [sys.executable, str(docs_build_script), "--skip-maven"],
                docs_root, "docs-smoke", "Docs smoke check", "Docs build"
            )
            docs_smoke_elapsed = time.time() - docs_smoke_start
            if returncode != 0:
                aborted("\n❌ Docs smoke check failed — fix the Docusaurus build before pushing juneau-docs.")
                return False
            say(f"✅ Docs smoke check passed ({docs_smoke_elapsed:.1f}s)")
            return True
        except Exception as e:
            say(f"\n❌ Docs smoke check failed: {e}", "error")
            return False

    docs_status, step_num = commit_and_push(
        docs_root, message, step_num,
        label=" (juneau-docs)",
        git_cmd=DOCS_GIT,
        pre_commit_hook=_docs_smoke_check,
        step_prefix="docs-", row_labels=("Docs", "Docs push"),
    )
    if docs_status == "error":
        return ("error", step_num)
    return ("ok", step_num)


def run_docs_only(args, juneau_root):
    """
    --docs-only mode: operate ONLY on the sibling juneau-docs repo.

    Skips the entire juneau code path (no container-tags/BOM checks, no test run, no
    mvn build/install, no juneau commit/push). Runs the
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
    run = _juneau.load()[0]
    console = not run.Sinks().is_default

    if not console or args.dry_run:
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

    missing = (f"❌ ERROR: juneau-docs repo not found at {docs_root}",
               "   Expected it as a sibling of the juneau checkout.")
    if args.dry_run:
        if not docs_root.exists():
            print("\n" + "\n".join(missing))
            return 1
        print("\nSteps that would be executed:")
        print(f"  1. Prechecks on juneau-docs: refuse unless git user.email is {REQUIRED_GIT_EMAIL}, the tree has no")
        print("     unreviewed changes and the branch isn't behind upstream (git fetch + compare), before any gate runs;")
        print("     the last two are checked again just before the commit")
        print("  2. Check juneau-docs git status (exit 0 with a no-changes message if clean)")
        print("  3. Run docs verification gate: python3 scripts/build-docs.py --skip-maven (runs verify-docs.py)")
        print(f"  4. Commit whatever is already staged: git commit -m \"{args.message}\"  (in juneau-docs; "
              "refuses if anything is unstaged/untracked-and-not-gitignored)")
        print("  5. Push to the tracked upstream  (in juneau-docs)")
        toolkit("run").checks("prechecks", "Prechecks", docs_prechecks(docs_root, dry_run=True), dry_run=True)
        print("\nDry run complete. Use without --dry-run to execute.")
        return 0

    repo = docs_root if docs_root.exists() else juneau_root
    return _in_session(f"📚 Juneau docs push · {current_branch(repo)}",
                       lambda s: _docs_only_steps(s, args, console, docs_root, missing),
                       mode="push", repo=repo,
                       plan=[("prechecks", "Prechecks"), ("docs-smoke", "Docs build"), ("commit", "Commit"),
                             ("push", "Push")])


def _docs_only_steps(s, args, console, docs_root, missing):
    """--docs-only's steps inside its session s.  Returns the exit code."""
    if not docs_root.exists():
        s.fail(missing[0], hint=missing[1:])
    s.checks("prechecks", "Prechecks", docs_prechecks(docs_root))
    # After Prechecks: upstream check, no-op detection, unreviewed-changes guard, docs smoke check,
    # commit, and push -- all delegated to commit_and_push() (shared with main()'s Step 6
    # follow-up) so the nothing-staged-vs-nothing-to-push fix and the unreviewed-changes guard
    # live in one place. The Docusaurus smoke build only has anything new to validate when there
    # IS a commit about to be made, so it's wired in as commit_and_push()'s pre_commit_hook
    # rather than running unconditionally.
    def _docs_smoke_check():
        say("\n📚 juneau-docs has changes — running Docusaurus smoke check first...")
        docs_build_script = docs_root / "scripts" / "build-docs.py"
        try:
            returncode = run_gate(
                [sys.executable, str(docs_build_script), "--skip-maven"],
                docs_root, "docs-smoke", "Docs smoke check", "Docs build"
            )
            if returncode != 0:
                aborted("\n❌ Docs smoke check failed — fix the Docusaurus build before pushing juneau-docs.")
                return False
            say("✅ Docs smoke check passed")
            return True
        except Exception as e:
            say(f"\n❌ Docs smoke check failed: {e}", "error")
            return False

    status, _ = commit_and_push(
        docs_root, args.message, 5,
        label=" (juneau-docs)",
        git_cmd=DOCS_GIT,
        pre_commit_hook=_docs_smoke_check,
    )
    if status == "error":
        return 1
    if status == "nothing_to_do":
        return 0

    if not console:
        print("\n" + "=" * 70)
        print("🎉 Docs-only push completed successfully!")
        print(f"📦 Commit message: '{args.message}'")
        print("=" * 70)
    return 0


def main():
    """
    The push.  _main() parses the arguments and resolves the commit message, then opens the run session, which owns
    the run and done markers, the console, the summary line, the bell and the sound.  A Ctrl-C (or, with markers on, a
    SIGTERM) before the session opens exits 130 with "Cancelled." too.
    """
    run = _juneau.load()[0]
    previous_sigterm = None
    if run.enabled():
        # Unwind to the KeyboardInterrupt handler below; nothing is torn down inside the signal handler.
        def on_sigterm(signum, frame):
            raise KeyboardInterrupt
        previous_sigterm = signal.signal(signal.SIGTERM, on_sigterm)
    try:
        return _main()
    except KeyboardInterrupt:
        toolkit("run").say("\n❌ Cancelled.", "error")
        toolkit("notify").bell(silence_env=(PUSH_NO_SOUND_ENV,))
        return 130
    finally:
        if previous_sigterm is not None:
            signal.signal(signal.SIGTERM, previous_sigterm)


#: Each step's row label, for the labelled plan (a pending row until the step starts).  build, tests and js-tests
#: are test.py's rows; the others are this script's.
PLAN_LABELS = {
    "prechecks": "Prechecks", "sonar": "Sonar", "tracker": "Tracker", "container-tags": "Tags",
    "bom": "BOM", "build": "Compile", "tests": "Java tests", "py-tests": "Python tests", "js-tests": "Browser tests", "timing": "Timing",
    "install": "Install", "commit": "Commit", "push": "Push", "docs-smoke": "Docs build",
    "docs-commit": "Docs", "docs-push": "Docs push",
}


def push_plan(args, script_dir, juneau_root, console):
    """The top-level steps this run will take, in order, as (id, label) pairs, for the console's pending rows and
    Overall figure."""
    ids = [] if args.test_only else ["prechecks"]
    if args.sonarqube:
        ids.append("sonar")
    if args.tracker_audit:
        ids.append("tracker")
    if args.skip_tests:
        ids.append("tests")
    else:
        ids += [step_id for step_id, name in (("container-tags", "check-container-tags.py"),
                                             ("bom", "check-bom-completeness.py")) if (script_dir / name).exists()]
        if (script_dir / "test.py").exists():
            ids += ["build", "tests", "py-tests", "js-tests"]
        else:
            ids.append("tests")
        if console and (script_dir / "push-timings.py").exists():
            ids.append("timing")
    ids.append("install")
    if not args.test_only:
        ids += ["commit", "push"]
        if (juneau_root.parent / "juneau-docs").exists():
            ids += ["docs-smoke", "docs-commit", "docs-push"]
    return [(step_id, PLAN_LABELS[step_id]) for step_id in ids]


def _in_session(title, body, *, mode, repo, plan):
    """Run body(session) -- which returns an exit code -- inside the run session; returns the session's exit code."""
    with toolkit("run").session(title, bell=True, sound=True, silence_env=(PUSH_NO_SOUND_ENV,),
                                mode=mode, project=repo.name, repo=repo, exit=False, plan=plan) as session:
        code = body(session)
        if code:
            raise SystemExit(code)
    return session.code


def parse_args(argv=None):
    """The command line, validated, with --console and --detail exported (JUNEAU_RUN_CONSOLE, default condensed, and
    JUNEAU_RUN_DETAIL) so the session and test.py use them.  Exits 2 on a bad combination."""
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
  python3 push.py "Tweaked console JS" --js-tests
  python3 push.py "Tweaked console JS" --no-js-tests
  python3 push.py "Fixed bug in RestClient" --detail modules
  python3 push.py "Fixed bug in RestClient" --console condensed
        """
    )
    
    parser.add_argument(
        "message",
        nargs="?",
        help="Git commit message; prompted for if omitted (not used with --test-only)"
    )

    parser.add_argument(
        "--test-only",
        action="store_true",
        dest="test_only",
        help=(
            "Run only the build and test gates (container tags, BOM completeness, tests, build and install). "
            "No identity check, commit, push or juneau-docs follow-up. Not the same flag as "
            "test.py's --test-only, which means 'tests without the build'."
        )
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
            "path (no container-tags/BOM checks, no tests, no mvn build/install, "
            "no juneau commit/push). Still enforces the "
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
            "Opt-in gate: run ~/Project Work/scripts/todo-status-audit.py --project juneau "
            "against ~/Project Work/todos/juneau/ and block the push if it flags any plan file "
            "(stale/misplaced status header, or a declared Counts: claim that no longer "
            "re-derives from committed HEAD content)."
        )
    )
    
    parser.add_argument(
        "--js-tests",
        action="store_true",
        dest="js_tests",
        help=(
            "Force the headless-browser JS harness (mvn -Pjs-tests, the CI js-tests job) as part of the "
            "test step. Fails the push if Node/npm are missing. Default: test.py auto-enables it when a "
            ".js/.css/.ftl file under a src/ tree changed vs origin/master, and skips with a notice if "
            "Node/npm are missing."
        )
    )

    parser.add_argument(
        "--no-js-tests",
        action="store_true",
        dest="no_js_tests",
        help="Never run the JS harness, even when JS/CSS/FTL files changed."
    )

    parser.add_argument(
        "--console",
        choices=("full", "condensed", "none"),
        help="Maven output on the console: full, condensed (default) or none (also JUNEAU_RUN_CONSOLE; the flag wins)."
    )

    parser.add_argument(
        "--detail",
        help="Console detail: summary, actionable (default), modules or all (also JUNEAU_RUN_DETAIL)."
    )

    args = parser.parse_args(argv)
    if args.js_tests and args.no_js_tests:
        parser.error("--js-tests and --no-js-tests are mutually exclusive")
    if args.test_only and (args.docs_only or args.sonarqube or args.tracker_audit or args.skip_tests):
        parser.error("--test-only cannot be combined with --docs-only, --sonarqube, --tracker-audit or --skip-tests")
    toolkit("run").export_console(args.console)    # precedence: flag > environment > condensed
    try:
        _juneau.load()[0].export_detail(args.detail)
    except ValueError as e:
        parser.error(str(e))
    return args


def _main():  # NOSONAR python:S3776 -- Cognitive complexity is acceptable for this main function
    args = parse_args()
    run = _juneau.load()[0]
    if not args.test_only:
        args.message = resolve_commit_message(args.message)

    # Get the Juneau root directory
    script_dir = Path(__file__).parent
    juneau_root = script_dir.parent

    # --docs-only short-circuits into its own self-contained flow (juneau-docs only);
    # everything below this is the unchanged default (full juneau build+push) flow.
    if args.docs_only:
        return run_docs_only(args, juneau_root)

    console = not run.Sinks().is_default

    if not console or args.dry_run:
        print("=" * 70)
        print("🚀 Juneau Build and Push Script")
        print("=" * 70)
        print(f"Working directory: {juneau_root}")
        if args.test_only:
            print("🧪 TEST-ONLY MODE (--test-only) - build and test gates only; nothing is committed or pushed")
        else:
            print(f"Commit message: '{args.message}'")
        if args.skip_tests:
            print("⚠ Tests will be SKIPPED")
        if args.sonarqube:
            print("🔎 SonarQube gate ENABLED (--sonarqube)")
        if args.tracker_audit:
            print("📋 Tracker audit gate ENABLED (--tracker-audit)")
        if args.js_tests:
            print("🌐 Browser tests FORCED (--js-tests)")
        if args.no_js_tests:
            print("🌐 Browser tests DISABLED (--no-js-tests)")
        if args.dry_run:
            print("🔍 DRY RUN MODE - No actual changes will be made")
        print("=" * 70)

    if args.dry_run:
        print("\nSteps that would be executed:")
        step_num = 1
        if not args.test_only:
            print(f"  {step_num}. Prechecks: refuse unless git user.email is {REQUIRED_GIT_EMAIL}, the tree has no unreviewed "
                  "(unstaged/untracked) changes and the branch isn't behind upstream (git fetch + compare) -- juneau, and "
                  "juneau-docs when present -- before any gate runs; the last two are checked again before the commit")
            step_num += 1
        if args.sonarqube:
            _branch = current_branch(juneau_root)
            _branch_arg = f" --branch {_branch}" if _branch not in ("master", "unknown") else ""
            print(f"  {step_num}. Run SonarQube gate: python3 scripts/sonarqube.py --all --run --fail-on-issues{_branch_arg} (blocks push if any issues; skips gracefully if this branch has no SonarCloud analysis yet)")
            step_num += 1
        if args.tracker_audit:
            print(f"  {step_num}. Run tracker audit gate: python3 ~/Project\\ Work/scripts/todo-status-audit.py --project juneau (blocks push if any plan file is flagged)")
            step_num += 1
        if not args.skip_tests:
            print(f"  {step_num}. Verify container test tags: python3 scripts/check-container-tags.py")
            step_num += 1
            print(f"  {step_num}. Verify BOM completeness: python3 scripts/check-bom-completeness.py")
            step_num += 1
            print(f"  {step_num}. Run tests: python3 scripts/test.py --full (timings go to ~/.cache/juneau-run/history/<repo>-<sha8>/test.jsonl)")
            step_num += 1
            print(f"  {step_num}. Print timing deltas: python3 scripts/push-timings.py --repo . (reads that store)")
            step_num += 1
        if args.skip_tests:
            print(f"  {step_num}. Build and install: mvn clean package install -DskipTests")
        else:
            print(f"  {step_num}. Build and install: skipped (test.py --full has already installed)")
        step_num += 1
        if not args.test_only:
            print(f"  {step_num}. Commit whatever is already staged: git commit -m \"{args.message}\" "
                  "(refuses if anything is unstaged/untracked-and-not-gitignored -- stage or discard it first)")
            step_num += 1
            print(f"  {step_num}. Push to the tracked upstream")
        if not args.test_only:
            toolkit("run").checks("prechecks", "Prechecks", prechecks(juneau_root, dry_run=True), dry_run=True)
        print("\nDry run complete. Use without --dry-run to execute.")
        return 0

    return _in_session(f"🚀 Juneau push · {current_branch(juneau_root)}",
                       lambda s: _push_steps(s, args, run, console, script_dir, juneau_root),
                       mode="test" if args.test_only else "push", repo=juneau_root,
                       plan=push_plan(args, script_dir, juneau_root, console))


def _push_steps(s, args, run, console, script_dir, juneau_root):  # NOSONAR python:S3776 -- Cognitive complexity is acceptable for this function
    """The steps of the push inside its session s.  Returns the exit code."""
    step_num = 1
    # Prechecks refuse before any gate (the row says why); --test-only never commits or pushes, so it has none.
    # Whether they covered juneau-docs (it was active then) decides whether the docs follow-up checks its identity late.
    docs_prechecked = False
    if not args.test_only:
        listed = prechecks(juneau_root)
        docs_prechecked = any(c.name == "docs identity" for c in listed)
        s.checks("prechecks", "Prechecks", listed)
    if console and args.skip_tests:
        say("⚠ Tests will be SKIPPED (--skip-tests)", "warn")

    # Step 0 (opt-in, --sonarqube/--sonar): SonarQube report gate. Runs first so it
    # aborts cheaply, before the container-tags/BOM checks, tests, and build.
    if args.sonarqube:
        gate_status = run_sonarqube_gate(juneau_root, step_num)
        if gate_status == "fail":
            return aborted("\n❌ Push aborted: SonarCloud currently reports open issues for this branch.",
                           "   Note: this reflects SonarCloud's last CI-analyzed commit (a ratchet), not your local diff.",
                           "   Resolve/triage the reported issues, or omit --sonarqube to push without this gate.")
        if gate_status == "error":
            return aborted("\n❌ Push aborted: the SonarQube gate could not be run.",
                           f"   Fix the issue {'in the full log' if console else 'reported above'} "
                           "(e.g. install a Python >= 3.10 interpreter), or omit --sonarqube to push without this gate.")
        if gate_status == "skip":
            say(f"⚠ Step {step_num}: SonarQube gate skipped — SonarCloud has no analysis for this "
                "branch yet (a new branch CI hasn't scanned). Continuing without the Sonar gate.", "warn")
        else:  # "pass"
            say(f"✅ Step {step_num}: SonarQube gate passed — SonarCloud reports zero issues for this branch.")
        step_num += 1

    # Step 0b (opt-in, --tracker-audit/--todo-audit): TODO tracker audit gate. Also runs
    # before the container-tags/BOM checks, tests, and build, so it aborts cheaply.
    tracker_gate_status = maybe_run_tracker_audit_gate(args, juneau_root, step_num)
    if tracker_gate_status is not None:
        if tracker_gate_status == "fail":
            return aborted("\n❌ Push aborted: the tracker audit flagged at least one plan file "
                           "under ~/Project Work/todos/juneau/.",
                           "   Run `python3 ~/Project\\ Work/scripts/todo-status-audit.py --project juneau` for "
                           "details, or omit --tracker-audit to push without this gate.")
        if tracker_gate_status == "error":
            return aborted("\n❌ Push aborted: the tracker audit gate could not be run.",
                           f"   Fix the issue {'in the full log' if console else 'reported above'}, "
                           "or omit --tracker-audit to push without this gate.")
        say(f"✅ Step {step_num}: Tracker audit gate passed — no plan file flagged.")
        step_num += 1

    # Step 1: Run tests (optional)
    installed_by_tests = False
    if not args.skip_tests:
        check_container_tags = script_dir / "check-container-tags.py"
        if check_container_tags.exists():
            if not run_marked(
                "container-tags", "Container test tags",
                [sys.executable, str(check_container_tags)],
                f"🔎 Step {step_num}: Checking container test tags...",
                juneau_root,
                label="Tags",
                summarize=tags_summary
            ):
                return aborted("\n❌ Build process aborted due to missing container test tags.")
            step_num += 1

        check_bom = script_dir / "check-bom-completeness.py"
        if check_bom.exists():
            if not run_marked(
                "bom", "BOM completeness",
                [sys.executable, str(check_bom)],
                f"🔎 Step {step_num}: Checking BOM completeness...",
                juneau_root,
                label="BOM",
                summarize=bom_summary
            ):
                return aborted("\n❌ Build process aborted: juneau-bom is out of sync with the reactor.")
            step_num += 1

        test_script = script_dir / 'test.py'
        if test_script.exists():
            say(f"\n🧪 Step {step_num}: Running tests via test.py...")
            try:
                _test_start = time.time()
                test_cmd = build_test_command(test_script, args)
                installed_by_tests = "--full" in test_cmd
                # The child draws its own Compile/Java tests/Browser tests/Perf rows; ours closes until it exits.  Its
                # environment carries JUNEAU_PUSH_ACTIVE (test.py then leaves the end-of-run bell to us) and, with
                # markers on, JUNEAU_RUN_N_BASE reserving the numbers of the steps it contributes so the whole run
                # stays in order.  Neither is set in our own environment, so later children don't inherit them.
                with toolkit("run").handoff(reserve=3) as extra:
                    result = run_test_script(test_cmd, juneau_root, run, env={**os.environ, **extra})
                _test_wall_sec = int(time.time() - _test_start)
                if result.returncode != 0:
                    return aborted("\n❌ Build process aborted due to test failures.")
                say(f"✅ Step {step_num}: Java tests passed")
                _append_test_run_history(juneau_root, _test_wall_sec)
            except Exception as e:
                say(f"\n❌ Error running tests: {e}", "error")
                return 1
        else:
            # Fallback to direct mvn test if test.py doesn't exist
            _test_start = time.time()
            _mvn_ok = run_marked(
                "tests", "Java tests",
                ["mvn", "test"],
                f"🧪 Step {step_num}: Running tests...",
                juneau_root,
                tool="maven"
            )
            _test_wall_sec = int(time.time() - _test_start)
            if not _mvn_ok:
                return aborted("\n❌ Build process aborted due to test failures.")
            _append_test_run_history(juneau_root, _test_wall_sec)
        timing_report = script_dir / "push-timings.py"
        if timing_report.exists():
            if run.console_active():
                run_timing_report(script_dir, juneau_root)
            else:
                run_command(
                    [sys.executable, str(timing_report), "--repo", str(juneau_root)],
                    f"📊 Step {step_num}: Timing regression report...",
                    juneau_root
                )
        step_num += 1
    else:
        say(f"\n⏭️  Step {step_num}: Skipping tests (--skip-tests flag)")
        with run.row("tests", "Java tests", label="Java tests") as tests_row:
            tests_row.skip("--skip-tests")
        step_num += 1
    
    # Step 2: Build and install -- unless test.py --full has just done `mvn clean install {PARALLELISM} -DskipTests`
    # (scripts/test.py:432-433), in which case a second install only rebuilds the same artifacts.
    if installed_by_tests:
        # Still the protocol step `install` (end status skip), so the release manager's step list doesn't change.
        with marked_step("install", "Build and install", label="Install") as install_step:
            install_step.skip("done by Java tests")
        if not console:
            say(f"\n⏭️  Step {step_num}: Build and install already done by test.py --full")
    elif not run_marked(
        "install", "Build and install",
        ["mvn", "clean", "package", "install", "-DskipTests"],
        f"🏗️  Step {step_num}: Building and installing project...",
        juneau_root,
        tool="maven",
        label="Install",
    ):
        return aborted("\n❌ Build process aborted due to build failure.")
    step_num += 1

    if args.test_only:
        run.note("info", "Test-only run: nothing was committed or pushed")
        if not console:
            print("\n" + "=" * 70)
            print("🧪 Test-only run completed successfully; nothing was committed or pushed.")
            print("=" * 70)
        return 0

    # Step 4/5: Commit (whatever is staged) and push (if there's anything ahead) --
    # extracted into commit_and_push() so the "nothing staged != nothing to push" fix and the
    # unreviewed-changes guard are unit-testable against real git without invoking the mvn
    # build/test steps above.
    push_status, step_num = commit_and_push(juneau_root, args.message, step_num)
    if push_status == "error":
        return 1
    if push_status == "nothing_to_do":
        return 0

    # Step 6 (optional): juneau-docs follow-up — see run_docs_followup() for why the guard is
    # "does juneau-docs exist" rather than "does it have local changes".
    docs_status, step_num = run_docs_followup(juneau_root, args.message, step_num, docs_prechecked=docs_prechecked)
    if docs_status == "error":
        return 1
    # "no_docs_root" or "ok" (including juneau-docs itself having had nothing to do) both fall
    # through -- the juneau push above already succeeded either way.

    # Success!
    if not console:
        print("\n" + "=" * 70)
        print("🎉 All operations completed successfully!")
        print(f"📦 Commit message: '{args.message}'")
        print("=" * 70)
    return 0


if __name__ == "__main__":
    sys.exit(main())

