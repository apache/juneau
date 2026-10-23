# cleanup-whitespace.py

Rewrites Java files in place to normalize whitespace: leading spaces to tabs, no trailing spaces, no repeated blank lines, and no final newline.

## When to use

- A one-off whitespace sweep over a source tree.
- It edits files in place with no dry-run, so commit or stage your work first.

## Usage

```bash
python3 scripts/cleanup-whitespace.py
python3 scripts/cleanup-whitespace.py juneau-core/juneau-marshall
```

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `root-dir` | path (optional positional) | repository root | Directory to scan for `.java` files. Only `sys.argv[1]` is read. |

## What it does

1. Takes the directory from the first argument, or the parent of `scripts/` (`main`).
2. Finds every `*.java` file under it, skipping paths with a `target` or `.git` component (`find_java_files`).
3. For each file (`clean_java_file`): converts each group of four leading spaces to a tab, strips trailing whitespace, collapses runs of blank lines to one, drops blank lines before the last `}`, and removes the trailing newline after the final `}`.
4. Writes the file back only when its content changed.
5. Prints the totals.

## Output and exit codes

A `Scanning...` line, the file count, a `Cleaned: <path>` line per modified file, and a summary with scanned, modified and unchanged counts.

| Code | Meaning |
|---|---|
| 0 | The scan finished. Read errors on single files are printed to stderr and the file is counted as unchanged. |

## Gotchas

- Files written by this script end without a newline, which is the repository convention for Java sources.
- The `Summary:` heading is printed from inside the loop body, so it appears once per modified file and not at all when nothing changed.
- It does not honor `.gitignore`; any `*.java` outside `target` and `.git` is rewritten.

## See also

- Docs site: Developer Info, Script: cleanup-whitespace (`04.02.08.ScriptCleanupWhitespace.md` in juneau-docs)
