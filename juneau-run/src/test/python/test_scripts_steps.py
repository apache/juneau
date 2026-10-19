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
"""juneau_scripts.state.Checkpoint and juneau_scripts.steps.Registry."""

import argparse
import io
import json
import os
import tempfile
import unittest
from contextlib import redirect_stderr, redirect_stdout
from pathlib import Path
from unittest import mock

import scripts_support

from juneau_scripts import run, steps, testing
from juneau_scripts._base import Result
from juneau_scripts.state import Checkpoint, CheckpointError


class CheckpointTest(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.path = Path(tmp.name) / "state" / "release.json"
        self.cp = Checkpoint(self.path)

    def test_round_trip(self):
        self.assertIsNone(self.cp.last())
        self.assertEqual(self.cp.get("x", 5), 5)
        self.cp.set("last", "tag")
        self.cp.set("version", "10.0.0")
        self.assertEqual((self.cp.last(), self.cp.get("version")), ("tag", "10.0.0"))
        self.assertEqual(json.loads(self.path.read_text(encoding="utf-8")), {"last": "tag", "version": "10.0.0"})
        self.cp.clear()
        self.assertFalse(self.path.exists())
        self.cp.clear()

    def test_checkpoint_update_atomic_and_none_removes(self):
        self.cp.set("a", 1)
        self.cp.set("b", 2)
        self.cp.update({"b": None, "c": 3, "d": "x"})
        self.assertEqual(json.loads(self.path.read_text(encoding="utf-8")), {"a": 1, "c": 3, "d": "x"})
        self.cp.update({"zzz": None})  # removing an absent key is a no-op
        self.assertEqual(self.cp.get("c"), 3)

    def test_update_is_one_write(self):
        self.cp.set("a", 1)
        with mock.patch("juneau_scripts.state.os.replace", side_effect=OSError("disk full")):
            with self.assertRaises(OSError):
                self.cp.update({"a": 2, "b": 3})
        self.assertEqual(json.loads(self.path.read_text(encoding="utf-8")), {"a": 1})
        self.assertEqual([p.name for p in self.path.parent.iterdir()], ["release.json"])

    def test_atomic_write_leaves_no_temporary_files(self):
        self.cp.set("last", "a")
        with mock.patch("juneau_scripts.state.os.replace", side_effect=OSError("disk full")):
            with self.assertRaises(OSError):
                self.cp.set("last", "b")
        self.assertEqual(self.cp.last(), "a")
        self.assertEqual(sorted(p.name for p in self.path.parent.iterdir()), ["release.json"])

    def test_corrupt_raises_naming_the_path(self):
        self.path.parent.mkdir(parents=True)
        for text in ("{not json", "[1, 2]"):
            with self.subTest(text=text):
                self.path.write_text(text, encoding="utf-8")
                with self.assertRaises(CheckpointError) as cm:
                    self.cp.last()
                self.assertIn(str(self.path), str(cm.exception))


class RegistryCase(unittest.TestCase):
    def setUp(self):
        cm = testing.clean_run_env()
        cm.__enter__()
        self.addCleanup(cm.__exit__, None, None, None)
        run._n = 0
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.cp = Checkpoint(Path(tmp.name) / "cp.json")
        self.ran = []
        self.reg = steps.Registry()
        for name, title in (("prereqs", "Prerequisites"), ("build", "Build"), ("tag", "Tag"), ("publish", "Publish")):
            self.reg.step(name, title)(self.make(name))
        self.parser = argparse.ArgumentParser(prog="release.py")
        self.reg.add_arguments(self.parser)
        self.fail_in = None

    def make(self, name):
        def fn(ctx):
            self.ran.append(name)
            if name == self.fail_in:
                return Result(False, 3, err="boom")
        return fn

    def go(self, *argv):
        out = io.StringIO()
        with redirect_stdout(out):
            r = self.reg.run(self.parser.parse_args(list(argv)), ctx={}, checkpoint=self.cp)
        return r, out.getvalue()


class PauseTest(RegistryCase):
    def pausing(self, name, done):
        def fn(ctx):
            self.ran.append(name)
            raise steps.Pause("Wait for the vote", done=done)
        i = self.reg.names.index(name)
        self.reg._steps[i] = (name, self.reg._steps[i][1], fn)

    def test_pause_done_records_last_and_stops(self):
        self.pausing("build", done=True)
        r, _ = self.go()
        self.assertEqual(self.ran, ["prereqs", "build"])
        self.assertEqual(self.cp.last(), "build")
        self.assertTrue(r.ok)

    def test_pause_not_done_keeps_last(self):
        self.pausing("build", done=False)
        self.go()
        self.assertEqual(self.cp.last(), "prereqs")

    def test_pause_result_status_paused(self):
        self.pausing("build", done=True)
        r, _ = self.go()
        self.assertEqual((r.ok, r.code, r.status, r.out), (True, 0, "paused", "Wait for the vote"))


class RegistryTest(RegistryCase):
    def test_list_steps(self):
        r, out = self.go("--list-steps")
        self.assertTrue(r.ok)
        self.assertEqual(self.ran, [])
        self.assertEqual([line.split()[:2] for line in out.splitlines()],
                         [["1.", "prereqs"], ["2.", "build"], ["3.", "tag"], ["4.", "publish"]])

    def test_runs_all_and_records_the_checkpoint(self):
        r, _ = self.go()
        self.assertTrue(r.ok)
        self.assertEqual(self.ran, ["prereqs", "build", "tag", "publish"])
        self.assertEqual(self.cp.last(), "publish")

    def test_start_step(self):
        self.go("--start-step", "tag")
        self.assertEqual(self.ran, ["tag", "publish"])

    def test_repeated_skip_step(self):
        self.go("--skip-step", "build", "--skip-step", "tag")
        self.assertEqual(self.ran, ["prereqs", "publish"])

    def test_failure_hint_and_resume(self):
        self.fail_in = "tag"
        r, out = self.go()
        self.assertEqual((r.ok, r.code), (False, 3))
        self.assertEqual(self.ran, ["prereqs", "build", "tag"])
        self.assertIn("To resume: release.py --start-step tag", out)
        self.assertEqual(self.cp.last(), "build")
        self.fail_in = None
        self.ran.clear()
        r, out = self.go("--resume")
        self.assertTrue(r.ok)
        self.assertEqual(self.ran, ["tag", "publish"])
        self.assertIn("Resuming after build", out)

    def test_resume_with_an_empty_checkpoint_starts_at_the_first_step(self):
        r, out = self.go("--resume")
        self.assertEqual(self.ran, ["prereqs", "build", "tag", "publish"])
        self.assertIn("No checkpoint recorded; starting at the first step.", out)

    def test_resume_after_the_last_step_runs_nothing(self):
        self.cp.set("last", "publish")
        r, out = self.go("--resume")
        self.assertTrue(r.ok)
        self.assertEqual(self.ran, [])
        self.assertIn("nothing to resume", out)

    def test_an_exception_in_a_step_fails_with_the_hint(self):
        def broken(ctx):
            raise ValueError("bad input")
        reg = steps.Registry()
        reg.step("one", "One")(broken)
        parser = argparse.ArgumentParser(prog="r.py")
        reg.add_arguments(parser)
        out = io.StringIO()
        with redirect_stdout(out):
            r = reg.run(parser.parse_args([]), ctx=None)
        self.assertEqual((r.ok, r.code), (False, 1))
        self.assertIn("❌ ERROR: ValueError: bad input", out.getvalue())
        self.assertIn("To resume: r.py --start-step one", out.getvalue())

    def test_the_resume_hint_is_an_error_line_so_every_detail_level_shows_it(self):
        def broken(ctx):
            raise ValueError("bad input")
        reg = steps.Registry()
        reg.step("one", "One")(broken)
        parser = argparse.ArgumentParser(prog="r.py")
        reg.add_arguments(parser)
        with mock.patch.object(run, "say") as say:
            reg.run(parser.parse_args([]), ctx=None)
        self.assertIn(mock.call("To resume: r.py --start-step one", "error"), say.call_args_list)

    def test_keyboard_interrupt_prints_the_hint_and_propagates(self):
        def cancel(ctx):
            raise KeyboardInterrupt
        reg = steps.Registry()
        reg.step("one", "One")(cancel)
        parser = argparse.ArgumentParser(prog="r.py")
        reg.add_arguments(parser)
        out = io.StringIO()
        with redirect_stdout(out), self.assertRaises(KeyboardInterrupt):
            reg.run(parser.parse_args([]), ctx=None)
        self.assertIn("To resume: r.py --start-step one", out.getvalue())

    def test_registry_plan_start_skip_and_resume(self):
        self.assertEqual(self.reg.plan(self.parser.parse_args(["--start-step", "tag"])), ["tag", "publish"])
        self.assertEqual(self.reg.plan(self.parser.parse_args(["--skip-step", "build"])), ["prereqs", "tag", "publish"])
        self.cp.set("last", "build")
        with redirect_stdout(io.StringIO()):
            planned = self.reg.plan(self.parser.parse_args(["--resume"]), self.cp)
        self.assertEqual(planned, ["tag", "publish"])

    def test_registry_plan_is_empty_when_every_step_is_done(self):
        self.cp.set("last", "publish")
        with redirect_stdout(io.StringIO()):
            self.assertEqual(self.reg.plan(self.parser.parse_args(["--resume"]), self.cp), [])

    def test_plan_with_resume_prints_nothing_and_run_prints_once(self):
        self.cp.set("last", "build")
        out = io.StringIO()
        with redirect_stdout(out):
            self.reg.plan(self.parser.parse_args(["--resume"]), self.cp)
        self.assertEqual(out.getvalue(), "")
        _, printed = self.go("--resume")
        self.assertEqual(printed.count("Resuming after build"), 1)

    def test_plan_and_run_cannot_disagree(self):
        argv = ["--skip-step", "build"]
        planned = self.reg.plan(self.parser.parse_args(argv))
        self.go(*argv)
        self.assertEqual(planned, self.ran)


class _StepSpy:
    """Stands in for run.step: records each row's id, n, label, and final status and summary."""

    def __init__(self):
        self.rows = []

    def __call__(self, id, title, *, n=None, parent=None, label=None):  # NOSONAR python:S5806 - mirrors run.step
        spy = self

        class _Ctx:
            def __enter__(self):
                self.s = run._NullStep()
                self.s.status, self.s.summary = "ok", None
                self.s.skip = lambda: setattr(self.s, "status", "skip")
                self.s.fail = lambda exit=None: setattr(self.s, "status", "fail")
                return self.s

            def __exit__(self, *exc):
                spy.rows.append({"id": id, "n": n, "label": label, "status": self.s.status,
                                 "summary": self.s.summary})
                return False
        return _Ctx()


class RegistryLabelsAndUnselectedTest(RegistryCase):
    def spy_go(self, *argv, reg=None, **kw):
        spy = _StepSpy()
        out = io.StringIO()
        reg = reg or self.reg
        with mock.patch.object(run, "step", spy), redirect_stdout(out):
            r = reg.run(self.parser.parse_args(list(argv)), ctx={}, checkpoint=self.cp, **kw)
        return r, spy.rows, out.getvalue()

    def labelled(self):
        reg = steps.Registry()
        for name, title, label in (("prereqs", "Prerequisites", None), ("build", "Build", "mvn"),
                                   ("tag", "Tag", None), ("publish", "Publish", "nexus")):
            reg.step(name, title, label=label)(self.make(name))
        return reg

    def test_a_step_label_reaches_run_step(self):
        _, rows, _ = self.spy_go(reg=self.labelled())
        self.assertEqual([(r["id"], r["label"]) for r in rows],
                         [("prereqs", None), ("build", "mvn"), ("tag", None), ("publish", "nexus")])

    def test_names_plan_and_run_are_unchanged_by_labels(self):
        reg = self.labelled()
        parser = argparse.ArgumentParser(prog="release.py")
        reg.add_arguments(parser)
        self.assertEqual(reg.names, self.reg.names)
        for argv in ([], ["--start-step", "tag"], ["--skip-step", "build"]):
            self.assertEqual(reg.plan(parser.parse_args(argv)), self.reg.plan(self.parser.parse_args(argv)))
        outs = []
        for r in (self.reg, reg):
            out = io.StringIO()
            with redirect_stdout(out):
                r.run(self.parser.parse_args(["--list-steps"]), ctx={})
            outs.append(out.getvalue())
        self.assertEqual(outs[0], outs[1])

    def test_unselected_steps_are_dim_rows_numbered_from_one_when_asked(self):
        r, rows, _ = self.spy_go("--start-step", "build", show_unselected=True)
        self.assertTrue(r.ok)
        self.assertEqual(self.ran, ["build", "tag", "publish"])
        self.assertEqual([(x["id"], x["n"], x["status"], x["summary"]) for x in rows],
                         [("prereqs", 1, "skip", "(done earlier)"), ("build", 2, "ok", None),
                          ("tag", 3, "ok", None), ("publish", 4, "ok", None)])

    def test_a_done_earlier_row_leaves_the_checkpoint_alone(self):
        self.fail_in = "build"
        r, rows, _ = self.spy_go("--start-step", "build", show_unselected=True)
        self.assertFalse(r.ok)
        self.assertEqual(rows[0]["summary"], "(done earlier)")
        self.assertIsNone(self.cp.last())

    def test_a_skip_step_row_is_recorded_as_last(self):
        self.fail_in = "tag"
        r, _, _ = self.spy_go("--skip-step", "build", show_unselected=True)
        self.assertFalse(r.ok)
        self.assertEqual(self.cp.last(), "build")

    def test_skipped_steps_say_skipped_when_asked(self):
        _, rows, out = self.spy_go("--skip-step", "build", show_unselected=True)
        self.assertEqual(self.ran, ["prereqs", "tag", "publish"])
        self.assertEqual([(x["id"], x["status"], x["summary"]) for x in rows if x["status"] == "skip"],
                         [("build", "skip", "(skipped)")])
        self.assertEqual(out, "")

    def test_a_step_that_returns_text_sets_its_row_summary(self):
        reg = steps.Registry()
        reg.step("prereqs", "Prerequisites")(lambda ctx: "Java 17")
        reg.step("build", "Build")(lambda ctx: None)
        reg.step("tag", "Tag")(lambda ctx: Result(True, 0))
        _, rows, _ = self.spy_go(reg=reg)
        self.assertEqual([(x["id"], x["status"], x["summary"]) for x in rows],
                         [("prereqs", "ok", "Java 17"), ("build", "ok", None), ("tag", "ok", None)])

    def test_unselected_rows_are_off_by_default(self):
        _, rows, out = self.spy_go("--start-step", "tag", "--skip-step", "publish")
        self.assertEqual([(x["id"], x["n"], x["status"], x["summary"]) for x in rows],
                         [("tag", 3, "ok", None), ("publish", 4, "skip", None)])
        self.assertIn("⏭  Skipping 4. Publish", out)


class RegistryErrorsTest(RegistryCase):
    def parse_exit(self, *argv):
        err = io.StringIO()
        with redirect_stderr(err), self.assertRaises(SystemExit) as cm:
            self.parser.parse_args(list(argv))
        return cm.exception.code, err.getvalue()

    def test_argparse_errors_exit_2(self):
        code, err = self.parse_exit("--start-step", "nope")
        self.assertEqual(code, 2)
        self.assertIn("invalid choice: 'nope'", err)
        self.assertIn("prereqs", err)      # the valid names are listed (quoted or not, by Python version)
        self.assertEqual(self.parse_exit("--skip-step", "nope")[0], 2)
        self.assertEqual(self.parse_exit("--bogus")[0], 2)
        code, err = self.parse_exit("--start-step", "tag", "--resume")
        self.assertEqual(code, 2)
        self.assertIn("not allowed with argument", err)

    def test_programming_errors_raise(self):
        with self.assertRaisesRegex(ValueError, "already registered"):
            self.reg.step("build", "Again")
        with self.assertRaisesRegex(ValueError, "unknown step 'nope'"):
            self.reg.run(argparse.Namespace(list_steps=False, start_step="nope", resume=False, skip_step=[]), {})
        with self.assertRaisesRegex(ValueError, "--resume needs a checkpoint"):
            self.reg.run(argparse.Namespace(list_steps=False, start_step=None, resume=True, skip_step=[]), {})


if __name__ == "__main__":
    unittest.main()
