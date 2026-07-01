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
package org.apache.juneau;

import static java.util.stream.Collectors.*;
import static org.apache.juneau.marshall.marshaller.MarshallUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.*;

import org.apache.juneau.commons.utils.*;
import org.junit.jupiter.api.*;

/**
 * Test utilities for the juneau-marshall module.
 *
 * <p>Contains a marshall-compatible subset of the methods available in the full {@code TestUtils}
 * in {@code juneau-integration-tests}.  Methods that depend on {@code juneau-rest-*} modules are excluded.</p>
 */
public class TestUtils extends Utils {

	public static void assertEqualsAll(Object...values) {
		for (var i = 1; i < values.length; i++) {
			assertEquals(values[0], values[i], fs("Elements at index {0} and {1} did not match. {0}={2}, {1}={3}", 0, i, r(values[0]), r(values[i])));
		}
	}

	public static String assertJson(String expected, Object value) {
		assertEquals(expected, json5(value));
		return expected;
	}

	public static void assertNotEqualsAny(Object actual, Object...values) {
		assertNotNull(actual, "Value was null.");
		for (var i = 0; i < values.length; i++) {
			assertNotEquals(values[i], actual, fs("Element at index {0} unexpectedly matched.  expected={1}, actual={2}", i, values[i], s(actual)));
		}
	}

	public static <T extends Throwable> T assertThrowsWithMessage(Class<T> expectedType, String expectedSubstring, org.junit.jupiter.api.function.Executable executable) {
		var exception = Assertions.assertThrows(expectedType, executable);
		var messages = getMessages(exception);
		assertTrue(messages.contains(expectedSubstring), fs("Expected message to contain: {0}.\nActual:\n{1}", expectedSubstring, messages));
		return exception;
	}

	private static String getMessages(Throwable t) {
		return Stream.iterate(t, Throwable::getCause).takeWhile(e -> e != null).map(Throwable::getMessage).collect(joining("\n"));
	}
}
