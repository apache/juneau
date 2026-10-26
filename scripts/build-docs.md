# build-docs.py

Builds the complete Juneau documentation site locally: Maven site and javadocs from a Juneau checkout, then the Docusaurus build, then the integrity and link checks.

## When to use

- You want to build the full site exactly as the publish pipeline does, before staging it.
- You only need the Docusaurus smoke build with no Juneau checkout: use `--skip-maven`.
- You are editing pages and want live preview: use `start-docusaurus.py`; it does not build the site.
- To publish what you built, use `release-docs-stage.py` and then `release-docs.py`.

## Usage

```bash
python3 scripts/build-docs.py
python3 scripts/build-docs.py --staging --juneau-root ~/git/apache/juneau
python3 scripts/build-docs.py --skip-maven --skip-copy
python3 scripts/build-docs.py --dry-run
```

Run from the docs checkout root.

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--skip-npm` | none | off | Skip `npm ci` and the Docusaurus build. |
| `--skip-maven` | none | off | Skip `mvn clean install`, `mvn site`, the apidocs check, the javadoc and site copies, and the link checks. The Juneau checkout may then be missing. |
| `--skip-copy` | none | off | Skip copying the Maven site into `static/site` and the `.asf.yaml` copy. The versioned javadocs copy still runs. |
| `--skip-verify` | none | off | Skip the documentation-integrity, project-page and entry-page checks (`verify-docs.py`, `check-project-pages.py`, `check-entry-pages.py`). |
| `--staging` | none | off | Build with `SITE_URL=https://juneau.staged.apache.org`. |
| `--dry-run` | none | off | Print every command and copy that would run; do no work. Output is plain, never the live view. |
| `--verbose` | none | off | Print the full banner for each stage instead of one line. |
| `--juneau-root` | directory | `$JUNEAU_ROOT`, then `../master`, then `../juneau` | Juneau source checkout to build against. Must contain `pom.xml`. |
| `--console` | `full`, `board`, `none` | `board` | Console output when the Juneau checkout provides `juneau-terminal`; also `JUNEAU_TERMINAL_CONSOLE` (the flag wins). |

## What it does

1. Resolves the Juneau checkout (`juneau_root.find_juneau_root`) and changes into the docs directory.
2. With a Juneau checkout that has `juneau-terminal`, each stage below is a row of its live view (Clean, npm, Install, Site, Copy, Docs build, Verify, Links); skipped stages show as skipped rows. Without it, or with `--dry-run`, output is plain.
3. **Clean:** checks that `node`, `npm`, `mvn` and `java` are on the path, then deletes `build/`.
4. **npm:** runs `npm ci`. It is skipped automatically when a Docusaurus dev server is detected (a `docusaurus start` process, something listening on port 3000, or a `.dev-server-running` file), because both would corrupt the shared webpack cache. The Docusaurus build is skipped for the same reason.
5. When the Juneau checkout exists (or `--skip-maven` is not set), reads the current release with the Juneau checkout's `scripts/current-release.py`, and regenerates `artifact-paths.json` from the module `pom.xml` files (it refuses to overwrite the file if no jar-packaged module is found).
6. **Install and Site:** runs `mvn clean install -DskipTests` and `mvn site -DskipTests` in the Juneau checkout, then checks that `target/site/apidocs` exists.
7. **Copy:** copies `target/site/apidocs` to `static/javadocs/<version>/` and adds or updates the entry in `static/javadocs/releases.json`; unless `--skip-copy`, replaces `static/site` with the Maven site.
8. Regenerates `artifact-packages.json` (module to base javadoc package), then points `static/site/apidocs` at the committed `static/javadocs/<version>` with a relative symlink.
9. **Docs build:** runs `npm run build`, setting `JUNEAU_VERSION` and, with `--staging`, `SITE_URL`.
10. Copies the Juneau checkout's `.asf.yaml` into `build/` (not with `--skip-copy` or `--skip-maven`).
11. **Verify:** runs `verify-docs.py` (Integrity), `check-project-pages.py` (Project pages) and `check-entry-pages.py` (Entry pages, with `--juneau-root` when the Juneau checkout has a `pom.xml`), three child rows of the Verify stage.
12. **Links:** runs `check-topic-links.py --juneau-root <root>`, `check-ai-artifacts.py --juneau-root <root>`, `check-code-fences.py --docs-root <docs>`, `check-java-fences.py --docs-root <docs> --strict` (a ` ```java ` fence that does not parse fails the build unless it is marked ` ```java fragment `) and `check-guide-snippets.py --juneau-root <root>` (every ` guide=FILE#REGION ` fence must match its region in the Juneau checkout's `juneau-examples/juneau-examples-guides` module).
13. Prints the location of the build and, after a complete build with no skips, the stage and release commands.

## Output and exit codes

Progress is the `juneau-terminal` live view or plain stage banners; the finished site is in `build/`. Files written outside `build/`: `artifact-paths.json`, `artifact-packages.json` (both only when their content changed), `static/javadocs/<version>/`, `static/javadocs/releases.json`, `static/site/`.

| Code | Meaning |
|---|---|
| 0 | Build completed. |
| 1 | A prerequisite tool is missing, a command failed, the Maven site or apidocs were not produced, the Juneau checkout is invalid or missing, or the run was interrupted. |

## Gotchas

- It changes tracked files (`artifact-paths.json`, `artifact-packages.json`, `static/javadocs/`); check `git status` afterwards.
- A full build runs Maven over the whole Juneau tree and is slow; use the skip flags for iteration.
- The completion reminder is suppressed when `JUNEAU_PUSH_ACTIVE` is set and the live view is active.
- Warnings (missing `current-release.py`, missing javadocs directory) do not fail the build; the affected step is skipped.

## See also

- `verify-docs.py`, `check-project-pages.py`, `check-entry-pages.py`, `check-topic-links.py`, `check-ai-artifacts.py`, `check-code-fences.py`, `check-java-fences.py`, `check-guide-snippets.py`, `release-docs-stage.py`, `release-docs.py`, `start-docusaurus.py`
- Docs site: Developer Info, Script: build-docs (`pages/developer-info/04.03.03.ScriptBuildDocs.md`)
