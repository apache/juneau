# Replace `RestContext` / `RestOpContext` Builder configuration with memoized fields

Shift the REST server configuration model away from large, stateful `RestContext.Builder` and `RestOpContext.Builder` classes, and toward **memoized fields directly on `RestContext` and `RestOpContext`**, following the pattern established by `RestContext.allowedParserOptions`:

```java
private final Memoizer<SortedSet<String>> allowedParserOptions = memoizer(this::findAllowedParserOptions);
```

Each logical setting becomes a `Memoizer<T>` backed by a `findXxx()` method that reads from `@Rest(...)` / `@RestOp(...)` annotations, system properties, environment, `@RestInject`-supplied beans, and (where applicable) parent-context fallbacks. Values are computed lazily on first access and are bootstrap-immutable thereafter (no `reset()` is exposed in 9.5 — see Resolved Decision #5).

**Target release:** **9.5.0** — semi-major release permitting simple breaking changes.

**Scope:** both `juneau-rest-server/src/main/java/org/apache/juneau/rest/RestContext.java` and `RestOpContext.java`.

**Related:**
- **Prerequisite for TODO-15 Phase 2.** TODO-15's consumer inventory (legacy `BasicBeanStore` / `BeanCreator` / `BeanBuilder` callers) should run **after** this work, so we don't migrate builders that are about to be deleted.
- Indirectly reduces blast radius for TODO-1 (REST server → `BeanStore2`).

**Completed work archive (read for context before touching the remaining items below):**
- **`FINISHED-16a-per-setting-inventory.md`** — Phase 0 inventory (configuration model, inheritance paradigm, standard migration questions, full `RestContext` R1–R58 + `RestOpContext` Op1–Op27 per-setting blocks with done/superseded statuses).
- **`FINISHED-16b-phases-1-2-execution.md`** — Phase 1 (simple settings) + Phase 2 (composite settings) execution narratives, the Phase-2 progression strategy, the post-landing coverage hardening pass, and the two bug fixes uncovered there (annotation precedence, URI `noInherit` gating).
- **`FINISHED-16c-phase-3-builder-deletion.md`** — Phase 3 pre-flight kill-list (Decisions #24/#25/#26 deletions), plus Builder-deletion Phases A (additive entry points), B (simple-callsite migration), C-1 (pre-build bean-store configurer hook), C-2 (drop the resource-ctor-takes-Builder protocol + delete `RestContext.create(...)`), and Phase C-3 Route B landing (drop the per-op `@RestInit(RestOpContext.Builder)` injection protocol + migrate Site 1 / Site 2 / delete `RestOpContext.create(...)`).

---

## Remaining work

### Phase 3 — Delete the builders (continued)

Completed Phase 3 work archived in `FINISHED-16c-phase-3-builder-deletion.md`:

- Pre-flight kill-list (Decisions #24/#25/#26 deletions).
- Phase A (additive entry points), Phase B (simple-callsite migration).
- Phase C-1 (pre-build bean-store configurer hook).
- Phase C-2 (drop the resource-ctor-takes-Builder protocol + delete `RestContext.create(...)`).
- Phase C-3 Route B (drop the per-op `@RestInit(RestOpContext.Builder)` protocol + migrate Site 1 / Site 2 / delete `RestOpContext.create(...)`).
- Phase C-3 pre-flight `dotAll` removal (Decision #17).
- Class-level `@RestInit(RestContext.Builder)` deletion (companion to Route B).
- Phase C-3 follow-up Javadoc cleanup of deleted Builder-injection examples + 2 unused setter deletions (`defaultAccept(String)`, `defaultContentType(String)`).

The end-state of this TODO is for `RestContext` and `RestOpContext` to be configured **purely via `@Rest` / `@RestOp` annotation reads + `@RestInject` bean lookups + memoized `findXxx()` reads**, with both Builder classes deleted outright (Decisions #1, #4). The hard work that remains is figuring out where the apply-pass machinery (currently routed through `*Annotation.apply(AnnotationInfo, RestOpContext.Builder)` writes into `RestOpContext.Builder.xxx()` setters) lives once the Builder is gone.

**Design decision (locked 2026-04-19): Option 2 — apply-pass demolition.** The 9 REST-specific apply classes are deleted; their per-line transforms migrate into `findXxx()` memoizers on `RestContext` / `RestOpContext` that walk the annotation chain directly. The marshaller-side `@ContextApply` infrastructure stays (still used by `@JsonConfig`, `@BeanConfig`, etc.) — only the REST apply classes go away. See **Phase D — Apply-pass demolition** below for the sub-phase breakdown.

#### Phase D — Apply-pass demolition

**Apply-class inventory (9 classes, all in `juneau-rest-server/src/main/java/org/apache/juneau/rest/annotation/`):**

1. `RestAnnotation.RestContextApply` — `@Rest` → `RestContext.Builder` (line 704).
2. `RestAnnotation.RestOpContextApply` — `@Rest` → `RestOpContext.Builder` (line 729; class-level `@Rest` settings fall through to per-op state).
3. `RestOpAnnotation.RestOpContextApply` — `@RestOp` → `RestOpContext.Builder` (line 455).
4. `RestGetAnnotation.RestOpContextApply` (line 380).
5. `RestPostAnnotation.RestOpContextApply` (line 440).
6. `RestPutAnnotation.RestOpContextApply` (line 440).
7. `RestDeleteAnnotation.RestOpContextApply` (line 340).
8. `RestPatchAnnotation.RestOpContextApply` (line 440).
9. `RestOptionsAnnotation.RestOpContextApply` (line 380).

**Sub-phases (each landable + testable independently):**

- [ ] **D-0 — Apply-line → memoizer mapping inventory.** For every line in every apply class, identify the `findXxx()` memoizer that should absorb it. Catalog reduction semantics per setting: scalar (last-non-empty wins / first-non-empty wins) vs list (concat in chain order) vs set (last-non-empty replaces). Flag any cross-bucket lines (e.g. `defaultAccept` writes into `defaultRequestHeaders`) — the destination memoizer must read **all** contributing annotation attributes. Catalog any settings that have **no** existing memoizer — those need a new `findXxx()` + `Memoizer<T>` field added before D-1/D-2 can demolish their apply lines.
  - Output: a checklist appended to this TODO listing every (apply-line, target-memoizer) pair, plus the list of memoizers that need to be created first.
  - **No code changes.** Pure inventory pass.

- [ ] **D-1 — Migrate `RestAnnotation.RestContextApply` (8 settings on `RestContext`).** For each apply line in this class: ensure the corresponding `findXxx()` on `RestContext` walks the `@Rest` annotation chain itself. Once verified, delete the apply line. After all lines migrated, delete the inner `RestContextApply` class and remove its `@ContextApply(...)` registration on `@Rest`.
  - Risk: low-medium. `@Rest` is class-scoped; only one annotation chain to walk.

- [ ] **D-2 — Migrate the 8 `RestOpContext`-targeting apply classes** (`RestAnnotation.RestOpContextApply` + 7 per-method classes). Same drill, but the chain walk is *class-then-method* annotations. The 6 method-specific apply classes (`RestGet/RestPost/RestPut/RestDelete/RestPatch/RestOptions`) are essentially copies of `RestOpAnnotation.RestOpContextApply` with a different annotation type — handle them in parallel sweeps once the `RestOpAnnotation` migration template is established.
  - Risk: medium. Chain semantics for class+method need to match the current apply-pass order exactly. Heavy test coverage on `defaultRequestHeaders` / `serializers` / `parsers` inheritance.

- [ ] **D-3 — Delete `RestOpContext.Builder`.** Once D-2 is complete, the Builder's `<init>(Method, RestContext)` body has nothing to do except hold transient bootstrap state. Inline whatever's left into `RestOpContext.<init>(Method, RestContext)` and delete the Builder class. Update `RrpcRestOpContext` to chain its protected ctor directly to the new `RestOpContext` ctor (the public 2-arg ctor for `RrpcRestOpContext` already handles the bean-store override internally per Phase C-3 Route B).
  - Risk: medium. Touches `RestContext.Builder.createRestOperations` (Site 1 / Site 2 instantiation) and the RrpcRestOpContext ctor chain.

- [ ] **D-4 — Delete `RestContext.Builder`.** Most invasive — the Builder's `init(Supplier)` does extensive bootstrap setup (children registration, bean-store seeding, `RestInject` discovery, swagger pre-registration, etc.). Move it into `RestContext.<init>(RestContextInit)` and into private helper methods. Drop the `Decision #23 Phase C-2` deprecation comments. The `RestContextInit` record stays (it's the ctor-arg bundle, not a holder).
  - Risk: high. Multi-session work. Each migrated chunk verified with `./scripts/test.py -f` + `./scripts/coverage.py --branches`.

- [ ] **D-5 — Cleanup.**
  - Sweep stale Javadoc `{@link RestContext.Builder#xxx}` / `{@link RestOpContext.Builder#xxx}` doclinks (the remaining set, not the deleted-protocol examples already cleaned in the Phase C-3 follow-up).
  - Drop residual `@Deprecated` markers on builder methods.
  - Update release notes (`9.5.0.md`) with the full removal list.
  - Update v9.5 migration guide with the apply-pass-replacement recipe (no user-facing impact in normal `@Rest` / `@RestOp` use; only matters for users who wrote custom `*Annotation` apply classes — vanishingly rare).

**Cross-cutting verification after each sub-phase:** `./scripts/test.py -f` (full build + tests, ~90s) + `./scripts/coverage.py --branches` on touched files. The existing inheritance-test corpus (`RestInherit_Test`, `NoInherit_Test`, the URI-rewriting tests fixed in Phase 2) is the primary safety net.

### Phase 4 — Public API cleanup

- [ ] Remove any `@Deprecated` builder methods that accumulated during Phases 1–2.
- [ ] Sweep consumers in `juneau-microservice-*`, `juneau-examples-*`, and `juneau-utest`.
- [ ] Add release-notes entry to `juneau-docs/pages/release-notes/9.5.0.md`:
  - List of removed `RestContext.Builder` methods (R1–R12, R42–R48, plus the Composite-bean-lookup set that loses its builder setter).
  - List of removed `RestOpContext.Builder` methods.
  - Migration pattern: `builder.xxx(v)` → `@Rest(xxx=...)` / `@RestOp(xxx=...)` annotation attribute, or `@RestInject(name="xxx")` method/field, or a bean supplied by the enclosing DI container (Spring, etc.).
  - Note on custom subclass impact for users who extended either builder.

---

## Risks & notes

- **Public API surface.** `RestContext.Builder` and `RestOpContext.Builder` are both public and widely subclassed by users. This is a hard break; the 9.5 "simple breaking changes" budget covers it but every removed method needs a release-note entry.
- **Per-request vs. per-context settings.** Stay within `RestContext` / `RestOpContext` scope. Anything varying per-request (headers, path params, etc.) is out of scope.
- **Servlet/Spring integration points.** Confirm `juneau-microservice-core`, `juneau-microservice-jetty`, and `juneau-microservice-springboot` don't depend on builder methods that get deleted.
- **`AnnotationWorkList` flow.** Several composite settings (`beanContext`, `jsonSchemaGenerator`, `partParser`, `partSerializer`, `parsers`, `serializers`) are currently populated via the `AnnotationWorkList` applied on the builder (ctor `apply(work)` lines in both files). The Phase C-3 design must keep that application ordering-correct — memoizers first-access triggers must see a fully-built work list.
- **Memoizer ordering.** Several settings depend on other settings (`config` → `varResolver`; `varResolver` → `messages`; `methodExecStore` → `thrownStore`; `consumes` → `parsers`/`restOperations`; `produces` → `serializers`/`restOperations`; `Op14` → `restMethod` + RRPC-aware path detection — `dotAll` removed per Decision #17). Document the chain and verify no cycles.

---

## Resolved decisions (reference)

These remain authoritative for any further work in this TODO. Numbered for stable cross-reference from per-setting blocks in `FINISHED-16a-*.md` and the migration guide.

1. **Builder callers are a non-concern.** Both `Builder` classes are deleted wholesale in this TODO. All configuration comes from `@Rest*` annotation attributes, `@RestInject` beans, and externally-supplied beans (Spring or any DI container registered with the bean store).
2. **First-access memoizer cost is acceptable.** Callers who care about warm-start latency can issue a dummy request. No hot-path optimizations required.
3. **Inheritance unified under `noInherit`.** The `allowedParserOptions` / `allowedSerializerOptions` pattern generalizes to all inheritable settings (see "Inheritance paradigm" in `FINISHED-16a`). Replaces the existing ad-hoc replace-vs-append complexity.
4. **No slim `Builder`.** Both `RestContext.Builder` and `RestOpContext.Builder` are deleted outright. Bootstrap state (servlet config, resource class, parent context, children list, Java `Method`) moves to constructor args — bundled into a small config record (`RestContextInit`) for `RestContext`; positional for `RestOpContext`.
5. **No `resetXxx()` methods.** Memoizers are bootstrap-immutable in 9.5. Runtime mutation API can be added later if a concrete caller emerges; adding it pre-emptively is out of scope.
6. **Env-var prefix preserved.** All env-var reads keep the `"RestContext.xxx"` / `"RestOpContext.xxx"` prefix to avoid collisions with non-Juneau code in the same JVM.
7. **Sequencing vs. TODO-15 confirmed:** **TODO-16 Phase 1–2 → TODO-15 Phase 2 (inventory) → TODO-15 Phase 3–4 → TODO-14**.
8. **`RestContext.defaultCharset` / `maxInput` deleted at the context level.** The annotation only feeds `RestOpContext`, so the `RestContext`-level field is dead once the builder is gone. `RestOpContext.findDefaultCharset()` / `findMaxInput()` walks `@RestOp(...)` first, then `@Rest(...)` on the resource-class hierarchy if inherited, then the hardcoded default. No `RestContext`-level memoizer.
9. **Sub-builder chaining replaced by `@RestInject`.** The old `EncoderSet.Builder encoders()` / `ParserSet.Builder parsers()` / etc. chains are gone. Configuration happens through annotations + `@RestInject(name="xxx")` beans. Any composition points the annotation+inject combination doesn't cover (e.g. programmatic listener registration) are evaluated individually under Resolved Decision #22 — exact parity is **not** required.
10. **Merge-semantics settings — single memoizer per output.** `guards` (Op24) and `matchers` (Op25) get memoized fields. Scalar inputs (`clientVersion`, `roleGuard`, `rolesDeclared`) become inputs to `findGuards()` / `findMatchers()`, not separately memoized fields.
11. **Drop the `"NONE"` sentinel from all three `allowed*` settings (R1, R2, R3).** Users who previously wrote `@Rest(allowedHeaderParams="NONE")` now write `@Rest(noInherit={"allowedHeaderParams"})`. Already captured in the v9.5 Migration Guide.
12. **`noInherit` syntax: single `String[]` of property names on `@Rest` / `@RestOp`.** Matches the existing `allowedParserOptions` / `allowedSerializerOptions` convention. Validation of the strings (typo detection) happens in the `findXxx()` memoizers themselves — an unknown name simply has no effect.
13. **v9.5 Migration Guide exists** at `juneau-docs/pages/topics/23.01.V9.5-migration-guide.md` and is registered in `juneau-docs/sidebars.ts` under section 23. Every breaking change landed during this TODO must add an `Old`/`New` row there. Audit of pre-existing 9.2.x breaking changes is tracked separately under TODO-17.
14. **`BeanCreateMethodFinder` / `static Optional<X> createXxx(...)` hooks replaced by `@RestInject` on a static method.** The magic-method-name reflection scan is gone. Users who previously wrote `static Optional<EncoderSet> createEncoders(...)` now write `@RestInject(name="encoders") static EncoderSet createEncoders(...)`.
15. **`defaultAccept` / `defaultContentType` promoted to first-class annotation attributes.** `@Rest(defaultAccept=..., defaultContentType=...)` and `@RestOp(defaultAccept=..., defaultContentType=...)`. Internally folded into `findDefaultRequestHeaders()` as `Accept` / `Content-Type` entries.
16. ~~**`defaultClasses` / `defaultSettings` (R20/R21) become memoizable.**~~ **Superseded by Decisions #25 and #26 — both fields are deleted outright in Phase 3, not memoized.**
17. **`dotAll` (Op3) dropped as a separate setting (LANDED 2026-04-19).** The legacy `Builder.dotAll()` flag's auto-append-`/*` behavior is gone. Users who previously called it now write `/*` (or `**`) explicitly in their `@RestOp(path=...)` value — `UrlPathMatcher` already understands those patterns natively. **Internal RRPC convention:** `Builder.getPathMatchers()` auto-appends `/*` when the path is auto-detected (no explicit `path` attribute) AND the resolved http method is `"RRPC"` — RRPC operations are intrinsically "match anything below the method's URL" by design. The migration-guide row was already in place pre-landing.
18. **Lifecycle method list representation unified on `MethodList`.** R45 / R46 / R48 (`startCallMethods`, `endCallMethods`, `preCallMethods`, `postCallMethods`) move from `MethodInvoker[]` → `MethodList` to match R44 (`postInitMethods`) and R47 (`destroyMethods`). Internal-only refactor.
19. **Memoizer dependency policy.** `findXxx()` methods MAY freely call `getYyy()` on the same context (or on `parentContext` / `context`), which transparently triggers Y's memoizer on first access. No runtime cycle-detection — the dependency DAG is the developer's responsibility. Documented chains: `config` → `varResolver` → `messages`; `encoders` / `parsers` / `serializers` → `beanStore`; `defaultRequestHeaders` ← `defaultAccept` + `defaultContentType` (#15); `varResolver` → `messages`; `callLogger` ← `thrownStore` + `methodExecStore`. Anyone introducing a new memoizer that calls `getYyy()` from `findXxx()` records the dependency in a comment on the field declaration.
20. **`@Rest(beans={MyConfig.class, ...})` — class-level bean configuration sources.** Lets users name one or more classes containing `@RestInject`-annotated methods/fields. Scanned at bean-store construction time; each `@RestInject` member contributes a named bean to the same bean store the resource class itself feeds. Purely additive — `@RestInject` on the resource class still works (Decision #14). **Inheritance:** participates in the `noInherit` paradigm — by default, parent `@Rest(beans=...)` classes contribute too, append-merged into a single ordered list. **Conflict resolution:** resource class wins over `@Rest(beans=...)`; child classes win over parent classes; earlier entries in `beans={...}` win over later. **Instantiation policy — three-tier resolution:** all-static → public static `INSTANCE` field → bean-store resolution.
21. **Full test coverage on new code, enforced via `coverage.py`.** Every new `findXxx()` memoizer, every new annotation attribute, every new `@RestInject` named-bean lookup, and every new bean-config-class instantiation tier must land with tests covering every branch — including parent-fallback, `noInherit` cutoff, negation tokens, and the "no source supplied → default" path. Workflow per batch: (a) write the change, (b) run `./scripts/coverage.py [--branches] <touched paths>`, (c) add tests for every uncovered line/branch, (d) re-run until the touched files report 100 % line + branch coverage on the new code.
22. **`@RestInject` parity — drop when awkward.** Each Composite-bean-lookup setting that previously exposed programmatic configuration via the builder *may* get an equivalent `@RestInject(name=?, methodScope=?)` slot, but parity is **not** a goal. If the old programmatic surface is awkward, obsolete, or trivially replaceable by the annotation alone, drop it.
23. **Constructor signature — `RestContextInit` record + 2-arg positional `RestOpContext` ctor (Phase A+B+C-1+C-2 landed).** With Decisions #24–#26 applied, the bootstrap-state count drops to:
    - **`RestContext`:** 6 fields (3 required, 3 optional) — `Class<?> resourceClass`, `RestContext parentContext` *(nullable)*, `ServletConfig servletConfig` *(nullable)*, `Supplier<?> resource`, `String path` *(default `""`)*, `List<Object> children` *(default empty)*. Phase C-1 added a 7th: `Consumer<BasicBeanStore> beanStoreConfigurer` *(nullable, no-op default)* for callers that need pre-build bean-store mutation (mock clients).
    - **`RestOpContext`:** 2 required positional args — `java.lang.reflect.Method method`, `RestContext context`. `dotAll` is dropped (#17, landed); `path` is annotation-driven via the existing `Builder.getPathMatchers()` scan (the deeper `findPathMatchers()` memoizer landing is tracked in `FINISHED-16a` Op14). The Phase C-3 Route B landing (2026-04-19) confirmed this 2-arg shape is sufficient — no `RestOpContextInit` record needed; `RrpcRestOpContext` got a parallel 2-arg public ctor that handles the root-bean-store override internally.
24. **Delete `Builder.childrenClass(...)` / `Builder.opContextClass(...)` + `@Rest(restChildrenClass=...)` outright (no replacement).** Both builder fields existed solely to let users swap in a custom `RestChildren` / `RestOpContext` subclass. No real-world callers anywhere. Defaults hard-code to `RestChildren.class` / `RestOpContext.class`.
25. **Delete `defaultClasses` (R20) outright; introduce typed-class-binding API on `BasicBeanStore`.** Added `addBeanType(Class<T>, Class<? extends T>)` / `getBeanType(Class<T>)` to `BasicBeanStore` with parent-chain traversal in `getBeanType` — preserves the original *deferred construction* semantics for components like `BasicTestCallLogger` that need beans not yet available at registration time.
26. **Delete `defaultSettings` (R21) and `DefaultSettingsMap` outright; promote `debugDefault` to `@Rest(debugDefault=...)`.** The map carried exactly **one** key (`"RestContext.debugDefault"`) read by exactly **one** consumer.
27. **Naming convention for the new ctor record — `XInit` (`Args` reversed).** `RestContextArgs` was already taken by an `@RestOp`-method parameter resolver (one of an entire `*Args` family wired through `DefaultConfig.restOpArgs={...}`). The new ctor record uses `Init` instead — short, unambiguous, doesn't collide with the arg-resolver family, and reads naturally as a parallel to the legacy `Builder.init(Supplier)` method this record consolidates: `new RestContext(new RestContextInit(...))`. `RestOpContext` does **not** get a peer `RestOpContextInit` — its bootstrap state is just `(Method, RestContext)`, small enough to pass positionally; and per the Phase D design decision, the apply-pass moves into `findXxx()` memoizers rather than into a transient mutable holder.

## Open questions

*(none open — all design questions resolved. Most recent: 2026-04-19 picked **Option 2 / Phase D apply-pass demolition** for builder deletion — see Phase D in "Remaining work" above.)*

---

## Out of scope

- New features (per-method settings, scoped overrides, runtime-mutation / JMX / hot-reload APIs) — track separately.
- `RestClient` builder — same pattern would apply there, but that's a different TODO.
- Backporting the pattern to other `Context.Builder` subclasses (serializers, parsers, etc.) — possible future work once the REST-side pattern is proven.
- `RestChildren` / `RestOperations` internals — out of scope except where they're referenced as dependencies of in-scope settings (R52/R53, Op12/Op13).
