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

import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;

/**
 * Represents a wrapped {@link BeanMap} where property values can be overridden, removed, or reordered without
 * affecting the underlying bean.
 *
 * <p>
 * Provides the {@link #filterKeys(List)} method for specifying the keys to keep in the bean map and in what order
 * they should appear.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @param <T> The class type of the wrapped bean.
 */
public class DelegateBeanMap<T> extends BeanMap<T> {

	private Set<String> keys = set();
	private JsonMap overrideValues = new JsonMap();

	/**
	 * Constructor.
	 *
	 * @param bean The bean being wrapped.
	 * @param session The bean session that created this bean map.
	 */
	@SuppressWarnings("unchecked")
	public
	DelegateBeanMap(T bean, BeanSession session) {
		super(session, bean, session.getBeanMeta((Class<T>)bean.getClass()));
	}

	/**
	 * Add a key in the next position.
	 *
	 * @param key The key to add.
	 */
	public void addKey(String key) {
		this.keys.add(key);
	}

	@Override /* Map */
	public Object put(String key, Object val) {
		this.overrideValues.put(key, val);
		this.keys.add(key);
		return null;
	}

	@Override /* Map */
	public Object get(Object key) {
		if (overrideValues.containsKey(key))
			return overrideValues.get(key);
		return super.get(key);
	}

	@Override /* Map */
	public Set<String> keySet() {
		return keys;
	}

	/**
	 * Remove all but the specified properties from this bean map.
	 *
	 * <p>
	 * This does not affect the underlying bean.
	 *
	 * @param keys The remaining keys in the bean map (in the specified order).
	 * @return This object.
	 */
	public DelegateBeanMap<T> filterKeys(List<String> keys) {
		this.keys.clear();
		this.keys.addAll(keys);
		return this;
	}

	@Override /* Map */
	public Object remove(Object key) {
		keys.remove(key);
		return null;
	}

	@Override /* BeanMap */
	public BeanMeta<T> getMeta() {
		return new BeanMetaFiltered<>(super.getMeta(), keys);
	}

	@Override /* Map */
	public synchronized Set<Entry<String,Object>> entrySet() {
		Set<Entry<String,Object>> s = set();
		keys.forEach(k -> {
			BeanMapEntry bme;
			if (overrideValues.containsKey(k))
				bme = new BeanMapEntryOverride(this, this.getPropertyMeta(k), overrideValues.get(k));
			else
				bme = this.getProperty(k);
			if (bme == null)
				throw new BeanRuntimeException(super.getClassMeta().getInnerClass(), "Property ''{0}'' not found on class.", k);
			s.add(bme);
		});
		return s;
	}

	@Override /* BeanMap */
	public Collection<BeanPropertyMeta> getProperties() {
		List<BeanPropertyMeta> l = new ArrayList<>(keys.size());
		keys.forEach(k -> {
			BeanPropertyMeta p = this.getPropertyMeta(k);
			if (overrideValues.containsKey(k))
				p = BeanPropertyMeta.builder(this.meta, k).overrideValue(overrideValues.get(k)).delegateFor(p).build();
			if (p == null)
				p = BeanPropertyMeta.builder(this.meta, k).overrideValue(null).delegateFor(p).build();
			l.add(p);
		});
		return l;
	}

	@Override
	public BeanMap<T> forEachProperty(Predicate<BeanPropertyMeta> filter, Consumer<BeanPropertyMeta> action) {
		getProperties().forEach(x -> {
			if (filter == null || filter.test(x))
				action.accept(x);
		});
		return this;
	}

	final class BeanMapEntryOverride extends BeanMapEntry {
		Object value;

		BeanMapEntryOverride(BeanMap<?> bm, BeanPropertyMeta bpm, Object value) {
			super(bm, bpm, bpm.getName());
			this.value = value;
		}

		@Override /* Map.Entry */
		public Object getValue() {
			return value;
		}
	}
}
