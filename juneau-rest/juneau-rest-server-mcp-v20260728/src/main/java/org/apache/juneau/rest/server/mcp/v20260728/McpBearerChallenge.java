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

import java.net.*;
import java.util.*;

/**
 * Fluent, insertion-ordered builder for a {@code WWW-Authenticate: Bearer ...} challenge value (RFC 6750 &sect;3, RFC
 * 9728 &sect;5.1) emitted by the {@code 2026-07-28} MCP resource server on a {@code 401}.
 *
 * <p>
 * Produces {@code Bearer <k>="<v>", ...} auth-params in the order added, so the emitted header parses cleanly on the
 * client side via {@code org.apache.juneau.rest.client.mcp.auth.WwwAuthenticateChallenge} (which lower-cases keys and
 * accepts quoted values).  Auth-param values are sanitized for the RFC 7235 quoted-string grammar: control characters
 * ({@code < 0x20} and {@code 0x7F}, including CR/LF that would otherwise enable header injection) are stripped, and
 * {@code "}/{@code \} are backslash-escaped ({@code \"} / {@code \\}) rather than mangled.
 *
 * <h5 class='section'>F3 extensibility:</h5>
 * <p>
 * The baseline emits {@link #realm(String) realm}, {@link #error(String) error},
 * {@link #errorDescription(String) error_description}, {@link #scope(Collection) scope}, and
 * {@link #resourceMetadata(URI) resource_metadata}.  The generic {@link #param(String, String)} seam lets F3
 * (SEP-2350 scoped step-up) add further auth-params without touching this class &mdash; F2 deliberately does not
 * implement scope-accumulation/step-up.
 *
 * @since 10.0.0
 */
public final class McpBearerChallenge {

	private final Map<String,String> params = new LinkedHashMap<>();

	/**
	 * Static creator.
	 *
	 * @return A new, empty challenge builder.
	 */
	public static McpBearerChallenge create() {
		return new McpBearerChallenge();
	}

	private McpBearerChallenge() {}

	/**
	 * Adds the {@code realm} auth-param.
	 *
	 * @param value The realm.  A <jk>null</jk> value is ignored.
	 * @return This object (for method chaining).
	 */
	public McpBearerChallenge realm(String value) {
		return param("realm", value);
	}

	/**
	 * Adds the {@code error} auth-param (e.g. {@code invalid_token}, {@code insufficient_scope}).
	 *
	 * @param value The error code.  A <jk>null</jk> value is ignored.
	 * @return This object (for method chaining).
	 */
	public McpBearerChallenge error(String value) {
		return param("error", value);
	}

	/**
	 * Adds the {@code error_description} auth-param.
	 *
	 * @param value The human-readable error description.  A <jk>null</jk> value is ignored.
	 * @return This object (for method chaining).
	 */
	public McpBearerChallenge errorDescription(String value) {
		return param("error_description", value);
	}

	/**
	 * Adds the space-delimited {@code scope} auth-param.
	 *
	 * @param scopes The scopes.  A <jk>null</jk> or empty collection is ignored.
	 * @return This object (for method chaining).
	 */
	public McpBearerChallenge scope(Collection<String> scopes) {
		if (scopes == null || scopes.isEmpty())
			return this;
		return param("scope", String.join(" ", scopes));
	}

	/**
	 * Adds the {@code resource_metadata} auth-param (RFC 9728 &sect;5.1) pointing at the PRM document.
	 *
	 * @param value The PRM document URI.  A <jk>null</jk> value is ignored.
	 * @return This object (for method chaining).
	 */
	public McpBearerChallenge resourceMetadata(URI value) {
		return param("resource_metadata", value == null ? null : value.toString());
	}

	/**
	 * Adds an arbitrary auth-param &mdash; the extensibility seam for future challenge parameters.
	 *
	 * @param name The auth-param name.  Must not be <jk>null</jk> or blank.
	 * @param value The auth-param value.  A <jk>null</jk> value is ignored (the param is not added).
	 * @return This object (for method chaining).
	 */
	public McpBearerChallenge param(String name, String value) {
		assertArgNotNullOrBlank("name", name);
		if (value != null)
			params.put(name, sanitize(value));
		return this;
	}

	/**
	 * Builds the {@code WWW-Authenticate} challenge value.
	 *
	 * @return The header value (e.g. {@code Bearer realm="mcp", resource_metadata="https://host/.well-known/..."}), or
	 * 	the bare {@code Bearer} scheme when no auth-params were added.
	 */
	public String build() {
		if (params.isEmpty())
			return "Bearer";
		var sb = new StringBuilder("Bearer ");
		var first = true;
		for (var e : params.entrySet()) {
			if (!first)
				sb.append(", ");
			sb.append(e.getKey()).append("=\"").append(e.getValue()).append('"');
			first = false;
		}
		return sb.toString();
	}

	@Override /* Overridden from Object */
	public String toString() {
		return build();
	}

	private static String sanitize(String v) {
		var sb = new StringBuilder(v.length());
		for (var i = 0; i < v.length(); i++) {
			var c = v.charAt(i);
			if (c < 0x20 || c == 0x7F)  // Strip all CTLs (incl. CR/LF) that would corrupt the response header.
				continue;
			if (c == '"' || c == '\\')  // RFC 7235 quoted-string: escape rather than mangle.
				sb.append('\\');
			sb.append(c);
		}
		return sb.toString();
	}
}
