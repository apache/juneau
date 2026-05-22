# FINISHED-10: Move `org.apache.juneau.http.annotation` to `juneau-rest-common`

Archived from `TODO-10-move-http-annotation-to-rest-common.md` on 2026-05-20.

## What shipped

The `org.apache.juneau.http.annotation` package, its constant classes, and the `httppart.bean` bean-meta types all moved out of `juneau-marshall` into `juneau-rest-common`. Package names are unchanged, so existing source compiles without import edits; only the Maven coordinate moved. Every cleanup item in the original plan (Phases 1–5) was completed, mostly as a by-product of the surrounding 9.5 work — the marshall→rest-common annotation move (commit `d4792d3c`), the bean-layer split (TODOs 5 / 21 / 26 / 29), the rest-common split (FINISHED-42), and the next-gen REST client consolidation (FINISHED-11a / 11b / 41).

Verified end state as of the audit (2026-05-20):

- `juneau-rest-common/src/main/java/org/apache/juneau/http/annotation/` contains **22 files**: 14 annotations (`@Contact`, `@Content`, `@FormData`, `@HasFormData`, `@HasQuery`, `@Header`, `@License`, `@Path`, `@PathRemainder`, `@Query`, `@Request`, `@Response`, `@StatusCode`, `@Tag`) + 3 constant classes (`CollectionFormatType`, `FormatType`, `ParameterType`) + `package-info.java` + 4 surviving Swagger-utility companion classes (`ContactAnnotation`, `LicenseAnnotation`, `ResponseAnnotation`, `TagAnnotation`).
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/http/annotation/` contains **0 files** — no stragglers, no forwarder/shim classes.
- `juneau-marshall` source tree has **0 references** to `org.apache.juneau.http.annotation` (no imports, no Javadoc cross-references). The circular-dependency risk called out as the primary risk in the original plan never materialized.
- The 10 dynamic-annotation companion classes the plan inventoried (`ContentAnnotation`, `FormDataAnnotation`, `HasFormDataAnnotation`, `HasQueryAnnotation`, `HeaderAnnotation`, `PathAnnotation`, `PathRemainderAnnotation`, `QueryAnnotation`, `RequestAnnotation`, `StatusCodeAnnotation`) were deleted outright, not moved. The 4 retained companions are Swagger-generation utilities and now live in `juneau-rest-common` alongside the annotations.
- The annotations themselves are pure data annotations — no `on()` / `onClass()` / `@ContextApply` / `@Repeatable`. The XApply prerequisite split called out in the plan was completed before the move and the `serializer()` / `parser()` attributes were replaced by the new `@HttpPartMarshalling` annotation in `juneau-marshall`.
- `RequestBeanMeta` and `ResponseBeanMeta` moved to `juneau-rest-common/src/main/java/org/apache/juneau/httppart/bean/`. `HttpPartSchema` stayed in `juneau-marshall` and no longer references the moved annotations (the dependency on `@Header` / `@Query` / etc. was broken when the annotations became pure data types).
- The Phase-4 NG-specific parameter annotations (`org.apache.juneau.ng.http.remote.Body`, `Header`, `Path`, `Query`) were retired. The whole `org.apache.juneau.ng.*` namespace was folded into the canonical `org.apache.juneau.rest.client.*` / `org.apache.juneau.http.remote.*` packages as part of FINISHED-11a / 11b and FINISHED-41; the residual `org.apache.juneau.ng.*` references in Javadoc are descriptive prose only and resolve to nothing on the classpath.

## Files delivered

The move itself was committed as part of `d4792d3c` ("Moving schema annotations into juneau-common"). Related cleanup landed across the bean-layer split, rest-common split, and NG-client consolidation commits.

Verified delivery sites (no edits in this archival pass — read-only audit only):

- `juneau-rest/juneau-rest-common/src/main/java/org/apache/juneau/http/annotation/*.java` — 22 files.
- `juneau-rest/juneau-rest-common/src/main/java/org/apache/juneau/httppart/bean/RequestBeanMeta.java`, `ResponseBeanMeta.java`, plus the supporting `*PropertyMeta` and `MethodInfoUtils`.
- `juneau-docs/pages/release-notes/9.5.0.md` lines 1742–1770 — full release-notes section titled "HTTP Annotations Moved from `juneau-marshall`", "Removed Features", "NG Duplicate Annotations Retired", and "New `@HttpPartMarshalling` Annotation".

## Verification

- Glob: 22 files at `juneau-rest/juneau-rest-common/src/main/java/org/apache/juneau/http/annotation/`, 0 files at `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/http/annotation/`.
- Grep: 0 matches for `org\.apache\.juneau\.http\.annotation` (and 0 matches for `http\.annotation`) anywhere under `juneau-core/juneau-marshall/`.
- Release notes coverage confirmed in `juneau-docs/pages/release-notes/9.5.0.md` (sections under "juneau-rest-common > HTTP Annotations Moved from `juneau-marshall`").
- Cross-checked against sibling archives FINISHED-11a (NG client design), FINISHED-11b (NG coverage closeout), FINISHED-40 (HC 4.5 removal), FINISHED-41 (JDK transport promotion), FINISHED-42 (rest-common / rest-common-classic split). The rest-common split archive (FINISHED-42) explicitly accounts for the 22 `http.annotation` files in `juneau-rest-common`'s file-count table.
- No `./scripts/test.py` re-run was performed for this archival pass — verification relies on the existing green runs recorded in FINISHED-42 (48,208 tests passing) and FINISHED-48 (51,198 tests passing).

## Decisions (recorded 2026-05-20)

1. **Status = bucket A (fully obsolete / already complete).** Every phase of the original plan was delivered, either by the direct annotation-move commit or by a sibling 9.5 work item. There is no remaining cleanup that is uniquely attributable to TODO-10.
2. **No further migration-guide entry required.** Release notes already document the move and the import-transparency (`Package names are unchanged ... import statements do not need updating ... existing compiled code will need recompilation`). The original `[TODO-10]` bullet in `todo/TODO.md` claimed "remaining follow-on cleanup" — that claim is no longer accurate.
3. **The 4 retained `*Annotation` companion classes (`ContactAnnotation`, `LicenseAnnotation`, `ResponseAnnotation`, `TagAnnotation`) stay in `juneau-rest-common`.** They are pure Swagger-generation helpers, not dynamic-annotation builders, so they belong with the annotations rather than with marshall.

## Original plan

Source: filed pre-9.5 (date not recorded in the original file). Below is the unmodified plan text for archival.

### Goal

Move the `org.apache.juneau.http.annotation` package from `juneau-marshall` into `juneau-rest-common` so that both the classic `RestClient` and the next-generation `NgRestClient` can share a single set of parameter annotations (`@Header`, `@Path`, `@Query`, `@Content`, etc.).

Today the NG client has its own simplified copies (`ng.http.remote.Body`, `Header`, `Path`, `Query`). Consolidating on the originals eliminates duplication and lets users annotate proxy interfaces once for both clients.

### Prerequisites (completed)

#### XApply annotation split (done)

All marshall annotations (`@Schema`, `@Bean`, `@Json`, `@Xml`, etc.) have been split so that:

- The core `@X` annotation is a **pure data annotation** with no `on()` / `onClass()` / `@ContextApply`.
- A separate `@XApply` annotation (in marshall) carries targeting and `@ContextApply`.
- `AnnotationProvider.Builder.addRuntimeAnnotations()` handles unwrapping `@XApply` generically.

This means annotations like `@Schema` can now move to `juneau-commons` without pulling in marshall.

#### REST annotation targeting removal (done)

All REST annotations (`@Rest`, `@RestOp`, `@RestGet`, etc.) had their unused `on()` / `onClass()` removed. The `@ContextApply` appliers remain (they apply real REST config), but there is no dynamic targeting.

### Current Location

`juneau-core/juneau-marshall/src/main/java/org/apache/juneau/http/annotation/`

#### Files (32 total)

- Annotations (14): `Contact`, `Content`, `FormData`, `HasFormData`, `HasQuery`, `Header`, `License`, `Path`, `PathRemainder`, `Query`, `Request`, `Response`, `StatusCode`, `Tag`.
- Annotation builder / utility classes (14): `ContactAnnotation`, `ContentAnnotation`, `FormDataAnnotation`, `HasFormDataAnnotation`, `HasQueryAnnotation`, `HeaderAnnotation`, `LicenseAnnotation`, `PathAnnotation`, `PathRemainderAnnotation`, `QueryAnnotation`, `RequestAnnotation`, `ResponseAnnotation`, `StatusCodeAnnotation`, `TagAnnotation`.
- Constant classes (3): `CollectionFormatType`, `FormatType`, `ParameterType`.
- Other: `package-info.java`.

### Key Obstacles

#### 1. juneau-marshall uses its own annotations (circular dependency risk)

Three files inside `juneau-marshall` had **code** dependencies on `http.annotation` types:

| File | Annotations used in code |
| --- | --- |
| `httppart/HttpPartSchema.java` | Content, Header, FormData, Query, Path, PathRemainder, Response, StatusCode, HasQuery, HasFormData |
| `httppart/bean/RequestBeanMeta.java` | Request, Header, Query, FormData, Path, Content |
| `httppart/bean/ResponseBeanMeta.java` | Response, StatusCode, Header, Content, Query, FormData |

Four more files referenced the annotations in Javadoc only (could be fixed with text edits).

Moving the annotations out of `juneau-marshall` meant these three files had to also move (or be refactored) to avoid a circular dependency.

#### 2. `*Annotation` builder classes had heavy juneau-marshall dependencies

The 14 builder classes imported from:

- `org.apache.juneau.annotation.*` (`@Schema`)
- `org.apache.juneau.httppart.*` (`HttpPartSerializer`, `HttpPartParser`)
- `org.apache.juneau.oapi.*` (`OpenApiSerializer`, `OpenApiParser`)
- `org.apache.juneau.svl.*` (SVL variable resolver)
- `org.apache.juneau.json.*` (`JsonSerializer`)

These could not move to `juneau-rest-common` without also moving their dependencies.

**Note:** Now that `@Schema` is a pure data annotation (no `@ContextApply`), it could potentially move to `juneau-commons` first, which would reduce the coupling of these builder classes.

#### 3. Wide usage across modules

~279 files imported from this package across all modules. Most used wildcard imports, so a package-name change was a single-line edit per file — but the volume was high.

### Plan

#### Phase 1 — Audit annotation dependencies

The `*Annotation` builder classes and the `httppart/bean` classes were the main coupling points. The builder classes existed primarily to support Juneau's dynamic annotation infrastructure. The NG client did not use any of that machinery.

Tasks:

- Audit which `*Annotation` builder features were actually needed by `juneau-rest-common` consumers (header classes, response classes, remote annotations). Likely answer: very few — those consumers used the annotations as plain markers, not the builder API.
- Determine whether `HttpPartSchema`, `RequestBeanMeta`, and `ResponseBeanMeta` should move to `juneau-rest-common` alongside the annotations, or whether those should stay in `juneau-marshall` and reference the annotations via a shared dependency.
- Assess whether the http.annotation `*Annotation` builder classes still need `on()` / `onClass()` or whether they also need the XApply split (check if any of these annotations have targeting).

#### Phase 2 — Move the bare annotations

Move the 14 `@interface` files + 3 constant classes + `package-info.java` into `juneau-rest-common` under the same package name (`org.apache.juneau.http.annotation`).

Tasks:

- Move annotation source files to `juneau-rest-common`.
- Leave `*Annotation` builder classes in `juneau-marshall` (they depend on marshall internals and are only consumed by the classic annotation-processing pipeline).
- Verify `juneau-marshall` can still compile. Options:
  - Move `HttpPartSchema` + `RequestBeanMeta` + `ResponseBeanMeta` to `juneau-rest-common` too.
  - Keep the annotations in `juneau-marshall` as thin re-exports / keep a copy.
  - Add `juneau-rest-common` as a dependency of `juneau-marshall` (**creates a cycle — not viable**).
- Update all import statements across the codebase (mostly mechanical — wildcard imports).

#### Phase 3 — Move `httppart` types if needed

If Phase 2 revealed that `HttpPartSchema` and the bean-meta classes had to also move:

- Assess whether `org.apache.juneau.httppart` can cleanly relocate to `juneau-rest-common`.
- Move `HttpPartSchema`, `HttpPartSerializer`, `HttpPartParser`, and the `httppart/bean` package.
- Update all references.

#### Phase 4 — Retire NG-specific parameter annotations

Once `org.apache.juneau.http.annotation.Header` (etc.) was available in `juneau-rest-common`:

- Update `RrpcInterfaceMeta` to read `org.apache.juneau.http.annotation.Header` / `Path` / `Query` / `Content` in addition to (or instead of) `ng.http.remote.Header` / `Path` / `Query` / `Body`.
- Update `NgRemoteClient.buildRequest()` to handle both annotation sets.
- Update `NgRemoteClient_Test` proxy interfaces to use the shared annotations.
- Delete `ng.http.remote.Body`, `Header`, `Path`, `Query` once no code references them.
- Verify all tests pass.

#### Phase 5 — Clean up

- Remove any stale Javadoc references.
- Run full build + test suite.
- Update release notes.

### Risk Notes

- **Circular dependencies** were the primary risk. Every change had to be verified against the Maven dependency graph: `juneau-commons → juneau-marshall → juneau-rest-common → juneau-rest-client`.
- **Split-package** issues with OSGi bundles — both modules could not export the same package. The annotations had to live in exactly one module.
- **Binary compatibility** — existing compiled code referencing the old location would break. Acceptable for a major release; documented in release notes.

## References

- Direct move commit: `d4792d3c` — "Moving schema annotations into juneau-common".
- Sibling archives that delivered overlapping work:
  - `todo/FINISHED-11a-restclient-ng-design-plan.md` — initial NG client design.
  - `todo/FINISHED-11b-restclient-ng-coverage-closeout.md` — NG coverage closeout.
  - `todo/FINISHED-38-rename-rest-client-to-classic.md` — split of REST client into NG + classic.
  - `todo/FINISHED-40-remove-hc45-from-rest-common-and-server.md` — HC 4.5 removal foundation.
  - `todo/FINISHED-41-merge-java-httpclient-default-transport.md` — JDK HttpClient promoted to default.
  - `todo/FINISHED-42-split-rest-common-classic.md` — `juneau-rest-common` / `juneau-rest-common-classic` split (explicitly inventories the 22-file `http.annotation` directory now in `juneau-rest-common`).
- Release notes: `juneau-docs/pages/release-notes/9.5.0.md` lines 1742–1770.
- Conventions: `.cursor/skills/code-conventions/SKILL.md`, `AGENTS.md`.
