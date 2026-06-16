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

Coverage data is aggregated across ALL modules' unit tests (plus the
integration-test suite), not just juneau-integration-tests.  JaCoCo writes a
separate target/jacoco.exec per Maven module, so a class exercised only by
unit tests in its own module (e.g. juneau-core/juneau-marshall) used to read
0% because only the integration exec was consulted.  This script discovers
every module's target/jacoco.exec and feeds them all -- together with each
owning module's target/classes -- to the JaCoCo CLI to produce one combined,
repo-wide report.

Usage:
    ./scripts/coverage.py <path> [options]

Arguments:
    path    A source file (.java) or source folder to report on.
            Paths can be absolute or relative to the repo root.

Options:
    --run, -r       Re-run the owning module's tests before reporting
                    (refreshes that module's .exec; other modules' existing
                    execs are still merged in).
    --branches, -b  Show only lines with missed branches (default: show all uncovered).
    --help, -h      Show this help message.

Examples:
    ./scripts/coverage.py juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/conversion/
    ./scripts/coverage.py juneau-core/juneau-marshall/src/main/java/org/apache/juneau/marshall/BitSetFormat.java
    ./scripts/coverage.py juneau-core/juneau-marshall/src/main/java/org/apache/juneau/marshall/BitSetFormat.java --run
"""

import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent

SRC_MARKERS = ["src/main/java", "src/test/java"]

# Combined, repo-wide JaCoCo XML report assembled from every module's exec file.
# Lives under target/ (git-ignored); regenerated on every run.
COMBINED_XML = REPO_ROOT / "target" / "coverage" / "jacoco.xml"

# Modules excluded from the combined report:
#  - juneau-shaded / juneau-distrib repackage classes from other modules, which would
#    introduce duplicate-class entries into the JaCoCo report.
#  - examples / petstore / microservice / sc / test-utils are outside the coverage
#    scope (mirrors <sonar.coverage.exclusions> in the root pom.xml).
EXCLUDED_MODULE_MARKERS = (
    "juneau-shaded",
    "juneau-distrib",
    "juneau-examples",
    "juneau-petstore",
    "juneau-microservice",
    "juneau-sc",
    "juneau-test-utils",
)


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


def discover_modules() -> list[Path]:
    """Return all build modules (dirs with a pom.xml) excluding the reactor root and excluded markers."""
    poms = list(REPO_ROOT.glob("*/pom.xml")) + list(REPO_ROOT.glob("*/*/pom.xml"))
    modules = []
    for pom in poms:
        m = pom.parent
        if m == REPO_ROOT:
            continue
        rel = str(m.relative_to(REPO_ROOT))
        if any(marker in rel for marker in EXCLUDED_MODULE_MARKERS):
            continue
        modules.append(m)
    return sorted(modules)


def collect_jacoco_inputs() -> tuple[list[Path], list[Path], list[Path]]:
    """
    Scan every active module and collect:
      - exec files     (target/jacoco.exec)        -- execution data to merge
      - class dirs      (target/classes)            -- bytecode to analyze
      - source dirs     (src/main/java)             -- for source attribution
    A module contributes its classes/sources whenever it has been compiled, and its
    exec whenever its tests have run.  Coverage of a class is the union of every exec
    that touched it (its own module's unit tests + the integration suite + any other).
    """
    execfiles, classdirs, srcdirs = [], [], []
    for m in discover_modules():
        classes = m / "target" / "classes"
        execf = m / "target" / "jacoco.exec"
        src = m / "src" / "main" / "java"
        if classes.is_dir() and any(classes.rglob("*.class")):
            classdirs.append(classes)
            if src.is_dir():
                srcdirs.append(src)
        if execf.is_file():
            execfiles.append(execf)
    return execfiles, classdirs, srcdirs


def jacoco_version() -> str:
    """Read <jacoco.plugin.version> from the root pom (fallback to a known-good default)."""
    pom = (REPO_ROOT / "pom.xml").read_text(encoding="utf-8")
    m = re.search(r"<jacoco\.plugin\.version>([^<]+)</jacoco\.plugin\.version>", pom)
    return m.group(1).strip() if m else "0.8.14"


def maven_local_repo() -> Path:
    """Best-effort resolution of the Maven local repository."""
    for env in ("MAVEN_REPO", "M2_REPO"):
        v = os.environ.get(env)
        if v and Path(v).is_dir():
            return Path(v)
    return Path.home() / ".m2" / "repository"


def java_binary() -> str:
    """Resolve a Java launcher, honoring JAVA_HOME, then ~/jdk/default, then PATH."""
    jh = os.environ.get("JAVA_HOME")
    if jh and (Path(jh) / "bin" / "java").exists():
        return str(Path(jh) / "bin" / "java")
    default_jdk = Path.home() / "jdk" / "default" / "bin" / "java"
    if default_jdk.exists():
        return str(default_jdk)
    return "java"


def jacoco_cli_jar() -> Path:
    """Locate (resolving via Maven if needed) the JaCoCo CLI 'nodeps' jar."""
    ver = jacoco_version()
    jar = maven_local_repo() / "org" / "jacoco" / "org.jacoco.cli" / ver / f"org.jacoco.cli-{ver}-nodeps.jar"
    if jar.exists():
        return jar
    print(f"Fetching JaCoCo CLI {ver}...")
    result = subprocess.run(
        ["mvn", "-q", "org.apache.maven.plugins:maven-dependency-plugin:3.6.1:get",
         f"-Dartifact=org.jacoco:org.jacoco.cli:{ver}:jar:nodeps"],
        cwd=REPO_ROOT, capture_output=True, text=True
    )
    if result.returncode != 0 or not jar.exists():
        print(result.stderr[-2000:], file=sys.stderr)
        die(f"Could not resolve JaCoCo CLI jar ({jar}).")
    return jar


def run_tests(module: Path):
    """Re-run a single module's tests to refresh its .exec file.

    Scoped to the owning module so single-file/class queries stay fast.  Other
    modules' existing execs are still merged in when the report is generated.
    Requires upstream module artifacts to be available in the local repo (from a
    prior full build, e.g. `mvn install -DskipTests`).
    """
    rel = module.relative_to(REPO_ROOT)
    print(f"Running tests for module {rel} to refresh coverage data...")
    result = subprocess.run(
        ["mvn", "-pl", str(rel), "test", "-Drat.skip=true", "-q"],
        cwd=REPO_ROOT,
        capture_output=True,
        text=True
    )
    if result.returncode != 0:
        print(result.stderr[-3000:], file=sys.stderr)
        die("Tests failed. Fix failures before checking coverage.")
    print("Tests passed.\n")


def generate_combined_report() -> Path:
    """Build the combined, repo-wide JaCoCo XML report from all module execs + classes."""
    execfiles, classdirs, srcdirs = collect_jacoco_inputs()
    if not execfiles:
        die("No jacoco.exec files found in any module. Run with --run, or build the "
            "project first (e.g. `mvn -pl juneau-integration-tests -am test`).")
    if not classdirs:
        die("No compiled classes (target/classes) found. Build the project first.")

    COMBINED_XML.parent.mkdir(parents=True, exist_ok=True)
    cmd = [java_binary(), "-jar", str(jacoco_cli_jar()), "report"]
    cmd += [str(e) for e in execfiles]
    for c in classdirs:
        cmd += ["--classfiles", str(c)]
    for s in srcdirs:
        cmd += ["--sourcefiles", str(s)]
    cmd += ["--xml", str(COMBINED_XML), "--name", "Juneau combined coverage", "--quiet"]

    print(f"Aggregating coverage from {len(execfiles)} module exec file(s)...")
    result = subprocess.run(cmd, cwd=REPO_ROOT, capture_output=True, text=True)
    if result.returncode != 0:
        print(result.stderr[-3000:], file=sys.stderr)
        die("Failed to generate combined JaCoCo report.")
    return COMBINED_XML


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


def report(xml_path: Path, pkg_filter: str, file_filter: str | None, branches_only: bool):  # NOSONAR python:S3776 -- Cognitive complexity is acceptable for XML report parsing and output formatting
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
            "Make sure the module is built and the exec file is up to date (use --run).")

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
                if lmb > 0 or (not branches_only and lmi > 0):
                    lcb = int(line.get("cb", 0))
                    uncovered.append((ln, lmb, lmb + lcb, lmi))

            files_data.append((pkg_name, fname, mb, cb, mi, ci, uncovered))

    if not files_data:
        print("No data found for the specified path.")
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
            print("\n  Uncovered lines:")
            for ln, lmb, ltotal, lmi in sorted(uncovered):
                parts = []
                if lmb > 0:
                    parts.append(f"{lmb}/{ltotal} branches missed")
                if lmi > 0:
                    parts.append(f"{lmi} instructions missed")
                print(f"    line {ln:4d}:  {', '.join(parts)}")
        else:
            print("\n  All lines covered!")

    # Print summary if multiple files
    if len(files_data) > 1:
        branch_total = total_mb + total_cb
        instr_total = total_mi + total_ci
        print(f"\n{'='*70}")
        print("  TOTAL SUMMARY")
        print(f"{'='*70}")
        print(f"  Branches:     {bar(total_cb, branch_total)}  {pct(total_cb, branch_total):>4}  ({total_cb}/{branch_total} covered, {total_mb} missed)")
        print(f"  Instructions: {bar(total_ci, instr_total)}  {pct(total_ci, instr_total):>4}  ({total_ci}/{instr_total} covered, {total_mi} missed)")
        print()


def main():  # NOSONAR: always returns 0 by design — standard POSIX exit code for success
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
        run_tests(module)

    xml_path = generate_combined_report()

    report(xml_path, pkg_filter, file_filter, branches_only)
    return 0


if __name__ == "__main__":
    sys.exit(main())
