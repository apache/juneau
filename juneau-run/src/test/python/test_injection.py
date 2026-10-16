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
"""PytestParser.prepare: injection of the juneau_run_pytest plugin (spec 1.4)."""

import os
import shutil
import subprocess
import sys
import tempfile
import types
import unittest
from pathlib import Path
from unittest import mock

import support
from support import jr


class InjectionTest(unittest.TestCase):
    def setUp(self):
        support.isolate_run_env(self)
        self.parser = jr.PytestParser()
        self.parser.step_id = "s1"

    def prep(self, cmd, env=None):
        with mock.patch.object(jr, "find_pytest_plugin", return_value="/plug/dir"):
            return self.parser.prepare(cmd, env, None)

    def test_injected_after_the_pytest_token(self):
        cmd, env = self.prep(["pytest", "tests"])
        self.assertIn("-p", cmd)
        self.assertEqual(cmd[cmd.index("-p") + 1], "juneau_run_pytest")
        self.assertTrue(env["PYTHONPATH"].startswith("/plug/dir"))

    def test_injected_before_double_dash(self):
        cmd, _ = self.prep(["pytest", "--", "x.py"])
        self.assertLess(cmd.index("juneau_run_pytest"), cmd.index("--"))

    def test_uv_run_double_dash_pytest_gets_args_after_the_pytest_token(self):
        with mock.patch.dict(os.environ, {"RUN_MARKERS": "1", "RUN_ARTIFACTS": "/art"}):
            cmd, _ = self.prep(["uv", "run", "--", "pytest", "tests"])
        at = cmd.index("pytest")
        self.assertEqual(cmd[at + 1:at + 3], ["-p", "juneau_run_pytest"])
        self.assertEqual(cmd[at + 3], "--junitxml=/art/s1-pytest.xml")
        self.assertEqual(cmd[-1], "tests")
        self.assertEqual(cmd.index("--"), 2)

    def test_pytest_double_dash_args_follow_the_pytest_token(self):
        cmd, _ = self.prep(["pytest", "--", "x.py"])
        self.assertEqual(cmd[:3], ["pytest", "-p", "juneau_run_pytest"])
        self.assertEqual(cmd[-2:], ["--", "x.py"])

    def test_module_form_inserts_after_pytest(self):
        cmd, _ = self.prep(["python3", "-m", "pytest", "-q"])
        self.assertEqual(cmd[:5], ["python3", "-m", "pytest", "-p", "juneau_run_pytest"])

    def test_is_pytest_command(self):
        for cmd in (["pytest"], ["python", "-m", "pytest"], ["uv", "run", "pytest"],
                    ["uv", "run", "--with", "x", "pytest"], ["uv", "run", "--", "pytest"], ["/v/bin/py.test", "t"]):
            self.assertTrue(jr._is_pytest_command(cmd), cmd)
        for cmd in (["tox", "-e", "pytest"], ["make", "pytest"], ["uv", "run", "--with", "pytest", "python", "-m", "foo"],
                    ["uv", "run", "--with=pytest", "foo"], ["echo", "pytest"]):
            self.assertFalse(jr._is_pytest_command(cmd), cmd)

    def test_opt_out_glued_form_and_junit_aliases(self):
        self.assertNotIn("juneau_run_pytest", self.prep(["pytest", "-pno:juneau_run_pytest"])[0])
        self.prep(["pytest", "--junit-xml=/t/a.xml"])
        self.assertEqual(self.parser.report_path, "/t/a.xml")
        self.parser = jr.PytestParser(); self.parser.step_id = "s1"
        self.prep(["pytest", "--junitxml", "/t/b.xml"])
        self.assertEqual(self.parser.report_path, "/t/b.xml")

    def test_junitxml_also_lands_before_double_dash(self):
        with mock.patch.dict(os.environ, {"RUN_MARKERS": "1", "RUN_ARTIFACTS": "/art"}):
            cmd, _ = self.prep(["pytest", "--", "x.py"])
        self.assertLess(cmd.index("--junitxml=/art/s1-pytest.xml"), cmd.index("--"))
        self.assertEqual(cmd[-1], "x.py")

    def test_module_form_and_py_test(self):
        for cmd in (["python3", "-m", "pytest"], ["py.test"], ["/venv/bin/pytest"]):
            self.assertIn("juneau_run_pytest", self.prep(cmd)[0])

    def test_pythonpath_prepended_to_existing(self):
        _, env = self.prep(["pytest"], {"PYTHONPATH": "/old"})
        self.assertEqual(env["PYTHONPATH"], "/plug/dir" + os.pathsep + "/old")

    def test_not_injected_for_wrappers_or_opt_outs(self):
        self.assertNotIn("juneau_run_pytest", self.prep(["make", "test"])[0])
        self.assertNotIn("juneau_run_pytest", self.prep(["pytest", "-p", "no:juneau_run_pytest"])[0])
        os.environ["JUNEAU_RUN_PYTEST_PLUGIN"] = "0"
        self.assertNotIn("juneau_run_pytest", self.prep(["pytest"])[0])

    def test_injected_when_junitxml_already_present(self):
        cmd, _ = self.prep(["pytest", "--junitxml=/tmp/r.xml"])
        self.assertIn("juneau_run_pytest", cmd)
        self.assertEqual(self.parser.report_path, "/tmp/r.xml")

    def test_missing_plugin_means_no_injection(self):
        with mock.patch.object(jr, "find_pytest_plugin", return_value=None):
            cmd, env = self.parser.prepare(["pytest"], None, None)
        self.assertNotIn("juneau_run_pytest", cmd)

    def test_finds_plugin_beside_the_engine_and_in_a_zip(self):
        self.assertEqual(jr.find_pytest_plugin(), str(support.SCRIPT.parent / "pytest_plugin"))      # source-tree route
        # zip-root route: a fake juneau_scripts module whose __file__ lives inside a .zip
        with mock.patch.object(jr, "_engine_dir", return_value=Path("/nowhere")), \
             mock.patch.dict(sys.modules, {"juneau_scripts": types.SimpleNamespace(__file__="/x/y.zip/juneau_scripts/__init__.py")}):
            self.assertEqual(jr.find_pytest_plugin(), "/x/y.zip" + os.sep + "pytest_plugin")

    def _import_error(self, where, name):
        """True if `name` imports in a fresh interpreter whose PYTHONPATH is exactly `where`, else its stderr."""
        with tempfile.TemporaryDirectory() as empty:        # python -c puts the cwd on sys.path, so use an empty one
            p = subprocess.run([sys.executable, "-B", "-c", f"import {name}"], env={**os.environ, "PYTHONPATH": where},
                               cwd=empty, capture_output=True, text=True)
        return p.returncode == 0 or p.stderr

    def _assert_plugin_only(self, where):
        self.assertIs(self._import_error(where, "juneau_run_pytest"), True)
        for name in ("bootstrap", "juneau_run", "juneau_scripts"):
            err = self._import_error(where, name)
            self.assertIn(f"No module named '{name}'", str(err), f"{name} must not be importable from {where}: {err}")

    def test_plugin_path_exposes_only_the_plugin(self):
        with mock.patch.dict(os.environ):
            os.environ.pop("PYTHONPATH", None)               # prepare() prepends to an inherited value
            _, env = self.parser.prepare(["pytest"], None, None)
        self.assertEqual(env["PYTHONPATH"], jr.find_pytest_plugin())
        self.assertEqual([p.name for p in Path(env["PYTHONPATH"]).iterdir() if p.name != "__pycache__"],
                         ["juneau_run_pytest.py"])
        self._assert_plugin_only(env["PYTHONPATH"])

    def test_zip_plugin_path_exposes_only_the_plugin(self):
        from scripts_support import build_zip
        tmp = tempfile.mkdtemp()
        self.addCleanup(shutil.rmtree, tmp, ignore_errors=True)
        zpath = build_zip(Path(tmp) / "t.zip")
        with mock.patch.object(jr, "_engine_dir", return_value=Path("/nowhere")), \
             mock.patch.dict(sys.modules, {"juneau_scripts": types.SimpleNamespace(__file__=f"{zpath}/juneau_scripts/__init__.py")}):
            where = jr.find_pytest_plugin()
        self.assertEqual(where, f"{zpath}{os.sep}pytest_plugin")
        self._assert_plugin_only(where)

    @unittest.skipUnless(shutil.which("uv"), "uv not installed")
    def test_real_default_launcher_accepts_the_plugin(self):
        tmp = tempfile.mkdtemp()
        self.addCleanup(shutil.rmtree, tmp, ignore_errors=True)
        Path(tmp, "test_a.py").write_text("def test_a(): pass\n")
        parser = jr.PytestParser()
        parser.step_id = "s"
        cmd, env = parser.prepare(
            ["uv", "run", "--native-tls", "--with", "pytest", "pytest", "-p", "no:cacheprovider"], None, tmp)
        p = subprocess.run(cmd, cwd=tmp, env={**os.environ, **(env or {})}, capture_output=True, text=True)
        self.assertEqual(p.returncode, 0, p.stdout + p.stderr)
        self.assertIn('##pytest {"ev":"total"', p.stdout, p.stdout + p.stderr)


if __name__ == "__main__":
    unittest.main()
