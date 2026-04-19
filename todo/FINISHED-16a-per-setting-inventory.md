# FINISHED-16a: Per-setting inventory and migration blocks

> **Archived from `TODO-16-restcontext-memoized-fields.md`.** This file preserves the original Phase 0 inventory (configuration model, inheritance paradigm, standard migration questions) and every per-setting migration block (R1–R58 for `RestContext`, Op1–Op27 for `RestOpContext`, plus derived `RestOpContext` fields). All entries are either landed or superseded by later decisions; nothing here is open work. See `TODO-16-restcontext-memoized-fields.md` for the active checklist and the resolved decisions that govern any remaining work.

---

### Phase 0 — Per-field inventory and migration questions

This was the analysis-only phase. Output: the inventory below plus answers to every per-setting question. Subsequent phases worked directly from this inventory.

Inventory snapshot was captured against `RestContext.java` 6442 lines / `RestOpContext.java` 2751 lines. Line numbers **drift** as migration proceeds; use them as historical starting points, not as invariants.

---

#### Configuration model (post-refactor)

With the builders gone, **every configurable setting is sourced from one of four places**:

1. **Annotation attributes** — `@Rest(xxx=...)` / `@RestOp(xxx=...)` / `@RestGet|Put|Post|Delete(xxx=...)`. Primary source.
2. **`@RestInject` beans on the resource class** — `@RestInject(name="xxx")` on methods or fields of the resource class, resolved via the bean store. The path that replaces per-setting programmatic builder configuration.
3. **`@RestInject` beans on bean-config classes** — `@RestInject(name="xxx")` on methods or fields of any class listed in `@Rest(beans={MyConfig.class, ...})`. Lets users factor reusable bean definitions out of the resource class, Spring-`@Configuration`-style. See Resolved Decision #20.
4. **Externally-supplied beans** — Spring-managed beans (via `juneau-microservice-springboot`) or any other DI container registered with the bean store.

**There are no `builder.xxx(...)` callers to worry about** — the builders are deleted in this TODO. Every `findXxx()` implementation sources its value from (1), falling back to (2)/(3), falling back to (4) where applicable.

#### Inheritance paradigm — `noInherit`

The `allowedParserOptions` / `allowedSerializerOptions` pair already uses a clean inheritance model that we want to generalize to **every list-valued and every inheritable scalar setting**:

```java
private final Memoizer<SortedSet<String>> allowedParserOptions = memoizer(this::findAllowedParserOptions);

private SortedSet<String> findAllowedParserOptions() {
    var l = new ArrayList<String>();
    var p = PROPERTY_allowedParserOptions;
    if (isInherited(p) && parentContext != null)
        l.addAll(parentContext.getAllowedParserOptions());
    getRestAnnotationsForProperty(p).forEach(x -> resolveCdl(x.getStringArray(p)).forEach(l::add));
    return Collections.unmodifiableSortedSet(treeSet(String.CASE_INSENSITIVE_ORDER, removeNegations(l)));
}
```

**The rules:**

1. **Default = inherit + append.** Parent context's resolved value flows in first; child annotation values append.
2. **Opt out per-property via `@Rest(noInherit={"xxx","yyy"})`** — listed property names do NOT inherit from the parent. Syntax: a **single `String[]` of property names** on `@Rest` / `@RestOp` (see Resolved Decision #12); each string matches a `PROPERTY_xxx` constant exposed by `RestContext` / `RestOpContext`.
3. **`noInherit` itself is never inherited** — it applies only to the `@Rest` on which it's declared.
4. **Per-level `noInherit` cutoff across the annotation hierarchy.** `getRestAnnotationsForProperty(property)` walks child-to-parent across the `@Rest` hierarchy and stops at the first level whose `noInherit` lists the property. This lets an intermediate class block its parents' contribution without blocking its own.
5. **Leading-hyphen removal tokens.** A child value like `"-foo"` removes an earlier `"foo"` contribution via `removeNegations(...)`. Lets a child subtract specific entries rather than blocking the whole inheritance chain.
6. **SVL resolution happens inline** — `resolveCdl(...)` applies `VarResolver` to each value, then splits on commas, trims, and filters blanks.

At operation scope, the same pattern runs through `@RestOp(noInherit=...)` / `@RestGet|Put|Post|Delete(noInherit=...)`, with the "parent" being the enclosing `RestContext`. The aggregation walks the full RestOp-group annotation set via `getRestOpAnnotations()`.

**This replaces the existing ad-hoc "replace-vs-append" complexity** scattered across individual settings. Every list-valued Composite-bean-lookup setting (`serializers`, `parsers`, `encoders`, `defaultRequestHeaders`, etc.) should be ported to this paradigm. Settings where inheritance is a plain yes/no (no append semantics) use `noInherit` as an on/off switch (`if (isInherited(p)) return parent.getXxx(); else return localOnly();`).

#### Standard migration questions (apply to every Memoize / Composite-bean-lookup candidate)

Per-setting blocks below add setting-specific questions on top of these three:

1. **Q-Inherit — Inheritance behavior.** Does this setting join the `noInherit` paradigm? For list-valued settings (the majority of Composite-bean-lookup), yes. For scalars with parent-fallback (e.g. `uriAuthority`, `clientVersionHeader`), use `noInherit` as a yes/no opt-out. For bootstrap identity (e.g. `resourceClass`), no inheritance applies. What's the correct answer for *this* setting?
2. **Q-Test — Test coverage.** Do `juneau-utest` tests assert builder-based configuration paths for this setting? They need to migrate to annotation + `@RestInject` + bean-injection equivalents. Name the tests that need rewriting.
3. **Q-Notes — Release-note wording.** One-line migration hint. (e.g. "`RestContext.Builder.allowedHeaderParams(String)` removed — configure via `@Rest(allowedHeaderParams=...)` or `@RestInject(name=\"allowedHeaderParams\")`.")

---

#### `RestContext` — summary table

"`noInherit`?" column marks candidates for the inheritance paradigm: **L** = list-valued append/negate model, **S** = scalar inherit yes/no, **—** = not inheritable (bootstrap identity or self-contained).

| # | Setting | Category | `noInherit`? | Builder field line | Context field line | Bean-store? |
|---|---|---|---|---|---|---|
| — | `noInherit` | Already-memoized | — | — | 5200 | No |
| — | `restAnnotations` | Already-memoized | — | — | 5210 | No |
| — | `allowedParserOptions` | Already-memoized (reference) | L | — | 5221 | No |
| — | `allowedSerializerOptions` | Already-memoized (reference) | L | — | 5239 | No |
| R1 | `allowedHeaderParams` | Memoize | L | 312 | 5040 | No |
| R2 | `allowedMethodHeaders` | Memoize | L | 313 | 5041 | No |
| R3 | `allowedMethodParams` | Memoize | L | 314 | 5042 | No |
| R4 | `clientVersionHeader` | Memoize | S | 315 | 5044 | No |
| R5 | `defaultCharset` | Memoize (RestContext-level likely dead) | S | 275 | 5013 | No |
| R6 | `disableContentParam` / `allowContentParam` | Memoize (inverted) | S | 272 | 5003 | No |
| R7 | `maxInput` | Memoize (RestContext-level likely dead) | S | 292 | 5005 | No |
| R8 | `renderResponseStackTraces` | Memoize | S | 274 | 5004 | No |
| R9 | `uriAuthority` | Memoize | S | 317 | 5047 | No |
| R10 | `uriContext` | Memoize | S | 318 | 5048 | No |
| R11 | `uriRelativity` | Memoize | S | 320 | 5052 | No |
| R12 | `uriResolution` | Memoize | S | 321 | 5053 | No |
| R13 | `path` / `fullPath` / `pathMatcher` | Bootstrap-only (parent chain) | — | 316 | 5046 / 5045 / 5054 | No |
| R14 | `beanContext` | Composite-bean-lookup | S | 265 | 5008 | Yes |
| R15 | `beanStore` | Bootstrap-only (foundational) | — | 270 | 5009 | Self |
| R16 | `rootBeanStore` | Bootstrap-only | — | 271 | 5010 | Self |
| R17 | `config` | Composite-bean-lookup | S | 279 | 5017 | Yes |
| R18 | `consumes` | Memoize (derived from `parsers`) | L | 288 | 5028 | No |
| R19 | `produces` | Memoize (derived from `serializers`) | L | 289 | 5029 | No |
| R20 | `defaultClasses` | Bootstrap-only (mutated by `create*` helpers) | — | 280 | 5019 | No |
| R21 | `defaultSettings` | Bootstrap-only (mutable map inherited from parent) | — | 281 | 5020 | Yes |
| R22 | `defaultRequestAttributes` | Composite-bean-lookup | L | 302 | 5033 | Yes |
| R23 | `defaultRequestHeaders` | Composite-bean-lookup | L | 283 | 5022 | Yes |
| R24 | `defaultResponseHeaders` | Composite-bean-lookup | L | 284 | 5023 | Yes |
| R25 | `encoders` | Composite-bean-lookup | L | 282 | 5021 | Yes |
| R26 | `jsonSchemaGenerator` | Composite-bean-lookup | S | 287 | 5027 | Yes |
| R27 | `logger` | Composite-bean-lookup | S | 291 | 5030 | Yes |
| R28 | `messages` | Composite-bean-lookup | S | 301 | 5031 | Yes (DONE) |
| R29 | `methodExecStore` | Composite-bean-lookup | S | 293 | 5032 | Yes (DONE) |
| R30 | `parsers` | Composite-bean-lookup | L | 303 | 5034 | Yes |
| R31 | `serializers` | Composite-bean-lookup | L | 310 | 5039 | Yes |
| R32 | `partParser` | Composite-bean-lookup | S | 285 | 5024 | Yes |
| R33 | `partSerializer` | Composite-bean-lookup | S | 286 | 5025 | Yes |
| R34 | `responseProcessors` | Composite-bean-lookup | L | 308 | 5038 | Yes |
| R35 | `restOpArgs` | Composite-bean-lookup | L | 306 | 5015 | Partial |
| R36 | `thrownStore` | Composite-bean-lookup | S | 319 | 5050 | Yes (DONE) |
| R37 | `varResolver` | Composite-bean-lookup | S | 322 | 5055 | Yes |
| R38 | `callLogger` | Composite-bean-lookup (`BeanCreator`) | S | 266 | 5012 | Yes |
| R39 | `debugEnablement` | Composite-bean-lookup (`BeanCreator`) | S | 267 | 5018 | Yes |
| R40 | `staticFiles` | Composite-bean-lookup (`BeanCreator`) | S | 268 | 5043 | Yes |
| R41 | `swaggerProvider` | Composite-bean-lookup (`BeanCreator`) | S | 269 | 5049 | Yes |
| R42 | `destroyMethods` | Memoize (lifecycle method list) | L | 294 | 5056 | Helper |
| R43 | `endCallMethods` | Memoize (lifecycle method list) | L | 295 | 5057 | Helper |
| R44 | `postCallMethods` | Memoize (lifecycle method list) | L | 296 | 5061 | Helper |
| R45 | `postInitChildFirstMethods` | Memoize (lifecycle method list) | L | 297 | 5058 | Helper |
| R46 | `postInitMethods` | Memoize (lifecycle method list) | L | 298 | 5059 | Helper |
| R47 | `preCallMethods` | Memoize (lifecycle method list) | L | 299 | 5062 | Helper |
| R48 | `startCallMethods` | Memoize (lifecycle method list) | L | 300 | 5060 | Helper |
| R49 | `children` | Bootstrap-only | — | 290 | — | Yes |
| R50 | `childrenClass` | Bootstrap-only | — | 276 | — | Yes |
| R51 | `opContextClass` | Bootstrap-only | — | 277 | — | Yes |
| R52 | `restChildren` | Bootstrap-only | — | 305 | 5035 | Yes |
| R53 | `restOperations` | Bootstrap-only | — | 307 | 5037 | Yes |
| R54 | `resource` | Bootstrap-only | — | 309 | 5063 | Yes |
| R55 | `resourceClass` | Bootstrap-only (immutable identity) | — | 278 | 5014 | No |
| R56 | `parentContext` | Bootstrap-only (immutable identity) | — | 304 | 5036 | No |
| R57 | `inner` (ServletConfig) | Bootstrap-only | — | 311 | — | Yes |
| R58 | `initialized` (builder flag) | Dead | — | 273 | — | No |

**Totals:** 4 already-memoized · 19 Memoize candidates (R1–R12, R42–R48) · 22 Composite-bean-lookup (R14, R17, R22–R41) · 16 Bootstrap-only (R13, R15–R16, R18–R21, R49–R57) · 1 Dead (R58).

---

#### `RestContext` — per-setting migration blocks

> Unless stated otherwise, every block implicitly takes the **Standard migration questions S1–S8** above.

##### R1. `allowedHeaderParams`

- **Current:** Builder field line 312 (`env("RestContext.allowedHeaderParams", "Accept,Content-Type")`). Setter line 388. Context field line 5040 (`Set<String>`). Ctor resolution line 5109 (wraps in `newCaseInsensitiveSet`, `"NONE"` → empty). Getter line 5480. Annotation `@Rest(allowedHeaderParams)`.
- **Target shape:**
  - `private final Memoizer<Set<String>> allowedHeaderParams = memoizer(this::findAllowedHeaderParams);`
  - `findAllowedHeaderParams()` walks `@Rest(allowedHeaderParams)` up the resource-class hierarchy (stopping when a child declares `noInherit=true`), falls back to the `"RestContext.allowedHeaderParams"` env var, then to the hardcoded default `"Accept,Content-Type"`. Comma-split, trim, wrap in `newCaseInsensitiveSet(...)`.
  - **No** parent-context fallback (do not walk `parentContext.getAllowedHeaderParams()`) — preserves current behavior.
  - **No** `"NONE"` sentinel — users declare `noInherit=true` instead. See Resolved Decision #11.
- **Resolved:**
  - Q-R1.1 ✓ Drop the `"NONE"` sentinel. Users set `noInherit=true` on the child annotation to suppress inheritance.
  - Q-R1.2 ✓ Case-insensitive wrapping moves inside `findAllowedHeaderParams()`; callers see a ready-to-use `Set<String>`.
  - Q-R1.3 ✓ Preserve current behavior — no parent-context chain walking.

##### R2. `allowedMethodHeaders`

- **Current:** Builder field 313; setter 451; context field 5041; ctor 5110; getter 5494; `@Rest(allowedMethodHeaders)`.
- **Target shape:** Same as R1. `findAllowedMethodHeaders()` walks `@Rest(allowedMethodHeaders)` hierarchy (honoring `noInherit`), falls back to env `"RestContext.allowedMethodHeaders"`, then hardcoded default. Wraps in case-insensitive set. No parent-context fallback. No `"NONE"` sentinel.

##### R3. `allowedMethodParams`

- **Current:** Builder field 314 (default `"HEAD,OPTIONS"`); setter 524; context field 5042; ctor 5111; getter 5508; `@Rest(allowedMethodParams)`.
- **Target shape:** Same as R1/R2. `findAllowedMethodParams()` walks `@Rest(allowedMethodParams)` hierarchy, env `"RestContext.allowedMethodParams"`, hardcoded default `"HEAD,OPTIONS"`. Case-insensitive. No parent-context fallback. No `"NONE"` sentinel.

##### R4. `clientVersionHeader`

- **Current:** Builder field 315 (default `"Client-Version"`); setter 975; context field 5044 (plain `String`); ctor 5112; getter 5566; `@Rest(clientVersionHeader)`.
- **Specific questions:**
  - Q-R4.1: Is empty-string a valid value (disables client-version matching), or should `findXxx()` return `Optional<String>` instead?

##### R5. `defaultCharset` — **DONE (Phase 1)** — deleted at `RestContext` level

- **Status:** `Builder.defaultCharset(Charset)` setter and field, `protected final Charset defaultCharset` context field, and ctor copy step all deleted. With nothing populating the field at the `RestContext` level, it became dead weight. `RestOpContext.findDefaultCharset()` (Op4) now walks `@RestOp(defaultCharset)` first, then `@Rest(defaultCharset)` on the resource-class hierarchy if inherited (via `context.mergeReplacedStringAttribute(...)`), then falls back to `UTF8`. No intermediate `RestContext`-level memoizer is needed. The `java.nio.charset.*` import was also dropped from `RestContext.java`.

##### R6. `disableContentParam` / `allowContentParam` (inverted)

- **Current:** Builder field `disableContentParam` line 272; setters 1595/1608; context field `allowContentParam` line 5003 (inverted). Ctor 5108. Getter `isAllowContentParam()` line 6025. `@Rest(disableContentParam)`.
- **Specific questions:**
  - Q-R6.1: Keep the inversion (builder/annotation use "disable", context field uses "allow"), or unify on one direction during the refactor?

##### R7. `maxInput` — **DONE (Phase 1)** — deleted at `RestContext` level

- **Status:** `Builder.maxInput(String)` setter and field, `protected final long maxInput` context field, and ctor copy step all deleted. `RestOpContext.findMaxInput()` (Op15) now walks `@RestOp(maxInput)`, then `@Rest(maxInput)` on the resource-class hierarchy if inherited (via `context.mergeReplacedStringAttribute(...)`), then falls back to the hardcoded default (100 MB).

##### R8. `renderResponseStackTraces`

- **Current:** Builder field 274; setters 2746/2762; context field 5004; ctor 5115; getter `isRenderResponseStackTraces()` line 6032; `@Rest(renderResponseStackTraces)`.
- **Specific questions:** none beyond standard.

##### R9. `uriAuthority`

- **Current:** Builder field 317 (default null); setter 3553; context field 5047; ctor 5116. Getter line 5910 **walks up `parentContext` chain** when null. `@Rest(uriAuthority)`.
- **Specific questions:**
  - Q-R9.1: Express the parent walk inside `findUriAuthority()` so the getter becomes trivial.

##### R10. `uriContext`

- **Current:** Builder field 318 (default null); setter 3614; context field 5048; ctor 5117. Getter 5929 walks `parentContext`. `@Rest(uriContext)`.
- **Specific questions:** Q-R10.1: same as R9 — pull parent-walk into `findUriContext()`.

##### R11. `uriRelativity`

- **Current:** Builder field 320 (default `RESOURCE`); setter 3673; context field 5052; ctor 5118; getter 5948; `@Rest(uriRelativity)`.
- **Specific questions:** none beyond standard.

##### R12. `uriResolution`

- **Current:** Builder field 321 (default `ROOT_RELATIVE`); setter 3732; context field 5053; ctor 5119; getter 5961; `@Rest(uriResolution)`.
- **Specific questions:** none beyond standard.

##### R13. `path` / `fullPath` / `pathMatcher` *(Bootstrap-only)*

- **Current:** Builder field 316; setter 2618 (trims slashes, empty-guard). Ctor 5101–5106 computes all three: `path` + `fullPath = parent.fullPath + "/" + path` + `pathMatcher = UrlPathMatcher.of(path + "/*")`.
- **Migration note:** Cannot be memoized lazily because `fullPath` depends on the parent chain being fully initialized at construction. Keep on builder (or promote to constructor args) and document.
- **Specific questions:**
  - Q-R13.1: Does the `pathMatcher` ever need re-computation after construction? (If not, keep as a plain `final` field.)

##### R14. `beanContext` ✓ DONE — *(conservative: builder API retained)*

- **Status:** `protected final BeanContext beanContext` context field deleted. New `private final Memoizer<BeanContext> beanContextMemo = memoizer(this::findBeanContext)` plus `findBeanContext()` method on `RestContext` that simply returns `builder.beanContext().build()`. The ctor's `beanContext = bs.add(BeanContext.class, builder.beanContext().build());` collapsed to `bs.addBean(BeanContext.class, getBeanContext());`. `getBeanContext()` returns `beanContextMemo.get()`.
- **Why the conservative pattern (vs the Tier-E full deletion of the builder API):** `Builder.beanContext()` returns a `BeanContext.Builder` sub-builder that `init()` mutates via `apply(work)` (annotation work derived from `@BeanConfig`-style annotations), and that users may chain off (`b.beanContext().notBeanClasses(...)`). Deleting the sub-builder would require routing all `@BeanConfig`-style annotation processing through a new `findBeanContext()` annotation walk, plus migrating user-facing chained config to `@RestInject(name="beanContext") BeanContext.Builder` overrides. That's a separate, larger refactor. R14 just memoizes the `.build()` step on the `RestContext` side so other Tier-C work (R26/R31/R30/R32/R33) can layer on top later without churning the field-vs-memoizer plumbing again.
- **Resolved:**
  - Q-R14.1 — Deferred. Not needed for the memoization move; `init()`'s `apply(work)` still mutates the builder before the memoizer fires.
  - Q-R14.2 — Deferred. `bs.addBean(BeanContext.class, getBeanContext())` in the ctor still eagerly resolves; switching to `bs.addSupplier(...)` is a downstream optimization once the rest of the construction sequence is supplier-friendly.
  - Q-R14.3 — N/A; nothing in the codebase relies on `getBeanContext()` as a side-effecting trigger.

##### R15. `beanStore` *(Bootstrap-only)*

- **Current:** Builder field 270 (populated in `init()` line 1819 via `createBeanStore(resource).build()`). Context field 5009. Ctor 5090–5099 builds it and registers several foundational beans (`RestContext`, `ServletConfig`, `Object resource`, etc.).
- **Migration note:** The bean store itself is the foundational container. Cannot be memoized — it has to exist before any `findXxx()` runs. Keep at construction.
- **Specific questions:**
  - Q-R15.1: Can the registration sequence at 5090–5099 be pushed into a single `initBeanStore()` method so the ctor shrinks? (Cosmetic but helpful.)
  - Q-R15.2: Post-TODO-15 rename, this becomes `BeanStore` (no suffix) — confirm all call sites that still mention the old name.

##### R16. `rootBeanStore` *(Bootstrap-only)*

- **Current:** Builder field 271 (inherited from parent at 342, else created in `init()` 1827–1830). Context field 5010. Ctor 5088. Getter 5821.
- **Specific questions:**
  - Q-R16.1: Is the distinction between `rootBeanStore` and `beanStore` needed post-TODO-15? Or does the new `BeanStore` cover both? (Likely still needed — root is "the single ancestor bean store for the full resource tree".)

##### R17. `config` ✓ DONE

- **Current:** Builder field 279; getter `config()` 1012; setter 1033. Context field 5017. Ctor 5131 resolves via `createConfig(...)` (line 1836 in `init()`), then `builder.config().resolving(varResolver.createSession())`. Registered `bs.add(Config.class, ...)`.
- **Specific questions:**
  - Q-R17.1: Ordering — `config` depends on `varResolver`. Memoizer chain must respect this (first access to `config` triggers first access to `varResolver`). Confirm memoizer wiring handles this cleanly.
  - Q-R17.2: `@Rest(config)` supplies the config path; bean store supplies the `Config` instance. Both inputs need to flow into `findConfig()`.
- **Done (Phase 2 / R17, conservative):**
  - Removed `protected final Config config` field on `RestContext`.
  - Added `private final Memoizer<Config> configMemo = memoizer(this::findConfig)` and `findConfig()` returning `builder.config().resolving(getVarResolver().createSession())` — preserves the original "wrap unresolved config with a resolving variant backed by a fresh `varResolver` session" semantic. The unresolved `builder.config()` is already populated in `Builder.init()` via `createConfig(beanStore, resource(), restConfig)` (`@Rest(config)` annotation walk → SystemDefault fallback → bean-store override → `@RestInject` slot via `BeanCreateMethodFinder`), so memoizing the resolving step on `RestContext` is sufficient — every configuration path is preserved through the existing builder-side init.
  - Updated `getConfig()` to return the memoizer.
  - Updated ctor wiring: `bs.add(Config.class, getConfig())` (was `config = bs.add(Config.class, builder.config().resolving(vr.createSession()))`).
  - Memoizer chain ordering (Q-R17.1) is satisfied through the memoizer's lazy semantics: first call to `getConfig()` triggers `findConfig()` → `getVarResolver()` → R37's memoizer (which is independent of `getConfig()` because `findVarResolver()` uses `builder.config()` unresolved, not `getConfig()`). No cycle.
  - Q-R17.2 satisfied: `@Rest(config)` path-walk and bean-store/`@RestInject` overrides all live in the builder's existing `createConfig(...)` (called from `Builder.init()`); the `RestContext`-side memoizer just adds the SVL-resolving wrapper using the context's own `varResolver`. Post-R37 the dependency is explicit through `getVarResolver()`.
  - Cleaned up the now-redundant local `vr` variable in the ctor — replaced `var vr = getVarResolver(); bs.add(VarResolver.class, vr); bs.add(Config.class, ...)` with `bs.add(VarResolver.class, getVarResolver()); bs.add(Config.class, getConfig());` (memoizer guarantees same instance).
  - Build clean; full test suite passes.

##### R18. `consumes` *(Bootstrap-only)*

- **Current:** Builder field 288; getter `consumes()` 1043 returns `Optional<List<MediaType>>`; setter 1093. Context field 5028. Ctor 5165–5171 derives from `restOperations.getOpContexts()` when unset (retain-all across op parser media types). `@Rest(consumes)`.
- **Migration note:** Depends on `restOperations` being built first. `restOperations` is itself Bootstrap-only. Keep `consumes` Bootstrap-only unless we memoize the whole chain.
- **Specific questions:**
  - Q-R18.1: Could this become a memoizer whose `findConsumes()` pulls from `restOperations.get().getOpContexts()`? Feasible if `restOperations` itself is memoized. Bundle with R53 decision.

##### R19. `produces` *(Bootstrap-only)*

- **Current:** Same shape as R18 but derives from serializers. Builder field 289, getter 2680, setter 2732, context field 5029, ctor 5158–5164, getter 5774, `@Rest(produces)`.
- **Specific questions:** Q-R19.1: Same as R18 — bundle with `serializers` (R31) + `restOperations` (R53) decision.

##### R20. `defaultClasses` *(SUPERSEDED — deleted outright per Resolved Decision #25)*

- **Original target:** Memoize per Resolved Decision #16.
- **Final outcome:** Decision #25 (landed 2026-04-19) deletes `defaultClasses` entirely; the type-binding mechanism moved to `BasicBeanStore.addBeanType(...)` / `getBeanType(...)`. See FINISHED-16c "Decision #25" for the full migration story.

##### R21. `defaultSettings` *(SUPERSEDED — deleted outright per Resolved Decision #26)*

- **Original target:** Memoize per Resolved Decision #16.
- **Final outcome:** Decision #26 (landed 2026-04-19) deletes `defaultSettings` and `DefaultSettingsMap` entirely; the lone consumer (`debugDefault`) was promoted to a first-class `@Rest(debugDefault=...)` annotation attribute. See FINISHED-16c "Decision #26" for details.

##### R22. `defaultRequestAttributes` ✓ DONE

- **Current:** Builder field 302; lazy getter 1301 (→ `createDefaultRequestAttributes(...)`); adder 1356. Context field 5033. Ctor 5141 `bs.add(NamedAttributeMap.class, ..., PROP_defaultRequestAttributes)`. Getter 5617. `@Rest(defaultRequestAttributes)`.
- **Resolved:**
  - Q-R22.1 ✓ Resolved Decision #14 — `BeanCreateMethodFinder` hook replaced by `@RestInject(name="defaultRequestAttributes")` static method.
  - Q-R22.2: `RestOpContext.findDefaultRequestAttributes()` walks `@RestOp(defaultRequestAttributes)` first, then (if inherited) calls `context.getDefaultRequestAttributes()` as parent-fallback. Standard noInherit pattern (Resolved Decision #19 confirms the dependency call is OK).
- **Done (Phase 2 / R22, conservative):**
  - Removed `protected final NamedAttributeMap defaultRequestAttributes` field.
  - Added `private final Memoizer<NamedAttributeMap> defaultRequestAttributesMemo = memoizer(this::findDefaultRequestAttributes)` and `findDefaultRequestAttributes()` returning `builder.defaultRequestAttributes()` — preserves all configuration paths (`@Rest(defaultRequestAttributes)` annotation apply, programmatic `Builder.defaultRequestAttributes(NamedAttribute...)` setter, bean-store override via name `"defaultRequestAttributes"`, `@RestInject(name="defaultRequestAttributes")` slot via `BeanCreateMethodFinder`).
  - Updated `getDefaultRequestAttributes()` to return the memoizer.
  - Updated ctor wiring: `bs.add(NamedAttributeMap.class, getDefaultRequestAttributes(), PROP_defaultRequestAttributes)`.
  - `RestOpContext.findDefaultRequestAttributes()` parent-fallback through `context.getDefaultRequestAttributes()` is unaffected — same getter, just now memoized.
  - Build clean; full test suite passes.

##### R23. `defaultRequestHeaders` ✓ DONE

- **Current:** Builder field 283; lazy getter 1367; setter 1429. Context field 5022. Ctor 5139 (same `bs.add` pattern). Also mutated by `defaultAccept(String)` (1183) and `defaultContentType(String)` (1290), which are convenience setters. Getter 5630. `@Rest(defaultRequestHeaders / defaultAccept / defaultContentType)`.
- **Specific questions:**
  - Q-R23.1 ✓ Resolved Decision #15 — `defaultAccept` / `defaultContentType` survive as **first-class annotation attributes** on `@Rest` and `@RestOp`, internally folded into `findDefaultRequestHeaders()` as `Accept` / `Content-Type` entries.
  - Q-R23.2 ✓ Resolved Decision #14 — `BeanCreateMethodFinder` hook replaced by `@RestInject(name="defaultRequestHeaders")` static method.
- **Done (Phase 2 / R23, conservative):**
  - Removed `protected final HeaderList defaultRequestHeaders` field.
  - Added `private final Memoizer<HeaderList> defaultRequestHeadersMemo = memoizer(this::findDefaultRequestHeaders)` and `findDefaultRequestHeaders()` returning `builder.defaultRequestHeaders()` — preserves all configuration paths (`@Rest(defaultRequestHeaders)`, `@Rest(defaultAccept)`, `@Rest(defaultContentType)` annotation apply, programmatic `Builder.defaultRequestHeaders(Header...)` setter, `Builder.defaultAccept(String)` / `Builder.defaultContentType(String)` convenience setters, bean-store override, `@RestInject(name="defaultRequestHeaders")` slot via `BeanCreateMethodFinder`).
  - Updated `getDefaultRequestHeaders()` to return the memoizer.
  - Updated ctor wiring: `bs.add(HeaderList.class, getDefaultRequestHeaders(), PROP_defaultRequestHeaders)`.
  - Updated `properties()` debug accumulator to use `getDefaultRequestHeaders()`.
  - Build clean; full test suite passes.

##### R24. `defaultResponseHeaders` ✓ DONE

- **Current:** Builder field 284; lazy getter 1440; setter 1498. Context field 5023. Ctor 5140. Getter 5643. `@Rest(defaultResponseHeaders)`.
- **Resolved:** Q-R24.1 ✓ Resolved Decision #14 — `BeanCreateMethodFinder` hook replaced by `@RestInject(name="defaultResponseHeaders")` static method.
- **Done (Phase 2 / R24, conservative):**
  - Same shape as R23 — removed `protected final HeaderList defaultResponseHeaders`, added `defaultResponseHeadersMemo` + `findDefaultResponseHeaders()` returning `builder.defaultResponseHeaders()`, getter returns memoizer, ctor uses `bs.add(HeaderList.class, getDefaultResponseHeaders(), PROP_defaultResponseHeaders)`, `properties()` uses `getDefaultResponseHeaders()`.
  - Build clean; full test suite passes.

##### R25. `encoders` ✓ DONE

- **Current:** Builder field 282 (`EncoderSet.Builder`); lazy getter 1639; typed getter 1721; adders 1664/1688. Context field 5021. Ctor 5122 `bs.add(EncoderSet.class, builder.encoders().build())`. Getter 5650. `@Rest(encoders)`.
- **Resolved:**
  - Q-R25.1 ✓ Resolved Decision #9 — sub-builder chaining replaced by `@RestInject`. `findEncoders()` returns a built `EncoderSet` from the bean store (looked up by name `"encoders"`) or constructs from `@Rest(encoders={class array})`.
  - Q-R25.2 ✓ Resolved Decision #14 — `BeanCreateMethodFinder` hook replaced by `@RestInject(name="encoders")` static method.
- **Done (Phase 2 / R25, conservative):**
  - Removed `protected final EncoderSet encoders` field.
  - Added `private final Memoizer<EncoderSet> encodersMemo = memoizer(this::findEncoders)` and `findEncoders()` returning `builder.encoders().build()` — preserves all configuration paths (annotation apply, programmatic `Builder.encoders(...)` adders, bean-store override, `@RestInject` slot via `BeanCreateMethodFinder`).
  - Updated `getEncoders()` to return the memoizer.
  - Updated ctor wiring: `bs.add(EncoderSet.class, getEncoders())`.
  - Build clean; full test suite passes.

##### R26. `jsonSchemaGenerator` ✓ DONE

- **Current:** Builder field 287; lazy getter 1917; setters 1941/1964. Context field 5027. Ctor 5136. Getter 5674. Applied via `AnnotationWorkList`.
- **Specific questions:** Q-R26.1: Same sub-builder-chaining question as R25.
- **Done (Phase 2 / R26, conservative):**
  - Removed `protected final JsonSchemaGenerator jsonSchemaGenerator` field.
  - Added `private final Memoizer<JsonSchemaGenerator> jsonSchemaGeneratorMemo = memoizer(this::findJsonSchemaGenerator)` and `findJsonSchemaGenerator()` returning `builder.jsonSchemaGenerator().build()` — preserves the AnnotationWorkList apply path (`partSerializer().apply(work); partParser().apply(work); jsonSchemaGenerator().apply(work);` in `Builder.init`) and any programmatic `Builder.jsonSchemaGenerator(...)` setters.
  - Updated `getJsonSchemaGenerator()` to return the memoizer.
  - Updated ctor wiring: `bs.add(JsonSchemaGenerator.class, getJsonSchemaGenerator())` (was `jsonSchemaGenerator = bs.add(...)` with the field assignment).
  - Kept `Builder.jsonSchemaGenerator()` sub-builder API.
  - Build clean; full test suite passes.

##### R27. `logger` — **DONE (Phase 2)**

- **Status:** `Builder.logger` field, `Builder.logger()` lazy getter, `Builder.logger(Logger)` setter, and `Builder.createLogger(...)` method all deleted. `protected final Logger logger` context field deleted. New `private final Memoizer<Logger> loggerMemo = memoizer(this::findLogger)` plus `findLogger()` method on `RestContext` carry the same default-then-bean-store-then-`@RestInject` resolution that `createLogger` did. `getLogger()` returns from the memoizer. The ctor still eagerly forces resolution and dual-registers in the bean store: `var lg = getLogger(); bs.addBean(Logger.class, lg); bs.addBean(java.util.logging.Logger.class, lg);` — preserves bootstrap ordering for downstream beans (`callLogger`, `varResolver`, etc.) while letting the find logic live on the context. Future deferral (per Resolved Decision #19) just removes the eager `getLogger()` call from the ctor and switches the registrations to `bs.addSupplier(...)`; that change is gated on auditing the recursive-lookup risk inside `findLogger()` (the existing `beanStore.getBean(Logger.class).ifPresent(v::set)` step would self-recurse if the supplier were registered first).
- **Resolved:**
  - Q-R27.1 ✓ Dual registration kept as-is in the ctor — both `Logger.class` and `java.util.logging.Logger.class` slots are populated from the same memoizer result, so callers reading either type see the same instance.

##### R28. `messages` — **DONE (Phase 2 — Tier B)**

- **Status:** `Builder.messages` field, `Builder.messages()` lazy getter, both `Builder.messages(Class)` / `Builder.messages(Messages)` setters, and `Builder.createMessages(...)` method all deleted. `Messages.Builder.class` removed from the `DELAYED_INJECTION` set (`Messages.class` retained — `@RestInject Messages` is still routed through `findMessages()`). `protected final Messages messages` context field deleted. New `private final Memoizer<Messages> messagesMemo = memoizer(this::findMessages)` plus `findMessages()` method on `RestContext` walk the `@Rest` hierarchy parent→child, calling `Messages.Builder.location(svl-resolved-value)` for each non-empty `messages` attribute, then apply the same bean-store-then-`@RestInject` overrides that `createMessages` did. SVL resolution uses `builder.varResolver().build().createSession()` because `RestContext.varResolver` isn't initialized yet at the construction-time call (consistent with R27 — eager resolution before the field's own initialization). The ctor still eagerly forces resolution to preserve bootstrap ordering for `varResolver.bean(Messages.class, msgs)`: `var msgs = getMessages(); bs.addBean(Messages.class, msgs); varResolver = bs.add(VarResolver.class, builder.varResolver().bean(Messages.class, msgs).build());`. The apply-time push in `RestAnnotation.RestContextApply` (`b.messages().location(string(a.messages()).orElse(null))`) was deleted — annotations are now read directly by `findMessages()`. Added `PROPERTY_messages = "messages"` constant in `RestServerConstants` for `noInherit` matching parity. Migrated `Rest_Messages_Test.B3` from the deleted `builder.messages().location(null, "B2x").location(B1.class, "B1x")` builder API to the equivalent `@RestInject public static Messages messages(Messages.Builder b) { return b.location(null, "B2x").location(B1.class, "B1x").build(); }` form, preserving multi-class-keyed location chaining.
- **Resolved:**
  - Q-R28.1 ✓ `varResolver` depends on `messages` only at the bean-store level (`bean(Messages.class, msgs)`); resolved by computing `getMessages()` first, then building `varResolver` with that bean. The SVL-during-`findMessages` chicken-and-egg is sidestepped via the bootstrap resolver. **Updated post-R37**: `findMessages()` now uses `getSimpleVarResolver().createSession()` instead of the old `builder.varResolver().build().createSession()` workaround — same semantics (no `Messages`/`Config` beans), but the dependency is explicit through the two-resolver design rather than leaking through the builder's mutable VR builder.

##### R29. `methodExecStore` ✓ DONE

- **Status:** `Builder.methodExecStore` field, `Builder.methodExecStore()` lazy getter, both `Builder.methodExecStore(Class)` / `Builder.methodExecStore(MethodExecStore)` setters, and `Builder.createMethodExecStore(...)` method all deleted. `MethodExecStore.Builder.class` removed from the `DELAYED_INJECTION` set (`MethodExecStore.class` retained — `@RestInject MethodExecStore` is still routed through `findMethodExecStore()`). `protected final MethodExecStore methodExecStore` context field deleted. New `private final Memoizer<MethodExecStore> methodExecStoreMemo = memoizer(this::findMethodExecStore)` plus `findMethodExecStore()` method on `RestContext` mirrors the original `createMethodExecStore` flow: default builder → `defaultClasses.get(MethodExecStore.class)` type override → bean-store override → `@RestInject` override → `.build()`. Cross-dependency on `thrownStore` is satisfied inline via `MethodExecStore.create(beanStore).thrownStoreOnce(getThrownStore())` — calling the `getThrownStore()` accessor transparently triggers R36's memoizer on first access (Resolved Decision #19). The ctor no longer needs separate `bs.add(MethodExecStore.class, builder.methodExecStore().thrownStoreOnce(thrownStore).build())`; it now reads `bs.addBean(MethodExecStore.class, getMethodExecStore());`. Internal `getMethodExecStats(Method)` updated from `this.methodExecStore.getStats(m)` to `getMethodExecStore().getStats(m)`.
- **Resolved:**
  - Q-R29.1 ✓ Cross-dependency with `thrownStore` handled by direct `getThrownStore()` call inside `findMethodExecStore()`. Memoizer ordering is implicit — `MethodExecStore.Builder.thrownStoreOnce()` only takes effect if no other path has set thrownStore, mirroring the original `thrownStoreOnce` semantic.

##### R30. `parsers` ✓ DONE

- **Current:** Builder field 303; lazy getter 2354; typed getter 1753; adders 2379/2403. Related: `parserListener(Class)` 2323 (mutates via `parsers.forEach`). Context field 5034. Ctor 5124. Getter 5719. `@Rest(parsers)`.
- **Resolved:**
  - Q-R30.1: `parserListener()` mutator goes away with the builder. Users wire listeners through `@Rest(parsers)`-referenced parser classes' own `@ParserConfig` / `@SerializerConfig`-time listener config, or by supplying a fully-configured `ParserSet` via `@RestInject(name="parsers")`.
  - Q-R30.2 ✓ Resolved Decision #14 — `BeanCreateMethodFinder` hook replaced by `@RestInject(name="parsers")` static method.
- **Done (Phase 2 / R30, conservative):**
  - Removed `protected final ParserSet parsers` field.
  - Added `private final Memoizer<ParserSet> parsersMemo = memoizer(this::findParsers)` and `findParsers()` returning `builder.parsers().build()` — preserves all configuration paths (`@Rest(parsers)` annotation apply, programmatic `Builder.parsers(...)` adders, `Builder.parserListener(...)` mutator that walks `parsers.forEach(...)`, bean-store override, `@RestInject` slot via `BeanCreateMethodFinder`).
  - Updated `getParsers()` to return the memoizer (existing public getter — no API surface change).
  - Updated ctor wiring: `bs.add(ParserSet.class, getParsers())` (was `parsers = bs.add(...)` with field assignment).
  - Kept `Builder.parsers()` sub-builder API and `Builder.parserListener(...)` mutator.
  - Build clean; full test suite passes.

##### R31. `serializers` ✓ DONE

- **Current:** Builder field 310; lazy getter 3181; typed getter 1777; adders 3206/3230. Related: `serializerListener(Class)` 3150. Context field 5039. Ctor 5123. Getter 5828. `@Rest(serializers)`.
- **Resolved:** Q-R31.1: Same pair as R30 — listener mutator dies with the builder; `BeanCreateMethodFinder` → `@RestInject(name="serializers")`.
- **Done (Phase 2 / R31, conservative):**
  - Removed `protected final SerializerSet serializers` field.
  - Added `private final Memoizer<SerializerSet> serializersMemo = memoizer(this::findSerializers)` and `findSerializers()` returning `builder.serializers().build()` — same shape as R30, preserves all configuration paths including the `Builder.serializerListener(...)` mutator path.
  - Updated `getSerializers()` to return the memoizer.
  - Updated ctor wiring: `bs.add(SerializerSet.class, getSerializers())`.
  - Build clean; full test suite passes.

##### R32. `partParser` ✓ DONE

- **Current:** Builder field 285 (`HttpPartParser.Creator`); lazy getter 2433; setters 2457/2480. Context field 5024. Ctor 5135. Getter 5728. `@Rest(partParser)`.
- **Resolved:** Q-R32.1 — strip the `HttpPartParser.Creator` indirection. `findPartParser()` either reads a fully-built `HttpPartParser` from the bean store via `@RestInject(name="partParser")`, or instantiates the class named in `@Rest(partParser=...)` directly. The Creator pattern was a vestige of the builder's lazy-construction model and serves no purpose under memoization.
- **Done (Phase 2 / R32, conservative — Creator indirection NOT stripped):**
  - Removed `protected final HttpPartParser partParser` field.
  - Added `private final Memoizer<HttpPartParser> partParserMemo = memoizer(this::findPartParser)` and `findPartParser()` returning `builder.partParser().create()` — preserves the existing `HttpPartParser.Creator` indirection (the conservative refactor; Q-R32.1's full-strip is deferred).
  - Updated `getPartParser()` to return the memoizer.
  - Updated ctor wiring: `bs.add(HttpPartParser.class, getPartParser())`.
  - Updated `properties()` debug accumulator to use `getPartParser()`.
  - Kept `Builder.partParser()` sub-builder API and the `HttpPartParser.Creator` plumbing.
  - Build clean; full test suite passes.

##### R33. `partSerializer` ✓ DONE

- **Current:** Builder field 286 (`HttpPartSerializer.Creator`); lazy getter 2509; setters 2533/2556. Context field 5025. Ctor 5134. Getter 5737. `@Rest(partSerializer)`.
- **Resolved:** Q-R33.1 — same as R32.
- **Done (Phase 2 / R33, conservative — Creator indirection NOT stripped):**
  - Removed `protected final HttpPartSerializer partSerializer` field.
  - Added `private final Memoizer<HttpPartSerializer> partSerializerMemo = memoizer(this::findPartSerializer)` and `findPartSerializer()` returning `builder.partSerializer().create()` — same shape as R32.
  - Updated `getPartSerializer()` to return the memoizer.
  - Updated ctor wiring: `bs.add(HttpPartSerializer.class, getPartSerializer())`.
  - Updated `properties()` debug accumulator to use `getPartSerializer()`.
  - Build clean; full test suite passes.

##### R34. `responseProcessors` ✓ DONE

- **Current:** Builder field 308; lazy getter 2887; adders 2912/2936. Context field `ResponseProcessor[]` 5038. Ctor 5132 `bs.add(ResponseProcessor[].class, ...toArray())`. **No public getter.** `@Rest(responseProcessors)`.
- **Resolved:** Q-R34.1 — promote to `public ResponseProcessor[] getResponseProcessors()` (memoized). No downside; matches the rest of the API surface.
- **Done (Phase 2 / R34, conservative):**
  - Removed `protected final ResponseProcessor[] responseProcessors` field on `RestContext`.
  - Added `private final Memoizer<ResponseProcessor[]> responseProcessorsMemo = memoizer(this::findResponseProcessors)` and `findResponseProcessors()` returning `builder.responseProcessors().build().toArray()` — the underlying `ResponseProcessorList.Builder` is the same object that `Builder.responseProcessors()` lazy-creates and that `Rest`-annotation apply work mutates via `b.responseProcessors().add(...)`, so memoizing the `.build()` step preserves all configuration paths (annotations, programmatic `responseProcessors(...)` adders, `@RestInject` slot, bean-store override).
  - Added new public getter `public ResponseProcessor[] getResponseProcessors()` returning the memoized array (matches the rest of the public getter surface — `getCallLogger()`, `getStaticFiles()`, etc.).
  - Updated `processResponse(...)` to grab a local `var rp = getResponseProcessors()` and iterate `rp[i]` (was iterating the now-removed field).
  - Updated `properties()` debug accumulator to use `getResponseProcessors()`.
  - Updated ctor wiring: `bs.add(ResponseProcessor[].class, getResponseProcessors())` (was `bs.add(ResponseProcessor[].class, builder.responseProcessors().build().toArray())`) — same lookup key/value, but now driven by the memoizer so any subsequent direct getter call sees the same array instance.
  - Kept `Builder.responseProcessors()` sub-builder API and `createResponseProcessors(...)` factory — they remain the configuration surface (annotation apply work and user code mutate the list builder via `.add(...)` before init completes).
  - `RestAnnotation.RestContextApply` keeps the existing `b.responseProcessors().add(a.responseProcessors())` line; no change needed there for this conservative memoization.
  - Build clean; full test suite passes.

##### R35. `restOpArgs` ✓ DONE

- **Current:** Builder field 306; lazy getter 3018; adder 3038. Context field `Class<? extends RestOpArg>[]` 5015. Ctor 5142. **No public getter** (used internally in `findRestOperationArgs` 6193). `@Rest(restOpArgs)`.
- **Resolved:** Q-R35.1 — Memoize as `Class<? extends RestOpArg>[]`. Per-op `RestOpArg` instances are resolved separately at per-op setup via the bean store; that path stays unchanged.
- **Done (Phase 2 / R35, conservative):**
  - Removed `protected final Class<? extends RestOpArg>[] restOpArgs` field on `RestContext`.
  - Added `private final Memoizer<Class<? extends RestOpArg>[]> restOpArgsMemo = memoizer(this::findRestOpArgs)` and `findRestOpArgs()` returning `builder.restOpArgs().build().asArray()` — same `RestOpArgList.Builder` that `Builder.restOpArgs()` lazy-creates and that `@Rest(restOpArgs)` annotation apply work mutates via `b.restOpArgs().add(...)`, plus programmatic `Builder.restOpArgs(Class...)` adders, bean-store override, and `@RestInject` slot via `BeanCreateMethodFinder`.
  - Promoted to **new public** `getRestOpArgs()` (matches the rest of the public getter surface — `getResponseProcessors()`, `getCallLogger()`, etc.; closes Q-R35.1).
  - Updated `findRestOperationArgs(Method, BasicBeanStore)` to grab a local `var roa = getRestOpArgs()` once per call and iterate `roa` instead of the now-removed field.
  - Updated `properties()` debug accumulator to use `getRestOpArgs()`.
  - Removed the eager `restOpArgs = builder.restOpArgs().build().asArray()` ctor assignment — the memoizer handles it lazily (first call is during op setup via `findRestOperationArgs`, while `builder` is still alive as a field).
  - Kept `Builder.restOpArgs()` sub-builder API and `createRestOpArgs(...)` factory — they remain the configuration surface (annotation apply work and user code mutate the list builder via `.add(...)` before init completes).
  - `RestAnnotation.RestContextApply` keeps the existing `b.restOpArgs().add(a.restOpArgs())` line unchanged.
  - Per-op `RestOpArg` instance resolution at `findRestOperationArgs` (`BeanCreator.of(RestOpArg.class, ...).type(c).run()`) is unchanged — still goes through the bean store as before.
  - Build clean; full test suite passes.

##### R36. `thrownStore` ✓ DONE

- **Status:** `Builder.thrownStore` field, `Builder.thrownStore()` lazy getter, both `Builder.thrownStore(Class)` / `Builder.thrownStore(ThrownStore)` setters, and `Builder.createThrownStore(...)` method all deleted. `ThrownStore.Builder.class` removed from the `DELAYED_INJECTION` set (`ThrownStore.class` retained — `@RestInject ThrownStore` is still routed through `findThrownStore()`). `protected final ThrownStore thrownStore` context field deleted. New `private final Memoizer<ThrownStore> thrownStoreMemo = memoizer(this::findThrownStore)` plus `findThrownStore()` method on `RestContext` mirrors the original `createThrownStore` flow: default builder seeded with `parentContext.getThrownStore()` for parent inheritance → `defaultClasses.get(ThrownStore.class)` type override → bean-store override → `@RestInject` override → `.build()`. Ctor uses `bs.addBean(ThrownStore.class, getThrownStore());`. `@Rest` annotation has no `thrownStore` attribute, so no annotation walk is needed (Tier-A leaf with parent-fallback only).
- **Resolved:** Standard questions only — no annotation, no SVL, no cross-deps from this side. Used by R29 `methodExecStore` and R38 `callLogger`.

##### R37. `varResolver` ✓ DONE — **two-resolver design**

- **Status:** `Builder.varResolver` (was a `VarResolver.Builder`), the `Builder.varResolver()` getter, both `Builder.vars(Class<? extends Var>...)` and `Builder.vars(Var...)` setters, and `Builder.createVarResolver(...)` method all deleted. `VarResolver.Builder.class` removed from the `DELAYED_INJECTION` set (`VarResolver.class` retained — `@RestInject VarResolver` is still routed through `findVarResolver()`). `protected final VarResolver varResolver` context field deleted.
- **Two-resolver design.** The previous code rebuilt a single `VarResolver` three times during construction (once in `init()` without Config, once after Config existed, once in the ctor with Messages). That dance is gone. Replaced with two independent memoized resolvers:
  - **`simpleVarResolver`** — bootstrap-time resolver with the full `Var` catalog (`ConfigVar`, `LocalizationVar`, `RequestVar`, …) and a `FileFinder` bean, but **no `Messages` bean and no `Config` bean**. Built by `Builder.createSimpleVarResolver(...)` and cached on the builder via `Builder.simpleVarResolver()` so the same instance is shared by `init()` and the `RestContext` memoizer (`simpleVarResolverMemo`/`findSimpleVarResolver()`). `LocalizationVar`/`ConfigVar` references silently resolve to empty strings at this stage, which matches the previous behavior at the corresponding bootstrap points.
  - **`varResolver`** — the runtime resolver returned by `getVarResolver()`. Built by `findVarResolver()` as `getSimpleVarResolver().copy().bean(Messages.class, getMessages()).bean(Config.class, builder.config())`, plus the standard `beanStore.getBean(VarResolver.class)` and `@RestInject VarResolver` overrides. The `Config` bean wired into the runtime resolver is the **bootstrap** Config (no resolving session) — using the runtime Config here would create a recursion through `ConfigVar` ↔ `Config.resolving(session)`. The runtime Config (`config` field on `RestContext`) is created in the ctor by wrapping the bootstrap Config with `getVarResolver().createSession()`.
- **`@RestInject` slots.**
  - Unnamed `@RestInject VarResolver` → overrides the **runtime** resolver (matches prior semantics).
  - Named `@RestInject(name="simpleVarResolver") VarResolver` → overrides the **bootstrap** resolver (rare — typically only needed if a user wants custom var lookups available during annotation resolution).
  - Added `PROP_simpleVarResolver = "simpleVarResolver"` constant and added it to `DELAYED_INJECTION_NAMES` so the init-time `@RestInject` walk skips it (it's invoked later by `createSimpleVarResolver(...)`).
- **`Builder.init()` simplified.** The previous three-line dance (`varResolver = createVarResolver(...); bs.add(VarResolver.class, varResolver.build()); ...; bs.add(VarResolver.class, varResolver.bean(Config.class, config).build());`) collapsed to `bs.add(VarResolver.class, simpleVarResolver()); config = bs.add(Config.class, createConfig(bs, r, rc));`. The annotation-work session at line 1469 now uses `simpleVarResolver().createSession()` instead of `varResolver().build().createSession()`.
- **Ctor simplified.** `varResolver = bs.add(VarResolver.class, builder.varResolver().bean(Messages.class, msgs).build()); config = bs.add(Config.class, builder.config().resolving(varResolver.createSession()));` collapsed to `var vr = getVarResolver(); bs.add(VarResolver.class, vr); config = bs.add(Config.class, builder.config().resolving(vr.createSession()));` — the runtime resolver is built once via the memoizer chain and replaces the simple resolver in the bean store.
- **R28 cleanup.** `findMessages()` previously used `builder.varResolver().build().createSession()` as a workaround. Now uses the explicit `getSimpleVarResolver().createSession()` — same semantics (no Messages/Config beans), but the dependency is explicit instead of leaking through the builder's mutable VR builder.
- **Resolved:**
  - Q-R37.1 ✓ Memoizer dependency policy works (Resolved Decision #19): `findVarResolver()` calls `getSimpleVarResolver()` and `getMessages()` directly. The Config chicken-and-egg is sidestepped by using the bootstrap `builder.config()` — when R17 lands, this becomes `findVarResolver()` → `findConfig().getBootstrap()` (or equivalent helper).
  - Q-R37.2: TODO-14 unaffected by R37; the TODO-14 win remains independent — once `VarResolver` is in commons, both memoizers can drop the `BasicBeanStore` parameter.
- **`init()` order constraint** retained: `simpleVarResolver()` must be cached on the builder so that the `RestContext` memoizer (`findSimpleVarResolver` reads `builder.simpleVarResolver()`) sees the same instance that init's `apply(work)` saw. Without this, two different simple resolvers would exist, one of which would re-invoke any `@RestInject(name="simpleVarResolver")` method (it has been DELAYED to the consumer-side findX, but the builder caches the result regardless to keep init() and the memoizer aligned).

##### R38. `callLogger` ✓ DONE *(was `BeanCreator`-based)*

- **Status:** `Builder.callLogger` field (was `BeanCreator<CallLogger>`), the `Builder.callLogger()` lazy getter, both `Builder.callLogger(CallLogger)` / `Builder.callLogger(Class<? extends CallLogger>)` setters, and `Builder.createCallLogger(...)` factory all deleted. `CallLogger.Builder.class` removed from the `DELAYED_INJECTION` set (`CallLogger.class` retained — `@RestInject CallLogger` is still routed through `findCallLogger()`). `protected final CallLogger callLogger` context field deleted. New `private final Memoizer<CallLogger> callLoggerMemo = memoizer(this::findCallLogger)` plus `findCallLogger()` method on `RestContext` walks the `@Rest(callLogger=...)` hierarchy via `getRestAnnotationsForProperty(PROPERTY_callLogger)` (most-derived non-`CallLogger.Void.class` wins) and applies the same `defaultClasses` → annotation → bean-store → `@RestInject` chain that `createCallLogger` did. Ordering matters here — annotations must be applied **after** `defaultClasses` so the annotation-supplied class wins (preserves the original behavior where `RestContextApply.apply()` mutated the BeanCreator after `createCallLogger` had set the default). Returns `creator.orElse(null)` so `BeanCreator` resolves `getInstance()` static factories (used by tests like `Rest_Debug_Test.CaptureLogger.getInstance()`). The ctor's `callLogger = bs.add(CallLogger.class, builder.callLogger().orElse(null));` collapsed to `bs.addBean(CallLogger.class, getCallLogger());`. Added `PROPERTY_callLogger` constant in `RestServerConstants`. Deleted the `type(a.callLogger()).ifPresent(x -> b.callLogger().type(x));` push from `RestAnnotation.RestContextApply.apply()` — the annotation is now read directly by `findCallLogger()`. Updated the `RestInject` javadoc table to drop the (unused) `CallLogger.Builder` slot.
- **Resolved:**
  - Q-R38.1 ✓ The builder-time `BeanCreator<CallLogger>` is gone, but `BeanCreator` itself is still used **inside** `findCallLogger()` to handle the `getInstance()` / static-factory / constructor-injection logic for the resolved `CallLogger` type. The user-facing builder API was the goal; the internal use of `BeanCreator` for instantiation remains the cleanest way to honor the `getInstance()` convention.
  - **Test compatibility note:** `Rest_Debug_Test.CaptureLogger.getInstance()` returns a singleton `LOGGER`. `BeanCreator.run()` detects the `getInstance()` static method and calls it, so the singleton routing continues to work without any explicit handling in `findCallLogger()`.

##### R39. `debugEnablement` ✓ DONE *(was `BeanCreator`-based)*

- **Status:** `Builder.debugEnablement` field (was `BeanCreator<DebugEnablement>`), the `Builder.debugEnablement()` lazy getter, both `Builder.debugEnablement(DebugEnablement)` / `Builder.debugEnablement(Class<? extends DebugEnablement>)` setters, and `Builder.createDebugEnablement(...)` factory all deleted. `DebugEnablement.Builder.class` removed from the `DELAYED_INJECTION` set (`DebugEnablement.class` retained — `@RestInject DebugEnablement` is still routed through `findDebugEnablement()`). `protected final DebugEnablement debugEnablement` context field deleted. New `private final Memoizer<DebugEnablement> debugEnablementMemo = memoizer(this::findDebugEnablement)` plus `findDebugEnablement()` method on `RestContext` walks the `@Rest(debugEnablement=...)` hierarchy via `getRestAnnotationsForProperty(PROPERTY_debugEnablement)` (most-derived non-`DebugEnablement.Void.class` wins) and applies the same `defaultClasses` → annotation → bean-store → `@RestInject` chain that `createDebugEnablement` did. Ordering matches R38: annotations applied **after** `defaultClasses` so the annotation-supplied class wins. Returns `creator.orElse(null)` so `BeanCreator` resolves any `getInstance()` static factories the consumer provides. The ctor's `debugEnablement = bs.add(DebugEnablement.class, builder.debugEnablement().orElse(null));` collapsed to `bs.addBean(DebugEnablement.class, getDebugEnablement());`. The single `debugEnablement.isDebug(...)` call inside `RestContext` (in the request-debug check) was rewritten to `getDebugEnablement().isDebug(...)`. Added `PROPERTY_debugEnablement` constant in `RestServerConstants`. Deleted the `type(a.debugEnablement()).ifPresent(x -> b.debugEnablement().type(x));` push from `RestAnnotation.RestContextApply.apply()` — the annotation is now read directly by `findDebugEnablement()`. Updated the `RestInject` javadoc table to drop the (unused) `DebugEnablement.Builder` slot. All `RestContext.Builder#debugEnablement()` `@link`/`@see` references in `Rest`, `RestOp`, `RestGet`, `RestPut`, `RestPost`, `RestPatch`, `RestDelete`, `RestOptions`, `CallLogger` rewritten to `RestContext#getDebugEnablement()`.
- **Resolved:**
  - Q-R39.1 ✓ Same conclusion as R38: builder-time `BeanCreator<DebugEnablement>` is gone, but `BeanCreator` itself is still used **inside** `findDebugEnablement()` to honor static factories / constructor injection / `getInstance()` for the resolved type.

##### R40. `staticFiles` ✓ DONE *(was `BeanCreator`-based)*

- **Status:** `Builder.staticFiles` field (was `BeanCreator<StaticFiles>`), the `Builder.staticFiles()` lazy getter, both `Builder.staticFiles(Class<? extends StaticFiles>)` / `Builder.staticFiles(StaticFiles)` setters, and `Builder.createStaticFiles(...)` factory all deleted. `StaticFiles.Builder.class` removed from the `DELAYED_INJECTION` set (`StaticFiles.class` retained — `@RestInject StaticFiles` is still routed through `findStaticFiles()`). `protected final StaticFiles staticFiles` context field deleted. New `private final Memoizer<StaticFiles> staticFilesMemo = memoizer(this::findStaticFiles)` plus `findStaticFiles()` method on `RestContext` walks the `@Rest(staticFiles=...)` hierarchy via `getRestAnnotationsForProperty(PROPERTY_staticFiles)` (most-derived non-`StaticFiles.Void.class` wins) and applies the same `defaultClasses` → annotation → bean-store → `@RestInject` chain that `createStaticFiles` did. Ordering matches R38/R39: annotations applied **after** `defaultClasses` so the annotation-supplied class wins. Returns `creator.orElse(null)` so `BeanCreator` resolves any `getInstance()` static factories. Constructor's `staticFiles = bs.add(StaticFiles.class, builder.staticFiles().orElse(null)); bs.add(FileFinder.class, staticFiles);` collapsed to a single `getStaticFiles()` call followed by `bs.addBean(StaticFiles.class, sf); bs.addBean(FileFinder.class, sf);` — the dual `FileFinder` registration is **kept** because `BasicSwaggerProvider` (and indirectly any `RestContextArgs`-injected `FileFinder` parameter) reads `FileFinder.class` from the bean store. The single-instance `getStaticFiles()`/`staticFilesMemo` guarantees both registrations point to the same bean. The `toBuilder().a(PROP_staticFiles, staticFiles)` site rewritten to `getStaticFiles()`. Added `PROPERTY_staticFiles` constant in `RestServerConstants`. Deleted the `type(a.staticFiles()).ifPresent(x -> b.staticFiles().type(x));` push from `RestAnnotation.RestContextApply.apply()` — the annotation is now read directly by `findStaticFiles()`. Updated the `RestInject` javadoc table to drop the (unused) `StaticFiles.Builder` slot.
- **Resolved:**
  - Q-R40.1 ✓ Per the `BasicSwaggerProvider` dependency, the `FileFinder.class` bean-store registration **stays**; it just shares the single memoized `StaticFiles` instance instead of being a separately-creator'd bean. No new `@RestInject(name="fileFinder")` slot needed — the bean store entry is sufficient and matches the historical contract.
  - **`FileFinder.Builder.class` / `FileFinder.class` retained in `DELAYED_INJECTION`:** these still gate the eager `@RestInject` walk for `FileFinder`-typed methods, since the constructor still installs `FileFinder` post-init from the resolved `StaticFiles`.

##### R41. `swaggerProvider` ✓ DONE *(was `BeanCreator`-based)*

- **Status:** `Builder.swaggerProvider` field (was `BeanCreator<SwaggerProvider>`), the `Builder.swaggerProvider()` lazy getter, both `Builder.swaggerProvider(Class<? extends SwaggerProvider>)` / `Builder.swaggerProvider(SwaggerProvider)` setters, and `Builder.createSwaggerProvider(...)` factory all deleted. `SwaggerProvider.Builder.class` removed from the `DELAYED_INJECTION` set (`SwaggerProvider.class` retained — `@RestInject SwaggerProvider` is still routed through `findSwaggerProvider()`). `protected final SwaggerProvider swaggerProvider` context field deleted. New `private final Memoizer<SwaggerProvider> swaggerProviderMemo = memoizer(this::findSwaggerProvider)` plus `findSwaggerProvider()` method on `RestContext` walks the `@Rest(swaggerProvider=...)` hierarchy via `getRestAnnotationsForProperty(PROPERTY_swaggerProvider)` (most-derived non-`SwaggerProvider.Void.class` wins) and applies the same `defaultClasses` → annotation → bean-store → `@RestInject` chain that `createSwaggerProvider` did. Ordering matches R38–R40: annotations applied **after** `defaultClasses` so the annotation-supplied class wins. Returns `creator.orElse(null)` so `BeanCreator` resolves any `getInstance()` static factories. Constructor's `swaggerProvider = bs.add(SwaggerProvider.class, builder.swaggerProvider().orElse(null));` collapsed to `bs.addBean(SwaggerProvider.class, getSwaggerProvider());`. The `getSwagger(Locale)` site rewritten from `swaggerProvider.getSwagger(this, locale)` to `getSwaggerProvider().getSwagger(this, locale)`; the `toBuilder().a(PROP_swaggerProvider, swaggerProvider)` site updated to `getSwaggerProvider()`. The per-locale `swaggerCache` (a `ConcurrentHashMap<Locale, Swagger>`) **stays as-is** per Q-R41.1 — it's a runtime cache, not configuration, and doesn't fit the single-value `Memoizer` pattern. Added `PROPERTY_swaggerProvider` constant in `RestServerConstants`. Deleted the `type(a.swaggerProvider()).ifPresent(b::swaggerProvider);` push from `RestAnnotation.RestContextApply.apply()` — note this one used the `b::swaggerProvider` setter directly (unlike R38–R40 which went through `b.x().type(…)`); the annotation is now read directly by `findSwaggerProvider()`. Updated the `RestInject` javadoc table to drop the (unused) `SwaggerProvider.Builder` slot. Updated `RestContext.getSwaggerProvider()` javadoc `See Also` to point at `@Rest(swaggerProvider)` instead of the deleted builder setters.
- **Resolved:**
  - Q-R41.1 ✓ Confirmed: `swaggerProvider` becomes a single memoized field; `swaggerCache` is left untouched.

##### R42–R48. Lifecycle method lists (destroy / endCall / postCall / postInitChildFirst / postInit / preCall / startCall) *(unified per Resolved Decision #18)*

- **Current:** Builder fields 294–300; lazy getters 1547/1699/2633/2644/2655/2669/3241 (each → `create*(beanStore(), resource())`). Context fields 5056–5062. Ctor lines 5144–5150. Getters: most are package-private / not exposed; `getPostCallMethods()` 6224, `getPreCallMethods()` 6231.
- **Target shape (uniform across R42–R48):**
  - All seven move to `Memoizer<MethodList>` (R45/R46/R48 migrate from `MethodInvoker[]` to `MethodList`).
  - `findXxxMethods()` scans the resource class for `@RestDestroy` / `@RestStartCall` / etc. annotations, instantiates invokers via the bean store, returns a `MethodList`.
  - Promote all seven to `public MethodList getXxxMethods()` (currently only R44/R47 are public). Internal callers convert to whatever iteration shape they need.
- **Resolved:**
  - Q-Rlife.1 ✓ Resolved Decision #19 — memoizer can call `getBeanStore()` to instantiate invokers at first access.
  - Q-Rlife.2 ✓ Resolved Decision #18 — unify on `MethodList`.
  - Q-Rlife.3 ✓ Promote all to public `getXxxMethods()` (uniform API surface beats internal asymmetry).

##### R49–R57. Bootstrap-only (children, operations, identity)

With both builders deleted, these become **constructor arguments** on `RestContext`:

- **R49 `children`** (line 290) — list of child resource classes/instances/`RestChild`s. Passed to the constructor; feeds `restChildren` (R52). Source: `@Rest(children={...})` on the resource class + any instances supplied by the parent/servlet container.
- **R50 `childrenClass`** (line 276) — `Class<? extends RestChildren>`. **SUPERSEDED** — Decision #24 deleted this entirely.
- **R51 `opContextClass`** (line 277) — `Class<? extends RestOpContext>`. **SUPERSEDED** — Decision #24 deleted this entirely.
- **R52 `restChildren`** — can be memoized; `findRestChildren()` builds from R49/R50. Must be built before the parent resolves its own `restOperations`.
- **R53 `restOperations`** — can be memoized; `findRestOperations()` scans `@RestOp`-annotated methods and instantiates `RestOpContext`s. Referenced by R18 (`consumes`) and R19 (`produces`), so the memoizer chain ordering is: `restOperations` → { `parsers`, `serializers`, `encoders`, etc. } → `restOpContexts` → `consumes`/`produces`.
- **R54 `resource`** (line 309) — the resource bean supplier. Constructor arg. Registered in bean store immediately.
- **R55 `resourceClass`** (line 278) — immutable `final Class<?>` identity. Constructor arg.
- **R56 `parentContext`** (line 304) — immutable `final RestContext` reference. Constructor arg.
- **R57 `inner`** (line 311) — `final ServletConfig`. Constructor arg.
- **Specific questions:**
  - Q-Rboot.1: R52 and R53 — confirm they can be memoized rather than eager-built. Cycle risk: child `RestContext`s and `RestOpContext`s reference back to the parent; validate that first-access memoization doesn't create a deadlock or double-init path.
  - Q-Rboot.2 ✓ Resolved by Decision #23 — `RestContextInit` record bundles R49, R54–R57 plus optional pre-build `beanStoreConfigurer`.

##### R58. `initialized` (builder flag) *(Dead)*

- Internal one-shot guard line 273, flipped in `init(Supplier<?>)` line 1812. Not externally configurable. **Delete outright** along with the builder consolidation. Unrelated to `AtomicBoolean initialized` field at 5006 (runtime flag, kept).

---

#### `RestOpContext` — summary table

"`noInherit`?" column: **L** = list-valued append/negate (uses `@RestOp(noInherit=...)`), **S** = scalar inherit yes/no, **—** = not inheritable.

| # | Setting | Category | `noInherit`? | Builder field line | Context field line | Bean-store? |
|---|---|---|---|---|---|---|
| — | `restOpAnnotations` | Already-memoized | — | — | 2191 | No |
| — | `noInheritOp` | Already-memoized | — | — | 2200 | No |
| — | `allowedParserOptions` | Already-memoized (reference) | L | — | 2211 | No |
| — | `allowedSerializerOptions` | Already-memoized (reference) | L | — | 2225 | No |
| Op1 | `beanContext` | Composite-bean-lookup | S | 110 | 2153 | Yes |
| Op2 | `beanStore` | Bootstrap-only | — | 111 | — | Self |
| Op3 | `dotAll` | Memoize | S | 112 | 2150 | No |
| Op4 | `defaultCharset` | Memoize | S | 113 | 2155 | No |
| Op5 | `encoders` | Composite-bean-lookup | L | 114 | 2157 | Yes |
| Op6 | `debug` (Enablement) | Memoize | S | 115 | 2156 | Partial |
| Op7 | `defaultRequestHeaders` | Composite-bean-lookup | L | 116 | 2158 | Yes |
| Op8 | `defaultResponseHeaders` | Composite-bean-lookup | L | 117 | 2159 | Yes |
| Op9 | `partParser` | Composite-bean-lookup | S | 118 | 2160 | Yes |
| Op10 | `partSerializer` | Composite-bean-lookup | S | 119 | 2161 | Yes |
| Op11 | `jsonSchemaGenerator` | Composite-bean-lookup | S | 120 | 2162 | Yes |
| Op12 | `consumes` → `supportedContentTypes` | Memoize (parser-derived default) | L | 121 | 2164 | No |
| Op13 | `produces` → `supportedAcceptTypes` | Memoize (serializer-derived default) | L | 122 | 2163 | No |
| Op14 | `path` → `pathMatchers[]` | Composite-bean-lookup | L | 123 | 2184 | Yes |
| Op15 | `maxInput` | Memoize | S | 124 | 2152 | No |
| Op16 | `restMethod` | Bootstrap-only | — | 125 | 2167 | No |
| Op17 | `defaultRequestAttributes` | Composite-bean-lookup | L | 126 | 2169 | Yes |
| Op18 | `parsers` | Composite-bean-lookup | L | 127 | 2172 | Yes |
| Op19 | `defaultRequestFormData` | Composite-bean-lookup | L | 128 | 2170 | Yes |
| Op20 | `defaultRequestQueryData` | Composite-bean-lookup | L | 129 | 2171 | Yes |
| Op21 | `restContext` | Bootstrap-only | — | 130 | 2173 | No |
| Op22 | `parent` (RestContext.Builder) | Bootstrap-only | — | 131 | — | No |
| Op23 | `converters` | Composite-bean-lookup | L | 132 | 2174 | Yes |
| Op24 | `guards` (+ `roleGuard` + `rolesDeclared`) | Composite-bean-lookup | L | 133, 136, 137 | 2175 | Yes |
| Op25 | `matchers` (+ `clientVersion`) | Composite-bean-lookup | L | 134, 138 | 2176 / 2177 | Yes |
| Op26 | `serializers` | Composite-bean-lookup | L | 135 | 2182 | Yes |
| Op27 | `httpMethod` | Memoize | S | 139 | 2183 | No |

**Totals:** 4 already-memoized · 7 Memoize candidates (Op3, Op4, Op6, Op12, Op13, Op15, Op27) · 15 Composite-bean-lookup (Op1, Op5, Op7–Op11, Op14, Op17–Op20, Op23–Op26) · 5 Bootstrap-only (Op2, Op16, Op21, Op22, plus the `path` composite at Op14).

---

#### `RestOpContext` — per-setting migration blocks

> Standard migration questions S1–S8 apply to every Memoize / Composite-bean-lookup entry.

##### Op1. `beanContext` ✓ DONE

- **Current:** Builder field 110; accessor `beanContext()` 223 (lazy via `createBeanContext` 1450). Context field 2153. Ctor 2315 `bs.add(BeanContext.class, builder.getBeanContext().orElse(context.getBeanContext()))`. Getter 2483. Applied via `apply(work)` ctor 174–175.
- **Resolved:**
  - Q-Op1.1: `findBeanContext()` falls back to `context.getBeanContext()` if no `@RestInject(name="beanContext")` bean and no `@RestOp`-derived override exists. Standard parent-fallback per Resolved Decision #19.
  - Q-Op1.2 ✓ Resolved Decision #14 — `BeanCreateMethodFinder<BeanContext>` replaced by `@RestInject(name="beanContext")` static method.
- **Done (Phase 2 / Op1, conservative):**
  - Removed `protected final BeanContext beanContext` field on `RestOpContext`.
  - Added `private final Memoizer<BeanContext> beanContextMemo = memoizer(this::findBeanContext)` and `findBeanContext()` returning `builder.getBeanContext().orElse(context.getBeanContext())` — preserves the parent-fallback shape and the `@RestInject(name="beanContext")` slot via the builder's `Optional` resolution.
  - Updated `getBeanContext()` to return the memoizer.
  - Updated ctor wiring: `bs.add(BeanContext.class, getBeanContext())`.
  - Added `protected final Builder builder` field on `RestOpContext` (assigned in ctor) so the lazy memoizers can read from it post-construction — same pattern as `RestContext`.
  - Build clean; full test suite passes.

##### Op2. `beanStore` *(Bootstrap-only)*

- **Current:** Builder field 111; setters at 237/257/280/1434. **Not stored on `RestOpContext`** — ctor lines 2308–2313 build a fresh `BasicBeanStore` layered on `context.getRootBeanStore()`.
- **Migration note:** Build-time plumbing only. Delete from any post-refactor public API; keep as internal ctor plumbing.

##### Op3. `dotAll` — **dropped per Resolved Decision #17**

- **Current:** Builder field 112; setter `dotAll()` 704 (no-arg flag). Context field 2150 (`boolean`). Ctor 2357. Consumed at path-matcher build (2003/2034).
- **Decision:** Delete the field, the setter, the context field, and the ctor copy. The path-matcher build sites (2003/2034) instead **infer** `dotAll` from the URL pattern itself — a path containing `**` or `/.*` implies `dotAll=true`. This makes the setting fully derived from the existing `path` attribute.
- **Migration row:** Already added to v9.5 Migration Guide (users who explicitly called `builder.dotAll(true)` delete the call; the URL pattern dictates behavior).

##### Op4. `defaultCharset` — **DONE (Phase 1)**

- **Status:** Builder field and `defaultCharset(Charset)` setter deleted on **both** `RestOpContext.Builder` and `RestContext.Builder`. `RestOpContext.findDefaultCharset()` walks `@RestOp`-group annotations (child-to-parent), applies SVL, then `Charset.forName`. If unset and `isInherited(PROPERTY_defaultCharset)`, walks `@Rest(defaultCharset)` on the resource-class hierarchy via `context.mergeReplacedStringAttribute(...)` (which honors `@Rest(noInherit={"defaultCharset"})` cutoff) with SVL applied. If still unset, falls back to the env default (`RestContext.defaultCharset`, UTF-8). The `RestContext.defaultCharset` field and ctor copy step are deleted (see R5).

##### Op5. `encoders` ✓ DONE

- **Current:** Builder field 114; accessor 714 (lazy via `createEncoders` 1701); setters 734/753. Context field 2157. Ctor 2317 `bs.add(EncoderSet.class, builder.getEncoders().orElse(context.getEncoders()))`. Getter 2532. `@RestOp(encoders)` / `@Rest(encoders)`.
- **Resolved:**
  - Q-Op5.1: Parent-fallback to `context.getEncoders()` preserved inside `findEncoders()` per Resolved Decision #19.
  - Q-Op5.2 ✓ Resolved Decisions #9 + #14 — sub-builder chaining gone; configure via `@RestOp(encoders={...})` or `@RestInject(name="encoders")`.
- **Done (Phase 2 / Op5, conservative):**
  - Removed `protected final EncoderSet encoders` field; added `encodersMemo` + `findEncoders()` returning `builder.getEncoders().orElse(context.getEncoders())`; getter routes through memoizer; ctor uses `bs.add(EncoderSet.class, getEncoders())`.
  - Build clean; full test suite passes.

##### Op6. `debug` (Enablement) — **DONE (Phase 1)**

- **Status:** Builder `debug(Enablement)` setter and field deleted. `RestOpContext.findDebugEnablement()` walks `@RestOp`-group annotations (child-to-parent) for the first non-blank `debug` attribute, applies SVL, and builds a fresh `DebugEnablement`. If unset, falls back to `context.getDebugEnablement()` when `noInherit` allows; otherwise an empty `DebugEnablement`. Inherited `Context.Builder.debug()` / `debug(boolean)` flags are unchanged (separate concern from the op-level `Enablement`).

##### Op7. `defaultRequestHeaders` ✓ DONE

- **Current:** Builder field 116; accessor 614 (lazy via `createDefaultRequestHeaders` 1623); setter 633. Also mutated in `processParameterAnnotations()` 2073 for `@Header` defaults. Context field 2158. Ctor 2337 `defaultRequestHeaders = builder.defaultRequestHeaders()`. `createDefaultRequestHeaders` seeds from `parent.defaultRequestHeaders().copy()`, then `BeanCreateMethodFinder<HeaderList>` named `"defaultRequestHeaders"`. Getter 2511.
- **Resolved:**
  - Q-Op7.1: `@Header` parameter annotations are scanned during `findDefaultRequestHeaders()` first-access — the Java method's parameter list is already accessible via the (final) `restMethod` field, so the scan happens inside the memoizer body. No mutation of the memoized result post-construction.
  - Q-Op7.2: Parent-context copy via `context.getDefaultRequestHeaders().copy()` happens inside `findDefaultRequestHeaders()` if inherited (per noInherit). Standard parent-fallback per Resolved Decision #19.
  - Q-Op7.3 (implicit): `defaultAccept` / `defaultContentType` from `@RestOp` (Resolved Decision #15) feed into the same memoizer as `Accept` / `Content-Type` header entries.
- **Done (Phase 2 / Op7, conservative):**
  - Removed `protected final HeaderList defaultRequestHeaders` field; added `defaultRequestHeadersMemo` + `findDefaultRequestHeaders()` returning `builder.defaultRequestHeaders()` — preserves all configuration paths (`@RestOp`/`@Rest(defaultRequestHeaders)` annotation apply, parent-context copy via `createDefaultRequestHeaders` seeding, `BeanCreateMethodFinder<HeaderList>` named `"defaultRequestHeaders"`, **plus** the `processParameterAnnotations()` mutations for `@Header` defaults that run during `Builder.init()` before ctor — the builder's `HeaderList` is fully populated by ctor time so memoizing `builder.defaultRequestHeaders()` retains all headers).
  - Updated `getDefaultRequestHeaders()` to return the memoizer; `properties()` debug accumulator updated to use the getter.
  - Build clean; full test suite passes.

##### Op8. `defaultResponseHeaders` ✓ DONE

- **Current:** Builder field 117; accessor 674; setter 693. Context field 2159. Ctor 2339. `createDefaultResponseHeaders` 1675 — same shape as Op7 without parameter-annotation injection.
- **Specific questions:** Q-Op8.1: Same parent-copy question as Op7.
- **Done (Phase 2 / Op8, conservative):**
  - Same shape as Op7 — `defaultResponseHeadersMemo` + `findDefaultResponseHeaders()` returning `builder.defaultResponseHeaders()`, getter through memoizer.
  - Build clean; full test suite passes.

##### Op9. `partParser` ✓ DONE

- **Current:** Builder field 118 (`HttpPartParser.Creator`); accessor 1093 (lazy via `createPartParser` 1912); setters 1112/1130. Applied via `apply(work)` 182–183. Context field 2160. Ctor 2321 `bs.add(HttpPartParser.class, builder.getPartParser().orElse(context.getPartParser()))`. Getter 2574.
- **Resolved:**
  - Q-Op9.1: Parent-fallback to `context.getPartParser()` preserved per Resolved Decision #19.
  - Q-Op9.2: `HttpPartParser.Creator` indirection stripped per R32 — `findPartParser()` reads from `@RestInject(name="partParser")` or instantiates `@RestOp(partParser=...)` directly.
- **Done (Phase 2 / Op9, conservative — Creator indirection NOT stripped):**
  - Removed `protected final HttpPartParser partParser` field; added `partParserMemo` + `findPartParser()` returning `builder.getPartParser().orElse(context.getPartParser())` — preserves the `Optional<HttpPartParser>` parent-fallback shape that the builder already exposes (which folds in the Creator indirection).
  - Updated `getPartParser()` to return the memoizer; ctor uses `bs.add(HttpPartParser.class, getPartParser())`.
  - Build clean; full test suite passes.

##### Op10. `partSerializer` ✓ DONE

- **Current:** Builder field 119; accessor 1140; setters 1159/1177. Applied via `apply(work)` 180–181. Context field 2161. Ctor 2322. Getter 2581.
- **Specific questions:** Q-Op10.1: Same as Op9.
- **Done (Phase 2 / Op10, conservative — Creator indirection NOT stripped):**
  - Same shape as Op9 — `partSerializerMemo` + `findPartSerializer()` returning `builder.getPartSerializer().orElse(context.getPartSerializer())`. Internal `createPartSerializer(...)` call in `getPartSerializer(HttpPartSchema)` updated from `partSerializer` field reference to `getPartSerializer()`.
  - Build clean; full test suite passes.

##### Op11. `jsonSchemaGenerator` ✓ DONE

- **Current:** Builder field 120; accessor 881 (lazy via `createJsonSchemaGenerator` 1780); setters 900/918. Applied via `apply(work)` 184–185. Context field 2162. Ctor 2319. Getter 2553.
- **Specific questions:** Q-Op11.1: Parent-fallback to `RestContext.getJsonSchemaGenerator()`.
- **Done (Phase 2 / Op11, conservative):**
  - Removed `protected final JsonSchemaGenerator jsonSchemaGenerator` field; added `jsonSchemaGeneratorMemo` + `findJsonSchemaGenerator()` returning `builder.getJsonSchemaGenerator().orElse(context.getJsonSchemaGenerator())`; getter through memoizer.
  - Build clean; full test suite passes.

##### Op12. `consumes` → `supportedContentTypes` ✓ DONE (Phase 1)

- **Current:** Builder field 121; setter 403. Context field `List<MediaType> supportedContentTypes` 2164. Ctor 2333 `u(nn(builder.consumes) ? builder.consumes : parsers.getSupportedMediaTypes())`. Getter `getSupportedContentTypes()` 2666. `@RestOp(consumes)` / `@Rest(consumes)` / `@RestPut` / `@RestPost`.
- **Status:** `findSupportedContentTypes()` reads directly from `@RestOp`-group + class-level `@Rest` annotations + SVL; `Builder.consumes(MediaType...)` setter and field deleted. See FINISHED-16b Phase 1 narrative.

##### Op13. `produces` → `supportedAcceptTypes` ✓ DONE (Phase 1)

- **Current:** Builder field 122; setter 1234. Context field 2163. Ctor 2332 (derives from `serializers`). Getter 2659. `@RestOp(produces)` etc.
- **Status:** `findSupportedAcceptTypes()` reads directly from `@RestOp`-group + class-level `@Rest` annotations + SVL; `Builder.produces(MediaType...)` setter and field deleted. See FINISHED-16b Phase 1 narrative.

##### Op14. `path` → `pathMatchers[]` *(Composite-bean-lookup)* ✓ DONE

- **Current:** Builder field 123 (`List<String>`); setter 1201. Context field `UrlPathMatcher[] pathMatchers` 2184. Ctor 2329 `pathMatchers = bs.add(UrlPathMatcher[].class, builder.getPathMatchers().asArray())`. `Builder.getPathMatchers()` 1997–2049 consumes `path`, `dotAll`, `restMethod`, plus `@RestGet/@RestPut/@RestPost/@RestDelete/@RestOp` annotations; uses `BeanCreateMethodFinder<UrlPathMatcherList>`.
- **Specific questions:**
  - Q-Op14.1: `getPathPattern()` (2588) returns `pathMatchers[0].toString()`. Preserve via `findPathMatchers()` memoizer.
  - Q-Op14.2: Annotation-scan logic at 2003–2049 is large — break it into `findPathMatchers()` cleanly.
  - Q-Op14.3: Depends on Op3 (`dotAll`) and Op16 (`restMethod`).
- **Done (Phase 2 / Op14, conservative):**
  - Removed `protected final UrlPathMatcher[] pathMatchers` field; added `pathMatchersMemo` + `findPathMatchers()` returning `builder.getPathMatchers().asArray()` — preserves the existing `Builder.getPathMatchers()` annotation-scan logic in place (Q-Op14.2 deferred; the Builder-side scan still owns the `path`/`dotAll`/`restMethod`/`@RestGet|Put|Post|Delete|Op` resolution).
  - Added new public getter `public UrlPathMatcher[] getPathMatchers()` returning the memoizer.
  - Updated `getPathPattern()` to use `getPathMatchers()[0].toString()`; updated `compareTo()` to use the getter on both `this` and `o`; updated `matchPattern()` to iterate `getPathMatchers()`.
  - Updated ctor wiring: `var pm = getPathMatchers(); bs.add(UrlPathMatcher[].class, pm); bs.addBean(UrlPathMatcher.class, pm.length > 0 ? pm[0] : null);` (was `pathMatchers = bs.add(...); bs.addBean(UrlPathMatcher.class, pathMatchers.length > 0 ? pathMatchers[0] : null);`).
  - Build clean; full test suite passes.

##### Op15. `maxInput` — **DONE (Phase 1)**

- **Status:** Builder field and `maxInput(String)` setter deleted on **both** `RestOpContext.Builder` and `RestContext.Builder`. `RestOpContext.findMaxInput()` walks `@RestOp`-group annotations (child-to-parent), applies SVL, and parses with `StringUtils.parseLongWithSuffix`. If unset and `isInherited(PROPERTY_maxInput)`, walks `@Rest(maxInput)` on the resource-class hierarchy via `context.mergeReplacedStringAttribute(...)` (which honors `@Rest(noInherit={"maxInput"})` cutoff) with SVL applied. If still unset, falls back to the env default (`RestContext.maxInput`, 100 MB). The `RestContext.maxInput` field and ctor copy step are deleted (see R7).

##### Op16. `restMethod` *(Bootstrap-only)*

- **Current:** Builder field 125 (set in ctor 145, no setter). Context field `Method method` 2167 + `MethodInfo mi` 2168. Ctor 2298/2305. Getter `getJavaMethod()` 2546.
- **Migration note:** Immutable identity — constructor arg.

##### Op17. `defaultRequestAttributes` ✓ DONE

- **Current:** Builder field 126; accessor 554 (lazy via `createDefaultRequestAttributes` 1571); setter 573. Context field 2169. Ctor 2335. Getter 2497. `@RestOp(defaultRequestAttributes)` / `@Rest(defaultRequestAttributes)`; `@RestInject(name="defaultRequestAttributes")`.
- **Specific questions:** Q-Op17.1: Parent-copy + named `BeanCreateMethodFinder` hook — preserve.
- **Done (Phase 2 / Op17, conservative):** Removed field; `defaultRequestAttributesMemo` + `findDefaultRequestAttributes()` returning `builder.defaultRequestAttributes()`; getter through memoizer. Builder-side `createDefaultRequestAttributes` (parent-copy + `BeanCreateMethodFinder` hook) preserved. Build clean; full test suite passes.

##### Op18. `parsers` ✓ DONE

- **Current:** Builder field 127; accessor 1043 (lazy via `createParsers` 1885); setters 1063/1082. Applied via `apply(work)` 178–179. Context field 2172. Ctor 2320 `bs.add(ParserSet.class, builder.getParsers().orElse(context.getParsers()))`. Getter 2567. `@RestOp(parsers)` / `@Rest(parsers)`.
- **Specific questions:** Q-Op18.1: Parent-fallback; same sub-builder chaining question as R30.
- **Done (Phase 2 / Op18, conservative):** Removed field; `parsersMemo` + `findParsers()` returning `builder.getParsers().orElse(context.getParsers())`; getter through memoizer; `findSupportedContentTypes()` field-access updated from `parsers.getSupportedMediaTypes()` to `getParsers().getSupportedMediaTypes()`. Build clean; full test suite passes.

##### Op19. `defaultRequestFormData` ✓ DONE

- **Current:** Builder field 128 (`PartList`); accessor 584 (lazy via `createDefaultRequestFormData` 1597); setter 603. Also mutated in `processParameterAnnotations()` 2087 via `@FormData` defaults. Context field 2170. Ctor 2336. Getter 2504. `@RestInject(name="defaultRequestFormData")`.
- **Specific questions:**
  - Q-Op19.1: `@FormData` parameter-annotation injection — same concern as Op7.
  - Q-Op19.2: No parent-context fallback (unlike headers) — seeds from `PartList.create()`. Confirm.
- **Done (Phase 2 / Op19, conservative):** Removed field; `defaultRequestFormDataMemo` + `findDefaultRequestFormData()` returning `builder.defaultRequestFormData()`. The `@FormData` parameter-annotation defaults applied during `Builder.init()` are already in the builder's `PartList` by ctor time. Build clean; full test suite passes.

##### Op20. `defaultRequestQueryData` ✓ DONE

- **Current:** Builder field 129; accessor 644; setter 663. Mutated in `processParameterAnnotations()` 2080 via `@Query` defaults. Context field 2171. Ctor 2338. Getter 2518.
- **Specific questions:** Q-Op20.1: Same pair as Op19.
- **Done (Phase 2 / Op20, conservative):** Same shape as Op19 — `defaultRequestQueryDataMemo` + `findDefaultRequestQueryData()` returning `builder.defaultRequestQueryData()`. Build clean; full test suite passes.

##### Op21. `restContext` *(Bootstrap-only)*

- **Current:** Builder field 130 (ctor arg 143). Context field `context` 2173. Ctor 2297. **No public getter.**
- **Migration note:** Constructor arg. Cannot be memoized — it *is* the parent context.

##### Op22. `parent` (RestContext.Builder) *(Bootstrap-only)*

- **Current:** Builder field 131 (ctor arg 144). Not stored on `RestOpContext`.
- **Migration note:** Build-time only. Delete from any post-refactor public API.

##### Op23. `converters` ✓ DONE

- **Current:** Builder field 132 (`RestConverterList.Builder`); accessor 414 (lazy via `createConverters` 1543); setters 434/453. Context field `RestConverter[]` 2174. Ctor 2316. Package-private getter 2743. `@RestOp(converters)` / `@Rest(converters)`.
- **Specific questions:**
  - Q-Op23.1: Three resolution paths in `createConverters` — `defaultClasses()`, direct `beanStore.getBean(RestConverterList.class)`, and `BeanCreateMethodFinder`. Preserve all three inside `findConverters()`.
- **Done (Phase 2 / Op23, conservative):** Removed `protected final RestConverter[] converters` field; added `convertersMemo` + `findConverters()` returning `builder.converters().build().asArray()` — preserves all three Builder-side resolution paths in `createConverters` (defaults + direct bean + `BeanCreateMethodFinder`). Package-private `getConverters()` returns memoizer; ctor uses `bs.add(RestConverter[].class, getConverters())`. Build clean; full test suite passes.

##### Op24. `guards` (+ `roleGuard` + `rolesDeclared`) ✓ DONE

- **Current:** `guards` builder field 133; accessor 764; setters 784/803. `roleGuard` field 136, setter 1299. `rolesDeclared` field 137, setter 1335. Context field `RestGuard[]` 2175. Ctor 2318 `bs.add(RestGuard[].class, builder.getGuards().asArray())`. `Builder.getGuards()` 2100–2113 merges `guards()` with `RoleBasedRestGuard` entries derived from `roleGuard`/`rolesDeclared`. `createGuards` 1747 uses defaults + direct bean lookup + `BeanCreateMethodFinder`. Package-private getter 2745.
- **Specific questions:**
  - Q-Op24.1: Merge logic (`roleGuard` + `rolesDeclared` → guards) needs to land inside `findGuards()`. Confirm it's correct to fold the two input fields into a single memoizer's computation.
  - Q-Op24.2: Three resolution paths, same as Op23.
- **Done (Phase 2 / Op24, conservative):** Removed `protected final RestGuard[] guards` field; added `guardsMemo` + `findGuards()` returning `builder.getGuards().asArray()` — the merge logic for `roleGuard`/`rolesDeclared` already lives in `Builder.getGuards()` (Q-Op24.1 satisfied — folded into the builder-side merge, memoized at the array level). Package-private `getGuards()` returns memoizer; `compareTo` updated to use `getGuards()` on both `this` and `o`; ctor uses `bs.add(RestGuard[].class, getGuards())`. Build clean; full test suite passes.

##### Op25. `matchers` (+ `clientVersion`) ✓ DONE

- **Current:** `matchers` builder field 134; accessor 928; setters 948/967. `clientVersion` field 138, setter 373. Context fields `RestMatcher[] optionalMatchers` 2176 and `requiredMatchers` 2177. Ctor 2325–2327 via `builder.getMatchers(context)` 2117–2123 (appends `ClientVersionMatcher` if `clientVersion` non-null). `createMatchers` 1852 — same three-path resolution. Internal-only consumption via `match()` 2700.
- **Specific questions:** Q-Op25.1: Same merge question as Op24 (`clientVersion` + `matchers`). Also: optional-vs-required split inside `findMatchers()` — return a struct or two separate memoizers?
- **Done (Phase 2 / Op25, conservative):**
  - Removed `protected final RestMatcher[] optionalMatchers` and `requiredMatchers` fields.
  - Resolved Q-Op25.1's split question by **chaining memoizers**: one shared `matchersListMemo: Memoizer<RestMatcherList> = memoizer(this::findMatchersList)` returning `builder.getMatchers(context)` (single computation, ensures `clientVersion` merging happens once), then two derived memoizers (`optionalMatchersMemo`, `requiredMatchersMemo`) whose `findX()` methods read `matchersListMemo.get().getOptionalEntries()` / `getRequiredEntries()`. Both arrays are stable across calls.
  - Added new public getters `public RestMatcher[] getOptionalMatchers()` / `public RestMatcher[] getRequiredMatchers()` returning the memoizers.
  - Updated `match()` to grab locals `var rm = getRequiredMatchers(); var om = getOptionalMatchers();` and iterate locals; updated `compareTo()` to call the getters on both `this` and `o`.
  - Build clean; full test suite passes.

##### Op26. `serializers` ✓ DONE

- **Current:** Builder field 135; accessor 1346 (lazy via `createSerializers` 1966); setters 1366/1385. Applied via `apply(work)` 176–177. Context field 2182. Ctor 2323. Getter 2652. `@RestOp(serializers)` / `@Rest(serializers)`.
- **Specific questions:** Q-Op26.1: Same as Op18 (parent-fallback + sub-builder chaining).
- **Done (Phase 2 / Op26, conservative):** Removed field; `serializersMemo` + `findSerializers()` returning `builder.getSerializers().orElse(context.getSerializers())`; getter through memoizer; `findSupportedAcceptTypes()` field-access updated from `serializers.getSupportedMediaTypes()` to `getSerializers().getSupportedMediaTypes()`. Build clean; full test suite passes.

##### Op27. `httpMethod` ✓ DONE (Phase 1)

- **Current:** Builder field 139; setter 865. Context field 2183. Ctor 2349–2354 (uses `builder.httpMethod`, else `HttpUtils.detectHttpMethod(method, true, "GET")`; `"METHOD"` → `"*"`; upper-case). Getter 2539. Annotation-derived from `@RestGet` / `@RestPut` / etc. and `@RestOp(method)`.
- **Status:** `findHttpMethod()` reads directly from `@RestOp`-group annotations: verb annotations imply their fixed verb; `@RestOp(method)`/`value()` are SVL-resolved; falls back to `HttpUtils.detectHttpMethod(method, true, "GET")`. `Builder.httpMethod(String)` and the field are deleted. See FINISHED-16b Phase 1 narrative.

---

#### Derived `RestOpContext` fields (no builder setter)

These have no configuration surface and aren't part of this TODO, but listed for completeness:

- `hierarchyDepth` 2151 (ctor walks class hierarchy) — leave alone.
- `callLogger` 2154 = `context.getCallLogger()` (ctor 2366) — **memoized** in the Op-batch refactor (`callLoggerMemo` + `findCallLogger()` returning `context.getCallLogger()`); the field was removed and a new public `getCallLogger()` getter was added on `RestOpContext`. The old eager `this.callLogger = context.getCallLogger();` ctor line was removed (the only consumer, `start()`, now calls `getCallLogger()`).
- `responseBeanMetas` 2165, `headerPartMetas` 2166 — runtime caches; leave alone.
- `mi` 2168, `methodInvoker` 2178, `postCallMethods` 2179, `preCallMethods` 2180, `responseMeta` 2181 — derived from `method` / annotations; leave alone.
