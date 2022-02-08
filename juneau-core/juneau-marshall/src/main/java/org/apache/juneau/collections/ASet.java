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

import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;

/**
 * A fluent {@link LinkedHashSet}.
 *
 * <p>
 * Provides various convenience methods for creating and populating a set with minimal code.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// A set of strings.</jc>
 * 	ASet&lt;String&gt; <jv>set</jv> = ASet.<jsm>of</jsm>(<js>"foo"</js>,<js>"bar"</js>);
 *
 * 	<jc>// Append to set.</jc>
 * 	<jv>set</jv>.a(<js>"baz"</js>).a(<js>"qux"</js>);
 *
 * 	<jc>// Create an unmodifiable view of this set.</jc>
 * 	Set&lt;String&gt; <jv>set2</jv> = <jv>set</jv>.unmodifiable();
 *
 * 	<jc>// Convert it to an array.</jc>
 * 	String[] <jv>array</jv> = <jv>set</jv>.asArrayOf(String.<jk>class</jk>);
 *
 * 	<jc>// Convert to simplified JSON.</jc>
 * 	String <jv>json</jv> = <jv>set</jv>.asString();
 *
 * 	<jc>// Convert to XML.</jc>
 * 	String <jv>json</jv> = <jv>set</jv>.asString(XmlSerializer.<jsf>DEFAULT</jsm>);
 * </p>
 *
 * <ul class='spaced-list'>
 * 	<li class='warn'>This class is not thread safe.
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 *
 * @param <T> The entry type.
 * @serial exclude
 */
@SuppressWarnings({"unchecked"})
public class ASet<T> extends LinkedHashSet<T> {

	private static final long serialVersionUID = 1L;

	//------------------------------------------------------------------------------------------------------------------
	// Constructors
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 */
	public ASet() {}

	/**
	 * Copy constructor.
	 *
	 * @param c Initial contents.  Can be <jk>null</jk>.
	 */
	public ASet(Collection<T> c) {
		super(c == null ? emptySet() : c);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Creators
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Convenience method for creating an empty set of objects.
	 *
	 * @return A new set.
	 */
	public static <T> ASet<T> create() {
		return new ASet<>();
	}

	/**
	 * Convenience method for creating a list of objects.
	 *
	 * @param t The initial values.
	 * @return A new list.
	 */
	@SafeVarargs
	public static <T> ASet<T> of(T...t) {
		return new ASet<T>().a(t);
	}

	/**
	 * Convenience method for creating a list of objects.
	 *
	 * @param c The initial values.
	 * @return A new list.
	 */
	public static <T> ASet<T> of(Collection<T> c) {
		return new ASet<T>().a(c);
	}

	/**
	 * Convenience method for creating a set of collection objects.
	 *
	 * @param values The initial values.
	 * @return A new list.
	 */
	public static <T extends Collection<?>> ASet<T> ofCollections(T...values) {
		ASet<T> l = new ASet<>();
		for (T v : values)
			l.add(v);
		return l;
	}

	/**
	 * Convenience method for creating a set of collection objects.
	 *
	 * @param values The initial values.
	 * @return A new list.
	 */
	public static <T> ASet<T[]> ofArrays(T[]...values) {
		ASet<T[]> l = new ASet<>();
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
	public static <T> ASet<T> nullable(Collection<T> values) {
		return values == null ? null : of(values);
	}

	/**
	 * Convenience method for creating an unmodifiable set of objects.
	 *
	 * @param t The initial values.
	 * @return A new list.
	 */
	public static <T> Set<T> unmodifiable(T...t) {
		return t.length == 0 ? emptySet() : of(t).unmodifiable();
	}

	/**
	 * Convenience method for creating an unmodifiable sert out of the specified collection.
	 *
	 * @param c The collection to add.
	 * @param <T> The element type.
	 * @return An unmodifiable set, never <jk>null</jk>.
	 */
	public static <T> Set<T> unmodifiable(Collection<T> c) {
		if (c == null || c.isEmpty())
			return Collections.emptySet();
		return new ASet<T>().a(c).unmodifiable();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Appenders
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Adds the value to this set.
	 *
	 * @param value The value to add to this set.
	 * @return This object.
	 */
	public ASet<T> append(T value) {
		add(value);
		return this;
	}

	/**
	 * Adds all the values in the specified array to this set.
	 *
	 * @param values The values to add to this set.
	 * @return This object.
	 */
	public ASet<T> append(T...values) {
		Collections.addAll(this, values);
		return this;
	}

	/**
	 * Adds all the values in the specified collection to this set.
	 *
	 * @param values The values to add to this set.
	 * @return This object.
	 */
	public ASet<T> append(Collection<? extends T> values) {
		addAll(values);
		return this;
	}

	/**
	 * Same as {@link #append(Object)}.
	 *
	 * @param value The entry to add to this set.
	 * @return This object.
	 */
	public ASet<T> a(T value) {
		return append(value);
	}

	/**
	 * Same as {@link #append(Object[])}.
	 *
	 * @param values The entries to add to this set.
	 * @return This object.
	 */
	public ASet<T> a(T...values) {
		return append(values);
	}

	/**
	 * Same as {@link #append(Collection)}.
	 *
	 * @param values The entries to add to this set.
	 * @return This object.
	 */
	public ASet<T> a(Collection<? extends T> values) {
		return append(values);
	}

	/**
	 * Adds a value to this set if the boolean value is <jk>true</jk>
	 *
	 * @param flag The boolean value.
	 * @param value The value to add.
	 * @return This object.
	 */
	public ASet<T> appendIf(boolean flag, T value) {
		if (flag)
			a(value);
		return this;
	}

	/**
	 * Adds entries to this set skipping <jk>null</jk> values.
	 *
	 * @param values The objects to add to the list.
	 * @return This object.
	 */
	public ASet<T> appendIfNotNull(T...values) {
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
	public ASet<T> appendIf(Predicate<Object> test, T value) {
		return appendIf(test.test(value), value);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other methods
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns an unmodifiable view of this set.
	 *
	 * @return An unmodifiable view of this set.
	 */
	public Set<T> unmodifiable() {
		return isEmpty() ? emptySet() : unmodifiableSet(this);
	}

	/**
	 * Convert the contents of this set into a new array.
	 *
	 * @param c The component type of the array.
	 * @return A new array.
	 */
	public <T2> T2[] asArrayOf(Class<T2> c) {
		return toArray((T2[])Array.newInstance(c, size()));
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
