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
package org.apache.juneau.json;

import static org.apache.juneau.json.JsonParser.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Builder class for building instances of JSON parsers.
 */
public class JsonParserBuilder extends ReaderParserBuilder {

	/**
	 * Constructor, default settings.
	 */
	public JsonParserBuilder() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param ps The initial configuration settings for this builder.
	 */
	public JsonParserBuilder(PropertyStore ps) {
		super(ps);
	}

	@Override /* ContextBuilder */
	public JsonParser build() {
		return build(JsonParser.class);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Validate end.
	 *
	 * <p>
	 * If <jk>true</jk>, after parsing a POJO from the input, verifies that the remaining input in
	 * the stream consists of only comments or whitespace.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link JsonParser#JSON_validateEnd}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default value is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public JsonParserBuilder validateEnd(boolean value) {
		return set(JSON_validateEnd, value);
	}

	/**
	 * Configuration property:  Validate end.
	 *
	 * <p>
	 * Shortcut for calling <code>validateEnd(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link JsonParser#JSON_validateEnd}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@ConfigurationProperty
	public JsonParserBuilder validateEnd() {
		return set(JSON_validateEnd, true);
	}

	//------------------------------------------------------------------------------------------------------------------
	// <CONFIGURATION-PROPERTIES>

	@Override /* GENERATED - ContextBuilder */
	public JsonParserBuilder add(Map<String,Object> properties)  {
		super.add(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonParserBuilder addTo(String name, Object value)  {
		super.addTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonParserBuilder addTo(String name, String key, Object value)  {
		super.addTo(name, key, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonParserBuilder apply(PropertyStore copyFrom)  {
		super.apply(copyFrom);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonParserBuilder applyAnnotations(java.lang.Class<?>...fromClasses)  {
		super.applyAnnotations(fromClasses);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonParserBuilder applyAnnotations(Method...fromMethods)  {
		super.applyAnnotations(fromMethods);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonParserBuilder applyAnnotations(AnnotationList al, VarResolverSession r)  {
		super.applyAnnotations(al, r);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonParserBuilder removeFrom(String name, Object value)  {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonParserBuilder set(Map<String,Object> properties)  {
		super.set(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public JsonParserBuilder set(String name, Object value)  {
		super.set(name, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder annotations(Annotation...values)  {
		super.annotations(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder beanClassVisibility(Visibility value)  {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder beanConstructorVisibility(Visibility value)  {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder beanDictionary(java.lang.Class<?>...values)  {
		super.beanDictionary(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder beanDictionary(Object...values)  {
		super.beanDictionary(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder beanDictionaryRemove(java.lang.Class<?>...values)  {
		super.beanDictionaryRemove(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder beanDictionaryRemove(Object...values)  {
		super.beanDictionaryRemove(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder beanDictionaryReplace(java.lang.Class<?>...values)  {
		super.beanDictionaryReplace(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder beanDictionaryReplace(Object...values)  {
		super.beanDictionaryReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder beanFieldVisibility(Visibility value)  {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder beanFilters(java.lang.Class<?>...values)  {
		super.beanFilters(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder beanFilters(Object...values)  {
		super.beanFilters(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder beanFiltersRemove(java.lang.Class<?>...values)  {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder beanFiltersRemove(Object...values)  {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder beanFiltersReplace(java.lang.Class<?>...values)  {
		super.beanFiltersReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder beanFiltersReplace(Object...values)  {
		super.beanFiltersReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder beanMapPutReturnsOldValue()  {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder beanMapPutReturnsOldValue(boolean value)  {
		super.beanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder beanMethodVisibility(Visibility value)  {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder beanTypePropertyName(String value)  {
		super.beanTypePropertyName(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder beansRequireDefaultConstructor()  {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder beansRequireDefaultConstructor(boolean value)  {
		super.beansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder beansRequireSerializable()  {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder beansRequireSerializable(boolean value)  {
		super.beansRequireSerializable(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder beansRequireSettersForGetters()  {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder beansRequireSettersForGetters(boolean value)  {
		super.beansRequireSettersForGetters(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder beansRequireSomeProperties(boolean value)  {
		super.beansRequireSomeProperties(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder bpi(Map<String,String> values)  {
		super.bpi(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder bpi(Class<?> beanClass, String properties)  {
		super.bpi(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder bpi(String beanClassName, String properties)  {
		super.bpi(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder bpro(Map<String,String> values)  {
		super.bpro(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder bpro(Class<?> beanClass, String properties)  {
		super.bpro(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder bpro(String beanClassName, String properties)  {
		super.bpro(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder bpwo(Map<String,String> values)  {
		super.bpwo(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder bpwo(Class<?> beanClass, String properties)  {
		super.bpwo(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder bpwo(String beanClassName, String properties)  {
		super.bpwo(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder bpx(Map<String,String> values)  {
		super.bpx(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder bpx(Class<?> beanClass, String properties)  {
		super.bpx(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder bpx(String beanClassName, String properties)  {
		super.bpx(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder debug()  {
		super.debug();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder debug(boolean value)  {
		super.debug(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder dictionary(java.lang.Class<?>...values)  {
		super.dictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder dictionary(Object...values)  {
		super.dictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder dictionaryRemove(java.lang.Class<?>...values)  {
		super.dictionaryRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder dictionaryRemove(Object...values)  {
		super.dictionaryRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder dictionaryReplace(java.lang.Class<?>...values)  {
		super.dictionaryReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder dictionaryReplace(Object...values)  {
		super.dictionaryReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> JsonParserBuilder example(Class<T> pojoClass, T o)  {
		super.example(pojoClass, o);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> JsonParserBuilder exampleJson(Class<T> pojoClass, String json)  {
		super.exampleJson(pojoClass, json);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder examples(String json)  {
		super.examples(json);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder excludeProperties(Map<String,String> values)  {
		super.excludeProperties(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder excludeProperties(Class<?> beanClass, String properties)  {
		super.excludeProperties(beanClass, properties);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder excludeProperties(String beanClassName, String value)  {
		super.excludeProperties(beanClassName, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder fluentSetters()  {
		super.fluentSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder fluentSetters(boolean value)  {
		super.fluentSetters(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder ignoreInvocationExceptionsOnGetters()  {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder ignoreInvocationExceptionsOnGetters(boolean value)  {
		super.ignoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder ignoreInvocationExceptionsOnSetters()  {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder ignoreInvocationExceptionsOnSetters(boolean value)  {
		super.ignoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder ignorePropertiesWithoutSetters(boolean value)  {
		super.ignorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder ignoreTransientFields(boolean value)  {
		super.ignoreTransientFields(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder ignoreUnknownBeanProperties()  {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder ignoreUnknownBeanProperties(boolean value)  {
		super.ignoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder ignoreUnknownNullBeanProperties(boolean value)  {
		super.ignoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder implClass(Class<?> interfaceClass, Class<?> implClass)  {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder implClasses(Map<String,Class<?>> values)  {
		super.implClasses(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder includeProperties(Map<String,String> values)  {
		super.includeProperties(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder includeProperties(Class<?> beanClass, String value)  {
		super.includeProperties(beanClass, value);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder includeProperties(String beanClassName, String value)  {
		super.includeProperties(beanClassName, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder locale(Locale value)  {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder mediaType(MediaType value)  {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder notBeanClasses(java.lang.Class<?>...values)  {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder notBeanClasses(Object...values)  {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder notBeanClassesRemove(java.lang.Class<?>...values)  {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder notBeanClassesRemove(Object...values)  {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder notBeanClassesReplace(java.lang.Class<?>...values)  {
		super.notBeanClassesReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder notBeanClassesReplace(Object...values)  {
		super.notBeanClassesReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder notBeanPackages(Object...values)  {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder notBeanPackages(String...values)  {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder notBeanPackagesRemove(Object...values)  {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder notBeanPackagesRemove(String...values)  {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder notBeanPackagesReplace(Object...values)  {
		super.notBeanPackagesReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder notBeanPackagesReplace(String...values)  {
		super.notBeanPackagesReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder pojoSwaps(java.lang.Class<?>...values)  {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder pojoSwaps(Object...values)  {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder pojoSwapsRemove(java.lang.Class<?>...values)  {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder pojoSwapsRemove(Object...values)  {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder pojoSwapsReplace(java.lang.Class<?>...values)  {
		super.pojoSwapsReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder pojoSwapsReplace(Object...values)  {
		super.pojoSwapsReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value)  {
		super.propertyNamer(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder sortProperties()  {
		super.sortProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder sortProperties(boolean value)  {
		super.sortProperties(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder timeZone(TimeZone value)  {
		super.timeZone(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder useEnumNames()  {
		super.useEnumNames();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder useEnumNames(boolean value)  {
		super.useEnumNames(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder useInterfaceProxies(boolean value)  {
		super.useInterfaceProxies(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder useJavaBeanIntrospector()  {
		super.useJavaBeanIntrospector();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public JsonParserBuilder useJavaBeanIntrospector(boolean value)  {
		super.useJavaBeanIntrospector(value);
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public JsonParserBuilder autoCloseStreams()  {
		super.autoCloseStreams();
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public JsonParserBuilder autoCloseStreams(boolean value)  {
		super.autoCloseStreams(value);
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public JsonParserBuilder debugOutputLines(int value)  {
		super.debugOutputLines(value);
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public JsonParserBuilder listener(Class<? extends org.apache.juneau.parser.ParserListener> value)  {
		super.listener(value);
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public JsonParserBuilder strict()  {
		super.strict();
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public JsonParserBuilder strict(boolean value)  {
		super.strict(value);
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public JsonParserBuilder trimStrings()  {
		super.trimStrings();
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public JsonParserBuilder trimStrings(boolean value)  {
		super.trimStrings(value);
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public JsonParserBuilder unbuffered()  {
		super.unbuffered();
		return this;
	}

	@Override /* GENERATED - ParserBuilder */
	public JsonParserBuilder unbuffered(boolean value)  {
		super.unbuffered(value);
		return this;
	}

	@Override /* GENERATED - ReaderParserBuilder */
	public JsonParserBuilder fileCharset(Charset value)  {
		super.fileCharset(value);
		return this;
	}

	@Override /* GENERATED - ReaderParserBuilder */
	public JsonParserBuilder streamCharset(Charset value)  {
		super.streamCharset(value);
		return this;
	}

	// </CONFIGURATION-PROPERTIES>
	//------------------------------------------------------------------------------------------------------------------
}