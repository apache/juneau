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


def unit_events(events):
    return [e for e in events if e[0] in ("unit_start", "unit_dot", "unit_end")]


class PluginMarkersTest(unittest.TestCase):
    def test_file_marker_opens_the_files_unit_once(self):
        events = ev(new_parser(), ['##pytest {"ev":"hello","v":1}', '##pytest {"ev":"file","f":"test_a.py"}',
                                   '##pytest {"ev":"file","f":"test_a.py"}', '##pytest {"ev":"file"}',
                                   '##pytest {"ev":"run","f":"test_b.py"}'])     # run is gone: nothing
        # feed() ends with finish(), which closes the file left open
        self.assertEqual(events, [("unit_start", "test_a.py", "test_a.py", "file"), ("unit_end", "test_a.py", "ok", "file")])

    def test_one_dot_per_test_and_error_is_fail(self):
        events = ev(new_parser(), ['##pytest {"ev":"hello","v":1}', '##pytest {"ev":"file","f":"a.py"}',
                                   _mark("passed", "a.py::t1"), _mark("error", "a.py::t2"),
                                   _mark("skipped", "a.py::t3"), _mark("xfailed", "a.py::t4")])
        self.assertEqual(unit_events(events)[1:-1], [("unit_dot", "a.py", "pass"), ("unit_dot", "a.py", "fail"),
                                                     ("unit_dot", "a.py", "skip"), ("unit_dot", "a.py", "skip")])

    def test_a_file_closes_with_its_last_test(self):
        events = ev(new_parser(), ['##pytest {"ev":"hello","v":1}', total_mark({"a.py": 2, "b.py": 1}),
                                   '##pytest {"ev":"file","f":"a.py"}', _mark("passed", "a.py::t1"),
                                   _mark("failed", "a.py::t2"), '##pytest {"ev":"file","f":"b.py"}',
                                   _mark("passed", "b.py::t1")])
        self.assertEqual(unit_events(events), [
            ("unit_start", "a.py", "a.py", "file"), ("unit_dot", "a.py", "pass"), ("unit_dot", "a.py", "fail"),
            ("unit_end", "a.py", "fail", "file"),
            ("unit_start", "b.py", "b.py", "file"), ("unit_dot", "b.py", "pass"), ("unit_end", "b.py", "ok", "file")])

    def test_a_late_error_after_close_is_a_fail_dot_and_before_close_fails_the_file(self):
        events = ev(new_parser(), ['##pytest {"ev":"hello","v":1}', total_mark({"a.py": 1, "b.py": 2}),
                                   '##pytest {"ev":"file","f":"a.py"}', _mark("passed", "a.py::t1"),
                                   _mark("error", "a.py::t1", late=True),
                                   '##pytest {"ev":"file","f":"b.py"}', _mark("passed", "b.py::t1"),
                                   _mark("error", "b.py::t1", late=True), _mark("passed", "b.py::t2")])
        self.assertEqual(unit_events(events), [
            ("unit_start", "a.py", "a.py", "file"), ("unit_dot", "a.py", "pass"), ("unit_end", "a.py", "ok", "file"),
            ("unit_dot", "a.py", "fail"),
            ("unit_start", "b.py", "b.py", "file"), ("unit_dot", "b.py", "pass"), ("unit_dot", "b.py", "pass"),
            ("unit_end", "b.py", "fail", "file")])

    def test_without_per_file_counts_units_close_in_finish(self):
        p = new_parser()
        events = []
        for line in ['##pytest {"ev":"hello","v":1}', '##pytest {"ev":"total","n":2}',
                     '##pytest {"ev":"file","f":"a.py"}', _mark("failed", "a.py::t1"),
                     '##pytest {"ev":"file","f":"b.py"}', _mark("passed", "b.py::t1")]:
            events += p.on_line(line)
        self.assertNotIn("unit_end", [e[0] for e in events])
        self.assertEqual(unit_events(p.finish(1)), [("unit_end", "a.py", "fail", "file"), ("unit_end", "b.py", "ok", "file")])
        self.assertEqual(unit_events(p.finish(1)), [])          # closed once

    def test_xdist_interleaved_files_and_an_orphan_test(self):
        events = ev(new_parser(), ['##pytest {"ev":"hello","v":1}', total_mark({"a.py": 2, "b.py": 2}),
                                   '##pytest {"ev":"file","f":"a.py"}', _mark("passed", "b.py::t1"),
                                   '##pytest {"ev":"file","f":"b.py"}', _mark("passed", "a.py::t1"),
                                   '##pytest {"ev":"file","f":"a.py"}', _mark("passed", "a.py::t2"),
                                   _mark("passed", "b.py::t2")])
        self.assertEqual(unit_events(events), [
            ("unit_start", "a.py", "a.py", "file"),
            ("unit_start", "b.py", "b.py", "file"), ("unit_dot", "b.py", "pass"),     # the orphan test opens b.py
            ("unit_dot", "a.py", "pass"), ("unit_dot", "a.py", "pass"), ("unit_end", "a.py", "ok", "file"),
            ("unit_dot", "b.py", "pass"), ("unit_end", "b.py", "ok", "file")])

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


def total_mark(files):
    return '##pytest ' + json.dumps({"ev": "total", "n": sum(files.values()), "f": files})


def _mark(outcome, nid, late=False):
    return '##pytest ' + json.dumps({"ev": "test", "o": outcome, "id": nid, "s": 0.0, **({"late": 1} if late else {})})


class FileGrainTest(unittest.TestCase):
    """With per-file counts in the plugin's total marker, the unit is the test file."""

    def test_total_announces_files_not_tests(self):
        events = ev(new_parser(), ['##pytest {"ev":"hello","v":1}', total_mark({"a.py": 2, "b.py": 3})])
        self.assertEqual(units(events), [("units", "p", 2, "files")])

    def test_a_file_finishes_with_its_last_test(self):
        p = new_parser()
        events = ev(p, [total_mark({"a.py": 2, "b.py": 1}), _mark("passed", "a.py::t1"), _mark("passed", "b.py::t1"),
                        _mark("passed", "a.py::t2")])
        self.assertEqual(units(events), [("units", "p", 2, "files"), ("unit", "p", 1, "pass"), ("unit", "p", 2, "pass")])
        self.assertEqual([e for e in events if e[0] == "tests"][-1], ("tests", "p", 3, 0, 0, 0))   # still counted per test

    def test_a_failed_or_errored_test_fails_its_file(self):
        events = ev(new_parser(), [total_mark({"a.py": 2, "b.py": 2}),
                                   _mark("passed", "a.py::t1"), _mark("failed", "a.py::t2"),
                                   _mark("error", "b.py::t1"), _mark("passed", "b.py::t2")])
        self.assertEqual([e for e in events if e[0] == "unit"], [("unit", "p", 1, "fail"), ("unit", "p", 2, "fail")])

    def test_a_file_is_skipped_only_when_every_test_skipped(self):
        events = ev(new_parser(), [total_mark({"a.py": 2, "b.py": 2}),
                                   _mark("skipped", "a.py::t1"), _mark("xfailed", "a.py::t2"),
                                   _mark("skipped", "b.py::t1"), _mark("passed", "b.py::t2")])
        self.assertEqual([e for e in events if e[0] == "unit"], [("unit", "p", 1, "skip"), ("unit", "p", 2, "pass")])

    def test_interleaved_files_finish_in_completion_order(self):    # xdist: tests of different files mix
        events = ev(new_parser(), [total_mark({"a.py": 2, "b.py": 2}),
                                   _mark("passed", "a.py::t1"), _mark("passed", "b.py::t1"),
                                   _mark("failed", "b.py::t2"), _mark("passed", "a.py::t2")])
        self.assertEqual([e for e in events if e[0] == "unit"], [("unit", "p", 1, "fail"), ("unit", "p", 2, "pass")])

    def test_a_late_error_fixes_a_finished_file_and_fails_an_open_one(self):
        events = ev(new_parser(), [total_mark({"a.py": 1, "b.py": 2}), _mark("passed", "a.py::t1"),
                                   _mark("error", "a.py::t1", late=True),
                                   _mark("passed", "b.py::t1"), _mark("error", "b.py::t1", late=True),
                                   _mark("passed", "b.py::t2")])
        self.assertIn(("unit_fix", "p", 0, "fail"), events)
        self.assertEqual([e for e in events if e[0] == "unit"][-1], ("unit", "p", 2, "fail"))

    def test_total_without_file_counts_stays_per_test(self):         # an older plugin, or the fallback
        events = ev(new_parser(), ['##pytest {"ev":"total","n":3}', _mark("passed", "a.py::t1")])
        self.assertEqual(units(events), [("units", "p", 3, "tests"), ("unit", "p", 1, "pass")])

    def test_scraped_xdist_progress_has_no_file_names_and_stays_per_test(self):
        events = ev(new_parser(), ["collected 6 items", "..F  [ 50%]"])
        self.assertEqual(units(events)[0], ("units", "p", 6, "tests"))


class BarTestsTest(unittest.TestCase):
    """bar="tests" on pytest: one cell per test even when the plugin's total marker carries per-file counts."""

    def parser(self):
        p = new_parser()
        p.bar = "tests"
        return p

    def test_a_file_map_still_announces_and_counts_tests(self):
        events = ev(self.parser(), ['##pytest {"ev":"hello","v":1}', total_mark({"a.py": 2, "b.py": 1}),
                                    _mark("passed", "a.py::t1"), _mark("failed", "b.py::t1"),
                                    _mark("passed", "a.py::t2")])
        self.assertEqual(units(events), [("units", "p", 3, "tests"), ("unit", "p", 1, "pass"), ("unit", "p", 2, "fail"),
                                         ("unit", "p", 3, "pass")])

    def test_each_file_line_still_closes_with_its_last_test(self):
        events = ev(self.parser(), [total_mark({"a.py": 2, "b.py": 1}), _mark("passed", "a.py::t1"),
                                    _mark("passed", "b.py::t1"), _mark("failed", "a.py::t2")])
        ends = [e for e in unit_events(events) if e[0] == "unit_end"]
        self.assertEqual(ends, [("unit_end", "b.py", "ok", "file"), ("unit_end", "a.py", "fail", "file")])

    def test_a_late_error_corrects_its_tests_cell(self):
        events = ev(self.parser(), [total_mark({"a.py": 2}), _mark("passed", "a.py::t1"),
                                    _mark("passed", "a.py::t2"), _mark("error", "a.py::t1", late=True)])
        self.assertIn(("unit_fix", "p", 0, "fail"), events)

    def test_bar_files_without_a_file_map_falls_back_to_tests(self):
        p = new_parser()
        p.bar = "files"
        events = ev(p, ['##pytest {"ev":"total","n":3}', _mark("passed", "a.py::t1")])
        self.assertEqual(units(events), [("units", "p", 3, "tests"), ("unit", "p", 1, "pass")])


if __name__ == "__main__":
    unittest.main()
