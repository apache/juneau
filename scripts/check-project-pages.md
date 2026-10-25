# check-project-pages.py

Checks that the Juneau project data (`src/data/projects.ts`), the project pages (`src/pages/about/*.mdx`) and the topic pages they link to agree. It reads files only and needs no Juneau checkout.

## When to use

- After editing `src/data/projects.ts` or any page under `src/pages/about/`.
- After a module rename, to confirm each project still lists real modules.
- As part of a full build: `build-docs.py` runs it in its Verify stage, after `verify-docs.py`.

## Usage

```bash
python3 scripts/check-project-pages.py
python3 scripts/check-project-pages.py --docs-dir ~/git/apache/juneau-docs
```

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--docs-dir` | directory | parent of the `scripts/` folder | The docs checkout whose `src/data/projects.ts` and `src/pages/about/` are checked. |

## What it does

Parses `projects.ts` with `project_index.py` (it does not run TypeScript), then runs these checks:

| Id | Check |
|---|---|
| C1 | Every project slug has `src/pages/about/<slug>.mdx`. |
| C2 | Every `src/pages/about/*.mdx` other than `index.mdx` has a project entry. |
| C3 | Every project page has exactly one `<ProjectLayout slug="X">` (double or single quotes, or `slug={'X'}` / `slug={"X"}`; the tag may span lines) and `X` is the file name. `index.mdx` has exactly one `<ProjectLayout>` with no `slug`. |
| C4 | Slugs are unique and kebab-case. |
| C5 | Every `group` is a `GROUPS` id, and every group has at least one project. |
| C6 | Every `modules[]` entry is a key in `artifact-paths.json` and is not an aggregator pom (`AGGREGATOR_POMS` in `project_index.py`). A module may appear in several projects. |
| C7 | Every `docs[]` value is a `slug:` front-matter value under `pages/topics/` (a topic with no `slug:` counts under its file name). |
| C8 | `name` starts with `Juneau ` and `tagline` is not empty. |
| C9 | No about page or `projects.ts` contains a pre-rename module, package, tool, environment-variable, run-feature or topic-slug name from J1047 spec section 6. The patterns are in `project_old_names.py`. |
| C10 | Each group's projects are contiguous in `projects.ts`, in `GROUPS` order. |
| C11 | Each project page's `title` and `description` front matter equal its `name` and `tagline`. The overview needs a title and description. Every about page needs `hide_table_of_contents: true`. |
| C12 | Every `/docs/topics/<X>` link in an about page matches a topic `slug:`. |

A file that breaks the `projects.ts` format rules is reported as `C0`, with the line number, and stops the run.

## Output and exit codes

One line per violation on stdout, `C<n>: <where>: <what>`, then nothing else. A clean run prints `Project pages OK (<n> projects)`. Nothing is written.

| Code | Meaning |
|---|---|
| 0 | No violations. |
| 1 | At least one violation. |

## Gotchas

- C9's list lives in `project_old_names.py` and is assembled from fragments, so no source file spells an old name and the zero-old-names grep gate needs no exclusion. Keep it that way: this file must not spell an old name either.
- C9 matches whole tokens, so the `-utils` test module is not flagged. Old heading anchors are not in the list; they come from the rename tool's report.
- C6 reads `artifact-paths.json`, so regenerate that file (a `build-docs.py` run does it) after a module rename.
- `scripts/tests/test_check_project_pages.py` has a real-tree test that runs once `src/pages/about/index.mdx` exists.

## See also

- `project_index.py`, `project_old_names.py`, `check-doc-inventory.py`, `verify-docs.py`, `build-docs.py`
