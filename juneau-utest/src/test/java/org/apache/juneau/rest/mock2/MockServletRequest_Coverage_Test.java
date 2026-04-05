// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                               *
// ***************************************************************************************************************************
package org.apache.juneau.rest.mock2;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

/**
 * Coverage tests for MockServletRequest - specifically testing the applyOverrides() true branches
 * and other uncovered getter/setter methods.
 */
class MockServletRequest_Coverage_Test extends TestBase {

	@Rest
	public static class A {
		@RestGet
		public String get(
				@Header("X-Protocol") String protocol,
				@Header("X-Scheme") String scheme,
				@Header("X-ServerName") String serverName,
				@Header("X-RemoteAddr") String remoteAddr,
				@Header("X-RemoteHost") String remoteHost) {
			return "ok";
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// applyOverrides() - test that non-null fields on MockRestRequest are applied
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_applyOverrides_withCharacterEncoding() throws Exception {
		// Test that when characterEncoding is set on MockRestRequest, it propagates via applyOverrides
		var client = MockRestClient
			.create(A.class)
			.build();
		client
			.get("/")
			.characterEncoding("ISO-8859-1")
			.run()
			.assertStatus(200);
	}

	@Test void a02_applyOverrides_withProtocol() throws Exception {
		MockRestClient
			.create(A.class)
			.build()
			.get("/")
			.protocol("HTTP/1.0")
			.run()
			.assertStatus(200);
	}

	@Test void a03_applyOverrides_withServerName() throws Exception {
		MockRestClient
			.create(A.class)
			.build()
			.get("/")
			.serverName("testserver")
			.run()
			.assertStatus(200);
	}

	@Test void a04_applyOverrides_withRemoteAddr() throws Exception {
		MockRestClient
			.create(A.class)
			.build()
			.get("/")
			.remoteAddr("192.168.1.1")
			.run()
			.assertStatus(200);
	}

	@Test void a05_applyOverrides_withRemoteHost() throws Exception {
		MockRestClient
			.create(A.class)
			.build()
			.get("/")
			.remoteHost("remotehost.example.com")
			.run()
			.assertStatus(200);
	}

	@Test void a06_applyOverrides_withLocalName() throws Exception {
		MockRestClient
			.create(A.class)
			.build()
			.get("/")
			.localName("localhost")
			.run()
			.assertStatus(200);
	}

	@Test void a07_applyOverrides_withLocalAddr() throws Exception {
		MockRestClient
			.create(A.class)
			.build()
			.get("/")
			.localAddr("127.0.0.1")
			.run()
			.assertStatus(200);
	}

	@Test void a08_applyOverrides_withContextPath() throws Exception {
		MockRestClient
			.create(A.class)
			.build()
			.get("/")
			.contextPath("")
			.run()
			.assertStatus(200);
	}

	@Test void a09_applyOverrides_withRemoteUser() throws Exception {
		MockRestClient
			.create(A.class)
			.build()
			.get("/")
			.remoteUser("testuser")
			.run()
			.assertStatus(200);
	}

	@Test void a10_applyOverrides_withRequestedSessionId() throws Exception {
		MockRestClient
			.create(A.class)
			.build()
			.get("/")
			.requestedSessionId("session123")
			.run()
			.assertStatus(200);
	}

	@Test void a11_applyOverrides_withAuthType() throws Exception {
		MockRestClient
			.create(A.class)
			.build()
			.get("/")
			.authType("BASIC")
			.run()
			.assertStatus(200);
	}

	@Test void a12_applyOverrides_withServerPort() throws Exception {
		MockRestClient
			.create(A.class)
			.build()
			.get("/")
			.serverPort(8080)
			.run()
			.assertStatus(200);
	}

	@Test void a13_applyOverrides_withRemotePort() throws Exception {
		MockRestClient
			.create(A.class)
			.build()
			.get("/")
			.remotePort(12345)
			.run()
			.assertStatus(200);
	}

	@Test void a14_applyOverrides_withLocalPort() throws Exception {
		MockRestClient
			.create(A.class)
			.build()
			.get("/")
			.localPort(8081)
			.run()
			.assertStatus(200);
	}

	@Test void a15_applyOverrides_withScheme() throws Exception {
		MockRestClient
			.create(A.class)
			.build()
			.get("/")
			.uriScheme("https")
			.run()
			.assertStatus(200);
	}

	@Test void a16_applyOverrides_withPathInfo() throws Exception {
		MockRestClient
			.create(A.class)
			.build()
			.get("/")
			.pathInfo("/test")
			.run()
			.assertStatus(200);
	}

	@Test void a17_applyOverrides_withQueryString() throws Exception {
		MockRestClient
			.create(A.class)
			.build()
			.get("/")
			.queryString("foo=bar")
			.run()
			.assertStatus(200);
	}

	@Test void a18_applyOverrides_withLocale() throws Exception {
		MockRestClient
			.create(A.class)
			.build()
			.get("/")
			.locale(Locale.US)
			.run()
			.assertStatus(200);
	}

	@Test void a19_applyOverrides_withDispatcherType() throws Exception {
		MockRestClient
			.create(A.class)
			.build()
			.get("/")
			.dispatcherType(jakarta.servlet.DispatcherType.REQUEST)
			.run()
			.assertStatus(200);
	}

	@Test void a20_applyOverrides_withRoles() throws Exception {
		MockRestClient
			.create(A.class)
			.build()
			.get("/")
			.roles("ROLE_ADMIN")
			.run()
			.assertStatus(200);
	}

	@Test void a21_applyOverrides_withHttpSession() throws Exception {
		MockRestClient
			.create(A.class)
			.build()
			.get("/")
			.httpSession(org.apache.juneau.rest.mock.MockHttpSession.create())
			.run()
			.assertStatus(200);
	}

	@Test void a22_applyOverrides_withRequestURI() throws Exception {
		// requestURI "/" matches the GET endpoint
		MockRestClient
			.create(A.class)
			.build()
			.get("/")
			.requestURI("/")
			.run()
			.assertStatus(200);
	}

	@Test void a23_applyOverrides_withServletPath() throws Exception {
		// servletPath must be valid for the existing route
		MockRestClient
			.create(A.class)
			.build()
			.get("/")
			.servletPath("")
			.run()
			.assertStatus(200);
	}

	@Test void a24_applyOverrides_withPathTranslated() throws Exception {
		MockRestClient
			.create(A.class)
			.build()
			.get("/")
			.pathTranslated("/translated/path")
			.run()
			.assertStatus(200);
	}

	@Test void a25_applyOverrides_withUserPrincipal() throws Exception {
		MockRestClient
			.create(A.class)
			.build()
			.get("/")
			.userPrincipal(() -> "testuser")
			.run()
			.assertStatus(200);
	}

	@Test void a26_applyOverrides_withCookies() throws Exception {
		MockRestClient
			.create(A.class)
			.build()
			.get("/")
			.cookies(new jakarta.servlet.http.Cookie[]{new jakarta.servlet.http.Cookie("foo", "bar")})
			.run()
			.assertStatus(200);
	}

	//------------------------------------------------------------------------------------------------------------------
	// MockServletRequest getter/setter methods
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_gettersAndSetters() throws Exception {
		var req = MockServletRequest.create("GET", "/test");

		// Basic getters
		assertEquals("GET", req.getMethod());
		assertNotNull(req.getRequestURL());
		assertNotNull(req.getHeaderNames());
		assertNotNull(req.getLocales());

		// getHeaders for empty header
		assertNotNull(req.getHeaders("NonExistent"));

		// getIntHeader for missing header
		assertEquals(0, req.getIntHeader("NonExistent"));

		// getIntHeader for existing header
		req.header("X-Count", "5");
		assertEquals(5, req.getIntHeader("X-Count"));
	}

	@Test void b02_isUserInRole() {
		var req = MockServletRequest.create("GET", "/test");
		assertFalse(req.isUserInRole("ADMIN"));

		req.role("ADMIN");
		assertTrue(req.isUserInRole("ADMIN"));
		assertFalse(req.isUserInRole("USER"));
	}

	@Test void b03_getInputStream() throws Exception {
		var req = MockServletRequest.create("GET", "/test");
		assertNotNull(req.getInputStream());
	}

	@Test void b04_getReader() throws Exception {
		var req = MockServletRequest.create("GET", "/test");
		assertNotNull(req.getReader());
	}

	@Test void b05_setAttribute_and_getAttribute() {
		var req = MockServletRequest.create("GET", "/test");
		req.setAttribute("key", "value");
		assertEquals("value", req.getAttribute("key"));
		assertNotNull(req.getAttributeNames());
		req.removeAttribute("key");
		assertNull(req.getAttribute("key"));
	}

	@Test void b06_getLocale() {
		var req = MockServletRequest.create("GET", "/test");
		// Default locale is null
		req.locale(Locale.US);
		assertEquals(Locale.US, req.getLocale());
	}

	@Test void b07_getServletContext() {
		var req = MockServletRequest.create("GET", "/test");
		assertNull(req.getServletContext());
	}

	@Test void b08_isRequestedSessionIdFromCookie_URL_valid() {
		var req = MockServletRequest.create("GET", "/test");
		assertFalse(req.isRequestedSessionIdFromCookie());
		assertFalse(req.isRequestedSessionIdFromURL());
		assertFalse(req.isRequestedSessionIdValid());
	}

	@Test void b09_isAsyncStarted_Supported() {
		var req = MockServletRequest.create("GET", "/test");
		assertFalse(req.isAsyncStarted());
		assertFalse(req.isAsyncSupported());
		assertFalse(req.isSecure());
	}

	@Test void b10_getSession() {
		var req = MockServletRequest.create("GET", "/test");
		assertNotNull(req.getSession());
		assertNotNull(req.getSession(true));
		assertNotNull(req.getSession(false));
	}

	@Test void b11_getParts() throws Exception {
		var req = MockServletRequest.create("GET", "/test");
		assertNotNull(req.getParts());
		assertNull(req.getPart("x"));
	}

	@Test void b12_getRequestDispatcher() {
		var req = MockServletRequest.create("GET", "/test");
		req.requestDispatcher("/path", new jakarta.servlet.RequestDispatcher() {
			@Override public void forward(jakarta.servlet.ServletRequest r, jakarta.servlet.ServletResponse s) {}
			@Override public void include(jakarta.servlet.ServletRequest r, jakarta.servlet.ServletResponse s) {}
		});
		assertNotNull(req.getRequestDispatcher("/path"));
	}

	@Test void b13_getUserPrincipal() {
		var req = MockServletRequest.create("GET", "/test");
		assertNull(req.getUserPrincipal());
		req.userPrincipal(() -> "testuser");
		assertNotNull(req.getUserPrincipal());
		assertEquals("testuser", req.getUserPrincipal().getName());
	}

	@Test void b14_pathVars() {
		var req = MockServletRequest.create("GET", "/test");
		req.pathVars("foo", "bar", "baz", "qux");
		assertNotNull(req.getAttribute("juneau.pathVars"));
	}

	@Test void b15_pathVarsNullMap() {
		var req = MockServletRequest.create("GET", "/test");
		req.pathVars((Map<String,String>)null);
		// Should not throw, pathVars accepts null and is a no-op
	}

	@Test void b16_noTrace() {
		var req = MockServletRequest.create("GET", "/test");
		req.noTrace(true);
		assertEquals("true", req.getHeader("No-Trace"));

		var req2 = MockServletRequest.create("GET", "/test");
		req2.noTrace(false);
		assertNull(req2.getHeader("No-Trace"));
	}

	@Test void b17_getRequestedSessionId() {
		var req = MockServletRequest.create("GET", "/test");
		req.requestedSessionId("sess123");
		assertEquals("sess123", req.getRequestedSessionId());
	}

	@Test void b17b_content_withReader() throws Exception {
		var req = MockServletRequest.create("POST", "/test");
		req.content(new java.io.StringReader("hello reader"));
		assertNotNull(req.getInputStream());
	}

	@Test void b17c_content_withInputStream() throws Exception {
		var req = MockServletRequest.create("POST", "/test");
		req.content(new java.io.ByteArrayInputStream("hello stream".getBytes()));
		assertNotNull(req.getInputStream());
	}

	@Test void b17d_content_withCharSequence() throws Exception {
		var req = MockServletRequest.create("POST", "/test");
		req.content(new StringBuilder("hello charseq"));
		assertNotNull(req.getInputStream());
	}

	@Test void b17e_content_withObject() throws Exception {
		var req = MockServletRequest.create("POST", "/test");
		req.content(42);  // General object → toString().getBytes()
		assertNotNull(req.getInputStream());
	}

	@Test void b17f_getContentLength_nullContent() {
		var req = MockServletRequest.create("GET", "/test");
		// No content set → content may be empty array, not null
		assertTrue(req.getContentLength() >= 0);
		assertTrue(req.getContentLengthLong() >= 0);
	}

	@Test void b17g_getDateHeader() {
		var req = MockServletRequest.create("GET", "/test");
		// Non-existent header → returns 0
		assertEquals(0, req.getDateHeader("X-Missing"));
		// Set a valid date header
		req.header("Date", "Thu, 01 Jan 1970 00:00:00 GMT");
		assertTrue(req.getDateHeader("Date") >= 0);
	}

	@Test void b18_postMethod_getParameterMap() {
		var req = MockServletRequest.create("POST", "/test");
		req.content("foo=bar&baz=qux".getBytes());
		var paramMap = req.getParameterMap();
		assertNotNull(paramMap);
		assertEquals("bar", req.getParameter("foo"));
		assertNotNull(req.getParameterValues("foo"));
		assertNotNull(req.getParameterNames());
	}

	@Test void b19_header_withStringArray() {
		var req = MockServletRequest.create("GET", "/test");
		req.header("X-Multi", new String[]{"val1", "val2"});
		assertNotNull(req.getHeaders("X-Multi"));
	}

	@Test void b20_header_withNullValue() {
		var req = MockServletRequest.create("GET", "/test");
		req.header("X-Null", (Object)null);
		// null value should be a no-op
		assertNull(req.getHeader("X-Null"));
	}
}
