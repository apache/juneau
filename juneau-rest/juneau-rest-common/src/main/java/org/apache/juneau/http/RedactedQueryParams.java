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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

/**
 * Utility for masking the values of query-string parameters that commonly carry secrets (passwords, tokens, API
 * keys) before a URI is written somewhere observable, such as a log line.
 *
 * <p>
 * Parameter-name matching is case-insensitive.  Only the value is masked; the parameter name and the rest of the URI
 * are preserved.
 *
 * <p class='bjava'>
 * 	String <jv>masked</jv> = RedactedQueryParams.<jsm>redactUri</jsm>(<js>"/a?user=bob&amp;token=abc"</js>);
 * 	<jc>// masked == "/a?user=bob&amp;token=[REDACTED]"</jc>
 * </p>
 *
 * @since 10.0.0
 */
public final class RedactedQueryParams {

	/** Canonical set of query parameter names commonly used to carry secrets, matched case-insensitively. */
	public static final Set<String> DEFAULT = Set.of(
		"password", "passwd", "secret", "token", "access_token", "api_key", "apikey", "key", "authorization", "auth",
		"session", "sessionid", "code", "client_secret");

	private RedactedQueryParams() {}

	/**
	 * Returns {@code true} if the given query parameter name is in {@link #DEFAULT} (case-insensitive).
	 *
	 * @param name The parameter name to test. Can be {@code null} (returns {@code false}).
	 * @return {@code true} if the name is sensitive.
	 */
	public static boolean isSensitive(String name) {
		return isSensitive(name, DEFAULT);
	}

	/**
	 * Returns {@code true} if the given query parameter name matches one of {@code names}, case-insensitively.
	 *
	 * @param name The parameter name to test. Can be {@code null} (returns {@code false}).
	 * @param names The candidate names. Can be {@code null} (returns {@code false}).  {@code null} elements are ignored.
	 * @return {@code true} if the name is sensitive.
	 */
	public static boolean isSensitive(String name, Collection<String> names) {
		if (name == null || names == null)
			return false;
		for (var n : names)
			if (n != null && eqic(name, n))
				return true;
		return false;
	}

	/**
	 * Masks the values of sensitive query parameters in a URI using {@link #DEFAULT}.
	 *
	 * @param uri The URI or URI reference. Can be {@code null} (returned as-is).
	 * @return The URI with sensitive parameter values replaced by {@link RedactedHeaders#REDACTED}.
	 */
	public static String redactUri(String uri) {
		return redactUri(uri, DEFAULT);
	}

	/**
	 * Masks the values of query parameters whose names match {@code names} in a URI.
	 *
	 * <p>
	 * The parameter name is compared after percent-decoding.  A fragment, if present, is left untouched.
	 *
	 * @param uri The URI or URI reference. Can be {@code null} (returned as-is).
	 * @param names The sensitive parameter names. Can be {@code null} (nothing is masked).
	 * @return The URI with matching parameter values replaced by {@link RedactedHeaders#REDACTED}.
	 */
	public static String redactUri(String uri, Collection<String> names) {
		if (uri == null || names == null)
			return uri;
		var q = uri.indexOf('?');
		if (q < 0)
			return uri;
		var hash = uri.indexOf('#', q);
		var end = hash < 0 ? uri.length() : hash;
		var sb = new StringBuilder(uri.length()).append(uri, 0, q + 1);
		var pairs = uri.substring(q + 1, end).split("&", -1);
		for (var i = 0; i < pairs.length; i++) {
			if (i > 0)
				sb.append('&');
			var pair = pairs[i];
			var eq = pair.indexOf('=');
			if (eq < 0) {
				sb.append(pair);
				continue;
			}
			var name = pair.substring(0, eq);
			sb.append(name).append('=').append(isSensitive(decode(name), names) ? RedactedHeaders.REDACTED : pair.substring(eq + 1));
		}
		return sb.append(uri, end, uri.length()).toString();
	}

	private static String decode(String s) {
		try {
			return java.net.URLDecoder.decode(s, java.nio.charset.StandardCharsets.UTF_8);
		} catch (@SuppressWarnings("unused") IllegalArgumentException e) {
			return s;
		}
	}
}
