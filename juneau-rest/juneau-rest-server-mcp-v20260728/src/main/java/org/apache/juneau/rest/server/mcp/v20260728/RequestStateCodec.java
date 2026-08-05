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

import java.security.Principal;
import java.util.*;

/**
 * Pluggable SPI for sealing/unsealing the opaque {@code requestState} continuation token (MCP {@code 2026-07-28}
 * SEP-2322 Multi-Round-Trip Requests).
 *
 * <p>
 * Mirrors the "pluggable SPI + built-in default" shape sub-project E established for the client-side
 * {@code McpResponseCache} (not on this module's classpath): this interface plus {@link AeadRequestStateCodec}
 * (the built-in default) is the server-side, seal/unseal counterpart.
 *
 * <p>
 * Implementations must treat any verification failure (tamper, AAD mismatch, or an implementation-specific
 * integrity check) as an {@link Optional#empty()} return from {@link #unseal(String, String)} &mdash; never a
 * thrown exception. Expiry is checked by the caller ({@code McpRevision}) against
 * {@link McpRequestState#expiresAtMs()}, not by the codec itself, since expiry is a dispatcher-level policy (see
 * {@link McpMrtrConfig}), not a codec integrity concern.
 *
 * <p>
 * <b>Trust model &mdash; {@code requestState} is a bearer token.</b> The sealed token is a replayable credential:
 * anyone who holds it can resume the paused operation until it expires, and it carries no caller identity. A
 * codec (and every call site) must treat it accordingly &mdash; transport only over authenticated TLS, and
 * <b>never log</b> a {@code requestState}. Because a captured token can be replayed any number of times within
 * its TTL, resume side effects must be idempotent; the max-rounds cap bounds chain depth, not replay count (see
 * {@link McpMrtrConfig}).
 *
 * <p>
 * <b>Canonical AAD format.</b> The dispatcher binds each token to the request that produced it by passing a
 * single canonical AAD string: {@code method + '\u0000' + protocolVersion} &mdash; the JSON-RPC method name and
 * the negotiated protocol version joined by a NUL ({@code U+0000}) separator (see {@code McpRevision#aad}). NUL
 * is chosen because it can never appear in a method name or a protocol-version literal, so the concatenation is
 * unambiguous. An implementation only needs to treat the AAD as opaque bytes; the format is documented here so
 * seal and unseal call sites (and any custom codec) agree on exactly what is authenticated.
 *
 * <p>
 * <b>Authenticated principal (READY-312f F4).</b> Both {@link #seal(McpRequestState, String, Principal)} and
 * {@link #unseal(String, String, Principal)} receive the caller's authenticated {@link Principal} &mdash; the same
 * identity the F2 resource-server layer establishes (see {@code McpResourceServerSupport#principal}). This is the
 * <i>seam</i> that unblocks TODO-325's principal-bound AAD: a hardened codec can fold the caller identity into its
 * authenticated data so a {@code requestState} minted for principal A cannot be resumed by principal B. The
 * built-in {@link AeadRequestStateCodec} does <b>not</b> bind the principal yet (that binding, plus the choice of
 * <i>which</i> identity attribute to bind &mdash; subject claim, issuer+subject, full claim set &mdash; is owned by
 * TODO-325); F4 only guarantees the principal is delivered to the codec at both seal and unseal. The principal is
 * <b>nullable</b>: when RS auth is disabled or the caller is anonymous it is <jk>null</jk>, and every codec must
 * seal/unseal cleanly (no NPE) in that case. The two-argument {@link #seal(McpRequestState, String)} /
 * {@link #unseal(String, String)} convenience overloads simply pass a <jk>null</jk> (no-principal) identity.
 */
public interface RequestStateCodec {

	/**
	 * Seals a payload into an opaque token, exposing the authenticated caller identity to the codec.
	 *
	 * @param state The payload to seal. Must not be <jk>null</jk>.
	 * @param aad Additional authenticated data (the dispatcher passes the canonical
	 * 	{@code method + '\u0000' + protocolVersion} form &mdash; see the class Javadoc). Must not be <jk>null</jk>.
	 * @param principal The authenticated caller (see the class Javadoc). May be <jk>null</jk> for an anonymous
	 * 	caller or when resource-server auth is disabled.
	 * @return The opaque token. Never <jk>null</jk>.
	 */
	String seal(McpRequestState state, String aad, Principal principal);

	/**
	 * Unseals a token, verifying integrity against the supplied AAD, with the authenticated caller identity
	 * available to the codec.
	 *
	 * <p>
	 * <b>The {@code token} is arbitrary, untrusted client input.</b> It arrives verbatim from the follow-up
	 * request's {@code requestState} field, so an implementation must assume nothing about its length or content:
	 * it must not allocate buffers sized from the raw token, and it must not throw &mdash; any malformed, oversized,
	 * tampered, or AAD-mismatched input must return {@link Optional#empty()} (see the class Javadoc contract).
	 *
	 * @param token The token previously returned by {@link #seal(McpRequestState, String, Principal)}. Must not be
	 * 	<jk>null</jk>.
	 * @param aad The AAD to verify against. Must match the value passed to
	 * 	{@link #seal(McpRequestState, String, Principal)} exactly, or verification fails.
	 * @param principal The authenticated caller (see the class Javadoc). May be <jk>null</jk> for an anonymous
	 * 	caller or when resource-server auth is disabled. A codec that binds the principal (TODO-325) must fail
	 * 	verification when it differs from the sealing principal.
	 * @return The original payload, or {@link Optional#empty()} if verification fails for any reason (tamper,
	 * 	AAD mismatch, malformed token, principal mismatch).
	 */
	Optional<McpRequestState> unseal(String token, String aad, Principal principal);

	/**
	 * No-principal convenience overload of {@link #seal(McpRequestState, String, Principal)} (anonymous caller).
	 *
	 * @param state The payload to seal. Must not be <jk>null</jk>.
	 * @param aad Additional authenticated data. Must not be <jk>null</jk>.
	 * @return The opaque token. Never <jk>null</jk>.
	 */
	default String seal(McpRequestState state, String aad) {
		return seal(state, aad, null);
	}

	/**
	 * No-principal convenience overload of {@link #unseal(String, String, Principal)} (anonymous caller).
	 *
	 * @param token The token previously returned by {@link #seal(McpRequestState, String)}. Must not be <jk>null</jk>.
	 * @param aad The AAD to verify against.
	 * @return The original payload, or {@link Optional#empty()} if verification fails for any reason.
	 */
	default Optional<McpRequestState> unseal(String token, String aad) {
		return unseal(token, aad, null);
	}
}
