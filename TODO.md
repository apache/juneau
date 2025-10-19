# TODO List

## Issues to Fix

- [ ] StringUtils.toHex8 should throw an exception if it's a negative number.
- [ ] CallLogger.getThrownStats() should call thrownStore.add(Throwable), not thrownStore.getStats(Throwable).
- [ ] Figure out why CallLogger and ThrownStore objects are not automatically injected into REST classes in spring boot.

## Code Quality Improvements

- [ ] Look for places where Utils can be replaced with static imports.
- [ ] Fields should be alphabetized.
- [ ] Investigate if there are any other W3 or RFC specifications that would make good candidates for new bean modules.
- [ ] Some package-info.java classes are using non-standard license headers.
- [ ] Look for any old commented-out code.
- [ ] Remove CsvParser.
- [ ] DataUtils.toValidISO8601DT should use StateEnum.
- [ ] Find other places where StateEnum should be used. Search for the regular expression "S1\s*=\s*1".
- [ ] Tests for Spring Boot testing.
- [ ] Search for places in code that should be using new try-with-return syntax.
- [ ] The name parameter on annotations like Query when used on method parameters should be optional if parameter names are persisted in the bytecode.
- [ ] It appears StringUtils is going to become a commonly-used external class. Let's see if we can enhance it with commonly used string utility methods.
- [ ] Search for calls to filteredMap() with the following pattern and alphabetize the lines:
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
- [ ] Search for "TODO" in javadoc comments for update.
- [ ] Look for places where we concatenate strings across multiple lines and determine if they can use Java multiline strings.

## Notes

This TODO list tracks specific issues that need to be addressed in the Juneau project.
