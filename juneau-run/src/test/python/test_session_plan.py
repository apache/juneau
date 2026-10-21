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
"""The whole-run plan: Overall arithmetic (pure), plan() and the Board's plan state (spec 6)."""

import unittest

from support import jr, reset_run_state


class OverallTest(unittest.TestCase):
    def fig(self, **kw):
        return jr.overall_figures(**kw)

    def test_pending_steps_sum_history(self):
        f = self.fig(elapsed=60, steps=[("a", "done", None, None), ("b", "pending", 100, None), ("c", "pending", 40, None)])
        self.assertEqual(f.remaining, 140); self.assertEqual(f.percent, 30); self.assertFalse(f.lower_bound)
        self.assertEqual((f.done, f.total), (1, 3))

    def test_running_step_contributes_total_minus_elapsed(self):
        f = self.fig(elapsed=30, steps=[("a", "running", 100, (80.0, 30.0)), ("b", "pending", 20, None)])
        self.assertEqual(f.remaining, 70)

    def test_pending_without_history_is_lower_bound(self):
        f = self.fig(elapsed=10, steps=[("a", "running", 100, (50.0, 10.0)), ("b", "pending", None, None)])
        self.assertTrue(f.lower_bound)

    def test_nothing_known_shows_nothing(self):
        self.assertIsNone(self.fig(elapsed=10, steps=[("a", "pending", None, None)]))

    def test_cap_99_and_text(self):
        f = self.fig(elapsed=1000, steps=[("a", "running", 1000, (1001.0, 1000.0))])
        self.assertEqual(f.percent, 99)
        self.assertEqual(jr.overall_text(jr.Overall(percent=42, remaining=130, lower_bound=False, done=3, total=7)),
                         "Overall  42%  ~2:10 left  · 3 of 7 steps")

    def test_a_lower_bound_shows_its_percent_as_approximate(self):
        # a step with no figure still to run: the time left is a floor, so the percent is only a ceiling
        self.assertEqual(jr.overall_text(jr.Overall(percent=96, remaining=130, lower_bound=True, done=3, total=7)),
                         "Overall  ~96%  ~2:10+ left  · 3 of 7 steps")

    def test_a_running_step_without_history_contributes_nothing_and_is_unknown(self):
        f = self.fig(elapsed=10, steps=[("a", "running", None, None), ("b", "pending", 30, None)])
        self.assertEqual(f.remaining, 30)
        self.assertTrue(f.lower_bound)


class Clock:
    def __init__(self):
        self.t = 100.0

    def __call__(self):
        return self.t

    def advance(self, s):
        self.t += s


def start(step):
    return ("step_start", dict(id=step, n=1, title=step, parent=None, tool=None, sig=""))


def end(step, status="ok"):
    return ("step_end", dict(id=step, status=status, ms=1000, exit=None, summary=None, totals=None))


class BoardPlanTest(unittest.TestCase):
    HIST = {"a": 60.0, "b": 100.0, "c": 40.0}

    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        self.clock = Clock()

    def board(self, plan=("a", "b", "c"), nested=False, hist=None):
        hist = self.HIST if hist is None else hist
        b = jr.Board("actionable", nested=nested, clock=self.clock,
                     estimator=lambda step, tool, sig=None: jr.Estimate(hist[step], None, None, 3) if step in hist else None)
        b.apply("session", dict(title="T", header=[], nested=nested))
        if plan is not None:
            b.apply("plan", dict(ids=list(plan)))
        return b

    def test_no_plan_is_none(self):
        self.assertIsNone(self.board(plan=None).overall())

    def test_pending_plan(self):
        o = self.board().overall()
        self.assertEqual((o.remaining, o.done, o.total, o.percent), (195, 0, 3, 0))

    def test_finished_and_running(self):
        b = self.board()
        b.apply(*start("a")); self.clock.advance(20)
        b.apply(*end("a")); b.apply(*start("b")); self.clock.advance(30)
        o = b.overall()
        self.assertEqual(o.done, 1)
        self.assertEqual(o.remaining, 120)       # b: 100 - 30, c: 40 = 110, shown on the 15s step

    def test_skipped_planned_step_stops_counting(self):
        b = self.board()
        b.apply(*start("a")); b.apply(*end("a", "skip"))
        o = b.overall()
        self.assertEqual((o.total, o.done, o.remaining), (2, 0, 135))       # 140 on the 15s step

    def test_pending_step_passed_over_stops_counting(self):
        b = self.board()
        b.apply(*start("c"))
        o = b.overall()
        self.assertEqual(o.total, 1)                 # a and b were passed over; c is running
        self.assertEqual(o.remaining, 40)

    def test_unplanned_step_adds_elapsed_but_no_remaining(self):
        b = self.board()
        b.apply(*start("x")); self.clock.advance(50); b.apply(*end("x"))
        o = b.overall()
        self.assertEqual(o.remaining, 195)       # 200 on the 15s step
        self.assertEqual(o.percent, 20)               # 50 / (50 + 200)

    def test_missing_history_is_a_lower_bound(self):
        o = self.board(hist={"a": 60.0}).overall()
        self.assertTrue(o.lower_bound)
        self.assertEqual(o.remaining, 60)

    def test_nothing_known_is_none(self):
        self.assertIsNone(self.board(hist={}).overall())

    def test_replacing_the_plan(self):
        b = self.board()
        b.apply("plan", dict(ids=["c"]))
        self.assertEqual(b.overall().total, 1)

    def test_nested_board_has_no_overall(self):
        self.assertIsNone(self.board(nested=True).overall())

    def test_running_step_on_the_live_units_path(self):
        b = self.board()
        b.apply(*start("a"))
        r = b.rows["a"]
        r.units_total, r.units_done = 10, 5
        self.clock.advance(30)                      # 5 of 10 units in 30s: ~60s total, 30s left
        o = b.overall()
        self.assertEqual(o.remaining, 165)       # 170 on the 15s step

    def test_overrun_with_nothing_else_known_has_no_left_time(self):
        b = self.board(plan=("a",), hist={"a": 10.0})
        b.apply(*start("a")); self.clock.advance(50)
        o = b.overall()
        self.assertEqual(o.remaining, 0)
        text = jr.overall_text(o)
        self.assertNotIn("left", text)
        self.assertIn("steps", text)

    def test_overrun_with_pending_work_keeps_left_time(self):
        b = self.board(plan=("a", "b"), hist={"a": 10.0, "b": 20.0})
        b.apply(*start("a")); self.clock.advance(50)
        self.assertIn("~0:20 left", jr.overall_text(b.overall()))

    def test_estimator_failure_is_swallowed(self):
        b = self.board()
        b.estimator = lambda *a: 1 / 0
        self.assertIsNone(b.overall())


class PlanApiTest(unittest.TestCase):
    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)

    def test_plan_publishes(self):
        seen = []
        jr.subscribe(lambda kind, f: seen.append((kind, f)))
        jr.plan(("a", "b"))
        self.assertEqual(seen, [("plan", {"ids": ["a", "b"], "labels": {}})])

    def test_plan_publishes_labels_for_labelled_entries(self):
        seen = []
        jr.subscribe(lambda kind, f: seen.append((kind, f)))
        jr.plan(["a", ("b", "Build")])
        self.assertEqual(seen, [("plan", {"ids": ["a", "b"], "labels": {"b": "Build"}})])

    def test_session_plan_duplicate_raises_in_plain_mode(self):
        import sys
        from unittest import mock
        from juneau_scripts import run
        with mock.patch.dict(sys.modules):
            sys.modules.pop("juneau_run", None)
            with self.assertRaises(ValueError):
                run.Session().plan(["a", "a"])

    def test_duplicate_id_raises(self):
        with self.assertRaises(ValueError):
            jr.plan(["a", "b", "a"])


class RegistryPlanTest(unittest.TestCase):
    def test_unselected_skip_rows_are_not_in_the_plan(self):
        import argparse
        from juneau_scripts import steps
        reg = steps.Registry()
        for name in ("a", "b", "c", "d"):
            reg.step(name, name.upper())(lambda ctx: None)
        parser = argparse.ArgumentParser(prog="r.py")
        reg.add_arguments(parser)
        self.assertEqual(reg.plan(parser.parse_args(["--start-step", "b", "--skip-step", "c"])), ["b", "d"])


if __name__ == "__main__":
    unittest.main()
