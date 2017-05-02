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

import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;

/**
 * Builder class for building instances of serializers and parsers.
 */
public abstract class CoreObjectBuilder {

	/** Contains all the modifiable settings for the implementation class. */
	protected final PropertyStore propertyStore;

	/**
	 * Constructor.
	 * Default settings.
	 */
	public CoreObjectBuilder() {
		this.propertyStore = PropertyStore.create();
	}

	/**
	 * Constructor.
	 * @param propertyStore The initial configuration settings for this builder.
	 */
	public CoreObjectBuilder(PropertyStore propertyStore) {
		this.propertyStore = PropertyStore.create(propertyStore);
	}

	/**
	 * Build the object.
	 *
	 * @return The built object.
	 * Subsequent calls to this method will create new instances.
	 */
	public abstract CoreObject build();

	/**
	 * Copies the settings from the specified property store into this builder.
	 *
	 * @param copyFrom The factory whose settings are being copied.
	 * @return This object (for method chaining).
	 */
	public CoreObjectBuilder apply(PropertyStore copyFrom) {
		this.propertyStore.copyFrom(propertyStore);
		return this;
	}

	/**
	 * Build a new instance of the specified object.
	 *
	 * @param c The subclass of {@link CoreObject} to instantiate.
	 * @return A new object using the settings defined in this builder.
	 */
	public <T extends CoreObject> T build(Class<T> c) {
		try {
			return c.getConstructor(PropertyStore.class).newInstance(propertyStore);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	/**
	 * Sets a configuration property on this object.
	 *
	 * @param name The property name.
	 * @param value The property value.
	 * @return This object (for method chaining).
	 * @see PropertyStore#setProperty(String, Object)
	 */
	public CoreObjectBuilder property(String name, Object value) {
		propertyStore.setProperty(name, value);
		return this;
	}

	/**
	 * Adds multiple configuration properties on this object.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling {@link PropertyStore#addProperties(Map)}.
	 * 	<li>Any previous properties are kept if they're not overwritten.
	 * </ul>
	 *
	 * @param properties The properties to set on this class.
	 * @return This object (for method chaining).
	 * @see PropertyStore#addProperties(java.util.Map)
	 */
	public CoreObjectBuilder properties(Map<String,Object> properties) {
		propertyStore.addProperties(properties);
		return this;
	}

	/**
	 * Sets multiple configuration properties on this object.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling {@link PropertyStore#setProperties(Map)}.
	 * 	<li>Any previous properties are discarded.
	 * </ul>
	 *
	 * @param properties The properties to set on this class.
	 * @return This object (for method chaining).
	 * @see PropertyStore#setProperties(java.util.Map)
	 */
	public CoreObjectBuilder setProperties(Map<String,Object> properties) {
		propertyStore.setProperties(properties);
		return this;
	}

	/**
	 * Adds a value to a SET property.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>PropertyStore.addToProperty(name, value);</code>.
	 * </ul>
	 *
	 * @param name The property name.
	 * @param value The new value to add to the SET property.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a SET property.
	 */
	public CoreObjectBuilder addToProperty(String name, Object value) {
		propertyStore.addToProperty(name, value);
		return this;
	}

	/**
	 * Adds or overwrites a value to a MAP property.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>PropertyStore.putToProperty(name, key, value);</code>.
	 * </ul>
	 *
	 * @param name The property name.
	 * @param key The property value map key.
	 * @param value The property value map value.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a MAP property.
	 */
	public CoreObjectBuilder putToProperty(String name, Object key, Object value) {
		propertyStore.putToProperty(name, key, value);
		return this;
	}

	/**
	 * Adds or overwrites a value to a MAP property.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>PropertyStore.putToProperty(name, value);</code>.
	 * </ul>
	 *
	 * @param name The property value.
	 * @param value The property value map value.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a MAP property.
	 */
	public CoreObjectBuilder putToProperty(String name, Object value) {
		propertyStore.putToProperty(name, value);
		return this;
	}

	/**
	 * Removes a value from a SET property.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>PropertyStore.removeFromProperty(name, value);</code>.
	 * </ul>
	 *
	 * @param name The property name.
	 * @param value The property value in the SET property.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a SET property.
	 */
	public CoreObjectBuilder removeFromProperty(String name, Object value) {
		propertyStore.removeFromProperty(name, value);
		return this;
	}

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
	 * Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 * <p>
	 * The {@link Bean @Bean} annotation can be used on a class to override this setting when <jk>true</jk>.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_beansRequireDefaultConstructor</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beansRequireDefaultConstructor
	 */
	public CoreObjectBuilder beansRequireDefaultConstructor(boolean value) {
		return property(BEAN_beansRequireDefaultConstructor, value);
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
	 * Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 * <p>
	 * The {@link Bean @Bean} annotation can be used on a class to override this setting when <jk>true</jk>.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_beansRequireSerializable</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beansRequireSerializable
	 */
	public CoreObjectBuilder beansRequireSerializable(boolean value) {
		return property(BEAN_beansRequireSerializable, value);
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
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_beansRequireSettersForGetters</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beansRequireSettersForGetters
	 */
	public CoreObjectBuilder beansRequireSettersForGetters(boolean value) {
		return property(BEAN_beansRequireSettersForGetters, value);
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
	 * Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 * <p>
	 * The {@link Bean @Bean} annotation can be used on a class to override this setting when <jk>true</jk>.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_beansRequireSomeProperties</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beansRequireSomeProperties
	 */
	public CoreObjectBuilder beansRequireSomeProperties(boolean value) {
		return property(BEAN_beansRequireSomeProperties, value);
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
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_beanMapPutReturnsOldValue</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanMapPutReturnsOldValue
	 */
	public CoreObjectBuilder beanMapPutReturnsOldValue(boolean value) {
		return property(BEAN_beanMapPutReturnsOldValue, value);
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
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_beanConstructorVisibility</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanConstructorVisibility
	 */
	public CoreObjectBuilder beanConstructorVisibility(Visibility value) {
		return property(BEAN_beanConstructorVisibility, value);
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
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_beanClassVisibility</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanClassVisibility
	 */
	public CoreObjectBuilder beanClassVisibility(Visibility value) {
		return property(BEAN_beanClassVisibility, value);
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
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_beanFieldVisibility</jsf>, value)</code>.
	 * 	<li>Use {@link Visibility#NONE} to prevent bean fields from being interpreted as bean properties altogether.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanFieldVisibility
	 */
	public CoreObjectBuilder beanFieldVisibility(Visibility value) {
		return property(BEAN_beanFieldVisibility, value);
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
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_methodVisibility</jsf>, value)</code>.
	 * 	<li>Use {@link Visibility#NONE} to prevent bean methods from being interpreted as bean properties altogether.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_methodVisibility
	 */
	public CoreObjectBuilder methodVisibility(Visibility value) {
		return property(BEAN_methodVisibility, value);
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
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_useJavaBeanIntrospector</jsf>, value)</code>.
	 * 	<li>Most {@link Bean @Bean} annotations will be ignored if you enable this setting.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_useJavaBeanIntrospector
	 */
	public CoreObjectBuilder useJavaBeanIntrospector(boolean value) {
		return property(BEAN_useJavaBeanIntrospector, value);
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
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_useInterfaceProxies</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_useInterfaceProxies
	 */
	public CoreObjectBuilder useInterfaceProxies(boolean value) {
		return property(BEAN_useInterfaceProxies, value);
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
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_ignoreUnknownBeanProperties</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_ignoreUnknownBeanProperties
	 */
	public CoreObjectBuilder ignoreUnknownBeanProperties(boolean value) {
		return property(BEAN_ignoreUnknownBeanProperties, value);
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
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_ignoreUnknownNullBeanProperties</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_ignoreUnknownNullBeanProperties
	 */
	public CoreObjectBuilder ignoreUnknownNullBeanProperties(boolean value) {
		return property(BEAN_ignoreUnknownNullBeanProperties, value);
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
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_ignorePropertiesWithoutSetters</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_ignorePropertiesWithoutSetters
	 */
	public CoreObjectBuilder ignorePropertiesWithoutSetters(boolean value) {
		return property(BEAN_ignorePropertiesWithoutSetters, value);
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
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_ignoreInvocationExceptionsOnGetters</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_ignoreInvocationExceptionsOnGetters
	 */
	public CoreObjectBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
		return property(BEAN_ignoreInvocationExceptionsOnGetters, value);
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
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_ignoreInvocationExceptionsOnSetters</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_ignoreInvocationExceptionsOnSetters
	 */
	public CoreObjectBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
		return property(BEAN_ignoreInvocationExceptionsOnSetters, value);
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
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_sortProperties</jsf>, value)</code>.
	 * 	<li>This property is disabled by default so that IBM JVM users don't have to use {@link Bean @Bean} annotations
	 * 		to force bean properties to be in a particular order and can just alter the order of the fields/methods
	 * 		in the Java file.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_sortProperties
	 */
	public CoreObjectBuilder sortProperties(boolean value) {
		return property(BEAN_sortProperties, value);
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
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_notBeanPackages</jsf>, values)</code>.
	 * </ul>
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages
	 */
	public CoreObjectBuilder setNotBeanPackages(String...values) {
		return property(BEAN_notBeanPackages, values);
	}

	/**
	 * <b>Configuration property:</b>  Packages whose classes should not be considered beans.
	 * <p>
	 * Same as {@link #setNotBeanPackages(String...)} but using a <code>Collection</code>.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages
	 */
	public CoreObjectBuilder setNotBeanPackages(Collection<String> values) {
		return property(BEAN_notBeanPackages, values);
	}

	/**
	 * <b>Configuration property:</b>  Add to packages whose classes should not be considered beans.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>addToProperty(<jsf>BEAN_notBeanPackages</jsf>, values)</code>
	 * 		or <code>property(<jsf>BEAN_notBeanPackages_add</jsf>, s)</code>.
	 * </ul>
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages_add
	 */
	public CoreObjectBuilder notBeanPackages(String...values) {
		return addToProperty(BEAN_notBeanPackages, values);
	}

	/**
	 * <b>Configuration property:</b>  Add to packages whose classes should not be considered beans.
	 * <p>
	 * Same as {@link #notBeanPackages(String...)} but using a <code>Collection</code>.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages
	 */
	public CoreObjectBuilder notBeanPackages(Collection<String> values) {
		return addToProperty(BEAN_notBeanPackages, values);
	}

	/**
	 * <b>Configuration property:</b>  Remove from packages whose classes should not be considered beans.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>removeFromProperty(<jsf>BEAN_notBeanPackages</jsf>, values)</code>
	 * 		or <code>property(<jsf>BEAN_notBeanPackages_remove</jsf>, s)</code>.
	 * </ul>
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages
	 * @see BeanContext#BEAN_notBeanPackages_remove
	 */
	public CoreObjectBuilder removeNotBeanPackages(String...values) {
		return removeFromProperty(BEAN_notBeanPackages, values);
	}

	/**
	 * <b>Configuration property:</b>  Remove from packages whose classes should not be considered beans.
	 * <p>
	 * Same as {@link #removeNotBeanPackages(String...)} but using a <code>Collection</code>.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages
	 * @see BeanContext#BEAN_notBeanPackages_remove
	 */
	public CoreObjectBuilder removeNotBeanPackages(Collection<String> values) {
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
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_notBeanClasses</jsf>, values)</code>.
	 * </ul>
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanClasses
	 */
	public CoreObjectBuilder setNotBeanClasses(Class<?>...values) {
		return property(BEAN_notBeanClasses, values);
	}

	/**
	 * <b>Configuration property:</b>  Classes to be excluded from consideration as being beans.
	 * <p>
	 * Same as {@link #setNotBeanClasses(Class...)} but using a <code>Collection</code>.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages
	 */
	public CoreObjectBuilder setNotBeanClasses(Collection<Class<?>> values) {
		return property(BEAN_notBeanClasses, values);
	}

	/**
	 * <b>Configuration property:</b>  Add to classes that should not be considered beans.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>addToProperty(<jsf>BEAN_notBeanClasses</jsf>, values)</code>
	 * 		or <code>property(<jsf>BEAN_notBeanClasses_add</jsf>, values)</code>.
	 * </ul>
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanClasses
	 * @see BeanContext#BEAN_notBeanClasses_add
	 */
	public CoreObjectBuilder notBeanClasses(Class<?>...values) {
		return addToProperty(BEAN_notBeanClasses, values);
	}

	/**
	 * <b>Configuration property:</b>  Add to classes that should not be considered beans.
	 * <p>
	 * Same as {@link #notBeanClasses(Class...)} but using a <code>Collection</code>.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanClasses
	 * @see BeanContext#BEAN_notBeanClasses_add
	 */
	public CoreObjectBuilder notBeanClasses(Collection<Class<?>> values) {
		return addToProperty(BEAN_notBeanClasses, values);
	}

	/**
	 * <b>Configuration property:</b>  Remove from classes that should not be considered beans.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>removeFromProperty(<jsf>BEAN_notBeanClasses</jsf>, values)</code>
	 * 		or <code>property(<jsf>BEAN_notBeanClasses_remove</jsf>, values)</code>.
	 * </ul>
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanClasses
	 * @see BeanContext#BEAN_notBeanClasses_remove
	 */
	public CoreObjectBuilder removeNotBeanClasses(Class<?>...values) {
		return removeFromProperty(BEAN_notBeanClasses, values);
	}

	/**
	 * <b>Configuration property:</b>  Remove from classes that should not be considered beans.
	 * <p>
	 * Same as {@link #removeNotBeanClasses(Class...)} but using a <code>Collection</code>.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanClasses
	 * @see BeanContext#BEAN_notBeanClasses_remove
	 */
	public CoreObjectBuilder removeNotBeanClasses(Collection<Class<?>> values) {
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
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_beanFilters</jsf>, values)</code>.
	 * </ul>
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanFilters
	 */
	public CoreObjectBuilder setBeanFilters(Class<?>...values) {
		return property(BEAN_beanFilters, values);
	}

	/**
	 * <b>Configuration property:</b>  Bean filters to apply to beans.
	 * <p>
	 * Same as {@link #setBeanFilters(Class...)} but using a <code>Collection</code>.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanFilters
	 */
	public CoreObjectBuilder setBeanFilters(Collection<Class<?>> values) {
		return property(BEAN_beanFilters, values);
	}

	/**
	 * <b>Configuration property:</b>  Add to bean filters.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>addToProperty(<jsf>BEAN_beanFilters</jsf>, values)</code>
	 * 		or <code>property(<jsf>BEAN_beanFilters_add</jsf>, values)</code>.
	 * </ul>
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanFilters
	 * @see BeanContext#BEAN_beanFilters_add
	 */
	public CoreObjectBuilder beanFilters(Class<?>...values) {
		return addToProperty(BEAN_beanFilters, values);
	}

	/**
	 * <b>Configuration property:</b>  Add to bean filters.
	 * <p>
	 * Same as {@link #beanFilters(Class...)} but using a <code>Collection</code>.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanFilters
	 * @see BeanContext#BEAN_beanFilters_add
	 */
	public CoreObjectBuilder beanFilters(Collection<Class<?>> values) {
		return addToProperty(BEAN_beanFilters, values);
	}

	/**
	 * <b>Configuration property:</b>  Remove from bean filters.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>removeFromProperty(<jsf>BEAN_beanFilters</jsf>, values)</code>
	 * 		or <code>property(<jsf>BEAN_beanFilters_remove</jsf>, values)</code>.
	 * </ul>
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanFilters
	 * @see BeanContext#BEAN_beanFilters_remove
	 */
	public CoreObjectBuilder removeBeanFilters(Class<?>...values) {
		return removeFromProperty(BEAN_beanFilters, values);
	}

	/**
	 * <b>Configuration property:</b>  Remove from bean filters.
	 * <p>
	 * Same as {@link #removeBeanFilters(Class...)} but using a <code>Collection</code>.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanFilters
	 * @see BeanContext#BEAN_beanFilters_remove
	 */
	public CoreObjectBuilder removeBeanFilters(Collection<Class<?>> values) {
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
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_pojoSwaps</jsf>, values)</code>.
	 * </ul>
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_pojoSwaps
	 */
	public CoreObjectBuilder setPojoSwaps(Class<?>...values) {
		return property(BEAN_pojoSwaps, values);
	}

	/**
	 * <b>Configuration property:</b>  POJO swaps to apply to Java objects.
	 * <p>
	 * Same as {@link #setPojoSwaps(Class...)} but using a <code>Collection</code>.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_pojoSwaps
	 */
	public CoreObjectBuilder setPojoSwaps(Collection<Class<?>> values) {
		return property(BEAN_pojoSwaps, values);
	}

	/**
	 * <b>Configuration property:</b>  Add to POJO swaps.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>addToProperty(<jsf>BEAN_pojoSwaps</jsf>, values)</code>
	 * 		or <code>property(<jsf>BEAN_pojoSwaps_add</jsf>, values)</code>.
	 * </ul>
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_pojoSwaps
	 * @see BeanContext#BEAN_pojoSwaps_add
	 */
	public CoreObjectBuilder pojoSwaps(Class<?>...values) {
		return addToProperty(BEAN_pojoSwaps, values);
	}

	/**
	 * <b>Configuration property:</b>  Add to POJO swaps.
	 * <p>
	 * Same as {@link #pojoSwaps(Class...)} but using a <code>Collection</code>.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_pojoSwaps
	 * @see BeanContext#BEAN_pojoSwaps_add
	 */
	public CoreObjectBuilder pojoSwaps(Collection<Class<?>> values) {
		return addToProperty(BEAN_pojoSwaps, values);
	}

	/**
	 * <b>Configuration property:</b>  Remove from POJO swaps.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>removeFromProperty(<jsf>BEAN_pojoSwaps</jsf>, values)</code>
	 * 		or <code>property(<jsf>BEAN_pojoSwaps_remove</jsf>, values)</code>.
	 * </ul>
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_pojoSwaps
	 * @see BeanContext#BEAN_pojoSwaps_remove
	 */
	public CoreObjectBuilder removePojoSwaps(Class<?>...values) {
		return removeFromProperty(BEAN_pojoSwaps, values);
	}

	/**
	 * <b>Configuration property:</b>  Remove from POJO swaps.
	 * <p>
	 * Same as {@link #removePojoSwaps(Class...)} but using a <code>Collection</code>.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_pojoSwaps
	 * @see BeanContext#BEAN_pojoSwaps_remove
	 */
	public CoreObjectBuilder removePojoSwaps(Collection<Class<?>> values) {
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
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_implClasses</jsf>, values)</code>.
	 * </ul>
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_implClasses
	 */
	public CoreObjectBuilder implClasses(Map<Class<?>,Class<?>> values) {
		return property(BEAN_implClasses, values);
	}

	/**
	 * <b>Configuration property:</b>  Implementation classes for interfaces and abstract classes.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>putToProperty(<jsf>BEAN_implClasses</jsf>, interfaceClass, implClass)</code>
	 * 		or <code>property(<jsf>BEAN_implClasses_put</jsf>, interfaceClass, implClass)</code>.
	 * </ul>
	 *
	 * @param interfaceClass The interface class.
	 * @param implClass The implementation class.
	 * @param <I> The class type of the interface.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_implClasses
	 * @see BeanContext#BEAN_implClasses_put
	 */
	public <I> CoreObjectBuilder implClass(Class<I> interfaceClass, Class<? extends I> implClass) {
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
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_beanDictionary</jsf>, values)</code>.
	 * </ul>
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanDictionary
	 */
	public CoreObjectBuilder setBeanDictionary(Class<?>...values) {
		return property(BEAN_beanDictionary, values);
	}

	/**
	 * <b>Configuration property:</b>  Bean lookup dictionary.
	 * <p>
	 * Same as {@link #setBeanDictionary(Class...)} but using a <code>Collection</code>.
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanDictionary
	 */
	public CoreObjectBuilder setBeanDictionary(Collection<Class<?>> values) {
		return property(BEAN_beanDictionary, values);
	}

	/**
	 * <b>Configuration property:</b>  Add to bean dictionary.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>addToProperty(<jsf>BEAN_beanDictionary</jsf>, values)</code>
	 * 		or <code>property(<jsf>BEAN_beanDictionary_add</jsf>, values)</code>.
	 * </ul>
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanDictionary
	 * @see BeanContext#BEAN_beanDictionary_add
	 */
	public CoreObjectBuilder beanDictionary(Class<?>...values) {
		return addToProperty(BEAN_beanDictionary, values);
	}

	/**
	 * <b>Configuration property:</b>  Add to bean dictionary.
	 * <p>
	 * Same as {@link #beanDictionary(Class...)} but using a <code>Collection</code>.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanDictionary
	 * @see BeanContext#BEAN_beanDictionary_add
	 */
	public CoreObjectBuilder beanDictionary(Collection<Class<?>> values) {
		return addToProperty(BEAN_beanDictionary, values);
	}

	/**
	 * <b>Configuration property:</b>  Remove from bean dictionary.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>removeFromProperty(<jsf>BEAN_beanDictionary</jsf>, values)</code>
	 * 		or <code>property(<jsf>BEAN_beanDictionary_remove</jsf>, values)</code>.
	 * </ul>
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanDictionary
	 * @see BeanContext#BEAN_beanDictionary_remove
	 */
	public CoreObjectBuilder removeFromBeanDictionary(Class<?>...values) {
		return removeFromProperty(BEAN_beanDictionary, values);
	}

	/**
	 * <b>Configuration property:</b>  Remove from bean dictionary.
	 * <p>
	 * Same as {@link #removeFromBeanDictionary(Class...)} but using a <code>Collection</code>.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanDictionary
	 * @see BeanContext#BEAN_beanDictionary_remove
	 */
	public CoreObjectBuilder removeFromBeanDictionary(Collection<Class<?>> values) {
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
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_beanTypePropertyName</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanTypePropertyName
	 */
	public CoreObjectBuilder beanTypePropertyName(String value) {
		return property(BEAN_beanTypePropertyName, value);
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
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_defaultParser</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_defaultParser
	 */
	public CoreObjectBuilder defaultParser(Class<?> value) {
		return property(BEAN_defaultParser, value);
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
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_locale</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_locale
	 */
	public CoreObjectBuilder locale(Locale value) {
		return property(BEAN_locale, value);
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
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_timeZone</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_timeZone
	 */
	public CoreObjectBuilder timeZone(TimeZone value) {
		return property(BEAN_timeZone, value);
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
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_mediaType</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_mediaType
	 */
	public CoreObjectBuilder mediaType(MediaType value) {
		return property(BEAN_mediaType, value);
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
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_debug</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_debug
	 */
	public CoreObjectBuilder debug(boolean value) {
		return property(BEAN_debug, value);
	}

	/**
	 * Sets the classloader used for created classes from class strings.
	 *
	 * @param classLoader The new classloader.
	 * @return This object (for method chaining).
	 * @see PropertyStore#setClassLoader(ClassLoader)
	 */
	public CoreObjectBuilder classLoader(ClassLoader classLoader) {
		propertyStore.setClassLoader(classLoader);
		return this;
	}
}