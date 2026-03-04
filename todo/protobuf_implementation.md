# Protocol Buffers Text Format Support Implementation Plan for Apache Juneau

## Overview

This plan covers implementing serialization and parsing support for the Protocol Buffers Text Format in Juneau. This is the **text format** (human-readable, used for protobuf configurations and debugging), NOT the binary wire format. The text format is schema-less in Juneau's usage -- Java bean metadata provides the type information that `.proto` files would normally supply.

The protobuf text format is widely recognized in the gRPC ecosystem and provides a clean, human-readable representation of structured data that is syntactically distinct from JSON (no commas between fields, no quotes on field names, optional colons before messages).

**Lossless round-trip marshalling** is a primary goal. Every data structure supported by the protobuf text format serializer must produce output that, when parsed back, yields an object equal to the original. This is equivalent to the JSON round-trip guarantee. See the "JSON Data Structure Parity and Limitations" section for the narrow set of cases where full JSON parity is not achievable.

## Specification

- **Text Format Spec**: https://protobuf.dev/reference/protobuf/textformat-spec
- **Media type**: `text/protobuf`, `text/x-protobuf`
- **Encoding**: UTF-8

## Protobuf Text Format Syntax

```
name: "John"
age: 42
active: true
address {
  city: "Springfield"
  state: "IL"
}
hobbies: ["reading", "gaming", "hiking"]
```

Key syntax rules:
- **Field names**: Bare identifiers (no quotes)
- **Scalar fields**: `field_name: value` (colon required)
- **Message fields**: `field_name { ... }` or `field_name: { ... }` (colon optional)
- **Strings**: Double-quoted `"..."` or single-quoted `'...'` with C-style escaping
- **Numbers**: Integers (decimal, hex `0x`, octal `0`), floats
- **Booleans**: `true`/`false`
- **No null**: Absent fields represent unset values
- **Collections**: List syntax `[val1, val2, val3]` or repeated field names
- **Comments**: `# comment to end of line`
- **Field separators**: Newlines, semicolons, or commas (all optional between fields)
- **No trailing commas required**: Unlike JSON

---

## Bean-to-Protobuf Text Format Mapping

### Mapping Rules

**1. Simple bean properties -> scalar fields:**

```java
class Server { String host; int port; boolean debug; double ratio; }
```
```
host: "localhost"
port: 8080
debug: true
ratio: 3.14
```

**2. Nested bean properties -> message fields:**

```java
class Config { String name; Database db; }
class Database { String host; int port; }
```
```
name: "myapp"
db {
  host: "localhost"
  port: 5432
}
```

No colon before the opening brace for message fields (following protobuf convention). The serializer omits it for message values and includes it for scalar values.

**3. Deeply nested beans -> nested message fields:**

```java
class Config { Server server; }
class Server { String host; Ssl ssl; }
class Ssl { boolean enabled; String cert; }
```
```
server {
  host: "localhost"
  ssl {
    enabled: true
    cert: "/path/to/cert.pem"
  }
}
```

**4. Collections of simple values -> list syntax:**

```java
class Config { List<String> tags; List<Integer> ports; }
```
```
tags: ["web", "api", "rest"]
ports: [8080, 8443, 9090]
```

**5. Collections of beans -> repeated message fields:**

```java
class Config { List<Server> servers; }
class Server { String host; int port; }
```
```
servers {
  host: "alpha"
  port: 8080
}
servers {
  host: "beta"
  port: 8081
}
```

Repeated message fields follow protobuf convention: the field name is repeated for each element (not list syntax, which is for scalars only).

**6. Maps -> message fields:**

```java
class Config { Map<String,String> env; }
```
```
env {
  PATH: "/usr/bin"
  HOME: "/home/user"
}
```

Map keys that are valid identifiers use bare syntax. Keys requiring quoting use quoted key syntax.

**7. Null handling:**

Protobuf text format has no null representation. Null properties are omitted. During parsing, missing fields leave bean properties at their Java defaults.

**8. Enum values:**

```java
class Config { LogLevel level; }
enum LogLevel { DEBUG, INFO, WARN, ERROR }
```
```
level: INFO
```

Enum values are serialized as unquoted identifiers (matching protobuf convention).

**9. String escaping:**

Protobuf text format uses C-style escape sequences: `\"`, `\\`, `\n`, `\t`, `\r`, `\b`, `\f`, `\'`, `\a`, `\v`, `\xHH` (hex byte), `\OOO` (octal byte), `\uHHHH` (Unicode), `\UHHHHHHHH` (extended Unicode).

**10. Type mappings:**

| Java Type | Protobuf Text Format | Example |
|-----------|---------------------|---------|
| `String` | Quoted string | `name: "John"` |
| `int`, `long` | Decimal integer | `count: 42` |
| `float`, `double` | Float literal | `ratio: 3.14` |
| `boolean` | `true`/`false` | `active: true` |
| `Enum` | Identifier (unquoted) | `level: INFO` |
| `null` | (omitted) | Field absent |
| Nested bean | Message `{ ... }` | `addr { city: "NYC" }` |
| `Map<String,V>` | Message `{ ... }` | `env { KEY: "val" }` |
| `List<simple>` | List `[v1, v2]` | `tags: ["a", "b"]` |
| `List<bean>` | Repeated fields | Multiple `name { ... }` |
| `byte[]` | Quoted string (escaped bytes) | `data: "\x0a\x05"` |

---

## JSON Data Structure Parity and Limitations

The protobuf text format supports the same data structures as JSON with the following mapping:

### Fully Supported (Lossless Round-Trip)

| JSON structure | Protobuf text equivalent | Lossless? |
|---|---|---|
| `String` | Quoted string | Yes |
| `int`/`long`/`float`/`double` | Numeric literal | Yes (with typed target) |
| `boolean` | `true`/`false` | Yes |
| Beans/Maps (objects) | Message fields `{ }` | Yes |
| `List<String>` / `List<Number>` | List syntax `[v1, v2]` | Yes |
| `List<Bean>` | Repeated field names | Yes |
| `byte[]` | C-style escaped bytes in string | Yes |
| `Enum` | Unquoted identifier | Yes |
| `Date`/`Calendar`/`Temporal` subtypes | ISO 8601 string | Yes (via swap) |
| `Duration` | ISO 8601 duration string | Yes (via swap) |
| `Optional<T>` | Same as T or omitted | Yes |

### Limitations Compared to JSON

- **No native null representation** — Null properties are omitted during serialization. On parsing, missing fields revert to Java defaults. For reference types (`String`, boxed types), the Java default is `null`, so this is lossless. For primitives (`int`, `boolean`), null is not possible in Java, so there is no loss.
- **No top-level scalars** — Protobuf text format requires a message (object) at the top level. Serializing a bare `String`, `int`, or `boolean` as the root object wraps it in a synthetic single-field message. This is a limitation vs. JSON, which supports any value at the root.
- **Untyped parsing loses numeric precision** — When parsing into `JsonMap`/`JsonList` (untyped), integer vs. long vs. double is inferred from the literal. With typed targets (bean classes), this is fully lossless.
- **Map keys must be strings** — JSON supports any string key including those with special characters. Protobuf text format uses bare identifiers for valid keys and quoted strings for others. Lossless as long as quoted key syntax is used for non-identifier keys.
- **Angle-bracket messages** (`< >`) are accepted on parse but are never emitted on serialize.

---

## Architecture

The protobuf text format is a text-based format with unique syntax (not JSON), so it extends `WriterSerializer`/`ReaderParser` directly and has its own tokenizer.

```
WriterSerializer
  └── ProtoSerializer
        └── ProtoSerializerSession (uses ProtoWriter)

ReaderParser
  └── ProtoParser
        └── ProtoParserSession (uses ProtoTokenizer)
```

---

## Files to Create

All source files in `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/proto/`.

### 1. `ProtoSerializer.java`

Extends `WriterSerializer`. Implements `ProtoMetaProvider`.

Class-level javadoc (matching `JsonSerializer` style):

```java
/**
 * Serializes POJO models to Protobuf Text Format.
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Accept</c> types:  <bc>text/protobuf, text/x-protobuf</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>text/protobuf</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * Implements the <a class="doclink" href="https://protobuf.dev/reference/protobuf/textformat-spec">Protobuf Text Format</a>
 * serializer.  This is the human-readable text format used for protobuf configuration files and debugging,
 * NOT the binary wire format.
 * <p>
 * The conversion is as follows...
 * <ul class='spaced-list'>
 * 	<li>
 * 		Maps (e.g. {@link HashMap}, {@link TreeMap}) and beans are converted to protobuf message fields.
 * 	<li>
 * 		Collections (e.g. {@link HashSet}, {@link LinkedList}) and Java arrays of simple values are converted to
 * 		protobuf list syntax {@code [v1, v2, ...]}.
 * 	<li>
 * 		Collections of beans are converted to repeated field names.
 * 	<li>
 * 		{@link String Strings} are converted to quoted strings with C-style escaping.
 * 	<li>
 * 		{@link Number Numbers} (e.g. {@link Integer}, {@link Long}, {@link Double}) are converted to numeric literals.
 * 	<li>
 * 		{@link Boolean Booleans} are converted to {@code true}/{@code false}.
 * 	<li>
 * 		Enum values are converted to unquoted identifiers.
 * 	<li>
 * 		{@code null} properties are omitted (protobuf text format has no null representation).
 * 	<li>
 * 		{@code byte[]} is converted to a quoted string using C-style hex escaping.
 * </ul>
 *
 * <p>
 * Date/time types ({@code Calendar}, {@code Date}, {@code Temporal} subtypes, {@code Duration}) are serialized as
 * ISO 8601 strings.  Swaps can be used to convert non-serializable POJOs into serializable forms or to override
 * the default format for types that already have built-in support.
 *
 * <h5 class='topic'>Behavior-specific subclasses</h5>
 * <p>
 * The following direct subclasses are provided for convenience:
 * <ul class='spaced-list'>
 * 	<li>
 * 		{@link ProtoSerializer.Readable} - Default serializer with additional indentation spacing.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Use one of the default serializers to serialize a POJO</jc>
 * 	String <jv>proto</jv> = ProtoSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>someObject</jv>);
 *
 * 	<jc>// Create a custom serializer</jc>
 * 	ProtoSerializer <jv>serializer</jv> = ProtoSerializer.<jsm>create</jsm>().addBeanTypes().build();
 *
 * 	<jc>// Clone an existing serializer and modify it</jc>
 * 	<jv>serializer</jv> = ProtoSerializer.<jsf>DEFAULT</jsf>.copy().useColonForMessages().build();
 *
 * 	<jc>// Serialize a POJO to Protobuf Text Format</jc>
 * 	String <jv>proto</jv> = <jv>serializer</jv>.serialize(<jv>someObject</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (Map of name/age):</h5>
 * <p class='bcode'>
 * 	name: "Alice"
 * 	age: 30
 * </p>
 *
 * <h5 class='figure'>Complex (nested object + array):</h5>
 * <p class='bcode'>
 * 	name: "Alice"
 * 	age: 30
 * 	address {
 * 	  street: "123 Main St"
 * 	  city: "Boston"
 * 	  state: "MA"
 * 	}
 * 	tags: ["a", "b", "c"]
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ProtobufBasics">Protobuf Text Format Basics</a>
 * 	<li class='link'><a class="doclink" href="https://protobuf.dev/reference/protobuf/textformat-spec">Protobuf Text Format Specification</a>
 * </ul>
 */
```

Class structure:

```java
package org.apache.juneau.proto;

public class ProtoSerializer extends WriterSerializer implements ProtoMetaProvider {

    public static final ProtoSerializer DEFAULT = ...;
    public static final ProtoSerializer DEFAULT_READABLE = ...; // with extra spacing

    public static class Builder extends WriterSerializer.Builder {
        boolean useListSyntaxForBeans = false; // Use [...] for bean collections too
        boolean useColonForMessages = false;   // Use ":" before { for message fields

        protected Builder() {
            produces("text/protobuf");
            accept("text/protobuf,text/x-protobuf");
        }
    }

    @Override
    public ProtoSerializerSession.Builder createSession() {
        return ProtoSerializerSession.create(this);
    }
}
```

Builder options:
- `useListSyntaxForBeans(boolean)` -- Use `[{...}, {...}]` list syntax for bean collections instead of repeated field names (non-standard but more compact; default: false)
- `useColonForMessages(boolean)` -- Include colon before message values (`field: { ... }` vs `field { ... }`; default: false, matching protobuf convention)

### 2. `ProtoSerializerSession.java`

Extends `WriterSerializerSession`. Core serialization logic.

Key methods:
- `doSerialize(SerializerPipe, Object)` -- Entry point
- `serializeAnything(ProtoWriter, Object, ClassMeta, String, BeanPropertyMeta)` -- Central dispatch
- `serializeBeanMap(ProtoWriter, BeanMap, String)` -- Serializes bean properties as fields
- `serializeMap(ProtoWriter, Map, ClassMeta)` -- Serializes map entries as fields
- `serializeCollection(ProtoWriter, Collection, ClassMeta, String)` -- Renders as list syntax or repeated fields
- `serializeScalarValue(ProtoWriter, Object, ClassMeta)` -- Writes typed scalar value

Serialization algorithm:
```
serializeBeanMap(writer, beanMap, typeName):
    for each (key, value) in beanMap:
        if value is null: continue (omit)

        if value is simple (string, number, boolean, enum, date/calendar/temporal, duration):
            writer.fieldName(key)
            writer.w(": ")
            serializeScalarValue(writer, value, classMeta)
            writer.newLine()

        else if value is bean:
            writer.fieldName(key)
            writer.w(" {")
            writer.newLine()
            indent++
            serializeBeanMap(writer, toBeanMap(value))
            indent--
            writer.indent().w("}")
            writer.newLine()

        else if value is map:
            writer.fieldName(key)
            writer.w(" {")
            writer.newLine()
            indent++
            serializeMap(writer, value, classMeta)
            indent--
            writer.indent().w("}")
            writer.newLine()

        else if value is collection of simple values:
            writer.fieldName(key)
            writer.w(": [")
            // comma-separated values
            writer.w("]")
            writer.newLine()

        else if value is collection of beans:
            for each item in collection:
                writer.fieldName(key)
                writer.w(" {")
                writer.newLine()
                indent++
                serializeBeanMap(writer, toBeanMap(item))
                indent--
                writer.indent().w("}")
                writer.newLine()
```

### 3. `ProtoWriter.java`

Extends `SerializerWriter`. Provides protobuf text format-specific write methods.

```java
package org.apache.juneau.proto;

public class ProtoWriter extends SerializerWriter {

    // Field writing
    ProtoWriter fieldName(String name);           // Writes bare identifier or quoted key
    ProtoWriter scalarField(String name, Object value); // name: value\n
    ProtoWriter messageStart(String name);        // name {\n
    ProtoWriter messageEnd();                     // }\n

    // Value writing
    ProtoWriter stringValue(String value);        // "value" with C-style escaping
    ProtoWriter integerValue(long value);          // 42
    ProtoWriter floatValue(double value);          // 3.14 (handles inf, nan)
    ProtoWriter booleanValue(boolean value);       // true/false
    ProtoWriter enumValue(String name);            // IDENTIFIER (unquoted)
    ProtoWriter bytesValue(byte[] data);           // "\x0a\x05..."

    // List writing
    ProtoWriter listStart();                       // [
    ProtoWriter listEnd();                         // ]

    // Formatting
    ProtoWriter comment(String text);              // # text
    ProtoWriter indent();                          // Write current indentation

    // Key encoding
    boolean isBareIdentifier(String name);         // Can be written without quotes
    String escapeString(String text);              // C-style escape sequences
}
```

Key encoding rules:
- Bare identifiers: `A-Za-z_` followed by `A-Za-z0-9_` (protobuf IDENT rule)
- Non-identifier keys (from maps with special-character keys) are quoted: `"my.key": value`

### 4. `ProtoParser.java`

Extends `ReaderParser`. Implements `ProtoMetaProvider`.

Class-level javadoc (matching `JsonParser` style):

```java
/**
 * Parses Protobuf Text Format into a POJO model.
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Content-Type</c> types:  <bc>text/protobuf, text/x-protobuf</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * This parser uses a state machine with a tokenizer, making it efficient for parsing protobuf
 * text format without requiring an intermediate DOM representation.
 *
 * <p>
 * Handles all valid Protobuf Text Format syntax including:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Message fields using brace delimiters ({@code field { ... }}).
 * 	<li>
 * 		Scalar fields using colon syntax ({@code field: value}).
 * 	<li>
 * 		List syntax ({@code field: [v1, v2]}) for collections of simple values.
 * 	<li>
 * 		Repeated field names aggregated into collections of beans.
 * 	<li>
 * 		Integers: decimal, hex ({@code 0x...}), octal ({@code 0...}).
 * 	<li>
 * 		Floats including special values {@code inf}, {@code -inf}, and {@code nan}.
 * 	<li>
 * 		Single and double quoted strings with C-style escape sequences ({@code \n}, {@code \t},
 * 		{@code \\}, {@code \"}, {@code \xHH}, {@code \OOO}, {@code \uHHHH}).
 * 	<li>
 * 		Multi-part adjacent string concatenation (e.g. {@code "hello" "world"}).
 * 	<li>
 * 		Angle-bracket message delimiters ({@code < >}) as an alternative to braces.
 * 	<li>
 * 		Comments ({@code # ...} to end of line).
 * 	<li>
 * 		Optional field separators: newlines, semicolons, or commas.
 * </ul>
 *
 * <p>
 * This parser handles the following input and automatically returns the corresponding Java class:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Protobuf messages are converted to {@link JsonMap JsonMaps}.
 * 		<b>Note:</b> If a <code><xa>_type</xa>=<xs>'xxx'</xs></code> field is present, then an
 * 		attempt is made to convert the message to an instance of the specified Java bean class.
 * 	<li>
 * 		List fields are converted to {@link JsonList JsonLists}.
 * 	<li>
 * 		Quoted strings are converted to {@link String Strings}.
 * 	<li>
 * 		Numeric literals are converted to {@link Integer Integers}, {@link Long Longs},
 * 		or {@link Double Doubles} depending on size and presence of a decimal point.
 * 	<li>
 * 		Booleans ({@code true}/{@code false}) are converted to {@link Boolean Booleans}.
 * 	<li>
 * 		Unquoted identifiers are converted to {@link String Strings} or enum constants
 * 		depending on the target property type.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Use the default parser to parse into a bean</jc>
 * 	MyBean <jv>bean</jv> = ProtoParser.<jsf>DEFAULT</jsf>.parse(<jv>input</jv>, MyBean.<jk>class</jk>);
 *
 * 	<jc>// Parse into an untyped map</jc>
 * 	JsonMap <jv>map</jv> = ProtoParser.<jsf>DEFAULT</jsf>.parse(<jv>input</jv>, JsonMap.<jk>class</jk>);
 *
 * 	<jc>// Create a strict-mode parser</jc>
 * 	ProtoParser <jv>parser</jv> = ProtoParser.<jsm>create</jsm>().strict().build();
 * 	MyBean <jv>bean</jv> = <jv>parser</jv>.parse(<jv>input</jv>, MyBean.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ProtobufBasics">Protobuf Text Format Basics</a>
 * 	<li class='link'><a class="doclink" href="https://protobuf.dev/reference/protobuf/textformat-spec">Protobuf Text Format Specification</a>
 * </ul>
 */
```

Class structure:

```java
package org.apache.juneau.proto;

public class ProtoParser extends ReaderParser implements ProtoMetaProvider {

    public static final ProtoParser DEFAULT = ...;

    public static class Builder extends ReaderParser.Builder {
        protected Builder() {
            consumes("text/protobuf,text/x-protobuf");
        }
    }

    @Override
    public ProtoParserSession.Builder createSession() {
        return ProtoParserSession.create(this);
    }
}
```

### 5. `ProtoParserSession.java`

Extends `ReaderParserSession`. Core parsing logic.

Parsing strategy -- two-phase (same as TOML/INI):
1. Tokenize and build intermediate `Map<String, Object>` structure
2. Populate target bean from intermediate structure

Key methods:
- `doParse(ParserPipe, ClassMeta)` -- Entry point
- `parseMessage(ProtoTokenizer)` -- Parses a message (between `{` and `}` or at top level) into a `Map<String, Object>`
- `parseValue(ProtoTokenizer, ClassMeta)` -- Parses a scalar or message value
- `parseList(ProtoTokenizer)` -- Parses `[val1, val2, ...]`
- `populateBean(Map, BeanMap)` -- Maps intermediate structure to bean properties

Parsing algorithm:
```
parseMessage(tokenizer):
    result = new LinkedHashMap()
    while not EOF and not "}":
        fieldName = readFieldName()

        if next token is ":":
            consume ":"
            if next token is "[":
                value = parseList()
            else:
                value = parseScalarValue()
        else if next token is "{":
            value = parseMessage()  // nested message

        // Handle repeated fields (same name appears multiple times)
        if result contains fieldName:
            existing = result.get(fieldName)
            if existing is not List:
                existing = new ArrayList(existing)
                result.put(fieldName, existing)
            existing.add(value)
        else:
            result.put(fieldName, value)

        // Consume optional separator (;, comma, or newline)
    return result
```

Scalar values (strings, numbers, booleans, identifiers) are parsed by `parseScalarValue()`. Date/time strings in ISO 8601 format are converted to target types (Date, Calendar, Temporal subtypes) via `Iso8601Utils.parse()` and `BeanSession.convertToMemberType()`. Duration strings are similarly converted for Duration-typed properties.

### 6. `ProtoTokenizer.java`

Internal tokenizer for protobuf text format.

```java
package org.apache.juneau.proto;

class ProtoTokenizer {
    ProtoTokenizer(Reader reader);

    Token peek();                    // Look ahead without consuming
    Token read();                    // Read and consume next token
    void skipWhitespaceAndComments();

    // Token types
    enum TokenType {
        IDENT,        // bare_identifier
        STRING,       // "quoted" or 'quoted'
        DEC_INT,      // 42, -17
        OCT_INT,      // 0755
        HEX_INT,      // 0xDEAD
        FLOAT,        // 3.14, 1e06, 10f
        LBRACE,       // {
        RBRACE,       // }
        LANGLE,       // <
        RANGLE,       // >
        LBRACKET,     // [
        RBRACKET,     // ]
        COLON,        // :
        COMMA,        // ,
        SEMICOLON,    // ;
        EOF
    }

    // Value reading
    String readIdentifier();         // Bare identifier
    String readString();             // Quoted string with unescaping
    Number readInteger();            // Decimal, hex, or octal integer
    double readFloat();              // Float including inf, nan
    boolean readBoolean();           // true/false

    int getLine();                   // For error messages
    int getColumn();
}
```

### 7. `ProtoMetaProvider.java`

```java
package org.apache.juneau.proto;

public interface ProtoMetaProvider {
    ProtoClassMeta getProtoClassMeta(ClassMeta<?> cm);
    ProtoBeanPropertyMeta getProtoBeanPropertyMeta(BeanPropertyMeta bpm);
}
```

### 8. `ProtoClassMeta.java` and `ProtoBeanPropertyMeta.java`

Extend `ExtendedClassMeta` and `ExtendedBeanPropertyMeta`.

`ProtoBeanPropertyMeta` fields:
- `comment` (String) -- Comment to emit before this field

### 9. `annotation/Proto.java`

```java
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
public @interface Proto {
    String comment() default "";       // Comment before field
    String on() default "";
    String[] onClass() default {};
}
```

### 10. `annotation/ProtoConfig.java`

```java
@Target({TYPE, METHOD})
@Retention(RUNTIME)
public @interface ProtoConfig {
    String useListSyntaxForBeans() default "";
    String useColonForMessages() default "";
    int rank() default 0;
}
```

### 11. `annotation/ProtoAnnotation.java` and `annotation/ProtoConfigAnnotation.java`

Utility classes for programmatic annotation creation.

### 12. `Proto.java` (Marshaller)

Location: `src/main/java/org/apache/juneau/marshaller/Proto.java`

Extends `CharMarshaller`.

Class-level javadoc (matching `Json.java` style):

```java
/**
 * A pairing of a {@link ProtoSerializer} and {@link ProtoParser} into a single class with convenience read/write methods.
 *
 * <p>
 * 	The general idea is to combine a single serializer and parser inside a simplified API for reading and writing POJOs.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Using instance.</jc>
 * 	Proto <jv>proto</jv> = <jk>new</jk> Proto();
 * 	MyPojo <jv>myPojo</jv> = <jv>proto</jv>.read(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = <jv>proto</jv>.write(<jv>myPojo</jv>);
 * </p>
 * <p class='bjava'>
 *	<jc>// Using DEFAULT instance.</jc>
 * 	MyPojo <jv>myPojo</jv> = Proto.<jsf>DEFAULT</jsf>.read(<jv>string</jv>, MyPojo.<jk>class</jk>);
 * 	String <jv>string</jv> = Proto.<jsf>DEFAULT</jsf>.write(<jv>myPojo</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (Map of name/age):</h5>
 * <p class='bcode'>
 * 	name: "Alice"
 * 	age: 30
 * </p>
 *
 * <h5 class='figure'>Complex (nested object + array):</h5>
 * <p class='bcode'>
 * 	name: "Alice"
 * 	age: 30
 * 	address {
 * 	  street: "123 Main St"
 * 	  city: "Boston"
 * 	  state: "MA"
 * 	}
 * 	tags: ["a", "b", "c"]
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshallers">Marshallers</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ProtobufBasics">Protobuf Text Format Basics</a>
 * </ul>
 */
```

Class structure:

```java
package org.apache.juneau.marshaller;

public class Proto extends CharMarshaller {
    public static final Proto DEFAULT = new Proto();

    public static String of(Object object) throws SerializeException { ... }
    public static Object of(Object object, Object output) throws SerializeException, IOException { ... }
    public static <T> T to(Object input, Class<T> type) throws ParseException, IOException { ... }
    public static <T> T to(Object input, Type type, Type...args) throws ParseException, IOException { ... }
    public static <T> T to(String input, Class<T> type) throws ParseException { ... }
    public static <T> T to(String input, Type type, Type...args) throws ParseException { ... }

    public Proto() { this(ProtoSerializer.DEFAULT, ProtoParser.DEFAULT); }
    public Proto(ProtoSerializer s, ProtoParser p) { super(s, p); }
}
```

Javadoc for `of(Object)` static method (matching `Json.of` style):

```java
/**
 * Serializes a Java object to a Protobuf Text Format string.
 *
 * <p>
 * A shortcut for calling <c><jsf>DEFAULT</jsf>.write(<jv>object</jv>)</c>.
 *
 * @param object The object to serialize.
 * @return The serialized object.
 * @throws SerializeException If a problem occurred trying to convert the output.
 */
```

Javadoc for `to(String, Class)` static method:

```java
/**
 * Parses a Protobuf Text Format string to the specified type.
 *
 * <p>
 * A shortcut for calling <c><jsf>DEFAULT</jsf>.read(<jv>input</jv>, <jv>type</jv>)</c>.
 *
 * @param <T> The class type of the object being created.
 * @param input The input.
 * @param type The object type to create.
 * @return The parsed object.
 * @throws ParseException Malformed input encountered.
 */
```

### 13. `package-info.java`

Package-level Javadoc.

---

## Files to Modify

### 1. `BasicUniversalConfig.java`

Add `ProtoSerializer.class` to `serializers` and `ProtoParser.class` to `parsers`.

### 2. `BasicProtoConfig.java` (New)

```java
@Rest(
    serializers={ProtoSerializer.class},
    parsers={ProtoParser.class},
    defaultAccept="text/protobuf"
)
public interface BasicProtoConfig extends DefaultConfig {}
```

### 3. `RestClient.java`

Add `ProtoSerializer.class` and `ProtoParser.class` to the `universal()` method.

### 4. Context: `Context.java`

Path: `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/Context.java`

Add `import org.apache.juneau.proto.annotation.*;` and add `{@link ProtoConfig}` to the list of config annotations in the `applyAnnotations()` Javadoc (alphabetically).

---

## Test Plan

All test files in `juneau-utest/src/test/java/`.

### 1. Serializer Tests: `org/apache/juneau/proto/ProtoSerializer_Test.java`

- **a01_simpleBean** -- String, int, boolean, float properties serialize as scalar fields
- **a02_nestedBean** -- Nested bean serializes as message field with braces
- **a03_deeplyNestedBean** -- 3+ levels of nesting
- **a04_collectionOfStrings** -- `List<String>` serializes as list syntax `["a", "b"]`
- **a05_collectionOfIntegers** -- `List<Integer>` serializes as list syntax `[1, 2, 3]`
- **a06_collectionOfBeans** -- `List<Bean>` serializes as repeated message fields
- **a07_mapProperty** -- `Map<String,String>` serializes as message field
- **a08_nullValues** -- Null properties omitted
- **a09_booleanValues** -- `true`/`false` lowercase
- **a10_floatValues** -- Proper float formatting (handles `inf`, `nan`)
- **a11_stringEscaping** -- C-style escaping (`\n`, `\t`, `\\`, `\"`)
- **a12_enumValues** -- Enums as unquoted identifiers
- **a13_emptyBean** -- Empty bean produces empty output
- **a14_emptyCollections** -- Empty list produces `field_name: []`
- **a15_objectSwaps** -- Swap transformations before encoding
- **a16_noColonBeforeMessages** -- Message fields use `name { ... }` not `name: { ... }`
- **a17_indentation** -- Proper indentation at each nesting level
- **a18_typeName** -- `_type` property when `addBeanTypes` enabled
- **a19_specialMapKeys** -- Map keys with dots/spaces use quoted key syntax
- **a20_binaryData** -- `byte[]` as hex-escaped string

### 2. Parser Tests: `org/apache/juneau/proto/ProtoParser_Test.java`

- **b01_parseSimpleBean** -- Scalar fields to bean properties
- **b02_parseNestedBean** -- Message field `{ ... }` to nested bean
- **b03_parseDeeplyNestedBean** -- Multi-level nesting
- **b04_parseListOfStrings** -- `["a", "b"]` to `List<String>`
- **b05_parseListOfIntegers** -- `[1, 2, 3]` to `List<Integer>`
- **b06_parseRepeatedMessages** -- Repeated field names to `List<Bean>`
- **b07_parseMapProperty** -- Message field to `Map<String,String>`
- **b08_parseBooleans** -- `true`/`false` to boolean
- **b09_parseIntegers** -- Decimal, hex (`0x`), octal (`0`) integers
- **b10_parseFloats** -- Float literals, `inf`, `-inf`, `nan`
- **b11_parseStrings** -- Double-quoted and single-quoted with escaping
- **b12_parseMultiPartStrings** -- Adjacent strings concatenated (`"hello" "world"`)
- **b13_parseComments** -- `#` comments ignored
- **b14_parseSemicolonSeparators** -- Fields separated by `;`
- **b15_parseCommaSeparators** -- Fields separated by `,`
- **b16_parseMissingFields** -- Missing fields leave defaults
- **b17_parseAngleBrackets** -- `< ... >` as alternative message delimiters
- **b18_parseColonBeforeMessage** -- `field: { ... }` accepted (colon optional)
- **b19_parseEnumValues** -- Unquoted identifiers to enum
- **b20_parseQuotedKeys** -- `"special.key": value` parsed correctly

### 3. Round-Trip Tests: `org/apache/juneau/proto/ProtoRoundTrip_Test.java`

- **c01_simpleBeanRoundTrip** -- All simple types round-trip
- **c02_nestedBeanRoundTrip** -- Nested beans round-trip
- **c03_collectionOfBeansRoundTrip** -- Repeated fields round-trip to bean collections
- **c04_collectionOfStringsRoundTrip** -- String lists round-trip
- **c05_mapRoundTrip** -- Map properties round-trip
- **c06_enumRoundTrip** -- Enum values round-trip
- **c07_stringEscapingRoundTrip** -- Strings with special chars round-trip
- **c08_booleanRoundTrip** -- Boolean values round-trip
- **c09_numericEdgeCases** -- Integer boundaries, float precision
- **c10_complexBeanRoundTrip** -- Bean with mix of all types
- **c11_objectSwapRoundTrip** -- Swapped values round-trip
- **c12_emptyCollectionsRoundTrip** -- Empty lists round-trip as `[]`
- **c13_nullableReferenceRoundTrip** -- Null `String`, `Integer`, and `Long` fields are omitted on serialize and return `null` on parse, confirming lossless behavior for reference types
- **c14_topLevelScalarLimitation** -- Documents that bare scalars (e.g. a plain `String` or `int`) at the root level must be wrapped in a synthetic single-field message to round-trip; asserts the wrapping and unwrapping behavior
- **c15_untypedJsonMapRoundTrip** -- A `JsonMap` with a mix of string, integer, boolean, nested map, and list values round-trips through `ProtoSerializer`/`ProtoParser` with untyped parsing

### 4. Tokenizer Tests: `org/apache/juneau/proto/ProtoTokenizer_Test.java`

- **d01_bareIdentifier** -- `field_name`, `_private`, `camelCase`
- **d02_quotedString** -- `"hello"`, `'hello'` with escape sequences
- **d03_multiPartString** -- `"part1" "part2"` concatenation
- **d04_decimalInteger** -- `42`, `+42`, `-17`
- **d05_hexInteger** -- `0xDEAD`, `0xFF`
- **d06_octalInteger** -- `0755`, `0123`
- **d07_floatLiteral** -- `3.14`, `1e06`, `10f`, `.5`
- **d08_specialFloats** -- `inf`, `-inf`, `nan`, `infinity`
- **d09_booleans** -- `true`, `false` (also `True`, `t`, `1`, `0`)
- **d10_comments** -- `# comment to end of line`
- **d11_structuralTokens** -- `{`, `}`, `<`, `>`, `[`, `]`, `:`, `,`, `;`
- **d12_stringEscapes** -- `\n`, `\t`, `\\`, `\"`, `\'`, `\xHH`, `\OOO`, `\uHHHH`
- **d13_edgeCaseNumberIdent** -- `10bar` is invalid (number followed by ident)
- **d14_whitespaceHandling** -- Tabs, newlines, carriage returns

### 5. Marshaller Tests: `org/apache/juneau/marshaller/Proto_Test.java`

- **e01_of** -- `Proto.of(bean)` serializes to string
- **e02_to** -- `Proto.to(input, Type)` parses from string
- **e03_roundTrip** -- `Proto.of()` + `Proto.to()` round-trip
- **e04_defaultInstance** -- `Proto.DEFAULT.write()` and `Proto.DEFAULT.read()`

### 6. Annotation Tests: `org/apache/juneau/proto/ProtoAnnotation_Test.java`

- **f01_comment** -- `@Proto(comment="...")` emits comment before field
- **f02_annotationEquivalency** -- Annotation equivalency checks

### 7. Edge Case Tests: `org/apache/juneau/proto/ProtoEdgeCases_Test.java`

- **g01_emptyInput** -- Empty input returns null/empty bean
- **g02_onlyComments** -- Document with only comments returns null/empty bean
- **g03_unicodeStrings** -- Unicode in keys and values
- **g04_veryLongStrings** -- Very long string values
- **g05_deeplyNestedMessages** -- 10+ levels of nesting
- **g06_windowsLineEndings** -- `\r\n` handled correctly
- **g07_mixedSeparators** -- Mix of newlines, semicolons, and commas
- **g08_cyclicReferences** -- Handled by recursion detection
- **g09_optionalProperties** -- `Optional<T>` bean properties
- **g10_mapWithNonStringKeys** -- Integer/enum map keys encoded as strings
- **g11_emptyMessages** -- `field {}` parsed as empty bean
- **g12_trailingComma** -- Trailing comma after last field accepted

### 8. Media Type Tests: `org/apache/juneau/proto/ProtoMediaType_Test.java`

- **h01_producesCorrectMediaType** -- Serializer produces `text/protobuf`
- **h02_consumesCorrectMediaType** -- Parser consumes `text/protobuf`, `text/x-protobuf`
- **h03_contentNegotiation** -- Correct serializer selected via Accept header

---

### 9. Cross-Format Round-Trip Integration: `org/apache/juneau/a/rttests/RoundTripDateTime_Test.java`

Add a `RoundTrip_Tester` entry for this format to the `TESTERS` array in `RoundTripDateTime_Test.java`. This test verifies date/time and Duration round-trip across all serializer/parser combinations. It covers beans with `Instant`, `ZonedDateTime`, `LocalDate`, `LocalDateTime`, `LocalTime`, `OffsetDateTime`, `OffsetTime`, `Year`, `YearMonth`, `Calendar`, `Date`, `Duration`, and `XMLGregorianCalendar` fields.

## Documentation

### 1. Javadoc

**`ProtoSerializer.java`:**
- Description of protobuf text format (link to spec)
- Clarification: text format, not binary wire format
- Media types: `text/protobuf`
- Bean-to-protobuf mapping rules with examples
- Comparison with JSON (similar data model, different syntax)
- Use cases: gRPC ecosystem integration, configuration files, debugging
- Thread safety notes

**`ProtoParser.java`:**
- Supported syntax elements
- Type inference rules (how unquoted values map to Java types)
- Repeated field handling (aggregated into collections)
- Error reporting (line/column numbers)

**`ProtoWriter.java`:**
- Each method documented with protobuf text format output examples
- Escaping rules

**`ProtoTokenizer.java`:**
- Internal class (package-private)
- Token types and lexical rules

### 2. Package Javadoc (`package-info.java`)

- What protobuf text format is and how it differs from binary protobuf
- No `.proto` schema files needed (Java bean metadata serves as schema)
- Complete mapping rules table
- Usage examples for serialization and parsing
- gRPC ecosystem context
- Comparison with JSON, TOML, INI

### 3. Update Release Notes
/Users/james.bognar/git/apache/juneau/docs/pages/release-notes

### 4. Update Documentation

Find how existing languages are documented in the following location and update the documentation to match
the same level of detail.
/Users/james.bognar/git/apache/juneau/docs/pages/topics

---

## Implementation Order

1. **`ProtoTokenizer.java`** -- Foundation for parsing
2. **`ProtoWriter.java`** -- Foundation for serialization
3. **`ProtoSerializer.java`** and **`ProtoSerializerSession.java`** -- Core serialization
4. **`ProtoParser.java`** and **`ProtoParserSession.java`** -- Core parsing
5. **`ProtoMetaProvider.java`**, **`ProtoClassMeta.java`**, **`ProtoBeanPropertyMeta.java`** -- Metadata
6. **`annotation/Proto.java`**, **`annotation/ProtoConfig.java`** -- Annotations
7. **`annotation/ProtoAnnotation.java`**, **`annotation/ProtoConfigAnnotation.java`** -- Annotation utilities
8. **`Proto.java`** (marshaller) -- Convenience API
9. **`package-info.java`** -- Package documentation
10. **Tokenizer tests** (`ProtoTokenizer_Test.java`) -- 14 test cases
11. **Serializer tests** (`ProtoSerializer_Test.java`) -- 20 test cases
12. **Parser tests** (`ProtoParser_Test.java`) -- 20 test cases
13. **Round-trip tests** (`ProtoRoundTrip_Test.java`) -- 12 test cases
14. **Marshaller tests** (`Proto_Test.java`)
15. **Annotation tests** (`ProtoAnnotation_Test.java`)
16. **Edge case tests** (`ProtoEdgeCases_Test.java`) -- 12 test cases
17. **Media type tests** (`ProtoMediaType_Test.java`)
18. **REST integration** (`BasicUniversalConfig`, `BasicProtoConfig`, `RestClient`)
19. **Context registration** (`ProtoConfig`, `ProtoConfigAnnotation`, `Context.java`)
20. **Final documentation review**

---

## Key Design Decisions

### 1. Text format, not binary wire format

Implementing protobuf binary wire format without `.proto` schema files is impractical -- the binary format uses field numbers (not names) and wire types that require schema knowledge to encode/decode. The text format uses field names and is self-describing, making it ideal for Juneau's schema-less bean serialization. The text format is widely used in protobuf configurations, tests, and debugging output.

### 2. Extends WriterSerializer/ReaderParser

Since this is a text format (not binary), it extends `WriterSerializer`/`ReaderParser` like JSON, XML, and other text serializers. The output is UTF-8 text.

### 3. No external dependencies

The protobuf text format grammar is simpler than TOML. The tokenizer handles: identifiers, quoted strings with C-style escaping, integers (decimal/hex/octal), floats (including `inf`/`nan`), comments, and structural tokens (`{}`, `<>`, `[]`, `:`, `,`, `;`).

### 4. Repeated field names for bean collections

Following protobuf convention, collections of beans use repeated field names rather than list syntax:
```
servers { host: "a" }
servers { host: "b" }
```
Not: `servers: [{host: "a"}, {host: "b"}]`

This is the standard protobuf text format convention. The parser aggregates fields with the same name into collections. The `useListSyntaxForBeans` builder option enables the non-standard but more compact list syntax if desired.

### 5. Optional colon before message values

Following protobuf convention, the colon before message values is omitted by default:
```
address {
  city: "NYC"
}
```
The `useColonForMessages` builder option enables colons for consistency with scalar fields if preferred.

### 6. Null omission

Protobuf has no null type. Null properties are omitted during serialization. During parsing, missing fields retain their Java defaults. This is consistent with protobuf semantics where unset fields have default values.

### 7. Type inference during parsing

The parser uses the target bean's `ClassMeta` for type resolution (same strategy as INI and TOML parsers). Unquoted identifiers are parsed as booleans (`true`/`false`) or enum values depending on the target property type. Numbers are parsed as integers or floats based on the target type.

### 8. Package name: `org.apache.juneau.proto`

Short form `proto` rather than `protobuf` to keep the package name concise, consistent with protobuf ecosystem conventions where `proto` is the standard abbreviation.

### 9. Lossless round-trip at JSON parity

The serializer/parser pair must pass the same round-trip tests as JSON for all supported POJO types. Where the format imposes a structural limitation (top-level scalars, null omission), it is documented explicitly in the "JSON Data Structure Parity and Limitations" section and tested to confirm the documented behavior. The `ProtoRoundTrip_Test.java` test class and the `RoundTripDateTime_Test.java` cross-format integration tests enforce this guarantee.

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

The following are explicitly not included in this implementation:

- **Binary wire format** -- Requires `.proto` schema files with field numbers and wire types
- **`.proto` file generation** -- Generating `.proto` schemas from Java beans (possible future feature)
- **Extension fields** -- `[com.example.ext]` syntax (requires type registry)
- **Any fields** -- `[type.googleapis.com/...]` syntax (requires type registry)
- **Group fields** -- Legacy proto2 feature
- **Half-precision floats** -- Not part of protobuf text format
- **Interoperability with protobuf binary messages** -- This is a text-format-only serializer
