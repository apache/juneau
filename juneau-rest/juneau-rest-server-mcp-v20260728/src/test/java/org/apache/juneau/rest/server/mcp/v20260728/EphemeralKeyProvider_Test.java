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

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * Coverage for {@link EphemeralKeyProvider}: self-resolve, foreign-keyId miss, and cross-instance distinctness
 * (the provider-level counterpart of {@code AeadRequestStateCodec_Test}'s {@code a06}).
 */
class EphemeralKeyProvider_Test {

	@Test void a01_resolveOwnKeyIdSucceeds() {
		var a = new EphemeralKeyProvider();
		var b = a.resolveKey(a.currentKey().keyId());
		assertTrue(b.isPresent());
		assertEquals(a.currentKey().key(), b.get());
	}

	@Test void a02_resolveForeignKeyIdReturnsEmpty() {
		var a = new EphemeralKeyProvider();
		assertTrue(a.resolveKey("not-my-key-id").isEmpty());
	}

	@Test void a03_twoInstancesHaveDistinctKeyIdsAndKeys() {
		var a = new EphemeralKeyProvider();
		var b = new EphemeralKeyProvider();
		assertNotEquals(a.currentKey().keyId(), b.currentKey().keyId());
		// Compare encoded key material directly rather than relying on SecretKey#equals(), which not every
		// JCE provider's key implementation overrides meaningfully.
		assertFalse(Arrays.equals(a.currentKey().key().getEncoded(), b.currentKey().key().getEncoded()));
	}
}
