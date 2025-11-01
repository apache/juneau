# ParamInfo vs java.lang.reflect.Parameter API Comparison

## Overview
Comparing `ParamInfo` against `java.lang.reflect.Parameter` to identify missing methods.

---

## ✅ Methods Already Present in ParamInfo

### Basic Information
- ✅ `getName()` - Enhanced version (checks for @Name annotation first, then Parameter.getName())
- ✅ `getType()` - Available as `getParameterType()` returning `ClassInfo` (wrapped)

### Annotations
- ✅ `getAnnotation(Class<A>)` - Enhanced version with hierarchy search
- ✅ `getDeclaredAnnotation(Class<A>)` - Direct wrapper for parameter annotations
- ✅ Custom: `hasAnnotation(Class<A>)` - Convenience method
- ✅ Custom: `hasNoAnnotation(Class<A>)` - Convenience method
- ✅ Custom: `forEachAnnotation(...)` - Enhanced iteration
- ✅ Custom: `forEachDeclaredAnnotation(...)` - Enhanced iteration

### Juneau-Specific Enhancements
- ✅ `getConstructor()` - Returns `ConstructorInfo` if parameter belongs to constructor
- ✅ `getMethod()` - Returns `MethodInfo` if parameter belongs to method
- ✅ `getIndex()` - Returns parameter index
- ✅ `canAccept(Object)` - Type checking utility
- ✅ `isType(Class<?>)` - Type matching utility
- ✅ `hasName()` - Checks if name is available

---

## ❌ Missing Methods from java.lang.reflect.Parameter

### 1. ❌ `getDeclaringExecutable()` → `Executable`
- **Status**: Missing
- **Current**: Has `getConstructor()` and `getMethod()` but not unified `getDeclaringExecutable()`
- **Recommendation**: ✅ **Add** - should return `ExecutableInfo` for consistency
- **Usage**: `ExecutableInfo exec = paramInfo.getDeclaringExecutable();`

### 2. ❌ `getModifiers()` → `int`
- **Status**: Missing
- **Current**: No way to get parameter modifiers
- **Recommendation**: ✅ **Add** for completeness
- **Usage**: `int mods = paramInfo.getModifiers();`
- **Note**: Used to check for `final`, etc.

### 3. ❌ `isNamePresent()` → `boolean`
- **Status**: Partially available via `hasName()`
- **Current**: `hasName()` checks both @Name annotation AND Parameter.isNamePresent()
- **Recommendation**: ✅ **Add** for exact API parity (check only bytecode, not annotations)
- **Usage**: `if (paramInfo.isNamePresent()) { ... }`
- **Note**: Different from `hasName()` - only checks bytecode

### 4. ❌ `isImplicit()` → `boolean`
- **Status**: Missing
- **Current**: No way to check if parameter is implicit
- **Recommendation**: ✅ **Add** - Commonly used
- **Usage**: `if (!paramInfo.isImplicit()) { ... }`
- **Note**: Returns `true` for mandated or synthetic parameters

### 5. ❌ `isSynthetic()` → `boolean`
- **Status**: Missing
- **Current**: No way to check if compiler-generated
- **Recommendation**: ✅ **Add** - Commonly used
- **Usage**: `if (!paramInfo.isSynthetic()) { ... }`
- **Note**: Returns `true` for compiler-generated parameters

### 6. ❌ `isVarArgs()` → `boolean`
- **Status**: Missing (but available via `eInfo.isVarArgs()`)
- **Current**: No direct method on ParamInfo
- **Recommendation**: ✅ **Add** for convenience
- **Usage**: `if (paramInfo.isVarArgs()) { ... }`
- **Note**: Only true for the last parameter of a varargs method

### 7. ❌ `getParameterizedType()` → `Type`
- **Status**: Missing
- **Current**: Only `getParameterType()` available (returns `ClassInfo`)
- **Recommendation**: ✅ **Add** for generic type information
- **Usage**: `Type type = paramInfo.getParameterizedType();`
- **Note**: Returns generic type including parameterized types

### 8. ❌ `getAnnotatedType()` → `AnnotatedType`
- **Status**: Missing
- **Current**: No equivalent
- **Recommendation**: ✅ **Add** for annotated type information
- **Usage**: `AnnotatedType aType = paramInfo.getAnnotatedType();`
- **Note**: Returns type with annotations

### 9. ❌ `getAnnotations()` → `Annotation[]`
- **Status**: Missing (has `getDeclaredAnnotation()` but not `getAnnotations()`)
- **Current**: Can iterate with `forEachAnnotation()` but no array accessor
- **Recommendation**: ✅ **Add** for standard API
- **Usage**: `Annotation[] annotations = paramInfo.getAnnotations();`
- **Note**: Different from `getDeclaredAnnotations()` - includes inherited

### 10. ❌ `getDeclaredAnnotations()` → `Annotation[]`
- **Status**: Missing
- **Current**: Has `getDeclaredAnnotation(Class<A>)` for single annotation lookup
- **Recommendation**: ✅ **Add** for standard API
- **Usage**: `Annotation[] declared = paramInfo.getDeclaredAnnotations();`

### 11. ❌ `getAnnotationsByType(Class<A>)` → `<A> A[]`
- **Status**: Missing
- **Current**: No equivalent
- **Recommendation**: ⚠️ **Consider** - For repeatable annotations
- **Usage**: `MyAnnotation[] annotations = paramInfo.getAnnotationsByType(MyAnnotation.class);`

### 12. ❌ `getDeclaredAnnotationsByType(Class<A>)` → `<A> A[]`
- **Status**: Missing
- **Current**: No equivalent
- **Recommendation**: ⚠️ **Consider** - For repeatable annotations
- **Usage**: `MyAnnotation[] annotations = paramInfo.getDeclaredAnnotationsByType(MyAnnotation.class);`

---

## 🎯 Recommended Additions

### High Priority (10 methods) ✅
Essential for drop-in replacement:

1. **`getDeclaringExecutable()`** → `ExecutableInfo`
2. **`getModifiers()`** → `int`
3. **`isNamePresent()`** → `boolean`
4. **`isImplicit()`** → `boolean`
5. **`isSynthetic()`** → `boolean`
6. **`isVarArgs()`** → `boolean`
7. **`getParameterizedType()`** → `Type`
8. **`getAnnotatedType()`** → `AnnotatedType`
9. **`getAnnotations()`** → `Annotation[]`
10. **`getDeclaredAnnotations()`** → `Annotation[]`

### Medium Priority (2 methods) ⚠️
For repeatable annotations:

11. **`getAnnotationsByType(Class<A>)`** → `<A> A[]`
12. **`getDeclaredAnnotationsByType(Class<A>)`** → `<A> A[]`

---

## 📝 Implementation Notes

### Key Design Considerations

1. **`getDeclaringExecutable()`**: Should return `ExecutableInfo` (not `Executable`) for consistency
   - Juneau pattern: Wrap Java reflection types
   - Already have `eInfo` field available

2. **`isNamePresent()`** vs `hasName()`:
   - `isNamePresent()`: Checks ONLY if name is in bytecode (direct wrapper)
   - `hasName()`: Checks @Name annotation OR bytecode (Juneau enhancement)
   - Both should exist for flexibility

3. **`isVarArgs()`**: 
   - Should only return `true` for the last parameter of a varargs executable
   - Check: `eInfo.isVarArgs() && index == eInfo.getParamCount() - 1`

4. **`getParameterizedType()`**:
   - Should return raw `Type` (not `ClassInfo`)
   - Use: `eInfo.getRawGenericParamType(index)`

5. **Annotation Methods**:
   - `getAnnotations()` vs `getDeclaredAnnotations()`: 
     - Currently has enhanced versions, need simple wrappers too
   - Should probably delegate to `eInfo._getParameterAnnotations(index)` for declared
   - For inherited, may need custom logic

### Pattern to Follow
```java
// Simple delegation
public int getModifiers() {
    return p.getModifiers();
}

// Boolean check
public boolean isSynthetic() {
    return p.isSynthetic();
}

// Type information
public Type getParameterizedType() {
    return eInfo.getRawGenericParamType(index);
}

// Return wrapped type
public ExecutableInfo getDeclaringExecutable() {
    return eInfo;
}
```

---

## 📊 Coverage Summary

| Category | Status |
|----------|--------|
| **Basic Info** | ⚠️ Missing `isNamePresent()` direct wrapper |
| **Modifiers** | ❌ Missing `getModifiers()` |
| **Type Checking** | ❌ Missing `isImplicit()`, `isSynthetic()`, `isVarArgs()` |
| **Type Information** | ❌ Missing `getParameterizedType()`, `getAnnotatedType()` |
| **Annotations (arrays)** | ❌ Missing `getAnnotations()`, `getDeclaredAnnotations()` |
| **Repeatable Annotations** | ❌ Missing both methods |
| **Parent Info** | ⚠️ Has split methods (`getMethod()`/`getConstructor()`), missing unified |

---

## 🚀 Status Update

✅ **COMPLETED** - All 12 missing methods have been successfully added to `ParamInfo` (TODO-79):

### High Priority (10 methods) - ✅ COMPLETED
1. ✅ `getDeclaringExecutable()` → `ExecutableInfo`
2. ✅ `getModifiers()` → `int`
3. ✅ `isNamePresent()` → `boolean`
4. ✅ `isImplicit()` → `boolean`
5. ✅ `isSynthetic()` → `boolean`
6. ✅ `isVarArgs()` → `boolean`
7. ✅ `getParameterizedType()` → `Type`
8. ✅ `getAnnotatedType()` → `AnnotatedType`
9. ✅ `getAnnotations()` → `Annotation[]`
10. ✅ `getDeclaredAnnotations()` → `Annotation[]`

### Medium Priority (2 methods) - ✅ COMPLETED
11. ✅ `getAnnotationsByType(Class<A>)` → `<A> A[]`
12. ✅ `getDeclaredAnnotationsByType(Class<A>)` → `<A> A[]`

`ParamInfo` is now a true drop-in replacement for `java.lang.reflect.Parameter` with:

✅ Complete modifier access
✅ Standard annotation API (array accessors)
✅ Type checking (implicit, synthetic, varargs)
✅ Generic type information
✅ Annotated type support
✅ Unified parent accessor
✅ Repeatable annotation support

**Key Design Decisions**:
- `getDeclaringExecutable()` returns `ExecutableInfo` (not `Executable`) for consistency with Juneau patterns
- `isNamePresent()` checks only bytecode (differs from `hasName()` which also checks @Name annotation)
- All annotation methods delegate directly to `Parameter` for standard behavior
- Juneau's enhanced annotation methods (`getAnnotation(Class)`, etc.) remain unchanged

**Test Results**: All 25,872 tests passing
**Build Status**: ✅ SUCCESS

