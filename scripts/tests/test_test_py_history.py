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
"""scripts/test.py writes the run history: the tests step's record carries one surefire_s.<bucket> series per bucket
and the reactor_s module times, whether or not the tests passed.  A fake mvn on PATH; never a real build."""

from __future__ import annotations

import importlib.util
import json
import os
import re
import stat
import sys
from pathlib import Path

import pytest

SCRIPTS_DIR = Path(__file__).resolve().parent.parent
RUN_VARIABLES = ("RUN_MARKERS", "RUN_ARTIFACTS", "JUNEAU_RUN_ACTIVE", "JUNEAU_RUN_N_BASE", "JUNEAU_RUN_CONSOLE",
                 "JUNEAU_RUN_FULL_LOG", "JUNEAU_RUN_CONDENSED_LOG", "JUNEAU_MVN_WRAPPER", "JUNEAU_RUN_SESSION",
                 "JUNEAU_RUN_DETAIL", "JUNEAU_PUSH_ACTIVE")

# A class line the Maven parser attributes to Module A, so its own surefire_s series has an entry.
CLASS_LINES = """\
[INFO] -------------------------< org.example:mod-a >--------------------------
[INFO] Building Module A 1.0                                              [1/2]
[INFO] Running a.ATest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.5 s -- in a.ATest
"""


@pytest.fixture(autouse=True)
def clean_env(monkeypatch):
    for variable in RUN_VARIABLES:
        monkeypatch.delenv(variable, raising=False)
    sys.modules.pop("juneau_run", None)
    yield
    sys.modules.pop("juneau_run", None)


@pytest.fixture
def test_mod(fake_reports, monkeypatch):
    spec = importlib.util.spec_from_file_location("_history_test", SCRIPTS_DIR / "test.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    monkeypatch.setattr(module, "REPO_ROOT", fake_reports.root)
    return module


@pytest.fixture
def fake_mvn(tmp_path, monkeypatch, fake_reports):
    """A `mvn` on PATH that prints the F2 Maven output (with a class line) and exits $FAKE_MVN_EXIT (default 0)."""
    bin_dir = tmp_path / "bin"
    bin_dir.mkdir()
    output = tmp_path / "mvn-output.txt"
    output.write_text(CLASS_LINES + fake_reports.text, encoding="utf-8")
    mvn = bin_dir / "mvn"
    mvn.write_text(f"#!/bin/sh\ncat '{output}'\nexit ${{FAKE_MVN_EXIT:-0}}\n", encoding="utf-8")
    mvn.chmod(mvn.stat().st_mode | stat.S_IEXEC)
    monkeypatch.setenv("PATH", f"{bin_dir}{os.pathsep}{os.environ['PATH']}")


def _tests_record(test_mod, run_history, fake_reports):
    run_history.start(fake_reports.root)
    code, _ = test_mod.test()
    records = [r for r in run_history.store("test", repo=fake_reports.root).read() if r["step"] == "tests"]
    assert len(records) == 1
    return code, records[0]


def test_tests_record_carries_one_series_per_bucket(test_mod, fake_mvn, run_history, fake_reports):
    code, record = _tests_record(test_mod, run_history, fake_reports)
    assert code == 0
    assert record["series"]["surefire_s.core"] == {"mod-a": 4.5}
    assert record["series"]["surefire_s.container.springboot"] == {"mod-b": 7.0}
    assert record["series"]["reactor_s"] == {"mod-a": 10.0, "mod-b": 20.0}
    assert record["step"] == "tests" and record["ok"] is True
    assert record["wall_s"] is not None and record["wall_s"] >= 0


def test_the_parser_surefire_s_total_is_still_there(test_mod, fake_mvn, run_history, fake_reports):
    compiled = fake_reports.root / "mod-a" / "target" / "test-classes" / "a" / "ATest.class"
    compiled.parent.mkdir(parents=True)
    compiled.write_bytes(b"")
    _, record = _tests_record(test_mod, run_history, fake_reports)
    assert record["series"]["surefire_s"]
    assert sum(record["series"]["surefire_s"].values()) == 4.5


def test_bucket_names_match_the_history_series_name_pattern(test_mod):
    """Each surefire_s.<bucket> name matches the pattern juneau_run.history_series accepts ([a-z0-9_.]{1,64});
    history_series drops any other name silently."""
    buckets = sorted({bucket for _, bucket in test_mod.CONTAINER_MARKERS} | {"core"})
    assert buckets == ["container.jetty", "container.springboot", "container.tomcat", "core"]
    for bucket in buckets:
        assert re.fullmatch(r"[a-z0-9_.]{1,64}", f"surefire_s.{bucket}")


def test_a_failed_test_run_still_records_its_buckets_with_ok_false(test_mod, fake_mvn, run_history, fake_reports,
                                                                    monkeypatch):
    monkeypatch.setenv("FAKE_MVN_EXIT", "1")
    code, record = _tests_record(test_mod, run_history, fake_reports)
    assert code == 1
    assert record["ok"] is False
    assert record["series"]["surefire_s.core"] == {"mod-a": 4.5}


def test_the_summarizer_never_fails_the_step(test_mod, fake_mvn, run_history, fake_reports, monkeypatch):
    calls = []

    def broken(root):
        calls.append(root)
        raise RuntimeError("reports unreadable")

    monkeypatch.setattr(test_mod, "discover_module_stats", broken)
    code, record = _tests_record(test_mod, run_history, fake_reports)
    assert calls, "the summarizer never ran, so this test proves nothing"
    assert code == 0
    assert record["ok"] is True
    assert "surefire_s.core" not in (record.get("series") or {})


def test_the_tests_record_fits_max_record_on_the_real_layout(run_history, tmp_path):
    """A tripwire: an oversize record silently loses both subs and series.  Worst case on this repo's own layout:
    every module in subs and in every series test.py writes (each bucket, surefire_s, reactor_s), at full width."""
    spec = importlib.util.spec_from_file_location("_layout_test", SCRIPTS_DIR / "test.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    paths = sorted(module.module_names(module.REPO_ROOT)[0].values())
    assert len(paths) > 50, "expected the real Juneau reactor"
    jr = run_history.jr()
    per_module = {path: 1234.567 for path in paths}
    series = {"surefire_s": per_module, "reactor_s": per_module}
    series.update({f"surefire_s.{bucket}": per_module
                   for bucket in ("core", "container.springboot", "container.jetty", "container.tomcat")})
    record = {"v": 1, "ts": "2026-10-14T00:00:00Z", "step": "tests", "tool": "maven", "title": "Java tests",
              "sig": "0123456789abcdef", "commit": "abcdef1234", "ok": True, "complete": True, "wall_s": 1234.567,
              "units": {"kind": "modules", "total": len(paths), "done": len(paths)}, "tests": 99999,
              "subs": per_module, "series": series}
    size = len(json.dumps(record, separators=(",", ":")))
    assert size < 0.8 * jr.HistoryStore.MAX_RECORD, f"{size} bytes on {len(paths)} modules"
    store = run_history.store(repo=tmp_path)
    store.append(record)
    assert store.read()[-1]["series"] == series
