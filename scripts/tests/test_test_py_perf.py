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
import json
import sys
from pathlib import Path

import pytest

SCRIPTS_DIR = Path(__file__).resolve().parent.parent


@pytest.fixture
def test_mod():
    sys.modules.pop("juneau_run", None)
    spec = importlib.util.spec_from_file_location("_perf_test", SCRIPTS_DIR / "test.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    yield module
    sys.modules.pop("juneau_run", None)


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
[INFO] Apache Juneau REST Server Views .................... FAILURE [ 35.0 s]
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
    test_mod.run_module = lambda: None          # plain output, no console
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
    test_mod.run_module = lambda: None
    report = tmp_path / "perf-guard.txt"
    test_mod.run_perf_guard(1.0, path, {}, enforce=False, report_file=report, unmapped=["Twin", "Other"])
    assert report.read_text(encoding="utf-8").count(
        "PERF-GUARD: 2 reactor module(s) not mapped to a pom (name shared or missing): Twin, Other") == 1


# ---- timing-log rows ---------------------------------------------------------------------------------------------------
def test_write_timing_log_adds_one_module_row_per_measured_module(test_mod, tmp_path, monkeypatch):
    monkeypatch.setattr(test_mod, "discover_module_stats", lambda root: {
        "juneau-rest/juneau-rest-mock": {"core": {"tests": 332, "seconds": 6.6}}})
    monkeypatch.setattr(test_mod, "git_value", lambda args: "x")
    log = tmp_path / "t.jsonl"
    test_mod.write_timing_log(log, passed=True, test_elapsed=180.0,
                              module_times={"juneau-rest/juneau-rest-mock": 9.0, "juneau-bom": 0.1})
    rows = [json.loads(line) for line in log.read_text(encoding="utf-8").splitlines()]
    assert [(r["module"], r["execution"]) for r in rows] == [
        ("juneau-rest/juneau-rest-mock", "core"),
        ("juneau-bom", "module"), ("juneau-rest/juneau-rest-mock", "module"),
        ("reactor", "suite")]
    mock_row = rows[2]
    assert mock_row["wallclock_s"] == 9.0 and mock_row["test_count"] == 332
    assert "test_count" not in rows[1]
    assert rows[3]["test_count"] == 332 and rows[3]["surefire_total_s"] == 6.6


def test_main_feeds_the_reactor_times_to_the_guard_and_the_log(test_mod, monkeypatch, tmp_path):
    seen = {}
    monkeypatch.setattr(sys, "argv", ["test.py", "--test-only", "--no-js-tests", "--timing-log", str(tmp_path / "t")])
    monkeypatch.setattr(test_mod, "test", lambda no_container=False: (0, "REACTOR"))
    monkeypatch.setattr(test_mod, "module_times", lambda output, root: ({"m": 1.0}, ["u"]) if output == "REACTOR" else None)
    monkeypatch.setattr(test_mod, "write_timing_log",
                        lambda path, passed, test_elapsed, module_times=None: seen.setdefault("log", module_times))
    monkeypatch.setattr(test_mod, "run_perf_guard",
                        lambda elapsed, baseline, times, enforce, unmapped=(): seen.setdefault("guard", (times, unmapped)) and 0)
    assert test_mod._main() == 0
    assert seen == {"log": {"m": 1.0}, "guard": ({"m": 1.0}, ["u"])}


# ---- --rebaseline ------------------------------------------------------------------------------------------------------
def history_log(path, runs):
    """runs: [(passed, suite seconds, {module: seconds})]; one JSONL run each, in order."""
    lines = []
    for i, (passed, suite, modules) in enumerate(runs):
        base = {"run_id": f"r{i}", "passed": passed}
        lines += [json.dumps({**base, "module": "m-bucket", "execution": "core", "wallclock_s": 999})]
        lines += [json.dumps({**base, "module": m, "execution": "module", "wallclock_s": s}) for m, s in modules.items()]
        lines += [json.dumps({**base, "module": "reactor", "execution": "suite", "wallclock_s": suite})]
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return path


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


def test_rebaseline_rewrites_suite_and_modules_and_keeps_everything_else(test_mod, tmp_path, capsys):
    baseline = tmp_path / "perf-baseline.txt"
    baseline.write_text(FILE, encoding="utf-8")
    runs = [(True, 170.0 + i, {"mod-a": 9.0, "mod-b": 3.0, "few": 7.0 if i < 2 else None}) for i in range(5)]
    runs = [(p, s, {k: v for k, v in m.items() if v is not None}) for p, s, m in runs]
    runs.append((False, 999.0, {"mod-a": 99.0}))                                   # a failed run is ignored
    log = history_log(tmp_path / "t.jsonl", runs)
    assert test_mod.rebaseline(baseline, log, window=20, min_runs=5) == 0
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
    assert test_mod.rebaseline(baseline, tmp_path / "missing.jsonl") == 1
    assert baseline.read_text(encoding="utf-8") == FILE
    assert "no passing runs" in capsys.readouterr().out


def test_rebaseline_needs_a_modules_section(test_mod, tmp_path, capsys):
    baseline = tmp_path / "perf-baseline.txt"
    baseline.write_text("suite = 1\n", encoding="utf-8")
    log = history_log(tmp_path / "t.jsonl", [(True, 2.0, {})] * 5)
    assert test_mod.rebaseline(baseline, log) == 1
    assert baseline.read_text(encoding="utf-8") == "suite = 1\n"
    assert "no [modules] section" in capsys.readouterr().out


def test_main_rebaseline_runs_no_build(test_mod, monkeypatch, tmp_path):
    seen = []
    monkeypatch.setattr(sys, "argv", ["test.py", "--rebaseline", "--window", "7", "--min-runs", "2",
                                      "--timing-log", str(tmp_path / "t.jsonl")])
    monkeypatch.setattr(test_mod, "build", lambda: pytest.fail("built"))
    monkeypatch.setattr(test_mod, "rebaseline",
                        lambda baseline, log, window, min_runs: seen.append((baseline, log, window, min_runs)) or 0)
    assert test_mod._main() == 0
    assert seen == [(test_mod.PERF_BASELINE, tmp_path / "t.jsonl", 7, 2)]


REAL_LAYOUT = """\
# header
suite = 130

[modules]

# ─── [observability] ───
[observability]
observability-noop-alloc-bytes-per-op = 0.000  # kept
"""


def test_rebaseline_on_the_real_layout_fills_modules_and_keeps_the_banner(test_mod, tmp_path):
    baseline = tmp_path / "perf-baseline.txt"
    baseline.write_text(REAL_LAYOUT, encoding="utf-8")
    log = history_log(tmp_path / "t.jsonl", [(True, 170.0, {"mod-a": 9.0, "mod-b": 3.0})] * 5)
    assert test_mod.rebaseline(baseline, log, window=20, min_runs=5) == 0
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
