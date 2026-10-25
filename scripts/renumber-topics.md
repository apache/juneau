# renumber-topics.py

Renumbers the `pages/topics` files gap-free in `mainSidebar` order after pages were removed or moved: it renames the files, rewrites front-matter `id:` lines, patches the matching `sidebars.ts` ids and labels in place, and rewrites references to the old ids and file names. It is a dry run unless you pass `--apply`.

## When to use

- After folding topic pages into their parents, once per chapter, to close the holes the deleted files left.
- After moving a sidebar entry to a new position, so the file prefixes follow the sidebar again.
- To rewrite `/docs/topics/<slug>` links, use `apply-renames.py`: slugs do not change on a renumber, so this script never touches them.

## Usage

```bash
python3 scripts/renumber-topics.py                     # dry run: print the plan
python3 scripts/renumber-topics.py --chapter 03        # dry run for one chapter
python3 scripts/renumber-topics.py --chapter 03 --apply
python3 scripts/renumber-topics.py --json
```

Run it on a tree whose other changes are already reviewed or set aside, and review the diff afterwards.

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--apply` | | off (dry run) | Rename the files and write the changes. |
| `--json` | | off | Print the plan as JSON (`mapped`, `total`, `renames`, `relabels`, `unmapped`, `applied`). |
| `--chapter` | two digits | all chapters | Only rename and relabel pages in this chapter, for example `03`. |
| `--docs-dir` | directory | parent of the `scripts/` folder | The docs checkout to renumber. |

## What it does

1. Loads `verify-docs.py` and reuses its sidebar parser and `Topic` class. Reads every `pages/topics/*.md` and `sidebars.ts`.
2. Walks `mainSidebar` depth first. Every doc and every category whose link is a doc gets the next number under its parent; a category with no doc link (a group) does not take a number and its items continue the count. Chapters keep their number, and a category landing gets a `.00` file suffix. A page's front-matter `id`, when it has one, keeps its name part and takes the new number.
3. Relabels a sidebar entry only when the number in its label matched the page's old file prefix, so hand-written labels are left alone.
4. Refuses (exit 1) when a sidebar id has no page, when two pages would get the same file name or id, when a page would be renamed onto a file that is not in `mainSidebar`, or when a label to rewrite appears more than once in `mainSidebar`.
5. With `--apply`: computes every new file text first, then renames the files in two steps (through `.renumtmp` names, so a shift such as `03.03` to `03.02` and `03.04` to `03.03` never overwrites), writes the rewritten pages, and patches `sidebars.ts` in place.
6. Reference rewriting, in one simultaneous pass so a shift never chains: `topics/<old id>` strings anywhere in `sidebars.ts`; labels inside `mainSidebar` only; in the topic pages, other pages under `pages/` (not release notes or developer-info) and `scripts/verify-docs-allowlist.txt`, bare old ids and file stems (a longer token that only starts with an old id is not touched) and `@site/pages/topics/<file>.md` imports.

## Output and exit codes

Stdout: one `RENAME old -> new`, `ID old -> new` or `LABEL 'old' -> 'new'` line per change, an `UNMAPPED` line per topic page not in `mainSidebar` (left alone), then `mapped N of M topic pages; R renamed, L relabelled, U unmapped; dry run, nothing written` (or `; applied`). Errors go to stderr.

| Code | Meaning |
|---|---|
| 0 | Planned (and, with `--apply`, written). |
| 1 | The sidebar and the files disagree in a way it will not resolve; nothing was written. |
| 2 | `pages/topics` or `sidebars.ts` is missing, or an argument is invalid. |

## Gotchas

- A second run on a renumbered tree prints `0 renamed, 0 relabelled`; anything else means the tree changed in between.
- It does no git calls. The renames show up as a delete plus an untracked file until someone stages them.
- `developerSidebar`, `releaseNotesSidebar` and `pages/guides` ids are never renamed.
- Gap-free numbering is a convention: `verify-docs.py` check 9 passes with gaps, and check 10 only warns on label mismatches. Run `verify-docs.py --only order,labels,links,imports` after an `--apply`.
- The generated AI artifacts take a module tag from the file name, so regenerate them after a renumber.

## See also

- `verify-docs.py`, `apply-renames.py`, `generate-ai-artifacts.py`
