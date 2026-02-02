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
 * Tests for {@link LogRecordCapture}.
 */
class LogRecordCapture_Test extends TestBase {

	private static Logger getLogger(String name) {
		var l = Logger.getLogger(name);
		l.setLevel(Level.OFF);
		return l;
	}

	//====================================================================================================
	// Basic capture functionality
	//====================================================================================================

	@Test void a01_capture_singleRecord() {
		var logger = getLogger("a01");
		try (var capture = logger.captureEvents()) {
			logger.info("Test message");

			var records = capture.getRecords();
			assertSize(1, records);
			assertEquals(Level.INFO, records.get(0).getLevel());
			assertEquals("Test message", records.get(0).getMessage());
		}
	}

	@Test void a02_capture_multipleRecords() {
		var logger = getLogger("a02");
		try (var capture = logger.captureEvents()) {
			logger.severe("Error");
			logger.warning("Warning");
			logger.info("Info");

			var records = capture.getRecords();
			assertSize(3, records);
			assertEquals(Level.SEVERE, records.get(0).getLevel());
			assertEquals(Level.WARNING, records.get(1).getLevel());
			assertEquals(Level.INFO, records.get(2).getLevel());
		}
	}

	@Test void a03_capture_initiallyEmpty() {
		var logger = getLogger("a03");
		try (var capture = logger.captureEvents()) {
			assertTrue(capture.isEmpty());
			assertEquals(0, capture.size());
		}
	}

	//====================================================================================================
	// getRecords() method
	//====================================================================================================

	@Test void b01_getRecords_returnsUnmodifiableList() {
		var logger = getLogger("b01");
		try (var capture = logger.captureEvents()) {
			logger.info("Message");

			var records = capture.getRecords();
			assertSize(1, records);

			// Should be unmodifiable
			assertThrows(UnsupportedOperationException.class, () -> {
				records.add(new LogRecord("test", Level.INFO, "test", null, null));
			});
		}
	}

	@Test void b02_getRecords_returnsCopy() {
		var logger = getLogger("b02");
		try (var capture = logger.captureEvents()) {
			logger.info("Message 1");

			var records1 = capture.getRecords();
			logger.info("Message 2");
			var records2 = capture.getRecords();

			// Each call should return a new copy
			assertSize(1, records1);
			assertSize(2, records2);
		}
	}

	//====================================================================================================
	// getRecords(String format) method
	//====================================================================================================

	@Test void c01_getRecords_withFormat() {
		var logger = getLogger("c01");
		try (var capture = logger.captureEvents()) {
			logger.info("Test message");

			var formatted = capture.getRecords("{level}: {msg}");
			assertSize(1, formatted);
			assertTrue(formatted.get(0).contains("INFO"));
			assertTrue(formatted.get(0).contains("Test message"));
		}
	}

	@Test void c02_getRecords_withFormat_multipleRecords() {
		var logger = getLogger("c02");
		try (var capture = logger.captureEvents()) {
			logger.severe("Error");
			logger.warning("Warning");

			var formatted = capture.getRecords("{level}");
			assertSize(2, formatted);
			assertTrue(formatted.get(0).contains("SEVERE"));
			assertTrue(formatted.get(1).contains("WARNING"));
		}
	}

	@Test void c03_getRecords_withFormatterSpecifiers() {
		var logger = getLogger("c03");
		try (var capture = logger.captureEvents()) {
			logger.info("Message");

			var formatted = capture.getRecords("%4$s: %5$s");
			assertSize(1, formatted);
			assertTrue(formatted.get(0).contains("INFO"));
			assertTrue(formatted.get(0).contains("Message"));
		}
	}

	//====================================================================================================
	// clear() method
	//====================================================================================================

	@Test void d01_clear_removesAllRecords() {
		var logger = getLogger("d01");
		try (var capture = logger.captureEvents()) {
			logger.info("Message 1");
			logger.info("Message 2");

			assertSize(2, capture.getRecords());
			capture.clear();
			assertTrue(capture.isEmpty());
			assertEquals(0, capture.size());
		}
	}

	@Test void d02_clear_afterClear_newRecordsCaptured() {
		var logger = getLogger("d02");
		try (var capture = logger.captureEvents()) {
			logger.info("Message 1");
			capture.clear();
			logger.info("Message 2");

			var records = capture.getRecords();
			assertSize(1, records);
			assertEquals("Message 2", records.get(0).getMessage());
		}
	}

	//====================================================================================================
	// size() and isEmpty() methods
	//====================================================================================================

	@Test void e01_size_returnsCorrectCount() {
		var logger = getLogger("e01");
		try (var capture = logger.captureEvents()) {
			assertEquals(0, capture.size());
			logger.info("Message 1");
			assertEquals(1, capture.size());
			logger.info("Message 2");
			assertEquals(2, capture.size());
		}
	}

	@Test void e02_isEmpty_initiallyTrue() {
		var logger = getLogger("e02");
		try (var capture = logger.captureEvents()) {
			assertTrue(capture.isEmpty());
		}
	}

	@Test void e03_isEmpty_afterLogging() {
		var logger = getLogger("e03");
		try (var capture = logger.captureEvents()) {
			logger.info("Message");
			assertFalse(capture.isEmpty());
		}
	}

	//====================================================================================================
	// close() method - auto-removal
	//====================================================================================================

	@Test void f01_close_removesListener() {
		var logger = getLogger("f01");
		LogRecordCapture capture = null;
		try {
			capture = logger.captureEvents();
			logger.info("Message 1");
			assertSize(1, capture.getRecords());
		} finally {
			if (capture != null) {
				capture.close();
			}
		}

		// After close, new capture should only see new messages
		try (var capture2 = logger.captureEvents()) {
			logger.info("Message 2");
			assertSize(1, capture2.getRecords());
			assertEquals("Message 2", capture2.getRecords().get(0).getMessage());
		}
	}

	@Test void f02_close_tryWithResources() {
		var logger = getLogger("f02");
		try (var capture = logger.captureEvents()) {
			logger.info("Message");
			assertSize(1, capture.getRecords());
		}
		// Capture should be closed automatically
		// Verify by creating new capture
		try (var capture2 = logger.captureEvents()) {
			logger.info("New message");
			assertSize(1, capture2.getRecords());
		}
	}

	//====================================================================================================
	// Thread safety
	//====================================================================================================

	@Test void g01_concurrentLogging_capturesAll() throws InterruptedException {
		var logger = getLogger("g01");
		try (var capture = logger.captureEvents()) {
			var thread1 = new Thread(() -> logger.info("Thread 1"));
			var thread2 = new Thread(() -> logger.info("Thread 2"));
			var thread3 = new Thread(() -> logger.info("Thread 3"));

			thread1.start();
			thread2.start();
			thread3.start();

			thread1.join();
			thread2.join();
			thread3.join();

			// All messages should be captured
			var records = capture.getRecords();
			assertEquals(3, records.size());
		}
	}

	//====================================================================================================
	// Integration with Logger
	//====================================================================================================

	@Test void h01_capturesAllLevels() {
		var logger = getLogger("h01");
		try (var capture = logger.captureEvents()) {
			logger.severe("Severe");
			logger.warning("Warning");
			logger.info("Info");
			logger.config("Config");
			logger.fine("Fine");
			logger.finer("Finer");
			logger.finest("Finest");

			var records = capture.getRecords();
			assertSize(7, records);
			assertEquals(Level.SEVERE, records.get(0).getLevel());
			assertEquals(Level.WARNING, records.get(1).getLevel());
			assertEquals(Level.INFO, records.get(2).getLevel());
			assertEquals(Level.CONFIG, records.get(3).getLevel());
			assertEquals(Level.FINE, records.get(4).getLevel());
			assertEquals(Level.FINER, records.get(5).getLevel());
			assertEquals(Level.FINEST, records.get(6).getLevel());
		}
	}

	@Test void h02_capturesFormattedMessages() {
		var logger = getLogger("h02");
		try (var capture = logger.captureEvents()) {
			logger.info("User {0} logged in", "John");
			logger.warning("Failed after %d attempts", 3);

			var records = capture.getRecords();
			assertSize(2, records);
			assertEquals("User John logged in", records.get(0).getMessage());
			assertTrue(records.get(1).getMessage().contains("3"));
		}
	}

	@Test void h03_capturesThrowables() {
		var logger = getLogger("h03");
		try (var capture = logger.captureEvents()) {
			var exception = new RuntimeException("Test error");
			logger.severe(exception, "Error occurred");

			var records = capture.getRecords();
			assertSize(1, records);
			assertSame(exception, records.get(0).getThrown());
		}
	}
}
