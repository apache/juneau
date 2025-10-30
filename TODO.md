# TODO List

This file tracks pending tasks for the Apache Juneau project. For completed items, see [TODO-completed.md](TODO-completed.md).

## Issues to Fix

- [ ] TODO-3 Figure out why CallLogger and ThrownStore objects are not automatically injected into REST classes in spring boot.

## Code Quality Improvements

- [ ] TODO-55 Convert `instanceof` followed by cast to use pattern matching for instanceof (Java 14+ feature).
  - **Pattern**: `if (obj instanceof Type) { Type t = (Type)obj; ... }` → `if (obj instanceof Type t) { ... }`
  - **Estimated instances**: ~52 occurrences across the codebase
  - **Examples found**:
    - `CharSequenceReader.java`: `if (cs instanceof String) s = (String)cs;` → `if (cs instanceof String s2) s = s2;`
    - `ClassUtils.java`: `if (t instanceof ParameterizedType) { ParameterizedType pt = (ParameterizedType)t; ...` → `if (t instanceof ParameterizedType pt) { ...`
    - `PropertyExtractor_Test.java`: `if (o instanceof String) { String str = (String)o; ...` → `if (o instanceof String str) { ...`
  - **Benefits**: Eliminates redundant casts, makes code more concise and type-safe, reduces verbosity
  - **Files with multiple opportunities**: ClassUtils.java, CharSequenceReader.java, HttpHeaders.java, HttpParts.java, RestContext.java, and various test files
- [ ] TODO-5 Fields should be alphabetized.
- [ ] TODO-6 Investigate if there are any other W3 or RFC specifications that would make good candidates for new bean modules.
- [ ] TODO-12 Tests for Spring Boot testing.
- [ ] TODO-14 The name parameter on annotations like Query when used on method parameters should be optional if parameter names are persisted in the bytecode.
- [ ] TODO-15 It appears StringUtils is going to become a commonly-used external class. Let's see if we can enhance it with commonly used string utility methods.
- [ ] TODO-27 Determine if there are any other good candidates for Stringifiers and Listifiers.
- [ ] TODO-29 Finish setting up SonarQube analysis in git workflow.
- [ ] TODO-49 Use static imports for all method calls to StringUtils.
- [x] TODO-52 Use static imports for all method calls to Utils.
- [ ] TODO-54 Search for places in code where Calendar should be replaced with ZonedDateTime.

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
