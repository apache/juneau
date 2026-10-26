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
"""The shared fixtures keep run history out of the real cache (spec 4.3)."""

import os
import sys
import unittest

import support


class HistoryFixtureTest(unittest.TestCase):
    def test_support_points_history_at_a_temp_dir(self):
        d = os.environ.get("JUNEAU_TERMINAL_HISTORY_DIR")
        self.assertTrue(d, "support.py must set JUNEAU_TERMINAL_HISTORY_DIR at import")
        self.assertNotIn(os.path.join(os.path.expanduser("~"), ".cache"), d)

    def test_isolate_run_env_disables_history(self):
        support.isolate_run_env(self)
        self.assertEqual(os.environ.get("JUNEAU_TERMINAL_HISTORY"), "0")

    def test_reset_run_state_leaves_history_vars_alone(self):
        self.addCleanup(os.environ.__setitem__, "JUNEAU_TERMINAL_HISTORY_DIR", os.environ["JUNEAU_TERMINAL_HISTORY_DIR"])
        os.environ["JUNEAU_TERMINAL_HISTORY_DIR"] = "/tmp/x"
        support.reset_run_state()
        self.assertEqual(os.environ.get("JUNEAU_TERMINAL_HISTORY_DIR"), "/tmp/x")

    def test_subprocess_helpers_disable_history(self):
        import scripts_support
        import test_cli
        self.assertEqual(scripts_support.base_env().get("JUNEAU_TERMINAL_HISTORY"), "0")
        r = test_cli.cli("generic", "--", sys.executable, "-c", "import os; print(os.environ.get('JUNEAU_TERMINAL_HISTORY'))")
        self.assertEqual(r.stdout.strip(), "0")


if __name__ == "__main__":
    unittest.main()
