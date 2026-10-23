# maven-version.py

Prints the major version number of the Maven tool installed on this machine (for example `3`).

## When to use

- A script or a human needs to know which Maven major version is on the `PATH` before a version-sensitive build step.
- For the Juneau project version, use `current-release.py` instead; this script reports the Maven tool, not the project.

## Usage

```bash
python3 scripts/maven-version.py
```

## Options

None.

## What it does

1. Runs `mvn -version` and reads stdout, or stderr when stdout is empty (`get_maven_version`).
2. Looks for `Maven <major>.<minor>` in the text.
3. Falls back to a `<major>.<minor>` at the very start of the text.
4. Prints the major number.

## Output and exit codes

Stdout is one integer, such as `3`. Errors go to stderr.

| Code | Meaning |
|---|---|
| 0 | A major version was found and printed. |
| 1 | `mvn` is not on the `PATH`, no version could be parsed from its output, or any other exception. |

## Gotchas

- It needs `mvn` on the `PATH`; the Maven wrapper in the repository is not used.
- It reports only the major number. `3.9.5` prints `3`.

## See also

- `current-release.py`
- Docs site: Developer Info, Script: maven-version (`04.02.06.ScriptMavenVersion.md` in juneau-docs)
