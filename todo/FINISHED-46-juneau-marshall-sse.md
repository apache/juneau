# FINISHED-46: juneau-marshall-sse

Archived from `TODO-46-juneau-marshall-sse.md` on 2026-05-20.

## What shipped

A new `org.apache.juneau.sse` package inside `juneau-marshall` plus an `org.apache.juneau.marshaller.Sse` helper. The public API is the `SseEvent` bean, the `SseSerializer` / `SseParser` pair (eager), `SseEventReader implements Iterator<SseEvent>, Closeable` for line-driven streaming, and the matching `SseSerializerSession` / `SseParserSession`. The `data` field is a pre-serialized `String` (settled in the plan — no composed sub-serializer). Per-event `Writer.flush()` was verified end-to-end against Jetty using `curl -N`.

## Files delivered

- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/sse/{SseEvent,SseEventReader,SseSerializer,SseSerializerSession,SseParser,SseParserSession,package-info}.java`
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/marshaller/Sse.java`
- `juneau-utest/src/test/java/org/apache/juneau/sse/Sse_Test.java` (93 tests after the coverage uplift)
- `juneau-examples/juneau-examples-rest/.../SseDemoResource.java` (new demo endpoint at `GET /sseDemo/stream`) + `RootResources.java` wiring
- `juneau-docs/pages/release-notes/9.5.0.md` (Server-Sent Events Support subsection)
- `juneau-docs/pages/topics/02.43.01.SseBasics.md` (new doc page) + `sidebars.ts`

## Verification

- 93 SSE unit tests pass.
- Coverage on `org.apache.juneau.sse`: 98% branches / 100% instructions (after the coverage uplift).
- Per-event flush verified end-to-end via `curl -N`: 10 events at ~500ms intervals, total 4.703s.
- Full `./scripts/test.py`: 48,561 tests, 0 failures (final verification run after uplift).

## Original plan

Source: filed 2026-05-19 (split out of TODO-40 follow-up discussion).

## Goal

Add a Juneau serializer and parser pair for [Server-Sent Events](https://html.spec.whatwg.org/multipage/server-sent-events.html) (WHATWG SSE spec) — MIME type `text/event-stream`. This unblocks Juneau-based streaming REST endpoints.

`ContentType.TEXT_EVENT_STREAM` already exists (`juneau-rest-common`, `org.apache.juneau.http.header.ContentType`); this TODO is about the **marshalling implementation**.

## Module placement

Ships as a new package `org.apache.juneau.sse` **inside** `juneau-core/juneau-marshall`, following the pattern used by every other zero-external-dep format (CSV, UON, JSON, JSONL, MsgPack, Markdown, …). The only standalone sibling — `juneau-core/juneau-marshall-rdf` — exists because it pulls in Apache Jena; SSE has zero new dependencies, so a new module is unwarranted.

## Why now

- SSE is the simplest path to server-push for HTTP/1.1 APIs (vs. WebSockets / gRPC / HTTP/2 streams). Lots of modern dashboards, log streamers, and AI-token-streaming endpoints use it.
- The Juneau serializer pipeline writes through a `Writer`, so per-event flush is straightforward to add inside `SseSerializerSession`.
- The wire format is small and well-specified — one new serializer + one new parser, no transport changes.

## Spec essentials

SSE events are line-oriented UTF-8 text. Each event is a sequence of `field: value` lines terminated by a blank line:

```
event: progress
data: {"step":4,"total":10}
id: 42
retry: 5000

data: line one
data: line two

```

Defined fields (compared **literally**, no case folding):

- `event` — event-type name. Optional; the EventSource default dispatch type is `message`.
- `data` — payload. Each `data:` line appends its value to the data buffer followed by a single `\n`; a trailing `\n` is stripped before dispatch.
- `id` — last-event-id. Value containing U+0000 NULL is ignored; an empty value sets the buffer to the empty string (it does **not** "reset" the previously-remembered id — that semantic lives on the client `EventSource`).
- `retry` — reconnect delay hint in ms. Parsed only if the value is **all ASCII digits**; otherwise ignored.
- Line starting with `:` (colon) — comment, ignored. Used in practice for heartbeats.

Other parsing rules pulled directly from the spec ABNF:

- Line terminators: any of `CR LF`, `CR`, or `LF`.
- A single optional `U+0020 SPACE` after the colon is stripped from the value.
- A line with no colon is treated as a field name with an empty value.
- A single leading UTF-8 BOM is stripped exactly once. Stream charset is UTF-8 and only UTF-8.

## Deliverables

### Package layout

All in `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/sse/`, modeled on `org.apache.juneau.csv` / `org.apache.juneau.jsonl`:

- `package-info.java` — package doc, matches CSV/JSONL boilerplate.
- `SseEvent` — bean with properties `event` (String), `data` (String — see "Design notes"), `id` (String), `retry` (Long).
- `SseSerializer` extends `org.apache.juneau.serializer.WriterSerializer`. Builder calls `produces("text/event-stream")` in its no-arg constructor; static `DEFAULT` field; `hashKey()` **must** include every new builder field (see `code-conventions` skill, "Adding Settings to Serializers/Parsers" → "Update hashKey() Method" — required to avoid the shared-cache bug between differently-configured builders).
- `SseSerializerSession` extends `WriterSerializerSession`. Implements `doSerialize(SerializerPipe, Object)` and calls `w.flush()` after each event so the response actually streams (see "REST integration" below).
- `SseParser` extends `org.apache.juneau.parser.ReaderParser`. Builder calls `consumes("text/event-stream")`.
- `SseParserSession` extends `ReaderParserSession`. Implements `doParse(ParserPipe, ClassMeta<T>)` for `List<SseEvent>` and bare `SseEvent` (single-event input) — see "Parser shape".
- **`SseEventReader implements Iterator<SseEvent>, Closeable`** — public helper exposed for callers who need true line-driven streaming over a `Reader` they own. Drives the WHATWG parse state machine one event at a time without going through `SseParserSession` (which closes after `doParse`). **This is a new public-API shape with no precedent elsewhere in the Juneau marshallers** — every other format ships only `Serializer` + `Parser` + their sessions, with no separate iterator-style helper. SSE warrants the deviation because SSE streams are long-lived (open until the client disconnects or the server pushes its last event) and routinely outlive any reasonable per-call buffer; forcing callers through the eager `List<SseEvent>` path would defeat the entire point of using SSE. The eager `parse(input, List.class, SseEvent.class)` entry point is still provided for one-shot use, so the standard Juneau API surface is preserved — `SseEventReader` is purely additive.

There should also be a sibling `Marshaller` convenience class `Sse` under `org.apache.juneau.marshaller` (pairs `SseSerializer` + `SseParser` with `Sse.of(...)` / `Sse.to(...)`), matching `Csv` / `Jsonl` / `Json5` / etc.

### Core types and API

- `SseEvent` is a plain Juneau bean (no `@BeanType` required; default property order is fine). `data` is `String`, **not** `Object` — see "Design notes" on why we deliberately do not compose a sub-serializer.
- `SseSerializer.serialize(Object,Writer)` accepts a single `SseEvent`, a `Collection<SseEvent>`, an `Iterable<SseEvent>`, an array, or a `java.util.stream.Stream<SseEvent>`. The session iterates and emits each event followed by a blank line, flushing per event.
- `SseSerializer.writeComment(Writer,String)` static helper for keepalive lines (`": ping\n\n"`).

### REST integration

- Register `SseSerializer.DEFAULT` / `SseParser.DEFAULT` opt-in via `@Rest(serializers={SseSerializer.class}, parsers={SseParser.class})` (or per-method via `@RestOp(serializers=…)`, or by contributing a `SerializerSet`/`ParserSet` bean named `serializers`/`parsers`). **There is no classpath auto-discovery in Juneau today** — `BasicSerializerGroup` / `BasicParserGroup` do not exist; the discovery classes are `org.apache.juneau.serializer.SerializerSet` and `org.apache.juneau.parser.ParserSet`, and they are populated explicitly.
- Per-event flush: `SerializedPojoProcessor` writes the response by calling `session.serialize(o, w)` and only flushes/finishes once at the end. To stream incrementally we must call `w.flush()` from inside `SseSerializerSession.doSerialize(...)` after each event. Confirmed by reading `juneau-rest-server/.../processor/SerializedPojoProcessor.java`.
- Returning `java.util.stream.Stream<SseEvent>` from `@RestGet` works as long as the SSE serializer is selected for the `Accept` header — the stream is just an `Iterable<SseEvent>` from the serializer's point of view, and we flush per element. **Returning a reactive-streams `Publisher<SseEvent>` is out of scope** (see "Out of scope") — Juneau has no reactive-streams plumbing in the response pipeline today.

### Parser shape

`ReaderParser.doParse(ParserPipe, ClassMeta<T>) → T` returns a single value and the parser session is closed after the call. A lazy `Stream<SseEvent>` returned from `doParse` would have its backing `Reader` torn down before the caller could consume it. Therefore:

- `SseParser` supports `parse(input, List.class, SseEvent.class) → List<SseEvent>` and `parse(input, SseEvent.class) → SseEvent` (single-event input — useful for unit tests and one-shot reads).
- For true line-driven streaming, callers use `new SseEventReader(reader)` directly. `SseEventReader` is `Iterator<SseEvent>` + `Closeable`; it implements the WHATWG parse state machine and is what `SseParserSession` itself uses internally.

### Tests

Location: `juneau-utest/src/test/java/org/apache/juneau/sse/` (the convention for `juneau-marshall` packages). Use `TestBase`, SSLLC, and `assertBean()` per the `code-conventions` skill.

Cases:

- **Spec compliance** — every example from WHATWG §9.2 (`Sse_Spec_Test`) parsed verbatim through `SseEventReader`, asserting `event`, `data`, `id`, `retry`.
- **Round-trip** — `List<SseEvent>` → `Sse.of(list)` → `Sse.to(text, List.class, SseEvent.class)` → equal list.
- **Multi-line `data:`** — values containing `\n` round-trip via multiple `data:` lines and re-join with `\n` on parse; trailing-LF strip-before-dispatch behavior is asserted.
- **Comment / heartbeat** — `: ping\n\n` is ignored; `SseSerializer.writeComment(...)` produces a parsable comment.
- **BOM** — exactly one leading UTF-8 BOM is stripped; a second BOM is preserved as data.
- **Field parsing edge cases** — `id:` with empty value, `retry: abc` (ignored), `event:` with no value (default `message` at dispatch), line with no colon, optional single space after colon, `CR LF` / `CR` / `LF` line terminators.
- **REST smoke test** in `juneau-utest` (uses the existing mock REST harness) — `@RestGet` returning `Stream<SseEvent>` is asserted only as "all events appear in the response body in order". **Do not** attempt to assert per-event flush timing from the mock harness; that boundary is verified manually via `curl -N` (see Verification). No unit-test flush assertion is in scope.

## Design notes

- **Streaming on serialize, eager-or-iterator on parse.** Serializer emits + flushes per event. Parser provides both an eager `List<SseEvent>` result via the standard `ReaderParser` path and a separate `SseEventReader` iterator for callers who own the `Reader`. We deliberately avoid pretending `doParse` can return a lazy stream — see "Parser shape".
- **`data` is `String`, not a sub-serialized bean.** Sibling marshallers do not compose with delegate serializers — they configure swaps and `ClassMeta` instead. Adding a "delegate serializer" knob would be a new pattern with non-obvious interactions with `ObjectSwap`, the bean dictionary, and `SerializerSet` lookup. Callers who want typed payloads pre-serialize their payload to a JSON string (one line via `Json.of(x)` or multi-line via `Json5.of(x)`) and stuff it in `SseEvent.data`. The SSE serializer is responsible only for splitting that string on `\n` into multiple `data:` lines per spec.
- **No `Last-Event-ID` resume logic** in the serializer/parser themselves. That's a transport concern; expose `id` on the bean and let the REST client deal with `Last-Event-ID` headers.
- **Heartbeat support.** Provide a `SseSerializer.writeComment(Writer,String)` static helper for keepalives. Many proxies kill SSE connections after 30s of silence.
- **Charset is fixed.** Spec mandates UTF-8 (no transcoding). The parser strips one leading BOM and rejects `consumes(...)` content with a non-UTF-8 charset parameter via a clear `ParseException`.

## Out of scope

- Reactive-Streams `Publisher<SseEvent>` returns from `@RestGet`. Juneau-rest has no reactive-streams plumbing today; adding it is a transport-layer change, not a marshalling change. Defer to a sibling TODO.
- Classpath auto-discovery of new serializers/parsers. The existing pattern is explicit registration via `@Rest`/`@RestOp`/`SerializerSet` beans, and this TODO does not change that.
- HTTP/2 `text/event-stream` over server push (works the same wire-wise but the transport story is different).
- WebSocket / gRPC alternatives — explicitly not in scope.
- Client-side `EventSource` reconnect, `Last-Event-ID` header round-trip, and exponential backoff for `retry`. That belongs in `juneau-rest-client`.

## Verification

- `mvn -pl juneau-core/juneau-marshall -am install` succeeds and `mvn -pl juneau-utest test -Dtest='org.apache.juneau.sse.*'` passes.
- `ContentType.TEXT_EVENT_STREAM` is referenced from the `SseSerializer` and `SseParser` class-level Javadocs.
- A working `@RestGet` example added to `juneau-examples-rest` streams 10 events with a `Thread.sleep` between them, observed via `curl -N` to confirm per-event flush. **Per-event flush is verified manually via `curl -N` only — no unit-test boundary assertion is in scope.**
- `./scripts/coverage.py juneau-core/juneau-marshall/src/main/java/org/apache/juneau/sse/ --run` reports ≥ 90% on the new package (self-imposed; there is no jacoco `check` threshold enforced by the build). Bean classes target 100% per the `code-conventions` skill.

## References

- WHATWG HTML §9.2 — Server-sent events (parsing, fields, dispatch): https://html.spec.whatwg.org/multipage/server-sent-events.html
- `ContentType.TEXT_EVENT_STREAM` constant — `juneau-rest/juneau-rest-common/src/main/java/org/apache/juneau/http/header/ContentType.java`
- Primary architectural references (line-oriented, in-`juneau-marshall` package, no external deps):
  - `org.apache.juneau.csv` — `CsvSerializer` / `CsvParser` / `CsvSerializerSession` / `CsvParserSession`
  - `org.apache.juneau.jsonl` — `JsonlSerializer` / `JsonlParser` (record-per-line, closest in shape)
- REST registration mechanism — `org.apache.juneau.rest.annotation.Rest.serializers()` + `org.apache.juneau.serializer.SerializerSet` (no classpath discovery)
- REST response write path that drives per-event flush requirement — `juneau-rest-server/src/main/java/org/apache/juneau/rest/processor/SerializedPojoProcessor.java`
- Coding conventions for everything implementation-side — `.cursor/skills/code-conventions/SKILL.md`
