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
import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.utils.*;

/**
 * Builder class for building instances of serializers, parsers, and bean contexts.
 *
 * <p>
 * All serializers and parsers extend from this class.
 *
 * <p>
 * Provides a base set of common config property setters that allow you to build up serializers and parsers.
 *
 * <p class='bcode w800'>
 * 	WriterSerializer s = JsonSerializer
 * 		.<jsm>create</jsm>()
 * 		.set(<jsf>JSON_simpleMode</jsf>, <jk>true</jk>)
 * 		.set(<jsf>SERIALIZER_useWhitespace</jsf>, <jk>true</jk>)
 * 		.set(<jsf>SERIALIZER_quoteChar</jsf>, <js>"'"</js>)
 * 		.build();
 * </p>
 *
 * <p>
 * Additional convenience methods are provided for setting properties using reduced syntax.
 *
 * <p class='bcode w800'>
 * 	WriterSerializer s = JsonSerializer
 * 		.<jsm>create</jsm>()  <jc>// Create a JsonSerializerBuilder</jc>
 * 		.simple()  <jc>// Simple mode</jc>
 * 		.ws()  <jc>// Use whitespace</jc>
 * 		.sq()  <jc>// Use single quotes </jc>
 * 		.build();  <jc>// Create a JsonSerializer</jc>
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'>{@doc juneau-marshall.ConfigurableProperties}
 * </ul>
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

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

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
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link Visibility#PUBLIC}.
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
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link Visibility#PUBLIC}.
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanDictionary}
	 * </ul>
	 *
	 * @param values
	 * 	The values to add to this property.
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
	 * @param values
	 * 	The values to add to this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beanDictionary(Class<?>...values) {
		return addTo(BEAN_beanDictionary, values);
	}

	/**
	 * Configuration property:  Bean dictionary.
	 *
	 * <p>
	 * Same as {@link #beanDictionary(Object...)} but replaces the existing value.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanDictionary}
	 * </ul>
	 *
	 * @param values
	 * 	The new values for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beanDictionaryReplace(Class<?>...values) {
		return set(BEAN_beanDictionary, values);
	}

	/**
	 * Configuration property:  Bean dictionary.
	 *
	 * <p>
	 * Same as {@link #beanDictionary(Object...)} but replaces the existing value.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanDictionary}
	 * </ul>
	 *
	 * @param values
	 * 	The new values for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beanDictionaryReplace(Object...values) {
		return set(BEAN_beanDictionary, values);
	}

	/**
	 * Configuration property:  Bean dictionary.
	 *
	 * <p>
	 * Removes from the list of classes that make up the bean dictionary in this bean context.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanDictionary}
	 * </ul>
	 *
	 * @param values
	 * 	The values to remove from this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beanDictionaryRemove(Class<?>...values) {
		return removeFrom(BEAN_beanDictionary, values);
	}

	/**
	 * Configuration property:  Bean dictionary.
	 *
	 * <p>
	 * Removes from the list of classes that make up the bean dictionary in this bean context.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanDictionary}
	 * </ul>
	 *
	 * @param values
	 * 	The values to remove from this property.
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
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link Visibility#PUBLIC}.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beanFieldVisibility(Visibility value) {
		return set(BEAN_beanFieldVisibility, value);
	}

	/**
	 * Configuration property:  Bean filters.
	 *
	 * <p>
	 * This is a programmatic equivalent to the {@link Bean @Bean} annotation.
	 * <br>It's useful when you want to use the Bean annotation functionality, but you don't have the ability to alter
	 * the bean classes.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanFilters}
	 * </ul>
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Values can consist of any of the following types:
	 * 	<ul>
	 * 		<li>Any bean class that specifies a value for {@link Bean#typeName() @Bean(typeName)}.
	 * 		<li>Any subclass of {@link BeanDictionaryList} containing a collection of bean classes with type name annotations.
	 * 		<li>Any subclass of {@link BeanDictionaryMap} containing a mapping of type names to classes without type name annotations.
	 * 		<li>Any array or collection of the objects above.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beanFilters(Object...values) {
		return addTo(BEAN_beanFilters, values);
	}

	/**
	 * Configuration property:  Bean filters.
	 *
	 * <p>
	 * Same as {@link #beanFilters(Object...)} but takes in an array of classes.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanFilters}
	 * </ul>
	 *
	 * @param values
	 * 	The values to add to this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beanFilters(Class<?>...values) {
		return addTo(BEAN_beanFilters, values);
	}

	/**
	 * Configuration property:  Bean filters.
	 *
	 * <p>
	 * Same as {@link #beanFilters(Object...)} but replaces the existing values.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanFilters}
	 * </ul>
	 *
	 * @param values
	 * 	The new values for this property.
	 * 	<br>Values can consist of any of the following types:
	 * 	<ul>
	 * 		<li>Any bean class that specifies a value for {@link Bean#typeName() @Bean(typeName)}.
	 * 		<li>Any subclass of {@link BeanDictionaryList} containing a collection of bean classes with type name annotations.
	 * 		<li>Any subclass of {@link BeanDictionaryMap} containing a mapping of type names to classes without type name annotations.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beanFiltersReplace(Class<?>...values) {
		return set(BEAN_beanFilters, values);
	}

	/**
	 * Configuration property:  Bean filters.
	 *
	 * <p>
	 * Same as {@link #beanFilters(Object...)} but replaces the existing values.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanFilters}
	 * </ul>
	 *
	 * @param values
	 * 	The new values for this property.
	 * 	<br>Values can consist of any of the following types:
	 * 	<ul>
	 * 		<li>Any bean class that specifies a value for {@link Bean#typeName() @Bean(typeName)}.
	 * 		<li>Any subclass of {@link BeanDictionaryList} containing a collection of bean classes with type name annotations.
	 * 		<li>Any subclass of {@link BeanDictionaryMap} containing a mapping of type names to classes without type name annotations.
	 * 		<li>Any array or collection of the objects above.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beanFiltersReplace(Object...values) {
		return set(BEAN_beanFilters, values);
	}

	/**
	 * Configuration property:  Bean filters.
	 *
	 * <p>
	 * Removes from the list of classes that make up the bean filters in this bean context.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanFilters}
	 * </ul>
	 *
	 * @param values
	 * 	The values to remove from this property.
	 * 	<br>Values can consist of any of the following types:
	 * 	<ul>
	 * 		<li>Any bean class that specifies a value for {@link Bean#typeName() @Bean(typeName)}.
	 * 		<li>Any subclass of {@link BeanDictionaryList} containing a collection of bean classes with type name annotations.
	 * 		<li>Any subclass of {@link BeanDictionaryMap} containing a mapping of type names to classes without type name annotations.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beanFiltersRemove(Class<?>...values) {
		return removeFrom(BEAN_beanFilters, values);
	}

	/**
	 * Configuration property:  Bean filters.
	 *
	 * <p>
	 * Removes from the list of classes that make up the bean filters in this bean context.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanFilters}
	 * </ul>
	 *
	 * @param values
	 * 	The values to remove from this property.
	 * 	<br>Values can consist of any of the following types:
	 * 	<ul>
	 * 		<li>Any bean class that specifies a value for {@link Bean#typeName() @Bean(typeName)}.
	 * 		<li>Any subclass of {@link BeanDictionaryList} containing a collection of bean classes with type name annotations.
	 * 		<li>Any subclass of {@link BeanDictionaryMap} containing a mapping of type names to classes without type name annotations.
	 * 		<li>Any array or collection of the objects above.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beanFiltersRemove(Object...values) {
		return removeFrom(BEAN_beanFilters, values);
	}

	/**
	 * Configuration property:  BeanMap.put() returns old property value.
	 *
	 * <p>
	 * If <jk>true</jk>, then the {@link BeanMap#put(String,Object) BeanMap.put()} method will return old property
	 * values.
	 * <br>Otherwise, it returns <jk>null</jk>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanMapPutReturnsOldValue}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beanMapPutReturnsOldValue(boolean value) {
		return set(BEAN_beanMapPutReturnsOldValue, value);
	}

	/**
	 * Configuration property:  BeanMap.put() returns old property value.
	 *
	 * <p>
	 * Shortcut for calling <code>beanMapPutReturnsOldValue(<jk>true</jk>)</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanMapPutReturnsOldValue}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beanMapPutReturnsOldValue() {
		return set(BEAN_beanMapPutReturnsOldValue, true);
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
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link Visibility#PUBLIC}
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
	 * <br>Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beansRequireDefaultConstructor}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beansRequireDefaultConstructor(boolean value) {
		return set(BEAN_beansRequireDefaultConstructor, value);
	}

	/**
	 * Configuration property:  Beans require no-arg constructors.
	 *
	 * <p>
	 * Shortcut for calling <code>beansRequireDefaultConstructor(<jk>true</jk>)</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beansRequireDefaultConstructor}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beansRequireDefaultConstructor() {
		return set(BEAN_beansRequireDefaultConstructor, true);
	}

	/**
	 * Configuration property:  Beans require Serializable interface.
	 *
	 * <p>
	 * If <jk>true</jk>, a Java class must implement the {@link Serializable} interface to be considered a bean.
	 * <br>Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beansRequireSerializable}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beansRequireSerializable(boolean value) {
		return set(BEAN_beansRequireSerializable, value);
	}

	/**
	 * Configuration property:  Beans require Serializable interface.
	 *
	 * <p>
	 * Shortcut for calling <code>beansRequireSerializable(<jk>true</jk>)</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beansRequireSerializable}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beansRequireSerializable() {
		return set(BEAN_beansRequireSerializable, true);
	}

	/**
	 * Configuration property:  Beans require setters for getters.
	 *
	 * <p>
	 * If <jk>true</jk>, only getters that have equivalent setters will be considered as properties on a bean.
	 * <br>Otherwise, they will be ignored.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beansRequireSettersForGetters}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beansRequireSettersForGetters(boolean value) {
		return set(BEAN_beansRequireSettersForGetters, value);
	}

	/**
	 * Configuration property:  Beans require setters for getters.
	 *
	 * <p>
	 * Shortcut for calling <code>beansRequireSettersForGetters(<jk>true</jk>)</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beansRequireSettersForGetters}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beansRequireSettersForGetters() {
		return set(BEAN_beansRequireSettersForGetters, true);
	}

	/**
	 * Configuration property:  Beans require at least one property.
	 *
	 * <p>
	 * If <jk>true</jk>, then a Java class must contain at least 1 property to be considered a bean.
	 * <br>Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beansRequireSomeProperties}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>true</jk>.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder beansRequireSomeProperties(boolean value) {
		return set(BEAN_beansRequireSomeProperties, value);
	}

	/**
	 * Configuration property:  Bean type property name.
	 *
	 * <p>
	 * This specifies the name of the bean property used to store the dictionary name of a bean type so that the
	 * parser knows the data type to reconstruct.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanTypePropertyName}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <js>"_type"</js>.
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
	 * 		Enables {@link Serializer#BEANTRAVERSE_detectRecursions}.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_debug}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder debug(boolean value) {
		return set(BEAN_debug, value);
	}

	/**
	 * Configuration property:  Debug mode.
	 *
	 * <p>
	 * Shortcut for calling <code>debug(<jk>true</jk>)</code>.
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
	 * Configuration property:  POJO example.
	 *
	 * <p>
	 * Specifies an example of the specified class.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_examples}
	 * </ul>
	 *
	 * @param pojoClass The POJO class.
	 * @param o An instance of the POJO class used for examples.
	 * @return This object (for method chaining).
	 */
	public <T> BeanContextBuilder example(Class<T> pojoClass, T o) {
		return addTo(BEAN_examples, pojoClass.getName(), o);
	}

	/**
	 * Configuration property:  POJO example.
	 *
	 * <p>
	 * Specifies an example of the specified class.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_examples}
	 * </ul>
	 *
	 * @param <T>
	 * @param pojoClass The POJO class.
	 * @param json The simple JSON representation of the example.
	 * @return This object (for method chaining).
	 */
	public <T> BeanContextBuilder exampleJson(Class<T> pojoClass, String json) {
		try {
			return addTo(BEAN_examples, pojoClass.getName(), SimpleJson.DEFAULT.read(json, pojoClass));
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Configuration property:  POJO examples.
	 *
	 * <p>
	 * Specifies an example of the specified class.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_examples}
	 * </ul>
	 *
	 * @param json The simple JSON representation of the example.
	 * @return This object (for method chaining).
	 * @throws ParseException If parameter is not valid Simple-JSON.
	 */
	public BeanContextBuilder examples(String json) throws ParseException {
		if (! isObjectMap(json, true))
			json = "{" + json + "}";
		ObjectMap m = new ObjectMap(json);
		for (Map.Entry<String,Object> e : m.entrySet())
			addTo(BEAN_examples, e.getKey(), e.getValue());
		return this;
	}

	/**
	 * Configuration property:  Bean property excludes.
	 *
	 * <p>
	 * Specifies to exclude the specified list of properties for the specified bean class.
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
	 * Configuration property:  Bean property excludes.
	 *
	 * <p>
	 * Specifies to exclude the specified list of properties for the specified bean classes.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_excludeProperties}
	 * </ul>
	 *
	 * @param values
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder excludeProperties(Map<String,String> values) {
		return set(BEAN_excludeProperties, values);
	}

	/**
	 * Configuration property:  Bean property excludes.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_excludeProperties}
	 * </ul>
	 *
	 * @param beanClassName
	 * 	The bean class name.
	 * 	<br>Can be a simple name, fully-qualified name, or <js>"*"</js> for all bean classes.
	 * @param value Comma-delimited list of property names.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder excludeProperties(String beanClassName, String value) {
		return addTo(BEAN_excludeProperties, beanClassName, value);
	}

	/**
	 * Configuration property:  Find fluent setters.
	 *
	 * <p>
	 * When enabled, fluent setters are detected on beans.
	 *
	 * <p>
	 * Fluent setters must have the following attributes:
	 * <ul>
	 * 	<li>Public.
	 * 	<li>Not static.
	 * 	<li>Take in one parameter.
	 * 	<li>Return the bean itself.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_fluentSetters}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder fluentSetters(boolean value) {
		return set(BEAN_fluentSetters, value);
	}

	/**
	 * Configuration property:  Find fluent setters.
	 *
	 * <p>
	 * Shortcut for calling <code>fluentSetters(<jk>true</jk>)</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_fluentSetters}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder fluentSetters() {
		return set(BEAN_fluentSetters, true);
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
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
		return set(BEAN_ignoreInvocationExceptionsOnGetters, value);
	}

	/**
	 * Configuration property:  Ignore invocation errors on getters.
	 *
	 * <p>
	 * Shortcut for calling <code>ignoreInvocationExceptionsOnGetters(<jk>true</jk>)</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_ignoreInvocationExceptionsOnGetters}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder ignoreInvocationExceptionsOnGetters() {
		return set(BEAN_ignoreInvocationExceptionsOnGetters, true);
	}

	/**
	 * Configuration property:  Ignore invocation errors on setters.
	 *
	 * <p>
	 * If <jk>true</jk>, errors thrown when calling bean setter methods will silently be ignored.
	 * <br>Otherwise, a {@code BeanRuntimeException} is thrown.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_ignoreInvocationExceptionsOnSetters}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
		return set(BEAN_ignoreInvocationExceptionsOnSetters, value);
	}

	/**
	 * Configuration property:  Ignore invocation errors on setters.
	 *
	 * <p>
	 * Shortcut for calling <code>ignoreInvocationExceptionsOnSetters(<jk>true</jk>)</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_ignoreInvocationExceptionsOnSetters}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder ignoreInvocationExceptionsOnSetters() {
		return set(BEAN_ignoreInvocationExceptionsOnSetters, true);
	}

	/**
	 * Configuration property:  Ignore properties without setters.
	 *
	 * <p>
	 * If <jk>true</jk>, trying to set a value on a bean property without a setter will silently be ignored.
	 * <br>Otherwise, a {@code BeanRuntimeException} is thrown.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_ignorePropertiesWithoutSetters}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>true</jk>.
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
	 * <br>Otherwise, a {@code BeanRuntimeException} is thrown.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_ignoreUnknownBeanProperties}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder ignoreUnknownBeanProperties(boolean value) {
		return set(BEAN_ignoreUnknownBeanProperties, value);
	}

	/**
	 * Configuration property:  Ignore unknown properties.
	 *
	 * <p>
	 * Shortcut for calling <code>ignoreUnknownBeanProperties(<jk>true</jk>)</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_ignoreUnknownBeanProperties}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder ignoreUnknownBeanProperties() {
		return set(BEAN_ignoreUnknownBeanProperties, true);
	}

	/**
	 * Configuration property:  Ignore unknown properties with null values.
	 *
	 * <p>
	 * If <jk>true</jk>, trying to set a <jk>null</jk> value on a non-existent bean property will silently be ignored.
	 * <br>Otherwise, a {@code BeanRuntimeException} is thrown.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_ignoreUnknownNullBeanProperties}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>true</jk>.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder ignoreUnknownNullBeanProperties(boolean value) {
		return set(BEAN_ignoreUnknownNullBeanProperties, value);
	}

	/**
	 * Configuration property:  Implementation classes.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_implClasses}
	 * </ul>
	 *
	 * @param interfaceClass The interface class.
	 * @param implClass The implementation class.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder implClass(Class<?> interfaceClass, Class<?> implClass) {
		return addTo(BEAN_implClasses, interfaceClass.getName(), implClass);
	}

	/**
	 * Configuration property:  Implementation classes.
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
	 * Configuration property:  Bean property includes.
	 *
	 * <p>
	 * Specifies the set and order of names of properties associated with the bean class.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_includeProperties}
	 * </ul>
	 *
	 * @param beanClass The bean class.
	 * @param value Comma-delimited list of property names.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder includeProperties(Class<?> beanClass, String value) {
		return addTo(BEAN_includeProperties, beanClass.getName(), value);
	}

	/**
	 * Configuration property:  Bean property includes.
	 *
	 * <p>
	 * Specifies the set and order of names of properties associated with the bean class.
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
	 * Configuration property:  Bean property includes.
	 *
	 * <p>
	 * Specifies the set and order of names of properties associated with the bean class.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_includeProperties}
	 * </ul>
	 *
	 * @param beanClassName
	 * 	The bean class name.
	 * 	<br>Can be a simple name, fully-qualified name, or <js>"*"</js> for all beans.
	 * @param value Comma-delimited list of property names.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder includeProperties(String beanClassName, String value) {
		return addTo(BEAN_includeProperties, beanClassName, value);
	}

	/**
	 * Configuration property:  Locale.
	 *
	 * <p>
	 * Specifies a default locale for serializer and parser sessions.
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
	 * Configuration property:  Bean class exclusions.
	 *
	 * <p>
	 * List of classes that should not be treated as beans even if they appear to be bean-like.
	 * <br>Not-bean classes are converted to <code>Strings</code> during serialization.
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
	 * Configuration property:  Bean class exclusions.
	 *
	 * <p>
	 * List of classes that should not be treated as beans even if they appear to be bean-like.
	 * <br>Not-bean classes are converted to <code>Strings</code> during serialization.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_notBeanClasses}
	 * </ul>
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Values can consist of any of the following types:
	 * 	<ul>
	 * 		<li>Classes.
	 * 		<li>Arrays and collections of classes.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder notBeanClasses(Object...values) {
		return addTo(BEAN_notBeanClasses, values);
	}

	/**
	 * Configuration property:  Bean class exclusions.
	 *
	 * <p>
	 * Not-bean classes are converted to <code>Strings</code> during serialization even if they appear to be
	 * bean-like.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_notBeanClasses}
	 * </ul>
	 *
	 * @param values
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder notBeanClassesReplace(Class<?>...values) {
		return set(BEAN_notBeanClasses, values);
	}

	/**
	 * Configuration property:  Bean class exclusions.
	 *
	 * <p>
	 * Not-bean classes are converted to <code>Strings</code> during serialization even if they appear to be
	 * bean-like.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_notBeanClasses}
	 * </ul>
	 *
	 * @param values
	 * 	The new value for this property.
	 * 	<br>Values can consist of any of the following types:
	 * 	<ul>
	 * 		<li>Classes.
	 * 		<li>Arrays and collections of classes.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder notBeanClassesReplace(Object...values) {
		return set(BEAN_notBeanClasses, values);
	}

	/**
	 * Configuration property:  Bean class exclusions.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_notBeanClasses}
	 * </ul>
	 *
	 * @param values
	 * 	The values to remove from this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder notBeanClassesRemove(Class<?>...values) {
		return removeFrom(BEAN_notBeanClasses, values);
	}

	/**
	 * Configuration property:  Bean class exclusions.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_notBeanClasses}
	 * </ul>
	 *
	 * @param values
	 * 	The values to remove from this property.
	 * 	<br>Values can consist of any of the following types:
	 * 	<ul>
	 * 		<li>Classes.
	 * 		<li>Arrays and collections of classes.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder notBeanClassesRemove(Object...values) {
		return removeFrom(BEAN_notBeanClasses, values);
	}

	/**
	 * Configuration property:  Bean package exclusions.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_notBeanPackages}
	 * </ul>
	 *
	 * @param values
	 * 	The values to add to this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder notBeanPackages(String...values) {
		return addTo(BEAN_notBeanPackages, values);
	}

	/**
	 * Configuration property:  Bean package exclusions.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_notBeanPackages}
	 * </ul>
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Values can consist of any of the following types:
	 * 	<ul>
	 * 		<li>Strings.
	 * 		<li>Arrays and collections of strings.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder notBeanPackages(Object...values) {
		return addTo(BEAN_notBeanPackages, values);
	}

	/**
	 * Configuration property:  Bean package exclusions.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_notBeanPackages}
	 * </ul>
	 *
	 * @param values
	 * 	<br>Values can consist of any of the following types:
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder notBeanPackagesReplace(String...values) {
		return set(BEAN_notBeanPackages, values);
	}

	/**
	 * Configuration property:  Bean package exclusions.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_notBeanPackages}
	 * </ul>
	 *
	 * @param values
	 * 	<br>Values can consist of any of the following types:
	 * 	<br>Possible values are:
	 * 	<ul>
	 * 		<li>Strings.
	 * 		<li>Arrays and collections of strings.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder notBeanPackagesReplace(Object...values) {
		return set(BEAN_notBeanPackages, values);
	}

	/**
	 * Configuration property:  Bean package exclusions.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_notBeanPackages}
	 * </ul>
	 *
	 * @param values The values to remove from this property.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder notBeanPackagesRemove(String...values) {
		return removeFrom(BEAN_notBeanPackages, values);
	}

	/**
	 * Configuration property:  Bean package exclusions.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_notBeanPackages}
	 * </ul>
	 *
	 * @param values
	 * 	<br>Values can consist of any of the following types:
	 * 	<br>Possible values are:
	 * 	<ul>
	 * 		<li>Strings.
	 * 		<li>Arrays and collections of strings.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder notBeanPackagesRemove(Object...values) {
		return removeFrom(BEAN_notBeanPackages, values);
	}

	/**
	 * Configuration property:  POJO swaps.
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
	 * Configuration property:  POJO swaps.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_pojoSwaps}
	 * </ul>
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Values can consist of any of the following types:
	 * 	<ul>
	 * 		<li>Any subclass of {@link PojoSwap}.
	 * 		<li>Any surrogate class.  A shortcut for defining a {@link SurrogateSwap}.
	 * 		<li>Any array or collection of the objects above.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder pojoSwaps(Object...values) {
		return addTo(BEAN_pojoSwaps, values);
	}

	/**
	 * Configuration property:  POJO swaps.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_pojoSwaps}
	 * </ul>
	 *
	 * @param values
	 * 	The values to remove from this property.
	 * 	<br>Values can consist of any of the following types:
	 * 	<ul>
	 * 		<li>Any subclass of {@link PojoSwap}.
	 * 		<li>Any surrogate class.  A shortcut for defining a {@link SurrogateSwap}.
	 * 		<li>Any array or collection of the objects above.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder pojoSwapsReplace(Class<?>...values) {
		return set(BEAN_pojoSwaps, values);
	}

	/**
	 * Configuration property:  POJO swaps.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_pojoSwaps}
	 * </ul>
	 *
	 * @param values
	 * 	The values to remove from this property.
	 * 	<br>Values can consist of any of the following types:
	 * 	<ul>
	 * 		<li>Any subclass of {@link PojoSwap}.
	 * 		<li>Any surrogate class.  A shortcut for defining a {@link SurrogateSwap}.
	 * 		<li>Any array or collection of the objects above.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder pojoSwapsReplace(Object...values) {
		return set(BEAN_pojoSwaps, values);
	}

	/**
	 * Configuration property:  POJO swaps.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_pojoSwaps}
	 * </ul>
	 *
	 * @param values
	 * 	The values to remove from this property.
	 * 	<br>Values can consist of any of the following types:
	 * 	<ul>
	 * 		<li>Any subclass of {@link PojoSwap}.
	 * 		<li>Any surrogate class.  A shortcut for defining a {@link SurrogateSwap}.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder pojoSwapsRemove(Class<?>...values) {
		return removeFrom(BEAN_pojoSwaps, values);
	}

	/**
	 * Configuration property:  POJO swaps.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_pojoSwaps}
	 * </ul>
	 *
	 * @param values
	 * 	The values to remove from this property.
	 * 	<br>Values can consist of any of the following types:
	 * 	<ul>
	 * 		<li>Any subclass of {@link PojoSwap}.
	 * 		<li>Any surrogate class.  A shortcut for defining a {@link SurrogateSwap}.
	 * 		<li>Any array or collection of the objects above.
	 * 	</ul>
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
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link PropertyNamerDefault}.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder propertyNamer(Class<? extends PropertyNamer> value) {
		return set(BEAN_propertyNamer, value);
	}

	/**
	 * Configuration property:  Sort bean properties.
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
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder sortProperties(boolean value) {
		return set(BEAN_sortProperties, value);
	}

	/**
	 * Configuration property:  Sort bean properties.
	 *
	 * <p>
	 * Shortcut for calling <code>sortProperties(<jk>true</jk>)</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_sortProperties}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder sortProperties() {
		return set(BEAN_sortProperties, true);
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
	 * Configuration property:  Use enum names.
	 *
	 * <p>
	 * When enabled, enums are always serialized by name instead of using {@link Object#toString()}.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_useEnumNames}
	 * </ul>
	 *
	 * @param value The property value.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder useEnumNames(boolean value) {
		return set(BEAN_useEnumNames, value);
	}

	/**
	 * Configuration property:  Use enum names.
	 *
	 * <p>
	 * When enabled, enums are always serialized by name instead of using {@link Object#toString()}.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_useEnumNames}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder useEnumNames() {
		return set(BEAN_useEnumNames, true);
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
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>true</jk>.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder useInterfaceProxies(boolean value) {
		return set(BEAN_useInterfaceProxies, value);
	}

	/**
	 * Configuration property:  Use Java Introspector.
	 *
	 * <p>
	 * Using the built-in Java bean introspector will not pick up fields or non-standard getters/setters.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>Most {@link Bean @Bean} annotations will be ignored if you enable this setting.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_useJavaBeanIntrospector}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder useJavaBeanIntrospector(boolean value) {
		return set(BEAN_useJavaBeanIntrospector, value);
	}

	/**
	 * Configuration property:  Use Java Introspector.
	 *
	 * <p>
	 * Shortcut for calling <code>useJavaBeanIntrospector(<jk>true</jk>)</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_useJavaBeanIntrospector}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public BeanContextBuilder useJavaBeanIntrospector() {
		return set(BEAN_useJavaBeanIntrospector, true);
	}

	@Override /* ContextBuilder */
	public BeanContextBuilder set(String name, Object value) {
		super.set(name, value);
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
	
	@Override
	public BeanContextBuilder applyAnnotations(AnnotationsMap m, StringResolver sr) {
		super.applyAnnotations(m, sr);
		return this;
	}
}