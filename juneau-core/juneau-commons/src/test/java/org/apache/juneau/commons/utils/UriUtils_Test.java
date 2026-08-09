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

package org.apache.juneau.commons.utils;

import static org.apache.juneau.commons.utils.UriUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Tests {@link UriUtils}.
 */
class UriUtils_Test extends TestBase {

	@Test void a00_constructor() {
		assertNotNull(new UriUtils());
	}

	@Test void a01_httpsAccepted() {
		assertTrue(isSecureOrLoopback(URI.create("https://example.com/token")));
	}

	@Test void a02_httpsAcceptedCaseInsensitiveScheme() {
		assertTrue(isSecureOrLoopback(URI.create("HTTPS://example.com/token")));
	}

	@Test void a03_loopbackHttpAccepted() {
		assertTrue(isSecureOrLoopback(URI.create("http://localhost:8080/token")));
		assertTrue(isSecureOrLoopback(URI.create("http://127.0.0.1:8080/token")));
		assertTrue(isSecureOrLoopback(URI.create("http://[::1]:8080/token")));
	}

	@Test void a04_loopbackHttpAcceptedCaseInsensitiveHost() {
		assertTrue(isSecureOrLoopback(URI.create("http://LOCALHOST/token")));
	}

	@Test void a05_plaintextHttpRejected() {
		assertFalse(isSecureOrLoopback(URI.create("http://example.com/token")));
	}

	@Test void a06_nullRejected() {
		assertFalse(isSecureOrLoopback(null));
	}

	@Test void a07_schemelessRejected() {
		assertFalse(isSecureOrLoopback(URI.create("//example.com/token")));
		assertFalse(isSecureOrLoopback(URI.create("/relative/path")));
	}

	@Test void a08_otherSchemeNonLoopbackRejected() {
		assertFalse(isSecureOrLoopback(URI.create("ftp://example.com/file")));
	}

	@Test void b01_assertHttpsReturnsUri() {
		var uri = URI.create("https://example.com/token");
		assertSame(uri, assertSecureOrLoopback(uri));
	}

	@Test void b02_assertLoopbackHttpReturnsUri() {
		var uri = URI.create("http://localhost/token");
		assertSame(uri, assertSecureOrLoopback(uri));
	}

	@Test void b03_assertPlaintextHttpThrows() {
		var uri = URI.create("http://example.com/token");
		var e = assertThrows(IllegalArgumentException.class, () -> assertSecureOrLoopback(uri));
		assertTrue(e.getMessage().contains("https"));
	}

	@Test void b04_assertNullThrows() {
		assertThrows(IllegalArgumentException.class, () -> assertSecureOrLoopback(null));
	}
}
