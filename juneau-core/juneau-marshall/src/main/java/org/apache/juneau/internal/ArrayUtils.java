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

import static org.apache.juneau.common.internal.ArgUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.common.internal.*;

/**
 * Quick and dirty utilities for working with arrays.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public final class ArrayUtils {

	/**
	 * Appends one or more elements to an array.
	 *
	 * @param <T> The element type.
	 * @param array The array to append to.
	 * @param newElements The new elements to append to the array.
	 * @return A new array with the specified elements appended.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] append(T[] array, T...newElements) {
		if (array == null)
			return newElements;
		if (newElements.length == 0)
			return array;
		T[] a = (T[])Array.newInstance(array.getClass().getComponentType(), array.length + newElements.length);
		for (int i = 0; i < array.length; i++)
			a[i] = array[i];
		for (int i = 0; i < newElements.length; i++)
			a[i+array.length] = newElements[i];
		return a;
	}

	/**
	 * Combine an arbitrary number of arrays into a single array.
	 *
	 * @param <E> The element type.
	 * @param arrays Collection of arrays to combine.
	 * @return A new combined array, or <jk>null</jk> if all arrays are <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	public static <E> E[] combine(E[]...arrays) {
		assertArgNotNull("arrays", arrays);
		int l = 0;
		E[] a1 = null;
		for (E[] a : arrays) {
			if (a1 == null && a != null)
				a1 = a;
			l += (a == null ? 0 : a.length);
		}
		if (a1 == null)
			return null;
		E[] a = (E[])Array.newInstance(a1.getClass().getComponentType(), l);
		int i = 0;
		for (E[] aa : arrays)
			if (aa != null)
				for (E t : aa)
					a[i++] = t;
		return a;
	}

	/**
	 * Converts the specified array to a <c>Set</c>.
	 *
	 * <p>
	 * The order of the entries in the set are the same as the array.
	 *
	 * @param <T> The entry type of the array.
	 * @param array The array being wrapped in a <c>Set</c> interface.
	 * @return The new set.
	 */
	public static <T> Set<T> asSet(final T[] array) {
		assertArgNotNull("array", array);
		return new AbstractSet<>() {

			@Override /* Set */
			public Iterator<T> iterator() {
				return new Iterator<>() {
					int i = 0;

					@Override /* Iterator */
					public boolean hasNext() {
						return i < array.length;
					}

					@Override /* Iterator */
					public T next() {
						if (i >= array.length)
							throw new NoSuchElementException();
						T t = array[i];
						i++;
						return t;
					}

					@Override /* Iterator */
					public void remove() {
						throw new UnsupportedOperationException("Not supported.");
					}
				};
			}

			@Override /* Set */
			public int size() {
				return array.length;
			}
		};
	}

	/**
	 * Converts the specified collection to an array.
	 *
	 * <p>
	 * Works on both object and primitive arrays.
	 *
	 * @param <E> The element type.
	 * @param c The collection to convert to an array.
	 * @param elementType The component type of the collection.
	 * @return A new array.
	 */
	public static <E> Object toArray(Collection<?> c, Class<E> elementType) {
		Object a = Array.newInstance(elementType, c.size());
		Iterator<?> it = c.iterator();
		int i = 0;
		while (it.hasNext())
			Array.set(a, i++, it.next());
		return a;
	}

	/**
	 * Returns <jk>true</jk> if the specified object is an array.
	 *
	 * @param array The array to test.
	 * @return <jk>true</jk> if the specified object is an array.
	 */
	public static boolean isArray(Object array) {
		return array != null && array.getClass().isArray();
	}

	/**
	 * Converts the specified array to an <c>ArrayList</c>
	 *
	 * @param <E> The element type.
	 * @param array The array to convert.
	 * @param elementType
	 * 	The type of objects in the array.
	 * 	It must match the actual component type in the array.
	 * @return A new {@link ArrayList}
	 */
	@SuppressWarnings("unchecked")
	public static <E> List<E> toList(Object array, Class<E> elementType) {
		List<E> l = new ArrayList<>(Array.getLength(array));
		for (int i = 0; i < Array.getLength(array); i++)
			l.add((E)Array.get(array, i));
		return l;
	}

	/**
	 * Recursively converts the specified array into a list of objects.
	 *
	 * @param array The array to convert.
	 * @return A new {@link ArrayList}
	 */
	public static List<Object> toObjectList(Object array) {
		List<Object> l = new ArrayList<>(Array.getLength(array));
		for (int i = 0; i < Array.getLength(array); i++) {
			Object o = Array.get(array, i);
			if (o != null && o.getClass().isArray())
				o = toObjectList(o);
			l.add(o);
		}
		return l;
	}

	/**
	 * Copies the specified array into the specified list.
	 *
	 * <p>
	 * Works on both object and primitive arrays.
	 *
	 * @param array The array to copy into a list.
	 * @param list The list to copy the values into.
	 * @return The same list passed in.
	 */
	@SuppressWarnings({"unchecked","rawtypes"})
	public static List copyToList(Object array, List list) {
		if (array != null) {
			int length = Array.getLength(array);
			for (int i = 0; i < length; i++)
				list.add(Array.get(array, i));
		}
		return list;
	}

	/**
	 * Returns <jk>true</jk> if the specified array contains the specified element using the {@link String#equals(Object)}
	 * method.
	 *
	 * @param element The element to check for.
	 * @param array The array to check.
	 * @return
	 * 	<jk>true</jk> if the specified array contains the specified element,
	 * 	<jk>false</jk> if the array or element is <jk>null</jk>.
	 */
	public static boolean contains(String element, String[] array) {
		return indexOf(element, array) != -1;
	}

	/**
	 * Returns the index position of the element in the specified array using the {@link String#equals(Object)} method.
	 *
	 * @param element The element to check for.
	 * @param array The array to check.
	 * @return
	 * 	The index position of the element in the specified array, or
	 * 	<c>-1</c> if the array doesn't contain the element, or the array or element is <jk>null</jk>.
	 */
	public static int indexOf(String element, String[] array) {
		if (element == null || array == null)
			return -1;
		for (int i = 0; i < array.length; i++)
			if (element.equals(array[i]))
				return i;
		return -1;
	}

	/**
	 * Converts the specified collection to an array of strings.
	 *
	 * <p>
	 * Entries are converted to strings using {@link #toString()}.
	 * <jk>null</jk> values remain <jk>null</jk>.
	 *
	 * @param c The collection to convert.
	 * @return The collection as a string array.
	 */
	public static String[] toStringArray(Collection<?> c) {
		String[] r = new String[c.size()];
		int i = 0;
		for (Object o : c)
			r[i++] = stringify(o);
		return r;
	}

	/**
	 * Returns <jk>true</jk> if the following sorted arrays are equals.
	 *
	 * @param a1 Array #1.
	 * @param a2 Array #2.
	 * @return <jk>true</jk> if the following sorted arrays are equals.
	 */
	public static boolean equals(String[] a1, String[] a2) {
		if (a1.length != a2.length)
			return false;
		for (int i = 0; i < a1.length; i++)
			if (! StringUtils.eq(a1[i], a2[i]))
				return false;
		return true;
	}

	/**
	 * Makes a copy of the specified array.
	 *
	 * @param array The array to copy.
	 * @param <T> The element type.
	 * @return A new copy of the array, or <jk>null</jk> if the array was <jk>null</jk>.s
	 */
	public static <T> T[] copyOf(T[] array) {
		return array == null ? null : Arrays.copyOf(array, array.length);
	}

	/**
	 * Returns <jk>true</jk> if the specified array is not null and has a length greater than zero.
	 *
	 * @param array The array to check.
	 * @return <jk>true</jk> if the specified array is not null and has a length greater than zero.
	 */
	public static final boolean isNotEmptyArray(Object[] array) {
		return array != null && array.length > 0;
	}

	/**
	 * Returns <jk>true</jk> if the specified array is null or has a length of zero.
	 *
	 * @param array The array to check.
	 * @return <jk>true</jk> if the specified array is null or has a length of zero.
	 */
	public static final boolean isEmptyArray(Object[] array) {
		return array == null || array.length == 0;
	}

	/**
	 * Returns <jk>true</jk> if both specified arrays are null or have a length of zero.
	 *
	 * @param array1 The array to check.
	 * @param array2 The array to check.
	 * @return <jk>true</jk> if the specified array is null or has a length of zero.
	 */
	public static final boolean isEmptyArray(Object[] array1, Object[] array2) {
		return isEmptyArray(array1) && isEmptyArray(array2);
	}

	/**
	 * Reverses the entries in an array.
	 *
	 * @param <E> The element type.
	 * @param array The array to reverse.
	 * @return The same array.
	 */
	public static final <E> E[] reverse(E[] array) {
		for (int i = 0; i < array.length / 2; i++) {
			E temp = array[i];
			array[i] = array[array.length - i - 1];
			array[array.length - i - 1] = temp;
		}
		return array;
	}
}
