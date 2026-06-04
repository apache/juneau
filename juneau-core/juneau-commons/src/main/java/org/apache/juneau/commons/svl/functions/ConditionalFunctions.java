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

import static org.apache.juneau.commons.utils.StringUtils.*;

import org.apache.juneau.commons.svl.*;

/**
 * Conditional and routing functions for the {@code #{...}} script catalog.
 *
 * <p>
 * These four functions replaced the legacy {@code IfVar}, {@code SwitchVar}, {@code CoalesceVar},
 * and {@code NotEmptyVar} classes when the {@code #{...}} script syntax landed in 9.5.0.
 */
public final class ConditionalFunctions {

	private ConditionalFunctions() {}

	/** All function classes in this category. */
	@SuppressWarnings({
		"unchecked", // Cast is safe: type verified by caller context.
		"java:S2386" // ALL is an immutable compile-time registry; exposed as an array for the cross-package/varargs functions(...) API, so visibility cannot be reduced.
	})
	public static final Class<? extends VarFunction>[] ALL = new Class[] {
		If.class, Switch.class, Coalesce.class, NotEmpty.class
	};

	/**
	 * {@code #{if(cond, then, else)}} — replacement for the legacy {@code $IF{...}}.
	 *
	 * <p>
	 * Returns {@code then} if {@code cond} is truthy, {@code else} otherwise. Truthiness rules
	 * match {@link ArgCoercer}'s boolean coercion table (explicit table, no
	 * implicit truthiness).
	 */
	public static class If extends TypedFunction {
		@Override public String name() { return "if"; }
		public String invoke(boolean cond, String thenVal, String elseVal) {
			var then = thenVal == null ? "" : thenVal;
			var els = elseVal == null ? "" : elseVal;
			return cond ? then : els;
		}
	}

	/**
	 * {@code #{switch(value, case1, val1, case2, val2, ..., default)}} — replacement for the
	 * legacy {@code $SW{...}}.
	 *
	 * <p>
	 * Compares {@code value} against each {@code caseN} <i>glob pattern</i> and returns the
	 * matching {@code valN} for the first match. {@code caseN} patterns may contain {@code *}
	 * (any sequence of characters) and {@code ?} (any single character) wildcards — same
	 * semantics as the legacy {@code SwitchVar}. Plain literals (no wildcards) match by
	 * full-string equality.
	 *
	 * <p>
	 * The last unmatched arg is the default; if the arg count is even (no trailing default),
	 * an empty string is returned for unmatched values.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <ul>
	 * 	<li>{@code #{switch(${os}, *win*, Windows, *nix*, Linux, *)}} — glob-routes by substring.
	 * 	<li>{@code #{switch(${env}, prod, A, dev, B, default)}} — exact equality, three branches.
	 * </ul>
	 */
	public static class Switch extends TypedFunction {
		@Override public String name() { return "switch"; }
		public String invoke(String[] args) {
			if (args.length == 0) return "";
			var value = args[0] == null ? "" : args[0];
			var i = 1;
			while (i + 1 < args.length) {
				if (globMatches(value, args[i])) return args[i + 1] == null ? "" : args[i + 1];
				i += 2;
			}
			// Trailing default if arity is even (value + N pairs + default).
			if (i < args.length) return args[i] == null ? "" : args[i];
			return "";
		}

		/** Full-string glob match supporting {@code *} and {@code ?} wildcards. */
		private static boolean globMatches(String value, String pattern) {
			if (pattern == null) return value == null;
			// Compile glob to regex once per call. Translation: '*' -> '.*', '?' -> '.',
			// every other regex metacharacter is quoted.
			var sb = new StringBuilder(pattern.length() + 4);
			for (var i = 0; i < pattern.length(); i++) {
				var c = pattern.charAt(i);
				switch (c) {
					case '*' -> sb.append(".*");
					case '?' -> sb.append('.');
					case '\\', '.', '[', ']', '(', ')', '{', '}', '|', '+', '^', '$' -> sb.append('\\').append(c);
					default -> sb.append(c);
				}
			}
			return java.util.regex.Pattern.compile(sb.toString()).matcher(value).matches();
		}
	}

	/**
	 * {@code #{coalesce(a, b, ...)}} — replacement for the legacy {@code $CO{...}}.
	 *
	 * <p>
	 * Returns the first non-empty arg, or {@code ""} if all args are empty.
	 */
	public static class Coalesce extends TypedFunction {
		@Override public String name() { return "coalesce"; }
		public String invoke(String[] args) {
			for (var a : args)
				if (!isEmpty(a)) return a;
			return "";
		}
	}

	/**
	 * {@code #{notEmpty(s)}} — replacement for the legacy {@code $NE{...}}.
	 *
	 * <p>
	 * Returns {@code "true"} if {@code s} is non-{@code null} and non-empty, {@code "false"}
	 * otherwise. Whitespace-only strings count as non-empty (matches {@code String.isEmpty()}).
	 */
	public static class NotEmpty extends TypedFunction {
		@Override public String name() { return "notEmpty"; }
		public String invoke(String s) { return String.valueOf(!isEmpty(s)); }
	}
}
