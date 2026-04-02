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
Coverage reporter for Apache Juneau.

Shows JaCoCo branch and instruction coverage for a source file or folder.

Usage:
    ./scripts/coverage.py <path> [options]

Arguments:
    path    A source file (.java) or source folder to report on.
            Paths can be absolute or relative to the repo root.

Options:
    --run, -r       Re-run tests before reporting (updates the .exec data).
    --branches, -b  Show only lines with missed branches (default: show all uncovered).
    --help, -h      Show this help message.

Examples:
    ./scripts/coverage.py juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/conversion/
    ./scripts/coverage.py juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/conversion/BasicConverter.java
    ./scripts/coverage.py juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/conversion/ --run
"""

import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
UTEST_MODULE = REPO_ROOT / "juneau-utest"
UTEST_EXEC = UTEST_MODULE / "target" / "jacoco.exec"

SRC_MARKERS = ["src/main/java", "src/test/java"]


def die(msg):
    print(f"ERROR: {msg}", file=sys.stderr)
    sys.exit(1)


def find_maven_module(path: Path) -> Path:
    """Walk up from path to find the nearest directory containing a pom.xml."""
    p = path if path.is_dir() else path.parent
    while p != REPO_ROOT.parent:
        if (p / "pom.xml").exists() and p != REPO_ROOT:
            return p
        p = p.parent
    return None


def path_to_jacoco_package(path: Path) -> tuple[str | None, str | None]:
    """
    Given a source path, return (jacoco_package, filename).
    jacoco_package uses slash separators (e.g. org/apache/juneau/commons/conversion).
    filename is the .java filename, or None if path is a directory.
    """
    parts = path.parts
    for marker in SRC_MARKERS:
        marker_parts = tuple(marker.split("/"))
        for i in range(len(parts) - len(marker_parts) + 1):
            if parts[i:i + len(marker_parts)] == marker_parts:
                remainder = parts[i + len(marker_parts):]
                if path.is_file():
                    pkg = "/".join(remainder[:-1])
                    fname = remainder[-1]
                else:
                    pkg = "/".join(remainder).rstrip("/")
                    fname = None
                return pkg, fname
    return None, None


def run_tests():
    """Re-run all tests in juneau-utest to refresh the .exec file."""
    print("Running tests to refresh coverage data...")
    result = subprocess.run(
        ["mvn", "test", "-Drat.skip=true", "-q"],
        cwd=UTEST_MODULE,
        capture_output=True,
        text=True
    )
    if result.returncode != 0:
        print(result.stderr[-3000:], file=sys.stderr)
        die("Tests failed. Fix failures before checking coverage.")
    print("Tests passed.\n")


def generate_report(module: Path) -> Path:
    """Generate JaCoCo XML report for the given module using the utest exec file."""
    xml_path = module / "target" / "site" / "jacoco" / "jacoco.xml"
    result = subprocess.run(
        ["mvn", "jacoco:report", f"-Djacoco.dataFile={UTEST_EXEC}", "-q"],
        cwd=module,
        capture_output=True,
        text=True
    )
    if result.returncode != 0:
        print(result.stderr[-2000:], file=sys.stderr)
        die(f"Failed to generate JaCoCo report for {module}.")
    return xml_path


def bar(covered, total, width=20):
    """Render a simple ASCII progress bar."""
    if total == 0:
        filled = width
    else:
        filled = round(covered / total * width)
    return "[" + "#" * filled + "." * (width - filled) + "]"


def pct(covered, total):
    if total == 0:
        return "100%"
    return f"{covered / total * 100:.0f}%"


def report(xml_path: Path, pkg_filter: str, file_filter: str | None, branches_only: bool):
    """Parse jacoco.xml and print coverage for the matching package/file."""
    if not xml_path.exists():
        die(f"JaCoCo report not found at {xml_path}. Run with --run to generate it.")

    tree = ET.parse(xml_path)
    root = tree.getroot()

    matched_packages = []
    for pkg in root.findall("package"):
        name = pkg.get("name", "")
        if pkg_filter and not (name == pkg_filter or name.startswith(pkg_filter + "/")):
            continue
        matched_packages.append(pkg)

    if not matched_packages:
        die(f"No JaCoCo data found for package '{pkg_filter}'.\n"
            f"Make sure the module is built and the exec file is up to date.")

    # Collect per-file data
    files_data = []  # list of (pkg_name, fname, lines_with_issues)
    total_mb = total_cb = total_mi = total_ci = 0

    for pkg in matched_packages:
        pkg_name = pkg.get("name", "")
        for sf in pkg.findall("sourcefile"):
            fname = sf.get("name", "")
            if file_filter and fname != file_filter:
                continue

            # Aggregate counters from <counter> elements
            mb = cb = mi = ci = 0
            for ctr in sf.findall("counter"):
                t = ctr.get("type")
                m = int(ctr.get("missed", 0))
                c = int(ctr.get("covered", 0))
                if t == "BRANCH":
                    mb, cb = m, c
                elif t == "INSTRUCTION":
                    mi, ci = m, c

            total_mb += mb
            total_cb += cb
            total_mi += mi
            total_ci += ci

            # Collect uncovered lines
            uncovered = []
            for line in sf.findall("line"):
                ln = int(line.get("nr", 0))
                lmb = int(line.get("mb", 0))
                lmi = int(line.get("mi", 0))
                if branches_only and lmb > 0:
                    lcb = int(line.get("cb", 0))
                    uncovered.append((ln, lmb, lmb + lcb, lmi))
                elif not branches_only and (lmb > 0 or lmi > 0):
                    lcb = int(line.get("cb", 0))
                    uncovered.append((ln, lmb, lmb + lcb, lmi))

            files_data.append((pkg_name, fname, mb, cb, mi, ci, uncovered))

    if not files_data:
        print(f"No data found for the specified path.")
        return

    # Print per-file results
    for pkg_name, fname, mb, cb, mi, ci, uncovered in sorted(files_data):
        branch_total = mb + cb
        instr_total = mi + ci
        branch_pct = pct(cb, branch_total)
        instr_pct = pct(ci, instr_total)
        print(f"\n{'='*70}")
        print(f"  {pkg_name.replace('/', '.')}.{fname.removesuffix('.java')}")
        print(f"{'='*70}")
        print(f"  Branches:     {bar(cb, branch_total)}  {branch_pct:>4}  ({cb}/{branch_total} covered, {mb} missed)")
        print(f"  Instructions: {bar(ci, instr_total)}  {instr_pct:>4}  ({ci}/{instr_total} covered, {mi} missed)")
        if uncovered:
            print(f"\n  Uncovered lines:")
            for ln, lmb, ltotal, lmi in sorted(uncovered):
                parts = []
                if lmb > 0:
                    parts.append(f"{lmb}/{ltotal} branches missed")
                if lmi > 0:
                    parts.append(f"{lmi} instructions missed")
                print(f"    line {ln:4d}:  {', '.join(parts)}")
        else:
            print(f"\n  All lines covered!")

    # Print summary if multiple files
    if len(files_data) > 1:
        branch_total = total_mb + total_cb
        instr_total = total_mi + total_ci
        print(f"\n{'='*70}")
        print(f"  TOTAL SUMMARY")
        print(f"{'='*70}")
        print(f"  Branches:     {bar(total_cb, branch_total)}  {pct(total_cb, branch_total):>4}  ({total_cb}/{branch_total} covered, {total_mb} missed)")
        print(f"  Instructions: {bar(total_ci, instr_total)}  {pct(total_ci, instr_total):>4}  ({total_ci}/{instr_total} covered, {total_mi} missed)")
        print()


def main():
    args = sys.argv[1:]
    if not args or "--help" in args or "-h" in args:
        print(__doc__)
        return 0

    path_arg = None
    do_run = False
    branches_only = False

    for arg in args:
        if arg in ("--run", "-r"):
            do_run = True
        elif arg in ("--branches", "-b"):
            branches_only = True
        elif arg.startswith("-"):
            die(f"Unknown option: {arg}")
        else:
            path_arg = arg

    if not path_arg:
        die("No path specified.")

    path = Path(path_arg)
    if not path.is_absolute():
        path = REPO_ROOT / path
    path = path.resolve()

    if not path.exists():
        die(f"Path does not exist: {path}")

    module = find_maven_module(path)
    if not module:
        die(f"Could not determine Maven module for path: {path}")

    pkg_filter, file_filter = path_to_jacoco_package(path)
    if pkg_filter is None:
        die(f"Path does not appear to be under src/main/java or src/test/java: {path}")

    if do_run:
        run_tests()

    if not UTEST_EXEC.exists():
        die(f"No exec file found at {UTEST_EXEC}. Run with --run first.")

    print(f"Generating JaCoCo report for module: {module.relative_to(REPO_ROOT)}")
    xml_path = generate_report(module)

    report(xml_path, pkg_filter, file_filter, branches_only)
    return 0


if __name__ == "__main__":
    sys.exit(main())
