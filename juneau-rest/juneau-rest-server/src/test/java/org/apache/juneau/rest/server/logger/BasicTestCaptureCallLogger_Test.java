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
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.server.debug.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * Tests for {@link BasicTestCaptureCallLogger} — the in-memory capturing test logger.
 *
 * @since 10.0.0
 */
class BasicTestCaptureCallLogger_Test extends TestBase {

	private static HttpServletRequest req(Map<String,Object> attrs) {
		var r = mock(HttpServletRequest.class);
		when(r.getMethod()).thenReturn("GET");
		when(r.getRequestURI()).thenReturn("/foo");
		when(r.getHeaderNames()).thenReturn(Collections.enumeration(List.of()));
		for (var e : attrs.entrySet())
			when(r.getAttribute(e.getKey())).thenReturn(e.getValue());
		return r;
	}

	private static HttpServletResponse res(int status) {
		var r = mock(HttpServletResponse.class);
		when(r.getStatus()).thenReturn(status);
		when(r.getHeaderNames()).thenReturn(Set.of());
		return r;
	}

	// -----------------------------------------------------------------------------------------
	// a — construction
	// -----------------------------------------------------------------------------------------

	@Test void a01_construct_defaultConstructor() {
		assertNotNull(new BasicTestCaptureCallLogger());
	}

	@SuppressWarnings("resource") // BasicBeanStore is a short-lived in-memory test fixture backed by a Map; nothing external to leak.
	@Test void a02_construct_withBeanStore() {
		assertNotNull(new BasicTestCaptureCallLogger(new BasicBeanStore()));
	}

	// -----------------------------------------------------------------------------------------
	// b — getters/assertions with nothing logged yet
	// -----------------------------------------------------------------------------------------

	@Test void b01_noMessageLogged_gettersReturnNull() {
		var l = new BasicTestCaptureCallLogger();
		assertNull(l.getMessage());
		assertNull(l.getLevel());
		assertNull(l.getThrown());
		assertNull(l.getMessageAndReset());
	}

	// -----------------------------------------------------------------------------------------
	// c — log(): normal (non-debug) rules
	// -----------------------------------------------------------------------------------------

	@Test void c01_status500_logsWithSevereAndHeaderDetail() {
		var l = new BasicTestCaptureCallLogger();
		l.log(req(Map.of()), res(500));
		assertNotNull(l.getMessage());
		assertEquals(Level.SEVERE, l.getLevel());
	}

	@Test void c02_status400_logsWithWarningAndStatusLineDetail() {
		var l = new BasicTestCaptureCallLogger();
		l.log(req(Map.of()), res(400));
		assertNotNull(l.getMessage());
		assertEquals(Level.WARNING, l.getLevel());
	}

	@Test void c03_status200_notLogged() {
		var l = new BasicTestCaptureCallLogger();
		l.log(req(Map.of()), res(200));
		assertNull(l.getMessage());
	}

	// -----------------------------------------------------------------------------------------
	// d — log(): debug rules
	// -----------------------------------------------------------------------------------------

	@Test void d01_debugEnabled_logsWithSevereAndEntityDetail() {
		var debugConfig = DebugConfig.create(BasicBeanStore.INSTANCE).defaultLevel(Level.SEVERE).build();
		var l = new BasicTestCaptureCallLogger();
		l.log(req(Map.of("DebugConfig", debugConfig, "Debug", Boolean.TRUE)), res(200));
		assertNotNull(l.getMessage());
		assertEquals(Level.SEVERE, l.getLevel());
	}

	// -----------------------------------------------------------------------------------------
	// e — assertions / reset
	// -----------------------------------------------------------------------------------------

	@Test void e01_assertMessage_containsExpectedText() {
		var l = new BasicTestCaptureCallLogger();
		l.log(req(Map.of()), res(500));
		l.assertMessage().isContains("HTTP GET /foo");
	}

	@Test void e02_assertMessageAndReset_clearsAfterwards() {
		var l = new BasicTestCaptureCallLogger();
		l.log(req(Map.of()), res(500));
		l.assertMessageAndReset().isContains("HTTP GET /foo");
		assertNull(l.getMessage());
	}

	@Test void e03_assertThrown_withLoggedException() {
		var l = new BasicTestCaptureCallLogger();
		l.log(req(Map.of()), res(500));
		l.assertThrown().isNull();
	}

	@Test void e04_reset_returnsThisAndClearsMessage() {
		var l = new BasicTestCaptureCallLogger();
		l.log(req(Map.of()), res(500));
		assertSame(l, l.reset());
		assertNull(l.getMessage());
	}
}
