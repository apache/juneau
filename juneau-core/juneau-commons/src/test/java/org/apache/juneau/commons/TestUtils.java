/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.commons;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.commons.utils.*;
import org.junit.jupiter.api.function.*;

/**
 * Test utility class for juneau-commons tests.
 *
 * <p>Extends {@link Utils} to include test-only assertion helpers.
 * Use {@code import static org.apache.juneau.commons.TestUtils.*;} to pull in
 * all utility methods from both this class and the inherited {@link Utils} methods.</p>
 */
@SuppressWarnings({
	"java:S1172"  // Test utility methods may have unused parameters for consistent signatures
})
public class TestUtils extends Utils {

	/**
	 * Asserts that the given executable throws an exception of the expected type
	 * and that the exception message contains the given substring.
	 */
	public static <T extends Throwable> T assertThrowsWithMessage(Class<T> expectedType, String expectedSubstring, Executable executable) {
		var exception = assertThrows(expectedType, executable);
		var messages = getMessages(exception);
		assertTrue(messages.contains(expectedSubstring), "Expected message to contain: " + expectedSubstring + ".\nActual:\n" + messages);
		return exception;
	}

	/**
	 * Asserts that the given executable throws an exception of the expected type
	 * and that the exception message contains all of the given substrings.
	 */
	public static <T extends Throwable> T assertThrowsWithMessage(Class<T> expectedType, List<String> expectedSubstrings, Executable executable) {
		var exception = assertThrows(expectedType, executable);
		var messages = getMessages(exception);
		expectedSubstrings.forEach(x -> assertTrue(messages.contains(x), "Expected message to contain: " + x + ".\nActual:\n" + messages));
		return exception;
	}

	private static String getMessages(Throwable t) {
		return Stream.iterate(t, Throwable::getCause).takeWhile(e -> e != null).map(Throwable::getMessage).collect(Collectors.joining("\n"));
	}
}
