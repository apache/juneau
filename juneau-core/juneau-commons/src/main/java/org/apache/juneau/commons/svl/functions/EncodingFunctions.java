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
package org.apache.juneau.commons.svl.functions;

import static java.nio.charset.StandardCharsets.*;
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.net.*;
import java.util.*;

import org.apache.juneau.commons.svl.*;

/**
 * Encoding / escaping functions for the {@code #{...}} script catalog.
 *
 * <p>
 * Includes Base64 encode/decode, URL-encoded-form encode/decode, and HTML-injection-safe
 * escape/unescape.
 */
public final class EncodingFunctions {

	private EncodingFunctions() {}

	/** All function classes in this category. */
	@SuppressWarnings({
		"unchecked", // Cast is safe: type verified by caller context.
		"java:S2386" // ALL is an immutable compile-time registry; exposed as an array for the cross-package/varargs functions(...) API, so visibility cannot be reduced.
	})
	public static final Class<? extends VarFunction>[] ALL = new Class[] {
		Base64Encode.class, Base64Decode.class, UrlEncode.class, UrlDecode.class,
		HtmlEscape.class, HtmlUnescape.class
	};

	/** {@code #{base64Encode(s)}} — Base64-encodes {@code s} (UTF-8). */
	public static class Base64Encode extends TypedFunction {
		@Override public String name() { return "base64Encode"; }
		public String invoke(String s) {
			if (s == null) return "";
			return Base64.getEncoder().encodeToString(s.getBytes(UTF_8));
		}
	}

	/** {@code #{base64Decode(s)}} — Base64-decodes {@code s} (UTF-8 result). */
	public static class Base64Decode extends TypedFunction {
		@Override public String name() { return "base64Decode"; }
		public String invoke(String s) {
			if (isEmpty(s)) return "";
			return new String(Base64.getDecoder().decode(s), UTF_8);
		}
	}

	/** {@code #{urlEncode(s)}} — URL-form-encodes (i.e. {@link URLEncoder#encode(String, java.nio.charset.Charset)}). */
	public static class UrlEncode extends TypedFunction {
		@Override public String name() { return "urlEncode"; }
		public String invoke(String s) {
			return s == null ? "" : URLEncoder.encode(s, UTF_8);
		}
	}

	/** {@code #{urlDecode(s)}} — URL-form-decodes. */
	public static class UrlDecode extends TypedFunction {
		@Override public String name() { return "urlDecode"; }
		public String invoke(String s) {
			return s == null ? "" : URLDecoder.decode(s, UTF_8);
		}
	}

	/**
	 * {@code #{htmlEscape(s)}} — escapes {@code &}, {@code <}, {@code >}, {@code "}, {@code '}
	 * (the OWASP minimal HTML-injection-safe set).
	 */
	public static class HtmlEscape extends TypedFunction {
		@Override public String name() { return "htmlEscape"; }
		public String invoke(String s) {
			if (s == null) return "";
			var sb = new StringBuilder(s.length() + 16);
			for (var i = 0; i < s.length(); i++) {
				var c = s.charAt(i);
				switch (c) {
					case '&': sb.append("&amp;"); break;
					case '<': sb.append("&lt;"); break;
					case '>': sb.append("&gt;"); break;
					case '"': sb.append("&quot;"); break;
					case '\'': sb.append("&#39;"); break;
					default: sb.append(c); break;
				}
			}
			return sb.toString();
		}
	}

	/**
	 * {@code #{htmlUnescape(s)}} — inverse of {@link HtmlEscape}. Supports numeric
	 * ({@code &#NNNN;}) and named ({@code &amp;} / {@code &lt;} / {@code &gt;} /
	 * {@code &quot;} / {@code &apos;} / {@code &#39;}) entities.
	 */
	@SuppressWarnings({
		"java:S3776" // Cognitive complexity: small inline entity decoder.
	})
	public static class HtmlUnescape extends TypedFunction {
		@Override public String name() { return "htmlUnescape"; }
		@SuppressWarnings({
			"java:S135" // State-machine entity-decode loop; early continues are clearer than restructuring.
		})
		public String invoke(String s) {
			if (s == null) return "";
			var sb = new StringBuilder(s.length());
			var i = 0;
			while (i < s.length()) {
				var c = s.charAt(i);
				if (c != '&') { sb.append(c); i++; continue; }
				var end = s.indexOf(';', i + 1);
				if (end < 0) { sb.append(c); i++; continue; }
				var entity = s.substring(i + 1, end);
				var decoded = decode(entity);
				if (decoded == null) {
					sb.append(c);
					i++;
				} else {
					sb.append(decoded);
					i = end + 1;
				}
			}
			return sb.toString();
		}

		private static String decode(String entity) {
			if (entity.isEmpty()) return null;
			if (entity.charAt(0) == '#') {
				try {
					int cp;
					if (entity.length() > 1 && (entity.charAt(1) == 'x' || entity.charAt(1) == 'X'))
						cp = Integer.parseInt(entity.substring(2), 16);
					else
						cp = Integer.parseInt(entity.substring(1));
					return new String(Character.toChars(cp));
				} catch (@SuppressWarnings("unused") NumberFormatException e) {
					return null;
				}
			}
			switch (entity) {
				case "amp":   return "&";
				case "lt":    return "<";
				case "gt":    return ">";
				case "quot":  return "\"";
				case "apos":  return "'";
				default:      return null;
			}
		}
	}
}
