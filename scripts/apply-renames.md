# apply-renames.py

One-off bulk URL and text substitution pass over the docs: it applies every rule in `renames-table.json` to the topic pages, `README.md` and `src/pages/downloads.md`.

## When to use

- A batch of page files or URLs was renamed and every link to the old names must be rewritten in one sweep.
- You added rules to `renames-table.json` and want them applied everywhere.
- To only find broken links without changing anything, use `check-topic-links.py` or `verify-docs.py` instead.

## Usage

```bash
python3 scripts/apply-renames.py
```

The script takes no arguments and edits files in place, so run it on a clean working tree and review the diff afterwards.

## Options

None.

## What it does

1. Loads `renames-table.json` (`load_rules`). Each rule has `pattern`, `replacement` and `is_regex`; regex patterns are compiled, others are literal. The optional `comment` is ignored at run time.
2. Collects every `*.md` under `pages/` plus `README.md` and `src/pages/downloads.md` (when it exists), sorted by path.
3. Skips the files in the hard-coded `CARVE_OUT_REL` set (pages that a concurrent rename was handling), and every file under `pages/release-notes/` whose name does not start with `10.` (older release notes are historical).
4. For each remaining file (`process_file`), masks fenced code blocks and inline code spans (`mask_code`) so rules cannot touch code. The one exception is an inline-code span that is the whole label of a Markdown link (`` [`Name`](url) ``); those are left unmasked so the label is rewritten along with the URL.
5. Applies the rules in table order (`apply_rules_to_text`), restores the masked code, and writes the file back only if the text changed.
6. Prints the rule count, then a summary: files modified with their substitution counts, the total, and how many carve-out and historical files were skipped.

## Output and exit codes

Stdout only: `Loaded N substitution rules from ...`, then the `SUBSTITUTION SUMMARY` block. Changed files are rewritten in place; nothing else is written.

| Code | Meaning |
|---|---|
| 0 | Completed, whether or not any file changed. |
| 1 | Python traceback: `renames-table.json` is missing or malformed, or a rule lacks a required key. |

A file that is not valid UTF-8 prints a warning on stderr and is skipped.

## Gotchas

- It writes to the working tree immediately; there is no dry-run flag.
- Idempotent: a second run finds nothing left to change as long as no rule's output matches another rule's pattern.
- Rules run in table order against the whole file text, so a later rule sees the output of earlier ones.
- Links inside code fences and inline code are deliberately not rewritten.
- The carve-out list is in the source (`CARVE_OUT_REL`), not in the table.

## See also

- `check-topic-links.py`, `verify-docs.py`
