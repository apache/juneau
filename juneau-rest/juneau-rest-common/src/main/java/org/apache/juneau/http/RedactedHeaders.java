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
package org.apache.juneau.http;

import java.util.*;

/**
 * Utility for masking the values of header names that commonly carry account credentials
 * (bearer tokens, session cookies, API keys, and the like) before they are written somewhere
 * observable, such as a log line or an echoed-back response body.
 *
 * <p>
 * Matching is always case-insensitive, mirroring HTTP header-name semantics.
 *
 * <p>
 * Callers that need a different or extended header set (for example, an additional
 * internal-trace header) can pass their own name collection to the two-argument overloads
 * rather than being limited to {@link #DEFAULT}.
 */
public final class RedactedHeaders {

	/** Sentinel value substituted for a masked header value. */
	public static final String REDACTED = "[REDACTED]";

	/** Canonical set of header names commonly used to carry credentials, matched case-insensitively. */
	public static final Set<String> DEFAULT = Set.of(
		"Authorization", "Cookie", "Set-Cookie", "Proxy-Authorization", "X-API-Key");

	private RedactedHeaders() {}

	/**
	 * Returns {@code true} if the given header name is in {@link #DEFAULT} (case-insensitive).
	 *
	 * @param name The header name to test. Can be {@code null} (returns {@code false}).
	 * @return {@code true} if the name matches one of {@link #DEFAULT}, case-insensitively.
	 */
	public static boolean isSensitive(String name) {
		return isSensitive(name, DEFAULT);
	}

	/**
	 * Returns {@code true} if the given header name matches one of {@code names}, case-insensitively.
	 *
	 * @param name The header name to test. Can be {@code null} (returns {@code false}).
	 * @param names The candidate header names. Can be {@code null} or empty (returns {@code false}).
	 * 	{@code null} elements are ignored.
	 * @return {@code true} if {@code name} matches one of {@code names}, case-insensitively.
	 */
	public static boolean isSensitive(String name, Collection<String> names) {
		if (name == null || names == null)
			return false;
		for (var n : names)
			if (n != null && name.equalsIgnoreCase(n))
				return true;
		return false;
	}

	/**
	 * Masks {@code value} with {@link #REDACTED} if {@code name} is in {@link #DEFAULT}
	 * (case-insensitive); otherwise returns {@code value} unchanged.
	 *
	 * @param name The header name. Can be {@code null} (never matches; {@code value} is returned as-is).
	 * @param value The header value to mask. Can be {@code null}.
	 * @return {@link #REDACTED} if {@code name} is sensitive, else {@code value}.
	 */
	public static String redact(String name, String value) {
		return isSensitive(name) ? REDACTED : value;
	}

	/**
	 * Masks {@code value} with {@link #REDACTED} if {@code name} matches one of {@code names}
	 * (case-insensitive); otherwise returns {@code value} unchanged.
	 *
	 * @param name The header name. Can be {@code null} (never matches; {@code value} is returned as-is).
	 * @param value The header value to mask. Can be {@code null}.
	 * @param names The candidate header names. Can be {@code null} or empty (nothing is masked).
	 * @return {@link #REDACTED} if {@code name} matches one of {@code names}, else {@code value}.
	 */
	public static String redact(String name, String value, Collection<String> names) {
		return isSensitive(name, names) ? REDACTED : value;
	}

	/**
	 * Default query parameter names whose values are replaced with {@link #REDACTED} when a query string is logged.
	 *
	 * <p>
	 * Names are matched case-insensitively.
	 */
	public static final Set<String> DEFAULT_QUERY_PARAMS = Set.of(
		"password", "passwd", "secret", "token", "access_token", "api_key", "apikey", "key", "authorization", "auth",
		"session", "sessionid", "code", "client_secret");

	/**
	 * Returns the specified query string with the values of the named parameters replaced with {@link #REDACTED}.
	 *
	 * <p>
	 * Parameter names are URL-decoded and matched case-insensitively.  Parameters that have no value are left as-is.
	 *
	 * @param queryString The raw query string (without the leading <js>'?'</js>).  Can be <jk>null</jk>.
	 * @param names The parameter names to redact.  Can be <jk>null</jk> (nothing is redacted).
	 * @return The redacted query string, or <jk>null</jk> if the input was <jk>null</jk>.
	 */
	public static String redactQueryString(String queryString, Collection<String> names) {
		if (queryString == null || names == null || names.isEmpty())
			return queryString;
		var sb = new StringBuilder(queryString.length());
		var parts = queryString.split("&", -1);
		for (var pi = 0; pi < parts.length; pi++) {
			var part = parts[pi];
			if (pi > 0)
				sb.append('&');
			var i = part.indexOf('=');
			if (i == -1) {
				sb.append(part);
				continue;
			}
			var n = part.substring(0, i);
			String decoded;
			try {
				decoded = java.net.URLDecoder.decode(n, java.nio.charset.StandardCharsets.UTF_8);
			} catch (@SuppressWarnings("unused") IllegalArgumentException e) {
				decoded = n;
			}
			sb.append(n).append('=').append(isSensitive(decoded.trim(), names) ? REDACTED : part.substring(i + 1));
		}
		return sb.toString();
	}
}
