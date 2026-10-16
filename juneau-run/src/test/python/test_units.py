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
"""Units: bucketed unit bar."""

import unittest

from support import jr, reset_run_state
from test_parsers import feed


class BucketTest(unittest.TestCase):
    GL = {"pass": "P", "fail": "F", "skip": "S"}

    def cells(self, outcomes, total, width=jr.BAR_CELLS):
        return "".join(jr.unit_bar(bytearray(outcomes), total, width, self.GL))

    def test_one_cell_per_unit_at_or_under_width(self):
        self.assertEqual(self.cells([1, 2, 3, 0], 5, 60), "PFS" + jr.PENDING + jr.PENDING)

    def test_bucket_worst_wins_fail_over_pass_over_skip(self):
        out = [1] * 4 + [1, 2, 1, 1] + [3, 3, 1, 3] + [3, 3, 3, 3]   # 16 units, width 4 -> 4 per bucket
        self.assertEqual(self.cells(out, 16, 4), "PFPS")

    def test_unfinished_bucket_is_pending(self):
        self.assertEqual(self.cells([1] * 4, 16, 4), "P" + jr.PENDING * 3)

    def test_rebucket_to_narrow_width(self):
        self.assertEqual(len(self.cells([1] * 300, 300, 20)), 20)

    def test_cell_count_is_exactly_width_when_over(self):
        for total in (61, 121, 300):
            self.assertEqual(len(self.cells([1] * total, total, 60)), 60, total)

    def test_even_split_keeps_a_fail_in_its_bucket(self):
        out = [1] * 61
        out[60] = 2
        bar = self.cells(out, 61, 60)
        self.assertEqual((len(bar), bar[-1], bar.count("F")), (60, "F", 1))

    def test_width_one(self):
        self.assertEqual(self.cells([1, 2, 1], 3, 1), "F")

    def test_late_fix_changes_bucket(self):
        out = bytearray([1] * 120)
        self.assertEqual(self.cells(out, 120, 60).count("F"), 0)
        out[7] = 2
        self.assertEqual(self.cells(out, 120, 60)[3], "F")

    def test_zero_total(self):
        self.assertEqual(self.cells([], 0, 60), "")


class BoardUnitsTest(unittest.TestCase):
    GL = {"pass": "P", "fail": "F", "skip": "S"}

    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        self.b = jr.Board("actionable", nested=False, full_log=None, clock=lambda: 100.0)
        self.b.apply("step_start", dict(id="s", n=1, title="pytest", parent=None))

    def test_units_then_unit(self):
        self.b.apply("units", dict(step="s", total=4, kind="tests"))
        self.b.apply("unit", dict(step="s", done=1, outcome="pass"))
        self.b.apply("unit", dict(step="s", done=3, outcome="fail"))   # a jump: the gap takes the jump's outcome
        r = self.b.rows["s"]
        self.assertEqual((r.units_done, r.units_total, r.unit_kind), (3, 4, "tests"))
        self.assertEqual(list(r.unit_cells), [1, 2, 2, 0])

    def test_playwright_jump_with_no_outcome_fills_as_pass(self):
        self.b.apply("units", dict(step="s", total=4, kind="tests"))
        self.b.apply("unit", dict(step="s", done=1, outcome=None))
        self.b.apply("unit", dict(step="s", done=3, outcome=None))
        self.assertEqual(list(self.b.rows["s"].unit_cells), [1, 1, 1, 0])

    def test_resend_grows_the_cells(self):
        self.b.apply("units", dict(step="s", total=2, kind="tests"))
        self.b.apply("unit", dict(step="s", done=2, outcome="pass"))
        self.b.apply("units", dict(step="s", total=5, kind="tests"))
        self.assertEqual(list(self.b.rows["s"].unit_cells), [1, 1, 0, 0, 0])

    def test_unit_fix(self):
        self.b.apply("units", dict(step="s", total=3, kind="tests"))
        self.b.apply("unit", dict(step="s", done=3, outcome="pass"))
        self.b.apply("unit_fix", dict(step="s", index=1, outcome="fail"))
        self.assertEqual(list(self.b.rows["s"].unit_cells), [1, 2, 1])

    def test_unit_beyond_total_extends(self):          # -x / a larger-than-announced run must not raise
        self.b.apply("units", dict(step="s", total=2, kind="tests"))
        self.b.apply("unit", dict(step="s", done=4, outcome="pass"))
        self.assertEqual(self.b.rows["s"].units_total, 4)

    def test_unit_fix_with_error_or_xfail_does_not_raise(self):
        self.b.apply("units", dict(step="s", total=3, kind="tests"))
        self.b.apply("unit", dict(step="s", done=3, outcome="pass"))
        self.b.apply("unit_fix", dict(step="s", index=0, outcome="error"))
        self.b.apply("unit_fix", dict(step="s", index=1, outcome="xfail"))
        self.assertEqual(list(self.b.rows["s"].unit_cells), [1, 1, 1])

    def test_smaller_resend_replaces_the_total(self):
        self.b.apply("units", dict(step="s", total=5, kind="tests"))
        self.b.apply("units", dict(step="s", total=3, kind="tests"))
        r = self.b.rows["s"]
        self.assertEqual((r.units_total, len(r.unit_cells)), (3, 3))

    def test_smaller_resend_never_drops_below_units_done(self):
        self.b.apply("units", dict(step="s", total=5, kind="tests"))
        self.b.apply("unit", dict(step="s", done=4, outcome="pass"))
        self.b.apply("units", dict(step="s", total=3, kind="tests"))
        r = self.b.rows["s"]
        self.assertEqual((r.units_total, len(r.unit_cells), list(r.unit_cells)), (4, 4, [1, 1, 1, 1]))

    def test_xdist_fallback_with_deselection_ends_full_and_complete(self):
        parser = jr.PytestParser()
        parser.step_id = "s"
        lines = ["collected 9 items", "2 workers [7 items]"] + [f"[gw0] [ 50%] PASSED tests/t.py::t{i}" for i in range(7)]
        events = feed(parser, "\n".join(lines))
        for e in events:
            if e[0] in ("units", "unit"):
                f = dict(zip(("step", "total" if e[0] == "units" else "done", "kind" if e[0] == "units" else "outcome"), e[1:]))
                self.b.apply(e[0], f)
        r = self.b.rows["s"]
        self.assertEqual((r.units_done, r.units_total), (7, 7))
        self.assertNotIn(0, list(r.unit_cells))

    def test_unit_fix_out_of_range_or_past_done_is_ignored(self):
        self.b.apply("units", dict(step="s", total=3, kind="tests"))
        self.b.apply("unit", dict(step="s", done=1, outcome="pass"))
        self.b.apply("unit_fix", dict(step="s", index=9, outcome="fail"))
        self.b.apply("unit_fix", dict(step="s", index=-1, outcome="fail"))
        self.b.apply("unit_fix", dict(step="s", index=2, outcome="fail"))     # not finished yet
        self.assertEqual(list(self.b.rows["s"].unit_cells), [1, 0, 0])

    def test_unknown_outcome_counts_as_pass(self):
        self.b.apply("units", dict(step="s", total=2, kind="tests"))
        self.b.apply("unit", dict(step="s", done=2, outcome="weird"))
        self.assertEqual(list(self.b.rows["s"].unit_cells), [1, 1])

    def test_total_zero(self):
        self.b.apply("units", dict(step="s", total=0, kind="tests"))
        self.assertEqual(self.b.rows["s"].units_total, 0)

    def test_unit_before_units(self):
        self.b.apply("unit", dict(step="s", done=2, outcome="fail"))
        r = self.b.rows["s"]
        self.assertEqual((r.units_total, r.units_done, list(r.unit_cells)), (2, 2, [2, 2]))

    def test_unit_events_reach_the_parent_row_through_a_sub_row_alias(self):
        self.b.apply("step_start", dict(id="c", n=2, title="cmd", parent="s"))
        self.b.apply("units", dict(step="c", total=2, kind="tests"))
        self.b.apply("unit", dict(step="c", done=2, outcome="pass"))
        self.assertEqual(list(self.b.rows["s"].unit_cells), [1, 1])

    def test_row_parts_with_empty_bar_has_no_leading_spaces(self):
        r = self.b.rows["s"]
        r.unit_kind = "modules"
        text, _ = jr.row_parts(r, self.GL, 100.0)
        self.assertEqual(text, text.lstrip())


if __name__ == "__main__":
    unittest.main()
