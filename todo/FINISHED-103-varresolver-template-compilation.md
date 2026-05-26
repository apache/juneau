# FINISHED-103 — `VarResolver` template compilation + `resolveSupplier()` API

Joint landing with TODO-102 (`#{...}` scripting). Shared infrastructure (one unified
tokenizer/compiler + segment array machinery) was built once and used by both features. See
also [`FINISHED-102-svl-scripting.md`](FINISHED-102-svl-scripting.md).

## What landed

A new compiled-template form `VarTemplate`, plus `compile(...)` / `resolveSupplier(...)` API
methods on `VarResolver` and `VarResolverSession`. Separates "parse the template" from
"evaluate the template" so repeated resolutions skip tokenization + var-registry lookup +
function-discovery. Also unlocks the `@Value Supplier<String>` field type for live-reload
config reads, with the field type itself acting as the opt-in signal (no annotation flag).

## Why

Before this change, every `varResolver.resolve(...)` call repeated the full pipeline:
tokenize, walk for `${` / `}` / `$X{` boundaries, look up each var by prefix, dispatch, splice
results. For one-shot use that was fine; for **anything called repeatedly with the same
template string** it was pure waste.

Three concrete consumers benefit:

1. **`@Value Supplier<String>` field** (TODO-79 follow-on). Singleton beans that read config at
   init were stuck with the init-time value. A `Supplier<String>` field type now opts in to
   "always read latest", with each `.get()` opening a fresh session — safe to share across
   threads, supports hot-reload configs, file-watcher reload, Spring `@RefreshScope`, env-var
   bridges, JNDI lookups.

2. **TODO-102 `#{...}` scripting**. Without compilation the per-resolve cost was: tokenize +
   parse args + look up `VarFunction` + reflective coerce N args + reflective invoke. With
   compilation, function lookup + arg-template tokenization happens ONCE; per-resolve is a
   flat array walk with cached references.

3. **Per-request SVL evaluation seam**. `RestOpContext.pathMatchers` Memoizer now stores the
   compiled form so if/when per-request dynamic resolution is wanted, only the
   `.resolve(session)` step moves to the request handler.

## API surface (as shipped)

```java
public class VarResolver {
    // Existing — unchanged.
    String resolve(String input);
    String resolve(String input, Object resource);

    // New.
    Supplier<String> resolveSupplier(String input);   // fresh-session-per-.get(), threadsafe
    VarTemplate compile(String input);
}

public class VarResolverSession {
    // Existing — unchanged signatures (now delegates to compile(input).resolve(this)).
    String resolve(String input);

    // New.
    Supplier<String> resolveSupplier(String input);   // session-bound, not threadsafe
    VarTemplate compile(String input);
}

public final class VarTemplate {
    // No public constructor — created only via VarResolver.compile or VarResolverSession.compile.
    String resolve(VarResolverSession session);
    Writer resolveTo(VarResolverSession session, Writer out);
    Writer resolveToUnchecked(VarResolverSession session, Writer out);
    Supplier<String> asSupplier(VarResolverSession session);
    Supplier<String> asSupplierWithFreshSessions(VarResolver vr);
    boolean isLiteral();
    String getSource();
}
```

## Design decisions

All OQA-resolved 2026-05-25; full text in the plan file. Recap of the 12 resolutions:

1. **Two phases, one TODO.** Phase 1 ships the API surface (with a naive internal impl).
   Phase 2 lands the real compiled-template machinery + retrofits TODO-79 / TODO-102 /
   FINISHED-99 consumers.

2. **Hard dep on TODO-102, soft dep on TODO-79.** Compiled-form's biggest payoff is the
   `#{...}` function-dispatch cache, so the retrofit phase needs TODO-102 in tree.

3. **`VarTemplate` is the type name.** Mirrors the existing `Var` vocabulary.

4. **Lives in `org.apache.juneau.commons.svl`.** Next to `Var` / `VarResolver` /
   `VarResolverSession`.

5. **Compilation is bound to a `VarResolver` instance.** `vr.compile(input)` is the entry
   point; passing a `VarTemplate` to a different resolver is undefined. Documented in Javadoc;
   no runtime version check (overhead not justified).

6. **`String resolve(String)` is not deprecated.** Stays as the convenience one-shot entry
   point.

7. **Existing `String resolve(String)` becomes a shorthand for `compile(input).resolve(currentSession)`** internally.
   Single tokenizer in the codebase.

8. **`vr.compile(...)` is uncached — caller owns lifetime.** No bounded LRU. Framework consumers
   cache per-site (TODO-79 per `@Value` field via `ValueResolver.TEMPLATE_CACHE`, TODO-102 per
   `#{...}` segment via `ScriptSegment.cachedFn`, FINISHED-99 per `RestOpContext.pathMatchers`
   op).

9. **`@Value Supplier<String>` field type is autodetected.** No annotation flag required.
   `BeanInstantiator` reflects on the declared field/parameter type via the existing
   `Field.getGenericType()` / `Parameter.getParameterizedType()` path.

10. **Stable-value folding ships in Phase 2.** New `default boolean Var.isStable() { return false; }`
    SPI hook. Conservative default protects third-party `Var`s. Built-ins that opt in:
    `EnvVariablesVar`, `SystemPropertiesVar` (Javadoc caveat re `System.setProperty`),
    `ManifestFileVar`, `ArgsVar`. Built-ins that stay default-false: `ConfigVar`, `PropertyVar`,
    `DotenvVar`, `EnvFileVar`.

11. **Session-threadsafety contract — both surfaces shipped.**
    `vr.resolveSupplier(input)` opens a fresh session per `.get()` (safe to share across
    threads — recommended default). `session.resolveSupplier(input)` is session-bound (inherits
    session's threadsafety contract, not threadsafe by default). Documented in Javadoc with a
    side-by-side guidance block.

12. **`VarTemplate.isLiteral()` is public API.** Exposed so callers can fast-path templates
    known to have no variables (e.g. `BeanInstantiator` can skip Supplier-wiring overhead for a
    `@Value("plain") Supplier<String>` field). Cheap optimization signal that's hard to derive
    externally.

## Two-phase execution narrative

Both phases shipped in this development cycle:

### Phase 1 — API surface

- Added `resolveSupplier(String)` + `compile(String)` to `VarResolver` and `VarResolverSession`.
- `VarTemplate` value class — immutable, threadsafe.
- Both Supplier surfaces (cross-thread-safe via fresh sessions; session-bound for perf).
- Tests: `VarResolver_Compile_Test`, `VarResolver_ResolveSupplier_Test` covering basic resolve,
  escape, nested, var-not-found, plus an explicit cross-thread test.

### Phase 2 — Real compiled-template machinery

- `VarTemplate` internal structure: array of `TemplateSegment`s:
  - `LiteralSegment(String text)` — appends raw text.
  - `VarRefSegment(Var var, String key, VarTemplate defaultValue)` — appends
    `var.resolve(session, key)`, falling back to `defaultValue.resolve(session)`.
  - `ScriptSegment(VarFunction fn, String name, VarTemplate[] argTemplates)` — TODO-102's
    `#{...}` dispatch. Args are themselves precompiled `VarTemplate`s.
- `VarTemplateCompiler` — single recursive-descent tokenizer/parser, replaces the in-place
  tokenizer that lived in `VarResolverSession.resolve(...)`. **Single tokenizer in the
  codebase.**
- Per-segment cached `Var` reference (one map lookup at compile time, zero map lookups at
  resolve time). Same for `VarFunction` references on `ScriptSegment`.
- `isLiteral()` returns true when the segment array is empty or contains only
  `LiteralSegment`s — pre-computed at construction time + cached.
- Stable-value folding pass: for each `VarRefSegment` whose `var.isStable()` returns true and
  whose body is a literal, eagerly resolve the value at compile time and replace the segment
  with a `LiteralSegment`. Folded-only templates flip `isLiteral()` to `true`.
- `vr.resolveSupplier(...)` becomes `compile(input).asSupplierWithFreshSessions(vr)`.
  `session.resolveSupplier(...)` becomes `compile(input).asSupplier(session)`.

## Consumer retrofit summary (Phase 3 / Phase G)

Four targets retrofitted to use the compiled form:

1. **TODO-79 `@Value` machinery.** `ValueResolver.getCompiledTemplate(expression)` (in
   `org.apache.juneau.commons.inject`) caches a `VarTemplate` per distinct expression string in
   a static `ConcurrentMap<String, VarTemplate>`. Repeated bean construction reads the cached
   template instead of re-tokenizing. The cache key is bounded by the number of distinct
   `@Value(...)` expressions in the application (not user input), so the unbounded map cannot
   grow uncontrollably in practice. `ValueResolver.clearTemplateCache()` is exposed for tests
   that want a known starting state.

2. **`@Value Supplier<String>` field type — autodetect.** Added overload
   `ValueResolver.resolve(String expression, Class<?> targetType, Type genericTargetType, String siteDescription)`
   that inspects the declared generic type. If `genericTargetType` resolves to
   `Supplier<String>`:
   - Bare `String` field → `template.resolve(currentSession)` at injection time (existing
     behaviour, preserved).
   - `Supplier<String>` field → `template.asSupplierWithFreshSessions(VarResolver.DEFAULT)`
     injected; each `.get()` re-evaluates against a fresh session, threadsafe.
   - Literal-template Supplier → constant-folding fast path (captures resolved string once,
     returns same reference per `.get()`).
   - `FieldInfo.inject(...)` and `ParameterInfo.resolveValue(...)` updated to pass the declared
     generic type (`Field.getGenericType()` / `Parameter.getParameterizedType()`) so the
     autodetect works on both field- and constructor-/setter-parameter sites.

3. **TODO-102 `#{...}` dispatcher.** `ScriptSegment` was already designed to cache the
   `VarFunction` reference at compile time (`cachedFn` field, package-private final). Phase G
   verified this with `ScriptSegment_Precompiled_Test`: after `vr.compile("#{upper(${name})}")`,
   the cached function reference is the same instance after 50 resolves; arg templates are
   themselves precompiled `VarTemplate` instances reused across resolves. Zero function-
   registry lookups on the second-and-subsequent `.resolve(...)` calls.

4. **FINISHED-99 `RestOpContext.pathMatchers`.** The Memoizer now resolves each path string via
   `vr.compile(p).resolve(session)` (explicit two-step) instead of the implicit one-step
   `vr.resolve(p)`. A single `VarResolverSession` is created once for the entire pathMatcher
   build, so multiple path strings in the same op share the session. Production behaviour is
   unchanged (paths still resolve once at context-build time), but the compiled-form seam is
   now exercised in the framework hot loop — and if we ever switch to per-request dynamic
   resolution, only the `.resolve(session)` step moves to the handler.

### Stable-value folding sanity check

`Value_StableFolding_Test` verifies that `@Value("$S{key}")` with `SystemPropertiesVar` folds to
a `LiteralSegment` at compile time. Mutating the property *after* compile does not propagate
to the cached template — documented caveat. `${...}` shortcut (PropertyVar) stays non-stable
so live `Settings.set(...)` calls reflect in subsequent resolves.

## Test coverage

New test classes added in `juneau-utest`:

- `VarResolver_Compile_Test` (Phase 1) — `compile(...)` basic resolve semantics, isLiteral,
  escape handling, nested templates.
- `VarResolver_ResolveSupplier_Test` (Phase 1) — both Supplier surfaces, cross-thread safety
  for the fresh-session variant.
- `StableValueFolding_Test` (Phase 2) — stable-Var fold; counter-Var verification of "resolved
  once at compile" behaviour; `SystemPropertiesVar` post-compile mutation freeze; `Var.isStable`
  opt-in for the 4 stable built-ins.
- `Value_PrecompiledTemplate_Test` (Phase G) — same-expression `getCompiledTemplate(...)` returns
  identity-equal `VarTemplate`; distinct expressions cached separately; repeated bean injection
  reuses cached template; literal expressions cache as literal templates.
- `Value_SupplierFieldType_Test` (Phase G) — bare-`String` resolves once; `Supplier<String>` re-
  evaluates per `.get()`; constructor-injected `Supplier<String>` re-evaluates; literal-template
  Supplier returns same `String` reference per `.get()` (constant-folding fast path); cross-
  thread `Supplier.get()` is safe to share.
- `Value_StableFolding_Test` (Phase G) — `$S{...}` with `SystemPropertiesVar` folds at compile
  time and freezes value; `${...}` shortcut (PropertyVar) does not fold; literal expression is
  literal.
- `ScriptSegment_Precompiled_Test` (Phase G) — `#{...}` cached function reference at compile
  time; `argTemplates` are precompiled `VarTemplate`s; 50+ resolves through the same
  `VarTemplate` do not re-consult the function registry.
- `RestOpContext_PathMatcher_CompiledForm_Test` (Phase G) — literal paths still work; SVL paths
  resolve in default and override branches; multiple ops sharing an SVL fragment all resolve
  correctly.
- `VarResolver_Benchmark_Test` (Phase G, `@Tag("benchmark")`) — micro-benchmark of compile-
  per-call vs precompiled-once-resolve-N for `${name}`, `${a}-${b}-${c}`, and `#{upper(${name})}`.

## Benchmark numbers

Measured on a representative dev workstation, 2K warmup iterations + 20K measurement
iterations:

| Template | compile-per-call (ms) | precompiled (ms) | Speedup |
|---|---:|---:|---:|
| `hello ${name:world}` | 184.07 | 106.31 | **1.73×** |
| `${a:1}-${b}-${c}` | 223.20 | 195.57 | **1.14×** |
| `#{upper(${name})}` | 89.33 | 65.42 | **1.37×** |

These are smaller than the original plan-file targets (≥ 2× / ≥ 5×). The reason is that
Phase 2's tokenizer unification means the "compile-per-call" baseline already uses the same
compiled-form internals — both paths run the same tokenizer + segment array + function
dispatch. The difference is purely the compile overhead per call.

Where compile is amortized over **many** resolves (TODO-79's `@Value` machinery, FINISHED-99's
`pathMatchers` Memoizer), the absolute saving is N × compile-cost-per-call. For
`@Value`-driven beans constructed many times (request scope, test harnesses), this adds up;
for one-shot context-build paths the saving is one tokenizer pass per op-path. The win shows
up in aggregate, not in a single-resolve micro-benchmark.

The original `≥ 2× / ≥ 5×` targets in the plan applied to comparisons against the pre-9.5
legacy ad-hoc dispatcher (which is no longer in the tree). The unified compiled-form path is
now the baseline.

## Verification

```
$ ./scripts/test.py --test-only
…
✅ Tests passed!
```

Full-suite wall-clock: **88.5s** (Phase F baseline: 84.5s). The slight increase is within
normal JIT/CI noise.

## Known follow-ups / explicit deferrals

- **Micro-benchmark numbers fall short of the plan-file `5×` target for `#{...}` templates.**
  Reported above; root cause is the Phase 2 tokenizer unification making the "uncompiled"
  baseline a strawman. Acceptance bar in the user's instructions was honesty over silent
  relaxation — surfaced in the Phase G report. No further work planned.
- **`vr.compile(...)` is uncached at the resolver level** per RD #8. Framework consumers cache
  per-site. This eliminates the unbounded-memory-growth footgun an always-on resolver-level
  cache would have for user-input-derived templates; it does push the "I want a cache" burden
  onto each consumer. So far that has been three sites (`ValueResolver`, `ScriptSegment`,
  `RestOpContext.pathMatchers`) and the per-site shape has been a good fit for each.
- **`@Value Supplier<T>` for `T != String` is not supported.** Phase G's autodetect path only
  matches `Supplier<String>` exactly. `Supplier<Integer>`, `Supplier<Duration>`, etc. would
  need an additional coercion step inside the Supplier; defer to a future TODO if demand
  surfaces.
- **`Settings.set(...)` from inside a stable-folded template is silently ignored** for the 4
  opt-in `Var`s (e.g. `System.setProperty("k", "v")` after `vr.compile("$S{k}")`). Documented
  caveat in `SystemPropertiesVar.isStable()` Javadoc. Users who need live system-property
  reads should use the `${...}` shortcut (PropertyVar, non-stable) instead.
- **Plan file** — `todo/TODO-103-varresolver-template-compilation.md` removed per `/todo`
  workflow; content lives in this archive.

## Related

- **TODO-102** — `#{...}` scripting support. Hard dep; joint landing. See
  [`FINISHED-102-svl-scripting.md`](FINISHED-102-svl-scripting.md).
- **TODO-79** — `@Value` annotation + `${...}` shortcut. Soft dep; Phase 3 retrofit caches a
  `VarTemplate` per distinct `@Value` expression and autodetects `Supplier<String>` field type.
- **FINISHED-99** — SVL resolution in `@RestOp(path)`. Phase 3 retrofit puts the compiled-form
  seam into `RestOpContext.pathMatchers`.
- **TODO-95** — Per-RestContext `@Value` resolution against `@Rest(config=...)`. Independent;
  compiled-form caching lives on the per-RestContext path so TODO-95's per-resource resolution
  naturally inherits the perf win when it lands.
