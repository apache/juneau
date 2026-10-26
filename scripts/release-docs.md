# release-docs.py

Promotes the staged documentation (`asf-staging` branch) to production by force-pushing it to the `asf-site` branch.

## When to use

- The staged site at http://juneau.staged.apache.org looks right and should go live.
- To put a new build on staging first, use `release-docs-stage.py`.
- To rehearse everything except the final push, use `--no-push`.

## Usage

```bash
python3 scripts/release-docs.py --no-push
python3 scripts/release-docs.py
```

Run from the docs checkout. The second form makes the staged site live.

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--no-push` | none | off | Do everything except the final `git push`. |
| `--commit-message` | text | none | Accepted for consistency with `release-docs-stage.py`; not used. |
| `--juneau-root` | directory | `$JUNEAU_ROOT`, then `../master`, then `../juneau` | Juneau checkout whose `juneau-terminal` draws the live view. Not validated. |
| `--console` | `full`, `board`, `none` | `board` | Console output when `juneau-terminal` is available; also `JUNEAU_TERMINAL_CONSOLE` (the flag wins). |

## What it does

1. **Prechecks** (`promote`): git `user.email` must be the ASF committer identity `jamesbognar@apache.org`; the script cannot change git config and prints the commands to run.
2. Reads the `remote.origin.url` of the repository it is run from, and requires the parent of the docs checkout to exist.
3. **Fetch:** updates the persistent sibling checkout `../asf-site` (or clones the remote there, removing a directory that is not a git repository), then fetches `asf-staging`.
4. **Switch:** `git switch --detach origin/asf-staging` in that checkout, and copies the current git `user.name` and `user.email` into it.
5. **Push:** `git push origin HEAD:asf-site --force`. Skipped with `--no-push`; the manual command is then printed.
6. Plays a success sound when the exit code is 0.

## Output and exit codes

Progress is the `juneau-terminal` live view (rows Prechecks and Promote) or plain text with a warning that the site will go live. Nothing is built or committed; the pushed commit is whatever `asf-staging` points to.

| Code | Meaning |
|---|---|
| 0 | Promoted (or `--no-push` rehearsal completed). |
| 1 | The identity precheck failed, the remote URL or parent directory was missing, a git step failed, or the run was interrupted. |

## Gotchas

- This changes the live production site. The push is a force push.
- The remote URL is read from the current directory's repository, so run it from inside the docs checkout.
- `../asf-site` is a persistent checkout reused between runs.
- `--help` prints the module docstring as an epilog.

## See also

- `release-docs-stage.py`, `build-docs.py`
- Docs site: Developer Info, Script: release-docs (`pages/developer-info/04.03.02.ScriptReleaseDocs.md`)
