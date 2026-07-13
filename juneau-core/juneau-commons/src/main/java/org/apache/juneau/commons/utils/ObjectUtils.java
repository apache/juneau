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
package org.apache.juneau.commons.utils;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.lang.*;

/**
 * Canonical home for object-level helpers: null/empty checks, equality, comparison,
 * coalescing, boolean folds, identity/stringify, small numeric helpers, and optionals.
 *
 * <p>Terse aliases for these methods live in {@link Shorts}.
 */
@SuppressWarnings({
	"java:S1118" // Utility class with static methods only.
})
public class ObjectUtils {

	private static final String ARG_values = "values";

	/** Constructor — this class is meant to be subclassed. */
	protected ObjectUtils() {}

	/** Returns <jk>true</jk> if the object is not <jk>null</jk>. */
	public static boolean isNotNull(Object o) { return o != null; }

	/** Returns <jk>true</jk> if the object is <jk>null</jk>. */
	public static <T> boolean isNull(T value) { return value == null; }

	/** Returns <jk>true</jk> if all specified values are <jk>null</jk> (or the array is <jk>null</jk>). */
	public static boolean isNull(Object...values) {
		if (values == null) return true;
		for (var v : values) if (v != null) return false;
		return true;
	}

	/** Returns <jk>null</jk> typed as the specified class (type-inference helper). */
	@SuppressWarnings({
		"java:S1172", // Parameter used for type inference only.
		"unused"      // Parameter used for type inference only.
	})
	public static <T> T nullObject(Class<T> type) { return null; }

	/** Null/array/annotation-aware equality. */
	public static <T> boolean equal(T o1, T o2) {
		if (o1 instanceof java.lang.annotation.Annotation o1a && o2 instanceof java.lang.annotation.Annotation o2a)
			return AnnotationUtils.equals(o1a, o2a);
		if (ClassUtils.isArray(o1) && ClassUtils.isArray(o2)) {
			var l1 = Array.getLength(o1);
			var l2 = Array.getLength(o2);
			if (l1 != l2) return false;
			for (var i = 0; i < l1; i++)
				if (notEqual(Array.get(o1, i), Array.get(o2, i))) return false;
			return true;
		}
		return Objects.equals(o1, o2);
	}

	/** String equality with optional case-insensitivity. */
	public static boolean equal(boolean caseInsensitive, String s1, String s2) {
		return caseInsensitive ? equalIgnoreCase(s1, s2) : equal(s1, s2);
	}

	/** Null-safe equality via a custom predicate. */
	public static <T,U> boolean equal(T o1, U o2, BiPredicate<T,U> test) {
		if (o1 == null) return o2 == null;
		if (o2 == null) return false;
		if (o1 == o2) return true;
		return test.test(o1, o2);
	}

	/** Returns <jk>true</jk> if the first argument equals any of the varargs. */
	@SafeVarargs
	public static <T> boolean equalsAny(T o1, T...o2) {
		if (o2 == null || o2.length == 0) return false;
		for (var o : o2) if (equal(o1, o)) return true;
		return false;
	}

	/** Case-insensitive equality of two objects' string forms. */
	public static boolean equalIgnoreCase(Object o1, Object o2) { return StringUtils.equalsIgnoreCase(o1, o2); }

	/** Inverse of {@link #equal(Object,Object)}. */
	public static <T> boolean notEqual(T o1, T o2) { return ! equal(o1, o2); }

	/** Inverse of {@link #equal(Object,Object,BiPredicate)}. */
	public static <T,U> boolean notEqual(T o1, U o2, BiPredicate<T,U> test) {
		if (o1 == null) return isNotNull(o2);
		if (o2 == null) return true;
		if (o1 == o2) return false;
		return ! test.test(o1, o2);
	}

	/** Null-tolerant natural-order compare (0 if types differ / not Comparable). */
	@SuppressWarnings({
		"unchecked",   // Type erasure requires unchecked casts.
		"java:S3740"   // Raw Comparable; parameterizing breaks compareTo.
	})
	public static int compare(Object o1, Object o2) {
		if (o1 == null) return o2 == null ? 0 : -1;
		if (o2 == null) return 1;
		if (equal(o1.getClass(), o2.getClass()) && o1 instanceof Comparable o12)
			return o12.compareTo(o2);
		return 0;
	}

	/**
	 * Returns <jk>true</jk> if o1 is less than o2.
	 *
	 * @param <T> The comparable type.
	 * @param o1 Object 1.
	 * @param o2 Object 2.
	 * @return <jk>true</jk> if o1 is less than o2.
	 */
	public static <T extends Comparable<T>> boolean lessThan(T o1, T o2) {
		if (o1 == null) return o2 != null;
		if (o2 == null) return false;
		return o1.compareTo(o2) < 0;
	}

	/**
	 * Returns <jk>true</jk> if o1 is less than or equal to o2.
	 *
	 * @param <T> The comparable type.
	 * @param o1 Object 1.
	 * @param o2 Object 2.
	 * @return <jk>true</jk> if o1 is less than or equal to o2.
	 */
	public static <T extends Comparable<T>> boolean lessThanOrEqual(T o1, T o2) {
		if (o1 == null) return true;
		if (o2 == null) return false;
		return o1.compareTo(o2) <= 0;
	}

	/**
	 * Returns <jk>true</jk> if o1 is greater than o2.
	 *
	 * @param <T> The comparable type.
	 * @param o1 Object 1.
	 * @param o2 Object 2.
	 * @return <jk>true</jk> if o1 is greater than o2.
	 */
	public static <T extends Comparable<T>> boolean greaterThan(T o1, T o2) {
		if (o1 == null) return false;
		if (o2 == null) return true;
		return o1.compareTo(o2) > 0;
	}

	/**
	 * Returns <jk>true</jk> if o1 is greater than or equal to o2.
	 *
	 * @param <T> The comparable type.
	 * @param o1 Object 1.
	 * @param o2 Object 2.
	 * @return <jk>true</jk> if o1 is greater than or equal to o2.
	 */
	public static <T extends Comparable<T>> boolean greaterThanOrEqual(T o1, T o2) {
		if (o1 == null) return o2 == null;
		if (o2 == null) return true;
		return o1.compareTo(o2) >= 0;
	}

	/**
	 * Returns the minimum of two comparable values.
	 *
	 * @param <T> The comparable type.
	 * @param o1 Value 1.
	 * @param o2 Value 2.
	 * @return The minimum value.
	 */
	public static <T extends Comparable<T>> T min(T o1, T o2) {
		if (o1 == null) return o2;
		if (o2 == null) return o1;
		return o1.compareTo(o2) <= 0 ? o1 : o2;
	}

	/**
	 * Returns the maximum of two comparable values.
	 *
	 * @param <T> The comparable type.
	 * @param o1 Value 1.
	 * @param o2 Value 2.
	 * @return The maximum value.
	 */
	public static <T extends Comparable<T>> T max(T o1, T o2) {
		if (o1 == null) return o2;
		if (o2 == null) return o1;
		return o1.compareTo(o2) >= 0 ? o1 : o2;
	}

	/** Returns the value if non-null, else the default (coalesce). */
	public static <T> T coalesce(T value, T defaultValue) { return isNotNull(value) ? value : defaultValue; }

	/** Returns the first non-null value, or <jk>null</jk>. */
	@SafeVarargs
	public static <T> T coalesce(T...t) {
		if (isNotNull(t)) for (var tt : t) if (isNotNull(tt)) return tt;
		return null;
	}

	/** Boolean AND-fold (vacuously true when empty). */
	public static boolean allTrue(boolean...values) {
		if (values == null || values.length == 0) return true;
		for (var v : values) if (! v) return false;
		return true;
	}

	/** Boolean OR-fold (false when empty). */
	public static boolean anyTrue(boolean...values) {
		if (values == null || values.length == 0) return false;
		for (var v : values) if (v) return true;
		return false;
	}

	/** Boolean negation. */
	public static boolean negate(boolean value) { return ! value; }

	/** Null-safe {@link Boolean#TRUE} check. */
	public static boolean isTrue(Object value) { return Boolean.TRUE.equals(value); }

	/** Null-safe {@link Boolean#FALSE} check. */
	public static boolean isFalse(Object value) { return Boolean.FALSE.equals(value); }

	/** Coerces an object to a boolean (false if null). */
	public static boolean toBoolean(Object val) {
		return optional(val).map(Object::toString).map(Boolean::valueOf).orElse(false);
	}

	/** Null-safe toString (returns <jk>null</jk> for <jk>null</jk>). */
	public static String stringify(Object val) { return val == null ? null : val.toString(); }

	/** "SimpleQualifiedName@identityHashCode" for the object (unwrapping Optionals). */
	public static String identity(Object o) {
		if (o instanceof Optional<?> opt) o = opt.orElse(null);
		if (o == null) return null;
		return ClassUtils.classNameSimpleQualified(o) + "@" + System.identityHashCode(o);
	}

	/** Unwraps a Supplier/Holder/Optional to its inner value. */
	public static Object unwrap(Object o) {
		if (o instanceof Supplier<?> o2) o = unwrap(o2.get());
		if (o instanceof Holder<?> o2) o = unwrap(o2.get());
		if (o instanceof Optional<?> o2) o = unwrap(o2.orElse(null));
		return o;
	}

	/** Absolute value across Number subtypes. */
	@SuppressWarnings({ "unchecked" // Type erasure requires unchecked cast for Number types.
	})
	public static <T extends Number> T abs(T value) {
		if (value == null) return null;
		if (value instanceof Integer) return (T)Integer.valueOf(Math.abs(value.intValue()));
		if (value instanceof Long) return (T)Long.valueOf(Math.abs(value.longValue()));
		if (value instanceof Double) return (T)Double.valueOf(Math.abs(value.doubleValue()));
		if (value instanceof Float) return (T)Float.valueOf(Math.abs(value.floatValue()));
		if (value instanceof Short) return (T)Short.valueOf((short)Math.abs(value.shortValue()));
		if (value instanceof Byte) return (T)Byte.valueOf((byte)Math.abs(value.byteValue()));
		return (T)Double.valueOf(Math.abs(value.doubleValue()));
	}

	/** Inclusive between. */
	public static boolean isBetween(int n, int lower, int higher) { return n >= lower && n <= higher; }

	/** True if the number is non-null and not -1. */
	public static <T extends Number> boolean isNotMinusOne(T value) { return isNotNull(value) && value.intValue() != -1; }

	/** Content/annotation/array-aware hash. */
	public static int hash(Object...values) {
		assertArgNotNull(ARG_values, values);
		return HashCode.of(values);
	}

	/** True if the object (String/Collection/Map/array/other) is null or empty. */
	public static boolean isEmpty(Object o) {
		if (o == null) return true;
		if (o instanceof Collection<?> o2) return o2.isEmpty();
		if (o instanceof Map<?,?> o2) return o2.isEmpty();
		if (ClassUtils.isArray(o)) return Array.getLength(o) == 0;
		return o.toString().isEmpty();
	}

	/** Inverse of {@link #isEmpty(Object)}. */
	public static boolean isNotEmpty(Object value) {
		if (value == null) return false;
		if (value instanceof CharSequence value2) return ! value2.isEmpty();
		if (value instanceof Collection<?> value2) return ! value2.isEmpty();
		if (value instanceof Map<?,?> value2) return ! value2.isEmpty();
		if (ClassUtils.isArray(value)) return Array.getLength(value) > 0;
		return isNotEmpty(stringify(value));
	}

	/** {@link Optional#ofNullable(Object)} shorthand. */
	public static <T> Optional<T> optional(T t) { return Optional.ofNullable(t); }

	/** {@link Optional#empty()} shorthand. */
	public static <T> Optional<T> emptyOptional() { return Optional.empty(); }

	/** {@code optional.orElse(defaultValue)} shorthand. */
	public static <T> T orElse(Optional<T> optional, T defaultValue) { return optional.orElse(defaultValue); }

	/** Asserts non-null and returns the value. */
	public static <T> T requireNonNull(T value) { return Objects.requireNonNull(value); }

	/** Null-safe size (0 for null; length/size for String/Collection/Map/array; else 1). */
	public static int size(Object o) {
		if (o == null) return 0;
		if (o instanceof CharSequence o2) return o2.length();
		if (o instanceof Collection<?> o2) return o2.size();
		if (o instanceof Map<?,?> o2) return o2.size();
		if (ClassUtils.isArray(o)) return Array.getLength(o);
		return 1;
	}
}
