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
 * Arithmetic functions for the {@code #{...}} script catalog.
 *
 * <p>
 * All operations work on {@code double} so integer and decimal inputs compose naturally; the
 * result renders as a String. Integer-perfect results omit the decimal point (e.g.
 * {@code add(2, 3)} returns {@code "5"}, not {@code "5.0"}).
 */
public final class ArithmeticFunctions {

	private ArithmeticFunctions() {}

	/** All function classes in this category. */
	@SuppressWarnings({
		"unchecked", // Cast is safe: type verified by caller context.
		"java:S2386" // ALL is an immutable compile-time registry; exposed as an array for the cross-package/varargs functions(...) API, so visibility cannot be reduced.
	})
	public static final Class<? extends VarFunction>[] ALL = new Class[] {
		Add.class, Subtract.class, Multiply.class, Divide.class, Modulo.class,
		Min.class, Max.class, Abs.class
	};

	/** Render a double as a String, dropping the decimal portion when it's an exact integer. */
	static String render(double v) {
		if (Double.isNaN(v) || Double.isInfinite(v)) return String.valueOf(v);
		if (v == Math.floor(v) && Math.abs(v) < 1e18)
			return String.valueOf((long) v);
		return String.valueOf(v);
	}

	/** {@code #{add(a, b)}} — returns {@code a + b}. */
	public static class Add extends TypedFunction {
		@Override public String name() { return "add"; }
		public String invoke(double a, double b) { return render(a + b); }
	}

	/** {@code #{subtract(a, b)}} — returns {@code a - b}. */
	public static class Subtract extends TypedFunction {
		@Override public String name() { return "subtract"; }
		public String invoke(double a, double b) { return render(a - b); }
	}

	/** {@code #{multiply(a, b)}} — returns {@code a * b}. */
	public static class Multiply extends TypedFunction {
		@Override public String name() { return "multiply"; }
		public String invoke(double a, double b) { return render(a * b); }
	}

	/** {@code #{divide(a, b)}} — returns {@code a / b}. {@code b = 0} returns {@code Infinity} or {@code NaN} per IEEE 754. */
	public static class Divide extends TypedFunction {
		@Override public String name() { return "divide"; }
		public String invoke(double a, double b) { return render(a / b); }
	}

	/** {@code #{modulo(a, b)}} — returns {@code a % b}. */
	public static class Modulo extends TypedFunction {
		@Override public String name() { return "modulo"; }
		public String invoke(double a, double b) { return render(a % b); }
	}

	/** {@code #{min(a, b)}} — returns the smaller of two numbers. */
	public static class Min extends TypedFunction {
		@Override public String name() { return "min"; }
		public String invoke(double a, double b) { return render(Math.min(a, b)); }
	}

	/** {@code #{max(a, b)}} — returns the larger of two numbers. */
	public static class Max extends TypedFunction {
		@Override public String name() { return "max"; }
		public String invoke(double a, double b) { return render(Math.max(a, b)); }
	}

	/** {@code #{abs(a)}} — returns the absolute value of {@code a}. */
	public static class Abs extends TypedFunction {
		@Override public String name() { return "abs"; }
		public String invoke(double a) { return render(Math.abs(a)); }
	}
}
