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

### Phase 1: Prepare juneau-common
1. **Add getMatchingArgs() to ClassUtils**
   - Extract `ClassUtils2.getMatchingArgs()` logic
   - Add as public method to `org.apache.juneau.common.utils.ClassUtils`
   - Add comprehensive javadoc
   - Add unit tests in juneau-common

### Phase 2: Update reflection classes
1. **Update imports in all 6 classes**
   - Change package from `org.apache.juneau.reflect` to `org.apache.juneau.common.reflect`
   - Update `FieldInfo`: Remove unused `import org.apache.juneau.*;`
   - Update `MethodInfo`: Change `ClassUtils2.getMatchingArgs()` to `ClassUtils.getMatchingArgs()`
   - Update `ConstructorInfo`: Change `ClassUtils2.getMatchingArgs()` to `ClassUtils.getMatchingArgs()`
   - Note: `ParamInfo` already fixed to use reflection for @Name

2. **Move all 6 files**
   - Move from `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/reflect/`
   - To `juneau-core/juneau-common/src/main/java/org/apache/juneau/common/reflect/`

### Phase 3: Create backward compatibility
1. **Add deprecated aliases in juneau-marshall**
   - Create `@Deprecated` classes in old package `org.apache.juneau.reflect`
   - Each class extends/wraps the new location
   - Point users to new location in deprecation message

2. **Update juneau-marshall imports**
   - Update all files in juneau-marshall that import these classes
   - Change to use new package location

### Phase 4: Testing & Documentation
1. **Run all tests** to ensure nothing broke
2. **Update documentation** to reference new package
3. **Update MIGRATION.md** with notes about deprecated classes

## Summary of Blockers

### Must be resolved before migration:

1. **ClassUtils2.getMatchingArgs()** (affects MethodInfo, ConstructorInfo)
   - Current location: `org.apache.juneau.internal.ClassUtils2` (juneau-marshall)
   - Needs to be: `org.apache.juneau.common.utils.ClassUtils` (juneau-common)
   - Impact: Used for smart parameter matching in reflection invoke operations

### Can be fixed during migration:

2. **Unused import** (affects FieldInfo)
   - `import org.apache.juneau.*;` appears to be unused
   - Simply remove it

### ✅ Already Resolved:

3. **@Name annotation dependency** (was affecting ParamInfo) - **FIXED**
   - Used reflection to work with any annotation named "Name"
   - No longer requires compile-time dependency on specific annotation
   - Works with `@Name` from any package

## Recommended Approach

**Option A: Complete Migration** (RECOMMENDED)
- Resolve remaining blocker (getMatchingArgs)
- Move getMatchingArgs() to ClassUtils
- Then move all 6 classes together
- Provides full reflection capability in juneau-common

**Option B: Partial Migration**
- Move ClassInfo, ExecutableInfo, FieldInfo, ParamInfo first (ready now)
- Leave MethodInfo, ConstructorInfo until getMatchingArgs dependency resolved
- Requires careful dependency management

**Option C: Keep Current Structure**
- Don't move anything
- Keep all reflection in juneau-marshall
- Simplest but doesn't achieve the stated goal

## Next Steps

1. **Review and approve** this plan
2. **Decide on approach** (A, B, or C)
3. **Implement Phase 1** (prepare juneau-common)
4. **Implement Phase 2** (move classes)
5. **Implement Phase 3** (backward compatibility)
6. **Test and document**

