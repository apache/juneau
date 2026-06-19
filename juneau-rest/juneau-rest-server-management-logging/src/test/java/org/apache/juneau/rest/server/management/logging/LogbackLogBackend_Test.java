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
package org.apache.juneau.rest.server.management.logging;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.rest.server.management.*;
import org.junit.jupiter.api.*;

import ch.qos.logback.classic.*;

/**
 * Tests for {@link LogbackLogBackend} driven against an explicit Logback {@link LoggerContext}, verifying it
 * honors the {@link LogBackend} contract (configured-level reads, empty-string-inherited, process-lifetime sets,
 * "ROOT" alias, hard-fail on bad level).
 */
class LogbackLogBackend_Test extends org.apache.juneau.TestBase {

	private static final String LNAME = "org.apache.juneau.test.logback.X";

	private LoggerContext ctx;
	private LogbackLogBackend backend;

	@BeforeEach
	void setup() {
		ctx = new LoggerContext();
		backend = new LogbackLogBackend(ctx);
	}

	@Test void a01_isLogBackend() {
		assertInstanceOf(LogBackend.class, backend);
	}

	@Test void a02_setAndGetConfiguredLevel() {
		backend.setLevel(LNAME, "DEBUG");
		assertEquals("DEBUG", backend.getLevel(LNAME));
		assertEquals("DEBUG", backend.getLevels().get(LNAME));
	}

	@Test void a03_levelNamesAreLogbackStyle() {
		backend.setLevel(LNAME, "WARN");
		assertEquals("WARN", backend.getLevel(LNAME));
	}

	@Test void a04_blankClearsToInherited() {
		backend.setLevel(LNAME, "DEBUG");
		backend.setLevel(LNAME, "");
		// Own level cleared -> inherits -> empty string.
		assertEquals("", backend.getLevel(LNAME));
	}

	@Test void a05_nullClearsToInherited() {
		backend.setLevel(LNAME, "DEBUG");
		backend.setLevel(LNAME, null);
		assertEquals("", backend.getLevel(LNAME));
	}

	@Test void a06_rootAlwaysPresentUnderRootKey() {
		// A fresh LoggerContext always has a ROOT logger (defaults to DEBUG).
		assertTrue(backend.getLevels().containsKey("ROOT"));
		backend.setLevel("ROOT", "INFO");
		assertEquals("INFO", backend.getLevel("ROOT"));
	}

	@Test void a07_unknownLoggerReturnsNull() {
		assertNull(backend.getLevel("no.such.logger.anywhere.xyz"));
	}

	@Test void a08_invalidLevelThrows() {
		assertThrows(IllegalArgumentException.class, () -> backend.setLevel(LNAME, "NOPE"));
	}

	@Test void a09_defaultCtorRequiresLogbackBinding() {
		// The test classpath binds SLF4J to Logback, so the no-arg ctor resolves the bound context.
		assertDoesNotThrow(() -> new LogbackLogBackend());
	}
}
