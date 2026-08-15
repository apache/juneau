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
package org.apache.juneau.rest.server.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.nio.charset.*;

import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * Tests for {@link CachingHttpServletRequest} &mdash; capture-cap enforcement (independent of downstream
 * consumption) and the teeing {@link CachingHttpServletRequest#getReader()} path.
 *
 * @since 10.0.0
 */
@SuppressWarnings("resource") // Mockito mocks / in-memory streams; nothing to close.
class CachingHttpServletRequest_Test {

	private static HttpServletRequest mockRequest(byte[] content) throws IOException {
		var req = mock(HttpServletRequest.class);
		when(req.getInputStream()).thenReturn(new BoundedServletInputStream(content));
		return req;
	}

	// -----------------------------------------------------------------------------------------
	// a — wrap() / cap enforcement at capture time
	// -----------------------------------------------------------------------------------------

	@Test void a01_wrap_idempotent() throws IOException {
		var req = mockRequest("abc".getBytes());
		var wrapped = CachingHttpServletRequest.wrap(req, 10);
		assertSame(wrapped, CachingHttpServletRequest.wrap(wrapped, 10));
	}

	@Test void a02_wrap_defaultCap() throws IOException {
		var req = mockRequest("abc".getBytes());
		var wrapped = CachingHttpServletRequest.wrap(req);
		wrapped.getInputStream().readAllBytes();
		assertEquals(CachingHttpServletRequest.DEFAULT_CAP, 8 * 1024);
		assertArrayEquals("abc".getBytes(), wrapped.getContent());
	}

	@Test void a03_capLimitsCapturedBytes_downstreamStillGetsEverything() throws IOException {
		var content = "0123456789".getBytes();
		var req = mockRequest(content);
		var wrapped = CachingHttpServletRequest.wrap(req, 4);

		var read = wrapped.getInputStream().readAllBytes();

		assertArrayEquals(content, read);
		assertArrayEquals("0123".getBytes(), wrapped.getContent());
		assertEquals(10, wrapped.getTotalLength());
	}

	@Test void a04_capZero_disablesCaptureButNotDownstream() throws IOException {
		var content = "0123456789".getBytes();
		var req = mockRequest(content);
		var wrapped = CachingHttpServletRequest.wrap(req, 0);

		var read = wrapped.getInputStream().readAllBytes();

		assertArrayEquals(content, read);
		assertEquals(0, wrapped.getContent().length);
		assertEquals(10, wrapped.getTotalLength());
	}

	@Test void a05_getInputStream_cachedOnRepeatedCalls() throws IOException {
		var req = mockRequest("abc".getBytes());
		var wrapped = CachingHttpServletRequest.wrap(req, 10);
		assertSame(wrapped.getInputStream(), wrapped.getInputStream());
	}

	// -----------------------------------------------------------------------------------------
	// b — getReader() teeing
	// -----------------------------------------------------------------------------------------

	@Test void b01_getReader_teesThroughCapturedStream() throws IOException {
		var content = "hello world".getBytes(StandardCharsets.UTF_8);
		var req = mockRequest(content);
		when(req.getCharacterEncoding()).thenReturn(null);
		var wrapped = CachingHttpServletRequest.wrap(req, 100);

		var line = wrapped.getReader().readLine();

		assertEquals("hello world", line);
		assertArrayEquals(content, wrapped.getContent());
		assertEquals(11, wrapped.getTotalLength());
	}

	@Test void b02_getReader_honorsCharacterEncoding() throws IOException {
		var content = "h\u00e9llo".getBytes(StandardCharsets.UTF_8);
		var req = mockRequest(content);
		when(req.getCharacterEncoding()).thenReturn("UTF-8");
		var wrapped = CachingHttpServletRequest.wrap(req, 100);

		assertEquals("h\u00e9llo", wrapped.getReader().readLine());
	}

	@Test void b03_getReader_cachedOnRepeatedCalls() throws IOException {
		var req = mockRequest("abc".getBytes());
		var wrapped = CachingHttpServletRequest.wrap(req, 10);
		assertSame(wrapped.getReader(), wrapped.getReader());
	}

	@Test void b04_getReader_respectsCap() throws IOException {
		var content = "0123456789".getBytes(StandardCharsets.UTF_8);
		var req = mockRequest(content);
		when(req.getCharacterEncoding()).thenReturn(null);
		var wrapped = CachingHttpServletRequest.wrap(req, 4);

		var line = wrapped.getReader().readLine();

		assertEquals("0123456789", line, "downstream reader must see the full body");
		assertArrayEquals("0123".getBytes(), wrapped.getContent(), "captured bytes must be capped");
	}
}
