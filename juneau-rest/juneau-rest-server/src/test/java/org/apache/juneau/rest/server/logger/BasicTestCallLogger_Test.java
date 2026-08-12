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
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * Tests for {@link BasicTestCallLogger} — the no-trace-aware default test logger.
 *
 * @since 10.0.0
 */
class BasicTestCallLogger_Test extends TestBase {

	private static HttpServletRequest req(Map<String,String> headers, Map<String,String> attrs, String queryString) {
		var r = mock(HttpServletRequest.class);
		when(r.getMethod()).thenReturn("GET");
		when(r.getRequestURI()).thenReturn("/foo");
		when(r.getHeaderNames()).thenReturn(Collections.enumeration(headers.keySet()));
		for (var e : headers.entrySet())
			when(r.getHeader(e.getKey())).thenReturn(e.getValue());
		for (var e : attrs.entrySet())
			when(r.getAttribute(e.getKey())).thenReturn(e.getValue());
		when(r.getQueryString()).thenReturn(queryString);
		return r;
	}

	private static HttpServletResponse res(int status) {
		var r = mock(HttpServletResponse.class);
		when(r.getStatus()).thenReturn(status);
		when(r.getHeaderNames()).thenReturn(Set.of());
		return r;
	}

	private static final class CapturingLogger extends BasicTestCallLogger {
		String captured;
		Level capturedLevel;

		CapturingLogger(BeanStore beanStore) {
			super(beanStore);
		}

		@Override
		protected void log(Level level, String msg, Throwable e) {
			captured = msg;
			capturedLevel = level;
		}
	}

	@SuppressWarnings("resource") // BasicBeanStore is a short-lived in-memory test fixture backed by a Map; nothing external to leak.
	private static CapturingLogger logger() {
		return new CapturingLogger(new BasicBeanStore());
	}

	// -----------------------------------------------------------------------------------------
	// a — construction (exercises the init()/Builder-chain instructions)
	// -----------------------------------------------------------------------------------------

	@Test void a01_construct_doesNotThrow() {
		assertNotNull(logger());
	}

	// -----------------------------------------------------------------------------------------
	// b — isNoTrace via enabledPredicate: 500+ rule (SEVERE, HEADER detail)
	// -----------------------------------------------------------------------------------------

	@Test void b01_status500_noTraceAttributeTrue_notLogged() {
		var l = logger();
		l.log(req(Map.of(), Map.of("NoTrace", "true"), null), res(500));
		assertNull(l.captured);
	}

	@Test void b02_status500_noTraceAttributeFalse_logged() {
		var l = logger();
		l.log(req(Map.of(), Map.of("NoTrace", "false"), null), res(500));
		assertNotNull(l.captured);
		assertEquals(Level.SEVERE, l.capturedLevel);
	}

	@Test void b03_status500_noTraceHeaderTrue_notLogged() {
		var l = logger();
		l.log(req(Map.of("No-Trace", "true"), Map.of(), null), res(500));
		assertNull(l.captured);
	}

	@Test void b04_status500_noTraceHeaderFalse_logged() {
		var l = logger();
		l.log(req(Map.of("No-Trace", "false"), Map.of(), null), res(500));
		assertNotNull(l.captured);
	}

	@Test void b05_status500_noTraceQueryParamTrue_notLogged() {
		var l = logger();
		l.log(req(Map.of(), Map.of(), "noTrace=true"), res(500));
		assertNull(l.captured);
	}

	@Test void b06_status500_noNoTraceIndicators_logged() {
		var l = logger();
		l.log(req(Map.of(), Map.of(), null), res(500));
		assertNotNull(l.captured);
	}

	// -----------------------------------------------------------------------------------------
	// c — status 400-499: the first rule's statusFilter is "x >= 500", so 400-499 responses fall
	// through to the second (WARNING) rule, matching the class-level Javadoc's documented intent.
	// -----------------------------------------------------------------------------------------

	@Test void c01_status400_noTraceAttributeTrue_notLogged() {
		var l = logger();
		l.log(req(Map.of(), Map.of("NoTrace", "true"), null), res(400));
		assertNull(l.captured);
	}

	@Test void c02_status400_noNoTraceIndicators_logged() {
		var l = logger();
		l.log(req(Map.of(), Map.of(), null), res(400));
		assertNotNull(l.captured);
		assertEquals(Level.WARNING, l.capturedLevel);
	}

	// -----------------------------------------------------------------------------------------
	// d — status &lt; 400: neither normal rule matches
	// -----------------------------------------------------------------------------------------

	@Test void d01_status200_notLogged() {
		var l = logger();
		l.log(req(Map.of(), Map.of(), null), res(200));
		assertNull(l.captured);
	}
}
