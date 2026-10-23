# create-mvn-site.py

Generates the Juneau Maven site (with javadocs) and copies it into the docs checkout's `static/site` directory so the local Docusaurus server and link checks can use it.

## When to use

- You run the dev server (`start-docusaurus.py`) and need `/site/...` and javadoc links to resolve locally.
- You only need the site refreshed, without the Docusaurus build or checks that `build-docs.py` also performs.

## Usage

```bash
python3 scripts/create-mvn-site.py
python3 scripts/create-mvn-site.py --juneau-root ~/git/apache/juneau
```

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--juneau-root` | directory | `$JUNEAU_ROOT`, then `../master`, then `../juneau` next to the docs checkout | Juneau source checkout to build. Must contain `pom.xml` when given explicitly. |

## What it does

1. Resolves the Juneau checkout (`juneau_root.find_juneau_root`).
2. Reads the project version with `mvn help:evaluate -Dexpression=project.version` and prints it (`unknown` if that fails).
3. Deletes the checkout's `target/site`.
4. Runs `mvn clean compile site` in the checkout, echoing the output to the console and to `create-mvn-site.log` in the checkout root.
5. Fails if `target/site` was not produced.
6. Deletes the docs checkout's `static/site`, recreates it, and copies every file and directory from `target/site` into it.
7. Prints a success banner with the local URL `http://localhost:3000/site/`.

## Output and exit codes

Console output is the Maven output plus timestamped progress lines. Files written: `<juneau-root>/create-mvn-site.log` and `static/site/`.

| Code | Meaning |
|---|---|
| 0 | Site generated and copied. |
| 1 | Maven failed, `target/site` is missing, the copy failed, or the Juneau checkout could not be found. |

## Gotchas

- `mvn clean compile site` cleans the whole Juneau checkout's `target` directories.
- It does not run `install` or tests. `build-docs.py` runs `mvn clean install -DskipTests` before `mvn site`.
- The log file is written into the Juneau checkout, not the docs checkout.
- `static/site` is replaced wholesale.

## See also

- `build-docs.py`, `start-docusaurus.py`
- Docs site: Developer Info, Script: create-mvn-site (`pages/developer-info/04.03.04.ScriptCreateMvnSite.md`)
