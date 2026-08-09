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

import org.junit.jupiter.api.*;

class RedirectSecurity_Test {

	@Test void a01_sameOrigin_exactMatch_forwards() {
		var from = URI.create("https://example.com/api");
		var to = URI.create("https://example.com/other");
		assertTrue(RedirectSecurity.sameOrigin(from, to));
		assertFalse(RedirectSecurity.shouldStripCredentials(from, to));
		assertEquals(RedirectSecurity.CredentialDecision.FORWARD, RedirectSecurity.decide(from, to));
	}

	@Test void a02_sameOrigin_explicitDefaultPort_matchesImplicitPort() {
		var from = URI.create("https://example.com/api");
		var to = URI.create("https://example.com:443/other");
		assertTrue(RedirectSecurity.sameOrigin(from, to));
		assertFalse(RedirectSecurity.shouldStripCredentials(from, to));
	}

	@Test void b01_crossHost_strips() {
		var from = URI.create("https://alpha.example.com/api");
		var to = URI.create("https://beta.example.com/other");
		assertFalse(RedirectSecurity.sameOrigin(from, to));
		assertTrue(RedirectSecurity.shouldStripCredentials(from, to));
		assertEquals(RedirectSecurity.CredentialDecision.STRIP, RedirectSecurity.decide(from, to));
	}

	@Test void b02_crossPort_strips() {
		var from = URI.create("https://example.com:8443/api");
		var to = URI.create("https://example.com:9443/api");
		assertFalse(RedirectSecurity.sameOrigin(from, to));
		assertTrue(RedirectSecurity.shouldStripCredentials(from, to));
	}

	@Test void c01_httpsToHttpDowngrade_strips() {
		var from = URI.create("https://example.com/api");
		var to = URI.create("http://example.com/api");
		assertTrue(RedirectSecurity.isDowngrade(from, to));
		assertTrue(RedirectSecurity.shouldStripCredentials(from, to));
		assertEquals(RedirectSecurity.CredentialDecision.STRIP, RedirectSecurity.decide(from, to));
	}

	@Test void c02_httpToHttpsUpgrade_isNotDowngrade_andForwardsWhenSameAuthority() {
		var from = URI.create("http://example.com/api");
		var to = URI.create("https://example.com/api");
		assertFalse(RedirectSecurity.isDowngrade(from, to));
		// Different scheme -> different origin, so credentials still strip on the upgrade hop itself,
		// but the downgrade-specific predicate correctly reports false.
		assertTrue(RedirectSecurity.shouldStripCredentials(from, to));
	}

	@Test void c03_httpToHttps_sameSchemeSameAuthority_forwards() {
		var from = URI.create("https://example.com/a");
		var to = URI.create("https://example.com/b");
		assertFalse(RedirectSecurity.isDowngrade(from, to));
		assertFalse(RedirectSecurity.shouldStripCredentials(from, to));
	}

	@Test void d01_nullArgs_throw() {
		var uri = URI.create("https://example.com/");
		assertThrows(IllegalArgumentException.class, () -> RedirectSecurity.sameOrigin(null, uri));
		assertThrows(IllegalArgumentException.class, () -> RedirectSecurity.sameOrigin(uri, null));
		assertThrows(IllegalArgumentException.class, () -> RedirectSecurity.shouldStripCredentials(null, uri));
	}

	@Test void d02_relativeUri_throws() {
		var absolute = URI.create("https://example.com/");
		var relative = URI.create("/relative/path");
		assertThrows(IllegalArgumentException.class, () -> RedirectSecurity.sameOrigin(absolute, relative));
		assertThrows(IllegalArgumentException.class, () -> RedirectSecurity.isDowngrade(relative, absolute));
	}

	@Test void e01_stripOnCrossOrigin_reusesRedactedHeadersDefault() {
		assertEquals(org.apache.juneau.http.RedactedHeaders.DEFAULT, RedirectSecurity.stripOnCrossOrigin());
	}

	@Test void e02_hostCaseInsensitive() {
		var from = URI.create("https://Example.COM/api");
		var to = URI.create("https://example.com/other");
		assertTrue(RedirectSecurity.sameOrigin(from, to));
	}
}
