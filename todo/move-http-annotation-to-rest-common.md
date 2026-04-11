# Move `org.apache.juneau.http.annotation` to `juneau-rest-common`

## Goal

Move the `org.apache.juneau.http.annotation` package from `juneau-marshall` into `juneau-rest-common` so that
both the classic `RestClient` and the next-generation `NgRestClient` can share a single set of parameter
annotations (`@Header`, `@Path`, `@Query`, `@Content`, etc.).

Today the NG client has its own simplified copies (`ng.http.remote.Body`, `Header`, `Path`, `Query`).
Consolidating on the originals eliminates duplication and lets users annotate proxy interfaces once for
both clients.

## Current Location

`juneau-core/juneau-marshall/src/main/java/org/apache/juneau/http/annotation/`

### Files (32 total)

**Annotations (14)**:
`Contact`, `Content`, `FormData`, `HasFormData`, `HasQuery`, `Header`, `License`,
`Path`, `PathRemainder`, `Query`, `Request`, `Response`, `StatusCode`, `Tag`

**Annotation builder/utility classes (14)**:
`ContactAnnotation`, `ContentAnnotation`, `FormDataAnnotation`, `HasFormDataAnnotation`,
`HasQueryAnnotation`, `HeaderAnnotation`, `LicenseAnnotation`, `PathAnnotation`,
`PathRemainderAnnotation`, `QueryAnnotation`, `RequestAnnotation`, `ResponseAnnotation`,
`StatusCodeAnnotation`, `TagAnnotation`

**Constant classes (3)**: `CollectionFormatType`, `FormatType`, `ParameterType`

**Other**: `package-info.java`

## Key Obstacles

### 1. juneau-marshall uses its own annotations (circular dependency risk)

Three files inside `juneau-marshall` have **code** dependencies on `http.annotation` types:

| File | Annotations used in code |
|------|--------------------------|
| `httppart/HttpPartSchema.java` | Content, Header, FormData, Query, Path, PathRemainder, Response, StatusCode, HasQuery, HasFormData |
| `httppart/bean/RequestBeanMeta.java` | Request, Header, Query, FormData, Path, Content |
| `httppart/bean/ResponseBeanMeta.java` | Response, StatusCode, Header, Content, Query, FormData |

Four more files reference the annotations in Javadoc only (can be fixed with text edits).

Moving the annotations out of `juneau-marshall` means these three files must also move (or be
refactored) to avoid a circular dependency.

### 2. `*Annotation` builder classes have heavy juneau-marshall dependencies

The 14 builder classes import from:
- `org.apache.juneau.annotation.*` (`@Schema`)
- `org.apache.juneau.httppart.*` (`HttpPartSerializer`, `HttpPartParser`)
- `org.apache.juneau.oapi.*` (`OpenApiSerializer`, `OpenApiParser`)
- `org.apache.juneau.svl.*` (SVL variable resolver)
- `org.apache.juneau.json.*` (`JsonSerializer`)

These cannot move to `juneau-rest-common` without also moving their dependencies.

### 3. Wide usage across modules

~279 files import from this package across all modules.  Most use wildcard imports, so
a package-name change is a single-line edit per file — but the volume is high.

## Plan

### Phase 1 — Decouple annotations from juneau-marshall internals

The `*Annotation` builder classes and the `httppart/bean` classes are the main coupling points.
The builder classes exist primarily to support Juneau's dynamic annotation infrastructure
(`AnnotationWorkList`, `@ContextApply`, etc.).  The NG client does not use any of that machinery.

**Tasks:**

- [ ] Audit which `*Annotation` builder features are actually needed by `juneau-rest-common` consumers
      (header classes, response classes, remote annotations).  Likely answer: very few — those
      consumers use the annotations as plain markers, not the builder API.
- [ ] Determine whether `HttpPartSchema`, `RequestBeanMeta`, and `ResponseBeanMeta` should move
      to `juneau-rest-common` alongside the annotations, or whether those should stay in
      `juneau-marshall` and reference the annotations via a shared dependency.

### Phase 2 — Move the bare annotations

Move the 14 `@interface` files + 3 constant classes + `package-info.java` into
`juneau-rest-common` under the same package name (`org.apache.juneau.http.annotation`).

**Tasks:**

- [ ] Move annotation source files to `juneau-rest-common`
- [ ] Leave `*Annotation` builder classes in `juneau-marshall` (they depend on marshall internals
      and are only consumed by the classic annotation-processing pipeline)
- [ ] Verify `juneau-marshall` can still compile — it already depends on nothing in `juneau-rest`,
      so the annotations must be available via a dependency.  Options:
      - Add `juneau-rest-common` as a dependency of `juneau-marshall` (**creates a cycle — not viable**)
      - Move `HttpPartSchema` + `RequestBeanMeta` + `ResponseBeanMeta` to `juneau-rest-common` too
      - Keep the annotations in `juneau-marshall` as thin re-exports / keep a copy
- [ ] Update all import statements across the codebase (mostly mechanical — wildcard imports)

### Phase 3 — Move `httppart` types if needed

If Phase 2 reveals that `HttpPartSchema` and the bean-meta classes must also move:

- [ ] Assess whether `org.apache.juneau.httppart` can cleanly relocate to `juneau-rest-common`
- [ ] Move `HttpPartSchema`, `HttpPartSerializer`, `HttpPartParser`, and the `httppart/bean` package
- [ ] Update all references

### Phase 4 — Retire NG-specific parameter annotations

Once `org.apache.juneau.http.annotation.Header` (etc.) is available in `juneau-rest-common`:

- [ ] Update `RrpcInterfaceMeta` to read `org.apache.juneau.http.annotation.Header/Path/Query/Content`
      in addition to (or instead of) `ng.http.remote.Header/Path/Query/Body`
- [ ] Update `NgRemoteClient.buildRequest()` to handle both annotation sets
- [ ] Update `NgRemoteClient_Test` proxy interfaces to use the shared annotations
- [ ] Delete `ng.http.remote.Body`, `Header`, `Path`, `Query` once no code references them
- [ ] Verify all tests pass

### Phase 5 — Clean up

- [ ] Remove any stale Javadoc references
- [ ] Run full build + test suite
- [ ] Update release notes

## Risk Notes

- **Circular dependencies** are the primary risk.  Every change must be verified against the
  Maven dependency graph: `juneau-commons → juneau-marshall → juneau-rest-common → juneau-rest-client`.
- **Split-package** issues with OSGi bundles — both modules cannot export the same package.
  The annotations must live in exactly one module.
- **Binary compatibility** — existing compiled code referencing the old location will break.
  This is acceptable for a major release but should be documented in release notes.
