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

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.commons.utils.Utils;

/**
 * Minimal module-local test utilities for <c>juneau-rest-server</c>.
 *
 * <p>
 * Provides only the {@code assertThrowsWithMessage} helpers required by the relocated
 * {@link org.apache.juneau.mstat} tests, and inherits the shared object-rendering helpers
 * (such as {@code r(Object)}) from {@link Utils}.
 *
 * <p>
 * This class is intentionally self-contained: it must NOT reference
 * {@code org.apache.juneau.rest.mock.*} or any type that would pull a higher module into
 * <c>juneau-rest-server</c>'s test scope and introduce a Maven reactor cycle. It is deliberately
 * NOT a copy of the full cross-module {@code TestUtils} that lives in the integration-test residual.
 */
public class TestUtils extends Utils {

	/**
	 * Asserts that the specified executable throws an exception whose chained messages contain the
	 * specified substring.
	 *
	 * @param <T> The expected exception type.
	 * @param expectedType The expected exception type.
	 * @param expectedSubstring The substring expected in the exception message chain.
	 * @param executable The code expected to throw.
	 * @return The thrown exception.
	 */
	public static <T extends Throwable> T assertThrowsWithMessage(Class<T> expectedType, String expectedSubstring, org.junit.jupiter.api.function.Executable executable) {
		var exception = assertThrows(expectedType, executable);
		var messages = getMessages(exception);
		assertTrue(messages.contains(expectedSubstring), "Expected message to contain: " + expectedSubstring + ".\nActual:\n" + messages);
		return exception;
	}

	private static String getMessages(Throwable t) {
		return Stream.iterate(t, Objects::nonNull, Throwable::getCause).map(Throwable::getMessage).collect(Collectors.joining("\n"));
	}
}
