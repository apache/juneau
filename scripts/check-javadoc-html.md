# check-javadoc-html.py

Runs javadoc's doclint `html`, `accessibility`, `reference` and `syntax` groups over every main source tree and fails on anything left once Juneau's own custom Javadoc elements are ignored.

## When to use

- You edited Javadoc and want to confirm it is valid HTML (heading levels, table captions, closed lists, alt text).
- You want the digest of what doclint still reports. `reference` and `syntax` are what CI's `maven-javadoc-plugin:aggregate` enforces (a `{@link}`, `@see` or `@throws` target that does not resolve fails the build), so a broken link is caught before push; `html` and `accessibility` are the groups the build leaves off.

## Usage

```bash
python3 scripts/check-javadoc-html.py
python3 scripts/check-javadoc-html.py --groups html --first 20
python3 scripts/check-javadoc-html.py --report-only
```

## Options

- `--groups LIST`: comma-separated doclint groups to run, from `html`, `accessibility`, `reference` and `syntax` (default: all four).
- `--refresh-classpath`: rebuild the cached dependency classpath the `reference` group needs.
- `--first N`: examples printed per kind of diagnostic (default 5).
- `--report-only`: print the digest but always exit 0.
- `--root DIR`: repository root (default: the checkout this script lives in).
- `--java-home DIR`: JDK whose `javadoc` to run (default: `$JAVA_HOME`, else `javadoc` on the PATH).
- `--from-file FILE`: read javadoc output from FILE instead of running javadoc (used by the tests).

## What it does

1. Collects every `*.java` file under a `src/main/java` tree (skipping `target`, `docs`, `module-info.java`) and runs one `javadoc -protected -Xdoclint:<groups> --ignore-source-errors` over all of them, so it needs no Maven build of the sources. `-protected` matches CI's default visibility, so a `{@link}` to a private member is reported as in CI.
   The `reference` group resolves types, so it gets the dependencies on `-cp`: the union of every module's compile classpath (Juneau's own artifacts removed), cached per module at `target/javadoc-classpath.txt` by one offline `mvn -o dependency:build-classpath` run. The first run builds the cache (about 30 seconds); delete the files or pass `--refresh-classpath` after a dependency change. Without the classpath, links to Jetty, Spring and the like show up as false "reference not found" reports.
2. Parses the `file:line: error|warning: message` diagnostics and drops duplicates.
3. Drops compile noise: errors from compiling without dependencies on the classpath (`cannot find symbol`, `package ... does not exist`, and similar). Those are not doclint findings.
4. Drops `unknown tag: <name>` where `<name>` is in `CUSTOM_ELEMENTS`, the single list of Juneau's own elements (`jk`, `jv`, `js`, `jc`, `c`, `jsm`, `ja`, ...), matching the selectors styled in `docs/src/javadoc/javadoc.css`. javadoc has no allowlist for custom elements, so this is the only way to filter them.
5. Prints counts of what was ignored, then the remaining diagnostics grouped by kind with the first N locations of each.

## Output and exit codes

| Code | Meaning |
|---|---|
| 0 | Nothing left after filtering, or `--report-only` was given. |
| 1 | At least one doclint diagnostic remains. |
| 2 | Bad arguments. |

## Gotchas

- Adding a new custom Javadoc element means adding it to `CUSTOM_ELEMENTS` and to `docs/src/javadoc/javadoc.css`.
- The headings convention: Juneau's section and topic headings are `<h2>` at the top of a class or package comment and `<h4>` inside members, so the sequence starts below the page's `<h1>`. `javadoc.css` styles `h2` to `h6` for the `section`, `topic` and `figure` classes.
- A new custom element name that is missing from the list shows up as `unknown tag: <name>`.
- The `docs` directory is skipped only at the repository root; `org.apache.juneau.rest.server.docs` is a real package and is scanned.
- The run takes about 15 seconds and writes only to a temporary directory.

## See also

- `docs/src/javadoc/javadoc.css`
- The root `pom.xml`, which sets `-Xdoclint:reference,syntax` for the build's javadoc. `push.py` runs this script as its Javadoc step.
