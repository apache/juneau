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
 * Utility class for creating {@link BeanDictionary} objects.
 * <p>
 * This class is a programmatic equivalent to the {@link BeanProperty#beanDictionary()} annotation and {@link BeanContext#BEAN_beanDictionary} property.
 * It allows bean dictionaries to be constructed
 */
public class BeanDictionaryBuilder {

	Map<String,Class<?>> map = new HashMap<String,Class<?>>();
	BeanContext beanContext;

	/**
	 * Add the specified classes to this type dictionary.
	 * <p>
	 * Classes can be of the following types:
	 * <ul>
	 * 	<li>Bean classes.
	 * 	<li>Subclasses of {@link BeanDictionaryBuilder} that identify an entire set of mappings.
	 * </ul>
	 *
	 * @param classes The classes to add to this dictionary builder.
	 * @return This object (for method chaining).
	 */
	public BeanDictionaryBuilder add(Class<?>...classes) {
		for (Class<?> c : classes) {
			if (c != null) {
				if (ClassUtils.isParentClass(BeanDictionaryBuilder.class, c)) {
					try {
						BeanDictionaryBuilder l2 = (BeanDictionaryBuilder)c.newInstance();
						for (Map.Entry<String,Class<?>> e : l2.map.entrySet())
							map.put(e.getKey(), e.getValue());
					} catch (Exception e) {
						throw new BeanRuntimeException(e);
					}
				} else {
					Bean b = c.getAnnotation(Bean.class);
					if (b == null || b.typeName().isEmpty())
						throw new BeanRuntimeException("Class ''{0}'' was passed to TypeDictionaryBuilder but it doesn't have a @Bean.typeName() annotation defined.", c.getName());
					map.put(b.typeName(), c);
				}
			}
		}
		return this;
	}

	BeanDictionaryBuilder setBeanContext(BeanContext beanContext) {
		this.beanContext = beanContext;
		return this;
	}

	BeanDictionary build() {
		return new BeanDictionary(this);
	}
}
