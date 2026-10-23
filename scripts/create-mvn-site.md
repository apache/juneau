# create-mvn-site.py

Builds the Maven site, including the Javadocs, and copies it into the documentation tree's static directory so it can be previewed locally.

## When to use

- You changed Javadoc or site content and want to see it rendered, or to check links.
- You need `docs/static/site` refreshed for a local documentation preview.

## Usage

```bash
python3 scripts/create-mvn-site.py
```

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--help`, `-h` | none | off | Print usage and exit 0 without building or removing anything. |
| `--dry-run` | none | off | Print what would be removed and run, then exit 0 without doing it. |

Any other argument exits 2 with usage and does nothing.

## What it does

1. Reads the project version with `mvn help:evaluate -Dexpression=project.version`; on failure it prints a warning and continues with `unknown` (`get_project_version`).
2. Removes `target/site` if it exists.
3. Runs `mvn clean compile site` in the repository root, streaming the output to the console and to `create-mvn-site.log` in the root (`run_maven_command`).
4. Fails if `target/site` was not produced.
5. Deletes `docs/static/site`, recreates it, and copies everything from `target/site` into it.
6. Prints a success banner with the preview address.

## Output and exit codes

Timestamped progress lines and the full Maven output on stdout. The Maven output is also written to `create-mvn-site.log` in the repository root, overwritten on each run.

| Code | Meaning |
|---|---|
| 0 | The site was built and copied, or `--help` was shown. |
| 2 | An unknown argument was given; nothing was run or removed. |
| 1 | Maven failed, `target/site` was not created, or the copy failed. |

## Gotchas

- Arguments are parsed first, so `--help` is safe to run; any run without arguments starts the full build.
- It runs `mvn clean`, so existing build output in the tree is wiped.
- The copy target is `docs/static/site` in this checkout, and any previous contents are deleted first.
- The success banner mentions `http://localhost:3000/site/`, which is the Docusaurus dev-server address; the script does not start that server.
- It needs `mvn` on the `PATH` and takes several minutes.

## See also

- Docs site: Developer Info, Script: create-mvn-site (`04.03.04.ScriptCreateMvnSite.md` in juneau-docs)
