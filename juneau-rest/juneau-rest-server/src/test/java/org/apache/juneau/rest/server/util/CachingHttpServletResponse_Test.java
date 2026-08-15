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

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * Tests for {@link CachingHttpServletResponse} &mdash; lazy stream acquisition, capture-cap enforcement, the bulk
 * {@code write(byte[],int,int)} override, and the teeing {@link CachingHttpServletResponse#getWriter()} path.
 *
 * @since 10.0.0
 */
@SuppressWarnings("resource") // Mockito mocks / in-memory streams; nothing to close.
class CachingHttpServletResponse_Test {

	/** Minimal in-memory ServletOutputStream backed by a ByteArrayOutputStream for testing the tee path. */
	private static final class FakeServletOutputStream extends ServletOutputStream {
		final ByteArrayOutputStream sink = new ByteArrayOutputStream();
		boolean closed;

		@Override public void write(int b) { sink.write(b); }

		@Override public void write(byte[] b, int off, int len) { sink.write(b, off, len); }

		@Override public void close() { closed = true; }

		@Override public boolean isReady() { return true; }

		@Override public void setWriteListener(WriteListener listener) { /* no-op */ }
	}

	private static HttpServletResponse mockResponse(FakeServletOutputStream sink) throws IOException {
		var res = mock(HttpServletResponse.class);
		when(res.getOutputStream()).thenReturn(sink);
		return res;
	}

	// -----------------------------------------------------------------------------------------
	// a — wrap() / lazy stream acquisition / cap enforcement at capture time
	// -----------------------------------------------------------------------------------------

	@Test void a01_wrap_idempotent() throws IOException {
		var res = mockResponse(new FakeServletOutputStream());
		var wrapped = CachingHttpServletResponse.wrap(res, 10);
		assertSame(wrapped, CachingHttpServletResponse.wrap(wrapped, 10));
	}

	@Test void a02_constructor_doesNotEagerlyOpenStream() throws IOException {
		var res = mockResponse(new FakeServletOutputStream());
		CachingHttpServletResponse.wrap(res, 10);
		verify(res, never()).getOutputStream();
	}

	@Test void a03_capLimitsCapturedBytes_downstreamStillGetsEverything() throws IOException {
		var sink = new FakeServletOutputStream();
		var res = mockResponse(sink);
		var wrapped = CachingHttpServletResponse.wrap(res, 4);

		wrapped.getOutputStream().write("0123456789".getBytes());

		assertArrayEquals("0123456789".getBytes(), sink.sink.toByteArray());
		assertArrayEquals("0123".getBytes(), wrapped.getContent());
		assertEquals(10, wrapped.getTotalLength());
	}

	@Test void a04_bulkWrite_capturedInSingleCall() throws IOException {
		var sink = new FakeServletOutputStream();
		var res = mockResponse(sink);
		var wrapped = CachingHttpServletResponse.wrap(res, 100);

		wrapped.getOutputStream().write("abc".getBytes(), 0, 3);

		assertArrayEquals("abc".getBytes(), wrapped.getContent());
		assertEquals(3, wrapped.getTotalLength());
	}

	@Test void a05_getOutputStream_cachedOnRepeatedCalls() throws IOException {
		var res = mockResponse(new FakeServletOutputStream());
		var wrapped = CachingHttpServletResponse.wrap(res, 100);
		assertSame(wrapped.getOutputStream(), wrapped.getOutputStream());
	}

	// -----------------------------------------------------------------------------------------
	// b — getWriter() teeing
	// -----------------------------------------------------------------------------------------

	@Test void b01_getWriter_teesThroughCapturedStream() throws IOException {
		var sink = new FakeServletOutputStream();
		var res = mockResponse(sink);
		when(res.getCharacterEncoding()).thenReturn("UTF-8");
		var wrapped = CachingHttpServletResponse.wrap(res, 100);

		wrapped.getWriter().write("hello");
		wrapped.getWriter().flush();

		assertEquals("hello", sink.sink.toString(StandardCharsets.UTF_8));
		assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), wrapped.getContent());
	}

	@Test void b02_getWriter_cachedOnRepeatedCalls() throws IOException {
		var res = mockResponse(new FakeServletOutputStream());
		var wrapped = CachingHttpServletResponse.wrap(res, 100);
		assertSame(wrapped.getWriter(), wrapped.getWriter());
	}

	@Test void b03_getWriter_afterOutputStream_doesNotThrow() throws IOException {
		// Real servlet containers throw IllegalStateException calling getWriter() after getOutputStream() on the
		// SAME response — proving the tee's getWriter() routes through its OWN getOutputStream(), not the raw one.
		var res = mockResponse(new FakeServletOutputStream());
		var wrapped = CachingHttpServletResponse.wrap(res, 100);
		wrapped.getOutputStream();
		assertDoesNotThrow(wrapped::getWriter);
	}

	@Test void b04_getWriter_respectsCap() throws IOException {
		var sink = new FakeServletOutputStream();
		var res = mockResponse(sink);
		when(res.getCharacterEncoding()).thenReturn("UTF-8");
		var wrapped = CachingHttpServletResponse.wrap(res, 4);

		wrapped.getWriter().write("0123456789");
		wrapped.getWriter().flush();

		assertEquals("0123456789", sink.sink.toString(StandardCharsets.UTF_8), "downstream must see the full body");
		assertArrayEquals("0123".getBytes(), wrapped.getContent(), "captured bytes must be capped");
	}
}
