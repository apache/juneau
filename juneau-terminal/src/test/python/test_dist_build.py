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
"""src/dist/build_dist.py: the wheel, sdist, .pyz and npm tarball are reproducible, hold the namespaced layout, and work
once installed.  Tests that need uv, npm, ruby or twine skip without them (dist_support.need)."""

import gzip
import importlib.util
import io
import json
import os
import shutil
import subprocess
import sys
import tarfile
import tempfile
import unittest
import zipfile
from pathlib import Path

import dist_support
from dist_support import BUILDER, DIST, build, need

spec = importlib.util.spec_from_file_location("build_dist", BUILDER)
bd = importlib.util.module_from_spec(spec)
spec.loader.exec_module(bd)

WHEEL = "apache_juneau_terminal-10.0.0.dev0-py3-none-any.whl"
SDIST = "apache_juneau_terminal-10.0.0.dev0.tar.gz"
PYZ = "juneau-terminal-10.0.0-SNAPSHOT.pyz"
TGZ = "juneau-terminal-10.0.0-SNAPSHOT-playwright.tgz"
TOP = ["juneau_terminal.py", "juneau_terminal_bootstrap.py", "juneau_terminal_cli.py", "juneau_terminal_pytest.py",
       "juneau_terminal_pytest_plugin/juneau_terminal_pytest.py",
       "juneau_terminal_playwright_reporter/juneau_terminal_playwright.cjs"]


def sha(path):
    import hashlib
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


class Tmp(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp(prefix="jt-dist-"))
        self.addCleanup(shutil.rmtree, self.tmp, ignore_errors=True)


class VersionAndEpochTest(unittest.TestCase):
    def test_python_version(self):
        self.assertEqual(bd.python_version("10.0.0"), "10.0.0")
        self.assertEqual(bd.python_version("10.0.0-SNAPSHOT"), "10.0.0.dev0")
        self.assertEqual(bd.python_version("10.1.0-RC2"), "10.1.0rc2")
        with self.assertRaises(bd.BuildError) as cm:
            bd.python_version("10.0.0-M1")
        self.assertEqual(cm.exception.code, 2)

    def test_epoch(self):
        self.assertEqual(bd.parse_epoch("2026-01-09T19:00:20Z"), 1767985220)
        self.assertEqual(bd.parse_epoch("1767985220"), 1767985220)
        self.assertEqual(bd.parse_epoch("0"), bd.ZIP_FLOOR)       # a zip entry cannot predate 1980
        with self.assertRaises(bd.BuildError):
            bd.parse_epoch("yesterday")

    def test_usage_error_is_exit_2(self):
        p = subprocess.run([sys.executable, "-B", str(BUILDER), "--version", "1", "--epoch", "x", "--out", "/nonexistent"],
                           capture_output=True, text=True)
        self.assertEqual(p.returncode, 2, p.stderr)
        self.assertIn("--epoch 'x'", p.stderr)


class PyzAndTarballTest(Tmp):
    """No outside tools: the .pyz and the npm tarball are written by the builder itself."""

    def test_same_bytes_under_any_umask(self):
        outs = []
        for mask in ("022", "077"):
            out = self.tmp / mask
            p = subprocess.run(["sh", "-c", f'umask {mask}; exec "$@"', "sh", sys.executable, "-B", str(BUILDER),
                                "--version", "10.0.0-SNAPSHOT", "--epoch", dist_support.EPOCH, "--out", str(out),
                                "--only", "pyz", "--only", "tgz"], capture_output=True, text=True)
            self.assertEqual(p.returncode, 0, p.stderr)
            outs.append(out)
        for name in (PYZ, TGZ):
            self.assertEqual(sha(outs[0] / name), sha(outs[1] / name), name)

    def test_pyz_layout(self):
        self.assertEqual(build(self.tmp, "pyz").returncode, 0)
        data = (self.tmp / PYZ).read_bytes()
        self.assertTrue(data.startswith(b"#!/usr/bin/env python3\n"))
        self.assertTrue(os.access(self.tmp / PYZ, os.X_OK))
        with zipfile.ZipFile(self.tmp / PYZ) as z:
            infos = z.infolist()
        names = [i.filename for i in infos]
        self.assertEqual(names, sorted(names))
        for want in ["__main__.py", "LICENSE", "NOTICE", "juneau_terminal_scripts/_build_version.py",
                     "juneau_terminal_scripts/protocol.py", *TOP]:
            self.assertIn(want, names)
        self.assertNotIn("pyproject.toml", names)
        self.assertEqual({i.date_time for i in infos}, {(2026, 1, 9, 19, 0, 20)})
        for i in infos:
            self.assertEqual(i.external_attr >> 16, 0o100755 if i.filename.endswith(".sh") else 0o100644, i.filename)

    def test_pyz_runs(self):
        self.assertEqual(build(self.tmp, "pyz").returncode, 0)
        home = self.tmp / "home"
        home.mkdir()
        env = {**os.environ, "HOME": str(home), "PYTHONDONTWRITEBYTECODE": "1"}

        def pyz(*args):
            return subprocess.run([sys.executable, str(self.tmp / PYZ), *args], capture_output=True, text=True, env=env)
        self.assertEqual(pyz("--version").stdout, "juneau-terminal 10.0.0.dev0 (terminal-protocol v1)\n")
        script = Path(pyz("--bin", "watch-run.sh").stdout.strip())
        self.assertEqual(script, home / ".cache" / "juneau-terminal" / "bin" / "10.0.0-SNAPSHOT" / "watch-run.sh")
        self.assertTrue(os.access(script, os.X_OK))
        reporter = Path(pyz("path", "reporter").stdout.strip())
        self.assertEqual(reporter.parent.parent, home / ".cache" / "juneau-terminal" / "reporter")
        self.assertEqual(reporter.name, "juneau_terminal_playwright.cjs")
        self.assertEqual(pyz("path", "plugin").stdout.strip(), str(self.tmp / PYZ) + os.sep + "juneau_terminal_pytest_plugin")

    def test_tarball(self):
        self.assertEqual(build(self.tmp, "tgz").returncode, 0)
        raw = (self.tmp / TGZ).read_bytes()
        self.assertEqual(raw[4:8], b"\0\0\0\0")             # gzip header mtime 0
        with tarfile.open(fileobj=io.BytesIO(raw)) as tar:
            members = tar.getmembers()
            pkg = json.loads(tar.extractfile("package/package.json").read())
        self.assertEqual([m.name for m in members], ["package/LICENSE", "package/NOTICE",
                                                      "package/juneau_terminal_playwright.cjs", "package/package.json"])
        self.assertEqual({(m.uid, m.gid, m.mode, m.mtime) for m in members}, {(0, 0, 0o644, 1767985220)})
        self.assertEqual(pkg["name"], "@apache/juneau-terminal-playwright")
        self.assertEqual(pkg["version"], "10.0.0-SNAPSHOT")
        self.assertEqual(pkg["main"], "juneau_terminal_playwright.cjs")
        self.assertEqual(pkg["peerDependencies"], {"@playwright/test": ">=1.30.0"})


class BackendTest(Tmp):
    """The wheel and sdist go through hatchling: the pinned one on this python, or uv's."""

    def setUp(self):
        super().setUp()
        try:
            bd.backend_python(bd.hatchling_pin())
        except bd.BuildError:
            need("uv")
            raise

    def test_reproducible_wheel_and_sdist(self):
        a, b = self.tmp / "a", self.tmp / "b"
        for out in (a, b):
            p = build(out, "wheel", "sdist")
            self.assertEqual(p.returncode, 0, p.stderr)
        for name in (WHEEL, SDIST):
            self.assertEqual(sha(a / name), sha(b / name), name)
        self.assertEqual((a / "maven" / "juneau-terminal-10.0.0-SNAPSHOT.whl").read_bytes(), (a / WHEEL).read_bytes())
        self.assertEqual((a / "maven" / "juneau-terminal-10.0.0-SNAPSHOT-sdist.tar.gz").read_bytes(),
                         (a / SDIST).read_bytes())

    def test_wheel_metadata(self):
        self.assertEqual(build(self.tmp, "wheel").returncode, 0)
        with zipfile.ZipFile(self.tmp / WHEEL) as z:
            names = z.namelist()
            info = "apache_juneau_terminal-10.0.0.dev0.dist-info/"
            entry_points = z.read(info + "entry_points.txt").decode()
            metadata = z.read(info + "METADATA").decode()
        for want in [*TOP, "juneau_terminal_scripts/_build_version.py", "juneau_terminal_scripts/bin/watch-run.sh",
                     info + "licenses/LICENSE", info + "licenses/NOTICE"]:
            self.assertIn(want, names)
        self.assertIn("juneau-terminal = juneau_terminal_cli:main", entry_points)
        self.assertIn("[pytest11]\njuneau_terminal_pytest = juneau_terminal_pytest", entry_points)
        self.assertIn("Requires-Python: >=3.10", metadata)
        self.assertIn("License-Expression: Apache-2.0", metadata)

    def test_installed_wheel(self):
        uv = need("uv")
        self.assertEqual(build(self.tmp, "wheel").returncode, 0)
        venv = self.tmp / "venv"
        p = subprocess.run([uv, "venv", "-q", "--offline", "-p", "3.10", str(venv)], capture_output=True, text=True)
        if p.returncode != 0:
            need("python3.10-in-uv-cache")
        p = subprocess.run([uv, "pip", "install", "-q", "--offline", str(self.tmp / WHEEL), "pytest"],
                           capture_output=True, text=True, env={**os.environ, "VIRTUAL_ENV": str(venv)})
        self.assertEqual(p.returncode, 0, p.stderr)
        bin_dir = venv / "bin"
        env = {**os.environ, "HOME": str(self.tmp), "PYTHONDONTWRITEBYTECODE": "1"}
        env.pop("JUNEAU_TERMINAL_PYTEST_STREAM", None)
        env.pop("PYTEST_PLUGINS", None)
        env.pop("PYTEST_ADDOPTS", None)
        out = subprocess.run([str(bin_dir / "juneau-terminal"), "--version"], capture_output=True, text=True, env=env)
        self.assertEqual(out.stdout, "juneau-terminal 10.0.0.dev0 (terminal-protocol v1)\n")
        script = subprocess.run([str(bin_dir / "juneau-terminal"), "path", "bin", "watch-run.sh"], capture_output=True,
                                text=True, env=env).stdout.strip()
        self.assertTrue(os.access(script, os.X_OK), script)
        suite = self.tmp / "suite"
        suite.mkdir()
        (suite / "test_a.py").write_text("def test_a():\n    pass\n")

        def lines(*args, **extra):
            p = subprocess.run([str(bin_dir / "python"), "-B", "-m", "pytest", "-q", "-p", "no:cacheprovider", *args],
                               cwd=suite, capture_output=True, text=True, env={**env, **extra})
            return [l for l in p.stdout.splitlines() if l.startswith("##pytest ")]
        self.assertEqual(lines(), [])                                      # installed, not asked for: silent
        self.assertEqual(lines("-p", "juneau_terminal_pytest")[0], '##pytest {"ev":"hello","v":1}')
        self.assertEqual(len([l for l in lines("-p", "juneau_terminal_pytest") if '"hello"' in l]), 1)   # registered once
        self.assertTrue(lines(JUNEAU_TERMINAL_PYTEST_STREAM="1"))
        self.assertTrue(lines(PYTEST_ADDOPTS="-p juneau_terminal_pytest"))

    def test_python_310_floor(self):
        uv = need("uv")
        found = subprocess.run([uv, "python", "find", "--offline", "3.10"], capture_output=True, text=True).stdout.strip()
        if not found:
            need("python3.10-in-uv-cache")
        a, b = self.tmp / "a", self.tmp / "b"
        self.assertEqual(build(a, "pyz", "tgz", "wheel").returncode, 0)
        p = build(b, "pyz", "tgz", "wheel", python=found)
        self.assertEqual(p.returncode, 0, p.stderr)
        for name in (PYZ, TGZ, WHEEL):
            self.assertEqual(sha(a / name), sha(b / name), name)

    def test_twine_check(self):
        twine = need("twine")
        self.assertEqual(build(self.tmp, "wheel", "sdist").returncode, 0)
        p = subprocess.run([twine, "check", "--strict", str(self.tmp / WHEEL), str(self.tmp / SDIST)],
                           capture_output=True, text=True)
        self.assertEqual(p.returncode, 0, p.stdout + p.stderr)


class OutsideToolsTest(Tmp):
    def test_npm_dry_run(self):
        npm = need("npm")
        self.assertEqual(build(self.tmp, "tgz").returncode, 0)
        home = self.tmp / "home"
        home.mkdir()
        p = subprocess.run([npm, "publish", "--dry-run", "--tag", "snapshot", "--registry", "http://127.0.0.1:9",
                            str(self.tmp / TGZ)], capture_output=True, text=True, cwd=self.tmp,
                           env={**os.environ, "HOME": str(home), "npm_config_cache": str(home / ".npm")})
        self.assertEqual(p.returncode, 0, p.stdout + p.stderr)
        self.assertIn("total files: 4", p.stdout + p.stderr)
        self.assertIn("@apache/juneau-terminal-playwright@10.0.0-SNAPSHOT", p.stdout + p.stderr)

    def test_formula_parses(self):
        ruby = need("ruby")
        formula = DIST / "homebrew" / "juneau-terminal.rb"
        p = subprocess.run([ruby, "-c", str(formula)], capture_output=True, text=True)
        self.assertEqual(p.returncode, 0, p.stderr)
        text = formula.read_text()
        self.assertIn('url "https://downloads.apache.org/juneau/@VERSION@/apache-juneau-terminal-@VERSION@.pyz"', text)
        self.assertNotIn("closer.lua", text)
        self.assertIn('sha256 "@SHA256@"', text)


if __name__ == "__main__":
    unittest.main()
