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

import java.util.*;
import java.util.function.*;

/**
 * Represents an HTTP <c>WWW-Authenticate</c> header.
 *
 * <p>
 * Indicates the authentication scheme that should be used to access the requested entity.
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
public class WwwAuthenticate extends HttpStringHeader {

	/** The header name */
	public static final String NAME = "WWW-Authenticate";

	/**
	 * Constructor with an eager value.
	 *
	 * @param value The header value. May be <jk>null</jk>.
	 */
	public WwwAuthenticate(String value) {
		super(NAME, value);
	}

	/**
	 * Constructor with a lazy value supplier.
	 *
	 * @param valueSupplier Supplier for the header value. Must not be <jk>null</jk>.
	 */
	public WwwAuthenticate(Supplier<String> valueSupplier) {
		super(NAME, valueSupplier);
	}

	/**
	 * Static factory method with an eager value.
	 *
	 * @param value The header value. May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static WwwAuthenticate of(String value) {
		return new WwwAuthenticate(value);
	}

	/**
	 * Static factory method with a lazy value supplier.
	 *
	 * @param valueSupplier Supplier for the header value. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static WwwAuthenticate of(Supplier<String> valueSupplier) {
		return new WwwAuthenticate(valueSupplier);
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
	 * Creates a {@link Builder} pre-populated with a <c>Basic</c> challenge for the given realm.
	 *
	 * @param realm The protection space realm. Must not be <jk>null</jk>.
	 * @return A new builder. Never <jk>null</jk>.
	 */
	public static Builder basic(String realm) {
		return create().basic(realm);
	}

	/**
	 * Creates a {@link Builder} pre-populated with a <c>Bearer</c> challenge.
	 *
	 * @return A new builder. Never <jk>null</jk>.
	 */
	public static Builder bearer() {
		return create().bearer();
	}

	/**
	 * Creates a {@link Builder} pre-populated with a <c>Digest</c> challenge.
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
	 * Fluent builder for assembling an HTTP <c>WWW-Authenticate</c> challenge value.
	 *
	 * <p>
	 * Composes the <c>scheme [ 1*SP #auth-param ]</c> challenge form defined by
	 * <a class='doclink' href='https://www.rfc-editor.org/rfc/rfc7235#section-4.1'>RFC 7235 §4.1</a>.  Typed setters
	 * cover the common Basic / Bearer / Digest auth-params and apply the correct quoting (quoted-string vs token);
	 * {@link #param(String, String)} is a generic escape hatch so new params never require an API change.  The scheme
	 * presets ({@link #basic(String)} / {@link #bearer()} / {@link #digest()}) overwrite the scheme; auth-param setters
	 * overwrite a previously-set param of the same name while preserving its original position.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	String <jv>value</jv> = WwwAuthenticate.<jsm>create</jsm>()
	 * 		.digest()
	 * 		.realm(<js>"http-auth@example.org"</js>)
	 * 		.qop(<js>"auth"</js>, <js>"auth-int"</js>)
	 * 		.nonce(<js>"7ypf/xlj9XXwfDPEoM4URrv/xwf94BcCAzFZH4GiTo0v"</js>)
	 * 		.algorithm(<js>"SHA-256"</js>)
	 * 		.build();
	 * 	<jc>// =&gt; "Digest realm=\"http-auth@example.org\", qop=\"auth,auth-int\", nonce=\"...\", algorithm=SHA-256"</jc>
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jc'>{@link WwwAuthenticate}
	 * 	<li class='extlink'><a class='doclink' href='https://www.rfc-editor.org/rfc/rfc7235'>RFC 7235 - HTTP/1.1 Authentication</a>
	 * </ul>
	 *
	 * @since 10.0.0
	 */
	public static class Builder {

		private String scheme;
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
		 * Sets the scheme to <c>Basic</c> and the <c>realm</c> auth-param.
		 *
		 * @param realm The protection space realm. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder basic(String realm) {
			return scheme("Basic").realm(realm);
		}

		/**
		 * Sets the scheme to <c>Bearer</c>.
		 *
		 * @return This object.
		 */
		public Builder bearer() {
			return scheme("Bearer");
		}

		/**
		 * Sets the scheme to <c>Digest</c>.
		 *
		 * @return This object.
		 */
		public Builder digest() {
			return scheme("Digest");
		}

		/**
		 * Sets the <c>realm</c> auth-param (rendered as a quoted-string).
		 *
		 * @param value The realm value. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder realm(String value) {
			return quotedParam("realm", value);
		}

		/**
		 * Sets the <c>charset</c> auth-param (rendered as a quoted-string).
		 *
		 * @param value The charset (e.g. <js>"UTF-8"</js>). Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder charset(String value) {
			return quotedParam("charset", value);
		}

		/**
		 * Sets the <c>qop</c> auth-param to a comma-separated, quoted list of quality-of-protection tokens.
		 *
		 * @param values The qop tokens (e.g. <js>"auth"</js>, <js>"auth-int"</js>). Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder qop(String...values) {
			assertArgNotNull("values", values);
			return quotedParam("qop", String.join(",", values));
		}

		/**
		 * Sets the <c>nonce</c> auth-param (rendered as a quoted-string).
		 *
		 * @param value The server nonce. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder nonce(String value) {
			return quotedParam("nonce", value);
		}

		/**
		 * Sets the <c>opaque</c> auth-param (rendered as a quoted-string).
		 *
		 * @param value The opaque value to be echoed back by the client. Must not be <jk>null</jk>.
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
		 * Sets the <c>domain</c> auth-param to a space-separated, quoted list of URIs.
		 *
		 * @param values The protection space URIs. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder domain(String...values) {
			assertArgNotNull("values", values);
			return quotedParam("domain", String.join(" ", values));
		}

		/**
		 * Sets the <c>stale</c> auth-param (rendered as a bare token, unquoted).
		 *
		 * @param value <jk>true</jk> if the previous request's nonce was stale.
		 * @return This object.
		 */
		public Builder stale(boolean value) {
			return tokenParam("stale", Boolean.toString(value));
		}

		/**
		 * Sets the <c>error</c> auth-param (rendered as a quoted-string; RFC 6750 Bearer).
		 *
		 * @param value The error code (e.g. <js>"invalid_token"</js>). Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder error(String value) {
			return quotedParam("error", value);
		}

		/**
		 * Sets the <c>error_description</c> auth-param (rendered as a quoted-string; RFC 6750 Bearer).
		 *
		 * @param value The human-readable error description. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder errorDescription(String value) {
			return quotedParam("error_description", value);
		}

		/**
		 * Generic escape hatch for setting any auth-param by name, rendered as a quoted-string.
		 *
		 * <p>
		 * Replaces any previously-registered value for the same param while preserving its original position.
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
			params.put(cleanName(name), q(value));
			return this;
		}

		private Builder tokenParam(String name, String value) {
			assertArgNotNull("value", value);
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
		 * Builds the rendered <c>WWW-Authenticate</c> challenge value.
		 *
		 * <p>
		 * Returns an empty string when no scheme or params have been registered.
		 *
		 * @return The header value. Never <jk>null</jk>.
		 */
		public String build() {
			if (scheme == null && params.isEmpty())
				return "";
			var sb = new StringBuilder();
			if (scheme != null)
				sb.append(scheme);
			if (! params.isEmpty()) {
				if (scheme != null)
					sb.append(' ');
				var parts = new ArrayList<String>(params.size());
				params.forEach((k, v) -> parts.add(k + "=" + v));
				sb.append(String.join(", ", parts));
			}
			return sb.toString();
		}

		/**
		 * Builds a {@link WwwAuthenticate} header bean directly from this builder.
		 *
		 * @return A {@link WwwAuthenticate} carrying {@link #build()}. Never <jk>null</jk>.
		 */
		public WwwAuthenticate toHeader() {
			return WwwAuthenticate.of(build());
		}

		@Override
		public String toString() {
			return build();
		}
	}
}
