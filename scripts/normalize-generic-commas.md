# normalize-generic-commas.py

Rewrites generic type arguments such as `Map<K, V>` to `Map<K,V>` in Java source, in code and inside comments and Javadoc.

## When to use

- You are applying the project's generic-comma spacing to existing sources. The Eclipse formatter cannot rewrite text inside comments, so this script covers that half.
- Use `--check` to see which files would change, for example in a review or a script.

## Usage

```bash
python3 scripts/normalize-generic-commas.py juneau-core
python3 scripts/normalize-generic-commas.py path/to/File.java other/dir
python3 scripts/normalize-generic-commas.py --check juneau-core
```

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--check` | none | off | Dry run. Report the files that would change and write nothing. Must be the first argument. |
| `path` | one or more (positional) | none | A `.java` file, or a directory walked recursively. `target/` and `.git/` are skipped. At least one is required. |

## What it does

1. With no path, prints the module's usage text and exits 1 (`main`).
2. Collects the `.java` files under each path (`find_java_files`).
3. Segmentation pass: classifies every character as code, line comment, block comment (Javadoc included), string, char or text block, honoring escapes (`transform_source`). Strings, chars and text blocks are copied through untouched.
4. Generic-span pass: in code and comment segments, finds a `<`, walks forward tracking nesting depth, and accepts only characters that can appear in a type-argument list. The first other character abandons the match and the `<` is left alone.
5. Inside an accepted span, collapses only a comma followed by spaces or tabs to a bare comma. Bounded wildcards are untouched.
6. Rewrites each changed file in place, or only reports it with `--check`, and prints a summary.

## Output and exit codes

`Changed: <path> (code=n, comment=m)` per file, or `WOULD CHANGE:` with `--check`, then a summary of files scanned, files changed, and comma sites tightened in code and in comments.

| Code | Meaning |
|---|---|
| 0 | Done, or `--check` found nothing to change. |
| 1 | No path was given, or `--check` found files that would change. |

## Gotchas

- It is a scanner, not a parser, and it only misses sites. It never rewrites a comma outside a validated `<...>` span.
- Known misses: a span interrupted by a comment, and type-use annotations inside the arguments (`List<@NonNull String>`).
- A span must contain at least one letter or `?`, so lists of comparison operators like `<123, >=123` are not treated as generics.
- Spans longer than 2000 characters are not matched (`_MAX_SPAN`).
- Without `--check` it edits files directly. Review the diff afterwards.

## See also

None.
