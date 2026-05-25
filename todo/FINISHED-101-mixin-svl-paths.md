# FINISHED-101 — Mixin op-paths SVL retrofit

## Summary

Building on FINISHED-99 (SVL resolution in `@RestOp(path)`), every Juneau-bundled mixin resource
whose mount path is a Juneau organizational/conventional default — not fixed by an external
specification — now declares its op-level `path` as `/${juneau.<role>.path:<default>}`. Deployers
can relocate the mount via system property, environment variable, or `Config` key without
subclassing. Three mixins whose paths are spec-fixed remain hardcoded with an in-class Javadoc
note explaining why.

## Audit table

| Mixin | Classification | SVL variable | Default mount | Notes |
|---|---|---|---|---|
| `BasicSwaggerResource` | configurable | `${juneau.swagger.path:api}` | `/api/*` | Swagger v2 doc |
| `BasicSwaggerUiResource` | configurable | `${juneau.swaggerui.path:swagger}` | `/swagger/*` | UI for Swagger v2 |
| `BasicOpenApiResource` | configurable | `${juneau.openapi.path:openapi}` | `/openapi/*`, `/openapi.json`, `/openapi.yaml` | Single variable reused across three ops on three methods (shared base; distinct extensions are part of the OpenAPI format pinning) |
| `BasicRedocResource` | configurable | `${juneau.redoc.path:redoc}` | `/redoc/*` | OpenAPI 3.1 Redoc UI |
| `BasicStaticFilesResource` | configurable | `${juneau.staticfiles.path:static}` | `/static/*` | Multi-path collapse — see below |
| `BasicVersionResource` | configurable | `${juneau.version.path:version}` | `/version` | Multi-path collapse — see below |
| `BasicEchoResource` | configurable | `${juneau.echo.path:echo}` | `/echo/*` | Multi-path collapse — see below |
| `BasicAdminResource` | configurable | `${juneau.admin.path:admin}` | `/admin/threads`, `/admin/heap`, `/admin/cache/flush`, `/admin/ratelimit` | Single prefix variable reused across four distinct ops on four methods (no multi-path arrays) |
| `BasicRouteIndexResource` | configurable | `${juneau.routeindex.path:options}` | `/options` | Multi-path collapse — see below |
| `BasicJspResource` | configurable | `${juneau.jsp.path:jsp}` | `/jsp/*` | Lives in `juneau-rest-server-view-jsp` |
| `BasicFaviconResource` | **hardcoded** | — | `/favicon.ico` | Browser convention + WHATWG HTML `rel="icon"` default — browsers fetch from the site root regardless of routing rewrites. |
| `BasicSeoResource` | **hardcoded** | — | `/robots.txt`, `/sitemap.xml` | RFC 9309 (Robots Exclusion Protocol) + sitemaps.org. Two distinct ops on two methods — not collapsed because they are semantically different endpoints, not aliases for the same endpoint. |
| `BasicWellKnownResource` | **hardcoded** | — | `/.well-known/security.txt` | RFC 8615 (well-known URIs) + RFC 9116 (security.txt). The `/.well-known/` prefix is the discovery convention; the filename suffix is the protocol-defined registry key. |

## Naming convention

`${juneau.<role>.path:<default>}`. The `juneau.` namespace prefix scopes the system-property /
env-var / Config key away from generic names like `swagger.path`. The `<role>` token is the
mixin's domain (`swagger`, `openapi`, `staticfiles`, etc.). The `<default>` is the previous
hardcoded mount value with the leading slash and any trailing `/*` stripped, so the SVL fallback
exactly reproduces pre-9.5.0 behavior when no override is supplied.

Where a mixin owns a small group of related ops on different methods sharing a common prefix
(`BasicAdminResource`, `BasicOpenApiResource`), a single SVL variable controls the shared
segment and the per-method `{path,extension}` tail is preserved verbatim.

## Multi-path collapse

The original development snapshots of four mixins declared multiple paths in a single op
annotation's `path={...}` array — historical "convenience alias" defaults from before the SVL
mechanism existed. With SVL the convenience-alias rationale is gone: one default plus
override capability is sufficient. These four classes have been collapsed to a single
SVL-configurable path per op.

| Mixin | Op | Original `path={...}` | Collapsed default | Removed alias(es) |
|---|---|---|---|---|
| `BasicStaticFilesResource` | `getStaticFile` / `headStaticFile` | `{"/static/*","/htdocs/*"}` | `/${juneau.staticfiles.path:static}/*` | `/htdocs/*` |
| `BasicVersionResource` | `getInfo` | `{"/version","/info","/about"}` | `/${juneau.version.path:version}` | `/info`, `/about` |
| `BasicEchoResource` | `echo` | `{"/echo/*","/debug/echo/*"}` | `/${juneau.echo.path:echo}/*` | `/debug/echo/*` |
| `BasicRouteIndexResource` | `getRoutes` | `{"/options","/routes"}` | `/${juneau.routeindex.path:options}` | `/routes` |

In all four cases the variable name was simplified at collapse time: the abandoned dual-namespace
form (e.g. `${juneau.staticfiles.static.path:static}` + `${juneau.staticfiles.htdocs.path:htdocs}`)
becomes the flat `${juneau.staticfiles.path:static}`, because there is now only one path to
configure.

**Why this is principled, not aggressive deprecation:**

1. Every secondary alias is still reachable, just one sysprop / env-var away. The deployer who
   was relying on `/htdocs/*` flips `-Djuneau.staticfiles.path=htdocs` and gets back to the
   previous URL.
2. The classpath search root in `BasicStaticFiles` still walks both `static/` and `htdocs/`
   directories at the JAR-resource layer, so collapsing the URL alias does not change which
   files are reachable — only how the deployer asks for them.
3. A deployer who genuinely needs both URLs simultaneously can compose a second instance of the
   mixin with a different override, or write a one-method subclass with their own
   `@RestGet(path="/static/*")` alongside the configurable mount.

**Why "multiple ops on different methods" was NOT collapsed:**

`BasicSeoResource` (`/robots.txt` + `/sitemap.xml`) and `BasicAdminResource` (`/admin/threads` +
`/admin/heap` + `/admin/cache/flush` + `/admin/ratelimit`) declare each path on a separate
handler method — they are semantically distinct ops, not alternative aliases for the same op.
`BasicSeoResource` stays hardcoded per spec; `BasicAdminResource` already had a clean
shared-prefix variable that worked for all four ops, so no collapse was needed.

## Hardcoded mixin rationale

Three mixins keep their hardcoded mount paths and now carry an explicit Javadoc section
explaining why a runtime override would be wrong:

- **`BasicFaviconResource`** — `/favicon.ico` is fixed by browser convention and the WHATWG HTML
  spec's default `rel="icon"` lookup. Browsers fetch `/favicon.ico` from the site root
  regardless of application-level routing rewrites, so a runtime mount-path override would have
  no practical effect.
- **`BasicSeoResource`** — `/robots.txt` is fixed by RFC 9309 (Robots Exclusion Protocol), which
  prescribes the policy file at the site root. `/sitemap.xml` is fixed by the sitemaps.org
  protocol (and search engines configured to look for the file at that exact path).
- **`BasicWellKnownResource`** — `/.well-known/security.txt` is fixed by RFC 8615 (Well-Known
  URIs) + RFC 9116 (security.txt). Both RFCs are explicit that the `/.well-known/` prefix is the
  discovery convention and the filename suffix is the protocol-defined registry key.

Each hardcoded class gained a `<h5 class='section'>Hardcoded mount path:</h5>` Javadoc section
that cites the relevant RFC / spec / convention, so future contributors are not tempted to
"complete" the SVL retrofit by extending it to these classes.

## Tests

Each configurable mixin now has a `*_SvlPathOverride_Test` in `juneau-utest`:

| Test | Override scenario |
|---|---|
| `BasicSwaggerResource_SvlPathOverride_Test` | `juneau.swagger.path=custom-api` |
| `BasicSwaggerUiResource_SvlPathOverride_Test` | `juneau.swaggerui.path=custom-swagger` |
| `BasicOpenApiResource_SvlPathOverride_Test` | `juneau.openapi.path=custom-openapi` (single override relocates all three op-paths) |
| `BasicRedocResource_SvlPathOverride_Test` | `juneau.redoc.path=custom-redoc` |
| `BasicStaticFilesResource_SvlPathOverride_Test` | `a01` generic relocate; `a02` migration scenario — `juneau.staticfiles.path=htdocs` exercises the collapsed-alias migration path |
| `BasicVersionResource_SvlPathOverride_Test` | `a01` generic relocate; `a02` `juneau.version.path=info` migration |
| `BasicEchoResource_SvlPathOverride_Test` | `a01` generic relocate; `a02` `juneau.echo.path=debug/echo` migration |
| `BasicAdminResource_SvlPathOverride_Test` | `juneau.admin.path=ops` (single override relocates all four ops; uses allow-all `RestGuardList` so `DenyAllGuard` doesn't short-circuit) |
| `BasicRouteIndexResource_SvlPathOverride_Test` | `a01` generic relocate; `a02` `juneau.routeindex.path=routes` migration |
| `BasicJspResource_SvlPathOverride_Test` | `juneau.jsp.path=views` (uses the MockRest 500-on-no-JSP-engine signal as the "route installed" assertion) |

Each test uses a *fresh* inner-class resource per scenario because `MockRestClient` caches
`RestContext` per resource class — SVL substitution is captured once at context-construction
time, so sharing a resource class across scenarios would silently mask the override. All tests
also restore the system property in a `try/finally` block so unrelated tests are not
disturbed.

**AsMixin test adjustments.** Three of the existing `*_AsMixin_Test` files contained explicit
secondary-alias assertions (e.g. `BasicStaticFilesResource_AsMixin_Test#a02_htdocsPathServesSameFile`)
that were exercising the pre-collapse dual default. Those assertions have been rewritten as
"legacy alias not mounted by default" 404 checks with a comment pointing at FINISHED-101 and
the matching `*_SvlPathOverride_Test`. This preserves the original intent (regression cover
for the alias surface) while reflecting the new single-default contract:

- `BasicStaticFilesResource_AsMixin_Test` — three `/htdocs/*` cases removed (now covered by
  `*_SvlPathOverride_Test#a02`); the GET / HEAD / 404 assertions on `/static/*` were
  preserved and renumbered.
- `BasicVersionResource_AsMixin_Test#a02` — was `versionInfoAboutAreSynonyms` (asserting
  `/version` == `/info` == `/about`); now `legacyAliasesNotMountedByDefault` asserts 404 on
  `/info` and `/about`. The migration path is covered by `*_SvlPathOverride_Test#a02`.
- `BasicEchoResource_AsMixin_Test#a02` / `#b02` — were checking `/debug/echo/*` mounted by
  default; now both check 404. The migration path is covered by
  `*_SvlPathOverride_Test#a02`.
- `BasicRouteIndexResource_AsMixin_Test#a02` — was `routesIsSynonymForOptions`; now
  `legacyRoutesAliasNotMountedByDefault` asserts 404 on `/routes`. Migration covered by
  `*_SvlPathOverride_Test#a02`. The `a04_excludesItself` assertion was also trimmed to drop
  its `/routes` self-reference check (no longer applicable).

## Javadoc surface

Configurable mixins gained a `<h5 class='section'>Configurable mount path:</h5>` section that
documents the SVL variable name, the three resolver sources (sysprop / env-var / Config), and
points to FINISHED-99 for the full resolution chain. The four collapsed-alias mixins also gained
a `<b>Migration note (9.5.0):</b>` paragraph spelling out which alias was removed and how to
preserve it via the SVL override.

Hardcoded mixins gained a `<h5 class='section'>Hardcoded mount path:</h5>` section citing the
specification.

## Files touched

**Mixin sources (no behavior change for default deployments):**

- `juneau-rest-server/.../docs/BasicSwaggerResource.java`
- `juneau-rest-server/.../docs/BasicSwaggerUiResource.java`
- `juneau-rest-server/.../docs/BasicOpenApiResource.java`
- `juneau-rest-server/.../docs/BasicRedocResource.java`
- `juneau-rest-server/.../staticfile/BasicStaticFilesResource.java`
- `juneau-rest-server/.../convention/BasicVersionResource.java`
- `juneau-rest-server/.../convention/BasicFaviconResource.java` (Javadoc only — hardcoded note)
- `juneau-rest-server/.../convention/BasicSeoResource.java` (Javadoc only — hardcoded note)
- `juneau-rest-server/.../convention/BasicWellKnownResource.java` (Javadoc only — hardcoded note)
- `juneau-rest-server/.../ops/BasicEchoResource.java`
- `juneau-rest-server/.../ops/BasicAdminResource.java`
- `juneau-rest-server/.../ops/BasicRouteIndexResource.java`
- `juneau-rest-server-view-jsp/.../view/jsp/BasicJspResource.java`

**New tests (`juneau-utest`):**

- `BasicSwaggerResource_SvlPathOverride_Test`
- `BasicSwaggerUiResource_SvlPathOverride_Test`
- `BasicOpenApiResource_SvlPathOverride_Test`
- `BasicRedocResource_SvlPathOverride_Test`
- `BasicStaticFilesResource_SvlPathOverride_Test`
- `BasicVersionResource_SvlPathOverride_Test`
- `BasicEchoResource_SvlPathOverride_Test`
- `BasicAdminResource_SvlPathOverride_Test`
- `BasicRouteIndexResource_SvlPathOverride_Test`
- `BasicJspResource_SvlPathOverride_Test`

**Existing AsMixin tests updated:**

- `BasicStaticFilesResource_AsMixin_Test`
- `BasicVersionResource_AsMixin_Test`
- `BasicEchoResource_AsMixin_Test`
- `BasicRouteIndexResource_AsMixin_Test`

**Release notes:**

- `juneau-docs/pages/release-notes/9.5.0.md` — new section under the TODO-99 entry.

## Cross-references

This work landed as a follow-up to FINISHED-99 (SVL substitution in op-paths) and amends the
mixin packs introduced in:

- FINISHED-74 (api-docs mixin pack — `BasicSwaggerResource`, `BasicSwaggerUiResource`,
  `BasicOpenApiResource`, `BasicRedocResource`).
- FINISHED-75 (static-files mixin — `BasicStaticFilesResource`).
- FINISHED-76 (convention mixin pack — `BasicFaviconResource`, `BasicSeoResource`,
  `BasicVersionResource`, `BasicWellKnownResource`).
- FINISHED-77 (ops / introspection mixin pack — `BasicEchoResource`, `BasicAdminResource`,
  `BasicRouteIndexResource`).
- FINISHED-78 (JSP view mixin module — `BasicJspResource`).

A short "FINISHED-101 follow-up" section has been appended to each of those archive files
pointing at this archive.
