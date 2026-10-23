# docs-images.py

Regenerates the generated images of the docs site (petstore UI screenshots and terminal-capture SVGs) from case lists, or checks that the images and the pages that reference them agree.

## When to use

- A petstore page or a `juneau-run` display changed and its documentation image must be refreshed.
- You want to know whether every generated image exists and every image reference in `pages/` resolves: use `--check` (it writes nothing).
- You are adding a screenshot: add a case to `docs-images/ui-cases.json` or `docs-images/terminal-cases.json`, then run this script for just that case.

## Usage

```bash
python3 scripts/docs-images.py                         # regenerate every image
python3 scripts/docs-images.py --check                 # write nothing; report problems
python3 scripts/docs-images.py --source terminal       # one source only
python3 scripts/docs-images.py --only 'PetstoreThemes*' # matching cases (globs)
python3 scripts/docs-images.py --base-url http://localhost:10000
python3 scripts/docs-images.py --juneau-root ~/git/apache/juneau
```

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--check` | none | off | Write nothing. Report `MISSING`, `ORPHANED`, `DANGLING` and `UNREFERENCED` lines and exit 1 if any error was found. |
| `--only` | one or more patterns | all cases | Case ids or image names (globs, matched against the id, the image path or the image file name). It is an error if nothing matches. |
| `--source` | `ui`, `terminal` | both | Regenerate one source only. |
| `--port` | number | `10000` | Port of the petstore to reuse or start. Ignored with `--base-url`. |
| `--base-url` | URL | reuse or start one | An already-running petstore to shoot. |
| `--juneau-root` | directory | `$JUNEAU_ROOT`, then `../master`, then `../juneau` | Juneau source checkout. |
| `--docs-root` | directory | parent of `scripts/` | Docs checkout. Hidden from `--help`; for tests. |

## What it does

1. Loads `scripts/docs-images/ui-cases.json` and `terminal-cases.json` (`load_cases`). Each case has an `id` and an `image` (path relative to `static/`, for example `topics/PetstoreThemes.Stock.1.png`). The file's `defaults` are merged into each case. Duplicate ids, or two cases writing the same image, are errors.
2. **`--check`:** compares the cases with `static/` and `pages/` (`check`) and prints one `KIND path: detail` line per problem:
   - `MISSING`: a case whose image does not exist;
   - `ORPHANED`: an image under `static/topics` or `static/developer-info` that no case produces and the `unmanaged` globs do not exempt;
   - `DANGLING`: an image reference in `pages/` to a file that does not exist;
   - `UNREFERENCED`: a case whose image no page references (a warning; does not fail the check).
3. **Terminal cases:** for each case, starts its `command` (the placeholders `{juneau_run}` and `{fixtures}` expand to the Juneau checkout's `juneau_run.py` and `scripts/docs-images/fixtures`) in a detached `tmux` session of fixed size on a private tmux server. It waits for the case's `waitFor` regex to appear and the pane to stop changing, captures it with `tmux capture-pane -e -p`, applies the `mask` regex replacements, and renders SVG text. A case with `panes` renders its panes side by side. Defaults are 100 columns, 30 rows and a 60 second timeout.
4. **UI cases:** reuses a petstore already answering on `http://localhost:<port>`, or starts `<juneau-root>/scripts/start-petstore-jetty.py --port <port>` with the clock fixed (`PETSTORE_FIXED_NOW=seed`) and stops it on exit. Installs Playwright and Chromium under `<juneau-root>/juneau-petstore/juneau-petstore-core/target/js` if missing, then runs the Juneau checkout's `petstore-browser.cjs` harness over the cases (`path`, `actions`, `selector`, `viewport`, `waitUntil`, `initScript`).
5. Moves images into `static/` only after every case of the run succeeded.

## Output and exit codes

Progress lines (`->` steps, then `static/<image>` for each image written) on stdout; errors on stderr as `ERROR: ...`. `--check` prints its findings and a count line. Images are written under `static/`.

| Code | Meaning |
|---|---|
| 0 | Generated, nothing to generate, or `--check` found no errors. |
| 1 | `--check` found errors, or a generation step failed (missing tool, case failure, port in use, bad cases file). |
| 130 | Interrupted; no image was replaced. |

## Gotchas

- Terminal cases need `tmux` on the path; UI cases need `node` and `npm`.
- A petstore not started by this script does not run on the fixed clock, so shots showing the date or time of day may drift; the script warns and skips cases marked `freshServerOnly`.
- If the port is occupied by something that is not the petstore, the run stops and names the process; pass `--port` or `--base-url`.
- A failed UI run leaves the old images; nothing is half-replaced.
- The first run can take several minutes (petstore build, Playwright and Chromium download).

## See also

- `docs-images/fixtures/` (inputs played back by terminal cases; not entry points), `start-docusaurus.py`
