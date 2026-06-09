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

Subcommands:
    observability   Run ObservabilityNoopBenchmark and assert gc.alloc.rate.norm ≤ threshold.

Options:
    --threshold <bytes>   Max allowed gc.alloc.rate.norm in bytes/op (default: 8).
    --verbose, -v         Stream full Maven output instead of tail-only.
    --help, -h            Show this message.

Exit codes:
    0   Benchmark passed (alloc rate within threshold).
    1   Benchmark failed (alloc rate exceeded threshold or Maven error).

The benchmark compiles and runs via:
    mvn -pl juneau-integration-tests -Pmicrobench test-compile exec:java

JSON output is written to juneau-integration-tests/jmh-results/observability-YYYY-MM-DD.json.
The gc.alloc.rate.norm secondary metric (bytes allocated per operation) is read from
that file to assert the zero-allocation contract.

To record the baseline in perf-baseline.txt (project root) after a clean run:
    1. Run this script and note the reported alloc bytes/op.
    2. Update the [observability] section in perf-baseline.txt manually.
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

    args = parser.parse_args()

    if args.subcommand == "observability":
        return cmd_observability(args)

    parser.print_help()
    return 1


if __name__ == "__main__":
    sys.exit(main())
