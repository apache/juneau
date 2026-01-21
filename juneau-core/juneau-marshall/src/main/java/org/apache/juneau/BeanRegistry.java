/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau;

import static org.apache.juneau.commons.reflect.ReflectionUtils.*;
import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.cp.*;

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
 */
public class BeanRegistry {

	private final Map<String,ClassMeta<?>> map;
	private final Map<Class<?>,String> reverseMap;
	private final BeanContext bc;
	private final AnnotationProvider ap;
	private final boolean isEmpty;

	BeanRegistry(BeanContext bc, BeanRegistry parent, List<ClassInfo> classes) {
		assertArgNotNull("bc", bc);
		assertArgNotNull("classes", classes);
		this.bc = bc;
		this.ap = bc.getAnnotationProvider();
		this.map = new ConcurrentHashMap<>();
		this.reverseMap = new ConcurrentHashMap<>();
		bc.getBeanDictionary().forEach(this::addClass);
		if (nn(parent))
			parent.map.forEach(this::addToMap);
		classes.forEach(this::addClass);
		isEmpty = map.isEmpty();
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
		var cm = map.get(typeName);
		if (nn(cm))
			return cm;
		if (typeName.charAt(typeName.length() - 1) == '^') {
			cm = getClassMeta(typeName.substring(0, typeName.length() - 1));
			if (nn(cm)) {
				cm = bc.getClassMeta(Array.newInstance(cm.inner(), 1).getClass());
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
		return isEmpty ? null : reverseMap.get(c.inner());
	}

	/**
	 * Returns <jk>true</jk> if this dictionary has an entry for the specified type name.
	 *
	 * @param typeName The bean type name.
	 * @return <jk>true</jk> if this dictionary has an entry for the specified type name.
	 */
	public boolean hasName(String typeName) {
		return nn(getClassMeta(typeName));
	}

	protected FluentMap<String,Object> properties() {
		var m = filteredBeanPropertyMap();
		map.forEach((k, v) -> m.a(k, v.toString(true)));
		return m;
	}

	@Override
	public String toString() {
		return r(properties());
	}

	private void addClass(ClassInfo ci) {
		try {
			if (nn(ci) && nn(ci.inner())) {
				if (ci.isAssignableTo(Collection.class)) {
					Collection<?> cc = BeanCreator.of(Collection.class).type(ci).run();
					cc.forEach(x -> {
						if (x instanceof Class<?> x2)
							addClass(info(x2));
						else
							throw bex("Collection class ''{0}'' passed to BeanRegistry does not contain Class objects.", ci.getName());
					});
				} else if (ci.isAssignableTo(Map.class)) {
					Map<?,?> m = BeanCreator.of(Map.class).type(ci).run();
					m.forEach((k, v) -> {
						var typeName = s(k);
						var val = (ClassMeta<?>)null;
						if (v instanceof Type v2)
							val = bc.getClassMeta(v2);
						else if (isArray(v))
							val = getTypedClassMeta(v);
						else
							throw bex("Class ''{0}'' was passed to BeanRegistry but value of type ''{1}'' found in map is not a Type object.", ci.getName(), cn(v));
						addToMap(typeName, val);
					});
				} else {
					// @formatter:off
					var typeName = ap.find(Bean.class, ci)
						.stream()
						.map(x -> x.inner().typeName())
						.filter(Utils::ne)
						.findFirst()
						.orElseThrow(() -> bex("Class ''{0}'' was passed to BeanRegistry but it doesn't have a @Bean(typeName) annotation defined.", ci.getName()));
					// @formatter:on
					addToMap(typeName, bc.getClassMeta(ci.inner()));
				}
			}
		} catch (BeanRuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw bex(e);
		}
	}

	private void addToMap(String typeName, ClassMeta<?> cm) {
		map.put(typeName, cm);
		reverseMap.put(cm.inner(), typeName);
	}

	private ClassMeta<?> getTypedClassMeta(Object array) {
		var len = Array.getLength(array);
		if (len == 0)
			throw bex("Map entry had an empty array value.");
		var type = (Type)Array.get(array, 0);
		var args = new Type[len - 1];
		for (var i = 1; i < len; i++)
			args[i - 1] = (Type)Array.get(array, i);
		return bc.getClassMeta(type, args);
	}
}