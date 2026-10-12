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
from ._base import Result

ACTIVE_ENV = "JUNEAU_RUN_ACTIVE"
SESSION_ENV = "JUNEAU_RUN_SESSION"
PUSH_ACTIVE_ENV = "JUNEAU_PUSH_ACTIVE"
N_BASE_ENV = "JUNEAU_RUN_N_BASE"
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

    def skip(self):
        pass


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


def cmd(cmd, *, title, id=None, n=None, tool=None, cwd=None, env=None, capture=False, tty=False):  # NOSONAR python:S5806
    """Run cmd as one step.  Under markers or the console it goes through jr.run_tool with the named parser (or a
    jr.Parser instance); otherwise a plain subprocess.run.  A tool that can't start gives Result(False, 127, err)."""
    _base.remember(title)
    if routed():
        return _routed(cmd, title=title, id=id or _slug(title), n=_next_n(n), parent=None, tool=tool, cwd=cwd,
                       env=env, capture=capture, tty=tty)
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


def _routed(cmd, *, title, id, n, parent, tool, cwd, env, capture, tty, statuses=None):  # NOSONAR python:S5806
    jr = _base.jr()
    parser = tool if tool is not None else "generic"
    if isinstance(parser, str) and parser not in jr.PARSERS:
        parser = "generic"
    try:
        r = jr.run_tool(cmd, parser, id, title, n=n, parent=parent, cwd=cwd, env=env, capture=capture, tty=tty,
                        statuses=statuses)
    except OSError as e:
        # run_tool has already ended the step with exit 127.  Close it again with no exit field, as Foundry does
        # (scripts/push.py:254-262), whose tests pin that marker.
        jr.emit("end", id=id, status="fail", ms=0)
        return Result(False, 127, err=str(e))
    return Result(r.exit == 0, r.exit, out=(r.output or "") if capture else "", ms=r.ms, summary=r.summary or None)


def gate(cmd, *, title, codes=GATE_CODES, **cmd_kwargs):
    """Run a gate script and map its exit code through codes (unknown codes are "error").  ok is True for pass and
    skip; Result.status says which.  Under markers or the console the gate is a step with the command as its child
    row, and both rows show the gate's status: codes goes to run_tool as statuses, so a skip is "skip" on the child
    row as well (Decision Q7)."""
    if not routed():
        r = _cmd(cmd, title=title, **cmd_kwargs)
        status = codes.get(r.code, "error")
        return dataclasses.replace(r, ok=status in ("pass", "skip"), status=status)
    _base.remember(title)
    step_id = cmd_kwargs.get("id") or _slug(title)
    with step(step_id, title, n=cmd_kwargs.get("n")) as st:
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


class _SessionFailed(BaseException):
    """Raised by Session.fail; only session() catches it.  BaseException, so a script's `except Exception` can't
    swallow it."""


class Session:
    """The handle a session block gets.  Set commit as soon as a commit exists; code is the exit code once the
    block has ended."""

    def __init__(self):
        self.commit = None
        self.code = None

    def fail(self, msg, *, hint=None):
        """Print msg verbatim (level error), then the hint lines, and end the session as failed.  msg is required
        (Decision Q5): a failure always says why."""
        say(msg, "error")
        for line in [hint] if isinstance(hint, str) else hint or ():
            say(line)
        raise _SessionFailed

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
            exit=True):
    """
    One script run: the run marker, the console view when requested, a SIGTERM handler, and on every path the summary,
    exactly one done marker, the bell and the sound.  ok exits 0, s.fail(msg)/s.require() 1, Ctrl-C or SIGTERM 130, any
    other exception 1 (reported as "❌ ERROR: <Type>: <msg>").  With exit=True it raises SystemExit(code) at the end;
    with exit=False the code is left in s.code.
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
            if jr is not None and jr.enabled():
                jr.run(mode, project, git.current_branch(repo) or "HEAD", git.short_head(repo))
            if console_requested():
                jr.session(title, header)
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
