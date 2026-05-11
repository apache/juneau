# Replace `BasicBeanStore` / `BeanCreator` with v2 equivalents — remaining work

Phases 0–3 (most of the migration) are complete. See `FINISHED-15-replace-basicbeanstore-with-v2.md`
for the full history of completed work.

**Target release:** 9.5.0 — ship everything together; remove deprecated code in the same release.

---

## What still remains

### Phase 3 — Remaining consumer migrations

- [ ] **`BeanBuilder<T>` replacement** (tracked in **TODO-26**) — `org.apache.juneau.BeanBuilder<T>`
  (root package, not `cp`) is the base class for many domain fluent builders. Replace with
  `BeanInstantiator`. Separate plan required given blast radius.

- [ ] **`ContextBeanCreator`** stays in `org.apache.juneau.cp` — investigation showed it doesn't
  actually use legacy `BeanCreator` machinery (just `Context.createBuilder(Class)` reflection),
  and it solves a different problem than `BeanInstantiator` (persistent `Context.Builder` holder
  for repeated annotation application, not single-shot bean instantiation). Just remove its
  `@Deprecated(since = "9.5.0")` annotation in Phase 5 cleanup. Two consumers
  (`HttpPartParser.Creator`, `HttpPartSerializer.Creator`) keep extending it unchanged.

- [ ] **`juneau-marshall` public API surfaces** — several classes still reference `BeanCreator`:
  - `BeanFilter.java` — check usage, migrate or update.
  - `BeanBuilder.java` — still references `BeanCreator`; assess whether the reference is in live code or Javadoc only.
  - `Assertion.java` — check usage.
  - `RestClient.java` (rest-client module) — last `BeanCreator` reference; migrate.
  - `Rest_BeanCreatorOverrides_Test.java` — test of legacy behavior; update or retire after the above migrations land.
  - `BeanStore_Test.java` — exercises legacy class behavior; dies with Phase 4.

- [ ] **Survey `BasicBeanStore.Entry<T>`** — public/extendable entry record exposed via protected
  `createEntry(...)`. Confirm no external subclassers before deleting in Phase 4.

- [ ] **Diff `cp.BeanCreator` vs `commons.inject.BeanInstantiator`** — verify no legacy-only methods
  or behaviors still need to be ported before deleting `BeanCreator`.

### Phase 4 — Cutover rename (blocked on Phase 3 completion)

- [ ] Delete `org.apache.juneau.cp.BasicBeanStore`, `BeanCreator`, `BeanBuilder`,
  `BeanCreateMethodFinder`. (`ContextBeanCreator` stays — see Phase 3 note.)
- [ ] Rename `BasicBeanStore2` → `BasicBeanStore` (drop the `2` suffix); repo-wide find/replace.
- [ ] `BeanInstantiator` already has its final name — no rename needed.
- [ ] Re-run `./scripts/test.py -f`.

### Phase 5 — Cleanup

- [ ] Remove any transitional overloads not pruned in Phase 4.
- [ ] Drop the `@Deprecated(since = "9.5.0")` annotation from
  `org.apache.juneau.cp.ContextBeanCreator` (it is no longer slated for removal).
- [ ] Update `juneau-docs` (injection / BeanStore topics).
- [ ] Add release-notes entry to `juneau-docs/docs/pages/release-notes/9.5.0.md`:
  - Deletion of `org.apache.juneau.cp.{BasicBeanStore, BeanCreator, BeanBuilder, BeanCreateMethodFinder}`.
  - New canonical home at `org.apache.juneau.commons.inject.*` (un-suffixed names).
  - Migration guide for external consumers.

---

## Blocked on

- **TODO-26** — plan and implement `BeanBuilder<T>` → `BeanInstantiator` replacement.

Prerequisite for Phase 4 (deleting the legacy classes).
