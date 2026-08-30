# scripts/tests/

This repo has no repo-local Python test suite. Its `scripts/todo-next-id.py` and
`scripts/todo-status-audit.py` (and their tests, `conftest.py` / `test_todo_next_id.py` /
`test_todo_status_audit.py`) were consolidated 2026-08-30 into a single parameterized copy at
`~/Project Work/scripts/` (`--project release-manager`), with tests alongside at
`~/Project Work/scripts/tests/`. See `@todo-and-waves` and `~/agents/AGENTS.md`.

This repo's own `push.py` (at the repo root, not under `scripts/`) is a bare add/commit/push
helper with no gates worth testing, and it has no `reset-side-clones.py` or
`repin-consumers.py` of its own (juneau's `test_push.py` / `test_reset_side_clones.py` /
`test_repin_consumers.py` cover those, and stay in juneau since the scripts they test do too).
