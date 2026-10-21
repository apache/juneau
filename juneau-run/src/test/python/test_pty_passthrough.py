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
"""Pass-through run_tool on a real terminal: the tool's own process group holds the foreground while it runs, the
foreground comes back however the tool ends, ^C and SIGTERM cancel, and ^Z stops the job as a shell would."""

import os
import re
import select
import signal
import subprocess
import sys
import tempfile
import time
import unittest
from pathlib import Path

from support import SCRIPT, jr

DEADLINE = 10.0

# A job-control shell in miniature: it leads the PTY's session and runs its arguments as one job in a process group of
# its own, in the foreground.  When the job stops it takes the terminal back, prints "SH stopped" and waits for a line
# (the test's "fg"), then gives the job the terminal again and resumes it.  Only the job sets its group: the usual
# parent-and-child pair of setpgid calls races on macOS, where the losing child's call can fail with EPERM.
SHELL = r"""
import os, signal, sys
signal.signal(signal.SIGTTOU, signal.SIG_IGN)
pid = os.fork()
if pid == 0:
    os.setpgid(0, 0)
    os.tcsetpgrp(0, os.getpgrp())
    signal.signal(signal.SIGTTOU, signal.SIG_DFL)
    os.execv(sys.executable, [sys.executable, *sys.argv[1:]])
while True:
    _, status = os.waitpid(pid, os.WUNTRACED)
    if not os.WIFSTOPPED(status):
        break
    os.tcsetpgrp(0, os.getpgrp())
    print("SH stopped", flush=True)
    sys.stdin.readline()
    os.tcsetpgrp(0, pid)
    os.killpg(pid, signal.SIGCONT)
print("SH exit", os.waitstatus_to_exitcode(status), flush=True)
"""

# The script under test: one pass-through run_tool of its arguments, then whether its own group has the terminal.
RUNNER = r"""
import os, sys
sys.path.insert(0, sys.argv[1])
import juneau_run as jr
print("R pid", os.getpid(), "fg", os.isatty(0) and os.tcgetpgrp(0) == os.getpgrp(), flush=True)
try:
    print("R exit", jr.run_tool(sys.argv[2:], "generic", "t", "T").exit, flush=True)
except KeyboardInterrupt:
    print("R cancelled", flush=True)
print("R back", os.isatty(0) and os.tcgetpgrp(0) == os.getpgrp(), flush=True)
"""

# The tool: whether it leads its own group and holds the terminal, then (with a pid file) a grandchild in its group,
# then one line read from its stdin.
TOOL = r"""
import os, subprocess, sys
print("T own", os.getpgrp() == os.getpid(), "fg", os.isatty(0) and os.tcgetpgrp(0) == os.getpgrp(), flush=True)
if len(sys.argv) > 1:
    kid = subprocess.Popen(["sleep", "30"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    with open(sys.argv[1], "w") as f:
        f.write(str(kid.pid))
print("T ready", flush=True)
print("T got", sys.stdin.readline().strip() or "-", flush=True)
"""


def _env():
    env = {k: v for k, v in os.environ.items()
           if not k.startswith(("RUN_", "JUNEAU_RUN_", "JUNEAU_PUSH_")) or k == "JUNEAU_RUN_LOG_DIR"}
    env["JUNEAU_RUN_HISTORY"] = "0"
    return env


def _gone(pid):
    """Waits up to the deadline for pid to stop existing."""
    end = time.monotonic() + DEADLINE
    while time.monotonic() < end:
        try:
            os.kill(pid, 0)
        except ProcessLookupError:
            return True
        time.sleep(0.05)
    return False


class Terminal:
    """SHELL running RUNNER running TOOL on a fresh PTY; the test reads what reaches the screen and types."""

    def __init__(self, *tool_args):
        self.master, slave = os.openpty()
        try:
            self.proc = subprocess.Popen(
                [sys.executable, "-c", SHELL, "-c", RUNNER, str(SCRIPT.parent), sys.executable, "-c", TOOL, *tool_args],
                stdin=slave, stdout=slave, stderr=slave, env=_env(), start_new_session=True,
                preexec_fn=jr._set_ctty, close_fds=True)
        finally:
            os.close(slave)
        self.screen = ""

    def wait_for(self, pattern):
        """Reads the screen until pattern (a regex) matches it; returns the match.  Fails after the deadline."""
        end = time.monotonic() + DEADLINE
        while time.monotonic() < end:
            m = re.search(pattern, self.screen)
            if m:
                return m
            if select.select([self.master], [], [], 0.05)[0]:
                try:
                    chunk = os.read(self.master, 4096)
                except OSError:     # EIO: every process has closed the slave side
                    break
                if not chunk:
                    break
                self.screen += chunk.decode("utf-8", errors="replace")
        raise AssertionError(f"{pattern!r} never reached the screen: {self.screen!r}")

    def wait_for_foreground(self, test):
        """Polls the terminal's foreground process group until test(pgid) holds.  Fails after the deadline."""
        end = time.monotonic() + DEADLINE
        while time.monotonic() < end:
            if test(os.tcgetpgrp(self.master)):
                return
            time.sleep(0.02)
        raise AssertionError(f"the foreground never changed: {self.screen!r}")

    def type(self, data):
        os.write(self.master, data)

    def close(self):
        if self.proc.poll() is None:
            os.killpg(self.proc.pid, signal.SIGKILL)
        self.proc.wait()
        os.close(self.master)


@unittest.skipUnless(os.name == "posix", "process groups and terminals are POSIX only")
class PassThroughTerminalTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())

    def terminal(self, *tool_args):
        t = Terminal(*tool_args)
        self.addCleanup(t.close)
        return t

    def test_the_tool_gets_its_own_group_and_the_foreground_and_reads_the_terminal(self):
        t = self.terminal()
        t.wait_for(r"R pid \d+ fg True")
        t.wait_for(r"T own True fg True")
        t.wait_for(r"T ready")
        t.type(b"hello\n")
        t.wait_for(r"T got hello")
        t.wait_for(r"R exit 0")
        t.wait_for(r"R back True")
        t.wait_for(r"SH exit 0")

    def test_ctrl_c_reaches_the_tool_and_cancels_with_the_foreground_restored(self):
        t = self.terminal()
        t.wait_for(r"T ready")
        t.type(b"\x03")
        t.wait_for(r"R cancelled")
        t.wait_for(r"R back True")
        t.wait_for(r"SH exit 0")

    def test_sigterm_to_the_runner_takes_down_the_tools_whole_group(self):
        pid_file = self.tmp / "kid.pid"
        t = self.terminal(str(pid_file))
        runner = int(t.wait_for(r"R pid (\d+)").group(1))
        t.wait_for(r"T ready")
        kid = int(pid_file.read_text())
        os.kill(runner, signal.SIGTERM)
        t.wait_for(r"R cancelled")
        t.wait_for(r"R back True")
        self.assertTrue(_gone(kid), "the tool's grandchild outlived the cancel")

    def test_ctrl_z_stops_the_job_and_fg_hands_the_terminal_back_to_the_tool(self):
        t = self.terminal()
        runner = int(t.wait_for(r"R pid (\d+)").group(1))
        t.wait_for(r"T ready")
        t.type(b"\x1a")
        t.wait_for(r"SH stopped")
        t.type(b"fg\n")
        t.wait_for_foreground(lambda pgid: pgid not in (t.proc.pid, runner))   # the tool's again, so it reads the line
        t.type(b"after\n")
        t.wait_for(r"T got after")
        t.wait_for(r"R exit 0")
        t.wait_for(r"R back True")
        t.wait_for(r"SH exit 0")


@unittest.skipUnless(os.name == "posix", "process groups are POSIX only")
class PassThroughWithoutTerminalTest(unittest.TestCase):
    def test_sigterm_takes_down_the_tools_whole_group(self):
        with tempfile.TemporaryDirectory() as tmp:
            pid_file = Path(tmp) / "kid.pid"
            proc = subprocess.Popen([sys.executable, "-c", RUNNER, str(SCRIPT.parent), sys.executable, "-c", TOOL,
                                     str(pid_file)],
                                    stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                    env=_env(), text=True)
            try:
                lines = []
                for line in proc.stdout:
                    lines.append(line)
                    if line.startswith("T ready"):
                        break
                self.assertIn("R pid", lines[0])
                self.assertIn("fg False", lines[0])
                self.assertIn("T own True fg False\n", lines)
                kid = int(pid_file.read_text())
                proc.send_signal(signal.SIGTERM)
                proc.wait(DEADLINE)     # stdin stays open: an EOF would end the tool before the cancel could
                self.assertIn("R cancelled", proc.stdout.read())
                self.assertTrue(_gone(kid), "the tool's grandchild outlived the cancel")
            finally:
                if proc.poll() is None:
                    proc.kill()
                proc.communicate()


if __name__ == "__main__":
    unittest.main()
