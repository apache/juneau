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
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * A parsed {@code WWW-Authenticate: Bearer ...} challenge as returned by an OAuth 2.1 / MCP resource server on a
 * {@code 401}/{@code 403} response (RFC 6750 &sect;3, RFC 9728 &sect;5.1).
 *
 * <p>
 * The MCP client uses this to recover the {@code resource_metadata} pointer to the RFC 9728 PRM document, the
 * per-operation {@code scope} on a step-up challenge, and the {@code error} code.  Auth-param values are matched
 * case-insensitively on the key per RFC 7235.
 *
 * <p>
 * {@link Serializable} (it is just a scheme string plus an immutable string map) so it can ride along a serialized
 * {@link McpInsufficientScopeException} without being dropped.
 *
 * @since 10.0.0
 */
public class WwwAuthenticateChallenge implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String scheme;
	private final Map<String,String> parameters;

	/**
	 * Constructor.
	 *
	 * @param scheme The auth scheme (e.g. {@code "Bearer"}).  Must not be <jk>null</jk> or blank.
	 * @param parameters The auth-params (lower-cased keys).  Must not be <jk>null</jk>.
	 */
	protected WwwAuthenticateChallenge(String scheme, Map<String,String> parameters) {
		this.scheme = assertArgNotNullOrBlank("scheme", scheme);
		this.parameters = u(cp(assertArgNotNull("parameters", parameters)));
	}

	/**
	 * Parses a single {@code WWW-Authenticate} header value.
	 *
	 * <p>
	 * Recognizes a single challenge of the form {@code <scheme> <key>=<value>, <key>="<value>", ...}.  Both quoted
	 * and bare token values are accepted; commas inside quoted values are preserved.
	 *
	 * @param headerValue The raw header value.  May be <jk>null</jk> or blank.
	 * @return The parsed challenge, or {@link Optional#empty()} if the value is null/blank or has no scheme token.
	 */
	public static Optional<WwwAuthenticateChallenge> parse(String headerValue) {
		if (headerValue == null)
			return oe();
		var s = headerValue.strip();
		if (s.isEmpty())
			return oe();
		var sp = s.indexOf(' ');
		if (sp < 0)
			return o(new WwwAuthenticateChallenge(s, Map.of()));
		var scheme = s.substring(0, sp);
		var rest = s.substring(sp + 1).strip();
		return o(new WwwAuthenticateChallenge(scheme, parseParams(rest)));
	}

	private static Map<String,String> parseParams(String s) {
		var out = new LinkedHashMap<String,String>();
		var i = 0;
		var n = s.length();
		while (i < n) {
			// Skip leading whitespace and separator commas.
			while (i < n && (Character.isWhitespace(s.charAt(i)) || s.charAt(i) == ','))
				i++;
			if (i >= n)
				break;
			var keyStart = i;
			while (i < n && s.charAt(i) != '=' && s.charAt(i) != ',')
				i++;
			if (i >= n || s.charAt(i) == ',')
				continue; // key with no value (e.g. bare token68); skip
			var key = s.substring(keyStart, i).strip().toLowerCase(Locale.ROOT);
			i++; // consume '='
			String value;
			if (i < n && s.charAt(i) == '"') {
				i++; // consume opening quote
				var sb = new StringBuilder();
				while (i < n && s.charAt(i) != '"') {
					if (s.charAt(i) == '\\' && i + 1 < n) // handle escaped chars inside quoted-string
						i++;
					sb.append(s.charAt(i));
					i++;
				}
				if (i < n)
					i++; // consume closing quote
				value = sb.toString();
			} else {
				var valStart = i;
				while (i < n && s.charAt(i) != ',')
					i++;
				value = s.substring(valStart, i).strip();
			}
			if (!key.isEmpty())
				out.putIfAbsent(key, value);
		}
		return out;
	}

	/**
	 * Returns the auth scheme.
	 *
	 * @return The scheme (e.g. {@code "Bearer"}).  Never {@code null}.
	 */
	public String scheme() {
		return scheme;
	}

	/**
	 * Returns whether the scheme is {@code Bearer} (case-insensitive).
	 *
	 * @return <jk>true</jk> if this is a Bearer challenge.
	 */
	public boolean isBearer() {
		return "bearer".equalsIgnoreCase(scheme);
	}

	/**
	 * Returns the parsed auth-params (lower-cased keys).
	 *
	 * @return An unmodifiable map.  Never {@code null}.
	 */
	public Map<String,String> parameters() {
		return parameters;
	}

	/**
	 * Returns the raw value of an auth-param.
	 *
	 * @param name The param name (matched case-insensitively).  Must not be <jk>null</jk>.
	 * @return The value, or {@link Optional#empty()} if absent.
	 */
	public Optional<String> parameter(String name) {
		assertArgNotNull("name", name);
		return o(parameters.get(name.toLowerCase(Locale.ROOT)));
	}

	/**
	 * Returns the {@code resource_metadata} pointer (RFC 9728 &sect;5.1) to the PRM document.
	 *
	 * @return The PRM document URI, or {@link Optional#empty()} if absent or unparseable as a URI.
	 */
	public Optional<URI> resourceMetadata() {
		var v = parameters.get("resource_metadata");
		if (v == null || v.isBlank())
			return oe();
		try {
			return o(URI.create(v));
		} catch (IllegalArgumentException e) { // HTT: Nimbus/servers emit valid absolute URIs; malformed value guarded defensively
			return oe();
		}
	}

	/**
	 * Returns the {@code error} code (e.g. {@code "insufficient_scope"}), if present.
	 *
	 * @return The error code, or {@link Optional#empty()}.
	 */
	public Optional<String> error() {
		return o(parameters.get("error"));
	}

	/**
	 * Returns the space-delimited {@code scope} param as an order-preserving set.
	 *
	 * @return The challenged scopes (empty if the param is absent or blank).  Never {@code null}.
	 */
	public Set<String> scopes() {
		var v = parameters.get("scope");
		if (v == null || v.isBlank())
			return Set.of();
		var out = new LinkedHashSet<String>();
		for (var tok : v.strip().split("\\s+"))
			if (!tok.isEmpty())
				out.add(tok);
		return u(out);
	}
}
