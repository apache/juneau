# check-code-fences.py

Fails when a fenced code block in a docs page has a line that starts with an HTML anchor (`<a href=...>`). That is what a Javadoc-style link looks like when it leaks into a code sample, for example `<a href="...">@Request</a>` above an annotated interface.

## When to use

- After editing code samples in `pages/`.
- As part of a full build: `build-docs.py` runs it in its Links stage.
- For Java syntax problems in fences, use `check-java-fences.py` instead.

## Usage

```bash
python3 scripts/check-code-fences.py
python3 scripts/check-code-fences.py --docs-root ~/git/apache/juneau-docs
```

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--docs-root` | directory | parent of the `scripts/` folder | The docs checkout whose `pages/` tree is scanned. |

## What it does

1. Reads every `*.md` under `pages/` and extracts its fenced blocks (backtick or tilde fences, indented or inside admonitions) with `code_fences.iter_fences`.
2. Flags each line in a block whose first non-blank text is `<a ... href=`.
3. Prints `file:line: HTML anchor in code fence: <line>` for each hit.

## Output and exit codes

Stdout only. Nothing is written.

| Code | Meaning |
|---|---|
| 0 | No such anchors found. |
| 1 | At least one anchor found. |

## Gotchas

- Only lines that start with an anchor are flagged. Anchors in the middle of a line (HTML output shown in a comment, a string literal, an HTML or XML sample) and XML elements named `a` are legitimate and ignored.
- A fence that is never closed runs to the end of the file.

## See also

- `check-java-fences.py`, `code_fences.py`, `build-docs.py`
