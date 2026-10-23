# verify-docs.py

Documentation-integrity checker: verifies that the topic pages, their frontmatter, `sidebars.ts` and the links between pages agree with one another. Standard library only; it does not invoke Docusaurus.

## When to use

- After adding, renaming, moving or deleting a topic page, or editing `sidebars.ts`.
- As part of a full build: `build-docs.py` runs it in its Verify stage and fails the build on errors.
- For links from Juneau source and Javadoc to topics, use `check-topic-links.py`. For the module list on the About page, use `check-doc-inventory.py`.

## Usage

```bash
python3 scripts/verify-docs.py
python3 scripts/verify-docs.py --strict --anchors
python3 scripts/verify-docs.py --only links,imports
python3 scripts/verify-docs.py --skip 10,15 --json
```

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--strict` | none | off | Warnings also make the exit code non-zero. |
| `--anchors` | none | off | Enable check 14 (anchor-target resolution). |
| `--json` | none | off | Print machine-readable JSON instead of the text report. |
| `--only` | comma-separated numbers or names | all checks | Run only these checks. |
| `--skip` | comma-separated numbers or names | none | Skip these checks. |
| `--docs-dir` | directory | parent of `scripts/` | Docs root to check. |

## What it does

Topic pages live in `pages/topics/*.md`. Each needs frontmatter with a `title` and a `slug` (an `id` is optional; the effective id is the `id` when present, else the file name without `.md`). File names look like `NN(.NN)*.PascalName.md`; landing pages (the target of a sidebar category link) carry a trailing `.00`. Sidebars reference pages as `topics/<id>`, and body links use `/docs/topics/<slug>`.

The checks, in reporting order (errors unless marked warning):

| # | Name | Verifies |
|---|---|---|
| 1 | `frontmatter` | `title` and `slug` present and non-empty. |
| 2 | `slug-filename` | The slug equals the PascalCase name part of the file name. |
| 3 | `filename-format` | The file name matches `^\d\d(\.\d\d)*\.[A-Z][A-Za-z0-9]*\.md$`. |
| 4 | `unique-ids` | Slugs and effective ids are unique. |
| 5 | `landing-00` | Category link targets end in `.00` and leaf pages do not. |
| 6 | `dangling` | Every sidebar `topics/<id>` resolves to a file. |
| 7 | `orphans` | Every topic file is referenced in `sidebars.ts`, except ids listed in `scripts/verify-docs-allowlist.txt`. |
| 8 | `counts` | File count versus distinct sidebar references. |
| 9 | `order` | Sidebar depth-first order equals directory order. |
| 10 | `labels` | The sidebar's dotted-number label matches the target file prefix (warning). |
| 11 | `links` | `/docs/topics/<slug>` and bare relative links resolve. |
| 12 | `imports` | `@site/pages/topics/<file>` imports resolve. |
| 13 | `javadoc` | `/site/apidocs/...` link count and format sanity (warning). |
| 14 | `anchors` | `/docs/topics/<slug>#<fragment>` heading targets exist; only with `--anchors` (warning). |
| 15 | `hygiene` | Duplicate titles, near-empty pages, stray files (warning). |

## Output and exit codes

A text report grouped by severity (or JSON with `--json`) on stdout, ending with counts. Nothing is written.

| Code | Meaning |
|---|---|
| 0 | No errors (and no warnings under `--strict`). |
| 1 | At least one error, or a warning under `--strict`. |
| 2 | `pages/topics` was not found, or `--only`/`--skip` named an unknown check. |

## Gotchas

- Only topic pages are checked, not release notes or the developer-info pages.
- Ordering and landing-page rules are strict by design; renaming a page usually means renumbering its siblings.
- `--only` and `--skip` accept either the number or the name from the table.

## See also

- `build-docs.py`, `check-topic-links.py`, `check-doc-inventory.py`, `normalize-headings.py`
