#!/usr/bin/env python3
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
"""juneau_terminal_scripts.java, lock and proc."""

import os
import signal
import subprocess
import sys
import tempfile
import threading
import time
import unittest
from pathlib import Path
from unittest import mock

import scripts_support

from juneau_terminal_scripts import _base, java, lock, proc


def jdk(root, name, version):
    home = Path(root) / name
    home.mkdir(parents=True)
    (home / "release").write_text(f'IMPLEMENTOR="Test"\nJAVA_VERSION="{version}"\n', encoding="utf-8")
    return home


class JavaTest(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.root = Path(tmp.name)
        patcher = mock.patch.object(java, "_roots", lambda: [self.root / "jdk"])
        patcher.start()
        self.addCleanup(patcher.stop)
        patcher = mock.patch.object(java.sys, "platform", "linux")
        patcher.start()
        self.addCleanup(patcher.stop)

    def test_release_major(self):
        self.assertEqual(java.release_major(jdk(self.root, "a", "17.0.12")), 17)
        self.assertEqual(java.release_major(jdk(self.root, "b", "1.8.0_402")), 8)
        self.assertEqual(java.release_major(jdk(self.root, "c", "21")), 21)
        self.assertIsNone(java.release_major(self.root / "missing"))

    def test_order(self):
        j17 = jdk(self.root / "jdk", "openjdk-17", "17.0.12")
        j21 = jdk(self.root / "jdk", "openjdk-21", "21.0.4")
        with mock.patch.dict(os.environ, {"JAVA_HOME": str(j21)}):
            self.assertEqual(java.find_home(21), str(j21))
            self.assertEqual(java.find_home(17), str(j17))     # a JAVA_HOME of the wrong major is passed over
            with mock.patch.dict(os.environ, {"MY_JDK": "/explicit"}):
                self.assertEqual(java.find_home(17, override_env="MY_JDK"), "/explicit")
            self.assertIsNone(java.find_home(11))

    def test_macos_layout_and_java_home_tool(self):
        mac = jdk(self.root / "jdk" / "temurin-17.jdk" / "Contents", "Home", "17.0.12")
        with mock.patch.dict(os.environ, {"JAVA_HOME": ""}):
            self.assertEqual(java.find_home(17), str(mac))
        tool = jdk(self.root, "tool-17", "17.0.1")
        done = subprocess.CompletedProcess([], 0, stdout=f"{tool}\n", stderr="")
        with mock.patch.dict(os.environ, {"JAVA_HOME": ""}), mock.patch.object(java.sys, "platform", "darwin"), \
                mock.patch.object(java.os.path, "exists", return_value=True), \
                mock.patch.object(java.subprocess, "run", return_value=done) as ran:
            self.assertEqual(java.find_home(17), str(tool))
        self.assertEqual(ran.call_args.args[0], ["/usr/libexec/java_home", "-v", "17"])


# Holds an exclusive flock on argv[1] from another process: prints "held", then lets go after argv[2] seconds, or
# when its stdin closes if argv[2] is negative.
HOLDER = r"""
import fcntl, os, sys, time
fd = os.open(sys.argv[1], os.O_RDWR | os.O_CREAT, 0o644)
fcntl.flock(fd, fcntl.LOCK_EX)
print("held", flush=True)
if float(sys.argv[2]) < 0:
    sys.stdin.readline()
else:
    time.sleep(float(sys.argv[2]))
"""


class LockTest(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.path = Path(tmp.name) / "sub" / "push.lock"

    def hold(self, seconds=-1):
        """Hold the lock from a child process, so the result is the same on Linux and macOS."""
        self.path.parent.mkdir(parents=True, exist_ok=True)
        child = subprocess.Popen([sys.executable, "-c", HOLDER, str(self.path), str(seconds)],
                                 stdin=subprocess.PIPE, stdout=subprocess.PIPE, text=True)
        self.addCleanup(child.wait, 10)
        self.addCleanup(child.stdin.close)
        self.addCleanup(child.stdout.close)
        self.assertEqual(child.stdout.readline(), "held\n")
        return child

    def test_creates_parent_and_releases(self):
        with lock.flock(self.path):
            self.assertTrue(self.path.exists())
        with lock.flock(self.path, blocking=False):
            pass

    def test_non_blocking_busy(self):
        self.hold()
        with self.assertRaises(lock.LockBusy) as cm:
            with lock.flock(self.path, blocking=False):
                pass
        self.assertIn(str(self.path), str(cm.exception))

    def test_blocking_with_timeout_busy(self):
        self.hold()
        t0 = time.monotonic()
        with self.assertRaises(lock.LockBusy):
            with lock.flock(self.path, timeout=0.3, poll=0.05):
                pass
        self.assertGreaterEqual(time.monotonic() - t0, 0.25)

    def test_blocking_with_timeout_acquires_when_released(self):
        self.hold(0.2)
        with lock.flock(self.path, timeout=5, poll=0.05):
            pass

    def test_windows(self):
        with mock.patch.object(_base.os, "name", "nt"), self.assertRaises(NotImplementedError):
            with lock.flock(self.path):
                pass


CHILD = r"""
import os, signal, subprocess, sys, time
ready, mode = sys.argv[1], sys.argv[2]
if mode == "ignore":
    signal.signal(signal.SIGTERM, signal.SIG_IGN)
grandchild = subprocess.Popen([sys.executable, "-c", "import signal, time; signal.signal(signal.SIGTERM, signal.SIG_IGN); time.sleep(60)"])
with open(ready, "w") as f:
    f.write(str(grandchild.pid))
time.sleep(60)
"""


def gone(pid, wait=5.0):
    deadline = time.monotonic() + wait
    while time.monotonic() < deadline:
        try:
            os.kill(pid, 0)
        except ProcessLookupError:
            return True
        time.sleep(0.05)
    return False


class RunGroupTest(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.ready = Path(tmp.name) / "ready"
        # A test process launched from a background shell inherits SIGINT as SIG_IGN, so the os.kill(SIGINT) below
        # would never raise KeyboardInterrupt; make the outcome independent of how the suite was launched.
        self.addCleanup(signal.signal, signal.SIGINT, signal.signal(signal.SIGINT, signal.default_int_handler))

    def interrupt_when_ready(self):
        def watch():
            while not self.ready.exists() or not self.ready.read_text():
                time.sleep(0.02)
            os.kill(os.getpid(), signal.SIGINT)     # a real signal: interrupt_main() can't break out of waitpid
        threading.Thread(target=watch, daemon=True).start()

    def test_exit_code(self):
        r = proc.run_group([sys.executable, "-c", "import sys; sys.exit(4)"])
        self.assertEqual((r.ok, r.code), (False, 4))
        self.assertEqual(proc.run_group(["/nonexistent/tool"]).code, 127)

    def test_kills_a_group_that_ignores_sigterm(self):
        self.interrupt_when_ready()
        with self.assertRaises(KeyboardInterrupt):
            proc.run_group([sys.executable, "-c", CHILD, str(self.ready), "ignore"], grace=0.5)
        self.assertTrue(gone(int(self.ready.read_text())), "the grandchild survived killpg")

    def test_cooperative_child_is_not_killed(self):
        self.interrupt_when_ready()
        with mock.patch.object(proc.os, "killpg") as killpg, self.assertRaises(KeyboardInterrupt):
            proc.run_group([sys.executable, "-c", CHILD, str(self.ready), "exit"], grace=10)
        killpg.assert_not_called()
        os.kill(int(self.ready.read_text()), signal.SIGKILL)   # the grandchild ignores SIGTERM; tidy it up

    def test_eperm_from_killpg_still_reaps_the_child(self):
        # macOS answers killpg with EPERM, not ESRCH, for a group that is exiting (only a zombie leader left)
        child = mock.Mock(pid=4242)
        child.wait.side_effect = [KeyboardInterrupt(), subprocess.TimeoutExpired("t", 0.1), 0]
        with mock.patch.object(proc.subprocess, "Popen", return_value=child), \
                mock.patch.object(proc.os, "killpg", side_effect=PermissionError(1, "Operation not permitted")), \
                self.assertRaises(KeyboardInterrupt):
            proc.run_group(["t"], grace=0.1)
        self.assertEqual(child.wait.call_count, 3)

    def test_windows(self):
        with mock.patch.object(_base.os, "name", "nt"), self.assertRaises(NotImplementedError):
            proc.run_group(["true"])


if __name__ == "__main__":
    unittest.main()
