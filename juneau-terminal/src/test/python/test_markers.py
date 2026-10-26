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
    """Every ##terminal line in captured stdout as a dict, ms normalized to 0."""
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
    """The whole stdout of one scenario: '##terminal ' lines as-is, '| ' lines as raw output with a literal \\r meaning CR."""
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
        patcher = mock.patch.dict(os.environ, {"TERMINAL_MARKERS": "1"})
        patcher.start()
        self.addCleanup(patcher.stop)
        self.addCleanup(reset_run_state)

    def test_disabled_by_default_emits_nothing(self):
        with mock.patch.dict(os.environ):
            os.environ.pop("TERMINAL_MARKERS")
            self.assertEqual(capture(lambda: jr.emit("note", level="info", text="x")), "")

    def test_only_exact_value_1_enables(self):
        with mock.patch.dict(os.environ, {"TERMINAL_MARKERS": "true"}):
            self.assertEqual(capture(lambda: jr.emit("note", level="info", text="x")), "")

    def test_emit_is_one_compact_line_and_drops_none(self):
        out = capture(lambda: jr.emit("note", level="info", text="héllo", href=None))
        self.assertEqual(out, '##terminal {"ev":"note","level":"info","text":"héllo"}\n')

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

    def test_unit_is_a_child_step_marker_and_one_bus_pair(self):
        seen = []
        jr.subscribe(lambda kind, fields: seen.append((kind, dict(fields))))
        def body():
            with jr.step("prechecks", 1, "Prechecks") as s:
                with s.unit("Whitespace check", "Whitespace check") as u:
                    u.dot("pass")
        self.assertEqual(parse(capture(body)), [
            {"ev": "step", "id": "prechecks", "n": 1, "title": "Prechecks"},
            {"ev": "step", "id": "prechecks/whitespace-check", "n": 1, "title": "Whitespace check",
             "parent": "prechecks"},
            {"ev": "end", "id": "prechecks/whitespace-check", "status": "ok", "ms": 0},
            {"ev": "end", "id": "prechecks", "status": "ok", "ms": 0}])
        units = [(k, f) for k, f in seen if k.startswith("unit_")]
        self.assertEqual([k for k, _ in units], ["unit_start", "unit_dot", "unit_end"])
        self.assertEqual(units[0][1], {"step": "prechecks", "id": "Whitespace check", "title": "Whitespace check",
                                       "src": "script"})
        self.assertEqual({k: v for k, v in units[2][1].items() if k != "ms"},
                         {"step": "prechecks", "id": "Whitespace check", "status": "ok", "src": "script"})
        self.assertEqual([k for k, _ in seen].count("step_start"), 1)     # not a sub-step

    def test_unit_fail_dot_and_fail_end_it_fail(self):
        def body():
            with jr.step("s", 1, "S") as s:
                with s.unit("a", "A") as u:
                    u.dot("fail")
                with s.unit("b", "B") as u:
                    u.fail()
        ends = [m for m in parse(capture(body)) if m["ev"] == "end"]
        self.assertEqual([(m["id"], m["status"]) for m in ends], [("s/a", "fail"), ("s/b", "fail"), ("s", "ok")])

    def test_unit_exception_ends_it_fail_and_reraises(self):
        def body():
            with self.assertRaises(RuntimeError):
                with jr.step("s", 1, "S") as s:
                    with s.unit("a", "A"):
                        raise RuntimeError("boom")
        ends = [m for m in parse(capture(body)) if m["ev"] == "end"]
        self.assertEqual([(m["id"], m["status"]) for m in ends], [("s/a", "fail"), ("s", "fail")])

    def test_unit_ids_stay_within_64_characters_and_a_cut_collision_gets_a_number(self):
        row = "r" * 40
        def body():
            with jr.step(row, 1, "R") as s:
                for i in range(3):
                    with s.unit("x" * 30 + str(i), "X"):
                        pass
        ids = [m["id"] for m in parse(capture(body)) if m["ev"] == "step"][1:]
        self.assertEqual(ids, [f"{row}/{'x' * 23}", f"{row}/{'x' * 21}-2", f"{row}/{'x' * 21}-3"])
        self.assertTrue(all(len(i) <= 64 for i in ids))

    def test_unit_ids_stay_within_64_characters_when_the_row_leaves_one_to_three(self):
        for size in (60, 61, 62):      # room 3, 2, 1
            with self.subTest(size=size):
                row = "r" * size
                def body():
                    with jr.step(row, 1, "R") as s:
                        for i in range(3):
                            with s.unit("abc" + str(i), "X"):
                                pass
                ids = [m["id"] for m in parse(capture(body)) if m["ev"] == "step"][1:]
                self.assertTrue(all(len(i) <= 64 for i in ids), ids)
                self.assertEqual(len(set(ids)), 3)

    def test_a_row_with_room_for_one_character_raises_once_its_ids_run_out(self):
        def body():
            with jr.step("r" * 62, 1, "R") as s:
                for i in range(9):
                    with s.unit(f"a{i}", "A"):
                        pass
                with self.assertRaises(ValueError):
                    with s.unit("a9", "A"):
                        pass
        ids = [m["id"] for m in parse(capture(body)) if m["ev"] == "step"][1:]
        self.assertEqual([i.split("/")[1] for i in ids], ["a", "2", "3", "4", "5", "6", "7", "8", "9"])

    def test_a_duplicate_open_unit_id_raises(self):
        def body():
            with jr.step("s", 1, "S") as s:
                with s.unit("a", "A"):
                    with self.assertRaises(ValueError):
                        with s.unit("a", "A again"):
                            pass
                with s.unit("a", "A again"):       # closed: it may open again, as a new child step
                    pass
        steps = [m["id"] for m in parse(capture(body)) if m["ev"] == "step"]
        self.assertEqual(steps, ["s", "s/a", "s/a-2"])

    def test_a_row_id_of_63_characters_leaves_no_room_for_a_unit(self):
        def body():
            with jr.step("r" * 63, 1, "R") as s:
                with self.assertRaises(ValueError):
                    with s.unit("a", "A"):
                        pass
        capture(body)

    def test_step_works_when_disabled(self):
        with mock.patch.dict(os.environ):
            os.environ.pop("TERMINAL_MARKERS")
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
            os.environ.pop("TERMINAL_ARTIFACTS", None)
            self.assertIsNone(jr.artifacts_dir())
            os.environ["TERMINAL_ARTIFACTS"] = "/tmp/a"
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
        expected = golden_sections(DOCS / "terminal-protocol-v1-golden.txt")[""]
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
        self.assertEqual(capture(scenario), 'partial\n##terminal {"ev":"note","level":"info","text":"x"}\n')

    def test_marker_at_line_start_gets_no_extra_newline(self):
        def scenario():
            jr.open_line("a")
            jr.close_line()
            jr.emit("note", level="info", text="x")
        self.assertEqual(capture(scenario), 'a\n##terminal {"ev":"note","level":"info","text":"x"}\n')

    def test_disabled_marker_leaves_the_line_open(self):
        def scenario():
            jr.open_line("a")
            with mock.patch.dict(os.environ, {"TERMINAL_MARKERS": "0"}):
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
            os.environ.pop("TERMINAL_MARKERS")
            self.assertEqual(capture(lambda: (jr.open_line("a"), jr.dot(), jr.close_line())), "a.\n")

    def test_extension_golden_dots_then_marker(self):
        def scenario():
            with jr.step("load", 1, "Load"):
                jr.open_line("ORDERS: filling UID")
                jr.dot()
                jr.dot()
                jr.dot()
        path = DOCS / "terminal-protocol-v1-golden-extensions.txt"
        self.assertEqual(normalized(capture(scenario)), golden_transcript(path, "dots-then-marker"))

    def test_extension_golden_set_tail_counter_then_marker(self):
        def scenario():
            with jr.step("task", 1, "Task x"):
                jr.open_line("Performing task x: ")
                jr.set_tail("1 of 2 complete")
                jr.set_tail("2 of 2 complete")
        path = DOCS / "terminal-protocol-v1-golden-extensions.txt"
        self.assertEqual(normalized(capture(scenario)), golden_transcript(path, "set-tail-counter-then-marker"))

    def extension(self, name):
        return golden_sections(DOCS / "terminal-protocol-v1-golden-extensions.txt")[name]

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
