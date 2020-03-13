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

import static org.apache.juneau.parser.InputStreamParser.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Base builder class for building instances of stream-based parsers.
 */
public class InputStreamParserBuilder extends ParserBuilder {

	/**
	 * Constructor, default settings.
	 */
	public InputStreamParserBuilder() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param ps The initial configuration settings for this builder.
	 */
	public InputStreamParserBuilder(PropertyStore ps) {
		super(ps);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Binary input format.
	 *
	 * <p>
	 * When using the {@link Parser#parse(Object,Class)} method on stream-based parsers and the input is a string, this defines the format to use
	 * when converting the string into a byte array.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link InputStreamParser#ISPARSER_binaryFormat}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default value is {@link BinaryFormat#HEX}.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public InputStreamParserBuilder binaryFormat(BinaryFormat value) {
		return set(ISPARSER_binaryFormat, value);
	}

	/**
	 * Configuration property:  Binary input format.
	 *
	 * <p>
	 * When using the {@link Parser#parse(Object,Class)} method on stream-based parsers and the input is a string, this defines the format to use
	 * when converting the string into a byte array.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link InputStreamParser#ISPARSER_binaryFormat}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default value is {@link BinaryFormat#HEX}.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public InputStreamParserBuilder binaryFormat(String value) {
		return set(ISPARSER_binaryFormat, BinaryFormat.valueOf(value));
	}

	//------------------------------------------------------------------------------------------------------------------
	// <CONFIGURATION-PROPERTIES>

	@Override /* GENERATED - ContextBuilder */
	public InputStreamParserBuilder add(Map<String,Object> properties)  {
		super.add(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public InputStreamParserBuilder addTo(String name, Object value)  {
		super.addTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public InputStreamParserBuilder addTo(String name, String key, Object value)  {
		super.addTo(name, key, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public InputStreamParserBuilder apply(PropertyStore copyFrom)  {
		super.apply(copyFrom);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public InputStreamParserBuilder applyAnnotations(java.lang.Class<?>...fromClasses)  {
		super.applyAnnotations(fromClasses);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public InputStreamParserBuilder applyAnnotations(Method...fromMethods)  {
		super.applyAnnotations(fromMethods);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public InputStreamParserBuilder applyAnnotations(AnnotationList al, VarResolverSession r)  {
		super.applyAnnotations(al, r);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public InputStreamParserBuilder removeFrom(String name, Object value)  {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public InputStreamParserBuilder set(Map<String,Object> properties)  {
		super.set(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public InputStreamParserBuilder set(String name, Object value)  {
		super.set(name, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder annotations(Annotation...values)  {
		super.annotations(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder beanClassVisibility(Visibility value)  {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder beanConstructorVisibility(Visibility value)  {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder beanDictionary(java.lang.Class<?>...values)  {
		super.beanDictionary(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder beanDictionary(Object...values)  {
		super.beanDictionary(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder beanDictionaryRemove(java.lang.Class<?>...values)  {
		super.beanDictionaryRemove(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder beanDictionaryRemove(Object...values)  {
		super.beanDictionaryRemove(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder beanDictionaryReplace(java.lang.Class<?>...values)  {
		super.beanDictionaryReplace(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder beanDictionaryReplace(Object...values)  {
		super.beanDictionaryReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder beanFieldVisibility(Visibility value)  {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder beanFilters(java.lang.Class<?>...values)  {
		super.beanFilters(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder beanFilters(Object...values)  {
		super.beanFilters(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder beanFiltersRemove(java.lang.Class<?>...values)  {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder beanFiltersRemove(Object...values)  {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder beanFiltersReplace(java.lang.Class<?>...values)  {
		super.beanFiltersReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder beanFiltersReplace(Object...values)  {
		super.beanFiltersReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder beanMapPutReturnsOldValue()  {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder beanMapPutReturnsOldValue(boolean value)  {
		super.beanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder beanMethodVisibility(Visibility value)  {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder beanTypePropertyName(String value)  {
		super.beanTypePropertyName(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder beansRequireDefaultConstructor()  {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder beansRequireDefaultConstructor(boolean value)  {
		super.beansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder beansRequireSerializable()  {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder beansRequireSerializable(boolean value)  {
		super.beansRequireSerializable(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder beansRequireSettersForGetters()  {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder beansRequireSettersForGetters(boolean value)  {
		super.beansRequireSettersForGetters(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder beansRequireSomeProperties(boolean value)  {
		super.beansRequireSomeProperties(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder bpi(Map<String,String> values)  {
		super.bpi(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder bpi(Class<?> beanClass, String properties)  {
		super.bpi(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder bpi(String beanClassName, String properties)  {
		super.bpi(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder bpro(Map<String,String> values)  {
		super.bpro(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder bpro(Class<?> beanClass, String properties)  {
		super.bpro(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder bpro(String beanClassName, String properties)  {
		super.bpro(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder bpwo(Map<String,String> values)  {
		super.bpwo(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder bpwo(Class<?> beanClass, String properties)  {
		super.bpwo(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder bpwo(String beanClassName, String properties)  {
		super.bpwo(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder bpx(Map<String,String> values)  {
		super.bpx(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder bpx(Class<?> beanClass, String properties)  {
		super.bpx(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder bpx(String beanClassName, String properties)  {
		super.bpx(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder debug()  {
		super.debug();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder debug(boolean value)  {
		super.debug(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder dictionary(java.lang.Class<?>...values)  {
		super.dictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder dictionary(Object...values)  {
		super.dictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder dictionaryRemove(java.lang.Class<?>...values)  {
		super.dictionaryRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder dictionaryRemove(Object...values)  {
		super.dictionaryRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder dictionaryReplace(java.lang.Class<?>...values)  {
		super.dictionaryReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder dictionaryReplace(Object...values)  {
		super.dictionaryReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> InputStreamParserBuilder example(Class<T> pojoClass, T o)  {
		super.example(pojoClass, o);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> InputStreamParserBuilder exampleJson(Class<T> pojoClass, String json)  {
		super.exampleJson(pojoClass, json);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder examples(String json)  {
		super.examples(json);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder excludeProperties(Map<String,String> values)  {
		super.excludeProperties(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder excludeProperties(Class<?> beanClass, String properties)  {
		super.excludeProperties(beanClass, properties);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder excludeProperties(String beanClassName, String value)  {
		super.excludeProperties(beanClassName, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder fluentSetters()  {
		super.fluentSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder fluentSetters(boolean value)  {
		super.fluentSetters(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder ignoreInvocationExceptionsOnGetters()  {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder ignoreInvocationExceptionsOnGetters(boolean value)  {
		super.ignoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder ignoreInvocationExceptionsOnSetters()  {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder ignoreInvocationExceptionsOnSetters(boolean value)  {
		super.ignoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder ignorePropertiesWithoutSetters(boolean value)  {
		super.ignorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder ignoreTransientFields(boolean value)  {
		super.ignoreTransientFields(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder ignoreUnknownBeanProperties()  {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder ignoreUnknownBeanProperties(boolean value)  {
		super.ignoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder ignoreUnknownNullBeanProperties(boolean value)  {
		super.ignoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder implClass(Class<?> interfaceClass, Class<?> implClass)  {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder implClasses(Map<String,Class<?>> values)  {
		super.implClasses(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder includeProperties(Map<String,String> values)  {
		super.includeProperties(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder includeProperties(Class<?> beanClass, String value)  {
		super.includeProperties(beanClass, value);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder includeProperties(String beanClassName, String value)  {
		super.includeProperties(beanClassName, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder locale(Locale value)  {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder mediaType(MediaType value)  {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder notBeanClasses(java.lang.Class<?>...values)  {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder notBeanClasses(Object...values)  {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder notBeanClassesRemove(java.lang.Class<?>...values)  {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder notBeanClassesRemove(Object...values)  {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder notBeanClassesReplace(java.lang.Class<?>...values)  {
		super.notBeanClassesReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder notBeanClassesReplace(Object...values)  {
		super.notBeanClassesReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder notBeanPackages(Object...values)  {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder notBeanPackages(String...values)  {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder notBeanPackagesRemove(Object...values)  {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder notBeanPackagesRemove(String...values)  {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder notBeanPackagesReplace(Object...values)  {
		super.notBeanPackagesReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder notBeanPackagesReplace(String...values)  {
		super.notBeanPackagesReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder pojoSwaps(java.lang.Class<?>...values)  {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder pojoSwaps(Object...values)  {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder pojoSwapsRemove(java.lang.Class<?>...values)  {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder pojoSwapsRemove(Object...values)  {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder pojoSwapsReplace(java.lang.Class<?>...values)  {
		super.pojoSwapsReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder pojoSwapsReplace(Object...values)  {
		super.pojoSwapsReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value)  {
		super.propertyNamer(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder sortProperties()  {
		super.sortProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder sortProperties(boolean value)  {
		super.sortProperties(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder timeZone(TimeZone value)  {
		super.timeZone(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder useEnumNames()  {
		super.useEnumNames();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder useEnumNames(boolean value)  {
		super.useEnumNames(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder useInterfaceProxies(boolean value)  {
		super.useInterfaceProxies(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder useJavaBeanIntrospector()  {
		super.useJavaBeanIntrospector();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public InputStreamParserBuilder useJavaBeanIntrospector(boolean value)  {
		super.useJavaBeanIntrospector(value);
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public InputStreamParserBuilder autoCloseStreams()  {
		super.autoCloseStreams();
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public InputStreamParserBuilder autoCloseStreams(boolean value)  {
		super.autoCloseStreams(value);
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public InputStreamParserBuilder debugOutputLines(int value)  {
		super.debugOutputLines(value);
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public InputStreamParserBuilder listener(Class<? extends org.apache.juneau.parser.ParserListener> value)  {
		super.listener(value);
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public InputStreamParserBuilder strict()  {
		super.strict();
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public InputStreamParserBuilder strict(boolean value)  {
		super.strict(value);
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public InputStreamParserBuilder trimStrings()  {
		super.trimStrings();
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public InputStreamParserBuilder trimStrings(boolean value)  {
		super.trimStrings(value);
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public InputStreamParserBuilder unbuffered()  {
		super.unbuffered();
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public InputStreamParserBuilder unbuffered(boolean value)  {
		super.unbuffered(value);
		return this;
	}

	// </CONFIGURATION-PROPERTIES>
	//------------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public InputStreamParser build() {
		return null;
	}
}