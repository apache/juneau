# TODO-61: RFC 7807 / 9457 Problem-Details server-side wiring

## Outcome summary

- Completed Phases 1-3 for RFC 7807/9457 server-side wiring, including the `ProblemDetailsProcessor`, per-resource and per-op `problemDetails` opt-in semantics, `ProblemException`, and mapper/localization SPI seams.
- Added and validated user-facing docs in `juneau-docs` (`pages/release-notes/9.5.0.md`, `pages/topics/10.20a.RestServerProblemDetails.md`, and `sidebars.ts`) that were deferred during Phase 2.
- Left Phase 4 (`application/problem+xml`) as intentionally deferred optional follow-on scope, matching the original out-of-scope declaration for v1.

Source: split out of TODO-18 brainstorm on 2026-05-22 (the recommended #1 pick).

## Locked decisions (2026-05-22)

Recorded after the Phase 0 inventory pass. These are the answers to the 8 open
questions in this plan; subsequent phases must respect them.

- **Q1 Adapter location** — The `Problem.fromException(BasicHttpException)` helper lives in **`juneau-rest-common`** as `org.apache.juneau.bean.rfc7807.adapter.ProblemAdapters#fromException(BasicHttpException)` (static helper class). The package name keeps it discoverable from the bean module's javadoc; the class lives in `juneau-rest-common` because that's the layer where the bidirectional dep is allowed. `juneau-rest-common/pom.xml` gains a dependency on `juneau-bean-rfc7807`.
- **Q2 Annotation name** — The opt-in attribute is **`problemDetails`** as `String problemDetails() default ""` on `@Rest` (and later on `@RestGet`/etc. in Phase 2). Tri-state: `"true"` enables, `"false"` disables, `""` inherits from the next-most-derived `@Rest` in the hierarchy.
- **Q3 Throw-path seam** — **Shape 3: inline branch in `RestContext.handleError`** ahead of the existing `text/plain` write. When `isProblemDetails()` is true and the error is a `BasicHttpException` (or wraps one), emit `application/problem+json` body via `ProblemAdapters.fromException(...)` + `JsonSerializer.DEFAULT`. The success path remains driven by the always-in-chain `ProblemDetailsProcessor`.
- **Q4 `ProblemException`** — **Ship in v1.** Lives in `juneau-bean-rfc7807` (the bean module stays clean of any `juneau-rest-server` dep). The processor unwraps via `instanceof ProblemException`.
- **Q5 `Accept` policy** — **(A) errors / (B) success**:
  - Error path: emits `application/problem+json` whenever `problemDetails="true"`, *regardless of* the client `Accept` (the RFC encourages this — the alternative leaves the client with a 4xx/5xx and a `text/plain` stack trace).
  - Success path: honors client `Accept` strictly; the processor only emits `application/problem+json` if `Accept` matches it or `*/*`. Otherwise pass-through (`return NEXT`).
- **Q6 Status precedence** — **Option C**: if `Problem.status` is non-null, the processor calls `res.setStatus(problem.getStatus())`; otherwise it leaves the existing response status alone (`RestSession.run()` normalizes `0` to `200`, so `@RestPost`/etc. defaults still flow through unchanged).
- **Q7 `Problem.type` default** — **Do not synthesize `about:blank`.** Serialize the bean as-is — a `null` `type` field is omitted from the JSON output by the `@Marshalled` bean machinery. This preserves the absent-vs-explicit distinction (see `Problem_RoundTrip_Test.b04_typeAbsent_doesNotAppearInJson`).
- **Q8 Localization** — **Out of scope** for this TODO. Not pursued in any of Phases 1–4. If a `Messages`-driven translation pass is needed later, file a sibling TODO.

## Goal

Add server-side wiring so that any Juneau REST resource can emit `application/problem+json` (RFC 7807 / 9457) responses, both reactively (translate uncaught `BasicHttpException` / unchecked `Throwable` into `Problem` payloads when the client asks for it) and declaratively (`@RestOp` methods that return `Problem` directly serialize to `application/problem+json` with the correct `Content-Type` and status code). Provide a thin `Problem.fromException(BasicHttpException)` adapter in `juneau-rest-common` so the `juneau-bean-rfc7807` module stays free of any `juneau-rest-common` dependency.

The end-state developer experience is:

```java
@Rest(path="/orders", problemDetails=true)  // opt-in flag
public class OrderResource {

    @RestGet("/{id}")
    public Order get(@Path long id) {
        throw new NotFound("Order {0} not found", id);  // → 404 application/problem+json
    }

    @RestPost
    public Problem create(Order in) {
        if (in.balance < in.amount)
            return Problem.fromStatus(403, "Insufficient credit", "Balance "+in.balance+" < amount "+in.amount)
                .setType(URI.create("https://example.com/probs/out-of-credit"))
                .set("balance", in.balance);
        return null;
    }
}
```

## Why now

- `juneau-bean-rfc7807` shipped in 9.5.0 (`FINISHED-45-juneau-bean-rfc7807.md`) and its archive explicitly parks this work: *"Wiring a `@Rest` exception handler that auto-emits Problem for every uncaught BasicHttpException… belongs in juneau-rest-server (not the bean module) and is a separate TODO."*
- `juneau-rest-server` was decoupled from `org.apache.http.*` in TODO-40 (`FINISHED-40-remove-hc45-from-rest-common-and-server.md`) and `BasicHttpException` gained the full fluent-setter surface, so the adapter is one-shot: `new Problem().setStatus(e.getStatusCode()).setTitle(e.getStatusLine().getReasonPhrase()).setDetail(e.getMessage())`.
- `ContentType.APPLICATION_PROBLEM_JSON` and `…APPLICATION_PROBLEM_XML` constants already live in `juneau-rest/juneau-rest-common/.../http/header/ContentType.java`.
- The `ResponseProcessorList` slot is open — no architectural prerequisite. No dependency on TODO-20 or TODO-35.

## Scope

**In scope (v1):**

- New `Problem.fromException(BasicHttpException)` adapter — lands in **`juneau-rest-common`** (so the `juneau-bean-rfc7807` module stays clean of any `juneau-rest-common` dep, per the locked-in decision in `FINISHED-45-juneau-bean-rfc7807.md`).
- New `org.apache.juneau.rest.processor.ProblemDetailsProcessor` slotted into `ResponseProcessorList` ahead of `ThrowableProcessor`. When the request `Accept` matches `application/problem+json` *and* the active response carries a `BasicHttpException` or a `Problem` bean, it serializes a `Problem` body with the right `Content-Type` and HTTP status.
- New opt-in flag on `@Rest` / `@RestOp` (`problemDetails=true`) that registers the processor and bumps `application/problem+json` to the default `Accept` priority for error responses on that resource.
- Direct support for `@RestOp` methods returning `Problem` — the processor sets `Content-Type: application/problem+json`, sets the HTTP status from `Problem.getStatus()` (defaulting to 200 when null), and serializes through the existing `JsonSerializer.DEFAULT`.
- New `org.apache.juneau.bean.rfc7807.ProblemException` (lives in `juneau-bean-rfc7807`, optional convenience) — a `RuntimeException` that wraps a `Problem` and lets handlers `throw new ProblemException(problem)` without manually building a `BasicHttpException`. **Locked decision needed:** whether to ship this in v1 or defer (recommend v1).
- Tests: unit + `MockRestClient`-based integration in `juneau-utest`, mirroring the `Problem_RoundTrip_Test` shape from `FINISHED-45-*`.
- Docs: a new release-notes entry under `juneau-rest-server` in `juneau-docs/pages/release-notes/9.5.0.md` (or 9.5.1 if open), plus a new topic page under `juneau-docs/pages/topics/` (slug `RestServerProblemDetails`).

**Explicitly out of scope (v1):**

- `application/problem+xml` rendering — the `ContentType.APPLICATION_PROBLEM_XML` constant exists but the bean module hasn't been built; defer to a sibling TODO.
- RFC 7807 §3.2 typed-extension *registration* (callers can still set extension fields via `Problem.set(key, value)` — that ships in the bean today).
- Localization of `title` / `detail` via Juneau message bundles — defer.
- Auto-translation of Jakarta Validation `ConstraintViolationException` into `Problem.errors[]` — that's part of **TODO-68** (Bean Validation integration); the hooks here will be designed to make it a one-class follow-on.
- Bridging into reactive / `CompletableFuture` return types — orthogonal to **TODO-70**.

## Phased steps

### Phase 0 — inventory & seam confirmation (read-only, 1–2 hours)

1. Re-read `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/processor/{ResponseProcessorList,ThrowableProcessor,HttpBodyProcessor,SerializedPojoProcessor}.java` to confirm processor ordering and the `int process(RestOpSession)` contract.
2. Confirm `RestResponse.getException()` / `RestResponse.setException(Throwable)` is the canonical seat for the in-flight exception (it is — `RestResponse.java` exposes both).
3. Confirm `ContentType.APPLICATION_PROBLEM_JSON` is reachable from `juneau-rest-server` after TODO-40/42 (`juneau-rest-server` depends on `juneau-rest-common`, which owns the constant — verified).
4. Re-read `FINISHED-45-juneau-bean-rfc7807.md` "Out of scope" line items to make sure nothing has shifted.
5. Decide whether the opt-in is `@Rest(problemDetails=true)` (annotation flag), a `@Bean ProblemDetailsProcessor` registration, or both. **Recommend both** (the annotation flag is the on-ramp; the bean is the override seam).

### Phase 1 — adapter + processor (no annotation changes) — DONE 2026-05-22

Lands the core capability. Phase 1 *did* ship the `problemDetails` attribute on `@Rest` (needed
as the opt-in seam for the throw path); the per-op `@RestGet`/`@RestPost`/etc. fanout is Phase 2.

1. [x] Adapter `org.apache.juneau.bean.rfc7807.adapter.ProblemAdapters#fromException(BasicHttpException)` lives in `juneau-rest-common`. `juneau-rest-common/pom.xml` got a `juneau-bean-rfc7807` dep.
2. [x] `org.apache.juneau.rest.processor.ProblemDetailsProcessor` lives in `juneau-rest-server`. Detection flow:
   - Content is `Problem` → honor `Accept`, serialize. `FINISHED`.
   - Content is `ProblemException` → unwrap → honor `Accept`, serialize. `FINISHED`.
   - Content is `BasicHttpException` → gated by `isProblemDetails()` (`@Rest(problemDetails="true")`); on error path **ignores `Accept`** (Q5(A)), adapts + serializes. `FINISHED`.
   - Otherwise → `NEXT`.
   - **Note:** The processor sits in the chain *after* `ThrowableProcessor` (not ahead — `ThrowableProcessor` only sets a `Thrown` header and returns `NEXT`, so order doesn't matter for the success path, but post-Throwable ordering keeps the `Thrown` header on the wire even when problem-details takes over the body).
3. [x] Wired into `DefaultConfig.responseProcessors` ahead of `HttpResponseProcessor` (after `ThrowableProcessor`). No regression in the baseline chain.
4. [x] Tests in `juneau-utest`:
   - `ProblemAdapters_Test` (19 tests) — adapter across 8 representative `BasicHttpException` subclasses (BadRequest/Unauthorized/Forbidden/NotFound/Conflict/InternalServerError/NotImplemented/ServiceUnavailable) × {with-message, with-cause, no-message, null-cause}; null-input safety; `getMessage()`-vs-reason-phrase fallback; verifies `type`/`instance` never synthesized.
   - `ProblemException_Test` (9 tests) — constructor, factory, throw/catch round-trip, message-from-detail-then-title fallback, extension-field preservation.
   - `ProblemDetailsProcessor_Test` (10 tests) — `Problem` return → problem+json; `String` return → unchanged; `Accept` strict on success (Q5-B); `Problem.status` precedence (Q6-C); null status leaves default 200; null `type` omitted (Q7); `ProblemException` return → unwrap; extension fields serialized.
   - `Rest_ProblemDetails_OptIn_Test` (6 tests) — end-to-end via `MockRestClient`. Throw `NotFound("Order {0} not found", id)` with opt-in → 404 + `application/problem+json`; Q5(A) Accept-html still emits problem+json; Q7 null type omitted; return `Problem.fromStatus(403, ...).set("balance", 30)` → custom-status problem+json with extension; **no opt-in (regression bar)** → text/plain; explicit `problemDetails="false"` → text/plain.
   - **Regression bar passed**: `RestOp_Throws_Test` (8) and `Rest_PredefinedStatusCodes_Test` (4) pass without modification.
5. [x] Coverage assessed via targeted tests; both `ProblemAdapters` and `ProblemException` are exhaustively covered. `ProblemDetailsProcessor` coverage is via end-to-end paths through `MockRestClient`.
6. [x] No release-notes entry yet — Phase 2 ships the per-op annotation fanout and is the user-visible cut line.

**Files added/modified (~340 LOC src, ~600 LOC test):**

| File | LOC | Description |
| --- | --- | --- |
| `juneau-rest/juneau-rest-common/pom.xml` | +5 | Added `juneau-bean-rfc7807` dep. |
| `juneau-rest/juneau-rest-common/src/main/java/org/apache/juneau/bean/rfc7807/adapter/ProblemAdapters.java` | ~80 | New: static `fromException(BasicHttpException)`. |
| `juneau-rest/juneau-rest-common/src/main/java/org/apache/juneau/bean/rfc7807/adapter/package-info.java` | ~30 | New: ASF + javadoc. |
| `juneau-bean/juneau-bean-rfc7807/src/main/java/org/apache/juneau/bean/rfc7807/ProblemException.java` | ~80 | New: `RuntimeException` carrying a `Problem`. |
| `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/processor/ProblemDetailsProcessor.java` | ~130 | New: success + error path; opt-in gate on `BasicHttpException`; `Accept` policy. |
| `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/config/DefaultConfig.java` | +1 | Inserted `ProblemDetailsProcessor.class` in `responseProcessors`. |
| `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/RestServerConstants.java` | +3 | `PROPERTY_problemDetails` constant. |
| `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/annotation/Rest.java` | +30 | `String problemDetails() default ""` + Javadoc; `noInherit` doc update. |
| `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/annotation/RestAnnotation.java` | +20 | Builder + Object fanout for `problemDetails`. |
| `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/RestContext.java` | +50 | `isProblemDetails()` memoizer + accessor; `handleError` opt-in branch + `writeProblemDetailsBody` helper (backstop for exceptions escaping `s.run()`). |
| `juneau-utest/src/test/java/org/apache/juneau/bean/rfc7807/ProblemAdapters_Test.java` | ~200 | 19 tests. |
| `juneau-utest/src/test/java/org/apache/juneau/bean/rfc7807/ProblemException_Test.java` | ~110 | 9 tests. |
| `juneau-utest/src/test/java/org/apache/juneau/rest/processor/ProblemDetailsProcessor_Test.java` | ~210 | 10 tests via `MockRestClient`. |
| `juneau-utest/src/test/java/org/apache/juneau/rest/Rest_ProblemDetails_OptIn_Test.java` | ~150 | 6 tests via `MockRestClient`. |

**Design note (recorded for Phase 2/3 implementers):** The processor's `BasicHttpException`
branch is the *primary* error-path seam in practice. `RestOpInvoker.invoke()` catches
`InvocationTargetException` and stores the thrown exception as response *content* via
`res.setContent(...)`; the framework then runs the processor chain (which includes our
processor) rather than escaping to `RestContext.handleError`. The handleError branch (locked
decision Q3) remains as a backstop for the few exceptions that *do* escape `s.run()` — e.g.
infrastructure errors in `createSession` / `startCall`. Both seams are in place and behave
identically with respect to opt-in + Accept policy.

**Test results (Phase 1):**

| Run | Tests run | Failures | Errors | Skipped |
| --- | --- | --- | --- | --- |
| Build (`./scripts/test.py -b`) | n/a | n/a | n/a | n/a |
| Targeted (`mvn -pl juneau-utest -Dtest=...`) | 78 | 0 | 0 | 0 |
| Full (`./scripts/test.py -v`) | **126,735** | **0** | **0** | **20** |

Baseline before Phase 1: 126,691 / 0 / 0 / 20 — exact +44 growth matches the new tests.

### Phase 2 — per-op annotation fanout + op-level wiring — DONE 2026-05-22

Lands the discoverable per-method on-ramp.

1. [x] `String problemDetails() default ""` added to all seven method-level annotations:
   `@RestOp`, `@RestGet`, `@RestPost`, `@RestPut`, `@RestPatch`, `@RestDelete`, `@RestOptions`.
   Tri-state: `"true"` enables, `"false"` disables, `""` inherits from `@Rest(problemDetails)`.
2. [x] Builder + Object fanout in each `*Annotation.java` (`RestOpAnnotation`, `RestGetAnnotation`,
   `RestPostAnnotation`, `RestPutAnnotation`, `RestPatchAnnotation`, `RestDeleteAnnotation`,
   `RestOptionsAnnotation`) — `private String problemDetails = "";` field, fluent setter, final
   field on the inner `Object` class, getter override.
3. [x] Op-level resolution in `RestOpContext`:
   - Memoizer `problemDetails` resolves explicit `"true"` / `"false"` from the op annotation chain
     via `findOpString(PROPERTY_problemDetails)`; falls back to `restContext().isProblemDetails()`
     when the op is empty (`""`) and inheritance is allowed (`isInherited(PROPERTY_problemDetails)`),
     defaulting to `false` at the root.
   - Public `isProblemDetails()` accessor on `RestOpContext`.
4. [x] `ProblemDetailsProcessor` rewired to consult op-level resolution:
   - The `BasicHttpException` gate now reads `opSession.getContext().isProblemDetails()` (op-level)
     instead of `opSession.getRestContext().isProblemDetails()` (resource-level), so a per-op
     `problemDetails="false"` overrides an opted-in resource and a per-op `problemDetails="true"`
     opts an op in on a non-opted-in resource.
   - The `ProblemException` branch now flips to error-path semantics (ignore `Accept`, Q5(A)) when
     the op is opted-in, so a thrown `ProblemException` on an opted-in op emits
     `application/problem+json` regardless of the client's `Accept`. Non-opted-in ops retain the
     Phase 1 success-path behavior (honor `Accept`) so existing return-value `ProblemException`
     tests on non-opted-in resources still pass.
   - The resource-level `RestContext.handleError` backstop (locked decision Q3) is unchanged — at
     that depth no op context exists, so the resource-level `isProblemDetails()` is the right gate.
5. [x] `ProblemException` throw-path is implicitly supported by the existing routing:
   `RestOpInvoker.invoke()` catches `InvocationTargetException`, `convertThrowable` returns the
   `ProblemException` unchanged (no matching branch), and `res.setContent(<ProblemException>)`
   feeds the processor's existing `instanceof ProblemException` branch. The Q6 status precedence
   (`Problem.status` overrides the default `500` set by `RestOpInvoker`) flows through unchanged.
6. [x] Tests:
   - `Rest_ProblemDetails_PerOp_Test` (15 tests) — six scenario classes covering all
     resource×op combinations, per-op fanout across all HTTP method annotations, and the
     thrown-`ProblemException` opted-in path with extension fields + Accept-html ignore.
7. [ ] **Deferred to Phase 3:** release-notes entry + new `juneau-docs/pages/topics/...` topic page.
   Phase 3 (`ProblemMapper` + `Messages` integration) is the cleaner cut line for user-visible
   docs — landing the docs incrementally per phase risks two churns; landing them with the full
   opt-in story (annotation + bean-store + mapper + localization hooks) keeps the topic-page
   table-of-contents stable.

**Files added/modified (~330 LOC src, ~280 LOC test):**

| File | LOC | Description |
| --- | --- | --- |
| `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/annotation/RestOp.java` | +25 | `String problemDetails() default ""` + Javadoc. |
| `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/annotation/RestGet.java` | +25 | Same. |
| `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/annotation/RestPost.java` | +25 | Same. |
| `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/annotation/RestPut.java` | +25 | Same. |
| `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/annotation/RestPatch.java` | +25 | Same. |
| `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/annotation/RestDelete.java` | +25 | Same (placed before `allowedSerializerOptions()`; `@RestDelete` has no `produces()`). |
| `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/annotation/RestOptions.java` | +25 | Same. |
| `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/annotation/RestOpAnnotation.java` | +20 | Builder field + setter; Object field + ctor + getter. |
| `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/annotation/RestGetAnnotation.java` | +20 | Same. |
| `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/annotation/RestPostAnnotation.java` | +20 | Same. |
| `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/annotation/RestPutAnnotation.java` | +20 | Same. |
| `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/annotation/RestPatchAnnotation.java` | +20 | Same. |
| `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/annotation/RestDeleteAnnotation.java` | +20 | Same. |
| `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/annotation/RestOptionsAnnotation.java` | +20 | Same. |
| `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/RestOpContext.java` | +35 | `problemDetails` memoizer + `isProblemDetails()` accessor. |
| `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/processor/ProblemDetailsProcessor.java` | ~10 | Switched gate to op-level; `ProblemException` branch flips to error-path when opted-in. Javadoc updated. |
| `juneau-utest/src/test/java/org/apache/juneau/rest/Rest_ProblemDetails_PerOp_Test.java` | ~280 | New: 15 tests via `MockRestClient`. |

**Test results (Phase 2):**

| Run | Tests run | Failures | Errors | Skipped |
| --- | --- | --- | --- | --- |
| Build (`./scripts/test.py -b`) | n/a | n/a | n/a | n/a |
| Targeted (8-test scope) | **93** | 0 | 0 | 0 |
| Full (`./scripts/test.py -v`) | **126,750** | **0** | **0** | **20** |

Baseline before Phase 2: 126,735 / 0 / 0 / 20 — exact +15 growth matches `Rest_ProblemDetails_PerOp_Test`. Regression bar (`RestOp_Throws_Test` 8 tests, `Rest_PredefinedStatusCodes_Test` 4 tests) held without modification.

**Design notes (Phase 2):**

- **`noInherit` interaction.** `noInherit` already covers `problemDetails` by default (the
  resource-level `noInherit` array is consulted via `RestOpContext#isInherited(PROPERTY_problemDetails)`),
  so a resource-level `@Rest(problemDetails="true", noInherit="problemDetails")` would prevent
  per-op inheritance — the op falls back to `false` rather than to the resource value. This
  matches the documented `noInherit` semantics for the other tri-state attributes
  (`debug`, `defaultCharset`, `maxInput`, etc.) and needs no special-casing.
- **Why op-level `ProblemException` flips to error-path when opted-in.** A thrown
  `ProblemException` on an opted-in op semantically signals an error; ignoring `Accept` matches
  RFC 7807 §3 ("the alternative leaves the client with a 4xx/5xx and a `text/plain` stack
  trace"). On a non-opted-in op, retaining Phase 1's Accept-honoring behavior keeps the
  return-value path predictable for callers who deliberately pass a `ProblemException` instance
  through `return` rather than `throw`, and keeps `ProblemDetailsProcessor_Test.f01` green
  without modification.

### Phase 3 — declarative problem-mapping + localization seam + user-visible docs — DONE 2026-05-22

Landed the declarative `ProblemMapper` SPI, the future-work localization seam, and the user-facing
release notes + topic page that were deferred from Phase 2.

1. [x] Added `org.apache.juneau.bean.rfc7807.ProblemMapper<T extends Throwable>` SPI in the bean module (pure interface, no rest deps): `Class<T> getExceptionType()` + `Problem map(T exception)`. Mappers that return `null` are skipped and the chain continues.
2. [x] Added `org.apache.juneau.bean.rfc7807.ProblemMapperList` aggregator bean (also in the bean module). Required because the Juneau `@Bean` walk pairs each factory method with its declared return type and collapses multiple `@Bean public ProblemMapper foo()` factories on one resource onto the single `ProblemMapper.class` bean-store slot. Wrapping in a `ProblemMapperList` keeps all entries reachable. **Design choice recorded:** the single-mapper case (one `@Bean public ProblemMapper<X> mapper()` on a resource) still works unchanged via `BeanStore.getBean(ProblemMapper.class)`; the list is only required when registering more than one mapper.
3. [x] `ProblemDetailsProcessor` rewired to consult mappers in this order:
   - `BeanStore.getBean(ProblemMapperList.class)` if present — iterate its entries.
   - Else `BeanStore.getBean(ProblemMapper.class)` — single-mapper fallback.
   - Sort matching mappers most-specific-first via `hierarchyDepth(getExceptionType(), thrownClass)`.
   - First non-`null` `map(thrown)` return wins; else fall back to `ProblemAdapters.fromException(BasicHttpException)` for `BasicHttpException` subclasses.
   - New branch handles any other `Throwable` whose class hierarchy has a matching mapper (gated by `isProblemDetails()`); returns `NEXT` when no mapper opines.
4. [x] **Localization seam** added per Q8 scaffolding directive: `org.apache.juneau.bean.rfc7807.ProblemLocalizationStrategy` — `@FunctionalInterface` with `IDENTITY` no-op default. Processor consults `BeanStore.getBean(ProblemLocalizationStrategy.class).orElse(IDENTITY)` on every emission and passes `(problem, request.getLocale())`. **Q8 reconfirmed:** the reference `Messages`/resource-bundle implementation is intentionally deferred; this is the SPI seam only. Future-work integration can land without changing the processor contract.
5. [x] Tests in `juneau-utest`:
   - `ProblemMapper_Test` (10 tests) — single mapper for a non-`BasicHttpException` domain exception (`a01`); single mapper for a `BasicHttpException` subclass overriding the default adapter (`b01`); default fallback when no mapper is registered (`c01`); most-specific mapper wins via `ProblemMapperList` (`d01`); null-return falls through to the default adapter (`e01`); null-return chains to the next mapper in the list (`e02`); localization strategy consulted with request locale (`f01`); no strategy registered → identity pass-through (`g01`); `IDENTITY` direct invocation with both a non-null locale (`h01`) and a null locale (`h02`).
   - **Regression bar held**: `RestOp_Throws_Test` (8) and `Rest_PredefinedStatusCodes_Test` (4) pass unchanged.
6. [x] Release-notes entry added to `juneau-docs/pages/release-notes/9.5.0.md`:
   - Under `### juneau-rest-server` — new `#### RFC 7807 / 9457 Problem-Details server-side wiring` subsection covering the opt-in flag, per-op fanout, `ProblemDetailsProcessor`, `ProblemException`, `ProblemAdapters`, `ProblemMapper` / `ProblemMapperList`, `ProblemLocalizationStrategy` future-work seam, and `Accept` policy.
   - Under `### juneau-bean-rfc7807 (new module)` — appended a `#### Throw + map SPIs` subsection naming `ProblemException`, `ProblemMapper`, `ProblemMapperList`, and `ProblemLocalizationStrategy` and cross-linking to the server-side section.
7. [x] New topic page `juneau-docs/pages/topics/10.20a.RestServerProblemDetails.md` (slug `RestServerProblemDetails`). Slot picked between `10.20.HttpStatusCodes` and `10.21.BuiltInParameters` — matches the existing `10.21a.SessionOptions` lettered-suffix convention. Covers motivation, opt-in semantics (tri-state, resource-level + per-op fanout, `Accept` policy), all three return paths (return `Problem`, throw `ProblemException`, throw `BasicHttpException`), `Problem` shape recap, `ProblemMapper` (single + list registration + default fallback), `ProblemLocalizationStrategy` future-work seam (with runnable example showing how a caller can wire one today), and a full end-to-end worked example.
8. [x] Wired into `juneau-docs/sidebars.ts` between `10.20.HttpStatusCodes` and `10.21.BuiltInParameters`.

**Files added/modified (~140 LOC src, ~330 LOC test, ~330 LOC docs):**

| File | LOC | Description |
| --- | --- | --- |
| `juneau-bean/juneau-bean-rfc7807/src/main/java/org/apache/juneau/bean/rfc7807/ProblemMapper.java` | ~95 | New SPI interface. |
| `juneau-bean/juneau-bean-rfc7807/src/main/java/org/apache/juneau/bean/rfc7807/ProblemMapperList.java` | ~115 | New aggregator bean for multi-mapper registration. |
| `juneau-bean/juneau-bean-rfc7807/src/main/java/org/apache/juneau/bean/rfc7807/ProblemLocalizationStrategy.java` | ~85 | New future-work SPI seam with `IDENTITY` default. |
| `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/processor/ProblemDetailsProcessor.java` | ~80 | Mapper-discovery + sort + localization seam; new generic-`Throwable` branch. Javadoc updated. |
| `juneau-utest/src/test/java/org/apache/juneau/rest/ProblemMapper_Test.java` | ~330 | 10 tests via `MockRestClient` + direct `IDENTITY` checks. |
| `juneau-docs/pages/release-notes/9.5.0.md` | +130 | New RFC 7807 server-side subsection under `### juneau-rest-server`; new `Throw + map SPIs` subsection under `### juneau-bean-rfc7807`. |
| `juneau-docs/pages/topics/10.20a.RestServerProblemDetails.md` | ~290 | New topic page (slug `RestServerProblemDetails`). |
| `juneau-docs/sidebars.ts` | +5 | New sidebar entry between `10.20.HttpStatusCodes` and `10.21.BuiltInParameters`. |

**Test results (Phase 3):**

| Run | Tests run | Failures | Errors | Skipped |
| --- | --- | --- | --- | --- |
| Build (`./scripts/test.py -b`) | n/a | n/a | n/a | n/a |
| Targeted (`mvn -pl juneau-utest -Dtest=...` — 9-test scope including `ProblemMapper_Test` and the `RestOp_Throws_Test` + `Rest_PredefinedStatusCodes_Test` regression bar) | **103** | 0 | 0 | 0 |
| Full (`./scripts/test.py -v`) | **126,760** | **0** | **0** | **20** |

Baseline before Phase 3: 126,750 / 0 / 0 / 20 — exact +10 growth matches `ProblemMapper_Test`.

**Design notes (Phase 3):**

- **Why `ProblemMapperList` exists.** The Juneau bean-store registration walk in `RestContext` (lines ~1252-1264) walks `@Bean` methods one at a time, and for each method calls `beanStore.createBeanFromMethod(returnType, resource, isBeanMethod)` to invoke the method, then `addBean(returnType, result, name)`. The `createBeanFromMethod` lookup is "find first matching method by return type" — it doesn't know which specific `@Bean` method the outer loop is iterating over. So when two `@Bean public ProblemMapper foo()` factories share a return type, both registration passes end up invoking the *first* method and overwriting the bean-store slot with the same instance. The result: only one of the two mappers is reachable, and the most-specific-first dispatch fails because the more-specific mapper never reaches the bean store. `ProblemMapperList` sidesteps this by giving the user a single bean-store slot to populate with an ordered registry, at the cost of one extra factory line.
- **Discovery order in `resolveMappers`.** The processor consults `ProblemMapperList` *first*, then falls back to `BeanStore.getBean(ProblemMapper.class)` when no list is present. This keeps the single-mapper ergonomics clean (`@Bean public ProblemMapper<X> mapper()` "just works") while making the multi-mapper case explicit (`@Bean public ProblemMapperList problemMappers()`). When both are registered the list wins, matching the more-explicit-intent-wins precedent set by `noInherit` / `@Bean(priority)` elsewhere in the framework.
- **Localization seam scope.** Q8 was locked as "out of scope" at the start of this TODO; Phase 3 ships only the SPI seam (`ProblemLocalizationStrategy`), not the reference `Messages` integration. The seam adds one `BeanStore.getBean(...).orElse(IDENTITY)` call per emission (no observable cost on the hot path when no strategy is registered) and is documented in the topic page with a runnable `ResourceBundle` example for callers who need locale-aware translation today. A full `Messages`-driven implementation can be added later without changing the processor contract — if/when it's wanted, file a sibling TODO referencing the seam.

### Phase 4 (optional, deferred) — `application/problem+xml`

Out of scope for v1, but parking the design: a sibling `juneau-bean-rfc7807-xml` bean module + an `XmlSerializer.DEFAULT_NS`-driven branch in `ProblemDetailsProcessor`. Touch only if a user files a request.

## Acceptance criteria

- [x] **(Phase 1)** `ProblemAdapters.fromException(BasicHttpException)` adapter lives in `juneau-rest-common`; `ProblemAdapters_Test` covers status/title/detail mapping for the 8 most-common `BasicHttpException` subclasses (`BadRequest`, `Unauthorized`, `Forbidden`, `NotFound`, `Conflict`, `InternalServerError`, `NotImplemented`, `ServiceUnavailable`).
- [x] **(Phase 1)** `ProblemDetailsProcessor` is registered in the default `responseProcessors` chain after `ThrowableProcessor` and before `HttpResponseProcessor`. With `problemDetails=""`/`"false"` (default) the processor short-circuits as `NEXT` for any `BasicHttpException` in the response content; `Problem`/`ProblemException` returns are still serialized regardless of opt-in (per locked decision — explicit `Problem` returns are opt-in by usage).
- [x] **(Phase 1)** `@Rest(problemDetails="true")` end-to-end: `MockRestClient` against a resource throwing `NotFound` returns `404` with `Content-Type: application/problem+json` and a Problem JSON body (`Rest_ProblemDetails_OptIn_Test.a01`).
- [x] **(Phase 1)** `@RestOp` methods returning `Problem` set `Content-Type: application/problem+json` and use `Problem.getStatus()` (defaulting to existing status when null; `RestSession.run()` normalizes 0 → 200 per Q6).
- [x] **(Phase 1)** `ProblemException` round-trip via return value: returning `ProblemException` from a handler produces the same wire body as returning the `Problem` directly (`ProblemDetailsProcessor_Test.f01`).
- [x] **(Phase 2)** Per-op `@RestGet`/`@RestPost`/`@RestPut`/`@RestPatch`/`@RestDelete`/`@RestOptions`/`@RestOp` `problemDetails` fanout (mirror the `@Rest` attribute, tri-state `"" | "true" | "false"` with op-level overriding resource-level).
- [x] **(Phase 2)** `ProblemException` throw round-trip: `throw new ProblemException(problem)` from a method on an opted-in op (or resource) produces `application/problem+json` with the user-supplied status and extension fields, regardless of `Accept` (Q5(A)). Routing is the `ProblemDetailsProcessor` content-slot path — `RestOpInvoker.invoke()` already places thrown exceptions into `res.setContent(...)` after `convertThrowable`, and `ProblemException` falls through `convertThrowable` unchanged so the processor's `instanceof ProblemException` branch unwraps it. Verified by `Rest_ProblemDetails_PerOp_Test.g01` and `g02`.
- [x] **(Phase 1)** Coverage on `ProblemAdapters` and `ProblemException` is comprehensive (every constructor, factory, fallback, null branch); `ProblemDetailsProcessor` is covered via end-to-end paths through `MockRestClient`.
- [x] **(Phase 3)** Release-notes entry under `### juneau-rest-server` (RFC 7807 server-side wiring subsection) and `### juneau-bean-rfc7807` (`Throw + map SPIs` subsection) in `juneau-docs/pages/release-notes/9.5.0.md`. New topic page `10.20a.RestServerProblemDetails.md` (slug `RestServerProblemDetails`) added and wired into `juneau-docs/sidebars.ts`.
- [x] **(Phase 3)** `ProblemMapper<T extends Throwable>` SPI lives in `juneau-bean-rfc7807` (pure interface, no rest deps). `ProblemDetailsProcessor` consults registered mappers via the bean store, picks the most-specific by exception class hierarchy, and falls back to `ProblemAdapters.fromException(BasicHttpException)` when no mapper opines. Verified by `ProblemMapper_Test` (10 tests, including a `c01` no-mapper-registered fallback case and an `e02` null-return chain-to-next-in-list case).
- [x] **(Phase 3)** `ProblemMapperList` aggregator bean (`juneau-bean-rfc7807`) ships as the recommended multi-mapper registration pattern, working around the single-slot collapse of multiple `@Bean public ProblemMapper foo()` factories that share a return type. Verified by `ProblemMapper_Test.d01` (most-specific wins) and `e02` (null-return chains to next mapper in the list).
- [x] **(Phase 3)** `ProblemLocalizationStrategy` (Q8 scaffolding-only seam) lives in `juneau-bean-rfc7807` as a `@FunctionalInterface` with `IDENTITY` no-op default. Processor consults `BeanStore.getBean(ProblemLocalizationStrategy.class).orElse(IDENTITY)` on every emission and passes `(problem, request.getLocale())`. Reference `Messages`-driven implementation explicitly deferred (Q8 reconfirmed). Verified by `ProblemMapper_Test.f01` (strategy receives locale), `g01` (no strategy → identity passes through), `h01` / `h02` (IDENTITY constant direct invocation).
- [x] **(Phase 1)** Full `./scripts/test.py` green: 126,735 / 0 / 0 / 20.
- [x] **(Phase 2)** Full `./scripts/test.py` green: **126,750 / 0 / 0 / 20** (+15 from `Rest_ProblemDetails_PerOp_Test`).
- [x] **(Phase 3)** Full `./scripts/test.py` green: **126,760 / 0 / 0 / 20** (+10 from `ProblemMapper_Test`).
- [x] **(Phase 1)** No regression in the existing `ThrowableProcessor` chain — `RestOp_Throws_Test` and `Rest_PredefinedStatusCodes_Test` pass unchanged.
- [x] **(Phase 2)** Regression bar held — `RestOp_Throws_Test` (8 tests) and `Rest_PredefinedStatusCodes_Test` (4 tests) pass unchanged after the per-op fanout + processor op-level resolution.
- [x] **(Phase 3)** Regression bar held — `RestOp_Throws_Test` (8 tests) and `Rest_PredefinedStatusCodes_Test` (4 tests) pass unchanged after the mapper-discovery + localization-seam changes.

## Open questions (historical — resolved 2026-05-22; see "Locked decisions" above)

Resolved 2026-05-22 — see Locked decisions above. Kept verbatim below as historical context for why the locked decisions came out the way they did.

1. ~~**Adapter package & module.**~~ Recommend `juneau-rest-common` as the home for `Problem.fromException(...)`, named `org.apache.juneau.bean.rfc7807.adapter.ProblemAdapters` (static helper class). Alternative: ship it as a default static method on a new `juneau-rest-common`-side interface. **Decision needed before Phase 1.**
2. ~~**Annotation name.**~~ Recommend `problemDetails` (camelCase, matches the existing `defaultRequestAttributes` / `defaultRequestHeaders` style). Alternative: `rfc7807=true`. Recommend `problemDetails` — neutral with respect to RFC 7807 vs 9457.
3. ~~**Default processor registration.**~~ Recommend "always in the chain, no-op when not opted-in." Alternative: only added when `problemDetails=true` is detected on the resource. The always-on path is simpler and lower-risk (the no-op cost is one `instanceof` check per response).
4. ~~**`ProblemException` ship in v1?**~~ Recommend yes — costs ~30 LOC, removes the only friction point ("how do I throw a custom Problem?"). Alternative: defer to Phase 3 alongside `ProblemMapper`.
5. ~~**`Accept` negotiation policy.**~~ Recommend: when `problemDetails=true` *and* the response is an error (4xx / 5xx), emit `application/problem+json` regardless of the client's `Accept` (the spec encourages this). When the response is success and the method returns a `Problem`, honor `Accept` strictly. **Confirm.**
6. ~~**Status code source-of-truth on `Problem` returns.**~~ When a method returns `Problem` with a non-null `status`, the processor uses it. When `Problem.status` is null, fall back to the method's `@RestPost`/etc default status (200/201), *not* a hard 200 — confirm this behaviour. (RFC 7807 §3.1 makes `status` OPTIONAL precisely so the HTTP status carries it.)
7. ~~**`Problem.type` default.**~~ RFC 7807 §3.1: absent `type` means `about:blank`. The bean today does *not* serialize `about:blank` on the wire (preserves the absent-vs-explicit distinction — see the `FINISHED-45` design notes). Confirm we keep that behavior at the server-emit boundary (i.e. don't synthesize a `type:"about:blank"` on the way out).
8. ~~**Localization.**~~ Out of scope for v1 — confirm. (Recommend: yes, defer; `Messages` integration is its own design.)

## Risks

- **Processor ordering bugs.** Inserting ahead of `ThrowableProcessor` is the obvious choice but easy to get wrong — covered by the `Rest_Exceptions_Test` non-regression bar in the acceptance criteria. Mitigation: Phase 0 confirms the chain ordering before any code change.
- **Content-type contention.** A user who has `@Rest(defaultAccept="application/json")` *and* `problemDetails=true` is asking for two different defaults on errors. Locked policy in Open Question #5 resolves this; document it loudly.
- **`Problem.status` vs HTTP status drift.** A method returning `new Problem().setStatus(500)` from a handler chained off a `@RestGet` (default 200) creates ambiguity. Decision in Open Question #6 makes the rule explicit ("if `Problem.status` is set, it wins").
- **Test-fixture sprawl.** RFC 7807 has many degrees of freedom (`type` absent/set, extensions, nested errors). Mitigation: model the test matrix on `Problem_RoundTrip_Test` from `FINISHED-45-*`, which already covers the bean side; the server-side test matrix only adds the HTTP-layer concerns (status, content-type, opt-in negotiation).
- **Cross-cutting overlap with TODO-20 (Rest Debug Rethink).** If TODO-20 reworks `CallLogger` mid-effort, the `Problem` payload may want to appear in `DebugFormat` output. Low risk — orthogonal concerns; flag for the TODO-20 implementer to keep `Problem` rendering in mind for `JsonFormat`.
- **Future Bean Validation integration (TODO-68).** If TODO-68 lands next, the processor's `Problem.errors[]` extension shape becomes the de facto contract. Worth picking a shape now even if we don't implement TODO-68 (recommend `errors: [{ field, message }]` to match Spring's `MethodArgumentNotValidException` mapper).

## Related work

- `todo/FINISHED-45-juneau-bean-rfc7807.md` — the `Problem` bean module this TODO consumes; the archive's "Out of scope" section explicitly names this server-side wiring as the named follow-up.
- `todo/FINISHED-40-remove-hc45-from-rest-common-and-server.md` — retyped `BasicHttpException` onto the JDK-native types and gave it the fluent setter surface this adapter relies on.
- `todo/FINISHED-42-split-rest-common-classic.md` — split `juneau-rest-common` from `juneau-rest-common-classic`; the adapter lives on the non-classic side.
- `todo/TODO-20-rest-debug-rethink.md` — overlap on `Problem`-as-debug-payload rendering; coordinate when TODO-20 designs `JsonFormat`.
- `todo/TODO-68-bean-validation-integration.md` (sibling) — natural follow-on for `Problem.errors[]` from `ConstraintViolationException`.
- `todo/TODO-70-async-completablefuture-virtual-threads.md` (sibling) — orthogonal; the processor needs to work for both sync and async returns when TODO-70 lands.
