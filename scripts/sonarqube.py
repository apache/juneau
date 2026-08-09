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
SonarQube/SonarCloud findings reporter for Apache Juneau.

Shows per-file SonarCloud findings (rule id, severity, line, message) for a
source file, package directory, or Maven module. Mirrors scripts/coverage.py.
Also supports a whole-repo mode that summarizes findings across every
module in a single pass.

Usage:
    ./scripts/sonarqube.py <path> [options]
    ./scripts/sonarqube.py --all [options]
    ./scripts/sonarqube.py [options]           (no path => whole-repo mode)

Arguments:
    path    A source file (.java), package directory under src/main/java or
            src/test/java, or Maven module root. Absolute or repo-relative.
            Optional: omitting it (or passing --all) switches to whole-repo
            mode, which reports totals and a per-module breakdown instead
            of per-file findings.

Options:
    --all                            Whole-repo mode: report findings across
                                    every module instead of a single path.
                                    Implied when no <path> is given.
    --run, -r                       Re-fetch issues from SonarCloud Web API
                                    and overwrite the local cache.
    --severity SEV[,SEV...]         Filter by severity. Comma-separated.
                                    BLOCKER,CRITICAL,MAJOR,MINOR,INFO
    --rule java:Sxxx                Filter by rule id. Repeatable.
    --type TYPE[,TYPE...]           Filter by issue type. Comma-separated.
                                    CODE_SMELL,BUG,VULNERABILITY,SECURITY_HOTSPOT
    --branch <branch>               SonarCloud branch (default: master).
    --with-suppress-hint            Append @SuppressWarnings hint per finding.
    --detail                        Whole-repo mode only: also print the
                                    per-file findings blocks after the
                                    summary (module table + top rules).
    --max <N>                       Cap printed findings (default 200).
    --fail-on-issues                After applying all filters above, exit
                                    with code 2 if any matched issues remain
                                    (exit 0 if none). All normal stdout
                                    reporting is unchanged; this only
                                    affects the process exit code. Intended
                                    for use as a CI/pre-push gate primitive,
                                    e.g. `--all --run --fail-on-issues`.
    --help, -h                      Show this help message.

Authentication:
    Anonymous by default (apache_juneau is a public SonarCloud project).
    If SONAR_TOKEN is set in the environment, it is sent as a Bearer token
    to raise the rate limit. The token is never echoed.

Examples:
    ./scripts/sonarqube.py juneau-core/juneau-marshall/src/main/java/org/apache/juneau/sse/
    ./scripts/sonarqube.py juneau-core/juneau-marshall/src/main/java/org/apache/juneau/MarshalledPropertyPostProcessor.java
    ./scripts/sonarqube.py juneau-core/juneau-marshall --severity BLOCKER,CRITICAL
    ./scripts/sonarqube.py juneau-core/juneau-marshall/src/main/java/org/apache/juneau/sse/ --rule java:S3776
    ./scripts/sonarqube.py path/to/file.java --with-suppress-hint
    ./scripts/sonarqube.py path/to/folder/ --run
    ./scripts/sonarqube.py --all --run
    ./scripts/sonarqube.py --all --severity BLOCKER,CRITICAL
    ./scripts/sonarqube.py --all --rule java:S3776 --detail
    ./scripts/sonarqube.py --all --run --fail-on-issues
    ./scripts/sonarqube.py --all --severity BLOCKER,CRITICAL --fail-on-issues
"""

import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
CACHE_PATH = REPO_ROOT / "target" / ".sonar-issues.json"

SONAR_HOST = "https://sonarcloud.io"
SONAR_PROJECT_KEY = "apache_juneau"
SONAR_ORG = "apache"
SONAR_PAGE_SIZE = 500
SONAR_MAX_ISSUES = 5000  # Safety cap; ~3000 issues today.

SRC_MARKERS = ["src/main/java", "src/test/java"]
SEVERITIES = ["BLOCKER", "CRITICAL", "MAJOR", "MINOR", "INFO"]
SEVERITY_RANK = {s: i for i, s in enumerate(SEVERITIES)}
ISSUE_TYPES = ["CODE_SMELL", "BUG", "VULNERABILITY", "SECURITY_HOTSPOT"]

USE_COLOR = sys.stdout.isatty()


def die(msg):
    print(f"ERROR: {msg}", file=sys.stderr)
    sys.exit(1)


def color(text, code):
    if not USE_COLOR or not code:
        return text
    return f"\033[{code}m{text}\033[0m"


def severity_color(sev):
    return {
        "BLOCKER": "1;31",   # bold red
        "CRITICAL": "31",    # red
        "MAJOR": "33",       # yellow
        "MINOR": "36",       # cyan
        "INFO": "2",         # dim
    }.get(sev, "")


def find_maven_module(path: Path) -> Path:
    """Walk up from path to find the nearest directory containing a pom.xml."""
    p = path if path.is_dir() else path.parent
    while p != REPO_ROOT.parent:
        if (p / "pom.xml").exists() and p != REPO_ROOT:
            return p
        p = p.parent
    return None


def repo_relative(path: Path) -> str:
    """Return path relative to REPO_ROOT as a forward-slash string."""
    try:
        rel = path.relative_to(REPO_ROOT)
    except ValueError:
        die(f"Path is not under repo root ({REPO_ROOT}): {path}")
    return rel.as_posix()


def bar(covered, total, width=20):
    """Render a simple ASCII progress bar (mirrors scripts/coverage.py)."""
    if total == 0:
        filled = width
    else:
        filled = round(covered / total * width)
    return "[" + "#" * filled + "." * (width - filled) + "]"


def pct(covered, total):
    if total == 0:
        return "100%"
    return f"{covered / total * 100:.0f}%"


def http_get_json(url: str) -> dict:
    """GET <url> as JSON. Honors SONAR_TOKEN from env if set. Never echoes it."""
    headers = {"Accept": "application/json", "User-Agent": "juneau-sonarqube.py/1.0"}
    token = os.environ.get("SONAR_TOKEN")
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        if e.code == 429:
            die(
                "SonarCloud rate limit hit (HTTP 429). "
                "Set SONAR_TOKEN in the environment to raise the limit, then retry."
            )
        if e.code in (401, 403):
            die(
                f"SonarCloud authorization failed (HTTP {e.code}). "
                "If SONAR_TOKEN is set, verify it is valid; otherwise unset it and retry anonymously."
            )
        die(f"SonarCloud HTTP {e.code}: {e.reason} for {scrub_url(url)}")
    except urllib.error.URLError as e:
        die(
            f"SonarCloud network error: {e.reason}. Check connectivity / VPN. "
            "TODO: a future --from-tsv <path> flag could load a manually-exported TSV as a fallback."
        )


def scrub_url(url: str) -> str:
    """Strip query strings that might contain sensitive bits before logging."""
    parsed = urllib.parse.urlsplit(url)
    return urllib.parse.urlunsplit((parsed.scheme, parsed.netloc, parsed.path, "", ""))


def fetch_all_issues(branch: str) -> dict:
    """
    Fetch all project-wide issues for apache_juneau on the given branch.

    The cache is intentionally project-wide (not path-scoped) so subsequent
    invocations on different paths can reuse it without re-hitting the API.
    Path / severity / rule / type filtering is applied client-side.
    """
    issues = []
    components = {}
    page = 1
    total = None
    while True:
        params = {
            "componentKeys": SONAR_PROJECT_KEY,
            "organization": SONAR_ORG,
            "branch": branch,
            "p": str(page),
            "ps": str(SONAR_PAGE_SIZE),
            "additionalFields": "rules",
        }
        url = f"{SONAR_HOST}/api/issues/search?{urllib.parse.urlencode(params)}"
        data = http_get_json(url)
        page_issues = data.get("issues", [])
        issues.extend(page_issues)
        for c in data.get("components", []) or []:
            key = c.get("key")
            if key and key not in components:
                components[key] = c
        total = data.get("total") or data.get("paging", {}).get("total", 0)
        if not page_issues:
            break
        if len(issues) >= total or len(issues) >= SONAR_MAX_ISSUES:
            break
        page += 1

    fetched_at = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    return {
        "meta": {
            "fetched_at": fetched_at,
            "branch": branch,
            "total": total or len(issues),
            "project_key": SONAR_PROJECT_KEY,
        },
        "issues": issues,
        "components": components,
    }


def load_cache() -> dict | None:
    if not CACHE_PATH.exists():
        return None
    try:
        with CACHE_PATH.open("r", encoding="utf-8") as fh:
            return json.load(fh)
    except (OSError, json.JSONDecodeError):
        return None


def save_cache(payload: dict) -> None:
    CACHE_PATH.parent.mkdir(parents=True, exist_ok=True)
    with CACHE_PATH.open("w", encoding="utf-8") as fh:
        json.dump(payload, fh, indent=2)


def issue_path_from_component(component_key: str) -> str:
    """
    Map a SonarCloud component key like 'apache_juneau:juneau-core/.../Foo.java'
    back to a repo-relative path 'juneau-core/.../Foo.java'.
    """
    if component_key.startswith(SONAR_PROJECT_KEY + ":"):
        return component_key[len(SONAR_PROJECT_KEY) + 1:]
    return component_key


def extract_path(issue: dict) -> str:
    """Best-effort extraction of repo-relative path from an issue dict."""
    comp = issue.get("component", "")
    return issue_path_from_component(comp)


def matches_path(issue_path: str, target: str, is_file: bool) -> bool:
    if is_file:
        return issue_path == target
    target_norm = target.rstrip("/") + "/"
    return (issue_path + "/").startswith(target_norm)


def passes_filters(iss: dict, severities: set, rules: set, types: set) -> bool:
    """Check a single issue against the severity/rule/type filters."""
    if severities and iss.get("severity") not in severities:
        return False
    if rules and iss.get("rule") not in rules:
        return False
    if types and iss.get("type") not in types:
        return False
    return True


def filter_issues(
    issues, target_path: str, is_file: bool,
    severities: set, rules: set, types: set
):
    """Apply the path scope plus client-side filters and return matching issues."""
    out = []
    for iss in issues:
        ipath = extract_path(iss)
        if not ipath:
            continue
        if not matches_path(ipath, target_path, is_file):
            continue
        if not passes_filters(iss, severities, rules, types):
            continue
        out.append(iss)
    return out


def filter_issues_all(issues, severities: set, rules: set, types: set):
    """Apply severity/rule/type filters across ALL cached issues (no path scope)."""
    out = []
    for iss in issues:
        ipath = extract_path(iss)
        if not ipath:
            continue
        if not passes_filters(iss, severities, rules, types):
            continue
        out.append(iss)
    return out


def group_by_file(issues):
    """Return {repo_relative_path: [issues...]} preserving order."""
    out = {}
    for iss in issues:
        p = extract_path(iss)
        out.setdefault(p, []).append(iss)
    return out


def severity_counts(issues):
    counts = {s: 0 for s in SEVERITIES}
    for iss in issues:
        sev = iss.get("severity", "INFO")
        if sev in counts:
            counts[sev] += 1
    return counts


def fmt_counts(counts):
    parts = []
    for s in SEVERITIES:
        if counts[s]:
            parts.append(f"{counts[s]} {s.lower()}")
    return ", ".join(parts) if parts else "none"


_MODULE_CACHE: dict = {}


def resolve_module_cached(issue_path: str) -> Path | None:
    """
    Resolve a repo-relative issue path to its Maven module directory.

    Memoized by the file's parent directory so whole-repo mode (~3000
    issues) doesn't re-stat the filesystem for every issue in the same
    package.
    """
    file_path = REPO_ROOT / issue_path
    parent = file_path.parent
    if parent not in _MODULE_CACHE:
        _MODULE_CACHE[parent] = find_maven_module(file_path)
    return _MODULE_CACHE[parent]


def group_by_module(issues):
    """Return {module_label: [issues...]}, bucketing unresolvable paths under '(unresolved)'."""
    out = {}
    for iss in issues:
        ipath = extract_path(iss)
        module = resolve_module_cached(ipath) if ipath else None
        label = repo_relative(module) if module else "(unresolved)"
        out.setdefault(label, []).append(iss)
    return out


def print_module_table(matched):
    """Print one row per Maven module: total + per-severity counts, sorted by severity."""
    by_module = group_by_module(matched)
    rows = []
    for label, iss_list in by_module.items():
        counts = severity_counts(iss_list)
        rows.append((label, len(iss_list), counts))
    rows.sort(key=lambda r: (-(r[2]["BLOCKER"] + r[2]["CRITICAL"]), -r[1], r[0]))

    label_width = max([len(r[0]) for r in rows] + [len("Module")])

    print()
    print("=" * 70)
    print("  BY MODULE")
    print("=" * 70)
    header = (
        f"  {'Module':<{label_width}}  {'Total':>5}  {'Blocker':>7}  "
        f"{'Critical':>8}  {'Major':>5}  {'Minor':>5}  {'Info':>4}"
    )
    print(header)
    print(f"  {'-' * (label_width + 46)}")
    for label, total, counts in rows:
        cols = "  ".join(
            color(f"{counts[s]:>{len(s.title())}}", severity_color(s))
            for s in ("BLOCKER", "CRITICAL", "MAJOR", "MINOR", "INFO")
        )
        print(f"  {label:<{label_width}}  {total:>5}  {cols}")


def print_findings_for_file(  # NOSONAR java:S3776 -- table-style formatter; cognitive complexity is acceptable for I/O code paths
    file_path: str, file_issues, with_suppress_hint: bool, max_remaining: int
) -> int:
    """Print a per-file block. Return number of findings printed."""
    file_issues = sorted(
        file_issues,
        key=lambda i: (i.get("line", 0) or 0, SEVERITY_RANK.get(i.get("severity", "INFO"), 99)),
    )
    counts = severity_counts(file_issues)
    total = len(file_issues)

    pretty_path = file_path.replace("/", ".").removesuffix(".java")
    print()
    print("=" * 70)
    print(f"  {pretty_path}")
    print("=" * 70)

    bar_width = 20
    blocker_critical = counts["BLOCKER"] + counts["CRITICAL"]
    filled = min(bar_width, blocker_critical) if total else 0
    progress = "[" + "#" * filled + "." * (bar_width - filled) + "]"
    summary_breakdown = fmt_counts(counts)
    print(f"  Findings:     {progress}  {total} issue{'s' if total != 1 else ''}  ({summary_breakdown})")
    if total == 0:
        return 0

    print()
    printed = 0
    for iss in file_issues:
        if printed >= max_remaining:
            print(f"  ... {total - printed} more (raise --max to see all).")
            break
        line_no = iss.get("line", 0) or 0
        rule = iss.get("rule", "?")
        sev = iss.get("severity", "INFO")
        msg = (iss.get("message") or "").replace("\n", " ").strip()
        sev_str = color(f"{sev:<8}", severity_color(sev))
        rule_str = f"{rule:<14}"
        if line_no:
            line_str = f"line {line_no:>4}:"
        else:
            line_str = "line    -:"
        print(f"  {line_str}  {rule_str}  {sev_str}  {msg}")
        if with_suppress_hint:
            indent = " " * len(f"  {line_str}  ")
            print(f"{indent}→ @SuppressWarnings(\"{rule}\")")
        printed += 1
    return printed


def report(  # NOSONAR java:S3776 -- top-level report orchestrator; complexity tracks the spec layout
    payload: dict,
    target_path: str,
    is_file: bool,
    severities: set,
    rules: set,
    types: set,
    with_suppress_hint: bool,
    max_print: int,
    branch: str,
):
    issues = payload.get("issues", [])
    matched = filter_issues(issues, target_path, is_file, severities, rules, types)

    total_in_cache = len(issues)
    total_matched = len(matched)
    counts = severity_counts(matched)

    print(f"Project:  {SONAR_PROJECT_KEY}  (branch: {branch})")
    print(f"Scope:    {target_path}{'  [file]' if is_file else '  [directory]'}")
    print(f"Cache:    {CACHE_PATH.relative_to(REPO_ROOT)}")
    flt_parts = []
    if severities and severities != set(SEVERITIES):
        flt_parts.append(f"severity={','.join(sorted(severities, key=SEVERITY_RANK.get))}")
    if rules:
        flt_parts.append(f"rule={','.join(sorted(rules))}")
    if types and types != set(ISSUE_TYPES):
        flt_parts.append(f"type={','.join(sorted(types))}")
    if flt_parts:
        print(f"Filters:  {'; '.join(flt_parts)}")
    print(
        f"Findings: {total_matched} matched of {total_in_cache} in cache  "
        f"({fmt_counts(counts)})"
    )

    if total_matched == 0:
        print()
        print("No findings for this scope/filter combination.")
        return total_matched

    by_file = group_by_file(matched)
    remaining = max_print
    for fpath in sorted(by_file.keys()):
        if remaining <= 0:
            print(f"\n... {sum(len(v) for v in by_file.values()) - max_print} more findings hidden (raise --max).")
            break
        printed = print_findings_for_file(
            fpath, by_file[fpath], with_suppress_hint, remaining
        )
        remaining -= printed

    if len(by_file) > 1:
        print()
        print("=" * 70)
        print("  TOTAL")
        print("=" * 70)
        print(f"  {total_matched} issues across {len(by_file)} files  ({fmt_counts(counts)})")
        rule_counter = {}
        for iss in matched:
            rule_counter[iss.get("rule", "?")] = rule_counter.get(iss.get("rule", "?"), 0) + 1
        top_rules = sorted(rule_counter.items(), key=lambda kv: -kv[1])[:5]
        if top_rules:
            print()
            print("  Top rules:")
            for r, n in top_rules:
                print(f"    {r:<14}  {n}")
        print()

    return total_matched


def report_whole_repo(  # NOSONAR java:S3776 -- top-level report orchestrator; complexity tracks the spec layout
    payload: dict,
    severities: set,
    rules: set,
    types: set,
    with_suppress_hint: bool,
    max_print: int,
    branch: str,
    detail: bool,
):
    issues = payload.get("issues", [])
    matched = filter_issues_all(issues, severities, rules, types)

    total_in_cache = len(issues)
    total_matched = len(matched)
    counts = severity_counts(matched)

    print(f"Project:  {SONAR_PROJECT_KEY}  (branch: {branch})")
    print("Scope:    WHOLE REPO")
    print(f"Cache:    {CACHE_PATH.relative_to(REPO_ROOT)}")
    flt_parts = []
    if severities and severities != set(SEVERITIES):
        flt_parts.append(f"severity={','.join(sorted(severities, key=SEVERITY_RANK.get))}")
    if rules:
        flt_parts.append(f"rule={','.join(sorted(rules))}")
    if types and types != set(ISSUE_TYPES):
        flt_parts.append(f"type={','.join(sorted(types))}")
    if flt_parts:
        print(f"Filters:  {'; '.join(flt_parts)}")
    print(
        f"Findings: {total_matched} matched of {total_in_cache} in cache  "
        f"({fmt_counts(counts)})"
    )

    if total_matched == 0:
        print()
        print("No findings for this filter combination.")
        return total_matched

    print_module_table(matched)

    rule_counter = {}
    for iss in matched:
        rule_counter[iss.get("rule", "?")] = rule_counter.get(iss.get("rule", "?"), 0) + 1
    top_rules = sorted(rule_counter.items(), key=lambda kv: -kv[1])[:15]
    if top_rules:
        print()
        print("=" * 70)
        print("  TOP RULES")
        print("=" * 70)
        for r, n in top_rules:
            print(f"    {r:<14}  {n}")
        print()

    if not detail:
        return total_matched

    by_file = group_by_file(matched)
    remaining = max_print
    for fpath in sorted(by_file.keys()):
        if remaining <= 0:
            print(f"\n... {sum(len(v) for v in by_file.values()) - max_print} more findings hidden (raise --max).")
            break
        printed = print_findings_for_file(
            fpath, by_file[fpath], with_suppress_hint, remaining
        )
        remaining -= printed

    return total_matched


def parse_csv_arg(raw: str, valid: list, label: str) -> set:
    out = set()
    for v in raw.split(","):
        v = v.strip().upper()
        if not v:
            continue
        if v not in valid:
            die(f"Invalid {label}: '{v}'. Allowed: {','.join(valid)}")
        out.add(v)
    return out


def parse_args(argv):  # NOSONAR java:S3776 -- argparse-by-hand mirror of coverage.py; complexity acceptable
    args = argv[1:]
    if not args or "--help" in args or "-h" in args:
        print(__doc__)
        sys.exit(0)

    path_arg = None
    do_run = False
    do_all = False
    detail = False
    severities = set()
    rules = set()
    types = set()
    branch = "master"
    with_suppress_hint = False
    max_print = 200
    fail_on_issues = False

    i = 0
    while i < len(args):
        a = args[i]
        if a in ("--run", "-r"):
            do_run = True
        elif a == "--all":
            do_all = True
        elif a == "--detail":
            detail = True
        elif a == "--fail-on-issues":
            fail_on_issues = True
        elif a == "--severity":
            i += 1
            if i >= len(args):
                die("--severity requires a value")
            severities |= parse_csv_arg(args[i], SEVERITIES, "severity")
        elif a.startswith("--severity="):
            severities |= parse_csv_arg(a.split("=", 1)[1], SEVERITIES, "severity")
        elif a == "--rule":
            i += 1
            if i >= len(args):
                die("--rule requires a value")
            rules.add(args[i])
        elif a.startswith("--rule="):
            rules.add(a.split("=", 1)[1])
        elif a == "--type":
            i += 1
            if i >= len(args):
                die("--type requires a value")
            types |= parse_csv_arg(args[i], ISSUE_TYPES, "type")
        elif a.startswith("--type="):
            types |= parse_csv_arg(a.split("=", 1)[1], ISSUE_TYPES, "type")
        elif a == "--branch":
            i += 1
            if i >= len(args):
                die("--branch requires a value")
            branch = args[i]
        elif a.startswith("--branch="):
            branch = a.split("=", 1)[1]
        elif a == "--with-suppress-hint":
            with_suppress_hint = True
        elif a == "--max":
            i += 1
            if i >= len(args):
                die("--max requires a value")
            max_print = int(args[i])
        elif a.startswith("--max="):
            max_print = int(a.split("=", 1)[1])
        elif a.startswith("-"):
            die(f"Unknown option: {a}")
        else:
            if path_arg is not None:
                die(f"Unexpected extra positional argument: {a}")
            path_arg = a
        i += 1

    whole_repo = do_all or not path_arg

    return {
        "path_arg": path_arg,
        "whole_repo": whole_repo,
        "do_run": do_run,
        "detail": detail,
        "severities": severities,
        "rules": rules,
        "types": types,
        "branch": branch,
        "with_suppress_hint": with_suppress_hint,
        "max_print": max_print,
        "fail_on_issues": fail_on_issues,
    }


def main():  # NOSONAR java:S3776 -- thin orchestrator with simple branching
    opts = parse_args(sys.argv)
    whole_repo = opts["whole_repo"]

    rel_path = None
    is_file = False
    if not whole_repo:
        path = Path(opts["path_arg"])
        if not path.is_absolute():
            path = REPO_ROOT / path
        path = path.resolve()
        if not path.exists():
            die(f"Path does not exist: {path}")

        rel_path = repo_relative(path)
        is_file = path.is_file()

        module = find_maven_module(path) if path.is_dir() or path.is_file() else None
        if path.is_file() and not module:
            die(f"Could not determine Maven module for path: {path}")

    payload = None
    if not opts["do_run"]:
        payload = load_cache()

    if payload is None:
        if not opts["do_run"]:
            print("No cache found. Fetching from SonarCloud...")
        else:
            print(f"Refreshing cache from SonarCloud (branch={opts['branch']})...")
        payload = fetch_all_issues(opts["branch"])
        save_cache(payload)
        print(
            f"Fetched {len(payload.get('issues', []))} issues "
            f"(total reported: {payload.get('meta', {}).get('total')})."
        )
    else:
        meta = payload.get("meta", {})
        cached_branch = meta.get("branch", "?")
        if cached_branch != opts["branch"]:
            print(
                f"Cached branch ({cached_branch}) differs from --branch {opts['branch']}. "
                "Re-run with --run to refresh."
            )
        else:
            print(
                f"Using cached issues (fetched {meta.get('fetched_at', '?')}, "
                f"branch={cached_branch}). Use --run to refresh."
            )

    if whole_repo:
        total_matched = report_whole_repo(
            payload,
            opts["severities"],
            opts["rules"],
            opts["types"],
            opts["with_suppress_hint"],
            opts["max_print"],
            opts["branch"],
            opts["detail"],
        )
    else:
        total_matched = report(
            payload,
            rel_path,
            is_file,
            opts["severities"],
            opts["rules"],
            opts["types"],
            opts["with_suppress_hint"],
            opts["max_print"],
            opts["branch"],
        )

    if opts["fail_on_issues"] and (total_matched or 0) > 0:
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
