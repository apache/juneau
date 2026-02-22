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
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.function.*;

/**
 * A set wrapper that filters elements based on a {@link Predicate} when they are added.
 *
 * <p>
 * This class wraps an underlying set and applies a filter to determine whether elements should be added.
 * Only elements that pass the filter (i.e., the predicate returns <jk>true</jk>) are actually stored in
 * the underlying set.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li><b>Flexible Filtering:</b> Use any {@link Predicate} to filter elements based on any criteria
 * 	<li><b>Optional Filter:</b> Filter is optional when using the builder - defaults to accepting all elements
 * 	<li><b>Multiple Filters:</b> Can combine multiple filters using AND logic
 * 	<li><b>Custom Set Types:</b> Works with any set implementation via the builder's <c>inner</c> method
 * 	<li><b>Type Conversion:</b> Supports automatic type conversion via element function
 * 	<li><b>Convenience Methods:</b> Provides {@link #add(Object)}, {@link #addAll(Collection)}, and {@link #addAny(Object...)} for easy element addition
 * 	<li><b>Transparent Interface:</b> Implements the full {@link Set} interface, so it can be used anywhere a set is expected
 * 	<li><b>Filter on Add:</b> Filtering happens when elements are added via {@link #add(Object)}, {@link #addAll(Collection)}, etc.
 * 	<li><b>Automatic Deduplication:</b> Duplicate elements are automatically handled by the underlying set
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a filtered set that only accepts non-null values</jc>
 * 	FilteredSet&lt;String&gt; <jv>set</jv> = FilteredSet
 * 		.<jsm>create</jsm>(String.<jk>class</jk>)
 * 		.filter(v -&gt; v != <jk>null</jk>)
 * 		.build();
 *
 * 	<jv>set</jv>.add(<js>"value1"</js>);  <jc>// Added</jc>
 * 	<jv>set</jv>.add(<jk>null</jk>);      <jc>// Filtered out</jc>
 * 	<jv>set</jv>.add(<js>"value3"</js>);  <jc>// Added</jc>
 *
 * 	<jc>// set now contains: ["value1", "value3"]</jc>
 * </p>
 *
 * <h5 class='section'>Example - Custom Set Type:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a filtered TreeSet that only accepts positive numbers</jc>
 * 	FilteredSet&lt;Integer&gt; <jv>set</jv> = FilteredSet
 * 		.<jsm>create</jsm>(Integer.<jk>class</jk>)
 * 		.filter(v -&gt; v != <jk>null</jk> &amp;&amp; v &gt; 0)
 * 		.inner(<jk>new</jk> TreeSet&lt;&gt;())
 * 		.build();
 *
 * 	<jv>set</jv>.add(5);   <jc>// Added</jc>
 * 	<jv>set</jv>.add(-1);  <jc>// Filtered out</jc>
 * 	<jv>set</jv>.add(10);  <jc>// Added</jc>
 * </p>
 *
 * <h5 class='section'>Behavior Notes:</h5>
 * <ul class='spaced-list'>
 * 	<li>Filtering is applied to all element addition methods: {@link #add(Object)}, {@link #addAll(Collection)}, etc.
 * 	<li>If the filter returns <jk>false</jk>, the element is silently ignored (not added)
 * 	<li>When {@link #add(Object)} filters out an element, it returns <jk>false</jk> (element was not added)
 * 	<li>The filter is not applied when reading from the set (e.g., {@link #contains(Object)})
 * 	<li>All other set operations behave as expected on the underlying set
 * 	<li>The underlying set type is determined by the <c>inner</c> method (defaults to {@link LinkedHashSet})
 * 	<li>Duplicate elements are automatically handled by the underlying set (only one occurrence is stored)
 * </ul>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is not thread-safe unless the underlying set is thread-safe. If thread safety is required,
 * use a thread-safe set type (e.g., {@link java.util.concurrent.CopyOnWriteArraySet}) via the <c>inner</c> method.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-commons">Overview &gt; juneau-commons</a>
 * </ul>
 *
 * @param <E> The element type.
 */
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention
})
public class FilteredSet<E> extends AbstractSet<E> {

	// Argument name constants for assertArgNotNull
	private static final String ARG_elementType = "elementType";
	private static final String ARG_filter = "filter";
	private static final String ARG_set = "set";
	private static final String ARG_value = "value";

	/**
	 * Builder for creating {@link FilteredSet} instances.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	FilteredSet&lt;Integer&gt; <jv>set</jv> = FilteredSet
	 * 		.<jsm>create</jsm>(Integer.<jk>class</jk>)
	 * 		.filter(v -&gt; v != <jk>null</jk> &amp;&amp; v &gt; 0)
	 * 		.inner(<jk>new</jk> TreeSet&lt;&gt;())
	 * 		.build();
	 * </p>
	 *
	 * @param <E> The element type.
	 */
	public static class Builder<E> {
		private Predicate<E> filter = v -> true;
		private Set<E> inner;
		private Class<E> elementType;
		private Function<Object,E> elementFunction;

		/**
		 * Sets the filter predicate that determines whether elements should be added.
		 *
		 * <p>
		 * The predicate receives the element value. If it returns <jk>true</jk>,
		 * the element is added to the set. If it returns <jk>false</jk>, the element is silently ignored.
		 *
		 * <p>
		 * This method is optional. If not called, the set will accept all elements (defaults to <c>v -&gt; true</c>).
		 *
		 * <p>
		 * This method can be called multiple times. When called multiple times, all filters are combined
		 * using AND logic - an element must pass all filters to be added to the set.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Filter out null values</jc>
		 * 	Builder&lt;String&gt; <jv>b</jv> = FilteredSet.<jsm>create</jsm>(String.<jk>class</jk>)
		 * 		.filter(v -&gt; v != <jk>null</jk>);
		 *
		 * 	<jc>// Filter based on value</jc>
		 * 	Builder&lt;Integer&gt; <jv>b2</jv> = FilteredSet.<jsm>create</jsm>(Integer.<jk>class</jk>)
		 * 		.filter(v -&gt; v != <jk>null</jk> &amp;&amp; v &gt; 0);
		 *
		 * 	<jc>// Multiple filters combined with AND</jc>
		 * 	Builder&lt;Integer&gt; <jv>b3</jv> = FilteredSet.<jsm>create</jsm>(Integer.<jk>class</jk>)
		 * 		.filter(v -&gt; v != <jk>null</jk>)           <jc>// First filter</jc>
		 * 		.filter(v -&gt; v &gt; 0)                    <jc>// Second filter (ANDed with first)</jc>
		 * 		.filter(v -&gt; v &lt; 100);                  <jc>// Third filter (ANDed with previous)</jc>
		 * </p>
		 *
		 * @param value The filter predicate. Must not be <jk>null</jk>.
		 * @return This object for method chaining.
		 */
		public Builder<E> filter(Predicate<E> value) {
			Predicate<E> newFilter = assertArgNotNull(ARG_value, value);
			if (filter == null)
				filter = newFilter;
			else
				filter = filter.and(newFilter);
			return this;
		}

		/**
		 * Sets the underlying set instance that will store the filtered elements.
		 *
		 * <p>
		 * If not specified, a new {@link LinkedHashSet} will be created during {@link #build()}.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Use a TreeSet for sorted order</jc>
		 * 	Builder&lt;String&gt; <jv>b</jv> = FilteredSet.<jsm>create</jsm>(String.<jk>class</jk>)
		 * 		.inner(<jk>new</jk> TreeSet&lt;&gt;());
		 *
		 * 	<jc>// Use a CopyOnWriteArraySet for thread safety</jc>
		 * 	Builder&lt;String&gt; <jv>b2</jv> = FilteredSet.<jsm>create</jsm>(String.<jk>class</jk>)
		 * 		.inner(<jk>new</jk> CopyOnWriteArraySet&lt;&gt;());
		 *
		 * 	<jc>// Use an existing set</jc>
		 * 	Set&lt;String&gt; <jv>existing</jv> = <jk>new</jk> LinkedHashSet&lt;&gt;();
		 * 	Builder&lt;String&gt; <jv>b3</jv> = FilteredSet.<jsm>create</jsm>(String.<jk>class</jk>)
		 * 		.inner(<jv>existing</jv>);
		 * </p>
		 *
		 * @param value The underlying set instance. Must not be <jk>null</jk>.
		 * @return This object for method chaining.
		 */
		public Builder<E> inner(Set<E> value) {
			inner = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Sets the function to use for converting elements when using {@link FilteredSet#add(Object)}.
		 *
		 * <p>
		 * If specified, elements passed to {@link FilteredSet#add(Object)} will be converted using
		 * this function before being added to the set. If not specified, elements must already be of the
		 * correct type (or <c>Object.class</c> is used if types were not specified, which accepts any type).
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	FilteredSet&lt;Integer&gt; <jv>set</jv> = FilteredSet
		 * 		.<jsm>create</jsm>(Integer.<jk>class</jk>)
		 * 		.filter(v -&gt; v != <jk>null</jk> &amp;&amp; v &gt; 0)
		 * 		.elementFunction(o -&gt; Integer.parseInt(o.toString()))
		 * 		.build();
		 *
		 * 	<jv>set</jv>.add(<js>"123"</js>);  <jc>// Element will be converted from String to Integer</jc>
		 * </p>
		 *
		 * @param value The element conversion function. Can be <jk>null</jk>.
		 * @return This object for method chaining.
		 */
		public Builder<E> elementFunction(Function<Object,E> value) {
			elementFunction = value;
			return this;
		}

		/**
		 * Builds a new {@link FilteredSet} instance.
		 *
		 * <p>
		 * If {@link #filter(Predicate)} was not called, the set will accept all elements
		 * (defaults to <c>v -&gt; true</c>).
		 *
		 * @return A new filtered set instance.
		 */
		public FilteredSet<E> build() {
			Set<E> set = inner != null ? inner : new LinkedHashSet<>();
			return new FilteredSet<>(filter, set, elementType, elementFunction);
		}
	}

	/**
	 * Creates a new builder for constructing a filtered set.
	 *
	 * @param <E> The element type.
	 * @param elementType The element type class. Must not be <jk>null</jk>.
	 * @return A new builder.
	 */
	public static <E> Builder<E> create(Class<E> elementType) {
		assertArgNotNull(ARG_elementType, elementType);
		var builder = new Builder<E>();
		builder.elementType = elementType;
		return builder;
	}

	/**
	 * Creates a new builder for constructing a filtered set with generic types.
	 *
	 * <p>
	 * This is a convenience method that creates a builder without requiring explicit type class parameters.
	 * The generic types must be explicitly specified using the diamond operator syntax.
	 *
	 * <p>
	 * When using this method, type checking is effectively disabled (using <c>Object.class</c> as the type),
	 * allowing any element types to be added. However, the generic type parameter should still be
	 * specified for type safety.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Explicitly specify generic types using diamond operator</jc>
	 * 	<jk>var</jk> <jv>set</jv> = FilteredSet
	 * 		.&lt;String&gt;<jsm>create</jsm>()
	 * 		.filter(<jv>v</jv> -&gt; <jv>v</jv> != <jk>null</jk>)
	 * 		.build();
	 * </p>
	 *
	 * @param <E> The element type.
	 * @return A new builder.
	 */
	@SuppressWarnings({
		"unchecked" // Type erasure requires cast to Builder<E>
	})
	public static <E> Builder<E> create() {
		var builder = new Builder<E>();
		builder.elementType = (Class<E>)Object.class;
		return builder;
	}
	private final Predicate<E> filter;
	private final Set<E> set;
	private final Class<E> elementType;
	private final Function<Object,E> elementFunction;

	/**
	 * Constructor.
	 *
	 * @param filter The filter predicate. Can be <jk>null</jk> (if null, all elements are accepted).
	 * @param set The underlying set. Must not be <jk>null</jk>.
	 * @param elementType The element type. Must not be <jk>null</jk> (use <c>Object.class</c> to disable type checking).
	 * @param elementFunction The element conversion function, or <jk>null</jk> if not specified.
	 */
	protected FilteredSet(Predicate<E> filter, Set<E> set, Class<E> elementType, Function<Object,E> elementFunction) {
		this.filter = assertArgNotNull(ARG_filter, filter);
		this.set = assertArgNotNull(ARG_set, set);
		this.elementType = assertArgNotNull(ARG_elementType, elementType);
		this.elementFunction = elementFunction;
	}

	/**
	 * Adds the specified element to this set, if it passes the filter.
	 *
	 * <p>
	 * If the element passes the filter, it is added to the set and this method returns <jk>true</jk>
	 * (or <jk>false</jk> if the element was already present in the set).
	 *
	 * <p>
	 * If the element is filtered out, it is not added to the set and this method returns <jk>false</jk>.
	 *
	 * @param e The element to be added to this set.
	 * @return <jk>true</jk> if the element was added, <jk>false</jk> if it was filtered out or already present.
	 */
	@Override
	public boolean add(E e) {
		if (filter.test(e))
			return set.add(e);
		return false;
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		boolean modified = false;
		for (var e : c) {
			if (filter.test(e) && set.add(e))
				modified = true;
		}
		return modified;
	}

	/**
	 * Tests whether the specified element would pass the filter and be added to this set.
	 *
	 * <p>
	 * This method can be used to check if an element would be accepted by the filter without actually
	 * adding it to the set. This is useful for validation, debugging, or pre-checking elements before
	 * attempting to add them.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	FilteredSet&lt;Integer&gt; <jv>set</jv> = FilteredSet
	 * 		.<jsm>create</jsm>(Integer.<jk>class</jk>)
	 * 		.filter(v -&gt; v != <jk>null</jk> &amp;&amp; v &gt; 0)
	 * 		.build();
	 *
	 * 	<jk>if</jk> (<jv>set</jv>.wouldAccept(5)) {
	 * 		<jv>set</jv>.add(5);  <jc>// Will be added</jc>
	 * 	}
	 * </p>
	 *
	 * @param element The element to test.
	 * @return <jk>true</jk> if the element would be added to the set, <jk>false</jk> if it would be filtered out.
	 */
	public boolean wouldAccept(E element) {
		return filter.test(element);
	}

	/**
	 * Returns the filter predicate used by this set.
	 *
	 * <p>
	 * This method provides access to the filter for debugging, inspection, or advanced use cases.
	 * The returned predicate is the combined filter used internally by this set. If multiple filters
	 * were set via the builder, they are combined using AND logic.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	FilteredSet&lt;Integer&gt; <jv>set</jv> = FilteredSet
	 * 		.<jsm>create</jsm>(Integer.<jk>class</jk>)
	 * 		.filter(v -&gt; v != <jk>null</jk> &amp;&amp; v &gt; 0)
	 * 		.build();
	 *
	 * 	Predicate&lt;Integer&gt; <jv>filter</jv> = <jv>set</jv>.getFilter();
	 * 	<jc>// Use filter for other purposes</jc>
	 * </p>
	 *
	 * @return The filter predicate. Never <jk>null</jk>.
	 */
	public Predicate<E> getFilter() {
		return filter;
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
	public boolean isEmpty() {
		return set.isEmpty();
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
	public Object[] toArray() {
		return set.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return set.toArray(a);
	}

	/**
	 * Adds an element to this set with automatic type conversion.
	 *
	 * <p>
	 * This method converts the element using the configured element function (if any) and validates
	 * the type (if element type was specified when creating the set). After conversion and validation,
	 * the element is added using the standard {@link #add(Object)} method, which applies the filter.
	 *
	 * <h5 class='section'>Type Conversion:</h5>
	 * <ul>
	 * 	<li>If an element function is configured, the element is converted by applying the function
	 * 	<li>If no function is configured, the object is used as-is (but may be validated if types were specified)
	 * </ul>
	 *
	 * <h5 class='section'>Type Validation:</h5>
	 * <ul>
	 * 	<li>If element type was specified (via {@link #create(Class)}), the converted element must be an instance of that type
	 * 	<li>If {@link #create()} was used (no types specified), <c>Object.class</c> is used, which accepts any type
	 * </ul>
	 *
	 * <h5 class='section'>Return Value:</h5>
	 * <ul>
	 * 	<li>If the element is added successfully, returns <jk>true</jk>
	 * 	<li>If the element is filtered out or already present, returns <jk>false</jk>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	FilteredSet&lt;Integer&gt; <jv>set</jv> = FilteredSet
	 * 		.<jsm>create</jsm>(Integer.<jk>class</jk>)
	 * 		.filter(v -&gt; v != <jk>null</jk> &amp;&amp; v &gt; 0)
	 * 		.elementFunction(o -&gt; Integer.parseInt(o.toString()))
	 * 		.build();
	 *
	 * 	<jv>set</jv>.addConverted(<js>"123"</js>);  <jc>// Element converted from String to Integer</jc>
	 * </p>
	 *
	 * @param element The element to add. Will be converted if an element function is configured.
	 * @return <jk>true</jk> if the element was added, <jk>false</jk> if it was filtered out or already present.
	 * @throws RuntimeException If conversion fails or type validation fails.
	 */
	public boolean addConverted(Object element) {
		E convertedElement = convertElement(element);
		return add(convertedElement);
	}

	/**
	 * Adds all elements from the specified collection with automatic type conversion.
	 *
	 * <p>
	 * This method iterates over all elements in the source collection and adds each one using {@link #addConverted(Object)},
	 * which applies conversion and validation before adding.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	FilteredSet&lt;Integer&gt; <jv>set</jv> = FilteredSet
	 * 		.<jsm>create</jsm>(Integer.<jk>class</jk>)
	 * 		.filter(v -&gt; v != <jk>null</jk> &amp;&amp; v &gt; 0)
	 * 		.elementFunction(o -&gt; Integer.parseInt(o.toString()))
	 * 		.build();
	 *
	 * 	Collection&lt;String&gt; <jv>source</jv> = List.of(<js>"5"</js>, <js>"-1"</js>);
	 * 	<jv>set</jv>.addAllConverted(<jv>source</jv>);  <jc>// Elements converted, negative value filtered out</jc>
	 * </p>
	 *
	 * @param source The collection containing elements to add. Can be <jk>null</jk> (no-op).
	 * @return This object for method chaining.
	 */
	public FilteredSet<E> addAllConverted(Collection<?> source) {
		if (source != null) {
			for (var e : source) {
				addConverted(e);
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
	 * </ul>
	 *
	 * <p>
	 * Each element from the collections/arrays will be added using {@link #add(Object)}, which applies
	 * conversion and filtering.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	FilteredSet&lt;Integer&gt; <jv>set</jv> = FilteredSet
	 * 		.<jsm>create</jsm>(Integer.<jk>class</jk>)
	 * 		.filter(v -&gt; v != <jk>null</jk> &amp;&amp; v &gt; 0)
	 * 		.build();
	 *
	 * 	Set&lt;Integer&gt; <jv>set1</jv> = Set.of(5, -1);
	 * 	Set&lt;Integer&gt; <jv>set2</jv> = Set.of(10);
	 * 	<jv>set</jv>.addAny(<jv>set1</jv>, <jv>set2</jv>);  <jc>// Adds 5, 10 (-1 filtered out)</jc>
	 * </p>
	 *
	 * @param values The values to add. Can be <jk>null</jk> or contain <jk>null</jk> values (ignored).
	 * @return This object for method chaining.
	 */
	public FilteredSet<E> addAny(Object...values) {
		if (values != null) {
			for (var o : values) {
				if (o == null)
					continue;
				if (o instanceof Collection<?> c) {
					addAllConverted(c);
				} else if (o.getClass().isArray()) {
					for (int i = 0; i < java.lang.reflect.Array.getLength(o); i++) {
						addConverted(java.lang.reflect.Array.get(o, i));
					}
				} else {
					addConverted(o);
				}
			}
		}
		return this;
	}

	private E convertElement(Object element) {
		if (elementFunction != null) {
			element = elementFunction.apply(element);
		}
		if (element == null) {
			// Allow null for non-primitive types
			if (elementType.isPrimitive())
				throw rex("Cannot set null element for primitive type {0}", elementType.getName());
			return null;
		}
		if (elementType.isInstance(element))
			return elementType.cast(element);
		throw rex("Object of type {0} could not be converted to element type {1}", cn(element), cn(elementType));
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

