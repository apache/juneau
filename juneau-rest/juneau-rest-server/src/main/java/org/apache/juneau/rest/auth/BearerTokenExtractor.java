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

import static org.apache.juneau.commons.utils.Utils.*;
import java.util.*;

/**
 * Package-private utility for RFC 6750 Bearer-token header parsing.
 *
 * <p>
 * Shared by {@link BearerTokenGuard} and {@link BearerTokenAuthFilter} so the
 * {@code "Bearer "} prefix check and token extraction are implemented once.
 */
class BearerTokenExtractor {

	/** Authorization header name. */
	static final String AUTHORIZATION = "Authorization";

	private static final String BEARER_PREFIX = "Bearer ";

	/**
	 * Extracts the raw Bearer token from an {@code Authorization} header value.
	 *
	 * <p>
	 * The caller is responsible for reading the header and handling the absent-header case
	 * (so that guard implementations can distinguish "missing header" from "malformed header"
	 * when producing different error messages).
	 *
	 * @param header The Authorization header value. Must not be blank.
	 * @return The token string, or {@link Optional#empty()} if the header is not the Bearer scheme
	 *   or the token portion is blank.
	 */
	static Optional<String> extract(String header) {
		if (header.length() <= BEARER_PREFIX.length()
				|| !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length()))
			return opte();
		var token = header.substring(BEARER_PREFIX.length()).trim();
		return token.isEmpty() ? opte() : opt(token);
	}

	private BearerTokenExtractor() {}
}
