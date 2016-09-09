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

/**
 * A lookup table for resolving bean types by name.
 * <p>
 * In a nutshell, provides a simple mapping of bean class objects to identifying names.
 * <p>
 * Class names are defined through the {@link Bean#typeName()} annotation.
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
public class BeanDictionary {

	private final Map<String,ClassMeta<?>> map;
	private final Map<Class<?>,String> reverseMap;
	private final BeanContext beanContext;
	private final String typePropertyName;
	private final BeanDictionary parent;

	BeanDictionary(BeanContext beanContext, BeanDictionary parent, Map<String,Class<?>> map) {
		this.beanContext = beanContext;
		this.typePropertyName = beanContext.getTypePropertyName();
		this.parent = parent == null ? beanContext.getBeanDictionary() : parent;
		Map<String,ClassMeta<?>> m1 = new HashMap<String,ClassMeta<?>>();
		for (Map.Entry<String,Class<?>> e : map.entrySet()) {
			ClassMeta<?> cm = beanContext.getClassMeta(e.getValue());
			if (! cm.isBean())
				throw new BeanRuntimeException("Invalid class type passed to dictionary.  ''{0}'' is not a bean.", cm);
			m1.put(e.getKey(), cm);
		}
		this.map = Collections.unmodifiableMap(m1);
		Map<Class<?>,String> m2 = new HashMap<Class<?>,String>();
		for (Map.Entry<String,Class<?>> e : map.entrySet())
			m2.put(e.getValue(), e.getKey());
		this.reverseMap = Collections.unmodifiableMap(m2);
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

	/**
	 * Converts the specified object map into a bean if it contains a <js>"_type"</js> entry in it.
	 *
	 * @param m The object map to convert to a bean if possible.
	 * @return The new bean, or the original <code>ObjectMap</code> if no <js>"_type"</js> entry was found.
	 */
	public Object cast(ObjectMap m) {
		Object o = m.get(typePropertyName);
		if (o == null)
			return m;
		String typeName = o.toString();
		ClassMeta<?> cm = findClassMeta(typeName);
		BeanMap<?> bm = beanContext.newBeanMap(cm.getInnerClass());

		// Iterate through all the entries in the map and set the individual field values.
		for (Map.Entry<String,Object> e : m.entrySet()) {
			String k = e.getKey();
			Object v = e.getValue();
			if (! k.equals(typePropertyName)) {
				// Attempt to recursively cast child maps.
				if (v instanceof ObjectMap)
					v = cast((ObjectMap)v);
				bm.put(k, v);
			}
		}
		return bm.getBean();
	}

	private ClassMeta<?> findClassMeta(String typeName) {
		ClassMeta<?> cm = map.get(typeName);
		if (cm == null && parent != null)
			cm = parent.findClassMeta(typeName);
		if (cm == null)
			throw new BeanRuntimeException("Could not find bean type ''{0}'' in dictionary.", typeName);
		return cm;
	}
}
