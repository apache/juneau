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
"""Loads juneau_terminal.py the way an adopter does (by path), so tests exercise the shipped file and no installed copy."""

import importlib.util
import re
import sys
from pathlib import Path

MODULE_ROOT = Path(__file__).resolve().parents[3]
SCRIPT = MODULE_ROOT / "src" / "main" / "python" / "juneau_terminal.py"
FIXTURES = Path(__file__).resolve().parent / "fixtures"
DOCS = MODULE_ROOT / "docs"

_spec = importlib.util.spec_from_file_location("juneau_terminal", SCRIPT)
jr = importlib.util.module_from_spec(_spec)
sys.modules["juneau_terminal"] = jr
_spec.loader.exec_module(jr)

# Board sinks default their full log into JUNEAU_TERMINAL_LOG_DIR; keep every test out of ~/.cache/juneau-terminal.
import os as _os
import tempfile as _tempfile
_os.environ["JUNEAU_TERMINAL_LOG_DIR"] = _tempfile.mkdtemp(prefix="juneau-terminal-test-logs-")
# Run history is default-on; keep it out of ~/.cache too.
_os.environ["JUNEAU_TERMINAL_HISTORY_DIR"] = _tempfile.mkdtemp(prefix="juneau-terminal-test-history-")


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
    for key in (jr.ACTIVE_ENV, "JUNEAU_TERMINAL_FULL_LOG", "JUNEAU_TERMINAL_SESSION", "JUNEAU_TERMINAL_DETAIL", "JUNEAU_TERMINAL_LABEL_WIDTH",
                "JUNEAU_TERMINAL_LIVE", "JUNEAU_TERMINAL_CONSOLE", "TERMINAL_MARKERS", "JUNEAU_TERMINAL_BOARD_LOG",
                "JUNEAU_TERMINAL_PENDING", "JUNEAU_TERMINAL_OVERALL", "JUNEAU_TERMINAL_HEADER", "JUNEAU_TERMINAL_HANDBACK",
                jr.CANCEL_ENV, jr.MARKERS_FILE_ENV, jr.RESULT_ENV, jr.RESULT_PARTS_ENV):
        os.environ.pop(key, None)


def isolate_run_env(case):
    """Strip the terminal-protocol and push variables (JUNEAU_PUSH_*, TERMINAL_MARKERS, TERMINAL_ARTIFACTS, JUNEAU_TERMINAL_* except
    JUNEAU_TERMINAL_PY) from os.environ for the test, so a push.py or Maven environment cannot leak in.  Restored on cleanup."""
    import os
    saved = dict(os.environ)  # not mock.patch.dict(...).start(): tests that call mock.patch.stopall() would undo it
    case.addCleanup(lambda: (os.environ.clear(), os.environ.update(saved)))
    # `python3 -m unittest` (3.10 and earlier) sets argv[0] to "python3 -m unittest", which names the default full log
    argv = sys.argv
    case.addCleanup(lambda: setattr(sys, "argv", argv))
    sys.argv = ["/x/scripts/test.py"]
    for key in list(os.environ):
        if (key in ("TERMINAL_MARKERS", "TERMINAL_ARTIFACTS") or key.startswith("JUNEAU_PUSH_")
                or (key.startswith("JUNEAU_TERMINAL_") and key != "JUNEAU_TERMINAL_PY")):
            del os.environ[key]
    os.environ["JUNEAU_TERMINAL_HISTORY"] = "0"


class _Line(list):
    """A screen line's cells; wrapped is set when the line autowrapped onto the next (a reflow joins them again)."""
    wrapped = False


class Screen:
    """A minimal VT100 model, enough for what LiveRenderer writes: text, \\r, \\n, ESC[nA, ESC[nB, ESC[H, ESC[K and ESC[J.
    SGR colour is ignored; cursor show/hide sets cursor_visible.  clamped is set if a cursor-up ever went above the first
    line.  With rows (and cols) it is a viewport: a \\n on the last row scrolls the top line into scrollback, a line
    autowraps at cols, and a wide or emoji code point takes two columns (jr._cw), as the renderer assumes.  start_row
    puts the cursor that far down first, as a run started near the bottom of the window.  scrollback holds the lines as
    text; _back holds the same lines' cells, for resize_like_tmux to reflow.  As in tmux (scroll-on-clear, on by
    default), ESC[J from the top-left corner of a viewport moves the screen's lines into scrollback first."""

    _CSI = re.compile(r"\x1b\[(\??)(\d*)([A-Za-z])")

    def __init__(self, rows=None, cols=None, start_row=0):
        self.rows, self.cols = rows, cols
        self.cells = [_Line() for _ in range(start_row + 1)]    # "" marks a wide character's second column
        self.row = start_row
        self.col = 0
        self.scrollback = []
        self._back = []
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
                    self._newline(wrapped=True)
                line = self.cells[self.row]
                line.extend([" "] * (self.col - len(line)))
                line[self.col:self.col + w] = [ch] + [""] * (w - 1)
                self.col += w
            i += 1

    def resize_like_tmux(self, rows=None, cols=None):
        """Change the viewport as tmux does, the height first, then the width.  Height: shrinking scrolls the top lines
        into scrollback until the cursor fits; growing pulls that many lines back from scrollback onto the top and moves
        the cursor down with them.  Width (reflow): see _reflow."""
        rows = self.rows if rows is None else rows
        if rows < self.rows:
            while self.row >= rows:
                self._scroll_off()
                self.row -= 1
            del self.cells[rows:]
        else:
            pulled = min(len(self.scrollback), rows - self.rows)
            for _ in range(pulled):
                self.scrollback.pop()
                self.cells.insert(0, self._back.pop())
            self.row += pulled
        self.rows = rows
        if cols is not None and cols != self.cols:
            self._reflow(cols)

    def _reflow(self, cols):
        """Rewrap at cols as tmux 3.x does (checked against tmux 3.7c): every line, scrollback included, is joined
        with the lines it autowrapped onto (a line ended by \\n is never joined) and wrapped again at cols, written
        spaces included; the screen is the last rows lines of the result and the rest is scrollback; the cursor keeps
        its place in the text.  So narrowing pushes the top of the screen into scrollback even with blank rows under
        the cursor, and widening pulls lines back from scrollback onto the top (or, with none, moves the cursor up)."""
        grid = self._back + self.cells + [_Line() for _ in range(self.rows - len(self.cells))]
        at = len(self._back) + self.row
        logical, cursor = [], None          # each logical line's cells; the cursor's (logical line, offset)
        joining = False
        for i, line in enumerate(grid):
            if not joining:
                logical.append([])
            if i == at:
                cursor = (len(logical) - 1, len(logical[-1]) + self.col)
            logical[-1] += line
            joining = line.wrapped
        out = []
        for n, cells in enumerate(logical):
            first = len(out)
            out.append(_Line())
            for c in range(len(cells)):
                if cells[c] == "":
                    continue                # a wide character's second column moves with its first
                w = 2 if c + 1 < len(cells) and cells[c + 1] == "" else 1
                if len(out[-1]) + w > cols:
                    out[-1].wrapped = True
                    out.append(_Line())
                out[-1] += cells[c:c + w]
            if cursor is not None and cursor[0] == n:
                at, col = first + cursor[1] // cols, cursor[1] % cols
                if at >= len(out):
                    out.append(_Line())
        while len(out) < self.rows:
            out.append(_Line())
        split = len(out) - self.rows
        self._back = out[:split]
        self.scrollback = ["".join(line).rstrip() for line in self._back]
        self.cells, self.row, self.col, self.cols = out[split:], at - split, col, cols

    def _scroll_off(self):
        line = self.cells.pop(0)
        self.scrollback.append("".join(line).rstrip())
        self._back.append(line)

    def _newline(self, wrapped=False):
        self.col = 0
        self.cells[self.row].wrapped = wrapped      # tmux sets the flag on an autowrap and clears it on a \n
        if self.rows and self.row == self.rows - 1:
            self._scroll_off()
            self.cells.append(_Line())
            return
        self.row += 1
        if self.row == len(self.cells):
            self.cells.append(_Line())

    def _csi(self, private, n, cmd):
        if private:
            if n == 25 and cmd in "hl":
                self.cursor_visible = cmd == "h"
            return
        if cmd == "m":
            return
        if cmd == "H":
            self.row, self.col = 0, 0
        elif cmd == "A":
            self.clamped = self.clamped or n > self.row
            self.row = max(0, self.row - n)
        elif cmd == "B":
            self.row = min(self.row + n, (self.rows or len(self.cells)) - 1)
            self.cells += [_Line() for _ in range(self.row + 1 - len(self.cells))]
        elif cmd == "K":
            del self.cells[self.row][self.col:]
            self.cells[self.row].wrapped = False    # the line ends here now, so it no longer runs onto the next
        elif cmd == "J":
            if self.rows and self.row == 0 and self.col == 0:
                while any(self.cells):
                    self._scroll_off()
                self.cells = [_Line()]
            del self.cells[self.row][self.col:]
            del self.cells[self.row + 1:]
            self.cells[self.row].wrapped = False

    def resize(self, rows=None, cols=None):
        """Change the viewport as a terminal that does not reflow would: lines already written keep their cells, and
        only later writes wrap and scroll at the new size.  Rows that no longer fit go to scrollback."""
        self.rows, self.cols = rows, cols
        while self.rows and len(self.cells) > self.rows:
            self._scroll_off()
            self.row -= 1
        self.row = max(0, self.row)

    def text(self):
        return "\n".join(line.rstrip() for line in self.lines).rstrip("\n")

    def history(self):
        """Everything the terminal holds: the scrollback, then the screen."""
        return "\n".join(self.scrollback + [self.text()]).strip("\n")
