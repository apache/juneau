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
import errno
import io
import json
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


class Events:
    """Bus subscriber that keeps (kind, fields) pairs."""

    def __init__(self):
        self.events = []

    def __call__(self, kind, fields):
        self.events.append((kind, fields))

    def of(self, kind):
        return [f for k, f in self.events if k == kind]


class RunToolConsoleTest(unittest.TestCase):
    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        self.bus = jr.subscribe(Events())
        self.sinks = jr.Sinks(console="none")

    def run_child(self, code, **kw):
        # stdout is redirected: with no console, the watchdog echoes prompt text there
        with mock.patch.dict(os.environ, {"RUN_MARKERS": ""}), contextlib.redirect_stdout(io.StringIO()):
            return jr.run_tool(child(code), "generic", "t", "T", sinks=self.sinks, **kw)

    def test_tty_gives_the_child_a_controlling_terminal(self):
        with mock.patch.object(jr, "_input_fd", return_value=None):
            r = self.run_child("import os\nfd = os.open('/dev/tty', os.O_RDWR)\nos.write(fd, b'tty ok\\n')", tty=True,
                               capture=True)
        self.assertEqual(r.exit, 0)
        self.assertIn("tty ok", r.output)

    def test_tty_forwards_keystrokes(self):
        read_end, write_end = os.pipe()
        self.addCleanup(os.close, read_end)
        os.write(write_end, b"yes\n")
        os.close(write_end)
        with mock.patch.object(jr, "_input_fd", return_value=read_end):
            r = self.run_child("import sys\nprint('got', sys.stdin.readline().strip())", tty=True, capture=True)
        self.assertIn("got yes", r.output)

    def test_the_watchdog_echoes_a_partial_line_and_ends_it_at_the_next_newline(self):
        for tty in (False, True):
            with self.subTest(tty=tty):
                self.bus.events.clear()
                with mock.patch.object(jr, "_input_fd", return_value=None):
                    self.run_child("import sys, time\nsys.stdout.write('Passphrase: '); sys.stdout.flush()\n"
                                   "time.sleep(0.6)\nprint('ok')", tty=tty, watchdog=0.2)
                self.assertEqual([f["text"] for f in self.bus.of("prompt")], ["Passphrase: "])
                self.assertEqual("".join(f["text"] for f in self.bus.of("prompt_echo")), "ok")
                self.assertEqual(len(self.bus.of("prompt_end")), 1)
                self.assertTrue(all(f["step"] == "t" for k in ("prompt", "prompt_echo", "prompt_end")
                                    for f in self.bus.of(k)))
                self.assertEqual(self.bus.of("quiet"), [])

    def test_the_watchdog_shows_quiet_when_there_is_no_partial_line(self):
        self.run_child("import time\nprint('start', flush=True)\ntime.sleep(0.6)\nprint('end')", watchdog=0.2)
        quiet = [f["seconds"] for f in self.bus.of("quiet")]
        self.assertEqual(quiet[0], 1)
        self.assertEqual(quiet[-1], 0)
        self.assertTrue(all(f["step"] == "t" for f in self.bus.of("quiet")))
        self.assertEqual(self.bus.of("prompt"), [])

    def test_no_watchdog_means_no_prompt_or_quiet_events(self):
        self.run_child("import sys, time\nsys.stdout.write('x'); sys.stdout.flush()\ntime.sleep(0.3)")
        self.assertEqual(self.bus.of("prompt") + self.bus.of("quiet"), [])

    def test_summarize_sets_the_step_end_summary(self):
        self.run_child("print('a')\nprint('b')", summarize=lambda text: text.split()[-1])
        self.assertEqual(self.bus.of("step_end")[-1]["summary"], "b")

    def test_label_is_published_on_step_start(self):
        self.run_child("pass", label="Commit")
        self.assertEqual(self.bus.of("step_start")[-1]["label"], "Commit")

    def test_capture_returns_the_text(self):
        self.assertEqual(self.run_child("print('hello')", capture=True).output, "hello\n")

    def test_the_console_ticks_while_the_tool_is_silent(self):
        console = mock.Mock()
        console.sinks.console = "none"
        with mock.patch.object(jr, "_console", console):
            self.run_child("import time\ntime.sleep(0.35)")
        self.assertGreaterEqual(console.tick.call_count, 2)

    def test_the_console_ticks_while_the_tool_is_busy(self):
        # A throttled redraw has no trailing edge of its own; tick must run even when select() never times out.
        console = mock.Mock()
        console.sinks.console = "none"
        with mock.patch.object(jr, "_console", console):
            self.run_child("import time\nfor i in range(40):\n    print(i, flush=True)\n    time.sleep(0.01)")
        self.assertGreaterEqual(console.tick.call_count, 20)

    def test_tty_restores_the_saved_terminal_after_stdin_eof(self):
        read_end, write_end = os.pipe()
        self.addCleanup(os.close, read_end)
        os.close(write_end)     # stdin EOF: keys becomes None mid-run
        calls = []
        before = (signal.getsignal(signal.SIGINT), signal.getsignal(signal.SIGTERM))
        fds = iter([read_end, None, None])
        with mock.patch.object(jr, "_input_fd", side_effect=lambda: next(fds)), \
                mock.patch.object(jr.os, "isatty", return_value=True), \
                mock.patch.object(jr.termios, "tcgetattr", return_value=["mode"]), \
                mock.patch.object(jr._tty, "setcbreak"), \
                mock.patch.object(jr.termios, "tcsetattr", side_effect=lambda *a: calls.append(a)):
            r = self.run_child("import time\ntime.sleep(0.3)\nprint('done')", tty=True, capture=True)
        self.assertEqual(r.exit, 0)
        self.assertEqual(calls, [(read_end, jr.termios.TCSADRAIN, ["mode"])])
        self.assertEqual((signal.getsignal(signal.SIGINT), signal.getsignal(signal.SIGTERM)), before)

    def test_tty_setup_failure_still_restores_handlers_and_closes_the_child(self):
        read_end, write_end = os.pipe()
        self.addCleanup(os.close, read_end)
        self.addCleanup(os.close, write_end)
        before = (signal.getsignal(signal.SIGINT), signal.getsignal(signal.SIGTERM))
        children, during = [], []
        real_popen, real_terminate = subprocess.Popen, jr._terminate_group

        def spawn(*args, **kwargs):
            children.append(real_popen(*args, **kwargs))
            return children[-1]

        def terminate(proc, grace=None):
            during.append((signal.getsignal(signal.SIGINT), signal.getsignal(signal.SIGTERM)))
            real_terminate(proc, grace)

        def kill_ours():
            for c in children:      # only our own child, by handle
                if c.poll() is None:
                    c.kill()
                    c.wait()

        self.addCleanup(kill_ours)
        masters = []
        with mock.patch.object(jr, "_input_fd", return_value=read_end), \
                mock.patch.object(jr.os, "isatty", return_value=True), \
                mock.patch.object(jr.termios, "tcgetattr", side_effect=jr.termios.error("boom")), \
                mock.patch.object(jr.termios, "tcsetattr") as restore, \
                mock.patch.object(jr.subprocess, "Popen", side_effect=spawn), \
                mock.patch.object(jr, "_terminate_group", side_effect=terminate), \
                mock.patch.object(jr.os, "openpty", side_effect=self.recording_openpty(masters)), \
                mock.patch.object(jr.os, "close", wraps=os.close) as close, \
                self.assertRaises(jr.termios.error):
            self.run_child("import time\ntime.sleep(5)", tty=True)
        restore.assert_not_called()
        self.assertIn(mock.call(masters[0]), close.call_args_list)     # the PTY master
        self.assertEqual(len(children), 1)
        self.assertIsNotNone(children[0].poll())    # the orphan guard took the child down
        self.assertEqual(during, [(signal.SIG_IGN, signal.SIG_IGN)])   # with no handler of ours to re-enter
        self.assertEqual((signal.getsignal(signal.SIGINT), signal.getsignal(signal.SIGTERM)), before)

    @staticmethod
    def recording_openpty(masters):
        real = os.openpty

        def openpty():
            fds = real()
            masters.append(fds[0])
            return fds

        return openpty

    def test_a_ctrl_c_during_cleanup_still_closes_the_pty_and_ends_the_step(self):
        read_end, write_end = os.pipe()
        self.addCleanup(os.close, read_end)
        self.addCleanup(os.close, write_end)
        before = (signal.getsignal(signal.SIGINT), signal.getsignal(signal.SIGTERM))
        masters = []
        try:
            with mock.patch.object(jr, "_input_fd", return_value=read_end), \
                    mock.patch.object(jr.os, "isatty", return_value=True), \
                    mock.patch.object(jr.termios, "tcgetattr", return_value=["mode"]), \
                    mock.patch.object(jr._tty, "setcbreak"), \
                    mock.patch.object(jr.termios, "tcsetattr",
                                      side_effect=lambda *a: signal.raise_signal(signal.SIGINT)), \
                    mock.patch.object(jr.os, "openpty", side_effect=self.recording_openpty(masters)), \
                    mock.patch.object(jr.os, "close", wraps=os.close) as close:
                r = self.run_child("print('done')", tty=True)
        except KeyboardInterrupt:
            self.fail("a Ctrl-C during the terminal restore cut the cleanup short")
        self.assertEqual(r.exit, 0)
        self.assertIn(mock.call(masters[0]), close.call_args_list)
        self.assertEqual([f["status"] for f in self.bus.of("step_end")], ["ok"])
        self.assertEqual((signal.getsignal(signal.SIGINT), signal.getsignal(signal.SIGTERM)), before)

    def test_tty_collapses_a_crlf_split_across_reads(self):
        code = ("import os, time, tty\ntty.setraw(1)    # no ONLCR: the bytes arrive as written\nfor part in (b'a\\r', b'\\nb\\r\\n'):\n    os.write(1, part)\n    time.sleep(0.2)")
        with mock.patch.object(jr, "_input_fd", return_value=None):
            r = self.run_child(code, tty=True, capture=True)
        self.assertEqual(r.output, "a\nb\n")

    def test_tty_points_GPG_TTY_at_the_pty_so_pinentry_prompts_through_it(self):
        code = "import os\nprint(os.environ.get('GPG_TTY'), os.ttyname(0))"
        for env in (None, {**os.environ, "GPG_TTY": "/dev/ttys999"}):
            with self.subTest(env=env is not None), mock.patch.dict(os.environ, {"GPG_TTY": "/dev/ttys999"}), \
                    mock.patch.object(jr, "_input_fd", return_value=None):
                r = self.run_child(code, tty=True, capture=True, env=env)
            gpg_tty, slave = r.output.split()
            self.assertEqual(gpg_tty, slave)

    def test_without_tty_GPG_TTY_is_left_alone(self):
        with mock.patch.dict(os.environ, {"GPG_TTY": "/dev/ttys999"}):
            r = self.run_child("import os\nprint(os.environ.get('GPG_TTY'))", capture=True)
        self.assertEqual(r.output.strip(), "/dev/ttys999")

    def test_keys_typed_after_the_child_has_gone_are_dropped(self):
        read_end, write_end = os.pipe()
        self.addCleanup(os.close, read_end)
        os.write(write_end, b"late\n")
        os.close(write_end)
        masters = []
        real_write = os.write

        def write(fd, data):
            if masters and fd == masters[0]:
                raise OSError(errno.EIO, "Input/output error")
            return real_write(fd, data)

        with mock.patch.object(jr, "_input_fd", return_value=read_end), \
                mock.patch.object(jr.os, "openpty", side_effect=self.recording_openpty(masters)), \
                mock.patch.object(jr.os, "write", side_effect=write):
            r = self.run_child("print('done')", tty=True)
        self.assertEqual(r.exit, 0)
        self.assertEqual([f["status"] for f in self.bus.of("step_end")], ["ok"])

    def test_an_error_in_the_read_loop_ends_the_step_as_failed_and_propagates(self):
        for tty in (False, True):
            with self.subTest(tty=tty):
                self.bus.events.clear()
                with mock.patch.object(jr, "_input_fd", return_value=None), \
                        mock.patch.object(jr._Watch, "output", side_effect=RuntimeError("boom")), \
                        self.assertRaises(RuntimeError):
                    self.run_child("print('done')", tty=tty)
                self.assertEqual([f["status"] for f in self.bus.of("step_end")], ["fail"])

    def test_pass_through_spawn_failure_ends_the_step_with_127_and_raises(self):
        with mock.patch.dict(os.environ, {"RUN_MARKERS": ""}), self.assertRaises(FileNotFoundError):
            jr.run_tool(["/nonexistent/juneau-run-tool"], "generic", "t", "T")
        self.assertEqual([(f["status"], f["exit"]) for f in self.bus.of("step_end")], [("fail", 127)])

    def test_piped_spawn_failure_ends_the_step_with_127_and_raises(self):
        for tty in (False, True):
            with self.subTest(tty=tty):
                self.bus.events.clear()
                before = (signal.getsignal(signal.SIGINT), signal.getsignal(signal.SIGTERM))
                out = io.StringIO()
                with mock.patch.dict(os.environ, {"RUN_MARKERS": "1"}), contextlib.redirect_stdout(out), \
                        self.assertRaises(FileNotFoundError):
                    jr.run_tool(["/nonexistent/juneau-run-tool"], "generic", "t", "T", sinks=self.sinks, tty=tty)
                self.assertEqual(len(self.bus.of("step_start")), 1)
                self.assertEqual([(f["status"], f["exit"]) for f in self.bus.of("step_end")], [("fail", 127)])
                self.assertEqual([(m["ev"], m.get("exit")) for m in markers(out.getvalue())],
                                 [("step", None), ("end", 127)])
                self.assertEqual((signal.getsignal(signal.SIGINT), signal.getsignal(signal.SIGTERM)), before)


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


class TTYOut(io.StringIO):
    def isatty(self):
        return True


class ScriptApiTest(unittest.TestCase):
    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        self.bus = jr.subscribe(Events())
        self.console = mock.Mock(mode="live")
        self.console.board.width.return_value = 9
        self.console.board.detail = "modules"
        patcher = mock.patch.object(jr, "_console", self.console)
        patcher.start()
        self.addCleanup(patcher.stop)

    def calls(self):
        return [c[0] for c in self.console.method_calls if c[0] in ("pause", "resume")]

    def test_ask_pauses_reads_a_line_and_resumes(self):
        with mock.patch("builtins.input", return_value="y") as read:
            self.assertEqual(jr.ask("Continue? "), "y")
        read.assert_called_once_with("Continue? ")
        self.assertEqual(self.calls(), ["pause", "resume"])
        self.assertEqual(self.bus.of("ask"), [{"prompt": "Continue? ", "answer": "y"}])

    def test_ask_resumes_even_when_reading_fails(self):
        with mock.patch("builtins.input", side_effect=EOFError), self.assertRaises(EOFError):
            jr.ask("Continue? ")
        self.assertEqual(self.calls(), ["pause", "resume"])

    def test_show_prints_the_text_in_full(self):
        out = io.StringIO()
        with mock.patch.object(sys, "stdout", out):
            jr.show("line 1\nline 2")
        self.assertEqual(out.getvalue(), "line 1\nline 2\n")
        self.assertEqual(self.calls(), ["pause", "resume"])
        self.assertEqual(self.bus.of("show"), [{"text": "line 1\nline 2"}])

    def pager(self, code):
        """A $PAGER command that runs code, and the file named by PAGED in that code."""
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        script, paged = Path(tmp.name) / "pager.py", Path(tmp.name) / "paged.txt"
        script.write_text(f"import os, signal, sys, time\nPAGED = {str(paged)!r}\n{code}\n", encoding="utf-8")
        return f"{sys.executable} -B {script}", paged

    def test_show_with_pager_uses_PAGER_on_a_terminal(self):
        pager, paged = self.pager("open(PAGED, 'w').write(sys.stdin.read())")
        out = TTYOut()
        with mock.patch.object(sys, "stdout", out), mock.patch.dict(os.environ, {"PAGER": pager}):
            jr.show("diff text", pager=True)
        self.assertEqual(paged.read_text(encoding="utf-8"), "diff text")
        self.assertEqual(out.getvalue(), "")

    def test_ctrl_c_in_the_pager_is_the_pager_s_and_does_not_cancel(self):
        def handler(signum, frame):
            raise KeyboardInterrupt

        previous = signal.signal(signal.SIGINT, handler)
        self.addCleanup(signal.signal, signal.SIGINT, previous)
        pager, _ = self.pager("sys.stdin.read()\nos.kill(os.getppid(), signal.SIGINT)\ntime.sleep(0.3)")
        out = TTYOut()
        try:
            with mock.patch.object(sys, "stdout", out), mock.patch.dict(os.environ, {"PAGER": pager}):
                jr.show("diff text", pager=True)
        except KeyboardInterrupt:
            self.fail("a Ctrl-C in the pager cancelled the caller")
        self.assertEqual(out.getvalue(), "")
        self.assertIs(signal.getsignal(signal.SIGINT), handler)
        self.assertEqual(self.calls(), ["pause", "resume"])

    def test_quitting_the_pager_before_it_reads_everything_is_not_an_error(self):
        pager, _ = self.pager("sys.exit(0)")
        out = TTYOut()
        with mock.patch.object(sys, "stdout", out), mock.patch.dict(os.environ, {"PAGER": pager}):
            jr.show("x" * (1 << 20), pager=True)
        self.assertEqual(out.getvalue(), "")

    def test_show_with_a_missing_or_unparseable_PAGER_prints_the_text(self):
        for pager in ("/nonexistent/juneau-run-pager", 'less "unterminated'):
            with self.subTest(pager=pager):
                out = TTYOut()
                with mock.patch.object(sys, "stdout", out), mock.patch.dict(os.environ, {"PAGER": pager}):
                    jr.show("diff text", pager=True)
                self.assertEqual(out.getvalue(), "diff text\n")

    def test_show_with_a_blank_PAGER_uses_less(self):
        with mock.patch.object(sys, "stdout", TTYOut()), mock.patch.dict(os.environ, {"PAGER": "   "}), \
                mock.patch.object(jr.subprocess, "Popen") as popen:
            popen.return_value.wait.return_value = 0
            jr.show("diff text", pager=True)
        popen.assert_called_once_with(["less", "-R"], stdin=subprocess.PIPE, text=True)
        popen.return_value.stdin.write.assert_called_once_with("diff text")

    def test_nested_pauses_resume_only_at_the_outermost_exit(self):
        with jr.handoff(), mock.patch.object(sys, "stdout", io.StringIO()):
            with mock.patch("builtins.input", return_value="y"):
                jr.ask("Continue? ")
            jr.show("text")
            self.assertEqual(self.calls(), ["pause"])
        self.assertEqual(self.calls(), ["pause", "resume"])

    def test_show_with_pager_prints_when_stdout_is_not_a_terminal(self):
        out = io.StringIO()
        with mock.patch.object(sys, "stdout", out), mock.patch.object(jr.subprocess, "run") as run:
            jr.show("diff text", pager=True)
        run.assert_not_called()
        self.assertEqual(out.getvalue(), "diff text\n")

    def test_passthrough_spawn_failure_ends_the_row_with_127_and_raises(self):
        with self.assertRaises(FileNotFoundError):
            jr.passthrough(["/nonexistent/juneau-run-tool"], "pgp", "PGP")
        end = self.bus.of("step_end")[-1]
        self.assertEqual((end["status"], end["exit"]), ("fail", 127))

    def test_passthrough_runs_on_the_real_terminal_and_reports_the_exit(self):
        r = jr.passthrough(child("import sys; sys.exit(3)"), "pgp", "PGP passphrase", label="PGP")
        self.assertEqual(r.exit, 3)
        self.assertEqual(self.calls(), ["pause", "resume"])
        self.assertEqual(self.bus.of("step_start")[-1]["label"], "PGP")
        end = self.bus.of("step_end")[-1]
        self.assertEqual((end["id"], end["status"], end["exit"]), ("pgp", "fail", 3))

    def test_handoff_pauses_and_exports_the_label_width_and_detail(self):
        with jr.handoff() as env:
            self.assertEqual(env, {"JUNEAU_RUN_LABEL_WIDTH": "9", "JUNEAU_RUN_DETAIL": "modules"})
            self.assertEqual(self.calls(), ["pause"])
        self.assertEqual(self.calls(), ["pause", "resume"])

    def test_console_active(self):
        self.assertTrue(jr.console_active())
        self.console.mode = None
        self.assertFalse(jr.console_active())


class ScriptApiWithoutConsoleTest(unittest.TestCase):
    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)

    def test_everything_works_with_no_session(self):
        self.assertFalse(jr.console_active())
        with jr.handoff() as env:
            self.assertEqual(env, {})
        with mock.patch("builtins.input", return_value="n"):
            self.assertEqual(jr.ask("? "), "n")
        self.assertEqual(jr.passthrough(child("pass"), "x", "X").exit, 0)

    def test_ask_is_written_to_the_condensed_log(self):
        with tempfile.TemporaryDirectory() as tmp:
            log = Path(tmp) / "c.log"
            jr._console = jr.subscribe(jr.Console(jr.Sinks(console="none", condensed_log=str(log))))
            with mock.patch("builtins.input", return_value="y"):
                jr.ask("Continue? ")
            jr._end_session()
            self.assertIn("Continue? y", log.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
