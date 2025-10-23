# TODO: Systematic `var` Keyword Migration Plan

This document outlines a systematic plan to find and convert appropriate variable declarations to use the `var` keyword throughout the entire Juneau codebase.

## Overview

The `var` keyword (introduced in Java 10) allows for local variable type inference, making code more concise while maintaining type safety. This plan covers all modules and packages in the Juneau project.

## Migration Guidelines

### When to Use `var`:
- Local variables with explicit initializers
- Variables where the type is obvious from the context
- Loop variables in enhanced for-loops
- Variables in try-with-resources statements
- Variables where the type is verbose but obvious

### When NOT to Use `var`:
- Fields (instance/static variables)
- Method parameters
- Method return types
- Variables without initializers
- Variables where type clarity is important for readability
- Variables where the inferred type might be unexpected

## Module-by-Module Analysis Plan

### 1. juneau-core/juneau-common
**Priority: High** - Core utilities, likely many simple variable declarations

#### Packages to Review:
- `org.apache.juneau.common.utils.*`
  - `StringUtils.java` - Many utility methods with local variables
  - `Utils.java` - Core utility methods
  - `IOUtils.java` - I/O utility methods
  - `ThrowableUtils.java` - Exception utility methods
  - `DateUtils.java` - Date utility methods
  - `ClassUtils.java` - Class utility methods
  - `ReflectionUtils.java` - Reflection utility methods

**Expected Opportunities:**
- Simple variable declarations in utility methods
- Loop variables in iteration methods
- Temporary variables in complex calculations
- Variables in try-with-resources blocks

### 2. juneau-core/juneau-assertions
**Priority: High** - Test utilities with many local variables

#### Packages to Review:
- `org.apache.juneau.assertions.*`
  - All assertion classes (47 Java files)
  - Test utility classes

**Expected Opportunities:**
- Variables in assertion methods
- Loop variables in collection assertions
- Temporary variables in complex assertion logic

### 3. juneau-core/juneau-bct - ✓ COMPLETED
**Priority: Medium** - Bean-Centric Testing framework

#### Files Converted:
- **`BasicBeanConverter.java`** - 9 conversions (c, pn, selfValue, o, o2, c, stringifier, swapper, l, s)
- **`BctAssertions.java`** - 18 conversions (converter, tokens, errors, actualList, i, i2, e, a, o, actualStrings, missingSubstrings, list, x, v, m, size)
- **`Stringifiers.java`** - 8 conversions (sb, element, v, o2, buff, nRead, b, bytes, buf, i)
- **`NestedTokenizer.java`** - 14 conversions (length, pos, state, currentValue, nestedDepth, nestedStart, tokens, lastWasComma, justCompletedNested, c, value, nestedContent, token, finalValue)
- **`Utils.java`** - 4 conversions (l, i, sb, c)
- **`PropertyExtractors.java`** - 7 conversions (l, index, m, f, c, n, m, c2)

**Total Conversions: 60**

**Summary:**
All main files in the BCT framework have been converted to use `var` where appropriate. Conversions focused on local variables in method bodies, loop variables, and temporary variables used for processing.

### 4. juneau-core/juneau-config
**Priority: Medium** - Configuration management

#### Packages to Review:
- `org.apache.juneau.config.*`
  - `Config.java`
  - `ConfigBuilder.java`
  - `ConfigMap.java`
  - `ConfigStore.java`
  - Internal configuration classes

**Expected Opportunities:**
- Variables in configuration parsing
- Variables in builder methods
- Variables in map operations

### 5. juneau-core/juneau-marshall
**Priority: High** - Largest module with most complex code

#### Packages to Review:

##### 5.1 Core Marshall Classes
- `org.apache.juneau.*`
  - `BeanContext.java`
  - `BeanMeta.java`
  - `BeanPropertyMeta.java`
  - `ClassMeta.java`
  - `BeanMap.java`
  - `BeanSession.java`

##### 5.2 Serialization/Deserialization
- `org.apache.juneau.serializer.*`
  - All serializer classes (16 files)
- `org.apache.juneau.parser.*`
  - All parser classes (19 files)
- `org.apache.juneau.marshaller.*`
  - All marshaller classes (14 files)

##### 5.3 Format-Specific Packages
- `org.apache.juneau.json.*` (18 files)
- `org.apache.juneau.xml.*` (26 files)
- `org.apache.juneau.html.*` (37 files)
- `org.apache.juneau.csv.*` (14 files)
- `org.apache.juneau.uon.*` (17 files)
- `org.apache.juneau.urlencoding.*` (13 files)
- `org.apache.juneau.plaintext.*` (13 files)
- `org.apache.juneau.msgpack.*` (16 files)

##### 5.4 Support Classes
- `org.apache.juneau.annotation.*` (40+ files)
- `org.apache.juneau.internal.*` (44 files)
- `org.apache.juneau.utils.*` (22 files)
- `org.apache.juneau.collections.*` (5 files)
- `org.apache.juneau.reflect.*` (12 files)
- `org.apache.juneau.cp.*` (14 files)
- `org.apache.juneau.swap.*` (15 files)
- `org.apache.juneau.swaps.*` (21 files)
- `org.apache.juneau.svl.*` (29 files)
- `org.apache.juneau.httppart.*` (27 files)
- `org.apache.juneau.http.*` (34 files)
- `org.apache.juneau.encoders.*` (7 files)
- `org.apache.juneau.objecttools.*` (21 files)
- `org.apache.juneau.jsonschema.*` (13 files)
- `org.apache.juneau.oapi.*` (13 files)
- `org.apache.juneau.soap.*` (11 files)

**Expected Opportunities:**
- Variables in serialization/deserialization methods
- Variables in parser state machines
- Variables in builder patterns
- Variables in reflection operations
- Variables in annotation processing
- Variables in format-specific logic

### 6. juneau-core/juneau-marshall-rdf
**Priority: Low** - RDF-specific functionality

#### Packages to Review:
- `org.apache.juneau.rdf.*`
  - RDF-specific marshalling classes (33 files)

### 7. juneau-bean (All Submodules)
**Priority: Medium** - Bean definition and processing

#### Submodules:
- `juneau-bean-atom` (17 Java files)
- **`juneau-bean-common` (3 Java files) - Completed ✓ (Already converted)**
  - `LinkString.java` - Already uses `var` (line 152)
  - `ResultSetList.java` - Already uses `var` extensively (lines 52-76, 101, 105)
  - `package-info.java` - Package documentation (no code)
  - **Total: 0 new conversions (already using var)**
- **`juneau-bean-html5` (118 Java files) - Completed ✓ (No opportunities)**
  - Module consists primarily of HTML5 bean POJOs with fluent setters
  - `HtmlElement.java` - Already uses `var` (line 1047)
  - `HtmlElementContainer.java` - Already uses `var` (lines 86, 145-149)
  - `HtmlBuilder.java` - Static factory methods with no local variables
  - `HtmlBeanDictionary.java` - Simple constructor calling super
  - `HtmlText.java` - Simple POJO with no local variables
  - All other files are simple bean classes with fluent setters and no local variable declarations
  - **Total: 0 new conversions (minimal logic, already using var where applicable)**
- `juneau-bean-jsonschema` (9 Java files)
- `juneau-bean-openapi-v3` (33 Java files)
- `juneau-bean-swagger-v2` (20 Java files)

**Expected Opportunities:**
- Variables in bean definition processing
- Variables in HTML5 bean generation
- Variables in OpenAPI/Swagger generation
- Variables in JSON Schema generation

### 8. juneau-rest (All Submodules)
**Priority: High** - REST framework with complex logic

#### Submodules:

##### 8.1 juneau-rest-common
- **`org.apache.juneau.rest.*` (184 Java files) - In Progress**
  - REST common functionality
  - **Completed Files:**
    - `PartList.java` - 8 conversions (PartList x, CharArrayBuffer sb, NameValuePair x, boolean isResolving, Supplier<?> value2)
    - `HeaderList.java` - 8 conversions (HeaderList x, CharArrayBuffer sb, Header x, boolean isResolving, Supplier<?> value2)
    - `HttpParts.java` - 3 conversions (ClassInfo ci, ConstructorInfo cc)
  - **Total so far: 19 var conversions in 3 files**
  - **Remaining:** ~180 files to review including metadata, bean, entity, response, and resource classes

##### 8.2 juneau-rest-client
- `org.apache.juneau.rest.client.*` (28 Java files)
- REST client functionality

##### 8.3 juneau-rest-server - ✓ COMPLETED
- **`org.apache.juneau.rest.*` (233 Java files) - Completed**
  - REST server functionality
  - **Core REST Classes:**
    - `RestContext.java` - 22 conversions (rc, resource, mi, ci, r, rc, r, bs, rci, vrs, work, map, creator, vr, cfv, cf, creator, rci, initMap, creator, creator, bs, opContexts, childMatch)
    - `RestOpContext.java` - 4 conversions (c, rbm, pm, pm)
  - **Request/Response Classes:**
    - `RestRequest.java` - 10 conversions (country, i, cp, best, h, swagger, x, i, j, pv, cp, sp, tz, uri, sb)
    - `RestResponse.java` - 2 conversions (context, h, charset)
  - **Session Classes:**
    - `RestSession.java` - 0 conversions
    - `RestOpSession.java` - 0 conversions
  - **Operation Classes:**
    - `RestOperations.java` - 0 conversions
    - `RestOpInvoker.java` - 1 conversion (args)
    - `RestChild.java` - 0 conversions
    - `RestChildren.java` - 1 conversion (pi)
  - **Package Directories:**
    - `arg/` - 0 conversions (33 files)
    - `httppart/` - 0 conversions (18 files)
    - `servlet/` - 0 conversions (9 files)
    - `processor/` - 0 conversions (12 files)
    - `logger/` - 0 conversions (8 files)
    - `matcher/` - 0 conversions (6 files)
    - `stats/` - 0 conversions (7 files)
    - `swagger/` - 0 conversions (5 files)
    - `util/` - 0 conversions (11 files)
    - `vars/` - 0 conversions (15 files)
    - `widget/` - 0 conversions (10 files)
  - **Total Conversions: 40**
  
**Summary:**
The juneau-rest-server module has been comprehensively reviewed. The core REST framework files had the most opportunities for `var` conversion, particularly in RestContext.java with its complex initialization logic. Most package directories contain smaller utility classes with minimal local variable declarations.

##### 8.4 juneau-rest-mock
- `org.apache.juneau.rest.mock.*` (11 Java files)
- REST mocking functionality

##### 8.5 juneau-rest-server-springboot - Completed ✓
- `org.apache.juneau.rest.springboot.*` (5 Java files)
- **SpringBeanStore.java** - 3 conversions (Optional o at lines 59, 74; ApplicationContext ctx at line 78; Stream o at line 105)
- **BasicSpringRestServlet.java** - 1 conversion (String favIcon at line 60)
- **BasicSpringRestServletGroup.java** - No local variables
- **SpringRestServlet.java** - No local variables
- **package-info.java** - Package documentation
- **Total: 4 conversions**

##### 8.6 juneau-rest-server-rdf - Completed ✓
- `org.apache.juneau.rest.rdf.*` (1 Java file)
- **BasicUniversalJenaConfig.java** - Annotation interface only (no code)
- **Total: 0 conversions**

**Expected Opportunities:**
- Variables in REST request/response processing
- Variables in HTTP header handling
- Variables in URL parsing and routing
- Variables in content negotiation
- Variables in REST client operations
- Variables in Spring Boot integration

### 9. juneau-microservice (All Submodules)
**Priority: Medium** - Microservice framework

#### Submodules:
- `juneau-microservice-core` (19 Java files)
- `juneau-microservice-jetty` (9 Java files)
- `juneau-my-jetty-microservice` (4 Java files)
- `juneau-my-springboot-microservice` (5 Java files)

**Expected Opportunities:**
- Variables in microservice configuration
- Variables in Jetty integration
- Variables in Spring Boot microservice setup

### 10. juneau-sc (All Submodules)
**Priority: Low** - Service Container functionality

#### Submodules:
- `juneau-sc-client` (1 Java file)
- `juneau-sc-server` (10 Java files)

### 11. juneau-examples (All Submodules)
**Priority: Low** - Example code

#### Submodules:
- **`juneau-examples-core` (34 Java files) - Completed ✓**
  - `ImageSerializer.java` - 3 conversions (image, mediaType, os)
  - `ImageParser.java` - 2 conversions (is, image)
  - `UonExample.java` - 3 conversions (pojo, serial, obj)
  - `UonComplexExample.java` - 6 conversions (values, setOne, setTwo, pojoc, uonSerializer, obj)
  - `HtmlSimpleExample.java` - 6 conversions (htmlSerializer, htmlParser, pojo, flat, parse, docSerialized)
  - `HtmlComplexExample.java` - 9 conversions (htmlSerializer, htmlParser, values, setOne, setTwo, pojoc, flat, parse)
  - `OapiExample.java` - 11 conversions (oapiSerializer, oapiParser, pojo, flat, parse, schema, value, output, schemab, s, httpPart, p)
  - `SvlExample.java` - 1 conversion (vr)
  - `JsonSimpleExample.java` - Already uses `var` (all variables already converted)
  - `JsonComplexExample.java` - Already uses `var` (all variables already converted)
  - `JsonConfigurationExample.java` - Already uses `var` (all variables already converted)
  - `XmlSimpleExample.java` - Already uses `var` (all variables already converted)
  - `XmlComplexExample.java` - Already uses `var` (all variables already converted)
  - `XmlConfigurationExample.java` - Already uses `var` (all variables already converted)
  - `AtomHtmlExample.java` - Already uses `var` (all variables already converted)
  - `AtomJsonExample.java` - Already uses `var` (all variables already converted)
  - `AtomXmlExample.java` - Already uses `var` (all variables already converted)
  - `BeanExample.java` - Already uses `var` (all variables already converted)
  - `SqlStore.java` - Already uses `var` (all local variables already converted; field declarations cannot use var)
  - All other files (Pojo.java, PojoComplex.java, AtomFeed.java, package-info.java files) - Simple POJOs or package documentation, no local variable declarations
  - **Total: 41 new var conversions**
- **`juneau-examples-rest` (14 Java files) - Completed ✓**
  - Most files are simple REST resources with annotations and fluent builders
  - `PhotosResource.java` - 3 conversions (Photo p variables)
  - All other files have no local variable declarations
  - **Total: 3 var conversions**
- **`juneau-examples-rest-jetty` (2 Java files) - Completed ✓ (No opportunities)**
  - `App.java` - Main method with fluent builder chain, no local variables
  - **Total: 0 var conversions**
- `juneau-examples-rest-jetty-ftest` (7 Java files)
- **`juneau-examples-rest-springboot` (5 Java files) - Completed ✓ (No opportunities)**
  - `App.java` - Spring Boot configuration with @Bean methods, no local variables
  - `RootResources.java` - Annotation-only class
  - `HelloWorldResource.java` - Simple REST resource, no local variables
  - `HelloWorldMessageProvider.java` - Simple supplier implementation, no local variables
  - **Total: 0 var conversions**

**Expected Opportunities:**
- Variables in example code
- Variables in test examples

### 12. juneau-utest
**Priority: High** - Test suite with many local variables

#### Packages to Review:
- `org.apache.juneau.*` (821 Java files)
- All test classes and test utilities

**Expected Opportunities:**
- Variables in test methods
- Variables in test setup/teardown
- Variables in assertion helpers
- Variables in test data creation
- Variables in mock object creation

## Implementation Strategy

### Phase 1: High-Impact, Low-Risk Modules
1. **juneau-core/juneau-common** - Core utilities
2. **juneau-core/juneau-assertions** - Test utilities
3. **juneau-utest** - Test suite

### Phase 2: Core Framework Modules
1. **juneau-core/juneau-marshall** - Main marshalling framework
2. **juneau-rest** - REST framework
3. **juneau-core/juneau-config** - Configuration

### Phase 3: Specialized Modules
1. **juneau-bean** - Bean processing
2. **juneau-microservice** - Microservice framework
3. **juneau-core/juneau-bct** - BCT framework

### Phase 4: Supporting Modules
1. **juneau-core/juneau-marshall-rdf** - RDF support
2. **juneau-sc** - Service Container
3. **juneau-examples** - Examples

## Search Patterns to Look For

### 1. Simple Variable Declarations
```java
// Before
String result = methodCall();
List<String> items = new ArrayList<>();
Map<String, Object> map = new HashMap<>();

// After
var result = methodCall();
var items = new ArrayList<String>();
var map = new HashMap<String, Object>();
```

### 2. Loop Variables
```java
// Before
for (String item : items) { ... }
for (Map.Entry<String, Object> entry : map.entrySet()) { ... }

// After
for (var item : items) { ... }
for (var entry : map.entrySet()) { ... }
```

### 3. Try-with-Resources
```java
// Before
try (FileInputStream fis = new FileInputStream(file)) { ... }

// After
try (var fis = new FileInputStream(file)) { ... }
```

### 4. Builder Pattern Variables
```java
// Before
StringBuilder sb = new StringBuilder();
BeanContext.Builder builder = BeanContext.create();

// After
var sb = new StringBuilder();
var builder = BeanContext.create();
```

### 5. Method Call Results
```java
// Before
ClassMeta<?> cm = getClassMeta();
BeanPropertyMeta bpm = getPropertyMeta();

// After
var cm = getClassMeta();
var bpm = getPropertyMeta();
```

## Automated Search Commands

### Find Variable Declarations to Convert
```bash
# Find common patterns
grep -r "^\s*[A-Z][a-zA-Z0-9_]*\s\+[a-zA-Z_][a-zA-Z0-9_]*\s*=" --include="*.java" .

# Find loop variables
grep -r "for\s*(\s*[A-Z][a-zA-Z0-9_]*\s\+[a-zA-Z_][a-zA-Z0-9_]*\s*:" --include="*.java" .

# Find try-with-resources
grep -r "try\s*(\s*[A-Z][a-zA-Z0-9_]*\s\+[a-zA-Z_][a-zA-Z0-9_]*\s*=" --include="*.java" .
```

### Find Specific Types
```bash
# Find String declarations
grep -r "^\s*String\s\+[a-zA-Z_][a-zA-Z0-9_]*\s*=" --include="*.java" .

# Find List declarations
grep -r "^\s*List<[^>]*>\s\+[a-zA-Z_][a-zA-Z0-9_]*\s*=" --include="*.java" .

# Find Map declarations
grep -r "^\s*Map<[^>]*>\s\+[a-zA-Z_][a-zA-Z0-9_]*\s*=" --include="*.java" .

# Find StringBuilder declarations
grep -r "^\s*StringBuilder\s\+[a-zA-Z_][a-zA-Z0-9_]*\s*=" --include="*.java" .
```

## Quality Assurance

### Before Conversion
1. Ensure the variable type is obvious from context
2. Verify the initializer is present
3. Check that the variable is local (not a field)
4. Confirm the type inference will be correct

### After Conversion
1. Compile the code to ensure no type errors
2. Run tests to verify functionality
3. Check that the code is still readable
4. Verify that the inferred type is as expected

## Expected Benefits

1. **Reduced Verbosity**: Less repetitive type declarations
2. **Improved Readability**: Focus on variable names rather than types
3. **Easier Refactoring**: Less type maintenance when changing APIs
4. **Modern Java**: Using current language features
5. **Consistency**: Uniform coding style across the codebase

## Estimated Impact

- **Total Java Files**: ~1,500+ files
- **Estimated Variable Declarations**: ~10,000+ opportunities
- **Expected Conversions**: ~3,000-5,000 variables
- **Time Estimate**: 2-3 weeks for systematic conversion
- **Risk Level**: Low (local variable changes only)

## Notes

- This is a systematic, non-breaking change
- All conversions are local variable declarations only
- No changes to public APIs or method signatures
- Maintains full type safety
- Improves code readability and maintainability
- Can be done incrementally by module
- Easy to review and verify changes
