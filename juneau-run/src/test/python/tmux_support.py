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
"""Private-tmux-server base classes and skip gates for the side-pane script tests."""

import os
import shutil
import subprocess
import sys
import tempfile
import time
import unittest
import uuid
from pathlib import Path

BIN = Path(__file__).resolve().parents[2] / "main" / "python" / "juneau_scripts" / "bin"
TMUX_PANE_SH = BIN / "tmux-pane.sh"
WATCH_RUN_SH = BIN / "watch-run.sh"


def _probe():
    """None when a private `tmux -L` server can start and be killed here, else the reason."""
    if not shutil.which("tmux"):
        return "tmux is not on PATH"
    sock = "juneau-probe-" + uuid.uuid4().hex[:8]
    try:
        r = subprocess.run(["tmux", "-L", sock, "new-session", "-d", "-s", "p", "true"],
                           capture_output=True, timeout=15)
        subprocess.run(["tmux", "-L", sock, "kill-server"], capture_output=True, timeout=15)
    except (OSError, subprocess.SubprocessError) as e:
        return "cannot start a private tmux server: %s" % e
    return None if r.returncode == 0 else "cannot start a private tmux server (sandbox?)"


def skip_reason():
    """Why the side-pane tests skip, or None to run them (macOS, or JUNEAU_RUN_TMUX_TESTS=1)."""
    if sys.platform != "darwin" and os.environ.get("JUNEAU_RUN_TMUX_TESTS") != "1":
        return "side-pane tests run on macOS, or anywhere with JUNEAU_RUN_TMUX_TESTS=1"
    return _probe()


_REASON = skip_reason()
requires_tmux = unittest.skipIf(_REASON is not None, _REASON or "")
# The cancel, reap, follower, layout, kept-pane and equal-width cases open several panes or wait on timers (TMUX_PANE_GRACE, sleeping probes); they run only on request.
# The cases left unmarked are the default smoke set: a pane opening and returning its exit code, list/close on kept panes, watch-run.sh end to end.
slow_tmux = unittest.skipUnless(os.environ.get("JUNEAU_RUN_TMUX_TESTS") == "1",
                                "slow side-pane test; set JUNEAU_RUN_TMUX_TESTS=1 to run it")


class PaneCase(unittest.TestCase):
    """Per test: a private tmux server (session `t`), the env the scripts run in, and the helpers.

    Subclasses set SCRIPT (a Path).  `run_cmd` is the board fixture's bare call (it cannot be named `run`, which
    would shadow TestCase.run)."""
    SCRIPT = TMUX_PANE_SH

    def setUp(self):
        tmp = Path(tempfile.mkdtemp(prefix="juneau-pane-"))
        self.addCleanup(shutil.rmtree, str(tmp), True)
        self.tmp = tmp
        self.sock = "juneau-pane-" + uuid.uuid4().hex[:8]
        subprocess.run(["tmux", "-L", self.sock, "new-session", "-d", "-s", "t", "-x", "200", "-y", "50"], check=True)
        self.addCleanup(subprocess.run, ["tmux", "-L", self.sock, "kill-server"], check=False)
        path = subprocess.run(["tmux", "-L", self.sock, "display", "-p", "#{socket_path},#{pid},0"],
                              capture_output=True, text=True, check=True).stdout.strip()
        env = {k: v for k, v in os.environ.items() if not k.startswith(("JUNEAU_RUN_", "TMUX_PANE_RUN_"))}
        self.agent = subprocess.run(["tmux", "-L", self.sock, "display", "-p", "-t", "t", "#{pane_id}"],
                                    capture_output=True, text=True, check=True).stdout.strip()
        # TMUX_PANE must name the throwaway server's pane, never one inherited from the tmux the tests were started in.
        env.update({"TMUX": path, "TMPDIR": str(tmp / "tmp"), "TMUX_PANE_GRACE": "2", "TMUX_PANE": self.agent})
        (tmp / "tmp").mkdir()
        self.env = env

    def run_cmd(self, *cmd, flags=()):
        r = subprocess.run([str(self.SCRIPT), "-k", "never", *flags, "--", *cmd], capture_output=True, text=True,
                           env=self.env, timeout=60)
        return r.returncode, r.stdout

    def start(self, *args):
        return subprocess.Popen([str(self.SCRIPT), *args], stdout=subprocess.PIPE, text=True, env=self.env)

    def panes(self):
        out = subprocess.run(["tmux", "-L", self.sock, "list-panes", "-s", "-t", "t", "-F", "#{pane_id}"],
                             capture_output=True, text=True, check=True).stdout
        return len(out.split())

    def sub(self, *args):
        r = subprocess.run([str(self.SCRIPT), *args], capture_output=True, text=True, env=self.env, timeout=60)
        return r.returncode, r.stdout + r.stderr

    def tmux(self, *args):
        return subprocess.run(["tmux", "-L", self.sock, *args], capture_output=True, text=True).stdout

    def wait_panes(self, n, timeout=10):
        deadline = time.time() + timeout
        while self.panes() != n and time.time() < deadline:
            time.sleep(0.1)
        return self.panes()


class WatchCase(PaneCase):
    """PaneCase for watch-run.sh: the log directory lives under the test's temp dir."""
    SCRIPT = WATCH_RUN_SH

    def setUp(self):
        super().setUp()
        self.logs = self.tmp / "logs"
        self.env["JUNEAU_RUN_LOG_DIR"] = str(self.logs)

    def popen(self, *args):
        return subprocess.Popen([str(self.SCRIPT), *args], stdout=subprocess.PIPE, text=True, env=self.env)

    def pane_sub(self, *args):
        """Run tmux-pane.sh (the runner behind watch-run.sh) and return (returncode, stdout + stderr)."""
        r = subprocess.run([str(TMUX_PANE_SH), *args], capture_output=True, text=True, env=self.env, timeout=60)
        return r.returncode, r.stdout + r.stderr

    def run_watch(self, *args):
        r = subprocess.run([str(self.SCRIPT), *args], capture_output=True, text=True, env=self.env, timeout=60)
        return r.returncode, r.stdout, r.stderr
