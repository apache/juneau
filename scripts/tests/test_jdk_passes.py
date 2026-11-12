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
"""Tests for jdk_passes.py: the record of which JDKs a commit passed the build gate on."""

import json
import subprocess
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
import jdk_passes  # noqa: E402

SHA = "a" * 40
NOW = datetime(2026, 11, 6, 12, 0, tzinfo=timezone.utc)


@pytest.fixture
def file(tmp_path):
    return tmp_path / "sub" / "jdk-passes.json"


@pytest.mark.parametrize("line,expected", [
    ('openjdk version "17.0.12" 2024-07-16', "17"),
    ('openjdk version "1.8.0_402"', "8"),
    ('openjdk version "28-ea" 2027-03-16', "28-ea"),
    ("21.0.3", "21"),
    ("", None),
    ("garbage", None),
])
def test_major(line, expected):
    assert jdk_passes.major(line) == expected


def test_path_honors_env(tmp_path, monkeypatch):
    assert jdk_passes.path() == tmp_path / "jdk-passes.json"
    monkeypatch.delenv("JUNEAU_JDK_PASSES")
    assert jdk_passes.path().name == "jdk-passes.json"
    assert ".cache" in str(jdk_passes.path())


def test_record_and_read(file):
    assert jdk_passes.record(SHA, "21", "push", file=file, now=NOW)
    assert jdk_passes.record(SHA, "17", "test", file=file, now=NOW)
    assert jdk_passes.record(SHA, "28-ea", "test", file=file, now=NOW)
    assert jdk_passes.passes(SHA, file=file) == ["17", "21", "28-ea"]
    entry = json.loads(file.read_text())[SHA][0]
    assert set(entry) == {"jdk", "time", "source"}
    assert list(file.parent.glob(".jdk-passes-*")) == []


def test_record_replaces_same_jdk(file):
    jdk_passes.record(SHA, "21", "test", file=file, now=NOW)
    jdk_passes.record(SHA, "21", "push", file=file, now=NOW)
    entries = json.loads(file.read_text())[SHA]
    assert len(entries) == 1
    assert entries[0]["source"] == "push"


def test_missing_and_corrupt_file(file):
    assert jdk_passes.passes(SHA, file=file) == []
    file.parent.mkdir(parents=True)
    file.write_text("{not json")
    assert jdk_passes.passes(SHA, file=file) == []
    assert jdk_passes.record(SHA, "21", "push", file=file, now=NOW)
    assert jdk_passes.passes(SHA, file=file) == ["21"]
    file.write_text("[1, 2]")
    assert jdk_passes.passes(SHA, file=file) == []


def test_ignores_nothing_to_record(file):
    assert not jdk_passes.record(None, "21", "push", file=file)
    assert not jdk_passes.record(SHA, None, "push", file=file)
    assert not file.exists()


def test_prunes_old_entries(file):
    old = NOW - timedelta(days=jdk_passes.MAX_AGE_DAYS + 1)
    jdk_passes.record("b" * 40, "17", "push", file=file, now=old)
    jdk_passes.record(SHA, "21", "push", file=file, now=NOW)
    assert jdk_passes.passes("b" * 40, file=file) == []
    assert jdk_passes.passes(SHA, file=file) == ["21"]


def test_prunes_to_newest_commits(file, monkeypatch):
    monkeypatch.setattr(jdk_passes, "MAX_COMMITS", 3)
    for i in range(5):
        jdk_passes.record(f"{i}" * 40, "21", "push", file=file, now=NOW + timedelta(minutes=i))
    assert sorted(json.loads(file.read_text())) == ["2" * 40, "3" * 40, "4" * 40]


def test_unwritable_location_returns_false(tmp_path):
    blocker = tmp_path / "blocker"
    blocker.write_text("x")
    assert not jdk_passes.record(SHA, "21", "push", file=blocker / "jdk-passes.json", now=NOW)


def _git(repo, *args):
    subprocess.run(["git", *args], cwd=repo, check=True, capture_output=True)


def test_head_sha_and_require_clean(tmp_path):
    repo = tmp_path / "repo"
    repo.mkdir()
    _git(repo, "init", "-q")
    (repo / "f").write_text("1")
    _git(repo, "add", "f")
    _git(repo, "-c", "user.name=t", "-c", "user.email=t@example.com", "commit", "-qm", "c")
    sha = jdk_passes.head_sha(repo)
    assert len(sha) == 40
    assert jdk_passes.head_sha(repo, require_clean=True) == sha
    (repo / "f").write_text("2")
    assert jdk_passes.head_sha(repo, require_clean=True) is None
    assert jdk_passes.head_sha(tmp_path / "nowhere") is None


def test_jdk_label_runs_java(tmp_path):
    home = tmp_path / "jdk"
    (home / "bin").mkdir(parents=True)
    java = home / "bin" / "java"
    java.write_text('#!/bin/sh\necho \'openjdk version "28-ea" 2027-03-16\' >&2\n')
    java.chmod(0o755)
    assert jdk_passes.jdk_label(home) == "28-ea"
    assert jdk_passes.jdk_label(tmp_path / "missing") is None


def test_record_head_uses_env_file(tmp_path, monkeypatch):
    repo = tmp_path / "repo"
    repo.mkdir()
    _git(repo, "init", "-q")
    (repo / "f").write_text("1")
    _git(repo, "add", "f")
    _git(repo, "-c", "user.name=t", "-c", "user.email=t@example.com", "commit", "-qm", "c")
    monkeypatch.setattr(jdk_passes, "jdk_label", lambda home=None: "21")
    assert jdk_passes.record_head(repo, "push")
    assert jdk_passes.passes(jdk_passes.head_sha(repo)) == ["21"]
