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
"""The zip: the assembly descriptor covers every toolkit module, the pom builds and attaches it, and the zip imports
through bootstrap.load and runs the authoring example against a bare remote, with markers on and off."""

import contextlib
import os
import re
import shutil
import subprocess
import tempfile
import textwrap
import unittest
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

from scripts_support import (ASSEMBLY, BOOTSTRAP, MODULE_ROOT, PYTHON_SRC, SCRIPT, base_env, build_zip, markers,
                             package_files, pycache_under, python)
from juneau_scripts.testing import bare_remote, fake_tool, hermetic_git

ASM = "{http://maven.apache.org/plugins/maven-assembly-plugin/assembly/1.1.3}"
POM = "{http://maven.apache.org/POM/4.0.0}"

BIN_NAMES = ["juneau_scripts/bin/tmux-pane.sh", "juneau_scripts/bin/watch-run.sh"]
EXPECTED_NAMES = sorted(package_files() + BIN_NAMES + ["LICENSE", "NOTICE", "bootstrap.py", "__main__.py",
                                                       "pytest_plugin/juneau_run_pytest.py",
                                                       "playwright_reporter/juneau_run_playwright.cjs"])


def _assert_bin_modes(test, z):
    for rel in BIN_NAMES:
        test.assertEqual((z.getinfo(rel).external_attr >> 16) & 0o777, 0o755, rel)


def _ant(pattern):
    """An Ant include pattern as a regex: ** spans directories, * stays within one."""
    out = re.escape(pattern).replace(r"\*\*/", "(?:.*/)?").replace(r"\*\*", ".*").replace(r"\*", "[^/]*")
    return re.compile(out + r"\Z")


class DescriptorTest(unittest.TestCase):
    def setUp(self):
        self.root = ET.parse(ASSEMBLY).getroot()

    def test_zip_layout(self):
        self.assertEqual([f.text for f in self.root.iter(f"{ASM}format")], ["zip"])
        self.assertEqual(self.root.findtext(f"{ASM}includeBaseDirectory"), "false")

    def test_every_toolkit_module_is_included(self):
        sets = self.root.findall(f"{ASM}fileSets/{ASM}fileSet")
        self.assertEqual(len(sets), 2)
        fs = sets[0]
        self.assertEqual(fs.findtext(f"{ASM}directory"), "${project.basedir}/src/main/python")
        includes = [_ant(i.text) for i in fs.iter(f"{ASM}include")]
        excludes = [_ant(e.text) for e in fs.iter(f"{ASM}exclude")]
        files = package_files()
        self.assertIn("juneau_scripts/__init__.py", files)
        self.assertIn("juneau_scripts/testing/__init__.py", files)
        for rel in files:
            with self.subTest(rel=rel):
                self.assertTrue(any(p.match(rel) for p in includes))
                self.assertFalse(any(p.match(rel) for p in excludes))
        for rel in ("juneau_run.py", "bootstrap.py", "juneau_scripts/__pycache__/run.cpython-314.pyc"):
            with self.subTest(rel=rel):
                self.assertFalse(any(p.match(rel) for p in includes) and not any(p.match(rel) for p in excludes))
        for rel in BIN_NAMES:
            with self.subTest(rel=rel):
                self.assertFalse(any(p.match(rel) for p in includes))
        bins = sets[1]
        self.assertEqual(bins.findtext(f"{ASM}directory"), "${project.basedir}/src/main/python")
        self.assertEqual(bins.findtext(f"{ASM}outputDirectory"), ".")
        self.assertEqual([i.text for i in bins.iter(f"{ASM}include")], ["juneau_scripts/bin/*.sh"])
        self.assertEqual(bins.findtext(f"{ASM}fileMode"), "0755")

    def test_root_files(self):
        sources = [f.findtext(f"{ASM}source") for f in self.root.findall(f"{ASM}files/{ASM}file")]
        self.assertEqual(sources, ["${project.basedir}/src/main/python/bootstrap.py",
                                   "${project.basedir}/src/main/python/__main__.py",
                                   "${project.basedir}/src/main/python/pytest_plugin/juneau_run_pytest.py",
                                   "${project.basedir}/src/main/js/playwright_reporter/juneau_run_playwright.cjs",
                                   "${project.basedir}/../LICENSE", "${project.basedir}/../NOTICE"])
        for s in sources:
            self.assertTrue(Path(s.replace("${project.basedir}", str(MODULE_ROOT))).is_file(), s)

    def test_build_zip_matches_the_descriptor(self):
        with tempfile.TemporaryDirectory() as tmp:
            with zipfile.ZipFile(build_zip(Path(tmp) / "t.zip")) as z:
                names = sorted(z.namelist())
                _assert_bin_modes(self, z)
        self.assertEqual(names, EXPECTED_NAMES)


@unittest.skipUnless(os.environ.get("JUNEAU_SCRIPTS_CHECK_ZIP"), "set JUNEAU_SCRIPTS_CHECK_ZIP to a mvn package zip")
class RealZipTest(unittest.TestCase):
    """The zip mvn package really built, against the source tree.  Opt-in: in mvn verify the tests run before the
    package phase, so target/juneau-run.zip is absent or stale then.  T14 Step 5 runs it after a fresh package."""

    def test_real_zip_holds_exactly_the_toolkit(self):
        with zipfile.ZipFile(os.environ["JUNEAU_SCRIPTS_CHECK_ZIP"]) as z:
            names = sorted(n for n in z.namelist() if not n.endswith("/"))
            _assert_bin_modes(self, z)
        self.assertEqual(names, EXPECTED_NAMES)


class PomTest(unittest.TestCase):
    def setUp(self):
        self.root = ET.parse(MODULE_ROOT / "pom.xml").getroot()
        self.plugins = self.root.findall(f"{POM}build/{POM}plugins/{POM}plugin")

    def plugin(self, artifact_id):
        return next(p for p in self.plugins if p.findtext(f"{POM}artifactId") == artifact_id)

    def test_assembly_execution(self):
        ex = self.plugin("maven-assembly-plugin").find(f"{POM}executions/{POM}execution")
        self.assertEqual(ex.findtext(f"{POM}phase"), "package")
        cfg = ex.find(f"{POM}configuration")
        self.assertEqual(cfg.findtext(f"{POM}descriptors/{POM}descriptor"), "src/assembly/juneau-scripts.xml")
        self.assertEqual(cfg.findtext(f"{POM}finalName"), "juneau-run")
        self.assertEqual(cfg.findtext(f"{POM}appendAssemblyId"), "false")
        self.assertEqual(cfg.findtext(f"{POM}attach"), "false")

    def test_assembly_runs_before_build_helper(self):
        ids = [p.findtext(f"{POM}artifactId") for p in self.plugins]
        self.assertLess(ids.index("maven-assembly-plugin"), ids.index("build-helper-maven-plugin"))

    def test_zip_attached_beside_the_py(self):
        artifacts = self.plugin("build-helper-maven-plugin").findall(f".//{POM}artifact")
        self.assertEqual([(a.findtext(f"{POM}file"), a.findtext(f"{POM}type"), a.findtext(f"{POM}classifier"))
                          for a in artifacts],
                         [("${project.basedir}/src/main/python/juneau_run.py", "py", None),
                          ("${project.basedir}/src/main/python/pytest_plugin/juneau_run_pytest.py", "py", "pytest"),
                          ("${project.build.directory}/juneau-run.zip", "zip", None)])

    def test_description_names_the_toolkit(self):
        self.assertIn("and the juneau_scripts toolkit (.zip)", self.root.findtext(f"{POM}description"))


# The spec's authoring example ("Authoring example"), verbatim inside main(), plus the argparse it assumes.
EXAMPLE = textwrap.dedent('''\
    import argparse
    from bootstrap import load
    jr, js = load(pom="pom.xml")
    from juneau_scripts import git, run, tools, notify

    def main(args):
        with run.session("push", bell=True) as s:
            dirty = git.unstaged_paths() + git.untracked_paths()
            if dirty:                                    # this project's policy
                s.fail("Unreviewed changes", hint=git.stash_hint(dirty))
            if not args.skip_tests:
                s.require(tools.maven(["verify"], title="Build and test"))
            if args.test_only:
                return
            s.require(git.commit_index(args.message))
            s.require(git.push(set_upstream=True))

    if __name__ == "__main__":
        p = argparse.ArgumentParser()
        p.add_argument("-m", "--message", default="Change")
        p.add_argument("--skip-tests", action="store_true")
        p.add_argument("--test-only", action="store_true")
        main(p.parse_args())
''')


class ZipEndToEndTest(unittest.TestCase):
    def setUp(self):
        stack = contextlib.ExitStack()
        self.addCleanup(stack.close)
        self.tmp = Path(stack.enter_context(tempfile.TemporaryDirectory()))
        self.git_env = stack.enter_context(hermetic_git(self.tmp))
        self.work, self.origin = bare_remote(self.tmp)
        self.scripts = self.tmp / "scripts"
        self.scripts.mkdir()
        (self.scripts / "push.py").write_text(EXAMPLE, encoding="utf-8")
        shutil.copy(BOOTSTRAP, self.scripts / "bootstrap.py")
        self.zip = build_zip(self.tmp / "juneau-run.zip")
        self.bin = self.tmp / "bin"
        fake_tool(self.bin, "mvn", log_env="MVN_LOG")
        self.mvn_log = self.tmp / "mvn.jsonl"

    def push(self, *args, **env):
        full = base_env(**self.git_env, JUNEAU_SCRIPTS_ZIP=str(self.zip), JUNEAU_RUN_PY=str(SCRIPT),
                        MVN_LOG=str(self.mvn_log), PATH=f"{self.bin}{os.pathsep}{os.environ['PATH']}", **env)
        return python([str(self.scripts / "push.py"), *args], cwd=self.work, env=full)

    def stage(self):
        (self.work / "README.md").write_text("changed\n", encoding="utf-8")
        subprocess.run(["git", "add", "README.md"], cwd=self.work, check=True)

    def head(self, repo):
        return subprocess.run(["git", "rev-parse", "main"], cwd=repo, capture_output=True, text=True,
                              check=True).stdout.strip()

    def test_plain_push(self):
        self.stage()
        done = self.push("-m", "Change the readme")
        self.assertEqual(done.returncode, 0, done.stdout + done.stderr)
        self.assertEqual(self.head(self.origin), self.head(self.work))
        self.assertEqual(markers(done.stdout), [])
        self.assertRegex(done.stdout.splitlines()[-1], r"^✅ push: ok \(\d+\.\ds\)$")
        self.assertIn('["mvn", "verify"]', self.mvn_log.read_text(encoding="utf-8"))
        self.assertEqual(list(self.tmp.rglob("__pycache__")), [])

    def test_markers_push(self):
        self.stage()
        done = self.push("-m", "Change the readme", RUN_MARKERS="1")
        self.assertEqual(done.returncode, 0, done.stdout + done.stderr)
        got = markers(done.stdout)
        self.assertEqual(got[0]["ev"], "run")
        self.assertEqual([m for m in got if m["ev"] == "done"], [{"ev": "done", "status": "ok"}])
        self.assertEqual(got[-1]["ev"], "done")
        self.assertIn("Build and test", [m.get("title") for m in got if m["ev"] == "step"])
        self.assertEqual(self.head(self.origin), self.head(self.work))

    def test_zip_carries_the_licence_files(self):
        with zipfile.ZipFile(self.zip) as z:
            self.assertLessEqual({"LICENSE", "NOTICE"}, set(z.namelist()))

    def test_refusal(self):
        for env in ({}, {"RUN_MARKERS": "1"}):
            with self.subTest(env=env):
                (self.work / "notes.txt").write_text("draft\n", encoding="utf-8")
                before = self.head(self.work)
                done = self.push(**env)
                self.assertEqual(done.returncode, 1, done.stdout + done.stderr)
                self.assertIn("Unreviewed changes", done.stdout + done.stderr)
                self.assertIn("notes.txt", done.stdout + done.stderr)
                self.assertEqual(self.head(self.work), before)
                if env:
                    self.assertEqual([m for m in markers(done.stdout) if m["ev"] == "done"],
                                     [{"ev": "done", "status": "fail"}])


class ParsersPackagingTest(unittest.TestCase):
    def test_the_zip_carries_the_parsers_package(self):
        files = package_files()
        for rel in ("juneau_scripts/parsers/__init__.py", "juneau_scripts/parsers/_side.py",
                    "juneau_scripts/parsers/bazel.py"):
            self.assertIn(rel, files)

    def test_juneau_run_beside_its_zip_resolves_the_bazel_parser(self):
        tmp = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, tmp, True)
        build_zip(tmp / "juneau-run.zip")
        shutil.copy(SCRIPT, tmp / "juneau-run.py")
        code = ("import importlib.util, sys\n"
                "spec = importlib.util.spec_from_file_location('juneau_run', sys.argv[1])\n"
                "jr = importlib.util.module_from_spec(spec); sys.modules['juneau_run'] = jr\n"
                "spec.loader.exec_module(jr)\n"
                "print(type(jr._resolve_parser('bazel')).__name__)\n")
        r = python(["-I", "-c", code, str(tmp / "juneau-run.py")], cwd=str(tmp), env=base_env())
        self.assertEqual((r.returncode, r.stdout), (0, "BazelParser\n"), r.stderr)
        self.assertEqual(pycache_under(tmp), [])


if __name__ == "__main__":
    unittest.main()
