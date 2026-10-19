# scripts/tests/

This repo's `scripts/todo-next-id.py` and `scripts/todo-status-audit.py` (and their tests,
`conftest.py` / `test_todo_next_id.py` / `test_todo_status_audit.py`) were consolidated
2026-08-30 into a single parameterized copy at `~/Project Work/scripts/` (`--project
release-manager`), with tests alongside at `~/Project Work/scripts/tests/`. See
`@todo-and-waves` and `~/agents/AGENTS.md`.

This repo's own `push.py` (at the repo root, not under `scripts/`) is a bare commit/push
helper with one gate worth testing (WORK-R0006): it refuses to commit or push **anything**
when the working tree has any unstaged modification to a tracked file, or any untracked file
that isn't gitignored (`_review()` / `commit_and_push()`, built on the `juneau_scripts` toolkit's `git` and `run` modules, loaded through the vendored `scripts/bootstrap.py`, which needs juneau-run 10.0.0-SNAPSHOT in `~/.m2` -- build Juneau first; the tests drive `push.main(argv, repo_dir)` and read its exit code).
A staged path is the operator's record of having reviewed it, so `push.py` commits the index
exactly as it finds it and never runs `git add`. `test_push.py` covers the three refusal
shapes (nothing-staged-while-dirty, staged+unstaged-tracked, staged+untracked-non-ignored)
plus the allowed staged-only case, the genuine-clean-tree no-op, and `--dry-run`'s real-outcome
reporting, against real temporary git repositories (a bare repo standing in for the remote).

This repo's `push.py` is its own thing — deliberately **not** carried byte-for-byte to
`apache/juneau` or `sandbox-support-console`. Those two repos have their own, independently
maintained `push.py` and `test_push.py` covering the same underlying gate (same three refusal
shapes) plus their own repo-specific build/test machinery that this repo's helper does not
have; this repo also has no `reset-side-clones.py` or `repin-consumers.py` of its own
(juneau's `test_reset_side_clones.py` / `test_repin_consumers.py` cover those, and stay in
juneau since the scripts they test do too).

## Running

`pytest` is not vendored anywhere in this repo. Two easy ways to run this suite without
installing anything globally:

```bash
# Option A -- uv (fastest, no setup, nothing left behind):
uv run --with pytest pytest scripts/tests/test_push.py

# Option B -- a local venv:
python3 -m venv .venv
.venv/bin/pip install pytest
.venv/bin/pytest scripts/tests/test_push.py
```

Do **not** `pip install --user pytest` / `pip install pytest` on a Homebrew-managed `python3`
— PEP 668 ("externally managed environment") will refuse it.

## Release CLI tests

`test_release_*.py` cover `scripts/release.py` and the `scripts/jrm_release/` package. They use fakes only
(`conftest.py`: FakeRunner, FakePrompts, FakeNexus, FakeHttp), so no network, Nexus, SVN, GitHub, gpg key or
real release is touched.

| File | Covers |
|---|---|
| `test_release_runfile.py` | the `run.json` checkpoint (atomic write, corrupt-file refusal) |
| `test_release_steps.py` | each rc, vote and promote step against the fakes |
| `test_release_emails.py` | vote, result and announce emails, `mailto:` length handling |
| `test_release_revert.py` | `--revert` checks and actions |
| `test_release_cli.py` | arguments, guards, lock, resume, exit codes (needs `JUNEAU_RUN_PY`) |
| `test_release_dryrun.py` | `--dry-run` runs read-only commands only and never writes `run.json` |
| `test_release_secrets.py` | no secret on argv, in `run.json` or in logs; `askpass.py` |

`test_release_cli.py` loads the shared toolkit through `scripts/bootstrap.py`, so point it at the Juneau checkout:

```bash
export JUNEAU_RUN_PY=$HOME/git/apache/juneau/juneau-run/src/main/python/juneau_run.py
uv run --offline --with pytest pytest -q -p no:cacheprovider scripts/tests/
```
