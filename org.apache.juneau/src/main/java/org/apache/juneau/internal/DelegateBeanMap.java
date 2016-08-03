/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.internal;

import java.util.*;

import org.apache.juneau.*;

/**
 * Represents a wrapped {@link BeanMap} where property values can be overridden, removed, or reordered
 * 	without affecting the underlying bean.
 * <p>
 * 	Provides the {@link #filterKeys(List)} method for specifying the keys to keep in the bean map
 * 		and in what order they should appear.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 * @param <T> The class type of the wrapped bean.
 */
@SuppressWarnings("hiding")
public class DelegateBeanMap<T> extends BeanMap<T> {

	private Set<String> keys = Collections.newSetFromMap(new LinkedHashMap<String,Boolean>());
	private ObjectMap overrideValues = new ObjectMap();

	/**
	 * Constructor.
	 *
	 * @param bean The bean being wrapped.
	 * @param bc The bean context that created this bean map.
	 */
	@SuppressWarnings("unchecked")
	public
	DelegateBeanMap(T bean, BeanContext bc) {
		super(bean, bc.getBeanMeta((Class<T>)bean.getClass()));
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
	 * <p>
	 * This does not affect the underlying bean.
	 *
	 * @param keys The remaining keys in the bean map (in the specified order).
	 */
	public void filterKeys(List<String> keys) {
		this.keys.clear();
		this.keys.addAll(keys);
	}

	@Override /* Map */
	public Object remove(Object key) {
		keys.remove(key);
		return null;
	}

	@Override /* BeanMap */
	public BeanMeta<T> getMeta() {
		return new BeanMetaFiltered<T>(super.getMeta(), keys);
	}

	@Override /* Map */
	public Set<Entry<String,Object>> entrySet() {
		Set<Entry<String,Object>> s = Collections.newSetFromMap(new LinkedHashMap<Map.Entry<String,Object>,Boolean>());
		for (final String key : keys) {
			BeanMapEntry<T> bme;
			if (overrideValues.containsKey(key))
				bme = new BeanMapEntryOverride<T>(this, this.getPropertyMeta(key), overrideValues.get(key));
			else
				bme = this.getProperty(key);
			if (bme == null)
				throw new BeanRuntimeException(super.getClassMeta().getInnerClass(), "Property ''{0}'' not found on class.", key);
			s.add(bme);
		}
		return s;
	}

	@Override /* BeanMap */
	public Collection<BeanPropertyMeta<T>> getProperties() {
		List<BeanPropertyMeta<T>> l = new ArrayList<BeanPropertyMeta<T>>(keys.size());
		for (final String key : keys) {
			BeanPropertyMeta<T> p = this.getPropertyMeta(key);
			if (overrideValues.containsKey(key)) {
				p = new BeanPropertyMeta<T>(this.meta, key) {
					@Override /* BeanPropertyMeta */
					public Object get(BeanMap<T> m) {
						return overrideValues.get(key);
					}
				};
			}
			if (p == null)
				throw new BeanRuntimeException(super.getClassMeta().getInnerClass(), "Property ''{0}'' not found on class.", key);
			l.add(p);
		}
		return l;
	}

	private class BeanMapEntryOverride<T2> extends BeanMapEntry<T2> {
		Object value;

		private BeanMapEntryOverride(BeanMap<T2> bm, BeanPropertyMeta<T2> bpm, Object value) {
			super(bm, bpm);
			this.value = value;
		}

		@Override /* Map.Entry */
		public Object getValue() {
			return value;
		}
	}
}
