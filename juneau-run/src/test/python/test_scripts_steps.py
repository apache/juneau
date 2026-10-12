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
