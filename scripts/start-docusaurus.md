# start-docusaurus.py

Starts the Docusaurus development server for the docs site on port 3000, after freeing the port and linking the committed javadocs.

## When to use

- You are editing pages and want a live-reloading local preview.
- The preview looks stale or webpack misbehaves: add `--clean`.
- For a production build, use `build-docs.py`; it will not run the Docusaurus build or `npm ci` while this server is running.

## Usage

```bash
python3 scripts/start-docusaurus.py
python3 scripts/start-docusaurus.py --clean
```

Then open http://localhost:3000.

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--clean` | none | off | Remove `.docusaurus/`, `build/` and `node_modules/.cache` before starting. |

## What it does

1. Requires `package.json` in the docs checkout; runs `npm install` if `node_modules` does not exist.
2. If the lock file `.dev-server-running` is present, assumes another instance is running, prints a warning and exits 0.
3. Kills every process listening on port 3000 (`lsof -ti:3000`, then `kill -9`), and waits two seconds. On Windows it uses `netstat` and `taskkill`.
4. With `--clean`, removes the cache directories.
5. Points `static/site/apidocs` at the committed `static/javadocs/<version>` with a relative symlink (the version is the newest entry of `static/javadocs/releases.json` that has a directory, or the only versioned directory). Missing javadocs only produce a warning.
6. Creates the `.dev-server-running` lock file, runs `npm start`, and removes the lock file when the server stops.

## Output and exit codes

Progress lines on the console, then the Docusaurus server output until it stops.

| Code | Meaning |
|---|---|
| 0 | Server stopped with Ctrl-C, or another instance was already running. |
| 1 | `package.json` is missing, `npm install` failed, or `npm start` failed. |

## Gotchas

- It force-kills whatever is on port 3000, not just a previous Docusaurus server.
- `.dev-server-running` is how `build-docs.py` knows to skip `npm ci` and the production build. A crash that skips the cleanup leaves a stale lock; delete the file if the script refuses to start.
- The `/site/` pages (Maven site) exist only after `create-mvn-site.py` or a full build; only the javadocs link is set up here.

## See also

- `build-docs.py`, `create-mvn-site.py`
- Docs site: Developer Info, Script: start-docusaurus (`pages/developer-info/04.03.05.ScriptStartDocusaurus.md`)
