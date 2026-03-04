# JSONL (JSON Lines) Implementation Plan for Apache Juneau

## Overview

JSONL (JSON Lines, also called Newline-Delimited JSON / NDJSON) is a text format where each line is a valid JSON value, separated by newline characters (`\n`). It is the standard format for LLM fine-tuning datasets, streaming AI inference, log aggregation, and bulk data pipelines.

This plan covers implementing JSONL serializer and parser support in Juneau by extending the existing `JsonSerializer`/`JsonParser` infrastructure, following the `Json5Serializer`/`Json5Parser` extension pattern.

## Specification

- **Media types**: `application/jsonl`, `application/x-ndjson`, `text/jsonl`
- **RFC reference**: https://jsonlines.org/
- **Key rules**:
  - Each line is a valid JSON value (typically an object)
  - Lines are separated by `\n` (newline)
  - No trailing comma or wrapping array brackets
  - Empty lines are ignored
  - UTF-8 encoding

## Architecture

JSONL builds directly on the existing JSON infrastructure. The key difference is at the collection/array level:

- **Standard JSON**: `[{"a":1},{"a":2},{"a":3}]`
- **JSONL**: `{"a":1}\n{"a":2}\n{"a":3}\n`

### Serializer Approach

`JsonlSerializer` extends `JsonSerializer`. It overrides session creation to use `JsonlSerializerSession`, which overrides `doSerialize()` to:
- If the object is a collection/array, serialize each element as a separate JSON line (no wrapping `[]`, no commas)
- If the object is a single value, serialize it as a single JSON line
- Each line uses the existing `serializeAnything()` method from `JsonSerializerSession`

### Parser Approach

`JsonlParser` extends `JsonParser`. It overrides session creation to use `JsonlParserSession`, which overrides `doParse()` to:
- Read input line by line
- Parse each non-empty line as a JSON value using the existing `parseAnything()` method from `JsonParserSession`
- Collect results into a `List`/`Collection` or array depending on the target type
- If the target type is a single object (not collection/array), parse only the first line

---

## Round-Trip Parity with JSON

JSONL supports **all the same data structures** as JSON:

- Maps and beans → serialized as JSON objects, one per line
- Collections and arrays → each element becomes one JSONL line
- Strings → JSON string literals, one per line
- Numbers → JSON number literals, one per line
- Booleans → `true` / `false`, one per line
- Null → `null`, one per line
- Nested objects and collections → stay compact on a single line per outer element
- Typed round-trip via `_type` attribute (using `addBeanTypes()` / `addRootType()`)
- ObjectSwaps, date/time types (ISO 8601), Iterators, and Streams

The only structural difference from JSON is at the **top-level container**: JSONL treats the top level as a sequence of values (one per line) rather than a single value. Collections and arrays round-trip naturally. Single non-collection objects produce a single JSONL line and round-trip identically to JSON.

### Limitations compared to JSON

Following the pattern established by `YamlSerializer` (`<h5 class='section'>Limitations compared to JSON</h5>`):

1. **No pretty-printing**: `useWhitespace()` is always suppressed in `JsonlSerializerSession`. JSONL requires each JSON value to fit on a single line; applying intra-line pretty-printing would embed newlines and produce invalid JSONL. The setting is silently ignored regardless of its configured value.

2. **No multi-line JSON input**: The parser reads one JSON value per line. Multi-line (pretty-printed) JSON piped as JSONL input will cause parse errors.

3. **Top-level semantics differ**: JSON treats the top-level as a single value. JSONL treats the top level as a stream of values. When parsing JSONL to a single non-collection type, only the first line is parsed; any trailing lines are silently ignored.

---

## Files to Create

All new source files go in the `juneau-core/juneau-marshall` module under `src/main/java/org/apache/juneau/jsonl/`.

### 1. `JsonlSerializer.java`

Extends `JsonSerializer`. Follows the `Json5Serializer` pattern.

```java
package org.apache.juneau.jsonl;

public class JsonlSerializer extends JsonSerializer {

    public static final JsonlSerializer DEFAULT = new JsonlSerializer(create());

    public static JsonSerializer.Builder create() {
        return JsonSerializer.create()
            .produces("application/jsonl")
            .accept("application/jsonl,application/x-ndjson,text/jsonl")
            .type(JsonlSerializer.class);
    }

    public JsonlSerializer(JsonSerializer.Builder builder) {
        super(builder);
    }

    @Override
    public JsonlSerializerSession.Builder createSession() {
        return JsonlSerializerSession.create(this);
    }
}
```

Key design decisions:
- Reuses `JsonSerializer.Builder` (no separate builder needed, same as `Json5Serializer`)
- Overrides `createSession()` to return a `JsonlSerializerSession` that changes serialization behavior at the collection level
- Provides `DEFAULT` static instance

### 2. `JsonlSerializerSession.java`

Extends `JsonSerializerSession`. Overrides `doSerialize()` to emit one JSON object per line.

```java
package org.apache.juneau.jsonl;

public class JsonlSerializerSession extends JsonSerializerSession {

    @Override
    protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
        JsonWriter w = getJsonWriter(out);
        ClassMeta<?> cm = getClassMetaForObject(o);

        if (cm != null && (cm.isCollection() || cm.isArray())) {
            Collection<?> c = cm.isArray() ? toList(cm.inner(), o) : (Collection<?>) o;
            ClassMeta<?> elementType = cm.getElementType();
            for (Object item : c) {
                serializeAnything(w.i(getInitialDepth()), item, elementType, "root", null);
                w.w('\n');
            }
        } else {
            serializeAnything(w.i(getInitialDepth()), o, getExpectedRootType(o), "root", null);
            w.w('\n');
        }
    }
}
```

Key behavior:
- Collections and arrays: each element is serialized as a compact JSON line (no whitespace between lines, no wrapping brackets)
- Single objects: serialized as a single JSON line
- Each line terminated with `\n`
- Reuses `serializeAnything()` from `JsonSerializerSession` for all JSON serialization logic

### 3. `JsonlParser.java`

Extends `JsonParser`. Follows the `Json5Parser` pattern.

```java
package org.apache.juneau.jsonl;

public class JsonlParser extends JsonParser {

    public static final JsonlParser DEFAULT = new JsonlParser(create());

    public static JsonParser.Builder create() {
        return JsonParser.create()
            .consumes("application/jsonl,application/x-ndjson,text/jsonl");
    }

    public JsonlParser(JsonParser.Builder builder) {
        super(builder);
    }

    @Override
    public JsonlParserSession.Builder createSession() {
        return JsonlParserSession.create(this);
    }
}
```

### 4. `JsonlParserSession.java`

Extends `JsonParserSession`. Overrides `doParse()` to read line-by-line and parse each line as a JSON value.

```java
package org.apache.juneau.jsonl;

public class JsonlParserSession extends JsonParserSession {

    @Override
    protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {
        try (ParserReader r = pipe.getParserReader()) {
            if (r == null)
                return null;

            // If target type is a collection or array, parse each line as an element
            if (type.isCollectionOrArray()) {
                ClassMeta<?> elementType = type.getElementType();
                Collection<Object> results = new JsonList(this);
                // Read lines, parse each non-empty line as JSON
                String line;
                while ((line = readLine(r)) != null) {
                    if (!line.trim().isEmpty()) {
                        ParserReader lineReader = createParserReader(line);
                        Object item = parseAnything(elementType, lineReader, getOuter(), null);
                        results.add(item);
                    }
                }
                // Convert to array if needed
                return convertToType(results, type);
            }

            // Single object: parse just the first non-empty line
            return super.doParse(pipe, type);
        }
    }
}
```

Key behavior:
- Collection/array target types: reads each line, parses as JSON, collects into list, converts to target type
- Single object target types: delegates to `JsonParserSession.doParse()` (parses the first JSON value)
- Empty lines are skipped
- Uses existing `parseAnything()` for all JSON parsing logic

### 5. `Jsonl.java` (Marshaller)

Location: `src/main/java/org/apache/juneau/marshaller/Jsonl.java`

Extends `CharMarshaller`, following the exact pattern of `Json5.java`.

```java
package org.apache.juneau.marshaller;

public class Jsonl extends CharMarshaller {

    public static final Jsonl DEFAULT = new Jsonl();

    public static String of(Object object) throws SerializeException {
        return DEFAULT.write(object);
    }

    public static Object of(Object object, Object output) throws SerializeException, IOException {
        DEFAULT.write(object, output);
        return output;
    }

    public static <T> T to(Object input, Class<T> type) throws ParseException, IOException {
        return DEFAULT.read(input, type);
    }

    public static <T> T to(Object input, Type type, Type... args) throws ParseException, IOException {
        return DEFAULT.read(input, type, args);
    }

    public static <T> T to(String input, Class<T> type) throws ParseException {
        return DEFAULT.read(input, type);
    }

    public static <T> T to(String input, Type type, Type... args) throws ParseException {
        return DEFAULT.read(input, type, args);
    }

    public Jsonl() {
        this(JsonlSerializer.DEFAULT, JsonlParser.DEFAULT);
    }

    public Jsonl(JsonlSerializer s, JsonlParser p) {
        super(s, p);
    }
}
```

### 6. `package-info.java`

Location: `src/main/java/org/apache/juneau/jsonl/package-info.java`

Standard Apache license header and package-level Javadoc explaining JSONL support.

### 7. `annotation/JsonlConfig.java`

Annotation for specifying config properties for REST classes and methods. Empty initially (rank only), following the `MarkdownConfig`/`TomlConfig` pattern.

```java
@Target({ TYPE, METHOD })
@Retention(RUNTIME)
@Inherited
@ContextApply({ JsonlConfigAnnotation.SerializerApply.class, JsonlConfigAnnotation.ParserApply.class })
public @interface JsonlConfig {
    int rank() default 0;
}
```

### 8. `annotation/JsonlConfigAnnotation.java`

Utility class for the `@JsonlConfig` annotation, with no-op `SerializerApply` and `ParserApply` appliers (JSONL reuses `JsonSerializer.Builder`, so no format-specific settings initially).

---

## Files to Modify

### 1. REST Configuration: `BasicUniversalConfig.java`

Path: `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/config/BasicUniversalConfig.java`

Add `JsonlSerializer.class` to the `serializers` array and `JsonlParser.class` to the `parsers` array in the `@Rest` annotation, alongside the other JSON variants.

### 2. REST Configuration: `BasicJsonlConfig.java` (New)

Path: `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/config/BasicJsonlConfig.java`

Create a new config interface following the `BasicJson5Config` pattern:

```java
@Rest(
    serializers={JsonlSerializer.class},
    parsers={JsonlParser.class},
    defaultAccept="application/jsonl"
)
public interface BasicJsonlConfig extends DefaultConfig {}
```

### 3. REST Client: `RestClient.java`

Path: `juneau-rest/juneau-rest-client/src/main/java/org/apache/juneau/rest/client/RestClient.java`

Add `JsonlSerializer.class` and `JsonlParser.class` to the `universal()` method's serializers/parsers lists.

### 4. Context: `Context.java`

Path: `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/Context.java`

Add `import org.apache.juneau.jsonl.annotation.*;` and add `{@link JsonlConfig}` to the list of config annotations in the `applyAnnotations()` Javadoc (alphabetically between JsonConfig and JsonSchemaConfig).

---

## Test Plan

All test files go in `juneau-utest/src/test/java/`.

### 1. Serializer Tests: `org/apache/juneau/jsonl/Jsonl_Test.java`

Test cases:

- **a01_serializeCollectionOfBeans** -- Verify that a `List<Bean>` serializes to one JSON object per line with no wrapping brackets
- **a02_serializeArray** -- Verify that a `Bean[]` array serializes identically to a collection
- **a03_serializeSingleBean** -- Verify that a single bean serializes as a single JSON line
- **a04_serializeCollectionOfMaps** -- Verify that `List<Map>` produces one JSON object per line
- **a05_serializeCollectionOfStrings** -- Verify that `List<String>` produces one quoted string per line
- **a06_serializeCollectionOfNumbers** -- Verify that `List<Integer>` produces one number per line
- **a07_serializeEmptyCollection** -- Verify that an empty collection produces empty output
- **a08_serializeNullValues** -- Verify null handling in collections (produces `null\n` lines)
- **a09_serializeNestedObjects** -- Verify that beans containing nested beans/collections serialize correctly (nested structure stays on one line)
- **a10_serializeWithSwaps** -- Verify ObjectSwap support works in JSONL context
- **a11_noTrailingComma** -- Verify no commas between lines
- **a12_noWhitespace** -- Verify compact output (no indentation between lines, though individual JSON values may be compact)

### 2. Parser Tests: `org/apache/juneau/jsonl/JsonlParser_Test.java`

Test cases:

- **b01_parseToListOfBeans** -- Parse multi-line JSONL into `List<Bean>`
- **b02_parseToArray** -- Parse multi-line JSONL into `Bean[]`
- **b03_parseSingleLine** -- Parse single-line JSONL into a single bean
- **b04_parseWithEmptyLines** -- Verify empty lines between JSON lines are skipped
- **b05_parseToListOfMaps** -- Parse JSONL into `List<JsonMap>`
- **b06_parseToListOfStrings** -- Parse JSONL with string values into `List<String>`
- **b07_parseEmptyInput** -- Parse empty input returns empty collection or null
- **b08_parseWithTrailingNewline** -- Verify trailing newline doesn't create extra element
- **b09_parseNestedObjects** -- Parse JSONL with nested JSON objects
- **b10_parseWithSwaps** -- Verify ObjectSwap support during parsing
- **b11_parseMalformedLine** -- Verify ParseException on invalid JSON in a line

### 3. Round-Trip Tests: `org/apache/juneau/jsonl/JsonlRoundTrip_Test.java`

Test cases:

- **c01_roundTripBeanCollection** -- Serialize `List<Bean>` to JSONL, parse back, verify equality
- **c02_roundTripBeanArray** -- Serialize `Bean[]` to JSONL, parse back, verify equality
- **c03_roundTripComplexBeans** -- Round-trip beans with nested objects, collections, maps
- **c04_roundTripWithNulls** -- Round-trip collections containing null elements
- **c05_roundTripMixedTypes** -- Round-trip `List<Object>` with mixed value types
- **c06_roundTripWithSwaps** -- Round-trip with ObjectSwap transformations
- **c07_roundTripSingleObject** -- Round-trip a single object (not collection)
- **c08_roundTripEmptyCollection** -- Round-trip empty collection

### 4. Marshaller Tests: `org/apache/juneau/marshaller/Jsonl_Test.java`

Follow the `Json5_Test.java` pattern:

- **d01_of** -- Test `Jsonl.of()` static method for serialization
- **d02_to** -- Test `Jsonl.to()` static method for parsing
- **d03_roundTrip** -- Test `Jsonl.of()` followed by `Jsonl.to()`
- **d04_defaultInstance** -- Test `Jsonl.DEFAULT.write()` and `Jsonl.DEFAULT.read()`

### 5. Media Type Tests: `org/apache/juneau/jsonl/JsonlMediaType_Test.java`

- **e01_producesCorrectMediaType** -- Verify serializer produces `application/jsonl`
- **e02_acceptsAllMediaTypes** -- Verify serializer accepts `application/jsonl`, `application/x-ndjson`, `text/jsonl`
- **e03_consumesAllMediaTypes** -- Verify parser consumes all three JSONL media types
- **e04_contentNegotiation** -- Verify correct serializer is selected via Accept header in REST context

### 6. Edge Case Tests: `org/apache/juneau/jsonl/JsonlEdgeCases_Test.java`

- **f01_veryLargeCollection** -- Serialize/parse a collection with 10,000+ elements
- **f02_unicodeContent** -- JSONL with Unicode characters in values
- **f03_specialCharactersInStrings** -- Strings containing `\n`, `\r`, `\t`, quotes
- **f04_deeplyNestedObjects** -- Objects nested 10+ levels deep (each still on one line)
- **f05_emptyObjects** -- Lines containing `{}` or `[]`
- **f06_mixedLineEndings** -- Input with `\r\n` (Windows) line endings
- **f07_whitespaceOnlyLines** -- Lines with only spaces/tabs are treated as empty
- **f08_booleanAndNullLines** -- Lines containing bare `true`, `false`, `null`
- **f09_numericLines** -- Lines containing bare numbers

---

### 7. Cross-Format Round-Trip Integration: `org/apache/juneau/a/rttests/RoundTripDateTime_Test.java`

Add a `RoundTrip_Tester` entry for this format to the `TESTERS` array in `RoundTripDateTime_Test.java`. This test verifies date/time and Duration round-trip across all serializer/parser combinations. It covers beans with `Instant`, `ZonedDateTime`, `LocalDate`, `LocalDateTime`, `LocalTime`, `OffsetDateTime`, `OffsetTime`, `Year`, `YearMonth`, `Calendar`, `Date`, `Duration`, and `XMLGregorianCalendar` fields.

Insert the entry **after the existing JSON testers (after the entry with ID 3)** and **before the XML testers (currently ID 4)**. All subsequent tester IDs shift up by 1.

```java
tester(4, "Jsonl - default")
    .serializer(JsonlSerializer.create().keepNullProperties().addBeanTypes().addRootType())
    .parser(JsonlParser.create())
    .skipIf(x -> !(x instanceof Collection || x.getClass().isArray()))
    .build(),
```

The `.skipIf()` guard is required because the round-trip test passes individual typed objects (not collections) to the tester. JSONL only guarantees lossless round-trip for collections and arrays at the top level; parsing JSONL to a single non-collection type returns only the first line, which does not provide a complete round-trip for the multi-field date/time beans used by this test. The guard skips all non-collection/non-array test objects for the JSONL tester.

## Documentation

### 1. Javadoc

Each new class must have comprehensive Javadoc following the existing Juneau patterns, using Juneau's custom HTML tags (`<jc>`, `<jsf>`, `<jsm>`, `<jv>`, `<bc>`, `<bjson>`, `<bjava>`, `<c>`) for code formatting.

---

#### `JsonlSerializer` — Class-level Javadoc template

Follows the `YamlSerializer` structure exactly:

```
/**
 * Serializes POJO models to JSONL (JSON Lines).
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Accept</c> types:  <bc>application/jsonl, application/x-ndjson, text/jsonl</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>application/jsonl</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * Serializes POJOs to JSONL format where each top-level value is written as a compact JSON
 * value on its own line.  The conversion is as follows...
 * <ul class='spaced-list'>
 *   <li>Maps and beans are converted to JSON objects, one per line.
 *   <li>Collections and arrays are converted to one JSON value per line (no wrapping brackets).
 *   <li>Strings are converted to JSON strings, one per line.
 *   <li>Numbers are converted to JSON numbers, one per line.
 *   <li>Booleans are converted to JSON booleans, one per line.
 *   <li>Nulls are converted to JSON nulls, one per line.
 *   <li>Nested objects and collections remain compact on a single line per outer element.
 * </ul>
 * <p>
 * Non-JSON-primitive types are transformed through
 * {@link org.apache.juneau.swap.ObjectSwap ObjectSwaps}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 *   <jc>// Use the default serializer to serialize a list of POJOs</jc>
 *   String <jv>jsonl</jv> = JsonlSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>myList</jv>);
 *
 *   <jc>// Create a custom serializer</jc>
 *   JsonlSerializer <jv>serializer</jv> = JsonlSerializer.<jsm>create</jsm>().build();
 *
 *   <jc>// Clone an existing serializer</jc>
 *   <jv>serializer</jv> = JsonlSerializer.<jsf>DEFAULT</jsf>.copy().build();
 *
 *   <jc>// Serialize a list of POJOs to JSONL</jc>
 *   String <jv>jsonl</jv> = <jv>serializer</jv>.serialize(<jv>myList</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (List of beans):</h5>
 * <p class='bjson'>
 *   {"name":"Alice","age":30}
 *   {"name":"Bob","age":25}
 *   {"name":"Carol","age":35}
 * </p>
 *
 * <h5 class='figure'>Complex (bean with nested object and array):</h5>
 * <p class='bjson'>
 *   {"name":"Alice","age":30,"address":{"street":"123 Main St","city":"Boston"},"tags":["a","b","c"]}
 *   {"name":"Bob","age":25,"address":{"street":"456 Oak Ave","city":"Portland"},"tags":["d","e"]}
 * </p>
 *
 * <h5 class='section'>Limitations compared to JSON</h5>
 * <p>
 * <ul class='spaced-list'>
 *   <li><b>No pretty-printing</b>: {@link org.apache.juneau.serializer.WriterSerializer.Builder#useWhitespace() useWhitespace()}
 *       is always suppressed.  JSONL requires each JSON value to fit on a single line;
 *       applying intra-line pretty-printing would embed newlines and produce invalid JSONL.
 *       The setting is silently ignored regardless of its configured value.
 *   <li><b>Top-level semantics differ</b>: Collections and arrays are unwrapped — each element
 *       becomes one line.  A single non-collection object produces one JSONL line.
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 *   <li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 *   <li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JsonlBasics">JSONL Basics</a>
 * </ul>
 */
```

---

#### `JsonlParser` — Class-level Javadoc template

Follows the `JsonParser` structure:

```
/**
 * Parses JSONL (JSON Lines) input into POJO models.
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Content-Type</c> types:  <bc>application/jsonl, application/x-ndjson, text/jsonl</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * Parses JSONL input where each non-empty line is a complete JSON value.  Blank lines are ignored.
 * The conversion is as follows...
 * <ul class='spaced-list'>
 *   <li>JSON objects (<c>{...}</c>) are converted to {@link org.apache.juneau.collections.JsonMap JsonMaps}
 *       or Java beans if a target type is provided.
 *   <li>JSON arrays (<c>[...]</c>) are converted to {@link org.apache.juneau.collections.JsonList JsonLists}.
 *   <li>JSON strings are converted to {@link java.lang.String Strings}.
 *   <li>JSON numbers are converted to {@link java.lang.Integer Integers}, {@link java.lang.Long Longs},
 *       {@link java.lang.Float Floats}, or {@link java.lang.Double Doubles}.
 *   <li>JSON booleans are converted to {@link java.lang.Boolean Booleans}.
 *   <li>JSON nulls are converted to <jk>null</jk>.
 * </ul>
 * <p>
 * When the target type is a {@link java.util.Collection Collection} or array, each line is parsed
 * as one element.  When the target type is a single value, only the first non-empty line is parsed.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 *   <jc>// Use the default parser to parse JSONL into a list of POJOs</jc>
 *   List&lt;MyBean&gt; <jv>list</jv> = JsonlParser.<jsf>DEFAULT</jsf>.parse(<jv>jsonlInput</jv>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
 *
 *   <jc>// Create a custom parser</jc>
 *   JsonlParser <jv>parser</jv> = JsonlParser.<jsm>create</jsm>().build();
 *
 *   <jc>// Clone an existing parser</jc>
 *   <jv>parser</jv> = JsonlParser.<jsf>DEFAULT</jsf>.copy().build();
 * </p>
 *
 * <h5 class='figure'>Example input:</h5>
 * <p class='bjson'>
 *   {"name":"Alice","age":30}
 *   {"name":"Bob","age":25}
 *   {"name":"Carol","age":35}
 * </p>
 *
 * <h5 class='section'>Limitations compared to JSON</h5>
 * <p>
 * <ul class='spaced-list'>
 *   <li><b>No multi-line JSON input</b>: Each JSON value must be on a single line.
 *       Multi-line (pretty-printed) JSON piped as JSONL input will cause parse errors.
 *   <li><b>First-line-only for non-collection targets</b>: When parsing JSONL to a single
 *       non-collection type, only the first non-empty line is parsed; trailing lines are
 *       silently ignored.
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 *   <li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 *   <li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JsonlBasics">JSONL Basics</a>
 * </ul>
 */
```

---

#### `Jsonl` (Marshaller) — Class-level Javadoc template

Follows the `Json5.java` marshaller structure:

```
/**
 * Pairs {@link JsonlSerializer} and {@link JsonlParser} into a single class with convenience
 * read/write methods.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 *   <jc>// Serialize to JSONL using instance</jc>
 *   Jsonl <jv>jsonl</jv> = <jk>new</jk> Jsonl();
 *   String <jv>out</jv> = <jv>jsonl</jv>.write(<jv>myList</jv>);
 *   List&lt;MyBean&gt; <jv>in</jv> = <jv>jsonl</jv>.read(<jv>out</jv>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
 *
 *   <jc>// Serialize to JSONL using DEFAULT instance</jc>
 *   String <jv>out</jv> = Jsonl.<jsm>of</jsm>(<jv>myList</jv>);
 *   List&lt;MyBean&gt; <jv>in</jv> = Jsonl.<jsm>to</jsm>(<jv>out</jv>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
 * </p>
 *
 * <h5 class='figure'>Example output (List of beans):</h5>
 * <p class='bjson'>
 *   {"name":"Alice","age":30}
 *   {"name":"Bob","age":25}
 *   {"name":"Carol","age":35}
 * </p>
 *
 * <h5 class='figure'>Complex (bean with nested object and array):</h5>
 * <p class='bjson'>
 *   {"name":"Alice","age":30,"address":{"street":"123 Main St","city":"Boston"},"tags":["a","b","c"]}
 *   {"name":"Bob","age":25,"address":{"street":"456 Oak Ave","city":"Portland"},"tags":["d","e"]}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 *   <li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshallers">Marshallers</a>
 * </ul>
 */
```

`of()` and `to()` static method javadoc follows the rigid `Json5.java` template:
- One-sentence summary naming the format and direction (e.g., "Serializes a Java object to a JSONL string.")
- `<p>A shortcut for calling <c><jsf>DEFAULT</jsf>.write(<jv>object</jv>)</c>.</p>`
- `@param` for each argument — with a `<ul>` listing all accepted subtypes for polymorphic `Object input` / `Object output` params
- `@return` on its own indented line
- `@throws SerializeException` / `@throws ParseException` / `@throws IOException` as applicable
- `@see BeanSession#getClassMeta` only on the `Type...args` overload

---

### 2. Package Javadoc (`package-info.java`)

Explain:
- What JSONL is and when to use it
- Relationship to JSON serializer/parser
- AI/ML use cases (fine-tuning datasets, streaming inference, log aggregation)
- Code examples showing basic usage

### 3. Update Release Notes
/Users/james.bognar/git/apache/juneau/docs/pages/release-notes

### 4. Update Documentation

Find how existing languages are documented in the following location and update the documentation to match
the same level of detail.
/Users/james.bognar/git/apache/juneau/docs/pages/topics

---

## Implementation Order

1. `JsonlSerializer.java` and `JsonlSerializerSession.java` -- Core serialization
2. `JsonlParser.java` and `JsonlParserSession.java` -- Core parsing
3. `Jsonl.java` (marshaller) -- Convenience API
4. `package-info.java` -- Package documentation
5. Serializer tests (`Jsonl_Test.java`)
6. Parser tests (`JsonlParser_Test.java`)
7. Round-trip tests (`JsonlRoundTrip_Test.java`)
8. Marshaller tests (marshaller `Jsonl_Test.java`)
9. Media type tests (`JsonlMediaType_Test.java`)
10. Edge case tests (`JsonlEdgeCases_Test.java`)
11. REST integration (`BasicUniversalConfig`, `BasicJsonlConfig`, `RestClient`)
12. Context registration (`JsonlConfig`, `JsonlConfigAnnotation`, `Context.java`)
13. Final documentation review and Javadoc polish

---

## Iterator/Iterable/Stream Support

As of 9.2.1, Juneau natively supports serialization of `Iterator`, `Iterable` (non-Collection), `Enumeration`, and `java.util.stream.Stream` types.

JSONL extends `JsonSerializer` but overrides `doSerialize()` to write one object per line. The top-level collection is consumed in `doSerialize()`, so streamable handling must be added there rather than relying on the base class `serializeAnything()`.

In `doSerialize()`, add a branch for streamable types alongside the existing `isCollection()/isArray()` checks:

```java
} else if (cm != null && cm.isStreamable()) {
    ClassMeta<?> elementType = cm.getElementType();
    forEachStreamableEntry(o, cm, item -> {
        serializeAnything(w.i(getInitialDepth()), item, elementType, "root", null);
        w.w('\n');
    });
}
```

This uses `forEachStreamableEntry()` from `SerializerSession` for lazy iteration, writing each element as a separate JSON line without materializing the entire collection into memory first.

---

## Key Design Decisions

1. **Extend, don't duplicate**: JSONL classes extend JSON classes rather than copying code. All JSON serialization/parsing logic is inherited; only the collection-level behavior changes.

2. **No separate Builder**: Following `Json5Serializer` precedent, `JsonlSerializer` reuses `JsonSerializer.Builder`. No JSONL-specific configuration properties are needed.

3. **Package location**: New `org.apache.juneau.jsonl` package (not inside `org.apache.juneau.json`) because JSONL has its own media type and semantics distinct from JSON. This follows the pattern where each format has its own package.

4. **Whitespace handling**: `useWhitespace()` is **always suppressed** in `JsonlSerializerSession`. JSONL requires each JSON value to fit on a single line; applying intra-line pretty-printing would embed newlines and produce invalid JSONL. The setting is silently ignored regardless of its configured value. In `doSerialize()`, the writer is always used in compact mode. This is the primary behavioral difference from `JsonSerializer`.

5. **Single object fallback**: When serializing/parsing a non-collection object, JSONL behaves identically to JSON (single line of JSON). This makes JSONL a superset of JSON for single-object use cases.

6. **Line reading strategy for parser**: The parser reads input line by line rather than using the character-level JSON parser state machine for line splitting. This is correct per the JSONL spec which mandates `\n` as the line separator.

7. **Date/time and Duration support**: Date/time types (Date, Calendar, java.time Temporal subtypes like Instant, ZonedDateTime, LocalDate, etc.) and java.time.Duration are inherited from the JSON serializer dispatch since `JsonlSerializerSession` delegates to `JsonSerializerSession.serializeAnything()`. They are serialized as ISO 8601 strings, one JSON value per line. Parsing uses `Iso8601Utils.parse()` to convert ISO 8601 strings back to the target types.
