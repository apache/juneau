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
        with mock.patch.dict(os.environ, {"JUNEAU_RUN_DETAIL": "modules"}):
            self.assertEqual(jr.resolve_detail(None), "modules")
            self.assertEqual(jr.resolve_detail("all"), "all")

    def test_unknown_level_lists_the_valid_ones(self):
        with self.assertRaises(ValueError) as e:
            jr.resolve_detail("loud")
        self.assertIn("summary, actionable, modules, all", str(e.exception))

    def test_export_sets_the_env_for_children(self):
        with mock.patch.dict(os.environ, {}):
            self.assertEqual(jr.export_detail("summary"), "summary")
            self.assertEqual(os.environ["JUNEAU_RUN_DETAIL"], "summary")

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

    def test_module_rows_by_detail(self):
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
        ("module_start", {"step": step, "module": f"{step}/p", "title": "Parent"}),
        ("module_compiled", {"step": step, "module": f"{step}/p", "ok": True}),
        ("module_end", {"step": step, "module": f"{step}/p", "status": "ok", "ms": 100, "totals": None}),
        ("module_start", {"step": step, "module": f"{step}/a", "title": "Mod A"}),
        ("class_done", {"step": step, "module": f"{step}/a", "outcome": "pass"}),
        ("module_compiled", {"step": step, "module": f"{step}/a", "ok": True}),
        ("module_tested", {"step": step, "module": f"{step}/a"}),
        ("module_end", {"step": step, "module": f"{step}/a", "status": "ok", "ms": 41000, "totals": (7139, 0, 0, 0)}),
        ("module_start", {"step": step, "module": f"{step}/b", "title": "Mod B"}),
        ("class_done", {"step": step, "module": f"{step}/b", "outcome": "fail"}),
        ("module_compiled", {"step": step, "module": f"{step}/b", "ok": True}),
        ("module_tested", {"step": step, "module": f"{step}/b"}),
        ("module_end", {"step": step, "module": f"{step}/b", "status": "fail", "ms": 2000, "totals": (10, 2, 0, 0)}),
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
             ("module_start", {"step": "build", "module": "build/a", "title": "Mod A"}),
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
        blocks = feed(b, ("session", {"title": "🚀 Juneau push", "header": ["master", "PGP ✓"], "nested": False}))
        self.assertEqual(plain(b, blocks, self.clock), "🚀 Juneau push · master · PGP ✓\n")
        self.clock.advance(348)
        self.assertEqual(plain(b, feed(b, ("done", {"status": "ok", "commit": None})), self.clock),
                         "✅ Done in 5:48\n")

    def test_final_line_forms(self):
        self.assertEqual(jr.format_final("fail", 0, "Tests", "/tmp/f.log"), "❌ Failed at Tests · full log: /tmp/f.log")
        self.assertEqual(jr.format_final("fail", 0, None, None), "❌ Failed")
        self.assertEqual(jr.format_final("cancelled", 61000, None, None), "⚠ Cancelled after 1:01")
        self.assertEqual(jr.fmt_elapsed(3723000), "1:02:03")

    def test_a_nested_board_has_no_header_and_no_final_line(self):
        b = self.board(nested=True)
        self.assertEqual(feed(b, ("session", {"title": "t", "header": [], "nested": True}),
                              ("done", {"status": "ok", "commit": None})), [])

    def test_the_label_width_comes_from_the_env_or_the_longest_label(self):
        b = self.board()
        feed(b, ("step_start", {"id": "x", "n": 1, "title": "A very long label", "parent": None}))
        self.assertEqual(b.width(), len("A very long label"))
        with mock.patch.dict(os.environ, {"JUNEAU_RUN_LABEL_WIDTH": "20"}):
            self.assertEqual(b.width(), 20)

    def test_a_sub_rows_maven_modules_draw_on_its_parent_row(self):
        """release.py: each command is a run_tool sub-row (parent=<step>), and Maven's modules fill the step's bar."""
        b = self.board()
        blocks = feed(b, ("step_start", {"id": "verify", "n": 1, "title": "Clean verify", "parent": None,
                                         "label": "Verify"}),
             ("step_start", {"id": "verify.1", "n": 1, "title": "git status", "parent": "verify"}),
             ("step_end", {"id": "verify.1", "status": "ok", "ms": 10, "exit": None, "summary": None,
                           "totals": None}))
        self.assertEqual(b.rows["verify"].boxes(), ["pass"])        # a generic command is one box
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
             ("module_start", {"step": "tests", "module": "tests/a", "title": "Mod A"}),
             ("class_done", {"step": "tests", "module": "tests/a", "outcome": "pass"}),
             ("module_start", {"step": "tests", "module": "tests/b", "title": "Mod B"}),
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
        for key in ("RUN_MARKERS", "TERM", "JUNEAU_RUN_CONSOLE", "JUNEAU_RUN_FULL_LOG", "JUNEAU_RUN_CONDENSED_LOG"):
            os.environ.pop(key, None)
        self.clock = Clock()

    def console(self, stream=None, **sinks):
        c = jr.Console(jr.Sinks(**sinks), stream=stream or io.StringIO(), clock=self.clock)
        self.addCleanup(c.close)
        return c

    def test_screen_mode(self):
        self.assertIsNone(self.console(console="full").mode)
        self.assertIsNone(self.console(console="none").mode)
        self.assertEqual(self.console(console="condensed").mode, "plain")
        self.assertEqual(self.console(TTY(), console="condensed").mode, "live")
        with mock.patch.dict(os.environ, {"TERM": "dumb"}):
            self.assertEqual(self.console(TTY(), console="condensed").mode, "plain")
        with mock.patch.dict(os.environ, {"JUNEAU_RUN_LIVE": "0"}):
            self.assertEqual(self.console(TTY(), console="condensed").mode, "plain")
        with mock.patch.dict(os.environ, {"RUN_MARKERS": "1"}):
            self.assertIsNone(self.console(TTY(), console="condensed").mode)

    def test_the_condensed_log_gets_the_rows_but_no_heartbeat(self):
        import tempfile
        from pathlib import Path
        with tempfile.TemporaryDirectory() as tmp:
            log = Path(tmp) / "c.log"
            c = self.console(console="none", condensed_log=str(log))
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
        c = self.console(out, console="condensed")
        c("step_start", {"id": "tests", "n": 3, "title": "Tests", "parent": None})
        c("module_start", {"step": "tests", "module": "tests/a", "title": "Mod A"})
        c("tests", {"step": "tests", "total": 10, "fail": 1, "err": 0, "skip": 0})
        self.clock.advance(14)
        c.tick()
        self.assertEqual(out.getvalue(), "")
        self.clock.advance(1)
        c.tick()
        self.assertEqual(out.getvalue(), "… 10 tests, 1 failed · 1 modules running  0:15\n")

    def test_close_still_closes_the_condensed_log_when_the_screen_fails_to_close(self):
        with tempfile.TemporaryDirectory() as tmp:
            c = self.console(TTY(), console="condensed", condensed_log=str(Path(tmp) / "c.log"))
            log = c._log
            c.screen = mock.Mock(**{"close.side_effect": OSError("screen gone")})
            c.close()
        self.assertTrue(log.closed)
        self.assertIsNone(c._log)

    def test_write_condensed_is_silent_while_a_console_is_active(self):
        out = io.StringIO()
        jr._console = self.console(console="condensed")
        with contextlib.redirect_stdout(out):
            jr.Sinks(console="condensed").write_condensed("✓ old condensed line")
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

    def test_last_line_is_overall_and_counts_in_max_lines(self):
        b = self.board()
        lines = self.live().region(b, 79, 18)
        self.assertEqual(lines[-1], "Overall  25%  ~1:30 left  · 0 of 2 steps")       # 30 / (30 + 30 + 60)
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
                self.assertEqual(len(lines), cap)
                self.assertTrue(lines[-1].startswith("Overall"), lines)

    def test_overall_sits_below_the_pending_rows(self):
        b = self.board()
        b.apply("plan", dict(ids=["a", "b"]))
        lines = self.live().region(b, 79, 18)
        self.assertTrue(lines[-1].startswith("Overall"), lines)

    def test_absent_without_a_plan_without_history_and_when_nested(self):
        for b in (self.board(plan=None), self.board(hist={}), self.board(nested=True)):
            self.assertTrue(self.live().region(b, 79, 18)[0][2:].startswith("A"))

    def test_setup_row_sits_above_the_overall_line(self):
        b = self.board()
        b.apply("reactor", dict(step="a", size=5, poms=["p0", "p1"], tests=False))
        lines = self.live().region(b, 79, 18)
        self.assertTrue(lines[-1].startswith("Overall"))
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
        patcher = mock.patch.dict(os.environ, {"JUNEAU_RUN_CONSOLE": "condensed"})
        patcher.start()
        self.addCleanup(patcher.stop)
        os.environ.pop("RUN_MARKERS", None)
        # `python3 -m unittest` (3.10 and earlier) sets argv[0] to "python3 -m unittest", which names the default full log
        argv = mock.patch.object(jr.sys, "argv", ["/x/scripts/test.py"])
        argv.start()
        self.addCleanup(argv.stop)

    def test_session_prints_the_header_and_marks_the_env(self):
        out = io.StringIO()
        with contextlib.redirect_stdout(out), mock.patch.object(jr.sys, "argv", ["/x/scripts/test.py"]):
            c = jr.session("🧪 Juneau test", ["master"])
        self.assertIs(jr._console, c)
        self.assertEqual(os.environ["JUNEAU_RUN_SESSION"], "1")
        self.assertRegex(out.getvalue(), r"^🧪 Juneau test · master · tail -f \S+/test-\d{8}-\d{6}(-\d+)?\.log\n$")

    def test_a_console_that_fails_to_start_leaves_the_env_unset(self):
        with tempfile.TemporaryDirectory() as tmp:
            Path(tmp, "file").write_text("")
            sinks = jr.Sinks(console="condensed", condensed_log=f"{tmp}/file/condensed.log")
            with contextlib.redirect_stdout(io.StringIO()), self.assertRaises(OSError):
                jr.session("t", sinks=sinks)
        self.assertNotIn("JUNEAU_RUN_SESSION", os.environ)
        self.assertIsNone(jr._console)

    def test_a_nested_session_prints_no_header(self):
        os.environ["JUNEAU_RUN_SESSION"] = "1"
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            jr.session("Juneau test", ["master"])
            jr.done("ok")
        self.assertEqual(out.getvalue(), "")

    def test_the_header_prints_a_tail_command_for_the_full_log(self):
        with tempfile.TemporaryDirectory() as tmp:
            out = io.StringIO()
            with contextlib.redirect_stdout(out):
                jr.session("🚀 Juneau push", ["master"], sinks=jr.Sinks(full_log=f"{tmp}/push-full.log"))
            self.assertEqual(out.getvalue(), f"🚀 Juneau push · master · tail -f {shlex.quote(tmp + '/push-full.log')}\n")

    def test_the_tail_command_quotes_a_path_with_spaces(self):
        with tempfile.TemporaryDirectory() as tmp:
            out = io.StringIO()
            with contextlib.redirect_stdout(out):
                jr.session("t", sinks=jr.Sinks(full_log=f"{tmp}/a b/push.log"))
            self.assertEqual(out.getvalue(), f"t · tail -f '{tmp}/a b/push.log'\n")

    def test_a_top_level_session_prunes_the_default_log_dir(self):
        directory = Path(os.environ["JUNEAU_RUN_LOG_DIR"])
        directory.mkdir(parents=True, exist_ok=True)
        for i in range(25):
            (directory / f"old-{i:02d}.log").write_text("")
            os.utime(directory / f"old-{i:02d}.log", (1000 + i, 1000 + i))
        with contextlib.redirect_stdout(io.StringIO()):
            jr.session("t")
        self.assertEqual(len(list(directory.glob("*.log"))), 20)

    def test_say_goes_through_the_console_once_a_session_owns_the_screen(self):
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            jr.session("t")
            jr.say("Running: mvn")          # info: hidden at actionable
            jr.say("⚠ dirty tree", level="warn")
        self.assertRegex(out.getvalue(), r"^t · tail -f \S+\.log\n⚠ dirty tree\n$")

    def test_with_markers_on_stdout_stays_the_marker_stream(self):
        os.environ["RUN_MARKERS"] = "1"
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
    return ("module_start", {"step": step, "module": f"{step}/{name}", "title": f"Mod {name.upper()}"})


def cls(name, outcome="pass", step="tests"):
    return ("class_done", {"step": step, "module": f"{step}/{name}", "outcome": outcome})


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
            env = {"JUNEAU_RUN_HISTORY": "1", "JUNEAU_RUN_HISTORY_DIR": hist_dir, "JUNEAU_RUN_CONSOLE": "none"}
            with mock.patch.dict(os.environ, env), mock.patch.object(jr.sys, "argv", ["/x/scripts/push.py"]):
                os.environ.pop("RUN_MARKERS", None)
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

    def test_a_running_row_and_its_module_redraw_in_place(self):
        self.push(*start(), mod_start("a"))
        self.push(cls("a"), cls("a", "fail"))
        self.assertEqual(self.screen.text(), "\n".join([
            (self.spin() + "Tests".ljust(10) + "  ▒░░  0/3").ljust(59 - 4) + "0:01",     # Mod A is in flight
            "    Mod A  •F",
        ]))
        self.push(("module_end", {"step": "tests", "module": "tests/a", "status": "fail", "ms": 1000,
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
        self.assertIn("84/84", lines[0])                                        # count stays on the first line
        self.assertTrue(all("84/84" not in line for line in lines[1:]))

    def test_a_bar_that_fits_exactly_does_not_wrap(self):
        cells = ["#"] * 5
        self.assertEqual(jr._fit_row("L", cells, "r", "", len("L  #####  r"), wrap=True), "L  #####  r")
        self.assertEqual(jr._fit_row("L", cells, "r", "", len("L  #####  r") - 1, wrap=True), "L  ####  r\n   #")

    def test_one_unit_is_one_cell(self):
        lines = self.wrapped(1, 79)
        self.assertEqual(len(lines), 1)
        self.assertEqual(lines[0].count("#"), 1)

    def test_a_failure_on_a_continuation_line_keeps_its_cell(self):
        lines = self.wrapped(84, 79, fail_at=83)
        self.assertEqual((len(lines) > 1, lines[-1].rstrip()[-1]), (True, jr.PLAIN_GLYPHS["fail"]))
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
        lines = self.screen.text().splitlines()                               # finished block prints wrapped too
        self.assertEqual((len(lines), self.live.height), (2, 0))
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

    def test_the_region_never_exceeds_rows_minus_two(self):
        self.dims = (60, 6)
        self.push(*start(size=5), *[mod_start(n) for n in "abcde"], *[cls(n) for n in "abcde"])
        region = self.live.region(self.board, 59, 4)
        self.assertEqual(len(region), 4)
        self.assertEqual(region[1], "    +3 more running")
        self.assertTrue(region[2].startswith("    Mod D  ") and region[3].startswith("    Mod E  "), region)
        self.assertTrue(all(len(line) <= 59 for line in region), region)
        self.assertEqual(len(self.screen.text().splitlines()), 4)
        self.assertTrue(all(len(line) <= 59 for line in self.screen.lines), self.screen.lines)
        self.assertFalse(self.screen.clamped)

    def test_boxes_cap_at_one_line(self):
        self.dims = (40, 10)
        self.push(*start(), mod_start("a"), *[cls("a") for _ in range(200)])
        self.assertEqual(self.live.region(self.board, 39, 8)[1:], ["    Mod A  " + "•" * 24 + "+176"])

    def test_a_module_that_exactly_fits_has_no_counter(self):
        self.dims = (40, 10)
        self.push(*start(), mod_start("a"), *[cls("a") for _ in range(28)])
        self.assertEqual(self.live.region(self.board, 39, 8)[1:], ["    Mod A  " + "•" * 28])

    def test_a_module_one_over_shows_the_counter_in_the_last_cells(self):
        self.dims = (40, 10)
        self.push(*start(), mod_start("a"), *[cls("a") for _ in range(29)])
        self.assertEqual(self.live.region(self.board, 39, 8)[1:], ["    Mod A  " + "•" * 26 + "+3"])

    def test_the_counter_keeps_the_strip_width_as_digits_grow(self):
        self.dims = (40, 10)
        for total in (29, 40, 126, 127, 128, 1000, 5000):
            with self.subTest(total=total):
                board = jr.Board(clock=lambda: 100.0)
                feed(board, *start(), mod_start("a"), *[cls("a") for _ in range(total)])
                live = jr.LiveRenderer(lambda text: None, size=lambda: os.terminal_size(self.dims), clock=lambda: 100.0,
                                       color=False)
                strip = live._sub_lines(next(iter(board.rows.values())).modules["tests/a"], 39)[0]
                self.assertEqual(jr._vlen(strip), jr._vlen("    Mod A  ") + 28)
                dots = strip[len("    Mod A  "):].lstrip("•")
                self.assertRegex(dots, r"^\+\d+$")
                self.assertEqual(int(dots[1:]), total - (28 - len(dots)))

    def test_the_counter_is_green_without_a_hidden_failure_and_red_with_one(self):
        self.dims = (40, 10)
        def strip(first):
            board = jr.Board(clock=lambda: 100.0)
            feed(board, *start(), mod_start("a"), cls("a", first), *[cls("a") for _ in range(40)])
            live = jr.LiveRenderer(lambda text: None, size=lambda: os.terminal_size(self.dims), clock=lambda: 100.0,
                                   color=True)
            return live._sub_lines(next(iter(board.rows.values())).modules["tests/a"], 39)[0]
        self.assertTrue(strip("pass").endswith("\x1b[32m+16\x1b[0m"), repr(strip("pass")))
        self.assertTrue(strip("skip").endswith("\x1b[32m+16\x1b[0m"), repr(strip("skip")))
        self.assertTrue(strip("fail").endswith("\x1b[31m+16\x1b[0m"), repr(strip("fail")))

    def test_a_failure_among_the_kept_dots_does_not_redden_the_counter(self):
        board = jr.Board(clock=lambda: 100.0)
        feed(board, *start(), mod_start("a"), *[cls("a") for _ in range(40)], cls("a", "fail"))
        live = jr.LiveRenderer(lambda text: None, size=lambda: os.terminal_size((40, 10)), clock=lambda: 100.0, color=True)
        strip = live._sub_lines(next(iter(board.rows.values())).modules["tests/a"], 39)[0]
        self.assertIn("\x1b[32m+16\x1b[0m", strip)

    def test_a_tiny_strip_still_fits(self):
        board = jr.Board(clock=lambda: 100.0)
        feed(board, *start(), mod_start("a"), *[cls("a") for _ in range(50)])
        live = jr.LiveRenderer(lambda text: None, size=lambda: os.terminal_size((40, 10)), clock=lambda: 100.0, color=False)
        module = next(iter(board.rows.values())).modules["tests/a"]
        for cols in range(1, 20):
            with self.subTest(cols=cols):
                strip = live._sub_lines(module, cols)[0]
                prefix = jr._vlen(f"    {jr._vcut(module.title, max(1, cols // 2))}  ")
                self.assertEqual(jr._vlen(strip) - prefix, max(1, cols - prefix))

    def test_shrinking_the_region_clears_the_old_lines(self):
        self.push(*start(), mod_start("a"), mod_start("b"))
        self.push(("module_end", {"step": "tests", "module": "tests/a", "status": "ok", "ms": 1, "totals": None}),
                  ("module_end", {"step": "tests", "module": "tests/b", "status": "ok", "ms": 1, "totals": None}))
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

    def test_hides_the_cursor_while_drawing_and_shows_it_on_close(self):
        writes = []
        live = jr.LiveRenderer(writes.append, size=lambda: os.terminal_size(self.dims), clock=self.clock, color=False)
        live.render(self.board, feed(self.board, *start()))
        self.assertTrue(writes[0].startswith("\x1b[?25l"))
        live.close()
        self.assertTrue(writes[-1].endswith("\x1b[?25h"))

    def test_pause_clears_the_region_and_resume_redraws_it(self):
        self.push(*start(), mod_start("a"))
        self.live.pause()
        self.assertEqual(self.screen.text(), "")
        self.clock.advance(1)
        self.live.tick(self.board)
        self.assertEqual(self.screen.text(), "")
        self.live.resume()
        self.assertTrue(self.screen.text().startswith(self.spin() + "Tests"))

    def test_a_watchdog_prompt_stops_redraws_until_the_line_ends(self):
        self.push(*start())
        self.push(("prompt", {"step": "tests", "text": "Passphrase: "}))
        self.assertEqual(self.screen.text(), "Passphrase:")
        self.assertTrue(self.screen.cursor_visible)
        self.clock.advance(1)
        self.live.tick(self.board)
        self.assertEqual(self.screen.text(), "Passphrase:")
        self.push(("prompt_end", {"step": "tests"}))
        self.assertTrue(self.screen.text().splitlines()[1].startswith(self.spin() + "Tests"))
        self.assertFalse(self.screen.cursor_visible)

    def test_a_line_printed_during_a_prompt_starts_on_its_own_line(self):
        self.push(*start())
        self.push(("prompt", {"step": "tests", "text": "Passphrase: "}))
        self.push(("step_end", {"id": "tests", "status": "ok", "ms": 0, "exit": None, "summary": "done",
                                "totals": None}))
        self.assertEqual(self.screen.text().splitlines()[:2], ["Passphrase:", "✔ " + "Tests".ljust(10) + "  done"])

    def test_a_large_reactor_keeps_its_count_and_totals_and_wraps_the_bar(self):
        self.dims = (64, 10)            # 8 region lines of 10 cells hold the 80 modules
        self.push(*start(size=80))
        for i in range(80):
            name = f"m{i}"
            self.push(mod_start(name), ("module_end", {"step": "tests", "module": f"tests/{name}", "status": "ok",
                                                       "ms": 1, "totals": (1, 0, 0, 0)}))
        self.push(("tests", {"step": "tests", "total": 7139, "fail": 2, "err": 0, "skip": 0}))
        lines = self.screen.text().splitlines()
        self.assertTrue(all(len(line) <= 63 for line in lines), lines)
        self.assertNotIn("\u2026", "".join(lines))        # no cell is cut: the bar wraps instead
        self.assertEqual("".join(lines).count("\u2588"), 80)
        self.assertTrue(lines[0].rstrip().endswith("80/80  7,139 tests, 2 failed  1:21"), lines[0])
        self.push(("step_end", {"id": "tests", "status": "fail", "ms": 90000, "exit": 1, "summary": None,
                                "totals": None}))
        row = self.screen.text().splitlines()[0]
        self.assertLessEqual(len(row), 63, row)
        self.assertTrue(row.endswith("80/80  7,139 tests, 2 failed  1:30"), row)
        self.assertTrue(row.startswith("✗ " + "Tests".ljust(10) + "  ██"), row)

    def test_a_row_too_long_even_without_its_bar_is_cut_from_the_right(self):
        line = jr._fit_row("A very long step label", ["█"] * 5, "5/5  7,139 tests, 2 failed", "0:01", 20)
        self.assertEqual(jr._vlen(line), 20)
        self.assertTrue(line.endswith("…  0:01"), line)

    def test_wide_characters_take_two_columns(self):
        self.assertEqual(jr._vlen("测试 ok"), 7)
        self.assertEqual(jr._vcut("测试测试", 5), "测试")
        self.assertEqual(jr._vlen(jr._fit("测" * 40, "0:01", 30)), 30)
        self.dims = (40, 10)
        self.push(*start(), ("module_start", {"step": "tests", "module": "tests/a", "title": "测试" * 20}),
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
        live.render(self.board, feed(self.board, ("module_end", {"step": "tests", "module": "tests/a",
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
                         ("module_start", {"step": "verify.1", "module": "verify.1/a", "title": "Mod A"}))
                feed(b, ("quiet", {"step": "verify.1", "seconds": 25}))
                self.assertEqual(b.rows["verify"].quiet, 25)
                feed(b, ("step_end", {"id": "verify.1", "status": "ok", "ms": 30000, "exit": None, "summary": None,
                                      "totals": None}))
                self.assertEqual(b.rows["verify"].quiet, 0)

    def test_a_quiet_row_says_so(self):
        self.push(*start(), ("quiet", {"step": "tests", "seconds": 25}))
        self.assertIn(" · quiet 25s", self.screen.text())


class CollapsedModuleLineTest(unittest.TestCase):
    """Name-only in-flight modules share one comma-joined line under their row; modules with results keep sub-lines."""

    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        self.board = jr.Board(clock=lambda: 100.0)

    def region(self, *events, cols=60):
        feed(self.board, *events)
        live = jr.LiveRenderer(lambda text: None, size=lambda: os.terminal_size((cols + 1, 20)), clock=lambda: 100.0,
                               color=False)
        return live.region(self.board, cols, 18)

    def test_name_only_modules_collapse_to_two_lines(self):
        for step in ("compile", "x-tool"):                      # data-driven: the step id does not matter
            with self.subTest(step=step):
                reset_run_state()
                self.board = jr.Board(clock=lambda: 100.0)
                lines = self.region(*start(step, 4), mod_start("a", step), mod_start("b", step), mod_start("c", step))
                self.assertEqual(len(lines), 2)
                self.assertEqual(lines[1], "    A, B, C")

    def test_modules_with_results_keep_their_own_sub_lines(self):
        lines = self.region(*start(), mod_start("a"), mod_start("b"), cls("a"), cls("a", "fail"))
        self.assertEqual(len(lines), 3)
        self.assertEqual(lines[1], "    B")                       # name-only first, then the detail sub-lines
        self.assertTrue(lines[2].startswith("    Mod A  "))

    def test_a_module_with_a_first_result_leaves_the_collapsed_line_on_the_next_redraw(self):
        lines = self.region(*start(), mod_start("a"), mod_start("b"))
        self.assertEqual(lines[1], "    A, B")
        lines = self.region(cls("b"))
        self.assertEqual(lines[1], "    A")
        self.assertTrue(lines[2].startswith("    Mod B  "))

    def test_pom_modules_are_never_on_the_collapsed_line(self):
        feed(self.board, ("step_start", {"id": "mvn", "n": 1, "title": "mvn", "parent": None}),
             ("reactor", {"step": "mvn", "size": 3, "poms": ["mvn/p"], "tests": False}),
             ("module_start", {"step": "mvn", "module": "mvn/p", "title": "Mod P"}))
        lines = self.region(("module_start", {"step": "mvn", "module": "mvn/a", "title": "Mod A"}),
                            ("module_start", {"step": "mvn", "module": "mvn/b", "title": "Mod B"}))
        self.assertEqual([line for line in lines if line.startswith("    ")], ["    A, B"])

    def test_the_prefix_uses_every_module_seen_not_only_those_in_flight(self):
        lines = self.region(*start(), mod_start("a"), mod_start("b"), mod_start("c"),
                            ("module_end", {"step": "tests", "module": "tests/b", "status": "ok", "ms": 1}),
                            ("module_end", {"step": "tests", "module": "tests/c", "status": "ok", "ms": 1}))
        self.assertEqual(lines[1], "    A")                      # still "Mod " stripped though only one is in flight


class LiveConsoleTest(unittest.TestCase):
    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        patcher = mock.patch.dict(os.environ, {})
        patcher.start()
        self.addCleanup(patcher.stop)
        for key in ("RUN_MARKERS", "TERM", "NO_COLOR", "JUNEAU_RUN_LIVE"):
            os.environ.pop(key, None)

    def test_live_mode_uses_the_live_renderer(self):
        c = jr.Console(jr.Sinks(console="condensed"), stream=TTY(), size=lambda: os.terminal_size((80, 24)))
        self.addCleanup(c.close)
        self.assertIsInstance(c.screen, jr.LiveRenderer)
        self.assertTrue(c.screen.color)

    def test_no_color_keeps_the_live_renderer_without_colour(self):
        with mock.patch.dict(os.environ, {"NO_COLOR": "1"}):
            c = jr.Console(jr.Sinks(console="condensed"), stream=TTY(), size=lambda: os.terminal_size((80, 24)))
        self.addCleanup(c.close)
        self.assertIsInstance(c.screen, jr.LiveRenderer)
        self.assertFalse(c.screen.color)

    def test_plain_output_never_has_escapes_or_carriage_returns(self):
        out = io.StringIO()
        c = jr.Console(jr.Sinks(console="condensed"), stream=out)
        self.addCleanup(c.close)
        for kind, fields in maven_events():
            c(kind, fields)
        c("step_end", {"id": "tests", "status": "fail", "ms": 1, "exit": 1, "summary": None, "totals": None})
        self.assertNotIn("\x1b", out.getvalue())
        self.assertNotIn("\r", out.getvalue())

    def test_default_stream_is_bound_once_so_a_later_stdout_swap_cannot_capture_frames(self):
        original, swapped = io.StringIO(), io.StringIO()
        with mock.patch.dict(os.environ, {"RUN_MARKERS": ""}), mock.patch.object(sys, "stdout", original):
            c = jr.Console(jr.Sinks(console="condensed"), clock=Clock())
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
        with mock.patch.dict(os.environ, {"TERM": "xterm", "RUN_MARKERS": ""}):
            console = jr.Console(jr.Sinks(console="condensed"), stream=out)
        self.assertEqual(console.mode, "live")
        console.screen.render = mock.Mock(side_effect=RuntimeError("boom"))
        console("step_start", {"id": "a", "n": 1, "title": "Alpha", "parent": None})
        self.assertIsInstance(console.screen, jr.PlainRenderer)
        self.assertIn("⚠ console view failed (RuntimeError('boom')); using plain output", out.getvalue())
        console("step_end", {"id": "a", "status": "ok", "ms": 10, "exit": None, "summary": None, "totals": None})
        self.assertIn("Alpha", out.getvalue())

    def test_a_tick_exception_also_falls_back(self):
        out = TTY()
        with mock.patch.dict(os.environ, {"TERM": "xterm", "RUN_MARKERS": ""}):
            console = jr.Console(jr.Sinks(console="condensed"), stream=out)
        console.screen.tick = mock.Mock(side_effect=ValueError("bad"))
        console.tick()
        self.assertIsInstance(console.screen, jr.PlainRenderer)

    def test_a_log_renderer_exception_stops_the_log_only(self):
        with tempfile.TemporaryDirectory() as tmp:
            console = jr.Console(jr.Sinks(console="none", condensed_log=str(Path(tmp) / "c.log")))
            console.log.render = mock.Mock(side_effect=RuntimeError("disk"))
            console("say", {"text": "x", "level": "warn", "step": None})
            self.assertIsNone(console.log)
            console("say", {"text": "y", "level": "warn", "step": None})
            console.close()

    def live_console(self, out, **sinks):
        with mock.patch.dict(os.environ, {"TERM": "xterm", "RUN_MARKERS": ""}):
            console = jr.Console(jr.Sinks(console="condensed", **sinks), stream=out,
                                 size=lambda: os.terminal_size((80, 24)))
        self.addCleanup(console.close)
        self.assertEqual(console.mode, "live")
        return console

    def test_a_log_renderer_exception_says_so_once_on_screen(self):
        out = io.StringIO()
        with tempfile.TemporaryDirectory() as tmp, mock.patch.dict(os.environ, {"RUN_MARKERS": ""}):
            console = jr.Console(jr.Sinks(console="condensed", condensed_log=str(Path(tmp) / "c.log")), stream=out)
            console.log.render = mock.Mock(side_effect=OSError("disk full"))
            console("say", {"text": "⚠ x", "level": "warn", "step": None})
            console("say", {"text": "⚠ y", "level": "warn", "step": None})
            console.close()
        self.assertEqual(out.getvalue().count("⚠ condensed log stopped (OSError('disk full'))"), 1)
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
        with mock.patch.dict(os.environ, {"RUN_MARKERS": ""}):
            console = jr.Console(jr.Sinks(console="condensed"), stream=out)
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
        out = TTY()
        console = self.live_console(out)
        console.screen._flush = mock.Mock(side_effect=[None, RuntimeError("flush")])
        console.board.apply = mock.Mock(return_value=[("raw", "raw-once\n"), ("text", "after")])
        console("say", {"text": "x", "level": "warn", "step": None})
        self.assertEqual(out.getvalue().count("raw-once"), 1)
        self.assertEqual(out.getvalue().count("after"), 1)

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
            console = self.live_console(out, condensed_log=str(log))
            console.board.apply = mock.Mock(side_effect=KeyError("board"))
            console("say", {"text": "x", "level": "warn", "step": None})
            console("say", {"text": "y", "level": "warn", "step": None})
            console.tick()
            self.assertIsNone(console.screen)
            self.assertIsNone(console.log)
            self.assertEqual(console.board.apply.call_count, 1)
            console.close()
            self.assertIn("⚠ console view and condensed log stopped (KeyError('board'))",
                          log.read_text(encoding="utf-8"))
        self.assertEqual(out.getvalue().count("⚠ console view and condensed log stopped (KeyError('board'))"), 1)

    def test_a_screen_failure_stops_the_view_but_not_the_condensed_log(self):
        out = io.StringIO()
        with tempfile.TemporaryDirectory() as tmp, mock.patch.dict(os.environ, {"RUN_MARKERS": ""}):
            log = Path(tmp) / "c.log"
            console = jr.Console(jr.Sinks(console="condensed", condensed_log=str(log)), stream=out)
            console.screen.render = mock.Mock(side_effect=BrokenPipeError("epipe"))
            console("say", {"text": "⚠ first", "level": "warn", "step": None})
            console("say", {"text": "⚠ second", "level": "warn", "step": None})
            self.assertIsNotNone(console.log)
            console.close()
            logged = log.read_text(encoding="utf-8")
        self.assertIn("⚠ console view stopped (BrokenPipeError('epipe'))", logged)
        self.assertIn("⚠ first", logged)
        self.assertIn("⚠ second", logged)
        self.assertNotIn("condensed log stopped", logged + out.getvalue())
        self.assertEqual(out.getvalue().count("⚠ console view stopped (BrokenPipeError('epipe'))"), 1)

    def session_console(self, out):
        """A plain console as this process's session, drawing on out."""
        jr._console = jr.subscribe(jr.Console(jr.Sinks(console="condensed"), stream=out))
        return jr._console

    def assert_prints_plainly_after_the_stop(self, out):
        text = out.getvalue()
        for line in ("first", "second", "FAILURE DETAIL", "❌ Failed"):
            self.assertEqual(text.count(line), 1, line)
        self.assertLess(text.index("⚠ console view"), text.index("first"))

    def test_after_a_board_exception_script_text_and_the_final_line_print_plainly(self):
        out = io.StringIO()
        with mock.patch.dict(os.environ, {"RUN_MARKERS": ""}), contextlib.redirect_stdout(out):
            self.session_console(out).board.apply = mock.Mock(side_effect=KeyError("board"))
            jr.say("first")
            jr.say("second")
            jr.failure("FAILURE DETAIL")
            jr.done("fail")
            self.assertTrue(jr.console_active())
        self.assert_prints_plainly_after_the_stop(out)

    def test_after_a_screen_failure_script_text_and_the_final_line_print_plainly(self):
        out = io.StringIO()
        with mock.patch.dict(os.environ, {"RUN_MARKERS": ""}), contextlib.redirect_stdout(out):
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
        with mock.patch.dict(os.environ, {"RUN_MARKERS": ""}), contextlib.redirect_stdout(out), \
                mock.patch.object(jr, "_input_fd", return_value=None):
            self.session_console(out).screen.render = mock.Mock(side_effect=RuntimeError("boom"))
            jr.say("x")
            jr.run_tool([sys.executable, "-c", code], "generic", "t", "T", sinks=jr.Sinks(console="condensed"),
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
        with mock.patch.dict(os.environ, {"RUN_MARKERS": ""}):
            console = self.session_console(out)
        console.screen.pause = mock.Mock(side_effect=RuntimeError("pause"))
        with mock.patch("builtins.input", return_value="y"):
            self.assertEqual(jr.ask("? "), "y")
        self.assertTrue(console.stopped)
        self.assertIn("⚠ console view stopped (RuntimeError('pause'))", out.getvalue())

    def test_a_fallback_re_renders_a_raw_block_whose_clear_failed(self):
        out = TTY()
        console = self.live_console(out)
        console.board.apply = mock.Mock(return_value=[("text", "before"), ("raw", "raw-once\n")])
        clear, calls = console.screen._clear, []

        def failing_clear():
            calls.append(None)
            if len(calls) == 2:     # the raw block's, after the text block's in _flush
                raise RuntimeError("clear")
            clear()

        console.screen._clear = failing_clear
        console("say", {"text": "x", "level": "warn", "step": None})
        self.assertIsInstance(console.screen, jr.PlainRenderer)
        self.assertEqual(out.getvalue().count("before"), 1)
        self.assertEqual(out.getvalue().count("raw-once"), 1)

    def test_a_plain_stop_during_a_prompt_starts_the_warning_on_its_own_line(self):
        out = io.StringIO()
        with mock.patch.dict(os.environ, {"RUN_MARKERS": ""}):
            console = jr.Console(jr.Sinks(console="condensed"), stream=out)
        console("step_start", {"id": "a", "n": 1, "title": "Alpha", "parent": None})
        console("prompt", {"step": "a", "text": "Passphrase: "})
        console.screen.render = mock.Mock(side_effect=RuntimeError("boom"))
        console("say", {"text": "x", "level": "warn", "step": None})
        self.assertIn("Passphrase: \n⚠ console view stopped", out.getvalue())

    def test_ctrl_c_in_run_tool_pauses_the_console_first(self):
        console = mock.Mock()
        console.sinks.console = "none"
        order = []
        console.pause.side_effect = lambda: order.append("pause")
        with mock.patch.object(jr, "_console", console), \
                mock.patch.object(jr, "_terminate_group", side_effect=lambda p, grace=None: order.append("kill")), \
                mock.patch.object(jr.select, "select", side_effect=KeyboardInterrupt), \
                self.assertRaises(KeyboardInterrupt):
            jr.run_tool([sys.executable, "-c", "pass"], "generic", "t", "T",
                        sinks=jr.Sinks(console="none"))
        self.assertEqual(order[:2], ["pause", "kill"])

    def test_ctrl_c_pauses_with_signals_ignored_and_a_failing_pause_still_kills_the_child(self):
        console = self.live_console(TTY())
        seen = []

        def pause():
            seen.append(signal.getsignal(signal.SIGINT))
            raise RuntimeError("pause")

        console.screen.pause = pause
        before = (signal.getsignal(signal.SIGINT), signal.getsignal(signal.SIGTERM))
        with mock.patch.object(jr, "_console", console), \
                mock.patch.object(jr, "_terminate_group", side_effect=lambda p, grace=None: seen.append("kill")), \
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
    """Equal-width continuation lines and the expected test total."""

    def test_every_line_of_a_wrapped_bar_holds_the_same_number_of_cells(self):
        line = jr._fit_row("Tests", ["#"] * 50, "50/50", "0:05", 40, wrap=True)
        lines = line.split("\n")
        counts = [l.count("#") for l in lines]
        self.assertGreater(len(lines), 2)
        self.assertEqual(len(set(counts[:-1])), 1, counts)
        self.assertLessEqual(counts[-1], counts[0])
        self.assertEqual(sum(counts), 50)

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
            events += [("module_start", {"step": "a", "module": name, "title": f"Mod {name.upper()}"}),
                       ("module_end", {"step": "a", "module": name, "status": status, "ms": 1, "totals": None})]
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
        import scripts_support  # noqa: F401 - puts juneau_scripts on sys.path
        from juneau_scripts import run
        out = io.StringIO()
        with mock.patch.dict(os.environ, {"RUN_MARKERS": ""}), mock.patch.dict(sys.modules, {"juneau_run": jr}):
            jr._console = jr.subscribe(jr.Console(jr.Sinks(console="condensed"), stream=out))
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


class FoldTest(PendingCase):
    def busy(self):
        b = self.planned([("tests", "Tests")] + [(f"p{i}", f"Pending {i}") for i in range(6)])
        feed(b, *start(size=4), *[mod_start(n) for n in "abcd"], *[cls(n) for n in "abcd"])
        return b

    def test_pending_folds_before_module_lines(self):
        region = self.live().region(self.busy(), 79, 8)
        self.assertEqual(len(region), 8)
        self.assertEqual(sum(line.startswith("    Mod ") for line in region), 4)     # every module line kept
        self.assertEqual(region[-1], "○ +4 more pending")
        self.assertEqual(region[-3:-1], ["○ Pending 0", "○ Pending 1"])

    def test_more_pending_line_always_kept(self):
        region = self.live().region(self.busy(), 79, 4)
        self.assertEqual(region[-1], "○ +6 more pending")

    def test_then_module_lines_fold(self):
        region = self.live().region(self.busy(), 79, 5)
        self.assertEqual(len(region), 5)
        self.assertIn("    +2 more running", region)
        self.assertEqual(region[-1], "○ +6 more pending")


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

    def test_inflight_sub_line_unchanged(self):
        b = self.board()
        feed(b, *start(), mod_start("a"), cls("a"), cls("a", "fail"))
        live = jr.LiveRenderer(lambda t: None, size=lambda: os.terminal_size((60, 10)), clock=self.clock, color=False)
        self.assertEqual(live.region(b, 59, 8)[1:], ["    Mod A  •F"])


class SetupRowTest(RowCase):
    def events(self, pom_status="ok"):
        return [step_start("tests", "Tests"),
                ("reactor", {"step": "tests", "size": 3, "poms": ["p0", "p1"], "tests": True}),
                ("module_start", {"step": "tests", "module": "p0", "title": "Parent"}),
                ("module_end", {"step": "tests", "module": "p0", "status": "ok", "ms": 1, "totals": None}),
                ("module_start", {"step": "tests", "module": "p1", "title": "Bom"}),
                ("module_end", {"step": "tests", "module": "p1", "status": pom_status, "ms": 1, "totals": None})]

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


class NestedPendingTest(PendingCase):
    def test_handoff_exports_pending(self):
        b = self.planned([("pre", "Prechecks"), ("tests", "Tests"), ("install", "Install")])
        feed(b, step_start("pre", "Prechecks"), step_end("pre"), step_start("tests", "Tests"))
        self.assertEqual(b.pending(), [("install", "Install")])
        console = mock.Mock(mode="live", board=b)
        with mock.patch.object(jr, "_console", console), jr.handoff() as env:
            self.assertEqual(json.loads(env["JUNEAU_RUN_PENDING"]), [["install", "Install"]])

    def test_child_draws_parent_pending_dedup(self):
        pending = json.dumps([["install", "Install"], ["commit", "Commit"]])
        with mock.patch.dict(os.environ, {"JUNEAU_RUN_PENDING": pending}):
            b = self.planned([("install", "Install")], nested=True)
        feed(b, step_start("compile", "Compile"))
        self.assertEqual(b.pending(), [("install", "Install"), ("commit", "Commit")])
        self.assertEqual([line for line in self.live().region(b, 79, 18) if line.startswith("○")],
                         ["○ Install", "○ Commit"])
        feed(b, step_end("compile"), ("done", {"status": "ok", "commit": None}))
        self.assertEqual(b.pending(), [])


class LogTest(RowCase):
    def run_log(self, *events):
        with tempfile.TemporaryDirectory() as tmp:
            log = Path(tmp) / "c.log"
            c = jr.Console(jr.Sinks(console="none", condensed_log=str(log)), stream=io.StringIO(), clock=self.clock)
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


if __name__ == "__main__":
    unittest.main()
