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
package org.apache.juneau.rest.auth;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link ClaimsPrincipal} &mdash; defensive-copy immutability, typed claim coercion,
 * and the {@link ClaimsPrincipal#getName()} / {@link ClaimsPrincipal#hasClaim(String)} contracts.
 *
 * @since 10.0.0
 */
@SuppressWarnings({"java:S5778" /* assertThrows lambdas with chained calls; intermediate invocations do not throw in practice */})
class ClaimsPrincipal_Test extends TestBase {

	@Test void a01_nameAndClaims() {
		var p = new ClaimsPrincipal("alice", Map.of("scope", "read", "iat", 1700000000L));
		Assertions.assertEquals("alice", p.getName());
		Assertions.assertEquals(2, p.getClaims().size());
		Assertions.assertTrue(p.hasClaim("scope"));
		Assertions.assertFalse(p.hasClaim("missing"));
	}

	@Test void a02_nullClaimsMapTreatedAsEmpty() {
		var p = new ClaimsPrincipal("bob", null);
		Assertions.assertEquals("bob", p.getName());
		Assertions.assertTrue(p.getClaims().isEmpty());
	}

	@Test void a03_claimsAreUnmodifiable() {
		var p = new ClaimsPrincipal("carol", Map.of("k", "v"));
		Assertions.assertThrows(UnsupportedOperationException.class,
			() -> p.getClaims().put("evil", "value"));
	}

	@Test void a04_constructorDefensivelyCopiesSource() {
		var source = new HashMap<String,Object>();
		source.put("scope", "read");
		var p = new ClaimsPrincipal("dave", source);
		source.put("scope", "admin");
		Assertions.assertEquals("read", p.getClaims().get("scope"),
			"source mutation should not leak into ClaimsPrincipal");
	}

	@Test void a05_blankNameRejected() {
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> new ClaimsPrincipal("", Map.of()));
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> new ClaimsPrincipal(null, Map.of()));
	}

	@Test void b01_getClaimStringIdentity() {
		var p = new ClaimsPrincipal("alice", Map.of("sub", "alice"));
		Assertions.assertEquals("alice", p.getClaim("sub", String.class).orElseThrow());
	}

	@Test void b02_getClaimNumberCoercion() {
		var p = new ClaimsPrincipal("alice", Map.of("iat", 1700000000L));
		Assertions.assertEquals(1700000000L, p.getClaim("iat", Long.class).orElseThrow());
		Assertions.assertEquals(1700000000, p.getClaim("iat", Integer.class).orElseThrow());
		Assertions.assertEquals(1.7E9, p.getClaim("iat", Double.class).orElseThrow(), 1.0);
	}

	@Test void b03_getClaimToStringFallback() {
		var p = new ClaimsPrincipal("alice", Map.of("count", 42));
		Assertions.assertEquals("42", p.getClaim("count", String.class).orElseThrow());
	}

	@Test void b04_getClaimMismatchedTypeYieldsEmpty() {
		var p = new ClaimsPrincipal("alice", Map.of("scope", "read"));
		Assertions.assertTrue(p.getClaim("scope", Long.class).isEmpty());
	}

	@Test void b05_getClaimMissingYieldsEmpty() {
		var p = new ClaimsPrincipal("alice", Map.of());
		Assertions.assertTrue(p.getClaim("missing", String.class).isEmpty());
	}

	@Test void b06_getClaimPrimitiveAndFloatCoercion() {
		var p = new ClaimsPrincipal("alice", Map.of("n", 42));
		// Primitive class literals route through the same coerce() branches as their boxed counterparts.
		Assertions.assertEquals(42, p.getClaim("n", int.class).orElseThrow());
		Assertions.assertEquals(42L, p.getClaim("n", long.class).orElseThrow());
		Assertions.assertEquals(42.0, p.getClaim("n", double.class).orElseThrow(), 0.001);
		Assertions.assertEquals(42.0f, p.getClaim("n", float.class).orElseThrow(), 0.001f);
		Assertions.assertEquals(42.0f, p.getClaim("n", Float.class).orElseThrow(), 0.001f);
	}

	@Test void b07_getClaimListValueDirectReturn() {
		var p = new ClaimsPrincipal("alice", Map.of("aud", List.of("api", "admin")));
		var aud = p.getClaim("aud", List.class).orElseThrow();
		Assertions.assertEquals(2, aud.size());
	}

	@Test void b08_getClaimRejectsNullArgs() {
		var p = new ClaimsPrincipal("alice", Map.of());
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> p.getClaim(null, String.class));
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> p.getClaim("k", null));
	}

	@Test void c01_toStringContainsNameAndClaimKeys() {
		var p = new ClaimsPrincipal("alice", Map.of("scope", "read", "sub", "alice"));
		var s = p.toString();
		Assertions.assertTrue(s.contains("alice"));
		Assertions.assertTrue(s.contains("scope"));
		Assertions.assertTrue(s.contains("sub"));
	}
}
