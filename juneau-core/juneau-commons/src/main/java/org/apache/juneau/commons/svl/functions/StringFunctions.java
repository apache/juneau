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

import java.util.*;

import org.apache.juneau.commons.svl.*;

/**
 * String-manipulation functions for the {@code #{...}} script catalog.
 *
 * <p>
 * Each public static nested class is a {@link TypedFunction} registered under the function name
 * shown in its {@link VarFunction#name()} method. The {@code Replaces} annotation in each
 * nested-class Javadoc names the legacy {@code Var} class that was retired in 9.5.0 in favor of
 * the function form (see {@code FINISHED-102-svl-scripting.md} for the full migration table).
 */
public final class StringFunctions {

	private StringFunctions() {}

	/** All function classes in this category, in registration order. */
	@SuppressWarnings({"unchecked","java:S2368"})
	public static final Class<? extends VarFunction>[] ALL = new Class[] {
		Substring.class, Upper.class, Lower.class, Trim.class, StripLeading.class,
		StripTrailing.class, StripSlashes.class, PathToken.class, Len.class, Replace.class,
		Contains.class, StartsWith.class, EndsWith.class, Concat.class, Repeat.class,
		Reverse.class, Format.class, Split.class, Join.class
	};

	/**
	 * {@code #{substring(s, start)}} / {@code #{substring(s, start, end)}} — replacement for the
	 * legacy {@code $ST{...}} {@code SubstringVar}.
	 *
	 * <p>
	 * Both endpoints are clamped to {@code [0, len(s)]} so out-of-range arguments don't throw —
	 * matches the legacy {@code SubstringVar} behavior.
	 */
	public static class Substring extends TypedFunction {
		@Override public String name() { return "substring"; }
		public String invoke(String s, int start) {
			if (s == null) return "";
			var len = s.length();
			var a = Math.max(0, Math.min(start, len));
			return s.substring(a);
		}
		public String invoke(String s, int start, int end) {
			if (s == null) return "";
			var len = s.length();
			var a = Math.max(0, Math.min(start, len));
			var b = Math.max(a, Math.min(end, len));
			return s.substring(a, b);
		}
	}

	/** {@code #{upper(s)}} — replacement for the legacy {@code $UC{...}}. */
	public static class Upper extends TypedFunction {
		@Override public String name() { return "upper"; }
		public String invoke(String s) { return s == null ? "" : s.toUpperCase(); }
	}

	/** {@code #{lower(s)}} — replacement for the legacy {@code $LC{...}}. */
	public static class Lower extends TypedFunction {
		@Override public String name() { return "lower"; }
		public String invoke(String s) { return s == null ? "" : s.toLowerCase(); }
	}

	/** {@code #{trim(s)}} — strips leading and trailing whitespace. */
	public static class Trim extends TypedFunction {
		@Override public String name() { return "trim"; }
		public String invoke(String s) { return s == null ? "" : s.trim(); }
	}

	/** {@code #{stripLeading(s)}} — strips leading whitespace. */
	public static class StripLeading extends TypedFunction {
		@Override public String name() { return "stripLeading"; }
		public String invoke(String s) { return s == null ? "" : s.stripLeading(); }
	}

	/** {@code #{stripTrailing(s)}} — strips trailing whitespace. */
	public static class StripTrailing extends TypedFunction {
		@Override public String name() { return "stripTrailing"; }
		public String invoke(String s) { return s == null ? "" : s.stripTrailing(); }
	}

	/** {@code #{stripSlashes(s)}} — strips leading and trailing {@code /} characters. */
	public static class StripSlashes extends TypedFunction {
		@Override public String name() { return "stripSlashes"; }
		public String invoke(String s) {
			if (s == null) return "";
			var start = 0;
			var end = s.length();
			while (start < end && s.charAt(start) == '/') start++;
			while (end > start && s.charAt(end - 1) == '/') end--;
			return s.substring(start, end);
		}
	}

	/**
	 * {@code #{pathToken(s)}} — normalize a user-supplied path token for safe interpolation
	 * into a {@code path=} template.
	 *
	 * <p>
	 * Strips leading and trailing {@code /}, plus a trailing {@code /*} (or {@code *} that
	 * sits immediately after a {@code /}). Multi-segment paths are preserved.
	 *
	 * <h5 class='section'>Edge cases:</h5>
	 * <ul>
	 * 	<li>{@code ""} → {@code ""}
	 * 	<li>{@code "/"} → {@code ""}
	 * 	<li>{@code "/*"} → {@code ""}
	 * 	<li>{@code "jsp"} → {@code "jsp"}
	 * 	<li>{@code "/jsp"} → {@code "jsp"}
	 * 	<li>{@code "/jsp/"} → {@code "jsp"}
	 * 	<li>{@code "/jsp/*"} → {@code "jsp"}
	 * 	<li>{@code "jsp/*"} → {@code "jsp"}
	 * 	<li>{@code "/api/v1/*"} → {@code "api/v1"}
	 * </ul>
	 *
	 * <p>
	 * Used in SVL-configurable mixin paths (FINISHED-101 retrofit) so users can supply
	 * {@code "jsp"}, {@code "/jsp"}, {@code "/jsp/"}, {@code "/jsp/*"}, or {@code "jsp/*"} and
	 * the template {@code /#{pathToken(${juneau.jsp.path:jsp})}/*} always resolves to a clean
	 * {@code /jsp/*}.
	 */
	public static class PathToken extends TypedFunction {
		@Override public String name() { return "pathToken"; }
		public String invoke(String s) {
			if (s == null || s.isEmpty()) return "";
			var t = s;
			// Strip a trailing /* or trailing * (when sitting right after a /).
			if (t.endsWith("/*"))
				t = t.substring(0, t.length() - 2);
			else if (t.endsWith("*") && t.length() >= 2 && t.charAt(t.length() - 2) == '/')
				t = t.substring(0, t.length() - 1);
			// Strip leading and trailing slashes.
			var start = 0;
			var end = t.length();
			while (start < end && t.charAt(start) == '/') start++;
			while (end > start && t.charAt(end - 1) == '/') end--;
			return t.substring(start, end);
		}
	}

	/**
	 * {@code #{len(s)}} / {@code #{len(s, delimiter)}} — replacement for the legacy
	 * {@code $LN{...}}.
	 *
	 * <p>
	 * Single-arg form returns the character count. Two-arg form splits on the literal
	 * {@code delimiter} (after trimming leading/trailing whitespace, matching the legacy
	 * {@code LenVar} behavior) and returns the part count.
	 */
	public static class Len extends TypedFunction {
		@Override public String name() { return "len"; }
		public String invoke(String s) { return s == null ? "0" : String.valueOf(s.length()); }
		public String invoke(String s, String delimiter) {
			if (s == null) return "0";
			if (delimiter == null || delimiter.isEmpty()) return String.valueOf(s.length());
			return String.valueOf(s.trim().split(java.util.regex.Pattern.quote(delimiter)).length);
		}
	}

	/**
	 * {@code #{replace(s, target, replacement)}} — literal (non-regex) string replacement.
	 *
	 * <p>
	 * For regex replacement use {@code #{replaceRegex(s, regex, replacement)}}.
	 */
	public static class Replace extends TypedFunction {
		@Override public String name() { return "replace"; }
		public String invoke(String s, String target, String replacement) {
			if (s == null) return "";
			return s.replace(target, replacement == null ? "" : replacement);
		}
	}

	/** {@code #{contains(s, substr)}} — returns {@code "true"} or {@code "false"}. */
	public static class Contains extends TypedFunction {
		@Override public String name() { return "contains"; }
		public String invoke(String s, String substr) {
			return String.valueOf(s != null && substr != null && s.contains(substr));
		}
	}

	/** {@code #{startsWith(s, prefix)}} — returns {@code "true"} or {@code "false"}. */
	public static class StartsWith extends TypedFunction {
		@Override public String name() { return "startsWith"; }
		public String invoke(String s, String prefix) {
			return String.valueOf(s != null && prefix != null && s.startsWith(prefix));
		}
	}

	/** {@code #{endsWith(s, suffix)}} — returns {@code "true"} or {@code "false"}. */
	public static class EndsWith extends TypedFunction {
		@Override public String name() { return "endsWith"; }
		public String invoke(String s, String suffix) {
			return String.valueOf(s != null && suffix != null && s.endsWith(suffix));
		}
	}

	/** {@code #{concat(s1, s2, ...)}} — variadic string concatenation. */
	public static class Concat extends TypedFunction {
		@Override public String name() { return "concat"; }
		public String invoke(String[] parts) {
			var sb = new StringBuilder();
			for (var p : parts)
				if (p != null) sb.append(p);
			return sb.toString();
		}
	}

	/** {@code #{repeat(s, n)}} — repeats {@code s} {@code n} times. */
	public static class Repeat extends TypedFunction {
		@Override public String name() { return "repeat"; }
		public String invoke(String s, int n) {
			if (s == null || n <= 0) return "";
			return s.repeat(n);
		}
	}

	/** {@code #{reverse(s)}} — reverses the characters of {@code s}. */
	public static class Reverse extends TypedFunction {
		@Override public String name() { return "reverse"; }
		public String invoke(String s) {
			return s == null ? "" : new StringBuilder(s).reverse().toString();
		}
	}

	/**
	 * {@code #{format(pattern, args...)}} — {@link String#format Java formatter-style
	 * formatting}. Pattern uses Java {@link java.util.Formatter} syntax (e.g. {@code %s},
	 * {@code %d}, {@code %.2f}). Variadic args; coerced per Java {@code Formatter} rules at
	 * invocation.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p>
	 * {@code #{format("%s-%03d", ${prefix}, ${counter})}}
	 *
	 * <p>
	 * All args are passed as raw {@link String} values; format specifiers like {@code %d} will
	 * convert numeric strings via {@link Integer#parseInt} / {@link Long#parseLong} as needed.
	 * {@code %s} accepts any string verbatim.
	 */
	public static class Format extends TypedFunction {
		@Override public String name() { return "format"; }
		public String invoke(String pattern, String[] args) {
			if (pattern == null) return "";
			var coerced = new Object[args.length];
			for (var i = 0; i < args.length; i++)
				coerced[i] = coerceArgForFormat(args[i]);
			return String.format(pattern, coerced);
		}

		/**
		 * Best-effort numeric coercion for {@code %d}/{@code %f}-style specifiers — strings
		 * that parse as long/double are passed as the typed value, otherwise as the raw
		 * string. This keeps {@code %s} as the safe default while letting numeric specifiers
		 * work without a separate {@code numericFormat(...)} entry point.
		 */
		@SuppressWarnings("java:S1166") // Exception swallowed — coercion is best-effort.
		private static Object coerceArgForFormat(String s) {
			if (s == null) return "";
			var t = s.trim();
			try { return Long.parseLong(t); } catch (@SuppressWarnings("unused") NumberFormatException e) { /* fall through */ }
			try { return Double.parseDouble(t); } catch (@SuppressWarnings("unused") NumberFormatException e) { /* fall through */ }
			return s;
		}
	}

	/**
	 * {@code #{split(s, separator)}} — splits {@code s} on the literal {@code separator}
	 * substring. Returns a JSON-array-shortcut string (e.g. {@code ["a","b","c"]}) so the
	 * result composes with other functions that accept {@code String[]}. Trailing empty fields
	 * dropped.
	 */
	public static class Split extends TypedFunction {
		@Override public String name() { return "split"; }
		public String invoke(String s, String separator) {
			if (s == null || s.isEmpty()) return "[]";
			var parts = new ArrayList<String>();
			if (separator == null || separator.isEmpty()) {
				parts.add(s);
			} else {
				var i = 0;
				while (true) {
					var hit = s.indexOf(separator, i);
					if (hit < 0) { parts.add(s.substring(i)); break; }
					parts.add(s.substring(i, hit));
					i = hit + separator.length();
				}
			}
			// Drop trailing empties.
			while (!parts.isEmpty() && parts.get(parts.size() - 1).isEmpty())
				parts.remove(parts.size() - 1);
			return JsonShortcut.encodeArray(parts);
		}
	}

	/**
	 * {@code #{join(separator, parts...)}} — concatenates {@code parts} with {@code separator}
	 * between each pair.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p>
	 * {@code #{join("/", ${tenant}, ${id})}} → {@code acme/42}
	 */
	public static class Join extends TypedFunction {
		@Override public String name() { return "join"; }
		public String invoke(String separator, String[] parts) {
			var sep = separator == null ? "" : separator;
			var sb = new StringBuilder();
			for (var i = 0; i < parts.length; i++) {
				if (i > 0) sb.append(sep);
				sb.append(parts[i] == null ? "" : parts[i]);
			}
			return sb.toString();
		}
	}
}
