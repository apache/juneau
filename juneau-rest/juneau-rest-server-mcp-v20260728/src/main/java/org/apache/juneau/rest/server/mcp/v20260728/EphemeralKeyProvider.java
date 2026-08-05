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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * Built-in default {@link KeyProvider}: a random AES-256 key with a random {@code keyId}, both fixed for the
 * lifetime of the instance.
 *
 * <p>
 * <b>Not restart-durable and not shareable across process instances by design</b> &mdash; mirrors
 * {@code AeadRequestStateCodec}'s pre-{@code KeyProvider} ephemeral-key behavior exactly, including the
 * cross-instance-unseal-fails guarantee (see {@code AeadRequestStateCodec_Test}'s {@code a06}), just relocated
 * one layer down. Operators who need cross-restart or multi-instance RESUME must supply {@link StaticKeyProvider}
 * (or a custom {@link KeyProvider}) instead of relying on this default.
 *
 * @since 10.0.0
 */
public class EphemeralKeyProvider implements KeyProvider {

	private static final int KEY_BITS = 256;
	// 8 random bytes -> 11 base64url chars (unpadded): short but collision-safe for a per-process identifier.
	private static final int KEY_ID_BYTES = 8;

	private static final Base64.Encoder B64URL = Base64.getUrlEncoder().withoutPadding();

	private final KeyedSecret current;

	/**
	 * Constructor. Generates a fresh, per-instance AES-256 key and a fresh, per-instance random {@code keyId}.
	 */
	public EphemeralKeyProvider() {
		try {
			var gen = KeyGenerator.getInstance("AES");
			gen.init(KEY_BITS);
			var key = gen.generateKey();
			var idBytes = new byte[KEY_ID_BYTES];
			new SecureRandom().nextBytes(idBytes);
			current = new KeyedSecret(B64URL.encodeToString(idBytes), key);
		} catch (Exception e) { // HTT every JDK guarantees AES-256 key generation via the standard JCE provider
			throw rex(e, "Failed to generate AES key");
		}
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
