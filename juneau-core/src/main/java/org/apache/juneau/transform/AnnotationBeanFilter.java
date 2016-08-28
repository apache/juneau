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

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;

/**
 * Bean filter constructed from a {@link Bean @Bean} annotation found on a class.
 * <p>
 * <b>*** Internal class - Not intended for external use ***</b>
 *
 * @author James Bognar (james.bognar@salesforce.com)
 * @param <T> The class type that this bean filter applies to.
 */
public final class AnnotationBeanFilter<T> extends BeanFilter<T> {

	/**
	 * Constructor.
	 *
	 * @param annotatedClass The class found to have a {@link Bean @Bean} annotation.
	 * @param annotations The {@link Bean @Bean} annotations found on the class and all parent classes in child-to-parent order.
	 */
	public AnnotationBeanFilter(Class<T> annotatedClass, List<Bean> annotations) {
		this(new Builder<T>(annotatedClass, annotations));
	}

	private AnnotationBeanFilter(Builder<T> b) {
		super(b.beanClass, b.properties, b.excludeProperties, b.interfaceClass, b.stopClass, b.sortProperties, b.propertyNamer);

		// Temp
		setSubTypeProperty(b.subTypeProperty);
		setSubTypes(b.subTypes);
	}

	private static class Builder<T> {
		Class<T> beanClass;
		String[] properties;
		String[] excludeProperties;
		Class<?> interfaceClass;
		Class<?> stopClass;
		boolean sortProperties;
		PropertyNamer propertyNamer;
		String subTypeProperty;
		LinkedHashMap<Class<?>,String> subTypes = new LinkedHashMap<Class<?>,String>();

		private Builder(Class<T> annotatedClass, List<Bean> annotations) {
			this.beanClass = annotatedClass;
			ListIterator<Bean> li = annotations.listIterator(annotations.size());
			while (li.hasPrevious()) {
				Bean b = li.previous();

				if (b.properties().length > 0 && properties == null)
					properties = b.properties();

				if (b.sort())
					sortProperties = true;

				if (b.excludeProperties().length > 0 && excludeProperties == null)
					excludeProperties = b.excludeProperties();

				if (b.propertyNamer() != PropertyNamerDefault.class)
					try {
						propertyNamer = b.propertyNamer().newInstance();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}

				if (b.interfaceClass() != Object.class && interfaceClass == null)
					interfaceClass = b.interfaceClass();

				if (b.stopClass() != Object.class)
					stopClass = b.stopClass();


				if (! b.subTypeProperty().isEmpty()) {
					subTypeProperty = b.subTypeProperty();

					for (BeanSubType bst : b.subTypes())
						subTypes.put(bst.type(), bst.id());
				}
			}
		}
	}
}
