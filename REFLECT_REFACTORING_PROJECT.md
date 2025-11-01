# Common Reflect Package Refactoring Project

## Project Goals

1. **Return `*Info` objects**: Methods should return `common.reflect` objects wherever possible instead of raw reflection objects
2. **Unmodifiable lists**: Return unmodifiable lists instead of arrays to prevent external modification
3. **Caching**: Use caching wherever possible (e.g., `getDeclaredAnnotations()` should be calculated once)
4. **Thread-safety with Atomics**: Replace `volatile`/`synchronized` with `Atomic` classes
5. **Dual annotation support**: Support both `Annotation` (for specific access like `@Bean`) and `AnnotationInfo` (for context-aware information)

## Approach

- Start from lowest levels (AccessibleInfo) and work upward
- Work method by method to ensure everything works after each change
- Create/update unit tests as we go
- Check for existing tests in other packages (classes were moved)

## Class Hierarchy

```
AccessibleInfo (base)
├── ExecutableInfo
│   ├── MethodInfo
│   └── ConstructorInfo
└── FieldInfo

Standalone:
├── ClassInfo
├── PackageInfo
├── ParamInfo
├── AnnotationInfo
└── AnnotationList
```

## Refactoring Order

1. ✅ **AccessibleInfo** - Base class (START HERE)
2. **ExecutableInfo** - Extends AccessibleInfo
3. **FieldInfo** - Extends AccessibleInfo
4. **ParamInfo** - Standalone
5. **MethodInfo** - Extends ExecutableInfo
6. **ConstructorInfo** - Extends ExecutableInfo
7. **ClassInfo** - Standalone (most complex)
8. **PackageInfo** - Standalone
9. **AnnotationInfo** - Standalone
10. **AnnotationList** - Standalone

---

## 1. AccessibleInfo

**Status**: 🟡 IN PROGRESS

### Current Methods

| Method | Return Type | Status | Notes |
|--------|-------------|--------|-------|
| `accessible()` | `AccessibleInfo` | ⏸️ TODO | Returns this |
| `setAccessible()` | `boolean` | ⏸️ TODO | Sets accessible, returns success |
| `isAccessible()` | `boolean` | ⏸️ TODO | Java 9+ |
| `getAnnotation(Class<A>)` | `<A> A` | ⏸️ TODO | Returns specific annotation |
| `getAnnotations()` | `Annotation[]` | ⏸️ TODO | **REFACTOR**: Return list + cache |
| `getDeclaredAnnotations()` | `Annotation[]` | ⏸️ TODO | **REFACTOR**: Return list + cache |
| `getAnnotationsByType(Class<A>)` | `<A> A[]` | ⏸️ TODO | **REFACTOR**: Return list |
| `getDeclaredAnnotationsByType(Class<A>)` | `<A> A[]` | ⏸️ TODO | **REFACTOR**: Return list |

### Refactoring Plan for AccessibleInfo

#### Phase 1: Add AnnotationInfo methods (DUAL SUPPORT)
- [ ] Add `getAnnotationInfo(Class<A>)` → Returns `AnnotationInfo<A>` or null
- [ ] Add `getAnnotationInfos()` → Returns `List<AnnotationInfo<?>>` (cached)
- [ ] Add `getDeclaredAnnotationInfos()` → Returns `List<AnnotationInfo<?>>` (cached)
- [ ] Add `getAnnotationInfosByType(Class<A>)` → Returns `List<AnnotationInfo<A>>`
- [ ] Add `getDeclaredAnnotationInfosByType(Class<A>)` → Returns `List<AnnotationInfo<A>>`

#### Phase 2: Update existing annotation methods to return lists
- [ ] Update `getAnnotations()` → Return `List<Annotation>` (cached, unmodifiable)
- [ ] Update `getDeclaredAnnotations()` → Return `List<Annotation>` (cached, unmodifiable)
- [ ] Update `getAnnotationsByType(Class<A>)` → Return `List<A>` (unmodifiable)
- [ ] Update `getDeclaredAnnotationsByType(Class<A>)` → Return `List<A>` (unmodifiable)

#### Phase 3: Implement caching with Atomics
- [ ] Add `AtomicReference<List<Annotation>> annotationsCache`
- [ ] Add `AtomicReference<List<Annotation>> declaredAnnotationsCache`
- [ ] Add `AtomicReference<List<AnnotationInfo<?>>> annotationInfosCache`
- [ ] Add `AtomicReference<List<AnnotationInfo<?>>> declaredAnnotationInfosCache`
- [ ] Implement thread-safe lazy initialization

#### Phase 4: Unit tests
- [ ] Test `getAnnotation()` - specific annotation access
- [ ] Test `getAnnotations()` - returns unmodifiable list
- [ ] Test `getDeclaredAnnotations()` - returns unmodifiable list
- [ ] Test `getAnnotationsByType()` - repeatable annotations
- [ ] Test `getAnnotationInfos()` - AnnotationInfo list
- [ ] Test caching - same instance returned on multiple calls
- [ ] Test thread-safety - concurrent access
- [ ] Test with Field, Method, Constructor

### Current Implementation Notes

```java
public abstract class AccessibleInfo {
    final AccessibleObject ao;
    
    // Current: Direct delegation to AccessibleObject
    // TODO: Add caching, return lists, add AnnotationInfo methods
}
```

### Test File Location
- **Primary**: `juneau-utest/src/test/java/org/apache/juneau/common/reflect/AccessibleInfo_Test.java`
- **Check for existing**: `juneau-utest/src/test/java/org/apache/juneau/reflect/` (old location)

---

## 2. ExecutableInfo

**Status**: ⏸️ PENDING

### Methods to Review
- TBD (will populate when AccessibleInfo is complete)

---

## 3. FieldInfo

**Status**: ⏸️ PENDING

### Methods to Review
- TBD (will populate when AccessibleInfo is complete)

---

## Progress Tracking

| Class | Methods Reviewed | Methods Refactored | Tests Created | Status |
|-------|------------------|-------------------|---------------|--------|
| AccessibleInfo | 0/8 | 0/8 | 0 | 🟡 IN PROGRESS |
| ExecutableInfo | 0/? | 0/? | 0 | ⏸️ PENDING |
| FieldInfo | 0/? | 0/? | 0 | ⏸️ PENDING |
| ParamInfo | 0/? | 0/? | 0 | ⏸️ PENDING |
| MethodInfo | 0/? | 0/? | 0 | ⏸️ PENDING |
| ConstructorInfo | 0/? | 0/? | 0 | ⏸️ PENDING |
| ClassInfo | 0/? | 0/? | 0 | ⏸️ PENDING |
| PackageInfo | 0/? | 0/? | 0 | ⏸️ PENDING |
| AnnotationInfo | 0/? | 0/? | 0 | ⏸️ PENDING |
| AnnotationList | 0/? | 0/? | 0 | ⏸️ PENDING |

---

## Notes

### Design Decisions

1. **Why dual annotation support?**
   - `Annotation` methods for direct access (e.g., `@Bean.class`)
   - `AnnotationInfo` methods for context-aware access (where declared, hierarchy, etc.)
   
2. **Why unmodifiable lists?**
   - Prevents external modification of internal caches
   - More modern API than arrays
   - Better type safety

3. **Why Atomics over synchronized?**
   - Better performance for read-heavy scenarios
   - Lock-free algorithms
   - Simpler code

### Breaking Changes

This refactoring will have breaking changes:
- Array return types → List return types
- May need deprecation period or major version bump

### Testing Strategy

For each class:
1. Test basic functionality
2. Test caching (same instance returned)
3. Test thread-safety (concurrent access)
4. Test unmodifiability (lists cannot be modified)
5. Test both Annotation and AnnotationInfo methods

---

## Current Session

**Date**: November 1, 2025
**Working On**: AccessibleInfo - Initial assessment complete
**Next Steps**: 
1. Ready to begin refactoring AccessibleInfo
2. No existing tests found - will need to create from scratch
3. First method to refactor: `getAnnotations()` - convert to return `List<Annotation>` with caching

### Session Summary

✅ Created project tracking file
✅ Examined AccessibleInfo implementation (211 lines, 8 methods)
✅ Checked for existing tests (none found)
✅ Ready to begin method-by-method refactoring

**Current Implementation Analysis**:
- AccessibleInfo has 8 methods total
- 2 accessibility methods (`setAccessible()`, `isAccessible()`)
- 6 annotation methods (all return arrays, no caching)
- Uses simple delegation to `AccessibleObject`
- No caching infrastructure
- No `AnnotationInfo` support

**Next Action**: Begin Phase 1 of AccessibleInfo refactoring

