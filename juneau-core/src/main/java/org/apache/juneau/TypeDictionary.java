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

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

/**
 * A lookup table for resolving classes by name.
 * <p>
 * In a nutshell, provides a simple mapping of class objects to identifying names by implementing the following two methods:
 * <ul>
 * 	<li>{@link #getClassForName(String)}
 * 	<li>{@link #getNameForClass(Class)}
 * </ul>
 * <p>
 * Class names are defined through the {@link Bean#typeName()} annotations when using the {@link TypeDictionary#TypeDictionary(Class...)}
 * 	constructor, but can be defined programmatically by using the {@link TypeDictionary#TypeDictionary(Map)} constructor.
 * <p>
 * The dictionary is used by the framework in the following ways:
 * <ul>
 * 	<li>If a class type cannot be inferred through reflection during parsing, then a helper <js>"_type"</js> is added to the serialized output
 * 		using the name defined for that class in this dictionary.  This helps determine the real class at parse time.
 * 	<li>The dictionary name is used as element names when serialized to XML.
 * </ul>
 *
 * @author james.bognar
 */
public class TypeDictionary {

	private final Map<String,Class<?>> map;
	private final Map<Class<?>,String> reverseMap;

	/**
	 * Constructor.
	 *
	 * @param classes
	 * 	List of classes to add to this dictionary table.
	 * 	Each class must be one of the following:
	 * 	<ul>
	 * 		<li>A bean class that specifies a value for {@link Bean#typeName() @Bean.name()}.
	 * 		<li>A subclass of {@link TypeDictionary} whose contents will be added to this list.
	 * 			Note that this subclass must implement a no-arg constructor.
	 * 	</ul>
	 */
	public TypeDictionary(Class<?>...classes) {
		Map<String,Class<?>> m = new HashMap<String,Class<?>>();
		for (Class<?> c : classes) {
			if (c != null) {
				if (ClassUtils.isParentClass(TypeDictionary.class, c)) {
					try {
						TypeDictionary l2 = (TypeDictionary)c.newInstance();
						m.putAll(l2.map);
					} catch (Exception e) {
						throw new BeanRuntimeException(e);
					}
				} else {
					Bean b = c.getAnnotation(Bean.class);
					String name = null;
					if (b != null && ! b.typeName().isEmpty()) {
						name = b.typeName();
					} else {
						name = c.getName();
					}
					m.put(name, c);
				}
			}
		}
		this.map = Collections.unmodifiableMap(m);
		Map<Class<?>,String> m2 = new HashMap<Class<?>,String>();
		for (Map.Entry<String,Class<?>> e : map.entrySet())
			m2.put(e.getValue(), e.getKey());
		this.reverseMap = Collections.unmodifiableMap(m2);
	}


	/**
	 * Alternate constructor for defining a dictionary through a user-defined name/class map.
	 *
	 * @param classMap
	 * 	Contains the name/class pairs to add to this lookup table.
	 */
	public TypeDictionary(Map<String,Class<?>> classMap) {
		if (classMap == null)
			throw new BeanRuntimeException("Null passed to TypeDictionary(Map) constructor.");
		Map<String,Class<?>> m = new HashMap<String,Class<?>>(classMap);
		this.map = Collections.unmodifiableMap(m);
		Map<Class<?>,String> m2 = new HashMap<Class<?>,String>();
		for (Map.Entry<String,Class<?>> e : map.entrySet())
			m2.put(e.getValue(), e.getKey());
		this.reverseMap = Collections.unmodifiableMap(m2);
	}

	/**
	 * Returns the class associated with the specified name.
	 *
	 * @param name The name associated with the class.
	 * @return The class associated with the specified name, or <jk>null</jk> if no association exists.
	 */
	public Class<?> getClassForName(String name) {
		return map.get(name);
	}

	/**
	 * Returns the name associated with the specified class.
	 *
	 * @param c The class associated with the name.
	 * @return The name associated with the specified class, or <jk>null</jk> if no association exists.
	 */
	public String getNameForClass(Class<?> c) {
		return reverseMap.get(c);
	}
}
