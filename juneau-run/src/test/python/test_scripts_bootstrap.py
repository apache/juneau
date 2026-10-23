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
"""bootstrap.load(), each case in a fresh subprocess: overrides, in-tree mode, ~/.m2 in place, the Maven copy,
idempotence, the namespace-package guard, the stale-API warning and the juneau_run decision."""

import json
import os
import shutil
import sys
import tempfile
import textwrap
import unittest
import unittest.mock
from pathlib import Path

import scripts_support  # noqa: F401  (puts src/main/python on sys.path)
from scripts_support import BOOTSTRAP, PYTHON_SRC, SCRIPT, base_env, build_zip, copy_package, pycache_under, python

DRIVER = textwrap.dedent('''
    import json, os, sys
    sys.path.insert(0, sys.argv[1])
    import bootstrap
    kw = json.loads(os.environ.get("LOAD_KW", "{}"))
    try:
        results = [bootstrap.load(**kw) for _ in range(int(os.environ.get("LOADS", "1")))]
    except bootstrap.BootstrapError as e:
        print(json.dumps({"error": str(e)}))
        sys.exit(0)
    jr, js = results[-1]
    exec(os.environ.get("EXTRA_CODE", ""))  # e.g. the script's own imports after load() returns
    print(json.dumps({
        "js": js.__file__,
        "jr": getattr(jr, "__file__", None),
        "api": js.BOOTSTRAP_API,
        "same": all(r[1] is js for r in results),
        "path_entries": [p for p in sys.path if p.endswith(".zip") or p.endswith("python") or "juneau-run" in p],
    }))
''')

# What a script does after load() returns (the authoring example).  The no-bytecode tests run it, so restoring
# sys.dont_write_bytecode after the __init__ import would write __pycache__ and fail them (Decision Q10).
LATE_IMPORTS = "from juneau_scripts import git, run, tools, notify"

# A fake mvn for dependency:copy: logs its argv, then copies $FAKE_ARTIFACTS/juneau-run.<type> into the output
# directory (or exits 1 with MVN_FAIL=1, or when the artifact isn't there).
FAKE_MVN = textwrap.dedent('''\
    #!{python}
    import json, os, shutil, sys
    with open(os.environ["MVN_LOG"], "a", encoding="utf-8") as f:
        f.write(json.dumps(sys.argv[1:]) + "\\n")
    if os.environ.get("MVN_FAIL") == "1":
        sys.exit(1)
    args = dict(a[2:].split("=", 1) for a in sys.argv[1:] if a.startswith("-D") and "=" in a)
    kind = args["artifact"].rsplit(":", 1)[1]
    source = os.path.join(os.environ["FAKE_ARTIFACTS"], "juneau-run." + kind)
    if not os.path.exists(source):
        sys.exit(1)
    os.makedirs(args["outputDirectory"], exist_ok=True)
    shutil.copy(source, os.path.join(args["outputDirectory"], "juneau-run." + kind))
''')

POM = '<project><properties><juneau.version>{version}</juneau.version></properties></project>\n'


class BootstrapCase(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.tmp = Path(tmp.name).resolve()     # bootstrap resolves paths; on macOS /var is /private/var
        self.vendor = self.tmp / "vendor"
        self.vendor.mkdir()
        shutil.copy(BOOTSTRAP, self.vendor / "bootstrap.py")
        (self.tmp / "driver.py").write_text(DRIVER, encoding="utf-8")
        self.home = self.tmp / "home"
        self.home.mkdir()
        self.bin = self.tmp / "bin"
        self.bin.mkdir()
        (self.bin / "mvn").write_text(FAKE_MVN.format(python=sys.executable), encoding="utf-8")
        (self.bin / "mvn").chmod(0o755)
        self.mvn_log = self.tmp / "mvn.jsonl"
        self.artifacts = self.tmp / "artifacts"
        self.artifacts.mkdir()
        build_zip(self.artifacts / "juneau-run.zip")
        shutil.copy(SCRIPT, self.artifacts / "juneau-run.py")
        self.repo = self.tmp / "repo"
        self.repo.mkdir()
        self.pom("10.0.0-SNAPSHOT")

    def pom(self, version):
        (self.repo / "pom.xml").write_text(POM.format(version=version), encoding="utf-8")

    def m2(self, version, *, py=True, zip=True):
        d = self.home / ".m2" / "repository" / "org" / "apache" / "juneau" / "juneau-run" / version
        d.mkdir(parents=True, exist_ok=True)
        if py:
            shutil.copy(SCRIPT, d / f"juneau-run-{version}.py")
        if zip:
            build_zip(d / f"juneau-run-{version}.zip")
        return d

    def load(self, *, loads=1, bytecode=False, extra_code=None, **kw):
        kw.setdefault("pom", str(self.repo / "pom.xml"))
        kw.setdefault("cache_dir", str(self.repo / "target" / "juneau-run"))
        env = base_env(HOME=str(self.home), PATH=f"{self.bin}{os.pathsep}{os.environ['PATH']}",
                       MVN_LOG=str(self.mvn_log), FAKE_ARTIFACTS=str(self.artifacts), LOAD_KW=json.dumps(kw),
                       LOADS=str(loads), EXTRA_CODE=extra_code or "")
        env.update(getattr(self, "env", {}))
        script = [str(self.tmp / "driver.py"), str(self.vendor)]
        done = python(script, cwd=self.repo, env=env, bytecode=bytecode)
        self.assertEqual(done.returncode, 0, done.stderr)
        self.stderr = done.stderr
        return json.loads(done.stdout.strip().splitlines()[-1])

    def mvn_calls(self):
        if not self.mvn_log.exists():
            return []
        return [json.loads(line) for line in self.mvn_log.read_text(encoding="utf-8").splitlines()]


class OverrideTest(BootstrapCase):
    def test_zip_override(self):
        z = build_zip(self.tmp / "toolkit.zip")
        self.env = {"JUNEAU_SCRIPTS_ZIP": str(z), "JUNEAU_RUN_PY": str(SCRIPT)}
        out = self.load()
        self.assertTrue(out["js"].startswith(str(z)), out)
        self.assertIsNone(out["jr"])
        self.env["RUN_MARKERS"] = "1"
        self.assertEqual(self.load()["jr"], str(SCRIPT))
        self.assertEqual(self.mvn_calls(), [])

    def test_directory_override_writes_no_bytecode(self):
        d = copy_package(self.tmp / "dir")
        self.env = {"JUNEAU_SCRIPTS_ZIP": str(d)}
        out = self.load(bytecode=True, extra_code=LATE_IMPORTS)
        self.assertEqual(out["js"], str(d / "juneau_scripts" / "__init__.py"))
        self.assertEqual(pycache_under(d), [])

    def test_zip_beside_juneau_run_py(self):
        d = self.m2("10.0.0-SNAPSHOT")
        self.env = {"JUNEAU_RUN_PY": str(d / "juneau-run-10.0.0-SNAPSHOT.py"), "RUN_MARKERS": "1"}
        out = self.load()
        self.assertTrue(out["js"].startswith(str(d / "juneau-run-10.0.0-SNAPSHOT.zip")), out)
        self.assertEqual(out["jr"], str(d / "juneau-run-10.0.0-SNAPSHOT.py"))
        self.assertEqual(self.mvn_calls(), [])

    def test_relative_zip_override_survives_chdir(self):
        build_zip(self.repo / "rel.zip")
        code = textwrap.dedent('''
            import os, sys
            sys.path.insert(0, sys.argv[1])
            import bootstrap
            bootstrap.load(pom=sys.argv[2])
            os.chdir(sys.argv[3])
            import juneau_scripts.lock
            print("ok")
        ''')
        elsewhere = self.tmp / "elsewhere"
        elsewhere.mkdir()
        env = base_env(HOME=str(self.home), PATH=f"{self.bin}{os.pathsep}{os.environ['PATH']}",
                       MVN_LOG=str(self.mvn_log), FAKE_ARTIFACTS=str(self.artifacts), JUNEAU_SCRIPTS_ZIP="rel.zip")
        done = python(["-c", code, str(self.vendor), str(self.repo / "pom.xml"), str(elsewhere)], cwd=self.repo,
                      env=env)
        self.assertEqual(done.stdout.strip(), "ok", done.stderr)
        self.assertEqual(self.mvn_calls(), [])

    def test_directory_beside_juneau_run_py(self):
        d = copy_package(self.tmp / "tree")
        self.env = {"JUNEAU_RUN_PY": str(d / "juneau_run.py")}
        self.assertEqual(self.load(bytecode=True, extra_code=LATE_IMPORTS)["js"], str(d / "juneau_scripts" / "__init__.py"))
        self.assertEqual(pycache_under(d), [])

    def test_nothing_beside_juneau_run_py_is_an_error_without_maven(self):
        d = self.m2("10.0.0-SNAPSHOT", zip=False)
        self.env = {"JUNEAU_RUN_PY": str(d / "juneau-run-10.0.0-SNAPSHOT.py")}
        out = self.load()
        self.assertIn(str(d / "juneau-run-10.0.0-SNAPSHOT.zip"), out["error"])
        self.assertIn("mvn install", out["error"])
        self.assertEqual(self.mvn_calls(), [])


class InTreeTest(BootstrapCase):
    def test_in_tree_mode_writes_no_bytecode(self):
        tree = copy_package(self.repo / "juneau-run" / "src" / "main" / "python")
        out = self.load(bytecode=True, run=True, extra_code=LATE_IMPORTS)
        self.assertEqual(out["js"], str(tree / "juneau_scripts" / "__init__.py"))
        self.assertEqual(out["jr"], str(tree / "juneau_run.py"))
        self.assertEqual(pycache_under(tree), [])
        self.assertEqual(self.mvn_calls(), [])


class MavenTest(BootstrapCase):
    def test_m2_hit_uses_the_files_in_place(self):
        d = self.m2("10.0.0-SNAPSHOT")
        out = self.load(run=True)
        self.assertTrue(out["js"].startswith(str(d / "juneau-run-10.0.0-SNAPSHOT.zip")), out)
        self.assertEqual(out["jr"], str(d / "juneau-run-10.0.0-SNAPSHOT.py"))
        self.assertEqual(self.mvn_calls(), [])

    def test_m2_miss_copies(self):
        for missing in ("py", "zip"):
            with self.subTest(missing=missing):
                shutil.rmtree(self.home / ".m2", ignore_errors=True)
                shutil.rmtree(self.repo / "target", ignore_errors=True)
                self.mvn_log.unlink(missing_ok=True)
                self.m2("10.0.0-SNAPSHOT", **{missing: False})
                out = self.load()
                cache = self.repo / "target" / "juneau-run"
                self.assertTrue(out["js"].startswith(str(cache / "juneau-run.zip")), out)
                self.assertEqual([c[3] for c in self.mvn_calls()],
                                 ["-Dartifact=org.apache.juneau:juneau-run:10.0.0-SNAPSHOT:py",
                                  "-Dartifact=org.apache.juneau:juneau-run:10.0.0-SNAPSHOT:zip"])
                self.assertEqual(self.mvn_calls()[0][:3], ["-q", "-nsu", "dependency:copy"])

    def test_snapshot_copies_once_per_process(self):
        self.load(loads=3)
        self.assertEqual(len(self.mvn_calls()), 2)
        self.load()
        self.assertEqual(len(self.mvn_calls()), 4)     # a new process copies a SNAPSHOT again

    def test_release_copies_only_when_missing(self):
        self.pom("10.0.0")
        self.load()
        self.assertEqual(len(self.mvn_calls()), 2)
        self.load()
        self.assertEqual(len(self.mvn_calls()), 2)

    def test_missing_artifact_names_the_coordinate(self):
        self.env = {"MVN_FAIL": "1"}
        out = self.load()
        self.assertIn("org.apache.juneau:juneau-run:10.0.0-SNAPSHOT:py", out["error"])
        self.assertIn("mvn install", out["error"])

    def test_no_juneau_version(self):
        (self.repo / "pom.xml").write_text("<project/>\n", encoding="utf-8")
        self.assertIn("juneau.version", self.load()["error"])


class ImportTest(BootstrapCase):
    def test_idempotent(self):
        self.m2("10.0.0-SNAPSHOT")
        out = self.load(loads=3, run=True)
        self.assertTrue(out["same"])
        self.assertEqual(len([p for p in out["path_entries"] if p.endswith(".zip") and p.startswith(str(self.home))]),
                         1)     # not the stdlib's own pythonNN.zip
        code = textwrap.dedent('''
            import sys
            sys.path.insert(0, sys.argv[1])
            import bootstrap
            jr1, js1 = bootstrap.load(pom=sys.argv[2], run=True)
            del sys.modules["juneau_run"]
            jr2, js2 = bootstrap.load(pom=sys.argv[2], run=True)
            print(jr2 is not None and jr2 is not jr1 and sys.modules["juneau_run"] is jr2 and js1 is js2)
        ''')
        done = python(["-c", code, str(self.vendor), str(self.repo / "pom.xml")], cwd=self.repo,
                      env=base_env(HOME=str(self.home), PATH=f"{self.bin}{os.pathsep}{os.environ['PATH']}",
                                   MVN_LOG=str(self.mvn_log), FAKE_ARTIFACTS=str(self.artifacts)))
        self.assertEqual(done.stdout.strip(), "True", done.stderr)
        self.assertEqual(self.mvn_calls(), [])     # ~/.m2 hit in place; only ever the fake mvn on PATH

    def test_zip_without_init_is_a_bootstrap_error(self):
        z = build_zip(self.tmp / "noinit.zip", init=False)
        self.env = {"JUNEAU_SCRIPTS_ZIP": str(z)}
        out = self.load()
        self.assertIn(str(z), out["error"])
        self.assertIn("__init__.py", out["error"])

    def test_zip_without_init_but_with_directory_entries_is_a_bootstrap_error(self):
        # The shape a Maven assembly writes.  Python 3.10-3.13 import this as a namespace package; so does 3.14.
        import zipfile
        z = build_zip(self.tmp / "noinit-dirs.zip", init=False)
        with zipfile.ZipFile(z, "a") as zz:
            zz.writestr("juneau_scripts/", "")
        self.env = {"JUNEAU_SCRIPTS_ZIP": str(z)}
        self.assertIn("__init__.py", self.load()["error"])

    def test_directory_without_init_is_a_bootstrap_error(self):
        d = copy_package(self.tmp / "noinit-dir")
        (d / "juneau_scripts" / "__init__.py").unlink()
        self.env = {"JUNEAU_SCRIPTS_ZIP": str(d)}
        out = self.load()
        self.assertIn(str(d), out["error"])
        self.assertIn("__init__.py", out["error"])

    def test_stale_bootstrap_api_warns_once(self):
        text = (self.vendor / "bootstrap.py").read_text(encoding="utf-8")
        (self.vendor / "bootstrap.py").write_text(text.replace("BOOTSTRAP_API = 1", "BOOTSTRAP_API = 0"),
                                                  encoding="utf-8")
        self.m2("10.0.0-SNAPSHOT")
        out = self.load(loads=2)
        self.assertEqual(out["api"], 0)
        self.assertEqual(self.stderr.count("bootstrap.py is older than juneau_scripts expects"), 1)

    def test_current_bootstrap_api_is_quiet(self):
        self.m2("10.0.0-SNAPSHOT")
        self.assertEqual(self.load()["api"], 1)
        self.assertNotIn("older than", self.stderr)


class RunRequestedTest(unittest.TestCase):
    def test_true_for_markers_board_and_none(self):
        sys.path.insert(0, str(PYTHON_SRC))
        self.addCleanup(sys.path.remove, str(PYTHON_SRC))
        import bootstrap
        for env, expected in (({}, False), ({"RUN_MARKERS": "1"}, True), ({"JUNEAU_RUN_CONSOLE": "board"}, True),
                              ({"JUNEAU_RUN_CONSOLE": "none"}, True), ({"JUNEAU_RUN_CONSOLE": "full"}, False)):
            with self.subTest(env=env), unittest.mock.patch.dict(os.environ, env, clear=True):
                self.assertEqual(bootstrap.run_requested(), expected)


class RunDecisionTest(BootstrapCase):
    def setUp(self):
        super().setUp()
        self.m2("10.0.0-SNAPSHOT")

    def test_environment_decides_when_run_is_none(self):
        self.assertIsNone(self.load()["jr"])
        for var, value in (("RUN_MARKERS", "1"), ("JUNEAU_RUN_CONSOLE", "board")):
            with self.subTest(var=var):
                self.env = {var: value}
                self.assertIsNotNone(self.load()["jr"])
        self.env = {"RUN_MARKERS": "0", "JUNEAU_RUN_CONSOLE": "full"}
        self.assertIsNone(self.load()["jr"])
        self.env = {}
        self.assertIsNone(self.load(run=False)["jr"])

    def test_console_none_loads_juneau_run(self):
        self.env = {"JUNEAU_RUN_CONSOLE": "none"}
        self.assertIsNotNone(self.load()["jr"])
        self.assertIsNone(self.load(run=False)["jr"])

    def test_missing_juneau_run_warns_when_requested_and_raises_when_forced(self):
        z = build_zip(self.tmp / "toolkit.zip")
        (self.home / ".m2").rename(self.tmp / "no-m2")
        self.env = {"JUNEAU_SCRIPTS_ZIP": str(z), "RUN_MARKERS": "1"}
        self.assertIsNone(self.load()["jr"])
        self.assertIn("warning: juneau-run unavailable", self.stderr)
        self.assertIn("juneau_run.py not found", self.load(run=True)["error"])


if __name__ == "__main__":
    unittest.main()
