# check-bom-completeness.py

Checks that `juneau-bom/pom.xml` lists every consumer-facing reactor module, and nothing that is not one.

## When to use

- You added or removed a publishable module and want to confirm the BOM matches.
- A build gate reports a BOM mismatch and you want the detail. `push.py` runs this script before the tests.

## Usage

```bash
python3 scripts/check-bom-completeness.py
```

## Options

None.

## What it does

1. Finds the repository root as the parent of `scripts/` and requires `juneau-bom/pom.xml` (`main`).
2. Walks every `pom.xml` under the root (`collect_publishable`). It skips poms under a `target/` directory, under `juneau-shaded/`, `juneau-examples/`, `juneau-petstore/` and `juneau-distrib/`, and poms whose `artifactId` is in the exclusion set (the BOM itself, the test-support modules, `juneau-integration-tests`, `juneau-terminal`, `juneau-microservice-examples`, and a few others, listed in `EXCLUDED_ARTIFACTS`). It also skips aggregators (packaging `pom` with `<modules>`). Unparseable poms produce a warning on stderr.
3. Reads the `org.apache.juneau` artifacts from the BOM's `<dependencyManagement>` (`collect_bom_entries`).
4. Compares the two sets: modules missing from the BOM, and BOM entries that are not publishable modules.

## Output and exit codes

On success, one line: `OK: juneau-bom enumerates all <n> consumer-facing modules.` On failure, the missing and stale artifact ids go to stderr, followed by a fix hint.

| Code | Meaning |
|---|---|
| 0 | The BOM is complete and has no stale entries. |
| 1 | The BOM file is missing, a module is missing from the BOM, or the BOM lists a non-publishable artifact. |

## Gotchas

- When a module is intentionally unpublished, the fix is to add it to `EXCLUDED_ARTIFACTS` or `EXCLUDED_PATH_PREFIXES` in the script, not to the BOM.
- Only the project's own `artifactId` counts; the parent's is ignored.
- The scan is a plain `rglob` over the tree, so it also looks at poms in untracked directories.

## See also

- `push.py`, which runs this check before the tests
- `check-container-tags.py`
