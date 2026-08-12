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

Hard-To-Test (HTT) exclusion:
    A source line can be marked as intentionally out of coverage scope by
    adding a "HTT" token to a comment on that line, e.g.:
        // HTT
        x++;  // HTT: cannot reach on this platform
        /* HTT */
    The marker must be the standalone word "HTT" (word-boundaried, so it does
    NOT match "HTTP"/"HTTPS"), and must appear inside a comment -- not inside
    a string literal. Lines marked this way are dropped from the "Uncovered
    lines" list and their missed/covered branch and instruction counts are
    subtracted from the file's (and the total summary's) totals, so they no
    longer affect the reported percentages. Any file with >=1 excluded line
    is flagged "(HTT-adjusted)", and the excluded line numbers are printed
    for transparency along with their reason (the text after "HTT:", or
    "(no reason given)" for a bare marker) -- e.g.:
        Excluded 2 hard-to-test line(s) (HTT):
          L123: cannot reach on this platform
          L145: (no reason given)

Usage:
    ./scripts/coverage.py <path> [options]

Arguments:
    path    A source file (.java) or source folder to report on.
            Paths can be absolute or relative to the repo root.

Options:
    --run, -r         Re-run the owning module's tests before reporting
                      (refreshes that module's .exec; other modules' existing
                      execs are still merged in).
    --branches, -b    Show only lines with missed branches (default: show all uncovered).
    --by-module       Print a per-module HTT-adjusted summary table (module, file
                      count, branch %, missed branches, instruction %, missed
                      instructions, HTT-excluded line count) sorted by branch %
                      ascending, with a TOTAL row -- instead of reporting a single
                      path. Uses whichever module target/jacoco.exec files already
                      exist on disk; run the test suite first (e.g.
                      `mvn clean test -Drat.skip=true`) if none are found.
    --help, -h        Show this help message.

Examples:
    ./scripts/coverage.py juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/conversion/
    ./scripts/coverage.py juneau-core/juneau-marshall/src/main/java/org/apache/juneau/marshall/BitSetFormat.java
    ./scripts/coverage.py juneau-core/juneau-marshall/src/main/java/org/apache/juneau/marshall/BitSetFormat.java --run
    ./scripts/coverage.py --by-module
"""

from __future__ import annotations

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


# Hard-To-Test (HTT) exclusion marker: a standalone "HTT" word in a comment.
# Word-boundaried so it does not match "HTTP"/"HTTPS".
HTT_TOKEN_RE = re.compile(r"\bHTT\b")


def extract_comment_portion(line: str) -> str | None:
    """
    Best-effort isolation of the comment portion of a single physical source line,
    for HTT marker detection.

    Heuristic only, and intentionally simple: returns the text after the first
    '//' on the line, or (if no '//') the text inside the first '/* ... */' on
    the line. Limitations: it does not track block-comment state across lines
    (a "HTT" token on its own line inside a multi-line /* ... */ block, with no
    '//' or '/*' on that same physical line, will NOT be detected), and it does
    not know about string/char literals, so a '//' or '/*' occurring inside a
    string literal earlier on the line would be mistaken for the start of a
    comment. This is acceptable for the intended use (short, single-line HTT
    annotations) but is not a real Java lexer.
    """
    slash_slash = line.find("//")
    if slash_slash != -1:
        return line[slash_slash + 2:]
    block_start = line.find("/*")
    if block_start != -1:
        block_end = line.find("*/", block_start + 2)
        end = block_end if block_end != -1 else len(line)
        return line[block_start + 2:end]
    return None


def htt_marker_reason(line: str) -> str | None:
    """
    If the line's comment portion (see extract_comment_portion) carries a standalone HTT
    marker, return its reason: the trimmed text following "HTT:" (e.g. "// HTT: cannot
    reach on this platform" -> "cannot reach on this platform"), or the literal string
    "(no reason given)" for a bare marker ("// HTT", "/* HTT */") or one with an empty
    reason after the colon. Returns None if the line has no HTT marker at all.
    """
    comment = extract_comment_portion(line)
    if comment is None:
        return None
    m = HTT_TOKEN_RE.search(comment)
    if m is None:
        return None
    remainder = comment[m.end():].lstrip()
    if remainder.startswith(":"):
        reason = remainder[1:].strip()
        if reason:
            return reason
    return "(no reason given)"


def is_htt_marked(line: str) -> bool:
    """True if the line's comment portion (see extract_comment_portion) contains a standalone HTT token."""
    return htt_marker_reason(line) is not None


def find_source_file(pkg_name: str, fname: str, srcdirs: list[Path]) -> Path | None:
    """Locate the source file for a JaCoCo package+filename among the given src/main/java dirs."""
    for src in srcdirs:
        candidate = src / pkg_name / fname
        if candidate.is_file():
            return candidate
    return None


def find_htt_lines(pkg_name: str, fname: str, srcdirs: list[Path]) -> dict[int, str]:
    """Return {1-based physical line number: reason} for every HTT-marked line in the owning source file."""
    src_file = find_source_file(pkg_name, fname, srcdirs)
    if src_file is None:
        return {}
    try:
        lines = src_file.read_text(encoding="utf-8").splitlines()
    except OSError:
        return {}
    result = {}
    for i, text in enumerate(lines):
        reason = htt_marker_reason(text)
        if reason is not None:
            result[i + 1] = reason
    return result


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


def report(xml_path: Path, pkg_filter: str, file_filter: str | None, branches_only: bool, srcdirs: list[Path]):  # NOSONAR python:S3776 -- Cognitive complexity is acceptable for XML report parsing and output formatting
    """Parse jacoco.xml and print coverage for the matching package/file, excluding HTT-marked lines (see find_htt_lines)."""
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
    files_data = []  # list of (pkg_name, fname, mb, cb, mi, ci, uncovered, excluded_lines)
    total_mb = total_cb = total_mi = total_ci = total_excluded = 0

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

            # Hard-To-Test (HTT) exclusion: lines marked "HTT" in a comment in the
            # owning source file are dropped from "uncovered", and their per-line
            # counter contributions are subtracted from this file's mb/cb/mi/ci
            # totals so the reported percentages no longer reflect them.
            htt_lines = find_htt_lines(pkg_name, fname, srcdirs)

            uncovered = []
            excluded_lines = []
            for line in sf.findall("line"):
                ln = int(line.get("nr", 0))
                lmb = int(line.get("mb", 0))
                lcb = int(line.get("cb", 0))
                lmi = int(line.get("mi", 0))
                lci = int(line.get("ci", 0))
                if ln in htt_lines and (lmb or lcb or lmi or lci):
                    mb -= lmb
                    cb -= lcb
                    mi -= lmi
                    ci -= lci
                    excluded_lines.append((ln, htt_lines[ln]))
                    continue
                if lmb > 0 or (not branches_only and lmi > 0):
                    uncovered.append((ln, lmb, lmb + lcb, lmi))

            total_mb += mb
            total_cb += cb
            total_mi += mi
            total_ci += ci
            total_excluded += len(excluded_lines)

            files_data.append((pkg_name, fname, mb, cb, mi, ci, uncovered, excluded_lines))

    if not files_data:
        print("No data found for the specified path.")
        return

    # Print per-file results
    for pkg_name, fname, mb, cb, mi, ci, uncovered, excluded_lines in sorted(files_data):
        branch_total = mb + cb
        instr_total = mi + ci
        branch_pct = pct(cb, branch_total)
        instr_pct = pct(ci, instr_total)
        htt_note = "  (HTT-adjusted)" if excluded_lines else ""
        print(f"\n{'='*70}")
        print(f"  {pkg_name.replace('/', '.')}.{fname.removesuffix('.java')}")
        print(f"{'='*70}")
        print(f"  Branches:     {bar(cb, branch_total)}  {branch_pct:>4}  ({cb}/{branch_total} covered, {mb} missed){htt_note}")
        print(f"  Instructions: {bar(ci, instr_total)}  {instr_pct:>4}  ({ci}/{instr_total} covered, {mi} missed){htt_note}")
        if excluded_lines:
            print(f"  Excluded {len(excluded_lines)} hard-to-test line(s) (HTT):")
            for ln, reason in sorted(excluded_lines):
                print(f"    L{ln}: {reason}")
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
        total_htt_note = "  (HTT-adjusted)" if total_excluded else ""
        print(f"\n{'='*70}")
        print("  TOTAL SUMMARY")
        print(f"{'='*70}")
        print(f"  Branches:     {bar(total_cb, branch_total)}  {pct(total_cb, branch_total):>4}  ({total_cb}/{branch_total} covered, {total_mb} missed){total_htt_note}")
        print(f"  Instructions: {bar(total_ci, instr_total)}  {pct(total_ci, instr_total):>4}  ({total_ci}/{instr_total} covered, {total_mi} missed){total_htt_note}")
        if total_excluded:
            files_with_excl = sum(1 for f in files_data if f[7])
            print(f"  Excluded {total_excluded} hard-to-test line(s) (HTT) across {files_with_excl} file(s)")
        print()


def index_module_sources(modules: list[Path]) -> dict[str, Path]:
    """
    Build a {"pkg/name/File.java": owning module Path} index across every given module's
    src/main/java tree, used by by_module_report to attribute a JaCoCo package+sourcefile
    entry (which carries no module identity of its own) back to its owning module.
    """
    index: dict[str, Path] = {}
    for m in modules:
        src = m / "src" / "main" / "java"
        if not src.is_dir():
            continue
        for f in src.rglob("*.java"):
            index[f.relative_to(src).as_posix()] = m
    return index


def by_module_report(xml_path: Path):  # NOSONAR python:S3776 -- Cognitive complexity is acceptable for XML report parsing and output formatting
    """
    Parse xml_path and print one HTT-adjusted coverage row per reactor module (module, file
    count, branch %, missed branches, instruction %, missed instructions, HTT-excluded line
    count), sorted by branch % ascending, followed by a TOTAL row. Reuses the same per-line
    HTT exclusion (find_htt_lines) as the single-path report().
    """
    if not xml_path.exists():
        die(f"JaCoCo report not found at {xml_path}. Run with --run to generate it.")

    modules = discover_modules()
    module_srcdirs = {m: m / "src" / "main" / "java" for m in modules if (m / "src" / "main" / "java").is_dir()}
    owner_index = index_module_sources(list(module_srcdirs.keys()))

    tree = ET.parse(xml_path)
    root = tree.getroot()

    # module -> [file count, mb, cb, mi, ci, htt-excluded line count]
    stats: dict[Path, list[int]] = {}

    for pkg in root.findall("package"):
        pkg_name = pkg.get("name", "")
        for sf in pkg.findall("sourcefile"):
            fname = sf.get("name", "")
            owner = owner_index.get(f"{pkg_name}/{fname}")
            if owner is None:
                continue  # not attributable to any in-scope module (e.g. excluded module)

            mb = cb = mi = ci = 0
            for ctr in sf.findall("counter"):
                t = ctr.get("type")
                m = int(ctr.get("missed", 0))
                c = int(ctr.get("covered", 0))
                if t == "BRANCH":
                    mb, cb = m, c
                elif t == "INSTRUCTION":
                    mi, ci = m, c

            htt_lines = find_htt_lines(pkg_name, fname, [module_srcdirs[owner]])
            excluded = 0
            for line in sf.findall("line"):
                lmb = int(line.get("mb", 0))
                lcb = int(line.get("cb", 0))
                lmi = int(line.get("mi", 0))
                lci = int(line.get("ci", 0))
                if int(line.get("nr", 0)) in htt_lines and (lmb or lcb or lmi or lci):
                    mb -= lmb
                    cb -= lcb
                    mi -= lmi
                    ci -= lci
                    excluded += 1

            row = stats.setdefault(owner, [0, 0, 0, 0, 0, 0])
            row[0] += 1
            row[1] += mb
            row[2] += cb
            row[3] += mi
            row[4] += ci
            row[5] += excluded

    if not stats:
        print("No JaCoCo data found for any module.")
        return

    def branch_frac(row):
        total = row[1] + row[2]
        return row[2] / total if total else 1.0

    def instr_frac(row):
        total = row[3] + row[4]
        return row[4] / total if total else 1.0

    rows = sorted(
        stats.items(),
        key=lambda kv: (branch_frac(kv[1]), instr_frac(kv[1]), str(kv[0].relative_to(REPO_ROOT))),
    )

    names = [str(m.relative_to(REPO_ROOT)) for m, _ in rows] + ["TOTAL"]
    name_w = max(len(n) for n in names)

    header = f"{'Module':<{name_w}}  {'Files':>6}  {'Branch%':>7}  {'BrMiss':>7}  {'Instr%':>7}  {'InstrMiss':>9}  {'HTT':>5}"
    print(f"\nPer-module coverage (HTT-adjusted, sorted by branch % ascending):\n")
    print(header)
    print("-" * len(header))

    total_files = total_mb = total_cb = total_mi = total_ci = total_htt = 0
    for module, (files, mb, cb, mi, ci, htt) in rows:
        name = str(module.relative_to(REPO_ROOT))
        print(f"{name:<{name_w}}  {files:>6}  {pct(cb, mb + cb):>7}  {mb:>7}  {pct(ci, mi + ci):>7}  {mi:>9}  {htt:>5}")
        total_files += files
        total_mb += mb
        total_cb += cb
        total_mi += mi
        total_ci += ci
        total_htt += htt

    print("-" * len(header))
    print(f"{'TOTAL':<{name_w}}  {total_files:>6}  {pct(total_cb, total_mb + total_cb):>7}  {total_mb:>7}  "
          f"{pct(total_ci, total_mi + total_ci):>7}  {total_mi:>9}  {total_htt:>5}")
    print()


def main():  # NOSONAR: always returns 0 by design — standard POSIX exit code for success
    args = sys.argv[1:]
    if not args or "--help" in args or "-h" in args:
        print(__doc__)
        return 0

    path_arg = None
    do_run = False
    branches_only = False
    by_module = False

    for arg in args:
        if arg in ("--run", "-r"):
            do_run = True
        elif arg in ("--branches", "-b"):
            branches_only = True
        elif arg == "--by-module":
            by_module = True
        elif arg.startswith("-"):
            die(f"Unknown option: {arg}")
        else:
            path_arg = arg

    if by_module:
        if do_run:
            print("Note: --run has no effect with --by-module (it only refreshes a single module's "
                  "tests). Run the full reactor test suite yourself first if needed, e.g. "
                  "`mvn clean test -Drat.skip=true`.")
        xml_path = generate_combined_report()
        by_module_report(xml_path)
        return 0

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

    _, _, srcdirs = collect_jacoco_inputs()
    report(xml_path, pkg_filter, file_filter, branches_only, srcdirs)
    return 0


if __name__ == "__main__":
    sys.exit(main())
