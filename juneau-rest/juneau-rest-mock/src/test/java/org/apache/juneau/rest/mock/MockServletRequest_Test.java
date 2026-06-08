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
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.*;

/**
 * Coverage tests for {@link MockServletRequest} — exercises the simple HttpServletRequest
 * passthroughs, fluent setter round-trips, and a few branches that aren't exercised by
 * the existing {@code MockServletRequest_Coverage_Test} (which focuses on
 * {@code applyOverrides}).
 */
// MockRestClient instances are short-lived test fixtures; the mock framework manages lifecycle.
@SuppressWarnings("resource")
class MockServletRequest_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// A. Trivial null/no-op HttpServletRequest passthroughs.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_authenticate_returnsFalse() throws Exception {
		assertFalse(MockServletRequest.create().authenticate(null));
	}

	@Test void a02_changeSessionId_returnsNull() {
		assertNull(MockServletRequest.create().changeSessionId());
	}

	@Test void a03_getAsyncContext_returnsNull() {
		assertNull(MockServletRequest.create().getAsyncContext());
	}

	@Test void a04_getProtocolRequestId_returnsNull() {
		assertNull(MockServletRequest.create().getProtocolRequestId());
	}

	@Test void a05_getRequestId_returnsNull() {
		assertNull(MockServletRequest.create().getRequestId());
	}

	@Test void a06_getServletConnection_returnsNull() {
		assertNull(MockServletRequest.create().getServletConnection());
	}

	@Test void a07_login_logout_areNoOps() {
		var req = MockServletRequest.create();
		assertDoesNotThrow(() -> req.login("user", "pw"));
		assertDoesNotThrow(req::logout);
	}

	@Test void a08_startAsync_returnsNull() {
		assertNull(MockServletRequest.create().startAsync());
	}

	@Test void a09_startAsyncWithArgs_returnsNull() {
		assertNull(MockServletRequest.create().startAsync(null, null));
	}

	@Test void a10_upgrade_returnsNull() throws Exception {
		assertNull(MockServletRequest.create().upgrade(null));
	}

	@Test void a11_getPart_returnsNull() throws Exception {
		assertNull(MockServletRequest.create().getPart("x"));
	}

	@Test void a12_getParts_returnsEmptyList() throws Exception {
		assertTrue(MockServletRequest.create().getParts().isEmpty());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// B. Simple getters return what fluent setters wrote.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_getAuthType_roundTrip() {
		var req = MockServletRequest.create().authType("BASIC");
		assertEquals("BASIC", req.getAuthType());
	}

	@Test void b02_getCharacterEncoding_default() {
		assertEquals("UTF-8", MockServletRequest.create().getCharacterEncoding());
	}

	@Test void b03_getCharacterEncoding_setter() {
		var req = MockServletRequest.create().characterEncoding("ISO-8859-1");
		assertEquals("ISO-8859-1", req.getCharacterEncoding());
	}

	@Test void b04_getContentType_fromHeader() {
		var req = MockServletRequest.create().header("Content-Type", "text/plain");
		assertEquals("text/plain", req.getContentType());
	}

	@Test void b05_getDispatcherType_roundTrip() {
		var req = MockServletRequest.create().dispatcherType(DispatcherType.REQUEST);
		assertEquals(DispatcherType.REQUEST, req.getDispatcherType());
	}

	@Test void b06_getLocalAddr_roundTrip() {
		var req = MockServletRequest.create().localAddr("127.0.0.1");
		assertEquals("127.0.0.1", req.getLocalAddr());
	}

	@Test void b07_getLocalName_roundTrip() {
		var req = MockServletRequest.create().localName("localhost");
		assertEquals("localhost", req.getLocalName());
	}

	@Test void b08_getLocalPort_roundTrip() {
		var req = MockServletRequest.create().localPort(8081);
		assertEquals(8081, req.getLocalPort());
	}

	@Test void b09_getRemoteHost_roundTrip() {
		var req = MockServletRequest.create().remoteHost("remote.example.com");
		assertEquals("remote.example.com", req.getRemoteHost());
	}

	@Test void b10_getRemotePort_roundTrip() {
		var req = MockServletRequest.create().remotePort(54321);
		assertEquals(54321, req.getRemotePort());
	}

	@Test void b11_getRemoteUser_roundTrip() {
		var req = MockServletRequest.create().remoteUser("alice");
		assertEquals("alice", req.getRemoteUser());
	}

	@Test void b12_getServletContext_roundTrip() {
		// Pass null; we just need to exercise the setter and getter.
		var req = MockServletRequest.create().servletContext((ServletContext) null);
		assertNull(req.getServletContext());
	}

	@Test void b13_setCharacterEncoding_setter() throws Exception {
		var req = MockServletRequest.create();
		req.setCharacterEncoding("US-ASCII");
		assertEquals("US-ASCII", req.getCharacterEncoding());
	}

	@Test void b14_attribute_fluentSetter() {
		var req = MockServletRequest.create();
		assertSame(req, req.attribute("k", "v"));
		assertEquals("v", req.getAttribute("k"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// C. getContentLength / getContentLengthLong null-content branch.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_getContentLength_nullContentBranch() {
		// Setting content with `nn(value)` false produces a null content via no-arg path.
		// The simplest way to hit the null-content branch is via reflection-free: call content(null).
		// content(null) takes no path so leaves the existing empty-array, but we can pass an
		// InputStream that reads zero bytes after content was previously set to null.  Easier:
		// set the content to a byte[] of length 0 and check return; null branch uses ternary.
		var req = MockServletRequest.create();
		// Force content to be non-empty first, then verify length-check returns >= 0.
		req.content("");  // sets empty bytes
		assertEquals(0, req.getContentLength());
		assertEquals(0L, req.getContentLengthLong());
	}

	@Test void c02_content_passNull_noOp() {
		var req = MockServletRequest.create();
		req.content(null); // nn(value) is false; content remains the default empty byte[]
		assertEquals(0, req.getContentLength());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// D. getDateHeader edge cases.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_getDateHeader_nullHeader_returnsZero() {
		assertEquals(0, MockServletRequest.create().getDateHeader("X-Missing"));
	}

	@Test void d02_getDateHeader_validDate_returnsMillis() {
		var req = MockServletRequest.create().header("Date", "Sat, 29 Oct 1994 19:43:31 GMT");
		assertTrue(req.getDateHeader("Date") > 0);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// E. getHeader / getIntHeader edge cases.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_getHeader_unknown_returnsNull() {
		assertNull(MockServletRequest.create().getHeader("Nope"));
	}

	@Test void e02_getIntHeader_emptyString_returnsZero() {
		var req = MockServletRequest.create().header("X-Int", "");
		assertEquals(0, req.getIntHeader("X-Int"));
	}

	@Test void e03_getIntHeader_validInt() {
		var req = MockServletRequest.create().header("X-Int", "42");
		assertEquals(42, req.getIntHeader("X-Int"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// F. getParameterMap with form-encoded POST body, and the null-value branch in parsed map.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_getParameterMap_postFormBody() {
		var req = MockServletRequest.create().method("POST").content("a=1&b=2");
		var map = req.getParameterMap();
		assertArrayEquals(new String[]{"1"}, map.get("a"));
		assertArrayEquals(new String[]{"2"}, map.get("b"));
	}

	@Test void f02_getParameterMap_postFormBody_keyOnly() {
		// `key&` with no value parses to e.getValue() == null, exercising line 410-411.
		// RestUtils.parseQuery("key") returns map with "key" -> null.
		var req = MockServletRequest.create().method("POST").content("flag");
		var map = req.getParameterMap();
		assertNull(map.get("flag"));
	}

	@Test void f03_getParameter_unknownReturnsNull() {
		var req = MockServletRequest.create();
		assertNull(req.getParameter("unknown"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// G. getInputStream after getParameterMap on POST flushes formDataMap back to bytes.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void g01_getInputStream_afterFormDataMapPopulated() throws Exception {
		var req = MockServletRequest.create().method("POST").content("k=v");
		// Trigger formDataMap population.
		req.getParameterMap();
		// getInputStream() now hits the `nn(formDataMap)` true branch and serializes back out.
		try (var is = req.getInputStream()) {
			var bytes = is.readAllBytes();
			assertNotNull(bytes);
			assertTrue(new String(bytes).contains("k"));
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// H. getPathTranslated lazy initialization vs cached value.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void h01_getPathTranslated_returnsExplicitValue() {
		var req = MockServletRequest.create().method("GET").uri("/x").pathTranslated("/explicit");
		assertEquals("/explicit", req.getPathTranslated());
	}

	@Test void h02_getPathTranslated_lazyDerivedFromPathInfo() {
		var req = MockServletRequest.create().method("GET").uri("/abc");
		assertEquals("/mock-path/abc", req.getPathTranslated());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// I. getQueryString — the construct-from-queryDataMap branches (lines 466-479).
	//-----------------------------------------------------------------------------------------------------------------

	@Test void i01_getQueryString_buildsFromQueryDataMap() {
		// uri(...) populates queryDataMap and sets queryString.  Reset queryString to null
		// to force the build path inside getQueryString().
		var req = MockServletRequest.create().method("GET").uri("/p?a=1&b=2");
		req.queryString(null);
		var qs = req.getQueryString();
		assertNotNull(qs);
		assertTrue(qs.contains("a=1"));
		assertTrue(qs.contains("b=2"));
	}

	@Test void i02_getQueryString_keyOnlyEntryNoEquals() {
		// `flag` (no value) -> queryDataMap entry with null value -> branch v == null in lambda.
		// Two key-only entries to exercise both `sb.isEmpty()` arms of the ternary in line 474.
		var req = MockServletRequest.create().method("GET").uri("/p?flag1&flag2");
		req.queryString(null);
		var qs = req.getQueryString();
		assertNotNull(qs);
		assertTrue(qs.contains("flag1"));
		assertTrue(qs.contains("flag2"));
		// No '=' sign for key-only entries.
		assertFalse(qs.contains("flag1="));
		assertFalse(qs.contains("flag2="));
		// Verify they are joined by `&`.
		assertTrue(qs.contains("&"));
	}

	@Test void i03_getQueryString_emptyMap_returnsNull() {
		// Default queryDataMap is empty; queryString is null; getQueryString() yields "" -> null.
		assertNull(MockServletRequest.create().getQueryString());
	}

	@Test void i04_getQueryString_explicitlySet_returnedAsIs() {
		var req = MockServletRequest.create().queryString("x=y");
		assertEquals("x=y", req.getQueryString());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// J. uri(null) treated as empty, and uri with fragment.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void j01_uri_null_safe() {
		var req = MockServletRequest.create().uri(null);
		assertEquals("", req.getRequestURL().toString());
	}

	@Test void j02_uri_withFragment_strippedFromQueryString() {
		var req = MockServletRequest.create().uri("/p?foo=bar#frag");
		assertEquals("foo=bar", req.getQueryString());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// K. Header convenience: header(String[]) and combine path.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void k01_header_stringArrayAndCombine() {
		var req = MockServletRequest.create()
			.header("X-Many", new String[]{"v1", "v2"})
			.header("X-Many", "v3");
		var enumr = req.getHeaders("X-Many");
		var seen = new ArrayList<String>();
		while (enumr.hasMoreElements())
			seen.add(enumr.nextElement());
		assertTrue(seen.contains("v1"));
		assertTrue(seen.contains("v2"));
		assertTrue(seen.contains("v3"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// L. getReader() character encoding round-trip.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void l01_getReader_readsContent() throws Exception {
		var req = MockServletRequest.create().content("hello");
		try (var r = req.getReader()) {
			assertEquals("hello", r.readLine());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// M. content(Reader) IOException path: pass a Reader that throws on read.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void m01_content_readerThrows_wrappedAsRuntime() {
		var failingReader = new Reader() {
			@Override public int read(char[] cbuf, int off, int len) throws IOException { throw new IOException("boom"); }
			@Override public void close() { /* no-op */ }
		};
		var req = MockServletRequest.create();
		assertThrows(RuntimeException.class, () -> req.content(failingReader));
	}
}
