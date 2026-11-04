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
"""juneau_terminal_cli.py, the `juneau-terminal` console script: routing, the pane preflight and the path lookups, with
bootstrap and the engine mocked."""

import contextlib
import io
import os
import sys
import unittest
from pathlib import Path
from unittest import mock

import scripts_support  # noqa: F401 - puts src/main/python on sys.path
import support
import bootstrap
import juneau_terminal_cli as cli


def run(*argv, env=None):
    out, err = io.StringIO(), io.StringIO()
    with mock.patch.dict(os.environ, env or {}, clear=False), contextlib.redirect_stdout(out), \
            contextlib.redirect_stderr(err):
        code = cli.main(list(argv))
    return code, out.getvalue(), err.getvalue()


class RoutingTest(unittest.TestCase):
    def test_version_from_a_source_tree(self):
        with mock.patch.object(cli, "build_version", return_value=None):
            self.assertEqual(run("--version")[:2], (0, "juneau-terminal unknown (source tree) (terminal-protocol v1)\n"))

    def test_version_when_packaged(self):
        with mock.patch.object(cli, "build_version", return_value="10.0.0"):
            self.assertEqual(run("--version")[1], "juneau-terminal 10.0.0 (terminal-protocol v1)\n")

    def test_windows_is_refused(self):
        with mock.patch.object(cli.os, "name", "nt"):
            self.assertEqual(run("--version"), (2, "", "juneau-terminal: POSIX-only (needs fcntl)\n"))

    def test_no_arguments_and_unknown_command(self):
        self.assertEqual(run(), (2, "", cli.USAGE + "\n"))
        self.assertEqual(run("bogus"), (2, "", "juneau-terminal: unknown command 'bogus'\n" + cli.USAGE + "\n"))

    def test_bin_goes_to_bootstrap_unchanged(self):
        with mock.patch.object(bootstrap, "main", return_value=0) as m:
            self.assertEqual(run("--bin", "watch-run.sh", "--version", "1.2")[0], 0)
        m.assert_called_once_with(["--bin", "watch-run.sh", "--version", "1.2"])

    def test_bin_after_a_subcommand_is_the_subcommand_s(self):
        with mock.patch.object(bootstrap, "main") as m, mock.patch("juneau_terminal.main", return_value=0) as engine:
            self.assertEqual(run("run", "--bin", "x")[0], 0)
        m.assert_not_called()
        engine.assert_called_once_with(["--bin", "x"])

    def test_run_returns_the_engine_s_code(self):
        with mock.patch("juneau_terminal.main", return_value=3) as engine:
            self.assertEqual(run("run", "--", "mvn", "test")[0], 3)
        engine.assert_called_once_with(["--", "mvn", "test"])


class PaneTest(unittest.TestCase):
    def test_no_tmux_first(self):
        env = {k: v for k, v in os.environ.items() if k != "TMUX"}
        with mock.patch.dict(os.environ, env, clear=True), mock.patch.object(cli.shutil, "which", return_value=None):
            code, _, err = run("watch", "--", "true")
        self.assertEqual(code, 2)
        self.assertEqual(err, "watch-run.sh: not inside tmux\n"
                              "juneau-terminal: use 'juneau-terminal run' to run a tool without a pane\n")

    def test_first_missing_tool(self):
        with mock.patch.object(cli.shutil, "which", side_effect=lambda t: None if t in ("ps", "perl") else "/bin/" + t):
            code, _, err = run("pane", "list", env={"TMUX": "/tmp/t,1,0"})
        self.assertEqual((code, err), (2, "juneau-terminal: tmux-pane.sh needs ps, which is not on the PATH\n"))

    def test_exec_bash_on_the_script(self):
        with mock.patch.object(cli.shutil, "which", return_value="/bin/x"), \
                mock.patch.object(cli, "build_version", return_value="10.0.0"), \
                mock.patch.object(bootstrap, "cli_bin", return_value=Path("/c/watch-run.sh")) as found, \
                mock.patch.object(cli.os, "execvp") as execvp:
            run("watch", "-t", "push", "--", "true", env={"TMUX": "/tmp/t,1,0"})
        found.assert_called_once_with("watch-run.sh", version="10.0.0")
        execvp.assert_called_once_with("bash", ["bash", "/c/watch-run.sh", "-t", "push", "--", "true"])

    def test_lookup_failure_is_exit_1(self):
        with mock.patch.object(cli.shutil, "which", return_value="/bin/x"), \
                mock.patch.object(bootstrap, "cli_bin", side_effect=bootstrap.BootstrapError("no toolkit")):
            self.assertEqual(run("watch", env={"TMUX": "x"})[::2], (1, "juneau-terminal: no toolkit\n"))


class PathTest(unittest.TestCase):
    def test_bin(self):
        with mock.patch.object(cli, "build_version", return_value="10.0.0"), \
                mock.patch.object(bootstrap, "cli_bin", return_value=Path("/c/tmux-pane.sh")) as found:
            self.assertEqual(run("path", "bin", "tmux-pane.sh")[:2], (0, "/c/tmux-pane.sh\n"))
        found.assert_called_once_with("tmux-pane.sh", pom=None, cache_dir=None, version="10.0.0")

    def test_bin_toolkit_version_wins(self):
        with mock.patch.object(cli, "build_version", return_value="10.0.0"), \
                mock.patch.object(bootstrap, "cli_bin", return_value=Path("/c/x")) as found:
            run("path", "bin", "x", "--toolkit-version", "9.9", "--pom", "p.xml", "--cache-dir", "c")
        found.assert_called_once_with("x", pom="p.xml", cache_dir="c", version="9.9")

    def test_reporter_is_kept_under_the_cache(self):
        with mock.patch("juneau_terminal.find_playwright_reporter", return_value="/k/r.cjs") as find:
            self.assertEqual(run("path", "reporter")[:2], (0, "/k/r.cjs\n"))
        find.assert_called_once_with(extract_dir=cli.reporter_cache())
        self.assertEqual(cli.reporter_cache(), Path.home() / ".cache" / "juneau-terminal" / "reporter")

    def test_plugin_not_found(self):
        with mock.patch("juneau_terminal.find_pytest_plugin", return_value=None):
            code, _, err = run("path", "plugin")
        self.assertEqual(code, 1)
        self.assertTrue(err.startswith("juneau-terminal: no plugin found beside "), err)

    def test_source_tree_plugin(self):
        # Other suites swap sys.modules["juneau_terminal"] for fakes, so pin the source-tree engine here.
        with mock.patch.dict(sys.modules, {"juneau_terminal": support.jr}):
            code, out, _ = run("path", "plugin")
        self.assertEqual((code, out), (0, str(scripts_support.PYTHON_SRC / "pytest_plugin") + "\n"))


if __name__ == "__main__":
    unittest.main()
