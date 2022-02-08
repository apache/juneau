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
import static org.apache.juneau.internal.ThrowableUtils.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;

/**
 * A fluent {@link LinkedHashMap}.
 *
 * <p>
 * Provides various convenience methods for creating and populating a map with minimal code.
 *
 * <h5 class='figure'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// A map of string key/value pairs.</jc>
 * 	AMap&lt;String,String&gt; <jv>map</jv> = AMap.<jsm>of</jsm>(<js>"foo"</js>,<js>"bar"</js>);
 *
 * 	<jc>// Append to map.</jc>
 * 	<jv>map</jv>.a(<js>"baz"</js>, <js>"qux"</js>);
 *
 * 	<jc>// Create an unmodifiable view of this list.</jc>
 * 	Map&lt;String,String&gt; <jv>map2</jv> = <jv>map</jv>.unmodifiable();
 *
 * 	<jc>// Convert to simplified JSON.</jc>
 * 	String <jv>json</jv> = <jv>map</jv>.asString();
 *
 * 	<jc>// Convert to XML.</jc>
 * 	String <jv>json</jv> = <jv>msp</jv>.asString(XmlSerializer.<jsf>DEFAULT</jsm>);
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
 * @param <K> The key type.
 * @param <V> The value type.
 * @serial exclude
 */
public class AMap<K,V> extends LinkedHashMap<K,V> {

	private static final long serialVersionUID = 1L;

	//------------------------------------------------------------------------------------------------------------------
	// Constructors
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 */
	public AMap() {}

	/**
	 * Copy constructor.
	 *
	 * @param copy The map to copy.  Can be <jk>null</jk>.
	 */
	public AMap(Map<K,V> copy) {
		super(copy == null ? emptyMap() : copy);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Creators
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Creates an empty map.
	 *
	 * @return A new empty map.
	 */
	public static <K,V> AMap<K,V> create() {
		return new AMap<>();
	}

	/**
	 * Creates a map with one entry.
	 *
	 * @param key Entry key.
	 * @param value Entry value.
	 * @return A new map with one entry.
	 */
	public static <K,V> AMap<K,V> of(K key, V value) {
		return new AMap<K,V>().a(key, value);
	}

	/**
	 * Creates a map out of a list of key/value pairs.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param parameters
	 * 	The parameters.
	 * 	<br>Must be an even number of parameters.
	 * 	<br>It's up to you to ensure that the parameters are the correct type.
	 * @return A new map.
	 */
	@SuppressWarnings("unchecked")
	public static <K,V> AMap<K,V> ofPairs(Object...parameters) {
		AMap<K,V> m = AMap.create();
		if (parameters.length % 2 != 0)
			throw runtimeException("Odd number of parameters passed into AMap.ofPairs()");
		for (int i = 0; i < parameters.length; i+=2)
			m.put((K)parameters[i], (V)parameters[i+1]);
		return m;
	}


	@SuppressWarnings("javadoc")
	public static <K,V> AMap<K,V> of(K k1, V v1, K k2, V v2) {
		return AMap.of(k1,v1).a(k2,v2);
	}

	@SuppressWarnings("javadoc")
	public static <K,V> AMap<K,V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
		return AMap.of(k1,v1).a(k2,v2).a(k3,v3);
	}

	@SuppressWarnings("javadoc")
	public static <K,V> AMap<K,V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
		return AMap.of(k1,v1).a(k2,v2).a(k3,v3).a(k4,v4);
	}

	@SuppressWarnings("javadoc")
	public static <K,V> AMap<K,V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
		return AMap.of(k1,v1).a(k2,v2).a(k3,v3).a(k4,v4).a(k5,v5);
	}

	@SuppressWarnings("javadoc")
	public static <K,V> AMap<K,V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6) {
		return AMap.of(k1,v1).a(k2,v2).a(k3,v3).a(k4,v4).a(k5,v5).a(k6,v6);
	}

	@SuppressWarnings("javadoc")
	public static <K,V> AMap<K,V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7) {
		return AMap.of(k1,v1).a(k2,v2).a(k3,v3).a(k4,v4).a(k5,v5).a(k6,v6).a(k7,v7);
	}

	@SuppressWarnings("javadoc")
	public static <K,V> AMap<K,V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8) {
		return AMap.of(k1,v1).a(k2,v2).a(k3,v3).a(k4,v4).a(k5,v5).a(k6,v6).a(k7,v7).a(k8,v8);
	}

	/**
	 * Creates a new map initialized with the specified contents.
	 *
	 * @param copy Initialize with these contents.  Can be <jk>null</jk>.
	 * @return A new map.  Never <jk>null</jk>.
	 */
	public static <K,V> AMap<K,V> of(Map<K,V> copy) {
		return new AMap<>(copy);
	}

	/**
	 * Creates an unmodifiable copy of the specified map.
	 *
	 * @param copy The map to copy.
	 * @return A new unmodifiable map, never <jk>null</jk>.
	 */
	public static <K,V> Map<K,V> unmodifiable(Map<K,V> copy) {
		if (copy == null || copy.isEmpty())
			return emptyMap();
		return new AMap<>(copy).unmodifiable();
	}

	/**
	 * Creates a copy of the collection if it's not <jk>null</jk>.
	 *
	 * @param c The initial values.
	 * @return A new list, or <jk>null</jk> if the collection is <jk>null</jk>.
	 */
	public static <K,V> AMap<K,V> nullable(Map<K,V> c) {
		return c == null ? null : of(c);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Appenders
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Adds an entry to this map.
	 *
	 * @param key The key.
	 * @param value The value.
	 * @return This object.
	 */
	public AMap<K,V> append(K key, V value) {
		put(key, value);
		return this;
	}

	/**
	 * Appends all the entries in the specified map to this map.
	 *
	 * @param values The map to copy.
	 * @return This object.
	 */
	public AMap<K,V> append(Map<K,V> values) {
		super.putAll(values);
		return this;
	}

	/**
	 * Same as {@link #append(Object,Object)}.
	 *
	 * @param key The key.
	 * @param value The value.
	 * @return This object.
	 */
	public AMap<K,V> a(K key, V value) {
		return append(key, value);
	}

	/**
	 * Same as {@link #append(Map)}.
	 *
	 * @param values The map to copy.
	 * @return This object.
	 */
	public AMap<K,V> a(Map<K,V> values) {
		return append(values);
	}

	/**
	 * Add if flag is <jk>true</jk>.
	 *
	 * @param flag The flag to check.
	 * @param key The key.
	 * @param value The value.
	 * @return This object.
	 */
	public AMap<K,V> appendIf(boolean flag, K key, V value) {
		if (flag)
			append(key, value);
		return this;
	}

	/**
	 * Add if predicate matches value.
	 *
	 * @param test The predicate to match against.
	 * @param key The key.
	 * @param value The value.
	 * @return This object.
	 */
	public AMap<K,V> appendIf(Predicate<Object> test, K key, V value) {
		return appendIf(test.test(value), key, value);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other methods
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns an unmodifiable view of this map.
	 *
	 * @return An unmodifiable view of this map.
	 */
	public Map<K,V> unmodifiable() {
		return this.isEmpty() ? emptyMap() : unmodifiableMap(this);
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
