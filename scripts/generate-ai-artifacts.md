# generate-ai-artifacts.py

Generates the deterministic AI-consumable knowledge artifacts for the site: `static/ai/juneau-knowledge.jsonl`, `static/ai/taxonomy.json`, `static/ai/manifest.json` and the llmstxt.org-style index `static/llms.txt`.

## When to use

- After changing topic pages, release notes or the sidebar, to refresh the artifacts before committing.
- With `--check`, to confirm the committed artifacts are current without writing anything (`check-ai-artifacts.py` does this as part of its checks).

## Usage

```bash
python3 scripts/generate-ai-artifacts.py
python3 scripts/generate-ai-artifacts.py --check
python3 scripts/generate-ai-artifacts.py --juneau-root ~/git/apache/juneau
npm run build:ai-artifacts
```

`npm run build:ai-artifacts` (see `package.json`) runs the first form.

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--docs-root` | directory | parent of `scripts/` | The docs checkout to read. |
| `--juneau-root` | directory | `../juneau` next to the docs root | Juneau source checkout, read for the current release and for API metadata. `JUNEAU_ROOT` is not consulted. |
| `--output-dir` | directory | `static/ai` in the docs root | Where the three `static/ai` files are written. `llms.txt` goes to its parent directory. |
| `--check` | none | off | Compare the generated content with what is on disk and write nothing. |

## What it does

1. Gets the current release by running the Juneau checkout's `scripts/current-release.py` (`unknown` if unavailable), and the `git rev-parse HEAD` of both checkouts for the manifest.
2. Builds one record per `pages/topics/*.md` and per `pages/release-notes/*.md` (`build_doc_record`), plus one `about` record per `src/pages/about/*.mdx` page (`build_about_records`; ids `about` and `about-<slug>`, the project's first module or `general`, with a Modules line placed before the body): id, title (frontmatter, then first heading, then file name), summary (first paragraph), description (body truncated to 2000 characters), module (from the topic file name), version and tags.
3. Adds an `api-metadata` record for every `.java` file under `juneau-core` and `juneau-rest` in the Juneau checkout that declares a `summary()` or `su()` annotation attribute with an empty default (`extract_api_metadata_records`).
4. Sorts the records, validates the required fields, and builds the taxonomy (counts by module and source type) and the manifest (schema version, release, a content-hash `generated_at`, source commits, record count).
5. Renders `llms.txt` from the top-level `Getting Started` and `Guides` categories and the children of the `Documentation` category of `sidebars.ts` (one section each, in that order), grouping topics as in the sidebar, plus an About section (the overview, then each project page in `projects.ts` order; omitted when there are no About pages) and a Resources section (release notes, the migration guide when present, javadocs, the knowledge base files).
6. Without `--check`: writes each file only when its content changed and prints `Updated` or `No change` per file.
7. With `--check`: compares each file with the expected content. For `manifest.json` the `source_commit` key is ignored, so a new commit with no content change is not stale.

## Output and exit codes

Stdout: one `Updated <path>` or `No change <path>` line per output and `Done. N file(s) updated.`; with `--check`, either `AI artifacts are up to date.` or an `ERROR: AI artifacts are stale` list of paths.

| Code | Meaning |
|---|---|
| 0 | Generated, or `--check` found everything current. |
| 1 | `--check` found stale or missing artifacts. |

A missing `Getting Started`, `Guides` or `Documentation` category in `sidebars.ts` or an invalid record raises an exception (exit 1 with a traceback).

## Gotchas

- Output is deterministic: the same inputs give byte-identical files, so a diff after a run reflects real content changes.
- `unknown` is written as the version if `current-release.py` cannot be run; check the Juneau root.
- `llms-full.txt` is deliberately not generated (the concatenated topics are large).
- The sidebar is read as text, starting at `mainSidebar:` so the helper code above it is never tokenized. Only the one-sidebar shape (Getting Started, Guides and Documentation as siblings) is supported.

## See also

- `check-ai-artifacts.py`, `build-docs.py`
- Docs site: Developer Info, Script: AI artifacts (`pages/developer-info/04.03.07.ScriptAiArtifacts.md`)
