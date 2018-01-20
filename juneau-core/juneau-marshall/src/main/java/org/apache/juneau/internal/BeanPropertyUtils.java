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

import static org.apache.juneau.internal.StringUtils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;

/**
 * Utility methods when working with setting of bean properties.
 */
public final class BeanPropertyUtils {
	
	/**
	 * Converts a value to a String.
	 * 
	 * @param o The value to convert.
	 * @return The converted value, or <jk>null</jk> if the input was null.
	 */
	public static String toStringVal(Object o) {
		return StringUtils.toString(o);
	}

	/**
	 * Converts a value to a Boolean.
	 * 
	 * @param o The value to convert.
	 * @return The converted value, or <jk>null</jk> if the input was null.
	 */
	public static Boolean toBoolean(Object o) {
		return ObjectUtils.toBoolean(o);
	}

	/**
	 * Converts a value to a Number.
	 * 
	 * @param o The value to convert.
	 * @return The converted value, or <jk>null</jk> if the input was null.
	 */
	public static Number toNumber(Object o) {
		return ObjectUtils.toNumber(o);
	}

	/**
	 * Converts a value to an Integer.
	 * 
	 * @param o The value to convert.
	 * @return The converted value, or <jk>null</jk> if the input was null.
	 */
	public static Integer toInteger(Object o) {
		return ObjectUtils.toInteger(o);
	}

	/**
	 * Converts a value to a URI.
	 * 
	 * @param o The value to convert.
	 * @return The converted value, or <jk>null</jk> if the input was null.
	 */
	public static java.net.URI toURI(Object o) {
		return StringUtils.toURI(o);
	}
	
	/**
	 * Adds a set of values to an existing list.
	 * 
	 * @param appendTo 
	 * 	The list to append to.
	 * 	<br>If <jk>null</jk>, a new {@link ArrayList} will be created.
	 * @param values The values to add.
	 * @param type The data type of the elements. 
	 * @param args The generic type arguments of the data type.
	 * @return The converted value, or <jk>null</jk> if the input was null.
	 */
	public static <T> List<T> addToList(List<T> appendTo, Object[] values, Class<T> type, Type...args) {
		if (values == null)
			return appendTo;
		try {
			List<T> l = appendTo == null ? new ArrayList<T>() : appendTo;
			for (Object o : values) {
				if (o != null) {
					if (isObjectList(o)) {
						for (Object o2 : new ObjectList(o.toString())) 
							l.add(toType(o2, type, args));
					} else if (o instanceof Collection) {
						for (Object o2 : (Collection<?>)o) 
							l.add(toType(o2, type, args));
					} else if (o.getClass().isArray()) {
						for (int i = 0; i < Array.getLength(o); i++)
							l.add(toType(Array.get(o, i), type, args));
					} else {
						l.add(toType(o, type, args));
					}
				}
			}
			return l.isEmpty() ? null : l;
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Adds a set of values to an existing map.
	 * 
	 * @param appendTo 
	 * 	The map to append to.
	 * 	<br>If <jk>null</jk>, a new {@link LinkedHashMap} will be created.
	 * @param values The values to add.
	 * @param keyType The data type of the keys. 
	 * @param valueType The data type of the values. 
	 * @param valueTypeArgs The generic type arguments of the data type of the values.
	 * @return The converted value, or <jk>null</jk> if the input was null.
	 */
	@SuppressWarnings("unchecked")
	public static <K,V> Map<K,V> addToMap(Map<K,V> appendTo, Object[] values, Class<K> keyType, Class<V> valueType, Type...valueTypeArgs) {
		if (values == null)
			return appendTo;
		try {
			Map<K,V> m = appendTo == null ? new LinkedHashMap<K,V>() : appendTo;
			for (Object o : values) {
				if (o != null) {
					if (isObjectMap(o)) {
						for (Map.Entry<String,Object> e : new ObjectMap(o.toString()).entrySet()) 
							m.put(toType(e.getKey(), keyType), toType(e.getValue(), valueType, valueTypeArgs));
					} else if (o instanceof Map) {
						for (Map.Entry<Object,Object> e : ((Map<Object,Object>)o).entrySet()) 
							m.put(toType(e.getKey(), keyType), toType(e.getValue(), valueType, valueTypeArgs));
					} else {
						throw new FormattedRuntimeException("Invalid object type {0} passed to addToMap()", o.getClass().getName()); 
					}
				}
			}
			return m.isEmpty() ? null : m;
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Converts an object to the specified type.
	 * 
	 * @param o The object to convert.
	 * @param type The type to covert to.
	 * @param args The type arguments for types of map or collection.
	 * @return The converted object.
	 */
	public static <T> T toType(Object o, Class<T> type, Type...args) {
		return ObjectUtils.toType(o, type, args);
	}
	
	/**
	 * Creates a new list from the specified collection.
	 * 
	 * @param val The value to copy from.
	 * @return A new {@link ArrayList}, or <jk>null</jk> if the input was null.
	 */
	public static <T> List<T> newList(Collection<T> val) {
		if (val == null)
			return null;
		return new ArrayList<>(val);
	}

	/**
	 * Copies the specified values into an existing list.
	 * 
	 * @param l 
	 * 	The list to add to.
	 * 	<br>If <jk>null</jk>, a new {@link ArrayList} will be created.
	 * @param val The values to add.
	 * @return The list with values copied into it.
	 */
	public static <T> List<T> addToList(List<T> l, Collection<T> val) {
		if (val != null) {
			if (l == null)
				l = new ArrayList<>(val);
			else
				l.addAll(val);
		}
		return l;
	}
	
	/**
	 * Creates a new map from the specified map.
	 * 
	 * @param val The value to copy from.
	 * @return A new {@link LinkedHashMap}, or <jk>null</jk> if the input was null.
	 */
	public static <K,V> Map<K,V> newMap(Map<K,V> val) {
		if (val == null)
			return null;
		return new LinkedHashMap<>(val);
	}
	
	/**
	 * Copies the specified values into an existing map.
	 * 
	 * @param m 
	 * 	The map to add to.
	 * 	<br>If <jk>null</jk>, a new {@link LinkedHashMap} will be created.
	 * @param val The values to add.
	 * @return The list with values copied into it.
	 */
	public static <K,V> Map<K,V> addToMap(Map<K,V> m, Map<K,V> val) {
		if (val != null) {
			if (m == null)
				m = new LinkedHashMap<>(val);
			else
				m.putAll(val);
		}
		return m;
	}

	/**
	 * Adds a single entry into an existing map.
	 * 
	 * @param m 
	 * 	The map to add to.
	 * 	<br>If <jk>null</jk>, a new {@link LinkedHashMap} will be created.
	 * @param key The entry key.
	 * @param value The entry value.
	 * @return The list with values copied into it.
	 */
	public static <K,V> Map<K,V> addToMap(Map<K,V> m, K key, V value) {
		if (m == null)
			m = new LinkedHashMap<>();
		m.put(key, value);
		return m;
	}
}
