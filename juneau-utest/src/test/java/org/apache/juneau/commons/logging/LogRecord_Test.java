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
package org.apache.juneau.commons.logging;

import static org.junit.jupiter.api.Assertions.*;

import java.util.logging.Level;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link LogRecord}.
 */
class LogRecord_Test extends TestBase {

	private Logger getLogger(String name) {
		var l = Logger.getLogger(name);
		l.setLevel(Level.OFF);
		return l;
	}

	//====================================================================================================
	// Constructor and basic properties
	//====================================================================================================

	@Test void a01_constructor_simpleMessage() {
		var record = new LogRecord("test.logger", Level.INFO, "Test message", null, null);

		assertEquals("test.logger", record.getLoggerName());
		assertEquals(Level.INFO, record.getLevel());
		assertEquals("Test message", record.getMessage());
		assertNull(record.getThrown());
		assertNull(record.getParameters());
	}

	@Test void a02_constructor_withParameters() {
		var record = new LogRecord("test.logger", Level.INFO, "User {0}", new Object[]{"John"}, null);

		assertEquals("test.logger", record.getLoggerName());
		assertEquals(Level.INFO, record.getLevel());
		assertEquals("User John", record.getMessage()); // Should be formatted
		assertNull(record.getThrown());
		assertNotNull(record.getParameters());
		assertEquals(1, record.getParameters().length);
	}

	@Test void a03_constructor_withThrowable() {
		var exception = new RuntimeException("Test error");
		var record = new LogRecord("test.logger", Level.SEVERE, "Error occurred", null, exception);

		assertEquals("test.logger", record.getLoggerName());
		assertEquals(Level.SEVERE, record.getLevel());
		assertEquals("Error occurred", record.getMessage());
		assertSame(exception, record.getThrown());
	}

	@Test void a04_constructor_emptyArgsArray() {
		var record = new LogRecord("test.logger", Level.INFO, "Message", new Object[0], null);

		assertEquals("Message", record.getMessage());
		assertNull(record.getParameters()); // Empty array should result in null
	}

	//====================================================================================================
	// Lazy message formatting
	//====================================================================================================

	@Test void b01_getMessage_formatsLazily() {
		var record = new LogRecord("test.logger", Level.INFO, "Value: {0}", new Object[]{42}, null);

		// Message should be formatted when accessed
		var message = record.getMessage();
		assertEquals("Value: 42", message);

		// Should return same formatted message on subsequent calls
		assertEquals("Value: 42", record.getMessage());
	}

	@Test void b02_getMessage_withMultipleArgs() {
		var record = new LogRecord("test.logger", Level.INFO, "{0} + {1} = {2}",
			new Object[]{1, 2, 3}, null);

		assertEquals("1 + 2 = 3", record.getMessage());
	}

	@Test void b03_getMessage_withNullArgs() {
		var record = new LogRecord("test.logger", Level.INFO, "Message", null, null);

		assertEquals("Message", record.getMessage());
		assertNull(record.getParameters());
	}

	//====================================================================================================
	// Source class and method name calculation
	//====================================================================================================

	@Test void c01_getSourceClassName_calculatedLazily() {
		// Create LogRecord through Logger to get proper call stack
		// findSource() filters out LogRecord, Logger, and lambda methods, so test classes are included
		try (var capture = getLogger("c01").captureEvents()) {
			var logger = getLogger("c01");
			logger.info("Message");

			var record = capture.getRecords().get(0);
			var className = record.getSourceClassName();
			// Should be calculated from stack trace
			assertNotNull(className);
			assertTrue(className.contains("LogRecord_Test")); // Should be this test class
		}
	}

	@Test void c02_getSourceMethodName_calculatedLazily() {
		// Create LogRecord through Logger to get proper call stack
		// findSource() filters out LogRecord, Logger, and lambda methods, so test methods are included
		try (var capture = getLogger("c02").captureEvents()) {
			var logger = getLogger("c02");
			logger.info("Message");

			var record = capture.getRecords().get(0);
			var methodName = record.getSourceMethodName();
			// Should be calculated from stack trace
			assertNotNull(methodName);
			assertEquals("c02_getSourceMethodName_calculatedLazily", methodName);
		}
	}

	@Test void c03_source_cachedAfterFirstAccess() {
		// Create LogRecord through Logger to get proper call stack
		try (var capture = getLogger("c03").captureEvents()) {
			var logger = getLogger("c03");
			logger.info("Message");

			var record = capture.getRecords().get(0);
			var className1 = record.getSourceClassName();
			var className2 = record.getSourceClassName();
			var methodName1 = record.getSourceMethodName();
			var methodName2 = record.getSourceMethodName();

			// Should return same values (cached)
			assertEquals(className1, className2);
			assertEquals(methodName1, methodName2);
		}
	}

	//====================================================================================================
	// formatted() method - named placeholders
	//====================================================================================================

	@Test void d01_formatted_namedPlaceholders() {
		var record = new LogRecord("test.logger", Level.INFO, "User logged in", null, null);

		var formatted = record.formatted("{level}: {msg}");
		assertTrue(formatted.contains("INFO"));
		assertTrue(formatted.contains("User logged in"));
	}

	@Test void d02_formatted_timestamp() {
		var record = new LogRecord("test.logger", Level.INFO, "Message", null, null);

		var formatted = record.formatted("{timestamp}");
		// Should be ISO8601 format
		assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{4}"));
	}

	@Test void d03_formatted_date() {
		var record = new LogRecord("test.logger", Level.INFO, "Message", null, null);

		var formatted = record.formatted("{date}");
		// Should be formatted date (not null)
		assertNotNull(formatted);
		assertFalse(formatted.isEmpty());
	}

	@Test void d04_formatted_classAndMethod() {
		// Create LogRecord through Logger to get proper call stack
		// findSource() filters out LogRecord, Logger, and lambda methods, so test classes are included
		try (var capture = getLogger("d04").captureEvents()) {
			var logger = getLogger("d04");
			logger.info("Message");

			var record = capture.getRecords().get(0);
			var formatted = record.formatted("{class}.{method}");
			assertNotNull(formatted);
			assertTrue(formatted.contains("LogRecord_Test"));
			assertTrue(formatted.contains("d04_formatted_classAndMethod"));
		}
	}

	@Test void d05_formatted_logger() {
		var record = new LogRecord("test.logger", Level.INFO, "Message", null, null);

		var formatted = record.formatted("{logger}");
		assertEquals("test.logger", formatted);
	}

	@Test void d06_formatted_level() {
		var record = new LogRecord("test.logger", Level.SEVERE, "Message", null, null);

		var formatted = record.formatted("{level}");
		assertEquals("SEVERE", formatted);
	}

	@Test void d07_formatted_msg() {
		var record = new LogRecord("test.logger", Level.INFO, "Test {0}", new Object[]{"value"}, null);

		var formatted = record.formatted("{msg}");
		assertEquals("Test value", formatted);
	}

	@Test void d08_formatted_thread() {
		var record = new LogRecord("test.logger", Level.INFO, "Message", null, null);

		var formatted = record.formatted("{thread}");
		assertNotNull(formatted);
		assertFalse(formatted.isEmpty());
	}

	@Test void d09_formatted_exception() {
		var exception = new RuntimeException("Test error");
		var record = new LogRecord("test.logger", Level.SEVERE, "Error", null, exception);

		var formatted = record.formatted("{exception}");
		assertEquals("Test error", formatted);
	}

	@Test void d10_formatted_exceptionWithoutThrowable() {
		var record = new LogRecord("test.logger", Level.INFO, "Message", null, null);

		var formatted = record.formatted("{exception}");
		// When there's no exception, {exception} is replaced with empty string
		assertEquals("", formatted);
	}

	@Test void d11_formatted_thrown() {
		var exception = new RuntimeException("Test error");
		var record = new LogRecord("test.logger", Level.SEVERE, "Error", null, exception);

		var formatted = record.formatted("{thrown}");
		assertNotNull(formatted);
		assertTrue(formatted.contains("RuntimeException"));
	}

	@Test void d12_formatted_source() {
		// Create LogRecord through Logger to get proper call stack
		// findSource() now only filters out LogRecord and Logger classes, so test classes are included
		try (var capture = getLogger("d12").captureEvents()) {
			var logger = getLogger("d12");
			logger.info("Message");

			var record = capture.getRecords().get(0);
			var formatted = record.formatted("{source}");
			assertNotNull(formatted);
			// The {source} placeholder resolves to %2$s which formats the source supplier
			// The source supplier returns "className methodName"
			assertTrue(formatted.contains("LogRecord_Test"));
			assertTrue(formatted.contains("d12_formatted_source"));
		}
	}

	//====================================================================================================
	// formatted() method - Formatter-style specifiers
	//====================================================================================================

	@Test void e01_formatted_formatterSpecifiers() {
		var record = new LogRecord("test.logger", Level.INFO, "Message", null, null);

		var formatted = record.formatted("%4$s: %5$s");
		assertTrue(formatted.contains("INFO"));
		assertTrue(formatted.contains("Message"));
	}

	@Test void e02_formatted_unknownPlaceholder() {
		var record = new LogRecord("test.logger", Level.INFO, "Message", null, null);

		// Unknown placeholder - resolver returns "null" (default case on line 246)
		// formatNamed replaces {unknown} with "null" when resolver returns "null"
		// This verifies that the default case (line 246) is executed
		var formatted = record.formatted("Test {unknown} placeholder");
		// The unknown placeholder should be replaced with "null"
		assertNotNull(formatted);
		assertTrue(formatted.contains("Test"));
		assertTrue(formatted.contains("placeholder"));
	}

	@Test void e03_formatted_dateTimeSpecifiers() {
		var record = new LogRecord("test.logger", Level.INFO, "Message", null, null);

		var formatted = record.formatted("%1$tc");
		// Should be formatted date/time
		assertNotNull(formatted);
		assertFalse(formatted.isEmpty());
	}

	@Test void e03_formatted_mixedPlaceholders() {
		var record = new LogRecord("test.logger", Level.INFO, "Test {0}", new Object[]{"value"}, null);

		var formatted = record.formatted("%1$tb %1$td, %1$tY {level}: {msg}");
		assertTrue(formatted.contains("INFO"));
		assertTrue(formatted.contains("Test value"));
	}

	//====================================================================================================
	// formatted() method - edge cases
	//====================================================================================================

	@Test void f01_formatted_withNewlines() {
		var record = new LogRecord("test.logger", Level.INFO, "Message", null, null);

		var formatted = record.formatted("{level}%n{msg}");
		assertTrue(formatted.contains("\n"));
		assertTrue(formatted.contains("INFO"));
		assertTrue(formatted.contains("Message"));
	}

	@Test void f02_formatted_complexFormat() {
		var exception = new RuntimeException("Error");
		var record = new LogRecord("test.logger", Level.SEVERE, "Failed: {0}",
			new Object[]{"operation"}, exception);

		var formatted = record.formatted("[{timestamp}] {level}: {msg}%n{exception}");
		assertTrue(formatted.contains("SEVERE"));
		assertTrue(formatted.contains("Failed: operation"));
		assertTrue(formatted.contains("Error"));
		assertTrue(formatted.contains("\n"));
	}
}
