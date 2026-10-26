# bootstrap.py

Vendored copy of the juneau-terminal loader. Push and release scripts import it to load the `juneau_terminal_scripts`
toolkit and, when asked, `juneau_terminal`. Run directly, it prints the path of a juneau-terminal bin script.

## When to use

- Importing the toolkit from a script here: `from bootstrap import load`.
- Finding a toolkit bin script (for example `watch-run.sh`) from the shell.
- Refreshing it after a Juneau upgrade: copy the file from the installed `juneau-terminal` zip. Never hand-patch it.

## Usage

```bash
python3 scripts/bootstrap.py --bin watch-run.sh
unzip -p ~/.m2/repository/org/apache/juneau/juneau-terminal/<v>/juneau-terminal-<v>.zip bootstrap.py > scripts/bootstrap.py
```

## Options

| Option | Meaning |
|---|---|
| `--bin` | Bin script name (`NAME`), for example `watch-run.sh`. Required. |
| `--pom` | Pom file with `<juneau.version>`. Default: the nearest `pom.xml` above `bootstrap.py`. |
| `--cache-dir` | Directory where `mvn dependency:copy` caches the zip. Default: `target/juneau-terminal` beside the pom. |
| `--version` | juneau-terminal version. Default: the pom's `<juneau.version>`. |

## What it does

1. Finds the toolkit: `JUNEAU_TERMINAL_SCRIPTS_ZIP` and `JUNEAU_TERMINAL_PY`, then an in-tree `juneau-terminal/src/main/python/`, then `~/.m2`, then a `mvn dependency:copy` into the cache directory.
2. As a module, `load()` returns the `juneau_terminal` and `juneau_terminal_scripts` modules.
3. As a script, prints the path of the named bin script.

## Output and exit codes

| Code | Meaning |
|---|---|
| 0 | Printed the bin script's path. |
| 1 | No toolkit found; the reason goes to stderr. |
| 2 | `--bin` missing; usage goes to stderr. |

## Gotchas

- This is a vendored copy; the source of truth is `juneau-terminal/src/main/python/bootstrap.py` in Apache Juneau.
- For a `-SNAPSHOT` version the copy runs once per process, so a freshly installed toolkit is picked up.

## See also

- `scripts/push.py`, which loads the toolkit through this file.
