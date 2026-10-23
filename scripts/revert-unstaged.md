# revert-unstaged.py

Restores one tracked file's working-tree edits to the staged (index) version, keeping what is already staged.

## When to use

- You staged a file, kept editing it, and want to drop only the edits made after staging.
- To restore the committed version instead, use `revert-staged.py`.

## Usage

```bash
python3 scripts/revert-unstaged.py juneau-core/juneau-marshall/src/main/java/org/apache/juneau/ClassMeta.java
```

Run it from the repository root; the path is passed to git as given. For a path that starts with `-`, put `--` first: `python3 scripts/revert-unstaged.py -- -odd.txt`.

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `file_path` | path (positional, required) | none | The file to restore. Exactly one is required; a missing one prints the usage text and exits 2. |
| `-h`, `--help` | none | off | Print the usage text and exit 0; no git command runs. |

## What it does

1. Parses the arguments (`main`).
2. Checks that the path exists on disk.
3. Checks that git tracks the path (`git ls-files --error-unmatch`); an untracked file has no staged version.
4. Checks for unstaged edits (`git diff --quiet`); if there are none it prints "Nothing to revert" and exits 0.
5. Prints the command it is about to run.
6. Runs `git restore --worktree -- <file_path>`, which copies the staged version over the working-tree file.
7. Prints a success line, or git's stderr on failure.

## Output and exit codes

Progress lines go to stdout: the `git restore` command line, then a success or an error line.

| Code | Meaning |
|---|---|
| 0 | The file was restored, or it had no unstaged edits. |
| 1 | The path does not exist, git does not track it, or `git restore` failed. |
| 2 | Wrong arguments (argparse usage error). |

## Gotchas

- A file with no staged version (new and untracked) is refused; a tracked file that was never staged is restored to the committed version, since the index holds that.
- When the restore does work, the edits made since staging are lost with no prompt.
- Only one file per run.

## See also

- `revert-staged.py`
- Docs site: Developer Info, Script: revert-unstaged (`04.02.11.ScriptRevertUnstaged.md` in juneau-docs)
