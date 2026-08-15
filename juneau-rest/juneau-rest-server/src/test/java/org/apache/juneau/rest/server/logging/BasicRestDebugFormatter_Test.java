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
package org.apache.juneau.rest.server.logging;

import static java.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * Unit tests for {@link BasicRestDebugFormatter} — per-tier rendering, header redaction, and body truncation.
 *
 * @since 10.0.0
 */
@SuppressWarnings("resource") // Mockito mocks; nothing to close.
class BasicRestDebugFormatter_Test {

	private final BasicRestDebugFormatter f = new BasicRestDebugFormatter();

	private RestRequest req;
	private RestResponse res;
	private HttpServletRequest sreq;
	private HttpServletResponse sres;

	@BeforeEach void setUp() {
		req = mock(RestRequest.class);
		res = mock(RestResponse.class);
		sreq = mock(HttpServletRequest.class);
		sres = mock(HttpServletResponse.class);
		when(req.getHttpServletRequest()).thenReturn(sreq);
		when(res.getHttpServletResponse()).thenReturn(sres);
		when(sres.getStatus()).thenReturn(200);
		when(sreq.getMethod()).thenReturn("GET");
		when(sreq.getRequestURI()).thenReturn("/foo");
	}

	// -----------------------------------------------------------------------------------------
	// a — formatBasic
	// -----------------------------------------------------------------------------------------

	@Test void a01_formatBasic_statusLine() {
		assertEquals("[200] HTTP GET /foo", f.formatBasic(req, res));
	}

	// -----------------------------------------------------------------------------------------
	// b — formatHeaders
	// -----------------------------------------------------------------------------------------

	@Test void b01_formatHeaders_lengthsAndExecTime() {
		when(req.getCachedContentLength()).thenReturn(5L);
		when(res.getCachedContentLength()).thenReturn(10L);
		when(req.getExecTime()).thenReturn(42L);
		when(sreq.getHeaderNames()).thenReturn(emptyEnumeration());
		when(sres.getHeaderNames()).thenReturn(emptyList());

		var s = f.formatHeaders(req, res);
		assertTrue(s.contains("Request length: 5 bytes"), s);
		assertTrue(s.contains("Response code: 200"), s);
		assertTrue(s.contains("Response length: 10 bytes"), s);
		assertTrue(s.contains("Exec time: 42ms"), s);
	}

	@Test void b02_formatHeaders_redactsSensitive() {
		when(req.getCachedContentLength()).thenReturn(-1L);
		when(res.getCachedContentLength()).thenReturn(-1L);
		when(sreq.getHeaderNames()).thenReturn(enumeration(List.of("Authorization", "User-Agent")));
		when(sreq.getHeader("Authorization")).thenReturn("Bearer secret");
		when(sreq.getHeader("User-Agent")).thenReturn("curl/8.0");
		when(sres.getHeaderNames()).thenReturn(emptyList());

		var s = f.formatHeaders(req, res);
		assertTrue(s.contains("Authorization: [REDACTED]"), s);
		assertFalse(s.contains("Bearer secret"), s);
		assertTrue(s.contains("User-Agent: curl/8.0"), s);
	}

	// -----------------------------------------------------------------------------------------
	// c — formatBody
	// -----------------------------------------------------------------------------------------

	@Test void c01_formatBody_utf8AndHex() {
		when(req.getCachedContent()).thenReturn("hi".getBytes());
		when(req.getCachedContentLength()).thenReturn(2L);
		when(res.getCachedContent()).thenReturn(new byte[0]);
		when(res.getCachedContentLength()).thenReturn(0L);

		var s = f.formatBody(req, res);
		assertTrue(s.contains("---Request Content UTF-8---"), s);
		assertTrue(s.contains("hi"), s);
		assertTrue(s.contains("---Request Content Hex---"), s);
	}

	@Test void c02_formatBody_truncationMarker() {
		when(req.getCachedContent()).thenReturn("12".getBytes());
		when(req.getCachedContentLength()).thenReturn(10L);
		when(res.getCachedContent()).thenReturn(new byte[0]);
		when(res.getCachedContentLength()).thenReturn(0L);

		var s = f.formatBody(req, res);
		assertTrue(s.contains("truncated 8 bytes"), s);
	}

	@Test void c03_formatBody_emptyWhenNoContent() {
		when(req.getCachedContent()).thenReturn(null);
		when(res.getCachedContent()).thenReturn(null);
		assertEquals("", f.formatBody(req, res));
	}
}
