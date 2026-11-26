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
"""tmux-toolbar.sh: the pure functions (status words, button layout, click mapping), then the pane behaviour on a throwaway tmux server."""

import subprocess
import time
import unittest

from tmux_support import BIN, PaneCase, requires_tmux

TOOLBAR_SH = BIN / "tmux-toolbar.sh"


def tb(*args):
    r = subprocess.run([str(TOOLBAR_SH), *args], capture_output=True, text=True, timeout=30)
    return r.returncode, r.stdout.rstrip("\n")


class PureFunctionsTest(unittest.TestCase):

    def test_status_words(self):
        cases = {
            "": "closed",
            "running": "running",
            "kept (exit 0)": "ok",
            "exit 0": "ok",
            "kept (exit 3)": "failed (exit 3)",
            "exit 10": "failed (exit 10)",
            "kept (cancelled, exit 143)": "cancelled",
        }
        for raw, want in cases.items():
            with self.subTest(raw=raw):
                self.assertEqual(tb("--print-status", raw), (0, want))

    def test_line_shows_buttons_then_title_and_status(self):
        self.assertEqual(tb("--print-line", "running", "0", "push irs"),
                         (0, "[Hide output] [Cancel] [Close]  push irs: running"))

    def test_line_flips_the_output_label_when_hidden(self):
        self.assertEqual(tb("--print-line", "kept (exit 0)", "1", "push"),
                         (0, "[Show output] [Cancel] [Close]  push: ok"))

    def test_hit_maps_columns_to_buttons(self):
        cases = {1: "hide", 13: "hide", 14: "none", 15: "cancel", 22: "cancel", 23: "none",
                 24: "close", 30: "close", 31: "none", 0: "none", "x": "none", "": "none"}
        for col, want in cases.items():
            with self.subTest(col=col):
                self.assertEqual(tb("--print-hit", str(col)), (0, want))

    def test_click_parses_only_left_presses(self):
        cases = {
            "[<0;5;1M": "5",       # left press at column 5
            "[<0;5;1m": "none",    # release
            "[<2;5;1M": "none",    # right button
            "[<64;5;1M": "none",   # wheel
            "[A": "none",          # an arrow key
            "[<0;;1M": "none",
            "": "none",
        }
        for seq, want in cases.items():
            with self.subTest(seq=seq):
                self.assertEqual(tb("--print-click", seq), (0, want))


@requires_tmux
class ToolbarPaneTest(PaneCase):
    SCRIPT = TOOLBAR_SH

    def setUp(self):
        super().setUp()
        self.marker = self.tmp / "cancelled.txt"
        self.leader = self.new_pane("sleep 300")
        self.log = self.new_pane("sleep 300")
        self.set_opts(self.leader, **{"@tmux_pane": "1", "@tmux_state": "running"})
        self.set_opts(self.log, **{"@tmux_pane": "1", "@tmux_state": "follower", "@tmux_parent": self.leader})
        cmd = ("TMUX_PANE_RUN_ID=%s TMUX_PANE_RUN_CANCEL='touch %s' exec %s --title demo"
               % (self.leader, self.marker, TOOLBAR_SH))
        self.bar = self.new_pane(cmd)
        self.wait_for("running")

    def new_pane(self, cmd):
        return self.tmux("split-window", "-d", "-v", "-t", self.agent, "-P", "-F", "#{pane_id}", "bash -c %r" % cmd).strip()

    def set_opts(self, pane, **opts):
        for k, v in opts.items():
            self.tmux("set-option", "-p", "-t", pane, k, v)

    def screen(self):
        return self.tmux("capture-pane", "-p", "-t", self.bar)

    def wait_for(self, text, timeout=8):
        deadline = time.time() + timeout
        while text not in self.screen() and time.time() < deadline:
            time.sleep(0.1)
        self.assertIn(text, self.screen())

    def alive(self, pane):
        return pane in self.tmux("list-panes", "-s", "-t", "t", "-F", "#{pane_id}").split()

    def window_of(self, pane):
        return self.tmux("display-message", "-p", "-t", pane, "#{window_id}").strip()

    def key(self, k):
        self.tmux("send-keys", "-t", self.bar, "-l", k)

    def wait_until(self, fn, timeout=8):
        deadline = time.time() + timeout
        while not fn() and time.time() < deadline:
            time.sleep(0.1)
        return fn()

    def test_status_follows_the_leaders_state(self):
        self.set_opts(self.leader, **{"@tmux_state": "kept (exit 0)"})
        self.wait_for("demo: ok")
        self.set_opts(self.leader, **{"@tmux_state": "kept (exit 3)"})
        self.wait_for("demo: failed (exit 3)")
        self.set_opts(self.leader, **{"@tmux_state": "kept (cancelled, exit 143)"})
        self.wait_for("demo: cancelled")

    def test_k_cancels_a_running_run_and_is_ignored_afterwards(self):
        self.key("k")
        self.assertTrue(self.wait_until(self.marker.exists))
        self.marker.unlink()
        self.set_opts(self.leader, **{"@tmux_state": "kept (exit 0)"})
        self.wait_for("demo: ok")
        self.key("k")
        time.sleep(1.5)
        self.assertFalse(self.marker.exists())

    def test_h_hides_and_shows_the_log_pane_without_killing_it(self):
        home = self.window_of(self.bar)
        self.assertEqual(self.window_of(self.log), home)
        self.key("h")
        self.assertTrue(self.wait_until(lambda: self.window_of(self.log) != home))
        self.wait_for("[Show output]")
        self.assertTrue(self.alive(self.log))
        self.key("h")
        self.assertTrue(self.wait_until(lambda: self.window_of(self.log) == home))
        self.wait_for("[Hide output]")

    def test_clicking_a_button_acts_like_its_key(self):
        home = self.window_of(self.bar)
        self.key("\033[<0;5;1M")        # left press on [Hide output]
        self.assertTrue(self.wait_until(lambda: self.window_of(self.log) != home))
        self.key("\033[<0;18;1M")       # left press on [Cancel]
        self.assertTrue(self.wait_until(self.marker.exists))

    def test_other_input_is_ignored(self):
        for k in ("z", "\033[<0;50;1M", "\033[<2;5;1M", "\033[A"):
            self.key(k)
        time.sleep(1.2)
        self.assertFalse(self.marker.exists())
        self.assertEqual(self.window_of(self.log), self.window_of(self.bar))
        self.assertTrue(self.alive(self.leader) and self.alive(self.bar))

    def test_x_cancels_then_closes_leader_log_and_toolbar(self):
        self.key("x")
        self.assertTrue(self.wait_until(self.marker.exists))
        # the fake cancel does not end the run, so finish it the way tmux-pane.sh would
        self.set_opts(self.leader, **{"@tmux_state": "kept (cancelled, exit 143)"})
        self.assertTrue(self.wait_until(lambda: self.panes() == 1, timeout=15))

    def test_x_after_the_run_closes_everything_even_with_the_log_hidden(self):
        self.set_opts(self.leader, **{"@tmux_state": "kept (exit 0)"})
        self.wait_for("demo: ok")
        self.key("h")
        self.wait_for("[Show output]")
        self.key("x")
        self.assertTrue(self.wait_until(lambda: self.panes() == 1, timeout=15))
        self.assertFalse(self.marker.exists())


if __name__ == "__main__":
    unittest.main()
