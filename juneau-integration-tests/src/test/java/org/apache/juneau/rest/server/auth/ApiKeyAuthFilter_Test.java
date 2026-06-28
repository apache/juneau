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
package org.apache.juneau.rest.server.auth;

import static org.apache.juneau.commons.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.security.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * Tests for {@link ApiKeyAuthFilter} — header, query, cookie sources; unknown key; and
 * {@link ClaimsPrincipal} role extraction.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S5778"  // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice
})
class ApiKeyAuthFilter_Test extends TestBase {

	private static final Principal ALICE = () -> "alice";
	private static final ApiKeyStore STORE = key -> "good-key".equals(key) ? opt(ALICE) : opte();

	private static HttpServletRequest reqWithHeader(String headerName, String value) {
		var r = mock(HttpServletRequest.class);
		when(r.getHeader(headerName)).thenReturn(value);
		when(r.getCookies()).thenReturn(null);
		return r;
	}

	private static HttpServletRequest reqWithQuery(String param, String value) {
		var r = mock(HttpServletRequest.class);
		when(r.getParameter(param)).thenReturn(value);
		when(r.getCookies()).thenReturn(null);
		return r;
	}

	private static HttpServletRequest reqWithCookie(String cookieName, String value) {
		var r = mock(HttpServletRequest.class);
		var cookie = mock(Cookie.class);
		when(cookie.getName()).thenReturn(cookieName);
		when(cookie.getValue()).thenReturn(value);
		when(r.getCookies()).thenReturn(new Cookie[]{cookie});
		return r;
	}

	@Test void a01_header_goodKey_returnsAuthResult() throws Exception {
		var f = ApiKeyAuthFilter.create().store(STORE).build();
		var result = f.authenticate(reqWithHeader("X-API-Key", "good-key"));
		assertTrue(result.isPresent());
		assertSame(ALICE, result.get().getPrincipal());
	}

	@Test void a02_header_missingKey_returnsEmpty() throws Exception {
		var f = ApiKeyAuthFilter.create().store(STORE).build();
		var result = f.authenticate(reqWithHeader("X-API-Key", null));
		assertTrue(result.isEmpty());
	}

	@Test void a03_header_blankKey_returnsEmpty() throws Exception {
		var f = ApiKeyAuthFilter.create().store(STORE).build();
		var result = f.authenticate(reqWithHeader("X-API-Key", "  "));
		assertTrue(result.isEmpty());
	}

	@Test void a04_header_unknownKey_throws() {
		var f = ApiKeyAuthFilter.create().store(STORE).build();
		assertThrows(AuthenticationException.class,
			() -> f.authenticate(reqWithHeader("X-API-Key", "bad-key")));
	}

	@Test void a05_customHeader_goodKey() throws Exception {
		var f = ApiKeyAuthFilter.create().store(STORE).fromHeader("X-Custom-Key").build();
		var result = f.authenticate(reqWithHeader("X-Custom-Key", "good-key"));
		assertTrue(result.isPresent());
	}

	@Test void a06_querySource_goodKey() throws Exception {
		var f = ApiKeyAuthFilter.create().store(STORE).fromQuery("apiKey").build();
		var result = f.authenticate(reqWithQuery("apiKey", "good-key"));
		assertTrue(result.isPresent());
		assertSame(ALICE, result.get().getPrincipal());
	}

	@Test void a07_querySource_missingParam_returnsEmpty() throws Exception {
		var f = ApiKeyAuthFilter.create().store(STORE).fromQuery("apiKey").build();
		var result = f.authenticate(reqWithQuery("apiKey", null));
		assertTrue(result.isEmpty());
	}

	@Test void a08_cookieSource_goodKey() throws Exception {
		var f = ApiKeyAuthFilter.create().store(STORE).fromCookie("api_key").build();
		var result = f.authenticate(reqWithCookie("api_key", "good-key"));
		assertTrue(result.isPresent());
		assertSame(ALICE, result.get().getPrincipal());
	}

	@Test void a09_cookieSource_fallbackToHeader() throws Exception {
		// Some mock containers don't populate getCookies(); fallback reads the raw Cookie header.
		var r = mock(HttpServletRequest.class);
		when(r.getCookies()).thenReturn(null);
		when(r.getHeader("Cookie")).thenReturn("api_key=good-key; other=val");
		var f = ApiKeyAuthFilter.create().store(STORE).fromCookie("api_key").build();
		var result = f.authenticate(r);
		assertTrue(result.isPresent());
		assertSame(ALICE, result.get().getPrincipal());
	}

	@Test void a10_storeThrowsRuntime_wrappedAsAuthException() {
		ApiKeyStore boom = key -> { throw new RuntimeException("store down"); };
		var f = ApiKeyAuthFilter.create().store(boom).build();
		var r = reqWithHeader("X-API-Key", "any-key");
		var ex = assertThrows(AuthenticationException.class, () -> f.authenticate(r));
		assertNotNull(ex.getCause());
	}

	@Test void a11_claimsPrincipal_rolesFlowToAuthResult() throws Exception {
		var claims = Map.<String, Object>of("roles", List.of("user"), "sub", "alice");
		var cp = new ClaimsPrincipal("alice", claims);
		ApiKeyStore store = key -> "good-key".equals(key) ? opt(cp) : opte();
		var f = ApiKeyAuthFilter.create().store(store).build();
		var result = f.authenticate(reqWithHeader("X-API-Key", "good-key"));
		assertTrue(result.isPresent());
		assertEquals(Set.of("user"), result.get().getRoles());
	}

	@Test void a12_buildWithoutStore_throws() {
		assertThrows(IllegalStateException.class, () -> ApiKeyAuthFilter.create().build());
	}
}
