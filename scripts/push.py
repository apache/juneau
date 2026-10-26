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

"""Build, test, commit and push juneau-release-manager.  Full docs: push.md"""

import argparse
import os
import shutil
import subprocess
import sys
from pathlib import Path

REPO_DIR = Path(__file__).resolve().parent.parent

# Set in the pytest child so a test that reaches the Python Tests step again cannot launch the suite recursively.
SCRIPT_TESTS_ACTIVE = "JRM_SCRIPT_TESTS_ACTIVE"
# Where the canonical Juneau checkout keeps juneau_terminal.py; test_release_cli.py skips without JUNEAU_TERMINAL_PY.
JUNEAU_TERMINAL_PY_DEFAULT = Path.home() / "git" / "apache" / "juneau" / "juneau-terminal" / "src" / "main" / "python" / "juneau_terminal.py"
# The personal JDK layout (~/jdk/default -> the current JDK 17), the default e2e/playwright.config.ts also uses.
DEFAULT_JAVA_HOME = Path.home() / "jdk" / "default"
# Variables e2e/playwright.config.ts reads that point the suite away from the tree under test; a stray exported value
# must not make the gate pass or fail against something else.  REUSE reuses a server already on the test port,
# BASE_URL turns off the webServer and targets a live URL, HOME makes the suite use an existing release home instead
# of a fresh temporary one.
E2E_REDIRECT_ENV = ("JRM_E2E_REUSE", "JRM_E2E_BASE_URL", "JRM_E2E_HOME")
# Where bootstrap extracts the toolkit when ~/.m2 lacks it.  Outside target/ because Compile runs `mvn clean`, which
# would delete the extracted copy before the later (lazy) imports, the commit and the push.
TOOLKIT_CACHE_DIR = Path.home() / ".cache" / "juneau-terminal" / "juneau-release-manager"

# bootstrap.py sits beside this file.  A direct run already has scripts/ on sys.path; an importlib load (the tests)
# does not, so keep the insert.
sys.path.insert(0, str(REPO_DIR / "scripts"))

import bootstrap  # noqa: E402
from bootstrap import run_requested  # noqa: E402,F401  (push.run_requested is the environment rule tests check)

# The toolkit modules, bound by toolkit() on first use so that importing this file has no side effects.
git = run = notify = checks = tools = None


class _NoMarkers:
    """Stands in for juneau_terminal when it isn't loaded: the calls this script makes directly are no-ops or False."""

    def console_active(self):
        return False

    def note(self, *args, **kwargs):
        # Intentionally empty: markers are off, so there is nothing to record.
        pass


# The terminal-protocol module in use: juneau_terminal when the environment asks for it and it loads, otherwise the stand-in.
run_markers = _NoMarkers()


def toolkit():
    """Load juneau_terminal_scripts (once per process) and bind git, run, notify, checks and tools.  Returns the package."""
    global git, run, notify, checks, tools
    # cache_dir: the first load fixes it for the process; not under target/, which Compile cleans mid-run.
    _, js = bootstrap.load(REPO_DIR / "pom.xml", terminal=False, cache_dir=TOOLKIT_CACHE_DIR)
    from juneau_terminal_scripts import checks, git, notify, run, tools  # noqa: F811
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


def script_tests_launcher():
    """The command prefix that runs pytest, or None.  `python3 -m pytest` when pytest is importable, else
    `uv run --native-tls --with pytest pytest` when uv is on the PATH.  Nothing is ever installed."""
    python = shutil.which("python3") or sys.executable
    try:
        importable = subprocess.run([python, "-c", "import pytest"], capture_output=True).returncode == 0
    except OSError:
        importable = False
    if importable:
        return [python, "-m", "pytest"]
    if shutil.which("uv"):
        return ["uv", "run", "--native-tls", "--with", "pytest", "pytest"]
    return None


def script_tests_command(launcher):
    return [*launcher, "-q", "-p", "no:cacheprovider", "scripts/tests"]


def script_tests_env():
    """The child's environment: the recursion guard, no bytecode, and JUNEAU_TERMINAL_PY when it is unset and findable."""
    # child_env: the suite must not inherit this run's markers or console (JUNEAU_TERMINAL_CONSOLE, JUNEAU_TERMINAL_SESSION, ...).
    env = dict(run.child_env(), PYTHONDONTWRITEBYTECODE="1", **{SCRIPT_TESTS_ACTIVE: "1"})
    if not env.get("JUNEAU_TERMINAL_PY") and JUNEAU_TERMINAL_PY_DEFAULT.is_file():
        env["JUNEAU_TERMINAL_PY"] = str(JUNEAU_TERMINAL_PY_DEFAULT)
    return env


def _skip_script_tests(why, short):
    """The Python Tests row, opened and skipped, so it does not stay pending."""
    with run.step("script-tests", "Python Tests") as st:  # NOSONAR python:S1192 - a step title reads more clearly inline
        say(f"⚠ Skipping Python Tests: {why}.")
        run_markers.note("warn", f"Python Tests skipped: {short}.")
        st.skip(short)


def _script_tests(s, repo_dir, dry_run, launcher=None):
    """
    The Python Tests row: the scripts/tests pytest suite, run before the commit.  A failure refuses the push -- nothing
    is committed -- except under --dry-run, where it is a "would refuse" warning.  Skipped, with a notice, when already
    inside the suite (the recursion guard) or when neither pytest nor uv is available.  Returns True when the push may
    go on.
    """
    if os.environ.get(SCRIPT_TESTS_ACTIVE):
        _skip_script_tests("already running inside them (no recursion)", "already inside the suite")
        return True
    launcher = launcher or script_tests_launcher()
    if launcher is None:
        _skip_script_tests("neither pytest nor uv found (python3 -m pytest / uv run --native-tls --with pytest)",
                           "neither pytest nor uv found")
        return True
    cmd = script_tests_command(launcher)
    say("\n🐍 Running Python Tests (scripts/tests)...")
    say(f"Running: {' '.join(cmd)}")
    r = run.cmd(cmd, title="Python Tests", id="script-tests", tool="pytest", cwd=str(repo_dir), env=script_tests_env(),
                capture=True)
    if r.ok:
        say("✅ Python Tests passed!")
        return True
    if r.out:
        say(r.out[-4000:])
    if dry_run:
        say(f"⚠ Would refuse to push: Python Tests failed (exit code {r.code}).")
        run_markers.note("warn", f"Dry run — would refuse to push: Python Tests failed (exit code {r.code}).")
        return False
    s.fail(f"\n❌ ERROR: Python Tests failed (exit code {r.code}); nothing committed.")
    return False


def java_home():
    """JAVA_HOME as set, else ~/jdk/default when that directory exists, else None (the PATH's java is used)."""
    return os.environ.get("JAVA_HOME") or (str(DEFAULT_JAVA_HOME) if DEFAULT_JAVA_HOME.is_dir() else None)


def build_env():
    """The Playwright child's environment: JAVA_HOME defaults to ~/jdk/default when unset."""
    env = dict(os.environ)
    home = java_home()
    if home:
        env["JAVA_HOME"] = home
    return env


def _would_run(step_id, title, what):
    """A build gate under --dry-run: its row is skipped and the command printed.  Not run, since it writes target/,
    binds a port and takes minutes."""
    with run.step(step_id, title) as st:
        say(f"🔍 Would run {title}: {what}")
        st.skip("dry run")


def maven_parser():
    """
    juneau_terminal's Maven parser plus the report marker it can't give a single-module build: it announces Surefire reports
    per reactor module, and a one-module build has no reactor lines.  Returns None when no terminal view is active (the
    plain "maven" parser name then applies).
    """
    jr = sys.modules.get("juneau_terminal")
    if jr is None or not run.routed():
        return None

    class SingleModuleMavenParser(jr.MavenParser):
        def finish(self, exit_code):
            events = super().finish(exit_code)
            if not self.structure_seen and (self.cwd / "target" / "surefire-reports").is_dir():
                events.append(("report", self.step_id, "surefire", "target/surefire-reports"))
            return events

    return SingleModuleMavenParser


def _maven_gate(s, repo_dir, dry_run, mvn_args, step_id, title):
    """One Maven row: mvn <mvn_args> in the repo, never `install` (this repo only consumes the Juneau snapshot already
    in ~/.m2).  A failure refuses the push: nothing is committed."""
    if dry_run:
        _would_run(step_id, title, " ".join(["mvn", *mvn_args]))
        return
    say(f"\n🔨 Running {title} (mvn {' '.join(mvn_args)})...")
    # java_home= also puts its bin on the PATH, so mvn and anything it forks agree on the JDK.
    r = tools.maven(mvn_args, cwd=str(repo_dir), java_home=java_home(), parser=maven_parser() or "maven", title=title,
                    id=step_id)
    if not r.ok:
        s.fail(f"\n❌ ERROR: {title} failed (exit code {r.code}); nothing committed.")
    say(f"✅ {title} passed!")


def _compile(s, repo_dir, dry_run):
    """The Compile row: `mvn clean test-compile`, so a compile error fails fast, before any test runs."""
    _maven_gate(s, repo_dir, dry_run, ["clean", "test-compile"], "compile", "Compile")


def _maven_tests(s, repo_dir, dry_run):
    """The Java Tests row: `mvn verify`, the Java Tests plus Apache RAT.  No clean: Compile just ran it."""
    _maven_gate(s, repo_dir, dry_run, ["verify"], "tests", "Java Tests")


def e2e_env():
    """
    The Browser Tests child's environment: build_env() without the variables in E2E_REDIRECT_ENV (they point the suite at a
    server or release home other than the tree under test), and PLAYWRIGHT_HTML_OPEN=never unless already set.
    On a terminal the playwright parser leaves PLAYWRIGHT_HTML_OPEN alone, and the html reporter's default (open on
    failure) would serve the report and hold the push open.
    """
    env = build_env()
    for name in E2E_REDIRECT_ENV:
        env.pop(name, None)
    env.setdefault("PLAYWRIGHT_HTML_OPEN", "never")
    return env


def _e2e(s, repo_dir, dry_run):
    """
    The Browser Tests row: the full Playwright suite (`npx playwright test` in e2e/).  Its webServer boots the app on the test
    port itself, so no server is started here.  The playwright parser adds its reporter through PW_TEST_REPORTER and no
    CLI arguments, so the config's own html reporter still runs.  Skipped with a warning when npx is not on the PATH;
    npm dependencies and Chromium are installed only when missing, and a failed install fails the gate.  A failing
    suite refuses the push: nothing is committed.
    """
    if dry_run:
        _would_run("e2e", "Browser Tests", "npx playwright test (in e2e/)")
        return
    if shutil.which("npx") is None:
        with run.step("e2e", "Browser Tests") as st:
            say("⚠ Skipping Browser Tests: npx not found on the PATH (install Node.js to run the Playwright suite).")
            run_markers.note("warn", "Browser Tests skipped: npx not found.")
            st.skip("npx not found")
        return
    e2e_dir = Path(repo_dir) / "e2e"
    say("\n🎭 Running Browser Tests (npx playwright test in e2e/)...")
    if not tools.playwright_ensure(e2e_dir).ok:
        s.fail("\n❌ ERROR: Browser Tests setup failed (npm dependencies or Chromium could not be installed); nothing committed.")
    r = tools.playwright([], cwd=str(e2e_dir), env=e2e_env(), title="Browser Tests", id="e2e")
    if not r.ok:
        s.fail(f"\n❌ ERROR: Browser Tests failed (exit code {r.code}); nothing committed.  Report: e2e/playwright-report/")
    say("✅ Browser Tests passed!")


def _skip_commit_and_push():
    """Under --dry-run neither Commit nor Push happens: open and skip both rows so they do not stay pending."""
    for step_id, title in (("commit", "Commit"), ("push", "Push")):
        with run.step(step_id, title) as st:
            st.skip("dry run")


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


def _skipped(flag, what):
    say(f"\n⏭️  Skipping {what} ({flag}).")


def _gates(s, repo_dir, args):
    """
    Compile, Java Tests, Python Tests and Browser Tests, in that order (every test step after Compile), each unless its skip flag is
    set.  A failing gate ends the session with nothing committed.  Returns False only for a Python Tests "would refuse" under --dry-run;
    otherwise True.
    """
    if args.skip_tests:
        _skipped("--skip-tests", "Compile and Java Tests")
    else:
        _compile(s, repo_dir, args.dry_run)
        _maven_tests(s, repo_dir, args.dry_run)
    if args.skip_pytest:
        _skipped("--skip-pytest", "Python Tests")
    elif not _script_tests(s, repo_dir, args.dry_run):
        return False
    if args.skip_e2e:
        _skipped("--skip-e2e", "Browser Tests")
    else:
        _e2e(s, repo_dir, args.dry_run)
    return True


def commit_and_push(s, repo_dir, args):
    """
    Run the Prechecks and the gates (Compile, Java Tests, Python Tests, Browser Tests), then commit whatever is already staged in
    repo_dir and push, refusing to commit or push ANYTHING if a precheck fails (identity, unreviewed content, behind
    upstream; see _prechecks()) or a gate fails.

    A fully CLEAN tree (nothing staged, nothing unreviewed) is a benign no-op, not a refusal, and runs no gate: there is
    genuinely nothing to commit, and this repo has no push-only path.  --dry-run reports the real outcome and changes
    nothing; a refusal under it is reported as "would refuse" and does not fail the run.  --test-only skips the
    Prechecks (so it works on a dirty tree), runs the gates and stops: no commit, no push.

    Returns normally on success (committed and pushed, a no-op on a clean tree, a --dry-run report or a --test-only run);
    every failure ends the session through s.fail().  s.commit is set as soon as the commit exists, so the done marker
    reports it even when the push then fails or is cancelled.
    """
    if not args.test_only:
        result = _prechecks(s, repo_dir, args.dry_run)
        if result.refused:
            return
        if result.verdicts["staged"].status == "warn":
            say("⚠ Nothing to commit — working tree is clean. Nothing to do.")
            run_markers.note("warn", "Nothing to commit — working tree is clean.")
            return

    if not _gates(s, repo_dir, args):
        if not args.test_only:
            _skip_commit_and_push()
        return

    if args.test_only:
        say("\n✅ Test-only run: nothing committed or pushed.")
        run_markers.note("info", "Test-only run: nothing committed or pushed.")
        return

    message = resolve_commit_message(s, args.message)

    if args.dry_run:
        _dry_run_report(git.staged_paths(repo_dir), message, repo_dir)
        _skip_commit_and_push()
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


def planned_steps(args):
    """The top-level steps this run will take, in order, as (id, label), for the console's pending rows and Overall
    figure.  A skip flag removes its steps; --test-only has no Prechecks, Commit or Push."""
    steps = [] if args.test_only else [("prechecks", "Prechecks")]
    steps += [(step_id, label) for step_id, label, skipped in (("compile", "Compile", args.skip_tests),
                                                               ("tests", "Java Tests", args.skip_tests),
                                                               ("script-tests", "Python Tests", args.skip_pytest),
                                                               ("e2e", "Browser Tests", args.skip_e2e)) if not skipped]
    if not args.test_only:
        steps += [("commit", "Commit"), ("push", "Push")]
    return steps


def build_parser():
    parser = argparse.ArgumentParser(
        description="Build and test juneau-release-manager, then commit the staged index and push it to origin.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python3 scripts/push.py "Fix credential validator NPE"
  python3 scripts/push.py
  python3 scripts/push.py --dry-run "Would-be commit message"
  python3 scripts/push.py --test-only
  python3 scripts/push.py --skip-e2e "Doc-only change"
  python3 scripts/push.py --console full "Fix credential validator NPE"

Steps: Prechecks (identity, review gate, staged, upstream: refuses when behind upstream, before committing), Python
tests (scripts/tests pytest), Compile (mvn clean test-compile), Java Tests (mvn verify: Java Tests and RAT), Browser Tests (npx
playwright test in e2e/), Commit, Push.  A failing gate refuses the push; nothing is committed.
        """,
    )
    parser.add_argument(
        "message",
        nargs="?",
        default=None,
        help="Commit message. If omitted, you'll be prompted for one (not used with --test-only).",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Show what would be done without making any changes (Compile, Java Tests and Browser Tests are shown, not run).",
    )
    parser.add_argument(
        "--test-only",
        action="store_true",
        help="Run the gates only: no Prechecks, no commit, no push (works on a dirty tree).",
    )
    parser.add_argument("--skip-tests", action="store_true", help="Skip Compile and Java Tests (the Maven build).")
    parser.add_argument("--skip-e2e", action="store_true", help="Skip Browser Tests (the Playwright suite).")
    parser.add_argument("--skip-pytest", action="store_true", help="Skip Python Tests (the scripts/tests suite).")
    parser.add_argument(
        "--console",
        choices=("full", "board", "none"),
        help="Console view of the steps: full, board (default) or none (also JUNEAU_TERMINAL_CONSOLE).",
    )
    return parser


def main(argv=None, repo_dir=REPO_DIR):
    global run_markers
    args = build_parser().parse_args(argv)
    try:
        toolkit()
        # Before juneau_terminal loads: the flag beats JUNEAU_TERMINAL_CONSOLE, which beats the board default.
        run.export_console(args.console)
        jr, _ = bootstrap.load(REPO_DIR / "pom.xml")
    except bootstrap.BootstrapError as e:
        print(f"❌ ERROR: {e}", file=sys.stderr)
        print("   (juneau-terminal 10.0.0-SNAPSHOT must be in ~/.m2: build Juneau first.)", file=sys.stderr)
        return 1
    except ValueError as e:
        print(f"❌ ERROR: {e}", file=sys.stderr)
        return 2
    run_markers = jr if jr is not None else _NoMarkers()

    with run.session("Test" if args.test_only else "Push",
                     header=("JRM", git.current_branch(repo_dir) or "HEAD"),
                     lines=(f"Commit Message: {args.message}",) if args.message else (), bell=True,
                     silence_env="JRM_PUSH_NO_BELL", mode="test" if args.test_only else "push",
                     project="juneau-release-manager", repo=repo_dir, exit=False, plan=planned_steps(args)) as s:
        if not run_markers.console_active():
            say("=" * 70)
            say("📦 juneau-release-manager push")
            if args.test_only:
                say("🧪 TEST-ONLY MODE (--test-only): the gates only; nothing is committed or pushed.")
            say("=" * 70)
        commit_and_push(s, repo_dir, args)
    return s.code


if __name__ == "__main__":
    sys.exit(main())
