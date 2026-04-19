# FINISHED-16b: Phases 1 & 2 — execution narratives, coverage hardening, bug fixes

> **Archived from `TODO-16-restcontext-memoized-fields.md`.** Captures the Phase 1 (simple settings) and Phase 2 (composite settings) execution narratives, the Phase-2 progression strategy, the post-landing coverage-hardening pass (2026-04-18), and the two bug fixes uncovered during that pass (annotation precedence + URI `noInherit` gating). All work here is **landed**. Per-setting `Status: DONE` rows live in `FINISHED-16a-per-setting-inventory.md`.

---

### Phase 1 — Convert simple settings

Worked through the **Memoize** candidates. Questions from Phase 0 were answered for each setting before editing code.

- [x] Reference implementation: `allowedParserOptions` (already in place on `RestContext`).
- [x] `RestContext` Memoize batch (primitives/simple): R1, R2, R3, R4, R6, R8, R9, R10, R11, R12 — all converted to `Memoizer<T> + findXxx()` pattern reading from `@Rest(...)` annotations + env defaults via `mergeReplacedStringAttribute` / `mergeReplacedBooleanAttribute`. Builder fields and setters deleted; ctor copies removed; getters return from memoizers.
- [x] `RestContext` Memoize batch (to-be-consolidated with `RestOpContext`): R5 / R7 — `defaultCharset` and `maxInput` deleted from `RestContext` entirely (no builder field, no context field, no ctor copy). `RestOpContext.findDefaultCharset()` / `findMaxInput()` walk `@RestOp(...)` first, then `@Rest(...)` on the resource-class hierarchy via `context.mergeReplacedStringAttribute(...)` (gated on op-level `noInherit`), then fall back to env defaults. The `java.nio.charset.*` import was removed from `RestContext.java`. `NoInherit_Test` `a06`/`a07`/`a08` were rewritten to declare `@Rest(defaultCharset|maxInput)` on the resource class instead of calling the deleted builder setters; new `a09` covers the maxInput-without-noInherit path.
- [x] `RestContext` Memoize batch (lifecycle method lists): R42–R48 — `Memoizer` + `LifecycleInvokerPair` (`MethodList` + `MethodInvoker[]`); `RestContext.Builder` no longer caches the seven `MethodList` fields; public `getDestroyMethods` / `getEndCallMethods` / `getPostInitMethods` / `getPostInitChildFirstMethods` / `getStartCallMethods` plus widened-public `getPreCallMethods` / `getPostCallMethods`.
- [x] `RestOpContext` Memoize batch: Op3, Op4, Op6, Op12, Op13, Op15, Op27 — Op3 unchanged (still explicit `dotAll` on builder until path-based inference); **Op4/Op6/Op15** now read directly from `@RestOp`-group annotations + SVL (`findDefaultCharset()` / `findMaxInput()` / `findDebugEnablement()`); their builder fields and setters (`Builder.defaultCharset(Charset)`, `Builder.maxInput(String)`, `Builder.debug(Enablement)`) are **deleted**, and the corresponding `b::defaultCharset` / `b::maxInput` / `b::debug` lines in `RestAnnotation`, `RestOpAnnotation`, `RestGetAnnotation`, `RestPostAnnotation`, `RestPutAnnotation`, `RestDeleteAnnotation`, `RestPatchAnnotation`, `RestOptionsAnnotation` are removed. `noInherit` gates the `context` fallback (which still carries the class-level `@Rest(defaultCharset|maxInput)` value via `RestContext.defaultCharset`/`maxInput`); when the fallback is denied, env defaults apply. **`opBootstrap` removed** (no retained builder reference). **Op12/Op13** now read directly from `@RestOp`-group + class-level `@Rest` annotations + SVL (`findSupportedContentTypes()` / `findSupportedAcceptTypes()`); their builder fields and setters (`Builder.consumes(MediaType...)`, `Builder.produces(MediaType...)`) are **deleted**; corresponding `b::consumes` / `b::produces` lines in `RestAnnotation` (RestOpContextApply only — RestContextApply still pushes to `RestContext.Builder`), `RestOpAnnotation`, `RestGetAnnotation`, `RestPostAnnotation`, `RestPutAnnotation`, `RestPatchAnnotation`, `RestOptionsAnnotation` are removed. `noInherit={"consumes"}` / `noInherit={"produces"}` blocks the class-level `@Rest(consumes|produces)` from contributing to the op; when no annotation values are declared, falls back to the op's `parsers.getSupportedMediaTypes()` / `serializers.getSupportedMediaTypes()`. **Op27** now reads directly from `@RestOp`-group annotations (`findHttpMethod()`): verb annotations imply their fixed verb, `@RestOp(method)`/`value()` are SVL-resolved; falls back to `HttpUtils.detectHttpMethod(method, true, "GET")`; `Builder.httpMethod(String)` and the field are **deleted**, and the `b.httpMethod(...)` lines in `RestOpAnnotation`, `RestGetAnnotation`, `RestPutAnnotation`, `RestPostAnnotation`, `RestDeleteAnnotation`, `RestPatchAnnotation`, `RestOptionsAnnotation` are removed. Javadoc references to the removed builder methods in the `@RestOp` / `@RestGet` / `@RestPut` / `@RestPost` / `@RestPatch` / `@RestOptions` annotations were redirected to `RestOpContext#getSupportedContentTypes()` / `getSupportedAcceptTypes()`.

**Per-batch protocol** (followed for every conversion):
1. Add `Memoizer<T>` field + `findXxx()` on the context class.
2. Delete builder field + setter.
3. Update `RestAnnotation` / `RestOpContextApply` / `@Rest*` processing to feed `findXxx()` (via annotation scan, not a builder slot).
4. Update `getXxx()` getter to return from the memoizer.
5. Fix internal callers.
6. Re-run `./scripts/test.py -f` after each batch.
7. After each batch, run `./scripts/coverage.py` on the touched files and **add tests for any uncovered new code** (every new `findXxx()` branch, every new `@Rest`/`@RestOp` annotation path, every new `@RestInject` lookup). See Resolved Decision #21.

---

### Phase 2 — Convert composite settings

Composite-bean-lookup candidates. Each was gated on its Phase 0 questions being answered.

- [x] `RestContext` batch: R14 (done), R17 (done), R22 (done), R23 (done), R24 (done), R25 (done), R26 (done), R27 (done), R28 (done), R29 (done), R30 (done), R31 (done), R32 (done), R33 (done), R34 (done), R35 (done), R36 (done), R37 (done), R38 (done), R39 (done), R40 (done), R41 (done).
- [x] `RestOpContext` batch: Op1 (done), Op5 (done), Op7 (done), Op8 (done), Op9 (done), Op10 (done), Op11 (done), Op14 (done), Op17 (done), Op18 (done), Op19 (done), Op20 (done), Op23 (done), Op24 (done), Op25 (done), Op26 (done).
- [x] These were the ones that pulled `BasicBeanStore` / `BeanCreator` / `BeanCreateMethodFinder` / `BeanBuilder` into the builder API — converting them shrinks TODO-15's footprint.
- [x] For each setting, confirmed the `@RestInject(name="xxx")` bean-lookup path covers the previous programmatic builder surface where that makes sense (see Resolved Decision #22 — drop parity when the old surface is awkward).
- [x] Preserved each setting's `static Optional<T> createX(...)` resource-class hook — it becomes an input to `findXxx()`, not a separate code path.
- [x] After each batch, ran `./scripts/coverage.py --branches` on the touched files and **added tests for any uncovered new code** — composite settings have more branches (annotation path, `@RestInject` path, parent-fallback path, defaults), so `--branches` was the right view here. See Resolved Decision #21.

#### Phase 2 progression strategy

After R27 was tackled as the proof-of-concept, the remaining Phase 2 candidates were split into **complexity tiers** (worked the easier tiers first to keep PRs reviewable):

1. **Tier A — leaf settings, no external sub-builder usage, no annotation flow:** R27 (logger). Pattern: drop builder field/setter/createX, add `Memoizer<T> + findT()` on `RestContext`, eagerly call `getT()` in the ctor to preserve bean-store registration ordering.
2. **Tier B — leaf settings with `@Rest`-annotation flow but no sub-builder consumers:** R28 (messages). Pattern: same as Tier A, but `findT()` walks the `@Rest` hierarchy via `getRestAnnotations()` and applies the per-attribute logic; the `RestAnnotation.RestContextApply` push is deleted; SVL resolution uses `builder.varResolver().build().createSession()` because the context's own var resolver isn't initialized yet at the construction-time call.
   - R36 `thrownStore` — leaf with no annotation, but R29 depends on it. Tier-A pattern with parent-fallback only (no annotation walk needed).
3. **Tier C — leaf settings with cross-deps:** R29 `methodExecStore` (depends on R36 — `findMethodExecStore()` calls `getThrownStore()` directly, satisfying Resolved Decision #19's intra-context memoizer chaining policy). R37 `varResolver` (depends on R28 `messages`).
4. **Tier D — settings with retained `Builder` sub-builders consumed externally:** R26 `jsonSchemaGenerator` (used by `RestOpContext.java:192,1548` for `canApply(work)` and `parent.jsonSchemaGenerator().copy()`) — needed to refactor `RestOpContext` to read from `RestContext.getJsonSchemaGenerator()` directly and route the per-op `AnnotationWorkList` apply through a different mechanism (likely an op-level `findJsonSchemaGenerator()` that reads class-level value + applies op-level annotations).
5. **Tier E — `BeanCreator`-based settings:** R38–R41 (callLogger, debugEnablement, staticFiles, swaggerProvider). Replaced `BeanCreator<T>` indirection with direct `findT()` reads.
6. **Tier F — collection settings:** R22, R23, R24, R25, R30, R31, R34. Heaviest because of parent-context copy semantics + adders.

**Bootstrap ordering invariant:** until the Tier A pattern is replaced by `bs.addSupplier(T.class, this::getT)` everywhere, the ctor must still call `getT()` eagerly for any bean that downstream ctor lines (or other contexts) read via `bs.getBean(T.class)`. Otherwise the bean store will return `Optional.empty()` for that slot.

#### Phase 2 coverage hardening (2026-04-18)

After all Phase-2 batches landed, ran `./scripts/coverage.py --branches` over `RestContext.java` and `RestOpContext.java` and added targeted tests for the new annotation-override branches. Notable artifacts:

- **`Rest_BeanCreatorOverrides_Test`** (`juneau-utest/src/test/java/org/apache/juneau/rest/annotation/`) — covers `@Rest(debugEnablement=…)`, `@Rest(staticFiles=…)`, `@Rest(swaggerProvider=…)` overrides and child-overrides-parent precedence for all three (R39/R40/R41). The child-override case caught and pinned a regression — see "Annotation precedence bug" below.
- **`RestOpContext_HttpMethodResolution_Test`** (`juneau-utest/src/test/java/org/apache/juneau/rest/`) — covers `findHttpMethod()` / `httpMethodFromAnnotation()` / `normalizeHttpMethod()` (Op27): `@RestPatch` / `@RestOptions` fixed-verb branches, `@RestOp(method=…)`, `@RestOp("VERB /path")` value parsing (with and without space), the `"METHOD"` → `"*"` wildcard, and the `HttpUtils.detectHttpMethod` Java-method-name fallback.
- **`RestOpContext_OpLevelOverrides_Test`** (`juneau-utest/src/test/java/org/apache/juneau/rest/`) — exercises the `v.isPresent()` branch in `findDefaultCharset()` / `findMaxInput()` / `findDebugEnablement()` (Op4/Op6/Op15): `@RestGet(defaultCharset=…)`, `@RestOp(method="get", maxInput=…)`, `@RestGet(debug="true")` overriding the class-level `@Rest(...)`. Complements `NoInherit_Test` which only covered the inherit-from-class and `noInherit`-skip paths.

**Result:** `RestOpContext` branch coverage 78 % → 80 % (210/270 → 215/270 covered). `RestContext` already at 66 % branches; remaining gaps are dominated by pre-existing builder/legacy code paths outside the Phase-2 footprint.

#### Annotation precedence bug (fixed 2026-04-18)

While writing the coverage tests above, the `child-overrides-parent` assertions failed for `findCallLogger()` / `findDebugEnablement()` / `findStaticFiles()` / `findSwaggerProvider()` (R38–R41). All four finders walked the `@Rest` hierarchy in **parent-to-child** order using `getRestAnnotationsForProperty(...)` and then called `.findFirst()` on the resulting stream — which selected the **least-derived** annotation value (parent class wins) instead of the most-derived (child wins).

**Fix:** replaced `.findFirst()` with `.reduce((first, second) -> second)` in all four methods so the **last** entry of a parent-to-child stream is picked. Restores the previous "child overrides parent" semantics that the builder-based code had via the standard annotation-merge order. The new `Rest_BeanCreatorOverrides_Test#*_childOverridesParent` cases pin the corrected behavior.

#### URI authority / context `noInherit` gating (fixed 2026-04-18)

Code review on the Phase-2 changes found that `findUriAuthority()` and `findUriContext()` returned `parentContext.getUriAuthority()` / `parentContext.getUriContext()` unconditionally when no annotation value was present, ignoring `@Rest(noInherit={"uriAuthority"|"uriContext"})`. Other inheritable scalars already gate the parent fallback on `isInherited(property)`. Added the missing `isInherited()` checks so both settings honor `noInherit` consistently with the rest of the family.
