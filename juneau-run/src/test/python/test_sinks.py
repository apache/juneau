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
import re
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

    def test_condensed_defaults_a_full_log_and_shares_it_through_the_env(self):
        os.environ["JUNEAU_RUN_LOG_DIR"] = str(self.tmp)
        with mock.patch.object(jr.sys, "argv", ["/x/scripts/push.py"]):
            first = jr.Sinks(console="condensed")
        self.assertRegex(str(first.full_path), rf"^{re.escape(str(self.tmp))}/push-\d{{8}}-\d{{6}}\.log$")
        self.assertEqual(os.environ["JUNEAU_RUN_FULL_LOG"], str(first.full_path))
        self.assertEqual(jr.Sinks(console="condensed").full_path, first.full_path)   # a nested run's Sinks

    def test_condensed_and_none_get_a_default_and_a_given_log_wins(self):
        os.environ["JUNEAU_RUN_LOG_DIR"] = str(self.tmp)
        self.assertIsNone(jr.Sinks(console="full").full_path)
        self.assertEqual(jr.Sinks(console="condensed", full_log=str(self.tmp / "mine.log")).full_path,
                         self.tmp / "mine.log")
        self.assertNotIn("JUNEAU_RUN_FULL_LOG", os.environ)
        self.assertEqual(jr.Sinks(console="none", full_log=str(self.tmp / "mine.log")).full_path,
                         self.tmp / "mine.log")
        self.assertNotIn("JUNEAU_RUN_FULL_LOG", os.environ)
        default = jr.Sinks(console="none").full_path
        self.assertRegex(str(default), rf"^{re.escape(str(self.tmp))}/.+-\d{{8}}-\d{{6}}\.log$")
        self.assertEqual(os.environ["JUNEAU_RUN_FULL_LOG"], str(default))

    def test_condensed_defaults_a_condensed_log_beside_the_full_one_and_shares_it(self):
        os.environ["JUNEAU_RUN_LOG_DIR"] = str(self.tmp)
        with mock.patch.object(jr.sys, "argv", ["/x/scripts/push.py"]):
            first = jr.Sinks(console="condensed")
        self.assertRegex(str(first.condensed_path),
                         rf"^{re.escape(str(self.tmp))}/push-\d{{8}}-\d{{6}}\.condensed\.log$")
        self.assertEqual(str(first.condensed_path), str(first.full_path)[:-4] + ".condensed.log")
        self.assertEqual(os.environ["JUNEAU_RUN_CONDENSED_LOG"], str(first.condensed_path))
        self.assertEqual(jr.Sinks(console="none").condensed_path, first.condensed_path)    # a nested run's Sinks

    def test_a_given_log_means_no_default_condensed_log(self):
        os.environ["JUNEAU_RUN_LOG_DIR"] = str(self.tmp)
        self.assertIsNone(jr.Sinks(console="condensed", full_log=str(self.tmp / "mine.log")).condensed_path)
        self.assertIsNone(jr.Sinks(console="full").condensed_path)
        mine = str(self.tmp / "mine.condensed.log")
        sinks = jr.Sinks(console="condensed", condensed_log=mine)
        self.assertEqual(sinks.condensed_path, Path(mine))
        self.assertEqual(os.environ.get("JUNEAU_RUN_CONDENSED_LOG"), None)

    def test_prune_counts_full_and_condensed_logs_separately(self):
        os.environ["JUNEAU_RUN_LOG_DIR"] = str(self.tmp)
        for i in range(25):
            for name in (f"push-{i:02d}.log", f"push-{i:02d}.condensed.log"):
                (self.tmp / name).write_text("")
                os.utime(self.tmp / name, (1000 + i, 1000 + i))
        for i in range(25, 30):     # newer condensed logs must not push full logs out, nor the reverse
            (self.tmp / f"test-{i}.condensed.log").write_text("")
            os.utime(self.tmp / f"test-{i}.condensed.log", (1000 + i, 1000 + i))
        jr.prune_full_logs()
        names = sorted(p.name for p in self.tmp.glob("*.log"))
        self.assertEqual([n for n in names if not n.endswith(".condensed.log")],
                         [f"push-{i:02d}.log" for i in range(5, 25)])
        self.assertEqual(len([n for n in names if n.endswith(".condensed.log")]), 20)
        self.assertNotIn("push-04.condensed.log", names)
        self.assertIn("test-29.condensed.log", names)

    def test_safe_name_replaces_whitespace_and_punctuation(self):
        for raw, want in (("python3 -m unittest", "python3-m-unittest"),
                          ("a/b c", "a-b-c"),
                          ("push", "push"),
                          ("  weird::name!! ", "weird-name"),
                          ("juneau_run.v2", "juneau_run.v2"),
                          ("", "run"),
                          ("///", "run")):
            with self.subTest(raw=raw):
                self.assertEqual(jr.safe_name(raw), want)

    def test_default_full_log_has_no_spaces_when_argv0_is_a_command(self):
        with mock.patch.object(jr.sys, "argv", ["python3 -m unittest"]):
            name = jr.default_full_log().name
        self.assertNotRegex(name, r"\s")
        self.assertTrue(name.startswith("python3-m-unittest-"), name)

    def test_default_full_log_is_unique_within_one_second(self):
        os.environ["JUNEAU_RUN_LOG_DIR"] = str(self.tmp)
        with mock.patch.object(jr.time, "strftime", return_value="20261018-173509"), \
                mock.patch.object(jr.sys, "argv", ["/x/scripts/push.py"]):
            paths = [jr.default_full_log() for _ in range(3)]
        self.assertEqual([p.name for p in paths],
                         ["push-20261018-173509.log", "push-20261018-173509-2.log", "push-20261018-173509-3.log"])
        self.assertTrue(all(p.exists() for p in paths))

    def test_prune_keeps_the_newest_twenty_logs(self):
        os.environ["JUNEAU_RUN_LOG_DIR"] = str(self.tmp)
        for i in range(25):
            path = self.tmp / f"push-{i:02d}.log"
            path.write_text("")
            os.utime(path, (1000 + i, 1000 + i))
        (self.tmp / "keep.txt").write_text("")
        jr.prune_full_logs()
        self.assertEqual(sorted(p.name for p in self.tmp.glob("*.log")), [f"push-{i:02d}.log" for i in range(5, 25)])
        self.assertTrue((self.tmp / "keep.txt").exists())

    def drive(self, sinks):
        """Writes one full chunk and one condensed line; returns the console text."""
        buf = io.StringIO()
        with contextlib.redirect_stdout(buf):
            sinks.open()
            sinks.write_full(b"full-line\n")
            sinks.write_condensed("condensed-line")
            sinks.close()
        return buf.getvalue()

    def test_condensed_lines_colour_their_marks_on_a_terminal_only(self):
        class Tty(io.StringIO):
            def isatty(self):
                return True
        for stream, no_color, want in ((Tty(), None, "\x1b[32m✓\x1b[0m Mod  1s\n"), (Tty(), "1", "✓ Mod  1s\n"),
                                       (io.StringIO(), None, "✓ Mod  1s\n")):
            with self.subTest(tty=isinstance(stream, Tty), no_color=no_color):
                cond = self.tmp / "marks-c.log"
                cond.unlink(missing_ok=True)
                sinks = jr.Sinks("condensed", None, cond)
                with mock.patch.dict(os.environ, {"NO_COLOR": no_color} if no_color else {}), \
                        contextlib.redirect_stdout(stream):
                    if not no_color:
                        os.environ.pop("NO_COLOR", None)
                    sinks.open()
                    sinks.write_condensed("✓ Mod  1s")
                    sinks.close()
                self.assertEqual(stream.getvalue(), want)
                self.assertEqual(cond.read_text(encoding="utf-8"), "✓ Mod  1s\n")

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
