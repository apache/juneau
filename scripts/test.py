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
    --timing-log <path>      Append per-bucket timing JSONL records
    --enforce-perf           Hard-fail if wall-clock exceeds perf-baseline.txt ±20% tolerance
    --profile <module>       Run one-shot JFR profile for module tests
    --help, -h               Show this help message

Perf guard:
    --enforce-perf compares the actual tests-only wall-clock against the suite baseline in
    juneau-utest/perf-baseline.txt (line 1).  Uses env-var JUNEAU_CI_PERF_THRESHOLD (default
    0.20, i.e. ±20%) — separate from JUNEAU_PUSH_TIMING_THRESHOLD.

    When --timing-log is also passed the per-bucket Surefire XML totals are compared against
    the core / container.springboot / container.jetty baselines (lines 2–4 of perf-baseline.txt).

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


def parse_surefire_dir(directory: Path):
	tests, seconds = 0, 0.0
	if not directory.exists():
		return tests, seconds
	for xml_file in sorted(directory.glob("TEST-*.xml")):
		root = ET.parse(xml_file).getroot()
		tests += int(root.attrib.get("tests", 0))
		seconds += float(root.attrib.get("time", 0.0))
	return tests, seconds


def container_bucket(module_dir: Path, class_name: str):
	source = module_dir / "src" / "test" / "java" / Path("/".join(class_name.split("."))).with_suffix(".java")
	if source.exists():
		content = source.read_text(encoding="utf-8")
		if "SpringbootTest" in content:
			return "container.springboot"
		if "JettyMicroserviceTest" in content:
			return "container.jetty"
	return "container.springboot" if "springboot" in class_name.lower() else "container.jetty"


def parse_container_subbuckets(module_dir: Path, directory: Path):
	buckets = {
		"container.springboot": {"tests": 0, "seconds": 0.0},
		"container.jetty": {"tests": 0, "seconds": 0.0},
	}
	if not directory.exists():
		return buckets
	for xml_file in sorted(directory.glob("TEST-*.xml")):
		root = ET.parse(xml_file).getroot()
		bucket = container_bucket(module_dir, root.attrib.get("name", ""))
		buckets[bucket]["tests"] += int(root.attrib.get("tests", 0))
		buckets[bucket]["seconds"] += float(root.attrib.get("time", 0.0))
	return buckets


def write_timing_log(path: Path, passed: bool):
	path = path.expanduser()
	path.parent.mkdir(parents=True, exist_ok=True)
	repo_root = Path(__file__).parent.parent
	module = "juneau-utest"
	module_dir = repo_root / module
	reports_root = module_dir / "target" / "surefire-reports"

	core_tests, core_seconds = parse_surefire_dir(reports_root / "core")
	container_tests, container_seconds = parse_surefire_dir(reports_root / "container")
	container_sub = parse_container_subbuckets(module_dir, reports_root / "container")

	run_id = f"{datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')}-{git_value(['rev-parse', '--short', 'HEAD'])}"
	base = {
		"ts": datetime.now(timezone.utc).isoformat(),
		"run_id": run_id,
		"branch": git_value(["rev-parse", "--abbrev-ref", "HEAD"]),
		"commit": git_value(["rev-parse", "--short", "HEAD"]),
		"module": module,
		"passed": passed,
	}
	rows = [
		{**base, "execution": "core", "wallclock_s": core_seconds, "test_count": core_tests},
		{**base, "execution": "container", "wallclock_s": container_seconds, "test_count": container_tests},
		{
			**base,
			"execution": "container.springboot",
			"wallclock_s": container_sub["container.springboot"]["seconds"],
			"test_count": container_sub["container.springboot"]["tests"],
		},
		{
			**base,
			"execution": "container.jetty",
			"wallclock_s": container_sub["container.jetty"]["seconds"],
			"test_count": container_sub["container.jetty"]["tests"],
		},
	]
	with path.open("a", encoding="utf-8") as f:
		for row in rows:
			f.write(json.dumps(row) + "\n")
	print(f"🕒 Timing metrics appended to {path}")


def read_baselines(baseline_file: Path) -> dict:
	"""Read baselines from perf-baseline.txt.

	Returns a dict mapping label to float:
	  'suite'               — line 1, suite wall-clock (used by --enforce-perf).
	  'core'                — line 2, core Surefire XML total.
	  'container.springboot'— line 3, springboot Surefire XML total.
	  'container.jetty'     — line 4, jetty Surefire XML total.
	"""
	keys = ["suite", "core", "container.springboot", "container.jetty"]
	result = {}
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
			value = float(stripped.split()[0])
			result[keys[idx]] = value
			idx += 1
		except (ValueError, IndexError):
			continue
	return result


def _read_latest_jsonl_run(log_path: Path) -> dict:
	"""Return {execution: wallclock_s} for the latest run_id in the JSONL."""
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
	result = {}
	for row in rows:
		if row.get("run_id") == latest_run_id:
			execution = row.get("execution", "")
			result[execution] = float(row.get("wallclock_s", 0.0))
	return result


def run_perf_guard(test_elapsed: float, baseline_file: Path, timing_log_path, enforce: bool) -> int:
	"""Run the v1 (suite wall-clock) and v2 (per-bucket Surefire XML) perf guards.

	Returns exit code: 0 = pass or warn-only mode, 1 = threshold breached with enforce=True.
	"""
	tolerance = float(os.environ.get("JUNEAU_CI_PERF_THRESHOLD", "0.20"))
	baselines = read_baselines(baseline_file)
	if not baselines:
		print(f"PERF-GUARD: baseline file not found ({baseline_file}); skipping check.")
		return 0

	pct = f"±{tolerance * 100:.0f}%"
	perf_failed = False

	suite_baseline = baselines.get("suite")
	if suite_baseline is not None:
		threshold = suite_baseline * (1 + tolerance)
		if test_elapsed > threshold:
			print(
				f"PERF-GUARD FAIL: tests took {test_elapsed:.1f}s "
				f"(baseline {suite_baseline}s, tolerance {pct}, threshold {threshold:.1f}s).\n"
				f"If this is intentional, bump juneau-utest/perf-baseline.txt and the FINISHED archive's ## Perf block in the same PR."
			)
			perf_failed = True
		else:
			print(f"PERF-GUARD OK: tests took {test_elapsed:.1f}s (baseline {suite_baseline}s, tolerance {pct}).")

	if timing_log_path is not None:
		log_path = Path(timing_log_path).expanduser()
		if log_path.exists():
			latest_run = _read_latest_jsonl_run(log_path)
			for bucket in ("core", "container.springboot", "container.jetty"):
				bucket_actual = latest_run.get(bucket)
				bucket_baseline = baselines.get(bucket)
				if bucket_actual is None or bucket_baseline is None:
					continue
				bucket_threshold = bucket_baseline * (1 + tolerance)
				if bucket_actual > bucket_threshold:
					print(
						f"PERF-GUARD FAIL [{bucket}]: {bucket_actual:.1f}s "
						f"(baseline {bucket_baseline}s, tolerance {pct}, threshold {bucket_threshold:.1f}s).\n"
						f"If this is intentional, bump juneau-utest/perf-baseline.txt and the FINISHED archive's ## Perf block in the same PR."
					)
					perf_failed = True
				else:
					print(f"PERF-GUARD OK [{bucket}]: {bucket_actual:.1f}s (baseline {bucket_baseline}s, tolerance {pct}).")

	if perf_failed:
		if not enforce:
			print("PERF-GUARD: warn-only mode (pass --enforce-perf to hard-fail on breach).")
		return 1 if enforce else 0
	return 0


def build(verbose=False):
	return run_command("mvn clean install -DskipTests", verbose)


def test(verbose=False, no_container=False):
	cmd = "mvn test -Drat.skip=true"
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
			write_timing_log(Path(args.timing_log), passed=(exit_code == 0))
		if exit_code != 0:
			return exit_code
		baseline_file = Path(__file__).parent.parent / "juneau-utest" / "perf-baseline.txt"
		perf_exit = run_perf_guard(test_elapsed, baseline_file, args.timing_log, enforce=args.enforce_perf)
		if perf_exit != 0:
			return perf_exit
	return exit_code

if __name__ == '__main__':
    sys.exit(main())
