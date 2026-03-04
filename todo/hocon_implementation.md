# HOCON (Human-Optimized Config Object Notation) Support Implementation Plan for Apache Juneau

## Overview

This plan covers implementing serialization and parsing support for HOCON in Juneau. HOCON is a superset of JSON used extensively in the JVM ecosystem (Akka, Play Framework, sbt, Apache Spark, Kafka, Flink) for configuration files. It adds significant features beyond JSON and JSON5: path expressions as keys (`a.b.c = value`), `=` as a key-value separator, unquoted strings, value concatenation, object merging, substitution variables (`${var}`), `include` directives, multi-line strings (`"""`), duration/size units, and optional root braces.

HOCON is the most complex JSON variant in this series. The implementation covers the core language features needed for lossless bean serialization, with some advanced features (includes, substitutions) supported in parsing only.

## Specification

- **Spec**: https://github.com/lightbend/config/blob/main/HOCON.md
- **Reference Implementation**: https://github.com/lightbend/config (Typesafe Config)
- **Media type**: `application/hocon`
- **File extension**: `.conf`
- **Encoding**: UTF-8, UTF-16, or UTF-32

---

## HOCON Syntax Summary

### Example

```hocon
# Database configuration
database {
  host = localhost
  port = 5432
  name = myapp

  connection-pool {
    max-size = 20
    min-idle = 5
    timeout = 30s
  }
}

server {
  host = "0.0.0.0"
  port = 8080
  ssl {
    enabled = true
    cert = /path/to/cert.pem
  }
}

tags = [web, api, rest]

description = """
  This is a multi-line
  string value.
  """
```

### Feature Summary

**1. Key-Value Separators:**
- `=` (most common in HOCON), `:` (JSON-style), or whitespace (for object values)
- `key = value`, `key: value`, `key { ... }` are all valid

**2. Unquoted Strings:**
- Keys and values can be unquoted
- Unquoted strings cannot contain `$"{}[]:=,+#^\/?!@*&` or whitespace, except as part of value concatenation
- Unquoted strings end before comments and certain punctuation

**3. Path Expressions:**
- Dot-separated keys create nested objects: `a.b.c = 10` is equivalent to `a { b { c = 10 } }`
- Path components can be quoted: `"a.b".c = 10` (key with literal dot)

**4. Object Merging:**
- Duplicate keys merge objects instead of replacing:
  ```hocon
  a { x = 1 }
  a { y = 2 }
  # Result: a { x = 1, y = 2 }
  ```
- `+=` appends to arrays: `list += newItem`

**5. Substitutions (`${var}`):**
- Reference other config values: `full-name = ${first-name} ${last-name}`
- `${?var}` is a "soft" substitution (ignored if var is not defined)
- Substitutions are resolved after the entire file is parsed

**6. Multi-line Strings:**
- Triple-quoted: `"""multi\nline"""` (no escape processing inside)

**7. Comments:**
- `//` and `#` single-line comments

**8. Value Concatenation:**
- Adjacent values are concatenated: `path = /usr/local "/bin"` → `"/usr/local/bin"`
- Array concatenation: `list = [1, 2] [3, 4]` → `[1, 2, 3, 4]`
- Object concatenation (merging): `obj = {a:1} {b:2}` → `{a:1, b:2}`

**9. Optional Root Braces:**
- Top-level `{}` can be omitted (like Hjson)

**10. Commas and Newlines:**
- Commas are optional; newlines work as separators
- Trailing commas allowed

**11. Duration and Size Units:**
- `10s`, `5m`, `2h`, `100ms` (durations)
- `512K`, `10M`, `2G` (sizes)
- These are strings in the format; application-level interpretation

---

## Bean-to-HOCON Mapping

### Serialization Rules

Juneau beans are serialized in a clean, HOCON-idiomatic style:

```java
class AppConfig {
    String name;
    int port;
    boolean debug;
    DatabaseConfig database;
    List<String> tags;
}
class DatabaseConfig {
    String host;
    int port;
    String connectionString;
}
```

Serialized:
```hocon
name = myapp
port = 8080
debug = true
database {
  host = localhost
  port = 5432
  connection-string = "jdbc:postgresql://localhost/myapp"
}
tags = [web, api, rest]
```

Key serialization decisions:
- **`=` separator** for scalar values (HOCON convention)
- **No separator** (just space/brace) for object values (`database { ... }`)
- **Unquoted strings** for simple values (no special chars)
- **Quoted strings** for values containing special characters
- **Root braces omitted** by default (HOCON config file convention)
- **No path expression flattening** in serializer (beans are written as nested objects)

### Type Mapping

| Java Type | HOCON Output | Example |
|-----------|-------------|---------|
| `String` (simple) | Unquoted string | `name = myapp` |
| `String` (special chars) | Quoted string | `path = "/usr/local/bin"` |
| `String` (multi-line) | Triple-quoted | `desc = """...\n..."""` |
| `int`, `long` | Number | `port = 8080` |
| `float`, `double` | Number | `ratio = 3.14` |
| `boolean` | Boolean | `debug = true` |
| `null` | `null` | `value = null` |
| `Enum` | Unquoted string | `level = INFO` |
| Nested bean | Nested object | `db { host = localhost }` |
| `List<simple>` | Array | `tags = [a, b, c]` |
| `List<bean>` | Array of objects | `servers = [{ host = a }, ...]` |
| `Map<String,V>` | Object | `env { KEY = value }` |

---

## Round-Trip Fidelity vs JSON

HOCON is a superset of JSON, so it can represent all JSON data structures. Bean-to-HOCON-to-bean round-trips are fully equivalent to JSON at the same level of configuration.

### Supported (same as JSON)

| Java Type | Support |
|-----------|---------|
| Objects (beans, maps) | Full round-trip |
| Arrays (collections, arrays) | Full round-trip |
| `String` (simple) | Full round-trip — serializer auto-quotes strings containing special chars |
| `String` (ambiguous: `"true"`, `"42"`, `"null"`) | Full round-trip — quoted in output to disambiguate |
| `String` (multi-line) | Full round-trip — serialized as triple-quoted `"""..."""` |
| `int`, `long`, `float`, `double` | Full round-trip |
| `boolean` | Full round-trip |
| `null` | Full round-trip |
| Nested/recursive structures | Full round-trip |
| Polymorphic types | Full round-trip when `addBeanTypes()` is enabled (same constraint as JSON) |
| Date/time types (`Instant`, `ZonedDateTime`, etc.) | Full round-trip as ISO 8601 quoted strings (same as JSON) |
| `Duration` | Full round-trip as ISO 8601 duration strings (same as JSON) |

### Limitations

These limitations apply only when Juneau is used to **parse hand-written HOCON files** that use advanced HOCON features. For bean → HOCON → bean round-trips, they have no effect.

| Limitation | Documented On | Behavior |
|---|---|---|
| `include` directives not supported | `HoconParser` | Ignored with a warning during parse; the serializer never emits them |
| Substitutions (`${var}`) not emitted by serializer | `HoconSerializer` | Serializer always writes resolved (concrete) values; parser resolves substitutions before bean conversion, so substitutions do not survive round-trip |
| Value concatenation not emitted by serializer | `HoconSerializer` | Parser resolves concatenation on input (e.g. `path = /usr "/local"` → `"/usr/local"`); serializer always writes complete strings |
| Path expressions not emitted by serializer | `HoconSerializer` | Parser supports both path-expression form (`a.b.c = 1`) and nested-brace form; serializer always writes nested-brace form |
| Duration/size units (`10s`, `512K`, `2G`) | `HoconParser` | Treated as unquoted strings; Juneau does not interpret them as `java.time.Duration` or byte-count types — that is an application-level concern |

---

## Architecture

HOCON has complex syntax (path expressions, substitutions, value concatenation, object merging) requiring a dedicated tokenizer and a multi-pass parser.

```
WriterSerializer
  └── HoconSerializer
        └── HoconSerializerSession (uses HoconWriter)

ReaderParser
  └── HoconParser
        └── HoconParserSession (uses HoconTokenizer)
              └── HoconResolver (substitution resolution)
```

### Parsing Strategy

HOCON parsing is inherently multi-pass:

1. **Tokenize**: Break input into tokens (strings, numbers, punctuation, comments)
2. **Parse structure**: Build an intermediate tree (handles path expressions, object merging, `+=`)
3. **Resolve substitutions**: Replace `${var}` references with resolved values
4. **Convert to bean**: Map the resolved tree to the target Java bean

---

## Files to Create

All source files in `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/hocon/`.

### 1. `HoconSerializer.java`

Extends `WriterSerializer`. Implements `HoconMetaProvider`.

```java
package org.apache.juneau.hocon;

public class HoconSerializer extends WriterSerializer implements HoconMetaProvider {

    public static final HoconSerializer DEFAULT = ...;            // Readable, no root braces
    public static final HoconSerializer DEFAULT_BRACES = ...;     // Readable, with root braces
    public static final HoconSerializer DEFAULT_COMPACT = ...;    // Single-line JSON-like

    public static class Builder extends WriterSerializer.Builder {
        boolean useEqualsSign = true;          // Use = (not :) for key-value
        boolean useUnquotedStrings = true;     // Omit quotes for simple values
        boolean useUnquotedKeys = true;        // Omit quotes for simple keys
        boolean omitRootBraces = true;         // Omit {} for root (config file style)
        boolean useMultilineStrings = true;    // Use """ for multi-line strings
        boolean useNewlineSeparators = true;   // Use newlines, not commas

        protected Builder() {
            produces("application/hocon");
            accept("application/hocon");
        }
    }

    @Override
    public HoconSerializerSession.Builder createSession() { ... }
}
```

### 2. `HoconSerializerSession.java`

Extends `WriterSerializerSession`. Core serialization logic.

Key methods:
- `doSerialize(SerializerPipe, Object)` -- Entry point
- `serializeAnything(HoconWriter, Object, ClassMeta, String, BeanPropertyMeta)` -- Central dispatch
- `serializeBeanMap(HoconWriter, BeanMap, String, boolean isRoot)` -- Bean properties with HOCON syntax
- `serializeMap(HoconWriter, Map, ClassMeta, boolean isRoot)` -- Map entries
- `serializeCollection(HoconWriter, Collection, ClassMeta)` -- Array values
- `serializeString(HoconWriter, String)` -- Decides: unquoted, quoted, or triple-quoted

Serialization algorithm:
```
serializeBeanMap(writer, beanMap, typeName, isRoot):
    if not isRoot or not omitRootBraces:
        writer.objectStart()  // {

    for each (key, value) in beanMap:
        if value is null and not keepNullProperties: continue

        if value is bean or map:
            writer.key(key)
            writer.append(" ")   // space before {, no = for objects
            serializeBeanMap/Map(writer, value, false)
        else:
            writer.key(key)
            writer.append(" = ")  // = separator for scalars
            serializeAnything(writer, value)
        writer.newLineOrComma()

    if not isRoot or not omitRootBraces:
        writer.objectEnd()  // }
```

### 3. `HoconWriter.java`

Extends `SerializerWriter`. HOCON-specific formatting.

```java
class HoconWriter extends SerializerWriter {
    void key(String name);                   // Unquoted or quoted key
    void pathKey(String path);               // Dot-separated path key
    void equalsSign();                       // " = "
    void objectStart();                      // "{\n"
    void objectEnd();                        // "}"
    void arrayStart();                       // "["
    void arrayEnd();                         // "]"
    void unquotedString(String value);       // Simple value
    void quotedString(String value);         // "value" with escaping
    void tripleQuotedString(String value);   // """..."""
    void separator();                        // Newline or comma
    boolean needsQuoting(String value);      // Special chars check
}
```

### 4. `HoconParser.java`

Extends `ReaderParser`. Implements `HoconMetaProvider`.

```java
package org.apache.juneau.hocon;

public class HoconParser extends ReaderParser implements HoconMetaProvider {

    public static final HoconParser DEFAULT = ...;

    public static class Builder extends ReaderParser.Builder {
        boolean resolveSubstitutions = true;  // Resolve ${var} references

        protected Builder() {
            consumes("application/hocon");
        }
    }

    @Override
    public HoconParserSession.Builder createSession() { ... }
}
```

### 5. `HoconParserSession.java`

Extends `ReaderParserSession`. Core parsing logic.

Parsing is multi-pass:

```
doParse(pipe, type):
    tokenizer = new HoconTokenizer(pipe.getReader())

    // Phase 1: Parse into intermediate tree (HoconValue)
    root = parseRoot(tokenizer)  // Handles root-braceless, path expressions, merging

    // Phase 2: Resolve substitutions
    if resolveSubstitutions:
        resolver = new HoconResolver(root)
        resolver.resolve()

    // Phase 3: Convert to target type
    return convertToBean(root, type)
```

Key methods:
- `parseRoot(HoconTokenizer)` -- Detects root-braceless or braced
- `parseObject(HoconTokenizer)` -- Parses `{ ... }` into `HoconObject`
- `parseArray(HoconTokenizer)` -- Parses `[ ... ]`
- `parseValue(HoconTokenizer)` -- Parses any value
- `parsePath(String)` -- Splits `a.b.c` into path components
- `mergeObjects(HoconObject, HoconObject)` -- Merges duplicate keys
- `convertToBean(HoconValue, ClassMeta)` -- Maps intermediate tree to bean

### 6. `HoconTokenizer.java`

Internal tokenizer.

```java
class HoconTokenizer {
    HoconTokenizer(Reader reader);

    Token peek();
    Token read();
    void skipWhitespaceAndComments();

    enum TokenType {
        UNQUOTED_STRING,   // Unquoted key or value
        QUOTED_STRING,     // "..." with escaping
        TRIPLE_QUOTED,     // """...""" 
        NUMBER,            // 42, 3.14
        TRUE, FALSE, NULL,
        LBRACE, RBRACE,
        LBRACKET, RBRACKET,
        COLON,             // :
        EQUALS,            // =
        PLUS_EQUALS,       // +=
        COMMA,
        NEWLINE,           // Significant newline
        SUBSTITUTION,      // ${var}
        OPT_SUBSTITUTION,  // ${?var}
        EOF
    }

    String readUnquotedString();     // Until special char
    String readQuotedString();       // "..." or '...'
    String readTripleQuoted();       // """..."""
    String readSubstitution();       // ${...} content
}
```

### 7. `HoconValue.java`

Intermediate representation for parsed HOCON data (before conversion to beans).

```java
abstract class HoconValue {
    enum Type { STRING, NUMBER, BOOLEAN, NULL, OBJECT, ARRAY, SUBSTITUTION }
}

class HoconObject extends HoconValue {
    LinkedHashMap<String, HoconValue> members;
    void merge(HoconObject other);           // Object merging
    void setPath(String[] path, HoconValue); // Path expression support
    HoconValue getPath(String[] path);       // Resolve dotted path
}

class HoconArray extends HoconValue {
    List<HoconValue> elements;
    void concat(HoconArray other);           // Array concatenation
}

class HoconString extends HoconValue { String value; }
class HoconNumber extends HoconValue { Number value; }
class HoconBoolean extends HoconValue { boolean value; }
class HoconNull extends HoconValue {}
class HoconSubstitution extends HoconValue {
    String path;       // The reference path
    boolean optional;  // ${?var} vs ${var}
}
class HoconConcat extends HoconValue {
    List<HoconValue> parts;  // Value concatenation
}
```

### 8. `HoconResolver.java`

Resolves `${var}` substitutions in the parsed tree.

```java
class HoconResolver {
    HoconResolver(HoconObject root);

    void resolve();  // Resolve all substitutions in-place

    HoconValue lookup(String path);  // Look up a path in the root
    String resolveConcat(HoconConcat concat);  // Resolve concatenation
}
```

Resolution rules:
- Substitutions reference paths in the root object (e.g., `${database.host}`)
- `${?var}` is ignored if the path doesn't exist (value removed)
- `${var}` throws an error if the path doesn't exist
- Circular references are detected and cause an error

### 9. `HoconMetaProvider.java`, `HoconClassMeta.java`, `HoconBeanPropertyMeta.java`

Standard metadata classes.

### 10. `annotation/Hocon.java` and `annotation/HoconConfig.java`

```java
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
public @interface Hocon {
    String on() default "";
    String[] onClass() default {};
}

@Target({TYPE, METHOD})
@Retention(RUNTIME)
public @interface HoconConfig {
    String useEqualsSign() default "";
    String useUnquotedStrings() default "";
    String omitRootBraces() default "";
    int rank() default 0;
}
```

### 11. `annotation/HoconAnnotation.java` and `annotation/HoconConfigAnnotation.java`

Annotation utility classes.

### 12. `Hocon.java` (Marshaller)

Location: `src/main/java/org/apache/juneau/marshaller/Hocon.java`

Extends `CharMarshaller`.

### 13. `package-info.java`

---

## Files to Modify

### 1. `BasicUniversalConfig.java`

Add `HoconSerializer.class` to `serializers` and `HoconParser.class` to `parsers`.

### 2. `BasicHoconConfig.java` (New)

### 3. `RestClient.java`

Add to `universal()` method.

### 4. Context: `Context.java`

Path: `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/Context.java`

Add `import org.apache.juneau.hocon.annotation.*;` and add `{@link HoconConfig}` to the list of config annotations in the `applyAnnotations()` Javadoc (alphabetically).

---

## Test Plan

### 1. Serializer Tests: `org/apache/juneau/hocon/HoconSerializer_Test.java`

- **a01_simpleBean** -- Unquoted keys and values with `=` separator
- **a02_nestedBean** -- Nested object without `=` (space-brace syntax)
- **a03_collectionOfStrings** -- Array `[a, b, c]`
- **a04_collectionOfBeans** -- Array of objects
- **a05_mapProperty** -- Map as object
- **a06_nullValues** -- `null` keyword
- **a07_booleanValues** -- `true`/`false`
- **a08_numberValues** -- Integers and floats
- **a09_quotedStringRequired** -- Strings with special chars quoted
- **a10_tripleQuotedString** -- Multi-line strings with `"""`
- **a11_unquotedStrings** -- Simple values unquoted
- **a12_unquotedKeys** -- Simple keys unquoted
- **a13_equalsSignSeparator** -- `key = value` syntax
- **a14_omitRootBraces** -- Root object without `{}`
- **a15_withRootBraces** -- Root object with `{}`
- **a16_newlineSeparators** -- Newlines between members
- **a17_compactMode** -- JSON-like single line
- **a18_indentation** -- Proper indentation
- **a19_emptyBean** -- `{}`
- **a20_emptyCollection** -- `[]`
- **a21_enumValues** -- Enums as unquoted strings
- **a22_stringLikeSpecialValues** -- `"true"`, `"42"`, `"null"` quoted for disambiguation

### 2. Parser Tests: `org/apache/juneau/hocon/HoconParser_Test.java`

- **b01_equalsSignSeparator** -- `key = value`
- **b02_colonSeparator** -- `key: value`
- **b03_unquotedKeys** -- `key = value`
- **b04_quotedKeys** -- `"key" = value`
- **b05_unquotedValues** -- `key = hello`
- **b06_quotedValues** -- `key = "hello world"`
- **b07_tripleQuotedValues** -- `key = """...\n..."""`
- **b08_pathExpressions** -- `a.b.c = 10` → nested object `{a:{b:{c:10}}}`
- **b09_objectMerging** -- `a { x = 1 }\na { y = 2 }` → `{a:{x:1,y:2}}`
- **b10_plusEquals** -- `list += item` appends to array
- **b11_substitutions** -- `${var}` resolved to referenced value
- **b12_optionalSubstitutions** -- `${?var}` ignored if undefined
- **b13_selfReferentialSubstitution** -- `path = ${path}"/bin"` appends
- **b14_valueConcatenation** -- `path = /usr "/local"` → `"/usr/local"`
- **b15_arrayConcatenation** -- `[1,2] [3,4]` → `[1,2,3,4]`
- **b16_objectConcatenation** -- `{a:1} {b:2}` → `{a:1,b:2}`
- **b17_hashComments** -- `# comment`
- **b18_slashComments** -- `// comment`
- **b19_newlineSeparators** -- Newlines between members
- **b20_trailingCommas** -- Trailing commas ignored
- **b21_rootBraceless** -- Root without `{}`
- **b22_nestedObjects** -- Multi-level nesting
- **b23_arrays** -- `[1, 2, 3]`
- **b24_quotedPathComponent** -- `"a.b".c = 10` (literal dot in key)
- **b25_numberValues** -- Integers and floats
- **b26_booleanNullValues** -- `true`, `false`, `null`

### 3. Round-Trip Tests: `org/apache/juneau/hocon/HoconRoundTrip_Test.java`

- **c01_simpleBeanRoundTrip** -- All simple types
- **c02_nestedBeanRoundTrip** -- Nested beans
- **c03_collectionRoundTrip** -- Collections
- **c04_mapRoundTrip** -- Maps
- **c05_enumRoundTrip** -- Enums
- **c06_nullRoundTrip** -- Null values
- **c07_multilineStringRoundTrip** -- Multi-line strings
- **c08_specialCharStringRoundTrip** -- Strings requiring quoting
- **c09_complexBeanRoundTrip** -- Mixed types
- **c10_emptyCollectionsRoundTrip** -- Empty lists
- **c11_booleanStringRoundTrip** -- String "true" round-trips correctly
- **c12_numberStringRoundTrip** -- String "42" round-trips correctly

### 4. Path Expression Tests: `org/apache/juneau/hocon/HoconPath_Test.java`

- **d01_simplePath** -- `a.b = 1` → `{a:{b:1}}`
- **d02_deepPath** -- `a.b.c.d = 1` → 4 levels
- **d03_quotedPathComponent** -- `"a.b".c = 1` → `{"a.b":{c:1}}`
- **d04_mixedPathAndNested** -- `a.b { c = 1 }` → `{a:{b:{c:1}}}`
- **d05_pathMerging** -- `a.x = 1\na.y = 2` → `{a:{x:1,y:2}}`
- **d06_pathOverwrite** -- `a.b = 1\na.b = 2` → `{a:{b:2}}`

### 5. Substitution Tests: `org/apache/juneau/hocon/HoconSubstitution_Test.java`

- **e01_simpleSubstitution** -- `${var}` resolved
- **e02_nestedPathSubstitution** -- `${a.b.c}` resolved
- **e03_optionalSubstitutionPresent** -- `${?var}` resolved when present
- **e04_optionalSubstitutionMissing** -- `${?var}` removed when missing
- **e05_requiredSubstitutionMissing** -- `${var}` throws error when missing
- **e06_circularSubstitution** -- Circular reference detected and throws error
- **e07_stringConcatSubstitution** -- `"Hello ${name}"` → string concatenation
- **e08_selfReferentialAppend** -- `path = ${path}"/bin"` → appends

### 6. Object Merging Tests: `org/apache/juneau/hocon/HoconMerging_Test.java`

- **f01_duplicateObjectsMerge** -- Two objects with same key merge
- **f02_scalarOverwrite** -- Duplicate scalar key overwrites
- **f03_objectOverScalar** -- Object replaces scalar of same key
- **f04_scalarOverObject** -- Scalar replaces object of same key
- **f05_plusEqualsArray** -- `list += item` appends
- **f06_deepMerge** -- Nested object merging

### 7. Tokenizer Tests: `org/apache/juneau/hocon/HoconTokenizer_Test.java`

- **g01_unquotedString** -- Until special char
- **g02_quotedString** -- `"..."` with escapes
- **g03_tripleQuoted** -- `"""..."""`
- **g04_number** -- `42`, `3.14`
- **g05_booleanNull** -- `true`, `false`, `null`
- **g06_structural** -- `{}[]:=,`
- **g07_plusEquals** -- `+=`
- **g08_substitution** -- `${var}`
- **g09_optSubstitution** -- `${?var}`
- **g10_hashComment** -- `# comment`
- **g11_slashComment** -- `// comment`
- **g12_equalsSign** -- `=` vs `:`

### 8. Marshaller Tests: `org/apache/juneau/marshaller/Hocon_Test.java`

- **h01_of** -- Serialize
- **h02_to** -- Parse
- **h03_roundTrip** -- Serialize + parse
- **h04_defaultInstance** -- `Hocon.DEFAULT`

### 9. Edge Case Tests: `org/apache/juneau/hocon/HoconEdgeCases_Test.java`

- **i01_emptyInput** -- Returns null
- **i02_onlyComments** -- Returns null
- **i03_unicodeStrings** -- Unicode in keys and values
- **i04_windowsLineEndings** -- `\r\n` handled
- **i05_deeplyNested** -- 10+ levels
- **i06_cyclicReferences** -- Recursion detection
- **i07_optionalProperties** -- `Optional<T>`
- **i08_veryLongUnquotedStrings** -- Long strings
- **i09_emptyObject** -- `{}`
- **i10_emptyArray** -- `[]`

### 10. Media Type Tests: `org/apache/juneau/hocon/HoconMediaType_Test.java`

- **j01_produces** -- `application/hocon`
- **j02_consumes** -- `application/hocon`
- **j03_contentNegotiation** -- Correct selection

### 11. Cross-Format Round-Trip Integration: `org/apache/juneau/a/rttests/RoundTripDateTime_Test.java`

Add a `RoundTrip_Tester` entry for this format to the `TESTERS` array in `RoundTripDateTime_Test.java`. This test verifies date/time and Duration round-trip across all serializer/parser combinations. It covers beans with `Instant`, `ZonedDateTime`, `LocalDate`, `LocalDateTime`, `LocalTime`, `OffsetDateTime`, `OffsetTime`, `Year`, `YearMonth`, `Calendar`, `Date`, `Duration`, and `XMLGregorianCalendar` fields.

**Exact tester configuration:**
```java
tester(N, "Hocon - default")
    .serializer(HoconSerializer.create().keepNullProperties().addBeanTypes().addRootType())
    .parser(HoconParser.create())
    .build()
```

No `.skipIf(...)` or `.returnOriginalObject()` is needed — HOCON supports all the same data structures as JSON and round-trips at full fidelity for all date/time and Duration types (serialized as ISO 8601 quoted strings). The tester index `N` should be the next sequential integer after the last entry in the `TESTERS` array at time of implementation.

---

## Documentation

### CSS Code Block Classes

HOCON output code blocks in Javadoc should use `<p class='bjson'>` (the existing bordered-JSON CSS class) for all HOCON syntax examples. This matches the approach used by TOML and YAML doc examples, which also reuse existing CSS classes rather than defining new ones. A dedicated `bhocon` CSS class can be added to `juneau-code.css` as a future enhancement if HOCON-specific syntax highlighting (e.g., different color for `=` separator or unquoted values) is desired.

---

### `HoconSerializer.java` Javadoc

Follow the `JsonSerializer` documentation pattern exactly. Include the following sections in order:

**`<h5 class='topic'>Media types</h5>`**
- Accepts: `application/hocon`
- Produces: `application/hocon`

**`<h5 class='topic'>Description</h5>`**

Use `<ul class='spaced-list'>` to list Java type → HOCON output mappings:
- `HashMap` / bean → HOCON object with unquoted keys and `=` separator (`{ key = value }`)
- `Collection` / array → HOCON array (`[a, b, c]`)
- `String` (simple, no special chars) → unquoted string (`name = myapp`)
- `String` (contains special chars or is ambiguous like `"true"`, `"42"`, `"null"`) → quoted string (`path = "/usr/local/bin"`)
- `String` (multi-line) → triple-quoted string (`desc = """..."""`)
- `Number` → number literal (`port = 8080`)
- `Boolean` → `true` or `false`
- `null` → `null` keyword
- Non-primitive types (e.g., `Date`, `URI`) → converted via `ObjectSwap` before serialization

**`<h5 class='topic'>Behavior-specific subclasses</h5>`**

`<ul class='spaced-list'>` listing:
- `HoconSerializer.DEFAULT` — readable, unquoted keys/values, `=` separator, root braces omitted (config-file style)
- `HoconSerializer.DEFAULT_BRACES` — same as `DEFAULT` but includes root `{ }` braces
- `HoconSerializer.DEFAULT_COMPACT` — single-line compact output, JSON-compatible

**`<h5 class='section'>Example:</h5>`**

`<p class='bjava'>` block showing:
```java
// Serialize a bean
String hocon = HoconSerializer.DEFAULT.serialize(myBean);

// Create a custom serializer instance
HoconSerializer s = HoconSerializer.create().omitRootBraces(false).build();
String hocon2 = s.serialize(myBean);
```

**`<h5 class='figure'>Example output (bean, root braces omitted):</h5>`**

`<p class='bjson'>` block:
```hocon
name = myapp
port = 8080
debug = true
database {
  host = localhost
  port = 5432
}
tags = [web, api, rest]
```

**`<h5 class='figure'>Example output (with root braces — DEFAULT_BRACES):</h5>`**

`<p class='bjson'>` block:
```hocon
{
  name = myapp
  port = 8080
  debug = true
}
```

**`<h5 class='section'>Limitations:</h5><ul>`**

Use `<li class='note'>` entries:
- Substitutions (`${var}`) are never emitted — the serializer always writes resolved (concrete) values.
- Value concatenation is never emitted — the serializer always writes complete strings.
- Path expressions (`a.b.c = value`) are never emitted — the serializer always uses nested-brace form.
- `include` directives are never emitted — these require file-system access outside the scope of a serializer.

**`<h5 class='section'>Notes:</h5><ul>`**
- `<li class='note'>` — This class is thread safe and reusable.

**`<h5 class='section'>See Also:</h5><ul>`**
- `<li class='link'>` — `<a class="doclink" href="https://juneau.apache.org/docs/topics/HoconBasics">hocon-basics</a>`

---

### `HoconParser.java` Javadoc

Follow the `JsonParser` documentation pattern. Include the following sections in order:

**`<h5 class='topic'>Media types</h5>`**
- Handles `Content-Type: application/hocon`

**`<h5 class='topic'>Description</h5>`**

First `<ul class='spaced-list'>` describing parser features:
- Multi-pass parsing strategy: tokenize → build intermediate tree → resolve substitutions → convert to bean
- Accepts all three key-value separators: `=` (HOCON convention), `:` (JSON style), and whitespace before `{` (for object values)
- Unquoted string keys and values (strings that do not contain `$"{}[]:=,+#^\/?!@*&`)
- Path expression support: `a.b.c = value` expands to nested objects equivalent to `{a:{b:{c:value}}}`
- Object merging: duplicate keys merge rather than overwrite (`a {x=1}\na {y=2}` → `a {x=1, y=2}`)
- Substitution resolution: `${var}` references are resolved after the full file is parsed; `${?var}` is optional (silently ignored if undefined)
- Triple-quoted strings (`"""..."""`) with no escape processing inside
- Both `//` and `#` single-line comments
- Optional root braces — top-level `{}` can be omitted

Second `<ul class='spaced-list'>` mapping HOCON value types → Java types (same structure as `JsonParser`):
- `{ ... }` or root-braceless object → bean / `JsonMap`
- `[ ... ]` → collection / `JsonList`
- Number literal → `Number`
- `true` / `false` → `Boolean`
- `null` → `null`
- Unquoted, quoted (`"..."`), or triple-quoted (`"""..."""`) string → `String`

**`<h5 class='figure'>Example input (bean):</h5>`**

`<p class='bjson'>` block:
```hocon
name = myapp
port = 8080
debug = true
database {
  host = localhost
  port = 5432
}
tags = [web, api, rest]
```

**`<h5 class='figure'>Example input (path expressions and substitutions):</h5>`**

`<p class='bjson'>` block:
```hocon
# Path expression - equivalent to nested braces
database.host = localhost
database.port = 5432

# Substitution
base-url = "https://example.com"
api-url = ${base-url}"/api/v1"
```

**`<h5 class='section'>Limitations:</h5><ul>`**

Use `<li class='note'>` entries:
- `include` directives are not supported — they are ignored with a warning during parsing.
- Duration/size unit suffixes (`10s`, `5m`, `512K`, `2G`) are treated as unquoted strings and are not converted to `java.time.Duration` or numeric byte counts — this is an application-level concern.
- Substitutions are resolved before bean conversion; the resolved (concrete) values are what Juneau sees. Substitution references are not preserved as-is.

**`<h5 class='section'>Notes:</h5><ul>`**
- `<li class='note'>` — This class is thread safe and reusable.

**`<h5 class='section'>See Also:</h5><ul>`**
- `<li class='link'>` — `<a class="doclink" href="https://juneau.apache.org/docs/topics/HoconBasics">hocon-basics</a>`

---

### `Hocon.java` (Marshaller) Javadoc

Follow the `Json.java` marshaller pattern exactly. Include the following sections in order:

**Opening sentence:**
> A pairing of a {@link HoconSerializer} and {@link HoconParser} into a single class with convenience read/write methods.

**`<h5 class='figure'>Examples:</h5>`**

Two `<p class='bjava'>` blocks:

Block 1 — instance usage:
```java
// Instance usage
Hocon hocon = new Hocon();
MyBean bean = hocon.read(hoconString, MyBean.class);
String hoconOut = hocon.write(bean);
```

Block 2 — static DEFAULT usage:
```java
// Static DEFAULT instance
MyBean bean = Hocon.DEFAULT.read(hoconString, MyBean.class);
String hoconOut = Hocon.DEFAULT.write(bean);
```

**`<h5 class='figure'>Example output (bean):</h5>`**

`<p class='bjson'>` block:
```hocon
name = myapp
port = 8080
debug = true
```

**`<h5 class='section'>See Also:</h5><ul>`**
- `<li class='link'>` — `<a class="doclink" href="https://juneau.apache.org/docs/topics/Marshallers">marshallers</a>`

---

### `HoconResolver.java` Javadoc

- Substitution resolution algorithm (single-pass over the tree, depth-first)
- Cycle detection (tracks in-progress paths to detect circular references)
- Optional (`${?var}`) vs required (`${var}`) substitutions
- How `HoconConcat` values (value concatenation) are resolved to final strings

### `HoconValue.java` Javadoc

- Purpose of the intermediate tree representation
- Object merging semantics (last value wins for scalars; recursive merge for objects; `+=` appends to arrays)
- How path expressions (`setPath`) create nested `HoconObject` nodes

### `package-info.java` Javadoc

- Full HOCON syntax summary with examples
- Bean mapping rules (key-value separators, unquoted strings, object/array syntax)
- Comparison with JSON: HOCON is a superset; all JSON is valid HOCON
- Round-trip fidelity summary (same as JSON, with the documented limitations)
- Links to the HOCON specification: `https://github.com/lightbend/config/blob/main/HOCON.md`

---

### Update Release Notes
/Users/james.bognar/git/apache/juneau/docs/pages/release-notes

### Update Documentation

Find how existing languages are documented in the following location and update the documentation to match
the same level of detail.
/Users/james.bognar/git/apache/juneau/docs/pages/topics

---

## Implementation Order

1. **`HoconTokenizer.java`** -- Foundation
2. **`HoconValue.java`** (and subclasses) -- Intermediate representation
3. **`HoconWriter.java`** -- HOCON-specific formatting
4. **`HoconSerializer.java`** + **`HoconSerializerSession.java`** -- Core serialization
5. **`HoconParser.java`** + **`HoconParserSession.java`** -- Core parsing (without substitutions)
6. **`HoconResolver.java`** -- Substitution resolution
7. **Tokenizer tests** (12 test cases)
8. **Serializer tests** (22 test cases)
9. **Parser tests** (26 test cases)
10. **Path expression tests** (6 test cases)
11. **Substitution tests** (8 test cases)
12. **Object merging tests** (6 test cases)
13. **Round-trip tests** (12 test cases)
14. **Metadata and annotations**
15. **Marshaller** + tests
16. **Edge case and media type tests**
17. **REST integration** (`BasicUniversalConfig`, `BasicHoconConfig`, `RestClient`)
18. **Context registration** (`HoconConfig`, `HoconConfigAnnotation`, `Context.java`)

---

## Key Design Decisions

### 1. Independent serializer/parser (not extending JsonSerializer)

HOCON's syntax differs substantially from JSON: `=` as separator, path expressions, object merging, value concatenation, substitutions, and triple-quoted strings. These require a dedicated tokenizer, intermediate representation, and multi-pass parser. Extending `JsonSerializer` would add more complexity than it saves.

### 2. Multi-pass parsing

HOCON cannot be parsed in a single pass because:
- Path expressions create nested structure (`a.b.c = 1`)
- Object merging requires accumulating all values for a key before building the final object
- Substitutions (`${var}`) reference values that may not yet be parsed

The three-pass approach (tokenize → build tree with merging → resolve substitutions) is the same strategy used by the Typesafe Config reference implementation.

### 3. Intermediate representation (`HoconValue`)

Unlike simpler formats where Juneau can parse directly into beans, HOCON requires an intermediate tree representation to handle merging, path expressions, and substitutions. The tree is then converted to beans in the final pass.

### 4. Serializer uses `=` and omits root braces by default

This produces HOCON that looks idiomatic for configuration files:
```hocon
host = localhost
port = 8080
```
Rather than JSON-like:
```hocon
{
  host: localhost
  port: 8080
}
```

### 5. Path expressions are not used in serialization

The serializer writes nested objects with braces, not flattened paths:
```hocon
database {
  host = localhost
}
```
Not:
```hocon
database.host = localhost
```

Both are equivalent HOCON, but the nested form is more readable and standard in config files. The parser, however, fully supports path expression input.

### 6. Substitution resolution is parsing-only

The serializer does not emit `${var}` substitutions (it writes concrete values from beans). Substitution support is a parser feature only, allowing Juneau to read HOCON config files that use substitutions.

### 7. `include` directives are not supported

`include` requires file system access to load external files, which is outside the scope of a serializer/parser. If encountered during parsing, `include` directives are ignored with a warning. This is consistent with the "no external dependencies" constraint.

### 8. Value concatenation is supported in parsing only

The parser handles value concatenation (`path = /usr "/local"` → `"/usr/local"`). The serializer does not emit concatenated values (it writes complete strings).

### 9. Date/Time and Duration Support

Date/time types (`Date`, `Calendar`, `Temporal` subtypes) and `Duration` are built-in first-class types. The HOCON serializer's `serializeAnything()` dispatch should include `sType.isDateOrCalendarOrTemporal()` and `sType.isDuration()` checks (before `isBean()`), formatting values as quoted ISO 8601 strings. The parser should use `Iso8601Utils.parse()` to convert ISO 8601 strings back to target types. See `builtin-datetime-iso8601.md` for the full architecture.

---

## Iterator/Iterable/Stream Support

As of 9.2.1, Juneau natively supports serialization of `Iterator`, `Iterable` (non-Collection), `Enumeration`, and `java.util.stream.Stream` types.

This format uses the **lazy (streaming) path**: elements are written directly to the output one at a time without materializing into an intermediate List.

In `serializeAnything()`, add a branch after the `isCollection()/isArray()` checks:

```java
} else if (sType.isStreamable()) {
    serializeStreamable(out, o, sType, eType);
}
```

Add a `serializeStreamable()` method using `forEachStreamableEntry()` from SerializerSession, following the same output pattern as `serializeCollection()` but without requiring a Collection parameter.

---

## Scope Exclusions

- **`include` directives** -- Requires file system access
- **Environment variable substitution** (`${?ENV_VAR}`) -- Would require system access
- **Duration/size unit parsing** -- Application-level concern (values are strings)
- **HOCON merging across multiple files** -- Single-file scope
- **Serializer path flattening** -- Nested objects used instead (both are valid HOCON)
- **Serializer value concatenation** -- Complete values written
- **Serializer substitution emission** -- Concrete values written
