# FINISHED-16: RestContext / RestOpContext builder-to-memoizer migration (complete)

This file is the consolidated closeout for TODO-16 and supersedes the per-phase archive split across:

- `FINISHED-16a-per-setting-inventory.md`
- `FINISHED-16b-phases-1-2-execution.md`
- `FINISHED-16c-phase-3-builder-deletion.md`

It captures the final state after all follow-up cleanup/doc tasks were completed.

---

## Scope

TODO-16 replaced legacy builder-driven configuration in `RestContext` and `RestOpContext` with context-owned memoized resolution (`Memoizer<T> + findXxx()`), migrated annotation/apply paths away from builder-only slots, and removed or demoted builder-era APIs/protocols that were no longer needed.

---

## Final outcomes

- Converted the target settings from eager builder fields to memoized `findXxx()` resolution on `RestContext` / `RestOpContext`.
- Preserved behavior for inheritance, annotation precedence, and bean-store override patterns while removing dead or redundant builder wiring.
- Deleted obsolete builder-centric protocols/factories where migration paths exist (including `RestContext.create(...)` / `RestOpContext.create(...)` and `@RestInit(...Builder)` injection variants).
- Introduced and adopted `RestContextInit` constructor-based bootstrap flow for programmatic context creation.
- Kept compatibility paths where intentionally deferred (documented in phase archives), while making builder classes private/package-private and unreachable as public API.

---

## Work completed by phase

### Phase 0 inventory and decisions

- Completed the per-setting inventory, migration questions, and decision record.
- See full detail in `FINISHED-16a-per-setting-inventory.md`.

### Phases 1 and 2 execution

- Landed simple-setting and composite-setting migrations.
- Hardened tests/coverage and fixed follow-on regressions discovered during branch-coverage passes (including annotation precedence and URI `noInherit` gating fixes).
- See full detail in `FINISHED-16b-phases-1-2-execution.md`.

### Phase 3 builder-deletion track

- Landed pre-flight cleanup decisions (#24/#25/#26) and additive migration path based on `RestContextInit`.
- Migrated callsites and removed the builder-injection protocols/factories as documented.
- See full detail in `FINISHED-16c-phase-3-builder-deletion.md`.

### Final D-5 cleanup (post phase archives)

- Swept stale Javadoc links pointing to deleted/legacy `RestContext.Builder#...` and `RestOpContext.Builder#...` members across `juneau-rest-server`.
- Verified and cleared residual `@Deprecated` markers tied to removed builder methods.
- Updated release notes with builder-removal migration guidance (current release-notes stream in `juneau-docs`).
- Updated the v9.5 migration guide with apply-pass replacement guidance.
- Addressed related static analysis follow-ups during closeout (null-flow/Sonar and Javadoc link resolution fixes encountered in touched files).

---

## Current status

- **TODO-16 is complete.**
- Remaining references under the `FINISHED-16a/b/c` files are historical execution narrative and rationale, not open work.
- Future incremental cleanups can be tracked under new TODO IDs as needed, but TODO-16 itself is closed.

