// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file		*
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance			*
// * with the License.  You may obtain a copy of the License at															  *
// *																														 *
// *  http://www.apache.org/licenses/LICENSE-2.0																			 *
// *																														 *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the		*
// * specific language governing permissions and limitations under the License.											  *
// ***************************************************************************************************************************
package org.apache.juneau.internal;

import java.util.*;

/**
 * Implements a reversed iteration {@link Iterable} without altering the underlying list.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 *
 * @param <T> The entry type.
 */
public class ReverseIterable<T> implements Iterable<T> {
	private final List<T> list;

	/**
	 * Constructor
	 *
	 * @param list The original list.
	 */
	public ReverseIterable(List<T> list) {
		this.list = list;
	}

	/**
	 * Convenience method for constructing instances.
	 *
	 * @param list The original list.
	 * @return A new {@link ReverseIterable}.
	 */
	public static <T> ReverseIterable<T> of(List<T> list) {
		return new ReverseIterable<>(list);
	}

	/**
	 * Convenience method for constructing instances.
	 *
	 * @param list The original list.
	 * @return A new {@link ReverseIterable}.
	 */
	public static <T> ReverseIterable<T> of(T[] list) {
		return new ReverseIterable<>(Arrays.asList(list));
	}

	@Override
	public Iterator<T> iterator() {
		final ListIterator<T> i = list.listIterator(list.size());

		return new Iterator<T>() {
			@Override
			public boolean hasNext() { return i.hasPrevious(); }
			@Override
			public T next() { return i.previous(); }
			@Override
			public void remove() { i.remove(); }
		};
	}
}