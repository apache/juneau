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

import java.util.*;

/**
 * An extension of {@link LinkedHashMap} with a convenience {@link #append(Object,Object)} method.
 *
 * <p>
 * Primarily used for testing purposes for quickly creating populated maps.
 * <p class='bcode w800'>
 * 	<jc>// Example:</jc>
 * 	Map&lt;String,Integer&gt; m = <jk>new</jk> AMap&lt;String,Integer&gt;()
 * 		.append(<js>"foo"</js>,1).append(<js>"bar"</js>,2);
 * </p>
 *
 * @param <K> The key type.
 * @param <V> The value type.
 */
public final class AMap<K,V> extends LinkedHashMap<K,V> {

	// TODO - Change to 1L in 8.0
	private static final long serialVersionUID = 7344180601810042208L;

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
	public static <K,V> AMap<K,V> create(K key, V value) {
		return new AMap<K,V>().append(key, value);
	}

	/**
	 * Adds an entry to this map.
	 *
	 * @param k The key.
	 * @param v The value.
	 * @return This object (for method chaining).
	 */
	public AMap<K,V> append(K k, V v) {
		put(k, v);
		return this;
	}
}
