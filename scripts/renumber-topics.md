# renumber-topics.py

Renumbers the `pages/topics` files gap-free in `mainSidebar` order after pages were removed or moved: it renames the files, rewrites front-matter `id:` lines, patches the matching `sidebars.ts` ids in place, and rewrites references to the old ids and file names. It does not touch sidebar labels: the numbers readers see in the sidebar are generated from position by `numbered()` in `sidebars.ts`, so file prefixes (`03.25.JsonSupport`) are an ordering convention and no longer equal the displayed number (`2.2.25`). It is a dry run unless you pass `--apply`.

## When to use

- After folding topic pages into their parents, once per chapter, to close the holes the deleted files left.
- After moving a sidebar entry to a new position, so the file prefixes follow the sidebar again. The displayed sidebar numbers follow the position by themselves.
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
| `--json` | | off | Print the plan as JSON (`mapped`, `total`, `renames`, `unmapped`, `applied`). |
| `--chapter` | two digits | all chapters | Only rename pages in this chapter, for example `03`. |
| `--docs-dir` | directory | parent of the `scripts/` folder | The docs checkout to renumber. |

## What it does

1. Loads `verify-docs.py` and reuses its sidebar parser and `Topic` class. Reads every `pages/topics/*.md` and `sidebars.ts`.
2. Walks `mainSidebar` depth first. Every doc and every category whose link is a doc gets the next number under its parent; a category with no doc link (a group) does not take a number and its items continue the count. Chapters keep their number, and a category landing gets a `.00` file suffix. A page's front-matter `id`, when it has one, keeps its name part and takes the new number.
3. Refuses (exit 1) when a sidebar id has no page, when two pages would get the same file name or id, or when a page would be renamed onto a file that is not in `mainSidebar`.
4. With `--apply`: computes every new file text first, then renames the files in two steps (through `.renumtmp` names, so a shift such as `03.03` to `03.02` and `03.04` to `03.03` never overwrites), writes the rewritten pages, and patches `sidebars.ts` in place.
5. Reference rewriting, in one simultaneous pass so a shift never chains: `topics/<old id>` strings anywhere in `sidebars.ts`; in the topic pages, other pages under `pages/` (not release notes or developer-info) and `scripts/verify-docs-allowlist.txt`, bare old ids and file stems (a longer token that only starts with an old id is not touched) and `@site/pages/topics/<file>.md` imports.

## Output and exit codes

Stdout: one `RENAME old -> new` or `ID old -> new` line per change, an `UNMAPPED` line per topic page not in `mainSidebar` (left alone), then `mapped N of M topic pages; R renamed, U unmapped; dry run, nothing written` (or `; applied`). Errors go to stderr.

| Code | Meaning |
|---|---|
| 0 | Planned (and, with `--apply`, written). |
| 1 | The sidebar and the files disagree in a way it will not resolve; nothing was written. |
| 2 | `pages/topics` or `sidebars.ts` is missing, or an argument is invalid. |

## Gotchas

- A second run on a renumbered tree prints `0 renamed`; anything else means the tree changed in between.
- It does no git calls. The renames show up as a delete plus an untracked file until someone stages them.
- Only `topics/` ids are renamed; `guides/`, `developer-info/` and `release-notes/` ids in `sidebars.ts` are left alone.
- Gap-free numbering is a convention: `verify-docs.py` check 9 passes with gaps, and the file prefixes and the displayed sidebar numbers are independent. Run `verify-docs.py --only order,links,imports` after an `--apply`.
- The generated AI artifacts take a module tag from the file name, so regenerate them after a renumber.

## See also

- `verify-docs.py`, `apply-renames.py`, `generate-ai-artifacts.py`
