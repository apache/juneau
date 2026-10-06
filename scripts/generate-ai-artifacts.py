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
Generate deterministic AI-consumable knowledge artifacts for Apache Juneau.

Outputs are written to static/ai:
 - manifest.json
 - juneau-knowledge.jsonl
 - taxonomy.json

and the llmstxt.org-style index static/llms.txt (the parent of the output directory).
llms-full.txt is deliberately not generated: the concatenated topics are ~3 MB.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
import sys
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Any


TOPIC_GLOB = "pages/topics/*.md"
RELEASE_NOTES_GLOB = "pages/release-notes/*.md"
SIDEBARS_FILE = "sidebars.ts"
SITE_URL = "https://juneau.apache.org"
LLMS_SUMMARY_MAX = 200
SCHEMA_VERSION = "1.0.0"
REQUIRED_FIELDS = [
    "id",
    "title",
    "summary",
    "description",
    "module",
    "source_type",
    "source_path",
    "version",
    "tags",
    "related_ids",
    "updated_at",
]


@dataclass
class ExtractConfig:
    docs_root: Path
    juneau_root: Path
    output_dir: Path


def parse_args() -> argparse.Namespace:
    script_dir = Path(__file__).resolve().parent
    docs_root = script_dir.parent
    default_juneau_root = docs_root.parent / "juneau"
    default_output_dir = docs_root / "static" / "ai"

    parser = argparse.ArgumentParser(description="Generate AI artifacts for Juneau docs.")
    parser.add_argument("--docs-root", type=Path, default=docs_root)
    parser.add_argument("--juneau-root", type=Path, default=default_juneau_root)
    parser.add_argument("--output-dir", type=Path, default=default_output_dir)
    parser.add_argument("--check", action="store_true", help="Verify generated output is up to date without writing.")
    return parser.parse_args()


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def get_git_commit(repo_root: Path) -> str:
    try:
        result = subprocess.run(
            ["git", "rev-parse", "HEAD"],
            cwd=repo_root,
            capture_output=True,
            text=True,
            check=True,
        )
        return result.stdout.strip()
    except Exception:
        return "unknown"


def get_current_release(juneau_root: Path) -> str:
    script = juneau_root / "scripts" / "current-release.py"
    if not script.exists():
        return "unknown"
    try:
        result = subprocess.run(
            [sys.executable, str(script)],
            cwd=juneau_root,
            capture_output=True,
            text=True,
            check=True,
        )
        return result.stdout.strip() or "unknown"
    except Exception:
        return "unknown"


def parse_frontmatter(content: str) -> dict[str, str]:
    m = re.match(r"^---\n(.*?)\n---\n?", content, re.DOTALL)
    if not m:
        return {}
    frontmatter = {}
    for line in m.group(1).splitlines():
        if ":" not in line:
            continue
        key, value = line.split(":", 1)
        frontmatter[key.strip()] = value.strip().strip("\"'")
    return frontmatter


def remove_frontmatter(content: str) -> str:
    return re.sub(r"^---\n.*?\n---\n?", "", content, count=1, flags=re.DOTALL)


def first_heading(content: str) -> str:
    for line in content.splitlines():
        if line.startswith("# "):
            return line[2:].strip()
    return ""


def first_paragraph(content: str) -> str:
    cleaned = remove_frontmatter(content)
    lines = [ln.rstrip() for ln in cleaned.splitlines()]
    bucket = []
    for line in lines:
        stripped = line.strip()
        if not stripped:
            if bucket:
                break
            continue
        if stripped.startswith("#") or stripped.startswith("```") or stripped.startswith(">"):
            continue
        bucket.append(stripped)
        if len(" ".join(bucket)) > 240:
            break
    return " ".join(bucket).strip()


def short_digest(parts: list[str]) -> str:
    payload = "|".join(parts)
    return hashlib.sha1(payload.encode("utf-8")).hexdigest()[:12]


def build_doc_record(path: Path, docs_root: Path, source_type: str, default_version: str) -> dict[str, Any]:
    content = read_text(path)
    fm = parse_frontmatter(content)
    rel_path = str(path.relative_to(docs_root))
    title = fm.get("title") or first_heading(content) or path.stem
    summary = first_paragraph(content) or title
    description = remove_frontmatter(content).strip()
    if len(description) > 2000:
        description = description[:1997] + "..."

    module = "general"
    if source_type == "topic":
        m = re.match(r"^\d+[a-z]?\.([^.]+)", path.name)
        if m:
            module = m.group(1)
    if source_type == "release-note":
        module = "release-notes"

    version = default_version
    if source_type == "release-note":
        version = path.stem

    return {
        "id": f"{source_type}:{short_digest([rel_path, title])}",
        "title": title,
        "summary": summary,
        "description": description,
        "module": module,
        "source_type": source_type,
        "source_path": rel_path,
        "version": version,
        "tags": [source_type, module],
        "related_ids": [],
        "updated_at": "source-controlled",
    }


def extract_api_metadata_records(juneau_root: Path, default_version: str) -> list[dict[str, Any]]:
    patterns = [
        r"\bsummary\s*\(\)\s*default\s*\"\"",
        r"\bsu\s*\(\)\s*default\s*\"\"",
    ]
    include_roots = [
        juneau_root / "juneau-core",
        juneau_root / "juneau-rest",
    ]

    records = []
    for root in include_roots:
        if not root.exists():
            continue
        for path in sorted(root.rglob("*.java")):
            text = read_text(path)
            if not any(re.search(pattern, text) for pattern in patterns):
                continue
            rel_path = str(path.relative_to(juneau_root))
            symbol = path.stem
            records.append(
                {
                    "id": f"api-metadata:{short_digest([rel_path, symbol])}",
                    "title": f"{symbol} summary/su metadata",
                    "summary": f"{symbol} exposes summary/su metadata fields for concise AI-oriented descriptions.",
                    "description": (
                        "Detected summary()/su() annotation attributes used by Juneau schema and HTTP metadata "
                        "annotations. These fields provide short descriptions intended for compact AI/LLM consumption."
                    ),
                    "module": "api-metadata",
                    "source_type": "api-metadata",
                    "source_path": rel_path,
                    "version": default_version,
                    "tags": ["api-metadata", "summary", "su"],
                    "related_ids": [],
                    "updated_at": "source-controlled",
                }
            )
    return records


def parse_sidebar_tree(sidebars_text: str) -> list[dict[str, Any]]:
    """Tokenize sidebars.ts into a tree of {type,label,id,link,children} dicts (top-level array items)."""
    token_re = re.compile(
        r"(?P<link>\blink\s*:\s*\{)|(?P<open>\{)|(?P<close>\})"
        r"|\b(?P<key>type|label|id)\s*:\s*'(?P<val>(?:[^'\\]|\\.)*)'"
    )
    root: dict[str, Any] = {"children": []}
    stack: list[dict[str, Any]] = [root]
    kinds: list[str] = ["root"]
    for m in token_re.finditer(sidebars_text):
        if m.group("link") or m.group("open"):
            node: dict[str, Any] = {"children": []}
            stack.append(node)
            kinds.append("link" if m.group("link") else "obj")
        elif m.group("close"):
            if len(stack) == 1:
                continue
            node = stack.pop()
            kind = kinds.pop()
            if kind == "link":
                stack[-1]["link"] = node
            else:
                stack[-1]["children"].append(node)
        else:
            stack[-1][m.group("key")] = m.group("val").replace("\\'", "'")
    # The mainSidebar array's single object wrapper is the root's only meaningful content.
    return root["children"]


def find_node(nodes: list[dict[str, Any]], label: str) -> dict[str, Any] | None:
    for n in nodes:
        if n.get("label") == label and n.get("type") == "category":
            return n
        found = find_node(n.get("children", []), label)
        if found:
            return found
    return None


def doc_ids_in_order(node: dict[str, Any]) -> list[str]:
    """Depth-first doc ids for a category: its own link page first, then its items."""
    ids: list[str] = []
    link = node.get("link")
    if link and link.get("id"):
        ids.append(link["id"])
    for child in node.get("children", []):
        if child.get("type") == "category":
            ids.extend(doc_ids_in_order(child))
        elif child.get("id"):
            ids.append(child["id"])
    return ids


def topic_url(record: dict[str, Any], docs_root: Path) -> str:
    fm = parse_frontmatter(read_text(docs_root / record["source_path"]))
    slug = fm.get("slug") or Path(record["source_path"]).stem
    return f"{SITE_URL}/docs/topics/{slug}"


def one_line(text: str, limit: int = LLMS_SUMMARY_MAX) -> str:
    text = re.sub(r"<[^>]+>", "", text)
    text = re.sub(r"\[([^\]]*)\]\([^)]*\)", r"\1", text)
    text = text.replace("**", "")
    text = re.sub(r"\s+", " ", text).strip()
    if len(text) <= limit:
        return text
    cut = text[: limit - 3].rsplit(" ", 1)[0].rstrip(",;:.")
    return cut + "..."


def render_llms_txt(config: ExtractConfig, records: list[dict[str, Any]], current_release: str) -> str:
    docs_root = config.docs_root
    by_id: dict[str, dict[str, Any]] = {}
    for rec in records:
        if rec["source_type"] != "topic":
            continue
        fm = parse_frontmatter(read_text(docs_root / rec["source_path"]))
        stem = Path(rec["source_path"]).stem
        by_id["topics/" + (fm.get("id") or stem)] = rec

    tree = parse_sidebar_tree(read_text(docs_root / SIDEBARS_FILE))
    documentation = find_node(tree, "Documentation")
    if documentation is None:
        raise ValueError("Could not find 'Documentation' category in sidebars.ts")

    lines = [
        "# Apache Juneau",
        "",
        "> Apache Juneau is a Java toolkit for marshalling POJOs to and from many formats (JSON, XML, HTML, "
        "UON, URL-encoding, MessagePack, RDF, CSV, YAML and more), building REST servers and clients, "
        "and writing configurable microservices, built around a common bean-context and annotation model. "
        "This index links to the documentation topics, grouped as in the site sidebar.",
        "",
    ]

    seen: set[str] = set()
    for section in documentation["children"]:
        if section.get("type") != "category":
            continue
        entries = []
        for doc_id in doc_ids_in_order(section):
            rec = by_id.get(doc_id)
            if rec is None or doc_id in seen:
                continue
            seen.add(doc_id)
            summary = one_line(rec["summary"])
            entries.append(f"- [{rec['title']}]({topic_url(rec, docs_root)}): {summary}")
        if entries:
            lines.append(f"## {section['label']}")
            lines.append("")
            lines.extend(entries)
            lines.append("")

    migration = next((r for r in records if r["source_path"].endswith("27.V10MigrationGuide.md")), None)
    lines.append("## Resources")
    lines.append("")
    lines.append(f"- [Release notes {current_release}]({SITE_URL}/docs/release-notes/{current_release}): "
                 "What changed in the current release.")
    if migration:
        lines.append(f"- [{migration['title']}]({topic_url(migration, docs_root)}): "
                     "Upgrading from Juneau 9.x to 10.0.")
    lines.append(f"- [Javadocs {current_release}]({SITE_URL}/javadocs/{current_release}/index.html): "
                 "API reference for the current release.")
    lines.append(f"- [Knowledge base (JSONL)]({SITE_URL}/ai/juneau-knowledge.jsonl): "
                 "Machine-readable records of all topics, release notes and API metadata, for RAG pipelines.")
    lines.append(f"- [Knowledge base manifest]({SITE_URL}/ai/manifest.json): "
                 "Version, record count and source commits for the knowledge base.")
    lines.append("")
    return "\n".join(lines)


def build_records(config: ExtractConfig, current_release: str) -> list[dict[str, Any]]:
    records = []
    for path in sorted(config.docs_root.glob(TOPIC_GLOB)):
        records.append(build_doc_record(path, config.docs_root, "topic", current_release))
    for path in sorted(config.docs_root.glob(RELEASE_NOTES_GLOB)):
        records.append(build_doc_record(path, config.docs_root, "release-note", current_release))

    records.extend(extract_api_metadata_records(config.juneau_root, current_release))
    records.sort(key=lambda r: (r["source_type"], r["source_path"], r["id"]))
    return records


def validate_records(records: list[dict[str, Any]]) -> None:
    for record in records:
        missing = [field for field in REQUIRED_FIELDS if field not in record]
        if missing:
            raise ValueError(f"Record missing required fields {missing}: {record.get('id')}")
        if not isinstance(record["tags"], list):
            raise ValueError(f"Record tags must be a list: {record['id']}")
        if not isinstance(record["related_ids"], list):
            raise ValueError(f"Record related_ids must be a list: {record['id']}")


def build_taxonomy(records: list[dict[str, Any]]) -> dict[str, Any]:
    by_module: dict[str, int] = defaultdict(int)
    by_type: dict[str, int] = defaultdict(int)
    for record in records:
        by_module[record["module"]] += 1
        by_type[record["source_type"]] += 1
    return {
        "schema_version": SCHEMA_VERSION,
        "modules": dict(sorted(by_module.items())),
        "source_types": dict(sorted(by_type.items())),
    }


def compute_generated_at(records: list[dict[str, Any]]) -> str:
    digest = short_digest([record["id"] for record in records])
    return f"content-hash:{digest}"


def build_manifest(
    docs_commit: str,
    juneau_commit: str,
    current_release: str,
    records: list[dict[str, Any]],
) -> dict[str, Any]:
    return {
        "schema_version": SCHEMA_VERSION,
        "version": current_release,
        "generated_at": compute_generated_at(records),
        "source_commit": {
            "juneau_docs": docs_commit,
            "juneau": juneau_commit,
        },
        "record_count": len(records),
    }


def render_jsonl(records: list[dict[str, Any]]) -> str:
    return "".join(json.dumps(record, sort_keys=True, ensure_ascii=True) + "\n" for record in records)


def render_json(value: dict[str, Any]) -> str:
    return json.dumps(value, indent=2, sort_keys=True, ensure_ascii=True) + "\n"


def write_if_changed(path: Path, content: str) -> bool:
    existing = path.read_text(encoding="utf-8") if path.exists() else None
    if existing == content:
        return False
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    return True


def main() -> int:
    args = parse_args()
    config = ExtractConfig(
        docs_root=args.docs_root.resolve(),
        juneau_root=args.juneau_root.resolve(),
        output_dir=args.output_dir.resolve(),
    )

    current_release = get_current_release(config.juneau_root)
    docs_commit = get_git_commit(config.docs_root)
    juneau_commit = get_git_commit(config.juneau_root)

    records = build_records(config, current_release)
    validate_records(records)
    taxonomy = build_taxonomy(records)
    manifest = build_manifest(docs_commit, juneau_commit, current_release, records)

    outputs = {
        config.output_dir / "juneau-knowledge.jsonl": render_jsonl(records),
        config.output_dir / "taxonomy.json": render_json(taxonomy),
        config.output_dir / "manifest.json": render_json(manifest),
        config.output_dir.parent / "llms.txt": render_llms_txt(config, records, current_release),
    }

    if args.check:
        stale = []
        for path, expected in outputs.items():
            actual = path.read_text(encoding="utf-8") if path.exists() else None
            if actual != expected:
                stale.append(path)
        if stale:
            print("ERROR: AI artifacts are stale. Regenerate with:")
            print("  python3 scripts/generate-ai-artifacts.py")
            for path in stale:
                print(f"  - {path}")
            return 1
        print("AI artifacts are up to date.")
        return 0

    changed = 0
    for path, content in outputs.items():
        if write_if_changed(path, content):
            changed += 1
            print(f"Updated {path}")
        else:
            print(f"No change {path}")

    print(f"Done. {changed} file(s) updated.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
