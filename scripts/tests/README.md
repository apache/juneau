# scripts/tests/

This repo's `scripts/todo-next-id.py` and `scripts/todo-status-audit.py` (and their tests,
`conftest.py` / `test_todo_next_id.py` / `test_todo_status_audit.py`) were consolidated
2026-08-30 into a single parameterized copy at `~/Project Work/scripts/` (`--project
release-manager`), with tests alongside at `~/Project Work/scripts/tests/`. See
`@todo-and-waves` and `~/agents/AGENTS.md`.

This repo's own `push.py` (at the repo root, not under `scripts/`) is a bare commit/push
helper with one gate worth testing (WORK-R0006): it refuses to commit or push **anything**
when the working tree has any unstaged modification to a tracked file, or any untracked file
that isn't gitignored (`check_unreviewed_changes()`/`get_staged_paths()`/`commit_and_push()`).
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
