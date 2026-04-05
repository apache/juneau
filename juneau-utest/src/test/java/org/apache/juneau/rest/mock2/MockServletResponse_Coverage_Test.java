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
package org.apache.juneau.rest.mock2;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

class MockServletResponse_Coverage_Test extends TestBase {

	@Test void a01_setBufferSize() {
		var res = new MockServletResponse();
		res.setBufferSize(1024);
		assertEquals(1024, res.getBufferSize());
	}

	@Test void a02_setCharacterEncoding() {
		var res = new MockServletResponse();
		res.setCharacterEncoding("ISO-8859-1");
		assertEquals("ISO-8859-1", res.getCharacterEncoding());
	}

	@Test void a03_setDateHeader() {
		var res = new MockServletResponse();
		// Epoch: Thu, 01 Jan 1970 00:00:00 GMT
		res.setDateHeader("Date", 0L);
		var header = res.getHeader("Date");
		assertNotNull(header);
		assertTrue(header.contains("1970"));
	}

	@Test void a04_setIntHeader() {
		var res = new MockServletResponse();
		res.setIntHeader("X-Count", 42);
		assertEquals("42", res.getHeader("X-Count"));
	}

	@Test void a05_setContentLength() {
		var res = new MockServletResponse();
		res.setContentLength(512);
		assertEquals("512", res.getHeader("Content-Length"));
	}

	@Test void a06_setContentLengthLong() {
		var res = new MockServletResponse();
		res.setContentLengthLong(1024L);
		assertEquals("1024", res.getHeader("Content-Length"));
	}

	@Test void a07_updateContentTypeHeader_utf8_noCharsetAppended() {
		// Default charset is UTF-8; setting content type should NOT append ";charset=UTF-8"
		var res = new MockServletResponse();
		res.setContentType("text/html");
		assertEquals("text/html", res.getContentType());
	}

	@Test void a08_updateContentTypeHeader_nonUtf8_charsetAppended() {
		// Non-UTF-8 charset should be appended to the content type
		var res = new MockServletResponse();
		res.setCharacterEncoding("ISO-8859-1");
		res.setContentType("text/html");
		assertEquals("text/html;charset=ISO-8859-1", res.getContentType());
	}

	@Test void a09_updateContentTypeHeader_existingCharsetReplaced() {
		// Content type already contains "charset="; it should be stripped and replaced
		var res = new MockServletResponse();
		res.setContentType("text/html;charset=windows-1252");
		res.setCharacterEncoding("UTF-16");
		// After setCharacterEncoding: strip existing "charset=windows-1252", append ";charset=UTF-16"
		assertEquals("text/html;charset=UTF-16", res.getContentType());
	}

	@Test void a10_getHeaders_null() {
		var res = new MockServletResponse();
		// getHeaders for non-existent header → empty collection
		var headers = res.getHeaders("X-Missing");
		assertNotNull(headers);
		assertTrue(headers.isEmpty());
	}

	@Test void a11_getHeaders_existing() {
		var res = new MockServletResponse();
		res.setHeader("X-Custom", "value1");
		var headers = res.getHeaders("X-Custom");
		assertNotNull(headers);
		assertFalse(headers.isEmpty());
	}

	@Test void a12_getLocale() {
		var res = new MockServletResponse();
		res.setLocale(java.util.Locale.FRANCE);
		assertEquals(java.util.Locale.FRANCE, res.getLocale());
	}

	@Test void a13_isCommitted() {
		var res = new MockServletResponse();
		assertFalse(res.isCommitted());
	}

	@Test void a14_reset() {
		var res = new MockServletResponse();
		assertDoesNotThrow(() -> res.reset());
	}

	@Test void a15_resetBuffer() {
		var res = new MockServletResponse();
		assertDoesNotThrow(() -> res.resetBuffer());
	}

	@Test void a16_sendError_int() throws Exception {
		var res = new MockServletResponse();
		res.sendError(404);
		assertEquals(404, res.getStatus());
	}

	@Test void a17_sendError_intAndMsg() throws Exception {
		var res = new MockServletResponse();
		res.sendError(500, "Server Error");
		assertEquals(500, res.getStatus());
		assertEquals("Server Error", res.getMessage());
	}

	@Test void a18_sendRedirect() throws Exception {
		var res = new MockServletResponse();
		res.sendRedirect("http://example.com");
		assertEquals(302, res.getStatus());
		assertEquals("http://example.com", res.getHeader("Location"));
	}

	@Test void a19_sendRedirect_withStatus() throws Exception {
		var res = new MockServletResponse();
		res.sendRedirect("http://example.com", 301, false);
		assertEquals(301, res.getStatus());
		assertEquals("http://example.com", res.getHeader("Location"));
	}

	@Test void a20_status_fluent() {
		var res = new MockServletResponse();
		var result = res.status(201);
		assertSame(res, result);
		assertEquals(201, res.getStatus());
	}

	@Test void a21_getOutputStream() throws Exception {
		var res = new MockServletResponse();
		assertNotNull(res.getOutputStream());
	}

	@Test void a22_getWriter() throws Exception {
		var res = new MockServletResponse();
		assertNotNull(res.getWriter());
	}

	@Test void a23_updateContentTypeHeader_nullCharset() throws Exception {
		// Test when charset is null: setCharacterEncoding(null) then setContentType triggers updateContentTypeHeader
		// with charset=null which skips the body
		var res = new MockServletResponse();
		res.setCharacterEncoding((String)null);  // charset becomes null
		res.setContentType("text/plain");  // contentType is non-null, but charset is null → skip body
		// Content type should be set as-is without charset modification
		assertNotNull(res.getContentType());
	}
}
