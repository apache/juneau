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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

/**
 * Tests for the redirect-target validation in {@link RestResponse}, exercised via the package-private
 * {@link RestResponse#isSafeRedirectUri(String,String...)} helper.
 *
 * @since 10.0.0
 */
class RestResponse_Test {

	@Test void a01_relativePath_isSafe() {
		assertTrue(RestResponse.isSafeRedirectUri("/foo/bar"));
		assertTrue(RestResponse.isSafeRedirectUri("foo/bar"));
	}

	@Test void a02_nullOrEmpty_isSafe() {
		assertTrue(RestResponse.isSafeRedirectUri(null));
		assertTrue(RestResponse.isSafeRedirectUri(""));
	}

	@Test void a03_absoluteUri_allowedHost_isSafe() {
		assertTrue(RestResponse.isSafeRedirectUri("https://example.com/foo", "example.com"));
	}

	@Test void a04_absoluteUri_disallowedHost_isUnsafe() {
		assertFalse(RestResponse.isSafeRedirectUri("https://evil.example/foo", "example.com"));
	}

	@Test void a05_absoluteUri_noAllowedHosts_isUnsafe() {
		assertFalse(RestResponse.isSafeRedirectUri("https://example.com/foo"));
	}

	@Test void a06_protocolRelativeUri_disallowedHost_isUnsafe() {
		assertFalse(RestResponse.isSafeRedirectUri("//evil.example/foo", "example.com"));
	}

	@Test void a07_protocolRelativeUri_allowedHost_isSafe() {
		assertTrue(RestResponse.isSafeRedirectUri("//example.com/foo", "example.com"));
	}

	@Test void a08_allowedHostMatch_isCaseInsensitive() {
		assertTrue(RestResponse.isSafeRedirectUri("https://Example.COM/foo", "example.com"));
	}

	@Test void a09_malformedUri_isUnsafe() {
		assertFalse(RestResponse.isSafeRedirectUri("https://exa mple.com/foo", "example.com"));
	}
}
