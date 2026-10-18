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
A thin layer over juneau_run.  With juneau_run not loaded (a plain run) every function falls back to print and
subprocess.  juneau_run is looked up on every call (sys.modules["juneau_run"]), never cached.
"""

import contextlib
import dataclasses
import os
import re
import signal
import subprocess
import time
from types import MappingProxyType

from . import _base, git, notify
from . import checks as _checks
from ._base import Result

ACTIVE_ENV = "JUNEAU_RUN_ACTIVE"
SESSION_ENV = "JUNEAU_RUN_SESSION"
PUSH_ACTIVE_ENV = "JUNEAU_PUSH_ACTIVE"
N_BASE_ENV = "JUNEAU_RUN_N_BASE"
CONSOLE_ENV = "JUNEAU_RUN_CONSOLE"
CONSOLE_MODES = ("full", "condensed", "none")
PROTOCOL_ENV = ("RUN_MARKERS", "RUN_ARTIFACTS")
# The gate-script contract (Juneau scripts/push.py:405-413 at HEAD): 0 pass, 2 fail, 3 skip.  Not pytest's.
GATE_CODES = MappingProxyType({0: "pass", 2: "fail", 3: "skip"})
_ROW_STATUS = {"pass": "ok", "skip": "skip"}   # a gate status as a row status; anything else is "fail"

_n = 0              # the session's step counter: the last step number handed out
_session = None     # the open Session, if any


def _next_n(n=None, count=1):
    """The number for the next step (or the first of count reserved numbers).  An explicit n is used as given and
    moves the counter past it."""
    global _n
    if n is not None:
        _n = max(_n, n + count - 1)
        return n
    first = _n + 1
    _n += count
    return first


def routed():
    """True when commands should go through juneau_run's run_tool: markers on, or a console view active.  Public
    because Foundry's run_tests also needs it (Decision Q4, revised in the review triage)."""
    jr = _base.jr()
    return jr is not None and (jr.enabled() or jr.console_active())


def console_requested():
    """True when juneau_run is loaded and its sinks aren't the default (JUNEAU_RUN_CONSOLE=condensed and the like), so
    session() opens a console view."""
    jr = _base.jr()
    return jr is not None and not jr.Sinks().is_default


def export_console(flag=None, *, default="condensed"):
    """Resolve the console mode and export it as JUNEAU_RUN_CONSOLE, so the session and child scripts use it: flag
    (the --console value, None when not given), else JUNEAU_RUN_CONSOLE, else default.  Call it before session().
    An unknown mode raises ValueError.  Returns the mode."""
    value = flag or os.environ.get(CONSOLE_ENV) or default
    if value not in CONSOLE_MODES:
        raise ValueError(f"console must be one of {', '.join(CONSOLE_MODES)}, not {value!r}")
    os.environ[CONSOLE_ENV] = value
    return value


def add_console_argument(parser):
    """Add --console {full,condensed,none} (default None, so export_console(args.console) applies the precedence)."""
    parser.add_argument("--console", choices=CONSOLE_MODES, default=None,
                        help="console output: full, condensed or none (also JUNEAU_RUN_CONSOLE; the flag wins)")


def paused():
    """jr.paused() when juneau_run is loaded: the console view is cleared for the block, so a prompt or an
    interactive child isn't drawn over.  A no-op context otherwise."""
    jr = _base.jr()
    return jr.paused() if jr is not None else contextlib.nullcontext()


def say(text="", level="info", *, step=None):
    """print() in a plain run; jr.say under markers or the console (a filtered note when the view owns the screen)."""
    jr = _base.jr()
    if jr is None:
        print(text, flush=True)
    else:
        jr.say(text, level, step)


class _NullStep:
    status = "ok"
    exit = None
    summary = None

    def fail(self, exit=None):  # NOSONAR python:S5806 - mirrors juneau_run.Step.fail
        pass

    def skip(self, reason=None):
        pass

    def warn(self, text):
        """Mirrors juneau_run.Step.warn: in a plain run the text is printed."""
        print(f"⚠ {text}", flush=True)


@contextlib.contextmanager
def step(id, title, *, n=None, parent=None, label=None):  # NOSONAR python:S5806 - mirrors juneau_run.step
    """jr.step around the block when juneau_run is loaded (a marker step and a console row); a null step otherwise.
    A child step (parent set) defaults to n=1; a top-level one takes the session counter's next number."""
    jr = _base.jr()
    if jr is None:
        yield _NullStep()
        return
    num = (n if n is not None else 1) if parent is not None else _next_n(n)
    with jr.step(id, num, title, parent=parent, label=label) as s:
        yield s


def _slug(title):
    # 60, not 64: a gate's child row appends "-run" and juneau_run's step ids allow 64 characters.
    return re.sub(r"[^a-z0-9]+", "-", title.lower()).strip("-")[:60] or "step"


def cmd(cmd, *, title, id=None, n=None, tool=None, cwd=None, env=None, capture=False, tty=False,  # NOSONAR python:S5806
        summarize=None):
    """Run cmd as one step.  Under markers or the console it goes through jr.run_tool with the named parser (or a
    jr.Parser instance); otherwise a plain subprocess.run.  A tool that can't start gives Result(False, 127, err).
    summarize(text) gives the step_end summary; it is called after the command exits, while its row is still open
    (so jr.history_series attaches to it).  With summarize, the command goes through run_tool whenever juneau_run is
    loaded, markers or not; without juneau_run, summarize is never called."""
    _base.remember(title)
    if routed() or (summarize is not None and _base.jr() is not None):
        return _routed(cmd, title=title, id=id or _slug(title), n=_next_n(n), parent=None, tool=tool, cwd=cwd,
                       env=env, capture=capture, tty=tty, summarize=summarize)
    _next_n(n)
    return _plain(cmd, cwd=cwd, env=env, capture=capture)


_cmd = cmd  # gate()'s first parameter is also called cmd


def _plain(cmd, *, cwd, env, capture):
    t0 = time.monotonic()
    try:
        done = subprocess.run(cmd, cwd=cwd, env=env, capture_output=capture, text=capture or None,
                              errors="replace" if capture else None)
    except OSError as e:
        return Result(False, 127, err=str(e), ms=int((time.monotonic() - t0) * 1000))
    return Result(done.returncode == 0, done.returncode, out=done.stdout or "", err=done.stderr or "",
                  ms=int((time.monotonic() - t0) * 1000))


def _routed(cmd, *, title, id, n, parent, tool, cwd, env, capture, tty, statuses=None,  # NOSONAR python:S5806
            summarize=None):
    jr = _base.jr()
    parser = tool if tool is not None else "generic"
    if isinstance(parser, str) and parser not in jr.PARSERS:
        parser = "generic"
    try:
        r = jr.run_tool(cmd, parser, id, title, n=n, parent=parent, cwd=cwd, env=env, capture=capture, tty=tty,
                        statuses=statuses, summarize=summarize)
    except OSError as e:
        # run_tool has already ended the step with exit 127.  Close it again with no exit field, as Foundry does
        # (scripts/push.py:254-262), whose tests pin that marker.
        jr.emit("end", id=id, status="fail", ms=0)
        return Result(False, 127, err=str(e))
    return Result(r.exit == 0, r.exit, out=(r.output or "") if capture else "", ms=r.ms, summary=r.summary or None)


def gate(cmd, *, title, codes=GATE_CODES, label=None, **cmd_kwargs):
    """Run a gate script and map its exit code through codes (unknown codes are "error").  ok is True for pass and
    skip; Result.status says which.  Under markers or the console the gate is a step with the command as its child
    row, and both rows show the gate's status: codes goes to run_tool as statuses, so a skip is "skip" on the child
    row as well (Decision Q7).  label is the step's short console label."""
    if not routed():
        r = _cmd(cmd, title=title, **cmd_kwargs)
        status = codes.get(r.code, "error")
        return dataclasses.replace(r, ok=status in ("pass", "skip"), status=status)
    _base.remember(title)
    step_id = cmd_kwargs.get("id") or _slug(title)
    with step(step_id, title, n=cmd_kwargs.get("n"), label=label) as st:
        r = _routed(cmd, title=title, id=f"{step_id}-run", n=1, parent=step_id,
                    tool=cmd_kwargs.get("tool") or "generic", cwd=cmd_kwargs.get("cwd"), env=cmd_kwargs.get("env"),
                    capture=cmd_kwargs.get("capture", False), tty=cmd_kwargs.get("tty", False),
                    statuses={code: _ROW_STATUS.get(status, "fail") for code, status in codes.items()})
        status = codes.get(r.code, "error")
        if status == "skip":
            st.skip()
        elif status != "pass":
            st.fail(exit=r.code)
    return dataclasses.replace(r, ok=status in ("pass", "skip"), status=status)


@contextlib.contextmanager
def handoff(*, reserve=0):
    """Give the terminal to a child script that joins this run.  Yields the environment additions for the child:
    JUNEAU_PUSH_ACTIVE=1, JUNEAU_RUN_N_BASE when markers are on and reserve > 0, and jr.handoff()'s label width and
    detail level.  Pass env={**os.environ, **extra}; os.environ is not changed."""
    extra = {PUSH_ACTIVE_ENV: "1"}
    jr = _base.jr()
    if jr is None:
        yield extra
        return
    if jr.enabled() and reserve > 0:
        extra[N_BASE_ENV] = str(_next_n(count=reserve) - 1)
    with jr.handoff() as values:
        yield {**values, **extra}


def child_env(*, strip_protocol=True, keep=("JUNEAU_RUN_PY",)):
    """A copy of os.environ for a child that must run as though standalone: without RUN_MARKERS, RUN_ARTIFACTS,
    JUNEAU_PUSH_ACTIVE and every JUNEAU_RUN_* variable, except those in keep."""
    env = dict(os.environ)
    if strip_protocol:
        for key in list(env):
            if key not in keep and (key in PROTOCOL_ENV or key == PUSH_ACTIVE_ENV or key.startswith("JUNEAU_RUN_")):
                del env[key]
    return env


@dataclasses.dataclass(frozen=True)
class CheckResult:
    """checks()'s answer: ok is False when a check failed (never in a dry run); verdicts maps each check's name to
    its checks.Verdict, in check order.  refused is True when any check failed, including the dry-run fails that
    became "would refuse" warns; consumers use it instead of matching the reason's wording."""
    ok: bool
    verdicts: dict
    refused: bool = False


_VERDICT_ICON = {"pass": "✔", "warn": "⚠", "fail": "✗"}


def checks(id, title, checks, *, label="Prechecks", n=None, dry_run=False):  # NOSONAR python:S5806 - mirrors step()
    """
    Run gate checks (juneau_scripts.checks.Check) as one step: one bar cell per check (green pass, yellow warn, red
    fail), the running check's name as the row's progress, and when any check fails or warns the row's reason (the
    first one's, plus " +N" for the others) and every hint beneath the row, each headed "<name>: ".  Every check runs,
    even after a failure.  A check that raises fails with "could not check <name>: <error>".  A failure fails the
    row.  With dry_run each fail is a warn whose reason starts "would refuse: ", the hints still print, and the
    result is ok.

    The step is one marker step with a note per warn or fail.  Without a console view (a plain run, markers, or
    --console full) each check prints a line instead: "✔ <name>", "⚠ <name>: <reason>" or "✗ <name>: <reason>",
    then the hints.  Returns CheckResult(ok, verdicts, refused).
    """
    checks = list(checks)
    _base.remember(title)
    with step(id, title, n=n, label=label) as st:
        result = drive_checks(id, st, checks, dry_run=dry_run)
    return result


def drive_checks(id, st, checks, *, dry_run=False):  # NOSONAR python:S5806 - mirrors step()
    """The body of checks() for a step that is already open (id, and st the step it yielded): runs the checks on
    its row, fails st when one fails, and returns the CheckResult.  Registry.checks uses it."""
    jr = _base.jr()
    verdicts = {}
    if jr is None:
        verdicts = {c.name: _checks.evaluate(c, dry_run=dry_run) for c in checks}
        _print_verdicts(verdicts)
    else:
        jr.publish("units", step=id, total=len(checks), kind="checks")
        for i, c in enumerate(checks):
            jr.publish("step_progress", step=id, text=c.name)
            jr.tick()
            v = verdicts[c.name] = _checks.evaluate(c, dry_run=dry_run)
            jr.publish("unit", step=id, done=i + 1, outcome=v.status)
            if v.status != "pass":
                jr.emit("note", level="error" if v.status == "fail" else "warn",
                        text=f"{c.name}: {v.reason or 'failed'}", href=None, step=id)
            jr.tick()
    level, reason, hint = _checks.summary(verdicts)
    if jr is not None and not jr.console_active():
        _print_verdicts(verdicts)
    elif jr is not None and level is not None:
        jr.publish("note", level=level, text=reason, href=None, step=id)
        if hint:
            jr.publish("hint", step=id, lines=list(hint))
    if level == "error":
        st.fail()
    refused = any(v.status == "fail" or v.refused for v in verdicts.values())
    return CheckResult(level != "error", verdicts, refused)


_run_checks = checks     # Session.checks's parameter of the same name hides it


def _print_verdicts(verdicts):
    for name, v in verdicts.items():
        say(f"{_VERDICT_ICON[v.status]} {name}" + (f": {v.reason}" if v.status != "pass" and v.reason else ""),
            "info" if v.status == "pass" else "warn" if v.status == "warn" else "error")
    for line in _checks.summary(verdicts)[2]:
        say(f"  {line}", "error")


class _SessionFailed(BaseException):
    """Raised by Session.fail; only session() catches it.  BaseException, so a script's `except Exception` can't
    swallow it."""


class Session:
    """The handle a session block gets.  Set commit as soon as a commit exists; code is the exit code once the
    block has ended."""

    def __init__(self):
        self.commit = None
        self.code = None

    def plan(self, ids):
        """Declare (or replace) the planned top-level steps, in order, for the Overall figure.  Each entry is a step
        id, or (id, label): a labelled step draws as a dim pending row until it starts.  Does nothing in a plain
        run."""
        entries = [e if isinstance(e, str) else tuple(e) for e in ids]
        ids = [e if isinstance(e, str) else e[0] for e in entries]
        # validated here even in a plain run (where jr is None) so a bad plan fails the same way everywhere
        if len(set(ids)) != len(ids):
            raise ValueError("duplicate step id in plan: " + ", ".join(sorted({i for i in ids if ids.count(i) > 1})))
        jr = _base.jr()
        if jr is not None:
            jr.plan(entries)

    def fail(self, msg, *, hint=None):
        """Print msg verbatim (level error), then the hint lines, and end the session as failed.  msg is required
        (Decision Q5): a failure always says why.  The hint goes out as a hint event, which every detail level
        shows: beneath the running row when there is one, else at once."""
        say(msg, "error")
        lines = [hint] if isinstance(hint, str) else list(hint or ())
        jr = _base.jr()
        if jr is None:
            for line in lines:
                print(line, flush=True)
        elif lines:
            jr.hint(lines)
        raise _SessionFailed

    def checks(self, id, title, checks, **kwargs):  # NOSONAR python:S5806 - mirrors run.checks
        """run.checks(...), and on a failure (never in a dry run) end the session failed with no further message:
        the row already says why."""
        result = _run_checks(id, title, checks, **kwargs)
        if not result.ok:
            raise _SessionFailed
        return result

    def require(self, result, msg=None, *, hint=None):
        """result when result.ok (a skipped gate is ok); otherwise fail with msg or a default naming the last step."""
        if result.ok:
            return result
        if msg is None:
            msg = f"❌ ERROR: {_base.last_title() or 'command'} failed (exit code {result.code})"
            last = (result.err or "").strip().splitlines()
            if last:
                msg += f"\n   {last[-1]}"
        self.fail(msg, hint=hint)


def _nested_env():
    env = os.environ
    return (env.get(ACTIVE_ENV) == "1" or env.get(SESSION_ENV) == "1" or env.get(PUSH_ACTIVE_ENV) == "1"
            or bool(env.get(N_BASE_ENV)))


def _on_sigterm(signum, frame):
    """The first SIGTERM unwinds like Ctrl-C; a second one, during the unwind, is ignored."""
    signal.signal(signal.SIGTERM, signal.SIG_IGN)
    raise KeyboardInterrupt


_NOT_INSTALLED = object()


def _install_sigterm():
    try:
        previous = signal.getsignal(signal.SIGTERM)
        signal.signal(signal.SIGTERM, _on_sigterm)
        return previous
    except ValueError:  # not the main thread
        return _NOT_INSTALLED


def _restore_sigterm(previous):
    if previous is not _NOT_INSTALLED:
        signal.signal(signal.SIGTERM, previous if previous is not None else signal.SIG_DFL)


def _done(status, commit):
    jr = _base.jr()
    if jr is not None:
        jr.done(status, commit=commit)


def _n_base():
    try:
        return max(0, int(os.environ.get(N_BASE_ENV) or 0))
    except ValueError:
        return 0


@contextlib.contextmanager
def session(title, *, header=(), bell=False, sound=False, silence_env=(), mode=None, project=None, repo=".",
            exit=True, plan=None):
    """
    One script run: the run marker, the console view when requested, a SIGTERM handler, and on every path the summary,
    exactly one done marker, the bell and the sound.  ok exits 0, s.fail(msg)/s.require() 1, Ctrl-C or SIGTERM 130, any
    other exception 1 (reported as "❌ ERROR: <Type>: <msg>").  With exit=True it raises SystemExit(code) at the end;
    with exit=False the code is left in s.code.  plan is the list of top-level step ids for the Overall figure; the run's
    history is recorded for repo (JUNEAU_RUN_HISTORY=0 turns it off).
    """
    global _session, _n
    if _session is not None:
        raise RuntimeError("a juneau_scripts session is already open in this process")
    import juneau_scripts
    juneau_scripts.check_bootstrap()
    started = time.monotonic()
    nested = _nested_env()
    saved = {k: os.environ.get(k) for k in (ACTIVE_ENV, SESSION_ENV)}
    _n = _n_base()
    s = _session = Session()
    previous = _NOT_INSTALLED
    status, code = "ok", 0
    try:
        previous = _install_sigterm()
        try:
            jr = _base.jr()
            if jr is not None:
                jr.history_start(repo)
            if jr is not None and jr.enabled():
                jr.run(mode, project, git.current_branch(repo) or "HEAD", git.short_head(repo))
            if console_requested():
                jr.session(title, header, repo=repo)
            if plan is not None:
                s.plan(plan)
            yield s
        except _SessionFailed:
            status, code = "fail", 1
        except KeyboardInterrupt:
            say("\n❌ Cancelled.", "error")
            status, code = "cancelled", 130
        except SystemExit as e:
            code = e.code if isinstance(e.code, int) else (0 if e.code is None else 1)
            if e.code is not None and not isinstance(e.code, int):
                say(f"\n❌ ERROR: {e.code}", "error")
            status = "ok" if code == 0 else "fail"
        except Exception as e:  # NOSONAR python:S2221 - the session reports every uncaught error the same way
            say(f"\n❌ ERROR: {type(e).__name__}: {e}", "error")
            status, code = "fail", 1
        jr = _base.jr()
        if not nested and not (jr is not None and jr.console_active()):
            word = {"ok": "ok", "cancelled": "cancelled"}.get(status, f"failed, exit code {code}")
            say(f"{'✅' if status == 'ok' else '❌'} {title}: {word} ({time.monotonic() - started:.1f}s)")
        _done(status, s.commit)
        if not nested:
            if bell:
                notify.bell(silence_env=silence_env)
            if sound:
                notify.sound(status == "ok", silence_env=silence_env)
    finally:
        _session = None
        for key, value in saved.items():
            if value is None:
                os.environ.pop(key, None)
            else:
                os.environ[key] = value
        _restore_sigterm(previous)
    s.code = code
    if exit:
        raise SystemExit(code)
