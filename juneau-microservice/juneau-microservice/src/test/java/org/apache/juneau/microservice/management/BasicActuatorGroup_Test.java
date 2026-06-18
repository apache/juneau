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
package org.apache.juneau.microservice.management;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.runtime.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.management.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link BasicActuatorGroup} &mdash; verifies the management endpoints are reachable under the
 * configured prefix, the deny-by-default gating on the diagnostics is honored, and the standalone flavors
 * remain independently mountable.
 */
@SuppressWarnings({
	"resource" // Closeable MockRestClient fixtures; lifecycle managed by the test/framework, not a real leak.
})
class BasicActuatorGroup_Test extends TestBase {

	private static final String LName = "org.apache.juneau.test.actuator.Probe";

	private static ManifestFile manifest() throws IOException {
		return new ManifestFile(new StringReader("Manifest-Version: 1.0\nImplementation-Version: 10.0.0\n"));
	}

	// The group's @Rest(path) prefix only applies when it is mounted as a routed child, so the tests host it
	// under a parent servlet via @Rest(children=...).  A child resolves beans from its own bean store, so the
	// manifest + dumps settings are declared on the child subclass.
	@Rest(path="/actuator")
	public static class ActuatorChild extends BasicActuatorGroup {
		private static final long serialVersionUID = 1L;
		@Bean public ManifestFile manifest() throws IOException { return BasicActuatorGroup_Test.manifest(); }
		@Bean public DumpsSettings dumpsSettings() {
			return DumpsSettings.create().enableThreadDump().enableHeapDump().build();
		}
		@Bean public LoggersSettings loggersSettings() {
			return LoggersSettings.create().enableWrite().build();
		}
	}

	@Rest(children={ActuatorChild.class})
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@AfterEach
	void resetProbeLogger() {
		Logger.getLogger(LName).setLevel(null);
	}

	@Test void a01_infoReachable() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		c.get("/actuator/info").accept("application/json").run().assertStatus(200)
			.assertContent().asString().isContains("Implementation-Version", "10.0.0");
	}

	@Test void a02_loggersReadReachable() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		c.get("/actuator/loggers").accept("application/json").run().assertStatus(200)
			.assertContent().asString().isContains("ROOT");
	}

	@Test void a03_loggersWriteRoundTrip() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		c.put("/actuator/loggers/" + LName, "FINE").accept("application/json").run().assertStatus(200);
		assertEquals(Level.FINE, Logger.getLogger(LName).getLevel());
	}

	@Test void a04_healthReachable() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		c.get("/actuator/healthz").accept("application/json").run().assertStatus(200);
	}

	@Test void a05_threadDumpEnabled() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		c.get("/actuator/threaddump").run().assertStatus(200).assertContent().asString().isNotEmpty();
	}

	@Test void a06_heapDumpEnabled() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		var bytes = c.get("/actuator/heapdump").run().assertStatus(200).getContent().asBytes();
		assertTrue(bytes.length > 0, "Heap dump body should be non-empty");
	}

	/** Group child with no DumpsSettings bean -> dumps deny-by-default even when assembled in the group. */
	@Rest(path="/actuator")
	public static class BareChild extends BasicActuatorGroup {
		private static final long serialVersionUID = 1L;
	}

	@Rest(children={BareChild.class})
	public static class B extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void b01_dumpsDeniedByDefaultInGroup() throws Exception {
		var c = MockRestClient.buildLax(B.class);
		c.get("/actuator/threaddump").run().assertStatus(403);
		c.get("/actuator/heapdump").run().assertStatus(403);
	}

	@Test void b03_loggersWriteDeniedByDefaultInGroup() throws Exception {
		var c = MockRestClient.buildLax(B.class);
		// Reads still work; the mutating set-level is denied without an opt-in LoggersSettings bean.
		c.get("/actuator/loggers").accept("application/json").run().assertStatus(200);
		c.put("/actuator/loggers/" + LName, "FINE").run().assertStatus(403);
	}

	@Test void b02_infoStillReachableWithoutManifest() throws Exception {
		// No manifest bean -> /info degrades to an empty map but stays reachable (200).
		var c = MockRestClient.buildLax(B.class);
		c.get("/actuator/info").accept("application/json").run().assertStatus(200).assertContent().asString().is("{}");
	}
}
