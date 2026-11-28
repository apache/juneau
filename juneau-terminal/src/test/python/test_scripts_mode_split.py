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
"""`--mode split` end to end on a throwaway tmux server: a fixture script that calls mode.apply, run through both engine paths.
The cases that wait on timers or open the new-session path are slow_tmux (JUNEAU_TERMINAL_TMUX_TESTS=1)."""

import os
import re
import shlex
import shutil
import subprocess
import sys
import tempfile
import time
from pathlib import Path

from scripts_support import PYTHON_SRC
from tmux_support import TMUX_PANE_SH, WatchCase, requires_tmux, slow_tmux

FIXTURE = """import argparse, os, sys, time
sys.path.insert(0, %r)
from juneau_terminal_scripts import mode
p = argparse.ArgumentParser()
mode.add_argument(p)
p.add_argument("--exit", type=int, default=0)
p.add_argument("--sleep", type=float, default=0)
p.add_argument("--console")
args = p.parse_args()
a = mode.apply(args)
if a.done:
    sys.exit(a.exit_code)
print("fixture ran mode=" + a.mode + " console=" + os.environ.get("JUNEAU_TERMINAL_CONSOLE", "-"), flush=True)
time.sleep(args.sleep)
sys.exit(args.exit)
"""


class ModeSplitCase(WatchCase):

    ENGINE = "shim"

    def setUp(self):
        super().setUp()
        self.fixture = self.tmp / "fixture.py"
        self.fixture.write_text(FIXTURE % str(PYTHON_SRC))
        self.env["JUNEAU_TERMINAL_ENGINE"] = self.ENGINE
        self.env["PYTHONDONTWRITEBYTECODE"] = "1"

    def fx(self, *args, env=None, timeout=60):
        r = subprocess.run([sys.executable, str(self.fixture), *args], capture_output=True, text=True, env=env or self.env,
                           timeout=timeout)
        return r.returncode, r.stdout, r.stderr

    def running_pane(self, timeout=15):
        """The leader pane whose run is going, once there is one."""
        deadline = time.time() + timeout
        while time.time() < deadline:
            for l in self.tmux("list-panes", "-s", "-t", "t", "-F", "#{pane_id} #{@tmux_state}").splitlines():
                if l.endswith(" running"):
                    return l.split()[0]
            time.sleep(0.1)
        self.fail("no running leader pane")

    def last_exit(self, out):
        return int(re.findall(r"^exit=(\d+)$", out, re.M)[-1])


@requires_tmux
class SplitInsideTest(ModeSplitCase):

    def test_split_runs_the_script_as_dash_and_returns_its_exit_code(self):
        code, out, _ = self.fx("--mode", "split", "--exit", "3")
        self.assertEqual(code, 3)
        self.assertEqual(self.last_exit(out), 3)
        self.assertIn("fixture ran mode=dash", out)

    def test_auto_without_a_tty_inside_tmux_takes_the_detached_path(self):
        code, out, _ = self.fx()
        self.assertEqual(code, 0)
        self.assertIn("tail -f ", out)
        self.assertIn("fixture ran mode=dash", out)

    def test_explicit_dash_and_raw_do_not_open_panes(self):
        for m, console in (("dash", "-"), ("raw", "full")):
            with self.subTest(mode=m):
                code, out, _ = self.fx("--mode", m)
                self.assertEqual(code, 0)
                self.assertIn("fixture ran mode=%s" % m, out)
                self.assertNotIn("tail -f", out)
                if m == "raw":
                    self.assertIn("console=full", out)
        self.assertEqual(self.panes(), 1)

    def test_inherited_session_keeps_an_explicit_split_at_dash(self):
        env = dict(self.env, JUNEAU_TERMINAL_SESSION="1")
        code, out, _ = self.fx("--mode", "split", env=env)
        self.assertEqual(code, 0)
        self.assertIn("fixture ran mode=dash", out)
        self.assertNotIn("tail -f", out)

    def test_hand_wrapped_run_stays_at_dash(self):
        code, out, _ = self.fx(env=dict(self.env, TMUX_PANE_RUN_ID="x"))
        self.assertIn("fixture ran mode=dash", out)
        self.assertNotIn("tail -f", out)

    def test_bad_mode_exits_2(self):
        self.assertEqual(self.fx("--mode", "bogus")[0], 2)

    def test_bad_environment_mode_exits_2(self):
        code, _, err = self.fx(env=dict(self.env, JUNEAU_TERMINAL_MODE="bogus"))
        self.assertEqual(code, 2)
        self.assertIn("JUNEAU_TERMINAL_MODE", err)

    def start_split(self, *args, env=None):
        p = subprocess.Popen([sys.executable, str(self.fixture), "--mode", "split", *args], stdout=subprocess.PIPE,
                             stderr=subprocess.DEVNULL, text=True, env=env or self.env)
        self.addCleanup(p.kill)
        return p

    @slow_tmux
    def test_toolbar_on_gives_a_third_pane_while_running(self):
        self.start_split("--sleep", "30", env=dict(self.env, JUNEAU_TERMINAL_TOOLBAR="1"))
        self.running_pane()
        self.assertEqual(self.wait_panes(4), 4)     # agent (hidden) + leader + log + toolbar

    def start_in_window(self, *args):
        """Run the fixture as split in a window of its own, on that pane's tty (a launcher a run may hide); returns the launcher pane."""
        keys = ("JUNEAU_TERMINAL_LOG_DIR", "JUNEAU_TERMINAL_TOOLBAR", "JUNEAU_TERMINAL_ENGINE", "PYTHONDONTWRITEBYTECODE", "TMPDIR", "TMUX_PANE_GRACE")
        cmd = "env %s %s %s --mode split %s; sleep 60" % (" ".join("%s=%s" % (k, shlex.quote(self.env[k])) for k in keys if k in self.env),
                                                         sys.executable, shlex.quote(str(self.fixture)), " ".join(args))
        return self.tmux("new-window", "-d", "-P", "-F", "#{pane_id}", "-t", "t", "sh -c %s" % shlex.quote(cmd)).strip()

    @slow_tmux
    def test_launcher_is_hidden_while_running_and_every_pane_closes_at_the_end(self):
        launcher = self.start_in_window("--sleep", "6")
        pane = self.running_pane()
        window = self.tmux("display-message", "-p", "-t", pane, "#{window_id}").strip()
        shown = self.tmux("list-panes", "-t", window, "-F", "#{pane_id}").split()
        self.assertNotIn(launcher, shown)           # the launcher pane is not in the run's window
        self.assertEqual(len(shown), 2)             # leader + log (toolbar is off)
        deadline = time.time() + 40
        while time.time() < deadline and "exit=0" not in self.tmux("capture-pane", "-p", "-t", launcher):
            time.sleep(0.3)
        text = self.tmux("capture-pane", "-p", "-t", launcher)
        self.assertIn("fixture ran mode=dash", text)    # the final board is printed where the run was started
        self.assertEqual(self.last_exit(text), 0)
        self.assertEqual(self.wait_panes(2), 2)         # the launcher's window only (plus the session's first pane)

    def wait_text(self, pane, needle, timeout=40):
        deadline = time.time() + timeout
        while time.time() < deadline and needle not in self.tmux("capture-pane", "-p", "-t", pane):
            time.sleep(0.3)
        return self.tmux("capture-pane", "-p", "-t", pane)

    @slow_tmux
    def test_cancel_and_refusal_work_with_the_launcher_hidden(self):
        launcher = self.start_in_window("--sleep", "60")
        pane = self.running_pane()
        time.sleep(1.5)
        env = dict(self.env, TMUX_PANE=launcher)        # a second run started from the launcher's own pane
        code, _, err = self.fx("--mode", "split", env=env)
        self.assertEqual(code, 3)
        self.assertRegex(err, r"cancel: tmux-pane\.sh cancel %\d+")
        code, out = self.pane_sub("cancel", pane)
        self.assertEqual(code, 0, out)
        self.assertEqual(self.last_exit(self.wait_text(launcher, "exit=130")), 130)

    @slow_tmux
    def test_ctrl_c_in_the_run_window_ends_the_run(self):
        launcher = self.start_in_window("--sleep", "60")
        pane = self.running_pane()
        time.sleep(1.5)
        self.tmux("send-keys", "-t", pane, "C-c")
        text = self.wait_text(launcher, "exit=")
        self.assertRegex(text, r"exit=(130|2)\b")

    @slow_tmux
    def test_a_failed_run_ends_with_the_log_path_and_the_error_lines(self):
        code, out, _ = self.fx("--mode", "split", "--keep", "never", "--exit", "4")
        self.assertEqual(code, 4)
        self.assertIn("full log: ", out)
        self.assertEqual(self.panes(), 1)

    def kept_states(self):
        return [l for l in self.tmux("list-panes", "-s", "-t", "t", "-F", "#{@tmux_state}").splitlines() if l.startswith("kept")]

    @slow_tmux
    def test_by_default_a_failing_run_keeps_its_panes_and_a_passing_run_closes_them(self):
        code, out, _ = self.fx("--mode", "split", "--exit", "4")
        self.assertEqual(code, 4)
        self.assertIn("full log: ", out)                    # the final board and the log path print even though the panes stay
        self.assertEqual(self.kept_states(), ["kept (exit 4)"])
        self.assertEqual(self.panes(), 3)                   # the caller's pane, the kept leader and the log
        self.pane_sub("close", "--kept")
        code, _, _ = self.fx("--mode", "split")
        self.assertEqual(code, 0)
        self.assertEqual(self.panes(), 1)

    @slow_tmux
    def test_keep_never_closes_a_failing_run(self):
        self.assertEqual(self.fx("--mode", "split", "--keep", "never", "--exit", "4")[0], 4)
        self.assertEqual(self.panes(), 1)
        env = dict(self.env, JUNEAU_TERMINAL_KEEP="never")
        self.assertEqual(self.fx("--mode", "split", "--exit", "4", env=env)[0], 4)
        self.assertEqual(self.panes(), 1)

    @slow_tmux
    def test_keep_always_keeps_a_passing_run(self):
        self.assertEqual(self.fx("--mode", "split", "--keep", "always")[0], 0)
        self.assertEqual(self.kept_states(), ["kept (exit 0)"])
        env = dict(self.env, JUNEAU_TERMINAL_KEEP="always")
        self.assertEqual(self.fx("--mode", "split", "--keep", "never", env=env)[0], 0)    # the flag wins over the variable
        self.assertEqual(self.panes(), 1)

    def test_bad_environment_keep_exits_2(self):
        code, _, err = self.fx(env=dict(self.env, JUNEAU_TERMINAL_KEEP="bogus"))
        self.assertEqual(code, 2)
        self.assertIn("JUNEAU_TERMINAL_KEEP", err)

    @slow_tmux
    def test_second_run_in_the_window_is_refused(self):
        first = subprocess.Popen([sys.executable, str(self.fixture), "--mode", "split", "--sleep", "30"], stdout=subprocess.PIPE,
                                 stderr=subprocess.DEVNULL, text=True, env=self.env)
        self.addCleanup(first.kill)
        self.running_pane()
        code, _, err = self.fx("--mode", "split")
        self.assertEqual(code, 3)
        self.assertIn("another run is still running in this window", err)
        self.assertRegex(err, r"cancel: tmux-pane\.sh cancel %\d+")

    @slow_tmux
    def test_cancel_through_tmux_pane_stops_the_inner_run(self):
        first = subprocess.Popen([sys.executable, str(self.fixture), "--mode", "split", "--sleep", "30"], stdout=subprocess.PIPE,
                                 stderr=subprocess.DEVNULL, text=True, env=self.env)
        self.addCleanup(first.kill)
        pane = self.running_pane()
        time.sleep(1.5)     # the run registers itself a moment after the pane shows running
        code, out = self.pane_sub("cancel", pane)
        self.assertEqual(code, 0, out)
        out, _ = first.communicate(timeout=30)
        self.assertEqual(first.returncode, 130)


@requires_tmux
class SplitDirectEngineTest(SplitInsideTest):
    """The same cases through the in-process engine."""
    ENGINE = "direct"


@requires_tmux
@slow_tmux
class SplitNewSessionTest(ModeSplitCase):
    """Outside tmux: a new tmux session on the default socket (`-L default`), which TMUX_TMPDIR points into the test's temp dir."""

    def setUp(self):
        super().setUp()
        sockdir = Path(tempfile.mkdtemp(prefix="jt-", dir="/tmp"))
        self.addCleanup(shutil.rmtree, str(sockdir), True)
        self.addCleanup(lambda: subprocess.run(["tmux", "-L", "default", "kill-server"], env=self.nest_env, capture_output=True))
        self.nest_env = {k: v for k, v in self.env.items() if k not in ("TMUX", "TMUX_PANE")}
        self.nest_env.update({"TMUX_TMPDIR": str(sockdir), "JUNEAU_TERMINAL_ATTACH_CMD": "true", "JUNEAU_TERMINAL_TOOLBAR": "1"})

    def sessions(self):
        env = {k: v for k, v in self.nest_env.items() if k != "TMUX"}
        r = subprocess.run(["tmux", "-L", "default", "list-sessions", "-F", "#{session_name}"], capture_output=True, text=True, env=env)
        return r.stdout.split()

    def pty_run(self, *args):
        """Run the fixture with stdin and stdout on a pty, so auto and split see a terminal."""
        import pty
        master, slave = pty.openpty()
        try:
            r = subprocess.run([sys.executable, str(self.fixture), *args], stdin=slave, stdout=slave, stderr=subprocess.PIPE,
                               text=True, env=self.nest_env, timeout=60)
        finally:
            os.close(slave)
            os.close(master)
        return r.returncode, r.stderr

    def test_stale_tmux_variable_still_creates_a_session(self):
        self.nest_env["TMUX"] = "/nonexistent/sock,1,0"
        code, _ = self.pty_run("--mode", "split", "--sleep", "8")     # the attach stub returns at once; the run must outlive the check
        self.assertEqual(code, 0)
        names = self.sessions()
        self.assertTrue(any(n.startswith("jt-fixture-") for n in names), names)

    def test_no_tty_outside_tmux_falls_back_to_raw_with_a_warning(self):
        r = subprocess.run([sys.executable, str(self.fixture), "--mode", "split"], capture_output=True, text=True, env=self.nest_env,
                           stdin=subprocess.DEVNULL, timeout=60)
        self.assertIn("fixture ran mode=raw", r.stdout)
        self.assertIn("using raw", r.stderr)

    def test_a_finished_unattached_session_is_replaced(self):
        self.pty_run("--mode", "split")
        first = [n for n in self.sessions() if n.startswith("jt-fixture-")]
        time.sleep(3)
        self.pty_run("--mode", "split", "--sleep", "8")
        second = [n for n in self.sessions() if n.startswith("jt-fixture-")]
        self.assertTrue(second)
        self.assertFalse(set(first) & set(second) and len(second) > 1, (first, second))

    def test_the_final_board_and_exit_code_come_back_to_the_launching_terminal(self):
        import pty
        import select
        self.nest_env[ "JUNEAU_TERMINAL_ATTACH_CMD"] = "sh -c 'while tmux -L default has-session -t \"$1\" 2>/dev/null; do sleep 0.3; done' attach"
        master, slave = pty.openpty()
        try:
            p = subprocess.Popen([sys.executable, str(self.fixture), "--mode", "split", "--keep", "never", "--exit", "3"], stdin=slave, stdout=slave,
                                 stderr=subprocess.PIPE, text=True, env=self.nest_env)
            code = p.wait(timeout=90)
            data = b""
            while select.select([master], [], [], 0.5)[0]:
                chunk = os.read(master, 65536)
                if not chunk:
                    break
                data += chunk
        finally:
            os.close(slave)
            os.close(master)
        self.assertEqual(code, 3)
        text = data.decode(errors="replace")
        self.assertIn("fixture ran mode=dash", text)
        self.assertIn("exit=3", text)
        self.assertIn("full log: ", text)

    def test_a_failing_run_leaves_the_session_up_and_the_outer_terminal_gets_the_board_when_it_ends(self):
        import pty
        import select
        # The attached client stays on the kept panes; this stub plays the user who reads them, then closes them (kills the session).
        self.nest_env["JUNEAU_TERMINAL_ATTACH_CMD"] = (
            "sh -c 'until tmux -L default list-panes -s -t \"$1\" -F \"#{@tmux_state}\" | grep -q kept; do sleep 0.2; done; sleep 2; "
            "tmux -L default list-panes -s -t \"$1\" -F \"#{pane_id}\" | wc -l > %s; tmux -L default kill-session -t \"$1\"' attach" % (self.tmp / "panes"))
        master, slave = pty.openpty()
        try:
            p = subprocess.Popen([sys.executable, str(self.fixture), "--mode", "split", "--exit", "3"], stdin=slave, stdout=slave,
                                 stderr=subprocess.PIPE, text=True, env=self.nest_env)
            code = p.wait(timeout=90)
            data = b""
            while select.select([master], [], [], 0.5)[0]:
                chunk = os.read(master, 65536)
                if not chunk:
                    break
                data += chunk
        finally:
            os.close(slave)
            os.close(master)
        self.assertEqual(code, 3)
        self.assertGreaterEqual(int((self.tmp / "panes").read_text()), 2)    # the leader and the log were still up for the user
        text = data.decode(errors="replace")
        after = text[text.rindex("\033[?1049l"):]
        self.assertIn("full log: ", after)
        self.assertEqual(after.count("exit=3"), 1)

    def test_attach_frames_stay_off_the_main_screen_and_the_launcher_is_hidden_at_attach(self):
        import pty
        import select
        seen = self.tmp_seen = Path(tempfile.mkdtemp(prefix="jt-seen-", dir="/tmp")) / "windows"
        self.addCleanup(shutil.rmtree, str(seen.parent), True)
        self.nest_env["JUNEAU_TERMINAL_ATTACH_CMD"] = (
            "sh -c 'tmux -L default list-windows -t \"$1\" -F \"#{window_name}\" > %s; printf \"FRAMEROW1\\nFRAMEROW2\\n\"; "
            "while tmux -L default has-session -t \"$1\" 2>/dev/null; do sleep 0.3; done' attach" % seen)
        master, slave = pty.openpty()
        try:
            p = subprocess.Popen([sys.executable, str(self.fixture), "--mode", "split"], stdin=slave, stdout=slave,
                                 stderr=subprocess.PIPE, text=True, env=self.nest_env)
            p.wait(timeout=90)
            data = b""
            while select.select([master], [], [], 0.5)[0]:
                chunk = os.read(master, 65536)
                if not chunk:
                    break
                data += chunk
        finally:
            os.close(slave)
            os.close(master)
        text = data.decode(errors="replace")
        self.assertIn("launcher", seen.read_text().split())          # already in a window of its own when the client attached
        on, off = text.index("\033[?1049h"), text.rindex("\033[?1049l")
        self.assertIn("FRAMEROW1", text[on:off])
        self.assertNotIn("FRAMEROW", text[off:])                      # nothing of the live frames after the alternate screen is left
        self.assertNotIn("FRAMEROW", text[:on])
        self.assertEqual(text[off:].count("fixture ran"), 1)
