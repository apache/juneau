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

import static org.apache.juneau.internal.ThrowableUtils.*;

import java.util.*;

/**
 * Utility class for defining an iterator over one or more iterables.
 *
 * @param <E> The element class type.
 */
public class MultiIterable<E> implements Iterable<E> {

	final List<Iterator<E>> iterators = new LinkedList<Iterator<E>>();

	/**
	 * Constructor.
	 *
	 * @param iterators The list of iterators to iterate over.
	 */
	@SuppressWarnings("unchecked")
	public MultiIterable(Iterator<E>...iterators) {
		for (Iterator<E> i : iterators)
			append(i);
	}

	/**
	 * Appends the specified iterator to this list of iterators.
	 *
	 * @param iterator The iterator to append.
	 * @return This object (for method chaining).
	 */
	public MultiIterable<E> append(Iterator<E> iterator) {
		assertFieldNotNull(iterator, "iterator");
		this.iterators.add(iterator);
		return this;
	}

	@Override /* Iterable */
	public Iterator<E> iterator() {
		return new Iterator<E>() {
			Iterator<Iterator<E>> i1 = iterators.iterator();
			Iterator<E> i2 = i1.hasNext() ? i1.next() : null;

			@Override /* Iterator */
			public boolean hasNext() {
				while (i2 != null && ! i2.hasNext())
					i2 = (i1.hasNext() ? i1.next() : null);
				return (i2 != null);
			}

			@Override /* Iterator */
			public E next() {
				hasNext();
				if (i2 == null)
					throw new NoSuchElementException();
				return i2.next();
			}

			@Override /* Iterator */
			public void remove() {
				if (i2 == null)
					throw new NoSuchElementException();
				i2.remove();
			}
		};
	}
}