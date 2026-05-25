# FINISHED-99 — SVL `${...}` resolution in `@RestOp(path)` / `@RestGet(path)` / `@RestOp(value)`

> Same-session enhancement, no plan file. Filed and landed in one Phase C1 subagent run during the TODO-78 cleanup family. No `TODO-99-*.md` plan file was created — the work was small and well-scoped enough to capture entirely in this archive.

## Summary

Closes the SVL-resolution asymmetry in `RestOpContext.pathMatchers`: class-level `@Rest(path)` / `@Rest(paths)` were already SVL-resolved against the host context's `VarResolver`, but op-level `@RestOp(path)` / `@RestGet(path)` / `@RestPost(path)` / etc. — and the verb-on-`value()` form on `@RestOp(value="METHOD path")` — were not. After this change, the op-level annotations pass through the same `varResolver().resolve(...)` step before being compiled into `UrlPathMatcher` patterns. Resources whose annotation strings contain no `${…}` / `$S{…}` / `$E{…}` / `$C{…}` markers are unchanged (SVL on a literal is a no-op). Unblocks the configurable-mixin-mount-path pattern needed for the TODO-78 family cleanup (and any future deployer that wants `@RestGet(path="/${myroute:default}/*")` instead of subclassing the mixin to relocate its mount).

## Files changed

- **`juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/RestOpContext.java`**
  - Inside the `pathMatchers` `Memoizer` (single existing `@SuppressWarnings("java:S3776")` block):
    - Added `var vr = varResolver();` once at the top of the lambda (matches the in-file convention used by `httpMethod`, `findOpString`, `collectAnnotationMediaTypes`).
    - For each path string read from `ai.getStringArray(PROPERTY_path)`, the value is now passed through `vr.resolve(...)` before `UrlPathMatcher.of(...)`.
    - For the `value()` shortcut (both branches: the `@RestOp` `"METHOD path"` space-split branch and the verb-annotation `else` branch), the trailing path token is also passed through `vr.resolve(...)`.
    - The auto-detected fallback path (`HttpUtils.detectHttpPath(method, httpMethod)`) is intentionally **not** SVL-resolved — it is framework-derived from the method name, not user input.
  - Empty-string handling: any path string whose entire SVL resolution yields `""` is **skipped** (no `UrlPathMatcher.of("")` is added). This matches how class-level `@Rest(paths)` drops empty pieces in the post-SVL comma-split (see `RestContext#splitPathsValue`).
  - Unresolved-variable handling: `vr.resolve("${unknown}")` returns the literal placeholder. Any such literal flows through to `UrlPathMatcher.of(...)` and either compiles to a pattern that won't match real requests (predictable 404) or, for the empty-after-resolve form `${name}` with no default, gets dropped before `UrlPathMatcher.of(...)`. No startup crash either way.
  - Added a focused javadoc paragraph on the `pathMatchers` Memoizer documenting the new behavior, the empty-string drop rule, and the auto-detected-fallback exemption.

- **`juneau-utest/src/test/java/org/apache/juneau/rest/RestOpContext_SvlInOpPath_Test.java`** (new file, license header + javadoc)
  - 5 `@Test` methods covering: default-branch `${name:default}`, system-property override, multi-path array (literal + SVL), `@RestOp(value="METHOD path")` verb-on-value form, and the unresolved-no-default no-crash invariant.
  - Uses one resource class per test rather than sharing a static `MockRestClient`, because `MockRestClient` caches `RestContext` by resource class and SVL substitution is captured at context-construction time — sharing a class across two tests with different `System.setProperty` snapshots would silently bind to the cached resolution.

- **`todo/FINISHED-99-svl-in-op-paths.md`** (this file).
- **`todo/FINISHED.md`** — new "Recent completions" entry pointing here.
- **`/Users/james.bognar/git/apache/juneau-docs/pages/release-notes/9.5.0.md`** — one bullet under `juneau-rest-server` describing the enhancement.

`todo/TODO.md` is **not** touched: TODO-99 was never an in-flight TODO bullet (it's a same-session enhancement triaged out of the TODO-78 family cleanup).

## How to use

Op-level path strings now accept Juneau's full SVL grammar. Use the `${name:default}` shortcut when you want a system-property-or-default fallback:

```java
@Rest(mixins=BasicJspResource.class)
public static class MyApp extends BasicRestServlet {}

@Rest(responseProcessors=JspViewRenderer.class)
public static class BasicJspResource {
    // Mounts at "/jsp/*" by default; override at runtime via -Djsp.path=ui or JSP_PATH env var.
    @RestGet(path="/${jsp.path:jsp}/*")
    public View hello() { return new JspView("hello.jsp"); }
}
```

The shorthand `${name:default}` is rewritten by `VarResolverSession#translateDollarBraceDefault` so the first top-level `:` becomes `,`, then dispatched to `PropertyVar` (the "P" var registered for the empty-name shortcut), which reads from `Settings.get()` — that walks per-thread and global stores, then service-loader sources, then system properties, then env vars. So the same annotation can be overridden via:

- System property: `-Djsp.path=ui`
- Environment variable: `JSP_PATH=ui` (after Settings normalizes case)
- A registered `Config` (when one is in the bean store): `[Section] jsp.path = ui`

The longer SVL forms still work and stay readable when the source is explicit:

- `@RestGet(path="/$S{my.route,default}/*")` — system property only.
- `@RestGet(path="/$E{MY_ROUTE,default}/*")` — environment variable only.
- `@RestGet(path={"/api", "/${admin.route:admin}"})` — multi-path array, mix literals with SVL.
- `@RestOp(value="GET /${legacy.route:legacy}")` — verb-on-value form (space-split).

## Test coverage

`juneau-utest/src/test/java/org/apache/juneau/rest/RestOpContext_SvlInOpPath_Test.java` — 5 `@Test` methods:

| Method | Resource class | What it asserts |
|---|---|---|
| `a01_singleSvlPath_defaultBranch_mountsAtDefault` | `A01_DefaultPath` | No sysprop set → `${a01.test.path:a01-default}` resolves to default; mount lands at `/a01-default/*`; the override path `/a01-override/*` 404s. |
| `a02_singleSvlPath_systemPropertyOverride_mountsAtOverride` | `A02_OverridePath` | `System.setProperty("a02.test.path", "a02-override")` BEFORE MockRest build → mount lands at `/a02-override/*`; the default path `/a02-default/*` 404s. Sysprop is cleared in a `try/finally`. |
| `b01_multiPathArray_literalAndSvl_bothMount` | `B_MultiPath` | `@RestGet(path={"/a", "/${a03.alt:b}"})` → both `/a` and `/b` mount; `/c` 404s. |
| `c01_verbOnValue_svlResolves` | `C_VerbOnValue` | `@RestOp(value="GET /${a04.path:legacy}")` (space-split) → mount lands at `/legacy`. |
| `d01_unresolvedVarNoDefault_doesNotCrashAtBuild` | `D_UnresolvedNoDefault` | `@RestGet(path="/${a05.missing}/foo")` with no sysprop and no default → resource builds without throwing; an unrelated request returns a predictable 404. |

`A01_DefaultPath` and `A02_OverridePath` are deliberately distinct classes (rather than sharing one) because `MockRestClient.preInit` caches `RestContext` by resource class; reusing the same class across the no-sysprop and sysprop-set tests would bind both to whichever resolution happened first.

## Verification

```
$ ./scripts/test.py
…
✅ Tests passed!
./scripts/test.py 2>&1  257.91s user 24.59s system 246% cpu 1:54.51 total
```

Wall-clock time: **114.59s (1:54.51)**. Baseline from the TODO-78 closeout summary was ~127s for clean-install + test, so this run came in slightly under baseline (likely because most modules were already up-to-date in the local repo from prior compile passes — full clean would be closer to baseline).

## Known limitations

1. **Auto-detected fallback path is not SVL-resolved.** When `@RestOp` / `@RestGet` / etc. declare neither `path()` nor a path-bearing `value()`, `RestOpContext` falls back to `HttpUtils.detectHttpPath(method, httpMethod)` — a framework-derived path computed from the Java method name (`getCustomers` → `/customers`). That path is intentionally **not** passed through `varResolver().resolve(...)`: it is not user-supplied annotation data, so SVL substitution there would only be a footgun (e.g. a method literally named `${something}` would be a Java-syntax error long before it became a routing question). The javadoc on `pathMatchers` documents this explicitly.

2. **Other op-level attributes are not SVL-resolved by this change.** Only `path[]` and `value()`-as-path are routed through the resolver. `method`, `produces`, `consumes`, `defaultRequestHeaders`, etc. each have their own resolution paths — some already SVL-resolved (see `findOpString`, `collectAnnotationMediaTypes`), some not. The user accepts that scope: if any other attribute turns out to need SVL too, file a follow-up TODO rather than expand this change.

3. **Caching implication for runtime overrides.** SVL is captured at `RestContext` construction time. Once the context is built, changing `System.setProperty(...)` does not retroactively rewrite mount paths — the same way it doesn't affect class-level `@Rest(paths)` resolution today. For deployer overrides this matches expectations (set the property at JVM start). For tests that *want* to vary the override, build a fresh `RestContext` per scenario (or use `overridingBeanStore` to bypass the per-class `MockRestClient` cache).

## Related

- Builds on **TODO-73** (runtime-overridable `@Rest(paths=...)`) — same SVL resolver, same `Settings`-backed lookup chain.
- Unblocks the post-TODO-78 Phase C2 mixin cleanup: `BasicJspResource` and the sibling view modules can now declare `@RestGet(path="/${myroute:default}/*")` so deployers can mount the JSP / Thymeleaf / Velocity / Freemarker mixins at non-default paths without subclassing.
- Does **not** affect class-level `@Rest(path)` / `@Rest(paths)` resolution — that path was already SVL-resolved, and no code in `RestContext` was touched.
