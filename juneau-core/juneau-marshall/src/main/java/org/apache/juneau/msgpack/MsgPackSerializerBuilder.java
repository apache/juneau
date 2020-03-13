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
package org.apache.juneau.msgpack;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;

/**
 * Builder class for building instances of MessagePack serializers.
 */
public class MsgPackSerializerBuilder extends OutputStreamSerializerBuilder {

	/**
	 * Constructor, default settings.
	 */
	public MsgPackSerializerBuilder() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param ps The initial configuration settings for this builder.
	 */
	public MsgPackSerializerBuilder(PropertyStore ps) {
		super(ps);
	}

	@Override /* ContextBuilder */
	public MsgPackSerializer build() {
		return build(MsgPackSerializer.class);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	//------------------------------------------------------------------------------------------------------------------
	// <CONFIGURATION-PROPERTIES>

	@Override /* GENERATED - ContextBuilder */
	public MsgPackSerializerBuilder add(Map<String,Object> properties)  {
		super.add(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MsgPackSerializerBuilder addTo(String name, Object value)  {
		super.addTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MsgPackSerializerBuilder addTo(String name, String key, Object value)  {
		super.addTo(name, key, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MsgPackSerializerBuilder apply(PropertyStore copyFrom)  {
		super.apply(copyFrom);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MsgPackSerializerBuilder applyAnnotations(java.lang.Class<?>...fromClasses)  {
		super.applyAnnotations(fromClasses);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MsgPackSerializerBuilder applyAnnotations(Method...fromMethods)  {
		super.applyAnnotations(fromMethods);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MsgPackSerializerBuilder applyAnnotations(AnnotationList al, VarResolverSession r)  {
		super.applyAnnotations(al, r);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MsgPackSerializerBuilder removeFrom(String name, Object value)  {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MsgPackSerializerBuilder set(Map<String,Object> properties)  {
		super.set(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MsgPackSerializerBuilder set(String name, Object value)  {
		super.set(name, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder annotations(Annotation...values)  {
		super.annotations(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder beanClassVisibility(Visibility value)  {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder beanConstructorVisibility(Visibility value)  {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder beanDictionary(java.lang.Class<?>...values)  {
		super.beanDictionary(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder beanDictionary(Object...values)  {
		super.beanDictionary(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder beanDictionaryRemove(java.lang.Class<?>...values)  {
		super.beanDictionaryRemove(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder beanDictionaryRemove(Object...values)  {
		super.beanDictionaryRemove(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder beanDictionaryReplace(java.lang.Class<?>...values)  {
		super.beanDictionaryReplace(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder beanDictionaryReplace(Object...values)  {
		super.beanDictionaryReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder beanFieldVisibility(Visibility value)  {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder beanFilters(java.lang.Class<?>...values)  {
		super.beanFilters(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder beanFilters(Object...values)  {
		super.beanFilters(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder beanFiltersRemove(java.lang.Class<?>...values)  {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder beanFiltersRemove(Object...values)  {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder beanFiltersReplace(java.lang.Class<?>...values)  {
		super.beanFiltersReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder beanFiltersReplace(Object...values)  {
		super.beanFiltersReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder beanMapPutReturnsOldValue()  {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder beanMapPutReturnsOldValue(boolean value)  {
		super.beanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder beanMethodVisibility(Visibility value)  {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder beanTypePropertyName(String value)  {
		super.beanTypePropertyName(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder beansRequireDefaultConstructor()  {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder beansRequireDefaultConstructor(boolean value)  {
		super.beansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder beansRequireSerializable()  {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder beansRequireSerializable(boolean value)  {
		super.beansRequireSerializable(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder beansRequireSettersForGetters()  {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder beansRequireSettersForGetters(boolean value)  {
		super.beansRequireSettersForGetters(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder beansRequireSomeProperties(boolean value)  {
		super.beansRequireSomeProperties(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder bpi(Map<String,String> values)  {
		super.bpi(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder bpi(Class<?> beanClass, String properties)  {
		super.bpi(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder bpi(String beanClassName, String properties)  {
		super.bpi(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder bpro(Map<String,String> values)  {
		super.bpro(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder bpro(Class<?> beanClass, String properties)  {
		super.bpro(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder bpro(String beanClassName, String properties)  {
		super.bpro(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder bpwo(Map<String,String> values)  {
		super.bpwo(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder bpwo(Class<?> beanClass, String properties)  {
		super.bpwo(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder bpwo(String beanClassName, String properties)  {
		super.bpwo(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder bpx(Map<String,String> values)  {
		super.bpx(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder bpx(Class<?> beanClass, String properties)  {
		super.bpx(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder bpx(String beanClassName, String properties)  {
		super.bpx(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder debug()  {
		super.debug();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder debug(boolean value)  {
		super.debug(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder dictionary(java.lang.Class<?>...values)  {
		super.dictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder dictionary(Object...values)  {
		super.dictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder dictionaryRemove(java.lang.Class<?>...values)  {
		super.dictionaryRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder dictionaryRemove(Object...values)  {
		super.dictionaryRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder dictionaryReplace(java.lang.Class<?>...values)  {
		super.dictionaryReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder dictionaryReplace(Object...values)  {
		super.dictionaryReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> MsgPackSerializerBuilder example(Class<T> pojoClass, T o)  {
		super.example(pojoClass, o);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> MsgPackSerializerBuilder exampleJson(Class<T> pojoClass, String json)  {
		super.exampleJson(pojoClass, json);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder examples(String json)  {
		super.examples(json);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder excludeProperties(Map<String,String> values)  {
		super.excludeProperties(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder excludeProperties(Class<?> beanClass, String properties)  {
		super.excludeProperties(beanClass, properties);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder excludeProperties(String beanClassName, String value)  {
		super.excludeProperties(beanClassName, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder fluentSetters()  {
		super.fluentSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder fluentSetters(boolean value)  {
		super.fluentSetters(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder ignoreInvocationExceptionsOnGetters()  {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder ignoreInvocationExceptionsOnGetters(boolean value)  {
		super.ignoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder ignoreInvocationExceptionsOnSetters()  {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder ignoreInvocationExceptionsOnSetters(boolean value)  {
		super.ignoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder ignorePropertiesWithoutSetters(boolean value)  {
		super.ignorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder ignoreTransientFields(boolean value)  {
		super.ignoreTransientFields(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder ignoreUnknownBeanProperties()  {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder ignoreUnknownBeanProperties(boolean value)  {
		super.ignoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder ignoreUnknownNullBeanProperties(boolean value)  {
		super.ignoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder implClass(Class<?> interfaceClass, Class<?> implClass)  {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder implClasses(Map<String,Class<?>> values)  {
		super.implClasses(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder includeProperties(Map<String,String> values)  {
		super.includeProperties(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder includeProperties(Class<?> beanClass, String value)  {
		super.includeProperties(beanClass, value);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder includeProperties(String beanClassName, String value)  {
		super.includeProperties(beanClassName, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder locale(Locale value)  {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder mediaType(MediaType value)  {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder notBeanClasses(java.lang.Class<?>...values)  {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder notBeanClasses(Object...values)  {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder notBeanClassesRemove(java.lang.Class<?>...values)  {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder notBeanClassesRemove(Object...values)  {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder notBeanClassesReplace(java.lang.Class<?>...values)  {
		super.notBeanClassesReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder notBeanClassesReplace(Object...values)  {
		super.notBeanClassesReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder notBeanPackages(Object...values)  {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder notBeanPackages(String...values)  {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder notBeanPackagesRemove(Object...values)  {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder notBeanPackagesRemove(String...values)  {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder notBeanPackagesReplace(Object...values)  {
		super.notBeanPackagesReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder notBeanPackagesReplace(String...values)  {
		super.notBeanPackagesReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder pojoSwaps(java.lang.Class<?>...values)  {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder pojoSwaps(Object...values)  {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder pojoSwapsRemove(java.lang.Class<?>...values)  {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder pojoSwapsRemove(Object...values)  {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder pojoSwapsReplace(java.lang.Class<?>...values)  {
		super.pojoSwapsReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder pojoSwapsReplace(Object...values)  {
		super.pojoSwapsReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value)  {
		super.propertyNamer(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder sortProperties()  {
		super.sortProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder sortProperties(boolean value)  {
		super.sortProperties(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder timeZone(TimeZone value)  {
		super.timeZone(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder useEnumNames()  {
		super.useEnumNames();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder useEnumNames(boolean value)  {
		super.useEnumNames(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder useInterfaceProxies(boolean value)  {
		super.useInterfaceProxies(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder useJavaBeanIntrospector()  {
		super.useJavaBeanIntrospector();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MsgPackSerializerBuilder useJavaBeanIntrospector(boolean value)  {
		super.useJavaBeanIntrospector(value);
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public MsgPackSerializerBuilder detectRecursions()  {
		super.detectRecursions();
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public MsgPackSerializerBuilder detectRecursions(boolean value)  {
		super.detectRecursions(value);
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public MsgPackSerializerBuilder ignoreRecursions()  {
		super.ignoreRecursions();
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public MsgPackSerializerBuilder ignoreRecursions(boolean value)  {
		super.ignoreRecursions(value);
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public MsgPackSerializerBuilder initialDepth(int value)  {
		super.initialDepth(value);
		return this;
	}

	@Override /* GENERATED - BeanTraverseBuilder */
	public MsgPackSerializerBuilder maxDepth(int value)  {
		super.maxDepth(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public MsgPackSerializerBuilder addBeanTypes()  {
		super.addBeanTypes();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public MsgPackSerializerBuilder addBeanTypes(boolean value)  {
		super.addBeanTypes(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public MsgPackSerializerBuilder addRootType()  {
		super.addRootType();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public MsgPackSerializerBuilder addRootType(boolean value)  {
		super.addRootType(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public MsgPackSerializerBuilder listener(Class<? extends org.apache.juneau.serializer.SerializerListener> value)  {
		super.listener(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public MsgPackSerializerBuilder sortCollections()  {
		super.sortCollections();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public MsgPackSerializerBuilder sortCollections(boolean value)  {
		super.sortCollections(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public MsgPackSerializerBuilder sortMaps()  {
		super.sortMaps();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public MsgPackSerializerBuilder sortMaps(boolean value)  {
		super.sortMaps(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public MsgPackSerializerBuilder trimEmptyCollections()  {
		super.trimEmptyCollections();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public MsgPackSerializerBuilder trimEmptyCollections(boolean value)  {
		super.trimEmptyCollections(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public MsgPackSerializerBuilder trimEmptyMaps()  {
		super.trimEmptyMaps();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public MsgPackSerializerBuilder trimEmptyMaps(boolean value)  {
		super.trimEmptyMaps(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public MsgPackSerializerBuilder trimNullProperties(boolean value)  {
		super.trimNullProperties(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public MsgPackSerializerBuilder trimStrings()  {
		super.trimStrings();
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public MsgPackSerializerBuilder trimStrings(boolean value)  {
		super.trimStrings(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public MsgPackSerializerBuilder uriContext(String value)  {
		super.uriContext(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public MsgPackSerializerBuilder uriContext(UriContext value)  {
		super.uriContext(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public MsgPackSerializerBuilder uriRelativity(String value)  {
		super.uriRelativity(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public MsgPackSerializerBuilder uriRelativity(UriRelativity value)  {
		super.uriRelativity(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public MsgPackSerializerBuilder uriResolution(String value)  {
		super.uriResolution(value);
		return this;
	}

	@Override /* GENERATED - SerializerBuilder */
	public MsgPackSerializerBuilder uriResolution(UriResolution value)  {
		super.uriResolution(value);
		return this;
	}

	@Override /* GENERATED - OutputStreamSerializerBuilder */
	public MsgPackSerializerBuilder binaryFormat(String value)  {
		super.binaryFormat(value);
		return this;
	}

	@Override /* GENERATED - OutputStreamSerializerBuilder */
	public MsgPackSerializerBuilder binaryFormat(BinaryFormat value)  {
		super.binaryFormat(value);
		return this;
	}

	// </CONFIGURATION-PROPERTIES>
	//------------------------------------------------------------------------------------------------------------------
}