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
	private final BeanContext beanContext;
	private final String beanTypePropertyName;

	BeanDictionary(BeanDictionaryBuilder builder) {
		this.beanContext = builder.beanContext;
		this.beanTypePropertyName = beanContext.getBeanTypePropertyName();
		Map<String,ClassMeta<?>> m1 = new HashMap<String,ClassMeta<?>>();
		for (Map.Entry<String,Class<?>> e : builder.map.entrySet()) {
			ClassMeta<?> cm = beanContext.getClassMeta(e.getValue());
			if (! cm.isBean())
				throw new BeanRuntimeException("Invalid class type passed to dictionary.  ''{0}'' is not a bean.", cm);
			m1.put(e.getKey(), cm);
		}
		this.map = Collections.unmodifiableMap(m1);
	}

	/**
	 * Converts the specified object map into a bean if it contains a <js>"_type"</js> entry in it.
	 *
	 * @param m The object map to convert to a bean if possible.
	 * @return The new bean, or the original <code>ObjectMap</code> if no <js>"_type"</js> entry was found.
	 */
	public Object cast(ObjectMap m) {
		Object o = m.get(beanTypePropertyName);
		if (o == null)
			return m;
		String typeName = o.toString();
		ClassMeta<?> cm = getClassMeta(typeName);
		BeanMap<?> bm = beanContext.newBeanMap(cm.getInnerClass());

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
	 * @return The class metadata for the bean.
	 * @throws BeanRuntimeException If name wasn't found in this dictionary.
	 */
	public ClassMeta<?> getClassMeta(String typeName) {
		ClassMeta<?> cm = map.get(typeName);
		if (cm == null)
			throw new BeanRuntimeException("Could not find bean type ''{0}'' in dictionary.", typeName);
		return cm;
	}
}
