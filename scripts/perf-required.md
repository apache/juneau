# perf-required.py

Answers one question for a change: does it touch a performance-sensitive hot-path file?

## When to use

- Your landing notes call for a performance section whenever a change touches the core hot-path classes, and you want to know whether this change does.
- It checks only that file trigger. A rule that depends on how many tests changed is not covered and stays a manual check.

## Usage

```bash
python3 scripts/perf-required.py
python3 scripts/perf-required.py --against origin/master
python3 scripts/perf-required.py --files path/to/RestContext.java path/to/Other.java
python3 scripts/perf-required.py --list-hot-paths
```

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--against` | ref | auto-detected upstream default branch | Compare against the merge base of `HEAD` and this ref instead. |
| `--files` | one or more paths | none | Check this explicit file list and do not run git. |
| `--list-hot-paths` | none | off | Print the hot-path file names, one per line, and exit 0. |

## What it does

1. With `--list-hot-paths`, prints the inventory and exits 0 (`main`).
2. With `--files`, uses that list. Otherwise computes the touched files with `git diff --name-only <merge-base>`, which includes uncommitted changes (`touched_files`).
3. The upstream branch is `--against` when given, else the target of `refs/remotes/origin/HEAD`, else the `HEAD branch` reported by `git remote show origin` (`detect_upstream_default_branch`).
4. With no usable upstream, falls back to `git diff --name-only HEAD`, the uncommitted changes only.
5. Keeps the files whose base name is one of: `RestContext.java`, `Settings.java`, `VarResolver.java`, `PropertyVar.java`, `BeanContext.java`, `BeanStore.java`, `MockRestClient.java`, `ValueResolver.java`, `FieldInfo.java`, `ParameterInfo.java` (`hot_path_hits`).
6. Prints `yes` with the matching files, or `no`.

## Output and exit codes

The first line starts with `yes` or `no`, followed by the matching files for `yes` and a reminder that the test-count trigger is a manual check.

| Code | Meaning |
|---|---|
| 0 | No hot-path file touched. |
| 1 | At least one hot-path file touched. |
| 2 | The diff could not be computed (not a git repository, an unresolvable `--against` ref, or a failed git command). |

## Gotchas

- The exit code is the answer, so it can be used in a shell conditional. Exit 1 means "yes", not an error.
- New files that git does not yet track are not seen by `git diff`; add them to the index first, or pass them with `--files`.
- Matching is by base name only, so a file with one of these names anywhere in the tree counts.
- An explicit `--against` that cannot be resolved is an error, not a silent fallback to `HEAD`.
- `--help` prints the full description of the rule as its epilog.

## See also

- `test.py`, whose `--enforce-perf` mode compares test timings against a baseline
