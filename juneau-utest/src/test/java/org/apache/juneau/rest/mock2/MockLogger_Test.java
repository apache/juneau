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
package org.apache.juneau.rest.mock2;

import static org.junit.jupiter.api.Assertions.*;

import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

class MockLogger_Test extends TestBase {

	@Test void a01_create() {
		assertNotNull(MockLogger.create());
		assertNotNull(new MockLogger());
	}

	@Test void a02_log_toString() {
		var logger = new MockLogger();
		logger.log(new LogRecord(Level.INFO, "Hello World"));
		assertTrue(logger.toString().contains("Hello World"));
	}

	@Test void a03_assertContents() {
		var logger = new MockLogger();
		logger.log(new LogRecord(Level.INFO, "test message"));
		logger.assertContents().isContains("test message");
	}

	@Test void a04_assertLastLevel_success() {
		var logger = new MockLogger();
		logger.log(new LogRecord(Level.WARNING, "warn msg"));
		logger.assertLastLevel(Level.WARNING);
	}

	@Test void a05_assertLastLevel_failure() {
		var logger = new MockLogger();
		logger.log(new LogRecord(Level.INFO, "info msg"));
		assertThrows(AssertionError.class, () -> logger.assertLastLevel(Level.WARNING));
	}

	@Test void a06_assertLastMessage() {
		var logger = new MockLogger();
		logger.log(new LogRecord(Level.INFO, "my message"));
		logger.assertLastMessage().isContains("my message");
	}

	@Test void a07_assertLogged_success() {
		var logger = new MockLogger();
		logger.log(new LogRecord(Level.INFO, "logged"));
		logger.assertLogged();
	}

	@Test void a08_assertLogged_failure() {
		var logger = new MockLogger();
		assertThrows(AssertionError.class, () -> logger.assertLogged());
	}

	@Test void a09_assertRecordCount() {
		var logger = new MockLogger();
		logger.assertRecordCount().is(0);
		logger.log(new LogRecord(Level.INFO, "one"));
		logger.assertRecordCount().is(1);
		logger.log(new LogRecord(Level.INFO, "two"));
		logger.assertRecordCount().is(2);
	}

	@Test void a10_format() {
		var logger = new MockLogger();
		logger.format("%5$s%n");
		logger.log(new LogRecord(Level.INFO, "formatted message"));
		assertTrue(logger.toString().contains("formatted message"));
	}

	@Test void a11_formatter() {
		var logger = new MockLogger();
		logger.formatter(new Formatter() {
			@Override
			public String format(LogRecord record) {
				return "CUSTOM:" + record.getMessage();
			}
		});
		logger.log(new LogRecord(Level.INFO, "custom"));
		assertTrue(logger.toString().contains("CUSTOM:custom"));
	}

	@Test void a12_level() {
		var logger = new MockLogger();
		logger.level(Level.SEVERE);
		assertEquals(Level.SEVERE, logger.getLevel());
	}

	@Test void a13_reset() {
		var logger = new MockLogger();
		logger.log(new LogRecord(Level.INFO, "before reset"));
		logger.assertRecordCount().is(1);
		logger.reset();
		logger.assertRecordCount().is(0);
		assertEquals("", logger.toString());
		logger.log(new LogRecord(Level.INFO, "after reset"));
		logger.assertRecordCount().is(1);
		assertTrue(logger.toString().contains("after reset"));
	}

	@Test void a14_getFormatter_withExistingSystemProperty() {
		// Test the branch where FORMAT_PROPERTY is already set before getFormatter() runs,
		// so the else branch (restore old value) is exercised.
		var key = "java.util.logging.SimpleFormatter.format";
		var existing = System.getProperty(key);
		System.setProperty(key, "%5$s%n");
		try {
			var logger = new MockLogger();
			logger.log(new LogRecord(Level.INFO, "system prop test"));
			assertTrue(logger.toString().contains("system prop test"));
		} finally {
			if (existing == null)
				System.clearProperty(key);
			else
				System.setProperty(key, existing);
		}
	}
}
