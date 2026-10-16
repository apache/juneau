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


import json
import unittest

from support import FIXTURES, jr
from test_parsers import feed


def new_parser(cls=jr.PytestParser, step="p"):
    p = cls()
    p.step_id = step
    return p


def ev(parser, lines):
    return feed(parser, "\n".join(lines))


def units(events):
    return [e for e in events if e[0] in ("units", "unit")]


class PluginMarkersTest(unittest.TestCase):
    def test_total_then_tests(self):
        events = ev(new_parser(), [
            '##pytest {"ev":"hello","v":1}', '##pytest {"ev":"total","n":3}',
            '##pytest {"ev":"test","o":"passed","id":"t::a","s":0.1}',
            '##pytest {"ev":"test","o":"failed","id":"t::b","s":0.1}',
            '##pytest {"ev":"test","o":"skipped","id":"t::c","s":0.0}'])
        self.assertEqual(units(events),
                         [("units", "p", 3, "tests"), ("unit", "p", 1, "pass"),
                          ("unit", "p", 2, "fail"), ("unit", "p", 3, "skip")])
        self.assertEqual([e for e in events if e[0] == "tests"][-1], ("tests", "p", 3, 1, 0, 1))

    def test_late_marker_fixes_unit_and_counts_error(self):
        events = ev(new_parser(), [
            '##pytest {"ev":"hello","v":1}', '##pytest {"ev":"total","n":1}',
            '##pytest {"ev":"test","o":"passed","id":"t::a","s":0.1}',
            '##pytest {"ev":"test","o":"error","id":"t::a","s":0.0,"late":1}'])
        self.assertIn(("unit_fix", "p", 0, "fail"), events)
        self.assertEqual([e for e in events if e[0] == "unit"], [("unit", "p", 1, "pass")])   # late does not advance
        self.assertEqual([e for e in events if e[0] == "tests"][-1][4], 1)                      # errors

    def test_markers_never_become_cond_events(self):
        events = ev(new_parser(), ['##pytest {"ev":"hello","v":1}', '##pytest not json'])
        self.assertFalse([e for e in events if e[0] == "cond"])

    def test_larger_total_resends_units(self):
        events = ev(new_parser(), ['##pytest {"ev":"total","n":5}', '##pytest {"ev":"total","n":9}'])
        self.assertEqual([e for e in events if e[0] == "units"], [("units", "p", 5, "tests"), ("units", "p", 9, "tests")])

    def test_no_series_or_sub_events_for_pytest(self):   # spec 4.2: a pytest record has no subs
        events = ev(new_parser(), ['##pytest {"ev":"hello","v":1}',
                                    '##pytest {"ev":"test","o":"passed","id":"a.py::t1","s":1.0}'])
        self.assertEqual([e for e in events if e[0] in ("series", "sub_start", "sub_end")], [])

    def test_late_marker_for_an_evicted_id_is_counted_without_fix(self):
        p = new_parser()
        lines = ['##pytest {"ev":"hello","v":1}']
        lines += ['##pytest {"ev":"test","o":"passed","id":"t::%d","s":0}' % i for i in range(2100)]
        lines.append('##pytest {"ev":"test","o":"error","id":"t::0","s":0,"late":1}')
        events = ev(p, lines)
        self.assertEqual([e for e in events if e[0] == "unit_fix"], [])
        self.assertEqual(p.errors, 1)
        self.assertLessEqual(len(p.index), 2000)

    def test_scraped_progress_is_ignored_after_hello_but_summary_still_counts(self):
        events = ev(new_parser(), ['##pytest {"ev":"hello","v":1}', "1 failed, 2 passed in 0.03s"])
        self.assertEqual([e for e in events if e[0] == "tests"][-1], ("tests", "p", 3, 1, 0, 0))


class FallbackTest(unittest.TestCase):
    def test_collected_n_and_dots(self):
        events = ev(new_parser(), ["collected 6 items", "test_f.py ..F.s  [ 83%]"])
        self.assertIn(("units", "p", 6, "tests"), events)
        self.assertIn(("unit", "p", 5, None), events)       # five result characters on the line

    def test_count_style(self):
        events = ev(new_parser(), ["collected 70 items", "test_f.py ..F  [60/70]"])
        self.assertIn(("unit", "p", 60, None), events)

    def test_verbose_lines_one_each(self):
        events = ev(new_parser(), ["collected 2 items", "tests/test_a.py::test_x PASSED   [ 50%]",
                                    "tests/test_a.py::test_y FAILED   [100%]"])
        self.assertEqual([e for e in events if e[0] == "unit"], [("unit", "p", 1, "pass"), ("unit", "p", 2, "fail")])

    def test_xdist_verbose(self):
        events = ev(new_parser(), ["[gw0] [ 43%] PASSED tests/test_a.py::test_x"])
        self.assertEqual([e for e in events if e[0] == "unit"], [("unit", "p", 1, "pass")])

    def test_xdist_workers_line_m_wins(self):
        events = ev(new_parser(), ["collected 9 items", "2 workers [7 items]"])
        self.assertEqual([e for e in events if e[0] == "units"][-1], ("units", "p", 7, "tests"))

    def test_bare_percent_with_known_n(self):
        events = ev(new_parser(), ["collected 10 items", "[ 43%]"])
        self.assertIn(("unit", "p", 4, None), events)

    def test_bare_percent_without_n_emits_nothing(self):
        events = ev(new_parser(), ["[ 43%]"])
        self.assertEqual(units(events), [])

    def test_dots_without_n_still_show_text_progress(self):
        events = ev(new_parser(), ["test_f.py ..  [ 43%]"])
        self.assertTrue([e for e in events if e[0] == "progress"])

    def test_hello_switches_fallback_off(self):
        p = new_parser()
        ev(p, ['##pytest {"ev":"hello","v":1}'])
        self.assertEqual(units(ev(p, ["collected 6 items", "test_f.py ..  [ 33%]"])), [])


class FixtureGoldenTest(unittest.TestCase):
    """Recorded plugin runs (plain, -v, -q, -n 2) replayed through the parser."""

    def test_recorded_runs_match_frozen_events(self):
        for fixture in sorted(FIXTURES.glob("pytest-*.log")):
            with self.subTest(fixture=fixture.name):
                events = feed(new_parser(), fixture.read_text(encoding="utf-8"))
                got = [list(e) for e in events if e[0] in ("units", "unit", "unit_fix")]
                want = json.loads(fixture.with_suffix(".events.json").read_text(encoding="utf-8"))
                self.assertEqual(got, want)


class PlaywrightUnitsTest(unittest.TestCase):
    def test_playwright_emits_both_numbers(self):
        events = ev(new_parser(jr.PlaywrightParser, "w"), ["[1/4] a.spec.ts", "[2/4] b.spec.ts"])
        self.assertEqual(units(events), [("units", "w", 4, "tests"), ("unit", "w", 1, None), ("unit", "w", 2, None)])


if __name__ == "__main__":
    unittest.main()
