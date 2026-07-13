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
package org.apache.juneau.rest.server.auth.oidc.rp;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.server.auth.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * Tests for {@link OidcSessionAuthFilter} &mdash; cookie lookup + auth-result projection.
 *
 * <p>
 * Uses {@link MockServletRequest} to drive the filter with controlled cookie arrays,
 * covering the null-cookies, no-matching-cookie, blank-value, and unknown-session branches.
 *
 * @since 10.0.0
 */
class OidcSessionAuthFilter_Test extends TestBase {

	private static final String COOKIE = "SESSION";
	private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	private static OidcSessionAuthFilter filter() {
		return new OidcSessionAuthFilter(new InMemorySessionStore(100, CLOCK), COOKIE);
	}

	private static OidcSession session(String id) {
		return new OidcSession(id, "alice", o("sess-1"),
			new ClaimsPrincipal("alice", Map.of("sub", "alice")),
			Set.of("user"), oe(), NOW, NOW.plus(Duration.ofHours(8)));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: null cookies array (line 72 true branch) — MockServletRequest returns null cookies by default.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_noCookies_returnsEmpty() throws Exception {
		var req = MockServletRequest.create("GET", "/");
		assertTrue(filter().authenticate(req).isEmpty());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: cookies present but none match the cookie name (line 76 false, line 81 true branches).
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_noMatchingCookie_returnsEmpty() throws Exception {
		var req = MockServletRequest.create("GET", "/")
			.cookies(new Cookie[]{ new Cookie("OTHER", "v") });
		assertTrue(filter().authenticate(req).isEmpty());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: matching cookie found, but session not in store (line 84 true branch).
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_unknownSessionId_returnsEmpty() throws Exception {
		var req = MockServletRequest.create("GET", "/")
			.cookies(new Cookie[]{ new Cookie(COOKIE, "no-such-id") });
		assertTrue(filter().authenticate(req).isEmpty());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: happy path — cookie present, session found, AuthResult returned (line 76 true, line 84 false).
	// -----------------------------------------------------------------------------------------------------------------

	@Test void d01_validSession_returnsAuthResult() throws Exception {
		var store = new InMemorySessionStore(100, CLOCK);
		var id = store.createSessionCookieValue(session("id-1"));
		var f = new OidcSessionAuthFilter(store, COOKIE);
		var req = MockServletRequest.create("GET", "/")
			.cookies(new Cookie[]{ new Cookie(COOKIE, id) });
		var result = f.authenticate(req);
		assertTrue(result.isPresent());
		assertEquals("alice", result.get().getPrincipal().getName());
	}
}
