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
"""Sinks: console modes, log routing, $RUN_ARTIFACTS resolution, argument-over-environment precedence."""

import contextlib
import io
import os
import tempfile
import unittest
from pathlib import Path
from unittest import mock

from support import jr


class SinksTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        patcher = mock.patch.dict(os.environ)
        patcher.start()
        self.addCleanup(patcher.stop)
        for key in ("JUNEAU_RUN_CONSOLE", "JUNEAU_RUN_FULL_LOG", "JUNEAU_RUN_CONDENSED_LOG", "RUN_ARTIFACTS"):
            os.environ.pop(key, None)

    def drive(self, sinks):
        """Writes one full chunk and one condensed line; returns the console text."""
        buf = io.StringIO()
        with contextlib.redirect_stdout(buf):
            sinks.open()
            sinks.write_full(b"full-line\n")
            sinks.write_condensed("condensed-line")
            sinks.close()
        return buf.getvalue()

    def test_routing_matrix(self):
        for console, want in (("full", "full-line\n"), ("condensed", "condensed-line\n"), ("none", "")):
            for with_logs in (False, True):
                with self.subTest(console=console, logs=with_logs):
                    full, cond = (self.tmp / f"{console}-f.log", self.tmp / f"{console}-c.log") if with_logs else (None, None)
                    sinks = jr.Sinks(console, full, cond)
                    self.assertEqual(self.drive(sinks), want)
                    if with_logs:
                        self.assertEqual(full.read_bytes(), b"full-line\n")
                        self.assertEqual(cond.read_bytes(), b"condensed-line\n")

    def test_default_is_pass_through_equivalent(self):
        self.assertTrue(jr.Sinks().is_default)
        self.assertFalse(jr.Sinks("condensed").is_default)
        self.assertFalse(jr.Sinks(full_log=self.tmp / "x.log").is_default)

    def test_relative_log_paths_resolve_under_run_artifacts(self):
        os.environ["RUN_ARTIFACTS"] = str(self.tmp / "art")
        sinks = jr.Sinks(full_log="out/full.log")
        self.assertEqual(sinks.full_path, self.tmp / "art" / "out" / "full.log")
        self.drive(sinks)
        self.assertTrue(sinks.full_path.exists())

    def test_relative_log_paths_without_run_artifacts_stay_relative(self):
        self.assertEqual(jr.Sinks(full_log="full.log").full_path, Path("full.log"))

    def test_environment_variables_configure_the_sinks(self):
        os.environ.update(JUNEAU_RUN_CONSOLE="none", JUNEAU_RUN_FULL_LOG=str(self.tmp / "e.log"))
        sinks = jr.Sinks()
        self.assertEqual((sinks.console, sinks.full_path), ("none", self.tmp / "e.log"))

    def test_argument_beats_environment(self):
        os.environ["JUNEAU_RUN_CONSOLE"] = "none"
        self.assertEqual(jr.Sinks(console="condensed").console, "condensed")

    def test_invalid_console_mode_is_rejected(self):
        with self.assertRaises(ValueError):
            jr.Sinks(console="loud")

    def test_logs_append_across_runs(self):
        log = self.tmp / "append.log"
        self.drive(jr.Sinks("none", log))
        self.drive(jr.Sinks("none", log))
        self.assertEqual(log.read_bytes(), b"full-line\nfull-line\n")

    def test_diagnostics_go_to_stderr(self):
        out, err = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            jr.warn("something")
        self.assertEqual(out.getvalue(), "")
        self.assertEqual(err.getvalue(), "juneau-run: something\n")


if __name__ == "__main__":
    unittest.main()
