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
"""Markers: wire format, golden conformance (v1 plus the additive extensions) and the nesting guard."""

import contextlib
import inspect
import io
import json
import os
import re
import unittest
from unittest import mock

from support import DOCS, jr, reset_run_state


def parse(out):
    """Every ##run line in captured stdout as a dict, ms normalized to 0."""
    found = []
    for line in out.splitlines():
        if line.startswith(jr.PREFIX):
            obj = json.loads(line[len(jr.PREFIX):])
            if "ms" in obj:
                obj["ms"] = 0
            found.append(obj)
    return found


def capture(fn):
    buf = io.StringIO()
    with contextlib.redirect_stdout(buf):
        fn()
    return buf.getvalue()


def golden_sections(path):
    """{scenario name: [marker dicts]} from a golden file; a file with no '# scenario:' lines is one scenario ''."""
    sections, current = {"": []}, ""
    for line in path.read_text(encoding="utf-8").splitlines():
        if line.startswith("# scenario:"):
            current = line.split(":", 1)[1].strip()
            sections[current] = []
        elif line.startswith(jr.PREFIX):
            sections[current].append(json.loads(line[len(jr.PREFIX):]))
    return sections


def golden_transcript(path, name):
    """The whole stdout of one scenario: '##run ' lines as-is, '| ' lines as raw output with a literal \\r meaning CR."""
    out, current = [], None
    for line in path.read_text(encoding="utf-8").splitlines():
        if line.startswith("# scenario:"):
            current = line.split(":", 1)[1].strip()
        elif current == name and line.startswith(jr.PREFIX):
            out.append(line)
        elif current == name and line.startswith("| "):
            out.append(line[2:].replace("\\r", "\r"))
    return "".join(l + "\n" for l in out)


def normalized(out):
    return re.sub(r'"ms":\d+', '"ms":0', out)


class MarkersTest(unittest.TestCase):
    def setUp(self):
        reset_run_state()
        patcher = mock.patch.dict(os.environ, {"RUN_MARKERS": "1"})
        patcher.start()
        self.addCleanup(patcher.stop)
        self.addCleanup(reset_run_state)

    def test_disabled_by_default_emits_nothing(self):
        with mock.patch.dict(os.environ):
            os.environ.pop("RUN_MARKERS")
            self.assertEqual(capture(lambda: jr.emit("note", level="info", text="x")), "")

    def test_only_exact_value_1_enables(self):
        with mock.patch.dict(os.environ, {"RUN_MARKERS": "true"}):
            self.assertEqual(capture(lambda: jr.emit("note", level="info", text="x")), "")

    def test_emit_is_one_compact_line_and_drops_none(self):
        out = capture(lambda: jr.emit("note", level="info", text="héllo", href=None))
        self.assertEqual(out, '##run {"ev":"note","level":"info","text":"héllo"}\n')

    def test_oversized_marker_is_dropped(self):
        self.assertEqual(capture(lambda: jr.emit("note", level="info", text="x" * 70000)), "")

    def test_newline_in_text_stays_on_one_line(self):
        out = capture(lambda: jr.emit("note", level="info", text="a\nb"))
        self.assertEqual(out.count("\n"), 1)
        self.assertEqual(parse(out), [{"ev": "note", "level": "info", "text": "a\nb"}])

    def test_step_exception_ends_fail_and_reraises(self):
        def body():
            with self.assertRaises(RuntimeError):
                with jr.step("commit", 4, "Commit"):
                    raise RuntimeError("boom")
        self.assertEqual(parse(capture(body))[-1], {"ev": "end", "id": "commit", "status": "fail", "ms": 0})

    def test_step_works_when_disabled(self):
        with mock.patch.dict(os.environ):
            os.environ.pop("RUN_MARKERS")
            def body():
                with jr.step("a", 1, "A") as s:
                    s.fail(exit=1)
                self.assertEqual(s.status, "fail")
            self.assertEqual(capture(body), "")

    def test_note_rejects_non_http_href(self):
        out = capture(lambda: jr.note("info", "x", href="javascript:alert(1)"))
        self.assertEqual(parse(out), [{"ev": "note", "level": "info", "text": "x"}])

    def test_parent_is_keyword_only(self):
        self.assertEqual(inspect.signature(jr.step).parameters["parent"].kind, inspect.Parameter.KEYWORD_ONLY)
        with self.assertRaises(TypeError):
            with jr.step("a", 1, "A", "p"):
                pass

    def test_artifacts_dir(self):
        with mock.patch.dict(os.environ):
            os.environ.pop("RUN_ARTIFACTS", None)
            self.assertIsNone(jr.artifacts_dir())
            os.environ["RUN_ARTIFACTS"] = "/tmp/a"
            self.assertEqual(str(jr.artifacts_dir()), "/tmp/a")

    # ---- golden conformance -------------------------------------------------------------------------------------

    def test_v1_golden_transcript(self):
        def scenario():
            jr.run(mode="push", project="demo", branch="main", head="abc1234")
            with jr.step("build", 1, "Build"):
                pass
            with jr.step("tests", 2, "Tests") as s:
                jr.report("tests", "surefire", "target/surefire-reports")
                s.fail(exit=1)
            with jr.step("push", 3, "Push") as s:
                s.skip()
            jr.note("warn", "Tests failed; nothing pushed", step="tests")
            jr.note("info", "Docs", href="https://example.org/runs")
            jr.done("fail")
        expected = golden_sections(DOCS / "run-protocol-v1-golden.txt")[""]
        self.assertEqual(parse(capture(scenario)), expected)

    def test_extension_golden_nested_steps_and_tests(self):
        def scenario():
            jr.run(mode="test", project="demo", branch="main", head="abc1234")
            with jr.step("mvn", 1, "Maven"):
                with jr.step("mvn/mod-a", 2, "Module A", parent="mvn"):
                    jr.tests("mvn/mod-a", 2, 0, 0, 0)
                    jr.tests("mvn/mod-a", 5, 1, 0, 1)
            jr.done("ok")
        self.assertEqual(parse(capture(scenario)), self.extension("nested-steps-and-tests"))

    def test_extension_golden_module_failure(self):
        def scenario():
            jr.run(mode="test", project="demo", branch="main", head="abc1234")
            with jr.step("mvn", 1, "Maven") as top:
                with jr.step("mvn/mod-b", 3, "Module B", parent="mvn") as child:
                    jr.note("error", "Module failed: Module B", step="mvn")
                    child.fail()
                top.fail(exit=1)
            jr.done("fail")
        self.assertEqual(parse(capture(scenario)), self.extension("module-failure"))

    def test_extension_golden_cancelled_children_closed_first(self):
        def scenario():
            jr.run(mode="test", project="demo", branch="main", head="abc1234")
            with jr.step("mvn", 1, "Maven") as top:
                with jr.step("mvn/mod-a", 2, "Module A", parent="mvn") as child:
                    child.fail()
                top.fail(exit=130)
            jr.done("cancelled")
        self.assertEqual(parse(capture(scenario)), self.extension("cancelled-children-closed-first"))

    def test_extension_golden_unknown_fields_are_ignorable(self):
        """Consumer rule: ignore unknown fields on a known ev.  The reference projection reads only known fields."""
        known = {"step": {"id", "n", "title", "parent"}, "end": {"id", "status", "ms", "exit"}}
        projected = [{k: v for k, v in m.items() if k == "ev" or k in known[m["ev"]]}
                     for m in self.extension("unknown-field-on-known-event")]
        self.assertEqual(projected, [
            {"ev": "step", "id": "x", "n": 1, "title": "X"},
            {"ev": "end", "id": "x", "status": "ok", "ms": 0},
        ])

    # ---- open lines ---------------------------------------------------------------------------------------------

    def test_marker_after_partial_output_starts_on_a_fresh_line(self):
        def scenario():
            jr.append("partial")
            jr.emit("note", level="info", text="x")
        self.assertEqual(capture(scenario), 'partial\n##run {"ev":"note","level":"info","text":"x"}\n')

    def test_marker_at_line_start_gets_no_extra_newline(self):
        def scenario():
            jr.open_line("a")
            jr.close_line()
            jr.emit("note", level="info", text="x")
        self.assertEqual(capture(scenario), 'a\n##run {"ev":"note","level":"info","text":"x"}\n')

    def test_disabled_marker_leaves_the_line_open(self):
        def scenario():
            jr.open_line("a")
            with mock.patch.dict(os.environ, {"RUN_MARKERS": "0"}):
                jr.emit("note", level="info", text="x")
            jr.dot()
        self.assertEqual(capture(scenario), "a.")

    def test_helpers(self):
        def scenario():
            jr.open_line("ORDERS: filling UID")
            jr.dot()
            jr.append("..")
            jr.close_line(" 1819 rows in 4s")
            jr.open_line("Performing task x: ")
            jr.set_tail("1 of 2 complete")
            jr.set_tail("2 of 2 complete")
            jr.close_line()
        self.assertEqual(capture(scenario),
                         "ORDERS: filling UID... 1819 rows in 4s\n"
                         "Performing task x: \rPerforming task x: 1 of 2 complete\rPerforming task x: 2 of 2 complete\n")

    def test_open_line_closes_an_open_line_first(self):
        def scenario():
            jr.open_line("a")
            jr.open_line("b")
            jr.close_line()
        self.assertEqual(capture(scenario), "a\nb\n")

    def test_set_tail_without_open_line_has_no_head(self):
        self.assertEqual(capture(lambda: jr.set_tail("x")), "\rx")

    def test_helpers_work_with_markers_off(self):
        with mock.patch.dict(os.environ):
            os.environ.pop("RUN_MARKERS")
            self.assertEqual(capture(lambda: (jr.open_line("a"), jr.dot(), jr.close_line())), "a.\n")

    def test_extension_golden_dots_then_marker(self):
        def scenario():
            with jr.step("load", 1, "Load"):
                jr.open_line("ORDERS: filling UID")
                jr.dot()
                jr.dot()
                jr.dot()
        path = DOCS / "run-protocol-v1-golden-extensions.txt"
        self.assertEqual(normalized(capture(scenario)), golden_transcript(path, "dots-then-marker"))

    def test_extension_golden_set_tail_counter_then_marker(self):
        def scenario():
            with jr.step("task", 1, "Task x"):
                jr.open_line("Performing task x: ")
                jr.set_tail("1 of 2 complete")
                jr.set_tail("2 of 2 complete")
        path = DOCS / "run-protocol-v1-golden-extensions.txt"
        self.assertEqual(normalized(capture(scenario)), golden_transcript(path, "set-tail-counter-then-marker"))

    def extension(self, name):
        return golden_sections(DOCS / "run-protocol-v1-golden-extensions.txt")[name]

    # ---- nesting guard ------------------------------------------------------------------------------------------

    def test_run_sets_the_active_flag_and_owner_still_emits_done(self):
        out = capture(lambda: (jr.run("test", "demo", "main", "abc"), jr.done("ok")))
        self.assertEqual(os.environ.get(jr.ACTIVE_ENV), "1")
        self.assertEqual([m["ev"] for m in parse(out)], ["run", "done"])

    def test_inherited_active_flag_makes_run_and_done_no_ops(self):
        os.environ[jr.ACTIVE_ENV] = "1"
        out = capture(lambda: (jr.run("test", "demo", "main", "abc"), jr.done("ok")))
        self.assertEqual(out, "")

    def test_inherited_active_flag_still_allows_steps(self):
        os.environ[jr.ACTIVE_ENV] = "1"
        def body():
            with jr.step("a", 1, "A"):
                pass
        self.assertEqual([m["ev"] for m in parse(capture(body))], ["step", "end"])


if __name__ == "__main__":
    unittest.main()
