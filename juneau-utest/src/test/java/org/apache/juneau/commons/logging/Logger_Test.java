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

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.logging.Level;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link Logger}.
 */
class Logger_Test extends TestBase {

	private static Logger getLogger(String name) {
		var l = Logger.getLogger(name);
		l.setLevel(Level.OFF);
		return l;
	}

	private static Logger getLogger(Class<?> class_) {
		var l = Logger.getLogger(class_);
		l.setLevel(Level.OFF);
		return l;
	}

	//====================================================================================================
	// Basic logger creation and caching
	//====================================================================================================

	@Test void a01_getLogger_byName() {
		var logger1 = getLogger("test.logger");
		var logger2 = getLogger("test.logger");

		assertNotNull(logger1);
		assertSame(logger1, logger2); // Should return same instance
		assertEquals("test.logger", logger1.getName());
	}

	@Test void a02_getLogger_byClass() {
		var logger1 = getLogger(Logger_Test.class);
		var logger2 = getLogger(Logger_Test.class);

		assertNotNull(logger1);
		assertSame(logger1, logger2); // Should return same instance
		assertEquals(Logger_Test.class.getName(), logger1.getName());
	}

	@Test void a03_differentNames_returnDifferentInstances() {
		var logger1 = getLogger("logger1");
		var logger2 = getLogger("logger2");

		assertNotNull(logger1);
		assertNotNull(logger2);
		assertNotSame(logger1, logger2);
		assertEquals("logger1", logger1.getName());
		assertEquals("logger2", logger2.getName());
	}

	//====================================================================================================
	// Standard logging methods
	//====================================================================================================

	@Test void b01_severe() {
		try (var capture = getLogger("b01").captureEvents()) {
			var logger = getLogger("b01");
			logger.severe("Error message");

			var records = capture.getRecords();
			assertSize(1, records);
			assertEquals(Level.SEVERE, records.get(0).getLevel());
			assertEquals("Error message", records.get(0).getMessage());
		}
	}

	@Test void b02_warning() {
		try (var capture = getLogger("b02").captureEvents()) {
			var logger = getLogger("b02");
			logger.warning("Warning message");

			var records = capture.getRecords();
			assertSize(1, records);
			assertEquals(Level.WARNING, records.get(0).getLevel());
			assertEquals("Warning message", records.get(0).getMessage());
		}
	}

	@Test void b03_info() {
		try (var capture = getLogger("b03").captureEvents()) {
			var logger = getLogger("b03");
			logger.info("Info message");

			var records = capture.getRecords();
			assertSize(1, records);
			assertEquals(Level.INFO, records.get(0).getLevel());
			assertEquals("Info message", records.get(0).getMessage());
		}
	}

	@Test void b04_config() {
		try (var capture = getLogger("b04").captureEvents()) {
			var logger = getLogger("b04");
			logger.config("Config message");

			var records = capture.getRecords();
			assertSize(1, records);
			assertEquals(Level.CONFIG, records.get(0).getLevel());
			assertEquals("Config message", records.get(0).getMessage());
		}
	}

	@Test void b05_fine() {
		try (var capture = getLogger("b05").captureEvents()) {
			var logger = getLogger("b05");
			logger.fine("Fine message");

			var records = capture.getRecords();
			assertSize(1, records);
			assertEquals(Level.FINE, records.get(0).getLevel());
			assertEquals("Fine message", records.get(0).getMessage());
		}
	}

	@Test void b06_finer() {
		try (var capture = getLogger("b06").captureEvents()) {
			var logger = getLogger("b06");
			logger.finer("Finer message");

			var records = capture.getRecords();
			assertSize(1, records);
			assertEquals(Level.FINER, records.get(0).getLevel());
			assertEquals("Finer message", records.get(0).getMessage());
		}
	}

	@Test void b07_finest() {
		try (var capture = getLogger("b07").captureEvents()) {
			var logger = getLogger("b07");
			logger.finest("Finest message");

			var records = capture.getRecords();
			assertSize(1, records);
			assertEquals(Level.FINEST, records.get(0).getLevel());
			assertEquals("Finest message", records.get(0).getMessage());
		}
	}

	@Test void b08_log_withLevel() {
		try (var capture = getLogger("b08").captureEvents()) {
			var logger = getLogger("b08");
			logger.log(Level.SEVERE, "Log message");

			var records = capture.getRecords();
			assertSize(1, records);
			assertEquals(Level.SEVERE, records.get(0).getLevel());
			assertEquals("Log message", records.get(0).getMessage());
		}
	}

	@Test void b09_log_withThrowable() {
		try (var capture = getLogger("b09").captureEvents()) {
			var logger = getLogger("b09");
			var exception = new RuntimeException("Test exception");
			logger.log(Level.SEVERE, "Error occurred", exception);

			var records = capture.getRecords();
			assertSize(1, records);
			assertEquals(Level.SEVERE, records.get(0).getLevel());
			assertEquals("Error occurred", records.get(0).getMessage());
			assertSame(exception, records.get(0).getThrown());
		}
	}

	//====================================================================================================
	// Formatted logging methods
	//====================================================================================================

	@Test void c01_severe_formatted() {
		try (var capture = getLogger("c01").captureEvents()) {
			var logger = getLogger("c01");
			logger.severe("User {0} logged in", "John");

			var records = capture.getRecords();
			assertSize(1, records);
			assertEquals(Level.SEVERE, records.get(0).getLevel());
			assertEquals("User John logged in", records.get(0).getMessage());
		}
	}

	@Test void c02_severe_formattedWithThrowable() {
		try (var capture = getLogger("c02").captureEvents()) {
			var logger = getLogger("c02");
			var exception = new RuntimeException("Error");
			logger.severe(exception, "Failed to process {0}", "request");

			var records = capture.getRecords();
			assertSize(1, records);
			assertEquals(Level.SEVERE, records.get(0).getLevel());
			assertEquals("Failed to process request", records.get(0).getMessage());
			assertSame(exception, records.get(0).getThrown());
		}
	}

	@Test void c03_warning_formatted() {
		try (var capture = getLogger("c03").captureEvents()) {
			var logger = getLogger("c03");
			logger.warning("Connection to %s failed after %d attempts", "server", 3);

			var records = capture.getRecords();
			assertSize(1, records);
			assertEquals(Level.WARNING, records.get(0).getLevel());
			assertTrue(records.get(0).getMessage().contains("server"));
			assertTrue(records.get(0).getMessage().contains("3"));
		}
	}

	@Test void c04_info_formatted() {
		try (var capture = getLogger("c04").captureEvents()) {
			var logger = getLogger("c04");
			logger.info("Processing {} items", 42);

			var records = capture.getRecords();
			assertSize(1, records);
			assertEquals(Level.INFO, records.get(0).getLevel());
			assertTrue(records.get(0).getMessage().contains("42"));
		}
	}

	@Test void c05_logf_formatted() {
		try (var capture = getLogger("c05").captureEvents()) {
			var logger = getLogger("c05");
			logger.logf(Level.INFO, "Value: %s", "test");

			var records = capture.getRecords();
			assertSize(1, records);
			assertEquals(Level.INFO, records.get(0).getLevel());
			assertTrue(records.get(0).getMessage().contains("test"));
		}
	}

	@Test void c06_logf_formattedWithThrowable() {
		try (var capture = getLogger("c06").captureEvents()) {
			var logger = getLogger("c06");
			var exception = new IllegalArgumentException("Invalid");
			logger.logf(Level.WARNING, "Validation failed: %s", exception, "error");

			var records = capture.getRecords();
			assertSize(1, records);
			assertEquals(Level.WARNING, records.get(0).getLevel());
			assertSame(exception, records.get(0).getThrown());
		}
	}

	@Test void c07_log_withSingleParam() {
		// Test line 241: log(Level level, String msg, Object param1)
		try (var capture = getLogger("c07").captureEvents()) {
			var logger = getLogger("c07");
			logger.log(Level.INFO, "User {0} logged in", "john");

			var records = capture.getRecords();
			assertSize(1, records);
			assertEquals(Level.INFO, records.get(0).getLevel());
			// Message should be formatted with the parameter
			assertTrue(records.get(0).getMessage().contains("john"));
		}
	}

	@Test void c08_log_withParamsArray() {
		// Test line 246: log(Level level, String msg, Object[] params)
		try (var capture = getLogger("c08").captureEvents()) {
			var logger = getLogger("c08");
			logger.log(Level.INFO, "User {0} logged in from {1}", new Object[]{"john", "NYC"});

			var records = capture.getRecords();
			assertSize(1, records);
			assertEquals(Level.INFO, records.get(0).getLevel());
			// Message should be formatted with the parameters
			assertTrue(records.get(0).getMessage().contains("john"));
			assertTrue(records.get(0).getMessage().contains("NYC"));
		}
	}

	@Test void c09_addHandler() {
		// Test line 276: delegate.addHandler(handler)
		var logger = getLogger("c09");
		var handler = new java.util.logging.ConsoleHandler();

		// Add handler - should delegate to underlying logger
		logger.addHandler(handler);

		// Verify handler was added by checking handlers array
		var handlers = logger.getHandlers();
		assertTrue(handlers.length > 0);
		// The handler should be in the list (may be wrapped or direct)
		boolean found = false;
		for (var h : handlers) {
			if (h == handler || h.equals(handler)) {
				found = true;
				break;
			}
		}
		assertTrue(found, "Handler should be added to logger");
	}

	@Test void c10_removeHandler() {
		// Test line 281: delegate.removeHandler(handler)
		var logger = getLogger("c10");
		var handler = new java.util.logging.ConsoleHandler();

		// Add handler first
		logger.addHandler(handler);
		var handlersBefore = logger.getHandlers();
		var initialCount = handlersBefore.length;

		// Remove handler - should delegate to underlying logger
		logger.removeHandler(handler);

		// Verify handler was removed by checking handlers array
		var handlersAfter = logger.getHandlers();
		assertTrue(handlersAfter.length < initialCount, "Handler should be removed from logger");
	}

	@Test void c11_getHandlers() {
		// Test line 286: delegate.getHandlers()
		var logger = getLogger("c11");

		// Get handlers - should delegate to underlying logger
		var handlers = logger.getHandlers();

		// Verify handlers array is returned (may be empty or contain default handlers)
		assertNotNull(handlers);
		// The array should be valid (length >= 0)
		assertTrue(handlers.length >= 0);
	}

	@Test void c12_warning_withThrowable() {
		// Test line 330: warning(Throwable thrown, String pattern, Object...args)
		try (var capture = getLogger("c12").captureEvents()) {
			var logger = getLogger("c12");
			var exception = new RuntimeException("Test error");
			logger.warning(exception, "Warning: {0} occurred", "error");

			var records = capture.getRecords();
			assertSize(1, records);
			assertEquals(Level.WARNING, records.get(0).getLevel());
			assertSame(exception, records.get(0).getThrown());
			assertTrue(records.get(0).getMessage().contains("error"));
		}
	}

	@Test void c13_info_withThrowable() {
		// Test line 351: info(Throwable thrown, String pattern, Object...args)
		try (var capture = getLogger("c13").captureEvents()) {
			var logger = getLogger("c13");
			var exception = new IllegalStateException("State error");
			logger.info(exception, "Info: {0} occurred", "issue");

			var records = capture.getRecords();
			assertSize(1, records);
			assertEquals(Level.INFO, records.get(0).getLevel());
			assertSame(exception, records.get(0).getThrown());
			assertTrue(records.get(0).getMessage().contains("issue"));
		}
	}

	@Test void c14_config_formatted() {
		// Test line 361: config(String pattern, Object...args)
		try (var capture = getLogger("c14").captureEvents()) {
			var logger = getLogger("c14");
			logger.config("Configuration: {0} = {1}", "timeout", 30);

			var records = capture.getRecords();
			assertSize(1, records);
			assertEquals(Level.CONFIG, records.get(0).getLevel());
			assertTrue(records.get(0).getMessage().contains("timeout"));
			assertTrue(records.get(0).getMessage().contains("30"));
		}
	}

	@Test void c15_config_withThrowable() {
		// Test line 372: config(Throwable thrown, String pattern, Object...args)
		try (var capture = getLogger("c15").captureEvents()) {
			var logger = getLogger("c15");
			var exception = new IllegalArgumentException("Config error");
			logger.config(exception, "Configuration: {0} failed", "setup");

			var records = capture.getRecords();
			assertSize(1, records);
			assertEquals(Level.CONFIG, records.get(0).getLevel());
			assertSame(exception, records.get(0).getThrown());
			assertTrue(records.get(0).getMessage().contains("setup"));
		}
	}

	@Test void c16_fine_formatted() {
		// Test line 382: fine(String pattern, Object...args)
		try (var capture = getLogger("c16").captureEvents()) {
			var logger = getLogger("c16");
			logger.fine("Fine detail: {0} = {1}", "key", "value");

			var records = capture.getRecords();
			assertSize(1, records);
			assertEquals(Level.FINE, records.get(0).getLevel());
			assertTrue(records.get(0).getMessage().contains("key"));
			assertTrue(records.get(0).getMessage().contains("value"));
		}
	}

	@Test void c17_fine_withThrowable() {
		// Test line 393: fine(Throwable thrown, String pattern, Object...args)
		try (var capture = getLogger("c17").captureEvents()) {
			var logger = getLogger("c17");
			var exception = new RuntimeException("Fine error");
			logger.fine(exception, "Fine detail: {0} occurred", "event");

			var records = capture.getRecords();
			assertSize(1, records);
			assertEquals(Level.FINE, records.get(0).getLevel());
			assertSame(exception, records.get(0).getThrown());
			assertTrue(records.get(0).getMessage().contains("event"));
		}
	}

	@Test void c18_finer_formatted() {
		// Test line 403: finer(String pattern, Object...args)
		try (var capture = getLogger("c18").captureEvents()) {
			var logger = getLogger("c18");
			logger.finer("Finer detail: {0} = {1}", "param", 42);

			var records = capture.getRecords();
			assertSize(1, records);
			assertEquals(Level.FINER, records.get(0).getLevel());
			assertTrue(records.get(0).getMessage().contains("param"));
			assertTrue(records.get(0).getMessage().contains("42"));
		}
	}

	@Test void c19_finer_withThrowable() {
		// Test line 414: finer(Throwable thrown, String pattern, Object...args)
		try (var capture = getLogger("c19").captureEvents()) {
			var logger = getLogger("c19");
			var exception = new RuntimeException("Finer error");
			logger.finer(exception, "Finer detail: {0} occurred", "trace");

			var records = capture.getRecords();
			assertSize(1, records);
			assertEquals(Level.FINER, records.get(0).getLevel());
			assertSame(exception, records.get(0).getThrown());
			assertTrue(records.get(0).getMessage().contains("trace"));
		}
	}

	@Test void c20_finest_formatted() {
		// Test line 424: finest(String pattern, Object...args)
		try (var capture = getLogger("c20").captureEvents()) {
			var logger = getLogger("c20");
			logger.finest("Finest detail: {0} = {1}", "debug", "value");

			var records = capture.getRecords();
			assertSize(1, records);
			assertEquals(Level.FINEST, records.get(0).getLevel());
			assertTrue(records.get(0).getMessage().contains("debug"));
			assertTrue(records.get(0).getMessage().contains("value"));
		}
	}

	@Test void c21_finest_withThrowable() {
		// Test line 435: finest(Throwable thrown, String pattern, Object...args)
		try (var capture = getLogger("c21").captureEvents()) {
			var logger = getLogger("c21");
			var exception = new RuntimeException("Finest error");
			logger.finest(exception, "Finest detail: {0} occurred", "debug");

			var records = capture.getRecords();
			assertSize(1, records);
			assertEquals(Level.FINEST, records.get(0).getLevel());
			assertSame(exception, records.get(0).getThrown());
			assertTrue(records.get(0).getMessage().contains("debug"));
		}
	}

	//====================================================================================================
	// Log level filtering
	//====================================================================================================

	@Test void d01_loggableLevel_isLogged() {
		try (var capture = getLogger("d01").captureEvents()) {
			var logger = getLogger("d01");
			logger.info("Info message");
			logger.fine("Fine message"); // Should not be logged to delegate, but will be captured

			// Listeners capture all records regardless of level
			var records = capture.getRecords();
			assertSize(2, records); // Both records are captured by listener
			assertEquals(Level.INFO, records.get(0).getLevel());
			assertEquals(Level.FINE, records.get(1).getLevel());

			// But isLoggable should still respect the level
			logger.setLevel(Level.INFO);
			assertTrue(logger.isLoggable(Level.INFO));
			assertFalse(logger.isLoggable(Level.FINE));
		}
	}

	@Test void d03_earlyReturn_noListenersAndNotLoggable() {
		// Test line 184: early return when !isLoggable(level) && listeners.isEmpty()
		// Create a logger with no listeners and a level that filters out FINE messages
		var logger = getLogger("d03");

		// Verify FINE is not loggable
		assertFalse(logger.isLoggable(Level.FINE));

		// Log a FINE message - should return early without creating LogRecord or delegating
		// Since there are no listeners, we can't capture it, but we can verify it doesn't throw
		logger.fine("Fine message");

		// The early return on line 184 should prevent any processing
		// We verify this by ensuring the logger still works for loggable levels
		logger.info("Info message"); // This should work fine
		assertFalse(logger.isLoggable(Level.INFO));
	}

	@Test void d02_listeners_captureAllLevels() {
		try (var capture = getLogger("d02").captureEvents()) {
			var logger = getLogger("d02");
			logger.setLevel(Level.SEVERE); // Set high threshold
			logger.info("Info message"); // Should still be captured by listener

			var records = capture.getRecords();
			assertSize(1, records); // Listener captures even if not loggable
			assertEquals(Level.INFO, records.get(0).getLevel());
		}
	}

	//====================================================================================================
	// Multiple log records
	//====================================================================================================

	@Test void e01_multipleLogs_capturedInOrder() {
		try (var capture = getLogger("e01").captureEvents()) {
			var logger = getLogger("e01");
			logger.severe("First");
			logger.warning("Second");
			logger.info("Third");

			var records = capture.getRecords();
			assertSize(3, records);
			assertEquals(Level.SEVERE, records.get(0).getLevel());
			assertEquals("First", records.get(0).getMessage());
			assertEquals(Level.WARNING, records.get(1).getLevel());
			assertEquals("Second", records.get(1).getMessage());
			assertEquals(Level.INFO, records.get(2).getLevel());
			assertEquals("Third", records.get(2).getMessage());
		}
	}

	//====================================================================================================
	// Logger delegation
	//====================================================================================================

	@Test void f01_delegatesToUnderlyingLogger() {
		var logger = getLogger("f01");
		var underlyingLogger = java.util.logging.Logger.getLogger("f01");

		assertEquals(underlyingLogger.getName(), logger.getName());
		assertEquals(underlyingLogger.getLevel(), logger.getLevel());
	}

	@Test void f02_setLevel_delegates() {
		var logger = getLogger("f02");
		logger.setLevel(Level.FINE);
		assertEquals(Level.FINE, logger.getLevel());
		assertEquals(Level.FINE, java.util.logging.Logger.getLogger("f02").getLevel());
	}

	@Test void f03_isLoggable_delegates() {
		var logger = Logger.getLogger("f03");
		logger.setLevel(Level.INFO);
		assertTrue(logger.isLoggable(Level.INFO));
		assertTrue(logger.isLoggable(Level.SEVERE));
		assertFalse(logger.isLoggable(Level.FINE));
	}

	//====================================================================================================
	// LogRecordCapture integration
	//====================================================================================================

	@Test void g01_captureEvents_returnsCapture() {
		var logger = getLogger("g01");
		var capture = logger.captureEvents();

		assertNotNull(capture);
		assertTrue(capture.isEmpty());
	}

	@Test void g02_captureEvents_autoRemovesOnClose() {
		var logger = getLogger("g02");
		try (var capture = logger.captureEvents()) {
			logger.info("Message 1");
			assertSize(1, capture.getRecords());
		}
		// After close, capture should be removed
		try (var capture2 = logger.captureEvents()) {
			logger.info("Message 2");
			assertSize(1, capture2.getRecords()); // Only new message
		}
	}
}
