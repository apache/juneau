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
package org.apache.juneau.rest;

import static java.util.Arrays.*;
import static org.apache.juneau.rest.RestMethodContext.*;

import java.lang.annotation.*;
import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.svl.*;
import java.lang.reflect.Method;

/**
 * Builder class for {@link RestMethodContext} objects.
 */
public class RestMethodContextBuilder extends BeanContextBuilder {

	RestContext context;
	java.lang.reflect.Method method;

	boolean dotAll;

	RestMethodContextBuilder(Object servlet, java.lang.reflect.Method method, RestContext context) throws RestServletException {
		this.context = context;
		this.method = method;

		String sig = method.getDeclaringClass().getName() + '.' + method.getName();
		MethodInfo mi = MethodInfo.of(servlet.getClass(), method);

		try {

			RestMethod m = mi.getLastAnnotation(RestMethod.class);

			// Also include methods on @Rest-annotated interfaces.
			if (m == null) {
				for (Method mi2 : mi.getMatching()) {
					Class<?> ci2 = mi2.getDeclaringClass();
					if (ci2.isInterface() && ci2.getAnnotation(Rest.class) != null)
						m = RestMethodAnnotation.DEFAULT;
				}
			}

			if (m == null)
				throw new RestServletException("@RestMethod annotation not found on method ''{0}''", sig);

			VarResolver vr = context.getVarResolver();
			VarResolverSession vrs = vr.createSession();

			applyAnnotations(mi.getAnnotationList(ConfigAnnotationFilter.INSTANCE), vrs);

		} catch (RestServletException e) {
			throw e;
		} catch (Exception e) {
			throw new RestServletException(e, "Exception occurred while initializing method ''{0}''", sig);
		}
	}

	/**
	 * When enabled, append <js>"/*"</js> to path patterns if not already present.
	 *
	 * @return This object (for method chaining).
	 */
	public RestMethodContextBuilder dotAll() {
		this.dotAll = true;
		return this;
	}

	//----------------------------------------------------------------------------------------------------
	// Properties
	//----------------------------------------------------------------------------------------------------

	/**
	 * <i><l>RestMethodContext</l> configuration property:&emsp;</i>  Client version pattern matcher.
	 *
	 * <p>
	 * Specifies whether this method can be called based on the client version.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestMethodContext#RESTMETHOD_clientVersion}
	 * </ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestMethodContextBuilder clientVersion(String value) {
		return set(RESTMETHOD_clientVersion, value);
	}

	/**
	 * <i><l>RestMethodContext</l> configuration property:&emsp;</i>  Debug mode.
	 *
	 * <p>
	 * Enables debugging on this method.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestMethodContext#RESTMETHOD_clientVersion}
	 * </ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestMethodContextBuilder debug(Enablement value) {
		return set(RESTMETHOD_debug, value);
	}

	/**
	 * <i><l>RestMethodContext</l> configuration property:&emsp;</i>  Default form data parameters.
	 *
	 * <p>
	 * Adds a single default form data parameter.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestMethodContext#RESTMETHOD_defaultFormData}
	 * </ul>
	 *
	 * @param name The form data parameter name.
	 * @param value The form data parameter value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestMethodContextBuilder defaultFormData(String name, Object value) {
		return defaultFormData(BasicNameValuePair.of(name, value));
	}

	/**
	 * <i><l>RestMethodContext</l> configuration property:&emsp;</i>  Default form data parameters.
	 *
	 * <p>
	 * Adds a single default form data parameter.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestMethodContext#RESTMETHOD_defaultFormData}
	 * </ul>
	 *
	 * @param name The form data parameter name.
	 * @param value The form data parameter value supplier.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestMethodContextBuilder defaultFormData(String name, Supplier<?> value) {
		return defaultFormData(BasicNameValuePair.of(name, value));
	}

	/**
	 * <i><l>RestMethodContext</l> configuration property:&emsp;</i>  Default form data parameters.
	 *
	 * <p>
	 * Specifies default values for form data parameters if they're not specified in the request body.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestMethodContext#RESTMETHOD_defaultFormData}
	 * </ul>
	 *
	 * @param values The form data parameters to add.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestMethodContextBuilder defaultFormData(NameValuePair...values) {
		asList(values).stream().forEach(x -> appendTo(RESTMETHOD_defaultFormData, x));
		return this;
	}

	/**
	 * <i><l>RestMethodContext</l> configuration property:&emsp;</i>  Default query parameters.
	 *
	 * <p>
	 * Adds a single default query parameter.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestMethodContext#RESTMETHOD_defaultQuery}
	 * </ul>
	 *
	 * @param name The query parameter name.
	 * @param value The query parameter value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestMethodContextBuilder defaultQuery(String name, Object value) {
		return defaultQuery(BasicNameValuePair.of(name, value));
	}

	/**
	 * <i><l>RestMethodContext</l> configuration property:&emsp;</i>  Default query parameters.
	 *
	 * <p>
	 * Adds a single default query parameter.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestMethodContext#RESTMETHOD_defaultQuery}
	 * </ul>
	 *
	 * @param name The query parameter name.
	 * @param value The query parameter value supplier.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestMethodContextBuilder defaultQuery(String name, Supplier<?> value) {
		return defaultQuery(BasicNameValuePair.of(name, value));
	}

	/**
	 * <i><l>RestMethodContext</l> configuration property:&emsp;</i>  Default query parameters.
	 *
	 * <p>
	 * Specifies default values for query parameters if they're not specified on the request.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestMethodContext#RESTMETHOD_defaultQuery}
	 * </ul>
	 *
	 * @param values The query parameters to add.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestMethodContextBuilder defaultQuery(NameValuePair...values) {
		asList(values).stream().forEach(x -> appendTo(RESTMETHOD_defaultQuery, x));
		return this;
	}

	/**
	 * <i><l>RestMethodContext</l> configuration property:&emsp;</i>  Default request attributes.
	 *
	 * <p>
	 * Adds a single default request attribute.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestMethodContext#RESTMETHOD_defaultRequestAttributes}
	 * </ul>
	 *
	 * @param name The attribute name.
	 * @param value The attribute value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestMethodContextBuilder defaultRequestAttribute(String name, Object value) {
		return defaultRequestAttributes(BasicNamedAttribute.of(name, value));
	}

	/**
	 * <i><l>RestMethodContext</l> configuration property:&emsp;</i>  Default request attributes.
	 *
	 * <p>
	 * Adds a single default request attribute.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestMethodContext#RESTMETHOD_defaultRequestAttributes}
	 * </ul>
	 *
	 * @param name The attribute name.
	 * @param value The attribute value supplier.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestMethodContextBuilder defaultRequestAttribute(String name, Supplier<?> value) {
		return defaultRequestAttributes(BasicNamedAttribute.of(name, value));
	}

	/**
	 * <i><l>RestMethodContext</l> configuration property:&emsp;</i>  Default request attributes.
	 *
	 * <p>
	 * Adds multiple default request attributes.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestMethodContext#RESTMETHOD_defaultRequestAttributes}
	 * </ul>
	 *
	 * @param values The request attributes to add.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestMethodContextBuilder defaultRequestAttributes(NamedAttribute...values) {
		asList(values).stream().forEach(x -> appendTo(RESTMETHOD_defaultRequestAttributes, x));
		return this;
	}

	/**
	 * <i><l>RestMethodContext</l> configuration property:&emsp;</i>  Default request headers.
	 *
	 * <p>
	 * Adds a single default request header.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestMethodContext#RESTMETHOD_defaultRequestHeaders}
	 * </ul>
	 *
	 * @param name The request header name.
	 * @param value The request header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestMethodContextBuilder defaultRequestHeader(String name, Object value) {
		return defaultRequestHeaders(BasicHeader.of(name, value));
	}

	/**
	 * <i><l>RestMethodContext</l> configuration property:&emsp;</i>  Default request headers.
	 *
	 * <p>
	 * Adds a single default request header.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestMethodContext#RESTMETHOD_defaultRequestHeaders}
	 * </ul>
	 *
	 * @param name The request header name.
	 * @param value The request header value supplier.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestMethodContextBuilder defaultRequestHeader(String name, Supplier<?> value) {
		return defaultRequestHeaders(BasicHeader.of(name, value));
	}

	/**
	 * <i><l>RestMethodContext</l> configuration property:&emsp;</i>  Default request headers.
	 *
	 * <p>
	 * Specifies default values for request headers if they're not passed in through the request.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestMethodContext#RESTMETHOD_defaultRequestHeaders}
	 * </ul>
	 *
	 * @param values The headers to add.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestMethodContextBuilder defaultRequestHeaders(Header...values) {
		asList(values).stream().forEach(x -> appendTo(RESTMETHOD_defaultRequestHeaders, x));
		return this;
	}

	/**
	 * <i><l>RestMethodContext</l> configuration property:&emsp;</i>  Default response headers.
	 *
	 * <p>
	 * Adds a single default response header.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestMethodContext#RESTMETHOD_defaultResponseHeaders}
	 * </ul>
	 *
	 * @param name The response header name.
	 * @param value The response header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestMethodContextBuilder defaultResponseHeader(String name, Object value) {
		return defaultResponseHeaders(BasicHeader.of(name, value));
	}

	/**
	 * <i><l>RestMethodContext</l> configuration property:&emsp;</i>  Default response headers.
	 *
	 * <p>
	 * Adds a single default response header.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestMethodContext#RESTMETHOD_defaultResponseHeaders}
	 * </ul>
	 *
	 * @param name The response header name.
	 * @param value The response header value supplier.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestMethodContextBuilder defaultResponseHeader(String name, Supplier<?> value) {
		return defaultResponseHeaders(BasicHeader.of(name, value));
	}

	/**
	 * <i><l>RestMethodContext</l> configuration property:&emsp;</i>  Default response headers.
	 *
	 * <p>
	 * Specifies default values for response headers if they're not set after the Java REST method is called.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestMethodContext#RESTMETHOD_defaultResponseHeaders}
	 * </ul>
	 *
	 * @param values The headers to add.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestMethodContextBuilder defaultResponseHeaders(Header...values) {
		asList(values).stream().forEach(x -> appendTo(RESTMETHOD_defaultResponseHeaders, x));
		return this;
	}

	/**
	 * Configuration property:  HTTP method name.
	 *
	 * <p>
	 * REST method name.
	 *
	 * <p>
	 * Typically <js>"GET"</js>, <js>"PUT"</js>, <js>"POST"</js>, <js>"DELETE"</js>, or <js>"OPTIONS"</js>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestMethodContext#RESTMETHOD_httpMethod}
	 * </ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestMethodContextBuilder httpMethod(String value) {
		return set(RESTMETHOD_httpMethod, value);
	}

	/**
	 * Configuration property:  Method-level matchers.
	 *
	 * <p>
	 * Associates one or more {@link RestMatcher RestMatchers} with the specified method.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestMethodContext#RESTMETHOD_matchers}
	 * </ul>
	 *
	 * @param values The new values for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestMethodContextBuilder matchers(RestMatcher...values) {
		return set(RESTMETHOD_matchers, values);
	}

	/**
	 * Configuration property:  Resource method paths.
	 *
	 * <p>
	 * Identifies the URL subpath relative to the servlet class.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestMethodContext#RESTMETHOD_path}
	 * </ul>
	 *
	 * @param values The new values for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestMethodContextBuilder path(String...values) {
		return set(RESTMETHOD_path, values);
	}

	/**
	 * Configuration property:  Priority.
	 *
	 * <p>
	 * URL path pattern priority.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestMethodContext#RESTMETHOD_priority}
	 * </ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestMethodContextBuilder priority(int value) {
		return set(RESTMETHOD_priority, value);
	}

	// <FluentSetters>

	@Override /* GENERATED - ContextBuilder */
	public RestMethodContextBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestMethodContextBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestMethodContextBuilder appendTo(String name, Object value) {
		super.appendTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestMethodContextBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestMethodContextBuilder applyAnnotations(java.lang.Class<?>...fromClasses) {
		super.applyAnnotations(fromClasses);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestMethodContextBuilder applyAnnotations(Method...fromMethods) {
		super.applyAnnotations(fromMethods);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestMethodContextBuilder applyAnnotations(AnnotationList al, VarResolverSession r) {
		super.applyAnnotations(al, r);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestMethodContextBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestMethodContextBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestMethodContextBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestMethodContextBuilder prependTo(String name, Object value) {
		super.prependTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestMethodContextBuilder putAllTo(String name, Object value) {
		super.putAllTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestMethodContextBuilder putTo(String name, String key, Object value) {
		super.putTo(name, key, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestMethodContextBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestMethodContextBuilder set(String name) {
		super.set(name);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestMethodContextBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestMethodContextBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestMethodContextBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestMethodContextBuilder unset(String name) {
		super.unset(name);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder annotations(Annotation...values) {
		super.annotations(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder beanInterceptor(Class<?> on, Class<? extends org.apache.juneau.transform.BeanInterceptor<?>> value) {
		super.beanInterceptor(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder beanMapPutReturnsOldValue() {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder beanProperties(Map<String,Object> values) {
		super.beanProperties(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder beanProperties(Class<?> beanClass, String properties) {
		super.beanProperties(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder beanProperties(String beanClassName, String properties) {
		super.beanProperties(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder beanPropertiesExcludes(Map<String,Object> values) {
		super.beanPropertiesExcludes(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder beanPropertiesExcludes(Class<?> beanClass, String properties) {
		super.beanPropertiesExcludes(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder beanPropertiesExcludes(String beanClassName, String properties) {
		super.beanPropertiesExcludes(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder beanPropertiesReadOnly(Map<String,Object> values) {
		super.beanPropertiesReadOnly(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder beanPropertiesReadOnly(Class<?> beanClass, String properties) {
		super.beanPropertiesReadOnly(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder beanPropertiesReadOnly(String beanClassName, String properties) {
		super.beanPropertiesReadOnly(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder beanPropertiesWriteOnly(Map<String,Object> values) {
		super.beanPropertiesWriteOnly(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder beanPropertiesWriteOnly(Class<?> beanClass, String properties) {
		super.beanPropertiesWriteOnly(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder beanPropertiesWriteOnly(String beanClassName, String properties) {
		super.beanPropertiesWriteOnly(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder beansRequireDefaultConstructor() {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder beansRequireSerializable() {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder beansRequireSettersForGetters() {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder dictionary(Object...values) {
		super.dictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
		super.dictionaryOn(on, values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder disableBeansRequireSomeProperties() {
		super.disableBeansRequireSomeProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder disableIgnoreMissingSetters() {
		super.disableIgnoreMissingSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder disableIgnoreTransientFields() {
		super.disableIgnoreTransientFields();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder disableIgnoreUnknownNullBeanProperties() {
		super.disableIgnoreUnknownNullBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder disableInterfaceProxies() {
		super.disableInterfaceProxies();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> RestMethodContextBuilder example(Class<T> pojoClass, T o) {
		super.example(pojoClass, o);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> RestMethodContextBuilder example(Class<T> pojoClass, String json) {
		super.example(pojoClass, json);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder findFluentSetters() {
		super.findFluentSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder findFluentSetters(Class<?> on) {
		super.findFluentSetters(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder ignoreInvocationExceptionsOnGetters() {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder ignoreInvocationExceptionsOnSetters() {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder ignoreUnknownBeanProperties() {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder implClass(Class<?> interfaceClass, Class<?> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder implClasses(Map<Class<?>,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder interfaceClass(Class<?> on, Class<?> value) {
		super.interfaceClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder interfaces(java.lang.Class<?>...value) {
		super.interfaces(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder notBeanPackages(Object...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder sortProperties(java.lang.Class<?>...on) {
		super.sortProperties(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder stopClass(Class<?> on, Class<?> value) {
		super.stopClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder swaps(Object...values) {
		super.swaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder typeName(Class<?> on, String value) {
		super.typeName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder typePropertyName(String value) {
		super.typePropertyName(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder typePropertyName(Class<?> on, String value) {
		super.typePropertyName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder useEnumNames() {
		super.useEnumNames();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestMethodContextBuilder useJavaBeanIntrospector() {
		super.useJavaBeanIntrospector();
		return this;
	}

	// </FluentSetters>
}