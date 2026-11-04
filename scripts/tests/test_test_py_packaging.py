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
"""scripts/test.py --full and JUNEAU_TERMINAL_REQUIRE_PACKAGING_TOOLS, which turns juneau-terminal's packaging-test skips
into failures."""

from __future__ import annotations

import importlib.util
import os
import sys
from pathlib import Path

import pytest

SCRIPTS_DIR = Path(__file__).resolve().parent.parent
REQUIRE = "JUNEAU_TERMINAL_REQUIRE_PACKAGING_TOOLS"


@pytest.fixture
def test_mod(monkeypatch):
    monkeypatch.delenv(REQUIRE, raising=False)
    spec = importlib.util.spec_from_file_location("_packaging_test", SCRIPTS_DIR / "test.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    yield module
    os.environ.pop(REQUIRE, None)       # require_packaging_tools writes os.environ directly


def test_explicit_full_sets_the_variable(test_mod):
    test_mod.require_packaging_tools(True)
    assert os.environ[REQUIRE] == "1"


def test_otherwise_it_is_left_alone(test_mod):
    test_mod.require_packaging_tools(False)
    assert REQUIRE not in os.environ


@pytest.mark.parametrize("argv,required", [(["--full"], True), ([], False), (["--test-only"], False)])
def test_main_exports_it_only_for_an_explicit_full(test_mod, monkeypatch, argv, required):
    seen = []
    monkeypatch.setattr(sys, "argv", ["test.py", "--no-js-tests", *argv])
    monkeypatch.setattr(test_mod, "build", lambda: (0, ""))
    monkeypatch.setattr(test_mod, "test", lambda no_container=False: seen.append(os.environ.get(REQUIRE)) or (0, ""))
    monkeypatch.setattr(test_mod, "run_perf_guard", lambda *a, **k: 0)
    assert test_mod._main() == 0
    assert seen == (["1"] if required else [None])
