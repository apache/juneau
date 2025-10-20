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
package org.apache.juneau.common.utils;

import static java.util.Collections.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.reflect.*;
import java.util.*;

/**
 * Builder for lists with fluent convenience methods.
 *
 * <p>
 * Supports adding single/multiple values, collections, arbitrary inputs (arrays/collections/convertible values),
 * optional sorting/comparators, sparse output (return null when empty), and unmodifiable output.
 *
 * @param <E> The element type
 */
public class ListBuilder<E> {

	/**
	 * Static creator.
	 *
	 * @param <E> The element type.
	 * @param elementType The element type.
	 * @param elementTypeArgs Optional element type arguments.
	 * @return A new builder.
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
	 * Adds arbitrary values to this list.
	 *
	 * <p>
	 * Objects can be any of the following:
	 * <ul>
	 * 	<li>The same type or convertible to the element type of this list.
	 * 	<li>Collections or arrays of anything on this list.
	 * </ul>
	 *
	 * @param values The values to add.
	 * @return This object.
	 */
	public ListBuilder<E> addAny(Object...values) {
		if (elementType == null)
			throw new IllegalStateException("Unknown element type. Cannot use this method.");
			if (values != null) {
				for (Object o : values) {
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