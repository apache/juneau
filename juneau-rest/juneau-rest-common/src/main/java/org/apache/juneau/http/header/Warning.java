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
 * Represents an HTTP <c>Warning</c> header.
 *
 * <p>
 * A general warning about possible problems with the entity body.
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
public class Warning extends HttpStringHeader {

	/** The header name */
	public static final String NAME = "Warning";

	/**
	 * Constructor with an eager value.
	 *
	 * @param value The header value. May be <jk>null</jk>.
	 */
	public Warning(String value) {
		super(NAME, value);
	}

	/**
	 * Constructor with a lazy value supplier.
	 *
	 * @param valueSupplier Supplier for the header value. Must not be <jk>null</jk>.
	 */
	public Warning(Supplier<String> valueSupplier) {
		super(NAME, valueSupplier);
	}

	/**
	 * Static factory method with an eager value.
	 *
	 * @param value The header value. May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static Warning of(String value) {
		return new Warning(value);
	}

	/**
	 * Static factory method with a lazy value supplier.
	 *
	 * @param valueSupplier Supplier for the header value. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static Warning of(Supplier<String> valueSupplier) {
		return new Warning(valueSupplier);
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
	 * Fluent builder for assembling an HTTP <c>Warning</c> header value.
	 *
	 * <p>
	 * Composes one or more warning-values defined by
	 * <a class='doclink' href='https://www.rfc-editor.org/rfc/rfc7234#section-5.5'>RFC 7234 §5.5</a> into the
	 * <c>warn-code SP warn-agent SP warn-text [SP warn-date]</c> form, with multiple warnings <c>,</c>-separated.  The
	 * 3-digit warn-code is zero-padded, the warn-text and optional warn-date are rendered as quoted-strings, and
	 * {@link #add(String)} is a generic escape hatch for a fully pre-rendered warning-value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	String <jv>value</jv> = Warning.<jsm>create</jsm>()
	 * 		.warning(110, <js>"anderson/1.3.37"</js>, <js>"Response is stale"</js>)
	 * 		.build();
	 * 	<jc>// =&gt; "110 anderson/1.3.37 \"Response is stale\""</jc>
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jc'>{@link Warning}
	 * 	<li class='extlink'><a class='doclink' href='https://www.rfc-editor.org/rfc/rfc7234'>RFC 7234 - HTTP/1.1 Caching</a>
	 * </ul>
	 *
	 * @since 10.0.0
	 */
	public static class Builder {

		private final List<String> warnings = new ArrayList<>();

		/**
		 * Adds a warning-value (without a warn-date).
		 *
		 * @param code The 3-digit warn-code. Must be in the range 0-999.
		 * @param agent The warn-agent (host:port or pseudonym, e.g. <js>"-"</js>). Must not be <jk>null</jk> or blank.
		 * @param text The human-readable warn-text. Must not be <jk>null</jk>.
		 * @return This object.
		 * @throws IllegalArgumentException If {@code code} is out of range or {@code agent} is <jk>null</jk> or blank.
		 */
		public Builder warning(int code, String agent, String text) {
			return add(render(code, agent, text, null));
		}

		/**
		 * Adds a warning-value with a warn-date.
		 *
		 * @param code The 3-digit warn-code. Must be in the range 0-999.
		 * @param agent The warn-agent (host:port or pseudonym, e.g. <js>"-"</js>). Must not be <jk>null</jk> or blank.
		 * @param text The human-readable warn-text. Must not be <jk>null</jk>.
		 * @param date The warn-date (an HTTP-date). Must not be <jk>null</jk>.
		 * @return This object.
		 * @throws IllegalArgumentException If {@code code} is out of range or {@code agent}/{@code date} is invalid.
		 */
		public Builder warning(int code, String agent, String text, String date) {
			assertArgNotNull("date", date);
			return add(render(code, agent, text, date));
		}

		/**
		 * Generic escape hatch for adding a fully pre-rendered warning-value verbatim.
		 *
		 * @param value The pre-rendered warning-value. Must not be <jk>null</jk> or blank.
		 * @return This object.
		 * @throws IllegalArgumentException If {@code value} is <jk>null</jk> or blank.
		 */
		public Builder add(String value) {
			assertArgNotNull("value", value);
			var v = value.trim();
			if (v.isEmpty())
				throw iaex("warning value must not be blank");
			warnings.add(v);
			return this;
		}

		private static String render(int code, String agent, String text, String date) {
			if (code < 0 || code > 999)
				throw iaex("warn-code must be in the range 0-999: {0}", code);
			assertArgNotNull("agent", agent);
			assertArgNotNull("text", text);
			var a = agent.trim();
			if (a.isEmpty())
				throw iaex("warn-agent must not be blank");
			var sb = new StringBuilder();
			sb.append(String.format("%03d", code)).append(' ').append(a).append(' ').append(q(text));
			if (date != null)
				sb.append(' ').append(q(date));
			return sb.toString();
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
		 * Builds the rendered <c>Warning</c> header value.
		 *
		 * <p>
		 * Returns an empty string when no warnings have been registered.
		 *
		 * @return The header value. Never <jk>null</jk>.
		 */
		public String build() {
			return String.join(", ", warnings);
		}

		/**
		 * Builds a {@link Warning} header bean directly from this builder.
		 *
		 * @return A {@link Warning} carrying {@link #build()}. Never <jk>null</jk>.
		 */
		public Warning toHeader() {
			return Warning.of(build());
		}

		@Override
		public String toString() {
			return build();
		}
	}
}
