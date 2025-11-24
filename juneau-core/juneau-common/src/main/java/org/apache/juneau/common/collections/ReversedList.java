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
package org.apache.juneau.common.collections;

import java.util.*;

/**
 * A reversed view of a list that does not modify the underlying list.
 *
 * <p>
 * This class provides a read-only reversed view of a list, where element access is transparently
 * reversed without copying or modifying the original list. All read operations (get, iterator, etc.)
 * operate on the underlying list in reverse order.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Zero-copy reverse view - no data duplication
 * 	<li>Efficient random access via index translation
 * 	<li>Reflects changes in the underlying list automatically
 * 	<li>Read-only - modification operations throw {@link UnsupportedOperationException}
 * 	<li>Iterator and ListIterator support in reversed order
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a list</jc>
 * 	List&lt;String&gt; <jv>original</jv> = List.<jsm>of</jsm>(<js>"A"</js>, <js>"B"</js>, <js>"C"</js>);
 *
 * 	<jc>// Create reversed view</jc>
 * 	List&lt;String&gt; <jv>reversed</jv> = <jk>new</jk> ReversedList&lt;&gt;(<jv>original</jv>);
 *
 * 	<jc>// Access in reverse order</jc>
 * 	<jv>reversed</jv>.get(0);  <jc>// Returns "C"</jc>
 * 	<jv>reversed</jv>.get(1);  <jc>// Returns "B"</jc>
 * 	<jv>reversed</jv>.get(2);  <jc>// Returns "A"</jc>
 *
 * 	<jc>// Iterate in reverse</jc>
 * 	<jk>for</jk> (String <jv>s</jv> : <jv>reversed</jv>) {
 * 		<jc>// Iterates: "C", "B", "A"</jc>
 * 	}
 * </p>
 *
 * <h5 class='section'>Notes:</h5>
 * <ul class='spaced-list'>
 * 	<li>The underlying list must not be null
 * 	<li>Changes to the underlying list are immediately visible in the reversed view
 * 	<li>All modification operations (add, remove, set, clear) throw {@link UnsupportedOperationException}
 * 	<li>Size changes in the underlying list are reflected in this view
 * </ul>
 *
 * @param <E> The element type.
 */
public class ReversedList<E> extends AbstractList<E> implements RandomAccess {

	private final List<E> list;

	/**
	 * Creates a new reversed view of the specified list.
	 *
	 * @param list The list to reverse. Must not be <jk>null</jk>.
	 * @throws IllegalArgumentException if list is <jk>null</jk>.
	 */
	public ReversedList(List<E> list) {
		if (list == null)
			throw new IllegalArgumentException("List cannot be null");
		this.list = list;
	}

	/**
	 * Returns the element at the specified position in this reversed view.
	 *
	 * <p>
	 * The position is translated to access the underlying list in reverse order.
	 * For example, index 0 returns the last element of the underlying list.
	 *
	 * @param index The index of the element to return (0-based, in reversed order).
	 * @return The element at the specified position in the reversed view.
	 * @throws IndexOutOfBoundsException if the index is out of range.
	 */
	@Override
	public E get(int index) {
		return list.get(list.size() - 1 - index);
	}

	/**
	 * Returns the number of elements in this reversed view.
	 *
	 * <p>
	 * This is always equal to the size of the underlying list.
	 *
	 * @return The number of elements in this reversed view.
	 */
	@Override
	public int size() {
		return list.size();
	}

	/**
	 * Returns an iterator over the elements in this reversed view in proper sequence.
	 *
	 * <p>
	 * The iterator traverses the underlying list in reverse order.
	 *
	 * @return An iterator over the elements in reversed order.
	 */
	@Override
	public Iterator<E> iterator() {
		return new Iterator<>() {
			private final ListIterator<E> it = list.listIterator(list.size());

			@Override
			public boolean hasNext() {
				return it.hasPrevious();
			}

			@Override
			public E next() {
				return it.previous();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("ReversedList is read-only");
			}
		};
	}

	/**
	 * Returns a list iterator over the elements in this reversed view.
	 *
	 * <p>
	 * The iterator traverses the underlying list in reverse order.
	 *
	 * @return A list iterator over the elements in reversed order.
	 */
	@Override
	public ListIterator<E> listIterator() {
		return listIterator(0);
	}

	/**
	 * Returns a list iterator over the elements in this reversed view, starting at the specified position.
	 *
	 * <p>
	 * The iterator traverses the underlying list in reverse order, starting from the translated position.
	 *
	 * @param index The index of the first element to be returned from the list iterator (in reversed order).
	 * @return A list iterator over the elements in reversed order.
	 * @throws IndexOutOfBoundsException if the index is out of range.
	 */
	@Override
	public ListIterator<E> listIterator(int index) {
		if (index < 0 || index > size())
			throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());

		return new ListIterator<>() {
			private final ListIterator<E> it = list.listIterator(list.size() - index);

			@Override
			public boolean hasNext() {
				return it.hasPrevious();
			}

			@Override
			public E next() {
				return it.previous();
			}

			@Override
			public boolean hasPrevious() {
				return it.hasNext();
			}

			@Override
			public E previous() {
				return it.next();
			}

			@Override
			public int nextIndex() {
				return list.size() - it.previousIndex() - 1;
			}

			@Override
			public int previousIndex() {
				return list.size() - it.nextIndex() - 1;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("ReversedList is read-only");
			}

			@Override
			public void set(E e) {
				throw new UnsupportedOperationException("ReversedList is read-only");
			}

			@Override
			public void add(E e) {
				throw new UnsupportedOperationException("ReversedList is read-only");
			}
		};
	}

	/**
	 * Returns a view of the portion of this reversed list between the specified indices.
	 *
	 * <p>
	 * The returned sublist is also a reversed view and reflects changes in the underlying list.
	 *
	 * @param fromIndex Low endpoint (inclusive) of the subList.
	 * @param toIndex High endpoint (exclusive) of the subList.
	 * @return A view of the specified range within this reversed list.
	 * @throws IndexOutOfBoundsException if the indices are out of range.
	 */
	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		if (fromIndex < 0 || toIndex > size() || fromIndex > toIndex)
			throw new IndexOutOfBoundsException("fromIndex: " + fromIndex + ", toIndex: " + toIndex + ", size: " + size());

		// Translate indices to the underlying list
		int translatedFrom = list.size() - toIndex;
		int translatedTo = list.size() - fromIndex;

		return new ReversedList<>(list.subList(translatedFrom, translatedTo));
	}

	/**
	 * Not supported - this is a read-only view.
	 *
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void add(int index, E element) {
		throw new UnsupportedOperationException("ReversedList is read-only");
	}

	/**
	 * Not supported - this is a read-only view.
	 *
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public E remove(int index) {
		throw new UnsupportedOperationException("ReversedList is read-only");
	}

	/**
	 * Not supported - this is a read-only view.
	 *
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public E set(int index, E element) {
		throw new UnsupportedOperationException("ReversedList is read-only");
	}

	/**
	 * Not supported - this is a read-only view.
	 *
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void clear() {
		throw new UnsupportedOperationException("ReversedList is read-only");
	}
}
