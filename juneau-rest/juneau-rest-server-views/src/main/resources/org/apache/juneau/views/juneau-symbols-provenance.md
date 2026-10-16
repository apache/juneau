# `juneau-symbols.svg` — provenance manifest

This file pins the **approved artwork** of every glyph in `juneau-symbols.svg` (the shipped console icon pack).

Its job is not only attribution: it is a **guard**. A future silent paste over one of these glyphs fails the build
until someone deliberately edits this file. `SymbolSprite_Provenance_Test` reads it and asserts it against the
sprite on every default-profile run.

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

1. **Console counterparts replace Juneau pixels when they exist** (operator ruling 2026-09-24).
   Where the source console sprite ships a glyph for the same console role and its artwork is cleared for copy,
   that artwork is copied into this sprite under the existing Juneau stem id (`juneau-sym-{stem}`). Origin for those
   rows is `original-artwork`. Artwork clearance only — do **not** copy third-party class names, trademarks as
   branding, proprietary fonts, branding assets, internal URLs, copyright headers, or verbatim proprietary code.
2. **Where the reference sprite has no glyph for that role, keep the Juneau glyph** (`origin` stays
   `juneau-original`). Those glyphs remain authored in this repository against the lattice rules below.
3. **`viewBox="0 0 24 24"`, always.** The host `<svg>` is `0 0 24 24` at every call site.
4. **Paint is `none`, `currentColor`, or a themable `var(--name, currentColor)` — never a literal colour.** Hover
   and disabled tinting is a CSS `color` change, and a hard-coded fill silently opts a glyph out of it. The `var()`
   form (used by `sort` for its two direction triangles) exists because custom properties inherit into a `<use>`
   shadow tree where document selectors cannot reach; its fallback must be `currentColor`, so the glyph defaults to
   the host `color`. The provenance test enforces exactly this rule.
5. **Every stroked path declares `stroke-width` explicitly.** Inheriting it makes the rendered weight depend on
   where the glyph is used.
6. **Juneau-original lattice** (still required for rows that stay `juneau-original`): draw on the 16px/12px chrome
   lattice documented historically for this sprite — 1px strokes on `0.75 + 1.5k`, 2px/filled edges on `1.5k`,
   butt caps. Purely diagonal geometry is exempt and centred on 12. `original-artwork` rows are not re-latticed; they keep
   the copied path data as cleared.

## Document family

`csv`, `pdf`, `spreadsheet`, and `copy` previously shared a Juneau-original page-frame contract. Those four stems
now ship **copied artwork** (`origin` `original-artwork`), so the old byte-identical frame path is **not** asserted across
them. Distinguishing marks come from the copied drawings themselves. Do not reintroduce a Juneau-only frame-identity
assert without also reverting those stems to Juneau-original artwork.

## Glyphs

`origin` is either:

- `juneau-original` — authored in this repository (no reference counterpart for that role, or the reference
  counterpart was already byte-identical so pixels did not change), or
- `original-artwork` — path data copied from the source console symbol sprite for the matching role (operator
  clearance 2026-09-24). Stem names stay Juneau (`search` stays `search`).

Roles the shipped sprite keeps as **Juneau artwork** because the reference draws nothing this sprite can copy —
grouped by *why* (see the trace in *Composition notes*, so a later pass does not re-skip a role that in fact has a
live `<use>` composition):

- **No live reference composition draws the role.** `chevronup` — the reference row-expand swaps
  `chevronright`↔`chevrondown` (never an up chevron), so there is no `<use>` of up to copy; rotating `chevrondown`
  180° would be *inventing* artwork the reference never draws. `sort` — the reference leaves DataTables' own CSS
  `▲`/`▼` marks and only retints the active direction; there is no sort sprite. Per spec U1 `sort` is still a real
  Juneau `<symbol>` (two triangles, each painted `var(--jc-sort-asc-fill|--jc-sort-desc-fill, currentColor)` so
  header CSS can tint the active direction), kept.
- **No reference glyph draws the role.** `more` (row-action overflow, author alias `more_vert`) — the reference
  `action` glyph is a lightning-bolt, a different role. Three filled circles on the `1.5k` lattice, `currentColor`.
- **Truly absent Foundry stems** (the reference chrome has no such control): `forceStop`, `openPr`.
- **Dormant ids from an unrelated sheet** — present in a stock icon sheet the source console never loads (its
  console symbol sprite is the live one), so they are *not* the live reference set and must not be copied: `print`,
  `push`, `stop`. Live cancel/close use `close`/`cancel`; `stop` ≠ the copied `pause`.

| Stem | Origin | Fingerprint (SHA-256 of the `<symbol>` element) |
|---|---|---|
| `cancel` | `original-artwork` | `1142648c3307e16e9b3e6abe9833511bae24d0e1e60b001bea801114cdaa0178` |
| `check` | `juneau-original` | `eb29de8eb151d13bac83a41a67d5e98e6c932cf8476b5e24168e2fc1b94da6e8` |
| `chevrondown` | `original-artwork` | `4c70570f3e42a789bdf3ef5d3b19ffecc20896a8bcace9d0d6d13f27dd687cf1` |
| `chevronleft` | `original-artwork` | `371534cfd3a920b1a71515e7dfe8e93d1786d873b23c845ee4dc5221c8d8d1bc` |
| `chevronright` | `original-artwork` | `d6b741f21c485a16fba7057324d1c284f7fb53008071e4dace18f43e1829fa62` |
| `chevronup` | `juneau-original` | `8bf893c85004a053572ffcf2b7a90550c861d88003ced74e59742ed881bc2ece` |
| `close` | `original-artwork` | `2e796f9ad254be5490a9b0ffe209c92f6b9012c113d22a08ad66a43b2622af7d` |
| `collapse_all` | `juneau-original` | `3567662a623db15fcb907156f14713566f0652f7a8649815c5e137cbffe391de` |
| `columns` | `original-artwork` | `63ce28c108c82ab89697705ebe62c11011e238a8c8b6e52c14b1df52dfdd6621` |
| `copy` | `original-artwork` | `1a7ae713f35b6259d8912fd02b695bae7348798fbf6d9f736817dde0aba23f50` |
| `csv` | `original-artwork` | `1d8eb08aaabb0b8eaf5cefb70b80d7087dae2783a046f7ab9bf6a83e9eb845bf` |
| `download` | `original-artwork` | `42ae2720c4d85efea0034637b2e0aadfe6ab4bb4d21bd859949e9c94af5ac239` |
| `edit` | `original-artwork` | `f1385ef077958b6888186280de8fe61698c07a70d6ec97d1681b46656216e585` |
| `filter` | `original-artwork` | `8fbe460c68daf05bcfe9afc205666d8191e0577ca23b8c3c74b94c0d77dfadd0` |
| `first_page` | `original-artwork` | `14bd5ebadf8d3bcfd0b44f19d9c897d04a8850c5d1c1c5d7169e55b86ccac408` |
| `forceStop` | `juneau-original` | `3732dbae1b32bc6b53144ee03469e87893b6d1d9686c3c7a53720040703458d3` |
| `last_page` | `original-artwork` | `947f6cc47850b1464d85465e214d58cd3a5d4ced84c90028c1780b389534909c` |
| `link` | `juneau-original` | `5117de4b0677d59a14d241db8039890f57fe43dccd0fd82033a2dd9dc5ba2103` |
| `more` | `juneau-original` | `88f14da9fd54c0a9ce3c55e04a63f282f329647e791166f38e89719730ad0182` |
| `new` | `original-artwork` | `5f1bd81e5d644a292343fb94c39260f818e587e124f15dc0117f48cd23101fda` |
| `openPr` | `juneau-original` | `2ab421c35b2e22ce4e50100ead41f183f310fd129b021928b230fa4257683470` |
| `pause` | `original-artwork` | `3d7a1bac4ecf957e146d0e5993aec052413fbf20f27e1ff53a719f1981215552` |
| `pdf` | `original-artwork` | `63a334fc35f36a1a0b0077f308981a2bdca8f675bd33f2ee46a241d85437cb1f` |
| `print` | `juneau-original` | `236eb8a210324db18c969faaf9623765586b84464850f026d2e5918d3650dc9e` |
| `push` | `juneau-original` | `11500ea634ea1ad14cca0b1d0a08973c5d2f0973b7245bea3872114767be0d59` |
| `refresh` | `original-artwork` | `626b5da36e11e99dbb55b62624542d2b874b838a8790aa902b8e693d3c81a81e` |
| `search` | `original-artwork` | `3d095317f5d549458dfe434dfaf7cee17b45712de16ae55048ad57eebe7361e1` |
| `settings` | `original-artwork` | `8a5c58d5e4427c10f45aaedb20898c5e48a3d0b8344e31f1964a3d02b2a23340` |
| `sort` | `juneau-original` | `a64e04a902869a580141b4ed76a3b5360b672bd6645c7bd43004bb2f4d85ac00` |
| `spreadsheet` | `original-artwork` | `e34e54acbddde596256a9b3e7ef38cf22195eb8231482f8475f2f5401db09283` |
| `stop` | `juneau-original` | `dea9a934b0ff9bf814d422fa6288d82486b1fbb6d2c5959585388975067a2605` |
| `toggle-deleted` | `original-artwork` | `1ccd56b9117c71b67e950eafcd42f34c56fe167c63b51b420ea020fcbea8dc0c` |
| `toggle_column_search` | `original-artwork` | `875b83672d60c7cdae200f197850bfbd05e1ded31f0968ed21a918b861fdbbd8` |
## Composition notes

**A `<use>` of a reference symbol — including one reused with rotate / flip / scale, and multi-`<use>`
compositions — *is* the reference artwork.** The reference often draws a chrome role by reusing one on-disk glyph
transformed, rather than by shipping a same-named symbol. The absence of a same-named symbol in the reference
console sprite is therefore **not** evidence that it has no artwork for the role; the composition is. The rows
below bake such compositions into self-contained Juneau `<symbol>`s (real overridable sprite stems per spec U1 — no
runtime CSS transform stand-in), reusing the chevron path already cleared into this sprite as `chevronright`
(origin `original-artwork`).

- **`chevronleft`** (`original-artwork`). The reference has no `chevronleft` on disk: prev/next paging and the
  release-calendar nav both draw the left chevron as the one `chevronright` asset **rotated 180°**
  (`transform="rotate(180 12 12)"`). Baked here as that single rotated chevron path — so Juneau's prev/next pair is
  one mirrored glyph, as the reference intends.
- **`first_page`** (`original-artwork`). The reference "skip to first" is the doubled paging chevron: **two** left
  chevrons (each the `chevronright` asset rotated 180°) overlapped into one glyph. Baked as two copies of the
  rotated chevron path offset `translate(∓3.5 0)` so they overlap ~40%, matching the reference doubled-paging-arrow
  negative-margin overlap (its 12px glyphs overlap 5px ≈ 42%). The overlap offset is a Juneau composition choice;
  the path data is the cleared chevron.
- **`last_page`** (`original-artwork`). Same doubled-chevron composition as `first_page` but **not** rotated — two
  right-pointing `chevronright` chevrons offset `translate(∓3.5 0)`.

These three replaced the previous Juneau-original single/doubled triangle glyphs (operator ruling 2026-09-24:
where the source console sprite draws the role, its artwork replaces the Juneau glyph). The earlier pass skipped
them only because it searched for a same-named `first_page` / `chevronleft` symbol in the source console sprite and
ignored the live `<use>` compositions of its `chevronright` symbol.

Roles kept as Juneau artwork, and why, are listed above under *Glyphs*. `chevronup` and `sort` were checked for a
live reference `<use>` composition (not just a same-named symbol) and have none; the rest are Foundry-absent or
dormant-unrelated-sheet-only.
