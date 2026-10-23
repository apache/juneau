# normalize-headings.py

Rewrites the heading levels in the topic pages (`pages/topics/**/*.md`) so every page has no body H1, starts at H2, uses nothing deeper than H4, and skips no level.

## When to use

- After importing or converting topic pages whose headings do not follow the site convention (the page title comes from the frontmatter, so the body starts at H2).
- To check page structure without rewriting, use `verify-docs.py`.

## Usage

```bash
python3 scripts/normalize-headings.py
python3 scripts/normalize-headings.py --verbose
```

Review the result with `git diff` afterwards: files are rewritten in place.

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--verbose` | none | off | Print the per-file change list and the file lists for each pass. Read directly from `sys.argv`; the script has no argument parser, so `--help` is not recognised. |

## What it does

1. Takes every `*.md` under the topics directory, sorted by path. The directory is the hard-coded absolute path `/Users/james.bognar/git/apache/juneau-docs/pages/topics` in `main`, not the checkout the script lives in.
2. For each file, ignoring lines inside fenced code blocks (backtick or tilde fences), applies four passes:
   - 2a: removes every body-level H1 line;
   - 2b: shifts all headings so the shallowest is H2 (clamped to H2..H6);
   - 2c: demotes H5 and H6 to H4;
   - 2d: promotes a heading that skips a level (H2 straight to H4) to the next level down from its predecessor.
3. Writes the file back only if the text changed.
4. Prints the summary: files processed and changed, files touched by each pass, and the heading counts per level before and after.

## Output and exit codes

Stdout only. Modified files are rewritten in place.

| Code | Meaning |
|---|---|
| 0 | Completed. |
| 1 | An unhandled error (a traceback), such as an unreadable file. |

## Gotchas

- The topics directory is hard-coded to one machine's checkout path. Run on another machine, or against a worktree, it processes that path (or fails if it does not exist) rather than the current checkout; edit `main` first.
- There is no dry-run mode; rewriting is immediate.
- Only headings at column 0 written as `#` characters are recognised; setext headings are not.

## See also

- `verify-docs.py`, `apply-renames.py`
