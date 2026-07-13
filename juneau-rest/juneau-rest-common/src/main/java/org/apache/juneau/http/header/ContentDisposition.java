/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at
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
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.http.*;


/**
 * Represents an HTTP <c>Content-Disposition</c> header.
 *
 * <p>
 * Content disposition (inline / attachment / filename).
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.marshall.ng.*}).
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"java:S1192" // Duplicated "value" literals are HTTP header component keys; a constant would obscure the header grammar.
})
public class ContentDisposition extends HttpStringRangesHeader {

	public static final String NAME = "Content-Disposition";

	public ContentDisposition(String value) {
		super(NAME, value);
	}

	public ContentDisposition(StringRanges value) {
		super(NAME, value);
	}

	private ContentDisposition(Supplier<?> supplier, int lazyMode) {
		super(NAME, supplier, lazyMode);
	}

	public static ContentDisposition of(String value) {
		return new ContentDisposition(value);
	}

	public static ContentDisposition of(StringRanges value) {
		return new ContentDisposition(value);
	}

	public static ContentDisposition ofLazyWire(Supplier<String> supplier) {
		return new ContentDisposition(supplier, LAZY_WIRE_STRING);
	}

	public static ContentDisposition ofLazyParsed(Supplier<StringRanges> supplier) {
		return new ContentDisposition(supplier, LAZY_STRING_RANGES);
	}

	/**
	 * Returns a {@code Content-Disposition} header value of the form {@code attachment; filename="<filename>"} with
	 * the file name properly escaped (backslashes and double-quotes are backslash-escaped).
	 *
	 * @param filename The attachment filename. Must not be {@code null}, blank, or contain CR/LF characters.
	 * @return A new {@code ContentDisposition} header.
	 * @throws IllegalArgumentException If {@code filename} is invalid.
	 */
	public static ContentDisposition attachment(String filename) {
		if (isBlank(filename))
			throw iaex("Attachment filename must not be null or blank.");
		for (var i = 0; i < filename.length(); i++) {
			var c = filename.charAt(i);
			if (c == '\r' || c == '\n')
				throw iaex("Attachment filename must not contain CR or LF characters.");
		}
		var sb = new StringBuilder(filename.length() + 8);
		for (var i = 0; i < filename.length(); i++) {
			var c = filename.charAt(i);
			if (c == '\\' || c == '"')
				sb.append('\\');
			sb.append(c);
		}
		return ContentDisposition.of("attachment; filename=\"" + sb + "\"");
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
	 * Fluent builder for assembling an HTTP <c>Content-Disposition</c> header value.
	 *
	 * <p>
	 * Composes the disposition-type and parameters defined by
	 * <a class='doclink' href='https://www.rfc-editor.org/rfc/rfc6266'>RFC 6266</a> into the
	 * <c>disposition-type; param=value; ...</c> wire format.  Typed setters cover the common parameters and apply the
	 * correct rendering (quoted-string for textual params, a bare number for <c>size</c>, and a raw
	 * <a class='doclink' href='https://www.rfc-editor.org/rfc/rfc5987'>RFC 5987</a> ext-value for <c>filename*</c>);
	 * {@link #param(String, String)} is a generic escape hatch.  Setters overwrite a previously-set parameter of the
	 * same name while preserving its original position.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	String <jv>value</jv> = ContentDisposition.<jsm>create</jsm>()
	 * 		.attachment()
	 * 		.filename(<js>"genome.jpeg"</js>)
	 * 		.size(12345)
	 * 		.build();
	 * 	<jc>// =&gt; "attachment; filename=\"genome.jpeg\"; size=12345"</jc>
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jc'>{@link ContentDisposition}
	 * 	<li class='extlink'><a class='doclink' href='https://www.rfc-editor.org/rfc/rfc6266'>RFC 6266 - Use of the Content-Disposition Header Field</a>
	 * </ul>
	 *
	 * @since 10.0.0
	 */
	public static class Builder {

		private String type;
		private final Map<String,String> params = new LinkedHashMap<>();

		/**
		 * Sets the disposition-type (e.g. <js>"inline"</js>, <js>"attachment"</js>, <js>"form-data"</js>).
		 *
		 * @param value The disposition-type. Must not be <jk>null</jk> or blank.
		 * @return This object.
		 * @throws IllegalArgumentException If {@code value} is <jk>null</jk> or blank.
		 */
		public Builder type(String value) {
			assertArgNotNull("value", value);
			var v = value.trim();
			if (v.isEmpty())
				throw iaex("disposition-type must not be blank");
			type = v;
			return this;
		}

		/**
		 * Sets the disposition-type to <c>inline</c>.
		 *
		 * @return This object.
		 */
		public Builder inline() {
			return type("inline");
		}

		/**
		 * Sets the disposition-type to <c>attachment</c>.
		 *
		 * @return This object.
		 */
		public Builder attachment() {
			return type("attachment");
		}

		/**
		 * Sets the disposition-type to <c>form-data</c>.
		 *
		 * @return This object.
		 */
		public Builder formData() {
			return type("form-data");
		}

		/**
		 * Sets the <c>name</c> parameter (rendered as a quoted-string).
		 *
		 * @param value The field name. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder name(String value) {
			return quotedParam("name", value);
		}

		/**
		 * Sets the <c>filename</c> parameter (rendered as a quoted-string with backslash/quote escaping).
		 *
		 * @param value The filename. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder filename(String value) {
			return quotedParam("filename", value);
		}

		/**
		 * Sets the <c>filename*</c> parameter (rendered as a raw RFC 5987 ext-value, unquoted).
		 *
		 * @param value The encoded ext-value (e.g. <js>"UTF-8''%e2%82%ac%20rates"</js>). Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder filenameExt(String value) {
			return tokenParam("filename*", value);
		}

		/**
		 * Sets the <c>creation-date</c> parameter (rendered as a quoted HTTP-date).
		 *
		 * @param value The creation date. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder creationDate(String value) {
			return quotedParam("creation-date", value);
		}

		/**
		 * Sets the <c>modification-date</c> parameter (rendered as a quoted HTTP-date).
		 *
		 * @param value The modification date. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder modificationDate(String value) {
			return quotedParam("modification-date", value);
		}

		/**
		 * Sets the <c>read-date</c> parameter (rendered as a quoted HTTP-date).
		 *
		 * @param value The read date. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder readDate(String value) {
			return quotedParam("read-date", value);
		}

		/**
		 * Sets the <c>size</c> parameter (rendered as a bare number).
		 *
		 * @param value The size in bytes. Must be non-negative.
		 * @return This object.
		 * @throws IllegalArgumentException If {@code value} is negative.
		 */
		public Builder size(long value) {
			if (value < 0)
				throw iaex("size must be non-negative: {0}", value);
			return tokenParam("size", Long.toString(value));
		}

		/**
		 * Generic escape hatch for setting any parameter by name, rendered as a quoted-string.
		 *
		 * <p>
		 * Replaces any previously-registered value for the same parameter while preserving its original position.
		 *
		 * @param name The parameter name. Must not be <jk>null</jk> or blank.
		 * @param value The parameter value (quoted automatically). Must not be <jk>null</jk>.
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
				throw iaex("parameter name must not be blank");
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
		 * Builds the rendered <c>Content-Disposition</c> header value.
		 *
		 * <p>
		 * Returns an empty string when no disposition-type or parameters have been registered.
		 *
		 * @return The header value. Never <jk>null</jk>.
		 */
		public String build() {
			var parts = new ArrayList<String>(params.size() + 1);
			if (type != null)
				parts.add(type);
			params.forEach((k, v) -> parts.add(k + "=" + v));
			return String.join("; ", parts);
		}

		/**
		 * Builds a {@link ContentDisposition} header bean directly from this builder.
		 *
		 * @return A {@link ContentDisposition} carrying {@link #build()}. Never <jk>null</jk>.
		 */
		public ContentDisposition toHeader() {
			return ContentDisposition.of(build());
		}

		@Override
		public String toString() {
			return build();
		}
	}
}
