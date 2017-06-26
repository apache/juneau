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

import static org.apache.juneau.internal.ClassUtils.*;

import java.beans.*;
import java.util.*;

import org.apache.juneau.*;

/**
 * Builder class for {@link BeanFilter} objects.
 * <p>
 * Bean filter builders must have a public no-arg constructor.
 * Builder settings should be set in the constructor using the provided setters on this class.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Create our serializer with a bean filter.</jc>
 * 	WriterSerializer s = <jk>new</jk> JsonSerializerBuilder().beanFilters(AddressFilter.<jk>class</jk>).build();
 *
 * 	Address a = <jk>new</jk> Address();
 * 	String json = s.serialize(a); <jc>// Serializes only street, city, state.</jc>
 *
 * 	<jc>// Filter class defined via setters</jc>
 * 	<jk>public class</jk> AddressFilter <jk>extends</jk> BeanFilterBuilder {
 * 		<jk>public</jk> AddressFilter() {
 * 			super(Address.<jk>class</jk>);
 * 			setProperties(<js>"street"</js>,<js>"city"</js>,<js>"state"</js>);
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>Additional information:</h5>
 * See <a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.transform</a> for more information.
 */
@SuppressWarnings("hiding")
public abstract class BeanFilterBuilder {

	Class<?> beanClass;
	String typeName;
	String[] properties, excludeProperties;
	Class<?> interfaceClass, stopClass;
	boolean sortProperties;
	PropertyNamer propertyNamer;
	List<Class<?>> beanDictionary;

	/**
	 * Constructor.
	 *
	 * @param beanClass The bean class that this filter applies to.
	 */
	public BeanFilterBuilder(Class<?> beanClass) {
		this.beanClass = beanClass;
	}

	/**
	 * Specifies the type name for this bean.
	 *
	 * @param typeName The dictionary name associated with this bean.
	 * @return This object (for method chaining).
	 */
	public BeanFilterBuilder typeName(String typeName) {
		this.typeName = typeName;
		return this;
	}

	/**
	 * Specifies the set and order of names of properties associated with the bean class.
	 * The order specified is the same order that the entries will be returned by the {@link BeanMap#entrySet()} and
	 * related methods.
	 * Entries in the list can also contain comma-delimited lists that will be split.
	 *
	 * @param properties The properties associated with the bean class.
	 * @return This object (for method chaining).
	 */
	public BeanFilterBuilder properties(String...properties) {
		this.properties = properties;
		return this;
	}

	/**
	 * Specifies the list of properties to ignore on a bean.
	 *
	 * @param excludeProperties The list of properties to ignore on a bean.
	 * @return This object (for method chaining).
	 */
	public BeanFilterBuilder excludeProperties(String...excludeProperties) {
		this.excludeProperties = excludeProperties;
		return this;
	}

	/**
	 * Identifies a class to be used as the interface class for this and all subclasses.
	 * <p>
	 * When specified, only the list of properties defined on the interface class will be used during serialization.
	 * Additional properties on subclasses will be ignored.
	 * <p class='bcode'>
	 * 	<jc>// Parent class</jc>
	 * 	<jk>public abstract class</jk> A {
	 * 		<jk>public</jk> String <jf>f0</jf> = <js>"f0"</js>;
	 * 	}
	 *
	 * 	<jc>// Sub class</jc>
	 * 	<jk>public class</jk> A1 <jk>extends</jk> A {
	 * 		<jk>public</jk> String <jf>f1</jf> = <js>"f1"</js>;
	 * 	}
	 *
	 * 	<jc>// Filter class</jc>
	 * 	<jk>public class</jk> AFilter <jk>extends</jk> BeanFilterBuilder {
	 * 		<jk>public</jk> AFilter() {
	 * 			super(A.<jk>class</jk>);
	 * 			setInterfaceClass(A.<jk>class</jk>);
	 * 		}
	 * 	}
	 *
	 * 	JsonSerializer s = new JsonSerializerBuilder().beanFilters(AFilter.<jk>class</jk>).build();
	 * 	A1 a1 = <jk>new</jk> A1();
	 * 	String r = s.serialize(a1);
	 * 	<jsm>assertEquals</jsm>(<js>"{f0:'f0'}"</js>, r);  <jc>// Note f1 is not serialized</jc>
	 * </p>
	 * <p>
	 * Note that this filter can be used on the parent class so that it filters to all child classes, or can be set
	 * individually on the child classes.
	 *
	 * @param interfaceClass The interface class to use for this bean class.
	 * @return This object (for method chaining).
	 */
	public BeanFilterBuilder interfaceClass(Class<?> interfaceClass) {
		this.interfaceClass = interfaceClass;
		return this;
	}

	/**
	 * Identifies a stop class for this class and all subclasses.
	 * <p>
	 * Identical in purpose to the stop class specified by {@link Introspector#getBeanInfo(Class, Class)}.
	 * Any properties in the stop class or in its base classes will be ignored during analysis.
	 * <p>
	 * For example, in the following class hierarchy, instances of <code>C3</code> will include property <code>p3</code>,
	 * but not <code>p1</code> or <code>p2</code>.
	 * <p class='bcode'>
	 * 	<jk>public class</jk> C1 {
	 * 		<jk>public int</jk> getP1();
	 * 	}
	 *
	 * 	<jk>public class</jk> C2 <jk>extends</jk> C1 {
	 * 		<jk>public int</jk> getP2();
	 * 	}
	 *
	 * 	<ja>@Bean</ja>(stopClass=C2.<jk>class</jk>)
	 * 	<jk>public class</jk> C3 <jk>extends</jk> C2 {
	 * 		<jk>public int</jk> getP3();
	 * 	}
	 * </p>
	 *
	 * @param stopClass
	 * @return This object (for method chaining).
	 */
	public BeanFilterBuilder stopClass(Class<?> stopClass) {
		this.stopClass = stopClass;
		return this;
	}

	/**
	 * Sort properties in alphabetical order.
	 *
	 * @param sortProperties
	 * @return This object (for method chaining).
	 */
	public BeanFilterBuilder sortProperties(boolean sortProperties) {
		this.sortProperties = sortProperties;
		return this;
	}

	/**
	 * The property namer to use to name bean properties.
	 *
	 * @param propertyNamer The property namer instance.
	 * @return This object (for method chaining).
	 */
	public BeanFilterBuilder propertyNamer(PropertyNamer propertyNamer) {
		this.propertyNamer = propertyNamer;
		return this;
	}

	/**
	 * The property namer to use to name bean properties.
	 *
	 * @param c The property namer class.  Must have a public no-arg constructor.
	 * @return This object (for method chaining).
	 * @throws Exception Thrown from constructor method.
	 */
	public BeanFilterBuilder propertyNamer(Class<? extends PropertyNamer> c) throws Exception {
		this.propertyNamer = newInstance(PropertyNamer.class, c);
		return this;
	}

	/**
	 * Sets the contents of this bean's bean dictionary.
	 *
	 * @param c The classes to set on this bean's bean dictionary.
	 * @return This object (for method chaining).
	 */
	public BeanFilterBuilder setBeanDictionary(Class<?>...c) {
		beanDictionary = new ArrayList<Class<?>>(Arrays.asList(c));
		return this;
	}

	/**
	 * Adds classes to this bean's bean dictionary.
	 *
	 * @param c The classes to add to this bean's bean dictionary.
	 * @return This object (for method chaining).
	 */
	public BeanFilterBuilder beanDictionary(Class<?>...c) {
		if (beanDictionary == null)
			beanDictionary = new ArrayList<Class<?>>(Arrays.asList(c));
		else for (Class<?> cc : c)
			beanDictionary.add(cc);
		return this;
	}

	/**
	 * Creates a {@link BeanFilter} with settings in this builder class.
	 *
	 * @return A new {@link BeanFilter} instance.
	 */
	public BeanFilter build() {
		return new BeanFilter(this);
	}
}
