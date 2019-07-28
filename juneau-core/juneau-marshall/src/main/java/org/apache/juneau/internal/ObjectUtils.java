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

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.transform.*;

/**
 * Utility class for efficiently converting objects between types.
 *
 * <p>
 * If the value isn't an instance of the specified type, then converts the value if possible.
 *
 * <p>
 * The following conversions are valid:
 * <table class='styled'>
 * 	<tr><th>Convert to type</th><th>Valid input value types</th><th>Notes</th></tr>
 * 	<tr>
 * 		<td>
 * 			A class that is the normal type of a registered {@link PojoSwap}.
 * 		</td>
 * 		<td>
 * 			A value whose class matches the transformed type of that registered {@link PojoSwap}.
 * 		</td>
 * 		<td>&nbsp;</td>
 * 	</tr>
 * 	<tr>
 * 		<td>
 * 			A class that is the transformed type of a registered {@link PojoSwap}.
 * 		</td>
 * 		<td>
 * 			A value whose class matches the normal type of that registered {@link PojoSwap}.
 * 		</td>
 * 		<td>&nbsp;</td>
 * 	</tr>
 * 	<tr>
 * 		<td>
 * 			{@code Number} (e.g. {@code Integer}, {@code Short}, {@code Float},...)
 * 			<br><code>Number.<jsf>TYPE</jsf></code> (e.g. <code>Integer.<jsf>TYPE</jsf></code>,
 * 			<code>Short.<jsf>TYPE</jsf></code>, <code>Float.<jsf>TYPE</jsf></code>,...)
 * 		</td>
 * 		<td>
 * 			{@code Number}, {@code String}, <jk>null</jk>
 * 		</td>
 * 		<td>
 * 			For primitive {@code TYPES}, <jk>null</jk> returns the JVM default value for that type.
 * 		</td>
 * 	</tr>
 * 	<tr>
 * 		<td>
 * 			{@code Map} (e.g. {@code Map}, {@code HashMap}, {@code TreeMap}, {@code ObjectMap})
 * 		</td>
 * 		<td>
 * 			{@code Map}
 * 		</td>
 * 		<td>
 * 			If {@code Map} is not constructible, a {@code ObjectMap} is created.
 * 		</td>
 * 	</tr>
 * 	<tr>
 * 		<td>
 * 			<c>Collection</c> (e.g. <c>List</c>, <c>LinkedList</c>, <c>HashSet</c>, <c>ObjectList</c>)
 * 		</td>
 * 		<td>
 * 			<c>Collection&lt;Object&gt;</c>
 * 			<br><c>Object[]</c>
 * 		</td>
 * 		<td>
 * 			If <c>Collection</c> is not constructible, a <c>ObjectList</c> is created.
 * 		</td>
 * 	</tr>
 * 	<tr>
 * 		<td>
 * 			<c>X[]</c> (array of any type X)
 * 		</td>
 * 		<td>
 * 			<c>List&lt;X&gt;</c>
 * 		</td>
 * 		<td>&nbsp;</td>
 * 	</tr>
 * 	<tr>
 * 		<td>
 * 			<c>X[][]</c> (multi-dimensional arrays)
 * 		</td>
 * 		<td>
 * 			<c>List&lt;List&lt;X&gt;&gt;</c>
 * 			<br><c>List&lt;X[]&gt;</c>
 * 			<br><c> List[]&lt;X&gt;</c>
 * 		</td>
 * 		<td>&nbsp;</td>
 * 	</tr>
 * 	<tr>
 * 		<td>
 * 			<c>Enum</c>
 * 		</td>
 * 		<td>
 * 			<c>String</c>
 * 		</td>
 * 		<td>&nbsp;</td>
 * 	</tr>
 * 	<tr>
 * 		<td>
 * 			Bean
 * 		</td>
 * 		<td>
 * 			<c>Map</c>
 * 		</td>
 * 		<td>&nbsp;</td>
 * 	</tr>
 * 	<tr>
 * 		<td>
 * 			<c>String</c>
 * 		</td>
 * 		<td>
 * 			Anything
 * 		</td>
 * 		<td>
 * 			Arrays are converted to JSON arrays
 * 		</td>
 * 	</tr>
 * 	<tr>
 * 		<td>
 * 			Anything with one of the following methods:
 * 			<br><code><jk>public static</jk> T fromString(String)</code>
 * 			<br><code><jk>public static</jk> T valueOf(String)</code>
 * 			<br><code><jk>public</jk> T(String)</code>
 * 		</td>
 * 		<td>
 * 			<c>String</c>
 * 		</td>
 * 		<td>
 * 			<br>
 * 		</td>
 * 	</tr>
 * </table>
 */
public final class ObjectUtils {

	// Session objects are usually not thread safe, but we're not using any feature
	// of bean sessions that would cause thread safety issues.
	private static final BeanSession session = BeanContext.DEFAULT.createSession();

	/**
	 * Converts the specified object to the specified type.
	 *
	 * @param <T> The class type to convert the value to.
	 * @param value The value to convert.
	 * @param type The class type to convert the value to.
	 * @throws InvalidDataConversionException If the specified value cannot be converted to the specified type.
	 * @return The converted value.
	 */
	public static <T> T toType(Object value, Class<T> type) {
		return session.convertToType(value, type);
	}


	/**
	 * Converts the specified object to the specified type.
	 *
	 * @param <T> The class type to convert the value to.
	 * @param value The value to convert.
	 * @param type The class type to convert the value to.
	 * @param args The type arguments.
	 * @throws InvalidDataConversionException If the specified value cannot be converted to the specified type.
	 * @return The converted value.
	 */
	public static <T> T toType(Object value, Class<T> type, Type...args) {
		return session.convertToType(value, type, args);
	}

	/**
	 * Returns <jk>true</jk> if the specified objects are equal.
	 *
	 * <p>
	 * Gracefully handles <jk>null</jk>s.
	 *
	 * @param o1 Object #1
	 * @param o2 Object #2
	 * @return <jk>true</jk> if the objects are equal or both <jk>null</jk>.
	 */
	public static boolean equals(Object o1, Object o2) {
		if (o1 == null && o2 == null)
			return true;
		if (o1 == null || o2 == null)
			return false;
		return o1.equals(o2);
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
	 * Converts an object to a Boolean.
	 *
	 * @param o The object to convert.
	 * @return The converted object.
	 */
	public static Boolean toBoolean(Object o) {
		return toType(o, Boolean.class);
	}

	/**
	 * Converts an object to an Integer.
	 *
	 * @param o The object to convert.
	 * @return The converted object.
	 */
	public static Integer toInteger(Object o) {
		return toType(o, Integer.class);
	}

	/**
	 * Converts an object to a Number.
	 *
	 * @param o The object to convert.
	 * @return The converted object.
	 */
	public static Number toNumber(Object o) {
		if (o == null)
			return null;
		if (o instanceof Number)
			return (Number)o;
		try {
			return StringUtils.parseNumber(o.toString(), null);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

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
	 * @return The first object whose call to {@link #isEmpty(Object)} returns <jk>false</jk>, otherwise <jk>null</jk>.
	 */
	@SafeVarargs
	public static <T> T firstNonEmpty(T...o) {
		for (T oo : o)
			if (! isEmpty(oo))
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
}
