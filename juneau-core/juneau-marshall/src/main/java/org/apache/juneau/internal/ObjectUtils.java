// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.internal;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.reflect.*;

/**
 * Various generic object utility methods.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class ObjectUtils {

	/**
	 * If the specified object is an instance of the specified class, casts it to that type.
	 *
	 * @param <T> The class to cast to.
	 * @param o The object to cast.
	 * @param c The class to cast to.
	 * @return The cast object, or <jk>null</jk> if the object wasn't an instance of the specified class.
	 */
	public static <T> T castOrNull(Object o, Class<T> c) {
		if (c.isInstance(o))
			return c.cast(o);
		return null;
	}

	/**
	 * Compares two objects for equality.
	 *
	 * <p>
	 * Nulls are always considered less-than unless both are null.
	 *
	 * @param o1 Object 1.
	 * @param o2 Object 2.
	 * @return
	 * 	<c>-1</c>, <c>0</c>, or <c>1</c> if <c>o1</c> is less-than, equal, or greater-than <c>o2</c>.
	 *	<br><c>0</c> if objects are not of the same type or do not implement the {@link Comparable} interface.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static int compare(Object o1, Object o2) {
		if (o1 == null) {
			if (o2 == null)
				return 0;
			return -1;
		} else if (o2 == null) {
			return 1;
		}

		if (o1.getClass() == o2.getClass() && o1 instanceof Comparable)
			return ((Comparable)o1).compareTo(o2);

		return 0;
	}

	/**
	 * Compare two integers numerically.
	 *
	 * @param i1 Integer #1
	 * @param i2 Integer #2
	 * @return
	 * 	The value <c>0</c> if Integer #1 is equal to Integer #2; a value less than <c>0</c> if
	 * 	Integer #1 numerically less than Integer #2; and a value greater than <c>0</c> if Integer #1 is
	 * 	numerically greater than Integer #2 (signed comparison).
	 */
	public static final int compare(int i1, int i2) {
		return (i1<i2 ? -1 : (i1==i2 ? 0 : 1));
	}

	/**
	 * Tests two objects for equality, gracefully handling nulls.
	 *
	 * @param <T> Object 1 type.
	 * @param <U> Object 2 type.
	 * @param o1 Object 1.
	 * @param o2 Object 2.
	 * @param test The test to use for equality.
	 * @return <jk>true</jk> if both objects are equal based on the test.
	 */
	public static <T,U> boolean eq(T o1, U o2, BiPredicate<T,U> test) {
		if (o1 == null)
			return o2 == null;
		if (o2 == null)
			return false;
		if (o1 == o2)
			return true;
		return test.test(o1, o2);
	}

	/**
	 * Tests two objects for equality, gracefully handling nulls and arrays.
	 *
	 * @param <T> The value types.
	 * @param o1 Object 1.
	 * @param o2 Object 2.
	 * @return <jk>true</jk> if both objects are equal based on the {@link Object#equals(Object)} method.
	 */
	public static <T> boolean eq(T o1, T o2) {
		if (isArray(o1) && isArray(o2)) {
			int l1 = Array.getLength(o1), l2 = Array.getLength(o2);
			if (l1 != l2)
				return false;
			for (int i = 0; i < l1; i++)
				if (! eq(Array.get(o1, i), Array.get(o2, i)))
					return false;
			return true;
		}
		return Objects.equals(o1, o2);
	}

	/**
	 * Tests two arrays for equality, gracefully handling nulls.
	 *
	 * @param <T> The value types.
	 * @param o1 Array 1.
	 * @param o2 Array 2.
	 * @return <jk>true</jk> if both arrays are equal based on the {@link Object#equals(Object)} method on each element.
	 */
	public static <T> boolean eq(T[] o1, T[] o2) {
		if (o1 == null || o2 == null) {
			if (o1 != null || o2 != null)
				return false;
			return true;
		}
		if (o1.length != o2.length)
			return false;
		for (int i = 0; i < o1.length; i++)
			if (! eq(o1[i], o2[i]))
				return false;
		return true;

	}

	/**
	 * Tests two objects for inequality, gracefully handling nulls.
	 *
	 * @param <T> Object 1 type.
	 * @param <U> Object 2 type.
	 * @param o1 Object 1.
	 * @param o2 Object 2.
	 * @param test The test to use for equality.
	 * @return <jk>false</jk> if both objects are equal based on the test.
	 */
	public static <T,U> boolean ne(T o1, U o2, BiPredicate<T,U> test) {
		if (o1 == null)
			return o2 != null;
		if (o2 == null)
			return true;
		if (o1 == o2)
			return false;
		return ! test.test(o1, o2);
	}

	/**
	 * Tests two objects for equality, gracefully handling nulls and arrays.
	 *
	 * @param o1 Object 1.
	 * @param o2 Object 2.
	 * @return <jk>false</jk> if both objects are equal based on the {@link Object#equals(Object)} method.
	 */
	public static boolean ne(Object o1, Object o2) {
		return ! eq(o1, o2);
	}

	private static boolean isArray(Object o) {
		return o != null && o.getClass().isArray();
	}

	/**
	 * If the specified object is a {@link Supplier} or {@link Value}, returns the inner value, otherwise the same value.
	 *
	 * @param o The object to unwrap.
	 * @return The unwrapped object.
	 */
	public static Object unwrap(Object o) {
		while (o instanceof Supplier)
			o = ((Supplier<?>)o).get();
		while (o instanceof Value)
			o = ((Value<?>)o).get();
		return o;
	}

	/**
	 * Returns <jk>true</jk> if the specified object is empty.
	 *
	 * <p>
	 * Return <jk>true</jk> if the value is any of the following:
	 * <ul>
	 * 	<li><jk>null</jk>
	 * 	<li>An empty Collection
	 * 	<li>An empty Map
	 * 	<li>An empty array
	 * 	<li>An empty CharSequence
	 * 	<li>An empty String when serialized to a string using {@link Object#toString()}.
	 * </ul>
	 *
	 * @param o The object to test.
	 * @return <jk>true</jk> if the specified object is empty.
	 */
	@SuppressWarnings("rawtypes")
	public static boolean isEmpty(Object o) {
		if (o == null)
			return true;
		if (o instanceof Collection)
			return ((Collection)o).isEmpty();
		if (o instanceof Map)
			return ((Map)o).isEmpty();
		if (o.getClass().isArray())
			return (Array.getLength(o) == 0);
		return o.toString().isEmpty();
	}

	/**
	 * Returns the first non-null value in the specified array
	 *
	 * @param <T> The value types.
	 * @param t The values to check.
	 * @return The first non-null value, or <jk>null</jk> if the array is null or empty or contains only <jk>null</jk> values.
	 */
	@SafeVarargs
	public static <T> T firstNonNull(T... t) {
		if (t != null)
			for (T tt : t)
				if (tt != null)
					return tt;
		return null;
	}

	/**
	 * Casts an object to a specific type if it's an instance of that type.
	 *
	 * @param <T> The type to cast to.
	 * @param c The type to cast to.
	 * @param o The object to cast to.
	 * @return The cast object, or <jk>null</jk> if the object wasn't the specified type.
	 */
	public static <T> T cast(Class<T> c, Object o) {
		return o != null && c.isInstance(o) ? c.cast(o) : null;
	}

	/**
	 * Converts the specified object into an identifiable string of the form "Class[identityHashCode]"
	 * @param o The object to convert to a string.
	 * @return An identity string.
	 */
	public static String identity(Object o) {
		if (o instanceof Optional)
			o = ((Optional<?>)o).orElse(null);
		if (o == null)
			return null;
		return ClassInfo.of(o).getShortName() + "@" + System.identityHashCode(o);
	}

	private static final ConcurrentHashMap<Class<?>,Map<String,MethodInfo>> PROPERTIES_METHODS = new ConcurrentHashMap<>();

	/**
	 * Searches for all <c>properties()</c> methods on the specified object and creates a combine map of them.
	 *
	 * @param o The object to return a property map of.
	 * @return A new property map.
	 */
	public static JsonMap toPropertyMap(Object o) {
		if (o == null)
			return null;
		Map<String,MethodInfo> methods = PROPERTIES_METHODS.get(o.getClass());
		if (methods == null) {
			ClassInfo ci = ClassInfo.of(o);
			Map<String,MethodInfo> methods2 = new LinkedHashMap<>();
			do {
				String cname = ci.getShortName();
				MethodInfo mi = ci.getDeclaredMethod(x -> x.hasName("properties"));
				if (mi != null)
					methods2.put(cname, mi.accessible());
				ci = ci.getSuperclass();
			} while (ci != null);
			methods = methods2;
			PROPERTIES_METHODS.put(o.getClass(), methods);
		}
		JsonMap m = JsonMap.create().append("id", identity(o));
		methods.forEach((k,v) -> m.put(k, v.invoke(o)));
		return m;
	}

	/**
	 * Returns <jk>true</jk> if the specified object is not <jk>null</jk>.
	 *
	 * @param <T> The value type.
	 * @param value The value being checked.
	 * @return <jk>true</jk> if the specified object is not <jk>null</jk>.
	 */
	public static <T> boolean isNotNull(T value) {
		return value != null;
	}

	/**
	 * Returns <jk>true</jk> if the specified boolean is not <jk>null</jk> and is <jk>true</jk>.
	 *
	 * @param value The value being checked.
	 * @return <jk>true</jk> if the specified boolean is not <jk>null</jk> and is <jk>true</jk>.
	 */
	public static boolean isTrue(Boolean value) {
		return value != null && value;
	}

	/**
	 * Returns <jk>true</jk> if the specified number is not <jk>null</jk> and not <c>-1</c>.
	 *
	 * @param <T> The value types.
	 * @param value The value being checked.
	 * @return <jk>true</jk> if the specified number is not <jk>null</jk> and not <c>-1</c>.
	 */
	public static <T extends Number> boolean isNotMinusOne(T value) {
		return value != null && value.intValue() != -1;
	}

	/**
	 * Returns <jk>true</jk> if the specified object is not <jk>null</jk> and not empty.
	 *
	 * Works on any of the following data types:  String, CharSequence, Collection, Map, array.
	 * All other types are stringified and then checked as a String.
	 *
	 * @param value The value being checked.
	 * @return <jk>true</jk> if the specified object is not <jk>null</jk> and not empty.
	 */
	public static boolean isNotEmpty(Object value) {
		if (value == null) return false;
		if (value instanceof CharSequence) return CharSequence.class.cast(value).length() > 0;
		if (value instanceof Collection) return ! Collection.class.cast(value).isEmpty();
		if (value instanceof Map) return ! Map.class.cast(value).isEmpty();
		if (value.getClass().isArray()) return Array.getLength(value) > 0;
		return StringUtils.isNotEmpty(StringUtils.stringify(value));
	}
}
