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
unless the environment has RUN_MARKERS=1.  Sections, in order: markers, sinks, parsers, runner, CLI.

CLI:  python3 juneau_run.py [--console full|condensed|none] [--full-log PATH] [--condensed-log PATH]
                            [--step ID] [--title T] <maven|pytest|playwright|generic> -- <cmd...>
"""

import argparse
import codecs
import collections
import json
import os
import re
import signal
import subprocess
import sys
import tempfile
import time
import xml.etree.ElementTree as ET
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


def enabled():
    """True only when RUN_MARKERS is exactly "1"."""
    return os.environ.get("RUN_MARKERS") == "1"


def emit(ev, **fields):
    """Print one marker line: `##run {"ev":...}`.  None-valued fields are omitted; oversized markers are dropped."""
    if not enabled():
        return
    obj = {"ev": ev, **{k: v for k, v in fields.items() if v is not None}}
    line = PREFIX + json.dumps(obj, ensure_ascii=False, separators=(",", ":"))
    if len(line.encode("utf-8")) > MAX_BYTES:
        return
    print(line, flush=True)


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
    """Mutable outcome of a step() block; defaults to ok."""

    def __init__(self):
        self.status = "ok"
        self.exit = None

    def fail(self, exit=None):  # NOSONAR python:S5806 - parameter names mirror protocol field names
        self.status = "fail"
        self.exit = exit

    def skip(self):
        self.status = "skip"


@contextmanager
def step(id, n, title, *, parent=None):  # NOSONAR python:S5806 - parameter names mirror protocol field names
    """Emit `step`, run the block, then emit `end` with its status and duration.  An exception ends it as fail and propagates."""
    emit("step", id=id, n=n, title=title, parent=parent)
    s = Step()
    t0 = time.monotonic()
    try:
        yield s
    except BaseException:
        s.status = "fail"
        raise
    finally:
        emit("end", id=id, status=s.status, ms=int((time.monotonic() - t0) * 1000), exit=s.exit)


def report(step, kind, path):
    """Point the runner at a step's test results (kind: surefire, junitxml, jest-json, playwright-json)."""
    emit("report", step=step, kind=kind, path=str(path))


def note(level, text, href=None, step=None):
    """A notable event (level: info, warn, error).  A non-http(s) href is dropped."""
    if href is not None and not href.startswith(("http://", "https://")):
        href = None
    emit("note", level=level, text=text, href=href, step=step)


def tests(step, total, fail, err, skip):
    """Absolute cumulative test totals for a step; the latest event for a step supersedes earlier ones."""
    emit("tests", step=step, total=total, fail=fail, err=err, skip=skip)


def done(status, commit=None):
    """Last marker of a run (status: ok, fail, cancelled).  A no-op inside a process that inherited an active run."""
    if _nested():
        return
    emit("done", status=status, commit=commit)


def artifacts_dir():
    """$RUN_ARTIFACTS as a Path, or None when unset."""
    value = os.environ.get("RUN_ARTIFACTS")
    return Path(value) if value else None


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

    def write_condensed(self, text):
        """text is one condensed line without its newline."""
        if self._condensed is not None:
            self._condensed.write((text + "\n").encode("utf-8"))
            self._condensed.flush()
        if self.console == "condensed":
            print(text, flush=True)


# ---------------------------------------------------------------------------------------------------------------------
# Parsers
#
# A parser is pure: it turns lines into events and never writes output.  Event tuples:
#   ("sub_start", id, n, title)          ("sub_end", id, status, ms)
#   ("tests", id, total, fail, err, skip) ("report", id, kind, path)
#   ("note", level, text, id)            ("cond", text)
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
        if exit_code == 0:
            return []
        return [("cond", line) for line in self.tail]


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


def read_pom_tree(root):
    """Walk pom.xml and its <modules> recursively.  Returns ({artifactId: dir}, {name: artifactId})."""
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
        modules = []
        for child in top:
            tag = local(child.tag)
            if tag == "artifactId":
                artifact = (child.text or "").strip()
            elif tag == "name":
                name = (child.text or "").strip()
            elif tag == "modules":
                modules = [(m.text or "").strip() for m in child if local(m.tag) == "module"]
        if artifact:
            dirs[artifact] = d
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
    PLUGIN = re.compile(r"^\[INFO\] --- (?:maven-)?(surefire|failsafe)(?:-plugin)?:\S+ \(.*\) @ (\S+) ---")
    CLASS_LINE = re.compile(
        r"^(?:\[(?:INFO|WARNING|ERROR)\] )?Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+), "
        r"Time elapsed: [\d.,]+ s(?: <<< (?:FAILURE|ERROR)!)? -- in (\S+)\s*$")
    FAILED_TEST = re.compile(r"^(?:\[(?:ERROR|WARNING)\] )?\S.+ <<< (?:FAILURE|ERROR)!\s*$")
    COMPILE_ERROR = re.compile(r"^\[ERROR\] \S+\.java:\[\d+,\d+\] .+")
    STATUS = {"SUCCESS": "ok", "FAILURE": "fail", "SKIPPED": "skip"}
    FAILURE_TRACE_LINES = 10

    def __init__(self):
        super().__init__()
        self.dirs, self.names = {}, {}
        self.parallel = False
        self.cwd = None
        self.total = self.fail = self.err = self.skip = 0
        self.mod = {}                    # sub id -> [total, fail, err, skip]
        self.open = {}                   # sub id -> (name, start time)
        self.ended = set()
        self.titles = {}                 # sub id -> name
        self.sub_of = {}                 # display name -> sub id, so the Reactor Summary finds what Building opened
        self.header_artifact = None      # artifactId of the nearest preceding `< g:a >` header
        self.surefire_sub = None
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
        self.cwd = Path(cwd) if cwd else Path.cwd()
        self.dirs, self.names = read_pom_tree(self.cwd)
        # A "sh -c" command is one string, so look for the option at a word boundary rather than as a whole argument.
        self.parallel = any(re.search(r"(?:^|\s)(?:-T|--threads)", a) for a in cmd[1:])
        return cmd, env

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

    def _xml_totals(self, directory):
        total = fail = err = skip = 0
        found = False
        for f in sorted(Path(directory).glob("TEST-*.xml")):
            try:
                root = ET.parse(f).getroot()
            except ET.ParseError:
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
        name = self.titles.get(sub, self._artifact_of(sub))
        module_dir = self.dirs.get(self._artifact_of(sub))
        if status == "skip":
            pass
        elif module_dir is None:
            events.append(("note", "info", f"Test reports for module {name} were not located", sub))
        else:
            for kind, dirname in (("surefire", "surefire-reports"), ("failsafe", "failsafe-reports")):
                reports = module_dir / "target" / dirname
                if reports.is_dir():
                    events.append(("report", sub, "surefire", str(reports)))
                    exact = self._xml_totals(reports)
                    if exact is not None and kind == "surefire":
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
        m = self.HEADER.match(line)
        if m:
            self.header_artifact = m.group(2)
            return events
        m = self.BUILDING.match(line)
        if m:
            self.structure_seen = True
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
        m = self.PLUGIN.match(line)
        if m:
            self.surefire_sub = f"{self.step_id}/{m.group(2)}"
            return events
        m = self.CLASS_LINE.match(line)
        if m:
            t, f, e, s = (int(m.group(i)) for i in range(1, 5))
            self.total += t
            self.fail += f
            self.err += e
            self.skip += s
            events.append(("tests", self.step_id, self.total, self.fail, self.err, self.skip))
            sub = self.surefire_sub
            if sub in self.open:
                c = self._counts(sub)
                c[0] += t
                c[1] += f
                c[2] += e
                c[3] += s
                events.append(("tests", sub, *c))
            return events
        if self.FAILED_TEST.match(line):
            if not self.failure_trace:
                self.failure_trace = [line]
                self.trace_left = self.FAILURE_TRACE_LINES
            events.append(("cond", line))
            self.cond_window = self.FAILURE_TRACE_LINES
            return events
        if self.cond_window > 0:
            self.cond_window -= 1
            events.append(("cond", line))
            if self.trace_left > 0 and self.failure_trace:
                self.failure_trace.append(line)
                self.trace_left -= 1
            return events
        if self.COMPILE_ERROR.match(line) and self.first_error is None:
            self.first_error = line
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
        return [("note", "error", text, self.step_id)]

    def finish(self, exit_code):
        events = []
        for sub in list(self.open):   # a module whose Reactor Summary row never arrived still gets its end
            events += self._end_module(sub, "skip", time.monotonic() - self.open[sub][1])
        if self.total:
            events.append(("tests", self.step_id, self.total, self.fail, self.err, self.skip))
        if not self.structure_seen and exit_code != 0:
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

    def apply(self, events):
        for ev in events:
            kind = ev[0]
            if kind == "sub_start":
                _, sub, n, title = ev
                self.open[sub] = time.monotonic()
                emit("step", id=sub, n=n, title=title, parent=self.step_id)
            elif kind == "sub_end":
                _, sub, status, ms = ev
                self.flush_tests(sub)
                self.open.pop(sub, None)
                emit("end", id=sub, status=status, ms=ms)
            elif kind == "tests":
                _, target, total, fail, err, skip = ev
                if target == self.step_id:
                    self.summary = {"total": total, "fail": fail, "err": err, "skip": skip}
                self.pending[target] = (total, fail, err, skip)
                if time.monotonic() - self.last_sent.get(target, -1e9) >= TESTS_INTERVAL:
                    self.flush_tests(target)
            elif kind == "report":
                _, target, rkind, path = ev
                emit("report", step=target, kind=rkind, path=path)
            elif kind == "note":
                _, level, text, target = ev
                note(level, text, step=target)
            elif kind == "cond":
                self.sinks.write_condensed(ev[1])

    def flush_tests(self, target):
        totals = self.pending.pop(target, None)
        if totals is not None:
            self.last_sent[target] = time.monotonic()
            tests(target, *totals)

    def close_children(self, status):
        """Producer rule: every open child gets its `end`, children first, before the parent's."""
        for sub in reversed(list(self.open)):
            started = self.open[sub]
            self.flush_tests(sub)
            emit("end", id=sub, status=status, ms=int((time.monotonic() - started) * 1000))
        self.open.clear()
        for target in list(self.pending):
            self.flush_tests(target)


def _resolve_parser(parser):
    if isinstance(parser, str):
        return PARSERS[parser]()
    return parser() if isinstance(parser, type) else parser


def run_tool(cmd, parser, step_id, title, *, n=None, parent=None, cwd=None, env=None, sinks=None, capture=False):
    """Run one tool as one step and return Result(exit, ms, summary).

    Pass-through mode (markers off, default sinks, no capture) hands the child the real terminal and no parser runs.
    Piped mode merges stdout and stderr, feeds the parser and honours the sinks.  SIGINT/SIGTERM unwind as a
    KeyboardInterrupt; the except block then takes down the child's whole process group, `end` is emitted with exit
    130 and the KeyboardInterrupt is raised again.
    """
    sinks = sinks or Sinks()
    t0 = time.monotonic()
    if not enabled() and sinks.is_default and not capture:
        proc = subprocess.run(cmd, cwd=cwd, env=env)
        return Result(proc.returncode, int((time.monotonic() - t0) * 1000))

    parser = _resolve_parser(parser)
    parser.step_id = step_id
    guard = _Isolated(parser)
    try:
        cmd, env = parser.prepare(cmd, env, cwd)
    except Exception as e:
        warn(f"parser {getattr(parser, 'name', '?')} prepare failed ({e!r}); using the original command")
    dispatch = _Dispatcher(step_id, sinks)
    emit("step", id=step_id, n=n if n is not None else 1, title=title, parent=parent)
    sinks.open()
    proc = subprocess.Popen(cmd, cwd=cwd, env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                            start_new_session=True)
    cancelled = []

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

    decoder = codecs.getincrementaldecoder("utf-8")(errors="replace")
    pending = ""
    captured = [] if capture else None

    def feed(text):
        nonlocal pending
        pending += text
        while "\n" in pending:
            raw, pending = pending.split("\n", 1)
            line = raw.rstrip("\r").rsplit("\r", 1)[-1]
            dispatch.apply(guard.call("on_line", line))

    try:
        try:
            fd = proc.stdout.fileno()
            while True:
                try:
                    chunk = os.read(fd, 65536)
                except InterruptedError:
                    continue
                if not chunk:
                    break
                sinks.write_full(chunk)
                text = decoder.decode(chunk)
                if captured is not None:
                    captured.append(text)
                feed(text)
            proc.wait()
            if pending:
                feed("\n")
            dispatch.apply(guard.call("finish", proc.returncode))
        except KeyboardInterrupt:
            if not cancelled:
                cancelled.append(signal.SIGINT)
            for sig in previous:   # no re-entry while the child's group is taken down
                signal.signal(sig, signal.SIG_IGN)
            _terminate_group(proc)
    finally:
        for sig, handler in previous.items():
            signal.signal(sig, handler)
        proc.stdout.close()
        sinks.close()

    ms = int((time.monotonic() - t0) * 1000)
    if cancelled:
        dispatch.close_children("fail")
        emit("end", id=step_id, status="fail", ms=ms, exit=130)
        raise KeyboardInterrupt
    status = "ok" if proc.returncode == 0 else "fail"
    dispatch.close_children(status)
    dispatch.flush_tests(step_id)
    emit("end", id=step_id, status=status, ms=ms, exit=None if proc.returncode == 0 else proc.returncode)
    return Result(proc.returncode, ms, dispatch.summary, "".join(captured) if captured is not None else None)


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
    ap.add_argument("tool", choices=sorted(PARSERS))
    ap.add_argument("cmd", nargs=argparse.REMAINDER)
    ns = ap.parse_args(argv)
    cmd = ns.cmd[1:] if ns.cmd[:1] == ["--"] else ns.cmd
    if not cmd:
        ap.error("no command given after --")
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
