# check-guide-snippets.py

Checks that every code fence carrying a `guide=FILE#REGION` pointer shows exactly that region of a file in the `juneau-examples/juneau-examples-guides` module of the Juneau checkout, and that every non-shell fence on a guide page carries a pointer. The module's own build compiles and tests those files, so a guide's code cannot drift from code that works.

## When to use

- After editing a guide page under `pages/guides/`, or a file in `juneau-examples-guides` that a guide points at.
- As part of a full build: `build-docs.py` runs it in its Links stage, and the docs gate runs it too.
- For Java that does not parse, use `check-java-fences.py`; for HTML anchors inside fences, `check-code-fences.py`.

## Usage

```bash
python3 scripts/check-guide-snippets.py --juneau-root ~/git/apache/juneau
python3 scripts/check-guide-snippets.py --juneau-root ~/git/apache/juneau --docs-root /path/to/juneau-docs
```

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--juneau-root` | directory | `$JUNEAU_ROOT`, then `../master`, then `../juneau` | The Juneau checkout holding `juneau-examples/juneau-examples-guides`. |
| `--docs-root` | directory | parent of the `scripts/` folder | The docs checkout whose `pages/` tree is scanned. |

## What it does

1. Reads every fence in `pages/**/*.md` (`code_fences.iter_fences`) and splits its info string on whitespace. A word `guide=FILE#REGION` is a pointer; at most one per fence. Other words (`fragment`, `title="..."`, `{1,3}`, `showLineNumbers`) are ignored.
2. Resolves `FILE` under the module's `src/test/java` and `src/test/resources`. A bare basename (`PetResource.java`) must match exactly one file; a relative path (`SpringBootRestApi/pom.xml`) is tried under each root. Sample POMs and config files live in `src/test/resources/<GuideSlug>/` and are always named by relative path.
3. Finds the region between marker lines `region NAME` and `endregion NAME`, written as a `//`, `#` or `<!-- -->` comment. A region may start above the `package` line. Region names are unique in a file and regions do not nest; every closer names its region.
4. Normalizes the fence body and the region body the same way: CRLF to LF, tabs expanded to four-column stops, trailing whitespace stripped, common indentation removed, leading and trailing blank lines dropped. They must be equal.
5. On a guide page (`pages/guides/*.md` except the `00.Guides.md` landing), every fence whose language is not `bash`, `sh`, `shell`, `console` or `text` must carry a pointer. A `text` fence may carry one, so the "Run it" output can point at a resource file the guide's test asserts against.

It never runs Maven: the module's tests are what make a region trustworthy.

## Output and exit codes

One `page:line: message` line per problem (the line of the opening fence), with the first lines of a unified diff for a mismatch, then `Guide fences checked: N; failing: M; regions unreferenced: K`. Unreferenced regions are informational.

| Code | Meaning |
|---|---|
| 0 | Every pointer matches and no guide-page fence lacks one, including when no page carries a pointer. |
| 1 | Any problem: `guide source not found`, `ambiguous guide source`, `region not found`, `region unterminated`, `endregion mismatch`, `duplicate region`, `nested region`, `guide fence does not match`, `more than one guide= pointer`, `malformed guide= pointer`, `guide fence without pointer`, or `guide module not found` when a pointer exists but the module does not. An explicit `--juneau-root` that is not a Juneau checkout also exits 1. |

## Gotchas

- Two test classes with the same basename in different packages make a basename pointer ambiguous; give each guide its own class names.
- A marker line is any comment line `region NAME` or `endregion NAME` with nothing else on it, so an INI or YAML comment that reads `# region east` opens a region.
- Docusaurus ignores the `guide=` word, so it never shows on the page.
- With no checkout found and no `--juneau-root`, the check passes only while no fence carries a pointer.

## See also

- `check-java-fences.py`, `check-code-fences.py`, `code_fences.py`, `juneau_root.py`, `build-docs.py`
- `pages/developer-info/02.09.AiKnowledgeAuthoring.md` ("Guide fences")
