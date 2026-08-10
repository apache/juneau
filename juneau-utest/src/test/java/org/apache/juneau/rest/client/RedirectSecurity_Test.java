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
package org.apache.juneau.rest.client;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Verifies the credential-forwarding decision used when a client transparently follows a redirect: credentials
 * are forwarded only to the exact same origin, and stripped on any origin change or scheme downgrade.
 */
class RedirectSecurity_Test extends TestBase {

	private static URI u(String s) {
		return URI.create(s);
	}

	@Test void a01_sameOriginForwards() {
		assertTrue(RedirectSecurity.sameOrigin(u("https://good.example/a"), u("https://good.example/b")));
		assertFalse(RedirectSecurity.shouldStripCredentials(u("https://good.example/a"), u("https://good.example/b")));
	}

	@Test void a02_sameOriginDefaultPortNormalized() {
		assertTrue(RedirectSecurity.sameOrigin(u("https://good.example/a"), u("https://good.example:443/b")));
	}

	@Test void a03_differentHostStrips() {
		assertTrue(RedirectSecurity.shouldStripCredentials(u("https://good.example/a"), u("https://evil.example/b")));
	}

	@Test void a04_differentPortStrips() {
		assertTrue(RedirectSecurity.shouldStripCredentials(u("https://good.example/a"), u("https://good.example:8443/b")));
	}

	@Test void a05_schemeDowngradeStrips() {
		assertTrue(RedirectSecurity.isDowngrade(u("https://good.example/a"), u("http://good.example/b")));
		assertTrue(RedirectSecurity.shouldStripCredentials(u("https://good.example/a"), u("http://good.example/b")));
	}

	@Test void a06_stripSetIncludesCommonCredentialHeaders() {
		var s = RedirectSecurity.stripOnCrossOrigin();
		assertTrue(s.stream().anyMatch("Authorization"::equalsIgnoreCase));
		assertTrue(s.stream().anyMatch("Cookie"::equalsIgnoreCase));
	}

	@Test void a07_nonAbsoluteRejected() {
		assertThrows(IllegalArgumentException.class, () -> RedirectSecurity.sameOrigin(u("/relative"), u("https://good.example/b")));
		assertThrows(IllegalArgumentException.class, () -> RedirectSecurity.sameOrigin(null, u("https://good.example/b")));
	}
}
