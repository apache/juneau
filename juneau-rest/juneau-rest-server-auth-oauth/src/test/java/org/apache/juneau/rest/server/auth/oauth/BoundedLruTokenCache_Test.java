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
package org.apache.juneau.rest.server.auth.oauth;

import static org.apache.juneau.commons.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.security.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link BoundedLruTokenCache} &mdash; size cap, TTL eviction, mixed entry types, and basic
 * threading.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S5778", // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice
	"java:S2925"  // Thread.sleep required to test TTL expiry; BoundedLruTokenCache has no clock injection point
})
class BoundedLruTokenCache_Test extends TestBase {

	private static final Principal ALICE = () -> "alice";
	private static final Principal BOB = () -> "bob";

	// Fixed reference instant: the cache takes "now" as an explicit parameter, so anchoring
	// both token expiry and the query time to a constant replaces the system clock (java:S8692).
	private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

	@Test void a01_create_defaultMaxEntries() {
		var c = BoundedLruTokenCache.create();
		assertEquals(BoundedLruTokenCache.DEFAULT_MAX_ENTRIES, c.getMaxEntries());
	}

	@Test void a02_create_customMaxEntries() {
		var c = BoundedLruTokenCache.create(7);
		assertEquals(7, c.getMaxEntries());
	}

	@Test void a03_create_rejectsNonPositive() {
		assertThrows(IllegalArgumentException.class, () -> BoundedLruTokenCache.create(0));
		assertThrows(IllegalArgumentException.class, () -> BoundedLruTokenCache.create(-1));
	}

	@Test void b01_putGet_principal_happyPath() {
		var c = BoundedLruTokenCache.create();
		c.putPrincipal("t1", ALICE, Duration.ofSeconds(60));
		var got = c.getPrincipal("t1");
		assertTrue(got.isPresent());
		assertSame(ALICE, got.get());
	}

	@Test void b02_getPrincipal_missingKey_empty() {
		var c = BoundedLruTokenCache.create();
		assertTrue(c.getPrincipal("missing").isEmpty());
	}

	@Test void b03_putPrincipal_rejectsZeroOrNegativeTtl() {
		var c = BoundedLruTokenCache.create();
		assertThrows(IllegalArgumentException.class, () -> c.putPrincipal("t", ALICE, Duration.ZERO));
		assertThrows(IllegalArgumentException.class, () -> c.putPrincipal("t", ALICE, Duration.ofSeconds(-1)));
	}

	@Test void c01_sizeCap_evictsLeastRecentlyUsed() {
		var c = BoundedLruTokenCache.create(3);
		c.putPrincipal("a", ALICE, Duration.ofMinutes(5));
		c.putPrincipal("b", BOB, Duration.ofMinutes(5));
		c.putPrincipal("c", ALICE, Duration.ofMinutes(5));
		c.getPrincipal("a");
		c.getPrincipal("b");
		c.putPrincipal("d", BOB, Duration.ofMinutes(5));
		assertEquals(3, c.size());
		assertTrue(c.getPrincipal("c").isEmpty());
	}

	@Test void d01_putGet_token_happyPath() {
		var c = BoundedLruTokenCache.create();
		var token = sampleToken(NOW.plusSeconds(60));
		c.putToken("k", token);
		var got = c.getToken("k", NOW, Duration.ZERO);
		assertTrue(got.isPresent());
		assertEquals("access-1", got.get().accessToken());
	}

	@Test void d02_getToken_expired_evictsAndReturnsEmpty() {
		var c = BoundedLruTokenCache.create();
		var token = sampleToken(NOW.minusSeconds(10));
		c.putToken("k", token);
		var got = c.getToken("k", NOW, Duration.ZERO);
		assertTrue(got.isEmpty());
		assertEquals(0, c.size());
	}

	@Test void d03_getToken_skewSubtractedFromExpiry() {
		var c = BoundedLruTokenCache.create();
		var token = sampleToken(NOW.plusSeconds(10));
		c.putToken("k", token);
		var skew = Duration.ofSeconds(30);
		assertTrue(c.getToken("k", NOW, skew).isEmpty());
	}

	@Test void b04_getPrincipal_expired_evictsAndReturnsEmpty() throws Exception {
		// line 117: expired principal → entry removed, Optional.empty() returned.
		var c = BoundedLruTokenCache.create();
		c.putPrincipal("t-exp", ALICE, Duration.ofMillis(1));
		Thread.sleep(5);
		assertTrue(c.getPrincipal("t-exp").isEmpty());
		assertEquals(0, c.size());
	}

	@Test void d04_getToken_negativeSkew_throws() {
		// line 139: negative skew → IllegalArgumentException.
		var c = BoundedLruTokenCache.create();
		c.putToken("k", sampleToken(NOW.plusSeconds(60)));
		assertThrows(IllegalArgumentException.class,
			() -> c.getToken("k", NOW, Duration.ofSeconds(-1)));
	}

	@Test void e01_invalidate_removesEntry() {
		var c = BoundedLruTokenCache.create();
		c.putPrincipal("a", ALICE, Duration.ofMinutes(5));
		c.invalidate("a");
		assertTrue(c.getPrincipal("a").isEmpty());
		c.invalidate("nonexistent");
	}

	@Test void e02_typeMismatch_returnsEmpty() {
		var c = BoundedLruTokenCache.create();
		c.putPrincipal("a", ALICE, Duration.ofMinutes(5));
		Optional<OAuthToken> mismatch = c.getToken("a", NOW, Duration.ZERO);
		assertTrue(mismatch.isEmpty());
	}

	private static OAuthToken sampleToken(Instant expiresAt) {
		return new OAuthToken("access-1", "Bearer", expiresAt, opte(), opte(), opte());
	}
}
