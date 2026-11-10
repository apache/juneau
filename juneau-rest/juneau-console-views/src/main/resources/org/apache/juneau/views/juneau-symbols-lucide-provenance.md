# `juneau-symbols-lucide.svg` - provenance

A default-loaded base icon layer: Lucide glyphs under the shipped Juneau-original sprite. A stem in
`juneau-symbols.svg` or an app override always wins over the same stem here. It is a sibling of
`juneau-symbols.svg` and `juneau-symbols-material.svg`; never mix these paths into either.

## License

[Lucide](https://github.com/lucide-icons/lucide) is ISC licensed. A subset of its glyphs is derived from
Feather and is MIT licensed (Cole Bemis). None of the glyphs below is on the package's Feather-derived list, so
every row is ISC; a Feather-derived glyph added later must be recorded as MIT. The license texts are in the
repo-root `LICENSE` ("Bundled components").

## Source

Pinned release: `v1.70.0` (npm package `lucide-static@1.70.0`), retrieved 2026-11-09. Each glyph is the body of
`package/icons/<name>.svg` wrapped as `<symbol id="juneau-sym-<stem>" viewBox="0 0 24 24" fill="none">` with the
stroke attributes (`currentColor`, width 2, round caps and joins) on an inner group, so they do not depend on the
host. Hyphens in the upstream name become underscores in the stem. No upstream names differed in this release.

| stem | upstream path | license |
|---|---|---|
| circle_check | icons/circle-check.svg | ISC |
| hourglass | icons/hourglass.svg | ISC |
| tag | icons/tag.svg | ISC |
| paw_print | icons/paw-print.svg | ISC |
| bird | icons/bird.svg | ISC |
| fish | icons/fish.svg | ISC |
| shapes | icons/shapes.svg | ISC |
| badge_check | icons/badge-check.svg | ISC |
| truck | icons/truck.svg | ISC |
| package | icons/package.svg | ISC |
| shopping_cart | icons/shopping-cart.svg | ISC |
| user_check | icons/user-check.svg | ISC |
| user_x | icons/user-x.svg | ISC |
| store | icons/store.svg | ISC |
| chart_line | icons/chart-line.svg | ISC |
| chart_column | icons/chart-column.svg | ISC |
| trending_up | icons/trending-up.svg | ISC |

## Authoring rules (this sprite only)

1. Upstream-licensed ISC or MIT glyphs only, one visual style, no brand logos.
2. Every `<symbol id>` has a row in the table above (checked by `LucideSprite_Provenance_Test`).
3. Do not paste these paths into `juneau-symbols.svg`.
