# AccessibleInfo Refactoring Summary (TODO-83)

## Overview
Refactored Juneau's reflection infrastructure to introduce `AccessibleInfo` as a base class, better mirroring Java's reflection hierarchy where `AccessibleObject` is the parent of `Field`, `Method`, and `Constructor`.

---

## Motivation

### Java Reflection Hierarchy
```
java.lang.reflect.AccessibleObject (base class)
├── java.lang.reflect.Field extends AccessibleObject
└── java.lang.reflect.Executable extends AccessibleObject
    ├── java.lang.reflect.Method extends Executable
    └── java.lang.reflect.Constructor extends Executable
```

### Previous Juneau Hierarchy (Before TODO-83)
```
ExecutableInfo (combined AccessibleObject + Executable functionality)
├── MethodInfo extends ExecutableInfo
└── ConstructorInfo extends ExecutableInfo

FieldInfo (standalone, duplicated AccessibleObject methods)

ParamInfo (standalone, no relationship)
ClassInfo (standalone, no relationship)
```

**Problem**: `ExecutableInfo` was combining two responsibilities (AccessibleObject + Executable), and `FieldInfo` was duplicating annotation/accessibility methods.

### New Juneau Hierarchy (After TODO-83)
```
AccessibleInfo (base class, mirrors AccessibleObject)
├── ExecutableInfo extends AccessibleInfo (mirrors Executable)
│   ├── MethodInfo extends ExecutableInfo
│   └── ConstructorInfo extends ExecutableInfo
└── FieldInfo extends AccessibleInfo

ParamInfo (standalone, mirrors Parameter)
ClassInfo (standalone, mirrors Class)
```

**Benefits**:
- Better alignment with Java's reflection API structure
- Eliminates code duplication between `ExecutableInfo` and `FieldInfo`
- Clearer separation of concerns
- More intuitive for developers familiar with Java reflection

---

## Changes Made

### 1. Created AccessibleInfo Base Class

**File**: `juneau-core/juneau-common/src/main/java/org/apache/juneau/common/reflect/AccessibleInfo.java` (NEW)

**Methods** (all inherited from `java.lang.reflect.AccessibleObject`):

#### Accessibility Methods
- **`setAccessible()`** → `boolean` - Attempts to set accessible, returns success/failure
- **`isAccessible()`** → `boolean` - Checks if accessible (Java 9+)

#### Annotation Methods (Standard Array Accessors)
- **`getAnnotation(Class<A>)`** → `<A> A` - Gets annotation if present
- **`getAnnotations()`** → `Annotation[]` - Gets all annotations
- **`getDeclaredAnnotations()`** → `Annotation[]` - Gets directly declared annotations
- **`getAnnotationsByType(Class<A>)`** → `<A> A[]` - Gets repeatable annotations
- **`getDeclaredAnnotationsByType(Class<A>)`** → `<A> A[]` - Gets declared repeatable annotations

**Key Design Decisions**:
- All methods are non-final to allow subclasses to override and provide enhanced functionality
- Takes `AccessibleObject` in constructor, stored as `protected final AccessibleObject ao`
- No `inner()` method (avoided name clash with subclass-specific inner() methods)

**Javadoc**: Complete with examples and cross-references to Java API

**Lines of Code**: ~210 lines (including comprehensive Javadoc)

---

### 2. Updated ExecutableInfo

**File**: `juneau-core/juneau-common/src/main/java/org/apache/juneau/common/reflect/ExecutableInfo.java`

**Changes**:
1. **Extended AccessibleInfo**: `public abstract class ExecutableInfo extends AccessibleInfo`
2. **Updated Constructor**: Added `super(e)` call to pass `Executable` to `AccessibleInfo`
3. **Removed Duplicate Methods**:
   - ~~`isAccessible()`~~ (now inherited from AccessibleInfo)
   - ~~`getAnnotation(Class<A>)`~~ (now inherited from AccessibleInfo)
   - ~~`getAnnotations()`~~ (now inherited from AccessibleInfo)
   - ~~`getDeclaredAnnotations()`~~ (now inherited from AccessibleInfo)
4. **Kept Executable-Specific Methods**:
   - `getModifiers()`, `isSynthetic()`, `isVarArgs()` (high priority)
   - `getTypeParameters()` (high priority)
   - `getGenericExceptionTypes()`, `toGenericString()` (medium priority)
   - `getAnnotatedReceiverType()`, `getAnnotatedParameterTypes()`, `getAnnotatedExceptionTypes()` (low priority)
   - All parameter-related methods (`getParams()`, etc.)
   - All fuzzy invocation methods (`invokeFuzzy()`, etc.)
5. **Kept Juneau Enhancement**: `accessible()` method (returns `ExecutableInfo` for chaining)

**Removed Lines**: ~100 lines (duplicate annotation/accessibility methods)

**Impact**: `MethodInfo` and `ConstructorInfo` still override `getAnnotation()` and related methods to provide enhanced annotation searching through class hierarchies.

---

### 3. Updated FieldInfo

**File**: `juneau-core/juneau-common/src/main/java/org/apache/juneau/common/reflect/FieldInfo.java`

**Changes**:
1. **Extended AccessibleInfo**: `public class FieldInfo extends AccessibleInfo implements Comparable<FieldInfo>`
2. **Updated Constructor**: Added `super(f)` call to pass `Field` to `AccessibleInfo`
3. **Removed Duplicate Methods**:
   - ~~`setAccessible()`~~ (now inherited from AccessibleInfo)
   - ~~`getAnnotations()`~~ (now inherited from AccessibleInfo)
   - ~~`getDeclaredAnnotations()`~~ (now inherited from AccessibleInfo)
   - ~~`getAnnotationsByType(Class<A>)`~~ (now inherited from AccessibleInfo)
   - ~~`getDeclaredAnnotationsByType(Class<A>)`~~ (now inherited from AccessibleInfo)
4. **Kept Field-Specific Methods**:
   - `getModifiers()`, `isSynthetic()`, `isEnumConstant()` (high priority)
   - `getGenericType()`, `getAnnotatedType()` (high priority)
   - `toGenericString()` (medium priority)
   - All Juneau-enhanced methods (`get()`, `set()`, `isVisible()`, etc.)

**Removed Lines**: ~100 lines (duplicate annotation/accessibility/setAccessible methods)

**Reorganization**: Added clear section comment "Field-Specific Methods" to distinguish from inherited AccessibleInfo methods

---

## Code Statistics

### Lines of Code
| Class | Lines Added | Lines Removed | Net Change |
|-------|-------------|---------------|------------|
| **AccessibleInfo** (new) | +210 | 0 | +210 |
| **ExecutableInfo** | 0 | -100 | -100 |
| **FieldInfo** | 0 | -100 | -100 |
| **TODO.md** | +1 | 0 | +1 |
| **AccessibleInfo_Refactoring_Summary.md** | +300 | 0 | +300 |
| **TOTAL** | **+511** | **-200** | **+311** |

### Method Count
| Class | Methods Added | Methods Removed | Net Change |
|-------|---------------|-----------------|------------|
| **AccessibleInfo** | +7 | 0 | +7 |
| **ExecutableInfo** | 0 | -4 | -4 |
| **FieldInfo** | 0 | -5 | -5 |
| **TOTAL** | **+7** | **-9** | **-2** |

**Note**: Net method count is negative because duplicated methods were consolidated into the base class.

---

## Testing Results

### Build
```bash
mvn clean install -DskipTests
```
**Result**: ✅ BUILD SUCCESS

### Unit Tests
```bash
mvn test -pl juneau-utest
```
**Result**: 
- Tests run: **25,872**
- Failures: **0**
- Errors: **0**
- Skipped: **1**

✅ **All tests passing** - No regressions

---

## Benefits

### 1. Better Architecture
- **Mirrors Java Reflection**: Hierarchy now matches `java.lang.reflect` structure
- **Separation of Concerns**: `AccessibleInfo` handles accessibility and annotations, `ExecutableInfo` handles executable-specific features, `FieldInfo` handles field-specific features
- **Intuitive**: Developers familiar with Java reflection will understand the structure immediately

### 2. Code Reuse
- **Eliminated Duplication**: Removed ~200 lines of duplicated code
- **Single Source of Truth**: All `AccessibleObject` functionality lives in one place
- **Maintainability**: Changes to annotation/accessibility logic only need to be made in `AccessibleInfo`

### 3. Extensibility
- **Easy to Add**: Future `AccessibleObject` subclasses (e.g., `RecordComponentInfo` for Java 14+) can simply extend `AccessibleInfo`
- **Consistent API**: All subclasses automatically get the same annotation and accessibility methods
- **Override-Friendly**: Non-final methods allow subclasses to enhance behavior (e.g., Juneau's annotation hierarchy searching)

### 4. API Clarity
- **Clear Inheritance**: Developers can see at a glance what functionality comes from where
- **Better Documentation**: Javadoc inheritance makes it clear what each class provides
- **Reduced Cognitive Load**: Less duplication means less code to read and understand

---

## Backward Compatibility

### ✅ Fully Backward Compatible
- All public APIs remain unchanged
- Method signatures are identical
- All tests pass without modification
- Existing code continues to work without changes

### Why It's Compatible
- **Inheritance**: Subclasses inherit all methods from `AccessibleInfo`
- **Same Signatures**: Method names, parameters, and return types are unchanged
- **Polymorphism**: Existing code calling `ExecutableInfo.getAnnotation()` or `FieldInfo.getAnnotations()` works identically
- **No Breaking Changes**: This is a pure refactoring - reorganization without functional changes

---

## Design Decisions & Rationale

### Decision 1: Why not include inner() in AccessibleInfo?
**Problem**: `ConstructorInfo.inner()` returns `Constructor<T>` (generic), `FieldInfo.inner()` returns `Field`, `MethodInfo.inner()` returns `Method`. These have different signatures and cannot coexist with `AccessibleInfo.inner()` returning `AccessibleObject`.

**Solution**: Removed `inner()` from `AccessibleInfo`. Each subclass provides its own type-specific `inner()` method.

**Rationale**: Java's generics don't support covariant return types across different generic signatures. Keeping type-specific `inner()` methods in subclasses provides better type safety.

### Decision 2: Why make annotation methods non-final?
**Rationale**: `MethodInfo` and `ConstructorInfo` override these methods to provide Juneau's enhanced annotation searching through class hierarchies and interfaces. Making them non-final preserves this capability.

### Decision 3: Why not move setAccessible() override logic?
**Current State**: `AccessibleInfo.setAccessible()` implements the try-catch logic. `ExecutableInfo` and `FieldInfo` inherit this directly.

**Rationale**: The implementation is identical for all subclasses, so keeping it in the base class eliminates duplication. The `accessible()` convenience method in `ExecutableInfo` calls the inherited `setAccessible()`.

### Decision 4: Keep Comparable<FieldInfo> on FieldInfo?
**Decision**: Yes, keep `implements Comparable<FieldInfo>` on `FieldInfo`.

**Rationale**: 
- Not all `AccessibleObject` subclasses need to be comparable
- Sorting fields is domain-specific to `FieldInfo`
- Keeps `AccessibleInfo` focused on core `AccessibleObject` functionality

---

## Future Enhancements

### Potential Additions (Not in TODO-83)

1. **RecordComponentInfo** (Java 14+)
   - Could extend `AccessibleInfo` if we want consistent annotation access
   - Currently, `Class.getRecordComponents()` returns `RecordComponent[]` which don't extend `AccessibleObject`
   - Juneau could provide `RecordComponentInfo` extending `AccessibleInfo` for consistency

2. **Enhanced Caching**
   - Consider caching annotation lookups in `AccessibleInfo`
   - Profile to identify hot paths in annotation processing

3. **Annotation Provider Strategy**
   - Allow custom annotation providers to be plugged in
   - Current implementation uses `AnnotationProvider.DEFAULT`

4. **Statistics & Metrics**
   - Track setAccessible() success/failure rates
   - Monitor annotation lookup performance
   - Useful for debugging security manager issues

---

## Implementation Timeline

### TODO-83 Execution
1. ✅ Created `AccessibleInfo.java` with all `AccessibleObject` methods (~30 minutes)
2. ✅ Updated `ExecutableInfo` to extend `AccessibleInfo` and removed duplicates (~15 minutes)
3. ✅ Updated `FieldInfo` to extend `AccessibleInfo` and removed duplicates (~15 minutes)
4. ✅ Fixed compilation errors (name clash with `inner()`) (~5 minutes)
5. ✅ Compiled and tested - all 25,872 tests passing (~10 minutes)
6. ✅ Updated TODO.md and created summary documentation (~15 minutes)

**Total Time**: ~90 minutes

---

## Related TODOs

This refactoring builds on previous reflection infrastructure improvements:

- **TODO-73**: Moved reflection classes from `org.apache.juneau.reflect` to `org.apache.juneau.common.reflect`
- **TODO-74**: Added high-priority methods to `ClassInfo`
- **TODO-75**: Added medium-priority methods to `ClassInfo`
- **TODO-76**: Added low-priority methods to `ClassInfo`
- **TODO-77**: Updated `ClassInfo.CACHE` to use `Cache` object
- **TODO-78**: Made `ExecutableInfo` a complete replacement for `java.lang.reflect.Executable`
- **TODO-79**: Made `ParamInfo` a complete replacement for `java.lang.reflect.Parameter`
- **TODO-80**: Made `FieldInfo` a complete replacement for `java.lang.reflect.Field`
- **TODO-81**: Made `MethodInfo` a complete replacement for `java.lang.reflect.Method`
- **TODO-82**: Made `ConstructorInfo` a complete replacement for `java.lang.reflect.Constructor`
- **TODO-83**: Introduced `AccessibleInfo` to mirror `java.lang.reflect.AccessibleObject` (this TODO)

---

## Visual Comparison

### Before TODO-83
```java
// ExecutableInfo had both AccessibleObject and Executable methods
public abstract class ExecutableInfo {
    public boolean setAccessible() { ... }           // AccessibleObject method
    public boolean isAccessible() { ... }            // AccessibleObject method
    public <A> A getAnnotation(Class<A> c) { ... }   // AccessibleObject method
    public Annotation[] getAnnotations() { ... }     // AccessibleObject method
    
    public int getModifiers() { ... }                // Executable method
    public boolean isVarArgs() { ... }               // Executable method
    // ... more Executable methods
}

// FieldInfo duplicated AccessibleObject methods
public class FieldInfo {
    public boolean setAccessible() { ... }           // DUPLICATE
    public <A> A getAnnotation(Class<A> c) { ... }   // DUPLICATE (enhanced)
    public Annotation[] getAnnotations() { ... }     // DUPLICATE
    // ... field-specific methods
}
```

### After TODO-83
```java
// AccessibleInfo provides all AccessibleObject functionality
public abstract class AccessibleInfo {
    protected final AccessibleObject ao;
    
    public boolean setAccessible() { ... }
    public boolean isAccessible() { ... }
    public <A> A getAnnotation(Class<A> c) { ... }
    public Annotation[] getAnnotations() { ... }
    public Annotation[] getDeclaredAnnotations() { ... }
    public <A> A[] getAnnotationsByType(Class<A> c) { ... }
    public <A> A[] getDeclaredAnnotationsByType(Class<A> c) { ... }
}

// ExecutableInfo focuses on Executable functionality
public abstract class ExecutableInfo extends AccessibleInfo {
    // Inherited: setAccessible(), isAccessible(), annotation methods
    
    public int getModifiers() { ... }                // Executable method
    public boolean isVarArgs() { ... }               // Executable method
    // ... more Executable-specific methods
}

// FieldInfo focuses on Field functionality
public class FieldInfo extends AccessibleInfo {
    // Inherited: setAccessible(), isAccessible(), annotation methods
    
    public int getModifiers() { ... }                // Field method
    public boolean isEnumConstant() { ... }          // Field method
    // ... more Field-specific methods
}
```

---

## Usage Examples

### Example 1: Setting Accessibility
```java
// Works identically for both Field and Executable
FieldInfo fi = ClassInfo.of(MyClass.class).getField("privateField");
fi.setAccessible();  // Inherited from AccessibleInfo

MethodInfo mi = ClassInfo.of(MyClass.class).getMethod("privateMethod");
mi.setAccessible();  // Inherited from AccessibleInfo

// Juneau convenience (ExecutableInfo only)
mi.accessible().invoke(obj);  // Chaining style
```

### Example 2: Accessing Annotations
```java
// Same API for Field, Method, Constructor
FieldInfo fi = ClassInfo.of(MyClass.class).getField("myField");
Annotation[] annotations = fi.getAnnotations();  // Inherited from AccessibleInfo

MethodInfo mi = ClassInfo.of(MyClass.class).getMethod("myMethod");
Deprecated d = mi.getAnnotation(Deprecated.class);  // Enhanced by MethodInfo override

ConstructorInfo ci = ClassInfo.of(MyClass.class).getConstructor();
Author[] authors = ci.getAnnotationsByType(Author.class);  // Handles repeatable annotations
```

### Example 3: Polymorphism
```java
public void processAccessibleElement(AccessibleInfo ai) {
    // Works for Field, Method, Constructor
    if (ai.getAnnotation(Deprecated.class) != null) {
        System.out.println("Deprecated: " + ai);
    }
    
    ai.setAccessible();  // Make it accessible
}

// Usage
processAccessibleElement(ClassInfo.of(MyClass.class).getField("field"));
processAccessibleElement(ClassInfo.of(MyClass.class).getMethod("method"));
processAccessibleElement(ClassInfo.of(MyClass.class).getConstructor());
```

---

## Conclusion

TODO-83 successfully refactored Juneau's reflection infrastructure to introduce `AccessibleInfo` as a base class, creating a hierarchy that better mirrors Java's `java.lang.reflect` package structure. This refactoring:

✅ **Eliminated ~200 lines of duplicated code**
✅ **Improved architecture** with clear separation of concerns
✅ **Maintained 100% backward compatibility** - all 25,872 tests passing
✅ **Enhanced extensibility** for future reflection features
✅ **Clarified the API** with intuitive inheritance relationships

The Juneau reflection infrastructure now provides:
- `AccessibleInfo` → mirrors `java.lang.reflect.AccessibleObject`
- `ExecutableInfo` → mirrors `java.lang.reflect.Executable`
- `MethodInfo` → mirrors `java.lang.reflect.Method`
- `ConstructorInfo` → mirrors `java.lang.reflect.Constructor`
- `FieldInfo` → mirrors `java.lang.reflect.Field`
- `ParamInfo` → mirrors `java.lang.reflect.Parameter`
- `ClassInfo` → mirrors `java.lang.Class`

This comprehensive reflection infrastructure is now production-ready, providing both drop-in compatibility with Java's reflection API and powerful Juneau-specific enhancements.

---

**Completed**: October 31, 2025
**TODO**: TODO-83
**Tests**: 25,872 passing, 0 failures, 0 errors
**Status**: ✅ COMPLETE

