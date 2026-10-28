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
import os

import pytest

import release  # scripts/release.py, importable via conftest's sys.path

pytestmark = pytest.mark.usefixtures("clean_env")


def test_unset_env_leaves_full_log_to_the_session(tmp_path, monkeypatch):
    monkeypatch.delenv("JUNEAU_TERMINAL_FULL_LOG", raising=False)
    assert release._external_full_log(tmp_path) is None
    assert not (tmp_path / "full.log").exists()


def test_empty_env_counts_as_unset(tmp_path, monkeypatch):
    monkeypatch.setenv("JUNEAU_TERMINAL_FULL_LOG", "")
    assert release._external_full_log(tmp_path) is None


def test_set_env_links_attempt_full_log_to_it(tmp_path, monkeypatch):
    attempt = tmp_path / "attempt"
    attempt.mkdir()
    (attempt / "full.log").touch()                      # the empty file release.py used to create
    log = tmp_path / "watch" / "full.log"
    monkeypatch.setenv("JUNEAU_TERMINAL_FULL_LOG", str(log))
    assert release._external_full_log(attempt) == log
    assert (attempt / "full.log").is_symlink()
    assert (attempt / "full.log").resolve() == log.resolve()
    log.write_text("hello\n")
    assert (attempt / "full.log").read_text() == "hello\n"


def test_unlinkable_log_falls_back_with_a_warning(tmp_path, monkeypatch, capsys):
    attempt = tmp_path / "attempt"
    attempt.mkdir()
    (attempt / "full.log").write_text("earlier output\n")   # a non-empty file is never replaced
    monkeypatch.setenv("JUNEAU_TERMINAL_FULL_LOG", str(tmp_path / "elsewhere.log"))
    assert release._external_full_log(attempt) is None
    assert "could not link" in capsys.readouterr().err
    assert (attempt / "full.log").read_text() == "earlier output\n"


def test_scratch_attempt_replaces_an_earlier_log(tmp_path, monkeypatch):
    attempt = tmp_path / "dry-run"
    attempt.mkdir()
    (attempt / "full.log").write_text("previous dry run\n")   # attempts/dry-run is reused, so its old log is scratch
    log = tmp_path / "watch.log"
    monkeypatch.setenv("JUNEAU_TERMINAL_FULL_LOG", str(log))
    assert release._external_full_log(attempt, scratch=True) == log
    assert (attempt / "full.log").resolve() == log.resolve()
