# TODO-73: Runtime-overridable `@Rest(paths=...)` resolution chain

Source: split out of the post-FINISHED-72 mixin-pack planning on 2026-05-23. Foundational primitive for TODO-74 / 75 / 76 / 77 / 78.

## Goal

Make the URL patterns from `@Rest(paths=...)` runtime-overridable so applications can rewire mixin paths (e.g. moving `/healthz` to `/health/live` to match k8s conventions) without forking the mixin class. Mirrors how `@Rest(path="...")` is already overridable through `RestContext.Builder.path(String)`; this TODO extends the same affordance to the multi-mount `paths` array shipped in FINISHED-72.

End-state developer experience:

```java
// 1. Annotation default — always present.  Each element is a template that goes through SVL,
//    then the resolved value is split on ',' (trim each piece, drop empties).
@Rest(paths={"/healthz","/readyz","/livez"})
public class BasicHealthResource extends BasicRestServlet { ... }

// 2. Annotation default driven by SVL — Juneau Config key with multi-value payload.
@Rest(paths={"$C{health.paths}"})       // e.g. health.paths = /probe/live, /probe/ready
public class HealthResource extends BasicRestServlet { ... }

// 3. Annotation default driven by SVL — env var with comma-separated default.
@Rest(paths={"$E{HEALTH_PATHS,/healthz,/readyz}"})
public class HealthResource extends BasicRestServlet { ... }

// 4. Importer overrides via getter (subclass / mixin host).
@Rest(mixins=BasicHealthResource.class, paths={"/api"})
public class ApiResource extends BasicRestServlet {
    @Override public String[] getPaths() { return new String[]{"/health/live","/health/ready"}; }
}

// 5. Programmatic override on the builder — wins over everything.
@RestInit
public void onInit(RestContext.Builder b) {
    b.paths("/health/live","/health/ready");
}
```

Documented precedence: **programmatic > getter > annotation default**.

## Design pivot — 2026-05-24

The original TODO-73 plan introduced a parallel `@Rest(pathsKey="...")` member that read its value
from a Juneau `Config` (and, under Spring Boot, also fell back to `Environment.getProperty(...)`).
On review, the user pointed out that this duplicates what SVL already provides for any other
`@Rest` string-typed member: the existing `$C{key}` / `$E{NAME,default}` / `$S{prop,default}`
machinery already does the same key-lookup-and-substitution job. Adding `pathsKey` was a parallel
mechanism whose only justification was that the `paths` array hadn't been wired through SVL yet.

The simplified design folds that wiring into the annotation default rung directly:

- **Each `@Rest(paths={...})` element runs through SVL** — same `VarResolver` surface as any
  other `@Rest` string member. `$C{key}` consults whatever `Config` is registered on the bean
  store (test overlay, `@Bean` factory, or the framework's runtime `Config`); `$E{NAME,default}`
  and `$S{prop,default}` use the bootstrap variable catalog without needing any beans.
- **The post-SVL value is split on `,`** — trim each piece, drop empties. A single template
  element can therefore expand to zero, one, or many mount paths.
- **`pathsKey` is gone** — the same use case is now expressed as `@Rest(paths={"$C{health.paths}"})`,
  with the comma-separated value living in the `Config` file just as it did before.
- **Spring `Environment` fallback is deferred to a separate work item** — once a Juneau-`Config`-
  to-`Environment` bridge lands under that work item, `$C{key}` will transparently consult
  Spring's `Environment` under Spring Boot with no API change here. The plumbing for the bridge
  is broader than the paths use case (it benefits every `$C{key}` reference in the framework),
  so it's tracked separately.

This dropped one annotation member, two helper methods (`resolvePathsKeyValue`,
`lookupSpringEnvironmentProperty`), one diagnostic log line, two dedicated test classes (the
`_ConfigKey_Test` and `_SpringEnvironment_Test`), and a sub-bullet from the resolver chain.

## Scope

**In scope (v1):**

- `RestContext.Builder.paths(String...)` programmatic setter (parallel to existing `path(String)` setter).
- `RestObject#getPaths()` / `RestServlet#getPaths()` virtual method on the canonical resource base classes — default implementation returns `null` (= "inherit annotation"). Subclasses can override to substitute paths at construction time.
- **SVL on `@Rest(paths=...)` element literals** — each array element runs through the bootstrap
  `VarResolver` (with a session bound to the live bean store, so `ConfigVar` can resolve
  `$C{key}` against the registered `Config`). The post-SVL value is split on `,` (trim each
  piece, drop empties). A single template element can expand to zero, one, or many mount paths.
- Resolution occurs once during `RestContext` construction; the resolved `String[]` is what `JettyServerComponent.restPathsFor(...)` (and Spring Boot's `JuneauRestInitializer`) sees.
- Public mount-time helper `RestContext.resolveTopLevelPaths(Class, Object, BeanStore)` so Spring
  Boot's manual `ServletRegistrationBean` flow can resolve the paths without instantiating a
  full `RestContext` first.
- Documented precedence chain. Tests for each rung. Tests for the multi-mount integration through Jetty's multi-pattern mount.

**Explicitly out of scope (v1):**

- Hot-reload of paths after `RestContext` construction. One resolution pass; rebuild the context to re-resolve.
- Per-request path rewriting. That's a different concern (URL rewriting, reverse proxy territory).
- Spring `Environment` integration as a `pathsKey` fallback. Superseded by the separate
  Juneau-`Config`-to-`Environment` bridge work item.
- A dedicated `pathsKey` annotation member. Superseded by SVL on `paths` elements.
- Migration of `@Rest(path="...")` (singular) to use this same chain. The singular case already has its own resolution; this TODO touches only the array variant.

## Dependency-injection notes

- **Mixin/resource resolution is unchanged.** The FINISHED-72 mixin walk and Spring `BeanStore` adapter (`juneau-rest/juneau-rest-server-springboot/src/main/java/org/apache/juneau/rest/springboot/SpringBeanStore.java`) already resolve resource-class instances from the active bean store. No new plumbing is needed at the resource level — both microservice (`BasicBeanStore` lookup) and Spring Boot (`SpringBeanStore` → `ApplicationContext.getBean(...)`) paths produce a `RestContext.Builder` whose `paths(...)` setter we're adding here.
- **Builder-time configuration sourcing.** The three override rungs are sourced as follows under each path:
    - **Programmatic setter** (`RestContext.Builder.paths(String...)`) — identical under both paths; called from `@RestInit` or a `RestServletInitializer` hook.
    - **`getPaths()` getter** — identical under both paths; the importer subclass overrides the method.
    - **Annotation default with SVL** — identical under both paths. The bootstrap `VarResolver`
      opens a session bound to the live `WritableBeanStore`, so `$C{key}` resolves against the
      registered `Config` (which under microservice comes from `BasicBeanStore`'s default
      registration and under Spring Boot comes from `SpringBeanStore`'s parent-chain to the
      `ApplicationContext`).
- **Spring-Boot integration is automatic for env vars and system properties.** `$E{NAME,default}`
  and `$S{prop,default}` use the bootstrap variable catalog without needing any beans, so they
  work identically under microservice and Spring Boot.
- **Spring `Environment` integration is deferred.** Until the separate Juneau-`Config`-to-
  `Environment` bridge work item lands, Spring Boot users who want `application.yaml` keys to
  drive `$C{...}` references must register a Juneau `Config` whose contents include the keys.
  Once the bridge lands, `$C{key}` will transparently consult Spring's `Environment` after a
  Juneau-`Config` miss — no API change here.
- **Acceptance bullet** below: "Mixin works identically when registered via Juneau `BeanStore` (microservice path) and via Spring `@Bean` (Spring Boot path); both paths covered by a test."

## Phased steps

### Phase 0 — confirm seams (read-only) [done]

1. `RestContext.Builder` — `path(String)` already memoizes a single resolved value at build time; `paths(String...)` was added alongside without disturbing the singular flow.
2. `JettyServerComponent.restPathsFor(...)` — the FINISHED-72 multi-pattern mount reads from `RestContext.getPaths()` (or equivalent) and not directly from the annotation.
3. Singular `@Rest(path="...")` — confirmed it does *not* itself apply SVL today; the SVL pass is new for the array form. (Surprise vs. the original assumption, captured in the design pivot.)
4. `splitPathsValue(...)` helper — reusable; trim-and-drop-empty semantics match what the SVL pass needs.

### Phase 1 — programmatic + getter rungs [done]

1. `RestContext.Builder.paths(String... paths)` setter; null/empty array clears.
2. `RestServlet#getPaths()` and `RestObject#getPaths()` default-`null` virtual methods.
3. Wire both into the path-resolution chain in `RestContext` build, with explicit precedence documented in the javadoc.
4. Tests:
    - `RestPathsRuntimeOverride_Programmatic_Test` — `Builder.paths(...)` overrides annotation; null arg resets to annotation default; empty arg explicitly clears (no mounts).
    - `RestPathsRuntimeOverride_Getter_Test` — `getPaths()` override on subclass beats annotation; `null` return falls through.

### Phase 2 — SVL on annotation defaults + comma-split [done]

1. Apply SVL to each `@Rest(paths={...})` element, then split on `,` (trim, drop empties).
2. Use a `VarResolverSession` bound to the live `WritableBeanStore` so `$C{key}` resolves against the registered `Config` (including test overlays).
3. SVL failures fall back to the literal element rather than throwing during construction.
4. Tests:
    - `RestPathsRuntimeOverride_SVL_Test` — `$C{key}` resolves from a registered `Config`; `$E{NAME,default}` resolves from env (with default fallback); `$S{prop,default}` resolves from system properties (with default fallback); mix of literal + SVL elements; `$C{key,default}` falls back to in-annotation default on Config miss; `$C{key}` with no default produces zero mounts (empty SVL substitution dropped by comma-split).
    - `RestPathsRuntimeOverride_CommaSplit_Test` — single element with comma → multi-mount; whitespace trimming; empty pieces dropped; multi-element + comma-in-element; SVL + comma combination.

### Phase 3 — multi-mount integration [done]

1. `JettyServerComponent` and the Spring Boot `JuneauRestInitializer` both consume the resolved `String[]` (not the raw annotation).
2. Tests:
    - `RestPathsRuntimeOverride_JettyMount_Test` — programmatic override produces correct exact-match mounts under Jetty; SVL-driven `$C{key}` mounts likewise.
    - `RestPathsRuntimeOverride_Springboot_Test` — same under Spring Boot's embedded servlet container, exercising the Spring `BeanStore` adapter end-to-end. Also verifies `BeanStore` parity (microservice vs. Spring Boot) for SVL resolution.

### Phase 4 — docs + release notes [done]

1. Release-notes entry under `### juneau-rest-server` and `### juneau-rest-server-springboot`.
2. New section in `docs/pages/topics/RestServerComposition.md` titled "Runtime-overridable paths" with the precedence table, worked examples for each rung, Spring Boot integration walkthrough, and the `null`-vs-empty-array semantic table.

## Acceptance criteria

- [x] `RestContext.Builder.paths(String...)` overrides `@Rest(paths=...)` and is the highest-priority rung.
- [x] `getPaths()` override on a `RestServlet`/`RestObject` subclass beats the annotation when programmatic setter is unused.
- [x] Each `@Rest(paths={...})` element runs through SVL substitution; the post-SVL value is split on `,` (trim each piece, drop empties).
- [x] `$C{key}` resolves from a registered Juneau `Config` (including test overlays); `$E{NAME,default}` and `$S{prop,default}` use the bootstrap variable catalog.
- [x] Empty annotation default + empty getter + no programmatic call → resource has no top-level mounts and a clear error message names the resource.
- [x] `null` getter return is treated as "inherit annotation"; explicit empty array (`new String[0]`) clears the mount list.
- [x] Mixin works identically when registered via Juneau `BeanStore` (microservice path) and via Spring `@Bean` (Spring Boot path); both paths covered by a test.
- [x] Coverage ≥ 95% on the new resolution code. Full `./scripts/test.py` green.

## Open questions

1. **Precedence order.** Programmatic > getter > annotation. **Recommend yes** — programmatic wins because it's the most explicit rung; getter sits between code and annotation because subclass authors control it directly; annotation is the fallback. Consistent with how `path(String)` resolves today. **Resolved 2026-05-24: accepted as recommended.**
2. **Null vs empty semantics.** `getPaths()` returning `null` means "inherit annotation"; returning `new String[0]` means "explicitly clear, no mounts". **Recommend that exact semantic** — gives subclasses a way to delete mounts entirely (rare but legal). **Resolved 2026-05-24: accepted as recommended.**
3. **Single vs multi getter.** Keep the existing scalar `getPath()` for `@Rest(path="...")` legacy single-pattern accessor; new `getPaths()` is array-canonical and orthogonal. **Recommend two getters, no merge** — they serve different annotations. **Resolved 2026-05-24: accepted as recommended.**
4. **SVL-element split format.** Each post-SVL value is split on `,` and trimmed; empties drop. **Recommend comma-delimited** — matches how `Config.getStringArray(...)` works and what users expect from a CSV-style env var. **Resolved 2026-05-24: accepted as recommended.**
5. ~~**Spring `@Value` / `Environment` integration via `pathsKey`.**~~ **Superseded by the design pivot.** Spring `Environment` integration is deferred to a separate Juneau-`Config`-to-`Environment` bridge work item; once that lands, `$C{key}` in `@Rest(paths=...)` will transparently consult Spring's `Environment` with no API change here.
6. ~~**SVL in `pathsKey`-loaded values.**~~ **Superseded by the design pivot.** SVL now runs directly on `@Rest(paths=...)` element literals; there is no `pathsKey` member.

## Risks

- **Precedence-rung ambiguity.** Users who set both a getter and a programmatic builder call may be surprised by which wins. Mitigation: document the precedence in javadoc on `getPaths()` and the builder setter.
- **SVL-on-elements as a quiet behavior change.** A pre-9.5.0 element value containing `$C{...}` or `$E{...}` would previously have been a literal (servlet-spec-illegal) string; under 9.5.0 it gets resolved. Mitigation: this is the documented behavior and the comma-split + trim-and-drop-empty semantics match every other SVL-bearing `@Rest` member. Pre-9.5.0 elements that *don't* contain SVL markers or embedded commas see identical behavior.
- **Multi-mount × programmatic-override interaction with collision detection.** FINISHED-72's importer-wins rule operates on the *resolved* paths; a runtime override that collides with another mounted resource needs to fail loudly (not silently overwrite). Mitigation: collision check runs on the resolved `String[]`, not on the annotation literal.
- **Eclipse / Maven incremental-build staleness.** SVL on annotation elements is read at runtime, but Eclipse may cache annotation values across rebuilds. Mitigation: AGENTS.md "Build Automatically" caveat; document.

## Related work

- `todo/FINISHED-72-rest-mixins-and-paths.md` — established `@Rest(paths=...)` and the multi-mount Jetty plumbing this TODO extends.
- `todo/TODO-74-mixin-api-docs.md` (sibling) — soft dependency; api-docs mixin's `paths={"/api","/openapi", ...}` defaults benefit from this primitive.
- `todo/TODO-75-mixin-static-files.md` (sibling) — soft dependency; static-files mixin's `paths={"/static/*","/htdocs/*"}` defaults benefit.
- `todo/TODO-76-mixin-convention-endpoints.md` (sibling) — soft dependency; favicon/seo/version/well-known mixins all default-configured paths.
- `todo/TODO-77-mixin-ops-introspection.md` (sibling) — soft dependency; admin/echo/route-index mixins benefit from SVL-driven prod/staging variance in `@Rest(paths=...)`.
- `todo/TODO-78-mixin-jsp-module.md` (sibling) — soft dependency; JSP mixin's `paths={"/jsp/*"}` default benefits.
- Separate Juneau-`Config`-to-`Environment` bridge work item — once it lands, `$C{key}` in `@Rest(paths=...)` will transparently consult Spring's `Environment` under Spring Boot.
- `juneau-rest/juneau-rest-server-springboot/` — Spring `BeanStore` adapter module; this TODO's SVL pass uses the same bean store that the adapter populates.
- `juneau-microservice/` and the `BeanStore` walk in `RestContext` — the microservice-path equivalent the same resolver runs against by default.
- Existing: `RestContext.Builder.path(String)` — the scalar precedent this TODO mirrors for the array case.
