# check-entry-pages.py

Checks that the docs entry pages (Getting Started, Ecosystem, Framework Comparisons, Why Juneau, the home page, the About pages, JuneauMarshall, REST server, Microservice, MCP quickstart, Petstore and Starter Projects) tell one story: one format count tied to the marshaller classes, one canonical first resource, the documented ports, and no comparison claim that Framework Comparisons does not support.

## When to use

- After editing an entry page, the JuneauMarshall badge block, or a REST sample on a first-resource page.
- As part of a full build: `build-docs.py` runs it as the "Entry pages" child row of its Verify stage, including under `--skip-maven`.
- For Java that does not parse, use `check-java-fences.py`; for the About hub's structure, `check-project-pages.py`.

## Usage

```bash
python3 scripts/check-entry-pages.py --juneau-root ~/git/apache/juneau
python3 scripts/check-entry-pages.py                      # K1b skipped if no Juneau checkout is found
python3 scripts/check-entry-pages.py --docs-root /path/to/juneau-docs
```

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--juneau-root` | directory | `$JUNEAU_ROOT`, then `../master`, then `../juneau` | The Juneau checkout whose marshaller packages K1b counts. K1b is skipped (with a note, not a failure) if both packages are not under it. |
| `--docs-root` | directory | parent of the `scripts/` folder | The docs checkout to check. |

## What it does

The entry pages are the 15 files in `ENTRY_PAGES` plus `src/pages/about/*.mdx` (or `src/pages/about.md` when there are none). "Prose" is a file with its fenced blocks blanked; fences are read with `code_fences.iter_fences`, language = first word of the info string.

1. **K0:** every entry page exists, and there is an About page.
2. **K1a** (entry pages, fences included): every `N formats` / `N+ formats` (optionally a link, optionally followed by `(C in ..., R RDF ...)`) has no `+`, N equals the JuneauMarshall badge total, and C and R equal the core and RDF badge counts. A badge linking to `/docs/topics/Rdf` is RDF; the rest are core.
3. **K1b** (needs Juneau): the core and RDF badge counts equal the facade classes (`public [final] class X extends CharMarshaller|StreamMarshaller`) directly in `juneau-marshall`'s and `juneau-marshall-rdf`'s `marshaller` packages. Today: 26 + 12 = 38. SOAP (serializer only) and `JsonPatchMarshaller` (JSON, in `juneau-bean-jsonpatch`) are not formats.
4. **K2a** (`pages/**/*.md` and `src/pages/**`, less release notes): no `/swagger-ui.html`, no `Java-serialized-object`. **K2b** (Java fences in `pages/**` and `src/pages/about/*.mdx`, less release notes): no `org.apache.juneau.rest.annotation`.
5. **K3** (first-resource pages: GettingStartedRestApp, WhyJuneau, JuneauRestServer, MicroserviceOverview, `about/rest-server.mdx` if present): **K3a** no bare `@Path` in a Java fence; **K3b** `extends BasicRestResource` in a Java fence only if the page links to `/docs/topics/GettingStartedRestApp#servlet-or-resource`; **K3c** no `juneau-shaded-all` in an XML fence.
6. **K4a** (entry pages): every `localhost:<port>` is 10000, 8080 or 5000. **K4b**: the MCP quickstart uses only 10000.
7. **K5** (entry-page prose, case-insensitive): none of `zero configuration`, `zero-dependency`, `zero dependencies`, `minimal memory footprint`, `fast startup`, `productive immediately`, `fewer security vulnerabilities`, `complex annotation system`, `minimal dependencies`.
8. Drops every failure named by `scripts/check-entry-pages-allowlist.txt` (`path<TAB>rule<TAB>literal<TAB>reason`, matched on path, rule and literal, not line). A malformed or unused entry is itself a failure.

## Output and exit codes

One line per failure, sorted: `path:line: rule: message`. Then any notes (`K1b skipped: no Juneau checkout at <path>`), then `Entry pages checked: N; failures: M`.

| Code | Meaning |
|---|---|
| 0 | No failures (K1b may have been skipped). |
| 1 | Any K0-K5 failure, or a malformed or stale allow-list entry. An explicit `--juneau-root` that is not a Juneau checkout (no `pom.xml`) also exits 1. |

## Gotchas

- Do not "correct" 38 to 39: a whole-repo search finds `JsonPatchMarshaller` too, but it is not in the two counted packages.
- K1a matches only a number directly before `formats`. "12 RDF formats" and "26 wire formats" are not counts to it, so write counts in the canonical phrase: `38 formats (26 in juneau-marshall, 12 RDF in juneau-marshall-rdf)`.
- K4 reads entry pages only. Guides keep their own ports (some use 9090, and say why).
- The allow-list literal for K5 is the phrase as listed (lower case); for K3 it is the fence line with its indentation stripped.

## See also

- `build-docs.py` (runs it in Verify), `check-project-pages.py`, `verify-docs.py`, `check-java-fences.py`
- The format-counting rule: the "Counting formats" section of `pages/topics/03.00.JuneauMarshall.md`
