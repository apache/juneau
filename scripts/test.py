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
    --verbose, -v            Accepted for compatibility; Maven output always streams live
    --no-container           Exclude @Tag("container") tests
    --timing-log <path>      Append per-(module, bucket) timing JSONL records
    --enforce-perf           Hard-fail if wall-clock exceeds perf-baseline.txt ±20% tolerance
    --js-tests               Also run the headless-browser JS harness (mvn -Pjs-tests, *_BrowserTest in
                             juneau-rest-server-views).  Fails if Node/npm are missing.
    --no-js-tests            Never run the JS harness (overrides auto-detect)
    --console <mode>         Maven output on the console: full (default), condensed or none
    --detail <level>         Console detail: summary, actionable (default), modules or all (JUNEAU_RUN_DETAIL)
    --full-log <path>        Also write every byte Maven prints to this file (appended)
    --condensed-log <path>   Also write the condensed one-line-per-event stream to this file (appended)
    --profile <module>       Run one-shot JFR profile for module tests
    --help, -h               Show this help message

Environment:
    JUNEAU_MVN_WRAPPER       Optional prefix for every mvn command (e.g. a lock script that serializes
                             concurrent runs on one checkout).

JS tests:
    CI's js-tests job is the only thing that normally runs the browser tests (-Pjs-tests).  When neither
    --js-tests nor --no-js-tests is given, this script enables them automatically if any .js, .css or .ftl
    file under a src/ tree differs from origin/master (committed, uncommitted or untracked).  If Node/npm
    are not on the PATH, an auto-enabled run prints a notice and skips; an explicit --js-tests fails instead.
    The first run downloads Playwright + Chromium (needs network); later runs reuse target/js.

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
import importlib.util
import json
import os
import re
import shutil
import signal
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path


RUN_MODULE_PATH = Path(__file__).resolve().parent.parent / "juneau-run" / "src" / "main" / "python" / "juneau_run.py"
_step_n = 0


def run_module():
	"""The in-tree juneau_run module (shared if already loaded), or None if its source is not there."""
	module = sys.modules.get("juneau_run")
	if module is None and RUN_MODULE_PATH.exists():
		spec = importlib.util.spec_from_file_location("juneau_run", RUN_MODULE_PATH)
		module = importlib.util.module_from_spec(spec)
		sys.modules["juneau_run"] = module
		# Keep the module's source directory free of __pycache__ (the Maven build's RAT check scans it).
		previous, sys.dont_write_bytecode = sys.dont_write_bytecode, True
		try:
			spec.loader.exec_module(module)
		finally:
			sys.dont_write_bytecode = previous
	return module


def next_step_number():
	"""Top-level step numbers continue after JUNEAU_RUN_N_BASE, which push.py sets for the steps it spawns us for."""
	global _step_n
	_step_n += 1
	return int(os.environ.get("JUNEAU_RUN_N_BASE", "0")) + _step_n


def say(text, level="info"):
	"""Script status text: through juneau_run (a filtered note once a console view owns the screen), else print."""
	run = run_module()
	if run is None:
		print(text, flush=True)
	else:
		run.say(text, level)


def run_command(cmd, step_id=None, title=None, label=None):
	"""Run a command, streaming its output live, and return exit code and full output.

	With run markers on, or console/log routing requested (see juneau_run), the command runs as the protocol step
	`step_id` through juneau_run.  Otherwise it behaves exactly as it always has.
	"""
	script_dir = Path(__file__).parent
	project_root = script_dir.parent
	# Optional command prefix (e.g. a lock script that serializes concurrent mvn runs on one checkout).
	wrapper = os.environ.get("JUNEAU_MVN_WRAPPER", "").strip()
	if wrapper and cmd.startswith("mvn "):
		cmd = f"{wrapper} {cmd}"
	say(f"Running: {cmd}")
	say("-" * 80)  # NOSONAR python:S1192 - the same banner rule as the separator in _main; a constant hides it
	run = run_module()
	if run is not None and step_id and (run.enabled() or not run.Sinks().is_default):
		# Through a shell, like the default path, so a JUNEAU_MVN_WRAPPER prefix (which may use && or quoting) keeps its meaning.
		result = run.run_tool(["/bin/sh", "-c", cmd], "maven", step_id, title or step_id, n=next_step_number(),
			cwd=str(project_root), capture=True, label=label)
		return result.exit, result.output
	# Echo each line as it arrives so a long or hung build is visible, and keep it for parse_test_results().
	lines = []
	with subprocess.Popen(cmd, shell=True, cwd=str(project_root), stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
			text=True, errors="replace", bufsize=1) as proc:
		for line in proc.stdout:
			sys.stdout.write(line)
			sys.stdout.flush()
			lines.append(line)
	return proc.returncode, "".join(lines)


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


PERF_REPORT = Path(__file__).resolve().parent.parent / "target" / "perf-guard.txt"


class _PerfOutput:
	"""Where PERF-GUARD lines go: notes on the Perf row when a console view owns the screen, else print.  Every line
	also goes to the report file."""

	def __init__(self, report_file):
		run = run_module()
		self.run = run if run is not None and run.console_owns_screen() else None
		self.report_file = Path(report_file)
		self.lines = []

	def line(self, text, level="info"):
		self.lines.append(text)
		if self.run is not None:
			self.run.note(level, text, step="perf")
		else:
			print(text)

	def report_only(self, text):
		self.lines.append(text)

	def write(self):
		self.report_file.parent.mkdir(parents=True, exist_ok=True)
		self.report_file.write_text("\n".join(self.lines) + "\n", encoding="utf-8")


def run_perf_guard(test_elapsed: float, baseline_file: Path, timing_log_path, enforce: bool, report_file=None) -> int:
	"""Per-module perf guard: suite wall-clock + per-(module, bucket) Surefire test-time.

	A breach is a warn note, or an error with enforce=True.  Modules with no baseline collapse into one info note.
	The full report goes to report_file (target/perf-guard.txt); the Perf row shows the counts and its path.
	Returns exit code: 0 = pass or warn-only mode, 1 = threshold breached with enforce=True.
	"""
	run = run_module()
	if run is None:
		return _perf_guard(test_elapsed, baseline_file, timing_log_path, enforce, _PerfOutput(report_file or PERF_REPORT))[0]
	with run.row("perf", "Perf", label="Perf") as row:
		out = _PerfOutput(report_file or PERF_REPORT)
		code, summary = _perf_guard(test_elapsed, baseline_file, timing_log_path, enforce, out)
		row.summary = summary
		if code != 0:
			row.status = "fail"
	return code


def _perf_guard(test_elapsed, baseline_file, timing_log_path, enforce, out):
	"""The check itself.  Returns (exit code, Perf row summary)."""
	tolerance = float(os.environ.get("JUNEAU_CI_PERF_THRESHOLD", "0.20"))
	breach = "error" if enforce else "warn"
	baselines = read_baselines(baseline_file)
	if not baselines:
		out.line(f"PERF-GUARD: baseline file not found or empty ({baseline_file}); skipping check.", "warn")
		out.write()
		return 0, f"no baseline file → {out.report_file}"

	pct = f"±{tolerance * 100:.0f}%"
	over = 0

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
			out.line(
				f"PERF-GUARD FAIL [suite]: tests took {test_elapsed:.1f}s "
				f"(baseline {suite_baseline}s, tolerance {pct}, threshold {threshold:.1f}s).\n"
				f"  If intentional, bump the 'suite' entry in perf-baseline.txt (project root).", breach)
			over += 1
		else:
			out.line(f"PERF-GUARD OK [suite]: tests took {test_elapsed:.1f}s (baseline {suite_baseline}s, tolerance {pct}).")
	else:
		out.line("PERF-GUARD WARN [suite]: no 'suite' baseline configured; skipping wall-clock check.")

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
			out.line(
				f"PERF-GUARD FAIL [{key}]: {actual:.1f}s "
				f"(baseline {baseline}s, tolerance {pct}, threshold {threshold:.1f}s).\n"
				f"  If intentional, bump '{key}' in perf-baseline.txt (project root).", breach)
			regressions += 1
			over += 1

	lines = [f"PERF-GUARD WARN [{key}]: {actuals[key]:.1f}s — no baseline entry yet (new/unknown module; not failing)."
			 for key in new_modules]
	if out.run is None:
		for text in lines:
			out.line(text)
	else:
		for text in lines:
			out.report_only(text)
		if new_modules:
			out.line(f"PERF-GUARD: {len(new_modules)} module-bucket(s) have no baseline entry yet: "
					 + ", ".join(new_modules))

	out.line(
		f"PERF-GUARD SUMMARY: {checked} module-bucket(s) checked, {regressions} regression(s), "
		f"{len(new_modules)} new/unknown."
	)
	if over and not enforce:
		out.line("PERF-GUARD: warn-only mode (pass --enforce-perf to hard-fail on breach).")
	out.write()

	if over:
		summary = f"⚠ {over} over baseline" + (f" · {len(new_modules)} without one" if new_modules else "")
	else:
		summary = f"✓ {checked} checked" + (f" · {len(new_modules)} without a baseline" if new_modules else "")
	summary += f" → {out.report_file}"
	return (1 if over and enforce else 0), summary


# Reactor-level parallel *module* builds.  -T1C runs one build thread per CPU core, so independent
# reactor modules (and their test forks) build concurrently, overlapping the otherwise-serial
# per-module test phases.  Requires the juneau-distrib dependency:copy ordering fix (MDEP-187) to be
# -T-safe.  This is parallel *module* execution only, NOT in-JVM concurrent test classes/methods.
PARALLELISM = "-T1C"


def build():
	return run_command(f"mvn clean install {PARALLELISM} -DskipTests", "build", "Build", label="Compile")


def test(no_container=False):
	cmd = f"mvn test {PARALLELISM} -Drat.skip=true"
	if no_container:
		cmd += " -DexcludedGroups=container"
	return run_command(cmd, "tests", "Tests", label="Tests")


JS_TEST_MODULE = "juneau-rest/juneau-rest-server-views"
JS_SOURCE_SUFFIXES = (".js", ".css", ".ftl")


def is_js_source(path):
	"""True for a .js/.css/.ftl file that lives under a src/ tree (any module)."""
	p = path.replace("\\", "/")
	parts = p.split("/")
	return p.lower().endswith(JS_SOURCE_SUFFIXES) and "src" in parts[:-1]


def changed_files_vs_origin(repo_root=None, base="origin/master"):
	"""Paths changed in HEAD, the working tree or untracked, relative to `base`.  Empty list if git/base unavailable."""
	repo_root = repo_root or Path(__file__).parent.parent
	names = []
	try:
		for args in (["diff", "--name-only", base], ["ls-files", "--others", "--exclude-standard"]):
			r = subprocess.run(["git", *args], cwd=str(repo_root), capture_output=True, text=True, check=True)
			names.extend(line for line in r.stdout.splitlines() if line.strip())
	except Exception:
		return []
	return names


def should_run_js_tests(js_flag, no_js_flag, changed_files):
	"""Resolve (enabled, explicit).  Explicit flags win (--no-js-tests over --js-tests); otherwise auto-detect."""
	if no_js_flag:
		return False, False
	if js_flag:
		return True, True
	return any(is_js_source(f) for f in changed_files), False


def js_prereq_problem():
	"""Return a description of the missing browser prerequisite, or None if Node and npm are on the PATH."""
	missing = [tool for tool in ("node", "npm") if shutil.which(tool) is None]
	if missing:
		return f"{' and '.join(missing)} not found on the PATH (the -Pjs-tests profile needs Node and npm)"
	return None


def js_tests(installed=False):
	"""The JS harness.  After this run's install, -f builds only the module (upstream comes from ~/.m2); otherwise
	-pl -am rebuilds what it needs, since .mvn/maven.config's --also-make applies either way."""
	scope = f"-f {JS_TEST_MODULE}/pom.xml" if installed else f"-pl {JS_TEST_MODULE} -am"
	cmd = (f"mvn -Pjs-tests {scope} test -Drat.skip=true "
		"-Dtest='*_BrowserTest' -Dsurefire.failIfNoSpecifiedTests=false")
	return run_command(cmd, "js-tests", "JS browser tests", label="JS tests")


def maybe_run_js_tests(js_flag, no_js_flag, changed_files, runner=None, installed=False):
	"""Run the JS harness if enabled.  Returns 0 on pass/skip, non-zero on failure."""
	enabled, explicit = should_run_js_tests(js_flag, no_js_flag, changed_files)
	if not enabled:
		return 0
	problem = js_prereq_problem()
	if problem:
		if explicit:
			say(f"\n❌ --js-tests requested but cannot run: {problem}.", "error")
			return 1
		say(f"\n⚠️  JS files changed, but skipping JS tests: {problem}. (CI will still run them.)", "warn")
		return 0
	say("\n🌐 Running JS browser tests (-Pjs-tests)..." + ("" if explicit else " (auto: JS/CSS/FTL files changed)"))
	code, _ = (runner or (lambda: js_tests(installed)))()
	say("\n✅ JS tests passed!" if code == 0 else "\n❌ JS tests failed!", "info" if code == 0 else "error")
	return code


def profile(module):
	ts = datetime.now().strftime("%Y%m%d-%H%M%S")
	profile_dir = Path("target/profile-results")
	profile_dir.mkdir(parents=True, exist_ok=True)
	safe_module = module.replace("/", "-")
	output_file = profile_dir / f"{safe_module}-{ts}.jfr"
	# Overriding argLine deliberately drops the JaCoCo agent so instrumentation doesn't skew the profile.
	argline = f"-XX:StartFlightRecording=filename={output_file},settings=profile,dumponexit=true"
	cmd = f"mvn test -pl {module} -Drat.skip=true -DargLine='{argline}'"
	code, out = run_command(cmd, "profile", "Profile", label="Profile")
	if code == 0:
		say(f"\n✅ JFR profile captured at {output_file}", "warn")
	return code, out


def _main():  # NOSONAR python:S3776 -- Cognitive complexity is acceptable for this main function
	parser = argparse.ArgumentParser(add_help=False)
	parser.add_argument("--build-only", "-b", action="store_true")
	parser.add_argument("--test-only", "-t", action="store_true")
	parser.add_argument("--full", "-f", action="store_true")
	parser.add_argument("--verbose", "-v", action="store_true")
	parser.add_argument("--no-container", action="store_true")
	parser.add_argument("--timing-log")
	parser.add_argument("--enforce-perf", action="store_true")
	parser.add_argument("--js-tests", action="store_true", dest="js_tests")
	parser.add_argument("--no-js-tests", action="store_true", dest="no_js_tests")
	parser.add_argument("--profile")
	parser.add_argument("--console", choices=("full", "condensed", "none"))
	parser.add_argument("--detail")
	parser.add_argument("--full-log")
	parser.add_argument("--condensed-log")
	parser.add_argument("--help", "-h", action="store_true")
	args, unknown = parser.parse_known_args()
	if args.help:
		print(__doc__)
		return 0
	if unknown:
		print(f"Unknown option(s): {' '.join(unknown)}")
		print(__doc__)
		return 1
	# juneau_run reads its output routing from the environment; an argument beats a value already set there.
	for flag, variable in ((args.console, "JUNEAU_RUN_CONSOLE"), (args.full_log, "JUNEAU_RUN_FULL_LOG"),
			(args.condensed_log, "JUNEAU_RUN_CONDENSED_LOG")):
		if flag:
			os.environ[variable] = flag
	run = run_module()
	if run is not None:
		try:
			run.export_detail(args.detail)
		except ValueError as e:
			print(e)
			return 2
		if not run.Sinks().is_default:
			run.session(f"🧪 Juneau test · {git_value(['rev-parse', '--abbrev-ref', 'HEAD'])}")

	build_only = args.build_only
	test_only = args.test_only
	full = args.full or not (build_only or test_only or args.profile)

	if args.profile:
		exit_code, _ = profile(args.profile)
		return exit_code

	if build_only and test_only:
		print("Cannot combine --build-only and --test-only")
		return 1
	if args.js_tests and args.no_js_tests:
		print("Cannot combine --js-tests and --no-js-tests")
		return 1

	exit_code = 0
	last_test_output = ""
	if build_only or full:
		exit_code, _ = build()
		if exit_code != 0:
			say("\n❌ Build failed!", "error")
			return exit_code
		say("\n✅ Build succeeded!")

	if test_only or full:
		if full:
			say("\n" + "=" * 80)
		test_start = time.time()
		exit_code, last_test_output = test(no_container=args.no_container)
		test_elapsed = time.time() - test_start
		if exit_code != 0:
			_, failures, errors = parse_test_results(last_test_output)
			if failures is not None and errors is not None:
				say(f"\n❌ Tests failed! ({failures + errors} failed: {failures} failures, {errors} errors)", "error")
			else:
				say("\n❌ Tests failed!", "error")
		else:
			say("\n✅ Tests passed!")
		if args.timing_log:
			write_timing_log(Path(args.timing_log), passed=(exit_code == 0), test_elapsed=test_elapsed)
		if exit_code != 0:
			return exit_code
		baseline_file = Path(__file__).parent.parent / "perf-baseline.txt"
		perf_exit = run_perf_guard(test_elapsed, baseline_file, args.timing_log, enforce=args.enforce_perf)
		if perf_exit != 0:
			return perf_exit
		js_changed = [] if (args.js_tests or args.no_js_tests) else changed_files_vs_origin()
		js_exit = maybe_run_js_tests(args.js_tests, args.no_js_tests, js_changed, installed=full)
		if js_exit != 0:
			return js_exit
	return exit_code


def main():
	"""Owns the run-protocol `run`/`done` markers (both are no-ops unless RUN_MARKERS=1 and no enclosing run exists)."""
	run = run_module()
	previous_sigterm = None
	if run is not None and run.enabled():
		# Unwind to the KeyboardInterrupt handler below; run_tool tears the Maven process group down on the way.
		def on_sigterm(signum, frame):
			raise KeyboardInterrupt
		previous_sigterm = signal.signal(signal.SIGTERM, on_sigterm)
	try:
		if run is not None:
			run.run(mode="test", project=Path(__file__).resolve().parent.parent.name,
				branch=git_value(["rev-parse", "--abbrev-ref", "HEAD"]), head=git_value(["rev-parse", "--short", "HEAD"]))
		try:
			code = _main()
		except KeyboardInterrupt:
			if run is None or not (run.enabled() or run.console_active()):
				raise
			run.done("cancelled")
			print("Cancelled.", file=sys.stderr)
			return 130
		if run is not None:
			run.done("ok" if code == 0 else "fail")
		return code
	finally:
		if previous_sigterm is not None:
			signal.signal(signal.SIGTERM, previous_sigterm)


if __name__ == '__main__':
    sys.exit(main())
