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
Generates juneau-symbols-key.svg from juneau-symbols.svg.

The key file is the human-facing authoring sheet for the icon sprite: it inlines every <symbol> definition from
the sprite into a <defs> block, then lays the glyphs out in a labelled grid so a reviewer can see the whole set
at once.  It is never served (only juneau-symbols.svg is a registered servable resource) - it ships so that the
artwork can be reviewed and hand-authored.

Why this exists rather than hand-editing the key file:

  The key file carries each glyph's viewBox TWICE - once on the inlined <symbol> and again on the per-cell
  wrapper <svg> that <use>s it - so touching one glyph's artwork means four coordinated hand-edits across two
  files, and adding or removing a glyph resizes the whole sheet.  Every one of those is a chance for the two
  files to drift apart silently, because nothing in the build reads the key file at all.

  Everything in the key file except the layout arithmetic is copied verbatim from the sprite, so the sprite is
  the single source of truth and this script is the copy.

Usage:

    python3 scripts/gen-symbols-key.py            # rewrite the key file from the sprite
    python3 scripts/gen-symbols-key.py --check    # exit 1 if the key file is not what this script would write

--check is the staleness gate.  It is strictly stronger than the accompanying JUnit test
(SymbolsKey_Staleness_Test), which asserts only that the two files' <symbol> bodies and their per-cell wrapper
viewBoxes agree: --check additionally pins the generated layout, so a hand-edit to the grid arithmetic or the
sheet size fails here while passing there.
"""

import argparse
import re
import sys
from pathlib import Path

# Sprite and key file, relative to the repository root.
VIEWS_RESOURCES = Path("juneau-rest/juneau-rest-server-views/src/main/resources/org/apache/juneau/views")
SPRITE = VIEWS_RESOURCES / "juneau-symbols.svg"
KEY = VIEWS_RESOURCES / "juneau-symbols-key.svg"

# Sheet layout, in the key file's own user units.  These reproduce the hand-authored sheet this script replaced.
MARGIN = 16          # outer margin on all four sides
TITLE_Y = 28         # baseline of the sheet title
TITLE_SIZE = 16
GRID_TOP = 52        # top of the first row of cells
COLS = 10            # cells per row
CELL_W = 100
CELL_H = 72
GLYPH = 28           # rendered size of one glyph inside its cell
GLYPH_TOP = 8        # glyph offset below the top of its cell
LABEL_BASELINE = 60  # label baseline below the top of its cell
LABEL_SIZE = 10
# A label longer than this many characters does not fit a cell at LABEL_SIZE, so it drops a step.
LABEL_LONG = 14
LABEL_SIZE_LONG = 8

CELL_STROKE = "#d8dde6"
INK = "#16325c"
PAPER = "#ffffff"
TITLE = "Juneau symbols"

SYMBOL_RE = re.compile(r"<symbol\s.*?</symbol>", re.S)
SYMBOL_ID_RE = re.compile(r'<symbol\s+id="([^"]+)"')
SYMBOL_VIEWBOX_RE = re.compile(r'<symbol\s+[^>]*viewBox="([^"]+)"')


def repo_root(start):
    """Walks up from `start` to the directory holding both juneau-core and juneau-rest."""
    for d in [start, *start.parents]:
        if (d / "juneau-core").is_dir() and (d / "juneau-rest").is_dir():
            return d
    sys.exit("could not locate the repository root (no ancestor holds both juneau-core and juneau-rest)")


def license_header(sprite_text):
    """The sprite's own XML declaration and ASF comment, up to but excluding its root <svg> element."""
    at = sprite_text.index("<svg ")
    return sprite_text[:at]


def symbols(sprite_text):
    """Every <symbol>...</symbol> element of the sprite, verbatim and in document order."""
    found = SYMBOL_RE.findall(sprite_text)
    if not found:
        sys.exit(f"no <symbol> elements found in {SPRITE}")
    return found


def stem_of(symbol):
    m = SYMBOL_ID_RE.search(symbol)
    if not m:
        sys.exit("a <symbol> element has no id attribute")
    return m.group(1)


def viewbox_of(symbol):
    m = SYMBOL_VIEWBOX_RE.search(symbol)
    if not m:
        sys.exit(f"<symbol id=\"{stem_of(symbol)}\"> has no viewBox attribute")
    return m.group(1)


def label_of(symbol):
    """The cell label: the stem id with the shared juneau-sym- prefix dropped."""
    return stem_of(symbol).removeprefix("juneau-sym-")


def cell(index, symbol):
    """One labelled grid cell: a hairline box, the glyph at GLYPH size, and the stem name under it."""
    col, row = index % COLS, index // COLS
    x, y = MARGIN + col * CELL_W, GRID_TOP + row * CELL_H
    label = label_of(symbol)
    size = LABEL_SIZE_LONG if len(label) > LABEL_LONG else LABEL_SIZE
    return (
        '<g class="cell">'
        f'<rect x="{x}" y="{y}" width="{CELL_W}" height="{CELL_H}" fill="none"'
        f' stroke="{CELL_STROKE}" stroke-width="0.4"/>'
        f'<svg x="{x + (CELL_W - GLYPH) / 2:.1f}" y="{y + GLYPH_TOP}" width="{GLYPH}" height="{GLYPH}"'
        f' viewBox="{viewbox_of(symbol)}"><use href="#{stem_of(symbol)}"/></svg>'
        f'<text x="{x + CELL_W // 2}" y="{y + LABEL_BASELINE}" font-size="{size}">{label}</text>'
        "</g>"
    )


def render(sprite_text):
    """The full key file this script would write for the specified sprite content."""
    syms = symbols(sprite_text)
    rows = -(-len(syms) // COLS)  # ceiling division
    width = 2 * MARGIN + COLS * CELL_W
    height = GRID_TOP + rows * CELL_H + MARGIN
    return (
        license_header(sprite_text)
        + f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}"'
        f' viewBox="0 0 {width} {height}" style="background:{PAPER};color:{INK}">\n'
        + "<defs>\n"
        + "\n".join(syms)
        + "\n</defs>\n"
        + "<style>\n"
        + f"  text {{ font-family: Helvetica, Arial, sans-serif; fill: {INK}; text-anchor: middle; }}\n"
        + f"  .cell use {{ color: {INK}; }}\n"
        + "</style>\n"
        + f'<text x="{width // 2}" y="{TITLE_Y}" font-size="{TITLE_SIZE}" font-weight="600">{TITLE}</text>\n'
        + "".join(cell(i, s) for i, s in enumerate(syms))
        + "\n</svg>\n"
    )


def main():
    p = argparse.ArgumentParser(description="Generate juneau-symbols-key.svg from juneau-symbols.svg.")
    p.add_argument("--check", action="store_true",
                   help="do not write; exit 1 if the key file differs from what would be written")
    args = p.parse_args()

    root = repo_root(Path(__file__).resolve().parent)
    sprite, key = root / SPRITE, root / KEY
    if not sprite.is_file():
        sys.exit(f"sprite not found: {sprite}")

    sprite_text = sprite.read_text(encoding="utf-8")
    want = render(sprite_text)

    if args.check:
        have = key.read_text(encoding="utf-8") if key.is_file() else ""
        if have == want:
            print(f"OK: {KEY} is up to date with {SPRITE.name} ({len(symbols(sprite_text))} glyphs)")
            return 0
        # A byte offset is more useful than a diff here: the two files are one long line for the cell grid, so a
        # unified diff would print the entire grid for a one-character change.
        at = next((i for i, (a, b) in enumerate(zip(have, want)) if a != b), min(len(have), len(want)))
        print(f"STALE: {KEY} does not match what {SPRITE.name} generates.", file=sys.stderr)
        print(f"       first difference at byte {at}", file=sys.stderr)
        print(f"       on disk:   {have[at:at + 80]!r}", file=sys.stderr)
        print(f"       generated: {want[at:at + 80]!r}", file=sys.stderr)
        print(f"       run: python3 scripts/{Path(__file__).name}", file=sys.stderr)
        return 1

    key.write_text(want, encoding="utf-8")
    print(f"wrote {KEY} ({len(want)} bytes)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
