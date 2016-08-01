/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.utils;

import static com.ibm.juno.core.utils.ArrayUtils.*;
import static com.ibm.juno.core.utils.ThrowableUtils.*;

import java.text.*;
import java.util.*;

/**
 * An instance of a <code>Map</code> where the keys and values
 * 	are simple <code>String[]</code> and <code>Object[]</code> arrays.
 * <p>
 * 	Typically more efficient than <code>HashMaps</code> for small maps (e.g. &lt;10 entries).
 * <p>
 * 	Does not support adding or removing entries.
 * <p>
 * 	Setting values overwrites the value on the underlying value array.
 *
 * @author James Bognar (jbognar@us.ibm.com)
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
