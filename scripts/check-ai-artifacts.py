#!/usr/bin/env python3
# ***************************************************************************************************************************
# * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
# * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
# * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
# * with the License.  You may obtain a copy of the License at                                                              *
# *                                                                                                                         *
# *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
# *                                                                                                                         *
# * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
# * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
# * specific language governing permissions and limitations under the License.                                              *
# ***************************************************************************************************************************
"""
Checks AI artifacts freshness and source drift signals.
"""

from __future__ import annotations

import json
import re
import subprocess
import sys
from pathlib import Path


def run(cmd: list[str], cwd: Path) -> subprocess.CompletedProcess[str]:
    return subprocess.run(cmd, cwd=cwd, text=True, capture_output=True)


def check_ai_artifact_freshness(docs_root: Path) -> list[str]:
    result = run(
        [sys.executable, "scripts/generate-ai-artifacts.py", "--check", "--docs-root", str(docs_root)],
        docs_root,
    )
    if result.returncode == 0:
        return []
    errors = ["AI artifact freshness check failed."]
    if result.stdout:
        errors.append(result.stdout.strip())
    if result.stderr:
        errors.append(result.stderr.strip())
    return errors


def check_readme_version(docs_root: Path, release: str) -> list[str]:
    readme = (docs_root / "README.md").read_text(encoding="utf-8")
    m = re.search(r"<version>([^<]+)</version>", readme)
    if not m:
        return ["Could not find Maven <version> in juneau-docs README.md quickstart section."]
    found = m.group(1).strip()
    if found != release:
        return [f"README quickstart version mismatch: expected {release}, found {found}."]
    return []


def check_javadocs_link_consistency(docs_root: Path) -> list[str]:
    config = (docs_root / "docusaurus.config.ts").read_text(encoding="utf-8")
    matches = re.findall(r"href:\s*'pathname:///([^']+)'", config)
    javadocs_links = sorted(link for link in matches if "javadocs" in link or "apidocs" in link)
    unique = sorted(set(javadocs_links))
    if len(unique) <= 1:
        return []
    return [f"Javadocs links are inconsistent in docusaurus.config.ts: {', '.join(unique)}."]


def check_release_note_source_presence(docs_root: Path, juneau_root: Path, release: str) -> list[str]:
    errors = []
    docs_release = docs_root / "pages" / "release-notes" / f"{release}.md"
    if not docs_release.exists():
        errors.append(f"Missing canonical release note page: {docs_release}")

    root_release = juneau_root / "RELEASE-NOTES.txt"
    if not root_release.exists():
        errors.append(f"Missing legacy release-notes file: {root_release}")
    return errors


def check_manifest_shape(docs_root: Path) -> list[str]:
    manifest_path = docs_root / "static" / "ai" / "manifest.json"
    if not manifest_path.exists():
        return [f"Missing manifest: {manifest_path}"]
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    required = {"schema_version", "version", "generated_at", "source_commit", "record_count"}
    missing = sorted(required.difference(manifest.keys()))
    if missing:
        return [f"Manifest missing required fields: {', '.join(missing)}"]
    return []


def resolve_current_release(juneau_root: Path) -> str:
    script = juneau_root / "scripts" / "current-release.py"
    result = run([sys.executable, str(script)], juneau_root)
    if result.returncode != 0:
        raise RuntimeError(result.stderr or "unable to resolve current release")
    return result.stdout.strip()


def main() -> int:
    script_dir = Path(__file__).resolve().parent
    docs_root = script_dir.parent
    juneau_root = (docs_root.parent / "juneau").resolve()

    release = resolve_current_release(juneau_root)
    errors: list[str] = []
    errors.extend(check_ai_artifact_freshness(docs_root))
    errors.extend(check_manifest_shape(docs_root))
    errors.extend(check_readme_version(docs_root, release))
    errors.extend(check_javadocs_link_consistency(docs_root))
    errors.extend(check_release_note_source_presence(docs_root, juneau_root, release))

    if errors:
        print("AI documentation checks failed:")
        for err in errors:
            print(f"- {err}")
        return 1

    print("All AI documentation checks passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
