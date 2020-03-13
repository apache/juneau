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
package org.apache.juneau.urlencoding;

import static org.apache.juneau.urlencoding.UrlEncodingParser.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.uon.*;

/**
 * Builder class for building instances of URL-Encoding parsers.
 */
public class UrlEncodingParserBuilder extends UonParserBuilder {

	/**
	 * Constructor, default settings.
	 */
	public UrlEncodingParserBuilder() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param ps The initial configuration settings for this builder.
	 */
	public UrlEncodingParserBuilder(PropertyStore ps) {
		super(ps);
	}

	@Override /* ContextBuilder */
	public UrlEncodingParser build() {
		return build(UrlEncodingParser.class);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property: Serialize bean property collections/arrays as separate key/value pairs.
	 *
	 * <p>
	 * This is the parser-side equivalent of the {@link UrlEncodingParser#URLENC_expandedParams} setting.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link UrlEncodingParser#URLENC_expandedParams}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public UrlEncodingParserBuilder expandedParams(boolean value) {
		return set(URLENC_expandedParams, value);
	}

	/**
	 * Configuration property: Serialize bean property collections/arrays as separate key/value pairs.
	 *
	 * <p>
	 * Shortcut for calling <code>expandedParams(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link UrlEncodingParser#URLENC_expandedParams}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public UrlEncodingParserBuilder expandedParams() {
		return set(URLENC_expandedParams, true);
	}

	// <CONFIGURATION-PROPERTIES>

	@Override /* GENERATED - ContextBuilder */
	public UrlEncodingParserBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public UrlEncodingParserBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public UrlEncodingParserBuilder addTo(String name, String key, Object value) {
		super.addTo(name, key, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public UrlEncodingParserBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public UrlEncodingParserBuilder applyAnnotations(java.lang.Class<?>...fromClasses) {
		super.applyAnnotations(fromClasses);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public UrlEncodingParserBuilder applyAnnotations(Method...fromMethods) {
		super.applyAnnotations(fromMethods);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public UrlEncodingParserBuilder applyAnnotations(AnnotationList al, VarResolverSession r) {
		super.applyAnnotations(al, r);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public UrlEncodingParserBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public UrlEncodingParserBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public UrlEncodingParserBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder annotations(Annotation...values) {
		super.annotations(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanDictionary(java.lang.Class<?>...values) {
		super.beanDictionary(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanDictionary(Object...values) {
		super.beanDictionary(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanDictionaryRemove(java.lang.Class<?>...values) {
		super.beanDictionaryRemove(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanDictionaryRemove(Object...values) {
		super.beanDictionaryRemove(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanDictionaryReplace(java.lang.Class<?>...values) {
		super.beanDictionaryReplace(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanDictionaryReplace(Object...values) {
		super.beanDictionaryReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanFilters(java.lang.Class<?>...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanFilters(Object...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanFiltersRemove(java.lang.Class<?>...values) {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanFiltersRemove(Object...values) {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanFiltersReplace(java.lang.Class<?>...values) {
		super.beanFiltersReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanFiltersReplace(Object...values) {
		super.beanFiltersReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanMapPutReturnsOldValue() {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanMapPutReturnsOldValue(boolean value) {
		super.beanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beanTypePropertyName(String value) {
		super.beanTypePropertyName(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beansDontRequireSomeProperties() {
		super.beansDontRequireSomeProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beansRequireDefaultConstructor() {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beansRequireDefaultConstructor(boolean value) {
		super.beansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beansRequireSerializable() {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beansRequireSerializable(boolean value) {
		super.beansRequireSerializable(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beansRequireSettersForGetters() {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beansRequireSettersForGetters(boolean value) {
		super.beansRequireSettersForGetters(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder beansRequireSomeProperties(boolean value) {
		super.beansRequireSomeProperties(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder bpi(Map<String,String> values) {
		super.bpi(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder bpi(Class<?> beanClass, String properties) {
		super.bpi(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder bpi(String beanClassName, String properties) {
		super.bpi(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder bpro(Map<String,String> values) {
		super.bpro(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder bpro(Class<?> beanClass, String properties) {
		super.bpro(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder bpro(String beanClassName, String properties) {
		super.bpro(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder bpwo(Map<String,String> values) {
		super.bpwo(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder bpwo(Class<?> beanClass, String properties) {
		super.bpwo(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder bpwo(String beanClassName, String properties) {
		super.bpwo(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder bpx(Map<String,String> values) {
		super.bpx(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder bpx(Class<?> beanClass, String properties) {
		super.bpx(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder bpx(String beanClassName, String properties) {
		super.bpx(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder debug(boolean value) {
		super.debug(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder dictionary(java.lang.Class<?>...values) {
		super.dictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder dictionary(Object...values) {
		super.dictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder dictionaryRemove(java.lang.Class<?>...values) {
		super.dictionaryRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder dictionaryRemove(Object...values) {
		super.dictionaryRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder dictionaryReplace(java.lang.Class<?>...values) {
		super.dictionaryReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder dictionaryReplace(Object...values) {
		super.dictionaryReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder dontIgnorePropertiesWithoutSetters() {
		super.dontIgnorePropertiesWithoutSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder dontIgnoreTransientFields() {
		super.dontIgnoreTransientFields();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder dontIgnoreUnknownNullBeanProperties() {
		super.dontIgnoreUnknownNullBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder dontUseInterfaceProxies() {
		super.dontUseInterfaceProxies();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> UrlEncodingParserBuilder example(Class<T> pojoClass, T o) {
		super.example(pojoClass, o);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> UrlEncodingParserBuilder exampleJson(Class<T> pojoClass, String json) {
		super.exampleJson(pojoClass, json);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder examples(String json) {
		super.examples(json);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder excludeProperties(Map<String,String> values) {
		super.excludeProperties(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder excludeProperties(Class<?> beanClass, String properties) {
		super.excludeProperties(beanClass, properties);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder excludeProperties(String beanClassName, String value) {
		super.excludeProperties(beanClassName, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder fluentSetters() {
		super.fluentSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder fluentSetters(boolean value) {
		super.fluentSetters(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder ignoreInvocationExceptionsOnGetters() {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
		super.ignoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder ignoreInvocationExceptionsOnSetters() {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
		super.ignoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder ignorePropertiesWithoutSetters(boolean value) {
		super.ignorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder ignoreTransientFields(boolean value) {
		super.ignoreTransientFields(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder ignoreUnknownBeanProperties() {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder ignoreUnknownBeanProperties(boolean value) {
		super.ignoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder ignoreUnknownNullBeanProperties(boolean value) {
		super.ignoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder implClass(Class<?> interfaceClass, Class<?> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder implClasses(Map<String,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder includeProperties(Map<String,String> values) {
		super.includeProperties(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder includeProperties(Class<?> beanClass, String value) {
		super.includeProperties(beanClass, value);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder includeProperties(String beanClassName, String value) {
		super.includeProperties(beanClassName, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder notBeanClasses(java.lang.Class<?>...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder notBeanClassesRemove(java.lang.Class<?>...values) {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder notBeanClassesRemove(Object...values) {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder notBeanClassesReplace(java.lang.Class<?>...values) {
		super.notBeanClassesReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder notBeanClassesReplace(Object...values) {
		super.notBeanClassesReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder notBeanPackages(Object...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder notBeanPackages(String...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder notBeanPackagesRemove(Object...values) {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder notBeanPackagesRemove(String...values) {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder notBeanPackagesReplace(Object...values) {
		super.notBeanPackagesReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder notBeanPackagesReplace(String...values) {
		super.notBeanPackagesReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder pojoSwaps(java.lang.Class<?>...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder pojoSwaps(Object...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder pojoSwapsRemove(java.lang.Class<?>...values) {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder pojoSwapsRemove(Object...values) {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder pojoSwapsReplace(java.lang.Class<?>...values) {
		super.pojoSwapsReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder pojoSwapsReplace(Object...values) {
		super.pojoSwapsReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder sortProperties(boolean value) {
		super.sortProperties(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder useEnumNames() {
		super.useEnumNames();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder useEnumNames(boolean value) {
		super.useEnumNames(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder useInterfaceProxies(boolean value) {
		super.useInterfaceProxies(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder useJavaBeanIntrospector() {
		super.useJavaBeanIntrospector();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public UrlEncodingParserBuilder useJavaBeanIntrospector(boolean value) {
		super.useJavaBeanIntrospector(value);
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public UrlEncodingParserBuilder autoCloseStreams() {
		super.autoCloseStreams();
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public UrlEncodingParserBuilder autoCloseStreams(boolean value) {
		super.autoCloseStreams(value);
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public UrlEncodingParserBuilder debugOutputLines(int value) {
		super.debugOutputLines(value);
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public UrlEncodingParserBuilder listener(Class<? extends org.apache.juneau.parser.ParserListener> value) {
		super.listener(value);
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public UrlEncodingParserBuilder strict() {
		super.strict();
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public UrlEncodingParserBuilder strict(boolean value) {
		super.strict(value);
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public UrlEncodingParserBuilder trimStrings() {
		super.trimStrings();
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public UrlEncodingParserBuilder trimStrings(boolean value) {
		super.trimStrings(value);
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public UrlEncodingParserBuilder unbuffered() {
		super.unbuffered();
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public UrlEncodingParserBuilder unbuffered(boolean value) {
		super.unbuffered(value);
		return this;
	}

	@Override /* GENERATED - ReaderParserBuilder */
	public UrlEncodingParserBuilder fileCharset(Charset value) {
		super.fileCharset(value);
		return this;
	}

	@Override /* GENERATED - ReaderParserBuilder */
	public UrlEncodingParserBuilder streamCharset(Charset value) {
		super.streamCharset(value);
		return this;
	}

	@Override /* GENERATED - UonParserBuilder */
	public UrlEncodingParserBuilder decoding() {
		super.decoding();
		return this;
	}

	@Override /* GENERATED - UonParserBuilder */
	public UrlEncodingParserBuilder decoding(boolean value) {
		super.decoding(value);
		return this;
	}

	@Override /* GENERATED - UonParserBuilder */
	public UrlEncodingParserBuilder validateEnd() {
		super.validateEnd();
		return this;
	}

	@Override /* GENERATED - UonParserBuilder */
	public UrlEncodingParserBuilder validateEnd(boolean value) {
		super.validateEnd(value);
		return this;
	}

	// </CONFIGURATION-PROPERTIES>
}