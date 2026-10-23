# check-doc-inventory.py

Checks that the list of Juneau modules in `artifact-paths.json` matches the module rows on the public About page, and that `artifact-packages.json` and the allow-list agree with it.

## When to use

- After a module is added, renamed or removed, to see which inventory entries must change.
- Before publishing, as a drift check that needs no build tooling.
- For page frontmatter, sidebars and links, use `verify-docs.py` instead.

## Usage

```bash
python3 scripts/check-doc-inventory.py
python3 scripts/check-doc-inventory.py --strict
python3 scripts/check-doc-inventory.py --json
```

Standard library only; it does not invoke Maven or Docusaurus.

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--strict` | none | off | Warnings also make the exit code non-zero. |
| `--json` | none | off | Print a JSON report (`summary` and `findings`) instead of text. |
| `--docs-dir` | directory | parent of `scripts/` | Docs root to check. |

## What it does

1. Loads `artifact-paths.json`, `artifact-packages.json` and `scripts/check-doc-inventory-allowlist.txt` (one module name per line, `#` comments; missing file means empty).
2. Computes the publishable modules: every key of `artifact-paths.json` minus the hard-coded `AGGREGATOR_POMS` set minus the allow-list.
3. **inventory:** every publishable module must have a `| [module](...) |` row in each page in `INVENTORY_PAGES` (currently `src/pages/about.md`); a missing page or module is an error.
4. **stale-rows:** every module row on an inventory page must be a key of `artifact-paths.json`.
5. **packages:** every `artifact-packages.json` key must be a key of `artifact-paths.json` (error); a publishable module with no packages entry is a warning.
6. **allowlist:** each allow-list entry must exist in `artifact-paths.json` (error), and must not also appear on an inventory page (warning).
7. Prints the findings grouped as errors, warnings and notes, with a final count line.

## Output and exit codes

Text report on stdout (or JSON with `--json`); nothing is written. The text ends with `check-doc-inventory: N errors, M warnings`.

| Code | Meaning |
|---|---|
| 0 | No errors (and no warnings under `--strict`). |
| 1 | At least one error, or a warning under `--strict`. |
| 2 | `artifact-paths.json` or `artifact-packages.json` is missing. |

## Gotchas

- `AGGREGATOR_POMS` and `INVENTORY_PAGES` are in the source; update them when the reactor structure changes.
- Both JSON files are regenerated or refreshed by `build-docs.py`, so run this after a build if you want it to see current modules.
- Only rows shaped like `| [name](...)` count as inventory entries.

## See also

- `verify-docs.py`, `build-docs.py`
