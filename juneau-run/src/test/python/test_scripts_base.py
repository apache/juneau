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
"""juneau_scripts package basics: Result, the juneau_run lookup, posix_only and the bootstrap API check."""

import dataclasses
import io
import os
import sys
import unittest
from contextlib import redirect_stderr
from unittest import mock

import scripts_support
from scripts_support import PYTHON_SRC

import juneau_scripts
from juneau_scripts import _base
from juneau_scripts._base import Result


class ResultTest(unittest.TestCase):
    def test_defaults(self):
        r = Result(True, 0)
        self.assertEqual((r.out, r.err, r.ms, r.summary, r.status), ("", "", None, None, None))

    def test_frozen(self):
        with self.assertRaises(dataclasses.FrozenInstanceError):
            Result(True, 0).ok = False

    def test_exported_from_the_package(self):
        self.assertIs(juneau_scripts.Result, Result)


class LookupTest(unittest.TestCase):
    def test_jr_is_looked_up_at_call_time(self):
        sentinel = object()
        with mock.patch.dict(sys.modules, {"juneau_run": sentinel}):
            self.assertIs(_base.jr(), sentinel)
        saved = sys.modules.pop("juneau_run")
        try:
            self.assertIsNone(_base.jr())
        finally:
            sys.modules["juneau_run"] = saved

    def test_posix_only_raises_on_windows(self):
        with mock.patch.object(os, "name", "nt"):
            with self.assertRaises(NotImplementedError):
                _base.posix_only("lock.flock")
        _base.posix_only("lock.flock")

    def test_last_title(self):
        _base.remember("mvn verify")
        self.assertEqual(_base.last_title(), "mvn verify")


class BootstrapCheckTest(unittest.TestCase):
    def setUp(self):
        self.addCleanup(setattr, juneau_scripts, "BOOTSTRAP_API", juneau_scripts.BOOTSTRAP_API)
        self.addCleanup(setattr, juneau_scripts, "_warned", False)
        juneau_scripts._warned = False

    def _check(self):
        err = io.StringIO()
        with redirect_stderr(err):
            juneau_scripts.check_bootstrap()
            juneau_scripts.check_bootstrap()
        return err.getvalue()

    def test_current_api_is_silent(self):
        juneau_scripts.BOOTSTRAP_API = juneau_scripts.REQUIRED_BOOTSTRAP_API
        self.assertEqual(self._check(), "")

    def test_missing_or_old_api_warns_once_with_the_remedy(self):
        for api in (None, 0):
            with self.subTest(api=api):
                juneau_scripts._warned = False
                juneau_scripts.BOOTSTRAP_API = api
                err = self._check()
                self.assertEqual(err.count("warning:"), 1)
                self.assertIn("unzip -p", err)
                self.assertIn("bootstrap.py", err)

    def test_package_import_pulls_in_nothing_heavy(self):
        code = ("import sys; sys.path.insert(0, sys.argv[1]); import juneau_scripts; "
                "print(sorted(m for m in sys.modules if m.startswith('juneau_scripts') or m == 'pytest'))")
        r = scripts_support.python(["-c", code, str(PYTHON_SRC)])
        self.assertEqual(r.returncode, 0, r.stderr)
        self.assertEqual(r.stdout.strip(), "['juneau_scripts', 'juneau_scripts._base']")


if __name__ == "__main__":
    unittest.main()
