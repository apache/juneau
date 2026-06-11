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
package org.apache.juneau.rest.server.logger;

import static org.apache.juneau.rest.server.logger.CallLoggingDetail.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.*;
import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.rest.server.stats.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;

/**
 * Coverage tests for {@link CallLogger} targeting Builder mutators, log() rendering branches
 * (HEADER/ENTITY), debug-rule selection, ThrownStore integration, and builder helpers
 * (logger by name, loggerOnce, thrownStoreOnce, rules, etc.).
 */
class CallLogger_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Helpers
	//-----------------------------------------------------------------------------------------------------------------

	/** Capture variant that records all messages for later inspection. */
	private static class CapturingLogger extends CallLogger {
		final AtomicReference<Level> level = new AtomicReference<>();
		final AtomicReference<String> message = new AtomicReference<>();
		final AtomicReference<Throwable> thrown = new AtomicReference<>();

		CapturingLogger(Builder b) {
			super(b);
		}

		@Override
		protected void log(Level lvl, String msg, Throwable e) {
			level.set(lvl);
			message.set(msg);
			thrown.set(e);
		}
	}

	private static CapturingLogger newCapturing(CallLogger.Builder b) {
		return new CapturingLogger(b);
	}

	private static CallLogger.Builder newBuilder() {
		return CallLogger.create(BasicBeanStore.INSTANCE);
	}

	private static CallLoggerRule rule(java.util.function.Consumer<CallLoggerRule.Builder> c) {
		var b = CallLoggerRule.create(BasicBeanStore.INSTANCE);
		c.accept(b);
		return b.build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a - Builder mutators (level, requestDetail, responseDetail, enabled, enabledTest, disabled, rules)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_builder_levelMutator() {
		var b = newBuilder().level(Level.WARNING);
		assertEquals(Level.WARNING, b.level);
	}

	@Test void a02_builder_requestDetailMutator() {
		var b = newBuilder().requestDetail(HEADER);
		assertEquals(HEADER, b.requestDetail);
	}

	@Test void a03_builder_responseDetailMutator() {
		var b = newBuilder().responseDetail(ENTITY);
		assertEquals(ENTITY, b.responseDetail);
	}

	@Test void a04_builder_enabledMutator() {
		var b = newBuilder().enabled(Enablement.NEVER);
		assertEquals(Enablement.NEVER, b.enabled);
	}

	@Test void a05_builder_enabledTestMutator() {
		java.util.function.Predicate<jakarta.servlet.http.HttpServletRequest> p = x -> true;
		var b = newBuilder().enabledTest(p);
		assertSame(p, b.enabledTest);
	}

	@Test void a06_builder_disabledShortcut() {
		var b = newBuilder().disabled();
		assertEquals(Enablement.NEVER, b.enabled);
	}

	@Test void a07_builder_rulesAddsToBoth() {
		var r = rule(x -> x.level(Level.INFO));
		var b = newBuilder().rules(r);
		assertEquals(1, b.normalRules.size());
		assertEquals(1, b.debugRules.size());
		assertSame(r, b.normalRules.get(0));
		assertSame(r, b.debugRules.get(0));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - Builder.logger string and loggerOnce
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_builder_loggerByName_setsNamedLogger() {
		var b = newBuilder().logger("CallLogger_Test.b01");
		assertNotNull(b.logger);
		assertEquals("CallLogger_Test.b01", b.logger.getName());
	}

	@Test void b02_builder_loggerByName_nullClearsLogger() {
		var b = newBuilder().logger((String)null);
		assertNull(b.logger);
	}

	@Test void b03_builder_loggerOnce_setsWhenNull() {
		var b = newBuilder().logger((String)null);
		var custom = Logger.getLogger("CallLogger_Test.b03");
		b.loggerOnce(custom);
		assertSame(custom, b.logger);
	}

	@Test void b04_builder_loggerOnce_doesNotOverwrite() {
		var first = Logger.getLogger("CallLogger_Test.b04.first");
		var second = Logger.getLogger("CallLogger_Test.b04.second");
		var b = newBuilder().logger(first).loggerOnce(second);
		assertSame(first, b.logger);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c - Builder.thrownStore and thrownStoreOnce
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_builder_thrownStore_set() {
		var ts = new ThrownStore();
		var b = newBuilder().thrownStore(ts);
		assertSame(ts, b.thrownStore);
	}

	@Test void c02_builder_thrownStoreOnce_setsWhenNull() {
		var ts = new ThrownStore();
		var b = newBuilder().thrownStoreOnce(ts);
		assertSame(ts, b.thrownStore);
	}

	@Test void c03_builder_thrownStoreOnce_doesNotOverwrite() {
		var first = new ThrownStore();
		var second = new ThrownStore();
		var b = newBuilder().thrownStore(first).thrownStoreOnce(second);
		assertSame(first, b.thrownStore);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d - log() with HEADER/ENTITY rendering — exercises the multi-branch detail block
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_log_headerDetail_includesQueryAndHeaders() {
		var logger = newCapturing(newBuilder()
			.normalRules(rule(x -> x.statusFilter(s -> s >= 500).level(Level.SEVERE).requestDetail(HEADER).responseDetail(HEADER)))
			.level(Level.OFF));
		var req = new MockHttpServletRequest("GET", "/foo");
		req.setQueryString("a=1&b=2");
		req.addHeader("X-Custom", "v1");
		var res = new MockHttpServletResponse();
		res.setStatus(500);
		res.setHeader("X-Resp", "rv");
		logger.log(req, res);
		var msg = logger.message.get();
		assertNotNull(msg);
		assertTrue(msg.contains("[500]"));
		assertTrue(msg.contains("HTTP GET /foo"));
		assertTrue(msg.contains("?a=1&b=2"));
		assertTrue(msg.contains("Response code: 500"));
		assertTrue(msg.contains("---Request Headers---"));
		assertTrue(msg.contains("X-Custom: v1"));
		assertTrue(msg.contains("---Response Headers---"));
		assertTrue(msg.contains("X-Resp: rv"));
		assertTrue(msg.contains("=== END"));
	}

	@Test void d02_log_entityDetail_includesContentBlocks() {
		var logger = newCapturing(newBuilder()
			.normalRules(rule(x -> x.statusFilter(s -> s >= 500).level(Level.SEVERE).requestDetail(ENTITY).responseDetail(ENTITY)))
			.level(Level.OFF));
		var req = new MockHttpServletRequest("POST", "/bar");
		req.setAttribute("RequestContent", "hello".getBytes());
		req.setAttribute("ResponseContent", "world".getBytes());
		req.setAttribute("ExecTime", 42L);
		var res = new MockHttpServletResponse();
		res.setStatus(500);
		logger.log(req, res);
		var msg = logger.message.get();
		assertNotNull(msg);
		assertTrue(msg.contains("Request length: 5 bytes"));
		assertTrue(msg.contains("Response length: 5 bytes"));
		assertTrue(msg.contains("Exec time: 42ms"));
		assertTrue(msg.contains("---Request Content UTF-8---"));
		assertTrue(msg.contains("hello"));
		assertTrue(msg.contains("---Request Content Hex---"));
		assertTrue(msg.contains("---Response Content UTF-8---"));
		assertTrue(msg.contains("world"));
		assertTrue(msg.contains("---Response Content Hex---"));
	}

	@Test void d03_log_statusLineOnly_omitsBlocks() {
		var logger = newCapturing(newBuilder()
			.normalRules(rule(x -> x.statusFilter(s -> s >= 500).level(Level.SEVERE).requestDetail(STATUS_LINE).responseDetail(STATUS_LINE)))
			.level(Level.OFF));
		var req = new MockHttpServletRequest("GET", "/x");
		var res = new MockHttpServletResponse();
		res.setStatus(503);
		logger.log(req, res);
		var msg = logger.message.get();
		assertNotNull(msg);
		assertTrue(msg.contains("[503] HTTP GET /x"));
		assertFalse(msg.contains("=== HTTP Call"));
		assertFalse(msg.contains("Response code:"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e - log() exception classification with ThrownStore (hash + count)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_log_withThrownStore_includesHashAndCount() {
		var ts = new ThrownStore();
		var logger = newCapturing(newBuilder()
			.normalRules(rule(x -> x.statusFilter(s -> s >= 500).level(Level.SEVERE).requestDetail(STATUS_LINE).responseDetail(STATUS_LINE)))
			.thrownStore(ts)
			.level(Level.OFF));
		var req = new MockHttpServletRequest("GET", "/err");
		req.setAttribute("Exception", new RuntimeException("boom"));
		var res = new MockHttpServletResponse();
		res.setStatus(500);
		logger.log(req, res);
		var msg = logger.message.get();
		assertNotNull(msg);
		// Format: [500,<hex8>.<count>]
		assertTrue(msg.matches("(?s)^\\[500,[0-9a-fA-F]+\\.\\d+\\] .*"), () -> "msg=" + msg);
		assertNotNull(logger.thrown.get());
	}

	@Test void e02_log_withThrownStore_secondOccurrenceClearsThrown() {
		var ts = new ThrownStore();
		var logger = newCapturing(newBuilder()
			.normalRules(rule(x -> x.statusFilter(s -> s >= 500).level(Level.SEVERE).requestDetail(STATUS_LINE).responseDetail(STATUS_LINE)))
			.thrownStore(ts)
			.level(Level.OFF));
		// First log creates the entry with count=1; second invocation increments count to 2 and should null out the throwable.
		var ex = new RuntimeException("repeat");
		for (var i = 0; i < 2; i++) {
			var req = new MockHttpServletRequest("GET", "/err");
			req.setAttribute("Exception", ex);
			var res = new MockHttpServletResponse();
			res.setStatus(500);
			logger.log(req, res);
		}
		// On the second call, count > 1 so e is set to null before being passed to log().
		assertNull(logger.thrown.get());
		assertTrue(logger.message.get().contains(".2]"), () -> "expected count=2 in: " + logger.message.get());
	}

	@Test void e03_log_withoutThrownStore_keepsThrowable() {
		var logger = newCapturing(newBuilder()
			.normalRules(rule(x -> x.statusFilter(s -> s >= 500).level(Level.SEVERE).requestDetail(STATUS_LINE).responseDetail(STATUS_LINE)))
			.level(Level.OFF));
		var req = new MockHttpServletRequest("GET", "/err");
		var ex = new IllegalStateException("nope");
		req.setAttribute("Exception", ex);
		var res = new MockHttpServletResponse();
		res.setStatus(500);
		logger.log(req, res);
		assertSame(ex, logger.thrown.get());
		// No hash/count when ThrownStore is null.
		assertTrue(logger.message.get().startsWith("[500] "), () -> "msg=" + logger.message.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f - isEnabled / level OFF / disabled rule short-circuits
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_log_disabledRule_neverLogs() {
		var logger = newCapturing(newBuilder()
			.normalRules(rule(x -> x.statusFilter(s -> s >= 500).level(Level.SEVERE).enabled(Enablement.NEVER)))
			.level(Level.OFF));
		var req = new MockHttpServletRequest("GET", "/x");
		var res = new MockHttpServletResponse();
		res.setStatus(500);
		logger.log(req, res);
		assertNull(logger.message.get());
	}

	@Test void f02_log_levelOff_skipsLogging() {
		// Default rule has no level; logger-level=OFF means skip.
		var logger = newCapturing(newBuilder().level(Level.OFF));
		var req = new MockHttpServletRequest("GET", "/x");
		var res = new MockHttpServletResponse();
		res.setStatus(200);
		logger.log(req, res);
		assertNull(logger.message.get());
	}

	@Test void f03_log_conditionalEnabledTrue_logs() {
		var logger = newCapturing(newBuilder()
			.normalRules(rule(x -> x.statusFilter(s -> s >= 500).level(Level.SEVERE).enabled(Enablement.CONDITIONAL).enabledPredicate(r -> "GET".equals(r.getMethod()))))
			.level(Level.OFF));
		var req = new MockHttpServletRequest("GET", "/x");
		var res = new MockHttpServletResponse();
		res.setStatus(500);
		logger.log(req, res);
		assertNotNull(logger.message.get());
	}

	@Test void f04_log_conditionalEnabledFalse_skips() {
		var logger = newCapturing(newBuilder()
			.normalRules(rule(x -> x.statusFilter(s -> s >= 500).level(Level.SEVERE).enabled(Enablement.CONDITIONAL).enabledPredicate(r -> false)))
			.level(Level.OFF));
		var req = new MockHttpServletRequest("GET", "/x");
		var res = new MockHttpServletResponse();
		res.setStatus(500);
		logger.log(req, res);
		assertNull(logger.message.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// g - getRule with debug attribute selects debugRules vs normalRules
	//-----------------------------------------------------------------------------------------------------------------

	@Test void g01_log_debugAttributeTrue_selectsDebugRules() {
		var logger = newCapturing(newBuilder()
			.normalRules(rule(x -> x.statusFilter(s -> s >= 500).level(Level.WARNING)))
			.debugRules(rule(x -> x.level(Level.SEVERE).requestDetail(ENTITY).responseDetail(ENTITY)))
			.level(Level.OFF));
		var req = new MockHttpServletRequest("GET", "/d");
		req.setAttribute("Debug", Boolean.TRUE);
		var res = new MockHttpServletResponse();
		res.setStatus(200);  // Would NOT match normal rule, but debug rule has no filter and matches.
		logger.log(req, res);
		assertEquals(Level.SEVERE, logger.level.get());
		assertNotNull(logger.message.get());
		assertTrue(logger.message.get().contains("=== HTTP Call"));
	}

	@Test void g02_log_debugAttributeAbsent_usesNormalRules() {
		var logger = newCapturing(newBuilder()
			.normalRules(rule(x -> x.statusFilter(s -> s >= 500).level(Level.WARNING).requestDetail(STATUS_LINE).responseDetail(STATUS_LINE)))
			.debugRules(rule(x -> x.level(Level.SEVERE)))
			.level(Level.OFF));
		var req = new MockHttpServletRequest("GET", "/n");
		var res = new MockHttpServletResponse();
		res.setStatus(500);
		logger.log(req, res);
		assertEquals(Level.WARNING, logger.level.get());
	}

	@Test void g03_log_noRulesMatch_fallsBackToDefault() {
		// No rules; logger-level OFF -> short-circuits before logging.
		var logger = newCapturing(newBuilder().level(Level.OFF));
		var req = new MockHttpServletRequest("GET", "/x");
		var res = new MockHttpServletResponse();
		res.setStatus(404);
		logger.log(req, res);
		assertNull(logger.message.get());
	}

	@Test void g04_log_noRulesMatch_loggerLevelDrives() {
		// No rules; logger-level INFO drives the log at default STATUS_LINE detail.
		var logger = newCapturing(newBuilder().level(Level.INFO));
		var req = new MockHttpServletRequest("GET", "/x");
		var res = new MockHttpServletResponse();
		res.setStatus(204);
		logger.log(req, res);
		assertEquals(Level.INFO, logger.level.get());
		assertNotNull(logger.message.get());
		assertTrue(logger.message.get().contains("[204] HTTP GET /x"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// h - toString / properties
	//-----------------------------------------------------------------------------------------------------------------

	@Test void h01_toString_reflectsBuilderConfig() {
		var logger = new CapturingLogger(newBuilder().enabled(Enablement.NEVER).level(Level.WARNING));
		var s = logger.toString();
		assertNotNull(s);
		assertTrue(s.contains("NEVER"));
		assertTrue(s.contains("WARNING"));
	}
}
