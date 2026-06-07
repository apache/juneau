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

import static org.junit.jupiter.api.Assertions.*;

import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link CallLoggerRule#matches}, {@link BasicCallLogger}, {@link BasicDisabledCallLogger},
 * and {@link BasicTestCaptureCallLogger}.
 */
class RestLogger_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// a - CallLoggerRule.matches() with status filters
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_ruleMatchesStatusFilter_ge500() throws Exception {
		var rule = CallLoggerRule.create(BasicBeanStore.INSTANCE)
			.statusFilter(x -> x >= 500)
			.build();
		var req = new org.springframework.mock.web.MockHttpServletRequest();
		var res = new org.springframework.mock.web.MockHttpServletResponse();
		res.setStatus(500);
		assertTrue(rule.matches(req, res));
	}

	@Test void a02_ruleDoesNotMatchStatusFilter_below500() throws Exception {
		var rule = CallLoggerRule.create(BasicBeanStore.INSTANCE)
			.statusFilter(x -> x >= 500)
			.build();
		var req = new org.springframework.mock.web.MockHttpServletRequest();
		var res = new org.springframework.mock.web.MockHttpServletResponse();
		res.setStatus(400);
		assertFalse(rule.matches(req, res));
	}

	@Test void a03_ruleMatchesStatusFilter_ge400() throws Exception {
		var rule = CallLoggerRule.create(BasicBeanStore.INSTANCE)
			.statusFilter(x -> x >= 400)
			.build();
		var req = new org.springframework.mock.web.MockHttpServletRequest();
		var res = new org.springframework.mock.web.MockHttpServletResponse();
		res.setStatus(404);
		assertTrue(rule.matches(req, res));
	}

	@Test void a04_ruleNoFilter_matchesEverything() throws Exception {
		var rule = CallLoggerRule.create(BasicBeanStore.INSTANCE).build();
		var req = new org.springframework.mock.web.MockHttpServletRequest();
		var res = new org.springframework.mock.web.MockHttpServletResponse();
		res.setStatus(200);
		assertTrue(rule.matches(req, res));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - CallLoggerRule.matches() with request filters
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_requestFilter_matches() throws Exception {
		var rule = CallLoggerRule.create(BasicBeanStore.INSTANCE)
			.requestFilter(x -> "GET".equals(x.getMethod()))
			.build();
		var req = new org.springframework.mock.web.MockHttpServletRequest("GET", "/foo");
		var res = new org.springframework.mock.web.MockHttpServletResponse();
		assertTrue(rule.matches(req, res));
	}

	@Test void b02_requestFilter_doesNotMatch() throws Exception {
		var rule = CallLoggerRule.create(BasicBeanStore.INSTANCE)
			.requestFilter(x -> "GET".equals(x.getMethod()))
			.build();
		var req = new org.springframework.mock.web.MockHttpServletRequest("POST", "/foo");
		var res = new org.springframework.mock.web.MockHttpServletResponse();
		assertFalse(rule.matches(req, res));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c - CallLoggerRule.matches() with response filters
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_responseFilter_matches() throws Exception {
		var rule = CallLoggerRule.create(BasicBeanStore.INSTANCE)
			.responseFilter(x -> x.getStatus() >= 500)
			.build();
		var req = new org.springframework.mock.web.MockHttpServletRequest();
		var res = new org.springframework.mock.web.MockHttpServletResponse();
		res.setStatus(503);
		assertTrue(rule.matches(req, res));
	}

	@Test void c02_responseFilter_doesNotMatch() throws Exception {
		var rule = CallLoggerRule.create(BasicBeanStore.INSTANCE)
			.responseFilter(x -> x.getStatus() >= 500)
			.build();
		var req = new org.springframework.mock.web.MockHttpServletRequest();
		var res = new org.springframework.mock.web.MockHttpServletResponse();
		res.setStatus(200);
		assertFalse(rule.matches(req, res));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d - CallLoggerRule.matches() with exception filter
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_exceptionFilter_matchesWhenExceptionPresent() throws Exception {
		var rule = CallLoggerRule.create(BasicBeanStore.INSTANCE)
			.exceptionFilter(x -> x instanceof IllegalArgumentException)
			.build();
		var req = new org.springframework.mock.web.MockHttpServletRequest();
		req.setAttribute("Exception", new IllegalArgumentException("test"));
		var res = new org.springframework.mock.web.MockHttpServletResponse();
		assertTrue(rule.matches(req, res));
	}

	@Test void d02_exceptionFilter_doesNotMatchWrongType() throws Exception {
		var rule = CallLoggerRule.create(BasicBeanStore.INSTANCE)
			.exceptionFilter(x -> x instanceof IllegalArgumentException)
			.build();
		var req = new org.springframework.mock.web.MockHttpServletRequest();
		req.setAttribute("Exception", new RuntimeException("test"));
		var res = new org.springframework.mock.web.MockHttpServletResponse();
		assertFalse(rule.matches(req, res));
	}

	@Test void d03_exceptionFilter_matchesWhenNoException() throws Exception {
		// When no exception is present, the exception filter is not evaluated; the rule still matches.
		var rule = CallLoggerRule.create(BasicBeanStore.INSTANCE)
			.exceptionFilter(x -> x instanceof IllegalArgumentException)
			.build();
		var req = new org.springframework.mock.web.MockHttpServletRequest();
		var res = new org.springframework.mock.web.MockHttpServletResponse();
		assertTrue(rule.matches(req, res));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e - CallLoggerRule.matches() with combined filters
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_combinedFilters_allMatch() throws Exception {
		var rule = CallLoggerRule.create(BasicBeanStore.INSTANCE)
			.statusFilter(x -> x >= 500)
			.requestFilter(x -> "GET".equals(x.getMethod()))
			.build();
		var req = new org.springframework.mock.web.MockHttpServletRequest("GET", "/error");
		var res = new org.springframework.mock.web.MockHttpServletResponse();
		res.setStatus(500);
		assertTrue(rule.matches(req, res));
	}

	@Test void e02_combinedFilters_statusMissesRequestMatches() throws Exception {
		var rule = CallLoggerRule.create(BasicBeanStore.INSTANCE)
			.statusFilter(x -> x >= 500)
			.requestFilter(x -> "GET".equals(x.getMethod()))
			.build();
		var req = new org.springframework.mock.web.MockHttpServletRequest("GET", "/ok");
		var res = new org.springframework.mock.web.MockHttpServletResponse();
		res.setStatus(200);
		assertFalse(rule.matches(req, res));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f - CallLoggerRule getter methods
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_getLevel() throws Exception {
		var rule = CallLoggerRule.create(BasicBeanStore.INSTANCE)
			.level(Level.SEVERE)
			.build();
		assertEquals(Level.SEVERE, rule.getLevel());
	}

	@Test void f02_getLevel_null() throws Exception {
		var rule = CallLoggerRule.create(BasicBeanStore.INSTANCE).build();
		assertNull(rule.getLevel());
	}

	@Test void f03_getRequestDetail() throws Exception {
		var rule = CallLoggerRule.create(BasicBeanStore.INSTANCE)
			.requestDetail(CallLoggingDetail.ENTITY)
			.build();
		assertEquals(CallLoggingDetail.ENTITY, rule.getRequestDetail());
	}

	@Test void f04_getResponseDetail() throws Exception {
		var rule = CallLoggerRule.create(BasicBeanStore.INSTANCE)
			.responseDetail(CallLoggingDetail.HEADER)
			.build();
		assertEquals(CallLoggingDetail.HEADER, rule.getResponseDetail());
	}

	@Test void f05_getEnabled() throws Exception {
		var rule = CallLoggerRule.create(BasicBeanStore.INSTANCE)
			.enabled(Enablement.NEVER)
			.build();
		assertEquals(Enablement.NEVER, rule.getEnabled());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// g - BasicTestCaptureCallLogger captures log messages
	//-----------------------------------------------------------------------------------------------------------------

	@Test void g01_captureLogger_logs500() throws Exception {
		var logger = new BasicTestCaptureCallLogger();
		// Simulate a log call directly
		var req = new org.springframework.mock.web.MockHttpServletRequest("GET", "/error");
		var res = new org.springframework.mock.web.MockHttpServletResponse();
		res.setStatus(500);
		logger.log(req, res);
		assertNotNull(logger.getMessage());
		assertTrue(logger.getMessage().contains("[500]"));
		assertTrue(logger.getMessage().contains("HTTP GET /error"));
		assertEquals(Level.SEVERE, logger.getLevel());
	}

	@Test void g02_captureLogger_noLog200() throws Exception {
		var logger = new BasicTestCaptureCallLogger();
		var req = new org.springframework.mock.web.MockHttpServletRequest("GET", "/ok");
		var res = new org.springframework.mock.web.MockHttpServletResponse();
		res.setStatus(200);
		logger.log(req, res);
		// 200 does not match any normal rule (>=400 or >=500)
		assertNull(logger.getMessage());
	}

	@Test void g03_captureLogger_logs400AsWarning() throws Exception {
		var logger = new BasicTestCaptureCallLogger();
		var req = new org.springframework.mock.web.MockHttpServletRequest("POST", "/bad");
		var res = new org.springframework.mock.web.MockHttpServletResponse();
		res.setStatus(400);
		logger.log(req, res);
		assertNotNull(logger.getMessage());
		assertTrue(logger.getMessage().contains("[400]"));
		assertEquals(Level.WARNING, logger.getLevel());
	}

	@Test void g04_captureLogger_reset() throws Exception {
		var logger = new BasicTestCaptureCallLogger();
		var req = new org.springframework.mock.web.MockHttpServletRequest("GET", "/error");
		var res = new org.springframework.mock.web.MockHttpServletResponse();
		res.setStatus(500);
		logger.log(req, res);
		assertNotNull(logger.getMessage());
		logger.reset();
		assertNull(logger.getMessage());
	}

	@Test void g05_captureLogger_getMessageAndReset() throws Exception {
		var logger = new BasicTestCaptureCallLogger();
		var req = new org.springframework.mock.web.MockHttpServletRequest("GET", "/error");
		var res = new org.springframework.mock.web.MockHttpServletResponse();
		res.setStatus(500);
		logger.log(req, res);
		var msg = logger.getMessageAndReset();
		assertNotNull(msg);
		assertTrue(msg.contains("[500]"));
		assertNull(logger.getMessage());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// h - BasicDisabledCallLogger never logs
	//-----------------------------------------------------------------------------------------------------------------

	@Test void h01_disabledLogger_neverLogs() throws Exception {
		var logger = new BasicDisabledCallLogger(BasicBeanStore.INSTANCE);
		var req = new org.springframework.mock.web.MockHttpServletRequest("GET", "/error");
		var res = new org.springframework.mock.web.MockHttpServletResponse();
		res.setStatus(500);
		// Disabled logger should not throw and should not produce output
		logger.log(req, res);
		// No exception = pass. Disabled logger has no observable output to verify directly
		// but we confirm it does not throw.
	}

	//-----------------------------------------------------------------------------------------------------------------
	// i - CallLoggingDetail.fromString
	//-----------------------------------------------------------------------------------------------------------------

	@Test void i01_fromString_statusLine() throws Exception {
		assertEquals(CallLoggingDetail.STATUS_LINE, CallLoggingDetail.fromString("STATUS_LINE"));
	}

	@Test void i02_fromString_caseInsensitive() throws Exception {
		assertEquals(CallLoggingDetail.HEADER, CallLoggingDetail.fromString("header"));
	}

	@Test void i03_fromString_null() throws Exception {
		assertNull(CallLoggingDetail.fromString(null));
	}

	@Test void i04_fromString_invalid() throws Exception {
		assertNull(CallLoggingDetail.fromString("INVALID"));
	}

	@Test void i05_fromString_entity() throws Exception {
		assertEquals(CallLoggingDetail.ENTITY, CallLoggingDetail.fromString("ENTITY"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// j - CallLoggerRule.toString and CallLogger.toString
	//-----------------------------------------------------------------------------------------------------------------

	@Test void j01_ruleToString_includesLevel() throws Exception {
		var rule = CallLoggerRule.create(BasicBeanStore.INSTANCE)
			.level(Level.WARNING)
			.requestDetail(CallLoggingDetail.HEADER)
			.build();
		var s = rule.toString();
		assertNotNull(s);
		assertTrue(s.contains("WARNING"));
	}

	@Test void j02_loggerToString_includesEnabled() throws Exception {
		var logger = CallLogger.create(BasicBeanStore.INSTANCE).build();
		var s = logger.toString();
		assertNotNull(s);
		assertTrue(s.contains("ALWAYS"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// k - CallLoggingDetail.isOneOf
	//-----------------------------------------------------------------------------------------------------------------

	@Test void k01_isOneOf_matches() throws Exception {
		assertTrue(CallLoggingDetail.HEADER.isOneOf(CallLoggingDetail.STATUS_LINE, CallLoggingDetail.HEADER));
	}

	@Test void k02_isOneOf_noMatch() throws Exception {
		assertFalse(CallLoggingDetail.ENTITY.isOneOf(CallLoggingDetail.STATUS_LINE, CallLoggingDetail.HEADER));
	}
}
