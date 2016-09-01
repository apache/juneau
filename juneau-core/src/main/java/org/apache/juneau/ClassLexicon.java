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
 * Class names are defined through the {@link Bean#name()} or {@link Pojo#name()} annotations when using the {@link ClassLexicon#ClassLexicon(Class...)}
 * 	constructor, but can be defined programmatically by using the {@link ClassLexicon#ClassLexicon(Map)} constructor.
 * <p>
 * The lexicon is used by the framework in the following ways:
 * <ul>
 * 	<li>If a class type cannot be inferred through reflection during parsing, then a helper <js>"_class"</js> is added to the serialized output
 * 		using the name defined for that class in this lexicon.  This helps determine the real class at parse time.
 * 	<li>The lexicon name is used as element names when serialized to XML.
 * </ul>
 *
 * @author james.bognar
 */
public class ClassLexicon {

	private final Map<String,Class<?>> map;
	private final Map<Class<?>,String> reverseMap;

	/**
	 * Constructor.
	 *
	 * @param classes
	 * 	List of classes to add to this lexicon table.
	 * 	Each class must be one of the following:
	 * 	<ul>
	 * 		<li>A bean class that specifies a value for {@link Bean#name() @Bean.name()}.
	 * 		<li>A POJO class that specifies a value for {@link Pojo#name() @Pojo.name()}.
	 * 		<li>A subclass of {@link ClassLexicon} whose contents will be added to this list.
	 * 			Note that this subclass must implement a no-arg constructor.
	 * 	</ul>
	 */
	public ClassLexicon(Class<?>...classes) {
		this(null, classes);
	}

	/**
	 * Constructor with optional copy-from lexicon.
	 *
	 * @param copyFrom The lexicon to initialize the contents of this lexicon with.
	 * @param classes List of classes to add to this lexicon.
	 */
	public ClassLexicon(ClassLexicon copyFrom, Class<?>...classes) {
		Map<String,Class<?>> m = new HashMap<String,Class<?>>();
		if (copyFrom != null)
			m.putAll(copyFrom.map);
		for (Class<?> c : classes) {
			if (c != null) {
				if (ClassUtils.isParentClass(ClassLexicon.class, c)) {
					try {
						ClassLexicon l2 = (ClassLexicon)c.newInstance();
						m.putAll(l2.map);
					} catch (Exception e) {
						throw new BeanRuntimeException(e);
					}
				} else {
					Pojo p = c.getAnnotation(Pojo.class);
					Bean b = c.getAnnotation(Bean.class);
					String name = null;
					if (p != null && ! p.name().isEmpty()) {
						name = p.name();
					} else if (b != null && ! b.name().isEmpty()) {
						name = b.name();
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
	 * Alternate constructor for defining a lexicon through a user-defined name/class map.
	 *
	 * @param classMap
	 * 	Contains the name/class pairs to add to this lookup table.
	 */
	public ClassLexicon(Map<String,Class<?>> classMap) {
		if (classMap == null)
			throw new BeanRuntimeException("Null passed to ClassLexicon(Map) constructor.");
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
