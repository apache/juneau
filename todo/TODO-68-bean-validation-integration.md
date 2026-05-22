# TODO-68: Bean Validation (Jakarta Validation 3.x) integration on request beans

Source: split out of TODO-18 brainstorm on 2026-05-22.

## Goal

Honor Jakarta Validation constraints (`@NotNull`, `@Email`, `@Size`, `@Min`, `@Max`, custom validators, validation groups) on `@Content` / `@FormData` / `@Request`-bound beans in `@RestOp` handler signatures. When constraint violations are found, fail-fast with a `400 Bad Request` (or, if TODO-61 has landed and `problemDetails=true` is set, a `400 application/problem+json` with the standard `errors[]` extension).

Today `HttpPartSchema` already participates in a `HttpPartSchema_JakartaValidation_Test` (so the dependency is on the test classpath and the imports compile), but request-bean validation is *not* wired into the handler arg pipeline — a `@Valid` annotation on a request bean is silently ignored.

End-state developer experience:

```java
public class OrderRequest {
    @NotBlank String customerId;
    @NotNull @Positive Integer quantity;
    @Email String contactEmail;
}

@RestPost("/orders")
public Order create(@Content @Valid OrderRequest in) {
    // If validation fails, the handler is never called.
    // Response is 400 + JSON body listing the violations.
    return orderService.create(in);
}
```

## Why now

- Spring Boot users universally expect this to work. Adopting Juneau today means writing validation-by-hand or accepting silent-pass-through.
- `HttpPartSchema_JakartaValidation_Test` already in `juneau-utest` proves the dep can be added (provided scope) without polluting downstream consumers.
- TODO-24 (`FINISHED-24-jsr330-and-spring-lite-support.md`) established the FQN-based annotation-recognition pattern (Juneau recognizes `jakarta.inject.Inject` / Spring `@Autowired` etc. by fully-qualified name without taking a hard dep on either). The same pattern works for `jakarta.validation.Valid` / `jakarta.validation.constraints.*`.
- Pairs naturally with TODO-61 — `ConstraintViolationException` → `Problem.errors[]` extension is the standard pattern.

## Scope

**In scope (v1):**

- Detection (by FQN) of `jakarta.validation.Valid` and `jakarta.validation.constraints.*` on `@Content` / `@FormData` / `@Request`-bound parameters in `@RestOp` handlers.
- Lookup of a `Validator` bean from the bean store (optional — if absent, `jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator()` provides the default).
- New arg-handler enhancement in `ContentArg` / `RequestBeanArg` / `FormDataArg` that, after binding, invokes `validator.validate(bean, groups...)` when a `@Valid` (or constraint) annotation is present.
- New `org.apache.juneau.rest.validation.ValidationException` (extends `BasicHttpException` with status 400) carrying the set of violations.
- A response-side renderer that:
   - With TODO-61's Problem-Details processor active: produces `application/problem+json` with `errors: [{field, message, invalidValue}]` extension.
   - Without TODO-61: produces a simple JSON body `{ "errors": [{field, message}], "status": 400 }`.
- Tests in `juneau-utest` covering primitive constraints (`@NotNull`, `@Size`, `@Email`), nested-bean validation, validation groups, custom validators.

**Explicitly out of scope (v1):**

- Method-level validation (`@Validated` on the resource class, constraints on `@RestOp` return values). Defer.
- Schema-driven validation (e.g. JSON Schema constraints on the request body). Out of scope; that lives in `JsonSchemaGenerator` territory.
- Validation message localization beyond what Jakarta Validation natively does via `ValidationMessages.properties`.
- Custom `ConstraintValidator` discovery beyond what `jakarta.validation` already does.
- Cross-field validation via custom annotations — works automatically as long as the user writes their own `ConstraintValidator`.

## Phased steps

### Phase 0 — confirm seams (read-only)

1. Re-read `HttpPartSchema_JakartaValidation_Test` to see what's already exercised and what dep is on the test path.
2. Inspect `ContentArg` / `RequestBeanArg` / `FormDataArg` in `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/arg/` to find the "after bind, before call" seam.
3. Decide whether `Validator` is constructed lazily per-call or eagerly per-`RestContext` (recommend eager, cached on `RestContext`).

### Phase 1 — SPI + arg integration

1. Add `org.apache.juneau.rest.validation.ValidationException`, `ValidationViolation` (small record: `field`, `message`, `invalidValue`).
2. Add a `Validator` bean-store lookup in `RestContext.createBeanStore(...)` with a default supplier that builds one from `Validation.buildDefaultValidatorFactory()`. Lazy via `Memoizer` so the validator isn't constructed if no `@Valid` is ever encountered.
3. Modify `ContentArg`, `RequestBeanArg`, `FormDataArg` to check for `@Valid` (FQN match) post-bind, invoke `validator.validate(...)`, and throw `ValidationException` on violations.
4. Tests:
   - `BeanValidation_Content_Test` — `@Content @Valid` failure → 400.
   - `BeanValidation_NestedBean_Test` — nested `@Valid` on a property cascades.
   - `BeanValidation_Groups_Test` — `@Valid` with a `Group.class` constraint marker.
   - `BeanValidation_CustomValidator_Test` — user `ConstraintValidator` is honored.

### Phase 2 — Problem-Details integration

1. If TODO-61's `ProblemDetailsProcessor` is in the chain, route `ValidationException` through it with the `errors[]` extension (matches Spring's `MethodArgumentNotValidException` mapper shape).
2. Tests:
   - `BeanValidation_ProblemDetails_Test` — `@Rest(problemDetails=true)` end-to-end: violation → `application/problem+json` with `errors[]`.

### Phase 3 — docs + release notes

1. Release-notes entry under `### juneau-rest-server`.
2. New doc page (or section in an existing page) walking through the `@Valid` flow and the response shapes.

## Acceptance criteria

- [ ] `@Content @Valid MyBean` on a handler parameter validates the bound bean; constraint violations fail-fast with `400 Bad Request` before the handler is invoked.
- [ ] Constraint annotations are recognized by FQN — no hard compile-time dep on Jakarta Validation from `juneau-rest-server`'s `pom.xml`.
- [ ] If `jakarta.validation-api` is *not* on the runtime classpath, `@Valid` is silently ignored (graceful degradation) — confirmed by a "no-jakarta-validation-on-classpath" test path.
- [ ] Default `Validator` is built from `Validation.buildDefaultValidatorFactory()`; users can override by registering a `@Bean Validator`.
- [ ] When TODO-61's `ProblemDetailsProcessor` is active and `@Rest(problemDetails=true)` is set, violations render as `application/problem+json` with the `errors[]` extension.
- [ ] Coverage ≥ 90% on the new validation classes. Full `./scripts/test.py` green.

## Open questions

1. **Hard vs optional dep on Jakarta Validation.** Recommend optional (`provided` scope), FQN-based detection, graceful degradation when absent (matches the TODO-24 pattern). Alternative: hard dep — simpler, but pulls Jakarta Validation onto every consumer.
2. **Validation groups annotation source.** Spring uses `@Validated` for groups. Juneau could either honor `@Validated` (FQN) or extend `@Valid` to accept a `groups()` attribute — but the latter requires a Juneau-owned `@Valid` clone. Recommend honor Spring's `@Validated` (FQN-based) for groups; let `jakarta.validation.Valid` mean "default group."
3. **Default response shape without Problem-Details.** `{ "errors": [...], "status": 400 }` (recommended) vs `{ "violations": [...] }`. Match Spring's shape where possible.
4. **`invalidValue` in the violation payload.** Include by default (recommended) or omit (privacy-sensitive)? Include but document that handlers can replace the renderer if the value is sensitive.
5. **`ValidationException` placement.** Under `org.apache.juneau.rest.validation` (recommended) vs `org.apache.juneau.http.response` (alongside other 4xx exceptions). Recommend the dedicated package — keeps validation concerns grouped.

## Risks

- **Classpath fragility.** Users on Jakarta Validation 2.x (`javax.validation.*`) won't be honored. Mitigation: detect both FQN families (`jakarta.validation.Valid` and `javax.validation.Valid`); document.
- **Performance.** Validator construction is expensive; cache on `RestContext`. Validation of large nested graphs is O(n); document.
- **Error-message localization.** Jakarta Validation reads `ValidationMessages.properties`; if the user has Juneau `Messages` + a different bundle, the two don't compose. Document; out of scope to bridge.
- **Cross-cutting overlap with TODO-61.** The `errors[]` shape becomes a contract. Mitigation: pick the shape once (Spring-compatible) and document it as part of the Problem-Details extension surface.

## Related work

- `todo/FINISHED-24-jsr330-and-spring-lite-support.md` — established the FQN-based recognition pattern this TODO reuses.
- `todo/TODO-61-rfc7807-server-side-wiring.md` (sibling) — `Problem.errors[]` shape is shared.
- Existing test: `juneau-utest/src/test/java/org/apache/juneau/httppart/HttpPartSchema_JakartaValidation_Test.java` — proves the Jakarta Validation dep can coexist with the existing build.
