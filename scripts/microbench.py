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
JMH micro-benchmark runner for Apache Juneau.

Usage:
    python3 scripts/microbench.py observability [options]
    python3 scripts/microbench.py runtime [options]

Subcommands:
    observability   Run ObservabilityNoopBenchmark and assert gc.alloc.rate.norm ≤ threshold.
    runtime         Run the runtime suite (org.apache.juneau.microbench.runtime): JSON/XML/HTML
                    serialize and parse, MarshallingContext/ClassMeta first use and cached, cold-JVM
                    first serialize/parse, and a MockRestClient round trip.  Prints ops/s (or time/op)
                    and gc.alloc.rate.norm per benchmark.

Options:
    --threshold <bytes>   (observability) Max allowed gc.alloc.rate.norm in bytes/op (default: 8).
    --include <regex>     (runtime) JMH include regex; repeatable (default: the whole suite).
    --forks <n>           (runtime) Override the per-class fork count.
    --warmup <n>          (runtime) Override the warmup iteration count.
    --iterations <n>      (runtime) Override the measurement iteration count.
    --time <seconds>      (runtime) Override the warmup and measurement iteration time.
    --quick               (runtime) Smoke run: 1 fork, 2 measurement iterations, 1s iterations.
    --jfr                 (runtime) Also record a JFR file per benchmark (JMH -prof jfr).
    --out-dir <dir>       (runtime) Where to write results (default: juneau-integration-tests/target/jmh-results).
    --verbose, -v         Stream full Maven output instead of tail-only.
    --help, -h            Show this message.

Exit codes:
    0   Benchmark passed (alloc rate within threshold; for runtime, the suite ran).
    1   Benchmark failed (alloc rate exceeded threshold or Maven error).

The benchmark compiles and runs via:
    mvn -pl juneau-integration-tests -Pmicrobench test-compile exec:java

JSON output is written to juneau-integration-tests/jmh-results/observability-YYYY-MM-DD.json.
The gc.alloc.rate.norm secondary metric (bytes allocated per operation) is read from
that file to assert the zero-allocation contract.

To record the baseline in perf-baseline.txt (project root) after a clean run:
    1. Run this script and note the reported alloc bytes/op.
    2. Update the [observability] section in perf-baseline.txt manually.

The runtime suite runs through the reactor instead, so it measures this checkout's code and needs no
prior ``mvn install``:
    mvn -pl juneau-integration-tests -Pmicrobench-runtime process-test-classes -Dmicrobench.args="..."

JMH runs in its own JVM (exec:exec), so benchmarks fork normally.  JSON output is written to
<out-dir>/runtime-YYYY-MM-DD-HHMMSS.json, and with --jfr the recordings go to
<out-dir>/jfr-YYYY-MM-DD-HHMMSS/<benchmark>/.  The default out-dir is under target/ so runs leave no
untracked files.
"""

import argparse
import glob
import json
import os
import subprocess
import sys
import time
from pathlib import Path


REPO_ROOT = Path(__file__).parent.parent
INTEGRATION_DIR = REPO_ROOT / "juneau-integration-tests"
JMH_RESULTS_DIR = INTEGRATION_DIR / "jmh-results"

# Benchmark class name → subcommand mapping
BENCHMARK_CLASSES = {
    "observability": "org.apache.juneau.microbench.observability.ObservabilityNoopBenchmark",
}

RUNTIME_PACKAGE = "org.apache.juneau.microbench.runtime"
RUNTIME_RESULTS_DIR = INTEGRATION_DIR / "target" / "jmh-results"

# Default alloc threshold in bytes/op.  Tighten to 0 once confirmed empirically.
DEFAULT_THRESHOLD_BYTES = 8.0


def run_benchmark(main_class: str, verbose: bool) -> tuple[int, str]:
    """Compile and run the benchmark via Maven, returning (exit_code, combined_output).

    Uses ``-f juneau-integration-tests/pom.xml`` instead of ``-pl juneau-integration-tests`` to avoid the
    ``--also-make`` in ``.mvn/maven.config`` causing exec:java to also run on the root project.
    Upstream deps are expected to be installed in ~/.m2 (i.e. ``mvn install`` was run recently).
    """
    cmd = (
        f"mvn -f juneau-integration-tests/pom.xml -Pmicrobench test-compile exec:java "
        f"-Dexec.mainClass={main_class}"
    )
    print(f"Running: {cmd}")
    print("-" * 80)
    result = subprocess.run(
        cmd,
        shell=True,
        cwd=str(REPO_ROOT),
        capture_output=not verbose,
        text=True,
    )
    if verbose:
        combined = ""
    else:
        combined = (result.stdout or "") + (result.stderr or "")
        lines = combined.splitlines()
        # Print the tail so JMH benchmark output is visible
        tail = lines[-80:] if len(lines) > 80 else lines
        print("\n".join(tail))
    return result.returncode, combined


def find_latest_json(prefix: str) -> Path | None:
    """Return the most-recently-modified JSON file matching the prefix pattern."""
    JMH_RESULTS_DIR.mkdir(parents=True, exist_ok=True)
    pattern = str(JMH_RESULTS_DIR / f"{prefix}-*.json")
    matches = sorted(glob.glob(pattern), key=os.path.getmtime, reverse=True)
    return Path(matches[0]) if matches else None


def parse_alloc_rate_norm(json_path: Path) -> float | None:
    """
    Extract gc.alloc.rate.norm from a JMH JSON result file.

    The GC profiler emits a secondary metric named
    ``<benchmark>.gc.alloc.rate.norm`` with score in B/op.
    Returns None if the metric is not found (e.g. the profiler was not active).
    """
    try:
        with open(json_path) as f:
            results = json.load(f)
    except (json.JSONDecodeError, OSError) as e:
        print(f"❌ Could not read JMH result file {json_path}: {e}")
        return None

    # JMH JSON: list of benchmark result objects, each may have a
    # "secondaryMetrics" dict with keys like "·gc.alloc.rate.norm".
    for entry in results:
        secondary = entry.get("secondaryMetrics", {})
        for key, metric in secondary.items():
            if "gc.alloc.rate.norm" in key:
                score = metric.get("score")
                if score is not None:
                    return float(score)
    return None


def cmd_observability(args: argparse.Namespace) -> int:
    """Run the observability NoOp benchmark and assert the allocation threshold."""
    threshold = args.threshold
    main_class = BENCHMARK_CLASSES["observability"]

    print("=" * 70)
    print("🔬 ObservabilityNoopBenchmark — NoOp hot path allocation assertion")
    print(f"   Threshold: ≤ {threshold} bytes/op (gc.alloc.rate.norm)")
    print("=" * 70)

    t0 = time.monotonic()
    exit_code, _ = run_benchmark(main_class, args.verbose)
    elapsed = time.monotonic() - t0

    if exit_code != 0:
        print(f"\n❌ Benchmark run failed (Maven exit code {exit_code})")
        return 1

    print(f"\n✅ Benchmark completed in {elapsed:.1f}s — reading results...")

    json_path = find_latest_json("observability")
    if json_path is None:
        print(f"❌ No result JSON found in {JMH_RESULTS_DIR}")
        return 1

    alloc = parse_alloc_rate_norm(json_path)
    if alloc is None:
        print(
            f"❌ gc.alloc.rate.norm not found in {json_path}.\n"
            "   Make sure the benchmark was compiled with -prof gc enabled in main()."
        )
        return 1

    print(f"\n📊 gc.alloc.rate.norm = {alloc:.3f} bytes/op  (threshold ≤ {threshold})")
    print(f"   Results written to: {json_path}")

    if alloc <= threshold:
        print(f"\n✅ PASS — allocation rate {alloc:.3f} B/op is within threshold {threshold} B/op")
        print(
            "\nTo update perf-baseline.txt, add or refresh the [observability] section:\n"
            f"  observability-noop-alloc-bytes-per-op = {alloc:.3f}"
        )
        return 0
    else:
        print(
            f"\n❌ FAIL — allocation rate {alloc:.3f} B/op exceeds threshold {threshold} B/op\n"
            "   The observability NoOp path is allocating unexpectedly.\n"
            "   Check for:\n"
            "     • varargs array creation in MetricsRecorder.record() or TracerHook.startSpan()\n"
            "     • Duration.ofNanos() not being elided by escape analysis\n"
            "     • A recent refactor introducing allocations on the hot path\n"
            "   Re-run with --verbose for full JMH output."
        )
        return 1


def runtime_jmh_args(args: argparse.Namespace, json_path: Path, jfr_dir: Path | None) -> list[str]:
    """Build the JMH command line for the runtime suite."""
    jmh = list(args.include) if args.include else [RUNTIME_PACKAGE + "."]
    jmh += ["-prof", "gc", "-rf", "json", "-rff", str(json_path)]
    forks, warmup, iterations, seconds = args.forks, args.warmup, args.iterations, args.time
    if args.quick:
        # Warmup count is left alone so ColdStartBenchmark keeps its zero-warmup single shot.
        forks, iterations, seconds = 1, 2, 1
    if forks is not None:
        jmh += ["-f", str(forks)]
    if warmup is not None:
        jmh += ["-wi", str(warmup)]
    if iterations is not None:
        jmh += ["-i", str(iterations)]
    if seconds is not None:
        jmh += ["-w", f"{seconds}s", "-r", f"{seconds}s"]
    if jfr_dir is not None:
        jmh += ["-prof", f"jfr:dir={jfr_dir}"]
    return jmh


def format_runtime_results(json_path: Path) -> list[str]:
    """Render one line per benchmark: name, params, score and gc.alloc.rate.norm."""
    with open(json_path) as f:
        results = json.load(f)
    lines = [f"{'Benchmark':<52} {'Params':<14} {'Score':>14} {'Error':>12}  {'Unit':<8} {'Alloc B/op':>12}"]
    for entry in results:
        name = entry["benchmark"].removeprefix(RUNTIME_PACKAGE + ".")
        params = ",".join(f"{k}={v}" for k, v in (entry.get("params") or {}).items())
        primary = entry["primaryMetric"]
        error = primary.get("scoreError")
        error_text = f"{error:.3f}" if isinstance(error, (int, float)) and error == error else "-"
        alloc = None
        for key, metric in entry.get("secondaryMetrics", {}).items():
            if key.endswith("gc.alloc.rate.norm"):
                alloc = metric.get("score")
        alloc_text = f"{alloc:.1f}" if alloc is not None else "-"
        lines.append(
            f"{name:<52} {params:<14} {primary['score']:>14.3f} {error_text:>12}  {primary['scoreUnit']:<8} {alloc_text:>12}"
        )
    return lines


def cmd_runtime(args: argparse.Namespace) -> int:
    """Run the runtime benchmark suite and print a results table."""
    stamp = time.strftime("%Y-%m-%d-%H%M%S")
    out_dir = Path(args.out_dir).resolve() if args.out_dir else RUNTIME_RESULTS_DIR
    out_dir.mkdir(parents=True, exist_ok=True)
    json_path = out_dir / f"runtime-{stamp}.json"
    jfr_dir = out_dir / f"jfr-{stamp}" if args.jfr else None
    jmh = runtime_jmh_args(args, json_path, jfr_dir)

    print("=" * 70)
    print("Runtime micro-benchmarks (" + RUNTIME_PACKAGE + ")")
    print("   JMH args: " + " ".join(jmh))
    print("=" * 70)

    cmd = [
        "mvn", "-pl", "juneau-integration-tests", "-Pmicrobench-runtime", "process-test-classes",
        "-Dmicrobench.args=" + " ".join(jmh),
    ]
    print("Running: " + " ".join(cmd))
    print("-" * 80)
    t0 = time.monotonic()
    result = subprocess.run(cmd, cwd=str(REPO_ROOT), capture_output=not args.verbose, text=True)
    elapsed = time.monotonic() - t0
    if not args.verbose:
        lines = ((result.stdout or "") + (result.stderr or "")).splitlines()
        print("\n".join(lines[-80:]))

    if result.returncode != 0:
        print(f"\n❌ Benchmark run failed (Maven exit code {result.returncode})")
        return 1
    if not json_path.exists():
        print(f"❌ No result JSON at {json_path}")
        return 1

    print(f"\n✅ Suite completed in {elapsed:.1f}s\n")
    print("\n".join(format_runtime_results(json_path)))
    print(f"\nResults written to: {json_path}")
    if jfr_dir is not None:
        print(f"JFR recordings in:  {jfr_dir}")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(
        description="JMH micro-benchmark runner for Apache Juneau.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    parser.add_argument(
        "--verbose", "-v",
        action="store_true",
        help="Stream full Maven output (default: tail-only).",
    )
    sub = parser.add_subparsers(dest="subcommand", required=True)

    obs = sub.add_parser(
        "observability",
        help="Assert the RestOpInvoker observability NoOp block allocates ≤ threshold bytes/op.",
    )
    obs.add_argument(
        "--threshold",
        type=float,
        default=DEFAULT_THRESHOLD_BYTES,
        metavar="BYTES",
        help=f"Max allowed gc.alloc.rate.norm in bytes/op (default: {DEFAULT_THRESHOLD_BYTES}).",
    )

    rt = sub.add_parser(
        "runtime",
        help="Run the runtime suite (marshalling, ClassMeta, cold start, MockRestClient round trip).",
    )
    rt.add_argument("--include", action="append", metavar="REGEX",
        help="JMH include regex; repeatable (default: the whole runtime suite).")
    rt.add_argument("--forks", type=int, metavar="N", help="Override the per-class fork count.")
    rt.add_argument("--warmup", type=int, metavar="N",
        help="Override the warmup iteration count (also gives ColdStartBenchmark warmups; exclude it).")
    rt.add_argument("--iterations", type=int, metavar="N", help="Override the measurement iteration count.")
    rt.add_argument("--time", type=int, metavar="SECONDS", help="Override the warmup and measurement iteration time.")
    rt.add_argument("--quick", action="store_true",
        help="Smoke run: 1 fork, 2 measurement iterations, 1s iterations.")
    rt.add_argument("--jfr", action="store_true", help="Also record a JFR file per benchmark (JMH -prof jfr).")
    rt.add_argument("--out-dir", metavar="DIR",
        help="Where to write results (default: juneau-integration-tests/target/jmh-results).")

    args = parser.parse_args()

    if args.subcommand == "observability":
        return cmd_observability(args)
    if args.subcommand == "runtime":
        return cmd_runtime(args)

    parser.print_help()
    return 1


if __name__ == "__main__":
    sys.exit(main())
