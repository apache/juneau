# TODO-35 — JUnit 5 test-time injection of mocks/fakes into BeanStore

## Goal

Provide a small, idiomatic mechanism for swapping production beans with test
doubles (mocks/fakes) inside a Juneau `BeanStore` for the duration of a JUnit 5
test, without modifying production code and without proxying.

The target ergonomic is "Spring `@TestConfiguration` + `@MockBean`, but
Juneau-flavored": declare a `@TestBean`-annotated field or static factory method
on the test class, register a JUnit 5 extension, point it at the
`RestContext`/`Microservice`/`SerializerSet`/etc. under test, and let the
extension overlay the bean store for the duration of the test.

The v1 scope, finalized from user review, includes:

- A new **`juneau-junit5`** module hosting the overlay primitive, the
  `@TestBean` annotation, and the JUnit 5 `Extension` that wires it.
- **Two supported wiring patterns**: **Mode INJECT** (fresh-instance with
  overrides — overlay is installed into the SUT's bean-store chain before
  the SUT is built) and **Mode OVERLAY** (existing-instance push/pop — overlay is
  pushed onto a running SUT's bean store at `@BeforeEach` and popped at
  `@AfterEach`).
- A **`@TestBean(name = "...")`** qualifier so named beans can be targeted
  with the same precision the framework already supports for `@Bean`.
- **Class-scope overlays** via `@TestBean(scope = CLASS)` (or a sibling
  annotation), driven by `BeforeAllCallback` / `AfterAllCallback`.
- **Non-REST builder hooks** so the same overlay mechanism works against
  `Microservice.Builder`, `SerializerSet.Builder`, `ParserSet.Builder`, and
  `EncoderSet.Builder` — not just `RestContext` / `MockRestClient`.
- **Framework extensions are in scope**: where the v1 features need a
  cleaner `BeanStore` or `Microservice` primitive than what exists today,
  v1 will add that primitive. The plan calls these out per phase.

The user constraint remains "keep it simple": no CGLIB, no AOT bytecode
generation, no autowiring magic, no `@SpyBean` in v1, no Mockito dependency.

## Reference patterns

- **Spring `@TestConfiguration` + `@MockBean` / `@SpyBean`** — replaces beans
  in the application context at test-class level; uses CGLIB proxies for
  `@SpyBean`. Not applicable for the proxy bits, but the annotation surface
  is the model.
- **Guice `Modules.override(production).with(testModule)`** — explicit
  override layering, no proxying. **This is the closest analog to what
  Juneau can do with its existing `overridingParent` BeanStore slot.**
- **Dagger Android `TestComponent`** — compile-time swap of the entire DI
  graph. Out of scope: requires a parallel component definition per test.
- **Quarkus `@InjectMock`** — build-time bytecode generation. Not relevant
  to Juneau (no AOT).
- **Micronaut `@MockBean`** — compile-time AOT. Same — not relevant.

The plan below adopts Guice-style explicit override layering, exposed through
a JUnit 5 extension and a `@TestBean` annotation.

## Constraints

- **No CGLIB / no dynamic proxies.** `BeanStore` returns plain instances. To
  replace a bean, an alternative instance must be registered before the
  consumer resolves it. Replacing references after the consumer has cached
  the bean is explicitly out of scope.
- **No AOT bytecode generation.** Everything must be runtime-reflective.
- **No Mockito dependency.** `juneau-junit5` depends only on
  `junit-jupiter-api`. Tests are free to use Mockito to construct their
  doubles, but the extension itself must not import or reflectively touch
  any Mockito class.
- **BeanStore is already hierarchical.** `BasicBeanStore` supports `parent`
  and `overridingParent`. Resolution order (top wins):
  1. `overridingParent` (if non-null)
  2. local entries (via `addBean` / `addSupplier`)
  3. `parent` (if non-null)
  4. local default suppliers (`addDefaultSupplier`)

  This means the cleanest test-injection point is **the `overridingParent`
  slot** — it already exists, is honored framework-wide, and trumps
  everything the resource configured locally.
- **`RestContext.getBeanStore()` is exposed** as a `WritableBeanStore`, so
  test code can address the bean store of a running resource. However the
  current `overridingParent` slot is fixed at `BasicBeanStore` construction
  time, so the canonical Mode-INJECT wiring installs the overlay **before**
  `RestContext` / `Microservice` / `SerializerSet` is built. Mode OVERLAY uses
  the new stack overlay API (see Phase 6 below) to push/pop onto a
  running instance.

## BeanStore primitives leveraged

The whole design rests on three properties of the existing `BasicBeanStore`
that the parent-agent investigation confirmed. Implementers should treat this
subsection as the "if you change this, change the plan too" contract.

### Supplier-backed storage

All beans live in `entries: ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Supplier<?>>>`
(`BasicBeanStore.java` line 98). `addBean(beanType, bean)` is a thin wrapper
that forwards to `addSupplier(beanType, () -> bean, name)` (lines 222–224 →
257–264). Net effect: **every bean is supplier-backed**, regardless of
whether the user registered an instance or a supplier. There is no
"instance-only" code path that would be missed by a supplier-based override.

```text
addBean(MyService.class, svc)                  // user-facing API
  └─ addSupplier(MyService.class, () -> svc)   // actual storage
       └─ entries[MyService.class][""] = supplier
```

### Resolution chain

`resolve(beanType, name)` in `BasicBeanStore.java` (lines 550–580) consults
sources in priority order:

1. `overridingParent.getBeanSupplier(...)` — if non-null. Currently a
   set-once slot fixed at construction.
2. Local `entries` — what `addBean` / `addSupplier` populated.
3. `parent.getBeanSupplier(...)` — if non-null. Hierarchical fallback.
4. Local `defaults` — memoizer-backed framework defaults registered via
   `addDefaultSupplier` (line 99 holds the map; line 282 the API).

The overlay design plugs into tier 1 (Mode INJECT) or sits on a stack above tier 1
(Mode OVERLAY + class scope). It never mutates tier 2 (local `entries`) and never
touches tier 4 (the memoizer).

### Snapshot/restore primitives

For the Mode OVERLAY fallback path (when an overlay-stack hook can't be installed
at SUT-construction time):

- `getBeanSupplier(Class)` — returns the raw `Supplier` resolved through the
  chain (lines 531–534, 593–597).
- `getDefaultSupplier(Class)` / `getDefaultSupplier(Class, name)` — returns
  the locally-registered default supplier without consulting parents
  (lines 447–472). Useful when the test needs to put back a memoizer-backed
  default after temporarily replacing it.
- `addSupplier(beanType, supplier, name)` — does a direct
  `typeMap.put(key, supplier)` (line 261). Overwriting is trivially
  supported; replacing supplier `A` with supplier `B` then re-replacing
  with supplier `A` restores the original behavior fully.

These three primitives are what makes Mode OVERLAY's snapshot-and-restore semantics
implementable even without the new stack-overlay API. The stack-overlay API
(Phase 6) is preferred; these primitives are the fallback when the stack
can't be installed pre-construction.

## Framework extensions (in v1 scope)

The original framing of this work assumed the implementation would only
**leverage existing** primitives. The user has since confirmed that
**extending the framework surface is also in scope** for v1 where it makes
the design cleaner.

> "extending the functionality of BeanStore is also feasible here"
> — user direction

> "Same is true for Microservice as well"
> — user direction

This section enumerates the candidate extensions. Each carries a
ship/defer recommendation so implementers know which extensions are blocking
v1 vs. nice-to-have.

### BeanStore extensions

1. **Overlay-stack API on `WritableBeanStore`.** Adds
   `pushOverlay(BeanStore)` / `popOverlay(Token)` (or
   `popOverlay(BeanStore)`) returning a stack-aware override target. The
   stack lives in the `overridingParent` slot via composition — a new
   internal `StackOverlay` class wraps a `Deque<BeanStore>` and is itself a
   `BeanStore`. **Recommendation: ship.** Replaces the set-once
   `overridingParent` constraint that today blocks class-scope and
   Mode OVERLAY from being clean. Touches `BasicBeanStore.java`,
   `WritableBeanStore.java`, and adds a new
   `org.apache.juneau.commons.inject.StackOverlay` class (package-private
   if possible; public if the JUnit extension needs to construct one
   directly).

2. **`Snapshot` value type + `snapshot()` / `restore(Snapshot)` on `WritableBeanStore`.**
   First-class API for Mode OVERLAY push/pop. A `Snapshot` is an opaque record
   capturing the supplier state for some `(beanType, name)` set at a moment
   in time; `restore(Snapshot)` puts those suppliers back. **Recommendation:
   ship, but only if the overlay-stack API alone proves insufficient.** In
   the recommended design Mode OVERLAY is implemented via the overlay stack, so a
   `Snapshot` type is mostly defensive — useful when the test can't install
   the stack pre-construction. Treat as a Phase 6 stretch goal.

3. **`BeanStoreOverridable` consistency convention across non-REST builders.**
   A common contract that `SerializerSet.Builder`, `ParserSet.Builder`,
   `EncoderSet.Builder`, and `Microservice.Builder` all honor:

   ```java
   public interface BeanStoreOverridable<B> {
       B overridingBeanStore(BeanStore overlay);
   }
   ```

   **Recommendation: ship the method-signature convention.** Whether to
   formalize it as a marker interface or leave it as a documented naming
   convention is a design call — both are listed in the open questions. The
   JUnit extension can wire by interface (cleaner) or by reflective probing
   of the method name (works without a marker interface). The marker
   interface adds one line per builder and lets the extension's wiring code
   be a single `instanceof` check rather than a list of `instanceof` per
   builder type.

4. **Optional: `WritableBeanStore.put(beanType, supplier)` returning the previous supplier.**
   Convenience for snapshot-and-restore. **Recommendation: defer.** The
   existing `getBeanSupplier(...) + addSupplier(...)` pair already covers
   this use case without adding new surface; the put-returns-previous
   variant is only needed if `Snapshot` ends up needing it, which is not
   the case in the recommended design.

### Microservice extensions

1. **Singleton overlay-install hook.** Adds `installBeanStoreOverlay(BeanStore overlay)`
   and `removeBeanStoreOverlay(BeanStore overlay)` on `Microservice` —
   wrappers that delegate to the underlying `WritableBeanStore`'s
   `pushOverlay` / `popOverlay`. **Recommendation: ship.** The wrappers
   centralize the "is the microservice in a state where overlays can be
   pushed" check (e.g. refuse to push after `stop()` fires `@PreDestroy`).
   Touches `Microservice.java`. Trivial implementation once the
   BeanStore overlay-stack lands.

2. **Multi-instance registry.** Adds a static
   `Collection<Microservice> getInstances()` and
   `Microservice getInstance(Class<? extends Microservice>)`. Today only
   `getInstance()` (last-instance-wins via `AtomicReference<Microservice>`)
   is exposed (`Microservice.java` lines 587, 607). **Recommendation:
   ship only if a real test case during Phase 5 demands class-based
   discrimination.** The change is small (a static
   `CopyOnWriteArrayList<Microservice>` populated in `start()` / drained
   in `stop()`) but does change the contract about what `getInstance()`
   returns in a multi-microservice JVM. Defer to v2 if the singleton
   covers Phase 5's test cases.

3. **Lifecycle state accessor.** Adds a `Microservice.State` enum
   (`UNSTARTED`, `STARTING`, `RUNNING`, `STOPPING`, `STOPPED`) and a
   `state()` accessor. Today only `volatile boolean stopped`
   (`Microservice.java` line 630) and a `synchronized start()` (line 1204)
   exist. **Recommendation: ship.** The test extension uses
   `state() == RUNNING` to refuse an overlay-pop on a microservice that's
   already shut down (which today would silently leak into the bean
   store's closed state). The enum is also useful to user code beyond
   tests — it's a small additive change with low blast radius.

4. **Builder `overridingBeanStore(...)` hook.** Adds
   `Microservice.Builder.overridingBeanStore(BeanStore)` per the
   `BeanStoreOverridable` convention above. **Recommendation: ship.**
   Today `Microservice.Builder` has `beanStore(WritableBeanStore)` (line
   564) which replaces the entire bean store — useful in a few cases but
   too coarse for the overlay pattern. The new method threads the overlay
   into the `overridingParent` slot of the bean store the microservice
   constructs internally (`Microservice.java` line 656).

### Files the framework extensions touch

- `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/inject/BasicBeanStore.java`
- `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/inject/WritableBeanStore.java`
- `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/inject/StackOverlay.java` (new)
- `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/inject/BeanStoreOverridable.java` (new, optional marker interface)
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/serializer/SerializerSet.java`
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/parser/ParserSet.java`
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/encoders/EncoderSet.java`
- `juneau-microservice/juneau-microservice/src/main/java/org/apache/juneau/microservice/Microservice.java`
- `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/RestContext.java` (extend `Args` record with optional `overridingParent` slot; thread through `createBeanStore(...)` at line 368)
- `juneau-rest/juneau-rest-mock/src/main/java/org/apache/juneau/rest/mock/MockRestClient.java` (Builder add `overridingBeanStore(...)`)
- `juneau-rest/juneau-rest-mock/src/main/java/org/apache/juneau/rest/mock/classic/MockRestClient.java` (same)

## Current state

- **No `@TestBean` / `@MockBean` / `@TestConfiguration` analog exists today.**
  Searching `juneau-utest` and `juneau-rest/*/src/test` finds no
  test-injection annotation in use.
- **Today's workaround pattern.** When a test needs to override a framework
  bean, the resource class itself declares a `@Bean` factory method that
  returns the test double, or the test subclasses the resource. Both couple
  the test double into the resource source, which is exactly what the user
  wants to avoid. Example from
  `juneau-utest/src/test/java/org/apache/juneau/rest/RestContext_Precedence_Test.java`:

  ```java
  @Rest
  public static class B_BeanBeatsSpring {
      @Bean public WritableBeanStore createBeanStore() { return springLikeBeanStore(); }
      @Bean public CallLogger callLogger() { return BEAN_LOGGER; }
  }
  ```

  Several tests in that file build a parallel `SpringLikeBeanStore` by hand
  and thread it in via the `@Bean WritableBeanStore createBeanStore()`
  factory hook on a synthetic inner-class resource. That works for the
  framework's own precedence tests but is heavy ceremony for application
  tests.
- **`MockRestClient.Builder` already accepts a per-test `Consumer<WritableBeanStore>`**
  via `RestContext.Args.beanStoreConfigurer` (`RestContext.java` lines 197,
  208, 219; invoked at line 1405). This is the closest existing hook to
  what we're building — it lets a test register beans into the live store
  before any memoizer fires. Phase 1 builds on this by adding a sibling
  `overridingParent` slot on `Args` so test-supplied beans land in tier 1
  of the resolution chain rather than tier 2.
- **The pieces we need are mostly in place.** `BasicBeanStore` already
  honors `overridingParent`. The gaps are (a) an ergonomic builder for the
  overlay, (b) a way to bind it into a `RestContext` / `Microservice` /
  serializer set from a test, (c) a JUnit 5 extension to manage lifecycle,
  and (d) — for Mode OVERLAY — the small new stack-overlay API described in the
  Framework extensions section.

## Recommended design

**Combine Options C (overlay BeanStore) + D (JUnit 5 extension)** from the
original design brief, plus a new stack overlay primitive to support class
scope and Mode OVERLAY. Reject Option A (`Supplier<T>`-only registrations in
production) as already solved — every bean is supplier-backed today (see
BeanStore primitives subsection). Reject "in-place mutate-the-production-store"
as the canonical Mode OVERLAY path; the overlay stack is cleaner. The in-place
`getBeanSupplier`/`addSupplier` choreography is documented as a fallback
when the overlay stack cannot be installed pre-construction.

### Phase 1 — `TestBeanStore` overlay class + REST builder hook

- **Module:** new module **`juneau-junit5`** under `juneau-core/`. Sibling
  of `juneau-bct` and `juneau-assertions`. `juneau-bct` is "Bean-Centric
  Testing" (a different concept — assertion DSL); test-injection is a
  distinct capability and deserves its own artifact so downstream consumers
  can pull it in without dragging in BCT.

  Justification for a new module rather than putting it in `juneau-bct`:
  consumers of `juneau-bct` today are using its assertion DSL and don't
  expect a JUnit-extension dependency to leak through. Keeping them
  separate also lets `juneau-junit5` depend on `juneau-bct` later if
  desired.

- **Class:**
  `org.apache.juneau.junit5.TestBeanStore extends BasicBeanStore`. Public.
  Thin fluent wrapper.

  ```java
  public class TestBeanStore extends BasicBeanStore {
      public TestBeanStore() { super(); }
      public <T> TestBeanStore override(Class<T> type, T bean)              { addBean(type, bean); return this; }
      public <T> TestBeanStore override(Class<T> type, T bean, String name) { addBean(type, bean, name); return this; }
      public <T> TestBeanStore override(Class<T> type, Supplier<T> s)       { addSupplier(type, s); return this; }
      public <T> TestBeanStore override(Class<T> type, Supplier<T> s, String name) { addSupplier(type, s, name); return this; }
  }
  ```

  No JUnit 5 dependency in the class itself — it's a pure overlay-builder.
  Tests outside JUnit (TestNG, raw `main`) can use it directly.

- **REST wiring — extend `RestContext.Args`.** Today `Args` carries
  `Consumer<WritableBeanStore> beanStoreConfigurer` (`RestContext.java`
  lines 202–223). Add a sibling slot for the overriding-parent overlay:

  ```java
  public static record Args(
      Class<?> resourceClass,
      RestContext parentContext,
      ServletConfig servletConfig,
      Supplier<?> resource,
      String path,
      Consumer<WritableBeanStore> beanStoreConfigurer,
      BeanStore overridingParent
  ) { ... }
  ```

  Thread the new field through `createBeanStore(BeanStore parentBs, Supplier<?> resource)`
  at line 368 so it becomes `createBeanStore(parentBs, resource, args.overridingParent())`,
  and the eventual `new BasicBeanStore(parentBs, args.overridingParent())`
  installs the overlay into tier 1 of the resolution chain.

- **`MockRestClient.Builder.overridingBeanStore(BeanStore)`** that wraps
  the supplied overlay into the `Args.overridingParent` slot when
  constructing the `RestContext`. Single new line in the builder, one
  threading line where `new RestContext.Args(...)` is constructed
  (`MockRestClient.java` line 220 in the `juneau-rest-mock/src/.../MockRestClient.java`).

  ```java
  MockRestClient.create(MyResource.class)
      .overridingBeanStore(testBeanStore)
      .build();
  ```

- **Tests:** `TestBeanStore_Test` in `juneau-utest` covering
  override + restore semantics, named beans, supplier overrides, and parent
  precedence (an entry in the overlay must shadow the production resource's
  `@Bean` factory method since `overridingParent` beats local entries).

### Phase 2 — `@TestBean` annotation + `JuneauBeanStoreExtension`

Builds on Phase 1. Adds the JUnit 5 surface.

- **Annotation:** `org.apache.juneau.junit5.TestBean`. Includes the `name`
  qualifier in v1 (user-confirmed reversal from the draft):

  ```java
  @Retention(RUNTIME)
  @Target({ FIELD, METHOD })
  public @interface TestBean {
      /** Bean name for named-bean overrides. Empty (default) = unnamed. */
      String name() default "";

      /** Bean type override. Empty (default) = use the annotated member's declared/return type. */
      Class<?> type() default Object.class;

      /** Overlay scope. METHOD (default) = per-test; CLASS = per-test-class. */
      Scope scope() default Scope.METHOD;

      enum Scope { METHOD, CLASS }
  }
  ```

  The `type()` member lets the test author declare a `Supplier`-returning
  factory whose return type is `Object` but whose intended target type is
  `MyService`. Cleaner than parsing the supplier's generic type from
  reflection.

  Applies to either:
  - **Instance / static fields** — value is used directly as the override.
  - **Static factory methods** — invoked once per test (or per class, if
    `scope = CLASS`) to produce the override instance. Method's return
    type is the override target type unless `type()` is set explicitly.

- **Extension:**
  `org.apache.juneau.junit5.JuneauBeanStoreExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback, ParameterResolver`.

  Behavior:
  - **`beforeAll`**: scan the test class for static `@TestBean(scope = CLASS)`
    fields and methods. Build a class-scope `TestBeanStore`. Stash in
    `ExtensionContext.Store` keyed `(extensionClass, "class-scope")`.
  - **`beforeEach`**: scan the test instance for `@TestBean(scope = METHOD)`
    fields and methods (the default). Build a per-test `TestBeanStore`,
    chained on top of the class-scope store if one exists. Stash in
    `ExtensionContext.Store` keyed `(extensionClass, "method-scope")`.
  - **`afterEach`**: drop the per-test overlay.
  - **`afterAll`**: drop the class-scope overlay.
  - **`ParameterResolver`**: test methods may declare a `TestBeanStore`
    parameter and receive the current effective overlay (method-scope
    if present, otherwise class-scope).

- **Registration:** two supported forms.
  - **Declarative**: `@ExtendWith(JuneauBeanStoreExtension.class)` on the
    test class.
  - **Programmatic**: a `@RegisterExtension` field that exposes the
    overlay directly:

    ```java
    @RegisterExtension
    JuneauBeanStoreExtension ext = JuneauBeanStoreExtension.create();
    ```

  Programmatic form lets the test grab the overlay before building the SUT:

  ```java
  var client = MockRestClient.create(MyResource.class)
      .overridingBeanStore(ext.getStore())
      .build();
  ```

- **Binding to the SUT — explicit, not magic.** The extension does **not**
  auto-discover a `RestContext` / `Microservice` field on the test and
  does **not** mutate it reflectively. The test author is responsible for
  one line: `.overridingBeanStore(ext.getStore())`. Rationale: the user
  explicitly said "don't overcomplicate" — auto-discovery is the kind of
  feature that pulls in conditional logic, edge cases, and surprises. Make
  the wiring explicit.

### Phase 3 — Class-level overlays

Adds the `Scope.CLASS` lifecycle the annotation declares. The extension's
`BeforeAllCallback` / `AfterAllCallback` are what wire it; the underlying
mechanics depend on the **overlay-stack API** from the Framework extensions
section.

- The class-scope overlay is pushed onto the SUT's bean-store stack at
  `@BeforeAll`. Per-test overlays are pushed on top of it at `@BeforeEach`
  and popped at `@AfterEach`. The class-scope overlay is popped at
  `@AfterAll`.
- **Stack invariant**: per-test push must come **after** the class-scope
  push, and per-test pop must come **before** the class-scope pop. JUnit's
  callback lifecycle already guarantees this nesting; the test should
  never observe a partial state.
- The extension exposes both stores via the `ParameterResolver`:
  - A `TestBeanStore` parameter resolves to the effective top-of-stack
    overlay (method-scope if present, else class-scope).
  - A parameter qualified with a custom annotation like `@ClassScope` can
    resolve to the class-scope store specifically. Open question (low
    priority): is this annotation needed in v1, or can the test just call
    `ext.getClassScopeStore()`? **Recommendation: defer the annotation;
    expose accessors on the extension instead.**
- **Failure modes to document:**
  - If a `@TestBean(scope = CLASS)` field is non-`static`, the extension
    must throw a `JUnitException` in `beforeAll` — instance fields don't
    exist yet at that point. The error message must name the offending
    field.
  - If a `@TestBean(scope = METHOD)` is declared on a static field, the
    extension SHOULD warn but not fail (the value is still resolvable).
    **Recommendation: do not warn; behave consistently with non-static.**

### Phase 4 — Non-REST builder hooks

Adds the same `.overridingBeanStore(...)` ergonomic to the non-REST builders
the user named. Each follows the `BeanStoreOverridable<B>` convention from
the Framework extensions section.

- **`SerializerSet.Builder.overridingBeanStore(BeanStore)`** — wraps the
  current `beanStore` field (`SerializerSet.java` line 103) with
  `new BasicBeanStore(currentBeanStore, overlay)` so `beanStore()`
  (line 150) returns the wrapped chain. Test impact: any
  `bs.createBeanFromMethod(...)` call inside the set's instantiation logic
  resolves against the overlay first.
- **`ParserSet.Builder.overridingBeanStore(BeanStore)`** — same pattern
  (`ParserSet.java` lines 103, 150).
- **`EncoderSet.Builder.overridingBeanStore(BeanStore)`** — same pattern
  (`EncoderSet.java` lines 92, 125).
- **`Microservice.Builder.overridingBeanStore(BeanStore)`** —
  `Microservice.java` line 564 today has `beanStore(WritableBeanStore)`;
  the new method threads the overlay into the `overridingParent` slot when
  the internal bean store is constructed at line 656. Specifically: when
  `builder.beanStore == null`, replace `new BasicBeanStore()` with
  `new BasicBeanStore(null, builder.overridingBeanStore)`. When
  `builder.beanStore != null`, the user has supplied a complete store and
  the overlay is wrapped onto it via the new stack-overlay API.

For all four builders the method signature matches:

```java
public Builder overridingBeanStore(BeanStore overlay) { ... }
```

Whether to also formalize this as a `BeanStoreOverridable<B>` interface is
called out in the open questions. The recommendation is to ship the
interface (one line per builder) so the JUnit extension can wire to any
builder via a single `instanceof` check, rather than four
`instanceof` branches.

### Phase 5 — Mode INJECT: fresh-instance with overrides

Mode INJECT is the **default** wiring pattern. The test builds a fresh SUT in
`@BeforeEach` (or `@BeforeAll` for class-scope sharing) and passes the
overlay into the SUT's builder. The overlay is installed into the bean
store's tier-1 `overridingParent` slot at construction time.

- **Implementation:** uses the builder hook from Phase 1 (REST) and
  Phase 4 (non-REST). No new framework code beyond what those phases add.
- **Semantics:** every bean lookup against the SUT's bean store consults
  the overlay first. Because the overlay is installed before any memoizer
  fires, even framework-managed beans (`CallLogger`, `SerializerSet`,
  `ParserSet`, `EncoderSet`, etc.) see the overlay value when first
  resolved — i.e. Mode INJECT is universal across all bean types.
- **Lifecycle:** the overlay's lifetime is bounded by the SUT's lifetime.
  When the SUT is GC'd (per-test fresh SUT) or shut down (per-class
  shared SUT with explicit teardown), the overlay goes with it. No
  explicit pop needed.
- **Tests:**
  - `MockRestClient_Mode_INJECT_Test` — overlay shadows a resource's `@Bean`
    factory for a framework type (`CallLogger`).
  - `Microservice_Mode_INJECT_Test` — fresh microservice with mocked
    `Config` and a mock `MicroserviceListener`.
  - `SerializerSet_Mode_INJECT_Test` — fresh `SerializerSet.Builder` with an
    overlaid bean that one of the registered `Serializer` types
    resolves at instantiation.

### Phase 6 — Mode OVERLAY: existing-instance with push/pop

Mode OVERLAY is opt-in. The test wires an overlay onto a **running** SUT for the
duration of the test and then drops it. The mechanism uses the
overlay-stack API from the Framework extensions section.

- **Builder flag (opt-in path)** — when the test constructs the SUT in
  `@BeforeAll` (Mode INJECT) and wants per-test overlays via Mode OVERLAY, the
  `JuneauBeanStoreExtension` is configured with
  `.mode(Mode.EXISTING_INSTANCE)` or the annotation declares
  `@TestBean(mode = LIVE)`. Either form tells the extension to skip
  builder-hook wiring and instead call `installBeanStoreOverlay(...)` on
  the running SUT at `@BeforeEach` and `removeBeanStoreOverlay(...)` at
  `@AfterEach`.
- **Canonical implementation: overlay-stack push/pop.** When the SUT was
  built with a `StackOverlay` in its `overridingParent` slot (e.g. when
  the test extension pre-installed one at SUT-construction time), the
  push/pop is a one-liner per layer. The test extension calls
  `store.pushOverlay(perTestOverlay)` in `beforeEach` and
  `store.popOverlay(perTestOverlay)` in `afterEach`. Stack semantics mean
  multiple tests sharing one SUT don't interfere.
- **Fallback implementation: snapshot-and-restore on `entries`.** When
  the SUT was built without a `StackOverlay` (e.g. the test is targeting
  a microservice booted by third-party code that doesn't know about the
  test extension), the extension falls back to:
  1. Snapshot each `(beanType, name)` it's about to override by calling
     `getBeanSupplier(beanType, name)` (or `getDefaultSupplier(beanType, name)`
     if the only registration is a default).
  2. Call `addSupplier(beanType, testDoubleSupplier, name)` to put the
     override in place.
  3. On `afterEach`, call `addSupplier(beanType, snapshottedSupplier, name)`
     to restore the original supplier. If no supplier existed previously,
     the extension cannot fully remove the entry (BasicBeanStore has no
     remove API) — document this as a known limitation and recommend
     Mode INJECT for that case.

#### Mode-OVERLAY-safe vs Mode-INJECT-only — bean type inventory

The audit below enumerates which bean types are eligible for Mode OVERLAY vs
which require Mode INJECT. The audit follows the rule **"resolved per-call from
the bean store" = Mode-OVERLAY-safe; "resolved at boot and pinned to a final
field or memoizer" = Mode-INJECT-only.**

##### Mode-OVERLAY-safe (per-call resolution from the bean store)

- **User-defined beans** registered via `@Bean` factory methods or
  programmatic `addBean(...)` on the resource — stored in `entries`,
  re-resolved on each `getBean(...)` call. If application code does
  `restContext.getBeanStore().getBean(MyService.class)` per request, the
  overlay wins.
- **`RestContext` accessor methods that hit the bean store on each call**:
  `getCallLogger()`, `getEncoders()`, `getSerializers()`, `getParsers()`,
  `getMarshallingContext()`, `getLogger()`, `getMessages()`,
  `getMethodExecStore()`, `getPartParser()`, `getPartSerializer()`,
  `getResponseProcessors()`, `getRestChildren()`, `getRestOperations()`,
  `getStaticFiles()`, `getSwaggerProvider()`, `getOpenApiProvider()`,
  `getThrownStore()`, `getVarResolver()`, `getDebugEnablement()`,
  `getConfig()`, `getJsonSchemaGenerator()`,
  `getDefaultRequestAttributes()`, `getDefaultRequestHeaders()`,
  `getDefaultResponseHeaders()`. All of these resolve through
  `beanStore.getBean(...)` per call (`RestContext.java` lines 2037–2622).
  **Caveat:** while the accessor itself re-resolves, the framework's
  *internal* use of these beans typically goes through `RestOpContext`
  memoizers that pin the value. So Mode OVERLAY influences user-code accessor
  calls but not the framework's already-pinned references.
- **`MicroserviceListener` beans** — iterated from the bean store on each
  call to `Microservice.start()` and `stop()` (`Microservice.java` lines
  1223, 1262) and on broadcast events (lines 1167, 1262). If the test
  installs the overlay before driving a `start()` / `stop()` /
  broadcast, the swapped listener fires.
- **`ConsoleCommand` beans** — only Mode-OVERLAY-safe **before** the
  `Microservice` constructor runs (line 748 pins them into
  `consoleCommandMap`). Effectively Mode-INJECT-only in practice.
- **`RestConverterList`, `RestGuardList`, `RestMatcherList`** —
  per-op-context `@Bean` override hooks (`RestOpContext.java` lines 259,
  512, 593). These are consulted once during op-context construction; a
  swap during op-context build sees the new value, but after that the
  list is pinned in a memoizer.

##### Mode-INJECT-only (boot-time or memoizer-pinned)

`RestContext` framework defaults (all memoizer-backed via
`registerFrameworkDefaults`, `RestContext.java` lines 389–428):

- `CallLogger`, `EncoderSet`, `SerializerSet`, `ParserSet`,
  `MarshallingContext`, `Logger`, `java.util.logging.Logger`, `ThrownStore`,
  `MethodExecStore`, `Messages`, `VarResolver`, `Config`,
  `ResponseProcessor[]`, `HttpPartSerializer`, `HttpPartParser`,
  `JsonSchemaGenerator`, `StaticFiles`, `FileFinder`, `DebugEnablement`,
  `SwaggerProvider`, `OpenApiProvider`, `RestOperations`, `RestChildren`.

  These types **are** Mode-OVERLAY-safe at the `getBean(...)` boundary (per the
  list above), but the framework's own consumers go through the memoizer
  which fires once and pins. To swap one of these in a way that *all*
  framework code sees the new value, use Mode INJECT.

`RestOpContext` per-op memoizers (`RestOpContext.java` lines 168–700):

- `callLogger`, `marshallingContext`, `converters`, `debugEnablement`,
  `defaultCharset`, `defaultRequestAttributes`, `defaultRequestFormData`,
  `defaultRequestHeaders`, `defaultRequestQueryData`,
  `defaultResponseHeaders`, `encoders`, `guards`, `httpMethod`,
  `jsonSchemaGenerator`, `matchersList`, `methodInvoker`, `maxInput`,
  `problemDetails`, `noInheritOp`, `optionalMatchers`, `postCallMethods`,
  `preCallMethods`, `parsers`, `partParser`, `partSerializer`.

  All pinned by per-op memoizers; once the op is invoked the first time
  the resolved instance is locked in for the op's lifetime.

`Microservice` constructor-pinned fields (`Microservice.java` lines 615–629):

- `args`, `config`, `manifest`, `varResolver`, `consoleCommandMap`,
  `consoleReader`, `consoleWriter`, `consoleThread`, `workingDir`,
  `configName`, `beanStore` (the reference itself).

`SerializerSet`, `ParserSet`, `EncoderSet` per-set fields:

- `entries` — populated at `build()` time from the supplied `BeanStore`;
  the set is immutable after construction. Mode-INJECT-only.

**Bottom line for documentation:** Mode OVERLAY is useful when the test's
assertions go through `restContext.getXxx()` / `restContext.getBeanStore().getBean(...)` /
per-request user code. Mode OVERLAY does **not** influence beans that the
framework has already pinned via a memoizer or constructor-final field.
When the test needs to swap one of those, use Mode INJECT (build a fresh SUT
with the overlay installed via the builder hook).

#### Push/pop API shape (preferred)

```java
public interface WritableBeanStore extends BeanStore, AutoCloseable {
    // ... existing API ...

    /**
     * Pushes a new overlay onto the override stack. Returns a token that
     * must be passed to popOverlay() to remove it. Pop order must be
     * LIFO; popping out of order is an IllegalStateException.
     */
    OverlayToken pushOverlay(BeanStore overlay);

    /** Removes the overlay identified by the token. */
    void popOverlay(OverlayToken token);
}
```

`OverlayToken` is an opaque public type. Internally it's an identity wrapper
around the pushed `BeanStore` so pop-order violations are easy to detect.

## Annotation surface — worked examples

### Example 1: `MockRestClient` + Mode INJECT (default) + named bean

```java
@ExtendWith(JuneauBeanStoreExtension.class)
class MyResourceTest {

    @TestBean
    MyExternalApi mockApi = Mockito.mock(MyExternalApi.class);

    @TestBean(name = "primary")
    static MyService primaryService() { return new InMemoryMyService("p"); }

    @TestBean(name = "secondary")
    static MyService secondaryService() { return new InMemoryMyService("s"); }

    @Test
    void aTest(TestBeanStore store) {
        var client = MockRestClient.create(MyResource.class)
            .overridingBeanStore(store)
            .build();

        client.get("/widgets/1").run().assertStatus().is(200);
        verify(mockApi).fetchWidget(1L);
    }
}
```

The two `MyService` beans are distinguished by name; the resource's code
uses `@Bean(name = "primary")` / `@Bean(name = "secondary")` constructor
parameters, and Mode INJECT overlay shadows both.

### Example 2: `Microservice` + Mode OVERLAY (existing instance, push/pop)

```java
@ExtendWith(JuneauBeanStoreExtension.class)
class MyMicroserviceTest {

    static Microservice microservice;

    @BeforeAll
    static void bootMicroservice() throws Exception {
        microservice = Microservice.create()
            .overridingBeanStore(new TestBeanStore())  // pre-install the stack overlay
            .build()
            .start();
    }

    @TestBean
    MyExternalApi mockApi = Mockito.mock(MyExternalApi.class);

    @Test
    void aTest(TestBeanStore store) {
        // Mode OVERLAY: the extension pushed `store` onto microservice's overlay stack
        // in @BeforeEach. mockApi is visible to anyone calling
        // microservice.getBeanStore().getBean(MyExternalApi.class).
        var listener = microservice.getBeanStore().getBean(MyMicroserviceListener.class).orElseThrow();
        listener.onSomething();
        verify(mockApi).fetchWidget(1L);
    }
    // @AfterEach: extension calls store.popOverlay(...) on microservice's bean store.
}
```

The extension recognizes that the `microservice` SUT was built with a
`TestBeanStore` (a stack-overlay-capable BeanStore) in its
`overridingParent` slot, and switches to Mode OVERLAY push/pop behavior
automatically. If the SUT had no stack-overlay-capable overlay installed,
the extension would fall back to snapshot-and-restore on the `entries` map
of `microservice.getBeanStore()` directly.

To switch a Mode OVERLAY test to Mode INJECT semantics (build fresh per test), simply
drop the `@BeforeAll` and let the per-test `@BeforeEach` build the SUT.

### Example 3: `SerializerSet` + Mode INJECT + class scope

```java
@ExtendWith(JuneauBeanStoreExtension.class)
class MySerializerTest {

    @TestBean(scope = CLASS)
    static MyPostProcessor postProcessor = new SpyPostProcessor();

    static SerializerSet serializers;

    @BeforeAll
    static void buildSerializers(TestBeanStore store) {
        serializers = SerializerSet.create()
            .overridingBeanStore(store)
            .add(JsonSerializer.class)
            .build();
    }

    @Test
    void aTest() throws Exception {
        var json = serializers.getSerializer("application/json").serialize(new MyBean());
        assertEquals(7, ((SpyPostProcessor) postProcessor).callCount());
    }
}
```

The class-scope overlay is installed once at `@BeforeAll`, the
`SerializerSet` is built against it, and every test shares that overlay.
`@TestBean(scope = METHOD)` fields (if any were declared) would be pushed
on top per test.

## Class summary

| Class                                                        | Package                                       | Module                  | Visibility            |
| ------------------------------------------------------------ | --------------------------------------------- | ----------------------- | --------------------- |
| `TestBeanStore`                                              | `org.apache.juneau.junit5`                    | `juneau-junit5`         | `public`              |
| `TestBean` (annotation, with `name`/`type`/`scope` members)  | `org.apache.juneau.junit5`                    | `juneau-junit5`         | `public`              |
| `JuneauBeanStoreExtension`                                   | `org.apache.juneau.junit5`                    | `juneau-junit5`         | `public`              |
| `StackOverlay` (internal stack-aware overlay BeanStore)      | `org.apache.juneau.commons.inject`            | `juneau-commons`        | `public` (consumed by `juneau-junit5`) |
| `BeanStoreOverridable<B>` (convention interface)             | `org.apache.juneau.commons.inject`            | `juneau-commons`        | `public`              |
| `OverlayToken` (opaque handle for push/pop)                  | `org.apache.juneau.commons.inject`            | `juneau-commons`        | `public`              |
| `WritableBeanStore.pushOverlay(BeanStore)` / `popOverlay(OverlayToken)` | `org.apache.juneau.commons.inject` | `juneau-commons`        | new interface methods |
| `Microservice.State` (enum)                                  | `org.apache.juneau.microservice`              | `juneau-microservice`   | `public`              |
| `Microservice.state()` / `installBeanStoreOverlay(...)` / `removeBeanStoreOverlay(...)` | `org.apache.juneau.microservice` | `juneau-microservice` | new methods           |
| `Microservice.Builder.overridingBeanStore(BeanStore)`        | `org.apache.juneau.microservice`              | `juneau-microservice`   | `public` (new method) |
| `SerializerSet.Builder.overridingBeanStore(BeanStore)`       | `org.apache.juneau.serializer`                | `juneau-marshall`       | `public` (new method) |
| `ParserSet.Builder.overridingBeanStore(BeanStore)`           | `org.apache.juneau.parser`                    | `juneau-marshall`       | `public` (new method) |
| `EncoderSet.Builder.overridingBeanStore(BeanStore)`          | `org.apache.juneau.encoders`                  | `juneau-marshall`       | `public` (new method) |
| `RestContext.Args.overridingParent` (new record component)   | `org.apache.juneau.rest`                      | `juneau-rest-server`    | `public` (new field)  |
| `MockRestClient.Builder.overridingBeanStore(BeanStore)`      | `org.apache.juneau.rest.mock` + `.classic`    | `juneau-rest-mock`      | `public` (new method) |

## Future direction — microservice-first re-orientation

Although the closest external analog (Spring `@MockBean`) is REST-shaped,
the **primary day-to-day target** for test-time bean injection in Juneau is
arguably the **`Microservice` runtime**, not individual `RestContext`
instances. REST end-to-end is already covered by `MockRestClient`; most
production code under test in a Juneau app is reachable through a
microservice's bean store, not through a per-resource one. If we accept the
microservice as the primary target, three design refinements come into
play. With **Modes A and B both promoted to v1** and the new framework
extensions in scope, the path forward is clearer.

### 1. Microservice singleton as the default override target

Today `Microservice.getInstance()` already returns a singleton-style
reference to the running microservice. With the `installBeanStoreOverlay(...)`
hook from the Framework extensions section, the JUnit 5 extension can
resolve `Microservice.getInstance()` automatically as the override target,
eliminating the explicit `.overridingBeanStore(...)` builder-hook ceremony
for the common case. The REST builder hooks
(`MockRestClient.Builder.overridingBeanStore(...)`) remain for cases where
no microservice has been booted (raw `@Rest`-class testing) but stop being
the canonical wiring.

This simplification only works if there is exactly one microservice per JVM
— which is the de facto reality today. See open question on the
multi-instance registry.

### 2. Multi-microservice support — identification via class or id

If a test process can host multiple microservices simultaneously — or if a
test wants to overlay one of several microservices distinguished by class
or by id — the overlay needs a discriminator. Two viable approaches:

- **Class-based discriminator:** `@TestBean(microservice = MyMicroservice.class)`
  — the overlay applies to the bean store of the microservice instance
  whose runtime class matches. Feasible **if** the multi-instance registry
  from the Framework extensions section ships in v1; see open question.
- **Id-based discriminator:** `@TestBean(microservice = "alpha")` — overlay
  applies to the microservice registered under id `"alpha"` in a
  microservice registry. Requires both the registry **and** a registration
  identity mechanism that doesn't exist today.

**Recommendation:** design the discriminator as a method on the annotation
(`Class<? extends Microservice> microservice() default Microservice.class;`)
so that v1 lands without an id-based registry. A future v2 can layer
id-based discrimination on top without breaking compatibility.

### 3. Two override modes — both in v1 (Phases 5 and 6)

The original future-direction recommendation called for both modes; the
finalized plan promotes both to v1.

- **Mode INJECT — fresh-instance with overrides.** Phase 5. Default mode.
- **Mode OVERLAY — existing-instance with temporary overrides.** Phase 6. Opt-in
  via the extension's `mode(Mode.EXISTING_INSTANCE)` builder flag or via
  `@TestBean(mode = LIVE)`. Mode OVERLAY is implemented via the stack-overlay
  API; the fallback is snapshot-and-restore on `entries`.

The Mode-OVERLAY-safe vs Mode-INJECT-only inventory in Phase 6 documents which beans
each mode can influence. The user-visible rule of thumb:

- Need to swap a framework-managed bean (`CallLogger`, `SerializerSet`,
  etc.) in a way *all* framework code sees? → **Mode INJECT.**
- Need to swap a user-defined service that's resolved per-request from
  the bean store? → **either mode**, but Mode OVERLAY saves the SUT rebuild
  cost.
- Need to swap a `MicroserviceListener` that fires on
  `start()` / `stop()`? → either mode, Mode OVERLAY is more natural.

## Decisions

1. **`BeanStoreOverridable<B>` — marker interface or naming convention?**
   The non-REST builders all expose `overridingBeanStore(BeanStore)` per
   Phase 4. Formalizing this as a public interface means the JUnit
   extension can wire by `instanceof BeanStoreOverridable<?>` rather than
   by a chain of `instanceof SerializerSet.Builder || instanceof ParserSet.Builder || …`.
   The cost is one line of `implements BeanStoreOverridable<Builder>` per
   builder plus the public interface itself.

   **Decision:** ship as a public interface. One-time documentation cost
   pays off forever in the extension code.

2. **`@TestBean.scope` enum — name and shape.** Today drafted as
   `enum Scope { METHOD, CLASS }`. Alternatives considered: `enum Lifetime`,
   `enum Lifecycle`, `boolean classScope() default false`. The enum form
   is preferred because it leaves room for additional scopes
   (`PER_RESOURCE`, `JVM`, etc.) without breaking compatibility.

   **Decision:** `enum Scope { METHOD, CLASS }` as drafted.

3. **Expressing Mode-OVERLAY-only restriction at compile/runtime.** Some bean
   types are Mode-INJECT-only (`CallLogger` and friends — see inventory). A
   test that declares `@TestBean(mode = LIVE) CallLogger callLogger = ...`
   will compile fine but silently fail to take effect at runtime — the
   real `CallLogger` was already pinned by the per-op memoizer. Options:
   - **(a)** Maintain a static `Set<Class<?>>` of Mode-INJECT-only types in the
     extension and throw if a Mode-OVERLAY `@TestBean` targets one. Brittle —
     the inventory drifts.
   - **(b)** Document the limitation and let it fail loudly via the test's
     own assertion. Less brittle, more user-error-prone.
   - **(c)** Add a `@ModeInjectOnly` marker annotation on the framework's
     memoized bean types and have the extension scan for it. Heavy.

   **Decision:** option **(b)** for v1. The inventory in Phase 6 is the
   documentation; the test failure when a Mode-OVERLAY swap of `CallLogger`
   doesn't take effect is the diagnostic. Revisit if real users
   misconfigure repeatedly.

4. **Overlay-stack vs single-slot for `overridingParent`.** Today
   `overridingParent` is set-once at `BasicBeanStore` construction. Class
   scope and Mode OVERLAY both want stack semantics. Options:
   - **(a)** Replace the slot with a stack — breaking change for the
     Spring bridge consumer.
   - **(b)** Keep the slot and add a sibling stack — two parallel
     resolution priorities, twice the precedence surface.
   - **(c)** Layer stack semantics on top via composition — a
     `StackOverlay extends BeanStore` that the slot points at.

   **Decision:** option **(c)** — composition via `StackOverlay`.
   **Conditional on the Phase 0 spike going clean.** If the spike reveals
   the composition path is non-trivial (e.g. `BasicBeanStore.toString()`
   recursion into the stack creates cycles, lifecycle cleanup races,
   etc.), fall back to option (a) per the Phase 0 go/no-go gate and
   document the migration impact for the Spring bridge consumer.

5. **Microservice multi-instance registry — ship in v1 or defer?**
   Class-based discrimination (`@TestBean(microservice = MyMS.class)`)
   requires `Microservice` to maintain a registry of live instances. The
   change is small (a static `CopyOnWriteArrayList<Microservice>`
   populated in `start()` / drained in `stop()`) but does change the
   framework's contract about what `Microservice.getInstance()` means in
   a multi-microservice JVM.

   **Decision:** ship the registry **only** if Phase 5 (Mode INJECT)
   implementation surfaces a real test-time use case for
   class-discrimination. If Mode INJECT only ever needs the singleton during
   Phase 5, defer the registry — and at that point, **create a new TODO**
   to track the registry feature as its own work item rather than letting
   it disappear into a future-direction note. The new TODO should
   reference back to TODO-35's Phase 5 implementation as its driving use
   case so the rationale isn't lost.

## Out of scope (v1)

- **CGLIB-based dynamic proxying.** User constraint.
- **`@SpyBean` (wrap a real bean with side-effect spying).** Defer to v2.
- **Autowiring mocks into the test class by reflection beyond `@TestBean`
  field discovery (e.g. `@Inject` on test fields).** Explicit assignment
  only.
- **Mockito convenience (auto-`Mockito.mock(...)` for `null` `@TestBean`
  fields).** Hard constraint: `juneau-junit5` must not depend on Mockito.
  Tests are free to construct mocks themselves.
- **Per-call supplier swap (Option A from the original brief — requiring
  production code to register beans as `Supplier<T>`).** Already
  trivially satisfied because all beans are supplier-backed; no work
  needed.
- **Id-based microservice discrimination** (`@TestBean(microservice = "alpha")`).
  Class-based discrimination is in scope iff the multi-instance registry
  ships per open question 5; id-based requires further infrastructure
  that doesn't exist today.
- **Auto-discovery of `RestContext` / `Microservice` on the test class.**
  Test author wires the overlay in explicitly via
  `.overridingBeanStore(...)`.

## Implementation phases

**v1 = Phases 0 (spike) + 1 + 2 + 3 + 4 + 5 + 6.** Phases 5 and 6 share
much of their plumbing; the split is doctrinal (default vs opt-in path)
rather than chronological.

### Phase 0 — Stack-overlay spike

Validate the composition-based stack overlay design from open question 4
before committing the framework changes. Two days, no dependent work.

1. Implement `StackOverlay extends BeanStore` in
   `org.apache.juneau.commons.inject`. Wraps a `Deque<BeanStore>`,
   resolves top-of-stack first.
2. Write a `StackOverlay_Test` in `juneau-utest` that exercises push/pop,
   LIFO violation detection, empty-stack fall-through to the next
   `BeanStore.resolve(...)` tier.
3. Sanity-check by wedging a `StackOverlay` into a sample
   `BasicBeanStore`'s `overridingParent` slot and confirming the existing
   Spring-bridge tests in `RestContext_Precedence_Test` still pass.
4. **Go/no-go decision:** if the spike reveals the composition path is
   non-trivial (e.g. `BasicBeanStore.toString()` recursion into the stack
   creates cycles), fall back to option (a) — replace the slot with a
   stack — and document the migration impact.

### Phase 1 — `TestBeanStore` + REST `Args.overridingParent`

Lands the Mode INJECT core capability. No JUnit dependency.

1. Create module `juneau-junit5` (`juneau-core/juneau-junit5/`). `pom.xml`
   depends on `juneau-commons` (for `BasicBeanStore`, `StackOverlay`) and
   on `junit-jupiter-api` at `provided` scope (so consumers can pin their
   own JUnit version).
2. Add `org.apache.juneau.junit5.TestBeanStore extends BasicBeanStore` with
   the four `override(...)` fluent methods sketched above.
3. Extend `RestContext.Args` with an optional `BeanStore overridingParent`
   record component. Thread it through `createBeanStore(...)` at
   `RestContext.java` line 368 — change the body to
   `new BasicBeanStore(parentBs, args.overridingParent())`.
4. Add `MockRestClient.Builder.overridingBeanStore(BeanStore)` in both
   `mock` and `mock.classic` packages. Construct the new `Args` instance
   with the overlay populated.
5. Tests in `juneau-utest`:
   - `TestBeanStore_Test` — basic overlay semantics.
   - `RestContext_Args_OverridingParent_Test` — `Args.overridingParent`
     wins over local `@Bean` factories.
   - `MockRestClient_TestOverride_Test` — end-to-end through the mock
     client.

### Phase 2 — `@TestBean` annotation + `JuneauBeanStoreExtension`

1. Add `org.apache.juneau.junit5.TestBean` annotation with `name`, `type`,
   `scope` members.
2. Add `org.apache.juneau.junit5.JuneauBeanStoreExtension` implementing
   `BeforeEachCallback`, `AfterEachCallback`, `ParameterResolver`. Per-test
   `TestBeanStore` lifecycle. Stash in `ExtensionContext.Store` keyed on
   the extension class. Parameter resolution for `TestBeanStore`
   parameters on test methods.
3. Tests in `juneau-utest`:
   - `JuneauBeanStoreExtension_Test` — field discovery, static method
     discovery, named-bean discovery, per-test isolation, parameter
     resolution.
   - `JuneauBeanStoreExtension_RestIntegration_Test` — full `@TestBean`
     + `MockRestClient` flow against a sample resource.

### Phase 3 — Class-scope overlays

1. Extend `JuneauBeanStoreExtension` with `BeforeAllCallback` and
   `AfterAllCallback`. Build a class-scope `TestBeanStore` at `beforeAll`,
   drop it at `afterAll`.
2. Make per-test overlays chain on top of the class-scope store via the
   `StackOverlay` from Phase 0.
3. Throw `JUnitException` in `beforeAll` if a `@TestBean(scope = CLASS)`
   field is non-`static`. Include the field name in the message.
4. Tests in `juneau-utest`:
   - `JuneauBeanStoreExtension_ClassScope_Test` — class-scope visible to
     every method, per-test scope chained on top, isolation between
     methods.
   - `JuneauBeanStoreExtension_ClassScope_Error_Test` — non-static
     `@TestBean(scope = CLASS)` produces the expected error.

### Phase 4 — Non-REST builder hooks + `BeanStoreOverridable`

1. Add `org.apache.juneau.commons.inject.BeanStoreOverridable<B>`
   interface.
2. Add `.overridingBeanStore(BeanStore)` to:
   - `SerializerSet.Builder` (also `implements BeanStoreOverridable<Builder>`).
   - `ParserSet.Builder` (same).
   - `EncoderSet.Builder` (same).
   - `Microservice.Builder` (same).
3. Wire each builder's stored `BeanStore` field with
   `new BasicBeanStore(currentBeanStore, overlay)` at build time so the
   eventual factory call sees the overlay first.
4. Tests in `juneau-utest`:
   - `SerializerSet_OverridingBeanStore_Test` — overlay shadows a
     serializer-instantiation dependency.
   - `ParserSet_OverridingBeanStore_Test` — same for parser set.
   - `EncoderSet_OverridingBeanStore_Test` — same for encoder set.
   - `Microservice_OverridingBeanStore_Test` — overlay shadows
     `MicroserviceListener` resolution at boot.

### Phase 5 — Mode INJECT integration

1. Update `JuneauBeanStoreExtension` to default `Mode.FRESH_INSTANCE`
   (Mode INJECT) — no behavior change beyond labeling, since Phases 1–4
   already implement the Mode INJECT path.
2. Add the `Mode` enum and `.mode(Mode)` builder method on the extension.
3. Tests in `juneau-utest`:
   - `Mode_INJECT_MockRestClient_Test` — end-to-end Mode INJECT through
     `MockRestClient`.
   - `Mode_INJECT_Microservice_Test` — end-to-end Mode INJECT through
     `Microservice.create().overridingBeanStore(...)`.
   - `Mode_INJECT_SerializerSet_Test` — end-to-end Mode INJECT through
     `SerializerSet.create().overridingBeanStore(...)`.

### Phase 6 — Mode OVERLAY push/pop + framework `pushOverlay`/`popOverlay`

1. Add `WritableBeanStore.pushOverlay(BeanStore)` / `popOverlay(OverlayToken)`
   on the interface; implement on `BasicBeanStore` via the `StackOverlay`
   from Phase 0.
2. Add `Microservice.installBeanStoreOverlay(BeanStore)` /
   `removeBeanStoreOverlay(BeanStore)` /
   `state(): Microservice.State` accessor. `State` enum lives in
   `org.apache.juneau.microservice`.
3. Extend `JuneauBeanStoreExtension` to honor
   `Mode.EXISTING_INSTANCE`: in `beforeEach`, resolve the SUT (either via
   the registered SUT field on the test class — explicit assignment — or
   via the `ParameterResolver`-passed reference), call `pushOverlay`,
   stash the token. In `afterEach`, call `popOverlay(token)`.
4. Implement the snapshot-and-restore fallback path. When the SUT's bean
   store doesn't honor `pushOverlay` (older version, third-party
   implementation), the extension snapshots the affected `(beanType, name)`
   suppliers via `getBeanSupplier(...)` / `getDefaultSupplier(...)`,
   does `addSupplier(...)` to install the override, and on `afterEach`
   reinstalls the snapshotted suppliers via `addSupplier(...)`.
5. Tests in `juneau-utest`:
   - `Mode_OVERLAY_MockRestClient_Test` — Mode OVERLAY against a per-class
     `MockRestClient` SUT.
   - `Mode_OVERLAY_Microservice_Test` — Mode OVERLAY against a per-class running
     microservice.
   - `Mode_OVERLAY_Fallback_Test` — Mode OVERLAY against a `BasicBeanStore` that
     doesn't honor `pushOverlay` (built with a custom overlay that lacks
     the stack); verify the snapshot-and-restore fallback works.
   - `Mode_OVERLAY_Inventory_Test` — sanity-check that swapping a known
     Mode-INJECT-only type (`CallLogger`) via Mode OVERLAY does **not** affect the
     framework's pinned reference. This is the loud-failure test for the
     inventory documented above.
6. Release-notes entry under
   `juneau-docs/docs/pages/release-notes/9.5.0.md` for the new module,
   the new `WritableBeanStore` methods, the new `Microservice` overlay
   API, and the four `overridingBeanStore(...)` builder methods.
