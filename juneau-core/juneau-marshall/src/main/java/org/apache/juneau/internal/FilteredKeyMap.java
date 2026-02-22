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
package org.apache.juneau.internal;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;

import org.apache.juneau.*;

/**
 * Wrapper around a map where the key names are overridden.
 *
 *
 * @param <K> The key class type.
 * @param <V> The value class type.
 */
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention
})
public class FilteredKeyMap<K,V> extends AbstractMap<K,V> implements Delegate<Map<K,V>> {

	// Argument name constants for assertArgNotNull
	private static final String ARG_innerMap = "innerMap";
	private static final String ARG_keys = "keys";

	/**
	 * A set with ordered entries (a List with a Set API).
	 */
	private static class ListSet<E> extends AbstractSet<E> {

		private List<E> entries;

		public ListSet(List<E> entries) {
			this.entries = entries;
		}

		@Override /* Overridden from Set */
		public Iterator<E> iterator() {
			return entries.iterator();
		}

		@Override /* Overridden from Set */
		public int size() {
			return entries.size();
		}
	}

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
	// TODO - Convert keys to List<K>
	public FilteredKeyMap(ClassMeta<Map<K,V>> classMeta, Map<K,V> innerMap, K[] keys) {
		assertArgNotNull(ARG_innerMap, innerMap);
		assertArgNotNull(ARG_keys, keys);

		this.classMeta = classMeta;
		this.innerMap = innerMap;
		List<Map.Entry<K,V>> l = new ArrayList<>(keys.length);
		for (var k : keys)
			if (innerMap.containsKey(k))
				l.add(createEntry(k));
		entries = new ListSet<>(l);
	}

	@Override /* Overridden from Map */
	public Set<Map.Entry<K,V>> entrySet() {
		return entries;
	}

	@Override /* Overridden from Delegate */
	public ClassMeta<Map<K,V>> getClassMeta() { return classMeta; }

	@Override /* Overridden from Object */
	public boolean equals(Object o) {
		return o instanceof Map<?,?> o2 && eq(this, o2, (x, y) -> x.entrySet().equals(y.entrySet()));
	}

	@Override /* Overridden from Object */
	public int hashCode() {
		return entries.hashCode();
	}

	private Map.Entry<K,V> createEntry(K key) {
		return new Map.Entry<>() {

			@Override /* Overridden from Map.Entry */
			public K getKey() { return key; }

			@Override /* Overridden from Map.Entry */
			public V getValue() { return innerMap.get(key); }

			@Override /* Overridden from Map.Entry */
			public V setValue(V v) {
				return innerMap.put(key, v);
			}
		};
	}
}