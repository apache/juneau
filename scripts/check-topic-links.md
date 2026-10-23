# check-topic-links.py

Checks that links to topic pages of the Juneau documentation site, found in the source tree, use the slug and title the topic pages define.

## When to use

- You renamed or added a topic page and want to find Javadoc or markdown links that no longer match.
- It only reports problems; it never changes a file and never fails the build.

## Usage

```bash
python3 scripts/check-topic-links.py
```

## Options

None.

## What it does

1. Reads every `*.md` file (except `README.md`) in `docs/pages/topics` and takes `slug` and `title` from the front matter (`extract_topic_info`).
2. Walks the repository root, skipping dot-directories and `target`, `node_modules`, `build`, `dist` and `scripts`, and reads `.java`, `.md`, `.xml`, `.properties`, `.txt`, `.adoc` and `.rst` files (`find_topic_links`). Code blocks and inline code are removed from markdown files first.
3. Collects every `https://juneau.apache.org/docs/topics/<slug>">Title</a>` link with its file and line.
4. Reports each link whose slug is unknown (`UNKNOWN SLUG`) or whose link text differs from the page title (`TITLE MISMATCH`) (`check_links`).

## Output and exit codes

Progress lines, then either `All topic links are correct!` or a list of warnings, each with file, line, slug, expected and actual title, and the link. Everything goes to stdout.

| Code | Meaning |
|---|---|
| 0 | The scan finished, with or without warnings. |
| 1 | No topics were found, for example because `docs/pages/topics` does not exist. |

## Gotchas

- Warnings never change the exit code.
- The topic pages are read from `docs/` in this checkout, so it needs that directory present.
- Link text is compared as written, so a different but acceptable label shows up as a mismatch.

## See also

- Docs site: Developer Info, Script: check-topic-links (`04.03.06.ScriptCheckTopicLinks.md` in juneau-docs)
