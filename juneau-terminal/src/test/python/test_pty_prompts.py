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
"""PTY prompts: --prompts stdio relays the job's prompt channel over the runner's stdin and stdout, and none of it is
logged."""

import json
import os
import queue
import subprocess
import sys
import tempfile
import threading
import time
import unittest
from pathlib import Path

from support import SCRIPT, reset_run_state

DEADLINE = 10.0

CHILD = r'''
import json, os, sys
out = os.fdopen(int(os.environ["JUNEAU_PROMPT_FD_OUT"]), "w", buffering=1)
inp = os.fdopen(int(os.environ["JUNEAU_PROMPT_FD_IN"]), "r")
out.write(json.dumps({"v": 1, "type": "prompt", "id": "p1", "kind": "secret", "message": "Token"}) + "\n")
out.write("this is not json\n")
ans = json.loads(inp.readline())
print("GOT", len(ans["value"]), flush=True)
'''

SLEEPER = r'''
import os, time
os.fdopen(int(os.environ["JUNEAU_PROMPT_FD_OUT"]), "w", buffering=1).write('{"v":1,"type":"prompt","id":"p1"}\n')
print("READY", flush=True)
time.sleep(60)
'''


def _env():
    env = {k: v for k, v in os.environ.items()
           if not k.startswith(("RUN_", "JUNEAU_TERMINAL_")) or k == "JUNEAU_TERMINAL_LOG_DIR"}
    env["JUNEAU_TERMINAL_HISTORY"] = "0"
    return env


class Job:
    """One `--pty --prompts stdio` run of a Python snippet; the test reads the runner's stdout and writes its stdin."""

    def __init__(self, tmp, code, *extra):
        self.log, self.events = Path(tmp) / "out.log", Path(tmp) / "out.events.jsonl"
        self.proc = subprocess.Popen(
            [sys.executable, str(SCRIPT), "--console", "none", "--pty", "--size", "80x24", "--full-log", str(self.log),
             "--events", str(self.events), "--prompts", "stdio", *extra, "--step", "tool", "generic", "--",
             sys.executable, "-c", code],
            stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, env=_env())
        self.lines = queue.Queue()
        self.stdout = []
        threading.Thread(target=self._pump, daemon=True).start()

    def _pump(self):
        for line in self.proc.stdout:
            self.stdout.append(line)
            self.lines.put(line)

    def next_line(self):
        return json.loads(self.lines.get(timeout=DEADLINE))

    def log_bytes(self):
        return self.log.read_bytes() if self.log.exists() else b""

    def wait_for(self, marker):
        end = time.monotonic() + DEADLINE
        while time.monotonic() < end:
            if marker in self.log_bytes():
                return
            time.sleep(0.02)
        raise AssertionError(f"{marker!r} never reached the log: {self.log_bytes()!r}")

    def send(self, obj):
        self.proc.stdin.write((json.dumps(obj) + "\n").encode())
        self.proc.stdin.flush()

    def finish(self):
        """(rc, log bytes, events, stderr bytes) once the run has ended."""
        try:
            self.proc.wait(DEADLINE)
        except subprocess.TimeoutExpired:
            self.kill()
            raise AssertionError(f"the run did not end: {self.log_bytes()!r}")
        err = self.proc.stderr.read()
        evs = ([json.loads(x) for x in self.events.read_text(encoding="utf-8").splitlines()]
               if self.events.exists() else [])
        return self.proc.returncode, self.log_bytes(), evs, err

    def everything(self):
        """Every byte the runner recorded or printed besides stdout: the log, the events file and stderr."""
        return self.log_bytes() + (self.events.read_bytes() if self.events.exists() else b"")

    def kill(self):
        if self.proc.poll() is None:
            self.proc.kill()
            self.proc.wait()
        for f in (self.proc.stdin, self.proc.stderr, self.proc.stdout):
            if not f.closed:
                f.close()


@unittest.skipUnless(os.name == "posix", "PTY mode is POSIX only")
class PtyPromptsTest(unittest.TestCase):
    def setUp(self):
        reset_run_state()
        self.tmp = tempfile.mkdtemp()

    def job(self, code, *extra):
        j = Job(self.tmp, code, *extra)
        self.addCleanup(j.kill)
        return j

    def test_prompt_lines_relayed_to_stdout_only(self):
        j = self.job(CHILD)
        first = j.next_line()
        self.assertEqual((first["type"], first["id"], first["kind"]), ("prompt", "p1", "secret"))
        j.send({"v": 1, "type": "answer", "id": "p1", "value": "x"})
        rc, log, evs, err = j.finish()
        self.assertEqual(rc, 0)
        for needle in (b"Token", b'"kind"', b'"type"'):
            self.assertNotIn(needle, j.everything())
            self.assertNotIn(needle, err)

    def test_answers_relayed_to_child(self):
        j = self.job(CHILD)
        j.next_line()
        j.send({"v": 1, "type": "answer", "id": "p1", "value": "hunter2"})
        j.wait_for(b"GOT 7")
        rc, log, evs, err = j.finish()
        self.assertEqual(rc, 0)
        self.assertIn(b"GOT 7", log)

    def test_answers_absent_from_terminal_log_and_events(self):
        j = self.job(CHILD)
        j.next_line()
        j.send({"v": 1, "type": "answer", "id": "p1", "value": "hunter2"})
        rc, log, evs, err = j.finish()
        self.assertEqual(rc, 0)
        self.assertNotIn(b"hunter2", j.everything())
        self.assertNotIn(b"hunter2", err)
        self.assertNotIn(b"hunter2", b"".join(j.stdout))

    def test_non_json_line_dropped_with_warn(self):
        j = self.job(CHILD)
        j.next_line()
        j.send({"v": 1, "type": "answer", "id": "p1", "value": "x"})
        rc, log, evs, err = j.finish()
        self.assertEqual(rc, 0)
        self.assertNotIn(b"this is not json", b"".join(j.stdout))
        self.assertIn(b"juneau-terminal:", err)
        self.assertNotIn(b"this is not json", err)

    def test_non_json_answer_dropped_with_warn(self):
        j = self.job(CHILD)
        j.next_line()
        j.proc.stdin.write(b"secret-garbage\n")
        j.proc.stdin.flush()
        j.send({"v": 1, "type": "answer", "id": "p1", "value": "x"})
        rc, log, evs, err = j.finish()
        self.assertEqual(rc, 0)
        self.assertNotIn(b"secret-garbage", err + j.everything())

    def test_stdin_eof_terminates_job_cancelled(self):
        j = self.job(SLEEPER)
        j.next_line()
        j.wait_for(b"READY")
        j.proc.stdin.close()
        t0 = time.monotonic()
        rc, log, evs, err = j.finish()
        self.assertLess(time.monotonic() - t0, 15)
        self.assertEqual(rc, 130)
        self.assertEqual(evs[-1], {"ev": "done", "status": "cancelled"})

    def test_prompts_with_input_is_usage_error(self):
        log = str(Path(self.tmp) / "x.log")
        r = subprocess.run([sys.executable, str(SCRIPT), "--pty", "--prompts", "stdio", "--input", "stdin",
                            "--full-log", log, "--events", log + ".e", "generic", "--", "true"],
                           capture_output=True, text=True)
        self.assertEqual(r.returncode, 2)
        self.assertIn("usage:", r.stderr)


if __name__ == "__main__":
    unittest.main()
