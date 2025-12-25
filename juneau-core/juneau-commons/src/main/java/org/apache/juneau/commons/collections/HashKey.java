/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.commons.collections;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;

/**
 * Represents a composite key composed of multiple values, suitable for use as a key in hash-based collections.
 *
 * <p>
 * This class provides an immutable, thread-safe way to create composite keys from multiple values.
 * It's commonly used for caching scenarios where you need to uniquely identify objects based on
 * multiple configuration parameters or attributes.
 *
 * <h5 class='section'>Usage Pattern:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a composite key from multiple values</jc>
 * 	HashKey <jv>key</jv> = HashKey.<jsm>of</jsm>(
 * 		<js>"config1"</js>,
 * 		<js>"config2"</js>,
 * 		<jk>true</jk>,
 * 		<jk>42</jk>
 * 	);
 *
 * 	<jc>// Use as a key in a cache or map</jc>
 * 	Map&lt;HashKey, MyObject&gt; <jv>cache</jv> = <jk>new</jk> HashMap&lt;&gt;();
 * 	<jv>cache</jv>.<jsm>put</jsm>(<jv>key</jv>, <jv>myObject</jv>);
 *
 * 	<jc>// Retrieve using an equivalent key</jc>
 * 	HashKey <jv>lookupKey</jv> = HashKey.<jsm>of</jsm>(<js>"config1"</js>, <js>"config2"</js>, <jk>true</jk>, <jk>42</jk>);
 * 	MyObject <jv>cached</jv> = <jv>cache</jv>.<jsm>get</jsm>(<jv>lookupKey</jv>);  <jc>// Returns myObject</jc>
 * </p>
 *
 * <h5 class='section'>Important Notes:</h5>
 * <ul class='spaced-list'>
 * 	<li>
 * 		<b>All relevant values must be included</b> - When using {@code HashKey} for caching, ensure all
 * 		values that affect the object's identity are included. Missing values can cause different objects
 * 		to incorrectly share the same cache entry.
 * 	<li>
 * 		<b>Order matters</b> - The order of arguments in {@link #of(Object...)} is significant.
 * 		Two keys with the same values in different orders will not be equal.
 * 	<li>
 * 		<b>Immutable</b> - Once created, a {@code HashKey} cannot be modified. This ensures keys
 * 		remain stable when used in hash-based collections.
 * 	<li>
 * 		<b>Thread-safe</b> - This class is thread-safe and can be safely used as keys in concurrent
 * 		collections and caches.
 * 	<li>
 * 		<b>Null values are supported</b> - {@code null} values can be included in the key and are
 * 		handled correctly in equality comparisons.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create keys for caching based on configuration</jc>
 * 	HashKey <jv>key1</jv> = HashKey.<jsm>of</jsm>(<js>"json"</js>, <jk>true</jk>, <jk>false</jk>);
 * 	HashKey <jv>key2</jv> = HashKey.<jsm>of</jsm>(<js>"json"</js>, <jk>true</jk>, <jk>false</jk>);
 * 	HashKey <jv>key3</jv> = HashKey.<jsm>of</jsm>(<js>"json"</js>, <jk>true</jk>, <jk>true</jk>);
 *
 * 	<jc>// key1 and key2 are equal (same values in same order)</jc>
 * 	<jv>key1</jv>.<jsm>equals</jsm>(<jv>key2</jv>);  <jc>// true</jc>
 * 	<jv>key1</jv>.<jsm>hashCode</jsm>() == <jv>key2</jv>.<jsm>hashCode</jsm>();  <jc>// true</jc>
 *
 * 	<jc>// key1 and key3 are different (different values)</jc>
 * 	<jv>key1</jv>.<jsm>equals</jsm>(<jv>key3</jv>);  <jc>// false</jc>
 *
 * 	<jc>// Use in a cache</jc>
 * 	Cache&lt;HashKey, Processor&gt; <jv>processorCache</jv> = Cache.<jsm>create</jsm>();
 * 	<jv>processorCache</jv>.<jsm>put</jsm>(<jv>key1</jv>, <jk>new</jk> JsonProcessor());
 * 	Processor <jv>p</jv> = <jv>processorCache</jv>.<jsm>get</jsm>(<jv>key2</jv>);  <jc>// Returns cached instance</jc>
 * </p>
 */
public class HashKey {

	/**
	 * Creates a new hash key from the specified values.
	 *
	 * <p>
	 * The order of arguments is significant - two calls with the same values in the same order
	 * will produce equal {@code HashKey} instances, while different orders or values will produce
	 * different keys.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	HashKey <jv>key1</jv> = HashKey.<jsm>of</jsm>(<js>"a"</js>, <js>"b"</js>, <jk>true</jk>);
	 * 	HashKey <jv>key2</jv> = HashKey.<jsm>of</jsm>(<js>"a"</js>, <js>"b"</js>, <jk>true</jk>);
	 * 	<jv>key1</jv>.<jsm>equals</jsm>(<jv>key2</jv>);  <jc>// true</jc>
	 *
	 * 	HashKey <jv>key3</jv> = HashKey.<jsm>of</jsm>(<js>"a"</js>, <js>"b"</js>, <jk>false</jk>);
	 * 	<jv>key1</jv>.<jsm>equals</jsm>(<jv>key3</jv>);  <jc>// false</jc>
	 *
	 * 	<jc>// Order matters</jc>
	 * 	HashKey <jv>key4</jv> = HashKey.<jsm>of</jsm>(<js>"b"</js>, <js>"a"</js>, <jk>true</jk>);
	 * 	<jv>key1</jv>.<jsm>equals</jsm>(<jv>key4</jv>);  <jc>// false (different order)</jc>
	 * </p>
	 *
	 * @param array The values that compose this composite key.
	 * 	All values that affect the key's identity should be included.
	 * 	<br>Can be empty (produces a key representing no values).
	 * 	<br>Can contain {@code null} values (handled correctly in equality comparisons).
	 * @return A new immutable hash key instance.
	 */
	public static HashKey of(Object...array) {
		return new HashKey(array);
	}

	private final int hashCode;
	private final Object[] array;

	HashKey(Object[] array) {
		this.array = array;
		this.hashCode = Arrays.deepHashCode(array);
	}

	/**
	 * Compares this hash key with another object for equality.
	 *
	 * <p>
	 * Two {@code HashKey} instances are considered equal if they contain the same values in the same order.
	 * The comparison uses deep equality checking for array elements via {@link org.apache.juneau.commons.utils.Utils#eq(Object, Object)}.
	 *
	 * <p>
	 * This method does not perform null or type checking - it assumes the caller has verified the object
	 * is a non-null {@code HashKey} instance. Passing {@code null} or a non-{@code HashKey} object will
	 * result in a {@code ClassCastException} or {@code NullPointerException}.
	 *
	 * @param o The object to compare with (must be a non-null {@code HashKey} instance).
	 * @return {@code true} if the objects are equal, {@code false} otherwise.
	 */
	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof HashKey))
			return false;
		var x = (HashKey)o;
		if (array.length != x.array.length)
			return false;
		for (var i = 0; i < array.length; i++)
			if (ne(array[i], x.array[i]))
				return false;
		return true;
	}

	/**
	 * Returns the hash code for this hash key.
	 *
	 * <p>
	 * The hash code is computed from all values in the key using {@link Arrays#deepHashCode(Object[])}.
	 * This ensures that equal keys have equal hash codes, making {@code HashKey} suitable for use
	 * as keys in hash-based collections like {@link java.util.HashMap} and {@link java.util.HashSet}.
	 * Arrays with the same contents (but different references) will produce the same hash code.
	 *
	 * @return The hash code value for this object.
	 */
	@Override
	public int hashCode() {
		return hashCode;
	}

	protected FluentMap<String,Object> properties() {
		// @formatter:off
		return filteredBeanPropertyMap()
			.a("hashCode", hashCode())
			.a("array", array);
		// @formatter:on
	}

	@Override /* Overridden from Object */
	public String toString() {
		return r(properties());
	}
}