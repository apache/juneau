# TODO List

**Last generated TODO number: TODO-73**

This file tracks pending tasks for the Apache Juneau project. For completed items, see [TODO-completed.md](TODO-completed.md).

## Issues to Fix

- [ ] TODO-3 Figure out why CallLogger and ThrownStore objects are not automatically injected into REST classes in spring boot.

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
- [ ] TODO-73 Move reflection classes (ClassInfo, ConstructorInfo, ExecutableInfo, FieldInfo, MethodInfo, ParamInfo) from org.apache.juneau.reflect to org.apache.juneau.common.reflect. See TODO-reflectionMigrationPlan.md for details.

## Framework Improvements

- [ ] TODO-19 ClassInfo improvements to getMethod (e.g. getMethodExact vs getMethod).
- [ ] TODO-21 Thrown NotFound causes - javax.servlet.ServletException: Invalid method response: 200

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
