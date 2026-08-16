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
package org.apache.juneau.rest.server.auth.oidc.rp;

import java.time.*;
import java.util.*;

/**
 * SPI for the single-use, TTL-bounded {@code state} &rarr; ({@code nonce}, PKCE {@code code_verifier},
 * redirect target) association created during {@link OidcRelyingParty#startLogin} and consumed during
 * {@link OidcRelyingParty#completeLogin}.
 *
 * <p>
 * The shipped default is {@link InMemoryLoginStateStore} (per-JVM, bounded, TTL-swept).  Supply a shared
 * (Redis / JDBC) implementation via {@link OidcRelyingParty.Builder#loginStateStore(LoginStateStore)} to
 * support a clustered deployment where {@code /login} and {@code /callback} may land on different nodes
 * without sticky sessions &mdash; the same motivation as a distributed {@link SessionStore}.  When the
 * hook is unused, behavior is identical to the single-node default.
 *
 * <h5 class='section'>Security contract (read before implementing)</h5>
 *
 * <p>
 * <b>Secret-bearing.</b> A stored entry carries the PKCE {@code code_verifier} (and {@code nonce}).  With a
 * stolen authorization {@code code} (Referer, proxy log, browser history) the {@code code_verifier} IS the
 * token-exchange credential.  The in-memory default keeps it on the JVM heap; a shared (Redis / JDBC)
 * implementation writes it to an external backend.  Implementations therefore MUST protect confidentiality
 * and integrity: TLS in transit and a locked-down ACL are the floor; application-level encryption-at-rest
 * is strongly recommended.  Implementations and callers MUST NOT log a {@link PendingLogin} (its
 * {@code toString()} is redacted for exactly this reason), and MUST NOT put {@link PendingLogin#codeVerifier()}
 * or {@link PendingLogin#nonce()} into MDC, metric tags, or field-wise serializers &mdash; the accessors
 * exist only for the token exchange and the ID-token {@code nonce} check.
 *
 * <p>
 * <b>The framework does not trust the store as an oracle.</b> A writable shared store (mis-ACL,
 * SSRF-to-Redis, replica, backup restore, compromised sibling service) can mint or alter records.  The
 * relying party therefore re-validates on consume: it re-sanitizes {@code redirectTarget} to a safe
 * relative path before redirecting (a tampered absolute / external URL is discarded in favor of the
 * configured post-login default), and it re-checks {@code expiresAt} against its own {@link java.time.Clock}.
 * The store MAY evict early; it is NOT the authoritative expiry decision-maker.  Note this defeats an
 * <i>honest</i> store that merely forgot to expire a record (past {@code expiresAt}); a malicious rewrite
 * of {@code expiresAt} to a far-future instant is the same trust boundary as writing a fresh forged blob
 * (an integrity concern an HMAC would address), which the framework does not attempt to defeat here.
 *
 * <p>
 * <b>Thread-safe.</b> Implementations MUST be safe for concurrent {@code store} / {@code consume} from
 * multiple request threads (as {@link SessionStore} requires).
 *
 * <p>
 * <b>Per-process by default.</b> The shipped default enforces single-use only within one JVM.  Cross-node
 * single-use requires a shared backend with an atomic consume primitive (see {@link #consume(String)}).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link InMemoryLoginStateStore}
 * 	<li class='jc'>{@link OidcRelyingParty.Builder#loginStateStore(LoginStateStore)}
 * 	<li class='jc'>{@link SessionStore}
 * </ul>
 *
 * @since 10.0.0
 */
public interface LoginStateStore {

	/** Hard cap on the framework-enforced login-state TTL &mdash; the single public home for the ceiling. */
	Duration MAX_TTL = Duration.ofMinutes(30);

	/**
	 * A pending-login association awaiting the IdP callback.
	 *
	 * <p>
	 * <b>Secret-bearing record.</b> {@code codeVerifier} and {@code nonce} are credentials (see the SPI
	 * security contract).  {@code toString()} redacts both, matching {@code OAuthToken.toString()} and the
	 * MCP {@code PendingAuthorization} precedent, so a stray log line, exception message, or debugger dump
	 * does not disclose them.
	 *
	 * <p>
	 * <b>Framework-minted timestamps.</b> The relying party stamps {@code createdAt} (from its configured
	 * {@link java.time.Clock}) and {@code expiresAt} ({@code createdAt} plus the RP's capped
	 * {@code stateNonceTtl}).  A store MUST persist these as opaque values and MUST NOT recompute them at
	 * consume time &mdash; the RP re-checks {@code expiresAt} independently, so an honest store that forgot
	 * to expire a record cannot extend usability.  Either timestamp may be {@code null} only for a foreign /
	 * deserialized blob; the RP treats a {@code null} {@code createdAt} / {@code expiresAt} on consume as
	 * expired (fail-closed), so no compact-constructor {@code requireNonNull} is imposed here.
	 *
	 * @param nonce The OIDC {@code nonce}.  Redacted in {@code toString()}.
	 * @param codeVerifier The PKCE {@code code_verifier}.  Redacted in {@code toString()}.
	 * @param redirectTarget The post-login redirect target.  May be {@code null}.  Re-validated by the RP.
	 * @param createdAt Framework-minted store instant.
	 * @param expiresAt Framework-minted expiry ceiling ({@code createdAt + stateNonceTtl}).
	 */
	record PendingLogin(String nonce, String codeVerifier, String redirectTarget, Instant createdAt, Instant expiresAt) {
		@Override
		public String toString() {
			return "PendingLogin(nonce=<redacted>,codeVerifier=<redacted>,redirectTarget=" + redirectTarget
				+ ",createdAt=" + createdAt + ",expiresAt=" + expiresAt + ")";
		}
	}

	/**
	 * Stores a framework-built pending-login association keyed by {@code state}.
	 *
	 * <p>
	 * Implementations MUST persist the association durably enough that a subsequent {@link #consume(String)}
	 * on any node handling the callback can retrieve it.  On an <b>infrastructure failure</b> (backend
	 * unreachable, write rejected) implementations MUST throw rather than silently drop the record:
	 * {@code store} is invoked on the {@code startLogin} leg before any authorization URL is issued, so a
	 * thrown exception fails the flow closed with no half-initiated login.  Implementations MUST NOT log the
	 * {@link PendingLogin} payload; wrap / sanitize backend exceptions so the {@code codeVerifier} /
	 * {@code state} do not leak into a 500 body or log.
	 *
	 * <p>
	 * Last-writer-wins: a repeated {@code store} for the same {@code state} overwrites (a Nimbus
	 * {@code State} collision is astronomical).  A backend MAY use {@code SET NX} but is not required to.  A
	 * backend MAY use the remaining time to {@code expiresAt} as a GC / {@code EXPIRE} hint, but GC failure
	 * MUST NOT extend usability past the framework's re-checked {@code expiresAt}.
	 *
	 * @param state The opaque {@code state} value.  Must not be {@code null} or blank.
	 * @param pending The framework-built association (already stamped with {@code createdAt} / {@code expiresAt}).
	 * 	Must not be {@code null}.
	 */
	void store(String state, PendingLogin pending);

	/**
	 * Atomically removes and returns the pending-login association for {@code state}, if present.
	 *
	 * <p>
	 * <b>Single-use / atomicity is mandatory (TOCTOU).</b> The removal and the read MUST be a single atomic
	 * operation so a given {@code state} is consumed at most once.  Of any two concurrent calls with the same
	 * {@code state}, <b>at most one</b> returns a present {@link Optional}; the other MUST miss &mdash;
	 * including across nodes sharing the store.  Use an atomic primitive against the primary (Redis
	 * {@code GETDEL} or a Lua {@code GET}+{@code DEL}), <b>not</b> a read-then-delete and <b>not</b> a
	 * possibly-stale replica read.  A consume that removes the entry but fails to return it (lost response)
	 * MUST be treated as a miss on retry &mdash; never a best-effort put-back (put-back reopens replay); the
	 * user simply restarts login.
	 *
	 * <p>
	 * <b>Bounded I/O.</b> Implementations MUST apply a bounded deadline to the backend round-trip so a hung
	 * backend cannot stall {@code completeLogin} on a request thread indefinitely.
	 *
	 * <p>
	 * <b>{@code empty()} vs. throw &mdash; distinct meanings.</b> Return {@link Optional#empty()}
	 * <b>only</b> for absent / already-consumed / (optionally) store-expired states &mdash; the caller
	 * treats {@code empty()} as a CSRF / replay / expiry rejection.  On an <b>infrastructure failure</b>
	 * (backend unreachable) the implementation MUST throw (not return {@code empty()}) so the caller can
	 * distinguish an attack from an outage; the RP catches it, logs without the payload, and rejects with an
	 * "unavailable" authentication error.  Either way the login fails closed; implementations MUST NEVER
	 * fabricate a {@link PendingLogin}.
	 *
	 * <p>
	 * <b>Expiry is not the store's sole responsibility.</b> An implementation MAY treat past-{@code expiresAt}
	 * entries as a miss (early eviction), but the RP re-checks {@code expiresAt} on the returned record
	 * regardless, so it is not a security defect if a store returns an over-age record &mdash; the framework
	 * rejects it.
	 *
	 * @param state The {@code state} value from the callback.  May be {@code null} (returns {@code empty()},
	 * 	matching the defensive callback path &mdash; do not throw).
	 * @return The association, or {@link Optional#empty()} if absent, expired, or already consumed.
	 */
	Optional<PendingLogin> consume(String state);
}
