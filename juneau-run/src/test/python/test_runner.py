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
"""Runner: mode selection, exit mapping, \\r handling, cancel, parser isolation, child closing, tests throttling."""

import contextlib
import io
import json
import os
import signal
import sys
import tempfile
import threading
import time
import unittest
from pathlib import Path
from unittest import mock

from support import jr, reset_run_state


def child(code):
    return [sys.executable, "-c", code]


def markers(out):
    result = []
    for line in out.splitlines():
        if line.startswith(jr.PREFIX):
            obj = json.loads(line[len(jr.PREFIX):])
            obj.pop("ms", None)
            result.append(obj)
    return result


class Recording(jr.Parser):
    """Records the lines it is given; optionally opens one sub-step on the first line and never closes it."""

    name = "recording"

    def __init__(self, open_child=False):
        super().__init__()
        self.lines, self.open_child, self.finished = [], open_child, None

    def handle(self, line):
        self.lines.append(line)
        if self.open_child and len(self.lines) == 1:
            return [("sub_start", f"{self.step_id}/kid", 2, "Kid")]
        return []

    def finish(self, exit_code):
        self.finished = exit_code
        return []


class Exploding(jr.Parser):
    name = "exploding"

    def handle(self, line):
        raise RuntimeError("bad parser")


class Counting(jr.Parser):
    name = "counting"

    def __init__(self):
        super().__init__()
        self.n = 0

    def handle(self, line):
        self.n += 1
        return [("tests", self.step_id, self.n, 0, 0, 0)]


class RunnerTest(unittest.TestCase):
    def setUp(self):
        reset_run_state()
        self.tmp = Path(tempfile.mkdtemp())
        patcher = mock.patch.dict(os.environ, {"RUN_MARKERS": "1"})
        patcher.start()
        self.addCleanup(patcher.stop)
        self.addCleanup(reset_run_state)
        for key in ("JUNEAU_RUN_CONSOLE", "JUNEAU_RUN_FULL_LOG", "JUNEAU_RUN_CONDENSED_LOG", "RUN_ARTIFACTS"):
            os.environ.pop(key, None)

    def go(self, cmd, parser, **kwargs):
        out, err = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            result = jr.run_tool(cmd, parser, "t", "Tool", **kwargs)
        return result, out.getvalue(), err.getvalue()

    # ---- mode selection -----------------------------------------------------------------------------------------

    def test_pass_through_when_markers_off_and_sinks_default(self):
        os.environ.pop("RUN_MARKERS")
        parser = mock.Mock()
        with mock.patch.object(jr.subprocess, "run", return_value=mock.Mock(returncode=7)) as run, \
                mock.patch.object(jr.subprocess, "Popen", side_effect=AssertionError("must not pipe")):
            result = jr.run_tool(["mvn", "test"], parser, "t", "T", cwd="/x")
        self.assertEqual(result.exit, 7)
        run.assert_called_once_with(["mvn", "test"], cwd="/x", env=None)   # no stdout/stderr: the child inherits the terminal
        parser.assert_not_called()

    def test_markers_on_selects_piped_mode(self):
        result, out, _ = self.go(child("print('hi')"), Recording())
        self.assertEqual(result.exit, 0)
        self.assertIn("hi", out)
        self.assertEqual([m["ev"] for m in markers(out)], ["step", "end"])

    def test_log_route_selects_piped_mode_without_markers(self):
        os.environ.pop("RUN_MARKERS")
        parser = Recording()
        sinks = jr.Sinks(full_log=self.tmp / "f.log")
        self.go(child("print('hi')"), parser, sinks=sinks)
        self.assertEqual(parser.lines, ["hi"])

    def test_capture_selects_piped_mode_and_returns_output(self):
        os.environ.pop("RUN_MARKERS")
        result, _, _ = self.go(child("print('a'); import sys; print('b', file=sys.stderr)"), "generic", capture=True)
        self.assertEqual(sorted(result.output.split()), ["a", "b"])

    # ---- exit mapping and steps ---------------------------------------------------------------------------------

    def test_exit_code_maps_to_end_status(self):
        result, out, _ = self.go(child("import sys; sys.exit(3)"), Recording())
        self.assertEqual(result.exit, 3)
        self.assertEqual(markers(out)[-1], {"ev": "end", "id": "t", "status": "fail", "exit": 3})
        result, out, _ = self.go(child("pass"), Recording())
        self.assertEqual(markers(out)[-1], {"ev": "end", "id": "t", "status": "ok"})

    def test_step_carries_n_and_parent(self):
        _, out, _ = self.go(child("pass"), Recording(), n=4, parent="outer")
        self.assertEqual(markers(out)[0], {"ev": "step", "id": "t", "n": 4, "title": "Tool", "parent": "outer"})

    def test_run_tool_never_emits_done(self):
        _, out, _ = self.go(child("pass"), Recording())
        self.assertNotIn("done", [m["ev"] for m in markers(out)])

    # ---- reading ------------------------------------------------------------------------------------------------

    def test_carriage_returns_reach_the_full_sink_and_parser_sees_the_last_segment(self):
        full = self.tmp / "f.log"
        parser = Recording()
        self.go(child("import sys; sys.stdout.buffer.write(b'a\\rb\\r\\nc\\n'); sys.stdout.flush()"), parser,
                sinks=jr.Sinks("none", full))
        self.assertEqual(full.read_bytes(), b"a\rb\r\nc\n")
        self.assertEqual(parser.lines, ["b", "c"])

    def test_parser_gets_final_unterminated_line_and_the_exit_code(self):
        parser = Recording()
        self.go(child("import sys; sys.stdout.write('tail')"), parser)
        self.assertEqual((parser.lines, parser.finished), (["tail"], 0))

    def test_marker_after_a_partial_chunk_starts_on_a_fresh_line(self):
        code = ("import sys, time; sys.stdout.buffer.write(b'first\\npart'); sys.stdout.flush(); time.sleep(0.3); "
                "sys.stdout.buffer.write(b'ial\\n'); sys.stdout.flush()")
        parser = Recording(open_child=True)
        _, out, _ = self.go(child(code), parser)
        self.assertIn("part\n" + jr.PREFIX, out)
        self.assertNotIn("part" + jr.PREFIX, out)
        for line in out.splitlines():
            if jr.PREFIX in line:
                self.assertTrue(line.startswith(jr.PREFIX), line)
        self.assertEqual(parser.lines, ["first", "partial"])

    # ---- isolation and closing ----------------------------------------------------------------------------------

    def test_failing_parser_is_disabled_with_one_warning_and_output_keeps_flowing(self):
        result, out, err = self.go(child("print('x'); print('y'); import sys; sys.exit(2)"), Exploding())
        self.assertIn("x\ny\n", out)
        self.assertEqual(len(err.strip().splitlines()), 1)
        self.assertEqual(result.exit, 2)

    def test_prepare_failure_falls_back_to_the_original_command(self):
        class BadPrepare(Recording):
            def prepare(self, cmd, env, cwd):
                raise RuntimeError("nope")
        result, out, err = self.go(child("print('ok')"), BadPrepare())
        self.assertEqual(result.exit, 0)
        self.assertIn("ok", out)
        self.assertEqual(len(err.strip().splitlines()), 1)

    def test_open_children_are_closed_before_the_parent_with_the_parent_status(self):
        _, out, _ = self.go(child("print('x'); import sys; sys.exit(1)"), Recording(open_child=True))
        events = [(m["ev"], m["id"], m.get("status")) for m in markers(out) if m["ev"] in ("step", "end")]
        self.assertEqual(events, [("step", "t", None), ("step", "t/kid", None),
                                  ("end", "t/kid", "fail"), ("end", "t", "fail")])

    def test_tests_events_are_throttled_and_the_final_totals_are_sent(self):
        result, out, _ = self.go(child("[print(i) for i in range(20)]"), Counting())
        sent = [m for m in markers(out) if m["ev"] == "tests"]
        self.assertLess(len(sent), 20)
        self.assertEqual(sent[-1]["total"], 20)
        self.assertEqual(result.summary["total"], 20)
        self.assertLess([m["ev"] for m in markers(out)].index("tests"), [m["ev"] for m in markers(out)].index("end"))

    # ---- cancel -------------------------------------------------------------------------------------------------

    def cancel_run(self, ignore_sigterm):
        pidfile = self.tmp / "pids"
        code = (
            "import os, signal, subprocess, sys, time\n"
            f"ignore = {ignore_sigterm!r}\n"
            "if ignore: signal.signal(signal.SIGTERM, signal.SIG_IGN)\n"
            "sub = subprocess.Popen([sys.executable, '-c', 'import signal, time\\n"
            "signal.signal(signal.SIGTERM, signal.SIG_IGN) if %r else None\\ntime.sleep(60)' % ignore])\n"
            f"open({str(pidfile)!r}, 'w').write('%d %d' % (os.getpid(), sub.pid))\n"
            "print('started', flush=True)\n"
            "time.sleep(60)\n")

        def killer():
            for _ in range(200):
                if pidfile.exists() and pidfile.read_text().count(" "):
                    break
                time.sleep(0.05)
            time.sleep(0.2)
            os.kill(os.getpid(), signal.SIGTERM)

        threading.Thread(target=killer, daemon=True).start()
        out, err = io.StringIO(), io.StringIO()
        started = time.monotonic()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            with self.assertRaises(KeyboardInterrupt):
                jr.run_tool(child(code), Recording(open_child=True), "t", "Tool")
        pids = [int(p) for p in pidfile.read_text().split()]
        return out.getvalue(), pids, time.monotonic() - started

    def assert_gone(self, pid):
        for _ in range(40):
            try:
                os.kill(pid, 0)
            except ProcessLookupError:
                return
            time.sleep(0.05)
        self.fail(f"pid {pid} is still alive")

    def test_cancel_terminates_the_group_closes_children_and_exits_130(self):
        out, pids, _ = self.cancel_run(ignore_sigterm=False)
        for pid in pids:
            self.assert_gone(pid)
        tail = [(m["ev"], m["id"], m.get("status"), m.get("exit")) for m in markers(out) if m["ev"] == "end"]
        self.assertEqual(tail, [("end", "t/kid", "fail", None), ("end", "t", "fail", 130)])
        self.assertNotIn("done", out)

    def test_cancel_escalates_to_sigkill_after_the_grace_period(self):
        with mock.patch.object(jr, "GRACE_SECONDS", 0.5):
            out, pids, elapsed = self.cancel_run(ignore_sigterm=True)
        for pid in pids:
            self.assert_gone(pid)
        self.assertGreaterEqual(elapsed, 0.5)
        self.assertEqual(markers(out)[-1]["exit"], 130)

    def test_default_grace_is_five_seconds(self):
        self.assertEqual(jr.GRACE_SECONDS, 5.0)

    def test_signal_handlers_are_restored(self):
        before = (signal.getsignal(signal.SIGINT), signal.getsignal(signal.SIGTERM))
        self.go(child("pass"), Recording())
        self.assertEqual((signal.getsignal(signal.SIGINT), signal.getsignal(signal.SIGTERM)), before)


if __name__ == "__main__":
    unittest.main()
