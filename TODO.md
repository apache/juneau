# TODO List

## Issues to Fix

- [ ] TODO-3 Figure out why CallLogger and ThrownStore objects are not automatically injected into REST classes in spring boot.

## Code Quality Improvements

- [ ] TODO-5 Fields should be alphabetized.
- [ ] TODO-6 Investigate if there are any other W3 or RFC specifications that would make good candidates for new bean modules.
- [ ] TODO-12 Tests for Spring Boot testing.
- [ ] TODO-13 Search for places in code that should be using new try-with-return syntax.
- [ ] TODO-14 The name parameter on annotations like Query when used on method parameters should be optional if parameter names are persisted in the bytecode.
- [ ] TODO-15 It appears StringUtils is going to become a commonly-used external class. Let's see if we can enhance it with commonly used string utility methods.
- [ ] TODO-16 Search for calls to filteredMap() with the following pattern and alphabetize the lines:
		return filteredMap()
			.append("addBeanTypes", addBeanTypes)
			.append("keepNullProperties", keepNullProperties)
			.append("trimEmptyCollections", trimEmptyCollections)
			.append("trimEmptyMaps", trimEmptyMaps)
			.append("trimStrings", trimStrings)
			.append("sortCollections", sortCollections)
			.append("sortMaps", sortMaps)
			.append("addRootType", addRootType)
			.append("uriContext", uriContext)
			.append("uriResolution", uriResolution)
			.append("uriRelativity", uriRelativity)
			.append("listener", listener);
- [ ] TODO-18 Look for places where we concatenate strings across multiple lines and determine if they can use Java multiline strings.
- [ ] TODO-27 Determine if there are any other good candidates for Stringifiers and Listifiers.
- [ ] TODO-28 Remove dependencies on jakarta.xml.bind-api.
- [ ] TODO-29 Finish setting up SonarQube analysis in git workflow.
- [ ] TODO-31 Cache should extend from ConcurrentHashMap.
- [ ] TODO-32 TupleXFunction classes are redundant. Replace them with FunctionX.
- [ ] TODO-33 Figure out if BidiMap needs an unmodifiable mode or if it can just be wrapped in an unmodifiable wrapper.
- [ ] TODO-34 CharValue should have the following methods: is(char), isAny(char...), isAny(String). Determine if there are other useful methods that can be added to the XValue classes.
- [ ] TODO-35 Replace instances of Objects.requireNonNull with assertArgNotNull.
- [ ] TODO-36 Replace instances of Object.equals with Utils.eq.

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

## Code Style and Consistency

- [ ] TODO-30 Ensure all Builder methods are consistently using "value" as setter parameter names.
