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
        // Match values from A_basicTests
        // Use SSLLC naming convention
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

This document serves as the definitive guide for unit testing in the Apache Juneau project, ensuring consistency, maintainability, and comprehensive test coverage.
