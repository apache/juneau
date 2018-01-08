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

	/**
	 * Configuration property:  Minimum bean class visibility.
	 *
	 * <p>
	 * Classes are not considered beans unless they meet the minimum visibility requirements.
	 * 
	 * <p>
	 * For example, if the visibility is <code>PUBLIC</code> and the bean class is <jk>protected</jk>, then the class
	 * will not be interpreted as a bean class and will be treated as a string.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanClassVisibility}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beanClassVisibility(Visibility value) {
		return set(BEAN_beanClassVisibility, value);
	}

	/**
	 * Configuration property:  Minimum bean constructor visibility.
	 *
	 * <p>
	 * Only look for constructors with the specified minimum visibility.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanConstructorVisibility}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beanConstructorVisibility(Visibility value) {
		return set(BEAN_beanConstructorVisibility, value);
	}

	/**
	 * Configuration property:  Bean dictionary.
	 *
	 * <p>
	 * Adds to the list of classes that make up the bean dictionary in this bean context.
	 * 
	 * <p>
	 * A dictionary is a name/class mapping used to find class types during parsing when they cannot be inferred
	 * through reflection.
	 * <br>The names are defined through the {@link Bean#typeName()} annotation defined on the bean class.
	 * <br>For example, if a class <code>Foo</code> has a type-name of <js>"myfoo"</js>, then it would end up serialized
	 * as <js>"{_type:'myfoo',...}"</js>.
	 * 
	 * <p>
	 * This setting tells the parsers which classes to look for when resolving <js>"_type"</js> attributes.
	 * 
	 * <p>
	 * Values can consist of any of the following types:
	 *	<ul>
	 * 	<li>Any bean class that specifies a value for {@link Bean#typeName() @Bean.typeName()}.
	 * 	<li>Any subclass of {@link BeanDictionaryList} containing a collection of bean classes with type name annotations.
	 * 	<li>Any subclass of {@link BeanDictionaryMap} containing a mapping of type names to classes without type name annotations.
	 * 	<li>Any array or collection of the objects above.
	 * </ul>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanDictionary}
	 * </ul>
	 * 
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beanDictionary(Object...values) {
		return addTo(BEAN_beanDictionary, values);
	}

	/**
	 * Configuration property:  Bean dictionary.
	 *
	 * <p>
	 * Same as {@link #beanDictionary(Object...)} but takes in an array of classes.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanDictionary}
	 * </ul>
	 * 
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beanDictionary(Class<?>...values) {
		return addTo(BEAN_beanDictionary, values);
	}

	/**
	 * Configuration property:  Bean lookup dictionary.
	 * 
	 * <p>
	 * Same as {@link #beanDictionary(Object...)} but allows you to optionally overwrite the previous value.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanDictionary}
	 * </ul>
	 * 
	 * @param append
	 * 	If <jk>true</jk>, the previous value is appended to.  Otherwise, the previous value is replaced. 
	 * @param values The new values for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beanDictionary(boolean append, Object...values) {
		return set(append, BEAN_beanDictionary, values);
	}

	/**
	 * Configuration property:  Remove from bean dictionary.
	 * 
	 * <p>
	 * Removes from the list of classes that make up the bean dictionary in this bean context.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanDictionary}
	 * </ul>
	 * 
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beanDictionaryRemove(Object...values) {
		return removeFrom(BEAN_beanDictionary, values);
	}

	/**
	 * Configuration property:  Minimum bean field visibility.
	 *
	 * <p>
	 * Only look for bean fields with the specified minimum visibility.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanFieldVisibility}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beanFieldVisibility(Visibility value) {
		return set(BEAN_beanFieldVisibility, value);
	}

	/**
	 * Configuration property:  Bean filters to apply to beans.
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanFilters}
	 * </ul>
	 * 
	 * @param append
	 * 	If <jk>true</jk>, the previous value is appended to.  Otherwise, the previous value is replaced. 
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beanFilters(boolean append, Object...values) {
		return set(append, BEAN_beanFilters, values);
	}

	/**
	 * Configuration property:  Add to bean filters.
	 * 
	 * <p>
	 * Same as {@link #beanFilters(Object...)} but takes in an array of classes.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanFilters}
	 * </ul>
	 * 
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beanFilters(Class<?>...values) {
		return addTo(BEAN_beanFilters, values);
	}

	/**
	 * Configuration property:  Add to bean filters.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>addToProperty(<jsf>BEAN_beanFilters</jsf>, values)</code>
	 * 		or <code>property(<jsf>BEAN_beanFilters_add</jsf>, values)</code>.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanFilters}
	 * </ul>
	 * 
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beanFilters(Object...values) {
		return addTo(BEAN_beanFilters, values);
	}

	/**
	 * Configuration property:  Remove from bean filters.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>removeFromProperty(<jsf>BEAN_beanFilters</jsf>, values)</code>
	 * 		or <code>property(<jsf>BEAN_beanFilters_remove</jsf>, values)</code>.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanFilters}
	 * </ul>
	 * 
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beanFiltersRemove(Object...values) {
		return removeFrom(BEAN_beanFilters, values);
	}

	/**
	 * Configuration property:  {@link BeanMap#put(String,Object) BeanMap.put()} method will return old property
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanMapPutReturnsOldValue}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beanMapPutReturnsOldValue(boolean value) {
		return set(BEAN_beanMapPutReturnsOldValue, value);
	}

	/**
	 * Configuration property:  Minimum bean method visibility.
	 *
	 * <p>
	 * Only look for bean methods with the specified minimum visibility.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanMethodVisibility}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beanMethodVisibility(Visibility value) {
		return set(BEAN_beanMethodVisibility, value);
	}

	/**
	 * Configuration property:  Beans require no-arg constructors.
	 *
	 * <p>
	 * If <jk>true</jk>, a Java class must implement a default no-arg constructor to be considered a bean.
	 * Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 *
	 * <p>
	 * The {@link Bean @Bean} annotation can be used on a class to override this setting when <jk>true</jk>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beansRequireDefaultConstructor}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beansRequireDefaultConstructor(boolean value) {
		return set(BEAN_beansRequireDefaultConstructor, value);
	}

	/**
	 * Configuration property:  Beans require {@link Serializable} interface.
	 *
	 * <p>
	 * If <jk>true</jk>, a Java class must implement the {@link Serializable} interface to be considered a bean.
	 * Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 *
	 * <p>
	 * The {@link Bean @Bean} annotation can be used on a class to override this setting when <jk>true</jk>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beansRequireSerializable}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beansRequireSerializable(boolean value) {
		return set(BEAN_beansRequireSerializable, value);
	}

	/**
	 * Configuration property:  Beans require setters for getters.
	 *
	 * <p>
	 * If <jk>true</jk>, only getters that have equivalent setters will be considered as properties on a bean.
	 * Otherwise, they will be ignored.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beansRequireSettersForGetters}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beansRequireSettersForGetters(boolean value) {
		return set(BEAN_beansRequireSettersForGetters, value);
	}

	/**
	 * Configuration property:  Beans require at least one property.
	 *
	 * <p>
	 * If <jk>true</jk>, then a Java class must contain at least 1 property to be considered a bean.
	 * Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 *
	 * <p>
	 * The {@link Bean @Bean} annotation can be used on a class to override this setting when <jk>true</jk>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beansRequireSomeProperties}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beansRequireSomeProperties(boolean value) {
		return set(BEAN_beansRequireSomeProperties, value);
	}

	/**
	 * Configuration property:  Name to use for the bean type properties used to represent a bean type.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanTypePropertyName}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beanTypePropertyName(String value) {
		return set(BEAN_beanTypePropertyName, value);
	}

	/**
	 * Configuration property:  Debug mode.
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_debug}
	 * </ul>
	 * 
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder debug() {
		return set(BEAN_debug, true);
	}

	/**
	 * Configuration property:  Exclude specified properties from beans.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_excludeProperties}
	 * </ul>
	 * 
	 * @param beanClass The bean class.
	 * @param properties Comma-delimited list of property names.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder excludeProperties(Class<?> beanClass, String properties) {
		return addTo(BEAN_excludeProperties, beanClass.getName(), properties);
	}

	/**
	 * Configuration property:  Exclude specified properties from beans.
	 *
	 * <p>
	 * Specifies to exclude the specified list of properties for the specified bean classes.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_excludeProperties}
	 * </ul>
	 * 
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder excludeProperties(Map<String,String> values) {
		return set(BEAN_excludeProperties, values);
	}

	/**
	 * Configuration property:  Exclude specified properties from beans.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_excludeProperties}
	 * </ul>
	 * 
	 * @param beanClassName The bean class name.  Can be a simple name, fully-qualified name, or <js>"*"</js>.
	 * @param properties Comma-delimited list of property names.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder excludeProperties(String beanClassName, String properties) {
		return addTo(BEAN_excludeProperties, beanClassName, properties);
	}

	/**
	 * Configuration property:  Ignore invocation errors on getters.
	 *
	 * <p>
	 * If <jk>true</jk>, errors thrown when calling bean getter methods will silently be ignored.
	 * Otherwise, a {@code BeanRuntimeException} is thrown.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_ignoreInvocationExceptionsOnGetters}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
		return set(BEAN_ignoreInvocationExceptionsOnGetters, value);
	}

	/**
	 * Configuration property:  Ignore invocation errors on setters.
	 *
	 * <p>
	 * If <jk>true</jk>, errors thrown when calling bean setter methods will silently be ignored.
	 * Otherwise, a {@code BeanRuntimeException} is thrown.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_ignoreInvocationExceptionsOnSetters}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
		return set(BEAN_ignoreInvocationExceptionsOnSetters, value);
	}

	/**
	 * Configuration property:  Ignore properties without setters.
	 *
	 * <p>
	 * If <jk>true</jk>, trying to set a value on a bean property without a setter will silently be ignored.
	 * Otherwise, a {@code BeanRuntimeException} is thrown.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_ignorePropertiesWithoutSetters}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder ignorePropertiesWithoutSetters(boolean value) {
		return set(BEAN_ignorePropertiesWithoutSetters, value);
	}

	/**
	 * Configuration property:  Ignore unknown properties.
	 *
	 * <p>
	 * If <jk>true</jk>, trying to set a value on a non-existent bean property will silently be ignored.
	 * Otherwise, a {@code BeanRuntimeException} is thrown.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_ignoreUnknownBeanProperties}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder ignoreUnknownBeanProperties(boolean value) {
		return set(BEAN_ignoreUnknownBeanProperties, value);
	}

	/**
	 * Configuration property:  Ignore unknown properties with null values.
	 *
	 * <p>
	 * If <jk>true</jk>, trying to set a <jk>null</jk> value on a non-existent bean property will silently be ignored.
	 * Otherwise, a {@code BeanRuntimeException} is thrown.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_ignoreUnknownNullBeanProperties}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder ignoreUnknownNullBeanProperties(boolean value) {
		return set(BEAN_ignoreUnknownNullBeanProperties, value);
	}

	/**
	 * Configuration property:  Implementation classes for interfaces and abstract classes.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_implClasses}
	 * </ul>
	 * 
	 * @param interfaceClass The interface class.
	 * @param implClass The implementation class.
	 * @param <I> The class type of the interface.
	 * @return This object (for method chaining).
	 */
	public <I> BeanContextBuilder implClass(Class<I> interfaceClass, Class<? extends I> implClass) {
		return addTo(BEAN_implClasses, interfaceClass.getName(), implClass);
	}

	/**
	 * Configuration property:  Implementation classes for interfaces and abstract classes.
	 *
	 * <p>
	 * For interfaces and abstract classes this method can be used to specify an implementation class for the
	 * interface/abstract class so that instances of the implementation class are used when instantiated (e.g. during a
	 * parse).
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_implClasses}
	 * </ul>
	 * 
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder implClasses(Map<String,Class<?>> values) {
		return set(BEAN_implClasses, values);
	}

	/**
	 * Configuration property:  Explicitly specify visible bean properties.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_includeProperties}
	 * </ul>
	 * 
	 * @param beanClass The bean class.
	 * @param properties Comma-delimited list of property names.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder includeProperties(Class<?> beanClass, String properties) {
		return addTo(BEAN_includeProperties, beanClass.getName(), properties);
	}

	/**
	 * Configuration property:  Explicitly specify visible bean properties.
	 *
	 * <p>
	 * Specifies to only include the specified list of properties for the specified bean classes.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_includeProperties}
	 * </ul>
	 * 
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder includeProperties(Map<String,String> values) {
		return set(BEAN_includeProperties, values);
	}

	/**
	 * Configuration property:  Explicitly specify visible bean properties.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_includeProperties}
	 * </ul>
	 * 
	 * @param beanClassName The bean class name.  Can be a simple name, fully-qualified name, or <js>"*"</js>.
	 * @param properties Comma-delimited list of property names.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder includeProperties(String beanClassName, String properties) {
		return addTo(BEAN_includeProperties, beanClassName, properties);
	}

	/**
	 * Configuration property:  Locale.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_locale}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder locale(Locale value) {
		return set(BEAN_locale, value);
	}

	/**
	 * Configuration property:  Media type.
	 *
	 * <p>
	 * Specifies a default media type value for serializer and parser sessions.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_mediaType}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder mediaType(MediaType value) {
		return set(BEAN_mediaType, value);
	}

	/**
	 * Configuration property:  Classes to be excluded from consideration as being beans.
	 *
	 * <p>
	 * Not-bean classes are typically converted to <code>Strings</code> during serialization even if they appear to be
	 * bean-like.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_notBeanClasses}
	 * </ul>
	 * 
	 * @param append
	 * 	If <jk>true</jk>, the previous value is appended to.  Otherwise, the previous value is replaced. 
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder notBeanClasses(boolean append, Object...values) {
		return set(append, BEAN_notBeanClasses, values);
	}

	/**
	 * Configuration property:  Add to classes that should not be considered beans.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_notBeanClasses}
	 * </ul>
	 * 
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder notBeanClasses(Class<?>...values) {
		return addTo(BEAN_notBeanClasses, values);
	}

	/**
	 * Configuration property:  Add to classes that should not be considered beans.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_notBeanClasses}
	 * </ul>
	 * 
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder notBeanClasses(Object...values) {
		return addTo(BEAN_notBeanClasses, values);
	}

	/**
	 * Configuration property:  Remove from classes that should not be considered beans.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_notBeanClasses}
	 * </ul>
	 * 
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder notBeanClassesRemove(Object...values) {
		return removeFrom(BEAN_notBeanClasses, values);
	}

	/**
	 * Configuration property:  Packages whose classes should not be considered beans.
	 *
	 * <p>
	 * When specified, the current list of ignore packages are appended to.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_notBeanPackages}
	 * </ul>
	 * 
	 * @param append
	 * 	If <jk>true</jk>, the previous value is appended to.  Otherwise, the previous value is replaced. 
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder notBeanPackages(boolean append, Object...values) {
		return set(append, BEAN_notBeanPackages, values);
	}

	/**
	 * Configuration property:  Add to packages whose classes should not be considered beans.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_notBeanPackages}
	 * </ul>
	 * 
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder notBeanPackages(Object...values) {
		return addTo(BEAN_notBeanPackages, values);
	}

	/**
	 * Configuration property:  Add to packages whose classes should not be considered beans.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_notBeanPackages}
	 * </ul>
	 * 
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder notBeanPackages(String...values) {
		return addTo(BEAN_notBeanPackages, values);
	}

	/**
	 * Configuration property:  Remove from packages whose classes should not be considered beans.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_notBeanPackages}
	 * </ul>
	 * 
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder notBeanPackagesRemove(Object...values) {
		return removeFrom(BEAN_notBeanPackages, values);
	}

	/**
	 * Configuration property:  POJO swaps to apply to Java objects.
	 *
	 * <p>
	 * There are two category of classes that can be passed in through this method:
	 * <ul>
	 * 	<li>Subclasses of {@link PojoSwap}.
	 * 	<li>Implementations of {@link Surrogate}.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_pojoSwaps}
	 * </ul>
	 * 
	 * @param append
	 * 	If <jk>true</jk>, the previous value is appended to.  Otherwise, the previous value is replaced. 
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder pojoSwaps(boolean append, Object...values) {
		return set(append, BEAN_pojoSwaps, values);
	}

	/**
	 * Configuration property:  Add to POJO swaps.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_pojoSwaps}
	 * </ul>
	 * 
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder pojoSwaps(Class<?>...values) {
		return addTo(BEAN_pojoSwaps, values);
	}

	/**
	 * Configuration property:  Add to POJO swaps.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_pojoSwaps}
	 * </ul>
	 * 
	 * @param values The values to add to this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder pojoSwaps(Object...values) {
		return addTo(BEAN_pojoSwaps, values);
	}

	/**
	 * Configuration property:  Remove from POJO swaps.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_pojoSwaps}
	 * </ul>
	 * 
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder pojoSwapsRemove(Object...values) {
		return removeFrom(BEAN_pojoSwaps, values);
	}

	/**
	 * Configuration property:  Bean property namer
	 *
	 * <p>
	 * The class to use for calculating bean property names.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_propertyNamer}
	 * </ul>
	 * 
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder propertyNamer(Class<? extends PropertyNamer> value) {
		return set(BEAN_propertyNamer, value);
	}

	/**
	 * Configuration property:  Sort bean properties in alphabetical order.
	 *
	 * <p>
	 * When <jk>true</jk>, all bean properties will be serialized and access in alphabetical order.
	 * Otherwise, the natural order of the bean properties is used which is dependent on the JVM vendor.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_sortProperties}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder sortProperties(boolean value) {
		return set(BEAN_sortProperties, value);
	}

	/**
	 * Configuration property:  TimeZone.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_timeZone}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder timeZone(TimeZone value) {
		return set(BEAN_timeZone, value);
	}
	
	/**
	 * Configuration property:  Use interface proxies.
	 *
	 * <p>
	 * If <jk>true</jk>, then interfaces will be instantiated as proxy classes through the use of an
	 * {@link InvocationHandler} if there is no other way of instantiating them.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_useInterfaceProxies}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder useInterfaceProxies(boolean value) {
		return set(BEAN_useInterfaceProxies, value);
	}

	/**
	 * Configuration property:  Use Java {@link Introspector} for determining bean properties.
	 *
	 * <p>
	 * Using the built-in Java bean introspector will not pick up fields or non-standard getters/setters.
	 *
	 * <h5 class 'section'>Notes:</h5>
	 * <ul>
	 * 	<li>Most {@link Bean @Bean} annotations will be ignored if you enable this setting.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_useJavaBeanIntrospector}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder useJavaBeanIntrospector(boolean value) {
		return set(BEAN_useJavaBeanIntrospector, value);
	}

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

	@Override /* ContextBuilder */
	public BeanContextBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}
}