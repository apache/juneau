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
"""Per-call startup cost of the shims and the cost of the toolbar's 1 s poll loop (measured against the old bash scripts' baseline)."""

import os
import shutil
import statistics
import subprocess
import sys
import time
import unittest

from tmux_support import BIN, PaneCase, requires_tmux

TOOLBAR_SH = BIN / "tmux-toolbar.sh"
RUNS = 20


def median_ms(cmd, runs=RUNS):
    """Median wall time in milliseconds of `runs` executions of cmd (output discarded)."""
    times = []
    for _ in range(runs):
        t = time.perf_counter()
        subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=False)
        times.append((time.perf_counter() - t) * 1000)
    return statistics.median(times)


def startup_ms():
    """The cost every key binding and run-shell pays: one shim call that touches no tmux."""
    return median_ms([str(TOOLBAR_SH), "--print-hit", "5"])


def startup_ms_with(python):
    """The same call through an explicit interpreter (the engine, bypassing the shim's `python3` lookup)."""
    return median_ms([python, str(BIN / "engine.py"), "tmux-toolbar", "--print-hit", "5"])


STARTUP_LIMIT_MS = 120
SYSTEM_PYTHON_LIMIT_MS = 200
POLL_CPU_LIMIT_S = 0.30
POLL_TMUX_CALL_LIMIT = 15


class StartupCostTest(unittest.TestCase):
    def test_the_shim_starts_within_the_limit(self):
        ms = startup_ms()
        print("\nshim startup median: %.1f ms (limit %d)" % (ms, STARTUP_LIMIT_MS))
        self.assertLessEqual(ms, STARTUP_LIMIT_MS)

    @unittest.skipUnless(os.path.exists("/usr/bin/python3"), "no system python3")
    def test_the_oldest_python_starts_within_its_limit(self):
        ms = startup_ms_with("/usr/bin/python3")
        print("\n/usr/bin/python3 startup median: %.1f ms (limit %d)" % (ms, SYSTEM_PYTHON_LIMIT_MS))
        self.assertLessEqual(ms, SYSTEM_PYTHON_LIMIT_MS)

    def test_the_startup_path_does_not_import_the_heavy_modules(self):
        r = subprocess.run([sys.executable, "-X", "importtime", str(BIN / "engine.py"), "tmux-toolbar", "--print-hit", "5"],
                           capture_output=True, text=True)
        imported = {line.rsplit("|", 1)[-1].strip() for line in r.stderr.splitlines() if "|" in line}
        for heavy in ("json", "tempfile", "random", "glob", "shutil"):
            with self.subTest(module=heavy):
                self.assertNotIn(heavy, imported)


def _cpu_seconds(pid):
    """CPU time of a process from `ps -o time=` (M:SS.cc on macOS, HH:MM:SS on Linux)."""
    text = subprocess.run(["ps", "-o", "time=", "-p", str(pid)], capture_output=True, text=True).stdout.strip()
    total = 0.0
    for part in text.split(":"):
        total = total * 60 + float(part)
    return total


@requires_tmux
class PollCostTest(PaneCase):
    SCRIPT = TOOLBAR_SH

    def new_pane(self, cmd):
        return self.tmux("split-window", "-d", "-v", "-t", self.agent, "-P", "-F", "#{pane_id}", "bash -c %r" % cmd).strip()

    def test_an_idle_toolbar_is_cheap(self):
        calls = self.tmp / "tmux-calls.log"
        shim = self.tmp / "shim"
        shim.mkdir()
        (shim / "tmux").write_text('#!/bin/sh\necho x >> %s\nexec %s "$@"\n' % (calls, shutil.which("tmux")))
        (shim / "tmux").chmod(0o755)
        leader = self.new_pane("sleep 300")
        log = self.new_pane("sleep 300")
        for k, v in {"@tmux_pane": "1", "@tmux_state": "running"}.items():
            self.tmux("set-option", "-p", "-t", leader, k, v)
        for k, v in {"@tmux_pane": "1", "@tmux_state": "follower", "@tmux_parent": leader}.items():
            self.tmux("set-option", "-p", "-t", log, k, v)
        bar = self.new_pane("PATH=%s:$PATH TMUX_PANE_RUN_ID=%s TMUX_PANE_RUN_CANCEL=true exec %s --title demo" % (shim, leader, TOOLBAR_SH))
        time.sleep(3)                                       # let it start and settle
        pid = int(self.tmux("display-message", "-p", "-t", bar, "#{pane_pid}").strip())
        c0, t0 = len(calls.read_text().splitlines()) if calls.exists() else 0, _cpu_seconds(pid)
        time.sleep(10)
        c1, t1 = len(calls.read_text().splitlines()), _cpu_seconds(pid)
        print("\ntoolbar over 10 s: cpu %.2f s (limit %.2f), tmux calls %d (limit %d)" % (t1 - t0, POLL_CPU_LIMIT_S, c1 - c0, POLL_TMUX_CALL_LIMIT))
        self.assertLessEqual(t1 - t0, POLL_CPU_LIMIT_S)
        self.assertLessEqual(c1 - c0, POLL_TMUX_CALL_LIMIT)


if __name__ == "__main__":
    unittest.main()
