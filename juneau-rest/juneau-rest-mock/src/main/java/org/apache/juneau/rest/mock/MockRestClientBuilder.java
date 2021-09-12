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
package org.apache.juneau.rest.mock;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.*;

import static org.apache.juneau.rest.mock.MockRestClient.*;
import static org.apache.juneau.rest.util.RestUtils.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.time.*;

import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.entity.*;
import org.apache.http.conn.*;
import org.apache.http.impl.client.*;
import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.uon.*;

import javax.net.ssl.*;
import javax.servlet.http.*;

import org.apache.http.auth.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.*;
import org.apache.http.config.*;
import org.apache.http.conn.routing.*;
import org.apache.http.conn.socket.*;
import org.apache.http.conn.util.*;
import org.apache.http.cookie.*;
import org.apache.http.protocol.*;

/**
 * Builder class for {@link MockRestClient} objects.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-mock}
 * </ul>
 */
@FluentSetters(ignore="debug")
public class MockRestClientBuilder extends RestClientBuilder {

	/**
	 * No-arg constructor.
	 *
	 * <p>
	 * Provided so that this class can be easily subclassed.
	 */
	protected MockRestClientBuilder() {
		super();
		connectionManager(new MockHttpClientConnectionManager());
		type(MockRestClient.class);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy from.
	 */
	protected MockRestClientBuilder(MockRestClient copyFrom) {
		super(copyFrom);
		connectionManager(new MockHttpClientConnectionManager());
		type(MockRestClient.class);
	}

	@Override /* ContextBuilder */
	public MockRestClientBuilder copy() {
		throw new NoSuchMethodError("Not implemented.");
	}

	/**
	 * Specifies the {@link Rest}-annotated bean class or instance to test against.
	 *
	 * @param bean The {@link Rest}-annotated bean class or instance.
	 * @return This object (for method chaining).
	 */
	public MockRestClientBuilder restBean(Object bean) {
		return set(MOCKRESTCLIENT_restBean, bean);
	}

	/**
	 * Identifies the context path for the REST resource.
	 *
	 * <p>
	 * 	This value is used to deconstruct the request URL and set the appropriate URL getters on the {@link HttpServletRequest}
	 * 	object correctly.
	 *
	 * <p>
	 * 	Should either be a value such as <js>"/foo"</js> or an empty string.
	 *
	 * <p>
	 * 	The following fixes are applied to non-conforming strings.
	 * <ul>
	 * 	<li><jk>nulls</jk> and <js>"/"</js> are converted to empty strings.
	 * 	<li>Trailing slashes are trimmed.
	 * 	<li>Leading slash is added if needed.
	 * </ul>
	 *
	 * @param value The context path.
	 * @return This object (for method chaining).
	 */
	public MockRestClientBuilder contextPath(String value) {
		return set(MOCKRESTCLIENT_contextPath, toValidContextPath(value));
	}

	/**
	 * Identifies the servlet path for the REST resource.
	 *
	 * <p>
	 * 	This value is used to deconstruct the request URL and set the appropriate URL getters on the {@link HttpServletRequest}
	 * 	object correctly.
	 *
	 * <p>
	 * 	Should either be a value such as <js>"/foo"</js> or an empty string.
	 *
	 * <p>
	 * 	The following fixes are applied to non-conforming strings.
	 * <ul>
	 * 	<li><jk>nulls</jk> and <js>"/"</js> are converted to empty strings.
	 * 	<li>Trailing slashes are trimmed.
	 * 	<li>Leading slash is added if needed.
	 * </ul>
	 *
	 * @param value The context path.
	 * @return This object (for method chaining).
	 */
	public MockRestClientBuilder servletPath(String value) {
		return set(MOCKRESTCLIENT_servletPath, toValidContextPath(value));
	}

	/**
	 * Add resolved path variables to this client.
	 *
	 * <p>
	 * Allows you to add resolved parent path variables when performing tests on child resource classes.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A parent class with a path variable.</jc>
	 * 	<ja>@Rest</ja>(
	 * 		path=<js>"/parent/{foo}"</js>,
	 * 		children={
	 * 			Child.<jk>class</jk>
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> Parent { ... }
	 *
	 * 	<jc>// A child class that uses the parent path variable.</jc>
	 * 	<ja>@Rest</ja>
	 * 	<jk>public class</jk> Child {
	 *
	 * 		<jk>@RestGet</jk>
	 * 		<jk>public</jk> String get(<ja>@Path</ja>(<js>"foo"</js>) String <jv>foo</jv>) {
	 * 			<jk>return</jk> <jv>foo<jv>;
	 * 		}
	 * 	}
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Test the method that uses the parent path variable.</jc>
	 * 	MockRestClient
	 * 		.<jsm>create</jsm>(Child.<jk>class</jk>)
	 * 		.simpleJson()
	 * 		.pathVars(<js>"foo"</js>,<js>"bar"</js>)
	 * 		.build()
	 * 		.get(<js>"/"</js>)
	 * 		.run()
	 * 		.assertStatus().code().is(200)
	 * 		.assertBody().is(<js>"bar"</js>);
	 * </p>
	 *
	 * <review>Needs review</review>
	 *
	 * @param value The path variables.
	 * @return This object (for method chaining).
	 * @see MockServletRequest#pathVars(Map)
	 */
	public MockRestClientBuilder pathVars(Map<String,String> value) {
		return putAllTo(MOCKRESTCLIENT_pathVars, value);
	}

	/**
	 * Add resolved path variables to this client.
	 *
	 * <p>
	 * Identical to {@link #pathVars(Map)} but allows you to specify as a list of key/value pairs.
	 *
	 * @param pairs The key/value pairs.  Must be an even number of parameters.
	 * @return This object (for method chaining).
	 */
	public MockRestClientBuilder pathVars(String...pairs) {
		return pathVars(AMap.<String,String>ofPairs((Object[])pairs));
	}

	/**
	 * Suppress logging on this client.
	 *
	 * @return This object.
	 */
	public MockRestClientBuilder suppressLogging() {
		return logRequests(DetailLevel.NONE, null, null);
	}

	@Override /* ContextBuilder */
	public MockRestClientBuilder debug() {
		header("Debug", "true");
		super.debug();
		return this;
	}

	@Override /* ContextBuilder */
	public MockRestClient build() {
		return (MockRestClient)super.build();
	}

	@Override /* ContextBuilder */
	public <T extends Context> T build(Class<T> c) {
		return super.build(c);
	}

	// <FluentSetters>

	@Override /* GENERATED - ContextBuilder */
	public MockRestClientBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MockRestClientBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MockRestClientBuilder appendTo(String name, Object value) {
		super.appendTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MockRestClientBuilder apply(ContextProperties copyFrom) {
		super.apply(copyFrom);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MockRestClientBuilder applyAnnotations(java.lang.Class<?>...fromClasses) {
		super.applyAnnotations(fromClasses);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MockRestClientBuilder applyAnnotations(Method...fromMethods) {
		super.applyAnnotations(fromMethods);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MockRestClientBuilder apply(AnnotationWorkList work) {
		super.apply(work);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MockRestClientBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MockRestClientBuilder prependTo(String name, Object value) {
		super.prependTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MockRestClientBuilder putAllTo(String name, Object value) {
		super.putAllTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MockRestClientBuilder putTo(String name, String key, Object value) {
		super.putTo(name, key, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MockRestClientBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MockRestClientBuilder set(String name) {
		super.set(name);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MockRestClientBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MockRestClientBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MockRestClientBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public MockRestClientBuilder unset(String name) {
		super.unset(name);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder annotations(Annotation...values) {
		super.annotations(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder beanInterceptor(Class<?> on, Class<? extends org.apache.juneau.transform.BeanInterceptor<?>> value) {
		super.beanInterceptor(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder beanProperties(Map<String,Object> values) {
		super.beanProperties(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder beanProperties(Class<?> beanClass, String properties) {
		super.beanProperties(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder beanProperties(String beanClassName, String properties) {
		super.beanProperties(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder beanPropertiesExcludes(Map<String,Object> values) {
		super.beanPropertiesExcludes(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder beanPropertiesExcludes(Class<?> beanClass, String properties) {
		super.beanPropertiesExcludes(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder beanPropertiesExcludes(String beanClassName, String properties) {
		super.beanPropertiesExcludes(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder beanPropertiesReadOnly(Map<String,Object> values) {
		super.beanPropertiesReadOnly(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder beanPropertiesReadOnly(Class<?> beanClass, String properties) {
		super.beanPropertiesReadOnly(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder beanPropertiesReadOnly(String beanClassName, String properties) {
		super.beanPropertiesReadOnly(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder beanPropertiesWriteOnly(Map<String,Object> values) {
		super.beanPropertiesWriteOnly(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder beanPropertiesWriteOnly(Class<?> beanClass, String properties) {
		super.beanPropertiesWriteOnly(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder beanPropertiesWriteOnly(String beanClassName, String properties) {
		super.beanPropertiesWriteOnly(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder beansRequireDefaultConstructor() {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder beansRequireSerializable() {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder beansRequireSettersForGetters() {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder dictionary(Object...values) {
		super.dictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
		super.dictionaryOn(on, values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder disableBeansRequireSomeProperties() {
		super.disableBeansRequireSomeProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder disableIgnoreMissingSetters() {
		super.disableIgnoreMissingSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder disableIgnoreTransientFields() {
		super.disableIgnoreTransientFields();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder disableIgnoreUnknownNullBeanProperties() {
		super.disableIgnoreUnknownNullBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder disableInterfaceProxies() {
		super.disableInterfaceProxies();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder findFluentSetters() {
		super.findFluentSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder findFluentSetters(Class<?> on) {
		super.findFluentSetters(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder ignoreInvocationExceptionsOnGetters() {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder ignoreInvocationExceptionsOnSetters() {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder ignoreUnknownBeanProperties() {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder implClass(Class<?> interfaceClass, Class<?> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder implClasses(Map<Class<?>,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder interfaceClass(Class<?> on, Class<?> value) {
		super.interfaceClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder interfaces(java.lang.Class<?>...value) {
		super.interfaces(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder notBeanPackages(Object...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder sortProperties(java.lang.Class<?>...on) {
		super.sortProperties(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder stopClass(Class<?> on, Class<?> value) {
		super.stopClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder swaps(Object...values) {
		super.swaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder typeName(Class<?> on, String value) {
		super.typeName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder typePropertyName(String value) {
		super.typePropertyName(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder typePropertyName(Class<?> on, String value) {
		super.typePropertyName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder useEnumNames() {
		super.useEnumNames();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public MockRestClientBuilder useJavaBeanIntrospector() {
		super.useJavaBeanIntrospector();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder accept(String value) {
		super.accept(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder acceptCharset(String value) {
		super.acceptCharset(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder acceptEncoding(String value) {
		super.acceptEncoding(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder acceptLanguage(String value) {
		super.acceptLanguage(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder addBeanTypes() {
		super.addBeanTypes();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder addInterceptorFirst(HttpRequestInterceptor itcp) {
		super.addInterceptorFirst(itcp);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder addInterceptorFirst(HttpResponseInterceptor itcp) {
		super.addInterceptorFirst(itcp);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder addInterceptorLast(HttpRequestInterceptor itcp) {
		super.addInterceptorLast(itcp);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder addInterceptorLast(HttpResponseInterceptor itcp) {
		super.addInterceptorLast(itcp);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder addRootType() {
		super.addRootType();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder authorization(String value) {
		super.authorization(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder backoffManager(BackoffManager backoffManager) {
		super.backoffManager(backoffManager);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder basicAuth(String host, int port, String user, String pw) {
		super.basicAuth(host, port, user, pw);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder cacheControl(String value) {
		super.cacheControl(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder callHandler(Class<? extends org.apache.juneau.rest.client.RestCallHandler> value) {
		super.callHandler(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder callHandler(RestCallHandler value) {
		super.callHandler(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder clientVersion(String value) {
		super.clientVersion(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder connection(String value) {
		super.connection(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder connectionBackoffStrategy(ConnectionBackoffStrategy connectionBackoffStrategy) {
		super.connectionBackoffStrategy(connectionBackoffStrategy);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder connectionManager(HttpClientConnectionManager connManager) {
		super.connectionManager(connManager);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder connectionManagerShared(boolean shared) {
		super.connectionManagerShared(shared);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder connectionReuseStrategy(ConnectionReuseStrategy reuseStrategy) {
		super.connectionReuseStrategy(reuseStrategy);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder connectionTimeToLive(long connTimeToLive, TimeUnit connTimeToLiveTimeUnit) {
		super.connectionTimeToLive(connTimeToLive, connTimeToLiveTimeUnit);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder console(PrintStream value) {
		super.console(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder console(Class<? extends java.io.PrintStream> value) {
		super.console(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder contentDecoderRegistry(Map<String,InputStreamFactory> contentDecoderMap) {
		super.contentDecoderRegistry(contentDecoderMap);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder contentEncoding(String value) {
		super.contentEncoding(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder contentLength(Long value) {
		super.contentLength(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder contentType(String value) {
		super.contentType(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder date(ZonedDateTime value) {
		super.date(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder debugOutputLines(int value) {
		super.debugOutputLines(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder defaultAuthSchemeRegistry(Lookup<AuthSchemeProvider> authSchemeRegistry) {
		super.defaultAuthSchemeRegistry(authSchemeRegistry);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder defaultConnectionConfig(ConnectionConfig config) {
		super.defaultConnectionConfig(config);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder defaultCookieSpecRegistry(Lookup<CookieSpecProvider> cookieSpecRegistry) {
		super.defaultCookieSpecRegistry(cookieSpecRegistry);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder defaultCookieStore(CookieStore cookieStore) {
		super.defaultCookieStore(cookieStore);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder defaultCredentialsProvider(CredentialsProvider credentialsProvider) {
		super.defaultCredentialsProvider(credentialsProvider);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder defaultRequestConfig(RequestConfig config) {
		super.defaultRequestConfig(config);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder defaultSocketConfig(SocketConfig config) {
		super.defaultSocketConfig(config);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder detectRecursions() {
		super.detectRecursions();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder disableAuthCaching() {
		super.disableAuthCaching();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder disableAutomaticRetries() {
		super.disableAutomaticRetries();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder disableConnectionState() {
		super.disableConnectionState();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder disableContentCompression() {
		super.disableContentCompression();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder disableCookieManagement() {
		super.disableCookieManagement();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder disableRedirectHandling() {
		super.disableRedirectHandling();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder errorCodes(Predicate<Integer> value) {
		super.errorCodes(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder evictExpiredConnections() {
		super.evictExpiredConnections();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder evictIdleConnections(long maxIdleTime, TimeUnit maxIdleTimeUnit) {
		super.evictIdleConnections(maxIdleTime, maxIdleTimeUnit);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder executorService(ExecutorService executorService, boolean shutdownOnClose) {
		super.executorService(executorService, shutdownOnClose);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder expect(String value) {
		super.expect(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder formData(NameValuePair...parts) {
		super.formData(parts);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder formData(NameValuePair part) {
		super.formData(part);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder formData(PartList parts) {
		super.formData(parts);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder formData(String name, String value) {
		super.formData(name, value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder formData(String name, Supplier<String> value) {
		super.formData(name, value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder formDataPairs(String...pairs) {
		super.formDataPairs(pairs);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder forwarded(String value) {
		super.forwarded(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder from(String value) {
		super.from(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder header(Header part) {
		super.header(part);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder header(String name, String value) {
		super.header(name, value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder header(String name, Supplier<String> value) {
		super.header(name, value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder headerPairs(String...pairs) {
		super.headerPairs(pairs);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder headers(Header...parts) {
		super.headers(parts);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder headers(HeaderList parts) {
		super.headers(parts);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder host(String value) {
		super.host(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder html() {
		super.html();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder htmlDoc() {
		super.htmlDoc();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder htmlStrippedDoc() {
		super.htmlStrippedDoc();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder httpClient(CloseableHttpClient value) {
		super.httpClient(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder httpClientBuilder(HttpClientBuilder value) {
		super.httpClientBuilder(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder httpProcessor(HttpProcessor httpprocessor) {
		super.httpProcessor(httpprocessor);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder ifMatch(String value) {
		super.ifMatch(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder ifModifiedSince(ZonedDateTime value) {
		super.ifModifiedSince(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder ifNoneMatch(String value) {
		super.ifNoneMatch(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder ifRange(String value) {
		super.ifRange(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder ifUnmodifiedSince(ZonedDateTime value) {
		super.ifUnmodifiedSince(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder ignoreErrors() {
		super.ignoreErrors();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder ignoreErrors(boolean value) {
		super.ignoreErrors(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder ignoreRecursions() {
		super.ignoreRecursions();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder initialDepth(int value) {
		super.initialDepth(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder interceptors(java.lang.Class<?>...values) throws Exception{
		super.interceptors(values);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder interceptors(Object...value) {
		super.interceptors(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder json() {
		super.json();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder keepAliveStrategy(ConnectionKeepAliveStrategy keepAliveStrategy) {
		super.keepAliveStrategy(keepAliveStrategy);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder keepHttpClientOpen() {
		super.keepHttpClientOpen();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder keepNullProperties() {
		super.keepNullProperties();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder leakDetection() {
		super.leakDetection();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder logRequests(DetailLevel detail, Level level, BiPredicate<RestRequest,RestResponse> test) {
		super.logRequests(detail, level, test);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder logToConsole() {
		super.logToConsole();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder logger(Logger value) {
		super.logger(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder marshall(Marshall value) {
		super.marshall(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder marshalls(Marshall...value) {
		super.marshalls(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder maxConnPerRoute(int maxConnPerRoute) {
		super.maxConnPerRoute(maxConnPerRoute);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder maxConnTotal(int maxConnTotal) {
		super.maxConnTotal(maxConnTotal);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder maxDepth(int value) {
		super.maxDepth(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder maxForwards(Integer value) {
		super.maxForwards(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder maxIndent(int value) {
		super.maxIndent(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder mediaType(String value) {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder msgPack() {
		super.msgPack();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder noTrace() {
		super.noTrace();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder oapiCollectionFormat(HttpPartCollectionFormat value) {
		super.oapiCollectionFormat(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder oapiFormat(HttpPartFormat value) {
		super.oapiFormat(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder openApi() {
		super.openApi();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder origin(String value) {
		super.origin(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder paramFormat(ParamFormat value) {
		super.paramFormat(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder paramFormatPlain() {
		super.paramFormatPlain();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder parser(Class<? extends org.apache.juneau.parser.Parser> value) {
		super.parser(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder parser(Parser value) {
		super.parser(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	@SuppressWarnings("unchecked")
	public MockRestClientBuilder parsers(java.lang.Class<? extends org.apache.juneau.parser.Parser>...value) {
		super.parsers(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder parsers(Parser...value) {
		super.parsers(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder partParser(Class<? extends org.apache.juneau.httppart.HttpPartParser> value) {
		super.partParser(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder partParser(HttpPartParser value) {
		super.partParser(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder partSerializer(Class<? extends org.apache.juneau.httppart.HttpPartSerializer> value) {
		super.partSerializer(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder partSerializer(HttpPartSerializer value) {
		super.partSerializer(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder pathData(NameValuePair...parts) {
		super.pathData(parts);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder pathData(NameValuePair part) {
		super.pathData(part);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder pathData(PartList parts) {
		super.pathData(parts);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder pathData(String name, String value) {
		super.pathData(name, value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder pathData(String name, Supplier<String> value) {
		super.pathData(name, value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder pathDataPairs(String...pairs) {
		super.pathDataPairs(pairs);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder plainText() {
		super.plainText();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder pooled() {
		super.pooled();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder pragma(String value) {
		super.pragma(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder proxy(HttpHost proxy) {
		super.proxy(proxy);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder proxyAuthenticationStrategy(AuthenticationStrategy proxyAuthStrategy) {
		super.proxyAuthenticationStrategy(proxyAuthStrategy);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder proxyAuthorization(String value) {
		super.proxyAuthorization(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder publicSuffixMatcher(PublicSuffixMatcher publicSuffixMatcher) {
		super.publicSuffixMatcher(publicSuffixMatcher);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder queryData(NameValuePair...parts) {
		super.queryData(parts);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder queryData(NameValuePair part) {
		super.queryData(part);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder queryData(PartList parts) {
		super.queryData(parts);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder queryData(String name, String value) {
		super.queryData(name, value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder queryData(String name, Supplier<String> value) {
		super.queryData(name, value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder queryDataPairs(String...pairs) {
		super.queryDataPairs(pairs);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder quoteChar(char value) {
		super.quoteChar(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder range(String value) {
		super.range(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder redirectStrategy(RedirectStrategy redirectStrategy) {
		super.redirectStrategy(redirectStrategy);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder referer(String value) {
		super.referer(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder requestExecutor(HttpRequestExecutor requestExec) {
		super.requestExecutor(requestExec);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder retryHandler(HttpRequestRetryHandler retryHandler) {
		super.retryHandler(retryHandler);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder rootUri(Object value) {
		super.rootUri(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder routePlanner(HttpRoutePlanner routePlanner) {
		super.routePlanner(routePlanner);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder schemePortResolver(SchemePortResolver schemePortResolver) {
		super.schemePortResolver(schemePortResolver);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder serializer(Class<? extends org.apache.juneau.serializer.Serializer> value) {
		super.serializer(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder serializer(Serializer value) {
		super.serializer(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	@SuppressWarnings("unchecked")
	public MockRestClientBuilder serializers(java.lang.Class<? extends org.apache.juneau.serializer.Serializer>...value) {
		super.serializers(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder serializers(Serializer...value) {
		super.serializers(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder serviceUnavailableRetryStrategy(ServiceUnavailableRetryStrategy serviceUnavailStrategy) {
		super.serviceUnavailableRetryStrategy(serviceUnavailStrategy);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder simpleJson() {
		super.simpleJson();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder skipEmptyFormData() {
		super.skipEmptyFormData();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder skipEmptyFormData(boolean value) {
		super.skipEmptyFormData(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder skipEmptyHeaders() {
		super.skipEmptyHeaders();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder skipEmptyHeaders(boolean value) {
		super.skipEmptyHeaders(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder skipEmptyQueryData() {
		super.skipEmptyQueryData();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder skipEmptyQueryData(boolean value) {
		super.skipEmptyQueryData(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder sortCollections() {
		super.sortCollections();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder sortMaps() {
		super.sortMaps();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder sq() {
		super.sq();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder sslContext(SSLContext sslContext) {
		super.sslContext(sslContext);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder sslHostnameVerifier(HostnameVerifier hostnameVerifier) {
		super.sslHostnameVerifier(hostnameVerifier);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder sslSocketFactory(LayeredConnectionSocketFactory sslSocketFactory) {
		super.sslSocketFactory(sslSocketFactory);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder strict() {
		super.strict();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder targetAuthenticationStrategy(AuthenticationStrategy targetAuthStrategy) {
		super.targetAuthenticationStrategy(targetAuthStrategy);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder te(String value) {
		super.te(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder trimEmptyCollections() {
		super.trimEmptyCollections();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder trimEmptyMaps() {
		super.trimEmptyMaps();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder trimStringsOnRead() {
		super.trimStringsOnRead();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder trimStringsOnWrite() {
		super.trimStringsOnWrite();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder uon() {
		super.uon();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder upgrade(String value) {
		super.upgrade(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder uriContext(UriContext value) {
		super.uriContext(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder uriRelativity(UriRelativity value) {
		super.uriRelativity(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder uriResolution(UriResolution value) {
		super.uriResolution(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder urlEnc() {
		super.urlEnc();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder useSystemProperties() {
		super.useSystemProperties();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder useWhitespace() {
		super.useWhitespace();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder userAgent(String value) {
		super.userAgent(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder userTokenHandler(UserTokenHandler userTokenHandler) {
		super.userTokenHandler(userTokenHandler);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder via(String value) {
		super.via(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder warning(String value) {
		super.warning(value);
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder ws() {
		super.ws();
		return this;
	}

	@Override /* GENERATED - RestClientBuilder */
	public MockRestClientBuilder xml() {
		super.xml();
		return this;
	}

	// </FluentSetters>
}
