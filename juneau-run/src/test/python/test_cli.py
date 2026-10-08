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
"""CLI: run as a subprocess; exit codes pass through and run/done are emitted only by the outermost process."""

import json
import os
import subprocess
import sys
import unittest

from support import SCRIPT, jr


def cli(*args, env=None):
    full_env = {k: v for k, v in os.environ.items() if not k.startswith(("RUN_", "JUNEAU_RUN_"))}
    full_env.update(env or {})
    return subprocess.run([sys.executable, str(SCRIPT), *args], capture_output=True, text=True, env=full_env)


def evs(stdout):
    return [json.loads(line[len(jr.PREFIX):])["ev"] for line in stdout.splitlines() if line.startswith(jr.PREFIX)]


class CliTest(unittest.TestCase):
    def test_exit_code_passes_through_in_pass_through_mode(self):
        r = cli("generic", "--", sys.executable, "-c", "print('hi'); import sys; sys.exit(3)")
        self.assertEqual((r.returncode, r.stdout), (3, "hi\n"))

    def test_with_markers_the_outermost_cli_emits_run_step_end_done(self):
        r = cli("--step", "t", "generic", "--", sys.executable, "-c", "pass", env={"RUN_MARKERS": "1"})
        self.assertEqual(evs(r.stdout), ["run", "step", "end", "done"])
        self.assertIn('"mode":"test"', r.stdout)

    def test_failure_ends_the_run_with_done_fail(self):
        r = cli("generic", "--", sys.executable, "-c", "import sys; sys.exit(2)", env={"RUN_MARKERS": "1"})
        self.assertEqual(r.returncode, 2)
        self.assertIn('{"ev":"done","status":"fail"}', r.stdout)

    def test_nested_cli_emits_steps_but_no_run_or_done(self):
        r = cli("generic", "--", sys.executable, "-c", "pass", env={"RUN_MARKERS": "1", jr.ACTIVE_ENV: "1"})
        self.assertEqual(evs(r.stdout), ["step", "end"])

    def test_condensed_console_prints_only_the_parser_lines_unlike_the_full_console(self):
        code = "print('noise')"
        full = cli("--console", "full", "generic", "--", sys.executable, "-c", code)
        condensed = cli("--console", "condensed", "generic", "--", sys.executable, "-c", code)
        self.assertEqual(full.stdout, "noise\n")
        self.assertEqual(condensed.stdout, "")   # the generic parser condenses a passing run to nothing

    def test_condensed_console_on_failure_shows_only_the_generic_tail(self):
        code = "[print(i) for i in range(100)]; import sys; sys.exit(1)"
        full = cli("--console", "full", "generic", "--", sys.executable, "-c", code)
        condensed = cli("--console", "condensed", "generic", "--", sys.executable, "-c", code)
        self.assertEqual(len(full.stdout.splitlines()), 100)
        self.assertEqual(condensed.stdout.splitlines(), [str(i) for i in range(60, 100)])

    def test_no_command_is_a_usage_error(self):
        self.assertEqual(cli("generic").returncode, 2)


if __name__ == "__main__":
    unittest.main()
