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

import java.util.*;
import java.util.function.*;

/**
 * Combines an {@link Iterable} with a {@link Function} so that you can map
 * entries while iterating over them.
 *
 * @param <I> The unmapped type.
 * @param <E> The mapped type.
 */
public class MappedIterable<I,E> implements Iterable<E> {

	final Iterator<I> i;
	final Function<I,E> f;

	/**
	 * Constructor.
	 *
	 * @param i The original iterable being wrapped.
	 * @param f The function to use to convert from unmapped to mapped types.
	 */
	protected MappedIterable(Iterable<I> i, Function<I,E> f) {
		this.i = i.iterator();
		this.f = f;
	}

	/**
	 * Constructor.
	 *
	 * @param i The original iterable being wrapped.
	 * @param f The function to use to convert from unmapped to mapped types.
	 * @return A new iterable.
	 */
	public static <I,E> Iterable<E> of(Iterable<I> i, Function<I,E> f) {
		return new MappedIterable<>(i, f);
	}

	@Override
	public Iterator<E> iterator() {
		return new Iterator<E>() {
			@Override
			public boolean hasNext() {
				return i.hasNext();
			}
			@Override
			public E next() {
				return f.apply(i.next());
			}
		};
	}
}
