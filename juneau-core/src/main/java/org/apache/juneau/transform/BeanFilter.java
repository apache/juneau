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

import java.beans.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Parent class for all bean filters.
 * <p>
 * 	Bean filters are used to control aspects of how beans are handled during serialization and parsing.
 * <p>
 * 	This class can be considered a programmatic equivalent to using the {@link Bean @Bean} annotation on bean classes.
 * 	Thus, it can be used to perform the same function as the <code>@Bean</code> annotation when you don't have
 * 		the ability to annotate those classes (e.g. you don't have access to the source code).
 *
 * <p>
 * 	When defining bean filters, you can either call the setters in the contructor, or override getters.
 *
 * <h6 class='topic'>Example</h6>
 * <p class='bcode'>
 * 	<jc>// Create our serializer with a bean filter.</jc>
 * 	WriterSerializer s = <jk>new</jk> JsonSerializer().addBeanFilters(AddressFilter.<jk>class</jk>);
 *
 * 	Address a = <jk>new</jk> Address();
 * 	String json = s.serialize(a); <jc>// Serializes only street, city, state.</jc>
 *
 * 	<jc>// Filter class defined via setters</jc>
 * 	<jk>public class</jk> AddressFilter <jk>extends</jk> BeanFilter&lt;Address&gt; {
 * 		<jk>public</jk> AddressFilter() {
 * 			setProperties(<js>"street"</js>,<js>"city"</js>,<js>"state"</js>);
 * 		}
 * 	}
 *
 * 	<jc>// Filter class defined by overriding getters</jc>
 * 	<jk>public class</jk> AddressFilter <jk>extends</jk> BeanFilter&lt;Address&gt; {
 * 		<jk>public</jk> String[] getProperties() {
 * 			<jk>return new</jk> String[]{<js>"street"</js>,<js>"city"</js>,<js>"state"</js>};
 * 		}
 * 	}
 * </p>
 * <p>
 * 	The examples in this class use the setters approach.
 *
 * <h6 class='topic'>Additional information</h6>
 * 	See {@link org.apache.juneau.transform} for more information.
 *
 *
 * @author James Bognar (james.bognar@salesforce.com)
 * @param <T> The class type that this filter applies to.
 */
public abstract class BeanFilter<T> {

	private final Class<T> beanClass;
	private final String[] properties, excludeProperties;
	private final Map<Class<?>, String> subTypes;
	private final String subTypeAttr;
	private final PropertyNamer propertyNamer;
	private final Class<?> interfaceClass, stopClass;
	private final boolean sortProperties;
	private final ClassLexicon classLexicon;

	/**
	 * Constructor.
	 *
	 * @param beanClass
	 * 	The bean class that this filter applies to.
	 * 	If <jk>null</jk>, then the value is inferred through reflection.
	 * @param properties
	 * 	Specifies the set and order of names of properties associated with a bean class.
	 * 	The order specified is the same order that the entries will be returned by the {@link BeanMap#entrySet()} and related methods.
	 * 	Entries in the list can also contain comma-delimited lists that will be split.
	 * @param excludeProperties
	 * 	Specifies a list of properties to ignore on a bean.
	 * @param interfaceClass
	 * 	Identifies a class to be used as the interface class for this and all subclasses.
	 * 	<p>
	 * 	When specified, only the list of properties defined on the interface class will be used during serialization.
	 * 	Additional properties on subclasses will be ignored.
	 * 	<p class='bcode'>
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
	 * 	<jk>public class</jk> AFilter <jk>extends</jk> BeanFilter&lt;A&gt; {
	 * 		<jk>public</jk> AFilter() {
	 * 			setInterfaceClass(A.<jk>class</jk>);
	 * 		}
	 * 	}
	 *
	 * 	JsonSerializer s = new JsonSerializer().addBeanFilters(AFilter.<jk>class</jk>);
	 * 	A1 a1 = <jk>new</jk> A1();
	 * 	String r = s.serialize(a1);
	 * 	<jsm>assertEquals</jsm>(<js>"{f0:'f0'}"</js>, r);  <jc>// Note f1 is not serialized</jc>
	 * 	</p>
	 *	 	<p>
	 * 	Note that this filter can be used on the parent class so that it filters to all child classes,
	 * 		or can be set individually on the child classes.
	 * @param stopClass
	 * 	Identifies a stop class for this class and all subclasses.
	 * 	<p>
	 * 	Identical in purpose to the stop class specified by {@link Introspector#getBeanInfo(Class, Class)}.
	 * 	Any properties in the stop class or in its base classes will be ignored during analysis.
	 * 	<p>
	 * 	For example, in the following class hierarchy, instances of <code>C3</code> will include property <code>p3</code>, but
	 * 		not <code>p1</code> or <code>p2</code>.
	 * 	<p class='bcode'>
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
	 * 	</p>
	 * @param sortProperties
	 * 	Sort properties in alphabetical order.
	 * @param propertyNamer
	 * 	The property namer to use to name bean properties.
	 * @param classLexicon
	 * 	The class lexicon to use for resolving class identifier names from classes.
	 * @param subTypeProperty
	 * 	Defines a virtual property on a superclass that identifies bean subtype classes.
	 * 	<p>
	 * 	In the following example, the abstract class has two subclasses that are differentiated
	 * 		by a property called <code>subType</code>
	 *
	 * 	<p class='bcode'>
	 * 	<jc>// Abstract superclass</jc>
	 * 	<jk>public abstract class</jk> A {
	 * 		<jk>public</jk> String <jf>f0</jf> = <js>"f0"</js>;
	 * 	}
	 *
	 * 	<jc>// Subclass 1</jc>
	 * 	<jk>public class</jk> A1 <jk>extends</jk> A {
	 * 		<jk>public</jk> String <jf>f1</jf>;
	 * 	}
	 *
	 * 	<jc>// Subclass 2</jc>
	 * 	<jk>public class</jk> A2 <jk>extends</jk> A {
	 * 		<jk>public</jk> String <jf>f2</jf>;
	 * 	}
	 *
	 * 	<jc>// Filter for defining subtypes</jc>
	 * 	<jk>public class</jk> AFilter <jk>extends</jk> BeanFilter&lt;A&gt; {
	 * 		<jk>public</jk> AFilter() {
	 * 			<jk>super</jk>(A.<jk>class</jk>, <jk>null</jk>, <jk>null</jk>, <jk>null</jk>, <jk>null</jk>, <jk>false</jk>, <jk>null</jk>, <jk>null</jk>, <js>"subType"</js>, <jsm>createSubTypes</jsm>())
	 * 		}
	 * 		<jk>private static</jk> Map&lt;Class&lt;?&gt;,String&gt; <jsm>createSubTypes</jsm>() {
	 * 			HashMap&lt;Class&lt;?&gt;,String&gt; m = new HashMap&lt;Class&lt;?&gt;,String&gt;();
	 * 			m.put(A1.<jk>class</jk>,<js>"A1"</js>);
	 * 			m.put(A2.<jk>class</jk>,<js>"A2"</js>);
	 * 			<jk>return</jk> m;
	 * 		}
	 * 	}
	 * 	</p>
	 * 	<p>
	 * 	The following shows what happens when serializing a subclassed object to JSON:
	 * 	<p class='bcode'>
	 * 	JsonSerializer s = <jk>new</jk> JsonSerializer().addBeanFilters(AFilter.<jk>class</jk>);
	 * 	A1 a1 = <jk>new</jk> A1();
	 * 	a1.<jf>f1</jf> = <js>"f1"</js>;
	 * 	String r = s.serialize(a1);
	 * 	<jsm>assertEquals</jsm>(<js>"{subType:'A1',f1:'f1',f0:'f0'}"</js>, r);
	 *		</p>
	 * 	<p>
	 * 	The following shows what happens when parsing back into the original object.
	 * 	<p class='bcode'>
	 * 	JsonParser p = <jk>new</jk> JsonParser().addBeanFilters(AFilter.<jk>class</jk>);
	 * 	A a = p.parse(r, A.<jk>class</jk>);
	 * 	<jsm>assertTrue</jsm>(a <jk>instanceof</jk> A1);
	 * 	</p>
	 * @param subTypes
	 */
	@SuppressWarnings("unchecked")
	public BeanFilter(Class<T> beanClass, String[] properties, String[] excludeProperties, Class<?> interfaceClass, Class<?> stopClass, boolean sortProperties, PropertyNamer propertyNamer, ClassLexicon classLexicon, String subTypeProperty, Map<Class<?>,String> subTypes) {

		if (beanClass == null) {
			Class<?> c = this.getClass().getSuperclass();
			Type t = this.getClass().getGenericSuperclass();
			while (c != BeanFilter.class) {
				t = c.getGenericSuperclass();
				c = c.getSuperclass();
			}

			// Attempt to determine the T and G classes using reflection.
			if (t instanceof ParameterizedType) {
				ParameterizedType pt = (ParameterizedType)t;
				Type[] pta = pt.getActualTypeArguments();
				if (pta.length > 0) {
					Type nType = pta[0];
					if (nType instanceof Class)
						beanClass = (Class<T>)nType;

					else
						throw new RuntimeException("Unsupported parameter type: " + nType);
				}
			}
		}

		this.beanClass = beanClass;
		this.properties = StringUtils.split(properties, ',');
		this.excludeProperties = StringUtils.split(excludeProperties, ',');
		this.interfaceClass = interfaceClass;
		this.stopClass = stopClass;
		this.sortProperties = sortProperties;
		this.propertyNamer = propertyNamer;
		this.classLexicon = classLexicon;
		this.subTypeAttr = subTypeProperty;
		this.subTypes = subTypes == null ? null : Collections.unmodifiableMap(subTypes);
	}

	/**
	 * Convenience constructor for defining interface bean filters.
	 *
	 * @param interfaceClass The interface class.
	 */
	@SuppressWarnings("unchecked")
	public BeanFilter(Class<?> interfaceClass) {
		this((Class<T>)interfaceClass, null, null, interfaceClass, null, false, null, null, null, null);
	}

	/**
	 * Convenience constructor for defining a bean filter that simply specifies the properties and order of properties for a bean.
	 *
	 * @param properties
	 */
	public BeanFilter(String...properties) {
		this(null, properties, null, null, null, false, null, null, null, null);
	}

	/**
	 * Dummy constructor.
	 */
	public BeanFilter() {
		this((Class<T>)null);
	}

	/**
	 * Returns the bean class that this filter applies to.
	 * @return The bean class that this filter applies to.
	 */
	public Class<T> getBeanClass() {
		return beanClass;
	}

	/**
	 * Returns the set and order of names of properties associated with a bean class.
	 * @return The name of the properties associated with a bean class, or <jk>null</jk> if all bean properties should be used.
	 */
	public String[] getProperties() {
		return properties;
	}

	/**
	 * Returns <jk>true</jk> if the properties defined on this bean class should be ordered alphabetically.
	 * <p>
	 * 	This method is only used when the {@link #getProperties()} method returns <jk>null</jk>.
	 * 	Otherwise, the ordering of the properties in the returned value is used.
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
	 * Returns the name of the sub type property associated with the bean class.
	 *
	 * @return The sub type property name, or <jk>null</jk> if bean has no subtypes defined.
	 */
	public String getSubTypeProperty() {
		return subTypeAttr;
	}

	/**
	 * Returns the class lexicon to use for this bean.
	 *
	 * @return The class lexicon to use for this bean.
	 */
	public ClassLexicon getClassLexicon() {
		return classLexicon;
	}

	/**
	 * Returns the subtypes associated with the bean class.
	 *
	 * @return The set of sub types associated with this bean class, or <jk>null</jk> if bean has no subtypes defined.
	 */
	public Map<Class<?>, String> getSubTypes() {
		return subTypes;
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
