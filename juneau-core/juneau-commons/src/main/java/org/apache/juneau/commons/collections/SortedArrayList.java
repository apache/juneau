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
package org.apache.juneau.commons.collections;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.util.*;

/**
 * A sorted list implementation backed by an {@link ArrayList}.
 *
 * <p>
 * Unlike {@link TreeSet}, this class allows duplicate elements and maintains them in sorted order
 * according to a specified {@link Comparator} or natural ordering. All modification operations
 * (add, set, etc.) automatically maintain the sorted order.
 *
 * <p>
 * This implementation uses an {@link ArrayList} internally, providing:
 * <ul>
 * 	<li>Fast random access (O(1) for get operations)
 * 	<li>Efficient binary search for finding insertion points (O(log n))
 * 	<li>Array shifting for insertions (O(n))
 * </ul>
 *
 * <p>
 * For better insertion performance with large lists, consider using {@link SortedLinkedList}.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Maintains elements in sorted order automatically
 * 	<li>Allows duplicate elements (unlike {@link TreeSet})
 * 	<li>Efficient insertion using binary search (O(log n) to find position, O(n) to insert)
 * 	<li>Fast random access (O(1) for get operations)
 * 	<li>Supports custom comparators or natural ordering
 * </ul>
 *
 * <h5 class='section'>Performance Characteristics:</h5>
 * <ul class='spaced-list'>
 * 	<li><b>add(E)</b>: O(n) - binary search to find position + array shift
 * 	<li><b>get(int)</b>: O(1) - direct array access
 * 	<li><b>contains(Object)</b>: O(log n) - binary search
 * 	<li><b>remove(Object)</b>: O(n) - binary search + array shift
 * 	<li><b>set(int, E)</b>: O(n) - may need to re-sort if element order changes
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Natural ordering</jc>
 * 	SortedArrayList&lt;String&gt; <jv>list</jv> = <jk>new</jk> SortedArrayList&lt;&gt;();
 * 	<jv>list</jv>.add(<js>"c"</js>);
 * 	<jv>list</jv>.add(<js>"a"</js>);
 * 	<jv>list</jv>.add(<js>"b"</js>);
 * 	<jc>// list contains: ["a", "b", "c"]</jc>
 *
 * 	<jc>// Custom comparator</jc>
 * 	SortedArrayList&lt;String&gt; <jv>list2</jv> = <jk>new</jk> SortedArrayList&lt;&gt;(Comparator.comparing(String::length));
 * 	<jv>list2</jv>.add(<js>"ccc"</js>);
 * 	<jv>list2</jv>.add(<js>"a"</js>);
 * 	<jv>list2</jv>.add(<js>"bb"</js>);
 * 	<jc>// list2 contains: ["a", "bb", "ccc"] (sorted by length)</jc>
 * </p>
 *
 * <h5 class='section'>Notes:</h5>
 * <ul class='spaced-list'>
 * 	<li>All elements must be mutually comparable (or comparable via the comparator)
 * 	<li>Null elements are allowed only if the comparator supports them
 * 	<li>The list maintains sorted order at all times - you cannot insert at a specific index
 * 	<li>Calling {@link #set(int, E)} may cause the element to move to maintain sorted order
 * 	<li>This implementation is not thread-safe
 * </ul>
 *
 * @param <E> The element type.
 * @see SortedLinkedList
 */
@SuppressWarnings("java:S115")
public class SortedArrayList<E> extends AbstractList<E> implements RandomAccess {

	// Argument name constants for assertArgNotNull
	private static final String ARG_comparator = "comparator";

	private final List<E> list;
	private final Comparator<? super E> comparator;

	/**
	 * Creates a new sorted list using natural ordering.
	 *
	 * <p>
	 * Elements must implement {@link Comparable}.
	 *
	 * @throws ClassCastException if elements are not comparable.
	 */
	@SuppressWarnings("unchecked")
	public SortedArrayList() {
		this((Comparator<? super E>)Comparator.naturalOrder());
	}

	/**
	 * Creates a new sorted list using the specified comparator.
	 *
	 * @param comparator The comparator to use for sorting. Must not be <jk>null</jk>.
	 */
	public SortedArrayList(Comparator<? super E> comparator) {
		this.comparator = assertArgNotNull(ARG_comparator, comparator);
		this.list = new ArrayList<>();
	}

	/**
	 * Creates a new sorted list with the specified initial capacity using natural ordering.
	 *
	 * @param initialCapacity The initial capacity.
	 */
	@SuppressWarnings("unchecked")
	public SortedArrayList(int initialCapacity) {
		this((Comparator<? super E>)Comparator.naturalOrder(), initialCapacity);
	}

	/**
	 * Creates a new sorted list with the specified initial capacity and comparator.
	 *
	 * @param comparator The comparator to use for sorting. Must not be <jk>null</jk>.
	 * @param initialCapacity The initial capacity.
	 */
	public SortedArrayList(Comparator<? super E> comparator, int initialCapacity) {
		this.comparator = assertArgNotNull(ARG_comparator, comparator);
		this.list = new ArrayList<>(initialCapacity);
	}

	/**
	 * Creates a new sorted list containing the elements of the specified collection.
	 *
	 * <p>
	 * The elements are sorted according to natural ordering.
	 *
	 * @param c The collection whose elements are to be placed into this list.
	 */
	@SuppressWarnings("unchecked")
	public SortedArrayList(Collection<? extends E> c) {
		this((Comparator<? super E>)Comparator.naturalOrder(), c);
	}

	/**
	 * Creates a new sorted list containing the elements of the specified collection.
	 *
	 * @param comparator The comparator to use for sorting. Must not be <jk>null</jk>.
	 * @param c The collection whose elements are to be placed into this list.
	 */
	public SortedArrayList(Comparator<? super E> comparator, Collection<? extends E> c) {
		this.comparator = assertArgNotNull(ARG_comparator, comparator);
		this.list = new ArrayList<>(c);
		Collections.sort(this.list, this.comparator);
	}

	/**
	 * Adds the specified element to this list in sorted order.
	 *
	 * <p>
	 * The element is inserted at the appropriate position to maintain sorted order.
	 * If there are equal elements, the new element is inserted after existing equal elements
	 * (maintains insertion order for equal elements).
	 *
	 * @param e The element to be added.
	 * @return <jk>true</jk> (as specified by {@link Collection#add(Object)}).
	 */
	@Override
	public boolean add(E e) {
		int index = findInsertionPoint(e);
		list.add(index, e);
		return true;
	}

	/**
	 * Inserts the specified element at the specified position.
	 *
	 * <p>
	 * <b>Note:</b> The index parameter is ignored - the element is always inserted in sorted order.
	 * This method exists to satisfy the {@link List} interface contract.
	 *
	 * @param index Ignored - element is inserted in sorted order.
	 * @param element The element to be inserted.
	 */
	@Override
	public void add(int index, E element) {
		// Ignore index - always insert in sorted order
		add(element);
	}

	/**
	 * Adds all elements from the specified collection to this list in sorted order.
	 *
	 * @param c The collection containing elements to be added.
	 * @return <jk>true</jk> if this list changed as a result of the call.
	 */
	@Override
	public boolean addAll(Collection<? extends E> c) {
		boolean modified = false;
		for (E e : c) {
			add(e);
			modified = true;
		}
		return modified;
	}

	/**
	 * Adds all elements from the specified collection at the specified position.
	 *
	 * <p>
	 * <b>Note:</b> The index parameter is ignored - elements are always inserted in sorted order.
	 *
	 * @param index Ignored - elements are inserted in sorted order.
	 * @param c The collection containing elements to be added.
	 * @return <jk>true</jk> if this list changed as a result of the call.
	 */
	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		// Ignore index - always insert in sorted order
		return addAll(c);
	}

	/**
	 * Replaces the element at the specified position with the specified element.
	 *
	 * <p>
	 * <b>Important:</b> After setting the element, the list is re-sorted to maintain sorted order.
	 * The element may move to a different position if its sort order changed.
	 *
	 * @param index The index of the element to replace.
	 * @param element The element to be stored at the specified position.
	 * @return The element previously at the specified position.
	 * @throws IndexOutOfBoundsException if the index is out of range.
	 */
	@Override
	public E set(int index, E element) {
		E oldValue = list.remove(index);
		add(element);  // Re-insert in sorted order
		return oldValue;
	}

	/**
	 * Returns the element at the specified position.
	 *
	 * @param index The index of the element to return.
	 * @return The element at the specified position.
	 * @throws IndexOutOfBoundsException if the index is out of range.
	 */
	@Override
	public E get(int index) {
		return list.get(index);
	}

	/**
	 * Removes the element at the specified position.
	 *
	 * @param index The index of the element to be removed.
	 * @return The element that was removed.
	 * @throws IndexOutOfBoundsException if the index is out of range.
	 */
	@Override
	public E remove(int index) {
		return list.remove(index);
	}

	/**
	 * Returns the number of elements in this list.
	 *
	 * @return The number of elements in this list.
	 */
	@Override
	public int size() {
		return list.size();
	}

	/**
	 * Returns the comparator used to order the elements in this list.
	 *
	 * @return The comparator, or <jk>null</jk> if natural ordering is used.
	 */
	public Comparator<? super E> comparator() {
		return comparator == Comparator.naturalOrder() ? null : comparator;
	}

	/**
	 * Finds the insertion point for the specified element using binary search.
	 *
	 * <p>
	 * If there are equal elements, the new element is inserted after existing equal elements
	 * to maintain insertion order for equal elements.
	 *
	 * @param e The element to find the insertion point for.
	 * @return The index at which the element should be inserted.
	 */
	private int findInsertionPoint(E e) {
		int index = Collections.binarySearch(list, e, comparator);
		if (index < 0) {
			// Element not found - convert to insertion point
			return -(index + 1);
		}
		// Element found - find the last occurrence of equal elements
		// and insert after it to maintain insertion order
		int size = list.size();
		while (index < size && comparator.compare(list.get(index), e) == 0) {
			index++;
		}
		return index;
	}

	/**
	 * Compares the specified object with this list for equality.
	 *
	 * <p>
	 * Returns <jk>true</jk> if and only if the specified object is also a list, both lists have the same size,
	 * and all corresponding pairs of elements in the two lists are <i>equal</i>. Two elements <c>e1</c> and
	 * <c>e2</c> are considered equal if <c>(e1==null ? e2==null : e1.equals(e2))</c>.
	 *
	 * @param obj The object to be compared for equality with this list.
	 * @return <jk>true</jk> if the specified object is equal to this list.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof List))
			return false;
		return list.equals(obj);
	}

	/**
	 * Returns the hash code value for this list.
	 *
	 * <p>
	 * The hash code of a list is defined to be the result of the following calculation:
	 * <p class='bcode'>
	 * 	<jk>int</jk> <jv>hashCode</jv> = 1;
	 * 	<jk>for</jk> (<jk>E</jk> <jv>e</jv> : <jv>list</jv>)
	 * 		<jv>hashCode</jv> = 31 * <jv>hashCode</jv> + (<jv>e</jv> == <jk>null</jk> ? 0 : <jv>e</jv>.hashCode());
	 * </p>
	 *
	 * @return The hash code value for this list.
	 */
	@Override
	public int hashCode() {
		return list.hashCode();
	}

	/**
	 * Returns a string representation of this list.
	 *
	 * <p>
	 * The string representation consists of a list of the list's elements in the order they are returned
	 * by its iterator, enclosed in square brackets (<c>"[]"</c>). Adjacent elements are separated by the
	 * characters <c>", "</c> (comma and space). Elements are converted to strings as by
	 * <c>String.valueOf(Object)</c>.
	 *
	 * @return A string representation of this list.
	 */
	@Override
	public String toString() {
		return list.toString();
	}
}

