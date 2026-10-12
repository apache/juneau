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
pytest fixtures over juneau_scripts.testing: hermetic_git, bare_remote, clean_run_env and fake_tool.

    pytest_plugins = ["juneau_scripts.testing.pytest_plugin"]       # in a conftest.py

The only module in juneau_scripts that imports pytest.
"""

import os

import pytest

from . import bare_remote as _bare_remote
from . import clean_run_env as _clean_run_env
from . import fake_tool as _fake_tool
from . import hermetic_git as _hermetic_git


@pytest.fixture
def hermetic_git(tmp_path):
    """No inherited GIT_* variables, no global or system config, no prompts.  Yields the overrides dict (not
    tmp_path, as some projects' own hermetic_git fixtures return)."""
    with _hermetic_git(tmp_path) as overrides:
        yield overrides


@pytest.fixture
def bare_remote(tmp_path, hermetic_git):
    """(work, origin): a work repo on main with one commit, tracking a bare origin."""
    return _bare_remote(tmp_path)


@pytest.fixture
def clean_run_env():
    """No run-protocol variables and no sys.modules["juneau_run"] for the test."""
    with _clean_run_env():
        yield


@pytest.fixture
def fake_tool(tmp_path, monkeypatch):
    """A factory: fake_tool(name, **kwargs) writes a stub into a bin directory that is first on PATH."""
    bin_dir = tmp_path / "fake-bin"
    bin_dir.mkdir()
    monkeypatch.setenv("PATH", f"{bin_dir}{os.pathsep}{os.environ.get('PATH', '')}")

    def make(name, **kwargs):
        return _fake_tool(bin_dir, name, **kwargs)
    return make
