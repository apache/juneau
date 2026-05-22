# TODO-61: RFC 7807 / 9457 Problem-Details server-side wiring

Source: split out of TODO-18 brainstorm on 2026-05-22 (the recommended #1 pick).

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

### Phase 1 — adapter + processor (no annotation changes)

Lands the core capability. No annotation surface; opt-in is "drop a `@Bean ProblemDetailsProcessor` in your resource."

1. Add the adapter. **The bean module deliberately has no `juneau-rest-common` dep** (locked decision in `FINISHED-45-*`). Reroute: the adapter lives in **`juneau-rest-common`** as a static helper class `org.apache.juneau.bean.rfc7807.adapter.ProblemAdapters#fromException(BasicHttpException)`. The package name keeps it discoverable from the bean's javadoc cross-reference; the *class* lives in `juneau-rest-common`'s tree because that's where the bidirectional dep is allowed.
2. Add `org.apache.juneau.rest.processor.ProblemDetailsProcessor` in `juneau-rest-server`. Implements `ResponseProcessor`. Skeleton:
   - If `res.getException() instanceof BasicHttpException` and `req.getHeader("Accept")` includes `application/problem+json` (or `problemDetails=true` is set on the resource), build a `Problem` via the adapter, set `Content-Type`, set the HTTP status from the exception, serialize through `JsonSerializer.DEFAULT`. Return `FINISHED`.
   - If `res.getContent(Object.class) instanceof Problem`, force `Content-Type: application/problem+json`, set the HTTP status from `Problem.getStatus()` (defaulting to 200 when null), serialize. Return `FINISHED`.
   - Otherwise return `NOT_PROCESSED`.
3. Hook `ProblemDetailsProcessor` into the **default** `ResponseProcessorList` in `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/config/DefaultConfig.java` (or wherever the processor chain is composed — confirm in Phase 0), *ahead* of `ThrowableProcessor`. **The processor is a no-op for non-Problem responses**, so adding it to the default chain has zero runtime cost for users who never touch it.
4. Tests in `juneau-utest`:
   - `ProblemDetailsProcessor_Test` — exercises (a) `BasicHttpException` → Problem JSON, (b) `Problem` return value → Problem JSON, (c) ordinary `String` return → unchanged, (d) `Accept` negotiation (only fires when client asked).
   - `ProblemAdapters_Test` — covers the adapter exhaustively (status, title from reason phrase, detail from message, null-safe).
5. Coverage target: ≥ 90% on the new processor + adapter (mirrors the bar in `FINISHED-45-juneau-bean-rfc7807.md`).
6. No release-notes entry yet — Phase 2 ships the annotation surface and is the user-visible cut line.

### Phase 2 — annotation + opt-in builder hook

Adds the discoverable on-ramp.

1. Add `problemDetails` attribute to `@Rest` and to all six `@RestGet`/`@RestPost`/`@RestPut`/`@RestPatch`/`@RestDelete`/`@RestOptions` annotations (mirror the existing `noInherit` fanout). Default: `false`. When `true`, the resource:
   - Registers `ProblemDetailsProcessor` via the resource's bean store at build time.
   - Adds `application/problem+json` to the default response media types for error paths (so a client with `Accept: */*` and a 4xx outcome gets `application/problem+json` rather than `text/plain` or `application/json`).
2. Add `org.apache.juneau.bean.rfc7807.ProblemException extends RuntimeException` in `juneau-bean-rfc7807` (carries a `Problem`; `getStatus()` returns the embedded status). **Stays in the bean module** — no `juneau-rest-server` dep. The processor checks for `ProblemException` specifically and unwraps it.
3. Tests:
   - `Rest_ProblemDetails_Annotation_Test` — `@Rest(problemDetails=true)` end-to-end: throw `NotFound`, assert response is `application/problem+json` with a well-formed body. Same for per-op `@RestGet(problemDetails=true)`.
   - `ProblemException_Test` — confirms `throw new ProblemException(problem)` produces the expected wire body via the processor.
4. Release-notes entry in `juneau-docs/pages/release-notes/9.5.0.md` (or 9.5.1 / 9.6.0 — pick whichever is open at land time) under `### juneau-rest-server` + `### juneau-bean-rfc7807`.
5. New doc page `juneau-docs/pages/topics/10.07.RestServerProblemDetails.md` (slug `RestServerProblemDetails`) with a worked example, the annotation reference, and the bean-store registration alternative. Sidebar entry under the `juneau-rest-server` section.

### Phase 3 (optional, recommended) — declarative problem-mapping for arbitrary exceptions

Lets users map a custom exception type to a custom `Problem` shape without writing a processor.

1. Add `org.apache.juneau.bean.rfc7807.ProblemMapper<T extends Throwable>` SPI (in the bean module — pure interface, no rest deps): `Problem map(T exception)`.
2. The `ProblemDetailsProcessor` resolves all `ProblemMapper` beans from the bean store, picks the most-specific by exception class hierarchy, and uses the result. Falls back to `Problem.fromException(BasicHttpException)` for `BasicHttpException`s with no explicit mapper.
3. Tests:
   - `ProblemMapper_Test` — register `ProblemMapper<MyDomainException>`, throw `MyDomainException`, assert the custom `Problem` body shape.
4. Defer if Phase 2 ships first and there's no concrete caller.

### Phase 4 (optional, deferred) — `application/problem+xml`

Out of scope for v1, but parking the design: a sibling `juneau-bean-rfc7807-xml` bean module + an `XmlSerializer.DEFAULT_NS`-driven branch in `ProblemDetailsProcessor`. Touch only if a user files a request.

## Acceptance criteria

- [ ] `Problem.fromException(BasicHttpException)` adapter lands in `juneau-rest-common`, with a `Problem_FromException_Test` covering status/title/detail mapping for the 8 most-common `BasicHttpException` subclasses (`BadRequest`, `Unauthorized`, `Forbidden`, `NotFound`, `Conflict`, `InternalServerError`, `NotImplemented`, `ServiceUnavailable`).
- [ ] `ProblemDetailsProcessor` is registered in the default `ResponseProcessorList` ahead of `ThrowableProcessor`. With `problemDetails=false` (default) the processor short-circuits as `NOT_PROCESSED`.
- [ ] `@Rest(problemDetails=true)` end-to-end: `MockRestClient` against a resource throwing `NotFound` returns `404` with `Content-Type: application/problem+json` and a body that round-trips through `JsonParser.DEFAULT.parse(body, Problem.class)`.
- [ ] `@RestOp` methods returning `Problem` set `Content-Type: application/problem+json` and use `Problem.getStatus()` (or 200 if null) as the HTTP status.
- [ ] `ProblemException` round-trip: `throw new ProblemException(problem)` produces the same wire body as returning the `Problem` directly.
- [ ] Coverage ≥ 90% on `ProblemDetailsProcessor`, `Problem.fromException`, `ProblemException`. Bean classes target 100% per the `code-conventions` skill.
- [ ] Release-notes entry under `### juneau-rest-server` and `### juneau-bean-rfc7807` in the active release-notes file. New topic page wired into `juneau-docs/sidebars.ts`.
- [ ] Full `./scripts/test.py` green; `./scripts/sonarqube.py juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/processor/` clean for the new file.
- [ ] No regression in the existing `ThrowableProcessor` chain — `Rest_Exceptions_Test` (or whatever the canonical exception-handling test is in `juneau-utest`) still passes unchanged with `problemDetails=false`.

## Open questions (need user direction before Phase 1)

1. **Adapter package & module.** Recommend `juneau-rest-common` as the home for `Problem.fromException(...)`, named `org.apache.juneau.bean.rfc7807.adapter.ProblemAdapters` (static helper class). Alternative: ship it as a default static method on a new `juneau-rest-common`-side interface. **Decision needed before Phase 1.**
2. **Annotation name.** Recommend `problemDetails` (camelCase, matches the existing `defaultRequestAttributes` / `defaultRequestHeaders` style). Alternative: `rfc7807=true`. Recommend `problemDetails` — neutral with respect to RFC 7807 vs 9457.
3. **Default processor registration.** Recommend "always in the chain, no-op when not opted-in." Alternative: only added when `problemDetails=true` is detected on the resource. The always-on path is simpler and lower-risk (the no-op cost is one `instanceof` check per response).
4. **`ProblemException` ship in v1?** Recommend yes — costs ~30 LOC, removes the only friction point ("how do I throw a custom Problem?"). Alternative: defer to Phase 3 alongside `ProblemMapper`.
5. **`Accept` negotiation policy.** Recommend: when `problemDetails=true` *and* the response is an error (4xx / 5xx), emit `application/problem+json` regardless of the client's `Accept` (the spec encourages this). When the response is success and the method returns a `Problem`, honor `Accept` strictly. **Confirm.**
6. **Status code source-of-truth on `Problem` returns.** When a method returns `Problem` with a non-null `status`, the processor uses it. When `Problem.status` is null, fall back to the method's `@RestPost`/etc default status (200/201), *not* a hard 200 — confirm this behaviour. (RFC 7807 §3.1 makes `status` OPTIONAL precisely so the HTTP status carries it.)
7. **`Problem.type` default.** RFC 7807 §3.1: absent `type` means `about:blank`. The bean today does *not* serialize `about:blank` on the wire (preserves the absent-vs-explicit distinction — see the `FINISHED-45` design notes). Confirm we keep that behavior at the server-emit boundary (i.e. don't synthesize a `type:"about:blank"` on the way out).
8. **Localization.** Out of scope for v1 — confirm. (Recommend: yes, defer; `Messages` integration is its own design.)

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
