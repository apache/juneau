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
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;

/**
 * Builder class for building instances of serializers, parsers, and bean contexts.
 */
public class BeanContextBuilder extends ContextBuilder {

	/**
	 * Constructor.
	 * 
	 * All default settings.
	 */
	public BeanContextBuilder() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param ps The initial configuration settings for this builder.
	 */
	public BeanContextBuilder(PropertyStore ps) {
		super(ps);
	}

	@Override /* ContextBuilder */
	public BeanContext build() {
		return build(BeanContext.class);
	}

	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	@Override /* ContextBuilder */
	public BeanContextBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public BeanContextBuilder set(boolean append, String name, Object value) {
		super.set(append, name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public BeanContextBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* ContextBuilder */
	public BeanContextBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* ContextBuilder */
	public BeanContextBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public BeanContextBuilder addTo(String name, String key, Object value) {
		super.addTo(name, key, value);
		return this;
	}

	@Override /* ContextBuilder */
	public BeanContextBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	/**
	 * <b>Configuration property:</b>  Beans require no-arg constructors.
	 *
	 * <p>
	 * If <jk>true</jk>, a Java class must implement a default no-arg constructor to be considered a bean.
	 * Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 *
	 * <p>
	 * The {@link Bean @Bean} annotation can be used on a class to override this setting when <jk>true</jk>.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_beansRequireDefaultConstructor</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beansRequireDefaultConstructor
	 */
	public BeanContextBuilder beansRequireDefaultConstructor(boolean value) {
		return set(BEAN_beansRequireDefaultConstructor, value);
	}

	/**
	 * <b>Configuration property:</b>  Beans require {@link Serializable} interface.
	 *
	 * <p>
	 * If <jk>true</jk>, a Java class must implement the {@link Serializable} interface to be considered a bean.
	 * Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 *
	 * <p>
	 * The {@link Bean @Bean} annotation can be used on a class to override this setting when <jk>true</jk>.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_beansRequireSerializable</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beansRequireSerializable
	 */
	public BeanContextBuilder beansRequireSerializable(boolean value) {
		return set(BEAN_beansRequireSerializable, value);
	}

	/**
	 * <b>Configuration property:</b>  Beans require setters for getters.
	 *
	 * <p>
	 * If <jk>true</jk>, only getters that have equivalent setters will be considered as properties on a bean.
	 * Otherwise, they will be ignored.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_beansRequireSettersForGetters</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beansRequireSettersForGetters
	 */
	public BeanContextBuilder beansRequireSettersForGetters(boolean value) {
		return set(BEAN_beansRequireSettersForGetters, value);
	}

	/**
	 * <b>Configuration property:</b>  Beans require at least one property.
	 *
	 * <p>
	 * If <jk>true</jk>, then a Java class must contain at least 1 property to be considered a bean.
	 * Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 *
	 * <p>
	 * The {@link Bean @Bean} annotation can be used on a class to override this setting when <jk>true</jk>.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_beansRequireSomeProperties</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beansRequireSomeProperties
	 */
	public BeanContextBuilder beansRequireSomeProperties(boolean value) {
		return set(BEAN_beansRequireSomeProperties, value);
	}

	/**
	 * <b>Configuration property:</b>  {@link BeanMap#put(String,Object) BeanMap.put()} method will return old property
	 * value.
	 *
	 * <p>
	 * If <jk>true</jk>, then the {@link BeanMap#put(String,Object) BeanMap.put()} method will return old property
	 * values.
	 * Otherwise, it returns <jk>null</jk>.
	 *
	 * <p>
	 * Disabled by default because it introduces a slight performance penalty.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_beanMapPutReturnsOldValue</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanMapPutReturnsOldValue
	 */
	public BeanContextBuilder beanMapPutReturnsOldValue(boolean value) {
		return set(BEAN_beanMapPutReturnsOldValue, value);
	}

	/**
	 * <b>Configuration property:</b>  Look for bean constructors with the specified minimum visibility.
	 *
	 * <p>
	 * Constructors not meeting this minimum visibility will be ignored.
	 * For example, if the visibility is <code>PUBLIC</code> and the constructor is <jk>protected</jk>, then the
	 * constructor will be ignored.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_beanConstructorVisibility</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanConstructorVisibility
	 */
	public BeanContextBuilder beanConstructorVisibility(Visibility value) {
		return set(BEAN_beanConstructorVisibility, value);
	}

	/**
	 * <b>Configuration property:</b>  Look for bean classes with the specified minimum visibility.
	 *
	 * <p>
	 * Classes are not considered beans unless they meet the minimum visibility requirements.
	 * For example, if the visibility is <code>PUBLIC</code> and the bean class is <jk>protected</jk>, then the class
	 * will not be interpreted as a bean class.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_beanClassVisibility</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanClassVisibility
	 */
	public BeanContextBuilder beanClassVisibility(Visibility value) {
		return set(BEAN_beanClassVisibility, value);
	}

	/**
	 * <b>Configuration property:</b>  Look for bean fields with the specified minimum visibility.
	 *
	 * <p>
	 * Fields are not considered bean properties unless they meet the minimum visibility requirements.
	 * For example, if the visibility is <code>PUBLIC</code> and the bean field is <jk>protected</jk>, then the field
	 * will not be interpreted as a bean property.
	 *
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
	public BeanContextBuilder beanFieldVisibility(Visibility value) {
		return set(BEAN_beanFieldVisibility, value);
	}

	/**
	 * <b>Configuration property:</b>  Look for bean methods with the specified minimum visibility.
	 *
	 * <p>
	 * Methods are not considered bean getters/setters unless they meet the minimum visibility requirements.
	 * For example, if the visibility is <code>PUBLIC</code> and the bean method is <jk>protected</jk>, then the method
	 * will not be interpreted as a bean getter or setter.
	 *
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
	public BeanContextBuilder methodVisibility(Visibility value) {
		return set(BEAN_methodVisibility, value);
	}

	/**
	 * <b>Configuration property:</b>  Use Java {@link Introspector} for determining bean properties.
	 *
	 * <p>
	 * Using the built-in Java bean introspector will not pick up fields or non-standard getters/setters.
	 *
	 * <h5 class 'section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_useJavaBeanIntrospector</jsf>, value)</code>.
	 * 	<li>Most {@link Bean @Bean} annotations will be ignored if you enable this setting.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_useJavaBeanIntrospector
	 */
	public BeanContextBuilder useJavaBeanIntrospector(boolean value) {
		return set(BEAN_useJavaBeanIntrospector, value);
	}

	/**
	 * <b>Configuration property:</b>  Use interface proxies.
	 *
	 * <p>
	 * If <jk>true</jk>, then interfaces will be instantiated as proxy classes through the use of an
	 * {@link InvocationHandler} if there is no other way of instantiating them.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_useInterfaceProxies</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_useInterfaceProxies
	 */
	public BeanContextBuilder useInterfaceProxies(boolean value) {
		return set(BEAN_useInterfaceProxies, value);
	}

	/**
	 * <b>Configuration property:</b>  Ignore unknown properties.
	 *
	 * <p>
	 * If <jk>true</jk>, trying to set a value on a non-existent bean property will silently be ignored.
	 * Otherwise, a {@code BeanRuntimeException} is thrown.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_ignoreUnknownBeanProperties</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_ignoreUnknownBeanProperties
	 */
	public BeanContextBuilder ignoreUnknownBeanProperties(boolean value) {
		return set(BEAN_ignoreUnknownBeanProperties, value);
	}

	/**
	 * <b>Configuration property:</b>  Ignore unknown properties with null values.
	 *
	 * <p>
	 * If <jk>true</jk>, trying to set a <jk>null</jk> value on a non-existent bean property will silently be ignored.
	 * Otherwise, a {@code BeanRuntimeException} is thrown.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_ignoreUnknownNullBeanProperties</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_ignoreUnknownNullBeanProperties
	 */
	public BeanContextBuilder ignoreUnknownNullBeanProperties(boolean value) {
		return set(BEAN_ignoreUnknownNullBeanProperties, value);
	}

	/**
	 * <b>Configuration property:</b>  Ignore properties without setters.
	 *
	 * <p>
	 * If <jk>true</jk>, trying to set a value on a bean property without a setter will silently be ignored.
	 * Otherwise, a {@code BeanRuntimeException} is thrown.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_ignorePropertiesWithoutSetters</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_ignorePropertiesWithoutSetters
	 */
	public BeanContextBuilder ignorePropertiesWithoutSetters(boolean value) {
		return set(BEAN_ignorePropertiesWithoutSetters, value);
	}

	/**
	 * <b>Configuration property:</b>  Ignore invocation errors on getters.
	 *
	 * <p>
	 * If <jk>true</jk>, errors thrown when calling bean getter methods will silently be ignored.
	 * Otherwise, a {@code BeanRuntimeException} is thrown.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_ignoreInvocationExceptionsOnGetters</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_ignoreInvocationExceptionsOnGetters
	 */
	public BeanContextBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
		return set(BEAN_ignoreInvocationExceptionsOnGetters, value);
	}

	/**
	 * <b>Configuration property:</b>  Ignore invocation errors on setters.
	 *
	 * <p>
	 * If <jk>true</jk>, errors thrown when calling bean setter methods will silently be ignored.
	 * Otherwise, a {@code BeanRuntimeException} is thrown.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_ignoreInvocationExceptionsOnSetters</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_ignoreInvocationExceptionsOnSetters
	 */
	public BeanContextBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
		return set(BEAN_ignoreInvocationExceptionsOnSetters, value);
	}

	/**
	 * <b>Configuration property:</b>  Sort bean properties in alphabetical order.
	 *
	 * <p>
	 * When <jk>true</jk>, all bean properties will be serialized and access in alphabetical order.
	 * Otherwise, the natural order of the bean properties is used which is dependent on the JVM vendor.
	 * On IBM JVMs, the bean properties are ordered based on their ordering in the Java file.
	 * On Oracle JVMs, the bean properties are not ordered (which follows the official JVM specs).
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>
	 * 		This is equivalent to calling <code>property(<jsf>BEAN_sortProperties</jsf>, value)</code>.
	 * 	<li>
	 * 		This property is disabled by default so that IBM JVM users don't have to use {@link Bean @Bean} annotations
	 * 		to force bean properties to be in a particular order and can just alter the order of the fields/methods
	 * 		in the Java file.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_sortProperties
	 */
	public BeanContextBuilder sortProperties(boolean value) {
		return set(BEAN_sortProperties, value);
	}

	/**
	 * <b>Configuration property:</b>  Packages whose classes should not be considered beans.
	 *
	 * <p>
	 * When specified, the current list of ignore packages are appended to.
	 *
	 * <p>
	 * Any classes within these packages will be serialized to strings using {@link Object#toString()}.
	 *
	 * <p>
	 * Note that you can specify prefix patterns to include all subpackages.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_notBeanPackages</jsf>, values)</code>.
	 * </ul>
	 * 
	 * @param append
	 * 	If <jk>true</jk>, the previous value is appended to.  Otherwise, the previous value is replaced. 
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages
	 */
	public BeanContextBuilder notBeanPackages(boolean append, String...values) {
		return set(append, BEAN_notBeanPackages, values);
	}

	/**
	 * <b>Configuration property:</b>  Packages whose classes should not be considered beans.
	 *
	 * Same as {@link #notBeanPackages(boolean, String...)} but using a <code>Collection</code>.
	 * 
	 * @param append
	 * 	If <jk>true</jk>, the previous value is appended to.  Otherwise, the previous value is replaced. 
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages
	 */
	public BeanContextBuilder notBeanPackages(boolean append, Collection<String> values) {
		return set(append, BEAN_notBeanPackages, values);
	}

	/**
	 * <b>Configuration property:</b>  Add to packages whose classes should not be considered beans.
	 *
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
	public BeanContextBuilder notBeanPackages(String...values) {
		return addTo(BEAN_notBeanPackages, values);
	}

	/**
	 * <b>Configuration property:</b>  Add to packages whose classes should not be considered beans.
	 *
	 * <p>
	 * Same as {@link #notBeanPackages(String...)} but using a <code>Collection</code>.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages
	 */
	public BeanContextBuilder notBeanPackages(Collection<String> values) {
		return addTo(BEAN_notBeanPackages, values);
	}

	/**
	 * <b>Configuration property:</b>  Remove from packages whose classes should not be considered beans.
	 *
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
	public BeanContextBuilder notBeanPackagesRemove(String...values) {
		return removeFrom(BEAN_notBeanPackages, values);
	}

	/**
	 * <b>Configuration property:</b>  Remove from packages whose classes should not be considered beans.
	 *
	 * <p>
	 * Same as {@link #notBeanPackagesRemove(String...)} but using a <code>Collection</code>.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages
	 * @see BeanContext#BEAN_notBeanPackages_remove
	 */
	public BeanContextBuilder notBeanPackagesRemove(Collection<String> values) {
		return removeFrom(BEAN_notBeanPackages, values);
	}

	/**
	 * <b>Configuration property:</b>  Classes to be excluded from consideration as being beans.
	 *
	 * <p>
	 * Not-bean classes are typically converted to <code>Strings</code> during serialization even if they appear to be
	 * bean-like.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_notBeanClasses</jsf>, values)</code>.
	 * </ul>
	 * 
	 * @param append
	 * 	If <jk>true</jk>, the previous value is appended to.  Otherwise, the previous value is replaced. 
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanClasses
	 */
	public BeanContextBuilder notBeanClasses(boolean append, Class<?>...values) {
		return set(append, BEAN_notBeanClasses, values);
	}

	/**
	 * <b>Configuration property:</b>  Classes to be excluded from consideration as being beans.
	 *
	 * <p>
	 * Same as {@link #notBeanClasses(boolean, Class...)} but using a <code>Collection</code>.
	 * 
	 * @param append
	 * 	If <jk>true</jk>, the previous value is appended to.  Otherwise, the previous value is replaced. 
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanPackages
	 */
	public BeanContextBuilder notBeanClasses(boolean append, Collection<Class<?>> values) {
		return set(append, BEAN_notBeanClasses, values);
	}

	/**
	 * <b>Configuration property:</b>  Add to classes that should not be considered beans.
	 *
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
	public BeanContextBuilder notBeanClasses(Class<?>...values) {
		return addTo(BEAN_notBeanClasses, values);
	}

	/**
	 * <b>Configuration property:</b>  Add to classes that should not be considered beans.
	 *
	 * <p>
	 * Same as {@link #notBeanClasses(Class...)} but using a <code>Collection</code>.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanClasses
	 * @see BeanContext#BEAN_notBeanClasses_add
	 */
	public BeanContextBuilder notBeanClasses(Collection<Class<?>> values) {
		return addTo(BEAN_notBeanClasses, values);
	}

	/**
	 * <b>Configuration property:</b>  Remove from classes that should not be considered beans.
	 *
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
	public BeanContextBuilder notBeanClassesRemove(Class<?>...values) {
		return removeFrom(BEAN_notBeanClasses, values);
	}

	/**
	 * <b>Configuration property:</b>  Remove from classes that should not be considered beans.
	 *
	 * <p>
	 * Same as {@link #notBeanClassesRemove(Class...)} but using a <code>Collection</code>.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_notBeanClasses
	 * @see BeanContext#BEAN_notBeanClasses_remove
	 */
	public BeanContextBuilder notBeanClassesRemove(Collection<Class<?>> values) {
		return removeFrom(BEAN_notBeanClasses, values);
	}

	/**
	 * <b>Configuration property:</b>  Bean filters to apply to beans.
	 *
	 * <p>
	 * This is a programmatic equivalent to the {@link Bean @Bean} annotation.
	 * It's useful when you want to use the Bean annotation functionality, but you don't have the ability to alter the
	 * bean classes.
	 *
	 * <p>
	 * There are two category of classes that can be passed in through this method:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Subclasses of {@link BeanFilterBuilder}.
	 * 		These must have a public no-arg constructor.
	 * 	<li>
	 * 		Bean interface classes.
	 * 		A shortcut for defining a {@link InterfaceBeanFilterBuilder}.
	 * 		Any subclasses of an interface class will only have properties defined on the interface.
	 * 		All other bean properties will be ignored.
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_beanFilters</jsf>, values)</code>.
	 * </ul>
	 * 
	 * @param append
	 * 	If <jk>true</jk>, the previous value is appended to.  Otherwise, the previous value is replaced. 
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanFilters
	 */
	public BeanContextBuilder beanFilters(boolean append, Class<?>...values) {
		return set(append, BEAN_beanFilters, values);
	}

	/**
	 * <b>Configuration property:</b>  Bean filters to apply to beans.
	 *
	 * <p>
	 * Same as {@link #beanFilters(boolean, Class...)} but using a <code>Collection</code>.
	 * 
	 * @param append
	 * 	If <jk>true</jk>, the previous value is appended to.  Otherwise, the previous value is replaced. 
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanFilters
	 */
	public BeanContextBuilder beanFilters(boolean append, Collection<Class<?>> values) {
		return set(append, BEAN_beanFilters, values);
	}

	/**
	 * <b>Configuration property:</b>  Add to bean filters.
	 *
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
	public BeanContextBuilder beanFilters(Class<?>...values) {
		return addTo(BEAN_beanFilters, values);
	}

	/**
	 * <b>Configuration property:</b>  Add to bean filters.
	 *
	 * <p>
	 * Same as {@link #beanFilters(Class...)} but using a <code>Collection</code>.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanFilters
	 * @see BeanContext#BEAN_beanFilters_add
	 */
	public BeanContextBuilder beanFilters(Collection<Class<?>> values) {
		return addTo(BEAN_beanFilters, values);
	}

	/**
	 * <b>Configuration property:</b>  Remove from bean filters.
	 *
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
	public BeanContextBuilder beanFiltersRemove(Class<?>...values) {
		return removeFrom(BEAN_beanFilters, values);
	}

	/**
	 * <b>Configuration property:</b>  Remove from bean filters.
	 *
	 * <p>
	 * Same as {@link #beanFiltersRemove(Class...)} but using a <code>Collection</code>.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanFilters
	 * @see BeanContext#BEAN_beanFilters_remove
	 */
	public BeanContextBuilder beanFiltersRemove(Collection<Class<?>> values) {
		return removeFrom(BEAN_beanFilters, values);
	}

	/**
	 * <b>Configuration property:</b>  POJO swaps to apply to Java objects.
	 *
	 * <p>
	 * There are two category of classes that can be passed in through this method:
	 * <ul>
	 * 	<li>Subclasses of {@link PojoSwap}.
	 * 	<li>Implementations of {@link Surrogate}.
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_pojoSwaps</jsf>, values)</code>.
	 * </ul>
	 * 
	 * @param append
	 * 	If <jk>true</jk>, the previous value is appended to.  Otherwise, the previous value is replaced. 
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_pojoSwaps
	 */
	public BeanContextBuilder pojoSwaps(boolean append, Class<?>...values) {
		return set(append, BEAN_pojoSwaps, values);
	}

	/**
	 * <b>Configuration property:</b>  POJO swaps to apply to Java objects.
	 *
	 * <p>
	 * Same as {@link #pojoSwaps(boolean, Class...)} but using a <code>Collection</code>.
	 * 
	 * @param append
	 * 	If <jk>true</jk>, the previous value is appended to.  Otherwise, the previous value is replaced. 
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_pojoSwaps
	 */
	public BeanContextBuilder pojoSwaps(boolean append, Collection<Class<?>> values) {
		return set(append, BEAN_pojoSwaps, values);
	}

	/**
	 * <b>Configuration property:</b>  Add to POJO swaps.
	 *
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
	public BeanContextBuilder pojoSwaps(Class<?>...values) {
		return addTo(BEAN_pojoSwaps, values);
	}

	/**
	 * <b>Configuration property:</b>  Add to POJO swaps.
	 *
	 * <p>
	 * Same as {@link #pojoSwaps(Class...)} but using a <code>Collection</code>.
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_pojoSwaps
	 * @see BeanContext#BEAN_pojoSwaps_add
	 */
	public BeanContextBuilder pojoSwaps(Collection<Class<?>> values) {
		return addTo(BEAN_pojoSwaps, values);
	}

	/**
	 * <b>Configuration property:</b>  Remove from POJO swaps.
	 *
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
	public BeanContextBuilder pojoSwapsRemove(Class<?>...values) {
		return removeFrom(BEAN_pojoSwaps, values);
	}

	/**
	 * <b>Configuration property:</b>  Remove from POJO swaps.
	 *
	 * <p>
	 * Same as {@link #pojoSwapsRemove(Class...)} but using a <code>Collection</code>.
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_pojoSwaps
	 * @see BeanContext#BEAN_pojoSwaps_remove
	 */
	public BeanContextBuilder pojoSwapsRemove(Collection<Class<?>> values) {
		return removeFrom(BEAN_pojoSwaps, values);
	}

	/**
	 * <b>Configuration property:</b>  Implementation classes for interfaces and abstract classes.
	 *
	 * <p>
	 * For interfaces and abstract classes this method can be used to specify an implementation class for the
	 * interface/abstract class so that instances of the implementation class are used when instantiated (e.g. during a
	 * parse).
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_implClasses</jsf>, values)</code>.
	 * </ul>
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_implClasses
	 */
	public BeanContextBuilder implClasses(Map<String,Class<?>> values) {
		return set(BEAN_implClasses, values);
	}

	/**
	 * <b>Configuration property:</b>  Implementation classes for interfaces and abstract classes.
	 *
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
	 */
	public <I> BeanContextBuilder implClass(Class<I> interfaceClass, Class<? extends I> implClass) {
		return addTo(BEAN_implClasses, interfaceClass.getName(), implClass);
	}

	/**
	 * <b>Configuration property:</b>  Explicitly specify visible bean properties.
	 *
	 * <p>
	 * Specifies to only include the specified list of properties for the specified bean classes.
	 *
	 * <p>
	 * The keys are either fully-qualified or simple class names, and the values are comma-delimited lists of property
	 * names.
	 * The key <js>"*"</js> means all bean classes.
	 *
	 * <p>
	 * For example, <code>{Bean1:<js>"foo,bar"</js>}</code> means only serialize the <code>foo</code> and <code>bar</code>
	 * properties on the specified bean.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_includeProperties</jsf>, values)</code>.
	 * </ul>
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_includeProperties
	 */
	public BeanContextBuilder includeProperties(Map<String,String> values) {
		return set(BEAN_includeProperties, values);
	}

	/**
	 * <b>Configuration property:</b>  Explicitly specify visible bean properties.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>putToProperty(<jsf>BEAN_includeProperties</jsf>, beanClassName, properties)</code>
	 * 		or <code>property(<jsf>BEAN_includeProperties_put</jsf>, beanClassName, properties)</code>.
	 * </ul>
	 *
	 * @param beanClassName The bean class name.  Can be a simple name, fully-qualified name, or <js>"*"</js>.
	 * @param properties Comma-delimited list of property names.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_includeProperties
	 */
	public BeanContextBuilder includeProperties(String beanClassName, String properties) {
		return addTo(BEAN_includeProperties, beanClassName, properties);
	}

	/**
	 * <b>Configuration property:</b>  Explicitly specify visible bean properties.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>putToProperty(<jsf>BEAN_includeProperties</jsf>, beanClass.getName(), properties)</code>
	 * 		or <code>property(<jsf>BEAN_includeProperties_put</jsf>, beanClass.getName(), properties)</code>.
	 * </ul>
	 *
	 * @param beanClass The bean class.
	 * @param properties Comma-delimited list of property names.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_includeProperties
	 */
	public BeanContextBuilder includeProperties(Class<?> beanClass, String properties) {
		return addTo(BEAN_includeProperties, beanClass.getName(), properties);
	}

	/**
	 * <b>Configuration property:</b>  Exclude specified properties from beans.
	 *
	 * <p>
	 * Specifies to exclude the specified list of properties for the specified bean classes.
	 *
	 * <p>
	 * The keys are either fully-qualified or simple class names, and the values are comma-delimited lists of property
	 * names.
	 * The key <js>"*"</js> means all bean classes.
	 *
	 * <p>
	 * For example, <code>{Bean1:<js>"foo,bar"</js>}</code> means don't serialize the <code>foo</code> and <code>bar</code>
	 * properties on the specified bean.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_excludeProperties</jsf>, values)</code>.
	 * </ul>
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_excludeProperties
	 */
	public BeanContextBuilder excludeProperties(Map<String,String> values) {
		return set(BEAN_excludeProperties, values);
	}

	/**
	 * <b>Configuration property:</b>  Exclude specified properties from beans.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>putToProperty(<jsf>BEAN_excludeProperties</jsf>, beanClassName, properties)</code>
	 * 		or <code>property(<jsf>BEAN_excludeProperties_put</jsf>, beanClassName, properties)</code>.
	 * </ul>
	 *
	 * @param beanClassName The bean class name.  Can be a simple name, fully-qualified name, or <js>"*"</js>.
	 * @param properties Comma-delimited list of property names.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_excludeProperties
	 */
	public BeanContextBuilder excludeProperties(String beanClassName, String properties) {
		return addTo(BEAN_excludeProperties, beanClassName, properties);
	}

	/**
	 * <b>Configuration property:</b>  Exclude specified properties from beans.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>putToProperty(<jsf>BEAN_excludeProperties</jsf>, beanClass.getName(), properties)</code>
	 * 		or <code>property(<jsf>BEAN_excludeProperties_put</jsf>, beanClass.getName(), properties)</code>.
	 * </ul>
	 *
	 * @param beanClass The bean class.
	 * @param properties Comma-delimited list of property names.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_excludeProperties
	 */
	public BeanContextBuilder excludeProperties(Class<?> beanClass, String properties) {
		return addTo(BEAN_excludeProperties, beanClass.getName(), properties);
	}

	/**
	 * <b>Configuration property:</b>  Bean lookup dictionary.
	 * 
	 * <h6 class='figure'>Example:</h6>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_beanDictionary</jsf>, values)</code>.
	 * </ul>
	 * 
	 * @param append
	 * 	If <jk>true</jk>, the previous value is appended to.  Otherwise, the previous value is replaced. 
	 * @param values 
	 * 	The new value for this property.
	 * 	<br>Values can be any of the following types:
	 * 	<ul>
	 * 		<li>Any bean class that specifies a value for {@link Bean#typeName() @Bean.typeName()}.
	 * 		<li>Any subclass of {@link BeanDictionaryList} containing a collection of bean classes with type name
	 * 			annotations.
	 * 		<li>Any subclass of {@link BeanDictionaryMap} containing a mapping of type names to classes without type name
	 * 			annotations.
	 * 		<li>Any array or collection of the types above:
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanDictionary
	 */
	public BeanContextBuilder beanDictionary(boolean append, Object...values) {
		return set(append, BEAN_beanDictionary, values);
	}

	/**
	 * <b>Configuration property:</b>  Bean lookup dictionary.
	 *
	 * <p>
	 * The list of classes that make up the bean dictionary in this bean context.
	 * 
	 * <p>
	 * A dictionary is a name/class mapping used to find class types during parsing when they cannot be inferred
	 * through reflection.
	 * <br>The names are defined through the {@link Bean#typeName()} annotation defined on the bean class.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	BeanContext bc = BeanContext.<jsf>create</jsf>().beanDictionary(Bar.<jk>class</jk>, Baz.<jk>class</jk>).build();
	 * </p>
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Properties:
	 * 		<ul> 	
	 * 			<li>{@link BeanContext#BEAN_beanDictionary}
	 * 			<li>{@link BeanContext#BEAN_beanDictionary_add}
	 * 			<li>{@link BeanContext#BEAN_beanDictionary_remove}
	 * 		</ul>
	 * 	<li>Annotations:  
	 * 		<ul>
	 * 			<li>{@link Bean#beanDictionary()}
	 * 			<li>{@link BeanProperty#beanDictionary()}
	 * 		</ul>
	 * 	<li>Methods:  
	 * 		<ul>
	 * 			<li>{@link BeanContextBuilder#beanDictionary(Object...)}
	 * 			<li>{@link BeanContextBuilder#beanDictionary(boolean,Object...)}
	 * 			<li>{@link BeanContextBuilder#beanDictionaryRemove(Object...)}
	 * 		</ul>
	 * 	<li>Values can consist of any of the following types:
	 *			<ul>
	 * 			<li>Any bean class that specifies a value for {@link Bean#typeName() @Bean.typeName()}.
	 * 			<li>Any subclass of {@link BeanDictionaryList} containing a collection of bean classes with type name
	 * 				annotations.
	 * 			<li>Any subclass of {@link BeanDictionaryMap} containing a mapping of type names to classes without type name
	 * 				annotations.
	 * 		</ul>
	 * 	<li>See <a class='doclink' href='../../../overview-summary.html#juneau-marshall.BeanDictionaries'>Bean Names and Dictionaries</a> 
	 * 		for more information.
	 *	</ul>
	 *
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beanDictionary(Object...values) {
		return addTo(BEAN_beanDictionary, values);
	}

	/**
	 * <b>Configuration property:</b>  Remove from bean dictionary.
	 *
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
	public BeanContextBuilder beanDictionaryRemove(Object...values) {
		return removeFrom(BEAN_beanDictionary, values);
	}

	/**
	 * <b>Configuration property:</b>  Name to use for the bean type properties used to represent a bean type.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_beanTypePropertyName</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_beanTypePropertyName
	 */
	public BeanContextBuilder beanTypePropertyName(String value) {
		return set(BEAN_beanTypePropertyName, value);
	}

	/**
	 * <b>Configuration property:</b>  Default parser to use when converting <code>Strings</code> to POJOs.
	 *
	 * <p>
	 * Used in the in the {@link BeanSession#convertToType(Object, Class)} method.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_defaultParser</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_defaultParser
	 */
	public BeanContextBuilder defaultParser(Class<?> value) {
		return set(BEAN_defaultParser, value);
	}

	/**
	 * <b>Configuration property:</b>  Locale.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_locale</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_locale
	 */
	public BeanContextBuilder locale(Locale value) {
		return set(BEAN_locale, value);
	}

	/**
	 * <b>Configuration property:</b>  TimeZone.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_timeZone</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_timeZone
	 */
	public BeanContextBuilder timeZone(TimeZone value) {
		return set(BEAN_timeZone, value);
	}

	/**
	 * <b>Configuration property:</b>  Media type.
	 *
	 * <p>
	 * Specifies a default media type value for serializer and parser sessions.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_mediaType</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_mediaType
	 */
	public BeanContextBuilder mediaType(MediaType value) {
		return set(BEAN_mediaType, value);
	}
	
	/**
	 * <b>Configuration property:</b>  Debug mode.
	 *
	 * <p>
	 * Enables the following additional information during serialization:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		When bean getters throws exceptions, the exception includes the object stack information
	 * 		in order to determine how that method was invoked.
	 * 	<li>
	 * 		Enables {@link Serializer#SERIALIZER_detectRecursions}.
	 * </ul>
	 *
	 * <p>
	 * Enables the following additional information during parsing:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		When bean setters throws exceptions, the exception includes the object stack information
	 * 		in order to determine how that method was invoked.
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>property(<jsf>BEAN_debug</jsf>, value)</code>.
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 * @see BeanContext#BEAN_debug
	 */
	public BeanContextBuilder debug() {
		return set(BEAN_debug, true);
	}

	@Override /* ContextBuilder */
	public BeanContextBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}
}