# push.py

Gates, commits and pushes the `docs` branch of juneau-docs, then stages the same build to `asf-staging`.

## When to use

- You have staged your reviewed docs changes and want them checked, pushed and previewed on http://juneau.staged.apache.org in one command (`push juneau-docs`).
- You want the checks and the push without a deploy: use `--no-stage`.
- To make staging live, run `release-docs.py` afterwards. This script never does.

## Usage

```bash
python3 scripts/push.py "Rewrote the REST client topic"
python3 scripts/push.py "Fix a typo" --no-stage
python3 scripts/push.py "Stage with a note" --stage-message "Stage docs for 10.0.0"
python3 scripts/push.py "Try it" --dry-run
```

Run from the docs checkout. The commit message is prompted for when omitted.

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `message` | text (optional positional) | prompted | Git commit message for `docs`. An empty message aborts with exit 1. |
| `--no-stage` | none | off | Gate, commit and push only. The build runs with `--skip-maven` (no Maven site, so no Juneau build) and nothing is staged. |
| `--dry-run` | none | off | Print the steps and run the Prechecks. Nothing is built, committed or pushed. |
| `--stage-message` | text | `Deploy documentation staging` | Commit message for the `asf-staging` commit. |
| `--juneau-root` | directory | `$JUNEAU_ROOT`, then `../master`, then `../juneau` | Juneau source checkout, for the Maven site, the sibling-checkout checks and the `juneau-terminal` view. Required for a staged push. |
| `--console` | `full`, `board`, `none` | `board` | Console output when the Juneau checkout provides `juneau-terminal`; also `JUNEAU_TERMINAL_CONSOLE` (the flag wins). Passed on to `build-docs.py`. |

## What it does

1. **Prechecks** (`precheck_list`): git `user.email` is `jamesbognar@apache.org`; the branch is `docs`; no unstaged or untracked-and-not-ignored file (`unreviewed_problem`); the branch has an upstream and is not behind it.
2. **Build and checks:** runs `build-docs.py --staging` (or `--skip-maven` with `--no-stage`). `build-docs.py` is the single list of checks: verify-docs, check-project-pages, check-entry-pages, check-doc-inventory, check-topic-links, check-ai-artifacts, check-code-fences, check-java-fences and check-guide-snippets, then the Docusaurus build. The sibling-checkout checks run whenever the Juneau checkout exists. Stale AI artifacts fail the run; regenerate them with `python3 scripts/generate-ai-artifacts.py`. This script never regenerates. A build that rewrites tracked files (artifact maps, the javadoc index) only produces a warning, since only the index is committed.
3. **Commit:** commits the index exactly as found (`git commit`). It stages nothing. With nothing staged the row is skipped.
4. **Push:** `git push` to the upstream, when there is a new commit or unpushed commits.
5. **Stage:** unless `--no-stage`, `release-docs-stage.py`'s `publish_build` stages the build from step 2 (no second build): fetch `../asf-staging`, copy `build/`, commit, force-push `asf-staging`, open the URL. The staged commit is printed at the end.

## Output and exit codes

Progress is the `juneau-terminal` live view (Prechecks, the build rows, Commit, Push, Stage push, URL) or plain text when that is unavailable.

| Code | Meaning |
|---|---|
| 0 | Pushed (and staged unless `--no-stage`), or `--dry-run` finished with passing Prechecks. |
| 1 | A precheck, the build or a check failed, or a git step failed. Nothing is committed when a gate fails. |
| 130 | Cancelled with Ctrl-C. |

## Gotchas

- The Maven steps inside its `build-docs.py` run wait for the shared Maven lock (see `build-docs.md`), so starting this together with `push.py` in the Juneau checkout is safe: the second waits for the first's Maven span.
- Everything outward-facing happens here: it pushes `docs` and force-pushes `asf-staging`. Run it only when you mean to.
- It refuses on any unstaged or untracked file. Stage what you have reviewed, or `git stash push -- <paths>` the rest.
- A staged push runs the full Maven build of the Juneau checkout (slow); `--no-stage` avoids it.
- The checks run before the commit, so a failed check leaves your staged work untouched.
- `release-docs.py` (production) is never called.

## See also

- `build-docs.py`, `release-docs-stage.py`, `release-docs.py`
- Juneau's `scripts/push.py`, which no longer touches juneau-docs; it prints a hint to run this script.
