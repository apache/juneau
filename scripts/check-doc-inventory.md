# check-doc-inventory.py

Checks that the list of Juneau modules in `artifact-paths.json` matches the `modules[]` of the projects in `src/data/projects.ts`, and that `artifact-packages.json` and the allow-list agree with it.

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

1. Loads `artifact-paths.json`, `artifact-packages.json`, the projects from `src/data/projects.ts` (through `project_index.py`) and `scripts/check-doc-inventory-allowlist.txt` (one module name per line, `#` comments; missing file means empty).
2. Computes the publishable modules: every key of `artifact-paths.json` minus the `AGGREGATOR_POMS` set (defined in `project_index.py`) minus the allow-list.
3. **inventory:** every publishable module must be in some project's `modules[]`; a module in none (and not allow-listed) is an error, as is an unreadable `projects.ts`.
4. **stale-rows:** every module in a project's `modules[]` must be a key of `artifact-paths.json`.
5. **packages:** every `artifact-packages.json` key must be a key of `artifact-paths.json` (error); a publishable module with no packages entry is a warning.
6. **allowlist:** each allow-list entry must exist in `artifact-paths.json` (error), and must not also be in a project's `modules[]` (error).
7. Prints the findings grouped as errors, warnings and notes, with a final count line.

## Output and exit codes

Text report on stdout (or JSON with `--json`); nothing is written. The text ends with `check-doc-inventory: N errors, M warnings`.

| Code | Meaning |
|---|---|
| 0 | No errors (and no warnings under `--strict`). |
| 1 | At least one error, or a warning under `--strict`. |
| 2 | `artifact-paths.json` or `artifact-packages.json` is missing. |

## Gotchas

- `AGGREGATOR_POMS` is in `project_index.py`; update it when the reactor structure changes.
- Both JSON files are regenerated or refreshed by `build-docs.py`, so run this after a build if you want it to see current modules.
- `modules[]` entries are read by the strict `projects.ts` parser; a format error in that file is reported as an `inventory` error with its line number.

## See also

- `verify-docs.py`, `build-docs.py`
