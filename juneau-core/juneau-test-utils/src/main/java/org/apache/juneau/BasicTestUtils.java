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
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.commons.utils.*;
import org.junit.jupiter.api.*;

/**
 * Shared, marshall-free test utilities common to Juneau test modules.
 *
 * <p>
 * Provides the stable pure-JDK/commons subset of the per-module {@code TestUtils} classes: exception-message
 * assertions, equality helpers, stream/reader factories, locale/timezone toggles, and URL construction.  Module-level
 * {@code TestUtils} classes extend this base and add their own marshall-bound helpers (e.g. {@code assertJson},
 * {@code json}, {@code validateXml}).
 *
 * <p>
 * This class lives in the {@code juneau-test-utils} module, which depends only on {@code juneau-commons} and JUnit, so
 * it never introduces a {@code juneau-marshall} dependency onto its consumers.
 */
public abstract class BasicTestUtils extends Shorts {

	private static final ThreadLocal<TimeZone> SYSTEM_TIME_ZONE = new ThreadLocal<>();

	/** Holds the previous default locale while a test overrides it via {@link #setLocale(Locale)}. */
	public static final ThreadLocal<Locale> SYSTEM_LOCALE = new ThreadLocal<>();

	/**
	 * Asserts that all the specified values are equal to the first value.
	 *
	 * @param values The values to compare.  Element 0 is the expected value.
	 */
	public static void assertEqualsAll(Object...values) {
		for (var i = 1; i < values.length; i++) {
			assertEquals(values[0], values[i], fs("Elements at index %1$s and %2$s did not match. %1$s=%3$s, %2$s=%4$s", 0, i, r(values[0]), r(values[i])));
		}
	}

	/**
	 * Asserts that the specified actual value does not equal any of the specified values.
	 *
	 * @param actual The actual value.  Must not be <jk>null</jk>.
	 * @param values The values that must not match the actual value.
	 */
	public static void assertNotEqualsAny(Object actual, Object...values) {
		assertNotNull(actual, "Value was null.");
		for (var i = 0; i < values.length; i++) {
			assertNotEquals(values[i], actual, fs("Element at index %s unexpectedly matched.  expected=%s, actual=%s", i, values[i], s(actual)));
		}
	}

	/**
	 * Asserts that the specified executable throws an exception whose chained messages contain all the specified substrings.
	 *
	 * @param <T> The expected exception type.
	 * @param expectedType The expected exception type.
	 * @param expectedSubstrings The substrings expected in the exception message chain.
	 * @param executable The code expected to throw.
	 * @return The thrown exception.
	 */
	public static <T extends Throwable> T assertThrowsWithMessage(Class<T> expectedType, List<String> expectedSubstrings, org.junit.jupiter.api.function.Executable executable) {
		var exception = Assertions.assertThrows(expectedType, executable);
		var messages = getMessages(exception);
		expectedSubstrings.forEach(x -> assertTrue(messages.contains(x), fs("Expected message to contain: %s.\nActual:\n%s", x, messages)));
		return exception;
	}

	/**
	 * Asserts that the specified executable throws an exception whose chained messages contain the specified substring.
	 *
	 * @param <T> The expected exception type.
	 * @param expectedType The expected exception type.
	 * @param expectedSubstring The substring expected in the exception message chain.
	 * @param executable The code expected to throw.
	 * @return The thrown exception.
	 */
	public static <T extends Throwable> T assertThrowsWithMessage(Class<T> expectedType, String expectedSubstring, org.junit.jupiter.api.function.Executable executable) {
		var exception = Assertions.assertThrows(expectedType, executable);
		var messages = getMessages(exception);
		assertTrue(messages.contains(expectedSubstring), fs("Expected message to contain: %s.\nActual:\n%s", expectedSubstring, messages));
		return exception;
	}

	/**
	 * Collects the newline-joined message chain of the specified throwable and all its causes.
	 *
	 * @param t The throwable.
	 * @return The joined message chain.
	 */
	protected static String getMessages(Throwable t) {
		return Stream.iterate(t, Throwable::getCause).takeWhile(e -> e != null).map(Throwable::getMessage).collect(joining("\n"));
	}

	/**
	 * Creates an input stream from the specified string.
	 *
	 * @param in The contents of the stream.
	 * @return A new input stream.
	 */
	public static final ByteArrayInputStream inputStream(String in) {
		return new ByteArrayInputStream(in.getBytes());
	}

	/**
	 * Creates a reader from the specified string.
	 *
	 * @param in The contents of the reader.
	 * @return A new reader.
	 */
	public static final StringReader reader(String in) {
		return new StringReader(in);
	}

	/**
	 * Temporarily sets the default system locale to the specified locale.
	 *
	 * @param v The new default locale.
	 */
	public static final void setLocale(Locale v) {
		SYSTEM_LOCALE.set(Locale.getDefault());
		Locale.setDefault(v);
	}

	/**
	 * Temporarily sets the default system timezone to the specified timezone ID.
	 *
	 * @param v The new default timezone ID.
	 */
	public static final synchronized void setTimeZone(String v) {
		SYSTEM_TIME_ZONE.set(TimeZone.getDefault());
		TimeZone.setDefault(TimeZone.getTimeZone(v));
	}

	/**
	 * Restores the default system locale saved by {@link #setLocale(Locale)}.
	 */
	public static final void unsetLocale() {
		Locale.setDefault(SYSTEM_LOCALE.get());
	}

	/**
	 * Restores the default system timezone saved by {@link #setTimeZone(String)}.
	 */
	public static final synchronized void unsetTimeZone() {
		TimeZone.setDefault(SYSTEM_TIME_ZONE.get());
	}

	/**
	 * Constructs a {@link URL} object from a string.
	 *
	 * @param value The URL string.
	 * @return A new {@link URL} object.
	 */
	public static URL url(String value) {
		return safe(()->new URI(value).toURL());
	}

	/**
	 * Returns an empty array of the specified type.
	 *
	 * @param <T> The component type.
	 * @param t The component type.
	 * @return A new empty array of the specified type.
	 */
	public static <T> T[] ea(Class<T> t) {
		return CollectionUtils.array(t, 0);
	}
}
