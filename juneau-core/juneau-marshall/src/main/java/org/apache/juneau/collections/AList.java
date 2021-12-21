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
import java.util.function.*;

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
 * 	l.append(<js>"baz"</js>, <js>"qux"</js>);
 *
 * 	<jc>// Create an unmodifiable view of this list.</jc>
 * 	List&lt;String&gt; l2 = l.unmodifiable();
 *
 * 	<jc>// Convert it to an array.</jc>
 * 	String[] array = l.asArray();
 *
 * 	<jc>// Convert to simplified JSON.</jc>
 * 	String json = l.asString();
 *
 * 	<jc>// Convert to XML.</jc>
 * 	String json = l.asString(XmlSerializer.<jsf>DEFAULT</jsm>);
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 *
 * @param <T> The entry type.
 */
@SuppressWarnings({"unchecked"})
public class AList<T> extends ArrayList<T> {

	private static final long serialVersionUID = 1L;

	//------------------------------------------------------------------------------------------------------------------
	// Constructors
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
	// Creators
	//------------------------------------------------------------------------------------------------------------------

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
	 * @param values The initial values.
	 * @return A new list.
	 */
	@SafeVarargs
	public static <T> AList<T> of(T...values) {
		return new AList<T>(values.length).a(values);
	}

	/**
	 * Convenience method for creating a list of objects.
	 *
	 * <p>
	 * Creates a list with the same capacity as the array.
	 *
	 * @param values The initial values.
	 * @return A new list.
	 */
	public static <T> AList<T> of(Collection<T> values) {
		values = values == null ? emptyList() : values;
		return new AList<T>(values.size()).a(values);
	}

	/**
	 * Convenience method for creating a list of collection objects.
	 *
	 * @param values The initial values.
	 * @return A new list.
	 */
	public static <T extends Collection<?>> AList<T> ofCollections(T...values) {
		AList<T> l = new AList<>();
		for (T v : values)
			l.add(v);
		return l;
	}

	/**
	 * Convenience method for creating a list of collection objects.
	 *
	 * @param values The initial values.
	 * @return A new list.
	 */
	public static <T> AList<T[]> ofArrays(T[]...values) {
		AList<T[]> l = new AList<>();
		for (T[] v : values)
			l.add(v);
		return l;
	}

	/**
	 * Creates a copy of the collection if it's not <jk>null</jk>.
	 *
	 * @param values The initial values.
	 * @return A new list, or <jk>null</jk> if the collection is <jk>null</jk>.
	 */
	public static <T> AList<T> nullable(Collection<T> values) {
		return values == null ? null : of(values);
	}

	/**
	 * Convenience method for creating an unmodifiable list of objects.
	 *
	 * <p>
	 * Creates a list with the same capacity as the array.
	 *
	 * @param values The initial values.
	 * @return A new list.
	 */
	public static <T> List<T> unmodifiable(T...values) {
		return values.length == 0 ? emptyList() : of(values).unmodifiable();
	}

	/**
	 * Convenience method for creating an unmodifiable list out of the specified collection.
	 *
	 * @param values The collection to add.
	 * @param <T> The element type.
	 * @return An unmodifiable list, never <jk>null</jk>.
	 */
	public static <T> List<T> unmodifiable(Collection<T> values) {
		if (values == null || values.isEmpty())
			return Collections.emptyList();
		return new AList<T>(values.size()).a(values).unmodifiable();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Appenders
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Adds the value to this list.
	 *
	 * @param value The value to add to this list.
	 * @return This object.
	 */
	public AList<T> append(T value) {
		add(value);
		return this;
	}

	/**
	 * Adds all the values in the specified array to this list.
	 *
	 * @param values The values to add to this list.
	 * @return This object.
	 */
	public AList<T> append(T...values) {
		Collections.addAll(this, values);
		return this;
	}

	/**
	 * Adds all the values in the specified collection to this list.
	 *
	 * @param values The values to add to this list.
	 * @return This object.
	 */
	public AList<T> append(Collection<? extends T> values) {
		addAll(values);
		return this;
	}

	/**
	 * Same as {@link #append(Object)}.
	 *
	 * @param value The entry to add to this list.
	 * @return This object.
	 */
	public AList<T> a(T value) {
		return append(value);
	}

	/**
	 * Same as {@link #append(Collection)}.
	 *
	 * @param values The collection to add to this list.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public AList<T> a(Collection<? extends T> values) {
		return append(values);
	}

	/**
	 * Same as {@link #append(Object...)}.
	 *
	 * @param values The array to add to this list.
	 * @return This object.
	 */
	public AList<T> a(T...values) {
		return append(values);
	}

	/**
	 * Adds an entry to this list if the boolean flag is <jk>true</jk>.
	 *
	 * @param flag The boolean flag.
	 * @param value The value to add.
	 * @return This object.
	 */
	public AList<T> appendIf(boolean flag, T value) {
		if (flag)
			a(value);
		return this;
	}

	/**
	 * Adds entries to this list skipping <jk>null</jk> values.
	 *
	 * @param values The objects to add to the list.
	 * @return This object.
	 */
	public AList<T> appendIfNotNull(T...values) {
		for (T o2 : values)
			if (o2 != null)
				a(o2);
		return this;
	}

	/**
	 * Add if predicate matches value.
	 *
	 * @param test The predicate to match against.
	 * @param value The value to add to the list.
	 * @return This object.
	 */
	public AList<T> appendIf(Predicate<Object> test, T value) {
		return appendIf(test.test(value), value);
	}

	/**
	 * Add reverse.
	 *
	 * <p>
	 * Adds all the entries in the specified collection to this list in reverse order.
	 *
	 * @param values The collection to add to this list.
	 * @return This object.
	 */
	public AList<T> appendReverse(List<? extends T> values) {
		for (ListIterator<? extends T> i = values.listIterator(values.size()); i.hasPrevious();)
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
	 * @param values The collection to add to this list.
	 * @return This object.
	 */
	public AList<T> appendReverse(T...values) {
		for (int i = values.length - 1; i >= 0; i--)
			add(values[i]);
		return this;
	}

	/**
	 * Sorts the contents of this list using natural ordering.
	 *
	 * @return This object.
	 */
	public AList<T> sort() {
		super.sort(null);
		return this;
	}

	/**
	 * Sorts the contents of this list using the specified comparator.
	 *
	 * @param c The comparator to use for sorting.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public AList<T> sortWith(Comparator<? super T> c) {
		super.sort(c);
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other methods
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
	public String asJson() {
		return SimpleJsonSerializer.DEFAULT.toString(this);
	}
}
