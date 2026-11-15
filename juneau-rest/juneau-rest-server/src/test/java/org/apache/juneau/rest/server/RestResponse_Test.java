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
package org.apache.juneau.rest.server;

import static org.apache.juneau.commons.http.SafePathTemplate.isSafeRedirect;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.http.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the redirect-target rule behind {@link RestResponse#sendSafeRedirect(String,String...)}, which delegates to
 * {@link SafePathTemplate#isSafeRedirect(String,String...)}.
 *
 * @since 10.0.0
 */
class RestResponse_Test {

	@Test void a01_relativePath_isSafe() {
		assertTrue(isSafeRedirect("/foo/bar"));
		assertTrue(isSafeRedirect("foo/bar"));
	}

	@Test void a02_nullOrEmpty_isSafe() {
		assertTrue(isSafeRedirect(null));
		assertTrue(isSafeRedirect(""));
	}

	@Test void a03_absoluteUri_allowedHost_isSafe() {
		assertTrue(isSafeRedirect("https://example.com/foo", "example.com"));
	}

	@Test void a04_absoluteUri_otherHost_isUnsafe() {
		assertFalse(isSafeRedirect("https://other.example/foo", "example.com"));
	}

	@Test void a05_absoluteUri_noAllowedHosts_isUnsafe() {
		assertFalse(isSafeRedirect("https://example.com/foo"));
	}

	@Test void a06_protocolRelativeUri_otherHost_isUnsafe() {
		assertFalse(isSafeRedirect("//other.example/foo", "example.com"));
	}

	@Test void a07_protocolRelativeUri_allowedHost_isSafe() {
		assertTrue(isSafeRedirect("//example.com/foo", "example.com"));
	}

	@Test void a08_allowedHostMatch_isCaseInsensitive() {
		assertTrue(isSafeRedirect("https://Example.COM/foo", "example.com"));
	}

	@Test void a09_malformedUri_isUnsafe() {
		assertFalse(isSafeRedirect("https://exa mple.com/foo", "example.com"));
	}

	@Test void a10_backslashAndControlChars_areUnsafe() {
		assertFalse(isSafeRedirect("/\\example.com/foo", "example.com"));
		assertFalse(isSafeRedirect("/foo\\bar"));
		assertFalse(isSafeRedirect("/foo\tbar"));
		assertFalse(isSafeRedirect("/foo\nbar"));
		assertFalse(isSafeRedirect("/foo bar"));
		assertFalse(isSafeRedirect("/foo\u007fbar"));
	}

	@Test void a99_schemeWithoutAuthority_isUnsafe() {
		assertFalse(isSafeRedirect("javascript:alert(1)", "example.com"));
		assertFalse(isSafeRedirect("data:text/html,x"));
		assertFalse(isSafeRedirect("http:other.example"));
		assertTrue(isSafeRedirect("foo/bar:baz"));
		assertTrue(isSafeRedirect("?a=b:c"));
	}
}
