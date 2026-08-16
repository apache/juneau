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
 * Tests for {@link BasicActuatorGroup} &mdash; verifies that {@code /info} and {@code /loggers} are off by
 * default (not disclosed to an unauthenticated client), that health and the deny-by-default diagnostics behave
 * as before, and that a-la-carte subclassing restores {@code /info} and {@code /loggers}.
 */
@SuppressWarnings({
	"resource" // Closeable MockRestClient fixtures; lifecycle managed by the test/framework, not a real leak.
})
class BasicActuatorGroup_Test extends TestBase {

	// Mixed-case name retained to match log-level probe expectations in test assertions.
	@SuppressWarnings("java:S115")
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
	}

	@Rest(children={ActuatorChild.class})
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@AfterEach
	void resetProbeLogger() {
		Logger.getLogger(LName).setLevel(null);
	}

	@Test void a01_infoOffByDefault() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		c.get("/actuator/info").accept("application/json").run().assertStatus(404);
	}

	@Test void a02_loggersReadOffByDefault() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		c.get("/actuator/loggers").accept("application/json").run().assertStatus(404);
	}

	@Test void a03_loggersWriteOffByDefault() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		// The entire LoggersMixin (read and write) is unmounted by default, so the write side is also
		// unreachable regardless of the child's LoggersSettings bean.
		c.put("/actuator/loggers/" + LName, "FINE").accept("application/json").run().assertStatus(404);
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

	/**
	 * A-la-carte re-enablement: a subclass that adds {@link InfoMixin} and {@link LoggersMixin} restores both
	 * endpoints, on top of the {@link org.apache.juneau.rest.server.health.HealthMixin}/{@link DumpsMixin}
	 * inherited from {@link BasicActuatorGroup} (mixins declared on a subclass are additive, not a replacement).
	 */
	@Rest(path="/actuator", mixins={InfoMixin.class, LoggersMixin.class})
	public static class EnabledChild extends BasicActuatorGroup {
		private static final long serialVersionUID = 1L;
		@Bean public ManifestFile manifest() throws IOException { return BasicActuatorGroup_Test.manifest(); }
	}

	@Rest(children={EnabledChild.class})
	public static class C extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void c01_infoReachableWhenMixinAddedALaCarte() throws Exception {
		var c = MockRestClient.buildLax(C.class);
		c.get("/actuator/info").accept("application/json").run().assertStatus(200)
			.assertContent().asString().isContains("Implementation-Version", "10.0.0");
	}

	@Test void c02_loggersReadReachableWhenMixinAddedALaCarte() throws Exception {
		var c = MockRestClient.buildLax(C.class);
		c.get("/actuator/loggers").accept("application/json").run().assertStatus(200)
			.assertContent().asString().isContains("ROOT");
	}

	@Test void c03_loggersWriteStillDeniedByDefaultWhenMixinAddedALaCarte() throws Exception {
		var c = MockRestClient.buildLax(C.class);
		// The read side is reachable once the mixin is mounted, but the mutating set-level endpoint keeps its
		// own independent deny-by-default policy (no LoggersSettings bean registered on EnabledChild).
		c.put("/actuator/loggers/" + LName, "FINE").run().assertStatus(403);
	}

	@Test void c04_healthAndDumpsStillMountedWhenMixinAddedALaCarte() throws Exception {
		// Confirms subclass mixins are additive: adding Info/Loggers didn't drop the inherited Health/Dumps.
		var c = MockRestClient.buildLax(C.class);
		c.get("/actuator/healthz").accept("application/json").run().assertStatus(200);
		c.get("/actuator/threaddump").run().assertStatus(403);
	}

	/** A-la-carte {@link InfoMixin} with no manifest bean registered. */
	@Rest(path="/actuator", mixins={InfoMixin.class})
	public static class D01_BareEnabledChild extends BasicActuatorGroup {
		private static final long serialVersionUID = 1L;
	}

	@Rest(children={D01_BareEnabledChild.class})
	public static class D01_D extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void d01_infoStillReachableWithoutManifest() throws Exception {
		// No manifest bean -> /info degrades to an empty map but stays reachable (200), once mounted a-la-carte.
		var c = MockRestClient.buildLax(D01_D.class);
		c.get("/actuator/info").accept("application/json").run().assertStatus(200).assertContent().asString().is("{}");
	}
}
