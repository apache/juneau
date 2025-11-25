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

import static org.apache.juneau.common.utils.AssertionUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;

import java.util.*;
import java.util.function.*;

/**
 * An array list that allows you to control whether it's read-only via a constructor parameter.
 *
 * <p>
 * Override methods such as {@link #overrideAdd(int, Object)} are provided that bypass the unmodifiable restriction
 * on the list.  They allow you to manipulate the list while not exposing the ability to manipulate the list through
 * any of the methods provided by the {@link List} interface (meaning you can pass the object around as an unmodifiable List).
 *
 * @param <E> The element type.
 */
public class ControlledArrayList<E> extends ArrayList<E> {

	private static final long serialVersionUID = -1L;

	private boolean unmodifiable;

	/**
	 * Constructor.
	 *
	 * @param unmodifiable If <jk>true</jk>, this list cannot be modified through normal list operation methods on the {@link List} interface.
	 */
	public ControlledArrayList(boolean unmodifiable) {
		this.unmodifiable = unmodifiable;
	}

	/**
	 * Constructor.
	 *
	 * @param unmodifiable If <jk>true</jk>, this list cannot be modified through normal list operation methods on the {@link List} interface.
	 * @param list The initial contents of this list. Must not be <jk>null</jk>.
	 */
	public ControlledArrayList(boolean unmodifiable, List<? extends E> list) {
		super(assertArgNotNull("list", list));
		this.unmodifiable = unmodifiable;
	}

	@Override
	public boolean add(E element) {
		assertModifiable();
		return overrideAdd(element);
	}

	@Override
	public void add(int index, E element) {
		assertModifiable();
		overrideAdd(index, element);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		assertModifiable();
		return overrideAddAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		assertModifiable();
		return overrideAddAll(index, c);
	}

	@Override
	public void clear() {
		assertModifiable();
		overrideClear();
	}

	/**
	 * Returns <jk>true</jk> if this list is modifiable.
	 *
	 * @return <jk>true</jk> if this list is modifiable.
	 */
	public boolean isModifiable() { return ! unmodifiable; }

	@Override
	public Iterator<E> iterator() {
		if (! unmodifiable)
			return overrideIterator();

		return new Iterator<>() {
			private final Iterator<? extends E> i = overrideIterator();

			@Override
			public void forEachRemaining(Consumer<? super E> action) {
				i.forEachRemaining(action);
			}

			@Override
			public boolean hasNext() {
				return i.hasNext();
			}

			@Override
			public E next() {
				return i.next();
			}

			@Override
			public void remove() {
				throw unsupportedOp();
			}
		};
	}

	@Override
	public ListIterator<E> listIterator() {
		return listIterator(0);
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		if (! unmodifiable)
			return overrideListIterator(index);

		return new ListIterator<>() {
			private final ListIterator<? extends E> i = overrideListIterator(index);

			@Override
			public void add(E e) {
				throw unsupportedOp();
			}

			@Override
			public void forEachRemaining(Consumer<? super E> action) {
				i.forEachRemaining(action);
			}

			@Override
			public boolean hasNext() {
				return i.hasNext();
			}

			@Override
			public boolean hasPrevious() {
				return i.hasPrevious();
			}

			@Override
			public E next() {
				return i.next();
			}

			@Override
			public int nextIndex() {
				return i.nextIndex();
			}

			@Override
			public E previous() {
				return i.previous();
			}

			@Override
			public int previousIndex() {
				return i.previousIndex();
			}

			@Override
			public void remove() {
				throw unsupportedOp();
			}

			@Override
			public void set(E e) {
				throw unsupportedOp();
			}
		};
	}

	/**
	 * Same as {@link #add(Object)} but bypasses the modifiable flag.
	 *
	 * @param element Element to be stored at the specified position.
	 * @return <jk>true</jk>.
	 */
	public boolean overrideAdd(E element) {
		return super.add(element);
	}

	/**
	 * Same as {@link #add(int, Object)} but bypasses the modifiable flag.
	 *
	 * @param index Index of the element to replace.
	 * @param element Element to be stored at the specified position.
	 */
	public void overrideAdd(int index, E element) {
		super.add(index, element);
	}

	/**
	 * Same as {@link #addAll(Collection)} but bypasses the modifiable flag.
	 *
	 * @param c Collection containing elements to be added to this list.
	 * @return <jk>true</jk> if this list changed as a result of the call.
	 */
	public boolean overrideAddAll(Collection<? extends E> c) {
		return super.addAll(c);
	}

	/**
	 * Same as {@link #addAll(int,Collection)} but bypasses the modifiable flag.
	 *
	 * @param index Index at which to insert the first element from the specified collection.
	 * @param c Collection containing elements to be added to this list.
	 * @return <jk>true</jk> if this list changed as a result of the call.
	 */
	public boolean overrideAddAll(int index, Collection<? extends E> c) {
		return super.addAll(index, c);
	}

	/**
	 * Same as {@link #clear()} but bypasses the modifiable flag.
	 */
	public void overrideClear() {
		super.clear();
	}

	/**
	 * Same as {@link #iterator()} but bypasses the modifiable flag.
	 *
	 * @return An iterator over the elements in this list in proper sequence.
	 */
	public Iterator<E> overrideIterator() {
		return super.iterator();
	}

	/**
	 * Same as {@link #listIterator()} but bypasses the modifiable flag.
	 *
	 * @param index Index of the first element to be returned from the list iterator.
	 * @return A list iterator over the elements in this list (in proper sequence), starting at the specified position in the list.
	 */
	public ListIterator<E> overrideListIterator(int index) {
		return super.listIterator(index);
	}

	/**
	 * Same as {@link #remove(int)} but bypasses the modifiable flag.
	 *
	 * @param index Index of the element to remove.
	 * @return The element that was removed from the list.
	 */
	public E overrideRemove(int index) {
		return super.remove(index);
	}

	/**
	 * Same as {@link #remove(Object)} but bypasses the modifiable flag.
	 *
	 * @param o Element to be removed from this list, if present.
	 * @return <jk>true</jk> if this list contained the specified element.
	 */
	public boolean overrideRemove(Object o) {
		return super.remove(o);
	}

	/**
	 * Same as {@link #removeAll(Collection)} but bypasses the modifiable flag.
	 *
	 * @param c Collection containing elements to be removed from this list.
	 * @return <jk>true</jk> if this list changed as a result of the call.
	 */
	public boolean overrideRemoveAll(Collection<?> c) {
		return super.removeAll(c);
	}

	/**
	 * Same as {@link #removeIf(Predicate)} but bypasses the modifiable flag.
	 *
	 * @param filter A predicate which returns true for elements to be removed.
	 * @return <jk>true</jk> if any elements were removed.
	 */
	public boolean overrideRemoveIf(Predicate<? super E> filter) {
		return super.removeIf(filter);
	}

	/**
	 * Same as {@link #replaceAll(UnaryOperator)} but bypasses the modifiable flag.
	 *
	 * @param operator The operator to apply to each element.
	 */
	public void overrideReplaceAll(UnaryOperator<E> operator) {
		super.replaceAll(operator);
	}

	/**
	 * Same as {@link #retainAll(Collection)} but bypasses the modifiable flag.
	 *
	 * @param c Collection containing elements to be retained in this list.
	 * @return <jk>true</jk> if this list changed as a result of the call.
	 */
	public boolean overrideRetainAll(Collection<?> c) {
		return super.retainAll(c);
	}

	/**
	 * Same as {@link #set(int, Object)} but bypasses the modifiable flag.
	 *
	 * @param index Index of the element to replace.
	 * @param element Element to be stored at the specified position.
	 * @return The element previously at the specified position.
	 */
	public E overrideSet(int index, E element) {
		return super.set(index, element);
	}

	/**
	 * Same as {@link #overrideSort(Comparator)} but bypasses the modifiable flag.
	 *
	 * @param c The Comparator used to compare list elements. A null value indicates that the elements' natural ordering should be used.
	 */
	public void overrideSort(Comparator<? super E> c) {
		super.sort(c);
	}

	@Override
	public E remove(int index) {
		assertModifiable();
		return overrideRemove(index);
	}

	@Override
	public boolean remove(Object o) {
		assertModifiable();
		return overrideRemove(o);
	}

	@Override
	public boolean removeAll(Collection<?> coll) {
		assertModifiable();
		return overrideRemoveAll(coll);
	}

	@Override
	public boolean removeIf(Predicate<? super E> filter) {
		assertModifiable();
		return overrideRemoveIf(filter);
	}

	@Override
	public void replaceAll(UnaryOperator<E> operator) {
		assertModifiable();
		overrideReplaceAll(operator);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		assertModifiable();
		return overrideRetainAll(c);
	}

	@Override
	public E set(int index, E element) {
		assertModifiable();
		return overrideSet(index, element);
	}

	/**
	 * Specifies whether this bean should be unmodifiable.
	 * <p>
	 * When enabled, attempting to set any properties on this bean will cause an {@link UnsupportedOperationException}.
	 *
	 * @return This object.
	 */
	public ControlledArrayList<E> setUnmodifiable() {
		unmodifiable = true;
		return this;
	}

	@Override
	public void sort(Comparator<? super E> c) {
		assertModifiable();
		overrideSort(c);
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		return new ControlledArrayList<>(unmodifiable, super.subList(fromIndex, toIndex));
	}

	/**
	 * Throws an {@link UnsupportedOperationException} if the unmodifiable flag is set on this bean.
	 */
	protected final void assertModifiable() {
		if (unmodifiable)
			throw unsupportedOp("List is read-only");
	}
}