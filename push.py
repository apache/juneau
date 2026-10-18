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
    ./push.py --console full "message"        # plain output; the default is the condensed console rows

    --console full|condensed|none sets JUNEAU_RUN_CONSOLE before juneau_run is loaded (the flag wins over a value
    already in the environment; with neither, the default is condensed), exactly as scripts/test.py does in
    apache/juneau.  Condensed (the default, or RUN_MARKERS=1) makes this script load juneau_run, with the git output
    going to the full log; full and none leave it unloaded, so both give the plain output.  This script
    has no Maven step, so the console view shows the Prechecks, Commit and Push steps only.

    When a run ends (success or failure) on a terminal, push.py rings the terminal bell.
    Set JRM_PUSH_NO_BELL=1 to silence it (it is also skipped when SANDBOX_RUNTIME=1).

Steps: Prechecks, Commit, Push.  Prechecks is one row of four checks that all run, none stopping at the first
    failure: identity, unreviewed (the review gate), staged (a warning, "nothing staged", ends the run with nothing to
    do) and upstream.  The upstream check fetches and refuses (before the commit) when the branch is behind its
    upstream, since the push would be rejected anyway; a branch with no upstream proceeds (the push then sets it),
    and a fetch error fails closed.  Under --dry-run each failing check shows as a "would refuse" warning.

Safety:
    - Never uses --force or --no-verify.
    - Never runs `git config` (it only reads config; you fix a missing
      identity yourself).
    - Never runs `git add`: a staged path is the operator's record that
      they've reviewed it and it's good, so this script commits the index
      exactly as it finds it and refuses outright -- commits and pushes
      nothing -- if the working tree has any unstaged modification or any
      untracked, non-gitignored file.
    - Exits non-zero with a clear message on any precondition failure
      (no git identity configured, unreviewed changes present, push
      rejected, etc).

Built on Apache Juneau's juneau_scripts toolkit, loaded by the vendored scripts/bootstrap.py from the juneau-run
artifact at the pom's juneau.version.  The policy above is this file's code; the toolkit supplies the git, run and
bell helpers.
"""

import argparse
import os
import sys
from pathlib import Path

REPO_DIR = Path(__file__).resolve().parent

sys.path.insert(0, str(REPO_DIR / "scripts"))  # push.py lives at the repo root; bootstrap.py is vendored under scripts/

import bootstrap  # noqa: E402
from bootstrap import run_requested  # noqa: E402,F401  (push.run_requested is the environment rule tests check)

# The toolkit modules, bound by toolkit() on first use so that importing this file has no side effects.
git = run = notify = checks = None


class _NoMarkers:
    """Stands in for juneau_run when it isn't loaded: the calls this script makes directly are no-ops or False."""

    def console_active(self):
        return False

    def note(self, *args, **kwargs):
        # Intentionally empty: markers are off, so there is nothing to record.
        pass


# The run-protocol module in use: juneau_run when the environment asks for it and it loads, otherwise the stand-in.
run_markers = _NoMarkers()


def toolkit():
    """Load juneau_scripts (once per process) and bind git, run and notify.  Returns the package."""
    global git, run, notify, checks
    # cache_dir: the first load fixes it for the process, so a fetched copy lands in <repo>/target as before.
    _, js = bootstrap.load(REPO_DIR / "pom.xml", run=False, cache_dir=REPO_DIR / "target" / "juneau-run")
    from juneau_scripts import checks, git, notify, run  # noqa: F811
    return js


def say(text="", level=None):
    """
    run.say(), with the level (error/warn/info) read from the leading mark when not given.  Identical to print() with
    a plain run; a filtered note under a console view.
    """
    if level is None:
        level = "info"
        if "❌" in text:
            level = "error"
        elif "⚠" in text:
            level = "warn"
    run.say(text, level)


def resolve_commit_message(s, cli_message):
    """Use the CLI-supplied message, or prompt for one; an aborted prompt ends the session (s.fail says why)."""
    if cli_message:
        return cli_message
    try:
        with run.paused():
            message = input("Commit message: ").strip()
    except (EOFError, KeyboardInterrupt):
        s.fail("\n❌ Aborted: no commit message provided.")
    if not message:
        s.fail("❌ Aborted: commit message cannot be empty.")
    return message


def _prechecks(s, repo_dir, dry_run):
    """
    The Prechecks row: identity, unreviewed, staged and upstream, all of which run (none stops at the first failure).
    Returns the CheckResult; a failure ends the session (the row already says why), except under --dry-run, where each
    failing check is a "would refuse" warning.

    The review gate: a staged path is the operator's record that they've reviewed it and it's good -- staging as they
    review, not as a mechanical step right before committing.  So this commits the index exactly as it finds it (no
    `git add`, anywhere, ever) and refuses outright -- commits and pushes NOTHING -- if the working tree has any
    unstaged modification to a tracked file, or any untracked, non-gitignored file, since their presence means the
    review pass isn't finished.  Gitignored untracked files are not flagged: build output was never going to be staged
    or reviewed.  This is deliberately strict with no override flag -- even a flag that names the exact paths being
    overridden is still a way to push something nobody reviewed, which is the one thing this refusal exists to
    prevent.  For genuinely partial work, the sanctioned way out is `git stash push -- <paths>` before running
    push.py and `git stash pop` after; the refusal hint prints that command with the offending paths filled in.

    The upstream gate fails early when the push is going to be rejected anyway: behind refuses before the commit, with a
    hint that keeps the reviewed index staged (never a plain `--autostash`, which would unstage reviewed changes).  No
    upstream proceeds; a fetch or rev-list error fails closed.  Every check only reads (the upstream check fetches), so
    a dry run reports the REAL outcome instead of a guess, and each check fails closed if git can't run.
    """
    return s.checks("prechecks", "Prechecks", [
        checks.identity(repo_dir),
        checks.unreviewed(repo_dir),
        checks.staged(repo_dir),
        checks.upstream(repo_dir),
    ], dry_run=dry_run)


def _dry_run_report(staged, message, repo_dir):
    """The --dry-run report: what the commit and push would do."""
    plural = "" if len(staged) == 1 else "s"
    say(f"\n🔍 DRY RUN — no changes will be made. Would commit the staged index ({len(staged)} path{plural}):")
    say(f"   git commit -F- <<< {message!r}")
    try:
        say(f"   {' '.join(git.push_command(repo_dir))}")
    except git.GitError as e:
        say(f"⚠ Would refuse to push: {e}")
        run_markers.note("warn", f"Dry run — would refuse to push: {e}")
        return
    run_markers.note("info", "Dry run — no changes made.")


def _commit(s, repo_dir, message):
    """Commit the index exactly as found.  Returns the short sha; s.commit is set as soon as it exists."""
    with run.step("commit", "Commit") as c:
        say("\n📝 Committing...")
        # Message via stdin (git commit -F -) so multi-line messages and odd characters need no quoting.
        r = git.commit_index(message, repo_dir, stdin=True)
        if not r.ok:
            s.fail(f"\n❌ ERROR: git commit failed (exit code {r.code}).")
        sha = git.short_head(repo_dir)
        s.commit = sha
        c.summary = sha
        run_markers.note("info", f"Committed {sha}", step="commit")
    return sha


def _push_incomplete(sha):
    text = f"Committed {sha} locally; push did not complete"
    say(f"\n⚠ {text}.")
    run_markers.note("warn", text, step="push")


def _push(s, repo_dir, sha):
    """Push the new commit to the branch's upstream (origin/<branch> with -u when it has none).  A failed or
    interrupted push still reports the local commit."""
    try:
        with run.step("push", "Push") as p:
            say("🚀 Pushing...")
            r = git.push_tracked(repo_dir)
            if r.ok:
                run_markers.note("info", "Pushed", step="push")
            else:
                if r.err:
                    say(r.err, "error")  # a refusal from push_command; git's own output went to the terminal or log
                p.fail()
    except KeyboardInterrupt:
        _push_incomplete(sha)
        raise
    if not r.ok:
        _push_incomplete(sha)
        s.fail(f"\n❌ ERROR: git push failed (exit code {r.code}).")


def commit_and_push(s, repo_dir, cli_message, *, dry_run=False):
    """
    Run the Prechecks, then commit whatever is already staged in repo_dir and push, refusing to commit or push ANYTHING
    if a precheck fails (identity, unreviewed content, behind upstream; see _prechecks()).

    A fully CLEAN tree (nothing staged, nothing unreviewed) is a benign no-op, not a refusal: there is genuinely
    nothing to commit, and this repo has no push-only path.  --dry-run reports the real outcome and changes nothing;
    a refusal under it is reported as "would refuse" and does not fail the run.

    Returns normally on success (committed and pushed, a no-op on a clean tree, or a --dry-run report); every failure
    ends the session through s.fail().  s.commit is set as soon as the commit exists, so the done marker reports it
    even when the push then fails or is cancelled.
    """
    result = _prechecks(s, repo_dir, dry_run)
    if result.refused:
        return
    if result.verdicts["staged"].status == "warn":
        say("⚠ Nothing to commit — working tree is clean. Nothing to do.")
        run_markers.note("warn", "Nothing to commit — working tree is clean.")
        return

    message = resolve_commit_message(s, cli_message)

    if dry_run:
        _dry_run_report(git.staged_paths(repo_dir), message, repo_dir)
        return

    branch = git.current_branch(repo_dir) or "HEAD"
    sha = _commit(s, repo_dir, message)
    _push(s, repo_dir, sha)

    say("\n" + "=" * 70)
    say("✅ Push complete.")
    say(f"   Branch: {branch}")
    say(f"   Commit: {sha}")
    say(f"   Remote: {git.remote_url(repo_dir) or '(no remote)'}")
    say("=" * 70)


def ring_bell():
    """Ring the terminal bell once (the session calls the same helper at the end of a run).  Best-effort once the
    toolkit loads.  Skipped in a sandbox (SANDBOX_RUNTIME=1), when JRM_PUSH_NO_BELL=1, or when stdout is not a TTY."""
    toolkit().notify.bell(silence_env="JRM_PUSH_NO_BELL")


def main(argv=None, repo_dir=REPO_DIR):
    global run_markers
    parser = argparse.ArgumentParser(
        description="Commit and push juneau-release-manager to origin.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  ./push.py "Fix credential validator NPE"
  ./push.py
  ./push.py --dry-run "Would-be commit message"
  ./push.py --console full "Fix credential validator NPE"

Steps: Prechecks (identity, review gate, staged, upstream: refuses when behind upstream, before committing), Commit,
Push.
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
    parser.add_argument(
        "--console",
        choices=("full", "condensed", "none"),
        help="Console view of the steps: full, condensed (default) or none (also JUNEAU_RUN_CONSOLE).",
    )
    args = parser.parse_args(argv)
    try:
        toolkit()
        # Before juneau_run loads: the flag beats JUNEAU_RUN_CONSOLE, which beats the condensed default.
        run.export_console(args.console)
        jr, _ = bootstrap.load(REPO_DIR / "pom.xml")
    except bootstrap.BootstrapError as e:
        print(f"❌ ERROR: {e}", file=sys.stderr)
        print("   (juneau-run 10.0.0-SNAPSHOT must be in ~/.m2: build Juneau first.)", file=sys.stderr)
        return 1
    except ValueError as e:
        print(f"❌ ERROR: {e}", file=sys.stderr)
        return 2
    run_markers = jr if jr is not None else _NoMarkers()

    with run.session("🚀 juneau-release-manager push", header=(git.current_branch(repo_dir) or "HEAD",), bell=True,
                     silence_env="JRM_PUSH_NO_BELL", mode="push", project="juneau-release-manager",
                     repo=repo_dir, exit=False,
                     plan=[("prechecks", "Prechecks"), ("commit", "Commit"), ("push", "Push")]) as s:
        if not run_markers.console_active():
            say("=" * 70)
            say("📦 juneau-release-manager push")
            say("=" * 70)
        commit_and_push(s, repo_dir, args.message, dry_run=args.dry_run)
    return s.code


if __name__ == "__main__":
    sys.exit(main())
