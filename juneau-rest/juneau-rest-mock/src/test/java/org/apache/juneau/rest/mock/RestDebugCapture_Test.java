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
package org.apache.juneau.rest.mock;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.logging.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.logging.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.logging.*;
import org.junit.jupiter.api.*;

/**
 * End-to-end REST debug capture tests through {@code mock.classic.MockRestClient}.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // MockRestClient instances are short-lived test fixtures.
})
class RestDebugCapture_Test {

	@Rest(path="/api")
	public static class A_Resource {

		@RestGet(path="/who")
		public String who() {
			return "ok";
		}

		@RestGet(path="/err")
		public String err(RestResponse res) {
			res.setException(new RuntimeException("boom"));
			return "handled";
		}
	}

	public static class A05_Mixin {
		@RestGet(path="/who")
		public String who() {
			return "ok";
		}
	}

	@Rest(path="/mix", mixins=A05_Mixin.class)
	public static class A05_HostResource {}

	/**
	 * A resource that IS its own {@link RestDebugFormatter} (highest-precedence resolution path, per
	 * {@code RestContext#getRestDebugFormatter()}) with a capture cap far below the 8&nbsp;KB wrapper default.
	 * Proves the cap is honored at <i>capture</i> time (Blocker #3 in the TODO-329 retrospective), not just as a
	 * post-hoc truncation-marker computation over already-8KB-capped bytes.
	 */
	@Rest(path="/cap4")
	public static class A06_Resource extends BasicRestDebugFormatter {
		public A06_Resource() {
			bodyCap(4);
		}

		@RestPost(path="/echo")
		public String echo(RestRequest req) throws IOException {
			return req.getContent().asString();
		}
	}

	/** Same as {@link A06_Resource}, but with capture fully disabled via {@code bodyCap(0)}. */
	@Rest(path="/cap0")
	public static class A07_Resource extends BasicRestDebugFormatter {
		public A07_Resource() {
			bodyCap(0);
		}

		@RestPost(path="/echo")
		public String echo(RestRequest req) throws IOException {
			return req.getContent().asString();
		}
	}

	/**
	 * A mixin composed by two independent hosts, used to prove host-level cascade (raising the host's own logger
	 * elevates the mixin op) <b>and</b> host isolation (raising one host does not leak into the other host's
	 * identically-shaped, identically-named-suffix op logger). TODO-329 retrospective Should-fix #4.
	 */
	public static class B_Mixin {
		@RestGet(path="/who")
		public String who() {
			return "ok";
		}
	}

	@Rest(path="/mixHostA", mixins=B_Mixin.class)
	public static class B_HostA {}

	@Rest(path="/mixHostB", mixins=B_Mixin.class)
	public static class B_HostB {}

	@Test void b01_twoHostsComposingSameMixin_hostLevelCascadesAndHostsAreIsolated() throws Exception {
		try (var ca = RichLogger.getLogger(B_HostA.class).captureEvents(Level.FINEST);
				var cb = RichLogger.getLogger(B_HostB.class).captureEvents(Level.FINEST)) {

			// .debug() raises ONLY B_HostA's own class logger to FINEST for the duration of the call.
			var clientA = org.apache.juneau.rest.mock.classic.MockRestClient.create(B_HostA.class).debug().build();
			var clientB = org.apache.juneau.rest.mock.classic.MockRestClient.create(B_HostB.class).build();

			clientA.get("/who").run().assertStatus().asCode().is(200);
			clientB.get("/who").run().assertStatus().asCode().is(200);

			var opA = ca.getRecords().stream()
				.filter(r -> (B_HostA.class.getName() + ".who").equals(r.getLoggerName()))
				.findFirst().orElse(null);
			assertNotNull(opA, "expected a mixin op-logger record named <HostA>.who (host-level cascade)");
			assertEquals(Level.FINEST, opA.getLevel());

			var opB = cb.getRecords().stream()
				.filter(r -> (B_HostB.class.getName() + ".who").equals(r.getLoggerName()))
				.findFirst().orElse(null);
			assertNotNull(opB, "expected a mixin op-logger record named <HostB>.who");
			assertEquals(Level.INFO, opB.getLevel(),
				"HostB's mixin op logger must NOT inherit HostA's elevated level -- hosts composing the same mixin "
					+ "must be isolated");
		}
	}

	/** Two sibling operations on the same resource, used to prove per-operation child-logger elevation. */
	@Rest(path="/perop")
	public static class C_Resource {
		@RestGet(path="/one")
		public String one() {
			return "one";
		}
		@RestGet(path="/two")
		public String two() {
			return "two";
		}
	}

	@Test void c01_perOperationChildLoggerElevation_doesNotAffectSiblingOperation() throws Exception {
		var opOneName = C_Resource.class.getName() + ".one";
		var opOneLogger = Logger.getLogger(opOneName); // strong local ref -- avoids the LogManager weak-ref GC hazard
		var prevLevel = opOneLogger.getLevel();
		opOneLogger.setLevel(Level.FINEST);
		try (var c = RichLogger.getLogger(C_Resource.class).captureEvents(Level.FINEST)) {
			var client = org.apache.juneau.rest.mock.classic.MockRestClient.create(C_Resource.class).build();

			client.get("/one").run().assertStatus().asCode().is(200);
			client.get("/two").run().assertStatus().asCode().is(200);

			var recOne = c.getRecords().stream().filter(r -> opOneName.equals(r.getLoggerName())).findFirst().orElse(null);
			var recTwo = c.getRecords().stream()
				.filter(r -> (C_Resource.class.getName() + ".two").equals(r.getLoggerName()))
				.findFirst().orElse(null);

			assertNotNull(recOne);
			assertNotNull(recTwo);
			assertEquals(Level.FINEST, recOne.getLevel());
			assertEquals(Level.INFO, recTwo.getLevel(),
				"sibling operation must not inherit the elevated per-op child logger level");
		} finally {
			opOneLogger.setLevel(prevLevel);
		}
	}

	/** Used to prove the two-phase pipeline: below {@code FINEST} no capture wrapper is installed, so the body
	 * never reaches the rendered record even though the handler still sees the full content. */
	@Rest(path="/twophase")
	public static class D_Resource {
		@RestPost(path="/echo")
		public String echo(RestRequest req) throws IOException {
			return req.getContent().asString();
		}
	}

	@Test void d01_fineTier_noCaptureWrapperInstalled_bodyNeverRendered() throws Exception {
		var target = Logger.getLogger(D_Resource.class.getName());
		var prevLevel = target.getLevel();
		target.setLevel(Level.FINE);
		try (var c = RichLogger.getLogger(D_Resource.class).captureEvents(Level.FINE)) {
			var client = org.apache.juneau.rest.mock.classic.MockRestClient.create(D_Resource.class).build();

			// Downstream handler must still see the full body -- FINE tier just doesn't wrap/capture it.
			client.post("/echo", "two-phase-secret").run().assertContent("two-phase-secret");

			assertFalse(c.isEmpty());
			assertEquals(Level.FINE, c.last().getLevel());
			assertFalse(c.last().getMessage().contains("two-phase-secret"),
				"FINE tier must not install the capture wrapper, so the body cannot appear in the record: " + c.last().getMessage());
		} finally {
			target.setLevel(prevLevel);
		}
	}

	@Test void d02_finestTier_captureWrapperInstalled_bodyRendered() throws Exception {
		try (var c = RichLogger.getLogger(D_Resource.class).captureEvents(Level.FINEST)) {
			var client = org.apache.juneau.rest.mock.classic.MockRestClient.create(D_Resource.class).debug().build();

			client.post("/echo", "two-phase-secret").run().assertContent("two-phase-secret");

			assertFalse(c.isEmpty());
			assertEquals(Level.FINEST, c.last().getLevel());
			assertTrue(c.last().getMessage().contains("two-phase-secret"),
				"FINEST tier must install the capture wrapper and render the body: " + c.last().getMessage());
		}
	}

	/** Formatter resolution precedence: resource-implements-the-SPI beats a bean-registered formatter. */
	public static class E_BeanFormatter implements RestDebugFormatter {
		@Override public String formatBasic(RestRequest req, RestResponse res) { return "BEAN-IMPL"; }
	}

	@Rest(path="/precedence1")
	public static class E_ResourceImplementsAndHasBean implements RestDebugFormatter {
		@Override public String formatBasic(RestRequest req, RestResponse res) { return "RESOURCE-IMPL"; }
		@RestGet(path="/who") public String who() { return "ok"; }
		@Bean public RestDebugFormatter formatter() { return new E_BeanFormatter(); }
	}

	@Rest(path="/precedence2")
	public static class E_BeanOnly {
		@RestGet(path="/who") public String who() { return "ok"; }
		@Bean public RestDebugFormatter formatter() { return new E_BeanFormatter(); }
	}

	@Test void e01_resourceImplementsFormatter_beatsBeanRegisteredFormatter() throws Exception {
		try (var c = RichLogger.getLogger(E_ResourceImplementsAndHasBean.class).captureEvents(Level.INFO)) {
			var client = org.apache.juneau.rest.mock.classic.MockRestClient.create(E_ResourceImplementsAndHasBean.class).build();
			client.get("/who").run().assertStatus().asCode().is(200);

			assertFalse(c.isEmpty());
			assertTrue(c.last().getMessage().contains("RESOURCE-IMPL"), c.last().getMessage());
			assertFalse(c.last().getMessage().contains("BEAN-IMPL"), c.last().getMessage());
		}
	}

	@Test void e02_beanRegisteredFormatter_beatsDefaultFormatter() throws Exception {
		try (var c = RichLogger.getLogger(E_BeanOnly.class).captureEvents(Level.INFO)) {
			var client = org.apache.juneau.rest.mock.classic.MockRestClient.create(E_BeanOnly.class).build();
			client.get("/who").run().assertStatus().asCode().is(200);

			assertFalse(c.isEmpty());
			assertTrue(c.last().getMessage().contains("BEAN-IMPL"), c.last().getMessage());
		}
	}

	/**
	 * A resource that replaces its resolved {@link RichLogger} via a {@code @Bean} factory, used to prove the
	 * per-operation logger is a hierarchical child of the <i>resolved</i> (bean-overridden) logger name, not the
	 * raw resource class name. TODO-329 retrospective Should-fix #10.
	 */
	@Rest(path="/beanlogger")
	public static class F_Resource {
		@RestGet(path="/who") public String who() { return "ok"; }
		@Bean public RichLogger logger() { return RichLogger.getLogger("todo371.custom.override.logger"); }
	}

	@Test void f01_beanOverriddenResourceLogger_isJulParentOfOpChildLogger() throws Exception {
		var overrideLogger = RichLogger.getLogger("todo371.custom.override.logger"); // strong ref
		var prevLevel = overrideLogger.getLevel();
		overrideLogger.setLevel(Level.FINEST);
		try (var c = overrideLogger.captureEvents(Level.FINEST)) {
			var client = org.apache.juneau.rest.mock.classic.MockRestClient.create(F_Resource.class).build();
			client.get("/who").run().assertStatus().asCode().is(200);

			var rec = c.getRecords().stream()
				.filter(r -> "todo371.custom.override.logger.who".equals(r.getLoggerName()))
				.findFirst().orElse(null);
			assertNotNull(rec, "op logger must be named as a child of the bean-overridden logger, not "
				+ F_Resource.class.getName() + ".who");
			assertEquals(Level.FINEST, rec.getLevel());
		} finally {
			overrideLogger.setLevel(prevLevel);
		}
	}

	/**
	 * Proves the 404/no-op path renders <b>only</b> the basic status line even at the {@code FINEST} tier --
	 * headers and bodies are never rendered when no operation was resolved. TODO-329 retrospective Should-fix #12.
	 */
	@Test void g01_noOpPath_atFinestTier_neverRendersHeadersOrBody() throws Exception {
		try (var c = RichLogger.getLogger(A_Resource.class).captureEvents(Level.FINEST)) {
			var client = org.apache.juneau.rest.mock.classic.MockRestClient.create(A_Resource.class).debug().build();

			client.get("/does-not-exist").header("X-Secret-Header", "leak-test-value").ignoreErrors().run()
				.assertStatus().asCode().is(404);

			var rec = c.getRecords().stream()
				.filter(r -> A_Resource.class.getName().equals(r.getLoggerName()))
				.reduce((a, b) -> b)
				.orElse(null);
			assertNotNull(rec);
			assertEquals(Level.FINEST, rec.getLevel(), "the resolved resource logger's tier is still FINEST");
			assertFalse(rec.getMessage().contains("X-Secret-Header"),
				"no headers should ever render on the 404/no-op path, even at FINEST: " + rec.getMessage());
			assertFalse(rec.getMessage().contains("leak-test-value"), rec.getMessage());
			assertTrue(rec.getMessage().contains("[404]"), rec.getMessage());
		}
	}

	@Test void a01_debugEnabled_capturesAtFinestTier() throws Exception {
		try (var c = RichLogger.getLogger(A_Resource.class).captureEvents(Level.FINEST)) {
			var client = org.apache.juneau.rest.mock.classic.MockRestClient
				.create(A_Resource.class)
				.debug()
				.build();

			client.get("/who").run().getContent().asString();

			assertFalse(c.isEmpty());
			assertEquals(Level.FINEST, c.last().getLevel());
			assertTrue(c.last().getMessage().contains("[200] HTTP GET /api/who"));
			assertNull(c.last().getThrown());
		}
	}

	@Test void a02_clear_resetsCapturedState() throws Exception {
		try (var c = RichLogger.getLogger(A_Resource.class).captureEvents(Level.FINEST)) {
			var client = org.apache.juneau.rest.mock.classic.MockRestClient
				.create(A_Resource.class)
				.debug()
				.build();

			client.get("/who").run().getContent().asString();
			assertFalse(c.isEmpty());

			c.clear();
			assertTrue(c.isEmpty());
			assertNull(c.last());

			client.get("/who").run().getContent().asString();
			assertFalse(c.isEmpty());
		}
	}

	@Test void a03_debugDisabled_leavesResourceLoggerLevelUnchanged() throws Exception {
		var target = Logger.getLogger(A_Resource.class.getName());
		var prevLevel = target.getLevel();
		target.setLevel(Level.OFF);
		try (var c = RichLogger.getLogger(A_Resource.class).captureEvents(Level.FINEST)) {
			var client = org.apache.juneau.rest.mock.classic.MockRestClient
				.create(A_Resource.class)
				.build();

			client.get("/who").run().getContent().asString();

			assertTrue(c.isEmpty(), "No records should be captured below the resolved logger tier");
			assertEquals(Level.OFF, target.getLevel(), "Logger level should remain unchanged without .debug()");
		} finally {
			target.setLevel(prevLevel);
		}
	}

	@Test void a04_thrownExceptionIsCaptured() throws Exception {
		try (var c = RichLogger.getLogger(A_Resource.class).captureEvents(Level.FINEST)) {
			var client = org.apache.juneau.rest.mock.classic.MockRestClient
				.create(A_Resource.class)
				.debug()
				.build();

			client.get("/err").run().assertStatus().asCode().is(200);

			assertNotNull(c.last());
			assertNotNull(c.last().getThrown());
			assertEquals("boom", c.last().getThrown().getMessage());
		}
	}

	@Test void a05_captureByHostName_observesOpAndNoOpLoggerPaths() throws Exception {
		var hostName = A05_HostResource.class.getName();
		try (var c = RichLogger.getLogger(A05_HostResource.class).captureEvents(Level.FINEST)) {
			var client = org.apache.juneau.rest.mock.classic.MockRestClient
				.create(A05_HostResource.class)
				.debug()
				.build();

			client.get("/who").run().assertStatus().asCode().is(200);
			client.get("/missing").ignoreErrors().run().assertStatus().asCode().is(404);

			var records = c.getRecords();
			assertTrue(records.stream().map(java.util.logging.LogRecord::getLoggerName).anyMatch((hostName + ".who")::equals));
			assertTrue(records.stream().map(java.util.logging.LogRecord::getLoggerName).anyMatch(hostName::equals));

			var noOpRecord = records.stream()
				.filter(x -> hostName.equals(x.getLoggerName()))
				.reduce((a, b) -> b)
				.orElse(null);
			assertNotNull(noOpRecord);
			assertTrue(noOpRecord.getMessage().contains("[404] HTTP GET /mix/missing"));
		}
	}

	@Test void a06_bodyCapOverride_lowersCaptureAtCaptureTime() throws Exception {
		try (var c = RichLogger.getLogger(A06_Resource.class).captureEvents(Level.FINEST)) {
			var client = org.apache.juneau.rest.mock.classic.MockRestClient
				.create(A06_Resource.class)
				.debug()
				.build();

			// 10-byte body; formatter overrides the cap to 4, well below the 8KB wrapper default.
			client.post("/echo", "0123456789").run().assertContent("0123456789");

			assertFalse(c.isEmpty());
			var msg = c.last().getMessage();
			assertTrue(msg.contains("0123"), msg);
			assertFalse(msg.contains("456789"), "captured body must be capped to 4 bytes, not the 8KB default: " + msg);
			assertTrue(msg.contains("truncated 6 bytes"), msg);
		}
	}

	@Test void a07_bodyCapZero_disablesCaptureWithoutAffectingDownstream() throws Exception {
		try (var c = RichLogger.getLogger(A07_Resource.class).captureEvents(Level.FINEST)) {
			var client = org.apache.juneau.rest.mock.classic.MockRestClient
				.create(A07_Resource.class)
				.debug()
				.build();

			// Downstream handler must still see the full body even though capture is disabled.
			client.post("/echo", "0123456789").run().assertContent("0123456789");

			assertFalse(c.isEmpty());
			var msg = c.last().getMessage();
			assertFalse(msg.contains("Request Content"), "no body section should render when bodyCap(0): " + msg);
		}
	}
}
