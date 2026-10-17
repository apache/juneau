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
"""
prompt-pgp-passphrase.py drops gpg's smartcard-probe line from the dummy signing call's stderr, and nothing else.
"""

from __future__ import annotations

import contextlib
import importlib.util
import io
import subprocess
import unittest
from pathlib import Path
from unittest import mock

SCRIPTS_DIR = Path(__file__).resolve().parent.parent
CARD = "gpg: error retrieving key fingerprint from card: Invalid name"


def load():
    spec = importlib.util.spec_from_file_location("prompt_pgp_passphrase", SCRIPTS_DIR / "prompt-pgp-passphrase.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class CardNoiseTest(unittest.TestCase):
    def run_with_stderr(self, stderr):
        mod = load()
        done = subprocess.CompletedProcess([], 2, stdout=None, stderr=stderr)
        err = io.StringIO()
        with mock.patch.object(mod, "_resolve_gpg", return_value="/x/gpg"), \
                mock.patch.object(mod.subprocess, "run", return_value=done) as run, \
                contextlib.redirect_stderr(err), contextlib.redirect_stdout(io.StringIO()):
            self.assertFalse(mod.prompt_pgp_passphrase())  # gpg exited 2
        return err.getvalue(), run

    def test_the_card_probe_line_is_dropped(self):
        err, run = self.run_with_stderr(CARD + "\n")
        self.assertEqual(err, "")
        self.assertEqual(run.call_args.kwargs["stderr"], subprocess.PIPE)

    def test_real_gpg_errors_are_kept(self):
        err, _ = self.run_with_stderr(f"{CARD}\ngpg: signing failed: Bad passphrase\ngpg: no secret key\n")
        self.assertEqual(err, "gpg: signing failed: Bad passphrase\ngpg: no secret key\n")


class OutcomeTest(unittest.TestCase):
    def run_prompt(self, expect=True, **run_kwargs):
        mod = load()
        out = io.StringIO()
        with mock.patch.object(mod, "_resolve_gpg", return_value="/x/gpg"), \
                mock.patch.object(mod.subprocess, "run", **run_kwargs), \
                contextlib.redirect_stderr(io.StringIO()), contextlib.redirect_stdout(out):
            self.assertEqual(mod.prompt_pgp_passphrase(), expect)
        return out.getvalue()

    def test_exit_zero_prints_success(self):
        out = self.run_prompt(return_value=subprocess.CompletedProcess([], 0, stdout=None, stderr=""))
        self.assertIn("✅ PGP passphrase entered successfully", out)
        self.assertNotIn("⚠", out)

    def test_non_zero_exit_warns_with_code_and_no_success(self):
        out = self.run_prompt(expect=False, return_value=subprocess.CompletedProcess([], 2, stdout=None, stderr=""))
        self.assertNotIn("✅", out)
        self.assertIn("⚠ gpg exited with code 2", out)
        self.assertIn("signing later in this run will probably fail", out)

    def test_main_exit_code_follows_gpg_result(self):
        for rc, expected in ((0, 0), (2, 1)):
            mod = load()
            done = subprocess.CompletedProcess([], rc, stdout=None, stderr="")
            with mock.patch.object(mod, "_resolve_gpg", return_value="/x/gpg"), \
                    mock.patch.object(mod.subprocess, "run", return_value=done), \
                    contextlib.redirect_stderr(io.StringIO()), contextlib.redirect_stdout(io.StringIO()), \
                    self.assertRaises(SystemExit) as ctx:
                mod.main()
            self.assertEqual(ctx.exception.code, expected)

    def test_timeout_warns(self):
        out = self.run_prompt(side_effect=subprocess.TimeoutExpired("gpg", 60))
        self.assertNotIn("✅", out)
        self.assertIn("timed out", out)

    def test_gpg_missing_warns(self):
        mod = load()
        out = io.StringIO()
        with mock.patch.object(mod, "_resolve_gpg", return_value=None), \
                mock.patch.object(mod.subprocess, "run") as run, \
                contextlib.redirect_stdout(out):
            self.assertTrue(mod.prompt_pgp_passphrase())
        run.assert_not_called()
        self.assertNotIn("✅", out.getvalue())
        self.assertIn("gpg command not found", out.getvalue())


if __name__ == "__main__":
    unittest.main()
