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
package org.apache.juneau.oapi;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.uon.*;

/**
 * Builder class for building instances of {@link OpenApiParser}.
 */
public class OpenApiParserBuilder extends UonParserBuilder {

	/**
	 * Constructor, default settings.
	 */
	public OpenApiParserBuilder() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param ps The initial configuration settings for this builder.
	 */
	public OpenApiParserBuilder(PropertyStore ps) {
		super(ps);
	}

	@Override /* ContextBuilder */
	public OpenApiParser build() {
		return build(OpenApiParser.class);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	//------------------------------------------------------------------------------------------------------------------
	// <CONFIGURATION-PROPERTIES>

	@Override /* GENERATED - ContextBuilder */
	public OpenApiParserBuilder add(Map<String,Object> properties)  {
		super.add(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public OpenApiParserBuilder addTo(String name, Object value)  {
		super.addTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public OpenApiParserBuilder addTo(String name, String key, Object value)  {
		super.addTo(name, key, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public OpenApiParserBuilder apply(PropertyStore copyFrom)  {
		super.apply(copyFrom);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public OpenApiParserBuilder applyAnnotations(java.lang.Class<?>...fromClasses)  {
		super.applyAnnotations(fromClasses);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public OpenApiParserBuilder applyAnnotations(Method...fromMethods)  {
		super.applyAnnotations(fromMethods);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public OpenApiParserBuilder applyAnnotations(AnnotationList al, VarResolverSession r)  {
		super.applyAnnotations(al, r);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public OpenApiParserBuilder removeFrom(String name, Object value)  {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public OpenApiParserBuilder set(Map<String,Object> properties)  {
		super.set(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public OpenApiParserBuilder set(String name, Object value)  {
		super.set(name, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder annotations(Annotation...values)  {
		super.annotations(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanClassVisibility(Visibility value)  {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanConstructorVisibility(Visibility value)  {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanDictionary(java.lang.Class<?>...values)  {
		super.beanDictionary(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanDictionary(Object...values)  {
		super.beanDictionary(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanDictionaryRemove(java.lang.Class<?>...values)  {
		super.beanDictionaryRemove(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanDictionaryRemove(Object...values)  {
		super.beanDictionaryRemove(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanDictionaryReplace(java.lang.Class<?>...values)  {
		super.beanDictionaryReplace(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanDictionaryReplace(Object...values)  {
		super.beanDictionaryReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanFieldVisibility(Visibility value)  {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanFilters(java.lang.Class<?>...values)  {
		super.beanFilters(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanFilters(Object...values)  {
		super.beanFilters(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanFiltersRemove(java.lang.Class<?>...values)  {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanFiltersRemove(Object...values)  {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanFiltersReplace(java.lang.Class<?>...values)  {
		super.beanFiltersReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanFiltersReplace(Object...values)  {
		super.beanFiltersReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanMapPutReturnsOldValue()  {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanMapPutReturnsOldValue(boolean value)  {
		super.beanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanMethodVisibility(Visibility value)  {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beanTypePropertyName(String value)  {
		super.beanTypePropertyName(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beansRequireDefaultConstructor()  {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beansRequireDefaultConstructor(boolean value)  {
		super.beansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beansRequireSerializable()  {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beansRequireSerializable(boolean value)  {
		super.beansRequireSerializable(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beansRequireSettersForGetters()  {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beansRequireSettersForGetters(boolean value)  {
		super.beansRequireSettersForGetters(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder beansRequireSomeProperties(boolean value)  {
		super.beansRequireSomeProperties(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder bpi(Map<String,String> values)  {
		super.bpi(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder bpi(Class<?> beanClass, String properties)  {
		super.bpi(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder bpi(String beanClassName, String properties)  {
		super.bpi(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder bpro(Map<String,String> values)  {
		super.bpro(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder bpro(Class<?> beanClass, String properties)  {
		super.bpro(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder bpro(String beanClassName, String properties)  {
		super.bpro(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder bpwo(Map<String,String> values)  {
		super.bpwo(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder bpwo(Class<?> beanClass, String properties)  {
		super.bpwo(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder bpwo(String beanClassName, String properties)  {
		super.bpwo(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder bpx(Map<String,String> values)  {
		super.bpx(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder bpx(Class<?> beanClass, String properties)  {
		super.bpx(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder bpx(String beanClassName, String properties)  {
		super.bpx(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder debug()  {
		super.debug();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder debug(boolean value)  {
		super.debug(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder dictionary(java.lang.Class<?>...values)  {
		super.dictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder dictionary(Object...values)  {
		super.dictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder dictionaryRemove(java.lang.Class<?>...values)  {
		super.dictionaryRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder dictionaryRemove(Object...values)  {
		super.dictionaryRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder dictionaryReplace(java.lang.Class<?>...values)  {
		super.dictionaryReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder dictionaryReplace(Object...values)  {
		super.dictionaryReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> OpenApiParserBuilder example(Class<T> pojoClass, T o)  {
		super.example(pojoClass, o);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> OpenApiParserBuilder exampleJson(Class<T> pojoClass, String json)  {
		super.exampleJson(pojoClass, json);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder examples(String json)  {
		super.examples(json);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder excludeProperties(Map<String,String> values)  {
		super.excludeProperties(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder excludeProperties(Class<?> beanClass, String properties)  {
		super.excludeProperties(beanClass, properties);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder excludeProperties(String beanClassName, String value)  {
		super.excludeProperties(beanClassName, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder fluentSetters()  {
		super.fluentSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder fluentSetters(boolean value)  {
		super.fluentSetters(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder ignoreInvocationExceptionsOnGetters()  {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder ignoreInvocationExceptionsOnGetters(boolean value)  {
		super.ignoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder ignoreInvocationExceptionsOnSetters()  {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder ignoreInvocationExceptionsOnSetters(boolean value)  {
		super.ignoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder ignorePropertiesWithoutSetters(boolean value)  {
		super.ignorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder ignoreTransientFields(boolean value)  {
		super.ignoreTransientFields(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder ignoreUnknownBeanProperties()  {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder ignoreUnknownBeanProperties(boolean value)  {
		super.ignoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder ignoreUnknownNullBeanProperties(boolean value)  {
		super.ignoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder implClass(Class<?> interfaceClass, Class<?> implClass)  {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder implClasses(Map<String,Class<?>> values)  {
		super.implClasses(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder includeProperties(Map<String,String> values)  {
		super.includeProperties(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder includeProperties(Class<?> beanClass, String value)  {
		super.includeProperties(beanClass, value);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder includeProperties(String beanClassName, String value)  {
		super.includeProperties(beanClassName, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder locale(Locale value)  {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder mediaType(MediaType value)  {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder notBeanClasses(java.lang.Class<?>...values)  {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder notBeanClasses(Object...values)  {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder notBeanClassesRemove(java.lang.Class<?>...values)  {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder notBeanClassesRemove(Object...values)  {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder notBeanClassesReplace(java.lang.Class<?>...values)  {
		super.notBeanClassesReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder notBeanClassesReplace(Object...values)  {
		super.notBeanClassesReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder notBeanPackages(Object...values)  {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder notBeanPackages(String...values)  {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder notBeanPackagesRemove(Object...values)  {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder notBeanPackagesRemove(String...values)  {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder notBeanPackagesReplace(Object...values)  {
		super.notBeanPackagesReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder notBeanPackagesReplace(String...values)  {
		super.notBeanPackagesReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder pojoSwaps(java.lang.Class<?>...values)  {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder pojoSwaps(Object...values)  {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder pojoSwapsRemove(java.lang.Class<?>...values)  {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder pojoSwapsRemove(Object...values)  {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder pojoSwapsReplace(java.lang.Class<?>...values)  {
		super.pojoSwapsReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder pojoSwapsReplace(Object...values)  {
		super.pojoSwapsReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value)  {
		super.propertyNamer(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder sortProperties()  {
		super.sortProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder sortProperties(boolean value)  {
		super.sortProperties(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder timeZone(TimeZone value)  {
		super.timeZone(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder useEnumNames()  {
		super.useEnumNames();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder useEnumNames(boolean value)  {
		super.useEnumNames(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder useInterfaceProxies(boolean value)  {
		super.useInterfaceProxies(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder useJavaBeanIntrospector()  {
		super.useJavaBeanIntrospector();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public OpenApiParserBuilder useJavaBeanIntrospector(boolean value)  {
		super.useJavaBeanIntrospector(value);
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public OpenApiParserBuilder autoCloseStreams()  {
		super.autoCloseStreams();
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public OpenApiParserBuilder autoCloseStreams(boolean value)  {
		super.autoCloseStreams(value);
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public OpenApiParserBuilder debugOutputLines(int value)  {
		super.debugOutputLines(value);
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public OpenApiParserBuilder listener(Class<? extends org.apache.juneau.parser.ParserListener> value)  {
		super.listener(value);
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public OpenApiParserBuilder strict()  {
		super.strict();
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public OpenApiParserBuilder strict(boolean value)  {
		super.strict(value);
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public OpenApiParserBuilder trimStrings()  {
		super.trimStrings();
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public OpenApiParserBuilder trimStrings(boolean value)  {
		super.trimStrings(value);
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public OpenApiParserBuilder unbuffered()  {
		super.unbuffered();
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public OpenApiParserBuilder unbuffered(boolean value)  {
		super.unbuffered(value);
		return this;
	}

	@Override /* GENERATED - ReaderParserBuilder */
	public OpenApiParserBuilder fileCharset(Charset value)  {
		super.fileCharset(value);
		return this;
	}

	@Override /* GENERATED - ReaderParserBuilder */
	public OpenApiParserBuilder streamCharset(Charset value)  {
		super.streamCharset(value);
		return this;
	}

	@Override /* GENERATED - UonParserBuilder */
	public OpenApiParserBuilder decoding()  {
		super.decoding();
		return this;
	}

	@Override /* GENERATED - UonParserBuilder */
	public OpenApiParserBuilder decoding(boolean value)  {
		super.decoding(value);
		return this;
	}

	@Override /* GENERATED - UonParserBuilder */
	public OpenApiParserBuilder validateEnd()  {
		super.validateEnd();
		return this;
	}

	@Override /* GENERATED - UonParserBuilder */
	public OpenApiParserBuilder validateEnd(boolean value)  {
		super.validateEnd(value);
		return this;
	}

	// </CONFIGURATION-PROPERTIES>
	//------------------------------------------------------------------------------------------------------------------
}