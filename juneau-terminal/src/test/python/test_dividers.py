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
"""Pause dividers and the run number on the board (spec 2.2): the plan event fields, the Board state, rendering and the
pending-tail elision on a short terminal."""

import os
import unittest

import scripts_support  # noqa: F401  (puts juneau_terminal_scripts on the path)
from support import jr, reset_run_state


class SessionSignatureTest(unittest.TestCase):
    def test_session_plan_and_divider_state_accept_the_new_keywords_in_a_plain_run(self):
        import inspect
        from juneau_terminal_scripts import run
        for fn in (run.session, run.Session.plan):
            names = inspect.signature(fn).parameters
            self.assertIn("run", names)
            self.assertIn("dividers", names)
        self.assertIn("history_alias", inspect.signature(run.session).parameters)
        self.assertTrue(hasattr(run.Session, "divider_state"))
        self.assertIn("dividers", run.BOARD_FEATURES)


def new_board(run=1, dividers=()):
    reset_run_state()
    b = jr.Board("actionable", nested=False, full_log=None, clock=lambda: 100.0)
    b.apply("plan", dict(ids=["s1", "s2", "s3"], labels={"s1": "01 One", "s2": "02 Two", "s3": "03 Three"},
                         **({"run": run} if run else {}), **({"dividers": [list(d) for d in dividers]} if dividers else {})))
    return b


def end(b, sid, status="ok", paused=False, note=None):
    b.apply("step_start", dict(id=sid, n=1, title=sid, parent=None))
    if paused:
        b.apply("note", dict(level="pause", text=note or "stop here", href=None, step=sid))
    return b.apply("step_end", dict(id=sid, status=status, ms=10, exit=0, summary=None, totals=None))


def plain(blocks, b):
    out = []
    jr.PlainRenderer(out.append, clock=lambda: 100.0).render(b, blocks)
    return "".join(out)


class DividerStateTest(unittest.TestCase):
    def setUp(self):
        self.addCleanup(reset_run_state)

    def test_a_stops_divider_prints_with_the_run_number_and_rules(self):
        b = new_board(run=3, dividers=[("s1", "send the email", "stops")])
        text = plain(end(b, "s1", paused=True, note="send the email"), b)
        self.assertIn("run 3 stops here: send the email", text)
        self.assertIn("=" * 10, text)

    def test_a_passed_divider_has_no_run_number(self):
        b = new_board(run=3, dividers=[("s1", "send the email", "passed")])
        text = plain(end(b, "s1"), b)
        self.assertIn("run ended: send the email", text)
        self.assertNotIn("3", text.split("run ended")[1])

    def test_a_later_divider_whose_step_ends_without_a_pause_becomes_passed(self):
        b = new_board(dividers=[("s1", "vote closes", "later")])
        end(b, "s1")
        self.assertEqual(b.dividers[0].state, "passed")

    def test_a_failed_step_prints_no_divider(self):
        b = new_board(dividers=[("s1", "x", "later")])
        blocks = end(b, "s1", status="fail")
        self.assertNotIn("divider", [blk[0] for blk in blocks])

    def test_runtime_pause_moves_the_one_stops_divider(self):
        b = new_board(dividers=[("s3", "predicted", "stops")])
        end(b, "s1", paused=True, note="vote still open")
        stops = [d for d in b.dividers if d.state == "stops"]
        self.assertEqual([(d.after, d.text) for d in stops], [("s1", "vote still open")])
        self.assertEqual([d.state for d in b.dividers if d.after == "s3"], ["later"])

    def test_divider_event_updates_state(self):
        b = new_board(dividers=[("s2", "x", "later")])
        b.apply("divider", dict(after="s2", state="passed"))
        self.assertEqual(b.dividers[0].state, "passed")

    def test_pending_items_interleave_dividers_with_pending_rows(self):
        b = new_board(dividers=[("s1", "x", "later")])
        self.assertEqual([i[0] for i in b.pending_items()], ["row", "divider", "row", "row"])

    def test_live_region_shows_the_divider_under_the_running_row(self):
        b = new_board(dividers=[("s1", "send mail", "later")])
        b.apply("step_start", dict(id="s1", n=1, title="s1", parent=None))
        live = jr.LiveRenderer(lambda t: None, size=lambda: os.terminal_size((100, 24)), clock=lambda: 100.0, color=False)
        lines = live.region(b, 99, 22)
        self.assertTrue(any("run 1 stops here: send mail" in ln for ln in lines))

    def test_dividers_are_numbered_in_plan_order_from_the_current_run(self):
        b = new_board(run=2, dividers=[("s1", "a", "passed"), ("s2", "b", "stops"), ("s3", "c", "later")])
        self.assertEqual([b.divider_run(d) for d in b.dividers], [None, 2, 3])

    def test_numbers_follow_the_current_plan_when_the_stop_moves(self):
        b = new_board(run=2, dividers=[("s1", "a", "stops"), ("s2", "b", "later"), ("s3", "c", "later")])
        b.apply("divider", dict(after="s1", state="passed"))
        b.apply("divider", dict(after="s2", state="stops"))
        self.assertEqual([b.divider_run(d) for d in b.dividers], [None, 2, 3])

    def test_a_later_divider_prints_its_own_run_number(self):
        b = new_board(run=2, dividers=[("s1", "a", "stops"), ("s2", "vote closes", "later")])
        live = jr.LiveRenderer(lambda t: None, size=lambda: os.terminal_size((100, 24)), clock=lambda: 100.0, color=False)
        b.apply("step_start", dict(id="s1", n=1, title="s1", parent=None))
        lines = live.region(b, 99, 22)
        self.assertTrue(any("run 3 stops here: vote closes" in ln for ln in lines))


class ElisionTest(unittest.TestCase):
    def setUp(self):
        self.addCleanup(reset_run_state)

    def test_a_short_terminal_keeps_the_running_row_and_overall_and_elides_the_pending_tail(self):
        reset_run_state()
        ids = [f"s{i}" for i in range(25)]
        b = jr.Board("actionable", nested=False, full_log=None, clock=lambda: 100.0)
        b.apply("plan", dict(ids=ids, labels={i: f"{n + 1:02d} Step" for n, i in enumerate(ids)}))
        b.apply("step_start", dict(id="s0", n=1, title="01 Step", parent=None))
        live = jr.LiveRenderer(lambda t: None, size=lambda: os.terminal_size((100, 12)), clock=lambda: 100.0, color=False)
        lines = live.region(b, 99, 10)
        self.assertLessEqual(len(lines), 10)
        self.assertTrue(any("01 Step" in ln for ln in lines))
        self.assertTrue(any(ln.startswith("... ") and "more steps" in ln for ln in lines))

    def test_elide_pending_ignores_cut_dividers_in_the_count(self):
        pend = [("a", True), ("d", False), ("b", True), ("c", True)]
        self.assertEqual(jr.elide_pending(pend, 3), ["a", "d", "... 2 more steps"])
        self.assertEqual(jr.elide_pending(pend, 9), ["a", "d", "b", "c"])
        self.assertEqual(jr.elide_pending(pend, 0), [])
