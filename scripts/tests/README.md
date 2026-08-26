# scripts/tests/

Pytest harness for the `scripts/` Python that has no other automated coverage:
`todo-next-id.py`, `todo-status-audit.py`, and the two juneau-only scripts noted below
(`push.py`'s tracker-audit gate and `reset-side-clones.py`). Everything here is hermetic --
temp directories, synthetic fixture files, and real temporary git repositories built in
fixtures -- and never touches the real `~/Project Work` trackers, the real `repos.md`, or the
real side clones.

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

- `test_push.py` -- **juneau-only exception** to the byte-for-byte-identical rule below: covers
  the `--tracker-audit` opt-in gate in `scripts/push.py`, which only exists in juneau's copy
  (release-manager's `push.py` is a bare add/commit/push helper with no gates at all;
  sandbox-support-console has no `push.py`). Its central assertion is that the off path never
  reaches `subprocess.run` at all -- not just that it returns early -- which is the strongest
  available proof the gate cannot affect push behavior, including runtime, while disabled.

- `test_reset_side_clones.py` -- **juneau-only exception** for the same reason: unlike the two
  tracker scripts, `reset-side-clones.py` is not a per-project script. It reads the one global
  `~/Project Work/repos.md` and drives clones by absolute path, so a single copy resets the
  juneau, console, and IRS pools and there is nothing for a second copy to specialize. The
  tests are about the guards, since that is where the script's value is: canonical-tree
  refusal by resolved real path (including via a symlink and via a `..` path), `in-flight`
  refusal, dirty/staged/untracked abort, fail-closed behavior on an unreadable or unparseable
  board, and the unreachability of `git clean -x` and of `git config`. Those two are asserted
  four ways -- the argv constant, a helper with no flag parameter at all, the wrapper refusing
  every other spelling, and an audit of every argv from a real end-to-end apply. Refusals are
  proven by monkeypatching `subprocess.run` to raise, so "it refused" means "no git ran"
  rather than "the exit code was 1". Everything else builds real temporary git repositories
  and lets real git run against them: a mocked test of a script whose entire risk is real git
  behavior would prove almost nothing. Temp repos get their identity and config isolation from
  environment variables, never `git config`, which is the same rule the script enforces.

Every OTHER file in this directory (i.e. all of the above except `test_push.py` and
`test_reset_side_clones.py`) is intended to be byte-for-byte identical across every repo that
carries `todo-next-id.py` / `todo-status-audit.py` -- the tests derive the project's id letter
and tracker slug from the scripts themselves rather than hardcoding them, so nothing here
needs to differ between copies.

**Intended** is not **actual**: the three copies of this README have diverged. Which repo is
ahead of which, by exactly what text, and what restoring identity requires is recorded in
`~/agents/AGENTS.md` under "Carried scripts (the three-repo set)" -- kept there rather than
here because it is a fact about all three copies at once, and because a note in this file
would itself be one of the things out of sync.
