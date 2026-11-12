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
Runs javadoc's doclint html and accessibility groups over every main source tree and reports what is left once Juneau's own
custom Javadoc elements are ignored.

Full docs: check-javadoc-html.md
"""

from __future__ import annotations

import argparse
import collections
import os
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

# The one place that lists Juneau's own Javadoc elements. javadoc has no allowlist for custom elements, so doclint
# reports each as "unknown tag". These are the names styled in docs/src/javadoc/javadoc.css.
CUSTOM_ELEMENTS = frozenset((
    "jc", "jd", "jt", "jk", "js", "jf", "jp", "jv", "jsf", "jsm", "ja",  # Java
    "xt", "xa", "xc", "xs", "xv",  # XML
    "joc", "jok", "jov",  # JSON
    "ua", "uk", "un", "us",  # URL-encoding / UON
    "mc", "mk", "mv", "mi",  # manifest files
    "cc", "cs", "ck", "cv", "ci",  # config files
    "c", "dc", "bc", "l", "property",  # misc
))

# Diagnostics that come from compiling the sources without their dependencies on the classpath, not from doclint.
COMPILE_NOISE = re.compile(
    r"cannot find symbol|package .+ does not exist|does not override or implement|cannot be converted to|is ambiguous"
    r"|name clash|non-static .+ cannot be referenced|static import only|cannot be applied to|incompatible types"
    r"|cannot access|bad class file|invalid method declaration|cannot infer|cannot be dereferenced|has private access"
    r"|not abstract and does not override|is not public in|unreported exception|already defined"
)

DIAGNOSTIC = re.compile(r"^(?P<file>[^:]+\.java):(?P<line>\d+): (?P<kind>error|warning): (?P<msg>.*)$")
UNKNOWN_TAG = re.compile(r"^unknown tag: (?P<name>\S+)$")
GROUPS = ("html", "accessibility")


def source_files(root: Path) -> list[Path]:
    """Every Java file under a src/main/java tree, skipping target, docs and module-info."""
    out = []
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if d not in ("target", "docs", ".git", "node_modules")]
        if "/src/main/java/" in dirpath.replace(os.sep, "/") + "/":
            out.extend(Path(dirpath) / f for f in filenames if f.endswith(".java") and f != "module-info.java")
    return sorted(out)


def parse(output: str) -> list[tuple[str, int, str, str]]:
    """The (file, line, kind, message) of each javadoc diagnostic in output."""
    found = []
    for text in output.splitlines():
        m = DIAGNOSTIC.match(text)
        if m:
            found.append((m["file"], int(m["line"]), m["kind"], m["msg"]))
    return found


def classify(diagnostics):
    """Split into (remaining, ignored_custom, compile_noise). Duplicate reports of one file/line/message collapse."""
    remaining, custom, noise = [], collections.Counter(), 0
    seen = set()
    for file, line, kind, msg in diagnostics:
        key = (file, line, msg)
        if key in seen:
            continue
        seen.add(key)
        if COMPILE_NOISE.search(msg):
            noise += 1
            continue
        m = UNKNOWN_TAG.match(msg)
        if m and m["name"] in CUSTOM_ELEMENTS:
            custom[m["name"]] += 1
            continue
        remaining.append((file, line, kind, msg))
    return remaining, custom, noise


def normalize(msg: str) -> str:
    """Group key for the digest: the message without the parts that vary per occurrence."""
    msg = re.sub(r"(heading used out of sequence: )<H\d>, compared to (implicit|previous) preceding heading: <H\d>", r"\1<Hn>", msg)
    return msg


def run_javadoc(root: Path, groups: list[str], java_home: str | None) -> str:
    javadoc = str(Path(java_home) / "bin" / "javadoc") if java_home else (shutil.which("javadoc") or "javadoc")
    files = source_files(root)
    with tempfile.TemporaryDirectory(prefix="check-javadoc-html-") as tmp:
        argfile = Path(tmp) / "sources.txt"
        argfile.write_text("\n".join(str(f) for f in files) + "\n", encoding="utf-8")
        cmd = [javadoc, "-quiet", "-protected", "-Xdoclint:" + ",".join(groups), "-Xmaxerrs", "1000000", "-Xmaxwarns", "1000000",
               "--ignore-source-errors", "-d", str(Path(tmp) / "out"), "@" + str(argfile)]
        proc = subprocess.run(cmd, cwd=root, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, check=False)
        return proc.stdout


def digest(remaining, custom, noise, first: int, root: Path, out=None) -> None:
    out = out or sys.stdout
    by_kind = collections.defaultdict(list)
    for file, line, _, msg in remaining:
        by_kind[normalize(msg)].append((file, line))
    print(f"Ignored {sum(custom.values())} custom-element reports ({len(custom)} elements) and {noise} compile-noise reports.", file=out)
    if not remaining:
        print("javadoc html check: clean.", file=out)
        return
    print(f"javadoc html check: {len(remaining)} diagnostics in {len(by_kind)} kinds.", file=out)
    for kind, places in sorted(by_kind.items(), key=lambda kv: -len(kv[1])):
        print(f"\n{len(places):6d}  {kind}", file=out)
        for file, line in places[:first]:
            try:
                file = str(Path(file).resolve().relative_to(root.resolve()))
            except ValueError:
                pass
            print(f"          {file}:{line}", file=out)


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--groups", default=",".join(GROUPS), help="comma-separated doclint groups to run (default: %(default)s)")
    ap.add_argument("--first", type=int, default=5, help="examples to print per kind (default: %(default)s)")
    ap.add_argument("--report-only", action="store_true", help="print the digest but always exit 0")
    ap.add_argument("--root", default=str(Path(__file__).resolve().parent.parent), help="repository root (default: this checkout)")
    ap.add_argument("--java-home", default=os.environ.get("JAVA_HOME"), help="JDK whose javadoc to run (default: $JAVA_HOME, else PATH)")
    ap.add_argument("--from-file", help="read javadoc output from this file instead of running javadoc (for tests)")
    args = ap.parse_args(argv)
    root = Path(args.root)
    groups = [g for g in args.groups.split(",") if g]
    bad = [g for g in groups if g not in GROUPS]
    if bad:
        ap.error("unknown group(s): " + ", ".join(bad))
    output = Path(args.from_file).read_text(encoding="utf-8") if args.from_file else run_javadoc(root, groups, args.java_home)
    remaining, custom, noise = classify(parse(output))
    digest(remaining, custom, noise, args.first, root)
    return 0 if args.report_only or not remaining else 1


if __name__ == "__main__":
    sys.exit(main())
