#!/usr/bin/env python3
"""
apply-renames.py — Mechanical bulk URL/text substitution pass for juneau-docs.

Reads renames-table.json and applies substitutions to:
  - juneau-docs/pages/**/*.md  (excluding historical release-notes pre-10.x)
  - juneau-docs/README.md
  - juneau-docs/src/pages/downloads.md

Rules:
  - Skips code-fence regions (``` blocks) to avoid false positives.
  - Skips inline-code spans (`...`) to avoid false positives.
  - Skips release-notes files that do NOT start with "10." (9.x and older are historical).
  - Is idempotent — safe to re-run.
  - Prints a per-file change count summary at the end.
"""

import json
import os
import re
import sys

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
DOCS_ROOT = os.path.dirname(SCRIPT_DIR)  # juneau-docs/
RENAMES_TABLE = os.path.join(SCRIPT_DIR, "renames-table.json")

# Files/directories to walk
PAGES_DIR = os.path.join(DOCS_ROOT, "pages")
EXTRA_FILES = [
    os.path.join(DOCS_ROOT, "README.md"),
    os.path.join(DOCS_ROOT, "src", "pages", "downloads.md"),
]

# Carve-out: files being renamed/deleted by a concurrent agent — skip entirely
# (relative to PAGES_DIR)
CARVE_OUT_REL = {
    "topics/JuneauPetstoreOverview.md",
    "topics/22.01.V9.0-migration-guide.md",
    "topics/24.01.V10.0-migration-guide.md",
    "topics/03.Module-juneau-marshall-rdf.md",
    "topics/03.01.RdfBasics.md",
    "topics/03.02.RdfSerializers.md",
    "topics/03.03.RdfParsers.md",
    "topics/02.34.BestPractices.md",
    "topics/16.01.MyJettyMicroserviceBasics.md",
    "topics/16.02.MyJettyMicroserviceInstalling.md",
    "topics/16.03.MyJettyMicroserviceRunning.md",
    "topics/16.04.MyJettyMicroserviceBuilding.md",
}


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
    for placeholder, original in restore_map:
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


def process_file(path, rules, carve_out_abs):
    """
    Read, apply rules (skipping code regions), write back if changed.
    Returns (changed: bool, n_substitutions: int).
    """
    if path in carve_out_abs:
        return False, 0

    # Check release-notes skip: only process 10.x files
    rel_pages = os.path.relpath(path, PAGES_DIR)
    if rel_pages.startswith("release-notes" + os.sep):
        fname = os.path.basename(path)
        if not fname.startswith("10."):
            return False, 0

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

    with open(path, "w", encoding="utf-8") as fh:
        fh.write(restored)
    return True, n


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    rules = load_rules(RENAMES_TABLE)
    print(f"Loaded {len(rules)} substitution rules from {RENAMES_TABLE}")

    # Build carve-out absolute paths
    carve_out_abs = set()
    for rel in CARVE_OUT_REL:
        carve_out_abs.add(os.path.join(PAGES_DIR, rel.replace("/", os.sep)))

    # Collect all target files
    target_files = []

    # Walk pages/
    for dirpath, _dirnames, filenames in os.walk(PAGES_DIR):
        for fn in filenames:
            if fn.endswith(".md"):
                target_files.append(os.path.join(dirpath, fn))

    # Extra top-level files
    for extra in EXTRA_FILES:
        if os.path.isfile(extra):
            target_files.append(extra)

    target_files.sort()

    # Process
    changed_files = []
    skipped_carveout = []
    skipped_historical = []
    total_subs = 0

    for path in target_files:
        # Carve-out check
        if path in carve_out_abs:
            skipped_carveout.append(path)
            continue

        # Release-notes historical check
        rel_pages = os.path.relpath(path, PAGES_DIR)
        if rel_pages.startswith("release-notes" + os.sep):
            fname = os.path.basename(path)
            if not fname.startswith("10."):
                skipped_historical.append(path)
                continue

        changed, n = process_file(path, rules, carve_out_abs)
        if changed:
            rel = os.path.relpath(path, DOCS_ROOT)
            changed_files.append((rel, n))
            total_subs += n

    # Summary
    print()
    print("=" * 72)
    print("SUBSTITUTION SUMMARY")
    print("=" * 72)
    if changed_files:
        print(f"\nFiles modified ({len(changed_files)}):")
        for rel, n in changed_files:
            print(f"  {n:4d}  {rel}")
    else:
        print("\n  (no files modified)")

    print(f"\nTotal substitutions: {total_subs}")
    print(f"Carve-out files skipped: {len(skipped_carveout)}")
    print(f"Historical release-notes skipped: {len(skipped_historical)}")
    print()


if __name__ == "__main__":
    main()
