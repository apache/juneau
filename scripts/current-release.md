# current-release.py

Prints the current Juneau release version taken from the root `pom.xml`, with any `-SNAPSHOT` suffix removed (for example `9.2.0`).

## When to use

- You need the release version as a plain string in a script or when writing release notes.
- For the installed Maven tool version, use `maven-version.py` instead.

## Usage

```bash
python3 scripts/current-release.py
```

## Options

None.

## What it does

1. Locates `pom.xml` in the repository root, the parent of the `scripts/` directory (`get_current_release`).
2. Runs `mvn help:evaluate -Dexpression=project.version -q -DforceStdout` in the root and, on success, strips `-SNAPSHOT` and returns the result.
3. If Maven fails for any reason, prints a warning to stderr and parses `pom.xml` directly, reading the project's own `<version>` element.
4. Prints the version.

## Output and exit codes

Stdout is the version string only. Warnings and errors go to stderr.

| Code | Meaning |
|---|---|
| 0 | A version was printed (from Maven or from the XML fallback). |
| 1 | `pom.xml` does not exist, or neither method produced a version. |

## Gotchas

- The Maven path needs `mvn` on the `PATH` and may download plugins on a fresh machine. The XML fallback reads only the project's own `<version>`, not an inherited parent version.
- Only the `-SNAPSHOT` suffix is removed; other qualifiers stay in the output.

## See also

- `maven-version.py`
- `release.py`
- Docs site: Developer Info, Script: current-release (`04.02.05.ScriptCurrentRelease.md` in juneau-docs)
