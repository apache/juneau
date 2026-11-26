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
"""tmux-toolbar.sh: a one-row toolbar [Hide output] [Cancel] [Close] and the run's status, for a watch-run.sh run window."""

import os
import re
import select
import shlex
import signal
import subprocess
import sys
import time

from . import script_path
from .mouse import left_press
from .tmux import tmux, tmux_out
from .ttymode import CbreakInput

# Fixed button columns (1-based, inclusive).  "Hide output" and "Show output" are the same width, so they never move.
HIDE_C1, HIDE_C2, CANCEL_C1, CANCEL_C2, CLOSE_C1, CLOSE_C2 = 1, 13, 15, 22, 24, 30


def status_word(raw):
    """The status word for the leader's @tmux_state: running, `exit N`, `kept (exit N)`, `kept (cancelled, exit N)`; empty = pane gone."""
    if raw == "":
        return "closed"
    if raw == "running":
        return "running"
    if "cancelled" in raw:
        return "cancelled"
    if "exit " in raw:
        digits = re.match(r"[0-9]*", raw.rsplit("exit ", 1)[1]).group()
        return "ok" if digits == "0" else "failed (exit %s)" % digits
    return raw


def render_line(raw, hidden, title, msg="", cols=0, styled=False):
    """The toolbar text.  hidden is "0"/"1"; cols = columns to fit (0 = no limit; a width at or under the buttons is not cut);
    styled dims [Cancel] once the run is over (the plain form is what the tests compare)."""
    hide = "[Show output]" if str(hidden) == "1" else "[Hide output]"
    rest = "  %s: %s" % (title, status_word(raw))
    if msg:
        rest += " (%s)" % msg
    if cols > CLOSE_C2:
        rest = rest[:cols - CLOSE_C2]
    if styled and raw != "running":
        return "%s \033[2m[Cancel]\033[22m [Close]%s" % (hide, rest)
    return "%s [Cancel] [Close]%s" % (hide, rest)


def hit_button(col):
    """Which button owns column COL: hide, cancel, close or none."""
    try:
        c = int(col)
        if not str(col).strip().isdigit():
            return "none"
    except (TypeError, ValueError):
        return "none"
    if HIDE_C1 <= c <= HIDE_C2:
        return "hide"
    if CANCEL_C1 <= c <= CANCEL_C2:
        return "cancel"
    if CLOSE_C1 <= c <= CLOSE_C2:
        return "close"
    return "none"


def click_column(seq):
    """The column of a left-button press in SEQ (the bytes after ESC of an SGR mouse report), else None."""
    press = left_press(seq, need_y=False)
    return press[0] if press else None


def _hooks(argv):
    """The --print-* test hooks; returns an exit code, or None when argv is not a hook."""
    if not argv:
        return None

    def arg(i, default=""):
        return argv[i] if len(argv) > i else default
    if argv[0] == "--print-status":
        print(status_word(arg(1)))
    elif argv[0] == "--print-line":
        print(render_line(arg(1), arg(2, "0"), arg(3)))
    elif argv[0] == "--print-hit":
        print(hit_button(arg(1)))
    elif argv[0] == "--print-click":
        col = click_column(arg(1))
        print(col if col is not None else "none")
    else:
        return None
    return 0


class Snapshot:
    def __init__(self, leader, log, hidden, height):
        self.leader, self.log, self.hidden, self.height = leader, log, hidden, height


SNAPSHOT_FORMAT = "#{pane_id}|#{@tmux_parent}|#{@tmux_state}|#{window_id}|#{pane_height}"


def parse_snapshot(text, run, self_pane):
    """From one `list-panes -a` call: the leader's @tmux_state ("" = gone), the log follower (a follower of this run that is not this
    pane; it may sit in the hidden window), whether it lives in another window than this pane, and this pane's height."""
    rows = [(line.split("|") + [""] * 5)[:5] for line in text.splitlines()]
    leader = next((r[2] for r in rows if r[0] == run), "")
    log = next((r for r in rows if r[1] == run and r[2] == "follower" and r[0] != self_pane), None)
    mine = next((r for r in rows if r[0] == self_pane), None)
    hidden = 1 if log and log[3] and log[3] != (mine[3] if mine else "") else 0
    return Snapshot(leader, log[0] if log else "", hidden, mine[4] if mine else "")


class KeyParser:
    """Turns terminal input into toolbar actions: hide, cancel, close.  Keys h k x (either case) and left clicks on a button."""

    def __init__(self):
        self.seq = None

    def feed(self, data):
        actions = []
        for ch in data:
            if self.seq is not None:
                self.seq += ch
                if ch in "Mm" or len(self.seq) >= 24:
                    col = click_column(self.seq)
                    self.seq = None
                    if col is not None:
                        act = hit_button(col)
                        if act != "none":
                            actions.append(act)
                continue
            if ch == "\033":
                self.seq = ""
            elif ch in "hH":
                actions.append("hide")
            elif ch in "kK":
                actions.append("cancel")
            elif ch in "xX":
                actions.append("close")
        return actions

    def timeout(self):
        """A second of silence mid-sequence: give up on it, as the bash `read -t 1` did."""
        self.seq = None


class Toolbar:
    def __init__(self, run, cancel_cmd, self_pane, title):
        self.run, self.cancel_cmd, self.self_pane, self.title = run, cancel_cmd, self_pane, title
        self.msg = ""
        self.last_raw = ""
        self.background = []

    def snapshot(self):
        return parse_snapshot(tmux_out("list-panes", "-a", "-F", SNAPSHOT_FORMAT), self.run, self.self_pane)

    def toggle_output(self):
        s = self.snapshot()
        if not s.log:
            self.msg = "no log pane"
            return
        if not s.hidden:
            if tmux("break-pane", "-d", "-s", s.log, "-n", "%s-log" % self.title)[0] != 0:
                self.msg = "could not hide the log"
        elif tmux("join-pane", "-d", "-v", "-l", "40%", "-s", s.log, "-t", self.run)[0] != 0:
            self.msg = "could not show the log"

    def do_cancel(self):
        if self.snapshot().leader != "running":
            return
        self.background = [p for p in self.background if p.poll() is None]
        self.background.append(subprocess.Popen(["sh", "-c", self.cancel_cmd], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL))
        self.msg = "cancelling"

    def do_close(self):
        """Cancel a running run first, then have tmux-pane.sh close the leader and every follower (this pane included) from the tmux
        server, so the close survives this pane dying.  When the leader is already gone, close what is left by hand."""
        if self.snapshot().leader == "running":
            subprocess.run(["sh", "-c", self.cancel_cmd], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            # tmux-pane.sh close refuses while the leader still says running, and the runner clears that a moment after the cancel returns
            for _ in range(100):
                if self.snapshot().leader != "running":
                    break
                time.sleep(0.1)
        s = self.snapshot()
        if s.leader:
            tmux("run-shell", "-b", "%s close %s >/dev/null 2>&1" % (shlex.quote(script_path("tmux-pane.sh")), shlex.quote(self.run)))
        else:
            if s.log:
                tmux("kill-pane", "-t", s.log)
            tmux("kill-pane", "-t", self.self_pane)

    def act(self, action):
        """Returns True when the toolbar should exit."""
        if action == "hide":
            self.toggle_output()
        elif action == "cancel":
            self.do_cancel()
        elif action == "close":
            self.do_close()
            return True
        return False

    def redraw(self):
        s = self.snapshot()
        # A window resize hands the extra rows to the last pane, which is this one; give them back to the panes above.
        if s.height and s.height != "1":
            tmux("resize-pane", "-y", "1", "-t", self.self_pane)
        if s.leader != self.last_raw:
            self.msg = ""
            self.last_raw = s.leader
        try:
            cols = os.get_terminal_size(0).columns
        except OSError:
            cols = 0
        sys.stdout.write("\r\033[2K" + render_line(s.leader, str(s.hidden), self.title, self.msg, cols, True))
        sys.stdout.flush()

    def loop(self):
        def die(signum, frame):
            raise SystemExit(130)
        for s in (signal.SIGHUP, signal.SIGINT, signal.SIGTERM):
            signal.signal(s, die)
        parser = KeyParser()
        try:
            with CbreakInput(0):
                sys.stdout.write("\033[?25l\033[?1000h\033[?1006h")
                sys.stdout.flush()
                tmux("resize-pane", "-y", "1", "-t", self.self_pane)
                self.redraw()
                while True:
                    if select.select([0], [], [], 1.0)[0]:
                        data = os.read(0, 1024)
                        if not data:
                            time.sleep(1.0)      # input closed: keep drawing until the pane goes away
                        else:
                            for action in parser.feed(data.decode("latin-1")):
                                if self.act(action):
                                    return 0
                    else:
                        parser.timeout()
                    self.redraw()
        finally:
            sys.stdout.write("\033[?1006l\033[?1000l\033[?25h")
            sys.stdout.flush()


def main(argv):
    hook = _hooks(argv)
    if hook is not None:
        return hook
    title = "run"
    i = 0
    while i < len(argv):
        if argv[i] == "--title":
            if i + 1 >= len(argv):
                print("tmux-toolbar.sh: --title needs a value", file=sys.stderr)
                return 2
            title = argv[i + 1]
            i += 1
        else:
            print("tmux-toolbar.sh: unknown argument %s" % argv[i], file=sys.stderr)
            return 2
        i += 1
    run = os.environ.get("TMUX_PANE_RUN_ID", "")
    cancel_cmd = os.environ.get("TMUX_PANE_RUN_CANCEL", "")
    self_pane = os.environ.get("TMUX_PANE", "")
    if not (os.environ.get("TMUX") and run and cancel_cmd and self_pane):
        print("tmux-toolbar.sh: must run as a watch-run.sh pane (TMUX, TMUX_PANE, TMUX_PANE_RUN_ID, TMUX_PANE_RUN_CANCEL)", file=sys.stderr)
        return 2
    return Toolbar(run, cancel_cmd, self_pane, title).loop()
