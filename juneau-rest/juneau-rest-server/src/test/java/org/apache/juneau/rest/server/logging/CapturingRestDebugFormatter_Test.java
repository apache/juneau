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

import java.util.logging.*;

import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * Unit tests for {@link CapturingRestDebugFormatter} — message capture, tier inference, and thrown capture.
 *
 * @since 10.0.0
 */
@SuppressWarnings("resource") // Mockito mocks; nothing to close.
class CapturingRestDebugFormatter_Test {

	private final CapturingRestDebugFormatter f = new CapturingRestDebugFormatter();

	private RestRequest req;
	private RestResponse res;

	@BeforeEach void setUp() {
		req = mock(RestRequest.class);
		res = mock(RestResponse.class);
		var sreq = mock(HttpServletRequest.class);
		var sres = mock(HttpServletResponse.class);
		when(req.getHttpServletRequest()).thenReturn(sreq);
		when(res.getHttpServletResponse()).thenReturn(sres);
		when(sres.getStatus()).thenReturn(200);
		when(sreq.getMethod()).thenReturn("GET");
		when(sreq.getRequestURI()).thenReturn("/foo");
		when(sreq.getHeaderNames()).thenReturn(emptyEnumeration());
		when(sres.getHeaderNames()).thenReturn(emptyList());
		when(req.getCachedContentLength()).thenReturn(-1L);
		when(res.getCachedContentLength()).thenReturn(-1L);
	}

	@Test void a01_capturesBasicMessage_atInfoTier() {
		f.formatBasic(req, res);
		assertEquals("[200] HTTP GET /foo", f.getMessage());
		assertEquals(Level.INFO, f.getLevel());
	}

	@Test void a02_headersRaiseTierToFine() {
		f.formatBasic(req, res);
		f.formatHeaders(req, res);
		assertEquals(Level.FINE, f.getLevel());
		assertTrue(f.getMessage().startsWith("[200] HTTP GET /foo"));
	}

	@Test void a03_bodyRaisesTierToFinest() {
		f.formatBasic(req, res);
		f.formatHeaders(req, res);
		f.formatBody(req, res);
		assertEquals(Level.FINEST, f.getLevel());
	}

	@Test void a04_capturesThrown() {
		var t = new RuntimeException("boom");
		when(req.getException()).thenReturn(t);
		f.formatBasic(req, res);
		assertSame(t, f.getThrown());
	}

	@Test void a05_getMessageAndReset_clears() {
		f.formatBasic(req, res);
		assertNotNull(f.getMessageAndReset());
		assertNull(f.getMessage());
		assertNull(f.getLevel());
	}
}
