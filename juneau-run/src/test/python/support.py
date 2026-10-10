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


def reset_run_state():
    """Forget run ownership, the stdout line state, the console and the bus between tests (globals and os.environ)."""
    import os
    jr._owns_run = False
    jr._at_line_start = True
    jr._head = None
    jr._events = None
    jr._line_offset = 0
    jr._pause_depth = 0
    jr._subscribers.clear()
    if getattr(jr, "_console", None) is not None:
        jr._console.close()
        jr._console = None
    for key in (jr.ACTIVE_ENV, "JUNEAU_RUN_SESSION", "JUNEAU_RUN_DETAIL", "JUNEAU_RUN_LABEL_WIDTH",
                "JUNEAU_RUN_LIVE"):
        os.environ.pop(key, None)


class Screen:
    """A minimal VT100 model, enough for what LiveRenderer writes: text, \\r, \\n, ESC[nA, ESC[K and ESC[J.
    SGR colour is ignored; cursor show/hide sets cursor_visible.  clamped is set if a cursor-up ever went above the first
    line."""

    _CSI = re.compile(r"\x1b\[(\??)(\d*)([A-Za-z])")

    def __init__(self):
        self.lines = [""]
        self.row = 0
        self.col = 0
        self.clamped = False
        self.cursor_visible = True

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
                self.row += 1
                self.col = 0
                if self.row == len(self.lines):
                    self.lines.append("")
            else:
                line = self.lines[self.row].ljust(self.col)
                self.lines[self.row] = line[:self.col] + ch + line[self.col + 1:]
                self.col += 1
            i += 1

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
            self.lines[self.row] = self.lines[self.row][:self.col]
        elif cmd == "J":
            self.lines[self.row] = self.lines[self.row][:self.col]
            del self.lines[self.row + 1:]

    def text(self):
        return "\n".join(line.rstrip() for line in self.lines).rstrip("\n")
