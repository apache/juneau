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

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Parent class for all bean filters.
 * <p>
 * Bean filters are used to control aspects of how beans are handled during serialization and parsing.
 * <p>
 * This class can be considered a programmatic equivalent to using the {@link Bean @Bean} annotation on bean classes.
 * Thus, it can be used to perform the same function as the <code>@Bean</code> annotation when you don't have
 * 	the ability to annotate those classes (e.g. you don't have access to the source code).
 */
public class BeanFilter {

	private final Class<?> beanClass;
	private final String[] properties, excludeProperties;
	private final PropertyNamer propertyNamer;
	private final Class<?> interfaceClass, stopClass;
	private final boolean sortProperties;
	private final String typeName;
	private final Class<?>[] beanDictionary;

	/**
	 * Constructor.
	 */
	BeanFilter(BeanFilterBuilder builder) {
		this.beanClass = builder.beanClass;
		this.typeName = builder.typeName;
		this.properties = StringUtils.split(builder.properties, ',');
		this.excludeProperties = StringUtils.split(builder.excludeProperties, ',');
		this.interfaceClass = builder.interfaceClass;
		this.stopClass = builder.stopClass;
		this.sortProperties = builder.sortProperties;
		this.propertyNamer = builder.propertyNamer;
		this.beanDictionary = builder.beanDictionary == null ? null : builder.beanDictionary.toArray(new Class<?>[builder.beanDictionary.size()]);
	}

	/**
	 * Returns the bean class that this filter applies to.
	 * @return The bean class that this filter applies to.
	 */
	public Class<?> getBeanClass() {
		return beanClass;
	}

	/**
	 * Returns the dictionary name associated with this bean.
	 *
	 * @return The dictionary name associated with this bean, or <jk>null</jk> if no name is defined.
	 */
	public String getTypeName() {
		return typeName;
	}

	/**
	 * Returns the set and order of names of properties associated with a bean class.
	 * @return The name of the properties associated with a bean class, or <jk>null</jk> if all bean properties should be used.
	 */
	public String[] getProperties() {
		return properties;
	}

	/**
	 * Returns the bean dictionary defined on this bean.
	 *
	 * @return The bean dictionary defined on this bean, or <jk>null</jk> if no bean dictionary is defined.
	 */
	public Class<?>[] getBeanDictionary() {
		return beanDictionary;
	}

	/**
	 * Returns <jk>true</jk> if the properties defined on this bean class should be ordered alphabetically.
	 * <p>
	 * This method is only used when the {@link #getProperties()} method returns <jk>null</jk>.
	 * Otherwise, the ordering of the properties in the returned value is used.
	 *
	 * @return <jk>true</jk> if bean properties should be sorted.
	 */
	public boolean isSortProperties() {
		return sortProperties;
	}

	/**
	 * Returns the list of properties to ignore on a bean.
	 *
	 * @return The name of the properties to ignore on a bean, or <jk>null</jk> to not ignore any properties.
	 */
	public String[] getExcludeProperties() {
		return excludeProperties;
	}

	/**
	 * Returns the {@link PropertyNamer} associated with the bean to tailor the names of bean properties.
	 *
	 * @return The property namer class, or <jk>null</jk> if no property namer is associated with this bean property.
	 */
	public PropertyNamer getPropertyNamer() {
		return propertyNamer;
	}

	/**
	 * Returns the interface class associated with this class.
	 *
	 * @return The interface class associated with this class, or <jk>null</jk> if no interface class is associated.
	 */
	public Class<?> getInterfaceClass() {
		return interfaceClass;
	}

	/**
	 * Returns the stop class associated with this class.
	 *
	 * @return The stop class associated with this class, or <jk>null</jk> if no stop class is associated.
	 */
	public Class<?> getStopClass() {
		return stopClass;
	}

	/**
	 * Subclasses can override this property to convert property values to some other
	 * 	object just before serialization.
	 *
	 * @param bean The bean from which the property was read.
	 * @param name The property name.
	 * @param value The value just extracted from calling the bean getter.
	 * @return The value to serialize.  Default is just to return the existing value.
	 */
	public Object readProperty(Object bean, String name, Object value) {
		return value;
	}

	/**
	 * Subclasses can override this property to convert property values to some other
	 * 	object just before calling the bean setter.
	 *
	 * @param bean The bean from which the property was read.
	 * @param name The property name.
	 * @param value The value just parsed.
	 * @return <jk>true</jk> if we set the property, <jk>false</jk> if we should allow the
	 * 	framework to call the setter.
	 */
	public boolean writeProperty(Object bean, String name, Object value) {
		return false;
	}
}
