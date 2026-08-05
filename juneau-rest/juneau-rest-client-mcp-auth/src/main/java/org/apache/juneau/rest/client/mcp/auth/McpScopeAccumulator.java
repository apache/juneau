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
package org.apache.juneau.rest.client.mcp.auth;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Computes and tracks the SEP-2350 step-up scope union &mdash; the set of scopes a client re-authorizes with after a
 * server emits a per-operation {@code insufficient_scope} challenge.
 *
 * <p>
 * The <b>mandated minimum</b> union (SEP-2350 "Step-Up Authorization Flow" step 2) is
 * <code>{previously-requested} &cup; {challenge scopes}</code>, which preserves previously-granted permissions when
 * servers emit per-operation challenges.  {@link #union(Collection, Collection)} computes exactly that.  The broader
 * overload {@link #union(Collection, Collection, Collection, Collection, Collection)} additionally folds in the
 * <i>opt-in</i> contributors the spec permits a client to consult (granted-token scopes, cached PRM
 * {@code scopes_supported}, and client-metadata scope) &mdash; supplying them widens the union; omitting them keeps the
 * default at the mandated minimum.
 *
 * <p>
 * The union is <b>order-preserving</b> ({@link LinkedHashSet}) with <b>exact-string dedup only</b> &mdash; scopes are
 * opaque strings, so {@code repo} and {@code repo:read} are both retained (no hierarchy-aware dedup client-side;
 * hierarchy reasoning is the <i>server's</i> MUST).
 *
 * <p>
 * As an instance, this class also maintains per-<code>(resource, issuer)</code> accumulated state so a step-up driver
 * can carry the growing union across successive challenges for the same resource/AS.  Thread-safe.
 *
 * @since 10.0.0
 */
public class McpScopeAccumulator {

	private final ConcurrentMap<String,Set<String>> byKey = new ConcurrentHashMap<>();

	/**
	 * Computes the mandated-minimum step-up union: {@code previouslyRequested ∪ challengeScopes}, order-preserving with
	 * exact-string dedup.
	 *
	 * @param previouslyRequested The scope set the client previously requested (may be {@code null} / empty).
	 * @param challengeScopes The scopes from the server's {@code insufficient_scope} challenge (may be {@code null} /
	 * 	empty).
	 * @return A new order-preserving union set.  Never {@code null}.
	 */
	public static Set<String> union(Collection<String> previouslyRequested, Collection<String> challengeScopes) {
		return union(previouslyRequested, challengeScopes, null, null, null);
	}

	/**
	 * Computes the broadened step-up union, folding in the opt-in contributors the SEP-2350 Scope Selection Strategy
	 * permits a client to consult.  Order-preserving; exact-string dedup.  Any {@code null} contributor is treated as
	 * empty.
	 *
	 * @param previouslyRequested The scope set the client previously requested.
	 * @param challengeScopes The scopes from the server's {@code insufficient_scope} challenge.
	 * @param grantedTokenScopes The scopes on the currently-held token (opt-in).
	 * @param prmScopesSupported The cached PRM {@code scopes_supported} (opt-in).
	 * @param clientMetadataScope The scope declared in client metadata (opt-in).
	 * @return A new order-preserving union set.  Never {@code null}.
	 */
	public static Set<String> union(Collection<String> previouslyRequested, Collection<String> challengeScopes,
			Collection<String> grantedTokenScopes, Collection<String> prmScopesSupported, Collection<String> clientMetadataScope) {
		var out = new LinkedHashSet<String>();
		addAll(out, previouslyRequested);
		addAll(out, challengeScopes);
		addAll(out, grantedTokenScopes);
		addAll(out, prmScopesSupported);
		addAll(out, clientMetadataScope);
		return out;
	}

	/**
	 * Seeds the accumulated state for a {@code (resource, issuer)} key with the client's previously-requested scopes,
	 * <b>unioning</b> into any existing entry for that key rather than replacing it.
	 *
	 * <p>
	 * Unioning (not overwriting) matters when a shared accumulator is seeded more than once for the same key &mdash;
	 * e.g. two {@link McpStepUpAuthorizer.Builder#build() builds} against one shared accumulator, or a re-seed after some
	 * challenge scopes were already {@link #accumulate accumulated} &mdash; so no previously-tracked scope is ever lost.
	 *
	 * @param resource The RFC 8707 resource indicator.  Must not be <jk>null</jk>.
	 * @param issuer The authorization server issuer.  Must not be <jk>null</jk>.
	 * @param previouslyRequested The previously-requested scopes.  May be {@code null} / empty.
	 * @return This object.
	 */
	public McpScopeAccumulator seed(URI resource, URI issuer, Collection<String> previouslyRequested) {
		assertArgNotNull("resource", resource);
		assertArgNotNull("issuer", issuer);
		byKey.merge(key(resource, issuer), union(previouslyRequested, null), McpScopeAccumulator::union);
		return this;
	}

	/**
	 * Unions the given challenge scopes into the accumulated state for the {@code (resource, issuer)} key and returns
	 * the updated union.
	 *
	 * @param resource The RFC 8707 resource indicator.  Must not be <jk>null</jk>.
	 * @param issuer The authorization server issuer.  Must not be <jk>null</jk>.
	 * @param challengeScopes The scopes from the latest {@code insufficient_scope} challenge.  May be {@code null} /
	 * 	empty.
	 * @return A copy of the updated accumulated union for the key.  Never {@code null}.
	 */
	public Set<String> accumulate(URI resource, URI issuer, Collection<String> challengeScopes) {
		assertArgNotNull("resource", resource);
		assertArgNotNull("issuer", issuer);
		var updated = byKey.compute(key(resource, issuer), (k, existing) -> union(existing, challengeScopes));
		return new LinkedHashSet<>(updated);
	}

	/**
	 * Returns a copy of the currently-accumulated union for the {@code (resource, issuer)} key.
	 *
	 * @param resource The RFC 8707 resource indicator.  Must not be <jk>null</jk>.
	 * @param issuer The authorization server issuer.  Must not be <jk>null</jk>.
	 * @return A copy of the current accumulated union (empty if the key was never seeded/accumulated).
	 */
	public Set<String> current(URI resource, URI issuer) {
		assertArgNotNull("resource", resource);
		assertArgNotNull("issuer", issuer);
		return new LinkedHashSet<>(byKey.getOrDefault(key(resource, issuer), Set.of()));
	}

	private static void addAll(Set<String> out, Collection<String> in) {
		if (in != null)
			for (var s : in)
				if (s != null && ! s.isBlank())
					out.add(s);
	}

	private static String key(URI resource, URI issuer) {
		return resource.toString() + '\n' + issuer.toString();
	}
}
