# TODO List

**Last generated TODO number: TODO-89**

This file tracks pending tasks for the Apache Juneau project. For completed items, see [TODO-completed.md](TODO-completed.md).

## Issues to Fix

- [ ] TODO-3 Figure out why CallLogger and ThrownStore objects are not automatically injected into REST classes in spring boot.
- [ ] TODO-87 Find references to `ParamInfo` and replace them with `ParameterInfo` throughout the codebase.

## Code Quality Improvements

- [ ] TODO-5 Fields should be alphabetized.
- [ ] TODO-6 Investigate if there are any other W3 or RFC specifications that would make good candidates for new bean modules.
- [ ] TODO-12 Tests for Spring Boot testing.
- [ ] TODO-14 The name parameter on annotations like Query when used on method parameters should be optional if parameter names are persisted in the bytecode.
- [ ] TODO-15 It appears StringUtils is going to become a commonly-used external class. Let's see if we can enhance it with commonly used string utility methods.
- [ ] TODO-27 Determine if there are any other good candidates for Stringifiers and Listifiers.
- [ ] TODO-29 Finish setting up SonarQube analysis in git workflow.
- [x] TODO-51 Convert local variable declarations to use `var` keyword where type is explicit on right-hand side.
  - **Phase 1**: ✅ Break up compound declarations (completed - 26 instances)
  - **Phase 2**: ✅ Convert safe patterns in local variable declarations (completed - 276 files)
- [ ] TODO-54 Search for places in code where Calendar should be replaced with ZonedDateTime.
- [x] TODO-73 Move reflection classes (ClassInfo, ConstructorInfo, ExecutableInfo, FieldInfo, MethodInfo, ParamInfo, AnnotationInfo, AnnotationList) from org.apache.juneau.reflect to org.apache.juneau.common.reflect. ✅ COMPLETED - All classes moved, 196+ files updated, 25,839 tests passing. See TODO-reflectionMigrationPlan.md and REFLECTION_MIGRATION_COMPLETE.md for details.
- [x] TODO-74 Add missing high-priority methods to ClassInfo to make it a complete replacement for java.lang.Class. ✅ COMPLETED - Added 8 methods including getClassLoader(), getDeclaringClass(), getEnclosingClass(), etc.
- [x] TODO-75 Add missing medium-priority methods to ClassInfo for enhanced functionality. ✅ COMPLETED - Added 10 methods including getCanonicalName(), isRecord(), isSealed(), getGenericSuperclass(), etc.
- [x] TODO-76 Add missing low-priority methods to ClassInfo for comprehensive API coverage. ✅ COMPLETED - Added 15 methods including getModule(), getNestHost(), getProtectionDomain(), arrayType(), etc.
- [x] TODO-77 Update ClassInfo.CACHE to use Juneau's Cache object for better cache management. ✅ COMPLETED - Replaced ConcurrentHashMap with Cache object supporting eviction and statistics.
- [x] TODO-78 Add missing methods to ExecutableInfo to make it a complete replacement for java.lang.reflect.Executable. ✅ COMPLETED - Added 13 methods including getModifiers(), isSynthetic(), isVarArgs(), getAnnotation(), getGenericExceptionTypes(), toGenericString(), etc. All 25,872 tests passing.
- [x] TODO-79 Add missing methods to ParamInfo to make it a complete replacement for java.lang.reflect.Parameter. ✅ COMPLETED - Added 12 methods including getDeclaringExecutable(), getModifiers(), isNamePresent(), isImplicit(), isSynthetic(), isVarArgs(), getParameterizedType(), getAnnotatedType(), getAnnotations(), getDeclaredAnnotations(), getAnnotationsByType(), getDeclaredAnnotationsByType(). All 25,872 tests passing.
- [x] TODO-80 Add missing methods to FieldInfo to make it a complete replacement for java.lang.reflect.Field. ✅ COMPLETED - Added 10 methods (7 high-priority + 3 medium-priority) including getModifiers(), isSynthetic(), isEnumConstant(), getGenericType(), getAnnotatedType(), getAnnotations(), getDeclaredAnnotations(), getAnnotationsByType(), getDeclaredAnnotationsByType(), toGenericString(). All 25,872 tests passing.
- [x] TODO-81 Add missing methods to MethodInfo to make it a complete replacement for java.lang.reflect.Method. ✅ COMPLETED - Added 4 methods (isBridge already existed) including getGenericReturnType(), getAnnotatedReturnType(), getDefaultValue(), isDefault(). All 25,872 tests passing.
- [x] TODO-82 Add missing methods to ConstructorInfo to make it a complete replacement for java.lang.reflect.Constructor. ✅ COMPLETED - Added 1 method: newInstance() as standard API alias for invoke(). All 25,872 tests passing.
- [x] TODO-83 Refactor reflection hierarchy to introduce AccessibleInfo base class. ✅ COMPLETED - Created AccessibleInfo to mirror java.lang.reflect.AccessibleObject, extracted common annotation and accessibility methods from ExecutableInfo and FieldInfo. New hierarchy: AccessibleInfo (base) -> ExecutableInfo -> MethodInfo/ConstructorInfo, and AccessibleInfo -> FieldInfo. All 25,872 tests passing.
- [x] TODO-85 Add missing methods to AnnotationInfo to make it a drop-in replacement for java.lang.annotation.Annotation. ✅ COMPLETED - Added 3 methods: annotationType(), hashCode(), equals(). AnnotationInfo now provides complete API compatibility with java.lang.annotation.Annotation while maintaining Juneau enhancements for context tracking and annotation hierarchy searching. All 25,872 tests passing.
- [x] TODO-86 Add PackageInfo class as a wrapper for java.lang.Package. ✅ COMPLETED - Created PackageInfo with caching, complete Package API implementation (getName(), getSpecificationTitle/Version/Vendor(), getImplementationTitle/Version/Vendor(), isSealed(), isCompatibleWith(), annotation methods), Juneau enhancements (hasAnnotation(), hasNoAnnotation()), and factory methods (of(Package), of(Class), of(ClassInfo)). Updated ClassInfo.getPackage() to return PackageInfo. All 25,872 tests passing.

## Framework Improvements

- [ ] TODO-19 ClassInfo improvements to getMethod (e.g. getMethodExact vs getMethod).
- [ ] TODO-21 Thrown NotFound causes - javax.servlet.ServletException: Invalid method response: 200
- [ ] TODO-88 Eliminate need for AssertionArgs in BctAssertions by allowing DEFAULT_CONVERTER to be overridden and resettable.
- [ ] TODO-89 Add ClassInfoTyped

## HTTP Response/Exception Improvements

- [ ] TODO-22 HttpResponse should use list of Headers and have a headers(Header...) method.
- [ ] TODO-23 HttpResponse should allow you to set code.
- [ ] TODO-24 HttpException subclasses can set status, but does it use code?
- [ ] TODO-25 HttpException should use list of Headers and have a headers(Header...) method.

- [ ] TODO-26 @ResponseBody and @ResponseHeaders shouldn't be required on HttpResponse objects.

## Notes

This TODO list tracks specific issues that need to be addressed in the Juneau project.

## Website/Docs

- [ ] TODO-29 Add searching to website using Algolia DocSearch.
