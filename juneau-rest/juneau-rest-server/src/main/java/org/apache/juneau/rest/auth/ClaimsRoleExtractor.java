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
package org.apache.juneau.rest.auth;

import java.security.*;
import java.util.*;

/**
 * Package-private utility for extracting role names from a {@link ClaimsPrincipal} claim.
 *
 * <p>
 * Shared by {@link BearerTokenAuthFilter} and {@link ApiKeyAuthFilter} so the
 * {@code ClaimsPrincipal} list-of-strings role extraction is implemented once.
 */
class ClaimsRoleExtractor {

	/**
	 * Extracts role names from a named claim on a {@link ClaimsPrincipal}.
	 *
	 * <p>
	 * Returns an empty set when the principal is not a {@link ClaimsPrincipal}, the named claim
	 * is absent, or the claim value is not a {@link List} of {@link String}.
	 *
	 * @param principal The authenticated principal.
	 * @param claimName The claim name to read (e.g. {@code "roles"}).
	 * @return The extracted role names.  Never {@code null}; may be empty.
	 */
	@SuppressWarnings("unchecked")
	static Set<String> extractRoles(Principal principal, String claimName) {
		if (principal instanceof ClaimsPrincipal cp) {
			var v = cp.getClaims().get(claimName);
			if (v instanceof List<?> list) {
				var roles = new HashSet<String>();
				for (var item : (List<Object>) list)
					if (item instanceof String s)
						roles.add(s);
				return roles;
			}
		}
		return Collections.emptySet();
	}

	private ClaimsRoleExtractor() {}
}
