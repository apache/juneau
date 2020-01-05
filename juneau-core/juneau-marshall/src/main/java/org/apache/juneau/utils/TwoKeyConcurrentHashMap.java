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
import java.util.concurrent.*;

/**
 * A hashmap that allows for two-part keys.
 * @param <K1> Key part 1 type.
 * @param <K2> Key part 2 type.
 * @param <V> Value type.
 */
public class TwoKeyConcurrentHashMap<K1,K2,V> extends ConcurrentHashMap<TwoKeyConcurrentHashMap.Key<K1,K2>,V> {
	private static final long serialVersionUID = 1L;

	/**
	 * Adds an entry to this map.
	 *
	 * @param key1 Key part 1.  Can be <jk>null</jk>.
	 * @param key2 Key part 2.  Can be <jk>null</jk>.
	 * @param value Value.
	 * @return The previous value if there was one.
	 */
	public V put(K1 key1, K2 key2, V value) {
		Key<K1,K2> key = new Key<>(key1, key2);
		return super.put(key, value);
	}

	/**
	 * Retrieves an entry from this map.
	 *
	 * @param key1 Key part 1.  Can be <jk>null</jk>.
	 * @param key2 Key part 2.  Can be <jk>null</jk>.
	 * @return The previous value if there was one.
	 */
	public V get(K1 key1, K2 key2) {
		Key<K1,K2> key = new Key<>(key1, key2);
		return super.get(key);
	}

	static class Key<K1,K2> {
		final K1 k1;
		final K2 k2;
		final int hashCode;

		Key(K1 k1, K2 k2) {
			this.k1 = k1;
			this.k2 = k2;
			this.hashCode = 31*(k1 == null ? 0 : k1.hashCode()) + (k2 == null ? 0 : k2.hashCode());
		}

		@Override /* Object */
		public int hashCode() {
			return hashCode;
		}

		@Override /* Object */
		@SuppressWarnings("unchecked")
		public boolean equals(Object o) {
			Key<K1,K2> ko = (Key<K1,K2>)o;
			return Objects.equals(k1, ko.k1) && Objects.equals(k2, ko.k2);
		}
	}
}
