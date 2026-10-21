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
"""Estimator and ETA arithmetic (pure, injected values)."""

import unittest

from support import jr

Est = jr.Estimate


class ArithmeticTest(unittest.TestCase):
    def test_live_n(self):
        pct, total, approx = jr.eta_figures(e=26.0, k=47, n=101, est=None)
        self.assertEqual(pct, 46)
        self.assertAlmostEqual(total, 26.0 + (101 - 47) * (26.0 / 47))
        self.assertFalse(approx)

    def test_blend_weights_live_by_progress(self):
        est = Est(wall_s=100, units=100, rate=1.0, runs=5)
        for k, n, want_r in ((0, 100, None), (50, 100, 0.5 * (20 / 50) + 0.5 * 1.0), (100, 100, None)):
            with self.subTest(k=k):
                pct, total, _ = jr.eta_figures(e=20.0, k=k, n=n, est=est)
                if k == 0:
                    self.assertEqual(total, 100)                      # wall_s at k == 0
                elif k == n:
                    self.assertIsNone(total)                           # finished-but-running: elapsed only
                else:
                    self.assertAlmostEqual(total, 20.0 + (n - k) * want_r)

    def test_live_k_historical_n_is_marked(self):
        est = Est(wall_s=60, units=30, rate=2.0, runs=3)
        pct, total, approx = jr.eta_figures(e=10.0, k=10, n=0, est=est)
        self.assertEqual(pct, 33)
        self.assertTrue(approx)

    def test_neither_uses_elapsed_over_wall(self):
        pct, total, approx = jr.eta_figures(e=30.0, k=0, n=0, est=Est(60, None, None, 4))
        self.assertEqual((pct, total, approx), (50, 60, True))

    def test_cap_at_99_and_overrun_shows_elapsed_only(self):
        pct, total, approx = jr.eta_figures(e=90.0, k=0, n=0, est=Est(60, None, None, 4))
        self.assertEqual(pct, 99)
        self.assertIsNone(total)
        self.assertTrue(approx)

    def test_no_history_no_estimate(self):
        self.assertEqual(jr.eta_figures(e=5.0, k=0, n=0, est=None), (None, None, False))

    def test_timer_text(self):
        self.assertEqual(jr.timer_text(26.0, 100.0), "0:26 / ~1:40")
        self.assertEqual(jr.timer_text(26.0, None), "0:26")


class EstimateFromRecordsTest(unittest.TestCase):
    def rec(self, wall, sig="a", ok=True, complete=True, total=10, kind="modules"):
        return {"v": 1, "step": "s", "tool": "t", "sig": sig, "ok": ok, "complete": complete,
                "wall_s": wall, "units": {"kind": kind, "total": total, "done": total}}

    def test_median_odd_even(self):
        self.assertEqual(jr.estimate_from([self.rec(w) for w in (10, 30, 20)], "a").wall_s, 20)
        self.assertEqual(jr.estimate_from([self.rec(w) for w in (10, 20, 30, 40)], "a").wall_s, 25)

    def test_incomplete_and_failed_ignored(self):
        recs = [self.rec(10), self.rec(999, ok=False), self.rec(999, complete=False)]
        self.assertEqual(jr.estimate_from(recs, "a").wall_s, 10)

    def test_sig_preference_needs_three(self):
        mix = [self.rec(10, "x"), self.rec(10, "x"), self.rec(100, "y"), self.rec(100, "y")]
        self.assertEqual(jr.estimate_from(mix, "x").wall_s, 55)                  # only 2 match: use all four
        mix.append(self.rec(10, "x"))
        self.assertEqual(jr.estimate_from(mix, "x").wall_s, 10)                  # 3 match: like-for-like

    def test_newest_ten_only(self):
        recs = [self.rec(1000)] * 5 + [self.rec(10)] * 10                         # oldest first
        self.assertEqual(jr.estimate_from(recs, "a").wall_s, 10)

    def test_no_records(self):
        self.assertIsNone(jr.estimate_from([], "a"))

    def mixed(self):
        """Two runs counted per file and one per test, as a Playwright step's history reads after the default changed."""
        return jr.estimate_from([self.rec(80, total=8, kind="files"), self.rec(120, total=8, kind="files"),
                                 self.rec(100, total=32, kind="tests")], "a")

    def test_kinds_hold_each_kinds_medians(self):
        self.assertEqual(self.mixed().kinds, {"files": (8, 12.5), "tests": (32, 3.125)})

    def test_the_other_figures_still_use_every_record(self):
        est = self.mixed()
        self.assertEqual((est.wall_s, est.units, est.rate, est.runs), (100, 8, 10.0, 3))

    def test_a_record_with_no_unit_kind_joins_no_kind(self):
        est = jr.estimate_from([self.rec(80, total=8, kind="files"), self.rec(100, total=4, kind=None)], "a")
        self.assertEqual(est.kinds, {"files": (8, 10.0)})

    def test_records_without_units_give_no_kinds(self):
        self.assertIsNone(jr.estimate_from([{"ok": True, "complete": True, "wall_s": 5.0}], None).kinds)


class EstimateForKindTest(unittest.TestCase):
    def setUp(self):
        self.est = Est(wall_s=100, units=8, rate=10.0, runs=3, tests=40, kinds={"files": (8, 12.5), "tests": (32, 3.125)})

    def test_the_kinds_pair_replaces_units_and_rate(self):
        self.assertEqual(jr.estimate_for_kind(self.est, "tests"), self.est._replace(units=32, rate=3.125))

    def test_a_kind_with_no_history_has_no_units_or_rate_and_the_figures_fall_back_to_wall(self):
        est = jr.estimate_for_kind(self.est, "modules")
        self.assertEqual((est.units, est.rate, est.wall_s, est.tests, est.runs), (None, None, 100, 40, 3))
        self.assertEqual(jr.eta_figures(30.0, 3, 0, est), (30, 100, True))

    def test_unchanged_without_an_estimate_kinds_or_a_kind(self):
        stub = Est(wall_s=100, units=8, rate=10.0, runs=3)
        self.assertIsNone(jr.estimate_for_kind(None, "files"))
        self.assertIs(jr.estimate_for_kind(stub, "files"), stub)
        self.assertIs(jr.estimate_for_kind(self.est, None), self.est)     # a unit before any units: no kind yet


class MakeEstimatorTest(unittest.TestCase):
    def test_prefers_records_matching_the_current_commands_sig(self):
        import os, tempfile, subprocess
        from unittest import mock
        with tempfile.TemporaryDirectory() as hist, tempfile.TemporaryDirectory() as repo:
            subprocess.run(["git", "init", "-q", repo], check=True)
            with mock.patch.dict(os.environ, {"JUNEAU_RUN_HISTORY": "1", "JUNEAU_RUN_HISTORY_DIR": hist}):
                store = jr.HistoryStore(repo, "push")
                def add(wall, sig):
                    store.append({"v": 1, "step": "mvn", "tool": "maven", "ok": True, "complete": True,
                                  "wall_s": wall, "sig": sig})
                for _ in range(3):
                    add(100.0, "cur")
                add(10.0, "other")        # newest, but a different command
                est = jr.make_estimator(repo, "push")
                self.assertEqual(est("mvn", "maven", "cur").wall_s, 100.0)
                self.assertEqual(est("mvn", "maven", "other").runs, 4)     # fewer than 3 like: falls back to all


class ExpectedTestsTest(unittest.TestCase):
    def rec(self, tests, **kw):
        return dict({"ok": True, "complete": True, "wall_s": 10.0, "tests": tests}, **kw)

    def test_median_test_count_of_the_records_used(self):
        self.assertEqual(jr.estimate_from([self.rec(100), self.rec(300), self.rec(200)], None).tests, 200)

    def test_none_without_test_counts(self):
        self.assertIsNone(jr.estimate_from([{"ok": True, "complete": True, "wall_s": 5.0}], None).tests)

    def test_prefers_like_for_like_records(self):
        records = [self.rec(10, sig="a")] + [self.rec(500, sig="b") for _ in range(3)]
        self.assertEqual(jr.estimate_from(records, "b").tests, 500)


if __name__ == "__main__":
    unittest.main()
