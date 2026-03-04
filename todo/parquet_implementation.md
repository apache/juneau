# Parquet Support Implementation Plan for Apache Juneau

## Overview

This plan covers implementing serialization and parsing support for Apache Parquet in Juneau. Parquet is a columnar binary storage format widely used in big data, analytics, and AI/ML data pipelines. Unlike all other Juneau serializers (which are row-oriented), Parquet stores data in a **columnar layout** where values for each bean property are grouped together, enabling efficient analytical queries and compression.

The implementation requires:
1. A minimal **Thrift Compact Protocol** encoder/decoder (Parquet metadata is Thrift-encoded)
2. A **columnar decomposition** strategy to split collections of beans into per-property columns
3. **RLE/Bit-Packing** encoder/decoder for definition levels and booleans
4. The standard Juneau serializer/parser framework

No external dependencies are added. Compression is limited to UNCOMPRESSED and GZIP (`java.util.zip` is in the JDK).

## Specification

- **Parquet Format**: https://parquet.apache.org/docs/file-format/
- **Thrift Definition**: https://github.com/apache/parquet-format/blob/master/src/main/thrift/parquet.thrift
- **Thrift Compact Protocol**: https://github.com/apache/thrift/blob/master/doc/specs/thrift-compact-protocol.md
- **Media type**: `application/vnd.apache.parquet`
- **File extension**: `.parquet`
- **Byte order**: Little-endian
- **Magic bytes**: `PAR1` (0x50 0x41 0x52 0x31)

---

## Parquet File Structure

```
+--------------------+
| "PAR1" (4 bytes)   |  Magic number
+--------------------+
| Column 1 Chunk 1   |  Row Group 1
| Column 2 Chunk 1   |
| ...                |
| Column N Chunk 1   |
+--------------------+
| Column 1 Chunk 2   |  Row Group 2 (if data is large)
| ...                |
+--------------------+
| FileMetaData       |  Thrift Compact Protocol encoded
+--------------------+
| Footer Length       |  4 bytes, little-endian int32
+--------------------+
| "PAR1" (4 bytes)   |  Magic number (repeated)
+--------------------+
```

Each column chunk contains one or more **pages**:
- **Dictionary Page** (optional, at most one per chunk)
- **Data Pages** (one or more, containing encoded values)

Each page has a **PageHeader** (Thrift-encoded) followed by page data.

---

## Fundamental Design: Collection-Oriented Serialization

Parquet is inherently **collection-oriented**: it stores many rows (records) organized by columns. This is fundamentally different from JSON/BSON/MsgPack where a single bean is the root object.

**Juneau mapping:**
- `ParquetSerializer` serializes a `Collection<Bean>` or `Bean[]` into a Parquet file
- `ParquetParser` parses a Parquet file into a `List<Bean>`
- A single bean is wrapped in a single-element collection

This is conceptually similar to CSV (tabular data) but in a binary columnar format.

### Bean-to-Parquet Schema Mapping

```java
class Server {
    String host;
    int port;
    boolean debug;
    double ratio;
}
```

Parquet schema (depth-first flattened list):
```
message root {          // SchemaElement: name="root", num_children=4
  required binary host (STRING);   // SchemaElement: type=BYTE_ARRAY, logicalType=STRING
  required int32 port;             // SchemaElement: type=INT32
  required boolean debug;          // SchemaElement: type=BOOLEAN
  required double ratio;           // SchemaElement: type=DOUBLE
}
```

Given 3 rows: `[{host="a",port=80,debug=true,ratio=1.0}, {host="b",port=443,debug=false,ratio=2.5}, ...]`

The columnar layout stores:
- Column "host": `["a", "b", ...]` (all host values together)
- Column "port": `[80, 443, ...]`
- Column "debug": `[true, false, ...]`
- Column "ratio": `[1.0, 2.5, ...]`

### Nested Bean Mapping

```java
class Config {
    String name;
    Database db;
}
class Database {
    String host;
    int port;
}
```

Parquet schema:
```
message root {
  required binary name (STRING);
  required group db {
    required binary host (STRING);
    required int32 port;
  }
}
```

Columns: `name`, `db.host`, `db.port` (leaf columns only, with dotted path).

### Type Mapping

| Java Type | Parquet Physical Type | Logical Type | Encoding |
|-----------|----------------------|-------------|----------|
| `boolean` | BOOLEAN | - | RLE |
| `byte`, `short`, `int` | INT32 | INT(8/16/32, signed) | PLAIN |
| `long` | INT64 | INT(64, signed) | PLAIN |
| `float` | FLOAT | - | PLAIN |
| `double` | DOUBLE | - | PLAIN |
| `String` | BYTE_ARRAY | STRING (UTF8) | PLAIN |
| `Enum` | BYTE_ARRAY | ENUM | PLAIN |
| `byte[]` | BYTE_ARRAY | - | PLAIN |
| `Date`/`Calendar`/`Instant`/`Temporal` | INT64 | TIMESTAMP(MILLIS, UTC) | PLAIN |
| `Duration` | BYTE_ARRAY | STRING (UTF8) | PLAIN |
| `BigDecimal` | BYTE_ARRAY | DECIMAL(p, s) | PLAIN |
| `UUID` | FIXED_LEN_BYTE_ARRAY(16) | UUID | PLAIN |
| Nested bean | GROUP | - | (recursive) |
| `List<T>` | REPEATED group | LIST | (see below) |
| `Map<K,V>` | REPEATED group | MAP | (see below) |
| `null` | (definition level 0) | - | RLE |

### Data Structure Support vs JSON

Parquet is schema-driven and columnar, which means it has a fundamentally different data model from JSON. The table below summarizes which Java types and structures are fully supported, partially supported, or unsupported compared to JSON.

#### Supported (full round-trip)

| Java Type / Structure | Notes |
|---|---|
| `Collection<Bean>`, `Bean[]` | Native Parquet model (each bean = one row) |
| Single bean | Auto-wrapped in a one-element list |
| `boolean`, `byte`, `short`, `int`, `long`, `float`, `double` | As bean properties; primitives map to REQUIRED columns |
| `String`, `Enum`, `byte[]`, `UUID`, `BigDecimal` | As bean properties |
| `Date`, `Calendar`, `Instant`, all `java.time.*` temporal types | As bean properties; stored as TIMESTAMP(MILLIS) by default |
| `Duration` | As bean property; stored as ISO 8601 string |
| Nested beans | Mapped to Parquet GROUP columns |
| `List<T>` where T is a leaf type or nested bean | Parquet LIST group convention |
| `Map<String,V>` | Parquet MAP convention; keys must be non-null strings |
| Nullable reference-type bean properties | Handled via definition levels (OPTIONAL columns) |

#### Limitations vs JSON

The following limitations must be documented in the Javadoc for `ParquetSerializer`, `ParquetParser`, and `Parquet` (marshaller).

| Limitation | Notes |
|---|---|
| Root must be a collection | `Collection<T>`, `T[]`, or single bean (wrapped). Bare `String`, `int`, `Map`, or `null` as the root object is not supported and throws `SerializeException`. |
| Parser always returns `List<T>` | Cannot return a bare `T`. Round-trip semantics differ from all other Juneau parsers. Callers must extract element 0 when they serialized a single bean. |
| Untyped root (`JsonMap`, `Object`) not natively supported | Parquet is schema-driven; a target class must be provided at parse time. When the target type is `JsonMap` or `Object`, types are inferred from the Parquet schema (strings, longs, doubles, booleans, bytes). |
| Null map keys not supported | Parquet MAP requires non-null keys. Null keys throw `SerializeException`. |
| `List<List<T>>` (nested lists) not supported in v1 | Repetition-level encoding for nested collections is complex; excluded from the initial implementation. Throws `SerializeException`. |
| Memory-bound parsing | Parser buffers the entire file in a `byte[]` before decoding (Parquet footer is at the end, requiring random access). Files larger than available JVM heap are not supported. |
| Compression: UNCOMPRESSED and GZIP only | Snappy, LZ4, and Zstd require external libraries and are not included. |
| No streaming writes | All rows must be materialised into memory before any bytes are written (Parquet format constraint — the footer containing all metadata is written last). |

### Nullable / Optional Fields

Parquet handles nulls via **definition levels**:
- For REQUIRED fields: no definition level needed (max_def_level = 0)
- For OPTIONAL fields: definition level 0 = null, 1 = present (max_def_level = 1)

All Java bean properties with reference types are treated as OPTIONAL. Primitives are REQUIRED.

### List Encoding (Parquet LIST convention)

```java
class Config { List<String> tags; }
```
```
optional group tags (LIST) {
  repeated group list {
    optional binary element (STRING);
  }
}
```

### Map Encoding (Parquet MAP convention)

```java
class Config { Map<String,Integer> counts; }
```
```
optional group counts (MAP) {
  repeated group key_value {
    required binary key (STRING);
    optional int32 value;
  }
}
```

---

## Architecture

Parquet is a binary stream format. It extends `OutputStreamSerializer`/`InputStreamParser`.

```
OutputStreamSerializer
  └── ParquetSerializer
        └── ParquetSerializerSession
              ├── uses ParquetSchemaBuilder (bean → schema)
              ├── uses ParquetColumnWriter (writes column data)
              ├── uses RleBitPackingEncoder (definition/repetition levels, booleans)
              └── uses ThriftCompactEncoder (metadata encoding)

InputStreamParser
  └── ParquetParser
        └── ParquetParserSession
              ├── uses ParquetSchemaReader (reads schema from metadata)
              ├── uses ParquetColumnReader (reads column data)
              ├── uses RleBitPackingDecoder (definition/repetition levels, booleans)
              └── uses ThriftCompactDecoder (metadata decoding)
```

### Serialization Flow

```
1. Collect all beans into memory (needed for columnar decomposition)
2. Generate Parquet schema from bean ClassMeta
3. For each bean property (column):
   a. Extract all values for that property across all beans
   b. Encode definition levels (RLE) for nullable fields
   c. Encode values using PLAIN encoding
   d. Optionally compress page data (GZIP)
   e. Write PageHeader (Thrift) + page data
   f. Track column chunk metadata (offsets, sizes, value counts)
4. Write FileMetaData footer (Thrift Compact Protocol)
5. Write footer length (4 bytes LE) + "PAR1"
```

### Parsing Flow

```
1. Read last 8 bytes: 4-byte footer length + "PAR1" magic
2. Seek to footer offset, read FileMetaData (Thrift decode)
3. Extract schema and row group metadata
4. For each row group:
   a. For each column chunk:
      - Read PageHeader (Thrift decode)
      - Decompress page data if needed
      - Decode definition levels (RLE)
      - Decode values using appropriate encoding
   b. Reassemble rows from columnar data into beans
5. Return list of beans
```

---

## Files to Create

### Package: `org.apache.juneau.parquet`

All source files in `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/parquet/`.

#### 1. `ThriftCompactEncoder.java`

Minimal Thrift Compact Protocol encoder for writing Parquet metadata.

```java
class ThriftCompactEncoder {
    ThriftCompactEncoder(OutputStream out);

    // Field writing (with delta-encoded field IDs)
    void writeFieldBegin(int fieldType, int fieldId);
    void writeFieldStop();

    // Type writers
    void writeBool(boolean value);           // Encoded in field header
    void writeByte(int value);               // 1 byte
    void writeI16(short value);              // Zigzag varint
    void writeI32(int value);                // Zigzag varint
    void writeI64(long value);               // Zigzag varint
    void writeDouble(double value);          // 8 bytes LE (IEEE 754)
    void writeBinary(byte[] data);           // Varint length + bytes
    void writeString(String value);          // UTF-8 binary

    // Container writers
    void writeListBegin(int elemType, int size); // Size + elem type header
    void writeStructBegin();                     // Reset field ID delta
    void writeStructEnd();                       // Restore previous field ID

    // Varint encoding
    void writeVarint(long value);            // ULEB128
    long toZigzag(long value);               // Signed → zigzag mapping

    // Thrift type codes for Compact Protocol
    static final int BOOLEAN_TRUE = 1;
    static final int BOOLEAN_FALSE = 2;
    static final int I8 = 3;
    static final int I16 = 4;
    static final int I32 = 5;
    static final int I64 = 6;
    static final int DOUBLE = 7;
    static final int BINARY = 8;
    static final int LIST = 9;
    static final int SET = 10;
    static final int MAP = 11;
    static final int STRUCT = 12;
}
```

#### 2. `ThriftCompactDecoder.java`

Minimal Thrift Compact Protocol decoder for reading Parquet metadata.

```java
class ThriftCompactDecoder {
    ThriftCompactDecoder(InputStream in);
    ThriftCompactDecoder(byte[] data);

    // Field reading
    FieldHeader readFieldHeader();     // Returns {type, fieldId} or STOP
    void skipField(int fieldType);     // Skip an unknown field

    // Type readers
    boolean readBool();
    byte readByte();
    short readI16();
    int readI32();
    long readI64();
    double readDouble();
    byte[] readBinary();
    String readString();

    // Container readers
    ListHeader readListHeader();       // Returns {elemType, size}
    void readStructBegin();
    void readStructEnd();

    // Varint decoding
    long readVarint();
    long fromZigzag(long value);

    // Helper
    static class FieldHeader { int type; int fieldId; boolean isStop; }
    static class ListHeader { int elemType; int size; }
}
```

#### 3. `RleBitPackingEncoder.java`

RLE/Bit-Packing Hybrid encoder for definition/repetition levels and booleans.

```java
class RleBitPackingEncoder {
    RleBitPackingEncoder(int bitWidth);

    void writeInt(int value);             // Buffer a value
    byte[] toByteArray();                 // Finalize and return encoded bytes
    byte[] toByteArrayWithLength();       // Prepend 4-byte LE length

    // Internal
    void flushRun();                      // Write accumulated RLE or bit-packed run
    void writeRleRun(int value, int count);
    void writeBitPackedRun(int[] values, int count);
}
```

#### 4. `RleBitPackingDecoder.java`

RLE/Bit-Packing Hybrid decoder.

```java
class RleBitPackingDecoder {
    RleBitPackingDecoder(byte[] data, int bitWidth);
    RleBitPackingDecoder(byte[] data, int offset, int length, int bitWidth);

    int readInt();                        // Read next value
    boolean hasNext();
}
```

#### 5. `ParquetSchemaBuilder.java`

Generates a Parquet schema (list of `SchemaElement` structures) from a Juneau `ClassMeta`.

```java
class ParquetSchemaBuilder {
    ParquetSchemaBuilder(BeanContext bc);

    // Build schema from bean class
    List<SchemaElement> buildSchema(ClassMeta<?> cm);

    // Internal schema element representation
    static class SchemaElement {
        String name;
        Integer type;               // Parquet physical type (null for groups)
        Integer typeLength;         // For FIXED_LEN_BYTE_ARRAY
        FieldRepetitionType repetitionType;  // REQUIRED, OPTIONAL, REPEATED
        Integer numChildren;        // Non-null for group nodes
        ConvertedType convertedType;
        LogicalType logicalType;

        void writeTo(ThriftCompactEncoder encoder);
    }

    // Type mapping
    int javaToParquetType(ClassMeta<?> cm);
    LogicalType javaToLogicalType(ClassMeta<?> cm);
    FieldRepetitionType javaToRepetition(ClassMeta<?> cm, boolean isPrimitive);
}
```

#### 6. `ParquetColumnWriter.java`

Writes column values using PLAIN encoding, with definition level support.

```java
class ParquetColumnWriter {
    ParquetColumnWriter(SchemaElement schema, CompressionCodec codec);

    // Value writers (PLAIN encoding, little-endian)
    void writeBoolean(boolean value);
    void writeInt32(int value);
    void writeInt64(long value);
    void writeFloat(float value);
    void writeDouble(double value);
    void writeByteArray(byte[] value);
    void writeFixedLenByteArray(byte[] value);
    void writeNull();                     // Writes definition level 0

    // Page management
    byte[] finalizePage();                // Returns encoded page data
    PageHeader createPageHeader(int numValues, int uncompressedSize, int compressedSize);

    // Accessors
    int getValueCount();
    long getUncompressedSize();
    long getCompressedSize();
}
```

#### 7. `ParquetColumnReader.java`

Reads column values from page data.

```java
class ParquetColumnReader {
    ParquetColumnReader(SchemaElement schema, byte[] pageData, int numValues,
                        CompressionCodec codec, int maxDefLevel);

    // Value readers
    boolean readBoolean();
    int readInt32();
    long readInt64();
    float readFloat();
    double readDouble();
    byte[] readByteArray();
    byte[] readFixedLenByteArray(int length);
    boolean isNull();                    // Check definition level

    boolean hasNext();
}
```

#### 8. `ParquetSerializer.java`

Extends `OutputStreamSerializer`. Implements `ParquetMetaProvider`.

```java
public class ParquetSerializer extends OutputStreamSerializer implements ParquetMetaProvider {

    public static final ParquetSerializer DEFAULT = ...;

    public static class Builder extends OutputStreamSerializer.Builder {
        CompressionCodec compressionCodec = UNCOMPRESSED;
        int rowGroupSize = 128 * 1024 * 1024;  // 128 MB default
        int pageSize = 1024 * 1024;             // 1 MB default
        boolean addBeanTypesParquet = false;
        boolean writeDatesAsTimestamp = true;

        protected Builder() {
            produces("application/vnd.apache.parquet");
            accept("application/vnd.apache.parquet");
        }

        public Builder compressionCodec(CompressionCodec value) { ... }
        public Builder rowGroupSize(int bytes) { ... }
        public Builder pageSize(int bytes) { ... }
    }

    @Override
    public ParquetSerializerSession.Builder createSession() { ... }
}
```

Builder options:
- `compressionCodec(CompressionCodec)` -- UNCOMPRESSED (default) or GZIP
- `rowGroupSize(int)` -- Target row group size in bytes (default 128MB)
- `pageSize(int)` -- Target page size in bytes (default 1MB)
- `addBeanTypesParquet(boolean)` -- Add `_type` column for polymorphic beans
- `writeDatesAsTimestamp(boolean)` -- Use TIMESTAMP(MILLIS) for Date/Calendar/Temporal (default true). When false, dates are serialized as ISO 8601 strings via `Iso8601Utils.format()`. `Duration` objects are always serialized as ISO 8601 duration strings (e.g., `PT1H30M`) since Parquet has no native duration type.

#### 9. `ParquetSerializerSession.java`

Extends `OutputStreamSerializerSession`. Core serialization logic.

Key methods:
- `doSerialize(SerializerPipe, Object)` -- Entry point; collects beans, decomposes to columns, writes file
- `collectBeans(Object)` -- Converts root object to `List<BeanMap>` (supports Collection, array, single bean)
- `buildSchema(ClassMeta)` -- Generates Parquet schema from bean metadata
- `writeColumnChunk(OutputStream, List<BeanMap>, SchemaElement, String)` -- Writes one column's data
- `writeFileMetaData(OutputStream, FileMetaData)` -- Writes Thrift-encoded footer

Serialization algorithm:
```
doSerialize(pipe, object):
    out = pipe.getOutputStream()

    // 1. Magic number
    out.write("PAR1")

    // 2. Collect all beans
    beans = collectBeans(object)  // List<BeanMap>

    // 3. Build schema from bean class
    schema = buildSchema(classMeta)
    leafColumns = getLeafColumns(schema)  // Only leaf nodes have data

    // 4. Write row group(s)
    rowGroupOffset = 4  // After magic
    for each row group (split beans by rowGroupSize):
        columnChunks = []
        for each leafColumn in leafColumns:
            chunkOffset = currentFilePosition
            // Extract values for this column from all beans in the group
            values = extractColumnValues(beans, leafColumn.path)
            // Write column chunk (pages with encoded data)
            chunkMeta = writeColumnChunk(out, values, leafColumn)
            columnChunks.add(chunkMeta)
        rowGroups.add(new RowGroup(columnChunks, numRows))

    // 5. Write FileMetaData (Thrift Compact Protocol)
    footerOffset = currentFilePosition
    fileMetaData = new FileMetaData(version=1, schema, numRows, rowGroups)
    footerBytes = thriftEncode(fileMetaData)
    out.write(footerBytes)

    // 6. Write footer length + magic
    out.writeLE4(footerBytes.length)
    out.write("PAR1")

writeColumnChunk(out, values, column):
    columnWriter = new ParquetColumnWriter(column, compressionCodec)

    // Write definition levels (for nullable columns)
    defLevels = new RleBitPackingEncoder(1)
    for each value in values:
        if value == null:
            defLevels.writeInt(0)
        else:
            defLevels.writeInt(1)

    // Write values using PLAIN encoding
    for each value in values:
        if value != null:
            columnWriter.writeTypedValue(value, column.type)

    // Create data page
    defLevelBytes = defLevels.toByteArrayWithLength()
    valueBytes = columnWriter.getEncodedValues()
    pageData = concat(defLevelBytes, valueBytes)  // No repetition levels for flat

    // Compress if needed
    compressedPage = compress(pageData, compressionCodec)

    // Write PageHeader (Thrift) + compressed page
    pageHeader = createDataPageHeader(values.size, pageData.length, compressedPage.length)
    thriftEncode(pageHeader, out)
    out.write(compressedPage)

    return columnChunkMetadata
```

#### 10. `ParquetParser.java`

Extends `InputStreamParser`. Implements `ParquetMetaProvider`.

```java
public class ParquetParser extends InputStreamParser implements ParquetMetaProvider {

    public static final ParquetParser DEFAULT = ...;

    public static class Builder extends InputStreamParser.Builder {
        protected Builder() {
            consumes("application/vnd.apache.parquet");
        }
    }

    @Override
    public ParquetParserSession.Builder createSession() { ... }
}
```

#### 11. `ParquetParserSession.java`

Extends `InputStreamParserSession`. Core parsing logic.

Parsing requires **random access** to the file (read footer at end, then seek to column chunks). Since `InputStreamParser` receives a stream, the implementation reads the entire input into a byte array first.

Key methods:
- `doParse(ParserPipe, ClassMeta)` -- Entry point
- `readFileMetaData(byte[], int)` -- Reads Thrift-encoded footer
- `readColumnChunk(byte[], ColumnChunkMetaData, SchemaElement)` -- Reads one column's values
- `reassembleRows(Map<String, List<Object>>, ClassMeta)` -- Converts columnar data back to beans

Parsing algorithm:
```
doParse(pipe, type):
    // 1. Read entire file into byte array (Parquet needs random access)
    fileBytes = readAllBytes(pipe)

    // 2. Validate magic numbers
    assert fileBytes starts with "PAR1" and ends with "PAR1"

    // 3. Read footer
    footerLength = readLE4(fileBytes, fileBytes.length - 8)
    footerOffset = fileBytes.length - 8 - footerLength
    fileMetaData = thriftDecode(fileBytes, footerOffset, footerLength)

    // 4. Reconstruct schema
    schema = fileMetaData.schema
    leafColumns = getLeafColumns(schema)

    // 5. Read each column's data
    columnData = new LinkedHashMap<String, List<Object>>()
    for each rowGroup in fileMetaData.rowGroups:
        for each columnChunk in rowGroup.columns:
            column = findColumn(leafColumns, columnChunk.pathInSchema)
            values = readColumnChunk(fileBytes, columnChunk, column)
            columnData.computeIfAbsent(column.path, []).addAll(values)

    // 6. Reassemble rows from columnar data
    numRows = fileMetaData.numRows
    List<T> result = new ArrayList()
    for i in 0..numRows:
        beanMap = newBeanMap(type)
        for each (path, values) in columnData:
            bpm = resolveProperty(beanMap, path)
            bpm.set(beanMap, path, values.get(i))
        result.add(beanMap.getBean())
    return result

readColumnChunk(fileBytes, chunkMeta, column):
    offset = chunkMeta.dataPageOffset
    numValues = chunkMeta.numValues

    // Read PageHeader (Thrift)
    pageHeader = thriftDecode(fileBytes, offset)
    offset += thriftSize

    // Read page data
    compressedData = Arrays.copyOfRange(fileBytes, offset, offset + pageHeader.compressedPageSize)
    pageData = decompress(compressedData, chunkMeta.codec)

    // Decode definition levels
    if column is OPTIONAL:
        defDecoder = new RleBitPackingDecoder(pageData, bitWidth=1)
        // Read def levels, then values

    // Decode values (PLAIN encoding)
    values = decodePlainValues(pageData, column.type, numNonNull)
    return values  // With nulls inserted based on def levels
```

#### 12. `CompressionCodec.java` (Enum)

```java
public enum CompressionCodec {
    UNCOMPRESSED(0),
    GZIP(2);

    final int thriftValue;

    byte[] compress(byte[] data) { ... }
    byte[] decompress(byte[] data, int uncompressedSize) { ... }
}
```

GZIP uses `java.util.zip.GZIPOutputStream`/`GZIPInputStream` (JDK built-in).

#### 13. `ParquetMetaProvider.java`

```java
public interface ParquetMetaProvider {
    ParquetClassMeta getParquetClassMeta(ClassMeta<?> cm);
    ParquetBeanPropertyMeta getParquetBeanPropertyMeta(BeanPropertyMeta bpm);
}
```

#### 14. `ParquetClassMeta.java` and `ParquetBeanPropertyMeta.java`

Extend `ExtendedClassMeta` and `ExtendedBeanPropertyMeta`.

`ParquetBeanPropertyMeta` fields:
- `parquetType` -- Override Parquet physical type
- `logicalType` -- Override logical type annotation

#### 15. `annotation/Parquet.java`

```java
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
public @interface Parquet {
    String parquetType() default "";    // Override physical type
    String logicalType() default "";    // Override logical type
    String on() default "";
    String[] onClass() default {};
}
```

#### 16. `annotation/ParquetConfig.java`

```java
@Target({TYPE, METHOD})
@Retention(RUNTIME)
public @interface ParquetConfig {
    String compressionCodec() default "";
    String rowGroupSize() default "";
    String pageSize() default "";
    String addBeanTypes() default "";
    int rank() default 0;
}
```

#### 17. `annotation/ParquetAnnotation.java` and `annotation/ParquetConfigAnnotation.java`

Utility classes for programmatic annotation creation.

#### 18. `Parquet.java` (Marshaller)

Location: `src/main/java/org/apache/juneau/marshaller/Parquet.java`

Extends `StreamMarshaller`.

```java
public class Parquet extends StreamMarshaller {
    public static final Parquet DEFAULT = new Parquet();

    public static byte[] of(Object object) throws SerializeException { ... }
    public static <T> T to(byte[] input, Class<T> type) throws ParseException { ... }
    // Convenience methods following MsgPack pattern

    public Parquet() { this(ParquetSerializer.DEFAULT, ParquetParser.DEFAULT); }
    public Parquet(ParquetSerializer s, ParquetParser p) { super(s, p); }
}
```

#### 19. `package-info.java`

Package-level Javadoc.

---

## Files to Modify

### 1. `BasicUniversalConfig.java`

Add `ParquetSerializer.class` to `serializers` and `ParquetParser.class` to `parsers`.

### 2. `BasicParquetConfig.java` (New)

```java
@Rest(
    serializers={ParquetSerializer.class},
    parsers={ParquetParser.class},
    defaultAccept="application/vnd.apache.parquet"
)
public interface BasicParquetConfig extends DefaultConfig {}
```

### 3. `RestClient.java`

Add `ParquetSerializer.class` and `ParquetParser.class` to the `universal()` method.

### 4. Context: `Context.java`

Path: `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/Context.java`

Add `import org.apache.juneau.parquet.annotation.*;` and add `{@link ParquetConfig}` to the list of config annotations in the `applyAnnotations()` Javadoc (alphabetically).

---

## Test Plan

All test files in `juneau-utest/src/test/java/`.

### 1. Thrift Compact Protocol Tests: `org/apache/juneau/parquet/ThriftCompact_Test.java`

- **a01_varintEncoding** -- Small values (0-127) → 1 byte, larger values → multi-byte
- **a02_zigzagEncoding** -- 0→0, -1→1, 1→2, -2→3, INT_MAX, INT_MIN
- **a03_writeReadBool** -- true/false round-trip
- **a04_writeReadByte** -- Byte values round-trip
- **a05_writeReadI32** -- Int32 zigzag varint round-trip (0, 1, -1, MAX, MIN)
- **a06_writeReadI64** -- Int64 zigzag varint round-trip
- **a07_writeReadDouble** -- IEEE 754 LE double round-trip
- **a08_writeReadString** -- UTF-8 string round-trip (empty, ASCII, Unicode)
- **a09_writeReadBinary** -- Binary data round-trip
- **a10_writeReadStruct** -- Struct with multiple fields (field delta encoding)
- **a11_writeReadNestedStruct** -- Nested struct round-trip
- **a12_writeReadList** -- List of i32 round-trip (short form ≤14, long form >14)
- **a13_fieldDeltaEncoding** -- Consecutive field IDs use short form (delta ≤15)
- **a14_fieldLongForm** -- Large field ID gap uses long form
- **a15_skipUnknownField** -- Unknown field types skipped correctly
- **a16_emptyStruct** -- Struct with only stop field

### 2. RLE/Bit-Packing Tests: `org/apache/juneau/parquet/RleBitPacking_Test.java`

- **b01_rleRun** -- Repeated value → RLE run header + value
- **b02_bitPackedRun** -- Mixed values → bit-packed groups of 8
- **b03_singleBitWidth** -- Bit width 1 (booleans / simple def levels)
- **b04_multiBitWidth** -- Bit width 2-8 for various level depths
- **b05_mixedRuns** -- Alternating RLE and bit-packed runs
- **b06_emptyInput** -- Zero values
- **b07_singleValue** -- One value
- **b08_allSameValues** -- Long run of identical values → single RLE run
- **b09_allDifferentValues** -- All distinct values → bit-packed
- **b10_roundTrip** -- Encode + decode produces original values
- **b11_largeDataSet** -- 10,000+ values
- **b12_boundaryValues** -- Max value for given bit width

### 3. Schema Builder Tests: `org/apache/juneau/parquet/ParquetSchemaBuilder_Test.java`

- **c01_flatBean** -- String, int, boolean, double → leaf elements
- **c02_nestedBean** -- Nested bean → group with children
- **c03_primitiveTypes** -- All Java primitives → correct Parquet physical types
- **c04_nullableTypes** -- Reference types → OPTIONAL repetition
- **c05_requiredTypes** -- Primitive types → REQUIRED repetition
- **c06_dateType** -- Date/Calendar → INT64 TIMESTAMP(MILLIS)
- **c07_enumType** -- Enum → BYTE_ARRAY with ENUM logical type
- **c08_stringType** -- String → BYTE_ARRAY with STRING logical type
- **c09_byteArrayType** -- byte[] → BYTE_ARRAY (no logical type)
- **c10_listType** -- `List<String>` → LIST group structure
- **c11_mapType** -- `Map<String,Integer>` → MAP group structure
- **c12_deeplyNestedBean** -- 3+ levels of nesting → correct depth-first schema

### 4. Serializer Tests: `org/apache/juneau/parquet/ParquetSerializer_Test.java`

- **d01_singleBean** -- One bean → valid Parquet file with 1 row
- **d02_multipleBeans** -- `List<Bean>` → Parquet with N rows
- **d03_emptyCollection** -- Empty list → valid Parquet with 0 rows
- **d04_booleanColumn** -- Boolean values → RLE-encoded BOOLEAN column
- **d05_intColumn** -- Integer values → PLAIN INT32 column
- **d06_longColumn** -- Long values → PLAIN INT64 column
- **d07_floatColumn** -- Float values → PLAIN FLOAT column (promoted to DOUBLE? or FLOAT)
- **d08_doubleColumn** -- Double values → PLAIN DOUBLE column
- **d09_stringColumn** -- String values → PLAIN BYTE_ARRAY column with STRING type
- **d10_nullValues** -- Null properties → definition level 0
- **d11_nestedBean** -- Nested bean columns with dotted paths
- **d12_dateColumn** -- Date values → TIMESTAMP(MILLIS)
- **d13_enumColumn** -- Enum values → BYTE_ARRAY with ENUM type
- **d14_byteArrayColumn** -- byte[] values → BYTE_ARRAY
- **d15_magicNumber** -- File starts and ends with "PAR1"
- **d16_footerStructure** -- Footer length and position correct
- **d17_gzipCompression** -- Compressed pages with GZIP codec
- **d18_uncompressed** -- Uncompressed pages (default)
- **d19_objectSwaps** -- Swap transformations before column extraction
- **d20_beanTypeColumn** -- `_type` column when `addBeanTypesParquet` enabled

### 5. Parser Tests: `org/apache/juneau/parquet/ParquetParser_Test.java`

- **e01_parseSingleRow** -- 1-row Parquet → single bean
- **e02_parseMultipleRows** -- N-row Parquet → `List<Bean>`
- **e03_parseEmptyFile** -- 0-row Parquet → empty list
- **e04_parseBooleanColumn** -- BOOLEAN column → boolean properties
- **e05_parseIntColumn** -- INT32 column → int properties
- **e06_parseLongColumn** -- INT64 column → long properties
- **e07_parseFloatColumn** -- FLOAT column → float properties
- **e08_parseDoubleColumn** -- DOUBLE column → double properties
- **e09_parseStringColumn** -- BYTE_ARRAY STRING column → String properties
- **e10_parseNullValues** -- Definition level 0 → null properties
- **e11_parseNestedBean** -- Dotted-path columns → nested bean properties
- **e12_parseDateColumn** -- TIMESTAMP(MILLIS) → Date properties
- **e13_parseEnumColumn** -- ENUM column → enum properties
- **e14_parseBinaryColumn** -- BYTE_ARRAY → byte[] properties
- **e15_parseGzipCompressed** -- Decompress GZIP pages
- **e16_parseUncompressed** -- Read uncompressed pages
- **e17_parseGenericMap** -- Untyped parse → `List<JsonMap>`
- **e18_parseColumnOrdering** -- Column order doesn't affect parsing

### 6. Round-Trip Tests: `org/apache/juneau/parquet/ParquetRoundTrip_Test.java`

- **f01_simpleBeanRoundTrip** -- All primitive types round-trip
- **f02_nestedBeanRoundTrip** -- Nested beans round-trip via columnar decomposition
- **f03_nullValuesRoundTrip** -- Null properties preserved
- **f04_collectionRoundTrip** -- `List<Bean>` with 100+ rows round-trip
- **f05_enumRoundTrip** -- Enum values round-trip
- **f06_dateRoundTrip** -- Date values round-trip through TIMESTAMP
- **f07_stringEscapingRoundTrip** -- Strings with Unicode, newlines round-trip
- **f08_emptyCollectionRoundTrip** -- Empty list round-trips
- **f09_binaryRoundTrip** -- byte[] values round-trip
- **f10_mixedNullsRoundTrip** -- Mix of null and non-null values in same column
- **f11_gzipRoundTrip** -- Compressed data round-trips
- **f12_singleBeanRoundTrip** -- Single bean (not in collection) round-trips
- **f13_largeDataSetRoundTrip** -- 1000+ rows round-trip

### 7. File Format Compliance Tests: `org/apache/juneau/parquet/ParquetCompliance_Test.java`

- **g01_magicNumberPresent** -- First 4 bytes are "PAR1", last 4 bytes are "PAR1"
- **g02_footerLengthCorrect** -- 4-byte LE int at offset -8 matches actual footer size
- **g03_fileMetaDataVersion** -- `version` field is 1
- **g04_schemaRootElement** -- First schema element is root with `num_children`
- **g05_columnChunkOffsets** -- `data_page_offset` points to valid PageHeader
- **g06_pageHeaderValid** -- PageHeader has correct type, sizes, and encoding
- **g07_valueCountConsistent** -- `num_values` in metadata matches actual data
- **g08_compressionCodecField** -- Codec field matches actual compression used
- **g09_createdByField** -- `created_by` contains "Apache Juneau" identifier

### 8. Marshaller Tests: `org/apache/juneau/marshaller/Parquet_Test.java`

- **h01_of** -- `Parquet.of(beans)` serializes to byte[]
- **h02_to** -- `Parquet.to(bytes, Type)` parses from byte[]
- **h03_roundTrip** -- `Parquet.of()` + `Parquet.to()` round-trip
- **h04_defaultInstance** -- `Parquet.DEFAULT.write()` and `Parquet.DEFAULT.read()`

### 9. Annotation Tests: `org/apache/juneau/parquet/ParquetAnnotation_Test.java`

- **i01_compressionCodecConfig** -- `@ParquetConfig(compressionCodec="GZIP")` applies
- **i02_addBeanTypes** -- `@ParquetConfig(addBeanTypes="true")` adds `_type` column
- **i03_annotationEquivalency** -- Annotation equivalency checks

### 10. Edge Case Tests: `org/apache/juneau/parquet/ParquetEdgeCases_Test.java`

- **j01_emptyInput** -- Empty/malformed input throws ParseException
- **j02_wrongMagic** -- Non-"PAR1" magic throws ParseException
- **j03_truncatedFooter** -- Truncated file throws ParseException
- **j04_veryLongStrings** -- Strings > 1MB in column
- **j05_manyColumns** -- Bean with 50+ properties
- **j06_manyRows** -- 10,000+ rows in a single row group
- **j07_allNullColumn** -- Column where every value is null
- **j08_singleRowGroup** -- All data in one row group
- **j09_cyclicReferences** -- Handled by recursion detection
- **j10_optionalProperties** -- `Optional<T>` bean properties
- **j11_unicodeStrings** -- Multi-byte UTF-8 in column values
- **j12_booleanOnlyBean** -- Bean with only boolean properties (RLE-only columns)

### 11. Media Type Tests: `org/apache/juneau/parquet/ParquetMediaType_Test.java`

- **k01_producesCorrectMediaType** -- Serializer produces `application/vnd.apache.parquet`
- **k02_consumesCorrectMediaType** -- Parser consumes `application/vnd.apache.parquet`
- **k03_contentNegotiation** -- Correct serializer selected via Accept header

---

### 12. Cross-Format Round-Trip Integration

#### `RoundTripTest_Base.TESTERS` — add entry #22

Add after the CSV tester (currently #21) in `juneau-utest/src/test/java/org/apache/juneau/a/rttests/RoundTripTest_Base.java`:

```java
tester(22, "Parquet")
    .serializer(ParquetSerializer.create().addBeanTypes())
    .parser(ParquetParser.create())
    // Parquet is collection-oriented: the parser always returns List<T>, not T.
    // returnOriginalObject() avoids comparing List<T> back to the original T.
    // Full collection round-trip tests live in ParquetRoundTrip_Test.
    .returnOriginalObject()
    // Skip types that cannot be a valid Parquet root or that require collection semantics
    .skipIf(o -> o == null
        || o instanceof String
        || o instanceof Number
        || o instanceof Boolean
        || o instanceof org.apache.juneau.collections.JsonList
        || o instanceof org.apache.juneau.collections.JsonMap)
    .build(),
```

This matches the CSV pattern: serialization is validated (a valid Parquet file is produced without error) but the comparison uses the original object because the parser's `List<T>` return type differs structurally from the input `T`.

#### `RoundTripDateTime_Test.java` and `RoundTripMaps_Test.java` per-test TESTERS

Add an equivalent entry (with `returnOriginalObject()` and the same `skipIf` predicate shown above) to the per-test `TESTERS` arrays in:
- `juneau-utest/src/test/java/org/apache/juneau/a/rttests/RoundTripDateTime_Test.java`
- `juneau-utest/src/test/java/org/apache/juneau/a/rttests/RoundTripMaps_Test.java`

These tests verify date/time, Duration, and map round-trip across all serializer/parser combinations. The Parquet entry uses `returnOriginalObject()` because the parser always returns `List<T>`; full typed collection round-trip tests are covered by the dedicated `ParquetRoundTrip_Test.java`.

## Documentation

### 1. Javadoc

#### `Parquet.java` (marshaller) — class-level Javadoc

Follow the style of `RdfThrift.java`. The class-level comment must include:

```java
/**
 * A pairing of {@link ParquetSerializer} and {@link ParquetParser} for Apache Parquet binary format.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize a list of beans to Parquet bytes</jc>
 * 	List&lt;MyBean&gt; <jv>beans</jv> = List.<jsm>of</jsm>(new MyBean(...), new MyBean(...));
 * 	byte[] <jv>bytes</jv> = Parquet.<jsm>of</jsm>(<jv>beans</jv>);
 *
 * 	<jc>// Parse Parquet bytes back into a list of beans</jc>
 * 	List&lt;MyBean&gt; <jv>parsed</jv> = Parquet.<jsm>to</jsm>(<jv>bytes</jv>, MyBean.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Using instance methods</jc>
 * 	Parquet <jv>m</jv> = Parquet.<jsf>DEFAULT</jsf>;
 * 	<jv>bytes</jv> = <jv>m</jv>.write(<jv>beans</jv>);
 * 	<jv>parsed</jv> = <jv>m</jv>.read(<jv>bytes</jv>, MyBean.<jk>class</jk>);
 * </p>
 *
 * <p>Output is binary (<jk>byte</jk>[]), Apache Parquet columnar format.</p>
 *
 * <p class='warnbox'>
 * 	<b>Note:</b> Parquet is <b>collection-oriented</b>. The serializer accepts a
 * 	{@link java.util.Collection} or array of beans (each bean is one row). A single
 * 	bean is automatically wrapped in a one-element list. The parser <em>always</em>
 * 	returns a {@code List<T>}, never a bare {@code T}.
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ParquetBasics">Parquet Basics</a>
 * 	<li class='link'>{doc jm.Marshallers}
 * </ul>
 */
```

#### `ParquetSerializer.java` — class-level Javadoc

```java
/**
 * Serializes bean collections to Apache Parquet binary format.
 *
 * <h5 class='section'>Media types:</h5>
 * <p>
 * Handles <c>Accept</c> types: <bc>application/vnd.apache.parquet</bc>
 * <p>
 * Produces <c>Content-Type</c> types: <bc>application/vnd.apache.parquet</bc>
 *
 * <h5 class='section'>Description:</h5>
 * <p>
 * Parquet is a <b>columnar</b> binary format designed for analytical workloads and data
 * engineering pipelines (Apache Spark, DuckDB, AWS Athena, etc.). Unlike row-oriented
 * formats (JSON, XML, MsgPack), Parquet groups all values for each bean property into a
 * column chunk, enabling efficient compression and predicate pushdown.
 *
 * <h5 class='section'>Collection-oriented semantics:</h5>
 * <p>
 * Parquet is tabular: each bean is one row and each bean property is one column.
 * The serializer accepts a {@link java.util.Collection}&lt;T&gt; or {@code T[]} at the
 * root; a single bean is automatically wrapped in a one-element list.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize a list of Server beans to Parquet bytes</jc>
 * 	List&lt;Server&gt; <jv>servers</jv> = getServers();
 * 	byte[] <jv>bytes</jv> = ParquetSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>servers</jv>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Custom configuration — GZIP compression, 64 MB row groups</jc>
 * 	ParquetSerializer <jv>s</jv> = ParquetSerializer
 * 		.<jsm>create</jsm>()
 * 		.compressionCodec(CompressionCodec.<jsf>GZIP</jsf>)
 * 		.rowGroupSize(64 * 1024 * 1024)
 * 		.build();
 * 	byte[] <jv>bytes</jv> = <jv>s</jv>.serialize(<jv>servers</jv>);
 * </p>
 *
 * <h5 class='section'>Java-to-Parquet type mapping:</h5>
 * <table class='styled'>
 * 	<tr><th>Java type</th><th>Parquet physical type</th><th>Logical type</th></tr>
 * 	<tr><td><jk>boolean</jk></td><td>BOOLEAN</td><td>-</td></tr>
 * 	<tr><td><jk>byte</jk>, <jk>short</jk>, <jk>int</jk></td><td>INT32</td><td>INT(8/16/32, signed)</td></tr>
 * 	<tr><td><jk>long</jk></td><td>INT64</td><td>INT(64, signed)</td></tr>
 * 	<tr><td><jk>float</jk></td><td>FLOAT</td><td>-</td></tr>
 * 	<tr><td><jk>double</jk></td><td>DOUBLE</td><td>-</td></tr>
 * 	<tr><td>{@link String}</td><td>BYTE_ARRAY</td><td>STRING (UTF-8)</td></tr>
 * 	<tr><td>{@link Enum}</td><td>BYTE_ARRAY</td><td>ENUM</td></tr>
 * 	<tr><td><jk>byte</jk>[]</td><td>BYTE_ARRAY</td><td>-</td></tr>
 * 	<tr><td>{@link java.util.Date}, {@link java.util.Calendar}, {@link java.time.Instant}, temporal types</td><td>INT64</td><td>TIMESTAMP(MILLIS, UTC)</td></tr>
 * 	<tr><td>{@link java.time.Duration}</td><td>BYTE_ARRAY</td><td>STRING (ISO 8601)</td></tr>
 * 	<tr><td>{@link java.math.BigDecimal}</td><td>BYTE_ARRAY</td><td>DECIMAL</td></tr>
 * 	<tr><td>{@link java.util.UUID}</td><td>FIXED_LEN_BYTE_ARRAY(16)</td><td>UUID</td></tr>
 * 	<tr><td>Nested bean</td><td>GROUP</td><td>-</td></tr>
 * 	<tr><td>{@link java.util.List}&lt;T&gt;</td><td>REPEATED group</td><td>LIST</td></tr>
 * 	<tr><td>{@link java.util.Map}&lt;String,V&gt;</td><td>REPEATED group</td><td>MAP</td></tr>
 * 	<tr><td><jk>null</jk></td><td>(definition level 0)</td><td>-</td></tr>
 * </table>
 *
 * <h5 class='section'>Limitations:</h5><ul>
 * 	<li class='note'>Root object must be a {@link java.util.Collection}, array, or single bean.
 * 	    Bare primitives, strings, maps, and <jk>null</jk> roots are not supported.
 * 	<li class='note'>Null map keys are not supported ({@code SerializeException} is thrown).
 * 	<li class='note'>{@code List<List<T>>} (nested lists) is not supported in this release.
 * 	<li class='note'>Compression: UNCOMPRESSED (default) and GZIP only; Snappy/LZ4/Zstd require external libraries.
 * 	<li class='note'>All rows are buffered in memory before writing (Parquet format constraint — footer is written last).
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ParquetBasics">Parquet Basics</a>
 * 	<li class='link'><a class="doclink" href="https://parquet.apache.org/docs/file-format/">Parquet File Format Specification</a>
 * </ul>
 */
```

#### `ParquetParser.java` — class-level Javadoc

```java
/**
 * Parses Apache Parquet binary data into bean collections.
 *
 * <h5 class='section'>Media types:</h5>
 * <p>
 * Handles <c>Content-Type</c> types: <bc>application/vnd.apache.parquet</bc>
 *
 * <h5 class='section'>Description:</h5>
 * <p>
 * Reads a Parquet file and reassembles its columnar data into a {@code List<T>}.
 * Each row in the file becomes one element of the returned list.
 *
 * <p class='warnbox'>
 * 	<b>Important:</b> This parser <em>always</em> returns a {@code List<T>}, not a bare
 * 	{@code T}. When parsing a single-bean Parquet file, callers must extract the first element:
 * 	<p class='bjava'>
 * 	List&lt;MyBean&gt; <jv>rows</jv> = ParquetParser.<jsf>DEFAULT</jsf>.parse(<jv>bytes</jv>, MyBean.<jk>class</jk>);
 * 	MyBean <jv>bean</jv> = <jv>rows</jv>.get(0);
 * 	</p>
 * </p>
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Parse Parquet bytes into a typed list</jc>
 * 	List&lt;Server&gt; <jv>servers</jv> = ParquetParser.<jsf>DEFAULT</jsf>.parse(<jv>bytes</jv>, Server.<jk>class</jk>);
 * </p>
 * <p class='bjava'>
 * 	<jc>// Untyped parse — returns List&lt;JsonMap&gt; with column types inferred from the Parquet schema</jc>
 * 	List&lt;JsonMap&gt; <jv>rows</jv> = ParquetParser.<jsf>DEFAULT</jsf>.parse(<jv>bytes</jv>, JsonMap.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>Limitations:</h5><ul>
 * 	<li class='note'>The entire file is read into a {@code byte[]} before parsing (Parquet's footer is at
 * 	    the end of the file and requires random access). Files larger than available JVM heap are not supported.
 * 	<li class='note'>Compression: UNCOMPRESSED and GZIP only; Snappy/LZ4/Zstd not supported.
 * 	<li class='note'>{@code List<List<T>>} (nested lists) not supported in this release.
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ParquetBasics">Parquet Basics</a>
 * 	<li class='link'><a class="doclink" href="https://parquet.apache.org/docs/file-format/">Parquet File Format Specification</a>
 * </ul>
 */
```

**`ThriftCompactEncoder.java` / `ThriftCompactDecoder.java`:**
- Internal classes (package-private)
- Thrift Compact Protocol encoding rules
- Zigzag varint, field delta encoding

**`RleBitPackingEncoder.java` / `RleBitPackingDecoder.java`:**
- Internal classes (package-private)
- RLE/Bit-Packing Hybrid encoding algorithm
- When each mode is used

### 2. Package Javadoc (`package-info.java`)

- What Parquet is and why it matters (analytics, AI/ML, data engineering)
- Columnar storage model explanation with diagrams
- Complete type mapping table
- Usage examples (serializing a list of beans, parsing from bytes)
- Compression options
- Comparison with CSV, JSON, MsgPack, BSON
- Limitations and scope
- REST integration examples

### 3. Update Release Notes
/Users/james.bognar/git/apache/juneau/docs/pages/release-notes

### 4. Update Documentation

Find how existing languages are documented in the following location and update the documentation to match
the same level of detail.
/Users/james.bognar/git/apache/juneau/docs/pages/topics

---

## Implementation Order

1. **`ThriftCompactEncoder.java`** + **`ThriftCompactDecoder.java`** -- Foundation for all metadata
2. **`ThriftCompact_Test.java`** -- Verify Thrift encoding/decoding (16 test cases)
3. **`RleBitPackingEncoder.java`** + **`RleBitPackingDecoder.java`** -- For def levels and booleans
4. **`RleBitPacking_Test.java`** -- Verify RLE encoding/decoding (12 test cases)
5. **`CompressionCodec.java`** -- UNCOMPRESSED and GZIP
6. **`ParquetSchemaBuilder.java`** -- Bean-to-Parquet schema generation
7. **`ParquetSchemaBuilder_Test.java`** -- Verify schema generation (12 test cases)
8. **`ParquetColumnWriter.java`** -- Column value encoding
9. **`ParquetColumnReader.java`** -- Column value decoding
10. **`ParquetSerializer.java`** + **`ParquetSerializerSession.java`** -- Core serialization
11. **`ParquetParser.java`** + **`ParquetParserSession.java`** -- Core parsing
12. **Serializer tests** (20 test cases)
13. **Parser tests** (18 test cases)
14. **Round-trip tests** (13 test cases)
15. **File format compliance tests** (9 test cases)
16. **`ParquetMetaProvider.java`**, **`ParquetClassMeta.java`**, **`ParquetBeanPropertyMeta.java`** -- Metadata
17. **`annotation/Parquet.java`**, **`annotation/ParquetConfig.java`** -- Annotations
18. **`annotation/ParquetAnnotation.java`**, **`annotation/ParquetConfigAnnotation.java`** -- Annotation utilities
19. **`Parquet.java`** (marshaller) -- Convenience API
20. **`package-info.java`** -- Package documentation
21. **Marshaller tests, annotation tests, edge case tests, media type tests**
22. **REST integration** (`BasicUniversalConfig`, `BasicParquetConfig`, `RestClient`)
23. **Context registration** (`ParquetConfig`, `ParquetConfigAnnotation`, `Context.java`)
24. **Final documentation review**

---

## Iterator/Iterable/Stream Support

As of 9.2.1, Juneau natively supports serialization of `Iterator`, `Iterable` (non-Collection), `Enumeration`, and `java.util.stream.Stream` types.

This format uses the **materialized path**: elements are collected into a List first since the format requires knowing the array size or inspecting elements upfront.

In `collectBeans()`, add a branch for streamable types alongside the existing `isCollection()/isArray()` checks:

```java
} else if (sType.isStreamable()) {
    serializeCollection(out, toListFromStreamable(o, sType), eType);
}
```

The `toListFromStreamable()` method from `SerializerSession` handles the conversion. Parquet's columnar format fundamentally requires full materialization since schema generation, column decomposition, and row group sizing all need access to the complete dataset before any bytes can be written.

---

## Key Design Decisions

### 1. Collection-oriented serialization

Parquet is a columnar format designed for tabular data. Unlike JSON/BSON/MsgPack where a single bean is the root, Parquet serializes collections of beans where each bean is a row. The serializer accepts `Collection<Bean>`, `Bean[]`, or a single bean (wrapped in a 1-element list). The parser always returns a `List<Bean>`. This is the only Juneau serializer with this semantic (similar to CSV but binary).

### 2. Extends OutputStreamSerializer/InputStreamParser

Binary format requiring `OutputStreamSerializer`/`InputStreamParser`. However, the parser needs random access to the file (footer is at the end), so it reads the entire input into a `byte[]` first. This limits parseable file size to available memory but is acceptable for Juneau's typical use cases (REST payloads, not multi-gigabyte data lakes).

### 3. Minimal Thrift Compact Protocol implementation

Parquet metadata (FileMetaData, PageHeader, SchemaElement, etc.) is encoded using the Thrift Compact Protocol. Rather than adding a Thrift dependency, a focused encoder/decoder is implemented that supports only the types needed by Parquet metadata: bool, i8, i16, i32, i64, double, binary/string, list, struct. This is approximately 200-300 lines of code for each direction.

### 4. PLAIN encoding only (initial implementation)

Parquet supports many encodings (PLAIN, RLE, DICTIONARY, DELTA_BINARY_PACKED, etc.). The initial implementation uses only PLAIN encoding for values (and RLE for definition levels and booleans). PLAIN is the mandatory baseline encoding all Parquet readers must support, ensuring maximum compatibility. Dictionary and delta encodings can be added as future optimizations.

### 5. UNCOMPRESSED and GZIP compression only

Java provides `java.util.zip.GZIPOutputStream`/`GZIPInputStream` in the JDK, so GZIP is available without external dependencies. Snappy, LZ4, and Zstd require external libraries and are not included. UNCOMPRESSED is the default for simplicity and debuggability.

### 6. Single row group for typical use cases

For Juneau's typical REST payload sizes (KB to low MB), all data fits in a single row group. The `rowGroupSize` builder option allows splitting into multiple row groups for larger datasets, but the default handles the common case simply.

### 7. Data Page V1

Data Page V1 is simpler and more widely supported than V2. The initial implementation uses V1 exclusively. V2 could be added later.

### 8. Flat and simple nested schemas

The initial implementation supports flat beans (leaf columns only) and one level of bean nesting (groups). Deep nesting, lists of beans, and maps are supported through Parquet's standard LIST and MAP group conventions, with definition and repetition levels managed accordingly.

### 9. No streaming writes

Because Parquet's file format requires the total document size and metadata at the end, all beans must be collected into memory before writing. This is fundamentally different from streaming formats like JSON Lines. This is a Parquet format constraint, not an implementation limitation.

### 10. Package name: `org.apache.juneau.parquet`

Matches the format name. Media type: `application/vnd.apache.parquet`.

---

## Scope Exclusions

- **Streaming/incremental writes** -- Parquet requires footer-at-end; all rows must be buffered
- **Multi-gigabyte files** -- Parser reads entire file into memory (OK for REST payloads)
- **Snappy/LZ4/Zstd compression** -- Requires external dependencies
- **Dictionary encoding** -- Future optimization (PLAIN-only initially)
- **Delta encoding** -- Future optimization
- **Bloom filters** -- Advanced query optimization feature
- **Page indexes (ColumnIndex/OffsetIndex)** -- Advanced query optimization
- **Encryption** -- AES-GCM encryption support
- **INT96 timestamps** -- Deprecated, not written (but could be read for compatibility)
- **Column projection/predicate pushdown** -- Query optimization (parser reads all columns)
- **Parquet metadata files (_metadata)** -- Multi-file Parquet datasets
- **Row group splitting across files** -- Single-file output only
