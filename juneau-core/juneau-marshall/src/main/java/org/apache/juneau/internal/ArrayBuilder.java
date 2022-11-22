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

import static org.apache.juneau.internal.ConsumerUtils.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

/**
 * Builder for arrays.
 *
 * <p>
 * Designed to create arrays without array copying.
 * Initial capacity cannot be exceeded without throwing a {@link ArrayIndexOutOfBoundsException}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @param <E> The array element type.
 */
public final class ArrayBuilder<E> {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Static creator.
	 *
	 * @param <E> The element type.
	 * @param elementType The element type.
	 * @return A new builder object.
	 */
	public static <E> ArrayBuilder<E> of(Class<E> elementType) {
		return new ArrayBuilder<>(elementType);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private Predicate<E> filter;
	private final Class<E> elementType;
	private int size = -1;
	private int i = 0;
	private List<E> list;

	/**
	 * Constructor.
	 *
	 * @param elementType The element type.
	 */
	public ArrayBuilder(Class<E> elementType) {
		this.elementType = elementType;
	}

	/**
	 * Sets the expected size for this array.
	 *
	 * @param value The new value for this setting.
	 * @return This object.
	 */
	public ArrayBuilder<E> size(int value) {
		size = value;
		return this;
	}

	/**
	 * The predicate to use to filter values added to this builder.
	 *
	 * @param value The new value for this setting.
	 * @return This object.
	 */
	public ArrayBuilder<E> filter(Predicate<E> value) {
		filter = value;
		return this;
	}

	/**
	 * Appends to this array if the specified value is not null.
	 *
	 * @param t The element to add.
	 * @return This object.
	 * @throws ArrayIndexOutOfBoundsException if size is exceeded.
	 */
	public ArrayBuilder<E> add(E t) {
		if (test(filter, t)) {
			if (list == null)
				list = size < 0 ? new ArrayList<>() : new ArrayList<>(size);
			list.add(t);
			i++;
		}
		return this;
	}

	/**
	 * Returns the populated array.
	 *
	 * @param def The default value if no values were added to this builder.
	 * @return A new array containing the added entries.
	 */
	@SuppressWarnings("unchecked")
	public E[] orElse(E[] def) {
		if (list == null)
			return def;
		E[] t = (E[]) Array.newInstance(elementType, list == null ? 0 : list.size());
		if (list != null)
			list.toArray(t);
		return t;
	}
}
