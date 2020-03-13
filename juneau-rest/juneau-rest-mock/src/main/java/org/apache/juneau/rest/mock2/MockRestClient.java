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
import java.lang.annotation.*;
import java.lang.reflect.Method;
import org.apache.juneau.*;
import org.apache.juneau.http.*;
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
		super(null, null);
		mrb = MockRest.create(impl);
		rootUrl("http://localhost");
	}

	/**
	 * Creates a new RestClient builder configured with the specified REST implementation bean or bean class.
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
	 * Creates a new RestClient builder configured with the specified REST implementation bean or bean class.
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link Rest @Rest}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @param m
	 * 	The marshall to use for serializing and parsing HTTP bodies.
	 * 	<br>Can be <jk>null</jk> (will remove the existing serializer/parser).
	 * @return A new builder.
	 */
	public static MockRestClient create(Object impl, Marshall m) {
		return create(impl).marshall(m);
	}

	/**
	 * Creates a new RestClient builder configured with the specified REST implementation bean or bean class.
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link Rest @Rest}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @param s
	 * 	The serializer to use for serializing HTTP bodies.
	 * 	<br>Can be <jk>null</jk> (will remove the existing serializer).
	 * @param p
	 * 	The parser to use for parsing HTTP bodies.
	 * 	<br>Can be <jk>null</jk> (will remove the existing parser).
	 * @return A new builder.
	 */
	public static MockRestClient create(Object impl, Serializer s, Parser p) {
		return create(impl).serializer(s).parser(p);
	}

	/**
	 * Convenience method for creating a RestClient over the specified REST implementation bean or bean class.
	 *
	 * <p>
	 * Equivalent to calling:
	 * <p class='bcode w800'>
	 * 	MockRestClient.create(impl, m).build();
	 * </p>
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link Rest @Rest}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @param m
	 * 	The marshall to use for specifying the <c>Accept</c> and <c>Content-Type</c> headers.
	 * 	<br>If <jk>null</jk>, headers will be reset.
	 * @return A new {@link MockRest} object.
	 */
	public static RestClient build(Object impl, Marshall m) {
		return create(impl, m).build();
	}

	/**
	 * Convenience method for creating a RestClient over the specified REST implementation bean or bean class.
	 *
	 * <p>
	 * Equivalent to calling:
	 * <p class='bcode w800'>
	 * 	MockRestClient.create(impl, s, p).build();
	 * </p>
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link Rest @Rest}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @param s
	 * 	The serializer to use for serializing HTTP bodies.
	 * 	<br>Can be <jk>null</jk> (will remove the existing serializer).
	 * @param p
	 * 	The parser to use for parsing HTTP bodies.
	 * 	<br>Can be <jk>null</jk> (will remove the existing parser).
	 * @return A new {@link MockRest} object.
	 */
	public static RestClient build(Object impl, Serializer s, Parser p) {
		return create(impl, s, p).build();
	}

	@Override
	public RestClient build() {
		httpClientConnectionManager(new MockHttpClientConnectionManager(mrb.build()));
		return super.build();
	}

	/**
	 * Enable debug mode.
	 *
	 * @return This object (for method chaining).
	 */
	@Override
	public MockRestClient debug() {
		mrb.debug();
		debug();
		return this;
	}

	/**
	 * Convenience method for setting <c>Accept</c> and <c>Content-Type</c> headers to <js>"application/json"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	@Override
	public MockRestClient json() {
		marshall(Json.DEFAULT);
		return this;
	}

	/**
	 * Convenience method for setting <c>Accept</c> and <c>Content-Type</c> headers to <js>"application/json+simple"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	@Override
	public MockRestClient simpleJson() {
		marshall(SimpleJson.DEFAULT);
		return this;
	}

	/**
	 * Convenience method for setting <c>Accept</c> and <c>Content-Type</c> headers to <js>"text/xml"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	@Override
	public MockRestClient xml() {
		marshall(Xml.DEFAULT);
		return this;
	}

	/**
	 * Convenience method for setting <c>Accept</c> and <c>Content-Type</c> headers to <js>"text/html"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	@Override
	public MockRestClient html() {
		marshall(Html.DEFAULT);
		return this;
	}

	/**
	 * Convenience method for setting <c>Accept</c> and <c>Content-Type</c> headers to <js>"text/plain"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	@Override
	public MockRestClient plainText() {
		marshall(PlainText.DEFAULT);
		return this;
	}

	/**
	 * Convenience method for setting <c>Accept</c> and <c>Content-Type</c> headers to <js>"octal/msgpack"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	@Override
	public MockRestClient msgpack() {
		marshall(MsgPack.DEFAULT);
		return this;
	}

	/**
	 * Convenience method for setting <c>Accept</c> and <c>Content-Type</c> headers to <js>"text/uon"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	@Override
	public MockRestClient uon() {
		marshall(Uon.DEFAULT);
		return this;
	}

	/**
	 * Convenience method for setting <c>Accept</c> and <c>Content-Type</c> headers to <js>"application/x-www-form-urlencoded"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	@Override
	public MockRestClient urlEnc() {
		marshall(UrlEncoding.DEFAULT);
		return this;
	}

	/**
	 * Convenience method for setting <c>Accept</c> and <c>Content-Type</c> headers to <js>"text/openapi"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	@Override
	public MockRestClient openapi() {
		marshall(OpenApi.DEFAULT);
		return this;
	}

	@Override
	public MockRestClient marshall(Marshall value) {
		super.marshall(value);
		mrb.marshall(value);
		return this;
	}

	@Override
	public MockRestClient serializer(Serializer value) {
		super.serializer(value);
		mrb.serializer(value);
		return this;
	}

	@Override
	public MockRestClient parser(Parser value) {
		super.parser(value);
		mrb.parser(value);
		return this;
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
	public MockRestClient addTo(String name, String key, Object value) {
		super.addTo(name, key, value);
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

	// </CONFIGURATION-PROPERTIES>
}
