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
 * A fluent wrapper around an arbitrary list that provides convenient methods for adding elements.
 *
 * <p>
 * This class wraps an underlying list and provides a fluent API for adding elements. All methods return
 * <c>this</c> to allow method chaining. The underlying list can be any {@link List} implementation.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li><b>Fluent API:</b> All methods return <c>this</c> for method chaining
 * 	<li><b>Arbitrary List Support:</b> Works with any list implementation
 * 	<li><b>Conditional Adding:</b> Add elements conditionally based on boolean expressions
 * 	<li><b>Transparent Interface:</b> Implements the full {@link List} interface, so it can be used anywhere a list is expected
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a FluentList wrapping an ArrayList</jc>
 * 	FluentList&lt;String&gt; <jv>list</jv> = <jk>new</jk> FluentList&lt;&gt;(<jk>new</jk> ArrayList&lt;&gt;());
 *
 * 	<jv>list</jv>
 * 		.a(<js>"item1"</js>)
 * 		.a(<js>"item2"</js>)
 * 		.ai(<jk>true</jk>, <js>"item3"</js>)   <jc>// Added</jc>
 * 		.ai(<jk>false</jk>, <js>"item4"</js>); <jc>// Not added</jc>
 *
 * 	<jc>// Add all elements from another collection</jc>
 * 	List&lt;String&gt; <jv>other</jv> = List.of(<js>"item5"</js>, <js>"item6"</js>);
 * 	<jv>list</jv>.aa(<jv>other</jv>);
 * </p>
 *
 * <h5 class='section'>Example - Conditional Building:</h5>
 * <p class='bjava'>
 * 	<jk>boolean</jk> <jv>includeDebug</jv> = <jk>true</jk>;
 * 	<jk>boolean</jk> <jv>includeTest</jv> = <jk>false</jk>;
 *
 * 	FluentList&lt;String&gt; <jv>config</jv> = <jk>new</jk> FluentList&lt;&gt;(<jk>new</jk> ArrayList&lt;&gt;())
 * 		.a(<js>"setting1"</js>)
 * 		.a(<js>"setting2"</js>)
 * 		.ai(<jv>includeDebug</jv>, <js>"debug"</js>)   <jc>// Added</jc>
 * 		.ai(<jv>includeTest</jv>, <js>"test"</js>);    <jc>// Not added</jc>
 * </p>
 *
 * <h5 class='section'>Behavior Notes:</h5>
 * <ul class='spaced-list'>
 * 	<li>All list operations are delegated to the underlying list
 * 	<li>The fluent methods ({@link #a(Object)}, {@link #aa(Collection)}, {@link #ai(boolean, Object)}) return <c>this</c> for chaining
 * 	<li>If a <jk>null</jk> collection is passed to {@link #a(Collection)}, it is treated as a no-op
 * 	<li>The underlying list is stored by reference (not copied), so modifications affect the original list
 * </ul>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is not thread-safe unless the underlying list is thread-safe. If thread safety is required,
 * use a thread-safe list type (e.g., {@link java.util.concurrent.CopyOnWriteArrayList}).
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-commons">Overview &gt; juneau-commons</a>
 * </ul>
 *
 * @param <E> The element type.
 */
public class FluentList<E> extends AbstractList<E> {

	private final List<E> list;

	/**
	 * Constructor.
	 *
	 * @param inner The underlying list to wrap. Must not be <jk>null</jk>.
	 */
	public FluentList(List<E> inner) {
		this.list = assertArgNotNull("inner", inner);
	}

	/**
	 * Adds a single element to this list.
	 *
	 * <p>
	 * This is a convenience method that calls {@link #add(Object)} and returns <c>this</c>
	 * for method chaining.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	FluentList&lt;String&gt; <jv>list</jv> = <jk>new</jk> FluentList&lt;&gt;(<jk>new</jk> ArrayList&lt;&gt;());
	 * 	<jv>list</jv>.a(<js>"item1"</js>).a(<js>"item2"</js>);
	 * </p>
	 *
	 * @param element The element to add.
	 * @return This object for method chaining.
	 */
	public FluentList<E> a(E element) {
		list.add(element);
		return this;
	}

	/**
	 * Adds all elements from the specified collection to this list.
	 *
	 * <p>
	 * This is a convenience method that calls {@link #addAll(Collection)} and returns <c>this</c>
	 * for method chaining. If the specified collection is <jk>null</jk>, this is a no-op.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	FluentList&lt;String&gt; <jv>list</jv> = <jk>new</jk> FluentList&lt;&gt;(<jk>new</jk> ArrayList&lt;&gt;());
	 * 	List&lt;String&gt; <jv>other</jv> = List.of(<js>"item1"</js>, <js>"item2"</js>);
	 * 	<jv>list</jv>.aa(<jv>other</jv>).a(<js>"item3"</js>);
	 * </p>
	 *
	 * @param c The collection whose elements are to be added. Can be <jk>null</jk> (no-op).
	 * @return This object for method chaining.
	 */
	public FluentList<E> aa(Collection<? extends E> c) {
		if (c != null)
			list.addAll(c);
		return this;
	}

	/**
	 * Adds an element to this list if the specified boolean condition is <jk>true</jk>.
	 *
	 * <p>
	 * This method is useful for conditionally adding elements based on runtime conditions.
	 * If the condition is <jk>false</jk>, the element is not added and this method returns <c>this</c>
	 * without modifying the list.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>boolean</jk> <jv>includeDebug</jv> = <jk>true</jk>;
	 * 	<jk>boolean</jk> <jv>includeTest</jv> = <jk>false</jk>;
	 *
	 * 	FluentList&lt;String&gt; <jv>list</jv> = <jk>new</jk> FluentList&lt;&gt;(<jk>new</jk> ArrayList&lt;&gt;())
	 * 		.a(<js>"basic"</js>)
	 * 		.ai(<jv>includeDebug</jv>, <js>"debug"</js>)   <jc>// Added</jc>
	 * 		.ai(<jv>includeTest</jv>, <js>"test"</js>);    <jc>// Not added</jc>
	 * </p>
	 *
	 * @param condition The condition to evaluate. If <jk>true</jk>, the element is added; if <jk>false</jk>, it is not.
	 * @param element The element to add if the condition is <jk>true</jk>.
	 * @return This object for method chaining.
	 */
	public FluentList<E> ai(boolean condition, E element) {
		if (condition)
			list.add(element);
		return this;
	}

	@Override
	public E get(int index) {
		return list.get(index);
	}

	@Override
	public int size() {
		return list.size();
	}

	@Override
	public boolean add(E e) {
		return list.add(e);
	}

	@Override
	public void add(int index, E element) {
		list.add(index, element);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		return list.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		return list.addAll(index, c);
	}

	@Override
	public E remove(int index) {
		return list.remove(index);
	}

	@Override
	public boolean remove(Object o) {
		return list.remove(o);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return list.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return list.retainAll(c);
	}

	@Override
	public void clear() {
		list.clear();
	}

	@Override
	public E set(int index, E element) {
		return list.set(index, element);
	}

	@Override
	public int indexOf(Object o) {
		return list.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return list.lastIndexOf(o);
	}

	@Override
	public ListIterator<E> listIterator() {
		return list.listIterator();
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return list.listIterator(index);
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		return list.subList(fromIndex, toIndex);
	}

	@Override
	public boolean contains(Object o) {
		return list.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return list.containsAll(c);
	}

	@Override
	public boolean isEmpty() {
		return list.isEmpty();
	}

	@Override
	public Iterator<E> iterator() {
		return list.iterator();
	}

	@Override
	public Object[] toArray() {
		return list.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return list.toArray(a);
	}

	@Override
	public String toString() {
		return list.toString();
	}

	@Override
	public boolean equals(Object o) {
		return list.equals(o);
	}

	@Override
	public int hashCode() {
		return list.hashCode();
	}
}

