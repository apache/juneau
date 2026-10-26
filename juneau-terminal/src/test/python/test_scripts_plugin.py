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
"""juneau_terminal_scripts.testing.pytest_plugin: fixture wrappers over the stdlib testing core."""

import importlib.util
import shutil
import subprocess
import sys
import tempfile
import textwrap
import unittest
from pathlib import Path

import scripts_support
from scripts_support import PYTHON_SRC, base_env, python

SUITE = textwrap.dedent('''
    import os, subprocess
    pytest_plugins = ["juneau_terminal_scripts.testing.pytest_plugin"]

    def test_hermetic_git(hermetic_git):
        assert os.environ["GIT_CONFIG_GLOBAL"] == os.devnull
        assert hermetic_git["GIT_TERMINAL_PROMPT"] == "0"

    def test_bare_remote(bare_remote):
        work, origin = bare_remote
        assert subprocess.run(["git", "rev-parse", "--abbrev-ref", "@{u}"], cwd=work, capture_output=True,
                              text=True).stdout.strip() == "origin/main"

    def test_clean_run_env(clean_run_env):
        assert "TERMINAL_MARKERS" not in os.environ

    def test_fake_tool(fake_tool, tmp_path):
        fake_tool("mvn", exit_env="FAKE_EXIT")
        os.environ["FAKE_EXIT"] = "3"
        try:
            assert subprocess.run(["mvn", "verify"]).returncode == 3
        finally:
            del os.environ["FAKE_EXIT"]
''')


def pytest_launcher():
    if importlib.util.find_spec("pytest") is not None:
        return [sys.executable, "-B", "-m", "pytest"]
    if shutil.which("uv"):
        probe = subprocess.run(["uv", "run", "--offline", "--with", "pytest", "python", "-c", "import pytest"],
                               capture_output=True, text=True, check=False)
        if probe.returncode == 0:
            return ["uv", "run", "--offline", "--with", "pytest", "python", "-B", "-m", "pytest"]
    return None


class PluginTest(unittest.TestCase):
    def test_fixtures(self):
        launcher = pytest_launcher()
        if launcher is None:
            self.skipTest("pytest isn't installed (and uv can't provide it offline)")
        with tempfile.TemporaryDirectory() as tmp:
            Path(tmp, "test_plugin_suite.py").write_text(SUITE, encoding="utf-8")
            done = subprocess.run([*launcher, "-q", "-p", "no:cacheprovider", "test_plugin_suite.py"], cwd=tmp,
                                  env=base_env(TERMINAL_MARKERS="1", PYTHONPATH=str(PYTHON_SRC)), capture_output=True,
                                  text=True, timeout=300)
        self.assertEqual(done.returncode, 0, done.stdout + done.stderr)
        self.assertIn("4 passed", done.stdout)

    def test_importing_the_toolkit_never_imports_pytest(self):
        done = python(["-c", "import sys; sys.path.insert(0, sys.argv[1]); import juneau_terminal_scripts, "
                             "juneau_terminal_scripts.testing, juneau_terminal_scripts.run, juneau_terminal_scripts.git; "
                             "print('pytest' in sys.modules)", str(PYTHON_SRC)], env=base_env())
        self.assertEqual(done.stdout.strip(), "False", done.stderr)


if __name__ == "__main__":
    unittest.main()
