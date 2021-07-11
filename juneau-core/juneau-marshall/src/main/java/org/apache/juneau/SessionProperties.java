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
package org.apache.juneau;

import static org.apache.juneau.internal.ClassUtils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;

/**
 * Contains the configurable properties for a single context session.
 */
public class SessionProperties {

	private final OMap map;

	/**
	 * Static creator.
	 *
	 * @return A new instance of this class.
	 */
	public static SessionProperties create() {
		return new SessionProperties(null);
	}

	/**
	 * Static creator.
	 *
	 * @param inner The initial contents of these properties.
	 * @return A new instance of this class.
	 */
	public static SessionProperties create(Map<String,Object> inner) {
		return new SessionProperties(inner);
	}

	/**
	 * Constructor.
	 */
	private SessionProperties(Map<String,Object> inner) {
		this(inner, false);
	}

	private SessionProperties(Map<String,Object> inner, boolean unmodifiable) {
		OMap m = inner == null ? new OMap() : inner instanceof OMap ? (OMap)inner : new OMap(inner);
		if (unmodifiable)
			 m = m.unmodifiable();
		this.map = m;
	}

	/**
	 * Returns an unmodifiable copy of these properties.
	 *
	 * @return An unmodifiable copy of these properties.
	 */
	public final SessionProperties unmodifiable() {
		return new SessionProperties(map, true);
	}

	/**
	 * Returns <jk>true</jk> if this session has the specified property defined.
	 *
	 * @param key The property key.
	 * @return <jk>true</jk> if this session has the specified property defined.
	 */
	public final boolean contains(String key) {
		return map.containsKey(key);
	}

	/**
	 * Returns the property with the specified name.
	 *
	 * @param key The property name.
	 * @return The property value.  Never <jk>null</jk>.
	 */
	public final Optional<Object> get(String key) {
		return Optional.ofNullable(map.get(key));
	}

	/**
	 * Returns the session property with the specified key and type.
	 *
	 * @param key The property key.
	 * @param type The type to convert the property to.
	 * @return The session property.
	 */
	public final <T> Optional<T> get(String key, Class<T> type) {
		return Optional.ofNullable(find(key, type));
	}

	/**
	 * Shortcut for calling <code>get(key, String.<jk>class</jk>)</code>.
	 *
	 * @param key The property name.
	 * @return The property value, never <jk>null</jk>.
	 */
	public final Optional<String> getString(String key) {
		return Optional.ofNullable(find(key, String.class));
	}

	/**
	 * Shortcut for calling <code>get(key, Boolean.<jk>class</jk>)</code>.
	 *
	 * @param key The property name.
	 * @return The property value, never <jk>null</jk>.
	 */
	public final Optional<Boolean> getBoolean(String key) {
		return Optional.ofNullable(find(key, Boolean.class));
	}

	/**
	 * Shortcut for calling <code>get(key, Integer.<jk>class</jk>)</code>.
	 *
	 * @param key The property name.
	 * @return The property value, never <jk>null</jk>.
	 */
	public final Optional<Integer> getInteger(String key) {
		return Optional.ofNullable(find(key, Integer.class));
	}

	/**
	 * Returns the session property as the specified instance type with the specified key.
	 *
	 * @param key The property key.
	 * @param type The type to convert the property to.
	 * @return The session property.
	 */
	public <T> Optional<T> getInstance(String key, Class<T> type) {
		return Optional.ofNullable(newInstance(type, map.get(key)));
	}

	/**
	 * Returns the specified property as an array of instantiated objects.
	 *
	 * @param key The property name.
	 * @param type The class type of the property.
	 * @return A new property instance.
	 */
	@SuppressWarnings("unchecked")
	public <T> Optional<T[]> getInstanceArray(String key, Class<T> type) {
		Object o = map.get(key);
		if (o == null)
			return Optional.empty();
		T[] t = null;
		if (o.getClass().isArray()) {
			if (o.getClass().getComponentType() == type)
				t = (T[])o;
			else {
				t = (T[])Array.newInstance(type, Array.getLength(o));
				for (int i = 0; i < Array.getLength(o); i++)
					t[i] = newInstance(type, Array.get(o, i));
			}
		} else if (o instanceof Collection) {
			Collection<?> c = (Collection<?>)o;
			t = (T[])Array.newInstance(type, c.size());
			int i = 0;
			for (Object o2 : c)
				t[i++] = newInstance(type, o2);
		}
		return Optional.ofNullable(t);
	}

	/**
	 * Returns all the keys in these properties.
	 *
	 * @return All the keys in these properties.
	 */
	public Set<String> keySet() {
		return map.keySet();
	}

	/**
	 * Returns the contents of this session as an unmodifiable map.
	 *
	 * @return The contents of this session as an unmodifiable map.
	 */
	public Map<String,Object> asMap() {
		return Collections.unmodifiableMap(map);
	}

	private <T> T find(String key, Class<T> c) {
		return newInstance(c, map.get(key));
	}

	@SuppressWarnings("unchecked")
	private <T> T newInstance(Class<T> type, Object o) {
		T t = null;
		if (o == null)
			return null;
		else if (type.isInstance(o))
			t = (T)o;
		else if (o.getClass() == Class.class)
			t = castOrCreate(type, o);
		else if (o.getClass() == String.class)
			t = ClassUtils.fromString(type, o.toString());
		if (t != null)
			return t;
		throw new ConfigException("Could not instantiate type ''{0}'' as type {1}", o, type);
	}

	/**
	 * Removes a property from this store.
	 *
	 * @param key The property key.
	 * @return This object (for method chaining).
	 */
	public SessionProperties remove(String key) {
		map.remove(key);
		return this;
	}

	/**
	 * Adds a property to this store.
	 *
	 * @param key The property key.
	 * @param value The property value.
	 * @return This object (for method chaining).
	 */
	public SessionProperties put(String key, Object value) {
		map.put(key, value);
		return this;
	}

	/**
	 * Adds multiple properties to this store.
	 *
	 * @param values The map containing the properties to add.
	 * @return This object (for method chaining).
	 */
	public SessionProperties putAll(Map<String,Object> values) {
		if (values != null)
			map.putAll(values);
		return this;
	}
}
