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

import java.util.Base64;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

/**
 * Coverage for {@link KeyedSecret}'s compact-constructor guards.
 */
class KeyedSecret_Test {

	private static SecretKey aesKey() throws Exception {
		var gen = KeyGenerator.getInstance("AES");
		gen.init(256);
		return gen.generateKey();
	}

	@Test void a01_validConstructionExposesKeyIdAndKey() throws Exception {
		var a = aesKey();
		var b = new KeyedSecret("2026-08-a", a);
		assertEquals("2026-08-a", b.keyId());
		assertEquals(a, b.key());
	}

	@Test void a02_nullKeyIdThrows() throws Exception {
		var a = aesKey();
		assertThrows(IllegalArgumentException.class, () -> new KeyedSecret(null, a));
	}

	@Test void a03_blankKeyIdThrows() throws Exception {
		var a = aesKey();
		assertThrows(IllegalArgumentException.class, () -> new KeyedSecret("   ", a));
	}

	@Test void a04_overLongKeyIdThrows() throws Exception {
		var a = aesKey();
		assertThrows(IllegalArgumentException.class, () -> new KeyedSecret("x".repeat(129), a));
	}

	@Test void a05_nullKeyThrows() {
		assertThrows(IllegalArgumentException.class, () -> new KeyedSecret("2026-08-a", null));
	}

	@Test void a06_toStringRedactsKeyMaterial() {
		var key = new SecretKeySpec(new byte[32], "AES");
		var a = new KeyedSecret("my-key-id-sentinel", key);
		var s = a.toString();
		assertTrue(s.contains("my-key-id-sentinel"));
		assertTrue(s.contains("redacted"));
		assertFalse(s.contains(Base64.getEncoder().encodeToString(key.getEncoded())));
	}
}
