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
"""juneau_scripts.tools against fake mvn, npm, npx, gradle, gradlew and uv on PATH."""

import io
import json
import os
import sys
import tempfile
import time
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from unittest import mock

import scripts_support
from scripts_support import jr, reset_run_state

from juneau_scripts import run, testing, tools

TOOLS = ("mvn", "npm", "npx", "gradle", "uv")


class ToolsCase(unittest.TestCase):
    def setUp(self):
        cm = testing.clean_run_env()
        cm.__enter__()
        self.addCleanup(cm.__exit__, None, None, None)
        run._n = 0
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.tmp = Path(tmp.name)
        self.bin = self.tmp / "bin"
        self.log = self.tmp / "argv.jsonl"
        for name in TOOLS:
            testing.fake_tool(self.bin, name, exit_env="FAKE_EXIT", log_env="FAKE_LOG")
        env = mock.patch.dict(os.environ, {"PATH": str(self.bin), "FAKE_LOG": str(self.log),
                                           "PLAYWRIGHT_BROWSERS_PATH": str(self.tmp / "browsers")})
        env.start()
        self.addCleanup(env.stop)
        os.environ.pop("FAKE_EXIT", None)
        self.work = self.tmp / "work"
        self.work.mkdir()

    def calls(self):
        if not self.log.exists():
            return []
        return [json.loads(line) for line in self.log.read_text(encoding="utf-8").splitlines()]


class MavenTest(ToolsCase):
    def test_argv_and_exit_code(self):
        r = tools.maven(["-q", "verify"], cwd=self.work)
        self.assertEqual((r.ok, r.code), (True, 0))
        self.assertEqual(self.calls(), [["mvn", "-q", "verify"]])
        os.environ["FAKE_EXIT"] = "1"
        self.assertEqual(tools.maven(["verify"], cwd=self.work).code, 1)

    def test_java_home_goes_first_on_path(self):
        jh = self.tmp / "jdk"
        testing.fake_tool(jh / "bin", "mvn", log_env="JH_LOG")
        with mock.patch.dict(os.environ, {"JH_LOG": str(self.tmp / "jh.jsonl")}), \
                mock.patch.object(run, "cmd", wraps=run.cmd) as cmd:
            tools.maven(["verify"], cwd=self.work, java_home=jh)
        self.assertTrue((self.tmp / "jh.jsonl").exists())
        self.assertEqual(self.calls(), [])
        env = cmd.call_args.kwargs["env"]
        self.assertEqual(env["JAVA_HOME"], str(jh))
        self.assertTrue(env["PATH"].startswith(str(jh / "bin") + os.pathsep))

    def test_title_defaults_to_the_command_and_parser_to_maven(self):
        with mock.patch.object(run, "cmd", return_value=run.Result(True, 0)) as cmd:
            tools.maven(["-q", "verify"])
            tools.maven(["verify"], title="Build", parser="generic")
        self.assertEqual(cmd.call_args_list[0].kwargs["title"], "mvn -q verify")
        self.assertEqual(cmd.call_args_list[0].kwargs["tool"], "maven")
        self.assertEqual((cmd.call_args_list[1].kwargs["title"], cmd.call_args_list[1].kwargs["tool"]),
                         ("Build", "generic"))


class PytestTest(ToolsCase):
    def test_launcher_order(self):
        self.assertEqual(tools.pytest_cmd(self.work), list(tools.UV_PYTEST))
        (self.bin / "uv").unlink()
        self.assertEqual(tools.pytest_cmd(self.work), [sys.executable, "-m", "pytest"])
        venv_py = self.work / ".venv" / "bin" / "python"
        venv_py.parent.mkdir(parents=True)
        venv_py.write_text("")
        self.assertEqual(tools.pytest_cmd(self.work), [str(venv_py), "-m", "pytest"])

    def test_venv_then_dot_venv_names(self):
        venv_py = self.work / "venv" / "bin" / "python"
        venv_py.parent.mkdir(parents=True)
        venv_py.write_text("")
        self.assertEqual(tools.pytest_cmd(self.work, launcher="venv"), [str(venv_py), "-m", "pytest"])

    def test_explicit_launchers(self):
        self.assertEqual(tools.pytest_cmd(self.work, launcher="uv"), list(tools.UV_PYTEST))
        self.assertEqual(tools.pytest_cmd(self.work, launcher="python"), [sys.executable, "-m", "pytest"])
        with self.assertRaisesRegex(ValueError, "no .venv or venv"):
            tools.pytest_cmd(self.work, launcher="venv")
        with self.assertRaisesRegex(ValueError, "launcher"):
            tools.pytest_cmd(self.work, launcher="conda")

    def test_pytest_runs_with_a_standalone_env(self):
        with mock.patch.dict(os.environ, {"JUNEAU_PUSH_ACTIVE": "1", "JUNEAU_RUN_N_BASE": "3"}):
            r = tools.pytest(["-q", "tests"], repo=self.work)
        self.assertTrue(r.ok)
        self.assertEqual(self.calls(), [["uv", "run", "--native-tls", "--with", "pytest", "pytest", "-q", "tests"]])
        with mock.patch.object(run, "cmd", return_value=run.Result(True, 0)) as cmd, \
                mock.patch.dict(os.environ, {"JUNEAU_PUSH_ACTIVE": "1"}):
            tools.pytest(["tests"], repo=self.work)
        self.assertNotIn("JUNEAU_PUSH_ACTIVE", cmd.call_args.kwargs["env"])
        self.assertEqual(cmd.call_args.kwargs["tool"], "pytest")


class PlaywrightTest(ToolsCase):
    def deps(self, *, lock_age=None):
        probe = self.work / "node_modules" / "@playwright" / "test"
        probe.mkdir(parents=True)
        if lock_age is not None:
            lock = self.work / "package-lock.json"
            lock.write_text("{}")
            now = time.time()
            os.utime(probe, (now, now))
            os.utime(lock, (now + lock_age, now + lock_age))

    def chromium(self):
        marker = self.tmp / "browsers" / "chromium-1234" / "INSTALLATION_COMPLETE"
        marker.parent.mkdir(parents=True)
        marker.write_text("")

    def test_fresh_deps_and_browser_start_no_subprocess(self):
        self.deps(lock_age=-60)
        self.chromium()
        r = tools.playwright_ensure(self.work)
        self.assertTrue(r.ok)
        self.assertEqual(self.calls(), [])

    def test_stale_deps_run_npm_ci(self):
        self.deps(lock_age=60)
        self.chromium()
        tools.playwright_ensure(self.work)
        self.assertEqual(self.calls(), [["npm", "ci"]])

    def test_no_lockfile_runs_npm_install(self):
        self.chromium()
        tools.playwright_ensure(self.work)
        self.assertEqual(self.calls(), [["npm", "install"]])

    def test_missing_chromium_installs_it(self):
        self.deps()
        tools.playwright_ensure(self.work)
        self.assertEqual(self.calls(), [["npx", "--yes", "playwright", "install", "chromium"]])

    def test_failed_install_stops_before_the_browser(self):
        os.environ["FAKE_EXIT"] = "1"
        r = tools.playwright_ensure(self.work)
        self.assertEqual((r.ok, r.code), (False, 1))
        self.assertEqual(self.calls(), [["npm", "install"]])

    def test_default_browser_cache(self):
        with mock.patch.dict(os.environ, {"PLAYWRIGHT_BROWSERS_PATH": ""}), \
                mock.patch.object(tools.sys, "platform", "darwin"):
            self.assertEqual(tools.browsers_root(), Path.home() / "Library" / "Caches" / "ms-playwright")
        with mock.patch.dict(os.environ, {"PLAYWRIGHT_BROWSERS_PATH": ""}), \
                mock.patch.object(tools.sys, "platform", "linux"):
            self.assertEqual(tools.browsers_root(), Path.home() / ".cache" / "ms-playwright")

    def test_playwright_runs_npx_playwright_test(self):
        tools.playwright(["--grep", "smoke"], cwd=self.work)
        self.assertEqual(self.calls(), [["npx", "playwright", "test", "--grep", "smoke"]])


class NpmTest(ToolsCase):
    def test_npm(self):
        tools.npm(["run", "build"], cwd=self.work)
        self.assertEqual(self.calls(), [["npm", "run", "build"]])

    def test_ensure_install(self):
        tools.npm(["run", "build"], cwd=self.work, ensure_install=True)
        self.assertEqual(self.calls(), [["npm", "install"], ["npm", "run", "build"]])
        self.log.unlink()
        (self.work / "node_modules").mkdir()
        tools.npm(["run", "build"], cwd=self.work, ensure_install=True)
        self.assertEqual(self.calls(), [["npm", "run", "build"]])


class GradleTest(ToolsCase):
    def test_gradle_on_path_then_gradlew(self):
        tools.gradle(["build"], cwd=self.work)
        testing.fake_tool(self.work, "gradlew", log_env="FAKE_LOG")
        tools.gradle(["build"], cwd=self.work)
        self.assertEqual(self.calls(), [["gradle", "build"], ["gradlew", "build"]])

    def test_markers_add_plain_console(self):
        reset_run_state()
        self.addCleanup(reset_run_state)
        sys.modules["juneau_run"] = jr
        os.environ["RUN_MARKERS"] = "1"
        with redirect_stdout(io.StringIO()):
            tools.gradle(["build"], cwd=self.work)
        self.assertEqual(self.calls(), [["gradle", "--console=plain", "build"]])


class BazelToolTest(unittest.TestCase):
    def test_bazel_runs_through_run_cmd_with_the_bazel_parser(self):
        with mock.patch.object(run, "cmd", return_value="r") as c:
            self.assertEqual(tools.bazel(["test", "//..."], cwd="w", env={"A": "1"}), "r")
        c.assert_called_once_with(["bazel", "test", "//..."], title="bazel test //...", tool="bazel", cwd="w",
                                  env={"A": "1"})

    def test_a_title_can_be_given(self):
        with mock.patch.object(run, "cmd") as c:
            tools.bazel(["build", "//a"], title="Build a")
        self.assertEqual(c.call_args.kwargs["title"], "Build a")
        self.assertEqual(c.call_args.kwargs["cwd"], ".")


if __name__ == "__main__":
    unittest.main()
