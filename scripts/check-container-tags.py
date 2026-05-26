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
Guardrail for container-backed tests.

Fails when a container-booting test class is missing @SpringbootTest or
@JettyMicroserviceTest.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

SPRING_HINTS = (
    r"@SpringBootTest\b",
    r"@AutoConfigureMockMvc\b",
    r"@ExtendWith\s*\(\s*SpringExtension\.class",
    r"SpringApplication\.run\s*\(",
)

JETTY_HINTS = (
    r"JettyMicroservice\.(?:create|builder)\s*\(",
    r"Microservice\.(?:create|builder)\s*\(",
    r"new\s+org\.eclipse\.jetty\..*?Server\s*\(",
)

TAG_MARKERS = (
    "@SpringbootTest",
    "@JettyMicroserviceTest",
    "@org.apache.juneau.testing.annotations.SpringbootTest",
    "@org.apache.juneau.testing.annotations.JettyMicroserviceTest",
)


def should_require_tag(content: str) -> bool:
    return any(re.search(p, content) for p in (*SPRING_HINTS, *JETTY_HINTS))


def has_container_tag(content: str) -> bool:
    return any(marker in content for marker in TAG_MARKERS)


def scan(repo_root: Path) -> list[Path]:
    test_root = repo_root / "juneau-utest" / "src" / "test" / "java"
    offenders: list[Path] = []
    for path in sorted(test_root.rglob("*Test.java")):
        content = path.read_text(encoding="utf-8")
        if should_require_tag(content) and not has_container_tag(content):
            offenders.append(path)
    return offenders


def main() -> int:
    repo_root = Path(__file__).resolve().parent.parent
    offenders = scan(repo_root)
    if not offenders:
        print("✅ Container tag guard passed.")
        return 0

    print("❌ Container tag guard failed. The following tests appear to boot containers but are missing a tag:")
    for path in offenders:
        rel = path.relative_to(repo_root)
        print(f"  - {rel}")
    print("\nAdd @SpringbootTest or @JettyMicroserviceTest to each class.")
    return 1


if __name__ == "__main__":
    sys.exit(main())
