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
package org.apache.juneau.rest.server.mcp.v20260728;

import static org.junit.jupiter.api.Assertions.*;

import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

/**
 * Coverage for {@link StaticKeyProvider}: the {@code aesKey(...)} helpers, builder current-designation and
 * multi-key resolve, unknown-keyId miss, {@code build()} guards, the {@code of(...)} one-liner, and
 * post-{@code build()} immutability.
 */
class StaticKeyProvider_Test {

	private static SecretKey randomAesKey() {
		var b = new byte[32];
		new SecureRandom().nextBytes(b);
		return StaticKeyProvider.aesKey(b);
	}

	@Test void a01_aesKeyFromBytesProducesAesSecretKey() {
		var a = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
		var b = StaticKeyProvider.aesKey(a);
		assertEquals("AES", b.getAlgorithm());
		assertArrayEquals(a, b.getEncoded());
	}

	@Test void a02_aesKeyFromBase64ProducesAesSecretKey() {
		var a = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
		var b = StaticKeyProvider.aesKey(Base64.getEncoder().encodeToString(a));
		assertEquals("AES", b.getAlgorithm());
		assertArrayEquals(a, b.getEncoded());
	}

	@Test void b01_currentDesignationAndMultiKeyResolve() {
		var a = randomAesKey();
		var b = randomAesKey();
		var c = StaticKeyProvider.create().addKey("2026-08-a", a).current("2026-08-a").addKey("2026-07-z", b).build();
		assertEquals("2026-08-a", c.currentKey().keyId());
		assertEquals(a, c.currentKey().key());
		assertEquals(a, c.resolveKey("2026-08-a").orElseThrow());
		assertEquals(b, c.resolveKey("2026-07-z").orElseThrow());
	}

	@Test void b02_unknownKeyIdResolvesToEmpty() {
		var a = StaticKeyProvider.create().addKey("2026-08-a", randomAesKey()).current("2026-08-a").build();
		assertTrue(a.resolveKey("unknown").isEmpty());
	}

	@Test void c01_buildRejectsMissingCurrent() {
		var builder = StaticKeyProvider.create().addKey("2026-08-a", randomAesKey());
		assertThrows(IllegalArgumentException.class, builder::build);
	}

	@Test void c02_buildRejectsDanglingCurrent() {
		var builder = StaticKeyProvider.create().addKey("2026-08-a", randomAesKey()).current("does-not-exist");
		assertThrows(IllegalArgumentException.class, builder::build);
	}

	@Test void c03_addKeyRejectsNullOrBlankKeyId() {
		var a = StaticKeyProvider.create();
		var b = randomAesKey();
		assertThrows(IllegalArgumentException.class, () -> a.addKey(null, b));
		assertThrows(IllegalArgumentException.class, () -> a.addKey("  ", b));
	}

	@Test void c04_addKeyRejectsNullKey() {
		var a = StaticKeyProvider.create();
		assertThrows(IllegalArgumentException.class, () -> a.addKey("2026-08-a", null));
	}

	@Test void c05_currentRejectsNullOrBlankKeyId() {
		var a = StaticKeyProvider.create();
		assertThrows(IllegalArgumentException.class, () -> a.current(null));
		assertThrows(IllegalArgumentException.class, () -> a.current("  "));
	}

	@Test void d01_ofOneLinerBuildsSingleKeyProvider() {
		var a = randomAesKey();
		var b = StaticKeyProvider.of("2026-08-a", a);
		assertEquals("2026-08-a", b.currentKey().keyId());
		assertEquals(a, b.resolveKey("2026-08-a").orElseThrow());
	}

	@Test void e01_builderReuseAfterBuildDoesNotMutatePriorInstance() {
		var a = randomAesKey();
		var b = randomAesKey();
		var builder = StaticKeyProvider.create().addKey("2026-08-a", a).current("2026-08-a");
		var c = builder.build();
		builder.addKey("2026-07-z", b);
		assertTrue(c.resolveKey("2026-07-z").isEmpty(), "mutating the builder after build() must not affect the already-built instance");
	}
}
