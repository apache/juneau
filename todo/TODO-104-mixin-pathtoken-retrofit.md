# TODO-104 — Mixin path retrofit to use `pathToken()` normalization

Retrofit all SVL-configurable mixin op-paths from FINISHED-101 to wrap their override token in `#{pathToken(...)}` so users can supply `"jsp"`, `"/jsp"`, `"/jsp/"`, `"/jsp/*"`, or `"jsp/*"` and the framework always produces a clean `/<token>/*` mount.

## Why

FINISHED-101 introduced SVL-configurable paths on 10 mixin resources, e.g.:

```java
@RestGet(path="/${juneau.jsp.path:jsp}/*", ...)
```

If a user sets `juneau.jsp.path=/jsp`, the path resolves to `//jsp/*` (double slash). If they set `juneau.jsp.path=jsp/*`, it resolves to `/jsp/*/*` (double wildcard). Both fail silently at routing time — the op stops matching and the user has to debug why their override broke. The framework should be liberal in what it accepts.

TODO-102's `pathToken(s)` function (added 2026-05-25 to the Phase D string catalog) solves this by stripping leading/trailing `/`, plus a trailing `/*` (or `*` after a `/`), while preserving multi-segment paths. Wrapping each configurable token in `#{pathToken(...)}` makes the override robust to whatever slash decoration the user includes.

## Scope

10 mixin classes with SVL-configurable paths from FINISHED-101. Hardcoded mixins (`BasicFaviconResource`, `BasicSeoResource`, `BasicWellKnownResource`) are out of scope — their paths are fixed-spec, no user override to normalize.

| Class | Current | Retrofitted |
|---|---|---|
| `BasicSwaggerResource` | `path="/${juneau.swagger.path:swagger}/*"` | `path="/#{pathToken(${juneau.swagger.path:swagger})}/*"` |
| `BasicSwaggerUiResource` | `path="/${juneau.swaggerui.path:swagger-ui}/*"` | `path="/#{pathToken(${juneau.swaggerui.path:swagger-ui})}/*"` |
| `BasicOpenApiResource` | `path="/${juneau.openapi.path:openapi}/*"` | `path="/#{pathToken(${juneau.openapi.path:openapi})}/*"` |
| `BasicRedocResource` | `path="/${juneau.redoc.path:redoc}/*"` | `path="/#{pathToken(${juneau.redoc.path:redoc})}/*"` |
| `BasicStaticFilesResource` | `path="/${juneau.staticfiles.path:static}/*"` | `path="/#{pathToken(${juneau.staticfiles.path:static})}/*"` |
| `BasicVersionResource` | `path="/${juneau.version.path:version}"` | `path="/#{pathToken(${juneau.version.path:version})}"` |
| `BasicEchoResource` | `path="/${juneau.echo.path:echo}/*"` | `path="/#{pathToken(${juneau.echo.path:echo})}/*"` |
| `BasicRouteIndexResource` | `path="/${juneau.routeindex.path:options}"` | `path="/#{pathToken(${juneau.routeindex.path:options})}"` |
| `BasicAdminResource` | `path="/${juneau.admin.path:admin}/..."` (3 ops) | `path="/#{pathToken(${juneau.admin.path:admin})}/..."` (3 ops) |
| `BasicJspResource` | `path="/${juneau.jsp.path:jsp}/*"` | `path="/#{pathToken(${juneau.jsp.path:jsp})}/*"` |

Per-class adjustments:
- `BasicVersionResource` / `BasicRouteIndexResource` — single-segment ops with no `/*` suffix. `pathToken` still appropriate to strip slashes.
- `BasicAdminResource` — three ops share the same `${juneau.admin.path:admin}` prefix; wrap each occurrence consistently.

## Dependencies

- **TODO-102 ✓ landed** — `pathToken(s)` function shipped in the FINISHED-102 catalog and is registered on `VarResolver.DEFAULT` via `defaultFunctions()`. See [`FINISHED-102-svl-scripting.md`](FINISHED-102-svl-scripting.md).
- **TODO-103 ✓ landed** — `RestOpContext.pathMatchers` already exercises the compiled `VarTemplate` seam (per Phase G #4 retrofit), so `pathToken` resolution happens once-per-context. See [`FINISHED-103-varresolver-template-compilation.md`](FINISHED-103-varresolver-template-compilation.md).

Unblocked. Can land any time.

## Phases

### Phase 1 — Path updates

1. Edit each of the 10 mixin classes per the table above. Single-character change per `path=...` declaration: replace `${...}` with `#{pathToken(${...})}`.
2. Update the Javadoc on each class's SVL-configurable-paths section to document the new override-input flexibility (e.g. "Override accepts bare token (`jsp`), absolute prefix (`/jsp`), trailing slash (`jsp/`), or wildcard suffix (`/jsp/*`) — all resolve to the same mount").

### Phase 2 — Test additions

Per-class `_SvlPathOverride_Test` already exists from FINISHED-101. Extend each with **5 new test cases** covering the equivalence of input variants:

```java
@Test void overrideBareToken()        { assertMounted("/admin/*", "juneau.admin.path", "admin"); }
@Test void overrideLeadingSlash()     { assertMounted("/admin/*", "juneau.admin.path", "/admin"); }
@Test void overrideTrailingSlash()    { assertMounted("/admin/*", "juneau.admin.path", "admin/"); }
@Test void overrideBothSlashes()      { assertMounted("/admin/*", "juneau.admin.path", "/admin/"); }
@Test void overrideWildcardSuffix()   { assertMounted("/admin/*", "juneau.admin.path", "/admin/*"); }
```

For the single-segment ops (`BasicVersionResource`, `BasicRouteIndexResource`), skip the `overrideWildcardSuffix` case since wildcard suffix doesn't apply.

Also add **one multi-segment override test per applicable class** to confirm `pathToken("/api/v1/*")` → `"api/v1"` works end-to-end:

```java
@Test void overrideMultiSegment() { assertMounted("/api/v1/admin/*", "juneau.admin.path", "/api/v1/admin/*"); }
```

### Phase 3 — Docs

1. Update `juneau-docs/pages/release-notes/9.5.0.md` with a TODO-104 entry under the SVL section noting that all SVL-configurable mixin paths now accept liberal input forms.
2. Cross-reference the `pathToken()` function entry in the new TODO-102 SVL-scripting topic page (added during TODO-102 Phase I).
3. Add an example to the mixin section of `9.5.0.md` showing the equivalent override forms.

## Acceptance criteria

- All 10 mixin classes use `#{pathToken(...)}` wrapping per the table.
- Each per-class `_SvlPathOverride_Test` extends to cover the 5 (or 4) input-form equivalence cases + 1 multi-segment case.
- `./scripts/test.py` green.
- Release-notes entry added.
- Manual smoke test: setting `juneau.jsp.path=/jsp/*` in `application.cfg` produces a mounted JSP resource at `/jsp/*` (not `/jsp/*/*`).

## Risks

- **Multi-segment ops with embedded variables.** None today; current mixin paths are simple. If future mixins use templates like `/${prefix}/${suffix}/*`, each variable position needs its own `pathToken` wrap. Document this convention in the FINISHED-104 archive.
- **`pathToken` over-stripping.** Edge case: a user sets `juneau.jsp.path=/jsp/**` (Servlet 3.0 deep-wildcard). `pathToken` strips to `/jsp/**` → `jsp/**` (since the trailing `**` is not `/*`). Acceptable — `**` is rare in Juneau path declarations and `pathToken` correctly leaves it alone.
- **Worker mid-flight on TODO-102+TODO-103.** This TODO is filed while the joint TODO-102/103 worker is still implementing. Once that worker reports complete with `pathToken(s)` in the catalog, this retrofit can dispatch in a focused follow-up.

## Related work

- **TODO-102** — `#{...}` scripting support; ships the `pathToken(s)` function in the Phase D string catalog. **HARD prereq.**
- **TODO-103** — `VarTemplate` compilation; soft dep — caching benefit only.
- **FINISHED-101** — Original SVL-configurable mixin path retrofit. This TODO closes the input-normalization gap that FINISHED-101 left open.
- **FINISHED-99** — SVL resolution in `@RestOp(path)`. The `pathToken(s)` call resolves at the same point in the pipeline as the existing `${...}` lookups.
