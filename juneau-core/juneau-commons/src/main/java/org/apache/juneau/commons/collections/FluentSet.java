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
 * A fluent wrapper around an arbitrary set that provides convenient methods for adding elements.
 *
 * <p>
 * This class wraps an underlying set and provides a fluent API for adding elements. All methods return
 * <c>this</c> to allow method chaining. The underlying set can be any {@link Set} implementation.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li><b>Fluent API:</b> All methods return <c>this</c> for method chaining
 * 	<li><b>Arbitrary Set Support:</b> Works with any set implementation
 * 	<li><b>Conditional Adding:</b> Add elements conditionally based on boolean expressions
 * 	<li><b>Transparent Interface:</b> Implements the full {@link Set} interface, so it can be used anywhere a set is expected
 * 	<li><b>Automatic Deduplication:</b> Duplicate elements are automatically handled by the underlying set
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a FluentSet wrapping a LinkedHashSet</jc>
 * 	FluentSet&lt;String&gt; <jv>set</jv> = <jk>new</jk> FluentSet&lt;&gt;(<jk>new</jk> LinkedHashSet&lt;&gt;());
 *
 * 	<jv>set</jv>
 * 		.a(<js>"item1"</js>)
 * 		.a(<js>"item2"</js>)
 * 		.ai(<jk>true</jk>, <js>"item3"</js>)   <jc>// Added</jc>
 * 		.ai(<jk>false</jk>, <js>"item4"</js>); <jc>// Not added</jc>
 *
 * 	<jc>// Add all elements from another collection</jc>
 * 	Set&lt;String&gt; <jv>other</jv> = Set.of(<js>"item5"</js>, <js>"item6"</js>);
 * 	<jv>set</jv>.aa(<jv>other</jv>);
 * </p>
 *
 * <h5 class='section'>Example - Conditional Building:</h5>
 * <p class='bjava'>
 * 	<jk>boolean</jk> <jv>includeDebug</jv> = <jk>true</jk>;
 * 	<jk>boolean</jk> <jv>includeTest</jv> = <jk>false</jk>;
 *
 * 	FluentSet&lt;String&gt; <jv>set</jv> = <jk>new</jk> FluentSet&lt;&gt;(<jk>new</jk> LinkedHashSet&lt;&gt;())
 * 		.a(<js>"setting1"</js>)
 * 		.a(<js>"setting2"</js>)
 * 		.ai(<jv>includeDebug</jv>, <js>"debug"</js>)   <jc>// Added</jc>
 * 		.ai(<jv>includeTest</jv>, <js>"test"</js>);    <jc>// Not added</jc>
 * </p>
 *
 * <h5 class='section'>Behavior Notes:</h5>
 * <ul class='spaced-list'>
 * 	<li>All set operations are delegated to the underlying set
 * 	<li>The fluent methods ({@link #a(Object)}, {@link #aa(Collection)}, {@link #ai(boolean, Object)}) return <c>this</c> for chaining
 * 	<li>If a <jk>null</jk> collection is passed to {@link #aa(Collection)}, it is treated as a no-op
 * 	<li>The underlying set is stored by reference (not copied), so modifications affect the original set
 * 	<li>Duplicate elements are automatically handled by the underlying set (only one occurrence is stored)
 * </ul>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is not thread-safe unless the underlying set is thread-safe. If thread safety is required,
 * use a thread-safe set type (e.g., {@link java.util.concurrent.CopyOnWriteArraySet}).
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-commons">Overview &gt; juneau-commons</a>
 * </ul>
 *
 * @param <E> The element type.
 */
@SuppressWarnings("java:S115")
public class FluentSet<E> extends AbstractSet<E> {

	// Argument name constants for assertArgNotNull
	private static final String ARG_inner = "inner";

	private final Set<E> set;

	/**
	 * Constructor.
	 *
	 * @param inner The underlying set to wrap. Must not be <jk>null</jk>.
	 */
	public FluentSet(Set<E> inner) {
		this.set = assertArgNotNull(ARG_inner, inner);
	}

	/**
	 * Adds a single element to this set.
	 *
	 * <p>
	 * This is a convenience method that calls {@link #add(Object)} and returns <c>this</c>
	 * for method chaining. If the element already exists in the set, it is not added again
	 * (sets do not allow duplicates).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	FluentSet&lt;String&gt; <jv>set</jv> = <jk>new</jk> FluentSet&lt;&gt;(<jk>new</jk> LinkedHashSet&lt;&gt;());
	 * 	<jv>set</jv>.a(<js>"item1"</js>).a(<js>"item2"</js>);
	 * </p>
	 *
	 * @param element The element to add.
	 * @return This object for method chaining.
	 */
	public FluentSet<E> a(E element) {
		set.add(element);
		return this;
	}

	/**
	 * Adds all elements from the specified collection to this set.
	 *
	 * <p>
	 * This is a convenience method that calls {@link #addAll(Collection)} and returns <c>this</c>
	 * for method chaining. If the specified collection is <jk>null</jk>, this is a no-op.
	 * Duplicate elements in the collection are automatically handled (only one occurrence is stored).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	FluentSet&lt;String&gt; <jv>set</jv> = <jk>new</jk> FluentSet&lt;&gt;(<jk>new</jk> LinkedHashSet&lt;&gt;());
	 * 	List&lt;String&gt; <jv>other</jv> = List.of(<js>"item1"</js>, <js>"item2"</js>);
	 * 	<jv>set</jv>.aa(<jv>other</jv>).a(<js>"item3"</js>);
	 * </p>
	 *
	 * @param c The collection whose elements are to be added. Can be <jk>null</jk> (no-op).
	 * @return This object for method chaining.
	 */
	public FluentSet<E> aa(Collection<? extends E> c) {
		if (c != null)
			set.addAll(c);
		return this;
	}

	/**
	 * Adds an element to this set if the specified boolean condition is <jk>true</jk>.
	 *
	 * <p>
	 * This method is useful for conditionally adding elements based on runtime conditions.
	 * If the condition is <jk>false</jk>, the element is not added and this method returns <c>this</c>
	 * without modifying the set.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>boolean</jk> <jv>includeDebug</jv> = <jk>true</jk>;
	 * 	<jk>boolean</jk> <jv>includeTest</jv> = <jk>false</jk>;
	 *
	 * 	FluentSet&lt;String&gt; <jv>set</jv> = <jk>new</jk> FluentSet&lt;&gt;(<jk>new</jk> LinkedHashSet&lt;&gt;())
	 * 		.a(<js>"basic"</js>)
	 * 		.ai(<jv>includeDebug</jv>, <js>"debug"</js>)   <jc>// Added</jc>
	 * 		.ai(<jv>includeTest</jv>, <js>"test"</js>);    <jc>// Not added</jc>
	 * </p>
	 *
	 * @param condition The condition to evaluate. If <jk>true</jk>, the element is added; if <jk>false</jk>, it is not.
	 * @param element The element to add if the condition is <jk>true</jk>.
	 * @return This object for method chaining.
	 */
	public FluentSet<E> ai(boolean condition, E element) {
		if (condition)
			set.add(element);
		return this;
	}

	@Override
	public Iterator<E> iterator() {
		return set.iterator();
	}

	@Override
	public int size() {
		return set.size();
	}

	@Override
	public boolean add(E e) {
		return set.add(e);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		return set.addAll(c);
	}

	@Override
	public boolean remove(Object o) {
		return set.remove(o);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return set.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return set.retainAll(c);
	}

	@Override
	public void clear() {
		set.clear();
	}

	@Override
	public boolean contains(Object o) {
		return set.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return set.containsAll(c);
	}

	@Override
	public boolean isEmpty() {
		return set.isEmpty();
	}

	@Override
	public Object[] toArray() {
		return set.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return set.toArray(a);
	}

	@Override
	public String toString() {
		return set.toString();
	}

	@Override
	public boolean equals(Object o) {
		return set.equals(o);
	}

	@Override
	public int hashCode() {
		return set.hashCode();
	}
}

