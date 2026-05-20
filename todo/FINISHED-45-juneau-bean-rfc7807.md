# FINISHED-45: juneau-bean-rfc7807

Archived from `TODO-45-juneau-bean-rfc7807.md` on 2026-05-20.

## What shipped

A new bean module `juneau-bean/juneau-bean-rfc7807` providing the `Problem.java` bean for RFC 7807 `application/problem+json` payloads. The v1 adapter surface is the primitives-only static factory `Problem.fromStatus(int, String, String)`, deliberately keeping the module dep-free of `juneau-rest-common`. The `type` field is nullable with a separate `getTypeOrDefault()` accessor that returns `URI.create("about:blank")` per RFC 7807 §3.1, preserving the absent-vs-explicit distinction on the wire. Extension members flow through the `@BeanProp("*")` triplet pattern (`extraKeys()` + `get(String)` + `set(String, Object)`).

## Files delivered

- `juneau-bean/juneau-bean-rfc7807/{pom.xml, src/main/java/org/apache/juneau/bean/rfc7807/Problem.java, package-info.java}`
- `juneau-bean/pom.xml` (module entry added)
- `juneau-utest/pom.xml` (test dep added)
- `juneau-utest/src/test/java/org/apache/juneau/bean/rfc7807/Problem_RoundTrip_Test.java` (22 tests)
- `juneau-docs/pages/release-notes/9.5.0.md` (juneau-bean-rfc7807 new-module section)
- `juneau-docs/pages/topics/04.09.JuneauBeanRfc7807.md` (new doc page)
- `juneau-docs/sidebars.ts` (sidebar entry)

## Verification

- 22 tests pass.
- `Problem.java`: 100% branches, 100% instructions coverage.
- Full `./scripts/test.py`: 51,198 tests, 0 failures (final verification run).

## Original plan

Source: filed 2026-05-19 (split out of TODO-40 follow-up discussion).

## Goal

Add a small Maven module `juneau-bean-rfc7807` under `juneau-bean/` that provides typed beans for [RFC 7807 — Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc7807). The format is a five-field JSON object with open-ended extensions:

```json
{
  "type":     "https://example.com/probs/out-of-credit",
  "title":    "You do not have enough credit.",
  "status":   403,
  "detail":   "Your current balance is 30, but that costs 50.",
  "instance": "/account/12345/msgs/abc",
  "balance":  30,
  "accounts": ["/account/12345", "/account/67890"]
}
```

RFC 7807 was obsoleted by [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) in July 2023, but the data model and `application/problem+json` IANA registration are unchanged. The constants `ContentType.APPLICATION_PROBLEM_JSON` and `ContentType.APPLICATION_PROBLEM_XML` already exist in `juneau-rest-common` (`org.apache.juneau.http.header.ContentType`); this TODO is about the **bean types**.

**Module name** is deliberately `juneau-bean-rfc7807` (and Java package `org.apache.juneau.bean.rfc7807`) even though RFC 9457 obsoletes RFC 7807 — this matches the existing `ContentType.APPLICATION_PROBLEM_JSON` constant lineage and the historical TODO id, and the data model is unchanged across the two RFCs.

## Why now

- RFC 7807 is essentially universal in modern REST APIs (Spring's `ProblemDetail`, ASP.NET's `ProblemDetails`, etc. all ship out of the box).
- Juneau's `BasicHttpException` (in `juneau-rest-common`) already carries everything needed to populate one (status code, reason phrase via the status line, message, cause); a `Problem` bean closes the loop and lets users emit canonical problem documents from `@RestOp` handlers.
- Pure JSON — no new serializer/parser work. Just typed beans + a small adapter from `BasicHttpException`.

## Deliverables

### Package layout

- Java package: `org.apache.juneau.bean.rfc7807` (matches the sibling pattern `org.apache.juneau.bean.{module}` — e.g. `mcp`, `jsonschema`, `html5`, `openapi3`).
- Module dir: `juneau-bean/juneau-bean-rfc7807/`.
- Source root: `src/main/java/org/apache/juneau/bean/rfc7807/`.
- Modeled on `juneau-bean-mcp` (smallest, simplest sibling): only depends on `juneau-marshall`, `<packaging>bundle</packaging>`, no manual OSGi `<instructions>` (BSN auto-generates as `org.apache.juneau.bean-rfc7807` from the artifactId).

### Files

1. **`juneau-bean/juneau-bean-rfc7807/pom.xml`** — copy `juneau-bean-mcp/pom.xml` verbatim, change `<artifactId>`, `<name>`, `<description>`. No other deltas.
2. **`juneau-bean/pom.xml`** — append `<module>juneau-bean-rfc7807</module>` to the `<modules>` block (the root reactor already aggregates `juneau-bean`).
3. **`juneau-utest/pom.xml`** — add a `<dependency>` on `juneau-bean-rfc7807` next to the other `juneau-bean-*` test deps so integration tests compile.
4. **`src/main/java/org/apache/juneau/bean/rfc7807/package-info.java`** — ASF license header + package javadoc with a `<h5 class='section'>See Also:</h5>` block linking `https://juneau.apache.org/docs/topics/JuneauBeanRfc7807` (matches sibling style).
5. **`src/main/java/org/apache/juneau/bean/rfc7807/Problem.java`** — class-level `@Marshalled`, no-arg public constructor, the five canonical fields plus extension-field plumbing (see "Design notes" below), fluent setters returning `Problem`. Also ships a single static factory `public static Problem fromStatus(int status, String title, String detail)` for the common case of building a `Problem` from primitive HTTP-response pieces; callers wire to `BasicHttpException` themselves with one line. No richer adapter ships in v1 — see "Out of scope".

### Tests

Integration tests live under **`juneau-utest/src/test/java/org/apache/juneau/bean/rfc7807/`** (sibling pattern: `juneau-utest/src/test/java/org/apache/juneau/bean/mcp/McpBeans_RoundTrip_Test.java`). Bean modules themselves do **not** have a `src/test/java` directory.

- `Problem_Test.java` (or `RFC7807Beans_RoundTrip_Test.java` to mirror the MCP naming) covering, at a minimum:
  - All five canonical fields plus arbitrary extension fields round-trip cleanly through `JsonSerializer.DEFAULT` / `JsonParser.DEFAULT`.
  - Extension fields appear at the **top level** of the JSON (flattened, not under an `extensions` key) — assert against the wire string, not just the bean.
  - `type` absent in JSON deserializes such that `null`-handling matches RFC 7807 §3.1 ("about:blank" default).
  - `status` is serialized as a JSON number when set, omitted when `null`.
  - `Problem.fromStatus(int, String, String)` produces the expected JSON for a couple of representative status codes (e.g. 404 + 500), populating `status`, `title`, and `detail`.
  - `getType()` returns `null` on a fresh bean and the wire JSON does not include a `"type"` member; `getTypeOrDefault()` returns `URI.create("about:blank")` for the same bean.
  - Follow the `code-conventions` skill: SSLLC for test data, `assertBean(...)` for deep-property assertions, and (where applicable) the nested `A_basicTests` / `B_serialization` / `C_extraProperties` structure.

## Design notes

- **`Problem` bean shape** matches the no-Builder, fluent-setter pattern used by `juneau-bean-mcp` (e.g. `JsonRpcError`, `Tool`):
  - Class-level `@org.apache.juneau.annotation.Marshalled`.
  - No-arg public constructor; no separate `Builder` class. Use `new Problem().setType(...).setTitle(...)` chaining for the general case. The **only** static factory is `Problem.fromStatus(int, String, String)` (see "Files" above) — added because the "build a `Problem` from a status code + title + detail" call site is overwhelmingly the common one.
  - Field types: `URI type`, `String title`, `Integer status`, `String detail`, `URI instance`. (Setters can also accept `String` for the URI fields and convert internally — see `JsonSchema.setIdUri(Object)` in `juneau-bean-jsonschema` for prior art.)
- **Extension members (RFC 7807 §3.2) flatten into the top-level JSON object** via the `@BeanProp("*")` triple-method pattern used by `SwaggerElement` (`juneau-bean-swagger-v2`) and `OpenApiElement` (`juneau-bean-openapi-v3`):
  - Private `Map<String,Object> extra` field (no annotation).
  - `@BeanProp("*") public Set<String> extraKeys()` — returns `extra.keySet()` or `Collections.emptySet()`.
  - `@BeanProp("*") public Object get(String property)` — null-safe lookup.
  - `@BeanProp("*") public Problem set(String property, Object value)` — initializes `extra` lazily, returns `this`.
  - **Do not** put a `Map<String,Object> extensions` field with an annotation directly on it — that pattern doesn't exist anywhere in `juneau-bean-*` and was a misread of `juneau-bean-jsonschema` (which uses `@BeanProp("$defs")` for explicit JSON-name overrides, not extension fan-out).
  - Note: the annotation is `@BeanProp` (`org.apache.juneau.commons.bean.BeanProp`), not `@Beanp`.
- **`type` field is nullable; `getTypeOrDefault()` returns the spec default.** RFC 7807 §3.1 says an absent `type` is equivalent to `"about:blank"`. The bean models this **lazily**: the underlying `URI type` field stays `null` when unset, plain `getType()` returns that raw nullable value, and a fresh `new Problem()` serializes without a `"type"` member. A separate accessor `public URI getTypeOrDefault()` returns `URI.create("about:blank")` when the field is null, for callers who want the spec default applied. **Do not** eagerly default the field in the constructor or any setter — that would round-trip a synthetic `"type":"about:blank"` onto the wire and lose the "absent vs explicitly-set" distinction.
- **`status` is `Integer`, not `int`** — RFC 7807 §3.1 says `status` is a JSON number and is OPTIONAL; `null` means "use the response's HTTP status".
- **No XML variant** in this initial cut. RFC 7807 also defines `application/problem+xml` with a fixed XML schema; we can add a sibling `juneau-bean-rfc7807-xml` later if anyone asks. `ContentType.APPLICATION_PROBLEM_XML` constant already exists.
- **`BasicHttpException` adapter — only the primitive-args static factory ships in v1.** The bean module keeps its dependency on `juneau-marshall` only (no `juneau-rest-common` dep), and the sole adapter surface is `Problem.fromStatus(int status, String title, String detail)` on the bean itself. Callers bridge from `BasicHttpException` themselves in one line: `Problem.fromStatus(e.getStatusCode(), e.getStatusLine().getReasonPhrase(), e.getMessage())` (`BasicHttpException` exposes the reason phrase via `getStatusLine().getReasonPhrase()`, not a top-level `getReasonPhrase()`). A richer `BasicHttpException`-to-`Problem` convenience adapter belongs in `juneau-rest-common` as a separate class — see "Out of scope".

## Out of scope

- **Follow-up:** a `BasicHttpException`-to-`Problem` convenience adapter (richer than the primitive-args `Problem.fromStatus(...)` static factory) belongs in **`juneau-rest-common`** as a separate class — placing it there avoids forcing `juneau-bean-rfc7807` to take an outbound dep on `juneau-rest-common`. Not a v1 deliverable; file as a separate TODO when there is a concrete caller.
- Wiring a `@Rest` exception handler that auto-emits `Problem` for every uncaught `BasicHttpException`. Worth doing later, but should live in `juneau-rest-server` (not the bean module) and is a separate TODO.
- `application/problem+xml` — separate follow-up if demanded.
- Translating localized titles / details via Juneau's message bundles. Nice to have, not a v1 requirement.

## Verification

- `mvn -pl juneau-bean/juneau-bean-rfc7807 -am install` succeeds.
- New module appears in the parent reactor build (verify via `mvn -pl :juneau help:effective-pom` or just `mvn install` from the root).
- `ContentType.APPLICATION_PROBLEM_JSON` is referenced from at least one Javadoc `{@link}` or test assertion in the new module so the link is discoverable.
- Coverage on the new module ≥ 90% as reported by `./scripts/coverage.py juneau-bean/juneau-bean-rfc7807/src/main/java/org/apache/juneau/bean/rfc7807/ --run`. Coverage is **not** currently enforced via a JaCoCo `<rule>` in any pom; the bar is informal but consistent across sibling bean modules.

## References

- RFC 7807 — [Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc7807) (data model in §3.1, extensions in §3.2, `application/problem+json` registration in §6.1).
- RFC 9457 — [Problem Details for HTTP APIs (obsoletes 7807)](https://www.rfc-editor.org/rfc/rfc9457).
- Existing constants: `juneau-rest/juneau-rest-common/src/main/java/org/apache/juneau/http/header/ContentType.java` (`APPLICATION_PROBLEM_JSON`, `APPLICATION_PROBLEM_XML`).
- Sibling bean module to clone: `juneau-bean/juneau-bean-mcp/` (pom + `package-info.java` + `JsonRpcError.java` are the closest templates).
- Extension-field pattern reference: `juneau-bean/juneau-bean-swagger-v2/src/main/java/org/apache/juneau/bean/swagger/SwaggerElement.java` and `juneau-bean/juneau-bean-openapi-v3/src/main/java/org/apache/juneau/bean/openapi3/OpenApiElement.java`.
- Test layout reference: `juneau-utest/src/test/java/org/apache/juneau/bean/mcp/McpBeans_RoundTrip_Test.java`.
- Adapter target: `juneau-rest/juneau-rest-common/src/main/java/org/apache/juneau/http/response/BasicHttpException.java` (Beta as of 9.2.1).
- Conventions: `.cursor/skills/code-conventions/SKILL.md` (Javadoc tags, SSLLC, `assertBean`, fluent-setter style).
