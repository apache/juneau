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
