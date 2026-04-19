# TODO-24: JSR-330 alignment + Spring-lite extensions for `commons.inject`

Source: created 2026-04-19.

## Goal

Make **`org.apache.juneau.commons.inject`** present a story we can summarize as:

> *"Juneau supports JSR-330 (`jakarta.inject` / `javax.inject`) annotations as closely as our lightweight model allows, **without** taking a hard dependency on `jakarta.inject-api`. Where Spring conventions add value to the lightweight model, we adopt them under our own annotations."*

**Non-goals:**

- Become a JSR-330 / CDI **provider**.
- Become a Spring replacement.
- Pull `jakarta.inject-api` into the compile classpath of `juneau-commons` or downstream modules.

**Related TODOs:** **TODO-21** (rename `@RestInject` to a `commons.inject` `@Bean`, plus optional `@Configuration`) and **TODO-23** (broader inject roadmap). This plan **focuses** on the JSR-330 angle and slots into the same package surface.

---

## Strategy

1. **Define Juneau-owned annotations** that are **shape-compatible** with JSR-330 in `org.apache.juneau.commons.inject` (or wherever `commons.inject` ends up post-TODO-15/14):
   - `@Inject`
   - `@Named` (already exists at `org.apache.juneau.annotation.Named`; decide whether to **move / mirror** it into `commons.inject` or keep current location and document equivalence).
   - `@Qualifier` (meta-annotation marker — used to mark *other* annotations as qualifiers).
   - `@Singleton` (scope marker; semantics described below).
   - `Provider<T>` interface (Juneau version) — minimal: `T get();`
2. **Recognize `jakarta.inject` / `javax.inject` annotations by fully-qualified name** in the same code paths that handle the Juneau equivalents — **no compile-time dependency**, **no `import`**. Use string-based annotation type matching (already partially the pattern in `ParameterInfo.findQualifierInternal()` which checks for simple names `Named` / `Qualifier`).
3. **Honor JSR-330 semantics where they fit our lightweight model**, document any deviations in a single page (charter / Javadoc package summary).
4. **Layer Spring-flavored conventions on top** (TODO-21 `@Bean`, optional `@Configuration`, `@Primary`, etc.) using **Juneau-owned** annotations only.

---

## JSR-330 surface — per-feature plan

| JSR-330 element | Juneau plan | Notes |
|------------------|------------|-------|
| **`@Inject`** (constructor / method / field) | Provide **`org.apache.juneau.commons.inject.Inject`** with same targets and runtime retention; treat **`jakarta.inject.Inject`** / **`javax.inject.Inject`** as equivalent in lookup code. **Expose `@Inject` as a user-facing annotation** on REST resources — fields and methods on `@Rest` resources annotated `@Inject` are populated from the resource's `BeanStore` at initialization time. | Constructor injection is the primary pattern Juneau already uses (`BeanCreator2` resolves params from `BeanStore`). User-facing field/method injection is scoped to types **the framework already constructs** (REST resource classes, registered `@Configuration`-style hosts) — not arbitrary user types created elsewhere. Document Juneau's deviations from full JSR-330 semantics (no circular-dependency resolver, no scope SPI). |
| **`@Named`** | Keep / move Juneau **`@Named`**; recognize **`jakarta.inject.Named`** / **`javax.inject.Named`** for both **resolution** and as **qualifier** info. | `ParameterInfo.findQualifierInternal()` is the existing template — generalize to any FQN ending in `Named` from these two packages. |
| **`@Qualifier`** (meta-annotation) | Add **`org.apache.juneau.commons.inject.Qualifier`** as a meta-annotation marker; treat **any** annotation that is itself meta-annotated with **Juneau `@Qualifier`** OR **`jakarta.inject.Qualifier`** OR **`javax.inject.Qualifier`** as a qualifier on a parameter / field. | Lookup keys: `(type, qualifier-set)`. Start by supporting **string-valued qualifiers** (mirror `@Named`) and exact-match annotation-type qualifiers; defer qualifier *attribute* equality (Spring-style) unless a consumer needs it. |
| **`@Scope` + `@Singleton`** | Add **`org.apache.juneau.commons.inject.Singleton`** as a scope marker; recognize JSR-330 `Singleton` by FQN. | Semantics in our model: a singleton is **registered once in a `BeanStore`** and reused; no proxying, no JVM-wide singleton. Custom `@Scope` annotations are **out of scope** for v1. |
| **`Provider<T>`** | Add **`org.apache.juneau.commons.inject.Provider<T>`** with single method `T get()`. Treat parameter / field of type **`jakarta.inject.Provider<T>`** / **`javax.inject.Provider<T>`** as resolvable when the `BeanStore` can supply `T` (wrap a `Supplier<T>` already returned by `getBeanSupplier`). | Avoids hard dep on `jakarta.inject-api`; Juneau code paths construct an adapter at the injection point using FQN reflection. **`Supplier<T>`** continues to be the native form. |

### Optional `jakarta.inject-api` interop (no dependency)

- All matching is on **`Class.getName()`** / annotation **FQN**, never on classloader-shared types.
- If `jakarta.inject-api` is on the **application** classpath, Juneau picks up its annotations transparently.
- If absent, Juneau-owned annotations cover the same role.
- Document this clearly: *"Add `jakarta.inject-api` to your project if you want to write `@jakarta.inject.Inject`; otherwise use `org.apache.juneau.commons.inject.Inject`."*

---

## Spring-lite additions worth adopting (Juneau-owned only)

Selected from **TODO-23 Tier A**, layered on top of the JSR-330 base:

- **`@Bean`** (TODO-21) — provider-style declaration on methods/fields hosted by a class registered with the framework.
- **`@Configuration`** (TODO-21, optional) — marker for types that host `@Bean` declarations.
- **`@Primary`** — disambiguate when multiple candidates of a type exist; equivalent in Spring; not in JSR-330.
- **`@Order` / integer priority** — for ordered contributors (filters, parsers); decide whether to mirror Spring `@Order` or use a `priority` attribute on `@Bean`.

Explicitly **declined** for v1 (revisit only with a real consumer):

- Field injection on user types outside framework callsites.
- Lazy proxies, AOP, scopes beyond singleton + per-store.
- Spring's `@Autowired(required=false)` / `ObjectProvider` — `Optional<T>` and `Provider<T>` cover these.

---

## Implementation plan

### Phase 1 — Charter and naming

- [ ] Confirm `org.apache.juneau.commons.inject` as the package home for the new annotations (align with TODO-15 / TODO-14 outcomes for class-name finalization).
- [ ] **Move** `org.apache.juneau.annotation.Named` to `org.apache.juneau.commons.inject.Named`. Update all references; add a row to the v9.5 Migration Guide (TODO-17). No parallel/deprecated copy is kept (decided: no current consumers depend on the old location overlapping with `jakarta.inject.Named`).
- [ ] Write a **package-info.java** charter for `commons.inject`: goals, JSR-330 alignment statement, list of recognized FQNs, list of declined Spring features.

### Phase 2 — Add Juneau-owned annotations + `Provider`

- [ ] **`@Inject`**, **`@Qualifier`** (meta), **`@Singleton`** added in `commons.inject`.
- [ ] **`Provider<T>`** interface added in `commons.inject`.
- [ ] Javadoc each one with the corresponding `jakarta.inject` link and the equivalence statement.

### Phase 3 — Recognition of JSR-330 annotations by FQN

- [ ] Centralize recognized FQN constants (e.g. `JsrInject.JAKARTA_INJECT`, `JsrInject.JAVAX_INJECT`, etc.) in a single utility class to avoid string-literal sprawl.
- [ ] Update **`ParameterInfo.findQualifierInternal()`** to match by FQN against:
   - `org.apache.juneau.commons.inject.Named` (and current `org.apache.juneau.annotation.Named` until/if moved)
   - `jakarta.inject.Named`, `javax.inject.Named`
   - any annotation meta-annotated with one of the recognized `@Qualifier` annotations.
- [ ] Update bean-creation paths (`BeanCreator2` and equivalents) to honor `@Inject` on constructors / methods using the same FQN list.

### Phase 4 — `Provider<T>` injection support

- [ ] At parameter resolution time, if the parameter type's raw class FQN is one of:
   - `org.apache.juneau.commons.inject.Provider`
   - `jakarta.inject.Provider`
   - `javax.inject.Provider`
   Then look up a `Supplier<T>` from the `BeanStore` and adapt it via **`java.lang.reflect.Proxy`** (the JSR-330 `Provider` is an interface, so this is supported) with an `InvocationHandler` that delegates `get()` to the supplier. Cache the proxy `Class` per `Provider` interface to avoid per-call generation.
- [ ] Confirm that the proxy is loaded with the **Provider interface's `ClassLoader`** so it works correctly when `jakarta.inject-api` is supplied by the application.
- [ ] Add tests for absence of `jakarta.inject-api`: simulate by excluding the artifact from the test classpath of one module.

### Phase 5 — `@Singleton` scope semantics

- [ ] Define: a class annotated `@Singleton` (Juneau or JSR-330) registered with a `WritableBeanStore` is **instantiated once per store** and reused; framework will not re-create on each `getBean` call.
- [ ] No JVM-wide singleton, no proxying.
- [ ] Document and add a positive + negative test (singleton vs default per-call behavior, depending on baseline).

### Phase 6 — Spring-lite layer

- [ ] Land **TODO-21** outcomes: `@Bean`, optional `@Configuration` in the same package.
- [ ] Add **`@Primary`** if a concrete consumer needs it; otherwise hold.
- [ ] Decide ordering story (`@Order` vs `priority` on `@Bean`); document.

### Phase 7 — Docs and migration

- [ ] `juneau-docs`: add an **Inject** topic that includes the JSR-330 equivalence table and a "What we don't do" section.
- [ ] Migration rows in **v9.5 Migration Guide** (TODO-17): any rename of `@Named` location, addition of `@Inject`/`@Qualifier`/`@Singleton`/`Provider` types.
- [ ] Release notes (`9.2.1` or next release file): summary of JSR-330 alignment.

---

## Resolved decisions

- **D1 (was Q1).** **Move** `org.apache.juneau.annotation.Named` into `org.apache.juneau.commons.inject.Named`. No deprecated alias kept at the old location. Add a Migration Guide row (TODO-17).
- **D2 (was Q2).** Confirmed: no current consumers depend on the `org.apache.juneau.annotation.Named` location overlapping with `jakarta.inject.Named`. The straight move in D1 is acceptable.
- **D3 (was Q3).** **Yes — expose `@Inject` as a user-facing annotation.** Support `@Inject` on **fields and methods of `@Rest` resources** (and other framework-instantiated types), populated from the resource `BeanStore` at initialization. Limit to types **Juneau already constructs**; do not introduce a global injector for arbitrary user objects. Document the deviations from full JSR-330 (no circular-dependency resolver, no Scope SPI beyond `@Singleton`).
- **D4 (was Q4).** **Confirmed.** `jakarta.inject.Provider` and `javax.inject.Provider` are interfaces, so `java.lang.reflect.Proxy` is sufficient. Cache the generated proxy class per `Provider` interface; load with the Provider interface's `ClassLoader`.

## Open questions

*(none currently — add new ones here as implementation progresses.)*

---

## Acceptance criteria

- The package Javadoc clearly states **"Juneau supports JSR-330"**, lists the recognized FQNs, and links to the spec.
- Code that needs to find an injection point / qualifier consults a **single utility** (no scattered FQN literals).
- A unit test demonstrates that the same Juneau resource works with **either** `@org.apache.juneau.commons.inject.Inject` **or** `@jakarta.inject.Inject` annotations on its constructor params, when `jakarta.inject-api` is on the test classpath.
- A unit test demonstrates that the project still builds and runs with `jakarta.inject-api` **absent**.
- No new compile-time dependency on `jakarta.inject-api` in `juneau-commons` or any module that did not already depend on it.
