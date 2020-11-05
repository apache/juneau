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

import static org.apache.juneau.internal.ClassUtils.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.transform.*;

/**
 * Parent class for all bean filters.
 *
 * <p>
 * Bean filters are used to control aspects of how beans are handled during serialization and parsing.
 *
 * <p>
 * Bean filters are created by {@link BeanFilterBuilder} which is the programmatic equivalent to the {@link Bean @Bean}
 * annotation.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc BeanFilters}
 * </ul>
 */
public final class BeanFilter {

	private final Class<?> beanClass;
	private final Set<String> properties, excludeProperties, readOnlyProperties, writeOnlyProperties;
	private final PropertyNamer propertyNamer;
	private final Class<?> implClass, interfaceClass, stopClass;
	private final boolean sortProperties, fluentSetters;
	private final String typeName, example;
	private final Class<?>[] beanDictionary;
	@SuppressWarnings("rawtypes")
	private final BeanInterceptor interceptor;

	/**
	 * Constructor.
	 */
	BeanFilter(BeanFilterBuilder builder) {
		this.beanClass = builder.beanClass;
		this.typeName = builder.typeName;
		this.properties = new LinkedHashSet<>(builder.properties);
		this.excludeProperties = new LinkedHashSet<>(builder.excludeProperties);
		this.readOnlyProperties = new LinkedHashSet<>(builder.readOnlyProperties);
		this.writeOnlyProperties = new LinkedHashSet<>(builder.writeOnlyProperties);
		this.example = builder.example;
		this.implClass = builder.implClass;
		this.interfaceClass = builder.interfaceClass;
		this.stopClass = builder.stopClass;
		this.sortProperties = builder.sortProperties;
		this.fluentSetters = builder.fluentSetters;
		this.propertyNamer = castOrCreate(PropertyNamer.class, builder.propertyNamer);
		this.beanDictionary =
			builder.dictionary == null
			? null
			: builder.dictionary.toArray(new Class<?>[builder.dictionary.size()]);
		this.interceptor =
			builder.interceptor == null
			? BeanInterceptor.DEFAULT
			: castOrCreate(BeanInterceptor.class, builder.interceptor);
	}

	/**
	 * Create a new instance of this bean filter.
	 *
	 * @param <T> The bean class being filtered.
	 * @param beanClass The bean class being filtered.
	 * @return A new {@link BeanFilterBuilder} object.
	 */
	public static <T> BeanFilterBuilder create(Class<T> beanClass) {
		return new BeanFilterBuilder(beanClass);
	}

	/**
	 * Returns the bean class that this filter applies to.
	 *
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
	 * Returns the bean dictionary defined on this bean.
	 *
	 * @return The bean dictionary defined on this bean, or <jk>null</jk> if no bean dictionary is defined.
	 */
	public Class<?>[] getBeanDictionary() {
		return beanDictionary;
	}

	/**
	 * Returns the set and order of names of properties associated with a bean class.
	 *
	 * @return
	 * 	The names of the properties associated with a bean class, or and empty set if all bean properties should
	 * 	be used.
	 */
	public Set<String> getProperties() {
		return properties;
	}

	/**
	 * Returns the list of properties to ignore on a bean.
	 *
	 * @return The names of the properties to ignore on a bean, or an empty set to not ignore any properties.
	 */
	public Set<String> getExcludeProperties() {
		return excludeProperties;
	}

	/**
	 * Returns the list of read-only properties on a bean.
	 *
	 * @return The names of the read-only properties on a bean, or an empty set to not have any read-only properties.
	 */
	public Set<String> getReadOnlyProperties() {
		return readOnlyProperties;
	}

	/**
	 * Returns the list of write-only properties on a bean.
	 *
	 * @return The names of the write-only properties on a bean, or an empty set to not have any write-only properties.
	 */
	public Set<String> getWriteOnlyProperties() {
		return writeOnlyProperties;
	}

	/**
	 * Returns <jk>true</jk> if the properties defined on this bean class should be ordered alphabetically.
	 *
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
	 * Returns <jk>true</jk> if we should find fluent setters.
	 *
	 * @return <jk>true</jk> if fluent setters should be found.
	 */
	public boolean isFluentSetters() {
		return fluentSetters;
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
	 * Returns the implementation class associated with this class.
	 *
	 * @return The implementation class associated with this class, or <jk>null</jk> if no implementation class is associated.
	 */
	public Class<?> getImplClass() {
		return implClass;
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
	 * Returns the example associated with this class.
	 *
	 * @return The example associated with this class, or <jk>null</jk> if no example is associated.
	 */
	public String getExample() {
		return example;
	}

	/**
	 * Calls the {@link BeanInterceptor#readProperty(Object, String, Object)} method on the registered property filters.
	 *
	 * @param bean The bean from which the property was read.
	 * @param name The property name.
	 * @param value The value just extracted from calling the bean getter.
	 * @return The value to serialize.  Default is just to return the existing value.
	 */
	@SuppressWarnings("unchecked")
	public Object readProperty(Object bean, String name, Object value) {
		return interceptor.readProperty(bean, name, value);
	}

	/**
	 * Calls the {@link BeanInterceptor#writeProperty(Object, String, Object)} method on the registered property filters.
	 *
	 * @param bean The bean from which the property was read.
	 * @param name The property name.
	 * @param value The value just parsed.
	 * @return The value to serialize.  Default is just to return the existing value.
	 */
	@SuppressWarnings("unchecked")
	public Object writeProperty(Object bean, String name, Object value) {
		return interceptor.writeProperty(bean, name, value);
	}
}
