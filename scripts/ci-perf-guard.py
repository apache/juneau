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
CI perf guard for Apache Juneau (per-module, TODO-160).

Reads per-module Surefire XML totals (or a --timing-log JSONL written by scripts/test.py)
and compares them against the per-module baselines in the project-root perf-baseline.txt.
Intended to run in Jenkinsfile's post-build block after the main Maven build has already
produced Surefire XML reports across the whole reactor.

Usage:
    python3 scripts/ci-perf-guard.py [--timing-log <path>] [--dry-run]

Options:
    --timing-log <path>   Read actual times from a JSONL written by scripts/test.py --timing-log.
                          When omitted, discovers and reads Surefire XML directly from every
                          <module>/target/surefire-reports/ under the reactor.
    --dry-run             Print detected totals and baselines but do not exit non-zero on breach.
    --help                Show this help message.

Env vars:
    JUNEAU_CI_PERF_THRESHOLD   Tolerance as a decimal fraction (default 0.20 = ±20%).
                               Separate from JUNEAU_PUSH_TIMING_THRESHOLD.

Baseline format (perf-baseline.txt, project root):
    <key> = <seconds>            # optional trailing comment
      'suite'             -> overall reactor wall-clock baseline (not used here; ci-perf-guard
                             works off Surefire XML test-time, not wall-clock).
      '<module>/<bucket>' -> per-module Surefire test-time baseline; <bucket> ∈
                             {core, container.springboot, container.jetty, container.tomcat}.
    Modules with no baseline entry are treated as new (warn, not fail).

NOTE: Baselines were calibrated on a developer M1 Pro laptop.  The first Jenkins run after a
re-baseline will likely show different values; recalibrate against the CI Linux numbers.
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


# Container annotation marker -> bucket.  Checked against the test class source.
CONTAINER_MARKERS = (
	("SpringbootTest", "container.springboot"),
	("JettyMicroserviceTest", "container.jetty"),
	("TomcatMicroserviceTest", "container.tomcat"),
)


def classify_bucket(module_dir: Path, class_name: str) -> str:
	"""Bucket a test class as core or a container.* flavor via source-annotation inspection."""
	source = module_dir / "src" / "test" / "java" / Path("/".join(class_name.split("."))).with_suffix(".java")
	if source.exists():
		try:
			content = source.read_text(encoding="utf-8")
			for marker, bucket in CONTAINER_MARKERS:
				if marker in content:
					return bucket
			return "core"
		except OSError:
			pass
	lowered = class_name.lower()
	if "springboot" in lowered:
		return "container.springboot"
	if "tomcat" in lowered:
		return "container.tomcat"
	if "jetty" in lowered:
		return "container.jetty"
	return "core"


def aggregate_module_reports(module_dir: Path, reports_dir: Path) -> dict:
	"""Return {bucket: seconds} for one module's surefire-reports tree (flat or nested)."""
	buckets: dict = {}
	for xml_file in sorted(reports_dir.rglob("TEST-*.xml")):
		try:
			root = ET.parse(xml_file).getroot()
		except (ET.ParseError, OSError):
			continue
		bucket = classify_bucket(module_dir, root.attrib.get("name", ""))
		buckets[bucket] = buckets.get(bucket, 0.0) + float(root.attrib.get("time", 0.0))
	return buckets


def read_baselines_from_file(baseline_file: Path) -> dict:
	"""Parse perf-baseline.txt: '<key> = <seconds>'.  Keeps 'suite' and '<module>/<bucket>' keys."""
	result: dict = {}
	if not baseline_file.exists():
		return result
	for line in baseline_file.read_text(encoding="utf-8").splitlines():
		stripped = line.split("#", 1)[0].strip()
		if not stripped or "=" not in stripped:
			continue
		key, _, value = stripped.partition("=")
		key = key.strip()
		if key != "suite" and "/" not in key:
			continue
		try:
			result[key] = float(value.strip().split()[0])
		except (ValueError, IndexError):
			continue
	return result


def read_actuals_from_jsonl(log_path: Path) -> dict:
	"""Return {'<module>/<bucket>': seconds} for the latest run_id in the JSONL (skips the suite row)."""
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
	result: dict = {}
	for row in rows:
		if row.get("run_id") != latest_run_id:
			continue
		module = row.get("module", "?")
		execution = row.get("execution", "?")
		if module == "reactor" and execution == "suite":
			continue
		result[f"{module}/{execution}"] = float(row.get("wallclock_s", 0.0))
	return result


def read_actuals_from_surefire(repo_root: Path) -> dict:
	"""Discover every <module>/target/surefire-reports/ under the reactor: {'<module>/<bucket>': seconds}."""
	actuals: dict = {}
	for reports_dir in sorted(repo_root.rglob("target/surefire-reports")):
		if not reports_dir.is_dir():
			continue
		module_dir = reports_dir.parent.parent
		module_key = module_dir.relative_to(repo_root).as_posix()
		for bucket, seconds in aggregate_module_reports(module_dir, reports_dir).items():
			actuals[f"{module_key}/{bucket}"] = seconds
	return actuals


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
	baseline_file = repo_root / "perf-baseline.txt"
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

	total_actual = sum(actuals.values())
	print(f"PERF-GUARD CI: detected total {total_actual:.1f}s across {len(actuals)} module-bucket(s).")

	checked = 0
	regressions = 0
	new_keys = []
	for key in sorted(actuals):
		actual = actuals[key]
		baseline = baselines.get(key)
		if baseline is None:
			new_keys.append(key)
			continue
		checked += 1
		threshold = baseline * (1 + tolerance)
		if actual > threshold:
			print(
				f"PERF-GUARD FAIL [{key}]: {actual:.1f}s "
				f"(baseline {baseline}s, tolerance {pct}, threshold {threshold:.1f}s).\n"
				f"  If intentional, bump '{key}' in perf-baseline.txt (project root)."
			)
			guard_failed = True
			regressions += 1

	for key in new_keys:
		print(f"PERF-GUARD WARN [{key}]: {actuals[key]:.1f}s — no baseline entry yet (new/unknown; not failing).")

	print(
		f"PERF-GUARD SUMMARY: {checked} module-bucket(s) checked, {regressions} regression(s), "
		f"{len(new_keys)} new/unknown."
	)

	if guard_failed and args.dry_run:
		print("PERF-GUARD: dry-run mode; breach detected but not failing the build.")
		return 0

	return 1 if guard_failed else 0


if __name__ == "__main__":
	raise SystemExit(main())
