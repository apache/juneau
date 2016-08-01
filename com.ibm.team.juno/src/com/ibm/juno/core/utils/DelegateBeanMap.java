/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.utils;

import java.util.*;

import com.ibm.juno.core.*;

/**
 * Represents a wrapped {@link BeanMap} where property values can be overridden, removed, or reordered
 * 	without affecting the underlying bean.
 * <p>
 * 	Provides the {@link #filterKeys(List)} method for specifying the keys to keep in the bean map
 * 		and in what order they should appear.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 * @param <T> The class type of the wrapped bean.
 */
@SuppressWarnings("hiding")
public class DelegateBeanMap<T> extends BeanMap<T> {

	private Set<String> keys = Collections.newSetFromMap(new LinkedHashMap<String,Boolean>());
	private ObjectMap overrideValues = new ObjectMap();

	@SuppressWarnings("unchecked")
	DelegateBeanMap(T bean, BeanContext bc) {
		super(bean, bc.getBeanMeta((Class<T>)bean.getClass()));
	}

	void addKey(String key) {
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
