# Reflection Classes Migration Plan

## Goal
Move the following classes from `org.apache.juneau.reflect` (juneau-marshall) to `org.apache.juneau.common.reflect` (juneau-common):
- `ClassInfo`
- `ConstructorInfo`
- `ExecutableInfo`
- `FieldInfo`
- `MethodInfo`
- `ParamInfo`

## Current Location
All six classes are currently in:
- **Package**: `org.apache.juneau.reflect`
- **Module**: `juneau-core/juneau-marshall`
- **Path**: `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/reflect/`

## Target Location
- **Package**: `org.apache.juneau.common.reflect`
- **Module**: `juneau-core/juneau-common`
- **Path**: `juneau-core/juneau-common/src/main/java/org/apache/juneau/common/reflect/`

## Dependency Analysis

### 1. ClassInfo.java
**Status**: ✅ **READY TO MOVE**

**Current Imports from juneau-marshall**: None

**Dependencies**:
- ✅ `org.apache.juneau.common.collections.*` - Already in juneau-common
- ✅ `org.apache.juneau.common.reflect.*` - Already in juneau-common
- ✅ `org.apache.juneau.common.utils.*` - Already in juneau-common

**Issues**: None

---

### 2. FieldInfo.java
**Status**: ⚠️ **HAS UNUSED IMPORT**

**Current Imports from juneau-marshall**:
- `import org.apache.juneau.*;` (line 28)

**Dependencies**:
- ✅ `org.apache.juneau.common.collections.*` - Already in juneau-common
- ✅ `org.apache.juneau.common.reflect.*` - Already in juneau-common

**Issues**: 
- The wildcard import `org.apache.juneau.*` appears to be unused (no references found to BeanContext, Context, etc.)

**Resolution**:
- Remove the unused `import org.apache.juneau.*;` statement

---

### 3. ParamInfo.java
**Status**: ✅ **READY TO MOVE**

**Current Imports from juneau-marshall**: None

**Dependencies**:
- ✅ `org.apache.juneau.common.collections.*` - Already in juneau-common
- ✅ `org.apache.juneau.common.reflect.*` - Already in juneau-common

**Issues**: None

**Resolution Applied**: 
- **Used reflection to work with any @Name annotation**
- Removed compile-time dependency on `org.apache.juneau.annotation.Name`
- `ParamInfo` now searches for any annotation with simple name "Name" using reflection
- Calls the annotation's `value()` method dynamically to extract the parameter name
- Works with `@Name` from any package without requiring a compile-time dependency
- Maintains backward compatibility with existing `@Name` annotations
- Implementation in `getNameFromAnnotation()` helper method

---

### 4. MethodInfo.java
**Status**: ❌ **BLOCKED - Requires ClassUtils2.getMatchingArgs()**

**Current Imports from juneau-marshall**:
- `import org.apache.juneau.internal.*;` (line 31)

**Dependencies**:
- ✅ `org.apache.juneau.common.collections.*` - Already in juneau-common
- ✅ `org.apache.juneau.common.reflect.*` - Already in juneau-common
- ❌ **`ClassUtils2.getMatchingArgs()`** - Currently in `org.apache.juneau.internal.ClassUtils2` (juneau-marshall)

**Usage of ClassUtils2**:
- Line 667: `return m.invoke(pojo, ClassUtils2.getMatchingArgs(m.getParameterTypes(), args));`

**Issues**:
- `MethodInfo.invoke()` uses `ClassUtils2.getMatchingArgs()` to match varargs parameters
- `ClassUtils2` is in juneau-marshall's internal package

**Resolution Options**:
1. **Move getMatchingArgs() to ClassUtils in juneau-common** (RECOMMENDED)
   - Extract `ClassUtils2.getMatchingArgs()` method
   - Move to `org.apache.juneau.common.utils.ClassUtils`
   - Make it public API (it's a useful utility)
   - Update call in `MethodInfo`

2. **Inline the logic**
   - Copy the parameter matching logic directly into `MethodInfo.invoke()`
   - Removes dependency but duplicates code

3. **Remove the feature**
   - Make `MethodInfo.invoke()` not support parameter reordering
   - Would be a breaking change

---

### 5. ConstructorInfo.java
**Status**: ❌ **BLOCKED - Requires ClassUtils2.getMatchingArgs()**

**Current Imports from juneau-marshall**:
- `import org.apache.juneau.internal.*;` (line 28)

**Dependencies**:
- ✅ `org.apache.juneau.common.collections.*` - Already in juneau-common
- ✅ `org.apache.juneau.common.reflect.*` - Already in juneau-common
- ❌ **`ClassUtils2.getMatchingArgs()`** - Currently in `org.apache.juneau.internal.ClassUtils2` (juneau-marshall)

**Usage of ClassUtils2**:
- Line 249: `return invoke(ClassUtils2.getMatchingArgs(c.getParameterTypes(), args));`

**Issues**: 
- Same as MethodInfo - uses `ClassUtils2.getMatchingArgs()`

**Resolution**:
- Same as MethodInfo resolution

---

### 6. ExecutableInfo.java
**Status**: ✅ **READY TO MOVE**

**Current Imports from juneau-marshall**: None

**Dependencies**:
- ✅ `org.apache.juneau.common.reflect.*` - Already in juneau-common

**Issues**: None

---

## Migration Plan

### Phase 1: Refactor AnnotationInfo/AnnotationList dependencies ✅ COMPLETED
**Goal**: Remove dependencies from ClassInfo/MethodInfo on AnnotationInfo/AnnotationList by moving annotation-related methods to AnnotationInfo as static methods.

**Methods moved from ClassInfo to AnnotationInfo:**
1. ✅ `ClassInfo.forEachAnnotationInfo(Predicate, Consumer)` → `AnnotationInfo.forEachAnnotationInfo(ClassInfo, Predicate, Consumer)`
2. ✅ `ClassInfo.getAnnotationList()` → `AnnotationInfo.getAnnotationList(ClassInfo)`
3. ✅ `ClassInfo.getAnnotationList(Predicate)` → `AnnotationInfo.getAnnotationList(ClassInfo, Predicate)`

**Methods moved from MethodInfo to AnnotationInfo:**
1. ✅ `MethodInfo.forEachAnnotationInfo(Predicate, Consumer)` → `AnnotationInfo.forEachAnnotationInfo(MethodInfo, Predicate, Consumer)`
2. ✅ `MethodInfo.getAnnotationList()` → `AnnotationInfo.getAnnotationList(MethodInfo)`
3. ✅ `MethodInfo.getAnnotationList(Predicate)` → `AnnotationInfo.getAnnotationList(MethodInfo, Predicate)`
4. ✅ `MethodInfo.getAnnotationListMethodOnly(Predicate)` → `AnnotationInfo.getAnnotationListMethodOnly(MethodInfo, Predicate)`
5. ✅ `MethodInfo.forEachAnnotationInfoMethodOnly(Predicate, Consumer)` → `AnnotationInfo.forEachAnnotationInfoMethodOnly(MethodInfo, Predicate, Consumer)`

**Implementation completed:**
1. ✅ Added static methods to AnnotationInfo that take ClassInfo/MethodInfo as parameters
2. ✅ Updated ClassInfo/MethodInfo to delegate to the new static methods (maintains backward compatibility)
3. ✅ Made `ClassInfo.splitRepeated()` and `MethodInfo.findMatchingOnClass()` package-private for access by AnnotationInfo
4. ✅ Removed private helper methods from MethodInfo (now in AnnotationInfo)
5. ✅ Full project compilation successful

### Phase 2: Prepare juneau-common
1. **Add getMatchingArgs() to ClassUtils**
   - Extract `ClassUtils2.getMatchingArgs()` logic
   - Add as public method to `org.apache.juneau.common.utils.ClassUtils`
   - Note: Can use ClassInfo.of() since ClassInfo will be in juneau-common
   - Add comprehensive javadoc
   - Add unit tests in juneau-common

### Phase 3: Move reflection classes
1. **Update imports in all 6 classes**
   - Change package from `org.apache.juneau.reflect` to `org.apache.juneau.common.reflect`
   - Update `FieldInfo`: Remove unused `import org.apache.juneau.*;`
   - Update `MethodInfo`: Change `ClassUtils2.getMatchingArgs()` to `ClassUtils.getMatchingArgs()`
   - Update `ConstructorInfo`: Change `ClassUtils2.getMatchingArgs()` to `ClassUtils.getMatchingArgs()`
   - Note: `ParamInfo` already fixed to use reflection for @Name

2. **Move all 6 files using git mv**
   - Move from `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/reflect/`
   - To `juneau-core/juneau-common/src/main/java/org/apache/juneau/common/reflect/`

3. **Update all imports across the codebase**
   - Update all files that import `org.apache.juneau.reflect.*` to use `org.apache.juneau.common.reflect.*`

### Phase 4: Create backward compatibility
1. **Add deprecated aliases in juneau-marshall**
   - Create `@Deprecated` classes in old package `org.apache.juneau.reflect`
   - Each class extends the new location
   - Point users to new location in deprecation message

### Phase 5: Testing & Documentation
1. **Run all tests** to ensure nothing broke
2. **Update documentation** to reference new package
3. **Update MIGRATION.md** with notes about deprecated classes

## Summary of Blockers

### Must be resolved before migration:

1. **AnnotationInfo/AnnotationList dependencies** (affects ClassInfo, MethodInfo) - ✅ **RESOLVED**
   - Status: Phase 1 completed successfully
   - Solution implemented: Refactored annotation-related methods to static methods on AnnotationInfo that take ClassInfo/MethodInfo as parameters
   - Impact: Circular dependency removed, ClassInfo/MethodInfo can now be moved to juneau-common
   - Backward compatibility: Original methods now delegate to static methods, maintaining existing API

2. **ClassUtils2.getMatchingArgs()** (affects MethodInfo, ConstructorInfo)
   - Current location: `org.apache.juneau.internal.ClassUtils2` (juneau-marshall)
   - Needs to be: `org.apache.juneau.common.utils.ClassUtils` (juneau-common)
   - Impact: Used for smart parameter matching in reflection invoke operations

### Can be fixed during migration:

3. **Unused import** (affects FieldInfo)
   - `import org.apache.juneau.*;` appears to be unused
   - Simply remove it

### ✅ Already Resolved:

4. **@Name annotation dependency** (was affecting ParamInfo) - **FIXED**
   - Used reflection to work with any annotation named "Name"
   - No longer requires compile-time dependency on specific annotation
   - Works with `@Name` from any package

## Recommended Approach

**Complete Migration with Refactoring** (RECOMMENDED)
1. **Phase 1**: Refactor AnnotationInfo/AnnotationList dependencies
   - Move annotation-related methods from ClassInfo/MethodInfo to AnnotationInfo as static methods
   - Update ClassInfo/MethodInfo to delegate to the new static methods (keeps backward compatibility)
   - This breaks the circular dependency between reflection classes and annotation classes
2. **Phase 2**: Add getMatchingArgs() to ClassUtils in juneau-common
3. **Phase 3**: Move all 6 reflection classes to juneau-common
4. **Phase 4**: Create deprecated aliases for backward compatibility
5. **Phase 5**: Test and document

**Benefits:**
- Provides full reflection capability in juneau-common
- Removes circular dependencies between modules
- Maintains backward compatibility through delegation methods
- Better separation of concerns (annotation processing vs reflection utilities)

## Next Steps

1. ✅ **Review and approve** this revised plan (DONE)
2. ✅ **Implement Phase 1**: Refactor AnnotationInfo dependencies (DONE)
   - ✅ Added static methods to AnnotationInfo that take ClassInfo/MethodInfo as parameters
   - ✅ Updated ClassInfo/MethodInfo to delegate to new static methods
   - ✅ Made helper methods package-private for AnnotationInfo access
   - ✅ Full project compilation successful
3. **Implement Phase 2**: Add getMatchingArgs() to ClassUtils
4. **Implement Phase 3**: Move reflection classes
5. **Implement Phase 4**: Create backward compatibility aliases
6. **Implement Phase 5**: Test and document

