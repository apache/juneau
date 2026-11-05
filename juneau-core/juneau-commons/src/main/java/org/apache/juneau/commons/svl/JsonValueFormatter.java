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

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.svl.functions.*;

/**
 * Default formatter that turns the typed result of a {@code #{...}} expression into text.
 *
 * <p>
 * <jk>null</jk> becomes the empty string; {@link CharSequence} values are emitted as-is (bare, unquoted); numbers and
 * booleans use {@code toString} (integral doubles drop the {@code .0}); a {@link Structure} uses its plain-text
 * {@code toString()}; collections, maps and arrays are written as JSON (keys quoted, strings quoted only inside a
 * container, <jk>null</jk> as {@code null}).  Any other object inside a container is written as its quoted
 * {@code String.valueOf} (there is no bean introspection here).
 *
 * <p>
 * Nesting is capped at {@link #DEFAULT_MAX_DEPTH}, each container at the configured element limit, and the whole
 * call at {@link #MAX_NODES} written values (so a shared acyclic DAG cannot fan out exponentially).  A container that
 * contains itself (a true cycle on the current path) is reported as a cycle.  Beyond any limit the output is truncated
 * with a single {@code "..."} marker in lenient mode, or a {@link VarResolverException} is thrown in strict mode;
 * nothing here can overflow the stack.
 *
 * <p>
 * JSON has no tokens for {@code NaN} or the infinities; inside containers they are written as the quoted strings
 * {@code "NaN"}, {@code "Infinity"} and {@code "-Infinity"}.  A double or float that is integral is written without the
 * decimal point, so {@code -0.0} is written as {@code 0}.  The characters U+007F, U+2028 and U+2029 and all control
 * characters below U+0020 are escaped as {@code \\uXXXX}; lone surrogates are written as-is.
 */
public class JsonValueFormatter implements Function<Object,String> {

	/** Default maximum container nesting depth. */
	public static final int DEFAULT_MAX_DEPTH = 32;

	/** Maximum number of values written by one call. */
	public static final int MAX_NODES = 100_000;

	private final int maxSize;
	private final boolean strict;

	/** Constructor using the default collection limit, lenient. */
	public JsonValueFormatter() {
		this(VarResolver.DEFAULT_MAX_COLLECTION_SIZE, false);
	}

	/**
	 * Constructor.
	 *
	 * @param maxSize Maximum elements written per container.
	 * @param strict <jk>true</jk> to throw instead of truncating when a limit is exceeded.
	 */
	public JsonValueFormatter(int maxSize, boolean strict) {
		this.maxSize = maxSize;
		this.strict = strict;
	}

	/** Per-call state. */
	private static final class State {
		int nodes;
		boolean stop;
		final Set<Object> path = Collections.newSetFromMap(new IdentityHashMap<>());
	}

	@Override
	public String apply(Object o) {
		if (o == null)
			return "";
		if (o instanceof CharSequence || o instanceof Structure)
			return o.toString();
		if (o instanceof Number || o instanceof Boolean)
			return scalar(o);
		if (isContainer(o)) {
			var sb = new StringBuilder();
			write(sb, o, 0, new State());
			return sb.toString();
		}
		return String.valueOf(o);
	}

	private static boolean isContainer(Object o) {
		return o instanceof Collection || o instanceof Map || o.getClass().isArray();
	}

	private static String scalar(Object o) {
		if (o instanceof Double d)
			return ArithmeticFunctions.render(d);
		if (o instanceof Float f) {
			if (f.isNaN() || f.isInfinite() || f != Math.floor(f))
				return Float.toString(f);
			return ArithmeticFunctions.render(f.doubleValue());
		}
		return o.toString();
	}

	private static boolean nonFinite(Object o) {
		return (o instanceof Double d && (d.isNaN() || d.isInfinite())) || (o instanceof Float f && (f.isNaN() || f.isInfinite()));
	}

	/** Reports a limit: throws in strict mode, otherwise writes one marker and stops all further output. */
	private void limit(StringBuilder sb, String what, boolean first, boolean map, State st) {
		if (strict)
			throw new VarResolverException("Value formatter %s limit exceeded", what);
		if (! first)
			sb.append(',');
		sb.append(map ? "\"...\":true" : "\"...\"");
		st.stop = true;
	}

	private void write(StringBuilder sb, Object o, int depth, State st) {
		if (st.stop)
			return;
		if (++st.nodes > MAX_NODES) {
			limit(sb, "node", true, false, st);
			return;
		}
		if (o == null) {
			sb.append("null");
		} else if (nonFinite(o)) {
			quote(sb, scalar(o));
		} else if (o instanceof Number || o instanceof Boolean) {
			sb.append(scalar(o));
		} else if (isContainer(o)) {
			if (depth >= DEFAULT_MAX_DEPTH) {
				limit(sb, "depth", true, false, st);
			} else if (! st.path.add(o)) {
				if (strict)
					throw new VarResolverException("Value formatter found a cyclic structure");
				sb.append("\"...\"");
			} else {
				try {
					writeContainer(sb, o, depth, st);
				} finally {
					st.path.remove(o);
				}
			}
		} else {
			quote(sb, String.valueOf(o));
		}
	}

	private void writeContainer(StringBuilder sb, Object o, int depth, State st) {
		if (o instanceof Map<?,?> m) {
			sb.append('{');
			int i = 0;
			for (var e : m.entrySet()) {
				if (st.stop)
					break;
				if (i >= maxSize) {
					limit(sb, "size", i == 0, true, st);
					break;
				}
				if (i++ > 0) sb.append(',');
				quote(sb, String.valueOf(e.getKey()));
				sb.append(':');
				write(sb, e.getValue(), depth + 1, st);
			}
			sb.append('}');
		} else {
			sb.append('[');
			var it = o instanceof Collection<?> c ? c.iterator() : arrayIterator(o);
			int i = 0;
			while (it.hasNext() && ! st.stop) {
				if (i >= maxSize) {
					limit(sb, "size", i == 0, false, st);
					break;
				}
				if (i++ > 0) sb.append(',');
				write(sb, it.next(), depth + 1, st);
			}
			sb.append(']');
		}
	}

	private static Iterator<Object> arrayIterator(Object array) {
		int n = Array.getLength(array);
		return new Iterator<>() {
			int i;
			@Override public boolean hasNext() { return i < n; }
			@Override public Object next() { return Array.get(array, i++); }
		};
	}

	private static void quote(StringBuilder sb, String s) {
		sb.append('"');
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '"': sb.append("\\\""); break;
				case '\\': sb.append("\\\\"); break;
				case '\n': sb.append("\\n"); break;
				case '\r': sb.append("\\r"); break;
				case '\t': sb.append("\\t"); break;
				case '\b': sb.append("\\b"); break;
				case '\f': sb.append("\\f"); break;
				default:
					if (c < 0x20 || c == 0x7f || c == 0x2028 || c == 0x2029)
						sb.append(String.format("\\u%04x", (int)c));
					else
						sb.append(c);
			}
		}
		sb.append('"');
	}
}
