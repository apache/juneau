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
package org.apache.juneau.transform;

import java.util.*;

import org.apache.juneau.annotation.*;

/**
 * Bean filter constructed from a {@link Bean @Bean} annotation found on a class.
 * <p>
 * <b>*** Internal class - Not intended for external use ***</b>
 *
 * @author James Bognar (james.bognar@salesforce.com)
 * @param <T> The class type that this transform applies to.
 */
public final class AnnotationBeanFilter<T> extends BeanFilter<T> {

	/**
	 * Constructor.
	 *
	 * @param annotatedClass The class found to have a {@link Bean @Bean} annotation.
	 * @param annotations The {@link Bean @Bean} annotations found on the class and all parent classes in child-to-parent order.
	 */
	public AnnotationBeanFilter(Class<T> annotatedClass, List<Bean> annotations) {
		super(annotatedClass);

		ListIterator<Bean> li = annotations.listIterator(annotations.size());
		while (li.hasPrevious()) {
			Bean b = li.previous();

			if (b.properties().length > 0 && getProperties() == null)
				setProperties(b.properties());

			if (b.sort())
				setSortProperties(true);

			if (b.excludeProperties().length > 0)
				setExcludeProperties(b.excludeProperties());

			setPropertyNamer(b.propertyNamer());

			if (b.interfaceClass() != Object.class)
				setInterfaceClass(b.interfaceClass());

			if (b.stopClass() != Object.class)
				setStopClass(b.stopClass());

			if (! b.subTypeProperty().isEmpty()) {
				setSubTypeProperty(b.subTypeProperty());

				LinkedHashMap<Class<?>,String> subTypes = new LinkedHashMap<Class<?>,String>();
				for (BeanSubType bst : b.subTypes())
					subTypes.put(bst.type(), bst.id());

				setSubTypes(subTypes);
			}
		}
	}
}
