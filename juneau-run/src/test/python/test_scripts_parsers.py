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
"""juneau_scripts.parsers: binding to juneau_run, SideFile, the registry and detect()."""

import shutil
import sys
import tempfile
import types
import unittest
from pathlib import Path

from scripts_support import PYTHON_SRC, base_env, jr, python

from juneau_scripts import parsers
from juneau_scripts.parsers import _side


class SideFileTest(unittest.TestCase):
    def setUp(self):
        self.dir = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.dir, True)
        self.path = self.dir / "side.json"

    def append(self, data):
        with open(self.path, "ab") as f:
            f.write(data)

    def test_a_missing_file_reads_nothing(self):
        s = parsers.SideFile(self.path)
        self.assertEqual(s.read_new(), [])
        self.assertEqual(s.drain(), [])

    def test_complete_lines_only_until_drain(self):
        s = parsers.SideFile(self.path)
        self.append(b"a\nb")
        self.assertEqual(s.read_new(), [b"a"])
        self.append(b"c\n")
        self.assertEqual(s.read_new(), [b"bc"])
        self.append(b"tail")
        self.assertEqual(s.read_new(), [])
        self.assertEqual(s.drain(), [b"tail"])
        self.assertEqual(s.drain(), [])

    def test_a_truncated_file_is_read_again_from_the_start(self):
        self.append(b"one\ntwo\n")
        s = parsers.SideFile(self.path)
        self.assertEqual(s.read_new(), [b"one", b"two"])
        self.path.write_bytes(b"x\n")
        self.assertEqual(s.read_new(), [b"x"])

    def test_ignore_existing_skips_a_stale_file_until_it_changes(self):
        self.append(b"stale\n")
        s = parsers.SideFile(self.path, ignore_existing=True)
        self.assertEqual(s.read_new(), [])
        self.path.write_bytes(b"fresh line\n")
        self.assertEqual(s.read_new(), [b"fresh line"])

    def test_ignore_existing_with_no_file_reads_the_new_one(self):
        s = parsers.SideFile(self.path, ignore_existing=True)
        self.append(b"new\n")
        self.assertEqual(s.read_new(), [b"new"])


class BindingTest(unittest.TestCase):
    def setUp(self):
        self.addCleanup(parsers.bind, jr)

    def test_unbound_access_raises_parser_not_bound(self):
        code = ("import sys; sys.path.insert(0, sys.argv[1])\n"
                "from juneau_scripts.parsers import _side, ParserNotBound\n"
                "try:\n    _side.Parser\nexcept ParserNotBound as e:\n    print('unbound:', e)\n")
        r = python(["-c", code, str(PYTHON_SRC)], env=base_env())
        self.assertEqual(r.returncode, 0, r.stderr)
        self.assertEqual(r.stdout,
                         "unbound: juneau_scripts.parsers is not bound to juneau_run (call parsers.bind(jr) first)\n")

    def test_bound_exports_are_juneau_runs(self):
        parsers.bind(jr)
        self.assertIs(_side.Parser, jr.Parser)
        self.assertIs(_side.GenericParser, jr.GenericParser)
        self.assertIs(_side.strip_ansi, jr.strip_ansi)
        self.assertIs(_side.warn, jr.warn)
        with self.assertRaises(AttributeError):
            _side.nothing_here

    def test_an_unregistered_name_is_unknown_and_load_raises_key_error(self):
        self.assertFalse(parsers.known("nope"))
        with self.assertRaises(KeyError):
            parsers.load("nope")

    def test_rebinding_a_different_module_drops_loaded_parser_modules(self):
        name = parsers.__name__ + "._probe"
        parsers.bind(jr)
        sys.modules[name] = types.ModuleType(name)
        self.addCleanup(sys.modules.pop, name, None)
        parsers.bind(jr)
        self.assertIn(name, sys.modules)
        parsers.bind(types.SimpleNamespace())
        self.assertNotIn(name, sys.modules)
        self.assertIn(_side.__name__, sys.modules)


DETECTED = [
    (["mvn", "verify"], "maven"),
    (["./mvnw", "verify"], "maven"),
    (["mvnw.cmd", "verify"], "maven"),
    (["gradle", "build"], "gradle"),
    (["./gradlew", "test"], "gradle"),
    (["gradlew.bat", "test"], "gradle"),
    (["pytest", "-q"], "pytest"),
    (["py.test"], "pytest"),
    (["python3", "-m", "pytest", "tests"], "pytest"),
    (["uv", "run", "pytest"], "pytest"),
    (["uv", "run", "--with", "pytest", "pytest", "-q", "scripts/tests"], "pytest"),
    (["npm", "ci"], "npm"),
    (["npm", "test"], "npm"),
    (["npm", "run", "test"], "npm"),
    (["npx", "playwright", "test"], "playwright"),
    (["npx", "--yes", "playwright", "test"], "playwright"),
    (["playwright", "test"], "playwright"),
    (["pnpm", "exec", "playwright", "test"], "playwright"),
    (["yarn", "playwright", "test"], "playwright"),
    (["npm", "exec", "--", "playwright", "test"], "playwright"),
    (["env", "CI=1", "mvn", "verify"], "maven"),
    (["env", "-i", "PATH=/bin", "npm", "ci"], "npm"),
    (["env", "-u", "HOME", "gradle", "build"], "gradle"),
    (["env", "CI=1", "pytest"], "pytest"),
    (["bazel", "build", "//..."], "bazel"),
    (["bazel", "test", "//..."], "bazel"),
    (["bazelisk", "coverage", "//..."], "bazel"),
    (["bazel", "--output_user_root=/x", "test", "//..."], "bazel"),
    ([Path("/usr/bin/mvn"), "verify"], "maven"),
]

NOT_DETECTED = [
    ["python3", "-c", "pass"],
    ["python3", "script.py"],
    ["npx", "playwright", "install"],
    ["playwright", "install"],
    ["bazel", "query", "//..."],
    ["bazel"],
    ["bazel", "--output_base", "/x", "test"],   # startup options take =values; /x is read as the command
    ["/nonexistent/tool"],
    ["uv", "run", "--with", "pytest", "python", "-m", "foo"],
    ["tox", "-e", "pytest"],
    ["make", "pytest"],
    [],
    "mvn test",
    b"mvn test",
    ["pnpm", "install"],
    ["yarn", "test"],
    ["git", "status"],
    ["mvn", "-q", "verify"],   # quiet Maven prints nothing the Maven parser reads: generic
    ["./mvnw", "--quiet", "test"],
    ["git", "commit", "-m", "pytest"],   # `-m pytest` counts only after a python program
]


class DetectTest(unittest.TestCase):
    def test_detected_commands(self):
        for argv, want in DETECTED:
            with self.subTest(argv=argv):
                self.assertEqual(parsers.detect(argv, is_pytest=jr._is_pytest_command), want)

    def test_commands_left_to_the_generic_parser(self):
        for argv in NOT_DETECTED:
            with self.subTest(argv=argv):
                self.assertIsNone(parsers.detect(argv, is_pytest=jr._is_pytest_command))

    def test_pytest_behind_env_is_detected_as_juneau_run_sees_it(self):
        for argv in (["env", "CI=1", "pytest"], ["env", "-u", "X", "python", "-m", "pytest"],
                     ["env", "A=1", "uv", "run", "pytest"], ["npx", "pytest"]):
            with self.subTest(argv=argv):
                self.assertGreaterEqual(jr._pytest_index(argv), 0)
                self.assertEqual(parsers.detect(argv, is_pytest=jr._is_pytest_command), "pytest")
                if "uv" not in argv:   # the fallback check does not know runners such as uv
                    self.assertEqual(parsers.detect(argv), "pytest")

    def test_program_index_walks_past_launchers(self):
        for argv, want in [(["gradle", "x"], 0), (["env", "A=1", "gradle"], 2), (["env", "-u", "A", "gradle"], 3),
                           (["env", "-i", "-", "gradle"], 3), (["npx", "-y", "tsc"], 2), (["pnpm", "exec", "tsc"], 2),
                           (["yarn", "tsc"], 1), (["npm", "exec", "--", "tsc"], 3), (["env", "A=1"], 2), ([], 0)]:
            with self.subTest(argv=argv):
                self.assertEqual(parsers.program_index(argv), want)

    def test_juneau_run_walks_past_launchers_the_same_way(self):
        for argv in [["gradle"], ["env", "A=1", "gradle"], ["env", "--unset=A", "-i", "mvn"], ["env", "-u"],
                     ["/usr/bin/env", "A=1", "bazel"], ["npx", "-y", "tsc"], ["pnpm", "dlx", "x"], ["pnpm"],
                     ["yarn", "run", "x"], ["npm", "exec", "--", "x"], ["npm", "exec"], ["npm", "test"], ["env"],
                     ["env", "npx", "-p", "pkg", "tsc"], []]:
            with self.subTest(argv=argv):
                self.assertEqual(jr._program_index(argv), parsers.program_index(argv))

    def test_a_dash_m_pytest_needs_a_python_program(self):
        self.assertIsNone(parsers.detect(["git", "commit", "-m", "pytest"]))
        self.assertIsNone(parsers.detect(["git", "commit", "-m", "pytest"], is_pytest=jr._is_pytest_command))
        self.assertEqual(parsers.detect(["python3.12", "-m", "pytest"]), "pytest")
        self.assertEqual(parsers.detect(["env", "CI=1", "python", "-m", "pytest"]), "pytest")
        self.assertEqual(parsers.detect(["uv", "run", "python", "-m", "pytest"], is_pytest=jr._is_pytest_command),
                         "pytest")

    def test_without_juneau_runs_pytest_check_a_simple_one_is_used(self):
        self.assertEqual(parsers.detect(["pytest"]), "pytest")
        self.assertEqual(parsers.detect(["python3", "-m", "pytest"]), "pytest")
        self.assertIsNone(parsers.detect(["tox", "-e", "pytest"]))
        self.assertEqual(parsers.detect(["mvn", "verify"]), "maven")

    def test_detect_never_imports_a_parser_module(self):
        before = {n for n in sys.modules if n.startswith(parsers.__name__ + ".")}
        parsers.detect(["bazel", "build", "//..."])
        self.assertEqual({n for n in sys.modules if n.startswith(parsers.__name__ + ".")}, before)
