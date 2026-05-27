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
"""
CI perf guard for Apache Juneau.

Reads per-bucket Surefire XML totals (or a --timing-log JSONL written by scripts/test.py)
and compares them against the baselines in juneau-utest/perf-baseline.txt.  Intended to run
in Jenkinsfile's post-build block after the main Maven build has already produced Surefire
XML reports.

Usage:
    python3 scripts/ci-perf-guard.py [--timing-log <path>] [--dry-run]

Options:
    --timing-log <path>   Read actual times from a JSONL written by scripts/test.py --timing-log.
                          When omitted, reads Surefire XML directly from
                          juneau-utest/target/surefire-reports/{core,container}/.
    --dry-run             Print detected totals and baselines but do not exit non-zero on breach.
    --help                Show this help message.

Env vars:
    JUNEAU_CI_PERF_THRESHOLD   Tolerance as a decimal fraction (default 0.20 = ±20%).
                               Separate from JUNEAU_PUSH_TIMING_THRESHOLD.

Baseline lines in perf-baseline.txt (lines 2–4, Surefire XML aggregate values):
    line 2 — core bucket total
    line 3 — container.springboot bucket total
    line 4 — container.jetty bucket total

NOTE: The per-bucket baselines were calibrated on a developer M1 Pro laptop.  The first
Jenkins run after this guard is introduced will likely show different values; follow the
first-CI-run calibration procedure in todo/PERF-BASELINE.md to set the canonical CI baseline.
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def parse_surefire_dir(directory: Path) -> float:
	"""Return the sum of Surefire XML 'time' attributes under directory."""
	total = 0.0
	if not directory.exists():
		return total
	for xml_file in sorted(directory.glob("TEST-*.xml")):
		try:
			root = ET.parse(xml_file).getroot()
			total += float(root.attrib.get("time", 0.0))
		except Exception:
			continue
	return total


def container_bucket(module_dir: Path, class_name: str) -> str:
	"""Classify a container test class as container.springboot or container.jetty."""
	source = module_dir / "src" / "test" / "java" / Path("/".join(class_name.split("."))).with_suffix(".java")
	if source.exists():
		try:
			content = source.read_text(encoding="utf-8")
			if "SpringbootTest" in content:
				return "container.springboot"
			if "JettyMicroserviceTest" in content:
				return "container.jetty"
		except OSError:
			pass
	return "container.springboot" if "springboot" in class_name.lower() else "container.jetty"


def parse_container_subbuckets(module_dir: Path, directory: Path) -> dict:
	"""Return {'container.springboot': float, 'container.jetty': float} Surefire XML totals."""
	buckets: dict[str, float] = {"container.springboot": 0.0, "container.jetty": 0.0}
	if not directory.exists():
		return buckets
	for xml_file in sorted(directory.glob("TEST-*.xml")):
		try:
			root = ET.parse(xml_file).getroot()
			bucket = container_bucket(module_dir, root.attrib.get("name", ""))
			buckets[bucket] += float(root.attrib.get("time", 0.0))
		except Exception:
			continue
	return buckets


def read_baselines_from_file(baseline_file: Path) -> dict:
	"""Parse perf-baseline.txt; positions 2–4 (0-based indices 1–3) are the per-bucket baselines."""
	keys = ["suite", "core", "container.springboot", "container.jetty"]
	result: dict[str, float] = {}
	if not baseline_file.exists():
		return result
	idx = 0
	for line in baseline_file.read_text(encoding="utf-8").splitlines():
		stripped = line.strip()
		if not stripped or stripped.startswith("#"):
			continue
		if idx >= len(keys):
			break
		try:
			result[keys[idx]] = float(stripped.split()[0])
		except (ValueError, IndexError):
			pass
		idx += 1
	return result


def read_actuals_from_jsonl(log_path: Path) -> dict:
	"""Return {execution: wallclock_s} for the latest run_id in the JSONL."""
	rows: list[dict] = []
	for line in log_path.read_text(encoding="utf-8").splitlines():
		stripped = line.strip()
		if not stripped:
			continue
		try:
			rows.append(json.loads(stripped))
		except json.JSONDecodeError:
			continue
	if not rows:
		return {}
	latest_run_id = rows[-1].get("run_id")
	result: dict[str, float] = {}
	for row in rows:
		if row.get("run_id") == latest_run_id:
			execution = row.get("execution", "")
			result[execution] = float(row.get("wallclock_s", 0.0))
	return result


def read_actuals_from_surefire(repo_root: Path) -> dict:
	"""Read per-bucket Surefire XML aggregate times directly from target/surefire-reports/."""
	module_dir = repo_root / "juneau-utest"
	reports_root = module_dir / "target" / "surefire-reports"
	core = parse_surefire_dir(reports_root / "core")
	sub = parse_container_subbuckets(module_dir, reports_root / "container")
	return {
		"core": core,
		"container.springboot": sub["container.springboot"],
		"container.jetty": sub["container.jetty"],
	}


def main() -> int:
	parser = argparse.ArgumentParser(add_help=False)
	parser.add_argument("--timing-log")
	parser.add_argument("--dry-run", action="store_true")
	parser.add_argument("--help", "-h", action="store_true")
	args, unknown = parser.parse_known_args()
	if args.help:
		print(__doc__)
		return 0
	if unknown:
		print(f"Unknown option(s): {' '.join(unknown)}")
		print(__doc__)
		return 1

	repo_root = Path(__file__).parent.parent
	baseline_file = repo_root / "juneau-utest" / "perf-baseline.txt"
	baselines = read_baselines_from_file(baseline_file)
	if not baselines:
		print(f"PERF-GUARD: baseline file not found or empty ({baseline_file}); skipping.")
		return 0

	if args.timing_log:
		log_path = Path(args.timing_log).expanduser()
		if not log_path.exists():
			print(f"PERF-GUARD: timing log not found ({log_path}); falling back to Surefire XML.")
			actuals = read_actuals_from_surefire(repo_root)
		else:
			actuals = read_actuals_from_jsonl(log_path)
	else:
		actuals = read_actuals_from_surefire(repo_root)

	tolerance = float(os.environ.get("JUNEAU_CI_PERF_THRESHOLD", "0.20"))
	pct = f"±{tolerance * 100:.0f}%"
	guard_failed = False

	total_actual = actuals.get("core", 0.0) + actuals.get("container.springboot", 0.0) + actuals.get("container.jetty", 0.0)
	print(f"PERF-GUARD CI: detected total {total_actual:.1f}s (core {actuals.get('core', 0.0):.1f}s + springboot {actuals.get('container.springboot', 0.0):.1f}s + jetty {actuals.get('container.jetty', 0.0):.1f}s)")

	for bucket in ("core", "container.springboot", "container.jetty"):
		actual = actuals.get(bucket)
		baseline = baselines.get(bucket)
		if actual is None:
			print(f"PERF-GUARD [{bucket}]: no data; skipping.")
			continue
		if baseline is None:
			print(f"PERF-GUARD [{bucket}]: no baseline configured; skipping.")
			continue
		threshold = baseline * (1 + tolerance)
		if actual > threshold:
			print(
				f"PERF-GUARD FAIL [{bucket}]: {actual:.1f}s "
				f"(baseline {baseline}s, tolerance {pct}, threshold {threshold:.1f}s).\n"
				f"  Follow the re-baseline procedure in todo/PERF-BASELINE.md if this is intentional.\n"
				f"  NOTE: If this is the first CI run, the developer-machine baseline needs CI calibration."
			)
			guard_failed = True
		else:
			print(f"PERF-GUARD OK [{bucket}]: {actual:.1f}s (baseline {baseline}s, tolerance {pct}).")

	if guard_failed and args.dry_run:
		print("PERF-GUARD: dry-run mode; breach detected but not failing the build.")
		return 0

	return 1 if guard_failed else 0


if __name__ == "__main__":
	raise SystemExit(main())
