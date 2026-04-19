# TODO-23: `org.apache.juneau.commons.inject` — feature roadmap

Source: created 2026-04-19.

## Goal

Grow **`org.apache.juneau.commons.inject`** into a **small, predictable** composition and lookup layer that Juneau (REST, marshall helpers, SVL, etc.) can rely on **without** growing into a general-purpose application container.

**Non-goal:** Replace Spring, Jakarta CDI, Guice, or Micronaut for full application wiring.

**Related:** **TODO-21** — `@Bean` and optional `@Configuration` in this package as lightweight Spring-*shaped* surface for discovery and naming, with clear Juneau semantics.

---

## Current baseline (inventory)

Today the package is primarily the **bean store** stack (e.g. `BeanStore`, `WritableBeanStore`, `CreatableBeanStore`, `BasicBeanStore2`, `BeanCreator2`). Treat this document as a **wishlist / decision log**: each subsection needs a **yes/no** and **scope** before implementation.

---

## Tier A — Likely fits a “simplified inject” story

Ideas that align with **named lookup**, **explicit graphs**, and **framework-owned lifecycles** (already familiar from `BeanStore` + REST resource trees).

| Spring-ish idea | Juneau-shaped question | Notes |
|-----------------|------------------------|--------|
| **`@Configuration`** | Marker type for “hosts `@Bean` methods / fields”? | TODO-21. Define inheritance (subclasses? nested static types?). |
| **`@Bean` (method/field)** | Single contribution point into a store | TODO-21. Mirrors “provider” more than Spring’s factory metadata. |
| **`@Qualifier` / name** | Already partly **name-based** in stores | Formalize: string qualifiers vs annotation qualifiers; collision rules. |
| **`@Primary`** | “Default when multiple candidates match type” | Small win for serializers, `CallLogger`, etc. Must not require full autowire resolution. |
| **`@Order` / priority** | Ordered lists of contributors (filters, guards, parsers) | Could be **integer on `@Bean`** or separate annotation; avoid generic `@Order` unless reused broadly. |
| **Conditional registration** | “Register only if class on classpath / property” | Spring `@ConditionalOn*` is heavy; a **single** `@Conditional(SomePredicate.class)` or name=value guard might suffice. |
| **`@Import` (minimal)** | Pull another configuration type into the same scan | Only if `@Configuration` exists; keep to **type references**, not XML/classpath scanning. |

---

## Tier B — Useful but higher cost; justify per use case

| Spring-ish idea | Risk | Juneau angle |
|-----------------|------|----------------|
| **Constructor / field injection** (`@Autowired`-style) | Competes with existing **explicit** `BeanStore` APIs; reflection-heavy | Prefer **constructor taking `BeanStore`** or **factory interfaces** over field injection unless one narrow entrypoint (e.g. test doubles). |
| **`@Lazy` / lazy proxies** | Proxy machinery, debugging pain | Only if a concrete Juneau feature needs circular breakup; default **eager** in stores. |
| **Scopes: request / thread / child store** | Lifecycle + leak risk | Juneau already thinks in **per-request** stores in REST; formalize as **scope enum** on `@Bean` rather than full `Scope` SPI. |
| **`@PostConstruct` / `@PreDestroy`** | Lifecycle ordering | Small set of hooks on types **registered** in a store, not global container events. |
| **`ObjectProvider`-style lookup** | API surface | `Supplier<T>` / `Optional<T>` from store might be enough. |

---

## Tier C — Explicitly out of scope (for this package)

- **AOP** (aspects, interceptors beyond what REST already defines).
- **Transactions**, **JPA**, **scheduling**, **caching** abstractions.
- **Classpath component scanning** (`@ComponentScan`) across the whole JVM.
- **Property placeholder resolution** as a container feature (keep in **config / SVL** layers unless inject only needs **references** into existing config).
- **Event bus** (`ApplicationEvent`) unless a **single** narrow use appears (e.g. “context closed”); even then prefer callbacks on `BeanStore` / `RestContext`.

---

## Design principles (gating new features)

1. **Store-centric:** new concepts should map cleanly to **put/get/supplier** on `BeanStore` (or documented child-store rules).
2. **No hidden global singleton** for application wiring — framework may hold a **root** store, apps opt in.
3. **Annotation = hint; Class = contract** — discovery uses **annotation type identity**, not simple names (Spring coexistence; see TODO-21).
4. **Prefer composition over graph solvers** — avoid full **autowire-by-type** unless scoped to a **known** set of types (e.g. REST built-ins).
5. **Footprint:** every feature pays for **binary size**, **reflection**, and **documentation**; default answer is “no” until a **second** consumer needs it.

---

## Suggested sequencing

1. **Land TODO-15 / TODO-16** stability on `BeanStore` naming and REST builder shrinkage so new annotations sit on stable types.
2. **TODO-21** — `@Bean` + semantics doc + REST migration off `@RestInject`.
3. **Decide `@Configuration`** (yes/no + minimal rules).
4. **Pick one Tier A follow-on** (e.g. `@Primary` *or* `@Order`, not both at once) driven by a **concrete REST or marshall** pain point.
5. Revisit Tier B only after Tier A is in use and gaps are **observed**, not anticipated.

---

## Deliverables for this TODO

- [ ] Short **“Inject package charter”** (could live as a package-level `package-info.java` + one docs topic): goals, non-goals, relationship to Spring.
- [ ] For each candidate feature: **owner decision** (in / out / later), **one paragraph** semantics, **link to consumer** (REST, tests, examples).
- [ ] Align **TODO-17** migration guide when any public annotation or type lands.
