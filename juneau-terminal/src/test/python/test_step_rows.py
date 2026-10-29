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

import sys
import unittest
from unittest import mock

import scripts_support  # noqa: F401  (puts juneau_terminal_scripts on the path)
from support import jr, reset_run_state


def board(estimator=None, clock=lambda: 100.0):
    reset_run_state()
    return jr.Board("actionable", nested=False, full_log=None, clock=clock, estimator=estimator)


class SingleChildTest(unittest.TestCase):
    def setUp(self):
        self.addCleanup(reset_run_state)

    def _step(self):
        b = board()
        b.apply("step_start", dict(id="step", n=1, title="Step", parent=None, label="05 Build"))
        return b

    def _child(self, b, cid, end=True):
        b.apply("step_start", dict(id=cid, n=1, title=cid, parent="step", tool=None, sig=""))
        if end:
            b.apply("step_end", dict(id=cid, status="ok", ms=5, exit=0, summary=None, totals=None))

    def test_one_child_has_no_box_and_no_sub_line(self):
        b = self._step()
        self._child(b, "mvn", end=False)
        r = b.rows["step"]
        self.assertIn("mvn", r.unbarred)
        self.assertEqual(r.boxes(), [])
        self.assertFalse(r.units_running)
        b.apply("step_end", dict(id="mvn", status="ok", ms=5, exit=0, summary=None, totals=None))
        self.assertEqual(r.boxes(), [])
        self.assertFalse(r.bar_units)

    def test_a_failed_only_child_still_names_itself_on_the_row(self):
        b = self._step()
        self._child(b, "mvn", end=False)
        b.apply("step_end", dict(id="mvn", status="fail", ms=5, exit=1, summary=None, totals=None))
        self.assertEqual(b.rows["step"].failed_units, ["mvn"])

    def test_a_second_child_shows_both(self):
        b = self._step()
        self._child(b, "one")
        self._child(b, "two", end=False)
        r = b.rows["step"]
        self.assertNotIn("one", r.unbarred)
        self.assertNotIn("two", r.unbarred)
        self.assertEqual(len(r.order), 1)           # the finished first child got its box back

    def test_progress_text_of_the_only_child_reaches_the_step_row(self):
        b = self._step()
        self._child(b, "mvn", end=False)
        b.apply("step_progress", dict(step="mvn", text="Downloading"))
        self.assertEqual(b.rows["step"].progress, "Downloading")


class CountApiTest(unittest.TestCase):
    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)

    def test_count_publishes_units_and_unit_for_the_open_step(self):
        from juneau_terminal_scripts import run
        seen = []
        jr.subscribe(lambda kind, f: seen.append((kind, f)))
        with mock.patch.dict(sys.modules, {"juneau_terminal": jr}), mock_open_step("stage"):
            run.count(2, 6)
        self.assertIn(("units", {"step": "stage", "total": 6, "kind": "files"}), seen)
        self.assertIn(("unit", {"step": "stage", "done": 2, "outcome": "pass"}), seen)

    def test_count_draws_a_files_bar(self):
        b = board()
        b.apply("step_start", dict(id="stage", n=1, title="Stage", parent=None))
        b.apply("units", dict(step="stage", total=6, kind="files"))
        b.apply("unit", dict(step="stage", done=2, outcome="pass"))
        text, _ = jr.row_parts(b.rows["stage"], jr.NO_COLOR_GLYPHS, 100.0)
        self.assertIn("2/6", text)

    def test_count_outside_a_step_or_a_plain_run_does_nothing(self):
        from juneau_terminal_scripts import run
        run.count(1, 2)        # no open step, no console: no error

    def test_the_feature_is_advertised(self):
        from juneau_terminal_scripts import run
        self.assertIn("count", run.BOARD_FEATURES)


def mock_open_step(sid):
    from juneau_terminal_scripts import run
    import contextlib

    @contextlib.contextmanager
    def cm():
        run._open_steps.append(sid)
        try:
            yield
        finally:
            run._open_steps.pop()
    return cm()


class TimeBarTest(unittest.TestCase):
    def setUp(self):
        self.addCleanup(reset_run_state)

    def _running(self, wall_s, t0=100.0):
        b = board(estimator=lambda step, tool, sig=None: jr.Estimate(wall_s, None, None, 3) if wall_s else None,
                  clock=lambda: t0)
        b.apply("step_start", dict(id="nx", n=1, title="Nexus", parent=None, tool=None, sig=""))
        return b

    def test_a_running_row_with_history_and_no_count_gets_a_dim_bar(self):
        b = self._running(60.0)
        cells, count, text, elapsed, cut = jr.row_text(b.rows["nx"], jr.PLAIN_GLYPHS, 130.0)
        self.assertIsNotNone(cells)
        self.assertEqual(len(cells), jr.TIME_BAR_CELLS)
        self.assertEqual(cells.count("="), 11)      # 30 s of ~60 s = half of 20 cells, plus the head cell
        self.assertIn("/ ~", elapsed)

    def test_no_history_means_no_bar(self):
        b = self._running(None)
        self.assertIsNone(jr.row_text(b.rows["nx"], jr.PLAIN_GLYPHS, 130.0)[0])

    def test_a_row_with_a_count_keeps_its_count_bar(self):
        b = self._running(60.0)
        b.apply("units", dict(step="nx", total=6, kind="files"))
        self.assertEqual(len(jr.row_text(b.rows["nx"], jr.PLAIN_GLYPHS, 130.0)[0]), 6)

    def test_a_single_command_step_still_gets_the_time_bar(self):
        b = self._running(60.0)
        b.apply("step_start", dict(id="mvn", n=1, title="mvn", parent="nx", tool=None, sig=""))
        self.assertEqual(len(jr.row_text(b.rows["nx"], jr.PLAIN_GLYPHS, 130.0)[0]), jr.TIME_BAR_CELLS)

    def test_the_bar_never_fills_completely_while_running(self):
        self.assertEqual(jr.time_bar(500, 60, jr.PLAIN_GLYPHS).count("-"), 1)

    def _finished(self, status, ms, glyphs=None):
        b = self._running(60.0)
        jr.row_text(b.rows["nx"], jr.PLAIN_GLYPHS, 130.0)       # drawn while running: it showed a time bar
        b.apply("step_end", dict(id="nx", status=status, ms=ms, exit=0, summary=None, totals=None))
        return jr.row_text(b.rows["nx"], glyphs or jr.PLAIN_GLYPHS, 200.0)[0]

    def test_a_finished_ok_row_keeps_a_full_bar(self):
        self.assertEqual("".join(self._finished("ok", 30000)), "=" * 20)
        self.assertEqual(self._finished("ok", 30000, jr.COLOR_GLYPHS), ["\x1b[32m▬\x1b[0m"] * 20)

    def test_a_finished_failed_row_shows_where_it_stopped(self):
        self.assertEqual("".join(self._finished("fail", 30000)), "=" * 10 + "-" * 10)
        cells = self._finished("fail", 30000, jr.COLOR_GLYPHS)
        self.assertEqual(cells[:10], ["\x1b[31m▬\x1b[0m"] * 10)
        self.assertEqual(cells[10:], ["\x1b[31m─\x1b[0m"] * 10)

    def test_a_row_that_never_showed_a_time_bar_gets_none_when_finished(self):
        b = self._running(60.0)
        b.apply("step_end", dict(id="nx", status="ok", ms=30000, exit=0, summary=None, totals=None))
        self.assertIsNone(jr.row_text(b.rows["nx"], jr.PLAIN_GLYPHS, 200.0)[0])

    def test_line_glyphs_per_palette(self):
        self.assertEqual("".join(jr.time_bar(24, 60, jr.PLAIN_GLYPHS)), "=========-----------")
        self.assertEqual("".join(jr.time_bar(24, 60, jr.NO_COLOR_GLYPHS)), "▬▬▬▬▬▬▬▬▬───────────")
        colored = jr.time_bar(24, 60, jr.COLOR_GLYPHS)
        self.assertEqual(colored[0], "\x1b[36m▬\x1b[0m")
        self.assertEqual(colored[8], "\x1b[36m▬\x1b[0m")
        self.assertEqual(colored[9], "\x1b[2m─\x1b[0m")
        self.assertEqual(len(colored), jr.TIME_BAR_CELLS)
        self.assertEqual(jr._vlen("".join(colored)), jr.TIME_BAR_CELLS)


if __name__ == "__main__":
    unittest.main()
