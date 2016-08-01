/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.filter;

import java.beans.*;
import java.lang.reflect.*;
import java.util.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;

/**
 * Parent class for all bean filters.
 * <p>
 * 	Bean filters are used to control aspects of how beans are handled during serialization and parsing.
 * <p>
 * 	This class can be considered a programmatic equivalent to using the {@link Bean @Bean} annotation on bean classes.
 * 	Thus, it can be used to perform the same function as the <code>@Bean</code> annotation when you don't have
 * 		the ability to annotate those classes (e.g. you don't have access to the source code).
 * <p>
 * 	Note that value returned by the {@link Filter#forClass()} method is automatically determined through reflection
 * 		when the no-arg constructor is used.
 *
 * <p>
 * 	When defining bean filters, you can either call the setters in the contructor, or override getters.
 *
 * <h6 class='topic'>Example</h6>
 * <p class='bcode'>
 * 	<jc>// Create our serializer with a filter.</jc>
 * 	WriterSerializer s = <jk>new</jk> JsonSerializer().addFilters(AddressFilter.<jk>class</jk>);
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
 * 	See {@link com.ibm.juno.core.filter} for more information.
 *
 *
 * @author James Bognar (jbognar@us.ibm.com)
 * @param <T> The class type that this filter applies to.
 */
public abstract class BeanFilter<T> extends Filter {

	private String[] properties, excludeProperties;
	private LinkedHashMap<Class<?>, String> subTypes;
	private String subTypeAttr;
	private Class<? extends PropertyNamer> propertyNamer;
	private Class<?> interfaceClass, stopClass;

	/**
	 * Constructor that determines the for-class value using reflection.
	 */
	@SuppressWarnings("unchecked")
	public BeanFilter() {
		super();
		this.type = FilterType.BEAN;

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
					this.forClass = (Class<T>)nType;

				else
					throw new RuntimeException("Unsupported parameter type: " + nType);
			}
		}
	}

	/**
	 * Constructor that specifies the for-class explicitly.
	 * <p>
	 * This constructor only needs to be called when the class type cannot be inferred through reflection.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * <p class='bcode'>
	 * 	<jk>public class</jk> SomeArbitraryFilter <jk>extends</jk> BeanFilter&lt?&gt; {
	 * 		<jk>public</jk> SomeArbitraryFilter(Class&lt?&gt; forClass) {
	 * 			<jk>super</jk>(forClass);
	 * 			...
	 * 		}
	 * 	}
	 * </p>
	 * 	</dd>
	 * </dl>
	 *
	 * @param forClass The class that this bean filter applies to.
	 */
	public BeanFilter(Class<T> forClass) {
		super(forClass);
		this.type = FilterType.BEAN;
	}

	/**
	 * Returns the set and order of names of properties associated with a bean class.
	 *
	 * @see #setProperties(String...)
	 * @return The name of the properties associated with a bean class, or <jk>null</jk> if all bean properties should be used.
	 */
	public String[] getProperties() {
		return properties;
	}

	/**
	 * Specifies the set and order of names of properties associated with a bean class.
	 * <p>
	 * 	The order specified is the same order that the entries will be returned by the {@link BeanMap#entrySet()} and related methods.
	 * <p>
	 * 	This method is an alternative to using the {@link Bean#properties()} annotation on a class.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * <p class='bcode'>
	 * 	<jc>// Create our serializer with a filter.</jc>
	 * 	WriterSerializer s = <jk>new</jk> JsonSerializer().addFilters(AddressFilter.<jk>class</jk>);
	 *
	 * 	Address a = <jk>new</jk> Address();
	 * 	String json = s.serialize(a); <jc>// Serializes only street, city, state.</jc>
	 *
	 * 	<jc>// Filter class</jc>
	 * 	<jk>public class</jk> AddressFilter <jk>extends</jk> BeanFilter&lt;Address&gt; {
	 * 		<jk>public</jk> AddressFilter() {
	 * 			setProperties(<js>"street"</js>,<js>"city"</js>,<js>"state"</js>);
	 * 		}
	 * 	}
	 * </p>
	 * 	</dd>
	 * </dl>
	 *
	 * @param properties The name of the properties associated with a bean class.
	 * @return This object (for method chaining).
	 */
	public BeanFilter<T> setProperties(String...properties) {
		this.properties = properties;
		return this;
	}

	/**
	 * Returns the list of properties to ignore on a bean.
	 *
	 * @see #setExcludeProperties(String...)
	 * @return The name of the properties to ignore on a bean, or <jk>null</jk> to not ignore any properties.
	 */
	public String[] getExcludeProperties() {
		return excludeProperties;
	}

	/**
	 * Specifies a list of properties to ignore on a bean.
	 * <p>
	 * 	This method is an alternative to using the {@link Bean#excludeProperties()} annotation on a class.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * <p class='bcode'>
	 * 	<jc>// Create our serializer with a filter.</jc>
	 * 	WriterSerializer s = <jk>new</jk> JsonSerializer().addFilters(NoCityOrStateFilter.<jk>class</jk>);
	 *
	 * 	Address a = <jk>new</jk> Address();
	 * 	String json = s.serialize(a); <jc>// Excludes city and state.</jc>
	 *
	 * 	<jc>// Filter class</jc>
	 * 	<jk>public class</jk> NoCityOrStateFilter <jk>extends</jk> BeanFilter&lt;Address&gt; {
	 * 		<jk>public</jk> AddressFilter() {
	 * 			setExcludeProperties(<js>"city"</js>,<js>"state"</js>);
	 * 		}
	 * 	}
	 * </p>
	 * 	</dd>
	 * </dl>
	 *
	 * @param excludeProperties The name of the properties to ignore on a bean class.
	 * @return This object (for method chaining).
	 */
	public BeanFilter<T> setExcludeProperties(String...excludeProperties) {
		this.excludeProperties = excludeProperties;
		return this;
	}

	/**
	 * Returns the {@link PropertyNamer} associated with the bean to tailor the names of bean properties.
	 *
	 * @see #setPropertyNamer(Class)
	 * @return The property namer class, or <jk>null</jk> if no property namer is associated with this bean property.
	 */
	public Class<? extends PropertyNamer> getPropertyNamer() {
		return propertyNamer;
	}

	/**
	 * Associates a {@link PropertyNamer} with this bean to tailor the names of the bean properties.
	 * <p>
	 * 	Property namers are used to transform bean property names from standard form to some other form.
	 * 	For example, the {@link PropertyNamerDashedLC} will convert property names to dashed-lowercase, and
	 * 		these will be used as attribute names in JSON, and element names in XML.
	 * <p>
	 * 	This method is an alternative to using the {@link Bean#propertyNamer()} annotation on a class.
	 *
	 * @param propertyNamer The property namer class.
	 * @return This object (for method chaining).
	 */
	public BeanFilter<T> setPropertyNamer(Class<? extends PropertyNamer> propertyNamer) {
		this.propertyNamer = propertyNamer;
		return this;
	}

	/**
	 * Returns the name of the sub type property associated with the bean class.
	 *
	 * @see #setSubTypeProperty(String)
	 * @return The sub type property name, or <jk>null</jk> if bean has no subtypes defined.
	 */
	public String getSubTypeProperty() {
		return subTypeAttr;
	}

	/**
	 * Defines a virtual property on a superclass that identifies bean subtype classes.
	 * <p>
	 * 	In the following example, the abstract class has two subclasses that are differentiated
	 * 		by a property called <code>subType</code>
	 *
	 * <p class='bcode'>
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
	 * 			setSubTypeProperty(<js>"subType"</js>);
	 * 			addSubType(Al.<jk>class</jk>, <js>"A1"</js>);
	 * 			addSubType(A2.<jk>class</jk>, <js>"A2"</js>);
	 * 		}
	 * 	}
	 * </p>
	 * <p>
	 * 	The following shows what happens when serializing a subclassed object to JSON:
	 * <p class='bcode'>
	 * 	JsonSerializer s = <jk>new</jk> JsonSerializer().addFilters(AFilter.<jk>class</jk>);
	 * 	A1 a1 = <jk>new</jk> A1();
	 * 	a1.<jf>f1</jf> = <js>"f1"</js>;
	 * 	String r = s.serialize(a1);
	 * 	<jsm>assertEquals</jsm>(<js>"{subType:'A1',f1:'f1',f0:'f0'}"</js>, r);
	 * </p>
	 * <p>
	 * 	The following shows what happens when parsing back into the original object.
	 * <p class='bcode'>
	 * 	JsonParser p = <jk>new</jk> JsonParser().addFilters(AFilter.<jk>class</jk>);
	 * 	A a = p.parse(r, A.<jk>class</jk>);
	 * 	<jsm>assertTrue</jsm>(a <jk>instanceof</jk> A1);
	 * </p>
	 * <p>
	 * 	This method is an alternative to using the {@link Bean#subTypeProperty()} annotation on a class.
	 *
	 * @param subTypeAttr The name of the attribute representing the subtype.
	 * @return This object (for method chaining).
	 */
	public BeanFilter<T> setSubTypeProperty(String subTypeAttr) {
		this.subTypeAttr = subTypeAttr;
		return this;
	}

	/**
	 * Returns the subtypes associated with the bean class.
	 *
	 * @see #setSubTypeProperty(String)
	 * @return The set of sub types associated with this bean class, or <jk>null</jk> if bean has no subtypes defined.
	 */
	public LinkedHashMap<Class<?>, String> getSubTypes() {
		return subTypes;
	}

	/**
	 * Specifies the set of subclasses of this bean class in addition to a string identifier for that subclass.
	 *
	 * @see #setSubTypeProperty(String)
	 * @param subTypes the map of subtype classes to subtype identifier strings.
	 * @return This object (for method chaining).
	 */
	public BeanFilter<T> setSubTypes(LinkedHashMap<Class<?>, String> subTypes) {
		this.subTypes = subTypes;
		return this;
	}

	/**
	 * Convenience method for adding a single subtype in leu of using {@link #setSubTypes(LinkedHashMap)} in one call.
	 *
	 * @see #setSubTypeProperty(String)
	 * @param c The subtype class.
	 * @param id The subtype identifier string for the specified subtype class.
	 * @return This object (for method chaining).
	 */
	public BeanFilter<T> addSubType(Class<?> c, String id) {
		if (subTypes == null)
			subTypes = new LinkedHashMap<Class<?>, String>();
		subTypes.put(c, id);
		return this;
	}

	/**
	 * Returns the interface class associated with this class.
	 *
	 * @see #setInterfaceClass(Class)
	 * @return The interface class associated with this class, or <jk>null</jk> if no interface class is associated.
	 */
	public Class<?> getInterfaceClass() {
		return interfaceClass;
	}

	/**
	 * Identifies a class to be used as the interface class for this and all subclasses.
	 * <p>
	 * 	Functionally equivalent to using the {@link Bean#interfaceClass()} annotation.
	 * <p>
	 * 	When specified, only the list of properties defined on the interface class will be used during serialization.
	 * 	Additional properties on subclasses will be ignored.
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
	 * 	<jk>public class</jk> AFilter <jk>extends</jk> BeanFilter&lt;A&gt; {
	 * 		<jk>public</jk> AFilter() {
	 * 			setInterfaceClass(A.<jk>class</jk>);
	 * 		}
	 * 	}
	 *
	 * 	JsonSerializer s = new JsonSerializer().addFilters(AFilter.<jk>class</jk>);
	 * 	A1 a1 = <jk>new</jk> A1();
	 * 	String r = s.serialize(a1);
	 * 	<jsm>assertEquals</jsm>(<js>"{f0:'f0'}"</js>, r);  <jc>// Note f1 is not serialized</jc>
	 * </p>
	 * <p>
	 * 	Note that this filter can be used on the parent class so that it filters to all child classes,
	 * 		or can be set individually on the child classes.
	 * <p>
	 * 	This method is an alternative to using the {@link Bean#interfaceClass()}} annotation.
	 *
	 * @param interfaceClass The interface class.
	 * @return This object (for method chaining).
	 */
	public BeanFilter<T> setInterfaceClass(Class<?> interfaceClass) {
		this.interfaceClass = interfaceClass;
		return this;
	}

	/**
	 * Returns the stop class associated with this class.
	 *
	 * @see #setStopClass(Class)
	 * @return The stop class associated with this class, or <jk>null</jk> if no stop class is associated.
	 */
	public Class<?> getStopClass() {
		return stopClass;
	}

	/**
	 * Identifies a stop class for this class and all subclasses.
	 * <p>
	 * 	Functionally equivalent to using the {@link Bean#stopClass()} annotation.
	 * <p>
	 * 	Identical in purpose to the stop class specified by {@link Introspector#getBeanInfo(Class, Class)}.
	 * 	Any properties in the stop class or in its baseclasses will be ignored during analysis.
	 * <p>
	 * 	For example, in the following class hierarchy, instances of <code>C3</code> will include property <code>p3</code>, but
	 * 		not <code>p1</code> or <code>p2</code>.
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
	 * @param stopClass The stop class.
	 * @return This object (for method chaining).
	 */
	public BeanFilter<T> setStopClass(Class<?> stopClass) {
		this.stopClass = stopClass;
		return this;
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
