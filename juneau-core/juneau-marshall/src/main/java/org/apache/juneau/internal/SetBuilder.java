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
 * Builder for sets.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @param <E> Element type.
 */
public final class SetBuilder<E> {

	private Set<E> set;
	private boolean unmodifiable, sparse;
	private Comparator<E> comparator;

	private Class<E> elementType;
	private Type[] elementTypeArgs;

	/**
	 * Constructor.
	 *
	 * @param elementType The element type.
	 * @param elementTypeArgs The element type generic arguments if there are any.
	 */
	public SetBuilder(Class<E> elementType, Type...elementTypeArgs) {
		this.elementType = elementType;
		this.elementTypeArgs = elementTypeArgs;
	}

	/**
	 * Constructor.
	 *
	 * @param addTo The set to add to.
	 */
	public SetBuilder(Set<E> addTo) {
		this.set = addTo;
	}

	/**
	 * Builds the set.
	 *
	 * @return A set conforming to the settings on this builder.
	 */
	public Set<E> build() {
		if (sparse) {
			if (set != null && set.isEmpty())
				set = null;
		} else {
			if (set == null)
				set = new LinkedHashSet<>(0);
		}
		if (set != null) {
			if (comparator != null) {
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

	/**
	 * Forces the existing set to be copied instead of appended to.
	 *
	 * @return This object.
	 */
	public SetBuilder<E> copy() {
		if (set != null)
			set = new LinkedHashSet<>(set);
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
	 * Appends the contents of the specified collection into this set.
	 *
	 * <p>
	 * This is a no-op if the value is <jk>null</jk>.
	 *
	 * @param value The collection to add to this set.
	 * @return This object.
	 */
	public SetBuilder<E> addAll(Collection<E> value) {
		if (value != null) {
			if (set == null)
				set = new LinkedHashSet<>(value);
			else
				set.addAll(value);
		}
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
		for (E v : values)
			add(v);
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
}
