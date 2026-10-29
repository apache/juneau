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
"""The console view: detail levels, the Board, the plain and live renderers, Console and session()."""

import contextlib
import io
import json
import os
import re
import shlex
import signal
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

from support import Screen, jr, reset_run_state


class DetailTest(unittest.TestCase):
    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)

    def test_resolution_order_flag_then_env_then_actionable(self):
        self.assertEqual(jr.resolve_detail(None), "actionable")
        with mock.patch.dict(os.environ, {"JUNEAU_TERMINAL_DETAIL": "modules"}):
            self.assertEqual(jr.resolve_detail(None), "modules")
            self.assertEqual(jr.resolve_detail("all"), "all")

    def test_unknown_level_lists_the_valid_ones(self):
        with self.assertRaises(ValueError) as e:
            jr.resolve_detail("loud")
        self.assertIn("summary, actionable, modules, all", str(e.exception))

    def test_export_sets_the_env_for_children(self):
        with mock.patch.dict(os.environ, {}):
            self.assertEqual(jr.export_detail("summary"), "summary")
            self.assertEqual(os.environ["JUNEAU_TERMINAL_DETAIL"], "summary")

    def test_note_levels_by_detail(self):
        table = {
            "summary": {"error"},
            "actionable": {"warn", "error"},
            "modules": {"warn", "error"},
            "all": {"info", "warn", "error"},
        }
        for detail, shown in table.items():
            for level in ("info", "warn", "error"):
                with self.subTest(detail=detail, level=level):
                    self.assertEqual(jr.shows_note(detail, level), level in shown)

    def test_finished_module_lines_by_detail(self):
        # (detail, failed, has_tests) -> shown
        table = {
            ("summary", True, True): False, ("summary", False, True): False,
            ("actionable", True, True): True, ("actionable", True, False): True,
            ("actionable", False, True): False,
            ("modules", False, True): True, ("modules", False, False): False, ("modules", True, False): True,
            ("all", False, False): True, ("all", False, True): True,
        }
        for (detail, failed, has_tests), shown in table.items():
            with self.subTest(detail=detail, failed=failed, has_tests=has_tests):
                self.assertEqual(jr.shows_module(detail, failed, has_tests), shown)


class Clock:
    """An injectable monotonic clock."""

    def __init__(self):
        self.t = 100.0

    def __call__(self):
        return self.t

    def advance(self, seconds):
        self.t += seconds


def feed(board, *events):
    blocks = []
    for kind, fields in events:
        blocks += board.apply(kind, fields)
    return blocks


def plain(board, blocks, clock):
    out = []
    jr.PlainRenderer(out.append, clock=clock).render(board, blocks)
    return "".join(out)


def maven_events(step="tests", tests_mode=True):
    """A three-module reactor: a pom parent, a passing module and a failing one."""
    return [
        ("step_start", {"id": step, "n": 3, "title": "Tests", "parent": None}),
        ("reactor", {"step": step, "size": 3, "poms": [f"{step}/p"], "tests": tests_mode}),
        ("unit_start", {"step": step, "id": f"{step}/p", "title": "Parent"}),
        ("module_compiled", {"step": step, "module": f"{step}/p", "ok": True}),
        ("unit_end", {"step": step, "id": f"{step}/p", "status": "ok", "ms": 100, "totals": None}),
        ("unit_start", {"step": step, "id": f"{step}/a", "title": "Mod A"}),
        ("unit_dot", {"step": step, "id": f"{step}/a", "outcome": "pass"}),
        ("module_compiled", {"step": step, "module": f"{step}/a", "ok": True}),
        ("module_tested", {"step": step, "module": f"{step}/a"}),
        ("unit_end", {"step": step, "id": f"{step}/a", "status": "ok", "ms": 41000, "totals": (7139, 0, 0, 0)}),
        ("unit_start", {"step": step, "id": f"{step}/b", "title": "Mod B"}),
        ("unit_dot", {"step": step, "id": f"{step}/b", "outcome": "fail"}),
        ("module_compiled", {"step": step, "module": f"{step}/b", "ok": True}),
        ("module_tested", {"step": step, "module": f"{step}/b"}),
        ("unit_end", {"step": step, "id": f"{step}/b", "status": "fail", "ms": 2000, "totals": (10, 2, 0, 0)}),
    ]


class BoardTest(unittest.TestCase):
    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        self.clock = Clock()

    def board(self, detail="actionable", nested=False, full_log=None):
        return jr.Board(detail, nested=nested, full_log=full_log, clock=self.clock)

    def test_progress_is_kept_on_the_row_but_never_printed_by_the_plain_renderer(self):
        b = self.board()
        feed(b, ("step_start", {"id": "py", "n": 1, "title": "Python", "parent": None}))
        self.assertEqual(feed(b, ("step_progress", {"step": "py", "text": "52% \u00b7 test_foo.py"})), [])
        self.assertEqual(b.rows["py"].progress, "52% \u00b7 test_foo.py")
        blocks = feed(b, ("step_end", {"id": "py", "status": "ok", "ms": 2000, "exit": None, "summary": "3 passed",
                                       "totals": None}))
        self.assertEqual(plain(b, blocks, self.clock), "✔ " + "Python".ljust(10) + "  3 passed  0:02\n")
        self.assertIsNone(b.rows["py"].progress)

    def test_a_sub_rows_step_end_clears_the_progress_it_put_on_its_parent_row(self):
        b = self.board()
        feed(b, ("step_start", {"id": "verify", "n": 1, "title": "Verify", "parent": None}),
             ("step_start", {"id": "verify.1", "n": 2, "title": "Compile", "parent": "verify"}),
             ("step_progress", {"step": "verify.1", "text": "52%"}))
        self.assertEqual(b.rows["verify"].progress, "52%")
        feed(b, ("step_end", {"id": "verify.1", "status": "ok", "ms": 10, "exit": 0, "summary": None, "totals": None}))
        self.assertIsNone(b.rows["verify"].progress)

    def test_an_alias_only_sub_rows_step_end_clears_the_parents_progress(self):
        b = self.board()
        feed(b, ("step_start", {"id": "verify", "n": 1, "title": "Verify", "parent": None}))
        b.aliases["verify.1"] = "verify"
        feed(b, ("step_progress", {"step": "verify.1", "text": "52%"}))
        self.assertEqual(b.rows["verify"].progress, "52%")
        feed(b, ("step_end", {"id": "verify.1", "status": "ok", "ms": 10, "exit": 0, "summary": None, "totals": None}))
        self.assertIsNone(b.rows["verify"].progress)

    def test_progress_for_an_unknown_step_is_ignored(self):
        self.assertEqual(feed(self.board(), ("step_progress", {"step": "nope", "text": "1%"})), [])

    def test_a_summary_row_uses_its_label_and_elapsed_time(self):
        b = self.board()
        blocks = feed(b, ("step_start", {"id": "bom", "n": 2, "title": "BOM completeness", "parent": None,
                                         "label": "BOM"}))
        self.assertEqual(blocks, [])
        blocks = feed(b, ("step_end", {"id": "bom", "status": "ok", "ms": 2000, "exit": None,
                                       "summary": "73 modules", "totals": None}))
        self.assertEqual(plain(b, blocks, self.clock), "✔ " + "BOM".ljust(10) + "  73 modules  0:02\n")

    def test_elapsed_under_a_second_is_left_off(self):
        b = self.board()
        blocks = feed(b, ("step_start", {"id": "install", "n": 7, "title": "Install", "parent": None}),
                      ("step_end", {"id": "install", "status": "ok", "ms": 3, "exit": None,
                                    "summary": "(done by Tests)", "totals": None}))
        self.assertEqual(plain(b, blocks, self.clock), "✔ " + "Install".ljust(10) + "  (done by Tests)\n")

    def test_tests_mode_boxes_and_the_failed_module_keeps_its_sub_row(self):
        b = self.board()
        blocks = feed(b, *maven_events())
        # Setup finishes once every pom box is set; actionable prints no module line (the row's text names it)
        self.assertEqual(plain(b, blocks, self.clock), "✔ " + "Setup".ljust(10) + "  #  1 pom module\n")
        blocks = feed(b, ("step_end", {"id": "tests", "status": "fail", "ms": 161000, "exit": 1, "summary": None,
                                       "totals": {"total": 7149, "fail": 2, "err": 0, "skip": 0}}))
        # the pom parent left the bar for the Setup row, so the bar counts the two real modules
        self.assertEqual(plain(b, blocks, self.clock),
                         "✗ " + "Tests".ljust(10) + "  #F  2/2  7,149 tests, 2 failed: Mod B  2:41\n")
        self.assertEqual(b.failed_title, "Tests")

    def test_compile_mode_boxes_come_from_module_compiled(self):
        b = self.board()
        events = [e for e in maven_events("build", tests_mode=False) if e[0] != "module_tested"]
        events[0] = ("step_start", {"id": "build", "n": 3, "title": "Build", "parent": None, "label": "Compile"})
        events.insert(12, ("module_compiled", {"step": "build", "module": "build/b", "ok": False}))
        blocks = feed(b, *events)
        blocks += feed(b, ("step_end", {"id": "build", "status": "fail", "ms": 36000, "exit": 1, "summary": None,
                                        "totals": None}))
        self.assertEqual(plain(b, blocks, self.clock), "✔ " + "Setup".ljust(10) + "  #  1 pom module\n"
                         + "✗ " + "Compile".ljust(10) + "  #F  2/2  1 failed: Mod B  0:36\n")

    def test_a_module_that_compiled_green_turns_red_on_a_later_error(self):
        b = self.board()
        feed(b, ("step_start", {"id": "build", "n": 3, "title": "Build", "parent": None}),
             ("reactor", {"step": "build", "size": 1, "poms": [], "tests": False}),
             ("unit_start", {"step": "build", "id": "build/a", "title": "Mod A"}),
             ("module_compiled", {"step": "build", "module": "build/a", "ok": True}),
             ("module_compiled", {"step": "build", "module": "build/a", "ok": False}))
        self.assertEqual(b.rows["build"].boxes(), ["fail"])

    def test_modules_detail_keeps_modules_that_ran_tests(self):
        b = self.board("modules")
        blocks = feed(b, *maven_events())
        self.assertEqual([blk[2].title for blk in blocks if blk[0] == "module"], ["Mod A", "Mod B"])

    def test_all_detail_keeps_every_module(self):
        b = self.board("all")
        blocks = feed(b, *maven_events())
        self.assertEqual([blk[2].title for blk in blocks if blk[0] == "module"], ["Parent", "Mod A", "Mod B"])

    def test_summary_detail_keeps_no_module(self):
        b = self.board("summary")
        self.assertEqual([blk for blk in feed(b, *maven_events()) if blk[0] == "module"], [])

    def test_notes_wait_for_their_row_and_skip_lines_a_failure_already_showed(self):
        b = self.board()
        feed(b, ("step_start", {"id": "tests", "n": 3, "title": "Tests", "parent": None}))
        self.assertEqual(feed(b, ("note", {"level": "warn", "text": "careful", "step": "tests"}),
                              ("note", {"level": "error", "text": "Bad.java:[1,21] incompatible types",
                                        "step": "tests"})), [])
        # failure text on a running row is held and goes out beneath it, replacing the error's identical line
        self.assertEqual(feed(b, ("failure", {"text": "Bad.java:[1,21] incompatible types", "step": "tests"})), [])
        blocks = feed(b, ("step_end", {"id": "tests", "status": "fail", "ms": 0, "exit": 1, "summary": "x",
                                       "totals": None}))
        r = b.rows["tests"]
        self.assertEqual((r.reason, r.reason_level), ("Bad.java:[1,21] incompatible types", "error"))
        self.assertEqual(blocks[0], ("row", r))
        self.assertIn(("logonly", "    ⚠ careful"), blocks)
        hints = [blk for blk in blocks if blk[0] == "hint"]
        self.assertEqual(hints, [("hint", r, ["Bad.java:[1,21] incompatible types"])])

    def test_a_note_without_a_step_attaches_to_the_latest_running_row(self):
        b = self.board()
        feed(b, ("step_start", {"id": "perf", "n": None, "title": "Perf", "parent": None}),
             ("note", {"level": "warn", "text": "13 over baseline", "step": None}))
        blocks = feed(b, ("step_end", {"id": "perf", "status": "ok", "ms": 0, "exit": None, "summary": "x",
                                       "totals": None}))
        r = b.rows["perf"]
        self.assertEqual((r.reason, r.warned), ("13 over baseline", True))
        self.assertEqual(blocks[1:], [("logonly", "    ⚠ 13 over baseline")])
        self.assertEqual(plain(b, blocks, self.clock), "⚠ " + "Perf".ljust(10) + "  13 over baseline\n")

    def test_a_note_with_no_row_prints_at_once(self):
        self.assertEqual(feed(self.board(), ("note", {"level": "error", "text": "no", "step": None})),
                         [("text", "✗ no")])

    def test_info_notes_and_say_text_show_only_at_all(self):
        for detail, shown in (("actionable", False), ("all", True)):
            with self.subTest(detail=detail):
                b = self.board(detail)
                blocks = feed(b, ("note", {"level": "info", "text": "fyi", "step": None}),
                              ("say", {"text": "Running: mvn", "level": "info", "step": None}))
                self.assertEqual(blocks, [("text", "ℹ fyi"), ("text", "Running: mvn")] if shown else [])

    def test_a_warn_say_shows_at_actionable(self):
        self.assertEqual(feed(self.board(), ("say", {"text": "⚠ dirty", "level": "warn", "step": None})),
                         [("text", "⚠ dirty")])

    def test_header_and_final_line(self):
        b = self.board(full_log="/tmp/push-full.log")
        blocks = feed(b, ("session", {"title": "Push", "header": ["Juneau", "master", "PGP ✓"], "nested": False}))
        self.assertEqual(plain(b, blocks, self.clock), "▶ Push: Juneau · master · PGP ✓\n")
        self.clock.advance(348)
        self.assertEqual(plain(b, feed(b, ("done", {"status": "ok", "commit": None})), self.clock),
                         "▶ Overall  ✅ Done in 5:48\n")

    def test_final_line_forms(self):
        f = jr.format_final
        self.assertEqual(f("ok", 243000, None, None), "Overall  ✅ Done in 4:03")
        self.assertEqual(f("ok", 243000, None, None, 5, 5), "Overall  ✅ Done in 4:03  · 5 of 5 steps")
        self.assertEqual(f("fail", 0, "Tests", "/tmp/f.log"), "Overall  ❌ Failed at Tests  · full log: /tmp/f.log")
        self.assertEqual(f("fail", 0, "Docs build", "/tmp/f.log", 9, 13),
                         "Overall  ❌ Failed at Docs build  · 9 of 13 steps  · full log: /tmp/f.log")
        self.assertEqual(f("fail", 0, None, None), "Overall  ❌ Failed")
        self.assertEqual(f("cancelled", 62000, None, None, 4, 13), "Overall  ⚠ Cancelled after 1:02  · 4 of 13 steps")
        self.assertEqual(f("ok", 0, None, None, 0, 0), "Overall  ✅ Done in 0:00")     # no counted steps: no counts
        self.assertEqual(jr.fmt_elapsed(3723000), "1:02:03")

    def test_final_line_marks_the_row_cyan_under_colour(self):
        block = ("final", "ok", 1000, None, None, 1, 1)
        self.assertEqual(jr.final_line(block), "▶ Overall  ✅ Done in 0:01  · 1 of 1 steps")
        self.assertTrue(jr.final_line(block, color=True).startswith("\x1b[36m▶"))

    def test_a_nested_board_has_no_header_and_no_final_line(self):
        b = self.board(nested=True)
        self.assertEqual(feed(b, ("session", {"title": "t", "header": [], "nested": True}),
                              ("done", {"status": "ok", "commit": None})), [])

    def test_the_label_width_comes_from_the_env_or_the_longest_label(self):
        b = self.board()
        feed(b, ("step_start", {"id": "x", "n": 1, "title": "A very long label", "parent": None}))
        self.assertEqual(b.width(), len("A very long label"))
        with mock.patch.dict(os.environ, {"JUNEAU_TERMINAL_LABEL_WIDTH": "20"}):
            self.assertEqual(b.width(), 20)

    def test_a_sub_rows_maven_modules_draw_on_its_parent_row(self):
        """release.py: each command is a run_tool sub-row (parent=<step>), and Maven's modules fill the step's bar."""
        b = self.board()
        blocks = feed(b, ("step_start", {"id": "verify", "n": 1, "title": "Clean verify", "parent": None,
                                         "label": "Verify"}),
             ("step_start", {"id": "verify.1", "n": 1, "title": "git status", "parent": "verify"}),
             ("step_end", {"id": "verify.1", "status": "ok", "ms": 10, "exit": None, "summary": None,
                           "totals": None}))
        self.assertEqual(b.rows["verify"].boxes(), [])        # a step's only command is the step itself: no box until a second one starts
        events = maven_events("verify.2")
        events[0] = ("step_start", {"id": "verify.2", "n": 2, "title": "mvn -B clean verify", "parent": "verify"})
        blocks += feed(b, *events, ("quiet", {"step": "verify.2", "seconds": 20}))
        r = b.rows["verify"]
        self.assertEqual(list(b.rows), ["verify"])
        self.assertEqual((r.boxes(), r.size, r.quiet), (["pass", "fail"], 3, 20))     # the pom is on the Setup row, not in the bar
        self.assertEqual(feed(b, ("step_end", {"id": "verify.2", "status": "fail", "ms": 5000, "exit": 1,
                                               "summary": None,
                                               "totals": {"total": 7149, "fail": 2, "err": 0, "skip": 0}})), [])
        blocks += feed(b, ("step_end", {"id": "verify", "status": "fail", "ms": 6000, "exit": None, "summary": None,
                                        "totals": None}))
        self.assertEqual(plain(b, blocks, self.clock), "✔ " + "Setup".ljust(10) + "  #  1 pom module\n"
                         + "✗ " + "Verify".ljust(10) + "  #F  2/2  7,149 tests, 2 failed: Mod B  0:06\n")

    def test_a_module_that_never_ended_is_skipped_when_its_step_ends(self):
        b = self.board()
        feed(b, ("step_start", {"id": "tests", "n": 3, "title": "Tests", "parent": None}),
             ("reactor", {"step": "tests", "size": 2, "poms": [], "tests": True}),
             ("unit_start", {"step": "tests", "id": "tests/a", "title": "Mod A"}),
             ("unit_dot", {"step": "tests", "id": "tests/a", "outcome": "pass"}),
             ("unit_start", {"step": "tests", "id": "tests/b", "title": "Mod B"}),
             ("step_end", {"id": "tests", "status": "fail", "ms": 1000, "exit": 1, "summary": None, "totals": None}))
        self.assertEqual(b.rows["tests"].boxes(), ["pass", "skip"])


class TTY(io.StringIO):
    def isatty(self):
        return True


class ConsoleTest(unittest.TestCase):
    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        patcher = mock.patch.dict(os.environ, {})
        patcher.start()
        self.addCleanup(patcher.stop)
        for key in ("TERMINAL_MARKERS", "TERM", "JUNEAU_TERMINAL_CONSOLE", "JUNEAU_TERMINAL_FULL_LOG", "JUNEAU_TERMINAL_BOARD_LOG"):
            os.environ.pop(key, None)
        self.clock = Clock()

    def console(self, stream=None, **sinks):
        c = jr.Console(jr.Sinks(**sinks), stream=stream or io.StringIO(), clock=self.clock)
        self.addCleanup(c.close)
        return c

    def test_screen_mode(self):
        self.assertIsNone(self.console(console="full").mode)
        self.assertIsNone(self.console(console="none").mode)
        self.assertEqual(self.console(console="board").mode, "plain")
        self.assertEqual(self.console(TTY(), console="board").mode, "live")
        with mock.patch.dict(os.environ, {"TERM": "dumb"}):
            self.assertEqual(self.console(TTY(), console="board").mode, "plain")
        with mock.patch.dict(os.environ, {"JUNEAU_TERMINAL_LIVE": "0"}):
            self.assertEqual(self.console(TTY(), console="board").mode, "plain")
        with mock.patch.dict(os.environ, {"TERMINAL_MARKERS": "1"}):
            self.assertIsNone(self.console(TTY(), console="board").mode)

    def test_the_board_log_gets_the_rows_but_no_heartbeat(self):
        import tempfile
        from pathlib import Path
        with tempfile.TemporaryDirectory() as tmp:
            log = Path(tmp) / "c.log"
            c = self.console(console="none", board_log=str(log))
            for kind, fields in maven_events():
                c(kind, fields)
            self.clock.advance(20)
            c.tick()
            c("step_end", {"id": "tests", "status": "fail", "ms": 20000, "exit": 1, "summary": None, "totals": None})
            c.close()
            text = log.read_text(encoding="utf-8")
        self.assertNotIn("Mod B  0:02", text)          # no module lines at actionable
        self.assertIn("✔ " + "Setup".ljust(10) + "  #  1 pom module\n" + "✗ " + "Tests".ljust(10) + "  #F  2/2", text)
        self.assertNotIn("…", text)

    def test_plain_screen_heartbeat_every_15_seconds(self):
        out = io.StringIO()
        c = self.console(out, console="board")
        c("step_start", {"id": "tests", "n": 3, "title": "Tests", "parent": None})
        c("unit_start", {"step": "tests", "id": "tests/a", "title": "Mod A"})
        c("tests", {"step": "tests", "total": 10, "fail": 1, "err": 0, "skip": 0})
        self.clock.advance(14)
        c.tick()
        self.assertEqual(out.getvalue(), "")
        self.clock.advance(1)
        c.tick()
        self.assertEqual(out.getvalue(), "… 10 tests, 1 failed · 1 module running  0:15\n")

    def test_close_still_closes_the_board_log_when_the_screen_fails_to_close(self):
        with tempfile.TemporaryDirectory() as tmp:
            c = self.console(TTY(), console="board", board_log=str(Path(tmp) / "c.log"))
            log = c._log
            c.screen = mock.Mock(**{"close.side_effect": OSError("screen gone")})
            c.close()
        self.assertTrue(log.closed)
        self.assertIsNone(c._log)

    def test_write_board_is_silent_while_a_console_is_active(self):
        out = io.StringIO()
        jr._console = self.console(console="board")
        with contextlib.redirect_stdout(out):
            jr.Sinks(console="board").write_board("✓ old board line")
        self.assertEqual(out.getvalue(), "")


class OverallDisplayTest(unittest.TestCase):
    """The Overall line in the live region and the plain heartbeat (spec 6.4)."""

    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        self.clock = Clock()

    def board(self, plan=("a", "b"), hist=None, nested=False):
        hist = {"a": 60.0, "b": 60.0} if hist is None else hist
        b = jr.Board("actionable", nested=nested, full_log=None, clock=self.clock,
                     estimator=lambda step, tool, sig=None: jr.Estimate(hist[step], None, None, 3) if step in hist else None)
        b.apply("session", dict(title="T", header=[], nested=nested))
        if plan is not None:
            b.apply("plan", dict(ids=list(plan)))
        b.apply("step_start", dict(id="a", n=1, title="A", parent=None, tool=None, sig=""))
        self.clock.advance(30)
        return b

    def live(self):
        return jr.LiveRenderer(lambda text: None, size=lambda: os.terminal_size((80, 20)), clock=self.clock, color=False)

    def test_bar_only_row_shows_its_current_file_under_it_until_it_ends(self):
        b = self.board(plan=None)
        feed(b, ("units", {"step": "a", "total": 3, "kind": "files"}),
             ("step_progress", {"step": "a", "text": "test_x.py"}))
        lines = self.live().region(b, 79, 18)
        self.assertEqual(lines[1], "    test_x.py")
        self.assertEqual(sum(1 for line in lines if "test_x.py" in line), 1)
        feed(b, ("step_end", {"id": "a", "status": "ok", "ms": 1, "summary": None, "totals": None}))
        self.assertFalse(any("test_x.py" in line for line in self.live().region(b, 79, 18)))

    def test_overall_marker_is_cyan_in_colour_only(self):
        for color in (True, False):
            with self.subTest(color=color):
                live = jr.LiveRenderer(lambda text: None, size=lambda: os.terminal_size((80, 20)), clock=self.clock, color=color)
                last = live.region(self.board(), 79, 18)[-1]
                self.assertEqual(last.startswith("\x1b[36m▶\x1b[0m Overall"), color)
                self.assertEqual(jr._ANSI.sub("", last)[:9], "▶ Overall")

    def test_last_line_is_overall_and_counts_in_max_lines(self):
        b = self.board()
        lines = self.live().region(b, 79, 18)
        self.assertEqual(lines[-1], "▶ Overall  25%  ~1:30 left  · 0 of 2 steps")       # 30 / (30 + 30 + 60)
        self.assertTrue(lines[0][2:].startswith("A"))
        self.assertEqual(len(self.live().region(b, 79, 1)), 1)                          # the budget still holds

    def test_overall_survives_a_cut_and_the_line_above_it_goes(self):
        b = self.board()
        b.apply("reactor", dict(step="a", size=5, poms=["p0", "p1"], tests=False))
        full = self.live().region(b, 79, 18)
        self.assertGreater(len(full), 2)
        for cap in range(1, len(full)):
            with self.subTest(cap=cap):
                lines = self.live().region(b, 79, cap)
                self.assertLessEqual(len(lines), cap)         # a running row drops with its Setup line
                self.assertTrue(lines[-1].startswith("▶ Overall"), lines)

    def test_overall_sits_below_the_pending_rows(self):
        b = self.board()
        b.apply("plan", dict(ids=["a", "b"]))
        lines = self.live().region(b, 79, 18)
        self.assertTrue(lines[-1].startswith("▶ Overall"), lines)

    def test_absent_without_a_plan_without_history_and_when_nested(self):
        for b in (self.board(plan=None), self.board(hist={}), self.board(nested=True)):
            self.assertTrue(self.live().region(b, 79, 18)[0][2:].startswith("A"))

    def test_nested_board_with_a_parent_overall_ends_on_the_overall_line(self):
        env = {"JUNEAU_TERMINAL_OVERALL": json.dumps({"elapsed": 30, "done": 1, "pending": [["install", 60.0]]})}
        with mock.patch.dict(os.environ, env):
            b = self.board(nested=True)
        lines = self.live().region(b, 79, 18)
        self.assertEqual(lines[-1], "▶ Overall  28%  ~2:30 left  · 1 of 4 steps")      # 60 / (60 + 150): a and b + install, 30s + 30s of parent time

    def test_setup_row_sits_above_the_overall_line(self):
        b = self.board()
        b.apply("reactor", dict(step="a", size=5, poms=["p0", "p1"], tests=False))
        lines = self.live().region(b, 79, 18)
        self.assertTrue(lines[-1].startswith("▶ Overall"))
        self.assertEqual(lines[0].split()[1], "Setup")

    def test_plain_heartbeat_gains_percent_and_remaining(self):
        out = []
        b = self.board()
        r = jr.PlainRenderer(out.append, heartbeat=True, clock=self.clock)
        self.clock.advance(jr.HEARTBEAT_SECONDS)
        r.tick(b)
        self.assertTrue(out[0].endswith(" · 37% · ~1:15 left\n"), out)

    def test_plain_heartbeat_unchanged_without_a_plan(self):
        out = []
        b = self.board(plan=None)
        r = jr.PlainRenderer(out.append, heartbeat=True, clock=self.clock)
        self.clock.advance(jr.HEARTBEAT_SECONDS)
        r.tick(b)
        self.assertNotIn("left", out[0])


class SessionTest(unittest.TestCase):
    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        self.addCleanup(lambda: jr._end_session())
        patcher = mock.patch.dict(os.environ, {"JUNEAU_TERMINAL_CONSOLE": "board"})
        patcher.start()
        self.addCleanup(patcher.stop)
        os.environ.pop("TERMINAL_MARKERS", None)
        # `python3 -m unittest` (3.10 and earlier) sets argv[0] to "python3 -m unittest", which names the default full log
        argv = mock.patch.object(jr.sys, "argv", ["/x/scripts/test.py"])
        argv.start()
        self.addCleanup(argv.stop)

    def test_session_prints_the_header_and_marks_the_env(self):
        out = io.StringIO()
        with contextlib.redirect_stdout(out), mock.patch.object(jr.sys, "argv", ["/x/scripts/test.py"]):
            c = jr.session("Test", ["Juneau", "master", "a message"])
        self.assertIs(jr._console, c)
        self.assertEqual(os.environ["JUNEAU_TERMINAL_SESSION"], "1")
        self.assertRegex(out.getvalue(), r'^▶ Test: Juneau · master · a message\n▶ Output: tail -f \S+/test-\d{8}-\d{6}(-\d+)?\.log\n'
                                         r'▶ Board: tail -f \S+\.board\.log\n$')

    def test_a_console_that_fails_to_start_leaves_the_env_unset(self):
        with tempfile.TemporaryDirectory() as tmp:
            Path(tmp, "file").write_text("")
            sinks = jr.Sinks(console="board", board_log=f"{tmp}/file/board.log")
            with contextlib.redirect_stdout(io.StringIO()), self.assertRaises(OSError):
                jr.session("t", sinks=sinks)
        self.assertNotIn("JUNEAU_TERMINAL_SESSION", os.environ)
        self.assertIsNone(jr._console)

    def test_a_nested_session_prints_no_header(self):
        os.environ["JUNEAU_TERMINAL_SESSION"] = "1"
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            jr.session("Juneau test", ["master"])
            jr.done("ok")
        self.assertEqual(out.getvalue(), "")

    def test_the_header_prints_the_title_and_items_then_a_tail_command_for_the_full_log(self):
        with tempfile.TemporaryDirectory() as tmp:
            out = io.StringIO()
            with contextlib.redirect_stdout(out):
                jr.session("Push", ["Juneau", "master", "Fix it"], sinks=jr.Sinks(full_log=f"{tmp}/push-full.log"))
            self.assertEqual(out.getvalue(),
                             f'▶ Push: Juneau · master · Fix it\n▶ Output: tail -f {shlex.quote(tmp + "/push-full.log")}\n')

    def test_the_board_tail_command_takes_its_own_line_after_the_full_logs(self):
        with tempfile.TemporaryDirectory() as tmp:
            out = io.StringIO()
            with contextlib.redirect_stdout(out), mock.patch.object(jr.shutil, "get_terminal_size",
                                                                    return_value=os.terminal_size((300, 24))):
                jr.session("T", ["b"], sinks=jr.Sinks(full_log=f"{tmp}/a.log", board_log=f"{tmp}/a.board.log"))
            self.assertEqual(out.getvalue(), f"▶ T: b\n▶ Output: tail -f {tmp}/a.log\n▶ Board: tail -f {tmp}/a.board.log\n")

    def test_each_scripts_header_shape(self):
        shapes = [  # push.py, push.py --test-only, test.py, release.py
            ("Push", ["Juneau", "master", "Fix it"], "▶ Push: Juneau · master · Fix it"),
            ("Test", ["Juneau", "master"], "▶ Test: Juneau · master"),
            ("Test", ["Juneau", "master"], "▶ Test: Juneau · master"),
            ("Release", ["Juneau", "9.9.9", "Java 17", "Maven 3.9.9"], "▶ Release: Juneau · 9.9.9 · Java 17 · Maven 3.9.9"),
            ("Push", ["Juneau", "master", "Fix it  \n\nlong body\nmore"], "▶ Push: Juneau · master · Fix it …"),   # multi-line item: first line, trimmed, then " …"
            ("Push", ["Juneau", "one\ntwo", "master"], "▶ Push: Juneau · one … · master"),
        ]
        for title, header, first in shapes:
            with self.subTest(title=title, header=header), tempfile.TemporaryDirectory() as tmp:
                reset_run_state()
                os.environ["JUNEAU_TERMINAL_CONSOLE"] = "board"     # reset_run_state drops it
                out = io.StringIO()
                with contextlib.redirect_stdout(out):
                    jr.session(title, header, sinks=jr.Sinks(full_log=f"{tmp}/a.log", board_log=f"{tmp}/a.board.log"))
                    jr._end_session()
                self.assertEqual(out.getvalue().splitlines()[:3],
                                 [first, f"▶ Output: tail -f {tmp}/a.log", f"▶ Board: tail -f {tmp}/a.board.log"])

    def test_a_long_title_wraps_with_a_hanging_indent_and_the_log_rows_do_not(self):
        with tempfile.TemporaryDirectory() as tmp:
            out = io.StringIO()
            log = f"{tmp}/{'x' * 40}.log"
            with contextlib.redirect_stdout(out), mock.patch.object(jr.shutil, "get_terminal_size",
                                                                    return_value=os.terminal_size((32, 24))):
                jr.session("Push", ["Juneau", "master", "one two three four five six seven"], sinks=jr.Sinks(full_log=log))
            self.assertEqual(out.getvalue().splitlines(),
                             ["▶ Push: Juneau · master · one", "  two three four five six seven", f"▶ Output: tail -f {log}"])

    def test_the_cancel_row_follows_the_log_rows_when_the_variable_is_set(self):
        os.environ["JUNEAU_TERMINAL_CANCEL"] = "stop-it %3"
        self.addCleanup(os.environ.pop, "JUNEAU_TERMINAL_CANCEL", None)
        with tempfile.TemporaryDirectory() as tmp:
            out = io.StringIO()
            with contextlib.redirect_stdout(out):
                jr.session("T", sinks=jr.Sinks(full_log=f"{tmp}/a.log", board_log=f"{tmp}/a.board.log"))
                jr._end_session()
            self.assertEqual(out.getvalue().splitlines()[:4], ["▶ T", f"▶ Output: tail -f {tmp}/a.log",
                                                               f"▶ Board: tail -f {tmp}/a.board.log",
                                                               "▶ Cancel: stop-it %3"])

    def test_no_cancel_row_without_the_variable(self):
        os.environ.pop("JUNEAU_TERMINAL_CANCEL", None)
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            jr.session("T")
            jr._end_session()
        self.assertNotIn("Cancel:", out.getvalue())

    def test_a_nested_session_prints_no_cancel_row(self):
        os.environ["JUNEAU_TERMINAL_SESSION"] = "1"
        os.environ["JUNEAU_TERMINAL_CANCEL"] = "stop-it %3"
        self.addCleanup(os.environ.pop, "JUNEAU_TERMINAL_CANCEL", None)
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            jr.session("T", ["b"])
            jr.done("ok")
        self.assertEqual(out.getvalue(), "")

    def test_a_default_board_session_prints_both_tails_and_writes_the_board_log(self):
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            jr.session("T", sinks=jr.Sinks("board"))
            jr.end_session()
        self.assertRegex(out.getvalue(), r"^▶ T\n▶ Output: tail -f \S+\.log\n▶ Board: tail -f \S+\.board\.log\n")
        self.assertTrue(Path(os.environ["JUNEAU_TERMINAL_BOARD_LOG"]).exists())

    def test_the_tail_command_quotes_a_path_with_spaces(self):
        with tempfile.TemporaryDirectory() as tmp:
            out = io.StringIO()
            with contextlib.redirect_stdout(out):
                jr.session("t", sinks=jr.Sinks(full_log=f"{tmp}/a b/push.log"))
            self.assertEqual(out.getvalue(), f"▶ t\n▶ Output: tail -f '{tmp}/a b/push.log'\n")

    def test_a_top_level_session_prunes_the_default_log_dir(self):
        directory = Path(os.environ["JUNEAU_TERMINAL_LOG_DIR"])
        directory.mkdir(parents=True, exist_ok=True)
        for i in range(25):
            (directory / f"old-{i:02d}.log").write_text("")
            os.utime(directory / f"old-{i:02d}.log", (1000 + i, 1000 + i))
        with contextlib.redirect_stdout(io.StringIO()):
            jr.session("t")
        self.assertEqual(len([p for p in directory.glob("*.log") if not p.name.endswith(".board.log")]), 20)

    def test_say_goes_through_the_console_once_a_session_owns_the_screen(self):
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            jr.session("t")
            jr.say("Running: mvn")          # info: hidden at actionable
            jr.say("⚠ dirty tree", level="warn")
        self.assertRegex(out.getvalue(), r"^▶ t\n▶ Output: tail -f \S+\.log\n▶ Board: tail -f \S+\.board\.log\n⚠ dirty tree\n$")

    def test_say_inside_a_paused_block_prints_as_is_once(self):
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            jr.session("t")
            with jr.paused():
                jr.say("Running: mvn")          # info: hidden at actionable, but the screen is the plain terminal now
                jr.say("⚠ dirty tree", level="warn")
            jr.say("⚠ after", level="warn")
        self.assertRegex(out.getvalue(), r"^▶ t\n▶ Output: tail -f \S+\.log\n▶ Board: tail -f \S+\.board\.log\nRunning: mvn\n⚠ dirty tree\n⚠ after\n$")

    def test_with_markers_on_stdout_stays_the_marker_stream(self):
        os.environ["TERMINAL_MARKERS"] = "1"
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            c = jr.session("🚀 t", ["master"])
            jr.say("Running: mvn")
            with jr.step("bom", 1, "BOM completeness", label="BOM"):
                pass
        self.assertIsNone(c.mode)
        self.assertFalse(jr._console_owns_screen())
        lines = out.getvalue().splitlines()
        self.assertEqual([line for line in lines if not line.startswith(jr.PREFIX)], ["Running: mvn"])
        self.assertEqual(len([line for line in lines if line.startswith(jr.PREFIX)]), 2)   # step and end


def start(step="tests", size=3, title="Tests"):
    return [("step_start", {"id": step, "n": 3, "title": title, "parent": None}),
            ("reactor", {"step": step, "size": size, "poms": [], "tests": True})]


def mod_start(name, step="tests"):
    return ("unit_start", {"step": step, "id": f"{step}/{name}", "title": f"Mod {name.upper()}"})


def cls(name, outcome="pass", step="tests"):
    return ("unit_dot", {"step": step, "id": f"{step}/{name}", "outcome": outcome})


class RowEstimateTest(unittest.TestCase):
    """The percent and `elapsed / ~total` figures a running row shows when the board has an estimator (spec 5.3)."""

    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        self.clock = Clock()
        self.est = jr.Estimate(wall_s=100.0, units=100, rate=1.0, runs=5)

    def board(self, estimator):
        b = jr.Board("actionable", nested=False, full_log=None, clock=self.clock, estimator=estimator)
        b.apply("step_start", dict(id="s", n=1, title="mvn", parent=None, tool="maven", sig=""))
        return b

    def cells(self, b):
        return jr.row_cells(b.rows["s"], jr.PLAIN_GLYPHS, self.clock())

    def test_running_row_shows_percent_and_elapsed_over_total(self):
        b = self.board(lambda step, tool, sig=None: self.est)
        b.apply("units", dict(step="s", total=100, kind="modules"))
        self.clock.advance(26)
        b.apply("unit", dict(step="s", done=26, outcome="pass"))
        cells, rest, elapsed = self.cells(b)
        self.assertIn("26/100  26%", rest)
        self.assertEqual(elapsed, "0:26 / ~1:45")

    def test_a_single_module_reactor_has_one_in_flight_cell_and_a_percent_from_tests_run(self):
        b = self.board(lambda step, tool, sig=None: jr.Estimate(wall_s=100.0, units=1, rate=1.0, runs=5, tests=4000))
        b.apply("reactor", dict(step="s", size=1, poms=[], tests=True))
        b.apply("unit_start", dict(step="s", id="s/a", title="A", src="sub"))
        b.apply("tests", dict(step="s", total=1000, fail=0, err=0, skip=0))
        self.clock.advance(10)
        cells, rest, elapsed = self.cells(b)
        self.assertEqual(cells, [jr.PLAIN_GLYPHS["run"]])
        self.assertTrue(rest.startswith("0/1  ~25%"), rest)
        self.assertIn("1,000 / ~4,000 tests", rest)

    def test_a_single_module_reactor_without_a_test_history_falls_back_to_elapsed_over_wall(self):
        b = self.board(lambda step, tool, sig=None: jr.Estimate(wall_s=100.0, units=1, rate=1.0, runs=5))
        b.apply("reactor", dict(step="s", size=1, poms=[], tests=True))
        self.clock.advance(40)
        cells, rest, elapsed = self.cells(b)
        self.assertEqual(cells, [jr.PLAIN_GLYPHS["run"]])
        self.assertEqual(rest, "0/1  ~40%")

    def test_a_running_single_module_reactor_draws_one_in_flight_cell_in_every_glyph_set(self):
        b = self.board(lambda step, tool, sig=None: jr.Estimate(wall_s=100.0, units=1, rate=1.0, runs=5))
        b.apply("reactor", dict(step="s", size=1, poms=[], tests=True))
        self.clock.advance(40)
        for glyphs in (jr.PLAIN_GLYPHS, jr.NO_COLOR_GLYPHS, jr.COLOR_GLYPHS):
            cells, rest, elapsed = jr.row_cells(b.rows["s"], glyphs, self.clock())
            self.assertEqual(cells, [glyphs["run"]])
            self.assertTrue(rest.startswith("0/1"), rest)

    def test_a_running_single_module_reactor_without_an_estimate_has_just_the_cell_and_the_tests(self):
        b = self.board(None)
        b.apply("reactor", dict(step="s", size=1, poms=[], tests=True))
        b.apply("unit_start", dict(step="s", id="s/a", title="A", src="sub"))
        b.apply("tests", dict(step="s", total=182, fail=0, err=0, skip=0))
        self.clock.advance(18)
        cells, rest, elapsed = self.cells(b)
        self.assertEqual("".join(cells), jr.PLAIN_GLYPHS["run"])
        self.assertEqual(rest, "0/1  182 tests")
        self.assertEqual(elapsed, "0:18")

    def test_a_reactor_resized_to_one_project_draws_one_in_flight_cell(self):
        b = self.board(lambda step, tool, sig=None: jr.Estimate(wall_s=100.0, units=1, rate=1.0, runs=5))
        b.apply("reactor", dict(step="s", size=4, poms=[], tests=True))
        b.apply("units", dict(step="s", total=1, kind="modules"))     # -pl: the Maven parser at its lone Building line
        self.clock.advance(40)
        cells, rest, elapsed = self.cells(b)
        self.assertEqual(cells, [jr.PLAIN_GLYPHS["run"]])
        self.assertEqual(rest, "0/1  ~40%")
        b.apply("unit", dict(step="s", done=1, outcome="pass"))     # its finish on a success
        b.apply("step_end", dict(id="s", status="ok", ms=60000, exit=0, summary=None, totals=None))
        cells, rest, elapsed = self.cells(b)
        self.assertEqual("".join(cells), jr.PLAIN_GLYPHS["pass"])
        self.assertIn("1/1", rest)

    def test_a_single_module_reactor_draws_its_cell_once_the_module_ends(self):
        b = self.board(lambda step, tool, sig=None: self.est)
        b.apply("reactor", dict(step="s", size=1, poms=[], tests=True))
        b.apply("unit_start", dict(step="s", id="s/a", title="A", src="sub"))
        b.apply("unit_dot", dict(step="s", id="s/a", outcome="fail", src="sub"))
        b.apply("unit_end", dict(step="s", id="s/a", status="fail", ms=1, totals=None, src="sub"))
        cells, rest, elapsed = self.cells(b)
        self.assertEqual("".join(cells), jr.PLAIN_GLYPHS["fail"])
        self.assertIn("1/1", rest)

    def test_a_single_module_reactor_counted_at_the_end_finishes_with_a_full_bar(self):
        b = self.board(lambda step, tool, sig=None: self.est)
        b.apply("reactor", dict(step="s", size=1, poms=[], tests=True))
        b.apply("unit", dict(step="s", done=1, outcome="pass"))     # the Maven parser's finish on a success
        b.apply("step_end", dict(id="s", status="ok", ms=26000, exit=0, summary=None, totals=None))
        cells, rest, elapsed = self.cells(b)
        self.assertEqual("".join(cells), jr.PLAIN_GLYPHS["pass"])
        self.assertIn("1/1", rest)

    def test_a_single_module_reactor_whose_module_ran_no_tests_finishes_one_of_one(self):
        b = self.board(lambda step, tool, sig=None: self.est)
        b.apply("reactor", dict(step="s", size=1, poms=[], tests=True))
        b.apply("module_tested", dict(step="s", module="s/a"))     # no tests ran: it drops out of the bar
        b.apply("units", dict(step="s", total=1, kind="modules"))  # the Maven parser's finish on a success
        b.apply("unit", dict(step="s", done=1, outcome="pass"))
        b.apply("step_end", dict(id="s", status="ok", ms=26000, exit=0, summary=None, totals=None))
        cells, rest, elapsed = self.cells(b)
        self.assertEqual((b.rows["s"].units_total, b.rows["s"].units_done), (1, 1))
        self.assertEqual("".join(cells), jr.PLAIN_GLYPHS["pass"])

    def test_finished_row_shows_elapsed_only(self):
        b = self.board(lambda step, tool, sig=None: self.est)
        b.apply("units", dict(step="s", total=100, kind="modules"))
        self.clock.advance(26)
        b.apply("unit", dict(step="s", done=100, outcome="pass"))
        b.apply("step_end", dict(id="s", status="ok", ms=26000, exit=None, summary=None, totals=None))
        cells, rest, elapsed = self.cells(b)
        self.assertEqual(elapsed, "0:26")
        self.assertNotIn("%", rest)

    def test_percent_hidden_until_one_second(self):
        b = self.board(lambda step, tool, sig=None: jr.Estimate(wall_s=100.0, units=None, rate=None, runs=5))
        self.clock.advance(0.5)
        cells, rest, elapsed = self.cells(b)
        self.assertEqual((rest, elapsed), ("", ""))
        self.clock.advance(0.6)
        self.assertEqual(self.cells(b)[1], "~1%")

    def test_narrow_row_falls_back_to_elapsed_only(self):
        b = self.board(lambda step, tool, sig=None: self.est)
        b.apply("units", dict(step="s", total=100, kind="modules"))
        self.clock.advance(26)
        b.apply("unit", dict(step="s", done=26, outcome="pass"))
        cells, rest, elapsed = self.cells(b)
        label = b.rows["s"].label.ljust(b.width())
        wide = jr._fit_row(label, cells, rest, elapsed, 80)
        self.assertTrue(wide.endswith("0:26 / ~1:45"))
        narrow = jr._fit_row(label, cells, rest, elapsed, len(label) + len(rest) + 8)
        self.assertTrue(narrow.endswith("0:26"), narrow)
        self.assertNotIn("~1:45", narrow)

    def test_marked_when_history_driven(self):
        b = self.board(lambda step, tool, sig=None: jr.Estimate(wall_s=100.0, units=None, rate=None, runs=5))
        self.clock.advance(63)
        cells, rest, elapsed = self.cells(b)
        self.assertEqual(rest, "~63%")
        self.assertEqual(elapsed, "1:03 / ~1:45")

    def test_no_estimator_matches_today(self):
        for estimator in (None, lambda step, tool, sig=None: None):
            b = self.board(estimator)
            b.apply("units", dict(step="s", total=100, kind="modules"))
            self.clock.advance(26)
            b.apply("unit", dict(step="s", done=26, outcome="pass"))
            cells, rest, elapsed = self.cells(b)
            self.assertEqual((rest, elapsed), ("26/100", "0:26"))
            self.clock.t = 100.0

    def test_estimator_is_asked_once_per_row_and_its_failure_is_swallowed(self):
        asked = []
        b = self.board(lambda step, tool, sig=None: asked.append((step, tool)))
        self.assertEqual(asked, [("s", "maven")])
        asked.clear()
        b = jr.Board("actionable", nested=False, full_log=None, clock=self.clock,
                     estimator=lambda step, tool, sig=None: asked.append((step, tool, sig)))
        b.apply("step_start", dict(id="s", n=1, title="mvn", parent=None, tool="maven", sig="abc"))
        self.assertEqual(asked, [("s", "maven", "abc")])
        b = self.board(lambda step, tool, sig=None: 1 / 0)
        self.assertIsNone(b.rows["s"].estimate)

    def test_a_console_gets_an_estimator_that_reads_the_history(self):
        with tempfile.TemporaryDirectory() as hist_dir, tempfile.TemporaryDirectory() as repo:
            subprocess.run(["git", "init", "-q", repo], check=True)
            env = {"JUNEAU_TERMINAL_HISTORY": "1", "JUNEAU_TERMINAL_HISTORY_DIR": hist_dir, "JUNEAU_TERMINAL_CONSOLE": "none"}
            with mock.patch.dict(os.environ, env), mock.patch.object(jr.sys, "argv", ["/x/scripts/push.py"]):
                os.environ.pop("TERMINAL_MARKERS", None)
                cwd = os.getcwd()
                os.chdir(repo)
                self.addCleanup(os.chdir, cwd)
                store = jr.HistoryStore(repo, "push")
                for _ in range(3):
                    store.append({"v": 1, "step": "mvn", "tool": "maven", "ok": True, "complete": True, "wall_s": 100.0,
                                  "sig": "", "units": {"kind": "modules", "total": 10, "done": 10}})
                c = jr.Console(jr.Sinks(console="none"))
                self.assertIsNotNone(c.board.estimator)
                c.board.apply("step_start", dict(id="mvn", n=1, title="mvn", parent=None, tool="maven", sig=""))
                self.assertEqual(c.board.rows["mvn"].estimate.wall_s, 100.0)
                c.board.apply("step_start", dict(id="other", n=2, title="o", parent=None, tool=None, sig=""))
                self.assertIsNone(c.board.rows["other"].estimate)
                c.close()


class FileBarTest(unittest.TestCase):
    """A file bar (pytest's per-file counts, Playwright's reporter): a cell in flight per running file, and the percent
    and `~total` from the tests run against history's expected count (spec 4.3, 4.4)."""

    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        self.clock = Clock()

    def board(self, est=jr.Estimate(wall_s=100.0, units=32, rate=100 / 32, runs=5, tests=4000), kind="files", total=32):
        b = jr.Board("actionable", nested=False, full_log=None, clock=self.clock,
                     estimator=None if est is None else lambda step, tool, sig=None: est)
        b.apply("step_start", dict(id="s", n=1, title="Tests", parent=None, tool="pytest", sig=""))
        b.apply("units", dict(step="s", total=total, kind=kind))
        return b

    def rest(self, b):
        return jr.row_cells(b.rows["s"], jr.PLAIN_GLYPHS, self.clock())[1:]

    def run_files(self, b, done, tests):
        self.clock.advance(50)
        b.apply("unit", dict(step="s", done=done, outcome="pass"))
        b.apply("tests", dict(step="s", total=tests, fail=0, err=0, skip=0))

    def test_the_count_is_files_and_the_percent_and_total_come_from_the_tests_run(self):
        b = self.board()
        self.run_files(b, 7, 1000)
        rest, elapsed = self.rest(b)
        self.assertTrue(rest.startswith("7/32  ~25%"), rest)    # 7 of 32 files is 21%: the tests say 25%
        self.assertIn("1,000 / ~4,000 tests", rest)
        self.assertEqual(elapsed, "0:50 / ~2:30")

    def test_the_total_follows_the_tests_not_the_files(self):
        b = self.board()
        self.run_files(b, 7, 3000)
        rest, elapsed = self.rest(b)
        self.assertTrue(rest.startswith("7/32  ~75%"), rest)
        self.assertEqual(elapsed, "0:50 / ~1:15")

    def test_past_the_expected_test_count_the_percent_holds_at_99_rather_than_drop_to_the_files(self):
        b = self.board()
        self.run_files(b, 7, 4100)                  # 7 of 32 files is 21%
        self.assertTrue(self.rest(b)[0].startswith("7/32  ~99%"), self.rest(b)[0])

    def test_past_the_expected_test_count_with_every_file_in_the_percent_is_99_until_the_step_ends(self):
        b = self.board()
        self.run_files(b, 32, 4100)
        self.assertTrue(self.rest(b)[0].startswith("32/32  ~99%"), self.rest(b)[0])
        b.apply("step_end", dict(id="s", status="ok", ms=50000, exit=0, summary=None, totals=None))
        self.assertNotIn("99%", self.rest(b)[0])

    def test_without_an_expected_test_count_the_percent_is_from_files(self):
        b = self.board(jr.Estimate(wall_s=100.0, units=32, rate=100 / 32, runs=5))
        self.run_files(b, 7, 1000)
        self.assertTrue(self.rest(b)[0].startswith("7/32  21%"), self.rest(b)[0])

    def test_before_any_test_reports_the_percent_is_from_files(self):
        b = self.board()
        self.clock.advance(50)
        b.apply("unit", dict(step="s", done=7, outcome="pass"))
        self.assertTrue(self.rest(b)[0].startswith("7/32  21%"), self.rest(b)[0])

    def test_a_tests_bar_keeps_its_own_figures(self):
        b = self.board(jr.Estimate(wall_s=100.0, units=4000, rate=100 / 4000, runs=5, tests=4000), kind="tests",
                       total=4000)
        self.run_files(b, 1000, 1000)
        self.assertTrue(self.rest(b)[0].startswith("1000/4000  25%"), self.rest(b)[0])

    def test_a_module_bar_follows_the_tests_run_when_history_has_no_sub_times(self):
        b = self.board(kind="modules")
        self.run_files(b, 7, 1000)
        self.assertTrue(self.rest(b)[0].startswith("7/32  ~25%"), self.rest(b)[0])

    def start(self, b, *names):
        for name in names:
            b.apply("unit_start", dict(step="s", id=name, title=name, src="file"))

    def end(self, b, name):
        b.apply("unit_end", dict(step="s", id=name, status="ok", src="file"))

    def test_each_running_file_has_a_cell_in_flight_after_the_finished_ones(self):
        b = self.board(None, total=5)
        self.start(b, "a", "b", "c", "d")
        self.end(b, "a")
        b.apply("unit", dict(step="s", done=1, outcome="pass"))       # a's cell comes as its line closes
        self.assertEqual(bytes(b.rows["s"].unit_view()), bytes([jr.UNIT_PASS] + [jr.UNIT_RUN] * 3 + [0]))

    def test_in_flight_cells_stop_at_the_total(self):
        b = self.board(None, total=3)
        self.start(b, "a", "b", "c", "d")
        self.end(b, "a")
        b.apply("unit", dict(step="s", done=1, outcome="fail"))
        self.assertEqual(bytes(b.rows["s"].unit_view()), bytes([jr.UNIT_FAIL, jr.UNIT_RUN, jr.UNIT_RUN]))

    def test_a_tests_bar_has_no_cells_in_flight(self):
        b = self.board(None, kind="tests", total=4)
        self.start(b, "a", "b")
        self.assertEqual(bytes(b.rows["s"].unit_view()), bytes(4))

    def test_a_finished_file_bar_has_no_cells_in_flight(self):
        b = self.board(None, total=2)
        self.start(b, "a")
        b.apply("step_end", dict(id="s", status="fail", ms=1, exit=1, summary=None, totals=None))
        self.assertEqual(bytes(b.rows["s"].unit_view()), bytes(2))


class LiveTest(unittest.TestCase):
    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        self.clock = Clock()
        self.dims = (60, 10)
        self.screen = Screen()
        self.live = jr.LiveRenderer(self.screen.feed, size=lambda: os.terminal_size(self.dims), clock=self.clock,
                                    color=False)
        self.board = jr.Board(clock=self.clock)

    def push(self, *events):
        self.clock.advance(1)
        self.live.render(self.board, feed(self.board, *events))

    def spin(self):
        return jr.icon("running", self.clock()) + " "

    def session(self, title="Push", header=("Juneau", "master"), lines=("Commit Message: m",), more=()):
        self.push(("session", {"title": title, "header": list(header), "lines": list(lines),
                               "header_more": list(more), "nested": False}))

    def test_finished_rows_stay_in_the_frame_under_the_header(self):
        self.screen = Screen(rows=10, cols=60)
        self.live.write = self.screen.feed
        self.session()
        self.push(*start())
        self.push(("step_end", {"id": "tests", "status": "ok", "ms": 4000, "exit": None, "summary": "done",
                                "totals": None}))
        self.assertEqual(self.screen.lines[:2], ["▶ Push: Juneau · master", "▶ Commit Message: m"])
        self.assertTrue(self.screen.lines[2].startswith("✔ " + "Tests".ljust(10) + "  done"), self.screen.lines)
        self.assertEqual(self.screen.scrollback, [])            # nothing reached scrollback yet
        self.assertEqual(self.live.height, 3)

    def test_a_board_taller_than_the_pane_shows_its_last_rows_and_merges_none(self):
        self.dims = (60, 8)
        self.screen = Screen(rows=8, cols=60)
        self.live.write = self.screen.feed
        self.session()
        for n in "abcdefgh":
            self.push(("step_start", {"id": n, "n": 1, "title": f"Step {n}", "parent": None}),
                      ("step_end", {"id": n, "status": "ok", "ms": 1, "exit": None, "summary": None, "totals": None}))
        self.push(("step_start", {"id": "z", "n": 1, "title": "Last", "parent": None}))
        shown = [line for line in self.screen.lines if line.strip()]
        self.assertEqual([line.split()[:2] for line in shown],
                         [["✔", f"Step {n}".split()[0]] for n in "defgh"] + [[self.spin().strip(), "Last"]])
        self.assertEqual([line.split()[2] for line in shown[:-1]], list("defgh"))   # the top rows scrolled out of view
        self.assertEqual(self.screen.scrollback, [])            # out of view, not printed: nothing reached scrollback
        self.assertFalse(self.screen.clamped)

    def test_growing_the_pane_does_not_leave_a_line_pulled_back_from_scrollback_above_the_header(self):
        self.dims = (60, 14)
        self.screen = Screen(rows=14, cols=60)
        self.live.write = self.screen.feed
        self.session(lines=("Commit Message: m", "Output: tail -f /x/log", "Board: tail -f /x/c.log"))
        for n in "ab":
            self.push(("step_start", {"id": n, "n": 1, "title": f"Step {n}", "parent": None}),
                      ("step_end", {"id": n, "status": "ok", "ms": 1, "exit": None, "summary": "done", "totals": None}))
        self.push(("step_start", {"id": "z", "n": 1, "title": "Last", "parent": None}))
        for rows in (7, 14):        # the pane shrinks, a redraw shows the frame's last rows, it grows back, all fit
            self.dims = (60, rows)
            self.screen.resize_like_tmux(rows)
            self.push()
        shown = [line for line in self.screen.lines if line.strip()]
        self.assertEqual(shown[0], "▶ Push: Juneau · master", self.screen.lines)
        self.assertEqual(sum(line.startswith("▶ Push:") for line in shown), 1, self.screen.lines)
        self.assertTrue(shown[-1].startswith(self.spin() + "Last"), shown)
        self.assertEqual([line for line in shown if line.startswith("✔")],
                         ["✔ Step a      done", "✔ Step b      done"], shown)

    def push_like_session(self, cols, rows, shell=False):
        """A tmux pane of cols x rows showing a push's frame: a long Commit Message, the log lines, four finished rows
        with an elapsed time and a running one.  shell puts a prompt line above, where the run starts."""
        self.dims = (cols, rows)
        self.screen = Screen(rows=rows, cols=cols)
        if shell:
            self.screen.feed("$ push\n")
        self.live = jr.LiveRenderer(self.screen.feed, size=lambda: os.terminal_size(self.dims), clock=self.clock,
                                    color=False)
        self.board = jr.Board(clock=self.clock)
        self.session(header=("SSC", "future"), lines=(
            "Commit Message: Add a doc next to every script, with a checker test and argparse help for each one",
            "Output: tail -f /Users/someone/.cache/juneau-terminal/push-1.log",
            "Board: tail -f /Users/someone/.cache/juneau-terminal/push-1.board.log"))
        for n, title in (("a", "Prechecks"), ("b", "Validate"), ("c", "Rebase"), ("d", "Compile")):
            self.push(("step_start", {"id": n, "n": 1, "title": title, "parent": None}),
                      ("step_end", {"id": n, "status": "ok", "ms": 1500, "exit": None, "summary": None,
                                    "totals": None}))
        self.push(("step_start", {"id": "z", "n": 1, "title": "Last", "parent": None}))

    def assert_one_frame_at(self, cols):
        """The screen holds exactly one frame, laid out at cols: one header on top, every finished row with its elapsed
        time flush with the frame's right edge (cols - 1) and no ellipsis left over from another width.  A shell line
        above the frame may still be there."""
        shown = [line.rstrip() for line in self.screen.lines if line.strip()]
        shown = shown[1:] if shown[0] == "$ push" else shown
        self.assertEqual(shown[0], "▶ Push: SSC · future", self.screen.lines)
        self.assertEqual(sum(line.startswith("▶ Push:") for line in shown), 1, self.screen.lines)
        self.assertEqual(sum(line.startswith("▶ Commit Message:") for line in shown), 1, self.screen.lines)
        done = [line for line in shown if line.startswith("✔")]
        self.assertEqual([line.split()[1] for line in done], ["Prechecks", "Validate", "Rebase", "Compile"], shown)
        for line in done:
            self.assertTrue(line.endswith("  0:01") and jr._vlen(line) == cols - 1, (cols, line))
        self.assertTrue(shown[-1].startswith(self.spin() + "Last"), shown)

    def test_any_resize_like_tmux_leaves_one_frame_laid_out_at_the_new_width(self):
        # Each step is (cols, rows) for one tmux resize, or a list of them for resizes that land between two redraws.
        sequences = {
            "shrink": [(45, 14)],
            "shrink then grow": [(45, 14), (60, 14)],
            "shrink then grow part way": [(40, 14), (52, 14)],
            "grow then shrink": [(80, 14), (45, 14)],
            "repeated": [(45, 14), (70, 14), (50, 14), (80, 14), (40, 14), (60, 14)],
            "with the height": [(45, 12), (70, 16), (50, 13), (60, 12)],
            "two resizes per redraw": [[(45, 14), (70, 14)], [(40, 12), (55, 14)]],
            "taller and back between redraws": [(45, 14), [(45, 18), (45, 14)]],
            "wider and shorter": [(45, 14), (60, 12)],
        }
        for shell in (False, True):
            for name, steps in sequences.items():
                with self.subTest(name, shell=shell):
                    self.push_like_session(60, 14, shell)
                    self.assert_one_frame_at(60)
                    for step in steps:
                        for cols, rows in (step if isinstance(step, list) else [step]):
                            self.screen.resize_like_tmux(rows, cols)
                        self.dims = (cols, rows)
                        self.push()
                        self.assert_one_frame_at(cols)

    def test_a_taller_pane_that_shrinks_back_between_two_redraws_leaves_no_stale_lines_above_the_header(self):
        self.push_like_session(60, 14)
        self.dims = (45, 14)
        self.screen.resize_like_tmux(cols=45)       # the frame's rewrapped top goes into scrollback
        self.push()
        self.screen.resize_like_tmux(rows=18)       # pulled back above the frame ...
        self.screen.resize_like_tmux(rows=14)       # ... and kept there: the blank rows go instead; no size change seen
        self.push()
        self.assert_one_frame_at(45)

    def test_closing_after_a_resize_prints_the_record_from_the_top_and_keeps_no_frame_in_scrollback(self):
        self.push_like_session(60, 14)
        self.push(("step_end", {"id": "z", "status": "ok", "ms": 1500, "exit": None, "summary": None, "totals": None}))
        self.dims = (45, 14)
        self.screen.resize_like_tmux(cols=45)
        self.push()
        back = list(self.screen.scrollback)         # the rewrapped top of the old frame, out of reach
        self.live.close()
        self.assertEqual(self.screen.scrollback, back)
        self.assertEqual(self.screen.lines[0], "▶ Push: SSC · future")
        self.assertEqual(sum(line.startswith("✔ Last") for line in self.screen.lines), 1, self.screen.lines)

    def test_closing_a_frame_at_the_top_of_a_tmux_pane_keeps_no_copy_of_it_in_scrollback(self):
        self.push_like_session(60, 14)
        self.live.close()
        self.assertEqual(self.screen.scrollback, [])
        self.assertEqual(sum(line.startswith("▶ Push:") for line in self.screen.lines), 1, self.screen.lines)

    def test_widening_after_a_narrowing_overwrites_the_frame_top_tmux_pulls_back_above_the_header(self):
        self.push_like_session(60, 14)
        self.dims = (45, 14)
        self.screen.resize_like_tmux(cols=45)       # the reflowed frame top goes into scrollback, out of reach
        self.push()
        self.assertIn("▶ Push: SSC · future", self.screen.scrollback)
        self.dims = (60, 14)
        self.screen.resize_like_tmux(cols=60)       # ... and comes back onto the screen above the frame
        self.push()
        shown = [line for line in self.screen.lines if line.strip()]
        self.assertEqual(sum(line.startswith("▶ Push:") for line in shown), 1, self.screen.lines)
        self.assertEqual(shown[0], "▶ Push: SSC · future", self.screen.lines)

    def test_a_finished_row_is_laid_out_again_at_the_new_width(self):
        self.push_like_session(60, 14)
        rebase = [line for line in self.screen.lines if line.startswith("✔ Rebase")]
        self.assertEqual(rebase, ["✔ Rebase".ljust(59 - 4) + "0:01"])
        for cols in (45, 70):       # narrower: no ellipsis where the elapsed time was; wider: no short alignment
            self.dims = (cols, 14)
            self.screen.resize_like_tmux(cols=cols)
            self.push()
            rebase = [line.rstrip() for line in self.screen.lines if line.startswith("✔ Rebase")]
            self.assertEqual(rebase, ["✔ Rebase".ljust(cols - 1 - 4) + "0:01"], cols)

    def test_a_transcript_line_wider_than_the_pane_is_cut_with_an_ellipsis(self):
        self.live.render(self.board, [("text", "x" * 200)])
        self.assertTrue(all(jr._vlen(line) <= 59 for line in self.screen.lines), self.screen.lines)
        self.assertIn("…", self.screen.text())

    def test_an_identical_frame_is_not_rewritten(self):
        writes = []
        live = jr.LiveRenderer(writes.append, size=lambda: os.terminal_size(self.dims), clock=self.clock, color=False)
        live.render(self.board, [("text", "hello")])
        drawn = len(writes)
        self.clock.advance(1)
        live.tick(self.board)                                   # no running row: nothing on screen moves
        self.assertEqual(len(writes), drawn)

    def test_a_pane_too_short_for_one_line_draws_nothing(self):
        self.dims = (60, 2)
        self.push(*start())
        self.assertEqual(self.screen.text(), "")
        self.dims = (60, 10)
        self.clock.advance(1)
        self.live.tick(self.board)
        self.assertTrue(self.screen.text().startswith(self.spin() + "Tests"))

    def test_a_tick_does_not_re_render_the_transcript(self):
        for n in range(200):
            self.push(("step_start", {"id": f"s{n}", "n": 1, "title": f"Step {n}", "parent": None}),
                      ("step_end", {"id": f"s{n}", "status": "ok", "ms": 1, "exit": None, "summary": None,
                                    "totals": None}))
        self.push(*start())
        calls = []
        real = self.live._block_lines
        self.live._block_lines = lambda *a, **k: calls.append(a) or real(*a, **k)
        for _ in range(10):
            self.clock.advance(0.1)
            self.live.tick(self.board)
        self.assertEqual(calls, [])                 # transcript lines are rendered once, cut once per width
        self.dims = (50, 10)
        self.clock.advance(0.1)
        self.live.tick(self.board)
        self.assertEqual(calls, [])                 # a new width lays out the parts read on arrival; no re-render

    def test_last_draw_moves_only_when_a_frame_is_written(self):
        self.push(*start())
        t0 = self.clock()
        self.assertEqual(self.live.last_draw, t0)
        self.clock.advance(0.06)
        self.live.tick(self.board)                  # throttled
        self.assertEqual(self.live.last_draw, t0)
        self.live.last_frame = None                 # force the next frame to differ
        self.clock.advance(0.06)
        self.live.tick(self.board)                  # 0.12 s after the last frame written: draws
        self.assertAlmostEqual(self.live.last_draw, t0 + 0.12)
        self.clock.advance(0.2)
        cols, rows = self.dims
        self.live.last_frame = self.live.frame(cols - 1, cols, rows - 2)    # the frame this tick would draw
        self.live.tick(self.board)                  # identical frame: nothing written, last_draw stays
        self.assertAlmostEqual(self.live.last_draw, t0 + 0.12)

    def test_finished_rows_keep_the_label_column_they_finished_with(self):
        self.push(("step_start", {"id": "a", "n": 1, "title": "Alpha", "parent": None}),
                  ("step_end", {"id": "a", "status": "ok", "ms": 1, "exit": None, "summary": "done", "totals": None}))
        before = self.screen.text().splitlines()[0]
        self.push(("step_start", {"id": "b", "n": 1, "title": "A much longer label", "parent": None}))
        self.assertEqual(self.screen.text().splitlines()[0], before)     # a longer running label does not shift it

    def test_a_running_row_and_its_module_redraw_in_place(self):
        self.push(*start(), mod_start("a"))
        self.push(cls("a"), cls("a", "fail"))
        self.assertEqual(self.screen.text(), "\n".join([
            (self.spin() + "Tests".ljust(10) + "  ▒░░  0/3").ljust(59 - 4) + "0:01",     # Mod A is in flight
            "    Mod A  •F".ljust(59 - 4) + "0:01",
        ]))
        self.push(("unit_end", {"step": "tests", "id": "tests/a", "status": "fail", "ms": 1000,
                                  "totals": (2, 1, 0, 0)}))
        self.push(("step_end", {"id": "tests", "status": "fail", "ms": 4000, "exit": 1, "summary": None,
                                "totals": None}))
        self.assertEqual(self.screen.text(), "\n".join([
            # no module line at actionable: the row names the failed module; a unit bar keeps its pending cells
            ("✗ " + "Tests".ljust(10) + "  F░░  1/3  1 failed: Mod A").ljust(59 - 4) + "0:04",
        ]))
        self.assertFalse(self.screen.clamped)

    def test_a_running_row_shows_its_progress_and_step_end_clears_it(self):
        self.push(*start(size=0))
        self.push(("step_progress", {"step": "tests", "text": "52% \u00b7 test_foo.py"}))
        self.assertEqual(self.screen.text(),
                         (self.spin() + "Tests".ljust(10) + "  52% \u00b7 test_foo.py").ljust(59 - 4) + "0:01")
        self.push(("step_progress", {"step": "tests", "text": "60% \u00b7 test_foo.py"}))
        self.assertIn("60% \u00b7 test_foo.py", self.screen.text())
        self.assertNotIn("52%", self.screen.text())
        self.push(("step_end", {"id": "tests", "status": "ok", "ms": 4000, "exit": None, "summary": "3 tests",
                                "totals": None}))
        self.assertNotIn("60%", self.screen.text())
        self.assertIsNone(self.board.rows["tests"].progress)

    def test_progress_follows_the_row_totals_and_never_shows_on_a_finished_row(self):
        # a row that never received units (a Maven row's reactor event announces them, which hides scraped progress)
        self.push(("step_start", {"id": "tests", "n": 3, "title": "Tests", "parent": None}),
                  ("tests", {"step": "tests", "total": 5, "fail": 0, "err": 0, "skip": 0}),
                  ("step_progress", {"step": "tests", "text": "7/9"}))
        row = self.live._row(self.board, self.board.rows["tests"], 59, pending=True)
        self.assertIn("5 tests \u00b7 7/9", row)
        self.board.rows["tests"].status, self.board.rows["tests"].ms = "ok", 1000
        self.assertNotIn("7/9", self.live._row(self.board, self.board.rows["tests"], 59))

    def unit_row(self, **units):
        self.board.apply("step_start", dict(id="s", n=1, title="Tests", parent=None))
        if units:
            self.board.apply("units", dict(step="s", **units))
        return self.board.rows["s"]

    def test_unit_row_shows_bar_and_count(self):
        row = self.unit_row(total=4, kind="tests")
        self.board.apply("unit", dict(step="s", done=2, outcome="pass"))
        cells, rest, _ = jr.row_cells(row, jr.PLAIN_GLYPHS, self.clock())
        self.assertEqual(cells, ["#", "#", jr.PENDING, jr.PENDING])
        self.assertTrue(rest.startswith("2/4"))

    def test_scraped_progress_text_hidden_once_units_known(self):
        row = self.unit_row()
        self.board.apply("step_progress", dict(step="s", text="43% \u00b7 test_f.py"))
        self.assertIn("43%", self.live._row(self.board, row, 120))              # no units yet: shown as today
        self.board.apply("units", dict(step="s", total=10, kind="tests"))
        self.assertNotIn("43% \u00b7 test_f.py", self.live._row(self.board, row, 120))

    def wrapped(self, total, cols, fail_at=None, glyphs=jr.PLAIN_GLYPHS):
        row = self.unit_row(total=total, kind="tests")
        for i in range(total):
            self.board.apply("unit", dict(step="s", done=i + 1, outcome="fail" if i == fail_at else "pass"))
        self.live.glyphs = glyphs
        return self.live._row(self.board, row, cols).split("\n")

    def test_a_unit_bar_has_one_cell_per_unit_and_wraps_to_the_bar_column(self):
        lines = self.wrapped(84, 79)
        self.assertGreater(len(lines), 1)
        self.assertTrue(all(len(line) <= 79 for line in lines), lines)
        self.assertEqual(sum(line.count("#") for line in lines), 84)
        indent = len("⠋ " + "Tests".ljust(self.board.width())) + 2
        self.assertTrue(all(line.startswith(" " * indent + "#") for line in lines[1:]), lines)
        self.assertEqual(len(lines[0]) - indent, 79 - indent)                   # a full row of cells on the first line
        self.assertIn("84/84", lines[-1])                                       # the count follows the last bar row
        self.assertTrue(all("84/84" not in line for line in lines[:-1]))

    def test_a_bar_that_fits_exactly_does_not_wrap(self):
        cells = ["#"] * 5
        self.assertEqual(jr._fit_row("L", cells, "r", "", len("L  #####  r"), wrap=True), "L  #####  r")
        self.assertEqual(jr._fit_row("L", cells, "r", "", len("L  #####  r") - 1, wrap=True), "L  #####\n   r")

    def test_a_wrapped_bar_puts_its_text_after_the_leftover_cells_on_the_last_row(self):
        cols = 40                               # label "L" + 2 = bar column 3, so every bar row holds 37 cells
        lines = jr._fit_row("L", ["#"] * 50, "r", "0:05", cols, wrap=True).split("\n")
        self.assertEqual(lines, ["L  " + "#" * 37, "   " + "#" * 13 + "  r" + " " * (cols - 3 - 13 - 3 - 4) + "0:05"])

    def test_every_bar_row_but_the_last_holds_a_full_width_of_cells(self):
        cols, indent = 40, 3
        cells = [str(i % 10) for i in range(2 * (cols - indent) + 5)]
        lines = jr._fit_row("L", cells, "r", "", cols, wrap=True).split("\n")
        self.assertEqual([len(line) - indent for line in lines[:-1]], [cols - indent] * (len(lines) - 1))
        self.assertTrue(all(len(line) <= cols for line in lines), lines)
        self.assertEqual("".join(line[indent:indent + cols - indent].rstrip() if i < len(lines) - 1 else line[indent:indent + 5]
                                 for i, line in enumerate(lines)), "".join(cells))      # nothing lost or repeated
        self.assertEqual(lines[-1], " " * indent + "".join(cells[-5:]) + "  r")
        self.assertEqual(len(lines), 3)

    def test_text_goes_on_its_own_line_when_the_last_row_is_full(self):
        cols = 40
        lines = jr._fit_row("L", ["#"] * 74, "r", "0:05", cols, wrap=True).split("\n")
        self.assertEqual(lines, ["L  " + "#" * 37, "   " + "#" * 37, "   r" + " " * (cols - 4 - 4) + "0:05"])

    def test_text_goes_on_its_own_line_when_the_leftover_cells_leave_no_room_for_it(self):
        cols = 40                               # 30 leftover cells + "  r" + "  0:05" is 41 wide
        lines = jr._fit_row("L", ["#"] * 67, "r", "0:05", cols, wrap=True).split("\n")
        self.assertEqual([line.count("#") for line in lines], [37, 30, 0])
        self.assertEqual(lines[2], "   r" + " " * (cols - 4 - 4) + "0:05")

    def test_long_trailing_text_wraps_the_bar_instead_of_cutting_it(self):
        rest = "60/86  12,345 tests, 0 failed · 4 modules · ~45%"   # 50 wide: the old fit cut the bar
        lines = jr._fit_row("Java Tests   ", ["#"] * 86, rest, "1:09", 72, wrap=True).split("\n")
        self.assertEqual(sum(line.count("#") for line in lines), 86)
        self.assertEqual(len(lines), 3)
        self.assertEqual(lines[2], " " * 15 + rest + " " * (72 - 15 - len(rest) - 4) + "1:09")

    def test_trailing_text_too_long_for_its_own_line_is_cut_with_an_ellipsis(self):
        lines = jr._fit_row("L", ["#"] * 50, "r" * 100, "1:09", 40, wrap=True).split("\n")
        self.assertEqual(sum(line.count("#") for line in lines), 50)
        self.assertEqual(len(lines), 3)
        self.assertTrue(lines[2].startswith("   rrr") and "…" in lines[2] and lines[2].endswith("  1:09"), lines[2])
        self.assertEqual(jr._vlen(lines[2]), 40)

    def test_the_timer_shortens_on_the_line_that_carries_the_text(self):
        elapsed = "0:05 / ~0:09"
        cols = 24                               # bar column 3, 21 cells per row
        last = jr._fit_row("L", ["#"] * 30, "r", elapsed, cols, wrap=True).split("\n")[-1]
        self.assertEqual(last, "   " + "#" * 9 + "  r" + " " * (cols - 3 - 9 - 3 - 4) + "0:05")      # 12 wide would not fit
        wide = jr._fit_row("L", ["#"] * 30, "r", elapsed, 40, wrap=True).split("\n")[-1]
        self.assertTrue(wide.endswith(elapsed), wide)           # room enough: the full timer stays
        own = jr._fit_row("L", ["#"] * 14, "r", elapsed, 17, wrap=True).split("\n")       # a full row: the text line
        self.assertEqual(own, ["L  " + "#" * 14, "   r" + " " * (17 - 4 - 4) + "0:05"])

    def test_no_line_of_a_wrapped_bar_is_wider_than_cols(self):
        for cols in range(12, 90):
            for n in (1, 5, 30, 86, 200):
                for elapsed in ("", "1:23", "1:23 / ~1:30"):
                    line = jr._fit_row("Compile", ["#"] * n, "86/86  100%", elapsed, cols, wrap=True)
                    for part in line.split("\n"):
                        self.assertLessEqual(jr._vlen(part), cols, (cols, n, elapsed, part))

    def test_one_unit_is_one_cell(self):
        lines = self.wrapped(1, 79)
        self.assertEqual(len(lines), 1)
        self.assertEqual(lines[0].count("#"), 1)

    def test_a_failure_on_a_continuation_line_keeps_its_cell(self):
        lines = self.wrapped(84, 79, fail_at=83)
        self.assertGreater(len(lines), 1)
        self.assertEqual(lines[-1].split()[0][-1], jr.PLAIN_GLYPHS["fail"])   # the last bar row ends on it
        self.assertEqual(sum(line.count("F") for line in lines), 1)

    def test_a_non_wrapping_bar_still_gives_way_with_an_ellipsis(self):
        line = jr._fit_row("L", ["x"] * 61, "r", "", 8)
        self.assertNotIn("\n", line)
        self.assertLessEqual(len(line), 8)

    def test_a_wrapped_bar_redraws_in_place_counting_its_extra_lines(self):
        self.dims = (80, 10)
        self.push(("step_start", {"id": "s", "n": 1, "title": "Tests", "parent": None}),
                  ("units", {"step": "s", "total": 84, "kind": "tests"}))
        first = self.screen.text().splitlines()
        self.assertEqual((len(first), self.live.height), (2, 2))
        for i in range(84):
            self.push(("unit", {"step": "s", "done": i + 1, "outcome": "pass"}))
        self.assertEqual(len(self.screen.text().splitlines()), 2)             # no stale fragment from earlier draws
        self.assertEqual(self.screen.text().count("\u2588"), 84)
        self.assertFalse(self.screen.clamped)
        self.push(("step_end", {"id": "s", "status": "ok", "ms": 5000, "exit": None, "summary": None,
                                "totals": None}))
        lines = self.screen.text().splitlines()                               # the finished row stays in the frame, wrapped
        self.assertEqual((len(lines), self.live.height), (2, 2))
        self.assertEqual(self.screen.text().count("\u2588"), 84)
        self.live.close()                                                     # close prints it into scrollback
        self.assertEqual(self.live.height, 0)
        self.assertEqual(len(self.screen.text().splitlines()), 2)
        self.assertEqual(self.screen.text().count("\u2588"), 84)

    def test_the_height_cap_holds_with_a_wrapped_bar(self):
        self.push(("step_start", {"id": "s", "n": 1, "title": "Tests", "parent": None}),
                  ("units", {"step": "s", "total": 400, "kind": "tests"}))
        self.assertLessEqual(len(self.live.region(self.board, 59, 4)), 4)

    def test_redraws_at_most_every_tenth_of_a_second(self):
        writes = []
        live = jr.LiveRenderer(writes.append, size=lambda: os.terminal_size(self.dims), clock=self.clock, color=False)
        live.render(self.board, feed(self.board, *start()))
        drawn = len(writes)
        self.clock.advance(0.05)
        live.tick(self.board)
        self.assertEqual(len(writes), drawn)
        self.clock.advance(0.06)
        live.tick(self.board)
        self.assertEqual(len(writes), drawn + 1)

    def test_a_short_pane_keeps_the_padding_of_the_unit_lines_it_shows(self):
        self.push(*start(size=3),
                  ("unit_start", {"step": "tests", "id": "tests/a", "title": "Short"}),
                  ("unit_start", {"step": "tests", "id": "tests/b", "title": "A much longer module"}),
                  ("unit_start", {"step": "tests", "id": "tests/c", "title": "Mid length"}))
        full = self.live.region(self.board, 59)
        short = self.live.region(self.board, 59, max_lines=2)        # the row and "Short" scrolled out of view
        self.assertEqual(short, full[-2:])                            # padded to the longest running unit both times
        self.assertTrue(short[-1].startswith("    Mid length" + " " * 12), short)

    def test_the_region_never_exceeds_rows_minus_two(self):
        self.dims = (60, 6)
        self.push(*start(size=5), *[mod_start(n) for n in "abcde"], *[cls(n) for n in "abcde"])
        region = self.live.region(self.board, 59, 4)
        self.assertEqual([line[4:9] for line in region], ["Mod B", "Mod C", "Mod D", "Mod E"])   # the last four
        self.assertEqual(region, self.live.region(self.board, 59)[-4:])
        self.assertTrue(all(len(line) <= 59 for line in region), region)
        self.assertEqual(len(self.screen.text().splitlines()), 4)
        self.assertTrue(all(len(line) <= 59 for line in self.screen.lines), self.screen.lines)
        self.assertFalse(self.screen.clamped)

    def test_boxes_cap_at_one_line(self):
        self.dims = (40, 10)
        self.push(*start(), mod_start("a"), *[cls("a") for _ in range(200)])
        self.assertEqual(self.live.region(self.board, 39, 8)[1:], ["    Mod A  " + "•" * 18 + "+182" + "  0:00"])

    def test_a_module_that_exactly_fits_has_no_counter(self):
        self.dims = (40, 10)
        self.push(*start(), mod_start("a"), *[cls("a") for _ in range(22)])
        self.assertEqual(self.live.region(self.board, 39, 8)[1:], ["    Mod A  " + "•" * 22 + "  0:00"])

    def test_a_module_one_over_shows_the_counter_in_the_last_cells(self):
        self.dims = (40, 10)
        self.push(*start(), mod_start("a"), *[cls("a") for _ in range(23)])
        self.assertEqual(self.live.region(self.board, 39, 8)[1:], ["    Mod A  " + "•" * 20 + "+3" + "  0:00"])

    def test_the_counter_keeps_the_strip_width_as_digits_grow(self):
        self.dims = (40, 10)
        for total in (23, 40, 126, 127, 128, 1000, 5000):
            with self.subTest(total=total):
                board = jr.Board(clock=lambda: 100.0)
                feed(board, *start(), mod_start("a"), *[cls("a") for _ in range(total)])
                live = jr.LiveRenderer(lambda text: None, size=lambda: os.terminal_size(self.dims), clock=lambda: 100.0,
                                       color=False)
                strip = live._sub_lines(next(iter(board.rows.values())).units["tests/a"], 39)[0]
                self.assertEqual(jr._vlen(strip), 39)                  # elapsed never moves
                self.assertTrue(strip.endswith("  0:00"), strip)
                dots = strip[len("    Mod A  "):-len("  0:00")].lstrip("•")
                self.assertRegex(dots, r"^\+\d+$")
                self.assertEqual(int(dots[1:]), total - (22 - len(dots)))

    def test_the_counter_is_green_without_a_hidden_failure_and_red_with_one(self):
        self.dims = (40, 10)
        def strip(first):
            board = jr.Board(clock=lambda: 100.0)
            feed(board, *start(), mod_start("a"), cls("a", first), *[cls("a") for _ in range(40)])
            live = jr.LiveRenderer(lambda text: None, size=lambda: os.terminal_size(self.dims), clock=lambda: 100.0,
                                   color=True)
            return live._sub_lines(next(iter(board.rows.values())).units["tests/a"], 39)[0]
        self.assertTrue(strip("pass").endswith("\x1b[32m+22\x1b[0m  0:00"), repr(strip("pass")))
        self.assertTrue(strip("skip").endswith("\x1b[32m+22\x1b[0m  0:00"), repr(strip("skip")))
        self.assertTrue(strip("fail").endswith("\x1b[31m+22\x1b[0m  0:00"), repr(strip("fail")))

    def test_a_failure_among_the_kept_dots_does_not_redden_the_counter(self):
        board = jr.Board(clock=lambda: 100.0)
        feed(board, *start(), mod_start("a"), *[cls("a") for _ in range(40)], cls("a", "fail"))
        live = jr.LiveRenderer(lambda text: None, size=lambda: os.terminal_size((40, 10)), clock=lambda: 100.0, color=True)
        strip = live._sub_lines(next(iter(board.rows.values())).units["tests/a"], 39)[0]
        self.assertIn("\x1b[32m+22\x1b[0m", strip)

    def test_a_tiny_strip_still_fits(self):
        board = jr.Board(clock=lambda: 100.0)
        feed(board, *start(), mod_start("a"), *[cls("a") for _ in range(50)])
        live = jr.LiveRenderer(lambda text: None, size=lambda: os.terminal_size((40, 10)), clock=lambda: 100.0, color=False)
        module = next(iter(board.rows.values())).units["tests/a"]
        for cols in range(1, 41):
            with self.subTest(cols=cols):
                line = live._sub_lines(module, cols)[0]
                self.assertLessEqual(jr._vlen(line), cols)      # a wider line would wrap and leave a ghost line
                if cols >= 4 + 1 + 2 + 1 + 2 + 4:       # room for a title column, one dot and elapsed: it fills cols
                    self.assertEqual(jr._vlen(line), cols)
                    self.assertTrue(line.endswith("  0:00"), line)

    def test_an_elapsed_of_ten_minutes_keeps_the_line_at_cols(self):
        clock = [100.0]
        board = jr.Board(clock=lambda: clock[0])
        feed(board, *start(), mod_start("a"), cls("a"))
        live = jr.LiveRenderer(lambda text: None, size=lambda: os.terminal_size((40, 10)), clock=lambda: clock[0],
                               color=False)
        module = next(iter(board.rows.values())).units["tests/a"]
        for cols in (15, 20, 39):     # 15: the narrowest line with a title column, a dot and a 5-column elapsed
            with self.subTest(cols=cols):
                clock[0] = 700.0
                line = live._sub_lines(module, cols)[0]
                self.assertEqual(jr._vlen(line), cols)
                self.assertTrue(line.endswith("  10:00"), line)
                self.assertIn("•", line)

    def test_a_unit_with_no_dots_in_colour_is_its_title_then_elapsed(self):
        board = jr.Board(clock=lambda: 100.0)
        feed(board, *start(), mod_start("a"))
        live = jr.LiveRenderer(lambda text: None, size=lambda: os.terminal_size((40, 10)), clock=lambda: 100.0, color=True)
        line = live._sub_lines(next(iter(board.rows.values())).units["tests/a"], 39)[0]
        self.assertEqual(line, "    Mod A".ljust(39 - 4) + "0:00")

    def test_shrinking_the_region_clears_the_old_lines(self):
        self.push(*start(), mod_start("a"), mod_start("b"))
        self.push(("unit_end", {"step": "tests", "id": "tests/a", "status": "ok", "ms": 1, "totals": None}),
                  ("unit_end", {"step": "tests", "id": "tests/b", "status": "ok", "ms": 1, "totals": None}))
        self.assertEqual(len(self.screen.text().splitlines()), 1)

    def test_colour_and_no_colour_glyphs(self):
        for color in (True, False):
            with self.subTest(color=color):
                writes = []
                board = jr.Board(clock=self.clock)
                live = jr.LiveRenderer(writes.append, size=lambda: os.terminal_size(self.dims), clock=self.clock,
                                       color=color)
                live.render(board, feed(board, *start(), mod_start("a"), cls("a")))
                text = "".join(writes)
                if color:
                    self.assertIn("\x1b[32m•", text)
                else:
                    self.assertIn("  Mod A  •", text)
                    self.assertNotIn("\x1b[32m", text)

    def test_a_failed_class_is_a_red_cross_among_the_dots(self):
        writes = []
        live = jr.LiveRenderer(writes.append, size=lambda: os.terminal_size(self.dims), clock=self.clock)
        live.render(self.board, feed(self.board, *start(), mod_start("a"), cls("a"), cls("a", "fail")))
        self.assertIn("\x1b[32m•\x1b[0m\x1b[31m✗\x1b[0m", "".join(writes))

    def test_check_marks_are_green_and_crosses_red_in_colour_only(self):
        for color in (True, False):
            with self.subTest(color=color):
                writes = []
                board = jr.Board(clock=self.clock)
                live = jr.LiveRenderer(writes.append, size=lambda: os.terminal_size(self.dims), clock=self.clock,
                                       color=color)
                live.render(board, [("header", "📤 Juneau push · a@b ✓ · PGP ✗")])
                text = "".join(writes)
                if color:
                    self.assertIn("a@b \x1b[32m✓\x1b[0m · PGP \x1b[31m✗\x1b[0m", text)
                else:
                    self.assertIn("a@b ✓ · PGP ✗", text)
                    self.assertNotIn("\x1b[3", text)

    def test_header_rows_mark_the_icon_column_and_the_title_wraps_with_a_hanging_indent(self):
        for color in (True, False):
            with self.subTest(color=color):
                writes, screen = [], Screen()
                live = jr.LiveRenderer(lambda s: writes.append(s) or screen.feed(s),
                                       size=lambda: os.terminal_size((21, 10)), clock=self.clock, color=color)
                live.render(jr.Board(clock=self.clock), [("header", "Push: Juneau · master · a b c d", jr.HEAD_MARK),
                                                         ("header", "Output: tail -f /a/very/long/path.log", jr.MORE_MARK)])
                self.assertEqual(screen.text().splitlines(),                  # the frame cuts the title to one line
                                 ["▶ Push: Juneau · ma…", "▶ Output: tail -f /a/very/long/path.log"])
                self.assertEqual("\x1b[2m▶\x1b[0m" in "".join(writes), color)     # the rows under the title are dim
                self.assertEqual("\x1b[36m▶\x1b[0m Push:" in "".join(writes), color)   # the title marker is cyan, like the spinner
                live.close()                                                    # scrollback wraps it, hanging indent
                self.assertEqual(screen.text().splitlines(),
                                 ["▶ Push: Juneau ·", "  master · a b c d", "▶ Output: tail -f /a/very/long/path.log"])

    def test_hides_the_cursor_while_drawing_and_shows_it_on_close(self):
        writes = []
        live = jr.LiveRenderer(writes.append, size=lambda: os.terminal_size(self.dims), clock=self.clock, color=False)
        live.render(self.board, feed(self.board, *start()))
        self.assertTrue(writes[0].startswith("\x1b[?25l"))
        live.close()
        self.assertTrue(writes[-1].endswith("\x1b[?25h"))

    def test_close_prints_the_header_wrapped_then_the_transcript_uncut_then_the_final_row(self):
        self.dims = (30, 10)
        self.session(header=("Juneau", "master", "a b c d e f"))
        self.push(("step_start", {"id": "a", "n": 1, "title": "Alpha", "parent": None}),
                  ("step_end", {"id": "a", "status": "ok", "ms": 1, "exit": None, "summary": None, "totals": None}))
        self.live.render(self.board, [("text", "y" * 80)])
        self.live.render(self.board, [("final", "ok", 4000, None, None, None, None)])
        self.assertNotIn("Overall", self.screen.text())             # the final row is not part of the frame
        self.live.close()
        lines = self.screen.text().splitlines()
        self.assertEqual(lines[:2], ["▶ Push: Juneau · master · a b", "  c d e f"])
        self.assertIn("y" * 80, lines)                               # uncut in scrollback
        self.assertEqual(lines[-1], "▶ Overall  ✅ Done in 0:04")
        self.assertTrue(self.screen.cursor_visible)

    def test_the_header_reaches_scrollback_exactly_once(self):
        self.session()
        self.push(*start())
        for _ in range(3):
            self.live.pause()
            self.live.resume()
        self.live.close()
        self.assertEqual(self.screen.text().count("Push: Juneau · master"), 1)
        self.assertEqual(self.screen.text().count("Commit Message: m"), 1)

    def test_pause_prints_the_transcript_and_resume_draws_only_the_live_part(self):
        self.session()
        self.push(("step_start", {"id": "a", "n": 1, "title": "Alpha", "parent": None}),
                  ("step_end", {"id": "a", "status": "ok", "ms": 1, "exit": None, "summary": None, "totals": None}))
        self.push(*start())
        self.live.pause()
        self.assertTrue(self.screen.text().splitlines()[-1].startswith("✔ Alpha"))
        self.assertEqual(self.live.transcript, [])
        self.live.resume()
        lines = self.screen.text().splitlines()
        self.assertEqual(lines.count("▶ Push: Juneau · master"), 2)      # scrollback's copy and the frame's title
        self.assertEqual(sum(line.startswith("✔ Alpha") for line in lines), 1)

    def test_after_close_a_block_prints_at_once(self):
        self.live.close()
        self.live.render(self.board, [("text", "late")])
        self.assertEqual(self.screen.text(), "late")

    def test_a_nested_renderer_never_prints_the_sticky_header(self):
        live = jr.LiveRenderer(self.screen.feed, size=lambda: os.terminal_size(self.dims), clock=self.clock,
                               color=False, sticky=[("header", "Push: Juneau · master", jr.HEAD_MARK)])
        live.render(self.board, feed(self.board, *start()))
        self.assertTrue(self.screen.text().startswith("▶ Push: Juneau · master"))
        live.pause()
        live.close()
        self.assertNotIn("Push:", self.screen.text())

    def test_the_up_count_follows_a_narrower_terminal_and_caps_at_rows_minus_one(self):
        self.live.widths, self.live.partial = [59, 59, 10], 0
        self.assertEqual(self.live._up(os.terminal_size((60, 20))), 3)
        self.assertEqual(self.live._up(os.terminal_size((30, 20))), 5)          # 2 + 2 + 1 after a reflow
        self.assertEqual(self.live._up(os.terminal_size((30, 4))), 3)           # the cursor stops at the top row
        self.live.partial = 45
        self.assertEqual(self.live._up(os.terminal_size((30, 20))), 6)          # a wrapped prompt line under it

    def test_a_run_started_on_the_bottom_line_redraws_without_climbing_past_its_frame(self):
        self.screen = Screen(rows=10, cols=60, start_row=9)
        self.live.write = self.screen.feed
        self.session(more=("Output: tail -f /" + "o" * 80,))                    # a log line that wraps to 2 lines
        self.push(*start(), mod_start("a"))
        for _ in range(5):
            self.push(cls("a"))
        self.assertFalse(self.screen.clamped)
        shown = [line for line in self.screen.lines if line.strip()]
        self.assertEqual(shown.count("▶ Push: Juneau · master"), 1)             # no stale copy of the frame
        self.assertEqual(sum("Mod A" in line for line in shown), 1)

    def test_a_log_line_longer_than_the_terminal_costs_its_wrapped_height(self):
        self.dims = (40, 8)
        self.screen = Screen(rows=8, cols=40)
        self.live.write = self.screen.feed
        self.session(lines=(), more=("Output: tail -f /" + "o" * 60,))
        self.push(*start())
        self.assertLessEqual(len([line for line in self.screen.lines if line.strip()]), 6)
        self.assertFalse(self.screen.clamped)

    def test_pause_clears_the_region_and_resume_redraws_it(self):
        self.push(*start(), mod_start("a"))
        self.live.pause()
        self.assertNotIn(self.spin() + "Tests", self.screen.text())
        self.clock.advance(1)
        self.live.tick(self.board)
        self.assertEqual(self.screen.text(), "")
        self.live.resume()
        self.assertTrue(self.screen.text().startswith(self.spin() + "Tests"))

    def test_a_watchdog_prompt_writes_under_the_frame_and_stops_redraws_until_the_line_ends(self):
        self.push(*start())
        self.push(("prompt", {"step": "tests", "text": "Passphrase: "}))
        lines = self.screen.text().splitlines()
        self.assertTrue(lines[0].startswith(self.spin() + "Tests"), lines)
        self.assertEqual(lines[1], "Passphrase:")
        self.assertTrue(self.screen.cursor_visible)
        self.clock.advance(1)
        self.live.tick(self.board)
        self.assertEqual(self.screen.text().splitlines()[1], "Passphrase:")
        self.push(("prompt_echo", {"step": "tests", "text": "***"}))
        self.push(("prompt_end", {"step": "tests"}))
        lines = self.screen.text().splitlines()
        self.assertEqual(lines[0], "Passphrase: ***")                  # the prompt joined the transcript
        self.assertTrue(lines[1].startswith(self.spin() + "Tests"), lines)
        self.assertEqual(len(lines), 2)                                  # no stale copy of the old frame
        self.assertFalse(self.screen.cursor_visible)

    def test_rows_that_finish_during_a_prompt_are_held_until_it_ends(self):
        self.push(*start())
        self.push(("prompt", {"step": "tests", "text": "Passphrase: "}))
        self.push(("step_end", {"id": "tests", "status": "ok", "ms": 0, "exit": None, "summary": "done",
                                "totals": None}))
        self.assertEqual(self.screen.text().splitlines()[1], "Passphrase:")      # the frame is not redrawn
        self.push(("prompt_end", {"step": "tests"}))
        self.assertEqual(self.screen.text().splitlines()[:2], ["Passphrase:", "✔ " + "Tests".ljust(10) + "  done"])

    def viewport(self, cols, rows):
        """A Screen of the renderer's size, with a shell line above where the run starts."""
        self.dims = (cols, rows)
        self.screen = Screen(rows=rows, cols=cols)
        self.screen.feed("$ push\n")
        self.live.write = self.screen.feed

    def test_rows_scrolled_out_of_view_come_back_when_the_pane_grows_again(self):
        self.dims = (60, 40)
        self.screen = Screen(rows=40, cols=60)
        self.live.write = self.screen.feed
        self.session()
        for n in "abcdef":
            self.push(("step_start", {"id": n, "n": 1, "title": f"Step {n}", "parent": None}),
                      ("step_end", {"id": n, "status": "ok", "ms": 1, "exit": None, "summary": None, "totals": None}))
        self.push(*start())
        words = lambda: [line.split()[:3] for line in self.screen.lines if line.strip()]     # not the ticking times
        full = words()
        self.assertEqual(full[0], ["▶", "Push:", "Juneau"])
        self.dims = (60, 6)
        self.screen.resize(rows=6, cols=60)         # Task 1's viewport: keeps the bottom rows, as a terminal does
        self.clock.advance(1)
        self.live.tick(self.board)
        self.assertEqual(words(), full[-4:])
        self.dims = (60, 40)
        self.screen.resize(rows=40, cols=60)
        self.clock.advance(1)
        self.live.tick(self.board)
        self.assertEqual(words(), full)

    def test_rows_scrolled_out_of_view_come_back_when_the_board_shrinks(self):
        self.dims = (60, 8)
        self.screen = Screen(rows=8, cols=60)
        self.live.write = self.screen.feed
        self.session()
        self.push(("step_start", {"id": "a", "n": 1, "title": "Alpha", "parent": None}),
                  ("step_end", {"id": "a", "status": "ok", "ms": 1, "exit": None, "summary": None, "totals": None}))
        self.push(*start(size=5), *[mod_start(n) for n in "abcde"])
        shown = [line for line in self.screen.lines if line.strip()]
        self.assertEqual([line.split()[:2] for line in shown], [[self.spin().strip(), "Tests"]]
                         + [["Mod", n] for n in "ABCDE"])          # the header and Alpha scrolled out of view
        self.push(*[cls(n) for n in "abcde"],
                  *[("unit_end", {"step": "tests", "id": f"tests/{n}", "status": "ok", "ms": 1}) for n in "abcde"])
        shown = [line for line in self.screen.lines if line.strip()]
        self.assertEqual(shown[:3], ["▶ Push: Juneau · master", "▶ Commit Message: m", shown[2]])
        self.assertTrue(shown[2].startswith("✔ Alpha"), shown)

    def test_the_spinner_still_turns_when_nothing_else_changes(self):
        self.push(*start())
        first = self.screen.text()
        self.clock.advance(0.15)        # past the next frame: 101.1 / 0.1 rounds down to the same one
        self.live.tick(self.board)
        self.assertNotEqual(self.screen.text(), first)

    def test_close_leaves_the_plain_renderers_record_apart_from_alignment(self):
        events = [("session", {"title": "Push", "header": ["Juneau", "master"], "lines": ["Commit Message: m"],
                               "header_more": [], "nested": False}),
                  ("step_start", {"id": "a", "n": 1, "title": "Alpha", "parent": None}),
                  ("step_end", {"id": "a", "status": "ok", "ms": 1000, "exit": None, "summary": "fine", "totals": None}),
                  ("step_start", {"id": "b", "n": 2, "title": "Beta", "parent": None}),
                  ("note", {"step": "b", "level": "warn", "text": "careful"}),
                  ("step_end", {"id": "b", "status": "warn", "ms": 2000, "exit": None, "summary": None, "totals": None}),
                  ("done", {"status": "ok"})]
        self.dims = (120, 40)
        self.screen = Screen(rows=40, cols=120)
        self.live.write = self.screen.feed
        board = jr.Board(clock=self.clock)      # started with the live one, so both finals read the same elapsed
        self.push(*events)
        self.live.close()
        live = [line.rstrip() for line in self.screen.text().splitlines() if line.strip()]
        expected = [line.rstrip() for line in plain(board, feed(board, *events), self.clock).splitlines()
                    if line.strip()]
        squash = lambda lines: [" ".join(line.split()) for line in lines]     # label-width padding may differ
        self.assertEqual(squash(live), squash(expected))

    def test_a_dump_at_a_prompt_keeps_the_line_above_the_frame(self):
        self.viewport(60, 10)
        self.push(*start())
        self.push(("prompt", {"step": "tests", "text": "Passphrase: "}))
        held = self.live.dump()              # Console._fallback's path
        self.live.close()
        self.assertEqual(self.screen.history().splitlines()[0], "$ push")
        self.assertIn("Passphrase:", [line.rstrip() for line in held])

    def test_a_prompt_and_echo_that_wrap_are_counted_at_the_width(self):
        self.viewport(30, 10)
        self.push(*start())
        self.push(("prompt", {"step": "tests", "text": "Enter the signing key passphrase: "}))   # 34 columns: wraps
        self.push(("prompt_echo", {"step": "tests", "text": "*" * 30}))                         # and wraps again
        self.push(("prompt_end", {"step": "tests"}))
        lines = self.screen.history().splitlines()
        self.assertFalse(self.screen.clamped)
        self.assertEqual(lines[0], "$ push")                             # the cursor-up did not overshoot
        self.assertEqual(sum(line.startswith("Enter the") for line in lines), 1)     # nor leave a stale copy
        self.assertTrue(lines[-1].startswith(self.spin() + "Tests"), lines)

    def test_a_prompt_line_exactly_as_wide_as_the_terminal_counts_once(self):
        self.viewport(30, 10)
        self.push(*start())
        self.push(("prompt", {"step": "tests", "text": "P" * 29 + " "}))  # 30 columns: the cursor waits at its end
        self.live._clear()                                               # what pause() or a resize does mid-prompt
        self.assertEqual(self.screen.history().splitlines()[0], "$ push")
        self.assertFalse(self.screen.clamped)

    def test_a_prompt_too_tall_for_the_frame_clears_it(self):
        self.viewport(20, 5)                                             # B = 3; a prompt wrapping to 2 lines is too tall
        self.push(*start())
        self.push(("prompt", {"step": "tests", "text": "Type the full passphrase for the key: "}))
        self.assertEqual(self.screen.text(), "$ push\nType the full passph\nrase for the key:")
        self.push(("prompt_end", {"step": "tests"}))
        self.assertTrue(self.screen.text().splitlines()[-2].startswith(self.spin() + "Tests"))    # the unit bar wraps to 2 lines

    def test_closing_at_a_prompt_keeps_the_line_above_the_frame(self):
        self.viewport(60, 10)
        self.push(*start())
        self.push(("prompt", {"step": "tests", "text": "Passphrase: "}))
        self.live.close()                    # Ctrl-C at the prompt: _end_session -> Console.close -> close()
        lines = self.screen.history().splitlines()
        self.assertEqual(lines[0], "$ push")
        self.assertEqual(lines.count("Passphrase:"), 1)
        self.assertFalse(self.screen.clamped)

    def test_a_legacy_raw_block_without_a_role_is_a_prompt(self):
        self.push(*start())
        self.live.render(self.board, [("raw", "Old: ")])
        self.assertTrue(self.live.prompting)
        self.live.render(self.board, [("raw", "\n")])
        self.assertFalse(self.live.prompting)
        self.assertEqual(self.screen.text().splitlines()[0], "Old:")

    def test_an_echo_or_end_without_a_prompt_does_not_misplace_the_frame(self):
        self.viewport(60, 10)
        self.push(*start())
        self.live.render(self.board, [("raw", "\n", "end")])            # an end with no prompt up: ignored
        self.assertEqual(self.screen.history().splitlines()[0], "$ push")
        self.live.render(self.board, [("raw", "x", "echo")])            # an echo with no prompt starts one
        self.assertTrue(self.live.prompting)

    def test_board_raw_blocks_carry_their_role(self):
        b = jr.Board(clock=self.clock)
        self.assertEqual(b.apply("prompt", {"step": None, "text": "P: "}), [("raw", "P: ", "prompt")])
        self.assertEqual(b.apply("prompt_echo", {"step": None, "text": "x"}), [("raw", "x", "echo")])
        self.assertEqual(b.apply("prompt_end", {"step": None}), [("raw", "\n", "end")])

    def test_a_large_reactor_keeps_its_count_and_totals_and_wraps_the_bar(self):
        self.dims = (64, 10)            # 8 region lines of 10 cells hold the 80 modules
        self.push(*start(size=80))
        for i in range(80):
            name = f"m{i}"
            self.push(mod_start(name), ("unit_end", {"step": "tests", "id": f"tests/{name}", "status": "ok",
                                                       "ms": 1, "totals": (1, 0, 0, 0)}))
        self.push(("tests", {"step": "tests", "total": 7139, "fail": 2, "err": 0, "skip": 0}))
        lines = self.screen.text().splitlines()
        self.assertTrue(all(len(line) <= 63 for line in lines), lines)
        self.assertNotIn("\u2026", "".join(lines))        # no cell is cut: the bar wraps instead
        self.assertEqual("".join(lines).count("\u2588"), 80)
        self.assertTrue(lines[-1].startswith(" " * 14 + "80/80  7,139 tests, 2 failed") and lines[-1].endswith("1:21"), lines[-1])   # on its own line
        self.push(("step_end", {"id": "tests", "status": "fail", "ms": 90000, "exit": 1, "summary": None,
                                "totals": None}))
        rows = self.screen.text().splitlines()
        self.assertTrue(all(len(row) <= 63 for row in rows), rows)
        self.assertTrue(rows[-1].lstrip().startswith("80/80  7,139 tests, 2 failed") and rows[-1].endswith("1:30"), rows[-1])
        self.assertTrue(rows[0].startswith("✗ " + "Tests".ljust(10) + "  ██"), rows[0])

    def test_a_row_too_long_even_without_its_bar_is_cut_from_the_right(self):
        line = jr._fit_row("A very long step label", ["█"] * 5, "5/5  7,139 tests, 2 failed", "0:01", 20)
        self.assertEqual(jr._vlen(line), 20)
        self.assertTrue(line.endswith("…  0:01"), line)

    def test_wide_characters_take_two_columns(self):
        self.assertEqual(jr._vlen("测试 ok"), 7)
        self.assertEqual(jr._vcut("测试测试", 5), "测试")
        self.assertEqual(jr._vlen(jr._fit("测" * 40, "0:01", 30)), 30)
        self.dims = (40, 10)
        self.push(*start(), ("unit_start", {"step": "tests", "id": "tests/a", "title": "测试" * 20}),
                  *[cls("a") for _ in range(100)])
        subs = self.live.region(self.board, 39, 8)[1:]
        self.assertEqual(len(subs), 1)
        self.assertTrue(jr._vlen(subs[0]) <= 39, subs)
        self.assertTrue(subs[0].startswith("    " + "测试" * 4 + "测  "), subs[0])

    def test_close_is_idempotent_and_stops_redraws(self):
        writes = []
        live = jr.LiveRenderer(writes.append, size=lambda: os.terminal_size(self.dims), clock=self.clock, color=False)
        live.render(self.board, feed(self.board, *start(), mod_start("a")))
        live.close()
        closed = len(writes)
        live.close()
        self.clock.advance(1)
        live.tick(self.board)
        self.assertEqual(len(writes), closed)
        live.render(self.board, feed(self.board, ("unit_end", {"step": "tests", "id": "tests/a",
                                                                 "status": "ok", "ms": 1, "totals": None}),
                                     ("step_end", {"id": "tests", "status": "ok", "ms": 0, "exit": None,
                                                   "summary": "late", "totals": None})))
        self.assertEqual("".join(writes[closed:]), "✔ " + "Tests".ljust(10) + "  late\n")

    def test_step_end_clears_the_quiet_state(self):
        b = jr.Board("actionable", clock=self.clock)
        feed(b, *start(), ("quiet", {"step": "tests", "seconds": 25}))
        self.assertEqual(b.rows["tests"].quiet, 25)
        feed(b, ("step_end", {"id": "tests", "status": "fail", "ms": 30000, "exit": 1, "summary": None,
                              "totals": None}))
        self.assertEqual(b.rows["tests"].quiet, 0)

    def test_a_sub_rows_step_end_clears_its_parents_quiet_state(self):
        """release.py: a sub-row (parent=) draws its quiet on the parent row, whether it is still one box on the
        parent's bar or its tool's modules took the bar over."""
        for takes_over in (False, True):
            with self.subTest(takes_over=takes_over):
                b = jr.Board("actionable", clock=self.clock)
                feed(b, ("step_start", {"id": "verify", "n": 1, "title": "Verify", "parent": None}),
                     ("step_start", {"id": "verify.1", "n": 1, "title": "mvn", "parent": "verify"}))
                if takes_over:
                    feed(b, ("reactor", {"step": "verify.1", "size": 1, "poms": [], "tests": True}),
                         ("unit_start", {"step": "verify.1", "id": "verify.1/a", "title": "Mod A"}))
                feed(b, ("quiet", {"step": "verify.1", "seconds": 25}))
                self.assertEqual(b.rows["verify"].quiet, 25)
                feed(b, ("step_end", {"id": "verify.1", "status": "ok", "ms": 30000, "exit": None, "summary": None,
                                      "totals": None}))
                self.assertEqual(b.rows["verify"].quiet, 0)

    def test_a_quiet_row_says_so(self):
        self.push(*start(), ("quiet", {"step": "tests", "seconds": 25}))
        self.assertIn(" · quiet 25s", self.screen.text())


class UnitLinesTest(unittest.TestCase):
    """Every running unit has a line under its row: its title padded to the row's longest, its dots, then its elapsed
    time at the right edge.  When the region is short its top lines scroll out of view; none are packed together."""

    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        self.board = jr.Board(clock=lambda: 100.0)

    def region(self, *events, cols=60, rows=18):
        feed(self.board, *events)
        live = jr.LiveRenderer(lambda text: None, size=lambda: os.terminal_size((cols + 1, rows + 2)),
                               clock=lambda: 100.0, color=False)
        return live.region(self.board, cols, rows)

    @staticmethod
    def unit(id, title, step="tests"):
        return ("unit_start", {"step": step, "id": f"{step}/{id}", "title": title})

    def test_every_running_unit_has_its_own_line(self):
        for step in ("compile", "x-tool"):                      # data-driven: the step id does not matter
            with self.subTest(step=step):
                reset_run_state()
                self.board = jr.Board(clock=lambda: 100.0)
                lines = self.region(*start(step, 4), mod_start("a", step), mod_start("b", step), mod_start("c", step))
                self.assertEqual(lines[1:], [f"    Mod {n}  ".ljust(60 - 4) + "0:00" for n in "ABC"])

    def test_a_unit_with_results_keeps_its_start_order(self):
        lines = self.region(*start(), mod_start("a"), mod_start("b"), cls("b"), cls("b", "fail"))
        self.assertEqual(lines[1:], ["    Mod A  ".ljust(56) + "0:00", "    Mod B  •F".ljust(56) + "0:00"])

    def test_titles_pad_to_the_longest_in_the_row(self):
        lines = self.region(*start(), self.unit("x", "X"), self.unit("y", "Longer Y"),
                            ("unit_dot", {"step": "tests", "id": "tests/x", "outcome": "pass"}))
        self.assertEqual(lines[1:], ["    " + "X".ljust(8) + "  •".ljust(44) + "0:00",
                                     "    Longer Y  ".ljust(56) + "0:00"])

    def test_a_long_title_is_cut_at_half_the_line(self):
        lines = self.region(*start(), self.unit("x", "x" * 50))
        self.assertEqual(lines[1], "    " + "x" * 30 + "  ".ljust(22) + "0:00")

    def test_pom_units_never_get_a_line(self):
        feed(self.board, ("step_start", {"id": "mvn", "n": 1, "title": "mvn", "parent": None}),
             ("reactor", {"step": "mvn", "size": 3, "poms": ["mvn/p"], "tests": False}),
             ("unit_start", {"step": "mvn", "id": "mvn/p", "title": "Mod P"}))
        lines = self.region(("unit_start", {"step": "mvn", "id": "mvn/a", "title": "Mod A"}),
                            ("unit_start", {"step": "mvn", "id": "mvn/b", "title": "Mod B"}))
        self.assertEqual([line for line in lines if line.startswith("    ")],
                         ["    Mod A  ".ljust(56) + "0:00", "    Mod B  ".ljust(56) + "0:00"])

    def test_a_short_pane_shows_the_last_unit_lines_whole_and_the_row_scrolls_out_of_view(self):
        lines = self.region(*start(), mod_start("a"), mod_start("b"), mod_start("c"), rows=3)
        self.assertEqual(lines, [f"    Mod {n}  ".ljust(56) + "0:00" for n in "ABC"])

    def test_a_tests_row_with_units_has_no_current_file_line(self):
        lines = self.region(*start(), ("step_progress", {"step": "tests", "text": "test_x.py"}), mod_start("a"))
        self.assertEqual(lines[1:], ["    Mod A  ".ljust(56) + "0:00"])

    def test_a_tests_row_without_units_keeps_its_current_file_line(self):
        lines = self.region(("step_start", {"id": "py", "n": 1, "title": "Py", "parent": None}),
                            ("units", {"step": "py", "total": 3, "kind": "tests"}),
                            ("step_progress", {"step": "py", "text": "test_x.py"}))
        self.assertEqual(lines[1:], ["    test_x.py"])

    def test_a_tests_row_whose_units_have_all_closed_shows_its_current_file_line_again(self):
        lines = self.region(("step_start", {"id": "py", "n": 1, "title": "Py", "parent": None}),
                            ("units", {"step": "py", "total": 3, "kind": "tests"}),
                            ("unit_start", {"step": "py", "id": "a.py", "title": "a.py", "src": "file"}),
                            ("unit_end", {"step": "py", "id": "a.py", "status": "ok", "src": "file"}),
                            ("step_progress", {"step": "py", "text": "test_x.py"}))
        self.assertEqual(lines[1:], ["    test_x.py"])     # "while it has a running unit" (D17)


class StepEndClosesUnitsTest(unittest.TestCase):
    """A unit still open when its step ends is closed there, so no unit line outlives its row."""

    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        self.clock = Clock()
        self.board = jr.Board(clock=self.clock)

    def end(self, status, *events):
        feed(self.board, *start(), mod_start("a"), *events)
        self.clock.advance(2)
        feed(self.board, ("step_end", {"id": "tests", "status": status, "ms": 2000, "exit": 0, "summary": None,
                                       "totals": None}))
        return self.board.rows["tests"].units["tests/a"]

    def test_an_open_unit_closes_ok_with_its_elapsed(self):
        unit = self.end("ok", cls("a"))
        self.assertEqual((unit.status, unit.ms), ("ok", 2000))

    def test_an_open_unit_with_a_fail_dot_closes_fail(self):
        self.assertEqual(self.end("ok", cls("a", "fail")).status, "fail")

    def test_an_open_unit_of_a_failed_row_closes_fail(self):
        self.assertEqual(self.end("fail").status, "fail")

    def test_an_ended_unit_is_left_alone(self):
        unit = self.end("fail", ("unit_end", {"step": "tests", "id": "tests/a", "status": "ok", "ms": 7}))
        self.assertEqual((unit.status, unit.ms), ("ok", 7))

    def test_open_file_script_and_row_units_close_with_no_finished_module_line(self):
        for status, dot in (("ok", "pass"), ("ok", "fail"), ("fail", "pass")):
            with self.subTest(status=status, dot=dot):
                board = jr.Board(detail="modules", clock=self.clock)
                feed(board, ("step_start", {"id": "s", "n": 1, "title": "Script", "parent": None}),
                     *[("unit_start", {"step": "s", "id": src, "title": src, "src": src}) for src in ("file", "script")],
                     ("step_start", {"id": "s.1", "n": 1, "title": "row", "parent": "s"}),
                     *[("unit_dot", {"step": "s", "id": i, "outcome": dot}) for i in ("file", "script", "s.1")])
                self.clock.advance(2)
                blocks = feed(board, ("step_end", {"id": "s", "status": status, "ms": 2000, "exit": 0,
                                                   "summary": None, "totals": None}))
                units = board.rows["s"].units
                want = "fail" if "fail" in (status, dot) else "ok"
                self.assertEqual({i: (u.src, u.status, u.in_progress) for i, u in units.items()},
                                 {"file": ("file", want, False), "script": ("script", want, False),
                                  "s.1": ("row", want, False)})
                self.assertEqual([b for b in blocks if b[0] == "module"], [])


class LiveConsoleTest(unittest.TestCase):
    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        patcher = mock.patch.dict(os.environ, {})
        patcher.start()
        self.addCleanup(patcher.stop)
        for key in ("TERMINAL_MARKERS", "TERM", "NO_COLOR", "JUNEAU_TERMINAL_LIVE"):
            os.environ.pop(key, None)

    def test_live_mode_uses_the_live_renderer(self):
        c = jr.Console(jr.Sinks(console="board"), stream=TTY(), size=lambda: os.terminal_size((80, 24)))
        self.addCleanup(c.close)
        self.assertIsInstance(c.screen, jr.LiveRenderer)
        self.assertTrue(c.screen.color)

    def test_no_color_keeps_the_live_renderer_without_colour(self):
        with mock.patch.dict(os.environ, {"NO_COLOR": "1"}):
            c = jr.Console(jr.Sinks(console="board"), stream=TTY(), size=lambda: os.terminal_size((80, 24)))
        self.addCleanup(c.close)
        self.assertIsInstance(c.screen, jr.LiveRenderer)
        self.assertFalse(c.screen.color)

    def test_plain_output_never_has_escapes_or_carriage_returns(self):
        out = io.StringIO()
        c = jr.Console(jr.Sinks(console="board"), stream=out)
        self.addCleanup(c.close)
        for kind, fields in maven_events():
            c(kind, fields)
        c("step_end", {"id": "tests", "status": "fail", "ms": 1, "exit": 1, "summary": None, "totals": None})
        self.assertNotIn("\x1b", out.getvalue())
        self.assertNotIn("\r", out.getvalue())

    def test_default_stream_is_bound_once_so_a_later_stdout_swap_cannot_capture_frames(self):
        original, swapped = io.StringIO(), io.StringIO()
        with mock.patch.dict(os.environ, {"TERMINAL_MARKERS": ""}), mock.patch.object(sys, "stdout", original):
            c = jr.Console(jr.Sinks(console="board"), clock=Clock())
            self.addCleanup(c.close)
            with mock.patch.object(sys, "stdout", swapped):
                c("step_start", {"id": "build", "n": 1, "title": "Build", "parent": None})
                c("step_end", {"id": "build", "status": "ok", "ms": 5, "exit": 0, "summary": None, "totals": None})
        self.assertIn("✔", original.getvalue())
        self.assertEqual(swapped.getvalue(), "")


class RobustnessTest(unittest.TestCase):
    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)

    def test_a_renderer_exception_switches_to_plain_with_a_warning(self):
        out = TTY()
        with mock.patch.dict(os.environ, {"TERM": "xterm", "TERMINAL_MARKERS": ""}):
            console = jr.Console(jr.Sinks(console="board"), stream=out)
        self.assertEqual(console.mode, "live")
        console.screen.render = mock.Mock(side_effect=RuntimeError("boom"))
        console("step_start", {"id": "a", "n": 1, "title": "Alpha", "parent": None})
        self.assertIsInstance(console.screen, jr.PlainRenderer)
        self.assertIn("⚠ console view failed (RuntimeError('boom')); using plain output", out.getvalue())
        console("step_end", {"id": "a", "status": "ok", "ms": 10, "exit": None, "summary": None, "totals": None})
        self.assertIn("Alpha", out.getvalue())

    def test_a_tick_exception_also_falls_back(self):
        out = TTY()
        with mock.patch.dict(os.environ, {"TERM": "xterm", "TERMINAL_MARKERS": ""}):
            console = jr.Console(jr.Sinks(console="board"), stream=out)
        console.screen.tick = mock.Mock(side_effect=ValueError("bad"))
        console.tick()
        self.assertIsInstance(console.screen, jr.PlainRenderer)

    def test_a_log_renderer_exception_stops_the_log_only(self):
        with tempfile.TemporaryDirectory() as tmp:
            console = jr.Console(jr.Sinks(console="none", board_log=str(Path(tmp) / "c.log")))
            console.log.render = mock.Mock(side_effect=RuntimeError("disk"))
            console("say", {"text": "x", "level": "warn", "step": None})
            self.assertIsNone(console.log)
            console("say", {"text": "y", "level": "warn", "step": None})
            console.close()

    def live_console(self, out, **sinks):
        with mock.patch.dict(os.environ, {"TERM": "xterm", "TERMINAL_MARKERS": ""}):
            console = jr.Console(jr.Sinks(console="board", **sinks), stream=out,
                                 size=lambda: os.terminal_size((80, 24)))
        self.addCleanup(console.close)
        self.assertEqual(console.mode, "live")
        return console

    def test_a_log_renderer_exception_says_so_once_on_screen(self):
        out = io.StringIO()
        with tempfile.TemporaryDirectory() as tmp, mock.patch.dict(os.environ, {"TERMINAL_MARKERS": ""}):
            console = jr.Console(jr.Sinks(console="board", board_log=str(Path(tmp) / "c.log")), stream=out)
            console.log.render = mock.Mock(side_effect=OSError("disk full"))
            console("say", {"text": "⚠ x", "level": "warn", "step": None})
            console("say", {"text": "⚠ y", "level": "warn", "step": None})
            console.close()
        self.assertEqual(out.getvalue().count("⚠ board log stopped (OSError('disk full'))"), 1)
        self.assertIn("⚠ y", out.getvalue())

    def test_a_bug_in_code_the_plain_renderer_shares_stops_the_view_without_escaping(self):
        out = TTY()
        console = self.live_console(out)
        console("step_start", {"id": "a", "n": 1, "title": "Alpha", "parent": None})
        with mock.patch.object(jr, "row_cells", side_effect=RuntimeError("shared")), \
                mock.patch.object(jr, "row_parts", side_effect=RuntimeError("shared")), \
                mock.patch.object(jr, "row_text", side_effect=RuntimeError("shared")):
            console("step_end", {"id": "a", "status": "ok", "ms": 10, "exit": None, "summary": None, "totals": None})
            console("step_start", {"id": "b", "n": 2, "title": "Beta", "parent": None})
            console.tick()
        self.assertIsNone(console.screen)
        self.assertTrue(console.stopped)
        self.assertEqual(console.mode, "plain")     # still a console session: console_active() stays True
        self.assertEqual(out.getvalue().count("⚠ console view failed"), 1)
        self.assertEqual(out.getvalue().count("⚠ console view stopped"), 1)

    def test_a_plain_screen_that_fails_stops_instead_of_falling_back_to_plain_again(self):
        out = io.StringIO()
        with mock.patch.dict(os.environ, {"TERMINAL_MARKERS": ""}):
            console = jr.Console(jr.Sinks(console="board"), stream=out)
        self.assertEqual(console.mode, "plain")
        console.screen.render = mock.Mock(side_effect=RuntimeError("plain"))
        console("say", {"text": "⚠ x", "level": "warn", "step": None})
        console("say", {"text": "⚠ y", "level": "warn", "step": None})
        self.assertIsNone(console.screen)
        self.assertNotIn("using plain output", out.getvalue())
        self.assertEqual(out.getvalue().count("⚠ console view stopped (RuntimeError('plain'))"), 1)

    def test_a_fallback_does_not_repeat_lines_already_printed(self):
        out = TTY()
        console = self.live_console(out)
        console.screen._draw = mock.Mock(side_effect=RuntimeError("draw"))
        console("say", {"text": "⚠ hello-once", "level": "warn", "step": None})
        self.assertIsInstance(console.screen, jr.PlainRenderer)
        self.assertEqual(out.getvalue().count("hello-once"), 1)

    def test_a_failing_region_does_not_repeat_lines_already_printed(self):
        out = TTY()
        console = self.live_console(out)
        for kind, fields in start() + [mod_start("a"), cls("a")]:
            console(kind, fields)
        with mock.patch.object(jr.LiveRenderer, "_sub_lines", side_effect=RuntimeError("sub")):
            # a warn say would become the running row's reason, so print a finished row instead
            console("step_start", {"id": "b", "n": 2, "title": "Beta", "parent": None})
            console("step_end", {"id": "b", "status": "ok", "ms": 0, "exit": None, "summary": "hello-once",
                                 "totals": None})
        self.assertIsInstance(console.screen, jr.PlainRenderer)
        self.assertEqual(out.getvalue().count("hello-once"), 1)

    def test_a_fallback_re_renders_only_the_blocks_after_a_raw_one_that_printed(self):
        for case in ("after", "raw"):
            with self.subTest(case):
                out = TTY()
                console = self.live_console(out)
                console.board.apply = mock.Mock(return_value=[("raw", "raw-once\n"), ("text", "after")])
                real, written = console.screen.frame, []
                console.screen.write = lambda text, write=console.screen.write: (written.append(text), write(text))

                def frame(*args):
                    if case == "after" and any(block[1] == "after" for _, block, _ in console.screen.transcript):
                        raise RuntimeError("frame")
                    if case == "raw" and "raw-once\n" in written:     # the raw block's closing redraw
                        raise RuntimeError("frame")
                    return real(*args)

                console.screen.frame = frame
                console("say", {"text": "x", "level": "warn", "step": None})
                self.assertIsInstance(console.screen, jr.PlainRenderer)
                screen = Screen(rows=40, cols=80)
                screen.feed(out.getvalue())
                self.assertEqual(screen.history().count("raw-once"), 1)
                self.assertEqual(screen.history().count("after"), 1)

    def test_a_fallback_prints_what_the_frame_held_untinted_then_the_warning_then_the_rest(self):
        out = TTY()
        console = self.live_console(out)
        console("step_start", {"id": "a", "n": 1, "title": "Alpha", "parent": None})
        console("step_end", {"id": "a", "status": "ok", "ms": 0, "exit": None, "summary": "held-once",
                             "totals": None})
        with mock.patch.object(jr.LiveRenderer, "frame", side_effect=RuntimeError("frame")):
            console("step_start", {"id": "b", "n": 2, "title": "Beta", "parent": None})
            console("step_end", {"id": "b", "status": "ok", "ms": 0, "exit": None, "summary": "after-once",
                                 "totals": None})
        self.assertIsInstance(console.screen, jr.PlainRenderer)
        screen = Screen(rows=40, cols=80)
        screen.feed(out.getvalue())
        history = screen.history()
        self.assertEqual(history.count("held-once"), 1)
        self.assertEqual(history.count("after-once"), 1)
        self.assertLess(history.index("held-once"), history.index("⚠ console view failed"))
        tail = out.getvalue()[out.getvalue().index("⚠ console view failed"):]
        self.assertNotIn("\x1b[3", tail)                               # the plain part is untinted

    def test_a_fallback_during_a_prompt_starts_the_warning_on_its_own_line(self):
        out = TTY()
        console = self.live_console(out)
        console("step_start", {"id": "a", "n": 1, "title": "Alpha", "parent": None})
        console("prompt", {"step": "a", "text": "Passphrase: "})
        self.assertTrue(console.screen.prompting)
        console.screen.render = mock.Mock(side_effect=RuntimeError("boom"))
        console("say", {"text": "x", "level": "warn", "step": None})
        self.assertIn("Passphrase: \n⚠ console view failed", jr._ANSI.sub("", out.getvalue()))

    def test_a_board_exception_stops_the_view_and_log_but_not_the_run(self):
        out = TTY()
        with tempfile.TemporaryDirectory() as tmp:
            log = Path(tmp) / "c.log"
            console = self.live_console(out, board_log=str(log))
            console.board.apply = mock.Mock(side_effect=KeyError("board"))
            console("say", {"text": "x", "level": "warn", "step": None})
            console("say", {"text": "y", "level": "warn", "step": None})
            console.tick()
            self.assertIsNone(console.screen)
            self.assertIsNone(console.log)
            self.assertEqual(console.board.apply.call_count, 1)
            console.close()
            self.assertIn("⚠ console view and board log stopped (KeyError('board'))",
                          log.read_text(encoding="utf-8"))
        self.assertEqual(out.getvalue().count("⚠ console view and board log stopped (KeyError('board'))"), 1)

    def test_a_screen_failure_stops_the_view_but_not_the_board_log(self):
        out = io.StringIO()
        with tempfile.TemporaryDirectory() as tmp, mock.patch.dict(os.environ, {"TERMINAL_MARKERS": ""}):
            log = Path(tmp) / "c.log"
            console = jr.Console(jr.Sinks(console="board", board_log=str(log)), stream=out)
            console.screen.render = mock.Mock(side_effect=BrokenPipeError("epipe"))
            console("say", {"text": "⚠ first", "level": "warn", "step": None})
            console("say", {"text": "⚠ second", "level": "warn", "step": None})
            self.assertIsNotNone(console.log)
            console.close()
            logged = log.read_text(encoding="utf-8")
        self.assertIn("⚠ console view stopped (BrokenPipeError('epipe'))", logged)
        self.assertIn("⚠ first", logged)
        self.assertIn("⚠ second", logged)
        self.assertNotIn("board log stopped", logged + out.getvalue())
        self.assertEqual(out.getvalue().count("⚠ console view stopped (BrokenPipeError('epipe'))"), 1)

    def session_console(self, out):
        """A plain console as this process's session, drawing on out."""
        jr._console = jr.subscribe(jr.Console(jr.Sinks(console="board"), stream=out))
        return jr._console

    def assert_prints_plainly_after_the_stop(self, out):
        text = out.getvalue()
        for line in ("first", "second", "FAILURE DETAIL", "❌ Failed"):
            self.assertEqual(text.count(line), 1, line)
        self.assertLess(text.index("⚠ console view"), text.index("first"))

    def test_after_a_board_exception_script_text_and_the_final_line_print_plainly(self):
        out = io.StringIO()
        with mock.patch.dict(os.environ, {"TERMINAL_MARKERS": ""}), contextlib.redirect_stdout(out):
            self.session_console(out).board.apply = mock.Mock(side_effect=KeyError("board"))
            jr.say("first")
            jr.say("second")
            jr.failure("FAILURE DETAIL")
            jr.done("fail")
            self.assertTrue(jr.console_active())
        self.assert_prints_plainly_after_the_stop(out)

    def test_after_a_screen_failure_script_text_and_the_final_line_print_plainly(self):
        out = io.StringIO()
        with mock.patch.dict(os.environ, {"TERMINAL_MARKERS": ""}), contextlib.redirect_stdout(out):
            self.session_console(out).screen.render = mock.Mock(side_effect=BrokenPipeError("epipe"))
            jr.say("first")
            jr.say("second")
            jr.failure("FAILURE DETAIL")
            jr.done("fail")
            self.assertTrue(jr.console_active())
        self.assert_prints_plainly_after_the_stop(out)

    def test_after_the_view_stops_a_watchdog_prompt_is_echoed_on_the_terminal(self):
        out = io.StringIO()
        code = "import sys, time\nsys.stdout.write('Passphrase: '); sys.stdout.flush()\ntime.sleep(0.6)\nprint('ok')"
        with mock.patch.dict(os.environ, {"TERMINAL_MARKERS": ""}), contextlib.redirect_stdout(out), \
                mock.patch.object(jr, "_input_fd", return_value=None):
            self.session_console(out).screen.render = mock.Mock(side_effect=RuntimeError("boom"))
            jr.say("x")
            jr.run_tool([sys.executable, "-c", code], "generic", "t", "T", sinks=jr.Sinks(console="board"),
                        watchdog=0.2)
        self.assertIn("Passphrase: ok\n", out.getvalue())

    def test_a_failing_pause_or_resume_falls_back_instead_of_failing_the_script(self):
        for method in ("pause", "resume"):
            for use in ("ask", "handoff"):
                with self.subTest(method=method, use=use):
                    reset_run_state()
                    out = TTY()
                    jr._console = self.live_console(out)
                    setattr(jr._console.screen, method, mock.Mock(side_effect=RuntimeError(method)))
                    if use == "ask":
                        with mock.patch("builtins.input", return_value="y"):
                            self.assertEqual(jr.ask("? "), "y")
                    else:
                        with jr.handoff():
                            pass
                    self.assertIsInstance(jr._console.screen, jr.PlainRenderer)
                    self.assertIn(f"⚠ console view failed (RuntimeError('{method}'))", out.getvalue())

    def test_a_failing_plain_pause_stops_the_view_instead_of_failing_the_script(self):
        out = io.StringIO()
        with mock.patch.dict(os.environ, {"TERMINAL_MARKERS": ""}):
            console = self.session_console(out)
        console.screen.pause = mock.Mock(side_effect=RuntimeError("pause"))
        with mock.patch("builtins.input", return_value="y"):
            self.assertEqual(jr.ask("? "), "y")
        self.assertTrue(console.stopped)
        self.assertIn("⚠ console view stopped (RuntimeError('pause'))", out.getvalue())

    def test_a_fallback_after_a_prompt_keeps_it_once(self):
        out = TTY()
        console = self.live_console(out)
        console.board.apply = mock.Mock(return_value=[("text", "before"), ("raw", "raw-once\n")])
        real = console.screen.frame

        def frame(*args):
            if any(block[1] == "before" for _, block, _ in console.screen.transcript):
                raise RuntimeError("frame")
            return real(*args)

        console.screen.frame = frame
        console("say", {"text": "x", "level": "warn", "step": None})
        self.assertIsInstance(console.screen, jr.PlainRenderer)
        screen = Screen(rows=40, cols=80)
        screen.feed(out.getvalue())
        self.assertEqual(screen.history().count("before"), 1)
        self.assertEqual(screen.history().count("raw-once"), 1)

    def test_a_plain_stop_during_a_prompt_starts_the_warning_on_its_own_line(self):
        out = io.StringIO()
        with mock.patch.dict(os.environ, {"TERMINAL_MARKERS": ""}):
            console = jr.Console(jr.Sinks(console="board"), stream=out)
        console("step_start", {"id": "a", "n": 1, "title": "Alpha", "parent": None})
        console("prompt", {"step": "a", "text": "Passphrase: "})
        console.screen.render = mock.Mock(side_effect=RuntimeError("boom"))
        console("say", {"text": "x", "level": "warn", "step": None})
        self.assertIn("Passphrase: \n⚠ console view stopped", out.getvalue())

    @staticmethod
    def _recording_kill():
        """A _terminate_group stand-in that records "kill", then still takes the child down, so it is reaped and no
        ResourceWarning is left."""
        terminate = jr._terminate_group
        return lambda record, p, grace: (record.append("kill"), terminate(p, grace))

    def test_ctrl_c_in_run_tool_pauses_the_console_first(self):
        kill = self._recording_kill()
        console = mock.Mock()
        console.sinks.console = "none"
        order = []
        console.pause.side_effect = lambda: order.append("pause")
        with mock.patch.object(jr, "_console", console), \
                mock.patch.object(jr, "_terminate_group", side_effect=lambda p, grace=None: kill(order, p, grace)), \
                mock.patch.object(jr.select, "select", side_effect=KeyboardInterrupt), \
                self.assertRaises(KeyboardInterrupt):
            jr.run_tool([sys.executable, "-c", "pass"], "generic", "t", "T",
                        sinks=jr.Sinks(console="none"))
        self.assertEqual(order[:2], ["pause", "kill"])

    def test_ctrl_c_pauses_with_signals_ignored_and_a_failing_pause_still_kills_the_child(self):
        kill = self._recording_kill()
        console = self.live_console(TTY())
        seen = []

        def pause():
            seen.append(signal.getsignal(signal.SIGINT))
            raise RuntimeError("pause")

        console.screen.pause = pause
        before = (signal.getsignal(signal.SIGINT), signal.getsignal(signal.SIGTERM))
        with mock.patch.object(jr, "_console", console), \
                mock.patch.object(jr, "_terminate_group", side_effect=lambda p, grace=None: kill(seen, p, grace)), \
                mock.patch.object(jr.select, "select", side_effect=KeyboardInterrupt), \
                self.assertRaises(KeyboardInterrupt):
            jr.run_tool([sys.executable, "-c", "pass"], "generic", "t", "T", sinks=jr.Sinks(console="none"))
        self.assertEqual(seen[:2], [signal.SIG_IGN, "kill"])
        self.assertEqual((signal.getsignal(signal.SIGINT), signal.getsignal(signal.SIGTERM)), before)

    def test_a_non_interrupt_error_in_the_read_loop_does_not_leave_the_child_running(self):
        children = []
        real_popen = subprocess.Popen

        def spawn(*args, **kwargs):
            children.append(real_popen(*args, **kwargs))
            return children[-1]

        try:
            with mock.patch.object(jr.subprocess, "Popen", side_effect=spawn), \
                    mock.patch.object(jr.select, "select", side_effect=RuntimeError("renderer")), \
                    self.assertRaises(RuntimeError):
                jr.run_tool([sys.executable, "-c", "import time; time.sleep(30)"], "generic", "t", "T",
                            sinks=jr.Sinks(console="none"))
            self.assertEqual(len(children), 1)
            self.assertIsNotNone(children[0].poll())
        finally:
            for child in children:    # only our own child, by handle
                if child.poll() is None:
                    child.kill()
                    child.wait()

    def test_session_turns_a_default_sigterm_into_keyboard_interrupt(self):
        previous = signal.signal(signal.SIGTERM, signal.SIG_DFL)
        self.addCleanup(signal.signal, signal.SIGTERM, previous)
        with mock.patch.dict(os.environ, {}, clear=False):
            os.environ.pop(jr.SESSION_ENV, None)
            jr.session("Test", sinks=jr.Sinks(console="none"))
        with self.assertRaises(KeyboardInterrupt):
            signal.raise_signal(signal.SIGTERM)

    def test_session_keeps_a_script_sigterm_handler(self):
        handler = mock.Mock()
        previous = signal.signal(signal.SIGTERM, handler)
        self.addCleanup(signal.signal, signal.SIGTERM, previous)
        with mock.patch.dict(os.environ, {}, clear=False):
            os.environ.pop(jr.SESSION_ENV, None)
            jr.session("Test", sinks=jr.Sinks(console="none"))
        self.assertIs(signal.getsignal(signal.SIGTERM), handler)

    def test_end_session_restores_the_sigterm_handler(self):
        previous = signal.signal(signal.SIGTERM, signal.SIG_DFL)
        self.addCleanup(signal.signal, signal.SIGTERM, previous)
        with mock.patch.dict(os.environ, {}, clear=False):
            os.environ.pop(jr.SESSION_ENV, None)
            jr.session("Test", sinks=jr.Sinks(console="none"))
            self.assertIs(signal.getsignal(signal.SIGTERM), jr._sigterm)
            jr._end_session()
        self.assertEqual(signal.getsignal(signal.SIGTERM), signal.SIG_DFL)


class WrapAndExpectedTest(unittest.TestCase):
    """Full-width continuation lines and the expected test total."""

    def test_every_row_of_a_wrapped_bar_but_the_last_holds_the_same_number_of_cells(self):
        line = jr._fit_row("Tests", ["#"] * 100, "100/100", "0:05", 40, wrap=True)
        lines = line.split("\n")
        counts = [l.count("#") for l in lines]
        self.assertGreater(len(lines), 2)
        self.assertEqual(set(counts[:-1]), {40 - len("Tests") - 2}, counts)
        self.assertLessEqual(counts[-1], counts[0])
        self.assertEqual(sum(counts), 100)

    def test_expected_total_shows_until_the_count_passes_it(self):
        self.assertEqual(jr.fmt_totals((41200, 0, 0, 0), expected=83000), "41,200 / ~83,000 tests")
        self.assertEqual(jr.fmt_totals((83051, 0, 0, 0), expected=83000), "83,051 tests")
        self.assertEqual(jr.fmt_totals((5, 2, 0, 0), expected=None), "5 tests, 2 failed")

    def test_running_row_shows_expected_tests_and_finished_row_drops_it(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        clock = Clock()
        est = jr.Estimate(wall_s=100.0, units=None, rate=None, runs=5, tests=1000)
        b = jr.Board("actionable", nested=False, full_log=None, clock=clock, estimator=lambda *a: est)
        b.apply("step_start", dict(id="s", n=1, title="py", parent=None, tool="pytest", sig=""))
        b.apply("tests", dict(step="s", total=400, fail=0, err=0, skip=0))
        clock.advance(2)
        self.assertIn("400 / ~1,000 tests", jr.row_cells(b.rows["s"], jr.PLAIN_GLYPHS, clock())[1])
        b.apply("step_end", dict(id="s", status="ok", ms=3000))
        self.assertNotIn("~1,000", jr.row_cells(b.rows["s"], jr.PLAIN_GLYPHS, clock())[1])

    def test_no_history_means_no_expected_total(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        clock = Clock()
        b = jr.Board("actionable", nested=False, full_log=None, clock=clock, estimator=lambda *a: None)
        b.apply("step_start", dict(id="s", n=1, title="py", parent=None, tool="pytest", sig=""))
        b.apply("tests", dict(step="s", total=400, fail=0, err=0, skip=0))
        clock.advance(2)
        self.assertEqual(jr.row_cells(b.rows["s"], jr.PLAIN_GLYPHS, clock())[1], "400 tests")


class DampedEstimateTest(unittest.TestCase):
    def test_rounding_steps(self):
        self.assertEqual(jr.round_estimate(32), 30)
        self.assertEqual(jr.round_estimate(100), 105)
        self.assertEqual(jr.round_estimate(700), 690)
        self.assertEqual(jr.round_estimate(1000), 990)

    def test_a_rounded_total_stays_above_elapsed(self):
        self.assertGreater(jr.round_estimate(31, floor=30), 30)

    def test_small_changes_keep_the_shown_total(self):
        self.assertEqual(jr.damp_total(472, 480, 100), 472)
        self.assertEqual(jr.damp_total(472, 472 + 47, 100), 472)       # within 10%
        self.assertEqual(jr.damp_total(40, 54, 10), 40)                # within 15s

    def test_a_large_change_or_an_overrun_moves_it(self):
        self.assertEqual(jr.damp_total(472, 600, 100), 600)
        self.assertEqual(jr.damp_total(100, 105, 100), 105)            # elapsed reached the shown total
        self.assertIsNone(jr.damp_total(100, None, 10))
        self.assertEqual(jr.damp_total(None, 472, 10), 465)


def step_end(step, status="ok", ms=2000, summary=None, totals=None, exit=None):
    return ("step_end", {"id": step, "status": status, "ms": ms, "exit": exit, "summary": summary, "totals": totals})


def step_start(step, title=None, n=1, parent=None):
    return ("step_start", {"id": step, "n": n, "title": title or step.title(), "parent": parent})


def note(level, text, step=None):
    return ("note", {"level": level, "text": text, "href": None, "step": step})


class RowCase(unittest.TestCase):
    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        self.clock = Clock()

    def board(self, detail="actionable", **kw):
        return jr.Board(detail, nested=kw.pop("nested", False), full_log=None, clock=self.clock, **kw)

    def finished(self, *events, detail="actionable"):
        """A board fed `events` then the row "a"'s end (ok unless an event set otherwise); returns (row, blocks)."""
        b = self.board(detail)
        blocks = feed(b, step_start("a"), *events)
        return b, b.rows["a"], blocks


class IconTest(RowCase):
    def test_paused_row_icon(self):
        b = self.board()
        feed(b, step_start("build", "Build"), note("pause", "Wait for the vote", step="build"), step_end("build", "ok"))
        row = b.rows["build"]
        self.assertEqual(jr.row_state(row), "paused")
        self.assertEqual(jr.row_icon(row, 0.0, False), jr.ICONS["paused"])
        self.assertEqual(jr._vlen(jr.ICONS["paused"]), 1)

    def test_spinner_frame_follows_clock(self):
        frames = [jr.icon("running", 100.0 + i * jr.LiveRenderer.REDRAW_SECONDS + 0.01) for i in range(11)]
        self.assertEqual("".join(frames[:10]), "⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏")
        self.assertEqual(frames[10], frames[0])
        self.assertEqual(jr.icon("running", 100.01, color=True), "\x1b[36m⠋\x1b[0m")       # cyan

    def test_finished_icons_coloured(self):
        for state, glyph, sgr in (("ok", "●", "32"), ("warn", "●", "33"), ("fail", "●", "31"), ("pending", "○", "2")):
            with self.subTest(state=state):
                self.assertEqual(jr.icon(state, color=True), f"\x1b[{sgr}m{glyph}\x1b[0m")

    def test_skip_icon_dim_single_column(self):
        self.assertEqual(jr.icon("skip", color=True), "\x1b[2m⊘\x1b[0m")
        self.assertEqual(jr._vlen("⊘"), 1)
        self.assertNotIn("️", "".join(jr.ICONS.values()))                       # never with VS16

    def test_no_color_same_glyphs(self):
        for state, glyph in jr.ICONS.items():
            with self.subTest(state=state):
                self.assertEqual(jr.icon(state), glyph)

    def test_plain_and_log_static_icon(self):
        b = self.board()
        blocks = feed(b, step_start("a"), step_end("a"), step_start("b"), step_end("b", "skip", summary="(skipped)"))
        self.assertEqual(plain(b, blocks, self.clock), "✔ A           0:02\n⊘ B           skipped  0:02\n".replace(
            "A           ", "A".ljust(10) + "  ").replace("B           ", "B".ljust(10) + "  "))
        self.assertNotIn("\x1b[", plain(b, blocks, self.clock))


class RowStatusTest(RowCase):
    def test_warn_note_flips_ok_row_to_warn(self):
        b, r, _ = self.finished(note("warn", "13 over baseline"), step_end("a"))
        self.assertEqual((r.status, jr.row_state(r), jr.row_icon(r, 0, False)), ("ok", "warn", "⚠"))

    def test_error_note_on_ok_row_is_warn(self):
        b, r, _ = self.finished(note("error", "flaky"), step_end("a"))
        self.assertEqual(jr.row_state(r), "warn")

    def test_fail_beats_warn(self):
        b, r, _ = self.finished(note("warn", "careful"), step_end("a", "fail", exit=1))
        self.assertEqual(jr.row_state(r), "fail")

    def test_every_skip_is_skip_icon(self):
        for summary in ("(skipped)", "(done by Tests)", "(done earlier)", None):
            with self.subTest(summary=summary):
                b, r, blocks = self.finished(step_end("a", "skip", summary=summary))
                self.assertEqual(jr.row_icon(r, 0, False), "⊘")
                if summary:
                    self.assertIn(summary.strip("()"), plain(b, blocks, self.clock))
                    self.assertNotIn("(", plain(b, blocks, self.clock))

    def test_warn_on_skipped_row_keeps_skip_icon(self):
        b, r, blocks = self.finished(note("warn", "no docs checkout"), step_end("a", "skip", summary="(skipped)"))
        self.assertEqual((jr.row_icon(r, 0, False), r.reason), ("⊘", "no docs checkout"))
        self.assertIn("no docs checkout", plain(b, blocks, self.clock))

    def test_detail_note_shows_beside_the_result_and_keeps_the_status(self):
        b, r, blocks = self.finished(note("detail", "3 retried"), step_end("a", summary="12 files"))
        out = plain(b, blocks, self.clock)
        self.assertIn("12 files  3 retried", out)
        self.assertEqual((r.status, jr.row_state(r), r.reason), ("ok", "ok", None))

    def test_detail_note_does_not_replace_a_warn_reason_or_a_failure(self):
        b, r, blocks = self.finished(note("detail", "extra"), note("warn", "careful"), step_end("a"))
        self.assertEqual(r.reason, "careful")
        self.assertIn("careful", plain(b, blocks, self.clock))

    def test_newest_warn_is_reason(self):
        b, r, blocks = self.finished(note("warn", "first"), note("warn", "second\nmore"), step_end("a"))
        self.assertEqual((r.reason, r.reason_level), ("second", "warn"))
        self.assertIn("⚠ " + "A".ljust(10) + "  second", plain(b, blocks, self.clock))

    def test_error_reason_not_replaced_by_warn(self):
        b, r, _ = self.finished(note("error", "broke"), note("warn", "later"), step_end("a", "fail", exit=1))
        self.assertEqual((r.reason, r.reason_level), ("broke", "error"))

    def test_reason_cut_before_bar(self):
        b = self.board()
        feed(b, step_start("a"), ("units", {"step": "a", "total": 5, "kind": "checks"}),
             *[("unit", {"step": "a", "done": i + 1, "outcome": "pass"}) for i in range(5)],
             note("warn", "a reason far too long to fit on the row beside its bar " * 3))
        live = jr.LiveRenderer(lambda t: None, size=lambda: os.terminal_size((60, 10)), clock=self.clock, color=False)
        line = live._row(b, b.rows["a"], 59, pending=True)
        self.assertNotIn("\n", line)
        self.assertLessEqual(jr._vlen(line), 59)
        self.assertIn("█████  5/5  a reason", line)                       # every cell kept
        self.assertIn("…", line)

    def test_failed_modules_reason_first_plus_n(self):
        b = self.board()
        events = [step_start("a", "Build"), ("reactor", {"step": "a", "size": 3, "poms": [], "tests": False})]
        for name, status in (("x", "fail"), ("y", "ok"), ("z", "fail")):
            events += [("unit_start", {"step": "a", "id": name, "title": f"Mod {name.upper()}"}),
                       ("unit_end", {"step": "a", "id": name, "status": status, "ms": 1, "totals": None})]
        blocks = feed(b, *events, step_end("a", "fail", exit=1))
        self.assertIn("2 failed: Mod X +1", plain(b, blocks, self.clock))

    def test_tests_row_reason_appends_failed_module(self):
        b = self.board()
        blocks = feed(b, *maven_events(), step_end("tests", "fail", exit=1,
                                                    totals={"total": 7149, "fail": 2, "err": 0, "skip": 0}))
        self.assertIn("7,149 tests, 2 failed: Mod B  ", plain(b, blocks, self.clock))


class HintTest(RowCase):
    def test_fail_prints_hint_lines_beneath_row(self):
        b, r, blocks = self.finished(("hint", {"step": "a", "lines": ["run this:", "  git pull"]}))
        blocks += feed(b, step_end("a", "fail", exit=1))
        self.assertEqual(plain(b, blocks, self.clock).splitlines()[1:], ["    run this:", "      git pull"])

    def test_failure_text_held_until_row_ends(self):
        b, r, blocks = self.finished(("failure", {"text": "Bad.java:[1,2] oops", "step": "a"}))
        self.assertEqual(blocks, [])
        blocks = feed(b, step_end("a", "fail", exit=1))
        self.assertEqual(plain(b, blocks, self.clock).splitlines()[1:], ["    Bad.java:[1,2] oops"])

    def test_hint_dedup_with_failure_lines(self):
        b, r, _ = self.finished(("failure", {"text": "same line", "step": "a"}),
                                ("hint", {"step": "a", "lines": ["same line", "new line"]}))
        blocks = feed(b, step_end("a", "fail", exit=1))
        self.assertEqual(plain(b, blocks, self.clock).splitlines()[1:], ["    same line", "    new line"])

    def test_session_fail_hint_reaches_screen_at_actionable(self):
        import scripts_support  # noqa: F401 - puts juneau_terminal_scripts on sys.path
        from juneau_terminal_scripts import run
        out = io.StringIO()
        with mock.patch.dict(os.environ, {"TERMINAL_MARKERS": ""}), mock.patch.dict(sys.modules, {"juneau_terminal": jr}):
            jr._console = jr.subscribe(jr.Console(jr.Sinks(console="board"), stream=out))
            self.assertEqual(jr._console.board.detail, "actionable")
            with self.assertRaises(run._SessionFailed):
                run.Session().fail("❌ No.", hint=["   try this"])
            jr._console.close()
        self.assertIn("   try this", out.getvalue())

    def test_hint_without_running_row_prints_at_once(self):
        b = self.board()
        blocks = feed(b, ("hint", {"step": None, "lines": ["do this"]}))
        self.assertEqual(plain(b, blocks, self.clock), "do this\n")


class FullLogNotesTest(RowCase):
    def test_say_note_hint_failure_appended_to_full_log(self):
        with tempfile.TemporaryDirectory() as tmp:
            full = Path(tmp) / "full.log"
            c = jr.Console(jr.Sinks(console="none", full_log=str(full)), stream=io.StringIO(), clock=self.clock)
            c("step_start", {"id": "a", "n": 1, "title": "A", "parent": None})
            c("say", {"text": "said", "level": "info", "step": None})
            c("note", {"level": "warn", "text": "noted", "href": None, "step": "a"})
            c("hint", {"step": "a", "lines": ["hint one", "hint two"]})
            c("failure", {"text": "failed text", "step": "a"})
            c.close()
            text = full.read_text(encoding="utf-8")
        self.assertEqual(text.splitlines()[-5:], ["said", "⚠ noted", "hint one", "hint two", "failed text"])


class PendingCase(RowCase):
    def live(self, rows=20, cols=80):
        return jr.LiveRenderer(lambda t: None, size=lambda: os.terminal_size((cols, rows)), clock=self.clock,
                               color=False)

    def planned(self, plan, **kw):
        b = self.board(**kw)
        feed(b, ("plan", {"ids": [e if isinstance(e, str) else e[0] for e in plan],
                          "labels": {e[0]: e[1] for e in plan if not isinstance(e, str)}}))
        return b


class PendingTest(PendingCase):
    PLAN = [("pre", "Prechecks"), ("tests", "Tests"), "hidden", ("install", "Install"), ("commit", "Commit")]

    def test_labelled_plan_draws_pending_rows(self):
        b = self.planned(self.PLAN)
        feed(b, step_start("pre", "Prechecks"))
        region = self.live().region(b, 79, 18)
        self.assertEqual(region[-3:], ["○ Tests", "○ Install", "○ Commit"])

    def test_unlabelled_ids_count_but_do_not_draw(self):
        b = self.planned(self.PLAN)
        feed(b, step_start("pre", "Prechecks"))
        self.assertNotIn("○ hidden", self.live().region(b, 79, 18))
        self.assertEqual(b.plan, ["pre", "tests", "hidden", "install", "commit"])

    def test_started_row_replaces_pending(self):
        b = self.planned(self.PLAN)
        feed(b, step_start("pre", "Prechecks"), step_end("pre"), step_start("tests", "Tests"))
        region = self.live().region(b, 79, 18)
        self.assertNotIn("○ Tests", region)
        self.assertTrue(any(line[2:].startswith("Tests") for line in region), region)

    def test_passed_over_pending_not_drawn(self):
        b = self.planned(self.PLAN)
        feed(b, step_start("install", "Install"))                   # Prechecks and Tests were passed over
        self.assertEqual([line for line in self.live().region(b, 79, 18) if line.startswith("○")], ["○ Commit"])

    def test_width_includes_pending_labels(self):
        b = self.planned([("a", "A"), ("long", "A much longer label")])
        feed(b, step_start("a", "A"))
        self.assertEqual(b.width(), len("A much longer label"))

    def test_no_plan_unchanged(self):
        b = self.board()
        feed(b, step_start("a", "A"))
        self.assertEqual(b.pending(), [])
        self.assertEqual(len(self.live().region(b, 79, 18)), 1)


class OverflowTest(PendingCase):
    def busy(self):
        b = self.planned([("tests", "Tests")] + [(f"p{i}", f"Pending {i}") for i in range(6)])
        feed(b, *start(size=4), *[mod_start(n) for n in "abcd"], *[cls(n) for n in "abcd"])
        return b

    def test_a_short_pane_keeps_the_running_rows_and_elides_the_pending_tail(self):
        full = self.live().region(self.busy(), 79)
        self.assertEqual(len(full), 11)                     # the row, four module lines, six pending rows
        running, pending = full[:5], full[5:]
        for rows in range(1, 12):
            with self.subTest(rows=rows):
                got = self.live().region(self.busy(), 79, rows)
                self.assertLessEqual(len(got), rows)
                if rows <= len(running):                    # not even the running rows fit: the last of them
                    self.assertEqual(got, running[-rows:])
                    continue
                room = rows - len(running)
                self.assertEqual(got[:len(running)], running)
                if room >= len(pending):
                    self.assertEqual(got, full)
                else:                                       # the pending tail gives way to "... N more steps"
                    self.assertEqual(got[len(running):-1], pending[:room - 1])
                    self.assertEqual(got[-1], f"... {len(pending) - room + 1} more steps")

    def test_rows_run_on_two_running_rows_stay_under_their_own_row(self):
        b = self.planned([("one", "One"), ("two", "Two")])
        feed(b, *start("one", size=2, title="One"), *start("two", size=2, title="Two"))
        for step, name in (("one", "a"), ("two", "c"), ("one", "b"), ("two", "d")):     # started in this order
            self.clock.advance(1)
            feed(b, mod_start(name, step))
        self.assertEqual([line.split()[:2] for line in self.live().region(b, 79, 5)],     # only One scrolled off
                         [["Mod", "A"], ["Mod", "B"], ["⠋", "Two"], ["Mod", "C"], ["Mod", "D"]])


class ModuleLinesTest(RowCase):
    def test_no_finished_module_lines_at_actionable(self):
        for detail in ("summary", "actionable"):
            with self.subTest(detail=detail):
                b = self.board(detail)
                self.assertEqual([blk for blk in feed(b, *maven_events()) if blk[0] == "module"], [])

    def test_modules_detail_keeps_lines(self):
        b = self.board("modules")
        blocks = feed(b, *maven_events())
        self.assertIn("    F Mod B  0:02  10 tests, 2 failed\n", plain(b, blocks, self.clock))

    def test_inflight_sub_line_ends_with_its_elapsed_time(self):
        b = self.board()
        feed(b, *start(), mod_start("a"), cls("a"), cls("a", "fail"))
        live = jr.LiveRenderer(lambda t: None, size=lambda: os.terminal_size((60, 10)), clock=self.clock, color=False)
        self.assertEqual(live.region(b, 59, 8)[1:], ["    Mod A  •F".ljust(59 - 4) + "0:00"])


class SetupRowTest(RowCase):
    def events(self, pom_status="ok"):
        return [step_start("tests", "Tests"),
                ("reactor", {"step": "tests", "size": 3, "poms": ["p0", "p1"], "tests": True}),
                ("unit_start", {"step": "tests", "id": "p0", "title": "Parent"}),
                ("unit_end", {"step": "tests", "id": "p0", "status": "ok", "ms": 1, "totals": None}),
                ("unit_start", {"step": "tests", "id": "p1", "title": "Bom"}),
                ("unit_end", {"step": "tests", "id": "p1", "status": pom_status, "ms": 1, "totals": None})]

    def test_setup_finishes_when_poms_done(self):
        b = self.board()
        blocks = feed(b, *self.events()[:-2])
        self.assertNotIn(("setup",), blocks)
        blocks = feed(b, *self.events()[-2:])      # a pom's box is set when it starts: a pom has no work to wait for
        self.assertEqual(blocks, [("setup",)])
        self.assertTrue(b.setup.ended)
        self.assertEqual(plain(b, blocks, self.clock), "✔ " + "Setup".ljust(10) + "  ##  2 pom modules\n")

    def test_setup_fails_on_pom_failure(self):
        b = self.board()
        out = []
        renderer = jr.PlainRenderer(out.append, clock=self.clock)
        for event in self.events("fail"):
            renderer.render(b, feed(b, event))
        # green at the last pom's start, then printed again red when that pom fails
        self.assertEqual("".join(out), "✔ " + "Setup".ljust(10) + "  ##  2 pom modules\n"
                         + "✗ " + "Setup".ljust(10) + "  #F  1 failed: Bom\n")

    def test_setup_ends_with_owner(self):
        b = self.board()
        feed(b, *self.events()[:4])                                      # p1 never reports
        blocks = feed(b, step_end("tests", "fail", exit=1))
        self.assertEqual(blocks[0], ("setup",))
        self.assertEqual(blocks[1], ("row", b.rows["tests"]))
        self.assertTrue(b.setup.ended)
        self.assertEqual(feed(b, step_start("next")), [])               # a later row does not reopen it


class StickyHeaderTest(unittest.TestCase):
    HEADER = [["head", "Push: Juneau · master", False], ["more", "Commit Message: m", True],
              ["more", "Output: tail -f /o", False]]

    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)

    def nested_console(self, out, value, color=False):
        env = {"TERM": "xterm", "TERMINAL_MARKERS": "", "JUNEAU_TERMINAL_HEADER": value}
        if not color:
            env["NO_COLOR"] = "1"
        with mock.patch.dict(os.environ, env):
            if color:
                os.environ.pop("NO_COLOR", None)
            console = jr.Console(jr.Sinks(console="board"), stream=out, size=lambda: os.terminal_size((80, 24)),
                                 nested=True)
        self.addCleanup(console.close)
        return console

    def test_the_parent_header_parses_into_header_blocks(self):
        with mock.patch.dict(os.environ, {"JUNEAU_TERMINAL_HEADER": json.dumps(self.HEADER)}):
            self.assertEqual(jr._parent_header(), [("header", "Push: Juneau · master", jr.HEAD_MARK),
                                                   ("header", "Commit Message: m", jr.MORE_MARK, "cut"),
                                                   ("header", "Output: tail -f /o", jr.MORE_MARK)])

    def test_a_missing_or_malformed_value_is_no_sticky_header_silently(self):
        for value in ("", "{", "[[1]]", '"x"', "[[\"head\"]]"):
            with self.subTest(value=value), mock.patch.dict(os.environ, {"JUNEAU_TERMINAL_HEADER": value}):
                self.assertEqual(jr._parent_header(), [])

    def test_a_nested_live_console_draws_it_on_top_and_never_prints_it(self):
        out = TTY()
        console = self.nested_console(out, json.dumps(self.HEADER))
        console("step_start", {"id": "t", "n": 1, "title": "Tests", "parent": None})
        self.assertTrue(jr._ANSI.sub("", out.getvalue()).lstrip("\r").startswith("▶ Push: Juneau · master"))
        console.pause()
        console.resume()
        console("step_end", {"id": "t", "status": "ok", "ms": 0, "exit": None, "summary": None, "totals": None})
        console.close()
        screen = Screen(rows=24, cols=80)
        screen.feed(out.getvalue())
        self.assertNotIn("Push: Juneau", screen.history())      # drawn live only: not in scrollback after close

    def test_under_colour_the_sticky_title_is_cyan(self):
        out = TTY()
        console = self.nested_console(out, json.dumps(self.HEADER), color=True)
        console("step_start", {"id": "t", "n": 1, "title": "Tests", "parent": None})
        self.assertIn("\x1b[36m▶\x1b[0m Push: Juneau · master", out.getvalue())

    def test_handoff_exports_this_sessions_header_or_its_own_sticky_one(self):
        console = mock.Mock(mode="live", header_blocks=[("header", "Push: Juneau · master", jr.HEAD_MARK),
                                                        ("header", "Commit Message: m", jr.MORE_MARK, "cut")],
                            sticky=[])
        console.board.pending.return_value = []
        console.board.overall_handoff.return_value = None
        with mock.patch.object(jr, "_console", console), jr.handoff() as env:
            self.assertEqual(json.loads(env["JUNEAU_TERMINAL_HEADER"]), [["head", "Push: Juneau · master", False],
                                                                   ["more", "Commit Message: m", True]])
        console.header_blocks, console.sticky = [], [("header", "Push: Juneau", jr.HEAD_MARK)]
        with mock.patch.object(jr, "_console", console), jr.handoff() as env:
            self.assertEqual(json.loads(env["JUNEAU_TERMINAL_HEADER"]), [["head", "Push: Juneau", False]])

    def test_handoff_reads_the_child_outcomes_and_removes_the_file(self):
        board = jr.Board(clock=Clock())
        board.apply("plan", {"ids": ["build", "install"], "labels": {"build": "Compile", "install": "Install"}})
        console = mock.Mock(mode="live", board=board, header_blocks=[], sticky=[])
        with mock.patch.object(jr, "_console", console), jr.handoff() as env:
            path = Path(env["JUNEAU_TERMINAL_HANDBACK"])
            path.write_text(json.dumps([["build", "ok", "Compile"]]))
        self.assertEqual(board.handed_back, {"build": "ok"})
        self.assertFalse(path.exists())
        console.log_only.assert_not_called()

    def test_a_handoff_ended_by_an_exception_still_counts_but_never_notes(self):
        for error in (KeyboardInterrupt, RuntimeError):
            with self.subTest(error=error.__name__):
                board = jr.Board(clock=Clock())
                board.apply("plan", {"ids": ["build"], "labels": {"build": "Compile"}})
                console = mock.Mock(mode="live", board=board, header_blocks=[], sticky=[])
                with self.assertRaises(error), mock.patch.object(jr, "_console", console), jr.handoff() as env:
                    Path(env["JUNEAU_TERMINAL_HANDBACK"]).write_text(json.dumps([["build", "fail", "Compile"]]))
                    raise error()
                self.assertEqual(board.handed_back, {"build": "fail"})
                with self.assertRaises(error), mock.patch.object(jr, "_console", console), jr.handoff():
                    raise error()                                       # Ctrl-C before the child wrote anything
                console.log_only.assert_not_called()

    def test_a_missing_or_malformed_handback_counts_nothing_and_notes_once(self):
        for text in ("", "{", '[["build", "ok"]]'):
            with self.subTest(text=text):
                board = jr.Board(clock=Clock())
                console = mock.Mock(mode="live", board=board, header_blocks=[], sticky=[])
                with mock.patch.object(jr, "_console", console), jr.handoff() as env:
                    Path(env["JUNEAU_TERMINAL_HANDBACK"]).write_text(text)
                self.assertEqual(board.handed_back, {})
                console.log_only.assert_called_once()


class NestedPendingTest(PendingCase):
    def test_handoff_exports_pending(self):
        b = self.planned([("pre", "Prechecks"), ("tests", "Tests"), ("install", "Install")])
        feed(b, step_start("pre", "Prechecks"), step_end("pre"), step_start("tests", "Tests"))
        self.assertEqual(b.pending(), [("install", "Install")])
        console = mock.Mock(mode="live", board=b, header_blocks=[], sticky=[])
        with mock.patch.object(jr, "_console", console), jr.handoff() as env:
            self.assertEqual(json.loads(env["JUNEAU_TERMINAL_PENDING"]), [["install", "Install"]])

    def test_child_draws_parent_pending_dedup(self):
        pending = json.dumps([["install", "Install"], ["commit", "Commit"]])
        with mock.patch.dict(os.environ, {"JUNEAU_TERMINAL_PENDING": pending}):
            b = self.planned([("install", "Install")], nested=True)
        feed(b, step_start("compile", "Compile"))
        self.assertEqual(b.pending(), [("install", "Install"), ("commit", "Commit")])
        self.assertEqual([line for line in self.live().region(b, 79, 18) if line.startswith("○")],
                         ["○ Install", "○ Commit"])
        feed(b, step_end("compile"), ("done", {"status": "ok", "commit": None}))
        self.assertEqual(b.pending(), [])


class HandBackTest(unittest.TestCase):
    PLAN = [("pre", "Prechecks"), ("build", "Compile"), ("tests", "Java Tests"), ("js-tests", "Browser Tests"),
            ("install", "Install")]

    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        self.clock = Clock()

    def parent(self):
        b = jr.Board(clock=self.clock)
        b.apply("plan", {"ids": [i for i, _ in self.PLAN], "labels": dict(self.PLAN)})
        feed(b, step_start("pre", "Prechecks"), step_end("pre"))
        return b

    def test_handed_back_ids_count_as_done_skips_are_left_out_and_no_row_is_drawn(self):
        b = self.parent()
        b.hand_back([("build", "ok", "Compile"), ("tests", "warn", "Java Tests"), ("js-tests", "skip", "Browser Tests")])
        feed(b, step_start("install", "Install"))
        self.assertEqual(set(b.rows), {"pre", "install"})
        self.assertEqual(b.pending(), [])
        feed(b, step_end("install"))
        self.assertEqual(b.step_counts(), (4, 4))

    def test_a_handed_back_fail_sets_the_failed_title_and_is_not_counted_done(self):
        b = self.parent()
        b.hand_back([("build", "ok", "Compile"), ("tests", "fail", "Tests")])
        self.assertEqual(b.failed_title, "Java Tests")                  # the parent's own label wins
        self.assertEqual(b.step_counts(), (2, 5))

    def test_a_fail_the_parent_has_no_label_for_takes_the_childs_label_not_the_id(self):
        b = self.parent()
        b.hand_back([("py-x", "fail", "Python Tests")])
        self.assertEqual(b.failed_title, "Python Tests")

    def test_an_id_with_a_row_keeps_its_row(self):
        b = self.parent()
        b.hand_back([("pre", "fail", "Prechecks")])
        self.assertIsNone(b.failed_title)
        self.assertEqual(b.handed_back, {})

    def nested(self, path):
        return mock.patch.dict(os.environ, {"JUNEAU_TERMINAL_HANDBACK": str(path), "TERMINAL_MARKERS": ""})

    def test_a_nested_console_writes_its_top_level_rows_outcomes_at_done(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "hb.json"
            with self.nested(path):
                console = jr.Console(jr.Sinks(console="none"), nested=True)
                for kind, fields in (step_start("build", "Compile"), step_end("build"),
                                     step_start("js-tests", "Browser Tests"), step_end("js-tests", "skip"),
                                     step_start("tests", "Java Tests"),
                                     ("step_start", {"id": "tests/core", "n": 1, "title": "core", "parent": "tests"})):
                    console(kind, fields)
                console("done", {"status": "fail", "commit": None})
            self.assertEqual(json.loads(path.read_text()), [["build", "ok", "Compile"], ["js-tests", "skip", "Browser Tests"],
                                                            ["tests", "fail", "Java Tests"]])     # no sub-row

    def test_a_nested_console_takes_the_handback_path_out_of_the_environment(self):
        with tempfile.TemporaryDirectory() as tmp, self.nested(Path(tmp) / "hb.json"):
            jr.Console(jr.Sinks(console="none"), nested=True)
            self.assertNotIn("JUNEAU_TERMINAL_HANDBACK", os.environ)     # a grandchild session must not write our file

    def test_a_nested_console_hands_back_after_its_board_failed(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "hb.json"
            with self.nested(path):
                console = jr.Console(jr.Sinks(console="none"), nested=True)
                for kind, fields in (step_start("build", "Compile"), step_end("build")):
                    console(kind, fields)
                with mock.patch.object(console.board, "apply", side_effect=RuntimeError("board")):
                    console(*step_start("tests", "Java Tests"))
                console("done", {"status": "fail", "commit": None})
            self.assertEqual(json.loads(path.read_text()), [["build", "ok", "Compile"]])

    def test_a_planless_nested_session_under_a_parent_overall_notes_it_in_the_board_log_only(self):
        overall = json.dumps({"elapsed": 5.0, "done": 1, "pending": [["install", 30.0]]})
        with tempfile.TemporaryDirectory() as tmp:
            log = Path(tmp) / "c.log"
            out = io.StringIO()
            with mock.patch.dict(os.environ, {"JUNEAU_TERMINAL_OVERALL": overall, "TERMINAL_MARKERS": ""}):
                console = jr.Console(jr.Sinks(console="board", board_log=str(log)), stream=out, nested=True)
                self.assertIsNone(console.board.overall())                  # pinned: no plan draws no Overall
                console("done", {"status": "ok", "commit": None})
                console.close()
            self.assertEqual(log.read_text().count("declared no plan"), 1)
            self.assertNotIn("declared no plan", out.getvalue())


class LogTest(RowCase):
    def run_log(self, *events):
        with tempfile.TemporaryDirectory() as tmp:
            log = Path(tmp) / "c.log"
            c = jr.Console(jr.Sinks(console="none", board_log=str(log)), stream=io.StringIO(), clock=self.clock)
            for kind, fields in events:
                c(kind, fields)
            c.close()
            return log.read_text(encoding="utf-8")

    def test_log_keeps_note_text_beneath_row(self):
        text = self.run_log(step_start("a", "A"), note("warn", "first line\nsecond line", "a"), step_end("a"))
        self.assertIn("⚠ " + "A".ljust(10) + "  first line", text)
        self.assertIn("    ⚠ first line\n      second line\n", text)

    def test_log_has_no_pending_rows(self):
        text = self.run_log(("plan", {"ids": ["a", "b"], "labels": {"a": "A", "b": "B"}}), step_start("a", "A"),
                            step_end("a"))
        self.assertNotIn("○", text)


class CompileOnlyMavenRowTest(unittest.TestCase):
    """A build whose goals stop before Surefire never reaches a module's tests, so its modules count at their end."""

    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        self.board = jr.Board("actionable", nested=False, full_log=None, clock=lambda: 100.0)
        jr.subscribe(self.board.apply)

    def build(self, args):
        from test_parsers import write_demo_tree
        from support import FIXTURES
        log = (FIXTURES / "maven-serial-success.log").read_text(encoding="utf-8")
        log = re.sub(r"\[INFO\] --- surefire:.*?\n(?=\[INFO\] -+< |\[INFO\] -+\n\[INFO\] Reactor Summary)", "", log,
                     flags=re.S)    # no test phase
        with tempfile.TemporaryDirectory() as tmp:
            write_demo_tree(Path(tmp), packaging="pom")
            for m in "abc":
                (Path(tmp) / f"mod-{m}" / "src" / "test" / "java").mkdir(parents=True)
            parser = jr.MavenParser()
            parser.step_id = "s"
            parser.prepare(["mvn", *args], None, tmp)
            d = jr._Dispatcher("s", jr.Sinks(console="none"))
            jr.publish("step_start", id="s", n=1, title="Compile", parent=None, tool="maven", sig="s")
            for line in log.splitlines():
                d.apply(parser.on_line(line))
            d.apply(parser.finish(0))
            jr.publish("step_end", id="s", status="ok", ms=13000, exit=0, summary=None, totals=None)
        return self.board.rows["s"]

    def test_a_finished_test_compile_row_counts_every_module_and_keeps_its_bar(self):
        row = self.build(["test-compile"])
        self.assertEqual((row.units_total, row.units_done), (3, 3))
        cells, _, text, _, _ = jr.row_text(row, jr.PLAIN_GLYPHS, 100.0)
        self.assertEqual("".join(cells), jr.PLAIN_GLYPHS["pass"] * 3)
        self.assertEqual(text, "")

    def test_a_clean_compile_row_counts_every_module_too(self):
        row = self.build(["-pl", "mod-a,mod-b,mod-c", "clean", "compile"])
        self.assertEqual((row.units_total, row.units_done), (3, 3))
        self.assertNotIn("no tests run", jr.row_text(row, jr.PLAIN_GLYPHS, 100.0)[2])


class ScreenTest(unittest.TestCase):
    """The viewport mode of support.Screen, which the fit tests rely on."""

    def test_a_newline_on_the_last_row_scrolls_into_scrollback(self):
        s = Screen(rows=3, cols=10)
        s.feed("a\nb\nc\nd")
        self.assertEqual((s.scrollback, s.lines), (["a"], ["b", "c", "d"]))

    def test_cursor_up_clamps_at_the_top_row(self):
        s = Screen(rows=3, cols=10, start_row=2)
        s.feed("\x1b[3A")
        self.assertEqual((s.row, s.clamped), (0, True))

    def test_a_long_line_autowraps_at_cols(self):
        s = Screen(rows=5, cols=4)
        s.feed("abcdef\n")
        self.assertEqual(s.lines[:2], ["abcd", "ef"])

    def test_a_wide_character_takes_two_columns(self):
        s = Screen(rows=5, cols=4)
        s.feed("🧱🧱x")
        self.assertEqual(s.lines[:2], ["🧱🧱", "x"])

    def test_without_a_viewport_it_behaves_as_before(self):
        s = Screen()
        s.feed("ab\ncd\x1b[1A\rX\x1b[K")
        self.assertEqual(s.text(), "X\ncd")
        self.assertEqual(s.scrollback, [])

    def test_history_is_scrollback_then_the_screen(self):
        s = Screen(rows=2, cols=10)
        s.feed("a\nb\nc")
        self.assertEqual(s.history(), "a\nb\nc")

    def test_a_newline_after_a_full_width_line_does_not_add_a_blank_line(self):
        s = Screen(rows=5, cols=4)
        s.feed("abcd\nef")
        self.assertEqual(s.lines[:2], ["abcd", "ef"])

    def test_resize_changes_the_viewport_without_reflowing(self):
        s = Screen(rows=5, cols=10)
        s.feed("abcdefgh\nx")
        s.resize(rows=3, cols=4)
        self.assertEqual((s.rows, s.cols, s.lines[0]), (3, 4, "abcdefgh"))     # a non-reflowing terminal

    # The tmux reflow cases below were checked against tmux 3.7c (a detached pane, resize-window, then cursor_y,
    # history_size and capture-pane).

    def test_narrowing_like_tmux_keeps_the_cursor_row_and_pushes_the_top_into_scrollback(self):
        s = Screen(rows=10, cols=40)
        s.feed("A" * 39 + "\n" + "B" * 39 + "\n" + "CCCCC\n")
        s.resize_like_tmux(cols=20)
        self.assertEqual((s.row, s.scrollback), (3, ["A" * 20, "A" * 19]))      # blank rows below do not absorb it
        self.assertEqual(s.lines[:3], ["B" * 20, "B" * 19, "CCCCC"])

    def test_widening_like_tmux_pulls_the_lines_back_from_scrollback(self):
        s = Screen(rows=10, cols=40)
        s.feed("A" * 39 + "\n" + "B" * 39 + "\n" + "CCCCC\n")
        s.resize_like_tmux(cols=20)
        s.resize_like_tmux(cols=40)
        self.assertEqual((s.row, s.scrollback, s.lines[:3]), (3, [], ["A" * 39, "B" * 39, "CCCCC"]))

    def test_widening_like_tmux_with_no_scrollback_moves_the_cursor_up(self):
        s = Screen(rows=10, cols=20)
        s.feed("A" * 30 + "\nBBBBB\n")
        s.resize_like_tmux(cols=40)
        self.assertEqual((s.row, s.lines[:2]), (2, ["A" * 30, "BBBBB"]))

    def test_erasing_below_from_the_top_left_corner_scrolls_the_screen_into_scrollback_first(self):
        s = Screen(rows=5, cols=10)
        s.feed("a\nb\n\x1b[H\x1b[J")
        self.assertEqual((s.scrollback, s.text()), (["a", "b"], ""))
        s = Screen(rows=5, cols=10)
        s.feed("a\nb\n\x1b[H\x1b[K\x1b[B\x1b[J\x1b[A")      # the first line on its own, then the rest
        self.assertEqual((s.scrollback, s.text(), s.row), ([], "", 0))

    def test_written_spaces_reflow_and_a_newline_ended_line_is_never_joined(self):
        s = Screen(rows=10, cols=40)
        s.feed("X" + " " * 30 + "\nY\n")
        s.resize_like_tmux(cols=20)
        self.assertEqual((s.row, len(s.scrollback)), (2, 1))
        s = Screen(rows=10, cols=10)
        s.feed("abc\ndef\n")
        s.resize_like_tmux(cols=20)
        self.assertEqual(s.lines[:2], ["abc", "def"])


class HeaderTest(unittest.TestCase):
    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        self.clock = Clock()

    def blocks(self, title, header=(), lines=(), more=()):
        return jr.Board(clock=self.clock).apply("session", {"title": title, "header": list(header), "lines": list(lines),
                                                            "header_more": list(more), "nested": False})

    def test_items_join_with_a_middle_dot_and_lines_are_cut_blocks(self):
        self.assertEqual(self.blocks("Push", ["Juneau", "master"], ["Commit Message: Fix it"], ["Output: tail -f /l"]), [
            ("header", "Push: Juneau · master", jr.HEAD_MARK),
            ("header", "Commit Message: Fix it", jr.MORE_MARK, "cut"),
            ("header", "Output: tail -f /l", jr.MORE_MARK)])

    def test_a_multi_line_entry_shows_its_first_line_and_an_ellipsis(self):
        self.assertEqual(self.blocks("Push", ["Juneau"], ["Commit Message: Fix it\n\nBody"])[1][1],
                         "Commit Message: Fix it …")

    def test_no_items_is_the_bare_title_and_no_lines_is_no_line_two(self):
        self.assertEqual(self.blocks("Test"), [("header", "Test", jr.HEAD_MARK)])

    def test_the_live_screen_cuts_the_title_and_the_cut_lines(self):
        title = ("header", "Push: Juneau · master · " + "x" * 80, jr.HEAD_MARK)
        line = ("header", "Commit Message: " + "y" * 80, jr.MORE_MARK, "cut")
        log = ("header", "Output: tail -f /" + "z" * 80, jr.MORE_MARK)
        for cols in (40, 60, 120):
            with self.subTest(cols=cols):
                for block in (title, line):
                    shown = jr.header_lines(block, cols, live=True)
                    self.assertEqual(len(shown), 1)
                    self.assertLessEqual(jr._vlen(shown[0]), cols)
                    self.assertTrue(shown[0].endswith("…") or jr._vlen(block[1]) + 2 <= cols)
                self.assertEqual(jr.header_lines(log, cols, live=True), ["▶ " + log[1]])   # a log line is never cut

    def test_the_plain_screen_never_cuts_and_still_wraps_the_title(self):
        line = ("header", "Commit Message: " + "y" * 80, jr.MORE_MARK, "cut")
        self.assertEqual(jr.header_lines(line, 40), ["▶ " + line[1]])
        self.assertGreater(len(jr.header_lines(("header", "Push: " + "w " * 40, jr.HEAD_MARK), 40)), 1)

    def test_an_emoji_title_is_cut_by_display_width(self):
        shown = jr.header_lines(("header", "Push: SSC · future · 🧱 Sandbox mode", jr.HEAD_MARK), 21, live=True)[0]
        self.assertLessEqual(jr._vlen(shown), 21)

    def test_session_publishes_lines(self):
        seen = []
        jr.subscribe(lambda kind, fields: seen.append((kind, fields)))
        with mock.patch.object(jr, "Console", side_effect=lambda *a, **k: mock.MagicMock()):
            jr.session("Push", ["Juneau"], lines=["Commit Message: m"], sinks=jr.Sinks(console="none"))
        fields = [f for k, f in seen if k == "session"][0]
        self.assertEqual(fields["lines"], ["Commit Message: m"])


class HandoffViewTest(unittest.TestCase):
    """A live parent hands off to a nested live child on the same terminal: one board, the header once, every row in
    order, whether the child draws the parent's rows above its own or (an older child) prints its own."""

    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        patcher = mock.patch.dict(os.environ, {"TERM": "xterm", "NO_COLOR": "1", "TERMINAL_MARKERS": ""})
        patcher.start()
        self.addCleanup(patcher.stop)
        for key in ("JUNEAU_TERMINAL_CONSOLE", "JUNEAU_TERMINAL_FULL_LOG", "JUNEAU_TERMINAL_BOARD_LOG", "JUNEAU_TERMINAL_SESSION"):
            os.environ.pop(key, None)
        self.out, self.dims, self.clock = TTY(), [80, 24], Clock()

    def console(self, nested=False):
        console = jr.Console(jr.Sinks(console="board"), stream=self.out, clock=self.clock, nested=nested,
                             size=lambda: os.terminal_size(tuple(self.dims)))
        self.addCleanup(console.close)
        return console

    @staticmethod
    def step(console, i, title, ms=2000):
        console("step_start", {"id": i, "n": 1, "title": title, "parent": None})
        console("step_end", {"id": i, "status": "ok", "ms": ms, "exit": None, "summary": None, "totals": None})

    def parent(self):
        jr._console = parent = self.console()
        parent("session", {"title": "Push", "header": ["Juneau", "master"], "lines": ["Commit Message: m"],
                           "header_more": [], "nested": False})
        self.step(parent, "pre", "Prechecks")
        return parent

    def run_child(self, env, titles=("BOM", "Compile")):
        """The child's session, closed; returns the terminal as it was just before the close."""
        with mock.patch.dict(os.environ, env):
            child = self.console(nested=True)
        for i, title in enumerate(titles):
            self.step(child, f"c{i}", title)
        before = self.screen()
        child.close()
        return before

    def screen(self):
        screen = Screen(rows=self.dims[1], cols=self.dims[0])
        screen.feed(self.out.getvalue())
        return screen

    def assert_one_board(self, text, rows):
        self.assertEqual(text.count("Push: Juneau · master"), 1, text)
        at = [text.index(row) for row in rows]
        self.assertEqual(at, sorted(at), text)

    def test_the_child_draws_the_parents_rows_and_hands_its_own_back(self):
        parent = self.parent()
        with jr.handoff() as env:
            during = self.run_child(env)
            self.assertEqual([kind for kind, _ in json.loads(Path(env["JUNEAU_TERMINAL_RECORD"]).read_text())["rows"]],
                             ["row", "row"])
            self.assertEqual(self.screen().history(), "")       # neither printed a row nor the header into scrollback
        self.assert_one_board(during.history(), ["Push: Juneau", "✔ Prechecks", "✔ BOM", "✔ Compile"])
        self.step(parent, "com", "Commit")
        rows = ["Push: Juneau", "✔ Prechecks", "✔ BOM", "✔ Compile", "✔ Commit "]
        self.assert_one_board(self.screen().text(), rows)
        parent.close()
        self.assert_one_board(self.screen().history(), rows)
        self.assertFalse(Path(env["JUNEAU_TERMINAL_RECORD"]).exists())

    def test_the_handed_back_rows_are_laid_out_again_at_a_new_width(self):
        parent = self.parent()
        with jr.handoff() as env:
            self.run_child(env, ("BOM",))
        self.dims[0] = 60
        self.step(parent, "com", "Commit")
        rows = [line.rstrip() for line in self.screen().lines if line.startswith("✔")]
        self.assertEqual([jr._vlen(line) for line in rows], [59] * 3, rows)     # every time ends a column short

    def test_an_older_child_that_prints_its_own_rows_still_leaves_one_header(self):
        parent = self.parent()
        with jr.handoff() as env:
            self.run_child({k: v for k, v in env.items() if k != "JUNEAU_TERMINAL_RECORD"})
        self.step(parent, "com", "Commit")
        live = self.screen().history()
        self.assertEqual(live.count("Push: Juneau · master"), 1, live)     # the redrawn view adds no second header
        parent.close()
        history = self.screen().history()
        self.assertEqual(history.count("Push: Juneau · master"), 1, history)
        for row in ("✔ Prechecks", "✔ BOM", "✔ Compile", "✔ Commit "):
            self.assertEqual(history.count(row), 1, history)

    def test_a_child_that_cannot_write_the_record_prints_its_own_rows(self):
        self.parent()
        with jr.handoff() as env:
            self.run_child({**env, "JUNEAU_TERMINAL_RECORD": "/nonexistent/record.json"})
            printed = self.screen().history()
        self.assertEqual([row for row in ("✔ Prechecks", "✔ BOM", "✔ Compile") if row in printed], ["✔ BOM", "✔ Compile"])

    def test_a_record_file_that_cannot_be_read_draws_no_rows_above(self):
        for text in ("", "{", '{"above": [[1]]}', '{"above": 3}', "[]"):
            with self.subTest(text=text), tempfile.NamedTemporaryFile("w", suffix=".json", delete=False) as f:
                f.write(text)
            self.addCleanup(os.unlink, f.name)
            self.assertEqual(jr._record(f.name, "above"), [])
        self.assertEqual(jr._record(None, "above"), [])
        self.assertEqual(jr._record("/nonexistent/record.json", "rows"), [])


class FitViewTest(unittest.TestCase):
    BOARD = [("▶ Push: Juneau · master", False), ("▶ Commit Message: m", False), ("▶ Output: tail -f /o", True),
             ("✔ Prechecks", False), ("⠋ Java Tests", False), ("○ Commit", False), ("▶ Overall  38%", False)]

    def test_everything_fits(self):
        self.assertEqual(jr.fit_view(self.BOARD, 30, 60), [t for t, _ in self.BOARD])

    def test_a_short_pane_keeps_the_last_rows(self):
        for budget in range(0, len(self.BOARD) + 1):
            with self.subTest(budget=budget):
                self.assertEqual(jr.fit_view(self.BOARD, budget, 60), [t for t, _ in self.BOARD][len(self.BOARD) - budget:])

    def test_a_log_line_costs_its_wrapped_height_and_is_left_out_whole_when_it_does_not_fit(self):
        board = [("▶ T", False), ("▶ Output: tail -f /" + "o" * 70, True), ("⠋ X", False)]
        self.assertEqual(jr.fit_view(board, 5, 30), [t for t, _ in board])
        self.assertEqual(jr.fit_view(board, 4, 30), [board[1][0], "⠋ X"])     # the log line takes three rows
        self.assertEqual(jr.fit_view(board, 3, 30), ["⠋ X"])        # 3 + 1 > 3: left out whole, not cut


if __name__ == "__main__":
    unittest.main()
