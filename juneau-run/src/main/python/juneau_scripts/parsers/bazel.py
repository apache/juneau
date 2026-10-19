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

"""Bazel: rows, targets, test counts and a failure excerpt from the Build Event Protocol JSON file.

For build, test and coverage the parser adds --build_event_json_file=<temp file> after the command word, unless the
command already names one (then it reads that file, ignoring what an earlier run left there).  It reads the file as
Bazel appends to it: poll() between output chunks, the rest in finish().  Console lines only feed a generic tail, used
when Bazel wrote no usable records or none explains the failure.  It works the same under a terminal: the flag adds a
side file and changes nothing Bazel prints."""

import json
import re
import shutil
import tempfile
import time
from pathlib import Path
from urllib.parse import unquote, urlparse

from . import _BAZEL_COMMANDS as COMMANDS
from . import program_index
from . import _side

FLAG = "--build_event_json_file"
TAIL_LINES = 40
BAD_RECORD_LIMIT = 5
PASS = ("PASSED", "FLAKY")
SKIP = ("NO_STATUS",)
PHASES = ("Analysis", "Build", "Test")
_ANALYZED = re.compile(r"^INFO: (?:Analyzed|Found) ")
_BARE_LEVEL = re.compile(r"^[A-Z]+:$")      # Bazel ends its output with a bare "INFO: "


def command_index(cmd):
    """Index of the Bazel command word: the first argument after the program (past any launcher in front of it) that is
    not a startup option; -1 if none."""
    for i in range(program_index(cmd) + 1, len(cmd)):
        if not cmd[i].startswith("-"):
            return i
    return -1


def user_bep_path(args, cwd):
    """The file the command already passes to --build_event_json_file (resolved against cwd), or None.  Arguments
    after `--` are target patterns."""
    for i, a in enumerate(args):
        if a == "--":
            return None
        if a.startswith(FLAG + "="):
            return Path(cwd or ".") / a[len(FLAG) + 1:]
        if a == FLAG and i + 1 < len(args):
            return Path(cwd or ".") / args[i + 1]
    return None


def file_uri_path(uri):
    """The local path of a file:// URI with an empty or localhost host, else None."""
    if not isinstance(uri, str):
        return None
    u = urlparse(uri)
    if u.scheme != "file" or u.netloc not in ("", "localhost"):
        return None
    return Path(unquote(u.path))


def _tail(path):
    """The last TAIL_LINES lines of a text file, or None if there is no path or it can't be read."""
    if path is None:
        return None
    try:
        lines = Path(path).read_text(encoding="utf-8", errors="replace").splitlines()
    except OSError:
        return None
    return "\n".join(lines[-TAIL_LINES:])


def _last_line(text):
    """The last non-blank line of a progress chunk as a terminal would show it: after its last carriage return, with
    escape sequences removed."""
    for line in reversed(text.splitlines()):
        line = _side.strip_ansi(line.rstrip("\r").rsplit("\r", 1)[-1]).strip()
        if line and not _BARE_LEVEL.match(line):
            return line
    return ""


class BazelParser(_side.Parser):
    """Rows Analysis, Build and Test (sub ids <step>/analysis, /build, /test); one unit per target, a test target's
    from its test summary; cumulative test counts; on failure, a test log tail, a failed action's stderr tail or
    message, or the console tail."""

    name = "bazel"

    def __init__(self, side_path=None, clock=time.monotonic):
        super().__init__()
        self.side_path = side_path       # where the injected flag points (tests); None means a temp dir
        self.clock = clock
        self.generic = _side.GenericParser()
        self.side = None
        self.tmp = None
        self.testing = False
        self.records = 0
        self.bad = 0
        self.row = None                  # (sub id, phase index, start time) of the open row
        self.phase = -1
        self.failed_phases = set()
        self.configured = {}             # label -> is a test target
        self.expected = 0                # targets the command line expanded to, known before any is configured
        self.units_sent = False
        self.done = set()                # labels that have their unit
        self.dots = 0
        self.total = self.fail = self.skip = 0
        self.test_failures = []          # (label, status, test.log path or None)
        self.action_failures = []        # (label, stderr path or None, message or None)
        self.messages = []

    def prepare(self, cmd, env, cwd):
        self.generic.step_id = self.step_id
        cmd = list(cmd)
        i = command_index(cmd)
        if i < 0 or cmd[i] not in COMMANDS:
            return cmd, env
        self.testing = cmd[i] in ("test", "coverage")
        user = user_bep_path(cmd[i + 1:], cwd)
        if user is not None:
            self.side = _side.SideFile(user, ignore_existing=True)
            return cmd, env
        path = self.side_path
        if path is None:
            self.tmp = tempfile.mkdtemp(prefix="juneau-run-bazel-")
            path = Path(self.tmp) / "bep.json"
        self.side = _side.SideFile(path)
        return cmd[:i + 1] + [f"{FLAG}={path}"] + cmd[i + 1:], env

    def handle(self, line):
        return self.generic.handle(line)

    def poll(self):
        if self.side is None:
            return []
        return self._process(self.side.read_new())

    def _process(self, lines):
        events = []
        for raw in lines:
            if not raw.strip():
                continue
            try:
                events += self.parse_record(json.loads(raw))
            except (ValueError, KeyError, TypeError, AttributeError):
                self.bad += 1
                continue
            self.records += 1
        return events

    def parse_record(self, obj):
        """The events for one record (a dict); record kinds the parser doesn't use give none."""
        rid = obj["id"]
        sid = self.step_id
        if "started" in rid:
            return self._enter(0)
        if "progress" in rid:
            body = obj.get("progress", {})
            err, out = body.get("stderr", ""), body.get("stdout", "")
            text = _last_line(err) or _last_line(out)
            events = [("progress", sid, text)] if text else []
            if self.phase < 1 and any(_ANALYZED.match(_side.strip_ansi(line).lstrip("\r")) for line in err.splitlines()):
                events += self._enter(1)
            return events
        if "pattern" in rid:
            self.expected += sum(1 for child in obj.get("children", []) if "targetConfigured" in child)
            return []
        if "targetConfigured" in rid:
            label = rid["targetConfigured"]["label"]
            if obj.get("aborted") is not None:
                self.failed_phases.add(0)
                self.messages.append(obj["aborted"].get("description") or f"{label} aborted")
                return self._unit(label, "skip")
            c = obj.get("configured", {})
            self.configured[label] = bool(c.get("testSize")) or c.get("targetKind", "").endswith("_test rule")
            return []
        if "targetCompleted" in rid:
            label = rid["targetCompleted"]["label"]
            events = self._enter(1)
            if not self.units_sent and (self.configured or self.expected):
                self.units_sent = True
                events.append(("units", sid, max(len(self.configured), self.expected), "targets"))
            if obj.get("aborted") is not None:
                return events + self._unit(label, "skip")
            if not obj.get("completed", {}).get("success"):
                self.failed_phases.add(1)
                return events + self._unit(label, "fail")
            if self.testing and self.configured.get(label):
                return events   # its unit comes with its test summary
            return events + self._unit(label, "pass")
        if "actionCompleted" in rid:
            events = self._enter(1)
            action = obj.get("action", {})
            if not action.get("success"):
                self.failed_phases.add(1)
                message = action.get("failureDetail", {}).get("message")
                label = action.get("label") or rid["actionCompleted"].get("label")
                self.action_failures.append((label, file_uri_path(action.get("stderr", {}).get("uri")), message))
                if message:
                    self.messages.append(message)
            return events
        if "testResult" in rid:
            return self._enter(2)
        if "testSummary" in rid:
            label = rid["testSummary"]["label"]
            summary = obj.get("testSummary", {})
            status = summary.get("overallStatus", "NO_STATUS")
            events = self._enter(2)
            if label in self.done:
                return events
            outcome = "pass" if status in PASS else "skip" if status in SKIP else "fail"
            self.total += 1
            if outcome == "fail":
                self.fail += 1
                self.failed_phases.add(2)
                failed = summary.get("failed") or [{}]
                self.test_failures.append((label, status, file_uri_path(failed[0].get("uri"))))
            elif outcome == "skip":
                self.skip += 1
            return events + self._unit(label, outcome) + [("tests", sid, self.total, self.fail, 0, self.skip)]
        if "buildFinished" in rid:
            return self._close_rows(obj.get("finished", {}).get("exitCode", {}).get("code", 0))
        return []

    def _enter(self, phase):
        """Close the open row and open the row for phase; rows only move forward."""
        if phase <= self.phase:
            return []
        events = self._close_row("fail" if self._failed_by(self.phase) else "ok") if self.row else []
        self.phase = phase
        name = PHASES[phase]
        sub = f"{self.step_id}/{name.lower()}"
        self.row = (sub, phase, self.clock())
        return events + [("sub_start", sub, phase + 1, name)]

    def _failed_by(self, phase):
        """Whether a failure was recorded for phase or an earlier one: Bazel 7 interleaves the phases, so a failure that
        arrives while a later row is open still belongs to that row."""
        return any(failed <= phase for failed in self.failed_phases)

    def _close_row(self, status):
        sub, _, start = self.row
        self.row = None
        return [("sub_end", sub, status, int((self.clock() - start) * 1000))]

    def _close_rows(self, code):
        """Close the open row at the end of the build: failed if its phase or an earlier one failed, or if the build
        failed and no phase says why."""
        if self.row is None:
            return []
        failed = self._failed_by(self.row[1]) or (code != 0 and not self.failed_phases)
        return self._close_row("fail" if failed else "ok")

    def _unit(self, label, outcome):
        if label in self.done:
            return []
        self.done.add(label)
        self.dots += 1
        return [("unit", self.step_id, self.dots, outcome)]

    def finish(self, exit_code):
        if self.side is None:
            return self.generic.finish(exit_code)
        events = self._process(self.side.drain())
        for label in self.configured:   # a test that never got a summary (the build stopped first) did not run
            if self.configured[label] and self.testing:
                events += self._unit(label, "skip")
        events += self._close_rows(exit_code)
        if self.records == 0 or self.bad > BAD_RECORD_LIMIT:
            return events + [("note", "warn", "no structured output from bazel", self.step_id)] + \
                self.generic.finish(exit_code)
        if exit_code != 0:
            events += self._failure(exit_code)
        return events

    def _failure(self, exit_code):
        """The first of: a failed test's log tail, a failed action's stderr tail, a recorded message, the console
        tail."""
        for label, status, path in self.test_failures:
            tail = _tail(path)
            if tail is not None:
                return self._excerpt(f"{label} {status}\n{tail}")
        for label, path, message in self.action_failures:
            tail = _tail(path)
            if tail is not None:
                return self._excerpt(f"{message or label}\n{tail}")
        if self.messages:
            return self._excerpt(self.messages[0])
        if self.test_failures:   # no log to show (remote execution, bytestream://, a replay): at least name the test
            label, status, _ = self.test_failures[0]
            return self._excerpt(f"{label} {status}")
        return self.generic.finish(exit_code)

    def _excerpt(self, text):
        return [("cond", line) for line in text.splitlines()] + [("failure", text, self.step_id)]

    def close(self):
        if self.tmp is not None:
            shutil.rmtree(self.tmp, ignore_errors=True)
            self.tmp = None


PARSER = BazelParser
