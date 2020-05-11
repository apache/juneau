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
package org.apache.juneau.collections;

import static java.util.Collections.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;

/**
 * A fluent {@link ArrayList}.
 *
 * <p>
 * Provides various convenience methods for creating and populating a list with minimal code.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// A list of strings.</jc>
 * 	AList&lt;String&gt; l = AList.<jsm>of</jsm>(<js>"foo"</js>,<js>"bar"</js>);
 *
 * 	<jc>// Append to list.</jc>
 * 	l.a(<js>"baz"</js>).a(<js>"qux"</js>);
 *
 * 	<jc>// Create an unmodifiable view of this list.</jc>
 * 	List&lt;String&gt; l2 = l.unmodifiable();
 *
 * 	<jc>// Convert it to an array.</jc>
 * 	String[] array = l.asArrayOf(String.<jk>class</jk>);
 *
 * 	<jc>// Convert to simplified JSON.</jc>
 * 	String json = l.asString();
 *
 * 	<jc>// Convert to XML.</jc>
 * 	String json = l.asString(XmlSerializer.<jsf>DEFAULT</jsm>);
 * </p>
 *
 * @param <T> The entry type.
 */
@SuppressWarnings({"unchecked"})
public final class AList<T> extends ArrayList<T> {

	private static final long serialVersionUID = 1L;

	//------------------------------------------------------------------------------------------------------------------
	// Constructors.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * <p>
	 * Creates an array list of default size.
	 */
	public AList() {}

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
	 * Copy constructor.
	 *
	 * @param c Initial contents.  Can be <jk>null</jk>.
	 */
	public AList(Collection<T> c) {
		super(c == null ? emptyList() : c);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Creators.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Convenience method for creating an empty list of objects.
	 *
	 * <p>
	 * Creates an array list of default size.
	 *
	 * @return A new list.
	 */
	public static <T> AList<T> of() {
		return new AList<>();
	}

	/**
	 * Convenience method for creating a list of objects.
	 *
	 * @param t The initial values.
	 * @return A new list.
	 */
	@SafeVarargs
	public static <T> AList<T> of(T...t) {
		return new AList<T>(t.length).a(t);
	}

	/**
	 * Convenience method for creating a list of objects.
	 *
	 * <p>
	 * Identical to {@link #of(Object...)} but allows you to distinguish from {@link #of(Collection)} when creating
	 * multi-dimensional lists.
	 *
	 * @param t The initial values.
	 * @return A new list.
	 */
	public static <T> AList<T> ofa(T...t) {
		return new AList<T>(t.length).a(t);
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
	public static <T> AList<T> of(Collection<T> c) {
		c = c == null ? emptyList() : c;
		return new AList<T>(c.size()).aa(c);
	}

	/**
	 * Creates a copy of the collection if it's not <jk>null</jk>.
	 *
	 * @param c The initial values.
	 * @return A new list, or <jk>null</jk> if the collection is <jk>null</jk>.
	 */
	public static <T> AList<T> nullable(Collection<T> c) {
		return c == null ? null : of(c);
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
	public static <T> List<T> unmodifiable(T...t) {
		return t.length == 0 ? emptyList() : of(t).unmodifiable();
	}

	/**
	 * Convenience method for creating an unmodifiable list out of the specified collection.
	 *
	 * @param c The collection to add.
	 * @param <T> The element type.
	 * @return An unmodifiable list, never <jk>null</jk>.
	 */
	public static <T> List<T> unmodifiable(Collection<T> c) {
		if (c == null || c.isEmpty())
			return Collections.emptyList();
		return new AList<T>(c.size()).aa(c).unmodifiable();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Appenders.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Add.
	 *
	 * <p>
	 * Adds an entry to this list.
	 *
	 * @param t The entry to add to this list.
	 * @return This object (for method chaining).
	 */
	public AList<T> a(T t) {
		add(t);
		return this;
	}

	/**
	 * Add.
	 *
	 * <p>
	 * Adds multiple entries to this list.
	 *
	 * @param t The entries to add to this list.
	 * @return This object (for method chaining).
	 */
	public AList<T> a(T...t) {
		Collections.addAll(this, t);
		return this;
	}

	/**
	 * Add all.
	 *
	 * <p>
	 * Adds all the entries in the specified collection to this list.
	 *
	 * @param c The collection to add to this list.  Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public AList<T> aa(Collection<? extends T> c) {
		if (c != null)
			addAll(c);
		return this;
	}

	/**
	 * Add if.
	 *
	 * <p>
	 * Adds an entry to this list if the boolean flag is <jk>true</jk>.
	 *
	 * @param b The boolean flag.
	 * @param val The value to add.
	 * @return This object (for method chaining).
	 */
	public AList<T> aif(boolean b, T val) {
		if (b)
			a(val);
		return this;
	}

	/**
	 * Add if not null.
	 *
	 * <p>
	 * Adds entries to this list skipping <jk>null</jk> values.
	 *
	 * @param t The objects to add to the list.
	 * @return This object (for method chaining).
	 */
	public AList<T> aifnn(T...t) {
		for (T o2 : t)
			if (o2 != null)
				a(o2);
		return this;
	}

	/**
	 * Add reverse.
	 *
	 * <p>
	 * Adds all the entries in the specified collection to this list in reverse order.
	 *
	 * @param c The collection to add to this list.
	 * @return This object (for method chaining).
	 */
	public AList<T> arev(List<? extends T> c) {
		for (ListIterator<? extends T> i = c.listIterator(c.size()); i.hasPrevious();)
			add(i.previous());
		return this;
	}

	/**
	 * Add reverse.
	 *
	 * <p>
	 * Adds the contents of the array to the list in reverse order.
	 *
	 * <p>
	 * i.e. add values from the array from end-to-start order to the end of the list.
	 *
	 * @param c The collection to add to this list.
	 * @return This object (for method chaining).
	 */
	public AList<T> arev(T[] c) {
		for (int i = c.length - 1; i >= 0; i--)
			add(c[i]);
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other methods.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns an unmodifiable view of this list.
	 *
	 * @return An unmodifiable view of this list.
	 */
	public List<T> unmodifiable() {
		return isEmpty() ? emptyList() : unmodifiableList(this);
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

	/**
	 * Convert to a string using the specified serializer.
	 *
	 * @param ws The serializer to use to serialize this collection.
	 * @return This collection serialized to a string.
	 */
	public String asString(WriterSerializer ws) {
		return ws.toString(this);
	}

	/**
	 * Convert to Simplified JSON.
	 *
	 * @return This collection serialized to a string.
	 */
	public String asString() {
		return SimpleJsonSerializer.DEFAULT.toString(this);
	}

	/**
	 * Convert to Simplified JSON.
	 */
	@Override /* Object */
	public String toString() {
		return asString(SimpleJsonSerializer.DEFAULT);
	}
}
