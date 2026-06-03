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
package org.apache.juneau.rest.auth.oauth;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;

import org.apache.juneau.TestBase;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link OAuthToken} record.
 *
 * @since 9.5.0
 */
@SuppressWarnings({"java:S5778" /* assertThrows lambdas with chained calls; intermediate invocations do not throw in practice */})
class OAuthToken_Test extends TestBase {

	@Test void a01_construct_happyPath() {
		var t = new OAuthToken("a", "Bearer", Instant.parse("2030-01-01T00:00:00Z"),
			Optional.of("r"), Optional.of(Set.of("read")), Optional.empty());
		assertEquals("a", t.accessToken());
		assertEquals("Bearer", t.tokenType());
		assertEquals("r", t.refreshToken().get());
		assertEquals(Set.of("read"), t.scope().get());
	}

	@Test void a02_construct_nullFieldsRejected() {
		assertThrows(NullPointerException.class, () -> new OAuthToken(null, "Bearer", Instant.MAX,
			Optional.empty(), Optional.empty(), Optional.empty()));
	}

	@Test void b01_isExpired_pastExpiry_true() {
		var t = new OAuthToken("a", "Bearer", Instant.parse("2020-01-01T00:00:00Z"),
			Optional.empty(), Optional.empty(), Optional.empty());
		assertTrue(t.isExpired(Instant.parse("2020-01-01T00:00:00Z"), Duration.ZERO));
		assertTrue(t.isExpired(Instant.parse("2025-01-01T00:00:00Z"), Duration.ZERO));
	}

	@Test void b02_isExpired_futureExpiry_false() {
		var t = new OAuthToken("a", "Bearer", Instant.parse("2030-01-01T00:00:00Z"),
			Optional.empty(), Optional.empty(), Optional.empty());
		assertFalse(t.isExpired(Instant.parse("2020-01-01T00:00:00Z"), Duration.ZERO));
	}

	@Test void b03_isExpired_skewBringsForward() {
		var t = new OAuthToken("a", "Bearer", Instant.parse("2030-01-01T00:00:10Z"),
			Optional.empty(), Optional.empty(), Optional.empty());
		assertTrue(t.isExpired(Instant.parse("2030-01-01T00:00:00Z"), Duration.ofMinutes(1)));
	}

	@Test void c01_scope_defensivelyCopied() {
		var src = new HashSet<>(Set.of("a"));
		var t = new OAuthToken("a", "Bearer", Instant.MAX, Optional.empty(), Optional.of(src), Optional.empty());
		assertThrows(UnsupportedOperationException.class, () -> t.scope().get().add("b"));
	}
}
