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

import org.apache.juneau.commons.svl.*;

/**
 * Boolean-logic and comparison functions for the {@code #{...}} script catalog.
 *
 * <p>
 * Boolean coercion is explicit (no implicit truthiness). Comparison functions ({@code lt},
 * {@code gt}, etc.) compare numerically; {@code eq} / {@code neq} compare strings.
 */
@SuppressWarnings({
	"java:S1192" // Duplicate string literals are SVL operator names used in annotation/config keys; intentional
})
public final class BooleanFunctions {

	private BooleanFunctions() {}

	/** All function classes in this category. */
	@SuppressWarnings({
		"unchecked", // Cast is safe: type verified by caller context.
		"java:S2386" // ALL is an immutable compile-time registry; exposed as an array for the cross-package/varargs functions(...) API, so visibility cannot be reduced.
	})
	public static final Class<? extends VarFunction>[] ALL = new Class[] {
		And.class, Or.class, Not.class, Xor.class,
		Eq.class, Neq.class, Lt.class, Lte.class, Gt.class, Gte.class
	};

	/** {@code #{and(a, b, ...)}} — returns {@code "true"} only if all args are true. Variadic. */
	public static class And extends TypedFunction {
		@Override public String name() { return "and"; }
		public String invoke(String[] args) {
			for (var a : args)
				if (!parseBool(a)) return "false";
			return "true";
		}
	}

	/** {@code #{or(a, b, ...)}} — returns {@code "true"} if any arg is true. Variadic. */
	public static class Or extends TypedFunction {
		@Override public String name() { return "or"; }
		public String invoke(String[] args) {
			for (var a : args)
				if (parseBool(a)) return "true";
			return "false";
		}
	}

	/** {@code #{not(a)}} — returns the boolean negation of {@code a}. */
	public static class Not extends TypedFunction {
		@Override public String name() { return "not"; }
		public String invoke(boolean a) { return String.valueOf(!a); }
	}

	/** {@code #{xor(a, b)}} — returns {@code a XOR b}. */
	public static class Xor extends TypedFunction {
		@Override public String name() { return "xor"; }
		public String invoke(boolean a, boolean b) { return String.valueOf(a ^ b); }
	}

	/** {@code #{eq(a, b)}} — string-equality. Returns {@code "true"} or {@code "false"}. */
	public static class Eq extends TypedFunction {
		@Override public String name() { return "eq"; }
		public String invoke(String a, String b) { return String.valueOf(a == null ? b == null : a.equals(b)); }
	}

	/** {@code #{neq(a, b)}} — string-inequality. */
	public static class Neq extends TypedFunction {
		@Override public String name() { return "neq"; }
		public String invoke(String a, String b) { return String.valueOf(!(a == null ? b == null : a.equals(b))); }
	}

	/** {@code #{lt(a, b)}} — numeric less-than. */
	public static class Lt extends TypedFunction {
		@Override public String name() { return "lt"; }
		public String invoke(double a, double b) { return String.valueOf(a < b); }
	}

	/** {@code #{lte(a, b)}} — numeric less-than-or-equal. */
	public static class Lte extends TypedFunction {
		@Override public String name() { return "lte"; }
		public String invoke(double a, double b) { return String.valueOf(a <= b); }
	}

	/** {@code #{gt(a, b)}} — numeric greater-than. */
	public static class Gt extends TypedFunction {
		@Override public String name() { return "gt"; }
		public String invoke(double a, double b) { return String.valueOf(a > b); }
	}

	/** {@code #{gte(a, b)}} — numeric greater-than-or-equal. */
	public static class Gte extends TypedFunction {
		@Override public String name() { return "gte"; }
		public String invoke(double a, double b) { return String.valueOf(a >= b); }
	}

	/** Same truthiness table as {@link ArgCoercer}, exposed for variadic use in {@link And}/{@link Or}. */
	private static boolean parseBool(String s) {
		var t = s == null ? "" : s.trim();
		if (t.equalsIgnoreCase("true") || t.equals("1") || t.equalsIgnoreCase("yes") || t.equalsIgnoreCase("on"))
			return true;
		if (t.isEmpty() || t.equalsIgnoreCase("false") || t.equals("0") || t.equalsIgnoreCase("no") || t.equalsIgnoreCase("off"))
			return false;
		throw new IllegalArgumentException("cannot coerce '" + s + "' to boolean (accepted: true/1/yes/on, false/0/no/off, empty)");
	}
}
