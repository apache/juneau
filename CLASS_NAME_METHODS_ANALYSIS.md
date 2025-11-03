# Class Name Methods Analysis

## Goal
Identify all class name methods in `ClassUtils` and `ClassInfo` to evaluate consolidation opportunities.

---

## Methods Inventory

### ClassUtils Methods

| Method | Returns | Package? | Outer Class? | Type Params? | Separator | Arrays | Example Output |
|--------|---------|----------|--------------|--------------|-----------|--------|----------------|
| `className(Object)` | String | ✅ Yes | ✅ $ | ❌ No | $ | JVM format | `"java.lang.String"`, `"java.util.Map$Entry"`, `"[Ljava.lang.String;"` |
| `simpleClassName(Object)` | String | ❌ No | ❌ No | ❌ No | N/A | [] | `"String"`, `"Entry"`, `"String[]"` |
| `simpleQualifiedClassName(Object)` | String | ❌ No | ✅ Yes | ❌ No | . (dot) | [] | `"String"`, `"Map.Entry"`, `"String[]"` |

### Utils Shortcuts (delegate to ClassUtils)

| Method | Delegates To |
|--------|--------------|
| `cn(Object)` | `ClassUtils.className(Object)` |
| `scn(Object)` | `ClassUtils.simpleClassName(Object)` |
| `sqcn(Object)` | `ClassUtils.simpleQualifiedClassName(Object)` |

### ClassInfo Methods

| Method | Returns | Package? | Outer Class? | Type Params? | Separator | Arrays | Notes |
|--------|---------|----------|--------------|--------------|-----------|--------|-------|
| `getName()` | String | ✅ Yes | ✅ $ | ❌ No | $ | JVM format | Delegates to `Class.getName()` or `Type.getTypeName()` |
| `getCanonicalName()` | String | ✅ Yes | ✅ . | ❌ No | . (dot) | [] | Delegates to `Class.getCanonicalName()` |
| `getSimpleName()` | String | ❌ No | ❌ No | ❌ No | N/A | [] | Delegates to `Class.getSimpleName()` or `Type.getTypeName()` |
| `getFullName()` | String | ✅ Yes | ✅ $ | ✅ **Yes** | $ | [] | **Includes generics!** `"java.util.HashMap<java.lang.String,java.lang.Integer>"` |
| `getShortName()` | String | ❌ No | ✅ $ | ✅ **Yes** | $ | [] | **Includes generics!** `"HashMap<String,Integer>"`, `"Outer$Inner"` |
| `getReadableName()` | String | ❌ No | ❌ No | ❌ No | N/A | "Array" | `"String"`, `"StringArray"`, `"StringArrayArray"` |
| `appendFullName(StringBuilder)` | StringBuilder | ✅ Yes | ✅ $ | ✅ **Yes** | $ | [] | **Core implementation for `getFullName()`** |
| `appendShortName(StringBuilder)` | StringBuilder | ❌ No | ✅ $ | ✅ **Yes** | $ | [] | **Core implementation for `getShortName()`** |

---

## Core Implementation Methods

### 10. `ClassInfo.appendFullName(StringBuilder)` ⭐ CORE METHOD
**What it does**: Appends full name with type parameters to a StringBuilder (no new allocations)
**Called by**: `getFullName()`
**Recursive**: Yes - calls itself for nested type parameters

**Implementation Logic**:
```java
public StringBuilder appendFullName(StringBuilder sb) {
    Class<?> ct = getComponentType().inner();
    int dim = getDimensions();
    
    // Non-array, non-parameterized: simple delegation
    if (ct != null && dim == 0 && !isParameterizedType)
        return sb.append(ct.getName());
    
    // Append base class name
    sb.append(ct != null ? ct.getName() : t.getTypeName());
    
    // Append type parameters recursively
    if (isParameterizedType) {
        ParameterizedType pt = (ParameterizedType)t;
        sb.append('<');
        boolean first = true;
        for (Type t2 : pt.getActualTypeArguments()) {
            if (!first) sb.append(',');
            first = false;
            ClassInfo.of(t2).appendFullName(sb);  // RECURSIVE!
        }
        sb.append('>');
    }
    
    // Append array brackets
    for (int i = 0; i < dim; i++)
        sb.append("[]");
    
    return sb;
}
```

**Example Outputs**:
```java
// Simple class
appendFullName(sb) for String.class
→ "java.lang.String"

// Parameterized type
appendFullName(sb) for HashMap<String, List<Integer>>
→ "java.util.HashMap<java.lang.String,java.util.List<java.lang.Integer>>"

// Array
appendFullName(sb) for String[].class
→ "java.lang.String[]"
```

**Key Features**:
- ✅ Includes full package name
- ✅ Includes type parameters (recursively)
- ✅ Uses `$` for inner classes
- ✅ Uses `[]` for arrays
- ✅ Efficient (StringBuilder, no allocations)
- ✅ Recursive for nested generics

---

### 11. `ClassInfo.appendShortName(StringBuilder)` ⭐ CORE METHOD
**What it does**: Appends short name with type parameters to a StringBuilder (no package)
**Called by**: `getShortName()`
**Recursive**: Yes - calls itself for nested type parameters

**Implementation Logic**:
```java
public StringBuilder appendShortName(StringBuilder sb) {
    Class<?> ct = getComponentType().inner();
    int dim = getDimensions();
    
    // Append base class name (handling member/local classes)
    if (ct != null) {
        if (ct.isLocalClass()) {
            // Local class: include enclosing class
            sb.append(ClassInfo.of(ct.getEnclosingClass()).getSimpleName())
              .append('$')
              .append(ct.getSimpleName());
        } else if (ct.isMemberClass()) {
            // Member class: include declaring class
            sb.append(ClassInfo.of(ct.getDeclaringClass()).getSimpleName())
              .append('$')
              .append(ct.getSimpleName());
        } else {
            // Regular class: just simple name
            sb.append(ct.getSimpleName());
        }
    } else {
        sb.append(t.getTypeName());
    }
    
    // Append type parameters recursively
    if (isParameterizedType) {
        ParameterizedType pt = (ParameterizedType)t;
        sb.append('<');
        boolean first = true;
        for (Type t2 : pt.getActualTypeArguments()) {
            if (!first) sb.append(',');
            first = false;
            ClassInfo.of(t2).appendShortName(sb);  // RECURSIVE!
        }
        sb.append('>');
    }
    
    // Append array brackets
    for (int i = 0; i < dim; i++)
        sb.append("[]");
    
    return sb;
}
```

**Example Outputs**:
```java
// Simple class
appendShortName(sb) for String.class
→ "String"

// Parameterized type
appendShortName(sb) for HashMap<String, List<Integer>>
→ "HashMap<String,List<Integer>>"

// Member class
appendShortName(sb) for Map.Entry.class
→ "Map$Entry"

// Array
appendShortName(sb) for String[].class
→ "String[]"
```

**Key Features**:
- ❌ No package name
- ✅ Includes type parameters (recursively)
- ✅ Uses `$` for inner classes
- ✅ Handles member/local classes specially
- ✅ Uses `[]` for arrays
- ✅ Efficient (StringBuilder, no allocations)
- ✅ Recursive for nested generics

---

## Detailed Analysis

### 1. `ClassUtils.className(Object)` / `Utils.cn(Object)`
**What it does**: Returns the fully-qualified JVM class name
```java
className(String.class)         // "java.lang.String"
className(new HashMap<>())      // "java.util.HashMap"
className(Map.Entry.class)      // "java.util.Map$Entry"
className(String[].class)       // "[Ljava.lang.String;"
className(int[].class)          // "[I"
```
**Implementation**: `value.getClass().getName()` or `((Class<?>)value).getName()`

---

### 2. `ClassUtils.simpleClassName(Object)` / `Utils.scn(Object)`
**What it does**: Returns only the simple class name (innermost name only)
```java
simpleClassName(String.class)   // "String"
simpleClassName(Map.Entry.class)// "Entry"  (NOT "Map.Entry")
simpleClassName(String[].class) // "String[]"
simpleClassName(int[].class)    // "int[]"
```
**Implementation**: `value.getClass().getSimpleName()` or `((Class<?>)value).getSimpleName()`

---

### 3. `ClassUtils.simpleQualifiedClassName(Object)` / `Utils.sqcn(Object)`
**What it does**: Returns simple name with outer classes, but no package, using dot separator
```java
simpleQualifiedClassName(String.class)      // "String"
simpleQualifiedClassName(Map.Entry.class)   // "Map.Entry"  ($ replaced with .)
simpleQualifiedClassName(String[].class)    // "String[]"
```
**Implementation**: Custom logic:
- Gets class name
- Removes package prefix (everything before last `.`)
- Replaces `$` with `.`
- Recursively handles arrays with `[]`

---

### 4. `ClassInfo.getName()`
**What it does**: Delegates to `Class.getName()` or `Type.getTypeName()`
```java
ClassInfo.of(String.class).getName()        // "java.lang.String"
ClassInfo.of(Map.Entry.class).getName()     // "java.util.Map$Entry"
```
**Implementation**: `c.getName()` or `t.getTypeName()`
**Equivalent to**: `ClassUtils.className()`

---

### 5. `ClassInfo.getCanonicalName()`
**What it does**: Delegates to `Class.getCanonicalName()` - source code representation
```java
ClassInfo.of(String.class).getCanonicalName()       // "java.lang.String"
ClassInfo.of(Map.Entry.class).getCanonicalName()    // "java.util.Map.Entry"  (. not $)
ClassInfo.of(String[].class).getCanonicalName()     // "java.lang.String[]"   ([] not [L...;)
ClassInfo.of(localClass).getCanonicalName()         // null (no canonical name)
```
**Implementation**: `c.getCanonicalName()`
**Key difference**: Uses `.` instead of `$`, and `[]` instead of JVM array notation
**Can return null** for local/anonymous classes

---

### 6. `ClassInfo.getSimpleName()`
**What it does**: Delegates to `Class.getSimpleName()` or `Type.getTypeName()`
```java
ClassInfo.of(String.class).getSimpleName()          // "String"
ClassInfo.of(Map.Entry.class).getSimpleName()       // "Entry"
```
**Implementation**: `c.getSimpleName()` or `t.getTypeName()`
**Equivalent to**: `ClassUtils.simpleClassName()`

---

### 7. `ClassInfo.getFullName()` ⭐ UNIQUE - Includes Type Parameters!
**What it does**: Full package name WITH generic type parameters
```java
ClassInfo.of(String.class).getFullName()                    // "java.lang.String"
ClassInfo.of(HashMap.class).getFullName()                   // "java.util.HashMap"

// Type variable example:
Type t = new TypeToken<HashMap<String,Integer>>(){}.getType();
ClassInfo.of(t).getFullName()  // "java.util.HashMap<java.lang.String,java.lang.Integer>"

// Arrays:
ClassInfo.of(String[].class).getFullName()                  // "java.lang.String[]"
```
**Implementation**: Custom recursive logic using `appendFullName()`
- For non-parameterized, non-array: uses `c.getName()`
- For parameterized types: builds `ClassName<Type1,Type2>` recursively
- For arrays: appends `[]` for each dimension

**Key feature**: **ONLY method that includes generic type parameters!**

---

### 8. `ClassInfo.getShortName()` ⭐ UNIQUE - Includes Type Parameters + Outer Classes!
**What it does**: No package, includes outer classes, WITH generic type parameters
```java
ClassInfo.of(String.class).getShortName()                   // "String"
ClassInfo.of(Map.Entry.class).getShortName()                // "Map$Entry" (uses $)

// Type variable example:
Type t = new TypeToken<HashMap<String,Integer>>(){}.getType();
ClassInfo.of(t).getShortName()  // "HashMap<String,Integer>"

// Member/local class handling:
ClassInfo.of(Outer.Inner.class).getShortName()              // "Outer$Inner"
```
**Implementation**: Custom recursive logic using `appendShortName()`
- For simple types: uses `c.getSimpleName()`
- For member classes: includes outer class simple name with `$` separator
- For local classes: includes enclosing class simple name with `$` separator
- For parameterized types: builds `ClassName<Type1,Type2>` recursively
- For arrays: appends `[]` for each dimension

**Key features**: 
- **Includes type parameters**
- **Includes outer classes** (with `$` separator)
- **Handles local/member classes specially**

---

### 9. `ClassInfo.getReadableName()` ⭐ UNIQUE - Array-friendly!
**What it does**: Simple name with "Array" instead of `[]` for arrays
```java
ClassInfo.of(String.class).getReadableName()                // "String"
ClassInfo.of(String[].class).getReadableName()              // "StringArray"
ClassInfo.of(String[][].class).getReadableName()            // "StringArrayArray"
ClassInfo.of(int[].class).getReadableName()                 // "intArray"
```
**Implementation**: Custom logic
- For non-arrays: uses `c.getSimpleName()`
- For arrays: counts dimensions and appends "Array" for each

**Use case**: Human-readable variable/method names

---

## Comparison Matrix

| Feature | className | simpleClassName | simpleQualifiedClassName | getName | getCanonicalName | getSimpleName | getFullName | getShortName | getReadableName | appendFullName | appendShortName |
|---------|-----------|-----------------|--------------------------|---------|------------------|---------------|-------------|--------------|-----------------|----------------|-----------------|
| **Package** | ✅ | ❌ | ❌ | ✅ | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ❌ |
| **Outer Classes** | ✅ $ | ❌ | ✅ . | ✅ $ | ✅ . | ❌ | ✅ $ | ✅ $ | ❌ | ✅ $ | ✅ $ |
| **Type Parameters** | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ | ❌ | ✅ | ✅ |
| **Array Format** | JVM | [] | [] | JVM | [] | [] | [] | [] | "Array" | [] | [] |
| **Separator** | $ | N/A | . | $ | . | N/A | $ | $ | N/A | $ | $ |
| **Returns** | String | String | String | String | String | String | String | String | String | StringBuilder | StringBuilder |
| **Recursive** | ❌ | ❌ | ✅ (array) | ❌ | ❌ | ❌ | ✅ | ✅ | ❌ | ✅ | ✅ |
| **Source** | ClassUtils | ClassUtils | ClassUtils | ClassInfo | ClassInfo | ClassInfo | ClassInfo | ClassInfo | ClassInfo | ClassInfo | ClassInfo |

---

## Consolidation Strategy

### Proposed Unified Method Signatures

**String-returning version** (for convenience):
```java
/**
 * Returns a formatted class name with configurable options.
 *
 * @param includePackage Include package name
 * @param includeOuterClasses Include outer/enclosing class names
 * @param includeTypeParams Include generic type parameters (only for ClassInfo)
 * @param separator Separator for outer classes: '$' or '.'
 * @param arrayFormat Array format: "jvm" = "[L...;", "brackets" = "[]", "word" = "Array"
 * @return The formatted class name
 */
public String getFormattedName(
    boolean includePackage,
    boolean includeOuterClasses, 
    boolean includeTypeParams,
    char separator,
    ArrayFormat arrayFormat
)
```

**StringBuilder-appending version** (for efficiency):
```java
/**
 * Appends a formatted class name to a StringBuilder with configurable options.
 *
 * <p>
 * This is the core implementation used by all other name methods. Using this method
 * directly avoids String allocations when building complex strings.
 *
 * @param sb The StringBuilder to append to
 * @param includePackage Include package name
 * @param includeOuterClasses Include outer/enclosing class names
 * @param includeTypeParams Include generic type parameters (recursively)
 * @param separator Separator for outer classes: '$' or '.'
 * @param arrayFormat Array format: JVM, BRACKETS, or WORD
 * @return The same StringBuilder for chaining
 */
public StringBuilder appendFormattedName(
    StringBuilder sb,
    boolean includePackage,
    boolean includeOuterClasses,
    boolean includeTypeParams,
    char separator,
    ArrayFormat arrayFormat
)
```

**Relationship**: `getFormattedName()` creates a StringBuilder and calls `appendFormattedName()`

### Mapping Current Methods to Proposed Method

| Current Method | includePackage | includeOuterClasses | includeTypeParams | separator | arrayFormat | Returns |
|----------------|----------------|---------------------|-------------------|-----------|-------------|---------|
| `className()` / `getName()` | true | true | false | '$' | JVM | String |
| `getCanonicalName()` | true | true | false | '.' | BRACKETS | String |
| `simpleClassName()` / `getSimpleName()` | false | false | false | N/A | BRACKETS | String |
| `simpleQualifiedClassName()` | false | true | false | '.' | BRACKETS | String |
| `getFullName()` | true | true | **true** | '$' | BRACKETS | String |
| `appendFullName()` | true | true | **true** | '$' | BRACKETS | **StringBuilder** |
| `getShortName()` | false | true | **true** | '$' | BRACKETS | String |
| `appendShortName()` | false | true | **true** | '$' | BRACKETS | **StringBuilder** |
| `getReadableName()` | false | false | false | N/A | WORD | String |

### Enum for Array Format
```java
public enum ArrayFormat {
    JVM,        // "[Ljava.lang.String;" or "[I"
    BRACKETS,   // "String[]" or "int[]"
    WORD        // "StringArray" or "intArray"
}
```

---

## Key Observations

### Redundancies
1. **`ClassUtils.className()` ≈ `ClassInfo.getName()`** - Exact duplicates
2. **`ClassUtils.simpleClassName()` ≈ `ClassInfo.getSimpleName()`** - Exact duplicates
3. **`ClassUtils` methods only work with `Object`** - `ClassInfo` works with `Type` too
4. **`getFullName()` ≈ `appendFullName()`** - Same logic, different return types
5. **`getShortName()` ≈ `appendShortName()`** - Same logic, different return types

### Unique Features
1. **`getFullName()` / `appendFullName()`** - ONLY methods that include type parameters with full package names
2. **`getShortName()` / `appendShortName()`** - ONLY methods that include type parameters WITHOUT package names
3. **`appendFullName()` / `appendShortName()`** - ONLY methods that are recursive (for nested generics) and zero-allocation
4. **`getReadableName()`** - ONLY method with "Array" format
5. **`getCanonicalName()`** - ONLY method using `.` separator + `[]` arrays + can return null
6. **`simpleQualifiedClassName()`** - ONLY ClassUtils method with outer classes (`.` separator)

### Performance Considerations
1. **`append*()` methods are more efficient** - No new String allocations, suitable for building complex strings
2. **`get*()` methods delegate to `append*()`** - Creating a new StringBuilder each time
3. **`append*()` methods are recursive** - Handle nested generic types efficiently

### Type Support
- **`ClassUtils` methods**: Only work with `Object` (gets class at runtime)
- **`ClassInfo` methods**: Work with both `Class<?>` and `Type` (supports generic type variables)

---

## Recommendations

### Option 1: Conservative - Keep Existing, Add Unified Method
- Keep all existing methods for backward compatibility
- Add new unified method `getFormattedName()` to `ClassInfo`
- Mark old methods as "convenience methods" that delegate to unified method
- Gradually migrate internal code to use unified method

### Option 2: Aggressive - Consolidate and Deprecate
- Add unified method to `ClassInfo`
- Deprecate redundant `ClassUtils` methods
- Update all internal usage to `ClassInfo` methods
- Remove `ClassUtils` methods in next major version

### Option 3: Hybrid - Clarify Roles
- **`ClassUtils`**: Simple utility methods for runtime objects (keep as-is)
- **`ClassInfo`**: Advanced formatting with type parameter support
- Add unified method to `ClassInfo` only
- Keep `ClassUtils` for simple, common cases

---

## Suggested Implementation Order

1. **Phase 1**: Add unified `getFormattedName()` method to `ClassInfo`
2. **Phase 2**: Refactor existing `ClassInfo` methods to use unified method
3. **Phase 3**: Update internal callers to use more appropriate methods
4. **Phase 4**: Consider deprecating `ClassUtils` redundant methods (major version)

---

## Example Unified Implementation Sketch

```java
public String getFormattedName(
    boolean includePackage,
    boolean includeOuterClasses,
    boolean includeTypeParams,
    char separator,
    ArrayFormat arrayFormat
) {
    StringBuilder sb = new StringBuilder();
    appendFormattedName(sb, includePackage, includeOuterClasses, includeTypeParams, separator, arrayFormat);
    return sb.toString();
}

private void appendFormattedName(
    StringBuilder sb,
    boolean includePackage,
    boolean includeOuterClasses,
    boolean includeTypeParams,
    char separator,
    ArrayFormat arrayFormat
) {
    // Handle arrays
    ClassInfo ct = getComponentType();
    int dim = getDimensions();
    
    if (dim > 0 && arrayFormat == ArrayFormat.WORD) {
        // "StringArray" format
        ct.appendFormattedName(sb, false, false, includeTypeParams, separator, arrayFormat);
        for (int i = 0; i < dim; i++)
            sb.append("Array");
        return;
    }
    
    // Get the base class name
    if (includePackage) {
        // Full package name
        if (includeOuterClasses) {
            // Full name with package and outer classes
            sb.append(c != null ? c.getName() : t.getTypeName());
            if (separator == '.') {
                // Replace $ with .
                int len = sb.length();
                for (int i = 0; i < len; i++) {
                    if (sb.charAt(i) == '$')
                        sb.setCharAt(i, '.');
                }
            }
        } else {
            // Package + simple name only
            sb.append(c != null ? c.getName() : t.getTypeName());
            // Remove outer class parts
            int lastSeparator = sb.lastIndexOf("$");
            if (lastSeparator != -1) {
                sb.delete(sb.lastIndexOf(".") + 1, lastSeparator + 1);
            }
        }
    } else {
        // No package
        if (includeOuterClasses) {
            // Outer classes + simple name
            if (ct.inner().isMemberClass() || ct.inner().isLocalClass()) {
                // Handle member/local classes specially
                appendShortNameForMemberClass(sb, separator);
            } else {
                sb.append(c != null ? c.getSimpleName() : t.getTypeName());
            }
        } else {
            // Simple name only
            sb.append(c != null ? c.getSimpleName() : t.getTypeName());
        }
    }
    
    // Add type parameters if requested
    if (includeTypeParams && isParameterizedType) {
        ParameterizedType pt = (ParameterizedType)t;
        sb.append('<');
        boolean first = true;
        for (Type t2 : pt.getActualTypeArguments()) {
            if (!first) sb.append(',');
            first = false;
            ClassInfo.of(t2).appendFormattedName(sb, includePackage, includeOuterClasses, includeTypeParams, separator, arrayFormat);
        }
        sb.append('>');
    }
    
    // Add array brackets
    if (dim > 0) {
        if (arrayFormat == ArrayFormat.JVM && dim == getDimensions()) {
            // JVM format: "[Ljava.lang.String;" - already in getName()
            // This is tricky, might need special handling
        } else {
            // "[]" format
            for (int i = 0; i < dim; i++)
                sb.append("[]");
        }
    }
}
```

---

## Questions for Discussion

1. Should we consolidate or keep methods separate for different use cases?
2. Is the unified method signature too complex?
3. Should `ClassUtils` methods stay for simple runtime use cases?
4. Can we deprecate any methods without breaking too much code?
5. Should type parameter support be ClassInfo-only feature?

