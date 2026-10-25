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
Mechanical bulk URL/text substitution pass for the docs pages, driven by renames-table.json.

Full docs: apply-renames.md
"""

import argparse
import json
import os
import re
import sys

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
DEFAULT_DOCS_ROOT = os.path.dirname(SCRIPT_DIR)  # juneau-docs/
DEFAULT_TABLE = os.path.join(SCRIPT_DIR, "renames-table.json")


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def load_rules(path):
    with open(path, "r", encoding="utf-8") as fh:
        raw = json.load(fh)
    compiled = []
    for rule in raw:
        comment = rule.get("comment", "")
        replacement = rule["replacement"]
        if rule["is_regex"]:
            compiled.append((re.compile(rule["pattern"]), replacement, comment, True))
        else:
            compiled.append((rule["pattern"], replacement, comment, False))
    return compiled


def mask_code(text):
    """
    Return (masked_text, restore_map) where every code-fence block and every
    inline-code span has been replaced with a placeholder token that contains
    no characters that would match the rename patterns.

    Exception: inline-code spans that appear inside a markdown link label
    [` ... `](url) are NOT masked — we DO want to rewrite test class names
    that appear as backtick-formatted link labels.

    The restore_map is a list of (placeholder, original) pairs to undo masking.
    """
    restore_map = []
    counter = [0]

    def make_placeholder(original):
        token = f"\x00MASKED_{counter[0]}\x00"
        counter[0] += 1
        restore_map.append((token, original))
        return token

    # Mask fenced code blocks first (``` ... ```)
    def mask_fence(m):
        return make_placeholder(m.group(0))

    text = re.sub(r"```.*?```", mask_fence, text, flags=re.DOTALL)

    # Mask inline code spans (` ... `)  — non-greedy, single-line.
    # BUT: if the inline code is the sole content of a link label [` ... `],
    # do NOT mask it — those labels should be rewritten alongside their URLs.
    # Strategy: first mark link-label inline code as temporarily exempt,
    # mask the rest, then restore the exempt ones.

    # Step 1: temporarily replace [` ... `] link-label backtick spans
    # with a sentinel form that won't be matched by the general inline mask.
    link_label_spans = {}
    link_sentinel_counter = [0]

    def exempt_link_label(m):
        sentinel = f"\x00LINKLABEL_{link_sentinel_counter[0]}\x00"
        link_sentinel_counter[0] += 1
        # Store only the inner backtick span (group 1), not the outer brackets.
        # Restore will reconstruct [inner] from [sentinel].
        link_label_spans[sentinel] = m.group(1)
        return f"[{sentinel}]"

    # Match [` ... `] — link label that is exactly one backtick span
    text = re.sub(r"\[(`[^`\n]+`)\]", exempt_link_label, text)

    # Step 2: mask all remaining inline code spans
    def mask_inline(m):
        return make_placeholder(m.group(0))

    text = re.sub(r"`[^`\n]+`", mask_inline, text)

    # Step 3: restore the exempt link labels (now substitutions can apply to them)
    # Restore by replacing [sentinel] → [inner_content]
    for sentinel, inner in link_label_spans.items():
        text = text.replace(f"[{sentinel}]", f"[{inner}]")

    return text, restore_map


def restore_code(text, restore_map):
    for placeholder, original in reversed(restore_map):
        text = text.replace(placeholder, original)
    return text


def apply_rules_to_text(text, rules):
    """Apply all rules to text, returning (new_text, total_changes)."""
    total = 0
    for pattern, replacement, _comment, is_regex in rules:
        if is_regex:
            new_text, n = pattern.subn(replacement, text)
        else:
            # Literal replacement — count occurrences then replace
            n = text.count(pattern)
            new_text = text.replace(pattern, replacement)
        total += n
        text = new_text
    return text, total


def target_files(docs_root):
    """Every *.md under pages/, every *.md, *.mdx and *.tsx under src/pages/ (the site pages that link
    /docs/topics/...), and README.md when present, sorted."""
    files = []
    for base, suffixes in (("pages", (".md",)), (os.path.join("src", "pages"), (".md", ".mdx", ".tsx"))):
        for dirpath, _dirnames, filenames in os.walk(os.path.join(docs_root, base)):
            for fn in filenames:
                if fn.endswith(suffixes):
                    files.append(os.path.join(dirpath, fn))
    readme = os.path.join(docs_root, "README.md")
    if os.path.isfile(readme):
        files.append(readme)
    return sorted(files)


def is_historical(path, pages_dir):
    """Release notes other than 10.x are historical and never rewritten."""
    rel_pages = os.path.relpath(path, pages_dir)
    return rel_pages.startswith("release-notes" + os.sep) and not os.path.basename(path).startswith("10.")


def process_file(path, rules, dry_run=False):
    """
    Read, apply rules (skipping code regions), write back if changed (unless dry_run).
    Returns (changed: bool, n_substitutions: int).
    """
    try:
        with open(path, "r", encoding="utf-8") as fh:
            original = fh.read()
    except UnicodeDecodeError:
        print(f"  WARNING: cannot decode {path} as UTF-8 — skipping", file=sys.stderr)
        return False, 0

    masked, restore_map = mask_code(original)
    updated, n = apply_rules_to_text(masked, rules)
    restored = restore_code(updated, restore_map)

    if restored == original:
        return False, 0

    if not dry_run:
        with open(path, "w", encoding="utf-8") as fh:
            fh.write(restored)
    return True, n


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def parse_args(argv=None):
    parser = argparse.ArgumentParser(description="Bulk URL/text substitution pass across the docs pages.")
    parser.add_argument("--table", default=DEFAULT_TABLE,
                        help="Rules file (default: renames-table.json beside this script).")
    parser.add_argument("--dry-run", action="store_true", help="Report what would change; write nothing.")
    parser.add_argument("--docs-dir", default=DEFAULT_DOCS_ROOT,
                        help="Docs root (default: the parent of scripts/).")
    return parser.parse_args(argv)


def main(argv=None):
    args = parse_args(argv)
    docs_root = os.path.abspath(args.docs_dir)
    pages_dir = os.path.join(docs_root, "pages")
    if not os.path.isfile(args.table):
        print(f"ERROR: rules table not found: {args.table}", file=sys.stderr)
        return 2
    if not os.path.isdir(pages_dir):
        print(f"ERROR: pages directory not found: {pages_dir}", file=sys.stderr)
        return 2

    rules = load_rules(args.table)
    print(f"Loaded {len(rules)} substitution rules from {args.table}")

    changed_files = []
    skipped_historical = []
    total_subs = 0

    for path in target_files(docs_root):
        if is_historical(path, pages_dir):
            skipped_historical.append(path)
            continue

        changed, n = process_file(path, rules, args.dry_run)
        if changed:
            changed_files.append((os.path.relpath(path, docs_root), n))
            total_subs += n

    # Summary
    print()
    print("=" * 72)
    print("SUBSTITUTION SUMMARY" + (" (dry run, nothing written)" if args.dry_run else ""))
    print("=" * 72)
    if changed_files:
        verb = "would change" if args.dry_run else "modified"
        print(f"\nFiles {verb} ({len(changed_files)}):")
        for rel, n in changed_files:
            print(f"  {n:4d}  {rel}")
    else:
        print("\n  (no files modified)")

    print(f"\nTotal substitutions: {total_subs}")
    print(f"Historical release-notes skipped: {len(skipped_historical)}")
    print()
    return 0


if __name__ == "__main__":
    sys.exit(main())
