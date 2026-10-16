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
"""PTY input: --input stdin copies this process's stdin to the job's terminal, unchanged."""

import hashlib
import json
import os
import signal
import subprocess
import sys
import tempfile
import time
import unittest
from pathlib import Path

from support import SCRIPT, reset_run_state
from test_pty import GOLDEN, SCREEN, pty

DEADLINE = 10.0


def _env():
    env = {k: v for k, v in os.environ.items()
           if not k.startswith(("RUN_", "JUNEAU_RUN_")) or k == "JUNEAU_RUN_LOG_DIR"}
    env["JUNEAU_RUN_HISTORY"] = "0"
    return env


class Job:
    """One `--pty --input stdin` run of a Python snippet, with stdin a pipe the test writes to."""

    def __init__(self, tmp, code, *extra, ignored=(), blocked=(), sh=False):
        self.log, self.events = Path(tmp) / "out.log", Path(tmp) / "out.events.jsonl"
        self.proc = subprocess.Popen(
            [sys.executable, str(SCRIPT), "--console", "none", "--pty", "--size", "80x24", "--full-log", str(self.log),
             "--events", str(self.events), *extra, "--step", "tool", "generic", "--", *(["sh", "-c", code] if sh else [sys.executable, "-c", code])],
            stdin=subprocess.PIPE, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE, env=_env(),
            preexec_fn=(lambda: self._inherit(ignored, blocked)) if ignored or blocked else None)

    @staticmethod
    def _inherit(ignored, blocked):
        """In the runner, before exec: ignore and block signals as a backgrounded launcher or a JVM leaves them."""
        for n in ignored:
            signal.signal(n, signal.SIG_IGN)
        signal.pthread_sigmask(signal.SIG_BLOCK, blocked)

    def log_bytes(self):
        return self.log.read_bytes() if self.log.exists() else b""

    def wait_for(self, marker):
        """Polls the log every 20 ms until it holds marker; fails after the deadline."""
        end = time.monotonic() + DEADLINE
        while time.monotonic() < end:
            if marker in self.log_bytes():
                return
            if self.proc.poll() is not None:
                break
            time.sleep(0.02)
        self.kill()
        raise AssertionError(f"{marker!r} never reached the log: {self.log_bytes()!r}")

    def send(self, data):
        self.proc.stdin.write(data)
        self.proc.stdin.flush()

    def finish(self):
        """Waits up to the deadline for the run to end; returns (rc, log bytes, events, stderr)."""
        try:
            self.proc.wait(DEADLINE)
        except subprocess.TimeoutExpired:
            self.kill()
            raise AssertionError(f"the run did not end: {self.log_bytes()!r}")
        if not self.proc.stdin.closed:
            self.proc.stdin.close()
        err = self.proc.stderr.read()
        self.proc.stderr.close()
        evs = ([json.loads(x) for x in self.events.read_text(encoding="utf-8").splitlines()]
               if self.events.exists() else [])
        return self.proc.returncode, self.log_bytes(), evs, err

    def kill(self):
        if self.proc.poll() is None:
            self.proc.kill()
            self.proc.wait()
        for f in (self.proc.stdin, self.proc.stderr):
            if not f.closed:
                f.close()


@unittest.skipUnless(os.name == "posix", "PTY mode is POSIX only")
class PtyInputTest(unittest.TestCase):
    def setUp(self):
        reset_run_state()
        self.tmp = tempfile.mkdtemp()

    def job(self, code, **kw):
        j = Job(self.tmp, code, "--input", "stdin", **kw)
        self.addCleanup(j.kill)
        return j

    def test_answers_prompt(self):
        j = self.job("a = input('Continue? [y/N] READY'); print('got ' + a)")
        j.wait_for(b"READY")
        j.send(b"y\n")
        rc, log, _, _ = j.finish()
        self.assertEqual(rc, 0)
        self.assertIn(b"got y", log)

    def test_bulk_input_arrives_once_and_in_order(self):
        data = bytes((i * 7 + i // 251) % 256 for i in range(20000))
        j = self.job("import hashlib, os, tty; tty.setraw(0); print('READY', flush=True); buf = b''\n"
                     "while len(buf) < 20000: buf += os.read(0, 65536)\n"
                     "print('sha', hashlib.sha256(buf).hexdigest(), len(buf), flush=True)")
        j.wait_for(b"READY")
        j.send(data)
        rc, log, _, _ = j.finish()
        self.assertEqual(rc, 0)
        self.assertIn(f"sha {hashlib.sha256(data).hexdigest()} 20000".encode(), log)

    def test_eof_keeps_job_running(self):
        j = self.job("import time; print('READY', flush=True); time.sleep(0.3); print('still here')")
        j.wait_for(b"READY")
        j.proc.stdin.close()
        rc, log, _, _ = j.finish()
        self.assertEqual(rc, 0)
        self.assertIn(b"still here", log)

    def test_ctrl_c_interrupts(self):
        j = self.job("import time; print('READY', flush=True); time.sleep(30)")
        j.wait_for(b"READY")
        j.send(b"\x03")
        rc, _, evs, _ = j.finish()
        self.assertEqual(rc, 130)
        self.assertIn({"ev": "note", "level": "warn", "text": "tool killed by SIGINT", "step": "tool"}, evs)
        self.assertEqual(evs[-1], {"ev": "done", "status": "fail"})

    def test_ctrl_c_interrupts_when_runner_started_with_sigint_ignored(self):
        # A runner started in the background by a non-interactive shell inherits SIGINT as ignored; the job must not.
        j = self.job("import time; print('READY', flush=True); time.sleep(30)", ignored=(signal.SIGINT,))
        j.wait_for(b"READY")
        j.send(b"\x03")
        rc, _, evs, _ = j.finish()
        self.assertEqual(rc, 130)
        self.assertIn({"ev": "note", "level": "warn", "text": "tool killed by SIGINT", "step": "tool"}, evs)

    # The child is sh, not Python: a Python.app child dying of SIGQUIT makes the macOS crash reporter show a dialog.
    def test_ctrl_backslash_quits_when_runner_started_with_sigquit_ignored(self):
        j = self.job("echo READY; exec sleep 30", ignored=(signal.SIGQUIT,), sh=True)
        j.wait_for(b"READY")
        j.send(b"\x1c")
        rc, _, evs, _ = j.finish()
        self.assertEqual(rc, 131)
        self.assertIn({"ev": "note", "level": "warn", "text": "tool killed by SIGQUIT", "step": "tool"}, evs)

    def test_ctrl_backslash_quits_when_runner_started_with_sigquit_blocked(self):
        j = self.job("echo READY; exec sleep 30", blocked=(signal.SIGQUIT,), sh=True)
        j.wait_for(b"READY")
        j.send(b"\x1c")
        rc, _, _, _ = j.finish()
        self.assertEqual(rc, 131)

    def test_input_needs_pty(self):
        r = subprocess.run([sys.executable, str(SCRIPT), "--input", "stdin", "generic", "--", "true"],
                           capture_output=True, stdin=subprocess.DEVNULL, env=_env())
        self.assertEqual(r.returncode, 2)
        self.assertIn(b"--input needs --pty", r.stderr)

    def test_no_echo_not_logged(self):
        j = self.job("import os, sys; os.system('stty -echo'); print('READY', flush=True); "
                     "line = sys.stdin.readline(); print('read', len(line))")
        j.wait_for(b"READY")
        j.send(b"secret\n")
        rc, log, _, _ = j.finish()
        self.assertEqual(rc, 0)
        self.assertIn(b"read 7", log)
        self.assertNotIn(b"secret", log)

    def test_ctrl_d_ends_a_read(self):
        j = self.job("import sys; print('READY', flush=True); print('eof', repr(sys.stdin.read()))")
        j.wait_for(b"READY")
        j.send(b"ab\x04")
        j.send(b"\x04")
        rc, log, _, _ = j.finish()
        self.assertEqual(rc, 0)
        self.assertIn(b"eof 'ab'", log)

    def test_ctrl_z_sends_sigtstp(self):
        code = ("import signal, time; signal.signal(signal.SIGTSTP, lambda *a: print('tstp', flush=True)); "
                "print('READY', flush=True); time.sleep(30)")
        j = self.job(code)
        j.wait_for(b"READY")
        j.send(b"\x1a")
        j.wait_for(b"tstp")

    def test_ctrl_z_does_not_stop_a_plain_job(self):
        # The job leads its own session, so its process group is orphaned and a terminal stop signal is discarded:
        # nothing could resume it.  A shell with job control of its own still stops its foreground job.
        j = self.job("import time\nprint('READY', flush=True)\n"
                     "for i in range(60): print('t', i, flush=True); time.sleep(0.05)")
        j.wait_for(b"READY")
        j.send(b"\x1a")
        j.wait_for(b"t 59")
        rc, _, _, _ = j.finish()
        self.assertEqual(rc, 0)

    def test_without_flag_unchanged(self):
        _, log, _ = pty(self.tmp, [sys.executable, str(SCREEN)])
        self.assertEqual(log, GOLDEN)

    def test_with_flag_and_no_input_unchanged(self):
        log = Path(self.tmp) / "in.log"
        subprocess.run([sys.executable, str(SCRIPT), "--pty", "--full-log", str(log), "--events",
                        str(Path(self.tmp) / "in.events.jsonl"), "--input", "stdin", "generic", "--", sys.executable,
                        str(SCREEN)], stdin=subprocess.DEVNULL, capture_output=True, env=_env())
        self.assertEqual(log.read_bytes(), GOLDEN)


if __name__ == "__main__":
    unittest.main()
