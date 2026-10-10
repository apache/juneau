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

    def test_the_reactor_event_comes_from_the_tree_then_from_the_first_building_line(self):
        with tempfile.TemporaryDirectory() as tmp:
            self.demo_tree_with_tests(Path(tmp))
            _, events = maven("maven-serial-success.log", cwd=tmp)
        self.assertEqual(events[0], ("reactor", 4, ["mvn/demo-parent"], True))
        self.assertEqual(kinds(events, "reactor"), [("reactor", 4, ["mvn/demo-parent"], True)] * 2)
        _, no_tree = maven("maven-serial-success.log")
        self.assertEqual(kinds(no_tree, "reactor"), [("reactor", 4, [], True)])

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


def write_demo_tree(root):
    (root / "pom.xml").write_text(
        '<project xmlns="http://maven.apache.org/POM/4.0.0"><artifactId>demo-parent</artifactId><name>Demo Parent</name>'
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

    def test_prepare_adds_junitxml_only_when_markers_are_on_and_artifacts_exist(self):
        with mock.patch.dict(os.environ, {"RUN_MARKERS": "1", "RUN_ARTIFACTS": "/art"}):
            p = self.parser()
            cmd, _ = p.prepare(["pytest", "-q"], None, None)
            self.assertEqual(cmd, ["pytest", "-q", "--junitxml=/art/py-pytest.xml"])
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

    def test_existing_junitxml_is_kept_and_reported(self):
        p = self.parser()
        cmd, _ = p.prepare(["pytest", "--junitxml=out.xml"], None, None)
        self.assertEqual((cmd, p.report_path), (["pytest", "--junitxml=out.xml"], "out.xml"))


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

    def test_prepare_adds_reporters_and_json_path_only_when_markers_are_on(self):
        with mock.patch.dict(os.environ, {"RUN_MARKERS": "1", "RUN_ARTIFACTS": "/art"}):
            os.environ.pop("PLAYWRIGHT_JSON_OUTPUT_NAME", None)
            p = self.parser()
            cmd, env = p.prepare(["npx", "playwright", "test"], {"A": "b"}, None)
            self.assertEqual(cmd, ["npx", "playwright", "test", "--reporter=line,json"])
            self.assertEqual(env["PLAYWRIGHT_JSON_OUTPUT_NAME"], "/art/pw-playwright.json")
            self.assertEqual(env["A"], "b")
        with mock.patch.dict(os.environ, {"RUN_ARTIFACTS": "/art"}):
            os.environ.pop("RUN_MARKERS", None)
            os.environ.pop("PLAYWRIGHT_JSON_OUTPUT_NAME", None)
            cmd, env = self.parser().prepare(["npx", "playwright", "test"], {}, None)
            self.assertEqual(cmd, ["npx", "playwright", "test"])
            self.assertNotIn("PLAYWRIGHT_JSON_OUTPUT_NAME", env)

    def test_existing_reporter_is_respected(self):
        with mock.patch.dict(os.environ, {"RUN_MARKERS": "1", "RUN_ARTIFACTS": "/art"}):
            cmd, _ = self.parser().prepare(["playwright", "test", "--reporter=list"], {}, None)
        self.assertEqual(cmd, ["playwright", "test", "--reporter=list"])

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
        self.assertEqual(sorted(jr.PARSERS), ["generic", "maven", "playwright", "pytest"])


if __name__ == "__main__":
    unittest.main()
