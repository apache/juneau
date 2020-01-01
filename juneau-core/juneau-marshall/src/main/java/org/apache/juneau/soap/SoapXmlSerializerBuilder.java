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
package org.apache.juneau.soap;

import static org.apache.juneau.soap.SoapXmlSerializer.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.xml.*;

/**
 * Builder class for building instances of soap/xml serializers.
 */
public class SoapXmlSerializerBuilder extends XmlSerializerBuilder {

	/**
	 * Constructor, default settings.
	 */
	public SoapXmlSerializerBuilder() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param ps The initial configuration settings for this builder.
	 */
	public SoapXmlSerializerBuilder(PropertyStore ps) {
		super(ps);
	}

	@Override /* ContextBuilder */
	public SoapXmlSerializer build() {
		return build(SoapXmlSerializer.class);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  The <c>SOAPAction</c> HTTP header value to set on responses.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link SoapXmlSerializer#SOAPXML_SOAPAction}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <js>"http://www.w3.org/2003/05/soap-envelope"</js>.
	 * @return This object (for method chaining).
	 */
	public SoapXmlSerializerBuilder soapAction(String value) {
		return set(SOAPXML_SOAPAction, value);
	}

	@Override /* WriterSerializerBuilder */
	public SoapXmlSerializerBuilder fileCharset(Charset value) {
		super.fileCharset(value);
		return this;
	}

	@Override /* WriterSerializerBuilder */
	public SoapXmlSerializerBuilder maxIndent(int value) {
		super.maxIndent(value);
		return this;
	}

	@Override /* WriterSerializerBuilder */
	public SoapXmlSerializerBuilder quoteChar(char value) {
		super.quoteChar(value);
		return this;
	}

	@Override /* WriterSerializerBuilder */
	public SoapXmlSerializerBuilder sq() {
		super.sq();
		return this;
	}

	@Override /* WriterSerializerBuilder */
	public SoapXmlSerializerBuilder streamCharset(Charset value) {
		super.streamCharset(value);
		return this;
	}

	@Override /* WriterSerializerBuilder */
	public SoapXmlSerializerBuilder useWhitespace(boolean value) {
		super.useWhitespace(value);
		return this;
	}

	@Override /* WriterSerializerBuilder */
	public SoapXmlSerializerBuilder useWhitespace() {
		super.useWhitespace();
		return this;
	}

	@Override /* WriterSerializerBuilder */
	public SoapXmlSerializerBuilder ws() {
		super.ws();
		return this;
	}

	@Override /* SerializerBuilder */
	public SoapXmlSerializerBuilder addBeanTypes(boolean value) {
		super.addBeanTypes(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public SoapXmlSerializerBuilder addBeanTypes() {
		super.addBeanTypes();
		return this;
	}

	@Override /* SerializerBuilder */
	public SoapXmlSerializerBuilder addRootType(boolean value) {
		super.addRootType(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public SoapXmlSerializerBuilder addRootType() {
		super.addRootType();
		return this;
	}

	@Override /* SerializerBuilder */
	public SoapXmlSerializerBuilder detectRecursions(boolean value) {
		super.detectRecursions(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public SoapXmlSerializerBuilder detectRecursions() {
		super.detectRecursions();
		return this;
	}

	@Override /* SerializerBuilder */
	public SoapXmlSerializerBuilder ignoreRecursions(boolean value) {
		super.ignoreRecursions(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public SoapXmlSerializerBuilder ignoreRecursions() {
		super.ignoreRecursions();
		return this;
	}
	@Override /* SerializerBuilder */
	public SoapXmlSerializerBuilder initialDepth(int value) {
		super.initialDepth(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public SoapXmlSerializerBuilder listener(Class<? extends SerializerListener> value) {
		super.listener(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public SoapXmlSerializerBuilder maxDepth(int value) {
		super.maxDepth(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public SoapXmlSerializerBuilder sortCollections(boolean value) {
		super.sortCollections(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public SoapXmlSerializerBuilder sortCollections() {
		super.sortCollections();
		return this;
	}

	@Override /* SerializerBuilder */
	public SoapXmlSerializerBuilder sortMaps(boolean value) {
		super.sortMaps(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public SoapXmlSerializerBuilder sortMaps() {
		super.sortMaps();
		return this;
	}

	@Override /* SerializerBuilder */
	public SoapXmlSerializerBuilder trimEmptyCollections(boolean value) {
		super.trimEmptyCollections(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public SoapXmlSerializerBuilder trimEmptyCollections() {
		super.trimEmptyCollections();
		return this;
	}

	@Override /* SerializerBuilder */
	public SoapXmlSerializerBuilder trimEmptyMaps(boolean value) {
		super.trimEmptyMaps(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public SoapXmlSerializerBuilder trimEmptyMaps() {
		super.trimEmptyMaps();
		return this;
	}

	@Override /* SerializerBuilder */
	public SoapXmlSerializerBuilder trimNullProperties(boolean value) {
		super.trimNullProperties(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public SoapXmlSerializerBuilder trimStrings(boolean value) {
		super.trimStrings(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public SoapXmlSerializerBuilder trimStrings() {
		super.trimStrings();
		return this;
	}

	@Override /* SerializerBuilder */
	public SoapXmlSerializerBuilder uriContext(UriContext value) {
		super.uriContext(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public SoapXmlSerializerBuilder uriRelativity(UriRelativity value) {
		super.uriRelativity(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public SoapXmlSerializerBuilder uriResolution(UriResolution value) {
		super.uriResolution(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	@Deprecated
	public SoapXmlSerializerBuilder beanDictionary(Class<?>...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	@Deprecated
	public SoapXmlSerializerBuilder beanDictionary(Object...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	@Deprecated
	public SoapXmlSerializerBuilder beanDictionaryReplace(Class<?>...values) {
		super.beanDictionaryReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	@Deprecated
	public SoapXmlSerializerBuilder beanDictionaryReplace(Object...values) {
		super.beanDictionaryReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	@Deprecated
	public SoapXmlSerializerBuilder beanDictionaryRemove(Class<?>...values) {
		super.beanDictionaryRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	@Deprecated
	public SoapXmlSerializerBuilder beanDictionaryRemove(Object...values) {
		super.beanDictionaryRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder beanFilters(Class<?>...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder beanFilters(Object...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder beanFiltersReplace(Class<?>...values) {
		super.beanFiltersReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder beanFiltersReplace(Object...values) {
		super.beanFiltersReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder beanFiltersRemove(Class<?>...values) {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder beanFiltersRemove(Object...values) {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder beanMapPutReturnsOldValue(boolean value) {
		super.beanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder beanMapPutReturnsOldValue() {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder beansRequireDefaultConstructor(boolean value) {
		super.beansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder beansRequireDefaultConstructor() {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder beansRequireSerializable(boolean value) {
		super.beansRequireSerializable(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder beansRequireSerializable() {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder beansRequireSettersForGetters(boolean value) {
		super.beansRequireSettersForGetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder beansRequireSettersForGetters() {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder beansRequireSomeProperties(boolean value) {
		super.beansRequireSomeProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder beanTypePropertyName(String value) {
		super.beanTypePropertyName(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder bpi(Class<?> beanClass, String value) {
		super.bpi(beanClass, value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder bpi(Map<String,String> values) {
		super.bpi(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder bpi(String beanClassName, String value) {
		super.bpi(beanClassName, value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder bpx(Class<?> beanClass, String properties) {
		super.bpx(beanClass, properties);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder bpx(Map<String,String> values) {
		super.bpx(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder bpx(String beanClassName, String value) {
		super.bpx(beanClassName, value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder bpro(Class<?> beanClass, String value) {
		super.bpro(beanClass, value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder bpro(Map<String,String> values) {
		super.bpro(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder bpro(String beanClassName, String value) {
		super.bpro(beanClassName, value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder bpwo(Class<?> beanClass, String properties) {
		super.bpwo(beanClass, properties);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder bpwo(Map<String,String> values) {
		super.bpwo(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder bpwo(String beanClassName, String value) {
		super.bpwo(beanClassName, value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder dictionary(Class<?>...values) {
		super.dictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder dictionary(Object...values) {
		super.dictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder dictionaryReplace(Class<?>...values) {
		super.dictionaryReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder dictionaryReplace(Object...values) {
		super.dictionaryReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder dictionaryRemove(Class<?>...values) {
		super.dictionaryRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder dictionaryRemove(Object...values) {
		super.dictionaryRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public <T> SoapXmlSerializerBuilder example(Class<T> c, T o) {
		super.example(c, o);
		return this;
	}

	@Override /* BeanContextBuilder */
	public <T> SoapXmlSerializerBuilder exampleJson(Class<T> c, String value) {
		super.exampleJson(c, value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
		super.ignoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder ignoreInvocationExceptionsOnGetters() {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
		super.ignoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder ignoreInvocationExceptionsOnSetters() {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder ignorePropertiesWithoutSetters(boolean value) {
		super.ignorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder ignoreUnknownBeanProperties(boolean value) {
		super.ignoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder ignoreUnknownBeanProperties() {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder ignoreUnknownNullBeanProperties(boolean value) {
		super.ignoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder implClass(Class<?> interfaceClass, Class<?> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder implClasses(Map<String,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder notBeanClasses(Class<?>...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder notBeanClassesReplace(Class<?>...values) {
		super.notBeanClassesReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder notBeanClassesReplace(Object...values) {
		super.notBeanClassesReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder notBeanClassesRemove(Class<?>...values) {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder notBeanClassesRemove(Object...values) {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder notBeanPackages(Object...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder notBeanPackages(String...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder notBeanPackagesReplace(String...values) {
		super.notBeanPackagesReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder notBeanPackagesReplace(Object...values) {
		super.notBeanPackagesReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder notBeanPackagesRemove(String...values) {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder notBeanPackagesRemove(Object...values) {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder pojoSwaps(Class<?>...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder pojoSwaps(Object...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder pojoSwapsReplace(Class<?>...values) {
		super.pojoSwapsReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder pojoSwapsReplace(Object...values) {
		super.pojoSwapsReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder pojoSwapsRemove(Class<?>...values) {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder pojoSwapsRemove(Object...values) {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder sortProperties(boolean value) {
		super.sortProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder useEnumNames(boolean value) {
		super.useEnumNames(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder useEnumNames() {
		super.useEnumNames();
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder useInterfaceProxies(boolean value) {
		super.useInterfaceProxies(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder useJavaBeanIntrospector(boolean value) {
		super.useJavaBeanIntrospector(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public SoapXmlSerializerBuilder useJavaBeanIntrospector() {
		super.useJavaBeanIntrospector();
		return this;
	}

	@Override /* ContextBuilder */
	public SoapXmlSerializerBuilder annotations(Annotation...values) {
		super.annotations(values);
		return this;
	}

	@Override /* ContextBuilder */
	public SoapXmlSerializerBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public SoapXmlSerializerBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* ContextBuilder */
	public SoapXmlSerializerBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* ContextBuilder */
	public SoapXmlSerializerBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public SoapXmlSerializerBuilder addTo(String name, String key, Object value) {
		super.addTo(name, key, value);
		return this;
	}

	@Override /* ContextBuilder */
	public SoapXmlSerializerBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public SoapXmlSerializerBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}

	@Override /* ContextBuilder */
	public SoapXmlSerializerBuilder applyAnnotations(AnnotationList al, VarResolverSession vrs) {
		super.applyAnnotations(al, vrs);
		return this;
	}

	@Override /* ContextBuilder */
	public SoapXmlSerializerBuilder applyAnnotations(Class<?> fromClass) {
		super.applyAnnotations(fromClass);
		return this;
	}

	@Override /* ContextBuilder */
	public SoapXmlSerializerBuilder applyAnnotations(Method fromMethod) {
		super.applyAnnotations(fromMethod);
		return this;
	}
}