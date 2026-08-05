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

import java.security.*;
import java.util.*;

import org.apache.juneau.rest.server.auth.ClaimsPrincipal;

/**
 * <a class="doclink" href="https://datatracker.ietf.org/doc/html/rfc8707">RFC 8707</a> audience enforcement &mdash; the
 * confused-deputy defense pairing with the MCP client's resource indicator.
 *
 * <p>
 * Confirms that a validated access token was actually minted for <i>this</i> resource by checking that the token's
 * {@code aud} or {@code resource} claim contains this server's resource identifier, rejecting a token whose audience
 * targets a different resource.
 *
 * <h5 class='section'>Claims requirement (READY-312f F2, H2):</h5>
 * <p>
 * Gate-level enforcement applies to a {@link ClaimsPrincipal} (as produced by {@code JwtTokenValidator} and OAuth
 * introspection).  A validator returning a bare {@link Principal} that exposes no claims cannot be checked at the gate;
 * the {@code requireAudienceClaim} flag decides the disposition:
 * <ul>
 * 	<li><jk>true</jk> (the default) &mdash; <b>fail-closed</b>: a claims-less principal is rejected, so the
 * 		confused-deputy defense is never silently a no-op.
 * 	<li><jk>false</jk> &mdash; <b>opt-out</b>: a claims-less principal passes, delegating audience enforcement to the
 * 		validator itself (e.g. {@code JwtTokenValidator} enforcing {@code aud} internally, or an introspection AS that
 * 		legitimately omits {@code aud}).
 * </ul>
 *
 * @since 10.0.0
 */
public final class McpAudienceValidator {

	private McpAudienceValidator() {}

	/**
	 * Returns whether the principal's token audience matches the expected resource identifier.
	 *
	 * @param principal The authenticated principal.  May be <jk>null</jk> (returns <jk>false</jk>).
	 * @param expectedAudience The expected audience (this server's resource identifier).  Must not be <jk>null</jk> or
	 * 	blank.
	 * @param requireAudienceClaim Whether a principal exposing no claims is rejected (fail-closed) rather than passed
	 * 	(see {@link McpResourceServerConfig#isRequireAudienceClaim()}).
	 * @return <jk>true</jk> if the audience matches, or the principal exposes no claims and {@code requireAudienceClaim}
	 * 	is <jk>false</jk>; <jk>false</jk> if the principal is <jk>null</jk>, exposes no claims while
	 * 	{@code requireAudienceClaim} is <jk>true</jk>, or carries claims whose {@code aud}/{@code resource} do not include
	 * 	the expected audience.
	 */
	public static boolean matches(Principal principal, String expectedAudience, boolean requireAudienceClaim) {
		assertArgNotNullOrBlank("expectedAudience", expectedAudience);
		if (principal == null)
			return false;
		if (!(principal instanceof ClaimsPrincipal cp))
			return !requireAudienceClaim;
		return audienceValues(cp).contains(expectedAudience);
	}

	private static Set<String> audienceValues(ClaimsPrincipal cp) {
		var out = new HashSet<String>();
		addClaim(out, cp.getClaims().get("aud"));
		addClaim(out, cp.getClaims().get("resource"));
		return out;
	}

	private static void addClaim(Set<String> out, Object v) {
		if (v == null)
			return;
		if (v instanceof Collection<?> c) {
			for (var o : c)
				if (o != null)
					out.add(o.toString());
		} else {
			out.add(v.toString());
		}
	}
}
