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

import javax.crypto.SecretKey;

/**
 * A key paired with the identifier an operator chose for it, as returned by {@link KeyProvider#currentKey()}
 * and resolved by {@link KeyProvider#resolveKey(String)}.
 *
 * <p>
 * <b>Never log {@code key}.</b> Mirrors {@link RequestStateCodec}'s "never log" contract for the sealed token:
 * this record holds the actual sealing key, so {@link #toString()} deliberately redacts {@code key} rather than
 * relying on the record's auto-generated form, which would print whatever a custom/HSM {@link SecretKey} impl's
 * own {@code toString()} legally chooses to include.
 *
 * @param keyId An opaque, operator-chosen short identifier for {@code key} (e.g. {@code "2026-08-a"}). Must not
 * 	be <jk>null</jk> or blank, and is length-bounded so a sealed token's cleartext {@code keyId} wire segment
 * 	(see {@code AeadRequestStateCodec}'s versioned wire format) stays bounded.
 * @param key The key material. Must not be <jk>null</jk>. The AEAD codec is AES-GCM-specific by design, so
 * 	non-AES key material fails at cipher-init time in {@code AeadRequestStateCodec}, not here.
 * @since 10.0.0
 */
public record KeyedSecret(String keyId, SecretKey key) {

	// Bounds the base64url'd keyId wire segment so an oversized keyId cannot inflate a sealed token.
	private static final int MAX_KEY_ID_CHARS = 128;

	/**
	 * Compact constructor &mdash; validates {@code keyId} is non-null/non-blank/length-bounded and {@code key}
	 * is non-null.
	 */
	public KeyedSecret {
		assertArgNotNullOrBlank("keyId", keyId);
		assertArg(keyId.length() <= MAX_KEY_ID_CHARS, "Argument 'keyId' length (%s) exceeds max of %s chars.", keyId.length(), MAX_KEY_ID_CHARS);
		assertArgNotNull("key", key);
	}

	/**
	 * Redacts {@code key} so key material never reaches logs via this record's {@code toString()}.
	 */
	@Override
	public String toString() {
		return "KeyedSecret[keyId=" + keyId + ", key=<redacted>]";
	}
}
