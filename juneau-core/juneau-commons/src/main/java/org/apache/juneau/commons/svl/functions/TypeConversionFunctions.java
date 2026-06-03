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
 * Type-conversion functions for the {@code #{...}} script catalog.
 *
 * <p>
 * Useful as bridges between SVL's string-typed default and the native typed args expected by
 * other functions or by application code. All four functions return their typed value rendered
 * back as a String for SVL interop — the actual coercion happens via {@link ArgCoercer} on the
 * way in.
 */
public final class TypeConversionFunctions {

	private TypeConversionFunctions() {}

	/** All function classes in this category. */
	@SuppressWarnings({
		"unchecked" // Cast is safe: type verified by caller context.
	})
	public static final Class<? extends VarFunction>[] ALL = new Class[] {
		ToInt.class, ToLong.class, ToDouble.class, ToBool.class
	};

	/** {@code #{toInt(s)}} — coerces {@code s} to {@code int} and renders as a String. */
	public static class ToInt extends TypedFunction {
		@Override public String name() { return "toInt"; }
		public String invoke(int x) { return String.valueOf(x); }
	}

	/** {@code #{toLong(s)}} — coerces {@code s} to {@code long} and renders as a String. */
	public static class ToLong extends TypedFunction {
		@Override public String name() { return "toLong"; }
		public String invoke(long x) { return String.valueOf(x); }
	}

	/** {@code #{toDouble(s)}} — coerces {@code s} to {@code double} and renders as a String. */
	public static class ToDouble extends TypedFunction {
		@Override public String name() { return "toDouble"; }
		public String invoke(double x) { return String.valueOf(x); }
	}

	/** {@code #{toBool(s)}} — coerces {@code s} to {@code boolean} and renders as a String. */
	public static class ToBool extends TypedFunction {
		@Override public String name() { return "toBool"; }
		public String invoke(boolean x) { return String.valueOf(x); }
	}
}
