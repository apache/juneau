# coverage.py

Per-package Java coverage report for the Release Manager, built on the JaCoCo data a `mvn test` run leaves in `target/`.

## When to use

- You want per-package branch and instruction coverage, or the uncovered lines of one package or file.
- To run the Java tests themselves, use `mvn verify` or `push.py --test-only`; this script only reports (or, with `--run`, refreshes the data first).

## Usage

```bash
python3 scripts/coverage.py --by-module
python3 scripts/coverage.py engine
python3 scripts/coverage.py --branches engine
python3 scripts/coverage.py --run --by-module
python3 scripts/coverage.py --help
```

This file only supplies this repository's settings (`REPO_ROOT`, base package `org/apache/juneau/releng`, report name, Maven arguments). The implementation is `juneau_scripts.coverage` from the Juneau `juneau-run` artifact, loaded through `scripts/bootstrap.py`.

## Options

The toolkit parses these by hand, not with `argparse`. Running with no arguments, `--help` or `-h` prints the help text and exits 0.

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `<package>` | top-level package name or a source path or file | none | Per-file detail and uncovered lines for that package, path or file. Example: `engine`. |
| `--by-module` | none | off | Summary table with one row per top-level package under `org.apache.juneau.releng`, sorted by branch percent ascending. Classes directly in that package are the `(root)` row. |
| `--run`, `-r` | none | off | Run `mvn test -Drat.skip=true -q` first, to refresh `target/jacoco.exec`. |
| `--branches`, `-b` | none | off | In the detail view, show only lines with missed branches. |
| `--help`, `-h` | none | | Print the help text. |

An unknown option that starts with `-` prints `ERROR: Unknown option: ...` and exits 1.

## What it does

1. Loads the toolkit through `bootstrap.load(pom.xml, run=False, cache_dir=target/juneau-run)`. If the installed toolkit has no `juneau_scripts.coverage`, it exits with an error asking you to refresh `juneau-run`.
2. Parses the arguments. With `--run`, runs `mvn test -Drat.skip=true -q`.
3. Requires `target/jacoco.exec` (otherwise exits 1, telling you to run `mvn clean test -Drat.skip=true` or pass `--run`), then runs the JaCoCo CLI over `target/classes` and `src/main/java` to write `target/coverage/jacoco.xml`.
4. Prints the `--by-module` table or the detail report for the named package. Lines marked with a standalone `HTT` token in a comment (hard to test) are dropped from the uncovered list and subtracted from the totals; their reasons are printed.
5. With neither `--by-module` nor a package, exits 1 with `No package or path specified`.

## Output and exit codes

Reports go to stdout. The generated XML is `target/coverage/jacoco.xml`.

| Code | Meaning |
|---|---|
| 0 | Report printed, or help shown. |
| 1 | Missing `target/jacoco.exec`, unknown option, no target given, or a stale toolkit. |

## Gotchas

- Needs the Juneau `juneau-run` toolkit (build or install Juneau first, or set `JUNEAU_SCRIPTS_ZIP`); see `bootstrap.py`.
- Coverage data comes from the JaCoCo agent wired into `pom.xml`, so a plain compile does not produce it.
- `HTT` matches only as a whole word inside a comment, not `HTTP`, and not inside a string literal.

## See also

- `push.md` (the Java Tests gate)
- `tests/README.md`
