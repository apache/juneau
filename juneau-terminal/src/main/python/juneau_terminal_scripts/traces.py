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
Stack-trace guard: scan a test gate's output for stack traces and fail on any that is not allowlisted.

A trace is a run of two or more stack frames (Java, JavaScript), a `Caused by:` line, or a Python `Traceback`.  scan(text)
finds them, load_allowlist(path) reads the repo's allowlist (one header regex per line plus a mandatory `# reason`), mark()
and Mark.read() give the part of the run's full log written since the mark, and gate() ties them together as one console
row.  A gate with nothing to read ends skipped with a warning, never silently passed.
"""

import contextlib
import dataclasses
import os
import re
import sys
from pathlib import Path

from . import _base

ANSI_RE = re.compile(r"\x1b\[[0-9;?]*[A-Za-z]|\x1b\][^\x07\x1b]*(?:\x07|\x1b\\)")
JAVA_FRAME = re.compile(r"^\s*at [\w.$/<>@-]+\([^()]*\)")
JS_FRAME = re.compile(r"^\s+at (?:async |new )?(?:[^\s()]+(?: \[as [^\]]+\])? \(.+\)|[^\s()]+:\d+:\d+)\s*$")
MORE_LINE = re.compile(r"^\s*\.\.\. \(?\d+ (?:more|common frames omitted)\)?\s*$")
CAUSE_LINE = re.compile(r"^\s*(?:Caused by|Suppressed): ")
PY_TRACEBACK = re.compile(r"^\s*Traceback \(most recent call last\):")
PY_INDENTED = re.compile(r"^\s{2,}\S")
JS_BOUNDARY = re.compile(r"^\s*(?:console\.\w+|(?:PASS|FAIL) \S+)\s*$")
RUNNING = re.compile(r"Running ([\w.$]+)\s*$")
JEST_FILE = re.compile(r"^(?:PASS|FAIL) (\S+)")
PYTEST_ID = re.compile(r"^(\S+\.py::\S+)")

CAUSE_JOIN_LINES = 8     # a frame run starting this soon after a Caused by line belongs to the same trace
JS_JOIN_LINES = 12       # likewise for two JS runs in one console block (an error cause, or a code frame between them)
ATTRIBUTION_LINES = 2000
FULL_LOG_ENV = "JUNEAU_TERMINAL_FULL_LOG"
MAX_HINTS = 5


@dataclasses.dataclass(frozen=True)
class Trace:
    """kind is java, python or js.  line is 1-based in the scanned text.  header is the line an allowlist regex matches.
    context is the log line just above the header (shown, never matched).  test is a best-effort attribution."""
    kind: str
    line: int
    header: str
    frames: int
    context: str = ""
    test: str = ""


def _is_frame(line):
    return bool(JAVA_FRAME.match(line) or JS_FRAME.match(line))


def _is_continuation(line):
    return _is_frame(line) or bool(MORE_LINE.match(line)) or bool(CAUSE_LINE.match(line))


def _header_above(lines, first):
    """The nearest non-blank, non-frame line above lines[first], and the line above that."""
    i = first - 1
    while i >= 0 and not lines[i].strip():
        i -= 1
    if i < 0 or _is_frame(lines[i]):
        return "", ""
    j = i - 1
    while j >= 0 and not lines[j].strip():
        j -= 1
    return lines[i].strip(), (lines[j].strip() if j >= 0 and not _is_frame(lines[j]) else "")


def _python_trace(lines, i):
    j = i + 1
    while j < len(lines) and PY_INDENTED.match(lines[j]):
        j += 1
    header = lines[j].strip() if j < len(lines) and lines[j].strip() else lines[i].strip()
    context = lines[i - 1].strip() if i > 0 else ""
    return Trace("python", i + 1, header, j - i - 1, context), j + 1


def _frame_run(lines, i):
    """The end of the run of frames, continuation lines and joined causes starting at lines[i]."""
    j, last_cause = i, -1
    while j < len(lines):
        line = lines[j]
        if _is_continuation(line):
            if CAUSE_LINE.match(line):
                last_cause = j
            j += 1
        elif last_cause >= 0 and j - last_cause <= CAUSE_JOIN_LINES:
            k = j
            while k < len(lines) and k - last_cause <= CAUSE_JOIN_LINES and not _is_frame(lines[k]):
                k += 1
            if k < len(lines) and _is_frame(lines[k]):
                j = k
            else:
                break
        else:
            break
    return j


def _attribute(lines, index):
    for k in range(index, max(-1, index - ATTRIBUTION_LINES), -1):
        for pattern in (RUNNING, JEST_FILE, PYTEST_ID):
            m = pattern.search(lines[k]) if pattern is RUNNING else pattern.match(lines[k])
            if m:
                return m.group(1)
    return ""


def scan(text):
    """The traces in text, in order.  ANSI is stripped and U+2028 counts as a line break."""
    lines = ANSI_RE.sub("", text).replace("\u2028", "\n").replace("\r\n", "\n").split("\n")
    found, i = [], 0
    while i < len(lines):
        line = lines[i]
        if PY_TRACEBACK.match(line):
            trace, i = _python_trace(lines, i)
            found.append((i, trace))
        elif _is_frame(line):
            end = _frame_run(lines, i)
            frames = sum(1 for k in range(i, end) if _is_frame(lines[k]))
            caused = any(CAUSE_LINE.match(lines[k]) for k in range(i, end))
            if frames >= 2 or caused:
                header, context = _header_above(lines, i)
                kind = "js" if JS_FRAME.match(line) and not JAVA_FRAME.match(line) else "java"
                found.append((i, Trace(kind, i + 1, header, frames, context)))
            i = end
        elif CAUSE_LINE.match(line):
            end = _frame_run(lines, i)
            found.append((i, Trace("java", i + 1, line.strip(), 0, "")))
            i = end
        else:
            i += 1
    return [dataclasses.replace(t, test=_attribute(lines, min(idx, len(lines) - 1))) for idx, t in _merge_js(lines, found)]


def _merge_js(lines, found):
    """Two JS runs in one console block (an error with a cause prints twice) count once."""
    merged = []
    for idx, trace in found:
        if merged and trace.kind == "js" and merged[-1][1].kind == "js":
            prev_idx, prev = merged[-1]
            between = lines[prev_idx + prev.frames:idx]
            if idx - prev_idx <= prev.frames + JS_JOIN_LINES and not any(JS_BOUNDARY.match(b) for b in between):
                merged[-1] = (prev_idx, dataclasses.replace(prev, frames=prev.frames + trace.frames))
                continue
        merged.append((idx, trace))
    return merged


@dataclasses.dataclass(frozen=True)
class Entry:
    pattern: object
    source: str
    reason: str
    line: int


class Allowlist:
    """The parsed allowlist.  errors are strings naming file:line; any error fails the gate."""

    def __init__(self, entries=(), errors=()):
        self.entries = tuple(entries)
        self.errors = tuple(errors)

    def partition(self, found):
        """(allowed, denied, used): the traces split by whether an entry matches their header, and the entries that matched."""
        allowed, denied, used = [], [], set()
        for trace in found:
            hit = next((e for e in self.entries if e.pattern.search(trace.header)), None)
            if hit is None:
                denied.append(trace)
            else:
                allowed.append(trace)
                used.add(hit.line)
        return allowed, denied, used

    def stale(self, used):
        return [e for e in self.entries if e.line not in used]


_ENTRY_LINE = re.compile(r"^(.*\S)\s+#(?:\s+(.*\S))?\s*$")


def load_allowlist(path):
    """Parse the allowlist at path (a missing file is an empty one).  A line is `<regex>  # <reason>`; blank lines and lines
    starting with # are ignored.  The reason is everything after the last whitespace-hash-whitespace, and must not be empty."""
    path = Path(path)
    if not path.is_file():
        return Allowlist()
    entries, errors, seen = [], [], {}
    for number, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        text = raw.strip()
        if not text or text.startswith("#"):
            continue
        where = f"{path.name}:{number}"
        m = _ENTRY_LINE.match(text)
        regex, reason = (m.group(1), m.group(2)) if m else (text, None)
        if not reason:
            errors.append(f"{where}: no `# reason` after the regex")
            continue
        try:
            pattern = re.compile(regex)
        except re.error as e:
            errors.append(f"{where}: bad regex ({e})")
            continue
        if regex in seen:
            errors.append(f"{where}: duplicate of line {seen[regex]}")
            continue
        seen[regex] = number
        entries.append(Entry(pattern, regex, reason, number))
    return Allowlist(entries, errors)


@dataclasses.dataclass(frozen=True)
class Mark:
    """A position in the run's full log.  path is None when the run has no full log."""
    path: object = None
    offset: int = 0

    def read(self):
        """The text written to the log since the mark ("" when there is no log)."""
        if self.path is None:
            return ""
        try:
            with open(self.path, "rb") as f:
                f.seek(self.offset)
                return f.read().decode("utf-8", errors="replace")
        except OSError:
            return ""


def mark():
    """The end of the run's full log ($JUNEAU_TERMINAL_FULL_LOG) as of now."""
    path = os.environ.get(FULL_LOG_ENV)
    if not path:
        return Mark()
    try:
        return Mark(path, os.path.getsize(path))
    except OSError:
        return Mark(path, 0)


class _PlainRow:
    status = "ok"
    summary = None


@contextlib.contextmanager
def _row(id, title, label):
    jr = _base.jr()
    if jr is None:
        yield _PlainRow()
    else:
        with jr.row(id, title, label=label) as r:
            yield r


def _say(level, text, id):
    jr = _base.jr()
    if jr is not None:
        jr.note(level, text, step=id)
    if jr is None or not jr.console_owns_screen():
        print(text, file=sys.stderr if level == "error" else sys.stdout, flush=True)


def _hint(lines, id):
    jr = _base.jr()
    if jr is not None:
        jr.hint(lines, step=id)
    else:
        for line in lines:
            print(line, flush=True)


def _describe(trace):
    near = f"  (near {trace.test})" if trace.test else ""
    return f"{trace.header or '(no header)'}{near}"


def _report_text(allowed, denied, stale, errors):
    out = []
    for title, items in (("NOT ALLOWLISTED", denied), ("ALLOWLISTED", allowed)):
        out.append(f"{title}: {len(items)}")
        for t in items:
            out.append(f"  line {t.line} [{t.kind}, {t.frames} frames] {t.header}")
            if t.context:
                out.append(f"    context: {t.context}")
            if t.test:
                out.append(f"    near: {t.test}")
    out += [f"STALE ALLOWLIST ENTRY (matched nothing): line {e.line}: {e.source}" for e in stale]
    out += [f"ALLOWLIST ERROR: {e}" for e in errors]
    return "\n".join(out) + "\n"


def gate(id, title, allowlist_path, *, since=None, text="", extra=(), warn_only=False, full_run=True, report=None,
         label=None):
    """The Traces row.  Reads the full log since `since` (a mark()) (else `text`, the captured output of the gates), plus every string
    in `extra` (for example a dev-server log), scans it, and returns 0 (clean, allowlisted, or a failure downgraded by
    warn_only) or 1 (a trace not on the allowlist, or an allowlist error).  Nothing to read ends the row skipped with a
    warning and returns 0.  full_run=False (a skip flag was used) suppresses the stale-entry report.  report, when
    given, is a path that gets every trace, allowed or not."""
    allowlist = load_allowlist(allowlist_path)
    with _row(id, title, label) as row:
        source = since.read() if since is not None and since.path is not None else text
        if not source and not extra and (since is None or since.path is None):
            row.status = "skip"
            row.summary = "no full log to scan"
            _say("warn", "Trace guard skipped: no full log (run with --console board or --full-log).", id)
            return 0
        found = scan(source) if source else []
        for more in extra:
            found += scan(more)
        allowed, denied, used = allowlist.partition(found)
        stale = allowlist.stale(used) if full_run else []
        if report:
            Path(report).parent.mkdir(parents=True, exist_ok=True)
            Path(report).write_text(_report_text(allowed, denied, stale, allowlist.errors), encoding="utf-8")
        for entry in stale:
            _say("warn", f"Stale trace allowlist entry (line {entry.line}): {entry.source}", id)
        if allowlist.errors:
            row.status = "fail"
            row.summary = f"{len(allowlist.errors)} allowlist errors"
            _hint(list(allowlist.errors), id)
            return 1
        suffix = f", {len(allowed)} allowlisted" if allowed else ""
        if not denied:
            row.summary = f"0 traces{suffix}"
            return 0
        row.summary = f"{len(denied)} traces{suffix}"
        lines = [_describe(t) for t in denied[:MAX_HINTS]]
        if len(denied) > MAX_HINTS:
            lines.append(f"+{len(denied) - MAX_HINTS} more" + (f" in {report}" if report else ""))
        if warn_only:
            _say("warn", "Stack traces downgraded to a warning by --warn-traces.", id)
            _hint(lines, id)
            return 0
        row.status = "fail"
        _hint(lines, id)
        return 1
