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

import java.util.concurrent.*;

/**
 * Map consisting of auto-generated atomic keys.
 *
 * <p>
 * Useful for creating in-memory 'databases' of POJOs.
 *
 * @param <K> The key type.
 * @param <V> The value type.
 */
public class IdMap<K,V> extends ConcurrentHashMap<K,V> {

	private static final long serialVersionUID = 1L;

	private final IdGenerator<K> idGen;


	/**
	 * Creates a new ID map with integer keys with generator initialized to <code>1</code>.
	 *
	 * @param c The value type.
	 * @return A new map.
	 */
	public static <T> IdMap<Integer,T> createIntMap(Class<T> c) {
		return createIntMap(c, 1);
	}

	/**
	 * Creates a new ID map with integer keys with generator initialized to the specified value.
	 *
	 * @param c The value type.
	 * @param initValue The initial value of the generator.
	 * @return A new map.
	 */
	public static <T> IdMap<Integer,T> createIntMap(Class<T> c, int initValue) {
		return create(c, IdGenerators.createIntGenerator(initValue));
	}

	/**
	 * Creates a new ID map with long keys with generator initialized to <code>1</code>.
	 *
	 * @param c The value type.
	 * @return A new map.
	 */
	public static <T> IdMap<Long,T> createLongMap(Class<T> c) {
		return createLongMap(c, 1l);
	}

	/**
	 * Creates a new ID map with long keys with generator initialized to the specified value.
	 *
	 * @param c The value type.
	 * @param initValue The initial value of the generator.
	 * @return A new map.
	 */
	public static <T> IdMap<Long,T> createLongMap(Class<T> c, long initValue) {
		return create(c, IdGenerators.createLongGenerator(initValue));
	}

	/**
	 * Creates a new map.
	 *
	 * @param c The value type.
	 * @param idGen An ID generator.
	 * @return A new instance.
	 */
	public static <K,T> IdMap<K,T> create(Class<T> c, IdGenerator<K> idGen) {
		return new IdMap<>(c, idGen);
	}

	private IdMap(Class<V> c, IdGenerator<K> idGen) {
		this.idGen = idGen;
	}

	/**
	 * Returns the next available ID.
	 *
	 * @return The next available ID.
	 */
	public K nextId() {
		return idGen.next();
	}

	/**
	 * Sets a lower bound on the specified ID.
	 * @param k The lower-bound key.
	 */
	public void lbId(K k) {
		idGen.lb(k);
	}
}
