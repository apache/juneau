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

import java.lang.reflect.*;
import java.util.*;

/**
 * Builder for arrays.
 *
 * <p>
 * Designed to create arrays without array copying.
 * Initial capacity cannot be exceeded without throwing a {@link ArrayIndexOutOfBoundsException}.
 *
 * @param <T> The array element type.
 */
public class ArrayBuilder<T> {

	private final T[] array;
	private int i = 0;
	private final boolean skipNulls;

	/**
	 * Static creator.
	 *
	 * @param elementType The element type.
	 * @param size The array size.
	 * @param skipNulls If <jk>true</jk>, <jk>null</jk> values will be ignored.
	 * @return A new builder object.
	 */
	public static <T> ArrayBuilder<T> create(Class<T> elementType, int size, boolean skipNulls) {
		return new ArrayBuilder<>(elementType, size, skipNulls);
	}

	/**
	 * Constructor.
	 *
	 * @param elementType The element type.
	 * @param capacity The array size.
	 * @param skipNulls If <jk>true</jk>, <jk>null</jk> values will be ignored.
	 */
	@SuppressWarnings("unchecked")
	public ArrayBuilder(Class<T> elementType, int capacity, boolean skipNulls) {
		array = (T[])Array.newInstance(elementType, capacity);
		this.skipNulls = skipNulls;
	}

	/**
	 * Appends to this array if the specified value is not null.
	 *
	 * @param t The element to add.
	 * @return This object.
	 * @throws ArrayIndexOutOfBoundsException if size is exceeded.
	 */
	public ArrayBuilder<T> add(T t) {
		if (!(skipNulls && t == null))
			array[i++] = t;
		return this;
	}

	/**
	 * Returns the populated array.
	 *
	 * @return The populated array.
	 */
	public T[] toArray() {
		if (i != array.length)
			return Arrays.copyOf(array, i);
		return array;
	}
}
