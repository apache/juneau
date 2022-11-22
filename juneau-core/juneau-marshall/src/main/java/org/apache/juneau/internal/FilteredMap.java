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

import java.util.*;

import org.apache.juneau.*;

/**
 * Wrapper around a map where the key names are overridden.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @param <K> The key class type.
 * @param <V> The value class type.
 */
public final class FilteredMap<K,V> extends AbstractMap<K,V> implements Delegate<Map<K,V>> {

	final Map<K,V> innerMap;
	final Set<Map.Entry<K,V>> entries;
	final ClassMeta<Map<K,V>> classMeta;

	/**
	 * Constructor.
	 *
	 * @param classMeta The class type of the map being wrapped.
	 * @param innerMap The map being wrapped.  Must not be <jk>null</jk>.
	 * @param keys The keys in the new map.  Must not be <jk>null</jk>.
	 */
	public FilteredMap(ClassMeta<Map<K,V>> classMeta, Map<K,V> innerMap, K[] keys) {
		assertArgNotNull("innerMap", innerMap);
		assertArgNotNull("keys", keys);

		this.classMeta = classMeta;
		this.innerMap = innerMap;
			List<Map.Entry<K,V>> l = new ArrayList<>(keys.length);
			for (K k : keys)
				if (innerMap.containsKey(k))
					l.add(createEntry(k));
			entries = new ListSet<>(l);
		}

	private Map.Entry<K,V> createEntry(final K key) {
		return new Map.Entry<>() {

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
	 * A set with ordered entries (a List with a Set API).
	 */
	private static final class ListSet<E> extends AbstractSet<E> {

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

	@Override /* Delegate */
	public ClassMeta<Map<K,V>> getClassMeta() {
		return classMeta;
	}
}
