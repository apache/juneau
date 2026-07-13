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
 * Represents an HTTP <c>Forwarded</c> header.
 *
 * <p>
 * Disclose original information of a client connecting to a web server through an HTTP proxy.
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
public class Forwarded extends HttpStringHeader {

	/** The header name */
	public static final String NAME = "Forwarded";

	/**
	 * Constructor with an eager value.
	 *
	 * @param value The header value. May be <jk>null</jk>.
	 */
	public Forwarded(String value) {
		super(NAME, value);
	}

	/**
	 * Constructor with a lazy value supplier.
	 *
	 * @param valueSupplier Supplier for the header value. Must not be <jk>null</jk>.
	 */
	public Forwarded(Supplier<String> valueSupplier) {
		super(NAME, valueSupplier);
	}

	/**
	 * Static factory method with an eager value.
	 *
	 * @param value The header value. May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static Forwarded of(String value) {
		return new Forwarded(value);
	}

	/**
	 * Static factory method with a lazy value supplier.
	 *
	 * @param valueSupplier Supplier for the header value. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static Forwarded of(Supplier<String> valueSupplier) {
		return new Forwarded(valueSupplier);
	}

	/**
	 * Creates a new empty {@link Builder}.
	 *
	 * @return A new builder. Never <jk>null</jk>.
	 */
	public static Builder create() {
		return new Builder();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Inner types
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Fluent builder for assembling an HTTP <c>Forwarded</c> header value.
	 *
	 * <p>
	 * Composes the forwarding information defined by
	 * <a class='doclink' href='https://www.rfc-editor.org/rfc/rfc7239'>RFC 7239</a> into the
	 * <c>key=value;key=value, key=value</c> wire format: <c>;</c>-separated parameters within a single forwarded
	 * element (one proxy hop) and <c>,</c>-separated elements across hops.  Typed setters cover the four registered
	 * parameters (<c>by</c>, <c>for</c>, <c>host</c>, <c>proto</c>); {@link #param(String, String)} is a generic escape
	 * hatch.  Values are emitted as bare tokens when possible and quoted automatically when they are not valid tokens
	 * (e.g. IPv6 node identifiers such as <c>"[2001:db8::1]:8080"</c>).  Call {@link #next()} to start a new hop.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	String <jv>value</jv> = Forwarded.<jsm>create</jsm>()
	 * 		.forValue(<js>"192.0.2.60"</js>)
	 * 		.proto(<js>"http"</js>)
	 * 		.by(<js>"203.0.113.43"</js>)
	 * 		.next()
	 * 		.forValue(<js>"198.51.100.17"</js>)
	 * 		.build();
	 * 	<jc>// =&gt; "for=192.0.2.60;proto=http;by=203.0.113.43, for=198.51.100.17"</jc>
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jc'>{@link Forwarded}
	 * 	<li class='extlink'><a class='doclink' href='https://www.rfc-editor.org/rfc/rfc7239'>RFC 7239 - Forwarded HTTP Extension</a>
	 * </ul>
	 *
	 * @since 10.0.0
	 */
	public static class Builder {

		private final List<Map<String,String>> elements = new ArrayList<>();
		private Map<String,String> current;

		/**
		 * Constructor.
		 */
		public Builder() {
			current = new LinkedHashMap<>();
			elements.add(current);
		}

		/**
		 * Sets the <c>by</c> parameter (the interface where the request came in to the proxy) on the current element.
		 *
		 * @param value The node identifier. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder by(String value) {
			return param("by", value);
		}

		/**
		 * Sets the <c>for</c> parameter (the client that initiated the request) on the current element.
		 *
		 * @param value The node identifier. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder forValue(String value) {
			return param("for", value);
		}

		/**
		 * Sets the <c>host</c> parameter (the original <c>Host</c> request header) on the current element.
		 *
		 * @param value The host. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder host(String value) {
			return param("host", value);
		}

		/**
		 * Sets the <c>proto</c> parameter (the protocol used to make the request) on the current element.
		 *
		 * @param value The protocol (e.g. <js>"http"</js>, <js>"https"</js>). Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder proto(String value) {
			return param("proto", value);
		}

		/**
		 * Generic escape hatch for setting any forwarded parameter by name on the current element.
		 *
		 * <p>
		 * Replaces any previously-registered value for the same parameter while preserving its original position.  The
		 * value is emitted as a bare token when valid, otherwise as an escaped quoted-string.
		 *
		 * @param name The parameter name. Must not be <jk>null</jk> or blank.
		 * @param value The parameter value. Must not be <jk>null</jk>.
		 * @return This object.
		 * @throws IllegalArgumentException If {@code name} is <jk>null</jk> or blank.
		 */
		public Builder param(String name, String value) {
			assertArgNotNull("name", name);
			assertArgNotNull("value", value);
			var n = name.trim();
			if (n.isEmpty())
				throw iaex("forwarded parameter name must not be blank");
			current.put(n, value);
			return this;
		}

		/**
		 * Commits the current forwarded element and begins a new one (the next proxy hop).
		 *
		 * @return This object.
		 */
		public Builder next() {
			current = new LinkedHashMap<>();
			elements.add(current);
			return this;
		}

		private static boolean isToken(String value) {
			if (value.isEmpty())
				return false;
			for (var i = 0; i < value.length(); i++) {
				var c = value.charAt(i);
				var ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
					|| "!#$%&'*+-.^_`|~".indexOf(c) >= 0;
				if (! ok)
					return false;
			}
			return true;
		}

		private static String render(String value) {
			if (isToken(value))
				return value;
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
		 * Builds the rendered <c>Forwarded</c> header value.
		 *
		 * <p>
		 * Empty elements (hops with no parameters) are omitted.  Returns an empty string when nothing has been
		 * registered.
		 *
		 * @return The header value. Never <jk>null</jk>.
		 */
		public String build() {
			var rendered = new ArrayList<String>(elements.size());
			for (var e : elements) {
				if (e.isEmpty())
					continue;
				var parts = new ArrayList<String>(e.size());
				e.forEach((k, v) -> parts.add(k + "=" + render(v)));
				rendered.add(String.join(";", parts));
			}
			return String.join(", ", rendered);
		}

		/**
		 * Builds a {@link Forwarded} header bean directly from this builder.
		 *
		 * @return A {@link Forwarded} carrying {@link #build()}. Never <jk>null</jk>.
		 */
		public Forwarded toHeader() {
			return Forwarded.of(build());
		}

		@Override
		public String toString() {
			return build();
		}
	}
}
