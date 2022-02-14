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
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 *
 * @param <T> The array element type.
 */
public final class ArrayBuilder<T> {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Static creator.
	 *
	 * @param elementType The element type.
	 * @return A new builder object.
	 */
	public static <T> ArrayBuilder<T> of(Class<T> elementType) {
		return new ArrayBuilder<>(elementType);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private Predicate<T> filter;
	private final Class<T> elementType;
	private int size = -1;
	private int i = 0;
	private List<T> list;

	/**
	 * Constructor.
	 *
	 * @param elementType The element type.
	 */
	public ArrayBuilder(Class<T> elementType) {
		this.elementType = elementType;
	}

	/**
	 * Sets the expected size for this array.
	 *
	 * @param value The new value for this setting.
	 * @return This object.
	 */
	public ArrayBuilder<T> size(int value) {
		size = value;
		return this;
	}

	/**
	 * The predicate to use to filter values added to this builder.
	 *
	 * @param value The new value for this setting.
	 * @return This object.
	 */
	public ArrayBuilder<T> filter(Predicate<T> value) {
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
	public ArrayBuilder<T> add(T t) {
		if (passes(filter, t)) {
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
	public T[] orElse(T[] def) {
		if (list == null)
			return def;
		T[] t = (T[]) Array.newInstance(elementType, list == null ? 0 : list.size());
		if (list != null)
			list.toArray(t);
		return t;
	}
}
