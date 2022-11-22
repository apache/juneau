// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.internal;

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.common.internal.ThrowableUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;
import static java.util.Collections.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.parser.*;

/**
 * Builder for lists.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @param <E> Element type.
 */
public final class ListBuilder<E> {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Static creator.
	 *
	 * @param <E> The element type.
	 * @param elementType The element type.
	 * @param elementTypeArgs Optional element type arguments.
	 * @return A new builder.
	 */
	public static <E> ListBuilder<E> create(Class<E> elementType, Type...elementTypeArgs) {
		return new ListBuilder<>(elementType, elementTypeArgs);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private List<E> list;
	private boolean unmodifiable = false, sparse = false;
	private Comparator<E> comparator;

	private Class<E> elementType;
	private Type[] elementTypeArgs;

	/**
	 * Constructor.
	 *
	 * @param elementType The element type.
	 * @param elementTypeArgs The element type generic arguments if there are any.
	 */
	public ListBuilder(Class<E> elementType, Type...elementTypeArgs) {
		this.elementType = elementType;
		this.elementTypeArgs = elementTypeArgs;
	}

	/**
	 * Constructor.
	 *
	 * @param addTo The list to add to.
	 */
	public ListBuilder(List<E> addTo) {
		this.list = addTo;
	}

	/**
	 * Builds the list.
	 *
	 * @return A list conforming to the settings on this builder.
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
	 * Adds entries to this list via JSON array strings.
	 *
	 * @param values The JSON array strings to parse and add to this list.
	 * @return This object.
	 */
	public ListBuilder<E> addJson(String...values) {
		return addAny((Object[])values);
	}

	/**
	 * Adds arbitrary values to this list.
	 *
	 * <p>
	 * Objects can be any of the following:
	 * <ul>
	 * 	<li>The same type or convertible to the element type of this list.
	 * 	<li>Collections or arrays of anything on this list.
	 * 	<li>JSON array strings parsed and convertible to the element type of this list.
	 * </ul>
	 *
	 * @param values The values to add.
	 * @return This object.
	 */
	public ListBuilder<E> addAny(Object...values) {
		if (elementType == null)
			throw new RuntimeException("Unknown element type.  Cannot use this method.");
		try {
			if (values != null) {
				for (Object o : values) {
					if (o != null) {
						if (o instanceof Collection) {
							((Collection<?>)o).forEach(x -> addAny(x));
						} else if (o.getClass().isArray()) {
							for (int i = 0; i < Array.getLength(o); i++)
								addAny(Array.get(o, i));
						} else if (isJsonArray(o, false)) {
							new JsonList(o.toString()).forEach(x -> addAny(x));
						} else if (elementType.isInstance(o)) {
							add(elementType.cast(o));
						} else {
							add(toType(o, elementType, elementTypeArgs));
						}
					}
				}
			}
		} catch (ParseException e) {
			throw asRuntimeException(e);
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
}
