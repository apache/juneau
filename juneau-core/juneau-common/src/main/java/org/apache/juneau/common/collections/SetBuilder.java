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
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.common.utils.*;

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
 * 	<li>Custom converters - type conversion via {@link Converter}
 * 	<li>Automatic deduplication - duplicate elements are automatically removed
 * </ul>
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Basic usage</jc>
 * 	Set&lt;String&gt; <jv>set</jv> = SetBuilder.<jsm>create</jsm>(String.<jk>class</jk>)
 * 		.add(<js>"apple"</js>, <js>"banana"</js>, <js>"cherry"</js>)
 * 		.build();
 *
 * 	<jc>// Automatic deduplication</jc>
 * 	Set&lt;Integer&gt; <jv>unique</jv> = SetBuilder.<jsm>create</jsm>(Integer.<jk>class</jk>)
 * 		.add(1, 2, 3, 2, 1)  <jc>// Duplicates ignored</jc>
 * 		.build();  <jc>// Contains: 1, 2, 3</jc>
 *
 * 	<jc>// With sorting</jc>
 * 	Set&lt;String&gt; <jv>sorted</jv> = SetBuilder.<jsm>create</jsm>(String.<jk>class</jk>)
 * 		.add(<js>"zebra"</js>, <js>"apple"</js>, <js>"banana"</js>)
 * 		.sorted()
 * 		.build();  <jc>// Returns TreeSet in natural order</jc>
 *
 * 	<jc>// Conditional elements</jc>
 * 	Set&lt;String&gt; <jv>features</jv> = SetBuilder.<jsm>create</jsm>(String.<jk>class</jk>)
 * 		.add(<js>"basic"</js>)
 * 		.addIf(<jv>hasPremium</jv>, <js>"premium"</js>)
 * 		.addIf(<jv>hasEnterprise</jv>, <js>"enterprise"</js>)
 * 		.build();
 *
 * 	<jc>// Immutable set</jc>
 * 	Set&lt;String&gt; <jv>immutable</jv> = SetBuilder.<jsm>create</jsm>(String.<jk>class</jk>)
 * 		.add(<js>"read"</js>, <js>"only"</js>)
 * 		.unmodifiable()
 * 		.build();
 *
 * 	<jc>// From multiple sources</jc>
 * 	Set&lt;Integer&gt; <jv>existing</jv> = Set.of(1, 2, 3);
 * 	Set&lt;Integer&gt; <jv>combined</jv> = SetBuilder.<jsm>create</jsm>(Integer.<jk>class</jk>)
 * 		.addAll(<jv>existing</jv>)
 * 		.add(4, 5, 6)
 * 		.build();
 *
 * 	<jc>// Sparse mode - returns null when empty</jc>
 * 	Set&lt;String&gt; <jv>maybeNull</jv> = SetBuilder.<jsm>create</jsm>(String.<jk>class</jk>)
 * 		.sparse()
 * 		.build();  <jc>// Returns null, not empty set</jc>
 * </p>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is <b>not thread-safe</b>. Each builder instance should be used by a single thread or
 * properly synchronized.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonCollections">juneau-common-collections</a>
 * 	<li class='jc'>{@link ListBuilder}
 * 	<li class='jc'>{@link MapBuilder}
 * </ul>
 *
 * @param <E> The element type.
 */
public class SetBuilder<E> {

	private Set<E> set;
	private boolean unmodifiable, sparse;
	private Comparator<E> comparator;

	private Class<E> elementType;
	private List<Converter> converters;

	/**
	 * Static creator.
	 *
	 * @param <E> The element type.
	 * @param elementType The element type.
	 * @param elementTypeArgs Optional element type arguments.
	 * @return A new builder.
	 */
	public static <E> SetBuilder<E> create(Class<E> elementType) {
		return new SetBuilder<>(elementType);
	}

	/**
	 * Constructor.
	 *
	 * @param elementType The element type.
	 * @param elementTypeArgs The element type generic arguments if there are any.
	 */
	public SetBuilder(Class<E> elementType) {
		this.elementType = elementType;
	}

	/**
	 * Specifies the set to append to.
	 *
	 * <p>
	 * If not specified, uses a new {@link LinkedHashSet}.
	 *
	 * @param set The set to append to.
	 * @return This object.
	 */
	public SetBuilder<E> to(Set<E> set) {
		this.set = set;
		return this;
	}

	/**
	 * Adds a single value to this set.
	 *
	 * @param value The value to add to this set.
	 * @return This object.
	 */
	public SetBuilder<E> add(E value) {
		if (set == null)
			set = new LinkedHashSet<>();
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
	public SetBuilder<E> add(E...values) {
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
	public SetBuilder<E> addAll(Collection<E> value) {
		if (nn(value)) {
			if (set == null)
				set = new LinkedHashSet<>(value);
			else
				set.addAll(value);
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
	public SetBuilder<E> addAny(Object...values) {
		if (elementType == null)
			throw new IllegalStateException("Unknown element type. Cannot use this method.");
		if (nn(values)) {
			for (var o : values) {
				if (nn(o)) {
					if (o instanceof Collection) {
						((Collection<?>)o).forEach(x -> addAny(x));
					} else if (isArray(o)) {
						for (int i = 0; i < Array.getLength(o); i++)
							addAny(Array.get(o, i));
					} else if (elementType.isInstance(o)) {
						add(elementType.cast(o));
					} else {
						if (nn(converters)) {
							var e = converters.stream().map(x -> x.convertTo(elementType, o)).filter(x -> nn(x)).findFirst().orElse(null);
							if (nn(e)) {
								add(e);
							} else {
								var l = converters.stream().map(x -> x.convertTo(List.class, o)).filter(x -> nn(x)).findFirst().orElse(null);
								if (nn(l))
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
	 * Registers value converters that can adapt incoming values in {@link #addAny(Object...)}.
	 *
	 * @param values Converters to register. Ignored if {@code null}.
	 * @return This object.
	 */
	public SetBuilder<E> converters(Converter...values) {
		if (values.length == 0)
			return this;
		if (converters == null)
			converters = new ArrayList<>();
		converters.addAll(l(values));
		return this;
	}

	/**
	 * Adds a value to this set if the specified flag is true.
	 *
	 * @param flag The flag.
	 * @param value The value.
	 * @return This object.
	 */
	public SetBuilder<E> addIf(boolean flag, E value) {
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
	public SetBuilder<E> addJson(String...values) {
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
	 * Applies sorting/unmodifiable/sparse options.
	 *
	 * @return The built set or {@code null} if {@link #sparse()} is set and the set is empty.
	 */
	public Set<E> build() {
		if (sparse) {
			if (nn(set) && set.isEmpty())
				set = null;
		} else {
			if (set == null)
				set = new LinkedHashSet<>(0);
		}
		if (nn(set)) {
			if (nn(comparator)) {
				Set<E> s = new TreeSet<>(comparator);
				s.addAll(set);
				set = s;
			}
			if (unmodifiable)
				set = unmodifiableSet(set);
		}
		return set;
	}

	/**
	 * Forces the existing set to be copied instead of appended to.
	 *
	 * @return This object.
	 */
	public SetBuilder<E> copy() {
		if (nn(set))
			set = new LinkedHashSet<>(set);
		return this;
	}

	/**
	 * Specifies the element type on this list.
	 *
	 * @param value The element type.
	 * @return This object.
	 */
	public SetBuilder<E> elementType(Class<E> value) {
		this.elementType = value;
		return this;
	}

	/**
	 * Converts the set into a {@link SortedSet}.
	 *
	 * @return This object.
	 */
	@SuppressWarnings("unchecked")
	public SetBuilder<E> sorted() {
		return sorted((Comparator<E>)Comparator.naturalOrder());
	}

	/**
	 * Converts the set into a {@link SortedSet} using the specified comparator.
	 *
	 * @param comparator The comparator to use for sorting.
	 * @return This object.
	 */
	public SetBuilder<E> sorted(Comparator<E> comparator) {
		this.comparator = comparator;
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
	public SetBuilder<E> sparse() {
		this.sparse = true;
		return this;
	}

	/**
	 * When specified, {@link #build()} will return an unmodifiable set.
	 *
	 * @return This object.
	 */
	public SetBuilder<E> unmodifiable() {
		this.unmodifiable = true;
		return this;
	}
}