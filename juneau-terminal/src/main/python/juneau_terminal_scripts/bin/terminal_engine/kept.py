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
"""The prompt a leader pane shows after its command ended: a clickable close button (SGR mouse reporting, tmux `mouse on`),
Enter, or a timeout; and the plain Enter prompt of a read-only pane."""

import os
import re
import select
import signal
import sys
import time

from .mouse import left_press
from .ttymode import CbreakInput

BUTTON = "[ click here to close ]"
HINT = "  (or press Enter)"
MOUSE_ON = "\033[?1000h\033[?1006h"
MOUSE_OFF = "\033[?1006l\033[?1000l"


def click_hit(seq, row=None, c1=None, c2=None):
    """True when seq (the bytes after the ESC of a mouse report) is a left press; with row, c1, c2 also on that row and between those columns."""
    press = left_press(seq)
    if press is None:
        return False
    if row is None:
        return True
    x, y = press
    return y == row and c1 <= x <= c2


def prompt_prefix(rc, cancelled):
    if cancelled:
        return "[cancelled, exit %s] stopped by tmux-pane.sh cancel;  " % rc
    return "[exit %s]  " % rc


def readonly_message(rc, cancelled):
    if cancelled:
        return "\n[cancelled, exit %s] stopped by tmux-pane.sh cancel; press Enter to close this pane\n" % rc
    return "\n[exit %s] press Enter to close this pane\n" % rc


def button_box(report, prefix):
    """(row, c1, c2) of the button from a cursor-position report `ESC [ row ; col` (with or without its closing R) read on
    the line below the button, or None."""
    m = re.search(rb"\[([0-9]+);([0-9]+)R?\Z", report)
    if not m:
        return None
    c1 = len(prefix) + 1
    return int(m.group(1)) - 1, c1, c1 + len(BUTTON) - 1


def _write(text):
    sys.stdout.write(text)
    sys.stdout.flush()


def _read_byte(fd, timeout):
    """One byte; None on timeout; b"" at end of input."""
    if timeout is not None and not select.select([fd], [], [], max(timeout, 0))[0]:
        return None
    return os.read(fd, 1)


def _read_until(fd, final, timeout):
    got = b""
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        b = _read_byte(fd, deadline - time.monotonic())
        if not b:
            break
        got += b
        if b == final:
            break
    return got


def _terminal_columns(fd):
    try:
        return os.get_terminal_size(fd).columns
    except OSError:
        return 0


def kept_wait(prefix, minutes=None, fd=0):
    """Print PREFIX and the close button, then wait for a click on the button, Enter, or the timeout (minutes).  The button's
    row comes from a cursor-position report taken on the line below it; when that fails, or the line would wrap, any left click closes."""
    def die(signum, frame):
        raise SystemExit(130)
    old = {s: signal.signal(s, die) for s in (signal.SIGHUP, signal.SIGINT, signal.SIGTERM)}
    try:
        with CbreakInput(fd):
            try:
                _write(MOUSE_ON)
                _write("\n%s%s%s\n" % (prefix, BUTTON, HINT))
                box = None
                if _terminal_columns(fd) > len(prefix) + len(BUTTON) + len(HINT):
                    _write("\033[6n")
                    box = button_box(_read_until(fd, b"R", 1.0), prefix)
                row, c1, c2 = box if box else (None, None, None)
                deadline = time.monotonic() + minutes * 60 if minutes is not None else None
                while True:
                    left = None if deadline is None else deadline - time.monotonic()
                    if left is not None and left <= 0:
                        break
                    ch = _read_byte(fd, left)
                    if ch is None or ch == b"":
                        break
                    if ch in (b"\r", b"\n"):
                        break
                    if ch != b"\033":
                        continue
                    seq = ""
                    while True:
                        c = _read_byte(fd, 1.0)
                        if not c:
                            break
                        seq += c.decode("latin-1")
                        if c in (b"M", b"m") or len(seq) >= 24:
                            break
                    if click_hit(seq, row, c1, c2):
                        break
            finally:
                _write(MOUSE_OFF)
    finally:
        for s, h in old.items():
            signal.signal(s, h)


def readonly_wait(rc, cancelled, minutes=None):
    """A read-only pane ignores keystrokes at the terminal, so no button and no mouse mode: print the prompt and wait for a
    line, or for the timeout."""
    _write(readonly_message(rc, cancelled))
    if select.select([sys.stdin], [], [], minutes * 60 if minutes is not None else None)[0]:
        try:
            sys.stdin.readline()
        except OSError:
            pass
