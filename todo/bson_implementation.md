# BSON Support Implementation Plan for Apache Juneau

## Overview

This plan covers implementing serialization and parsing support for BSON (Binary JSON) in Juneau. BSON is the binary serialization format used by MongoDB. It is a self-describing binary format that includes field names, type tags, and length prefixes, making it richer than MsgPack (which Juneau already supports). BSON supports typed values (dates, binary data, regex, 32-bit vs 64-bit integers) which map cleanly to Java types.

The implementation follows the existing MsgPack serializer/parser architecture since both are binary stream-based formats, but differs in several key areas:
- **Little-endian byte order** (MsgPack uses big-endian)
- **Length-prefixed documents** (total byte count precedes content, requiring buffering)
- **Arrays encoded as documents** with numeric string keys (`"0"`, `"1"`, ...)
- **Richer type system** (typed integers, dates, binary subtypes, regex)

## Specification

- **BSON Spec**: https://bsonspec.org/spec.html (Version 1.1)
- **Media type**: `application/bson`
- **Byte order**: Little-endian

---

## BSON Type System and Java Mapping

### BSON Element Types

| Type Byte | BSON Type | Java Mapping | Notes |
|-----------|-----------|-------------|-------|
| `0x01` | Double (64-bit IEEE 754) | `double`, `float`, `Double`, `Float` | Floats promoted to double |
| `0x02` | UTF-8 String | `String`, `Enum`, `CharSequence` | Length-prefixed + null-terminated |
| `0x03` | Embedded Document | Bean, `Map<K,V>`, `JsonMap` | Recursive document encoding |
| `0x04` | Array | `List`, `Collection`, arrays | Document with `"0"`, `"1"`, ... keys |
| `0x05` | Binary Data | `byte[]`, `InputStream` | Subtype 0x00 (generic) |
| `0x07` | ObjectId (12 bytes) | `String` (hex) | Read-only (parsed as hex string) |
| `0x08` | Boolean | `boolean`, `Boolean` | 0x00=false, 0x01=true |
| `0x09` | UTC Datetime | `Date`, `Calendar`, `Instant`, `long` | int64 millis since epoch |
| `0x0A` | Null | `null` | No value bytes |
| `0x10` | 32-bit Integer | `int`, `short`, `byte`, `Integer` | 4 bytes little-endian |
| `0x12` | 64-bit Integer | `long`, `Long`, `AtomicLong` | 8 bytes little-endian |
| `0x13` | 128-bit Decimal | `BigDecimal` | 16 bytes IEEE 754-2008 |

Types NOT mapped (MongoDB-specific): Undefined (0x06, deprecated), DBPointer (0x0C, deprecated), JavaScript (0x0D), Symbol (0x0E, deprecated), JS with Scope (0x0F, deprecated), Timestamp (0x11, MongoDB internal), MinKey (0xFF), MaxKey (0x7F).

### BSON Document Structure

```
document ::= int32 e_list 0x00
             ^^^^^                total byte count (including this int32 and trailing 0x00)
                    ^^^^^^        zero or more elements
                           ^^^^   terminating null byte

element  ::= type_byte e_name value
             ^^^^^^^^^              one of the type codes above
                       ^^^^^^       cstring (null-terminated UTF-8)
                              ^^^^^ type-dependent value encoding

string   ::= int32 (byte*) 0x00
             ^^^^^                 byte count of string bytes + 1 (for null)
                    ^^^^^^^        UTF-8 string bytes
                            ^^^^   terminating null byte

cstring  ::= (byte*) 0x00
             ^^^^^^^               UTF-8 bytes (must NOT contain 0x00)
                     ^^^^           terminating null byte

binary   ::= int32 subtype (byte*)
             ^^^^^                  byte count
                    ^^^^^^^         subtype byte
                            ^^^^^^^ raw bytes
```

### Serialization Examples

**Simple bean:**
```java
class Server { String host; int port; boolean debug; }
// host="localhost", port=8080, debug=true
```
BSON encoding (shown as hex):
```
37 00 00 00                          // document size: 55 bytes
  02                                 // type: string
    68 6F 73 74 00                   // key: "host\0"
    0A 00 00 00                      // string length: 10
    6C 6F 63 61 6C 68 6F 73 74 00   // value: "localhost\0"
  10                                 // type: int32
    70 6F 72 74 00                   // key: "port\0"
    90 1F 00 00                      // value: 8080 (little-endian)
  08                                 // type: boolean
    64 65 62 75 67 00                // key: "debug\0"
    01                               // value: true
00                                   // document terminator
```

**Nested bean:**
```java
class Config { String name; Database db; }
class Database { String host; int port; }
// name="myapp", db={host="localhost", port=5432}
```
BSON: outer document contains a type 0x03 (embedded document) element for `db`.

**Collection of beans:**
```java
class Config { List<Server> servers; }
```
BSON: `servers` encoded as type 0x04 (array), which is a document with keys `"0"`, `"1"`, `"2"`, ... where each value is a type 0x03 embedded document.

**Map:**
```java
class Config { Map<String,String> env; }
// env={"PATH":"/usr/bin", "HOME":"/home/user"}
```
BSON: `env` encoded as type 0x03 (embedded document) with string entries.

---

## Architecture

BSON is a binary stream format, so it extends `OutputStreamSerializer`/`InputStreamParser` (same as MsgPack).

```
OutputStreamSerializer
  └── BsonSerializer
        └── BsonSerializerSession (uses BsonOutputStream)

InputStreamParser
  └── BsonParser
        └── BsonParserSession (uses BsonInputStream)
```

### Length-Prefix Challenge

BSON documents are length-prefixed: the total byte count must appear before the content. Since we don't know the size until the document is fully serialized, the implementation uses a **buffered nested document** strategy:

1. Each document/sub-document is serialized into a `ByteArrayOutputStream`.
2. Once complete, the total size (4 bytes for int32 + buffered content + 1 byte for terminator) is calculated.
3. The int32 size, buffered bytes, and 0x00 terminator are written to the parent stream.

`BsonOutputStream` manages this nesting internally, making the serialization code clean.

---

## Files to Create

All source files in `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/bson/`.

### 1. `BsonOutputStream.java`

Extends `OutputStream`. Core binary encoder for BSON format (little-endian).

```java
package org.apache.juneau.bson;

public class BsonOutputStream extends OutputStream {

    // Document lifecycle
    void startDocument();                  // Begin buffering a new document
    byte[] endDocument();                  // Finalize: returns [int32_size][content][0x00]
    void writeDocumentTo(OutputStream os); // Finalize and write to parent stream

    // Element header
    void writeElement(int type, String name);  // Writes type byte + cstring name

    // Scalar value writers (little-endian)
    void writeDouble(double value);        // 8 bytes IEEE 754 LE
    void writeInt32(int value);            // 4 bytes LE
    void writeInt64(long value);           // 8 bytes LE
    void writeBoolean(boolean value);      // 1 byte: 0x00 or 0x01
    void writeNull();                      // No value bytes (type byte handles it)
    void writeString(String value);        // int32 length + UTF-8 bytes + 0x00
    void writeCString(String value);       // UTF-8 bytes + 0x00
    void writeBinary(byte[] data);         // int32 length + subtype(0x00) + bytes
    void writeDateTime(long millis);       // int64 LE (UTC millis since epoch)

    // Sub-document and array support
    BsonOutputStream createChild();        // Creates a new buffered child stream
    void writeChildDocument(BsonOutputStream child); // Writes child's content as document

    // Utility
    void writeLE4(int value);              // 4-byte little-endian int
    void writeLE8(long value);             // 8-byte little-endian long
}
```

The key difference from MsgPack's `MsgPackOutputStream`: BSON is **little-endian** and **length-prefixed**. Each document is buffered in a `ByteArrayOutputStream` child. When the document is finalized, the total size is prepended.

### 2. `BsonInputStream.java`

Extends `ParserInputStream`. Core binary decoder for BSON format.

```java
package org.apache.juneau.bson;

public class BsonInputStream extends ParserInputStream {

    // Document reading
    int readDocumentSize();                // Reads int32 document size
    int readElementType();                 // Reads 1-byte type code
    String readElementName();              // Reads cstring (null-terminated)
    boolean isDocumentEnd();               // Next byte is 0x00 (document terminator)
    void readDocumentTerminator();         // Consumes the 0x00

    // Value readers (little-endian)
    double readDouble();                   // 8 bytes IEEE 754 LE
    int readInt32();                       // 4 bytes LE
    long readInt64();                      // 8 bytes LE
    boolean readBoolean();                 // 1 byte
    String readString();                   // int32 length + UTF-8 bytes + 0x00
    String readCString();                  // UTF-8 bytes until 0x00
    byte[] readBinary();                   // int32 length + subtype + bytes
    long readDateTime();                   // int64 LE (millis since epoch)
    String readObjectId();                 // 12 bytes → hex string
    BigDecimal readDecimal128();           // 16 bytes IEEE 754-2008

    // Utility
    void skipValue(int type);              // Skip a value of the given type
    int readLE4();                         // 4-byte little-endian int
    long readLE8();                        // 8-byte little-endian long
}
```

### 3. `DataType.java`

Enum for BSON element type codes.

```java
package org.apache.juneau.bson;

public enum DataType {
    DOUBLE(0x01),
    STRING(0x02),
    DOCUMENT(0x03),
    ARRAY(0x04),
    BINARY(0x05),
    OBJECT_ID(0x07),
    BOOLEAN(0x08),
    DATETIME(0x09),
    NULL(0x0A),
    REGEX(0x0B),
    INT32(0x10),
    INT64(0x12),
    DECIMAL128(0x13),
    // Deprecated/internal types included for parsing compatibility
    UNDEFINED(0x06),
    DB_POINTER(0x0C),
    JAVASCRIPT(0x0D),
    SYMBOL(0x0E),
    JS_WITH_SCOPE(0x0F),
    TIMESTAMP(0x11),
    MIN_KEY(0xFF),
    MAX_KEY(0x7F);

    final int value;
    static DataType fromByte(int b);
}
```

### 4. `BsonSerializer.java`

Extends `OutputStreamSerializer`. Implements `BsonMetaProvider`.

```java
package org.apache.juneau.bson;

public class BsonSerializer extends OutputStreamSerializer implements BsonMetaProvider {

    public static final BsonSerializer DEFAULT = ...;

    // Inner classes for text representations (matching MsgPack pattern)
    public static class SpacedHex extends BsonSerializer { ... }
    public static class Base64 extends BsonSerializer { ... }

    public static class Builder extends OutputStreamSerializer.Builder {
        boolean addBeanTypesBson = false;
        boolean writeDatesAsDatetime = true;  // Use BSON datetime type for Date/Calendar

        protected Builder() {
            produces("application/bson");
            accept("application/bson");
        }

        public Builder addBeanTypesBson(boolean value) { ... }
        public Builder writeDatesAsDatetime(boolean value) { ... }
    }

    @Override
    public BsonSerializerSession.Builder createSession() {
        return BsonSerializerSession.create(this);
    }
}
```

Builder options:
- `addBeanTypesBson(boolean)` -- Add `_type` property to beans when type name is known
- `writeDatesAsDatetime(boolean)` -- Use BSON datetime type (0x09) for `Date`/`Calendar`/`Instant` (default: true). When false, dates are serialized as strings.

### 5. `BsonSerializerSession.java`

Extends `OutputStreamSerializerSession`. Core serialization logic.

Key methods:
- `doSerialize(SerializerPipe, Object)` -- Entry point
- `serializeAnything(BsonOutputStream, Object, ClassMeta, String, BeanPropertyMeta)` -- Central dispatch
- `serializeDocument(BsonOutputStream, BeanMap/Map)` -- Writes a BSON document
- `serializeArray(BsonOutputStream, Collection)` -- Writes a BSON array (document with numeric keys)
- `writeElement(BsonOutputStream, String, Object, ClassMeta)` -- Writes a typed element (name + value)

Serialization algorithm:
```
serializeAnything(out, object, classMeta, attrName, beanProperty):
    if null:
        out.writeElement(0x0A, attrName)  // NULL type
        return

    determine actual type via swaps, push recursion detection

    if boolean:
        out.writeElement(0x08, attrName)
        out.writeBoolean(value)

    else if int/short/byte:
        out.writeElement(0x10, attrName)
        out.writeInt32(value)

    else if long:
        out.writeElement(0x12, attrName)
        out.writeInt64(value)

    else if float/double:
        out.writeElement(0x01, attrName)
        out.writeDouble(value)

    else if String/CharSequence/Enum:
        out.writeElement(0x02, attrName)
        out.writeString(value.toString())

    else if sType.isDateOrCalendarOrTemporal() (and writeDatesAsDatetime):
        out.writeElement(0x09, attrName)
        out.writeDateTime(millis)

    else if sType.isDateOrCalendarOrTemporal() (and !writeDatesAsDatetime):
        out.writeElement(0x02, attrName)
        out.writeString(Iso8601Utils.format(value, sType, getTimeZone()))

    else if sType.isDuration():
        out.writeElement(0x02, attrName)
        out.writeString(value.toString())   // ISO 8601 duration: PT1H30M

    else if byte[]:
        out.writeElement(0x05, attrName)
        out.writeBinary(value)

    else if Bean:
        out.writeElement(0x03, attrName)
        child = out.createChild()
        child.startDocument()
        serializeBeanMap(child, toBeanMap(object), typeName)
        child.writeDocumentTo(out)

    else if Map:
        out.writeElement(0x03, attrName)
        child = out.createChild()
        child.startDocument()
        serializeMap(child, map, classMeta)
        child.writeDocumentTo(out)

    else if Collection/Array:
        out.writeElement(0x04, attrName)
        child = out.createChild()
        child.startDocument()
        serializeArray(child, collection)
        child.writeDocumentTo(out)

    else:
        out.writeElement(0x02, attrName)
        out.writeString(toString(object))  // Fallback to string

serializeBeanMap(out, beanMap, typeName):
    if typeName: write _type element
    for each (key, value) in beanMap:
        if value is null: skip (or write NULL element based on config)
        writeElement(out, key, value, propertyClassMeta)

serializeMap(out, map, classMeta):
    for each (key, value) in map:
        writeElement(out, toString(key), value, valueClassMeta)

serializeArray(out, collection):
    index = 0
    for each item in collection:
        writeElement(out, String.valueOf(index), item, elementClassMeta)
        index++
```

The root-level serialization wraps everything in a top-level document:
```
doSerialize(pipe, object):
    out = new BsonOutputStream(pipe.getOutputStream())
    out.startDocument()
    if object is Bean or Map:
        serializeBeanMap/serializeMap(out, object)
    else:
        // Wrap non-document root values in {"value": x}
        writeElement(out, "value", object, classMeta)
    out.writeDocumentTo(pipe.getOutputStream())
```

### 6. `BsonParser.java`

Extends `InputStreamParser`. Implements `BsonMetaProvider`.

```java
package org.apache.juneau.bson;

public class BsonParser extends InputStreamParser implements BsonMetaProvider {

    public static final BsonParser DEFAULT = ...;

    public static class SpacedHex extends BsonParser { ... }
    public static class Base64 extends BsonParser { ... }

    public static class Builder extends InputStreamParser.Builder {
        protected Builder() {
            consumes("application/bson");
        }
    }

    @Override
    public BsonParserSession.Builder createSession() {
        return BsonParserSession.create(this);
    }
}
```

### 7. `BsonParserSession.java`

Extends `InputStreamParserSession`. Core parsing logic.

Key methods:
- `doParse(ParserPipe, ClassMeta)` -- Entry point
- `parseAnything(ClassMeta, BsonInputStream)` -- Central dispatch
- `parseDocument(BsonInputStream, ClassMeta)` -- Reads BSON document into bean or map
- `parseArray(BsonInputStream, ClassMeta)` -- Reads BSON array into collection

Parsing algorithm:
```
doParse(pipe, type):
    is = new BsonInputStream(pipe)
    return parseAnything(type, is)

parseAnything(type, is):
    if type is Bean:
        documentSize = is.readDocumentSize()
        beanMap = newBeanMap(type)
        while not is.isDocumentEnd():
            elementType = is.readElementType()
            elementName = is.readElementName()

            if elementName == "_type":
                typeName = readTypedValue(is, elementType)
                // Resolve actual type, may rebuild beanMap
            else:
                bpm = beanMap.getProperty(elementName)
                if bpm != null:
                    value = readTypedValue(is, elementType, bpm.getClassMeta())
                    bpm.set(beanMap, elementName, value)
                else:
                    handleUnknownProperty(elementName)
                    skipValue(is, elementType)
        is.readDocumentTerminator()
        return beanMap.getBean()

    else if type is Map:
        documentSize = is.readDocumentSize()
        map = newMap(type)
        while not is.isDocumentEnd():
            elementType = is.readElementType()
            elementName = is.readElementName()
            value = readTypedValue(is, elementType, valueClassMeta)
            map.put(elementName, value)
        is.readDocumentTerminator()
        return map

    else if type is Collection:
        // BSON arrays are documents with "0", "1", ... keys
        documentSize = is.readDocumentSize()
        collection = newCollection(type)
        while not is.isDocumentEnd():
            elementType = is.readElementType()
            elementName = is.readElementName()  // "0", "1", ... (ignored)
            value = readTypedValue(is, elementType, elementClassMeta)
            collection.add(value)
        is.readDocumentTerminator()
        return collection

readTypedValue(is, elementType, targetType):
    switch elementType:
        0x01 (DOUBLE):    return is.readDouble()
        0x02 (STRING):    return is.readString()
        0x03 (DOCUMENT):  return parseAnything(targetType, is)  // recursive
        0x04 (ARRAY):     return parseAnything(targetType, is)  // recursive
        0x05 (BINARY):    return is.readBinary()
        0x07 (OBJECTID):  return is.readObjectId()  // as hex string
        0x08 (BOOLEAN):   return is.readBoolean()
        0x09 (DATETIME):  return new Date(is.readDateTime())
        0x0A (NULL):      return null
        0x10 (INT32):     return is.readInt32()
        0x12 (INT64):     return is.readInt64()
        0x13 (DECIMAL128): return is.readDecimal128()
        default:          skipValue(is, elementType)
```

### 8. `BsonMetaProvider.java`

```java
package org.apache.juneau.bson;

public interface BsonMetaProvider {
    BsonClassMeta getBsonClassMeta(ClassMeta<?> cm);
    BsonBeanPropertyMeta getBsonBeanPropertyMeta(BeanPropertyMeta bpm);
}
```

### 9. `BsonClassMeta.java` and `BsonBeanPropertyMeta.java`

Extend `ExtendedClassMeta` and `ExtendedBeanPropertyMeta`.

Initially minimal (placeholder for future BSON-specific metadata).

### 10. `annotation/Bson.java`

```java
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
public @interface Bson {
    String on() default "";
    String[] onClass() default {};
}
```

### 11. `annotation/BsonConfig.java`

```java
@Target({TYPE, METHOD})
@Retention(RUNTIME)
public @interface BsonConfig {
    String addBeanTypes() default "";
    String writeDatesAsDatetime() default "";
    int rank() default 0;
}
```

### 12. `annotation/BsonAnnotation.java` and `annotation/BsonConfigAnnotation.java`

Utility classes for programmatic annotation creation.

### 13. `Bson.java` (Marshaller)

Location: `src/main/java/org/apache/juneau/marshaller/Bson.java`

Extends `StreamMarshaller`.

```java
package org.apache.juneau.marshaller;

public class Bson extends StreamMarshaller {
    public static final Bson DEFAULT = new Bson();

    public static byte[] of(Object object) throws SerializeException { ... }
    public static <T> T to(byte[] input, Class<T> type) throws ParseException { ... }
    // ... full set of convenience methods following MsgPack pattern

    public Bson() { this(BsonSerializer.DEFAULT, BsonParser.DEFAULT); }
    public Bson(BsonSerializer s, BsonParser p) { super(s, p); }
}
```

### 14. `package-info.java`

Package-level Javadoc.

---

## Files to Modify

### 1. `BasicUniversalConfig.java`

Add `BsonSerializer.class` to `serializers` and `BsonParser.class` to `parsers`.

### 2. `BasicBsonConfig.java` (New)

```java
@Rest(
    serializers={BsonSerializer.class},
    parsers={BsonParser.class},
    defaultAccept="application/bson"
)
public interface BasicBsonConfig extends DefaultConfig {}
```

### 3. `RestClient.java`

Add `BsonSerializer.class` and `BsonParser.class` to the `universal()` method.

### 4. Context: `Context.java`

Path: `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/Context.java`

Add `import org.apache.juneau.bson.annotation.*;` and add `{@link BsonConfig}` to the list of config annotations in the `applyAnnotations()` Javadoc (alphabetically).

---

## Test Plan

All test files in `juneau-utest/src/test/java/`.

### 1. BsonOutputStream Tests: `org/apache/juneau/bson/BsonOutputStream_Test.java`

Low-level binary encoding verification.

- **a01_writeInt32** -- Little-endian 4-byte int encoding (0, 1, -1, MAX_VALUE, MIN_VALUE)
- **a02_writeInt64** -- Little-endian 8-byte long encoding
- **a03_writeDouble** -- IEEE 754 double encoding (0.0, -1.5, NaN, Infinity)
- **a04_writeBoolean** -- 0x00 for false, 0x01 for true
- **a05_writeString** -- Length-prefixed + null-terminated UTF-8 (empty, ASCII, Unicode)
- **a06_writeCString** -- Null-terminated UTF-8 (no length prefix)
- **a07_writeBinary** -- Length + subtype + raw bytes
- **a08_writeDateTime** -- 8-byte little-endian millis
- **a09_startEndDocument** -- Document: int32 size + content + 0x00 terminator
- **a10_nestedDocument** -- Child documents correctly length-prefixed
- **a11_emptyDocument** -- 5 bytes: int32(5) + 0x00
- **a12_elementHeader** -- Type byte + cstring name encoding

### 2. BsonInputStream Tests: `org/apache/juneau/bson/BsonInputStream_Test.java`

Low-level binary decoding verification.

- **b01_readInt32** -- Little-endian int32 decoding
- **b02_readInt64** -- Little-endian int64 decoding
- **b03_readDouble** -- IEEE 754 double decoding
- **b04_readBoolean** -- Boolean byte decoding
- **b05_readString** -- Length-prefixed + null-terminated string decoding
- **b06_readCString** -- Null-terminated string decoding
- **b07_readBinary** -- Binary data with subtype
- **b08_readDateTime** -- Datetime millis decoding
- **b09_readDocumentSize** -- Document size reading
- **b10_readElementType** -- Type byte reading
- **b11_readElementName** -- CString element name reading
- **b12_isDocumentEnd** -- 0x00 terminator detection
- **b13_readObjectId** -- 12-byte ObjectId to hex string
- **b14_skipValue** -- Skipping values of each type

### 3. Serializer Tests: `org/apache/juneau/bson/BsonSerializer_Test.java`

- **c01_simpleBean** -- String, int, boolean, double properties
- **c02_nestedBean** -- Nested bean as embedded document (type 0x03)
- **c03_deeplyNestedBean** -- 3+ levels of document nesting
- **c04_collectionOfStrings** -- `List<String>` as BSON array (type 0x04)
- **c05_collectionOfIntegers** -- `List<Integer>` as BSON array
- **c06_collectionOfBeans** -- `List<Bean>` as array of embedded documents
- **c07_mapProperty** -- `Map<String,String>` as embedded document
- **c08_nullValues** -- Null properties as BSON null (type 0x0A) or omitted
- **c09_booleanValues** -- Boolean encoding (0x00/0x01)
- **c10_intVsLong** -- `int` → int32 (0x10), `long` → int64 (0x12) type differentiation
- **c11_floatAndDouble** -- Both map to BSON double (type 0x01)
- **c12_dateValues** -- `Date`/`Calendar` as BSON datetime (type 0x09)
- **c13_byteArrayValues** -- `byte[]` as BSON binary (type 0x05)
- **c14_enumValues** -- Enums serialized as strings (type 0x02)
- **c15_emptyBean** -- Empty document (5 bytes)
- **c16_emptyCollections** -- Empty array document
- **c17_objectSwaps** -- Swap transformations before encoding
- **c18_typeName** -- `_type` property when `addBeanTypesBson` enabled
- **c19_arrayWithNumericKeys** -- Array elements keyed as `"0"`, `"1"`, `"2"`
- **c20_documentSizeCorrect** -- Verify int32 size prefix is accurate for various documents
- **c21_stringEscaping** -- Strings with null bytes, Unicode, multi-byte characters
- **c22_bigDecimal** -- `BigDecimal` as decimal128 (type 0x13)

### 4. Parser Tests: `org/apache/juneau/bson/BsonParser_Test.java`

- **d01_parseSimpleBean** -- Scalar elements to bean properties
- **d02_parseNestedBean** -- Embedded document to nested bean
- **d03_parseDeeplyNestedBean** -- Multi-level document nesting
- **d04_parseListOfStrings** -- BSON array to `List<String>`
- **d05_parseListOfIntegers** -- BSON array to `List<Integer>`
- **d06_parseListOfBeans** -- Array of embedded documents to `List<Bean>`
- **d07_parseMapProperty** -- Embedded document to `Map<String,String>`
- **d08_parseBooleans** -- Boolean elements to boolean properties
- **d09_parseInt32** -- int32 elements to int/short/byte properties
- **d10_parseInt64** -- int64 elements to long properties
- **d11_parseDouble** -- Double elements to float/double properties
- **d12_parseString** -- String elements to String properties
- **d13_parseDatetime** -- Datetime elements to Date/Calendar
- **d14_parseBinary** -- Binary elements to byte[]
- **d15_parseNull** -- Null elements to null property values
- **d16_parseObjectId** -- ObjectId elements to hex strings
- **d17_parseDecimal128** -- Decimal128 to BigDecimal
- **d18_parseUnknownProperties** -- Unknown element names handled gracefully
- **d19_parseEmptyDocument** -- 5-byte empty document
- **d20_parseGenericMap** -- BSON document to `JsonMap` (untyped)
- **d21_typeCoercion** -- int32 read into long property, double read into float, etc.

### 5. Round-Trip Tests: `org/apache/juneau/bson/BsonRoundTrip_Test.java`

- **e01_simpleBeanRoundTrip** -- All simple types round-trip
- **e02_nestedBeanRoundTrip** -- Nested beans round-trip
- **e03_collectionOfBeansRoundTrip** -- Bean collections round-trip
- **e04_collectionOfStringsRoundTrip** -- String lists round-trip
- **e05_mapRoundTrip** -- Map properties round-trip
- **e06_enumRoundTrip** -- Enum values round-trip
- **e07_dateRoundTrip** -- Date/Calendar round-trip through datetime
- **e08_binaryRoundTrip** -- byte[] round-trip through binary
- **e09_booleanRoundTrip** -- Boolean values round-trip
- **e10_intLongRoundTrip** -- int32/int64 type preserved on round-trip
- **e11_complexBeanRoundTrip** -- Bean with mix of all types
- **e12_objectSwapRoundTrip** -- Swapped values round-trip
- **e13_emptyCollectionsRoundTrip** -- Empty lists round-trip
- **e14_nullPropertiesRoundTrip** -- Null properties round-trip
- **e15_bigDecimalRoundTrip** -- BigDecimal round-trip through decimal128

### 6. Marshaller Tests: `org/apache/juneau/marshaller/Bson_Test.java`

- **f01_of** -- `Bson.of(bean)` serializes to byte[]
- **f02_to** -- `Bson.to(bytes, Type)` parses from byte[]
- **f03_roundTrip** -- `Bson.of()` + `Bson.to()` round-trip
- **f04_defaultInstance** -- `Bson.DEFAULT.write()` and `Bson.DEFAULT.read()`

### 7. Annotation Tests: `org/apache/juneau/bson/BsonAnnotation_Test.java`

- **g01_bsonConfig** -- `@BsonConfig(addBeanTypes="true")` enables type property
- **g02_writeDatesAsDatetime** -- `@BsonConfig(writeDatesAsDatetime="false")` writes dates as strings
- **g03_annotationEquivalency** -- Annotation equivalency checks

### 8. Edge Case Tests: `org/apache/juneau/bson/BsonEdgeCases_Test.java`

- **h01_emptyInput** -- Zero-length input returns null
- **h02_malformedDocument** -- Truncated document throws ParseException
- **h03_wrongDocumentSize** -- Mismatched size prefix handled
- **h04_unicodeStrings** -- Multi-byte UTF-8 in keys and values
- **h05_veryLongStrings** -- Strings > 65KB
- **h06_deeplyNestedDocuments** -- 10+ levels of nesting
- **h07_cyclicReferences** -- Handled by recursion detection
- **h08_optionalProperties** -- `Optional<T>` bean properties
- **h09_mapWithNonStringKeys** -- Integer/enum map keys converted to strings
- **h10_emptyString** -- Empty string correctly encoded (length=1 for null terminator)
- **h11_maxInt32Boundary** -- int at Integer.MAX_VALUE/MIN_VALUE
- **h12_largeDocument** -- Document with many elements (>100)
- **h13_bsonArrayOrdering** -- Array element ordering preserved
- **h14_deprecatedTypes** -- Parsing documents with deprecated type codes (ObjectId, Undefined)

### 9. Media Type Tests: `org/apache/juneau/bson/BsonMediaType_Test.java`

- **i01_producesCorrectMediaType** -- Serializer produces `application/bson`
- **i02_consumesCorrectMediaType** -- Parser consumes `application/bson`
- **i03_contentNegotiation** -- Correct serializer selected via Accept header

### 10. Compatibility Tests: `org/apache/juneau/bson/BsonCompatibility_Test.java`

Verify BSON output matches known-good encodings (reference vectors).

- **j01_emptyDocumentBytes** -- `05 00 00 00 00` (5 bytes)
- **j02_helloWorldBytes** -- `{"hello": "world"}` matches reference encoding
- **j03_nestedDocumentBytes** -- Nested document matches reference encoding
- **j04_arrayDocumentBytes** -- Array encoding with `"0"`, `"1"` keys matches reference
- **j05_int32Bytes** -- Known int32 encodings verified
- **j06_int64Bytes** -- Known int64 encodings verified
- **j07_doubleByes** -- Known double encodings verified
- **j08_booleanBytes** -- `08 xx 00 01` (true) and `08 xx 00 00` (false) verified

### 11. Cross-Format Round-Trip Integration

To achieve round-trip parity with JSON, BSON must be added to the **shared `TESTERS` array in `RoundTripTest_Base.java`** (not only to `RoundTripDateTime_Test.java`). `RoundTripTest_Base` is the base class extended by all round-trip test files, so adding BSON there automatically runs BSON through every round-trip test suite.

**Primary change**: Add a `RoundTrip_Tester` entry for BSON to:
- `juneau-utest/src/test/java/org/apache/juneau/a/rttests/RoundTripTest_Base.java`

This covers all subclass test files:

| Test file | What it tests |
|---|---|
| `SimpleObjects_RoundTripTest` | null, String, int, arrays, List, Map |
| `PrimitivesBeans_RoundTripTest` | Beans with primitive (int, long, boolean, etc.) fields |
| `PrimitiveObjectBeans_RoundTripTest` | Beans with boxed primitive (Integer, Long, Boolean, etc.) fields |
| `BeanProperties_RoundTripTest` | Arrays of lists, complex generics in bean fields |
| `RoundTripMaps_Test` | HashMap, TreeMap, LinkedHashMap, EnumMap, etc. |
| `Generics_RoundTripTest` | Generic types with unbound/bound type variables, Pair<A,B> |
| `OptionalObjects_RoundTripTest` | Optional<T>, OptionalInt, OptionalLong, OptionalDouble |
| `Enum_RoundTripTest` | Java enum types as standalone values and bean fields |
| `BeanInheritance_RoundTripTest` | Beans with class inheritance hierarchies |
| `Records_RoundTripTest` | Java record types |
| `Classes_RoundTripTest` | Class objects serialized as fully-qualified class name strings |
| `ReadOnlyBeans_RoundTripTest` | Read-only beans (no setters; constructed via constructor arguments) |
| `RoundTripTransformBeans_Test` | Beans with @Swap annotations |
| `RoundTripDateTime_Test` | Date/time and Duration types (Instant, ZonedDateTime, LocalDate, LocalDateTime, LocalTime, OffsetDateTime, OffsetTime, Year, YearMonth, Calendar, Date, Duration, XMLGregorianCalendar) |
| `RoundTripBeanMaps_Test` | Beans used as or containing BeanMap wrappers |
| `RoundTripBeansWithBuilders_Test` | Beans that use builder-pattern construction |
| `JsonMaps_RoundTripTest` | Classes with JsonMap-based constructor / toJsonMap() method |
| `ObjectsAsStrings_RoundTripTest` | Types that parse from strings (valueOf, fromString, parse, parseString) |
| `DTOs_RoundTripTest` | Juneau internal DTOs such as JsonSchema |
| `BeanMaps_RoundTripTest` | BeanMap wrappers |
| `TrimStrings_RoundTripTest` | trimStrings serializer/parser option |
| `NameProperty_RoundTripTest` | @NameProperty annotation |
| `ParentProperty_RoundTripTest` | @ParentProperty annotation |

**Note**: `RoundTripAddClassAttrs_Test` explicitly limits itself to text-format serializers (JSON/XML/HTML/UON/UrlEncoding) and should NOT include BSON. The `_type` polymorphism feature for BSON is covered by `BsonAnnotation_Test.java` (`g01_bsonConfig`).

---

## Documentation

### 1. Javadoc

All Javadoc follows the style established by `JsonSerializer`, `JsonParser`, and `Json.java` (richer than `MsgPackSerializer`/`MsgPackParser`). The precise structure for each class is specified below.

#### `BsonSerializer.java`

Class-level Javadoc structure (in order):

1. **Summary line**: `"Serializes POJO models to BSON (Binary JSON)."`
2. **`<h5 class='topic'>Media types</h5>`**
   - Handles `Accept` types: `application/bson`
   - Produces `Content-Type` types: `application/bson`
3. **`<h5 class='topic'>Description</h5>`** — Java-to-BSON type mapping list using `<ul class='spaced-list'>`:
   - `Map` / bean → BSON document (type `0x03`)
   - `Collection` / array → BSON array (type `0x04`, document with numeric string keys)
   - `String` / `Enum` → BSON string (type `0x02`)
   - `boolean` → BSON boolean (type `0x08`)
   - `int` / `short` / `byte` → BSON int32 (type `0x10`)
   - `long` → BSON int64 (type `0x12`)
   - `float` / `double` → BSON double (type `0x01`)
   - `Date` / `Calendar` / `Instant` → BSON datetime (type `0x09`, UTC millis) when `writeDatesAsDatetime=true`; otherwise ISO-8601 string
   - `Duration` → ISO-8601 duration string (e.g. `PT1H30M`)
   - `byte[]` → BSON binary (type `0x05`, subtype `0x00`)
   - `BigDecimal` → BSON decimal128 (type `0x13`)
   - `null` → BSON null (type `0x0A`)
   - All other types → fallback to `toString()` as BSON string
4. **`<h5 class='topic'>Behavior-specific subclasses</h5>`** — describes `SpacedHex` (spaced-hex text output for debugging) and `Base64` (BASE64 text output for text-channel transmission)
5. **`<h5 class='section'>Example:</h5>`** with `<p class='bjava'>` block:
   ```java
   // Use one of the default serializers to serialize a POJO
   byte[] bson = BsonSerializer.DEFAULT.serialize(someObject);

   // Create a custom serializer that writes dates as ISO-8601 strings instead of BSON datetime
   BsonSerializer s = BsonSerializer.create().writeDatesAsDatetime(false).build();

   // Clone an existing serializer and modify it
   s = BsonSerializer.DEFAULT.copy().addBeanTypes().build();

   // Serialize a POJO to BSON
   byte[] bson = s.serialize(someObject);
   ```
6. **`<h5 class='section'>Notes:</h5>`** — thread-safety note (`<li class='note'>This class is thread safe and reusable.`)
7. **`<h5 class='section'>Limitations:</h5>`** — see Limitations section below
8. **`<h5 class='section'>See Also:</h5>`** — link to `https://juneau.apache.org/docs/topics/BsonBasics`

Inner class Javadoc (single-line):
- `SpacedHex`: `"Default serializer, spaced-hex string output."`
- `Base64`: `"Default serializer, BASE64 string output."`
- `DEFAULT` field: `"Default serializer, all default settings."`
- `DEFAULT_SPACED_HEX` field: `"Default serializer, all default settings, spaced-hex string output."`
- `DEFAULT_BASE64` field: `"Default serializer, all default settings, BASE64 string output."`

#### `BsonParser.java`

Class-level Javadoc structure (in order):

1. **Summary line**: `"Parses BSON (Binary JSON) into POJO models."`
2. **`<h5 class='topic'>Media types</h5>`**
   - Handles `Content-Type` types: `application/bson`
3. **`<h5 class='topic'>Description</h5>`** — BSON-to-Java type mapping and coercion rules using `<ul class='spaced-list'>`:
   - BSON document (0x03) → bean or `Map`
   - BSON array (0x04) → `Collection` or Java array (numeric string keys are ignored)
   - BSON string (0x02) → `String`; also coerced to any type parseable from string
   - BSON boolean (0x08) → `boolean`
   - BSON int32 (0x10) → `int`; coerced to `long`, `short`, `byte` as needed
   - BSON int64 (0x12) → `long`; coerced to `int` if target is `int`
   - BSON double (0x01) → `double`; coerced to `float`
   - BSON datetime (0x09) → `Date`, `Calendar`, `Instant`, or `long` (millis)
   - BSON binary (0x05) → `byte[]`
   - BSON decimal128 (0x13) → `BigDecimal`
   - BSON null (0x0A) → `null`
   - BSON ObjectId (0x07) → 24-character hex `String`
   - Deprecated/unsupported types → silently skipped
4. **No Java code example block** (parsers do not show usage examples — follows `JsonParser` / `MsgPackParser` convention)
5. **`<h5 class='section'>Notes:</h5>`** — thread-safety note
6. **`<h5 class='section'>Limitations:</h5>`** — see Limitations section below
7. **`<h5 class='section'>See Also:</h5>`** — link to `https://juneau.apache.org/docs/topics/BsonBasics`

Inner class Javadoc (single-line):
- `SpacedHex`: `"Default parser, string input encoded as spaced-hex."`
- `Base64`: `"Default parser, string input encoded as BASE64."`
- `DEFAULT` field: `"Default parser, all default settings."`
- `DEFAULT_SPACED_HEX` field: `"Default parser, all default settings, spaced-hex string input."`
- `DEFAULT_BASE64` field: `"Default parser, all default settings, BASE64 string input."`

#### `Bson.java` (marshaller)

Class-level Javadoc structure (follows `Json.java` / `MsgPack.java` pattern):

1. **Summary line**: `"A pairing of a {@link BsonSerializer} and {@link BsonParser} into a single class with convenience read/write methods."`
2. **Brief prose paragraph**: explains the simplified API for reading and writing POJOs
3. **`<h5 class='figure'>Examples:</h5>`** with two `<p class='bjava'>` blocks:
   - Block 1 — instance usage:
     ```java
     // Using instance.
     Bson bson = new Bson();
     MyPojo myPojo = bson.read(bytes, MyPojo.class);
     byte[] bytes = bson.write(myPojo);
     ```
   - Block 2 — DEFAULT static usage:
     ```java
     // Using DEFAULT instance.
     MyPojo myPojo = Bson.DEFAULT.read(bytes, MyPojo.class);
     byte[] bytes = Bson.DEFAULT.write(myPojo);
     ```
4. **No format output figures** — BSON is binary; spaced-hex representation would reduce readability. Omit format-output `<h5 class='figure'>` blocks (unlike `Json.java`, matching `MsgPack.java` pattern).
5. **`<h5 class='section'>See Also:</h5>`** — link to `https://juneau.apache.org/docs/topics/Marshallers`

#### `BsonOutputStream.java` / `BsonInputStream.java`

Class-level Javadoc:
- Binary encoding details and little-endian byte order rationale
- Document buffering strategy for length prefixes (why `ByteArrayOutputStream` children are used)
- Not public API; document intended for implementors

#### Limitations (Javadoc section for `BsonSerializer` and `BsonParser`)

Both `BsonSerializer` and `BsonParser` must include a `<h5 class='section'>Limitations:</h5>` section covering:

| Limitation | Detail |
|---|---|
| Root must be a document | BSON requires a top-level document. Standalone scalars, collections, and arrays serialized as the root value are automatically wrapped as `{"_value": x}`. Round-trip is supported via this convention, but the wire format differs from standalone bean/map serialization. |
| Map keys must be strings | BSON document field names are always strings. Non-string map keys (e.g., `Map<Integer, String>`) are converted via `toString()`, matching JSON behavior. |
| ObjectId is read-only | BSON ObjectId values (type `0x07`) encountered during parsing are returned as 24-character hex strings. Juneau does not generate ObjectIds during serialization. |
| Deprecated MongoDB types skipped | BSON types deprecated by MongoDB (Undefined `0x06`, DBPointer `0x0C`, Symbol `0x0E`, JS with Scope `0x0F`) are silently skipped during parsing. |
| MongoDB-internal types unsupported | JavaScript `0x0D`, Timestamp `0x11`, MinKey `0xFF`, MaxKey `0x7F` are not supported (see Scope Exclusions section). |
| No human-readable text output | BSON is a binary format. The `SpacedHex` and `Base64` variants provide text-encodable representations for debugging and text-channel transmission. |

### 2. Package Javadoc (`package-info.java`)

- What BSON is and its relationship to MongoDB
- Complete type mapping table
- Usage examples for serialization and parsing
- Comparison with MsgPack (both binary, but different encodings)
- Comparison with JSON (binary vs text, typed vs untyped integers)
- When to use BSON vs MsgPack vs JSON
- REST integration examples

### 3. Update Release Notes
/Users/james.bognar/git/apache/juneau/docs/pages/release-notes

### 4. Update Documentation

Find how existing languages are documented in the following location and update the documentation to match
the same level of detail.
/Users/james.bognar/git/apache/juneau/docs/pages/topics



---

## Implementation Order

1. **`DataType.java`** -- BSON type code enum
2. **`BsonOutputStream.java`** -- Binary encoder with document buffering
3. **`BsonInputStream.java`** -- Binary decoder
4. **`BsonOutputStream_Test.java`** -- Verify binary encoding (12 test cases)
5. **`BsonInputStream_Test.java`** -- Verify binary decoding (14 test cases)
6. **`BsonSerializer.java`** and **`BsonSerializerSession.java`** -- Core serialization
7. **`BsonParser.java`** and **`BsonParserSession.java`** -- Core parsing
8. **`BsonMetaProvider.java`**, **`BsonClassMeta.java`**, **`BsonBeanPropertyMeta.java`** -- Metadata
9. **`annotation/Bson.java`**, **`annotation/BsonConfig.java`** -- Annotations
10. **`annotation/BsonAnnotation.java`**, **`annotation/BsonConfigAnnotation.java`** -- Annotation utilities
11. **`Bson.java`** (marshaller) -- Convenience API
12. **`package-info.java`** -- Package documentation
12a. **`RoundTripTest_Base.java`** -- Add BSON `RoundTrip_Tester` entry to the shared `TESTERS` array (ensures all round-trip test suites include BSON before format-specific round-trip tests are written)
13. **Serializer tests** (`BsonSerializer_Test.java`) -- 22 test cases
14. **Parser tests** (`BsonParser_Test.java`) -- 21 test cases
15. **Round-trip tests** (`BsonRoundTrip_Test.java`) -- 15 test cases
16. **Compatibility tests** (`BsonCompatibility_Test.java`) -- 8 test cases
17. **Marshaller tests** (`Bson_Test.java`)
18. **Annotation tests** (`BsonAnnotation_Test.java`)
19. **Edge case tests** (`BsonEdgeCases_Test.java`) -- 14 test cases
20. **Media type tests** (`BsonMediaType_Test.java`)
21. **REST integration** (`BasicUniversalConfig`, `BasicBsonConfig`, `RestClient`)
22. **Context registration** (`BsonConfig`, `BsonConfigAnnotation`, `Context.java`)
23. **Final documentation review**

---

## Key Design Decisions

### 1. Extends OutputStreamSerializer/InputStreamParser (binary format)

BSON is a binary format like MsgPack. It extends `OutputStreamSerializer`/`InputStreamParser`, not `WriterSerializer`/`ReaderParser`. The implementation follows the MsgPack architecture closely.

### 2. Little-endian byte order

BSON uses little-endian encoding, unlike MsgPack (big-endian) and Java's native big-endian `DataOutputStream`. All multi-byte integer and float writes must reverse byte order. `BsonOutputStream.writeLE4()` and `writeLE8()` handle this.

### 3. Length-prefixed documents require buffering

BSON documents begin with an int32 total byte count. Since the size is unknown until all elements are written, each document is serialized into a `ByteArrayOutputStream` child. When complete, the size is calculated and the `[size][content][0x00]` is written to the parent stream. This is the primary architectural difference from MsgPack, which writes element counts (not byte counts) and doesn't require buffering.

### 4. Arrays are documents with numeric string keys

BSON encodes arrays as documents: `["a", "b"]` becomes `{"0": "a", "1": "b"}`. The serializer writes numeric string keys (`"0"`, `"1"`, ...). The parser recognizes BSON array type (0x04) and ignores the keys, reading values in order into a Java collection.

### 5. Typed integers (int32 vs int64)

Unlike MsgPack (which uses variable-length encoding) or JSON (which has only `number`), BSON distinguishes 32-bit and 64-bit integers. Java `int`/`short`/`byte` map to BSON int32 (0x10), and Java `long` maps to BSON int64 (0x12). This preserves type fidelity on round-trip.

### 6. Native date support

BSON has a datetime type (0x09) that stores UTC milliseconds as int64. Java `Date`, `Calendar`, and `Instant` are serialized as BSON datetime by default (`writeDatesAsDatetime=true`). This is a significant advantage over MsgPack and JSON where dates are typically serialized as strings or numbers without type distinction.

Date/time types are now first-class built-in types in Juneau. The serializer dispatch checks `sType.isDateOrCalendarOrTemporal()` before `sType.isBean()`. When `writeDatesAsDatetime=false`, dates fall through to string serialization via `Iso8601Utils.format()`. `java.time.Duration` objects are serialized as ISO 8601 duration strings (e.g., `PT1H30M`) via the `sType.isDuration()` dispatch check and `o.toString()`. On the parser side, `Iso8601Utils.parse()` and `BeanSession.convertToMemberType()` handle conversion from strings back to the target date/time or Duration type.

### 7. BigDecimal support via decimal128

BSON's decimal128 type (0x13) maps naturally to Java `BigDecimal`. The 128-bit IEEE 754-2008 decimal format preserves exact decimal values without floating-point rounding, making BSON a good choice for financial applications.

### 8. No external dependencies

The entire BSON binary encoding/decoding is implemented in `BsonOutputStream` and `BsonInputStream`. The format is straightforward (simpler than MsgPack in some ways) -- the main complexity is the little-endian byte order and document length prefixing.

### 9. MongoDB-specific types handled gracefully

Types like ObjectId (0x07) are read as hex strings during parsing. Deprecated types (Undefined, DBPointer, Symbol, JS with Scope) are skipped with a warning. This allows parsing BSON documents produced by MongoDB without requiring MongoDB-specific Java types.

### 10. Package name: `org.apache.juneau.bson`

Matches the established format name. Media type: `application/bson`.

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

## Scope Exclusions

The following are explicitly not included:

- **MongoDB driver integration** -- No dependency on or integration with the MongoDB Java driver
- **ObjectId generation** -- ObjectIds are read (as hex strings) but not generated
- **DBRef resolution** -- MongoDB-specific reference resolution
- **Encrypted BSON values** -- Binary subtype 6
- **Compressed BSON columns** -- Binary subtype 7 (MongoDB internal)
- **BSON vectors** -- Binary subtype 9 (AI/ML specific; could be a future addition)
- **JavaScript code fields** -- Types 0x0D and 0x0F (MongoDB-specific)
- **Timestamp type** -- Type 0x11 (MongoDB replication internal)
- **MinKey/MaxKey** -- Types 0xFF/0x7F (MongoDB query internal)
