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

import java.util.*;

import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;

/**
 * A fluent {@link TreeMap}.
 *
 * <p>
 * Provides various convenience methods for creating and populating a sorted map with minimal code.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// A map of string key/value pairs.</jc>
 * 	AMap&lt;String,String&gt; m = AMap.<jsm>of</jsm>(<js>"foo"</js>,<js>"bar"</js>);
 *
 * 	<jc>// Append to map.</jc>
 * 	m.a(<js>"baz"</js>, <js>"qux"</js>);
 *
 * 	<jc>// Create an unmodifiable view of this list.</jc>
 * 	Map&lt;String,String&gt; m2 = m.unmodifiable();
 *
 * 	<jc>// Convert to simplified JSON.</jc>
 * 	String json = m.asString();
 *
 * 	<jc>// Convert to XML.</jc>
 * 	String json = m.asString(XmlSerializer.<jsf>DEFAULT</jsm>);
 * </p>
 *
 * @param <K> The key type.
 * @param <V> The value type.
 */
public final class ASortedMap<K,V> extends TreeMap<K,V> {

	private static final long serialVersionUID = 1L;

	//------------------------------------------------------------------------------------------------------------------
	// Constructors.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 */
	public ASortedMap() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param c Comparator to use for key comparison.
	 */
	public ASortedMap(Comparator<K> c) {
		super(c);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copy The map to copy.
	 */
	public ASortedMap(Map<K,V> copy) {
		super(copy == null ? emptyMap() : copy);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Creators.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Creates an empty map.
	 *
	 * @return A new empty map.
	 */
	public static <K,V> ASortedMap<K,V> of() {
		return new ASortedMap<>();
	}

	/**
	 * Creates a map with one entry.
	 *
	 * @param key Entry key.
	 * @param value Entry value.
	 * @return A new map with one entry.
	 */
	public static <K,V> ASortedMap<K,V> of(K key, V value) {
		return new ASortedMap<K,V>().a(key, value);
	}

	/**
	 * Creates a new map initialized with the specified contents.
	 *
	 * @param copy Initialize with these contents.  Can be <jk>null</jk>.
	 * @return A new map.  Never <jk>null</jk>.
	 */
	public static <K,V> ASortedMap<K,V> of(Map<K,V> copy) {
		return new ASortedMap<>(copy);
	}

	/**
	 * Convenience method for creating an unmodifiable list out of the specified collection.
	 *
	 * @param c The collection to add.
	 * @return An unmodifiable list, never <jk>null</jk>.
	 */
	public static <K,V> SortedMap<K,V> unmodifiable(Map<K,V> c) {
		if (c == null || c.isEmpty())
			return Collections.emptySortedMap();
		return new ASortedMap<>(c).unmodifiable();
	}

	/**
	 * Creates a copy of the collection if it's not <jk>null</jk>.
	 *
	 * @param c The initial values.
	 * @return A new list, or <jk>null</jk> if the collection is <jk>null</jk>.
	 */
	public static <K,V> ASortedMap<K,V> nullable(Map<K,V> c) {
		return c == null ? null : of(c);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Appenders.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Add.
	 *
	 * <p>
	 * Adds an entry to this map.
	 *
	 * @param k The key.
	 * @param v The value.
	 * @return This object (for method chaining).
	 */
	public ASortedMap<K,V> a(K k, V v) {
		put(k, v);
		return this;
	}

	/**
	 * Add all.
	 *
	 * <p>
	 * Appends all the entries in the specified map to this map.
	 *
	 * @param c The map to copy.
	 * @return This object (for method chaining).
	 */
	public ASortedMap<K,V> aa(Map<K,V> c) {
		if (c != null)
			super.putAll(c);
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other methods.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns an unmodifiable view of this set.
	 *
	 * @return An unmodifiable view of this set.
	 */
	public SortedMap<K,V> unmodifiable() {
		return isEmpty() ? emptySortedMap() : unmodifiableSortedMap(this);
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
