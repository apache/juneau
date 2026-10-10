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
Run wrapper: run-protocol v1 markers, output routing and tool-output parsers for Maven, pytest and Playwright.

Stdlib-only and standalone, so it can be copied or loaded from anywhere.  Every marker function is a no-op
unless the environment has RUN_MARKERS=1; the open-line helpers (open_line, append, dot, set_tail, close_line)
always write.  A marker always starts on a fresh line.  Sections, in order: markers, sinks, parsers, runner, CLI.

CLI:  python3 juneau_run.py [--console full|condensed|none] [--full-log PATH] [--condensed-log PATH]
                            [--step ID] [--title T] [--pty --events PATH [--size CxR]]
                            <maven|pytest|playwright|generic> -- <cmd...>

With --pty the tool runs under a pseudo-terminal of the given size (default 120x40); its raw bytes go to --full-log,
the size to <full-log>.size, and run-view events (not ##run markers) to the --events JSONL file.  POSIX only.
"""

import argparse
import atexit
import codecs
import collections
import errno
import fcntl
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
import time
import tty as _tty
import unicodedata
import xml.etree.ElementTree as ET
from collections import OrderedDict
from contextlib import contextmanager
from pathlib import Path

PREFIX = "##run "
MAX_BYTES = 65536
ACTIVE_ENV = "JUNEAU_RUN_ACTIVE"
GRACE_SECONDS = 5.0
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

    def __init__(self):
        self.status = "ok"
        self.exit = None
        self.summary = None

    def fail(self, exit=None):  # NOSONAR python:S5806 - parameter names mirror protocol field names
        self.status = "fail"
        self.exit = exit

    def skip(self):
        self.status = "skip"


@contextmanager
def step(id, n, title, *, parent=None, label=None):  # NOSONAR python:S5806 - parameter names mirror protocol field names
    """Emit `step`, run the block, then emit `end` with its status and duration.  An exception ends it as fail and propagates."""
    emit("step", id=id, n=n, title=title, parent=parent)
    publish("step_start", id=id, n=n, title=title, parent=parent, **({"label": label} if label else {}))
    s = Step()
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


def report(step, kind, path):
    """Point the runner at a step's test results (kind: surefire, junitxml, jest-json, playwright-json)."""
    emit("report", step=step, kind=kind, path=str(path))


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


def publish(kind, **fields):
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
    """Script status text.  Printed as-is unless a console view owns the screen, which shows it as a filtered note."""
    if not _console_owns_screen():
        _out(text + "\n")
    publish("say", text=text, level=level, step=step)


def failure(text, step=None):
    """Failure detail that every detail level shows: printed as-is, or by the console view when it owns the screen."""
    if not _console_owns_screen():
        _out(text + "\n")
    publish("failure", text=text, step=step)


@contextmanager
def row(id, title, *, label=None):  # NOSONAR python:S5806 - parameter names mirror protocol field names
    """A console row around a block, with no marker.  For script work that is not a protocol step (Perf, Timing)."""
    publish("step_start", id=id, n=None, title=title, parent=None, **({"label": label} if label else {}))
    s = Step()
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
PLAIN_GLYPHS = {"pass": "#", "fail": "F", "skip": "."}
NO_COLOR_GLYPHS = {"pass": "█", "fail": "F", "skip": "·"}
COLOR_GLYPHS = {"pass": "\x1b[32m█\x1b[0m", "fail": "\x1b[31m█\x1b[0m", "skip": "\x1b[2m█\x1b[0m"}
PENDING = "░"
_NOTE_PREFIX = {"info": "ℹ", "warn": "⚠", "error": "✗"}


def fmt_elapsed(ms):
    """M:SS, or H:MM:SS from an hour."""
    h, rem = divmod(int(ms // 1000), 3600)
    m, s = divmod(rem, 60)
    return f"{h}:{m:02d}:{s:02d}" if h else f"{m}:{s:02d}"


def _totals(t):
    """A tests event's fields or a run_tool summary dict, as a (total, fail, err, skip) tuple."""
    if t is None or isinstance(t, tuple):
        return t
    if isinstance(t, list):
        return tuple(t)
    return (t["total"], t["fail"], t["err"], t["skip"])


def fmt_totals(totals, tests_mode=False):
    if not totals or not totals[0]:
        return "no tests run" if tests_mode else ""
    total, fail, err, _ = totals
    return f"{total:,} tests" + (f", {fail + err} failed" if fail + err else "")


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

    @property
    def in_progress(self):
        """Still running and not yet boxed.  Pom modules are boxed by the reactor event, so they never show here."""
        return self.status is None and self.box is None


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
        self.notes = []         # (level, text), held until the row ends
        self.size = None        # reactor size N
        self.poms = set()
        self.tests_mode = False
        self.quiet = 0          # seconds the tool has been silent, from the watchdog

    @property
    def running(self):
        return self.status is None

    def boxes(self):
        return [self.modules[m].box for m in self.order]


class Board:
    """The run's state for the console view.  apply(kind, fields) returns the blocks that event produces."""

    def __init__(self, detail="actionable", nested=False, full_log=None, clock=time.monotonic):
        self.detail = detail
        self.nested = nested
        self.full_log = full_log
        self.clock = clock
        self.t0 = clock()
        self.rows = OrderedDict()
        self.module_rows = {}       # module id -> its _Row
        self.failure_lines = set()  # stripped lines already shown by a failure event, so notes don't repeat them
        self.failed_title = None
        self.aliases = {}           # a run_tool sub-row's step id -> its parent row's id (parent=, release.py)

    def width(self):
        env = os.environ.get(LABEL_WIDTH_ENV, "")
        return max([int(env) if env.isdigit() else 10] + [len(r.label) for r in self.rows.values()])

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
        if self.nested:
            return []
        return [("header", " · ".join([f["title"]] + list(f["header"])))]   # the title carries its own emoji

    def _on_step_start(self, f):
        parent = self.rows.get(f.get("parent"))
        if parent is not None:      # a sub-row: a box on its parent's row until its tool sends modules (_adopt)
            self.aliases[f["id"]] = parent.id
            return self._on_module_start({"step": parent.id, "module": f["id"], "title": f["title"]})
        self.rows[f["id"]] = _Row(f["id"], f["n"], f["title"], f.get("label"), self.clock())
        return []

    def _on_module_start(self, f):
        r = self._adopt(f["step"])
        if r is not None:
            m = r.modules.get(f["module"])
            if m is None:
                r.modules[f["module"]] = _Mod(f["module"], f["title"], self.clock())
            else:
                m.title, m.t0 = f["title"], self.clock()     # a pom module, created by the reactor event
            self.module_rows[f["module"]] = r
        return []

    def _on_reactor(self, f):
        """Sent at the first line (pom-tree size) and again at the first Building line (its N).  Pom modules get their
        dim box at once."""
        r = self._adopt(f["step"])
        if r is not None:
            r.size = f["size"]
            r.poms = set(f["poms"])
            r.tests_mode = f["tests"]
            for pom in f["poms"]:
                _, m = self._mod({"step": r.id, "module": pom}, create=True)
                self._grant(r, m, "skip")
        return []

    def _on_class_done(self, f):
        _, m = self._mod(f, create=True)
        if m is not None:
            m.outcomes.append(f["outcome"])
        return []

    def _on_module_compiled(self, f):
        r, m = self._mod(f, create=True)
        if m is None:
            return []
        if not f["ok"]:
            self._grant(r, m, "fail")
        elif m.id in r.poms:
            self._grant(r, m, "skip")
        elif not r.tests_mode and m.box is None:
            self._grant(r, m, "pass")
        return []

    def _on_module_tested(self, f):
        r, m = self._mod(f, create=True)
        if m is not None and m.box != "fail":
            self._grant(r, m, "fail" if "fail" in m.outcomes else "pass" if m.outcomes else "skip")
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
        if failed:
            self._grant(r, m, "fail")
        elif m.box is None:
            self._grant(r, m, "skip" if m.id in r.poms or m.status == "skip" else "pass")
        has_tests = bool(m.totals and m.totals[0])
        return [("module", r, m)] if shows_module(self.detail, failed, has_tests) else []

    def _on_step_end(self, f):
        if f["id"] not in self.rows and f["id"] in self.module_rows:
            if f["id"] in self.aliases:
                self.module_rows[f["id"]].quiet = 0     # a sub-row's tool exited: its parent row is not silent
            return self._on_module_end({"step": self.module_rows[f["id"]].id, "module": f["id"],
                                        "status": f["status"], "ms": f["ms"], "totals": f.get("totals")})
        if f["id"] not in self.rows and f["id"] in self.aliases:
            r = self._target(f["id"])   # a sub-row whose tool took over the parent's bar; the parent ends later
            if r is not None:
                r.quiet = 0     # the tool exited: nothing is silent any more
                if f.get("totals"):
                    r.totals = _totals(f["totals"])
            return []
        r = self.rows.get(f["id"])
        if r is None:
            return []
        r.status, r.ms = f["status"], f["ms"]
        r.quiet = 0     # the tool exited while silent: no quiet(0) follows
        if f.get("summary") is not None:
            r.summary = f["summary"]
        if f.get("totals"):
            r.totals = _totals(f["totals"])
        for m in r.modules.values():   # never ended: it ran nothing we saw (close_children ends the open ones)
            if m.box is None:
                self._grant(r, m, "fail" if "fail" in m.outcomes else "pass" if m.outcomes else "skip")
        if r.status == "fail" and self.failed_title is None:
            self.failed_title = r.label
        notes = []
        for level, text in r.notes:
            kept = [line for line in text.splitlines() if line.strip() not in self.failure_lines]
            if kept:
                notes.append(("text", f"  {_NOTE_PREFIX[level]} " + "\n    ".join(kept)))
        return [("row", r)] + notes

    def _on_note(self, f):
        if not shows_note(self.detail, f["level"]):
            return []
        r = self._target(f.get("step")) or self.module_rows.get(f.get("step")) or self._latest_running()
        if r is not None and r.running:
            r.notes.append((f["level"], f["text"]))
            return []
        line = f"{_NOTE_PREFIX[f['level']]} {f['text']}"
        return [("text", "  " + line if r is not None else line)]

    def _on_failure(self, f):
        self.failure_lines.update(line.strip() for line in f["text"].splitlines())
        return [("text", f["text"])]

    def _on_say(self, f):
        return [("text", f["text"])] if shows_note(self.detail, f["level"]) else []

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
        if self.nested:
            return []
        return [("final", f["status"], int((self.clock() - self.t0) * 1000), self.failed_title, self.full_log)]


def row_cells(row, glyphs, now, pending=False):
    """A step row as (cells, rest, elapsed): its bar's cells (None when it has no bar), the count and totals text
    after the bar, and the elapsed time.  Elapsed is empty under a second, so instant rows (Install) show none."""
    cells, parts = None, []
    if row.modules or row.size:
        cells = [glyphs[b] for b in row.boxes()]
        if pending and row.size:
            cells += [PENDING] * max(0, row.size - len(row.order))
        parts.append(f"{len(row.order)}/{row.size}" if row.size else str(len(row.order)))
    text = row.summary if row.summary is not None else fmt_totals(row.totals, row.tests_mode and not row.running)
    if text:
        parts.append(text)
    ms = row.ms if row.ms is not None else int((now - row.t0) * 1000)
    return cells, "  ".join(parts), fmt_elapsed(ms) if ms >= 1000 else ""


def row_parts(row, glyphs, now, pending=False):
    """A step row as (text, elapsed)."""
    cells, rest, elapsed = row_cells(row, glyphs, now, pending)
    return ("".join(cells) + "  " + rest if cells is not None else rest), elapsed


def module_line(mod, glyphs, now):
    ms = mod.ms if mod.ms is not None else int((now - mod.t0) * 1000)
    tests = fmt_totals(mod.totals)
    return f"  {glyphs[mod.box or 'pass']} {mod.title}  {fmt_elapsed(ms)}" + (f"  {tests}" if tests else "")


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
                self._write("  ".join(p for p in (block[1].label.ljust(board.width()), text, elapsed) if p) + "\n")
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
        k = sum(1 for r in running for m in r.modules.values() if m.in_progress)
        self._write(f"… {total:,} tests, {failed} failed · {k} modules running  "
                    f"{fmt_elapsed((now - board.t0) * 1000)}\n")

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


def _fit(left, right, cols):
    """left, then right flush with column cols; left is cut with an ellipsis when both don't fit."""
    room = cols - (len(right) + 2 if right else 0)
    if _vlen(left) > room:
        left = _vcut(left, max(0, room - 1)) + "…"
    return left + " " * max(2, cols - _vlen(left) - len(right)) + right if right else left


def _fit_row(label, cells, rest, elapsed, cols):
    """A step row fitted to cols.  When it is too wide the bar gives way first, keeping its newest cells behind an
    ellipsis, so the label, count, totals and elapsed survive; only a row too wide even then is cut from the right."""
    line = label + ("  " + "".join(cells) + "  " + rest if cells is not None else "  " + rest if rest else "")
    if cells is None or _vlen(line) <= cols - (len(elapsed) + 2 if elapsed else 0):
        return _fit(line, elapsed, cols)
    room = cols - (len(elapsed) + 2 if elapsed else 0) - _vlen(label + "    " + rest)
    if room < 2:
        return _fit(line, elapsed, cols)
    return _fit(label + "  …" + "".join(cells[len(cells) - (room - 1):]) + "  " + rest, elapsed, cols)


# Known limitations: narrowing a reflowing terminal can leave stale fragments above the region, because the cursor-up
# count is the number of lines drawn at the old width and the terminal has rewrapped them into more.
class LiveRenderer:
    """Finished blocks print permanently; running rows and their modules redraw in place below them, at most once per
    REDRAW_SECONDS, and never taller than the terminal's rows minus 2."""

    REDRAW_SECONDS = 0.1
    MAX_SUB_LINES = 3

    def __init__(self, write, size=shutil.get_terminal_size, clock=time.monotonic, color=True):
        self.write = write
        self.size = size
        self.clock = clock
        self.color = color
        self.glyphs = COLOR_GLYPHS if color else NO_COLOR_GLYPHS
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
            elif kind == "module":
                lines.append(module_line(block[2], self.glyphs, self.clock()))
            elif kind == "final":
                lines.append(format_final(*block[1:]))
        self._flush(lines)
        self.flushed = len(blocks)
        self._draw(force=bool(blocks))

    def _row(self, board, r, cols, pending=False):
        cells, rest, elapsed = row_cells(r, self.glyphs, self.clock(), pending)
        if pending and r.quiet:
            rest += f" · quiet {r.quiet}s"
        return _fit_row(r.label.ljust(board.width()), cells, rest, elapsed, cols)

    def _flush(self, lines):
        if lines:
            self._clear()
            if self.prompting:
                self.write("\n")           # end the prompt's partial line first; the prompt is no longer on it
                self.prompting = False
            self.write("".join(line + "\n" for line in lines))

    def tick(self, board):
        self.board = board
        self._draw()

    def region(self, board, cols, max_lines):
        running = board.running()
        subs = [(r, m) for r in running for m in r.modules.values() if m.in_progress]
        blocks = {id(m): self._sub_lines(m, cols) for _, m in subs}
        budget = max_lines - len(running)
        folded = {}
        while subs and sum(len(blocks[id(m)]) for _, m in subs) + len(folded) > budget:
            r, _ = subs.pop(0)
            folded[r.id] = folded.get(r.id, 0) + 1
        lines = []
        for r in running:
            lines.append(self._row(board, r, cols, pending=True))
            if r.id in folded:
                lines.append(f"  +{folded[r.id]} more running")
            lines += [line for owner, m in subs if owner is r for line in blocks[id(m)]]
        return lines[:max(1, max_lines)]

    def _sub_lines(self, m, cols):
        prefix = f"  {_vcut(m.title, max(1, cols // 2))}  "
        per = max(1, cols - _vlen(prefix))
        cells = [self.glyphs[o] for o in m.outcomes]
        cap = per * self.MAX_SUB_LINES
        if len(cells) > cap:
            width = len(f"… +{len(cells)} ")
            keep = max(1, cap - width)
            cells = list(f"… +{len(cells) - keep} ".ljust(width)) + cells[-keep:]
        chunks = [cells[i:i + per] for i in range(0, len(cells), per)] or [[]]
        return [(prefix if i == 0 else " " * _vlen(prefix)) + "".join(c) for i, c in enumerate(chunks)]

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
        out += [line + "\x1b[K\n" for line in lines]
        out.append("\x1b[J")
        self.write("".join(out))
        self.height = len(lines)
        self.last_draw = now

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
                 nested=False):
        self.sinks = sinks or Sinks()
        self._stream = stream
        self.clock = clock
        self.size = size
        self.board = Board(resolve_detail(detail), nested=nested, full_log=self.sinks.full_path, clock=clock)
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
        return self._stream if self._stream is not None else sys.stdout

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
        if self.stream is sys.stdout and text:
            _seen(text.endswith("\n"))

    def _log_write(self, text):
        self._log.write(text)
        self._log.flush()

    def __call__(self, kind, fields):
        owned = not self.stopped    # say() and failure() left this event's text to the view
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


def session(title, header=(), *, detail=None, sinks=None):
    """Start this process's console view and return its Console.  Inside another session (JUNEAU_RUN_SESSION set)
    it is nested: no header and no final line, so a child script's rows join its parent's view."""
    global _console
    if _console is not None:
        return _console
    nested = os.environ.get(SESSION_ENV) == "1"
    sinks = sinks or Sinks()
    _console = subscribe(Console(sinks, detail, nested=nested))
    os.environ[SESSION_ENV] = "1"   # only once the console is up, so a failed start leaves children un-nested
    atexit.register(_end_session)
    try:
        if signal.getsignal(signal.SIGTERM) == signal.SIG_DFL:
            signal.signal(signal.SIGTERM, _sigterm)
    except ValueError:  # not the main thread
        pass
    lines = list(header) + ([f"full log: {sinks.full_path}"] if sinks.full_path is not None else [])
    publish("session", title=title, header=lines, nested=nested)
    return _console


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
            proc.stdin.write(text)
        except BrokenPipeError:     # the pager was quit before it read everything
            pass
        try:
            proc.stdin.close()
        except BrokenPipeError:
            pass
        proc.wait()
    finally:
        if previous is not None:
            signal.signal(signal.SIGINT, previous)
    return True


def passthrough(cmd, step_id, title, *, n=None, parent=None, cwd=None, env=None, label=None):
    """Run an interactive command on the real terminal, as a step whose row shows its outcome: no capture, no new
    session, nothing copied to the full log.  Returns Result(exit, ms)."""
    emit("step", id=step_id, n=n if n is not None else 1, title=title, parent=parent)
    publish("step_start", id=step_id, n=n if n is not None else 1, title=title, parent=parent,
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
    so its rows line up with ours (label width, detail level); empty with no session."""
    if _console is None:
        yield {}
        return
    with _paused():
        yield {LABEL_WIDTH_ENV: str(_console.board.width()), DETAIL_ENV: _console.board.detail}


# ---------------------------------------------------------------------------------------------------------------------
# Sinks
# ---------------------------------------------------------------------------------------------------------------------

CONSOLE_MODES = ("full", "condensed", "none")


def warn(text):
    """Wrapper diagnostics always go to stderr, never into the tool's output stream."""
    print(f"juneau-run: {text}", file=sys.stderr, flush=True)


class Sinks:
    """Where the full and condensed streams go.  Arguments beat JUNEAU_RUN_* environment variables."""

    def __init__(self, console=None, full_log=None, condensed_log=None):
        self.console = console or os.environ.get("JUNEAU_RUN_CONSOLE") or "full"
        if self.console not in CONSOLE_MODES:
            raise ValueError(f"console must be one of {CONSOLE_MODES}, not {self.console!r}")
        self.full_path = self._resolve(full_log or os.environ.get("JUNEAU_RUN_FULL_LOG"))
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
            _out(text + "\n")


# ---------------------------------------------------------------------------------------------------------------------
# Parsers
#
# A parser is pure: it turns lines into events and never writes output.  Event tuples:
#   ("sub_start", id, n, title)          ("sub_end", id, status, ms)
#   ("tests", id, total, fail, err, skip) ("report", id, kind, path)
#   ("note", level, text, id)            ("cond", text)
#   ("class_done", id, outcome)          ("compiled", id, ok)          ("tested", id)
#   ("reactor", size, [pom sub ids], tests_mode)                    ("failure", text, id)
# ---------------------------------------------------------------------------------------------------------------------

ANSI_RE = re.compile(r"\x1b\[[0-9;?]*[A-Za-z]|\x1b\][^\x07\x1b]*(?:\x07|\x1b\\)")


def strip_ansi(text):
    return ANSI_RE.sub("", text)


class Parser:
    """Base parser: no sub-steps, no counts.  step_id is set by the runner before prepare()."""

    name = "base"

    def __init__(self):
        self.step_id = None

    def prepare(self, cmd, env, cwd):
        """May adjust the command or environment; called only in piped mode."""
        return cmd, env

    def on_line(self, line):
        return self.handle(strip_ansi(line))

    def handle(self, line):
        return []

    def finish(self, exit_code):
        return []


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
    ANY_PLUGIN = re.compile(r"^\[INFO\] --- (\S+?):(\S+):(\S+) \(.*\) @ (\S+) ---")
    COMPILE_PATH = re.compile(r"^\[ERROR\] (\S+\.java):\[")
    SKIP_TESTS = re.compile(r"(?:^|\s)-D(?:skipTests|maven\.test\.skip)(?:=true)?(?=\s|$)")
    FILE_OPTION = re.compile(r"(?:^|\s)(?:-f|--file)(?:=|\s+)(\S+)")
    CLASS_LINE = re.compile(
        r"^(?:\[(?:INFO|WARNING|ERROR)\] )?Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+), "
        r"Time elapsed: [\d.,]+ s(?: <<< (?:FAILURE|ERROR)!)? -- in (\S+)\s*$")
    FAILED_TEST = re.compile(r"^(?:\[(?:ERROR|WARNING)\] )?\S.+ <<< (?:FAILURE|ERROR)!\s*$")
    COMPILE_ERROR = re.compile(r"^\[ERROR\] \S+\.java:\[\d+,\d+\] .+")
    COMPILE_GOAL_FAILED = re.compile(r"^\[ERROR\] Failed to execute goal \S+:maven-compiler-plugin:\S+ \(.*\) on project ([^\s:]+)")
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
        self.class_cache = {}            # test class FQN -> artifactIds of the modules containing it
        self.packaging = {}              # artifactId -> packaging
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
        args = " ".join(cmd[1:])
        root = self.cwd
        m = self.FILE_OPTION.search(args)
        if m:
            pom = self.cwd / m.group(1)
            root = pom.parent if pom.suffix == ".xml" else pom
        self.packaging = {}
        self.dirs, self.names = read_pom_tree(root, self.packaging)
        self.parallel = any(re.search(r"(?:^|\s)(?:-T|--threads)", a) for a in cmd[1:])
        self.tests_mode = not self.SKIP_TESTS.search(args)
        return cmd, env

    def _reactor(self, size):
        poms = [f"{self.step_id}/{a}" for a, p in self.packaging.items() if p == "pom"]
        return ("reactor", size, poms, self.tests_mode)

    def _has_tests(self, artifact):
        d = self.dirs.get(artifact)
        return d is None or (d / "src" / "test" / "java").is_dir()

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
            return events
        m = self.CLASS_LINE.match(line)
        if m:
            t, f, e, s = (int(m.group(i)) for i in range(1, 5))
            self.total += t
            self.fail += f
            self.err += e
            self.skip += s
            events.append(("tests", self.step_id, self.total, self.fail, self.err, self.skip))
            sub = self._attribute(m.group(5))
            if sub is not None:
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


class PytestParser(Parser):
    """pytest: junitxml report plus counts from the final summary line."""

    name = "pytest"
    SUMMARY = re.compile(r"^(?:=+ )?((?:\d+ [a-z]+(?:, )?)+) in [\d.]+s(?: \(\d+:\d+:\d+\))?(?: =+)?\s*$")
    ITEM = re.compile(r"(\d+) (passed|failed|skipped|errors?|xfailed|xpassed|deselected|warnings?)")
    SHORT = re.compile(r"^=+ short test summary info =+\s*$")

    def __init__(self):
        super().__init__()
        self.report_path = None
        self.in_short = False
        self.counts = None

    def prepare(self, cmd, env, cwd):
        existing = next((a for a in cmd if a.startswith("--junitxml")), None)
        if existing is not None:
            if "=" in existing:
                self.report_path = existing.split("=", 1)[1]
            return cmd, env
        if enabled():
            directory = artifacts_dir() or Path(tempfile.mkdtemp(prefix="juneau-run-"))
            self.report_path = str(directory / f"{self.step_id.replace('/', '-')}-pytest.xml")
            cmd = list(cmd) + [f"--junitxml={self.report_path}"]
        return cmd, env

    def handle(self, line):
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


PARSERS = {
    "maven": MavenParser,
    "pytest": PytestParser,
    "playwright": PlaywrightParser,
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
    """Calls parser methods, disabling the parser after its first exception."""

    def __init__(self, parser):
        self.parser = parser
        self.disabled = False

    def call(self, method, *args):
        if self.disabled:
            return []
        try:
            return getattr(self.parser, method)(*args) or []
        except Exception as e:  # parsers are third-party code; the tool's output must keep flowing
            self.disabled = True
            warn(f"parser {getattr(self.parser, 'name', '?')} disabled after error: {e!r}")
            return []


class _Dispatcher:
    """Turns parser events into markers and condensed lines for one step."""

    def __init__(self, step_id, sinks):
        self.step_id = step_id
        self.sinks = sinks
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
                emit("report", step=target, kind=rkind, path=path)
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
                publish("reactor", step=self.step_id, size=ev[1], poms=list(ev[2]), tests=ev[3])
            elif kind == "failure":
                publish("failure", text=ev[1], step=ev[2])
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


def _resolve_parser(parser):
    if isinstance(parser, str):
        return PARSERS[parser]()
    return parser() if isinstance(parser, type) else parser


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


def run_tool(cmd, parser, step_id, title, *, n=None, parent=None, cwd=None, env=None, sinks=None, capture=False,
             tty=False, watchdog=None, summarize=None, label=None):
    """Run one tool as one step and return Result(exit, ms, summary).

    Pass-through mode (markers off, default sinks, no capture or summarize) hands the child the real terminal and no
    parser runs.  Piped mode merges stdout and stderr, feeds the parser and honours the sinks; with tty=True the
    child runs under a PTY and our keystrokes are forwarded to it.  GPG_TTY then names the PTY, so a terminal
    pinentry prompts through it (with echo off) instead of racing us for the real terminal.  watchdog=<s> publishes a
    silent partial line as a prompt, or the silence as quiet.  summarize(text) becomes the step_end summary.
    SIGINT/SIGTERM unwind as a KeyboardInterrupt; the except block then takes down the child's whole process group,
    `end` is emitted with exit 130 and the KeyboardInterrupt is raised again.  A tool that cannot be started ends its
    step with exit 127 and the OSError is raised again.  Any other error ends the step as failed and is raised again.
    """
    sinks = sinks or Sinks()
    t0 = time.monotonic()
    labelled = {"label": label} if label else {}
    if not enabled() and sinks.is_default and not capture and summarize is None:
        publish("step_start", id=step_id, n=n if n is not None else 1, title=title, parent=parent, **labelled)
        rc = 130
        try:
            rc = subprocess.run(cmd, cwd=cwd, env=env).returncode
        except OSError:
            rc = 127   # cannot start: as run_pty reports it, not as a Ctrl-C
            raise
        finally:
            ms = int((time.monotonic() - t0) * 1000)
            publish("step_end", id=step_id, status="ok" if rc == 0 else "fail", ms=ms,
                    exit=None if rc == 0 else rc, summary=None, totals=None)
        return Result(rc, ms)

    parser = _resolve_parser(parser)
    parser.step_id = step_id
    guard = _Isolated(parser)
    try:
        cmd, env = parser.prepare(cmd, env, cwd)
    except Exception as e:
        warn(f"parser {getattr(parser, 'name', '?')} prepare failed ({e!r}); using the original command")
    dispatch = _Dispatcher(step_id, sinks)
    emit("step", id=step_id, n=n if n is not None else 1, title=title, parent=parent)
    publish("step_start", id=step_id, n=n if n is not None else 1, title=title, parent=parent, **labelled)
    sinks.open()
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
                                    start_new_session=True)
            fd = proc.stdout.fileno()
    except OSError:
        # Cannot start: end the step (as run_pty does) so its row is not left running, then let the caller see why.
        if master is not None:
            os.close(master)
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

    def feed(text):
        nonlocal pending
        pending += text
        while "\n" in pending:
            raw, pending = pending.split("\n", 1)
            line = strip_ansi(raw) if tty else raw
            line = line.rstrip("\r").rsplit("\r", 1)[-1]
            dispatch.apply(guard.call("on_line", line))

    def take(chunk):
        sinks.write_full(chunk)
        text = decoder.decode(chunk)
        if captured is not None:
            captured.append(text)
        watch.output(text)
        feed(text)

    def read(source):
        """One chunk, or b"" at end of output.  A PTY master reports the slave side closing as EIO."""
        try:
            return os.read(source, 65536)
        except OSError as e:
            if e.errno != errno.EIO:
                raise
            return b""

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
    status = "ok" if proc.returncode == 0 else "fail"
    dispatch.close_children(status)
    dispatch.flush_tests(step_id)
    emit("end", id=step_id, status=status, ms=ms, exit=None if proc.returncode == 0 else proc.returncode)
    text = "".join(captured) if captured is not None else None
    if text is not None and tty:
        text = text.replace("\r\n", "\n")    # joined first: a CRLF split across two reads still collapses
    summary = None
    if summarize is not None:
        try:
            summary = summarize(text)
        except Exception as e:  # a summary is display only; it never fails the step
            warn(f"summary for {step_id} failed: {e!r}")
    publish("step_end", id=step_id, status=status, ms=ms, exit=None if proc.returncode == 0 else proc.returncode,
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
    """In the child, after setsid: make the PTY slave (stdin) the controlling terminal."""
    try:
        fcntl.ioctl(0, termios.TIOCSCTTY, 0)
    except OSError:
        pass


def run_pty(cmd, parser, step_id, title, *, cols, rows, sinks, cwd=None, env=None):
    """Run one tool under a cols x rows PTY as one step and return Result(exit, ms, summary).

    The tool's bytes go to the full log exactly as written (the PTY turns \\n into \\r\\n).  Each line is cleaned with
    pty_clean() before the parser sees it, and _line_offset holds the log offset of its start while its events are
    written.  A signal death exits 128+n with a warn note.  SIGINT/SIGTERM cancel as in run_tool().
    """
    global _line_offset
    t0 = time.monotonic()
    parser = _resolve_parser(parser)
    parser.step_id = step_id
    guard = _Isolated(parser)
    env = dict(os.environ if env is None else env)
    env.pop("RUN_MARKERS", None)
    env.pop(ACTIVE_ENV, None)
    env["TERM"] = "xterm-256color"
    try:
        cmd, env = parser.prepare(cmd, env, cwd)
    except Exception as e:
        warn(f"parser {getattr(parser, 'name', '?')} prepare failed ({e!r}); using the original command")
    dispatch = _Dispatcher(step_id, sinks)
    sinks.open()
    base = sinks.full_path.stat().st_size
    _line_offset = base
    emit("step", id=step_id, n=1, title=title)

    master, slave = os.openpty()
    fcntl.ioctl(slave, termios.TIOCSWINSZ, struct.pack("HHHH", rows, cols, 0, 0))
    try:
        proc = subprocess.Popen(cmd, cwd=cwd, env=env, stdin=slave, stdout=slave, stderr=slave,
                                start_new_session=True, preexec_fn=_set_ctty, close_fds=True)
    except OSError as e:
        os.close(master)
        os.close(slave)
        sinks.close()
        warn(f"cannot start {cmd[0]}: {e}")
        ms = int((time.monotonic() - t0) * 1000)
        emit("end", id=step_id, status="fail", ms=ms, exit=127)
        return Result(127, ms)
    os.close(slave)
    cancelled = []

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

    def drain(fd):
        """Reads whatever is still buffered on the PTY master without blocking, until it is empty or reports EIO."""
        while select.select([fd], [], [], 0)[0]:
            try:
                chunk = os.read(fd, 65536)
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
                    ready, _, _ = select.select([master], [], [], 0.1)
                    if not ready:
                        if proc.poll() is not None:
                            drain(master)   # the child may have written its last bytes after select() timed out
                            break
                        continue
                    chunk = os.read(master, 65536)
                except InterruptedError:
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
            _terminate_group(proc)
    finally:
        if proc.poll() is None:   # an unexpected error escaped the read loop: do not leave the child running
            try:
                os.killpg(proc.pid, signal.SIGKILL)
            except OSError:
                proc.kill()
            proc.wait()
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
                             cols=cols, rows=rows, sinks=sinks)
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


def _git(*args):
    try:
        out = subprocess.run(["git", *args], capture_output=True, text=True, check=True).stdout.strip()
        return out or None
    except Exception:
        return None


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
    ap.add_argument("tool", choices=sorted(PARSERS))
    ap.add_argument("cmd", nargs=argparse.REMAINDER)
    ns = ap.parse_args(argv)
    cmd = ns.cmd[1:] if ns.cmd[:1] == ["--"] else ns.cmd
    if not cmd:
        ap.error("no command given after --")
    if ns.pty or ns.events or ns.size:
        return _main_pty(ap, ns, cmd)
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
