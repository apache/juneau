#!/usr/bin/env python3
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
"""Tests for check-lockfile-registry.py."""
import importlib.util
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[1] / "check-lockfile-registry.py"
spec = importlib.util.spec_from_file_location("check_lockfile_registry", SCRIPT)
check = importlib.util.module_from_spec(spec)
spec.loader.exec_module(check)

GOOD = '{"packages": {"node_modules/a": {\n"resolved": "https://registry.npmjs.org/a/-/a-1.0.0.tgz"}}}\n'


def test_public_registry_passes(tmp_path, capsys):
    f = tmp_path / "package-lock.json"
    f.write_text(GOOD)
    assert check.main(["--lockfile", str(f)]) == 0


def test_other_host_fails_with_line(tmp_path, capsys):
    f = tmp_path / "package-lock.json"
    f.write_text(GOOD.replace("https://registry.npmjs.org/", "https://mirror.example.net/npm/"))
    assert check.main(["--lockfile", str(f)]) == 1
    assert ":2: `resolved` URL is not on the public npm registry: https://mirror.example.net/npm/a/-/a-1.0.0.tgz" in capsys.readouterr().out


def test_missing_lockfile(tmp_path):
    assert check.main(["--lockfile", str(tmp_path / "none.json")]) == 2


def test_repo_lockfile_is_on_public_registry():
    assert check.bad_urls(check.DEFAULT_LOCKFILE) == []
