# FINISHED-34 — Generalize `JsonMap` / `JsonList` into `MarshalledMap` / `MarshalledList` + per-language subclasses

Archived from `TODO-34-marshalledmap-marshalledlist.md` on 2026-05-18 — the core architectural work (Phases A, B, C) is complete. Phase D (per-marshaller `XMap` / `XList` fan-out) and Phase E (per-language docs/tests) are intentionally deferred as follow-on enhancements; if they're picked up later they should be tracked under a fresh `TODO-<new-id>` rather than re-opening this one.

Source: promoted from `TODO.md` on 2026-05-18.

---

## Final status — CLOSED (2026-05-18)

This TODO is considered complete at the end of Phase C. The core architectural goal
(`MarshalledMap` / `MarshalledList` extraction, parser factory-hook model, Json5 flavored
parser output, strict-JSON `JsonMap` / `JsonList` retarget, and compatibility sweep) is done
and validated on a full green test suite.

The Phase D fan-out (`XMap` / `XList` for every remaining marshaller) and Phase E docs/tests
work are intentionally deferred as follow-on enhancements rather than blockers for closing this
item.

---

## Phase A status — DONE (2026-05-18)

The foundational refactor is complete. No behavioral change yet — `JsonMap` / `JsonList`
still produce JSON5 `toString()` and still default to `Json5Parser` in their `(CharSequence)` /
`(Reader)` constructors. Phase B will retarget them to strict JSON.

Delivered:
- New `org.apache.juneau.collections.MarshalledMap` carrying all neutral methods (typed
  accessors, fluent setters, bean integration, `ObjectRest` path navigation, cast/cast2,
  inner-map plumbing, `EMPTY_MAP`, `UnmodifiableMarshalledMap`). Default `toString()` is the
  inherited `LinkedHashMap` form. New protected hook `newMarshalledMap(MarshallingSession)`
  lets subclasses customize the abstract-map fallback from `cast2`.
- New `org.apache.juneau.collections.MarshalledList` carrying all neutral methods (`getMap`,
  `getList`, `elements`, `append`/`appendIf`/`appendReverse`, `unmodifiable`/`modifiable`,
  `getAt`/`putAt`/`postAt`/`deleteAt`, `EMPTY_LIST`, `UnmodifiableMarshalledList`). Default
  `toString()` is the inherited `LinkedList` form.
- `JsonMap` reduced to a JSON-flavored subclass of `MarshalledMap`: keeps `EMPTY_MAP`,
  JSON-specific factories (`ofJson(CharSequence)`, `ofJson(Reader)`), the JSON-specific
  fluent surface (`toJson`, `toJson5`, `toJsonl`, `toJcs`, `toHjson`, `toReadableJson5`,
  `putJson`, `writeTo(Writer)`), `Json5Parser`-defaulting `(CharSequence)` / `(Reader)` ctors,
  `toString()` returning `Json5.of(this)`, and covariant overrides of every neutral
  fluent / collection-returning method. `newMarshalledMap` is overridden to keep
  `cast(Map.class)` returning a `JsonMap`.
- `JsonList` reduced to a JSON-flavored subclass of `MarshalledList` with the analogous
  surface (JSON-specific factories, `Json5Parser`-defaulting ctors, JSON `toString()`,
  covariant overrides of fluent setters and `getMap` / `getList`).
- `ResolvingJsonMap` continues to extend `JsonMap` unchanged (parent change to
  `MarshalledMap` deferred to Phase B per plan — completed in Phase B as
  `ResolvingMarshalledMap`).
- `org.apache.juneau.collections/package-info.java` updated to document the new hierarchy.
- New `MarshalledMap_Test` (21 tests) and `MarshalledList_Test` (17 tests) under
  `juneau-utest` covering the neutral base directly.
- Full Maven `clean install` + full `test` suite green.

Coverage after Phase A (JaCoCo):
- `MarshalledMap`: 65% instructions, 58% branches.
- `MarshalledList`: 76% instructions, 56% branches.

Most remaining gaps are in `cast2` / `narrowClassMeta` corner cases that get exercised by
existing `JsonMap` callers under their inherited paths; the gaps will close further once
Phase B adds `Json5Map`/`Json5List` tests and Phase C fans out to per-marshaller subclasses.

---

## Phase B status — DONE (partial, 2026-05-18)

Phase B delivered all of the **non-behavioral** prep work needed to turn on Option B
(parser-produced flavor) safely. The behavioral switch itself — having `Json5ParserSession`
actually return `Json5Map` / `Json5List` from the new factory hooks — was attempted,
caused **6 test failures + 93 errors** in the full utest suite (mostly in
`org.apache.juneau.rest.Swagger_Test` plus scattered `(JsonMap)` / `instanceof JsonMap`
cast sites across XML / HTML / UrlEncoding / Bson / Proto / Parquet / Schema parsers and
`BasicSwaggerProviderSession`), and was **rolled back**. Switching the flavor without
first widening those downstream sites blows up at runtime via `ClassCastException`s deep
inside the Swagger provider and the per-marshaller parser pipelines. Phase C will do the
cast-site sweep and the `Json5ParserSession` override **atomically** so the behavioral
break only ever lands on green.

Delivered in Phase B:

- New `org.apache.juneau.json5.Json5Map` / `Json5List` (extending the strict-JSON
  `JsonMap` / `JsonList` for now to preserve back-compat; Phase C may re-parent them
  directly to `MarshalledMap` / `MarshalledList` when the cast-site sweep lands).
- New `Json5Map_Test` / `Json5List_Test` smoke tests under
  `juneau-utest/src/test/java/org/apache/juneau/json5/`. The
  `a22_parserProducesJson5Map` test was authored, found to fail because the
  `Json5ParserSession` override was reverted, and was **removed** with a comment in
  `Json5Map_Test.java` explaining that the assertion will return in Phase C alongside
  the cast-site sweep.
- `MarshallingSession.newGenericMap(ClassMeta)` opened up as a `protected` overridable
  hook (was hardcoded `new JsonMap(this)`); new `protected MarshalledList newGenericList()`
  hook added alongside it. The base defaults still return `new JsonMap(this)` /
  `new JsonList(this)` for back-compat — the switch to the neutral `MarshalledMap` /
  `MarshalledList` base default is deferred to Phase C so it lands atomically with the
  cast-site sweep. `newGenericMap(ClassMeta)` for string-keyed maps routes through
  `newGenericMap()`.
- `JsonParserSession` overrides both hooks (returning `JsonMap` / `JsonList`) and its
  internal `new JsonMap(this)` / `new JsonList(this)` literals have been migrated to call
  the hooks. This proves out the override mechanism without changing observable behavior.
- `MarshallingContext` (line ~4214, Gap 17 Map → Bean converter): widened
  `instanceof JsonMap jm` to `instanceof MarshalledMap mm` (and the `jm.getString(...)` /
  `jm.cast(cm)` call sites updated to use the new variable name).
- `ParserSession.cast(...)` widened from accepting `JsonMap` to accepting `MarshalledMap`.
  Cast-call sites in `RdfParserSession`, `RdfStreamParserSession`, `YamlParserSession`,
  and `HoconParserSession` widened from `(JsonMap)` to `(MarshalledMap)`.
- `Generics_RoundTripTest.a01_beansWithUnboundTypeVars` widened to assert
  `instanceof MarshalledMap` instead of a specific concrete class name.
- `MarshalledMap_Test.a08b_parseProducesJsonMapInternally` updated to assert
  `instanceof JsonMap` (the current back-compat behavior) with a comment that the
  flipped-to-`Json5Map` assertion will return in Phase C.
- **`ResolvingJsonMap` renamed to `ResolvingMarshalledMap`** and re-parented directly
  from `JsonMap` to `MarshalledMap` (its SVL resolution is language-agnostic). Hard
  rename, no deprecation shim — the only consumer outside the class itself was its own
  test. All `@Override /* Overridden from JsonMap */` comments updated to
  `/* Overridden from MarshalledMap */`. Test file also renamed
  (`ResolvingMarshalledMapTest`).
- `Xml_Test` casts left as the original `(JsonMap) Json5Parser.DEFAULT.parse(...)` form
  — back-compat preserved, no widening needed yet.

Full utest suite is green at end of Phase B:
`Tests run: 50754, Failures: 0, Errors: 0, Skipped: 20`
(one less than pre-Phase-B because `a22_parserProducesJson5Map` was removed pending
Phase C).

---

## Phase C status — DONE (2026-05-18)

Phase C delivered the **behavioral flip** that Phase B punted on, atomically with the
cast-site sweep so it could land on green. The four-step sequence (Json5 override,
JsonMap/JsonList retargeting, base-default flip, deferred-test restore) was executed
sequentially with full utest runs between each step. No mid-step rollbacks were
required.

Delivered in Phase C:

- **Phase C step 3 — `Json5ParserSession` factory hook override.** `Json5ParserSession`
  now overrides `newGenericMap()` / `newGenericList()` to return `Json5Map` / `Json5List`,
  re-introducing the `import org.apache.juneau.collections.*;` that was removed at the
  end of Phase B. This was the override that triggered the Phase B explosion — with the
  cast-site sweep from Phase C step 1 already landed, only `BasicSwaggerProviderSession`
  (the original Phase B failure surface) needed further work, and that surface was
  migrated wholesale to `Json5Map` / `Json5List` (the call to
  `JsonSchemaGeneratorSession.addBeanDef` kept its `JsonMap` parameter via a
  `new JsonMap(...)` copy because that API still expects strict `JsonMap`).
- **Phase C step 4 — `JsonMap` / `JsonList` retargeted to strict JSON.** Their
  `toString()` now returns `Json.of(this)` and their `(CharSequence)` / `(Reader)`
  constructors / `ofJson(...)` factories now use `JsonParser.DEFAULT`. Production code
  that depended on JSON5 `JsonMap.toString()` / `JsonMap.ofJson(...)` semantics was
  migrated to `Json5Map` / `Json5List` (see audit list below).
- **Phase C step 5 — `MarshallingSession` base defaults flipped to the neutral
  collections.** `newGenericMap()` / `newGenericList()` now return `MarshalledMap` /
  `MarshalledList`. Only `JsonParserSession` and `Json5ParserSession` override the hook
  today; every other parser session (HTML, URL-encoding, XML, MessagePack, CSV, UON,
  CBOR, BSON, Markdown, Hjson, Hocon, Toml, Jsonl, Yaml, RDF, etc.) inherits the base
  and now produces `MarshalledMap` / `MarshalledList` during generic parsing. The
  per-language `XMap` / `XList` fan-out from Phase D will change that.
- **Phase C step 6 — Deferred tests restored / flipped.**
  - `Json5Map_Test.a22_parserProducesJson5Map` restored (replacing the
    `// NOTE: Phase B is currently additive-only …` deferral comment block).
  - `MarshalledMap_Test.a08b_parseProducesJsonMapInternally` renamed to
    `a08b_parseProducesJson5MapInternally` and flipped to assert `instanceof Json5Map`
    plus `!instanceof JsonMap`, per the test-impact note further down in this plan.

Behavioral break audit — migrated callers (production code):

- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/MarshallingSession.java`
  — `parseToMap(...)` / `parseToList(...)` switched to `Json5Map.ofJson5(...)` /
  `new Json5List(...)` so bean-modeling annotation values like
  `properties="{key:'val'}"` keep working.
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/MarshallingContext.java`
  — `CharSequence → Array` / `CharSequence → Map` / `CharSequence → Collection`
  converters switched to `Json5List.ofJson5(...)` / `new Json5List()` /
  `Json5Map.ofJson5(...)` for the same reason.
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/BeanMapLoader.java` —
  `load(BeanMap<T>, String)` now parses JSON5 input via `Json5Map.ofJson5(input)`.
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/jsonschema/SchemaUtils.java`
  — `parseMap(...)` / `parseSet(...)` now parse via `Json5Map.ofJson5(...)` /
  `Json5List.ofJson5OrCdl(...)` and wrap the result in `JsonMap` where the public API
  still hands back the strict type.
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/UriContext.java` —
  constructor JSON parsing switched to `Json5Map.ofJson5(s)`.
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/xml/XmlParserSession.java`
  — `parseAnything(...)` `sType.isMap()` branch now wraps the `preserveRootElement`
  result in the **target** map type (via `sType.newInstance(outer)` when possible)
  instead of an unconditional `newGenericMap()`. Without this, parsing
  `XmlParser.preserveRootElement(...)` into `JsonMap.class` returned a `MarshalledMap`
  after step 5 and broke the implicit `(JsonMap)` cast on the way out.
- `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/processor/PlainTextPojoProcessor.java`
  — for `Map` / `Collection` / array payloads it now serializes via `Json5.of(o)` so
  REST endpoints that return `JsonMap` / `JsonList` from a plain-text handler keep their
  historical JSON5 wire format instead of suddenly emitting strict JSON.
- `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/swagger/BasicSwaggerProviderSession.java`
  — Swagger model builder switched to `Json5Map` / `Json5List` throughout (the surface
  Phase B blew up on) so the inline JSON5 in `@Schema` / `@OpenApi` annotations
  continues to parse cleanly.
- `juneau-rest/juneau-rest-client/src/main/java/org/apache/juneau/rest/client/RestClient.java`
  — `callback(String)` now parses the inline headers map via `Json5Map.ofJson5(headers)`
  so JSON5-style callback strings like `GET {Foo-X:123,Foo-Y:'abc'} /testCallback`
  still work.

Behavioral break audit — migrated tests (toString or input-format follow-ups):

- Wholesale-migrated tests (their inputs were JSON5 — rewritten to use `Json5Map` /
  `Json5List`): `JsonMap_Test`, `JsonList_Test`, `UonSerializer_Test`,
  `UrlEncodingSerializer_Test`, `FilteredMapTest`, `Json_Test`, `ObjectSwap_Test`,
  `MsgPackSerializerTest`, `DataConversion_Test`, `OpenApiPartSerializer_Test`,
  `JsonSchemaBeanGenerator_Test` (uses `new JsonMap(Json5Map.ofJson5(...))` for API
  compatibility), `ObjectRest_Test` (mixed — field types switched to `Json5Map` /
  `Json5List`, with locally-introduced `jm` / `jl` `JsonMap` / `JsonList` copies for
  call sites that strictly expect those types).
- Tests that kept `JsonMap` and switched their inputs to strict JSON:
  `JsonMaps_RoundTripTest`, `TrimStrings_RoundTripTest`, `SimpleObjects_RoundTripTest`,
  `BeanMap_Test`, `MarshalledConfig_Test`.
- Endpoint signatures flipped from `JsonMap` to `Json5Map` so test assertions that
  expect JSON5 output keep working (these endpoints store the parsed map and return
  `m.toString()` as the response body): `Remote_HeaderAnnotation_Test`,
  `Remote_FormDataAnnotation_Test`, `Remote_QueryAnnotation_Test`,
  `Remote_PathAnnotation_Test`, `Remote_RequestAnnotation_Test`,
  `Restx_Serializers_Test` (`JsonList` → `Json5List`), `Rest_AllowContentParam_Test`,
  `RestClient_CallbackStrings_Test`.
- `XmlParser_Test` parse targets switched to `Json5Map.class` for the JSON5-asserting
  cases; `a03_preserveRootElement` left on `JsonMap.class` (with assertions updated to
  strict JSON) because the test exercises the new XmlParserSession wrapper-typing path
  documented above.
- `UrlEncodingParser_Test` (`a06_commaDelimitedListsWithSpecialChars`,
  `a07_whitespace`) assertions converted to strict JSON to match the new
  `JsonMap.toString()`.
- `BeanMap_Test.a27_castToLinkedListBean` widened from `instanceof JsonMap` to
  `instanceof Json5Map` to reflect that `MarshallingSession.parseToMap` now produces
  `Json5Map`.
- `OpenApiPartParser_Test.h02_objectType_2d` / `h03_objectType_3d` widened from
  `instanceof JsonList` to `instanceof MarshalledList` (UON parsing now hits the
  neutral base default).

Final utest count at end of Phase C:
`Tests run: 50755, Failures: 0, Errors: 0, Skipped: 20`
(one more than end-of-Phase-B because `Json5Map_Test.a22_parserProducesJson5Map`
came back).

---

## Phase C — Option B behavioral switch

This is the deferred Phase B "Option B switch" plus the cast-site sweep that has to
land with it. Items here MUST be done together (or in a tight sequence guarded by full
test runs) to avoid the same 6-failure + 93-error explosion that triggered the Phase B
rollback. All but the first item are about making it **safe** to flip the flavor.

1. **Sweep and widen downstream cast / instanceof sites.** Grep the entire monorepo
   (and `juneau-examples`) for `instanceof JsonMap`, `(JsonMap)`, `instanceof JsonList`,
   `(JsonList)` — widen each to `MarshalledMap` / `MarshalledList` (or to the matching
   flavored type if flavor matters at that site). Known hot spots:
   - `juneau-rest-server` `BasicSwaggerProviderSession` (~10 sites — the biggest
     blast-radius surface the Phase B attempt hit).
   - XML / HTML / UrlEncoding / Bson / Proto / Parquet / Schema parser sessions.
   - Test code that explicitly casts parser output (e.g. `Xml_Test`).
   - REST argument parsing (`FormDataArg`, `HeaderArg`, `PathArg`, `QueryArg`).
   - `cp.Messages`, `Microservice` / `ConfigResource`.
2. **Migrate the remaining parser sessions' `new JsonMap(this)` / `new JsonList(this)`
   literals to the factory hooks** (the ones that weren't migrated in Phase B):
   `UonParserSession`, `CborParserSession`, `XmlParserSession`, `BsonParserSession`,
   `MarkdownParserSession`, `HtmlParserSession`, `MsgPackParserSession`,
   `HjsonParserSession`, `UrlEncodingParserSession`, `HoconParserSession`,
   `TomlParserSession`, `JsonlParserSession`, `MarkdownDocParserSession`,
   `CsvParserSession`, `YamlParserSession`. (`JsonParserSession` is already done.)
   Audit-style replacement, no logic change beyond the type — sets each session up to
   be flavored independently.
3. **Add the `Json5ParserSession` override** that returns `Json5Map` / `Json5List` from
   `newGenericMap` / `newGenericList`. This is the override that was reverted at the
   end of Phase B; once steps 1 + 2 are in place it lands safely.
4. **Retarget `JsonMap` / `JsonList` `toString()` to strict JSON; switch their default
   parser to `JsonParser.DEFAULT`** (the original Phase B "behavioral break" step 3 from
   the execution order). Migrate every internal caller that depends on JSON5
   `JsonMap.toString()` to `Json5Map` / explicit `Json5.of(...)`. Audit list:
   `MarshallingContext`, `BasicSwaggerProviderSession`, `cp.Messages`, REST argument
   parsing, `Microservice` / `ConfigResource`, plus any test asserting
   `assertEquals("{...}", jsonMap.toString())`.
5. **Flip the `MarshallingSession.newGenericMap()` / `newGenericList()` base default
   from `JsonMap` / `JsonList` to the neutral `MarshalledMap` / `MarshalledList`.** This
   is the final step that makes the neutral base the actual default for any session
   whose parser doesn't supply a flavor. Only safe after step 1.
6. **Restore the `a22_parserProducesJson5Map` test** in `Json5Map_Test` (its assertion
   is documented in the file as deferred from Phase B). Update
   `MarshalledMap_Test.a08b_parseProducesJsonMapInternally` to expect `Json5Map` (and
   not `JsonMap`) per the **Parser session factory hooks > Test impact** section below.

After Phase C, the fan-out described in step 9 of **Suggested execution order**
(`Json5Map` → `JsonlMap` / `JcsMap` / `HjsonMap` → text non-JSON → binary → RDF) can
proceed one subclass at a time without further behavioral risk.

---

## Goal

- Introduce a new neutral pair `MarshalledMap` / `MarshalledList` in `org.apache.juneau.collections` that carries **all marshaller-agnostic functionality** currently on `JsonMap` / `JsonList`. Default `toString()` is the inherited `LinkedHashMap` / `LinkedList` form — no language coupling.
- Repurpose the existing `JsonMap` / `JsonList` as **strict-JSON-flavored** subclasses. **Behavioral break**: their `toString()` switches from `Json5.of(this)` to `Json.of(this)`, and their `CharSequence`/`Reader` constructors switch their default parser from `Json5Parser.DEFAULT` to `JsonParser.DEFAULT`. Must be called out in the v9.5 migration guide and release notes.
- Add new `Json5Map` / `Json5List` (in `org.apache.juneau.json5`) that pick up the **JSON5** default behavior currently on `JsonMap`. These are the drop-in replacement for callers that today rely on `JsonMap.toString()` producing JSON5.
- Add an `XMap` / `XList` pair for every supported marshaller (text and binary), each in the matching package alongside its serializer/parser.

---

## Design decisions (locked in)

1. **Neutral base**: `MarshalledMap` / `MarshalledList` are concrete (non-generic) classes. No `toString()` override. Inherits from `LinkedHashMap<String,Object>` / `LinkedList<Object>` as `JsonMap`/`JsonList` do today.
2. **Coverage**: All 26 marshallers + RDF. Includes binary (`Bson`, `MsgPack`, `Cbor`, `Parquet`, `Proto`).
3. **Strict-JSON `JsonMap` / `JsonList` kept**: same package (`org.apache.juneau.collections`) — minimizes import churn — but retargeted to strict JSON.
4. **Self-type strategy: covariant overrides** (not recursive generic). Base methods return the neutral types; each subclass narrows via `@Override`. Rationale: keeps public APIs free of `<?,?>` wildcards across `MarshallingContext`, `MarshallingSession`, `ObjectRest`, `BeanSession`, swagger plumbing, etc.
5. **Parser sessions produce flavored maps/lists** (Option B). Each `ParserSession` subclass overrides a small set of protected factory hooks (`newGenericMap` / `newGenericList`) so that parsing an arbitrary value into `Object` / `Map` / `Collection` yields the marshaller's flavored type. After this lands:
   - `JsonParser.DEFAULT.parse(s, Object.class)` returns a `JsonMap`.
   - `Json5Parser.DEFAULT.parse(s, Object.class)` returns a `Json5Map`.
   - `XmlParser.DEFAULT.parse(s, Object.class)` returns an `XmlMap`.
   - …and so on for every per-marshaller subclass.
   Nested maps/lists inside a parsed structure are also produced by the same hooks, so flavor propagates uniformly through the parsed tree. The base hooks (on the neutral `MarshallingSession`) return the neutral `MarshalledMap` / `MarshalledList` for any session whose marshaller doesn't supply a flavor. See **Parser session factory hooks** below.

---

## Files in scope

### New classes (in `juneau-marshall`)

| Class | Package | Notes |
|---|---|---|
| `MarshalledMap` | `org.apache.juneau.collections` | Neutral base. Fluent methods return `MarshalledMap`. |
| `MarshalledList` | `org.apache.juneau.collections` | Neutral base. Fluent methods return `MarshalledList`. |
| `Json5Map` / `Json5List` | `org.apache.juneau.json5` | `toString()` = `Json5.of(this)`; default parser = `Json5Parser.DEFAULT`. Inherits today's `JsonMap` behavior. |
| `JsonlMap` / `JsonlList` | `org.apache.juneau.jsonl` | `toString()` = `Jsonl.of(this)`. |
| `JcsMap` / `JcsList` | `org.apache.juneau.jcs` | `toString()` = `Jcs.of(this)`. |
| `HjsonMap` / `HjsonList` | `org.apache.juneau.hjson` | `toString()` = `Hjson.of(this)`. |
| `XmlMap` / `XmlList` | `org.apache.juneau.xml` | `toString()` = `Xml.of(this)`. |
| `HtmlMap` / `HtmlList` | `org.apache.juneau.html` | `toString()` = `Html.of(this)`. |
| `UonMap` / `UonList` | `org.apache.juneau.uon` | `toString()` = `Uon.of(this)`. |
| `UrlEncodingMap` / `UrlEncodingList` | `org.apache.juneau.urlencoding` | `toString()` = `UrlEncoding.of(this)`. |
| `HoconMap` / `HoconList` | `org.apache.juneau.hocon` | `toString()` = `Hocon.of(this)`. |
| `YamlMap` / `YamlList` | `org.apache.juneau.yaml` | `toString()` = `Yaml.of(this)`. |
| `TomlMap` / `TomlList` | `org.apache.juneau.toml` | `toString()` = `Toml.of(this)`. |
| `IniMap` / `IniList` | `org.apache.juneau.ini` | `toString()` = `Ini.of(this)`. |
| `CsvMap` / `CsvList` | `org.apache.juneau.csv` | `toString()` = `Csv.of(this)`. |
| `OpenApiMap` / `OpenApiList` | `org.apache.juneau.oapi` | `toString()` = `OpenApi.of(this)`. |
| `MarkdownMap` / `MarkdownList` | `org.apache.juneau.markdown` | `toString()` = `Markdown.of(this)`. |
| `PlainTextMap` / `PlainTextList` | `org.apache.juneau.plaintext` | `toString()` = `PlainText.of(this)`. |
| `BsonMap` / `BsonList` | `org.apache.juneau.bson` | Binary. Adds `toBytes()` + `writeTo(OutputStream)`; `toString()` falls back to `Json5.of(this)` for debuggability. |
| `MsgPackMap` / `MsgPackList` | `org.apache.juneau.msgpack` | Same shape as `BsonMap`. |
| `CborMap` / `CborList` | `org.apache.juneau.cbor` | Same shape as `BsonMap`. |
| `ParquetMap` / `ParquetList` | `org.apache.juneau.parquet` | Same shape as `BsonMap`. |
| `ProtoMap` / `ProtoList` | `org.apache.juneau.proto` | Same shape as `BsonMap`. |

### New classes (in `juneau-marshall-rdf`)

Per-RDF-language pairs in `org.apache.juneau.jena.*` — final list pinned during implementation after reading the `Rdf*` marshaller wrappers. Candidates: `RdfXmlMap`/`List`, `NTripleMap`/`List`, `TurtleMap`/`List`, `N3Map`/`List`.

### Modified

| File | Change |
|---|---|
| `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/collections/JsonMap.java` | Drop neutral methods (now on base). Keep JSON-specific surface. `toString()` → `Json.of(this)`. `CharSequence`/`Reader` constructors default to `JsonParser.DEFAULT`. Subclass of `MarshalledMap`. |
| `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/collections/JsonList.java` | Same shape of change. Subclass of `MarshalledList`. |
| `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/collections/ResolvingMarshalledMap.java` | Re-parented from `JsonMap` to `MarshalledMap` and renamed from `ResolvingJsonMap` (DONE — Phase B). SVL resolution is language-agnostic. Hard rename, no deprecation shim. |
| `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/collections/package-info.java` | Documents the new hierarchy. |
| `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/MarshallingSession.java` | `newGenericMap(ClassMeta)` becomes a `protected` overridable hook returning `MarshalledMap` (was hardcoded `new JsonMap(this)`). Add matching `protected MarshalledList newGenericList()` hook. |
| Every `XxxParserSession.java` (all 26+ marshallers, plus RDF) | Override the factory hooks to return the matching flavored type (`Json5ParserSession` → `Json5Map`/`Json5List`, `XmlParserSession` → `XmlMap`/`XmlList`, etc.). Replace every existing `new JsonMap(this)` / `new JsonList(this)` literal in the parser session with the factory call so flavor is decided polymorphically. Specifically affects (non-exhaustive): `JsonParserSession`, `Json5ParserSession`, `XmlParserSession`, `HtmlParserSession`, `UonParserSession`, `UrlEncodingParserSession`, `YamlParserSession`, `TomlParserSession`, `HoconParserSession`, `HjsonParserSession`, `JcsParserSession`, `JsonlParserSession`, `IniParserSession`, `CsvParserSession`, `OpenApiParserSession`, `MarkdownParserSession`, `PlainTextParserSession`, `BsonParserSession`, `MsgPackParserSession`, `CborParserSession`, `ParquetParserSession`, `ProtoParserSession`, and the RDF parser sessions. |

### Touched but mostly unchanged

- `MarshallingContext`, `MarshallingSession`, `ObjectRest`, `BeanSession`, `Parser` / `ParserSession`, `BasicSwaggerProviderSession`, `cp.Messages`, `MarshalledFilter` related plumbing — they reference `JsonMap` / `JsonList`. Update minimally: widen return types or take `MarshalledMap` / `MarshalledList` where appropriate; otherwise leave the explicit `JsonMap` / `JsonList` references intact (their strict-JSON semantics are still correct in most of those spots).
- Annotation Javadoc strings (`Schema`, `Path`, `Query`, `Header`, etc.) that mention `JsonMap` / `JsonList` — left intact. They're now strict-JSON-specific, which matches HTTP-part annotation intent.

---

## Method partitioning

### Stays in `MarshalledMap` / `MarshalledList` (neutral)

- All `LinkedHashMap` / `LinkedList` overrides: `get`, `put`, `entrySet`, `keySet`, `containsKey`, `containsKeyNotEmpty`, `containsOuterKey`, `equals`, `hashCode`, etc.
- All typed accessors: `get(key, Class<T>)`, `getInt`, `getLong`, `getBoolean`, `getString`, `getStringArray`, `getSwapped`, `getWithDefault`, `getClassMeta`, `findKeyIgnoreCase`, `find`, `findInt`, `findLong`, `findBoolean`, `findString`, `findList`, `findMap`, `getFirstKey`, `removeBoolean`, `removeInt`, `removeString`, `removeWithDefault`.
- `getMap` / `getList` family — base versions return `MarshalledMap` / `MarshalledList`. Subclasses narrow (see template).
- Path-based ops: `getAt`, `putAt`, `postAt`, `deleteAt` — `ObjectRest`-driven, language-neutral.
- Bean integration: `cast(Class)`, `cast(ClassMeta)`, `setBeanSession`, `getMarshallingSession`, `session()`.
- Construction helpers: `create()`, `of(...)`, `ofText(CharSequence, Parser)`, `ofText(Reader, Parser)`, `filtered(...)`, `inner(...)`, `unmodifiable()`, `modifiable()`, `include`, `exclude`, `keepAll`, `removeAll`, `append`, `appendIf`, `appendIfAbsent`, `appendIfAbsentIf`, `appendFirst`.
- Polymorphic `toString(WriterSerializer)` / `toString(OutputStreamSerializer)` — language-neutral, takes the serializer as a parameter.
- `EMPTY_MAP` / `EMPTY_LIST` — neutral immutables.
- `UnmodifiableMarshalledMap` / `UnmodifiableMarshalledList` inner classes.

### Lives on each per-marshaller subclass

- Language-specific default `toString()`.
- Language-specific `writeTo(Writer)` (text) or `writeTo(OutputStream)` (binary) using that marshaller's `DEFAULT` serializer.
- Language-specific factories: `ofJson`, `ofXml`, `ofYaml`, etc., each calling the matching `XxxParser.DEFAULT`.
- Language-specific `CharSequence` / `Reader` constructors that default to the matching `XxxParser.DEFAULT`.
- Binary subclasses additionally expose: `toBytes()`, `writeTo(OutputStream)`, and a `toString()` that falls back to `Json5.of(this)` so the value remains debuggable.
- Covariant `@Override` narrowing for every fluent / get-typed-collection method (see template below).

### Lives on each per-marshaller parser session

- Override of `newGenericMap(ClassMeta)` returning the matching flavored `XMap`.
- Override of `newGenericList()` returning the matching flavored `XList`.
- See **Parser session factory hooks** below.

### Removed from `JsonMap` / `JsonList`

- `toJson5()`, `toJsonl()`, `toJcs()`, `toHjson()`, `toReadableJson5()` — move to `Json5Map`/`Json5List`, `JsonlMap`/`JsonlList`, `JcsMap`/`JcsList`, `HjsonMap`/`HjsonList` respectively.
- `putJson(String, String)` (the JSON5-flavored one) — moves to `Json5Map`. `JsonMap` gets a strict-JSON variant.

---

## Self-type strategy: covariant overrides

Each subclass narrows the return type of fluent methods and of `getMap` / `getList` (and their overloads) via `@Override` rather than threading a recursive `SELF` generic through the base. This keeps public signatures free of `<?,?>` wildcards.

### Base shape (`MarshalledMap`)

```java
public class MarshalledMap extends LinkedHashMap<String,Object> {

    public MarshalledMap  getMap(String key)                              { return get(key, MarshalledMap.class); }
    public MarshalledMap  getMap(String key, boolean createIfNotExists)   { ... }
    public MarshalledMap  getMap(String key, MarshalledMap defVal)        { ... }

    public MarshalledList getList(String key)                             { return get(key, MarshalledList.class); }
    public MarshalledList getList(String key, boolean createIfNotExists)  { ... }
    public MarshalledList getList(String key, MarshalledList defVal)      { ... }

    public MarshalledMap  append(String k, Object v)                      { ... }
    public MarshalledMap  filtered()                                      { ... }
    public MarshalledMap  inner(Map<String,Object> inner)                 { ... }
    public MarshalledMap  session(MarshallingSession s)                   { ... }
    public MarshalledMap  unmodifiable()                                  { ... }
    public MarshalledMap  modifiable()                                    { ... }
    public MarshalledMap  exclude(String...keys)                          { ... }
    public MarshalledMap  include(String...keys)                          { ... }
    public MarshalledMap  keepAll(String...keys)                          { ... }
    // ...
}
```

### Per-subclass template (`Json5Map`)

```java
public class Json5Map extends MarshalledMap {

    // Narrowing overrides — re-call get(key, ThisSubclass.class) so the runtime conversion actually targets Json5Map.
    @Override public Json5Map  getMap(String key)                            { return get(key, Json5Map.class); }
    @Override public Json5Map  getMap(String key, boolean createIfNotExists) { /* new Json5Map() when creating */ }
    @Override public Json5Map  getMap(String key, Json5Map defVal)           { ... }

    @Override public Json5List getList(String key)                           { return get(key, Json5List.class); }
    @Override public Json5List getList(String key, boolean createIfNotExists){ /* new Json5List() when creating */ }
    @Override public Json5List getList(String key, Json5List defVal)         { ... }

    @Override public Json5Map  append(String k, Object v)                    { super.append(k, v); return this; }
    @Override public Json5Map  filtered()                                    { super.filtered(); return this; }
    @Override public Json5Map  inner(Map<String,Object> inner)               { super.inner(inner); return this; }
    @Override public Json5Map  session(MarshallingSession s)                 { super.session(s); return this; }
    @Override public Json5Map  unmodifiable()                                { /* new UnmodifiableJson5Map(this) */ }
    @Override public Json5Map  modifiable()                                  { ... }
    @Override public Json5Map  exclude(String...keys)                        { ... }
    @Override public Json5Map  include(String...keys)                        { ... }
    @Override public Json5Map  keepAll(String...keys)                        { ... }

    // Language-specific surface.
    public static Json5Map ofJson5(CharSequence in) throws ParseException    { ... }
    public Json5Map writeTo(Writer w) throws IOException, SerializeException { Json5Serializer.DEFAULT.serialize(this, w); return this; }
    @Override public String toString()                                       { return Json5.of(this); }
}
```

### Practical guidance for the implementer

1. **Settle the exact override list once on the base.** It's roughly: every fluent setter that returns the receiver, plus the three `getMap` overloads, the three `getList` overloads, `unmodifiable`, `modifiable`, `exclude`, `include`, `keepAll`, `writeTo`. ~15–20 methods.
2. **Build `Json5Map` / `Json5List` first as the canonical template.** Nail the override list, generics, Javadoc, and `UnmodifiableJson5Map` inner class. Only fan out to the other 21+ subclass pairs once this is right — a bad template multiplies.
3. **`MarshallingSession` factory hooks become flavored.** `newGenericMap(ClassMeta)` and a new `newGenericList()` are `protected` hooks. Base returns the neutral `MarshalledMap` / `MarshalledList`. Each per-marshaller `ParserSession` overrides to return its flavored type. Every existing `new JsonMap(this)` / `new JsonList(this)` literal in a parser session is replaced with the hook call so the flavor is decided polymorphically. See **Parser session factory hooks** for the mechanism.
4. **`getMap` / `getList` narrowing must re-call `get(key, ThisSubclass.class)`** — covariant return alone doesn't change the runtime conversion target. Add a single Javadoc sentence on the base method that explicitly documents this contract for subclasses.
5. **Old code keeps working downstream** because `Json5Map` is-a `MarshalledMap` is-a `LinkedHashMap<String,Object>` — assignments to `Map<String,Object>` are fine.

---

## Parser session factory hooks

This is the mechanism that makes design decision 5 (parser-produced flavor) work. Each parser session overrides a small set of protected factory hooks to control the runtime type of maps/lists created during parsing.

### Base hooks on `MarshallingSession`

```java
protected MarshalledMap newGenericMap(ClassMeta<?> mapMeta) {
    var k = mapMeta.getKeyType();
    return (k == null || k.isString()) ? new MarshalledMap(this) : map();
}

protected MarshalledList newGenericList() {
    return new MarshalledList(this);
}
```

Today only `newGenericMap(ClassMeta)` exists, and it returns `new JsonMap(this)`. As part of Phase B it gets re-typed to return `MarshalledMap` and `newGenericList()` is added alongside it. Both must be `protected` (not `final`, not `package-private`) so per-marshaller parser sessions can override.

### Per-marshaller override pattern

```java
// Json5ParserSession
@Override protected Json5Map  newGenericMap(ClassMeta<?> mapMeta) { return new Json5Map(this); }
@Override protected Json5List newGenericList()                    { return new Json5List(this); }

// XmlParserSession
@Override protected XmlMap  newGenericMap(ClassMeta<?> mapMeta) { return new XmlMap(this); }
@Override protected XmlList newGenericList()                    { return new XmlList(this); }
```

…and so on for every parser session listed in **Files in scope > Modified**.

### Call-site migration inside parser sessions

Every direct `new JsonMap(this)` and `new JsonList(this)` literal in a parser session (see Phase A grep for the full list — `UrlEncodingParserSession`, `YamlParserSession`, `TomlParserSession`, `MarkdownParserSession`, `ProtoParserSession`, etc.) must be migrated to `newGenericMap(...)` / `newGenericList()` calls. Audit-style replacement, no logic change beyond the type.

For the unconditional non-factory paths (e.g. `UrlEncodingParserSession.java:273` which currently allocates a `JsonMap` for an HTTP-part-specific code path), keep it locally scoped: replace with `new MarshalledMap(this)` if no flavor is desired at that callsite, otherwise route through the same hook. The audit pass during Phase B / Phase C identifies which way each line goes.

### What this does *not* affect

- **Serializers don't need a parallel hook.** Serializers walk inputs polymorphically; any flavored map/list serializes correctly via its own `toString()` / `writeTo` / serializer-driven path.
- **Callers that pass an explicit target class** (`parser.parse(text, JsonMap.class)`) get the type they asked for — the hook is only consulted for `Object` / `Map<String,Object>` / `Collection<Object>` targets.
- **Bean-targeted parsing** (`parser.parse(text, MyBean.class)`) is unaffected — the parser fills the bean, no generic map is constructed.

### Test impact

The Phase A `MarshalledMap_Test.a08b_parseProducesJsonMapInternally` test documents today's behavior:

```java
var m = MarshalledMap.ofText("{nested:{x:1}}", Json5Parser.DEFAULT);
var nested = m.getMap("nested");
assertTrue(nested instanceof JsonMap, "Json5Parser currently produces JsonMap for nested objects");
```

In Phase B this becomes:

```java
var m = MarshalledMap.ofText("{nested:{x:1}}", Json5Parser.DEFAULT);
var nested = m.getMap("nested");
assertTrue(nested instanceof Json5Map, "Json5Parser produces Json5Map for nested objects");
assertFalse(nested instanceof JsonMap, "Strict-JSON JsonMap is not used by Json5Parser");
```

Note: the outer `m` stays a plain `MarshalledMap` because the *caller* (the `ofText` factory) instantiated `MarshalledMap` and the parser filled it in place. Only the parser-created nested structures pick up the flavor. This is intentional and documented.

---

## Migration / call-site updates

- **No deprecated aliases needed.** `JsonMap` / `JsonList` are kept (just retargeted to strict JSON); `Json5Map` / `Json5List` are net-new.
- **Behavioral break audit**: every internal caller of `JsonMap.toString()` that depends on JSON5 must be migrated in this PR to either `Json5Map` or an explicit `Json5.of(...)`. Candidates to audit:
  - `MarshallingContext`
  - `BasicSwaggerProviderSession` (the `JsonMap` usage in `juneau-rest-server/src/main/java/org/apache/juneau/rest/swagger/BasicSwaggerProviderSession.java`)
  - `cp.Messages`
  - REST argument parsing (`FormDataArg`, `HeaderArg`, `PathArg`, `QueryArg`)
  - `Microservice` / `ConfigResource` (`juneau-microservice-core`)
  - Tests under `juneau-utest` and `juneau-examples` — any `assertEquals("{...}", jsonMap.toString())` style assertions need to switch instance type or comparator.
- **Same-package `JsonMap` constructor change** (`JsonMap(CharSequence)` now defaults to `JsonParser` instead of `Json5Parser`): audit callers that pass JSON5-with-unquoted-keys text into the bare constructor — they must switch to `Json5Map(CharSequence)` or pass an explicit `Json5Parser.DEFAULT`.
- **Parser-produced flavor change**: any code that calls `parser.parse(text, Object.class)` / `parser.parse(text, Map.class)` and then casts the result to `JsonMap` or `JsonList` will break for non-strict-JSON parsers. After Phase B, the runtime type matches the parser flavor (`Json5Map`, `XmlMap`, etc.). Recommended fixes:
  - Switch the cast to the matching flavored type, or
  - Use the neutral `MarshalledMap` / `MarshalledList` if the cast site doesn't depend on flavor, or
  - Pass an explicit target type — e.g. `parser.parse(text, JsonMap.class)` — to force the old runtime type.
  Specifically audit: `BasicSwaggerProviderSession`, `cp.Messages`, REST argument parsing (`FormDataArg`, `HeaderArg`, `PathArg`, `QueryArg`), `Microservice`, `ConfigResource`, and the existing `juneau-utest` / `juneau-examples` casts uncovered by the test suite.

---

## Documentation & release notes

- **v9.5 Migration Guide** (`juneau-docs/pages/topics/23.01.V9.5-migration-guide.md`):
  - `JsonMap.toString()` / `JsonList.toString()` now return strict JSON (was JSON5). Replace with `Json5Map` / `Json5List` to preserve old behavior.
  - `JsonMap(CharSequence)` / `JsonList(CharSequence)` default parser changed from `Json5Parser` to `JsonParser`. Same advice.
- **Release notes** (`juneau-docs/pages/release-notes/9.5.0.md`):
  - Headline: new neutral `MarshalledMap` / `MarshalledList` base + per-marshaller `XMap` / `XList` subclasses for all supported languages.
  - Per-module note under `juneau-marshall` listing the new packages.
- **Package Javadoc** (`org.apache.juneau.collections/package-info.java`): update to document the new hierarchy and the strict-JSON vs JSON5 split.
- **Per-marshaller package Javadoc**: brief mention of the new `XMap` / `XList` pair where it lands.

---

## Tests

- New `MarshalledMapTest` / `MarshalledListTest` under `juneau-utest` exercising the neutral base directly (no language coupling). **(DONE — Phase A)**
- Per-subclass smoke tests confirming:
  - `toString()` produces the expected language form.
  - `writeTo` round-trips through the matching parser.
  - `getMap` / `getList` return the correct subclass and not the base.
  - `unmodifiable()` returns the same flavored subclass, not the base.
  - **Parser-produced flavor**: `XxxParser.DEFAULT.parse(text, Object.class) instanceof XxxMap` → true. Same for `XxxList` on array input. Nested maps/lists inside a parsed structure are also `XxxMap` / `XxxList`.
  - For binary subclasses: `toBytes()` round-trips through the matching parser; `XxxParserSession.newGenericMap` / `newGenericList` produce the binary flavor.
- Update `JsonMap_Test` / `JsonList_Test` to assert strict-JSON `toString()` output (with paired `Json5Map_Test` / `Json5List_Test` carrying the old JSON5 assertions).
- Update `MarshalledMap_Test.a08b_parseProducesJsonMapInternally` to assert the post-Phase-B behavior: nested parser-created objects are `Json5Map` (and not `JsonMap`). See **Parser session factory hooks > Test impact** for the exact assertion.

---

## Out of scope (follow-on TODOs)

- Moving `MarshalledMap` / `MarshalledList` to `juneau-commons` — they still depend on `MarshallingSession`, `ClassMeta`, `BeanMap`, `ObjectRest`. Dovetails with TODO-30 (ClassMeta to commons).
- Bulk-migration of every existing `JsonMap` / `JsonList` call site to `MarshalledMap` / `MarshalledList` where typed JSON behavior isn't needed. Done opportunistically; not a goal of this TODO.

---

## Risks

- **Behavioral break on `JsonMap.toString()`** — must be aggressively communicated. Audit any places where `JsonMap.toString()` is fed back into a JSON5 parser as a round-trip; those now need `Json5Map`.
- **Parser-produced flavor change** (Option B): existing code that assumes `parser.parse(text, Object.class) instanceof JsonMap` will break for every non-strict-JSON parser. The "Migration / call-site updates" audit list above must be cleared in the same PR. Compile-only failures are easy; the dangerous ones are silent runtime `ClassCastException`s deep in plugin code. Mitigation: search the entire monorepo (and `juneau-examples`) for `instanceof JsonMap`, `(JsonMap)`, `instanceof JsonList`, `(JsonList)`, plus the same forms with `Map<String,Object>` / `List<Object>` patterns; touch every one.
- **Surface area**: ~46+ new classes plus ~700 trivial covariant overrides across them. Each override is one line, but the volume means the first subclass pair (`Json5Map` / `Json5List`) must nail the template exactly before fanning out.
- **Binary `toString()` falling back to JSON5** may surprise users — call out explicitly in the Javadoc of every binary subclass.

---

## Suggested execution order

1. **Sketch the override template.** Lock the exact set of methods to override on the base. Document it on `MarshalledMap` / `MarshalledList` Javadoc. **(DONE — Phase A)**
2. **Extract `MarshalledMap` / `MarshalledList`.** Move the neutral methods from current `JsonMap` / `JsonList`. Make `JsonMap extends MarshalledMap`, `JsonList extends MarshalledList`. Compile. **(DONE — Phase A)**
3. **Retarget `JsonMap` / `JsonList` to strict JSON.** Switch `toString()` and the default parser. Run the test suite — any failures are the behavioral-break audit list. **(DONE — Phase C step 4)** — bundled with the cast-site sweep to avoid the Phase B regression. See the **Phase C status** section above for the full migrated-callers audit.
4. **Open the `MarshallingSession` factory hooks.** Re-type `newGenericMap` to return `MarshalledMap` (was hardcoded `new JsonMap(this)`); add `newGenericList()`. Override in `JsonParserSession` (and `Json5ParserSession`, see step 6) to keep the JSON family's observable behavior consistent with the new flavored model. **(DONE — Phase B + Phase C step 5)** — hooks opened in Phase B with `JsonParserSession` override in place; Phase C step 5 flipped the base default from `JsonMap` / `JsonList` to the neutral `MarshalledMap` / `MarshalledList`.
5. **Migrate `new JsonMap(this)` / `new JsonList(this)` call sites inside parser sessions** to use the new hooks. Phase A's grep already enumerated them (`UrlEncodingParserSession`, `YamlParserSession`, `TomlParserSession`, `MarkdownParserSession`, `ProtoParserSession`, plus the JSON family). At this stage every parser still produces `MarshalledMap` / `MarshalledList` by default; only `JsonParserSession` (and `Json5ParserSession` after step 6) is flavored. **(DONE — Phase B step 5 + Phase C step 2)** — `JsonParserSession` migrated in Phase B; the remaining parser sessions migrated in Phase C step 2 ahead of the Phase C step 3 flavor flip. After Phase C step 5, every non-JSON parser produces `MarshalledMap` / `MarshalledList` from the base default until Phase D adds per-language overrides.
6. **Add `Json5Map` / `Json5List`** and **override `Json5ParserSession.newGenericMap` / `newGenericList`** to return them. This is the template subclass — get it perfectly right (class shape, factory overrides, parser session override) before continuing. **(DONE — Phase B + Phase C step 3)** — `Json5Map` / `Json5List` classes added with smoke tests in Phase B; the `Json5ParserSession` override landed in Phase C step 3 once the cast-site sweep made it safe.
7. **Re-parent `ResolvingJsonMap` to `MarshalledMap`** (and rename to `ResolvingMarshalledMap`). **(DONE — Phase B)** — outright rename, no deprecation shim.
8. **Migrate internal callers** that depend on JSON5 `toString()` to `Json5Map` / explicit `Json5.of(...)`. Also migrate any cast sites the new parser-flavor behavior would break (see Migration audit list). **(DONE — Phase C step 4)** — landed atomically with the `JsonMap` / `JsonList` retargeting (`toString` → strict JSON, default parser → `JsonParser.DEFAULT`). See the **Phase C status** section above for the full migrated-callers audit.
9. **Fan out**: JSON family (`JsonlMap`, `JcsMap`, `HjsonMap`), then text non-JSON, then binary, then RDF. For **each** subclass, add both:
   - The flavored `XMap` / `XList` class (copy of the template with class names and serializer/parser references swapped).
   - The matching `XxxParserSession.newGenericMap` / `newGenericList` overrides.
   **(TODO — Phase D)** — renumbered from Phase C; runs after the Phase C behavioral switch lands.
10. **Docs**: migration guide, release notes, package Javadoc. **(TODO — Phase E)**
11. **Tests**: per-subclass smoke tests (including `parser.parse(s, Object.class) instanceof XMap` assertions and nested-flavor assertions) + the updated `JsonMap_Test` / `JsonList_Test`. Update `MarshalledMap_Test.a08b_parseProducesJsonMapInternally` to expect `Json5Map` per the **Parser session factory hooks** test-impact note. **(TODO — Phase E)**
