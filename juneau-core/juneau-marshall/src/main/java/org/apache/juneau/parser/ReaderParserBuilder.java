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

import static org.apache.juneau.parser.ReaderParser.*;

import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Base builder class for building instances of reader-based parsers.
 */
public abstract class ReaderParserBuilder extends ParserBuilder {

	/**
	 * Constructor, default settings.
	 */
	public ReaderParserBuilder() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param ps The initial configuration settings for this builder.
	 */
	public ReaderParserBuilder(PropertyStore ps) {
		super(ps);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  File charset.
	 *
	 * <p>
	 * The character set to use for reading <c>Files</c> from the file system.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link ReaderParser#RPARSER_fileCharset}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default value is <js>"DEFAULT"</js> which causes the system default to be used.
	 * @return This object (for method chaining).
	 */
	public ReaderParserBuilder fileCharset(Charset value) {
		return set(RPARSER_fileCharset, value);
	}

	/**
	 * Configuration property:  Input stream charset.
	 *
	 * <p>
	 * The character set to use for converting <c>InputStreams</c> and byte arrays to readers.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link ReaderParser#RPARSER_streamCharset}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default value is <js>"UTF-8"</js>.
	 * @return This object (for method chaining).
	 */
	public ReaderParserBuilder streamCharset(Charset value) {
		return set(RPARSER_streamCharset, value);
	}

	@Override /* ParserBuilder */
	public ReaderParserBuilder autoCloseStreams(boolean value) {
		super.autoCloseStreams(value);
		return this;
	}

	@Override /* ParserBuilder */
	public ReaderParserBuilder autoCloseStreams() {
		super.autoCloseStreams();
		return this;
	}

	@Override /* ParserBuilder */
	public ReaderParserBuilder debugOutputLines(int value) {
		super.debugOutputLines(value);
		return this;
	}

	@Override /* ParserBuilder */
	public ReaderParserBuilder listener(Class<? extends ParserListener> value) {
		super.listener(value);
		return this;
	}

	@Override /* ParserBuilder */
	public ReaderParserBuilder strict(boolean value) {
		super.strict(value);
		return this;
	}

	@Override /* ParserBuilder */
	public ReaderParserBuilder strict() {
		super.strict();
		return this;
	}

	@Override /* ParserBuilder */
	public ReaderParserBuilder trimStrings(boolean value) {
		super.trimStrings(value);
		return this;
	}

	@Override /* ParserBuilder */
	public ReaderParserBuilder trimStrings() {
		super.trimStrings();
		return this;
	}

	@Override /* ParserBuilder */
	public ReaderParserBuilder unbuffered(boolean value) {
		super.unbuffered(value);
		return this;
	}

	@Override /* ParserBuilder */
	public ReaderParserBuilder unbuffered() {
		super.unbuffered();
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder beanDictionary(Class<?>...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder beanDictionary(Object...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder beanDictionaryReplace(Class<?>...values) {
		super.beanDictionaryReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder beanDictionaryReplace(Object...values) {
		super.beanDictionaryReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder beanDictionaryRemove(Class<?>...values) {
		super.beanDictionaryRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder beanDictionaryRemove(Object...values) {
		super.beanDictionaryRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder beanFilters(Class<?>...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder beanFilters(Object...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder beanFiltersReplace(Class<?>...values) {
		super.beanFiltersReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder beanFiltersReplace(Object...values) {
		super.beanFiltersReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder beanFiltersRemove(Class<?>...values) {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder beanFiltersRemove(Object...values) {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder beanMapPutReturnsOldValue(boolean value) {
		super.beanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder beanMapPutReturnsOldValue() {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder beansRequireDefaultConstructor(boolean value) {
		super.beansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder beansRequireDefaultConstructor() {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder beansRequireSerializable(boolean value) {
		super.beansRequireSerializable(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder beansRequireSerializable() {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder beansRequireSettersForGetters(boolean value) {
		super.beansRequireSettersForGetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder beansRequireSettersForGetters() {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder beansRequireSomeProperties(boolean value) {
		super.beansRequireSomeProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder beanTypePropertyName(String value) {
		super.beanTypePropertyName(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* BeanContextBuilder */
	public <T> ReaderParserBuilder example(Class<T> c, T o) {
		super.example(c, o);
		return this;
	}

	@Override /* BeanContextBuilder */
	public <T> ReaderParserBuilder exampleJson(Class<T> c, String value) {
		super.exampleJson(c, value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
		super.ignoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder ignoreInvocationExceptionsOnGetters() {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
		super.ignoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder ignoreInvocationExceptionsOnSetters() {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder ignorePropertiesWithoutSetters(boolean value) {
		super.ignorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder ignoreUnknownBeanProperties(boolean value) {
		super.ignoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder ignoreUnknownBeanProperties() {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder ignoreUnknownNullBeanProperties(boolean value) {
		super.ignoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder implClass(Class<?> interfaceClass, Class<?> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder implClasses(Map<String,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder notBeanClasses(Class<?>...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder notBeanClassesReplace(Class<?>...values) {
		super.notBeanClassesReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder notBeanClassesReplace(Object...values) {
		super.notBeanClassesReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder notBeanClassesRemove(Class<?>...values) {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder notBeanClassesRemove(Object...values) {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder notBeanPackages(Object...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder notBeanPackages(String...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder notBeanPackagesReplace(String...values) {
		super.notBeanPackagesReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder notBeanPackagesReplace(Object...values) {
		super.notBeanPackagesReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder notBeanPackagesRemove(String...values) {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder notBeanPackagesRemove(Object...values) {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder pojoSwaps(Class<?>...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder pojoSwaps(Object...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder pojoSwapsReplace(Class<?>...values) {
		super.pojoSwapsReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder pojoSwapsReplace(Object...values) {
		super.pojoSwapsReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder pojoSwapsRemove(Class<?>...values) {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder pojoSwapsRemove(Object...values) {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder sortProperties(boolean value) {
		super.sortProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder useEnumNames(boolean value) {
		super.useEnumNames(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder useEnumNames() {
		super.useEnumNames();
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder useInterfaceProxies(boolean value) {
		super.useInterfaceProxies(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder useJavaBeanIntrospector(boolean value) {
		super.useJavaBeanIntrospector(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public ReaderParserBuilder useJavaBeanIntrospector() {
		super.useJavaBeanIntrospector();
		return this;
	}

	@Override /* ContextBuilder */
	public ReaderParserBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public ReaderParserBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* ContextBuilder */
	public ReaderParserBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* ContextBuilder */
	public ReaderParserBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public ReaderParserBuilder addTo(String name, String key, Object value) {
		super.addTo(name, key, value);
		return this;
	}

	@Override /* ContextBuilder */
	public ReaderParserBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public ReaderParserBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}

	@Override /* ContextBuilder */
	public ReaderParserBuilder applyAnnotations(AnnotationList al, VarResolverSession vrs) {
		super.applyAnnotations(al, vrs);
		return this;
	}

	@Override /* ContextBuilder */
	public ReaderParserBuilder applyAnnotations(Class<?> fromClass) {
		super.applyAnnotations(fromClass);
		return this;
	}

	@Override /* ContextBuilder */
	public ReaderParserBuilder applyAnnotations(Method fromMethod) {
		super.applyAnnotations(fromMethod);
		return this;
	}
}
