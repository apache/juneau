/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.utils;

import static com.ibm.juno.core.utils.ThrowableUtils.*;

import java.util.*;

/**
 * Wrapper around a map where the key names are overridden.
 *
 * @param <K> The key class type.
 * @param <V> The value class type.
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class FilteredMap<K,V> extends AbstractMap<K,V> {

	private Map<K,V> innerMap;
	private Set<Map.Entry<K,V>> entries;

	/**
	 * Constructor.
	 *
	 * @param innerMap The map being wrapped.  Must not be <jk>null</jk>.
	 * @param keys The keys in the new map.  Must not be <jk>null</jk>.
	 */
	public FilteredMap(Map<K,V> innerMap, K[] keys) {
		assertFieldNotNull(innerMap, "innerMap");
		assertFieldNotNull(keys, "keys");

		this.innerMap = innerMap;
			List<Map.Entry<K,V>> l = new ArrayList<Map.Entry<K,V>>(keys.length);
			for (K k : keys)
				if (innerMap.containsKey(k))
					l.add(createEntry(k));
			entries = new ListSet<Map.Entry<K,V>>(l);
		}

	private Map.Entry<K,V> createEntry(final K key) {
		return new Map.Entry<K,V>() {

			@Override /* Map.Entry */
			public K getKey() {
				return key;
			}

			@Override /* Map.Entry */
			public V getValue() {
				return innerMap.get(key);
			}

			@Override /* Map.Entry */
			public V setValue(V v) {
				return innerMap.put(key, v);
			}
		};
	}


	@Override /* Map */
	public Set<Map.Entry<K,V>> entrySet() {
		return entries;
	}

	/**
	 * A set with ordered entries (i.e. a List with a Set API).
	 */
	private static class ListSet<E> extends AbstractSet<E> {

		private List<E> entries;

		public ListSet(List<E> entries) {
			this.entries = entries;
		}

		@Override /* Set */
		public Iterator<E> iterator() {
			return entries.iterator();
		}

		@Override /* Set */
		public int size() {
			return entries.size();
		}
	}
}
