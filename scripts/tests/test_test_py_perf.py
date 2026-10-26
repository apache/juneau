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
"""scripts/test.py's perf guard: reactor wall clock per module, the two-margin rule, baselines and --rebaseline."""

from __future__ import annotations

import importlib.util
import sys
from pathlib import Path

import pytest

SCRIPTS_DIR = Path(__file__).resolve().parent.parent


@pytest.fixture
def test_mod():
    sys.modules.pop("juneau_terminal", None)
    spec = importlib.util.spec_from_file_location("_perf_test", SCRIPTS_DIR / "test.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    yield module
    sys.modules.pop("juneau_terminal", None)


def pom(directory: Path, name: str | None, parent_name: str = "Apache Juneau", artifact: str = "a"):
    directory.mkdir(parents=True, exist_ok=True)
    own = f"<name>{name}</name>" if name is not None else ""
    (directory / "pom.xml").write_text(
        '<project xmlns="http://maven.apache.org/POM/4.0.0">'
        f"<parent><artifactId>p</artifactId><name>{parent_name}</name></parent>"
        f"<artifactId>{artifact}</artifactId>{own}</project>", encoding="utf-8")


REACTOR = """\
[INFO] Apache Juneau Marshall ............................. SUCCESS [ 24.8 s]
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for Apache Juneau 10.0.0-SNAPSHOT:
[INFO]
[INFO] Apache Juneau ...................................... SUCCESS [  0.215 s]
[INFO] Apache Juneau Marshall ............................. SUCCESS [ 24.789 s]
[INFO] Apache Juneau Integration Tests .................... SUCCESS [01:13 min]
[INFO] Apache Juneau Console Views .................... FAILURE [ 35.0 s]
[INFO] Apache Juneau Distribution ......................... SKIPPED
[INFO] \x1b[1mApache Juneau REST Mock\x1b[m ........................... SUCCESS [  9.012 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
"""


# ---- measuring ---------------------------------------------------------------------------------------------------------
def test_reactor_times_reads_seconds_and_minutes_from_the_summary_only(test_mod):
    assert test_mod.reactor_times(REACTOR) == {
        "Apache Juneau": 0.215,
        "Apache Juneau Marshall": 24.789,
        "Apache Juneau Integration Tests": 73.0,
        "Apache Juneau REST Mock": 9.012,
    }


def test_reactor_times_without_a_summary_is_empty(test_mod):
    assert test_mod.reactor_times("[INFO] BUILD SUCCESS\n") == {}


def test_module_names_use_the_projects_own_name_and_report_duplicates(test_mod, tmp_path):
    pom(tmp_path, "Apache Juneau")                                      # the root pom is skipped
    pom(tmp_path / "juneau-rest" / "juneau-rest-mock", "Apache Juneau REST Mock")
    pom(tmp_path / "juneau-core" / "juneau-marshall-rdf", None, artifact="juneau-marshall-rdf")
    pom(tmp_path / "x" / "one", "Twin")
    pom(tmp_path / "x" / "two", "Twin")
    pom(tmp_path / "juneau-rest" / "juneau-rest-mock" / "target" / "copy", "Apache Juneau REST Mock")
    pom(tmp_path / "m" / "src" / "test" / "resources" / "fixture", "Fixture")
    names, ambiguous = test_mod.module_names(tmp_path)
    assert names == {"Apache Juneau REST Mock": "juneau-rest/juneau-rest-mock",
                     "juneau-marshall-rdf": "juneau-core/juneau-marshall-rdf"}
    assert ambiguous == ["Twin"]


def test_build_overlay_poms_are_not_modules(test_mod, tmp_path):
    pom(tmp_path / "juneau-examples" / "juneau-examples-core", "Apache Juneau Core Examples")
    pom(tmp_path / "juneau-examples" / "juneau-examples-core" / "build-overlay", "Apache Juneau Core Examples")
    names, ambiguous = test_mod.module_names(tmp_path)
    assert names == {"Apache Juneau Core Examples": "juneau-examples/juneau-examples-core"}
    assert ambiguous == []


def test_module_times_maps_names_to_paths_and_lists_the_rest(test_mod, tmp_path):
    pom(tmp_path, "Apache Juneau")
    pom(tmp_path / "juneau-rest" / "juneau-rest-mock", "Apache Juneau REST Mock")
    times, unmapped = test_mod.module_times(REACTOR, tmp_path)
    assert times == {"juneau-rest/juneau-rest-mock": 9.012}
    assert unmapped == ["Apache Juneau", "Apache Juneau Integration Tests", "Apache Juneau Marshall"]


def test_module_times_of_an_empty_run_does_not_walk_the_tree(test_mod, monkeypatch):
    monkeypatch.setattr(test_mod, "module_names", lambda root: pytest.fail("walked the tree"))
    assert test_mod.module_times("", Path("/nonexistent")) == ({}, [])


# ---- baselines ---------------------------------------------------------------------------------------------------------
BASELINE = """\
# header comment
suite = 185                 # trailing comment
juneau-old/core = 9         # outside [modules]: ignored

[modules]
juneau-rest/juneau-rest-mock = 9.5
juneau-core/juneau-marshall = 25

# ─── [observability] ───
[observability]
observability-noop-alloc-bytes-per-op = 0.000
"""


def test_read_baselines_reads_suite_and_the_modules_section_only(test_mod, tmp_path):
    path = tmp_path / "perf-baseline.txt"
    path.write_text(BASELINE, encoding="utf-8")
    assert test_mod.read_baselines(path) == {
        "suite": 185.0, "juneau-rest/juneau-rest-mock": 9.5, "juneau-core/juneau-marshall": 25.0}


# ---- the module rule ---------------------------------------------------------------------------------------------------
def guard(test_mod, tmp_path, baseline_text, times, elapsed=100.0, enforce=False):
    path = tmp_path / "perf-baseline.txt"
    path.write_text(baseline_text, encoding="utf-8")
    report = tmp_path / "perf-guard.txt"
    code = test_mod.run_perf_guard(elapsed, path, times, enforce=enforce, report_file=report)
    return code, report.read_text(encoding="utf-8")


@pytest.mark.parametrize("baseline,actual,breach", [
    (10, 12.5, False),      # over 20% but only 2.5s over: under the 3s floor
    (100, 110, False),      # 10s over but only 10%: under the relative margin
    (10, 14, True),         # 40% and 4s over: both margins
    (10, 13, False),        # exactly on the threshold max(12, 13) = 13: not over
])
def test_a_module_breaches_only_past_both_margins(test_mod, tmp_path, monkeypatch, baseline, actual, breach):
    monkeypatch.delenv("JUNEAU_CI_PERF_THRESHOLD", raising=False)
    monkeypatch.delenv("JUNEAU_CI_PERF_MIN_SECONDS", raising=False)
    code, text = guard(test_mod, tmp_path, f"suite = 1000\n[modules]\nm = {baseline}\n", {"m": actual}, enforce=True)
    assert ("PERF-GUARD FAIL [m]" in text) is breach
    assert code == (1 if breach else 0)


def test_the_min_seconds_margin_comes_from_the_environment(test_mod, tmp_path, monkeypatch):
    monkeypatch.setenv("JUNEAU_CI_PERF_MIN_SECONDS", "1")
    _, text = guard(test_mod, tmp_path, "suite = 1000\n[modules]\nm = 10\n", {"m": 12.5})
    assert "PERF-GUARD FAIL [m]: 12.5s (baseline 10.0s, tolerance ±20% and 1s, threshold 12.0s)." in text


def test_the_suite_keeps_the_relative_rule_alone(test_mod, tmp_path, monkeypatch):
    monkeypatch.delenv("JUNEAU_CI_PERF_MIN_SECONDS", raising=False)
    _, text = guard(test_mod, tmp_path, "suite = 10\n[modules]\n", {}, elapsed=12.5)
    assert "PERF-GUARD FAIL [suite]: tests took 12.5s (baseline 10.0s, tolerance ±20%, threshold 12.0s)." in text


def test_unmapped_reactor_names_are_listed_once_in_the_report(test_mod, tmp_path):
    path = tmp_path / "perf-baseline.txt"
    path.write_text("suite = 1000\n[modules]\n", encoding="utf-8")
    report = tmp_path / "perf-guard.txt"
    test_mod.run_perf_guard(1.0, path, {}, enforce=False, report_file=report, unmapped=["Twin", "Other"])
    assert report.read_text(encoding="utf-8").count(
        "PERF-GUARD: 2 reactor module(s) not mapped to a pom (name shared or missing): Twin, Other") == 1


# ---- the JSONL timing log is gone --------------------------------------------------------------------------------------
def test_main_no_longer_writes_a_jsonl(test_mod, monkeypatch, tmp_path):
    """The reactor times reach the guard from the summarizer's _last_times, and nothing is written under
    ~/.cache/juneau-push-timings."""
    seen = {}
    monkeypatch.setenv("HOME", str(tmp_path))
    monkeypatch.setattr(sys, "argv", ["test.py", "--test-only", "--no-js-tests"])

    def fake_test(no_container=False):
        test_mod._last_times = ({"m": 1.0}, ["u"])
        return 0, "REACTOR"
    monkeypatch.setattr(test_mod, "test", fake_test)
    monkeypatch.setattr(test_mod, "module_times", lambda output, root: pytest.fail("re-parsed the reactor summary"))
    monkeypatch.setattr(test_mod, "run_perf_guard",
                        lambda elapsed, baseline, times, enforce, unmapped=(): seen.setdefault("guard", (times, unmapped)) and 0)
    assert test_mod._main() == 0
    assert seen == {"guard": ({"m": 1.0}, ["u"])}
    assert not (tmp_path / ".cache" / "juneau-push-timings").exists()
    assert not hasattr(test_mod, "write_timing_log") and not hasattr(test_mod, "default_timing_log")


def test_main_falls_back_to_parsing_the_output_when_the_summarizer_did_not_run(test_mod, monkeypatch):
    seen = {}
    monkeypatch.setattr(sys, "argv", ["test.py", "--test-only", "--no-js-tests"])
    monkeypatch.setattr(test_mod, "test", lambda no_container=False: (0, "REACTOR"))
    monkeypatch.setattr(test_mod, "module_times", lambda output, root: ({"m": 1.0}, ["u"]) if output == "REACTOR" else None)
    monkeypatch.setattr(test_mod, "run_perf_guard",
                        lambda elapsed, baseline, times, enforce, unmapped=(): seen.setdefault("guard", (times, unmapped)) and 0)
    assert test_mod._main() == 0
    assert seen == {"guard": ({"m": 1.0}, ["u"])}


def test_timing_log_flag_is_accepted_and_ignored_with_a_warning(test_mod, monkeypatch, tmp_path, capsys):
    log = tmp_path / "t.jsonl"
    monkeypatch.setattr(sys, "argv", ["test.py", "--test-only", "--no-js-tests", "--timing-log", str(log)])
    monkeypatch.setattr(test_mod, "test", lambda no_container=False: (0, "REACTOR"))
    monkeypatch.setattr(test_mod, "module_times", lambda output, root: ({}, []))
    monkeypatch.setattr(test_mod, "run_perf_guard", lambda *a, **k: 0)
    assert test_mod._main() == 0
    out = capsys.readouterr().out
    assert out.count("--timing-log is ignored") == 1
    assert not log.exists()


# ---- --rebaseline ------------------------------------------------------------------------------------------------------
def history_records(make, runs):
    """runs: [(passed, suite seconds, {module: seconds})]; one `tests` store record each, in order (the suite time in
    wall_s, the module times in series.reactor_s)."""
    return [make(ts=f"2026-10-14T00:00:{i:02d}Z", ok=passed, wall_s=suite,
                 series={"surefire_s": {"m-bucket": 999}, "reactor_s": dict(modules)})
            for i, (passed, suite, modules) in enumerate(runs)]


FILE = """\
# header kept byte-for-byte
suite = 130                 # old comment

# comment above modules
[modules]
gone = 4
few = 2.5
mod-a = 1

# ─── [observability] ───
[observability]
observability-noop-alloc-bytes-per-op = 0.000  # kept
"""


def test_rebaseline_value_is_the_windowed_median_without_outliers_rounded_up(test_mod):
    assert test_mod.rebaseline_value([100.0, 1.0, 2.0, 3.0], window=3) == 2.0       # the window drops 100
    assert test_mod.rebaseline_value([1.0, 2.0, 3.0, 4.0, 50.0], window=5) == 2.5    # 50 > 3 × 3 is dropped
    assert test_mod.rebaseline_value([1.21, 1.22, 1.23], window=20) == 1.3           # rounded up, not to nearest
    assert test_mod.rebaseline_value([2.0, 2.0], window=20) == 2.0                   # an exact tenth stays


def test_rebaseline_value_takes_p75_for_a_bimodal_series_only_when_asked(test_mod):
    bimodal = [7.2, 7.4, 7.6, 7.5, 16.0, 16.5, 16.9, 7.3]
    assert test_mod.rebaseline_value(bimodal, window=20) == 7.6                          # median by default
    assert test_mod.rebaseline_value(bimodal, window=20, bimodal_p75=True) == 16.2      # p75 lands in the slow mode
    steady = [7.2, 7.4, 7.6, 7.5, 7.3]
    assert test_mod.rebaseline_value(steady, window=20, bimodal_p75=True) == 7.4         # median


def test_rebaseline_value_bimodal_threshold_is_strictly_over_half(test_mod):
    assert test_mod.rebaseline_value([10.0, 10.0, 10.0, 15.0], window=20, bimodal_p75=True) == 10.0   # exactly 0.5
    assert test_mod.rebaseline_value([10.0, 10.0, 10.0, 15.1], window=20, bimodal_p75=True) == 11.3   # just over: p75


def test_rebaseline_rewrites_suite_and_modules_and_keeps_everything_else(test_mod, tmp_path, capsys, history_record):
    baseline = tmp_path / "perf-baseline.txt"
    baseline.write_text(FILE, encoding="utf-8")
    runs = [(True, 170.0 + i, {"mod-a": 9.0, "mod-b": 3.0, "few": 7.0 if i < 2 else None}) for i in range(5)]
    runs = [(p, s, {k: v for k, v in m.items() if v is not None}) for p, s, m in runs]
    runs.append((False, 999.0, {"mod-a": 99.0}))                                   # a failed run is ignored
    records = history_records(history_record, runs)
    assert test_mod.rebaseline(baseline, records, window=20, min_runs=5) == 0
    assert baseline.read_text(encoding="utf-8") == """\
# header kept byte-for-byte
suite = 172

# comment above modules
[modules]
few = 2.5
mod-a = 9
mod-b = 3

# ─── [observability] ───
[observability]
observability-noop-alloc-bytes-per-op = 0.000  # kept
"""
    out = capsys.readouterr().out
    assert "2 changed, 1 added, 1 removed, 1 skipped (fewer than 5 runs)" in out
    assert "   ~ suite: 130 → 172" in out
    assert "   ~ mod-a: 1 → 9" in out
    assert "   + mod-b: 3" in out
    assert "   - gone: 4" in out
    assert "   ? few: 2 run(s)" in out


def test_rebaseline_without_history_rewrites_nothing(test_mod, tmp_path, capsys):
    baseline = tmp_path / "perf-baseline.txt"
    baseline.write_text(FILE, encoding="utf-8")
    assert test_mod.rebaseline(baseline, []) == 1
    assert baseline.read_text(encoding="utf-8") == FILE
    assert "no passing runs" in capsys.readouterr().out


def test_rebaseline_needs_a_modules_section(test_mod, tmp_path, capsys, history_record):
    baseline = tmp_path / "perf-baseline.txt"
    baseline.write_text("suite = 1\n", encoding="utf-8")
    records = history_records(history_record, [(True, 2.0, {})] * 5)
    assert test_mod.rebaseline(baseline, records) == 1
    assert baseline.read_text(encoding="utf-8") == "suite = 1\n"
    assert "no [modules] section" in capsys.readouterr().out


def test_main_rebaseline_runs_no_build(test_mod, monkeypatch, tmp_path, run_history, history_record):
    seen = []
    store = run_history.store(repo=tmp_path)
    for record in history_records(history_record, [(True, 1.0, {"m": 2.0})] * 2):
        store.append(record)
    monkeypatch.setattr(test_mod, "REPO_ROOT", tmp_path)
    monkeypatch.setattr(sys, "argv", ["test.py", "--rebaseline", "--window", "7", "--min-runs", "2"])
    monkeypatch.setattr(test_mod, "build", lambda: pytest.fail("built"))
    monkeypatch.setattr(test_mod, "rebaseline",
                        lambda baseline, records, window, min_runs: seen.append((baseline, records, window, min_runs)) or 0)
    assert test_mod._main() == 0
    assert len(seen) == 1
    baseline, records, window, min_runs = seen[0]
    assert (baseline, window, min_runs) == (test_mod.PERF_BASELINE, 7, 2)
    assert [r["wall_s"] for r in records] == [1.0, 1.0]


REAL_LAYOUT = """\
# header
suite = 130

[modules]

# ─── [observability] ───
[observability]
observability-noop-alloc-bytes-per-op = 0.000  # kept
"""


def test_rebaseline_on_the_real_layout_fills_modules_and_keeps_the_banner(test_mod, tmp_path, history_record):
    baseline = tmp_path / "perf-baseline.txt"
    baseline.write_text(REAL_LAYOUT, encoding="utf-8")
    records = history_records(history_record, [(True, 170.0, {"mod-a": 9.0, "mod-b": 3.0})] * 5)
    assert test_mod.rebaseline(baseline, records, window=20, min_runs=5) == 0
    assert baseline.read_text(encoding="utf-8") == """\
# header
suite = 170

[modules]
mod-a = 9
mod-b = 3

# ─── [observability] ───
[observability]
observability-noop-alloc-bytes-per-op = 0.000  # kept
"""


# ---- characterization: the rebaseline the store-backed --rebaseline must reproduce ------------------------------------
GOLDEN_BASELINE = """\
# perf baseline
suite = 100

[modules]
mod-a = 9
mod-b = 5
old = 3

[observability]
x = 1  # kept
"""

GOLDEN_REBASELINED = """\
# perf baseline
suite = 122.5

[modules]
mod-a = 10.5
mod-b = 22.5

[observability]
x = 1  # kept
"""

GOLDEN_RUNS = [(True, 120.0 + i, {"mod-a": 10.0 + i * 0.2, "mod-b": 20.0 + i}) for i in range(6)]


def test_rebaseline_golden_from_the_records(test_mod, tmp_path, capsys, history_record):
    baseline = tmp_path / "perf-baseline.txt"
    baseline.write_text(GOLDEN_BASELINE, encoding="utf-8")
    records = history_records(history_record, GOLDEN_RUNS)
    assert test_mod.rebaseline(baseline, records, window=20, min_runs=5) == 0
    assert baseline.read_text(encoding="utf-8") == GOLDEN_REBASELINED
    out = capsys.readouterr().out
    assert "3 changed, 0 added, 1 removed, 0 skipped (fewer than 5 runs)" in out
    assert "   ~ suite: 100 → 122.5" in out


def test_rebaseline_from_the_store_matches_the_jsonl_golden(test_mod, monkeypatch, tmp_path, capsys, run_history,
                                                           history_record):
    """C11's numbers as `tests` records in the store, read by `--rebaseline`, give C11's golden output.  Records of
    other steps and failed runs do not count."""
    baseline = tmp_path / "perf-baseline.txt"
    baseline.write_text(GOLDEN_BASELINE, encoding="utf-8")
    store = run_history.store(repo=tmp_path)
    for record in history_records(history_record, GOLDEN_RUNS):
        store.append(record)
    # Under 3× the median, so the outlier rule would keep them: only the step and ok filters drop them.
    for decoy in (history_record(step="build", wall_s=200.0, series={"reactor_s": {"mod-a": 30.0}}),
                  history_record(ok=False, wall_s=200.0, series={"reactor_s": {"mod-a": 30.0}})) * 2:
        store.append(decoy)
    monkeypatch.setattr(test_mod, "REPO_ROOT", tmp_path)
    monkeypatch.setattr(test_mod, "PERF_BASELINE", baseline)
    monkeypatch.setattr(sys, "argv", ["test.py", "--rebaseline"])
    assert test_mod._main() == 0
    assert baseline.read_text(encoding="utf-8") == GOLDEN_REBASELINED
    out = capsys.readouterr().out
    assert "3 changed, 0 added, 1 removed, 0 skipped (fewer than 5 runs)" in out
    assert "   ~ suite: 100 → 122.5" in out


def test_rebaseline_with_history_off_says_so(test_mod, monkeypatch, tmp_path, capsys, run_history, history_record):
    baseline = tmp_path / "perf-baseline.txt"
    baseline.write_text(GOLDEN_BASELINE, encoding="utf-8")
    store = run_history.store(repo=tmp_path)
    for record in history_records(history_record, GOLDEN_RUNS):
        store.append(record)
    monkeypatch.setenv("JUNEAU_TERMINAL_HISTORY", "0")
    monkeypatch.setattr(test_mod, "REPO_ROOT", tmp_path)
    monkeypatch.setattr(test_mod, "PERF_BASELINE", baseline)
    monkeypatch.setattr(sys, "argv", ["test.py", "--rebaseline"])
    assert test_mod._main() == 1
    assert baseline.read_text(encoding="utf-8") == GOLDEN_BASELINE
    assert "JUNEAU_TERMINAL_HISTORY=0" in capsys.readouterr().out


def test_baseline_series_is_the_renamed_reader(test_mod):
    """test.py's own reader is baseline_series, so it never collides with juneau_terminal's history_series."""
    assert callable(test_mod.baseline_series)
    assert not hasattr(test_mod, "history_series")


def test_baseline_series_counts_an_ok_run_whatever_its_complete_flag(test_mod, history_record):
    """ok decides whether a run counts; complete is ignored, so an ok but incomplete run is still a baseline run."""
    records = [history_record(ok=True, complete=False, wall_s=100.0, series={"reactor_s": {"mod-a": 10.0}}),
               history_record(ok=False, complete=True, wall_s=900.0, series={"reactor_s": {"mod-a": 90.0}})]
    assert test_mod.baseline_series(records) == {"suite": [100.0], "mod-a": [10.0]}


def test_baseline_series_counts_only_the_newest_sig(test_mod, history_record):
    """Another command shape (a --no-container or wrapped run) does not feed the baseline of the newest one."""
    records = [history_record(sig="other", wall_s=500.0, series={"reactor_s": {"mod-a": 50.0}}),
               history_record(sig="mine", wall_s=100.0, series={"reactor_s": {"mod-a": 10.0}}),
               history_record(sig="other", wall_s=600.0, series={"reactor_s": {"mod-a": 60.0}}),
               history_record(sig="mine", wall_s=110.0, series={"reactor_s": {"mod-a": 11.0}})]
    assert test_mod.baseline_series(records) == {"suite": [100.0, 110.0], "mod-a": [10.0, 11.0]}


def test_rebaseline_with_too_few_runs_of_the_newest_sig_says_so_and_writes_nothing(test_mod, tmp_path, capsys,
                                                                                    history_record):
    baseline = tmp_path / "perf-baseline.txt"
    baseline.write_text(FILE, encoding="utf-8")
    records = history_records(history_record, [(True, 170.0, {"mod-a": 9.0})] * 6)
    records.append(history_record(sig="mine", wall_s=1.0, series={"reactor_s": {"mod-a": 1.0}}))
    assert test_mod.rebaseline(baseline, records, window=20, min_runs=5) == 1
    assert baseline.read_text(encoding="utf-8") == FILE
    assert "no baseline for this command shape (1 run)" in capsys.readouterr().out
