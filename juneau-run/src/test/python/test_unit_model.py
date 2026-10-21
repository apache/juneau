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
"""The board's Unit model: unit_start / unit_dot / unit_end for each src, and the gates that keep file and script units
out of the Maven module bar, the Setup row, the heartbeat and History."""

import unittest

from support import jr, reset_run_state


class Clock:
    def __init__(self):
        self.t = 100.0

    def __call__(self):
        return self.t


class UnitModelTest(unittest.TestCase):
    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        self.clock = Clock()
        self.board = jr.Board(clock=self.clock)
        self.board.apply("step_start", {"id": "s", "n": 1, "title": "Script tests", "parent": None})
        self.row = self.board.rows["s"]

    def apply(self, kind, **f):
        return self.board.apply(kind, dict(step="s", **f))

    def test_each_src_opens_a_unit_with_its_src(self):
        for src in ("sub", "row", "file", "script"):
            self.apply("unit_start", id=f"u-{src}", title=f"U {src}", src=src)
        self.assertEqual([(u.id, u.title, u.src) for u in self.row.units.values()],
                         [("u-sub", "U sub", "sub"), ("u-row", "U row", "row"), ("u-file", "U file", "file"),
                          ("u-script", "U script", "script")])
        self.assertTrue(all(u.in_progress for u in self.row.units.values()))

    def test_a_missing_src_is_sub(self):
        self.apply("unit_start", id="m", title="M")
        self.assertEqual(self.row.units["m"].src, "sub")

    def test_dots_append_outcomes(self):
        self.apply("unit_start", id="f", title="F", src="file")
        self.apply("unit_dot", id="f", outcome="pass")
        self.apply("unit_dot", id="f", outcome="fail")
        self.assertEqual(self.row.units["f"].outcomes, ["pass", "fail"])

    def test_an_orphan_dot_opens_its_unit_titled_by_its_id_with_no_bar(self):
        self.clock.t = 105.0
        self.apply("unit_dot", id="o", outcome="pass")
        u = self.row.units["o"]
        self.assertEqual((u.title, u.t0, u.outcomes, u.src), ("o", 105.0, ["pass"], "script"))
        self.assertNotIn("o", self.board.unit_rows)
        self.assertFalse(self.row.bar_units)

    def test_an_orphan_maven_dot_opens_a_module(self):
        self.apply("unit_dot", id="m", outcome="pass", src="sub")      # _Dispatcher's class_done translation
        self.assertEqual(self.row.units["m"].src, "sub")
        self.assertIs(self.board.unit_rows["m"], self.row)

    def test_a_repeat_start_resets_t0_for_sub_and_row(self):
        for src in ("sub", "row"):
            with self.subTest(src=src):
                self.clock.t = 100.0
                self.apply("unit_start", id=src, title="old", src=src)
                self.clock.t = 110.0
                self.apply("unit_start", id=src, title="new", src=src)
                self.assertEqual((self.row.units[src].title, self.row.units[src].t0), ("new", 110.0))

    def test_a_repeat_start_keeps_an_open_file_or_script_unit(self):
        for src in ("file", "script"):
            with self.subTest(src=src):
                self.clock.t = 100.0
                self.apply("unit_start", id=src, title="old", src=src)
                self.apply("unit_dot", id=src, outcome="pass")
                self.clock.t = 110.0
                self.apply("unit_start", id=src, title="new", src=src)
                u = self.row.units[src]
                self.assertEqual((u.title, u.t0, u.outcomes), ("old", 100.0, ["pass"]))

    def test_a_start_after_a_file_or_script_unit_closed_opens_it_fresh(self):
        self.apply("unit_start", id="f", title="F", src="file")
        self.apply("unit_dot", id="f", outcome="fail")
        self.apply("unit_end", id="f", status="fail", src="file")
        self.clock.t = 120.0
        self.apply("unit_start", id="f", title="F", src="file")
        u = self.row.units["f"]
        self.assertEqual((u.status, u.t0, u.outcomes), (None, 120.0, []))

    def test_file_and_script_units_end_without_a_box_a_cell_or_a_finished_line(self):
        self.board.detail = "all"
        for src in ("file", "script"):
            with self.subTest(src=src):
                self.apply("unit_start", id=src, title=src, src=src)
                self.clock.t += 2
                blocks = self.apply("unit_end", id=src, status="fail", src=src)
                u = self.row.units[src]
                self.assertEqual((u.status, u.ms, u.box, u.unit_index), ("fail", 2000, None, None))
                self.assertEqual(blocks, [])
        # a failed file names its file in the row's text; a failed script unit never does
        self.assertEqual((self.row.order, self.row.units_done, self.row.failed_units), ([], 0, ["file"]))

    def test_a_file_unit_with_a_fail_dot_is_named_once(self):
        self.apply("unit_start", id="f", title="test_f.py", src="file")
        self.apply("unit_dot", id="f", outcome="fail")
        self.apply("unit_dot", id="f", outcome="fail")
        self.apply("unit_end", id="f", status="ok", src="file")
        self.assertEqual(self.row.failed_units, ["test_f.py"])

    def test_a_fail_dot_on_a_closed_file_unit_names_it_without_reopening_it(self):
        self.apply("unit_start", id="f", title="test_f.py", src="file")
        self.apply("unit_end", id="f", status="ok", src="file")
        self.apply("unit_dot", id="f", outcome="fail")       # a late teardown error
        u = self.row.units["f"]
        self.assertEqual((u.status, u.in_progress, self.row.failed_units), ("ok", False, ["test_f.py"]))

    def test_a_failed_script_unit_is_never_named(self):
        self.apply("unit_start", id="c", title="identity", src="script")
        self.apply("unit_dot", id="c", outcome="fail")
        self.apply("unit_end", id="c", status="fail", src="script")
        self.assertEqual(self.row.failed_units, [])

    def test_the_close_pass_closes_open_files_fail_but_names_only_those_with_a_fail_dot(self):
        self.apply("unit_start", id="f", title="test_f.py", src="file")
        self.apply("unit_start", id="g", title="test_g.py", src="file")
        self.apply("unit_dot", id="f", outcome="fail")
        self.board.apply("step_end", {"id": "s", "status": "fail", "ms": 1, "exit": 1, "summary": None,
                                      "totals": None})
        self.assertEqual([self.row.units[i].status for i in ("f", "g")], ["fail", "fail"])  # g ends fail with its row
        self.assertEqual(self.row.failed_units, ["test_f.py"])     # an interrupted run doesn't name every open file

    def test_the_close_pass_does_not_name_an_open_file_of_a_passing_row(self):
        self.apply("unit_start", id="f", title="test_f.py", src="file")
        self.board.apply("step_end", {"id": "s", "status": "ok", "ms": 1, "exit": 0, "summary": None,
                                      "totals": None})
        self.assertEqual((self.row.units["f"].status, self.row.failed_units), ("ok", []))

    def feed_playwright(self, lines):
        """The unit events a PlaywrightParser makes of lines, then of its finish(1), applied to the board."""
        parser = jr.PlaywrightParser()
        parser.step_id = "s"
        for ev in [e for line in lines for e in parser.on_line(line)] + parser.finish(1):
            if ev[0] == "unit_start":
                self.apply("unit_start", id=ev[1], title=ev[2], src=ev[3])
            elif ev[0] == "unit_dot":
                self.apply("unit_dot", id=ev[1], outcome=ev[2])
            elif ev[0] == "unit_end":
                self.apply("unit_end", id=ev[1], status=ev[2], src=ev[3])

    def test_an_interrupted_playwright_run_names_only_its_failed_file_and_still_fails(self):
        lines = ['##playwright {"ev":"hello","v":1}', '##playwright {"ev":"begin","f":{"a.ts":3,"b.ts":3,"c.ts":3,"d.ts":3}}']
        lines += [f'##playwright {{"ev":"start","f":"{c}.ts"}}' for c in "abcd"]
        lines += ['##playwright {"ev":"test","f":"a.ts","o":"fail"}']
        lines += [f'##playwright {{"ev":"test","f":"{c}.ts","o":"skip"}}' for c in "bcd"]     # interrupted
        self.feed_playwright(lines)
        self.apply("tests", total=4, fail=1, err=0, skip=0)
        self.board.apply("step_end", {"id": "s", "status": "fail", "ms": 1, "exit": 1, "summary": None,
                                      "totals": None})
        self.assertEqual(self.row.failed_units, ["a.ts"])
        self.assertEqual(self.row.status, "fail")
        self.assertEqual(jr.row_text(self.row, jr.PLAIN_GLYPHS, self.clock())[2], "4 tests, 1 failed: a.ts")

    def test_a_playwright_test_whose_retry_never_ran_names_its_file(self):
        # --max-failures stopped the run with a.ts's retry queued: the reporter's onEnd sends its outcome so far
        lines = ['##playwright {"ev":"hello","v":1}', '##playwright {"ev":"begin","f":{"a.ts":1,"b.ts":1}}',
                 '##playwright {"ev":"start","f":"a.ts"}', '##playwright {"ev":"start","f":"b.ts"}',
                 '##playwright {"ev":"test","f":"b.ts","o":"fail"}', '##playwright {"ev":"test","f":"a.ts","o":"fail"}']
        self.feed_playwright(lines)
        self.board.apply("step_end", {"id": "s", "status": "fail", "ms": 1, "exit": 1, "summary": None,
                                      "totals": None})
        self.assertEqual(([self.row.units[f].status for f in ("a.ts", "b.ts")], self.row.failed_units),
                         (["fail", "fail"], ["b.ts", "a.ts"]))

    def test_a_failing_script_tests_row_reads_totals_then_its_files(self):
        self.board.apply("units", {"step": "s", "total": 2, "kind": "files"})
        for f in ("scripts/tests/test_push.py", "scripts/tests/test_release.py"):
            self.apply("unit_start", id=f, title=f, src="file")
            self.apply("unit_end", id=f, status="fail", src="file")
        self.apply("tests", total=107, fail=2, err=0, skip=0)
        self.board.apply("step_end", {"id": "s", "status": "fail", "ms": 1, "exit": 1, "summary": None,
                                      "totals": None})
        text = jr.row_text(self.row, jr.PLAIN_GLYPHS, self.clock())[2]
        self.assertEqual(text, "107 tests, 2 failed: scripts/tests/test_push.py +1")

    def test_a_failed_maven_module_outside_tests_mode_keeps_its_count_text(self):
        self.board.apply("reactor", {"step": "s", "size": 2, "poms": [], "tests": False})
        self.apply("unit_start", id="s/a", title="Mod A", src="sub")
        self.apply("unit_end", id="s/a", status="fail", ms=5, src="sub")
        self.apply("tests", total=12, fail=1, err=0, skip=0)
        self.board.apply("step_end", {"id": "s", "status": "fail", "ms": 1, "exit": 1, "summary": None,
                                      "totals": None})
        self.assertEqual(jr.row_text(self.row, jr.PLAIN_GLYPHS, self.clock())[2], "1 failed: Mod A")

    def test_unit_end_keeps_a_given_ms(self):
        self.apply("unit_start", id="f", title="F", src="script")
        self.apply("unit_end", id="f", status="ok", ms=1234, src="script")
        self.assertEqual(self.row.units["f"].ms, 1234)

    def test_file_and_script_units_make_no_box_bar(self):
        self.apply("unit_start", id="f", title="F", src="file")
        cells, count, *_ = jr.row_text(self.row, jr.PLAIN_GLYPHS, self.clock())
        self.assertEqual((cells, count), (None, ""))

    def test_sub_and_row_units_still_box(self):
        self.apply("unit_start", id="r", title="R", src="row")
        self.apply("unit_end", id="r", status="ok", ms=5, src="row")
        self.assertEqual((self.row.order, self.row.units["r"].box), (["r"], "pass"))

    def test_step_end_grants_only_bar_units(self):
        for src in ("sub", "file", "script"):
            self.apply("unit_start", id=src, title=src, src=src)
        self.board.apply("step_end", {"id": "s", "status": "ok", "ms": 1, "exit": 0, "summary": None, "totals": None})
        self.assertEqual(self.row.order, ["sub"])
        self.assertEqual([self.row.units[i].box for i in ("file", "script")], [None, None])

    def test_file_and_script_ids_are_not_registered_board_wide(self):
        self.apply("unit_start", id="identity", title="identity", src="script")
        self.apply("unit_start", id="a.py", title="a.py", src="file")
        self.apply("unit_start", id="s/m", title="M", src="sub")
        self.assertEqual(set(self.board.unit_rows), {"s/m"})

    def test_compiled_and_tested_still_read_module(self):
        self.board.apply("reactor", {"step": "s", "size": 2, "poms": [], "tests": False})
        self.apply("unit_start", id="s/a", title="A", src="sub")
        self.apply("module_compiled", module="s/a", ok=True)
        self.assertEqual(self.row.units["s/a"].box, "pass")
        self.board.apply("reactor", {"step": "s", "size": 2, "poms": [], "tests": True})
        self.apply("unit_start", id="s/b", title="B", src="sub")
        self.apply("unit_dot", id="s/b", outcome="fail", src="sub")
        self.apply("module_tested", module="s/b")
        self.assertEqual(self.row.units["s/b"].box, "fail")

    def test_module_events_leave_file_and_script_units_alone(self):
        self.board.apply("reactor", {"step": "s", "size": 2, "poms": [], "tests": True})
        for src in ("file", "script"):
            self.apply("unit_start", id=src, title=src, src=src)
            self.apply("unit_dot", id=src, outcome="pass")
            self.apply("module_compiled", module=src, ok=False)
            self.apply("module_tested", module=src)
        self.assertEqual(([u.box for u in self.row.units.values()], self.row.order, self.row.units_done),
                         ([None, None], [], 0))
        self.assertEqual("".join(jr.unit_bar(self.row.unit_view(), self.row.units_total, jr.PLAIN_GLYPHS)),
                         jr.PENDING * 2)        # nor are they in flight on the module bar

    def test_an_orphan_dot_then_a_sub_start_makes_a_module_that_fails_into_the_bar(self):
        self.board.apply("reactor", {"step": "s", "size": 2, "poms": [], "tests": False})
        self.apply("unit_dot", id="s/a", outcome="fail")        # no src: a script unit, for now
        self.apply("unit_start", id="s/a", title="Mod A", src="sub")
        self.apply("unit_end", id="s/a", status="fail", ms=5, src="sub")
        u = self.row.units["s/a"]
        self.assertEqual((u.src, u.box, self.row.failed_units, self.row.units_done), ("sub", "fail", ["Mod A"], 1))
        self.assertIs(self.board.unit_rows["s/a"], self.row)

    def test_a_sub_start_after_an_orphan_end_puts_the_unit_back_in_progress(self):
        self.apply("unit_dot", id="s/a", outcome="fail")
        self.apply("unit_end", id="s/a", status="fail", ms=7)      # no src: a closed script unit, for now
        self.apply("unit_start", id="s/a", title="Mod A", src="sub")
        u = self.row.units["s/a"]
        self.assertEqual((u.src, u.status, u.ms, u.outcomes, u.in_progress), ("sub", None, None, [], True))

    def test_an_orphan_end_with_no_src_is_a_script_unit_and_a_bar_end_with_no_ms_is_timed(self):
        self.apply("unit_end", id="o", status="fail")
        self.assertEqual((self.row.units["o"].src, self.row.order, self.row.failed_units), ("script", [], []))
        self.apply("unit_start", id="s/a", title="A", src="sub")
        self.clock.t += 3
        self.apply("unit_end", id="s/a", status="ok", src="sub")
        self.assertEqual(self.row.units["s/a"].ms, 3000)


class SubRowUnitTest(unittest.TestCase):
    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        self.board = jr.Board(clock=Clock())
        self.board.apply("step_start", {"id": "verify", "n": 1, "title": "Verify", "parent": None})
        self.board.apply("step_start", {"id": "verify.1", "n": 1, "title": "mvn verify", "parent": "verify"})
        self.row = self.board.rows["verify"]

    def test_a_sub_row_is_a_row_unit(self):
        u = self.row.units["verify.1"]
        self.assertEqual((u.src, u.title, u.in_progress), ("row", "mvn verify", True))

    def test_a_maven_sub_row_hands_the_bar_and_the_lines_to_its_modules(self):
        self.board.apply("reactor", {"step": "verify.1", "size": 2, "poms": [], "tests": False})
        self.board.apply("unit_start", {"step": "verify.1", "id": "verify.1/a", "title": "Mod A", "src": "sub"})
        self.assertEqual([(u.id, u.src) for u in self.row.units.values()], [("verify.1/a", "sub")])

    def test_a_generic_sub_row_keeps_its_unit_until_its_end(self):
        self.board.apply("step_end", {"id": "verify.1", "status": "ok", "ms": 10, "exit": 0, "summary": None,
                                      "totals": None})
        u = self.row.units["verify.1"]
        self.assertEqual((u.status, u.box, u.in_progress), ("ok", "pass", False))


class HeartbeatTest(unittest.TestCase):
    def test_the_heartbeat_counts_only_sub_and_row_units(self):
        clock = Clock()
        board = jr.Board(clock=clock)
        board.apply("step_start", {"id": "s", "n": 1, "title": "Tests", "parent": None})
        for src in ("sub", "row", "file", "script"):
            board.apply("unit_start", {"step": "s", "id": src, "title": src, "src": src})
        out = []
        plain = jr.PlainRenderer(out.append, heartbeat=True, clock=clock)
        clock.t += jr.HEARTBEAT_SECONDS
        plain.tick(board)
        self.assertIn("· 2 modules running", "".join(out))

    def test_the_heartbeat_uses_the_singular_for_one_module(self):
        clock = Clock()
        board = jr.Board(clock=clock)
        board.apply("step_start", {"id": "s", "n": 1, "title": "Tests", "parent": None})
        board.apply("unit_start", {"step": "s", "id": "sub", "title": "sub", "src": "sub"})
        out = []
        plain = jr.PlainRenderer(out.append, heartbeat=True, clock=clock)
        clock.t += jr.HEARTBEAT_SECONDS
        plain.tick(board)
        self.assertIn("· 1 module running", "".join(out))


if __name__ == "__main__":
    unittest.main()
