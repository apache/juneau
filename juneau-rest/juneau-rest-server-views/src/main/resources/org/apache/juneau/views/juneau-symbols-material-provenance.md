# `juneau-symbols-material.svg` — provenance

This is the **opt-in** Material Symbols Outlined sprite. It is a sibling of `juneau-symbols.svg`, not a
replacement: the default pack stays Juneau-original (`juneau-symbols.svg`). Apps select this sprite with
`JuneauViews.icons.pack("material")` or `data-juneau-icon-pack="material"` on the `juneau-icons.js` script tag.

## License

Material Symbols Outlined from [google/material-design-icons](https://github.com/google/material-design-icons),
Apache License 2.0. Product attribution lives in the repo-root `NOTICE` paragraph that names this pack.

## Source

These fourteen glyphs are the **exact path data** that shipped inline in `juneau-icons.js` from commit
`4b56621c3e` until the sprite conversion at `94d4febbdc`. They were recovered from git history of this
repository — not re-downloaded, and not copied from IRS (`irs-icon-overrides.js` / `irs-symbols.svg`) or SLDS.

Stem ids match the hosts `juneau-icons.js` already paints (`juneau-sym-copy`, …) so swapping packs does not
rename registry keys. Glyphs added later as Juneau-original art (`print`, row-action stems, …) are **not** in
this file; they stay on the original sprite.

## Authoring rules (this sprite only)

1. **Do not paste IRS / SLDS / Salesforce artwork** into this file. Dual-hat: `@dual-hat-irs`.
2. **Do not mix these paths into `juneau-symbols.svg`.** That sprite is Juneau-original and is fingerprint-guarded
   by `juneau-symbols-provenance.md`.
3. New Material glyphs may be added only from the same Apache-2.0 upstream (or from this repo's own git history
   of the NOTICE-approved inline set), with a matching `NOTICE` check.
