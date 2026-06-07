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
package org.apache.juneau.rest.server.auth;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.security.*;
import java.util.*;

/**
 * A {@link Principal} that exposes typed read-only access to a map of claims.
 *
 * <p>
 * Used by token validators that produce structured assertions about the authenticated subject &mdash;
 * e.g. JWTs (the {@code juneau-rest-server-auth-jwt} {@code JwtTokenValidator} returns a
 * {@code ClaimsPrincipal} with the verified token's claim set) or OAuth 2.0 introspection responses.
 *
 * <p>
 * The "principal name" returned by {@link #getName()} is supplied at construction time; callers can
 * pick whichever claim is canonical for their identity model (typically {@code sub}). The full claim
 * set is reachable via {@link #getClaims()} and per-claim typed lookup via
 * {@link #getClaim(String, Class)}.
 *
 * <p>
 * Instances are <b>immutable</b> &mdash; the claims map supplied to the constructor is defensively
 * copied and exposed as an unmodifiable view. {@code ClaimsPrincipal} can be safely cached and
 * shared across requests.
 *
 * <h5 class='section'>Type coercion contract for {@link #getClaim(String, Class)}:</h5>
 *
 * <ul>
 * 	<li>If the underlying value is already an instance of the requested type, it is returned directly.
 * 	<li>If the requested type is {@link String}, the value's {@link Object#toString()} is returned.
 * 	<li>{@link Number} sub-types ({@link Integer}, {@link Long}, {@link Double}) coerce via the
 * 		matching {@code Number.xxxValue()} accessor; non-numeric values yield {@link Optional#empty()}.
 * 	<li>{@link Boolean} coerces from {@link Boolean} only.
 * 	<li>Any other mismatch yields {@link Optional#empty()} rather than a cast exception.
 * </ul>
 *
 * <p>
 * The coercion table is intentionally conservative &mdash; the validator that built the claims map is
 * the authoritative source of richer type-mapping. If a JWT validator decodes a claim as a
 * {@code java.util.List<String>}, callers can read it back as {@code List.class} or pull individual
 * elements via {@link #getClaims()} and operate on the {@link List} directly.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link Auth}
 * 	<li class='jc'>{@link TokenValidator}
 * 	<li class='link'><a class="doclink" href="https://datatracker.ietf.org/doc/html/rfc7519#section-4">RFC 7519 &sect;4 &mdash; JWT Claims</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/AuthGuards">AuthN Guards</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class ClaimsPrincipal implements Principal {

	private final String name;
	private final Map<String,Object> claims;

	/**
	 * Constructor.
	 *
	 * @param name The principal name. Must not be <jk>null</jk> or blank.
	 * @param claims The claim map. May be <jk>null</jk> (treated as an empty map). Defensively copied.
	 */
	public ClaimsPrincipal(String name, Map<String,Object> claims) {
		assertArgNotNullOrBlank("name", name);
		this.name = name;
		this.claims = claims == null
			? Collections.emptyMap()
			: Collections.unmodifiableMap(new LinkedHashMap<>(claims));
	}

	@Override /* Overridden from Principal */
	public String getName() {
		return name;
	}

	/**
	 * Returns an unmodifiable view of the full claim set.
	 *
	 * @return The full claim map. Never <jk>null</jk>.
	 */
	public Map<String,Object> getClaims() {
		return claims;
	}

	/**
	 * Returns the value of a claim by name with the supplied target type.
	 *
	 * <p>
	 * See the class-level Javadoc for the coercion contract.
	 *
	 * @param <T> The target type.
	 * @param claimName The claim name. Must not be <jk>null</jk>.
	 * @param type The target type. Must not be <jk>null</jk>.
	 * @return The coerced value, or {@link Optional#empty()} if the claim is absent or cannot be coerced.
	 */
	public <T> Optional<T> getClaim(String claimName, Class<T> type) {
		assertArgNotNull("claimName", claimName);
		assertArgNotNull("type", type);
		var v = claims.get(claimName);
		if (v == null)
			return opte();
		return opt(coerce(v, type));
	}

	/**
	 * Returns <jk>true</jk> if a claim with the supplied name exists in the claim set.
	 *
	 * @param claimName The claim name. Must not be <jk>null</jk>.
	 * @return <jk>true</jk> if the claim is present, even if its value is the JSON {@code null} literal.
	 */
	public boolean hasClaim(String claimName) {
		assertArgNotNull("claimName", claimName);
		return claims.containsKey(claimName);
	}

	@SuppressWarnings({
		"unchecked" // Cast is safe: type parameter is verified by isInstance check before casting.
	})
	private static <T> T coerce(Object v, Class<T> type) {
		if (type.isInstance(v))
			return (T) v;
		if (type == String.class)
			return (T) v.toString();
		if (v instanceof Number n) {
			if (type == Integer.class || type == int.class)
				return (T) Integer.valueOf(n.intValue());
			if (type == Long.class || type == long.class)
				return (T) Long.valueOf(n.longValue());
			if (type == Double.class || type == double.class)
				return (T) Double.valueOf(n.doubleValue());
			if (type == Float.class || type == float.class)
				return (T) Float.valueOf(n.floatValue());
		}
		return null;
	}

	@Override /* Overridden from Object */
	public String toString() {
		return "ClaimsPrincipal[name=" + name + ", claims=" + claims.keySet() + "]";
	}
}
