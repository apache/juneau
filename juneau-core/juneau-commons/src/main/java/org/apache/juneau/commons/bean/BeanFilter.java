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
package org.apache.juneau.commons.bean;

import java.util.List;
import java.util.Set;

import org.apache.juneau.commons.reflect.ClassInfo;
import org.apache.juneau.commons.reflect.ClassInfoTyped;

/**
 * Bean-modeling SPI seam that exposes the per-class filter surface the bean-runtime types
 * ({@link BeanMeta}-equivalent) need without coupling the bean-modeling layer to the marshalling-side
 * {@code MarshalledFilter}.
 *
 * <p>
 * Marshalling-side {@code MarshalledFilter} implements this interface; bean-modeling-side code only
 * sees {@link BeanFilter}.  This interface is restricted to commons-compatible return types
 * (collections, {@link ClassInfo}, {@link PropertyNamer}, plain values).
 */
public interface BeanFilter {

	/**
	 * @return The bean class that this filter applies to, or <jk>null</jk> if this is a non-bean filter.
	 */
	ClassInfoTyped<?> getBeanClass();

	/**
	 * @return The class that this filter applies to.
	 */
	Class<?> getMarshalledClass();

	/**
	 * @return The dictionary name associated with this bean, or <jk>null</jk> if no name is defined.
	 */
	String getTypeName();

	/**
	 * @return The example associated with this class, or <jk>null</jk>.
	 */
	String getExample();

	/**
	 * @return The implementation class associated with this class, or <jk>null</jk>.
	 */
	ClassInfo getImplClass();

	/**
	 * @return The interface class associated with this class, or <jk>null</jk>.
	 */
	ClassInfo getInterfaceClass();

	/**
	 * @return The stop class associated with this class, or <jk>null</jk>.
	 */
	ClassInfo getStopClass();

	/**
	 * @return The names of the properties associated with the bean class (ordered), or an empty set if all properties.
	 */
	Set<String> getProperties();

	/**
	 * @return The names of the properties to ignore on a bean, or an empty set.
	 */
	Set<String> getExcludeProperties();

	/**
	 * @return The names of the read-only properties on a bean, or an empty set.
	 */
	Set<String> getReadOnlyProperties();

	/**
	 * @return The names of the write-only properties on a bean, or an empty set.
	 */
	Set<String> getWriteOnlyProperties();

	/**
	 * @return The property namer for this filter, or <jk>null</jk>.
	 */
	PropertyNamer getPropertyNamer();

	/**
	 * @return The list of bean dictionary classes, or an empty list.
	 */
	List<ClassInfo> getBeanDictionary();

	/**
	 * @return <jk>true</jk> if fluent setters should be found.
	 */
	boolean isFluentSetters();

	/**
	 * @return <jk>true</jk> if this bean opts out of alphabetical property sorting.
	 */
	boolean isUnsortedProperties();

	/**
	 * Calls the {@link BeanInterceptor#readProperty(Object, String, Object)} on the registered interceptor.
	 *
	 * @param bean The bean from which the property was read.
	 * @param name The property name.
	 * @param value The value just extracted from calling the bean getter.
	 * @return The value to serialize.  Default is just to return the existing value.
	 */
	Object readProperty(Object bean, String name, Object value);

	/**
	 * Calls the {@link BeanInterceptor#writeProperty(Object, String, Object)} on the registered interceptor.
	 *
	 * @param bean The bean to which the property is being written.
	 * @param name The property name.
	 * @param value The value just parsed.
	 * @return The value to assign.  Default is just to return the existing value.
	 */
	Object writeProperty(Object bean, String name, Object value);
}
