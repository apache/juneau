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
"""PTY mode: raw bytes byte-exact in the log, the size sidecar, run-view events with byte offsets, exit codes."""

import contextlib
import io
import json
import os
import signal
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

from support import FIXTURES, SCRIPT, jr, reset_run_state

SCREEN = FIXTURES / "pty-screen.py"
GOLDEN = (b"\x1b[1;31mred\x1b[0m plain\r\n\rcount 0\rcount 1\rcount 2\r\n\x1b[2Aup\x1b[2B\r\n"
          b"\x1b[?1049halt\x1b[?1049ldone\r\n")


def pty(tmp, *args, tool="generic", env=None, cwd=None):
    """Run the CLI in PTY mode with its log and events under tmp; returns (proc, log bytes, events)."""
    full_env = {k: v for k, v in os.environ.items() if not k.startswith(("RUN_", "JUNEAU_RUN_")) or k == "JUNEAU_RUN_LOG_DIR"}
    full_env["JUNEAU_RUN_HISTORY"] = "0"
    full_env.update(env or {})
    log, events = Path(tmp) / "out.log", Path(tmp) / "out.events.jsonl"
    r = subprocess.run([sys.executable, str(SCRIPT), "--pty", "--full-log", str(log), "--events", str(events),
                        *args[:-1], tool, "--", *args[-1]],
                       capture_output=True, env=full_env, cwd=cwd)
    evs = [json.loads(x) for x in events.read_text(encoding="utf-8").splitlines()] if events.exists() else []
    return r, log.read_bytes() if log.exists() else b"", evs


def py(code):
    return [sys.executable, "-c", code]


@unittest.skipUnless(os.name == "posix", "PTY mode is POSIX only")
class PtyTest(unittest.TestCase):
    def setUp(self):
        reset_run_state()
        self.tmp = tempfile.mkdtemp()

    def _run_in_process(self, code, parser):
        jr._events = jr._EventsFile(Path(self.tmp) / "e.jsonl", "generic")
        try:
            sinks = jr.Sinks("none", str(Path(self.tmp) / "o.log"))
            jr.run_pty(py(code), parser, "t", "T", cols=80, rows=24, sinks=sinks)
        finally:
            jr._events.close()
            jr._events = None
        log = (Path(self.tmp) / "o.log").read_bytes()
        evs = [json.loads(x) for x in (Path(self.tmp) / "e.jsonl").read_text(encoding="utf-8").splitlines()]
        return log, evs

    def test_rewrites_log_never_changes_the_pty_log(self):
        class Rewriting(jr.Parser):
            name = "rewriting"
            rewrites_log = True

            def render(self, line):
                return "rewritten"

        log, _ = self._run_in_process("print('{\"text\": \"hello\"}')", Rewriting())
        self.assertEqual(log, b'{"text": "hello"}\r\n')

    def test_poll_and_close_run_under_the_pty(self):
        class Sub(jr.Parser):
            name = "sub"

            def __init__(self):
                super().__init__()
                self.seen, self.sent, self.calls, self.seen_interactive = False, False, [], None

            def prepare(self, cmd, env, cwd):
                self.seen_interactive = self.interactive
                return cmd, env

            def handle(self, line):
                self.seen = True
                return []

            def poll(self):
                if self.seen and not self.sent:
                    self.sent = True
                    return [("sub_start", "t/sub", 1, "Sub")]
                return []

            def finish(self, exit_code):
                self.calls.append("finish")
                return []

            def close(self):
                self.calls.append("close")

        p = Sub()
        log, evs = self._run_in_process("import time; print('a', flush=True); time.sleep(0.5)", p)
        sub = [e for e in evs if e["ev"] == "step" and e["title"] == "Sub"]
        self.assertEqual(len(sub), 1)
        self.assertEqual(sub[0]["rawOffset"], len(log))   # just past the only line (its PTY line end included)
        self.assertEqual(p.calls, ["finish", "close"])
        self.assertIs(p.seen_interactive, True)

    def test_close_runs_when_the_tool_cannot_start_under_the_pty(self):
        class Closing(jr.Parser):
            name = "closing"

            def __init__(self):
                super().__init__()
                self.calls = []

            def finish(self, exit_code):
                self.calls.append("finish")
                return []

            def close(self):
                self.calls.append("close")

        p = Closing()
        sinks = jr.Sinks("none", str(Path(self.tmp) / "o.log"))
        with contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
            r = jr.run_pty(["/nonexistent/x"], p, "t", "T", cols=80, rows=24, sinks=sinks)
        self.assertEqual(r.exit, 127)
        self.assertEqual(p.calls.count("close"), 1)

    def test_close_runs_when_the_sinks_cannot_open_under_the_pty(self):
        class NoOpen(jr.Sinks):
            def open(self):
                raise OSError("disk gone")

        class Closing(jr.Parser):
            name = "closing"

            def __init__(self):
                super().__init__()
                self.calls = []

            def close(self):
                self.calls.append("close")

        p = Closing()
        sinks = NoOpen("none", str(Path(self.tmp) / "o.log"))
        with contextlib.redirect_stdout(io.StringIO()), self.assertRaises(OSError):
            jr.run_pty([sys.executable, "-c", "pass"], p, "t", "T", cols=80, rows=24, sinks=sinks)
        self.assertEqual(p.calls, ["close"])

    def test_close_runs_when_the_pty_cannot_be_opened(self):
        class Closing(jr.Parser):
            name = "closing"

            def __init__(self):
                super().__init__()
                self.calls = []

            def close(self):
                self.calls.append("close")

        p = Closing()
        sinks = jr.Sinks("none", str(Path(self.tmp) / "o.log"))
        with contextlib.redirect_stdout(io.StringIO()), mock.patch.object(jr.os, "openpty", side_effect=OSError("none")), \
                self.assertRaises(OSError):
            jr.run_pty([sys.executable, "-c", "pass"], p, "t", "T", cols=80, rows=24, sinks=sinks)
        self.assertEqual(p.calls, ["close"])
        self.assertIsNone(sinks._full)

    def test_raw_bytes_reach_the_log_byte_exact(self):
        r, log, _ = pty(self.tmp, [sys.executable, str(SCREEN)])
        self.assertEqual(log, GOLDEN)
        self.assertEqual(r.returncode, 3)
        self.assertEqual(r.stdout, b"")   # --console defaults to none in PTY mode

    def test_size_reaches_the_child_and_the_sidecar(self):
        _, log, _ = pty(self.tmp, "--size", "100x30", ["stty", "size"])
        self.assertEqual(log, b"30 100\r\n")
        self.assertEqual(json.loads((Path(self.tmp) / "out.log.size").read_text()), {"cols": 100, "rows": 30})

    def test_default_size_is_120x40(self):
        _, log, _ = pty(self.tmp, ["stty", "size"])
        self.assertEqual(log, b"40 120\r\n")
        self.assertEqual(json.loads((Path(self.tmp) / "out.log.size").read_text()), {"cols": 120, "rows": 40})

    def test_child_sees_a_terminal_and_term_but_no_markers(self):
        code = "import os,sys; print(sys.stdout.isatty(), os.environ.get('TERM'), os.environ.get('RUN_MARKERS'))"
        _, log, _ = pty(self.tmp, py(code), env={"RUN_MARKERS": "1"})
        self.assertEqual(log, b"True xterm-256color None\r\n")

    def test_events_are_run_view_events_even_without_run_markers(self):
        r, log, evs = pty(self.tmp, "--step", "t", "--title", "Tool", py("print('hi')"))
        self.assertEqual(evs, [{"ev": "step", "id": "t", "title": "Tool", "n": 1, "rawOffset": 0},
                               {"ev": "end", "id": "t", "status": "ok", "ms": evs[1]["ms"]},
                               {"ev": "done", "status": "ok"}])
        self.assertNotIn(b"##run", log)
        self.assertNotIn(b"##run", r.stdout)

    def test_nonzero_exit_passes_through_and_ends_fail(self):
        r, _, evs = pty(self.tmp, "--step", "t", py("import sys; sys.exit(3)"))
        self.assertEqual(r.returncode, 3)
        self.assertEqual((evs[-2]["status"], evs[-2]["exit"], evs[-1]), ("fail", 3, {"ev": "done", "status": "fail"}))

    def test_signal_death_exits_128_plus_n_with_a_note(self):
        r, _, evs = pty(self.tmp, "--step", "t", py("import os,signal; os.kill(os.getpid(), signal.SIGKILL)"))
        self.assertEqual(r.returncode, 137)
        self.assertIn({"ev": "note", "level": "warn", "text": "t killed by SIGKILL", "step": "t"}, evs)
        self.assertEqual(evs[-2]["exit"], 137)

    def test_missing_command_exits_127(self):
        r, _, evs = pty(self.tmp, "--step", "t", ["/no/such/tool"])
        self.assertEqual(r.returncode, 127)
        self.assertEqual((evs[-2]["status"], evs[-2]["exit"], evs[-1]["status"]), ("fail", 127, "fail"))
        self.assertIn(b"cannot start /no/such/tool", r.stderr)

    def test_maven_steps_carry_the_offset_of_their_building_line(self):
        src = (FIXTURES / "maven-serial-success.log").read_text(encoding="utf-8")
        colour = "".join(f"\x1b[1m{x}\x1b[0m\n" for x in src.splitlines())   # SGR around every line
        feed = Path(self.tmp) / "feed.txt"
        feed.write_text(colour, encoding="utf-8")
        _, log, evs = pty(self.tmp, "--step", "mvn", ["cat", str(feed)], tool="maven", cwd=self.tmp)
        steps = [e for e in evs if e["ev"] == "step"]
        self.assertEqual([s["id"] for s in steps], ["mvn", "mvn.demo-parent", "mvn.mod-a", "mvn.mod-b", "mvn.mod-c"])
        for s in steps[1:]:
            at = s["rawOffset"]
            self.assertTrue(at == 0 or log[at - 1:at] == b"\n", f"{s['id']} offset {at} is not a line start")
            line = log[at:log.index(b"\n", at)]
            self.assertIn(b"Building " + s["title"].encode(), line, s)
        for e in evs:
            if e["ev"] == "suite":
                at = e["rawOffset"]
                self.assertTrue(at == 0 or log[at - 1:at] == b"\n", e)

    def test_last_bytes_of_a_fast_exiting_child_are_not_lost(self):
        for i in range(50):
            tmp = tempfile.mkdtemp()
            _, log, _ = pty(tmp, py(f"print('line {i}')"))
            self.assertEqual(log, f"line {i}\r\n".encode(), i)

    def test_unexpected_error_kills_and_reaps_the_child(self):
        pidfile = Path(self.tmp) / "pid"
        code = f"import os,signal,time; signal.signal(signal.SIGHUP, signal.SIG_IGN); open({str(pidfile)!r},'w').write(str(os.getpid())); print('x', flush=True); time.sleep(60)"

        class Boom(jr.Sinks):
            def write_full(self, chunk):
                raise RuntimeError("sink failed")

        jr._events = jr._EventsFile(Path(self.tmp) / "e.jsonl", "generic")
        try:
            sinks = Boom("none", str(Path(self.tmp) / "o.log"))
            with self.assertRaises(RuntimeError):
                jr.run_pty(py(code), "generic", "t", "T", cols=80, rows=24, sinks=sinks)
        finally:
            jr._events.close()
            jr._events = None
        pid = int(pidfile.read_text())
        with self.assertRaises(ProcessLookupError):
            os.kill(pid, 0)

    def test_usage_errors(self):
        log = str(Path(self.tmp) / "x.log")
        cases = [
            ["--pty", "--full-log", log, "generic", "--", "true"],                      # no --events
            ["--pty", "--events", log + ".e", "generic", "--", "true"],                 # no --full-log
            ["--events", log + ".e", "--full-log", log, "generic", "--", "true"],       # no --pty
            ["--pty", "--size", "120", "--full-log", log, "--events", log + ".e", "generic", "--", "true"],
            ["--pty", "--size", "0x40", "--full-log", log, "--events", log + ".e", "generic", "--", "true"],
        ]
        for args in cases:
            r = subprocess.run([sys.executable, str(SCRIPT), *args], capture_output=True, text=True)
            self.assertEqual(r.returncode, 2, args)
            self.assertIn("usage:", r.stderr, args)


class EventsFileTest(unittest.TestCase):
    def setUp(self):
        reset_run_state()

    def test_ids_are_mapped_onto_the_step_id_grammar(self):
        self.assertEqual([jr._event_id(x) for x in ("mvn/mod-a", "a b", "-x", "", "x" * 80)],
                         ["mvn.mod-a", "a-b", "s-x", "s", "x" * 64])

    def test_clip_counts_utf16_units(self):
        self.assertEqual(jr._clip("ab\U0001F600c", 3), "ab")
        self.assertEqual(jr._clip("ab\U0001F600c", 4), "ab\U0001F600")

    def test_tests_become_a_suite_placeholder(self):
        path = Path(tempfile.mkdtemp()) / "e.jsonl"
        f = jr._EventsFile(path, "maven")
        jr._line_offset = 42
        f.write("tests", {"step": "mvn/mod-a", "total": 10, "fail": 1, "err": 2, "skip": 3})
        f.write("run", {"v": 1})
        f.write("report", {"step": "x", "kind": "surefire", "path": "/p"})
        f.close()
        self.assertEqual([json.loads(x) for x in path.read_text().splitlines()],
                         [{"ev": "suite", "step": "mvn.mod-a", "fw": "maven", "suite": "mvn.mod-a",
                           "counts": {"pass": 4, "fail": 3, "skip": 3}, "rawOffset": 42}])

    def test_pty_clean(self):
        self.assertEqual(jr.pty_clean(b"\x1b[1;31m[INFO]\x1b[0m x\x1b]0;title\x07 y\r"), "[INFO] x y")
        self.assertEqual(jr.pty_clean(b"10%\r50%\r\x1b[K100%"), "100%")
        self.assertEqual(jr.pty_clean(b"a\x1b(Bb\x1bPq#0\x1b\\c\x00\x7f"), "abc")


class NonPtyUnchangedTest(unittest.TestCase):
    def test_piped_mode_still_writes_markers_to_stdout(self):
        env = {k: v for k, v in os.environ.items() if not k.startswith(("RUN_", "JUNEAU_RUN_")) or k == "JUNEAU_RUN_LOG_DIR"}
        env["JUNEAU_RUN_HISTORY"] = "0"
        env["RUN_MARKERS"] = "1"
        r = subprocess.run([sys.executable, str(SCRIPT), "--step", "t", "generic", "--", "true"],
                           capture_output=True, text=True, env=env)
        self.assertIn('##run {"ev":"step","id":"t","n":1', r.stdout)


if __name__ == "__main__":
    unittest.main()
