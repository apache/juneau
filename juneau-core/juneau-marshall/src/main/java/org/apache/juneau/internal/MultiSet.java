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

import static org.apache.juneau.assertions.Assertions.*;
import java.util.*;

/**
 * Encapsulates multiple collections so they can be iterated over as if they were all part of the same collection.
 *
 * @param <E> The object type of this set.
 */
public class MultiSet<E> extends AbstractSet<E> {

	/** Inner collections. */
	final List<Collection<E>> l = new ArrayList<>();

	/**
	 * Create a new Set that consists as a coalesced set of the specified collections.
	 *
	 * @param c Zero or more collections to add to this set.
	 */
	@SafeVarargs
	public MultiSet(Collection<E>...c) {
		for (Collection<E> cc : c)
			append(cc);
	}

	/**
	 * Appends the specified collection to this set of collections.
	 *
	 * @param c The collection to append to this set of collections.
	 * @return This object (for method chaining).
	 */
	public MultiSet<E> append(Collection<E> c) {
		assertArgNotNull("c", c);
		l.add(c);
		return this;
	}

	/**
	 * Iterates over all entries in all collections.
	 */
	@Override /* Set */
	public Iterator<E> iterator() {
		return new Iterator<E>() {
			int i = 0;
			Iterator<E> i2 = (l.size() > 0 ? l.get(i++).iterator() : null);

			@Override /* Iterator */
			public boolean hasNext() {
				if (i2 == null)
					return false;
				if (i2.hasNext())
					return true;
				for (int j = i; j < l.size(); j++)
					if (l.get(j).size() > 0)
						return true;
				return false;
			}

			@Override /* Iterator */
			public E next() {
				if (i2 == null)
					throw new NoSuchElementException();
				while (! i2.hasNext()) {
					if (i >= l.size())
						throw new NoSuchElementException();
					i2 = l.get(i++).iterator();
				}
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

	/**
	 * Enumerates over all entries in all collections.
	 *
	 * @return An enumeration wrapper around this set.
	 */
	public Enumeration<E> enumerator() {
		return Collections.enumeration(this);
	}

	@Override /* Set */
	public int size() {
		int i = 0;
		for (Collection<E> c : l)
			i += c.size();
		return i;
	}
}