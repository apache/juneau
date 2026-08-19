#!/usr/bin/env python3
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""
Headless reproduction of Eclipse JDT compiler warnings via `ecj`.

`ecj` is the exact JDT compiler Eclipse embeds. Driven by a module's own
.settings/org.eclipse.jdt.core.prefs (the file scripts/apply-eclipse-prefs.py
seeds/syncs), it reproduces Eclipse's Problems-view diagnostics verbatim,
including exact message strings -- without a live Eclipse session or the
AssistAI Eclipse MCP server. See the `eclipse-warnings` skill's "Headless
fallback" section for the full technique writeup and when to prefer it.

Usage (run from the repo root):
    python3 scripts/eclipse-warnings.py <module-path>   # e.g. juneau-rest/juneau-rest-server
    python3 scripts/eclipse-warnings.py --all           # every source module apply-eclipse-prefs.py would touch

Requires:
    - `ecj` 3.45.0. Resolved automatically from ~/.m2/repository/org/eclipse/jdt/ecj/3.45.0/
      (already present if the project has ever built; the ecj Maven coordinate is
      org.eclipse.jdt:ecj:3.45.0). Override with the ECJ_JAR env var if it lives elsewhere.
    - the module's own .settings/org.eclipse.jdt.core.prefs to exist (run
      apply-eclipse-prefs.py first if missing/stale).
    - `mvn` on PATH, to resolve the module's runtime+test classpath.

Javadoc diagnostics additionally need -enableJavadoc plus explicit javadoc
severities (passed below unconditionally) because the repo's tracked .prefs
files set no javadoc options at all -- those particular diagnostics come from
workspace-level Eclipse settings that no tracked file controls. Omitting the
flags means a javadoc warning Eclipse reports simply won't reproduce here.

Caveat -- classpath uses INSTALLED reactor jars, not live sources: the runtime
classpath comes from `mvn dependency:build-classpath`, which resolves other
reactor modules' jars from ~/.m2 as of their last `mvn install`, not their
current on-disk source. A class added/changed in an upstream module since the
last install won't be visible here ("X cannot be resolved" for a genuinely
new/renamed type is the tell) -- `mvn install` the affected upstream module(s)
first. Also expect occasional "invalid Class-Path header in manifest of jar
file ..." lines from ecj scanning a third-party dependency's own bundled
manifest; that's noise unrelated to Juneau's code and safe to ignore.
"""

import glob
import importlib.util
import os
import subprocess
import sys
import tempfile
from pathlib import Path

DEFAULT_ECJ_JAR = Path.home() / ".m2/repository/org/eclipse/jdt/ecj/3.45.0/ecj-3.45.0.jar"


def find_ecj_jar():
    override = os.environ.get("ECJ_JAR")
    if override:
        return Path(override)
    return DEFAULT_ECJ_JAR


def load_apply_prefs_module(script_dir):
    """Reuse apply-eclipse-prefs.py's module discovery so both scripts agree
    on what counts as a module and how it's classified source vs. test."""
    spec = importlib.util.spec_from_file_location(
        "apply_eclipse_prefs", script_dir / "apply-eclipse-prefs.py"
    )
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def build_classpath(module_dir):
    """Run `mvn dependency:build-classpath` from inside the module dir itself
    (not `-pl <module>` from the reactor root). `-pl` still triggers a
    multi-module reactor pass, and multiple modules' plugin executions racing
    to write the same `-Dmdep.outputFile` target corrupts the output (jar
    paths get concatenated with missing `:` separators). Running from the
    module's own directory keeps this to a single-module build."""
    with tempfile.NamedTemporaryFile(prefix="ecj-cp-", suffix=".txt", delete=False) as tmp:
        cp_file = tmp.name
    try:
        result = subprocess.run(
            ["mvn", "-q", "dependency:build-classpath", f"-Dmdep.outputFile={cp_file}"],
            cwd=module_dir, capture_output=True, text=True, timeout=180,
        )
        if result.returncode != 0:
            print(f"Warning: `mvn dependency:build-classpath` failed for {module_dir}:", file=sys.stderr)
            print(result.stderr, file=sys.stderr)
            return ""
        return Path(cp_file).read_text().strip()
    finally:
        os.unlink(cp_file)


def java_sources(module_dir):
    files = []
    for sub in ("src/main/java", "src/test/java"):
        src_dir = module_dir / sub
        if src_dir.exists():
            files.extend(glob.glob(str(src_dir / "**" / "*.java"), recursive=True))
    return files


def run_ecj(ecj_jar, module_dir, module_path, classpath):
    prefs = module_dir / ".settings" / "org.eclipse.jdt.core.prefs"
    if not prefs.exists():
        print(f"Skipping {module_path}: no .settings/org.eclipse.jdt.core.prefs "
              f"(run apply-eclipse-prefs.py first)", file=sys.stderr)
        return None

    sources = java_sources(module_dir)
    if not sources:
        print(f"Skipping {module_path}: no .java sources found", file=sys.stderr)
        return None

    target_classes = module_dir / "target" / "classes"
    test_classes = module_dir / "target" / "test-classes"
    cp_parts = [classpath] if classpath else []
    for extra in (target_classes, test_classes):
        if extra.exists():
            cp_parts.append(str(extra))
    full_cp = os.pathsep.join(p for p in cp_parts if p)

    with tempfile.TemporaryDirectory(prefix="ecj-out-") as out_dir:
        cmd = [
            "java", "-jar", str(ecj_jar),
            "-properties", str(prefs),
            "-enableJavadoc", "-warn:+allJavadoc,invalidJavadoc,javadoc",
            "-cp", full_cp,
            "-d", out_dir,
        ] + sources
        result = subprocess.run(cmd, cwd=module_dir, capture_output=True, text=True, timeout=300)
        return result.stdout + result.stderr


def summarize(output):
    """Pull ecj's trailing '<n> problems (<e> errors, <w> warnings)' line, if present."""
    for line in reversed(output.strip().splitlines()):
        if "problem" in line and ("error" in line or "warning" in line):
            return line.strip()
    return None


def main():
    script_dir = Path(__file__).parent
    root_dir = script_dir.parent

    if len(sys.argv) != 2:
        print(__doc__)
        sys.exit(1)

    ecj_jar = find_ecj_jar()
    if not ecj_jar.exists():
        print(f"ecj jar not found at {ecj_jar}.", file=sys.stderr)
        print("Resolve it first, e.g.: mvn dependency:get -Dartifact=org.eclipse.jdt:ecj:3.45.0", file=sys.stderr)
        print("or set ECJ_JAR to point at an existing copy.", file=sys.stderr)
        sys.exit(2)

    if sys.argv[1] == "--all":
        apply_prefs = load_apply_prefs_module(script_dir)
        source_modules, test_modules = apply_prefs.discover_projects(root_dir)
        module_paths = source_modules + test_modules
    else:
        module_paths = [sys.argv[1]]

    any_problems = False
    for module_path in module_paths:
        module_dir = root_dir / module_path
        if not module_dir.exists():
            print(f"Warning: module not found: {module_path}", file=sys.stderr)
            continue

        print(f"\n{'=' * 60}\n{module_path}\n{'=' * 60}")
        classpath = build_classpath(module_dir)
        output = run_ecj(ecj_jar, module_dir, module_path, classpath)
        if output is None:
            continue

        print(output)
        summary = summarize(output)
        if summary:
            print(f">>> {module_path}: {summary}")
            if "0 errors, 0 warnings" not in summary:
                any_problems = True

    sys.exit(1 if any_problems else 0)


if __name__ == '__main__':
    main()
