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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.security.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mutable helper that folds a sequence of {@link AuthResult}s into a single result, honoring
 * {@link AuthResult.MergeMode}.
 *
 * <p>
 * {@link AuthResult.MergeMode#ADD ADD} keeps the first non-<jk>null</jk> principal and unions roles from
 * later results <b>only when</b> those results carry no principal of their own or the <b>same</b> principal
 * (compared by {@link Principal#getName()}, null-safe).  A later result with a <i>different</i> non-<jk>null</jk>
 * principal does not contribute its roles &mdash; a second authenticated identity must not silently elevate the
 * first.  {@link AuthResult.MergeMode#REPLACE REPLACE} resets the accumulated principal + roles.  Used by both
 * {@link AuthFilterChain} and the resource-level fold in {@link org.apache.juneau.rest.server.RestContext}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link AuthResult}
 * 	<li class='jc'>{@link AuthFilterChain}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerAuthenticator">REST Authenticator</a>
 * </ul>
 *
 * @since 10.0.0
 */
public final class AuthResultAccumulator {

	private static final Logger LOG = Logger.getLogger(AuthResultAccumulator.class.getName());

	private Principal principal;
	private final Set<String> roles = new LinkedHashSet<>();
	private boolean any;

	/**
	 * Folds the next result into this accumulator.
	 *
	 * @param r The result to fold in. <jk>null</jk> is ignored.
	 * @return This object.
	 */
	public AuthResultAccumulator add(AuthResult r) {
		if (r == null)
			return this;
		any = true;
		if (r.getMode() == AuthResult.MergeMode.REPLACE) {
			principal = r.getPrincipal();
			roles.clear();
			roles.addAll(r.getRoles());
			return this;
		}
		if (principal == null) {
			principal = r.getPrincipal();  // may still be null (roles-only)
			roles.addAll(r.getRoles());
			return this;
		}
		var otherPrincipal = r.getPrincipal();
		if (otherPrincipal == null || Objects.equals(principal.getName(), otherPrincipal.getName())) {
			roles.addAll(r.getRoles());
		} else {
			var establishedName = principal.getName();
			LOG.log(Level.WARNING, () -> "Ignoring roles from a distinct authenticated principal ('"
				+ otherPrincipal.getName() + "'); request identity remains '" + establishedName + "'.");
		}
		return this;
	}

	/**
	 * Returns the folded result.
	 *
	 * @return The folded result, or {@link Optional#empty()} if nothing was added or no principal was ever resolved.
	 */
	public Optional<AuthResult> result() {
		if (! any || principal == null)
			return oe();
		return o(AuthResult.of(principal, roles));
	}
}
