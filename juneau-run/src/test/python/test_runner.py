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
import shutil
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


class OldStyle:
    """A parser written before poll, close and render existed: only the original methods."""

    name = "old"

    def __init__(self):
        self.step_id = None

    def prepare(self, cmd, env, cwd):
        return cmd, env

    def on_line(self, line):
        return []

    def finish(self, exit_code):
        return [("note", "info", "finished", self.step_id)]


class IsolatedTest(unittest.TestCase):
    def test_missing_optional_method_is_a_no_op_and_does_not_disable(self):
        g = jr._Isolated(OldStyle())
        with contextlib.redirect_stderr(io.StringIO()) as err:
            self.assertEqual(g.call("poll"), [])
            self.assertEqual(g.render("x"), "x")
            g.close()
        self.assertFalse(g.disabled)
        self.assertEqual(g.call("finish", 0), [("note", "info", "finished", None)])
        self.assertEqual(err.getvalue(), "")

    def test_poll_error_disables(self):
        class Bad(jr.Parser):
            name = "bad"

            def poll(self):
                raise ValueError("no")

        g = jr._Isolated(Bad())
        with contextlib.redirect_stderr(io.StringIO()) as err:
            self.assertEqual(g.call("poll"), [])
            self.assertEqual(g.call("on_line", "x"), [])
        self.assertTrue(g.disabled)
        self.assertEqual(err.getvalue(), "juneau-run: parser bad disabled after error: ValueError('no')\n")

    def test_close_runs_on_disabled_parser(self):
        class Bad(jr.Parser):
            name = "bad"
            closed = 0

            def handle(self, line):
                raise ValueError("no")

            def close(self):
                self.closed += 1

        p = Bad()
        g = jr._Isolated(p)
        with contextlib.redirect_stderr(io.StringIO()):
            g.call("on_line", "x")
        g.close()
        g.close()
        self.assertTrue(g.disabled)
        self.assertEqual(p.closed, 1)

    def test_close_error_is_warned_not_raised(self):
        class Bad(jr.Parser):
            name = "bad"

            def close(self):
                raise OSError("busy")

        with contextlib.redirect_stderr(io.StringIO()) as err:
            jr._Isolated(Bad()).close()
        self.assertEqual(err.getvalue(), "juneau-run: parser bad close failed: OSError('busy')\n")

    def test_render_falls_back_to_the_raw_line_on_error(self):
        class Bad(jr.Parser):
            name = "bad"

            def render(self, line):
                raise ValueError("no")

        class NotText(jr.Parser):
            name = "nottext"

            def render(self, line):
                return 42

        g = jr._Isolated(Bad())
        with contextlib.redirect_stderr(io.StringIO()):
            self.assertEqual(g.render("raw"), "raw")
        self.assertTrue(g.disabled)
        self.assertEqual(g.render("next"), "next")
        g2 = jr._Isolated(NotText())
        self.assertEqual(g2.render("raw"), "raw")
        self.assertFalse(g2.disabled)


class Contract(jr.Parser):
    """Records the contract calls: interactive at prepare time, how often poll ran, finish and close."""

    name = "contract"

    def __init__(self, poll_events=None):
        super().__init__()
        self.poll_events = poll_events or (lambda n: [])
        self.polls = 0
        self.calls = []
        self.seen_interactive = None

    def prepare(self, cmd, env, cwd):
        self.seen_interactive = self.interactive
        return cmd, env

    def poll(self):
        self.polls += 1
        return self.poll_events(self.polls)

    def finish(self, exit_code):
        self.calls.append("finish")
        return []

    def close(self):
        self.calls.append("close")


class Rewriting(jr.Parser):
    """Logs a JSON line's "text" instead of the line; sees the raw line."""

    name = "rewriting"
    rewrites_log = True

    def __init__(self, boom=False):
        super().__init__()
        self.lines = []
        self.boom = boom

    def handle(self, line):
        self.lines.append(line)
        return []

    def render(self, line):
        if self.boom:
            raise ValueError("render broke")
        try:
            return json.loads(line)["text"]
        except (ValueError, TypeError, KeyError):
            return line


class RunToolContractTest(unittest.TestCase):
    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        self.bus = jr.subscribe(Events())
        self.tmp = tempfile.mkdtemp()
        self.addCleanup(shutil.rmtree, self.tmp, True)
        self.log = Path(self.tmp) / "o.log"
        self.sinks = jr.Sinks("none", str(self.log))

    def run_child(self, code, parser, **kw):
        with mock.patch.dict(os.environ, {"RUN_MARKERS": ""}), contextlib.redirect_stdout(io.StringIO()):
            return jr.run_tool(child(code), parser, "t", "T", sinks=self.sinks, **kw)

    def test_interactive_is_false_in_piped_mode_and_true_under_tty(self):
        for tty in (False, True):
            with self.subTest(tty=tty):
                p = Contract()
                with mock.patch.object(jr, "_input_fd", return_value=None):
                    self.run_child("pass", p, tty=tty)
                self.assertIs(p.seen_interactive, tty)

    def test_poll_drives_events_when_the_tool_prints_nothing(self):
        p = Contract(lambda n: [("progress", "t", f"poll {n}")] if n == 3 else [])
        self.run_child("import time; time.sleep(0.5)", p)
        self.assertGreaterEqual(p.polls, 3)
        self.assertEqual([f["text"] for f in self.bus.of("step_progress")], ["poll 3"])

    def test_poll_drives_events_when_the_tool_prints_only_carriage_returns(self):
        p = Contract(lambda n: [("progress", "t", f"poll {n}")] if n == 3 else [])
        self.run_child("import sys, time\nfor i in range(10):\n    sys.stdout.write(f'\\r{i}'); sys.stdout.flush(); "
                       "time.sleep(0.05)", p)
        self.assertEqual([f["text"] for f in self.bus.of("step_progress")], ["poll 3"])

    def test_close_runs_once_after_finish_on_a_normal_exit(self):
        p = Contract()
        self.run_child("print('x')", p)
        self.assertEqual(p.calls, ["finish", "close"])

    def test_close_runs_on_cancel(self):
        def poll_events(n):
            if n == 2:
                raise KeyboardInterrupt
            return []

        p = Contract(poll_events)
        with self.assertRaises(KeyboardInterrupt):
            self.run_child("import time; time.sleep(30)", p)
        self.assertEqual(p.calls, ["close"])

    def test_close_runs_on_error(self):
        class Boom(jr.Sinks):
            def write_full(self, chunk):
                raise RuntimeError("sink failed")

        self.sinks = Boom("none", str(self.log))
        p = Contract()
        with self.assertRaises(RuntimeError):
            self.run_child("print('x')", p)
        self.assertEqual(p.calls, ["close"])

    def test_close_runs_when_the_tool_cannot_start(self):
        p = Contract()
        with self.assertRaises(OSError), mock.patch.dict(os.environ, {"RUN_MARKERS": ""}), \
                contextlib.redirect_stdout(io.StringIO()):
            jr.run_tool(["/nonexistent/x"], p, "t", "T", sinks=self.sinks)
        self.assertEqual(p.calls, ["close"])

    def test_close_runs_when_the_sinks_cannot_open(self):
        class NoOpen(jr.Sinks):
            def open(self):
                raise OSError("disk gone")

        class Tmp(Contract):
            def prepare(self, cmd, env, cwd):
                self.made = Path(tempfile.mkdtemp())
                return super().prepare(cmd, env, cwd)

            def close(self):
                shutil.rmtree(self.made, ignore_errors=True)
                super().close()

        self.sinks = NoOpen("none", str(self.log))
        p = Tmp()
        with self.assertRaises(OSError):
            self.run_child("print('x')", p)
        self.assertEqual(p.calls, ["close"])
        self.assertFalse(p.made.exists())

    def test_a_chatty_tool_does_not_poll_on_every_chunk(self):
        p = Contract()
        self.run_child("import sys\nfor i in range(3000):\n    sys.stdout.write(f'line {i}\\n'); sys.stdout.flush()", p)
        self.assertLess(p.polls, 15)

    def test_the_last_events_still_arrive_when_polling_is_throttled(self):
        class Late(jr.Parser):
            name = "late"

            def __init__(self):
                super().__init__()
                self.lines = 0

            def on_line(self, line):
                self.lines += 1
                return []

            def poll(self):
                return [("progress", "t", f"saw {self.lines}")] if self.lines >= 500 else []

        self.run_child("import sys\nfor i in range(500):\n    sys.stdout.write(f'line {i}\\n'); sys.stdout.flush()",
                       Late())
        self.assertEqual([f["text"] for f in self.bus.of("step_progress")][-1:], ["saw 500"])

    JSON_THEN_PLAIN = "print('{\"text\": \"hello\"}'); print('plain')"

    def test_full_log_gets_rendered_lines(self):
        p = Rewriting()
        self.run_child(self.JSON_THEN_PLAIN, p)
        self.assertEqual(self.log.read_bytes(), b"hello\nplain\n")
        self.assertEqual(p.lines, ['{"text": "hello"}', "plain"])

    def test_capture_and_summarize_get_rendered_text(self):
        seen = []
        r = self.run_child(self.JSON_THEN_PLAIN, Rewriting(), capture=True, summarize=lambda text: seen.append(text))
        self.assertEqual(r.output, "hello\nplain\n")
        self.assertEqual(seen, ["hello\nplain\n"])

    def test_rewrites_log_is_ignored_under_tty(self):
        with mock.patch.object(jr, "_input_fd", return_value=None):
            self.run_child(self.JSON_THEN_PLAIN, Rewriting(), tty=True)
        self.assertIn(b'{"text": "hello"}', self.log.read_bytes())

    def test_a_failing_render_falls_back_to_raw_lines(self):
        with contextlib.redirect_stderr(io.StringIO()) as err:
            self.run_child(self.JSON_THEN_PLAIN, Rewriting(boom=True))
        self.assertEqual(self.log.read_bytes(), b'{"text": "hello"}\nplain\n')
        self.assertIn("parser rewriting disabled after error: ValueError('render broke')", err.getvalue())


class FakeToolkit:
    """Stands in for juneau_scripts.parsers: a name -> class map, with bind recorded (or failing)."""

    def __init__(self, parsers=None, bind_error=None):
        self.parsers = dict(parsers or {})
        self.REGISTRY = {name: name for name in self.parsers}
        self.bound = []
        self.bind_error = bind_error

    def bind(self, module):
        self.bound.append(module)
        if self.bind_error is not None:
            raise self.bind_error

    def known(self, name):
        return name in self.parsers

    def load(self, name):
        return self.parsers[name]


class Echo(jr.Parser):
    name = "echo"


NOPE_NOTE = "parser nope unavailable: not a registered parser; using generic"


class ExternalParserTest(unittest.TestCase):
    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        patcher = mock.patch.dict(sys.modules, {"juneau_run": jr})   # other tests may leave another copy registered
        patcher.start()
        self.addCleanup(patcher.stop)

    def toolkit(self, **kw):
        patcher = mock.patch.object(jr, "_toolkit_package", **kw)
        self.addCleanup(patcher.stop)
        return patcher.start()

    def test_the_bytecode_flag_is_restored_after_an_import_that_fails(self):
        saved = sys.dont_write_bytecode
        self.addCleanup(setattr, sys, "dont_write_bytecode", saved)
        sys.dont_write_bytecode = False
        seen = []

        def boom(name, *args):
            seen.append(sys.dont_write_bytecode)
            raise ImportError("broken")

        with mock.patch.object(jr.importlib, "import_module", side_effect=boom), \
                mock.patch.dict(sys.modules), mock.patch.object(sys, "path", list(sys.path)):
            sys.modules.pop("juneau_scripts", None)
            with mock.patch.object(jr.importlib.util, "find_spec", return_value=None):
                with self.assertRaises(ImportError):
                    jr._toolkit_package()
        self.assertEqual(seen, [True])
        self.assertIs(sys.dont_write_bytecode, False)

    def test_the_package_beside_the_script_goes_last_on_the_path(self):
        first = sys.path[0]
        with mock.patch.dict(sys.modules), mock.patch.object(sys, "path", list(sys.path)), \
                mock.patch.object(jr.importlib, "import_module", return_value=object()), \
                mock.patch.object(jr.importlib.util, "find_spec", return_value=None):
            sys.modules.pop("juneau_scripts", None)
            jr._toolkit_package()
            self.assertEqual(sys.path[0], first)
            self.assertEqual(sys.path[-1], str(Path(jr.__file__).resolve().parent))

    def test_builtin_names_never_touch_the_toolkit(self):
        self.toolkit(side_effect=AssertionError("toolkit touched"))
        self.assertIsInstance(jr._resolve_parser("maven"), jr.PARSERS["maven"])
        self.assertIsInstance(jr._resolve_parser("generic"), jr.GenericParser)

    def test_a_registered_name_loads_after_binding_juneau_run(self):
        fake = FakeToolkit({"echo": Echo})
        self.toolkit(return_value=fake)
        self.assertIsInstance(jr._resolve_parser("echo"), Echo)
        self.assertIsInstance(jr._resolve_parser("echo"), Echo)
        self.assertEqual(fake.bound, [jr])   # bound once, to this module

    def test_an_unknown_name_falls_back_with_one_note(self):
        self.toolkit(return_value=FakeToolkit())
        with contextlib.redirect_stderr(io.StringIO()) as err:
            first = jr._resolve_parser("nope")
            second = jr._resolve_parser("nope")
        self.assertIsInstance(first, jr._FallbackParser)
        first.step_id = "t"
        self.assertEqual(first.poll(), [("note", "warn", NOPE_NOTE, "t")])
        self.assertEqual(first.poll(), [])
        self.assertEqual(first.finish(0), [])
        self.assertIs(type(second), jr.GenericParser)
        self.assertEqual(err.getvalue(), f"juneau-run: {NOPE_NOTE}\n")

    def test_the_note_comes_with_finish_if_poll_never_ran(self):
        self.toolkit(return_value=FakeToolkit())
        with contextlib.redirect_stderr(io.StringIO()):
            p = jr._resolve_parser("nope")
        p.step_id = "t"
        p.on_line("boom")
        self.assertEqual(p.finish(1), [("note", "warn", NOPE_NOTE, "t"), ("cond", "boom"), ("failure", "boom", "t")])

    def test_a_missing_toolkit_names_its_reason(self):
        self.toolkit(side_effect=ImportError("no juneau_scripts beside juneau_run.py"))
        with contextlib.redirect_stderr(io.StringIO()) as err:
            p = jr._resolve_parser("bazel")
        self.assertIsInstance(p, jr._FallbackParser)
        self.assertEqual(err.getvalue(), "juneau-run: parser bazel unavailable: no juneau_scripts beside juneau_run.py; "
                                         "using generic\n")

    def test_a_failed_bind_is_cached_and_reported_per_name(self):
        fake = FakeToolkit({"echo": Echo}, bind_error=RuntimeError("bad bind"))
        self.toolkit(return_value=fake)
        with contextlib.redirect_stderr(io.StringIO()) as err:
            jr._resolve_parser("echo")
            jr._resolve_parser("other")
            jr._resolve_parser("echo")
        self.assertEqual(len(fake.bound), 1)
        self.assertEqual(err.getvalue().splitlines(),
                         ["juneau-run: parser echo unavailable: bad bind; using generic",
                          "juneau-run: parser other unavailable: bad bind; using generic"])

    def test_known_parser_checks_without_binding(self):
        fake = FakeToolkit({"echo": Echo})
        self.toolkit(return_value=fake)
        self.assertTrue(jr.known_parser("maven"))
        self.assertTrue(jr.known_parser("echo"))
        self.assertFalse(jr.known_parser("nope"))
        self.assertEqual(fake.bound, [])
        self.toolkit(side_effect=ImportError("gone"))
        self.assertFalse(jr.known_parser("echo"))

    def test_run_tool_publishes_the_note_once_and_keeps_it_out_of_the_log(self):
        self.toolkit(return_value=FakeToolkit())
        tmp = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, tmp, True)
        got = []
        for step in ("t", "u"):
            out = io.StringIO()
            sinks = jr.Sinks("none", str(tmp / f"{step}.log"))
            with mock.patch.dict(os.environ, {"RUN_MARKERS": "1"}), contextlib.redirect_stdout(out), \
                    contextlib.redirect_stderr(io.StringIO()):
                jr.run_tool(child("print('x')"), "nope", step, "T", sinks=sinks)
            got.append([m for m in markers(out.getvalue()) if m["ev"] == "note"])
            self.assertEqual((tmp / f"{step}.log").read_bytes(), b"x\n")
        self.assertEqual(got, [[{"ev": "note", "level": "warn", "text": NOPE_NOTE, "step": "t"}], []])


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
                # a slow child start can trip a quiet notice before the prompt text arrives; none may follow it
                kinds = [k for k, _ in self.bus.events]
                self.assertNotIn("quiet", kinds[kinds.index("prompt"):])

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
        with mock.patch.object(jr, "_foreground_tty", return_value=None), \
                mock.patch.object(jr.subprocess, "Popen", return_value=mock.Mock(**{"wait.return_value": 7})) as popen:
            result = jr.run_tool(["mvn", "test"], parser, "t", "T", cwd="/x")
        self.assertEqual(result.exit, 7)
        # no stdout/stderr: the child inherits the terminal, in a process group of its own
        popen.assert_called_once_with(["mvn", "test"], cwd="/x", env=None, **jr._OWN_GROUP)
        parser.assert_not_called()

    def test_pass_through_without_a_terminal_never_hands_one_over(self):
        os.environ.pop("RUN_MARKERS")
        with mock.patch.object(jr.os, "isatty", return_value=False), \
                mock.patch.object(jr.os, "tcsetpgrp", side_effect=AssertionError("tcsetpgrp")):
            result = jr.run_tool(child("import os, sys; sys.exit(os.getpgrp() == os.getpid() and 5)"), "generic",
                                 "t", "T")
        self.assertEqual(result.exit, 5)    # its own group all the same

    def test_pass_through_input_reaches_the_tools_stdin(self):
        os.environ.pop("RUN_MARKERS")
        result = jr.run_tool(child("import sys; sys.exit(len(sys.stdin.buffer.read()))"), "generic", "t", "T",
                             input=b"s3cret")
        self.assertEqual(result.exit, 6)

    def test_pass_through_on_a_terminal_hands_the_foreground_over_and_takes_it_back(self):
        os.environ.pop("RUN_MARKERS")
        handed = []
        with mock.patch.object(jr, "_foreground_tty", return_value=0), \
                mock.patch.object(jr, "_set_foreground", side_effect=lambda fd, pgid: handed.append(pgid)), \
                mock.patch.object(jr, "_wait_in_foreground", side_effect=lambda proc, fd: proc.wait()):
            result = jr.run_tool(child("import sys; sys.exit(4)"), "generic", "t", "T")
        self.assertEqual(result.exit, 4)
        self.assertEqual(len(handed), 2)
        self.assertNotEqual(handed[0], os.getpgrp())
        self.assertEqual(handed[1], os.getpgrp())

    def test_pass_through_takes_the_foreground_back_when_the_wait_fails(self):
        os.environ.pop("RUN_MARKERS")
        handed = []
        with mock.patch.object(jr, "_foreground_tty", return_value=0), \
                mock.patch.object(jr, "_set_foreground", side_effect=lambda fd, pgid: handed.append(pgid)), \
                mock.patch.object(jr, "_wait_in_foreground", side_effect=RuntimeError("wait")), \
                self.assertRaises(RuntimeError):
            jr.run_tool(child("import time; time.sleep(30)"), "generic", "t", "T")
        self.assertEqual(handed[-1], os.getpgrp())

    def test_a_ctrl_c_that_ends_the_tool_on_a_terminal_cancels(self):
        os.environ.pop("RUN_MARKERS")
        bus = jr.subscribe(Events())
        for code in ("import sys; sys.exit(130)", "import os, signal; os.kill(os.getpid(), signal.SIGINT)"):
            with self.subTest(code=code), mock.patch.object(jr, "_foreground_tty", return_value=0), \
                    mock.patch.object(jr, "_set_foreground"), \
                    mock.patch.object(jr, "_wait_in_foreground", side_effect=lambda proc, fd: proc.wait()), \
                    self.assertRaises(KeyboardInterrupt):
                jr.run_tool(child(code), "generic", "t", "T")
            self.assertEqual(bus.of("step_end")[-1]["exit"], 130)

    def test_a_stopped_tool_stops_the_runner_and_is_resumed_with_the_terminal(self):
        calls = []
        proc = mock.Mock(pid=4242, returncode=None)
        stopped = (signal.SIGTSTP << 8) | 0x7f   # the wait status of a child stopped by SIGTSTP
        with mock.patch.object(jr.os, "waitpid", side_effect=[(4242, stopped), (4242, 0)]), \
                mock.patch.object(jr.os, "getpgrp", return_value=99), \
                mock.patch.object(jr, "_foreground_of", return_value=99), \
                mock.patch.object(jr, "_set_foreground", side_effect=lambda fd, pgid: calls.append(("fg", pgid))), \
                mock.patch.object(jr.os, "killpg", side_effect=lambda pgid, sig: calls.append((pgid, sig))):
            self.assertEqual(jr._wait_in_foreground(proc, 0), 0)
        self.assertEqual(calls, [("fg", 99), (99, signal.SIGTSTP), ("fg", 4242), (4242, signal.SIGCONT)])

    def test_a_tool_stopped_for_reading_before_the_handoff_is_only_resumed(self):
        calls = []
        proc = mock.Mock(pid=4242, returncode=None)
        ttin = (signal.SIGTTIN << 8) | 0x7f      # stopped by SIGTTIN, though the tool holds the terminal
        with mock.patch.object(jr.os, "waitpid", side_effect=[(4242, ttin), (4242, 3 << 8)]), \
                mock.patch.object(jr, "_foreground_of", return_value=4242), \
                mock.patch.object(jr, "_set_foreground", side_effect=lambda fd, pgid: calls.append(("fg", pgid))), \
                mock.patch.object(jr.os, "killpg", side_effect=lambda pgid, sig: calls.append((pgid, sig))):
            self.assertEqual(jr._wait_in_foreground(proc, 0), 3)
        self.assertEqual(calls, [(4242, signal.SIGCONT)])

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

    def test_statuses_map_exit_codes_to_the_rows_status(self):
        statuses = {0: "ok", 3: "skip", 2: "fail"}
        result, out, _ = self.go(child("import sys; sys.exit(3)"), Recording(), statuses=statuses)
        self.assertEqual(result.exit, 3)
        self.assertEqual(markers(out)[-1], {"ev": "end", "id": "t", "status": "skip"})
        _, out, _ = self.go(child("import sys; sys.exit(2)"), Recording(), statuses=statuses)
        self.assertEqual(markers(out)[-1], {"ev": "end", "id": "t", "status": "fail", "exit": 2})
        _, out, _ = self.go(child("import sys; sys.exit(9)"), Recording(), statuses=statuses)
        self.assertEqual(markers(out)[-1], {"ev": "end", "id": "t", "status": "fail", "exit": 9})

    def test_exit_127_and_130_end_fail_whatever_statuses_says(self):
        for rc in (127, 130):
            _, out, _ = self.go(child(f"import sys; sys.exit({rc})"), Recording(), statuses={rc: "skip"})
            self.assertEqual(markers(out)[-1], {"ev": "end", "id": "t", "status": "fail", "exit": rc})

    def test_statuses_reach_the_bus_in_pass_through_mode(self):
        os.environ.pop("RUN_MARKERS")
        bus = jr.subscribe(Events())
        result = jr.run_tool(child("import sys; sys.exit(3)"), "generic", "t", "T", statuses={3: "skip"})
        self.assertEqual(result.exit, 3)
        self.assertEqual([(f["status"], f["exit"]) for f in bus.of("step_end")], [("skip", None)])

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


class Reporting(jr.Parser):
    """Announces each line it is given as a report: "<step> <kind> <path>"."""

    name = "reporting"

    def handle(self, line):
        target, kind, path = line.split(" ", 2)
        return [("report", target, kind, path)]


class ReportCopyTest(unittest.TestCase):
    """With markers on and $RUN_ARTIFACTS set, a report is copied under it and the copy announced (J0877)."""

    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        self.tmp = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.tmp, True)
        self.art = self.tmp / "art"
        patcher = mock.patch.dict(os.environ, {"RUN_MARKERS": "1", "RUN_ARTIFACTS": str(self.art)})
        patcher.start()
        self.addCleanup(patcher.stop)
        self.surefire = self.tmp / "mod" / "target" / "surefire-reports"
        self.surefire.mkdir(parents=True)
        (self.surefire / "TEST-a.xml").write_text("<testsuite tests='1'/>", encoding="utf-8")

    def reports(self, fn, *args, **kwargs):
        """The report markers fn prints, and its stderr."""
        out, err = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            fn(*args, **kwargs)
        return [m for m in markers(out.getvalue()) if m["ev"] == "report"], err.getvalue()

    def test_a_directory_is_copied_and_the_copy_announced_so_clean_cannot_take_it(self):
        got, _ = self.reports(jr.report, "mvn/mod", "surefire", self.surefire)
        copy = self.art / "reports" / "mvn.mod" / "surefire-reports"
        self.assertEqual(got, [{"ev": "report", "step": "mvn/mod", "kind": "surefire", "path": str(copy)}])
        shutil.rmtree(self.tmp / "mod")     # mvn clean
        self.assertEqual((copy / "TEST-a.xml").read_text(encoding="utf-8"), "<testsuite tests='1'/>")

    def test_a_relative_file_is_taken_from_cwd_and_copied(self):
        (self.tmp / "out.json").write_text("{}", encoding="utf-8")
        got, _ = self.reports(jr.report, "js", "jest-json", "out.json", cwd=self.tmp)
        copy = self.art / "reports" / "js" / "out.json"
        self.assertEqual(got[0]["path"], str(copy))
        self.assertEqual(copy.read_text(encoding="utf-8"), "{}")

    def test_a_glob_has_its_matches_copied_into_one_directory(self):
        (self.surefire / "TEST-b.xml").write_text("<testsuite/>", encoding="utf-8")
        got, _ = self.reports(jr.report, "t", "surefire", self.surefire / "TEST-*.xml")
        copy = self.art / "reports" / "t" / "surefire"
        self.assertEqual(got[0]["path"], str(copy))
        self.assertEqual(sorted(p.name for p in copy.iterdir()), ["TEST-a.xml", "TEST-b.xml"])

    def test_modules_and_reruns_never_overwrite_each_other(self):
        other = self.tmp / "other" / "target" / "surefire-reports"
        other.mkdir(parents=True)
        got, _ = self.reports(lambda: (jr.report("mvn/a", "surefire", self.surefire),
                                       jr.report("mvn/b", "surefire", other),
                                       jr.report("mvn/a", "surefire", self.surefire)))
        base = self.art / "reports"
        self.assertEqual([m["path"] for m in got], [str(base / "mvn.a" / "surefire-reports"),
                                                    str(base / "mvn.b" / "surefire-reports"),
                                                    str(base / "mvn.a" / "surefire-reports-2")])

    def test_a_report_already_under_run_artifacts_is_announced_in_place(self):
        xml = self.art / "t-pytest.xml"
        self.art.mkdir()
        xml.write_text("<testsuites/>", encoding="utf-8")
        got, _ = self.reports(jr.report, "t", "junitxml", xml)
        self.assertEqual(got[0]["path"], str(xml))
        self.assertFalse((self.art / "reports").exists())

    def test_a_path_that_matches_nothing_is_announced_as_given(self):
        got, err = self.reports(jr.report, "t", "surefire", self.tmp / "missing")
        self.assertEqual(got[0]["path"], str(self.tmp / "missing"))
        self.assertEqual(err, "")

    def test_a_failed_copy_warns_and_announces_the_original(self):
        with mock.patch.object(jr.shutil, "copytree", side_effect=OSError("disk full")):
            got, err = self.reports(jr.report, "t", "surefire", self.surefire)
        self.assertEqual(got[0]["path"], str(self.surefire))
        self.assertIn("disk full", err)

    def test_without_run_artifacts_the_path_is_announced_unchanged_and_nothing_copied(self):
        os.environ.pop("RUN_ARTIFACTS")
        got, _ = self.reports(jr.report, "t", "surefire", "target/surefire-reports")
        self.assertEqual(got, [{"ev": "report", "step": "t", "kind": "surefire", "path": "target/surefire-reports"}])
        self.assertFalse(self.art.exists())

    def test_without_markers_nothing_is_copied(self):
        os.environ.pop("RUN_MARKERS")
        jr.report("t", "surefire", self.surefire)
        self.assertFalse(self.art.exists())

    def test_run_tool_copies_a_parsers_reports(self):
        got, _ = self.reports(jr.run_tool, child(f"print('t surefire {self.surefire}')"), Reporting(), "t", "T")
        self.assertEqual(got[0]["path"], str(self.art / "reports" / "t" / "surefire-reports"))

    def test_run_tool_takes_a_parsers_relative_report_from_its_cwd(self):
        got, _ = self.reports(jr.run_tool, child("print('t surefire mod/target/surefire-reports')"), Reporting(),
                              "t", "T", cwd=str(self.tmp))
        self.assertEqual(got[0]["path"], str(self.art / "reports" / "t" / "surefire-reports"))

    def test_run_tool_reports_false_drops_report_markers_and_copies_nothing(self):
        got, _ = self.reports(jr.run_tool, child(f"print('t surefire {self.surefire}')"), Reporting(), "t", "T",
                              reports=False)
        self.assertEqual(got, [])
        self.assertFalse(self.art.exists())


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
        self.console.board.pending.return_value = []
        self.console.board.overall_handoff.return_value = None
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

    def test_a_pager_gone_before_the_final_flush_is_not_an_error(self):
        pager, _ = self.pager("sys.exit(0)")
        real_popen = subprocess.Popen

        def popen_and_wait(*args, **kwargs):    # the pager has really exited before anything is flushed to it
            proc = real_popen(*args, **kwargs)
            proc.wait()
            return proc

        out = TTYOut()
        with mock.patch.object(sys, "stdout", out), mock.patch.dict(os.environ, {"PAGER": pager}), \
                mock.patch.object(subprocess, "Popen", popen_and_wait):
            jr.show("short text", pager=True)  # under the buffer size: the write succeeds, the flush on close fails
        self.assertEqual(out.getvalue(), "")

    def test_a_failed_write_to_the_pager_still_closes_and_waits_for_it(self):
        pager, _ = self.pager("sys.stdin.read()")
        procs = []
        real_popen = subprocess.Popen

        def spawn(*args, **kwargs):
            procs.append(real_popen(*args, **kwargs))
            return procs[-1]

        previous = signal.getsignal(signal.SIGINT)
        self.addCleanup(lambda: [p.kill() or p.wait() for p in procs if p.poll() is None])
        with mock.patch.object(sys, "stdout", TTYOut()), mock.patch.dict(os.environ, {"PAGER": pager}), \
                mock.patch.object(jr.subprocess, "Popen", side_effect=spawn):
            with self.assertRaises(UnicodeEncodeError):
                jr.show("bad \udcff text", pager=True)
        self.assertEqual(len(procs), 1)
        self.assertIsNotNone(procs[0].returncode, "the pager was left running")
        self.assertIs(signal.getsignal(signal.SIGINT), previous)

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

    def test_handoff_exports_the_pending_rows(self):
        self.console.board.pending.return_value = [("release", "Release")]
        with jr.handoff() as env:
            self.assertEqual(json.loads(env["JUNEAU_RUN_PENDING"]), [["release", "Release"]])

    def test_handoff_exports_the_overall(self):
        self.console.board.overall_handoff.return_value = {"elapsed": 12.5, "done": 2, "pending": [["install", 30.0]]}
        with jr.handoff() as env:
            self.assertEqual(json.loads(env["JUNEAU_RUN_OVERALL"]),
                             {"elapsed": 12.5, "done": 2, "pending": [["install", 30.0]]})

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
