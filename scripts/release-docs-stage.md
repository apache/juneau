# release-docs-stage.py

Builds the documentation for staging and publishes the result to the `asf-staging` branch, which serves http://juneau.staged.apache.org.

## When to use

- You have finished a docs change and want to preview the built site on the staging host before it goes live.
- You want to rehearse the whole thing without pushing: use `--no-push`.
- To make staging live on the production branch, run `release-docs.py` afterwards.

## Usage

```bash
python3 scripts/release-docs-stage.py
python3 scripts/release-docs-stage.py --no-push
python3 scripts/release-docs-stage.py --commit-message "Stage docs for 10.0.0" --juneau-root ~/git/apache/juneau
```

Run from the docs checkout.

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--no-push` | none | off | Build and commit in the staging checkout but do not push, and do not open the browser. |
| `--commit-message` | text | `Deploy documentation staging` | Commit message for the staging commit. |
| `--juneau-root` | directory | `$JUNEAU_ROOT`, then `../master`, then `../juneau` | Juneau source checkout; passed on to `build-docs.py`. |
| `--use-existing-build` | none | off | Stage the existing `build/` directory as it is: no build-docs.py run, and no build rows. It must be a `--staging` build. `scripts/push.py` stages its own build this way. |
| `--console` | `full`, `board`, `none` | `board` | Console output when the Juneau checkout provides `juneau-terminal`; also `JUNEAU_TERMINAL_CONSOLE` (the flag wins). Passed on to `build-docs.py`. |

## What it does

1. **Prechecks** (`release_stage`): git `user.email` must be the ASF committer identity `jamesbognar@apache.org` (the script cannot change git config and prints the commands to run); a docs branch that is behind its upstream, detached, or not checkable is only a warning.
2. Runs `build-docs.py --staging` (with `--juneau-root` and `--console` when given; `build_for_staging`) in the docs checkout and stops if it fails or `build/` is missing. With `--use-existing-build` this step is skipped and `build/` must already exist.
3. (`publish_build`, which `scripts/push.py` also calls.) Reads the `remote.origin.url` of the docs checkout, and requires the parent of the docs checkout to exist.
4. **Fetch** (`fetch_staging`): updates the persistent sibling checkout `../asf-staging`, or clones the remote there (removing a directory that is not a git repository), fetches `asf-staging`, and checks out `asf-staging` (creating the branch if it does not exist yet).
5. **Copy** (`copy_build`): deletes everything in the staging checkout except `.git`, then copies the contents of `build/` into it.
6. **Commit** (`commit_staging`): copies the current git `user.name` and `user.email` into the staging checkout, runs `git add -A`, and commits when there is a staged change.
7. **Push:** `git push origin asf-staging --force`, then opens the staging URL in a browser. With `--no-push`, both are skipped and the manual push command is printed.
8. Prints the staged commit (`Staged commit: <sha> <subject>`).
9. Plays a success or failure sound.

## Output and exit codes

Progress is the `juneau-terminal` live view (rows Prechecks, the build rows, Stage push and URL) or plain text. The commit lives in `../asf-staging`.

| Code | Meaning |
|---|---|
| 0 | Staged (and pushed unless `--no-push`). |
| 1 | A precheck failed, the build failed, a git step failed, or the run was interrupted. |

## Gotchas

- The push is a force push to `asf-staging`; the staging branch is rewritten from the build each time.
- The staging checkout `../asf-staging` is persistent and reused. Do not keep other work in it.
- The remote URL is read from the docs checkout.
- A full build runs first and is slow; see `build-docs.py`.
- `--help` prints the module docstring as an epilog.

## See also

- `build-docs.py`, `release-docs.py`, `push.py` (gates, pushes `docs`, then stages the same build)
- Docs site: Developer Info, Script: release-docs-stage (`pages/developer-info/04.03.01.ScriptReleaseDocsStage.md`)
