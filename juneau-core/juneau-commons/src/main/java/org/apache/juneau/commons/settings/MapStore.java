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
package org.apache.juneau.commons.settings;

import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * A writable {@link SettingStore} implementation backed by a thread-safe map.
 *
 * <p>
 * This class provides a mutable store for settings that can be modified at runtime. It's particularly useful
 * for creating custom property stores (e.g., Spring properties, configuration files) that can be added to
 * {@link Settings} as sources.
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is thread-safe. The internal map is lazily initialized using {@link AtomicReference} and
 * {@link ConcurrentHashMap} for thread-safe operations.
 *
 * <h5 class='section'>Null Value Handling:</h5>
 * <p>
 * Setting a value to <c>null</c> stores <c>Optional.empty()</c> in the map, which means {@link #get(String)}
 * will return <c>Optional.empty()</c> (not <c>null</c>). This allows you to explicitly override system properties
 * with null values. Use {@link #unset(String)} if you want to remove a key entirely (so {@link #get(String)} returns <c>null</c>).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a store and add properties</jc>
 * 	MapStore <jv>store</jv> = <jk>new</jk> MapStore();
 * 	<jv>store</jv>.set(<js>"my.property"</js>, <js>"value"</js>);
 * 	<jv>store</jv>.set(<js>"another.property"</js>, <js>"another-value"</js>);
 *
 * 	<jc>// Add to Settings as a source (stores can be used as sources)</jc>
 * 	Settings.<jsf>get</jsf>().addSource(<jv>store</jv>);
 *
 * 	<jc>// Override a system property with null</jc>
 * 	<jv>store</jv>.set(<js>"system.property"</js>, <jk>null</jk>);
 * 	<jc>// get() will now return Optional.empty() for "system.property"</jc>
 *
 * 	<jc>// Remove a property entirely</jc>
 * 	<jv>store</jv>.unset(<js>"my.property"</js>);
 * 	<jc>// get() will now return null for "my.property"</jc>
 * </p>
 */
public class MapStore implements SettingStore {

	private final AtomicReference<Map<String,Optional<String>>> map = new AtomicReference<>();

	/**
	 * Returns a setting from this store.
	 *
	 * <p>
	 * Returns <c>null</c> if the key doesn't exist in the map, or the stored value (which may be
	 * <c>Optional.empty()</c> if the value was explicitly set to <c>null</c>).
	 *
	 * @param key The property name.
	 * @return The property value, <c>null</c> if the key doesn't exist, or <c>Optional.empty()</c> if the key
	 * 	exists but has a null value.
	 */
	@Override
	public Optional<String> get(String key) {
		var m = map.get();
		if (m == null)
			return null;
		return m.get(key);
	}

	/**
	 * Sets a setting in this store.
	 *
	 * <p>
	 * The internal map is lazily initialized on the first call to this method. Setting a value to <c>null</c>
	 * stores <c>Optional.empty()</c> in the map, which means {@link #get(String)} will return <c>Optional.empty()</c>
	 * (not <c>null</c>). This allows you to explicitly override system properties with null values.
	 *
	 * @param key The property name.
	 * @param value The property value, or <c>null</c> to set an empty override.
	 */
	@Override
	public void set(String key, String value) {
		var m = map.get();
		if (m == null) {
			var newMap = new ConcurrentHashMap<String,Optional<String>>();
			m = map.compareAndSet(null, newMap) ? newMap : map.get();  // Not easily testable.
		}
		m.put(key, opt(value));
	}

	/**
	 * Clears all entries from this store.
	 *
	 * <p>
	 * After calling this method, all keys will be removed from the map, and {@link #get(String)} will return
	 * <c>null</c> for all keys.
	 */
	@Override
	public void clear() {
		var m = map.get();
		if (m != null)
			m.clear();
	}

	/**
	 * Removes a setting from this store.
	 *
	 * <p>
	 * After calling this method, {@link #get(String)} will return <c>null</c> for the specified key,
	 * indicating that the key doesn't exist in this store (as opposed to returning <c>Optional.empty()</c>,
	 * which would indicate the key exists but has a null value).
	 *
	 * @param name The property name to remove.
	 */
	@Override
	public void unset(String name) {
		var m = map.get();
		if (m != null)
			m.remove(name);
	}

}
