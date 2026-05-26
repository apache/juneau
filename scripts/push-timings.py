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
Report timing deltas from push timing JSONL history.
"""

from __future__ import annotations

import argparse
import json
import os
import statistics
from collections import defaultdict
from pathlib import Path


def load_records(path: Path) -> list[dict]:
    if not path.exists():
        return []
    rows = []
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        try:
            rows.append(json.loads(line))
        except json.JSONDecodeError:
            continue
    return rows


def fmt_seconds(value: float) -> str:
    return f"{value:.1f}s"


def fmt_delta(value: float) -> str:
    sign = "+" if value >= 0 else ""
    return f"{sign}{value * 100:.1f}%"


def main() -> int:
    parser = argparse.ArgumentParser(description="Print push timing regression summary.")
    parser.add_argument("--log", required=True, help="Path to timing JSONL")
    parser.add_argument("--window", type=int, default=20, help="Rolling median window per execution")
    args = parser.parse_args()

    log_path = Path(args.log).expanduser()
    records = load_records(log_path)
    if not records:
        print("📊 Push-timing report: no timing history yet.")
        return 0

    latest_run = records[-1].get("run_id")
    if not latest_run:
        print("📊 Push-timing report: missing run metadata in latest record.")
        return 0

    latest_records = [r for r in records if r.get("run_id") == latest_run and r.get("passed", True)]
    if not latest_records:
        print("📊 Push-timing report: latest run did not produce successful timing rows.")
        return 0

    threshold = float(os.environ.get("JUNEAU_PUSH_TIMING_THRESHOLD", "0.05"))
    print(f"📊 Push-timing report (last {args.window} runs on this branch):")

    prior_by_key: dict[tuple[str, str], list[float]] = defaultdict(list)
    for row in records:
        if row.get("run_id") == latest_run:
            continue
        key = (row.get("module", "?"), row.get("execution", "?"))
        if row.get("passed", True):
            prior_by_key[key].append(float(row.get("wallclock_s", 0.0)))

    for row in sorted(latest_records, key=lambda r: (r.get("module", ""), r.get("execution", ""))):
        module = row.get("module", "?")
        execution = row.get("execution", "?")
        wallclock = float(row.get("wallclock_s", 0.0))
        key = (module, execution)
        prior = prior_by_key.get(key, [])
        if prior:
            baseline = statistics.median(prior[-args.window:])
            delta = (wallclock - baseline) / baseline if baseline > 0 else 0.0
            print(f"   {module}/{execution:<18}: {fmt_seconds(wallclock):>6}  (median {fmt_seconds(baseline)}, delta {fmt_delta(delta)})")
            if abs(delta) > threshold:
                print(
                    f"   ⚠ {module}/{execution} wall-clock {fmt_seconds(wallclock)} is {fmt_delta(delta)} "
                    f"vs rolling median {fmt_seconds(baseline)} (threshold {threshold * 100:.0f}%)"
                )
        else:
            print(f"   {module}/{execution:<18}: {fmt_seconds(wallclock):>6}  (first run, no baseline)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
