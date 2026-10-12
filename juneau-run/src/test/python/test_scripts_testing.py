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
"""juneau_scripts.testing's stdlib core: hermetic_git, bare_remote, clean_run_env and fake_tool."""

import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

import scripts_support  # noqa: F401

from juneau_scripts import testing


class HermeticGitTest(unittest.TestCase):
    def test_removes_inherited_git_variables_and_restores_them(self):
        with tempfile.TemporaryDirectory() as tmp, \
                mock.patch.dict(os.environ, {"GIT_TRACE": "1", "GIT_DIR": "/nowhere", "GIT_AUTHOR_NAME": "X"}):
            with testing.hermetic_git(tmp) as overrides:
                self.assertNotIn("GIT_TRACE", os.environ)
                self.assertNotIn("GIT_DIR", os.environ)
                self.assertNotIn("GIT_AUTHOR_NAME", os.environ)
                self.assertEqual(os.environ["GIT_CONFIG_GLOBAL"], os.devnull)
                self.assertEqual(os.environ["GIT_CONFIG_NOSYSTEM"], "1")
                self.assertEqual(os.environ["GIT_TERMINAL_PROMPT"], "0")
                self.assertEqual(os.environ["GIT_CEILING_DIRECTORIES"], str(Path(os.path.realpath(tmp)).parent))
                self.assertEqual(overrides["GIT_TERMINAL_PROMPT"], "0")
            self.assertEqual(os.environ["GIT_TRACE"], "1")
            self.assertNotIn("GIT_CONFIG_GLOBAL", os.environ)

    def test_sets_no_identity(self):
        with tempfile.TemporaryDirectory() as tmp, testing.hermetic_git(tmp):
            self.assertFalse([k for k in os.environ if k.startswith(("GIT_AUTHOR_", "GIT_COMMITTER_"))])


class BareRemoteTest(unittest.TestCase):
    def test_work_tracks_a_bare_origin_on_main(self):
        with tempfile.TemporaryDirectory() as tmp, testing.hermetic_git(tmp):
            work, origin = testing.bare_remote(tmp)

            def g(*args):
                return subprocess.run(["git", *args], cwd=work, check=True, capture_output=True, text=True).stdout.strip()

            self.assertEqual(g("symbolic-ref", "--short", "HEAD"), "main")
            self.assertEqual(g("rev-parse", "--abbrev-ref", "@{upstream}"), "origin/main")
            self.assertEqual(g("config", "user.email"), "test@example.com")
            self.assertEqual(g("config", "commit.gpgsign"), "false")
            self.assertEqual(g("rev-list", "--count", "HEAD"), "1")
            self.assertTrue((origin / "HEAD").is_file())


class CleanRunEnvTest(unittest.TestCase):
    def test_strips_protocol_variables_but_keeps_the_locator(self):
        env = {"RUN_MARKERS": "1", "RUN_ARTIFACTS": "/a", "JUNEAU_RUN_CONSOLE": "condensed", "JUNEAU_RUN_PY": "/x.py"}
        with mock.patch.dict(os.environ, env):
            saved = sys.modules.get("juneau_run")
            with testing.clean_run_env():
                self.assertNotIn("RUN_MARKERS", os.environ)
                self.assertNotIn("RUN_ARTIFACTS", os.environ)
                self.assertNotIn("JUNEAU_RUN_CONSOLE", os.environ)
                self.assertEqual(os.environ["JUNEAU_RUN_PY"], "/x.py")
                self.assertNotIn("juneau_run", sys.modules)
            self.assertEqual(os.environ["RUN_MARKERS"], "1")
            self.assertIs(sys.modules.get("juneau_run"), saved)


class FakeToolTest(unittest.TestCase):
    def test_logs_argv_prints_and_exits_with_the_code(self):
        with tempfile.TemporaryDirectory() as tmp:
            log = Path(tmp) / "log.jsonl"
            tool = testing.fake_tool(Path(tmp) / "bin", "mvn", exit_env="FAKE_EXIT", log_env="FAKE_LOG", stdout="hi\n")
            env = {**os.environ, "FAKE_EXIT": "3", "FAKE_LOG": str(log)}
            r = subprocess.run([str(tool), "-q", "verify"], env=env, capture_output=True, text=True)
            self.assertEqual(r.returncode, 3)
            self.assertEqual(r.stdout, "hi\n")
            self.assertEqual(json.loads(log.read_text().splitlines()[0]), ["mvn", "-q", "verify"])

    def test_exits_0_without_the_variable(self):
        with tempfile.TemporaryDirectory() as tmp:
            tool = testing.fake_tool(Path(tmp), "npm", exit_env="FAKE_EXIT")
            env = {k: v for k, v in os.environ.items() if k != "FAKE_EXIT"}
            self.assertEqual(subprocess.run([str(tool)], env=env).returncode, 0)


if __name__ == "__main__":
    unittest.main()
