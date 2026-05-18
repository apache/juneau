# TODO-34 — Generalize `JsonMap` / `JsonList` into `MarshalledMap` / `MarshalledList` + per-language subclasses

Source: promoted from `TODO.md` on 2026-05-18.

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
| `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/collections/ResolvingJsonMap.java` | Re-parent from `JsonMap` to `MarshalledMap`. SVL resolution is language-agnostic. Class rename to `ResolvingMarshalledMap` deferred as a follow-on. |
| `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/collections/package-info.java` | Documents the new hierarchy. |

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
3. **`MarshallingSession` factory methods** (`newMap`, `newCollection`, etc.) should keep returning the neutral `MarshalledMap` / `MarshalledList`. Add overloads taking `Class<? extends MarshalledMap>` only when an actual call-site needs a flavored result.
4. **`getMap` / `getList` narrowing must re-call `get(key, ThisSubclass.class)`** — covariant return alone doesn't change the runtime conversion target. Add a single Javadoc sentence on the base method that explicitly documents this contract for subclasses.
5. **Old code keeps working downstream** because `Json5Map` is-a `MarshalledMap` is-a `LinkedHashMap<String,Object>` — assignments to `Map<String,Object>` are fine.

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

- New `MarshalledMapTest` / `MarshalledListTest` under `juneau-utest` exercising the neutral base directly (no language coupling).
- Per-subclass smoke tests confirming:
  - `toString()` produces the expected language form.
  - `writeTo` round-trips through the matching parser.
  - `getMap` / `getList` return the correct subclass and not the base.
  - `unmodifiable()` returns the same flavored subclass, not the base.
  - For binary subclasses: `toBytes()` round-trips through the matching parser.
- Update `JsonMap_Test` / `JsonList_Test` to assert strict-JSON `toString()` output (with paired `Json5Map_Test` / `Json5List_Test` carrying the old JSON5 assertions).

---

## Out of scope (follow-on TODOs)

- Renaming `ResolvingJsonMap` to `ResolvingMarshalledMap` (parent change is in scope; class rename punted to limit blast radius).
- Moving `MarshalledMap` / `MarshalledList` to `juneau-commons` — they still depend on `MarshallingSession`, `ClassMeta`, `BeanMap`, `ObjectRest`. Dovetails with TODO-30 (ClassMeta to commons).
- Bulk-migration of every existing `JsonMap` / `JsonList` call site to `MarshalledMap` / `MarshalledList` where typed JSON behavior isn't needed. Done opportunistically; not a goal of this TODO.

---

## Risks

- **Behavioral break on `JsonMap.toString()`** — must be aggressively communicated. Audit any places where `JsonMap.toString()` is fed back into a JSON5 parser as a round-trip; those now need `Json5Map`.
- **Surface area**: ~46+ new classes plus ~700 trivial covariant overrides across them. Each override is one line, but the volume means the first subclass pair (`Json5Map` / `Json5List`) must nail the template exactly before fanning out.
- **Binary `toString()` falling back to JSON5** may surprise users — call out explicitly in the Javadoc of every binary subclass.

---

## Suggested execution order

1. **Sketch the override template.** Lock the exact set of methods to override on the base. Document it on `MarshalledMap` / `MarshalledList` Javadoc.
2. **Extract `MarshalledMap` / `MarshalledList`.** Move the neutral methods from current `JsonMap` / `JsonList`. Make `JsonMap extends MarshalledMap`, `JsonList extends MarshalledList`. Compile.
3. **Retarget `JsonMap` / `JsonList` to strict JSON.** Switch `toString()` and the default parser. Run the test suite — any failures are the behavioral-break audit list.
4. **Add `Json5Map` / `Json5List`.** This is the template subclass. Get it perfectly right before continuing.
5. **Re-parent `ResolvingJsonMap` to `MarshalledMap`.**
6. **Migrate internal callers** that depend on JSON5 `toString()` to `Json5Map` / explicit `Json5.of(...)`.
7. **Fan out**: JSON family (`JsonlMap`, `JcsMap`, `HjsonMap`), then text non-JSON, then binary, then RDF. Each subclass is a copy of the template with class names and serializer/parser references swapped.
8. **Docs**: migration guide, release notes, package Javadoc.
9. **Tests**: per-subclass smoke tests + the updated `JsonMap_Test` / `JsonList_Test`.
