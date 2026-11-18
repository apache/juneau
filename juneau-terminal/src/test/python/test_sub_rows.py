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
"""Row adoption: a step's only command is the step's own row, the count API, and the time-based bar."""
"""Sub-rows: a step declares one row per child (jr.subrows), each with its own bar, count, result and time."""

import unittest

import scripts_support  # noqa: F401  (puts juneau_terminal_scripts on the path)
from support import jr, reset_run_state


class Clock:
    def __init__(self):
        self.now = 100.0

    def __call__(self):
        return self.now


def board(clock):
    reset_run_state()
    b = jr.Board("actionable", nested=False, full_log=None, clock=clock)
    b.apply("step_start", dict(id="verify", n=1, title="Verify", parent=None, label="05 Verify"))
    return b


def declare(b, items):
    b.apply("subrows", dict(step="verify", items=items))


def start(b, sid, title=None):
    b.apply("step_start", dict(id=sid, n=1, title=title or sid, parent="verify", tool=None, sig=""))


def end(b, sid, status="ok", ms=5000, totals=None):
    return b.apply("step_end", dict(id=sid, status=status, ms=ms, exit=0, summary=None, totals=totals))


def lines(b, clock):
    return jr.kid_lines(b.rows["verify"], jr.NO_COLOR_GLYPHS, clock(), 100)


class SubRowsTest(unittest.TestCase):
    def setUp(self):
        self.addCleanup(reset_run_state)
        self.clock = Clock()
        self.b = board(self.clock)

    def test_declared_rows_are_pending_and_the_step_bar_counts_them(self):
        declare(self.b, [["j17", "JDK 17", "pending", None], ["j21", "JDK 21", "pending", None]])
        r = self.b.rows["verify"]
        self.assertEqual((r.unit_kind, r.units_done, r.units_total), ("rows", 0, 2))
        text, _ = jr.row_parts(r, jr.NO_COLOR_GLYPHS, 100.0)
        self.assertIn("0/2", text)
        self.assertEqual(lines(self.b, self.clock), ["    ○ JDK 17", "    ○ JDK 21"])

    def test_a_started_child_runs_on_its_own_row_not_the_steps(self):
        declare(self.b, [["j17", "JDK 17", "pending", None], ["j21", "JDK 21", "pending", None]])
        start(self.b, "j17", "Verify on JDK 17")
        self.assertEqual(self.b.rows["verify"].hidden, set())      # not demoted into the step's own row
        self.b.apply("reactor", dict(step="j17", size=4, poms=[], tail=[], tests=False))
        self.b.apply("units", dict(step="j17", total=4, kind="modules"))
        self.b.apply("unit", dict(step="j17", done=1, outcome="pass"))
        self.b.apply("tests", dict(step="j17", total=12, fail=0, err=0, skip=0))
        self.clock.now = 103.0
        first, second = lines(self.b, self.clock)
        self.assertIn("JDK 17", first)
        self.assertIn("1/4", first)
        self.assertIn("12", first)
        self.assertTrue(first.rstrip().endswith("0:03"), first)
        self.assertEqual(second, "    ○ JDK 21")
        self.assertEqual(self.b.rows["verify"].units_done, 0)      # the step counts finished JDKs only

    def test_end_records_the_result_and_advances_the_step_bar(self):
        declare(self.b, [["j17", "JDK 17", "pending", None], ["j21", "JDK 21", "pending", None]])
        start(self.b, "j17")
        end(self.b, "j17", "ok", 65000)
        start(self.b, "j21")
        end(self.b, "j21", "fail", 7000)
        r = self.b.rows["verify"]
        self.assertEqual(r.units_done, 2)
        self.assertEqual(r.failed_units, ["JDK 21"])
        done = lines(self.b, self.clock)
        self.assertIn("✔", done[0])
        self.assertIn("1:05", done[0])
        self.assertIn("✗", done[1])

    def test_a_row_declared_passed_is_done_at_once(self):
        declare(self.b, [["j17", "JDK 17", "pass", "passed earlier"], ["j21", "JDK 21", "pending", None]])
        r = self.b.rows["verify"]
        self.assertEqual(r.units_done, 1)
        first = lines(self.b, self.clock)[0]
        self.assertIn("✔", first)
        self.assertIn("passed earlier", first)

    def test_unfinished_rows_are_closed_when_the_step_ends(self):
        declare(self.b, [["j17", "JDK 17", "pending", None], ["j21", "JDK 21", "pending", None]])
        start(self.b, "j17")
        end(self.b, "j17", "fail")
        blocks = self.b.apply("step_end", dict(id="verify", status="fail", ms=9000, exit=1, summary=None, totals=None))
        self.assertEqual([x[0] for x in blocks], ["row", "kids"])
        self.assertTrue(self.b.rows["verify"].kids["j21"].status == "skip")
        out = []
        jr.PlainRenderer(out.append, clock=self.clock).render(self.b, blocks)
        self.assertIn("JDK 17", "".join(out))
        self.assertTrue(all(len(ln) < 80 for ln in "".join(out).splitlines()))     # plain: no flush-right padding
        self.assertIn("JDK 21", "".join(out))

    def test_a_child_with_another_id_still_acts_as_a_command(self):
        declare(self.b, [["j17", "JDK 17", "pending", None]])
        start(self.b, "other")
        self.assertIn("other", self.b.rows["verify"].hidden)

    def test_the_live_region_draws_the_rows_under_the_step(self):
        declare(self.b, [["j17", "JDK 17", "pending", None], ["j21", "JDK 21", "pending", None]])
        start(self.b, "j17")
        out = []
        live = jr.LiveRenderer(out.append, size=lambda: type("S", (), {"columns": 100, "lines": 30})(),
                               clock=self.clock, color=False)
        region = live.region(self.b, 99)
        self.assertEqual(sum("JDK" in ln for ln in region), 2)
        self.assertIn("0/2", region[0])


class UnchangedTest(unittest.TestCase):
    def setUp(self):
        self.addCleanup(reset_run_state)

    def test_a_step_without_subrows_has_no_kids_and_no_kids_block(self):
        b = board(Clock())
        r = b.rows["verify"]
        start(b, "mvn")
        self.assertEqual((r.kids, r.unit_kind), ({}, None))
        blocks = end(b, "mvn") + b.apply("step_end", dict(id="verify", status="ok", ms=5, exit=0, summary=None,
                                                          totals=None))
        self.assertNotIn("kids", [x[0] for x in blocks])

    def test_the_feature_is_advertised_and_the_api_publishes(self):
        from juneau_terminal_scripts import run
        self.assertIn("sub-rows", run.BOARD_FEATURES)
        seen = []
        jr.subscribe(lambda kind, f: seen.append((kind, f)))
        jr.subrows("verify", [("a", "A"), ("b", "B", "pass", "ok")])
        self.assertEqual(seen, [("subrows", {"step": "verify", "items": [
            ["a", "A", "pending", None], ["b", "B", "pass", "ok"]]})])


if __name__ == "__main__":
    unittest.main()
