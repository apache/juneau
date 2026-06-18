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
Drift guard for the published juneau-bom.

Every consumer-facing reactor module must be enumerated in juneau-bom/pom.xml's
<dependencyManagement>.  This catches the common failure mode where a new
publishable module is added to the reactor but the maintainer forgets to add it
to the BOM, so downstreams importing the BOM silently can't omit its version.

A module is "consumer-facing publishable" when it is a leaf artifact
(packaging jar/bundle, or a leaf packaging=pom dependency bundle with no
<modules>) AND it is not one of the documented non-published modules:

  - reactor aggregators (packaging=pom WITH <modules>) - not artifacts
  - juneau-bom itself
  - juneau-test-utils                    (test-only support)
  - juneau-shaded/*                      (uber-jars, not BOM-managed)
  - juneau-examples/*, juneau-petstore/* (sample apps, not published)
  - juneau-integration-tests             (test-only)
  - juneau-distrib                       (assembly only)

Exit 0 when the BOM is complete (and free of stale entries); exit 1 otherwise.
"""

from __future__ import annotations

import sys
import xml.etree.ElementTree as ET
from pathlib import Path

NS = {"m": "http://maven.apache.org/POM/4.0.0"}

# Path-prefix-based exclusions (relative to repo root).
EXCLUDED_PATH_PREFIXES = (
    "juneau-shaded/",
    "juneau-examples/",
    "juneau-petstore/",
    "juneau-distrib/",
)

# Artifact-id-based exclusions (leaf artifacts that are intentionally not published via the BOM).
EXCLUDED_ARTIFACTS = {
    "juneau-bom",                # the BOM cannot manage itself
    "juneau-test-utils",         # test-only support module
    "juneau-integration-tests",  # cross-module test harness, not published
    "juneau",                    # reactor root (defensive; also an aggregator)
}


def _text(el: ET.Element, tag: str) -> str | None:
    child = el.find(f"m:{tag}", NS)
    return child.text.strip() if child is not None and child.text else None


def repo_root() -> Path:
    # scripts/ lives directly under the reactor root.
    return Path(__file__).resolve().parent.parent


def own_artifact_id(root: ET.Element) -> str | None:
    """The module's own artifactId - the direct <project><artifactId>, not the <parent> one."""
    return _text(root, "artifactId")


def is_aggregator(root: ET.Element) -> bool:
    """A reactor aggregator: packaging=pom AND declares <modules>."""
    packaging = _text(root, "packaging") or "jar"
    has_modules = root.find("m:modules", NS) is not None
    return packaging == "pom" and has_modules


def collect_publishable(root_dir: Path) -> dict[str, Path]:
    """Map of consumer-facing publishable artifactId -> pom path."""
    out: dict[str, Path] = {}
    for pom in sorted(root_dir.rglob("pom.xml")):
        rel = pom.relative_to(root_dir).as_posix()
        if "/target/" in f"/{rel}":
            continue
        if any(rel.startswith(p) for p in EXCLUDED_PATH_PREFIXES):
            continue
        try:
            root = ET.parse(pom).getroot()
        except ET.ParseError as e:
            print(f"WARNING: could not parse {rel}: {e}", file=sys.stderr)
            continue
        aid = own_artifact_id(root)
        if not aid or aid in EXCLUDED_ARTIFACTS:
            continue
        if is_aggregator(root):
            continue
        # Skip the integration-tests reactor module (artifactId 'juneau', already excluded) and
        # any build-overlay helper poms living under an excluded tree (handled by path prefixes).
        out[aid] = pom
    return out


def collect_bom_entries(bom_pom: Path) -> set[str]:
    root = ET.parse(bom_pom).getroot()
    dm = root.find("m:dependencyManagement", NS)
    if dm is None:
        return set()
    out: set[str] = set()
    for dep in dm.iterfind("m:dependencies/m:dependency", NS):
        group = _text(dep, "groupId")
        if group != "org.apache.juneau":
            continue
        aid = _text(dep, "artifactId")
        if aid:
            out.add(aid)
    return out


def main() -> int:
    root_dir = repo_root()
    bom_pom = root_dir / "juneau-bom" / "pom.xml"
    if not bom_pom.is_file():
        print(f"ERROR: BOM not found at {bom_pom}", file=sys.stderr)
        return 1

    publishable = collect_publishable(root_dir)
    bom = collect_bom_entries(bom_pom)

    missing = sorted(set(publishable) - bom)
    stale = sorted(bom - set(publishable))

    if not missing and not stale:
        print(f"OK: juneau-bom enumerates all {len(publishable)} consumer-facing modules.")
        return 0

    if missing:
        print("ERROR: the following published modules are MISSING from juneau-bom:", file=sys.stderr)
        for aid in missing:
            print(f"  - {aid}  ({publishable[aid].relative_to(root_dir).as_posix()})", file=sys.stderr)
    if stale:
        print("ERROR: juneau-bom lists artifacts that are NOT consumer-facing reactor modules:", file=sys.stderr)
        for aid in stale:
            print(f"  - {aid}", file=sys.stderr)
    print(
        "\nFix: add the missing <dependency> entries to juneau-bom/pom.xml (or remove stale ones), "
        "or update the exclusion lists in scripts/check-bom-completeness.py if the module is "
        "intentionally not published.",
        file=sys.stderr,
    )
    return 1


if __name__ == "__main__":
    sys.exit(main())
