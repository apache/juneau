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
 * Utility methods for collections.
 */
public class CollectionUtils {

	/**
	 * Reverses the order of a {@link LinkedHashMap}.
	 *
	 * @param in The map to reverse the order on.
	 * @return A new {@link LinkedHashMap} with keys in reverse order.
	 */
	public static <K,V> LinkedHashMap<K,V> reverse(Map<K,V> in) {
		if (in == null)
			return null;
		LinkedHashMap<K,V> m = new LinkedHashMap<K,V>();

		// Note:  Entry objects are reusable in an entry set, so we simply can't
		// create a reversed iteration of that set.
		List<K> keys = new ArrayList<K>(in.keySet());
		List<V> values = new ArrayList<V>(in.values());
		for (int i = in.size()-1; i >= 0; i--)
			m.put(keys.get(i), values.get(i));

		return m;
	}

	/**
	 * Add a value to a list if the value is not null.
	 *
	 * @param l The list to add to.
	 * @param o The element to add.
	 * @return The same list.
	 */
	public static <T> List<T> addIfNotNull(List<T> l, T o) {
		if (o != null)
			l.add(o);
		return l;
	}
}
