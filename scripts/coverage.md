# coverage.py

Reports JaCoCo branch and instruction coverage for a source file or folder, merged across every module's test data.

## When to use

- You want the coverage of one class or package after changing it, including the uncovered line numbers.
- You want a per-module summary of the whole repository (`--by-module`).
- You need to mark a line as intentionally out of scope for coverage (see Gotchas).

## Usage

```bash
python3 scripts/coverage.py juneau-core/juneau-marshall/src/main/java/org/apache/juneau/marshall/BitSetFormat.java
python3 scripts/coverage.py juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/conversion/
python3 scripts/coverage.py path/to/File.java --run --branches
python3 scripts/coverage.py --by-module
```

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `path` | file or folder (positional) | none | A `.java` file or a source folder under `src/main/java` or `src/test/java`. Absolute, or relative to the repository root. Required unless `--by-module` is used. |
| `--run`, `-r` | none | off | Re-run the owning module's tests (and its sibling `-tests` module, if any) first to refresh its `jacoco.exec`. |
| `--branches`, `-b` | none | off | Show only lines with missed branches instead of all uncovered lines. |
| `--by-module` | none | off | Print a per-module summary table sorted by branch percentage, with a TOTAL row, instead of reporting a path. |
| `--help`, `-h` | none | off | Print the usage text and exit 0. Also printed when no argument is given. |

## What it does

1. With no arguments or a help flag, prints the usage text and exits 0 (`main`).
2. Finds the nearest Maven module above the path (`find_maven_module`) and derives the JaCoCo package and file name (`path_to_jacoco_package`).
3. With `--run`, runs `mvn -pl <module>[,<module>-tests] test -Drat.skip=true -q` (`run_tests`). A failure stops the script.
4. Collects every module's `target/jacoco.exec`, `target/classes` and `src/main/java`, skipping the shaded, distribution, examples, petstore, microservice, `juneau-sc` and test-utils modules (`collect_jacoco_inputs`, `EXCLUDED_MODULE_MARKERS`).
5. Runs the JaCoCo CLI to write one combined XML report to `target/coverage/jacoco.xml` (`generate_combined_report`). The CLI jar is taken from `~/.m2`, or fetched with Maven if absent (`jacoco_cli_jar`).
6. Prints per-file and total branch and instruction coverage with bars and the uncovered line numbers (`report`), or the module table (`by_module_report`).

## Output and exit codes

Per file: branch and instruction percentages, missed counts, and the uncovered lines. Files with excluded lines are marked `(HTT-adjusted)` and list the excluded line numbers with their reasons.

| Code | Meaning |
|---|---|
| 0 | A report was printed, or help was shown. |
| 1 | An error: unknown option, no path, a path outside `src/main/java` or `src/test/java`, no `jacoco.exec` or compiled classes found, failed tests with `--run`, or a JaCoCo failure. |

## Gotchas

- Coverage is the union of every module's execution data, so a class tested only by its own module's unit tests is still counted. Build and test the project first so the `jacoco.exec` files exist.
- `--run` refreshes only the owning module. The other modules' existing data is merged in as is. It needs upstream module artifacts in `~/.m2` from an earlier `mvn install -DskipTests`.
- `--run` has no effect with `--by-module`, which prints a note. Run the full test suite first, for example `mvn clean test -Drat.skip=true`.
- Hard-to-test lines: a standalone `HTT` word in a comment on a line (`// HTT`, `x++; // HTT: reason`, `/* HTT */`) removes that line from the uncovered list and subtracts its counts from the totals. `HTTP` does not match, and a match inside a string literal does not count.
- The JaCoCo version comes from `jacoco.plugin.version` in the root `pom.xml`. The Java launcher is taken from `JAVA_HOME`, then `~/jdk/default`, then `PATH`. `MAVEN_REPO` or `M2_REPO` can point at a different local repository.

## See also

- `sonarqube.py`
- `test.py`
