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
Build and test helper script for Apache Juneau.

Usage:
    ./scripts/test.py [options]

Options:
    --build-only, -b         Only build (skip tests)
    --test-only, -t          Only run tests (no build)
    --full, -f               Clean build + run tests (default)
    --verbose, -v            Show full Maven output
    --no-container           Exclude @Tag("container") tests
    --timing-log <path>      Append per-(module, bucket) timing JSONL records
    --enforce-perf           Hard-fail if wall-clock exceeds perf-baseline.txt ±20% tolerance
    --profile <module>       Run one-shot JFR profile for module tests
    --help, -h               Show this help message

Perf guard (per-module, TODO-160):
    Timing/perf statistics are collected PER MODULE.  write_timing_log() discovers every
    target/surefire-reports/ directory under the reactor (not just juneau-integration-tests's), attributes
    each Surefire XML to its OWNING module (the parent of target/surefire-reports), and buckets
    each test class as core / container.springboot / container.jetty / container.tomcat.

    --enforce-perf compares the measured tests-only wall-clock against the 'suite' baseline and
    each module's Surefire test-time against its '<module>/<bucket>' baseline in the project-root
    perf-baseline.txt.  Uses env-var JUNEAU_CI_PERF_THRESHOLD (default 0.20, i.e. ±20%) — separate
    from JUNEAU_PUSH_TIMING_THRESHOLD.  Modules with no baseline entry are treated as new (warn,
    not fail) so the guard degrades gracefully as TODO-160 migrates modules.

    Without --enforce-perf the check runs in warn-only mode (prints results, never exits non-zero
    for a perf breach).
"""

import argparse
import json
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path


def run_command(cmd, verbose=False):
	"""Run a command and return exit code and full output."""
	script_dir = Path(__file__).parent
	project_root = script_dir.parent
	print(f"Running: {cmd}")
	print("-" * 80)
	result = subprocess.run(cmd, shell=True, cwd=str(project_root), capture_output=True, text=True)
	output = result.stdout + result.stderr
	if verbose:
		print(result.stdout)
		print(result.stderr, file=sys.stderr)
	else:
		lines = output.splitlines()
		print("\n".join(lines[-50:]) if len(lines) > 50 else output)
	return result.returncode, output


def git_value(args):
	try:
		result = subprocess.run(["git", *args], cwd=str(Path(__file__).parent.parent), capture_output=True, text=True, check=True)
		return result.stdout.strip()
	except Exception:
		return "unknown"


def parse_test_results(output):
	matches = list(re.finditer(r"\[ERROR\]\s+Tests run:\s+(\d+),\s+Failures:\s+(\d+),\s+Errors:\s+(\d+)", output))
	if matches:
		match = matches[-1]
		total = int(match.group(1))
		failures = int(match.group(2))
		errors = int(match.group(3))
		return total, failures, errors
	return None, None, None


# ─── Per-module timing/perf subsystem (TODO-160) ────────────────────────────────
#
# After the TODO-160 migration, tests live in each module's own src/test/java and
# report into that module's target/surefire-reports/.  The helpers below discover
# ALL such report dirs under the reactor (not just juneau-integration-tests's), attribute each
# Surefire XML to its OWNING module (the parent of target/surefire-reports), and
# bucket each test class.
#
# Module key = the module's path relative to the repo root (e.g. "juneau-integration-tests",
#              "juneau-core/juneau-junit5"; MAY contain '/').
# Bucket     ∈ {core, container.springboot, container.jetty, container.tomcat}.
#
# juneau-integration-tests additionally splits its reports into surefire-reports/{core,container}/
# via two Surefire executions; the recursive XML scan below transparently handles
# both that nested layout and the standard flat layout every migrated module uses.

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
	# Source unreadable (rare): fall back to a name-based heuristic, defaulting to core.
	lowered = class_name.lower()
	if "springboot" in lowered:
		return "container.springboot"
	if "tomcat" in lowered:
		return "container.tomcat"
	if "jetty" in lowered:
		return "container.jetty"
	return "core"


def aggregate_module_reports(module_dir: Path, reports_dir: Path) -> dict:
	"""Return {bucket: {'tests': int, 'seconds': float}} for one module's surefire-reports tree."""
	buckets: dict = {}
	for xml_file in sorted(reports_dir.rglob("TEST-*.xml")):
		try:
			root = ET.parse(xml_file).getroot()
		except (ET.ParseError, OSError):
			continue
		bucket = classify_bucket(module_dir, root.attrib.get("name", ""))
		stats = buckets.setdefault(bucket, {"tests": 0, "seconds": 0.0})
		stats["tests"] += int(root.attrib.get("tests", 0))
		stats["seconds"] += float(root.attrib.get("time", 0.0))
	return buckets


def discover_module_stats(repo_root: Path) -> dict:
	"""Map module-key -> {bucket -> {tests, seconds}} for every surefire-reports dir under the reactor."""
	modules: dict = {}
	for reports_dir in sorted(repo_root.rglob("target/surefire-reports")):
		if not reports_dir.is_dir():
			continue
		module_dir = reports_dir.parent.parent
		module_key = module_dir.relative_to(repo_root).as_posix()
		buckets = aggregate_module_reports(module_dir, reports_dir)
		if buckets:
			modules[module_key] = buckets
	return modules


def write_timing_log(path: Path, passed: bool, test_elapsed: float):
	"""Append one JSONL row per (module, bucket) plus a reactor/suite roll-up row."""
	path = path.expanduser()
	path.parent.mkdir(parents=True, exist_ok=True)
	repo_root = Path(__file__).parent.parent
	module_stats = discover_module_stats(repo_root)

	run_id = f"{datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')}-{git_value(['rev-parse', '--short', 'HEAD'])}"
	base = {
		"ts": datetime.now(timezone.utc).isoformat(),
		"run_id": run_id,
		"branch": git_value(["rev-parse", "--abbrev-ref", "HEAD"]),
		"commit": git_value(["rev-parse", "--short", "HEAD"]),
		"passed": passed,
	}

	rows = []
	total_tests = 0
	total_seconds = 0.0
	for module_key in sorted(module_stats):
		for bucket in sorted(module_stats[module_key]):
			stats = module_stats[module_key][bucket]
			total_tests += stats["tests"]
			total_seconds += stats["seconds"]
			rows.append({
				**base,
				"module": module_key,
				"execution": bucket,
				"wallclock_s": round(stats["seconds"], 3),
				"test_count": stats["tests"],
			})
	# Roll-up: reactor wall-clock + total counts across all modules.
	rows.append({
		**base,
		"module": "reactor",
		"execution": "suite",
		"wallclock_s": round(test_elapsed, 3),
		"test_count": total_tests,
		"surefire_total_s": round(total_seconds, 3),
	})

	with path.open("a", encoding="utf-8") as f:
		for row in rows:
			f.write(json.dumps(row) + "\n")
	print(
		f"🕒 Timing metrics appended to {path} "
		f"({len(module_stats)} modules, {total_tests} tests, reactor {test_elapsed:.1f}s)"
	)


def read_baselines(baseline_file: Path) -> dict:
	"""Parse the per-module perf baseline file.

	Format (one entry per line):  <key> = <seconds>   # optional trailing comment
	  'suite'             -> overall reactor wall-clock baseline.
	  '<module>/<bucket>' -> per-module Surefire test-time baseline.

	Blank lines, '#' comments, the [observability] section, and any non-'suite' key
	without a '/' (e.g. the observability metric) are ignored.
	"""
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


def _latest_run_actuals(log_path: Path) -> dict:
	"""Return {'<module>/<bucket>': wallclock_s, ..., 'suite': wallclock_s} for the latest run_id."""
	rows = []
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
		wallclock = float(row.get("wallclock_s", 0.0))
		if module == "reactor" and execution == "suite":
			result["suite"] = wallclock
		else:
			result[f"{module}/{execution}"] = wallclock
	return result


def _actuals_from_surefire(repo_root: Path) -> dict:
	"""Fallback per-module actuals straight from surefire reports: {'<module>/<bucket>': seconds}."""
	actuals: dict = {}
	for module_key, buckets in discover_module_stats(repo_root).items():
		for bucket, stats in buckets.items():
			actuals[f"{module_key}/{bucket}"] = stats["seconds"]
	return actuals


def run_perf_guard(test_elapsed: float, baseline_file: Path, timing_log_path, enforce: bool) -> int:
	"""Per-module perf guard: suite wall-clock + per-(module, bucket) Surefire test-time.

	Returns exit code: 0 = pass or warn-only mode, 1 = threshold breached with enforce=True.
	"""
	tolerance = float(os.environ.get("JUNEAU_CI_PERF_THRESHOLD", "0.20"))
	baselines = read_baselines(baseline_file)
	if not baselines:
		print(f"PERF-GUARD: baseline file not found or empty ({baseline_file}); skipping check.")
		return 0

	pct = f"±{tolerance * 100:.0f}%"
	perf_failed = False

	# Source of per-module actuals: prefer the latest timing-log run, else discover from surefire.
	actuals: dict = {}
	if timing_log_path is not None:
		log_path = Path(timing_log_path).expanduser()
		if log_path.exists():
			actuals = _latest_run_actuals(log_path)
	if not any(k != "suite" for k in actuals):
		actuals.update(_actuals_from_surefire(Path(__file__).parent.parent))
	# Suite wall-clock always comes from the measured subprocess time.
	actuals["suite"] = test_elapsed

	# 1) Suite-level guard.
	suite_baseline = baselines.get("suite")
	if suite_baseline is not None:
		threshold = suite_baseline * (1 + tolerance)
		if test_elapsed > threshold:
			print(
				f"PERF-GUARD FAIL [suite]: tests took {test_elapsed:.1f}s "
				f"(baseline {suite_baseline}s, tolerance {pct}, threshold {threshold:.1f}s).\n"
				f"  If intentional, bump the 'suite' entry in perf-baseline.txt (project root)."
			)
			perf_failed = True
		else:
			print(f"PERF-GUARD OK [suite]: tests took {test_elapsed:.1f}s (baseline {suite_baseline}s, tolerance {pct}).")
	else:
		print("PERF-GUARD WARN [suite]: no 'suite' baseline configured; skipping wall-clock check.")

	# 2) Per-module guards.
	module_keys = sorted(k for k in actuals if k != "suite")
	checked = 0
	regressions = 0
	new_modules = []
	for key in module_keys:
		actual = actuals[key]
		baseline = baselines.get(key)
		if baseline is None:
			new_modules.append(key)
			continue
		checked += 1
		threshold = baseline * (1 + tolerance)
		if actual > threshold:
			print(
				f"PERF-GUARD FAIL [{key}]: {actual:.1f}s "
				f"(baseline {baseline}s, tolerance {pct}, threshold {threshold:.1f}s).\n"
				f"  If intentional, bump '{key}' in perf-baseline.txt (project root)."
			)
			perf_failed = True
			regressions += 1

	for key in new_modules:
		print(f"PERF-GUARD WARN [{key}]: {actuals[key]:.1f}s — no baseline entry yet (new/unknown module; not failing).")

	print(
		f"PERF-GUARD SUMMARY: {checked} module-bucket(s) checked, {regressions} regression(s), "
		f"{len(new_modules)} new/unknown."
	)

	if perf_failed:
		if not enforce:
			print("PERF-GUARD: warn-only mode (pass --enforce-perf to hard-fail on breach).")
		return 1 if enforce else 0
	return 0


# Reactor-level parallel *module* builds.  -T1C runs one build thread per CPU core, so independent
# reactor modules (and their test forks) build concurrently, overlapping the otherwise-serial
# per-module test phases.  Requires the juneau-distrib dependency:copy ordering fix (MDEP-187) to be
# -T-safe.  This is parallel *module* execution only, NOT in-JVM concurrent test classes/methods.
PARALLELISM = "-T1C"


def build(verbose=False):
	return run_command(f"mvn clean install {PARALLELISM} -DskipTests", verbose)


def test(verbose=False, no_container=False):
	cmd = f"mvn test {PARALLELISM} -Drat.skip=true"
	if no_container:
		cmd += " -DexcludedGroups=container"
	return run_command(cmd, verbose)


def profile(module, verbose=False):
	ts = datetime.now().strftime("%Y%m%d-%H%M%S")
	profile_dir = Path("target/profile-results")
	profile_dir.mkdir(parents=True, exist_ok=True)
	safe_module = module.replace("/", "-")
	output_file = profile_dir / f"{safe_module}-{ts}.jfr"
	argline = f"-XX:StartFlightRecording=filename={output_file},settings=profile,dumponexit=true"
	cmd = f"mvn test -pl {module} -Drat.skip=true -DargLine='{argline}'"
	code, out = run_command(cmd, verbose)
	if code == 0:
		print(f"\n✅ JFR profile captured at {output_file}")
	return code, out


def main():  # NOSONAR python:S3776 -- Cognitive complexity is acceptable for this main function
	parser = argparse.ArgumentParser(add_help=False)
	parser.add_argument("--build-only", "-b", action="store_true")
	parser.add_argument("--test-only", "-t", action="store_true")
	parser.add_argument("--full", "-f", action="store_true")
	parser.add_argument("--verbose", "-v", action="store_true")
	parser.add_argument("--no-container", action="store_true")
	parser.add_argument("--timing-log")
	parser.add_argument("--enforce-perf", action="store_true")
	parser.add_argument("--profile")
	parser.add_argument("--help", "-h", action="store_true")
	args, unknown = parser.parse_known_args()
	if args.help:
		print(__doc__)
		return 0
	if unknown:
		print(f"Unknown option(s): {' '.join(unknown)}")
		print(__doc__)
		return 1

	build_only = args.build_only
	test_only = args.test_only
	full = args.full or not (build_only or test_only or args.profile)
	verbose = args.verbose

	if args.profile:
		exit_code, _ = profile(args.profile, verbose)
		return exit_code

	if build_only and test_only:
		print("Cannot combine --build-only and --test-only")
		return 1

	exit_code = 0
	last_test_output = ""
	if build_only or full:
		exit_code, _ = build(verbose)
		if exit_code != 0:
			print("\n❌ Build failed!")
			return exit_code
		print("\n✅ Build succeeded!")

	if test_only or full:
		if full:
			print("\n" + "=" * 80)
		if not args.enforce_perf:
			print("PERF-GUARD: warn-only mode (pass --enforce-perf to hard-fail on breach).")
		test_start = time.time()
		exit_code, last_test_output = test(verbose, no_container=args.no_container)
		test_elapsed = time.time() - test_start
		if exit_code != 0:
			_, failures, errors = parse_test_results(last_test_output)
			if failures is not None and errors is not None:
				print(f"\n❌ Tests failed! ({failures + errors} failed: {failures} failures, {errors} errors)")
			else:
				print("\n❌ Tests failed!")
		else:
			print("\n✅ Tests passed!")
		if args.timing_log:
			write_timing_log(Path(args.timing_log), passed=(exit_code == 0), test_elapsed=test_elapsed)
		if exit_code != 0:
			return exit_code
		baseline_file = Path(__file__).parent.parent / "perf-baseline.txt"
		perf_exit = run_perf_guard(test_elapsed, baseline_file, args.timing_log, enforce=args.enforce_perf)
		if perf_exit != 0:
			return perf_exit
	return exit_code

if __name__ == '__main__':
    sys.exit(main())
