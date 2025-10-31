# Reflection Classes Migration Plan

## Goal
Move the following classes from `org.apache.juneau.reflect` (juneau-marshall) to `org.apache.juneau.common.reflect` (juneau-common):
- `ClassInfo`
- `ConstructorInfo`
- `ExecutableInfo`
- `FieldInfo`
- `MethodInfo`
- `ParamInfo`
- `AnnotationInfo` *(added - must move with ClassInfo/MethodInfo due to circular references)*
- `AnnotationList` *(added - must move with AnnotationInfo)*

## Current Location
All eight classes are currently in:
- **Package**: `org.apache.juneau.reflect`
- **Module**: `juneau-core/juneau-marshall`
- **Path**: `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/reflect/`

## Target Location
- **Package**: `org.apache.juneau.common.reflect`
- **Module**: `juneau-core/juneau-common`
- **Path**: `juneau-core/juneau-common/src/main/java/org/apache/juneau/common/reflect/`

## Note on Circular Dependencies
`AnnotationInfo` and `AnnotationList` were added to the migration because they have circular references with `ClassInfo` and `MethodInfo`. Phase 1 successfully removed all juneau-marshall dependencies from these classes so they can now move to juneau-common, but they must move together with the other reflection classes.

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

### 7. AnnotationInfo.java
**Status**: ✅ **READY TO MOVE** (must move with ClassInfo/MethodInfo)

**Current Imports from juneau-marshall**: None (all removed in Phase 1c)

**Dependencies**:
- ✅ `org.apache.juneau.common.annotation.*` - Already in juneau-common (AnnotationGroup)
- ✅ `org.apache.juneau.common.reflect.*` - Already in juneau-common (ExecutableException)
- ✅ `org.apache.juneau.common.utils.*` - Already in juneau-common

**Circular References**:
- Has fields of type `ClassInfo` and `MethodInfo`
- Has static methods that take `ClassInfo` and `MethodInfo` as parameters
- **Must move together with ClassInfo/MethodInfo**

**Issues**: None (all juneau-marshall dependencies removed in Phase 1c)

**Phase 1c Changes Applied**:
- ✅ Refactored `toJsonMap()` → `toMap()` using `LinkedHashMap` instead of `JsonMap`
- ✅ Simplified `toString()` to use standard Java instead of `Json5`
- ✅ Removed `getApplies()` method (logic moved to `AnnotationWorkList`)
- ✅ Removed `applyConstructors` field

---

### 8. AnnotationList.java
**Status**: ✅ **READY TO MOVE** (must move with AnnotationInfo)

**Current Imports from juneau-marshall**: None

**Dependencies**:
- ✅ `org.apache.juneau.common.utils.*` - Already in juneau-common (PredicateUtils)

**Issues**: None

---

## Migration Plan

### Phase 1: Prepare AnnotationInfo/AnnotationList for migration ✅ COMPLETED
**Goal**: Remove juneau-marshall dependencies from AnnotationInfo/AnnotationList so they can move to juneau-common together with ClassInfo/MethodInfo.

**Sub-phase 1a: Add static methods to AnnotationInfo** ✅
- Added static methods that take ClassInfo/MethodInfo as parameters
- Made `ClassInfo.splitRepeated()` and `MethodInfo.findMatchingOnClass()` package-private

**Sub-phase 1b: Remove annotation methods from ClassInfo/MethodInfo** ✅
- Removed methods returning AnnotationList from ClassInfo/MethodInfo
- Updated all callers (~108 call sites) to use static methods on AnnotationInfo

**Sub-phase 1c: Remove juneau-marshall dependencies from AnnotationInfo** ✅
1. ✅ **Refactored `toJsonMap()` and `toString()`**
   - Changed `toJsonMap()` → `toMap()` returning `LinkedHashMap<String, Object>`
   - Simplified `toString()` to use standard Java `toString()`
   - Removed dependencies: `JsonMap`, `Json5`

2. ✅ **Moved `AnnotationGroup` to juneau-common** (user action)
   - Moved from `org.apache.juneau.annotation.AnnotationGroup`
   - To `org.apache.juneau.common.annotation.AnnotationGroup`
   - Result: `isInGroup()` method now clean

3. ✅ **Refactored `getApplies()` method**
   - Moved applier instantiation logic from `AnnotationInfo.getApplies()` to `AnnotationWorkList.applyAnnotation()`
   - Removed `getApplies()` method from AnnotationInfo
   - Removed `applyConstructors` field from AnnotationInfo
   - Removed dependencies: `VarResolverSession`, `ContextApply`, `AnnotationApplier`

**Final AnnotationInfo dependencies (all juneau-common or JDK):**
- ✅ Standard Java (JDK) classes only
- ✅ `org.apache.juneau.common.annotation.*` (AnnotationGroup)
- ✅ `org.apache.juneau.common.reflect.*` (ExecutableException)
- ✅ `org.apache.juneau.common.utils.*` (utility methods)

**Final AnnotationList dependencies (all juneau-common or JDK):**
- ✅ Standard Java (JDK) classes only
- ✅ `org.apache.juneau.common.utils.*` (PredicateUtils)

**Result:** ✅ Both AnnotationInfo and AnnotationList are now ready to move to juneau-common (must move together with ClassInfo/MethodInfo due to circular references)

### Phase 2: Prepare juneau-common ✅ COMPLETED (pending Phase 3)
**Goal**: Add `getMatchingArgs()` method to ClassUtils in juneau-common so that MethodInfo and ConstructorInfo can use it after migration.

**Changes completed:**

1. ✅ **Added `getMatchingArgs()` to `org.apache.juneau.common.utils.ClassUtils`**
   - Extracted logic from `ClassUtils2.getMatchingArgs()` in juneau-marshall
   - Added comprehensive Javadoc with detailed examples
   - Covers all use cases: argument reordering, missing parameters, extra parameters, primitive/wrapper handling, type hierarchy
   - Method signature: `public static Object[] getMatchingArgs(Class<?>[] paramTypes, Object...args)`

2. ✅ **Updated MethodInfo to use new location**
   - Changed import from `org.apache.juneau.internal.*` to `org.apache.juneau.common.utils.*`
   - Changed call from `ClassUtils2.getMatchingArgs()` to `ClassUtils.getMatchingArgs()`
   - File: `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/reflect/MethodInfo.java` (line 654)

3. ✅ **Updated ConstructorInfo to use new location**
   - Changed import from `org.apache.juneau.internal.*` to `org.apache.juneau.common.utils.*`
   - Changed call from `ClassUtils2.getMatchingArgs()` to `ClassUtils.getMatchingArgs()`
   - File: `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/reflect/ConstructorInfo.java` (line 249)

**Note:** juneau-common won't compile yet because `ClassUtils.getMatchingArgs()` references `ClassInfo.of()`, and `ClassInfo` is still in juneau-marshall. This will be resolved in Phase 3 when all 8 reflection classes move together to juneau-common.

### Phase 3: Move reflection classes ✅ COMPLETED
**Goal**: Move all 8 reflection classes to juneau-common and update all imports across the codebase.

**Changes completed:**

1. ✅ **Created target directory** in juneau-common
   - `juneau-core/juneau-common/src/main/java/org/apache/juneau/common/reflect/`

2. ✅ **Moved all 8 classes using git mv**
   - ClassInfo.java
   - ConstructorInfo.java
   - ExecutableInfo.java
   - FieldInfo.java
   - MethodInfo.java
   - ParamInfo.java
   - AnnotationInfo.java
   - AnnotationList.java

3. ✅ **Updated package declarations** in all 8 classes
   - Changed from `package org.apache.juneau.reflect;`
   - To `package org.apache.juneau.common.reflect;`

4. ✅ **Updated all imports across codebase** (196+ files)
   - Changed `import org.apache.juneau.reflect.*` to `import org.apache.juneau.common.reflect.*`
   - Changed all static imports as well

5. ✅ **Fixed remaining juneau-marshall files** that still needed access to Mutater/Mutaters
   - Added `import org.apache.juneau.reflect.*;` for Mutater/Mutaters (which remain in juneau-marshall)
   - Fixed: ClassMeta.java, SimplePartParserSession.java, SimplePartSerializerSession.java, UonSerializerSession.java, RrpcServlet.java

6. ✅ **Moved test files** to match new package structure
   - Moved all test files from `juneau-utest/src/test/java/org/apache/juneau/reflect/`
   - To `juneau-utest/src/test/java/org/apache/juneau/common/reflect/`
   - Updated package declarations in all test files
   - This was necessary to maintain package-private access to internal methods
   - Files moved: AnnotationInfoTest.java, ClassInfo_Test.java, ConstructorInfoTest.java, ExecutableInfo_Test.java, FieldInfo_Test.java, MethodInfo_Test.java, ParamInfoTest.java, AClass.java, AInterface.java, PA.java, package-info.java

7. ✅ **Fixed test imports**
   - Updated ClassMeta_Test.java to import from new location
   - Fixed MutatersTest.java to import Mutaters from old location (still in juneau-marshall)
   - Removed duplicate imports

**Result:** ✅ **Full project compilation successful!**
- juneau-common: 101 source files (up from 93)
- juneau-marshall: Compiles successfully
- All modules: Compile and test-compile successfully
- Total time: ~17 seconds

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

### ✅ Resolved:

1. **AnnotationInfo/AnnotationList juneau-marshall dependencies** - ✅ **RESOLVED**
   - Status: Phase 1 completed successfully
   - Solution implemented:
     - Sub-phase 1a: Added static methods to AnnotationInfo that take ClassInfo/MethodInfo as parameters
     - Sub-phase 1b: Removed AnnotationList-returning methods from ClassInfo/MethodInfo
     - Sub-phase 1c: Removed all juneau-marshall dependencies from AnnotationInfo (toJsonMap/toString refactoring, getApplies refactoring, AnnotationGroup moved)
   - Result: AnnotationInfo and AnnotationList can now move to juneau-common (must move together with ClassInfo/MethodInfo due to circular references)

2. **ClassUtils2.getMatchingArgs()** (affects MethodInfo, ConstructorInfo) - ✅ **RESOLVED**
   - Status: Phase 2 completed successfully
   - Solution implemented:
     - Added `getMatchingArgs()` to `org.apache.juneau.common.utils.ClassUtils`
     - Updated MethodInfo and ConstructorInfo to use new location
   - Result: MethodInfo and ConstructorInfo are ready to move (pending Phase 3 when ClassInfo moves)

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

**Complete Migration with Refactoring** (IN PROGRESS)
1. ✅ **Phase 1**: Prepare AnnotationInfo/AnnotationList for migration (COMPLETED)
   - ✅ Sub-phase 1a: Added static methods to AnnotationInfo
   - ✅ Sub-phase 1b: Removed AnnotationList-returning methods from ClassInfo/MethodInfo
   - ✅ Sub-phase 1c: Removed all juneau-marshall dependencies from AnnotationInfo
2. ⏳ **Phase 2**: Add getMatchingArgs() to ClassUtils in juneau-common (IN PROGRESS)
3. ⏭️ **Phase 3**: Move all 8 reflection classes to juneau-common
4. ⏭️ **Phase 4**: Create deprecated aliases for backward compatibility
5. ⏭️ **Phase 5**: Test and document

**Benefits:**
- Provides full reflection capability in juneau-common
- Removes circular dependencies between modules
- All 8 reflection classes can move together to juneau-common
- Better separation of concerns (annotation processing vs reflection utilities)

## Phase 1c Summary (Completed)

This phase was critical additional work discovered after starting Phase 1. The goal was to remove all juneau-marshall dependencies from `AnnotationInfo` and `AnnotationList` so they could move to juneau-common.

**Work Completed:**

1. **Refactored `toJsonMap()` and `toString()` methods**
   - Problem: Used `JsonMap` (juneau-marshall) and `Json5` (juneau-marshall) 
   - Solution: Changed to use standard Java `LinkedHashMap` and `.toString()`
   - Files modified: `AnnotationInfo.java`

2. **Moved `AnnotationGroup` annotation to juneau-common**
   - Problem: `isInGroup()` method depended on `AnnotationGroup` annotation in juneau-marshall
   - Solution: User moved `AnnotationGroup` from `org.apache.juneau.annotation` to `org.apache.juneau.common.annotation`
   - Result: `isInGroup()` method now has no juneau-marshall dependencies

3. **Refactored `getApplies()` method**
   - Problem: Method used `VarResolverSession`, `ContextApply`, and `AnnotationApplier` from juneau-marshall
   - Solution: Moved applier instantiation logic to `AnnotationWorkList.applyAnnotation()` helper method
   - Files modified: `AnnotationInfo.java`, `AnnotationWorkList.java`
   - Removed: `getApplies()` method, `applyConstructors` field
   - Call sites updated: 1 (in `AnnotationWorkList`)

**Final Result:**
- ✅ `AnnotationInfo` now depends ONLY on juneau-common and JDK classes
- ✅ `AnnotationList` now depends ONLY on juneau-common and JDK classes
- ✅ Both classes ready to move (must move with ClassInfo/MethodInfo due to circular references)
- ✅ Full project compilation successful

## Next Steps

1. ✅ **Phase 1 Complete**: Prepare AnnotationInfo/AnnotationList for migration
   - ✅ Sub-phase 1a: Added static methods to AnnotationInfo
   - ✅ Sub-phase 1b: Removed annotation methods from ClassInfo/MethodInfo
   - ✅ Sub-phase 1c: Removed all juneau-marshall dependencies from AnnotationInfo
     - ✅ Refactored toJsonMap()/toString()
     - ✅ AnnotationGroup moved to juneau-common
     - ✅ Refactored getApplies() method
   - ✅ Full project compilation successful

2. ✅ **Phase 2 Complete**: Add getMatchingArgs() to ClassUtils
   - ✅ Extracted `ClassUtils2.getMatchingArgs()` logic from juneau-marshall
   - ✅ Added as public method to `org.apache.juneau.common.utils.ClassUtils` with comprehensive javadoc
   - ✅ Updated MethodInfo and ConstructorInfo to use new ClassUtils location
   - Note: juneau-common won't compile until Phase 3 (ClassInfo still in juneau-marshall)

3. ✅ **Phase 3 Complete**: Move all 8 reflection classes to juneau-common
   - ✅ Moved all 8 classes using git mv
   - ✅ Updated package declarations in all classes
   - ✅ Updated 196+ import statements across entire codebase
   - ✅ Fixed juneau-marshall files needing Mutater/Mutaters
   - ✅ Moved test files to new package structure
   - ✅ Full project compilation successful

4. ⏭️ **Phase 4**: SKIPPED - Backward compatibility not needed for internal APIs

5. ⏭️ **Phase 5**: Test and document
   - Run all tests
   - Update documentation (if needed)
   - Update MIGRATION.md (if needed)

