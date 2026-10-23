# revert-staged.py

Discards the changes to one git-tracked file by restoring it from `HEAD`, the last commit.

## When to use

- You want to throw away all in-progress edits to a single file and get the committed version back.
- To keep staged work and drop only later edits, use `revert-unstaged.py` instead.

## Usage

```bash
python3 scripts/revert-staged.py juneau-core/juneau-marshall/src/main/java/org/apache/juneau/ClassMeta.java
```

Run it from the repository root; the path is passed to git as given, after a `--` so a path that starts with `-` or matches a ref name is read as a path. For a path that starts with `-`, put `--` before it on the command line too.

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `file_path` | path (positional, required) | none | The tracked file to restore. Exactly one argument is required; any other count prints the usage text to stderr and exits 2. |
| `-h`, `--help` | none | off | Print the usage text and exit 0 without calling git. |

## What it does

1. Parses the arguments with argparse (`main`), so `--help` and bad arguments never reach git.
2. Runs `git ls-files --error-unmatch -- <file_path>`; an untracked path stops the script.
3. Prints a warning and the command it is about to run.
4. Runs `git restore --source=HEAD -- <file_path>`.
5. Prints a success line, or git's stderr on failure.

## Output and exit codes

Progress lines go to stdout: the warning, the `git restore` command line, and either a success or an error line. Usage errors from argparse go to stderr.

| Code | Meaning |
|---|---|
| 0 | `git restore` succeeded. |
| 1 | The file is not tracked by git, or `git restore` failed. |
| 2 | Wrong number of arguments (usage text on stderr). |

## Gotchas

- The banner says "staged and unstaged", but the script runs `git restore --source=HEAD -- <file>` without `--staged`. Git restores the working tree only in that form, so the index is left alone: a change that was already staged stays staged (`git diff --cached` still shows it) and `git status` can show the file as modified against the index. To also unstage, run `git restore --staged <file>` yourself.
- Run from inside the repository; `--` makes the path literal, so it is not read as a ref or option.
- There is no confirmation prompt. The working-tree edits are gone once the command runs.
- Only one file per run. It does not accept directories or globs through its own checks; git decides what a path means.

## See also

- `revert-unstaged.py`
- Docs site: Developer Info, Script: revert-staged (`04.02.10.ScriptRevertStaged.md` in juneau-docs)
