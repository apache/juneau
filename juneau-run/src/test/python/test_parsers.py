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
"""Parsers: pure line-to-event tests against recorded fixtures (see fixtures/ and the module README)."""

import json
import os
import re
import tempfile
import unittest
from pathlib import Path
from unittest import mock

from support import FIXTURES, jr


def feed(parser, text, exit_code=0):
    events = []
    for line in text.splitlines():
        events += parser.on_line(line)
    return events + parser.finish(exit_code)


def maven(fixture, args=("test",), cwd="/nonexistent", exit_code=0):
    parser = jr.MavenParser()
    parser.step_id = "mvn"
    parser.prepare(["mvn", *args], None, cwd)
    text = (FIXTURES / fixture).read_text(encoding="utf-8")
    return parser, feed(parser, text, exit_code)


def kinds(events, kind):
    return [e for e in events if e[0] == kind]


def class_line_totals(text):
    """Sums every Surefire class result line in a fixture, independently of the parser."""
    rx = re.compile(r"Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+), Time elapsed: .* -- in ")
    totals = [0, 0, 0, 0]
    for m in rx.finditer(text):
        for i in range(4):
            totals[i] += int(m.group(i + 1))
    return tuple(totals)


class MavenParserTest(unittest.TestCase):
    def test_serial_success_opens_and_closes_each_module_in_order(self):
        _, events = maven("maven-serial-success.log")
        flow = [(e[0], e[1]) for e in events if e[0] in ("sub_start", "sub_end")]
        self.assertEqual(flow, [
            ("sub_start", "mvn/demo-parent"), ("sub_end", "mvn/demo-parent"),
            ("sub_start", "mvn/mod-a"), ("sub_end", "mvn/mod-a"),
            ("sub_start", "mvn/mod-b"), ("sub_end", "mvn/mod-b"),
            ("sub_start", "mvn/mod-c"), ("sub_end", "mvn/mod-c")])
        self.assertEqual([e[2] for e in kinds(events, "sub_start")], [1, 2, 3, 4])

    def test_a_one_module_success_counts_its_module_at_finish(self):
        from test_history import SINGLE_MODULE_LOG
        with tempfile.TemporaryDirectory() as tmp:
            (Path(tmp) / "pom.xml").write_text("<project><artifactId>solo</artifactId></project>", encoding="utf-8")
            parser = jr.MavenParser()
            parser.step_id = "mvn"
            parser.prepare(["mvn", "verify"], None, tmp)
            ok = feed(parser, SINGLE_MODULE_LOG)
            parser = jr.MavenParser()
            parser.step_id = "mvn"
            parser.prepare(["mvn", "verify"], None, tmp)
            failed = feed(parser, SINGLE_MODULE_LOG.replace("BUILD SUCCESS", "BUILD FAILURE"), exit_code=1)
        self.assertEqual(kinds(ok, "reactor")[0][1], 1)
        self.assertEqual(kinds(ok, "unit"), [("unit", "mvn", 1, "pass")])
        self.assertEqual(kinds(failed, "unit"), [])           # a failed build leaves its module uncounted

    def test_one_project_of_a_multi_module_tree_is_resized_to_one_at_its_building_line_and_counted_at_finish(self):
        from test_history import PL_MODULE_LOG
        with tempfile.TemporaryDirectory() as tmp:
            write_demo_tree(Path(tmp), packaging="pom")
            parser = jr.MavenParser()
            parser.step_id = "mvn"
            parser.prepare(["mvn", "verify", "-pl", "mod-b"], None, tmp)
            lines = PL_MODULE_LOG.splitlines()
            at = next(i for i, line in enumerate(lines) if line.startswith("[INFO] Building "))
            before = [e for line in lines[:at] for e in parser.on_line(line)]
            building = parser.on_line(lines[at])
            rest = [e for line in lines[at + 1:] for e in parser.on_line(line)] + parser.finish(0)
            parser = jr.MavenParser()
            parser.step_id = "mvn"
            parser.prepare(["mvn", "verify", "-pl", "mod-b"], None, tmp)
            failed = feed(parser, PL_MODULE_LOG.replace("BUILD SUCCESS", "BUILD FAILURE"), exit_code=1)
        self.assertEqual(kinds(before, "reactor")[0][1], 4)       # sized from the pom tree
        self.assertEqual(kinds(before, "units"), [])
        self.assertEqual(kinds(building, "units"), [("units", "mvn", 1, "modules")])
        self.assertEqual(kinds(rest, "units"), [])                # sent once, not again at finish
        self.assertEqual(kinds(rest, "unit"), [("unit", "mvn", 1, "pass")])
        self.assertEqual(kinds(failed, "units"), [("units", "mvn", 1, "modules")])
        self.assertEqual(kinds(failed, "unit"), [])               # a failed build leaves its project uncounted

    def test_a_one_module_tree_is_resized_only_by_its_reactor(self):
        from test_history import SINGLE_MODULE_LOG
        with tempfile.TemporaryDirectory() as tmp:
            (Path(tmp) / "pom.xml").write_text("<project><artifactId>solo</artifactId></project>", encoding="utf-8")
            parser = jr.MavenParser()
            parser.step_id = "mvn"
            parser.prepare(["mvn", "verify"], None, tmp)
            events = feed(parser, SINGLE_MODULE_LOG)
        self.assertEqual(kinds(events, "units"), [("units", "mvn", 1, "modules")])     # finish's, as before

    def test_a_multi_module_tree_needs_a_single_project_building_line_to_be_counted_as_one(self):
        from test_history import QUIET_LOG
        jar = "[INFO] Building jar: /tmp/mod-a/target/mod-a-1.0.jar\n"     # the jar plugin's line, not a project's
        for text in (QUIET_LOG, QUIET_LOG + jar):
            with tempfile.TemporaryDirectory() as tmp:
                write_demo_tree(Path(tmp), packaging="pom")
                parser = jr.MavenParser()
                parser.step_id = "mvn"
                parser.prepare(["mvn", "-q", "verify"], None, tmp)
                events = feed(parser, text)
            self.assertEqual(kinds(events, "units") + kinds(events, "unit"), [], text)

    def test_a_multi_module_build_is_counted_by_its_modules_alone(self):
        for fixture, args in (("maven-serial-success.log", ("test",)), ("maven-parallel-success.log", ("test", "-T", "2"))):
            with tempfile.TemporaryDirectory() as tmp:
                write_demo_tree(Path(tmp))
                _, events = maven(fixture, args, cwd=tmp)
            self.assertEqual(kinds(events, "units") + kinds(events, "unit"), [], fixture)
        _, events = maven("maven-serial-success.log")              # no pom tree: no reactor, nothing to count
        self.assertEqual(kinds(events, "units") + kinds(events, "unit"), [])

    def test_titles_and_reactor_index(self):
        _, events = maven("maven-serial-success.log")
        self.assertEqual(kinds(events, "sub_start")[1], ("sub_start", "mvn/mod-a", 2, "Demo Module A"))

    def test_parent_totals_are_the_sum_of_class_lines_and_results_is_not_double_counted(self):
        text = (FIXTURES / "maven-serial-success.log").read_text(encoding="utf-8")
        self.assertEqual(len(re.findall(r"^\[INFO\] Results:$", text, re.M)), 3)   # three Results blocks, none counted
        _, events = maven("maven-serial-success.log")
        self.assertEqual(kinds(events, "tests")[-1], ("tests", "mvn", *class_line_totals(text)))
        self.assertEqual(class_line_totals(text), (6, 0, 0, 1))

    def test_test_failure_marks_the_module_fails_skips_the_rest_and_notes_the_first_failing_test(self):
        _, events = maven("maven-test-failure.log", exit_code=1)
        ends = {e[1]: e[2] for e in kinds(events, "sub_end")}
        self.assertEqual((ends["mvn/mod-b"], ends["mvn/demo-module-c"]), ("fail", "skip"))
        note = kinds(events, "note")[-1]
        self.assertEqual(note[1], "error")
        self.assertIn("Module failed: Demo Module B", note[2])
        self.assertIn("brokenSum", note[2])
        self.assertIn("expected: <5> but was: <3>", note[2])
        self.assertLessEqual(len(note[2].splitlines()), 12)

    def test_skipped_modules_that_never_started_still_get_a_step_and_end(self):
        _, events = maven("maven-test-failure.log", exit_code=1)
        flow = [e[:2] for e in events if e[1] == "mvn/demo-module-c"]
        self.assertEqual(flow, [("sub_start", "mvn/demo-module-c"), ("sub_end", "mvn/demo-module-c")])

    def test_compile_failure_note_names_the_module_and_the_compiler_error(self):
        _, events = maven("maven-compile-failure.log", exit_code=1)
        note = kinds(events, "note")[-1]
        self.assertIn("Module failed: Demo Module B", note[2])
        self.assertIn("incompatible types", note[2])

    def test_parallel_build_defers_module_ends_to_the_summary_and_counts_exactly(self):
        parser, events = maven("maven-parallel-success.log", args=("test", "-T2"))
        self.assertTrue(parser.parallel)
        flow = [e[0] for e in events if e[0] in ("sub_start", "sub_end")]
        self.assertEqual(flow, ["sub_start"] * 4 + ["sub_end"] * 4)   # nothing ends before the Reactor Summary
        self.assertEqual(kinds(events, "tests")[-1], ("tests", "mvn", 6, 0, 0, 1))

    def test_interleaved_headers_in_the_recorded_juneau_log(self):
        text = (FIXTURES / "maven-juneau-T1C.log").read_text(encoding="utf-8")
        parser, events = maven("maven-juneau-T1C.log", args=("test", "-T1C", "-Drat.skip=true"))
        starts, ends = kinds(events, "sub_start"), kinds(events, "sub_end")
        self.assertGreater(len(starts), 20)
        self.assertEqual({e[1] for e in starts}, {e[1] for e in ends})
        self.assertEqual(len(starts), len({e[1] for e in starts}))
        self.assertTrue(all(e[2] == "ok" for e in ends))
        first_end = next(i for i, e in enumerate(events) if e[0] == "sub_end")
        self.assertGreater(len([e for e in events[:first_end] if e[0] == "sub_start"]), 2)   # several open at once
        self.assertEqual(kinds(events, "tests")[-1][2:], class_line_totals(text))

    def test_plugin_header_attributes_class_lines_to_the_module_best_effort(self):
        with tempfile.TemporaryDirectory() as tmp:
            write_demo_tree(Path(tmp))
            _, events = maven("maven-serial-success.log", cwd=tmp)
        first = [e for e in kinds(events, "tests") if e[1] == "mvn/mod-b"][0]
        self.assertEqual(first, ("tests", "mvn/mod-b", 3, 0, 0, 1))

    def test_exact_module_counts_and_reports_come_from_the_report_directory(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            write_demo_tree(root)
            reports = root / "mod-a" / "target" / "surefire-reports"
            reports.mkdir(parents=True)
            (reports / "TEST-x.CalcTest.xml").write_text(
                '<testsuite tests="7" failures="1" errors="2" skipped="3"/>', encoding="utf-8")
            _, events = maven("maven-serial-success.log", cwd=str(root))
        report = [e for e in kinds(events, "report") if e[1] == "mvn/mod-a"]
        self.assertEqual(report, [("report", "mvn/mod-a", "surefire", str(reports))])
        mod_a = [e for e in kinds(events, "tests") if e[1] == "mvn/mod-a"][-1]
        self.assertEqual(mod_a, ("tests", "mvn/mod-a", 7, 1, 2, 3))

    def test_unresolved_module_directory_produces_one_info_note(self):
        _, events = maven("maven-serial-success.log", cwd="/nonexistent")
        notes = [e for e in kinds(events, "note") if "mvn/mod-a" == e[3]]
        self.assertEqual(len(notes), 1)
        self.assertEqual(notes[0][1], "info")

    def test_artifact_id_to_directory_map_from_a_nested_pom_tree(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            write_demo_tree(root)
            nested = root / "mod-a" / "inner"
            nested.mkdir()
            (root / "mod-a" / "pom.xml").write_text(
                '<project xmlns="http://maven.apache.org/POM/4.0.0"><artifactId>mod-a</artifactId>'
                '<name>Demo Module A</name><modules><module>inner</module></modules></project>', encoding="utf-8")
            (nested / "pom.xml").write_text(
                '<project><artifactId>inner-x</artifactId><name>${project.artifactId}</name></project>', encoding="utf-8")
            dirs, names = jr.read_pom_tree(root)
        self.assertEqual(sorted(dirs), ["demo-parent", "inner-x", "mod-a", "mod-b", "mod-c"])
        self.assertEqual(dirs["inner-x"], nested)
        self.assertEqual(names["Demo Module A"], "mod-a")
        self.assertNotIn("${project.artifactId}", names)

    def test_titles_resolve_to_artifact_ids_when_the_pom_tree_is_known(self):
        with tempfile.TemporaryDirectory() as tmp:
            write_demo_tree(Path(tmp))
            _, events = maven("maven-serial-success.log", cwd=tmp)
        self.assertIn("mvn/mod-a", [e[1] for e in kinds(events, "sub_start")])

    def test_without_a_pom_tree_ids_come_from_the_headers_and_totals_attach_to_the_module_that_ran_them(self):
        parser, events = maven("maven-serial-success.log", cwd="/nonexistent")
        self.assertEqual(parser.names, {})
        self.assertEqual([e[1] for e in kinds(events, "sub_start")],
                         ["mvn/demo-parent", "mvn/mod-a", "mvn/mod-b", "mvn/mod-c"])
        per_module = {e[1]: e[2:] for e in kinds(events, "tests") if e[1] != "mvn"}
        self.assertEqual(per_module, {"mvn/mod-a": (2, 0, 0, 0), "mvn/mod-b": (3, 0, 0, 1), "mvn/mod-c": (1, 0, 0, 0)})

    def test_totals_attach_to_the_failing_module_in_the_failure_fixture(self):
        _, events = maven("maven-test-failure.log", exit_code=1)
        mod_b = [e for e in kinds(events, "tests") if e[1] == "mvn/mod-b"][-1]
        self.assertEqual(mod_b, ("tests", "mvn/mod-b", 3, 1, 0, 1))

    def _class_line_total(self, *lines):
        parser = jr.MavenParser()
        parser.step_id = "mvn"
        parser.prepare(["mvn", "test"], None, "/nonexistent")
        events = []
        for line in lines:
            events += parser.on_line(line)
        return kinds(events, "tests")[-1][2:] if kinds(events, "tests") else None

    def test_a_class_line_spliced_onto_an_open_line_still_counts(self):
        spliced = ("test_env (test_scripts_git.ReadersTest.test_env) ... [INFO] Tests run: 11907, Failures: 0, "
                   "Errors: 0, Skipped: 2, Time elapsed: 2.974 s -- in org.apache.juneau.foo.BarTest")
        self.assertEqual(self._class_line_total(spliced), (11907, 0, 0, 2))

    def test_a_class_line_with_a_display_name_still_counts(self):
        named = "[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in R - Phase 4 general-purpose API"
        self.assertEqual(self._class_line_total(named), (13, 0, 0, 0))

    def test_a_display_name_class_line_attributes_to_the_current_module(self):
        parser = jr.MavenParser()
        parser.step_id = "mvn"
        parser.prepare(["mvn", "test"], None, "/nonexistent")
        parser.on_line("[INFO] --- surefire:3.2.5:test (default-test) @ mod-a ---")
        events = parser.on_line("[INFO] Tests run: 13, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 0.006 s <<< FAILURE! -- in R - Phase 4 API")
        self.assertIn(("class_done", "mvn/mod-a", "fail"), events)

    def test_a_plain_class_line_still_attributes_to_its_module(self):
        parser = jr.MavenParser()
        parser.step_id = "mvn"
        parser.prepare(["mvn", "test"], None, "/nonexistent")
        parser.on_line("[INFO] --- surefire:3.2.5:test (default-test) @ mod-a ---")
        events = parser.on_line("[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.01 s -- in x.CalcTest")
        self.assertIn(("class_done", "mvn/mod-a", "pass"), events)
        self.assertEqual(kinds(events, "tests")[-1][2:], (4, 0, 0, 0))

    def _exec_module(self, header="[INFO] --- exec:3.5.0:exec (python-unittest) @ mod-a ---"):
        parser = jr.MavenParser()
        parser.step_id = "mvn"
        parser.prepare(["mvn", "test"], None, "/nonexistent")
        parser.on_line(header)
        return parser

    def _unittest(self, parser, *lines):
        events = []
        for line in lines:
            events += parser.on_line(line)
        return events

    def test_a_unittest_summary_under_an_exec_header_counts_for_its_module(self):
        parser = self._exec_module()
        events = self._unittest(parser, "----------------------------------------------------------------------",
                                "Ran 1100 tests in 12.345s", "", "OK")
        self.assertEqual(kinds(events, "tests")[-1], ("tests", "mvn", 1100, 0, 0, 0))
        self.assertIn(("class_done", "mvn/mod-a", "pass"), events)
        self.assertIn(("series", "mvn", "surefire_s", "mvn/mod-a", 12.345), events)
        self.assertEqual(parser.mod["mvn/mod-a"], [1100, 0, 0, 0])

    def test_a_unittest_summary_with_skips_counts_them(self):
        parser = self._exec_module("[INFO] --- exec-maven-plugin:3.1.0:exec (python-unittest) @ mod-a ---")
        events = self._unittest(parser, "Ran 10 tests in 0.5s", "", "OK (skipped=3)")
        self.assertEqual(kinds(events, "tests")[-1], ("tests", "mvn", 10, 0, 0, 3))

    def test_a_failed_unittest_summary_marks_the_module_failed(self):
        parser = self._exec_module()
        events = self._unittest(parser, "Ran 20 tests in 1.0s", "", "FAILED (failures=2, errors=1, skipped=4)")
        self.assertEqual(kinds(events, "tests")[-1], ("tests", "mvn", 20, 2, 1, 4))
        self.assertIn(("class_done", "mvn/mod-a", "fail"), events)

    def test_a_unittest_summary_spliced_onto_other_output_still_counts(self):
        parser = self._exec_module()
        events = self._unittest(parser, "[INFO] Running x.FooTest Ran 7 tests in 0.1s", "[INFO] OK")
        self.assertEqual(kinds(events, "tests")[-1], ("tests", "mvn", 7, 0, 0, 0))

    def test_a_unittest_summary_outside_an_exec_context_does_not_count(self):
        parser = jr.MavenParser()
        parser.step_id = "mvn"
        parser.prepare(["mvn", "test"], None, "/nonexistent")
        self.assertEqual(self._unittest(parser, "Ran 5 tests in 0.1s", "OK"), [])
        parser.on_line("[INFO] --- surefire:3.2.5:test (default-test) @ mod-a ---")
        self.assertEqual(self._unittest(parser, "Ran 5 tests in 0.1s", "OK"), [])

    def test_a_unittest_summary_survives_other_modules_plugin_headers_under_a_parallel_build(self):
        parser = self._exec_module()
        for other in ("mod-b", "mod-c", "mod-d"):
            for header in ("compiler:3.11.0:compile (default-compile)", "surefire:3.2.5:test (default-test)",
                           "jar:3.3.0:jar (default-jar)"):
                parser.on_line(f"[INFO] --- {header} @ {other} ---")
                parser.on_line("[INFO] Nothing to do")
        events = self._unittest(parser, "----------------------------------------------------------------------",
                                "Ran 1107 tests in 93.334s", "", "OK (skipped=13)")
        self.assertEqual(kinds(events, "tests")[-1], ("tests", "mvn", 1107, 0, 0, 13))
        self.assertIn(("class_done", "mvn/mod-a", "pass"), events)
        self.assertEqual(parser.mod["mvn/mod-a"], [1107, 0, 0, 13])

    def test_pending_exec_modules_are_credited_in_order_and_each_only_once(self):
        parser = self._exec_module()
        parser.on_line("[INFO] --- exec:3.5.0:exec (python-unittest) @ mod-b ---")
        parser.on_line("[INFO] --- jar:3.3.0:jar (default-jar) @ mod-c ---")
        self._unittest(parser, "Ran 3 tests in 0.1s", "OK")
        self._unittest(parser, "Ran 4 tests in 0.2s", "OK")
        self.assertEqual(parser.mod["mvn/mod-a"], [3, 0, 0, 0])
        self.assertEqual(parser.mod["mvn/mod-b"], [4, 0, 0, 0])
        self.assertEqual(self._unittest(parser, "Ran 9 tests in 0.1s", "OK"), [])
        self.assertEqual(parser.total, 7)

    def test_a_ran_line_without_a_unittest_result_line_does_not_count(self):
        parser = self._exec_module()
        self.assertEqual(self._unittest(parser, "Ran 5 tests in 0.1s", "something else", "OK"), [])
        self.assertEqual(parser.total, 0)

    def test_a_module_with_only_python_tests_has_tests(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "py" / "src" / "test" / "python").mkdir(parents=True)
            parser = jr.MavenParser()
            parser.dirs = {"py": root / "py", "none": root / "none"}
            (root / "none").mkdir()
            self.assertTrue(parser._has_tests("py"))
            self.assertFalse(parser._has_tests("none"))

    def test_plugin_lines_and_building_lines_share_one_id_scheme(self):
        parser, events = maven("maven-serial-success.log", cwd="/nonexistent")
        opened = {e[1] for e in kinds(events, "sub_start")}
        self.assertTrue({e[1] for e in kinds(events, "tests") if e[1] != "mvn"} <= opened)

    def test_a_header_artifact_already_used_by_another_module_is_never_reused(self):
        parser = jr.MavenParser()
        parser.step_id = "mvn"
        parser.parallel = True
        for line in ("[INFO] ---------< g:one >---------", "[INFO] Building First 1.0   [1/2]",
                     "[INFO] Building Second 1.0   [2/2]"):
            events = parser.on_line(line)
        self.assertEqual(events[-1][:2], ("sub_start", "mvn/second"))

    def test_reactor_summary_rows_without_dot_leaders_are_parsed_and_trimmed(self):
        rows = {"Apache Juneau REST Server Console UI \u2014 FreeMarker DataTables": "SUCCESS [  5.713 s]",
                "Apache Juneau NG REST Client \u2014 Apache HttpClient 4.5 Transport": "SUCCESS [  7.178 s]",
                "Apache Juneau NG REST Client \u2014 Jetty HttpClient Transport": "FAILURE [  8.557 s]",
                "Apache Juneau REST Server Console UI \u2014 FreeMarker": "SUCCESS [ 23.226 s]"}
        for name, tail in rows.items():
            line = f"[INFO] {name} {tail}" if len(name) > 55 else f"[INFO] {name} .. {tail}"
            m = jr.MavenParser.SUMMARY_ROW.match(line)
            self.assertIsNotNone(m, line)
            self.assertEqual(m.group(1).strip(), name)

    def test_the_recorded_juneau_summary_has_dotless_rows_and_every_module_is_closed(self):
        text = (FIXTURES / "maven-juneau-T1C.log").read_text(encoding="utf-8")
        dotless = [l for l in text.splitlines()
                   if re.match(r"^\[INFO\] .+ (SUCCESS|FAILURE|SKIPPED) \[", l) and " ..  " not in l and " .. " not in l]
        self.assertGreaterEqual(len(dotless), 3)
        _, events = maven("maven-juneau-T1C.log", args=("test", "-T1C"))
        ended = {e[1]: e for e in kinds(events, "sub_end")}
        started = {e[1]: e for e in kinds(events, "sub_start")}
        self.assertEqual(set(started), set(ended))
        titles = {e[3] for e in started.values()}
        for line in dotless:
            title = re.match(r"^\[INFO\] (.+?)(?: \.+)? (?:SUCCESS|FAILURE|SKIPPED)", line).group(1).strip()
            self.assertIn(title, titles)

    def test_finish_closes_every_open_sub_step_even_when_the_summary_never_arrives(self):
        parser = jr.MavenParser()
        parser.step_id = "mvn"
        events = []
        for line in ("[INFO] ---------< g:one >---------", "[INFO] Building First 1.0   [1/2]",
                     "[INFO] ---------< g:two >---------", "[INFO] Building Second 1.0   [2/2]"):
            events += parser.on_line(line)
        events += parser.finish(1)
        starts = [e[1] for e in kinds(events, "sub_start")]
        ends = kinds(events, "sub_end")
        self.assertEqual(sorted(e[1] for e in ends), sorted(starts))
        self.assertEqual({e[2] for e in ends}, {"ok", "skip"})   # First closed by Second's Building line; Second by finish

    def test_a_missed_summary_row_is_closed_by_finish_with_skip(self):
        text = (FIXTURES / "maven-serial-success.log").read_text(encoding="utf-8")
        text = text.replace("[INFO] Demo Module C ...................................... SUCCESS [  0.374 s]\n", "")
        parser = jr.MavenParser()
        parser.step_id = "mvn"
        parser.prepare(["mvn", "test"], None, "/nonexistent")
        events = feed(parser, text)
        starts = [e[1] for e in kinds(events, "sub_start")]
        ends = {e[1]: e[2] for e in kinds(events, "sub_end")}
        self.assertEqual(sorted(ends), sorted(starts))
        self.assertEqual(ends["mvn/mod-c"], "skip")

    def test_quiet_run_degrades_to_generic_behaviour(self):
        _, events = maven("maven-quiet-failure.log", args=("-q", "test"), exit_code=1)
        self.assertEqual(kinds(events, "sub_start"), [])
        self.assertGreater(len(kinds(events, "cond")), 5)

    def test_prepare_never_changes_the_command(self):
        parser = jr.MavenParser()
        cmd = ["lock.sh", "mvn", "test", "-T1C"]
        self.assertEqual(parser.prepare(cmd, None, "/nonexistent"), (cmd, None))
        self.assertTrue(parser.parallel)

    def test_parallel_is_detected_inside_a_shell_command_string(self):
        parser = jr.MavenParser()
        parser.prepare(["/bin/sh", "-c", "lock.sh mvn test -T1C -Drat.skip=true"], None, "/nonexistent")
        self.assertTrue(parser.parallel)
        serial = jr.MavenParser()
        serial.prepare(["/bin/sh", "-c", "lock.sh mvn test -Dtest='A-Tb'"], None, "/nonexistent")
        self.assertFalse(serial.parallel)

    def test_osc_sequences_are_stripped_with_either_terminator(self):
        self.assertEqual(jr.strip_ansi("\x1b]8;;http://x\x07link\x1b]8;;\x07"), "link")
        self.assertEqual(jr.strip_ansi("\x1b]0;title\x1b\\[INFO] Building A 1.0   [1/2]"), "[INFO] Building A 1.0   [1/2]")
        self.assertEqual(jr.strip_ansi("\x1b[1;34mINFO\x1b[m"), "INFO")

    def test_ansi_colour_is_stripped_before_matching(self):
        parser = jr.MavenParser()
        parser.step_id = "mvn"
        events = parser.on_line("\x1b[1;34mINFO\x1b[m")   # unrelated colour must not break matching
        self.assertEqual(events, [])
        events = parser.on_line("[\x1b[1;34mINFO\x1b[m] Building \x1b[36mDemo\x1b[m 1.0        [1/2]")
        self.assertEqual(events[-1][:2], ("sub_start", "mvn/demo"))

    def test_interleaved_classes_are_credited_to_the_module_that_contains_them(self):
        with tempfile.TemporaryDirectory() as tmp:
            write_t1c_tree(Path(tmp))
            _, events = maven("maven-juneau-T1C.log", args=("test", "-T1C"), cwd=tmp)
        per = {e[1]: e[2:] for e in kinds(events, "tests") if e[1] != "mvn"}
        self.assertEqual(per["mvn/juneau-commons"], (231, 0, 0, 0))
        self.assertEqual(per["mvn/juneau-secret-macos-keychain"], (16, 0, 0, 1))
        self.assertEqual(per["mvn/juneau-rest-server-views-markdown"], (19, 0, 0, 0))
        done = kinds(events, "class_done")
        self.assertEqual(len(done), 15)
        self.assertEqual([e[2] for e in done if e[1] == "mvn/juneau-secret-macos-keychain"], ["pass", "pass"])

    def test_a_class_found_in_several_modules_falls_back_to_the_surefire_header(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            write_demo_tree(root)
            for m in "abc":
                class_file(root / f"mod-{m}", "x.CalcTest")    # x.CalcTest runs in all three modules
            _, events = maven("maven-serial-success.log", cwd=tmp)
        per = {e[1]: e[2:] for e in kinds(events, "tests") if e[1] != "mvn"}
        self.assertEqual(per, {"mvn/mod-a": (2, 0, 0, 0), "mvn/mod-b": (3, 0, 0, 1), "mvn/mod-c": (1, 0, 0, 0)})

    def test_class_done_outcomes(self):
        parser = jr.MavenParser()
        parser.step_id = "mvn"
        parser.on_line("[INFO] --- surefire:3.5.4:test (default-test) @ mod-a ---")
        outcomes = []
        for line in ("Tests run: 3, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 1 s <<< FAILURE! -- in x.A",
                     "Tests run: 3, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 1 s <<< ERROR! -- in x.B",
                     "Tests run: 2, Failures: 0, Errors: 0, Skipped: 2, Time elapsed: 0 s -- in x.C",
                     "Tests run: 2, Failures: 0, Errors: 0, Skipped: 1, Time elapsed: 0 s -- in x.D",
                     "Tests run: 0, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0 s -- in x.E"):
            outcomes += [e[2] for e in parser.on_line(line) if e[0] == "class_done"]
        self.assertEqual(outcomes, ["fail", "fail", "skip", "pass", "pass"])

    def demo_tree_with_tests(self, root, with_tests=("a",)):
        write_demo_tree(root)
        (root / "pom.xml").write_text(
            '<project><artifactId>demo-parent</artifactId><name>Demo Parent</name><packaging>pom</packaging>'
            '<modules><module>mod-a</module><module>mod-b</module><module>mod-c</module></modules></project>',
            encoding="utf-8")
        for m in with_tests:
            (root / f"mod-{m}" / "src" / "test" / "java").mkdir(parents=True)

    def test_compile_and_test_completion_per_artifact(self):
        with tempfile.TemporaryDirectory() as tmp:
            self.demo_tree_with_tests(Path(tmp))
            _, events = maven("maven-serial-success.log", cwd=tmp)
        self.assertEqual([e for e in events if e[0] in ("compiled", "tested")], [
            ("compiled", "mvn/mod-a", True), ("tested", "mvn/mod-a"),       # after testCompile; tested at its end
            ("compiled", "mvn/mod-b", True), ("tested", "mvn/mod-b"),       # no src/test/java: right after compile
            ("compiled", "mvn/mod-c", True), ("tested", "mvn/mod-c"),
        ])
        a = events.index(("compiled", "mvn/mod-a", True))
        self.assertLess(a, events.index(next(e for e in events if e[:2] == ("sub_end", "mvn/mod-a"))))

    def test_compile_mode_sends_no_tested_events(self):
        with tempfile.TemporaryDirectory() as tmp:
            self.demo_tree_with_tests(Path(tmp), with_tests="abc")
            _, events = maven("maven-serial-success.log", args=("clean", "install", "-DskipTests"), cwd=tmp)
        self.assertEqual(kinds(events, "tested"), [])
        self.assertEqual([e[1] for e in kinds(events, "compiled")], ["mvn/mod-a", "mvn/mod-b", "mvn/mod-c"])

    def test_tests_mode_comes_from_the_command(self):
        for cmd, expected in ((["mvn", "test"], True), (["mvn", "install", "-DskipTests"], False),
                              (["mvn", "install", "-Dmaven.test.skip=true"], False),
                              (["/bin/sh", "-c", "lock.sh mvn clean install -T1C -DskipTests"], False)):
            with self.subTest(cmd=cmd):
                parser = jr.MavenParser()
                parser.prepare(cmd, None, "/nonexistent")
                self.assertEqual(parser.tests_mode, expected)

    def test_goals_that_stop_before_surefire_are_not_tests_mode(self):
        for args, expected in ((["test-compile"], False), (["compile"], False), (["clean", "compile"], False),
                               (["-pl", "foo", "test-compile"], False), (["-T", "1C", "-P", "x", "process-classes"], False),
                               (["-D", "a=b", "-f", "mod-b/pom.xml", "test-compile"], False),
                               (["test"], True), (["install"], True), (["surefire:test"], True),
                               (["clean", "install"], True), (["compile", "test"], True), ([], True), (["-pl", "foo"], True),
                               (["-DskipTests", "install"], False)):
            with self.subTest(args=args):
                parser = jr.MavenParser()
                parser.prepare(["mvn", *args], None, "/nonexistent")
                self.assertEqual(parser.tests_mode, expected)
        for line, expected in (("lock.sh mvn -pl foo test-compile", False), ("lock.sh mvn -T1C clean test-compile", False),
                               ("lock.sh mvn -pl compile test", True), ("lock.sh mvn test-compile && ./after.sh", True)):
            with self.subTest(line=line):
                parser = jr.MavenParser()
                parser.prepare(["/bin/sh", "-c", line], None, "/nonexistent")
                self.assertEqual(parser.tests_mode, expected)

    def test_the_reactor_event_comes_from_the_tree_size_then_the_build_order_block(self):
        with tempfile.TemporaryDirectory() as tmp:
            self.demo_tree_with_tests(Path(tmp))
            _, events = maven("maven-serial-success.log", cwd=tmp)
        self.assertEqual(events[0], ("reactor", 4, [], True))     # poms only come from a Build Order block
        # first line (no poms yet), the fixture's Reactor Build Order block, then the first Building line
        self.assertEqual(kinds(events, "reactor"), [("reactor", 4, [], True), ("reactor", 4, ["mvn/demo-parent"], True),
                                                    ("reactor", 4, ["mvn/demo-parent"], True)])
        _, no_tree = maven("maven-serial-success.log")
        self.assertEqual(kinds(no_tree, "reactor"), [("reactor", 4, [], True)] * 2)    # no tree: block, then Building

    def test_packaging_from_the_pom_tree(self):
        with tempfile.TemporaryDirectory() as tmp:
            self.demo_tree_with_tests(Path(tmp))
            packaging = {}
            jr.read_pom_tree(Path(tmp), packaging)
        self.assertEqual(packaging, {"demo-parent": "pom", "mod-a": "jar", "mod-b": "jar", "mod-c": "jar"})

    def test_prepare_honours_the_file_option(self):
        with tempfile.TemporaryDirectory() as tmp:
            write_demo_tree(Path(tmp))
            for cmd in (["mvn", "-Pjs-tests", "-f", "mod-b/pom.xml", "test"],
                        ["/bin/sh", "-c", "lock.sh mvn -Pjs-tests --file=mod-b/pom.xml test"]):
                with self.subTest(cmd=cmd):
                    parser = jr.MavenParser()
                    parser.prepare(cmd, None, tmp)
                    self.assertEqual(sorted(parser.dirs), ["mod-b"])

    def test_a_compile_error_turns_its_module_red_once_and_is_a_failure(self):
        _, events = maven("maven-compile-failure.log", args=("clean", "install", "-DskipTests"), exit_code=1)
        self.assertIn(("compiled", "mvn/mod-a", True), events)
        self.assertEqual([e for e in kinds(events, "compiled") if e[1] == "mvn/mod-b"], [("compiled", "mvn/mod-b", False)])
        self.assertEqual(kinds(events, "failure")[0], (
            "failure", "[ERROR] /work/demo/mod-b/src/main/java/x/Bad.java:[1,21] incompatible types: "
                       "java.lang.String cannot be converted to int", "mvn"))

    def test_a_compile_error_is_matched_to_its_module_by_path(self):
        with tempfile.TemporaryDirectory() as tmp:
            write_demo_tree(Path(tmp))
            text = (FIXTURES / "maven-compile-failure.log").read_text(encoding="utf-8").replace(
                "[ERROR] /work/demo/mod-b/", f"[ERROR] {tmp}/mod-c/")
            parser = jr.MavenParser()
            parser.step_id = "mvn"
            parser.prepare(["mvn", "install", "-DskipTests"], None, tmp)
            events = feed(parser, text, 1)
        self.assertIn(("compiled", "mvn/mod-c", False), events)

    def parser_for_demo_tree(self, tmp, cwd=None):
        write_demo_tree(Path(tmp))
        parser = jr.MavenParser()
        parser.step_id = "mvn"
        parser.prepare(["mvn", "install", "-DskipTests"], None, cwd or tmp)
        return parser

    def test_a_failed_compiler_goal_reddens_its_module_even_without_a_path_line(self):
        with tempfile.TemporaryDirectory() as tmp:
            parser = self.parser_for_demo_tree(tmp)
            events = feed(parser, "\n".join([
                "[INFO] --- maven-compiler-plugin:3.14.0:compile (default-compile) @ mod-a ---",
                "[INFO] --- maven-compiler-plugin:3.14.0:compile (default-compile) @ mod-b ---",
                "[ERROR] error: release version 99 not supported",
                "[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.14.0:compile "
                "(default-compile) on project mod-a: Fatal error compiling: error: release version 99 not supported",
            ]), 0)
        self.assertEqual(kinds(events, "compiled"), [("compiled", "mvn/mod-a", False)])
        self.assertEqual(len(kinds(events, "failure")), 1)

    def test_a_failed_compiler_goal_after_a_path_error_does_not_publish_twice(self):
        _, events = maven("maven-compile-failure.log", args=("clean", "install", "-DskipTests"), exit_code=1)
        self.assertEqual([e for e in kinds(events, "compiled") if e[1] == "mvn/mod-b"], [("compiled", "mvn/mod-b", False)])
        self.assertEqual(len(kinds(events, "failure")), 1)

    def test_a_compile_error_within_another_modules_failure_trace_is_not_swallowed(self):
        with tempfile.TemporaryDirectory() as tmp:
            parser = self.parser_for_demo_tree(tmp)
            events = feed(parser, "\n".join([
                "[ERROR] x.CalcTest.brokenSum -- Time elapsed: 0.007 s <<< FAILURE!",
                "org.opentest4j.AssertionFailedError: expected: <5> but was: <3>",
                f"[ERROR] {tmp}/mod-c/src/main/java/x/Bad.java:[1,2] cannot find symbol",
            ]), 1)
        self.assertIn(("compiled", "mvn/mod-c", False), events)
        self.assertIn(("cond", f"[ERROR] {tmp}/mod-c/src/main/java/x/Bad.java:[1,2] cannot find symbol"), events)

    def test_an_ambiguous_class_ignores_a_surefire_header_that_is_not_among_the_hits(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            write_demo_tree(root)
            for m in "ab":
                class_file(root / f"mod-{m}", "x.CalcTest")
            parser = jr.MavenParser()
            parser.step_id = "mvn"
            parser.prepare(["mvn", "test"], None, tmp)
            parser.on_line("[INFO] --- surefire:3.5.4:test (default-test) @ mod-c ---")
            events = parser.on_line("Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1 s -- in x.CalcTest")
        self.assertEqual([e[1] for e in kinds(events, "class_done")], ["mvn/mod-a"])

    def test_compile_error_paths_match_through_symlinks_and_dotdot(self):
        with tempfile.TemporaryDirectory() as tmp:
            real = Path(os.path.realpath(tmp))
            link = real.parent / (real.name + "-link")
            link.symlink_to(real)
            self.addCleanup(link.unlink)
            for path in (real, f"{link}/mod-a/../mod-c/.."):
                with self.subTest(path=str(path)):
                    parser = self.parser_for_demo_tree(str(real), cwd=str(link))
                    events = feed(parser, f"[ERROR] {path}/mod-c/src/main/java/x/Bad.java:[1,2] boom", 1)
                    self.assertIn(("compiled", "mvn/mod-c", False), events)

    def test_stale_only_report_directory_sends_no_report_event(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            write_demo_tree(root)
            reports = root / "mod-a" / "target" / "surefire-reports"
            reports.mkdir(parents=True)
            path = reports / "TEST-x.Old.xml"
            path.write_text('<testsuite tests="1" failures="0" errors="0" skipped="0"/>', encoding="utf-8")
            os.utime(path, (1.0, 1.0))
            parser = jr.MavenParser()
            parser.step_id = "mvn"
            parser.prepare(["mvn", "test"], None, tmp)
            events = feed(parser, "[INFO] Building Demo Module A 1.0 [1/2]\n[INFO] Building Demo Module B 1.0 [2/2]", 0)
        self.assertEqual(kinds(events, "report"), [])

    def test_a_failed_test_trace_is_a_failure(self):
        parser, events = maven("maven-test-failure.log", exit_code=1)
        self.assertEqual(kinds(events, "failure"), [("failure", "\n".join(parser.failure_trace), "mvn")])

    def test_a_quiet_failure_sends_the_generic_tail_as_a_failure(self):
        fixture = (FIXTURES / "maven-quiet-failure.log").read_text(encoding="utf-8").splitlines()
        lead = [f"[ERROR] earlier line {i}" for i in range(jr.GenericParser.TAIL)]    # so the tail is cut
        parser = jr.MavenParser()
        parser.step_id = "mvn"
        parser.prepare(["mvn", "-q", "test"], None, "/nonexistent")
        events = feed(parser, "\n".join(lead + fixture), 1)
        failure = kinds(events, "failure")
        self.assertEqual(len(failure), 1)
        self.assertEqual(failure[0][2], "mvn")
        tail = failure[0][1].splitlines()
        self.assertEqual(tail, (lead + fixture)[-jr.GenericParser.TAIL:])
        self.assertEqual(tail, [e[1] for e in kinds(events, "cond")][-len(tail):])   # the tail follows any live cond lines

    def test_report_xml_older_than_the_step_is_ignored_with_two_seconds_of_slack(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            write_demo_tree(root)
            reports = root / "mod-a" / "target" / "surefire-reports"
            reports.mkdir(parents=True)
            for name, tests, failures, mtime in (("Old", 100, 9, 999_997.0), ("Slack", 3, 1, 999_999.0),
                                                 ("New", 4, 0, 1_000_005.0)):
                path = reports / f"TEST-x.{name}.xml"
                path.write_text(f'<testsuite tests="{tests}" failures="{failures}" errors="0" skipped="0"/>',
                                encoding="utf-8")
                os.utime(path, (mtime, mtime))
            with mock.patch.object(jr.time, "time", return_value=1_000_000.0):
                _, events = maven("maven-serial-success.log", cwd=str(root))
        mod_a = [e for e in kinds(events, "tests") if e[1] == "mvn/mod-a"][-1]
        self.assertEqual(mod_a, ("tests", "mvn/mod-a", 7, 1, 0, 0))

    def test_failsafe_reports_count_with_surefire_reports(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            write_demo_tree(root)
            for dirname, tests in (("surefire-reports", 7), ("failsafe-reports", 2)):
                d = root / "mod-a" / "target" / dirname
                d.mkdir(parents=True)
                (d / "TEST-x.T.xml").write_text(f'<testsuite tests="{tests}" failures="0" errors="0" skipped="0"/>',
                                               encoding="utf-8")
            _, events = maven("maven-serial-success.log", cwd=str(root))
        mod_a = [e for e in kinds(events, "tests") if e[1] == "mvn/mod-a"][-1]
        self.assertEqual(mod_a, ("tests", "mvn/mod-a", 9, 0, 0, 0))


def write_demo_tree(root, packaging="jar"):
    (root / "pom.xml").write_text(
        '<project xmlns="http://maven.apache.org/POM/4.0.0"><artifactId>demo-parent</artifactId><name>Demo Parent</name>'
        f'<packaging>{packaging}</packaging>'
        '<modules><module>mod-a</module><module>mod-b</module><module>mod-c</module></modules></project>', encoding="utf-8")
    for m in "abc":
        (root / f"mod-{m}").mkdir(exist_ok=True)
        (root / f"mod-{m}" / "pom.xml").write_text(
            f'<project><artifactId>mod-{m}</artifactId><name>Demo Module {m.upper()}</name></project>', encoding="utf-8")


T1C_MODULES = {
    "juneau-commons": ("Apache Juneau Commons", ("org.apache.juneau.commons.", "org.apache.juneau.utils.")),
    "juneau-secret-macos-keychain": ("Apache Juneau Secret Store - macOS Keychain", ("org.apache.juneau.secret.",)),
    "juneau-rest-server-views-markdown": ("Apache Juneau REST Server Views Markdown",
                                          ("org.apache.juneau.rest.server.views.markdown.",)),
}


def class_file(module_dir, fqn):
    path = module_dir / "target" / "test-classes" / Path(*fqn.split(".")[:-1]) / (fqn.rsplit(".", 1)[1] + ".class")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(b"")


def write_t1c_tree(root):
    """The three modules that ran tests in the T1C fixture, each holding the classes the fixture says it ran."""
    text = (FIXTURES / "maven-juneau-T1C.log").read_text(encoding="utf-8")
    fqns = set(re.findall(r"-- in (\S+)\s*$", text, re.M))
    modules = "".join(f"<module>{a}</module>" for a in T1C_MODULES)
    (root / "pom.xml").write_text(
        f"<project><artifactId>juneau</artifactId><name>Apache Juneau</name><packaging>pom</packaging>"
        f"<modules>{modules}</modules></project>", encoding="utf-8")
    for artifact, (name, prefixes) in T1C_MODULES.items():
        d = root / artifact
        d.mkdir()
        (d / "pom.xml").write_text(f"<project><artifactId>{artifact}</artifactId><name>{name}</name></project>",
                                   encoding="utf-8")
        for fqn in fqns:
            if fqn.startswith(prefixes):
                class_file(d, fqn)


PYTEST_FAIL = """\
============================= test session starts ==============================
collected 3 items

test_x.py .F.                                                            [100%]

=================================== FAILURES ===================================
___________________________________ test_b _____________________________________

    def test_b():
>       assert 1 == 2
E       assert 1 == 2

test_x.py:5: AssertionError
=========================== short test summary info ============================
FAILED test_x.py::test_b - assert 1 == 2
========================= 1 failed, 2 passed in 0.03s ==========================
"""

PLAYWRIGHT_FAIL = """\
Running 4 tests using 1 worker
[1/4] tests/a.spec.ts:3:1 › passes
[2/4] tests/a.spec.ts:7:1 › fails
  1) tests/a.spec.ts:7:1 › fails ──────────────────────────────────────────────────────────

    Error: expect(received).toBe(expected)

    Expected: 2
    Received: 1

[3/4] tests/a.spec.ts:11:1 › skipped
[4/4] tests/a.spec.ts:15:1 › passes too

  1 failed
    tests/a.spec.ts:7:1 › fails ─────────────────────────────────────────────────────────────
  1 skipped
  2 passed (1.2s)
"""


class PytestParserTest(unittest.TestCase):
    def parser(self):
        p = jr.PytestParser()
        p.step_id = "py"
        return p

    def test_counts_condensed_lines_and_report(self):
        with tempfile.TemporaryDirectory() as tmp:
            xml = Path(tmp) / "py-pytest.xml"
            xml.write_text("<testsuites/>", encoding="utf-8")
            p = self.parser()
            p.report_path = str(xml)
            events = feed(p, PYTEST_FAIL, 1)
        self.assertEqual(kinds(events, "tests")[-1], ("tests", "py", 3, 1, 0, 0))
        cond = [e[1] for e in kinds(events, "cond")]
        self.assertEqual(cond[0], "=========================== short test summary info ============================")
        self.assertIn("FAILED test_x.py::test_b - assert 1 == 2", cond)
        self.assertEqual(kinds(events, "report"), [("report", "py", "junitxml", str(xml))])

    def test_quiet_summary_line_without_fences(self):
        events = feed(self.parser(), "1 failed, 2 passed, 1 skipped, 1 error in 0.03s\n", 1)
        self.assertEqual(kinds(events, "tests")[-1], ("tests", "py", 5, 1, 1, 1))

    @mock.patch.object(jr, "find_pytest_plugin", return_value=None)
    def test_prepare_adds_junitxml_only_when_markers_are_on_and_artifacts_exist(self, _):
        with mock.patch.dict(os.environ, {"RUN_MARKERS": "1", "RUN_ARTIFACTS": "/art"}):
            p = self.parser()
            cmd, _ = p.prepare(["pytest", "-q"], None, None)
            self.assertEqual(cmd, ["pytest", "--junitxml=/art/py-pytest.xml", "-q"])
        with mock.patch.dict(os.environ, {"RUN_ARTIFACTS": "/art"}):
            os.environ.pop("RUN_MARKERS", None)
            self.assertEqual(self.parser().prepare(["pytest"], None, None)[0], ["pytest"])

    def test_prepare_without_run_artifacts_writes_the_junitxml_to_a_temp_dir_and_still_reports_it(self):
        with mock.patch.dict(os.environ, {"RUN_MARKERS": "1"}):
            os.environ.pop("RUN_ARTIFACTS", None)
            p = self.parser()
            cmd, _ = p.prepare(["pytest"], None, None)
            self.assertEqual(cmd[-1], f"--junitxml={p.report_path}")
            self.assertEqual(Path(p.report_path).name, "py-pytest.xml")
            self.assertTrue(Path(p.report_path).parent.is_dir())
            self.assertTrue(str(Path(p.report_path).parent).startswith(tempfile.gettempdir()) or
                            Path(p.report_path).parent.name.startswith("juneau-run-"))
            Path(p.report_path).write_text("<testsuites/>", encoding="utf-8")
            self.assertEqual(p.finish(0), [("report", "py", "junitxml", p.report_path)])

    @mock.patch.object(jr, "find_pytest_plugin", return_value=None)
    def test_existing_junitxml_is_kept_and_reported(self, _):
        p = self.parser()
        cmd, _ = p.prepare(["pytest", "--junitxml=out.xml"], None, None)
        self.assertEqual((cmd, p.report_path), (["pytest", "--junitxml=out.xml"], "out.xml"))

    def progress(self, text):
        return [e[2] for e in kinds(feed(self.parser(), text), "progress")]

    def test_default_output_reports_percent_and_the_file(self):
        self.assertEqual(self.progress("test_f1.py ......... [ 43%]\ntest_f2.py ..F.s    [100%]\n"),
                         ["43% \u00b7 test_f1.py", "100% \u00b7 test_f2.py"])

    def test_progress_events_carry_the_step_id(self):
        events = feed(self.parser(), "test_f1.py ...... [ 43%]\n")
        self.assertEqual(kinds(events, "progress"), [("progress", "py", "43% \u00b7 test_f1.py")])

    def test_a_wrapped_line_has_no_file_so_the_last_one_is_remembered(self):
        text = "tests/test_f1.py ................................ [ 40%]\n.........  [ 49%]\n"
        self.assertEqual(self.progress(text), ["40% \u00b7 tests/test_f1.py", "49% \u00b7 tests/test_f1.py"])

    def test_quiet_output_has_no_file(self):
        self.assertEqual(self.progress("..........F....sxX [ 51%]\n"), ["51%"])

    def test_count_style(self):
        self.assertEqual(self.progress("test_f1.py ......  [ 60/70]\n.....  [65/70]\n"),
                         ["60/70 \u00b7 test_f1.py", "65/70 \u00b7 test_f1.py"])

    def test_xdist_style_has_no_file(self):
        self.assertEqual(self.progress("..................................                                       [ 52%]\n"),
                         ["52%"])

    def test_output_without_percentages_reports_nothing(self):
        text = "collected 3 items\n\ntest_f1.py ...\n\n3 passed in 0.01s\n"
        self.assertEqual(self.progress(text), [])

    def test_lines_that_merely_resemble_progress_are_ignored_without_raising(self):
        for line in ("[ 43%]", "see [ 43%]", "q" * 5000 + " [ 1%]", ". " * 3000, "test.py ...... [ 4x%]", "",
                     "FAILED test_x.py::t - assert 1 == 2 [ 50%]"):
            self.assertEqual(self.progress(line + "\n"), [], line[:40])


class PlaywrightParserTest(unittest.TestCase):
    def parser(self):
        p = jr.PlaywrightParser()
        p.step_id = "pw"
        return p

    def test_progress_failures_and_final_summary(self):
        events = feed(self.parser(), PLAYWRIGHT_FAIL, 1)
        progress = [e for e in kinds(events, "tests")][:2]
        self.assertEqual(progress, [("tests", "pw", 1, 0, 0, 0), ("tests", "pw", 2, 0, 0, 0)])
        self.assertEqual(kinds(events, "tests")[-1], ("tests", "pw", 4, 1, 0, 1))
        cond = [e[1] for e in kinds(events, "cond")]
        self.assertTrue(any("1) tests/a.spec.ts:7:1" in c for c in cond))
        self.assertTrue(any("Expected: 2" in c for c in cond))
        self.assertIn("  2 passed (1.2s)", cond)

    def prep(self, cmd, env=None, where="/r/juneau_run_playwright.cjs", console=True, interactive=False):
        parser = self.parser()
        parser.interactive = interactive
        with mock.patch.object(jr, "find_playwright_reporter", return_value=where), \
                mock.patch.object(jr, "_console", object() if console else None):
            return parser.prepare(cmd, env, None)

    def clean_env(self, **values):
        """os.environ with none of the variables prepare() reads, plus values."""
        patch = mock.patch.dict(os.environ, values)
        patch.start()
        self.addCleanup(patch.stop)
        for name in ("RUN_MARKERS", "RUN_ARTIFACTS", "PLAYWRIGHT_JSON_OUTPUT_NAME", "JUNEAU_RUN_PLAYWRIGHT_REPORTER",
                     "PW_TEST_REPORTER", "JUNEAU_RUN_PLAYWRIGHT_JSON", "JUNEAU_RUN_PLAYWRIGHT_MARKERS",
                     "PLAYWRIGHT_HTML_OPEN", "PW_TEST_HTML_REPORT_OPEN"):
            if name not in values:
                os.environ.pop(name, None)

    def test_prepare_appends_the_reporter_through_the_environment_and_asks_it_for_json_under_markers(self):
        self.clean_env(RUN_MARKERS="1", RUN_ARTIFACTS="/art")
        cmd, env = self.prep(["npx", "playwright", "test"], {"A": "b"})
        self.assertEqual(cmd, ["npx", "playwright", "test"])        # no --reporter: the config's reporters still run
        self.assertEqual(env["PW_TEST_REPORTER"], "/r/juneau_run_playwright.cjs")
        self.assertEqual(env["JUNEAU_RUN_PLAYWRIGHT_JSON"], str(Path("/art") / "pw-playwright.json"))
        self.assertNotIn("JUNEAU_RUN_PLAYWRIGHT_MARKERS", env)
        self.assertNotIn("PLAYWRIGHT_JSON_OUTPUT_NAME", env)      # would redirect a json reporter in the config
        self.assertEqual(env["A"], "b")

    def test_a_console_run_gets_the_reporter_and_no_json(self):
        self.clean_env(RUN_ARTIFACTS="/art")
        cmd, env = self.prep(["npx", "playwright", "test"], {})
        self.assertEqual(cmd, ["npx", "playwright", "test"])
        self.assertEqual(env["PW_TEST_REPORTER"], "/r/juneau_run_playwright.cjs")
        self.assertNotIn("JUNEAU_RUN_PLAYWRIGHT_JSON", env)

    def test_a_plain_run_with_no_session_or_markers_is_left_alone(self):
        self.clean_env()
        cmd, env = self.prep(["npx", "playwright", "test"], {}, console=False)
        self.assertEqual((cmd, env), (["npx", "playwright", "test"], {}))

    def test_an_interactive_run_keeps_playwrights_output_and_gets_only_the_json_under_markers(self):
        self.clean_env(RUN_ARTIFACTS="/art")
        self.assertEqual(self.prep(["playwright", "test"], {}, interactive=True), (["playwright", "test"], {}))
        os.environ["RUN_MARKERS"] = "1"
        cmd, env = self.prep(["playwright", "test"], {}, interactive=True)
        self.assertEqual(cmd, ["playwright", "test"])
        self.assertEqual((env["PW_TEST_REPORTER"], env["JUNEAU_RUN_PLAYWRIGHT_MARKERS"]),
                         ("/r/juneau_run_playwright.cjs", "0"))      # prints nothing: only the report file is added
        self.assertEqual(env["JUNEAU_RUN_PLAYWRIGHT_JSON"], str(Path("/art") / "pw-playwright.json"))
        os.environ.pop("RUN_ARTIFACTS")
        self.assertEqual(self.prep(["playwright", "test"], {}, interactive=True), (["playwright", "test"], {}))

    def test_a_reporter_the_caller_names_is_respected(self):
        self.clean_env(RUN_MARKERS="1", RUN_ARTIFACTS="/art")
        for cmd in (["playwright", "test", "--reporter=list"], ["playwright", "test", "--reporter", "list"]):
            self.assertEqual(self.prep(cmd, {}), (cmd, {}))
        self.assertEqual(self.prep(["playwright", "test"], {"PW_TEST_REPORTER": "mine.js"})[1], {"PW_TEST_REPORTER": "mine.js"})

    def test_a_json_path_the_caller_set_is_kept_and_reported(self):
        self.clean_env(RUN_MARKERS="1", RUN_ARTIFACTS="/art")
        parser = self.parser()
        with mock.patch.object(jr, "find_playwright_reporter", return_value="/r/x.cjs"), mock.patch.object(jr, "_console", None):
            env = parser.prepare(["playwright", "test"], {"JUNEAU_RUN_PLAYWRIGHT_JSON": "/mine.json"}, None)[1]
        self.assertEqual((env["JUNEAU_RUN_PLAYWRIGHT_JSON"], parser.report_path), ("/mine.json", "/mine.json"))

    def test_when_the_reporter_is_not_added_a_json_report_the_caller_asked_playwright_for_is_announced(self):
        self.clean_env(RUN_MARKERS="1", RUN_ARTIFACTS="/art")
        parser = self.parser()
        with mock.patch.object(jr, "_console", None):
            parser.prepare(["playwright", "test", "--reporter=json"], {"PLAYWRIGHT_JSON_OUTPUT_NAME": "/pw.json"}, None)
        self.assertEqual(parser.report_path, "/pw.json")

    def test_a_flag_that_only_starts_with_reporter_is_not_a_reporter(self):
        self.clean_env()
        self.assertEqual(self.prep(["playwright", "test", "--reporters-dir=x"], {})[1]["PW_TEST_REPORTER"],
                         "/r/juneau_run_playwright.cjs")

    def test_the_opt_out_or_a_missing_reporter_leaves_the_run_alone(self):
        self.clean_env(JUNEAU_RUN_PLAYWRIGHT_REPORTER="0", RUN_MARKERS="1", RUN_ARTIFACTS="/art")
        self.assertEqual(self.prep(["playwright", "test"], {}), (["playwright", "test"], {}))
        os.environ.pop("JUNEAU_RUN_PLAYWRIGHT_REPORTER")
        self.assertEqual(self.prep(["playwright", "test"], {}, where=None), (["playwright", "test"], {}))

    def test_a_reporter_path_with_a_comma_is_fine(self):
        self.clean_env()
        self.assertEqual(self.prep(["playwright", "test"], {}, where="/a,b/r.cjs")[1]["PW_TEST_REPORTER"], "/a,b/r.cjs")

    def test_an_injected_run_outside_a_terminal_never_opens_the_html_report(self):
        # html's open-on-failure serves the report and waits whenever stdin is a terminal, which a console run's is
        self.clean_env()
        self.assertEqual(self.prep(["playwright", "test"], {})[1]["PLAYWRIGHT_HTML_OPEN"], "never")
        for name in ("PLAYWRIGHT_HTML_OPEN", "PW_TEST_HTML_REPORT_OPEN"):
            env = self.prep(["playwright", "test"], {name: "always"})[1]
            self.assertEqual((env[name], env.get("PLAYWRIGHT_HTML_OPEN", "always")), ("always", "always"), name)
        os.environ.update(RUN_MARKERS="1", RUN_ARTIFACTS="/art")
        self.assertNotIn("PLAYWRIGHT_HTML_OPEN", self.prep(["playwright", "test"], {}, interactive=True)[1])

    def test_an_injected_run_outside_a_terminal_drops_an_inherited_markers_off(self):
        self.clean_env()
        self.assertNotIn("JUNEAU_RUN_PLAYWRIGHT_MARKERS",
                         self.prep(["playwright", "test"], {"JUNEAU_RUN_PLAYWRIGHT_MARKERS": "0"})[1])

    def test_the_opt_out_is_read_from_the_commands_environment(self):
        self.clean_env(RUN_MARKERS="1")
        env = {"JUNEAU_RUN_PLAYWRIGHT_REPORTER": "0"}
        self.assertEqual(self.prep(["playwright", "test"], env), (["playwright", "test"], env))
        self.assertIn("PW_TEST_REPORTER", self.prep(["playwright", "test"], {"JUNEAU_RUN_PLAYWRIGHT_REPORTER": "1"})[1])

    def test_a_leftover_report_is_removed_so_a_cancelled_run_announces_none(self):
        with tempfile.TemporaryDirectory() as tmp:
            self.clean_env(RUN_MARKERS="1", RUN_ARTIFACTS=tmp)
            old = Path(tmp) / "pw-playwright.json"
            old.write_text("{}", encoding="utf-8")
            parser = self.parser()
            with mock.patch.object(jr, "find_playwright_reporter", return_value="/r/x.cjs"), \
                    mock.patch.object(jr, "_console", None):
                parser.prepare(["playwright", "test"], {}, None)
            self.assertFalse(old.exists())
            self.assertEqual(kinds(parser.finish(-15), "report"), [])

    def test_interrupted_tests_in_the_summary_count_as_skipped(self):
        p = self.parser()
        events = [e for line in ("  1 failed", "  2 interrupted", "  3 passed") for e in p.on_line(line)]
        self.assertEqual(kinds(events, "tests")[-1], ("tests", "pw", 6, 1, 0, 2))

    def marker(self, ev, **fields):
        return "##playwright " + json.dumps({"ev": ev, **fields})

    def test_reporter_markers_open_dot_and_close_each_file(self):
        p = self.parser()
        lines = [self.marker("hello", v=1), self.marker("begin", f={"a.spec.ts": 2, "b.spec.ts": 1}),
                 self.marker("start", f="a.spec.ts"), self.marker("test", f="a.spec.ts", o="pass"),
                 self.marker("start", f="b.spec.ts"), self.marker("start", f="a.spec.ts"),
                 self.marker("test", f="a.spec.ts", o="fail"), self.marker("test", f="b.spec.ts", o="skip")]
        events = [e for line in lines for e in p.on_line(line) if e[0].startswith("unit_")]
        self.assertEqual(events, [("unit_start", "a.spec.ts", "a.spec.ts", "file"), ("unit_dot", "a.spec.ts", "pass"),
                                  ("unit_start", "b.spec.ts", "b.spec.ts", "file"), ("unit_dot", "a.spec.ts", "fail"),
                                  ("unit_end", "a.spec.ts", "fail", "file"), ("unit_dot", "b.spec.ts", "skip"),
                                  ("unit_end", "b.spec.ts", "ok", "file")])

    def test_the_bar_counts_files_from_markers_once_the_reporter_says_hello(self):
        p = self.parser()
        self.assertEqual(p.on_line(self.marker("hello", v=1)), [])
        self.assertEqual(p.on_line(self.marker("begin", f={"a.ts": 2, "b.ts": 1})), [("units", "pw", 2, "files")])
        self.assertEqual(p.on_line("[1/3] a.ts:3:1 › passes"), [])      # the line reporter's count is ignored
        events = p.on_line(self.marker("test", f="a.ts", o="fail"))    # a.ts has a test to go: no cell yet
        self.assertEqual((kinds(events, "unit"), kinds(events, "tests")), ([], [("tests", "pw", 1, 1, 0, 0)]))
        self.assertEqual(p.on_line(self.marker("test", f="b.ts", o="skip"))[:2],
                         [("unit", "pw", 1, "skip"), ("tests", "pw", 2, 1, 0, 1)])
        self.assertEqual(p.on_line(self.marker("test", f="a.ts", o="pass"))[:2],
                         [("unit", "pw", 2, "fail"), ("tests", "pw", 3, 1, 0, 1)])
        self.assertEqual(kinds(p.on_line("  1 failed"), "tests"), [("tests", "pw", 1, 1, 0, 0)])  # the summary still wins

    def test_bar_tests_counts_one_cell_per_test(self):
        p = self.parser()
        p.bar = "tests"
        self.assertEqual(p.on_line(self.marker("hello", v=1)), [])
        self.assertEqual(p.on_line(self.marker("begin", f={"a.ts": 2, "b.ts": 1})), [("units", "pw", 3, "tests")])
        self.assertEqual(p.on_line(self.marker("test", f="a.ts", o="fail"))[:2],
                         [("unit", "pw", 1, "fail"), ("tests", "pw", 1, 1, 0, 0)])
        self.assertEqual(p.on_line(self.marker("test", f="b.ts", o="skip"))[:2],
                         [("unit", "pw", 2, "skip"), ("tests", "pw", 2, 1, 0, 1)])
        events = p.on_line(self.marker("test", f="a.ts", o="pass"))
        self.assertEqual(kinds(events, "unit"), [("unit", "pw", 3, "pass")])
        self.assertIn(("unit_end", "a.ts", "fail", "file"), events)      # the file's line still closes with its last test

    def file_cells(self, begin, tests):
        """The unit events of a reporter run: hello, begin, then a test marker per (file, outcome)."""
        p = self.parser()
        lines = [self.marker("hello", v=1), self.marker("begin", f=begin)]
        lines += [self.marker("test", f=f, o=o) for f, o in tests]
        return [e for line in lines for e in p.on_line(line) if e[0] in ("units", "unit")]

    def test_a_file_cell_fails_with_any_failure_skips_only_when_all_skipped_and_else_passes(self):
        events = self.file_cells({"a.ts": 2, "b.ts": 2, "c.ts": 2},
                                 [("a.ts", "pass"), ("a.ts", "fail"), ("b.ts", "skip"), ("b.ts", "skip"),
                                  ("c.ts", "skip"), ("c.ts", "pass")])
        self.assertEqual(events, [("units", "pw", 3, "files"), ("unit", "pw", 1, "fail"), ("unit", "pw", 2, "skip"),
                                  ("unit", "pw", 3, "pass")])

    def test_a_flaky_test_is_one_pass_result_and_passes_its_file(self):
        # the reporter sends one test marker per test, with its final outcome: a retry that passed is "pass"
        self.assertEqual(self.file_cells({"a.ts": 1}, [("a.ts", "pass")])[1:], [("unit", "pw", 1, "pass")])

    def test_files_take_cells_in_the_order_they_finish(self):
        events = self.file_cells({"a.ts": 2, "b.ts": 1}, [("a.ts", "fail"), ("b.ts", "pass"), ("a.ts", "pass")])
        self.assertEqual(events[1:], [("unit", "pw", 1, "pass"), ("unit", "pw", 2, "fail")])

    def test_a_file_cell_is_added_once_however_many_results_its_file_gets(self):
        # results past the count begin gave the file (a repeated or late marker) add nothing: its cell is in
        events = self.file_cells({"a.ts": 1}, [("a.ts", "pass"), ("a.ts", "fail")])
        self.assertEqual(events[1:], [("unit", "pw", 1, "pass")])

    def test_a_test_whose_file_is_not_in_begin_gets_no_cell(self):
        events = self.file_cells({"a.ts": 1}, [("b.ts", "pass"), ("a.ts", "pass")])
        self.assertEqual(events[1:], [("unit", "pw", 1, "pass")])

    def test_a_second_begin_resends_the_units_only_when_the_file_count_changes(self):
        p = self.parser()
        for line in (self.marker("hello", v=1), self.marker("begin", f={"a.ts": 1})):
            p.on_line(line)
        self.assertEqual(p.on_line(self.marker("begin", f={"b.ts": 3})), [])
        self.assertEqual(p.on_line(self.marker("begin", f={"b.ts": 3, "c.ts": 1})), [("units", "pw", 2, "files")])

    def test_a_second_begin_with_the_same_files_adds_no_cells(self):
        p = self.parser()
        for line in (self.marker("hello", v=1), self.marker("begin", f={"a.ts": 1, "b.ts": 1})):
            p.on_line(line)
        self.assertEqual(kinds(p.on_line(self.marker("test", f="a.ts", o="pass")), "unit"), [("unit", "pw", 1, "pass")])
        self.assertEqual(p.on_line(self.marker("begin", f={"a.ts": 1, "b.ts": 1})), [])
        self.assertEqual(kinds(p.on_line(self.marker("test", f="a.ts", o="pass")), "unit"), [])
        self.assertEqual(kinds(p.on_line(self.marker("test", f="b.ts", o="pass")), "unit"), [("unit", "pw", 2, "pass")])

    def test_a_file_with_no_tests_in_begin_gets_no_cell(self):
        events = self.file_cells({"a.ts": 1, "b.ts": 0}, [("a.ts", "pass")])
        self.assertEqual(events, [("units", "pw", 1, "files"), ("unit", "pw", 1, "pass")])

    def test_a_clean_run_ends_the_cells_of_files_that_never_reported_as_skips(self):
        p = self.parser()
        for line in (self.marker("hello", v=1), self.marker("begin", f={"a.ts": 1, "b.ts": 2, "c.ts": 1}),
                     self.marker("test", f="a.ts", o="pass"), self.marker("test", f="b.ts", o="pass")):
            p.on_line(line)
        events = p.finish(0)
        self.assertEqual(kinds(events, "unit"), [("unit", "pw", 2, "skip"), ("unit", "pw", 3, "skip")])
        self.assertEqual(kinds(events, "unit_end"), [("unit_end", "b.ts", "ok", "file")])

    def test_a_failed_run_leaves_the_cells_of_files_that_never_reported_pending(self):
        p = self.parser()
        for line in (self.marker("hello", v=1), self.marker("begin", f={"a.ts": 1, "c.ts": 1}),
                     self.marker("test", f="a.ts", o="pass")):
            p.on_line(line)
        self.assertEqual(kinds(p.finish(1), "unit"), [])

    def test_bar_tests_adds_no_cells_at_the_end_of_a_clean_run(self):
        p = self.parser()
        p.bar = "tests"
        for line in (self.marker("hello", v=1), self.marker("begin", f={"a.ts": 1, "c.ts": 1}),
                     self.marker("test", f="a.ts", o="pass")):
            p.on_line(line)
        self.assertEqual(kinds(p.finish(0), "unit"), [])

    def test_hello_without_a_usable_begin_counts_one_cell_per_test(self):
        for begin in ([], {}, {"a.ts": 0}, None):
            with self.subTest(begin=begin):
                p = self.parser()
                lines = [self.marker("hello", v=1)] + ([] if begin is None else [self.marker("begin", f=begin)])
                self.assertEqual([e for line in lines for e in p.on_line(line)], [])
                self.assertEqual(kinds(p.on_line(self.marker("test", f="a.ts", o="pass")), "unit"),
                                 [("unit", "pw", 1, "pass")])
                self.assertEqual(kinds(p.on_line(self.marker("test", f="b.ts", o="fail")), "unit"),
                                 [("unit", "pw", 2, "fail")])
                self.assertEqual(kinds(p.finish(0), "unit"), [])

    def test_an_interrupted_run_closes_its_open_files_and_adds_no_cell_for_them(self):
        p = self.parser()
        for line in (self.marker("hello", v=1), self.marker("begin", f={"a.ts": 1, "b.ts": 3}),
                     self.marker("test", f="a.ts", o="pass"), self.marker("test", f="b.ts", o="pass")):
            p.on_line(line)
        events = p.finish(1)
        self.assertEqual((kinds(events, "unit"), kinds(events, "unit_end")), ([], [("unit_end", "b.ts", "ok", "file")]))

    def test_a_test_line_after_the_summary_moves_the_bar_but_leaves_the_summary_counts(self):
        # the reporter's onEnd sends a test whose retry never ran after Playwright printed its summary
        p = self.parser()
        for line in (self.marker("hello", v=1), self.marker("begin", f={"a.ts": 1, "b.ts": 1}),
                     self.marker("test", f="b.ts", o="fail"), "  1 failed", "  1 flaky"):
            p.on_line(line)
        events = p.on_line(self.marker("test", f="a.ts", o="pass"))
        self.assertEqual((kinds(events, "unit"), kinds(events, "tests")), ([("unit", "pw", 2, "pass")], []))

    def test_a_retried_test_counts_once(self):
        p = self.parser()
        lines = [self.marker("hello", v=1), self.marker("begin", f={"a.ts": 1}), self.marker("start", f="a.ts"),
                 "[1/2] a.ts:3:1 › flaky", self.marker("start", f="a.ts"), "[2/2] a.ts:3:1 › flaky (retry #1)",
                 self.marker("test", f="a.ts", o="pass")]
        events = [e for line in lines for e in p.on_line(line)]
        self.assertEqual(kinds(events, "unit"), [("unit", "pw", 1, "pass")])
        self.assertEqual(kinds(events, "units"), [("units", "pw", 1, "files")])

    def test_a_test_with_no_file_still_counts(self):
        p = self.parser()
        for line in (self.marker("hello", v=1), self.marker("begin", f={"": 1})):
            p.on_line(line)
        # its file "" gets a cell like any other, and no line
        self.assertEqual(p.on_line(self.marker("test", f="", o="pass")), [("unit", "pw", 1, "pass"), ("tests", "pw", 1, 0, 0, 0)])

    def test_a_run_whose_reporter_never_said_hello_says_so_once(self):
        with mock.patch.dict(os.environ, {"RUN_MARKERS": "1"}):
            os.environ.pop("PW_TEST_REPORTER", None)
            os.environ.pop("JUNEAU_RUN_PLAYWRIGHT_REPORTER", None)
            for exit_code, text in ((0, "  2 passed"), (1, "  1 failed"), (1, "[1/2] a.ts:3:1 › passes")):
                p = self.parser()
                with mock.patch.object(jr, "find_playwright_reporter", return_value="/r/x.cjs"):
                    p.prepare(["playwright", "test"], {}, None)
                if text:
                    p.on_line(text)
                self.assertEqual(kinds(p.finish(exit_code), "note"),
                                 [("note", "warn", "Playwright reporter not loaded: no per-file lines", "pw")])

    def test_no_note_without_the_reporter_or_when_nothing_ran(self):
        p = self.parser()                                   # not injected
        self.assertEqual(kinds(p.finish(0), "note"), [])
        with mock.patch.dict(os.environ, {"RUN_MARKERS": "1"}):
            os.environ.pop("PW_TEST_REPORTER", None)
            os.environ.pop("JUNEAU_RUN_PLAYWRIGHT_REPORTER", None)
            p = self.parser()
            with mock.patch.object(jr, "find_playwright_reporter", return_value="/r/x.cjs"):
                p.prepare(["playwright", "test"], {}, None)
            self.assertEqual(kinds(p.finish(1), "note"), [])  # a config error: Playwright stopped before any test
            p = self.parser()
            with mock.patch.object(jr, "find_playwright_reporter", return_value="/r/x.cjs"):
                p.prepare(["playwright", "test", "--list"], {}, None)
            p.on_line("Total: 3 tests in 1 file")
            self.assertEqual(kinds(p.finish(0), "note"), [])  # --list, --help: no test ran, so no summary
            p = self.parser()
            with mock.patch.object(jr, "find_playwright_reporter", return_value="/r/x.cjs"):
                p.prepare(["playwright", "test"], {}, None)
            p.on_line(self.marker("hello", v=1))
            self.assertEqual(kinds(p.finish(0), "note"), [])

    def test_a_marker_after_test_output_with_no_newline_is_found(self):
        p = self.parser()
        p.on_line(self.marker("begin", f={"a.spec.ts": 3}))
        events = p.on_line("[1/3] a.spec.ts:3:1 › passes" + self.marker("start", f="a.spec.ts"))
        self.assertEqual(events, [("units", "pw", 3, "tests"), ("unit", "pw", 1, None), ("tests", "pw", 1, 0, 0, 0),
                                  ("unit_start", "a.spec.ts", "a.spec.ts", "file")])
        self.assertEqual(p.on_line("console.log text" + self.marker("test", f="a.spec.ts", o="pass")),
                         [("unit_dot", "a.spec.ts", "pass")])

    def test_a_test_line_before_its_start_opens_the_file(self):
        p = self.parser()
        p.on_line(self.marker("begin", f={"a.ts": 2}))
        self.assertEqual(p.on_line(self.marker("test", f="a.ts", o="pass")),
                         [("unit_start", "a.ts", "a.ts", "file"), ("unit_dot", "a.ts", "pass")])

    def test_a_dot_on_a_closed_file_is_passed_on_without_reopening_it(self):
        p = self.parser()
        for line in (self.marker("begin", f={"a.ts": 1}), self.marker("test", f="a.ts", o="pass")):
            p.on_line(line)
        self.assertEqual(p.on_line(self.marker("start", f="a.ts")), [])
        self.assertEqual(p.on_line(self.marker("test", f="a.ts", o="fail")), [("unit_dot", "a.ts", "fail")])
        self.assertEqual(p.finish(1), [])

    def test_a_file_missing_from_begin_closes_only_at_finish(self):
        p = self.parser()
        for line in (self.marker("begin", f={"a.ts": 1}), self.marker("start", f="b.ts"),
                     self.marker("test", f="b.ts", o="pass"), self.marker("test", f="b.ts", o="pass")):
            self.assertNotIn("unit_end", [e[0] for e in p.on_line(line)], line)
        self.assertEqual(p.finish(0), [("unit_end", "b.ts", "ok", "file")])

    def test_marker_lines_stay_out_of_the_condensed_log(self):
        text = PLAYWRIGHT_FAIL.replace("    Error: expect(received).toBe(expected)",
                                       "    Error: expect(received).toBe(expected)\n" + self.marker("start", f="x.ts"))
        cond = [e[1] for e in kinds(feed(self.parser(), text, 1), "cond")]
        self.assertFalse(any("##playwright" in c for c in cond))

    def test_finish_closes_open_files_fail_only_with_a_fail_dot_whatever_the_exit_code(self):
        for exit_code in (0, 1):
            p = self.parser()
            for line in (self.marker("begin", f={"a.ts": 5, "b.ts": 5}), self.marker("test", f="a.ts", o="fail"),
                         self.marker("test", f="b.ts", o="pass")):
                p.on_line(line)
            self.assertEqual(p.finish(exit_code), [("unit_end", "a.ts", "fail", "file"), ("unit_end", "b.ts", "ok", "file")])

    def test_an_interrupted_run_names_only_the_file_with_a_failure(self):
        p = self.parser()
        lines = [self.marker("hello", v=1), self.marker("begin", f={f"{c}.ts": 3 for c in "abcd"})]
        lines += [self.marker("start", f=f"{c}.ts") for c in "abcd"]
        lines += [self.marker("test", f="a.ts", o="fail")] + [self.marker("test", f=f"{c}.ts", o="skip") for c in "bcd"]
        for line in lines:
            p.on_line(line)
        self.assertEqual(p.finish(1), [("unit_end", f"{c}.ts", "fail" if c == "a" else "ok", "file") for c in "abcd"])

    def test_bad_markers_are_ignored(self):
        p = self.parser()
        for line in ("##playwright {", "##playwright []", self.marker("start"), self.marker("test", f="a.ts", o="odd"),
                     self.marker("begin", f="a.ts")):
            self.assertEqual(p.on_line(line), [], line)

    def test_report_is_emitted_when_the_json_file_exists(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "r.json"
            path.write_text("{}", encoding="utf-8")
            p = self.parser()
            p.report_path = str(path)
            self.assertEqual(p.finish(0), [("report", "pw", "playwright-json", str(path))])


class GenericParserTest(unittest.TestCase):
    def test_tail_is_emitted_only_on_failure_and_is_limited_to_forty_lines(self):
        text = "\n".join(f"line {i}" for i in range(100))
        p = jr.GenericParser()
        p.step_id = "g"
        failed = feed(p, text, 1)
        self.assertEqual([e[1] for e in kinds(failed, "cond")], [f"line {i}" for i in range(60, 100)])
        self.assertEqual(kinds(failed, "failure"), [("failure", "\n".join(f"line {i}" for i in range(60, 100)), "g")])
        self.assertEqual(feed(jr.GenericParser(), text, 0), [])

    def test_registry_names(self):
        self.assertEqual(sorted(jr.PARSERS), ["generic", "gradle", "maven", "npm", "playwright", "pytest"])


def gradle(text, exit_code=0, args=("build",)):
    parser = jr.GradleParser()
    parser.step_id = "g"
    cmd, _ = parser.prepare(["gradle", *args], None, "/nonexistent")
    return parser, cmd, feed(parser, text, exit_code)


class GradleConsoleOptionTest(unittest.TestCase):
    """--console=plain goes right after the program that is run, behind any launcher."""

    def prepared(self, argv):
        parser = jr.GradleParser()
        parser.step_id = "g"
        return parser.prepare(list(argv), None, "/nonexistent")[0]

    def test_plain_gradle_gets_the_option_after_the_program(self):
        self.assertEqual(self.prepared(["gradle", "build"]), ["gradle", "--console=plain", "build"])

    def test_the_option_follows_env_assignments(self):
        self.assertEqual(self.prepared(["env", "JAVA_HOME=/x", "gradle", "--version"]),
                         ["env", "JAVA_HOME=/x", "gradle", "--console=plain", "--version"])

    def test_the_option_follows_env_options(self):
        self.assertEqual(self.prepared(["env", "-u", "X", "-i", "A=1", "./gradlew", "build"]),
                         ["env", "-u", "X", "-i", "A=1", "./gradlew", "--console=plain", "build"])

    def test_a_console_option_already_given_is_left_alone(self):
        for argv in (["gradle", "--console=rich", "build"], ["env", "A=1", "gradle", "--console", "verbose", "build"]):
            with self.subTest(argv=argv):
                self.assertEqual(self.prepared(argv), argv)

    def test_a_console_looking_value_in_an_env_assignment_does_not_count(self):
        self.assertEqual(self.prepared(["env", "C=--console=rich", "gradle", "build"]),
                         ["env", "C=--console=rich", "gradle", "--console=plain", "build"])

    def test_the_maven_options_are_read_from_the_wrapped_programs_arguments(self):
        parser = jr.MavenParser()
        parser.step_id = "m"
        parser.prepare(["env", "A=-T4", "mvn", "test"], None, "/nonexistent")
        self.assertFalse(parser.parallel)
        parser.prepare(["env", "A=1", "mvn", "test", "-T", "2"], None, "/nonexistent")
        self.assertTrue(parser.parallel)


class GradleStructureTest(unittest.TestCase):
    """GradleParser on inline lines, so the test run never needs Gradle."""

    LINES = "\n".join([
        "> Task :lib:compileJava",
        "> Task :lib:processResources NO-SOURCE",
        "> Task :lib:test",
        "> Task :app:compileJava UP-TO-DATE",
        "> Task :app:test",
        "AppTest > two() FAILED",
        "    org.opentest4j.AssertionFailedError at AppTest.java:5",
        "2 tests completed, 1 failed",
        "> Task :app:test FAILED",
        "",
        "FAILURE: Build failed with an exception.",
        "",
        "* What went wrong:",
        "Execution failed for task ':app:test'.",
        "> There were failing tests. See the report at: file:///scratch/app/build/reports/tests/test/index.html",
        "",
        "* Try:",
        "> Run with --scan to get full insights.",
        "",
        "BUILD FAILED in 3s",
    ])

    def test_prepare_adds_plain_console_once(self):
        _, cmd, _ = gradle("", args=("build",))
        self.assertEqual(cmd, ["gradle", "--console=plain", "build"])
        _, cmd, _ = gradle("", args=("--console=rich", "build"))
        self.assertEqual(cmd, ["gradle", "--console=rich", "build"])

    def test_sub_steps_one_per_task(self):
        _, _, events = gradle(self.LINES, 1)
        self.assertEqual([e[1:] for e in kinds(events, "sub_start")],
                         [("g/lib-compilejava", 1, ":lib:compileJava"),
                          ("g/lib-processresources", 2, ":lib:processResources"),
                          ("g/lib-test", 3, ":lib:test"),
                          ("g/app-compilejava", 4, ":app:compileJava"),
                          ("g/app-test", 5, ":app:test")])
        self.assertEqual([(e[1], e[2]) for e in kinds(events, "sub_end")],
                         [("g/lib-compilejava", "ok"), ("g/lib-processresources", "skip"), ("g/lib-test", "ok"),
                          ("g/app-compilejava", "ok"), ("g/app-test", "fail")])

    def test_counts(self):
        _, _, events = gradle(self.LINES, 1)
        self.assertEqual(kinds(events, "tests"), [("tests", "g", 2, 1, 0, 0)])
        _, _, events = gradle("3 tests completed, 1 failed, 1 skipped\n", 1)
        self.assertEqual(kinds(events, "tests"), [("tests", "g", 3, 1, 0, 1)])
        _, _, events = gradle("1 test completed, 1 failed\n", 1)
        self.assertEqual(kinds(events, "tests"), [("tests", "g", 1, 1, 0, 0)])

    def test_what_went_wrong_block_is_the_failure(self):
        _, _, events = gradle(self.LINES, 1)
        failure = kinds(events, "failure")
        self.assertEqual(len(failure), 1)
        self.assertEqual(failure[0][2], "g")
        self.assertTrue(failure[0][1].startswith("* What went wrong:\nExecution failed for task ':app:test'."))
        self.assertNotIn("* Try:", failure[0][1])
        cond = [e[1] for e in kinds(events, "cond")]
        self.assertIn("Execution failed for task ':app:test'.", cond)
        self.assertIn("BUILD FAILED in 3s", cond)

    def test_success_has_no_failure_and_closes_open_tasks(self):
        _, _, events = gradle("> Task :a\n> Task :b\nBUILD SUCCESSFUL in 1s\n", 0)
        self.assertEqual(kinds(events, "failure"), [])
        self.assertEqual([(e[1], e[2]) for e in kinds(events, "sub_end")], [("g/a", "ok"), ("g/b", "ok")])

    def test_failed_exit_with_no_block_falls_back_to_the_tail(self):
        _, _, events = gradle("> Task :a\nsomething broke\n", 1)
        self.assertEqual([(e[1], e[2]) for e in kinds(events, "sub_end")], [("g/a", "fail")])
        self.assertEqual(kinds(events, "failure"), [("failure", "> Task :a\nsomething broke", "g")])


class NpmParserTest(unittest.TestCase):
    def npm(self, text, exit_code=0):
        parser = jr.NpmParser()
        parser.step_id = "n"
        return feed(parser, text, exit_code)

    def test_no_sub_steps(self):
        events = self.npm("added 3 packages in 1s\n")
        self.assertEqual(kinds(events, "sub_start"), [])

    def test_summary_line_from_added(self):
        events = self.npm("\nadded 3 packages, and audited 4 packages in 1s\nfound 0 vulnerabilities\n")
        self.assertEqual(kinds(events, "cond"), [("cond", "added 3 packages, and audited 4 packages in 1s")])
        self.assertEqual(kinds(events, "failure"), [])

    def test_error_lines_are_the_failure(self):
        text = "> demo@1.0.0 build\nnoise\nnpm error code 1\nnpm ERR! old style\nmore noise\n"
        events = self.npm(text, 1)
        self.assertEqual(kinds(events, "cond"), [("cond", "npm error code 1"), ("cond", "npm ERR! old style")])
        self.assertEqual(kinds(events, "failure"), [("failure", "npm error code 1\nnpm ERR! old style", "n")])

    def test_failure_with_no_error_lines_is_the_tail(self):
        text = "\n".join(f"line {i}" for i in range(100))
        events = self.npm(text, 1)
        self.assertEqual(kinds(events, "failure"), [("failure", "\n".join(f"line {i}" for i in range(60, 100)), "n")])

    def test_success_ignores_stray_error_lines(self):
        self.assertEqual(kinds(self.npm("npm error not really\n", 0), "failure"), [])


class NpmFixtureTest(unittest.TestCase):
    def npm(self, fixture, exit_code):
        parser = jr.NpmParser()
        parser.step_id = "n"
        return feed(parser, (FIXTURES / fixture).read_text(encoding="utf-8"), exit_code)

    def test_install_success(self):
        events = self.npm("npm-install-success.log", 0)
        self.assertRegex(kinds(events, "cond")[0][1], r"^added 1 package\b")
        self.assertEqual(kinds(events, "failure"), [])

    def test_missing_script(self):
        events = self.npm("npm-missing-script.log", 1)
        self.assertTrue(kinds(events, "failure")[0][1].startswith('npm error Missing script: "no-such-script"'))


class GradleFixtureTest(unittest.TestCase):
    def fixture(self, name, exit_code):
        return gradle((FIXTURES / name).read_text(encoding="utf-8"), exit_code)[2]

    def test_success(self):
        events = self.fixture("gradle-success.log", 0)
        self.assertIn(("sub_start", "g/lib-compilejava", 1, ":lib:compileJava"), events)
        self.assertEqual({e[2] for e in kinds(events, "sub_end")} - {"ok", "skip"}, set())
        self.assertEqual(kinds(events, "failure"), [])

    def test_test_failure(self):
        events = self.fixture("gradle-test-failure.log", 1)
        self.assertIn(("tests", "g", 2, 1, 0, 0), events)
        self.assertIn(("g/app-test", "fail"), [(e[1], e[2]) for e in kinds(events, "sub_end")])
        self.assertTrue(kinds(events, "failure")[0][1].startswith("* What went wrong:"))

    def test_compile_failure(self):
        events = self.fixture("gradle-compile-failure.log", 1)
        self.assertIn(("g/lib-compilejava", "fail"), [(e[1], e[2]) for e in kinds(events, "sub_end")])
        self.assertIn("Compilation failed", kinds(events, "failure")[0][1])


class ReactorBuildOrderTest(unittest.TestCase):
    # titles as read_pom_tree sees them from write_demo_tree(): "Demo Parent" (pom) and "Demo Module A".."C"
    ORDER = ["[INFO] Reactor Build Order:", "[INFO] ",
             "[INFO] Demo Parent                                                   [pom]",
             "[INFO] Demo Module A                                                 [jar]",
             "[INFO] Demo Module B                                                 [jar]",
             "[INFO] Demo Module C                                                 [jar]",
             "[INFO] ", "[INFO] -----< org.demo:mod-a >-----",
             "[INFO] Building Demo Module A 1.0 [2/4]"]

    def reactor(self, lines, **tree):
        with tempfile.TemporaryDirectory() as tmp:
            write_demo_tree(Path(tmp), **tree)
            parser = jr.MavenParser()
            parser.step_id = "mvn"
            parser.prepare(["mvn", "test"], None, tmp)
            return [e for e in feed(parser, "\n".join(lines)) if e[0] == "reactor"]

    def test_size_and_poms_come_from_the_build_order(self):
        last = self.reactor(self.ORDER, packaging="pom")[-1]
        self.assertEqual(last[1], 4)
        self.assertEqual(last[2], ["mvn/demo-parent"])          # the one pom-packaged member, as a sub id

    def test_poms_are_empty_until_the_block_is_seen(self):
        first = self.reactor(self.ORDER, packaging="pom")[0]    # the first-line event precedes the block
        self.assertEqual(first[2], [])

    def test_pl_build_does_not_use_the_whole_tree(self):
        order = ["[INFO] Reactor Build Order:", "[INFO] ", "[INFO] Demo Module A  [jar]", "[INFO] ",
                 "[INFO] Building Demo Module A 1.0 [1/1]"]
        last = self.reactor(order, packaging="pom")[-1]          # the tree still has a pom parent
        self.assertEqual((last[1], last[2]), (1, []))

    def test_no_build_order_means_no_poms(self):
        last = self.reactor(["[INFO] Building Demo Module A 1.0 [1/9]"], packaging="pom")[-1]
        self.assertEqual(last[2], [])

    def test_unmappable_pom_title_stays_in_the_bars(self):
        order = ["[INFO] Reactor Build Order:", "[INFO] ", "[INFO] Unknown Aggregator  [pom]",
                 "[INFO] Demo Module A  [jar]", "[INFO] ", "[INFO] Building Demo Module A 1.0 [2/2]"]
        last = self.reactor(order, packaging="pom")[-1]
        self.assertEqual((last[1], last[2]), (2, []))


    def test_duplicate_pom_titles_give_one_id(self):
        order = ["[INFO] Reactor Build Order:", "[INFO] ", "[INFO] Demo Parent  [pom]", "[INFO] Demo Parent  [pom]",
                 "[INFO] Demo Module A  [jar]", "[INFO] ", "[INFO] Building Demo Module A 1.0 [3/3]"]
        last = self.reactor(order, packaging="pom")[-1]
        self.assertEqual(last[2], ["mvn/demo-parent"])


    def test_a_trailing_pom_is_not_a_setup_pom(self):
        order = ["[INFO] Reactor Build Order:", "[INFO] ", "[INFO] Demo Module A  [jar]", "[INFO] Demo Module B  [war]",
                 "[INFO] Demo Parent  [pom]", "[INFO] ", "[INFO] Building Demo Module A 1.0 [1/3]"]
        last = self.reactor(order, packaging="pom")[-1]
        self.assertEqual(last, ("reactor", 3, [], True, ["mvn/demo-parent"]))      # it rides in the optional fifth field

    def test_only_the_poms_before_the_first_real_module_are_setup_poms(self):
        order = ["[INFO] Reactor Build Order:", "[INFO] ", "[INFO] Demo Parent  [pom]", "[INFO] Demo Module A  [jar]",
                 "[INFO] Demo Module B  [pom]", "[INFO] ", "[INFO] Building Demo Parent 1.0 [1/3]"]
        last = self.reactor(order, packaging="pom")[-1]
        self.assertEqual(last[2], ["mvn/demo-parent"])
        self.assertEqual(last[4], ["mvn/mod-b"])

    def test_the_reactor_event_keeps_four_fields_without_a_trailing_pom(self):
        last = self.reactor(self.ORDER, packaging="pom")[-1]
        self.assertEqual(len(last), 4)


class ParserContractDefaultsTest(unittest.TestCase):
    def test_new_members_are_inert(self):
        p = jr.Parser()
        self.assertIs(p.interactive, False)
        self.assertIs(p.rewrites_log, False)
        self.assertEqual(p.poll(), [])
        self.assertIsNone(p.close())
        self.assertEqual(p.render('{"a": 1}'), '{"a": 1}')

    def test_builtin_parsers_inherit_inert_members(self):
        for name, cls in jr.PARSERS.items():
            with self.subTest(parser=name):
                p = cls()
                self.assertIs(p.rewrites_log, False)
                self.assertEqual(p.poll(), [])
                self.assertEqual(p.render("x"), "x")


if __name__ == "__main__":
    unittest.main()
