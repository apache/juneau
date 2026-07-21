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
import org.apache.logging.log4j.*;
import org.apache.logging.log4j.core.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link Log4j2LogBackend} driven against the current Log4j2 {@link LoggerContext}, verifying it honors
 * the {@link LogBackend} contract (configured-level reads, process-lifetime sets via {@code Configurator}, "ROOT"
 * alias, hard-fail on bad level).
 */
class Log4j2LogBackend_Test extends org.apache.juneau.TestBase {

	private static final String LNAME = "org.apache.juneau.test.log4j2.X";

	private Log4j2LogBackend backend;

	@BeforeEach
	void setup() {
		backend = new Log4j2LogBackend((LoggerContext) LogManager.getContext(false));
	}

	@Test void a01_isLogBackend() {
		assertInstanceOf(LogBackend.class, backend);
	}

	@Test void a02_setAndGetConfiguredLevel() {
		backend.setLevel(LNAME, "DEBUG");
		assertEquals("DEBUG", backend.getLevel(LNAME));
		assertEquals("DEBUG", backend.getLevels().get(LNAME));
	}

	@Test void a03_levelNamesAreLog4jStyle() {
		backend.setLevel(LNAME, "WARN");
		assertEquals("WARN", backend.getLevel(LNAME));
		backend.setLevel(LNAME, "ERROR");
		assertEquals("ERROR", backend.getLevel(LNAME));
	}

	@Test void a04_rootAddressableUnderRootKey() {
		backend.setLevel("ROOT", "INFO");
		assertTrue(backend.getLevels().containsKey("ROOT"));
		assertEquals("INFO", backend.getLevel("ROOT"));
	}

	@Test void a05_unknownLoggerReturnsNull() {
		assertNull(backend.getLevel("no.such.logger.anywhere.xyz"));
	}

	@Test void a06_invalidLevelThrows() {
		assertThrows(IllegalArgumentException.class, () -> backend.setLevel(LNAME, "NOPE"));
	}

	@Test void a07_caseInsensitiveLevelName() {
		backend.setLevel(LNAME, "debug");
		assertEquals("DEBUG", backend.getLevel(LNAME));
	}

	@Test
	@SuppressWarnings("java:S1612") // Log4j2LogBackend::new is ambiguous here: it matches both assertDoesNotThrow(Executable) and assertDoesNotThrow(ThrowingSupplier<T>), unlike the equivalent lambda.
	void a08_defaultCtorResolvesContext() {
		assertDoesNotThrow(() -> new Log4j2LogBackend());
	}
}
