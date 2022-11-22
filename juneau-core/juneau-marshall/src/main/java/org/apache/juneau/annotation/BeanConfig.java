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
package org.apache.juneau.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.swap.*;

/**
 * Annotation for specifying config properties defined in {@link BeanContext} and {@link BeanTraverseContext}.
 *
 * <p>
 * Used primarily for specifying bean configuration properties on REST classes and methods.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
@Target({TYPE,METHOD})
@Retention(RUNTIME)
@Inherited
@ContextApply(BeanConfigAnnotation.Applier.class)
public @interface BeanConfig {

	/**
	 * Optional rank for this config.
	 *
	 * <p>
	 * Can be used to override default ordering and application of config annotations.
	 *
	 * @return The annotation value.
	 */
	int rank() default 0;

	//-----------------------------------------------------------------------------------------------------------------
	// BeanContext
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Minimum bean class visibility.
	 *
	 * <p>
	 * Classes are not considered beans unless they meet the minimum visibility requirements.
	 *
	 * <p>
	 * For example, if the visibility is <c>PUBLIC</c> and the bean class is <jk>protected</jk>, then the class
	 * will not be interpreted as a bean class and be serialized as a string.
	 * <br>Use this setting to reduce the visibility requirement.
	 *
	 * <ul class='values'>
	 * 	<li><js>"PUBLIC"</js> (default)
	 * 	<li><js>"PROTECTED"</js>
	 * 	<li><js>"DEFAULT"</js>
	 * 	<li><js>"PRIVATE"</js>
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beanClassVisibility(Visibility)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String beanClassVisibility() default "";

	/**
	 * Minimum bean constructor visibility.
	 *
	 * <p>
	 * Only look for constructors with the specified minimum visibility.
	 *
	 * <p>
	 * This setting affects the logic for finding no-arg constructors for bean.
	 * <br>Normally, only <jk>public</jk> no-arg constructors are used.
	 * <br>Use this setting if you want to reduce the visibility requirement.
	 *
	 * <ul class='values'>
	 * 	<li><js>"PUBLIC"</js> (default)
	 * 	<li><js>"PROTECTED"</js>
	 * 	<li><js>"DEFAULT"</js>
	 * 	<li><js>"PRIVATE"</js>
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beanConstructorVisibility(Visibility)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String beanConstructorVisibility() default "";

	/**
	 * Minimum bean field visibility.
	 *
	 * <p>
	 * Only look for bean fields with the specified minimum visibility.
	 *
	 * <p>
	 * This affects which fields on a bean class are considered bean properties.
	 * <br>Normally only <jk>public</jk> fields are considered.
	 * <br>Use this setting if you want to reduce the visibility requirement.
	 *
	 * <ul class='values'>
	 * 	<li><js>"PUBLIC"</js> (default)
	 * 	<li><js>"PROTECTED"</js>
	 * 	<li><js>"DEFAULT"</js>
	 * 	<li><js>"PRIVATE"</js>
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beanFieldVisibility(Visibility)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String beanFieldVisibility() default "";

	/**
	 * BeanMap.put() returns old property value.
	 *
	 * <p>
	 * If <js>"true"</js>, then the {@link BeanMap#put(String,Object) BeanMap.put()} method will return old property
	 * values.
	 * <br>Otherwise, it returns <jk>null</jk>.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default because it introduces a slight performance penalty during serialization)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beanMapPutReturnsOldValue()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String beanMapPutReturnsOldValue() default "";

	/**
	 * Minimum bean method visibility.
	 *
	 * <p>
	 * Only look for bean methods with the specified minimum visibility.
	 *
	 * <p>
	 * This affects which methods are detected as getters and setters on a bean class.
	 * <br>Normally only <jk>public</jk> getters and setters are considered.
	 * <br>Use this setting if you want to reduce the visibility requirement.
	 *
	 * <ul class='values'>
	 * 	<li><js>"PUBLIC"</js> (default)
	 * 	<li><js>"PROTECTED"</js>
	 * 	<li><js>"DEFAULT"</js>
	 * 	<li><js>"PRIVATE"</js>
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beanMethodVisibility(Visibility)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String beanMethodVisibility() default "";

	/**
	 * Beans require no-arg constructors.
	 *
	 * <p>
	 * If <js>"true"</js>, a Java class must implement a default no-arg constructor to be considered a bean.
	 * <br>Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * 	<li class='note'>
	 * 		The {@link Bean @Bean} annotation can be used on a class to override this setting when <js>"true"</js>.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beansRequireDefaultConstructor()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String beansRequireDefaultConstructor() default "";

	/**
	 * Beans require Serializable interface.
	 *
	 * <p>
	 * If <js>"true"</js>, a Java class must implement the {@link Serializable} interface to be considered a bean.
	 * <br>Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * 	<li class='note'>
	 * 		The {@link Bean @Bean} annotation can be used on a class to override this setting when <js>"true"</js>.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beansRequireSerializable()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String beansRequireSerializable() default "";

	/**
	 * Beans require setters for getters.
	 *
	 * <p>
	 * If <js>"true"</js>, only getters that have equivalent setters will be considered as properties on a bean.
	 * <br>Otherwise, they will be ignored.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beansRequireSettersForGetters()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String beansRequireSettersForGetters() default "";

	/**
	 * Beans don't require at least one property.
	 *
	 * <p>
	 * If <js>"true"</js>, then a Java class doesn't need to contain at least 1 property to be considered a bean.
	 * <br>Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#disableBeansRequireSomeProperties()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String disableBeansRequireSomeProperties() default "";

	/**
	 * Bean type property name.
	 *
	 * <p>
	 * This specifies the name of the bean property used to store the dictionary name of a bean type so that the
	 * parser knows the data type to reconstruct.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Default value: <js>"_type"</js>.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>

	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Bean#typePropertyName()}
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#typePropertyName(String)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String typePropertyName() default "";

	/**
	 * Debug mode.
	 *
	 * <p>
	 * Enables the following additional information during serialization:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		When bean getters throws exceptions, the exception includes the object stack information
	 * 		in order to determine how that method was invoked.
	 * 	<li>
	 * 		Enables {@link org.apache.juneau.BeanTraverseContext.Builder#detectRecursions()}.
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
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.Context.Builder#debug()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String debug() default "";

	/**
	 * Bean dictionary.
	 *
	 * <p>
	 * The list of classes that make up the bean dictionary in this bean context.
	 *
	 * <p>
	 * A dictionary is a name/class mapping used to find class types during parsing when they cannot be inferred
	 * through reflection.
	 * <br>The names are defined through the {@link Bean#typeName() @Bean(typeName)} annotation defined on the bean class.
	 * <br>For example, if a class <c>Foo</c> has a type-name of <js>"myfoo"</js>, then it would end up serialized
	 * as <js>"{_type:'myfoo',...}"</js>.
	 *
	 * <p>
	 * This setting tells the parsers which classes to look for when resolving <js>"_type"</js> attributes.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Bean#dictionary()}
	 * 	<li class='ja'>{@link Beanp#dictionary()}
	 * 	<li class='ja'>{@link BeanConfig#dictionary_replace()}
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beanDictionary(Class...)}
	 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.BeanDictionaries">Bean Names and Dictionaries</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<?>[] dictionary() default {};

	/**
	 * Replace bean dictionary.
	 *
	 * <p>
	 * Same as {@link #dictionary()} but replaces any existing value.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Bean#dictionary()}
	 * 	<li class='ja'>{@link Beanp#dictionary()}
	 * 	<li class='ja'>{@link BeanConfig#dictionary()}
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beanDictionary(Class...)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<?>[] dictionary_replace() default {};

	/**
	 * Find fluent setters.
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
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Bean#findFluentSetters()}
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#findFluentSetters()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String findFluentSetters() default "";

	/**
	 * Ignore invocation errors on getters.
	 *
	 * <p>
	 * If <js>"true"</js>, errors thrown when calling bean getter methods will silently be ignored.
	 * <br>Otherwise, a {@code BeanRuntimeException} is thrown.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#ignoreInvocationExceptionsOnGetters()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String ignoreInvocationExceptionsOnGetters() default "";

	/**
	 * Ignore invocation errors on setters.
	 *
	 * <p>
	 * If <js>"true"</js>, errors thrown when calling bean setter methods will silently be ignored.
	 * <br>Otherwise, a {@code BeanRuntimeException} is thrown.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#ignoreInvocationExceptionsOnSetters()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String ignoreInvocationExceptionsOnSetters() default "";

	/**
	 * Don't silently ignore missing setters.
	 *
	 * <p>
	 * If <js>"true"</js>, trying to set a value on a bean property without a setter will throw a {@code BeanRuntimeException}.
	 * <br>Otherwise it will be sliently ignored.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#disableIgnoreMissingSetters()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String disableIgnoreMissingSetters() default "";

	/**
	 * Don't ignore transient fields.
	 *
	 * <p>
	 * If <jk>true</jk>, methods and fields marked as <jk>transient</jk> will not be ignored as bean properties.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#disableIgnoreTransientFields()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String disableIgnoreTransientFields() default "";

	/**
	 * Ignore unknown properties.
	 *
	 * <p>
	 * If <js>"true"</js>, trying to set a value on a non-existent bean property will silently be ignored.
	 * <br>Otherwise, a {@code RuntimeException} is thrown.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#ignoreUnknownBeanProperties()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String ignoreUnknownBeanProperties() default "";

	/**
	 * Ignore unknown enum values.
	 *
	 * <p>
	 * If <js>"true"</js>, unknown enum values are set to <jk>null</jk> instead of throwing an exception.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#ignoreUnknownEnumValues()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String ignoreUnknownEnumValues() default "";

	/**
	 * Don't ignore unknown properties with null values.
	 *
	 * <p>
	 * If <js>"true"</js>, trying to set a <jk>null</jk> value on a non-existent bean property will throw a {@code BeanRuntimeException}.
	 * Otherwise it will be silently ignored.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#disableIgnoreUnknownNullBeanProperties()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String disableIgnoreUnknownNullBeanProperties() default "";

	/**
	 * Identifies a set of interfaces.
	 *
	 * <p>
	 * When specified, only the list of properties defined on the interface class will be used during serialization
	 * of implementation classes.  Additional properties on subclasses will be ignored.
	 *
	 * <p class='bjava'>
	 * 	<jc>// Parent class or interface</jc>
	 * 	<jk>public abstract class</jk> A {
	 * 		<jk>public</jk> String <jf>foo</jf> = <js>"foo"</js>;
	 * 	}
	 *
	 * 	<jc>// Sub class</jc>
	 * 	<jk>public class</jk> A1 <jk>extends</jk> A {
	 * 		<jk>public</jk> String <jf>bar</jf> = <js>"bar"</js>;
	 * 	}
	 *
	 * 	<jc>// Apply it to a config</jc>
	 * 	<ja>@BeanConfig</ja>(
	 * 		interfaces={
	 * 			A.<jk>class</jk>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * <p>
	 * This annotation can be used on the parent class so that it filters to all child classes, or can be set
	 * individually on the child classes.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>The {@link Bean#interfaceClass() @Bean(interfaceClass)} annotation is the equivalent annotation-based solution.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<?>[] interfaces() default {};

	/**
	 * Locale.
	 *
	 * <p>
	 * Specifies the default locale for serializer and parser sessions.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanSession.Builder#locale(Locale)}
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#locale(Locale)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String locale() default "";

	/**
	 * Media type.
	 *
	 * <p>
	 * Specifies the default media type value for serializer and parser sessions.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanSession.Builder#mediaType(MediaType)}
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#mediaType(MediaType)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String mediaType() default "";

	/**
	 * Bean class exclusions.
	 *
	 * <p>
	 * List of classes that should not be treated as beans even if they appear to be bean-like.
	 * <br>Not-bean classes are converted to <c>Strings</c> during serialization.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link BeanIgnore}
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#notBeanClasses(Class...)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<?>[] notBeanClasses() default {};

	/**
	 * Replace classes that should not be considered beans.
	 *
	 * <p>
	 * Same as {@link #notBeanClasses()} but replaces any existing value.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#notBeanClasses(Class...)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<?>[] notBeanClasses_replace() default {};

	/**
	 * Bean package exclusions.
	 *
	 * <p>
	 * When specified, the current list of ignore packages are appended to.
	 *
	 * <p>
	 * Any classes within these packages will be serialized to strings using {@link Object#toString()}.
	 *
	 * <p>
	 * Note that you can specify suffix patterns to include all subpackages.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The default value excludes the following packages:
	 * 		<ul class='compact'>
	 * 			<li class='jp'><c>java.lang</c>
	 * 			<li class='jp'><c>java.lang.annotation</c>
	 * 			<li class='jp'><c>java.lang.ref</c>
	 * 			<li class='jp'><c>java.lang.reflect</c>
	 * 			<li class='jp'><c>java.io</c>
	 * 			<li class='jp'><c>java.net</c>
	 * 			<li class='jp'><c>java.nio.*</c>
	 * 			<li class='jp'><c>java.util.*</c>
	 * 		</ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#notBeanPackages(String...)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] notBeanPackages() default {};

	/**
	 * Replace packages whose classes should not be considered beans.
	 *
	 * <p>
	 * Same as {@link #notBeanPackages()} but replaces any existing value.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#notBeanPackages(String...)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] notBeanPackages_replace() default {};

	/**
	 * Bean property namer.
	 *
	 * <p>
	 * The class to use for calculating bean property names.
	 *
	 * <p>
	 * Predefined classes:
	 * <ul>
	 * 	<li>{@link BasicPropertyNamer} (default)
	 * 	<li>{@link PropertyNamerDLC} - Dashed-lower-case names.
	 * 	<li>{@link PropertyNamerULC} - Dashed-upper-case names.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#propertyNamer(Class)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<? extends PropertyNamer> propertyNamer() default PropertyNamer.Void.class;

	/**
	 * Sort bean properties.
	 *
	 * <p>
	 * When <jk>true</jk>, all bean properties will be serialized and access in alphabetical order.
	 * <br>Otherwise, the natural order of the bean properties is used which is dependent on the JVM vendor.
	 * <br>On IBM JVMs, the bean properties are ordered based on their ordering in the Java file.
	 * <br>On Oracle JVMs, the bean properties are not ordered (which follows the official JVM specs).
	 *
	 * <p>
	 * This property is disabled by default so that IBM JVM users don't have to use {@link Bean @Bean} annotations
	 * to force bean properties to be in a particular order and can just alter the order of the fields/methods
	 * in the Java file.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#sortProperties()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String sortProperties() default "";

	/**
	 * Java object swaps.
	 *
	 * <p>
	 * Swaps are used to "swap out" non-serializable classes with serializable equivalents during serialization,
	 * and "swap in" the non-serializable class during parsing.
	 *
	 * <p>
	 * An example of a swap would be a <c>Calendar</c> object that gets swapped out for an ISO8601 string.
	 *
	 * <p>
	 * Multiple swaps can be associated with a single class.
	 * <br>When multiple swaps are applicable to the same class, the media type pattern defined by
	 * {@link ObjectSwap#forMediaTypes()} or {@link Swap#mediaTypes() @Swap(mediaTypes)} are used to come up with the best match.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#swaps(Class...)}
	 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.Swaps">Swaps</a>
	 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.PerMediaTypeSwaps">Per-media-type Swaps</a>
	 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.OneWaySwaps">One-way Swaps</a>
	 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.SwapAnnotation">@Swap Annotation</a>
	 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.AutoSwaps">Auto-detected swaps</a>
	 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.SurrogateClasses">Surrogate Classes</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<?>[] swaps() default {};

	/**
	 * Replace Java object swap classes.
	 *
	 * <p>
	 * Same as {@link #swaps()} but replaces any existing value.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#swaps(Class...)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<?>[] swaps_replace() default {};

	/**
	 * Time zone.
	 *
	 * <p>
	 * Specifies the default timezone for serializer and parser sessions.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanSession.Builder#timeZone(TimeZone)}
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#timeZone(TimeZone)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String timeZone() default "";

	/**
	 * Use enum names.
	 *
	 * <p>
	 * When enabled, enums are always serialized by name, not using {@link Object#toString()}.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#useEnumNames()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String useEnumNames() default "";

	/**
	 * Don't use interface proxies.
	 *
	 * <p>
	 * Disables the feature where interfaces will be instantiated as proxy classes through the use of an
	 * {@link InvocationHandler} if there is no other way of instantiating them.
	 * <br>Setting this to <js>"true"</js> causes this to be a {@link BeanRuntimeException}.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#disableInterfaceProxies()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String disableInterfaceProxies() default "";

	/**
	 * Use Java Introspector.
	 *
	 * <p>
	 * Using the built-in Java bean introspector will not pick up fields or non-standard getters/setters.
	 * <br>Most {@link Bean @Bean} annotations will be ignored.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#useJavaBeanIntrospector()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String useJavaBeanIntrospector() default "";
}
