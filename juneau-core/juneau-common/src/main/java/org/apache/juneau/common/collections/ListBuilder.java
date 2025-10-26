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

import static java.util.Collections.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.common.utils.*;

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
 * 	<li>Custom converters - type conversion via {@link Converter}
 * </ul>
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Basic usage</jc>
 * 	List&lt;String&gt; <jv>list</jv> = ListBuilder.<jsm>create</jsm>(String.<jk>class</jk>)
 * 		.add(<js>"apple"</js>, <js>"banana"</js>, <js>"cherry"</js>)
 * 		.build();
 *
 * 	<jc>// With sorting</jc>
 * 	List&lt;Integer&gt; <jv>sorted</jv> = ListBuilder.<jsm>create</jsm>(Integer.<jk>class</jk>)
 * 		.add(3, 1, 4, 1, 5, 9, 2, 6)
 * 		.sorted()
 * 		.build();
 *
 * 	<jc>// Conditional elements</jc>
 * 	List&lt;String&gt; <jv>filtered</jv> = ListBuilder.<jsm>create</jsm>(String.<jk>class</jk>)
 * 		.add(<js>"always"</js>)
 * 		.addIf(<jv>includeOptional</jv>, <js>"optional"</js>)
 * 		.build();
 *
 * 	<jc>// Immutable list</jc>
 * 	List&lt;String&gt; <jv>immutable</jv> = ListBuilder.<jsm>create</jsm>(String.<jk>class</jk>)
 * 		.add(<js>"read"</js>, <js>"only"</js>)
 * 		.unmodifiable()
 * 		.build();
 *
 * 	<jc>// Sparse mode - returns null when empty</jc>
 * 	List&lt;String&gt; <jv>maybeNull</jv> = ListBuilder.<jsm>create</jsm>(String.<jk>class</jk>)
 * 		.sparse()
 * 		.build();  <jc>// Returns null, not empty list</jc>
 *
 * 	<jc>// From multiple sources</jc>
 * 	List&lt;Integer&gt; <jv>existing</jv> = Arrays.asList(1, 2, 3);
 * 	List&lt;Integer&gt; <jv>combined</jv> = ListBuilder.<jsm>create</jsm>(Integer.<jk>class</jk>)
 * 		.addAll(<jv>existing</jv>)
 * 		.add(4, 5, 6)
 * 		.build();
 * </p>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is <b>not thread-safe</b>. Each builder instance should be used by a single thread or
 * properly synchronized.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonCollections">juneau-common-collections</a>
 * 	<li class='jc'>{@link MapBuilder}
 * 	<li class='jc'>{@link SetBuilder}
 * </ul>
 *
 * @param <E> The element type.
 */
public class ListBuilder<E> {

	/**
	 * Creates a new list builder for the specified element type.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	List&lt;String&gt; <jv>list</jv> = ListBuilder.<jsm>create</jsm>(String.<jk>class</jk>)
	 * 		.add(<js>"a"</js>, <js>"b"</js>, <js>"c"</js>)
	 * 		.build();
	 * </p>
	 *
	 * @param <E> The element type.
	 * @param elementType The element type class. Required for type-safe operations.
	 * @return A new list builder instance.
	 */
	public static <E> ListBuilder<E> create(Class<E> elementType) {
		return new ListBuilder<>(elementType);
	}

	private List<E> list;
	private boolean unmodifiable = false, sparse = false;
	private Comparator<E> comparator;
	private List<Converter> converters;

	private Class<E> elementType;

	/**
	 * Constructor.
	 *
	 * @param elementType The element type.
	 */
	public ListBuilder(Class<E> elementType) {
		this.elementType = elementType;
	}

	/**
	 * Specifies the list to append to.
	 *
	 * <p>
	 * If not specified, uses a new {@link ArrayList}.
	 *
	 * @param list The list to append to.
	 * @return This object.
	 */
	public ListBuilder<E> to(List<E> list) {
		this.list = list;
		return this;
	}

	/**
	 * Adds a single value to this list.
	 *
	 * @param value The value to add to this list.
	 * @return This object.
	 */
	public ListBuilder<E> add(E value) {
		if (list == null)
			list = new ArrayList<>();
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
	public ListBuilder<E> add(E...values) {
		for (E v : values)
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
	public ListBuilder<E> addAll(Collection<E> value) {
		if (value != null) {
			if (list == null)
				list = new LinkedList<>(value);
			else
				list.addAll(value);
		}
		return this;
	}

	/**
	 * Registers value converters that can adapt incoming values in {@link #addAny(Object...)}.
	 *
	 * @param values Converters to register. Ignored if {@code null}.
	 * @return This object.
	 */
	public ListBuilder<E> converters(Converter...values) {
		if (values.length == 0)
			return this;
		if (converters == null)
			converters = new ArrayList<>();
		converters.addAll(Arrays.asList(values));
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
	 * 	<li>Convertible types - converted using registered {@link Converter}s
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Mix different input types</jc>
	 * 	List&lt;Integer&gt; <jv>list</jv> = ListBuilder.<jsm>create</jsm>(Integer.<jk>class</jk>)
	 * 		.addAny(1, 2, 3)                           <jc>// Direct values</jc>
	 * 		.addAny(Arrays.asList(4, 5, 6))            <jc>// Collection</jc>
	 * 		.addAny(<jk>new int</jk>[]{7, 8, 9})                 <jc>// Array</jc>
	 * 		.build();
	 * </p>
	 *
	 * @param values The values to add. <jk>null</jk> values are ignored.
	 * @return This object for method chaining.
	 * @throws IllegalStateException if element type is unknown.
	 * @throws RuntimeException if a value cannot be converted to the element type.
	 */
	public ListBuilder<E> addAny(Object...values) {
		if (elementType == null)
			throw new IllegalStateException("Unknown element type. Cannot use this method.");
		if (values != null) {
			for (var o : values) {
				if (o != null) {
					if (o instanceof Collection) {
						((Collection<?>)o).forEach(x -> addAny(x));
					} else if (isArray(o)) {
						for (int i = 0; i < Array.getLength(o); i++)
							addAny(Array.get(o, i));
					} else if (elementType.isInstance(o)) {
						add(elementType.cast(o));
					} else {
						if (converters != null) {
							var e = converters.stream().map(x -> x.convertTo(elementType, o)).filter(x -> x != null).findFirst().orElse(null);
							if (e != null) {
								add(e);
							} else {
								var l = converters.stream().map(x -> x.convertTo(List.class, o)).filter(x -> x != null).findFirst().orElse(null);
								if (l != null)
									addAny(l);
								else
									throw ThrowableUtils.runtimeException("Object of type {0} could not be converted to type {1}", o.getClass().getName(), elementType);
							}
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
	public ListBuilder<E> addIf(boolean flag, E value) {
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
	 * Applies sorting/unmodifiable/sparse options.
	 *
	 * @return The built list or {@code null} if {@link #sparse()} is set and the list is empty.
	 */
	public List<E> build() {
		if (sparse) {
			if (list != null && list.isEmpty())
				list = null;
		} else {
			if (list == null)
				list = new ArrayList<>(0);
		}
		if (list != null) {
			if (comparator != null)
				Collections.sort(list, comparator);
			if (unmodifiable)
				list = unmodifiableList(list);
		}
		return list;
	}

	/**
	 * Forces the existing list to be copied instead of appended to.
	 *
	 * @return This object.
	 */
	public ListBuilder<E> copy() {
		if (list != null)
			list = new ArrayList<>(list);
		return this;
	}

	/**
	 * Specifies the element type on this list.
	 *
	 * @param value The element type.
	 * @return This object.
	 */
	public ListBuilder<E> elementType(Class<E> value) {
		this.elementType = value;
		return this;
	}

	/**
	 * Sorts the contents of the list.
	 *
	 * @return This object.
	 */
	@SuppressWarnings("unchecked")
	public ListBuilder<E> sorted() {
		return sorted((Comparator<E>)Comparator.naturalOrder());
	}

	/**
	 * Sorts the contents of the list using the specified comparator.
	 *
	 * @param comparator The comparator to use for sorting.
	 * @return This object.
	 */
	public ListBuilder<E> sorted(Comparator<E> comparator) {
		this.comparator = comparator;
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
	public ListBuilder<E> sparse() {
		this.sparse = true;
		return this;
	}

	/**
	 * When specified, {@link #build()} will return an unmodifiable list.
	 *
	 * @return This object.
	 */
	public ListBuilder<E> unmodifiable() {
		this.unmodifiable = true;
		return this;
	}
}