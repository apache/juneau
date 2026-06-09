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
#
"""
Roll up Surefire XML results into timing buckets.
"""

from __future__ import annotations

import argparse
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path


@dataclass
class BucketStats:
    tests: int = 0
    seconds: float = 0.0


def parse_xml_dir(directory: Path) -> BucketStats:
    stats = BucketStats()
    if not directory.exists():
        return stats
    for xml_file in sorted(directory.glob("TEST-*.xml")):
        root = ET.parse(xml_file).getroot()
        stats.tests += int(root.attrib.get("tests", 0))
        stats.seconds += float(root.attrib.get("time", 0.0))
    return stats


def source_for_class(module_dir: Path, class_name: str) -> Path | None:
    if not class_name:
        return None
    parts = class_name.split(".")
    if len(parts) < 2:
        return None
    return module_dir / "src" / "test" / "java" / Path("/".join(parts)).with_suffix(".java")


def source_content(module_dir: Path, class_name: str) -> str:
    source = source_for_class(module_dir, class_name)
    if source and source.exists():
        return source.read_text(encoding="utf-8")
    return ""


def container_bucket(module_dir: Path, class_name: str) -> str:
    content = source_content(module_dir, class_name)
    if "SpringbootTest" in content:
        return "container.springboot"
    if "JettyMicroserviceTest" in content:
        return "container.jetty"
    lower = class_name.lower()
    if "springboot" in lower:
        return "container.springboot"
    if "jetty" in lower or "microservice" in lower:
        return "container.jetty"
    return ""


def parse_container_subbuckets(module_dir: Path, container_dir: Path) -> dict[str, BucketStats]:
    buckets: dict[str, BucketStats] = {
        "container.springboot": BucketStats(),
        "container.jetty": BucketStats(),
    }
    if not container_dir.exists():
        return buckets
    for xml_file in sorted(container_dir.glob("TEST-*.xml")):
        root = ET.parse(xml_file).getroot()
        class_name = root.attrib.get("name", "")
        bucket = container_bucket(module_dir, class_name) or "container.jetty"
        buckets[bucket].tests += int(root.attrib.get("tests", 0))
        buckets[bucket].seconds += float(root.attrib.get("time", 0.0))
    return buckets


def parse_flat_reports(module_dir: Path, reports_root: Path):
    core = BucketStats()
    sub = {
        "container.springboot": BucketStats(),
        "container.jetty": BucketStats(),
    }
    for xml_file in sorted(reports_root.glob("TEST-*.xml")):
        root = ET.parse(xml_file).getroot()
        tests = int(root.attrib.get("tests", 0))
        seconds = float(root.attrib.get("time", 0.0))
        class_name = root.attrib.get("name", "")
        content = source_content(module_dir, class_name)
        bucket = container_bucket(module_dir, class_name)
        if "SpringbootTest" in content:
            sub["container.springboot"].tests += tests
            sub["container.springboot"].seconds += seconds
        elif "JettyMicroserviceTest" in content:
            sub["container.jetty"].tests += tests
            sub["container.jetty"].seconds += seconds
        elif bucket.startswith("container."):
            sub[bucket].tests += tests
            sub[bucket].seconds += seconds
        else:
            core.tests += tests
            core.seconds += seconds
    container = BucketStats(
        tests=sub["container.springboot"].tests + sub["container.jetty"].tests,
        seconds=sub["container.springboot"].seconds + sub["container.jetty"].seconds,
    )
    return core, container, sub


def bar(seconds: float, max_seconds: float, width: int = 20) -> str:
    if max_seconds <= 0:
        return ""
    filled = int((seconds / max_seconds) * width)
    filled = max(1 if seconds > 0 else 0, min(width, filled))
    return "▓" * filled


def main() -> int:
    parser = argparse.ArgumentParser(description="Aggregate Surefire reports by execution bucket.")
    parser.add_argument("module", default="juneau-integration-tests", nargs="?", help="Module path (default: juneau-integration-tests)")
    args = parser.parse_args()

    repo_root = Path(__file__).resolve().parent.parent
    module_dir = repo_root / args.module
    reports_root = module_dir / "target" / "surefire-reports"

    core_dir = reports_root / "core"
    container_dir = reports_root / "container"
    if core_dir.exists() or container_dir.exists():
        core = parse_xml_dir(core_dir)
        container = parse_xml_dir(container_dir)
        sub = parse_container_subbuckets(module_dir, container_dir)
    else:
        core, container, sub = parse_flat_reports(module_dir, reports_root)

    rows = [
        ("core", core.tests, core.seconds),
        ("container.springboot", sub["container.springboot"].tests, sub["container.springboot"].seconds),
        ("container.jetty", sub["container.jetty"].tests, sub["container.jetty"].seconds),
        ("container", container.tests, container.seconds),
    ]
    max_seconds = max((seconds for _, _, seconds in rows), default=0.0)

    print(f"{args.module}/")
    for name, tests, seconds in rows:
        print(f"  {name:<20}: {bar(seconds, max_seconds)} ({tests} tests, {seconds:.3f}s)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
