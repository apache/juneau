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

import static org.apache.juneau.BeanContext.*;

import java.beans.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.Visibility;
import org.apache.juneau.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;

/**
 * Common super class for all core-API serializers, parsers, and serializer/parser groups.
 *
 * <h5 class='section'>Description:</h5>
 * <p>
 * Maintains an inner {@link ContextFactory} instance that can be used by serializer and parser subclasses
 * 	to work with beans in a consistent way.
 * <p>
 * Provides several duplicate convenience methods from the {@link ContextFactory} class to set properties on that class from this class.
 * <p>
 * Also implements the {@link Lockable} interface to allow for easy locking and cloning.
 */
public abstract class CoreApi extends Lockable {

	private ContextFactory contextFactory = ContextFactory.create();
	private BeanContext beanContext;

	/**
	 * Returns the {@link ContextFactory} object associated with this class.
	 * <p>
	 * The context factory stores all configuration properties for this class.
	 * Adding/modifying properties on this factory will alter the behavior of this object.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>Calling the {@link ContextFactory#lock()} method on the returned object will prevent any further modifications to the configuration for this object
	 * 		ANY ANY OTHERS THAT SHARE THE SAME FACTORY!.
	 * 	<li>Calling the {@link #lock()} method on this class will only lock the configuration for this particular instance of the class.
	 * </ul>
	 *
	 * @return The context factory associated with this object.
	 */
	public ContextFactory getContextFactory() {
		return contextFactory;
	}

	/**
	 * Returns the bean context to use for this class.
	 *
	 * @return The bean context object.
	 */
	public BeanContext getBeanContext() {
		if (beanContext == null)
			return contextFactory.getContext(BeanContext.class);
		return beanContext;
	}

	/**
	 * Creates a {@link Context} class instance of the specified type.
	 * <p>
	 * For example, to create an <code>HtmlSerializerContext</code> object that contains a read-only snapshot
	 * of all the current settings in this object...
	 * <p class='bcode'>
	 * 	HtmlSerializerContext ctx = htmlParser.getContext(HtmlDocSerializerContext.<jk>class</jk>);
	 * </p>
	 *
	 * @param contextClass The class instance to create.
	 * @return A context class instance of the specified type.
	 */
	protected final <T extends Context> T getContext(Class<T> contextClass) {
		return contextFactory.getContext(contextClass);
	}

	/**
	 * Sets a configuration property on this object.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>getContextFactory().setProperty(name, value);</code>.
	 * </ul>
	 *
	 * @param name The property name.
	 * @param value The property value.
	 * @return This class (for method chaining).
	 * @throws LockedException If {@link #lock()} has been called on this object or {@link ContextFactory} object.
	 * @see ContextFactory#setProperty(String, Object)
	 */
	public CoreApi setProperty(String name, Object value) throws LockedException {
		checkLock();
		contextFactory.setProperty(name, value);
		return this;
	}

	/**
	 * Sets multiple configuration properties on this object.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>getContextFactory().setProperties(properties);</code>.
	 * </ul>
	 *
	 * @param properties The properties to set on this class.
	 * @return This class (for method chaining).
	 * @throws LockedException If {@link #lock()} has been called on this object.
	 * @see ContextFactory#setProperties(java.util.Map)
	 */
	public CoreApi setProperties(ObjectMap properties) throws LockedException {
		checkLock();
		contextFactory.setProperties(properties);
		return this;
	}

	/**
	 * Adds a value to a SET property.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>getContextFactory().addToProperty(name, value);</code>.
	 * </ul>
	 *
	 * @param name The property name.
	 * @param value The new value to add to the SET property.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a SET property.
	 * @throws LockedException If {@link #lock()} has been called on this object.
	 */
	public CoreApi addToProperty(String name, Object value) throws LockedException {
		checkLock();
		contextFactory.addToProperty(name, value);
		return this;
	}

	/**
	 * Adds or overwrites a value to a MAP property.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>getContextFactory().putToProperty(name, key, value);</code>.
	 * </ul>
	 *
	 * @param name The property name.
	 * @param key The property value map key.
	 * @param value The property value map value.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a MAP property.
	 * @throws LockedException If {@link #lock()} has been called on this object.
	 */
	public CoreApi putToProperty(String name, Object key, Object value) throws LockedException {
		checkLock();
		contextFactory.putToProperty(name, key, value);
		return this;
	}

	/**
	 * Adds or overwrites a value to a MAP property.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>getContextFactory().putToProperty(name, value);</code>.
	 * </ul>
	 *
	 * @param name The property value.
	 * @param value The property value map value.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a MAP property.
	 * @throws LockedException If {@link #lock()} has been called on this object.
	 */
	public CoreApi putToProperty(String name, Object value) throws LockedException {
		checkLock();
		contextFactory.putToProperty(name, value);
		return this;
	}

	/**
	 * Removes a value from a SET property.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>getContextFactory().removeFromProperty(name, value);</code>.
	 * </ul>
	 *
	 * @param name The property name.
	 * @param value The property value in the SET property.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a SET property.
	 * @throws LockedException If {@link #lock()} has been called on this object.
	 */
	public CoreApi removeFromProperty(String name, Object value) throws LockedException {
		checkLock();
		contextFactory.removeFromProperty(name, value);
		return this;
	}

	/**
	 * Returns the universal <code>Object</code> metadata object.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>getBeanContext().object();</code>.
	 * </ul>
	 *
	 * @return The reusable {@link ClassMeta} for representing the {@link Object} class.
	 */
	public ClassMeta<Object> object() {
		return getBeanContext().object();
	}

	/**
	 * Returns the universal <code>String</code> metadata object.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>getBeanContext().string();</code>.
	 * </ul>
	 *
	 * @return The reusable {@link ClassMeta} for representing the {@link String} class.
	 */
	public ClassMeta<String> string() {
		return getBeanContext().string();
	}


	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	/**
	 * <b>Configuration property:</b>  Beans require no-arg constructors.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.beansRequireDefaultConstructor"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, a Java class must implement a default no-arg constructor to be considered a bean.
	 * Otherwise, the bean will be serialized as a string using the {@link #toString()} method.
	 * <p>
	 * The {@link Bean @Bean} annotation can be used on a class to override this setting when <jk>true</jk>.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>BEAN_beansRequireDefaultConstructor</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beansRequireDefaultConstructor
	 */
	public CoreApi setBeansRequireDefaultConstructor(boolean value) throws LockedException {
		return setProperty(BEAN_beansRequireDefaultConstructor, value);
	}

	/**
	 * <b>Configuration property:</b>  Beans require {@link Serializable} interface.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.beansRequireSerializable"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, a Java class must implement the {@link Serializable} interface to be considered a bean.
	 * Otherwise, the bean will be serialized as a string using the {@link #toString()} method.
	 * <p>
	 * The {@link Bean @Bean} annotation can be used on a class to override this setting when <jk>true</jk>.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>BEAN_beansRequireSerializable</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beansRequireSerializable
	 */
	public CoreApi setBeansRequireSerializable(boolean value) throws LockedException {
		return setProperty(BEAN_beansRequireSerializable, value);
	}

	/**
	 * <b>Configuration property:</b>  Beans require setters for getters.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.beansRequireSettersForGetters"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, only getters that have equivalent setters will be considered as properties on a bean.
	 * Otherwise, they will be ignored.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>BEAN_beansRequireSettersForGetters</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beansRequireSettersForGetters
	 */
	public CoreApi setBeansRequireSettersForGetters(boolean value) throws LockedException {
		return setProperty(BEAN_beansRequireSettersForGetters, value);
	}

	/**
	 * <b>Configuration property:</b>  Beans require at least one property.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.beansRequireSomeProperties"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, then a Java class must contain at least 1 property to be considered a bean.
	 * Otherwise, the bean will be serialized as a string using the {@link #toString()} method.
	 * <p>
	 * The {@link Bean @Bean} annotation can be used on a class to override this setting when <jk>true</jk>.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>BEAN_beansRequireSomeProperties</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beansRequireSomeProperties
	 */
	public CoreApi setBeansRequireSomeProperties(boolean value) throws LockedException {
		return setProperty(BEAN_beansRequireSomeProperties, value);
	}

	/**
	 * <b>Configuration property:</b>  {@link BeanMap#put(String,Object) BeanMap.put()} method will return old property value.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.beanMapPutReturnsOldValue"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, then the {@link BeanMap#put(String,Object) BeanMap.put()} method will return old property values.
	 * Otherwise, it returns <jk>null</jk>.
	 * <p>
	 * Disabled by default because it introduces a slight performance penalty.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>BEAN_beanMapPutReturnsOldValue</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanMapPutReturnsOldValue
	 */
	public CoreApi setBeanMapPutReturnsOldValue(boolean value) throws LockedException {
		return setProperty(BEAN_beanMapPutReturnsOldValue, value);
	}

	/**
	 * <b>Configuration property:</b>  Look for bean constructors with the specified minimum visibility.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.beanConstructorVisibility"</js>
	 * 	<li><b>Data type:</b> {@link Visibility}
	 * 	<li><b>Default:</b> {@link Visibility#PUBLIC}
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * Constructors not meeting this minimum visibility will be ignored.
	 * For example, if the visibility is <code>PUBLIC</code> and the constructor is <jk>protected</jk>, then
	 * 	the constructor will be ignored.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>BEAN_beanConstructorVisibility</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanConstructorVisibility
	 */
	public CoreApi setBeanConstructorVisibility(Visibility value) throws LockedException {
		return setProperty(BEAN_beanConstructorVisibility, value);
	}

	/**
	 * <b>Configuration property:</b>  Look for bean classes with the specified minimum visibility.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.beanClassVisibility"</js>
	 * 	<li><b>Data type:</b> {@link Visibility}
	 * 	<li><b>Default:</b> {@link Visibility#PUBLIC}
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * Classes are not considered beans unless they meet the minimum visibility requirements.
	 * For example, if the visibility is <code>PUBLIC</code> and the bean class is <jk>protected</jk>, then
	 * 	the class will not be interpreted as a bean class.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>BEAN_beanClassVisibility</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanClassVisibility
	 */
	public CoreApi setBeanClassVisibility(Visibility value) throws LockedException {
		return setProperty(BEAN_beanClassVisibility, value);
	}

	/**
	 * <b>Configuration property:</b>  Look for bean fields with the specified minimum visibility.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.beanFieldVisibility"</js>
	 * 	<li><b>Data type:</b> {@link Visibility}
	 * 	<li><b>Default:</b> {@link Visibility#PUBLIC}
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * Fields are not considered bean properties unless they meet the minimum visibility requirements.
	 * For example, if the visibility is <code>PUBLIC</code> and the bean field is <jk>protected</jk>, then
	 * 	the field will not be interpreted as a bean property.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>BEAN_beanFieldVisibility</jsf>, value)</code>.
	 * 	<li>Use {@link Visibility#NONE} to prevent bean fields from being interpreted as bean properties altogether.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanFieldVisibility
	 */
	public CoreApi setBeanFieldVisibility(Visibility value) throws LockedException {
		return setProperty(BEAN_beanFieldVisibility, value);
	}

	/**
	 * <b>Configuration property:</b>  Look for bean methods with the specified minimum visibility.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.methodVisibility"</js>
	 * 	<li><b>Data type:</b> {@link Visibility}
	 * 	<li><b>Default:</b> {@link Visibility#PUBLIC}
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * Methods are not considered bean getters/setters unless they meet the minimum visibility requirements.
	 * For example, if the visibility is <code>PUBLIC</code> and the bean method is <jk>protected</jk>, then
	 * 	the method will not be interpreted as a bean getter or setter.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>BEAN_methodVisibility</jsf>, value)</code>.
	 * 	<li>Use {@link Visibility#NONE} to prevent bean methods from being interpreted as bean properties altogether.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_methodVisibility
	 */
	public CoreApi setMethodVisibility(Visibility value) throws LockedException {
		return setProperty(BEAN_methodVisibility, value);
	}

	/**
	 * <b>Configuration property:</b>  Use Java {@link Introspector} for determining bean properties.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.useJavaBeanIntrospector"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * Using the built-in Java bean introspector will not pick up fields or non-standard getters/setters.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>BEAN_useJavaBeanIntrospector</jsf>, value)</code>.
	 * 	<li>Most {@link Bean @Bean} annotations will be ignored if you enable this setting.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_useJavaBeanIntrospector
	 */
	public CoreApi setUseJavaBeanIntrospector(boolean value) throws LockedException {
		return setProperty(BEAN_useJavaBeanIntrospector, value);
	}

	/**
	 * <b>Configuration property:</b>  Use interface proxies.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.useInterfaceProxies"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, then interfaces will be instantiated as proxy classes through the use of an {@link InvocationHandler}
	 * if there is no other way of instantiating them.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>BEAN_useInterfaceProxies</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_useInterfaceProxies
	 */
	public CoreApi setUseInterfaceProxies(boolean value) throws LockedException {
		return setProperty(BEAN_useInterfaceProxies, value);
	}

	/**
	 * <b>Configuration property:</b>  Ignore unknown properties.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.ignoreUnknownBeanProperties"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, trying to set a value on a non-existent bean property will silently be ignored.
	 * Otherwise, a {@code BeanRuntimeException} is thrown.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>BEAN_ignoreUnknownBeanProperties</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_ignoreUnknownBeanProperties
	 */
	public CoreApi setIgnoreUnknownBeanProperties(boolean value) throws LockedException {
		return setProperty(BEAN_ignoreUnknownBeanProperties, value);
	}

	/**
	 * <b>Configuration property:</b>  Ignore unknown properties with null values.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.ignoreUnknownNullBeanProperties"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, trying to set a <jk>null</jk> value on a non-existent bean property will silently be ignored.
	 * Otherwise, a {@code BeanRuntimeException} is thrown.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>BEAN_ignoreUnknownNullBeanProperties</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_ignoreUnknownNullBeanProperties
	 */
	public CoreApi setIgnoreUnknownNullBeanProperties(boolean value) throws LockedException {
		return setProperty(BEAN_ignoreUnknownNullBeanProperties, value);
	}

	/**
	 * <b>Configuration property:</b>  Ignore properties without setters.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.ignorePropertiesWithoutSetters"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, trying to set a value on a bean property without a setter will silently be ignored.
	 * Otherwise, a {@code BeanRuntimeException} is thrown.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>BEAN_ignorePropertiesWithoutSetters</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_ignorePropertiesWithoutSetters
	 */
	public CoreApi setIgnorePropertiesWithoutSetters(boolean value) throws LockedException {
		return setProperty(BEAN_ignorePropertiesWithoutSetters, value);
	}

	/**
	 * <b>Configuration property:</b>  Ignore invocation errors on getters.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.ignoreInvocationExceptionsOnGetters"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, errors thrown when calling bean getter methods will silently be ignored.
	 * Otherwise, a {@code BeanRuntimeException} is thrown.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>BEAN_ignoreInvocationExceptionsOnGetters</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_ignoreInvocationExceptionsOnGetters
	 */
	public CoreApi setIgnoreInvocationExceptionsOnGetters(boolean value) throws LockedException {
		return setProperty(BEAN_ignoreInvocationExceptionsOnGetters, value);
	}

	/**
	 * <b>Configuration property:</b>  Ignore invocation errors on setters.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.ignoreInvocationExceptionsOnSetters"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, errors thrown when calling bean setter methods will silently be ignored.
	 * Otherwise, a {@code BeanRuntimeException} is thrown.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>BEAN_ignoreInvocationExceptionsOnSetters</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_ignoreInvocationExceptionsOnSetters
	 */
	public CoreApi setIgnoreInvocationExceptionsOnSetters(boolean value) throws LockedException {
		return setProperty(BEAN_ignoreInvocationExceptionsOnSetters, value);
	}

	/**
	 * <b>Configuration property:</b>  Sort bean properties in alphabetical order.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.sortProperties"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * When <jk>true</jk>, all bean properties will be serialized and access in alphabetical order.
	 * Otherwise, the natural order of the bean properties is used which is dependent on the
	 * 	JVM vendor.
	 * On IBM JVMs, the bean properties are ordered based on their ordering in the Java file.
	 * On Oracle JVMs, the bean properties are not ordered (which follows the offical JVM specs).
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>BEAN_sortProperties</jsf>, value)</code>.
	 * 	<li>This property is disabled by default so that IBM JVM users don't have to use {@link Bean @Bean} annotations
	 * 		to force bean properties to be in a particular order and can just alter the order of the fields/methods
	 * 		in the Java file.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_sortProperties
	 */
	public CoreApi setSortProperties(boolean value) throws LockedException {
		return setProperty(BEAN_sortProperties, value);
	}

	/**
	 * <b>Configuration property:</b>  Packages whose classes should not be considered beans.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.notBeanPackages.set"</js>
	 * 	<li><b>Data type:</b> <code>Set&lt;String&gt;</code>
	 * 	<li><b>Default:</b>
	 * 	<ul>
	 * 		<li><code>java.lang</code>
	 * 		<li><code>java.lang.annotation</code>
	 * 		<li><code>java.lang.ref</code>
	 * 		<li><code>java.lang.reflect</code>
	 * 		<li><code>java.io</code>
	 * 		<li><code>java.net</code>
	 * 		<li><code>java.nio.*</code>
	 * 		<li><code>java.util.*</code>
	 * 	</ul>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * When specified, the current list of ignore packages are appended to.
	 * <p>
	 * Any classes within these packages will be serialized to strings using {@link Object#toString()}.
	 * <p>
	 * Note that you can specify prefix patterns to include all subpackages.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>BEAN_notBeanPackages</jsf>, values)</code>.
	 * </ul>
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanPackages
	 */
	public CoreApi setNotBeanPackages(String...values) throws LockedException {
		return setProperty(BEAN_notBeanPackages, values);
	}

	/**
	 * <b>Configuration property:</b>  Packages whose classes should not be considered beans.
	 * <p>
	 * Same as {@link #setNotBeanPackages(String...)} but using a <code>Collection</code>.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanPackages
	 */
	public CoreApi setNotBeanPackages(Collection<String> values) throws LockedException {
		return setProperty(BEAN_notBeanPackages, values);
	}

	/**
	 * <b>Configuration property:</b>  Add to packages whose classes should not be considered beans.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>addToProperty(<jsf>BEAN_notBeanPackages</jsf>, values)</code>
	 * 		or <code>setProperty(<jsf>BEAN_notBeanPackages_add</jsf>, s)</code>.
	 * </ul>
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanPackages_add
	 */
	public CoreApi addNotBeanPackages(String...values) throws LockedException {
		return addToProperty(BEAN_notBeanPackages, values);
	}

	/**
	 * <b>Configuration property:</b>  Add to packages whose classes should not be considered beans.
	 * <p>
	 * Same as {@link #addNotBeanPackages(String...)} but using a <code>Collection</code>.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanPackages
	 */
	public CoreApi addNotBeanPackages(Collection<String> values) throws LockedException {
		return addToProperty(BEAN_notBeanPackages, values);
	}

	/**
	 * <b>Configuration property:</b>  Remove from packages whose classes should not be considered beans.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>removeFromProperty(<jsf>BEAN_notBeanPackages</jsf>, values)</code>
	 * 		or <code>setProperty(<jsf>BEAN_notBeanPackages_remove</jsf>, s)</code>.
	 * </ul>
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanPackages
	 * @see BeanContext#BEAN_notBeanPackages_remove
	 */
	public CoreApi removeNotBeanPackages(String...values) throws LockedException {
		return removeFromProperty(BEAN_notBeanPackages, values);
	}

	/**
	 * <b>Configuration property:</b>  Remove from packages whose classes should not be considered beans.
	 * <p>
	 * Same as {@link #removeNotBeanPackages(String...)} but using a <code>Collection</code>.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanPackages
	 * @see BeanContext#BEAN_notBeanPackages_remove
	 */
	public CoreApi removeNotBeanPackages(Collection<String> values) throws LockedException {
		return removeFromProperty(BEAN_notBeanPackages, values);
	}

	/**
	 * <b>Configuration property:</b>  Classes to be excluded from consideration as being beans.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.notBeanClasses.set"</js>
	 * 	<li><b>Data type:</b> <code>Set&lt;Class&gt;</code>
	 * 	<li><b>Default:</b> empty set
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * Not-bean classes are typically converted to <code>Strings</code> during serialization even if they
	 * appear to be bean-like.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>BEAN_notBeanClasses</jsf>, values)</code>.
	 * </ul>
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanClasses
	 */
	public CoreApi setNotBeanClasses(Class<?>...values) throws LockedException {
		return setProperty(BEAN_notBeanClasses, values);
	}

	/**
	 * <b>Configuration property:</b>  Classes to be excluded from consideration as being beans.
	 * <p>
	 * Same as {@link #setNotBeanClasses(Class...)} but using a <code>Collection</code>.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanPackages
	 */
	public CoreApi setNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		return setProperty(BEAN_notBeanClasses, values);
	}

	/**
	 * <b>Configuration property:</b>  Add to classes that should not be considered beans.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>addToProperty(<jsf>BEAN_notBeanClasses</jsf>, values)</code>
	 * 		or <code>setProperty(<jsf>BEAN_notBeanClasses_add</jsf>, c)</code>.
	 * </ul>
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanClasses
	 * @see BeanContext#BEAN_notBeanClasses_add
	 */
	public CoreApi addNotBeanClasses(Class<?>...values) throws LockedException {
		return addToProperty(BEAN_notBeanClasses, values);
	}

	/**
	 * <b>Configuration property:</b>  Add to classes that should not be considered beans.
	 * <p>
	 * Same as {@link #addNotBeanClasses(Class...)} but using a <code>Collection</code>.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanClasses
	 * @see BeanContext#BEAN_notBeanClasses_add
	 */
	public CoreApi addNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		return addToProperty(BEAN_notBeanClasses, values);
	}

	/**
	 * <b>Configuration property:</b>  Remove from classes that should not be considered beans.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>removeFromProperty(<jsf>BEAN_notBeanClasses</jsf>, values)</code>
	 * 		or <code>setProperty(<jsf>BEAN_notBeanClasses_remove</jsf>, c)</code>.
	 * </ul>
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanClasses
	 * @see BeanContext#BEAN_notBeanClasses_remove
	 */
	public CoreApi removeNotBeanClasses(Class<?>...values) throws LockedException {
		return removeFromProperty(BEAN_notBeanClasses, values);
	}

	/**
	 * <b>Configuration property:</b>  Remove from classes that should not be considered beans.
	 * <p>
	 * Same as {@link #removeNotBeanClasses(Class...)} but using a <code>Collection</code>.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_notBeanClasses
	 * @see BeanContext#BEAN_notBeanClasses_remove
	 */
	public CoreApi removeNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		return removeFromProperty(BEAN_notBeanClasses, values);
	}

	/**
	 * <b>Configuration property:</b>  Bean filters to apply to beans.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.beanFilters.list"</js>
	 * 	<li><b>Data type:</b> <code>List&lt;Class&gt;</code>
	 * 	<li><b>Default:</b> empty list
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * This is a programmatic equivalent to the {@link Bean @Bean} annotation.
	 * It's useful when you want to use the Bean annotation functionality, but you don't have the ability
	 * 	to alter the bean classes.
	 * <p>
	 * There are two category of classes that can be passed in through this method:
	 * <ul class='spaced-list'>
	 * 	<li>Subclasses of {@link BeanFilterBuilder}.
	 * 		These must have a public no-arg constructor.
	 * 	<li>Bean interface classes.
	 * 		A shortcut for defining a {@link InterfaceBeanFilterBuilder}.
	 * 		Any subclasses of an interface class will only have properties defined on the interface.
	 * 		All other bean properties will be ignored.
	 * </ul>
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>BEAN_beanFilters</jsf>, values)</code>.
	 * </ul>
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanFilters
	 */
	public CoreApi setBeanFilters(Class<?>...values) throws LockedException {
		return setProperty(BEAN_beanFilters, values);
	}

	/**
	 * <b>Configuration property:</b>  Bean filters to apply to beans.
	 * <p>
	 * Same as {@link #setBeanFilters(Class...)} but using a <code>Collection</code>.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanFilters
	 */
	public CoreApi setBeanFilters(Collection<Class<?>> values) throws LockedException {
		return setProperty(BEAN_beanFilters, values);
	}

	/**
	 * <b>Configuration property:</b>  Add to bean filters.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>addToProperty(<jsf>BEAN_beanFilters</jsf>, values)</code>
	 * 		or <code>setProperty(<jsf>BEAN_beanFilters_add</jsf>, c)</code>.
	 * </ul>
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanFilters
	 * @see BeanContext#BEAN_beanFilters_add
	 */
	public CoreApi addBeanFilters(Class<?>...values) throws LockedException {
		return addToProperty(BEAN_beanFilters, values);
	}

	/**
	 * <b>Configuration property:</b>  Add to bean filters.
	 * <p>
	 * Same as {@link #addBeanFilters(Class...)} but using a <code>Collection</code>.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanFilters
	 * @see BeanContext#BEAN_beanFilters_add
	 */
	public CoreApi addBeanFilters(Collection<Class<?>> values) throws LockedException {
		return addToProperty(BEAN_beanFilters, values);
	}

	/**
	 * <b>Configuration property:</b>  Remove from bean filters.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>removeFromProperty(<jsf>BEAN_beanFilters</jsf>, values)</code>
	 * 		or <code>setProperty(<jsf>BEAN_beanFilters_remove</jsf>, c)</code>.
	 * </ul>
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanFilters
	 * @see BeanContext#BEAN_beanFilters_remove
	 */
	public CoreApi removeBeanFilters(Class<?>...values) throws LockedException {
		return removeFromProperty(BEAN_beanFilters, values);
	}

	/**
	 * <b>Configuration property:</b>  Remove from bean filters.
	 * <p>
	 * Same as {@link #removeBeanFilters(Class...)} but using a <code>Collection</code>.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanFilters
	 * @see BeanContext#BEAN_beanFilters_remove
	 */
	public CoreApi removeBeanFilters(Collection<Class<?>> values) throws LockedException {
		return removeFromProperty(BEAN_beanFilters, values);
	}

	/**
	 * <b>Configuration property:</b>  POJO swaps to apply to Java objects.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.pojoSwaps.list"</js>
	 * 	<li><b>Data type:</b> <code>List&lt;Class&gt;</code>
	 * 	<li><b>Default:</b> empty list
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * There are two category of classes that can be passed in through this method:
	 * <ul>
	 * 	<li>Subclasses of {@link PojoSwap}.
	 * 	<li>Surrogate classes.  A shortcut for defining a {@link SurrogateSwap}.
	 * </ul>
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>BEAN_pojoSwaps</jsf>, values)</code>.
	 * </ul>
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_pojoSwaps
	 */
	public CoreApi setPojoSwaps(Class<?>...values) throws LockedException {
		return setProperty(BEAN_pojoSwaps, values);
	}

	/**
	 * <b>Configuration property:</b>  POJO swaps to apply to Java objects.
	 * <p>
	 * Same as {@link #setPojoSwaps(Class...)} but using a <code>Collection</code>.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_pojoSwaps
	 */
	public CoreApi setPojoSwaps(Collection<Class<?>> values) throws LockedException {
		return setProperty(BEAN_pojoSwaps, values);
	}

	/**
	 * <b>Configuration property:</b>  Add to POJO swaps.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>addToProperty(<jsf>BEAN_pojoSwaps</jsf>, values)</code>
	 * 		or <code>setProperty(<jsf>BEAN_pojoSwaps_add</jsf>, c)</code>.
	 * </ul>
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_pojoSwaps
	 * @see BeanContext#BEAN_pojoSwaps_add
	 */
	public CoreApi addPojoSwaps(Class<?>...values) throws LockedException {
		return addToProperty(BEAN_pojoSwaps, values);
	}

	/**
	 * <b>Configuration property:</b>  Add to POJO swaps.
	 * <p>
	 * Same as {@link #addPojoSwaps(Class...)} but using a <code>Collection</code>.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_pojoSwaps
	 * @see BeanContext#BEAN_pojoSwaps_add
	 */
	public CoreApi addPojoSwaps(Collection<Class<?>> values) throws LockedException {
		return addToProperty(BEAN_pojoSwaps, values);
	}

	/**
	 * <b>Configuration property:</b>  Remove from POJO swaps.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>removeFromProperty(<jsf>BEAN_pojoSwaps</jsf>, values)</code>
	 * 		or <code>setProperty(<jsf>BEAN_pojoSwaps_remove</jsf>, c)</code>.
	 * </ul>
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_pojoSwaps
	 * @see BeanContext#BEAN_pojoSwaps_remove
	 */
	public CoreApi removePojoSwaps(Class<?>...values) throws LockedException {
		return removeFromProperty(BEAN_pojoSwaps, values);
	}

	/**
	 * <b>Configuration property:</b>  Remove from POJO swaps.
	 * <p>
	 * Same as {@link #removePojoSwaps(Class...)} but using a <code>Collection</code>.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_pojoSwaps
	 * @see BeanContext#BEAN_pojoSwaps_remove
	 */
	public CoreApi removePojoSwaps(Collection<Class<?>> values) throws LockedException {
		return removeFromProperty(BEAN_pojoSwaps, values);
	}

	/**
	 * <b>Configuration property:</b>  Implementation classes for interfaces and abstract classes.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.implClasses.map"</js>
	 * 	<li><b>Data type:</b> <code>Map&lt;Class,Class&gt;</code>
	 * 	<li><b>Default:</b> empty map
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * For interfaces and abstract classes this method can be used to specify an implementation
	 * 	class for the interface/abstract class so that instances of the implementation
	 * 	class are used when instantiated (e.g. during a parse).
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>BEAN_implClasses</jsf>, values)</code>.
	 * </ul>
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_implClasses
	 */
	public CoreApi setImplClasses(Map<Class<?>,Class<?>> values) throws LockedException {
		return setProperty(BEAN_implClasses, values);
	}

	/**
	 * <b>Configuration property:</b>  Implementation classes for interfaces and abstract classes.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>putToProperty(<jsf>BEAN_implClasses</jsf>, interfaceClass, implClass)</code>
	 * 		or <code>setProperty(<jsf>BEAN_implClasses_put</jsf>, interfaceClass, implClass)</code>.
	 * </ul>
	 *
	 * @param interfaceClass The interface class.
	 * @param implClass The implementation class.
	 * @param <T> The class type of the interface.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_implClasses
	 * @see BeanContext#BEAN_implClasses_put
	 */
	public <T> CoreApi addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		return putToProperty(BEAN_implClasses, interfaceClass, implClass);
	}

	/**
	 * <b>Configuration property:</b>  Bean lookup dictionary.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.beanDictionary.list"</js>
	 * 	<li><b>Data type:</b> <code>List&lt;Class&gt;</code>
	 * 	<li><b>Default:</b> empty list
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * This list can consist of the following class types:
	 * <ul>
	 * 	<li>Any bean class that specifies a value for {@link Bean#typeName() @Bean.typeName()}.
	 * 	<li>Any subclass of {@link BeanDictionaryList} containing a collection of bean classes with type name annotations.
	 * 	<li>Any subclass of {@link BeanDictionaryMap} containing a mapping of type names to classes without type name annotations.
	 * </ul>
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>BEAN_beanDictionary</jsf>, values)</code>.
	 * </ul>
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanDictionary
	 */
	public CoreApi setBeanDictionary(Class<?>...values) throws LockedException {
		return setProperty(BEAN_beanDictionary, values);
	}

	/**
	 * <b>Configuration property:</b>  Bean lookup dictionary.
	 * <p>
	 * Same as {@link #setBeanDictionary(Class...)} but using a <code>Collection</code>.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanDictionary
	 */
	public CoreApi setBeanDictionary(Collection<Class<?>> values) throws LockedException {
		return setProperty(BEAN_beanDictionary, values);
	}

	/**
	 * <b>Configuration property:</b>  Add to bean dictionary.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>addToProperty(<jsf>BEAN_beanDictionary</jsf>, values)</code>
	 * 		or <code>setProperty(<jsf>BEAN_beanDictionary_add</jsf>, c)</code>.
	 * </ul>
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanDictionary
	 * @see BeanContext#BEAN_beanDictionary_add
	 */
	public CoreApi addToBeanDictionary(Class<?>...values) throws LockedException {
		return addToProperty(BEAN_beanDictionary, values);
	}

	/**
	 * <b>Configuration property:</b>  Add to bean dictionary.
	 * <p>
	 * Same as {@link #addToBeanDictionary(Class...)} but using a <code>Collection</code>.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanDictionary
	 * @see BeanContext#BEAN_beanDictionary_add
	 */
	public CoreApi addToBeanDictionary(Collection<Class<?>> values) throws LockedException {
		return addToProperty(BEAN_beanDictionary, values);
	}

	/**
	 * <b>Configuration property:</b>  Remove from bean dictionary.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>removeFromProperty(<jsf>BEAN_beanDictionary</jsf>, values)</code>
	 * 		or <code>setProperty(<jsf>BEAN_beanDictionary_remove</jsf>, c)</code>.
	 * </ul>
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanDictionary
	 * @see BeanContext#BEAN_beanDictionary_remove
	 */
	public CoreApi removeFromBeanDictionary(Class<?>...values) throws LockedException {
		return removeFromProperty(BEAN_beanDictionary, values);
	}

	/**
	 * <b>Configuration property:</b>  Remove from bean dictionary.
	 * <p>
	 * Same as {@link #removeFromBeanDictionary(Class...)} but using a <code>Collection</code>.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanDictionary
	 * @see BeanContext#BEAN_beanDictionary_remove
	 */
	public CoreApi removeFromBeanDictionary(Collection<Class<?>> values) throws LockedException {
		return removeFromProperty(BEAN_beanDictionary, values);
	}

	/**
	 * <b>Configuration property:</b>  Name to use for the bean type properties used to represent a bean type.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.beanTypePropertyName"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>"_type"</js>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>BEAN_beanTypePropertyName</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_beanTypePropertyName
	 */
	public CoreApi setBeanTypePropertyName(String value) throws LockedException {
		return addToProperty(BEAN_beanTypePropertyName, value);
	}

	/**
	 * <b>Configuration property:</b>  Default parser to use when converting <code>Strings</code> to POJOs.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.defaultParser"</js>
	 * 	<li><b>Data type:</b> <code>Class</code>
	 * 	<li><b>Default:</b> {@link JsonSerializer}
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * Used in the in the {@link BeanSession#convertToType(Object, Class)} method.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>BEAN_defaultParser</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_defaultParser
	 */
	public CoreApi setDefaultParser(Class<?> value) throws LockedException {
		return addToProperty(BEAN_defaultParser, value);
	}

	/**
	 * <b>Configuration property:</b>  Locale.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.locale"</js>
	 * 	<li><b>Data type:</b> <code>Locale</code>
	 * 	<li><b>Default:</b> <code>Locale.getDefault()</code>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>BEAN_locale</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_locale
	 */
	public CoreApi setLocale(Locale value) throws LockedException {
		return addToProperty(BEAN_locale, value);
	}

	/**
	 * <b>Configuration property:</b>  TimeZone.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.timeZone"</js>
	 * 	<li><b>Data type:</b> <code>TimeZone</code>
	 * 	<li><b>Default:</b> <jk>null</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>BEAN_timeZone</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_timeZone
	 */
	public CoreApi setTimeZone(TimeZone value) throws LockedException {
		return setProperty(BEAN_timeZone, value);
	}

	/**
	 * <b>Configuration property:</b>  Media type.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.mediaType"</js>
	 * 	<li><b>Data type:</b> <code>MediaType</code>
	 * 	<li><b>Default:</b> <jk>null</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Specifies a default media type value for serializer and parser sessions.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>BEAN_mediaType</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_mediaType
	 */
	public CoreApi setMediaType(MediaType value) throws LockedException {
		return addToProperty(BEAN_mediaType, value);
	}

	/**
	 * <b>Configuration property:</b>  Debug mode.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"BeanContext.debug"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Enables the following additional information during serialization:
	 * <ul class='spaced-list'>
	 * 	<li>When bean getters throws exceptions, the exception includes the object stack information
	 * 		in order to determine how that method was invoked.
	 * 	<li>Enables {@link SerializerContext#SERIALIZER_detectRecursions}.
	 * </ul>
	 * <p>
	 * Enables the following additional information during parsing:
	 * <ul class='spaced-list'>
	 * 	<li>When bean setters throws exceptions, the exception includes the object stack information
	 * 		in order to determine how that method was invoked.
	 * </ul>
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>BEAN_debug</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see BeanContext#BEAN_debug
	 */
	public CoreApi setDebug(boolean value) throws LockedException {
		return addToProperty(BEAN_debug, value);
	}

	/**
	 * Sets the classloader used for created classes from class strings.
	 *
	 * @param classLoader The new classloader.
	 * @throws LockedException If {@link ContextFactory#lock()} was called on this class or the bean context.
	 * @return This object (for method chaining).
	 * @see ContextFactory#setClassLoader(ClassLoader)
	 */
	public CoreApi setClassLoader(ClassLoader classLoader) throws LockedException {
		checkLock();
		contextFactory.setClassLoader(classLoader);
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Lockable */
	protected void checkLock() {
		super.checkLock();
		beanContext = null;
	}

	@Override /* Lockable */
	public CoreApi lock() {
		try {
			super.lock();
			contextFactory = contextFactory.clone();
			contextFactory.lock();
			beanContext = contextFactory.getContext(BeanContext.class);
			return this;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override /* Lockable */
	public CoreApi clone() throws CloneNotSupportedException {
		CoreApi c = (CoreApi)super.clone();
		c.contextFactory = ContextFactory.create(contextFactory);
		c.beanContext = null;
		return c;
	}
}
