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

import static org.apache.juneau.internal.ArrayUtils.*;
import static org.apache.juneau.internal.ThrowableUtils.*;

import java.text.*;
import java.util.*;

/**
 * An instance of a <code>Map</code> where the keys and values
 * 	are simple <code>String[]</code> and <code>Object[]</code> arrays.
 * <p>
 * Typically more efficient than <code>HashMaps</code> for small maps (e.g. &lt;10 entries).
 * <p>
 * Does not support adding or removing entries.
 * <p>
 * Setting values overwrites the value on the underlying value array.
 */
public final class SimpleMap extends AbstractMap<String,Object> {

	private final String[] keys;
	private final Object[] values;
	private final Map.Entry<String,Object>[] entries;

	/**
	 * Constructor.
	 *
	 * @param keys The map keys.  Must not be <jk>null</jk>.
	 * @param values The map values.  Must not be <jk>null</jk>.
	 */
	public SimpleMap(String[] keys, Object[] values) {
		assertFieldNotNull(keys, "keys");
		assertFieldNotNull(values, "values");
		if (keys.length != values.length)
			illegalArg("keys ''{0}'' and values ''{1}'' array lengths differ", keys.length, values.length);

		this.keys = keys;
		this.values = values;
		entries = new SimpleMapEntry[keys.length];
		for (int i = 0; i < keys.length; i++) {
			if (keys[i] == null)
				illegalArg("Keys array cannot contain a null value.");
			entries[i] = new SimpleMapEntry(i);
	}
	}

	@Override /* Map */
	public Set<Map.Entry<String,Object>> entrySet() {
		return asSet(entries);
	}

	@Override /* Map */
	public Object get(Object key) {
		for (int i = 0; i < keys.length; i++)
			if (keys[i].equals(key))
				return values[i];
		return null;
	}

	@Override /* Map */
	public Set<String> keySet() {
		return asSet(keys);
	}

	@Override /* Map */
	public Object put(String key, Object value) {
		for (int i = 0; i < keys.length; i++) {
			if (keys[i].equals(key)) {
				Object v = values[i];
				values[i] = value;
				return v;
			}
		}
		throw new IllegalArgumentException(MessageFormat.format("No key ''{0}'' defined in map", key));
	}

	private class SimpleMapEntry implements Map.Entry<String,Object> {

		private int index;

		private SimpleMapEntry(int index) {
			this.index = index;
		}

		@Override /* Map.Entry */
		public String getKey() {
			return keys[index];
		}

		@Override /* Map.Entry */
		public Object getValue() {
			return values[index];
		}

		@Override /* Map.Entry */
		public Object setValue(Object val) {
			Object v = values[index];
			values[index] = val;
			return v;
		}
	}
}
