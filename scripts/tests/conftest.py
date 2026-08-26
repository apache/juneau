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
"""
Shared pytest fixtures for scripts/tests/.

scripts/todo-next-id.py and scripts/todo-status-audit.py have hyphens in their filenames, so
they cannot be `import`ed by name -- they're loaded here by file path via importlib. Each
fixture loads a brand-new module object per test (never cached/shared across tests), so a
test that monkeypatches a module-level constant (e.g. MAX_CLAIM_ATTEMPTS) can never leak
that mutation into an unrelated test.

This file (and everything else under scripts/tests/) is identical across every repo that
carries todo-next-id.py / todo-status-audit.py -- it derives the project's id letter and
tracker slug from the scripts themselves (next_id_module.ID_PROJECT_LETTER / TRACKER_SLUG),
never hardcodes them, so the same body works everywhere without per-repo edits.
"""

from __future__ import annotations

import importlib.util
from pathlib import Path

import pytest

SCRIPTS_DIR = Path(__file__).resolve().parent.parent


def _load_script(script_stem: str):
    """Load scripts/<script_stem>.py (e.g. "todo-next-id") as a fresh module object."""
    path = SCRIPTS_DIR / f"{script_stem}.py"
    spec = importlib.util.spec_from_file_location(f"_undertest_{script_stem.replace('-', '_')}", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


@pytest.fixture
def scripts_dir() -> Path:
    """Absolute path to the scripts/ directory containing the two tracker scripts under test."""
    return SCRIPTS_DIR


@pytest.fixture
def next_id_module():
    """A fresh scripts/todo-next-id.py module object (see module docstring for why "fresh")."""
    return _load_script("todo-next-id")


@pytest.fixture
def audit_module():
    """A fresh scripts/todo-status-audit.py module object (see module docstring for why "fresh")."""
    return _load_script("todo-status-audit")
