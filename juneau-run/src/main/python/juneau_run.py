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
Run wrapper: run-protocol v1 markers, output routing and tool-output parsers for Maven, pytest, Playwright, Gradle and npm.

Stdlib-only and standalone, so it can be copied or loaded from anywhere.  Every marker function is a no-op
unless the environment has RUN_MARKERS=1; the open-line helpers (open_line, append, dot, set_tail, close_line)
always write.  A marker always starts on a fresh line.  Sections, in order: markers, sinks, parsers, runner, CLI.

CLI:  python3 juneau_run.py [--console full|condensed|none] [--full-log PATH] [--condensed-log PATH]
                            [--step ID] [--title T] [--pty --events PATH [--size CxR] [--input stdin]]
                            <maven|pytest|playwright|gradle|npm|generic> -- <cmd...>

With --pty the tool runs under a pseudo-terminal of the given size (default 120x40); its raw bytes go to --full-log,
the size to <full-log>.size, and run-view events (not ##run markers) to the --events JSONL file.  POSIX only.
--input stdin   with --pty: copy stdin to the job's terminal
"""

import argparse
import atexit
import codecs
import collections
import errno
import fcntl
import glob
import hashlib
import importlib
import importlib.util
import json
import os
import re
import select
import shlex
import shutil
import signal
import struct
import subprocess
import sys
import tempfile
import termios
import threading
import time
import tty as _tty
import unicodedata
import xml.etree.ElementTree as ET
from collections import OrderedDict
from contextlib import contextmanager, suppress
from pathlib import Path

PREFIX = "##run "
MAX_BYTES = 65536
ACTIVE_ENV = "JUNEAU_RUN_ACTIVE"
GRACE_SECONDS = 5.0
PROMPTS_GRACE_SECONDS = 10.0   # --prompts stdio: SIGTERM to SIGKILL once the host has gone
MAX_PROMPT_LINE = 1 << 20
TESTS_INTERVAL = 0.5

# ---------------------------------------------------------------------------------------------------------------------
# Markers
# ---------------------------------------------------------------------------------------------------------------------

_owns_run = False
_at_line_start = True   # whether the last character this process put on stdout was "\n" (or nothing was written)
_head = None            # the open line's head, for set_tail
_events = None          # the --events writer in PTY mode; when set, every marker goes there instead of stdout
_line_offset = 0        # in PTY mode, the log offset of the start of the line being parsed
_subscribers = []       # bus subscribers, called as fn(kind, fields)
_console = None         # the session's Console, when run.session() has been called
_pause_depth = 0        # nested _paused() blocks (an ask inside a handoff); only the outermost one resumes
_history = None         # the run's History subscriber, once history_start() has installed it


def enabled():
    """True only when RUN_MARKERS is exactly "1"."""
    return os.environ.get("RUN_MARKERS") == "1"


def emit(ev, **fields):
    """Print one marker line: `##run {"ev":...}`.  None-valued fields are omitted; oversized markers are dropped.

    In PTY mode the marker is translated into a run-view event and written to the --events file instead.
    """
    if _events is not None:
        _events.write(ev, {k: v for k, v in fields.items() if v is not None})
        return
    if not enabled():
        return
    obj = {"ev": ev, **{k: v for k, v in fields.items() if v is not None}}
    line = PREFIX + json.dumps(obj, ensure_ascii=False, separators=(",", ":"))
    if len(line.encode("utf-8")) > MAX_BYTES:
        return
    _out(line + "\n" if _at_line_start else "\n" + line + "\n")


def _out(text):
    """Write text to stdout, flush, and record whether stdout now ends a line."""
    global _at_line_start
    if not text:
        return
    sys.stdout.write(text)
    sys.stdout.flush()
    _at_line_start = text.endswith("\n")


def _colour_stdout():
    """True when stdout is a terminal and NO_COLOR is unset."""
    return not os.environ.get("NO_COLOR") and getattr(sys.stdout, "isatty", lambda: False)()


def _seen(ends_line):
    """Record the line state after output written to stdout by other means (the raw tee, condensed lines)."""
    global _at_line_start
    _at_line_start = ends_line


def open_line(text):
    """Start an open output line whose head is text, ending any line still open.  Always writes."""
    global _head
    if not _at_line_start:
        _out("\n")
    _head = text
    _out(text)


def append(text):
    """Extend the open line.  Always writes."""
    _out(text)


def dot():
    """Same as append(".")."""
    _out(".")


def set_tail(text):
    """Rewrite everything after the open line's head: writes a bare \\r, the head and text.  Always writes."""
    _out("\r" + (_head or "") + text)


def close_line(text=""):
    """Append text and end the open line.  Always writes."""
    global _head
    _head = None
    _out(text + "\n")


def _nested():
    """True when an enclosing process owns the run (JUNEAU_RUN_ACTIVE inherited), so run()/done() must stay quiet."""
    return os.environ.get(ACTIVE_ENV) == "1" and not _owns_run


def run(mode, project, branch, head):
    """First marker of a run.  mode is "push" or "test".  A no-op inside a process that inherited an active run."""
    global _owns_run
    if _nested() or not enabled():
        return
    emit("run", v=1, mode=mode, project=project, branch=branch, head=head)
    _owns_run = True
    os.environ[ACTIVE_ENV] = "1"


class Step:
    """Mutable outcome of a step() block; defaults to ok.  summary is the console row's text, when set."""

    def __init__(self, id=None):  # NOSONAR python:S5806 - parameter names mirror protocol field names
        self.id = id
        self.status = "ok"
        self.exit = None
        self.summary = None
        self.warned = False
        self.paused = False

    def fail(self, exit=None):  # NOSONAR python:S5806 - parameter names mirror protocol field names
        self.status = "fail"
        self.exit = exit

    def skip(self, reason=None):
        """End the step skipped; reason, when given, is the row's text."""
        self.status = "skip"
        if reason is not None:
            self.summary = reason

    def warn(self, text):
        """The step still ends ok, but its row draws ⚠ with text as its reason: a warn note on the step."""
        self.warned = True
        note("warn", text, step=self.id)
        if not _console_owns_screen():
            _out(f"⚠ {text}\n")

    def pause(self, text):
        """The step ends ok but its row draws ⏸ with text: bus-only, no run-protocol marker."""
        self.paused = True
        publish("note", level="pause", text=text, href=None, step=self.id)

    def detail(self, text):
        """A line shown beside the row's result, not instead of it (unlike warn, whose text becomes the reason).
        Bus-only, no run-protocol marker; the row's status is unchanged."""
        publish("note", level="detail", text=text, href=None, step=self.id)


@contextmanager
def step(id, n, title, *, parent=None, label=None):  # NOSONAR python:S5806 - parameter names mirror protocol field names
    """Emit `step`, run the block, then emit `end` with its status and duration.  An exception ends it as fail and propagates."""
    emit("step", id=id, n=n, title=title, parent=parent)
    publish("step_start", id=id, n=n, title=title, parent=parent, tool=None, sig="",
            **({"label": label} if label else {}))
    s = Step(id)
    t0 = time.monotonic()
    try:
        yield s
    except BaseException:
        s.status = "fail"
        raise
    finally:
        ms = int((time.monotonic() - t0) * 1000)
        emit("end", id=id, status=s.status, ms=ms, exit=s.exit)
        publish("step_end", id=id, status=s.status, ms=ms, exit=s.exit, summary=s.summary, totals=None)


def report(step, kind, path, *, cwd=None):
    """Point the runner at a step's test results (kind: surefire, junitxml, jest-json, playwright-json).  With markers
    on and $RUN_ARTIFACTS set, results outside it are copied under it and the copy is announced, so a later `clean`
    cannot take them away.  A relative path is taken from cwd (default the current directory)."""
    path = str(path)
    if enabled() and _events is None and artifacts_dir() is not None:
        path = _keep_report(step, kind, path, cwd)
    emit("report", step=step, kind=kind, path=path)


def _keep_report(step, kind, path, cwd):
    """Copy a report under $RUN_ARTIFACTS/reports/<step>/ (a glob's matches into a directory named for kind) and return
    the copy's path.  path itself when it is already under $RUN_ARTIFACTS, matches nothing, or cannot be copied (with
    a warning).  A name another module or a rerun has taken gets a -2, -3, ... suffix."""
    art = artifacts_dir()
    src = Path(cwd or ".") / path   # an absolute path stays as it is
    if src.resolve().is_relative_to(art.resolve()):
        return path
    sources = [src] if src.exists() else [Path(p) for p in sorted(glob.glob(str(src)))]
    if not sources:
        return path
    dest = _free_path(art / "reports" / _event_id(step) / (src.name if sources == [src] else kind))
    try:
        if sources == [src]:
            _copy_report(src, dest)
        else:
            for source in sources:
                _copy_report(source, _free_path(dest / source.name))
    except OSError as e:    # shutil.Error is one
        warn(f"could not copy the {kind} report {path} to {dest}: {e}")
        return path
    return str(dest)


def _copy_report(src, dest):
    if src.is_dir():
        shutil.copytree(src, dest)
    else:
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dest)


def _free_path(p):
    """p, or the first of p-2, p-3, ... (before its suffix) that does not exist."""
    candidate, i = p, 2
    while candidate.exists():
        candidate, i = p.with_name(f"{p.stem}-{i}{p.suffix}"), i + 1
    return candidate


def note(level, text, href=None, step=None):
    """A notable event (level: info, warn, error).  A non-http(s) href is dropped."""
    if href is not None and not href.startswith(("http://", "https://")):
        href = None
    emit("note", level=level, text=text, href=href, step=step)
    publish("note", level=level, text=text, href=href, step=step)


def tests(step, total, fail, err, skip):
    """Absolute cumulative test totals for a step; the latest event for a step supersedes earlier ones."""
    emit("tests", step=step, total=total, fail=fail, err=err, skip=skip)
    publish("tests", step=step, total=total, fail=fail, err=err, skip=skip)


def done(status, commit=None):
    """Last marker of a run (status: ok, fail, cancelled).  The marker is a no-op inside a process that inherited an
    active run; the bus event is always published, so a console view can end."""
    publish("done", status=status, commit=commit)
    if _nested():
        return
    emit("done", status=status, commit=commit)


def artifacts_dir():
    """$RUN_ARTIFACTS as a Path, or None when unset."""
    value = os.environ.get("RUN_ARTIFACTS")
    return Path(value) if value else None


# ---------------------------------------------------------------------------------------------------------------------
# Bus
#
# Everything the console view shows is an event published here.  Markers are not a subscriber: emit() keeps its own
# call sites, so the v1 protocol cannot drift.  Events with no marker form (say, failure, class_done, ...) never reach
# emit().
# ---------------------------------------------------------------------------------------------------------------------


def subscribe(fn):
    """Add fn(kind, fields) to the bus and return it."""
    _subscribers.append(fn)
    return fn


def unsubscribe(fn):
    if fn in _subscribers:
        _subscribers.remove(fn)


def publish(kind, /, **fields):
    """Call every subscriber with (kind, fields).  Subscribers must not raise; an exception propagates to the
    publisher."""
    for fn in list(_subscribers):
        fn(kind, fields)


def _console_owns_screen():
    """True when a session's Console draws the terminal, so raw script text must not be printed as well.  Never with
    markers on: stdout is then the v1 marker stream, the Console draws nothing there (Console.screen_mode), and script
    text prints beside the markers as it does today.  Nor once the view has stopped: the terminal is plain again."""
    return (_console is not None and _console.sinks.console != "full" and not enabled()
            and not _console.stopped)


def console_owns_screen():
    """True when the console view owns the terminal, so plain prints would be hidden."""
    return _console_owns_screen()


def say(text="", level="info", step=None):
    """Script status text.  Printed as-is unless a console view owns the screen, which shows it as a filtered note.
    Inside a paused() block the view has cleared the screen, so the text is printed as-is and the view does not
    repeat it."""
    printed = _pause_depth > 0 and _console_owns_screen()
    if printed or not _console_owns_screen():
        _out(text + "\n")
    publish("say", text=text, level=level, step=step, **({"printed": True} if printed else {}))


def failure(text, step=None):
    """Failure detail that every detail level shows: printed as-is, or by the console view when it owns the screen."""
    if not _console_owns_screen():
        _out(text + "\n")
    publish("failure", text=text, step=step)


def hint(lines, step=None):
    """Lines that tell the user what to do about a failure, at every detail level: printed as-is, or by the console
    view when it owns the screen, beneath the running row (held until it ends) or at once when none is running."""
    lines = [lines] if isinstance(lines, str) else list(lines)
    if not _console_owns_screen():
        _out("".join(line + "\n" for line in lines))
    publish("hint", step=step, lines=lines)


def tick():
    """Let the console view redraw (the spinner, the elapsed time) during script work that blocks.  Throttled by the
    view; a no-op with no session."""
    if _console is not None:
        _console.tick()


@contextmanager
def row(id, title, *, label=None):  # NOSONAR python:S5806 - parameter names mirror protocol field names
    """A console row around a block, with no marker.  For script work that is not a protocol step (Perf, Timing)."""
    publish("step_start", id=id, n=None, title=title, parent=None, tool=None, sig="",
            **({"label": label} if label else {}))
    s = Step(id)
    t0 = time.monotonic()
    try:
        yield s
    except BaseException:
        s.status = "fail"
        raise
    finally:
        publish("step_end", id=id, status=s.status, ms=int((time.monotonic() - t0) * 1000), exit=s.exit,
                summary=s.summary, totals=None)


# ---------------------------------------------------------------------------------------------------------------------
# Detail levels
#
# They filter only the console and condensed-log views, never markers or the full log.
# ---------------------------------------------------------------------------------------------------------------------

DETAIL_LEVELS = ("summary", "actionable", "modules", "all")
DETAIL_ENV = "JUNEAU_RUN_DETAIL"
_NOTE_LEVELS = {
    "summary": {"error"},
    "actionable": {"warn", "error"},
    "modules": {"warn", "error"},
    "all": {"info", "warn", "error"},
}


def resolve_detail(flag=None):
    """--detail, else $JUNEAU_RUN_DETAIL, else actionable.  An unknown level raises ValueError naming the valid ones."""
    value = flag or os.environ.get(DETAIL_ENV) or "actionable"
    if value not in DETAIL_LEVELS:
        raise ValueError(f"detail must be one of {', '.join(DETAIL_LEVELS)}, not {value!r}")
    return value


def export_detail(flag=None):
    """Resolve the level and export it as $JUNEAU_RUN_DETAIL, so child scripts use the same one."""
    value = resolve_detail(flag)
    os.environ[DETAIL_ENV] = value
    return value


def shows_note(detail, level):
    return level in _NOTE_LEVELS[detail]


def shows_module(detail, failed, has_tests):
    """Whether a finished module keeps its own row under its step row."""
    if detail == "summary":
        return False
    if failed or detail == "all":
        return True
    return detail == "modules" and has_tests


# ---------------------------------------------------------------------------------------------------------------------
# Console view
#
# Board holds the run's state and applies the detail filter; apply() turns one bus event into output blocks.  A
# renderer turns blocks into text.  Console subscribes to the bus and feeds one Board to a screen renderer and to
# the condensed-log renderer.
# ---------------------------------------------------------------------------------------------------------------------

SESSION_ENV = "JUNEAU_RUN_SESSION"
LABEL_WIDTH_ENV = "JUNEAU_RUN_LABEL_WIDTH"
LIVE_ENV = "JUNEAU_RUN_LIVE"
HEARTBEAT_SECONDS = 15.0
# "run" is a unit that has started but not ended (distinct from PENDING, which has not started)
# "warn" is a checks unit that would refuse or only warns; a checks bar never shows "run", so the two yellows can't meet
PLAIN_GLYPHS = {"pass": "#", "fail": "F", "skip": ".", "run": "~", "warn": "!"}
NO_COLOR_GLYPHS = {"pass": "█", "fail": "F", "skip": "·", "run": "▒", "warn": "!"}
COLOR_GLYPHS = {"pass": "\x1b[32m█\x1b[0m", "fail": "\x1b[31m█\x1b[0m", "skip": "\x1b[2m█\x1b[0m",
                "run": "\x1b[33m█\x1b[0m", "warn": "\x1b[33m█\x1b[0m"}
PENDING = "░"
DOT_GLYPHS = {"pass": "•", "fail": "F", "skip": "·"}
COLOR_DOT_GLYPHS = {"pass": "\x1b[32m•\x1b[0m", "fail": "\x1b[31m✗\x1b[0m", "skip": "\x1b[2m•\x1b[0m"}
_MARK_COLORS = {"✓": "\x1b[32m✓\x1b[0m", "✗": "\x1b[31m✗\x1b[0m"}
_MARK_RE = re.compile("(?<!m)[✓✗]")   # not one already coloured (right after an SGR escape)
_NOTE_PREFIX = {"info": "ℹ", "warn": "⚠", "error": "✗"}
PENDING_ENV = "JUNEAU_RUN_PENDING"     # a parent session's labelled pending rows, [[id, label], ...], for a nested one
# A row's icon (spec 5.2): a spinner while it runs, then its outcome: a coloured ● for ok, warn and fail, or ✔ ⚠ ✗
# when colour is off.  No VS16 after ⚠: it must stay one column.
SPINNER = "⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏"
ICONS = {"ok": "✔", "warn": "⚠", "skip": "⊘", "fail": "✗", "pending": "○", "paused": "⏸"}
COLOR_ICONS = {**ICONS, "ok": "●", "warn": "●", "fail": "●"}   # with colour on, the hue carries the outcome
_ICON_COLORS = {"running": "\x1b[36m", "ok": "\x1b[32m", "warn": "\x1b[33m", "skip": "\x1b[2m", "fail": "\x1b[31m",
                "pending": "\x1b[2m", "paused": "\x1b[36m"}


def tint(text):
    """text with its ✓ marks green and its ✗ marks red."""
    return _MARK_RE.sub(lambda m: _MARK_COLORS[m.group()], text)


def fmt_elapsed(ms):
    """M:SS, or H:MM:SS from an hour."""
    h, rem = divmod(int(ms // 1000), 3600)
    m, s = divmod(rem, 60)
    return f"{h}:{m:02d}:{s:02d}" if h else f"{m}:{s:02d}"


Estimate = collections.namedtuple("Estimate", "wall_s units rate runs tests", defaults=(None,))


def _median(xs):
    xs = sorted(xs)
    mid = len(xs) // 2
    return xs[mid] if len(xs) % 2 else (xs[mid - 1] + xs[mid]) / 2


def estimate_from(records, sig):
    """Estimate from `records` (oldest first, already filtered to one (step, tool) key): the newest 10 with ok and
    complete, preferring like-for-like `sig` when 3 or more match (spec 5.1)."""
    good = [r for r in records if r.get("ok") and r.get("complete") and r.get("wall_s") is not None][-10:]
    like = [r for r in good if sig and r.get("sig") == sig]
    use = like if len(like) >= 3 else good
    if not use:
        return None
    withunits = [r for r in use if (r.get("units") or {}).get("total")]
    withtests = [r for r in use if isinstance(r.get("tests"), (int, float)) and r["tests"] > 0]
    return Estimate(_median([r["wall_s"] for r in use]),
                    _median([r["units"]["total"] for r in withunits]) if withunits else None,
                    _median([r["wall_s"] / r["units"]["total"] for r in withunits]) if withunits else None,
                    len(use),
                    _median([r["tests"] for r in withtests]) if withtests else None)


def eta_figures(e, k, n, est):
    """(percent, estimated total seconds, approx) for a running row (spec 5.2); None where nothing can be said.
    The total is None once the row has overrun it, so only elapsed shows."""
    hist_units = est.units if est else None
    if n:
        units, approx = n, False
    elif k and hist_units and k < hist_units:
        units, approx = hist_units, True
    else:
        units, approx = None, True
    if units:
        pct = min(99, int(100 * k / units))
        if k == 0:
            total = est.wall_s if est else None
            approx = True
        elif k >= units:
            return pct, None, approx
        else:
            live = e / k
            w = k / units
            rate = w * live + (1 - w) * est.rate if est and est.rate else live
            total = e + (units - k) * rate
        return pct, (total if total is not None and total > e else None), approx
    if est and est.wall_s:
        return min(99, int(100 * e / est.wall_s)), (est.wall_s if est.wall_s > e else None), True
    return None, None, False


def _step_for(seconds):
    """The coarseness the displayed estimates round to: 5s under a minute, 15s under ten, 30s above."""
    return 5 if seconds < 60 else 15 if seconds < 600 else 30


def round_estimate(seconds, floor=0):
    """`seconds` rounded to its _step_for step, and above `floor` (the elapsed time) so a shown total never trails it."""
    step = _step_for(seconds)
    rounded = int(seconds / step + 0.5) * step
    return rounded if rounded > floor else (int(floor // step) + 1) * step


def damp_total(shown, new, e, rounded=True):
    """The estimated total to display (seconds): the one already `shown` is kept until `new` differs from it by more
    than max(15s, 10%) or the elapsed time `e` reaches it; a replacement is rounded (see round_estimate) unless
    `rounded` is off.  None when `new` is None."""
    if new is None:
        return None
    if shown is not None and e < shown and abs(new - shown) <= max(15, 0.1 * shown):
        return shown
    return round_estimate(new, e) if rounded else new


def timer_text(e, total):
    """Elapsed seconds `e` as M:SS, followed by ` / ~M:SS` when an estimated total is known."""
    return fmt_elapsed(e * 1000) + (" / ~" + fmt_elapsed(total * 1000) if total else "")


Overall = collections.namedtuple("Overall", "percent remaining lower_bound done total")


def overall_figures(elapsed, steps):
    """The whole-run figure (spec 6.2).  `elapsed` is the seconds since the session started; `steps` is a list of
    `(id, state, hist_wall_s, running)`, state one of done / running / pending (anything else is ignored) and `running`
    the `(T, e)` of 5.2 for a running step or None.  None when no step still to run has a known figure."""
    remaining, known, lower_bound, done, total = 0.0, False, False, 0, 0
    for _, state, hist, running in steps:
        if state == "done":
            done += 1
        elif state == "running":
            if running is not None:
                remaining += max(running[0] - running[1], 0)
                known = True
            else:
                lower_bound = True
        elif state == "pending":
            if hist is not None:
                remaining += hist
                known = True
            else:
                lower_bound = True
        else:
            continue
        total += 1
    if not known:
        return None
    span = elapsed + remaining
    percent = min(99, int(100 * elapsed / span)) if span > 0 else 0
    return Overall(percent, remaining, lower_bound, done, total)


def overall_text(o):
    """The live region's last line: `Overall  42%  ~2:10 left  · 3 of 7 steps`."""
    left = f"  ~{fmt_elapsed(o.remaining * 1000)}{'+' if o.lower_bound else ''} left" if o.remaining > 0 or o.lower_bound else ""
    return f"Overall  {o.percent}%{left}  · {o.done} of {o.total} steps"


UNIT_PASS, UNIT_FAIL, UNIT_SKIP = 1, 2, 3     # bytes in a row's unit_cells; 0 is "not finished"
UNIT_RUN = 4        # only in a view built by _Row.unit_view(), never stored in unit_cells: started, not ended
UNIT_WARN = 5       # a checks unit that warned


def unit_bar(cells, total, glyphs):
    """The cells of a unit bar: exactly one per unit, each its unit's outcome (fail, in-flight, pass or skip) and
    PENDING while the unit has not started.  Never bucketed: a bar wider than the terminal wraps (see _fit_row).
    Computed on demand from `cells` (a bytearray, 0 unfinished / 1 pass / 2 fail / 3 skip / 4 in-flight), never
    stored."""
    out = []
    for i in range(max(0, total)):
        v = cells[i] if i < len(cells) else 0
        out.append(glyphs["fail"] if v == UNIT_FAIL else glyphs["run"] if v == UNIT_RUN
                   else glyphs["warn"] if v == UNIT_WARN
                   else glyphs["pass"] if v == UNIT_PASS else glyphs["skip"] if v == UNIT_SKIP else PENDING)
    return out


def _totals(t):
    """A tests event's fields or a run_tool summary dict, as a (total, fail, err, skip) tuple."""
    if t is None or isinstance(t, tuple):
        return t
    if isinstance(t, list):
        return tuple(t)
    return (t["total"], t["fail"], t["err"], t["skip"])


def fmt_totals(totals, tests_mode=False, expected=None):
    """The totals text; `expected` (history's median test count) shows as `41,200 / ~83,000 tests` until the count passes it."""
    if not totals or not totals[0]:
        return "no tests run" if tests_mode else ""
    total, fail, err, _ = totals
    of = f" / ~{round(expected):,}" if expected and total < expected else ""
    return f"{total:,}{of} tests" + (f", {fail + err} failed" if fail + err else "")


def format_final(status, ms, failed_title, full_log):
    if status == "ok":
        return f"✅ Done in {fmt_elapsed(ms)}"
    if status == "cancelled":
        return f"⚠ Cancelled after {fmt_elapsed(ms)}"
    return "❌ Failed" + (f" at {failed_title}" if failed_title else "") + (f" · full log: {full_log}" if full_log else "")


class _Mod:
    """One reactor module under a step row."""

    def __init__(self, id, title, t0):  # NOSONAR python:S5806 - parameter names mirror protocol field names
        self.id = id
        self.title = title
        self.t0 = t0
        self.outcomes = []      # class_done outcomes, in order
        self.status = None      # None while running
        self.ms = None
        self.totals = None
        self.box = None         # pass, fail or skip once granted
        self.unit_index = None  # its cell in the row's module bar once counted
        self.started = False    # a module_start arrived for it (the in-flight signal for the module bar)
        self.dropped = False    # a tests-mode module that ran no tests: left out of the bar and of its total

    @property
    def in_progress(self):
        """Still running and not yet boxed.  Pom modules are boxed by the reactor event, so they never show here."""
        return self.status is None and self.box is None


class _Setup:
    """The run's one Setup row: a box per pom module built before the first real module of the first Maven reactor
    that has any (spec 2.1).  A trailing pom (an aggregator root built last) is not on it."""

    def __init__(self, row, ids, t0):
        self.row = row              # id of the step row that owns it
        self.ids = list(dict.fromkeys(ids))     # pom module ids, in reactor order, once each
        self.boxes = {i: None for i in self.ids}    # None while pending, else pass or fail
        self.t0 = t0
        self.ended = False          # every box set, or its owner row ended: the row is permanent (spec 5.4)

    @property
    def total(self):
        return len(self.ids)

    @property
    def done(self):
        return sum(1 for b in self.boxes.values() if b is not None)

    @property
    def failed(self):
        return any(b == "fail" for b in self.boxes.values())

    def add(self, ids):
        if self.ended:
            return
        for i in dict.fromkeys(ids):
            if i not in self.boxes:
                self.ids.append(i)
                self.boxes[i] = None


def setup_text(setup):
    """The Setup row's totals text."""
    return f"{setup.total} pom module{'' if setup.total == 1 else 's'}"


def setup_cells(setup, glyphs):
    return [PENDING if setup.boxes[i] is None else glyphs[setup.boxes[i]] for i in setup.ids]


class _Row:
    """One step row."""

    def __init__(self, id, n, title, label, t0):  # NOSONAR python:S5806 - parameter names mirror protocol field names
        self.id = id
        self.n = n
        self.title = title
        self.label = label or title
        self.t0 = t0
        self.status = None      # None while running
        self.ms = None
        self.summary = None
        self.totals = None
        self.modules = OrderedDict()
        self.order = []         # module ids, in the order their boxes were granted
        self.notes = []         # (level, text), held until the row ends: info ones for the screen at `all`, warn
                                # and error ones (full text) for the condensed log
        self.paused = False     # a pause note was attributed to it: ⏸ when ok
        self.warned = False     # a warn (or an error on a row that still ends ok) was attributed to it: ⚠ when ok
        self.detail = None      # a detail note's first line: drawn after the result, never in place of it
        self.reason = None      # the newest warn or error text's first line, cut only when drawn
        self.reason_level = None    # warn or error; an error reason is never replaced by a warn
        self.hints = []         # lines printed beneath the row when it ends failed (spec 5.1)
        self.held_failure = False   # failure text is among the hints: they print however the row ends
        self.failed_mods = []   # titles of its failed modules, in failure order, for the row's text
        self.size = None        # reactor size N
        self.poms = set()       # the Setup poms: built before the reactor's first real module
        self.tail = set()       # poms built after a real module: on neither the Setup row nor the bar
        self.tests_mode = False
        self.quiet = 0          # seconds the tool has been silent, from the watchdog
        self.units_total = 0    # announced unit count (tests, files...) for the unit bar
        self.units_done = 0
        self.unit_kind = None
        self.unit_cells = bytearray()   # one byte per unit: 0 pending, 1 pass, 2 fail, 3 skip
        self.units_dropped = 0  # tests-mode modules that ran no tests, taken out of units_total (see _drop_unit)
        self.progress = None    # live progress text from the tool's own output; shown on the running row only
        self.estimate = None    # the Estimate from history, when there is one (spec 5.1)
        self.shown_total = None # the estimated total last displayed, held steady by damp_total

    @property
    def running(self):
        return self.status is None

    @property
    def unbarred(self):
        """Modules that never get a cell in the module bar: the Setup poms and the trailing ones."""
        return self.poms | self.tail

    def boxes(self):
        return [self.modules[m].box for m in self.order]

    def unit_view(self):
        """unit_cells for drawing: the modules that have started but not ended fill the cells right after the finished
        ones as UNIT_RUN.  A module bar fills in finish order, so an in-flight module has no cell of its own yet; this
        shows how many are running, not which.  Only a Maven module bar has the signal (module_start); other unit
        kinds (tests, tasks, files) never show in-flight and keep PENDING for what has not finished."""
        if self.unit_kind != "modules" or not self.running:
            return self.unit_cells
        running = sum(1 for m in self.modules.values()
                      if m.started and m.status is None and m.unit_index is None and not m.dropped and m.id not in self.unbarred)
        view = bytearray(self.unit_cells)
        for i in range(self.units_done, min(self.units_total, self.units_done + running)):
            view[i] = UNIT_RUN
        return view


def _parent_pending():
    """JUNEAU_RUN_PENDING as [(id, label)], or [] when it is unset or malformed."""
    try:
        value = json.loads(os.environ.get(PENDING_ENV) or "[]")
        return [(str(i), str(label)) for i, label in value]
    except (ValueError, TypeError):
        return []


class Board:
    """The run's state for the console view.  apply(kind, fields) returns the blocks that event produces."""

    def __init__(self, detail="actionable", nested=False, full_log=None, clock=time.monotonic, estimator=None):
        self.detail = detail
        self.estimator = estimator  # fn(step_id, tool, sig) -> Estimate or None, asked once when a row starts
        self.nested = nested
        self.full_log = full_log
        self.clock = clock
        self.t0 = clock()
        self.rows = OrderedDict()
        self.module_rows = {}       # module id -> its _Row
        self.failure_lines = set()  # stripped lines already shown by a failure event, so notes don't repeat them
        self.failed_title = None
        self.aliases = {}           # a run_tool sub-row's step id -> its parent row's id (parent=, release.py)
        self.setup = None           # the run's one Setup row (pom modules), owned by the first Maven row that has poms
        self.plan = None            # the planned top-level step ids, in order (spec 6), or None
        self.plan_labels = {}       # planned step id -> label, for the ones that draw a pending row
        self.parent_pending = _parent_pending() if nested else []     # a parent session's pending rows, (id, label)
        self.finished = False       # the done event arrived: nothing is pending any more
        self.shown_end = None       # the Overall figure's projected end (session seconds) last displayed, held by damp_total
        self.plan_estimates = {}    # planned step id -> history wall_s or None, asked of the estimator once per plan
        self.session_t0 = self.t0   # when the session event arrived (the Overall figure's E)

    def width(self):
        env = os.environ.get(LABEL_WIDTH_ENV, "")
        return max([int(env) if env.isdigit() else 10] + [len(r.label) for r in self.rows.values()]
                   + [len(label) for label in self.plan_labels.values()]
                   + [len(label) for _, label in self.parent_pending])

    def pending(self):
        """The pending rows, as (id, label): the labelled planned steps that have not started, in plan order, then
        a parent session's (JUNEAU_RUN_PENDING) that this one has no row or pending row for.  A planned step that a
        later started one has passed over is not pending (as in overall())."""
        if self.finished:
            return []
        out = []
        if self.plan:
            last = max((i for i, sid in enumerate(self.plan) if sid in self.rows), default=-1)
            out = [(sid, self.plan_labels[sid]) for i, sid in enumerate(self.plan)
                   if i > last and sid in self.plan_labels and sid not in self.rows]
        have = set(self.rows) | {i for i, _ in out}
        return out + [(i, label) for i, label in self.parent_pending if i not in have]

    def setup_failed(self):
        """The titles of the Setup row's failed pom modules, in reactor order."""
        owner = self.rows.get(self.setup.row) if self.setup is not None else None
        return [owner.modules[i].title if owner is not None and i in owner.modules else i
                for i in self.setup.ids if self.setup.boxes[i] == "fail"] if self.setup is not None else []

    def _end_setup(self):
        """The Setup row goes permanent, once: when every pom box is set or its owner row ends."""
        if self.setup is None or self.setup.ended:
            return []
        self.setup.ended = True
        return [("setup",)]

    def _setup_settled(self):
        if self.setup is not None and self.setup.total and self.setup.done == self.setup.total:
            return self._end_setup()
        return []

    def _attribute(self, step_id):
        """The row a note, say, hint or failure belongs to: the step's (or its module's), else the newest running
        row."""
        if step_id:
            r = self._target(step_id) or self.module_rows.get(step_id)
            if r is not None:
                return r
        return self._latest_running()

    def _hold(self, r, lines):
        """Hint lines for r, less those a failure event already showed."""
        r.hints += [line for line in lines if line.strip() not in self.failure_lines]

    def _reason(self, r, level, text):
        """A warn or error attributed to a running row: its first line is the row's reason (an error is never
        replaced by a warn), an error's other lines are hints, and the row is warned."""
        lines = text.splitlines()
        first = next((i for i, line in enumerate(lines) if line.strip()), None)
        r.warned = True
        if first is None:
            return
        if level == "error" or r.reason_level != "error":
            if level == "error" and r.reason_level == "error" and r.reason:
                self._hold(r, [r.reason])     # an older error is not lost: it joins the hints
            r.reason, r.reason_level = lines[first].strip(), level
        if level == "error":
            self._hold(r, lines[first + 1:])

    def running(self):
        return [r for r in self.rows.values() if r.running]

    def apply(self, kind, fields):
        handler = getattr(self, "_on_" + kind, None)
        return handler(fields) if handler is not None else []

    def _latest_running(self):
        running = self.running()
        return running[-1] if running else None

    def _target(self, step_id):
        """The row a step id draws on: its own, or, for a sub-row started with parent=, its parent's."""
        r = self.rows.get(step_id)
        return r if r is not None else self.rows.get(self.aliases.get(step_id))

    def _adopt(self, step_id):
        """The row for a reactor or module_start event.  When a sub-row's tool sends the first one, that tool's
        modules take over the parent row's bar: the boxes the row's sub-rows had (one per command) are dropped."""
        r = self._target(step_id)
        if r is not None and step_id in self.aliases and step_id in r.modules:
            for sub in [m for m in r.modules if m in self.aliases]:
                del r.modules[sub]
                self.module_rows.pop(sub, None)
            r.order = [m for m in r.order if m in r.modules]
        return r

    def _mod(self, f, create=False):
        r = self._target(f["step"])
        if r is None:
            return None, None
        m = r.modules.get(f["module"])
        if m is None and create:
            m = r.modules[f["module"]] = _Mod(f["module"], f["module"], self.clock())
            self.module_rows[f["module"]] = r
        return r, m

    @staticmethod
    def _grant(r, m, box):
        if m.box is None:
            r.order.append(m.id)
        m.box = box

    def _on_session(self, f):
        self.session_t0 = self.clock()
        if self.nested:
            return []
        return [("header", " · ".join([f["title"]] + list(f["header"])))]   # the title carries its own emoji

    def _on_plan(self, f):
        self.plan = list(f["ids"])
        self.plan_labels = dict(f.get("labels") or {})
        self.plan_estimates = {}
        return []

    def _hist_wall(self, step_id):
        """History's median wall seconds for a planned step (tool left open), or None."""
        if step_id not in self.plan_estimates:
            est = None
            if self.estimator is not None:
                try:
                    est = self.estimator(step_id, None, None)
                except Exception:
                    est = None
            self.plan_estimates[step_id] = est.wall_s if est else None
        return self.plan_estimates[step_id]

    def overall(self):
        """The whole-run Overall figure, or None: no plan, a nested session, or nothing known (spec 6.2, 6.3)."""
        if not self.plan or self.nested:
            return None
        # Parallel planned steps each add their remaining time, so a plan that runs steps concurrently is an overestimate.
        now = self.clock()
        last_started = max((i for i, sid in enumerate(self.plan) if sid in self.rows), default=-1)
        steps = []
        for i, sid in enumerate(self.plan):
            r = self.rows.get(sid)
            if r is None:
                if i < last_started:
                    continue                    # passed over by a later planned step
                steps.append((sid, "pending", self._hist_wall(sid), None))
            elif r.running:
                e = now - r.t0
                _, total, _ = eta_figures(e, r.units_done, r.units_total, r.estimate)
                total = r.shown_total = damp_total(r.shown_total, total, e)
                if total is None and r.estimate is not None:
                    total = max(r.estimate.wall_s, e)      # overran its history: nothing left to add
                steps.append((sid, "running", r.estimate.wall_s if r.estimate else None,
                              (total, e) if total is not None else None))
            elif r.status != "skip":
                steps.append((sid, "done", None, None))
        o = overall_figures(now - self.session_t0, steps)
        if o is None or o.remaining <= 0:
            return o
        elapsed = now - self.session_t0       # damp the projected end like a row's total, then show what is left of it
        self.shown_end = damp_total(self.shown_end, elapsed + o.remaining, elapsed, rounded=False)
        left = max(self.shown_end - elapsed, 0)
        return o._replace(remaining=round_estimate(left) if left > 0 else left)

    def _on_step_start(self, f):
        parent = self.rows.get(f.get("parent"))
        if parent is not None:      # a sub-row: a box on its parent's row until its tool sends modules (_adopt)
            self.aliases[f["id"]] = parent.id
            return self._on_module_start({"step": parent.id, "module": f["id"], "title": f["title"]})
        row = self.rows[f["id"]] = _Row(f["id"], f["n"], f["title"], f.get("label"), self.clock())
        if self.estimator is not None:
            try:
                row.estimate = self.estimator(f["id"], f.get("tool"), f.get("sig") or None)
            except Exception:
                row.estimate = None
        return []

    def _on_module_start(self, f):
        r = self._adopt(f["step"])
        if r is not None:
            m = r.modules.get(f["module"])
            if m is None:
                r.modules[f["module"]] = _Mod(f["module"], f["title"], self.clock())
            else:
                m.title, m.t0 = f["title"], self.clock()     # a pom module, created by the reactor event
            r.modules[f["module"]].started = True
            self.module_rows[f["module"]] = r
            if f["module"] in r.poms:     # a pom has no work to wait for; under -T its end only comes with the summary
                self._setup_box(r, r.modules[f["module"]], "pass")
                return self._setup_settled()
        return []

    def _on_reactor(self, f):
        """Sent at the first line (pom-tree size), again at the Reactor Build Order block and at the first Building
        line.  The module bar counts the non-pom modules; the pom modules built before the first real module go to the
        run's Setup row (spec 2.1).  A pom built after a real module (`tail`, an aggregator root) is on neither."""
        r = self._adopt(f["step"])
        if r is not None:
            r.size = f["size"]
            r.poms = set(f["poms"])
            r.tail = set(f.get("tail") or ())     # absent from an older publisher: no trailing pom
            r.tests_mode = f["tests"]
            if r.size > 0:        # a reactor of no size says nothing: the row keeps the box bar
                r.unit_kind = "modules"
                r.units_total = max(0, r.size - len(r.poms) - len(r.tail) - r.units_dropped, r.units_done)    # set, not grown: the block shrinks the tree's size
                del r.unit_cells[r.units_total:]
                r.unit_cells.extend(bytes(r.units_total - len(r.unit_cells)))
            for pom in f["poms"]:
                self._mod({"step": r.id, "module": pom}, create=True)
            if r.poms and self.setup is None:
                self.setup = _Setup(r.id, f["poms"], self.clock())
            elif self.setup is not None and self.setup.row == r.id:
                self.setup.add(f["poms"])
        return []

    def _setup_box(self, r, m, box):
        """A pom module finished: its Setup box turns green (or red)."""
        if self.setup is not None and m.id in self.setup.boxes and self.setup.boxes[m.id] != "fail":
            self.setup.boxes[m.id] = box

    def _count_unit(self, r, m, outcome):
        """A real module of a Maven row counts once toward its module bar; a later failure turns its cell red."""
        if r.unit_kind != "modules" or m.id in r.unbarred:
            return
        if m.dropped:
            if outcome != "fail":
                return
            self._undrop_unit(r, m)         # it failed after all (the build broke in it): it counts, red
        if m.unit_index is None:
            m.unit_index = r.units_done
            self._on_unit({"step": r.id, "done": r.units_done + 1, "outcome": outcome})
        elif outcome == "fail":
            self._on_unit_fix({"step": r.id, "index": m.unit_index, "outcome": "fail"})

    @staticmethod
    def _resize_cells(r):
        del r.unit_cells[r.units_total:]
        r.unit_cells.extend(bytes(r.units_total - len(r.unit_cells)))

    def _drop_unit(self, r, m):
        """A tests-mode module ended having run no tests: it leaves the bar and the total instead of drawing a skip cell.
        The count of modules that will run tests is only known as modules end (--also-make pulls in many upstream
        modules with none), so the live bar starts at the reactor's size and N shrinks as those drop out.  The n/N
        and the bar therefore only ever lose cells that were never going to say anything."""
        if r.unit_kind != "modules" or m.id in r.unbarred or m.dropped or m.unit_index is not None:
            return
        m.dropped = True
        r.units_dropped += 1
        r.units_total = max(r.units_done, r.units_total - 1)
        self._resize_cells(r)

    def _undrop_unit(self, r, m):
        m.dropped = False
        r.units_dropped -= 1
        r.units_total += 1
        self._resize_cells(r)

    OUTCOME_BYTE = {"pass": UNIT_PASS, "fail": UNIT_FAIL, "skip": UNIT_SKIP, "warn": UNIT_WARN}     # anything else: pass

    def _on_units(self, f):
        r = self._adopt(f["step"])
        if r is not None:
            # an explicit count replaces the total (a later, authoritative count beats an earlier estimate), but never
            # below the units already done, so no finished cell is lost
            r.units_total, r.unit_kind = max(r.units_done, f["total"] - r.units_dropped), f["kind"]
            del r.unit_cells[r.units_total:]
            r.unit_cells.extend(bytes(r.units_total - len(r.unit_cells)))
        return []

    def _on_unit(self, f):
        r = self._adopt(f["step"])
        if r is not None:
            done = f["done"]
            if done > r.units_total:                      # more units than announced (a resend lagging, -x absent)
                self._on_units({"step": f["step"], "total": done, "kind": r.unit_kind})
            byte = self.OUTCOME_BYTE.get(f["outcome"], UNIT_PASS)
            if done > r.units_done:                       # cumulative: a jump fills every skipped unit
                r.unit_cells[r.units_done:done] = bytes([byte]) * (done - r.units_done)
            r.units_done = max(r.units_done, done)
        return []

    def _on_unit_fix(self, f):
        r = self._adopt(f["step"])
        if r is not None and 0 <= f["index"] < r.units_done:       # only a unit that has finished can be corrected
            r.unit_cells[f["index"]] = self.OUTCOME_BYTE.get(f["outcome"], UNIT_PASS)
        return []

    def _on_class_done(self, f):
        _, m = self._mod(f, create=True)
        if m is not None:
            m.outcomes.append(f["outcome"])
            r = self._target(f["step"])
            if m.dropped and r is not None:
                self._undrop_unit(r, m)     # tests ran after all (module_tested only means "past its test phase")
                self._count_unit(r, m, self._tests_outcome(m))
        return []

    @staticmethod
    def _tests_outcome(m):
        """A tests-mode module's outcome so far: fail if any class failed, skip if it had tests and all were skipped
        (or it had none), else pass."""
        if "fail" in m.outcomes:
            return "fail"
        if m.outcomes:
            return "skip" if all(o == "skip" for o in m.outcomes) else "pass"
        if m.totals and m.totals[0]:
            return "skip" if m.totals[3] == m.totals[0] else "pass"
        return "skip"

    def _on_module_compiled(self, f):
        r, m = self._mod(f, create=True)
        if m is None:
            return []
        if m.id in r.unbarred:
            return []                 # pom modules live on the Setup row (or, trailing, nowhere), never in a bar
        if not f["ok"]:
            self._grant(r, m, "fail")
            if not r.tests_mode:
                self._count_unit(r, m, "fail")
        elif not r.tests_mode and m.box is None:
            self._grant(r, m, "pass")
            self._count_unit(r, m, "pass")
        return []

    def _on_module_tested(self, f):
        r, m = self._mod(f, create=True)
        if m is not None and m.id not in r.unbarred and m.box != "fail":
            outcome = self._tests_outcome(m)
            self._grant(r, m, outcome)
            if r.tests_mode:      # under -T module_end waits for the Reactor Summary: count the unit when its tests end
                if m.outcomes or (m.totals and m.totals[0]):
                    self._count_unit(r, m, outcome)     # all-skipped tests still count, as skip
                else:
                    self._drop_unit(r, m)
        return []

    def _on_tests(self, f):
        totals = _totals(f)
        if self._target(f["step"]) is not None:
            self._target(f["step"]).totals = totals
        elif f["step"] in self.module_rows:
            self.module_rows[f["step"]].modules[f["step"]].totals = totals
        return []

    def _on_module_end(self, f):
        r, m = self._mod(f, create=True)
        if m is None:
            return []
        m.status, m.ms = f["status"], f["ms"]
        if f.get("totals"):
            m.totals = _totals(f["totals"])
        failed = m.status == "fail" or bool(m.totals and m.totals[1] + m.totals[2])
        if m.dropped and (m.outcomes or (m.totals and m.totals[0])):
            self._undrop_unit(r, m)         # its totals arrived late: it did run tests
        blocks = []
        if m.id in r.poms:      # a pom lives on the Setup row only, never in a bar's order
            m.box = "fail" if failed else "pass"
            was_failed = self.setup is not None and self.setup.failed
            self._setup_box(r, m, m.box)
            if self.setup is not None and self.setup.ended and self.setup.failed and not was_failed:
                blocks = [("setup",)]     # it went permanent green at the pom's start: print it again, red
            else:
                blocks = self._setup_settled()
        elif m.id in r.tail:    # a trailing pom: no Setup box, no bar cell; its failure still fails the row and is named
            m.box = "fail" if failed else "pass"
            if failed and m.title not in r.failed_mods:
                r.failed_mods.append(m.title)
        else:
            if failed:
                self._grant(r, m, "fail")
            elif m.box is None:
                self._grant(r, m, "skip" if m.status == "skip" else "pass")
            # a module already boxed red (compile or test failure) stays a failed unit when finish() closes it as skip
            failed = failed or m.box == "fail"
            if failed and m.title not in r.failed_mods:
                r.failed_mods.append(m.title)
            if r.tests_mode and not failed and m.unit_index is None and not (m.outcomes or (m.totals and m.totals[0])):
                self._drop_unit(r, m)       # ended without ever running a test (no module_tested, or a skipped module)
            else:
                tested = r.tests_mode and (m.outcomes or (m.totals and m.totals[0]))
                self._count_unit(r, m, "fail" if failed else "skip" if m.status == "skip" else
                                 self._tests_outcome(m) if tested else "pass")
        has_tests = bool(m.totals and m.totals[0])
        # finished-module lines only at modules and all: below that a failed module is in its row's text (spec 5.1)
        if self.detail in ("modules", "all") and shows_module(self.detail, failed, has_tests):
            blocks.append(("module", r, m))
        return blocks

    def _on_step_progress(self, f):
        r = self._target(f["step"])
        if r is not None:
            r.progress = f["text"]
        return []

    def _on_step_end(self, f):
        if f["id"] not in self.rows and f["id"] in self.module_rows:
            if f["id"] in self.aliases:
                self.module_rows[f["id"]].quiet = 0     # a sub-row's tool exited: its parent row is not silent
                self.module_rows[f["id"]].progress = None
            return self._on_module_end({"step": self.module_rows[f["id"]].id, "module": f["id"],
                                        "status": f["status"], "ms": f["ms"], "totals": f.get("totals")})
        if f["id"] not in self.rows and f["id"] in self.aliases:
            r = self._target(f["id"])   # a sub-row whose tool took over the parent's bar; the parent ends later
            if r is not None:
                r.quiet = 0     # the tool exited: nothing is silent any more
                r.progress = None
                if f.get("totals"):
                    r.totals = _totals(f["totals"])
            return []
        r = self.rows.get(f["id"])
        if r is None:
            return []
        r.status, r.ms = f["status"], f["ms"]
        r.quiet = 0     # the tool exited while silent: no quiet(0) follows
        r.progress = None
        if f.get("summary") is not None:
            r.summary = f["summary"]
        if f.get("totals"):
            r.totals = _totals(f["totals"])
        for m in r.modules.values():   # never ended: it ran nothing we saw (close_children ends the open ones)
            if m.box is None and m.id not in r.unbarred:
                self._grant(r, m, "fail" if "fail" in m.outcomes else "pass" if m.outcomes else "skip")
                if m.box == "fail" and m.title not in r.failed_mods:
                    r.failed_mods.append(m.title)
        if r.status == "fail" and self.failed_title is None:
            self.failed_title = r.label
        blocks = self._end_setup() if self.setup is not None and self.setup.row == r.id else []
        blocks.append(("row", r))
        hints = r.status not in ("ok", "skip") or r.held_failure or (r.unit_kind == "checks" and r.warned)
        shown = {line.strip() for line in r.hints} if hints else set()
        for level, text in r.notes:
            kept = [line for line in text.splitlines() if line.strip() not in self.failure_lines]
            if level == "say":
                blocks += [("text", "\n".join(kept))] if kept else []
            elif level == "info":
                blocks += [("text", f"    {_NOTE_PREFIX[level]} " + "\n      ".join(kept))] if kept else []
            else:   # the full warn or error text, for the condensed log only (D6), less what the hints repeat
                kept = kept[:1] + [line for line in kept[1:] if line.strip() not in shown]
                blocks += [("logonly", f"    {_NOTE_PREFIX[level]} " + "\n      ".join(kept))] if kept else []
        if hints and r.hints:
            blocks.append(("hint", r, list(r.hints)))
        return blocks

    def _on_note(self, f):
        """A warn or error note on a running row sets its reason and emits nothing (at every level: summary and
        actionable look the same on screen); an info one is held for the row's end at `all` only.  With no running
        row the note prints at once, filtered by the detail level."""
        level = f["level"]
        r = self._attribute(f.get("step"))
        if level == "pause":
            if r is not None:
                r.paused = True
            return []
        if level == "detail":
            line = next((x.strip() for x in f["text"].splitlines() if x.strip()), None)
            if r is not None and line:
                r.detail = line
            return []
        if r is not None and r.running:
            if level != "info":
                self._reason(r, level, f["text"])
                r.notes.append((level, f["text"]))
            elif shows_note(self.detail, level):
                r.notes.append((level, f["text"]))
            return []
        if not shows_note(self.detail, level):
            return []
        line = f"{_NOTE_PREFIX[level]} {f['text']}"
        return [("text", "    " + line if r is not None else line)]

    def _on_failure(self, f):
        """Failure text: held in the running row's hints (it prints when the row ends), else printed at once."""
        lines = f["text"].splitlines()
        new = {line.strip() for line in lines}
        self.failure_lines.update(new)
        r = self._attribute(f.get("step"))
        if r is not None and r.running:
            r.hints = [line for line in r.hints if line.strip() not in new] + lines
            r.held_failure = True
            return []
        return [("text", f["text"])]

    def _on_hint(self, f):
        """Hint lines: held on the running row (beneath it when it ends failed), else printed at once."""
        r = self._attribute(f.get("step"))
        if r is not None and r.running:
            self._hold(r, f["lines"])
            return []
        lines = [line for line in f["lines"] if line.strip() not in self.failure_lines]
        return [("hint", None, lines)] if lines else []

    def _on_say(self, f):
        """A say is a note on the running row (warn and error set its reason, info is held at `all`), else printed
        at once, filtered by the detail level."""
        level = f["level"]
        r = self._attribute(f.get("step"))
        if r is not None and r.running:
            if level != "info":
                self._reason(r, level, f["text"])
                r.notes.append((level, f["text"]))
            elif shows_note(self.detail, level):
                r.notes.append(("say", f["text"]))
            return []
        return [("text", f["text"])] if shows_note(self.detail, level) and not f.get("printed") else []

    def _on_ask(self, f):
        return [("logonly", f"{f['prompt']}{f['answer']}")]

    def _on_show(self, f):
        return [("logonly", f["text"])]

    def _on_prompt(self, f):
        return [("raw", f["text"])]

    def _on_prompt_echo(self, f):
        return [("raw", f["text"])]

    def _on_prompt_end(self, f):
        return [("raw", "\n")]

    def _on_quiet(self, f):
        r = self._target(f["step"])
        if r is not None:
            r.quiet = f["seconds"]
        return []

    def _on_done(self, f):
        self.finished = True
        if self.nested:
            return []
        return [("final", f["status"], int((self.clock() - self.t0) * 1000), self.failed_title, self.full_log)]


def row_state(row):
    """running, ok, warn, paused, skip or fail (spec 5.2): fail wins; a skip is always skip; ok with a pause is
    paused, with a warn is warn."""
    if row.running:
        return "running"
    if row.status == "skip":
        return "skip"
    if row.status == "ok":
        return "paused" if row.paused else "warn" if row.warned else "ok"
    return "fail"


def icon(state, now=0.0, color=False):
    """The glyph for a row state (or "pending"): the spinner frame for `now` while running.  color adds its SGR and
    draws ok, warn and fail as a ● of that colour."""
    if state == "running":
        glyph = SPINNER[int(now / LiveRenderer.REDRAW_SECONDS) % len(SPINNER)]
    else:
        glyph = (COLOR_ICONS if color else ICONS)[state]
    return f"{_ICON_COLORS[state]}{glyph}\x1b[0m" if color else glyph


def row_icon(row, now, color):
    """The row's icon: a spinner (cyan) while it runs, then ● (green, yellow or red) or, without
    colour, ✔ ⚠ ✗; ⊘ (dim) when skipped."""
    return icon(row_state(row), now, color)


def _failed_text(row, expected):
    """A row with failed modules: `N failed: <first> [+N-1]`, or in tests mode the totals and `: <first> [+N-1]`."""
    names = row.failed_mods[0] + (f" +{len(row.failed_mods) - 1}" if len(row.failed_mods) > 1 else "")
    totals = fmt_totals(row.totals, False, expected) if row.tests_mode else ""
    return f"{totals}: {names}" if totals else f"{len(row.failed_mods)} failed: {names}"


def row_text(row, glyphs, now, pending=False):
    """A step row as (cells, count, text, elapsed, cut): its bar's cells (None when it has no bar), the count and
    percent after the bar, one text (the failed modules, else the reason, else the summary or totals), the elapsed
    time, and whether text may be cut to fit (a reason or failed-module text).  Elapsed is empty under a second, so
    instant rows (Install) show none."""
    cells, parts = None, []
    # A module row can carry a unit bar too, hence the `row.modules` test below.  A unit bar's PENDING cells also
    # show on a finished row whose units did not all report.
    unreported = not row.running and row.unit_kind == "modules" and not row.units_done
    # A finished module bar that never counted a unit (a -q build sends no module events) shows no bar at all.
    if not unreported and (row.modules or row.size or row.units_total or row.unit_kind == "modules"):
        if row.unit_kind:
            cells = unit_bar(row.unit_view(), row.units_total, glyphs)
            parts.append(f"{row.units_done}/{row.units_total}")
        else:
            cells = [glyphs[b] for b in row.boxes()]
            if pending and row.size:
                cells += [PENDING] * max(0, row.size - len(row.order))
            parts.append(f"{len(row.order)}/{row.size}" if row.size else str(len(row.order)))
    ms = row.ms if row.ms is not None else int((now - row.t0) * 1000)
    elapsed = fmt_elapsed(ms) if ms >= 1000 else ""
    if row.running and row.estimate is not None:
        pct, total, approx = eta_figures(ms / 1000, row.units_done, row.units_total, row.estimate)
        total = row.shown_total = damp_total(row.shown_total, total, ms / 1000)
        if pct is not None and ms >= 1000:
            parts.append(("~" if approx else "") + f"{pct}%")
        if elapsed:
            elapsed = timer_text(ms / 1000, total)
    expected = row.estimate.tests if row.running and row.estimate is not None else None
    cut = True
    if row.failed_mods:
        text = _failed_text(row, expected)
    elif row.reason:
        text = row.reason
    else:
        cut = False
        text = row.summary if row.summary is not None else fmt_totals(row.totals, row.tests_mode and not row.running,
                                                                       expected)
        if row.status == "skip" and text and text.startswith("(") and text.endswith(")"):
            text = text[1:-1]       # "⊘ Install    done by Tests"
        if row.detail:
            text = f"{text}  {row.detail}" if text else row.detail
    return cells, "  ".join(parts), text or "", elapsed, cut


def row_cells(row, glyphs, now, pending=False):
    """A step row as (cells, rest, elapsed): its bar's cells (None when it has no bar), the count and text after the
    bar (row_text), and the elapsed time."""
    cells, count, text, elapsed, _ = row_text(row, glyphs, now, pending)
    return cells, "  ".join(p for p in (count, text) if p), elapsed


def row_parts(row, glyphs, now, pending=False):
    """A step row as (text, elapsed)."""
    cells, rest, elapsed = row_cells(row, glyphs, now, pending)
    return ("  ".join(p for p in ("".join(cells), rest) if p) if cells is not None else rest), elapsed


def row_label(row, width, now, color=False):
    """The row's label column: its icon, a space and the label padded to width."""
    return f"{row_icon(row, now, color)} {row.label.ljust(width)}"


def pending_line(label, color=False):
    """A planned step that has not started: a dim ○ and its label."""
    return f"\x1b[2m○ {label}\x1b[0m" if color else f"○ {label}"


def module_line(mod, glyphs, now):
    ms = mod.ms if mod.ms is not None else int((now - mod.t0) * 1000)
    tests = fmt_totals(mod.totals)
    return f"    {glyphs[mod.box or 'pass']} {mod.title}  {fmt_elapsed(ms)}" + (f"  {tests}" if tests else "")


def hint_lines(block):
    """A hint block's lines: indented under the row's label when it belongs to a row, else as given."""
    _, row, lines = block
    return [("    " + line if row is not None else line) for line in lines]


def setup_line(board, glyphs, cols=None, now=0.0, color=False):
    """The Setup row as a line: its icon (a spinner until every pom box is set or its owner ends, then ● or, without colour, ✔ or ✗), the
    label column, one box per pom module, and the totals text or, when a pom failed, `N failed: <pom>`."""
    setup = board.setup
    state = "running" if not setup.ended else "fail" if setup.failed else "ok"
    label = f"{icon(state, now, color)} {'Setup'.ljust(board.width())}"
    failed = board.setup_failed()
    text = (f"{len(failed)} failed: {failed[0]}" + (f" +{len(failed) - 1}" if len(failed) > 1 else "")
            if failed else setup_text(setup))
    if cols is None:
        return label + "  " + "".join(setup_cells(setup, glyphs)) + "  " + text
    return _fit_row(label, setup_cells(setup, glyphs), text, "", cols)


def _cut_text(text, room, minimum=12):
    """text cut with an ellipsis to fit room columns, but never below minimum (the bar gives way after that)."""
    if _vlen(text) <= room:
        return text
    keep = max(room, min(minimum, _vlen(text)))
    return _vcut(text, max(0, keep - 1)) + "…" if keep >= 1 else ""


class PlainRenderer:
    """Prints each block once, when it happens.  The screen form on a non-TTY console, and the condensed log."""

    def __init__(self, write, glyphs=None, heartbeat=False, log=False, clock=time.monotonic):
        self.write = write
        self.glyphs = glyphs or PLAIN_GLYPHS
        self.heartbeat = heartbeat
        self.log = log
        self.clock = clock
        self.last_beat = clock()
        self.prompting = False      # the last write left a watchdog prompt's partial line (Console._close_screen)

    def _write(self, text):
        self.write(text)
        self.prompting = not text.endswith("\n")

    def render(self, board, blocks):
        for block in blocks:
            kind = block[0]
            if kind in ("header", "text"):
                self._write(block[1] + "\n")
            elif kind == "row":
                text, elapsed = row_parts(block[1], self.glyphs, self.clock())
                label = row_label(block[1], board.width(), self.clock())
                self._write("  ".join(p for p in (label, text, elapsed) if p) + "\n")
            elif kind == "setup":
                self._write(setup_line(board, self.glyphs) + "\n")
            elif kind == "hint":
                self._write("".join(line + "\n" for line in hint_lines(block)))
            elif kind == "module":
                self._write(module_line(block[2], self.glyphs, self.clock()) + "\n")
            elif kind == "raw":
                self._write(block[1])
            elif kind == "logonly" and self.log:
                self._write(block[1] + "\n")
            elif kind == "final":
                self._write(format_final(*block[1:]) + "\n")

    def tick(self, board):
        now = self.clock()
        if not self.heartbeat or now - self.last_beat < HEARTBEAT_SECONDS:
            return
        self.last_beat = now
        running = board.running()
        if not running:
            return
        totals = [r.totals for r in running if r.totals]
        total = sum(t[0] for t in totals)
        failed = sum(t[1] + t[2] for t in totals)
        k = sum(1 for r in running for m in r.modules.values() if m.in_progress and m.id not in r.unbarred)
        overall = board.overall()
        figure = (f" · {overall.percent}% · ~{fmt_elapsed(overall.remaining * 1000)}"
                  f"{'+' if overall.lower_bound else ''} left") if overall else ""
        self._write(f"… {total:,} tests, {failed} failed · {k} modules running  "
                    f"{fmt_elapsed((now - board.t0) * 1000)}{figure}\n")

    def pause(self):
        """Nothing is drawn in place, so there is nothing to clear."""

    def resume(self):
        """Nothing is drawn in place, so there is nothing to redraw."""

    def close(self):
        """Nothing is drawn in place, so there is nothing to clear."""


_ANSI = re.compile(r"\x1b\[[0-9;?]*[A-Za-z]")


def _cw(ch):
    """Columns a character takes: East Asian wide and fullwidth characters take two."""
    return 2 if unicodedata.east_asian_width(ch) in ("W", "F") else 1


def _vlen(text):
    """Visible width in columns: escapes take none, wide characters two."""
    return sum(_cw(ch) for ch in _ANSI.sub("", text))


def _vcut(text, n):
    """The first n visible columns, keeping escapes, with colour reset if there was any.  A wide character that would
    straddle column n is left out."""
    out, seen, i = [], 0, 0
    while i < len(text) and seen < n:
        m = _ANSI.match(text, i)
        if m:
            out.append(m.group())
            i = m.end()
            continue
        w = _cw(text[i])
        if seen + w > n:
            break
        out.append(text[i])
        seen += w
        i += 1
    return "".join(out) + ("\x1b[0m" if "\x1b[" in text else "")


def common_prefix(titles):
    """Longest run of whole leading words shared by every title and proper for all of them (spec 3, step 2).  A word
    ends at whitespace, or after a ':' or '/'.  Fewer than two distinct titles: no prefix."""
    titles = sorted(set(titles))
    if len(titles) < 2:
        return ""
    split = [re.findall(r"[^\s:/]+[:/]?\s*|[:/]\s*", t) for t in titles]
    shared = []
    for group in zip(*split):
        if all(g.strip() == group[0].strip() for g in group):     # a final word has no trailing space: compare stripped
            shared.append(group[0])
        else:
            break
    prefix = "".join(shared)
    while prefix and any(t.strip() == prefix.strip() for t in titles):
        shared.pop()
        prefix = "".join(shared)
    return prefix


def collapse_names(names, cols):
    """Comma-join `names` into at most `cols - 2` display columns: `cols` is the line width the caller already
    reduced to leave the last terminal column free (spec 3), less the caller's two-column indent.  When some names do
    not fit end with `, … and N others`, counted in the fit; a first name too wide is cut with an ellipsis (spec 3,
    step 3).  The result never exceeds the budget, however narrow."""
    budget = cols - 2
    if not names or budget < 1:
        return ""
    joined = ", ".join(names)
    if _vlen(joined) <= budget:
        return joined
    for keep in range(len(names) - 1, 0, -1):
        line = ", ".join(names[:keep]) + f", … and {len(names) - keep} others"
        if _vlen(line) <= budget:
            return line
    suffix = f", … and {len(names) - 1} others" if len(names) > 1 else ""
    room = budget - _vlen(suffix) - 1
    if room < 1:        # the suffix alone leaves no room for a name: keep the name, drop the suffix
        suffix, room = "", budget - 1
    line = (_vcut(names[0], room) + "…" if room >= 1 else "…") + suffix       # _vcut adds no ellipsis
    return line if _vlen(line) <= budget else _vcut(line, budget)


def _fit(left, right, cols):
    """left, then right flush with column cols; left is cut with an ellipsis when both don't fit."""
    room = cols - (len(right) + 2 if right else 0)
    if _vlen(left) > room:
        left = _vcut(left, max(0, room - 1)) + "…"
    return left + " " * max(2, cols - _vlen(left) - len(right)) + right if right else left


def _fit_row(label, cells, rest, elapsed, cols, wrap=False):
    """A step row fitted to cols.  When it is too wide the bar gives way first, keeping its newest cells behind an
    ellipsis, so the label, count, totals and elapsed survive; only a row too wide even then is cut from the right.
    With wrap (a unit bar, one cell per unit) the bar never gives way: the cells that don't fit beside the label and
    count continue on extra lines indented to the bar's column, and the result holds several lines joined by "\n"."""
    if " / ~" in elapsed:       # the "M:SS / ~M:SS" timer gives way to plain elapsed when even the bare row won't fit
        bare = _vlen(label + ("  " + rest if rest else "")) + len(elapsed) + 2
        if bare > cols:
            elapsed = elapsed.split(" / ~")[0]
    line = label + ("  " + "".join(cells) + "  " + rest if cells is not None else "  " + rest if rest else "")
    if cells is None or _vlen(line) <= cols - (len(elapsed) + 2 if elapsed else 0):
        return _fit(line, elapsed, cols)
    room = cols - (len(elapsed) + 2 if elapsed else 0) - _vlen(label + "    " + rest)
    if wrap and room >= 1:
        indent = _vlen(label) + 2
        per = room          # every line holds as many cells as the first
        first, more = cells[:room], cells[room:]
        lines = [_fit(label + "  " + "".join(first) + "  " + rest, elapsed, cols)]
        lines += [" " * indent + "".join(more[i:i + per]) for i in range(0, len(more), per)]
        return "\n".join(lines)
    if room < 2:
        return _fit(line, elapsed, cols)
    return _fit(label + "  …" + "".join(cells[len(cells) - (room - 1):]) + "  " + rest, elapsed, cols)


# Known limitations: narrowing a reflowing terminal can leave stale fragments above the region, because the cursor-up
# count is the number of lines drawn at the old width and the terminal has rewrapped them into more.
class LiveRenderer:
    """Finished blocks print permanently; running rows and their modules redraw in place below them, at most once per
    REDRAW_SECONDS, and never taller than the terminal's rows minus 2."""

    REDRAW_SECONDS = 0.1

    def __init__(self, write, size=shutil.get_terminal_size, clock=time.monotonic, color=True):
        self.write = write
        self.size = size
        self.clock = clock
        self.color = color
        self.glyphs = COLOR_GLYPHS if color else NO_COLOR_GLYPHS
        self.dots = COLOR_DOT_GLYPHS if color else DOT_GLYPHS     # one per test class on a module's sub-lines
        self.height = 0             # lines of the region now on screen
        self.last_draw = None
        self.paused = False
        self.prompting = False      # a watchdog prompt's partial line is on screen
        self.cursor_hidden = False
        self.closed = False         # after close() permanent blocks still print, but nothing redraws
        self.board = None
        self.flushed = 0            # blocks of the last render() call already printed (Console._fallback)

    def render(self, board, blocks):
        self.board = board
        self.flushed = 0
        lines = []
        for i, block in enumerate(blocks):
            kind = block[0]
            if kind == "raw":
                self._flush(lines)
                lines = []
                self.flushed = i
                self._clear()
                self.write(block[1])
                self.flushed = i + 1
                self.prompting = not block[1].endswith("\n")
                if self.prompting:
                    self._show_cursor()     # the user types at the prompt; the next _draw hides it again
            elif kind in ("header", "text"):
                lines.append(block[1])
            elif kind == "row":
                lines.append(self._row(board, block[1], self.size().columns - 1))
            elif kind == "setup":
                lines.append(setup_line(board, self.glyphs, self.size().columns - 1, self.clock(), self.color))
            elif kind == "hint":
                lines += hint_lines(block)
            elif kind == "module":
                lines.append(module_line(block[2], self.glyphs, self.clock()))
            elif kind == "final":
                lines.append(format_final(*block[1:]))
        self._flush(lines)
        self.flushed = len(blocks)
        self._draw(force=bool(blocks))

    def _row(self, board, r, cols, pending=False):
        now = self.clock()
        cells, count, text, elapsed, cut = row_text(r, self.glyphs, now, pending)
        label = row_label(r, board.width(), now, self.color)
        if cut and text:    # a reason gives way (cut with …) before the bar does
            used = _vlen(label) + (2 + len(cells) if cells is not None else 0) + (2 + _vlen(count) if count else 0)
            text = _cut_text(text, cols - (len(elapsed) + 2 if elapsed else 0) - used - 2)
        rest = "  ".join(p for p in (count, text) if p)
        if pending and r.quiet:
            rest += f" · quiet {r.quiet}s"
        if r.running and r.progress and (not r.unit_kind or r.unit_kind == "checks"):
            rest = f"{rest} · {r.progress}" if rest else r.progress
        return _fit_row(label, cells, rest, elapsed, cols, wrap=bool(r.unit_kind))

    def _flush(self, lines):
        if lines:
            self._clear()
            if self.prompting:
                self.write("\n")           # end the prompt's partial line first; the prompt is no longer on it
                self.prompting = False
            self.write("".join(self._tint(line) + "\n" for line in lines))

    def tick(self, board):
        self.board = board
        self._draw()

    def region(self, board, cols, max_lines):
        """The running rows in start order (each with its Setup row and sub-lines), then the pending rows in plan
        order, then Overall.  When it is taller than max_lines the pending rows fold first (into "○ +N more pending",
        kept while anything is pending), then the module sub-lines (+N more running), then the lines above Overall
        are cut: Overall itself is never dropped."""
        running = board.running()
        inflight = [(r, m) for r in running for m in r.modules.values() if m.in_progress and m.id not in r.unbarred]
        subs = [(r, m) for r, m in inflight if m.outcomes or m.totals]
        collapsed = {}
        for r in running:
            names = [m.title for owner, m in inflight if owner is r and not (m.outcomes or m.totals)]
            if names:
                prefix = common_prefix([m.title for m in r.modules.values() if m.id not in r.unbarred])
                collapsed[r.id] = "    " + collapse_names([t[len(prefix):] or t for t in names], cols - 2)
        blocks = {id(m): self._sub_lines(m, cols) for _, m in subs}
        current = {r.id: "    " + _vcut(r.progress, max(1, cols - 4)) for r in running
                   if r.progress and r.unit_kind in ("tests", "files")}   # what a bar-only row is running now
        setup_running = (board.setup is not None and not board.setup.ended
                         and any(r.id == board.setup.row for r in running))
        overall = board.overall()
        row_text_ = {r.id: self._row(board, r, cols, pending=True) for r in running}
        row_lines = sum(t.count("\n") + 1 for t in row_text_.values())     # a wrapped unit bar spans several lines
        fixed = row_lines + len(collapsed) + len(current) + (1 if setup_running else 0) + (1 if overall else 0)
        pending = board.pending()
        shown = len(pending)
        if pending and fixed + sum(len(b) for b in blocks.values()) + shown > max_lines:
            shown = max(0, max_lines - fixed - sum(len(b) for b in blocks.values()) - 1)
            if shown + 1 >= len(pending):
                shown = len(pending)
        pending_lines = [_vcut(pending_line(label, self.color), cols) for _, label in pending[:shown]]
        if shown < len(pending):
            pending_lines.append(pending_line(f"+{len(pending) - shown} more pending", self.color))
        budget = max_lines - fixed - len(pending_lines)
        folded = {}
        while subs and sum(len(blocks[id(m)]) for _, m in subs) + len(folded) > budget:
            r, _ = subs.pop(0)
            folded[r.id] = folded.get(r.id, 0) + 1
        lines = []
        for r in running:
            if setup_running and board.setup.row == r.id:
                lines.append(setup_line(board, self.glyphs, cols, self.clock(), self.color))
            lines.append(row_text_[r.id])
            if r.id in current:
                lines.append(current[r.id])
            if r.id in collapsed:
                lines.append(collapsed[r.id])
            if r.id in folded:
                lines.append(f"    +{folded[r.id]} more running")
            lines += [line for owner, m in subs if owner is r for line in blocks[id(m)]]
        lines += pending_lines
        lines = [part for line in lines for part in line.split("\n")]
        if not overall:
            return lines[:max(1, max_lines)]
        return lines[:max(0, max_lines - 1)] + [_vcut(overall_text(overall), cols)]

    def _sub_lines(self, m, cols):
        """One line per module: the newest dots that fit.  When they do not all fit, the last cells hold "+N", N being
        the older results dropped (red if any of them failed, else green), so the strip keeps its full width."""
        prefix = f"    {_vcut(m.title, max(1, cols // 2))}  "
        per = max(1, cols - _vlen(prefix))
        cells = [self.dots[o] for o in m.outcomes]
        if len(cells) > per:
            for digits in range(1, len(str(len(cells))) + 1):
                hidden = len(cells) - (per - digits - 1)
                if per - digits - 1 >= 0 and len(str(hidden)) == digits:
                    break
            else:
                hidden = len(cells)                 # no room for the digits: the count is cut to the strip
            text = _vcut(f"+{hidden}", per)
            count = f"\x1b[{31 if 'fail' in m.outcomes[:hidden] else 32}m{text}\x1b[0m" if self.color else text
            cells = cells[hidden:] + [count] if per > len(text) else [count]
        return [prefix + "".join(cells)]

    def _draw(self, force=False):
        if self.closed or self.paused or self.prompting or self.board is None:
            return
        now = self.clock()
        if not force and self.last_draw is not None and now - self.last_draw < self.REDRAW_SECONDS:
            return
        size = self.size()
        lines = self.region(self.board, size.columns - 1, size.lines - 2)
        out = []
        if not self.cursor_hidden:
            out.append("\x1b[?25l")
            self.cursor_hidden = True
        if self.height:
            out.append(f"\x1b[{self.height}A")
        out += [self._tint(line) + "\x1b[K\n" for line in lines]
        out.append("\x1b[J")
        self.write("".join(out))
        self.height = len(lines)
        self.last_draw = now

    def _tint(self, line):
        return tint(line) if self.color else line

    def _clear(self):
        if self.height:
            self.write(f"\x1b[{self.height}A\x1b[J")
            self.height = 0

    def _show_cursor(self):
        if self.cursor_hidden:
            self.write("\x1b[?25h")
            self.cursor_hidden = False

    def pause(self):
        """Clear the region and give the terminal back (ask, show, passthrough, handoff)."""
        self._clear()
        self._show_cursor()
        self.paused = True

    def resume(self):
        self.paused = False
        self._draw(force=True)

    def close(self):
        if self.closed:
            return
        self._clear()
        self._show_cursor()
        self.closed = True


class Console:
    """The bus subscriber for a session: one Board, a screen renderer (live, plain or none) and the condensed log."""

    def __init__(self, sinks=None, detail=None, stream=None, clock=time.monotonic, size=shutil.get_terminal_size,
                 nested=False, repo="."):
        self.sinks = sinks or Sinks()
        self._stdout = sys.stdout       # bound once: a caller that swaps sys.stdout later must not capture the view
        self._stream = stream if stream is not None else self._stdout
        self.clock = clock
        self.size = size
        self.board = Board(resolve_detail(detail), nested=nested, full_log=self.sinks.full_path, clock=clock,
                           estimator=make_estimator(repo, script_name()))
        self.mode = self.screen_mode()
        self.screen = self._make_screen()
        self._log = None
        self.log = None
        self.board_failed = False
        self.stopped = False        # the screen view gave up; the terminal is plain again (mode is kept)
        if self.sinks.condensed_path is not None:
            self.sinks.condensed_path.parent.mkdir(parents=True, exist_ok=True)
            self._log = open(self.sinks.condensed_path, "a", encoding="utf-8")
            self.log = PlainRenderer(self._log_write, log=True, clock=clock)

    @property
    def stream(self):
        return self._stream

    def screen_mode(self):
        """live on a capable TTY in condensed mode, plain otherwise in condensed mode, None for full or none.  None
        with markers on, too: stdout is then the v1 marker stream, which the JRM run view reads with stderr merged
        in (DefaultProcessRunner's redirectErrorStream), so rows on either stream would land in it.  The condensed
        log still gets the rows."""
        if self.sinks.console != "condensed" or enabled():
            return None
        tty = getattr(self.stream, "isatty", lambda: False)()
        if not tty or os.environ.get("TERM") == "dumb" or os.environ.get(LIVE_ENV) == "0":
            return "plain"
        return "live"

    def _make_screen(self):
        if self.mode is None:
            return None
        if self.mode == "live":
            return LiveRenderer(self._screen_write, size=self.size, clock=self.clock,
                                color=not os.environ.get("NO_COLOR"))
        return PlainRenderer(self._screen_write, heartbeat=True, clock=self.clock)

    def _screen_write(self, text):
        self.stream.write(text)
        self.stream.flush()
        if self._stream is self._stdout and text:
            _seen(text.endswith("\n"))

    def _log_write(self, text):
        self._log.write(text)
        self._log.flush()

    def __call__(self, kind, fields):
        owned = not self.stopped    # say() and failure() left this event's text to the view
        if kind in self._FULL_TEXT:
            self._append_full(kind, fields)
        if self.board_failed:
            self._plain(kind, fields, self._final_blocks(kind, fields), owned)
            return
        try:
            blocks = self.board.apply(kind, fields)
        except Exception as e:  # a Board bug must never fail the run either, but no later block can be trusted: the
            self.board_failed = True    # view and the log stop for the rest of the run (the full log has everything)
            self._stop(e, log=True)
            self._plain(kind, fields, self._final_blocks(kind, fields), owned)
            return
        unshown = blocks
        if self.screen is not None:
            try:
                self.screen.render(self.board, blocks)
                unshown = []
            except Exception as e:  # a display bug must never fail the run
                unshown = blocks[getattr(self.screen, "flushed", 0):]
                self._fallback(e, unshown)
        if self.log is not None:
            try:
                self.log.render(self.board, blocks)
            except Exception as e:  # the condensed log is a convenience; the full log still has everything
                self.log = None
                self._note(f"⚠ condensed log stopped ({e!r})")
        self._plain(kind, fields, unshown, owned)

    # The script text each event carries, as the full log gets it: every level, whatever the view shows (spec 5.1).
    _FULL_TEXT = {
        "say": lambda f: f["text"],
        "note": lambda f: f"{_NOTE_PREFIX.get(f['level'], '')} {f['text']}",
        "hint": lambda f: "\n".join(f["lines"]),
        "failure": lambda f: f["text"],
    }

    def _append_full(self, kind, fields):
        """Append the event's text to the session's full log, best-effort: never a reason to fail the run."""
        if self.sinks.full_path is None:
            return
        try:
            text = self._FULL_TEXT[kind](fields)
            with open(self.sinks.full_path, "ab") as f:
                f.write((text + "\n").encode("utf-8", errors="replace"))
        except (OSError, TypeError, KeyError):
            pass

    def tick(self):
        if self.screen is not None:
            try:
                self.screen.tick(self.board)
            except Exception as e:  # a display bug must never fail the run
                self._fallback(e, [])

    def _note(self, text):
        """One warning line on the screen, if the view still draws, or as plain text once it has stopped."""
        if self.screen is not None:
            try:
                self.screen.render(self.board, [("text", text)])
            except Exception as e:  # a display bug must never fail the run
                self._fallback(e, [("text", text)][getattr(self.screen, "flushed", 0):])
        elif self.stopped:
            self._plain_write(text)

    def _final_blocks(self, kind, fields):
        """The final line's block for a done event, from what the failed Board still knows, best-effort."""
        if kind != "done":
            return []
        try:
            return self.board._on_done(fields)
        except Exception:  # nothing more can be trusted
            return []

    def _plain(self, kind, fields, unshown, owned):
        """Once the view has stopped, print what it would have drawn and nothing else will: this event's say() or
        failure() text when the view still owned the screen as they ran, and the final line."""
        if not self.stopped:
            return
        if owned and kind in ("say", "failure"):
            self._plain_write(fields["text"])
        for block in unshown:
            if block[0] == "final":
                self._plain_write(format_final(*block[1:]))

    def _plain_write(self, text):
        try:
            self._screen_write(text + "\n")
        except Exception:  # nothing more can be shown
            pass

    def _fallback(self, error, blocks):
        """Swap the screen renderer for a plain one, say so, and re-render the blocks it had not printed.  A plain
        screen that fails, or a plain re-render that fails too (the bug is in code both renderers share), stops the
        view instead; the condensed log goes on."""
        if self.mode != "live":
            self._stop(error)
            return
        self._close_screen()
        self.mode = "plain"
        self.screen = PlainRenderer(self._screen_write, heartbeat=True, clock=self.clock)
        try:
            self.screen.render(self.board, [("text", f"⚠ console view failed ({error!r}); using plain output")]
                               + blocks)
        except Exception as e:
            self._stop(e)

    def _stop(self, error, log=False):
        """Give up on the screen view for the rest of the run, and with log on the condensed log too, saying so once
        on each, best-effort.  The run goes on, with the terminal plain (stopped); the full log still has
        everything.  mode stays as it was, so console_active() keeps choosing the session's cancel and done paths."""
        screen = self.screen is not None
        log = log and self.log is not None
        stopped = " and ".join(name for name, on in (("console view", screen), ("condensed log", log)) if on)
        if not stopped:
            return
        text = f"⚠ {stopped} stopped ({error!r})" + (f"; full log: {self.sinks.full_path}"
                                                     if self.sinks.full_path is not None else "")
        if screen:
            self._close_screen()
            self.screen, self.stopped = None, True
            self._plain_write(text)
        if self.log is not None:
            if log:
                self.log = None
            try:
                self._log_write(text + "\n")
            except Exception:  # nor written
                pass

    def _close_screen(self):
        """Close the screen renderer as best it can, ending a watchdog prompt's partial line first."""
        try:
            if getattr(self.screen, "prompting", False):
                self._screen_write("\n")
            self.screen.close()
        except Exception:  # the broken renderer may not close cleanly either; at least show the cursor again
            try:
                self._screen_write("\x1b[?25h\n")
            except Exception:  # nothing more can be shown
                pass

    def pause(self):
        if self.screen is not None:
            try:
                self.screen.pause()
            except Exception as e:  # a display bug must never fail the run, nor the ask or handoff that paused
                self._fallback(e, [])

    def resume(self):
        if self.screen is not None:
            try:
                self.screen.resume()
            except Exception as e:  # a display bug must never fail the run
                self._fallback(e, [])

    def close(self):
        try:
            if self.screen is not None:
                self.screen.close()
        except Exception:  # a display bug must never fail the run, nor leave the condensed log open
            pass
        finally:
            if self._log is not None:
                self._log.close()
                self._log = None
                self.log = None


def _sigterm(signum, frame):
    """SIGTERM unwinds like Ctrl-C, so atexit closes the live region and the script's cancel path runs."""
    raise KeyboardInterrupt


def session(title, header=(), *, detail=None, sinks=None, repo="."):
    """Start this process's console view and return its Console.  Inside another session (JUNEAU_RUN_SESSION set)
    it is nested: no header and no final line, so a child script's rows join its parent's view."""
    global _console
    if _console is not None:
        return _console
    history_start(repo)
    nested = os.environ.get(SESSION_ENV) == "1"
    sinks = sinks or Sinks()
    _console = subscribe(Console(sinks, detail, nested=nested, repo=repo))
    os.environ[SESSION_ENV] = "1"   # only once the console is up, so a failed start leaves children un-nested
    atexit.register(_end_session)
    if not nested and sinks.full_path is not None and sinks.full_path.parent == full_log_dir():
        prune_full_logs()
    try:
        if signal.getsignal(signal.SIGTERM) == signal.SIG_DFL:
            signal.signal(signal.SIGTERM, _sigterm)
    except ValueError:  # not the main thread
        pass
    lines = list(header) + ([f"tail -f {shlex.quote(str(sinks.full_path))}"] if sinks.full_path is not None else [])
    publish("session", title=title, header=lines, nested=nested)
    return _console


def plan(ids):
    """Declare the planned top-level steps, in order, for the Overall figure (spec 6.1).  Each entry is a step id, or
    (id, label): a labelled step draws as a dim pending row (○) until it starts; an unlabelled one only counts toward
    Overall.  Replaces any earlier plan; a duplicate id raises ValueError."""
    entries = [e if isinstance(e, str) else tuple(e) for e in ids]
    ids = [e if isinstance(e, str) else e[0] for e in entries]
    # validated here as well as in Session.plan so a bad plan fails in a plain run, not only under a view
    if len(set(ids)) != len(ids):
        raise ValueError("duplicate step id in plan: " + ", ".join(sorted({i for i in ids if ids.count(i) > 1})))
    publish("plan", ids=ids, labels={e[0]: e[1] for e in entries if not isinstance(e, str) and e[1]})


def session_open():
    """True when this process has a console view (session() was called and the view has not been ended)."""
    return _console is not None


def end_session():
    """Close this process's console view now, rather than at exit.  A no-op with no session; safe to call twice.  For a
    caller that runs several sessions in one process (a script's own test suite)."""
    atexit.unregister(_end_session)
    _end_session()


def _end_session():
    global _console
    if _console is not None:
        unsubscribe(_console)
        _console.close()
        _console = None
        try:
            if signal.getsignal(signal.SIGTERM) is _sigterm:    # ours: session() only replaces the default
                signal.signal(signal.SIGTERM, signal.SIG_DFL)
        except ValueError:  # not the main thread
            pass


# ---------------------------------------------------------------------------------------------------------------------
# Script API
#
# The ways a script talks to the terminal while a session's view is drawn.  Each pauses the view (clearing a live
# region), uses the real terminal, then resumes.  All of them work, and only print, when there is no session.
# ---------------------------------------------------------------------------------------------------------------------


def console_active():
    """True when this process's session draws a condensed view (live or plain) on the terminal."""
    return _console is not None and _console.mode is not None


@contextmanager
def _paused():
    global _pause_depth
    if _console is not None and _pause_depth == 0:
        _console.pause()
    _pause_depth += 1
    try:
        yield
    finally:
        _pause_depth -= 1
        if _console is not None and _pause_depth == 0:
            _console.resume()


def paused():
    """Pause the console view for the block, so a prompt or an interactive child isn't drawn over.  Nests; a no-op
    with no session."""
    return _paused()


def ask(prompt):
    """Read one line from the terminal with the view paused.  The prompt and answer also go to the condensed log."""
    with _paused():
        answer = input(prompt)
    publish("ask", prompt=prompt, answer=answer)
    return answer


def show(text, pager=False):
    """Print text in full with the view paused, or pipe it to $PAGER (less -R) when pager=True on a terminal.  A
    $PAGER that cannot be parsed or started prints the text instead.  Ctrl-C in the pager belongs to the pager and
    does not cancel the caller; quitting it (q in less) returns."""
    with _paused():
        if not (pager and sys.stdout.isatty() and _page(text)):
            _out(text if text.endswith("\n") else text + "\n")
    publish("show", text=text)


def _page(text):
    """Pipe text to $PAGER (less -R, also when $PAGER is blank).  False when $PAGER cannot be parsed or started.

    As git does, SIGINT is ignored here while the pager runs: the pager handles Ctrl-C itself, and a KeyboardInterrupt
    in the wait would kill it with the terminal still in its mode (no echo, the alternate screen)."""
    try:
        cmd = shlex.split(os.environ.get("PAGER") or "") or ["less", "-R"]
        proc = subprocess.Popen(cmd, stdin=subprocess.PIPE, text=True)
    except (OSError, ValueError):   # no such pager, or unbalanced quotes in $PAGER
        return False
    try:
        previous = signal.signal(signal.SIGINT, signal.SIG_IGN)
    except ValueError:  # not the main thread
        previous = None
    try:
        try:
            with proc:  # on every exit path, closes the pipe and waits for the pager, still under SIG_IGN
                proc.stdin.write(text)
        except BrokenPipeError:     # the pager was quit before it read everything, on the write or the final flush
            pass
    finally:
        if previous is not None:
            signal.signal(signal.SIGINT, previous)
    return True


def passthrough(cmd, step_id, title, *, n=None, parent=None, cwd=None, env=None, label=None):
    """Run an interactive command on the real terminal, as a step whose row shows its outcome: no capture, no new
    session, nothing copied to the full log.  Returns Result(exit, ms)."""
    emit("step", id=step_id, n=n if n is not None else 1, title=title, parent=parent)
    publish("step_start", id=step_id, n=n if n is not None else 1, title=title, parent=parent, tool=None, sig="",
            **({"label": label} if label else {}))
    t0 = time.monotonic()
    rc = 130
    try:
        with _paused():
            rc = subprocess.run(cmd, cwd=cwd, env=env).returncode
    except OSError:
        rc = 127   # cannot start: not a Ctrl-C
        raise
    finally:
        ms = int((time.monotonic() - t0) * 1000)
        status = "ok" if rc == 0 else "fail"
        emit("end", id=step_id, status=status, ms=ms, exit=None if rc == 0 else rc)
        publish("step_end", id=step_id, status=status, ms=ms, exit=None if rc == 0 else rc, summary=None,
                totals=None)
    return Result(rc, ms)


@contextmanager
def handoff():
    """Give the terminal to a child session (push.py running test.py) for the block.  Yields the env the child needs
    so its rows line up with ours (label width, detail level) and our pending rows stay visible under its own
    (JUNEAU_RUN_PENDING); empty with no session."""
    if _console is None:
        yield {}
        return
    env = {LABEL_WIDTH_ENV: str(_console.board.width()), DETAIL_ENV: _console.board.detail}
    pending = list(_console.board.pending())
    if pending:
        env[PENDING_ENV] = json.dumps([[i, label] for i, label in pending])
    with _paused():
        yield env


# ---------------------------------------------------------------------------------------------------------------------
# Sinks
# ---------------------------------------------------------------------------------------------------------------------

CONSOLE_MODES = ("full", "condensed", "none")


def warn(text):
    """Wrapper diagnostics always go to stderr, never into the tool's output stream."""
    print(f"juneau-run: {text}", file=sys.stderr, flush=True)


FULL_LOG_KEEP = 20


def full_log_dir():
    """Where condensed mode's default full logs go: $JUNEAU_RUN_LOG_DIR, else ~/.cache/juneau-run."""
    return Path(os.environ.get("JUNEAU_RUN_LOG_DIR") or Path.home() / ".cache" / "juneau-run")


def safe_name(text, fallback="run"):
    """A file-name-safe form of `text`: every run of whitespace or punctuation (anything but a word character or a dot)
    becomes one `-`, leading and trailing `-` and `.` are trimmed, and an empty result is `fallback`."""
    return re.sub(r"[^\w.]+", "-", text or "").strip("-.") or fallback


def default_full_log():
    """<full_log_dir>/<tool>-<YYYYMMDD-HHMMSS>.log, <tool> being the running script's name (push, test, release).
    The file is created here, exclusively, so two runs in the same second get -2, -3, ... instead of one shared log."""
    tool = safe_name(Path(sys.argv[0]).stem) if sys.argv and sys.argv[0] else "run"
    base = f"{tool}-{time.strftime('%Y%m%d-%H%M%S')}"
    directory = full_log_dir()
    try:
        directory.mkdir(parents=True, exist_ok=True)
    except OSError:
        return directory / f"{base}.log"
    for n in range(1, 1000):
        path = directory / (f"{base}.log" if n == 1 else f"{base}-{n}.log")
        try:
            os.close(os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o644))
            return path
        except FileExistsError:
            continue
        except OSError:
            break
    return directory / f"{base}.log"


# --- History store (spec 4.1, 4.4) ---

HISTORY_KEEP = 50


def history_enabled():
    return os.environ.get("JUNEAU_RUN_HISTORY") != "0"


def history_root():
    return Path(os.environ.get("JUNEAU_RUN_HISTORY_DIR") or Path.home() / ".cache" / "juneau-run" / "history")


def script_name():
    """The running script's sanitized name (push, test, release), or `run`."""
    return safe_name(Path(sys.argv[0]).stem) if sys.argv and sys.argv[0] else "run"


def repo_key(repo="."):
    """<basename>-<sha1[:8]> of the git common dir, so linked worktrees share one history (spec 4.1)."""
    base = Path(repo).resolve()
    anchor = base
    try:
        out = subprocess.run(["git", "-C", str(base), "rev-parse", "--git-common-dir"],
                             capture_output=True, text=True, timeout=10)
        if out.returncode == 0 and out.stdout.strip():
            anchor = Path(os.path.realpath(base / out.stdout.strip()))
    except (OSError, subprocess.SubprocessError):
        pass
    name = anchor.parent.name if anchor.name == ".git" else anchor.name
    return f"{safe_name(name)}-{hashlib.sha1(str(anchor).encode()).hexdigest()[:8]}"


class HistoryStore:
    """One script's append-only JSONL history for one repo (spec 4.4).  Every failure is swallowed.  The path (and the
    git call behind it) is resolved on first use, so a disabled store does no work."""
    COMPACT_BYTES = 256 * 1024
    MAX_RECORD = 64 * 1024   # a full Juneau reactor tests record (subs plus three series) is about 20 KB
    KEEP_SLACK = 25             # a key may exceed HISTORY_KEEP by this many records before it forces a compaction
    LOCK_WAIT = 2.0             # seconds to wait for the write lock before skipping the write

    def __init__(self, repo, script):
        self._repo, self._script = repo, script
        self._path = None
        self._floor = 0         # the file size after the last compaction

    @property
    def path(self):
        if self._path is None:
            self._path = history_root() / repo_key(self._repo) / f"{self._script}.jsonl"
        return self._path

    def _lock(self, lock):
        deadline = time.monotonic() + self.LOCK_WAIT
        while True:
            try:
                fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
                return True
            except OSError:
                if time.monotonic() >= deadline:
                    return False
                time.sleep(0.02)

    def append(self, record):
        if not history_enabled():
            return
        try:
            line = json.dumps(record, separators=(",", ":")).encode()
            if len(line) > self.MAX_RECORD:
                line = json.dumps({k: v for k, v in record.items() if k not in ("subs", "series")},
                                  separators=(",", ":")).encode()
                if len(line) > self.MAX_RECORD:
                    return
            self.path.parent.mkdir(parents=True, exist_ok=True)
            with open(str(self.path) + ".lock", "a") as lock:
                if not self._lock(lock):
                    return
                fd = os.open(self.path, os.O_WRONLY | os.O_APPEND | os.O_CREAT, 0o644)
                try:
                    if os.fstat(fd).st_size and not self._ends_with_newline():
                        line = b"\n" + line
                    os.write(fd, line + b"\n")
                finally:
                    os.close(fd)
                size = self.path.stat().st_size
                if size > self.COMPACT_BYTES and (self._floor == 0 or size > 2 * self._floor or self._crowded()):
                    self._compact()
        except Exception:
            pass

    def _crowded(self):
        counts = collections.Counter((r["step"], r["tool"]) for r in self.read())
        return any(n > HISTORY_KEEP + self.KEEP_SLACK for n in counts.values())

    def _ends_with_newline(self):
        with open(self.path, "rb") as f:
            f.seek(-1, os.SEEK_END)
            return f.read(1) == b"\n"

    @staticmethod
    def _valid(r):
        """A well-typed v1 record: every field the estimate or compaction reads has the type they expect."""
        def num(x):
            return isinstance(x, (int, float)) and not isinstance(x, bool)
        if not isinstance(r, dict) or type(r.get("v")) is not int or r["v"] != 1:
            return False
        if not isinstance(r.get("step"), str) or not isinstance(r.get("tool"), str):
            return False
        if not all(isinstance(r.get(k, ""), str) for k in ("sig", "ts", "commit")):
            return False
        if not all(isinstance(r.get(k, False), bool) for k in ("ok", "complete")):
            return False
        if r.get("wall_s") is not None and not num(r["wall_s"]):
            return False
        if r.get("tests") is not None and not num(r["tests"]):
            return False
        if r.get("title") is not None and not isinstance(r["title"], str):
            return False
        u = r.get("units")
        if u is not None and not (isinstance(u, dict) and all(num(u.get(k, 0)) for k in ("total", "done"))):
            return False
        return all(r.get(k) is None or isinstance(r[k], dict) for k in ("subs", "series"))

    def read(self):
        """The valid v1 records, oldest first; unreadable or malformed lines are skipped."""
        if not history_enabled():
            return []
        out = []
        try:
            with open(self.path, "rb") as f:
                for raw in f:
                    try:
                        r = json.loads(raw)
                    except ValueError:
                        continue
                    if self._valid(r):
                        out.append(r)
        except OSError:
            return []
        return out

    def _compact(self):
        seen, keep = {}, []
        for r in reversed(self.read()):
            n = seen.get((r["step"], r["tool"]), 0)
            if n < HISTORY_KEEP:
                seen[(r["step"], r["tool"])] = n + 1
                keep.append(r)
        keep.reverse()
        tmp = str(self.path) + ".tmp"
        with open(tmp, "wb") as f:
            for r in keep:
                f.write(json.dumps(r, separators=(",", ":")).encode() + b"\n")
        os.replace(tmp, self.path)
        self._floor = self.path.stat().st_size


class History:
    """The bus subscriber that writes one history record per finished top-level row (spec 4.2, 4.3).  A child row
    (started with parent=) folds into its parent's record.  Never raises."""
    SUBS_CAP = 400

    def __init__(self, store, clock=time.monotonic, commit=None):
        self.store = store
        self.clock = clock
        self.commit = commit
        self.rows = {}          # top-level row id -> state
        self.aliases = {}       # a child row's id -> its top-level row's id
        self.open = []          # ids of the top-level rows that have started and not ended, oldest first

    def current(self):
        """The id of the most recently started top-level row that has not ended, else None."""
        return self.open[-1] if self.open else None

    def __call__(self, kind, fields):
        self.on_event(kind, fields)

    def on_event(self, kind, f):
        try:
            handler = getattr(self, "_on_" + kind, None)
            if handler is not None:
                handler(f)
        except Exception:
            pass

    @staticmethod
    def _key(step_id, name):
        """A sub/series key without its "<step>/" prefix: the module id or task path (spec 4.2)."""
        name = str(name)
        prefix = f"{step_id}/"
        return name[len(prefix):] if name.startswith(prefix) and len(name) > len(prefix) else name

    def _row(self, step_id):
        return self.rows.get(self.aliases.get(step_id, step_id))

    def _on_step_start(self, f):
        parent = f.get("parent")
        if parent is not None and self._row(parent) is not None:
            row = self._row(parent)
            self.aliases[f["id"]] = parent if parent in self.rows else self.aliases[parent]
            if f.get("tool") and not row["tool"]:
                row["tool"], row["sig"] = f["tool"], f.get("sig") or ""
            return
        if self.commit is None:
            self.commit = _git("rev-parse", "--short", "HEAD", cwd=getattr(self.store, "_repo", None)) or ""
        self.rows[f["id"]] = {"title": f.get("title"), "tool": f.get("tool"), "sig": f.get("sig") or "",
                              "commit": self.commit, "units": None, "tests": None,
                              "subs": {}, "series": {}, "poms": set(), "counted": set()}
        if f["id"] in self.open:
            self.open.remove(f["id"])
        self.open.append(f["id"])

    def _on_units(self, f):
        row = self._row(f["step"])
        if row is not None:
            u = row["units"] or {"kind": f["kind"], "total": 0, "done": 0}
            u["kind"], u["total"] = f["kind"], max(u["done"], f["total"])    # replaces; clamped to the units done
            row["units"] = u

    def _on_unit(self, f):
        row = self._row(f["step"])
        if row is not None:
            u = row["units"] or {"kind": None, "total": 0, "done": 0}
            u["done"] = max(u["done"], f["done"])
            u["total"] = max(u["total"], u["done"])
            row["units"] = u

    def _on_reactor(self, f):
        row = self._row(f["step"])
        if row is not None and f["size"] > 0:
            row["poms"] = set(f["poms"])
            u = row["units"] or {"kind": "modules", "total": 0, "done": 0}
            u["kind"], u["total"] = "modules", max(0, f["size"] - len(row["poms"]), u["done"])
            row["units"] = u

    def _on_series(self, f):
        row = self._row(f["step"])
        if row is not None:
            row["series"].setdefault(f["name"], {})[self._key(f["step"], f["sub"])] = f["value"]

    def _on_tests(self, f):
        row = self._row(f["step"])
        if row is not None:
            row["tests"] = f["total"]

    def _on_module_end(self, f):
        row = self._row(f["step"])
        if row is None:
            return
        module = f["module"]
        key = self._key(f["step"], module)
        if key in row["subs"] or len(row["subs"]) < self.SUBS_CAP:
            row["subs"][key] = round(f["ms"] / 1000, 3)
        if module in row["poms"] or module in row["counted"]:
            return
        if row["units"] is None and row["tool"] == "gradle":
            row["units"] = {"kind": "tasks", "total": 0, "done": 0}
            row["gradle"] = True
        u = row["units"]
        if u is not None and u["kind"] == "modules":
            row["counted"].add(module)
            u["done"] += 1
            u["total"] = max(u["total"], u["done"])
        elif u is not None and row.get("gradle"):
            row["counted"].add(module)
            u["done"] += 1
            u["total"] = u["done"]

    def _on_step_end(self, f):
        if f["id"] in self.aliases:
            return
        row = self.rows.pop(f["id"], None)
        if f["id"] in self.open:
            self.open.remove(f["id"])
        self.aliases = {k: v for k, v in self.aliases.items() if v != f["id"]}
        if row is None or f["status"] == "skip":
            return
        ok = f["status"] == "ok"
        u = row["units"]
        record = {"v": 1, "ts": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()), "step": f["id"],
                  "tool": row["tool"] or "step", "title": row["title"], "sig": row["sig"], "commit": row["commit"],
                  "ok": ok, "complete": ok and (not u or u["done"] == u["total"]), "wall_s": f["ms"] / 1000}
        if u:
            record["units"] = {"kind": u["kind"], "total": u["total"], "done": u["done"]}
        if row["tests"] is not None:
            record["tests"] = row["tests"]
        if row["subs"]:
            record["subs"] = row["subs"]
        if row["series"]:
            record["series"] = row["series"]
        self.store.append(record)


def history_start(repo=".", history=None):
    """Subscribe this process's History to the bus (once).  Does nothing when history is disabled.  `history` is a
    prebuilt History, for tests."""
    global _history
    if _history is not None or not history_enabled():
        return
    _history = subscribe(history or History(HistoryStore(repo, script_name())))


def history_series(name, values):
    """Record a named series of per-sub numbers, `{sub: number}`, on the innermost running top-level row (spec 4.3).
    Does nothing without a running row or when history is off; never raises."""
    try:
        if _history is None or not isinstance(name, str) or not re.fullmatch(r"[a-z0-9_.]{1,64}", name):
            return
        step_id = _history.current()
        if step_id is None:
            return
        for sub, value in values.items():
            if isinstance(value, (int, float)) and not isinstance(value, bool) and value == value \
                    and abs(value) != float("inf"):
                publish("series", step=step_id, name=name, sub=str(sub), value=float(value))
    except Exception:
        pass


def make_estimator(repo, script, store=None):
    """The Board's estimator: fn(step, tool, sig) -> Estimate or None, from this script's history in `repo` (spec 5.3).
    The store is read afresh for each lookup.  A `tool` of None takes the tool of the newest record for the step that
    matches `sig` (else the newest record's).  Like-for-like records are those with the same `sig` (spec 5.1).  Off
    when history is disabled."""
    if not history_enabled():
        return lambda step, tool, sig=None: None
    store = store or HistoryStore(repo, script)

    def estimator(step, tool, sig=None):
        records = [r for r in store.read() if r["step"] == step]
        if tool is None and records:
            same = [r for r in records if sig and r.get("sig") == sig]
            tool = (same or records)[-1]["tool"]
        records = [r for r in records if r["tool"] == tool]
        return estimate_from(records, sig)

    return estimator


def prune_full_logs(keep=FULL_LOG_KEEP):
    """Delete all but the newest `keep` *.log files in full_log_dir(), best-effort."""
    try:
        logs = sorted(full_log_dir().glob("*.log"), key=lambda p: p.stat().st_mtime, reverse=True)
    except OSError:
        return
    for old in logs[keep:]:
        try:
            old.unlink()
        except OSError:
            pass


class Sinks:
    """Where the full and condensed streams go.  Arguments beat JUNEAU_RUN_* environment variables.
    A condensed or none console with no full log gets default_full_log()."""

    def __init__(self, console=None, full_log=None, condensed_log=None):
        self.console = console or os.environ.get("JUNEAU_RUN_CONSOLE") or "full"
        if self.console not in CONSOLE_MODES:
            raise ValueError(f"console must be one of {CONSOLE_MODES}, not {self.console!r}")
        full = full_log or os.environ.get("JUNEAU_RUN_FULL_LOG")
        if not full and self.console in ("condensed", "none"):
            # Condensed and none always have a log: to tail, and to recover the output nothing else shows.  Exported, so later Sinks() and child scripts write the same one.
            full = str(default_full_log())
            os.environ["JUNEAU_RUN_FULL_LOG"] = full
        self.full_path = self._resolve(full)
        self.condensed_path = self._resolve(condensed_log or os.environ.get("JUNEAU_RUN_CONDENSED_LOG"))
        self._full = None
        self._condensed = None

    @staticmethod
    def _resolve(path):
        """Relative log paths live under $RUN_ARTIFACTS when it is set, otherwise under the current directory."""
        if not path:
            return None
        p = Path(path)
        if not p.is_absolute() and artifacts_dir() is not None:
            p = artifacts_dir() / p
        return p

    @property
    def is_default(self):
        """True when output is exactly what running the tool directly would show."""
        return self.console == "full" and self.full_path is None and self.condensed_path is None

    def open(self):
        """Open the log files for append, creating parent directories."""
        if self.full_path is not None:
            self.full_path.parent.mkdir(parents=True, exist_ok=True)
            self._full = open(self.full_path, "ab")
        if self.condensed_path is not None:
            self.condensed_path.parent.mkdir(parents=True, exist_ok=True)
            self._condensed = open(self.condensed_path, "ab")

    def close(self):
        for f in (self._full, self._condensed):
            if f is not None:
                f.close()
        self._full = self._condensed = None

    def write_full(self, chunk):
        """chunk is raw bytes exactly as the tool wrote them."""
        if self._full is not None:
            self._full.write(chunk)
            self._full.flush()
        if self.console == "full":
            sys.stdout.flush()
            buffer = getattr(sys.stdout, "buffer", None)
            if buffer is not None:
                buffer.write(chunk)
                buffer.flush()
            else:
                sys.stdout.write(chunk.decode("utf-8", errors="replace"))
                sys.stdout.flush()
            if chunk:
                _seen(chunk.endswith(b"\n"))

    def write_condensed(self, text):
        """text is one condensed line without its newline."""
        if _console is not None:
            return          # the session's Console renders the condensed view instead
        if self._condensed is not None:
            self._condensed.write((text + "\n").encode("utf-8"))
            self._condensed.flush()
        if self.console == "condensed":
            _out((tint(text) if _colour_stdout() else text) + "\n")


# ---------------------------------------------------------------------------------------------------------------------
# Parsers
#
# A parser is pure: it turns lines into events and never writes output.  Event tuples:
#   ("sub_start", id, n, title)          ("sub_end", id, status, ms)
#   ("tests", id, total, fail, err, skip) ("report", id, kind, path)
#   ("note", level, text, id)            ("cond", text)
#   ("class_done", id, outcome)          ("compiled", id, ok)          ("tested", id)
#   ("reactor", size, [pom sub ids], tests_mode[, [trailing pom sub ids]])    ("failure", text, id)
#   ("progress", id, text)               bus only (step_progress), never a marker
#   ("units", id, total, kind)           ("unit", id, done, outcome)   bus only (units / unit), never a marker
#   ("unit_fix", id, index, outcome)     ("series", id, name, sub, value)   bus only (unit_fix / series)
#   poll() and finish() return the same tuples; render(line) returns text, not events (see Parser.render)
# ---------------------------------------------------------------------------------------------------------------------

ANSI_RE = re.compile(r"\x1b\[[0-9;?]*[A-Za-z]|\x1b\][^\x07\x1b]*(?:\x07|\x1b\\)")


def strip_ansi(text):
    return ANSI_RE.sub("", text)


def _program_index(cmd):
    """Index of the real program in cmd, past the launchers in front of it (env with its options and NAME=value pairs,
    npx/bunx, pnpm/yarn, npm exec); len(cmd) if nothing follows them.  A parser that edits argv inserts its options
    after this program, not after cmd[0].  Kept here because juneau_run must run without juneau_scripts;
    juneau_scripts.parsers.program_index is the same walk (a test holds them together)."""
    i = 0
    while i < len(cmd):
        prog = os.path.basename(cmd[i].replace("\\", "/"))
        if prog == "env":
            i += 1
            while i < len(cmd):
                a = cmd[i]
                if a in ("-i", "--ignore-environment", "-"):
                    i += 1
                elif a in ("-u", "--unset"):
                    i += 2
                elif a.startswith("--unset=") or ("=" in a and not a.startswith("-")):
                    i += 1
                else:
                    break
        elif prog in ("npx", "bunx"):
            i += 1
            while i < len(cmd) and cmd[i].startswith("-"):
                i += 1
        elif prog == "pnpm":
            i += 2 if cmd[i + 1:i + 2] in (["exec"], ["dlx"]) else 1
        elif prog == "yarn":
            i += 2 if cmd[i + 1:i + 2] in (["run"], ["exec"], ["dlx"]) else 1
        elif prog == "npm" and cmd[i + 1:i + 2] == ["exec"]:
            i += 3 if cmd[i + 2:i + 3] == ["--"] else 2
        else:
            return i
    return len(cmd)


class Parser:
    """Base parser: no sub-steps, no counts.  step_id and interactive are set by the runner before prepare().

    interactive is True when the tool runs under a terminal (tty=True or --pty): prepare() must not then change what the
    tool prints (no --console=plain), only add side channels such as a report file.  A parser that sets rewrites_log
    gets render() called for every complete output line in piped mode, and the full log holds what it returns."""

    name = "base"
    interactive = False
    rewrites_log = False

    def __init__(self):
        self.step_id = None

    def prepare(self, cmd, env, cwd):
        """May adjust the command or environment; called once before the tool starts, in piped, tty and PTY mode."""
        return cmd, env

    def on_line(self, line):
        return self.handle(strip_ansi(line))

    def handle(self, line):
        return []

    def poll(self):
        """Called on every pass of the runner's read loop, output or not (at least every 0.1 s): events from a side
        file the tool writes."""
        return []

    def finish(self, exit_code):
        return []

    def close(self):
        """Called once after the step, however it ended (finish, cancel, error, the tool not starting): remove temp
        files.  Called even after the parser was disabled by an error."""

    def render(self, line):
        """The full log's text for one output line (rewrites_log only, piped mode only).  The runner adds a newline
        unless the text ends with one; "" drops the line.  Under a terminal the log is always the tool's own bytes."""
        return line


class GenericParser(Parser):
    """Condensed output is the last 40 lines, on a non-zero exit only."""

    name = "generic"
    TAIL = 40

    def __init__(self):
        super().__init__()
        self.tail = collections.deque(maxlen=self.TAIL)

    def handle(self, line):
        self.tail.append(line)
        return []

    def finish(self, exit_code):
        if exit_code == 0 or not self.tail:
            return []
        return [("cond", line) for line in self.tail] + [("failure", "\n".join(self.tail), self.step_id)]


def _seconds(text):
    """'7.513 s' -> 7.513, '02:45 min' -> 165.0, '01:28 h' -> 5280.0."""
    text = text.strip()
    m = re.match(r"^(\d+):(\d+)\s*(min|h)?$", text)
    if m:
        a, b, unit = int(m.group(1)), int(m.group(2)), m.group(3)
        return a * 3600 + b * 60 if unit == "h" else a * 60 + b
    m = re.match(r"^([\d.]+)\s*s?$", text)
    return float(m.group(1)) if m else 0.0


def _fmt_seconds(seconds):
    return f"{seconds:.1f}s"


def _slug(text):
    return re.sub(r"[^A-Za-z0-9._-]+", "-", text).strip("-").lower() or "module"


def read_pom_tree(root, packaging=None):
    """Walk pom.xml and its <modules> recursively.  Returns ({artifactId: dir}, {name: artifactId}).  When packaging
    is a dict, it is filled with {artifactId: packaging} (jar when the pom doesn't say)."""
    dirs, names = {}, {}
    seen = set()

    def local(tag):
        return tag.rsplit("}", 1)[-1]

    def walk(d):
        d = Path(d)
        pom = d / "pom.xml"
        if pom in seen or not pom.is_file():
            return
        seen.add(pom)
        try:
            top = ET.parse(pom).getroot()
        except ET.ParseError:
            return
        artifact = name = None
        kind = "jar"
        modules = []
        for child in top:
            tag = local(child.tag)
            if tag == "artifactId":
                artifact = (child.text or "").strip()
            elif tag == "name":
                name = (child.text or "").strip()
            elif tag == "packaging":
                kind = (child.text or "").strip() or "jar"
            elif tag == "modules":
                modules = [(m.text or "").strip() for m in child if local(m.tag) == "module"]
        if artifact:
            dirs[artifact] = d
            if packaging is not None:
                packaging[artifact] = kind
            if name and "${" not in name:
                names[name] = artifact
        for m in modules:
            walk(d / m)

    walk(root)
    return dirs, names


class MavenParser(Parser):
    """Reactor-aware Maven output: module sub-steps, Surefire counts, reports and failure notes."""

    name = "maven"
    BUILDING = re.compile(r"^\[INFO\] Building (.+?) (\S+)\s+\[(\d+)/(\d+)\]\s*$")
    SUMMARY_HEAD = re.compile(r"^\[INFO\] Reactor Summary")
    # Maven drops the dot leader when the name fills the column, so the dots are optional.
    SUMMARY_ROW = re.compile(r"^\[INFO\] (.+?)(?: \.+)? (SUCCESS|FAILURE|SKIPPED)(?: \[\s*([^\]]+?)\s*\])?\s*$")
    HEADER = re.compile(r"^\[INFO\] -+< (\S+):(\S+) >-+\s*$")
    ORDER_HEAD = re.compile(r"^\[INFO\] Reactor Build Order:\s*$")
    ORDER_ROW = re.compile(r"^\[INFO\] (.+?)\s+\[(\w[\w-]*)\]\s*$")
    ANY_PLUGIN = re.compile(r"^\[INFO\] --- (\S+?):(\S+):(\S+) \(.*\) @ (\S+) ---")
    COMPILE_PATH = re.compile(r"^\[ERROR\] (\S+\.java):\[")
    SKIP_TESTS = re.compile(r"(?:^|\s)-D(?:skipTests|maven\.test\.skip)(?:=true)?(?=\s|$)")
    FILE_OPTION = re.compile(r"(?:^|\s)(?:-f|--file)(?:=|\s+)(\S+)")
    CLASS_LINE = re.compile(      # searched, not anchored: a verbose test run elsewhere can leave a line open for this one to land on
        r"(?:\[(?:INFO|WARNING|ERROR)\] )?Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+), "
        r"Time elapsed: [\d.,]+ s(?: <<< (?:FAILURE|ERROR)!)? -- in (.+?)\s*$")    # a @DisplayName can hold spaces
    CLASS_TIME = re.compile(r"Time elapsed: ([\d.,]+) s")      # kept apart from CLASS_LINE so its group numbers stay put
    FAILED_TEST = re.compile(r"^(?:\[(?:ERROR|WARNING)\] )?\S.+ <<< (?:FAILURE|ERROR)!\s*$")
    COMPILE_ERROR = re.compile(r"^\[ERROR\] \S+\.java:\[\d+,\d+\] .+")
    COMPILE_GOAL_FAILED = re.compile(r"^\[ERROR\] Failed to execute goal \S+:maven-compiler-plugin:\S+ \(.*\) on project ([^\s:]+)")
    # A module can run a non-Surefire suite under the exec plugin (juneau-run's Python unittest).  Its summary counts
    # only between that plugin's header and the next one, and only in unittest's own two-line shape.
    EXEC_GOALS = ("exec", "exec-maven-plugin")
    UNITTEST_RAN = re.compile(r"(?:^|\s)Ran (\d+) tests? in ([\d.]+)s\s*$")
    UNITTEST_RESULT = re.compile(r"(?:^|\s)(?:OK(?: \(([^)]*)\))?|FAILED \(([^)]*)\))\s*$")
    UNITTEST_COUNT = re.compile(r"(failures|errors|skipped)=(\d+)")
    STATUS = {"SUCCESS": "ok", "FAILURE": "fail", "SKIPPED": "skip"}
    FAILURE_TRACE_LINES = 10
    STALE_SLACK_SECONDS = 2.0

    def __init__(self):
        super().__init__()
        self.dirs, self.names = {}, {}
        self.parallel = False
        self.cwd = None
        self.started = time.time()
        self.total = self.fail = self.err = self.skip = 0
        self.mod = {}                    # sub id -> [total, fail, err, skip]
        self.open = {}                   # sub id -> (name, start time)
        self.ended = set()
        self.titles = {}                 # sub id -> name
        self.sub_of = {}                 # display name -> sub id, so the Reactor Summary finds what Building opened
        self.header_artifact = None      # artifactId of the nearest preceding `< g:a >` header
        self.surefire_sub = None
        self.exec_pending = []           # sub ids of exec unittest modules still owed their `Ran N tests` summary, oldest first
        self.unittest_ran = None         # (tests, seconds) from a `Ran N tests` line still waiting for its result line
        self.surefire_s = {}             # sub id -> summed class seconds, for the history `series` entry
        self.class_cache = {}            # test class FQN -> artifactIds of the modules containing it
        self.packaging = {}              # artifactId -> packaging
        self.order_poms = []             # sub ids of the pom-packaged members built before the block's first real module
        self.order_tail = []             # sub ids of the pom-packaged members built after it (an aggregator root)
        self.in_order = False            # inside the Reactor Build Order block
        self.order_members = []          # (title, packaging) rows read so far
        self.phase = {}                  # artifactId -> compile, testCompile, compiled, surefire, tested or done
        self.compile_failed = set()      # sub ids already painted red by a compile error
        self.last_compiler = None        # artifactId of the last compiler:* header
        self.tests_mode = True
        self.reactor_sent = False
        self.building_seen = False
        self.in_summary = False
        self.summary_index = 0
        self.failed_modules = []
        self.failure_trace = []
        self.trace_left = 0
        self.cond_window = 0
        self.first_error = None
        self.structure_seen = False
        self.generic = GenericParser()

    def prepare(self, cmd, env, cwd):
        self.started = time.time()
        self.cwd = Path(cwd) if cwd else Path.cwd()
        # A "sh -c" command is one string, so look for options at a word boundary rather than as whole arguments.
        at = _program_index(cmd) + 1
        args = " ".join(cmd[at:])
        root = self.cwd
        m = self.FILE_OPTION.search(args)
        if m:
            pom = self.cwd / m.group(1)
            root = pom.parent if pom.suffix == ".xml" else pom
        self.packaging = {}
        self.dirs, self.names = read_pom_tree(root, self.packaging)
        self.parallel = any(re.search(r"(?:^|\s)(?:-T|--threads)", a) for a in cmd[at:])
        self.tests_mode = not self.SKIP_TESTS.search(args)
        return cmd, env

    def _reactor(self, size):
        if self.order_tail:     # the optional fifth field: absent when there is no trailing pom
            return ("reactor", size, list(self.order_poms), self.tests_mode, list(self.order_tail))
        return ("reactor", size, list(self.order_poms), self.tests_mode)

    def _has_tests(self, artifact):
        d = self.dirs.get(artifact)
        return d is None or any((d / "src" / "test" / lang).is_dir() for lang in ("java", "python"))

    def _plugin_header(self, plugin, goal, artifact):
        """Advance one artifact's compile/test phase at a plugin header; returns compiled/tested events."""
        if self.packaging.get(artifact) == "pom":
            return []
        sub = f"{self.step_id}/{artifact}"
        phase = self.phase.get(artifact)
        events = []
        if phase == "testCompile" or (phase == "compile" and not self._has_tests(artifact)):
            phase = "compiled"
            if sub not in self.compile_failed:
                events.append(("compiled", sub, True))
            if self.tests_mode and not self._has_tests(artifact):
                phase = "done"
                events.append(("tested", sub))
        elif phase == "surefire":
            phase = "tested"
            events.append(("tested", sub))
        if plugin == "compiler":
            self.last_compiler = artifact
            if phase in (None, "compile"):
                phase = "testCompile" if goal == "testCompile" else "compile"
        elif plugin in ("surefire", "failsafe") and phase in ("compiled", "tested") and self.tests_mode:
            phase = "surefire"
        self.phase[artifact] = phase
        return events

    def _compile_error(self, line):
        """compiled(False) for the module the error's path is in (longest directory prefix), else the module of the
        last compiler header.  The first compile error of the run is also a failure."""
        m = self.COMPILE_PATH.match(line)
        artifact = None
        if m:
            path = m.group(1)
            path = os.path.realpath(path)
            matches = [(len(real), a) for a, real in ((a, os.path.realpath(d)) for a, d in self.dirs.items())
                       if path.startswith(real + os.sep)]
            artifact = max(matches)[1] if matches else None
        return self._mark_compile_failed(artifact or self.last_compiler, line)

    def _mark_compile_failed(self, artifact, line):
        """compiled(False) for the artifact once, and `line` as the run's first failure when none was sent yet."""
        events = []
        if artifact is not None:
            sub = f"{self.step_id}/{artifact}"
            if sub not in self.compile_failed:
                self.compile_failed.add(sub)
                events.append(("compiled", sub, False))
        if self.first_error is None:
            self.first_error = line
            events.append(("failure", line, self.step_id))
        return events

    def _compile_goal_failed(self, line):
        """A compiler goal failure names its project even when the error lines carry no source path."""
        return self._mark_compile_failed(self.COMPILE_GOAL_FAILED.match(line).group(1), line)

    def _sub(self, name):
        """Sub-step id `<step>/<artifactId>` for a module's display name; stable for the whole run.

        The artifactId comes from the pom tree, else from the nearest preceding `< g:a >` header, else the name is
        slugged.  A candidate already used by a different module is skipped, so ids stay unique under -T.
        """
        known = self.sub_of.get(name)
        if known is not None:
            return known
        for candidate in (self.names.get(name), self.header_artifact, _slug(name)):
            if not candidate:
                continue
            sub = f"{self.step_id}/{candidate}"
            if self.titles.get(sub, name) == name:
                self.sub_of[name] = sub
                return sub
        sub = f"{self.step_id}/{_slug(name)}-{len(self.sub_of) + 1}"
        self.sub_of[name] = sub
        return sub

    def _artifact_of(self, sub):
        return sub.split("/", 1)[1]

    def _counts(self, sub):
        return self.mod.setdefault(sub, [0, 0, 0, 0])

    def _attribute(self, fqn):
        """The module a test class ran in, from the reactor modules whose test-classes hold it.  A unique hit wins.
        The same FQN can live in several modules: then the last surefire/failsafe header's module when it is among
        them, else the first hit.  With no hit, the last header's module."""
        if fqn not in self.class_cache:
            package, _, simple = fqn.rpartition(".")
            rel = Path(*package.split(".")) / f"{simple}.class" if package else Path(f"{simple}.class")
            self.class_cache[fqn] = tuple(a for a, d in self.dirs.items()
                                          if (d / "target" / "test-classes" / rel).is_file())
        hits = self.class_cache[fqn]
        if len(hits) == 1:
            return f"{self.step_id}/{hits[0]}"
        if hits and self.surefire_sub not in {f"{self.step_id}/{a}" for a in hits}:
            return f"{self.step_id}/{hits[0]}"
        return self.surefire_sub

    def _xml_totals(self, directory):
        """Counts from TEST-*.xml written during this step (mtime at or after its start, less the slack), else None."""
        total = fail = err = skip = 0
        found = False
        cutoff = self.started - self.STALE_SLACK_SECONDS
        for f in sorted(Path(directory).glob("TEST-*.xml")):
            try:
                if f.stat().st_mtime < cutoff:
                    continue
                root = ET.parse(f).getroot()
            except (OSError, ET.ParseError):
                continue
            found = True
            total += int(root.get("tests", 0))
            fail += int(root.get("failures", 0))
            err += int(root.get("errors", 0))
            skip += int(root.get("skipped", 0))
        return [total, fail, err, skip] if found else None

    def _end_module(self, sub, status, seconds):
        """Close a module: exact per-module counts and reports from its report directories, then the sub_end."""
        events = []
        if sub in self.exec_pending:
            self.exec_pending.remove(sub)   # the module ended without a summary, so it is owed none
        artifact = self._artifact_of(sub)
        if self.phase.get(artifact) == "surefire":
            self.phase[artifact] = "tested"
            events.append(("tested", sub))
        name = self.titles.get(sub, self._artifact_of(sub))
        module_dir = self.dirs.get(self._artifact_of(sub))
        if status == "skip":
            pass
        elif module_dir is None:
            events.append(("note", "info", f"Test reports for module {name} were not located", sub))
        else:
            exact = None
            for dirname in ("surefire-reports", "failsafe-reports"):
                reports = module_dir / "target" / dirname
                if reports.is_dir():
                    found = self._xml_totals(reports)
                    if found is not None:
                        events.append(("report", sub, "surefire", str(reports)))
                        exact = found if exact is None else [a + b for a, b in zip(exact, found)]
            if exact is not None:
                self.mod[sub] = exact
        counts = self.mod.get(sub)
        if counts is not None:
            events.append(("tests", sub, *counts))
        events.append(("sub_end", sub, status, int(seconds * 1000)))
        glyph = {"ok": "✓", "fail": "✗", "skip": "–"}[status]
        line = f"{glyph} {name}  {_fmt_seconds(seconds)}"
        if counts is not None and counts[0]:
            line += f"  {counts[0]} tests"
            if counts[1] + counts[2]:
                line += f", {counts[1] + counts[2]} failed"
        events.append(("cond", line))
        self.ended.add(sub)
        self.open.pop(sub, None)
        return events

    def handle(self, line):
        self.generic.handle(line)
        events = []
        if not self.reactor_sent:
            self.reactor_sent = True
            if self.dirs:
                events.append(self._reactor(len(self.dirs)))
        if self.ORDER_HEAD.match(line):
            self.in_order, self.order_members = True, []
            return events
        if self.in_order:
            m = self.ORDER_ROW.match(line)
            if m:
                self.order_members.append((m.group(1), m.group(2)))
                return events
            if self.order_members and line.rstrip() == "[INFO]":
                self.in_order = False
                # a pom title absent from the pom tree stays in the module count but cannot be mapped to a sub id
                # only the poms built before the first real module are Setup poms; one built after it (an aggregator
                # root, last in the order) is on neither the Setup row nor the module bar
                first_real = next((i for i, (_, pkg) in enumerate(self.order_members) if pkg != "pom"),
                                  len(self.order_members))
                poms = [(i < first_real, f"{self.step_id}/{self.names[t]}")
                        for i, (t, pkg) in enumerate(self.order_members) if pkg == "pom" and t in self.names]
                self.order_poms = list(dict.fromkeys(i for lead, i in poms if lead))
                self.order_tail = list(dict.fromkeys(i for lead, i in poms if not lead))
                events.append(self._reactor(len(self.order_members)))
                return events
            if not self.order_members and line.rstrip() == "[INFO]":
                return events
            self.in_order = False
        m = self.HEADER.match(line)
        if m:
            self.header_artifact = m.group(2)
            return events
        m = self.BUILDING.match(line)
        if m:
            self.structure_seen = True
            if not self.building_seen:
                self.building_seen = True
                events.append(self._reactor(int(m.group(4))))
            name, n = m.group(1), int(m.group(3))
            sub = self._sub(name)
            if not self.parallel:
                for prev in list(self.open):
                    events += self._end_module(prev, "ok", time.monotonic() - self.open[prev][1])
            self.titles[sub] = name
            self.open[sub] = (name, time.monotonic())
            events.append(("sub_start", sub, n, name))
            return events
        if self.SUMMARY_HEAD.match(line):
            self.in_summary = True
            self.structure_seen = True
            self.header_artifact = None
            return events
        if self.in_summary:
            row = self.SUMMARY_ROW.match(line)
            if row:
                name, status, took = row.group(1).strip(), self.STATUS[row.group(2)], row.group(3)
                sub = self._sub(name)
                self.summary_index += 1
                if sub in self.ended:
                    return events
                if sub not in self.open:
                    self.titles[sub] = name
                    events.append(("sub_start", sub, self.summary_index, name))
                    self.open[sub] = (name, time.monotonic())
                if status == "fail":
                    self.failed_modules.append(name)
                events += self._end_module(sub, status, _seconds(took) if took else 0.0)
                return events
            if line.startswith("[INFO] ---"):
                if self.summary_index:
                    self.in_summary = False
                return events
            return events
        m = self.ANY_PLUGIN.match(line)
        if m:
            plugin = re.sub(r"^maven-(.+)-plugin$", r"\1", m.group(1))
            events += self._plugin_header(plugin, m.group(3), m.group(4))
            if plugin in ("surefire", "failsafe"):
                self.surefire_sub = f"{self.step_id}/{m.group(4)}"
            if plugin in self.EXEC_GOALS and m.group(3) == "exec":
                sub = f"{self.step_id}/{m.group(4)}"
                if sub not in self.exec_pending:
                    self.exec_pending.append(sub)   # kept across other modules' headers: -T interleaves them before the summary
            self.unittest_ran = None
            return events
        if self.exec_pending:
            events += self._unittest_line(line)
            if events:
                return events
        m = self.CLASS_LINE.search(line)
        if m:
            t, f, e, s = (int(m.group(i)) for i in range(1, 5))
            self.total += t
            self.fail += f
            self.err += e
            self.skip += s
            events.append(("tests", self.step_id, self.total, self.fail, self.err, self.skip))
            sub = self._attribute(m.group(5))
            if sub is not None:
                secs = float(self.CLASS_TIME.search(line).group(1).replace(",", ""))
                self.surefire_s[sub] = self.surefire_s.get(sub, 0.0) + secs
                events.append(("series", self.step_id, "surefire_s", sub, round(self.surefire_s[sub], 3)))
                c = self._counts(sub)
                c[0] += t
                c[1] += f
                c[2] += e
                c[3] += s
                if sub in self.open:
                    events.append(("tests", sub, *c))
                events.append(("class_done", sub, "fail" if f + e else "skip" if t and s == t else "pass"))
            return events
        if self.FAILED_TEST.match(line):
            if not self.failure_trace:
                self.failure_trace = [line]
                self.trace_left = self.FAILURE_TRACE_LINES
            events.append(("cond", line))
            self.cond_window = self.FAILURE_TRACE_LINES
            return events
        if self.COMPILE_ERROR.match(line):
            events += self._compile_error(line)
        elif self.COMPILE_GOAL_FAILED.match(line):
            events += self._compile_goal_failed(line)
        if self.cond_window > 0:
            self.cond_window -= 1
            events.append(("cond", line))
            if self.trace_left > 0 and self.failure_trace:
                self.failure_trace.append(line)
                self.trace_left -= 1
            return events
        if line.startswith("[ERROR]") and line.strip() != "[ERROR]" and "Tests run:" not in line:
            events.append(("cond", line))
        elif line.startswith(("[INFO] BUILD ", "[INFO] Total time:")):
            events.append(("cond", line))
            if line.startswith("[INFO] BUILD FAILURE"):
                events += self._failure_note()
        return events

    def _unittest_line(self, line):
        """Credits a unittest summary (`Ran N tests in Xs`, then `OK` or `FAILED (...)`) to the exec module."""
        m = self.UNITTEST_RAN.search(line)
        if m:
            self.unittest_ran = (int(m.group(1)), float(m.group(2)))
            return []
        if self.unittest_ran is None or not line.strip():
            return []
        tests, secs = self.unittest_ran
        self.unittest_ran = None
        m = self.UNITTEST_RESULT.search(line)
        if not m:
            return []
        found = {k: int(v) for k, v in self.UNITTEST_COUNT.findall(m.group(1) or m.group(2) or "")}
        f, e, s = found.get("failures", 0), found.get("errors", 0), found.get("skipped", 0)
        self.total += tests
        self.fail += f
        self.err += e
        self.skip += s
        sub = self.exec_pending.pop(0)
        self.surefire_s[sub] = self.surefire_s.get(sub, 0.0) + secs
        c = self._counts(sub)
        c[0] += tests
        c[1] += f
        c[2] += e
        c[3] += s
        events = [("tests", self.step_id, self.total, self.fail, self.err, self.skip),
                  ("series", self.step_id, "surefire_s", sub, round(self.surefire_s[sub], 3))]
        if sub in self.open:
            events.append(("tests", sub, *c))
        events.append(("class_done", sub, "fail" if f + e else "skip" if tests and s == tests else "pass"))
        return events

    def _failure_note(self):
        parts = []
        if self.failed_modules:
            parts.append("Module failed: " + ", ".join(self.failed_modules))
        if self.failure_trace:
            parts.append("\n".join(self.failure_trace))
        elif self.first_error:
            parts.append(self.first_error)
        text = "\n".join(parts) if parts else "Build failed"
        events = [("note", "error", text, self.step_id)]
        if self.failure_trace:
            events.append(("failure", "\n".join(self.failure_trace), self.step_id))
        return events

    def finish(self, exit_code):
        events = []
        for sub in list(self.open):   # a module whose Reactor Summary row never arrived still gets its end
            events += self._end_module(sub, "skip", time.monotonic() - self.open[sub][1])
        if self.total:
            events.append(("tests", self.step_id, self.total, self.fail, self.err, self.skip))
        if not self.structure_seen and exit_code != 0:
            self.generic.step_id = self.step_id
            events += self.generic.finish(exit_code)
        return events


def _engine_dir():
    return Path(__file__).resolve().parent


def find_pytest_plugin():
    """The plugin-only directory to put on the child's PYTHONPATH so `-p juneau_run_pytest` imports, or None (spec 1.4).
    It holds nothing but the plugin module, so the suite under test never sees bootstrap, juneau_run or juneau_scripts.
    In a zip it is the `pytest_plugin` subdirectory of the archive (zipimport accepts `x.zip/pytest_plugin`)."""
    if (_engine_dir() / "pytest_plugin" / "juneau_run_pytest.py").is_file():
        return str(_engine_dir() / "pytest_plugin")
    scripts = sys.modules.get("juneau_scripts")
    path = getattr(scripts, "__file__", None) or ""
    marker = ".zip" + os.sep
    if marker in path:
        return path[:path.index(marker) + len(".zip")] + os.sep + "pytest_plugin"
    return None


_RUNNERS = ("uv", "poetry", "pipenv", "pdm", "hatch", "rye")
# Launcher flags that take a separate value; their value (e.g. `--with pytest`) is not the program being run.
_RUNNER_VALUE_FLAGS = {"--with", "--with-requirements", "--with-editable", "--python", "-p", "--project", "--directory",
                       "--package", "--extra", "--group", "--env-file", "--from", "--index", "--index-url", "-w", "-e"}


def _is_pytest_name(arg):
    return os.path.basename(arg) in ("pytest", "py.test")


def _pytest_index(cmd):
    """Index of the token that launches pytest (pytest, py.test, or the `pytest` after `-m`), or -1.  Only the program
    position counts: cmd[0], the program after a launcher's `run` (past its flags and their values, or after `--`), or
    the module after `-m` -- so `tox -e pytest`, `make pytest` and `uv run --with pytest python -m foo` do not match.
    Launchers in front of the program (env and its options and NAME=value pairs, npx) are looked through."""
    if not cmd:
        return -1
    lead = _program_index(cmd)
    if lead:
        found = _pytest_index(cmd[lead:])
        return found + lead if found >= 0 else -1
    if _is_pytest_name(cmd[0]):
        return 0
    for i in range(len(cmd) - 1):
        if cmd[i] == "-m":
            return i + 1 if cmd[i + 1] == "pytest" else -1
    if os.path.basename(cmd[0]) in _RUNNERS and "run" in cmd[1:]:
        i = cmd.index("run", 1) + 1
        while i < len(cmd):
            a = cmd[i]
            if a == "--":
                i += 1
                break
            if not a.startswith("-"):
                break
            i += 2 if a in _RUNNER_VALUE_FLAGS else 1
        if i < len(cmd) and _is_pytest_name(cmd[i]):
            return i
    return -1


def _is_pytest_command(cmd):
    return _pytest_index(cmd) >= 0


class PytestParser(Parser):
    """pytest: junitxml report plus counts from the final summary line."""

    name = "pytest"
    SUMMARY = re.compile(r"^(?:=+ )?((?:\d+ [a-z]+(?:, )?)+) in [\d.]+s(?: \(\d+:\d+:\d+\))?(?: =+)?\s*$")
    ITEM = re.compile(r"(\d+) (passed|failed|skipped|errors?|xfailed|xpassed|deselected|warnings?)")
    SHORT = re.compile(r"^=+ short test summary info =+\s*$")
    # "test_f.py ..F.s  [ 43%]" or "[60/70]"; a wrapped line has no file name, and xdist never has one.
    PROGRESS = re.compile(r"^(?:((?![.FEsxX]+ )\S+) +)?([.FEsxX]+) +\[ *(?:(\d+%)|(\d+/\d+))\] *$")
    # Fallback (plugin never said hello): collected count, xdist worker line, verbose result lines, [k/N] counts.
    COLLECTED = re.compile(r"^collected (\d+) items?")
    WORKERS = re.compile(r"workers \[(\d+) items?\]")
    VERBOSE = re.compile(r"^(?:\[gw\d+\] \[\s*\d+%\] (PASSED|FAILED|ERROR|SKIPPED|XFAIL|XPASS) (\S+::\S+)"
                         r"|(\S+::\S+) (PASSED|FAILED|ERROR|SKIPPED|XFAIL|XPASS))")
    COUNT = re.compile(r"\[ *(\d+)/(\d+)\]\s*$")
    BARE_PERCENT = re.compile(r"^\s*\[ *(\d+)%\]\s*$")
    OUTCOME = {"passed": "pass", "xpassed": "pass", "failed": "fail", "error": "fail", "skipped": "skip", "xfailed": "skip",
               "PASSED": "pass", "XPASS": "pass", "FAILED": "fail", "ERROR": "fail", "SKIPPED": "skip", "XFAIL": "skip"}
    INDEX_LIMIT = 2000

    def __init__(self):
        super().__init__()
        self.report_path = None
        self.in_short = False
        self.counts = None
        self.last_file = None
        self.last_progress = None
        self.plugin = False
        self.done = 0
        self.n = 0
        self.failed = self.errors = self.skipped = 0
        self.index = OrderedDict()   # node id -> unit index, bounded: a late marker follows its test at once
        # File grain (plugin total marker with per-file test counts): one bar unit per test file, finished with its last test
        self.files = None            # file -> tests it holds; None until a total marker carries them
        self.file_seen = {}          # file -> tests finished so far
        self.file_state = {}         # file -> [any failed, all skipped]
        self.file_cell = {}          # finished file -> its bar index
        self.files_done = 0

    def prepare(self, cmd, env, cwd):
        existing = None
        for i, a in enumerate(cmd):
            for flag in ("--junitxml", "--junit-xml"):
                if a == flag and i + 1 < len(cmd):
                    existing = cmd[i + 1]
                elif a.startswith(flag + "="):
                    existing = a.split("=", 1)[1]
        self.report_path = existing
        extra = []
        at = _pytest_index(cmd) + 1
        if (os.environ.get("JUNEAU_RUN_PYTEST_PLUGIN") != "0" and at > 0
                and "no:juneau_run_pytest" not in cmd and "-pno:juneau_run_pytest" not in cmd):
            where = find_pytest_plugin()
            if where:
                extra += ["-p", "juneau_run_pytest"]
                env = dict(os.environ if env is None else env)
                old = env.get("PYTHONPATH")
                env["PYTHONPATH"] = where + (os.pathsep + old if old else "")
        if existing is None and enabled():
            if at == 0:         # not recognisably pytest (a wrapper): keep appending, before any `--`
                at = cmd.index("--") if "--" in cmd else len(cmd)
            directory = artifacts_dir() or Path(tempfile.mkdtemp(prefix="juneau-run-"))
            self.report_path = str(directory / f"{self.step_id.replace('/', '-')}-pytest.xml")
            extra.append(f"--junitxml={self.report_path}")
        if extra:
            cmd = list(cmd)
            cmd[at:at] = extra
        return cmd, env

    def _tests_event(self):
        return ("tests", self.step_id, self.done, self.failed, self.errors, self.skipped)

    def _plugin_event(self, line):
        try:
            m = json.loads(line[len("##pytest "):])
        except ValueError:
            return []
        if not isinstance(m, dict):
            return []
        kind = m.get("ev")
        if kind == "hello":
            self.plugin = True
            return []
        if kind == "run":
            name = m.get("f")
            return [("progress", self.step_id, name[:400])] if isinstance(name, str) and name else []
        if kind == "total":
            n = m.get("n")
            if isinstance(n, int) and n >= 0:
                self.n = n
                files = m.get("f")
                if isinstance(files, dict) and files and all(isinstance(v, int) for v in files.values()):
                    changed = self.files is None or len(files) != len(self.files)
                    self.files = files
                    return [("units", self.step_id, len(files), "files")] if changed else []
                if self.files is not None:
                    return []
                return [("units", self.step_id, n, "tests")]
            return []
        if kind != "test":
            return []
        outcome = m.get("o")
        unit = self.OUTCOME.get(outcome)
        if unit is None:
            return []
        if m.get("late"):
            self.errors += 1
            events = []
            fname = self._file_of(m.get("id"))
            if self.files is not None:
                if fname in self.file_cell:
                    events.append(("unit_fix", self.step_id, self.file_cell[fname], "fail"))
                elif fname in self.files:
                    self._file_note(fname, "error")
                idx = None
            else:
                idx = self.index.get(m.get("id"))
            if idx is not None:
                events.append(("unit_fix", self.step_id, idx, "fail"))
            events.append(self._tests_event())
            return events
        self.done += 1
        self.index[m.get("id")] = self.done - 1
        self.index.move_to_end(m.get("id"))
        if len(self.index) > self.INDEX_LIMIT:
            self.index.popitem(last=False)
        if outcome == "failed":
            self.failed += 1
        elif outcome == "error":
            self.errors += 1
        elif unit == "skip":
            self.skipped += 1
        if self.files is not None:
            events = [self._tests_event()]
            fname = self._file_of(m.get("id"))
            if fname in self.files:
                self._file_note(fname, outcome)
                self.file_seen[fname] = self.file_seen.get(fname, 0) + 1
                if self.file_seen[fname] >= self.files[fname] and fname not in self.file_cell:
                    state = self.file_state[fname]
                    self.file_cell[fname] = self.files_done
                    self.files_done += 1
                    events.insert(0, ("unit", self.step_id, self.files_done,
                                      "fail" if state[0] else "skip" if state[1] else "pass"))
            return events
        return [("unit", self.step_id, self.done, unit), self._tests_event()]

    @staticmethod
    def _file_of(node_id):
        return str(node_id).split("::", 1)[0][:400]

    def _file_note(self, fname, outcome):
        state = self.file_state.setdefault(fname, [False, True])
        if outcome in ("failed", "error"):
            state[0] = True
        if self.OUTCOME.get(outcome) != "skip":
            state[1] = False

    def _set_n(self, n):
        if n == self.n:
            return []
        self.n = n
        return [("units", self.step_id, n, "tests")]

    def _advance(self, done, outcome):
        if done <= self.done:
            return []
        self.done = done
        return [("unit", self.step_id, done, outcome)]

    def handle(self, line):
        if line.startswith("##pytest "):
            return self._plugin_event(line)
        events = []
        if self.plugin:
            return self._summary_only(line)       # scraped progress ignored once the plugin said hello
        m = self.COLLECTED.match(line)
        if m:
            return self._set_n(int(m.group(1)))
        m = self.WORKERS.search(line)
        if m:
            return self._set_n(int(m.group(1)))
        m = None if self.in_short else self.VERBOSE.match(line)
        if m:
            word = m.group(1) or m.group(4)
            return self._advance(self.done + 1, self.OUTCOME[word])
        m = self.BARE_PERCENT.match(line)
        if m:
            if self.n:
                return self._advance(round(int(m.group(1)) * self.n / 100), None)
            return events
        progress = self.PROGRESS.match(line) if line.endswith("]") else None
        if progress:
            self.last_file = progress.group(1) or self.last_file
            text = progress.group(3) or progress.group(4)
            if self.last_file:
                text += f" · {self.last_file}"
            if text != self.last_progress:
                self.last_progress = text
                events.append(("progress", self.step_id, text))
            count = self.COUNT.search(line)
            if count:
                events += self._set_n(int(count.group(2)))
                events += self._advance(int(count.group(1)), None)
            else:
                events += self._advance(self.done + len(progress.group(2)), None)
            return events
        return self._summary_only(line)

    def _summary_only(self, line):
        events = []
        if self.SHORT.match(line):
            self.in_short = True
            events.append(("cond", line))
            return events
        m = self.SUMMARY.match(line)
        if m:
            c = {}
            for count, word in self.ITEM.findall(m.group(1)):
                c[word.rstrip("s") if word.startswith("error") else word] = int(count)
            total = sum(v for k, v in c.items() if k in ("passed", "failed", "skipped", "error", "xfailed", "xpassed"))
            self.counts = (total, c.get("failed", 0), c.get("error", 0), c.get("skipped", 0) + c.get("xfailed", 0))
            events.append(("tests", self.step_id, *self.counts))
            events.append(("cond", line))
            self.in_short = False
            return events
        if self.in_short or line.startswith(("FAILED ", "ERROR ")):
            events.append(("cond", line))
        return events

    def finish(self, exit_code):
        if self.report_path and os.path.exists(self.report_path):
            return [("report", self.step_id, "junitxml", self.report_path)]
        return []


class PlaywrightParser(Parser):
    """Playwright line reporter: JSON report plus counts from progress and the final summary."""

    name = "playwright"
    PROGRESS = re.compile(r"^\[(\d+)/(\d+)\] ")
    FAILURE = re.compile(r"^\s+(\d+)\) ")
    SUMMARY = re.compile(r"^\s+(\d+) (passed|failed|skipped|flaky|interrupted|did not run)\b")
    BLOCK_LINES = 30

    def __init__(self):
        super().__init__()
        self.report_path = None
        self.run_count = 0
        self.total = 0
        self.failures = 0
        self.block_left = 0
        self.final = {}

    def prepare(self, cmd, env, cwd):
        env = dict(env if env is not None else os.environ)
        if enabled():
            if not any(a.startswith("--reporter") for a in cmd):
                cmd = list(cmd) + ["--reporter=line,json"]
            directory = artifacts_dir()
            if "PLAYWRIGHT_JSON_OUTPUT_NAME" not in env and directory is not None:
                env["PLAYWRIGHT_JSON_OUTPUT_NAME"] = str(directory / f"{self.step_id.replace('/', '-')}-playwright.json")
        self.report_path = env.get("PLAYWRIGHT_JSON_OUTPUT_NAME")
        return cmd, env

    def handle(self, line):
        events = []
        m = self.PROGRESS.match(line)
        if m:
            self.run_count = int(m.group(1))
            total = int(m.group(2))
            if total != self.total:
                self.total = total
                events.append(("units", self.step_id, total, "tests"))
            events.append(("unit", self.step_id, self.run_count, None))
            events.append(("tests", self.step_id, self.run_count, self.failures, 0, 0))
            return events
        m = self.SUMMARY.match(line)
        if m:
            self.final[m.group(2)] = int(m.group(1))
            self.block_left = 0
            events.append(("cond", line))
            total = sum(v for k, v in self.final.items() if k != "did not run")
            events.append(("tests", self.step_id, total, self.final.get("failed", 0), 0, self.final.get("skipped", 0)))
            return events
        if self.FAILURE.match(line):
            self.failures += 1
            self.block_left = self.BLOCK_LINES
            events.append(("cond", line))
            return events
        if self.block_left > 0:
            self.block_left -= 1
            events.append(("cond", line))
        return events

    def finish(self, exit_code):
        if self.report_path and os.path.exists(self.report_path):
            return [("report", self.step_id, "playwright-json", self.report_path)]
        return []


class GradleParser(Parser):
    """Gradle with --console=plain: a sub-step per task, counts from the test summary, and the "What went wrong"
    block as the failure."""

    name = "gradle"
    TASK = re.compile(r"^> Task (:\S+)(?:\s+(UP-TO-DATE|NO-SOURCE|SKIPPED|FROM-CACHE|FAILED))?\s*$")
    COUNTS = re.compile(r"^(\d+) tests? completed(?:, (\d+) failed)?(?:, (\d+) skipped)?\s*$")
    WRONG = "* What went wrong:"
    OUTCOMES = {"UP-TO-DATE": "ok", "FROM-CACHE": "ok", "NO-SOURCE": "skip", "SKIPPED": "skip", "FAILED": "fail"}
    TAIL = 40

    def __init__(self):
        super().__init__()
        self.tasks = 0
        self.open = None        # (sub id, monotonic start) of the running task
        self.block = None       # lines of the "What went wrong" block while it's being read
        self.blocks = []
        self.tail = collections.deque(maxlen=self.TAIL)

    def prepare(self, cmd, env, cwd):
        at = _program_index(cmd) + 1
        if not any(a.startswith("--console") for a in cmd[at:]):
            cmd = [*cmd[:at], "--console=plain", *cmd[at:]]
        return cmd, env

    def _close(self, status):
        sub, t0 = self.open
        self.open = None
        return [("sub_end", sub, status, int((time.monotonic() - t0) * 1000))]

    def handle(self, line):
        self.tail.append(line)
        if self.block is not None:
            if line.startswith("* ") or line.startswith("BUILD "):
                self.blocks.append("\n".join(self.block).rstrip())
                self.block = None
            else:
                self.block.append(line)
                return [("cond", line)] if line.strip() else []
        if line.strip() == self.WRONG:
            self.block = [line]
            return [("cond", line)]
        m = self.TASK.match(line)
        if m:
            path, outcome = m.group(1), m.group(2)
            sub = f"{self.step_id}/{_slug(path)}"
            events = []
            if self.open is not None and self.open[0] == sub:
                return self._close(self.OUTCOMES.get(outcome, "ok"))
            if self.open is not None:
                events += self._close("ok")
            self.tasks += 1
            events.append(("sub_start", sub, self.tasks, path))
            self.open = (sub, time.monotonic())
            if outcome is not None:
                events += self._close(self.OUTCOMES[outcome])
            return events
        m = self.COUNTS.match(line)
        if m:
            total, failed, skipped = int(m.group(1)), int(m.group(2) or 0), int(m.group(3) or 0)
            return [("tests", self.step_id, total, failed, 0, skipped), ("cond", line)]
        if line.startswith("BUILD FAILED"):
            return [("cond", line)]
        return []

    def finish(self, exit_code):
        events = self._close("ok" if exit_code == 0 else "fail") if self.open is not None else []
        if self.block is not None:
            self.blocks.append("\n".join(self.block).rstrip())
        if exit_code == 0:
            return events
        if self.blocks:
            return events + [("failure", "\n\n".join(self.blocks), self.step_id)]
        if self.tail:
            return events + [("failure", "\n".join(self.tail), self.step_id)]
        return events


class NpmParser(GenericParser):
    """npm: no sub-steps.  "npm error"/"npm ERR!" lines are the failure (else the last 40 lines, as GenericParser),
    and "added N packages" is the summary line."""

    name = "npm"
    ERROR = re.compile(r"^npm (?:error|ERR!)")
    ADDED = re.compile(r"^added \d+ packages?\b")

    def __init__(self):
        super().__init__()
        self.errors = []

    def handle(self, line):
        super().handle(line)
        if self.ERROR.match(line):
            self.errors.append(line)
            return [("cond", line)]
        if self.ADDED.match(line):
            return [("cond", line)]
        return []

    def finish(self, exit_code):
        if exit_code != 0 and self.errors:
            return [("failure", "\n".join(self.errors), self.step_id)]
        return super().finish(exit_code)


PARSERS = {
    "maven": MavenParser,
    "pytest": PytestParser,
    "playwright": PlaywrightParser,
    "gradle": GradleParser,
    "npm": NpmParser,
    "generic": GenericParser,
}


# ---------------------------------------------------------------------------------------------------------------------
# Runner
# ---------------------------------------------------------------------------------------------------------------------


class Result:
    """Outcome of run_tool().  summary is the step's last tests totals; output is set only with capture=True."""

    def __init__(self, exit, ms, summary=None, output=None):
        self.exit = exit
        self.ms = ms
        self.summary = summary or {}
        self.output = output


def _group_alive(pgid):
    try:
        os.killpg(pgid, 0)
        return True
    except ProcessLookupError:
        return False
    except PermissionError:
        return True


def _terminate_group(proc, grace=None):
    """SIGTERM the child's process group, wait up to `grace` seconds, then SIGKILL whatever is left and reap."""
    grace = GRACE_SECONDS if grace is None else grace
    pgid = proc.pid
    try:
        os.killpg(pgid, signal.SIGTERM)
    except ProcessLookupError:
        pass
    deadline = time.monotonic() + grace
    while time.monotonic() < deadline:
        proc.poll()
        if proc.returncode is not None and not _group_alive(pgid):
            return
        time.sleep(0.05)
    try:
        os.killpg(pgid, signal.SIGKILL)
    except ProcessLookupError:
        pass
    proc.wait()


class _Isolated:
    """Calls parser methods, disabling the parser after its first exception.  A method the parser does not have (a
    parser written before poll, close and render existed) is a no-op.  close() runs once, even on a disabled parser,
    so its temp files go."""

    def __init__(self, parser):
        self.parser = parser
        self.disabled = False
        self.closed = False

    def _disable(self, e):
        self.disabled = True
        warn(f"parser {getattr(self.parser, 'name', '?')} disabled after error: {e!r}")

    def call(self, method, *args):
        if self.disabled:
            return []
        fn = getattr(self.parser, method, None)
        if fn is None:
            return []
        try:
            return fn(*args) or []
        except Exception as e:  # parsers are third-party code; the tool's output must keep flowing
            self._disable(e)
            return []

    def render(self, line):
        """The parser's text for the full log, or the line itself if the parser is disabled, fails or returns no str."""
        if self.disabled:
            return line
        fn = getattr(self.parser, "render", None)
        if fn is None:
            return line
        try:
            text = fn(line)
        except Exception as e:
            self._disable(e)
            return line
        return text if isinstance(text, str) else line

    def close(self):
        if self.closed:
            return
        self.closed = True
        fn = getattr(self.parser, "close", None)
        if fn is None:
            return
        try:
            fn()
        except Exception as e:
            warn(f"parser {getattr(self.parser, 'name', '?')} close failed: {e!r}")


class _Dispatcher:
    """Turns parser events into markers and condensed lines for one step."""

    def __init__(self, step_id, sinks, *, cwd=None, reports=True):
        self.step_id = step_id
        self.sinks = sinks
        self.cwd = cwd             # where a parser's relative report paths are taken from
        self.reports = reports     # False drops report events
        self.open = collections.OrderedDict()   # sub id -> start time
        self.pending = {}                        # tests target -> latest totals not yet emitted
        self.last_sent = {}
        self.summary = {}
        self.latest = {}   # tests target -> its latest totals, for module_end

    def apply(self, events):
        for ev in events:
            kind = ev[0]
            if kind == "sub_start":
                _, sub, n, title = ev
                self.open[sub] = time.monotonic()
                emit("step", id=sub, n=n, title=title, parent=self.step_id)
                publish("module_start", step=self.step_id, module=sub, title=title)
            elif kind == "sub_end":
                _, sub, status, ms = ev
                self.flush_tests(sub)
                self.open.pop(sub, None)
                emit("end", id=sub, status=status, ms=ms)
                publish("module_end", step=self.step_id, module=sub, status=status, ms=ms, totals=self.latest.get(sub))
            elif kind == "tests":
                _, target, total, fail, err, skip = ev
                if target == self.step_id:
                    self.summary = {"total": total, "fail": fail, "err": err, "skip": skip}
                self.pending[target] = self.latest[target] = (total, fail, err, skip)
                if time.monotonic() - self.last_sent.get(target, -1e9) >= TESTS_INTERVAL:
                    self.flush_tests(target)
            elif kind == "report":
                _, target, rkind, path = ev
                if self.reports:
                    report(target, rkind, path, cwd=self.cwd)
            elif kind == "note":
                _, level, text, target = ev
                note(level, text, step=target)
            elif kind == "class_done":
                publish("class_done", step=self.step_id, module=ev[1], outcome=ev[2])
            elif kind == "compiled":
                publish("module_compiled", step=self.step_id, module=ev[1], ok=ev[2])
            elif kind == "tested":
                publish("module_tested", step=self.step_id, module=ev[1])
            elif kind == "reactor":
                publish("reactor", step=self.step_id, size=ev[1], poms=list(ev[2]), tests=ev[3],
                        **({"tail": list(ev[4])} if len(ev) > 4 else {}))   # `tail` only when there is one
            elif kind == "failure":
                publish("failure", text=ev[1], step=ev[2])
            elif kind == "progress":
                publish("step_progress", step=ev[1], text=ev[2])   # bus only: never a marker, so protocol v1 is unchanged
            elif kind == "units":
                publish("units", step=ev[1], total=ev[2], kind=ev[3])
            elif kind == "unit":
                publish("unit", step=ev[1], done=ev[2], outcome=ev[3])
            elif kind == "unit_fix":
                publish("unit_fix", step=ev[1], index=ev[2], outcome=ev[3])
            elif kind == "series":
                publish("series", step=ev[1], name=ev[2], sub=ev[3], value=ev[4])
            elif kind == "cond":
                self.sinks.write_condensed(ev[1])

    def flush_tests(self, target):
        totals = self.pending.pop(target, None)
        if totals is not None:
            self.last_sent[target] = time.monotonic()
            tests(target, *totals)

    def close_children(self, status):
        """Producer rule: every open child gets its `end`, children first, before the parent's.  The bus gets the
        matching module_end."""
        for sub in reversed(list(self.open)):
            started = self.open[sub]
            self.flush_tests(sub)
            ms = int((time.monotonic() - started) * 1000)
            emit("end", id=sub, status=status, ms=ms)
            publish("module_end", step=self.step_id, module=sub, status=status, ms=ms, totals=self.latest.get(sub))
        self.open.clear()
        for target in list(self.pending):
            self.flush_tests(target)


_bind_lock = threading.Lock()
_toolkit = None            # juneau_scripts.parsers once bound to this module, or the exception that stopped it
_fallback_noted = set()    # parser names already warned about: one note per name per process


def _toolkit_package():
    """juneau_scripts.parsers, imported but not bound: from the juneau_scripts already imported, else the one on
    sys.path, else the one beside this script (juneau-run-<v>.zip, or a juneau_scripts/ directory; appended to sys.path,
    so a juneau_scripts earlier on it still wins).  A directory source never gets bytecode written beside it
    (sys.dont_write_bytecode, as bootstrap.py does), for the length of the import only."""
    no_bytecode = False
    pkg = sys.modules.get("juneau_scripts")
    if pkg is not None:
        no_bytecode = os.path.isfile(getattr(pkg, "__file__", None) or "")
    else:
        spec = importlib.util.find_spec("juneau_scripts")
        if spec is None:
            here = Path(__file__).resolve()
            zipped = here.with_suffix(".zip")
            if zipped.is_file():
                source = zipped
            elif (here.parent / "juneau_scripts" / "__init__.py").is_file():
                source = here.parent
                no_bytecode = True
            else:
                raise ImportError(f"no juneau_scripts beside {here.name} (looked for {zipped.name} and juneau_scripts/)")
            sys.path.append(str(source))
        elif spec.origin and os.path.isfile(spec.origin):
            no_bytecode = True
    saved = sys.dont_write_bytecode
    sys.dont_write_bytecode = saved or no_bytecode
    try:
        return importlib.import_module("juneau_scripts.parsers")
    finally:
        sys.dont_write_bytecode = saved


@contextmanager
def _no_bytecode_beside_a_directory_toolkit():
    """For the length of a parser module's import: no bytecode written if the imported juneau_scripts is a directory (a
    zip is never written to)."""
    pkg = sys.modules.get("juneau_scripts")
    saved = sys.dont_write_bytecode
    sys.dont_write_bytecode = saved or os.path.isfile(getattr(pkg, "__file__", None) or "")
    try:
        yield
    finally:
        sys.dont_write_bytecode = saved


def _toolkit_parsers():
    """juneau_scripts.parsers, bound to this module on first use.  The outcome, success or failure, is kept."""
    global _toolkit
    with _bind_lock:
        if _toolkit is None:
            try:
                pkg = _toolkit_package()
                pkg.bind(sys.modules[__name__])
                _toolkit = pkg
            except Exception as e:  # any failure means the generic parser, never a broken step
                _toolkit = e
    if isinstance(_toolkit, Exception):
        raise ImportError(str(_toolkit)) from _toolkit
    return _toolkit


class _FallbackParser(GenericParser):
    """The generic parser standing in for one that could not be loaded; its first poll or finish carries the note."""

    def __init__(self, text):
        super().__init__()
        self.text = text

    def _note(self):
        text, self.text = self.text, None
        return [("note", "warn", text, self.step_id)] if text else []

    def poll(self):
        return self._note()

    def finish(self, exit_code):
        return self._note() + super().finish(exit_code)


def _load_external(name):
    """A parser from juneau_scripts.parsers, or the generic parser: with a warn note the first time a name fails, and
    silently after that."""
    try:
        pkg = _toolkit_parsers()
        if not pkg.known(name):
            raise LookupError("not a registered parser")
        with _no_bytecode_beside_a_directory_toolkit():
            return pkg.load(name)()
    except Exception as e:
        reason = str(e) or type(e).__name__
    if name in _fallback_noted:
        return GenericParser()
    _fallback_noted.add(name)
    text = f"parser {name} unavailable: {reason}; using generic"
    warn(text)
    return _FallbackParser(text)


def _resolve_parser(parser):
    if isinstance(parser, str):
        return PARSERS[parser]() if parser in PARSERS else _load_external(parser)
    return parser() if isinstance(parser, type) else parser


def known_parser(name):
    """True if name is a built-in parser or one registered in juneau_scripts.parsers.  Never binds or imports a
    parser module."""
    if name in PARSERS:
        return True
    try:
        return bool(_toolkit_package().known(name))
    except Exception:
        return False


def _parser_name(parser):
    """The parser's `name` (maven, pytest, ...), whether given as a name, a class or an instance."""
    return parser if isinstance(parser, str) else getattr(parser, "name", None)


def _input_fd():
    """The terminal whose keystrokes tty=True forwards to the child, or None when stdin is not a terminal."""
    try:
        fd = sys.stdin.fileno()
    except (AttributeError, ValueError, OSError):
        return None
    return fd if os.isatty(fd) else None


class _Watch:
    """run_tool's watchdog: the trailing partial line, when output last arrived, and what has been published."""

    def __init__(self, step_id, seconds, echo):
        self.step_id = step_id
        self.seconds = seconds
        self.echo = echo                 # echo() is True when prompt text also goes to stdout: nothing else would
                                         # show it.  Asked each time, as the console view can stop mid-step.
        self.last = time.monotonic()
        self.partial = ""
        self.echoing = False
        self.quiet = 0

    def _publish(self, kind, text=None):
        if text is None:
            publish(kind, step=self.step_id)
        else:
            publish(kind, step=self.step_id, text=text)
        if self.echo():
            _out("\n" if kind == "prompt_end" else text)

    def output(self, text):
        """Called with each decoded chunk before it is parsed."""
        self.last = time.monotonic()
        if self.quiet:
            self.quiet = 0
            publish("quiet", step=self.step_id, seconds=0)
        if self.echoing:
            head, newline, _ = text.partition("\n")
            head = strip_ansi(head).replace("\r", "")
            if head:
                self._publish("prompt_echo", head)
            if newline:
                self.echoing = False
                self._publish("prompt_end")
        self.partial = (self.partial + text).rsplit("\n", 1)[-1]

    def idle(self):
        """Called when select() times out."""
        if self.seconds is None or self.echoing:
            return
        silent = time.monotonic() - self.last
        if silent < self.seconds:
            return
        partial = strip_ansi(self.partial.rsplit("\r", 1)[-1])
        if partial.strip():
            self.echoing = True
            self._publish("prompt", partial)
        elif max(1, int(silent)) > self.quiet:
            self.quiet = max(1, int(silent))
            publish("quiet", step=self.step_id, seconds=self.quiet)


def _tool_status(rc, statuses):
    """The status and exit field for a tool's row: statuses[rc] when given, else ok for 0 and fail otherwise.
    Exit 127 (cannot start) and 130 (interrupted) always fail, whatever statuses says."""
    if rc in (127, 130):
        return "fail", rc
    status = (statuses or {}).get(rc) or ("ok" if rc == 0 else "fail")
    return status, (rc if status == "fail" else None)


POLL_INTERVAL = 0.1     # seconds between a parser's poll() calls in run_tool


def _feed_stdin(proc, data):
    """Write data to proc's stdin from a thread (so a full pipe cannot block the output loop), then close it."""
    try:
        proc.stdin.write(data)
    except OSError:   # the child closed its stdin early
        pass
    finally:
        try:
            proc.stdin.close()
        except OSError:
            pass


# A process group of the child's own; preexec_fn before 3.11, which has no process_group.
_OWN_GROUP = {"process_group": 0} if sys.version_info >= (3, 11) else {"preexec_fn": os.setpgrp}


def _foreground_tty():
    """0 when stdin is our controlling terminal and our process group holds its foreground, else None."""
    try:
        return 0 if os.isatty(0) and os.tcgetpgrp(0) == os.getpgrp() else None
    except OSError:
        return None


def _foreground_of(fd):
    """The process group holding the terminal fd's foreground, or None once the terminal has gone."""
    try:
        return os.tcgetpgrp(fd)
    except OSError:
        return None


def _set_foreground(fd, pgid):
    """Give the terminal fd's foreground to pgid, with SIGTTOU blocked so our group, once in the background, can take
    it back without being stopped.  A terminal that has gone is left as it is."""
    mask = signal.pthread_sigmask(signal.SIG_BLOCK, {signal.SIGTTOU})
    try:
        os.tcsetpgrp(fd, pgid)
    except OSError:
        pass
    finally:
        signal.pthread_sigmask(signal.SIG_SETMASK, mask)


def _wait_in_foreground(proc, fd):
    """Wait for a child whose group holds the terminal fd and return its exit code.  A stop for touching the terminal
    while it holds the foreground (it read before the handoff) only resumes it.  Any other stop (^Z) is passed on:
    the terminal comes back to our group, which stops too (a stop an orphaned group discards); resumed, the child
    gets the terminal again if we hold it, and is resumed either way."""
    while True:
        _, status = os.waitpid(proc.pid, os.WUNTRACED)
        if not os.WIFSTOPPED(status):
            proc.returncode = os.waitstatus_to_exitcode(status)
            return proc.returncode
        if os.WSTOPSIG(status) not in (signal.SIGTTIN, signal.SIGTTOU) or _foreground_of(fd) != proc.pid:
            _set_foreground(fd, os.getpgrp())
            os.killpg(os.getpgrp(), signal.SIGTSTP)
            if _foreground_of(fd) == os.getpgrp():
                _set_foreground(fd, proc.pid)
        os.killpg(proc.pid, signal.SIGCONT)


def _run_in_foreground(cmd, cwd, env, input):
    """Pass-through run_tool's child (see run_tool) and its exit code.  SIGINT and SIGTERM unwind as a
    KeyboardInterrupt once the child's group is taken down."""
    fd = _foreground_tty()

    def on_signal(signum, frame):
        raise KeyboardInterrupt

    previous = {}
    try:
        for sig in (signal.SIGINT, signal.SIGTERM):
            previous[sig] = signal.signal(sig, on_signal)
    except ValueError:  # not the main thread; no signal handling is possible
        previous = {}
    proc = None
    try:
        proc = subprocess.Popen(cmd, cwd=cwd, env=env, **({} if input is None else {"stdin": subprocess.PIPE}),
                                **_OWN_GROUP)
        try:
            if input is not None:
                threading.Thread(target=_feed_stdin, args=(proc, input), daemon=True).start()
            if fd is None:
                rc = proc.wait()
            else:
                _set_foreground(fd, proc.pid)
                rc = _wait_in_foreground(proc, fd)
        except BaseException:
            for sig in previous:   # no re-entry while the child's group is taken down
                signal.signal(sig, signal.SIG_IGN)
            if proc.poll() is None or _group_alive(proc.pid):    # what the child started may outlive it
                _terminate_group(proc)
            raise
    finally:
        if fd is not None and proc is not None:
            _set_foreground(fd, os.getpgrp())
        for sig, handler in previous.items():
            signal.signal(sig, handler)
    if fd is not None and rc in (130, -signal.SIGINT):
        raise KeyboardInterrupt     # the ^C reached only the child's group
    return rc


def run_tool(cmd, parser, step_id, title, *, n=None, parent=None, cwd=None, env=None, sinks=None, capture=False,
             tty=False, watchdog=None, summarize=None, label=None, statuses=None, input=None, reports=True):
    """Run one tool as one step and return Result(exit, ms, summary).

    Pass-through mode (markers off, default sinks, no capture or summarize) hands the child the real terminal and no
    parser runs.  The child gets a process group of its own, so a cancel takes all of it down; when stdin is our
    controlling terminal and our group holds its foreground, the child's group holds it while the child runs, and it
    comes back to ours however the child ends.  A ^C that ends the child (exit 130, or killed by SIGINT) then cancels,
    as it did when the ^C reached us too.  A ^Z that stops the child stops us as well; resumed in the foreground, we
    give the child the terminal again and resume it.  Piped mode merges stdout and stderr, feeds the parser and honours the sinks; with tty=True the
    child runs under a PTY and our keystrokes are forwarded to it.  GPG_TTY then names the PTY, so a terminal
    pinentry prompts through it (with echo off) instead of racing us for the real terminal.  watchdog=<s> publishes a
    silent partial line as a prompt, or the silence as quiet.  summarize(text) becomes the step_end summary.
    statuses maps an exit code to this row's status ("ok", "fail" or "skip"); a code not in it is "ok" for 0 and
    "fail" otherwise; 127 (cannot start) and 130 (interrupted) always fail.  exit is reported only for a failed row.
    SIGINT/SIGTERM unwind as a KeyboardInterrupt; the except block then takes down the child's whole process group,
    `end` is emitted with exit 130 and the KeyboardInterrupt is raised again.  A tool that cannot be started ends its
    step with exit 127 and the OSError is raised again.  Any other error ends the step as failed and is raised again.
    input (bytes) is written to the child's stdin and stdin is closed; it is never recorded or published.  Not for tty.
    A parser's report events go through report() (copied under $RUN_ARTIFACTS when it is set, relative paths taken
    from cwd); reports=False drops them.
    """
    sinks = sinks or Sinks()
    t0 = time.monotonic()
    labelled = {"label": label} if label else {}
    labelled.update(tool=_parser_name(parser), sig=hashlib.sha1(" ".join(map(str, cmd)).encode()).hexdigest()[:8])
    if not enabled() and sinks.is_default and not capture and summarize is None:
        publish("step_start", id=step_id, n=n if n is not None else 1, title=title, parent=parent, **labelled)
        rc = 130
        try:
            rc = _run_in_foreground(cmd, cwd, env, input)
        except OSError:
            rc = 127   # cannot start: as run_pty reports it, not as a Ctrl-C
            raise
        finally:
            ms = int((time.monotonic() - t0) * 1000)
            status, exit_code = _tool_status(rc, statuses)
            publish("step_end", id=step_id, status=status, ms=ms, exit=exit_code, summary=None, totals=None)
        return Result(rc, ms)

    parser = _resolve_parser(parser)
    parser.step_id = step_id
    parser.interactive = bool(tty)  # before prepare: under a terminal, a parser must not change what the tool prints
    guard = _Isolated(parser)
    try:
        cmd, env = parser.prepare(cmd, env, cwd)
    except Exception as e:
        warn(f"parser {getattr(parser, 'name', '?')} prepare failed ({e!r}); using the original command")
    rewriting = bool(getattr(parser, "rewrites_log", False)) and not tty   # read once; under a terminal never
    dispatch = _Dispatcher(step_id, sinks, cwd=cwd, reports=reports)
    try:
        emit("step", id=step_id, n=n if n is not None else 1, title=title, parent=parent)
        publish("step_start", id=step_id, n=n if n is not None else 1, title=title, parent=parent, **labelled)
        sinks.open()
    except BaseException:
        guard.close()   # prepare() may have made files: the main try below is not reached
        sinks.close()
        raise
    master = None
    try:
        if tty:
            master, slave = os.openpty()
            try:
                size = shutil.get_terminal_size()
                fcntl.ioctl(slave, termios.TIOCSWINSZ, struct.pack("HHHH", size.lines, size.columns, 0, 0))
                env = {**(os.environ if env is None else env), "GPG_TTY": os.ttyname(slave)}
                proc = subprocess.Popen(cmd, cwd=cwd, env=env, stdin=slave, stdout=slave, stderr=slave,
                                        start_new_session=True, preexec_fn=_set_ctty, close_fds=True)
            finally:
                os.close(slave)
            fd = master
        else:
            proc = subprocess.Popen(cmd, cwd=cwd, env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                    stdin=subprocess.PIPE if input is not None else None, start_new_session=True)
            fd = proc.stdout.fileno()
            if input is not None:
                threading.Thread(target=_feed_stdin, args=(proc, input), daemon=True).start()
    except OSError:
        # Cannot start: end the step (as run_pty does) so its row is not left running, then let the caller see why.
        if master is not None:
            os.close(master)
        guard.close()
        sinks.close()
        ms = int((time.monotonic() - t0) * 1000)
        emit("end", id=step_id, status="fail", ms=ms, exit=127)
        publish("step_end", id=step_id, status="fail", ms=ms, exit=127, summary=None, totals=None)
        raise
    cancelled = []
    failed = False

    def on_signal(signum, frame):
        # Only unwind: the terminate-and-wait for the child's group happens in the except block below, never in here.
        cancelled.append(signum)
        raise KeyboardInterrupt

    previous = {}
    try:
        for sig in (signal.SIGINT, signal.SIGTERM):
            previous[sig] = signal.signal(sig, on_signal)
    except ValueError:  # not the main thread; no signal handling is possible
        previous = {}

    keys = _input_fd() if tty else None
    saved_mode = None
    saved_fd = None
    watch = _Watch(step_id, watchdog, echo=lambda: sinks.console != "full" and (_console is None or _console.stopped))
    decoder = codecs.getincrementaldecoder("utf-8")(errors="replace")
    pending = ""
    captured = [] if capture or summarize is not None else None

    def show(text, chunk=None):
        """Output as the log, capture and watchdog see it: the tool's bytes, or a rewriting parser's rendered text."""
        sinks.write_full(chunk if chunk is not None else text.encode("utf-8"))
        if captured is not None:
            captured.append(text)
        watch.output(text)

    def feed(text):
        nonlocal pending
        pending += text
        while "\n" in pending:
            raw, pending = pending.split("\n", 1)
            if rewriting:
                shown = guard.render(raw)
                if shown:
                    show(shown if shown.endswith("\n") else shown + "\n")
            line = strip_ansi(raw) if tty else raw
            line = line.rstrip("\r").rsplit("\r", 1)[-1]
            dispatch.apply(guard.call("on_line", line))

    def take(chunk):
        text = decoder.decode(chunk)
        if not rewriting:
            show(text, chunk)
        feed(text)

    def read(source):
        """One chunk, or b"" at end of output.  A PTY master reports the slave side closing as EIO."""
        try:
            return os.read(source, 65536)
        except OSError as e:
            if e.errno != errno.EIO:
                raise
            return b""

    next_poll = 0.0     # monotonic time of the next poll in the loop: a chatty tool must not cost one per chunk

    try:
        if keys is not None and os.isatty(keys):
            saved_mode = termios.tcgetattr(keys)
            saved_fd = keys
            _tty.setcbreak(keys)    # the PTY echoes what is typed; ISIG stays on, so Ctrl-C still arrives
        try:
            while True:
                try:
                    ready, _, _ = select.select([fd] + ([keys] if keys is not None else []), [], [], 0.1)
                except InterruptedError:
                    continue
                if _console is not None:
                    _console.tick()     # every pass, not only on a timeout: a throttled redraw has no trailing edge
                now = time.monotonic()
                if now >= next_poll:    # every pass too, but at most every POLL_INTERVAL: a side-file parser has news
                    next_poll = now + POLL_INTERVAL     # when the tool is silent
                    dispatch.apply(guard.call("poll"))
                if keys is not None and keys in ready:
                    typed = os.read(keys, 1024)
                    if typed:
                        try:
                            os.write(master, typed)
                        except OSError:     # the child has gone (EIO): drop the keys
                            pass
                    else:
                        keys = None
                if fd not in ready:
                    watch.idle()
                    if tty and proc.poll() is not None:
                        while select.select([fd], [], [], 0)[0]:   # bytes written after the last select()
                            chunk = read(fd)
                            if not chunk:
                                break
                            take(chunk)
                        break
                    continue
                chunk = read(fd)
                if not chunk:
                    break
                take(chunk)
            proc.wait()
            if pending:
                feed("\n")
            dispatch.apply(guard.call("poll"))      # what the throttle held back
            dispatch.apply(guard.call("finish", proc.returncode))
        except KeyboardInterrupt:
            if not cancelled:
                cancelled.append(signal.SIGINT)
            for sig in previous:   # no re-entry while the child's group is taken down
                signal.signal(sig, signal.SIG_IGN)
            if _console is not None:
                _console.pause()    # clear the live region and show the cursor before anything else prints
            _terminate_group(proc)
    except Exception:
        failed = True
        raise
    finally:
        for sig in previous:    # a Ctrl-C now would cut the cleanup short: the PTY left open, the row left running
            signal.signal(sig, signal.SIG_IGN)
        try:
            try:
                if saved_mode is not None:
                    termios.tcsetattr(saved_fd, termios.TCSADRAIN, saved_mode)
            finally:
                if proc.poll() is None:     # an error before the child was reaped (e.g. terminal setup) must not
                    _terminate_group(proc)  # orphan it; as above, no re-entry while its group is taken down
                guard.close()
                if master is not None:
                    os.close(master)
                else:
                    proc.stdout.close()
                sinks.close()
        finally:
            for sig, handler in previous.items():
                signal.signal(sig, handler)
            if failed:  # an error, not a cancel: end the row (the error is raised again) so it is not left running
                ms = int((time.monotonic() - t0) * 1000)
                dispatch.close_children("fail")
                emit("end", id=step_id, status="fail", ms=ms, exit=proc.returncode or None)
                publish("step_end", id=step_id, status="fail", ms=ms, exit=proc.returncode or None, summary=None,
                        totals=None)

    ms = int((time.monotonic() - t0) * 1000)
    if cancelled:
        dispatch.close_children("fail")
        emit("end", id=step_id, status="fail", ms=ms, exit=130)
        publish("step_end", id=step_id, status="fail", ms=ms, exit=130, summary=None, totals=None)
        raise KeyboardInterrupt
    status, exit_code = _tool_status(proc.returncode, statuses)
    dispatch.close_children(status)
    dispatch.flush_tests(step_id)
    emit("end", id=step_id, status=status, ms=ms, exit=exit_code)
    text = "".join(captured) if captured is not None else None
    if text is not None and tty:
        text = text.replace("\r\n", "\n")    # joined first: a CRLF split across two reads still collapses
    summary = None
    if summarize is not None:
        try:
            summary = summarize(text)
        except Exception as e:  # a summary is display only; it never fails the step
            warn(f"summary for {step_id} failed: {e!r}")
    publish("step_end", id=step_id, status=status, ms=ms, exit=exit_code,
            summary=summary, totals=dispatch.summary or None)
    return Result(proc.returncode, ms, dispatch.summary, text if capture else None)


# ---------------------------------------------------------------------------------------------------------------------
# PTY mode
#
# The tool runs under a pseudo-terminal; its bytes go to the full log unchanged and the parser sees a cleaned copy of
# each line.  Markers are translated into run-view events (step, end, suite, note, done) and written to a JSONL file.
# ---------------------------------------------------------------------------------------------------------------------

DEFAULT_SIZE = (120, 40)
SIZE_RE = re.compile(r"^(\d{1,4})x(\d{1,4})$")
STEP_ID_RE = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$")
FW_RE = re.compile(r"^[a-z0-9][a-z0-9._-]{0,31}$")
MAX_TITLE = 200
MAX_NOTE = 1000
MAX_PTY_LINE = 1 << 20

# CSI, OSC, DCS/SOS/PM/APC, other two-byte escapes, then C0 controls other than TAB, LF and CR, and DEL.
PTY_ESC_RE = re.compile(
    r"\x1b\[[0-?]*[ -/]*[@-~]"
    r"|\x1b\][^\x07\x1b]*(?:\x07|\x1b\\)?"
    r"|\x1b[PX^_][^\x1b]*(?:\x1b\\)?"
    r"|\x1b[ -/]*[0-~]"
    r"|[\x00-\x08\x0b\x0c\x0e-\x1f\x7f]")


def pty_clean(raw):
    """The text a parser sees for one PTY line: escapes and controls removed, then only what follows the last \\r."""
    text = PTY_ESC_RE.sub("", raw.decode("utf-8", errors="replace"))
    return text.rstrip("\r").rsplit("\r", 1)[-1]


def _event_id(value):
    """Maps a marker step id onto the run-view step-id grammar: / becomes ., anything else invalid becomes -."""
    s = re.sub(r"[^A-Za-z0-9._-]", "-", str(value).replace("/", "."))
    if not re.match(r"[A-Za-z0-9]", s):
        s = "s" + s
    return s[:64]


def _clip(text, limit):
    """text cut to at most limit UTF-16 code units, the unit the run-view contract counts in."""
    text = str(text)
    out, units = [], 0
    for ch in text:
        units += 2 if ord(ch) > 0xFFFF else 1
        if units > limit:
            break
        out.append(ch)
    return "".join(out)


class _EventsFile:
    """Writes run-view events to a JSONL file, one flushed line per event, so a reader never sees half a line."""

    def __init__(self, path, fw):
        self.fw = fw if FW_RE.match(fw) else "generic"
        Path(path).parent.mkdir(parents=True, exist_ok=True)
        self._f = open(path, "a", encoding="utf-8")

    def close(self):
        self._f.close()

    def write(self, ev, fields):
        obj = self._translate(ev, fields)
        if obj is None:
            return
        line = json.dumps(obj, ensure_ascii=False, separators=(",", ":"))
        if len(line.encode("utf-8")) > MAX_BYTES:   # not reachable from PTY mode: titles and notes are clipped well below this
            return
        self._f.write(line + "\n")
        self._f.flush()

    def _translate(self, ev, f):
        if ev == "step":
            out = {"ev": "step", "id": _event_id(f["id"]), "title": _clip(f.get("title") or f["id"], MAX_TITLE) or "-"}
            n = f.get("n")
            if isinstance(n, int) and 1 <= n <= 9999:
                out["n"] = n
            out["rawOffset"] = _line_offset
            return out
        if ev == "end":
            out = {"ev": "end", "id": _event_id(f["id"]), "status": f["status"]}
            for k in ("ms", "exit"):
                if k in f:
                    out[k] = f[k]
            return out
        if ev == "tests":
            fail = f["fail"] + f["err"]
            sid = _event_id(f["step"])
            counts = {"pass": max(0, f["total"] - fail - f["skip"]), "fail": fail, "skip": f["skip"]}
            return {"ev": "suite", "step": sid, "fw": self.fw, "suite": sid, "counts": counts, "rawOffset": _line_offset}
        if ev == "note":
            out = {"ev": "note", "level": f["level"], "text": _clip(f["text"], MAX_NOTE) or "-"}
            if "href" in f:
                out["href"] = f["href"]
            if "step" in f:
                out["step"] = _event_id(f["step"])
            return out
        if ev == "done":
            return {"ev": "done", "status": f["status"]}
        return None   # run and report have no run-view form


def _set_ctty():
    """In the child, after setsid: make the PTY slave (stdin) the controlling terminal and let ^C and ^\\ reach the job.

    SIGINT and SIGQUIT are reset to their defaults because a backgrounded launcher passes on ignored signals, and an
    ignored signal survives exec.  The signal mask is cleared too, because a JVM blocks SIGQUIT in its threads and a
    child started from one inherits that.  SIGTSTP, SIGTTIN and SIGTTOU are left alone: only an interactive shell
    ignores them, and it resets them for its children; the job leads an orphaned process group, which never stops on
    them anyway."""
    signal.signal(signal.SIGINT, signal.SIG_DFL)
    signal.signal(signal.SIGQUIT, signal.SIG_DFL)
    signal.pthread_sigmask(signal.SIG_SETMASK, ())
    try:
        fcntl.ioctl(0, termios.TIOCSCTTY, 0)
    except OSError:
        pass


class _LineBuffer:
    """Splits a byte stream into lines of at most MAX_PROMPT_LINE bytes.  A longer line is dropped whole, with a warn
    that names no content."""

    def __init__(self, what):
        self.what = what
        self.buf = b""
        self.skipping = False   # inside an oversize line, up to its newline

    def feed(self, data):
        lines = []
        self.buf += data
        while True:
            i = self.buf.find(b"\n")
            if i < 0:
                if len(self.buf) > MAX_PROMPT_LINE:
                    warn(f"{self.what} line over the 1 MiB limit dropped")
                    self.skipping = True
                if self.skipping:
                    self.buf = b""
                return lines
            line, self.buf = self.buf[:i], self.buf[i + 1:]
            if self.skipping:
                self.skipping = False
            elif len(line) > MAX_PROMPT_LINE:
                warn(f"{self.what} line over the 1 MiB limit dropped")
            else:
                lines.append(line)


class _PromptRelay:
    """Two pipes between the job and this process's stdio: the job's prompt lines go to our stdout, our stdin lines
    go to the job.  Nothing here is logged or recorded; a malformed line is dropped with a warn that names no
    content, and a line is relayed as received, so the relay needs to know nothing of ids or kinds."""

    def __init__(self):
        self.out_r, self.out_w = os.pipe()     # job -> us
        self.in_r, self.in_w = os.pipe()       # us -> job
        os.set_blocking(self.out_r, False)
        os.set_blocking(self.in_w, False)
        self._from_job = _LineBuffer("prompt")
        self._from_host = _LineBuffer("answer")
        self._to_job = b""
        self.eof = False                       # our stdin reached EOF: the host is gone
        self._job_closed = False               # every writer of out_r has closed it

    def env(self):
        return {"JUNEAU_PROMPT_FD_OUT": str(self.out_w), "JUNEAU_PROMPT_FD_IN": str(self.in_r)}

    def pass_fds(self):
        return (self.out_w, self.in_r)

    def after_spawn(self):
        """The job has its ends: close ours of them."""
        for fd in (self.out_w, self.in_r):
            with suppress(OSError):
                os.close(fd)

    def read_fds(self):
        return ([] if self._job_closed else [self.out_r]) + ([] if self.eof else [0])

    def write_fds(self):
        return [self.in_w] if self._to_job else []

    def on_readable(self, fd):
        try:
            data = os.read(fd, 65536)
        except BlockingIOError:
            return
        except OSError:
            data = b""
        if fd == 0:
            if not data:
                self.eof = True
                return
            for line in self._from_host.feed(data):
                if self._valid(line, "answer"):
                    self._to_job += line + b"\n"
        elif data:
            self._relay(data)
        else:
            self._job_closed = True

    def _relay(self, data):
        for line in self._from_job.feed(data):
            if self._valid(line, "prompt"):
                sys.stdout.buffer.write(line + b"\n")
                sys.stdout.buffer.flush()

    @staticmethod
    def _valid(line, what):
        try:
            json.loads(line)
        except ValueError:
            warn(f"{what} line is not JSON ({len(line)} bytes), dropped")
            return False
        return True

    def on_writable(self):
        try:
            n = os.write(self.in_w, self._to_job)
            self._to_job = self._to_job[n:]
        except BlockingIOError:
            pass
        except OSError:   # the job closed its end
            self._to_job = b""

    def drain(self):
        """After the job exits: deliver whatever prompt lines are still buffered in the pipe."""
        while True:
            try:
                data = os.read(self.out_r, 65536)
            except OSError:   # BlockingIOError included: nothing more is buffered
                break
            if not data:
                break
            self._relay(data)

    def close(self):
        for fd in (self.out_r, self.in_w):
            with suppress(OSError):
                os.close(fd)


def run_pty(cmd, parser, step_id, title, *, cols, rows, sinks, cwd=None, env=None, stdin_fd=None, prompts=None):
    """Run one tool under a cols x rows PTY as one step and return Result(exit, ms, summary).

    The tool's bytes go to the full log exactly as written (the PTY turns \\n into \\r\\n).  Each line is cleaned with
    pty_clean() before the parser sees it, and _line_offset holds the log offset of its start while its events are
    written.  A signal death exits 128+n with a warn note.  SIGINT/SIGTERM cancel as in run_tool().

    With stdin_fd, bytes read from it are copied to the PTY master unchanged, so they reach the job through the
    terminal's line discipline (^C interrupts, ^D ends a read); they reach the log only if the terminal echoes them.
    EOF on stdin_fd stops the copying but not the job.

    With prompts (a _PromptRelay), the job gets two extra pipes (JUNEAU_PROMPT_FD_OUT and _IN) relayed to this
    process's stdout and stdin.  EOF on stdin then means the host is gone: the job's group gets SIGTERM, then SIGKILL
    after PROMPTS_GRACE_SECONDS, and the run is cancelled.  Not combined with stdin_fd.
    """
    global _line_offset
    t0 = time.monotonic()
    parser = _resolve_parser(parser)
    parser.step_id = step_id
    parser.interactive = True
    guard = _Isolated(parser)
    env = dict(os.environ if env is None else env)
    env.pop("RUN_MARKERS", None)
    env.pop(ACTIVE_ENV, None)
    env["TERM"] = "xterm-256color"
    if prompts is not None:
        env.update(prompts.env())
    try:
        cmd, env = parser.prepare(cmd, env, cwd)
    except Exception as e:
        warn(f"parser {getattr(parser, 'name', '?')} prepare failed ({e!r}); using the original command")
    dispatch = _Dispatcher(step_id, sinks)
    master = slave = None
    try:
        sinks.open()
        base = sinks.full_path.stat().st_size
        _line_offset = base
        emit("step", id=step_id, n=1, title=title)

        master, slave = os.openpty()
        fcntl.ioctl(slave, termios.TIOCSWINSZ, struct.pack("HHHH", rows, cols, 0, 0))
    except BaseException:
        for fd in (master, slave):
            if fd is not None:
                os.close(fd)
        guard.close()   # prepare() may have made files: the try below is not reached
        sinks.close()
        raise
    try:
        proc = subprocess.Popen(cmd, cwd=cwd, env=env, stdin=slave, stdout=slave, stderr=slave,
                                start_new_session=True, preexec_fn=_set_ctty, close_fds=True,
                                pass_fds=prompts.pass_fds() if prompts is not None else ())
    except OSError as e:
        if prompts is not None:
            prompts.after_spawn()
            prompts.close()
        os.close(master)
        os.close(slave)
        guard.close()
        sinks.close()
        warn(f"cannot start {cmd[0]}: {e}")
        ms = int((time.monotonic() - t0) * 1000)
        emit("end", id=step_id, status="fail", ms=ms, exit=127)
        return Result(127, ms)
    os.close(slave)
    if prompts is not None:
        prompts.after_spawn()
    cancelled = []
    pending = bytearray()   # input read from stdin_fd but not yet written to the master
    in_fd = stdin_fd
    if stdin_fd is not None:
        os.set_blocking(master, False)

    def on_signal(signum, frame):
        cancelled.append(signum)
        raise KeyboardInterrupt

    previous = {}
    try:
        for sig in (signal.SIGINT, signal.SIGTERM):
            previous[sig] = signal.signal(sig, on_signal)
    except ValueError:
        previous = {}

    line = bytearray()
    line_start = base

    def parse(raw, start):
        global _line_offset
        _line_offset = start
        dispatch.apply(guard.call("on_line", pty_clean(bytes(raw))))

    def feed(chunk):
        nonlocal line_start
        line.extend(chunk)
        while True:
            i = line.find(b"\n")
            if i < 0:
                break
            parse(line[:i], line_start)
            del line[:i + 1]
            line_start += i + 1
        if len(line) > MAX_PTY_LINE:   # a line with no end in sight: parse what there is and start a new one
            parse(line, line_start)
            line_start += len(line)
            line.clear()

    def pump(rlist, wlist):
        """One select() turn with input on: copies stdin to the master; returns True if the master is readable."""
        nonlocal in_fd
        r, w, _ = select.select(rlist, wlist, [], 0.1)
        if in_fd is not None and in_fd in r:
            try:
                chunk = os.read(in_fd, 4096)
            except BlockingIOError:
                chunk = None   # nothing readable after all; try again next turn
            except OSError:
                chunk = b""
            if chunk:
                pending.extend(chunk)
            elif chunk is not None:
                in_fd = None   # EOF: stop copying; the job keeps running
        if w:
            try:
                n = os.write(master, bytes(pending[:4096]))
                del pending[:n]
            except BlockingIOError:
                pass
            except OSError as e:
                if e.errno != errno.EIO:
                    raise
                pending.clear()
                in_fd = None
        return master in r

    def pump_prompts():
        """One select() turn with prompts on: relays both directions; returns True if the master is readable.
        Stdin EOF cancels the run."""
        r, w, _ = select.select([master] + prompts.read_fds(), prompts.write_fds(), [], 0.1)
        for fd in r:
            if fd != master:
                prompts.on_readable(fd)
        if w:
            prompts.on_writable()
        if prompts.eof:
            cancelled.append(signal.SIGTERM)
            raise KeyboardInterrupt
        return master in r

    def drain(fd):
        """Reads whatever is still buffered on the PTY master without blocking, until it is empty or reports EIO."""
        while select.select([fd], [], [], 0)[0]:
            try:
                chunk = os.read(fd, 65536)
            except BlockingIOError:
                return
            except OSError as e:
                if e.errno != errno.EIO:
                    raise
                return
            if not chunk:
                return
            sinks.write_full(chunk)
            feed(chunk)

    try:
        try:
            while True:
                try:
                    if prompts is not None:
                        ready = pump_prompts()
                    elif stdin_fd is None:
                        ready, _, _ = select.select([master], [], [], 0.1)
                    else:
                        ready = pump([master] + ([in_fd] if in_fd is not None and not pending else []),
                                     [master] if pending else [])
                    _line_offset = line_start               # a poll event's rawOffset is the end of the last line seen
                    dispatch.apply(guard.call("poll"))
                    if not ready:
                        if proc.poll() is not None:
                            drain(master)   # the child may have written its last bytes after select() timed out
                            break
                        continue
                    chunk = os.read(master, 65536)
                except (InterruptedError, BlockingIOError):
                    continue
                except OSError as e:
                    if e.errno != errno.EIO:   # EIO is how a PTY master reports that the slave side has closed
                        raise
                    chunk = b""
                if not chunk:
                    break
                sinks.write_full(chunk)
                feed(chunk)
            proc.wait()
            if prompts is not None:
                prompts.drain()
            if line:
                parse(line, line_start)
                line_start += len(line)
            _line_offset = line_start
            dispatch.apply(guard.call("finish", proc.returncode))
        except KeyboardInterrupt:
            if not cancelled:
                cancelled.append(signal.SIGINT)
            for sig in previous:
                signal.signal(sig, signal.SIG_IGN)
            _terminate_group(proc, PROMPTS_GRACE_SECONDS if prompts is not None else None)
    finally:
        if prompts is not None:
            prompts.close()
        if proc.poll() is None:   # an unexpected error escaped the read loop: do not leave the child running
            try:
                os.killpg(proc.pid, signal.SIGKILL)
            except OSError:
                proc.kill()
            proc.wait()
        guard.close()
        for sig, handler in previous.items():
            signal.signal(sig, handler)
        os.close(master)
        sinks.close()

    ms = int((time.monotonic() - t0) * 1000)
    if cancelled:
        dispatch.close_children("fail")
        emit("end", id=step_id, status="fail", ms=ms, exit=130)
        raise KeyboardInterrupt
    rc = proc.returncode
    if rc < 0:
        try:
            name = signal.Signals(-rc).name
        except ValueError:
            name = f"signal {-rc}"
        note("warn", f"{step_id} killed by {name}", step=step_id)
        rc = 128 - rc
    status = "ok" if rc == 0 else "fail"
    dispatch.close_children(status)
    dispatch.flush_tests(step_id)
    emit("end", id=step_id, status=status, ms=ms, exit=None if rc == 0 else rc)
    return Result(rc, ms, dispatch.summary)


def _main_pty(ap, ns, cmd):
    """The --pty CLI: check the options, write the .size sidecar, run the tool and always end with a done event."""
    global _events
    if ns.input and not ns.pty:
        ap.error("--input needs --pty")
    if ns.prompts and not ns.pty:
        ap.error("--prompts needs --pty")
    if ns.prompts and ns.input:
        ap.error("--prompts cannot be combined with --input")
    if not ns.pty:
        ap.error("--events and --size need --pty")
    if not ns.events:
        ap.error("--pty needs --events PATH")
    if not ns.full_log:
        ap.error("--pty needs --full-log PATH")
    if os.name != "posix":
        ap.error("--pty is only supported on POSIX systems")
    cols, rows = DEFAULT_SIZE
    if ns.size:
        m = SIZE_RE.match(ns.size)
        if not m or not (1 <= int(m.group(1)) <= 9999 and 1 <= int(m.group(2)) <= 9999):
            ap.error(f"--size must be COLSxROWS, e.g. 120x40; got {ns.size!r}")
        cols, rows = int(m.group(1)), int(m.group(2))
    sinks = Sinks(ns.console or "none", ns.full_log, ns.condensed_log)
    sinks.full_path.parent.mkdir(parents=True, exist_ok=True)
    Path(str(sinks.full_path) + ".size").write_text(json.dumps({"cols": cols, "rows": rows}), encoding="utf-8")
    _events = _EventsFile(Sinks._resolve(ns.events), ns.tool)
    try:
        try:
            result = run_pty(cmd, ns.tool, ns.step or ns.tool, ns.title or " ".join(cmd[:2]),
                             cols=cols, rows=rows, sinks=sinks, stdin_fd=0 if ns.input == "stdin" else None,
                             prompts=_PromptRelay() if ns.prompts == "stdio" else None)
        except KeyboardInterrupt:
            _events.write("done", {"status": "cancelled"})
            return 130
        _events.write("done", {"status": "ok" if result.exit == 0 else "fail"})
        return result.exit
    finally:
        _events.close()
        _events = None


# ---------------------------------------------------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------------------------------------------------


def _git(*args, cwd=None):
    try:
        out = subprocess.run(["git", *args], capture_output=True, text=True, check=True, cwd=cwd,
                             timeout=5).stdout.strip()
        return out or None
    except Exception:
        return None


def _parser_names():
    """The CLI's parser choices: the built-ins plus any registered in juneau_scripts.parsers, if it can be found."""
    names = set(PARSERS)
    try:
        names.update(_toolkit_package().REGISTRY)
    except Exception:
        pass
    return sorted(names)


def main(argv=None):
    ap = argparse.ArgumentParser(prog="juneau_run.py", description="Run one tool as one run-protocol step.")
    ap.add_argument("--console", choices=CONSOLE_MODES)
    ap.add_argument("--full-log")
    ap.add_argument("--condensed-log")
    ap.add_argument("--step")
    ap.add_argument("--title")
    ap.add_argument("--pty", action="store_true")
    ap.add_argument("--size")
    ap.add_argument("--events")
    ap.add_argument("--input", choices=["stdin"], help="copy this process's stdin to the PTY (needs --pty)")
    ap.add_argument("--prompts", choices=["stdio"],
                    help="relay the job's prompt channel over this process's stdin and stdout (needs --pty)")
    ap.add_argument("tool", choices=_parser_names())
    ap.add_argument("cmd", nargs=argparse.REMAINDER)
    ns = ap.parse_args(argv)
    cmd = ns.cmd[1:] if ns.cmd[:1] == ["--"] else ns.cmd
    if not cmd:
        ap.error("no command given after --")
    if ns.pty or ns.events or ns.size or ns.input or ns.prompts:
        return _main_pty(ap, ns, cmd)
    history_start()
    sinks = Sinks(ns.console, ns.full_log, ns.condensed_log)
    owner = enabled() and not _nested()
    if owner:
        run(mode="test", project=Path.cwd().name, branch=_git("rev-parse", "--abbrev-ref", "HEAD"),
            head=_git("rev-parse", "--short", "HEAD"))
    try:
        result = run_tool(cmd, ns.tool, ns.step or ns.tool, ns.title or " ".join(cmd[:2]), sinks=sinks)
    except KeyboardInterrupt:
        if owner:
            done("cancelled")
        return 130
    if owner:
        done("ok" if result.exit == 0 else "fail")
    return result.exit


if __name__ == "__main__":
    sys.exit(main())
