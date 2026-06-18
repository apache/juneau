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
package org.apache.juneau.rest.server.management;

import static org.junit.jupiter.api.Assertions.*;

import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the {@code /loggers} runtime log-level management endpoint (mixin + resource flavors + the
 * shared {@link LoggersManager} worker).
 */
class Loggers_Test extends TestBase {

	private static final String LName = "org.apache.juneau.test.loggers.Probe";

	@Rest(mixins={LoggersMixin.class})
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@org.apache.juneau.commons.inject.Bean public LoggersSettings loggersSettings() {
			return LoggersSettings.create().enableWrite().build();
		}
	}

	@AfterEach
	void resetProbeLogger() {
		Logger.getLogger(LName).setLevel(null);
	}

	// =================================================================================
	// A. LoggersManager worker
	// =================================================================================

	@Test void a01_managerGetSetClear() {
		var m = new LoggersManager();
		m.setLevel(LName, "FINE");
		assertEquals("FINE", m.getLevel(LName));
		assertTrue(m.getLevels().containsKey(LName));
		assertEquals("FINE", m.getLevels().get(LName));
		// Blank clears the level (inherits) -> empty string.
		m.setLevel(LName, "");
		assertEquals("", m.getLevel(LName));
	}

	@Test void a02_managerUnknownLoggerNull() {
		assertNull(new LoggersManager().getLevel("no.such.logger.anywhere.xyz"));
	}

	@Test void a03_managerRootAlias() {
		// "ROOT" resolves to the empty-named root logger; it's always present in the snapshot.
		assertTrue(new LoggersManager().getLevels().containsKey("ROOT"));
	}

	@Test void a04_managerInvalidLevelThrows() {
		var m = new LoggersManager();
		assertThrows(IllegalArgumentException.class, () -> m.setLevel(LName, "NOPE"));
	}

	@Test void a05_managerNullLevelClears() {
		var m = new LoggersManager();
		m.setLevel(LName, "FINE");
		m.setLevel(LName, null);
		assertNull(Logger.getLogger(LName).getLevel());
	}

	@Test void a06_resolveSettingsNullContextDefault() {
		var s = new LoggersManager().resolveSettings(null);
		assertSame(LoggersSettings.DEFAULT, s);
		assertFalse(s.isWriteEnabled());
	}

	@Test void a07_rootAndNullResolveToRootLogger() {
		var m = new LoggersManager();
		// "ROOT" and null both map to the empty-named root logger, which is always present.
		assertNotNull(m.getLevel("ROOT"));
		assertNotNull(m.getLevel(null));
	}

	// =================================================================================
	// B. Mixin flavor over MockRestClient
	// =================================================================================

	@Test void b01_listLoggers() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		c.get("/loggers").accept("application/json").run().assertStatus(200).assertContent().asString().isContains("ROOT");
	}

	@Test void b02_getSetGetRoundTrip() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		c.put("/loggers/" + LName, "FINE").accept("application/json").run().assertStatus(200);
		c.get("/loggers/" + LName).accept("application/json").run().assertStatus(200).assertContent().asString().isContains("FINE");
		assertEquals(Level.FINE, Logger.getLogger(LName).getLevel());
	}

	@Test void b03_postAliasSets() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		c.post("/loggers/" + LName, "WARNING").accept("application/json").run().assertStatus(200);
		assertEquals(Level.WARNING, Logger.getLogger(LName).getLevel());
	}

	@Test void b04_getUnknownLogger404() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		c.get("/loggers/no.such.logger.anywhere.xyz").run().assertStatus(404);
	}

	@Test void b05_setBlankInherits() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		Logger.getLogger(LName).setLevel(Level.FINE);
		c.put("/loggers/" + LName, "").accept("application/json").run().assertStatus(200);
		assertNull(Logger.getLogger(LName).getLevel());
	}

	// =================================================================================
	// C. Resource flavor
	// =================================================================================

	// A routed child resolves beans from its own bean store, so LoggersSettings is declared on the child
	// subclass — mirroring the HealthResource child-flavor test precedent.
	@Rest(path="/loggers")
	public static class LoggersChild extends LoggersResource {
		@org.apache.juneau.commons.inject.Bean public LoggersSettings loggersSettings() {
			return LoggersSettings.create().enableWrite().build();
		}
	}

	@Rest(children={LoggersChild.class})
	public static class B extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void c01_resourceListAndSet() throws Exception {
		var c = MockRestClient.buildLax(B.class);
		c.get("/loggers").accept("application/json").run().assertStatus(200).assertContent().asString().isContains("ROOT");
		c.put("/loggers/" + LName, "INFO").accept("application/json").run().assertStatus(200);
		assertEquals(Level.INFO, Logger.getLogger(LName).getLevel());
	}

	@Test void c02_resourceGetSingleAndPostAlias() throws Exception {
		var c = MockRestClient.buildLax(B.class);
		c.post("/loggers/" + LName, "WARNING").accept("application/json").run().assertStatus(200);
		c.get("/loggers/" + LName).accept("application/json").run().assertStatus(200).assertContent().asString().isContains("WARNING");
	}

	@Test void c03_resourceGetUnknown404() throws Exception {
		var c = MockRestClient.buildLax(B.class);
		c.get("/loggers/no.such.logger.anywhere.xyz").run().assertStatus(404);
	}

	// Resource child with no LoggersSettings bean -> write denied.
	@Rest(path="/loggers")
	public static class LoggersChildReadOnly extends LoggersResource {}

	@Rest(children={LoggersChildReadOnly.class})
	public static class E extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void c04_resourceWriteDeniedByDefault() throws Exception {
		var c = MockRestClient.buildLax(E.class);
		c.get("/loggers").accept("application/json").run().assertStatus(200);
		c.put("/loggers/" + LName, "FINE").run().assertStatus(403);
		c.post("/loggers/" + LName, "FINE").run().assertStatus(403);
		assertNull(Logger.getLogger(LName).getLevel());
	}

	// =================================================================================
	// D. Write deny-by-default (no LoggersSettings bean)
	// =================================================================================

	@Rest(mixins={LoggersMixin.class})
	public static class C extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void d01_writeDeniedByDefault() throws Exception {
		var c = MockRestClient.buildLax(C.class);
		// Reads still work...
		c.get("/loggers").accept("application/json").run().assertStatus(200).assertContent().asString().isContains("ROOT");
		// ...but writes are denied without an opt-in LoggersSettings bean.
		c.put("/loggers/" + LName, "FINE").run().assertStatus(403);
		c.post("/loggers/" + LName, "FINE").run().assertStatus(403);
		assertNull(Logger.getLogger(LName).getLevel());
	}
}
