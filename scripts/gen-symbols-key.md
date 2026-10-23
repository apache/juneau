# gen-symbols-key.py

Regenerates `juneau-symbols-key.svg`, the labelled review sheet of every icon glyph, from the icon sprite `juneau-symbols.svg`.

## When to use

- You added, removed or edited a glyph in the sprite and need the key sheet brought back in step.
- You want to check, without writing, that the key sheet is current: use `--check`.

## Usage

```bash
python3 scripts/gen-symbols-key.py
python3 scripts/gen-symbols-key.py --check
```

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--check` | none | off | Do not write. Exit 1 if the key file differs from what would be generated. |

## What it does

1. Locates the repository root by walking up to a directory that holds both `juneau-core` and `juneau-rest` (`repo_root`).
2. Reads the sprite `juneau-rest/juneau-rest-server-views/src/main/resources/org/apache/juneau/views/juneau-symbols.svg`.
3. Collects every `<symbol>...</symbol>` verbatim, in order (`symbols`), and reuses the sprite's own XML declaration and license comment as the header (`license_header`).
4. Lays the glyphs out in a grid of 10 columns with a label under each (`cell`, `render`). The sheet size follows the glyph count.
5. Writes `juneau-symbols-key.svg` beside the sprite, or with `--check` compares the generated text with the file on disk.

## Output and exit codes

Default run: `wrote <path> (<n> bytes)`. With `--check`: `OK: ... is up to date` on success; on failure a `STALE:` message on stderr with the first differing byte offset, both excerpts, and the command to run.

| Code | Meaning |
|---|---|
| 0 | Key file written, or `--check` found it current. |
| 1 | `--check` found the key file stale. |
| non-zero (message on stderr) | Repository root or sprite not found, no `<symbol>` in the sprite, or a symbol without `id` or `viewBox`. |

## Gotchas

- The sprite is the single source of truth. Edit it and rerun this script; do not hand-edit the key file.
- `--check` is stricter than the JUnit staleness test: it also pins the layout arithmetic.
- The key file is never served; it exists for review and hand authoring of the artwork.

## See also

- `SymbolsKey_Staleness_Test`, the JUnit test that checks symbol bodies and viewBoxes
