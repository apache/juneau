# CBOR Support Implementation Plan for Apache Juneau

## Overview

CBOR (Concise Binary Object Representation) is a binary data format defined in RFC 8949 that provides compact encoding for JSON-like data models. It is designed for extremely small code size, very small message size, and extensibility without version negotiation. CBOR is widely used in IoT, constrained environments, COSE (CBOR Object Signing and Encryption), and WebAuthn.

This plan covers implementing a CBOR serializer and parser from scratch with no external dependencies, following the same architecture as the existing `MsgPackSerializer`/`MsgPackParser` (binary stream-based, extending `OutputStreamSerializer`/`InputStreamParser`).

## Specification

- **RFC 8949**: https://www.rfc-editor.org/rfc/rfc8949.html (obsoletes RFC 7049)
- **Media type**: `application/cbor`
- **Encoding**: Binary, self-describing

---

## CBOR Encoding Format

### Initial Byte Structure

Every CBOR data item starts with an initial byte:
- **High 3 bits** (bits 7-5): Major type (0-7)
- **Low 5 bits** (bits 4-0): Additional information (argument or length indicator)

### Major Types

| Major Type | Meaning | Java Mapping |
|-----------|---------|-------------|
| 0 | Unsigned integer (0 to 2^64-1) | `int`, `long` |
| 1 | Negative integer (-1 to -2^64) | `int`, `long` |
| 2 | Byte string | `byte[]` |
| 3 | Text string (UTF-8) | `String` |
| 4 | Array of data items | `Collection`, arrays |
| 5 | Map of data item pairs | beans, `Map` |
| 6 | Semantic tag (followed by data item) | metadata/hints |
| 7 | Float and simple values | `float`, `double`, `boolean`, `null` |

### Additional Information Encoding

The low 5 bits encode the argument:
- **0-23**: Value is the argument itself (single byte total)
- **24**: 1-byte argument follows
- **25**: 2-byte argument follows (network byte order)
- **26**: 4-byte argument follows (network byte order)
- **27**: 8-byte argument follows (network byte order)
- **28-30**: Reserved
- **31**: Indefinite length (major types 2-5) or break code (major type 7)

### Simple Values (Major Type 7)

| Additional Info | Meaning | Byte |
|----------------|---------|------|
| 20 | `false` | `0xF4` |
| 21 | `true` | `0xF5` |
| 22 | `null` | `0xF6` |
| 23 | `undefined` | `0xF7` |
| 25 | IEEE 754 Half-Precision Float (2 bytes) | `0xF9` + 2 bytes |
| 26 | IEEE 754 Single-Precision Float (4 bytes) | `0xFA` + 4 bytes |
| 27 | IEEE 754 Double-Precision Float (8 bytes) | `0xFB` + 8 bytes |
| 31 | Break (end of indefinite-length item) | `0xFF` |

---

## Bean-to-CBOR Mapping

The mapping from Java beans to CBOR is structurally identical to JSON (and MsgPack), just in binary form:

| Java Type | CBOR Encoding |
|-----------|---------------|
| `null` | Major 7, simple value 22 (`0xF6`) |
| `true` | Major 7, simple value 21 (`0xF5`) |
| `false` | Major 7, simple value 20 (`0xF4`) |
| Positive `int`/`long` | Major 0, compact integer encoding |
| Negative `int`/`long` | Major 1, compact integer encoding (value = -1 - n) |
| `float` | Major 7, additional info 26, IEEE 754 single-precision |
| `double` | Major 7, additional info 27, IEEE 754 double-precision |
| `String` | Major 3, UTF-8 text string with length |
| `byte[]` | Major 2, byte string with length |
| Bean | Major 5 (map), property names as text string keys |
| `Map<K,V>` | Major 5 (map) |
| `Collection<T>`, `T[]` | Major 4 (array) |
| `Enum` | Major 3 (text string of enum name) |
| URI/URL | Major 3 (text string) |

---

## Data Structure Parity with JSON

CBOR supports the same data model as JSON. The table below documents the mapping for every Java type that Juneau's JSON serializer handles, along with any known limitations.

| Java Type | JSON Encoding | CBOR Encoding | Parity |
|-----------|---------------|---------------|--------|
| `null` | `null` | Major 7, `0xF6` | Full |
| `boolean` | `true`/`false` | Major 7, `0xF4`/`0xF5` | Full |
| `int`, `long`, `short`, `byte` | JSON number | Major 0/1, compact integer | Full |
| `float` | JSON number | Major 7, `0xFA` (IEEE 754 single) | Full |
| `double` | JSON number | Major 7, `0xFB` (IEEE 754 double) | Full |
| `BigInteger` | JSON number | Major 0/1, `longValue()` cast (**lossy**) | Partial — see limitation #1 |
| `BigDecimal` | JSON number | Major 7, `doubleValue()` cast (**lossy**) | Partial — see limitation #1 |
| `String` | JSON string | Major 3, UTF-8 text string | Full |
| `byte[]` | Base64 text string | Major 2, byte string | **Better than JSON** — native binary, no base64 overhead |
| Bean / `Map<K,V>` | JSON object | Major 5, map | Full |
| `Collection<T>`, `T[]` | JSON array | Major 4, array | Full |
| `Enum` | String | Major 3, text string of enum name | Full |
| `URI`, `URL` | String | Major 3, text string | Full |
| `Date`, `Calendar`, temporal types | ISO 8601 string | Major 3, ISO 8601 text string | Full |
| `Duration` | ISO 8601 string | Major 3, ISO 8601 text string | Full |
| `Optional<T>` | Value or `null` | Value or `0xF6` | Full |
| `Iterator`, `Iterable`, `Stream` | JSON array (materialized) | Major 4, array (materialized) | Full |
| Non-string map keys | `K.toString()` string key | Major 3, text string key | Full (same stringify behavior) |

### Documented Limitations

These limitations must be documented in the Javadoc of both `CborSerializer` and `CborParser`:

1. **`BigInteger`/`BigDecimal` precision loss** — `BigInteger` values are cast to `long` (values exceeding ±2⁶³−1 lose precision silently); `BigDecimal` values are cast to `double` (values with more than ~15 significant digits lose precision). This matches `MsgPackSerializer` behavior. Use an `ObjectSwap` to serialize these types as strings if precision must be preserved. Native CBOR bignum/decimal-fraction tags (RFC 8949 tags 2, 3, 4) are a future enhancement.

2. **Indefinite-length encoding not supported (initial release)** — The parser only handles definite-length CBOR arrays, maps, byte strings, and text strings. CBOR produced by other implementations using indefinite-length encoding (the `0xFF` break code) will throw a `ParseException`. Indefinite-length support is a future enhancement.

3. **Half-precision float output not produced** — The serializer always writes `float32` (`0xFA`) for `float` values and `float64` (`0xFB`) for `double` values. It never writes `float16` (`0xF9`). The parser does read and correctly convert `float16` values from external CBOR producers.

4. **Semantic tags ignored on input** — The parser reads and silently discards all CBOR semantic tags (major type 6). The tag number does not affect the parsed Java type; the target `ClassMeta` drives all type coercion. Semantic tag output is deferred to a future release.

---

## Architecture

CBOR follows the same binary serializer pattern as MsgPack:

```
OutputStreamSerializer
  └── CborSerializer
        └── CborSerializerSession (uses CborOutputStream for binary encoding)

InputStreamParser
  └── CborParser
        └── CborParserSession (uses CborInputStream for binary decoding)
```

### Class Hierarchy (mirrors MsgPack)

```
MsgPackSerializer  →  CborSerializer
  OutputStreamSerializer    OutputStreamSerializer

MsgPackSerializerSession  →  CborSerializerSession
  OutputStreamSerializerSession    OutputStreamSerializerSession

MsgPackOutputStream  →  CborOutputStream
  OutputStream             OutputStream

MsgPackParser  →  CborParser
  InputStreamParser    InputStreamParser

MsgPackParserSession  →  CborParserSession
  InputStreamParserSession    InputStreamParserSession

MsgPackInputStream  →  CborInputStream
  ParserInputStream      ParserInputStream
```

---

## Files to Create

All source files in `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/cbor/`.

### 1. `CborOutputStream.java`

Extends `OutputStream`. The core binary encoder for CBOR format.

This is the most critical file -- it implements the CBOR encoding rules from RFC 8949.

```java
package org.apache.juneau.cbor;

public class CborOutputStream extends OutputStream {

    // Low-level byte writers
    void write1(int b);              // Write 1 byte
    void write2(int value);          // Write 2 bytes big-endian
    void write4(int value);          // Write 4 bytes big-endian
    void write8(long value);         // Write 8 bytes big-endian

    // CBOR head encoding (major type + argument)
    void writeHead(int majorType, long argument);
    // Encodes argument as:
    //   0-23: single byte (majorType << 5 | argument)
    //   24-255: 2 bytes (majorType << 5 | 24, argument)
    //   256-65535: 3 bytes (majorType << 5 | 25, 2-byte argument)
    //   65536-4294967295: 5 bytes (majorType << 5 | 26, 4-byte argument)
    //   larger: 9 bytes (majorType << 5 | 27, 8-byte argument)

    // Type-specific encoders
    CborOutputStream appendNull();           // 0xF6
    CborOutputStream appendUndefined();      // 0xF7
    CborOutputStream appendBoolean(boolean); // 0xF4 or 0xF5

    // Integer encoding (auto-selects major type 0 or 1)
    CborOutputStream appendInt(int value);
    CborOutputStream appendLong(long value);
    // Positive: major type 0 with compact argument
    // Negative: major type 1 with argument = -1 - value

    // Float encoding
    CborOutputStream appendFloat(float value);   // 0xFA + 4 bytes IEEE 754
    CborOutputStream appendDouble(double value);  // 0xFB + 8 bytes IEEE 754
    CborOutputStream appendNumber(Number value);  // Dispatches to appropriate method

    // String encoding (major type 3)
    CborOutputStream appendString(CharSequence s);
    // Calculates UTF-8 byte length, writes head(3, length), then UTF-8 bytes

    // Binary encoding (major type 2)
    CborOutputStream appendBinary(byte[] data);
    CborOutputStream appendBinary(InputStream data);

    // Structure encoding
    CborOutputStream startArray(int size);   // Major type 4 with length
    CborOutputStream startMap(int size);     // Major type 5 with length

    // Tag encoding (major type 6)
    CborOutputStream writeTag(long tagNumber);
    // Used for optional semantic tagging (e.g., tag 0 for date-time strings)
}
```

Key encoding details:

**Integer encoding** -- The most compact representation is chosen automatically:
```
value >= 0:  Major type 0
  0-23:       1 byte  (type | value)
  24-255:     2 bytes (type | 24, value)
  256-65535:  3 bytes (type | 25, value in 2 bytes)
  up to 2^32: 5 bytes (type | 26, value in 4 bytes)
  up to 2^64: 9 bytes (type | 27, value in 8 bytes)

value < 0:   Major type 1, argument = -1 - value
  Same size tiers as above
```

**String encoding** -- UTF-8 with same compact length tiers as integers.

### 2. `CborInputStream.java`

Extends `ParserInputStream`. The core binary decoder for CBOR format.

```java
package org.apache.juneau.cbor;

class CborInputStream extends ParserInputStream {

    // Read the initial byte and extract components
    int readInitialByte();
    int getMajorType(int initialByte);      // High 3 bits
    int getAdditionalInfo(int initialByte);  // Low 5 bits

    // Read the argument based on additional info
    long readArgument(int additionalInfo);
    // 0-23: return additionalInfo
    // 24: read 1 byte
    // 25: read 2 bytes big-endian
    // 26: read 4 bytes big-endian
    // 27: read 8 bytes big-endian

    // High-level type reading
    DataType readDataType();   // Reads initial byte, returns DataType enum
    long readLength();         // Returns argument (length for strings/arrays/maps)

    // Value readers
    boolean readBoolean();
    int readInt();
    long readLong();
    float readFloat();
    double readDouble();
    String readString();       // Reads UTF-8 text string
    byte[] readBinary();       // Reads byte string

    // Multi-byte readers
    int readUInt1();           // Read 1 unsigned byte
    int readUInt2();           // Read 2 bytes big-endian
    long readUInt4();          // Read 4 bytes big-endian
    long readUInt8();          // Read 8 bytes big-endian
}
```

### 3. `DataType.java`

Enum for CBOR data types (mirrors MsgPack's DataType pattern).

```java
package org.apache.juneau.cbor;

enum DataType {
    UINT,        // Major type 0: unsigned integer
    NINT,        // Major type 1: negative integer
    BINARY,      // Major type 2: byte string
    STRING,      // Major type 3: text string
    ARRAY,       // Major type 4: array
    MAP,         // Major type 5: map
    TAG,         // Major type 6: semantic tag
    FLOAT,       // Major type 7: float (half/single/double)
    BOOLEAN,     // Major type 7: simple value 20/21
    NULL,        // Major type 7: simple value 22
    UNDEFINED,   // Major type 7: simple value 23
    BREAK,       // Major type 7: additional info 31
    SIMPLE       // Major type 7: other simple values
}
```

### 4. `CborSerializer.java`

Extends `OutputStreamSerializer`. Implements `CborMetaProvider`.

```java
package org.apache.juneau.cbor;

public class CborSerializer extends OutputStreamSerializer implements CborMetaProvider {

    public static final CborSerializer DEFAULT = ...;
    public static final CborSerializer DEFAULT_SPACED_HEX = ...;
    public static final CborSerializer DEFAULT_BASE64 = ...;

    public static class Builder extends OutputStreamSerializer.Builder {
        boolean addBeanTypesCbor;     // Add _type properties
        boolean useTags = false;      // Use CBOR semantic tags (tag 0 for dates, etc.)

        protected Builder() {
            produces("application/cbor");
        }
    }

    // Inner classes for text-encoded output (for debugging/testing)
    public static class SpacedHex extends CborSerializer { ... }
    public static class Base64 extends CborSerializer { ... }

    @Override
    public CborSerializerSession.Builder createSession() {
        return CborSerializerSession.create(this);
    }
}
```

Builder options:
- `addBeanTypesCbor()` -- Add `_type` properties to beans when type cannot be inferred
- `useTags(boolean)` -- Use CBOR semantic tags for dates (tag 0 for date-time string, tag 1 for epoch) and other typed values

SpacedHex/Base64 inner classes follow the MsgPack pattern for text-friendly binary representation (useful in testing and debugging).

### 5. `CborSerializerSession.java`

Extends `OutputStreamSerializerSession`. Core serialization logic.

Mirrors `MsgPackSerializerSession` almost exactly:

```java
package org.apache.juneau.cbor;

public class CborSerializerSession extends OutputStreamSerializerSession {

    @Override
    protected void doSerialize(SerializerPipe out, Object o)
        throws IOException, SerializeException {
        serializeAnything(getCborOutputStream(out), o, getExpectedRootType(o), "root", null);
    }

    // Central dispatch -- identical pattern to MsgPackSerializerSession
    CborOutputStream serializeAnything(CborOutputStream out, Object o,
        ClassMeta<?> eType, String attrName, BeanPropertyMeta pMeta)
        throws SerializeException {
        // null -> out.appendNull()
        // boolean -> out.appendBoolean()
        // number -> out.appendNumber()
        // date/calendar/temporal -> out.appendString(Iso8601Utils.format(o, sType, getTimeZone()))
        // duration -> out.appendString(o.toString())
        // bean -> serializeBeanMap()
        // map -> serializeMap()
        // collection/array -> serializeCollection()
        // byte[] -> out.appendBinary()
        // string -> out.appendString()
    }

    void serializeBeanMap(CborOutputStream out, BeanMap<?> m, String typeName)
        throws SerializeException {
        // Count properties, call out.startMap(count)
        // Serialize _type if needed
        // Iterate properties: serialize key (string) + value (recursive)
    }

    void serializeMap(CborOutputStream out, Map m, ClassMeta<?> type)
        throws SerializeException {
        // Count entries, call out.startMap(count)
        // Iterate entries: serialize key + value recursively
    }

    void serializeCollection(CborOutputStream out, Collection c, ClassMeta<?> type)
        throws SerializeException {
        // Count elements, call out.startArray(count)
        // Iterate elements: serialize each recursively
    }

    CborOutputStream getCborOutputStream(SerializerPipe out) {
        // Wrap raw output in CborOutputStream if not already
    }
}
```

### 6. `CborParser.java`

Extends `InputStreamParser`. Implements `CborMetaProvider`.

```java
package org.apache.juneau.cbor;

public class CborParser extends InputStreamParser implements CborMetaProvider {

    public static final CborParser DEFAULT = ...;
    public static final CborParser DEFAULT_SPACED_HEX = ...;
    public static final CborParser DEFAULT_BASE64 = ...;

    public static class Builder extends InputStreamParser.Builder {
        protected Builder() {
            consumes("application/cbor");
        }
    }

    public static class SpacedHex extends CborParser { ... }
    public static class Base64 extends CborParser { ... }

    @Override
    public CborParserSession.Builder createSession() {
        return CborParserSession.create(this);
    }
}
```

### 7. `CborParserSession.java`

Extends `InputStreamParserSession`. Core parsing logic.

Mirrors `MsgPackParserSession`:

```java
package org.apache.juneau.cbor;

public class CborParserSession extends InputStreamParserSession {

    @Override
    protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type)
        throws IOException, ParseException, ExecutableException {
        try (CborInputStream is = new CborInputStream(pipe)) {
            return parseAnything(type, is, getOuter(), null);
        }
    }

    // Central dispatch -- mirrors MsgPackParserSession.parseAnything()
    <T> T parseAnything(ClassMeta<T> eType, CborInputStream is,
        Object outer, BeanPropertyMeta pMeta)
        throws IOException, ParseException, ExecutableException {

        DataType dt = is.readDataType();
        long length = is.readLength();

        switch (dt) {
            case NULL: return null;
            case BOOLEAN: return convert(is.readBoolean(), eType);
            case UINT: return convert(is.readUnsignedInt(length), eType);
            case NINT: return convert(is.readNegativeInt(length), eType);
            case FLOAT: return convert(is.readFloat/Double(length), eType);
            case STRING:
                String s = is.readString(length);
                if (eType.isDateOrCalendarOrTemporal() || eType.isDuration())
                    return (T) Iso8601Utils.parse(s, eType, getTimeZone());
                return convert(s, eType);
            case BINARY: return convert(is.readBinary(length), eType);
            case ARRAY: // loop length times, parse elements
            case MAP: // loop length times, parse key-value pairs
            case TAG: // read tag number, then parse following data item
        }
    }
}
```

### 8. `CborMetaProvider.java`

```java
package org.apache.juneau.cbor;

public interface CborMetaProvider {
    CborClassMeta getCborClassMeta(ClassMeta<?> cm);
    CborBeanPropertyMeta getCborBeanPropertyMeta(BeanPropertyMeta bpm);
}
```

### 9. `CborClassMeta.java` and `CborBeanPropertyMeta.java`

Extend `ExtendedClassMeta` and `ExtendedBeanPropertyMeta`. Initially minimal (similar to MsgPack which has empty metadata classes).

### 10. `annotation/Cbor.java`

```java
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
public @interface Cbor {
    String on() default "";
    String[] onClass() default {};
}
```

Minimal initially. Can be extended later with CBOR-specific options like tag numbers.

### 11. `annotation/CborConfig.java`

```java
@Target({TYPE, METHOD})
@Retention(RUNTIME)
public @interface CborConfig {
    String addBeanTypes() default "";
    String useTags() default "";
    int rank() default 0;
}
```

### 12. `annotation/CborAnnotation.java` and `annotation/CborConfigAnnotation.java`

Utility classes for programmatic annotation creation.

### 13. `Cbor.java` (Marshaller)

Location: `src/main/java/org/apache/juneau/marshaller/Cbor.java`

Extends `StreamMarshaller` (not `CharMarshaller`), following the `MsgPack.java` pattern.

```java
package org.apache.juneau.marshaller;

public class Cbor extends StreamMarshaller {

    public static final Cbor DEFAULT = new Cbor();

    public static byte[] of(Object object) throws SerializeException {
        return DEFAULT.write(object);
    }

    public static Object of(Object object, OutputStream output)
        throws SerializeException, IOException {
        DEFAULT.write(object, output);
        return output;
    }

    public static <T> T to(byte[] input, Class<T> type) throws ParseException {
        return DEFAULT.read(input, type);
    }

    public static <T> T to(Object input, Class<T> type)
        throws ParseException, IOException {
        return DEFAULT.read(input, type);
    }

    public Cbor() { this(CborSerializer.DEFAULT, CborParser.DEFAULT); }
    public Cbor(CborSerializer s, CborParser p) { super(s, p); }
}
```

### 14. `package-info.java`

Package-level Javadoc.

---

## Files to Modify

### 1. `BasicUniversalConfig.java`

Add `CborSerializer.class` to `serializers` and `CborParser.class` to `parsers`.

### 2. `BasicCborConfig.java` (New)

```java
@Rest(
    serializers={CborSerializer.class},
    parsers={CborParser.class},
    defaultAccept="application/cbor"
)
public interface BasicCborConfig extends DefaultConfig {}
```

### 3. `RestClient.java`

Add `CborSerializer.class` and `CborParser.class` to the `universal()` method.

### 4. Context: `Context.java`

Path: `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/Context.java`

Add `import org.apache.juneau.cbor.annotation.*;` and add `{@link CborConfig}` to the list of config annotations in the `applyAnnotations()` Javadoc (alphabetically).

---

## Detailed CBOR Encoding Implementation

### Integer Encoding (`CborOutputStream.appendInt/appendLong`)

```
if value >= 0:
    writeHead(majorType=0, argument=value)
else:
    writeHead(majorType=1, argument=-1 - value)

writeHead(majorType, argument):
    typeBits = majorType << 5
    if argument <= 23:
        write1(typeBits | argument)
    else if argument <= 0xFF:
        write1(typeBits | 24)
        write1(argument)
    else if argument <= 0xFFFF:
        write1(typeBits | 25)
        write2(argument)
    else if argument <= 0xFFFFFFFF:
        write1(typeBits | 26)
        write4(argument)
    else:
        write1(typeBits | 27)
        write8(argument)
```

### String Encoding (`CborOutputStream.appendString`)

```
utf8Bytes = string.getBytes(UTF_8)  // or manual UTF-8 encoding
writeHead(majorType=3, argument=utf8Bytes.length)
write(utf8Bytes)
```

### Array Encoding

```
startArray(size):
    writeHead(majorType=4, argument=size)
// Then serialize each element
```

### Map Encoding

```
startMap(size):
    writeHead(majorType=5, argument=size)
// Then serialize key-value pairs
```

### Float Encoding

```
appendFloat(float):
    write1(0xFA)  // Major 7, additional info 26
    write4(Float.floatToRawIntBits(value))

appendDouble(double):
    write1(0xFB)  // Major 7, additional info 27
    write8(Double.doubleToRawLongBits(value))
```

### CBOR Decoding (`CborInputStream`)

```
readDataType():
    initialByte = readByte()
    majorType = (initialByte >> 5) & 0x07
    additionalInfo = initialByte & 0x1F

    switch majorType:
        0: type = UINT
        1: type = NINT
        2: type = BINARY
        3: type = STRING
        4: type = ARRAY
        5: type = MAP
        6: type = TAG
        7:
            switch additionalInfo:
                20: type = BOOLEAN (false)
                21: type = BOOLEAN (true)
                22: type = NULL
                23: type = UNDEFINED
                25, 26, 27: type = FLOAT
                31: type = BREAK

    argument = readArgument(additionalInfo)
    return (type, argument)

readArgument(additionalInfo):
    if additionalInfo <= 23: return additionalInfo
    if additionalInfo == 24: return readUInt1()
    if additionalInfo == 25: return readUInt2()
    if additionalInfo == 26: return readUInt4()
    if additionalInfo == 27: return readUInt8()
```

---

## Test Plan

All test files in `juneau-utest/src/test/java/`.

### 1. Output Stream Tests: `org/apache/juneau/cbor/CborOutputStream_Test.java`

Low-level encoding verification against expected byte sequences:

**Integer encoding:**
- **a01_positiveFixint** -- 0-23 encode as single byte `0x00`-`0x17`
- **a02_uint8** -- 24-255 encode as `0x18` + 1 byte
- **a03_uint16** -- 256-65535 encode as `0x19` + 2 bytes
- **a04_uint32** -- Up to 2^32-1 encode as `0x1A` + 4 bytes
- **a05_uint64** -- Up to 2^64-1 encode as `0x1B` + 8 bytes
- **a06_negativeFixint** -- -1 to -24 encode as single byte `0x20`-`0x37`
- **a07_nint8** -- -25 to -256 encode as `0x38` + 1 byte
- **a08_nint16** -- Larger negatives with 2-byte argument
- **a09_nint32** -- Larger negatives with 4-byte argument
- **a10_nint64** -- Largest negatives with 8-byte argument
- **a11_integerBoundaries** -- Exact boundary values between encoding tiers

**Simple values:**
- **a12_null** -- `0xF6`
- **a13_true** -- `0xF5`
- **a14_false** -- `0xF4`

**Floats:**
- **a15_float32** -- `0xFA` + 4 bytes IEEE 754
- **a16_float64** -- `0xFB` + 8 bytes IEEE 754
- **a17_floatSpecial** -- Infinity, NaN encoding

**Strings:**
- **a18_emptyString** -- `0x60`
- **a19_shortString** -- Up to 23 bytes: `0x60`-`0x77`
- **a20_mediumString** -- 24-255 bytes: `0x78` + 1 byte length
- **a21_longString** -- 256+ bytes: `0x79` + 2 byte length
- **a22_unicodeString** -- Multi-byte UTF-8 characters

**Binary:**
- **a23_emptyBinary** -- `0x40`
- **a24_shortBinary** -- Up to 23 bytes: `0x40`-`0x57`
- **a25_longBinary** -- Larger binary with length prefix

**Arrays:**
- **a26_emptyArray** -- `0x80`
- **a27_shortArray** -- 1-23 elements: `0x81`-`0x97`
- **a28_longArray** -- 24+ elements with length prefix

**Maps:**
- **a29_emptyMap** -- `0xA0`
- **a30_shortMap** -- 1-23 entries: `0xA1`-`0xB7`
- **a31_longMap** -- 24+ entries with length prefix

### 2. Input Stream Tests: `org/apache/juneau/cbor/CborInputStream_Test.java`

Verify decoding of each encoding pattern:

- **b01_decodePositiveIntegers** -- All integer tier sizes
- **b02_decodeNegativeIntegers** -- All negative integer tiers
- **b03_decodeBoolean** -- True and false
- **b04_decodeNull** -- Null value
- **b05_decodeFloat32** -- Single-precision float
- **b06_decodeFloat64** -- Double-precision float
- **b07_decodeStrings** -- All string length tiers
- **b08_decodeBinary** -- All binary length tiers
- **b09_decodeArrays** -- All array size tiers
- **b10_decodeMaps** -- All map size tiers
- **b11_decodeUnicodeStrings** -- Multi-byte UTF-8
- **b12_dataTypeDetection** -- Correct DataType enum from initial byte

### 3. Serializer Tests: `org/apache/juneau/cbor/CborSerializer_Test.java`

- **c01_simpleBean** -- Bean with string, int, boolean properties
- **c02_nestedBean** -- Bean with nested bean property
- **c03_collectionOfBeans** -- `List<Bean>` serialized as CBOR array of maps
- **c04_mapProperty** -- `Map<String,Object>` serialized as CBOR map
- **c05_nullValues** -- Null properties encoded as `0xF6`
- **c06_booleanValues** -- True/false encoding
- **c07_integerValues** -- Various integer ranges (compact encoding verified)
- **c08_floatValues** -- Float and double values
- **c09_stringValues** -- Various string lengths
- **c10_binaryValues** -- `byte[]` properties
- **c11_enumValues** -- Enum serialized as string
- **c12_objectSwaps** -- Swap transformations before encoding
- **c13_emptyBean** -- Bean with no properties (empty map)
- **c14_emptyCollections** -- Empty list (empty array)
- **c15_typeName** -- `_type` property when `addBeanTypes` enabled
- **c16_deeplyNestedBean** -- Multiple nesting levels
- **c17_collectionOfStrings** -- String array
- **c18_collectionOfNumbers** -- Integer/float arrays
- **c19_spacedHexOutput** -- SpacedHex variant produces readable hex
- **c20_base64Output** -- Base64 variant produces base64 string

### 4. Parser Tests: `org/apache/juneau/cbor/CborParser_Test.java`

- **d01_parseSimpleBean** -- CBOR map to bean
- **d02_parseNestedBean** -- Nested CBOR maps to nested beans
- **d03_parseCollectionOfBeans** -- CBOR array of maps to `List<Bean>`
- **d04_parseMapProperty** -- CBOR map to `Map<String,Object>`
- **d05_parseNullValues** -- `0xF6` to null
- **d06_parseBooleans** -- `0xF4`/`0xF5` to boolean
- **d07_parseIntegers** -- All integer tiers to int/long
- **d08_parseNegativeIntegers** -- Major type 1 to negative int/long
- **d09_parseFloats** -- Float32/Float64 to float/double
- **d10_parseStrings** -- UTF-8 text strings to String
- **d11_parseBinary** -- Byte strings to `byte[]`
- **d12_parseEnums** -- String to enum
- **d13_parseWithSwaps** -- ObjectSwap reverse transformations
- **d14_parseEmptyStructures** -- Empty map/array
- **d15_parseTypeName** -- `_type` used for type resolution
- **d16_spacedHexInput** -- Parse from spaced-hex string
- **d17_base64Input** -- Parse from base64 string
- **d18_parseTag** -- Semantic tags read and tag number skipped (value parsed normally)
- **d19_unknownSimpleValues** -- Unknown simple values handled gracefully
- **d20_autoClose** -- InputStream auto-closed after parsing

### 5. Round-Trip Tests: `org/apache/juneau/cbor/CborRoundTrip_Test.java`

- **e01_simpleBeanRoundTrip** -- All simple types round-trip
- **e02_nestedBeanRoundTrip** -- Nested beans round-trip
- **e03_collectionOfBeansRoundTrip** -- Bean collections round-trip
- **e04_mapRoundTrip** -- Map properties round-trip
- **e05_allPrimitiveTypesRoundTrip** -- Every Java primitive type and wrapper
- **e06_nullRoundTrip** -- Null values round-trip
- **e07_binaryDataRoundTrip** -- `byte[]` properties round-trip
- **e08_stringEdgeCasesRoundTrip** -- Empty strings, Unicode, long strings
- **e09_integerBoundariesRoundTrip** -- Values at encoding tier boundaries
- **e10_enumRoundTrip** -- Enum values round-trip
- **e11_objectSwapRoundTrip** -- Swapped values round-trip
- **e12_complexBeanRoundTrip** -- Bean with mix of all types
- **e13_emptyCollectionsRoundTrip** -- Empty lists/arrays
- **e14_largeBeanRoundTrip** -- Bean with many properties (map size > 23)
- **e15_largeCollectionRoundTrip** -- Collection with many elements (array size > 23)

### 6. Marshaller Tests: `org/apache/juneau/marshaller/Cbor_Test.java`

- **f01_of** -- `Cbor.of(bean)` serializes to byte array
- **f02_to** -- `Cbor.to(bytes, Type)` parses from byte array
- **f03_roundTrip** -- `Cbor.of()` + `Cbor.to()` round-trip
- **f04_ofToOutputStream** -- Serialize to OutputStream
- **f05_toFromInputStream** -- Parse from InputStream

### 7. RFC Compliance Tests: `org/apache/juneau/cbor/CborCompliance_Test.java`

Test against CBOR diagnostic notation examples from RFC 8949 Appendix A:

- **g01_integer_0** -- `0` encodes to `0x00`
- **g02_integer_1** -- `1` encodes to `0x01`
- **g03_integer_23** -- `23` encodes to `0x17`
- **g04_integer_24** -- `24` encodes to `0x1818`
- **g05_integer_100** -- `100` encodes to `0x1864`
- **g06_integer_1000** -- `1000` encodes to `0x1903E8`
- **g07_integer_1000000** -- `1000000` encodes to `0x1A000F4240`
- **g08_negativeInteger_1** -- `-1` encodes to `0x20`
- **g09_negativeInteger_100** -- `-100` encodes to `0x3863`
- **g10_float_0_0** -- `0.0` encodes to `0xFA00000000` (or `0xF90000` if half-precision)
- **g11_float_1_1** -- `1.1` encodes correctly
- **g12_float_infinity** -- Infinity encodes correctly
- **g13_float_nan** -- NaN encodes correctly
- **g14_emptyString** -- `""` encodes to `0x60`
- **g15_shortString** -- `"a"` encodes to `0x6161`
- **g16_emptyArray** -- `[]` encodes to `0x80`
- **g17_simpleArray** -- `[1, 2, 3]` encodes to `0x83010203`
- **g18_emptyMap** -- `{}` encodes to `0xA0`
- **g19_false** -- `false` encodes to `0xF4`
- **g20_true** -- `true` encodes to `0xF5`
- **g21_null** -- `null` encodes to `0xF6`

### 8. Edge Case Tests: `org/apache/juneau/cbor/CborEdgeCases_Test.java`

- **h01_maxPositiveInt** -- `Integer.MAX_VALUE` and `Long.MAX_VALUE`
- **h02_minNegativeInt** -- `Integer.MIN_VALUE` and `Long.MIN_VALUE`
- **h03_floatSpecialValues** -- `Float.NaN`, `Float.POSITIVE_INFINITY`, `Float.NEGATIVE_INFINITY`
- **h04_doubleSpecialValues** -- Same for double
- **h05_veryLongString** -- String exceeding 65535 bytes
- **h06_veryLargeBinary** -- Binary data exceeding 65535 bytes
- **h07_veryLargeArray** -- Array with > 65535 elements
- **h08_veryLargeMap** -- Map with > 65535 entries
- **h09_emptyBinaryData** -- Zero-length byte array
- **h10_unicodeEdgeCases** -- Surrogate pairs, BMP characters
- **h11_cyclicReferences** -- Handled by recursion detection
- **h12_optionalProperties** -- `Optional<T>` bean properties
- **h13_unknownTags** -- Parser ignores unknown semantic tags
- **h14_zeroLengthInput** -- Empty input stream returns null

### 9. Media Type Tests: `org/apache/juneau/cbor/CborMediaType_Test.java`

- **i01_producesCorrectMediaType** -- Serializer produces `application/cbor`
- **i02_consumesCorrectMediaType** -- Parser consumes `application/cbor`
- **i03_contentNegotiation** -- Correct serializer selected via Accept header

### 10. Cross-Format Round-Trip Integration: `org/apache/juneau/a/rttests/RoundTripDateTime_Test.java`

Add a `RoundTrip_Tester` entry for this format to the `TESTERS` array in `RoundTripDateTime_Test.java`. This test verifies date/time and Duration round-trip across all serializer/parser combinations. It covers beans with `Instant`, `ZonedDateTime`, `LocalDate`, `LocalDateTime`, `LocalTime`, `OffsetDateTime`, `OffsetTime`, `Year`, `YearMonth`, `Calendar`, `Date`, `Duration`, and `XMLGregorianCalendar` fields.

---

## Documentation

### 1. Javadoc

**`CborSerializer.java`** — follows the JSON serializer Javadoc pattern:

- **Media types section**: Accept: `application/cbor`, Content-Type: `application/cbor`
- **Description section** with Java-to-CBOR type mapping:
  - Maps/beans → CBOR maps (major type 5)
  - Collections/arrays → CBOR arrays (major type 4)
  - Strings → CBOR text strings (major type 3, UTF-8)
  - Integers → CBOR compact integers (major types 0/1)
  - Floats/doubles → CBOR IEEE 754 floats (major type 7, `0xFA`/`0xFB`)
  - Booleans → CBOR simple values 20/21 (`0xF4`/`0xF5`)
  - `null` → CBOR simple value 22 (`0xF6`)
  - `byte[]` → CBOR byte strings (major type 2)
- **Behavior-specific subclasses section**: `SpacedHex` (spaced-hex text output), `Base64` (Base64 text output)
- **Examples section** with two code blocks using Juneau Javadoc tag conventions:

```java
// Serialize a bean to CBOR bytes using the default serializer
byte[] <jv>cbor</jv> = CborSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>myBean</jv>);

// Custom serializer with bean type properties enabled
CborSerializer <jv>s</jv> = CborSerializer.<jsm>create</jsm>().addBeanTypes().build();
byte[] <jv>cbor</jv> = <jv>s</jv>.serialize(<jv>myBean</jv>);
```

- **Limitations section**: the 4 documented limitations from the "Documented Limitations" section above
- **Notes**: thread safe and reusable
- **See Also**: link to "CborBasics" doc topic, RFC 8949 (https://www.rfc-editor.org/rfc/rfc8949.html)

---

**`CborParser.java`** — follows the JSON parser Javadoc pattern:

- **Media types section**: Content-Type: `application/cbor`
- **Description section** with CBOR-to-Java type mapping (when no target type is specified):
  - CBOR map (major type 5) → `JsonMap` (or Java bean if `_type` property present)
  - CBOR array (major type 4) → `JsonList`
  - CBOR text string (major type 3) → `String`
  - CBOR unsigned/negative integer (major types 0/1) → `Integer` or `Long` (size-dependent)
  - CBOR float (major type 7, `0xFA`) → `Float`
  - CBOR double (major type 7, `0xFB`) → `Double`
  - CBOR half-precision float (major type 7, `0xF9`) → `Float` (converted)
  - CBOR boolean (major type 7, `0xF4`/`0xF5`) → `Boolean`
  - CBOR null (major type 7, `0xF6`) → `null`
  - CBOR byte string (major type 2) → `byte[]`
  - CBOR semantic tag (major type 6) → ignored, tagged value is parsed normally
- **Examples section** with two code blocks:

```java
// Parse CBOR bytes into a bean
MyBean <jv>parsed</jv> = CborParser.<jsf>DEFAULT</jsf>.parse(<jv>cborBytes</jv>, MyBean.<jk>class</jk>);

// Parse into a generic map
JsonMap <jv>map</jv> = CborParser.<jsf>DEFAULT</jsf>.parse(<jv>cborBytes</jv>, JsonMap.<jk>class</jk>);
```

- **Limitations section**: the same 4 documented limitations
- **Notes**: thread safe and reusable
- **See Also**: link to "CborBasics" doc topic, RFC 8949 (https://www.rfc-editor.org/rfc/rfc8949.html)

---

**`Cbor.java`** (marshaller) — follows the `MsgPack` marshaller Javadoc pattern:

- **Examples section** with two code blocks (static method pattern + DEFAULT instance pattern):

```java
// Using static convenience methods
byte[] <jv>cbor</jv> = Cbor.<jsm>of</jsm>(<jv>myBean</jv>);
MyBean <jv>parsed</jv> = Cbor.<jsm>to</jsm>(<jv>cbor</jv>, MyBean.<jk>class</jk>);
```

```java
// Using the DEFAULT instance
byte[] <jv>cbor</jv> = Cbor.<jsf>DEFAULT</jsf>.write(<jv>myBean</jv>);
MyBean <jv>parsed</jv> = Cbor.<jsf>DEFAULT</jsf>.read(<jv>cbor</jv>, MyBean.<jk>class</jk>);
```

- Note that output is binary (`byte[]`), RFC 8949 CBOR format
- **See Also**: link to the Marshallers doc topic

---

**`CborOutputStream.java`:**
- RFC 8949 reference
- Media type: `application/cbor`
- Encoding format overview (major types, compact integers)
- Thread safety notes (not thread-safe — per-session instance)

**`CborParser.java`** field-level Javadoc (in addition to class-level above):
- Each `DEFAULT`, `DEFAULT_SPACED_HEX`, `DEFAULT_BASE64` field documented

**`CborOutputStream.java`:**
- Each method documented with exact byte sequence output
- RFC 8949 section references

**`CborInputStream.java`:**
- Decoding logic documented
- Error handling for malformed input

### 2. Package Javadoc (`package-info.java`)

- What CBOR is and when to use it (IoT, constrained networks, WebAuthn)
- Comparison with JSON (same data model, binary encoding)
- Comparison with MsgPack (both binary, different specs and ecosystems)
- Type mapping table
- Usage examples
- RFC 8949 reference link

### 3. Update Release Notes
/Users/james.bognar/git/apache/juneau/docs/pages/release-notes

### 4. Update Documentation

Find how existing languages are documented in the following location and update the documentation to match
the same level of detail.
/Users/james.bognar/git/apache/juneau/docs/pages/topics

---

## Implementation Order

1. **`DataType.java`** -- Enum for CBOR types
2. **`CborOutputStream.java`** -- Binary encoder (most critical, needs RFC compliance)
3. **`CborInputStream.java`** -- Binary decoder
4. **`CborSerializer.java`** and **`CborSerializerSession.java`** -- Serialization
5. **`CborParser.java`** and **`CborParserSession.java`** -- Parsing
6. **`CborMetaProvider.java`**, **`CborClassMeta.java`**, **`CborBeanPropertyMeta.java`** -- Metadata
7. **`annotation/Cbor.java`**, **`annotation/CborConfig.java`** -- Annotations
8. **`annotation/CborAnnotation.java`**, **`annotation/CborConfigAnnotation.java`** -- Annotation utilities
9. **`Cbor.java`** (marshaller) -- Convenience API
10. **`package-info.java`** -- Package documentation
11. **Output stream tests** (`CborOutputStream_Test.java`) -- Verify exact byte sequences
12. **Input stream tests** (`CborInputStream_Test.java`) -- Verify decoding
13. **RFC compliance tests** (`CborCompliance_Test.java`) -- Verify against RFC 8949 Appendix A examples
14. **Serializer tests** (`CborSerializer_Test.java`) -- 20 test cases
15. **Parser tests** (`CborParser_Test.java`) -- 20 test cases
16. **Round-trip tests** (`CborRoundTrip_Test.java`) -- 15 test cases
17. **Marshaller tests** (`Cbor_Test.java`)
18. **Edge case tests** (`CborEdgeCases_Test.java`) -- 14 test cases
19. **Media type tests** (`CborMediaType_Test.java`)
20. **REST integration** (`BasicUniversalConfig`, `BasicCborConfig`, `RestClient`)
21. **Context registration** (`CborConfig`, `CborConfigAnnotation`, `Context.java`)
22. **Final documentation review**

---

## Key Design Decisions

### 1. Mirrors MsgPack architecture exactly

CBOR and MsgPack serve the same purpose (binary JSON-like encoding) and have almost identical architectures. The implementation mirrors `MsgPackSerializer`/`MsgPackParser` class-for-class: `OutputStreamSerializer`, `InputStreamParser`, custom `OutputStream`/`InputStream`, `DataType` enum, `StreamMarshaller`. This consistency makes the codebase easier to maintain.

### 2. No external dependencies

The CBOR encoding is simpler than TOML parsing -- it's a straightforward binary format with a small number of major types and a consistent length-encoding scheme. The entire encoder/decoder fits in two classes (`CborOutputStream`, `CborInputStream`).

### 3. Definite-length encoding only (serializer)

The serializer always uses definite-length encoding (the element count is written before the data). Indefinite-length encoding (where a break code terminates the sequence) is not used for output. This simplifies the serializer and produces smaller output. The parser should handle indefinite-length input from other CBOR producers as a future enhancement.

### 4. No half-precision floats (serializer)

The serializer always uses single-precision (`float32`) or double-precision (`float64`) floats. Half-precision (`float16`) encoding is not produced. The parser should accept half-precision floats from other producers.

### 5. Semantic tags are optional

CBOR semantic tags (major type 6) are supported but disabled by default. When `useTags(true)` is enabled, dates are tagged with tag 0 (RFC 3339 date-time string) or tag 1 (epoch-based). The parser always recognizes tags and processes the following data item, discarding the tag number (the bean's `ClassMeta` determines the target type, not the tag).

### 6. SpacedHex and Base64 variants

Following the MsgPack pattern, `SpacedHex` and `Base64` inner classes allow CBOR data to be represented as text strings for debugging, testing, and environments that require text transport. These use `BinaryFormat.SPACED_HEX` and `BinaryFormat.BASE64` respectively.

### 7. Same data model as JSON/MsgPack

CBOR maps to the same data model as JSON and MsgPack: maps, arrays, strings, numbers, booleans, null. The serializer session logic (`serializeAnything`, `serializeBeanMap`, `serializeCollection`, `serializeMap`) is structurally identical to both `JsonSerializerSession` and `MsgPackSerializerSession`, just writing different bytes.

---

## Iterator/Iterable/Stream Support

As of 9.2.1, Juneau natively supports serialization of `Iterator`, `Iterable` (non-Collection), `Enumeration`, and `java.util.stream.Stream` types.

This format uses the **materialized path**: elements are collected into a List first since the format requires knowing the array size or inspecting elements upfront.

In `serializeAnything()`, add a branch after the `isCollection()/isArray()` checks:

```java
} else if (sType.isStreamable()) {
    serializeCollection(out, toListFromStreamable(o, sType), eType);
}
```

The `toListFromStreamable()` method from `SerializerSession` handles the conversion.

---

## Scope Exclusions (Future Work)

The initial implementation targets core CBOR support. The following features are deferred:

- **Indefinite-length encoding** -- Parser support for indefinite-length arrays/maps/strings from external producers
- **Half-precision float parsing** -- Accepting `float16` input from other CBOR producers
- **CBOR sequences** -- Multiple CBOR items in a single stream (RFC 8742)
- **COSE integration** -- CBOR Object Signing and Encryption (RFC 9052)
- **BigInteger/BigDecimal semantic tags** -- RFC 8949 tags 2 and 3 for bignum integers and tag 4 for decimal fractions. Currently `BigInteger` is cast to `long` and `BigDecimal` is cast to `double` (matching `MsgPackSerializer`). A future `useTags(true)` mode would emit proper bignum/decimal-fraction tags and round-trip these types without precision loss.
- **CBOR tags for other types** -- URI (tag 32), regex (tag 35), and other application-defined tags deferred
- **Canonical CBOR** -- RFC 8949 Section 4.2 deterministic encoding
