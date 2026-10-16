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
the defaults, the rows come from test.py's `tests` records in the repo's run history, and the full table goes to
test-timings.txt beside the store.
"""

from __future__ import annotations

import importlib.util
import json
import os
import subprocess
import sys
from pathlib import Path

import pytest

SCRIPTS_DIR = Path(__file__).resolve().parent.parent


@pytest.fixture(autouse=True)
def clean_env(monkeypatch):
    for variable in ("JUNEAU_PUSH_TIMING_THRESHOLD", "JUNEAU_PUSH_TIMING_MIN_SECONDS"):
        monkeypatch.delenv(variable, raising=False)


@pytest.fixture
def timings():
    saved = sys.dont_write_bytecode      # the shared loader sets it
    spec = importlib.util.spec_from_file_location("_push_timings", SCRIPTS_DIR / "push-timings.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    try:
        yield module
    finally:
        sys.dont_write_bytecode = saved


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


def store_records(make, modules, wall_s=100.0):
    """test.py `tests` records for {module: ([earlier seconds...], latest seconds)}, bucket "surefire": one earlier
    record per value, then the latest; every record's wall_s is wall_s, so reactor/suite never moves."""
    depth = max(len(prior) for prior, _ in modules.values())
    records = []
    for i in range(depth):
        values = {m: prior[i] for m, (prior, _) in modules.items() if i < len(prior)}
        records.append(make(ts=f"2026-10-14T00:00:{i:02d}Z", wall_s=wall_s, series={"surefire_s.surefire": values}))
    records.append(make(ts="2026-10-14T00:01:00Z", wall_s=wall_s,
                        series={"surefire_s.surefire": {m: latest for m, (_, latest) in modules.items()}}))
    return records


def fill(store, records):
    for record in records:
        store.append(record)
    return store


def test_main_reads_the_repo_store_by_default(timings, tmp_path, capsys, run_history, history_record, monkeypatch):
    store = fill(run_history.store(repo=tmp_path), store_records(
        history_record, {"big": ([40.0] * 3, 50.0), "small": ([10.0] * 3, 13.0), "new": ([], 1.0)}))
    monkeypatch.setattr(timings, "REPO_ROOT", tmp_path)
    assert timings.main([]) == 0
    report = store.path.with_name("test-timings.txt")
    assert capsys.readouterr().out.splitlines() == [
        f"📊 Push-timing report (last 20 runs in this repo): 1 slower than 20% and 5s · 1 without a baseline → {report}",
        "   ⚠ big/surefire: 50.0s (median 40.0s, delta +25.0%, +10.0s)",
    ]
    assert report.read_text(encoding="utf-8").splitlines() == [
        "Push-timing report (last 20 runs in this repo)",
        " ⚠ big/surefire: 50.0s (median 40.0s, delta +25.0%, +10.0s)",
        "   new/surefire: 1.0s (first run, no baseline)",
        "   reactor/suite: 100.0s (median 100.0s, delta +0.0%, +0.0s)",
        "   small/surefire: 13.0s (median 10.0s, delta +30.0%, +3.0s)",
        "1 slower than 20% and 5s · 1 without a baseline",
    ]


def test_report_wording_no_longer_says_this_branch(timings, tmp_path, capsys, run_history, history_record):
    fill(run_history.store(repo=tmp_path), store_records(history_record, {"m": ([10.0], 10.0)}))
    report = tmp_path / "t.txt"
    assert timings.main(["--repo", str(tmp_path), "--report", str(report)]) == 0
    out = capsys.readouterr().out + report.read_text(encoding="utf-8")
    assert "this branch" not in out
    assert out.count("in this repo") == 2


def test_report_is_written_beside_the_store(timings, tmp_path, run_history, history_record):
    store = fill(run_history.store(repo=tmp_path), store_records(history_record, {"m": ([10.0], 10.0)}))
    assert timings.main(["--repo", str(tmp_path)]) == 0
    assert timings.report_path(tmp_path) == store.path.with_name("test-timings.txt")
    assert store.path.with_name("test-timings.txt").exists()


def test_report_overrides_the_table_path(timings, tmp_path, run_history, history_record):
    store = fill(run_history.store(repo=tmp_path), store_records(history_record, {"m": ([10.0], 10.0)}))
    report = tmp_path / "elsewhere" / "t.txt"
    assert timings.main(["--repo", str(tmp_path), "--report", str(report)]) == 0
    assert report.exists()
    assert not store.path.with_name("test-timings.txt").exists()


def test_history_reads_a_records_file(timings, tmp_path, capsys, history_record):
    path = tmp_path / "copy.jsonl"
    write_log(path, store_records(history_record, {"m": ([10.0], 10.0)}))
    assert timings.main(["--history", str(path)]) == 0
    assert "0 slower than 20% and 5s" in capsys.readouterr().out
    assert (tmp_path / "copy-timings.txt").exists()


def test_main_without_history_says_so_and_writes_nothing(timings, tmp_path, capsys, run_history):
    assert timings.main(["--repo", str(tmp_path)]) == 0
    assert capsys.readouterr().out == "📊 Push-timing report: no timing history yet.\n"
    assert not timings.report_path(tmp_path).exists()


def test_main_with_history_off_says_so(timings, tmp_path, capsys, run_history, history_record, monkeypatch):
    store = fill(run_history.store(repo=tmp_path), store_records(history_record, {"m": ([10.0], 10.0)}))
    monkeypatch.setenv("JUNEAU_RUN_HISTORY", "0")
    assert timings.main(["--repo", str(tmp_path)]) == 0
    assert capsys.readouterr().out == "📊 Push-timing report: run history is off (JUNEAU_RUN_HISTORY=0).\n"
    assert not store.path.with_name("test-timings.txt").exists()


def test_the_log_option_is_gone(timings, tmp_path):
    with pytest.raises(SystemExit):
        timings.main(["--log", str(tmp_path / "master.jsonl")])


def test_module_rows_are_not_part_of_the_timing_report(timings):
    records = history({"mod": ([10.0], 10.0)})
    records.insert(-1, {"run_id": "latest", "module": "juneau-rest/juneau-rest-mock", "execution": "module",
                        "wallclock_s": 9.0})
    rows, _ = timings.latest_run(records)
    assert [r["execution"] for r in rows] == ["surefire"]
    assert [e["module"] for e in timings.analyse(records)] == ["mod"]


# ---- characterization: the verdicts the store-backed report must reproduce --------------------------------------------
def golden_rows():
    """Six runs in today's row format: mod-a/core is slower on the last run; module rows are left out of the report."""
    rows = []
    for i in range(6):
        last = i == 5
        rows += [{"run_id": f"r{i}", "passed": True, "module": "mod-a", "execution": "core",
                  "wallclock_s": 20.0 if last else 10.0 + i * 0.1},
                 {"run_id": f"r{i}", "passed": True, "module": "mod-b", "execution": "container.springboot",
                  "wallclock_s": 7.0},
                 {"run_id": f"r{i}", "passed": True, "module": "mod-a", "execution": "module", "wallclock_s": 30.0 + i},
                 {"run_id": f"r{i}", "passed": True, "module": "reactor", "execution": "suite",
                  "wallclock_s": 100.0 + i}]
    return rows


GOLDEN_ENTRIES = [
    {"module": "mod-a", "execution": "core", "seconds": 20.0, "baseline": 10.2, "delta": 0.9607843137254903,
     "flag": "slower", "line": "mod-a/core: 20.0s (median 10.2s, delta +96.1%, +9.8s)"},
    {"module": "mod-b", "execution": "container.springboot", "seconds": 7.0, "baseline": 7.0, "delta": 0.0,
     "flag": None, "line": "mod-b/container.springboot: 7.0s (median 7.0s, delta +0.0%, +0.0s)"},
    {"module": "reactor", "execution": "suite", "seconds": 105.0, "baseline": 102.0, "delta": 0.029411764705882353,
     "flag": None, "line": "reactor/suite: 105.0s (median 102.0s, delta +2.9%, +3.0s)"},
]
GOLDEN_COUNTS = "1 slower than 20% and 5s"


def test_analyse_matches_the_golden_verdicts(timings):
    entries = timings.analyse(golden_rows())
    assert entries == GOLDEN_ENTRIES
    assert timings.counts(entries) == GOLDEN_COUNTS


# ---- the store adapter --------------------------------------------------------------------------------------------------
def golden_records(make):
    """C10's six runs as test.py `tests` records: the buckets as surefire_s.<bucket> series, the suite in wall_s, and
    the module reactor times (left out of the report) in reactor_s; plus the parser's surefire_s total, also left out."""
    records = []
    for i in range(6):
        last = i == 5
        core = 20.0 if last else 10.0 + i * 0.1
        records.append(make(ts=f"2026-10-14T00:00:{i:02d}Z", commit=f"c{i}", wall_s=100.0 + i, series={
            "surefire_s.core": {"mod-a": core}, "surefire_s.container.springboot": {"mod-b": 7.0},
            "surefire_s": {"mod-a": core, "mod-b": 7.0}, "reactor_s": {"mod-a": 30.0 + i}}))
    return records


def test_rows_from_records_matches_todays_row_shape(timings, history_record):
    record = history_record(ts="2026-10-14T01:02:03Z", commit="abc1234", ok=False, wall_s=100.0, series={
        "surefire_s.core": {"mod-a": 4.5}, "surefire_s.container.springboot": {"mod-b": 7.0},
        "surefire_s": {"mod-a": 4.5, "mod-b": 7.0}, "reactor_s": {"mod-a": 10.0}})
    other = history_record(step="build", wall_s=9.0)
    run_id = "2026-10-14T01:02:03Z-abc1234"
    assert sorted(timings.rows_from_records([other, record]), key=lambda r: r["module"]) == [
        {"run_id": run_id, "passed": False, "module": "mod-a", "execution": "core", "wallclock_s": 4.5},
        {"run_id": run_id, "passed": False, "module": "mod-b", "execution": "container.springboot", "wallclock_s": 7.0},
        {"run_id": run_id, "passed": False, "module": "reactor", "execution": "suite", "wallclock_s": 100.0},
    ]


def test_an_ok_run_is_passed_whatever_its_complete_flag(timings, history_record):
    """passed is the record's ok; complete is ignored."""
    record = history_record(ok=True, complete=False, wall_s=100.0, series={"surefire_s.core": {"mod-a": 4.5}})
    assert {row["passed"] for row in timings.rows_from_records([record])} == {True}


def test_store_records_give_the_golden_verdicts(timings, history_record):
    entries = timings.analyse(timings.rows_from_records(golden_records(history_record)))
    assert entries == GOLDEN_ENTRIES
    assert timings.counts(entries) == GOLDEN_COUNTS


def test_only_records_with_the_newest_sig_count(timings, history_record):
    """A --no-container or wrapped run has another sig: it is left out of the baseline of the newest command shape."""
    records = [history_record(ts=f"2026-10-14T00:00:{i:02d}Z", sig="other", wall_s=500.0,
                              series={"surefire_s.core": {"m": 50.0}}) for i in range(3)]
    records += [history_record(ts=f"2026-10-14T00:01:{i:02d}Z", sig="mine", wall_s=100.0,
                               series={"surefire_s.core": {"m": 10.0}}) for i in range(3)]
    entries = timings.analyse(timings.rows_from_records(records))
    assert [(e["module"], e["baseline"]) for e in entries] == [("m", 10.0), ("reactor", 100.0)]


def test_the_newest_sig_is_the_filter_even_when_older_runs_had_another(timings, history_record):
    """The newest record names the shape: the older sig's runs drop out even when they are the majority."""
    records = [history_record(ts="2026-10-14T00:00:00Z", sig="mine", wall_s=100.0, series={}),
               history_record(ts="2026-10-14T00:01:00Z", sig="other", wall_s=500.0, series={}),
               history_record(ts="2026-10-14T00:02:00Z", sig="other", wall_s=500.0, series={})]
    assert {row["wallclock_s"] for row in timings.rows_from_records(records)} == {500.0}


def test_a_newest_sig_with_too_few_runs_has_no_verdict(timings, tmp_path, capsys, run_history, history_record,
                                                       monkeypatch):
    """Many runs of another sig do not give the newest command shape a baseline: one line, no report."""
    records = [history_record(ts=f"2026-10-14T00:00:{i:02d}Z", sig="other", wall_s=500.0,
                              series={"surefire_s.core": {"m": 50.0}}) for i in range(6)]
    records.append(history_record(ts="2026-10-14T00:01:00Z", sig="mine", wall_s=100.0,
                                  series={"surefire_s.core": {"m": 10.0}}))
    store = fill(run_history.store(repo=tmp_path), records)
    monkeypatch.setattr(timings, "REPO_ROOT", tmp_path)
    assert timings.main([]) == 0
    assert capsys.readouterr().out == "📊 Push-timing report: no baseline for this command shape (1 run).\n"
    assert not store.path.with_name("test-timings.txt").exists()


def test_a_new_branch_sees_the_repos_history(timings, tmp_path, run_history, history_record):
    repo = tmp_path / "repo"
    repo.mkdir()
    git = {**os.environ, "GIT_CONFIG_GLOBAL": os.devnull, "GIT_CONFIG_NOSYSTEM": "1"}
    subprocess.run(["git", "init", "-q", "-b", "a", str(repo)], check=True, env=git)
    fill(run_history.store(repo=repo), store_records(history_record, {"m": ([10.0] * 3, 10.0)}))
    subprocess.run(["git", "-C", str(repo), "switch", "-q", "-c", "b"], check=True, env=git)
    rows = timings.load_history(repo)
    assert rows
    entries = timings.analyse(rows)
    assert all(e["baseline"] is not None for e in entries)
