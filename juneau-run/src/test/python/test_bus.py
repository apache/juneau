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
"""The in-process event bus: what each producer publishes, independently of markers."""

import contextlib
import io
import os
import unittest
from unittest import mock

from support import jr, reset_run_state


class Recorder:
    def __init__(self):
        self.events = []

    def __call__(self, kind, fields):
        self.events.append((kind, dict(fields)))

    def kinds(self):
        return [k for k, _ in self.events]

    def of(self, kind):
        return [f for k, f in self.events if k == kind]


class BusTest(unittest.TestCase):
    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        patcher = mock.patch.dict(os.environ, {})
        patcher.start()
        self.addCleanup(patcher.stop)
        for key in ("RUN_MARKERS", "JUNEAU_RUN_CONSOLE", "JUNEAU_RUN_FULL_LOG", "JUNEAU_RUN_CONDENSED_LOG"):
            os.environ.pop(key, None)
        self.rec = jr.subscribe(Recorder())
        self.out = io.StringIO()

    def quiet(self):
        return contextlib.redirect_stdout(self.out)

    def test_step_publishes_start_and_end_with_summary_even_with_markers_off(self):
        with self.quiet(), jr.step("bom", 2, "BOM completeness") as s:
            s.summary = "73 modules"
        self.assertEqual(self.rec.events[0],
                         ("step_start", {"id": "bom", "n": 2, "title": "BOM completeness", "parent": None}))
        end = self.rec.of("step_end")[0]
        self.assertEqual((end["id"], end["status"], end["summary"], end["exit"], end["totals"]),
                         ("bom", "ok", "73 modules", None, None))
        self.assertIsInstance(end["ms"], int)
        self.assertEqual(self.out.getvalue(), "")   # markers off: nothing printed

    def test_a_failing_step_publishes_fail(self):
        with self.quiet(), self.assertRaises(RuntimeError), jr.step("x", 1, "X"):
            raise RuntimeError("boom")
        self.assertEqual(self.rec.of("step_end")[0]["status"], "fail")

    def test_note_and_tests_publish(self):
        jr.note("warn", "careful", href="file:///x", step="bom")
        jr.note("info", "report", href="https://ci/x", step="bom")
        jr.tests("tests", 10, 1, 2, 3)
        self.assertEqual(self.rec.of("note"), [{"level": "warn", "text": "careful", "href": None, "step": "bom"},
                                               {"level": "info", "text": "report", "href": "https://ci/x",
                                                "step": "bom"}])
        self.assertEqual(self.rec.of("tests"), [{"step": "tests", "total": 10, "fail": 1, "err": 2, "skip": 3}])

    def test_done_publishes_even_when_nested(self):
        os.environ[jr.ACTIVE_ENV] = "1"
        jr.done("ok")
        self.assertEqual(self.rec.of("done"), [{"status": "ok", "commit": None}])

    def test_say_prints_without_a_console_and_always_publishes(self):
        with self.quiet():
            jr.say("Running: mvn test", level="info", step="tests")
        self.assertEqual(self.out.getvalue(), "Running: mvn test\n")
        self.assertEqual(self.rec.of("say"), [{"text": "Running: mvn test", "level": "info", "step": "tests"}])

    def test_say_never_emits_a_marker(self):
        os.environ["RUN_MARKERS"] = "1"
        with self.quiet():
            jr.say("hello")
        self.assertNotIn(jr.PREFIX, self.out.getvalue())

    def test_failure_prints_without_a_console_and_publishes(self):
        with self.quiet():
            jr.failure("❌ identity mismatch", step="identity")
        self.assertEqual(self.out.getvalue(), "❌ identity mismatch\n")
        self.assertEqual(self.rec.of("failure"), [{"text": "❌ identity mismatch", "step": "identity"}])

    def test_row_is_bus_only(self):
        os.environ["RUN_MARKERS"] = "1"
        with self.quiet(), jr.row("perf", "Perf") as r:
            r.summary = "13 over baseline"
        self.assertNotIn(jr.PREFIX, self.out.getvalue())
        self.assertEqual(self.rec.kinds(), ["step_start", "step_end"])
        self.assertEqual(self.rec.of("step_end")[0]["summary"], "13 over baseline")

    def test_unsubscribe(self):
        jr.unsubscribe(self.rec)
        jr.note("info", "x")
        self.assertEqual(self.rec.events, [])

    def test_run_tool_pass_through_publishes_start_and_end(self):
        with mock.patch.object(jr.subprocess, "run", return_value=mock.Mock(returncode=4)):
            jr.run_tool(["mvn", "test"], "generic", "tests", "Tests", n=3, cwd="/x")
        self.assertEqual(self.rec.of("step_start"),
                         [{"id": "tests", "n": 3, "title": "Tests", "parent": None}])
        end = self.rec.of("step_end")[0]
        self.assertEqual((end["status"], end["exit"]), ("fail", 4))

    def test_run_tool_piped_publishes_start_end_and_totals(self):
        with self.quiet():
            jr.run_tool([jr.sys.executable, "-c", "print('hi')"], "generic", "g", "G", capture=True)
        self.assertEqual(self.rec.kinds()[0], "step_start")
        self.assertEqual(self.rec.of("step_end")[0]["status"], "ok")


class DispatcherBusTest(unittest.TestCase):
    def setUp(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        os.environ.pop("RUN_MARKERS", None)
        self.rec = jr.subscribe(Recorder())
        self.d = jr._Dispatcher("tests", jr.Sinks(console="none"))

    def test_parser_tuples_become_bus_events(self):
        self.d.apply([
            ("sub_start", "tests/a", 1, "Mod A"),
            ("reactor", 3, ["tests/parent"], True),
            ("class_done", "tests/a", "pass"),
            ("compiled", "tests/a", True),
            ("tested", "tests/a"),
            ("tests", "tests/a", 5, 0, 0, 1),
            ("sub_end", "tests/a", "ok", 1200),
            ("failure", "boom", "tests"),
        ])
        self.assertEqual(self.rec.kinds(), [
            "module_start", "reactor", "class_done", "module_compiled", "module_tested", "tests", "module_end",
            "failure"])
        self.assertEqual(self.rec.of("module_start")[0], {"step": "tests", "module": "tests/a", "title": "Mod A"})
        self.assertEqual(self.rec.of("reactor")[0], {"step": "tests", "size": 3, "poms": ["tests/parent"], "tests": True})
        self.assertEqual(self.rec.of("class_done")[0], {"step": "tests", "module": "tests/a", "outcome": "pass"})
        self.assertEqual(self.rec.of("module_compiled")[0], {"step": "tests", "module": "tests/a", "ok": True})
        self.assertEqual(self.rec.of("module_end")[0],
                         {"step": "tests", "module": "tests/a", "status": "ok", "ms": 1200, "totals": (5, 0, 0, 1)})
        self.assertEqual(self.rec.of("failure")[0], {"text": "boom", "step": "tests"})

    def test_close_children_publishes_module_end_for_each_open_module(self):
        self.d.apply([("sub_start", "tests/a", 1, "Mod A"), ("sub_start", "tests/b", 2, "Mod B"),
                      ("tests", "tests/b", 3, 1, 0, 0)])
        self.d.close_children("fail")
        self.assertEqual([(f["module"], f["status"], f["totals"]) for f in self.rec.of("module_end")],
                         [("tests/b", "fail", (3, 1, 0, 0)), ("tests/a", "fail", None)])


if __name__ == "__main__":
    unittest.main()
