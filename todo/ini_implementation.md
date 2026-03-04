# INI File Support Implementation Plan for Apache Juneau

## Overview

INI is a classic configuration file format with sections and key-value pairs, widely used in legacy systems, Windows configuration, Python (`configparser`), PHP, and Git (`.gitconfig`). This plan covers implementing a lossless round-trip INI serializer and parser for Java beans, built as a standalone serializer/parser (not extending JSON) but using the same architectural patterns.

## Specification

- **Media types**: `text/ini`, `text/x-ini`
- **Key rules**:
  - Sections: `[sectionName]`
  - Key-value pairs: `key = value`
  - Comments: `#` at start of line
  - Blank lines for readability between sections
  - UTF-8 encoding

## Bean-to-INI Mapping Strategy

The central design challenge is mapping Java's hierarchical object model to INI's flat section/key-value structure while maintaining lossless round-trip fidelity.

### Mapping Rules

**1. Simple bean properties (strings, numbers, booleans, enums, dates) -> key-value pairs in the default (unnamed) section:**

```java
class Person { String name; int age; boolean active; }
```
```ini
name = John
age = 42
active = true
```

**2. Nested bean properties -> INI sections, where the section name is the property name:**

```java
class Person {
    String name;
    Address address;
}
class Address { String city; String state; }
```
```ini
name = John

[address]
city = Springfield
state = IL
```

**3. Deeply nested beans (2+ levels) -> dotted section names:**

```java
class Person {
    String name;
    Employment employment;
}
class Employment {
    String title;
    Company company;
}
class Company { String name; String ticker; }
```
```ini
name = John

[employment]
title = Engineer

[employment/company]
name = Acme Corp
ticker = ACME
```

Section names use `/` as a path separator (consistent with Juneau's config module which uses `section/key` paths).

**4. Collections/arrays of simple values -> JSON-encoded values:**

```java
class Person {
    String name;
    List<String> hobbies;
}
```
```ini
name = John
hobbies = ['reading','gaming','hiking']
```

Simple collections use JSON5 encoding (compact, single-quoted) for the value. This leverages Juneau's existing `Json5Serializer` internally, providing lossless type preservation without reinventing encoding.

**5. Maps with simple values -> INI sections:**

```java
class Config {
    String version;
    Map<String,String> settings;
}
```
```ini
version = 1.0

[settings]
theme = dark
language = en
debug = false
```

**6. Collections of beans / Maps with complex values -> JSON5-encoded values:**

```java
class Team {
    String name;
    List<Person> members;
}
```
```ini
name = Engineering
members = [{name:'Alice',age:30},{name:'Bob',age:25}]
```

When a value cannot be naturally represented as INI key-value pairs (collections of beans, deeply nested structures in a value position, maps with non-string keys), the value is encoded as a JSON5 string. This is the same strategy Juneau's `Config` module uses.

**7. Null values:**

```ini
name = null
```

The literal string `null` represents null. During parsing, the unquoted token `null` is interpreted as a Java null.

**8. String values that look like other types:**

Strings that could be confused with numbers, booleans, or null are quoted with single quotes:

```ini
code = '42'
flag = 'true'
nothing = 'null'
label = 'hello world'
```

Quoting rules:
- Strings containing `=`, `[`, `]`, `#`, `\n`, leading/trailing whitespace, or that parse as numbers/booleans/null are single-quoted
- Single quotes within values are escaped as `''` (doubled)

### Summary of Value Encoding

| Java Type | INI Representation | Example |
|-----------|-------------------|---------|
| String | Unquoted or single-quoted | `name = John` or `name = 'John Smith'` |
| int/long/float/double | Unquoted number | `age = 42` |
| boolean | Unquoted `true`/`false` | `active = true` |
| null | Unquoted `null` | `value = null` |
| Enum | Unquoted name | `status = ACTIVE` |
| Nested bean | Section `[propName]` | See rule 2 |
| Deep nested bean | Section `[path/to/prop]` | See rule 3 |
| Simple Map | Section `[propName]` | See rule 5 |
| List of primitives | JSON5-encoded | `tags = ['a','b','c']` |
| List of beans | JSON5-encoded | `items = [{...},{...}]` |
| Complex Map | JSON5-encoded | `data = {key:[1,2,3]}` |

---

## Data Structure Support vs JSON

This section documents which Java types are fully supported, which are supported via JSON5 fallback, and which are not supported. This drives the runtime behavior of `IniSerializer` and `IniParser`, and the limitation notices in their class-level Javadoc.

### Supported Types (native INI)

These types serialize to and parse from native INI key-value or section syntax with full round-trip fidelity:

| Java Type | INI Representation |
|---|---|
| `String` | Unquoted or single-quoted value |
| `int`, `long`, `float`, `double` (and wrappers) | Unquoted number |
| `boolean` / `Boolean` | Unquoted `true`/`false` |
| `null` | Unquoted `null` |
| `char` / `Character` | Single-character string |
| `enum` | Unquoted enum name |
| `URI` / `URL` | Unquoted URI string |
| `Date`, `Calendar`, `java.time.*` temporals | ISO 8601 string |
| `java.time.Duration` | ISO 8601 duration string (e.g. `PT1H30M`) |
| Nested bean | INI section `[propertyName]` |
| `Map<String, simple>` | INI section `[propertyName]` |
| `Optional<T>` | Unwrapped; inner type dispatched normally |

### Supported Types (via JSON5 inline fallback)

These types cannot be represented natively in INI's section/key-value structure, so they are encoded as JSON5 inline values. They are **fully round-trippable** — the parser recognizes `[...]` and `{...}` values and delegates them to `JsonParser`. The limitation is presentation only (the INI file is less human-editable for these values).

| Java Type | INI Representation |
|---|---|
| `Collection` / array of primitives | JSON5 array: `tags = ['a','b','c']` |
| `Collection` / array of beans | JSON5 array: `items = [{name:'Alice'},{name:'Bob'}]` |
| `Stream` / `Iterator` / `Iterable` | Materialized then JSON5-encoded |
| `Map<String, complex>` | JSON5 object: `data = {key:[1,2,3]}` |
| `Map<non-String-key, any>` | JSON5 object |
| Complex nested structures in value position | JSON5-encoded |

### Unsupported Types (limitations)

These types cannot be serialized to INI format at all. `IniSerializer` will throw a `SerializeException`; these limitations are documented in the class-level Javadoc.

| Limitation | JSON behavior | INI behavior |
|---|---|---|
| Top-level `Collection` or array | Serializes as `[...]` | **Not supported.** Throws `SerializeException`. Caller must wrap in a bean. |
| Top-level scalar (`String`, `int`, `boolean`, etc.) | Serializes as bare value | **Not supported.** Throws `SerializeException`. Caller must wrap in a bean. |
| `Reader` / `InputStream` passthrough | Piped verbatim | **Not supported.** Throws `SerializeException`. |

> **Summary**: All bean and map types are supported with full round-trip fidelity. Complex values (collections of beans, nested maps) are embedded as JSON5 inline strings and are still round-trippable. Only top-level non-container types and raw stream passthrough are unsupported.

---

## Architecture

INI serialization is fundamentally different from JSON -- it's not a recursive tree format but a two-level section/key structure. Therefore, `IniSerializer` extends `WriterSerializer` directly (not `JsonSerializer`), but internally delegates to `Json5Serializer` for encoding complex values that can't be represented natively in INI.

```
WriterSerializer
  └── IniSerializer
        └── IniSerializerSession (uses Json5Serializer for complex values)

ReaderParser
  └── IniParser
        └── IniParserSession (uses JsonParser for complex values)
```

### Why Not Extend JsonSerializer

Unlike JSONL (which is a line-delimited variant of JSON), INI has a completely different syntax. The `doSerialize()` method, the writer, and the parsing state machine are all different. Extending `JsonSerializer` would mean overriding virtually everything, gaining no code reuse. Instead, we:
- Extend `WriterSerializer`/`ReaderParser` directly (same base as JSON)
- Use `Json5Serializer`/`JsonParser` internally as utility for encoding/decoding complex values
- Follow the same Builder, Session, Writer, and Marshaller patterns established by JSON

---

## Files to Create

All new source files in `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/ini/`.

### 1. `IniSerializer.java`

Extends `WriterSerializer`. Implements `IniMetaProvider`.

```java
package org.apache.juneau.ini;

public class IniSerializer extends WriterSerializer implements IniMetaProvider {

    public static final IniSerializer DEFAULT = ...;
    public static final IniSerializer DEFAULT_READABLE = ...; // with blank lines between sections

    public static class Builder extends WriterSerializer.Builder {
        // INI-specific settings:
        boolean useComments;        // Include bean property descriptions as comments
        char kvSeparator = '=';     // Key-value separator ('=' or ':')
        boolean spacedSeparator = true; // " = " vs "="

        protected Builder() {
            produces("text/ini");
            accept("text/ini,text/x-ini");
        }
    }

    @Override
    public IniSerializerSession.Builder createSession() {
        return IniSerializerSession.create(this);
    }
}
```

Builder options:
- `useComments()` -- Whether to emit `#` comments from bean property descriptions
- `kvSeparator(char)` -- Use `=` (default) or `:` as key-value separator
- `spacedSeparator()` -- Whether to pad separator with spaces: `key = value` vs `key=value`

### 2. `IniSerializerSession.java`

Extends `WriterSerializerSession`. Contains the core serialization logic.

Key methods:
- `doSerialize(SerializerPipe, Object)` -- Entry point. Converts object to `BeanMap`, iterates properties, decides section vs inline encoding
- `serializeBeanMap(IniWriter, BeanMap, String)` -- Serializes a bean's properties. Simple properties go to key-value pairs; nested beans/maps become sections; complex values get JSON5-encoded
- `serializeSimpleValue(IniWriter, String, Object, ClassMeta)` -- Writes a single `key = value` line with appropriate quoting/encoding
- `serializeSectionBean(IniWriter, String, BeanMap)` -- Writes a `[sectionPath]` header followed by the bean's simple properties, then recurses for nested beans
- `encodeComplexValue(Object, ClassMeta)` -- Delegates to `Json5Serializer` for complex values
- `needsQuoting(String)` -- Determines if a string value needs single-quote wrapping

Two-pass approach for `doSerialize()`:
1. First pass: Write all simple (non-bean, non-map) properties as key-value pairs in the default section
2. Second pass: Write each bean/map property as a named section with `[sectionName]` header

This ensures the default section appears first, followed by named sections.

### 3. `IniWriter.java`

Extends `SerializerWriter`. Provides INI-specific write methods.

```java
package org.apache.juneau.ini;

public class IniWriter extends SerializerWriter {

    IniWriter section(String name);          // Writes [name] with preceding blank line
    IniWriter comment(String text);          // Writes # text
    IniWriter keyValue(String key, Object value, char sep); // Writes key = value
    IniWriter quotedString(String value);    // Writes 'value' with escaping
    IniWriter blankLine();                   // Writes empty line
}
```

### 4. `IniParser.java`

Extends `ReaderParser`. Implements `IniMetaProvider`.

```java
package org.apache.juneau.ini;

public class IniParser extends ReaderParser implements IniMetaProvider {

    public static final IniParser DEFAULT = ...;

    public static class Builder extends ReaderParser.Builder {
        protected Builder() {
            consumes("text/ini,text/x-ini");
        }
    }

    @Override
    public IniParserSession.Builder createSession() {
        return IniParserSession.create(this);
    }
}
```

### 5. `IniParserSession.java`

Extends `ReaderParserSession`. Contains the core parsing logic.

Key methods:
- `doParse(ParserPipe, ClassMeta)` -- Entry point. Reads the INI content, builds a section-keyed intermediate map, then populates a bean
- `parseIniContent(Reader)` -- Reads lines, identifies sections and key-value pairs, returns `Map<String, Map<String, String>>` (section name -> key -> raw value)
- `populateBean(BeanMap, Map, String)` -- Maps parsed sections/values to bean properties
- `parseValue(String, ClassMeta)` -- Converts a raw string value to the target Java type:
  - Unquoted `null` -> Java null
  - Unquoted `true`/`false` -> Boolean
  - Unquoted number -> Number (int, long, float, double as appropriate)
  - Single-quoted string -> String (with unescaping)
  - JSON5 array/object (`[...]` or `{...}`) -> Parsed via `JsonParser`
  - Unquoted string -> String
- `parseLine(String)` -- Splits a line into key and value, handling separator and inline comments

Parsing algorithm:
1. Read all lines into a structured map: `{ "" -> {key: value, ...}, "section1" -> {key: value, ...}, ... }`
2. If target type is a bean: iterate bean properties, look up values in default section and sub-sections
3. If target type is a map: populate directly from parsed key-value pairs
4. Section names with `/` are interpreted as nested paths

### 6. `IniMetaProvider.java`

Interface for accessing INI-specific metadata.

```java
package org.apache.juneau.ini;

public interface IniMetaProvider {
    IniClassMeta getIniClassMeta(ClassMeta<?> cm);
    IniBeanPropertyMeta getIniBeanPropertyMeta(BeanPropertyMeta bpm);
}
```

### 7. `IniClassMeta.java`

Extends `ExtendedClassMeta`. Stores class-level INI metadata from `@Ini` annotation.

Potential metadata:
- Custom section name (override property name as section header)

### 8. `IniBeanPropertyMeta.java`

Extends `ExtendedBeanPropertyMeta`. Stores property-level INI metadata from `@Ini` annotation.

Potential metadata:
- `section` -- Force this property into a specific section
- `comment` -- Comment text to emit before this key
- `encoding` -- Force JSON5 encoding even for simple values

### 9. `annotation/Ini.java`

```java
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
public @interface Ini {
    String section() default "";    // Custom section name
    String comment() default "";    // Comment to emit before this property
    String on() default "";         // Dynamic application target
    String[] onClass() default {};  // Dynamic class application
}
```

### 10. `annotation/IniConfig.java`

```java
@Target({TYPE, METHOD})
@Retention(RUNTIME)
public @interface IniConfig {
    String kvSeparator() default "";        // "=" or ":"
    String spacedSeparator() default "";    // "true" or "false"
    String useComments() default "";        // "true" or "false"
    int rank() default 0;
}
```

### 11. `annotation/IniAnnotation.java` and `annotation/IniConfigAnnotation.java`

Utility classes for programmatic annotation creation, following the `JsonAnnotation` and `JsonConfigAnnotation` patterns.

### 12. `Ini.java` (Marshaller)

Location: `src/main/java/org/apache/juneau/marshaller/Ini.java`

Extends `CharMarshaller`, following the `Json5.java` pattern.

```java
package org.apache.juneau.marshaller;

public class Ini extends CharMarshaller {
    public static final Ini DEFAULT = new Ini();

    public static String of(Object object) throws SerializeException { ... }
    public static Object of(Object object, Object output) throws SerializeException, IOException { ... }
    public static <T> T to(Object input, Class<T> type) throws ParseException, IOException { ... }
    public static <T> T to(Object input, Type type, Type... args) throws ParseException, IOException { ... }
    public static <T> T to(String input, Class<T> type) throws ParseException { ... }
    public static <T> T to(String input, Type type, Type... args) throws ParseException { ... }

    public Ini() { this(IniSerializer.DEFAULT, IniParser.DEFAULT); }
    public Ini(IniSerializer s, IniParser p) { super(s, p); }
}
```

### 13. `package-info.java`

Package-level Javadoc explaining INI format support, mapping rules, and usage examples.

---

## Files to Modify

### 1. `BasicUniversalConfig.java`

Path: `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/config/BasicUniversalConfig.java`

Add `IniSerializer.class` to `serializers` array and `IniParser.class` to `parsers` array.

### 2. `BasicIniConfig.java` (New)

Path: `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/config/BasicIniConfig.java`

```java
@Rest(
    serializers={IniSerializer.class},
    parsers={IniParser.class},
    defaultAccept="text/ini"
)
public interface BasicIniConfig extends DefaultConfig {}
```

### 3. `RestClient.java`

Path: `juneau-rest/juneau-rest-client/src/main/java/org/apache/juneau/rest/client/RestClient.java`

Add `IniSerializer.class` and `IniParser.class` to the `universal()` method's serializers/parsers lists.

### 4. Context: `Context.java`

Path: `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/Context.java`

Add `import org.apache.juneau.ini.annotation.*;` and add `{@link IniConfig}` to the list of config annotations in the `applyAnnotations()` Javadoc (alphabetically).

---

## Detailed Serialization Algorithm

```
doSerialize(pipe, object):
    beanMap = toBeanMap(object)
    writer = getIniWriter(pipe)

    // Phase 1: Default section (simple properties)
    for each property in beanMap:
        if property is simple (string, number, boolean, enum, date/calendar/temporal, duration, null):
            writer.keyValue(propertyName, value)
        else if property is collection/array of simple values:
            writer.keyValue(propertyName, json5Encode(value))
        else if property is complex (collection of beans, map with complex values):
            writer.keyValue(propertyName, json5Encode(value))

    // Phase 2: Named sections (bean and simple-map properties)
    for each property in beanMap:
        if property is a bean:
            serializeSection(writer, propertyName, toBeanMap(value))
        else if property is a Map<String, simple>:
            serializeMapSection(writer, propertyName, value)

serializeSection(writer, sectionPath, beanMap):
    writer.blankLine()
    writer.section(sectionPath)

    // Simple properties of this nested bean
    for each property in beanMap:
        if property is simple:
            writer.keyValue(propertyName, value)
        else if property is collection/complex:
            writer.keyValue(propertyName, json5Encode(value))

    // Recurse for nested beans
    for each property in beanMap:
        if property is a bean:
            serializeSection(writer, sectionPath + "/" + propertyName, toBeanMap(value))
        else if property is a Map<String, simple>:
            serializeMapSection(writer, sectionPath + "/" + propertyName, value)
```

## Detailed Parsing Algorithm

```
doParse(pipe, targetType):
    reader = pipe.getParserReader()

    // Phase 1: Read INI into intermediate structure
    sections = parseIniContent(reader)
    // sections = {"" -> {"name": "John", "age": "42"},
    //             "address" -> {"city": "Springfield", "state": "IL"}}

    // Phase 2: Map to target type
    if targetType is bean:
        return populateBean(targetType, sections, "")
    else if targetType is Map:
        return populateMap(targetType, sections)

populateBean(beanMap, sections, sectionPath):
    defaultSection = sections.get(sectionPath)
    for each key in defaultSection:
        bpm = beanMap.getPropertyMeta(key)
        if bpm is simple type:
            beanMap.put(key, parseValue(defaultSection.get(key), bpm.classMeta))
        else:
            beanMap.put(key, parseValue(defaultSection.get(key), bpm.classMeta))
            // JSON5-encoded complex values are parsed by JsonParser

    // Check for section-based properties
    for each sectionName in sections:
        if sectionName starts with sectionPath + "/":
            childName = extract immediate child name
            bpm = beanMap.getPropertyMeta(childName)
            if bpm is bean type:
                childBean = newBeanMap(bpm.classMeta)
                populateBean(childBean, sections, sectionPath + "/" + childName)
                beanMap.put(childName, childBean.getBean())
            else if bpm is Map type:
                map = populateMap(bpm.classMeta, sections.get(sectionName))
                beanMap.put(childName, map)

parseValue(rawString, targetType):
    if rawString == "null": return null
    if rawString == "true" or "false": return Boolean
    if rawString is numeric: return Number
    if rawString starts with "'" and ends with "'": return unescapeQuotedString
    if rawString starts with "[" or "{": return jsonParser.parse(rawString, targetType)
    // ISO 8601 date/time strings converted via Iso8601Utils.parse() and BeanSession.convertToMemberType()
    if targetType.isDateOrCalendarOrTemporal(): return Iso8601Utils.parse(rawString, targetType, getTimeZone())
    if targetType.isDuration(): return Duration.parse(rawString)
    return rawString  // plain unquoted string
```

---

## Test Plan

All test files in `juneau-utest/src/test/java/org/apache/juneau/ini/`.

### 1. Core Serializer Tests: `IniSerializer_Test.java`

- **a01_simpleBean** -- Bean with string, int, boolean, enum properties serializes to flat key-value pairs
- **a02_nestedBean** -- Bean with nested bean property creates `[propName]` section
- **a03_deeplyNestedBean** -- Three levels deep creates `[level1/level2]` section path
- **a04_beanWithListOfStrings** -- List of strings serializes as JSON5 array value
- **a05_beanWithListOfNumbers** -- List of numbers serializes as JSON5 array value
- **a06_beanWithListOfBeans** -- List of beans serializes as JSON5 array of objects
- **a07_beanWithMap** -- Map<String,String> property creates a section
- **a08_beanWithComplexMap** -- Map with non-string values uses JSON5 encoding
- **a09_nullValues** -- Null properties serialize as `key = null`
- **a10_stringQuoting** -- Strings that look like numbers/booleans/null are single-quoted
- **a11_specialCharacters** -- Strings with `=`, `#`, `[`, `]`, newlines are quoted
- **a12_emptyBean** -- Bean with no properties produces empty output
- **a13_emptyStrings** -- Empty string values are quoted: `key = ''`
- **a14_enumValues** -- Enum values serialize as unquoted names
- **a15_dateValues** -- Date/temporal values serialize appropriately
- **a16_durationValues** -- Duration values serialize in ISO 8601 format (e.g. PT1H30M)
- **a17_objectSwaps** -- ObjectSwap transformations applied before INI encoding
- **a18_typeProperty** -- `_type` property emitted when `addBeanTypes` is enabled
- **a19_sectionOrdering** -- Default section appears before named sections
- **a20_multipleNestedBeans** -- Multiple bean properties each get their own section
- **a21_beanWithArrayProperty** -- Arrays handled same as collections

### 2. Core Parser Tests: `IniParser_Test.java`

- **b01_simpleBean** -- Parse flat key-value pairs into bean
- **b02_nestedBean** -- Parse `[section]` into nested bean property
- **b03_deeplyNestedBean** -- Parse `[section/subsection]` into deeply nested bean
- **b04_listOfStrings** -- Parse JSON5 array value into `List<String>`
- **b05_listOfNumbers** -- Parse JSON5 array value into `List<Integer>`
- **b06_listOfBeans** -- Parse JSON5 array of objects into `List<Bean>`
- **b07_mapProperty** -- Parse section into `Map<String,String>`
- **b08_complexMap** -- Parse JSON5-encoded map value
- **b09_nullValues** -- Parse `null` token as Java null
- **b10_quotedStrings** -- Parse single-quoted strings, including escaped quotes
- **b11_typeCoercion** -- Unquoted numbers parsed as numbers, booleans as booleans
- **b12_emptyInput** -- Empty input returns null or empty bean
- **b13_commentsIgnored** -- Lines starting with `#` are skipped
- **b14_blankLinesIgnored** -- Blank lines between sections are skipped
- **b15_enumValues** -- Unquoted enum names parsed to enum instances
- **b16_objectSwaps** -- ObjectSwap reverse transformations applied during parsing
- **b17_unknownProperties** -- Unknown keys are ignored (when configured) or throw
- **b18_colonSeparator** -- Support `:` as key-value separator
- **b19_noSpaceSeparator** -- Support `key=value` without spaces
- **b20_trimValues** -- Leading/trailing whitespace in values is trimmed

### 3. Round-Trip Tests: `IniRoundTrip_Test.java`

These are the most critical tests proving lossless fidelity.

- **c01_simpleBeanRoundTrip** -- Serialize + parse a simple bean, verify equality
- **c02_nestedBeanRoundTrip** -- Serialize + parse a bean with nested beans
- **c03_deepNestingRoundTrip** -- 3+ levels of nesting round-trip correctly
- **c04_allPrimitiveTypes** -- All Java primitive types and wrappers round-trip
- **c05_stringEdgeCases** -- Strings containing special chars, empty strings, strings that look like other types
- **c06_collectionsRoundTrip** -- Lists of strings, numbers, and beans round-trip
- **c07_mapsRoundTrip** -- Maps with string keys and various value types round-trip
- **c08_nullPropertiesRoundTrip** -- Null values round-trip correctly
- **c09_enumRoundTrip** -- Enum values round-trip correctly
- **c10_complexBeanRoundTrip** -- Bean with mix of simple, nested, collection, map properties
- **c11_objectSwapRoundTrip** -- Values with registered swaps round-trip correctly
- **c12_emptyCollectionsRoundTrip** -- Empty lists and maps round-trip correctly
- **c13_beanWithArrays** -- Array properties (int[], String[], Bean[]) round-trip
- **c14_nestedMaps** -- Map<String, Map<String, Object>> round-trip
- **c15_unicodeRoundTrip** -- Unicode characters in keys and values round-trip

### 4. Marshaller Tests: `org/apache/juneau/marshaller/Ini_Test.java`

- **d01_of** -- `Ini.of()` serializes to string
- **d02_to** -- `Ini.to()` parses from string
- **d03_roundTrip** -- `Ini.of()` + `Ini.to()` round-trip
- **d04_defaultInstance** -- `Ini.DEFAULT.write()` and `Ini.DEFAULT.read()`

### 5. Annotation Tests: `IniAnnotation_Test.java`

- **e01_customSection** -- `@Ini(section="custom")` overrides section name
- **e02_comment** -- `@Ini(comment="...")` emits comment before property
- **e03_annotationEquivalency** -- Annotation equivalency checks

### 6. Config Annotation Tests: `IniConfigAnnotation_Test.java`

- **e04_kvSeparator** -- `@IniConfig(kvSeparator=":")` changes separator
- **e05_spacedSeparator** -- `@IniConfig(spacedSeparator="false")` removes spacing
- **e06_useComments** -- `@IniConfig(useComments="true")` enables comments

### 7. Edge Case Tests: `IniEdgeCases_Test.java`

- **f01_keysWithSpecialChars** -- Bean property names with underscores, camelCase
- **f02_veryLongValues** -- Values exceeding typical line length
- **f03_unicodeKeys** -- Property names with Unicode characters
- **f04_emptySection** -- Section with no keys
- **f05_sectionNameConflict** -- Property name conflicts with section name (property takes precedence, section uses qualified path)
- **f06_mapWithNonStringKeys** -- Maps with Integer/Enum keys (JSON5-encoded)
- **f07_cyclicReferences** -- Beans with circular references (handled by recursion detection)
- **f08_beanPropertyOrdering** -- Property order preserved (uses bean property order)
- **f09_readOnly_writeOnlyProperties** -- Properties with only getters or only setters
- **f10_optionalProperties** -- `Optional<T>` bean properties
- **f11_windowsLineEndings** -- Input with `\r\n` line endings parsed correctly
- **f12_trailingWhitespace** -- Values with trailing spaces are trimmed
- **f13_inlineComments** -- Values followed by `# comment` (comment stripped)

### 8. Media Type Tests: `IniMediaType_Test.java`

- **g01_producesCorrectMediaType** -- Serializer produces `text/ini`
- **g02_acceptsMediaTypes** -- Serializer accepts `text/ini`, `text/x-ini`
- **g03_consumesMediaTypes** -- Parser consumes `text/ini`, `text/x-ini`

---

### 9. Cross-Format Round-Trip Integration: `org/apache/juneau/a/rttests/RoundTripDateTime_Test.java`

Add a `RoundTrip_Tester` entry for this format to the `TESTERS` array in `RoundTripDateTime_Test.java`. This test verifies date/time and Duration round-trip across all serializer/parser combinations. It covers beans with `Instant`, `ZonedDateTime`, `LocalDate`, `LocalDateTime`, `LocalTime`, `OffsetDateTime`, `OffsetTime`, `Year`, `YearMonth`, `Calendar`, `Date`, `Duration`, and `XMLGregorianCalendar` fields.

## Documentation

### 1. Javadoc

All three primary API classes get comprehensive Javadoc following the patterns established by `JsonSerializer`, `JsonParser`, and `Json5` (marshaller). The standard cross-format example bean used throughout is "Alice" with a nested `address` object and a `tags` array (the same example used by JSON, JSON5, and XML).

---

#### `IniSerializer.java` — class-level Javadoc template

Follows the `JsonSerializer` pattern. Includes: Media types topic, Description topic with type-mapping list and limitations, usage example, figure output in `bini` blocks, Notes, See Also.

```java
/**
 * Serializes POJO models to INI format.
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Accept</c> types:  <bc>text/ini, text/x-ini</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>text/ini</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * Converts Java POJOs to INI format (section/key-value structure).
 * The conversion is as follows:
 * <ul class='spaced-list'>
 *   <li>Top-level beans and {@link java.util.Map Maps} with String keys are converted to INI key-value pairs,
 *       with nested beans and simple maps becoming named sections (<c>[sectionName]</c>).
 *   <li>{@link String Strings} are converted to unquoted values, or single-quoted when the value contains
 *       special characters or is ambiguous (e.g. looks like a number or <jk>null</jk>).
 *   <li>{@link Number Numbers} are converted to unquoted numeric values.
 *   <li>{@link Boolean Booleans} are converted to unquoted <c>true</c>/<c>false</c>.
 *   <li><jk>null</jk> values are converted to the unquoted token <c>null</c>.
 *   <li>{@link java.util.Date}, {@link java.util.Calendar}, and <c>java.time.*</c> temporal types are
 *       converted to ISO 8601 strings.
 *   <li>{@link java.time.Duration} is converted to ISO 8601 duration strings (e.g. <c>PT1H30M</c>).
 *   <li>{@link java.net.URI} and {@link java.net.URL} are converted to URI strings.
 *   <li>Enum values are converted to their unquoted name.
 *   <li>Collections and arrays are JSON5-encoded as inline values (e.g. <c>tags = ['a','b','c']</c>).
 *   <li>Collections of beans are JSON5-encoded as inline values (e.g. <c>items = [{name:'Alice'}]</c>).
 *   <li>Maps with non-String keys or complex values are JSON5-encoded as inline values.
 * </ul>
 *
 * <p>
 * <b>Limitations:</b>
 * <ul class='spaced-list'>
 *   <li>Top-level Collections, arrays, and scalar values are not supported. The top-level object must
 *       be a bean or <c>Map&lt;String,?&gt;</c>. Attempting to serialize a top-level collection or
 *       scalar will throw a {@link org.apache.juneau.serializer.SerializeException}.
 *   <li>{@link java.io.Reader} and {@link java.io.InputStream} passthrough is not supported.
 * </ul>
 *
 * <p>
 * Values that cannot be represented natively in INI (collections, complex maps) are encoded as
 * JSON5 inline strings and remain fully round-trippable — the parser recognizes <c>[...]</c> and
 * <c>{...}</c> values and delegates them to the JSON parser automatically.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 *   <jc>// Use the default serializer to serialize a POJO</jc>
 *   String <jv>ini</jv> = IniSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>someObject</jv>);
 *
 *   <jc>// Create a custom serializer</jc>
 *   IniSerializer <jv>serializer</jv> = IniSerializer.<jsm>create</jsm>().useComments().build();
 *
 *   <jc>// Serialize a POJO to INI</jc>
 *   String <jv>ini</jv> = <jv>serializer</jv>.serialize(<jv>someObject</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (Map of name/age):</h5>
 * <p class='bini'>
 *   <ck>name</ck> = <cv>Alice</cv>
 *   <ck>age</ck> = <cv>30</cv>
 * </p>
 *
 * <h5 class='figure'>Complex (nested object + array):</h5>
 * <p class='bini'>
 *   <ck>name</ck> = <cv>Alice</cv>
 *   <ck>age</ck> = <cv>30</cv>
 *   <ck>tags</ck> = <cv>['a','b','c']</cv>
 *
 *   <cs>[address]</cs>
 *   <ck>street</ck> = <cv>123 Main St</cv>
 *   <ck>city</ck> = <cv>Boston</cv>
 *   <ck>state</ck> = <cv>MA</cv>
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 *   <li class='note'>This class is thread safe and reusable.
 *   <li class='note'>Complex values (collections of beans, maps with complex values) are embedded
 *       as JSON5 inline strings and are fully round-trippable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 *   <li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/IniBasics">INI Basics</a>
 * </ul>
 */
```

INI CSS tags used in the `bini` blocks (from `juneau-code.css`):
- `<cs>` — config section header (e.g. `[address]`)
- `<ck>` — config key
- `<cv>` — config value
- `<cc>` — config comment (`# ...`)

---

#### `IniParser.java` — class-level Javadoc template

Follows the `JsonParser` pattern. Includes: Media types topic, Description topic with parsing rules per token type, limitations, figure input in `bini` block, Notes, See Also.

```java
/**
 * Parses INI-formatted text into POJO models.
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Content-Type</c> types:  <bc>text/ini, text/x-ini</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * Parses INI files (section/key-value format) into Java beans and maps.
 * Value tokens are interpreted as follows:
 * <ul class='spaced-list'>
 *   <li>Unquoted <c>null</c> is converted to a Java <jk>null</jk>.
 *   <li>Unquoted <c>true</c>/<c>false</c> are converted to {@link Boolean Booleans}.
 *   <li>Unquoted numeric tokens are converted to {@link Number Numbers}
 *       ({@link Integer}, {@link Long}, {@link Float}, or {@link Double} as appropriate).
 *   <li>Single-quoted strings (<c>'...'</c>) are converted to {@link String Strings},
 *       with doubled single-quotes (<c>''</c>) unescaped to a single quote.
 *   <li>Values starting with <c>[</c> or <c>{</c> are delegated to the JSON parser and
 *       converted to collections, arrays, or beans as appropriate for the target type.
 *   <li>ISO 8601 strings are converted to {@link java.util.Date}, {@link java.util.Calendar},
 *       or <c>java.time.*</c> types when the target property type requires it.
 *   <li>ISO 8601 duration strings are converted to {@link java.time.Duration}.
 *   <li>All other unquoted tokens are converted to {@link String Strings}.
 * </ul>
 *
 * <p>
 * Sections (e.g. <c>[address]</c>) are mapped to nested bean properties or
 * {@link java.util.Map Map} properties with matching names. Dotted/slash-separated section
 * names (e.g. <c>[employment/company]</c>) are mapped to deeply-nested bean properties.
 *
 * <p>
 * <b>Limitations:</b>
 * <ul class='spaced-list'>
 *   <li>Parsing into top-level Collections, arrays, or scalar types is not supported.
 *       The target type must be a bean class or <c>Map&lt;String,?&gt;</c>.
 * </ul>
 *
 * <h5 class='figure'>Example input (Map of name/age):</h5>
 * <p class='bini'>
 *   <ck>name</ck> = <cv>Alice</cv>
 *   <ck>age</ck> = <cv>30</cv>
 * </p>
 *
 * <h5 class='figure'>Complex (nested object + array):</h5>
 * <p class='bini'>
 *   <ck>name</ck> = <cv>Alice</cv>
 *   <ck>age</ck> = <cv>30</cv>
 *   <ck>tags</ck> = <cv>['a','b','c']</cv>
 *
 *   <cs>[address]</cs>
 *   <ck>street</ck> = <cv>123 Main St</cv>
 *   <ck>city</ck> = <cv>Boston</cv>
 *   <ck>state</ck> = <cv>MA</cv>
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 *   <li class='note'>This class is thread safe and reusable.
 *   <li class='note'>Values starting with <c>[</c> or <c>{</c> are automatically delegated
 *       to the JSON parser, enabling full round-trip fidelity for collections and complex maps.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 *   <li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/IniBasics">INI Basics</a>
 * </ul>
 */
```

---

#### `Ini.java` (marshaller) — class-level Javadoc template

Follows the short `Json5.java` / `Xml.java` pattern. Includes: one-sentence summary, two `bjava` blocks (instance and `DEFAULT`), `bini` figure blocks for simple and complex output, brief limitation note, See Also.

```java
/**
 * A pairing of a {@link IniSerializer} and {@link IniParser} into a single class with convenience read/write methods.
 *
 * <p>
 *   The general idea is to combine a single serializer and parser inside a simplified API for reading and writing POJOs.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 *   <jc>// Using instance.</jc>
 *   Ini <jv>ini</jv> = <jk>new</jk> Ini();
 *   MyPojo <jv>myPojo</jv> = <jv>ini</jv>.read(<jv>string</jv>, MyPojo.<jk>class</jk>);
 *   String <jv>string</jv> = <jv>ini</jv>.write(<jv>myPojo</jv>);
 * </p>
 * <p class='bjava'>
 *   <jc>// Using DEFAULT instance.</jc>
 *   MyPojo <jv>myPojo</jv> = Ini.<jsf>DEFAULT</jsf>.read(<jv>string</jv>, MyPojo.<jk>class</jk>);
 *   String <jv>string</jv> = Ini.<jsf>DEFAULT</jsf>.write(<jv>myPojo</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (Map of name/age):</h5>
 * <p class='bini'>
 *   <ck>name</ck> = <cv>Alice</cv>
 *   <ck>age</ck> = <cv>30</cv>
 * </p>
 *
 * <h5 class='figure'>Complex (nested object + array):</h5>
 * <p class='bini'>
 *   <ck>name</ck> = <cv>Alice</cv>
 *   <ck>age</ck> = <cv>30</cv>
 *   <ck>tags</ck> = <cv>['a','b','c']</cv>
 *
 *   <cs>[address]</cs>
 *   <ck>street</ck> = <cv>123 Main St</cv>
 *   <ck>city</ck> = <cv>Boston</cv>
 *   <ck>state</ck> = <cv>MA</cv>
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 *   <li class='note'>The top-level object must be a bean or <c>Map&lt;String,?&gt;</c>.
 *       Top-level collections, arrays, and scalars are not supported by INI format.
 *   <li class='note'>Collections and complex map values are embedded as JSON5 inline strings
 *       and are fully round-trippable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 *   <li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshallers">Marshallers</a>
 * </ul>
 */
```

---

### 2. Package Javadoc (`package-info.java`)

Comprehensive package documentation including:
- What INI format is
- How beans map to sections and key-value pairs
- Complete mapping rules table (native vs JSON5 fallback vs unsupported)
- Round-trip examples
- Use cases (legacy system integration, configuration files, human-readable data exchange)
- Comparison with Juneau's Config module (this is a general-purpose serializer; Config is for application configuration with live reloading, listeners, etc.)

### 3. Update Release Notes
/Users/james.bognar/git/apache/juneau/docs/pages/release-notes

### 4. Update Documentation

Find how existing languages are documented in the following location and update the documentation to match
the same level of detail.
/Users/james.bognar/git/apache/juneau/docs/pages/topics

---

## Implementation Order

1. **`IniWriter.java`** -- The INI-specific writer (needed by serializer)
2. **`IniSerializer.java`** and **`IniSerializerSession.java`** -- Core serialization
3. **`IniParser.java`** and **`IniParserSession.java`** -- Core parsing
4. **`IniMetaProvider.java`**, **`IniClassMeta.java`**, **`IniBeanPropertyMeta.java`** -- Metadata infrastructure
5. **`annotation/Ini.java`**, **`annotation/IniConfig.java`** -- Annotations
6. **`annotation/IniAnnotation.java`**, **`annotation/IniConfigAnnotation.java`** -- Annotation utilities
7. **`Ini.java`** (marshaller) -- Convenience API
8. **`package-info.java`** -- Package documentation
9. **Serializer tests** (`IniSerializer_Test.java`) -- 21 test cases
10. **Parser tests** (`IniParser_Test.java`) -- 20 test cases
11. **Round-trip tests** (`IniRoundTrip_Test.java`) -- 15 test cases (highest priority for proving lossless fidelity)
12. **Marshaller tests** (`Ini_Test.java`)
13. **Annotation tests** (`IniAnnotation_Test.java`, `IniConfigAnnotation_Test.java`)
14. **Edge case tests** (`IniEdgeCases_Test.java`) -- 13 test cases
15. **Media type tests** (`IniMediaType_Test.java`)
16. **REST integration** (`BasicUniversalConfig`, `BasicIniConfig`, `RestClient`)
17. **Context registration** (`IniConfig`, `IniConfigAnnotation`, `Context.java`)
18. **Final documentation review**

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

The `toListFromStreamable()` method from `SerializerSession` handles the conversion. The resulting list is then encoded as a JSON5 value, consistent with how other collections are handled in INI format.

---

## Key Design Decisions

### 1. Standalone serializer, not extending JsonSerializer

INI's two-level section/key structure is fundamentally different from JSON's recursive tree. Extending `WriterSerializer` directly provides a clean implementation. JSON5 is used internally as a utility for encoding complex values, not as a base class.

### 2. JSON5 for complex values

When a value can't be naturally represented in INI (arrays, collections of objects, nested maps), it's encoded as a JSON5 string. This is the same proven strategy Juneau's `Config` module uses. It provides:
- Lossless type preservation
- Compact representation
- Reuse of existing, well-tested serializer/parser code
- Human readability (JSON5 uses single quotes and unquoted attribute names)

### 3. `/` as section path separator

Consistent with Juneau's `Config` module which uses `section/key` notation. This avoids conflicts with `.` which appears in many property names (e.g., Java package names, DNS names).

### 4. Single-quote string escaping

Follows JSON5 convention for quoted strings. Single quotes within values are doubled (`''`), which is a common INI convention and easily reversible.

### 5. Two-pass serialization

Writing simple properties first (default section), then named sections, produces clean INI output that humans expect: top-level values at the top, sections below.

### 6. Type inference during parsing

The parser uses the target bean's `ClassMeta` to determine the expected type for each property, avoiding ambiguity. The raw string `42` is parsed as `int` if the bean property is `int`, or as `String` if the property is `String`. JSON5-encoded values (starting with `[` or `{`) are delegated to `JsonParser` which handles all type resolution.

### 7. Date/time and Duration as built-in first-class types

Date, Calendar, `java.time` Temporal subtypes (Instant, ZonedDateTime, LocalDate, etc.), and `java.time.Duration` are built-in first-class types. Serialization uses `Iso8601Utils.format()` for date/time types and `toString()` (ISO 8601 duration format) for Duration. Parsing uses `Iso8601Utils.parse()` and `Duration.parse()`. These checks occur before `isBean()` in the serializer/parser dispatch (see builtin-datetime-iso8601.md for full details).
