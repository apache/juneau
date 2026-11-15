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

	@Test void c01_isLoopbackHost() {
		assertTrue(isLoopbackHost("localhost"));
		assertTrue(isLoopbackHost("LocalHost"));
		assertTrue(isLoopbackHost("127.0.0.1"));
		assertTrue(isLoopbackHost("::1"));
		assertTrue(isLoopbackHost("[::1]"));
		assertFalse(isLoopbackHost(null));
		assertFalse(isLoopbackHost(""));
		assertFalse(isLoopbackHost("127.0.0.2"));
		assertFalse(isLoopbackHost("127.000.000.001"));
		assertFalse(isLoopbackHost("example.com"));
	}

	@Test void d01_parseIpv4_valid() {
		assertArrayEquals(new byte[]{10, 0, 0, 1}, parseIpv4("10.0.0.1"));
		assertArrayEquals(new byte[]{0, 0, 0, 0}, parseIpv4("0.0.0.0"));
		assertArrayEquals(new byte[]{(byte)255, (byte)255, (byte)255, (byte)255}, parseIpv4("255.255.255.255"));
		assertArrayEquals(new byte[]{(byte)192, (byte)168, 1, 100}, parseIpv4("192.168.1.100"));
	}

	@Test void d02_parseIpv4_invalid() {
		assertNull(parseIpv4(null));
		assertNull(parseIpv4(""));
		assertNull(parseIpv4("1.2.3"));
		assertNull(parseIpv4("1.2.3.4.5"));
		assertNull(parseIpv4("1.2.3."));
		assertNull(parseIpv4(".1.2.3"));
		assertNull(parseIpv4("1..2.3"));
		assertNull(parseIpv4("256.0.0.1"));
		assertNull(parseIpv4("1.2.3.01"));
		assertNull(parseIpv4("1.2.3.00"));
		assertNull(parseIpv4("1.2.3.+4"));
		assertNull(parseIpv4("1.2.3.-4"));
		assertNull(parseIpv4("1.2.3.1000"));
		assertNull(parseIpv4("1.2.3.a"));
		assertNull(parseIpv4(" 1.2.3.4"));
		assertNull(parseIpv4("0x1.2.3.4"));
		assertNull(parseIpv4("1.2.3.\u0664"));
	}
}
