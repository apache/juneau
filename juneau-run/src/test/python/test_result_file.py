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
"""JUNEAU_RUN_MARKERS_FILE and JUNEAU_RUN_RESULT: markers in a file beside the console view, and the result JSON."""

import io
import json
import os
import sys
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from unittest import mock

import scripts_support
from scripts_support import SCRIPT, isolate_run_env, jr, markers, python, reset_run_state

import bootstrap
from juneau_scripts import notify, run, testing

# A nested script: loads juneau_run.py by path, as an adopter does, and runs one step of its own (fail when argv[2] is
# "fail").  Its environment is the owner's, so it inherits JUNEAU_RUN_ACTIVE and the files.
CHILD = """
import importlib.util, sys
spec = importlib.util.spec_from_file_location("juneau_run", sys.argv[1])
jr = importlib.util.module_from_spec(spec)
sys.modules["juneau_run"] = jr
spec.loader.exec_module(jr)
jr.run("test", "demo", "main", "abc1234")
with jr.step("tests", 2, "Java tests", label="Tests") as s:
    jr.tests("tests", 12, 1 if sys.argv[2] == "fail" else 0, 0, 2)
    if sys.argv[2] == "fail":
        s.fail(exit=1)
jr.note("warn", "slow module")
jr.done("fail" if sys.argv[2] == "fail" else "ok")
"""


def file_markers(path):
    return markers(Path(path).read_text(encoding="utf-8"))


def replay(found):
    """The protocol's consumer rules over a marker list: (run, {step id: status}, done status)."""
    started, steps, done = None, {}, None
    for m in found:
        if m["ev"] == "run":
            started = m
        elif m["ev"] == "step":
            steps[m["id"]] = None
        elif m["ev"] == "end":
            steps[m["id"]] = m["status"]
        elif m["ev"] == "done":
            done = m["status"]
    status = done or "fail"
    return started, {k: v or ("fail" if status == "fail" else "skip") for k, v in steps.items()}, status


class FileCase(unittest.TestCase):
    def setUp(self):
        isolate_run_env(self)
        reset_run_state()
        self.addCleanup(reset_run_state)
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.tmp = Path(tmp.name)
        os.environ["JUNEAU_RUN_LOG_DIR"] = str(self.tmp / "logs")
        self.markers_file = self.tmp / "m"
        self.result_file = self.tmp / "r.json"

    def result(self):
        return json.loads(self.result_file.read_text(encoding="utf-8"))

    def child(self, outcome="ok"):
        done = python(["-c", CHILD, str(SCRIPT), outcome], env=dict(os.environ))
        self.assertEqual(done.returncode, 0, done.stderr)
        return done


class MarkersFileTest(FileCase):
    def setUp(self):
        super().setUp()
        os.environ[jr.MARKERS_FILE_ENV] = str(self.markers_file)

    def test_board_console_draws_and_the_file_gets_the_markers(self):
        out = io.StringIO()
        with redirect_stdout(out):
            jr.run("test", "demo", "main", "abc1234")
            jr.session("Demo", sinks=jr.Sinks("board", str(self.tmp / "full.log")))
            with jr.step("build", 1, "Compile", label="Compile"):
                jr.tests("build", 3, 0, 0, 0)
            jr.done("ok")
            jr.end_session()
        self.assertNotIn(jr.PREFIX, out.getvalue())
        self.assertIn("Compile", out.getvalue())     # the plain view's row
        self.assertEqual(file_markers(self.markers_file), [
            {"ev": "run", "v": 1, "mode": "test", "project": "demo", "branch": "main", "head": "abc1234"},
            {"ev": "step", "id": "build", "n": 1, "title": "Compile"},
            {"ev": "tests", "step": "build", "total": 3, "fail": 0, "err": 0, "skip": 0},
            {"ev": "end", "id": "build", "status": "ok"},
            {"ev": "done", "status": "ok"},
        ])

    def test_with_run_markers_stdout_and_the_file_get_the_same_lines(self):
        os.environ["RUN_MARKERS"] = "1"
        out = io.StringIO()
        with redirect_stdout(out):
            jr.run("push", "demo", "main", "abc1234")
            jr.note("info", "commit made")
            jr.done("ok", commit="abc1234")
        self.assertEqual(markers(out.getvalue()), file_markers(self.markers_file))
        self.assertEqual(len(file_markers(self.markers_file)), 3)

    def test_without_run_markers_nothing_reaches_stdout(self):
        out = io.StringIO()
        with redirect_stdout(out):
            jr.emit("note", level="info", text="x")
        self.assertEqual(out.getvalue(), "")
        self.assertEqual(file_markers(self.markers_file), [{"ev": "note", "level": "info", "text": "x"}])

    def test_the_owner_truncates_the_file_when_its_run_starts(self):
        self.markers_file.write_text('##run {"ev":"done","status":"fail"}\n', encoding="utf-8")
        with redirect_stdout(io.StringIO()):
            jr.run("test", "demo", "main", "abc1234")
        self.assertEqual([m["ev"] for m in file_markers(self.markers_file)], ["run"])

    def test_a_nested_script_appends_without_a_second_run_or_done(self):
        with redirect_stdout(io.StringIO()):
            jr.run("push", "demo", "main", "abc1234")
            with jr.step("prechecks", 1, "Prechecks"):
                pass
            self.child()
            jr.done("ok")
        found = file_markers(self.markers_file)
        self.assertEqual([m["ev"] for m in found].count("run"), 1)
        self.assertEqual([m["ev"] for m in found].count("done"), 1)
        self.assertEqual(found[0]["ev"], "run")
        self.assertEqual(found[-1], {"ev": "done", "status": "ok"})
        started, steps, status = replay(found)
        self.assertEqual((started["mode"], steps, status), ("push", {"prechecks": "ok", "tests": "ok"}, "ok"))

    def test_a_relative_path_is_made_absolute_for_nested_processes(self):
        os.environ[jr.MARKERS_FILE_ENV] = os.path.relpath(self.markers_file)
        with redirect_stdout(io.StringIO()):
            jr.run("test", "demo", "main", "abc1234")
        self.assertEqual(os.environ[jr.MARKERS_FILE_ENV], os.path.abspath(self.markers_file))

    def test_an_oversized_marker_is_dropped_from_the_file_too(self):
        os.environ["RUN_MARKERS"] = "1"
        out = io.StringIO()
        with redirect_stdout(out):
            jr.emit("note", level="info", text="x" * 70000)
            jr.emit("note", level="info", text="small")
        self.assertEqual(file_markers(self.markers_file), [{"ev": "note", "level": "info", "text": "small"}])
        self.assertEqual(markers(out.getvalue()), file_markers(self.markers_file))

    def test_a_file_that_cannot_be_written_warns_once_and_the_run_goes_on(self):
        os.environ[jr.MARKERS_FILE_ENV] = str(self.tmp / "missing-dir-is-a-file" / "m")
        (self.tmp / "missing-dir-is-a-file").write_text("", encoding="utf-8")
        err = io.StringIO()
        with mock.patch.object(sys, "stderr", err):
            jr.emit("note", level="info", text="a")
            jr.emit("note", level="info", text="b")
        self.assertEqual(err.getvalue().count("cannot write the markers file"), 1)

    def test_the_pty_child_gets_none_of_the_variables(self):
        os.environ[jr.RESULT_ENV] = str(self.result_file)
        os.environ[jr.RESULT_PARTS_ENV] = str(self.tmp)
        log = self.tmp / "env.json"
        jr._events = jr._EventsFile(self.tmp / "events.jsonl", "generic")
        self.addCleanup(lambda: (jr._events.close(), setattr(jr, "_events", None)))
        code = "import json, os, sys; json.dump(dict(os.environ), open(sys.argv[1], 'w'))"
        jr.run_pty([sys.executable, "-c", code, str(log)], "generic", "env", "env", cols=80, rows=24,
                   sinks=jr.Sinks("none", str(self.tmp / "pty.log")))
        env = json.loads(log.read_text(encoding="utf-8"))
        for key in (jr.MARKERS_FILE_ENV, jr.RESULT_ENV, jr.RESULT_PARTS_ENV):
            self.assertNotIn(key, env)


class ResultFileTest(FileCase):
    def setUp(self):
        super().setUp()
        os.environ[jr.RESULT_ENV] = str(self.result_file)

    def test_ok_run_with_a_nested_script(self):
        os.environ[jr.MARKERS_FILE_ENV] = str(self.markers_file)
        with redirect_stdout(io.StringIO()):
            jr.run("push", "demo", "main", "abc1234")
            jr.session("Demo", sinks=jr.Sinks("board", str(self.tmp / "full.log"), str(self.tmp / "cond.log")))
            with jr.step("prechecks", 1, "Prechecks", label="Prechecks") as s:
                s.summary = "3 checks"
            self.child()
            jr.note("info", "branch pushed", href="https://example.org/pr/1")
            jr.done("ok", commit="abc1234", exit=0)
            jr.end_session()
        r = self.result()
        self.assertEqual({k: r[k] for k in ("v", "status", "exit", "project", "branch", "head", "commit")},
                         {"v": 1, "status": "ok", "exit": 0, "project": "demo", "branch": "main", "head": "abc1234",
                          "commit": "abc1234"})
        self.assertIsInstance(r["ms"], int)
        rows = [{k: v for k, v in row.items() if k != "ms"} for row in r["rows"]]
        self.assertEqual(rows, [
            {"id": "prechecks", "label": "Prechecks", "status": "ok", "summary": "3 checks"},
            {"id": "tests", "label": "Tests", "status": "ok", "summary": "12 tests",
             "tests": {"total": 12, "fail": 0, "err": 0, "skip": 2}},
        ])
        self.assertEqual(r["notes"], [{"level": "warn", "text": "slow module"},
                                      {"level": "info", "text": "branch pushed", "href": "https://example.org/pr/1"}])
        self.assertEqual(r["logs"], {"full": str(self.tmp / "full.log"), "board": str(self.tmp / "cond.log")})
        self.assertNotIn(jr.RESULT_PARTS_ENV, os.environ)     # the parts directory is gone with it
        self.assertEqual(replay(file_markers(self.markers_file))[1], {r["id"]: r["status"] for r in r["rows"]})

    def test_a_failed_nested_script_fails_its_row(self):
        with redirect_stdout(io.StringIO()):
            jr.run("push", "demo", "main", "abc1234")
            self.child("fail")
            jr.done("fail", exit=1)
        r = self.result()
        self.assertEqual((r["status"], r["exit"]), ("fail", 1))
        self.assertEqual([(row["id"], row["status"], row["summary"]) for row in r["rows"]],
                         [("tests", "fail", "12 tests, 1 failed")])

    def test_a_row_still_running_at_done_ends_as_the_done_rule_says(self):
        for status, row_status in (("fail", "fail"), ("cancelled", "skip")):
            with self.subTest(status=status):
                reset_run_state()   # which forgets the variables too
                os.environ[jr.RESULT_ENV] = str(self.result_file)
                with redirect_stdout(io.StringIO()):
                    jr.run("test", "demo", "main", "abc1234")
                    jr.publish("step_start", id="build", n=1, title="Compile", parent=None, tool=None, sig="")
                    jr.done(status)
                r = self.result()
                self.assertEqual(r["exit"], 1 if status == "fail" else 130)
                self.assertEqual([row["status"] for row in r["rows"]], [row_status])

    def test_the_owner_removes_an_old_result_when_its_run_starts(self):
        self.result_file.write_text("{}", encoding="utf-8")
        with redirect_stdout(io.StringIO()):
            jr.run("test", "demo", "main", "abc1234")
        self.assertFalse(self.result_file.exists())

    def test_a_nested_process_writes_no_result_file(self):
        os.environ[jr.ACTIVE_ENV] = "1"
        os.environ[jr.RESULT_PARTS_ENV] = str(self.tmp / "parts")
        (self.tmp / "parts").mkdir()
        with redirect_stdout(io.StringIO()):
            jr.run("test", "demo", "main", "abc1234")
            with jr.step("build", 1, "Compile"):
                pass
            jr.done("ok")
        self.assertFalse(self.result_file.exists())
        part, = (self.tmp / "parts").glob("*.json")
        self.assertEqual([row["id"] for row in json.loads(part.read_text(encoding="utf-8"))["rows"]], ["build"])

    def test_the_write_is_atomic(self):
        seen = []
        real_replace = os.replace

        def replace(src, dst):
            seen.append((Path(src).parent, Path(src).name, Path(dst).exists(), json.loads(Path(src).read_text())))
            real_replace(src, dst)
        with redirect_stdout(io.StringIO()), mock.patch.object(os, "replace", replace):
            jr.run("test", "demo", "main", "abc1234")
            jr.done("ok")
        (folder, name, existed, content), = seen
        self.assertEqual(folder, self.result_file.parent)    # a temp file beside it, renamed over it
        self.assertNotEqual(name, self.result_file.name)
        self.assertFalse(existed)
        self.assertEqual(content, self.result())
        self.assertEqual(sorted(p.name for p in self.tmp.iterdir() if p.name.startswith(".")), [])

    def test_a_failed_write_leaves_no_file_and_warns(self):
        err = io.StringIO()
        with redirect_stdout(io.StringIO()), mock.patch.object(sys, "stderr", err), \
                mock.patch.object(os, "replace", mock.Mock(side_effect=OSError("disk full"))):
            jr.run("test", "demo", "main", "abc1234")
            jr.done("ok")
        self.assertFalse(self.result_file.exists())
        self.assertEqual([p.name for p in self.tmp.iterdir() if p.name.startswith(".")], [])
        self.assertIn("cannot write the result file", err.getvalue())


class ScriptSessionResultTest(FileCase):
    """Through juneau_scripts.run.session, as push.py and test.py run: ok, fail and Ctrl-C."""

    def setUp(self):
        super().setUp()
        cm = testing.clean_run_env()
        cm.__enter__()
        self.addCleanup(cm.__exit__, None, None, None)
        os.environ["JUNEAU_RUN_LOG_DIR"] = str(self.tmp / "logs")
        os.environ[jr.RESULT_ENV] = str(self.result_file)
        os.environ[jr.MARKERS_FILE_ENV] = str(self.markers_file)
        sys.modules["juneau_run"] = jr
        run._n = 0
        run._session = None
        for name in ("bell", "sound"):
            patcher = mock.patch.object(notify, name, lambda *a, **kw: None)
            patcher.start()
            self.addCleanup(patcher.stop)

    def go(self, body):
        with redirect_stdout(io.StringIO()):
            with run.session("Demo", mode="test", project="demo", exit=False) as s:
                with run.step("build", "Compile"):
                    pass
                body(s)
        return s

    def test_ok_fail_and_ctrl_c(self):
        def fails(s):
            with run.step("tests", "Java tests") as st:
                st.fail(exit=2)
            s.fail("❌ It broke.")

        def interrupt(s):
            with run.step("tests", "Java tests"):
                raise KeyboardInterrupt

        for body, status, code in ((lambda s: None, "ok", 0), (fails, "fail", 1), (interrupt, "cancelled", 130)):
            with self.subTest(status=status):
                reset_run_state()   # which forgets the variables too
                os.environ[jr.RESULT_ENV] = str(self.result_file)
                os.environ[jr.MARKERS_FILE_ENV] = str(self.markers_file)
                sys.modules["juneau_run"] = jr
                s = self.go(body)
                self.assertEqual(s.code, code)
                r = self.result()
                self.assertEqual((r["status"], r["exit"], r["project"]), (status, code, "demo"))
                self.assertEqual(r["rows"][0]["id"], "build")
                expected = {"ok": [], "fail": [("tests", "fail")], "cancelled": [("tests", "fail")]}[status]
                self.assertEqual([(row["id"], row["status"]) for row in r["rows"][1:]], expected)
                found = file_markers(self.markers_file)
                self.assertEqual((found[0]["ev"], found[-1]), ("run", {"ev": "done", "status": status}))


class BootstrapTest(unittest.TestCase):
    def setUp(self):
        isolate_run_env(self)

    def test_either_variable_asks_for_juneau_run(self):
        self.assertFalse(bootstrap.run_requested())
        for key in ("JUNEAU_RUN_MARKERS_FILE", "JUNEAU_RUN_RESULT"):
            with self.subTest(key=key), mock.patch.dict(os.environ, {key: "/tmp/x"}):
                self.assertTrue(bootstrap.run_requested())


if __name__ == "__main__":
    unittest.main()
