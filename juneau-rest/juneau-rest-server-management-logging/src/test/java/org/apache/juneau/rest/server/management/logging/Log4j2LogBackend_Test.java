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

import java.util.*;

import org.apache.juneau.rest.server.management.*;
import org.apache.logging.log4j.*;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.config.*;
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
	@SuppressWarnings({
		"java:S1612" // Log4j2LogBackend::new is ambiguous here: it matches both assertDoesNotThrow(Executable) and assertDoesNotThrow(ThrowingSupplier<T>), unlike the equivalent lambda.
	})
	void a08_defaultCtorResolvesContext() {
		assertDoesNotThrow(() -> new Log4j2LogBackend());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Blank / null level and name handling.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_nullNameAddressesRoot() {
		var original = backend.getLevel("ROOT");
		try {
			backend.setLevel(null, "WARN");
			assertEquals("WARN", backend.getLevel(null));
			assertEquals("WARN", backend.getLevel("ROOT"));
			assertEquals("WARN", backend.getLevels().get("ROOT"));
		} finally {
			backend.setLevel("ROOT", original);
		}
	}

	@Test void b02_blankLevelDoesNotThrow() {
		backend.setLevel(LNAME, "DEBUG");
		assertDoesNotThrow(() -> backend.setLevel(LNAME, "  "));
		assertDoesNotThrow(() -> backend.setLevel(LNAME, null));
	}

	@Test void b03_levelNameIsTrimmed() {
		backend.setLevel(LNAME, "  warn ");
		assertEquals("WARN", backend.getLevel(LNAME));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Custom Configuration: shapes the stock Log4j2 configuration never produces.
	//-----------------------------------------------------------------------------------------------------------------

	/** LoggerConfig whose configured level is reported as <jk>null</jk>. */
	private static class NullLevelConfig extends LoggerConfig {
		NullLevelConfig(String name) {
			super(name, Level.INFO, true);
		}

		@Override
		public Level getLevel() {
			return null;
		}
	}

	/** Configuration exposing a caller-supplied root and logger map. */
	private static class StubConfiguration extends DefaultConfiguration {
		private final LoggerConfig root;
		private final Map<String,LoggerConfig> loggers;
		private final boolean ready;  // The superclass constructor calls the overridden accessors before our fields are assigned.

		StubConfiguration(LoggerConfig root, Map<String,LoggerConfig> loggers) {
			this.root = root;
			this.loggers = loggers;
			this.ready = true;
		}

		@Override
		public LoggerConfig getRootLogger() {
			return ready ? root : super.getRootLogger();
		}

		@Override
		public Map<String,LoggerConfig> getLoggers() {
			return ready ? loggers : super.getLoggers();
		}
	}

	@SuppressWarnings({
		"resource" // The stub LoggerContext is handed to the backend under test and never started, so there is nothing to close.
	})
	private static Log4j2LogBackend backendFor(Configuration config) {
		return new Log4j2LogBackend(new LoggerContext("stub") {
			@Override
			public Configuration getConfiguration() {
				return config;
			}
		});
	}

	@Test void c01_noRootLogger() {
		var named = new LoggerConfig("a.b", Level.WARN, true);
		var b = backendFor(new StubConfiguration(null, Map.of("a.b", named)));
		assertEquals(Map.of("a.b", "WARN"), b.getLevels());
		assertNull(b.getLevel("ROOT"));
		assertNull(b.getLevel(null));
	}

	@Test void c02_nullLevelsReportEmptyString() {
		var root = new NullLevelConfig("");
		var named = new NullLevelConfig("a.b");
		var b = backendFor(new StubConfiguration(root, Map.of("a.b", named)));
		assertEquals(Map.of("ROOT", "", "a.b", ""), b.getLevels());
		assertEquals("", b.getLevel("ROOT"));
		assertEquals("", b.getLevel("a.b"));
	}

	@Test void c03_emptyNamedLoggerSkipped() {
		var root = new LoggerConfig("", Level.ERROR, true);
		var dup = new LoggerConfig("", Level.TRACE, true);
		var named = new LoggerConfig("a.b", Level.WARN, true);
		var b = backendFor(new StubConfiguration(root, Map.of("", dup, "a.b", named)));
		assertEquals(Map.of("ROOT", "ERROR", "a.b", "WARN"), b.getLevels());
	}
}
