# repin-consumers.py

Rebuilds Juneau's `HEAD` and installs it into the shared `~/.m2` repository, so downstream projects that resolve the floating `10.0.0-SNAPSHOT` version pick up the new build.

## When to use

- You changed Juneau and a downstream project (Release Manager or another consumer) should build against the change.
- "Re-pin" means exactly this install. There is no version property to bump and no pin file to edit.

## Usage

```bash
python3 scripts/repin-consumers.py
python3 scripts/repin-consumers.py --verify-consumers
python3 scripts/repin-consumers.py --juneau-dir ~/git/apache/juneau-9.2.1 --verify-consumers
```

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--juneau-dir` | path | `~/git/apache/juneau` | Juneau repository root to build and install. |
| `--verify-consumers` | none | off | After a successful install, run `mvn -B -nsu clean test-compile` in each consumer checkout and report pass or fail. Never runs git in them. |
| `--release-manager-dir` | path | `~/git/apache/release-manager` | Release Manager checkout, used only with `--verify-consumers`. |
| `--support-console-dir` | path | `SUPPORT_CONSOLE_DIR_DEFAULT` in the script | Second consumer checkout, used only with `--verify-consumers`. |

## What it does

1. Resolves the Juneau root and exits 2 if it is not a directory or has no `.git` (`main`).
2. Guard: runs `git status --porcelain` and refuses, exit 1, if any line is returned. Modified, staged and untracked non-ignored files all count (`check_dirty`).
3. Prints the short hash and subject of `HEAD`, and warns if `HEAD` differs from the locally known `origin/master` (`describe_head`). No `git fetch` is run, so the comparison can be stale.
4. Sets `JAVA_HOME` to `~/jdk/default` if that exists, otherwise keeps the environment's value (`resolve_java_home`).
5. Runs `mvn -B -nsu clean install -DskipTests` in the Juneau tree with live output (`run_streaming`). `-nsu` stops Maven from pulling a published snapshot that would shadow the local build.
6. With `--verify-consumers`, runs `mvn -B -nsu clean test-compile` in each consumer checkout (`verify_consumer`). A missing directory counts as a failure.
7. Prints a summary: the version read from the root `pom.xml` (`read_pom_version`), the commit, and the install location under `~/.m2/repository/org/apache/juneau/`.

## Output and exit codes

Progress lines, Maven's own output, then a `=== Summary ===` block.

| Code | Meaning |
|---|---|
| 0 | The install succeeded and, with `--verify-consumers`, every consumer compiled. |
| 1 | The dirty-tree guard refused. Nothing was built or installed. |
| 2 | Usage error, or the Juneau tree or its `pom.xml` could not be found or read. |
| 3 | The Maven install failed. `~/.m2` may be partly updated. |
| 4 | The install succeeded but at least one consumer failed to compile. |

## Gotchas

- There is no `--allow-dirty` flag, on purpose. Installing a dirty tree puts unreviewed work into a repository shared by every build on the machine. Commit, stash or discard the changes first.
- A clean tree is not the same as a pushed one. Unpushed commits are installed, with a warning.
- Every project on the machine that resolves the snapshot sees the new build on its next build.
- Consumer checkouts are only built, never modified. Nothing runs git in them.

## See also

- `push.py`, whose unreviewed-changes gate this guard mirrors
