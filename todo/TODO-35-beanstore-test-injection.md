# TODO-35 — JUnit 5 test-time injection of mocks/fakes into BeanStore

## Goal

Provide a small, idiomatic mechanism for swapping production beans with test
doubles (mocks/fakes) inside a Juneau `BeanStore` for the duration of a JUnit 5
test, without modifying production code and without proxying.

The target ergonomic is "Spring `@TestConfiguration` + `@MockBean`, but
Juneau-flavored": declare a `@TestBean`-annotated field or static factory method
on the test class, register a JUnit 5 extension, point it at the
`RestContext`/SUT, and let the extension overlay the bean store for each test.

Constraint from the user: **keep it simple**. No CGLIB, no AOT bytecode
generation, no autowiring magic, no `@SpyBean` in v1.

## Reference patterns

- **Spring `@TestConfiguration` + `@MockBean` / `@SpyBean`** — replaces beans in
  the application context at test-class level; uses CGLIB proxies for
  `@SpyBean`. Not applicable for the proxy bits, but the annotation surface is
  the model.
- **Guice `Modules.override(production).with(testModule)`** — explicit override
  layering, no proxying. **This is the closest analog to what Juneau can do
  with its existing `overridingParent` BeanStore slot.**
- **Dagger Android `TestComponent`** — compile-time swap of the entire DI
  graph. Out of scope: requires a parallel component definition per test.
- **Quarkus `@InjectMock`** — build-time bytecode generation. Not relevant to
  Juneau (no AOT).
- **Micronaut `@MockBean`** — compile-time AOT. Same — not relevant.

The plan below adopts Guice-style explicit override layering, exposed through
a JUnit 5 extension and a `@TestBean` annotation.

## Constraints

- **No CGLIB / no dynamic proxies.** `BeanStore` returns plain instances. To
  replace a bean, an alternative instance must be registered before the
  consumer resolves it. Replacing references after the consumer has cached the
  bean is explicitly out of scope.
- **No AOT bytecode generation.** Everything must be runtime-reflective.
- **BeanStore is already hierarchical.** `BasicBeanStore` supports `parent` and
  `overridingParent`. Resolution order (top wins):
  1. `overridingParent` (if non-null)
  2. local entries (via `addBean` / `addSupplier`)
  3. `parent` (if non-null)
  4. local default suppliers (`addDefaultSupplier`)

  This means the cleanest test-injection point is **the `overridingParent`
  slot** — it already exists, is honored framework-wide, and trumps everything
  the resource configured locally.
- **`RestContext.getBeanStore()` is exposed** as a `WritableBeanStore`, so
  test code can address the bean store of a running resource. However
  `overridingParent` is fixed at `BasicBeanStore` construction time, so a test
  overlay must be installed **before** the `RestContext` is built or via a
  builder hook on `MockRestClient` / `RestContext.Builder`.
- **v1 scope is REST.** `BeanStore` is also used by `SerializerSet`,
  `ParserSet`, `EncoderSet`, and by the microservice runtime, but the user's
  intent and the closest existing analog (Spring `@MockBean`) is REST-shaped.
  Non-REST consumers can use the Phase 1 `TestBeanStore` directly without the
  JUnit extension.

## Current state

- **No `@TestBean` / `@MockBean` / `@TestConfiguration` analog exists today.**
  Searching `juneau-utest` and `juneau-rest/*/src/test` finds no test-injection
  annotation in use.
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

  Several tests in that file build a parallel `SpringLikeBeanStore` by hand and
  thread it in via the `@Bean WritableBeanStore createBeanStore()` factory hook
  on a synthetic inner-class resource. That works for the framework's own
  precedence tests but is heavy ceremony for application tests.
- **The pieces we need are already in place.** `BasicBeanStore` already
  honors `overridingParent`. The gap is (a) an ergonomic builder for the
  overlay, (b) a way to bind it into a `RestContext` from a test, and (c) a
  JUnit 5 extension to manage lifecycle.

## Recommended design

**Combine Options C (overlay BeanStore) + D (JUnit 5 extension)** from the
user-supplied design brief. Reject Option A (`Supplier<T>`-only registrations
in production) as too invasive; reject Option B (in-place push/pop on the
production store) as a fallback only — the overlay path is cleaner and the
production store is never mutated.

### Phase 1 — `TestBeanStore` overlay class

- **Module:** new module **`juneau-junit5`** under `juneau-core/`. Sibling of
  `juneau-bct` and `juneau-assertions`. `juneau-bct` is "Bean-Centric Testing"
  (a different concept — assertion DSL); test-injection is a distinct
  capability and deserves its own artifact so downstream consumers can pull it
  in without dragging in BCT.

  Justification for a new module rather than putting it in `juneau-bct`:
  consumers of `juneau-bct` today are using its assertion DSL and don't expect
  a JUnit-extension dependency to leak through. Keeping them separate also
  lets `juneau-junit5` depend on `juneau-bct` later if desired.

- **Class:**
  `org.apache.juneau.junit5.TestBeanStore extends BasicBeanStore`.
  Public. Thin fluent wrapper.

  ```java
  public class TestBeanStore extends BasicBeanStore {
      public TestBeanStore() { super(); }
      public <T> TestBeanStore override(Class<T> type, T bean)              { addBean(type, bean); return this; }
      public <T> TestBeanStore override(Class<T> type, T bean, String name) { addBean(type, bean, name); return this; }
      public <T> TestBeanStore override(Class<T> type, Supplier<T> s)       { addSupplier(type, s); return this; }
  }
  ```

  No JUnit 5 dependency in the class itself — it's a pure overlay-builder.
  Tests outside JUnit (TestNG, raw `main`) can use it directly.

- **Wiring into a `RestContext`:** add a new public method on
  `RestContext.Builder`:

  ```java
  public RestContext.Builder overridingBeanStore(BeanStore store) { ... }
  ```

  This sets the `overridingParent` slot when the eventual `BasicBeanStore` is
  constructed at line ~1184 of `RestContext.java`. Mirror the same method on
  `MockRestClient.Builder`:

  ```java
  MockRestClient.create(MyResource.class)
      .overridingBeanStore(testBeanStore)
      .build();
  ```

  This is the only production-code change in Phase 1. The change is additive
  (no breaking API) and uses the BeanStore precedence mechanism that already
  exists.

- **Tests:** `TestBeanStore_Test` in `juneau-utest` covering
  override + restore semantics, named beans, supplier overrides, and parent
  precedence (an entry in the overlay must shadow the production resource's
  `@Bean` factory method since `overridingParent` beats local entries).

### Phase 2 — `JuneauBeanStoreExtension` + `@TestBean` annotation

Builds on Phase 1. Adds the JUnit 5 surface.

- **Annotation:** `org.apache.juneau.junit5.TestBean`. Type-only in v1
  (no `name` qualifier).

  ```java
  @Retention(RUNTIME)
  @Target({ FIELD, METHOD })
  public @interface TestBean {}
  ```

  Applies to either:
  - **Instance / static fields** — value is used directly as the override.
  - **Static factory methods** — invoked once per test to produce the override
    instance. Method's return type is the override target type. Lifecycle is
    `@BeforeEach`.

- **Extension:**
  `org.apache.juneau.junit5.JuneauBeanStoreExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver`.

  Behavior:
  - **`beforeEach`**: scan the test instance for `@TestBean` fields and
    methods. Build a fresh `TestBeanStore` from them. Stash it in the
    extension's `ExtensionContext.Store` (test-scoped). Per-test isolation.
  - **`afterEach`**: drop the overlay. The production resource is unaffected.
  - **`ParameterResolver`**: test methods may declare a `TestBeanStore`
    parameter and receive the current overlay — convenient for asserting on
    test-double state or for wiring into a builder.

- **Registration:** two supported forms.
  - **Declarative**: `@ExtendWith(JuneauBeanStoreExtension.class)` on the test
    class.
  - **Programmatic**: a `@RegisterExtension` field that exposes the overlay
    directly:

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
  auto-discover a `RestContext` field on the test and does **not** mutate it
  reflectively. The test author is responsible for one line:
  `.overridingBeanStore(ext.getStore())`. Rationale: the user explicitly said
  "don't overcomplicate" — auto-discovery is the kind of feature that pulls
  in conditional logic, edge cases, and surprises. Make the wiring explicit.

### Annotation surface — worked example

```java
@ExtendWith(JuneauBeanStoreExtension.class)
class MyResourceTest {

    @TestBean
    MyExternalApi mockApi = Mockito.mock(MyExternalApi.class);

    @TestBean
    static MyService fakeService() { return new InMemoryMyService(); }

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

### Class summary

| Class                        | Package                     | Module          | Visibility |
| ---------------------------- | --------------------------- | --------------- | ---------- |
| `TestBeanStore`              | `org.apache.juneau.junit5`  | `juneau-junit5` | `public`   |
| `TestBean` (annotation)      | `org.apache.juneau.junit5`  | `juneau-junit5` | `public`   |
| `JuneauBeanStoreExtension`   | `org.apache.juneau.junit5`  | `juneau-junit5` | `public`   |
| `RestContext.Builder.overridingBeanStore(BeanStore)`     | `org.apache.juneau.rest`    | `juneau-rest-server` | `public` (new method) |
| `MockRestClient.Builder.overridingBeanStore(BeanStore)`  | `org.apache.juneau.rest.mock.classic` | `juneau-rest-mock` | `public` (new method) |

## Open questions

1. **Module location for the JUnit extension.** New `juneau-junit5` module
   (recommended) vs. folding it into `juneau-bct`. Recommendation: new
   module to keep concerns separate, but call out the alternative for review.
2. **`@TestBean(name = "...")` qualifier support in v1.** Recommendation: skip
   for v1 (type-only). Add later if surveying real usage shows demand.
3. **Class-level overlays via `@BeforeAll` / `@AfterAll`.** Recommendation:
   skip in v1; per-test isolation is the safer default and matches the user's
   "don't overcomplicate" preference.
4. **Auto-discovery vs. explicit binding.** Recommendation: explicit
   (`ext.getStore()` + `.overridingBeanStore(...)`). Reject auto-discovery for
   v1.
5. **Mockito convenience (`@MockBean`).** Should the extension auto-create a
   `Mockito.mock(...)` for `@TestBean` fields whose value is `null` at
   `beforeEach` time? Recommendation: defer to Phase 3. v1 expects the test
   to construct the double itself (Mockito, fake, anonymous subclass, etc.).
6. **Non-REST contexts.** `BeanStore` is also used by `SerializerSet`,
   `ParserSet`, `EncoderSet`, and the microservice runtime. v1 binding hook
   is REST-only (`RestContext.Builder.overridingBeanStore` +
   `MockRestClient.Builder.overridingBeanStore`). Non-REST consumers can still
   use `TestBeanStore` standalone as the `overridingParent` to a manually-built
   `BasicBeanStore`. Should v1 also add binding hooks for those other
   contexts, or wait for demand?

## Out of scope (v1)

- **CGLIB-based dynamic proxying.** User constraint.
- **`@SpyBean`** (wrap a real bean with side-effect spying). Defer to v2.
- **Autowiring mocks into the test class** by reflection beyond `@TestBean`
  field discovery (e.g. `@Inject` on test fields). Explicit assignment only.
- **Per-call supplier swap (Option A)** — requiring production code to
  register beans as `Supplier<T>` for them to be overridable. Rejected as
  invasive.
- **In-place mutate/restore on the production store (Option B)** — would
  require `pushOverride` / `popOverride` API on `WritableBeanStore` and
  introduces lifecycle/leak risk. The overlay design avoids this entirely.
- **Class-level overlays** via `@BeforeAll` / `@AfterAll`. Per-test isolation
  only in v1.
- **Auto-discovery of `RestContext` on the test class.** Test author wires
  the overlay in explicitly via `.overridingBeanStore(...)`.
- **Bindings into non-REST consumers** (serializers, microservice). Phase 1's
  `TestBeanStore` is usable directly as an `overridingParent` for any
  `BasicBeanStore`; only the REST builder hook is added in v1.

## Implementation phases

**v1 = Phase 1 + Phase 2.**

### Phase 1 — `TestBeanStore` + REST builder hook

Lands the core capability. No JUnit dependency.

1. Create module `juneau-junit5` (`juneau-core/juneau-junit5/`). `pom.xml`
   depends on `juneau-commons` (for `BasicBeanStore`) and on
   `junit-jupiter-api` at `provided` scope (so the artifact doesn't force a
   JUnit version on consumers in Phase 1).
2. Add `org.apache.juneau.junit5.TestBeanStore extends BasicBeanStore` with
   the three `override(...)` fluent methods sketched above.
3. Add `RestContext.Builder.overridingBeanStore(BeanStore)` and thread the
   value through to the `BasicBeanStore(parent, overridingParent)` call site
   inside `RestContext.createBeanStore(...)` / the constructor (around lines
   1178–1184).
4. Add `MockRestClient.Builder.overridingBeanStore(BeanStore)` that forwards
   to the underlying `RestContext.Builder`.
5. Tests in `juneau-utest`:
   - `TestBeanStore_Test` — basic overlay semantics.
   - `RestContext_TestOverride_Test` — overlay shadows a resource's `@Bean`
     factory method (proves `overridingParent` precedence still holds for
     test-supplied beans).
   - `MockRestClient_TestOverride_Test` — end-to-end through the mock client.

### Phase 2 — `@TestBean` annotation + `JuneauBeanStoreExtension`

1. Add `org.apache.juneau.junit5.TestBean` annotation (field + method,
   type-only).
2. Add `org.apache.juneau.junit5.JuneauBeanStoreExtension` implementing
   `BeforeEachCallback`, `AfterEachCallback`, `ParameterResolver`.
   - Per-test `TestBeanStore` lifecycle.
   - Stash in `ExtensionContext.Store` keyed on the extension class.
   - Parameter resolution for `TestBeanStore` parameters on test methods.
3. Tests in `juneau-utest`:
   - `JuneauBeanStoreExtension_Test` — field discovery, static method
     discovery, per-test isolation, parameter resolution.
   - `JuneauBeanStoreExtension_RestIntegration_Test` — full
     `@TestBean` + `MockRestClient` flow against a sample resource.
4. Release-notes entry under `juneau-docs/docs/pages/release-notes/9.5.0.md`
   for the new module and the `overridingBeanStore` builder methods.

### Phase 3 (optional, deferred) — Mockito convenience

Add an `@MockBean` annotation (or a `mockito = true` attribute on `@TestBean`)
that auto-creates a `Mockito.mock(...)` when the annotated field is `null` at
`beforeEach` time. Requires an optional Mockito dependency on `juneau-junit5`.
Skip unless the user requests it after v1 lands.
