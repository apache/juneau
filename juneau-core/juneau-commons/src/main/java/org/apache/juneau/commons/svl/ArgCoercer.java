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
package org.apache.juneau.commons.svl;

import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.util.*;

/**
 * Centralized argument coercion for {@link VarFunction} dispatch.
 *
 * <p>
 * Implements the type-coercion table for {@code #{...}} function arguments:
 * <ul>
 * 	<li>{@link String} → identity.
 * 	<li>{@code int} / {@link Integer} → {@link Integer#parseInt(String)}.
 * 	<li>{@code long} / {@link Long} → {@link Long#parseLong(String)}.
 * 	<li>{@code double} / {@link Double} → {@link Double#parseDouble(String)}.
 * 	<li>{@code boolean} / {@link Boolean} → explicit truthiness table:
 * 		<ul>
 * 			<li>TRUE: {@code "true"}, {@code "1"}, {@code "yes"}, {@code "on"} (case-insensitive).
 * 			<li>FALSE: {@code "false"}, {@code "0"}, {@code "no"}, {@code "off"}, {@code ""}
 * 				(case-insensitive).
 * 			<li>Anything else: {@link IllegalArgumentException}.
 * 		</ul>
 * 	<li>{@code String[]} → JSON-array shortcut parse (e.g. {@code ["a","b","c"]}).
 * 	<li>{@link Object} or any unrecognized type → passthrough as {@link String}.
 * </ul>
 *
 * <p>
 * Coercion failures wrap the underlying {@link NumberFormatException} (etc.) in an
 * {@link IllegalArgumentException} carrying the function name and arg index for debuggability:
 * <c>"Function 'X' arg N: cannot coerce 'Y' to int"</c>.
 *
 * <h5 class='section'>Variadic functions:</h5>
 * <p>
 * When a function declares a final {@code String[]} parameter (the variadic position), excess
 * arguments are gathered into an array and coerced to {@code String} per element. Other array
 * types in the variadic position are not supported.
 *
 * <h5 class='section'>Typed-arg coercion (the recommended path):</h5>
 * <p>
 * {@link TypedFunction} captures the per-arg target {@link Class types} from its concrete
 * subclass's {@code invoke(...)} method signature, then drives this coercer. Direct
 * implementations of {@link VarFunction} bypass the typed table; their {@code invoke(...)}
 * receives raw {@link String} args via {@link #passthrough(List)}.
 */
final class ArgCoercer {

	private ArgCoercer() {}

	/**
	 * Returns {@code args} as-is, unwrapped — used by direct {@link VarFunction} implementations
	 * that don't go through {@link TypedFunction}'s typed-arg coercion.
	 */
	static List<Object> passthrough(List<Object> args) {
		return args;
	}

	/**
	 * Coerce a list of raw arguments to the target types declared by {@code paramTypes}.
	 *
	 * <p>
	 * Used by {@link TypedFunction#invoke(VarResolverSession, List)}. The args are resolved
	 * strings produced by {@link VarTemplate#resolve(VarResolverSession)} per
	 * {@link ScriptSegment} arg template.
	 *
	 * @param fnName The function name (for diagnostic messages).
	 * @param paramTypes The target Java types — one per arg slot. Last slot may be
	 *	 {@code String[]} for variadic functions; in that case all args from that index onward
	 *	 are collected into a {@code String[]}.
	 * @param args The raw resolved string arguments (one per declared arg, with extras for
	 *	 variadic).
	 * @return A list of coerced values, length equal to {@code paramTypes.length}.
	 * @throws IllegalArgumentException If the number of args is wrong or coercion fails.
	 */
	static Object[] coerce(String fnName, Class<?>[] paramTypes, List<Object> args) {
		var n = paramTypes.length;
		var variadic = n > 0 && paramTypes[n - 1] == String[].class;
		var fixedCount = variadic ? n - 1 : n;
		if (args.size() < fixedCount)
			throw illegalArg("Function ''{0}'' expected at least {1} arg(s), got {2}",
				fnName, fixedCount, args.size());
		if (!variadic && args.size() > n)
			throw illegalArg("Function ''{0}'' expected at most {1} arg(s), got {2}",
				fnName, n, args.size());

		var out = new Object[n];
		for (var i = 0; i < fixedCount; i++)
			out[i] = coerceOne(fnName, i, paramTypes[i], args.get(i));

		if (variadic) {
			var rest = new String[args.size() - fixedCount];
			for (var i = 0; i < rest.length; i++) {
				var raw = args.get(fixedCount + i);
				rest[i] = raw == null ? "" : raw.toString();
			}
			out[n - 1] = rest;
		}

		return out;
	}

	/**
	 * Coerce a single raw argument to the requested type. Public-static so tests
	 * and function impls can exercise the coercion table directly.
	 *
	 * @param fnName Function name (for diagnostic messages).
	 * @param argIndex Zero-based arg position (for diagnostic messages).
	 * @param target Target Java type.
	 * @param raw The raw value (typically a {@link String} produced by template resolution).
	 * @return The coerced value.
	 */
	@SuppressWarnings({
		"java:S3776", // Cognitive complexity: the type-dispatch chain is intentional.
	})
	static Object coerceOne(String fnName, int argIndex, Class<?> target, Object raw) {
		if (target == String.class)
			return raw == null ? "" : raw.toString();
		var s = raw == null ? "" : raw.toString();
		try {
			if (target == int.class || target == Integer.class)
				return Integer.parseInt(s.trim());
			if (target == long.class || target == Long.class)
				return Long.parseLong(s.trim());
			if (target == double.class || target == Double.class)
				return Double.parseDouble(s.trim());
			if (target == boolean.class || target == Boolean.class)
				return parseBoolean(fnName, argIndex, s);
			if (target == String[].class)
				return parseStringArray(fnName, argIndex, s);
			if (target == Object.class)
				return s;
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			throw illegalArg("Function ''{0}'' arg {1}: cannot coerce ''{2}'' to {3}",
				fnName, argIndex, s, target.getSimpleName());
		}
		// Unrecognized target type: passthrough as String.
		return s;
	}

	/**
	 * Parse the explicit truthiness table. Throws {@link IllegalArgumentException}
	 * on inputs that don't match any TRUE or FALSE token.
	 */
	private static boolean parseBoolean(String fnName, int argIndex, String s) {
		var t = s.trim();
		if (t.equalsIgnoreCase("true") || t.equals("1") || t.equalsIgnoreCase("yes") || t.equalsIgnoreCase("on"))
			return true;
		if (t.isEmpty() || t.equalsIgnoreCase("false") || t.equals("0") || t.equalsIgnoreCase("no") || t.equalsIgnoreCase("off"))
			return false;
		throw illegalArg("Function ''{0}'' arg {1}: cannot coerce ''{2}'' to boolean (accepted: true/1/yes/on, false/0/no/off, empty)",
			fnName, argIndex, s);
	}

	/**
	 * Parse a JSON-array shortcut string into a {@code String[]}.
	 *
	 * <p>
	 * Inputs of the form {@code ["a","b","c"]} are parsed into 3 elements. The parsing is
	 * intentionally permissive (matches Juneau's JSON shortcut form): whitespace tolerated,
	 * single or double quotes accepted, simple {@code \\}-style escapes inside quoted strings
	 * recognized. Non-JSON-array input (no surrounding {@code [}/{@code ]}) is wrapped as a
	 * single-element array of the raw string.
	 *
	 * <p>
	 * This is intentionally a small inline parser — depending on the full Juneau JSON parser
	 * here would create a circular module dependency (juneau-commons → juneau-marshall).
	 */
	@SuppressWarnings({
		"java:S3776", // Cognitive complexity: simple inline JSON array parser.
	})
	private static String[] parseStringArray(String fnName, int argIndex, String s) {
		var t = s.trim();
		if (t.isEmpty())
			return new String[0];
		// Single-element wrap if not in array form.
		if (!(t.startsWith("[") && t.endsWith("]")))
			return new String[]{s};
		var body = t.substring(1, t.length() - 1).trim();
		if (body.isEmpty())
			return new String[0];
		var out = new ArrayList<String>();
		var i = 0;
		var len = body.length();
		while (i < len) {
			while (i < len && Character.isWhitespace(body.charAt(i)))
				i++;
			if (i >= len)
				break;
			var c = body.charAt(i);
			if (c == '"' || c == '\'') {
				var quote = c;
				i++;
				var sb = new StringBuilder();
				while (i < len) {
					var c2 = body.charAt(i);
					if (c2 == '\\' && i + 1 < len) {
						sb.append(body.charAt(i + 1));
						i += 2;
					} else if (c2 == quote) {
						i++;
						break;
					} else {
						sb.append(c2);
						i++;
					}
				}
				out.add(sb.toString());
			} else {
				var start = i;
				while (i < len && body.charAt(i) != ',')
					i++;
				out.add(body.substring(start, i).trim());
			}
			while (i < len && Character.isWhitespace(body.charAt(i)))
				i++;
			if (i < len) {
				if (body.charAt(i) != ',')
					throw illegalArg("Function ''{0}'' arg {1}: malformed JSON array near offset {2}",
						fnName, argIndex, i);
				i++;
			}
		}
		return out.toArray(new String[0]);
	}
}
