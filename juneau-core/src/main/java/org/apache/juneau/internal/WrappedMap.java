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

import java.util.*;

/**
 * Wraps an existing map inside an extensible interface so that convenience methods can be added to the subclass.
 * @param <K> The key type.
 * @param <V> The value type.
 */
public class WrappedMap<K,V> implements Map<K,V> {

	private final Map<K,V> inner;

	/**
	 * Constructor.
	 *
	 * @param inner The inner map.
	 */
	protected WrappedMap(Map<K,V> inner) {
		this.inner = inner;
	}

	@Override /* Map */
	public void clear() {
		inner.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		return inner.containsKey(key);
	}

	@Override /* Map */
	public boolean containsValue(Object value) {
		return inner.containsValue(value);
	}

	@Override /* Map */
	public Set<java.util.Map.Entry<K,V>> entrySet() {
		return inner.entrySet();
	}

	@Override /* Map */
	public V get(Object key) {
		return inner.get(key);
	}

	@Override /* Map */
	public boolean isEmpty() {
		return inner.isEmpty();
	}

	@Override /* Map */
	public Set<K> keySet() {
		return inner.keySet();
	}

	@Override /* Map */
	public V put(K key, V value) {
		return inner.put(key, value);
	}

	@Override /* Map */
	public void putAll(Map<? extends K,? extends V> m) {
		inner.putAll(m);
	}

	@Override /* Map */
	public V remove(Object key) {
		return inner.remove(key);
	}

	@Override /* Map */
	public int size() {
		return inner.size();
	}

	@Override /* Map */
	public Collection<V> values() {
		return inner.values();
	}
}
