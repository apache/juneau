# apply-renames.py

Bulk URL and text substitution pass over the docs: it applies every rule in a renames table (by default `renames-table.json`) to the pages under `pages/`, the site pages under `src/pages/` (`*.md`, `*.mdx`, `*.tsx`) and `README.md`.

## When to use

- A batch of page files or URLs was renamed and every link to the old names must be rewritten in one sweep.
- Pages were folded into their parents and every `/docs/topics/<old slug>` link must point at `/docs/topics/<parent slug>#<anchor>`: write a table for the batch and pass it with `--table`.
- To only find broken links without changing anything, use `check-topic-links.py` or `verify-docs.py` instead.

## Usage

```bash
python3 scripts/apply-renames.py --dry-run
python3 scripts/apply-renames.py
python3 scripts/apply-renames.py --table /path/to/fold-renames.json --dry-run
```

Without `--dry-run` it edits files in place, so review the dry-run list first and the diff afterwards.

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--table` | file | `renames-table.json` beside the script | The rules file to apply. |
| `--dry-run` | | off | Report which files would change and how many substitutions each gets; write nothing. |
| `--docs-dir` | directory | parent of the `scripts/` folder | The docs checkout to rewrite. |

## What it does

1. Loads the rules table (`load_rules`). Each rule has `pattern`, `replacement` and `is_regex`; regex patterns are compiled, others are literal. The optional `comment` is ignored at run time.
2. Collects every `*.md` under `pages/`, every `*.md`, `*.mdx` and `*.tsx` under `src/pages/` (the about, downloads, security, home and 404 pages link `/docs/topics/...`), and `README.md` (when it exists), sorted by path (`target_files`).
3. Skips every file under `pages/release-notes/` whose name does not start with `10.` (older release notes are historical).
4. For each remaining file (`process_file`), masks fenced code blocks and inline code spans (`mask_code`) so rules cannot touch code. The one exception is an inline-code span that is the whole label of a Markdown link (`` [`Name`](url) ``); those are left unmasked so the label is rewritten along with the URL.
5. Applies the rules in table order (`apply_rules_to_text`), restores the masked code, and writes the file back only if the text changed (never with `--dry-run`).
6. Prints the rule count, then a summary: files modified (or that would change) with their substitution counts, the total, and how many historical files were skipped.

A fold rule pair keeps an existing anchor and adds one to a bare link, with a lookahead so a longer slug that starts with the old one is not touched:

```json
[
  {"comment": "fold OldSlug, anchored", "pattern": "/docs/topics/OldSlug#", "replacement": "/docs/topics/Parent#", "is_regex": true},
  {"comment": "fold OldSlug, bare", "pattern": "/docs/topics/OldSlug(?![\\w#-])", "replacement": "/docs/topics/Parent#old-slug", "is_regex": true}
]
```

## Output and exit codes

Stdout only: `Loaded N substitution rules from ...`, then the `SUBSTITUTION SUMMARY` block (marked `(dry run, nothing written)` with `--dry-run`). Changed files are rewritten in place; nothing else is written.

| Code | Meaning |
|---|---|
| 0 | Completed, whether or not any file changed. |
| 1 | Python traceback: the table is malformed, or a rule lacks a required key. |
| 2 | The table file or the `pages/` directory is missing. |

A file that is not valid UTF-8 prints a warning on stderr and is skipped.

## Gotchas

- Idempotent: a second run finds nothing left to change as long as no rule's output matches another rule's pattern.
- Rules run in table order against the whole file text, so a later rule sees the output of earlier ones. Put the anchored rule of a fold pair before the bare one.
- Links inside code fences and inline code are deliberately not rewritten. Only backtick fences are masked; a link inside a `~~~` fence is rewritten.
- It only rewrites this checkout. Javadoc and source links to the docs live in the juneau checkout; `check-topic-links.py` reports them.
- It does not rename files; `renumber-topics.py` does.

## See also

- `check-topic-links.py`, `verify-docs.py`, `renumber-topics.py`
