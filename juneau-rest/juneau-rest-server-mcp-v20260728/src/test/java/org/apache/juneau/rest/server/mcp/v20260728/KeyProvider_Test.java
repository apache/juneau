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

import java.util.Optional;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

/**
 * Interface-contract coverage for {@link KeyProvider}, proving the SPI shape itself is sound via a trivial fake
 * implementation, before {@link EphemeralKeyProvider} / {@link StaticKeyProvider}'s real behavior exists.
 */
class KeyProvider_Test {

	private static SecretKey aesKey() throws Exception {
		var gen = KeyGenerator.getInstance("AES");
		gen.init(256);
		return gen.generateKey();
	}

	private static final class FakeKeyProvider implements KeyProvider {

		private final KeyedSecret current;

		FakeKeyProvider(KeyedSecret current) {
			this.current = current;
		}

		@Override /* KeyProvider */
		public KeyedSecret currentKey() {
			return current;
		}

		@Override /* KeyProvider */
		public Optional<SecretKey> resolveKey(String keyId) {
			return keyId.equals(current.keyId()) ? Optional.of(current.key()) : Optional.empty();
		}
	}

	@Test void a01_currentKeyIsNeverNull() throws Exception {
		var a = new FakeKeyProvider(new KeyedSecret("k1", aesKey()));
		assertNotNull(a.currentKey());
	}

	@Test void a02_resolveKeyMissReturnsEmptyAndNeverThrows() throws Exception {
		var a = new FakeKeyProvider(new KeyedSecret("k1", aesKey()));
		assertDoesNotThrow(() -> assertTrue(a.resolveKey("unknown-key-id").isEmpty()));
	}

	@Test void a03_resolveKeyHitReturnsKey() throws Exception {
		var key = aesKey();
		var a = new FakeKeyProvider(new KeyedSecret("k1", key));
		var b = a.resolveKey("k1");
		assertTrue(b.isPresent());
		assertArrayEquals(key.getEncoded(), b.get().getEncoded());
	}
}
