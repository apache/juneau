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

import static org.apache.juneau.common.internal.ArgUtils.*;
import static org.apache.juneau.internal.ArrayUtils.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * An instance of a <c>Map</c> where the keys and values are simple arrays.
 *
 * <p>
 * Typically more efficient than <c>HashMaps</c> for small maps (e.g. &lt;10 entries).
 *
 * <p>
 * Does not support adding or removing entries.
 *
 * <p>
 * Setting values overwrites the value on the underlying value array.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @param <K> The key type.
 * @param <V> The value type.
 */
public final class SimpleMap<K,V> extends AbstractMap<K,V> {

	final K[] keys;
	final V[] values;
	final SimpleMapEntry[] entries;

	/**
	 * Constructor.
	 *
	 * @param keys The map keys.  Must not be <jk>null</jk>.
	 * @param values The map values.  Must not be <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	public SimpleMap(K[] keys, V[] values) {
		assertArgNotNull("keys", keys);
		assertArgNotNull("values", values);
		assertArg(keys.length == values.length, "keys ''{0}'' and values ''{1}'' array lengths differ", keys.length, values.length);

		this.keys = keys;
		this.values = values;
		entries = (SimpleMapEntry[]) Array.newInstance(SimpleMapEntry.class, keys.length);
		for (int i = 0; i < keys.length; i++) {
			assertArg(keys[i] != null, "Keys array cannot contain a null value.");
			entries[i] = new SimpleMapEntry(i);
	}
	}

	@Override /* Map */
	public Set<Map.Entry<K,V>> entrySet() {
		return asSet(entries);
	}

	@Override /* Map */
	public V get(Object key) {
		for (int i = 0; i < keys.length; i++)
			if (keys[i].equals(key))
				return values[i];
		return null;
	}

	@Override /* Map */
	public Set<K> keySet() {
		return asSet(keys);
	}

	@Override /* Map */
	public V put(K key, V value) {
		for (int i = 0; i < keys.length; i++) {
			if (keys[i].equals(key)) {
				V v = values[i];
				values[i] = value;
				return v;
			}
		}
		throw new IllegalArgumentException("No key '"+key+"' defined in map");
	}

	final class SimpleMapEntry implements Map.Entry<K,V> {

		private int index;

		SimpleMapEntry(int index) {
			this.index = index;
		}

		@Override /* Map.Entry */
		public K getKey() {
			return keys[index];
		}

		@Override /* Map.Entry */
		public V getValue() {
			return values[index];
		}

		@Override /* Map.Entry */
		public V setValue(V val) {
			V v = values[index];
			values[index] = val;
			return v;
		}
	}
}
