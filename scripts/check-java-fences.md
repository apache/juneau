# check-java-fences.py

Parses every `java` code fence in the docs pages with the JDK's own parser and reports the ones that are not valid Java. It parses only: no symbols are resolved and nothing is compiled against jars.

## When to use

- To find broken Java samples (missing `class`, stray markup, unbalanced braces) before readers do.
- Run it by hand after editing a page. `build-docs.py` runs it with `--strict` in its Links stage (the Java fences row), so a fence that does not parse fails the build.
- For anchors inside fences, use `check-code-fences.py` instead.

## Usage

```bash
python3 scripts/check-java-fences.py
python3 scripts/check-java-fences.py --strict
python3 scripts/check-java-fences.py --java /path/to/jdk/bin/java
```

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--docs-root` | directory | parent of the `scripts/` folder | The docs checkout whose `pages/` tree (plus the `*.mdx` pages in `src/pages/about/`) is scanned. |
| `--java` | file | `~/jdk/default/bin/java` | The `java` launcher to run `JavaFenceParse.java` with (JDK 17 or later). |
| `--strict` | none | off | Exit 1 when any fence fails to parse. Without it the exit code is 0 whatever was found. |

## What it does

1. Extracts every fence whose language is `java` (`code_fences.iter_fences`), skipping those whose meta contains the word `fragment` (write ` ```java fragment `).
2. Writes the fences to a temporary directory and runs `java scripts/JavaFenceParse.java <dir> <count>` once for all of them.
3. The helper uses `JavacTask.parse()` and tries each fence as a compilation unit, then wrapped in a class body, then wrapped in a method body. If all three fail it splits the fence into independent pieces with JShell's completeness analysis (so a class declaration followed by loose statements is accepted) and parses every piece the same three ways. JShell is used only to split; it alone would accept incomplete constructs such as `class Pet { ... }`. A fence is reported only if every strategy fails.
4. Prints `file:line: java fence does not parse` for each failure, then the totals.

## Output and exit codes

Stdout only. Nothing is written outside the temporary directory.

| Code | Meaning |
|---|---|
| 0 | The scan completed (and, with `--strict`, nothing failed). |
| 1 | `--strict` was given and at least one fence failed. |

A missing JDK or a helper crash raises an error with the helper's stderr.

## Gotchas

- Placeholders such as `...` or `{...}`, bare expressions, and signature-only snippets do not parse. Mark deliberate fragments with ` ```java fragment `.
- Because `build-docs.py` runs it with `--strict`, an unmarked fence that does not parse fails the build.
- The helper needs the `jdk.jshell` module, which every full JDK 17+ ships.
- Parse-only means a sample with an unknown type or a wrong method name still passes.
- The line number is that of the opening fence.

## See also

- `check-code-fences.py`, `code_fences.py`, `JavaFenceParse.java`
