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
Coverage reporter for the Juneau Release Manager (ported from Juneau's scripts/coverage.py).

Reads target/jacoco.exec (written by the JaCoCo agent wired into pom.xml during
`mvn test`) and target/classes, runs the JaCoCo CLI to produce target/coverage/jacoco.xml,
and prints HTT-adjusted branch/instruction coverage.

The Release Manager is a single Maven module, so "module" here means a top-level package
under org.apache.juneau.releng (sub-packages such as engine/steps roll up into their
top-level package; classes directly in org.apache.juneau.releng are reported as "(root)").

Hard-To-Test (HTT) exclusion:
    A source line can be marked as intentionally out of coverage scope by adding a
    standalone "HTT" token to a comment on that line, e.g.:
        // HTT: cannot reach on this platform
        /* HTT */
    The marker is word-boundaried (does NOT match "HTTP"/"HTTPS") and must be inside a
    comment, not a string literal. Marked lines are dropped from the uncovered list and
    their missed/covered branch and instruction counts are subtracted from the totals.
    Excluded lines are printed with their reason (text after "HTT:").

Usage:
    ./scripts/coverage.py --by-module        per-package summary table, sorted by branch % ascending
    ./scripts/coverage.py <package>          per-file detail + uncovered lines for a top-level
                                             package (e.g. engine) or a source path/file
    Options:
      --branches, -b   show only lines with missed branches
      --run, -r        run `mvn test -Drat.skip=true` first to refresh target/jacoco.exec
      --help, -h       show this help

Run `mvn clean test -Drat.skip=true` first if target/jacoco.exec does not exist.
"""

from __future__ import annotations

import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
EXEC_FILE = REPO_ROOT / "target" / "jacoco.exec"
CLASSES_DIR = REPO_ROOT / "target" / "classes"
SRC_DIR = REPO_ROOT / "src" / "main" / "java"
COMBINED_XML = REPO_ROOT / "target" / "coverage" / "jacoco.xml"
BASE_PKG = "org/apache/juneau/releng"


def die(msg):
    print(f"ERROR: {msg}", file=sys.stderr)
    sys.exit(1)


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
    """Read <jacoco.plugin.version> from pom.xml (fallback to a known-good default)."""
    pom = (REPO_ROOT / "pom.xml").read_text(encoding="utf-8")
    m = re.search(r"<jacoco\.plugin\.version>([^<]+)</jacoco\.plugin\.version>", pom)
    return m.group(1).strip() if m else "0.8.15"


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


def run_tests():
    """Run the unit tests to refresh target/jacoco.exec."""
    print("Running tests to refresh coverage data...")
    result = subprocess.run(["mvn", "test", "-Drat.skip=true", "-q"], cwd=REPO_ROOT,
                            capture_output=True, text=True)
    if result.returncode != 0:
        print(result.stdout[-3000:], file=sys.stderr)
        die("Tests failed. Fix failures before checking coverage.")
    print("Tests passed.\n")


def generate_report() -> Path:
    """Build the JaCoCo XML report from target/jacoco.exec + target/classes."""
    if not EXEC_FILE.is_file():
        die("target/jacoco.exec not found. Run `mvn clean test -Drat.skip=true` or pass --run.")
    if not CLASSES_DIR.is_dir():
        die("No compiled classes (target/classes) found. Build the project first.")
    COMBINED_XML.parent.mkdir(parents=True, exist_ok=True)
    cmd = [java_binary(), "-jar", str(jacoco_cli_jar()), "report", str(EXEC_FILE),
           "--classfiles", str(CLASSES_DIR), "--sourcefiles", str(SRC_DIR),
           "--xml", str(COMBINED_XML), "--name", "Release Manager coverage", "--quiet"]
    result = subprocess.run(cmd, cwd=REPO_ROOT, capture_output=True, text=True)
    if result.returncode != 0:
        print(result.stderr[-3000:], file=sys.stderr)
        die("Failed to generate JaCoCo report.")
    return COMBINED_XML


def bar(covered, total, width=20):
    """Render a simple ASCII progress bar."""
    filled = width if total == 0 else round(covered / total * width)
    return "[" + "#" * filled + "." * (width - filled) + "]"


def pct(covered, total):
    if total == 0:
        return "100%"
    return f"{covered / total * 100:.0f}%"


def top_package(pkg_name: str) -> str:
    """Map a JaCoCo package (slash form) to its top-level package under org.apache.juneau.releng."""
    if pkg_name == BASE_PKG:
        return "(root)"
    if pkg_name.startswith(BASE_PKG + "/"):
        return pkg_name[len(BASE_PKG) + 1:].split("/")[0]
    return pkg_name


def file_stats(sf, pkg_name: str, fname: str, branches_only: bool = False):
    """Return (mb, cb, mi, ci, uncovered[(line, mb, total, mi)], excluded[(line, reason)]) for one sourcefile, HTT-adjusted."""
    mb = cb = mi = ci = 0
    for ctr in sf.findall("counter"):
        t = ctr.get("type")
        m = int(ctr.get("missed", 0))
        c = int(ctr.get("covered", 0))
        if t == "BRANCH":
            mb, cb = m, c
        elif t == "INSTRUCTION":
            mi, ci = m, c
    htt_lines = find_htt_lines(pkg_name, fname, [SRC_DIR])
    uncovered, excluded = [], []
    for line in sf.findall("line"):
        ln = int(line.get("nr", 0))
        lmb, lcb = int(line.get("mb", 0)), int(line.get("cb", 0))
        lmi, lci = int(line.get("mi", 0)), int(line.get("ci", 0))
        if ln in htt_lines and (lmb or lcb or lmi or lci):
            mb -= lmb
            cb -= lcb
            mi -= lmi
            ci -= lci
            excluded.append((ln, htt_lines[ln]))
            continue
        if lmb > 0 or (not branches_only and lmi > 0):
            uncovered.append((ln, lmb, lmb + lcb, lmi))
    return mb, cb, mi, ci, uncovered, excluded


def by_module_report(xml_path: Path):
    """Print one HTT-adjusted row per top-level package, sorted by branch % ascending, plus a TOTAL row."""
    root = ET.parse(xml_path).getroot()
    stats: dict[str, list[int]] = {}
    for pkg in root.findall("package"):
        pkg_name = pkg.get("name", "")
        for sf in pkg.findall("sourcefile"):
            mb, cb, mi, ci, uncovered, excluded = file_stats(sf, pkg_name, sf.get("name", ""))
            row = stats.setdefault(top_package(pkg_name), [0, 0, 0, 0, 0, 0, 0])
            row[0] += 1
            row[1] += mb
            row[2] += cb
            row[3] += mi
            row[4] += ci
            row[5] += len(excluded)
            row[6] += len(uncovered)
    if not stats:
        print("No JaCoCo data found.")
        return

    def key(kv):
        r = kv[1]
        bt, it = r[1] + r[2], r[3] + r[4]
        return (r[2] / bt if bt else 1.0, r[4] / it if it else 1.0, kv[0])

    rows = sorted(stats.items(), key=key)
    w = max(len(n) for n, _ in rows + [("TOTAL", None)])
    header = f"{'Package':<{w}}  {'Files':>5}  {'Branch%':>7}  {'BrMiss':>6}  {'Instr%':>6}  {'InstrMiss':>9}  {'UncovLines':>10}  {'HTT':>4}"
    print("\nPer-package coverage (HTT-adjusted, sorted by branch % ascending):\n")
    print(header)
    print("-" * len(header))
    tot = [0] * 7
    for name, r in rows:
        print(f"{name:<{w}}  {r[0]:>5}  {pct(r[2], r[1] + r[2]):>7}  {r[1]:>6}  {pct(r[4], r[3] + r[4]):>6}  {r[3]:>9}  {r[6]:>10}  {r[5]:>4}")
        tot = [a + b for a, b in zip(tot, r)]
    print("-" * len(header))
    print(f"{'TOTAL':<{w}}  {tot[0]:>5}  {pct(tot[2], tot[1] + tot[2]):>7}  {tot[1]:>6}  {pct(tot[4], tot[3] + tot[4]):>6}  {tot[3]:>9}  {tot[6]:>10}  {tot[5]:>4}")
    print()


def parse_detail_target(target: str) -> tuple[str, str | None, str | None]:
    """Split a detail-report target into (normalized target, package filter, file filter)."""
    t = target.replace("\\", "/")
    marker = "src/main/java/"
    if marker not in t:
        return t, None, None
    rest = t.split(marker, 1)[1].rstrip("/")
    if rest.endswith(".java"):
        pkg_filter, file_filter = rest.rsplit("/", 1)
        return t, pkg_filter, file_filter
    return t, rest, None


def package_matches(pkg_name: str, t: str, pkg_filter: str | None) -> bool:
    """True if a JaCoCo package belongs in the detail report for the given filters."""
    if pkg_filter is not None:
        return pkg_name == pkg_filter or pkg_name.startswith(pkg_filter + "/")
    return top_package(pkg_name) == t


def print_file_detail(pkg_name: str, fname: str, stats) -> None:
    """Print the coverage block for one source file."""
    mb, cb, mi, ci, uncovered, excluded = stats
    note = "  (HTT-adjusted)" if excluded else ""
    print(f"\n{'=' * 70}\n  {pkg_name.replace('/', '.')}.{fname.removesuffix('.java')}\n{'=' * 70}")
    print(f"  Branches:     {bar(cb, mb + cb)}  {pct(cb, mb + cb):>4}  ({cb}/{mb + cb} covered, {mb} missed){note}")
    print(f"  Instructions: {bar(ci, mi + ci)}  {pct(ci, mi + ci):>4}  ({ci}/{mi + ci} covered, {mi} missed){note}")
    if excluded:
        print(f"  Excluded {len(excluded)} hard-to-test line(s) (HTT):")
        for ln, reason in sorted(excluded):
            print(f"    L{ln}: {reason}")
    if not uncovered:
        print("\n  All lines covered!")
        return
    print("\n  Uncovered lines:")
    for ln, lmb, total, lmi in sorted(uncovered):
        parts = []
        if lmb > 0:
            parts.append(f"{lmb}/{total} branches missed")
        if lmi > 0:
            parts.append(f"{lmi} instructions missed")
        print(f"    line {ln:4d}:  {', '.join(parts)}")


def detail_report(xml_path: Path, target: str, branches_only: bool):
    """Print per-file coverage and uncovered lines for a top-level package name or a source path."""
    t, pkg_filter, file_filter = parse_detail_target(target)
    root = ET.parse(xml_path).getroot()
    shown = 0
    for pkg in sorted(root.findall("package"), key=lambda p: p.get("name", "")):
        pkg_name = pkg.get("name", "")
        if not package_matches(pkg_name, t, pkg_filter):
            continue
        for sf in sorted(pkg.findall("sourcefile"), key=lambda s: s.get("name", "")):
            fname = sf.get("name", "")
            if file_filter and fname != file_filter:
                continue
            print_file_detail(pkg_name, fname, file_stats(sf, pkg_name, fname, branches_only))
            shown += 1
    if shown == 0:
        die(f"No JaCoCo data found for '{target}'.")


def main():
    args = sys.argv[1:]
    if not args or "--help" in args or "-h" in args:
        print(__doc__)
        return
    target, do_run, branches_only, by_module = None, False, False, False
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
            target = arg
    if do_run:
        run_tests()
    xml_path = generate_report()
    if by_module:
        by_module_report(xml_path)
    elif target:
        detail_report(xml_path, target, branches_only)
    else:
        die("No package or path specified (or use --by-module).")


if __name__ == "__main__":
    main()
