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

import static org.apache.juneau.commons.utils.StringUtils.*;

import jakarta.servlet.http.*;

/**
 * Package-private utility for reading API keys from HTTP requests.
 *
 * <p>
 * Shared by {@link ApiKeyGuard} and {@link ApiKeyAuthFilter} so the cookie-parsing logic
 * (including the mock-container fallback) is implemented once.
 */
class ApiKeyExtractor {

	/**
	 * Reads an API key from a cookie by name.
	 *
	 * <p>
	 * First checks {@link HttpServletRequest#getCookies()} (the standard path used by real servlet
	 * containers), then falls back to parsing the raw {@code Cookie} header.  The fallback is needed
	 * for mock containers (e.g. {@code juneau-rest-mock}) that do not automatically parse the
	 * {@code Cookie} header into cookie objects — they only return cookies that were explicitly set
	 * via their own API.
	 *
	 * @param req The HTTP request.
	 * @param cookieName The cookie name to look up.
	 * @return The cookie value, or {@code null} if not found.
	 */
	static String readCookie(HttpServletRequest req, String cookieName) {
		var cookies = req.getCookies();
		if (cookies != null) {
			for (var c : cookies)
				if (cookieName.equals(c.getName()))
					return c.getValue();
		}
		var raw = req.getHeader("Cookie");
		if (isBlank(raw))
			return null;
		for (var pair : raw.split(";")) {
			var eq = pair.indexOf('=');
			if (eq < 0)
				continue;
			var k = pair.substring(0, eq).trim();
			if (cookieName.equals(k))
				return pair.substring(eq + 1).trim();
		}
		return null;
	}

	private ApiKeyExtractor() {}
}
