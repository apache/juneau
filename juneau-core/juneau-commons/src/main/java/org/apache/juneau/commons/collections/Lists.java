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

import static java.util.Collections.*;
import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

/**
 * A fluent builder for constructing {@link List} instances with various configuration options.
 *
 * <p>
 * This builder provides a flexible and type-safe way to construct lists with support for adding elements,
 * collections, arrays, sorting, and applying modifiers like unmodifiable or sparse modes. It's particularly
 * useful when you need to construct lists dynamically with conditional elements or from multiple sources.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Fluent API - all methods return <c>this</c> for method chaining
 * 	<li>Multiple add methods - single elements, varargs, collections, arrays
 * 	<li>Arbitrary input support - automatic type conversion with {@link #addAny(Object...)}
 * 	<li>Conditional adding - {@link #addIf(boolean, Object)} for conditional elements
 * 	<li>Sorting support - natural order or custom {@link Comparator}
 * 	<li>Sparse mode - return <jk>null</jk> for empty lists
 * 	<li>Unmodifiable mode - create immutable lists
 * 	<li>Filtering support - exclude unwanted elements via {@link #filtered()} or {@link #filtered(Predicate)}
 * 	<li>Custom conversion functions - type conversion via {@link #elementFunction(Function)}
 * </ul>
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Basic usage</jc>
 * 	List&lt;String&gt; <jv>list</jv> = Lists.<jsm>create</jsm>(String.<jk>class</jk>)
 * 		.add(<js>"apple"</js>, <js>"banana"</js>, <js>"cherry"</js>)
 * 		.build();
 *
 * 	<jc>// With sorting</jc>
 * 	List&lt;Integer&gt; <jv>sorted</jv> = Lists.<jsm>create</jsm>(Integer.<jk>class</jk>)
 * 		.add(3, 1, 4, 1, 5, 9, 2, 6)
 * 		.sorted()
 * 		.build();
 *
 * 	<jc>// Conditional elements</jc>
 * 	List&lt;String&gt; <jv>filtered</jv> = Lists.<jsm>create</jsm>(String.<jk>class</jk>)
 * 		.add(<js>"always"</js>)
 * 		.addIf(<jv>includeOptional</jv>, <js>"optional"</js>)
 * 		.build();
 *
 * 	<jc>// Immutable list</jc>
 * 	List&lt;String&gt; <jv>immutable</jv> = Lists.<jsm>create</jsm>(String.<jk>class</jk>)
 * 		.add(<js>"read"</js>, <js>"only"</js>)
 * 		.unmodifiable()
 * 		.build();
 *
 * 	<jc>// Sparse mode - returns null when empty</jc>
 * 	List&lt;String&gt; <jv>maybeNull</jv> = Lists.<jsm>create</jsm>(String.<jk>class</jk>)
 * 		.sparse()
 * 		.build();  <jc>// Returns null, not empty list</jc>
 *
 * 	<jc>// From multiple sources</jc>
 * 	List&lt;Integer&gt; <jv>existing</jv> = l(1, 2, 3);
 * 	List&lt;Integer&gt; <jv>combined</jv> = Lists.<jsm>create</jsm>(Integer.<jk>class</jk>)
 * 		.addAll(<jv>existing</jv>)
 * 		.add(4, 5, 6)
 * 		.build();
 *
 * 	<jc>// FluentList wrapper - use buildFluent()</jc>
 * 	FluentList&lt;String&gt; <jv>fluent</jv> = Lists.<jsm>create</jsm>(String.<jk>class</jk>)
 * 		.add(<js>"one"</js>, <js>"two"</js>)
 * 		.buildFluent();
 *
 * 	<jc>// FilteredList - use buildFiltered()</jc>
 * 	FilteredList&lt;Integer&gt; <jv>filtered</jv> = Lists.<jsm>create</jsm>(Integer.<jk>class</jk>)
 * 		.filtered(v -&gt; v &gt; 0)
 * 		.add(5)
 * 		.add(-1)  <jc>// Filtered out</jc>
 * 		.buildFiltered();
 * </p>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is <b>not thread-safe</b>. Each builder instance should be used by a single thread or
 * properly synchronized.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonsCollections">juneau-commons-collections</a>
 * 	<li class='jc'>{@link Maps}
 * 	<li class='jc'>{@link Sets}
 * </ul>
 *
 * @param <E> The element type.
 */
public class Lists<E> {

	/**
	 * Creates a new list builder for the specified element type.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	List&lt;String&gt; <jv>list</jv> = Lists.<jsm>create</jsm>(String.<jk>class</jk>)
	 * 		.add(<js>"a"</js>, <js>"b"</js>, <js>"c"</js>)
	 * 		.build();
	 * </p>
	 *
	 * @param <E> The element type.
	 * @param elementType The element type class. Required for type-safe operations. Must not be <jk>null</jk>.
	 * @return A new list builder instance.
	 */
	public static <E> Lists<E> create(Class<E> elementType) {
		return new Lists<>(assertArgNotNull("elementType", elementType));
	}

	private List<E> list;
	private boolean unmodifiable = false, sparse = false, concurrent = false;
	private Comparator<E> comparator;

	private Predicate<E> filter;
	private Class<E> elementType;
	private Function<Object,E> elementFunction;

	/**
	 * Constructor.
	 *
	 * @param elementType The element type. Must not be <jk>null</jk>.
	 */
	public Lists(Class<E> elementType) {
		this.elementType = assertArgNotNull("elementType", elementType);
	}

	/**
	 * Adds a single value to this list.
	 *
	 * <p>
	 * Note: Filtering is applied at build time, not when adding elements.
	 *
	 * @param value The value to add to this list.
	 * @return This object.
	 */
	public Lists<E> add(E value) {
		if (list == null)
			list = list();
		list.add(value);
		return this;
	}

	/**
	 * Adds multiple values to this list.
	 *
	 * @param values The values to add to this list.
	 * @return This object.
	 */
	@SuppressWarnings("unchecked")
	public Lists<E> add(E...values) {
		assertArgNotNull("values", values);
		for (var v : values)
			add(v);
		return this;
	}

	/**
	 * Appends the contents of the specified collection into this list.
	 *
	 * <p>
	 * This is a no-op if the value is <jk>null</jk>.
	 *
	 * @param value The collection to add to this list.
	 * @return This object.
	 */
	public Lists<E> addAll(Collection<E> value) {
		if (nn(value)) {
			if (list == null)
				list = new LinkedList<>(value);
			else
				list.addAll(value);
		}
		return this;
	}

	/**
	 * Adds arbitrary values to this list with automatic type conversion.
	 *
	 * <p>
	 * This method provides flexible input handling by automatically converting and flattening various input types:
	 * <ul class='spaced-list'>
	 * 	<li>Direct instances of the element type - added as-is
	 * 	<li>Collections - recursively flattened and elements converted
	 * 	<li>Arrays - recursively flattened and elements converted
	 * 	<li>Convertible types - converted using {@link #elementFunction(Function)}
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Mix different input types</jc>
	 * 	List&lt;Integer&gt; <jv>list</jv> = Lists.<jsm>create</jsm>(Integer.<jk>class</jk>)
	 * 		.addAny(1, 2, 3)                           <jc>// Direct values</jc>
	 * 		.addAny(l(4, 5, 6))            <jc>// Collection</jc>
	 * 		.addAny(<jk>new int</jk>[]{7, 8, 9})                 <jc>// Array</jc>
	 * 		.build();
	 * </p>
	 *
	 * @param values The values to add. <jk>null</jk> values are ignored.
	 * @return This object for method chaining.
	 * @throws IllegalStateException if element type is unknown.
	 * @throws RuntimeException if a value cannot be converted to the element type.
	 */
	public Lists<E> addAny(Object...values) {
		if (nn(values)) {
			for (var o : values) {
				if (nn(o)) {
					if (o instanceof Collection<?> o2) {
						o2.forEach(x -> addAny(x));
					} else if (isArray(o)) {
						for (var i = 0; i < Array.getLength(o); i++)
							addAny(Array.get(o, i));
					} else if (elementType.isInstance(o)) {
						add(elementType.cast(o));
					} else {
						E converted = convertElement(o);
						if (converted != null) {
							add(converted);
						} else {
							throw rex("Object of type {0} could not be converted to type {1}", cn(o), cn(elementType));
						}
					}
				}
			}
		}
		return this;
	}

	/**
	 * Appends a value to this list of the flag is true.
	 *
	 * @param flag The flag.
	 * @param value The value.
	 * @return This object.
	 */
	public Lists<E> addIf(boolean flag, E value) {
		if (flag)
			add(value);
		return this;
	}

	/**
	 * Builds the list.
	 *
	 * @return A list conforming to the settings on this builder.
	 */
	/**
	 * Builds the list.
	 *
	 * <p>
	 * Applies filtering, sorting, concurrent, unmodifiable, and sparse options.
	 *
	 * <p>
	 * If filtering is applied, the result is wrapped in a {@link FilteredList}.
	 *
	 * @return The built list, or {@code null} if {@link #sparse()} is set and the list is empty.
	 */
	public List<E> build() {
		if (sparse && e(list))
			return null;

		var list2 = (List<E>)null;
		if (nn(comparator))
			list2 = new SortedArrayList<>(comparator);
		else
			list2 = new ArrayList<>();

		if (concurrent)
			list2 = synchronizedList(list2);

		if (nn(filter) || nn(elementFunction)) {
			var list3b = FilteredList.create(elementType);
			if (nn(filter))
				list3b.filter(filter);
			if (nn(elementFunction))
				list3b.elementFunction(elementFunction);
			list2 = list3b.inner(list2).build();
		}

		if (nn(list))
			list2.addAll(list);

		if (unmodifiable)
			list2 = unmodifiableList(list2);

		return list2;
	}

	/**
	 * Builds the list and wraps it in a {@link FluentList}.
	 *
	 * <p>
	 * This is a convenience method that calls {@link #build()} and wraps the result in a {@link FluentList}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.commons.utils.CollectionUtils.*;
	 *
	 * 	FluentList&lt;String&gt; <jv>list</jv> = Lists.<jsm>create</jsm>(String.<jk>class</jk>)
	 * 		.add(<js>"one"</js>, <js>"two"</js>)
	 * 		.buildFluent();
	 * </p>
	 *
	 * @return The built list wrapped in a {@link FluentList}, or {@code null} if {@link #sparse()} is set and the list is empty.
	 */
	public FluentList<E> buildFluent() {
		List<E> result = build();
		return result == null ? null : new FluentList<>(result);
	}

	/**
	 * Builds the list as a {@link FilteredList}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.commons.utils.CollectionUtils.*;
	 *
	 * 	FilteredList&lt;Integer&gt; <jv>list</jv> = Lists.<jsm>create</jsm>(Integer.<jk>class</jk>)
	 * 		.filtered(v -&gt; v != <jk>null</jk> &amp;&amp; v &gt; 0)
	 * 		.add(5)
	 * 		.add(-1)  <jc>// Will be filtered out</jc>
	 * 		.buildFiltered();
	 * </p>
	 *
	 * <p>
	 * Note: If {@link #unmodifiable()} is set, the returned list will be wrapped in an unmodifiable view,
	 * which may cause issues if the FilteredList tries to modify it internally. It's recommended to avoid
	 * using {@link #unmodifiable()} when calling this method.
	 *
	 * @return The built list as a {@link FilteredList}, or {@code null} if {@link #sparse()} is set and the list is empty.
	 */
	public FilteredList<E> buildFiltered() {
		var l = build();
		if (l == null)  // sparse mode and empty
			return null;
		if (l instanceof FilteredList<E> l2)
			return l2;
		// Note that if unmodifiable is true, 'l' will be unmodifiable and will cause an error if you try
		// to insert a value from within FilteredList.
		return FilteredList.create(elementType).inner(l).build();
	}

	/**
	 * Sets the element conversion function for converting elements in {@link #addAny(Object...)}.
	 *
	 * <p>
	 * The function is applied to each element when adding elements in {@link #addAny(Object...)}.
	 *
	 * @param elementFunction The function to convert elements. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public Lists<E> elementFunction(Function<Object,E> elementFunction) {
		this.elementFunction = assertArgNotNull("elementFunction", elementFunction);
		return this;
	}

	/**
	 * Specifies the element type on this list.
	 *
	 * @param value The element type. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public Lists<E> elementType(Class<E> value) {
		elementType = assertArgNotNull("value", value);
		return this;
	}

	/**
	 * Applies a default filter that excludes common "empty" or "unset" values from being added to the list.
	 *
	 * <p>
	 * The following values are filtered out:
	 * <ul>
	 * 	<li>{@code null}
	 * 	<li>{@link Boolean#FALSE}
	 * 	<li>Numbers with {@code intValue() == -1}
	 * 	<li>Empty arrays
	 * 	<li>Empty {@link Map Maps}
	 * 	<li>Empty {@link Collection Collections}
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.commons.utils.CollectionUtils.*;
	 *
	 * 	List&lt;Object&gt; <jv>list</jv> = Lists.<jsm>create</jsm>(Object.<jk>class</jk>)
	 * 		.filtered()
	 * 		.add(<js>"name"</js>)
	 * 		.add(-1)              <jc>// Filtered out at build time</jc>
	 * 		.add(<jk>false</jk>)     <jc>// Filtered out at build time</jc>
	 * 		.add(<jk>new</jk> String[0]) <jc>// Filtered out at build time</jc>
	 * 		.build();
	 * </p>
	 *
	 * @return This object.
	 */
	public Lists<E> filtered() {
		// @formatter:off
		return filtered(v -> ! (
			v == null
			|| (v instanceof Boolean v2 && v2.equals(false))
			|| (v instanceof Number v3 && v3.intValue() == -1)
			|| (isArray(v) && Array.getLength(v) == 0)
			|| (v instanceof Map v2 && v2.isEmpty())
			|| (v instanceof Collection v3 && v3.isEmpty())
			));
		// @formatter:on
	}

	/**
	 * Applies a filter predicate to elements when building the list.
	 *
	 * <p>
	 * The filter receives the element value. Elements where the predicate returns
	 * {@code true} will be kept; elements where it returns {@code false} will be filtered out.
	 *
	 * <p>
	 * This method can be called multiple times. When called multiple times, all filters are combined
	 * using AND logic - an element must pass all filters to be kept in the list.
	 *
	 * <p>
	 * Note: Filtering is applied at build time, not when adding elements.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.commons.utils.CollectionUtils.*;
	 *
	 * 	<jc>// Keep only non-null, positive integers</jc>
	 * 	List&lt;Integer&gt; <jv>list</jv> = Lists.<jsm>create</jsm>(Integer.<jk>class</jk>)
	 * 		.filtered(v -&gt; v != <jk>null</jk> &amp;&amp; v &gt; 0)
	 * 		.add(5)
	 * 		.add(-1)     <jc>// Filtered out at build time</jc>
	 * 		.add(<jk>null</jk>) <jc>// Filtered out at build time</jc>
	 * 		.build();
	 *
	 * 	<jc>// Multiple filters combined with AND</jc>
	 * 	List&lt;Integer&gt; <jv>list2</jv> = Lists.<jsm>create</jsm>(Integer.<jk>class</jk>)
	 * 		.filtered(v -&gt; v != <jk>null</jk>)           <jc>// First filter</jc>
	 * 		.filtered(v -&gt; v &gt; 0)                    <jc>// Second filter (ANDed with first)</jc>
	 * 		.filtered(v -&gt; v &lt; 100);                  <jc>// Third filter (ANDed with previous)</jc>
	 * 		.add(5)
	 * 		.add(150)  <jc>// Filtered out (not &lt; 100)</jc>
	 * 		.add(-1)   <jc>// Filtered out (not &gt; 0)</jc>
	 * 		.build();
	 * </p>
	 *
	 * @param filter The filter predicate. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public Lists<E> filtered(Predicate<E> filter) {
		Predicate<E> newFilter = assertArgNotNull("filter", filter);
		if (this.filter == null)
			this.filter = newFilter;
		else
			this.filter = this.filter.and(newFilter);
		return this;
	}

	/**
	 * Sorts the contents of the list.
	 *
	 * @return This object.
	 */
	@SuppressWarnings("unchecked")
	public Lists<E> sorted() {
		return sorted((Comparator<E>)Comparator.naturalOrder());
	}

	/**
	 * Sorts the contents of the list using the specified comparator.
	 *
	 * @param comparator The comparator to use for sorting. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public Lists<E> sorted(Comparator<E> comparator) {
		this.comparator = assertArgNotNull("comparator", comparator);
		return this;
	}

	/**
	 * When specified, the {@link #build()} method will return <jk>null</jk> if the list is empty.
	 *
	 * <p>
	 * Otherwise {@link #build()} will never return <jk>null</jk>.
	 *
	 * @return This object.
	 */
	public Lists<E> sparse() {
		sparse = true;
		return this;
	}


	/**
	 * When specified, {@link #build()} will return an unmodifiable list.
	 *
	 * @return This object.
	 */
	public Lists<E> unmodifiable() {
		this.unmodifiable = true;
		return this;
	}

	/**
	 * When specified, {@link #build()} will return a thread-safe synchronized list.
	 *
	 * <p>
	 * The list will be wrapped using {@link Collections#synchronizedList(List)} to provide thread-safety.
	 * This is useful when the list needs to be accessed from multiple threads.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Create a thread-safe list</jc>
	 * 	List&lt;String&gt; <jv>list</jv> = Lists.<jsm>create</jsm>(String.<jk>class</jk>)
	 * 		.add(<js>"one"</js>, <js>"two"</js>)
	 * 		.concurrent()
	 * 		.build();
	 * </p>
	 *
	 * @return This object.
	 */
	public Lists<E> concurrent() {
		concurrent = true;
		return this;
	}

	/**
	 * Sets whether {@link #build()} should return a thread-safe synchronized list.
	 *
	 * <p>
	 * When <c>true</c>, the list will be wrapped using {@link Collections#synchronizedList(List)} to provide thread-safety.
	 * This is useful when the list needs to be accessed from multiple threads.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Conditionally create a thread-safe list</jc>
	 * 	List&lt;String&gt; <jv>list</jv> = Lists.<jsm>create</jsm>(String.<jk>class</jk>)
	 * 		.add(<js>"one"</js>, <js>"two"</js>)
	 * 		.concurrent(<jv>needsThreadSafety</jv>)
	 * 		.build();
	 * </p>
	 *
	 * @param value Whether to make the list thread-safe.
	 * @return This object.
	 */
	public Lists<E> concurrent(boolean value) {
		concurrent = value;
		return this;
	}

	/**
	 * Converts an element object to the element type.
	 *
	 * @param o The object to convert.
	 * @return The converted element, or <jk>null</jk> if conversion is not possible.
	 */
	@SuppressWarnings("unchecked")
	private E convertElement(Object o) {
		if (elementType.isInstance(o))
			return (E)o;
		if (nn(elementFunction))
			return elementFunction.apply(o);
		return null;
	}
}