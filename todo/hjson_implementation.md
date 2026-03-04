# Hjson (Human JSON) Support Implementation Plan for Apache Juneau

## Overview

This plan covers implementing serialization and parsing support for Hjson (Human JSON) in Juneau. Hjson is a syntax extension of JSON designed to be more human-friendly for configuration files and hand-edited data. It adds features beyond JSON5 that Juneau's existing parsers do not support: **quoteless string values**, **multiline strings** (`'''`), and **optional root braces**.

The implementation is a new serializer/parser pair with its own tokenizer, since Hjson's quoteless string and multiline string syntax requires fundamentally different lexing logic from JSON/JSON5.

## Specification

- **Spec**: https://hjson.github.io/syntax.html
- **RFC Draft**: https://hjson.github.io/rfc.html
- **Media type**: `application/hjson`, `application/hjson+json`
- **File extension**: `.hjson`
- **Encoding**: UTF-8 (no BOM)

---

## Hjson Syntax Summary

### Example

```hjson
{
  # this is a comment
  name: John Doe
  age: 42
  active: true

  address: {
    city: Springfield
    state: IL
  }

  // tags don't need quotes either
  tags: [web, api, rest]

  description:
    '''
    This is a multiline
    string without escapes.
    It preserves formatting.
    '''

  notes: This is a quoteless string value that ends at the newline
}
```

### Key Features (beyond JSON and JSON5)

| Feature | JSON | JSON5 | Hjson | Notes |
|---------|------|-------|-------|-------|
| `//` and `/* */` comments | No | Yes | Yes | |
| `#` comments | No | No | **Yes** | Hash-style comments |
| Unquoted keys | No | Yes | Yes | |
| Unquoted string values | No | No | **Yes** | End at newline |
| Multiline strings (`'''`) | No | No | **Yes** | Triple-quoted |
| Trailing commas | No | Yes | Yes | |
| Optional commas (newline as separator) | No | No | **Yes** | Commas or newlines |
| Optional root braces | No | No | **Yes** | Top-level can omit `{}` |
| Single-quoted strings | No | Yes | Yes | |

### Detailed Syntax Rules

**1. Comments:**
- `#` single-line comment (to end of line)
- `//` single-line comment (to end of line)
- `/* ... */` multi-line comment

**2. Quoteless String Values:**
- A value that is not a number, boolean, null, object, array, or quoted string is a quoteless string
- Quoteless strings cannot start with `{}[],:"'` or `#`, `//`, `/*`
- They end at the end of the current line (newline terminates them)
- Leading and trailing whitespace is trimmed
- No escape sequences in quoteless strings
- Parser only treats a value as quoteless if no other interpretation (number, boolean, null) works

**3. Multiline Strings (`'''`):**
- Delimited by `'''` (triple single quotes)
- First line after `'''` opening is the start of content (leading whitespace on the opening line is ignored)
- Content continues until closing `'''` is found
- The column position of the opening `'''` defines the indentation baseline -- whitespace up to that column is stripped from each subsequent line
- The final newline before closing `'''` is ignored
- Line feed is always `\n` (OS-independent)
- No escape sequences

**4. Keys:**
- Quoted keys: `"key"` or `'key'` (with standard JSON escaping)
- Unquoted keys: Any characters except `{}[],:"'` and whitespace. Unquoted keys end at `:` or whitespace.

**5. Separators:**
- Values/members can be separated by commas OR newlines
- Trailing commas are ignored
- Commas are optional

**6. Root Object:**
- The `{}` braces for the root-level object are optional
- A file can contain only key-value pairs without wrapping braces

**7. Values:**
- Numbers: same as JSON (decimal, no hex/octal in standard Hjson)
- Booleans: `true`, `false`
- Null: `null`
- Strings: double-quoted, single-quoted, quoteless, or multiline
- Objects: `{ ... }`
- Arrays: `[ ... ]`

---

## Bean-to-Hjson Mapping

```java
class Config {
    String name;
    int port;
    boolean debug;
    Database db;
    List<String> tags;
    String description;
}
class Database { String host; int port; }
```

Serialized (readable mode, default):
```hjson
{
  name: myapp
  port: 8080
  debug: true
  db: {
    host: localhost
    port: 5432
  }
  tags: [
    web
    api
    rest
  ]
  description:
    '''
    A multi-line description
    with preserved formatting.
    '''
}
```

Serialized (compact mode):
```hjson
{name:myapp,port:8080,debug:true,db:{host:localhost,port:5432},tags:[web,api,rest],description:"A multi-line description\nwith preserved formatting."}
```

---

## Supported Data Structures

All data structures supported by JSON are fully supported by Hjson. The table below shows the explicit mapping:

| Java type | JSON representation | Hjson representation (readable mode) |
|---|---|---|
| `Map` (HashMap, TreeMap, etc.) | `{...}` | `{...}` (quoteless keys by default) |
| `Collection` / arrays | `[...]` | `[...]` (newline-separated by default) |
| `String` (simple) | `"value"` | `value` (quoteless, no quotes) |
| `String` (special chars or multi-line) | `"val\nue"` | `'''val\nue'''` or `"val\nue"` |
| `Number` (Integer, Long, Double, etc.) | `42`, `3.14` | same as JSON |
| `Boolean` | `true` / `false` | same as JSON |
| `null` | `null` | same as JSON |
| Bean (POJO) | `{...}` | `{...}` |
| `Date`, `Calendar`, temporal types | ISO-8601 string | ISO-8601 quoteless or quoted string |
| `Enum` | `"VALUE"` | `VALUE` (quoteless) |
| `Iterator`, `Iterable`, `Stream` | `[...]` | `[...]` (lazy streaming) |

Full round-trip is achievable for all types. Non-primitive types use `ObjectSwap` exactly as in JSON.

---

### Serialization Rules

- **Simple string values** (no special characters): Serialized as quoteless strings (no quotes)
- **Strings containing `{}[],:"'#` or newlines**: Serialized with double quotes and JSON escaping
- **Multi-line strings**: Serialized with `'''` triple quotes (when `useMultilineStrings` is enabled and the string contains newlines)
- **Numbers, booleans, null**: Standard JSON representation
- **Keys**: Unquoted if they contain only safe characters, otherwise quoted
- **Newlines as separators**: In readable mode, use newlines instead of commas
- **Root braces**: Always included by serializer (for unambiguous output)

---

## Architecture

Hjson has unique lexing requirements (quoteless strings, multiline strings, `#` comments, newline-as-separator), so it extends `WriterSerializer`/`ReaderParser` directly with its own tokenizer.

```
WriterSerializer
  └── HjsonSerializer
        └── HjsonSerializerSession (uses HjsonWriter)

ReaderParser
  └── HjsonParser
        └── HjsonParserSession (uses HjsonTokenizer)
```

---

## Files to Create

All source files in `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/hjson/`.

### 1. `HjsonSerializer.java`

Extends `WriterSerializer`. Implements `HjsonMetaProvider`.

```java
package org.apache.juneau.hjson;

public class HjsonSerializer extends WriterSerializer implements HjsonMetaProvider {

    public static final HjsonSerializer DEFAULT = ...;           // Readable (newline separators, indented)
    public static final HjsonSerializer DEFAULT_COMPACT = ...;   // Single line, commas

    public static class Builder extends WriterSerializer.Builder {
        boolean useMultilineStrings = true;    // Use ''' for multi-line strings
        boolean useQuotelessStrings = true;    // Omit quotes for simple string values
        boolean useQuotelessKeys = true;       // Omit quotes for simple keys
        boolean omitRootBraces = false;        // Omit {} for root object
        boolean useNewlineSeparators = true;   // Use newlines instead of commas

        protected Builder() {
            produces("application/hjson");
            accept("application/hjson,application/hjson+json");
        }
    }

    @Override
    public HjsonSerializerSession.Builder createSession() { ... }
}
```

Builder options:
- `useMultilineStrings(boolean)` -- Use `'''` for strings containing newlines (default: true)
- `useQuotelessStrings(boolean)` -- Omit quotes for simple string values (default: true)
- `useQuotelessKeys(boolean)` -- Omit quotes for simple keys (default: true)
- `omitRootBraces(boolean)` -- Omit `{}` for the root-level object (default: false)
- `useNewlineSeparators(boolean)` -- Use newlines instead of commas between members (default: true)

### 2. `HjsonSerializerSession.java`

Extends `WriterSerializerSession`. Core serialization logic.

Key methods:
- `doSerialize(SerializerPipe, Object)` -- Entry point
- `serializeAnything(HjsonWriter, Object, ClassMeta, String, BeanPropertyMeta)` -- Central dispatch
- `serializeBeanMap(HjsonWriter, BeanMap, String)` -- Bean properties as key-value pairs
- `serializeMap(HjsonWriter, Map, ClassMeta)` -- Map entries
- `serializeCollection(HjsonWriter, Collection, ClassMeta)` -- Array values (newline or comma separated)
- `serializeString(HjsonWriter, String)` -- Decides: quoteless, quoted, or multiline

String serialization decision:
```
serializeString(writer, value):
    if useMultilineStrings and value contains '\n':
        writeMultilineString(writer, value)  // '''...\n'''
    else if useQuotelessStrings and isSimpleString(value):
        writer.append(value)                 // quoteless
    else:
        writeQuotedString(writer, value)     // "..." with JSON escaping

isSimpleString(value):
    return value is not empty
       and does not start with {}[],:"'#///*
       and does not contain newlines
       and is not "true", "false", "null"
       and is not parseable as a number
```

### 3. `HjsonWriter.java`

Extends `SerializerWriter`. Provides Hjson-specific formatting.

```java
class HjsonWriter extends SerializerWriter {
    void objectStart(boolean omitBraces);   // { or nothing
    void objectEnd(boolean omitBraces);     // } or nothing
    void arrayStart();                      // [
    void arrayEnd();                        // ]
    void memberSeparator();                 // newline or comma based on config
    void keyValueSeparator();               // ": " or ":"
    void quotelessString(String value);     // value (no quotes)
    void quotedString(String value);        // "value" with JSON escaping
    void multilineString(String value);     // '''...\n'''
    void comment(String text);              // # text
    boolean needsQuoting(String key);       // Does this key need quotes?
    boolean isSimpleValue(String value);    // Can this value be quoteless?
}
```

### 4. `HjsonParser.java`

Extends `ReaderParser`. Implements `HjsonMetaProvider`.

```java
package org.apache.juneau.hjson;

public class HjsonParser extends ReaderParser implements HjsonMetaProvider {

    public static final HjsonParser DEFAULT = ...;

    public static class Builder extends ReaderParser.Builder {
        protected Builder() {
            consumes("application/hjson,application/hjson+json");
        }
    }

    @Override
    public HjsonParserSession.Builder createSession() { ... }
}
```

### 5. `HjsonParserSession.java`

Extends `ReaderParserSession`. Core parsing logic.

Key methods:
- `doParse(ParserPipe, ClassMeta)` -- Entry point (detects root braces or root-braceless)
- `parseAnything(ClassMeta, HjsonTokenizer)` -- Central dispatch
- `parseObject(HjsonTokenizer, ClassMeta)` -- Parse object (with or without braces)
- `parseArray(HjsonTokenizer, ClassMeta)` -- Parse array `[...]`
- `parseValue(HjsonTokenizer, ClassMeta)` -- Parse value (number, boolean, null, string, object, array)
- `parseQuotelessString(HjsonTokenizer)` -- Read to end of line, trim whitespace

Parsing root-braceless objects:
```
doParse(pipe, type):
    tokenizer = new HjsonTokenizer(pipe.getReader())
    tokenizer.skipWhitespaceAndComments()

    if tokenizer.peek() == '{':
        return parseObject(tokenizer, type)
    else if tokenizer.peek() == '[':
        return parseArray(tokenizer, type)
    else:
        // Root-braceless: treat as object members until EOF
        return parseRootBraceless(tokenizer, type)
```

### 6. `HjsonTokenizer.java`

Internal tokenizer for Hjson format.

```java
class HjsonTokenizer {
    HjsonTokenizer(Reader reader);

    Token peek();
    Token read();
    void skipWhitespaceAndComments();

    enum TokenType {
        STRING,        // "..." or '...'
        QUOTELESS,     // quoteless string value
        MULTILINE,     // '''...'''
        NUMBER,        // 42, 3.14
        TRUE, FALSE, NULL,
        LBRACE, RBRACE,    // { }
        LBRACKET, RBRACKET, // [ ]
        COLON,         // :
        COMMA,         // ,
        NEWLINE,       // Significant newline (separates members)
        EOF
    }

    // Value reading
    String readQuotedString();       // "..." or '...' with JSON escaping
    String readQuotelessString();    // To end of line, trimmed
    String readMultilineString();    // '''...\n'''
    Number readNumber();
    String readKey();                // Quoted or unquoted key

    int getLine();
    int getColumn();
}
```

Key tokenizer challenge: determining whether a token is a quoteless string or another value type. The tokenizer must:
1. Check if the next character starts a recognizable token (`{`, `[`, `"`, `'`, `'''`, digit, `-`, `t`, `f`, `n`)
2. If none match, treat it as a quoteless string (read to end of line)
3. For quoteless strings that start like numbers/booleans but aren't (e.g., `5 times`), read to end of line and return as string

### 7. `HjsonMetaProvider.java`, `HjsonClassMeta.java`, `HjsonBeanPropertyMeta.java`

Standard metadata classes. Initially minimal.

### 8. `annotation/Hjson.java` and `annotation/HjsonConfig.java`

```java
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
public @interface Hjson {
    String on() default "";
    String[] onClass() default {};
}

@Target({TYPE, METHOD})
@Retention(RUNTIME)
public @interface HjsonConfig {
    String useMultilineStrings() default "";
    String useQuotelessStrings() default "";
    String useQuotelessKeys() default "";
    String omitRootBraces() default "";
    int rank() default 0;
}
```

### 9. `annotation/HjsonAnnotation.java` and `annotation/HjsonConfigAnnotation.java`

Annotation utility classes.

### 10. `Hjson.java` (Marshaller)

Location: `src/main/java/org/apache/juneau/marshaller/Hjson.java`

Extends `CharMarshaller`.

```java
package org.apache.juneau.marshaller;

public class Hjson extends CharMarshaller {
    public static final Hjson DEFAULT = new Hjson();

    public static String of(Object object) throws SerializeException { ... }
    public static <T> T to(String input, Class<T> type) throws ParseException { ... }

    public Hjson() { this(HjsonSerializer.DEFAULT, HjsonParser.DEFAULT); }
    public Hjson(HjsonSerializer s, HjsonParser p) { super(s, p); }
}
```

### 11. `package-info.java`

---

## Files to Modify

### 1. `BasicUniversalConfig.java`

Add `HjsonSerializer.class` to `serializers` and `HjsonParser.class` to `parsers`.

### 2. `BasicHjsonConfig.java` (New)

```java
@Rest(
    serializers={HjsonSerializer.class},
    parsers={HjsonParser.class},
    defaultAccept="application/hjson"
)
public interface BasicHjsonConfig extends DefaultConfig {}
```

### 3. `RestClient.java`

Add `HjsonSerializer.class` and `HjsonParser.class` to the `universal()` method.

### 4. Context: `Context.java`

Path: `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/Context.java`

Add `import org.apache.juneau.hjson.annotation.*;` and add `{@link HjsonConfig}` to the list of config annotations in the `applyAnnotations()` Javadoc (alphabetically).

---

## Test Plan

### 1. Serializer Tests: `org/apache/juneau/hjson/HjsonSerializer_Test.java`

- **a01_simpleBean** -- String, int, boolean properties with quoteless values
- **a02_nestedBean** -- Nested bean as indented object
- **a03_collectionOfStrings** -- Array with newline-separated quoteless values
- **a04_collectionOfBeans** -- Array of objects
- **a05_mapProperty** -- Map as object with quoteless keys and values
- **a06_nullValues** -- Null serialized as `null`
- **a07_booleanValues** -- `true`/`false`
- **a08_numberValues** -- Integer and float values
- **a09_quotedStringRequired** -- Strings containing `{}[],:"'` are quoted
- **a10_multilineString** -- String with `\n` uses `'''` syntax
- **a11_quotelessStrings** -- Simple strings without quotes
- **a12_quotelessKeys** -- Simple keys without quotes
- **a13_quotedKeys** -- Keys with special chars use quotes
- **a14_newlineSeparators** -- Members separated by newlines (no commas)
- **a15_compactMode** -- Single-line with commas
- **a16_emptyBean** -- `{}`
- **a17_emptyCollection** -- `[]`
- **a18_stringLikeBoolean** -- String `"true"` must be quoted to avoid ambiguity
- **a19_stringLikeNumber** -- String `"42"` must be quoted to avoid ambiguity
- **a20_stringLikeNull** -- String `"null"` must be quoted to avoid ambiguity
- **a21_omitRootBraces** -- Root object without `{}`
- **a22_indentation** -- Proper indentation at each level

### 2. Parser Tests: `org/apache/juneau/hjson/HjsonParser_Test.java`

- **b01_quotedStrings** -- `"hello"` and `'hello'`
- **b02_quotelessStrings** -- `hello world` (to end of line)
- **b03_multilineStrings** -- `'''...\n'''` parsing
- **b04_multilineIndentation** -- Indentation stripping based on `'''` column
- **b05_hashComments** -- `# comment` ignored
- **b06_slashComments** -- `// comment` ignored
- **b07_blockComments** -- `/* comment */` ignored
- **b08_newlineSeparators** -- Members separated by newlines
- **b09_commaSeparators** -- Members separated by commas
- **b10_mixedSeparators** -- Mix of newlines and commas
- **b11_trailingCommas** -- Trailing commas ignored
- **b12_unquotedKeys** -- `key: value` (no quotes on key)
- **b13_quotedKeys** -- `"key with spaces": value`
- **b14_rootBraceless** -- Parse file without root `{}`
- **b15_rootWithBraces** -- Parse file with root `{}`
- **b16_numberValues** -- Integers and floats parsed correctly
- **b17_booleanValues** -- `true`/`false`
- **b18_nullValue** -- `null`
- **b19_nestedObjects** -- Nested `{ ... }` parsed
- **b20_arrays** -- `[ ... ]` with mixed separators
- **b21_quotelessValueEdgeCases** -- `5 times` is string, `5` is number, `true` is boolean
- **b22_quotelessStartingWithHash** -- Leading `#` treated as comment (Hjson spec edge case)

### 3. Round-Trip Tests: `org/apache/juneau/hjson/HjsonRoundTrip_Test.java`

- **c01_simpleBeanRoundTrip** -- Simple types round-trip
- **c02_nestedBeanRoundTrip** -- Nested beans round-trip
- **c03_collectionRoundTrip** -- Collections round-trip
- **c04_mapRoundTrip** -- Map properties round-trip
- **c05_enumRoundTrip** -- Enum values round-trip (quoteless string)
- **c06_multilineStringRoundTrip** -- Multiline strings round-trip through `'''`
- **c07_specialCharStringRoundTrip** -- Strings with special chars round-trip (quoted)
- **c08_booleanStringRoundTrip** -- String `"true"` round-trips (not confused with boolean)
- **c09_numberStringRoundTrip** -- String `"42"` round-trips (not confused with number)
- **c10_nullRoundTrip** -- Null properties round-trip
- **c11_emptyStringRoundTrip** -- Empty string round-trips (must be quoted `""`)
- **c12_complexBeanRoundTrip** -- Bean with all types

### 4. Tokenizer Tests: `org/apache/juneau/hjson/HjsonTokenizer_Test.java`

- **d01_quotedDouble** -- `"hello"` tokenized as STRING
- **d02_quotedSingle** -- `'hello'` tokenized as STRING
- **d03_multilineToken** -- `'''...\n'''` tokenized as MULTILINE
- **d04_quotelessToken** -- `hello world` tokenized as QUOTELESS
- **d05_numberToken** -- `42`, `3.14`, `-1`
- **d06_booleanTokens** -- `true`, `false`
- **d07_nullToken** -- `null`
- **d08_structuralTokens** -- `{}[]:,`
- **d09_hashComment** -- `# comment` skipped
- **d10_slashComment** -- `// comment` skipped
- **d11_blockComment** -- `/* comment */` skipped
- **d12_newlineAsToken** -- Significant newline between members

### 5. Marshaller Tests: `org/apache/juneau/marshaller/Hjson_Test.java`

- **e01_of** -- Serialize to Hjson string
- **e02_to** -- Parse from Hjson string
- **e03_roundTrip** -- Serialize + parse
- **e04_defaultInstance** -- `Hjson.DEFAULT`

### 6. Edge Case Tests: `org/apache/juneau/hjson/HjsonEdgeCases_Test.java`

- **f01_emptyInput** -- Returns null
- **f02_onlyComments** -- Returns null
- **f03_unicodeStrings** -- Unicode in keys and values
- **f04_windowsLineEndings** -- `\r\n` handled
- **f05_deeplyNested** -- 10+ nesting levels
- **f06_cyclicReferences** -- Recursion detection
- **f07_optionalProperties** -- `Optional<T>` handled
- **f08_quotelessWithTrailingSpaces** -- Trailing spaces trimmed
- **f09_multilineEmpty** -- `'''\n'''` is empty string
- **f10_mixedQuoteStyles** -- Mix of quoted and quoteless in same object

### 7. Media Type Tests: `org/apache/juneau/hjson/HjsonMediaType_Test.java`

- **g01_produces** -- `application/hjson`
- **g02_consumes** -- `application/hjson`, `application/hjson+json`
- **g03_contentNegotiation** -- Correct selection

### 8. Cross-Format Round-Trip Integration: `org/apache/juneau/a/rttests/RoundTripDateTime_Test.java`

Add a `RoundTrip_Tester` entry for this format to the `TESTERS` array in `RoundTripDateTime_Test.java`. This test verifies date/time and Duration round-trip across all serializer/parser combinations. It covers beans with `Instant`, `ZonedDateTime`, `LocalDate`, `LocalDateTime`, `LocalTime`, `OffsetDateTime`, `OffsetTime`, `Year`, `YearMonth`, `Calendar`, `Date`, `Duration`, and `XMLGregorianCalendar` fields.

---

## Documentation

### Javadoc

- **`HjsonSerializer.java`**: Hjson spec, comparison with JSON/JSON5, quoteless/multiline string rules, serialization decision logic, use cases (config files, human-edited data)
- **`HjsonParser.java`**: Parsing rules, quoteless string disambiguation, root braceless detection
- **`package-info.java`**: Full syntax summary, mapping rules, examples

### 3. Update Release Notes
/Users/james.bognar/git/apache/juneau/docs/pages/release-notes

### 4. Update Documentation

Find how existing languages are documented in the following location and update the documentation to match
the same level of detail.
/Users/james.bognar/git/apache/juneau/docs/pages/topics

---

---

## Javadoc Templates

The following templates define the class-level Javadoc to write for each of the three public-facing classes.
They follow the same structure as `Json.java`, `JsonSerializer.java`, and `JsonParser.java`.

### `Hjson.java` (Marshaller)

```java
/**
 * A pairing of a {@link HjsonSerializer} and {@link HjsonParser} into a single class with convenience read/write methods.
 *
 * <p>
 * 	The general idea is to combine a single serializer and parser inside a simplified API for reading and writing POJOs.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Using instance.</jc>
 * 	Hjson <jv>hjson</jv> = <jk>new</jk> Hjson();
 * 	MyPojo <jv>myPojo</jv> = <jv>hjson</jv>.read(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = <jv>hjson</jv>.write(<jv>myPojo</jv>);
 * </p>
 * <p class='bjava'>
 *	<jc>// Using DEFAULT instance.</jc>
 * 	MyPojo <jv>myPojo</jv> = Hjson.<jsf>DEFAULT</jsf>.read(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = Hjson.<jsf>DEFAULT</jsf>.write(<jv>myPojo</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output - readable mode (Map of name/age):</h5>
 * <p class='bjson'>
 * 	{
 * 	  name: Alice
 * 	  age: 30
 * 	}
 * </p>
 *
 * <h5 class='figure'>Example output - compact mode ({@link #DEFAULT_COMPACT}):</h5>
 * <p class='bjson'>
 * 	{name:Alice,age:30}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshallers">Marshallers</a>
 * 	<li class='link'><a class="doclink" href="https://hjson.github.io/syntax.html">Hjson Specification</a>
 * </ul>
 */
```

### `HjsonSerializer.java`

```java
/**
 * Serializes POJO models to Hjson (Human JSON).
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Accept</c> types:  <bc>application/hjson, application/hjson+json</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>application/hjson</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * Hjson is a syntax extension of JSON designed for human-friendly configuration files and hand-edited data.
 * This serializer produces output that is valid Hjson and can be parsed back by {@link HjsonParser}.
 *
 * <p>
 * The conversion is as follows...
 * <ul class='spaced-list'>
 * 	<li>
 * 		Maps (e.g. {@link HashMap HashMaps}, {@link TreeMap TreeMaps}) are converted to Hjson objects.
 * 	<li>
 * 		Collections (e.g. {@link HashSet HashSets}, {@link LinkedList LinkedLists}) and Java arrays are converted to
 * 		Hjson arrays.
 * 	<li>
 * 		{@link String Strings} are serialized as quoteless values when they contain only safe characters.
 * 		Strings that would be misinterpreted as booleans, null, or numbers are automatically double-quoted.
 * 	<li>
 * 		{@link Number Numbers} (e.g. {@link Integer}, {@link Long}, {@link Double}) are converted to Hjson numbers.
 * 	<li>
 * 		{@link Boolean Booleans} are converted to {@code true} or {@code false}.
 * 	<li>
 * 		{@code null} values are converted to Hjson {@code null}.
 * 	<li>
 * 		{@code beans} are converted to Hjson objects.
 * </ul>
 *
 * <p>
 * The types above are considered "Hjson-primitive" object types.
 * Any non-Hjson-primitive object types are transformed into Hjson-primitive object types through
 * {@link org.apache.juneau.swap.ObjectSwap ObjectSwaps} associated through the
 * {@link org.apache.juneau.BeanContext.Builder#swaps(Class...)} method.
 * Several default transforms are provided for transforming Dates, Enums, Iterators, etc.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Use one of the default serializers to serialize a POJO</jc>
 * 	String <jv>hjson</jv> = HjsonSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>someObject</jv>);
 *
 * 	<jc>// Create a compact serializer (comma-separated, no newlines between members)</jc>
 * 	HjsonSerializer <jv>compact</jv> = HjsonSerializer.<jsm>create</jsm>().compact().build();
 *
 * 	<jc>// Clone an existing serializer and enable multiline strings</jc>
 * 	<jv>compact</jv> = HjsonSerializer.<jsf>DEFAULT</jsf>.copy().useMultilineStrings(<jk>true</jk>).build();
 * </p>
 *
 * <h5 class='figure'>Example output - readable mode (Map of name/age):</h5>
 * <p class='bjson'>
 * 	{
 * 	  name: Alice
 * 	  age: 30
 * 	}
 * </p>
 *
 * <h5 class='figure'>Example output - compact mode:</h5>
 * <p class='bjson'>
 * 	{name:Alice,age:30}
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>String values equal to <c>true</c>, <c>false</c>, <c>null</c>, or parseable as numbers
 * 		are automatically serialized in double quotes to prevent parse ambiguity.
 * 	<li class='note'>Empty strings are always serialized as <c>""</c>.
 * 	<li class='note'>Root-level objects always include enclosing braces <c>{}</c> by default.
 * 		Use {@link Builder#omitRootBraces(boolean)} to suppress them.
 * 	<li class='note'>In compact mode, strings containing newlines are serialized as double-quoted strings
 * 		with JSON escape sequences rather than multiline <c>'''</c> blocks.
 * 	<li class='note'>Format-specific field-level annotations ({@link Hjson @Hjson}) are supported
 * 		but intentionally minimal in the initial implementation.
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HjsonBasics">Hjson Basics</a>
 * 	<li class='link'><a class="doclink" href="https://hjson.github.io/syntax.html">Hjson Specification</a>
 * </ul>
 */
```

### `HjsonParser.java`

```java
/**
 * Parses Hjson (Human JSON) text into POJO models.
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Content-Type</c> types:  <bc>application/hjson, application/hjson+json</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * This parser handles all standard Hjson syntax as defined by the
 * <a class="doclink" href="https://hjson.github.io/syntax.html">Hjson specification</a>.
 *
 * <p>
 * In addition to standard JSON, this parser handles the following:
 * <ul class='spaced-list'>
 * 	<li>
 * 		{@code #} single-line comments (to end of line).
 * 	<li>
 * 		{@code //} single-line comments.
 * 	<li>
 * 		{@code /* ... *}{@code /} block comments.
 * 	<li>
 * 		Quoteless string values (text read to end of line, leading/trailing whitespace trimmed).
 * 	<li>
 * 		Multiline strings delimited by {@code '''} (triple single-quote) with indentation stripping.
 * 	<li>
 * 		Optional commas — newlines may serve as member/element separators.
 * 	<li>
 * 		Trailing commas (ignored).
 * 	<li>
 * 		Unquoted keys.
 * 	<li>
 * 		Root-braceless objects (top-level content without wrapping {@code {}}).
 * </ul>
 *
 * <p>
 * Value type disambiguation: the parser identifies value types in this order:
 * number → boolean → null → quoteless string.
 * A value such as {@code 5 times} that starts like a number but contains non-numeric
 * characters is treated as a quoteless string.
 *
 * <p>
 * This parser handles the following input and returns the corresponding Java type:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Hjson/JSON objects ({@code {...}}) are converted to {@link JsonMap JsonMaps}.
 * 		<b>Note:</b> If a <code><xa>_type</xa>=<xs>'xxx'</xs></code> attribute is specified on the object,
 * 		an attempt is made to convert the object to an instance of the specified Java bean class.
 * 	<li>
 * 		Hjson/JSON arrays ({@code [...]}) are converted to {@link JsonList JsonLists}.
 * 	<li>
 * 		Quoted strings ({@code "..."} or {@code '...'}) are converted to {@link String Strings}.
 * 	<li>
 * 		Multiline strings ({@code '''...'''}) are converted to {@link String Strings}.
 * 	<li>
 * 		Quoteless strings are converted to {@link String Strings}.
 * 	<li>
 * 		Numbers are converted to {@link Integer Integers}, {@link Long Longs},
 * 		{@link Float Floats}, or {@link Double Doubles} depending on size and format.
 * 	<li>
 * 		{@code true}/{@code false} are converted to {@link Boolean Booleans}.
 * 	<li>
 * 		{@code null} returns <jk>null</jk>.
 * 	<li>
 * 		Input consisting of only whitespace or comments returns <jk>null</jk>.
 * </ul>
 *
 * <h5 class='figure'>Example input - readable mode:</h5>
 * <p class='bjson'>
 * 	{
 * 	  name: Alice
 * 	  age: 30
 * 	}
 * </p>
 *
 * <h5 class='figure'>Example input - compact mode:</h5>
 * <p class='bjson'>
 * 	{name:Alice,age:30}
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>Root-braceless Hjson files (content without a wrapping {@code {}}) are supported on input,
 * 		but {@link HjsonSerializer} always emits root braces by default.
 * 		A parse-then-serialize round-trip of a root-braceless file will produce braced output.
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HjsonBasics">Hjson Basics</a>
 * 	<li class='link'><a class="doclink" href="https://hjson.github.io/syntax.html">Hjson Specification</a>
 * </ul>
 */
```

---

## Implementation Order

1. **`HjsonTokenizer.java`** -- Foundation
2. **`HjsonWriter.java`** -- Formatting
3. **`HjsonSerializer.java`** + **`HjsonSerializerSession.java`**
4. **`HjsonParser.java`** + **`HjsonParserSession.java`**
5. **Tokenizer tests** (12 test cases)
6. **Serializer tests** (22 test cases)
7. **Parser tests** (22 test cases)
8. **Round-trip tests** (12 test cases)
9. **Metadata and annotations**
10. **Marshaller** + tests
11. **Edge case and media type tests**
12. **REST integration** (`BasicUniversalConfig`, `BasicHjsonConfig`, `RestClient`)
13. **Context registration** (`HjsonConfig`, `HjsonConfigAnnotation`, `Context.java`)

---

## Key Design Decisions

### 1. Independent serializer/parser (not extending JsonSerializer)

Hjson's quoteless strings and multiline strings require fundamentally different tokenizing logic. While JSON5 could extend JsonParser (same quoted-string tokenizing), Hjson's tokenizer must handle "everything else is a string" semantics that don't fit the JSON parser's state machine.

### 2. Quoteless strings are the default serialization

In readable mode, simple string values are serialized without quotes (matching Hjson's human-friendly philosophy). Quotes are added only when needed for disambiguation or special characters.

### 3. Multiline strings for strings containing newlines

When `useMultilineStrings` is true (default), strings containing `\n` are serialized using `'''` syntax rather than `"..."` with `\n` escapes. This produces more readable output.

### 4. Newlines as separators (default)

In readable mode, members are separated by newlines rather than commas, matching Hjson convention. Commas are used in compact mode.

### 5. Root braces included by default in serializer output

Although Hjson allows omitting root braces, the serializer includes them by default for unambiguous output. The `omitRootBraces` option enables the braceless root style.

### 6. String disambiguation in parser

The parser determines value type by attempting interpretations in order: number → boolean → null → quoteless string. If a value starts like a number but contains non-numeric characters (e.g., `5 times`), it falls back to quoteless string.

---

## Round-Trip Analysis and Limitations

### Automatic String Disambiguation (handled transparently — not a limitation)

The serializer automatically detects and quotes strings that would be misinterpreted on parse:
- Strings equal to `true`, `false`, `null` → serialized as `"true"`, `"false"`, `"null"` (double-quoted)
- Strings parseable as numbers (e.g. `"42"`, `"3.14"`) → serialized as `"42"`, `"3.14"` (double-quoted)
- Empty strings → serialized as `""` (must be quoted; quoteless has no empty form)

The parser resolves this symmetrically: any double-quoted or single-quoted string is always a `String`, never a boolean or number. Round-trip fidelity is preserved for all string values.

### Documented Limitations

These are true limitations that must be documented in the `Notes:` section of each class's Javadoc:

1. **Root-braceless input (parser only)**: The parser can parse Hjson files without a wrapping `{}`, but the serializer always emits root braces (unless `omitRootBraces=true`). A parse → serialize round-trip of a root-braceless file will produce braced output. This should be noted in both `HjsonParser` and `HjsonSerializer` Javadoc.

2. **Newlines in compact mode**: In compact mode (`DEFAULT_COMPACT`), strings containing `\n` are serialized as double-quoted `"..."` with JSON escape sequences, not as `'''` multiline blocks. The parser handles both forms, so the round-trip is lossless, but compact output is not human-readable for multi-line strings.

3. **`_type` polymorphic attribute**: The `_type` key/value pair (used by Juneau for polymorphic bean deserialization) serializes with a quoteless key (`_type`) and a quoteless class name value (e.g. `_type: com.example.Foo`). The parser reads the class name as a quoteless string and Juneau resolves the type correctly. No special handling is needed; this is noted here for completeness.

4. **No format-specific field-level annotations**: Unlike XML (which has `@Xml`) or JSON (which has `@Json`), the initial Hjson implementation defines `@Hjson` and `@HjsonConfig` annotations but they are intentionally minimal. All field-level serialization uses standard bean introspection. This should be documented in the `HjsonSerializer` Javadoc `Notes:` section.

### 7. Date/Time and Duration Support

Date/time types (`Date`, `Calendar`, `Temporal` subtypes) and `Duration` are built-in first-class types. Hjson's `serializeAnything()` should include `sType.isDateOrCalendarOrTemporal()` and `sType.isDuration()` dispatch checks (before `isBean()`), formatting values as ISO 8601 strings via `Iso8601Utils.format()`. The parser should use `Iso8601Utils.parse()` to convert ISO 8601 strings back to target types. See `builtin-datetime-iso8601.md` for the full architecture.

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
