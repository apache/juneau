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
package org.apache.juneau.jso;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;

/**
 * Builder class for building instances of Java Serialized Object serializers.
 */
public class JsoSerializerBuilder extends OutputStreamSerializerBuilder {

	/**
	 * Constructor, default settings.
	 */
	public JsoSerializerBuilder() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param ps The initial configuration settings for this builder.
	 */
	public JsoSerializerBuilder(PropertyStore ps) {
		super(ps);
	}

	@Override /* ContextBuilder */
	public JsoSerializer build() {
		return build(JsoSerializer.class);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* OutputStreamSerializerBuilder */
	public JsoSerializerBuilder binaryFormat(BinaryFormat value) {
		super.binaryFormat(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public JsoSerializerBuilder addBeanTypes(boolean value) {
		super.addBeanTypes(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public JsoSerializerBuilder addBeanTypes() {
		super.addBeanTypes();
		return this;
	}

	@Override /* SerializerBuilder */
	public JsoSerializerBuilder addRootType(boolean value) {
		super.addRootType(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public JsoSerializerBuilder addRootType() {
		super.addRootType();
		return this;
	}

	@Override /* SerializerBuilder */
	public JsoSerializerBuilder detectRecursions(boolean value) {
		super.detectRecursions(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public JsoSerializerBuilder detectRecursions() {
		super.detectRecursions();
		return this;
	}

	@Override /* SerializerBuilder */
	public JsoSerializerBuilder ignoreRecursions(boolean value) {
		super.ignoreRecursions(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public JsoSerializerBuilder ignoreRecursions() {
		super.ignoreRecursions();
		return this;
	}
	@Override /* SerializerBuilder */
	public JsoSerializerBuilder initialDepth(int value) {
		super.initialDepth(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public JsoSerializerBuilder listener(Class<? extends SerializerListener> value) {
		super.listener(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public JsoSerializerBuilder maxDepth(int value) {
		super.maxDepth(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public JsoSerializerBuilder sortCollections(boolean value) {
		super.sortCollections(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public JsoSerializerBuilder sortCollections() {
		super.sortCollections();
		return this;
	}

	@Override /* SerializerBuilder */
	public JsoSerializerBuilder sortMaps(boolean value) {
		super.sortMaps(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public JsoSerializerBuilder sortMaps() {
		super.sortMaps();
		return this;
	}

	@Override /* SerializerBuilder */
	public JsoSerializerBuilder trimEmptyCollections(boolean value) {
		super.trimEmptyCollections(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public JsoSerializerBuilder trimEmptyCollections() {
		super.trimEmptyCollections();
		return this;
	}

	@Override /* SerializerBuilder */
	public JsoSerializerBuilder trimEmptyMaps(boolean value) {
		super.trimEmptyMaps(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public JsoSerializerBuilder trimEmptyMaps() {
		super.trimEmptyMaps();
		return this;
	}

	@Override /* SerializerBuilder */
	public JsoSerializerBuilder trimNullProperties(boolean value) {
		super.trimNullProperties(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public JsoSerializerBuilder trimStrings(boolean value) {
		super.trimStrings(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public JsoSerializerBuilder trimStrings() {
		super.trimStrings();
		return this;
	}

	@Override /* SerializerBuilder */
	public JsoSerializerBuilder uriContext(UriContext value) {
		super.uriContext(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public JsoSerializerBuilder uriRelativity(UriRelativity value) {
		super.uriRelativity(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public JsoSerializerBuilder uriResolution(UriResolution value) {
		super.uriResolution(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	@Deprecated
	public JsoSerializerBuilder beanDictionary(Class<?>...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	@Deprecated
	public JsoSerializerBuilder beanDictionary(Object...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	@Deprecated
	public JsoSerializerBuilder beanDictionaryReplace(Class<?>...values) {
		super.beanDictionaryReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	@Deprecated
	public JsoSerializerBuilder beanDictionaryReplace(Object...values) {
		super.beanDictionaryReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	@Deprecated
	public JsoSerializerBuilder beanDictionaryRemove(Class<?>...values) {
		super.beanDictionaryRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	@Deprecated
	public JsoSerializerBuilder beanDictionaryRemove(Object...values) {
		super.beanDictionaryRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder beanFilters(Class<?>...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder beanFilters(Object...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder beanFiltersReplace(Class<?>...values) {
		super.beanFiltersReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder beanFiltersReplace(Object...values) {
		super.beanFiltersReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder beanFiltersRemove(Class<?>...values) {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder beanFiltersRemove(Object...values) {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder beanMapPutReturnsOldValue(boolean value) {
		super.beanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder beanMapPutReturnsOldValue() {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder beansRequireDefaultConstructor(boolean value) {
		super.beansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder beansRequireDefaultConstructor() {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder beansRequireSerializable(boolean value) {
		super.beansRequireSerializable(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder beansRequireSerializable() {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder beansRequireSettersForGetters(boolean value) {
		super.beansRequireSettersForGetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder beansRequireSettersForGetters() {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder beansRequireSomeProperties(boolean value) {
		super.beansRequireSomeProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder beanTypePropertyName(String value) {
		super.beanTypePropertyName(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder bpi(Class<?> beanClass, String value) {
		super.bpi(beanClass, value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder bpi(Map<String,String> values) {
		super.bpi(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder bpi(String beanClassName, String value) {
		super.bpi(beanClassName, value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder bpx(Class<?> beanClass, String properties) {
		super.bpx(beanClass, properties);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder bpx(Map<String,String> values) {
		super.bpx(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder bpx(String beanClassName, String value) {
		super.bpx(beanClassName, value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder bpro(Class<?> beanClass, String value) {
		super.bpro(beanClass, value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder bpro(Map<String,String> values) {
		super.bpro(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder bpro(String beanClassName, String value) {
		super.bpro(beanClassName, value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder bpwo(Class<?> beanClass, String properties) {
		super.bpwo(beanClass, properties);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder bpwo(Map<String,String> values) {
		super.bpwo(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder bpwo(String beanClassName, String value) {
		super.bpwo(beanClassName, value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder dictionary(Class<?>...values) {
		super.dictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder dictionary(Object...values) {
		super.dictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder dictionaryReplace(Class<?>...values) {
		super.dictionaryReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder dictionaryReplace(Object...values) {
		super.dictionaryReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder dictionaryRemove(Class<?>...values) {
		super.dictionaryRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder dictionaryRemove(Object...values) {
		super.dictionaryRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public <T> JsoSerializerBuilder example(Class<T> c, T o) {
		super.example(c, o);
		return this;
	}

	@Override /* BeanContextBuilder */
	public <T> JsoSerializerBuilder exampleJson(Class<T> c, String value) {
		super.exampleJson(c, value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
		super.ignoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder ignoreInvocationExceptionsOnGetters() {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
		super.ignoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder ignoreInvocationExceptionsOnSetters() {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder ignorePropertiesWithoutSetters(boolean value) {
		super.ignorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder ignoreUnknownBeanProperties(boolean value) {
		super.ignoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder ignoreUnknownBeanProperties() {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder ignoreUnknownNullBeanProperties(boolean value) {
		super.ignoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder implClass(Class<?> interfaceClass, Class<?> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder implClasses(Map<String,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder notBeanClasses(Class<?>...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder notBeanClassesReplace(Class<?>...values) {
		super.notBeanClassesReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder notBeanClassesReplace(Object...values) {
		super.notBeanClassesReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder notBeanClassesRemove(Class<?>...values) {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder notBeanClassesRemove(Object...values) {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder notBeanPackages(Object...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder notBeanPackages(String...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder notBeanPackagesReplace(String...values) {
		super.notBeanPackagesReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder notBeanPackagesReplace(Object...values) {
		super.notBeanPackagesReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder notBeanPackagesRemove(String...values) {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder notBeanPackagesRemove(Object...values) {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder pojoSwaps(Class<?>...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder pojoSwaps(Object...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder pojoSwapsReplace(Class<?>...values) {
		super.pojoSwapsReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder pojoSwapsReplace(Object...values) {
		super.pojoSwapsReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder pojoSwapsRemove(Class<?>...values) {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder pojoSwapsRemove(Object...values) {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder sortProperties(boolean value) {
		super.sortProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder useEnumNames(boolean value) {
		super.useEnumNames(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder useEnumNames() {
		super.useEnumNames();
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder useInterfaceProxies(boolean value) {
		super.useInterfaceProxies(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder useJavaBeanIntrospector(boolean value) {
		super.useJavaBeanIntrospector(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public JsoSerializerBuilder useJavaBeanIntrospector() {
		super.useJavaBeanIntrospector();
		return this;
	}

	@Override /* ContextBuilder */
	public JsoSerializerBuilder annotations(Annotation...values) {
		super.annotations(values);
		return this;
	}

	@Override /* ContextBuilder */
	public JsoSerializerBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public JsoSerializerBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* ContextBuilder */
	public JsoSerializerBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* ContextBuilder */
	public JsoSerializerBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public JsoSerializerBuilder addTo(String name, String key, Object value) {
		super.addTo(name, key, value);
		return this;
	}

	@Override /* ContextBuilder */
	public JsoSerializerBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public JsoSerializerBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}

	@Override /* ContextBuilder */
	public JsoSerializerBuilder applyAnnotations(AnnotationList al, VarResolverSession vrs) {
		super.applyAnnotations(al, vrs);
		return this;
	}

	@Override /* ContextBuilder */
	public JsoSerializerBuilder applyAnnotations(Class<?> fromClass) {
		super.applyAnnotations(fromClass);
		return this;
	}

	@Override /* ContextBuilder */
	public JsoSerializerBuilder applyAnnotations(Method fromMethod) {
		super.applyAnnotations(fromMethod);
		return this;
	}
}