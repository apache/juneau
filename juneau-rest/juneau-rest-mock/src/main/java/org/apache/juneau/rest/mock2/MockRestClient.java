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
package org.apache.juneau.rest.mock2;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.*;

import java.lang.annotation.*;
import java.lang.reflect.Method;

import org.apache.http.conn.*;
import org.apache.http.impl.client.*;
import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client2.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;

/**
 * Mocked {@link RestClient}.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-mock}
 * </ul>
 */
public class MockRestClient extends RestClientBuilder {

	private MockRest.Builder mrb;

	/**
	 * Constructor.
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link Rest @Rest}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 */
	protected MockRestClient(Object impl) {
		super(null);
		mrb = MockRest.create(impl);
		rootUrl("http://localhost");
	}

	/**
	 * Creates a new {@link RestClientBuilder} configured with the specified REST implementation bean or bean class.
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link Rest @Rest}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @return A new builder.
	 */
	public static MockRestClient create(Object impl) {
		return new MockRestClient(impl);
	}

	/**
	 * Creates a new {@link RestClient} with no registered serializer or parser.
	 *
	 * <p>
	 * Equivalent to calling:
	 * <p class='bcode w800'>
	 * 	MockRestClient.create(impl).build();
	 * </p>
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link Rest @Rest}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @return A new builder.
	 */
	public static RestClient build(Object impl) {
		return create(impl).build();
	}

	/**
	 * Creates a new {@link RestClient} with JSON marshalling support.
	 *
	 * <p>
	 * Equivalent to calling:
	 * <p class='bcode w800'>
	 * 	MockRestClient.create(impl).json().build();
	 * </p>
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link Rest @Rest}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @return A new builder.
	 */
	public static RestClient buildJson(Object impl) {
		return create(impl).json().build();
	}

	/**
	 * Creates a new {@link RestClient} with Simplified-JSON marshalling support.
	 *
	 * <p>
	 * Equivalent to calling:
	 * <p class='bcode w800'>
	 * 	MockRestClient.create(impl).json().build();
	 * </p>
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link Rest @Rest}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @return A new builder.
	 */
	public static RestClient buildSimpleJson(Object impl) {
		return create(impl).simpleJson().build();
	}

	@Override
	public RestClient build() {
		if (peek(BeanContext.BEAN_debug) == Boolean.TRUE)
			mrb.debug();
		httpClientConnectionManager(new MockHttpClientConnectionManager(mrb.build()));
		return super.build();
	}

	// <CONFIGURATION-PROPERTIES>

	@Override /* GENERATED - ContextBuilder */
	public MockRestClient add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MockRestClient addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MockRestClient appendTo(String name, Object value) {
		super.appendTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MockRestClient apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MockRestClient applyAnnotations(java.lang.Class<?>...fromClasses) {
		super.applyAnnotations(fromClasses);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MockRestClient applyAnnotations(Method...fromMethods) {
		super.applyAnnotations(fromMethods);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MockRestClient applyAnnotations(AnnotationList al, VarResolverSession r) {
		super.applyAnnotations(al, r);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MockRestClient prependTo(String name, Object value) {
		super.prependTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MockRestClient putAllTo(String name, Object value) {
		super.putAllTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MockRestClient putTo(String name, String key, Object value) {
		super.putTo(name, key, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MockRestClient removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MockRestClient set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MockRestClient set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient annotations(Annotation...values) {
		super.annotations(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public MockRestClient beanDictionary(java.lang.Class<?>...values) {
		super.beanDictionary(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public MockRestClient beanDictionary(Object...values) {
		super.beanDictionary(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public MockRestClient beanDictionaryRemove(java.lang.Class<?>...values) {
		super.beanDictionaryRemove(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public MockRestClient beanDictionaryRemove(Object...values) {
		super.beanDictionaryRemove(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public MockRestClient beanDictionaryReplace(java.lang.Class<?>...values) {
		super.beanDictionaryReplace(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public MockRestClient beanDictionaryReplace(Object...values) {
		super.beanDictionaryReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient beanFilters(java.lang.Class<?>...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient beanFilters(Object...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient beanFiltersRemove(java.lang.Class<?>...values) {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient beanFiltersRemove(Object...values) {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient beanFiltersReplace(java.lang.Class<?>...values) {
		super.beanFiltersReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient beanFiltersReplace(Object...values) {
		super.beanFiltersReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient beanMapPutReturnsOldValue() {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient beanMapPutReturnsOldValue(boolean value) {
		super.beanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient beanTypePropertyName(String value) {
		super.beanTypePropertyName(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient beansDontRequireSomeProperties() {
		super.beansDontRequireSomeProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient beansRequireDefaultConstructor() {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient beansRequireDefaultConstructor(boolean value) {
		super.beansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient beansRequireSerializable() {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient beansRequireSerializable(boolean value) {
		super.beansRequireSerializable(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient beansRequireSettersForGetters() {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient beansRequireSettersForGetters(boolean value) {
		super.beansRequireSettersForGetters(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient beansRequireSomeProperties(boolean value) {
		super.beansRequireSomeProperties(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient bpi(Map<String,String> values) {
		super.bpi(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient bpi(Class<?> beanClass, String properties) {
		super.bpi(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient bpi(String beanClassName, String properties) {
		super.bpi(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient bpro(Map<String,String> values) {
		super.bpro(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient bpro(Class<?> beanClass, String properties) {
		super.bpro(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient bpro(String beanClassName, String properties) {
		super.bpro(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient bpwo(Map<String,String> values) {
		super.bpwo(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient bpwo(Class<?> beanClass, String properties) {
		super.bpwo(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient bpwo(String beanClassName, String properties) {
		super.bpwo(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient bpx(Map<String,String> values) {
		super.bpx(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient bpx(Class<?> beanClass, String properties) {
		super.bpx(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient bpx(String beanClassName, String properties) {
		super.bpx(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient debug() {
		super.debug();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient debug(boolean value) {
		super.debug(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient dictionary(java.lang.Class<?>...values) {
		super.dictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient dictionary(Object...values) {
		super.dictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient dictionaryRemove(java.lang.Class<?>...values) {
		super.dictionaryRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient dictionaryRemove(Object...values) {
		super.dictionaryRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient dictionaryReplace(java.lang.Class<?>...values) {
		super.dictionaryReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient dictionaryReplace(Object...values) {
		super.dictionaryReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient dontIgnorePropertiesWithoutSetters() {
		super.dontIgnorePropertiesWithoutSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient dontIgnoreTransientFields() {
		super.dontIgnoreTransientFields();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient dontIgnoreUnknownNullBeanProperties() {
		super.dontIgnoreUnknownNullBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient dontUseInterfaceProxies() {
		super.dontUseInterfaceProxies();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> MockRestClient example(Class<T> pojoClass, T o) {
		super.example(pojoClass, o);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> MockRestClient exampleJson(Class<T> pojoClass, String json) {
		super.exampleJson(pojoClass, json);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient examples(String json) {
		super.examples(json);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public MockRestClient excludeProperties(Map<String,String> values) {
		super.excludeProperties(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public MockRestClient excludeProperties(Class<?> beanClass, String properties) {
		super.excludeProperties(beanClass, properties);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public MockRestClient excludeProperties(String beanClassName, String value) {
		super.excludeProperties(beanClassName, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient fluentSetters() {
		super.fluentSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient fluentSetters(boolean value) {
		super.fluentSetters(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient ignoreInvocationExceptionsOnGetters() {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient ignoreInvocationExceptionsOnGetters(boolean value) {
		super.ignoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient ignoreInvocationExceptionsOnSetters() {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient ignoreInvocationExceptionsOnSetters(boolean value) {
		super.ignoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient ignorePropertiesWithoutSetters(boolean value) {
		super.ignorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient ignoreTransientFields(boolean value) {
		super.ignoreTransientFields(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient ignoreUnknownBeanProperties() {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient ignoreUnknownBeanProperties(boolean value) {
		super.ignoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient ignoreUnknownNullBeanProperties(boolean value) {
		super.ignoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient implClass(Class<?> interfaceClass, Class<?> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient implClasses(Map<String,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public MockRestClient includeProperties(Map<String,String> values) {
		super.includeProperties(values);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public MockRestClient includeProperties(Class<?> beanClass, String value) {
		super.includeProperties(beanClass, value);
		return this;
	}

	@Deprecated @Override /* GENERATED - BeanContextBuilder */
	public MockRestClient includeProperties(String beanClassName, String value) {
		super.includeProperties(beanClassName, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient notBeanClasses(java.lang.Class<?>...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient notBeanClassesRemove(java.lang.Class<?>...values) {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient notBeanClassesRemove(Object...values) {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient notBeanClassesReplace(java.lang.Class<?>...values) {
		super.notBeanClassesReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient notBeanClassesReplace(Object...values) {
		super.notBeanClassesReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient notBeanPackages(Object...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient notBeanPackages(String...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient notBeanPackagesRemove(Object...values) {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient notBeanPackagesRemove(String...values) {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient notBeanPackagesReplace(Object...values) {
		super.notBeanPackagesReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient notBeanPackagesReplace(String...values) {
		super.notBeanPackagesReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient pojoSwaps(java.lang.Class<?>...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient pojoSwaps(Object...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient pojoSwapsRemove(java.lang.Class<?>...values) {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient pojoSwapsRemove(Object...values) {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient pojoSwapsReplace(java.lang.Class<?>...values) {
		super.pojoSwapsReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient pojoSwapsReplace(Object...values) {
		super.pojoSwapsReplace(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient sortProperties(boolean value) {
		super.sortProperties(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient useEnumNames() {
		super.useEnumNames();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient useEnumNames(boolean value) {
		super.useEnumNames(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient useInterfaceProxies(boolean value) {
		super.useInterfaceProxies(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient useJavaBeanIntrospector() {
		super.useJavaBeanIntrospector();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClient useJavaBeanIntrospector(boolean value) {
		super.useJavaBeanIntrospector(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient accept(Object value) {
		super.accept(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient acceptCharset(Object value) {
		super.acceptCharset(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient acceptEncoding(Object value) {
		super.acceptEncoding(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient acceptLanguage(Object value) {
		super.acceptLanguage(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient addBeanTypes() {
		super.addBeanTypes();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient addBeanTypes(boolean value) {
		super.addBeanTypes(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient addRootType() {
		super.addRootType();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient addRootType(boolean value) {
		super.addRootType(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient authorization(Object value) {
		super.authorization(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient autoCloseStreams() {
		super.autoCloseStreams();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient autoCloseStreams(boolean value) {
		super.autoCloseStreams(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient basicAuth(String host, int port, String user, String pw) {
		super.basicAuth(host, port, user, pw);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient binaryInputFormat(BinaryFormat value) {
		super.binaryInputFormat(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient binaryOutputFormat(BinaryFormat value) {
		super.binaryOutputFormat(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient cacheControl(Object value) {
		super.cacheControl(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient callHandler(Class<? extends org.apache.juneau.rest.client2.RestCallHandler> value) {
		super.callHandler(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient callHandler(RestCallHandler value) {
		super.callHandler(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient clientVersion(Object value) {
		super.clientVersion(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient connection(Object value) {
		super.connection(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient contentLength(Object value) {
		super.contentLength(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient contentType(Object value) {
		super.contentType(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient date(Object value) {
		super.date(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient debugOutputLines(int value) {
		super.debugOutputLines(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient detectRecursions() {
		super.detectRecursions();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient detectRecursions(boolean value) {
		super.detectRecursions(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient errorCodes(Predicate<Integer> value) {
		super.errorCodes(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient executorService(ExecutorService executorService, boolean shutdownOnClose) {
		super.executorService(executorService, shutdownOnClose);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient expect(Object value) {
		super.expect(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient fileCharset(String value) {
		super.fileCharset(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient formData(Object...params) {
		super.formData(params);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient formData(String name, Object value) {
		super.formData(name, value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient formData(String name, Object value, HttpPartSerializer serializer, HttpPartSchema schema) {
		super.formData(name, value, serializer, schema);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient formDataPairs(Object...pairs) {
		super.formDataPairs(pairs);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient forwarded(Object value) {
		super.forwarded(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient from(Object value) {
		super.from(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient header(Object header) {
		super.header(header);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient header(String name, Object value) {
		super.header(name, value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient header(String name, Object value, HttpPartSerializer serializer, HttpPartSchema schema) {
		super.header(name, value, serializer, schema);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient headerPairs(Object...pairs) {
		super.headerPairs(pairs);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient headers(Object...headers) {
		super.headers(headers);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient host(Object value) {
		super.host(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient html() {
		super.html();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient httpClient(CloseableHttpClient value) {
		super.httpClient(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient httpClientBuilder(HttpClientBuilder value) {
		super.httpClientBuilder(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient httpClientConnectionManager(HttpClientConnectionManager httpClientConnectionManager) {
		super.httpClientConnectionManager(httpClientConnectionManager);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient ifMatch(Object value) {
		super.ifMatch(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient ifModifiedSince(Object value) {
		super.ifModifiedSince(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient ifNoneMatch(Object value) {
		super.ifNoneMatch(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient ifRange(Object value) {
		super.ifRange(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient ifUnmodifiedSince(Object value) {
		super.ifUnmodifiedSince(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient ignoreRecursions() {
		super.ignoreRecursions();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient ignoreRecursions(boolean value) {
		super.ignoreRecursions(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient initialDepth(int value) {
		super.initialDepth(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient inputStreamCharset(String value) {
		super.inputStreamCharset(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	@SuppressWarnings("unchecked")
	public MockRestClient interceptors(java.lang.Class<? extends org.apache.juneau.rest.client2.RestCallInterceptor>...values) throws Exception{
		super.interceptors(values);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient interceptors(RestCallInterceptor...value) {
		super.interceptors(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient json() {
		super.json();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient keepHttpClientOpen() {
		super.keepHttpClientOpen();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient keepHttpClientOpen(boolean value) {
		super.keepHttpClientOpen(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient keepNullProperties() {
		super.keepNullProperties();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient keepNullProperties(boolean value) {
		super.keepNullProperties(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient leakDetection() {
		super.leakDetection();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient leakDetection(boolean value) {
		super.leakDetection(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient listenerP(Class<? extends org.apache.juneau.parser.ParserListener> value) {
		super.listenerP(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient listenerS(Class<? extends org.apache.juneau.serializer.SerializerListener> value) {
		super.listenerS(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient logTo(Level level, Logger log) {
		super.logTo(level, log);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient logToConsole() {
		super.logToConsole();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient marshall(Marshall value) {
		super.marshall(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient marshalls(Marshall...value) {
		super.marshalls(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient maxDepth(int value) {
		super.maxDepth(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient maxForwards(Object value) {
		super.maxForwards(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient maxIndent(int value) {
		super.maxIndent(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient msgPack() {
		super.msgPack();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient noTrace() {
		super.noTrace();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient openApi() {
		super.openApi();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient origin(Object value) {
		super.origin(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient paramFormat(String value) {
		super.paramFormat(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient paramFormatPlain() {
		super.paramFormatPlain();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient parser(Class<? extends org.apache.juneau.parser.Parser> value) {
		super.parser(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient parser(Parser value) {
		super.parser(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	@SuppressWarnings("unchecked")
	public MockRestClient parsers(java.lang.Class<? extends org.apache.juneau.parser.Parser>...value) {
		super.parsers(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient parsers(Parser...value) {
		super.parsers(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient partParser(Class<? extends org.apache.juneau.httppart.HttpPartParser> value) {
		super.partParser(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient partParser(HttpPartParser value) {
		super.partParser(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient partSerializer(Class<? extends org.apache.juneau.httppart.HttpPartSerializer> value) {
		super.partSerializer(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient partSerializer(HttpPartSerializer value) {
		super.partSerializer(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient plainText() {
		super.plainText();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient pooled() {
		super.pooled();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient pragma(Object value) {
		super.pragma(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient proxyAuthorization(Object value) {
		super.proxyAuthorization(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient query(Object...params) {
		super.query(params);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient query(String name, Object value) {
		super.query(name, value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient query(String name, Object value, HttpPartSerializer serializer, HttpPartSchema schema) {
		super.query(name, value, serializer, schema);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient queryPairs(Object...pairs) {
		super.queryPairs(pairs);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient quoteChar(char value) {
		super.quoteChar(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient range(Object value) {
		super.range(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient referer(Object value) {
		super.referer(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient rootUrl(Object value) {
		super.rootUrl(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient serializer(Class<? extends org.apache.juneau.serializer.Serializer> value) {
		super.serializer(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient serializer(Serializer value) {
		super.serializer(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	@SuppressWarnings("unchecked")
	public MockRestClient serializers(java.lang.Class<? extends org.apache.juneau.serializer.Serializer>...value) {
		super.serializers(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient serializers(Serializer...value) {
		super.serializers(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient simpleJson() {
		super.simpleJson();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient sortCollections() {
		super.sortCollections();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient sortCollections(boolean value) {
		super.sortCollections(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient sortMaps() {
		super.sortMaps();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient sortMaps(boolean value) {
		super.sortMaps(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient sq() {
		super.sq();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient strict() {
		super.strict();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient strict(boolean value) {
		super.strict(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient te(Object value) {
		super.te(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient trimEmptyCollections() {
		super.trimEmptyCollections();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient trimEmptyCollections(boolean value) {
		super.trimEmptyCollections(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient trimEmptyMaps() {
		super.trimEmptyMaps();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient trimEmptyMaps(boolean value) {
		super.trimEmptyMaps(value);
		return this;
	}

	@Deprecated @Override /* GENERATED - RestClientBuilder */
	public MockRestClient trimNullProperties(boolean value) {
		super.trimNullProperties(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient trimStringsP() {
		super.trimStringsP();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient trimStringsP(boolean value) {
		super.trimStringsP(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient trimStringsS() {
		super.trimStringsS();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient trimStringsS(boolean value) {
		super.trimStringsS(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient unbuffered() {
		super.unbuffered();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient unbuffered(boolean value) {
		super.unbuffered(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient uon() {
		super.uon();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient upgrade(Object value) {
		super.upgrade(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient uriContext(UriContext value) {
		super.uriContext(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient uriRelativity(UriRelativity value) {
		super.uriRelativity(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient uriResolution(UriResolution value) {
		super.uriResolution(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient urlEnc() {
		super.urlEnc();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient useWhitespace() {
		super.useWhitespace();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient useWhitespace(boolean value) {
		super.useWhitespace(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient userAgent(Object value) {
		super.userAgent(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient via(Object value) {
		super.via(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient warning(Object value) {
		super.warning(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient ws() {
		super.ws();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClient xml() {
		super.xml();
		return this;
	}

	// </CONFIGURATION-PROPERTIES>
}
