# check-topic-template.py

Advisory report on how far the topic pages are from the reference-topic template: pages under a word count, pages with no code fence, and pages whose template headings are out of order. It never fails a build.

## When to use

- Before and after editing a chapter's topic pages, to see which pages are still stubs (`--chapter`).
- When choosing pages to fold or fill: the JSON output is the input to a stub triage table.
- To check sidebar structure, slugs or links, use `verify-docs.py` instead; this script only measures page bodies.

## Usage

```bash
python3 scripts/check-topic-template.py
python3 scripts/check-topic-template.py --chapter 03
python3 scripts/check-topic-template.py --json --min-words 200
```

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--docs-dir` | directory | parent of the `scripts/` folder | The docs checkout whose `pages/topics` is scanned. |
| `--min-words` | number | 150 | Report pages with fewer words than this. |
| `--json` | | off | Print `{"totals": ..., "pages": [...]}` instead of the text report. |
| `--chapter` | two digits | all chapters | Only scan pages whose file name starts with this chapter prefix, for example `03`. |

## What it does

1. Reads every `pages/topics/*.md` in file-name order (only the files whose name starts with `NN.` when `--chapter` is given).
2. Removes the front matter, then removes every fenced code block (opener, body and closer) found by `code_fences.iter_fences`, so backtick and tilde fences and indented fences are all excluded.
3. Counts the remaining words by whitespace, so HTML tags such as `<a href=...>` count as words, and counts the fences.
4. Collects the `##` headings outside fences and matches them, case-insensitively, against the template headings in order: `When to use` or `When to use it`; `Example`, `Examples` or `A complete example`; `Options`, `Configuration` or `Options and configuration`; `See also`. A page whose recognized headings appear in a different order is reported; a page with none of them is not.
5. Prints three lists (under the word count, no fence, out of order) with each page's word count, then a totals line.

## Output and exit codes

Stdout only. Nothing is written.

The text report ends with a line such as `Totals: 470 pages, median 347.5 words; 85 under 150 (52 under 100); ...; under 150 by chapter: 02: 2, 03: 32, ...`. With `--json`, each page entry has `file`, `chapter`, `words`, `fences`, `h2_count`, `template_headings` and `out_of_order`.

| Code | Meaning |
|---|---|
| 0 | Completed, whatever it found. |
| 2 | The docs directory has no `pages/topics`, or an argument is invalid. |

## Gotchas

- It is advisory by design: a page under the word count is not an error, and a filled page may stay under 150 words when each template part is short.
- It checks structure only. The voice rule (present tense, second person, no tutorial narrative) is reviewed by hand.
- The lead paragraph has no heading, so the script cannot see whether a page has one.
- Developer-info, guide and release-note pages are not scanned.

## See also

- `verify-docs.py`, `code_fences.py`, `check-java-fences.py`
- The template itself: `pages/developer-info/02.10.TopicPageTemplate.md`
