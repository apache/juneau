# FINISHED-24: JSR-330 alignment + Spring-lite extensions for `commons.inject` (complete)

Archived from `TODO-24-jsr330-and-spring-lite-support.md`. TODO-23 (the `commons.inject` framework
roadmap) was folded into this work and is also closed.

Delivered in **9.5.0**. See:

- `juneau-docs/pages/release-notes/9.5.0.md` — "JSR-330 Alignment + Spring-Lite Additions in
  `commons.inject` (TODO-24)" section.
- `juneau-docs/pages/topics/06.02.07.JuneauCommonsInject.md` — dedicated topic.
- `juneau-docs/pages/topics/23.01.V9.5-migration-guide.md` — migration rows.

---

## Outcome (what shipped)

`org.apache.juneau.commons.inject` now presents a single coherent injection story:

> Juneau supports JSR-330 (`jakarta.inject` / `javax.inject`) annotations as closely as our lightweight
> model allows, without taking a hard dependency on `jakarta.inject-api`. Where Spring conventions add
> value to the lightweight model, we adopt them under our own annotations.

All recognition is FQN-based via a single `JsrSupport` utility — no compile-time dependency on
`jakarta.inject-api` or `jakarta.annotation-api`.

### Annotations and types added

| Type | Notes |
|------|-------|
| `Inject`, `Named`, `Qualifier` (meta), `Singleton`, `Provider<T>` | JSR-330 base. JSR-330 / Spring FQNs recognized via `JsrSupport`. |
| `PostConstruct`, `PreDestroy` | JSR-250 lifecycle. JSR-250 FQNs (`jakarta.annotation` / `javax.annotation`) recognized via `JsrSupport`. |
| `Primary`, `Order(int)` | Spring-lite. `@Bean#priority()` added as a fallback ordering knob; `@Order` wins when both are present. |
| `Configuration`, with `imports = { ... }` | Spring-lite. Subclasses inherit `@Bean` members (parent-first registration); nested static configurations are NOT auto-scanned. Duplicate `(type, name)` registration throws `BeanCreationException`. |
| `Conditional(Class<? extends Condition>)`, `ConditionalOnClass`, `ConditionalOnMissingBean`, `ConditionalOnProperty` | Class-level skip cascades; member-level skips one bean. Evaluated eagerly at registration time. Silent debug-log skip on failure. |
| `Condition`, `ConditionContext` | SPI for custom predicates with access to bean store, settings, classloader, annotated element. |
| `JsrSupport` | Single source of FQN constants and matcher methods. |

### Behaviour changes / wiring

- `BeanInstantiator`, `ParameterInfo`, `FieldInfo`, `ClassInfo` switched from simple-name to FQN-based
  recognition for `@Inject`, `@Named`, `@Qualifier`, `@PostConstruct`.
- `Provider<T>` parameter resolution: Juneau-owned returns a direct `Provider`; `jakarta` / `javax`
  flavours are resolved via `java.lang.reflect.Proxy` using the provider interface's `ClassLoader`.
- `BasicBeanStore` honours `@Primary` on local entries for unqualified `getBean(type)` lookups; throws
  on multiple primaries; tracks `@Order` / `priority` via `BeanSourceMeta` for `getBeansOfType`.
- `WritableBeanStore extends AutoCloseable`. `close()` fires `@PreDestroy` in LIFO order on resolved
  beans, aggregates failures as suppressed exceptions, and rejects further operations.
- `WritableBeanStore.registerConfiguration(Class)` / `registerConfigurations(Class...)` register
  `@Configuration` classes, recursively process `imports` (deduped per bean store), and honour
  `@Conditional` at both class and member level.
- `RestContext` calls `ClassInfo.inject(resource, beanStore)` after `@Bean` field back-fill so that
  `@Inject` (Juneau / `jakarta` / `javax` / Spring `@Autowired`) on `@Rest` resources is populated, and
  `@PostConstruct` fires after injection.
- `RestContext.destroy()` calls `beanStore.close()` after `@RestDestroy` hooks.

### Constructor visibility widening (post-merge follow-up)

`BeanInstantiator` now falls back to package-private constructors after public and protected — both
for the bean type and for builder discovery. Private constructors remain excluded (they signal
"do not instantiate"). This lets `@Configuration` classes and other framework-instantiated types stay
package-scoped without exposing a `public` constructor purely to satisfy the bean store. Documented
in the release notes and `06.02.07.JuneauCommonsInject.md`.

### `@Named` relocation

`org.apache.juneau.annotation.Named` was moved to `org.apache.juneau.commons.inject.Named`. No
deprecated alias is kept at the old location; references were updated across the codebase. Migration
row added to the v9.5 migration guide.

## Coverage delivered

- `Configuration_Test` — static/instance field + method beans, imports recursion + dedup, superclass
  inheritance, named beans, duplicate-bean error, non-`@Configuration` rejection. Fixtures use
  package-private classes and constructors to also regression-test the constructor-visibility fallback.
- `Conditional_Test` — class-level cascade skip, importer chain skip behaviour, member-level skip,
  `@ConditionalOnClass`, `@ConditionalOnMissingBean`, `@ConditionalOnProperty` (matchIfMissing path
  too), custom `@Conditional` with `ConditionContext` access.
- `Lifecycle_Test` — `@PostConstruct` on instantiation, `@PreDestroy` LIFO order, idempotent close,
  closed-store rejection, suppressed-exception aggregation, unresolved-bean skip.
- `PrimaryAndOrder_Test` — single primary wins, multiple primaries throw, no-primary unnamed lookup
  empty, `@Order` ascending sort, `priority` ascending sort, `@Order` beats `priority`.
- `RestInjectAnnotation_Test` — `@Inject` populates `@Rest` resource fields and methods using Juneau,
  `jakarta.inject.Inject`, and Spring `@Autowired` FQN paths; `@PostConstruct` fires after injection.
- `BeanInstantiator_Test.a10`–`a13` — package-private constructor with and without dependencies,
  public-over-package precedence, and the private-constructor exclusion.
- Synthetic FQN stubs in test classpath: `org.springframework.beans.factory.annotation.Autowired`,
  `jakarta.inject.Inject` (kept tiny and test-only).
- Whole-tree `mvn -pl juneau-utest test` clean — 50,025+ tests, 0 failures.

## Decisions locked in

- **Move** `org.apache.juneau.annotation.Named` to `org.apache.juneau.commons.inject.Named`. No
  deprecated alias.
- Expose `@Inject` as a user-facing annotation on `@Rest` resources (and other framework-instantiated
  types). Do not introduce a global injector for arbitrary user objects.
- `jakarta.inject.Provider` / `javax.inject.Provider` are interfaces → `java.lang.reflect.Proxy` is
  sufficient.
- `@Configuration`: classes only; nested static types not auto-scanned; superclasses inherited;
  duplicate `(type, name)` is a hard error; class-level conditions cascade to imports.
- `@Conditional` family is small and explicit: generic `@Conditional`, `@ConditionalOnClass`,
  `@ConditionalOnMissingBean`, `@ConditionalOnProperty`. Eager evaluation at registration time, silent
  skip with debug log on failure.
- Both `@Order(int)` annotation and `priority()` attribute on `@Bean` are supported; `@Order` wins.
- `WritableBeanStore extends AutoCloseable`; `RestContext.destroy()` delegates to `beanStore.close()`.
- `BeanInstantiator` ladder: public → protected → package-private; private is excluded.
