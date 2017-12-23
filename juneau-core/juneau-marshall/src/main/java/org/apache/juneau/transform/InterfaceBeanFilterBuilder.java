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
package org.apache.juneau.transform;

import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Simple bean filter that simply identifies a class to be used as an interface class for all child classes.
 *
 * <p>
 * These objects are created when you pass in non-<code>BeanFilterBuilder</code> classes to
 * {@link BeanContextBuilder#beanFilters(Class...)}, and are equivalent to adding a
 * <code><ja>@Bean</ja>(interfaceClass=Foo.<jk>class</jk>)</code> annotation on the <code>Foo</code> class.
 */
public class InterfaceBeanFilterBuilder extends BeanFilterBuilder {

	/**
	 * Constructor.
	 * 
	 * @param bc Bean context used for instantiating {@link PropertyNamer} instances. 
	 * @param interfaceClass The class to use as an interface on all child classes.
	 */
	public InterfaceBeanFilterBuilder(BeanContext bc, Class<?> interfaceClass) {
		super(interfaceClass);
		interfaceClass(interfaceClass);
		Map<Class<?>,Bean> annotations = ReflectionUtils.findAnnotationsMap(Bean.class, interfaceClass);

		ListIterator<Bean> li = new ArrayList<>(annotations.values()).listIterator(annotations.size());
		while (li.hasPrevious()) {
			Bean b = li.previous();

			if (! b.properties().isEmpty())
				properties(split(b.properties()));

			if (! b.typeName().isEmpty())
				typeName(b.typeName());

			if (b.sort())
				sortProperties(true);

			if (! b.excludeProperties().isEmpty())
				excludeProperties(split(b.excludeProperties()));

			try {
				if (b.propertyNamer() != PropertyNamerDefault.class)
					propertyNamer(bc, b.propertyNamer());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			if (b.interfaceClass() != Object.class)
				interfaceClass(b.interfaceClass());

			if (b.stopClass() != Object.class)
				stopClass(b.stopClass());

			if (b.beanDictionary().length > 0)
				beanDictionary(b.beanDictionary());
		}
	}
}
