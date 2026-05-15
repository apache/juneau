# FINISHED-33: Dynamic add/remove of child REST resources

**Status:** Shipped in 9.5.0.

## What landed

Runtime add/remove of child REST resources on any parent `RestContext`, with ergonomic facades on `BasicRestServletGroup` / `BasicRestObjectGroup`.

### API

On `RestChildren`:

```java
public RestContext addChild(Class<?> resourceClass) throws ServletException;
public RestContext addChild(Object resource) throws ServletException;
public RestContext addChild(Object resource, boolean replace) throws ServletException;
public RestContext addChild(String path, Object resource) throws ServletException;
public RestContext addChild(String path, Object resource, boolean replace) throws ServletException;
public RestContext removeChild(String path);
public RestContext removeChild(Class<?> resourceClass);
```

Convenience pass-throughs on `BasicRestServletGroup` and `BasicRestObjectGroup`:

```java
public RestChildren getChildResources();
public RestContext addChild(Class<?>);
public RestContext addChild(Object);
public RestContext addChild(String path, Object);
public RestContext removeChild(String path);
public RestContext removeChild(Class<?>);
```

### Concurrency model

`RestChildren` now holds children in a `volatile` copy-on-write snapshot (`Map<String,RestContext>` wrapped via `Collections.unmodifiableMap`). Route matching (`findMatch(...)`) reads the snapshot once and iterates lock-free. Mutations are serialized through an internal `writeLock`, build a fresh `LinkedHashMap`, then atomically swap the snapshot. `asMap()` returns the snapshot directly — no defensive copy.

### Lifecycle

- `addChild(...)` builds the child via the shared `RestChildren.buildChildContext(...)` recipe (bean-store lookup or `BeanInstantiator`, optional reflective `setContext`), then invokes `postInit()` + `postInitChildFirst()` on the child before returning.
- `removeChild(...)` calls `Servlet.destroy()` for Servlet-backed children (which transitively calls `RestContext.destroy()` exactly once) or `RestContext.destroy()` directly for non-Servlet children. `RestContext.destroy()` recursively destroys grandchildren before closing its own bean store.
- Duplicate paths without `replace=true` throw `IllegalStateException`.

## Files changed

- `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/RestChildren.java` — rewritten with volatile snapshot and the new public API; factored out `buildChildContext(...)` for reuse by the static-init memoizer.
- `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/RestContext.java`
  - The `restChildren` memoizer now delegates to `RestChildren.create(this, bs, servletConfig)` and `RestChildren.buildChildContext(...)`.
  - Path resolution prioritises `Args.path` when non-empty, then falls back to `@Rest(path)`.
  - `postInit()` now uses `ClassInfo.getMethod(...)` (not `getPublicMethod`) so it finds protected `setContext` declarations on subclasses of `RestServlet`.
  - `destroy()` invokes `@RestDestroy` hooks, then recursively destroys children (while the bean store is still open), then closes the bean store — fixing a latent ordering bug uncovered by the new dynamic remove path.
- `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/servlet/BasicRestServletGroup.java` — added `getChildResources()` + `addChild`/`removeChild` convenience methods; Javadoc updated.
- `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/servlet/BasicRestObjectGroup.java` — identical API for parity.
- `juneau-utest/src/test/java/org/apache/juneau/rest/BasicRestServletGroup_DynamicChildren_Test.java` — 15 tests covering by-class / by-instance / path-override adds, remove-by-path / remove-by-class, duplicate-path errors, replace semantics, insertion order, lifecycle hooks, Servlet-typed child setContext + Servlet.destroy, bean-store-backed instantiation, and a concurrent reader/writer smoke test.
- `juneau-docs/pages/topics/10.03.03.ChildResources.md` — new "Dynamic Child Resources" section documenting the API and concurrency model.
- `juneau-docs/pages/release-notes/9.5.0.md` — release-notes entry under `juneau-rest-server`.

## Coverage

`RestChildren.java`: 88% branches / 93% instructions. Remaining gaps are defensive paths (null-guards, exception wrappers) and the parent `destroy()` walk that is not exercised by `MockRestClient` tests.

## Verification

- `mvn -pl juneau-utest test` — 50,121 tests pass, 0 failures, 0 errors.
- `BasicRestServletGroup_DynamicChildren_Test` — all 15 tests pass.
