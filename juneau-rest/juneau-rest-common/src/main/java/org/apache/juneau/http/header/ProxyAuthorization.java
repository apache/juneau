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
package org.apache.juneau.http.header;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.nio.charset.*;
import java.util.*;
import java.util.function.*;

/**
 * Represents an HTTP <c>Proxy-Authorization</c> header.
 *
 * <p>
 * Authorization credentials for connecting to a proxy.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.marshall.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/NextGenRestClient">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"java:S1192" // Duplicated "value" literals are HTTP header component keys; a constant would obscure the header grammar.
})
public class ProxyAuthorization extends HttpStringHeader {

	/** The header name */
	public static final String NAME = "Proxy-Authorization";

	/**
	 * Constructor with an eager value.
	 *
	 * @param value The header value. May be <jk>null</jk>.
	 */
	public ProxyAuthorization(String value) {
		super(NAME, value);
	}

	/**
	 * Constructor with a lazy value supplier.
	 *
	 * @param valueSupplier Supplier for the header value. Must not be <jk>null</jk>.
	 */
	public ProxyAuthorization(Supplier<String> valueSupplier) {
		super(NAME, valueSupplier);
	}

	/**
	 * Static factory method with an eager value.
	 *
	 * @param value The header value. May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static ProxyAuthorization of(String value) {
		return new ProxyAuthorization(value);
	}

	/**
	 * Static factory method with a lazy value supplier.
	 *
	 * @param valueSupplier Supplier for the header value. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static ProxyAuthorization of(Supplier<String> valueSupplier) {
		return new ProxyAuthorization(valueSupplier);
	}

	/**
	 * Creates a new empty {@link Builder}.
	 *
	 * @return A new builder. Never <jk>null</jk>.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Creates a {@link Builder} pre-populated with <c>Basic</c> credentials (base64 of <c>user:password</c>).
	 *
	 * @param user The username. Must not be <jk>null</jk>.
	 * @param password The password. Must not be <jk>null</jk>.
	 * @return A new builder. Never <jk>null</jk>.
	 */
	public static Builder basic(String user, String password) {
		return create().basic(user, password);
	}

	/**
	 * Creates a {@link Builder} pre-populated with <c>Bearer</c> credentials.
	 *
	 * @param token The bearer token. Must not be <jk>null</jk>.
	 * @return A new builder. Never <jk>null</jk>.
	 */
	public static Builder bearer(String token) {
		return create().bearer(token);
	}

	/**
	 * Creates a {@link Builder} pre-populated with a <c>Digest</c> scheme.
	 *
	 * @return A new builder. Never <jk>null</jk>.
	 */
	public static Builder digest() {
		return create().digest();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Inner types
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Fluent builder for assembling an HTTP <c>Proxy-Authorization</c> credentials value.
	 *
	 * <p>
	 * Composes the <c>scheme 1*SP ( token68 / #auth-param )</c> credentials form defined by
	 * <a class='doclink' href='https://www.rfc-editor.org/rfc/rfc7235#section-4.4'>RFC 7235 §4.4</a>.  The
	 * {@link #basic(String, String)} and {@link #bearer(String)} presets render the opaque <c>token68</c> form; the
	 * {@link #digest()} preset enables the typed Digest auth-params, which apply the correct quoting (quoted-string vs
	 * token).  {@link #param(String, String)} is a generic escape hatch so new params never require an API change.
	 * Setting a scheme preset or {@link #token(String)} clears any previously-registered auth-params and vice versa, so
	 * a built value carries either a token68 or an auth-param list, never both.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	String <jv>value</jv> = ProxyAuthorization.<jsm>create</jsm>()
	 * 		.digest()
	 * 		.username(<js>"Mufasa"</js>)
	 * 		.realm(<js>"http-auth@example.org"</js>)
	 * 		.uri(<js>"/dir/index.html"</js>)
	 * 		.qop(<js>"auth"</js>)
	 * 		.nc(<js>"00000001"</js>)
	 * 		.response(<js>"8ca523f5e9506fed4657c9700eebdbec"</js>)
	 * 		.build();
	 * 	<jc>// =&gt; "Digest username=\"Mufasa\", realm=\"...\", uri=\"/dir/index.html\", qop=auth, nc=00000001, response=\"...\""</jc>
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jc'>{@link ProxyAuthorization}
	 * 	<li class='extlink'><a class='doclink' href='https://www.rfc-editor.org/rfc/rfc7235'>RFC 7235 - HTTP/1.1 Authentication</a>
	 * </ul>
	 *
	 * @since 10.0.0
	 */
	public static class Builder {

		private String scheme;
		private String token68;
		private final Map<String,String> params = new LinkedHashMap<>();

		/**
		 * Sets the authentication scheme (e.g. <js>"Basic"</js>, <js>"Bearer"</js>, <js>"Digest"</js>).
		 *
		 * @param value The scheme name. Must not be <jk>null</jk> or blank.
		 * @return This object.
		 * @throws IllegalArgumentException If {@code value} is <jk>null</jk> or blank.
		 */
		public Builder scheme(String value) {
			assertArgNotNull("value", value);
			var v = value.trim();
			if (v.isEmpty())
				throw iaex("scheme must not be blank");
			scheme = v;
			return this;
		}

		/**
		 * Sets <c>Basic</c> credentials by base64-encoding <c>user:password</c> as the token68 form.
		 *
		 * @param user The username. Must not be <jk>null</jk>.
		 * @param password The password. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder basic(String user, String password) {
			assertArgNotNull("user", user);
			assertArgNotNull("password", password);
			var encoded = Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
			return scheme("Basic").token(encoded);
		}

		/**
		 * Sets <c>Bearer</c> credentials with the supplied token as the token68 form.
		 *
		 * @param token The bearer token. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder bearer(String token) {
			return scheme("Bearer").token(token);
		}

		/**
		 * Sets the scheme to <c>Digest</c> (typed Digest auth-params can then be chained).
		 *
		 * @return This object.
		 */
		public Builder digest() {
			return scheme("Digest");
		}

		/**
		 * Sets the opaque <c>token68</c> credentials form, clearing any auth-params.
		 *
		 * @param value The token68 value. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder token(String value) {
			assertArgNotNull("value", value);
			params.clear();
			token68 = value;
			return this;
		}

		/**
		 * Sets the <c>username</c> auth-param (rendered as a quoted-string).
		 *
		 * @param value The username. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder username(String value) {
			return quotedParam("username", value);
		}

		/**
		 * Sets the <c>realm</c> auth-param (rendered as a quoted-string).
		 *
		 * @param value The realm. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder realm(String value) {
			return quotedParam("realm", value);
		}

		/**
		 * Sets the <c>nonce</c> auth-param (rendered as a quoted-string).
		 *
		 * @param value The server nonce echoed back from the challenge. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder nonce(String value) {
			return quotedParam("nonce", value);
		}

		/**
		 * Sets the <c>uri</c> auth-param (rendered as a quoted-string).
		 *
		 * @param value The effective request URI. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder uri(String value) {
			return quotedParam("uri", value);
		}

		/**
		 * Sets the <c>response</c> auth-param (rendered as a quoted-string).
		 *
		 * @param value The computed digest response. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder response(String value) {
			return quotedParam("response", value);
		}

		/**
		 * Sets the <c>cnonce</c> auth-param (rendered as a quoted-string).
		 *
		 * @param value The client nonce. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder cnonce(String value) {
			return quotedParam("cnonce", value);
		}

		/**
		 * Sets the <c>opaque</c> auth-param (rendered as a quoted-string).
		 *
		 * @param value The opaque value echoed back from the challenge. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder opaque(String value) {
			return quotedParam("opaque", value);
		}

		/**
		 * Sets the <c>algorithm</c> auth-param (rendered as a bare token, unquoted).
		 *
		 * @param value The algorithm (e.g. <js>"MD5"</js>, <js>"SHA-256"</js>). Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder algorithm(String value) {
			return tokenParam("algorithm", value);
		}

		/**
		 * Sets the <c>qop</c> auth-param (rendered as a bare token, unquoted; a single value in credentials).
		 *
		 * @param value The quality-of-protection token (e.g. <js>"auth"</js>). Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder qop(String value) {
			return tokenParam("qop", value);
		}

		/**
		 * Sets the <c>nc</c> (nonce-count) auth-param (rendered as a bare token, unquoted).
		 *
		 * @param value The 8-digit hex nonce-count (e.g. <js>"00000001"</js>). Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder nc(String value) {
			return tokenParam("nc", value);
		}

		/**
		 * Generic escape hatch for setting any auth-param by name, rendered as a quoted-string.
		 *
		 * <p>
		 * Replaces any previously-registered value for the same param while preserving its original position, and
		 * clears any previously-set {@link #token(String) token68}.
		 *
		 * @param name The auth-param name. Must not be <jk>null</jk> or blank.
		 * @param value The auth-param value (quoted automatically). Must not be <jk>null</jk>.
		 * @return This object.
		 * @throws IllegalArgumentException If {@code name} is <jk>null</jk> or blank.
		 */
		public Builder param(String name, String value) {
			return quotedParam(name, value);
		}

		private Builder quotedParam(String name, String value) {
			assertArgNotNull("value", value);
			token68 = null;
			params.put(cleanName(name), q(value));
			return this;
		}

		private Builder tokenParam(String name, String value) {
			assertArgNotNull("value", value);
			token68 = null;
			params.put(cleanName(name), value);
			return this;
		}

		private static String cleanName(String name) {
			assertArgNotNull("name", name);
			var n = name.trim();
			if (n.isEmpty())
				throw iaex("auth-param name must not be blank");
			return n;
		}

		private static String q(String value) {
			var sb = new StringBuilder(value.length() + 2);
			sb.append('"');
			for (var i = 0; i < value.length(); i++) {
				var c = value.charAt(i);
				if (c == '\\' || c == '"')
					sb.append('\\');
				sb.append(c);
			}
			sb.append('"');
			return sb.toString();
		}

		/**
		 * Builds the rendered <c>Proxy-Authorization</c> credentials value.
		 *
		 * <p>
		 * Returns an empty string when no scheme has been registered.
		 *
		 * @return The header value. Never <jk>null</jk>.
		 */
		public String build() {
			if (scheme == null)
				return "";
			var sb = new StringBuilder(scheme);
			if (token68 != null) {
				sb.append(' ').append(token68);
			} else if (! params.isEmpty()) {
				sb.append(' ');
				var parts = new ArrayList<String>(params.size());
				params.forEach((k, v) -> parts.add(k + "=" + v));
				sb.append(String.join(", ", parts));
			}
			return sb.toString();
		}

		/**
		 * Builds a {@link ProxyAuthorization} header bean directly from this builder.
		 *
		 * @return A {@link ProxyAuthorization} carrying {@link #build()}. Never <jk>null</jk>.
		 */
		public ProxyAuthorization toHeader() {
			return ProxyAuthorization.of(build());
		}

		@Override
		public String toString() {
			return build();
		}
	}
}
