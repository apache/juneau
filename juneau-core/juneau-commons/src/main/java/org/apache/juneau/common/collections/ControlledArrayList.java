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
 * An {@link ArrayList} that allows you to control whether it's read-only via a constructor parameter.
 *
 * <p>
 * This class provides a unique capability: it can appear as an unmodifiable list to external code (via the standard
 * {@link List} interface methods) while still allowing internal code to modify it through special "override" methods.
 * This is useful when you need to pass a list to code that should not modify it, but you still need to modify it
 * internally.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li><b>Controlled Mutability:</b> Can be configured as modifiable or unmodifiable at construction time
 * 	<li><b>Override Methods:</b> Special methods (e.g., {@link #overrideAdd(Object)}) bypass the unmodifiable restriction
 * 	<li><b>Standard List Interface:</b> Implements all standard {@link List} methods with proper unmodifiable enforcement
 * 	<li><b>Iterator Protection:</b> Iterators returned from unmodifiable lists prevent modification operations
 * 	<li><b>Dynamic Control:</b> Can be made unmodifiable after construction via {@link #setUnmodifiable()}
 * </ul>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Passing a list to external code that should not modify it, while maintaining internal modification capability
 * 	<li>Building a list internally and then "freezing" it before exposing it to clients
 * 	<li>Creating a list that appears read-only to consumers but can be modified by trusted internal code
 * 	<li>Implementing defensive copying patterns where the original list needs to remain mutable internally
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a list that appears unmodifiable to external code</jc>
 * 	ControlledArrayList&lt;String&gt; <jv>list</jv> = <jk>new</jk> ControlledArrayList&lt;&gt;(<jk>true</jk>);
 *
 * 	<jc>// Internal code can still modify using override methods</jc>
 * 	<jv>list</jv>.overrideAdd(<js>"item1"</js>);
 * 	<jv>list</jv>.overrideAdd(<js>"item2"</js>);
 *
 * 	<jc>// External code sees it as unmodifiable</jc>
 * 	<jv>list</jv>.add(<js>"item3"</js>);  <jc>// Throws UnsupportedOperationException</jc>
 *
 * 	<jc>// Pass to external code safely</jc>
 * 	processList(<jv>list</jv>);  <jc>// External code cannot modify it</jc>
 *
 * 	<jc>// But internal code can still modify</jc>
 * 	<jv>list</jv>.overrideAdd(<js>"item3"</js>);  <jc>// Works!</jc>
 * </p>
 *
 * <h5 class='section'>Override Methods:</h5>
 * <p>
 * The "override" methods (e.g., {@link #overrideAdd(Object)}, {@link #overrideRemove(int)}) bypass the unmodifiable
 * restriction and allow modification regardless of the list's modifiable state. These methods are intended for use
 * by trusted internal code that needs to modify the list even when it's marked as unmodifiable.
 *
 * <p class='bjava'>
 * 	<jc>// Create unmodifiable list</jc>
 * 	ControlledArrayList&lt;String&gt; <jv>list</jv> = <jk>new</jk> ControlledArrayList&lt;&gt;(<jk>true</jk>);
 *
 * 	<jc>// Standard methods throw exceptions</jc>
 * 	<jv>list</jv>.add(<js>"x"</js>);        <jc>// UnsupportedOperationException</jc>
 * 	<jv>list</jv>.remove(0);               <jc>// UnsupportedOperationException</jc>
 *
 * 	<jc>// Override methods work</jc>
 * 	<jv>list</jv>.overrideAdd(<js>"x"</js>);     <jc>// OK</jc>
 * 	<jv>list</jv>.overrideRemove(0);            <jc>// OK</jc>
 * </p>
 *
 * <h5 class='section'>Iterator Behavior:</h5>
 * <p>
 * When the list is unmodifiable, iterators returned by {@link #iterator()} and {@link #listIterator()} are read-only.
 * Attempting to call {@link Iterator#remove()} or {@link ListIterator#set(Object)} on these iterators will throw
 * {@link UnsupportedOperationException}. However, the override methods can still be used to modify the list.
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is <b>not thread-safe</b>. If multiple threads access a ControlledArrayList concurrently, and at least
 * one thread modifies the list structurally, it must be synchronized externally. The unmodifiable flag does not
 * provide thread-safety; it only controls whether standard {@link List} interface methods can modify the list.
 *
 * <h5 class='section'>Example - Building and Freezing:</h5>
 * <p class='bjava'>
 * 	<jc>// Build a list internally</jc>
 * 	ControlledArrayList&lt;String&gt; <jv>config</jv> = <jk>new</jk> ControlledArrayList&lt;&gt;(<jk>false</jk>);
 * 	<jv>config</jv>.add(<js>"setting1"</js>);
 * 	<jv>config</jv>.add(<js>"setting2"</js>);
 *
 * 	<jc>// Freeze it before exposing</jc>
 * 	<jv>config</jv>.setUnmodifiable();
 *
 * 	<jc>// Now safe to expose - external code cannot modify</jc>
 * 	<jk>return</jk> <jv>config</jv>;
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonCollections">juneau-common-collections</a>
 * </ul>
 *
 * @param <E> The element type.
 */
public class ControlledArrayList<E> extends ArrayList<E> {

	private static final long serialVersionUID = -1L;

	private boolean unmodifiable;

	/**
	 * Constructor.
	 *
	 * <p>
	 * Creates an empty list with the specified modifiability setting.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Create an empty unmodifiable list</jc>
	 * 	ControlledArrayList&lt;String&gt; <jv>list</jv> = <jk>new</jk> ControlledArrayList&lt;&gt;(<jk>true</jk>);
	 *
	 * 	<jc>// Create an empty modifiable list</jc>
	 * 	ControlledArrayList&lt;String&gt; <jv>list2</jv> = <jk>new</jk> ControlledArrayList&lt;&gt;(<jk>false</jk>);
	 * </p>
	 *
	 * @param unmodifiable If <jk>true</jk>, this list cannot be modified through normal list operation methods
	 *                     on the {@link List} interface. Use override methods to modify when unmodifiable.
	 */
	public ControlledArrayList(boolean unmodifiable) {
		this.unmodifiable = unmodifiable;
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * Creates a list with the specified initial contents and modifiability setting.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Create an unmodifiable list with initial contents</jc>
	 * 	List&lt;String&gt; <jv>initial</jv> = List.of(<js>"a"</js>, <js>"b"</js>, <js>"c"</js>);
	 * 	ControlledArrayList&lt;String&gt; <jv>list</jv> = <jk>new</jk> ControlledArrayList&lt;&gt;(<jk>true</jk>, <jv>initial</jv>);
	 *
	 * 	<jc>// Standard methods throw exceptions</jc>
	 * 	<jv>list</jv>.add(<js>"d"</js>);  <jc>// UnsupportedOperationException</jc>
	 * </p>
	 *
	 * @param unmodifiable If <jk>true</jk>, this list cannot be modified through normal list operation methods
	 *                     on the {@link List} interface. Use override methods to modify when unmodifiable.
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
	 * Returns <jk>true</jk> if this list is modifiable through standard {@link List} interface methods.
	 *
	 * <p>
	 * Note that even when this method returns <jk>false</jk>, the list can still be modified using
	 * the override methods (e.g., {@link #overrideAdd(Object)}).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ControlledArrayList&lt;String&gt; <jv>list</jv> = <jk>new</jk> ControlledArrayList&lt;&gt;(<jk>true</jk>);
	 * 	<jsm>assertFalse</jsm>(<jv>list</jv>.isModifiable());  <jc>// Standard methods cannot modify</jc>
	 *
	 * 	ControlledArrayList&lt;String&gt; <jv>list2</jv> = <jk>new</jk> ControlledArrayList&lt;&gt;(<jk>false</jk>);
	 * 	<jsm>assertTrue</jsm>(<jv>list2</jv>.isModifiable());  <jc>// Standard methods can modify</jc>
	 * </p>
	 *
	 * @return <jk>true</jk> if this list is modifiable through standard {@link List} interface methods.
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
	 * <p>
	 * This method allows you to add an element to the list even when it's marked as unmodifiable.
	 * It's intended for use by trusted internal code that needs to modify the list regardless of
	 * its modifiable state.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ControlledArrayList&lt;String&gt; <jv>list</jv> = <jk>new</jk> ControlledArrayList&lt;&gt;(<jk>true</jk>);
	 * 	<jv>list</jv>.add(<js>"x"</js>);           <jc>// Throws UnsupportedOperationException</jc>
	 * 	<jv>list</jv>.overrideAdd(<js>"x"</js>);   <jc>// Works!</jc>
	 * </p>
	 *
	 * @param element Element to be added to this list.
	 * @return <jk>true</jk> (as specified by {@link Collection#add(Object)}).
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
	 * Makes this list unmodifiable through standard {@link List} interface methods.
	 *
	 * <p>
	 * After calling this method, all standard modification methods (e.g., {@link #add(Object)},
	 * {@link #remove(int)}) will throw {@link UnsupportedOperationException}. However, the override
	 * methods (e.g., {@link #overrideAdd(Object)}) can still be used to modify the list.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ControlledArrayList&lt;String&gt; <jv>list</jv> = <jk>new</jk> ControlledArrayList&lt;&gt;(<jk>false</jk>);
	 * 	<jv>list</jv>.add(<js>"a"</js>);  <jc>// Works</jc>
	 *
	 * 	<jv>list</jv>.setUnmodifiable();
	 * 	<jv>list</jv>.add(<js>"b"</js>);  <jc>// Throws UnsupportedOperationException</jc>
	 * 	<jv>list</jv>.overrideAdd(<js>"b"</js>);  <jc>// Still works</jc>
	 * </p>
	 *
	 * @return This object for method chaining.
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
	 * Throws an {@link UnsupportedOperationException} if the unmodifiable flag is set on this list.
	 *
	 * <p>
	 * This method is called by all standard {@link List} interface modification methods to enforce
	 * the unmodifiable restriction. Override methods bypass this check.
	 *
	 * @throws UnsupportedOperationException if the list is unmodifiable.
	 */
	protected final void assertModifiable() {
		if (unmodifiable)
			throw unsupportedOp("List is read-only");
	}
}