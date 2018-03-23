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
package org.apache.juneau.parser;

import static org.apache.juneau.parser.Parser.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;

/**
 * Builder class for building instances of parsers.
 */
public class ParserBuilder extends BeanContextBuilder {

	/**
	 * Constructor, default settings.
	 */
	public ParserBuilder() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param ps The initial configuration settings for this builder.
	 */
	public ParserBuilder(PropertyStore ps) {
		super(ps);
	}

	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	/**
	 * Configuration property:  Auto-close streams.
	 * 
	 * <p>
	 * If <jk>true</jk>, <l>InputStreams</l> and <l>Readers</l> passed into parsers will be closed
	 * after parsing is complete.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Parser#PARSER_autoCloseStreams}
	 * </ul>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default value is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public ParserBuilder autoCloseStreams(boolean value) {
		return set(PARSER_autoCloseStreams, value);
	}

	/**
	 * Configuration property:  Auto-close streams.
	 * 
	 * <p>
	 * Shortcut for calling <code>autoCloseStreams(<jk>true</jk>)</code>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Parser#PARSER_autoCloseStreams}
	 * </ul>
	 * 
	 * @return This object (for method chaining).
	 */
	public ParserBuilder autoCloseStreams() {
		return set(PARSER_autoCloseStreams, true);
	}

	/**
	 * Configuration property:  Parser listener.
	 * 
	 * <p>
	 * Class used to listen for errors and warnings that occur during parsing.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Parser#PARSER_listener}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ParserBuilder listener(Class<? extends ParserListener> value) {
		return set(PARSER_listener, value);
	}

	/**
	 * Configuration property:  Strict mode.
	 * 
	 * <p>
	 * If <jk>true</jk>, strict mode for the parser is enabled.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Parser#PARSER_strict}
	 * </ul>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default value is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public ParserBuilder strict(boolean value) {
		return set(PARSER_strict, value);
	}

	/**
	 * Configuration property:  Strict mode.
	 * 
	 * <p>
	 * Shortcut for calling <code>strict(<jk>true</jk>)</code>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Parser#PARSER_strict}
	 * </ul>
	 * 
	 * @return This object (for method chaining).
	 */
	public ParserBuilder strict() {
		return set(PARSER_strict, true);
	}

	/**
	 * Configuration property:  Trim parsed strings.
	 * 
	 * <p>
	 * If <jk>true</jk>, string values will be trimmed of whitespace using {@link String#trim()} before being added to
	 * the POJO.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Parser#PARSER_trimStrings}
	 * </ul>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default value is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public ParserBuilder trimStrings(boolean value) {
		return set(PARSER_trimStrings, value);
	}

	/**
	 * Configuration property:  Trim parsed strings.
	 * 
	 * <p>
	 * Shortcut for calling <code>trimStrings(<jk>true</jk>)</code>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Parser#PARSER_trimStrings}
	 * </ul>
	 * 
	 * @return This object (for method chaining).
	 */
	public ParserBuilder trimStrings() {
		return set(PARSER_trimStrings, true);
	}

	/**
	 * Configuration property:  Unbuffered.
	 * 
	 * <p>
	 * If <jk>true</jk>, don't use internal buffering during parsing.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Parser#PARSER_unbuffered}
	 * </ul>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>The default value is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public ParserBuilder unbuffered(boolean value) {
		return set(PARSER_unbuffered, value);
	}

	/**
	 * Configuration property:  Unbuffered.
	 * 
	 * <p>
	 * Shortcut for calling <code>unbuffered(<jk>true</jk>)</code>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link Parser#PARSER_unbuffered}
	 * </ul>
	 * 
	 * @return This object (for method chaining).
	 */
	public ParserBuilder unbuffered() {
		return set(PARSER_unbuffered, true);
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder beanDictionary(boolean append, Object...values) {
		super.beanDictionary(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder beanDictionary(Class<?>...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder beanDictionary(Object...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder beanDictionaryRemove(Object...values) {
		super.beanDictionaryRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder beanFilters(boolean append, Object...values) {
		super.beanFilters(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder beanFilters(Class<?>...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder beanFilters(Object...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder beanFiltersRemove(Object...values) {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder beanMapPutReturnsOldValue(boolean value) {
		super.beanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder beanMapPutReturnsOldValue() {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder beansRequireDefaultConstructor(boolean value) {
		super.beansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder beansRequireDefaultConstructor() {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder beansRequireSerializable(boolean value) {
		super.beansRequireSerializable(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder beansRequireSerializable() {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder beansRequireSettersForGetters(boolean value) {
		super.beansRequireSettersForGetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder beansRequireSettersForGetters() {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder beansRequireSomeProperties(boolean value) {
		super.beansRequireSomeProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder beanTypePropertyName(String value) {
		super.beanTypePropertyName(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
		super.ignoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder ignoreInvocationExceptionsOnGetters() {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
		super.ignoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder ignoreInvocationExceptionsOnSetters() {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder ignorePropertiesWithoutSetters(boolean value) {
		super.ignorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder ignoreUnknownBeanProperties(boolean value) {
		super.ignoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder ignoreUnknownBeanProperties() {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder ignoreUnknownNullBeanProperties(boolean value) {
		super.ignoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public <T> ParserBuilder implClass(Class<T> interfaceClass, Class<? extends T> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder implClasses(Map<String,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder notBeanClasses(boolean append, Object...values) {
		super.notBeanClasses(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder notBeanClasses(Class<?>...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder notBeanClassesRemove(Object...values) {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder notBeanPackages(boolean append, Object...values) {
		super.notBeanPackages(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder notBeanPackages(Object...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder notBeanPackages(String...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder notBeanPackagesRemove(Object...values) {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder pojoSwaps(boolean append, Object...values) {
		super.pojoSwaps(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder pojoSwaps(Class<?>...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder pojoSwaps(Object...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder pojoSwapsRemove(Object...values) {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder sortProperties(boolean value) {
		super.sortProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder useInterfaceProxies(boolean value) {
		super.useInterfaceProxies(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder useJavaBeanIntrospector(boolean value) {
		super.useJavaBeanIntrospector(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ParserBuilder useJavaBeanIntrospector() {
		super.useJavaBeanIntrospector();
		return this;
	}

	@Override /* ContextBuilder */
	public ParserBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public ParserBuilder set(boolean append, String name, Object value) {
		super.set(append, name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public ParserBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* ContextBuilder */
	public ParserBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* ContextBuilder */
	public ParserBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public ParserBuilder addTo(String name, String key, Object value) {
		super.addTo(name, key, value);
		return this;
	}

	@Override /* ContextBuilder */
	public ParserBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public ParserBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}
	
	@Override /* Context */
	public Parser build() {
		return null;
	}
}