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
"""Shared helpers for the convenience-package tests (test_dist_build.py): where the builder is, and how a test that
needs an outside tool (uv, npm, ruby, twine) skips when it is missing.  With JUNEAU_TERMINAL_REQUIRE_PACKAGING_TOOLS=1
(scripts/test.py --full sets it) a missing tool fails the test instead, so a release machine cannot skip them silently."""

import os
import shutil
import subprocess
import sys
import unittest

from support import MODULE_ROOT

DIST = MODULE_ROOT / "src" / "dist"
BUILDER = DIST / "build_dist.py"
REQUIRE = os.environ.get("JUNEAU_TERMINAL_REQUIRE_PACKAGING_TOOLS") == "1"
EPOCH = "2026-01-09T19:00:20Z"


def need(tool):
    """The tool's path; when it is missing, skip (printing why) or, under REQUIRE, fail."""
    found = shutil.which(tool)
    if found:
        return found
    if REQUIRE:
        raise AssertionError(f"{tool} not found, and JUNEAU_TERMINAL_REQUIRE_PACKAGING_TOOLS=1")
    print(f"SKIPPED packaging test: {tool} not found", file=sys.stderr)
    raise unittest.SkipTest(f"{tool} not found")


def build(out, *only, version="10.0.0-SNAPSHOT", python=None, env=None):
    """Run the builder into out; returns the completed process."""
    argv = [python or sys.executable, "-B", str(BUILDER), "--version", version, "--epoch", EPOCH, "--out", str(out)]
    for kind in only:
        argv += ["--only", kind]
    return subprocess.run(argv, capture_output=True, text=True, env={**os.environ, "PYTHONDONTWRITEBYTECODE": "1", **(env or {})})
