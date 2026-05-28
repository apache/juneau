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
import java.security.Principal;
import java.time.*;

import org.apache.juneau.TestBase;
import org.apache.juneau.rest.auth.ClaimsPrincipal;
import org.junit.jupiter.api.Test;

/**
 * Cache-side tests for {@link OAuthIntrospectionValidator}: the validator must short-circuit on cache hits.
 *
 * <p>
 * We seed the underlying {@link TokenCache} directly with a principal, then call {@code validate(token)}.
 * If the validator hits the cache (it should), it returns the seeded principal without ever touching the
 * introspection endpoint (which is set to a bogus URI that would fail if dialed).
 *
 * @since 9.5.0
 */
class OAuthIntrospectionValidator_Cache_Test extends TestBase {

	@Test void a01_cacheHit_shortCircuitsIntrospection() throws Exception {
		var cache = BoundedLruTokenCache.create();
		var seeded = new ClaimsPrincipal("alice", java.util.Map.of("sub", "alice", "scope", "read"));
		cache.putPrincipal("token-1", seeded, Duration.ofMinutes(5));
		var v = OAuthIntrospectionValidator.create()
			.introspectionEndpoint(URI.create("http://invalid.invalid.invalid/introspect"))
			.clientId("client")
			.clientSecret("secret")
			.tokenCache(cache)
			.build();
		Principal got = v.validate("token-1");
		assertSame(seeded, got);
	}

	@Test void a02_cacheMiss_attemptsIntrospection() {
		var cache = BoundedLruTokenCache.create();
		var v = OAuthIntrospectionValidator.create()
			.introspectionEndpoint(URI.create("http://127.0.0.1:1/introspect"))
			.clientId("client")
			.clientSecret("secret")
			.tokenCache(cache)
			.build();
		assertThrows(Exception.class, () -> v.validate("token-2"));
	}
}
