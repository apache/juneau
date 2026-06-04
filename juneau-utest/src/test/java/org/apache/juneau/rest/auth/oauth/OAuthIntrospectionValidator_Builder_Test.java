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

import java.net.URI;
import java.time.Duration;

import org.apache.juneau.TestBase;
import org.junit.jupiter.api.Test;

/**
 * Builder validation tests for {@link OAuthIntrospectionValidator}.
 *
 * @since 10.0.0
 */
@SuppressWarnings({"java:S5778" /* assertThrows lambdas with chained calls; intermediate invocations do not throw in practice */})
class OAuthIntrospectionValidator_Builder_Test extends TestBase {

	private static OAuthIntrospectionValidator.Builder fresh() {
		return OAuthIntrospectionValidator.create()
			.introspectionEndpoint(URI.create("https://idp.example.com/oauth2/introspect"))
			.clientId("api-server")
			.clientSecret("secret");
	}

	@Test void a01_build_happyPath() {
		var v = fresh().build();
		assertNotNull(v);
		assertEquals("api-server", v.getClientId());
		assertEquals(OAuthIntrospectionValidator.DEFAULT_CACHE_TTL, v.getCacheTtl());
		assertEquals(BoundedLruTokenCache.DEFAULT_MAX_ENTRIES, ((BoundedLruTokenCache) v.getTokenCache()).getMaxEntries());
	}

	@Test void a02_build_requiresEndpoint() {
		assertThrows(IllegalStateException.class, () -> OAuthIntrospectionValidator.create()
			.clientId("x").clientSecret("y").build());
	}

	@Test void a03_build_requiresClientId() {
		assertThrows(IllegalStateException.class, () -> OAuthIntrospectionValidator.create()
			.introspectionEndpoint(URI.create("https://x.example.com")).clientSecret("y").build());
	}

	@Test void a04_build_requiresClientSecret() {
		assertThrows(IllegalStateException.class, () -> OAuthIntrospectionValidator.create()
			.introspectionEndpoint(URI.create("https://x.example.com")).clientId("y").build());
	}

	@Test void b01_cacheTtl_rejectsZeroOrNegative() {
		assertThrows(IllegalArgumentException.class, () -> fresh().cacheTtl(Duration.ZERO));
		assertThrows(IllegalArgumentException.class, () -> fresh().cacheTtl(Duration.ofSeconds(-1)));
	}

	@Test void b02_cacheTtl_rejectsOverMaximum() {
		assertThrows(IllegalArgumentException.class, () -> fresh().cacheTtl(OAuthIntrospectionValidator.MAX_CACHE_TTL.plusMinutes(1)));
	}

	@Test void b03_cacheTtl_acceptsAtMax() {
		var v = fresh().cacheTtl(OAuthIntrospectionValidator.MAX_CACHE_TTL).build();
		assertEquals(OAuthIntrospectionValidator.MAX_CACHE_TTL, v.getCacheTtl());
	}

	@Test void c01_requiredScopes_accumulate() {
		var v = fresh().requiredScopes("a").requiredScopes("b", "c").build();
		assertEquals(java.util.Set.of("a", "b", "c"), v.getRequiredScopes());
	}

	@Test void c02_requiredScopes_blankRejected() {
		assertThrows(IllegalArgumentException.class, () -> fresh().requiredScopes(""));
		assertThrows(IllegalArgumentException.class, () -> fresh().requiredScopes("ok", "  "));
	}

	@Test void d01_clientSecret_blankRejected() {
		assertThrows(IllegalArgumentException.class, () -> OAuthIntrospectionValidator.create().clientSecret(""));
	}
}
