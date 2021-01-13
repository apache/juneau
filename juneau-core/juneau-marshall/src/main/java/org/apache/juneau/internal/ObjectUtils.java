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
import java.util.function.*;

import org.apache.juneau.reflect.*;
import org.apache.juneau.utils.*;

/**
 * Various generic object utility methods.
 */
public class ObjectUtils {

	/**
	 * Returns the enum names for the specified enum class.
	 *
	 * @param c The enum class.
	 * @return A modifiable list of all names for that class.
	 */
	@SuppressWarnings("unchecked")
	public static Enum<?>[] getEnumConstants(Class<?> c) {
		return ((Class<Enum<?>>)c).getEnumConstants();
	}

	/**
	 * If the specified object is an instance of the specified class, casts it to that type.
	 *
	 * @param o The object to cast.
	 * @param c The class to cast to.
	 * @return The cast object, or <jk>null</jk> if the object wasn't an instance of the specified class.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T castOrNull(Object o, Class<T> c) {
		if (c.isInstance(o))
			return (T)o;
		return null;
	}

	/**
	 * Returns the first non-zero value in the list of ints.
	 *
	 * @param ints The ints to check.
	 * @return The first non-zero value, or <c>0</c> if they were all zero.
	 */
	public static int firstNonZero(int...ints) {
		for (int i : ints)
			if (i != 0)
				return i;
		return 0;
	}

	/**
	 * Returns the first non-empty value in the list of objects.
	 *
	 * @param o The objects to check.
	 * @return The first object whose call to {@link ObjectUtils#isEmpty(Object)} returns <jk>false</jk>, otherwise <jk>null</jk>.
	 */
	@SafeVarargs
	public static <T> T firstNonEmpty(T...o) {
		for (T oo : o)
			if (! ObjectUtils.isEmpty(oo))
				return oo;
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
	 * @param o1 Object 1.
	 * @param o2 Object 2.
	 * @return <jk>true</jk> if both objects are equal based on the {@link Object#equals(Object)} method.
	 */
	public static boolean eq(Object o1, Object o2) {
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

	/**
	 * Calculates the hashcode for the specified object.
	 *
	 * <p>
	 * Unlike just calling {@link Object#hashCode()}, this method calculates hashsums of arrays by using the contents
	 * of the array instead of the hashsum of the array itself.
	 *
	 * @param o The object to calculate a hashsum on.
	 * @return The hashsum.
	 */
	public static int hashCode(Object o) {
		if (o == null)
			return 0;
		if (isArray(o)) {
			int x = 1;
			for (int i = 0; i < Array.getLength(o); i++)
				x = 31 * x + hashCode(Array.get(o, i));
			return x;
		}
		if (isCollection(o)) {
			int x = 1;
			for (Object o2 : (Collection<?>)o)
				x = 31 * x + hashCode(o2);
			return x;
		}
		if (isMap(o)) {
			int x = 1;
			for (Map.Entry<?,?> o2 : ((Map<?,?>)o).entrySet())
				x = 31 * x + (hashCode(o2.getKey()) ^ hashCode(o2.getValue()));
			return x;
		}
		return o.hashCode();
	}

	private static boolean isArray(Object o) {
		return o != null && o.getClass().isArray();
	}

	private static boolean isCollection(Object o) {
		return o != null && o.getClass().isAssignableFrom(Collection.class);
	}

	private static boolean isMap(Object o) {
		return o != null && o.getClass().isAssignableFrom(Map.class);
	}

	/**
	 * If the specified object is a {@link Supplier} or {@link Mutable}, returns the inner value, otherwise the same value.
	 *
	 * @param o The object to unwrap.
	 * @return The unwrapped object.
	 */
	public static Object unwrap(Object o) {
		while (o instanceof Supplier)
			o = ((Supplier<?>)o).get();
		while (o instanceof Mutable)
			o = ((Mutable<?>)o).get();
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
	 * Returns <jk>true</jk> if the specified object is not empty.
	 *
	 * <p>
	 * Return <jk>false</jk> if the value is any of the following:
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
	public static boolean isNotEmpty(Object o) {
		return ! isEmpty(o);
	}

	/**
	 * Returns the first non-null value in the specified array
	 *
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
}
