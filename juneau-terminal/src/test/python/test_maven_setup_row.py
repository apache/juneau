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
"""The Setup row: pom modules leave the Maven bars for one board-level row, and the bars count real modules only."""

import os
import unittest

from support import jr, reset_run_state


def live_region_lines(board, rows=24, cols=100):
    live = jr.LiveRenderer(lambda text: None, size=lambda: os.terminal_size((cols, rows)), clock=lambda: 100.0,
                           color=False)
    return live.region(board, cols - 1, rows - 2)


def setup_events(poms=3, total=5, step="mvn"):
    ids = [f"pom{i}" for i in range(poms)]
    events = [("step_start", dict(id=step, n=1, title="mvn", parent=None)),
              ("reactor", dict(step=step, size=total, poms=ids, tests=False))]
    for pid in ids:
        events += [("unit_start", dict(step=step, id=pid, title=pid)),
                   ("unit_end", dict(step=step, id=pid, status="ok", ms=1))]
    events.append(("step_end", dict(id=step, status="ok", ms=2000, exit=0, summary=None, totals=None)))
    return events


def plain_run(events):
    out = []
    board = jr.Board("actionable", nested=False, full_log=None, clock=lambda: 100.0)
    renderer = jr.PlainRenderer(out.append, clock=lambda: 100.0)
    for kind, fields in events:
        renderer.render(board, board.apply(kind, fields))
    return "".join(out)


class SetupRowTest(unittest.TestCase):
    def board(self, poms=16, total=101, step="mvn"):
        reset_run_state()
        self.addCleanup(reset_run_state)
        b = jr.Board("actionable", nested=False, full_log=None, clock=lambda: 100.0)
        b.apply("step_start", dict(id=step, n=1, title="mvn", parent=None))
        ids = [f"pom{i}" for i in range(poms)]
        b.apply("reactor", dict(step=step, size=total, poms=ids, tests=False))
        return b, ids

    def test_units_exclude_poms(self):
        b, _ = self.board()
        r = b.rows["mvn"]
        self.assertEqual((r.units_total, r.unit_kind), (85, "modules"))

    def test_k_over_n_counts_real_modules_only(self):
        b, ids = self.board(poms=2, total=5)
        for i, pid in enumerate(ids):                       # poms end first
            b.apply("unit_start", dict(step="mvn", id=pid, title=pid))
            b.apply("unit_end", dict(step="mvn", id=pid, status="ok", ms=1))
        self.assertEqual(b.rows["mvn"].units_done, 0)
        b.apply("unit_start", dict(step="mvn", id="core", title="Core"))
        b.apply("unit_end", dict(step="mvn", id="core", status="ok", ms=1))
        self.assertEqual(b.rows["mvn"].units_done, 1)

    def test_setup_row_boxes_turn_green(self):
        b, ids = self.board(poms=3, total=5)
        b.apply("unit_start", dict(step="mvn", id=ids[0], title="p"))
        b.apply("unit_end", dict(step="mvn", id=ids[0], status="ok", ms=1))
        s = b.setup
        self.assertEqual((s.total, s.done), (3, 1))
        self.assertEqual(jr.setup_text(s), "3 pom modules")

    def test_setup_text_is_singular_for_one_pom(self):
        b, _ = self.board(poms=1, total=3)
        self.assertEqual(jr.setup_text(b.setup), "1 pom module")

    def test_not_repeated_for_a_later_maven_step(self):
        b, _ = self.board()
        first = b.setup
        b.apply("step_start", dict(id="mvn2", n=2, title="mvn2", parent=None))
        b.apply("reactor", dict(step="mvn2", size=10, poms=["x1"], tests=False))
        self.assertIs(b.setup, first)
        self.assertEqual(b.rows["mvn2"].units_total, 9)       # its own poms still excluded

    def test_a_resent_reactor_event_keeps_one_setup_row_and_follows_the_new_counts(self):
        b, _ = self.board(poms=0, total=101)                  # the first-line event: tree size, no poms yet
        self.assertIsNone(b.setup)
        self.assertEqual(b.rows["mvn"].units_total, 101)
        b.apply("reactor", dict(step="mvn", size=101, poms=[f"pom{i}" for i in range(16)], tests=False))
        self.assertEqual(b.rows["mvn"].units_total, 85)       # the Build Order block shrinks it
        self.assertEqual(b.setup.total, 16)
        b.apply("reactor", dict(step="mvn", size=101, poms=[f"pom{i}" for i in range(16)], tests=False))
        self.assertEqual((b.setup.total, b.rows["mvn"].units_total), (16, 85))

    def test_no_poms_no_setup_row(self):
        b, _ = self.board(poms=0, total=3)
        self.assertIsNone(b.setup)

    def test_total_never_negative(self):
        b, _ = self.board(poms=5, total=2)
        self.assertEqual(b.rows["mvn"].units_total, 0)

    def test_gray_cell_for_real_module_without_tests_stays(self):
        b, _ = self.board(poms=0, total=2)
        b.apply("unit_start", dict(step="mvn", id="jar1", title="J"))
        b.apply("unit_end", dict(step="mvn", id="jar1", status="skip", ms=1))
        self.assertEqual(b.rows["mvn"].units_done, 1)
        self.assertEqual(b.rows["mvn"].unit_cells[0], jr.UNIT_SKIP)

    def test_all_pom_reactor_has_an_empty_bar_and_a_setup_row(self):
        b, _ = self.board(poms=3, total=3)
        self.assertEqual(b.rows["mvn"].units_total, 0)
        self.assertEqual((b.setup.total, b.rows["mvn"].unit_kind), (3, "modules"))      # no fall back to the old bar

    def test_compile_mode_unit_advances_on_module_compiled_tests_mode_on_unit_end(self):
        b, _ = self.board(poms=0, total=2)                    # the board() helper sends tests=False: compile mode
        b.apply("unit_start", dict(step="mvn", id="mvn/a", title="A"))
        b.apply("module_compiled", dict(step="mvn", module="mvn/a", ok=True))
        self.assertEqual(b.rows["mvn"].units_done, 1)         # today's bar fills on compile in compile mode
        b.apply("unit_end", dict(step="mvn", id="mvn/a", status="ok", ms=1, totals=None))
        self.assertEqual(b.rows["mvn"].units_done, 1)         # and the end does not count it twice

    def test_tests_mode_unit_advances_on_unit_end_only(self):
        b, _ = self.board(poms=0, total=2)
        b.apply("reactor", dict(step="mvn", size=2, poms=[], tests=True))
        b.apply("unit_start", dict(step="mvn", id="mvn/a", title="A"))
        b.apply("module_compiled", dict(step="mvn", module="mvn/a", ok=True))
        self.assertEqual(b.rows["mvn"].units_done, 0)
        b.apply("unit_end", dict(step="mvn", id="mvn/a", status="fail", ms=1, totals=None))
        self.assertEqual(b.rows["mvn"].units_done, 1)
        self.assertEqual(b.rows["mvn"].unit_cells[0], jr.UNIT_FAIL)

    def test_region_draws_setup_above_first_owning_row_and_counts_height(self):
        b, _ = self.board()
        lines = live_region_lines(b, rows=24, cols=100)         # no plan is set, so no Overall line (G2) precedes it
        self.assertEqual(lines[0].split()[1], "Setup")
        self.assertIn("16 pom modules", lines[0])
        self.assertLessEqual(len(lines), 22)                   # max_lines = rows - 2 still honoured

    def test_setup_nests_under_its_step_on_a_multi_step_board(self):
        b, _ = self.board()
        b.apply("plan", dict(ids=["pre", "mvn", "post"], labels={"pre": "Pre", "mvn": "Mvn", "post": "Post"}))
        lines = live_region_lines(b, rows=24, cols=100)
        i = next(n for n, l in enumerate(lines) if "Setup" in l)
        self.assertIn("0/", lines[i - 1])                   # the step's row first
        self.assertTrue(lines[i].startswith("    "))        # the Setup row indented under it
        self.assertIn("16 pom modules", lines[i])

    def test_nested_setup_stays_under_its_row_when_poms_finish(self):
        b, ids = self.board(poms=2, total=5)
        b.apply("plan", dict(ids=["pre", "mvn"], labels={"pre": "Pre", "mvn": "Mvn"}))
        for pid in ids:
            b.apply("unit_start", dict(step="mvn", id=pid, title=pid))
            b.apply("unit_end", dict(step="mvn", id=pid, status="ok", ms=1))
        lines = live_region_lines(b, rows=24, cols=100)
        i = next(n for n, l in enumerate(lines) if "Setup" in l)
        self.assertIn("0/", lines[i - 1])

    def test_single_command_board_keeps_setup_on_top(self):
        b, _ = self.board()
        lines = live_region_lines(b, rows=24, cols=100)
        self.assertFalse(lines[0].startswith("    "))

    def test_region_budget_takes_the_setup_line_off_the_sub_lines(self):
        b, _ = self.board(poms=2, total=40)
        for i in range(30):
            b.apply("unit_start", dict(step="mvn", id=f"m{i}", title=f"M{i}"))
        lines = live_region_lines(b, rows=12, cols=100)
        self.assertLessEqual(len(lines), 10)

    def test_plain_renderer_prints_one_setup_line(self):
        out = plain_run(setup_events())
        self.assertEqual(len([l for l in out.splitlines() if l.split()[1:2] == ["Setup"]]), 1)
        self.assertIn("3 pom modules", out)


    def test_tests_mode_counts_a_unit_when_its_tests_finish_not_only_at_the_summary(self):
        b, _ = self.board(poms=0, total=2)                    # -T: unit_end can wait for the Reactor Summary
        b.apply("reactor", dict(step="mvn", size=2, poms=[], tests=True))
        b.apply("unit_start", dict(step="mvn", id="mvn/a", title="A"))
        b.apply("unit_dot", dict(step="mvn", id="mvn/a", outcome="pass"))
        b.apply("module_tested", dict(step="mvn", module="mvn/a"))
        self.assertEqual(b.rows["mvn"].units_done, 1)
        self.assertEqual(b.rows["mvn"].unit_cells[0], jr.UNIT_PASS)
        b.apply("unit_end", dict(step="mvn", id="mvn/a", status="ok", ms=1, totals=None))
        self.assertEqual(b.rows["mvn"].units_done, 1)         # counted once

    def test_tested_failure_and_a_module_without_tests(self):
        b, _ = self.board(poms=0, total=3)
        b.apply("reactor", dict(step="mvn", size=3, poms=[], tests=True))
        b.apply("unit_dot", dict(step="mvn", id="mvn/a", outcome="fail", src="sub"))     # the class_done translation
        b.apply("module_tested", dict(step="mvn", module="mvn/a"))
        b.apply("module_tested", dict(step="mvn", module="mvn/b"))
        r = b.rows["mvn"]
        # mvn/b ran no tests: it leaves the bar and the total instead of drawing a skip cell
        self.assertEqual((r.units_done, r.units_total, r.unit_cells[0]), (1, 2, jr.UNIT_FAIL))
        self.assertEqual(r.units_dropped, 1)

    def mk_tests_board(self, total=4):
        b, _ = self.board(poms=0, total=total)
        b.apply("reactor", dict(step="mvn", size=total, poms=[], tests=True))
        return b, b.rows["mvn"]

    def bar(self, r, glyphs=None):
        return "".join(jr.unit_bar(r.unit_view(), r.units_total, glyphs or jr.PLAIN_GLYPHS))

    def test_a_module_that_ran_no_tests_is_dropped_from_the_bar_and_the_total(self):
        b, r = self.mk_tests_board(4)
        self.assertEqual(self.bar(r), jr.PENDING * 4)
        for m in ("a", "b"):                    # upstream modules from --also-make: compile only, no tests
            b.apply("unit_start", dict(step="mvn", id=f"mvn/{m}", title=m))
            b.apply("module_tested", dict(step="mvn", module=f"mvn/{m}"))
            b.apply("unit_end", dict(step="mvn", id=f"mvn/{m}", status="ok", ms=1, totals=None))
        self.assertEqual((r.units_done, r.units_total, r.units_dropped), (0, 2, 2))    # N shrinks as they drop out
        self.assertEqual(self.bar(r), jr.PENDING * 2)
        b.apply("unit_start", dict(step="mvn", id="mvn/c", title="c"))
        b.apply("unit_dot", dict(step="mvn", id="mvn/c", outcome="pass"))
        b.apply("module_tested", dict(step="mvn", module="mvn/c"))
        b.apply("unit_end", dict(step="mvn", id="mvn/c", status="ok", ms=1, totals=(1, 0, 0, 0)))
        self.assertEqual((r.units_done, r.units_total), (1, 2))
        self.assertEqual(self.bar(r), "#" + jr.PENDING)
        _, rest, _ = jr.row_cells(r, jr.PLAIN_GLYPHS, 100.0, False)
        self.assertIn("1/2", rest)

    def test_a_dropped_module_survives_a_reactor_resend(self):
        b, r = self.mk_tests_board(4)
        b.apply("unit_start", dict(step="mvn", id="mvn/a", title="a"))
        b.apply("module_tested", dict(step="mvn", module="mvn/a"))
        b.apply("reactor", dict(step="mvn", size=4, poms=[], tests=True))
        self.assertEqual(r.units_total, 3)

    def test_a_module_whose_tests_were_all_skipped_still_shows_skip(self):
        b, r = self.mk_tests_board(2)
        b.apply("unit_start", dict(step="mvn", id="mvn/a", title="a"))
        b.apply("unit_dot", dict(step="mvn", id="mvn/a", outcome="skip"))
        b.apply("module_tested", dict(step="mvn", module="mvn/a"))
        b.apply("unit_end", dict(step="mvn", id="mvn/a", status="ok", ms=1, totals=(2, 0, 0, 2)))
        self.assertEqual((r.units_done, r.units_total), (1, 2))
        self.assertEqual(self.bar(r), "." + jr.PENDING)         # skip, not dropped

    def test_a_dropped_module_that_fails_counts_after_all(self):
        b, r = self.mk_tests_board(2)
        b.apply("unit_start", dict(step="mvn", id="mvn/a", title="a"))
        b.apply("module_tested", dict(step="mvn", module="mvn/a"))
        b.apply("unit_end", dict(step="mvn", id="mvn/a", status="fail", ms=1, totals=None))
        self.assertEqual((r.units_done, r.units_total, r.units_dropped), (1, 2, 0))
        self.assertEqual(self.bar(r), "F" + jr.PENDING)

    def test_compile_rows_still_count_every_module(self):
        b, _ = self.board(poms=0, total=3)
        r = b.rows["mvn"]
        b.apply("unit_start", dict(step="mvn", id="mvn/a", title="a"))
        b.apply("unit_end", dict(step="mvn", id="mvn/a", status="ok", ms=1, totals=None))
        self.assertEqual((r.units_done, r.units_total, r.units_dropped), (1, 3, 0))

    def test_started_modules_are_in_flight_until_they_end(self):
        b, r = self.mk_tests_board(5)
        for m in ("a", "b"):
            b.apply("unit_start", dict(step="mvn", id=f"mvn/{m}", title=m))
        self.assertEqual(self.bar(r), "~~" + jr.PENDING * 3)
        self.assertEqual(self.bar(r, jr.NO_COLOR_GLYPHS), "▣▣" + jr.PENDING * 3)
        self.assertEqual(self.bar(r, jr.COLOR_GLYPHS), "\x1b[36m■\x1b[0m" * 2 + "\x1b[2m□\x1b[0m" * 3)   # unstarted cells are dim
        self.assertNotEqual(jr.COLOR_GLYPHS["run"], jr.COLOR_GLYPHS["warn"])
        b.apply("unit_dot", dict(step="mvn", id="mvn/a", outcome="pass"))
        b.apply("module_tested", dict(step="mvn", module="mvn/a"))
        self.assertEqual(self.bar(r), "#~" + jr.PENDING * 3)        # a finished (counted), b still running
        b.apply("unit_end", dict(step="mvn", id="mvn/a", status="ok", ms=1, totals=(1, 0, 0, 0)))
        b.apply("step_end", dict(id="mvn", status="ok", ms=2000, exit=0, summary=None, totals=None))
        self.assertNotIn("~", self.bar(r))                           # a finished row shows no in-flight cells

    def test_a_pom_in_flight_alone_shows_one_run_cell_until_it_ends(self):
        b, ids = self.board(poms=1, total=4)
        r = b.rows["mvn"]
        b.apply("unit_start", dict(step="mvn", id=ids[0], title="root"))
        self.assertEqual(self.bar(r), "~" + jr.PENDING * 2)
        b.apply("unit_end", dict(step="mvn", id=ids[0], status="ok", ms=1, totals=None))
        self.assertEqual(self.bar(r), jr.PENDING * 3)

    def test_a_pom_in_flight_adds_no_run_cell_beside_a_running_module(self):
        b, ids = self.board(poms=1, total=4)
        r = b.rows["mvn"]
        b.apply("unit_start", dict(step="mvn", id=ids[0], title="root"))
        b.apply("unit_start", dict(step="mvn", id="mvn/a", title="a"))
        self.assertEqual(self.bar(r), "~" + jr.PENDING * 2)

    def test_a_module_compiled_before_it_starts_is_not_counted_until_its_end(self):
        b, _ = self.board(poms=0, total=2)
        r = b.rows["mvn"]
        b.apply("module_compiled", dict(step="mvn", module="mvn/a", ok=True))
        self.assertEqual(r.units_done, 0)
        b.apply("unit_start", dict(step="mvn", id="mvn/a", title="A"))
        self.assertEqual((r.units_done, self.bar(r)), (0, "~" + jr.PENDING))
        b.apply("unit_end", dict(step="mvn", id="mvn/a", status="ok", ms=1, totals=None))
        self.assertEqual((r.units_done, self.bar(r)), (1, "#" + jr.PENDING))

    def test_a_modules_bar_without_unit_start_never_shows_in_flight(self):
        b, r = self.mk_tests_board(3)
        b.apply("unit_dot", dict(step="mvn", id="mvn/a", outcome="pass", src="sub"))    # no unit_start: no signal
        self.assertEqual(self.bar(r), jr.PENDING * 3)

    def test_each_unit_keeps_its_own_cell_failed_or_in_flight(self):
        cells = bytearray([jr.UNIT_FAIL, jr.UNIT_RUN])
        self.assertEqual("".join(jr.unit_bar(cells, 2, jr.PLAIN_GLYPHS)), "F~")
        cells = bytearray([jr.UNIT_PASS, jr.UNIT_RUN])
        self.assertEqual("".join(jr.unit_bar(cells, 2, jr.PLAIN_GLYPHS)), "#~")

    def test_a_pom_box_turns_green_when_it_starts(self):
        b, ids = self.board(poms=3, total=5)
        b.apply("unit_start", dict(step="mvn", id=ids[1], title="p"))     # -T: its end waits for the summary
        self.assertEqual((b.setup.done, b.setup.boxes[ids[1]]), (1, "pass"))
        b.apply("unit_end", dict(step="mvn", id=ids[1], status="fail", ms=1))
        self.assertEqual(b.setup.boxes[ids[1]], "fail")

    def test_pom_modules_never_enter_the_row_order(self):
        b, ids = self.board(poms=1, total=3)
        b.apply("unit_start", dict(step="mvn", id=ids[0], title="p"))
        b.apply("unit_end", dict(step="mvn", id=ids[0], status="ok", ms=1))
        b.apply("step_end", dict(id="mvn", status="ok", ms=2000, exit=0, summary=None, totals=None))
        self.assertEqual(b.rows["mvn"].order, [])

    def test_duplicate_pom_ids_let_done_reach_total(self):
        b, _ = self.board(poms=0, total=4)
        b.apply("reactor", dict(step="mvn", size=4, poms=["p", "p", "q"], tests=False))
        for pid in ("p", "q"):
            b.apply("unit_end", dict(step="mvn", id=pid, status="ok", ms=1))
        self.assertEqual((b.setup.total, b.setup.done), (2, 2))

    def test_close_of_a_failed_module_by_finish_counts_a_fail_unit(self):
        b, _ = self.board(poms=0, total=2)
        b.apply("reactor", dict(step="mvn", size=2, poms=[], tests=False))
        b.apply("unit_start", dict(step="mvn", id="mvn/a", title="A"))
        b.apply("module_compiled", dict(step="mvn", module="mvn/a", ok=False))      # box fail; compile mode counts it
        b.apply("unit_start", dict(step="mvn", id="mvn/b", title="B"))
        b.apply("reactor", dict(step="mvn", size=2, poms=[], tests=True))
        b.apply("module_compiled", dict(step="mvn", module="mvn/b", ok=False))      # fail box, not counted in tests mode
        b.apply("unit_end", dict(step="mvn", id="mvn/b", status="skip", ms=1, totals=None))   # finish() closes it
        r = b.rows["mvn"]
        self.assertEqual((r.units_done, r.unit_cells[1]), (2, jr.UNIT_FAIL))

    def test_quiet_build_with_no_module_events_shows_no_bar_when_finished(self):
        b, _ = self.board(poms=0, total=101)                  # a -q build: only the first-line reactor event arrived
        r = b.rows["mvn"]
        cells, rest, _ = jr.row_cells(r, jr.NO_COLOR_GLYPHS, 100.0, False)
        self.assertIsNotNone(cells)                           # still running: the bar shows
        b.apply("step_end", dict(id="mvn", status="ok", ms=5000, exit=0, summary=None, totals=None))
        cells, rest, _ = jr.row_cells(r, jr.NO_COLOR_GLYPHS, 100.0, False)
        self.assertIsNone(cells)
        self.assertNotIn("0/101", rest)

    def test_finished_row_with_some_units_keeps_its_bar(self):
        b, _ = self.board(poms=0, total=3)
        b.apply("unit_start", dict(step="mvn", id="mvn/a", title="A"))
        b.apply("unit_end", dict(step="mvn", id="mvn/a", status="ok", ms=1))
        b.apply("step_end", dict(id="mvn", status="fail", ms=5000, exit=1, summary=None, totals=None))
        cells, rest, _ = jr.row_cells(b.rows["mvn"], jr.NO_COLOR_GLYPHS, 100.0, False)
        self.assertIsNotNone(cells)
        self.assertIn("1/3", rest)


class TrailingPomTest(unittest.TestCase):
    """A pom built after a real module (an aggregator root) is on neither the Setup row nor the module bar."""

    def board(self, poms, tail, total):
        reset_run_state()
        self.addCleanup(reset_run_state)
        b = jr.Board("actionable", nested=False, full_log=None, clock=lambda: 100.0)
        b.apply("step_start", dict(id="mvn", n=1, title="mvn", parent=None))
        b.apply("reactor", dict(step="mvn", size=total, poms=poms, tail=tail, tests=False))
        return b

    def run_module(self, b, module, status="ok"):
        b.apply("unit_start", dict(step="mvn", id=module, title=module))
        return b.apply("unit_end", dict(step="mvn", id=module, status=status, ms=1, totals=None))

    def test_jar_war_then_a_trailing_pom_has_no_setup_row_and_a_bar_of_two(self):
        b = self.board([], ["mvn/app"], 3)
        self.assertIsNone(b.setup)
        self.assertEqual((b.rows["mvn"].units_total, b.rows["mvn"].unit_kind), (2, "modules"))
        for m in ("mvn/app-api", "mvn/app-server", "mvn/app"):
            self.run_module(b, m)
        b.apply("step_end", dict(id="mvn", status="ok", ms=2000, exit=0, summary=None, totals=None))
        r = b.rows["mvn"]
        self.assertIsNone(b.setup)
        self.assertEqual((r.units_total, r.units_done), (2, 2))
        self.assertNotIn("mvn/app", r.order)

    def test_a_trailing_pom_never_shows_as_in_flight_in_the_bar(self):
        b = self.board([], ["mvn/app"], 3)
        b.apply("unit_start", dict(step="mvn", id="mvn/app", title="app"))
        self.assertEqual(b.rows["mvn"].unit_view(), bytearray(2))

    def test_leading_parent_and_trailing_aggregator_give_a_setup_row_of_one(self):
        b = self.board(["mvn/parent"], ["mvn/agg"], 4)
        self.assertEqual((b.setup.total, jr.setup_text(b.setup)), (1, "1 pom module"))
        self.assertEqual(b.rows["mvn"].units_total, 2)
        self.run_module(b, "mvn/parent")
        self.assertEqual((b.setup.done, b.setup.ended), (1, True))

    def test_a_resent_reactor_event_keeps_the_trailing_pom_out(self):
        b = self.board(["mvn/parent"], ["mvn/agg"], 4)
        b.apply("reactor", dict(step="mvn", size=4, poms=["mvn/parent"], tail=["mvn/agg"], tests=False))
        self.assertEqual((b.setup.ids, b.rows["mvn"].units_total), (["mvn/parent"], 2))

    def test_an_event_without_tail_behaves_as_before(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        b = jr.Board("actionable", nested=False, full_log=None, clock=lambda: 100.0)
        b.apply("step_start", dict(id="mvn", n=1, title="mvn", parent=None))
        b.apply("reactor", dict(step="mvn", size=4, poms=["mvn/parent"], tests=False))
        self.assertEqual((b.setup.total, b.rows["mvn"].units_total), (1, 3))

    def test_a_failing_trailing_pom_fails_the_row_and_is_named(self):
        b = self.board([], ["mvn/app"], 3)
        self.run_module(b, "mvn/app-api")
        self.run_module(b, "mvn/app-server")
        self.run_module(b, "mvn/app", status="fail")
        b.apply("step_end", dict(id="mvn", status="fail", ms=2000, exit=1, summary=None, totals=None))
        r = b.rows["mvn"]
        self.assertIsNone(b.setup)
        self.assertEqual(r.failed_units, ["mvn/app"])
        self.assertEqual(r.units["mvn/app"].box, "fail")
        # a trailing pom has no cell until it fails: then it draws a red one, so the bar never reads all green
        self.assertEqual((r.units_total, r.units_done, list(r.unit_cells)), (3, 3, [jr.UNIT_PASS, jr.UNIT_PASS, jr.UNIT_FAIL]))

    def test_a_trailing_pom_that_fails_before_the_other_modules_end_still_draws_a_red_cell(self):
        b = self.board([], ["mvn/app"], 3)          # -T: the summary can end the failed module first
        self.run_module(b, "mvn/app", status="fail")
        self.run_module(b, "mvn/app-api")
        self.run_module(b, "mvn/app-server")
        self.assertEqual(list(b.rows["mvn"].unit_cells), [jr.UNIT_FAIL, jr.UNIT_PASS, jr.UNIT_PASS])

    def test_a_module_that_passed_its_tests_then_fails_a_later_goal_ends_red(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        b = jr.Board("actionable", nested=False, full_log=None, clock=lambda: 100.0)
        b.apply("step_start", dict(id="mvn", n=1, title="mvn", parent=None))
        b.apply("reactor", dict(step="mvn", size=2, poms=[], tail=["mvn/run"], tests=True))
        for m in ("mvn/a", "mvn/run"):              # run is a pom-packaged wrapper whose exec goal runs more tests
            b.apply("unit_start", dict(step="mvn", id=m, title=m, src="sub"))
            b.apply("unit_dot", dict(step="mvn", id=m, outcome="pass", src="sub"))
            b.apply("module_tested", dict(step="mvn", module=m))
        b.apply("unit_end", dict(step="mvn", id="mvn/run", status="fail", ms=1, totals=None, src="sub"))
        b.apply("unit_end", dict(step="mvn", id="mvn/a", status="ok", ms=1, totals=None, src="sub"))
        r = b.rows["mvn"]
        self.assertEqual((r.failed_units, list(r.unit_cells)), (["mvn/run"], [jr.UNIT_PASS, jr.UNIT_FAIL]))


class TrailingPomGoldenTest(unittest.TestCase):
    """The same rule end to end: the recorded jar, war, pom reactor order through the Maven parser."""

    def tree(self, root):
        from pathlib import Path
        root = Path(root)

        def write(d, artifact, packaging, modules=""):
            (root / d).mkdir(exist_ok=True)
            (root / d / "pom.xml").write_text(f"<project><artifactId>{artifact}</artifactId><name>{artifact}</name>"
                                              f"<packaging>{packaging}</packaging>{modules}</project>", encoding="utf-8")
        write(".", "app", "pom", "<modules><module>app-api</module><module>app-server</module></modules>")
        write("app-api", "app-api", "jar")
        write("app-server", "app-server", "war")

    def run_log(self, text):
        import tempfile
        from test_parsers import feed
        reset_run_state()
        self.addCleanup(reset_run_state)
        with tempfile.TemporaryDirectory() as tmp:
            self.tree(tmp)
            parser = jr.MavenParser()
            parser.step_id = "mvn"
            parser.prepare(["mvn", "test"], None, tmp)
            events = feed(parser, text)
        board = jr.Board("actionable", nested=False, full_log=None, clock=lambda: 100.0)
        board.apply("step_start", dict(id="mvn", n=1, title="mvn", parent=None))
        pump(board, events)
        return board, board.rows["mvn"]

    def fixture(self):
        from support import FIXTURES
        return (FIXTURES / "maven-trailing-pom.log").read_text(encoding="utf-8")

    def test_the_recorded_order_has_no_setup_row_and_a_full_bar_of_two(self):
        board, r = self.run_log(self.fixture())
        board.apply("step_end", dict(id="mvn", status="ok", ms=3000, exit=0, summary=None, totals=None))
        self.assertIsNone(board.setup)
        self.assertEqual((r.units_total, r.units_done), (2, 2))

    def test_the_recorded_order_with_a_failing_aggregator_names_it(self):
        text = self.fixture().replace("app ................................................ SUCCESS",
                                      "app ................................................ FAILURE").replace(
                                          "BUILD SUCCESS", "BUILD FAILURE")
        board, r = self.run_log(text)
        board.apply("step_end", dict(id="mvn", status="fail", ms=3000, exit=1, summary=None, totals=None))
        self.assertIsNone(board.setup)
        self.assertEqual(r.failed_units, ["app"])
        self.assertEqual((r.units_total, r.units_done, r.unit_cells[2]), (3, 3, jr.UNIT_FAIL))     # the failed aggregator gets a red cell


def pump(board, events, step="mvn"):
    """Feed parser events to a board the way the dispatcher publishes them; returns the bar progress after each event."""
    seen, totals = [], {}
    for ev in events:
        kind = ev[0]
        if kind == "reactor":
            board.apply("reactor", dict(step=step, size=ev[1], poms=list(ev[2]), tests=ev[3],
                                        tail=list(ev[4]) if len(ev) > 4 else []))
        elif kind == "sub_start":
            board.apply("unit_start", dict(step=step, id=ev[1], title=ev[3]))
        elif kind == "tests":
            totals[ev[1]] = ev[2:]
        elif kind == "sub_end":
            t = totals.get(ev[1])
            board.apply("unit_end", dict(step=step, id=ev[1], status=ev[2], ms=ev[3],
                                           totals=dict(total=t[0], fail=t[1], err=t[2], skip=t[3]) if t else None))
        elif kind == "compiled":
            board.apply("module_compiled", dict(step=step, module=ev[1], ok=ev[2]))
        elif kind == "tested":
            board.apply("module_tested", dict(step=step, module=ev[1]))
        elif kind == "class_done":
            board.apply("unit_dot", dict(step=step, id=ev[1], outcome=ev[2], src="sub"))   # as _Dispatcher does
        seen.append((kind, board.rows[step].units_done, board.setup.done if board.setup else 0, ev[1]))
    return seen


class MavenBoardGoldenTest(unittest.TestCase):
    def run_fixture(self, fixture, args, poms_in_tree):
        import tempfile
        from pathlib import Path
        from test_parsers import feed, write_demo_tree
        from support import FIXTURES
        reset_run_state()
        self.addCleanup(reset_run_state)
        with tempfile.TemporaryDirectory() as tmp:
            write_demo_tree(Path(tmp), packaging="pom")
            parser = jr.MavenParser()
            parser.step_id = "mvn"
            parser.prepare(["mvn", *args], None, tmp)
            if not poms_in_tree:
                parser.names = {}
            events = feed(parser, (FIXTURES / fixture).read_text(encoding="utf-8"))
        board = jr.Board("actionable", nested=False, full_log=None, clock=lambda: 100.0)
        board.apply("step_start", dict(id="mvn", n=1, title="mvn", parent=None))
        seen = pump(board, events)
        return board, board.rows["mvn"], seen

    def test_serial_build_excludes_the_pom_from_the_bar(self):
        board, r, _ = self.run_fixture("maven-serial-success.log", ("test",), True)
        board.apply("step_end", dict(id="mvn", status="ok", ms=3000, exit=0, summary=None, totals=None))
        self.assertEqual((r.units_total, r.units_done), (3, 3))
        self.assertEqual((board.setup.total, board.setup.done, jr.setup_text(board.setup)), (1, 1, "1 pom module"))

    def test_parallel_build_progresses_before_the_reactor_summary(self):
        board, r, seen = self.run_fixture("maven-parallel-success.log", ("test", "-T", "2"), True)
        self.assertEqual((r.units_total, r.units_done), (2, 2))    # the bar ends full; one module ran no tests
        summary_at = next(i for i, e in enumerate(seen) if e[0] == "sub_end" and e[3] != "mvn/demo-parent")
        before = max(e[1] for e in seen[:summary_at])
        self.assertGreater(before, 0)                         # intermediate progress, ahead of the first module_end
        self.assertEqual(board.setup.done, 1)
        first_pom_green = next(i for i, e in enumerate(seen) if e[2] == 1)
        self.assertLess(first_pom_green, summary_at)          # the pom box does not wait for the summary

    def test_juneau_t1c_log_counts_real_modules_and_fills_the_bar(self):
        import re
        from test_parsers import feed
        from support import FIXTURES
        reset_run_state()
        self.addCleanup(reset_run_state)
        text = (FIXTURES / "maven-juneau-T1C.log").read_text(encoding="utf-8")
        block = text.split("Reactor Build Order:")[1].split("\n[INFO] \n")[1].split("\n[INFO] \n")[0]
        members = re.findall(r"^\[INFO\] (.+?)\s+\[(\w[\w-]*)\]\s*$", block, re.M)
        pom_titles = [t for t, pkg in members if pkg == "pom"]
        parser = jr.MavenParser()
        parser.step_id = "mvn"
        parser.prepare(["mvn", "test", "-T", "1C"], None, "/nonexistent")
        parser.names = {t: f"pom{i}" for i, t in enumerate(pom_titles)}
        events = feed(parser, text)
        board = jr.Board("actionable", nested=False, full_log=None, clock=lambda: 100.0)
        board.apply("step_start", dict(id="mvn", n=1, title="mvn", parent=None))
        seen = pump(board, events)
        board.apply("step_end", dict(id="mvn", status="ok", ms=3000, exit=0, summary=None, totals=None))
        r = board.rows["mvn"]
        self.assertEqual(len(members), 96)
        self.assertEqual(len(pom_titles), 16)
        # 96 modules less 16 poms is 80, but only juneau-commons ran tests: the other 79 dropped out of the bar
        self.assertEqual((r.units_total, r.unit_kind, r.units_dropped), (1, "modules", 79))
        self.assertEqual(r.units_done, 1)
        # only the 3 poms ahead of the first jar are Setup poms; the 13 built after it are on neither the row nor the bar
        self.assertEqual((board.setup.total, board.setup.done, jr.setup_text(board.setup)), (3, 3, "3 pom modules"))
        first_end = next(i for i, e in enumerate(seen) if e[0] == "sub_end")
        self.assertGreater(max(e[1] for e in seen[:first_end]), 0)


if __name__ == "__main__":
    unittest.main()


class UnitGoalTest(unittest.TestCase):
    def test_sub_row_shows_the_goal_after_tests_finish(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        b = jr.Board("actionable", nested=False, full_log=None, clock=lambda: 100.0)
        b.apply("step_start", dict(id="mvn", n=1, title="mvn", parent=None))
        b.apply("unit_start", dict(step="mvn", id="mvn/a", title="a"))
        b.apply("unit_dot", dict(step="mvn", id="mvn/a", outcome="pass", src="sub"))
        b.apply("unit_goal", dict(step="mvn", id="mvn/a", goal="gpg:sign"))
        lines = live_region_lines(b)
        self.assertTrue(any(l.strip().startswith("a") and "gpg:sign" in l for l in lines), lines)


class StepSummaryAfterCommandTest(unittest.TestCase):
    def test_step_summary_follows_the_command_totals_in_one_row(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        out = []
        b = jr.Board("actionable", nested=False, full_log=None, clock=lambda: 100.0)
        r = jr.PlainRenderer(out.append, clock=lambda: 100.0)
        totals = dict(total=5, fail=0, err=0, skip=0)
        for kind, f in [("step_start", dict(id="rp", n=11, title="Release perform", parent=None)),
                        ("step_start", dict(id="rp-run", n=1, title="Release perform", parent="rp")),
                        ("reactor", dict(step="rp-run", size=2, poms=[], tests=True)),
                        ("step_end", dict(id="rp-run", status="ok", ms=1000, exit=0, summary=None, totals=totals)),
                        ("step_end", dict(id="rp", status="ok", ms=1100, exit=0, summary="staged in x", totals=None))]:
            r.render(b, b.apply(kind, f))
        rows = [l for l in "".join(out).splitlines() if "Release perform" in l]
        self.assertEqual(len(rows), 1, rows)
        self.assertIn("5 tests", rows[0])
        self.assertIn("staged in x", rows[0])
