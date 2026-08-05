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

import java.util.Optional;

import javax.crypto.SecretKey;

/**
 * SPI for supplying the key(s) used to seal/unseal MCP MRTR {@code requestState} continuation tokens (see
 * {@code AeadRequestStateCodec}).
 *
 * <p>
 * A dedicated abstraction beneath the AEAD codec so an operator can supply a stable, shared sealing key (fixing
 * horizontally-scaled RESUME behind a load balancer) and rotate keys via a {@code keyId}, while the codec's
 * zero-config default ({@link EphemeralKeyProvider}) keeps today's per-process ephemeral-key behavior unchanged.
 *
 * <p>
 * <b>Thread-safety.</b> A single provider instance is shared across all requests against a binding (the codec
 * is per-binding), so implementations must be safe for concurrent calls to both {@link #currentKey()} and
 * {@link #resolveKey(String)}.
 *
 * <p>
 * <b>Never-throw contract.</b> {@link #resolveKey(String)} is called on the unseal path with an untrusted,
 * client-supplied {@code keyId}. Implementations must treat any unknown or retired {@code keyId} as
 * {@link Optional#empty()} and must never throw &mdash; the caller ({@code AeadRequestStateCodec#unseal}) has no
 * exception-handling path for this call.
 *
 * <p>
 * Not annotated {@code @FunctionalInterface}: unlike single-method SPIs such as
 * {@link org.apache.juneau.rest.server.auth.ApiKeyStore}, this SPI declares two abstract methods.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link KeyedSecret}
 * 	<li class='jc'>{@link EphemeralKeyProvider}
 * 	<li class='jc'>{@link StaticKeyProvider}
 * </ul>
 *
 * @since 10.0.0
 */
public interface KeyProvider {

	/**
	 * The key to seal new tokens with, together with its identifier.
	 *
	 * <p>
	 * May return a different value over time &mdash; a changing return value is the sanctioned mechanism for
	 * live runtime key rotation in a custom provider. Built-in providers ({@link EphemeralKeyProvider},
	 * {@link StaticKeyProvider}) return a fixed value for the lifetime of the instance.
	 *
	 * @return The current sealing key. Never <jk>null</jk>.
	 */
	KeyedSecret currentKey();

	/**
	 * Resolves a {@code keyId} seen on an incoming token to the key that sealed it.
	 *
	 * @param keyId The key identifier read from the untrusted, client-supplied token, before decryption. Never
	 * 	<jk>null</jk>.
	 * @return The resolved key, or {@link Optional#empty()} if {@code keyId} is unknown or retired. Never
	 * 	throws.
	 */
	Optional<SecretKey> resolveKey(String keyId);
}
