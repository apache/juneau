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
"""A split run's panes stay readable when the window is resized: the running board redraws at the new width, and a finished board kept on screen is laid out again, each line cut to the width with an ellipsis."""

import shlex
import sys
import time

from scripts_support import PYTHON_SRC
from tmux_support import PaneCase, WATCH_RUN_SH, requires_tmux, slow_tmux

BOARD = """import os, sys, time
sys.path.insert(0, %r)
import juneau_terminal as jt
import juneau_terminal_scripts as jts
jts.BOOTSTRAP_API = jts.REQUIRED_BOOTSTRAP_API
from juneau_terminal_scripts import run
os.environ["JUNEAU_TERMINAL_CONSOLE"] = "board"
LONG = "x" * 150
with run.session("Resize: board", lines=["First: " + LONG, "Second: " + LONG, "Third: " + LONG], plan=["s0", "s1"]) as s:
    for i in range(2):
        with run.step("s%%d" %% i, "Step %%d" %% i, label="Step %%d" %% i):
            for _ in range(%d):
                time.sleep(0.2)
                jt.tick()
    sys.exit(%d)
"""


@requires_tmux
@slow_tmux
class ResizeTest(PaneCase):

    def board_script(self, ticks, rc):
        path = self.tmp / "board.py"
        path.write_text(BOARD % (str(PYTHON_SRC), ticks, rc))
        return path

    def window_of(self, pane):
        return self.tmux("display-message", "-p", "-t", pane, "#{window_id}").strip()

    def resize_through(self, window, widths):
        for w in widths:
            self.tmux("resize-window", "-t", window, "-x", str(w), "-y", "40")
            time.sleep(0.15)
        time.sleep(1.2)

    def assert_clean(self, pane):
        width = int(self.tmux("display-message", "-p", "-t", pane, "#{pane_width}").strip())
        text = self.tmux("capture-pane", "-p", "-t", pane)
        for head in ("First:", "Second:", "Third:"):
            self.assertEqual(text.count(head), 1, "%s in:\n%s" % (head, text))
        for line in text.splitlines():
            self.assertLessEqual(len(line), width, line)
            self.assertFalse(line and set(line) == {"x"}, "a wrapped fragment: " + line)    # a long line is cut, not wrapped
        for line in text.splitlines():
            if line.startswith(("First:", "Second:", "Third:")):
                self.assertTrue(line.endswith("\u2026"), line)

    def test_a_running_board_redraws_at_every_width(self):
        cmd = "TMUX_PANE_RUN_ID=%%1 %s %s; sleep 60" % (sys.executable, shlex.quote(str(self.board_script(40, 0))))
        pane = self.tmux("new-window", "-d", "-P", "-F", "#{pane_id}", "-t", "t", cmd).strip()
        time.sleep(2)
        self.resize_through(self.window_of(pane), (180, 150, 120, 90, 70, 100, 140, 190, 110, 80, 120))
        self.assert_clean(pane)

    def test_a_finished_kept_board_is_laid_out_again_after_a_resize(self):
        script = self.board_script(1, 1)
        pane = self.tmux("new-window", "-d", "-P", "-F", "#{pane_id}", "-t", "t",
                         "env JUNEAU_TERMINAL_TOOLBAR=0 JUNEAU_TERMINAL_LOG_DIR=%s TMUX_PANE_GRACE=2 %s -k always -- %s %s; sleep 60"
                         % (shlex.quote(str(self.tmp / "logs")), WATCH_RUN_SH, sys.executable, shlex.quote(str(script)))).strip()
        deadline = time.time() + 30
        leader = None
        while time.time() < deadline and not leader:
            for l in self.tmux("list-panes", "-s", "-t", "t", "-F", "#{pane_id} #{@tmux_state}").splitlines():
                if " kept" in l:
                    leader = l.split()[0]
            time.sleep(0.3)
        self.assertIsNotNone(leader, "the failed run's leader pane never reached the kept state")
        self.resize_through(self.window_of(leader), (180, 150, 120, 90, 70, 100, 140, 80, 120))
        self.assert_clean(leader)
