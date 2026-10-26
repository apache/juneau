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
Coverage reporter for a single-module Maven project that runs the JaCoCo agent during `mvn test`.

Reads target/jacoco.exec and target/classes, runs the JaCoCo CLI to produce target/coverage/jacoco.xml, and prints
HTT-adjusted branch and instruction coverage.  A repository's scripts/coverage.py builds a `Config` and calls `main`:

    from juneau_terminal_scripts import coverage
    coverage.main(coverage.Config(repo_root=ROOT, base_pkg="org/example/app", report_name="App coverage",
                                  title="the App", scope="..."))

What differs between repositories is data on the Config (base package, display names, extra Maven arguments, the
`--depth` option) or a hook (`modes`: an option that replaces the JaCoCo report with the repository's own report).
"""

from __future__ import annotations

import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from collections.abc import Iterator
from dataclasses import dataclass, field, replace
from pathlib import Path

__all__ = ["Config", "Options", "main", "die", "pct", "bar"]


@dataclass
class Config:
    """One repository's coverage settings.

    repo_root     the Maven project directory (holds pom.xml, target/ and src/main/java).
    base_pkg      the package the by-module report rolls up under, in JaCoCo's slash form.
    report_name   the --name passed to the JaCoCo CLI.
    title         the project's name in the help text ("Coverage reporter for <title>").
    scope         the help paragraph describing what "module" means for this project.
    package_example  a sample package name for the usage text.
    mvn_args      extra arguments for `mvn test` (for --run).
    depth         package depth for the by-module report.
    depth_option  accept `--depth N` on the command line.
    extra_usage   extra usage lines (printed under Usage:), for modes and options this repository adds.
    extra_doc     an extra help paragraph, printed before the HTT section.
    modes         {option: hook(config)}; an option here runs the hook instead of the JaCoCo report.
    """
    repo_root: Path
    base_pkg: str
    report_name: str
    title: str
    scope: str
    package_example: str = "engine"
    mvn_args: tuple = ()
    depth: int = 1
    depth_option: bool = False
    extra_usage: tuple = ()
    extra_doc: str = ""
    modes: dict = field(default_factory=dict)

    @property
    def exec_file(self) -> Path:
        return self.repo_root / "target" / "jacoco.exec"

    @property
    def classes_dir(self) -> Path:
        return self.repo_root / "target" / "classes"

    @property
    def src_dir(self) -> Path:
        return self.repo_root / "src" / "main" / "java"

    @property
    def xml_file(self) -> Path:
        return self.repo_root / "target" / "coverage" / "jacoco.xml"


@dataclass
class Options:
    """The parsed command line."""
    target: str | None = None
    do_run: bool = False
    branches_only: bool = False
    by_module: bool = False
    depth: int | None = None
    mode: str | None = None


def die(msg):
    """Print an error to stderr and exit 1."""
    print(f"ERROR: {msg}", file=sys.stderr)
    sys.exit(1)


# Hard-To-Test (HTT) exclusion marker: a standalone "HTT" word in a comment.  Word-boundaried so it does not match
# "HTTP"/"HTTPS".
HTT_TOKEN_RE = re.compile(r"\bHTT\b")


def extract_comment_portion(line: str) -> str | None:
    """
    Best-effort isolation of the comment portion of a single physical source line, for HTT marker detection.

    Heuristic only, and intentionally simple: returns the text after the first '//' on the line, or (if no '//')
    the text inside the first '/* ... */' on the line.  It does not track block-comment state across lines and does
    not know about string/char literals.  That is acceptable for short, single-line HTT annotations; it is not a
    real Java lexer.
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
    If the line's comment portion carries a standalone HTT marker, return its reason: the trimmed text following
    "HTT:", or "(no reason given)" for a bare marker or an empty reason.  None if the line has no HTT marker.
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


def find_source_file(pkg_name: str, fname: str, srcdirs: list[Path]) -> Path | None:
    """Locate the source file for a JaCoCo package+filename among the given source directories."""
    for src in srcdirs:
        candidate = src / pkg_name / fname
        if candidate.is_file():
            return candidate
    return None


def find_htt_lines(pkg_name: str, fname: str, srcdirs: list[Path]) -> dict[int, str]:
    """Return {1-based line number: reason} for every HTT-marked line in the owning source file."""
    src_file = find_source_file(pkg_name, fname, srcdirs)
    if src_file is None:
        return {}
    try:
        lines = src_file.read_text(encoding="utf-8").splitlines()
    except OSError:
        return {}
    marked = ((i + 1, htt_marker_reason(text)) for i, text in enumerate(lines))
    return {n: reason for n, reason in marked if reason is not None}


def jacoco_version(cfg: Config) -> str:
    """Read <jacoco.plugin.version> from pom.xml (fallback to a known-good default)."""
    pom = (cfg.repo_root / "pom.xml").read_text(encoding="utf-8")
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


def jacoco_cli_jar(cfg: Config) -> Path:
    """Locate (resolving via Maven if needed) the JaCoCo CLI 'nodeps' jar."""
    ver = jacoco_version(cfg)
    jar = maven_local_repo() / "org" / "jacoco" / "org.jacoco.cli" / ver / f"org.jacoco.cli-{ver}-nodeps.jar"
    if jar.exists():
        return jar
    print(f"Fetching JaCoCo CLI {ver}...")
    result = subprocess.run(
        ["mvn", "-q", "org.apache.maven.plugins:maven-dependency-plugin:3.6.1:get",
         f"-Dartifact=org.jacoco:org.jacoco.cli:{ver}:jar:nodeps"],
        cwd=cfg.repo_root, capture_output=True, text=True
    )
    if result.returncode != 0 or not jar.exists():
        print(result.stderr[-2000:], file=sys.stderr)
        die(f"Could not resolve JaCoCo CLI jar ({jar}).")
    return jar


def mvn_test_command(cfg: Config) -> str:
    """The Maven command line the help text and error messages name."""
    return " ".join(["mvn", "test", *cfg.mvn_args])


def run_tests(cfg: Config):
    """Run the unit tests to refresh target/jacoco.exec."""
    print("Running tests to refresh coverage data...")
    result = subprocess.run(["mvn", "test", *cfg.mvn_args, "-q"], cwd=cfg.repo_root,
                            capture_output=True, text=True)
    if result.returncode != 0:
        print(result.stdout[-3000:], file=sys.stderr)
        die("Tests failed. Fix failures before checking coverage.")
    print("Tests passed.\n")


def generate_report(cfg: Config) -> Path:
    """Build the JaCoCo XML report from target/jacoco.exec + target/classes."""
    if not cfg.exec_file.is_file():
        die(f"target/jacoco.exec not found. Run `mvn clean test{_args_suffix(cfg)}` or pass --run.")
    if not cfg.classes_dir.is_dir():
        die("No compiled classes (target/classes) found. Build the project first.")
    cfg.xml_file.parent.mkdir(parents=True, exist_ok=True)
    cmd = [java_binary(), "-jar", str(jacoco_cli_jar(cfg)), "report", str(cfg.exec_file),
           "--classfiles", str(cfg.classes_dir), "--sourcefiles", str(cfg.src_dir),
           "--xml", str(cfg.xml_file), "--name", cfg.report_name, "--quiet"]
    result = subprocess.run(cmd, cwd=cfg.repo_root, capture_output=True, text=True)
    if result.returncode != 0:
        print(result.stderr[-3000:], file=sys.stderr)
        die("Failed to generate JaCoCo report.")
    return cfg.xml_file


def _args_suffix(cfg: Config) -> str:
    return "".join(" " + a for a in cfg.mvn_args)


def bar(covered, total, width=20):
    """Render a simple ASCII progress bar."""
    filled = width if total == 0 else round(covered / total * width)
    return "[" + "#" * filled + "." * (width - filled) + "]"


def pct(covered, total):
    """A whole-number percentage string; 100% for an empty total."""
    if total == 0:
        return "100%"
    return f"{covered / total * 100:.0f}%"


def top_package(cfg: Config, pkg_name: str) -> str:
    """Map a JaCoCo package (slash form) to its package at cfg.depth levels under cfg.base_pkg."""
    if pkg_name == cfg.base_pkg:
        return "(root)"
    if pkg_name.startswith(cfg.base_pkg + "/"):
        return "/".join(pkg_name[len(cfg.base_pkg) + 1:].split("/")[:cfg.depth])
    return pkg_name


def _file_totals(sf):
    """(missed branches, covered branches, missed instructions, covered instructions) from a sourcefile's counters."""
    mb = cb = mi = ci = 0
    for ctr in sf.findall("counter"):
        t = ctr.get("type")
        m = int(ctr.get("missed", 0))
        c = int(ctr.get("covered", 0))
        if t == "BRANCH":
            mb, cb = m, c
        elif t == "INSTRUCTION":
            mi, ci = m, c
    return mb, cb, mi, ci


def _line_counts(line):
    return (int(line.get("mb", 0)), int(line.get("cb", 0)), int(line.get("mi", 0)), int(line.get("ci", 0)))


def file_stats(cfg: Config, sf, pkg_name: str, fname: str, branches_only: bool = False):
    """Return (mb, cb, mi, ci, uncovered[(line, mb, total, mi)], excluded[(line, reason)]) for one sourcefile, HTT-adjusted."""
    mb, cb, mi, ci = _file_totals(sf)
    htt_lines = find_htt_lines(pkg_name, fname, [cfg.src_dir])
    uncovered, excluded = [], []
    for line in sf.findall("line"):
        ln = int(line.get("nr", 0))
        lmb, lcb, lmi, lci = _line_counts(line)
        if ln in htt_lines and (lmb or lcb or lmi or lci):
            mb, cb, mi, ci = mb - lmb, cb - lcb, mi - lmi, ci - lci
            excluded.append((ln, htt_lines[ln]))
        elif lmb > 0 or (not branches_only and lmi > 0):
            uncovered.append((ln, lmb, lmb + lcb, lmi))
    return mb, cb, mi, ci, uncovered, excluded


def _module_rows(cfg: Config, root) -> dict[str, list[int]]:
    """{package: [files, mb, cb, mi, ci, excluded lines, uncovered lines]} summed over the report."""
    stats: dict[str, list[int]] = {}
    for pkg in root.findall("package"):
        pkg_name = pkg.get("name", "")
        for sf in pkg.findall("sourcefile"):
            mb, cb, mi, ci, uncovered, excluded = file_stats(cfg, sf, pkg_name, sf.get("name", ""))
            row = stats.setdefault(top_package(cfg, pkg_name), [0] * 7)
            for i, v in enumerate((1, mb, cb, mi, ci, len(excluded), len(uncovered))):
                row[i] += v
    return stats


def _module_sort_key(kv):
    r = kv[1]
    bt, it = r[1] + r[2], r[3] + r[4]
    return (r[2] / bt if bt else 1.0, r[4] / it if it else 1.0, kv[0])


def by_module_report(cfg: Config, xml_path: Path):
    """Print one HTT-adjusted row per package, sorted by branch % ascending, plus a TOTAL row."""
    stats = _module_rows(cfg, ET.parse(xml_path).getroot())
    if not stats:
        print("No JaCoCo data found.")
        return
    rows = sorted(stats.items(), key=_module_sort_key)
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


def package_matches(cfg: Config, pkg_name: str, t: str, pkg_filter: str | None) -> bool:
    """True if a JaCoCo package belongs in the detail report for the given filters."""
    if pkg_filter is not None:
        return pkg_name == pkg_filter or pkg_name.startswith(pkg_filter + "/")
    return top_package(cfg, pkg_name) == t


def print_uncovered(uncovered):
    """Print the per-line uncovered branch/instruction summary, or the all-covered note."""
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
    print_uncovered(uncovered)


def detail_report(cfg: Config, xml_path: Path, target: str, branches_only: bool):
    """Print per-file coverage and uncovered lines for a package name or a source path."""
    t, pkg_filter, file_filter = parse_detail_target(target)
    root = ET.parse(xml_path).getroot()
    shown = 0
    for pkg in sorted(root.findall("package"), key=lambda p: p.get("name", "")):
        pkg_name = pkg.get("name", "")
        if not package_matches(cfg, pkg_name, t, pkg_filter):
            continue
        for sf in sorted(pkg.findall("sourcefile"), key=lambda s: s.get("name", "")):
            fname = sf.get("name", "")
            if file_filter and fname != file_filter:
                continue
            print_file_detail(pkg_name, fname, file_stats(cfg, sf, pkg_name, fname, branches_only))
            shown += 1
    if shown == 0:
        die(f"No JaCoCo data found for '{target}'.")


def help_text(cfg: Config) -> str:
    """The --help text for this configuration."""
    suffix = _args_suffix(cfg)
    depth_line = "      --depth N        package depth for --by-module (default 1)\n" if cfg.depth_option else ""
    extra_usage = "".join(f"    {u}\n" for u in cfg.extra_usage)
    extra_doc = f"{cfg.extra_doc.rstrip()}\n\n" if cfg.extra_doc else ""
    return f"""
Coverage reporter for {cfg.title} (ported from Juneau's scripts/coverage.py).

Reads target/jacoco.exec (written by the JaCoCo agent wired into pom.xml during
`mvn test`) and target/classes, runs the JaCoCo CLI to produce target/coverage/jacoco.xml,
and prints HTT-adjusted branch/instruction coverage.

{cfg.scope.strip()}

{extra_doc}Hard-To-Test (HTT) exclusion:
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
                                             package (e.g. {cfg.package_example}) or a source path/file
{extra_usage}    Options:
      --branches, -b   show only lines with missed branches
{depth_line}      --run, -r        run `mvn test{suffix}` first to refresh target/jacoco.exec
      --help, -h       show this help

Run `mvn clean test{suffix}` first if target/jacoco.exec does not exist.
"""


_FLAGS = {"--run": "do_run", "-r": "do_run", "--branches": "branches_only", "-b": "branches_only",
          "--by-module": "by_module"}


def _apply_arg(cfg: Config, opts: Options, arg: str, it: Iterator[str]):
    """Fold one command-line argument into opts; an unknown option is fatal."""
    if arg in cfg.modes:
        opts.mode = arg
    elif arg == "--depth" and cfg.depth_option:
        opts.depth = int(next(it, "1"))
    elif arg in _FLAGS:
        setattr(opts, _FLAGS[arg], True)
    elif arg.startswith("-"):
        die(f"Unknown option: {arg}")
    else:
        opts.target = arg


def parse_args(cfg: Config, args) -> Options:
    """Parse the command line into Options."""
    opts = Options()
    it = iter(args)
    for arg in it:
        _apply_arg(cfg, opts, arg, it)
    return opts


def main(cfg: Config, argv=None):
    """Run the coverage command line for cfg; argv defaults to sys.argv[1:]."""
    args = sys.argv[1:] if argv is None else list(argv)
    if not args or "--help" in args or "-h" in args:
        print(help_text(cfg))
        return
    opts = parse_args(cfg, args)
    if opts.depth is not None:
        cfg = replace(cfg, depth=opts.depth)
    if opts.mode:
        cfg.modes[opts.mode](cfg)
        return
    if opts.do_run:
        run_tests(cfg)
    xml_path = generate_report(cfg)
    if opts.by_module:
        by_module_report(cfg, xml_path)
    elif opts.target:
        detail_report(cfg, xml_path, opts.target, opts.branches_only)
    else:
        die("No package or path specified (or use --by-module).")
