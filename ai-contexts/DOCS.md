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
