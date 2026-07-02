#!/usr/bin/env python3
"""
Heading normalization script for Juneau Docs pages/topics/*.md files.

Performs 4 passes:
  2a: Remove body-level H1 lines (# Heading)
  2b: Shift headings so minimum is H2
  2c: Demote H5/H6 to H4
  2d: Verify no skipped levels (promote H4 to H3 when no H3 exists between H2 and H4)
"""

import os
import re
import sys
from pathlib import Path


def is_heading_line(line):
    """Return (level, text) if line is a heading at column 0, else None."""
    m = re.match(r'^(#{1,6}) (.+)', line)
    if m:
        return len(m.group(1)), m.group(2)
    return None


def get_heading_lines(lines):
    """
    Return list of (index, level, text) for all heading lines,
    excluding those inside fenced code blocks.
    """
    results = []
    in_fence = False
    fence_marker = None
    for i, line in enumerate(lines):
        stripped = line.rstrip('\n')
        # Track fenced code blocks
        fence_match = re.match(r'^(`{3,}|~{3,})', stripped)
        if fence_match:
            marker = fence_match.group(1)[0]
            marker_len = len(fence_match.group(1))
            if not in_fence:
                in_fence = True
                fence_marker = (marker, marker_len)
            elif fence_marker and marker == fence_marker[0] and marker_len >= fence_marker[1]:
                in_fence = False
                fence_marker = None
            continue
        if in_fence:
            continue
        h = is_heading_line(stripped)
        if h:
            results.append((i, h[0], h[1]))
    return results


def process_file(filepath):
    """
    Process a single .md file. Returns (changed, stats_before, stats_after).
    stats = dict with keys 1..6 -> count of headings at each level.
    """
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    # Compute before stats
    heading_lines_before = get_heading_lines(lines)
    stats_before = {}
    for _, level, _ in heading_lines_before:
        stats_before[level] = stats_before.get(level, 0) + 1

    new_lines = list(lines)
    changes = []

    # --- Pass 2a: Remove body-level H1 lines ---
    heading_info = get_heading_lines(new_lines)
    h1_indices = [i for i, level, _ in heading_info if level == 1]
    if h1_indices:
        # Remove H1 lines (in reverse order to keep indices valid)
        for idx in sorted(h1_indices, reverse=True):
            changes.append(f"2a: removed H1 at line {idx+1}")
            del new_lines[idx]

    # --- Pass 2b: Shift headings so minimum is H2 ---
    heading_info = get_heading_lines(new_lines)
    levels_present = [level for _, level, _ in heading_info]
    if levels_present:
        min_level = min(levels_present)
        if min_level != 2:
            shift = min_level - 2  # positive = need to reduce hashes; negative = need more
            # shift > 0: e.g. min is 3 -> shift=1 -> subtract 1 hash (promote)
            # shift < 0: e.g. min is 1 -> but H1 removed in 2a; shouldn't happen
            if shift != 0:
                changes.append(f"2b: shift headings by -{shift} (min was {min_level})")
                in_fence = False
                fence_marker = None
                for i, line in enumerate(new_lines):
                    stripped = line.rstrip('\n')
                    fence_match = re.match(r'^(`{3,}|~{3,})', stripped)
                    if fence_match:
                        marker = fence_match.group(1)[0]
                        marker_len = len(fence_match.group(1))
                        if not in_fence:
                            in_fence = True
                            fence_marker = (marker, marker_len)
                        elif fence_marker and marker == fence_marker[0] and marker_len >= fence_marker[1]:
                            in_fence = False
                            fence_marker = None
                        continue
                    if in_fence:
                        continue
                    h = is_heading_line(stripped)
                    if h:
                        level, text = h
                        new_level = level - shift
                        # Clamp to reasonable range
                        new_level = max(2, min(6, new_level))
                        eol = '\n' if line.endswith('\n') else ''
                        new_lines[i] = '#' * new_level + ' ' + text + eol

    # --- Pass 2c: Demote H5/H6 to H4 ---
    heading_info = get_heading_lines(new_lines)
    h5plus = [(i, level, text) for i, level, text in heading_info if level >= 5]
    if h5plus:
        in_fence = False
        fence_marker = None
        for i, line in enumerate(new_lines):
            stripped = line.rstrip('\n')
            fence_match = re.match(r'^(`{3,}|~{3,})', stripped)
            if fence_match:
                marker = fence_match.group(1)[0]
                marker_len = len(fence_match.group(1))
                if not in_fence:
                    in_fence = True
                    fence_marker = (marker, marker_len)
                elif fence_marker and marker == fence_marker[0] and marker_len >= fence_marker[1]:
                    in_fence = False
                    fence_marker = None
                continue
            if in_fence:
                continue
            h = is_heading_line(stripped)
            if h and h[0] >= 5:
                level, text = h
                eol = '\n' if line.endswith('\n') else ''
                new_lines[i] = '#### ' + text + eol
                changes.append(f"2c: H{level} -> H4 at line {i+1}")

    # --- Pass 2d: Fix skipped levels (e.g. H2 -> H4 with no H3) ---
    # Walk through headings; if we jump from level N to level N+2 or more,
    # promote the deeper heading to N+1
    heading_info = get_heading_lines(new_lines)
    prev_level = 1  # document starts conceptually at H1 (from frontmatter)
    # Build a mapping of line index -> new level for corrections
    corrections = {}
    for i, level, text in heading_info:
        if level > prev_level + 1:
            # Skip detected
            new_level = prev_level + 1
            corrections[i] = new_level
            changes.append(f"2d: H{level} -> H{new_level} at line {i+1} (skip fix)")
            prev_level = new_level
        else:
            prev_level = level

    if corrections:
        for i, new_level in corrections.items():
            line = new_lines[i]
            stripped = line.rstrip('\n')
            h = is_heading_line(stripped)
            if h:
                _, text = h
                eol = '\n' if line.endswith('\n') else ''
                new_lines[i] = '#' * new_level + ' ' + text + eol

    # Compute after stats
    heading_lines_after = get_heading_lines(new_lines)
    stats_after = {}
    for _, level, _ in heading_lines_after:
        stats_after[level] = stats_after.get(level, 0) + 1

    changed = new_lines != lines
    if changed:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.writelines(new_lines)

    return changed, stats_before, stats_after, changes


def main():
    topics_dir = Path('/Users/james.bognar/git/apache/juneau-docs/pages/topics')
    md_files = sorted(topics_dir.glob('**/*.md'))

    total_before = {}
    total_after = {}
    changed_files = []
    files_2a = []
    files_2b = []
    files_2c = []
    files_2d = []

    for filepath in md_files:
        changed, stats_before, stats_after, changes = process_file(filepath)
        for level, count in stats_before.items():
            total_before[level] = total_before.get(level, 0) + count
        for level, count in stats_after.items():
            total_after[level] = total_after.get(level, 0) + count
        if changed:
            changed_files.append(str(filepath.name))
            has_2a = any(c.startswith('2a') for c in changes)
            has_2b = any(c.startswith('2b') for c in changes)
            has_2c = any(c.startswith('2c') for c in changes)
            has_2d = any(c.startswith('2d') for c in changes)
            if has_2a:
                files_2a.append(filepath.name)
            if has_2b:
                files_2b.append(filepath.name)
            if has_2c:
                files_2c.append(filepath.name)
            if has_2d:
                files_2d.append(filepath.name)
            if '--verbose' in sys.argv:
                print(f"\n{filepath.name}:")
                for c in changes:
                    print(f"  {c}")

    print(f"\n=== Heading Normalization Summary ===")
    print(f"Total .md files processed: {len(md_files)}")
    print(f"Files changed: {len(changed_files)}")
    print(f"  2a (H1 removed):   {len(files_2a)} files")
    print(f"  2b (level shift):  {len(files_2b)} files")
    print(f"  2c (H5/H6->H4):   {len(files_2c)} files")
    print(f"  2d (skip fix):     {len(files_2d)} files")

    print(f"\nBefore heading counts:")
    for level in sorted(total_before.keys()):
        print(f"  H{level}: {total_before[level]}")

    print(f"\nAfter heading counts:")
    for level in sorted(total_after.keys()):
        print(f"  H{level}: {total_after[level]}")

    if '--verbose' in sys.argv:
        print(f"\n2a files: {files_2a}")
        print(f"\n2b files: {files_2b}")
        print(f"\n2c files: {files_2c}")
        print(f"\n2d files: {files_2d}")


if __name__ == '__main__':
    main()
