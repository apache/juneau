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
"""The lookups the convenience packages need: the engine finds the plugin and reporter under the wheel's and the .pyz's
namespaced directory names, `path reporter` keeps its extracted copy, bootstrap names a .pyz's cache directory by its
version, and the pytest plugin stays silent unless the run asks for it."""

import importlib.util
import os
import shutil
import subprocess
import sys
import tempfile
import types
import unittest
import zipfile
from pathlib import Path
from unittest import mock

import support
from support import jr
from scripts_support import BOOTSTRAP, PYTHON_SRC

REPORTER = support.MODULE_ROOT / "src" / "main" / "js" / "playwright_reporter" / "juneau_terminal_playwright.cjs"
PLUGIN_DIR = PYTHON_SRC / "pytest_plugin"
HAVE_PYTEST = importlib.util.find_spec("pytest") is not None


def fake_pyz(path):
    """An archive with the .pyz's directory names (no pytest_plugin/, no playwright_reporter/)."""
    with zipfile.ZipFile(path, "w") as z:
        z.writestr("juneau_terminal_scripts/__init__.py", "")
        z.writestr("juneau_terminal_pytest_plugin/juneau_terminal_pytest.py", "")
        z.write(REPORTER, "juneau_terminal_playwright_reporter/juneau_terminal_playwright.cjs")
    return path


class ArchiveLookupTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp(prefix="jt-lookup-"))
        self.addCleanup(shutil.rmtree, self.tmp, ignore_errors=True)
        self.pyz = fake_pyz(self.tmp / "juneau-terminal-1.2.3.pyz")

    def inside(self, archive):
        return mock.patch.dict(sys.modules, {"juneau_terminal_scripts": types.SimpleNamespace(
            __file__=f"{archive}/juneau_terminal_scripts/__init__.py")})

    def test_archive_of(self):
        self.assertEqual(jr._archive_of("/a/t.pyz/juneau_terminal_scripts/x.py"), "/a/t.pyz")
        self.assertEqual(jr._archive_of("/a/t.zip/b.pyz/x.py"), "/a/t.zip")       # whichever suffix comes first
        self.assertIsNone(jr._archive_of("/a/zipped/x.py"))

    def test_plugin_in_a_pyz(self):
        with mock.patch.object(jr, "_engine_dir", return_value=Path("/nowhere")), self.inside(self.pyz):
            self.assertEqual(jr.find_pytest_plugin(), f"{self.pyz}{os.sep}juneau_terminal_pytest_plugin")

    def test_unreadable_archive_assumes_the_zip_layout(self):
        with mock.patch.object(jr, "_engine_dir", return_value=Path("/nowhere")), self.inside("/x/y.pyz"):
            self.assertEqual(jr.find_pytest_plugin(), "/x/y.pyz" + os.sep + "pytest_plugin")

    def test_installed_wheel_layout(self):
        site = self.tmp / "site"
        (site / "juneau_terminal_pytest_plugin").mkdir(parents=True)
        (site / "juneau_terminal_pytest_plugin" / "juneau_terminal_pytest.py").write_text("")
        (site / "juneau_terminal_playwright_reporter").mkdir()
        shutil.copy(REPORTER, site / "juneau_terminal_playwright_reporter")
        with mock.patch.object(jr, "_engine_dir", return_value=site):
            self.assertEqual(jr.find_pytest_plugin(), str(site / "juneau_terminal_pytest_plugin"))
            self.assertEqual(jr.find_playwright_reporter(),
                             str(site / "juneau_terminal_playwright_reporter" / "juneau_terminal_playwright.cjs"))

    def test_reporter_kept_under_extract_dir(self):
        keep = self.tmp / "keep"
        with mock.patch.object(jr, "_engine_dir", return_value=Path("/nowhere")), self.inside(self.pyz), \
                mock.patch.object(jr, "_playwright_extracted", None), \
                mock.patch.object(jr.atexit, "register") as register:
            first = Path(jr.find_playwright_reporter(extract_dir=keep))
            os.utime(first, (1, 1))
            second = Path(jr.find_playwright_reporter(extract_dir=keep))
        register.assert_not_called()                                   # kept, not removed at exit
        self.assertEqual(first, second)
        self.assertEqual(first.parent.parent, keep)
        self.assertEqual(len(first.parent.name), 12)
        self.assertEqual(first.read_bytes(), REPORTER.read_bytes())
        self.assertEqual(first.stat().st_mtime, 1)                    # same bytes: not rewritten
        self.assertEqual(first.stat().st_mode & 0o777, 0o644)


class BootstrapNamingTest(unittest.TestCase):
    def setUp(self):
        spec = importlib.util.spec_from_file_location("_bs_naming", BOOTSTRAP)
        self.bs = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(self.bs)

    def test_pyz_and_apache_names_carry_the_version(self):
        for name in ("juneau-terminal-10.0.0.zip", "juneau-terminal-10.0.0.pyz", "apache-juneau-terminal-10.0.0.pyz"):
            self.assertEqual(self.bs._bin_version(Path("/d") / name, None), ("10.0.0", None), name)

    def test_cli_bin_defaults_and_passes_through(self):
        with mock.patch.object(self.bs, "_cli_source", return_value="/src") as source, \
                mock.patch.object(self.bs, "bin_path", return_value=Path("/b/x")) as bin_path:
            self.assertEqual(self.bs.cli_bin("x", version="1.0"), Path("/b/x"))
        pom = source.call_args.args[0]
        self.assertTrue(pom.endswith("pom.xml"))
        self.assertEqual(source.call_args.args[1], str(Path(pom).resolve().parent / "target" / "juneau-terminal"))
        bin_path.assert_called_once_with("x", source="/src", version="1.0")


GATE = """
import sys, pytest, juneau_terminal_pytest
sys.exit(pytest.main(["-q", "-p", "no:cacheprovider", *sys.argv[1:]], plugins=[juneau_terminal_pytest]))
"""


@unittest.skipUnless(HAVE_PYTEST, "pytest not installed")
class PluginGateTest(unittest.TestCase):
    """Registered the way an installed pytest11 entry point registers it (not named with -p), the plugin is silent."""

    def lines(self, *args, **env):
        with tempfile.TemporaryDirectory() as tmp:
            Path(tmp, "test_a.py").write_text("def test_a():\n    pass\n")
            base = {k: v for k, v in os.environ.items()
                    if k not in ("JUNEAU_TERMINAL_PYTEST_STREAM", "PYTEST_PLUGINS", "PYTEST_ADDOPTS")}
            p = subprocess.run([sys.executable, "-B", "-c", GATE, *args], cwd=tmp, capture_output=True, text=True,
                               env={**base, "PYTHONPATH": str(PLUGIN_DIR), "PYTHONDONTWRITEBYTECODE": "1", **env})
        self.assertIn(p.returncode, (0,), p.stdout + p.stderr)
        return [l for l in p.stdout.splitlines() if l.startswith("##pytest ")]

    def test_silent_unless_asked(self):
        self.assertEqual(self.lines(), [])

    def test_stream_env(self):
        self.assertEqual(self.lines(JUNEAU_TERMINAL_PYTEST_STREAM="1")[0], '##pytest {"ev":"hello","v":1}')

    def test_named_with_p(self):
        self.assertEqual(self.lines("-p", "juneau_terminal_pytest")[0], '##pytest {"ev":"hello","v":1}')

    def test_named_in_addopts_and_pytest_plugins(self):
        self.assertTrue(self.lines(PYTEST_ADDOPTS="-p juneau_terminal_pytest"))
        self.assertTrue(self.lines(PYTEST_PLUGINS="juneau_terminal_pytest"))


if __name__ == "__main__":
    unittest.main()
