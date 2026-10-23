# ***************************************************************************************************************************
# * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
# * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
# * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
# * with the License.  You may obtain a copy of the License at                                                              *
# *                                                                                                                         *
# *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
# *                                                                                                                         *
# * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
# * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
# * specific language governing permissions and limitations under the License.                                              *
"""
Tests for the command line of scripts/slack-notify.py: --help prints usage and exits 0 before the config is read or
any network call is made.  Nothing here touches the network.
"""

from __future__ import annotations

import importlib.util
from pathlib import Path

import pytest

SCRIPTS_DIR = Path(__file__).resolve().parent.parent


@pytest.fixture
def module():
    spec = importlib.util.spec_from_file_location("_undertest_slack_notify", SCRIPTS_DIR / "slack-notify.py")
    m = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(m)
    return m


@pytest.mark.parametrize("flag", ["--help", "-h"])
def test_help_exits_zero_without_config_or_network(module, flag, monkeypatch, capsys):
    def boom(*a, **k):
        raise AssertionError("--help must not read the config or touch the network")

    monkeypatch.setattr(module, "_load_webhook_url", boom)
    monkeypatch.setattr(module.urllib.request, "urlopen", boom)
    with pytest.raises(SystemExit) as exc:
        module.main(["slack-notify.py", flag])
    assert exc.value.code == 0
    assert "usage" in capsys.readouterr().out.lower()
