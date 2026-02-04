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
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

/**
 * A fluent builder for constructing {@link Set} instances with various configuration options.
 *
 * <p>
 * This builder provides a flexible and type-safe way to construct sets with support for adding elements,
 * collections, arrays, sorting, and applying modifiers like unmodifiable or sparse modes. Sets automatically
 * handle duplicates - adding the same element multiple times will result in only one occurrence in the final set.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Fluent API - all methods return <c>this</c> for method chaining
 * 	<li>Multiple add methods - single elements, varargs, collections, arrays
 * 	<li>Arbitrary input support - automatic type conversion with {@link #addAny(Object...)}
 * 	<li>Conditional adding - {@link #addIf(boolean, Object)} for conditional elements
 * 	<li>Sorting support - natural order or custom {@link Comparator}
 * 	<li>Sparse mode - return <jk>null</jk> for empty sets
 * 	<li>Unmodifiable mode - create immutable sets
 * 	<li>Filtering support - exclude unwanted elements via {@link #filtered()} or {@link #filtered(Predicate)}
 * 	<li>Custom conversion functions - type conversion via {@link #elementFunction(Function)}
 * 	<li>Automatic deduplication - duplicate elements are automatically removed
 * </ul>
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Basic usage</jc>
 * 	Set&lt;String&gt; <jv>set</jv> = Sets.<jsm>create</jsm>(String.<jk>class</jk>)
 * 		.add(<js>"apple"</js>, <js>"banana"</js>, <js>"cherry"</js>)
 * 		.build();
 *
 * 	<jc>// Automatic deduplication</jc>
 * 	Set&lt;Integer&gt; <jv>unique</jv> = Sets.<jsm>create</jsm>(Integer.<jk>class</jk>)
 * 		.add(1, 2, 3, 2, 1)  <jc>// Duplicates ignored</jc>
 * 		.build();  <jc>// Contains: 1, 2, 3</jc>
 *
 * 	<jc>// With sorting</jc>
 * 	Set&lt;String&gt; <jv>sorted</jv> = Sets.<jsm>create</jsm>(String.<jk>class</jk>)
 * 		.add(<js>"zebra"</js>, <js>"apple"</js>, <js>"banana"</js>)
 * 		.sorted()
 * 		.build();  <jc>// Returns TreeSet in natural order</jc>
 *
 * 	<jc>// Conditional elements</jc>
 * 	Set&lt;String&gt; <jv>features</jv> = Sets.<jsm>create</jsm>(String.<jk>class</jk>)
 * 		.add(<js>"basic"</js>)
 * 		.addIf(<jv>hasPremium</jv>, <js>"premium"</js>)
 * 		.addIf(<jv>hasEnterprise</jv>, <js>"enterprise"</js>)
 * 		.build();
 *
 * 	<jc>// Immutable set</jc>
 * 	Set&lt;String&gt; <jv>immutable</jv> = Sets.<jsm>create</jsm>(String.<jk>class</jk>)
 * 		.add(<js>"read"</js>, <js>"only"</js>)
 * 		.unmodifiable()
 * 		.build();
 *
 * 	<jc>// From multiple sources</jc>
 * 	Set&lt;Integer&gt; <jv>existing</jv> = Set.of(1, 2, 3);
 * 	Set&lt;Integer&gt; <jv>combined</jv> = Sets.<jsm>create</jsm>(Integer.<jk>class</jk>)
 * 		.addAll(<jv>existing</jv>)
 * 		.add(4, 5, 6)
 * 		.build();
 *
 * 	<jc>// Sparse mode - returns null when empty</jc>
 * 	Set&lt;String&gt; <jv>maybeNull</jv> = Sets.<jsm>create</jsm>(String.<jk>class</jk>)
 * 		.sparse()
 * 		.build();  <jc>// Returns null, not empty set</jc>
 *
 * 	<jc>// FluentSet wrapper - use buildFluent()</jc>
 * 	FluentSet&lt;String&gt; <jv>fluent</jv> = Sets.<jsm>create</jsm>(String.<jk>class</jk>)
 * 		.add(<js>"one"</js>, <js>"two"</js>)
 * 		.buildFluent();
 *
 * 	<jc>// FilteredSet - use buildFiltered()</jc>
 * 	FilteredSet&lt;Integer&gt; <jv>filtered</jv> = Sets.<jsm>create</jsm>(Integer.<jk>class</jk>)
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
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonsCollections">Collections Package</a>
 * 	<li class='jc'>{@link Lists}
 * 	<li class='jc'>{@link Maps}
 * </ul>
 *
 * @param <E> The element type.
 */
@SuppressWarnings("java:S115")
public class Sets<E> {

	// Argument name constants for assertArgNotNull
	private static final String ARG_comparator = "comparator";
	private static final String ARG_elementFunction = "elementFunction";
	private static final String ARG_elementType = "elementType";
	private static final String ARG_filter = "filter";
	private static final String ARG_value = "value";
	private static final String ARG_values = "values";

	/**
	 * Static creator.
	 *
	 * @param <E> The element type.
	 * @param elementType The element type. Must not be <jk>null</jk>.
	 * @return A new builder.
	 */
	public static <E> Sets<E> create(Class<E> elementType) {
		return new Sets<>(assertArgNotNull(ARG_elementType, elementType));
	}

	private Set<E> set;
	private boolean unmodifiable;
	private boolean sparse;
	private boolean concurrent;
	private boolean ordered = false;

	private Comparator<E> comparator;
	private Predicate<E> filter;
	private Class<E> elementType;
	private Function<Object,E> elementFunction;

	/**
	 * Constructor.
	 *
	 * @param elementType The element type. Must not be <jk>null</jk>.
	 */
	public Sets(Class<E> elementType) {
		this.elementType = assertArgNotNull(ARG_elementType, elementType);
	}

	/**
	 * Adds a single value to this set.
	 *
	 * <p>
	 * Note: Filtering is applied at build time, not when adding elements.
	 *
	 * @param value The value to add to this set.
	 * @return This object.
	 */
	public Sets<E> add(E value) {
		if (set == null) {
			if (ordered)
				set = new LinkedHashSet<>();
			else if (nn(comparator))
				set = new TreeSet<>(comparator);
			else
				set = new HashSet<>();
		}
		set.add(value);
		return this;
	}

	/**
	 * Adds multiple values to this set.
	 *
	 * @param values The values to add to this set.
	 * @return This object.
	 */
	@SuppressWarnings("unchecked")
	public Sets<E> add(E...values) {
		assertArgNotNull(ARG_values, values);
		for (var v : values)
			add(v);
		return this;
	}

	/**
	 * Appends the contents of the specified collection into this set.
	 *
	 * <p>
	 * This is a no-op if the value is <jk>null</jk>.
	 *
	 * @param value The collection to add to this set.
	 * @return This object.
	 */
	public Sets<E> addAll(Collection<E> value) {
		if (nn(value)) {
			if (set == null) {
				if (ordered)
					set = new LinkedHashSet<>(value);
				else if (nn(comparator))
					set = new TreeSet<>(comparator);
				else
					set = new HashSet<>(value);
			} else {
				set.addAll(value);
			}
		}
		return this;
	}

	/**
	 * Adds arbitrary values to this set.
	 *
	 * <p>
	 * Objects can be any of the following:
	 * <ul>
	 * 	<li>The same type or convertible to the element type of this set.
	 * 	<li>Collections or arrays of anything on this set.
	 * 	<li>JSON array strings parsed and convertible to the element type of this set.
	 * </ul>
	 *
	 * @param values The values to add.
	 * @return This object.
	 */
	@SuppressWarnings("java:S3776")
	public Sets<E> addAny(Object...values) {
		if (nn(values)) {
			for (var o : values) {
				if (nn(o)) {
					if (o instanceof Collection<?> o2) {
						o2.forEach(this::addAny);
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
	 * Adds a value to this set if the specified flag is true.
	 *
	 * @param flag The flag.
	 * @param value The value.
	 * @return This object.
	 */
	public Sets<E> addIf(boolean flag, E value) {
		if (flag)
			add(value);
		return this;
	}

	/**
	 * Adds entries to this set via JSON array strings.
	 *
	 * @param values The JSON array strings to parse and add to this set.
	 * @return This object.
	 */
	public Sets<E> addJson(String...values) {
		return addAny((Object[])values);
	}

	/**
	 * Builds the set.
	 *
	 * @return A set conforming to the settings on this builder.
	 */
	/**
	 * Builds the set.
	 *
	 * <p>
	 * Applies filtering, sorting, ordering, concurrent, unmodifiable, and sparse options.
	 *
	 * <p>
	 * Set type selection:
	 * <ul>
	 * 	<li>If {@link #sorted()} is set: Uses {@link TreeSet} (or synchronized TreeSet if concurrent)
	 * 	<li>If {@link #ordered()} is set: Uses {@link LinkedHashSet} (or synchronized LinkedHashSet if concurrent)
	 * 	<li>Otherwise: Uses {@link HashSet} (or synchronized HashSet if concurrent)
	 * </ul>
	 *
	 * <p>
	 * If filtering is applied, the result is wrapped in a {@link FilteredSet}.
	 *
	 * @return The built set, or {@code null} if {@link #sparse()} is set and the set is empty.
	 */
	public Set<E> build() {
		if (sparse && e(set))
			return null;

		Set<E> set2 = null;

		if (ordered) {
			set2 = new LinkedHashSet<>();
		} else if (nn(comparator)) {
			set2 = new TreeSet<>(comparator);
		} else {
			set2 = new HashSet<>();
		}

		if (concurrent)
			set2 = synchronizedSet(set2);

		if (nn(filter) || nn(elementFunction)) {
			var set3b = FilteredSet.create(elementType);
			if (nn(filter))
				set3b.filter(filter);
			if (nn(elementFunction))
				set3b.elementFunction(elementFunction);
			set2 = set3b.inner(set2).build();
		}

		if (nn(set))
			set2.addAll(set);

		if (unmodifiable)
			set2 = unmodifiableSet(set2);

		return set2;
	}

	/**
	 * Builds the set and wraps it in a {@link FluentSet}.
	 *
	 * <p>
	 * This is a convenience method that calls {@link #build()} and wraps the result in a {@link FluentSet}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.commons.utils.CollectionUtils.*;
	 *
	 * 	FluentSet&lt;String&gt; <jv>set</jv> = Sets.<jsm>create</jsm>(String.<jk>class</jk>)
	 * 		.add(<js>"one"</js>, <js>"two"</js>)
	 * 		.buildFluent();
	 * </p>
	 *
	 * @return The built set wrapped in a {@link FluentSet}, or {@code null} if {@link #sparse()} is set and the set is empty.
	 */
	public FluentSet<E> buildFluent() {
		Set<E> result = build();
		return result == null ? null : new FluentSet<>(result);
	}

	/**
	 * Builds the set as a {@link FilteredSet}.
	 *
	 * <p>
	 * Set type selection:
	 * <ul>
	 * 	<li>If {@link #sorted()} is set: Uses {@link TreeSet} (or synchronized TreeSet if concurrent)
	 * 	<li>If {@link #ordered()} is set: Uses {@link LinkedHashSet} (or synchronized LinkedHashSet if concurrent)
	 * 	<li>Otherwise: Uses {@link HashSet} (or synchronized HashSet if concurrent)
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.commons.utils.CollectionUtils.*;
	 *
	 * 	FilteredSet&lt;Integer&gt; <jv>set</jv> = Sets.<jsm>create</jsm>(Integer.<jk>class</jk>)
	 * 		.filtered(v -&gt; v != <jk>null</jk> &amp;&amp; v &gt; 0)
	 * 		.add(5)
	 * 		.add(-1)  <jc>// Will be filtered out</jc>
	 * 		.buildFiltered();
	 * </p>
	 *
	 * <p>
	 * Note: If {@link #unmodifiable()} is set, the returned set will be wrapped in an unmodifiable view,
	 * which may cause issues if the FilteredSet tries to modify it internally. It's recommended to avoid
	 * using {@link #unmodifiable()} when calling this method.
	 *
	 * @return The built set as a {@link FilteredSet}, or {@code null} if {@link #sparse()} is set and the set is empty.
	 */
	public FilteredSet<E> buildFiltered() {
		var s = build();
		if (s == null)  // sparse mode and empty
			return null;
		if (s instanceof FilteredSet<E> s2)
			return s2;
		// Note that if unmodifiable is true, 's' will be unmodifiable and will cause an error if you try
		// to insert a value from within FilteredSet.
		return FilteredSet.create(elementType).inner(s).build();
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
	public Sets<E> elementFunction(Function<Object,E> elementFunction) {
		this.elementFunction = assertArgNotNull(ARG_elementFunction, elementFunction);
		return this;
	}

	/**
	 * Specifies the element type on this list.
	 *
	 * @param value The element type. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public Sets<E> elementType(Class<E> value) {
		elementType = assertArgNotNull(ARG_value, value);
		return this;
	}

	/**
	 * Applies a default filter that excludes common "empty" or "unset" values from being added to the set.
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
	 * 	Set&lt;Object&gt; <jv>set</jv> = Sets.<jsm>create</jsm>(Object.<jk>class</jk>)
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
	public Sets<E> filtered() {
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
	 * Applies a filter predicate to elements when building the set.
	 *
	 * <p>
	 * The filter receives the element value. Elements where the predicate returns
	 * {@code true} will be kept; elements where it returns {@code false} will be filtered out.
	 *
	 * <p>
	 * This method can be called multiple times. When called multiple times, all filters are combined
	 * using AND logic - an element must pass all filters to be kept in the set.
	 *
	 * <p>
	 * Note: Filtering is applied at build time, not when adding elements.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.commons.utils.CollectionUtils.*;
	 *
	 * 	<jc>// Keep only non-null, positive integers</jc>
	 * 	Set&lt;Integer&gt; <jv>set</jv> = Sets.<jsm>create</jsm>(Integer.<jk>class</jk>)
	 * 		.filtered(v -&gt; v != <jk>null</jk> &amp;&amp; v &gt; 0)
	 * 		.add(5)
	 * 		.add(-1)     <jc>// Filtered out at build time</jc>
	 * 		.add(<jk>null</jk>) <jc>// Filtered out at build time</jc>
	 * 		.build();
	 *
	 * 	<jc>// Multiple filters combined with AND</jc>
	 * 	Set&lt;Integer&gt; <jv>set2</jv> = Sets.<jsm>create</jsm>(Integer.<jk>class</jk>)
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
	public Sets<E> filtered(Predicate<E> filter) {
		Predicate<E> newFilter = assertArgNotNull(ARG_filter, filter);
		if (this.filter == null)
			this.filter = newFilter;
		else
			this.filter = this.filter.and(newFilter);
		return this;
	}

	/**
	 * Converts the set into a {@link SortedSet}.
	 *
	 * <p>
	 * Note: If {@link #ordered()} was previously called, calling this method will override it.
	 * The last method called ({@link #ordered()} or {@link #sorted()}) determines the final set type.
	 *
	 * @return This object.
	 */
	@SuppressWarnings("unchecked")
	public Sets<E> sorted() {
		return sorted((Comparator<E>)Comparator.naturalOrder());
	}

	/**
	 * Converts the set into a {@link SortedSet} using the specified comparator.
	 *
	 * <p>
	 * Note: If {@link #ordered()} was previously called, calling this method will override it.
	 * The last method called ({@link #ordered()} or {@link #sorted()}) determines the final set type.
	 *
	 * @param comparator The comparator to use for sorting. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public Sets<E> sorted(Comparator<E> comparator) {
		this.comparator = assertArgNotNull(ARG_comparator, comparator);
		ordered = false;
		return this;
	}

	/**
	 * When specified, the {@link #build()} method will return <jk>null</jk> if the set is empty.
	 *
	 * <p>
	 * Otherwise {@link #build()} will never return <jk>null</jk>.
	 *
	 * @return This object.
	 */
	public Sets<E> sparse() {
		sparse = true;
		return this;
	}


	/**
	 * When specified, {@link #build()} will return an unmodifiable set.
	 *
	 * @return This object.
	 */
	public Sets<E> unmodifiable() {
		unmodifiable = true;
		return this;
	}

	/**
	 * When specified, {@link #build()} will return a thread-safe synchronized set.
	 *
	 * <p>
	 * The thread-safety implementation depends on other settings:
	 * <ul>
	 * 	<li>If {@link #sorted()} is set: Uses synchronized {@link TreeSet}
	 * 	<li>If {@link #ordered()} is set: Uses synchronized {@link LinkedHashSet}
	 * 	<li>Otherwise: Uses synchronized {@link HashSet}
	 * </ul>
	 *
	 * <p>
	 * This is useful when the set needs to be accessed from multiple threads.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Create a thread-safe set using synchronized HashSet</jc>
	 * 	Set&lt;String&gt; <jv>set</jv> = Sets.<jsm>create</jsm>(String.<jk>class</jk>)
	 * 		.add(<js>"one"</js>, <js>"two"</js>)
	 * 		.concurrent()
	 * 		.build();
	 *
	 * 	<jc>// Create a thread-safe ordered set</jc>
	 * 	Set&lt;String&gt; <jv>set2</jv> = Sets.<jsm>create</jsm>(String.<jk>class</jk>)
	 * 		.ordered()
	 * 		.concurrent()
	 * 		.add(<js>"one"</js>)
	 * 		.build();
	 * </p>
	 *
	 * @return This object.
	 */
	public Sets<E> concurrent() {
		concurrent = true;
		return this;
	}

	/**
	 * Sets whether {@link #build()} should return a thread-safe synchronized set.
	 *
	 * <p>
	 * The thread-safety implementation depends on other settings:
	 * <ul>
	 * 	<li>If {@link #sorted()} is set: Uses synchronized {@link TreeSet}
	 * 	<li>If {@link #ordered()} is set: Uses synchronized {@link LinkedHashSet}
	 * 	<li>Otherwise: Uses synchronized {@link HashSet}
	 * </ul>
	 *
	 * <p>
	 * This is useful when the set needs to be accessed from multiple threads.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Conditionally create a thread-safe set</jc>
	 * 	Set&lt;String&gt; <jv>set</jv> = Sets.<jsm>create</jsm>(String.<jk>class</jk>)
	 * 		.add(<js>"one"</js>, <js>"two"</js>)
	 * 		.concurrent(<jv>needsThreadSafety</jv>)
	 * 		.build();
	 * </p>
	 *
	 * @param value Whether to make the set thread-safe.
	 * @return This object.
	 */
	public Sets<E> concurrent(boolean value) {
		concurrent = value;
		return this;
	}

	/**
	 * When specified, {@link #build()} will use a {@link LinkedHashSet} to preserve insertion order.
	 *
	 * <p>
	 * If not specified, a {@link HashSet} is used by default (no guaranteed order).
	 *
	 * <p>
	 * Note: If {@link #sorted()} was previously called, calling this method will override it.
	 * The last method called ({@link #ordered()} or {@link #sorted()}) determines the final set type.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Create an ordered set (preserves insertion order)</jc>
	 * 	Set&lt;String&gt; <jv>set</jv> = Sets.<jsm>create</jsm>(String.<jk>class</jk>)
	 * 		.ordered()
	 * 		.add(<js>"one"</js>)
	 * 		.add(<js>"two"</js>)
	 * 		.build();
	 * </p>
	 *
	 * @return This object.
	 */
	public Sets<E> ordered() {
		return ordered(true);
	}

	/**
	 * Sets whether {@link #build()} should use a {@link LinkedHashSet} to preserve insertion order.
	 *
	 * <p>
	 * If <c>false</c> (default), a {@link HashSet} is used (no guaranteed order).
	 * If <c>true</c>, a {@link LinkedHashSet} is used (preserves insertion order).
	 *
	 * <p>
	 * Note: If {@link #sorted()} was previously called, calling this method with <c>true</c> will override it.
	 * The last method called ({@link #ordered()} or {@link #sorted()}) determines the final set type.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Conditionally create an ordered set</jc>
	 * 	Set&lt;String&gt; <jv>set</jv> = Sets.<jsm>create</jsm>(String.<jk>class</jk>)
	 * 		.ordered(<jv>preserveOrder</jv>)
	 * 		.add(<js>"one"</js>)
	 * 		.build();
	 * </p>
	 *
	 * @param value Whether to preserve insertion order.
	 * @return This object.
	 */
	public Sets<E> ordered(boolean value) {
		ordered = value;
		if (ordered)
			comparator = null;
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