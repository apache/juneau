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
package org.apache.juneau.utils;

import static java.util.Collections.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.internal.*;

/**
 * An extension of {@link LinkedList} with a convenience {@link #append(Object)} method.
 *
 * <p>
 * Primarily used for testing purposes for quickly creating populated lists.
 * <p class='bcode w800'>
 * 	<jc>// Example:</jc>
 * 	List&lt;String&gt; l = <jk>new</jk> AList&lt;String&gt;().append(<js>"foo"</js>).append(<js>"bar"</js>);
 * </p>
 *
 * @param <T> The entry type.
 */
@SuppressWarnings({"unchecked"})
public final class AList<T> extends ArrayList<T> {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * <p>
	 * Creates an array list of default size.
	 * @param capacity Initial capacity.
	 */
	public AList(int capacity) {
		super(capacity);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * Creates an array list of default size.
	 */
	public AList() {}

	/**
	 * Convenience method for creating an empty list of objects.
	 *
	 * <p>
	 * Creates an array list of default size.
	 *
	 * @return A new list.
	 */
	public static <T> AList<T> create() {
		return new AList<>();
	}

	/**
	 * Convenience method for creating a list of objects.
	 *
	 * <p>
	 * Creates a list with the same capacity as the array.
	 *
	 * @param t The initial values.
	 * @return A new list.
	 */
	@SafeVarargs
	public static <T> AList<T> create(T...t) {
		return new AList<T>(t.length).appendAll(t);
	}

	/**
	 * Convenience method for creating a list of objects.
	 *
	 * <p>
	 * Creates a list with the same capacity as the array.
	 *
	 * @param c The initial values.
	 * @return A new list.
	 */
	public static <T> AList<T> create(Collection<T> c) {
		c = c == null ? emptyList() : c;
		return new AList<T>(c.size()).appendAll(c);
	}

	/**
	 * Creates a list if the collection being added is not null.
	 *
	 * @param c The initial values.
	 * @return A new list, or <jk>null</jk> if the collection is null.
	 */
	public static <T> AList<T> createOrNull(Collection<T> c) {
		return c == null ? null : create(c);
	}

	/**
	 * Convenience method for creating an unmodifiable list of objects.
	 *
	 * <p>
	 * Creates a list with the same capacity as the array.
	 *
	 * @param t The initial values.
	 * @return A new list.
	 */
	public static <T> List<T> createUnmodifiable(T...t) {
		return t.length == 0 ? emptyList() : create(t).unmodifiable();
	}

	/**
	 * Convenience method for creating an unmodifiable list out of the specified collection.
	 *
	 * @param c The collection to add.
	 * @param <T> The element type.
	 * @return An unmodifiable list, never <jk>null</jk>.
	 */
	public static <T> List<T> createUnmodifiable(Collection<T> c) {
		if (c == null || c.isEmpty())
			return Collections.emptyList();
		return new AList<T>(c.size()).appendAll(c).unmodifiable();
	}

	/**
	 * Convenience method for creating a list of objects.
	 *
	 * <p>
	 * Identical to {@link #create(Object...)}.
	 *
	 * @param t The initial values.
	 * @return A new list.
	 */
	@SafeVarargs
	public static <T> AList<T> of(T...t) {
		return new AList<T>(t.length).appendAll(t);
	}

	/**
	 * Adds an entry to this list.
	 *
	 * @param t The entry to add to this list.
	 * @return This object (for method chaining).
	 */
	public AList<T> append(T t) {
		add(t);
		return this;
	}

	/**
	 * Adds multiple entries to this list.
	 *
	 * @param t The entries to add to this list.
	 * @return This object (for method chaining).
	 */
	public AList<T> appendAll(T...t) {
		Collections.addAll(this, t);
		return this;
	}

	/**
	 * Adds an entry to this list if the boolean flag is <jk>true</jk>.
	 *
	 * @param b The boolean flag.
	 * @param val The value to add.
	 * @return This object (for method chaining).
	 */
	public AList<T> appendIf(boolean b, T val) {
		if (b)
			append(val);
		return this;
	}

	/**
	 * Returns an unmodifiable view of this list.
	 *
	 * @return An unmodifiable view of this list.
	 */
	public List<T> unmodifiable() {
		return isEmpty() ? emptyList() : unmodifiableList(this);
	}

	/**
	 * Adds all the entries in the specified collection to this list.
	 *
	 * @param c The collection to add to this list.
	 * @return This object (for method chaining).
	 */
	public AList<T> appendAll(Collection<T> c) {
		addAll(c);
		return this;
	}

	/**
	 * Adds all the entries in the specified collection to this list in reverse order.
	 *
	 * @param c The collection to add to this list.
	 * @return This object (for method chaining).
	 */
	public AList<T> appendReverse(List<? extends T> c) {
		for (ListIterator<? extends T> i = c.listIterator(c.size()); i.hasPrevious();)
			add(i.previous());
		return this;
	}

	/**
	 * Adds the contents of the array to the list in reverse order.
	 *
	 * <p>
	 * i.e. add values from the array from end-to-start order to the end of the list.
	 *
	 * @param c The collection to add to this list.
	 * @return This object (for method chaining).
	 */
	public AList<T> appendReverse(T[] c) {
		for (int i = c.length - 1; i >= 0; i--)
			add(c[i]);
		return this;
	}

	/**
	 * Convert the contents of this list into a new array.
	 *
	 * @param c The component type of the array.
	 * @return A new array.
	 */
	public <T2> T2[] asArrayOf(Class<T2> c) {
		return toArray((T2[])Array.newInstance(c, size()));
	}

	/**
	 * Returns a reverse iterable over this list.
	 *
	 * @return An iterable over the collection.
	 */
	public Iterable<T> riterable() {
		return new ReverseIterable<>(this);
	}
}
