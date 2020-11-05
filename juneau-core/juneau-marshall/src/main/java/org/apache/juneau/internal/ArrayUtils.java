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

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.ThrowableUtils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.reflect.*;

/**
 * Quick and dirty utilities for working with arrays.
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
	 * Appends one or more elements to an array.
	 *
	 * @param <T> The element type.
	 * @param array The array to append to.
	 * @param newElements The new elements to append to the array.
	 * @return A new array with the specified elements appended.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] append(T[] array, Collection<T> newElements) {
		assertFieldNotNull(array, "array");
		if (newElements.size() == 0)
			return array;
		T[] a = (T[])Array.newInstance(array.getClass().getComponentType(), array.length + newElements.size());
		for (int i = 0; i < array.length; i++)
			a[i] = array[i];
		int l = array.length;
		for (T t : newElements)
			a[l++] = t;
		return a;
	}

	/**
	 * Combine an arbitrary number of arrays into a single array.
	 *
	 * @param arrays Collection of arrays to combine.
	 * @return A new combined array, or <jk>null</jk> if all arrays are <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] combine(T[]...arrays) {
		assertFieldNotNull(arrays, "arrays");
		int l = 0;
		T[] a1 = null;
		for (T[] a : arrays) {
			if (a1 == null && a != null)
				a1 = a;
			l += (a == null ? 0 : a.length);
		}
		if (a1 == null)
			return null;
		T[] a = (T[])Array.newInstance(a1.getClass().getComponentType(), l);
		int i = 0;
		for (T[] aa : arrays)
			if (aa != null)
				for (T t : aa)
					a[i++] = t;
		return a;
	}

	/**
	 * Creates a new array with reversed entries.
	 *
	 * @param <T> The class type of the array.
	 * @param array The array to reverse.
	 * @return A new array with reversed entries, or <jk>null</jk> if the array was <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] reverse(T[] array) {
		if (array == null)
			return null;
		Class<T> c = (Class<T>)array.getClass().getComponentType();
		T[] a2 = (T[])Array.newInstance(c, array.length);
		for (int i = 0; i < array.length; i++)
			a2[a2.length-i-1] = array[i];
		return a2;
	}

	/**
	 * Sorts the elements in an array without creating a new array.
	 *
	 * @param array The array to sort.
	 * @return The same array.
	 */
	public static <T> T[] reverseInline(T[] array) {
		if (array == null)
			return null;
		T t;
		for (int i = 0, j = array.length-1; i < j; i++, j--) {
			t = array[i];
			array[i] = array[j];
			array[j] = t;
		}
		return array;
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
		assertFieldNotNull(array, "array");
		return new AbstractSet<T>() {

			@Override /* Set */
			public Iterator<T> iterator() {
				return new Iterator<T>() {
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
						throw new UnsupportedOperationException();
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
	 * Returns an iterator against an array.
	 *
	 * <p>
	 * This works with any array type (e.g. <c>String[]</c>, <c>Object[]</c>,
	 * <code><jk>int</jk>[]</code>, etc...).
	 *
	 * @param array The array to create an iterator over.
	 * @return An iterator over the specified array.
	 */
	public static Iterator<Object> iterator(final Object array) {
		return new Iterator<Object>() {
			int i = 0;
			int length = array == null ? 0 : Array.getLength(array);

			@Override /* Iterator */
			public boolean hasNext() {
				return i < length;
			}

			@Override /* Iterator */
			public Object next() {
				if (i >= length)
					throw new NoSuchElementException();
				return Array.get(array, i++);
			}

			@Override /* Iterator */
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	/**
	 * Converts the specified collection to an array.
	 *
	 * <p>
	 * Works on both object and primitive arrays.
	 *
	 * @param c The collection to convert to an array.
	 * @param componentType The component type of the collection.
	 * @return A new array.
	 */
	public static <T> Object toArray(Collection<?> c, Class<T> componentType) {
		Object a = Array.newInstance(componentType, c.size());
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
	 * @param array The array to convert.
	 * @param componentType
	 * 	The type of objects in the array.
	 * 	It must match the actual component type in the array.
	 * @return A new {@link ArrayList}
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> toList(Object array, Class<T> componentType) {
		List<T> l = new ArrayList<>(Array.getLength(array));
		for (int i = 0; i < Array.getLength(array); i++)
			l.add((T)Array.get(array, i));
		return l;
	}

	/**
	 * Shortcut for calling <c>myList.toArray(new T[myList.size()]);</c>
	 *
	 * @param c The collection being converted to an array.
	 * @param componentType The component type of the array.
	 * @return The collection converted to an array.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] toObjectArray(Collection<?> c, Class<T> componentType) {
		Object a = Array.newInstance(componentType, c.size());
		Iterator<?> it = c.iterator();
		int i = 0;
		while (it.hasNext())
			Array.set(a, i++, it.next());
		return (T[])a;
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
	 * Returns <jk>true</jk> if the specified array contains the specified element using the {@link Object#equals(Object)}
	 * method.
	 *
	 * @param element The element to check for.
	 * @param array The array to check.
	 * @return
	 * 	<jk>true</jk> if the specified array contains the specified element,
	 * 	<jk>false</jk> if the array or element is <jk>null</jk>.
	 */
	public static <T> boolean contains(T element, T[] array) {
		return indexOf(element, array) != -1;
	}

	/**
	 * Returns <jk>true</jk> if the specified array contains the specified integer
	 *
	 * @param element The element to check for.
	 * @param array The array to check.
	 * @return
	 * 	<jk>true</jk> if the specified array contains the specified element,
	 * 	<jk>false</jk> if the array or element is <jk>null</jk>.
	 */
	public static boolean contains(int element, int[] array) {
		if (array != null)
			for (int i : array)
				if (element == i)
					return true;
		return false;
	}

	/**
	 * Returns the index position of the element in the specified array using the {@link Object#equals(Object)} method.
	 *
	 * @param element The element to check for.
	 * @param array The array to check.
	 * @return
	 * 	The index position of the element in the specified array, or <c>-1</c> if the array doesn't contain the
	 * 	element, or the array or element is <jk>null</jk>.
	 */
	public static <T> int indexOf(T element, T[] array) {
		if (element == null)
			return -1;
		if (array == null)
			return -1;
		for (int i = 0; i < array.length; i++)
			if (element.equals(array[i]))
				return i;
		return -1;
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
		if (element == null)
			return -1;
		if (array == null)
			return -1;
		for (int i = 0; i < array.length; i++)
			if (element.equals(array[i]))
				return i;
		return -1;
	}

	/**
	 * Converts a primitive wrapper array (e.g. <c>Integer[]</c>) to a primitive array (e.g. <code><jk>int</jk>[]</code>).
	 *
	 * @param o The array to convert.  Must be a primitive wrapper array.
	 * @return A new array.
	 * @throws IllegalArgumentException If object is not a wrapper object array.
	 */
	public static Object toPrimitiveArray(Object o) {
		Class<?> c = o.getClass();
		if (! c.isArray())
			throw new IllegalArgumentException("Cannot pass non-array objects to toPrimitiveArray()");
		int l = Array.getLength(o);
		Class<?> tc = ClassInfo.of(c.getComponentType()).getPrimitiveForWrapper();
		if (tc == null)
			throw new IllegalArgumentException("Array type is not a primitive wrapper array.");
		Object a = Array.newInstance(tc, l);
		for (int i = 0; i < l; i++)
			Array.set(a, i, Array.get(o, i));
		return a;
	}

	/**
	 * Converts an Iterable to a list.
	 *
	 * @param i The iterable to convert.
	 * @return A new list of objects copied from the iterable.
	 */
	public static List<?> toList(Iterable<?> i) {
		List<Object> l = new ArrayList<>();
		Iterator<?> i2 = i.iterator();
		while (i2.hasNext())
			l.add(i2.next());
		return l;
	}

	/**
	 * Returns the first object in the specified collection or array.
	 *
	 * @param val The collection or array object.
	 * @return
	 * 	The first object, or <jk>null</jk> if the collection or array is empty or <jk>null</jk> or the value
	 * 	isn't a collection or array.
	 */
	public static Object getFirst(Object val) {
		if (val != null) {
			if (val instanceof Collection) {
				Collection<?> c = (Collection<?>)val;
				if (c.isEmpty())
					return null;
				return c.iterator().next();
			}
			if (val.getClass().isArray())
				return Array.getLength(val) == 0 ? null : Array.get(val, 0);
		}
		return null;
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
			if (! StringUtils.isEquals(a1[i], a2[i]))
				return false;
		return true;
	}

	/**
	 * Converts a collection to an array containing the elements in reversed order.
	 *
	 * @param c The component type of the array.
	 * @param l
	 * 	The collection to convert.
	 * 	<br>The collection is not modified.
	 * @return
	 * 	A new array, or <jk>null</jk> if the collection was <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] toReverseArray(Class<T> c, Collection<T> l) {
		if (l == null)
			return null;
		Object a = Array.newInstance(c, l.size());
		Iterator<T> i = l.iterator();
		int j = l.size();
		while (i.hasNext())
			Array.set(a, --j, i.next());
		return (T[])a;
	}

	/**
	 * Removes the specified element from the specified array.
	 *
	 * @param element The element to remove from the array.
	 * @param array The array to remove the element from.
	 * @return A new array with the element removed, or the original array if the array did not contain the element.
	 */
	public static Object[] remove(Object element, Object[] array) {
		if (! contains(element, array))
			return array;
		List<Object> l = new ArrayList<>(array.length);
		for (Object o2 : array) {
			if (! element.equals(o2))
				l.add(o2);
		}
		return l.toArray(new Object[l.size()]);
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
}
