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
"""Run history must never reach the developer's real ~/.cache: the session-running suites, run with the history-dir
override removed and HOME pointed at a throw-away directory, leave that directory's cache empty (spec 4.3), and the
README documents the history variables."""

import os
import subprocess
import sys
import shutil
import tempfile
import unittest
from pathlib import Path

import support

# Suites that open sessions or start history; each is run on its own in a child process.
SESSION_SUITES = ("test_scripts_session.py", "test_scripts_run.py", "test_session_plan.py", "test_scripts_steps.py")
# The child drops the history-dir override support.py installs, so only the fixtures stand between a test and HOME.
CHILD = ("import os, sys, unittest\n"
         "sys.path.insert(0, 'src/test/python')\n"
         "import support\n"
         "os.environ.pop('JUNEAU_TERMINAL_HISTORY_DIR', None)\n"
         "suite = unittest.defaultTestLoader.discover('src/test/python', pattern=sys.argv[1])\n"
         "sys.exit(0 if unittest.TextTestRunner(verbosity=0).run(suite).wasSuccessful() else 1)\n")


class RealCacheGuardTest(unittest.TestCase):
    def test_a_session_run_with_the_shared_fixtures_leaves_the_real_cache_empty(self):
        home = tempfile.mkdtemp(prefix="jr-home-")
        self.addCleanup(shutil.rmtree, home, ignore_errors=True)
        code = ("import support; jr = support.jr\n"
                "support.reset_run_state()\n"
                "import os; os.environ.pop('JUNEAU_TERMINAL_HISTORY_DIR', None)\n"      # simulate a fixture-less module
                "support.isolate_run_env(type('C', (), {'addCleanup': lambda s, f: None})())\n"
                "jr.history_start('.')\n")
        subprocess.run([sys.executable, "-B", "-c", code], cwd=support.FIXTURES.parent,
                       env={**os.environ, "HOME": home}, check=True)
        self.assertFalse(Path(home, ".cache", "juneau-terminal", "history").exists())

    def test_the_session_suites_leave_the_real_cache_empty(self):
        for suite in SESSION_SUITES:
            with self.subTest(suite=suite):
                home = tempfile.mkdtemp(prefix="jr-home-")
                self.addCleanup(shutil.rmtree, home, ignore_errors=True)
                env = {k: v for k, v in os.environ.items() if not k.startswith("JUNEAU_TERMINAL_") or k == "JUNEAU_TERMINAL_PY"}
                env["HOME"] = home
                done = subprocess.run([sys.executable, "-B", "-c", CHILD, suite], cwd=support.MODULE_ROOT, env=env,
                                      capture_output=True, text=True, timeout=300)
                # Only the cache matters here (those suites assert their own results), but the child must have run them.
                self.assertIn("\nRan ", done.stderr, done.stderr[-2000:])
                self.assertFalse(Path(home, ".cache", "juneau-terminal", "history").exists())

    def test_readme_documents_the_new_variables(self):
        text = (support.MODULE_ROOT / "README.md").read_text(encoding="utf-8")
        for name in ("JUNEAU_TERMINAL_HISTORY", "JUNEAU_TERMINAL_HISTORY_DIR", "JUNEAU_TERMINAL_PYTEST_PLUGIN"):
            with self.subTest(name=name):
                self.assertIn("| `" + name, text)


if __name__ == "__main__":
    unittest.main()
