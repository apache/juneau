# TODO-103 — `VarResolver` template compilation + `resolveSupplier()` API

Introduce a compiled template form (`VarTemplate`) and a re-evaluating-supplier API (`resolveSupplier()`) to `VarResolver` / `VarResolverSession`. Separates "parse the template" from "evaluate the template" so repeated resolutions skip tokenization + per-call var-registry lookup + per-call function discovery; unlocks the `@Value Supplier<String>` field pattern; gives TODO-102's `#{...}` scripting layer a place to cache function dispatch + arg-coercion metadata.

## Why

Today every `varResolver.resolve(...)` call repeats the full pipeline: tokenize the input string, walk it looking for `${` / `}` / `$X{` boundaries, look up each var by prefix in the var registry, dispatch, splice the result back into the surrounding template. For one-shot use that's fine; for **anything called repeatedly with the same template string**, it's pure waste. Three concrete consumers:

- **`@Value Supplier<String>` field** (TODO-79 follow-on) — singleton beans that read config at init are stuck with the init-time value. A `Supplier<String>` field type makes "always read latest" explicit, which is the immediate use case for hot-reload configs (file-watcher reload of `.cfg`, Spring `@RefreshScope`, env-var bridges, JNDI lookups). Without a `Supplier` form, users hand-roll closures.

- **TODO-102 `#{...}` scripting** — the per-resolve cost without compilation is: tokenize + parse args + look up `Function` impl + reflective coerce N args + reflective invoke. Compiled form resolves function lookup + arg coercion ONCE; per-resolve becomes a flat array walk with cached references. This is the single biggest perf payoff the compiled form unlocks.

- **Per-request SVL evaluation** — if/when we ever want dynamic `@RestOp(path=...)` resolution per-request (instead of the one-shot resolve at context-build time that FINISHED-99 added), the compiled form makes that affordable.

## Resolved design decisions

1. **Two phases, one TODO.** Phase 1 ships the API surface (with a naive internal impl). Phase 2 lands the real compiled-template machinery + retrofits the TODO-79 / TODO-102 / FINISHED-99 consumers. Splitting into two PRs is fine; splitting into two TODOs risks the Supplier API landing as a permanent stub.

2. **Hard dep on TODO-102, soft dep on TODO-79.** Compiled-form's biggest payoff is the `#{...}` function-dispatch cache, so the retrofit phase needs TODO-102 in tree. The `@Value Supplier<String>` field pattern is independently useful but additive on TODO-79's `BeanInstantiator` wiring.

3. **`VarTemplate` is the type name.** `Template` is too generic; `CompiledTemplate` is verbose; `VarTemplate` mirrors the existing `Var` vocabulary.

4. **Lives in `org.apache.juneau.commons.svl`** next to `Var` / `VarResolver` / `VarResolverSession`.

5. **Compilation is bound to a `VarResolver` instance.** `vr.compile(input)` is the only entry point; passing a `VarTemplate` to a different `VarResolver` is undefined. Rebuilding the `VarResolver` invalidates any compiled templates from the old instance — caller's responsibility to re-compile. Documented in Javadoc; no runtime check (would require either a back-reference or a version counter, both add overhead).

6. **`String resolve(String)` is not deprecated.** Stays as the convenience one-shot entry point. The compile path is for callers that resolve the same template repeatedly.

7. **Existing `String resolve(String)` becomes a shorthand for `compile(input).resolve(currentSession)`** internally — same code path, no separate tokenizer. Phase 2 task.

## Open questions (resolve before plan-file lock-in)

1. **Should `vr.compile(...)` cache its result internally?** Three options:
   - **A.** Uncached — caller owns lifetime. Bean caches its own `VarTemplate`. Simplest contract; no surprise memory growth.
   - **B.** Always-cached (bounded LRU on the `VarResolver`). Convenient for ad-hoc callers; risks unbounded growth if templates are user-input-derived.
   - **C.** Opt-in via builder knob `VarResolver.Builder.compileCache(maxSize)`. Default uncached; framework consumers (TODO-79's `@Value` machinery, TODO-102's `#{...}` dispatcher) wire themselves cached.

2. **`@Value Supplier<String>` field handling.** Should TODO-79's `BeanInstantiator` auto-detect `Supplier<String>` field types and call `resolveSupplier(...)` instead of `resolve(...)`? Or require a marker like `@Value(supplier=true)` to opt in explicitly?

3. **Stable-value folding optimization.** Some `Var` implementations resolve to constants — e.g. `EnvVariablesVar.resolve("HOME")` is stable for the JVM lifetime. Should the compile phase ask each `Var` "is this key stable?" via a new optional `Var.isStable(String key) → boolean` method, and fold stable refs to literal segments at compile time? Worth doing on day 1, or defer as a follow-on optimization?

4. **`VarResolverSession` thread-safety.** Today's sessions are not threadsafe (per-call state). The `Supplier<String>` returned by `vr.resolveSupplier(...)` must either (a) open a fresh session per `.get()` call (safe, slightly more overhead), (b) hold a long-lived session and require the caller to not share the Supplier across threads (matches today's session semantics, no overhead, footgun), or (c) require the user to pass a session factory. Recommend option (a) for the user-facing `vr.resolveSupplier(...)` form; offer option (b) via `session.resolveSupplier(...)` for the perf-sensitive case where the caller knows they own the session.

5. **Should `VarTemplate.isLiteral()` be exposed publicly** so callers can fast-path templates known to have no variables? Or kept internal as an implementation detail?

## API surface

```java
public class VarResolver {
    // Existing — unchanged signatures.
    String resolve(String input);
    String resolve(String input, Object resource);

    // New (Phase 1).
    Supplier<String> resolveSupplier(String input);
    VarTemplate compile(String input);
}

public class VarResolverSession {
    // Existing — unchanged signatures.
    String resolve(String input);

    // New (Phase 1).
    Supplier<String> resolveSupplier(String input);  // session-bound, not threadsafe
    VarTemplate compile(String input);
}

public final class VarTemplate {
    // No public constructor; created only via VarResolver.compile(...) or VarResolverSession.compile(...).
    String resolve(VarResolverSession session);
    Supplier<String> asSupplier(VarResolverSession session);
    Supplier<String> asSupplierWithFreshSessions(VarResolver vr);  // option (4a) for safe cross-thread Suppliers
    boolean isLiteral();  // optimization hint (per OQ #5)
}
```

`VarTemplate` is immutable + threadsafe (Phase 2 contract). Phase 1 may ship a naive impl that just stores the raw string and delegates to `vr.resolve(...)` — the public API is identical, so consumers can be retrofitted incrementally.

## Phases

### Phase 1 — API surface (naive impl)

1. Add `resolveSupplier(String)` + `compile(String)` to `VarResolver` + `VarResolverSession`.
2. `VarTemplate` value class — immutable, but Phase 1 impl holds the raw `String` + `VarResolver` reference and delegates `resolve(...)` straight back to the existing tokenize-and-resolve path. `isLiteral()` returns false (or does a cheap "contains `${` or `#{` or `$X{`" scan).
3. `resolveSupplier(String)` returns `() -> vr.resolve(input)` (option 4a for safe cross-thread use) or a session-bound closure (`session.resolveSupplier(...)`).
4. Javadoc on `VarTemplate` documents the binding contract (OQ #5) + threadsafety.
5. Tests: `VarResolver_ResolveSupplier_Test`, `VarResolver_Compile_Test` covering basic resolve, escape, nested, var-not-found, threadsafety of the cross-thread Supplier form.

**Acceptance:** new API methods exist and behave identically to `resolve(...)` for output; no perf change yet.

### Phase 2 — Real compiled-template machinery

1. Real `VarTemplate` internal structure: array of `TemplateSegment`s:
   - `LiteralSegment(String text)` — appends raw text.
   - `VarRefSegment(Var var, String key, VarTemplate defaultValue)` — appends `var.resolve(session, key)`, falling back to `defaultValue.resolve(session)` on null/empty.
   - `ScriptSegment(Function fn, ArgSpec[] args)` — TODO-102's `#{...}` dispatch; appends `fn.invoke(session, coercedArgs)`. Args are themselves `VarTemplate`s (so nested `${...}` and `#{...}` work naturally).

2. Compiler: refactored from the existing `VarResolverSession.resolve(...)` tokenizer into a `VarTemplateCompiler` that produces a segment array instead of evaluating in-place. Existing `resolve(...)` becomes shorthand for `compile(input).resolve(session)` so there's one tokenizer in the codebase, not two.

3. Per-segment cached `Var` reference (one map lookup at compile time, zero map lookups at resolve time). Same for `Function` references for `#{...}` segments.

4. `isLiteral()` returns true when the segment array contains only `LiteralSegment`s — caller can fast-path with an `if (template.isLiteral())` check around the resolve loop.

5. Optional: stable-value folding (OQ #3) — if enabled, segments whose var implements `Var.isStable(key) → true` collapse to `LiteralSegment` at compile time.

6. `resolveSupplier(...)` becomes `compile(input).asSupplierWithFreshSessions(vr)` so the perf win flows through to the Supplier path.

**Acceptance:** micro-benchmark in `juneau-utest` shows compiled-form repeated `resolve(...)` ≥ 2× faster than uncompiled-form for representative templates (`"hello ${name:world}"`, `"${a:default}-${b}-${c}"`, and `"#{upper(${name})}"`); functional behavior identical (existing `VarResolver` test suite stays green with zero changes).

### Phase 3 — Consumer retrofit

1. **TODO-79 `@Value` machinery** — `BeanInstantiator` precompiles each `@Value("${...}")` template once at bean construction, stashes the `VarTemplate` on the field's resolution metadata. Subsequent reads use the cached compiled form.

2. **`@Value Supplier<String>` field type** (per OQ #2) — `BeanInstantiator` detects the field's declared type; if it's `Supplier<String>`, wires `template.asSupplierWithFreshSessions(vr)` into the field instead of `template.resolve(...)`.

3. **TODO-102 `#{...}` dispatcher** — `ScriptSegment` is the primary consumer; function lookup + arg-template compilation happens at compile time, not per-resolve.

4. **FINISHED-99 (`@RestOp(path)` SVL)** — `RestOpContext.pathMatchers` Memoizer caches the *compiled* form, not the resolved-string form, so when/if we want dynamic per-request path resolution later we already have the fast path wired.

**Acceptance:** TODO-79's `@Value` resolution path uses cached `VarTemplate`s; TODO-102's `#{...}` dispatch reuses precompiled function metadata; `@Value Supplier<String>` field type works end-to-end with at least one regression test.

### Phase 4 — Docs + release notes

1. New topic page (or section in the existing SVL topic page) covering `VarTemplate`, when to use `compile(...)` vs `resolve(...)`, and the `Supplier<String>` re-evaluation pattern.
2. Release-notes entry in `9.5.0.md`:
   - New API: `VarResolver.resolveSupplier(...)` + `VarResolver.compile(...)` + `VarTemplate` value class.
   - `@Value Supplier<String>` field type now supported for re-evaluating config reads.
   - Internal perf: `${...}` / `#{...}` repeated-resolve path is significantly faster (cite the micro-benchmark numbers).
3. `FINISHED-103-varresolver-template-compilation.md` archive captures the segment-class design, the compile-binding contract, the OQA resolutions, and benchmark numbers.

**Acceptance:** topic page + release-notes entry merged.

## Acceptance criteria (rollup)

- `vr.resolveSupplier(...)` + `vr.compile(...)` + `VarTemplate` ship on the `VarResolver` / `VarResolverSession` surface.
- Existing `String resolve(String)` semantics unchanged (zero regressions in current test suite).
- Compiled-form repeated `resolve(...)` ≥ 2× faster than uncompiled-form on the representative micro-benchmark; ≥ 5× faster for `#{...}` scripting templates (where function-dispatch caching pays off).
- TODO-79's `@Value` machinery uses precompiled templates; `@Value Supplier<String>` field type works.
- TODO-102's `#{...}` dispatcher uses precompiled function metadata.
- Threadsafety: `VarTemplate` instances are safe to share across threads; `asSupplierWithFreshSessions(vr)` Suppliers are safe to share; `session.resolveSupplier(...)` Suppliers inherit session threadsafety (documented).
- Release notes + topic page published.

## Risks

- **Compile-binding contract.** Compiled templates cache `Var` references from the `VarResolver` they were compiled against. If the user rebuilds the `VarResolver` (adds/removes Vars) and continues using old `VarTemplate`s, the cached references are stale. Mitigation: clear Javadoc + naming (`vr.compile(...)` makes the binding obvious). No runtime version check (overhead not justified).
- **Memory cost of cached templates.** Per-`@Value`-site `VarTemplate` is fine; an ad-hoc unbounded LRU on `vr.compile(...)` would be a footgun. OQ #1 resolves this — defaulting to uncached + opt-in bounded cache is the safe call.
- **Stable-value folding correctness.** If we ship OQ #3 (`Var.isStable(key)` folding), it has to be conservative — `ManifestFileVar` is stable per-classpath but not across classpath reloads; `EnvVariablesVar` is stable per-JVM-process; `ConfigVar` is *not* stable if the config can be reloaded. Default-`false` `isStable` + per-Var opt-in is the safe call. Could just defer entirely as a follow-on.
- **Session-threadsafety footgun.** `vr.resolveSupplier(...)` returning a fresh-session-per-`.get()` Supplier is the safe default; `session.resolveSupplier(...)` returning a session-bound Supplier is the perf-sensitive form but only safe within the session's owning thread. Has to be documented prominently.
- **Two-tokenizer drift.** Phase 2 refactors the existing in-place tokenizer in `VarResolverSession.resolve(...)` to call through `compile(...).resolve(session)`. If we accidentally leave both code paths, they can drift. Acceptance criterion: single tokenizer in the codebase after Phase 2.

## Related work

- **TODO-79** — `@Value` annotation. Phase 3 of this TODO retrofits TODO-79's `BeanInstantiator` to use cached `VarTemplate`s + `Supplier<String>` field type support. Soft dep — TODO-103 Phase 1 can land before TODO-79; Phase 3 needs TODO-79 in tree.
- **TODO-102** — `#{...}` scripting support. **Hard dep** — Phase 3 retrofit of the `#{...}` dispatcher requires TODO-102's `Function` SPI to exist. TODO-103 sequenced *after* TODO-102.
- **FINISHED-99** — SVL resolution in `@RestOp(path)`. Phase 3 retrofits the `RestOpContext.pathMatchers` Memoizer to cache compiled form.
- **TODO-95** — Per-RestContext `@Value` resolution against `@Rest(config=...)` Configs. Compiled-form caching is on the per-RestContext `BeanStore` path, so TODO-95's per-resource resolution naturally inherits the perf win.
