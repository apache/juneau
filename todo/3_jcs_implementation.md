# JCS (JSON Canonicalization Scheme) Support Implementation Plan for Apache Juneau

## Overview

This plan covers implementing full marshalling support for JCS (JSON Canonicalization Scheme), defined by RFC 8785. JCS produces a deterministic, byte-for-byte canonical representation of JSON data, enabling reliable cryptographic operations (hashing, signing) on JSON without converting to an opaque format like base64.

The `Jcs` marshaller pairs `JcsSerializer` with the standard `JsonParser`, enabling full round-trip serialization and deserialization at the same level as JSON. No separate `JcsParser` class is needed because JCS output is valid JSON and can be parsed directly by `JsonParser`. The implementation extends `JsonSerializer` to enforce the strict canonicalization rules.

## Specification

- **RFC**: https://www.rfc-editor.org/rfc/rfc8785
- **Media type**: `application/json` (canonical JSON is standard JSON)
- **Encoding**: UTF-8
- **I-JSON**: RFC 7493 (required subset)

---

## JCS Canonicalization Rules

### 1. No Whitespace

No whitespace between tokens. Output is a single contiguous line.

```
{"age":42,"name":"John"}
```

Not:
```
{ "age": 42, "name": "John" }
```

### 2. Sorted Object Properties

Properties are sorted by their **raw** (unescaped) Unicode values, using UTF-16 code unit comparison (matching ECMAScript's `Array.prototype.sort()`). Sorting is applied recursively to all nested objects. Array element order is preserved.

```json
{"a":1,"b":2,"c":3}
```

Not:
```json
{"c":3,"a":1,"b":2}
```

Sort order for non-ASCII keys follows UTF-16 code unit ordering:
```
U+000D (Carriage Return) < U+0031 ("1") < U+0080 (Control) < U+00F6 ("ö") < U+20AC ("€") < U+D83D+DE00 (Emoji) < U+FB33 (Hebrew)
```

### 3. ECMAScript Number Serialization

Numbers are serialized according to ECMAScript's `JSON.stringify()` rules (Section 7.1.12.1 of ECMA-262):

- No leading zeros: `0.5` not `0.50`
- No trailing zeros: `4.5` not `4.50`
- No unnecessary plus sign in exponent: `1e+30` (but this is ECMAScript's output)
- Integer values: no decimal point (`42` not `42.0`)
- Negative zero: serialized as `0` (not `-0`)
- Shortest representation that round-trips correctly
- NaN and Infinity are **errors** (not permitted)

Examples from the RFC:

| IEEE 754 Double | JCS Output |
|----------------|-----------|
| 0.0 | `0` |
| -0.0 | `0` |
| 5e-324 | `5e-324` |
| 1.7976931348623157e+308 | `1.7976931348623157e+308` |
| 333333333.33333329 | `333333333.3333333` |
| 1E30 | `1e+30` |
| 4.50 | `4.5` |
| 2e-3 | `0.002` |
| 0.000000000000000000000000001 | `1e-27` |

### 4. ECMAScript String Serialization

Strings follow ECMAScript's `JSON.stringify()`:

- Always double-quoted
- Mandatory escapes: `\\` and `\"`
- Control characters U+0000-U+001F: use `\b`, `\t`, `\n`, `\f`, `\r` for predefined escapes; `\uHHHH` (lowercase hex) for others
- All characters outside the control range are serialized as-is (no unnecessary `\uHHHH` escaping)
- Lone surrogates (e.g., U+DEAD) cause an error

### 5. Literal Serialization

`null`, `true`, `false` serialized exactly as-is (already deterministic).

### 6. UTF-8 Output

Output MUST be encoded in UTF-8.

---

## Architecture

JCS is a variant of JSON serialization, so it extends `JsonSerializer` -- similar to how `Json5Serializer` extends `JsonSerializer`.

```
JsonSerializer
  └── JcsSerializer
        └── JcsSerializerSession
              └── JcsWriter (extends JsonWriter)

Jcs (marshaller)
  ├── JcsSerializer  (serialization)
  └── JsonParser     (parsing — JCS output is valid JSON)
```

The `Jcs` marshaller pairs `JcsSerializer` with the existing `JsonParser` for full round-trip support. No separate `JcsParser` class is needed.

---

## Limitations

The following spec-level constraints must be **documented in the Javadoc** of `JcsSerializer` and `Jcs`, and must be accounted for in round-trip tests (e.g., via `skipIf` predicates where the values cannot be round-tripped):

| Limitation | Detail |
|---|---|
| **`BigDecimal` / `BigInteger` precision** | RFC 8785 requires all numbers to fit in IEEE 754 double precision. `BigDecimal` values with more significant digits than a `double` can hold will either lose precision silently or throw a `SerializeException`. This is a spec-level constraint, not an implementation gap. |
| **`NaN` and `Infinity`** | `Double.NaN`, `Double.POSITIVE_INFINITY`, and `Double.NEGATIVE_INFINITY` are rejected per RFC 8785 and will throw a `SerializeException`. |
| **Lone surrogates** | Strings containing lone UTF-16 surrogate code units (e.g., U+DEAD without a paired surrogate) are rejected per the spec and will throw a `SerializeException`. |
| **Media type** | JCS does not define its own media type. Output is `application/json`. Do not register `JcsSerializer` as a content-negotiation alternative to `JsonSerializer`; use it explicitly by class reference. |

### Round-Trip Compatibility

For all other data structures supported by JSON, JCS provides full round-trip support via `JsonParser`. This includes:
- Primitives and their wrappers (`String`, `int`, `long`, `double`, `boolean`, etc.)
- Arrays (1D, 2D, 3D)
- Collections (`List`, `Set`, `Queue`, etc.)
- Maps (`Map<K,V>`, including `TreeMap`, `LinkedHashMap`, etc.)
- Beans and records
- Enums
- Date/time types (`Date`, `Calendar`, `Instant`, `ZonedDateTime`, `LocalDate`, etc.)
- `Optional<T>`
- `Iterator`, `Iterable`, `Stream` (serialization only — consumed once)

---

## Files to Create

All source files in `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/json/`.

### 1. `JcsSerializer.java`

Extends `JsonSerializer`.

```java
package org.apache.juneau.json;

public class JcsSerializer extends JsonSerializer {

    public static final JcsSerializer DEFAULT = ...;

    public static class Builder extends JsonSerializer.Builder {
        protected Builder() {
            super();
            // Override parent defaults for canonical output
            sortProperties();     // Sort bean properties alphabetically
            quoteChar('"');       // Always double quotes
            simpleAttrs(false);   // Never unquoted attributes
            ws(false);            // No whitespace
        }
    }

    @Override
    public JcsSerializerSession.Builder createSession() {
        return JcsSerializerSession.create(this);
    }
}
```

Key: The builder pre-configures `sortProperties()` and forces strict JSON output (no JSON5 relaxations). The heavy lifting is in the session.

### 2. `JcsSerializerSession.java`

Extends `JsonSerializerSession`. Overrides key serialization behaviors.

```java
package org.apache.juneau.json;

public class JcsSerializerSession extends JsonSerializerSession {

    @Override
    protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
        // Same as parent but with canonical writer
        serializeAnything(getJcsWriter(out), o, getExpectedRootType(o), "root", null);
    }

    // Override map serialization to sort keys by UTF-16 code unit comparison
    @Override
    protected void serializeMap(JsonWriter out, Map m, ClassMeta type)
            throws IOException, SerializeException {
        // Sort map keys using JCS UTF-16 code unit comparison
        List<Map.Entry> sorted = new ArrayList<>(m.entrySet());
        sorted.sort((a, b) -> jcsCompare(toString(a.getKey()), toString(b.getKey())));
        // Serialize in sorted order
        out.append('{');
        boolean first = true;
        for (Map.Entry e : sorted) {
            if (!first) out.append(',');
            first = false;
            serializeString(out, toString(e.getKey()));
            out.append(':');
            serializeAnything(out, e.getValue(), ...);
        }
        out.append('}');
    }

    // Override bean serialization to sort properties by UTF-16 code unit comparison
    @Override
    protected void serializeBeanMap(JsonWriter out, BeanMap m, String typeName)
            throws IOException, SerializeException {
        // Collect all properties, sort by name using JCS comparison, serialize
    }

    // Override number serialization for ECMAScript compliance
    @Override
    protected void serializeNumber(JsonWriter out, Number n)
            throws IOException, SerializeException {
        // Implement ECMAScript-compatible number serialization
        // - No trailing zeros
        // - Shortest representation that round-trips
        // - Negative zero → "0"
        // - NaN/Infinity → throw error
    }

    // JCS key comparison: UTF-16 code unit ordering
    static int jcsCompare(String a, String b) {
        // Compare as arrays of UTF-16 code units (unsigned)
        int len = Math.min(a.length(), b.length());
        for (int i = 0; i < len; i++) {
            int diff = Character.compare(a.charAt(i), b.charAt(i));
            if (diff != 0) return diff;
        }
        return Integer.compare(a.length(), b.length());
    }
}
```

Core implementation areas:

**a) Property/Key Sorting:**
Juneau already has `sortProperties()` which sorts bean properties alphabetically. For JCS, maps also need their keys sorted, and the sort must use UTF-16 code unit comparison (not locale-sensitive). Java's `String.compareTo()` already compares by UTF-16 code units, so `jcsCompare` can delegate to it for most cases. The key difference from the existing `sortProperties` is that JCS also requires sorting of **map keys** in `serializeMap`, which the parent `JsonSerializerSession` does not do by default.

**b) ECMAScript Number Serialization:**
This is the most complex part. Java's `Double.toString()` does NOT produce ECMAScript-compatible output. For example:
- Java: `4.5E-7` → JCS requires: `4.5e-7` (lowercase `e`)
- Java: `333333333.33333330` → JCS requires: `333333333.3333333` (no trailing zero)
- Java: `-0.0` → JCS requires: `0`

The implementation needs an ECMAScript-compatible number-to-string algorithm. Options:
1. Adapt the Ryu algorithm (used by the JCS reference implementation) -- pure Java, no dependencies
2. Use `Double.toString()` and post-process to match ECMAScript rules

Post-processing `Double.toString()` is simpler and sufficient:
- Remove trailing zeros after decimal point
- Remove decimal point if no fraction remains
- Lowercase `E` to `e`
- Add `+` after `e` for positive exponents (ECMAScript does this)
- Convert `-0.0` to `0`
- Reject NaN and Infinity

**c) String Serialization:**
The existing JSON serializer already handles most string escaping. JCS-specific requirements:
- Control characters U+0000-U+001F must use lowercase hex in `\uHHHH` (the existing serializer should be verified)
- No unnecessary escaping of non-ASCII characters (they should be output as-is in UTF-8)
- Lone surrogates must cause an error

### 3. `Jcs.java` (Marshaller)

Location: `src/main/java/org/apache/juneau/marshaller/Jcs.java`

Extends `CharMarshaller`. Uses `JcsSerializer` for serialization and `JsonParser` for parsing (canonical JSON is valid JSON).

```java
package org.apache.juneau.marshaller;

public class Jcs extends CharMarshaller {
    public static final Jcs DEFAULT = new Jcs();

    public static String of(Object object) throws SerializeException { ... }
    public static <T> T to(String input, Class<T> type) throws ParseException { ... }

    public Jcs() { this(JcsSerializer.DEFAULT, JsonParser.DEFAULT); }
    public Jcs(JcsSerializer s, JsonParser p) { super(s, p); }
}
```

### 4. `package-info.java` updates

Add JCS documentation to the existing `org.apache.juneau.json` package Javadoc.

### 5. `annotation/JcsConfig.java`

Annotation for specifying config properties for REST classes and methods. Empty initially (rank only), following the `MarkdownConfig`/`TomlConfig` pattern. JCS uses fixed canonical output, so no format-specific settings; the annotation provides consistency and future extensibility.

```java
@Target({ TYPE, METHOD })
@Retention(RUNTIME)
@Inherited
@ContextApply(JcsConfigAnnotation.SerializerApply.class)
public @interface JcsConfig {
    int rank() default 0;
}
```

Note: JcsConfig only has SerializerApply (no ParserApply) because JCS uses `JsonParser` for parsing, not a separate `JcsParser`.

### 6. `annotation/JcsConfigAnnotation.java`

Utility class for the `@JcsConfig` annotation, with no-op `SerializerApply` applier.

---

## Files to Modify

### 1. `BasicUniversalConfig.java`

Add `JcsSerializer.class` to `serializers` (parser is already the standard JSON parser).

### 2. `RestClient.java`

Add `JcsSerializer.class` to the `universal()` method.

### 3. Context: `Context.java`

Path: `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/Context.java`

Add `import org.apache.juneau.json.annotation.*;` (if not already present) and add `{@link JcsConfig}` to the list of config annotations in the `applyAnnotations()` Javadoc (alphabetically between JsonSchemaConfig and MarkdownConfig). Note: JcsConfig is in the `org.apache.juneau.json.annotation` package since JCS lives under the json package.

---

## Test Plan

### 1. Number Serialization Tests: `org/apache/juneau/json/JcsNumbers_Test.java`

Tests against RFC 8785 Appendix B reference values.

- **a01_zero** -- `0.0` → `0`
- **a02_negativeZero** -- `-0.0` → `0`
- **a03_minPosNumber** -- `5e-324`
- **a04_minNegNumber** -- `-5e-324`
- **a05_maxPosNumber** -- `1.7976931348623157e+308`
- **a06_maxNegNumber** -- `-1.7976931348623157e+308`
- **a07_maxPosInt** -- `9007199254740992`
- **a08_maxNegInt** -- `-9007199254740992`
- **a09_largeInteger** -- `295147905179352830000`
- **a10_noTrailingZeros** -- `4.50` → `4.5`
- **a11_scientificNotation** -- `1E30` → `1e+30`
- **a12_smallDecimal** -- `2e-3` → `0.002`
- **a13_verySmallDecimal** -- `0.000000000000000000000000001` → `1e-27`
- **a14_rejectNaN** -- `Double.NaN` throws error
- **a15_rejectInfinity** -- `Double.POSITIVE_INFINITY` throws error
- **a16_rejectNegInfinity** -- `Double.NEGATIVE_INFINITY` throws error
- **a17_integerNoDecimalPoint** -- `42.0` → `42`
- **a18_roundToEven** -- `1424953923781206.2` (matches IEEE 754 round-to-even)
- **a19_edgeCasePrecision** -- `333333333.3333333` (exact match)
- **a20_floatValues** -- Float values promoted to double and serialized correctly

### 2. String Serialization Tests: `org/apache/juneau/json/JcsStrings_Test.java`

- **b01_simpleString** -- `"hello"` → `"hello"`
- **b02_escapeBackslash** -- `\` → `"\\"`
- **b03_escapeQuote** -- `"` → `"\""`
- **b04_controlChars** -- `\b`, `\t`, `\n`, `\f`, `\r` use named escapes
- **b05_otherControlChars** -- U+0000-U+001F (non-predefined) use lowercase `\u00XX`
- **b06_nonAsciiLiteral** -- `€` (U+20AC) output as literal UTF-8 bytes, not `\u20ac`
- **b07_rfcStringExample** -- `"€$\u000f\nA'B\"\\\\\"/"`matches RFC example
- **b08_emojiLiteral** -- Emoji output as literal UTF-8 (surrogate pairs preserved)
- **b09_loneSurrogateError** -- Lone surrogate (U+DEAD) causes error
- **b10_emptyString** -- `""` → `""`

### 3. Property Sorting Tests: `org/apache/juneau/json/JcsSorting_Test.java`

- **c01_alphabeticalOrder** -- `{"a":1,"b":2,"c":3}` sorted
- **c02_reverseInputOrder** -- `{"c":3,"b":2,"a":1}` → `{"a":1,"b":2,"c":3}`
- **c03_nestedObjectsSorted** -- Inner objects also sorted recursively
- **c04_arrayOrderPreserved** -- Array elements NOT reordered
- **c05_utf16SortOrder** -- RFC sort test: `\r` < `1` < `\u0080` < `ö` < `€` < emoji < Hebrew
- **c06_mapKeysSorted** -- `Map<String,Object>` keys sorted by UTF-16 code units
- **c07_emptyObject** -- `{}` → `{}`
- **c08_singleProperty** -- `{"a":1}` → `{"a":1}`
- **c09_numericStringKeys** -- `"1"` < `"10"` < `"2"` (lexicographic, not numeric)
- **c10_caseSensitive** -- `"A"` < `"a"` (uppercase before lowercase in UTF-16)

### 4. Full Canonicalization Tests: `org/apache/juneau/json/JcsCanonical_Test.java`

End-to-end tests matching RFC 8785 examples.

- **d01_rfcExample1** -- RFC Section 3.2.2 full example (numbers + string + literals)
- **d02_rfcExample2** -- RFC Section 3.2.3 sorted version
- **d03_rfcByteOutput** -- RFC Section 3.2.4 exact UTF-8 byte sequence
- **d04_noWhitespace** -- No spaces, tabs, newlines in output
- **d05_simpleBeanCanonical** -- Bean with multiple properties, deterministic output
- **d06_nestedBeanCanonical** -- Nested beans, all levels sorted
- **d07_mixedTypesCanonical** -- Bean with strings, numbers, booleans, nulls, arrays, nested objects
- **d08_deterministicRoundTrip** -- Serialize same bean twice, byte-identical output
- **d09_hashStability** -- SHA-256 of output is identical across multiple serializations
- **d10_rfcSortingTestData** -- RFC sorting test with Unicode keys (exact match)

### 5. Marshaller Tests: `org/apache/juneau/marshaller/Jcs_Test.java`

- **e01_of** -- `Jcs.of(bean)` produces canonical JSON string
- **e02_to** -- `Jcs.to(input, Type)` parses using standard JSON parser
- **e03_roundTrip** -- Serialize + parse round-trip
- **e04_defaultInstance** -- `Jcs.DEFAULT` works correctly

### 6. Edge Case Tests: `org/apache/juneau/json/JcsEdgeCases_Test.java`

- **f01_emptyBean** -- `{}`
- **f02_nullRoot** -- `null`
- **f03_booleanRoot** -- `true` / `false`
- **f04_numberRoot** -- `42`
- **f05_stringRoot** -- `"hello"`
- **f06_arrayRoot** -- `[1,2,3]`
- **f07_deepNesting** -- 10+ levels, all sorted
- **f08_duplicateMapKeys** -- Last value wins (JSON semantics), sorted output
- **f09_largeObject** -- 100+ properties, correctly sorted
- **f10_emptyStrings** -- Empty string keys and values handled

---

### 7. Cross-Format Round-Trip Integration

JCS output is valid JSON, so `JsonParser` can parse it. Use **full round-trip entries** (no `.returnOriginalObject()`) in all TESTERS arrays. The entry pattern is:

```java
tester(N, "Jcs - default")
    .serializer(JcsSerializer.create().keepNullProperties().addBeanTypes().addRootType())
    .parser(JsonParser.create())
    .build(),
```

Add this entry to **all** of the following TESTERS arrays (assign the next available index `N` in each file):

- `RoundTripTest_Base.java` — base class; all subclass tests inherit its TESTERS list
- `RoundTripDateTime_Test.java` — verifies date/time and Duration values serialize correctly in JCS output
- Any other files with their own private `TESTERS` arrays (e.g., `RoundTripMaps_Test`, `RoundTripBeanMaps_Test`)

**Note**: Certain types have spec-level constraints (see Limitations section). The following `skipIf` predicates may be needed for values that JCS cannot represent:

```java
tester(N, "Jcs - default")
    .serializer(JcsSerializer.create().keepNullProperties().addBeanTypes().addRootType())
    .parser(JsonParser.create())
    .skipIf(o -> o instanceof Double && (((Double)o).isNaN() || ((Double)o).isInfinite()))
    .build(),
```

## Documentation

### 1. Javadoc

Follow the exact structure of `JsonSerializer.java` for the serializer and `Json.java` for the marshaller.

**`JcsSerializer.java`** — full class-level Javadoc structure:

```
/**
 * Serializes POJO models to canonical JSON per RFC 8785 (JCS).
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * 	Produces media type: <bc>application/json</bc>
 * </p>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * 	JCS (JSON Canonicalization Scheme) produces a deterministic, byte-for-byte canonical
 * 	representation of JSON, enabling reliable cryptographic operations such as hashing and
 * 	digital signing. All canonicalization rules are defined in
 * 	<a class="doclink" href="https://www.rfc-editor.org/rfc/rfc8785">RFC 8785</a>.
 * </p>
 * <ul class='spaced-list'>
 * 	<li>No whitespace between tokens.
 * 	<li>Object properties sorted by UTF-16 code unit order, applied recursively to all nested objects.
 * 	<li>Numbers serialized using ECMAScript-compatible rules: shortest round-trip representation,
 * 	    no trailing zeros, lowercase {@code e}, positive exponent sign included (e.g. {@code 1e+30}).
 * 	<li>Negative zero serialized as {@code 0}.
 * 	<li>Non-ASCII characters emitted as literal UTF-8 bytes (no unnecessary {@code \uHHHH} escaping).
 * 	<li>Array element order is preserved (not sorted).
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a bean.</jc>
 * 	MyBean <jv>bean</jv> = <jk>new</jk> MyBean().name(<js>"Alice"</js>).age(30);
 *
 * 	<jc>// Serialize to canonical JSON.</jc>
 * 	String <jv>json</jv> = JcsSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>bean</jv>);
 *
 * 	<jc>// Or use the Jcs marshaller convenience method.</jc>
 * 	String <jv>json</jv> = Jcs.<jsm>of</jsm>(<jv>bean</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (bean with name/age):</h5>
 * <p class='bjson'>
 * 	{<jok>"age"</jok>:<jov>30</jov>,<jok>"name"</jok>:<jov>"Alice"</jov>}
 * </p>
 *
 * <h5 class='figure'>Example output with nested object:</h5>
 * <p class='bjson'>
 * 	{<jok>"address"</jok>:{<jok>"city"</jok>:<jov>"Denver"</jov>,<jok>"zip"</jok>:<jov>"80201"</jov>},<jok>"name"</jok>:<jov>"Alice"</jov>}
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * 	<li class='note'>
 * 		JCS output is valid JSON and can be parsed using the standard {@link JsonParser}.
 * 		The {@link Jcs} marshaller pairs this serializer with {@link JsonParser} for full round-trip support.
 * 	<li class='note'>
 * 		{@link java.math.BigDecimal} and {@link java.math.BigInteger} values beyond IEEE 754 double
 * 		precision range will lose precision or throw a {@link SerializeException}.
 * 		This is a spec-level constraint defined by RFC 8785.
 * 	<li class='note'>
 * 		{@link Double#NaN}, {@link Double#POSITIVE_INFINITY}, and {@link Double#NEGATIVE_INFINITY}
 * 		are not permitted and will throw a {@link SerializeException}.
 * 	<li class='note'>
 * 		Strings containing lone UTF-16 surrogate code units will throw a {@link SerializeException}.
 * 	<li class='note'>
 * 		JCS does not define its own media type. Output is {@code application/json}.
 * 		Do not register this serializer as a content-negotiation alternative to {@link JsonSerializer};
 * 		use it explicitly by class reference.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://www.rfc-editor.org/rfc/rfc8785">RFC 8785 — JSON Canonicalization Scheme</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Jcs">JCS topic</a>
 * 	<li class='jc'>{@link Jcs}
 * 	<li class='jc'>{@link JsonParser}
 * </ul>
 */
```

**`Jcs.java`** (marshaller) — full class-level Javadoc structure:

```
/**
 * A pairing of a {@link JcsSerializer} and {@link JsonParser} into a single class with
 * convenience read/write methods.
 *
 * <p>
 * 	Produces canonical JSON per <a class="doclink" href="https://www.rfc-editor.org/rfc/rfc8785">RFC 8785</a>.
 * 	Parsing uses the standard {@link JsonParser} since JCS output is valid JSON.
 * </p>
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Using static convenience methods.</jc>
 * 	String <jv>s</jv> = Jcs.<jsm>of</jsm>(<jv>myBean</jv>);
 * 	MyBean <jv>b</jv> = Jcs.<jsm>to</jsm>(<jv>s</jv>, MyBean.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Using instance.</jc>
 * 	Jcs <jv>jcs</jv> = <jk>new</jk> Jcs();
 * 	String <jv>s</jv> = <jv>jcs</jv>.write(<jv>myBean</jv>);
 * 	MyBean <jv>b</jv> = <jv>jcs</jv>.read(<jv>s</jv>, MyBean.<jk>class</jk>);
 * </p>
 *
 * <h5 class='figure'>Example output (bean with name/age):</h5>
 * <p class='bjson'>
 * 	{<jok>"age"</jok>:<jov>30</jov>,<jok>"name"</jok>:<jov>"Alice"</jov>}
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		{@link java.math.BigDecimal} and {@link java.math.BigInteger} values beyond IEEE 754 double
 * 		precision range will lose precision or throw during serialization.
 * 	<li class='note'>
 * 		{@link Double#NaN}, {@link Double#POSITIVE_INFINITY}, and {@link Double#NEGATIVE_INFINITY}
 * 		are not permitted and will throw during serialization.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://www.rfc-editor.org/rfc/rfc8785">RFC 8785 — JSON Canonicalization Scheme</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Jcs">JCS topic</a>
 * 	<li class='jc'>{@link JcsSerializer}
 * 	<li class='jc'>{@link JsonParser}
 * </ul>
 */
```

**No separate `JcsParser` class** — parsing is handled by the existing `{@link JsonParser}`.

### 2. Package Javadoc

Add JCS section to existing `org.apache.juneau.json` package documentation.

### 3. Update Release Notes
/Users/james.bognar/git/apache/juneau/docs/pages/release-notes

### 4. Update Documentation

Find how existing languages are documented in the following location and update the documentation to match
the same level of detail.
/Users/james.bognar/git/apache/juneau/docs/pages/topics

---

## Implementation Order

1. **ECMAScript number serialization** -- Implement and unit test the number formatting algorithm
2. **`JcsSerializer.java`** and **`JcsSerializerSession.java`** -- Extend JsonSerializer with sorting and canonical output
3. **Number tests** (`JcsNumbers_Test.java`) -- 20 test cases against RFC reference values
4. **String tests** (`JcsStrings_Test.java`) -- 10 test cases
5. **Sorting tests** (`JcsSorting_Test.java`) -- 10 test cases
6. **Canonical tests** (`JcsCanonical_Test.java`) -- 10 test cases with RFC byte-level verification
7. **`Jcs.java`** marshaller
8. **Marshaller and edge case tests** -- 14 test cases
9. **REST integration** (`BasicUniversalConfig`, `RestClient`)
10. **Context registration** (`JcsConfig`, `JcsConfigAnnotation`, `Context.java`)
11. **Documentation**

---

## Iterator/Iterable/Stream Support

As of 9.2.1, Juneau natively supports serialization of `Iterator`, `Iterable` (non-Collection), `Enumeration`, and `java.util.stream.Stream` types.

This format **inherits** Iterator/Iterable/Stream support from its parent `JsonSerializerSession`. The `serializeStreamable()` method in the base class handles lazy streaming of these types. Verify that any overrides of `serializeAnything()` in this serializer session do not bypass the base class handling of `isStreamable()` types.

---

## Key Design Decisions

### 1. Full round-trip via JsonParser (no dedicated parser)

JCS output is valid JSON. The `Jcs` marshaller pairs `JcsSerializer` with the standard `JsonParser`, enabling full round-trip serialization and deserialization at the same level as JSON. No separate `JcsParser` class is needed or created.

### 2. Extends JsonSerializer

JCS is a strict subset of JSON output. It builds directly on `JsonSerializer`, overriding only the methods that need canonical behavior: property sorting, number formatting, and string escaping. This minimizes code duplication.

### 3. UTF-16 code unit sorting

JCS mandates UTF-16 code unit comparison for property sorting. Java's `String.compareTo()` already performs this comparison (Java strings are UTF-16 internally), so no conversion is needed.

### 4. ECMAScript number formatting via post-processing

Rather than implementing the full Ryu algorithm, the implementation post-processes `Double.toString()` output to match ECMAScript's `JSON.stringify()`. This is simpler and sufficient because:
- Java's `Double.toString()` already produces a round-trip-safe representation
- The post-processing rules are straightforward (lowercase `e`, remove trailing zeros, handle `-0.0`)

### 5. No separate media type

JCS output is standard `application/json`. It does not define its own media type. The serializer produces `application/json` with `+jcs` as an optional qualifier.

### 6. Pre-configured sortProperties()

The builder calls `sortProperties()` in the constructor, ensuring bean properties are sorted. Additionally, `serializeMap()` is overridden to sort map keys, which `sortProperties()` alone does not cover.

### 7. Date/Time and Duration Support

Date/time types (`Date`, `Calendar`, `Temporal` subtypes) and `Duration` are built-in first-class types. JCS extends `JsonSerializer`, so it inherits the `isDateOrCalendarOrTemporal()` and `isDuration()` dispatch checks from `JsonSerializerSession`. These types are formatted as ISO 8601 strings; the deterministic property ordering applies to the string representation. See `builtin-datetime-iso8601.md` for the full architecture.

---

## Scope Exclusions

- **Dedicated JcsParser class** -- No separate parser is needed; JCS output is valid JSON and `JsonParser` handles it directly
- **Signature verification** -- JCS defines the canonical form; signature schemes are application-level concerns
- **Unicode normalization** -- RFC 8785 explicitly excludes Unicode normalization
- **Extended precision numbers** -- Numbers must fit in IEEE 754 double precision per the spec; `BigDecimal`/`BigInteger` values beyond this range are not supported
