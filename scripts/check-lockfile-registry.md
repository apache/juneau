# check-lockfile-registry.py

Read-only check that every `resolved` URL in `e2e/package-lock.json` is on the public npm registry (`https://registry.npmjs.org/`). Standard library only.

## When to use

- After `npm install` or any lockfile change in `e2e/`, to catch a local npm mirror setting that leaked into the lock.
- The `e2e/.npmrc` file pins the registry for new installs; this script catches a lockfile that was edited or generated some other way.

## Usage

```bash
python3 scripts/check-lockfile-registry.py
python3 scripts/check-lockfile-registry.py --lockfile path/to/package-lock.json
```

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--lockfile` | file | `e2e/package-lock.json` | The lockfile to check. |

## What it does

1. Reads the lockfile line by line.
2. Reports every `"resolved"` URL that does not start with `https://registry.npmjs.org/`, with its file, line and URL.
3. Prints a one-line summary.

## Output and exit codes

Stdout: one line per offending URL, then a summary line. Nothing is written.

| Code | Meaning |
|---|---|
| 0 | Every `resolved` URL is on the public npm registry. |
| 1 | At least one `resolved` URL is elsewhere. |
| 2 | The lockfile was not found. |

## Gotchas

- Only `resolved` fields are checked; `integrity` hashes and everything else are ignored.
- `push.py` applies the same rule (through `bad_urls`) at the start of its Browser Tests row, so a push fails on a bad lockfile; run this script by hand to see the offending lines sooner.

## See also

- `push.py` (its Browser Tests row runs this check before installing the `e2e/` npm dependencies)
