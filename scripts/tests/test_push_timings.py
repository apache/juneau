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
scripts/push-timings.py: an entry is flagged only past both margins, faster entries are marked, the env overrides
the defaults, and the full table goes to the .txt beside the log.
"""

from __future__ import annotations

import importlib.util
import json
from pathlib import Path

import pytest

SCRIPTS_DIR = Path(__file__).resolve().parent.parent


@pytest.fixture(autouse=True)
def clean_env(monkeypatch):
    for variable in ("JUNEAU_PUSH_TIMING_THRESHOLD", "JUNEAU_PUSH_TIMING_MIN_SECONDS"):
        monkeypatch.delenv(variable, raising=False)


@pytest.fixture
def timings():
    spec = importlib.util.spec_from_file_location("_push_timings", SCRIPTS_DIR / "push-timings.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def history(modules):
    """Timing records for {module: ([earlier seconds...], latest seconds)}: one earlier run per value, then the latest."""
    records = []
    for module, (prior, _) in modules.items():
        records += [{"run_id": f"r{i}", "module": module, "execution": "surefire", "wallclock_s": seconds}
                    for i, seconds in enumerate(prior)]
    records += [{"run_id": "latest", "module": module, "execution": "surefire", "wallclock_s": latest}
                for module, (_, latest) in modules.items()]
    return records


def flags(entries):
    return {entry["module"]: entry["flag"] for entry in entries}


def write_log(path, records):
    path.write_text("".join(json.dumps(record) + "\n" for record in records), encoding="utf-8")


def test_an_entry_is_flagged_only_when_slower_by_both_margins(timings):
    # +30% but only 3s; +25% and 10s; 6s but only +10%.
    entries = timings.analyse(history({"small": ([10.0] * 3, 13.0), "big": ([40.0] * 3, 50.0),
                                       "flat": ([60.0] * 3, 66.0)}))
    assert flags(entries) == {"small": None, "big": "slower", "flat": None}


def test_faster_by_both_margins_is_marked_faster(timings):
    # -40% and 20s; -50% but exactly 5s, which isn't more than 5s.
    entries = timings.analyse(history({"quick": ([50.0] * 3, 30.0), "tiny": ([10.0] * 3, 5.0)}))
    assert flags(entries) == {"quick": "faster", "tiny": None}


def test_the_defaults_are_20_percent_and_5_seconds(timings):
    assert (timings.threshold(), timings.min_seconds()) == (0.20, 5.0)


def test_the_env_overrides_both_margins(timings, monkeypatch):
    monkeypatch.setenv("JUNEAU_PUSH_TIMING_THRESHOLD", "0.05")
    monkeypatch.setenv("JUNEAU_PUSH_TIMING_MIN_SECONDS", "0")
    assert (timings.threshold(), timings.min_seconds()) == (0.05, 0.0)
    assert flags(timings.analyse(history({"m": ([10.0] * 3, 11.0)}))) == {"m": "slower"}


def test_the_baseline_is_the_median_of_the_window(timings):
    records = history({"m": ([100.0] * 5 + [10.0] * 3, 10.0)})
    assert timings.analyse(records, window=3)[0]["baseline"] == 10.0
    assert timings.analyse(records)[0]["baseline"] == 100.0


def test_a_first_run_has_no_baseline_and_is_never_flagged(timings):
    [entry] = timings.analyse(history({"new": ([], 99.0)}))
    assert (entry["baseline"], entry["delta"], entry["flag"]) == (None, 0.0, None)
    assert entry["line"] == "new/surefire: 99.0s (first run, no baseline)"


def test_failed_rows_of_the_latest_run_are_left_out(timings):
    records = history({"ok": ([10.0], 10.0)}) + [
        {"run_id": "latest", "module": "broken", "execution": "surefire", "wallclock_s": 1.0, "passed": False}]
    assert [entry["module"] for entry in timings.analyse(records)] == ["ok"]


def test_counts_collapse_the_first_runs_into_one_number(timings):
    entries = timings.analyse(history({"big": ([40.0] * 3, 50.0), "quick": ([50.0] * 3, 30.0),
                                       "a": ([], 1.0), "b": ([], 2.0)}))
    assert timings.counts(entries) == "1 slower than 20% and 5s · 1 faster · 2 without a baseline"
    assert timings.counts([]) == "0 slower than 20% and 5s"


def test_no_history_gives_no_entries(timings):
    assert timings.analyse([]) == []


def test_main_writes_the_full_table_and_prints_only_the_slower_entries(timings, tmp_path, capsys):
    log = tmp_path / "master.jsonl"
    write_log(log, history({"big": ([40.0] * 3, 50.0), "small": ([10.0] * 3, 13.0), "new": ([], 1.0)}))
    assert timings.main(["--log", str(log)]) == 0
    report = tmp_path / "master.txt"
    assert capsys.readouterr().out.splitlines() == [
        f"📊 Push-timing report (last 20 runs on this branch): 1 slower than 20% and 5s · 1 without a baseline → {report}",
        "   ⚠ big/surefire: 50.0s (median 40.0s, delta +25.0%, +10.0s)",
    ]
    assert report.read_text(encoding="utf-8").splitlines() == [
        "Push-timing report (last 20 runs on this branch)",
        " ⚠ big/surefire: 50.0s (median 40.0s, delta +25.0%, +10.0s)",
        "   new/surefire: 1.0s (first run, no baseline)",
        "   small/surefire: 13.0s (median 10.0s, delta +30.0%, +3.0s)",
        "1 slower than 20% and 5s · 1 without a baseline",
    ]


def test_the_report_marks_faster_entries(timings, tmp_path):
    report = tmp_path / "t.txt"
    timings.write_report(timings.analyse(history({"quick": ([50.0] * 3, 30.0)})), report)
    assert " ↓ quick/surefire: 30.0s (median 50.0s, delta -40.0%, -20.0s)" in report.read_text(encoding="utf-8")


def test_report_overrides_the_table_path(timings, tmp_path):
    log = tmp_path / "master.jsonl"
    write_log(log, history({"m": ([10.0], 10.0)}))
    report = tmp_path / "elsewhere" / "t.txt"
    assert timings.main(["--log", str(log), "--report", str(report)]) == 0
    assert report.exists()
    assert not (tmp_path / "master.txt").exists()


def test_main_without_history_says_so_and_writes_nothing(timings, tmp_path, capsys):
    assert timings.main(["--log", str(tmp_path / "none.jsonl")]) == 0
    assert capsys.readouterr().out == "📊 Push-timing report: no timing history yet.\n"
    assert not (tmp_path / "none.txt").exists()
