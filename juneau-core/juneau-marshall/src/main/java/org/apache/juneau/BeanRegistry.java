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

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.ClassUtils.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.reflect.*;

/**
 * A lookup table for resolving bean types by name.
 *
 * <p>
 * In a nutshell, provides a simple mapping of bean class objects to identifying names.
 *
 * <p>
 * Class names are defined through the {@link Bean#typeName() @Bean(typeName)} annotation.
 *
 * <p>
 * The dictionary is used by the framework in the following ways:
 * <ul>
 * 	<li>If a class type cannot be inferred through reflection during parsing, then a helper <js>"_type"</js> is added
 * 		to the serialized output using the name defined for that class in this dictionary.  This helps determine the
 * 		real class at parse time.
 * 	<li>The dictionary name is used as element names when serialized to XML.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class BeanRegistry {

	private final Map<String,ClassMeta<?>> map;
	private final Map<Class<?>,String> reverseMap;
	private final BeanContext beanContext;
	private final boolean isEmpty;

	BeanRegistry(BeanContext beanContext, BeanRegistry parent, Class<?>...classes) {
		this.beanContext = beanContext;
		this.map = new ConcurrentHashMap<>();
		this.reverseMap = new ConcurrentHashMap<>();
		beanContext.getBeanDictionary().forEach(c -> addClass(c));
		if (parent != null)
			parent.map.forEach((k,v) -> addToMap(k, v));
		for (Class<?> c : classes)
			addClass(c);
		isEmpty = map.isEmpty();
	}

	private void addClass(Class<?> c) {
		try {
			if (c != null) {
				ClassInfo ci = ClassInfo.of(c);
				if (ci.isChildOf(Collection.class)) {
					Collection<?> cc = BeanCreator.of(Collection.class).type(c).run();
					cc.forEach(x -> {
						if (x instanceof Class)
							addClass((Class<?>)x);
						else
							throw new BeanRuntimeException("Collection class ''{0}'' passed to BeanRegistry does not contain Class objects.", className(c));
					});
				} else if (ci.isChildOf(Map.class)) {
					Map<?,?> m = BeanCreator.of(Map.class).type(c).run();
					m.forEach((k,v) -> {
						String typeName = stringify(k);
						ClassMeta<?> val = null;
						if (v instanceof Type)
							val = beanContext.getClassMeta((Type)v);
						else if (v.getClass().isArray())
							val = getTypedClassMeta(v);
						else
							throw new BeanRuntimeException("Class ''{0}'' was passed to BeanRegistry but value of type ''{1}'' found in map is not a Type object.", className(c), className(v));
						addToMap(typeName, val);
					});
				} else {
					Value<String> typeName = Value.empty();
					ci.forEachAnnotation(beanContext, Bean.class, x -> isNotEmpty(x.typeName()), x -> typeName.set(x.typeName()));
					addToMap(
						typeName.orElseThrow(() -> new BeanRuntimeException("Class ''{0}'' was passed to BeanRegistry but it doesn't have a @Bean(typeName) annotation defined.", className(c))),
						beanContext.getClassMeta(c)
					);
				}
			}
		} catch (BeanRuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new BeanRuntimeException(e);
		}
	}

	private ClassMeta<?> getTypedClassMeta(Object array) {
		int len = Array.getLength(array);
		if (len == 0)
			throw new BeanRuntimeException("Map entry had an empty array value.");
		Type type = (Type)Array.get(array, 0);
		Type[] args = new Type[len-1];
		for (int i = 1; i < len; i++)
			args[i-1] = (Type)Array.get(array, i);
		return beanContext.getClassMeta(type, args);
	}

	private void addToMap(String typeName, ClassMeta<?> cm) {
		map.put(typeName, cm);
		reverseMap.put(cm.innerClass, typeName);
	}

	/**
	 * Gets the class metadata for the specified bean type name.
	 *
	 * @param typeName
	 * 	The bean type name as defined by {@link Bean#typeName() @Bean(typeName)}.
	 * 	Can include multi-dimensional array type names (e.g. <js>"X^^"</js>).
	 * @return The class metadata for the bean.
	 */
	public ClassMeta<?> getClassMeta(String typeName) {
		if (isEmpty || typeName == null)
			return null;
		ClassMeta<?> cm = map.get(typeName);
		if (cm != null)
			return cm;
		if (typeName.charAt(typeName.length()-1) == '^') {
			cm = getClassMeta(typeName.substring(0, typeName.length()-1));
			if (cm != null) {
				cm = beanContext.getClassMeta(Array.newInstance(cm.innerClass, 1).getClass());
				map.put(typeName, cm);
			}
			return cm;
		}
		return null;
	}

	/**
	 * Given the specified class, return the dictionary name for it.
	 *
	 * @param c The class to lookup in this registry.
	 * @return The dictionary name for the specified class in this registry, or <jk>null</jk> if not found.
	 */
	public String getTypeName(ClassMeta<?> c) {
		if (isEmpty)
			return null;
		return reverseMap.get(c.innerClass);
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

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		map.forEach((k,v) -> sb.append(k).append(":").append(v.toString(true)).append(", "));
		sb.append('}');
		return sb.toString();
	}
}
