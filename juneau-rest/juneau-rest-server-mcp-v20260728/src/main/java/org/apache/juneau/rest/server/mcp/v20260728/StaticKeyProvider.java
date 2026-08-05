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

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Built-in, immutable, static-at-startup {@link KeyProvider}: an operator-supplied keyring of one or more AES
 * keys, one of which is designated the current sealing key. Unlike {@link EphemeralKeyProvider}, the keyring is
 * fixed at construction time (via {@link Builder} or {@link #of(String, SecretKey)}) rather than generated
 * randomly, so it can be shared across process instances &mdash; the fix for horizontally-scaled MRTR RESUME
 * (see {@code AeadRequestStateCodec}).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	KeyProvider <jv>kp</jv> = StaticKeyProvider.<jsm>create</jsm>()
 * 		.addKey(<js>"2026-08-a"</js>, <jv>k1</jv>)
 * 		.current(<js>"2026-08-a"</js>)
 * 		.addKey(<js>"2026-07-z"</js>, <jv>kOld</jv>)
 * 		.build();
 *
 * 	<jc>// Or, for the common single-shared-key case:</jc>
 * 	KeyProvider <jv>kp2</jv> = StaticKeyProvider.<jsm>of</jsm>(<js>"2026-08-a"</js>, <jv>k1</jv>);
 * </p>
 *
 * @since 10.0.0
 */
public final class StaticKeyProvider implements KeyProvider {

	private final KeyedSecret current;
	private final Map<String,SecretKey> keysById;

	private StaticKeyProvider(KeyedSecret current, Map<String,SecretKey> keysById) {
		this.current = current;
		// Defensive copy: keysById may be the Builder's own live map, and the Builder is not consumed by
		// build() (an operator can keep calling addKey(...)/build() to mint successive keyrings), so this
		// instance must not observe later mutations of that map.
		this.keysById = Collections.unmodifiableMap(new HashMap<>(keysById));
	}

	/**
	 * Creates a new {@link Builder}.
	 *
	 * @return A new builder. Never <jk>null</jk>.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * One-liner for the common single-shared-key case: a provider with exactly one key, designated current.
	 *
	 * @param keyId The key's identifier. Must not be <jk>null</jk> or blank.
	 * @param key The key material. Must not be <jk>null</jk>.
	 * @return A new, immutable {@link StaticKeyProvider} holding just this one key. Never <jk>null</jk>.
	 * @throws IllegalArgumentException If {@code keyId} is <jk>null</jk>/blank or {@code key} is <jk>null</jk>.
	 */
	public static StaticKeyProvider of(String keyId, SecretKey key) {
		return create().addKey(keyId, key).current(keyId).build();
	}

	/**
	 * Builds an AES {@link SecretKey} from raw key bytes.
	 *
	 * @param value The raw AES key bytes (16/24/32 bytes for AES-128/192/256). Must not be <jk>null</jk>.
	 * @return A new AES {@link SecretKey} wrapping {@code value}. Never <jk>null</jk>.
	 */
	public static SecretKey aesKey(byte[] value) {
		assertArgNotNull("value", value);
		return new SecretKeySpec(value, "AES");
	}

	/**
	 * Builds an AES {@link SecretKey} from base64-encoded key bytes.
	 *
	 * @param base64 The base64-encoded AES key bytes. Must not be <jk>null</jk> or blank.
	 * @return A new AES {@link SecretKey} wrapping the decoded bytes. Never <jk>null</jk>.
	 */
	public static SecretKey aesKey(String base64) {
		assertArgNotNullOrBlank("base64", base64);
		return aesKey(Base64.getDecoder().decode(base64));
	}

	@Override /* KeyProvider */
	public KeyedSecret currentKey() {
		return current;
	}

	@Override /* KeyProvider */
	public Optional<SecretKey> resolveKey(String keyId) {
		return Optional.ofNullable(keysById.get(keyId));
	}

	/**
	 * Builder for {@link StaticKeyProvider}.
	 */
	public static final class Builder {

		private final Map<String,SecretKey> keys = new HashMap<>();
		private String currentKeyId;

		private Builder() {}

		/**
		 * Adds a resolvable key.
		 *
		 * @param keyId The key's identifier. Must not be <jk>null</jk> or blank.
		 * @param key The key material. Must not be <jk>null</jk>.
		 * @return This object.
		 * @throws IllegalArgumentException If {@code keyId} is <jk>null</jk>/blank or {@code key} is
		 * 	<jk>null</jk>.
		 */
		public Builder addKey(String keyId, SecretKey key) {
			assertArgNotNullOrBlank("keyId", keyId);
			assertArgNotNull("key", key);
			keys.put(keyId, key);
			return this;
		}

		/**
		 * Designates the current sealing key by identifier. Must name a key previously added via
		 * {@link #addKey(String, SecretKey)}.
		 *
		 * @param keyId The identifier of the key to designate as current. Must not be <jk>null</jk> or blank.
		 * @return This object.
		 * @throws IllegalArgumentException If {@code keyId} is <jk>null</jk> or blank.
		 */
		public Builder current(String keyId) {
			assertArgNotNullOrBlank("keyId", keyId);
			currentKeyId = keyId;
			return this;
		}

		/**
		 * Builds an immutable {@link StaticKeyProvider} snapshot of this builder's current state.
		 *
		 * @return A new, immutable {@link StaticKeyProvider}. Never <jk>null</jk>.
		 * @throws IllegalArgumentException If no current key was designated via {@link #current(String)}, or
		 * 	the designated current key was never added via {@link #addKey(String, SecretKey)}.
		 */
		public StaticKeyProvider build() {
			assertArg(currentKeyId != null, "No current key designated; call current(keyId) before build().");
			var key = keys.get(currentKeyId);
			assertArg(key != null, "current(''%s'') does not name a key added via addKey(...).", currentKeyId);
			return new StaticKeyProvider(new KeyedSecret(currentKeyId, key), keys);
		}
	}
}
