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

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

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
 */
public class BeanRegistry {

	private final Map<String,ClassMeta<?>> map;
	private final BeanContext beanContext;
	private final String beanTypePropertyName;
	private final boolean isEmpty;

	BeanRegistry(BeanContext beanContext, BeanRegistry parent, Class<?>...classes) {
		this.beanContext = beanContext;
		this.beanTypePropertyName = beanContext.getBeanTypePropertyName();
		this.map = new ConcurrentHashMap<String,ClassMeta<?>>();
		for (Class<?> c : beanContext.beanDictionaryClasses)
			addClass(c);
		if (parent != null)
			this.map.putAll(parent.map);
		for (Class<?> c : classes)
			addClass(c);
		isEmpty = map.isEmpty();
	}

	private void addClass(Class<?> c) {
		if (c != null) {
			if (ClassUtils.isParentClass(Collection.class, c)) {
				try {
					@SuppressWarnings("rawtypes")
					Collection cc = (Collection)c.newInstance();
					for (Object o : cc) {
						if (o instanceof Class)
							addClass((Class<?>)o);
						else
							throw new BeanRuntimeException("Collection class passed to BeanRegistry does not contain Class objects.", c.getName());
					}
				} catch (Exception e) {
					throw new BeanRuntimeException(e);
				}
			} else {
				Bean b = c.getAnnotation(Bean.class);
				if (b == null || b.typeName().isEmpty())
					throw new BeanRuntimeException("Class ''{0}'' was passed to BeanRegistry but it doesn't have a @Bean.typeName() annotation defined.", c.getName());
				map.put(b.typeName(), beanContext.getClassMeta(c));
			}
		}
	}

	/**
	 * Converts the specified object map into a bean if it contains a <js>"_type"</js> entry in it.
	 *
	 * @param m The object map to convert to a bean if possible.
	 * @return The new bean, or the original <code>ObjectMap</code> if no <js>"_type"</js> entry was found.
	 */
	public Object cast(ObjectMap m) {
		if (isEmpty)
			return m;
		Object o = m.get(beanTypePropertyName);
		if (o == null)
			return m;
		String typeName = o.toString();
		ClassMeta<?> cm = getClassMeta(typeName);
		BeanMap<?> bm = m.getBeanSession().newBeanMap(cm.getInnerClass());

		// Iterate through all the entries in the map and set the individual field values.
		for (Map.Entry<String,Object> e : m.entrySet()) {
			String k = e.getKey();
			Object v = e.getValue();
			if (! k.equals(beanTypePropertyName)) {
				// Attempt to recursively cast child maps.
				if (v instanceof ObjectMap)
					v = cast((ObjectMap)v);
				bm.put(k, v);
			}
		}
		return bm.getBean();
	}

	/**
	 * Gets the class metadata for the specified bean type name.
	 *
	 * @param typeName The bean type name as defined by {@link Bean#typeName()}.
	 * 	Can include multi-dimensional array type names (e.g. <js>"X^^"</js>).
	 * @return The class metadata for the bean.
	 */
	public ClassMeta<?> getClassMeta(String typeName) {
		if (isEmpty)
			return null;
		if (typeName == null)
			return null;
		ClassMeta<?> cm = map.get(typeName);
		if (cm != null)
			return cm;
		if (typeName.charAt(typeName.length()-1) == '^') {
			cm = getClassMeta(typeName.substring(0, typeName.length()-1));
			if (cm != null)
				cm = beanContext.getClassMeta(Array.newInstance(cm.innerClass, 1).getClass());
			map.put(typeName, cm);
			return cm;
		}
		return null;
	}

	/**
	 * Returns <jk>true</jk> if this dictionary has an entry for the specified type name.
	 *
	 * @param typeName The bean type name.
	 * @return <jk>true</jk> if this dictionary has an entry for the specified type name.
	 */
	public boolean hasName(String typeName) {
		return getClassMeta(typeName) != null;
	}
}
