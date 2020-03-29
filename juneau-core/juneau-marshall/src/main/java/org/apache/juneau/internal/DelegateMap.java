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

import org.apache.juneau.*;
import org.apache.juneau.collections.*;

/**
 * Represents a wrapped {@link Map} where entries in the map can be removed without affecting the underlying map.
 *
 * @param <T> The class type of the wrapped bean.
 */
@SuppressWarnings("rawtypes")
public class DelegateMap<T extends Map> extends OMap implements Delegate<T> {
	private static final long serialVersionUID = 1L;

	private transient ClassMeta<T> classMeta;

	/**
	 * Constructor.
	 *
	 * @param m The metadata object that created this delegate object.
	 * @param session The current bean session.
	 */
	public DelegateMap(T m, BeanSession session) {
		this.classMeta = session.getClassMetaForObject(m);
		for (Map.Entry e : (Set<Map.Entry>)m.entrySet())
			put(StringUtils.stringify(e.getKey()), e.getValue());
	}

	@Override /* Delegate */
	public ClassMeta<T> getClassMeta() {
		return classMeta;
	}

	/**
	 * Remove all but the specified keys from this map.
	 *
	 * <p>
	 * This does not affect the underlying map.
	 *
	 * @param keys The remaining keys in the map (in the specified order).
	 * @return This object (for method chaining).
	 */
	public DelegateMap<T> filterKeys(List<String> keys) {
		OMap m2 = new OMap();
		for (String k : keys)
			if (containsKey(k))
				m2.put(k, get(k));
		this.clear();
		this.putAll(m2);
		return this;
	}
}
