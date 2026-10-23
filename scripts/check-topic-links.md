# check-topic-links.py

Scans the Juneau source tree for links to documentation topics (`https://juneau.apache.org/docs/topics/<slug>">Title</a>`) and reports any whose slug does not exist or whose link text differs from the topic's title.

## When to use

- After renaming a topic page or changing its `title` or `slug`, to find the Javadoc and source links that now point at the old name.
- As part of a full build: `build-docs.py` runs it in its Links stage.
- For links between docs pages, use `verify-docs.py` instead.

## Usage

```bash
python3 scripts/check-topic-links.py
python3 scripts/check-topic-links.py --juneau-root ~/git/apache/juneau
```

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--docs-root` | directory | parent of the `scripts/` folder | The docs checkout whose `pages/topics` define the valid slugs and titles. |
| `--juneau-root` | directory | `$JUNEAU_ROOT`, then `../master`, then `../juneau` next to the docs root | Juneau source checkout to scan. Must contain `pom.xml` when given explicitly. |

## What it does

1. Resolves the Juneau checkout with `juneau_root.find_juneau_root` (exits 1 if none is found).
2. Reads every `pages/topics/*.md` except `README.md` and records `slug` and `title` from the frontmatter (`extract_topic_info`). Pages missing either one produce a warning line.
3. Walks the Juneau checkout (`find_topic_links`), skipping dot-directories and `target`, `node_modules`, `build`, `dist` and `scripts`, and reads `.java`, `.md`, `.xml`, `.properties`, `.txt`, `.adoc` and `.rst` files. In Markdown files, code blocks and inline code are removed first.
4. Matches the pattern `https://juneau.apache.org/docs/topics/<slug>">Title</a>` and compares each hit with the topic table (`check_links`): an unknown slug is `UNKNOWN SLUG`, a different title is `TITLE MISMATCH`.
5. Prints each finding with file and line, then the total.

## Output and exit codes

Stdout only: a line per topic found, counts, and the findings. Nothing is written.

| Code | Meaning |
|---|---|
| 0 | The scan completed, with or without warnings (`Warnings are informational only and do not fail the build`). |
| 1 | No topics were found in the docs, or the Juneau checkout could not be located. |

## Gotchas

- Findings never fail the script, so check the output rather than the exit code.
- Only links in the exact `...topics/<slug>">Title</a>` form are checked.
- The `scripts` directory of the scanned tree is skipped, so links inside Juneau's own scripts are not checked.

## See also

- `verify-docs.py`, `build-docs.py`, `juneau_root.py` (checkout lookup)
- Docs site: Developer Info, Script: check-topic-links (`pages/developer-info/04.03.06.ScriptCheckTopicLinks.md`)
