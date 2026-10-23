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
"""Loads juneau_run.py the way an adopter does (by path), so tests exercise the shipped file and no installed copy."""

import importlib.util
import re
import sys
from pathlib import Path

MODULE_ROOT = Path(__file__).resolve().parents[3]
SCRIPT = MODULE_ROOT / "src" / "main" / "python" / "juneau_run.py"
FIXTURES = Path(__file__).resolve().parent / "fixtures"
DOCS = MODULE_ROOT / "docs"

_spec = importlib.util.spec_from_file_location("juneau_run", SCRIPT)
jr = importlib.util.module_from_spec(_spec)
sys.modules["juneau_run"] = jr
_spec.loader.exec_module(jr)

# Condensed sinks default their full log into JUNEAU_RUN_LOG_DIR; keep every test out of ~/.cache/juneau-run.
import os as _os
import tempfile as _tempfile
_os.environ["JUNEAU_RUN_LOG_DIR"] = _tempfile.mkdtemp(prefix="juneau-run-test-logs-")
# Run history is default-on; keep it out of ~/.cache too.
_os.environ["JUNEAU_RUN_HISTORY_DIR"] = _tempfile.mkdtemp(prefix="juneau-run-test-history-")


def reset_run_state():
    """Forget run ownership, the stdout line state, the console and the bus between tests (globals and os.environ)."""
    import os
    jr._owns_run = False
    jr._at_line_start = True
    jr._head = None
    jr._events = None
    jr._line_offset = 0
    jr._pause_depth = 0
    jr._history = None
    jr._markers_file_failed = False
    jr._result = None
    jr._subscribers.clear()
    jr._toolkit = None
    jr._fallback_noted.clear()
    if getattr(jr, "_console", None) is not None:
        jr._console.close()
        jr._console = None
    for key in (jr.ACTIVE_ENV, "JUNEAU_RUN_FULL_LOG", "JUNEAU_RUN_SESSION", "JUNEAU_RUN_DETAIL", "JUNEAU_RUN_LABEL_WIDTH",
                "JUNEAU_RUN_LIVE", "JUNEAU_RUN_CONSOLE", "RUN_MARKERS", "JUNEAU_RUN_CONDENSED_LOG",
                "JUNEAU_RUN_PENDING", "JUNEAU_RUN_OVERALL", "JUNEAU_RUN_HEADER", "JUNEAU_RUN_HANDBACK",
                jr.CANCEL_ENV, jr.MARKERS_FILE_ENV, jr.RESULT_ENV, jr.RESULT_PARTS_ENV):
        os.environ.pop(key, None)


def isolate_run_env(case):
    """Strip the run-protocol and push variables (JUNEAU_PUSH_*, RUN_MARKERS, RUN_ARTIFACTS, JUNEAU_RUN_* except
    JUNEAU_RUN_PY) from os.environ for the test, so a push.py or Maven environment cannot leak in.  Restored on cleanup."""
    import os
    saved = dict(os.environ)  # not mock.patch.dict(...).start(): tests that call mock.patch.stopall() would undo it
    case.addCleanup(lambda: (os.environ.clear(), os.environ.update(saved)))
    # `python3 -m unittest` (3.10 and earlier) sets argv[0] to "python3 -m unittest", which names the default full log
    argv = sys.argv
    case.addCleanup(lambda: setattr(sys, "argv", argv))
    sys.argv = ["/x/scripts/test.py"]
    for key in list(os.environ):
        if (key in ("RUN_MARKERS", "RUN_ARTIFACTS") or key.startswith("JUNEAU_PUSH_")
                or (key.startswith("JUNEAU_RUN_") and key != "JUNEAU_RUN_PY")):
            del os.environ[key]
    os.environ["JUNEAU_RUN_HISTORY"] = "0"


class Screen:
    """A minimal VT100 model, enough for what LiveRenderer writes: text, \\r, \\n, ESC[nA, ESC[K and ESC[J.
    SGR colour is ignored; cursor show/hide sets cursor_visible.  clamped is set if a cursor-up ever went above the first
    line.  With rows (and cols) it is a viewport: a \\n on the last row scrolls the top line into scrollback, a line
    autowraps at cols, and a wide or emoji code point takes two columns (jr._cw), as the renderer assumes.  start_row
    puts the cursor that far down first, as a run started near the bottom of the window."""

    _CSI = re.compile(r"\x1b\[(\??)(\d*)([A-Za-z])")

    def __init__(self, rows=None, cols=None, start_row=0):
        self.rows, self.cols = rows, cols
        self.cells = [[] for _ in range(start_row + 1)]     # "" marks a wide character's second column
        self.row = start_row
        self.col = 0
        self.scrollback = []
        self.clamped = False
        self.cursor_visible = True

    @property
    def lines(self):
        return ["".join(self.cells[i]) for i in range(len(self.cells))]

    def feed(self, data):
        i = 0
        while i < len(data):
            m = self._CSI.match(data, i)
            if m:
                self._csi(m.group(1), int(m.group(2) or 1), m.group(3))
                i = m.end()
                continue
            ch = data[i]
            if ch == "\r":
                self.col = 0
            elif ch == "\n":
                self._newline()
            else:
                w = jr._cw(ch) if self.rows else 1
                if self.cols and self.col + w > self.cols:
                    self._newline()
                line = self.cells[self.row]
                line.extend([" "] * (self.col - len(line)))
                line[self.col:self.col + w] = [ch] + [""] * (w - 1)
                self.col += w
            i += 1

    def _newline(self):
        self.col = 0
        if self.rows and self.row == self.rows - 1:
            self.scrollback.append("".join(self.cells.pop(0)).rstrip())
            self.cells.append([])
            return
        self.row += 1
        if self.row == len(self.cells):
            self.cells.append([])

    def _csi(self, private, n, cmd):
        if private:
            if n == 25 and cmd in "hl":
                self.cursor_visible = cmd == "h"
            return
        if cmd == "m":
            return
        if cmd == "A":
            self.clamped = self.clamped or n > self.row
            self.row = max(0, self.row - n)
        elif cmd == "K":
            del self.cells[self.row][self.col:]
        elif cmd == "J":
            del self.cells[self.row][self.col:]
            del self.cells[self.row + 1:]

    def resize(self, rows=None, cols=None):
        """Change the viewport as a terminal that does not reflow would: lines already written keep their cells, and
        only later writes wrap and scroll at the new size.  Rows that no longer fit go to scrollback."""
        self.rows, self.cols = rows, cols
        while self.rows and len(self.cells) > self.rows:
            self.scrollback.append("".join(self.cells.pop(0)).rstrip())
            self.row -= 1
        self.row = max(0, self.row)

    def text(self):
        return "\n".join(line.rstrip() for line in self.lines).rstrip("\n")

    def history(self):
        """Everything the terminal holds: the scrollback, then the screen."""
        return "\n".join(self.scrollback + [self.text()]).strip("\n")
