# Check Fluent Setter Overrides Script

## Purpose

This script scans the Juneau codebase to find missing fluent setter overrides in subclasses.

## Problem

Juneau uses fluent-style setters extensively for method chaining:

```java
public class X {
    public X setY(Y y) {
        this.y = y;
        return this;
    }
}
```

When a class extends another class with fluent setters, it should override those setters to return the correct subclass type:

```java
public class X2 extends X {
    @Override 
    public X2 setY(Y y) {
        super.setY(y);
        return this;
    }
}
```

Without these overrides, method chaining breaks:

```java
// Without override - compiler error!
X2 obj = new X2()
    .setY(y)    // Returns X, not X2
    .setX2SpecificMethod();  // Error: X doesn't have this method

// With proper override - works!
X2 obj = new X2()
    .setY(y)    // Returns X2
    .setX2SpecificMethod();  // OK
```

## Usage

### Basic Usage

Run from the Juneau root directory:

```bash
python3 scripts/check-fluent-setter-overrides.py
```

Or run directly (script is executable):

```bash
./scripts/check-fluent-setter-overrides.py
```

### Output

The script will:
1. Scan all Java files in the source tree
2. Identify classes and their inheritance relationships
3. Find fluent setter methods (methods that return `this`)
4. Check if subclasses properly override these setters
5. Report any missing overrides grouped by class

Example output:

```
Juneau Fluent Setter Override Checker
==================================================

Scanning for Java files...
Found 2847 Java files

Extracting class information...
Found 1523 classes
Found 3421 fluent setter methods

Building class hierarchy...

Checking for missing fluent setter overrides...

MISSING OVERRIDES (23 found):
==================================================

Class: RestClientBuilder
  File: juneau-rest/juneau-rest-client/src/main/java/org/apache/juneau/rest/client/RestClientBuilder.java
  Missing 5 override(s):
    - debug(Enablement)
      From parent: BeanContextBuilder
    - locale(Locale)
      From parent: BeanContextBuilder
    - mediaType(MediaType)
      From parent: BeanContextBuilder
    - timeZone(TimeZone)
      From parent: BeanContextBuilder
    - beansRequireDefaultConstructor()
      From parent: BeanContextBuilder

Class: HtmlDocSerializerBuilder
  File: juneau-core/juneau-marshall/src/main/java/org/apache/juneau/html/HtmlDocSerializerBuilder.java
  Missing 3 override(s):
    - addBeanTypes()
      From parent: HtmlSerializerBuilder
    - detectRecursions()
      From parent: SerializerBuilder
    - ignoreRecursions()
      From parent: SerializerBuilder

==================================================
Total missing overrides: 23

Note: These are informational and do not fail the build.
Consider adding these overrides to maintain fluent API consistency.
```

## What the Script Checks

The script identifies fluent setters by looking for:
- Public methods that return the class type (e.g., `public X setFoo(...)`)
- Methods that contain `return this;` in their body
- Methods in parent classes that should be overridden in subclasses

## What to Do with Results

When the script reports missing overrides:

1. **Review the findings** - Not all reported methods may need overrides
2. **Add missing overrides** - For methods that should be overridden:

```java
@Override
public SubClass methodName(ParamType param) {
    super.methodName(param);
    return this;
}
```

3. **Consider fluent API design** - Ensure method chaining works correctly across the inheritance hierarchy

## Limitations

- The script uses regex-based parsing (not a full Java parser)
- May not catch all edge cases (inner classes, complex generics, etc.)
- Reports are informational only - manual review is recommended
- Does not validate that overrides are implemented correctly

## Integration

This script can be run:
- Manually during development
- As part of code review process
- In CI/CD pipelines (informational only)
- Before releases to ensure API consistency

## Exit Code

The script always exits with code 0 (success) to avoid failing builds. Results are informational only.

