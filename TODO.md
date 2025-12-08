# TODO List

**Last generated TODO number: TODO-94**

This file tracks pending tasks for the Apache Juneau project. For completed items, see [TODO-completed.md](TODO-completed.md).

## Issues to Fix

- [ ] TODO-3 Figure out why CallLogger and ThrownStore objects are not automatically injected into REST classes in spring boot.

## Code Quality Improvements

- [ ] TODO-5 Fields should be alphabetized.
- [ ] TODO-6 Investigate if there are any other W3 or RFC specifications that would make good candidates for new bean modules.
- [ ] TODO-12 Tests for Spring Boot testing.
- [ ] TODO-14 The name parameter on annotations like Query when used on method parameters should be optional if parameter names are persisted in the bytecode.
- [x] TODO-15 It appears StringUtils is going to become a commonly-used external class. Let's see if we can enhance it with commonly used string utility methods.
- [ ] TODO-27 Determine if there are any other good candidates for Stringifiers and Listifiers.
- [ ] TODO-29 Finish setting up SonarQube analysis in git workflow.
- [ ] TODO-54 Search for places in code where Calendar should be replaced with ZonedDateTime.
- [ ] TODO-90 Investigate replacing `StringUtils.parseIsoCalendar()` with java.time APIs and removing the helper if possible.
- [ ] TODO-92 Investigate if `ClassInfo.asSubclass(Class<U>)` can return a `ClassInfoTyped<U>` object instead of `ClassInfo` for better type safety.
- [ ] TODO-93 Investigate if `ReflectionUtils.info(Class<?>)` should return a `ClassInfoTyped` object instead of `ClassInfo` for better type safety.

## Framework Improvements

- [x] TODO-19 ClassInfo improvements to getMethod (e.g. getMethodExact vs getMethod).
- [ ] TODO-21 Thrown NotFound causes - javax.servlet.ServletException: Invalid method response: 200
- [x] TODO-89 Add ClassInfoTyped
- [ ] TODO-91 Security: LogsResource returns HTTP 500 instead of 404 for malformed query parameters (CWE-74). When accessing log file URLs with encoded special characters in query parameters (e.g., `?method=VIEW%5C%5C%5C%22`), the system returns HTTP 500 "Invalid method response: 200" instead of HTTP 404. The error suggests it's incorrectly trying to find a Java method matching a malformed path. Should return 404 for invalid/malformed requests.
- [ ] TODO-94 Add a "cloaked" mode to IRS to always return 404s in place of 40x/50x responses.

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
