# Multi-Line String (Text Block) Opportunities

This document lists all locations in the Apache Juneau codebase where Java text blocks (multi-line strings) could be used to replace concatenated strings. Text blocks were introduced in Java 15 and provide a cleaner way to write multi-line strings.

## High Priority Candidates

These locations have extensive string concatenation that would significantly benefit from text blocks:

### 1. Queryable.java - SWAGGER_PARAMS
**Location:** `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/converter/Queryable.java:85-94`
```java
public static final String SWAGGER_PARAMS = "" + "{" + "in:'query'," + "name:'s'," + "description:'" + "Search.\n" + "Key/value pairs representing column names and search tokens.\n"
    + "\\'*\\' and \\'?\\' can be used as meta-characters in string fields.\n" + "\\'>\\', \\'>=\\', \\'<\\', and \\'<=\\' can be used as limits on numeric and date fields.\n"
    + "Date fields can be matched with partial dates (e.g. \\'2018\\' to match any date in the year 2018)." + "'," + "type:'array'," + "collectionFormat:'csv',"
    // ... continues for 10 lines
```
**Benefit:** Extremely long concatenated string spanning 10 lines. Text block would be much more readable.
**Lines:** Approximately 10 lines of concatenation

### 2. MenuItemWidget.java - HTML Generation
**Location:** `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/widget/MenuItemWidget.java:136-184`
```java
sb.append("\n\t\tfunction onPreShow" + id + "() {");
// ...
+ "\n\t<a onclick='"+onclick+"'>"+getLabel(req, res)+"</a>"
+ "\n<div class='popup-content'>"
// ...
+ "\n\t</div>"
+ "\n</div>"
```
**Benefit:** HTML structure with newlines - perfect candidate for text block
**Lines:** Multiple concatenations with \n escape sequences

### 3. Introspectable.java - SWAGGER_PARAMS
**Location:** `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/converter/Introspectable.java`
**Benefit:** Similar to Queryable.java, contains Swagger parameter definitions with extensive concatenation
**Lines:** Multiple lines of string concatenation

### 4. Content_Test.java - Test Data
**Location:** `juneau-utest/src/test/java/org/apache/juneau/rest/annotation/Content_Test.java`
**Benefit:** Test strings with concatenation - approximately 60 occurrences
**Lines:** Throughout the file

### 5. UrlEncodingSerializer_Test.java - Test Data  
**Location:** `juneau-utest/src/test/java/org/apache/juneau/urlencoding/UrlEncodingSerializer_Test.java`
**Benefit:** Test strings with concatenation - approximately 160 occurrences
**Lines:** Throughout the file

### 6. UrlEncodingParser_Test.java - Test Data
**Location:** `juneau-utest/src/test/java/org/apache/juneau/urlencoding/UrlEncodingParser_Test.java`
**Benefit:** Test strings with concatenation - approximately 80 occurrences
**Lines:** Throughout the file

## Medium Priority Candidates

These locations have string concatenation that could benefit from text blocks:

### 7. Rest_Debug_Test.java
**Location:** `juneau-utest/src/test/java/org/apache/juneau/rest/annotation/Rest_Debug_Test.java`
**Lines:** Approximately 8 occurrences

### 8. DirectoryResource.java
**Location:** `juneau-microservice/juneau-microservice-core/src/main/java/org/apache/juneau/microservice/resources/DirectoryResource.java`
**Lines:** Approximately 4 occurrences

### 9. TestUtils.java
**Location:** `juneau-utest/src/test/java/org/apache/juneau/TestUtils.java`
**Lines:** Approximately 4 occurrences

### 10. Query_Test.java
**Location:** `juneau-utest/src/test/java/org/apache/juneau/rest/annotation/Query_Test.java`
**Lines:** Approximately 4 occurrences

### 11. ReflectionMap.java
**Location:** `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/utils/ReflectionMap.java`
**Lines:** Approximately 3 occurrences

### 12. ManifestFile.java
**Location:** `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/utils/ManifestFile.java`
**Lines:** Approximately 2 occurrences

### 13. UrlPathMatcher.java
**Location:** `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/util/UrlPathMatcher.java`
**Lines:** Approximately 2 occurrences

### 14. RestUtils.java
**Location:** `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/util/RestUtils.java`
**Lines:** Approximately 2 occurrences

## Additional Candidates in Test Files

### BCT Test Files
These test files contain multiple string concatenations for test data:

- **Swapper_Test.java** - `juneau-utest/src/test/java/org/apache/juneau/junit/bct/Swapper_Test.java` (3 occurrences)
- **Stringifier_Test.java** - `juneau-utest/src/test/java/org/apache/juneau/junit/bct/Stringifier_Test.java` (5 occurrences)
- **BctAssertions_Test.java** - `juneau-utest/src/test/java/org/apache/juneau/junit/bct/BctAssertions_Test.java`
- **BeanConverter_Test.java** - `juneau-utest/src/test/java/org/apache/juneau/junit/bct/BeanConverter_Test.java`
- **Listifier_Test.java** - `juneau-utest/src/test/java/org/apache/juneau/junit/bct/Listifier_Test.java`

### Assertion Test Files
- **AnyAssertion_Test.java** - `juneau-utest/src/test/java/org/apache/juneau/assertions/AnyAssertion_Test.java`
- **BeanAssertion_Test.java** - `juneau-utest/src/test/java/org/apache/juneau/assertions/BeanAssertion_Test.java`
- **BeanListAssertion_Test.java** - `juneau-utest/src/test/java/org/apache/juneau/assertions/BeanListAssertion_Test.java`
- **ArrayAssertion_Test.java** - `juneau-utest/src/test/java/org/apache/juneau/assertions/ArrayAssertion_Test.java`
- **ObjectAssertion_Test.java** - `juneau-utest/src/test/java/org/apache/juneau/assertions/ObjectAssertion_Test.java`

### Other Test Files
- **BeanStore_Test.java** - `juneau-utest/src/test/java/org/apache/juneau/cp/BeanStore_Test.java` (8 occurrences)
- **HasQuery_Test.java** - `juneau-utest/src/test/java/org/apache/juneau/rest/annotation/HasQuery_Test.java` (2 occurrences)
- **HasFormData_Test.java** - `juneau-utest/src/test/java/org/apache/juneau/rest/annotation/HasFormData_Test.java`
- **FormData_Test.java** - `juneau-utest/src/test/java/org/apache/juneau/rest/annotation/FormData_Test.java` (2 occurrences)
- **BeanMap_Test.java** - `juneau-utest/src/test/java/org/apache/juneau/BeanMap_Test.java` (2 occurrences)

## Core Library Candidates

### Serialization/Parsing
- **SerializerSession.java** - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/serializer/SerializerSession.java`
- **MediaType.java** - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/MediaType.java`
- **Namespace.java** - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/xml/Namespace.java`

### Utility Classes
- **VarResolverSession.java** - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/svl/VarResolverSession.java`
- **DefaultingVar.java** - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/svl/DefaultingVar.java`
- **VersionRange.java** - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/internal/VersionRange.java`
- **LocalDir.java** - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/cp/LocalDir.java`
- **Messages.java** - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/cp/Messages.java`
- **ObjectRest.java** - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/objecttools/ObjectRest.java`

### Swaps and Templates
- **StackTraceElementSwap.java** - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/StackTraceElementSwap.java`
- **BasicHtmlDocTemplate.java** - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/html/BasicHtmlDocTemplate.java`

### REST Components
- **RestRequest.java** - `juneau-rest/juneau-rest-client/src/main/java/org/apache/juneau/rest/client/RestRequest.java`
- **MockServletRequest.java** - `juneau-rest/juneau-rest-mock/src/main/java/org/apache/juneau/rest/mock/MockServletRequest.java`

### Microservice Components
- **HelpCommand.java** - `juneau-microservice/juneau-microservice-core/src/main/java/org/apache/juneau/microservice/console/HelpCommand.java`
- **LogEntryFormatter.java** - `juneau-microservice/juneau-microservice-core/src/main/java/org/apache/juneau/microservice/resources/LogEntryFormatter.java`

### Bean Modules
- **Items.java** - `juneau-bean/juneau-bean-openapi-v3/src/main/java/org/apache/juneau/bean/openapi3/Items.java`

### Examples
- **XmlConfigurationExample.java** - `juneau-examples/juneau-examples-core/src/main/java/org/apache/juneau/examples/core/xml/XmlConfigurationExample.java`

## Summary

- **Total files identified:** 70+ files
- **High priority (10+ lines of concatenation):** 6 files
- **Medium priority (2-10 lines):** 8 files  
- **Test files:** 50+ files
- **Core library files:** 20+ files

## Benefits of Text Blocks

- **Improved readability:** Multi-line strings are easier to read without concatenation
- **Less escaping:** No need to escape quotes in most cases
- **Better formatting:** Preserves indentation and structure
- **Fewer errors:** Less chance of missing concatenation operators or quotes
- **Cleaner code:** Eliminates visual noise from + operators and \n escapes

## Example Conversion

**Before:**
```java
public static final String SWAGGER_PARAMS = "" + "{" + "in:'query'," + "name:'s'," + "description:'" 
    + "Search.\n" + "Key/value pairs representing column names and search tokens.\n"
    + "\\'*\\' and \\'?\\' can be used as meta-characters in string fields.\n";
```

**After:**
```java
public static final String SWAGGER_PARAMS = """
    {
      in:'query',
      name:'s',
      description:'Search.
    Key/value pairs representing column names and search tokens.
    \\'*\\' and \\'?\\' can be used as meta-characters in string fields.
    """;
```

## Notes

- Java text blocks require Java 15 or later
- Text blocks preserve indentation relative to the closing delimiter
- Automatic whitespace management makes formatting easier
- Quotes do not need to be escaped unless they are triple quotes
- Consider enabling text blocks incrementally, starting with high-priority candidates

