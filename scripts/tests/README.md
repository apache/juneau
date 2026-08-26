# scripts/tests/

Pytest harness for `scripts/todo-next-id.py` and `scripts/todo-status-audit.py`. These two
scripts have no other automated coverage today; everything here is hermetic (temp
directories, synthetic fixture files) and never touches the real `~/Project Work` trackers.

## Running

`pytest` is not vendored anywhere in this repo (it's pure-stdlib elsewhere in `scripts/` on
purpose). Two easy ways to run this suite without installing anything globally:

```bash
# Option A -- uv (fastest, no setup, nothing left behind):
uv run --with pytest pytest scripts/tests

# Option B -- a local venv:
python3 -m venv .venv
.venv/bin/pip install pytest
.venv/bin/pytest scripts/tests
```

Do **not** `pip install --user pytest` / `pip install pytest` on a Homebrew-managed
`python3` -- PEP 668 ("externally managed environment") will refuse it (or force
`--break-system-packages`, which this repo's tooling should not depend on).

## Layout

- `conftest.py` -- loads the two hyphenated scripts as fresh module objects per test (they
  can't be `import`ed by name) via `next_id_module` / `audit_module` fixtures.
- `test_todo_next_id.py` -- the id allocator: concurrent-claim uniqueness (25/30/60-way),
  every recognized filename prefix (including the `finished/` archive), the
  unrecognized-prefix id-reuse hazard, and bounded retry/collision behavior.
- `test_todo_status_audit.py` -- the status/header pre-filter: every documented prefix is
  actually scanned (cross-checked against the module's own docstring, not hand-duplicated),
  every documented reason code is actually emittable (same cross-check, one level up),
  `empty_status_value` fires only on genuinely blank/decoration-only values, markdown
  decoration doesn't defeat the lifecycle checks, and the TODO/READY/MAYBE/HOLD
  lifecycle-mismatch signals. The `ready_but_wave_already_accepted` wave cross-check carries
  both directions: it fires on an item still queued behind an accepted-and-closed wave, and
  stays silent on the four false-positive shapes that exist in the real corpus -- a body-only
  cross-reference to another wave's members, a non-`Ready to execute` umbrella that names the
  wave in its header, a wave still in flight (live `waves/WAVE-nnnn-*.md`), and a negated
  board line ("not accepted"). A regression test also pins that naming pre-existing symbols is
  never a signal, since this check reads tracker records only and never the source tree.

This file (and every file in this directory) is intended to be byte-for-byte identical
across every repo that carries `todo-next-id.py` / `todo-status-audit.py` -- the tests derive
the project's id letter and tracker slug from the scripts themselves rather than hardcoding
them, so nothing here needs to differ between copies.
