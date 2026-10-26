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

import importlib.util, json, os, shutil, subprocess, sys, tempfile, textwrap, unittest
from pathlib import Path
import support

PLUGIN_DIR = support.MODULE_ROOT / "src" / "main" / "python" / "pytest_plugin"
HAVE_PYTEST = importlib.util.find_spec("pytest") is not None
HAVE_XDIST = importlib.util.find_spec("xdist") is not None

SUITE = '''
import pytest
def test_pass(): pass
def test_fail(): assert 0
@pytest.mark.skip
def test_skip(): pass
@pytest.mark.xfail
def test_xfail(): assert 0
@pytest.mark.xfail
def test_xpass(): pass
@pytest.fixture
def broken(): raise RuntimeError("setup")
def test_setup_error(broken): pass
@pytest.fixture
def bad_teardown():
    yield
    raise RuntimeError("teardown")
def test_teardown_error(bad_teardown): pass
'''


def run_pytest(tmp, *args, suite=SUITE):
    Path(tmp, "test_s.py").write_text(suite)
    env = {**os.environ, "PYTHONPATH": str(PLUGIN_DIR), "PYTHONDONTWRITEBYTECODE": "1"}
    p = subprocess.run([sys.executable, "-m", "pytest", "-p", "juneau_terminal_pytest", "-p", "no:cacheprovider", *args],
                       cwd=tmp, env=env, capture_output=True, text=True)
    marks = [json.loads(l[len("##pytest "):]) for l in p.stdout.splitlines() if l.startswith("##pytest ")]
    return p, marks


@unittest.skipUnless(HAVE_PYTEST, "pytest not installed")
class PluginTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.mkdtemp(prefix="jr-plugin-")
        self.addCleanup(shutil.rmtree, self.tmp, ignore_errors=True)

    def test_markers_for_every_outcome(self):
        _, marks = run_pytest(self.tmp)
        self.assertEqual(marks[0], {"ev": "hello", "v": 1})
        self.assertEqual(marks[1], {"ev": "total", "n": 7, "f": {"test_s.py": 7}})
        tests = [m for m in marks if m["ev"] == "test" and not m.get("late")]
        self.assertEqual(sorted(m["o"] for m in tests),
                         sorted(["passed", "failed", "skipped", "xfailed", "xpassed", "error", "passed"]))
        late = [m for m in marks if m.get("late")]
        self.assertEqual(len(late), 1)
        self.assertEqual(late[0]["o"], "error")
        self.assertTrue(all(m["id"].startswith("test_s.py::") for m in tests))

    def test_file_marker_names_each_test_file_once_as_it_starts(self):
        Path(self.tmp, "test_t.py").write_text("def test_a(): pass\ndef test_b(): pass\n")
        _, marks = run_pytest(self.tmp, suite="def test_a(): pass\ndef test_b(): pass\n")
        self.assertEqual([m["f"] for m in marks if m["ev"] == "file"], ["test_s.py", "test_t.py"])
        self.assertEqual([m for m in marks if m["ev"] == "run"], [])

    def test_total_equals_collected_and_each_test_once(self):
        _, marks = run_pytest(self.tmp)
        ids = [m["id"] for m in marks if m["ev"] == "test" and not m.get("late")]
        total = [m["n"] for m in marks if m["ev"] == "total"]
        self.assertEqual(total, [7])
        self.assertEqual(len(ids), total[0])
        self.assertEqual(len(ids), len(set(ids)))

    def test_survives_capture_modes(self):
        for flags in ((), ("-s",), ("-v",), ("-q",), ("-p", "no:terminal")):
            with self.subTest(flags=flags):
                p, marks = run_pytest(self.tmp, *flags)
                self.assertTrue(any(m["ev"] == "total" for m in marks), p.stdout + p.stderr)
                self.assertEqual(len([m for m in marks if m["ev"] == "test" and not m.get("late")]), 7)

    def test_marker_never_inside_a_progress_line(self):
        p, _ = run_pytest(self.tmp)
        for line in p.stdout.splitlines():
            if "##pytest" in line:
                self.assertTrue(line.startswith("##pytest "), line)

    def test_x_stops_early_with_larger_total(self):
        _, marks = run_pytest(self.tmp, "-x")
        n = [m for m in marks if m["ev"] == "total"][0]["n"]
        self.assertGreater(n, len([m for m in marks if m["ev"] == "test" and not m.get("late")]))

    def test_imports_cleanly_under_warnings_as_errors(self):
        p = subprocess.run([sys.executable, "-W", "error", "-c", "import juneau_terminal_pytest"],
                           env={**os.environ, "PYTHONPATH": str(PLUGIN_DIR)}, capture_output=True, text=True)
        self.assertEqual(p.returncode, 0, p.stderr)

    def test_passing_run_without_xdist_has_no_warning(self):
        p, _ = run_pytest(self.tmp, "-p", "no:xdist", "-W", "error", suite="def test_a(): pass\n")
        self.assertEqual(p.returncode, 0, p.stdout + p.stderr)

    def test_without_xdist_the_plugin_loads_and_reports_the_total(self):
        p, marks = run_pytest(self.tmp, "-p", "no:xdist", suite="def test_a(): pass\n")
        self.assertEqual(p.returncode, 0, p.stdout + p.stderr)
        self.assertIn({"ev": "total", "n": 1, "f": {"test_s.py": 1}}, marks)

    def test_zero_collected_still_emits_total(self):
        _, marks = run_pytest(self.tmp, suite="x = 1\n")
        self.assertEqual([m["n"] for m in marks if m["ev"] == "total"], [0])

    def test_marker_is_flushed_before_the_next_test_runs_when_piped(self):
        suite = ("import sys, os\n"
                 "def test_a(): pass\n"
                 "def test_b():\n"
                 "    os.write(2, b'B-START\\n')\n")
        # stdout and stderr go to the same pipe: A's marker must be there before test_b writes to fd 2
        Path(self.tmp, "test_s.py").write_text(suite)
        env = {**os.environ, "PYTHONPATH": str(PLUGIN_DIR), "PYTHONDONTWRITEBYTECODE": "1"}
        p = subprocess.run([sys.executable, "-m", "pytest", "-p", "juneau_terminal_pytest", "-p", "no:cacheprovider", "-s"],
                           cwd=self.tmp, env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
        self.assertLess(p.stdout.index('"id":"test_s.py::test_a"'), p.stdout.index("B-START"), p.stdout)


@unittest.skipUnless(HAVE_PYTEST and HAVE_XDIST, "pytest-xdist not installed")
class XdistTest(unittest.TestCase):
    def test_total_once_and_workers_silent(self):
        tmp = tempfile.mkdtemp(prefix="jr-plugin-x-")
        self.addCleanup(shutil.rmtree, tmp, ignore_errors=True)
        suite = "import pytest\n" + "".join(f"def test_{i}(): pass\n" for i in range(12))
        p, marks = run_pytest(tmp, "-n", "2", suite=suite)
        self.assertEqual(len([m for m in marks if m["ev"] == "hello"]), 1, p.stdout)
        self.assertEqual([m["n"] for m in marks if m["ev"] == "total"], [12])
        self.assertEqual(len([m for m in marks if m["ev"] == "test"]), 12)
