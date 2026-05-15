# FINISHED-31: Inject-aware Microservice

## Summary

`juneau-microservice-core` and `juneau-microservice-jetty` are now first-class consumers of the
inject framework (`BeanStore`, `@Configuration`, `@Bean`, `@PostConstruct`, `@PreDestroy`,
`@Primary`, `@Order`, `@Conditional`). An application or test can bootstrap a microservice from
one or more `@Configuration` classes whose `@Bean` methods produce the resources, servlets,
listeners, and infrastructure the microservice needs.

Existing builder/config-file workflow remains byte-for-byte unchanged.

## What shipped

### Phase 1 — `juneau-microservice-core`

`Microservice` gains an internal `WritableBeanStore` (accessible via
`Microservice.getBeanStore()`). Bootstrap order:

1. `WritableBeanStore` constructed (or taken from builder via new `.beanStore(...)` method).
2. `@Configuration` classes registered via new `.configurations(Class<?>...)` /
   `.configurations(List<Class<?>>)` builder methods are processed first; `@Bean` methods are
   invoked with parameter injection and their results land in the store.
3. Each microservice field (`Args`, `ManifestFile`, `Config`, `VarResolver`, `MicroserviceListener`)
   is resolved with priority **explicit builder > `@Bean` > built-in default**, then registered
   into the store (overwriting any `@Bean` contribution under the same `(type, name)`).
4. The microservice instance itself is self-registered as a bean.
5. Console commands contributed via `@Bean ConsoleCommand` are merged into the console command map
   alongside builder-supplied and `Console/commands` config-file entries.

Lifecycle:

- `Microservice.stop()` closes the bean store, which walks every **resolved** bean in LIFO order
  and invokes `@PreDestroy` methods. Errors are logged at `WARNING` and do not abort the rest of
  the shutdown sequence.
- `@PostConstruct` callbacks fire automatically on beans instantiated through the inject framework
  (e.g. when a `@Bean` factory method depends on another `@Bean`-supplied dependency, or via
  `BeanStore.instantiate(X)`).

New API surface on `Microservice.Builder`:

```java
public Builder configurations(Class<?>... configs)
public Builder configurations(List<Class<?>> configs)
public Builder beanStore(WritableBeanStore externalStore)
```

New API surface on `Microservice`:

```java
public WritableBeanStore getBeanStore()
```

### Phase 2 — `juneau-microservice-jetty`

`JettyMicroservice.Builder` overrides the new builder methods for fluent-typing. The constructor
self-registers `JettyMicroservice`, `JettyMicroserviceListener`, and `JettyServerFactory` into the
store, and looks up `@Bean`-supplied values when the builder doesn't provide them.

`createServer()`:

- If a `@Bean Server` is present in the store (with the `"ServletContextHandler"` attribute set),
  uses it directly and skips the `jetty.xml` factory step entirely. Otherwise, uses the resolved
  `JettyServerFactory` to build the server from `jetty.xml` as before.
- Publishes the created `Server` back to the bean store.
- Iterates `store.getBeansOfType(Servlet.class)` and auto-mounts every servlet whose runtime class
  carries `@Rest`, at the path declared by `@Rest(path = "...")` (defaulting to `/` when no path is
  set).
- Tracks every mounted pathspec with its declaring source (`Jetty/servlets[FQN]`,
  `Builder.servlet(FQN)`, `@Bean FQN[name]`) and throws a `RuntimeException` with both
  contributors on path collision. Previously silent collisions across the five servlet sources are
  now hard startup failures.

### Phase 3 — Docs + release notes

- New topic page `pages/topics/14.09.InjectAwareMicroservice.md` (slug `MicroserviceCoreInject`)
  with a worked example, the resolution-priority table, the auto-discovery semantics, lifecycle
  notes, external-store integration, and back-compat guarantees.
- Sidebar entry added at `14.9. Inject-Aware Microservice`.
- Two new sections in `pages/release-notes/9.5.0.md` (under `### juneau-microservice-core` and
  `### juneau-microservice-jetty`) summarizing the new builder methods, the `getBeanStore()`
  accessor, lifecycle semantics, `@Rest` servlet auto-mount behavior, and the path-collision
  hard-failure invariant.

## Tests

New test classes in `juneau-utest`:

- `Microservice_Inject_Test` (12 tests): bean-store presence and self-registration, external bean
  store, `@Configuration` bean registration, resolution priority (builder > `@Bean`),
  configuration-contributed console commands, `@PreDestroy` lifecycle, legacy builder back-compat.
- `JettyMicroservice_Inject_Test` (9 tests): Jetty-specific self-registration, explicit
  `JettyServerFactory` / `JettyMicroserviceListener` precedence over `@Bean` values, end-to-end
  `@Bean Server` + `@Bean Servlet` auto-mount at `@Rest(path = ...)`, servlets without `@Rest` are
  not auto-mounted, path collisions fail hard.

All 183 existing microservice-related tests (`Microservice_Builder_Test`,
`BasicMicroserviceListener_Test`, `JettyMicroservice_Builder_Test`,
`BasicJettyMicroserviceListener_Test`, `JettyLogger_Test`, `LogConfig_Test`,
`ConsoleCommand_Test`) continue to pass unchanged. Full `./scripts/test.py --full` is green.

## Notable design decisions

1. **Builder values stay authoritative.** `Microservice` does not move its fields into the store
   wholesale. Instead, the constructor resolves each field with `builder > store > default`
   precedence and then writes the final value into the store. This preserves the existing
   "explicit builder call always wins" invariant while still letting `@Bean` methods supply
   defaults when the builder is silent.

2. **`@Bean` return types must match the lookup type.** `BeanStore.getBeansOfType(Class)` is
   exact-type (not assignable-to). The user-facing contract documented in the new topic page:
   declare `@Bean` methods that produce a servlet with return type `Servlet`, not the concrete
   subclass. Spring-compatible "assignable-to" semantics would require an additive change to the
   `BeanStore` API and is intentionally out of scope here; can be revisited if a follow-on need
   emerges.

3. **`@Bean.name()` is not repurposed as a mount path.** `@Bean(name = ...)` already has a
   well-defined meaning (disambiguate multiple beans of the same Java type). The mount path comes
   solely from `@Rest(path = ...)` on the servlet class, with `/` as the fallback. If multi-mount
   ever becomes a real requirement, a dedicated `@Mount("/path")` annotation is the right answer,
   not overloading `@Bean.name()`.

4. **`@PreDestroy` only fires for resolved beans.** This is the existing `BasicBeanStore.close()`
   contract — beans registered but never fetched via `getBean` / `getBeansOfType` are not tracked
   and won't receive a `@PreDestroy` callback. For a microservice this almost never matters in
   practice because servlets, listeners, and console commands are all pulled from the store
   during start-up.

5. **Path-collision is a hard failure, not a warning.** Across all five servlet sources
   (`Jetty/servlets`, `Jetty/servletMap`, `Jetty/servletAttributes` via config, `.servlet(...)`
   builder calls, `@Bean` discovery), `createServer()` tracks the first contributor for each
   normalized pathspec and throws `RuntimeException` with both names if a second contributor
   claims the same path. Previously these silently chained additional `ServletHolder` instances
   onto the same pathspec.

## Files touched

**Core code:**

- `juneau-microservice/juneau-microservice-core/src/main/java/org/apache/juneau/microservice/Microservice.java`
- `juneau-microservice/juneau-microservice-jetty/src/main/java/org/apache/juneau/microservice/jetty/JettyMicroservice.java`

**Tests:**

- `juneau-utest/src/test/java/org/apache/juneau/microservice/Microservice_Inject_Test.java`
- `juneau-utest/src/test/java/org/apache/juneau/microservice/jetty/JettyMicroservice_Inject_Test.java`

**Docs:**

- `juneau-docs/pages/topics/14.09.InjectAwareMicroservice.md`
- `juneau-docs/pages/release-notes/9.5.0.md`
- `juneau-docs/sidebars.ts`

## Downstream unblocked

[TODO-11] — RestClient NG closeout — cross-transport remote-interface test suite. The
`MicroserviceTestFixture` plan in `todo/TODO-11-restclient-ng-coverage-closeout.md` can now build
on top of `Microservice.create().configurations(...)` with a `@Configuration` test class supplying
the `@Rest` servlets.
