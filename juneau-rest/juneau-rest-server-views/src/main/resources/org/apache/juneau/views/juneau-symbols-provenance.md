# `juneau-symbols.svg` — provenance manifest

This file pins the **approved artwork** of every glyph in `juneau-symbols.svg`.

Its job is not attribution. All twenty glyphs are Juneau-original, so no third-party attribution is incurred and
neither `LICENSE` nor `NOTICE` names anything on account of this sprite. Its job is to be a **guard**: to make a
future paste of foreign path data over one of these glyphs fail the build until someone deliberately edits this
file. `SymbolSprite_Provenance_Test` reads it and asserts it against the sprite on every default-profile run.

> **If a fingerprint check has failed and you are here to make it pass:** do not update the row until you can say,
> for the glyph in question, what this file's *Authoring rules* say you must be able to say. A fingerprint edit is
> the reviewed act; the test is only the prompt for it.

## What the fingerprint covers

SHA-256, hex, lower case, over the **exact bytes of the whole `<symbol …>…</symbol>` element**, UTF-8, including
the opening tag and therefore including `viewBox`. Whitespace and attribute order are inside the hash. A glyph
normalised to a different `viewBox` is a fingerprint change, deliberately: a glyph's `viewBox` sets the lattice its
stroke centrelines are snapped to (`juneau-icons.js` hard-codes the host at `viewBox="0 0 24 24"`, and the
grid-fit lattice is defined in *host* units), so a foreign modulus arriving unnoticed is exactly the failure this
covers.

## Authoring rules

Every glyph in this sprite is authored from scratch, in this repository, against these rules:

1. **No external artwork is opened.** Not an icon set, not a design-system sheet, not another product's sprite.
   Looking at the *rendered* image of the glyph being replaced is a permitted look-and-behaviour reference;
   reading its coordinate list is not, with or without arithmetic applied. The test to apply: *could you draw this
   from a PNG of it alone?*
2. **`viewBox="0 0 24 24"`, always.** The host `<svg>` is `0 0 24 24` at every call site, so any other modulus
   interposes a scale factor between the author's coordinates and the pixel grid.
3. **Paint is `none` or `currentColor` — never a literal colour.** Hover and disabled tinting is a CSS `color`
   change, and a hard-coded fill silently opts a glyph out of it.
4. **Every stroked path declares `stroke-width` explicitly.** Inheriting it makes the rendered weight depend on
   where the glyph is used.
5. **Draw on the lattice.** Chrome renders these at 16px (`--jc-chrome-glyph-size`) and 12px
   (`--jc-chrome-glyph-size-small`). At 16px one pixel is 1.5 units, so:
   - a **1px stroke** has its centreline on `0.75 + 1.5k` — its edges then land on `1.5k`, the pixel boundaries;
   - a **2px stroke**, and every **filled** edge, lands on `1.5k` directly;
   - **butt caps**, so a stroke's end lands on a boundary rather than half a stroke past it.
   Purely diagonal geometry is exempt and is centred on 12 instead: a 45° edge is anti-aliased at any offset, so
   there is nothing to snap and centring is the better use of the freedom. `chevrondown`, `chevronright` and
   `close` are the three glyphs this applies to.
   Curves — circles, arcs, the gear outline — are snapped where they have axis-aligned tangents (a circle's four
   extremes) and are soft elsewhere by construction. This is stated rather than hidden: it is why `cancel`,
   `new`, `search` and `refresh` measure lower on the crispness metric than the rectilinear glyphs.

## Document family

`csv`, `pdf`, `spreadsheet` and `copy` are **one deliverable drawn from this spec**, not four glyphs each drawn
against its own predecessor. The spec is declared here *before* the drawing, and the four are drawn from it.

| Constant | Value |
|---|---|
| Page frame | axis-aligned rectangle, **12 × 15 units** |
| Corner radius | **0** — square corners |
| Fold treatment | **present**, top-right only: the corner is removed by a 45° chamfer of chord **4.5 units**. **No fold flap** — the two lines that would draw the turned-back corner are omitted; see the note below. |
| Stroke | `stroke="currentColor"`, `stroke-width="1.5"` (1px at 16px), `fill="none"`, `stroke-linecap="butt"`, `stroke-linejoin="miter"` |
| Frame position | top-left at **(5.25, 3.75)** for `csv`, `pdf`, `spreadsheet` |
| Frame `d` | `M5.25 3.75H12.75L17.25 8.25V18.75H5.25Z` — **byte-identical** in those three, asserted |
| Mark cell | the interior region **x ∈ [7.5, 15], y ∈ [9, 16.5]** — below the chamfer, inside the frame, on `1.5k` |
| Mark rhythm | **three rows** at y = 9, 12, 15, each **1.5 units** tall; where a row is subdivided, **three columns** at x = 7.5, 10.5, 13.5, each **1.5 units** wide. The rhythm is a statement about **ink extent**, not about path coordinates, so a filled mark states it directly while a stroked one states it through its centrelines: `spreadsheet`'s grid runs its outer rule down the mark cell's own boundary and its two inner rules down the middle column and middle row. |
| Wordmark | **none.** See below. |
| `copy` | the same frame — same 12 × 15, same radius 0, same 4.5 chamfer, same stroke — drawn **twice**: front page top-left at **(3.75, 5.25)**, back page offset **(+3, −3)** from it and drawn as its visible edges only. Only the position differs from the constants above; the frame shape does not. |

**The wordmark is deliberately absent, and this is a visual change from the artwork being replaced.** The incumbent
`csv`, `pdf` and `spreadsheet` each carried a three-letter wordmark inside the frame. At the size these are
actually rendered the frame's interior is 6 × 8 px, so three letters get ~2px of width each and resolve to a grey
smear rather than letterforms — measured, not asserted: on the render harness's *mush* metric (share of the box in
the ambiguous 60..215 luminance band) the incumbent set was the three worst glyphs in the sprite at 12px, at
54.86% (`csv`), 54.17% (`pdf`) and 62.50% (`spreadsheet`). A single large capital was drawn and measured first and
was rejected too: at 4 × 4 px against a 1px frame 3 units away, a `C` and a `P` read as brackets, not letters.

So the members are distinguished **structurally** instead, on the declared mark rhythm, which resolves to whole
crisp pixels at 16px:

| Member | Interior mark | What it says |
|---|---|---|
| `csv` | a **3 × 3 grid of filled 1px cells** on the declared rhythm | discrete delimited values |
| `pdf` | **three filled rules**, full width except the last | running text |
| `spreadsheet` | a **stroked 2 × 2 grid** | ruled cells |
| `copy` | none — the second page is the distinction | — |

## Glyphs

`origin` is `juneau-original` for all twenty: eighteen redrawn from scratch under `READY-J0451`, and `check` and
`collapse_all`, which were already Juneau-original and are **byte-unchanged** by that work.

| Stem | Origin | Fingerprint (SHA-256 of the `<symbol>` element) |
|---|---|---|
| `cancel` | `juneau-original` | `a9290f19a96fb76f21ee0e7c18e8a4a86a47789cb313bfebfba5ad23f81968a8` |
| `check` | `juneau-original` | `eb29de8eb151d13bac83a41a67d5e98e6c932cf8476b5e24168e2fc1b94da6e8` |
| `chevrondown` | `juneau-original` | `0cff710f91e66490b2e0f9bcea8586ebe0b7358e55d58804e597189a7a28f05d` |
| `chevronright` | `juneau-original` | `f88f4812f45ed5aec6a7f1b54e986f941b7f90ded7e92e8b0ebb9b4e2ba4d8dd` |
| `close` | `juneau-original` | `fdf4be3a7918635703619708ad86d163f824f21ff4f664defd721750c7aa3fee` |
| `collapse_all` | `juneau-original` | `3567662a623db15fcb907156f14713566f0652f7a8649815c5e137cbffe391de` |
| `columns` | `juneau-original` | `0300d7ab7052f9079fea4eb2faa7e11663db7bbbf27a16d257f5d4edf46818f1` |
| `copy` | `juneau-original` | `746c5e8bc1ee9f7ba2e64baf09054f10e39b1be1efc0c2a9a16642942e537ace` |
| `csv` | `juneau-original` | `6abb9ea47097741d48a56e194aa5d068ba296daa3a8950b541ee2658a98e2246` |
| `download` | `juneau-original` | `cc9b5776f4ad7f1302c7af2e1ab71b34f994073ca615be3b19726ae10b4e3bf9` |
| `edit` | `juneau-original` | `4d89fef533d8930afaa124dbd73ed34f3dc8fcf542e624d3e589bdda4b91fc3d` |
| `filter` | `juneau-original` | `498dd4f95d4a0d1c3208098bac405b6ff5e6b46ce132a418afc0c3f813ff3847` |
| `new` | `juneau-original` | `703a4403c820d2d6b6a21e578eaeb410c262572ea0d0f60aef9ffef7bb726754` |
| `pdf` | `juneau-original` | `a1cdef16b4c57ff0d9da9af5d3fd93416bc8752fdd5388345d601986d4c7667a` |
| `refresh` | `juneau-original` | `33073e39fa8e58ab295b13dc18a8b7c28cbc7bf3c91ddb6ccc8290cb45d720cc` |
| `search` | `juneau-original` | `03f04247d886a8f95fbf48e21968957c7115b002d770f6d67b39b2ea0296ab7f` |
| `settings` | `juneau-original` | `d1fa0d3ebe2c1638425c35593379320634d11ae83a58c5b415d9e4d4fc12bc2a` |
| `spreadsheet` | `juneau-original` | `3da479a6614c03da0adf222bb9f52f63ce423fe5f191a5922284c6305beec7b8` |
| `toggle-deleted` | `juneau-original` | `2c12c07f4f1208ff61abb62a198f73d32315a168007c01bf6dd7224a6ca2f349` |
| `toggle_column_search` | `juneau-original` | `c1a6ef076a8d226f544402edd862e868ea51909fed52f58dea345e56e2eba3b5` |

## Composition notes

What each glyph is, as a construction. These are the design briefs the artwork was drawn from, and they are
recorded because the useful question about any of these glyphs later is "what was it meant to be", not "what are
its coordinates".

| Stem | Construction |
|---|---|
| `cancel` | circle r=7.5 about (11.25, 11.25), plus a diagonal cross inscribed in it. |
| `check` | unchanged. |
| `chevrondown` | one 90° polyline, 15 units wide by 7.5 tall, apex down, centred on 12. |
| `chevronright` | the same polyline turned a quarter turn, apex right. |
| `close` | two full-width diagonals crossing at the centre. |
| `collapse_all` | unchanged. |
| `columns` | three filled bars, 3 units wide on a 4.5-unit pitch, 15 tall. **Drawn to its name — see the note below.** |
| `copy` | two pages per the family spec, the back one offset (+3, −3) and clipped to its visible edges. |
| `csv` | family frame, plus a 3 × 3 grid of filled 1px cells on the declared mark rhythm. |
| `download` | a vertical shaft, a 90° arrowhead at its foot, and a detached tray below. |
| `edit` | a 45° pencil: a parallelogram body, a symmetric point at the lower left, and a ferrule line across the body 3 units back from the cap. |
| `filter` | a funnel: a full-width mouth, two symmetric slopes to a 3-unit throat, and an offset spout. |
| `new` | the `cancel` circle with an inscribed plus instead of a cross. |
| `pdf` | family frame, plus three filled rules on the declared mark rhythm, the last one short. |
| `refresh` | two 150° arcs of r=7.5 in 180° rotational symmetry, with a 30° gap at each end, each terminating in a filled triangular arrowhead whose base is radial and whose apex points along the direction of travel. |
| `search` | circle r=6 about (9.75, 9.75) with a 45° handle from its lower-right quadrant. |
| `settings` | a filled 6-tooth gear about (11.25, 11.25): outer radius 7.5, root radius 5.25, tooth half-angles 13°/22°, with an `evenodd` hole of r=2.25 knocked out of the centre. |
| `spreadsheet` | family frame, plus a stroked 2 × 2 grid on the declared mark rhythm. |
| `toggle-deleted` | the `cancel` construction at r=9. |
| `toggle_column_search` | the `search` construction at r=4.5 — deliberately the same idiom at a smaller weight, which is the relationship the two glyphs had before. |

### `columns` is drawn to its name

The artwork this replaced was a **cog** — the same composition as `settings`, so the two toolbar buttons were
visually identical and neither told the user which one it was. `columns` is now three vertical bars. This is a
deliberate behaviour change, not a side effect of the redraw, and it is the one glyph in this sprite whose meaning
moved. It is required to be distinguishable at 16px from both `settings` and `spreadsheet`; the render harness
renders both pairs side by side under that name for exactly this check.
