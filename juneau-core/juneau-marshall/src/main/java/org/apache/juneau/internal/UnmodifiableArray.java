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

import static org.apache.juneau.internal.ThrowableUtils.*;

import java.lang.reflect.*;
import java.util.*;

/**
 * Simple read-only wrapper around an object array.
 *
 * <p>
 * Allows for forward or reverse access to elements of an array without being able to modify the array and without
 * involving copying the array.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 *
 * @param <E> Array element type.
 */
public class UnmodifiableArray<E> implements List<E> {

	final E[] array;
	final int length;
	private final boolean reversed;

	/**
	 * Constructor.
	 *
	 * @param array The array being wrapped.
	 */
	public UnmodifiableArray(E[] array) {
		this(array, false);
	}

	/**
	 * Constructor.
	 *
	 * @param array The array being wrapped.
	 * @param reversed <jk>true</jk> if elements of array should be addressed in reverse.
	 */
	@SuppressWarnings("unchecked")
	public UnmodifiableArray(E[] array, boolean reversed) {
		this.array = array == null ? (E[])new Object[0] : array;
		this.length = this.array.length;
		this.reversed = reversed;
	}

	@Override
	public int size() {
		return array.length;
	}

	@Override
	public boolean isEmpty() {
		return array.length == 0;
	}

	@Override
	public boolean contains(Object o) {
		return indexOf(o) != -1;
	}

	@Override
	public Iterator<E> iterator() {
		if (reversed) {
			return new Iterator<E>() {
				int i = length-1;

				@Override
				public boolean hasNext() {
					return i > -1;
				}

				@Override
				public E next() {
					return array[i--];
				}
			};
		}
		return new Iterator<E>() {
			int i = 0;

			@Override
			public boolean hasNext() {
				return i < length;
			}

			@Override
			public E next() {
				return array[i++];
			}
		};
	}

	@Override
	public Object[] toArray() {
		Object[] o2 = (Object[])Array.newInstance(array.getClass().getComponentType(), array.length);
		for (int i = 0; i < array.length; i++)
			o2[i] = reversed ? array[length-i-1] : array[i];
		return o2;
	}

	@Override
	@SuppressWarnings({ "unchecked", "hiding" })
	public <T> T[] toArray(T[] a) {
		if (a.length < length)
			return (T[])toArray();
		for (int i = 0; i < array.length; i++)
			a[i] = reversed ? (T)array[length-i-1] : (T)array[i];
		return a;
	}

	@Override
	public boolean add(E e) {
		throw unsupportedOperationException("Object is read-only.");
	}

	@Override
	public boolean remove(Object o) {
		throw unsupportedOperationException("Object is read-only.");
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object o : c)
			if (! contains(o))
				return false;
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		throw unsupportedOperationException("Object is read-only.");
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		throw unsupportedOperationException("Object is read-only.");
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw unsupportedOperationException("Object is read-only.");
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw unsupportedOperationException("Object is read-only.");
	}

	@Override
	public void clear() {
		throw unsupportedOperationException("Object is read-only.");
	}

	@Override
	public E get(int index) {
		return reversed ? array[length-index-1] : array[index];
	}

	@Override
	public E set(int index, E element) {
		throw unsupportedOperationException("Object is read-only.");
	}

	@Override
	public void add(int index, E element) {
		throw unsupportedOperationException("Object is read-only.");
	}

	@Override
	public E remove(int index) {
		throw unsupportedOperationException("Object is read-only.");
	}

	@Override
	public int indexOf(Object o) {
		for (int i = 0; i < length; i++) {
			int j = reversed ? length-i-1 : i;
			E t = array[j];
			if ((o == t) || (o != null && o.equals(t)))
				return j;
		}
		return -1;
	}

	@Override
	public int lastIndexOf(Object o) {
		for (int i = length-1; i >= 0; i--) {
			int j = reversed ? length-i-1 : i;
			E t = array[j];
			if ((o == t) || (o != null && o.equals(t)))
				return j;
		}
		return -1;
	}

	@Override
	public ListIterator<E> listIterator() {
		throw unsupportedOperationException("Object is read-only.");
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		throw unsupportedOperationException("Object is read-only.");
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		if (reversed) {
			List<E> l = Arrays.asList(array);
			Collections.reverse(l);
			return l.subList(fromIndex, toIndex);
		}
		return Arrays.asList(array).subList(fromIndex, toIndex);
	}

	@Override
	public String toString() {
		if (array.length == 0)
			return "[]";

		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for (int i = 0; i < array.length; i++) {
			if (i != 0)
				sb.append(',').append(' ');
			sb.append(array[i] == this ? "(this Collection)" : array[i]);
		}
		sb.append(']');
		return sb.toString();
	}
}
