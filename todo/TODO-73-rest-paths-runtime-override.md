# TODO-73: Runtime-overridable `@Rest(paths=...)` resolution chain

Source: split out of the post-FINISHED-72 mixin-pack planning on 2026-05-23. Foundational primitive for TODO-74 / 75 / 76 / 77 / 78.

## Goal

Make the URL patterns from `@Rest(paths=...)` runtime-overridable so applications can rewire mixin paths (e.g. moving `/healthz` to `/health/live` to match k8s conventions) without forking the mixin class. Mirrors how `@Rest(path="...")` is already overridable through `RestContext.Builder.path(String)`; this TODO extends the same affordance to the multi-mount `paths` array shipped in FINISHED-72.

End-state developer experience:

```java
// 1. Annotation default — always present.
@Rest(paths={"/healthz","/readyz","/livez"})
public class BasicHealthResource extends BasicRestServlet { ... }

// 2. Importer overrides via getter (subclass / mixin host).
@Rest(mixins=BasicHealthResource.class, paths={"/api"})
public class ApiResource extends BasicRestServlet {
    @Override public String[] getPaths() { return new String[]{"/health/live","/health/ready"}; }
}

// 3. Importer overrides via config key (Juneau Config or Spring Environment).
@Rest(mixins=BasicHealthResource.class, pathsKey="health.paths")
public class ApiResource extends BasicRestServlet { ... }

// 4. Programmatic override on the builder — wins over everything.
@RestInit
public void onInit(RestContext.Builder b) {
    b.paths("/health/live","/health/ready");
}
```

Documented precedence: **programmatic > config-key > getter > annotation default**.

## Why now

- Five sibling mixin TODOs (TODO-74 through TODO-78) each ship with a `@Rest(paths=...)` default. Without this primitive, every one of them would need to invent its own override pattern. Land this once, reuse everywhere.
- `@Rest(path="...")` already supports a programmatic `RestContext.Builder.path(String)` setter, so half the precedence chain already exists for the scalar case — extending it to the array case is mostly a parity exercise.
- Spring Boot users already expect `@Value("${health.paths}")` style externalization; FINISHED-72 currently forces them to subclass.
- The change is additive — null/empty getter return falls back to the annotation, so existing FINISHED-72 callers see no behavior change.

## Scope

**In scope (v1):**

- `RestContext.Builder.paths(String...)` programmatic setter (parallel to existing `path(String)` setter).
- `RestObject#getPaths()` / `RestServlet#getPaths()` virtual method on the canonical resource base classes — default implementation returns `null` (= "inherit annotation"). Subclasses can override to substitute paths at construction time.
- `@Rest(pathsKey="...")` annotation member — when set, the framework reads `Config.getString(pathsKey)` (and, under the Spring-Boot-`BeanStore` adapter, `Environment.getProperty(pathsKey)`) and parses the comma-delimited result as a `String[]`. Empty / unset key falls through to the next rung.
- Resolution occurs once during `RestContext` construction; the resolved `String[]` is what `JettyServerComponent.restPathsFor(...)` (and Spring Boot's `JuneauRestInitializer`) sees.
- Documented precedence chain. Tests for each rung. Tests for the multi-mount integration through Jetty's multi-pattern mount.

**Explicitly out of scope (v1):**

- Hot-reload of paths after `RestContext` construction. One resolution pass; rebuild the context to re-resolve.
- Per-request path rewriting. That's a different concern (URL rewriting, reverse proxy territory).
- Wildcard expansion in `pathsKey` — the resolved value is taken literally and split on commas, no glob support.
- Migration of `@Rest(path="...")` (singular) to use this same chain. The singular case already has its own resolution; this TODO touches only the array variant.

## Dependency-injection notes

- **Mixin/resource resolution is unchanged.** The FINISHED-72 mixin walk and Spring `BeanStore` adapter (`juneau-rest/juneau-rest-server-springboot/src/main/java/org/apache/juneau/rest/springboot/SpringBeanStore.java`) already resolve resource-class instances from the active bean store. No new plumbing is needed at the resource level — both microservice (`BasicBeanStore` lookup) and Spring Boot (`SpringBeanStore` → `ApplicationContext.getBean(...)`) paths produce a `RestContext.Builder` whose `paths(...)` setter we're adding here.
- **Builder-time configuration sourcing.** The four override rungs are sourced as follows under each path:
    - **Programmatic setter** (`RestContext.Builder.paths(String...)`) — identical under both paths; called from `@RestInit` or a `RestServletInitializer` hook.
    - **`getPaths()` getter** — identical under both paths; the importer subclass overrides the method.
    - **`pathsKey` config-key resolution** — under microservice, looks up `BeanStore.getBean(Config.class)` → `Config.getString(pathsKey)`. Under Spring Boot, the `SpringBeanStore`'s parent-chain returns the same `Config` if one was registered, but the resolver also falls back to `BeanStore.getBean(org.springframework.core.env.Environment.class)` → `Environment.getProperty(pathsKey)` so Spring's standard property-resolution chain (`application.yml`, system props, `--args`, profiles, etc.) works without explicit Juneau-`Config` wiring.
    - **Annotation default** — identical under both paths; pure compile-time literal.
- **Spring-Boot-specific gotchas.**
    - `Environment.getProperty(pathsKey)` returns a single `String`; we split on `,` (trimmed) to match `Config.getStringArray(...)` semantics. Document explicitly so users don't expect Spring's `String[]` array binding.
    - `SpringBeanStore.getBean(Environment.class)` works only when the resource is constructed through the `JuneauRestInitializer` adapter (which seeds the bean store with the active `ApplicationContext`). Pure-microservice deployments harmlessly miss the `Environment` lookup and fall through to the next rung.
    - `@Primary` / `@Qualifier` are not relevant for `Environment` (singleton) but are relevant if a user later registers a custom `Config` bean — the existing `BeanStore.getBean(Config.class)` returns the first match, which under Spring follows `@Primary` semantics. No new contract.
- **Acceptance bullet** added below: "Mixin works identically when registered via Juneau `BeanStore` (microservice path) and via Spring `@Bean` (Spring Boot path); both paths covered by a test."

## Phased steps

### Phase 0 — confirm seams (read-only)

1. `RestContext.Builder` — confirm that `path(String)` already memoizes a single resolved value at build time and that we can add `paths(String...)` alongside without disturbing the singular flow.
2. `JettyServerComponent.restPathsFor(...)` — confirm that the FINISHED-72 multi-pattern mount reads from `RestContext.getPaths()` (or equivalent) and not directly from the annotation.
3. `SpringBeanStore` — confirm that `BeanStore.getBean(Environment.class)` resolves cleanly when the adapter is in use and returns `Optional.empty()` otherwise.
4. `Config.getString(...)` / `Config.getStringArray(...)` — confirm both are present and which signature gives us cleanest comma-split semantics.

### Phase 1 — programmatic + getter rungs

1. Add `RestContext.Builder.paths(String... paths)` setter; null/empty array clears.
2. Add `RestServlet#getPaths()` and `RestObject#getPaths()` default-`null` virtual methods.
3. Wire both into the path-resolution chain in `RestContext` build, with explicit precedence documented in the javadoc.
4. Tests:
    - `RestPathsRuntimeOverride_Programmatic_Test` — `Builder.paths(...)` overrides annotation; null arg resets to annotation default; empty arg explicitly clears (no mounts).
    - `RestPathsRuntimeOverride_Getter_Test` — `getPaths()` override on subclass beats annotation; `null` return falls through.

### Phase 2 — `pathsKey` config-key rung

1. Add `String pathsKey() default ""` to `@Rest`.
2. Resolution code: when `pathsKey` is non-empty, look up `Config` from the bean store and call `getStringArray(pathsKey)`; if result is empty, fall through.
3. Tests:
    - `RestPathsRuntimeOverride_ConfigKey_Test` — `pathsKey` resolves from a `Config` bean; missing key falls through; empty value falls through.

### Phase 3 — Spring `Environment` fallback

1. In the `pathsKey` resolver, after the `Config` miss, also try `BeanStore.getBean(Environment.class)` and split on `,`.
2. Tests:
    - `RestPathsRuntimeOverride_SpringEnvironment_Test` (in `juneau-rest/juneau-rest-server-springboot` test sources) — Spring `Environment.getProperty(...)` resolves the key when no Juneau `Config` is registered.

### Phase 4 — multi-mount integration

1. Confirm `JettyServerComponent` and the Spring Boot `JuneauRestInitializer` both consume the resolved `String[]` (not the raw annotation).
2. Tests:
    - `RestPathsRuntimeOverride_JettyMount_Test` — programmatic override produces correct exact-match mounts under Jetty.
    - `RestPathsRuntimeOverride_Springboot_Test` — same under Spring Boot's embedded servlet container, exercising the Spring `BeanStore` adapter end-to-end.

### Phase 5 — docs + release notes

1. Release-notes entry under `### juneau-rest-server` and `### juneau-rest-server-springboot`.
2. New section in `docs/pages/topics/RestServerComposition.md` titled "Runtime-overridable paths" with the precedence table, plus a one-row migration note in the 9.5.0 migration guide if any FINISHED-72 caller's behavior shifts (expected: none, fully additive).

## Acceptance criteria

- [ ] `RestContext.Builder.paths(String...)` overrides `@Rest(paths=...)` and is the highest-priority rung.
- [ ] `getPaths()` override on a `RestServlet`/`RestObject` subclass beats the annotation when programmatic setter is unused.
- [ ] `@Rest(pathsKey="x")` reads from a registered Juneau `Config`; missing key falls through.
- [ ] When deployed through `juneau-rest-server-springboot`, `pathsKey` additionally resolves from Spring's `Environment` (after the `Config` miss).
- [ ] Empty annotation default + empty getter + empty config-key + no programmatic call → resource has no top-level mounts and a clear error message names the resource.
- [ ] `null` getter return is treated as "inherit annotation"; explicit empty array (`new String[0]`) clears the mount list.
- [ ] Mixin works identically when registered via Juneau `BeanStore` (microservice path) and via Spring `@Bean` (Spring Boot path); both paths covered by a test.
- [ ] Coverage ≥ 95% on the new resolution code. Full `./scripts/test.py` green.

## Open questions

1. **Precedence order.** Programmatic > config-key > getter > annotation. **Recommend yes** — programmatic wins because it's the most explicit rung; getter sits between code and config because subclass authors control it directly; annotation is the fallback. Consistent with how `path(String)` resolves today.
2. **Null vs empty semantics.** `getPaths()` returning `null` means "inherit annotation"; returning `new String[0]` means "explicitly clear, no mounts". **Recommend that exact semantic** — gives subclasses a way to delete mounts entirely (rare but legal).
3. **Single vs multi getter.** Keep the existing scalar `getPath()` for `@Rest(path="...")` legacy single-pattern accessor; new `getPaths()` is array-canonical and orthogonal. **Recommend two getters, no merge** — they serve different annotations.
4. **`pathsKey` value format.** Comma-delimited string parsed at resolve time (e.g. `"/healthz,/readyz"`). **Recommend comma-delimited** — matches `Config.getStringArray(...)` and Spring's standard `String[]` binding.
5. **Spring `@Value` / `Environment` integration.** Should `@Rest(pathsKey="...")` resolve from Spring's `Environment` automatically when running under Spring Boot, in addition to Juneau's `Config`? **Recommend yes** — when the resource is resolved through the Spring-`BeanStore` adapter, fall back to `Environment.getProperty(pathsKey)` if the Juneau `Config` lookup misses. Documented as a Spring-Boot-path enhancement.
6. **SVL variable resolution in `pathsKey`-loaded values.** Should the loaded value run through SVL like other annotation values? **Recommend yes** for parity with the rest of `@Rest`'s string-typed members; `${env.HEALTH_PATHS:/healthz,/readyz}` should work uniformly.

## Risks

- **Precedence-rung ambiguity.** Users who set both a getter and a config key may be surprised by which wins. Mitigation: document the precedence in javadoc on `pathsKey`, and emit a `Logger.fine(...)` line at resolution time naming the chosen rung.
- **Spring `Environment` fallback masking missing `Config`.** A user who intends to read from `Config` but typos the key may silently fall through to `Environment` and pick up an unrelated value. Mitigation: log when the fallback fires; document loudly.
- **Multi-mount × programmatic-override interaction with collision detection.** FINISHED-72's importer-wins rule operates on the *resolved* paths; a runtime override that collides with another mounted resource needs to fail loudly (not silently overwrite). Mitigation: collision check runs on the resolved `String[]`, not on the annotation literal.
- **Eclipse / Maven incremental-build staleness.** `@Rest(pathsKey=...)` is read at runtime, but Eclipse may cache annotation values. Mitigation: AGENTS.md "Build Automatically" caveat; document.

## Related work

- `todo/FINISHED-72-rest-mixins-and-paths.md` — established `@Rest(paths=...)` and the multi-mount Jetty plumbing this TODO extends.
- `todo/TODO-74-mixin-api-docs.md` (sibling) — soft dependency; api-docs mixin's `paths={"/api","/openapi", ...}` defaults benefit from this primitive.
- `todo/TODO-75-mixin-static-files.md` (sibling) — soft dependency; static-files mixin's `paths={"/static/*","/htdocs/*"}` defaults benefit.
- `todo/TODO-76-mixin-convention-endpoints.md` (sibling) — soft dependency; favicon/seo/version/well-known mixins all default-configured paths.
- `todo/TODO-77-mixin-ops-introspection.md` (sibling) — soft dependency; admin/echo/route-index mixins benefit from `pathsKey`-driven prod/staging variance.
- `todo/TODO-78-mixin-jsp-module.md` (sibling) — soft dependency; JSP mixin's `paths={"/jsp/*"}` default benefits.
- `juneau-rest/juneau-rest-server-springboot/` — Spring `BeanStore` adapter module; this TODO adds the `Environment` fallback hook.
- `juneau-microservice/` and the `BeanStore` walk in `RestContext` — the microservice-path equivalent the same resolver runs against by default.
- Existing: `RestContext.Builder.path(String)` — the scalar precedent this TODO mirrors for the array case.
