#!/usr/bin/env python3
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
"""
Report timing deltas from the repo's run history: test.py's `tests` records (juneau_run's HistoryStore, script
"test"), shared by every branch and worktree of the repo.

Each record becomes today's rows: one per (module, bucket) of its surefire_s.<bucket> series, and a reactor/suite row
from its wall_s.  Only records with the newest record's sig (its command shape) count toward the baseline; a shape with
fewer than two runs gets a "no baseline for this command shape" line, not a verdict.  An entry (one module and bucket
of the latest run) is flagged slower only when it is both more than JUNEAU_PUSH_TIMING_THRESHOLD (default 0.20) and
more than JUNEAU_PUSH_TIMING_MIN_SECONDS (default 5) slower than its rolling median, and faster when it beats the
median by both margins.  The full table goes to test-timings.txt beside test.jsonl in the store directory; the console
gets one summary line and the slower entries.
push.py's Timing row loads this file as a module and uses load_history(), report_path(), latest_run(), analyse(),
write_report() and counts().

    push-timings.py [--repo DIR | --history FILE] [--window N] [--report FILE]
"""

from __future__ import annotations

import argparse
import json
import os
import statistics
import sys
from collections import defaultdict
from pathlib import Path

# The shared loader (_juneau.py) sits beside this script; appended, so no script here shadows an installed module.
if str(Path(__file__).resolve().parent) not in sys.path:
    sys.path.append(str(Path(__file__).resolve().parent))
import _juneau  # noqa: E402

REPO_ROOT = Path(__file__).resolve().parent.parent
THRESHOLD_ENV = "JUNEAU_PUSH_TIMING_THRESHOLD"
MIN_SECONDS_ENV = "JUNEAU_PUSH_TIMING_MIN_SECONDS"
DEFAULT_THRESHOLD = 0.20
DEFAULT_MIN_SECONDS = 5.0
MARKS = {"slower": "⚠", "faster": "↓", None: " "}
MODULE_ROWS = "module"
BUCKET_SERIES = "surefire_s."     # surefire_s.<bucket>: {module: summed Surefire seconds}


def load_records(path: Path) -> list[dict]:
    """The JSON objects of a JSONL file, oldest first; a missing file or a malformed line is skipped."""
    if not path.exists():
        return []
    rows = []
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        try:
            row = json.loads(line)
        except json.JSONDecodeError:
            continue
        if isinstance(row, dict):
            rows.append(row)
    return rows


def rows_from_records(records: list[dict]) -> list[dict]:
    """
    Today's rows from test.py's `tests` records: {run_id, passed, module, execution, wallclock_s} per module and bucket,
    plus reactor/suite from wall_s.  The run_id is the record's ts and commit; passed is its ok.  Only records with the
    newest record's sig count (_juneau.same_shape_runs).
    """
    tests = _juneau.same_shape_runs(records)
    rows = []
    for record in tests:
        base = {"run_id": f"{record.get('ts', '')}-{record.get('commit', '')}", "passed": record.get("ok") is True}
        for name, values in sorted((record.get("series") or {}).items()):
            if name.startswith(BUCKET_SERIES) and isinstance(values, dict):
                rows += [{**base, "module": module, "execution": name[len(BUCKET_SERIES):], "wallclock_s": seconds}
                         for module, seconds in sorted(values.items())]
        if record.get("wall_s") is not None:
            rows.append({**base, "module": "reactor", "execution": "suite", "wallclock_s": record["wall_s"]})
    return rows


def _store(repo):
    jr = _juneau.load()[0]
    return jr, jr.HistoryStore(Path(repo), "test")


def history_off() -> bool:
    """True when JUNEAU_RUN_HISTORY=0: the store neither records nor reads."""
    return not _juneau.load()[0].history_enabled()


def load_history(repo=None, path=None) -> list[dict]:
    """The rows (rows_from_records) of the repo's test.py history, or of a records file when path is given."""
    if path is not None:
        return rows_from_records(load_records(Path(path)))
    return rows_from_records(_store(repo or REPO_ROOT)[1].read())


def report_path(repo=None, path=None) -> Path:
    """Where the full table goes: test-timings.txt beside the store's test.jsonl, or <stem>-timings.txt beside a
    records file."""
    if path is not None:
        path = Path(path)
        return path.with_name(f"{path.stem}-timings.txt")
    return _store(repo or REPO_ROOT)[1].path.with_name("test-timings.txt")


def fmt_seconds(value: float) -> str:
    return f"{value:.1f}s"


def fmt_delta(value: float) -> str:
    sign = "+" if value >= 0 else ""
    return f"{sign}{value * 100:.1f}%"


def _env_float(name: str, default: float) -> float:
    """$name as a float, or default when it is unset or not a number (the CLI must always exit 0)."""
    try:
        return float(os.environ.get(name) or default)
    except ValueError:
        return default


def threshold() -> float:
    """The relative margin, as a fraction: $JUNEAU_PUSH_TIMING_THRESHOLD, default 0.20."""
    return _env_float(THRESHOLD_ENV, DEFAULT_THRESHOLD)


def min_seconds() -> float:
    """The absolute margin in seconds: $JUNEAU_PUSH_TIMING_MIN_SECONDS, default 5."""
    return _env_float(MIN_SECONDS_ENV, DEFAULT_MIN_SECONDS)


def latest_run(records: list[dict]) -> tuple[list[dict], str | None]:
    """The latest run's passing rows, or an empty list and the reason there are none."""
    if not records:
        return [], "no timing history yet"
    run_id = records[-1].get("run_id")
    if not run_id:
        return [], "missing run metadata in latest record"
    rows = [r for r in records if r.get("run_id") == run_id and r.get("passed", True)
            and r.get("execution") != MODULE_ROWS]
    if not rows:
        return [], "latest run did not produce successful timing rows"
    return rows, None


def classify(seconds: float, baseline: float | None, rel: float, abs_seconds: float) -> str | None:
    """"slower" or "faster" when the entry moved by more than both margins, else None."""
    if not baseline:
        return None
    diff = seconds - baseline
    if abs(diff) / baseline > rel and abs(diff) > abs_seconds:
        return "slower" if diff > 0 else "faster"
    return None


def analyse(records: list[dict], window: int = 20) -> list[dict]:
    """
    One entry per (module, execution) in the latest run, sorted by module and execution.  Each is a dict with module,
    execution, seconds, baseline (the median of the last `window` earlier passing runs, None on a first run), delta (a
    fraction, 0 without a baseline), flag ("slower", "faster" or None) and line (its row in the table).
    """
    rows, _ = latest_run(records)
    if not rows:
        return []
    run_id = rows[0]["run_id"]
    prior: dict[tuple[str, str], list[float]] = defaultdict(list)
    for row in records:
        if row.get("run_id") != run_id and row.get("passed", True):
            prior[(row.get("module", "?"), row.get("execution", "?"))].append(float(row.get("wallclock_s", 0.0)))
    rel, abs_seconds = threshold(), min_seconds()
    entries = []
    for row in sorted(rows, key=lambda r: (r.get("module", ""), r.get("execution", ""))):
        module, execution = row.get("module", "?"), row.get("execution", "?")
        seconds = float(row.get("wallclock_s", 0.0))
        earlier = prior.get((module, execution), [])
        baseline = statistics.median(earlier[-window:]) if earlier else None
        delta = (seconds - baseline) / baseline if baseline else 0.0
        if baseline is None:
            line = f"{module}/{execution}: {fmt_seconds(seconds)} (first run, no baseline)"
        else:
            line = (f"{module}/{execution}: {fmt_seconds(seconds)} (median {fmt_seconds(baseline)}, "
                    f"delta {fmt_delta(delta)}, {seconds - baseline:+.1f}s)")
        entries.append({"module": module, "execution": execution, "seconds": seconds, "baseline": baseline,
                        "delta": delta, "flag": classify(seconds, baseline, rel, abs_seconds), "line": line})
    return entries


def counts(entries: list[dict]) -> str:
    """
    "<n> slower than 20% and 5s · <k> faster · <m> without a baseline"; the faster and baseline parts are left out
    when zero.
    """
    slower = sum(1 for e in entries if e["flag"] == "slower")
    faster = sum(1 for e in entries if e["flag"] == "faster")
    first_runs = sum(1 for e in entries if e["baseline"] is None)
    parts = [f"{slower} slower than {threshold():.0%} and {min_seconds():g}s"]
    if faster:
        parts.append(f"{faster} faster")
    if first_runs:
        parts.append(f"{first_runs} without a baseline")
    return " · ".join(parts)


def write_report(entries: list[dict], path: Path, window: int = 20) -> None:
    """Write the full table to path, replacing it: every entry with its mark, then the counts."""
    lines = [f"Push-timing report (last {window} runs in this repo)"]
    lines += [f" {MARKS[e['flag']]} {e['line']}" for e in entries] or ["   no successful timing rows in the latest run"]
    lines.append(counts(entries))
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Print push timing regression summary.")
    source = parser.add_mutually_exclusive_group()
    source.add_argument("--repo", help="The repo whose run history to read (default: this script's repo)")
    source.add_argument("--history", help="A test.py history records file to read instead of the repo's store")
    parser.add_argument("--window", type=int, default=20, help="Rolling median window per execution")
    parser.add_argument("--report", help="Where to write the full table (default: test-timings.txt beside the store)")
    args = parser.parse_args(argv)

    history = Path(args.history).expanduser() if args.history else None
    repo = Path(args.repo).expanduser() if args.repo else REPO_ROOT
    if history is None and history_off():
        print("📊 Push-timing report: run history is off (JUNEAU_RUN_HISTORY=0).")
        return 0
    records = load_history(repo, history)
    _, reason = latest_run(records)
    if reason:
        print(f"📊 Push-timing report: {reason}.")
        return 0

    runs = len({row["run_id"] for row in records if row.get("passed", True)})
    if runs < 2:
        print(f"📊 Push-timing report: {_juneau.no_baseline_line(runs)}.")
        return 0

    entries = analyse(records, args.window)
    report = Path(args.report).expanduser() if args.report else report_path(repo, history)
    write_report(entries, report, args.window)
    print(f"📊 Push-timing report (last {args.window} runs in this repo): {counts(entries)} → {report}")
    for entry in entries:
        if entry["flag"] == "slower":
            print(f"   ⚠ {entry['line']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
