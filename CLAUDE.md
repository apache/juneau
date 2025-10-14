# Claude AI Assistant Rules for Apache Juneau

This document outlines the rules, guidelines, and best practices that Claude AI follows when working on the Apache Juneau project.

**Note**: This file is referred to as "my rules" and serves as the definitive reference for all guidelines and conventions I follow when working on the Apache Juneau project.

## Core Working Principles

### 1. Code Quality and Consistency
- Follow existing code patterns and conventions
- Maintain consistency with the existing codebase
- Use established naming conventions and formatting
- Preserve existing functionality while making improvements

### 2. Testing Standards
- Ensure comprehensive test coverage for all changes
- Follow the established unit testing patterns
- Use the Sequential Single-Letter Label Convention (SSLLC)
- Implement proper assertion patterns using `assertBean()`

### 3. Documentation Standards
- Maintain comprehensive javadoc documentation
- Follow established documentation formatting rules
- Include practical examples in documentation
- Link to relevant specifications and resources

### 4. Build and Compilation Issues

**IMPORTANT: Maven vs IDE Compilation Divergence**

When compiling code using Maven and the user compiles through their IDE (Eclipse), the compiled code can diverge, leading to unexpected behavior such as:
- Code changes not being reflected in test runs
- Old code behavior persisting despite source changes
- Stale class files causing incorrect results

**Resolution Strategy:**
If you encounter situations where your code changes don't appear to be taking effect:
1. **Immediately run** `mvn clean install` to clear all compiled artifacts and rebuild fresh
2. This ensures Maven and IDE compiled code are synchronized
3. Rerun tests after the clean build to verify changes are properly reflected

**When to suspect this issue:**
- Tests fail in unexpected ways after code changes
- Behavior doesn't match recent code modifications
- Test results seem to reflect old code despite edits
- Compilation succeeds but runtime behavior is wrong

**Java Runtime Location:**
If you can't find Java on the file system using standard commands, look for it in the `~/jdk` folder. For example:
- Java 17 can be found at: `~/jdk/openjdk_17.0.14.0.101_17.57.18_aarch64/bin/java`
- Use this path when you need to run Java commands directly

### 5. HTML5 Bean Enhancement Rules

#### Javadoc Enhancement for HTML5 Beans
- **Class-level documentation**: Add comprehensive descriptions of the HTML element's purpose
- **Attribute documentation**: Provide detailed descriptions of what each attribute does
- **Enumerated values**: List all possible values for attributes that have them (e.g., `contenteditable` can have `true`, `false`, `plaintext-only`)
- **Boolean attributes**: Document the deminimized behavior (e.g., `hidden(true)` produces `hidden='hidden'`)
- **Examples**: Include multiple practical examples showing different use cases

#### HtmlBuilder Integration
- **Javatree structure**: Add javatree documentation for HtmlBuilder creator methods
- **Example refactoring**: Replace constructor-based examples with HtmlBuilder methods where appropriate
- **Rule for examples**: Leave examples as-is if they use only strings and have no children defined
- **Builder method references**: Use the specified javatree format for referencing HtmlBuilder methods

#### Javatree Format for HtmlBuilder Methods
```java
/**
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#methodName() methodName()}
 * 		<li class='jm'>{@link HtmlBuilder#methodName(Object, Object...) methodName(Object, Object...)}
 * 	</ul>
 * </ul>
 * </p>
 */
```

### 5. Swagger/OpenAPI3 Bean Testing Rules

#### Collection Property Method Consistency
All collection bean properties must have exactly 4 methods:
1. `setX(X...)` - varargs setter
2. `setX(Collection<X>)` - Collection setter  
3. `addX(X...)` - varargs adder
4. `addX(Collection<X>)` - Collection adder

#### Test Structure for D_additionalMethods
The `D_additionalMethods` test class should contain three tests:

1. **d01_collectionSetters**: Tests `setX(Collection<X>)` methods
2. **d02_varargAdders**: Tests `addX(X...)` methods, with each method called twice
3. **d03_collectionAdders**: Tests `addX(Collection<X>)` methods, with each method called twice

#### Varargs vs Collection Setter Testing
- **A_basicTests**: Use varargs setters (e.g., `setTags("tag1", "tag2")`)
- **D_additionalMethods**: Test Collection setters (e.g., `setTags(list("tag1", "tag2"))`)

#### C_extraProperties Testing
- Use `set("propertyName", value)` instead of fluent setters
- Cover ALL the same properties as `A_basicTests`
- Match values from `A_basicTests` exactly
- Example: `set("basePath", "a")` instead of `setBasePath("a")`

### 6. Error Handling and Validation
- Use `assertThrowsWithMessage` for exception testing
- Test both valid and invalid scenarios
- Include proper error messages in assertions
- Test null parameter validation where applicable

### 7. Code Coverage Guidelines
- Aim for 100% instruction coverage on bean classes
- Use JaCoCo reports to identify missing coverage
- Focus on methods with 0% coverage first
- Add comprehensive tests for all code paths

### 8. File Organization and Naming
- Follow established file naming conventions
- Maintain proper package structure
- Use consistent import organization
- Follow established class and method naming patterns

### 9. Documentation Links and References
- Use hardcoded links to `https://juneau.apache.org/docs/topics/`
- Include specification links for OpenAPI/Swagger properties
- Use proper cross-references with `{@link}` tags
- Maintain consistent link formatting

### 10. Systematic Approach
- Work through tasks systematically and alphabetically when specified
- Complete tasks without breaks when requested
- Verify all changes work correctly
- Maintain consistency across similar files

---

# Documentation Guidelines for Apache Juneau

This document outlines the documentation conventions, formatting rules, and best practices for the Apache Juneau project.

## Javadoc Formatting Standards

### HTML Tags and Formatting

**Java Variables in Examples**:
- Wrap local Java variables in `<jv>` tags
- Example: `<jv>json</jv>`, `<jv>swagger</jv>`, `<jv>result</jv>`

**Static Method References**:
- Use `<jsm>` tags for static method names
- Example: `Json.<jsm>from</jsm>(<jv>x</jv>)`

**Static Field References**:
- Use `<jsf>` tags for static field names
- Example: `JsonSerializer.<jsf>DEFAULT</jsf>`

**Code Comments**:
- Use `<jc>` tags for code comments
- Example: `<jc>// Serialize using JsonSerializer.</jc>`

**Class References**:
- Use `<jk>` tags for class names in text
- Example: `<jk>null</jk>`, `<jk>String</jk>`

### Syntax Highlighting Tags (from juneau-code.css)

**Java Code Tags**:
- `<jc>` - Java comment (green)
- `<jd>` - Javadoc comment (blue)
- `<jt>` - Javadoc tag (blue, bold)
- `<jk>` - Java keyword (purple, bold)
- `<js>` - Java string (blue)
- `<jf>` - Java field (dark blue)
- `<jsf>` - Java static field (dark blue, italic)
- `<jsm>` - Java static method (italic)
- `<ja>` - Java annotation (grey)
- `<jp>` - Java parameter (brown)
- `<jv>` - Java local variable (brown)

**XML Code Tags**:
- `<xt>` - XML tag (dark cyan)
- `<xa>` - XML attribute (purple)
- `<xc>` - XML comment (medium blue)
- `<xs>` - XML string (blue, italic)
- `<xv>` - XML value (black)

**JSON Code Tags**:
- `<joc>` - JSON comment (green)
- `<jok>` - JSON key (purple)
- `<jov>` - JSON value (blue)

**URL Encoding/UON Tags**:
- `<ua>` - Attribute name (black)
- `<uk>` - true/false/null (purple, bold)
- `<un>` - Number value (dark blue)
- `<us>` - String value (blue)

**Manifest File Tags**:
- `<mc>` - Manifest comment (green)
- `<mk>` - Manifest key (dark red, bold)
- `<mv>` - Manifest value (dark blue)
- `<mi>` - Manifest import (dark blue, italic)

**Config File Tags**:
- `<cc>` - Config comment (green)
- `<cs>` - Config section (dark red, bold)
- `<ck>` - Config key (dark red)
- `<cv>` - Config value (dark blue)
- `<ci>` - Config import (dark red, bold, italic)

**Special Tags**:
- `<c>` - Synonym for `<code>`
- `<dc>` - Deleted code (strikethrough)
- `<bc>` - Bold code (bold)

**Code Block Classes**:
- `bcode` - Bordered code block
- `bjava` - Bordered Java code block
- `bjson` - Bordered JSON code block
- `bxml` - Bordered XML code block
- `bini` - Bordered INI code block
- `buon` - Bordered UON code block
- `burlenc` - Bordered URL encoding code block
- `bconsole` - Bordered console output (black background, yellow text)
- `bschema` - Bordered schema code block
- `code` - Unbordered code block

### Javadoc Structure

**Standard Method Javadoc**:
```java
/**
 * Brief description of what the method does.
 *
 * <p>
 * Longer description if needed, explaining the purpose and behavior.
 * </p>
 *
 * @param paramName Description of the parameter.
 * @return Description of what is returned.
 * @throws ExceptionType Description of when this exception is thrown.
 */
```

**Property Documentation**:
```java
/**
 * The property name.
 *
 * @param value The new value for this property.
 * @return This object.
 */
```

### Parameter Documentation

**Standard Parameters**:
- Use `value` as the parameter name for fluent setters
- Document parameter types and constraints
- Specify when parameters can be null

**Null Parameter Handling**:
```java
/**
 * Sets the property value.
 *
 * @param value The new value. Can be <jk>null</jk> to unset the property.
 * @return This object.
 */
```

**Required Parameters**:
```java
/**
 * Sets the property value.
 *
 * @param value The new value. Must not be <jk>null</jk>.
 * @return This object.
 */
```

## Link and Reference Standards

### Documentation Links

**Juneau Documentation Site**:
- Use hardcoded links to `https://juneau.apache.org/docs/topics/`
- Use slug names as topic names
- Use page titles for anchor text

**Examples**:
```java
/**
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanSwagger2">juneau-bean-swagger-v2</a>
 * </ul>
 */
```

**OpenAPI/Swagger Specification Links**:
```java
/**
 * <a class="doclink" href="https://swagger.io/specification/v2/#operationObject">operation</a> object.
 */
```

### Cross-References

**Internal Class References**:
- Use `{@link ClassName}` for internal class references
- Use `{@link ClassName#methodName}` for method references

**External References**:
- Use `<a class="doclink" href="URL">text</a>` for external links
- Include specification links for OpenAPI/Swagger properties

## Code Examples

### JSON Serialization Examples

**Standard Pattern**:
```java
/**
 * <jc>// Serialize using JsonSerializer.</jc>
 * String <jv>json</jv> = Json.<jsm>from</jsm>(<jv>x</jv>);
 *
 * <jc>// Or just use toString() which does the same as above.</jc>
 * String <jv>json</jv> = <jv>x</jv>.toString();
 */
```

**Consistency Requirements**:
- Always include both `Json.from()` and `toString()` examples
- Use consistent variable names (`<jv>json</jv>`, `<jv>x</jv>`)
- Include explanatory comments

### Method Usage Examples

**Fluent Setter Examples**:
```java
/**
 * <jc>// Create a link element</jc>
 * A <jv>link</jv> = a().href("https://example.com").target("_blank");
 */
```

**Builder Pattern Examples**:
```java
/**
 * <jc>// Create a Swagger document</jc>
 * Swagger <jv>swagger</jv> = swagger()
 *     .info(info().title("My API").version("1.0"))
 *     .path("/users", pathItem().get(operation().summary("Get users")));
 */
```

## Property Documentation

### HTML5 Bean Properties

**Standard Attribute Documentation**:
```java
/**
 * <a class="doclink" href="https://www.w3.org/TR/html5/links.html#attr-hyperlink-href">href</a> attribute.
 *
 * @param value The URL. Typically a {@link URL} or {@link String}.
 * @return This object.
 */
```

**Boolean Attribute Documentation**:
```java
/**
 * <a class="doclink" href="https://www.w3.org/TR/html5/forms.html#attr-input-readonly">readonly</a> attribute.
 *
 * @param value The readonly value.
 * @return This object.
 */
```

### Swagger/OpenAPI Properties

**Standard Property Documentation**:
```java
/**
 * The <a class="doclink" href="https://swagger.io/specification/v2/#operationObject">operation</a> summary.
 *
 * @param value A brief description of the operation.
 * @return This object.
 */
```

## Class Documentation

### Class-Level Javadoc

**Standard Structure**:
```java
/**
 * Brief description of the class purpose.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ModuleName">module-name</a>
 * </ul>
 */
```

**Builder Classes**:
```java
/**
 * Builder class for creating {@link ClassName} objects.
 *
 * <p>
 * This class provides fluent methods for building complex objects.
 * </p>
 */
```

## Validation and Error Documentation

### Parameter Validation

**Null Parameter Validation**:
```java
/**
 * Sets the property value.
 *
 * @param value The new value. Must not be <jk>null</jk>.
 * @throws IllegalArgumentException If value is <jk>null</jk>.
 * @return This object.
 */
```

**String Validation**:
```java
/**
 * Sets the property value.
 *
 * @param value The new value. Must not be <jk>null</jk> or blank.
 * @throws IllegalArgumentException If value is <jk>null</jk> or blank.
 * @return This object.
 */
```

### Strict Mode Documentation

**Strict Mode Behavior**:
```java
/**
 * Sets the property value.
 *
 * <p>
 * In strict mode, invalid values will throw {@link RuntimeException}.
 * In non-strict mode, invalid values are ignored.
 * </p>
 *
 * @param value The new value.
 * @return This object.
 */
```

## Special Cases

### Constructor Documentation

**Standard Constructor**:
```java
/**
 * Constructor.
 *
 * @param children The child nodes.
 */
public ClassName(Object...children) {
```

**Parameterized Constructor**:
```java
/**
 * Constructor.
 *
 * @param param1 Description of first parameter.
 * @param param2 Description of second parameter.
 */
public ClassName(Type1 param1, Type2 param2) {
```

### Override Documentation

**Method Overrides**:
```java
@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
public ClassName methodName(Type value) {
```

**Interface Implementations**:
```java
@Override /* GENERATED - org.apache.juneau.bean.html5.HtmlElement */
public ClassName methodName(Type value) {
```

## Best Practices

### Consistency
- Use consistent parameter names (`value` for fluent setters)
- Use consistent variable names in examples
- Use consistent link formatting

### Completeness
- Document all public methods and constructors
- Include parameter types and constraints
- Include return value descriptions
- Include exception documentation

### Clarity
- Use clear, concise descriptions
- Include practical examples
- Link to relevant specifications
- Explain complex behavior

### Maintenance
- Keep documentation up-to-date with code changes
- Use consistent formatting throughout
- Include cross-references where helpful
- Document edge cases and special behavior

## Common Patterns

### Fluent Setter Documentation
```java
/**
 * Sets the property value.
 *
 * @param value The new value for this property.
 * @return This object.
 */
public ClassName property(Type value) {
```

### Collection Setter Documentation
```java
/**
 * Sets the collection of items.
 *
 * @param value The new collection. Can be <jk>null</jk> to unset the property.
 * @return This object.
 */
public ClassName items(Collection<Item> value) {
```

### Builder Method Documentation
```java
/**
 * Creates a new instance.
 *
 * @return A new instance.
 */
public static ClassName create() {
```

This document serves as the definitive guide for documentation in the Apache Juneau project, ensuring consistency, completeness, and clarity across all documentation.

---

# Unit Testing Guidelines for Apache Juneau

This document outlines the testing conventions, methodologies, and best practices for unit testing in the Apache Juneau project.

## Core Testing Conventions

### Sequential Single-Letter Label Convention (SSLLC)

**Purpose**: Provides consistent, readable naming for test data that improves test maintainability and readability.

**Rules**:
1. **Single values**: Use single letters (`a`, `b`, `c`, etc.)
2. **Multiple values on same line**: Append numbers (`a1`, `a2`, `b1`, `b2`, etc.)
3. **New bean instances**: Reset labels to 'a' when creating new bean instances
4. **Consistent prefixes**: Use consistent prefixes for multiple values on the same line

**Examples**:
```java
// Single values
var a = swagger();
var b = info();

// Multiple values on same line
var a1 = operation("get", "/users");
var a2 = operation("post", "/users");
var b1 = response("200");
var b2 = response("400");

// New bean instance - reset to 'a'
var a = new Swagger();
var b = a.info();
```

### Deep Property Path Assertion Pattern (DPPAP)

**Purpose**: Assert on actual nested property values using deep property paths rather than just collection sizes.

**Usage**: Use `assertBean()` with deep property paths to verify actual values, not just collection sizes.

**Examples**:
```java
// Instead of just checking size
assertNotNull(swagger.getPaths());
assertEquals(2, swagger.getPaths().size());

// Use deep property path assertions
assertBean(swagger, "paths{get:/users{summary,operationId},post:/users{summary,operationId}}");
```

### TestUtils Convenience Methods

**Purpose**: Use TestUtils methods instead of direct serializer/parser calls for consistency and readability.

**Methods**:
- `TestUtils.json(Object)` - Serialize object to JSON string
- `TestUtils.json(String, Class)` - Deserialize JSON string to object
- `TestUtils.jsonRoundTrip(Object, Class)` - Round-trip serialization/deserialization testing

**Examples**:
```java
// Instead of direct serializer calls
String json = Json5Serializer.DEFAULT.toString(swagger);
Swagger parsed = Json5Parser.DEFAULT.parse(json, Swagger.class);

// Use TestUtils convenience methods
String json = json(swagger);
Swagger parsed = json(json, Swagger.class);
Swagger roundTrip = jsonRoundTrip(swagger, Swagger.class);
```

## Test Structure Patterns

### Standard Test Class Structure

```java
public class BeanName_Test extends TestBase {
    
    @Test void A_basicTests() {
        // Test all bean properties using varargs setters
        // Use SSLLC naming convention
        // Use DPPAP for assertions
    }
    
    @Test void B_serialization() {
        // Test JSON serialization/deserialization
        // Use TestUtils convenience methods
    }
    
    @Test void C_extraProperties() {
        // Test set(String, Object) method
        // Use set(String, Object) for ALL the same properties as A_basicTests
        // Match values from A_basicTests exactly
        // Use SSLLC naming convention
        // Example: set("basePath", "a") instead of setBasePath("a")
    }
    
    @Test void D_additionalMethods() {
        // Test additional methods like copyFrom()
        // Use assertBean for property verification
    }
    
    @Test void E_strictMode() {
        // Test strict mode validation
        // Use assertThrowsWithMessage for exception validation
        // Combine invalid/valid tests into single test per validation method
    }
    
    @Test void F_refs() {
        // Test resolveRefs() methods where applicable
        // Test valid refs, invalid refs, null/empty refs, not-found refs
    }
    
    @Test void a08_otherGettersAndSetters() {
        // Test additional getter/setter methods
        // Use assertBean directly for getter tests
        // Test collection variants vs varargs variants
        // Follow SSLLC conventions
    }
    
    @Test void a09_nullParameters() {
        // Test null parameter validation
        // Use assertThrowsWithMessage for validation
    }
}
```

### Test Method Naming Conventions

- **A_basicTests**: Core functionality tests
- **B_serialization**: Serialization/deserialization tests
- **C_extraProperties**: Dynamic property setting tests
- **D_additionalMethods**: Additional method tests
- **E_strictMode**: Strict mode validation tests
- **F_refs**: Reference resolution tests
- **a08_otherGettersAndSetters**: Additional getter/setter tests
- **a09_nullParameters**: Null parameter validation tests

## Assertion Patterns

### assertBean Usage

**Basic Usage**:
```java
assertBean(bean, "property1,property2,property3");
```

**Deep Property Paths**:
```java
assertBean(bean, "paths{get:/users{summary,operationId}}");
```

**Collection Assertions**:
```java
// Use # notation for uniform collections
assertBean(bean, "parameters{#{in,name}}");

// Use explicit indexing for non-uniform collections
assertBean(bean, "parameters{0{in,name},1{in,name}}");
```

**Map Assertions**:
```java
// Use assertMap for map entries
assertMap(map, "key1=value1", "key2=value2");
```

### Exception Testing

**Use assertThrowsWithMessage**:
```java
assertThrowsWithMessage(IllegalArgumentException.class, 
    "Parameter 'name' cannot be null", 
    () -> bean.setName(null));
```

## Bean Testing Patterns

### Swagger/OpenAPI3 Bean Testing

**Property Coverage**: Ensure `A_basicTests` covers all bean properties and `C_extraProperties` covers the same properties using the `set()` method.

**Getter/Setter Variants**: Test both collection variants and varargs variants where applicable.

**Reference Resolution**: Test `resolveRefs()` methods with various scenarios:
- Valid references
- Invalid references  
- Null/empty references
- Not-found references
- Type conversion

### Fluent Setter Testing

**Parameter Naming**: All fluent setters should use `value` as the parameter name for consistency.

**Method Chaining**: Test fluent setter chaining:
```java
var result = bean
    .property1("value1")
    .property2("value2")
    .property3("value3");
```

### Collection Property Method Consistency

**Rule**: All collection bean properties should have exactly 4 methods:
1. `setX(X...)` - varargs setter
2. `setX(Collection<X>)` - Collection setter  
3. `addX(X...)` - varargs adder
4. `addX(Collection<X>)` - Collection adder

**Examples**:
```java
// For a tags property of type Set<String>:
public Bean setTags(String...value) { ... }
public Bean setTags(Collection<String> value) { ... }
public Bean addTags(String...values) { ... }
public Bean addTags(Collection<String> values) { ... }
```

### Varargs vs Collection Setter Testing

**Rule**: When a bean has both varargs and Collection setter methods for the same property:
- **A_basicTests**: Use the varargs version (e.g., `setTags("tag1", "tag2")`)
- **D_additionalMethods**: Test the Collection version (e.g., `setTags(list("tag1", "tag2"))`)

**Examples**:
```java
// A_basicTests - use varargs
.setTags("tag1", "tag2")
.setConsumes(MediaType.of("application/json"))

// D_additionalMethods - test Collection version
.setTags(list("tag1", "tag2"))
.setConsumes(list(MediaType.of("application/json"), MediaType.of("application/xml")))
```

**D_additionalMethods Test Structure**: This test class should contain three tests:

1. **d01_collectionSetters**: Tests `setX(Collection<X>)` methods
   ```java
   @Test void d01_collectionSetters() {
       var x = bean()
           .setTags(list("tag1", "tag2"))
           .setConsumes(list(MediaType.of("application/json"), MediaType.of("application/xml")));
       
       assertBean(x,
           "tags,consumes",
           "[tag1,tag2],[application/json,application/xml]"
       );
   }
   ```

2. **d02_varargAdders**: Tests `addX(X...)` methods - each method should be called twice
   ```java
   @Test void d02_varargAdders() {
       var x = bean()
           .addTags("tag1")
           .addTags("tag2")
           .addConsumes(MediaType.of("application/json"))
           .addConsumes(MediaType.of("application/xml"));
       
       assertBean(x,
           "tags,consumes",
           "[tag1,tag2],[application/json,application/xml]"
       );
   }
   ```

3. **d03_collectionAdders**: Tests `addX(Collection<X>)` methods - each method should be called twice
   ```java
   @Test void d03_collectionAdders() {
       // Note: Collection versions of addX methods exist but are difficult to test
       // due to Java method resolution preferring varargs over Collection
       // For now, we test the basic functionality with varargs versions
       var x = bean();
       
       // Test that the addX methods work by calling them multiple times
       x.addTags("tag1");
       x.addTags("tag2");
       x.addConsumes(MediaType.of("application/json"));
       x.addConsumes(MediaType.of("application/xml"));
       
       assertBean(x,
           "tags,consumes",
           "[tag1,tag2],[application/json,application/xml]"
       );
   }
   ```

In all cases, `assertBean` should be used to validate results.

## Code Coverage Guidelines

### Target Coverage
- **Bean Classes**: Aim for 100% instruction coverage
- **UI Classes**: Can be excluded from coverage targets
- **Builder Classes**: Include comprehensive tests for all builder methods

### Coverage Analysis
- Use JaCoCo reports to identify missing coverage
- Focus on methods with 0% coverage first
- Add tests for uncovered code paths
- Verify coverage improvements after adding tests

## Best Practices

### Test Data Management
- Use SSLLC for consistent test data naming
- Reset labels when creating new bean instances
- Use meaningful test data that represents real-world scenarios

### Assertion Strategy
- Prefer `assertBean()` over individual property assertions
- Use deep property paths for comprehensive validation
- Test both positive and negative scenarios

### Code Organization
- Group related tests in logical test methods
- Use descriptive test method names
- Follow consistent test structure across all test classes

### Documentation
- Include javadoc for test methods explaining their purpose
- Document any special test scenarios or edge cases
- Keep test code readable and maintainable

## Common Patterns

### Round-trip Testing
```java
@Test void B_serialization() {
    var original = createTestBean();
    var roundTrip = jsonRoundTrip(original, BeanClass.class);
    assertBean(original, roundTrip);
}
```

### Strict Mode Testing
```java
@Test void E_strictMode() {
    // Test invalid value with strict mode
    assertThrowsWithMessage(RuntimeException.class, 
        "Invalid value", 
        () -> bean.setProperty("invalid"));
    
    // Test valid value with strict mode
    assertDoesNotThrow(() -> bean.setProperty("valid"));
}
```

### Collection Testing
```java
@Test void a08_otherGettersAndSetters() {
    var a = bean.addItems("item1", "item2");
    assertBean(a, "items{0=item1,1=item2}");
    
    var b = bean.setItems(Arrays.asList("item3", "item4"));
    assertBean(b, "items{0=item3,1=item4}");
}
```

## Serializer and Parser Implementation Rules

### Adding Settings to Serializers/Parsers

When adding a new setting (configuration property) to a serializer or parser, follow these steps:

#### 1. Add Field to Builder Class
```java
public static class Builder extends XmlSerializer.Builder {
    String textNodeDelimiter;  // Add the field
}
```

#### 2. Initialize in Constructor
```java
protected Builder() {
    textNodeDelimiter = env("XmlSerializer.textNodeDelimiter", "");  // Set default
}
```

#### 3. Add to Copy Constructors
```java
protected Builder(XmlSerializer copyFrom) {
    super(copyFrom);
    textNodeDelimiter = copyFrom.textNodeDelimiter;  // Copy from serializer
}

protected Builder(Builder copyFrom) {
    super(copyFrom);
    textNodeDelimiter = copyFrom.textNodeDelimiter;  // Copy from builder
}
```

#### 4. Add Setter Method
```java
public Builder textNodeDelimiter(String value) {
    textNodeDelimiter = value == null ? "" : value;
    return this;
}
```

#### 5. **CRITICAL: Update hashKey() Method**
This is essential to prevent caching issues where different configurations would incorrectly share the same cached instance:

```java
@Override /* Context.Builder */
public HashKey hashKey() {
    return HashKey.of(
        super.hashKey(),
        addBeanTypesXml,
        addNamespaceUrisToRoot,
        // ... other fields ...
        textNodeDelimiter  // ADD NEW SETTING HERE
    );
}
```

**Why this is critical**: Serializers and parsers use caching based on the hash key. If a new setting is not included in the hash key, two builders with different values for that setting will hash to the same key and incorrectly use the same cached instance, causing the second configuration to be ignored.

#### 6. Add Field to Main Class
```java
public class XmlSerializer extends WriterSerializer {
    final String textNodeDelimiter;  // Add to main class
    
    public XmlSerializer(Builder builder) {
        super(builder);
        textNodeDelimiter = builder.textNodeDelimiter;  // Initialize from builder
    }
}
```

#### 7. Pass to Session (if needed)
If the setting needs to be accessed during serialization:

```java
public class XmlSerializerSession extends WriterSerializerSession {
    private final String textNodeDelimiter;
    
    protected XmlSerializerSession(Builder builder) {
        super(builder);
        textNodeDelimiter = ctx.textNodeDelimiter;  // Get from context
    }
}
```

#### 8. Override in Subclasses (if needed)
If the serializer has subclasses (e.g., `HtmlSerializer` extends `XmlSerializer`), override the setter to maintain fluent API:

```java
// In HtmlSerializer.Builder
@Override
public Builder textNodeDelimiter(String value) {
    super.textNodeDelimiter(value);  // Call parent
    return this;  // Return correct type
}
```

### Common Pitfalls
- ❌ **Forgetting to add the setting to `hashKey()`** - This causes caching bugs where different configurations share the same cached instance
- ❌ Not copying the field in all copy constructors
- ❌ Not overriding setter methods in subclass builders
- ❌ Not passing the setting to the session if it's needed during serialization

This document serves as the definitive guide for unit testing in the Apache Juneau project, ensuring consistency, maintainability, and comprehensive test coverage.

---

## Release Notes Guidelines

### Location
When asked to "add to the release notes", this refers to the current release file located at:
- `/juneau-docs/docs/release-notes/<VERSION>.md`
- **Current version**: `9.2.0`
- **Current file**: `/juneau-docs/docs/release-notes/9.2.0.md`

### Structure
Release notes are organized into two main sections:

1. **Top-level major changes** - High-level overview at the beginning of the file listing significant changes
2. **Per-module updates** - Detailed changes organized by module (similar to 9.0.0.md structure):
   - `juneau-marshall`
   - `juneau-rest-common`
   - `juneau-rest-server`
   - `juneau-rest-client`
   - `juneau-dto`
   - `juneau-microservice`
   - `juneau-examples`
   - Other modules as applicable

### Format
Each section should include:
- New features
- Bug fixes
- Breaking changes
- Deprecations
- Performance improvements
- Documentation updates
- API changes

### Process
1. Read the current release notes file (9.2.0.md) to understand the existing structure
2. Determine if the change is a major change (top-level) or module-specific
3. Add new entries under the appropriate section and module
4. Use clear, concise descriptions with code examples where helpful
5. Include issue/PR references where applicable
6. Maintain consistent formatting with existing entries (see 9.0.0.md for reference)
