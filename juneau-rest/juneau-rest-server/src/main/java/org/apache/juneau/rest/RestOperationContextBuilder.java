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

import static org.apache.juneau.rest.HttpRuntimeException.*;
import java.lang.annotation.*;
import java.util.*;
import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.svl.*;
import java.lang.reflect.Method;

/**
 * Builder class for {@link RestOperationContext} objects.
 */
@FluentSetters
public class RestOperationContextBuilder extends BeanContextBuilder {

	RestContext restContext;
	Method restMethod;
	String httpMethod, clientVersion;
	Enablement debug;
	List<String> path;
	PartListBuilder defaultFormData, defaultQueryData;
	NamedAttributeList defaultRequestAttributes;
	HeaderListBuilder defaultRequestHeaders, defaultResponseHeaders;
	RestMatcherListBuilder restMatchers;

	private BeanStore beanStore;

	@Override /* ContextBuilder */
	public RestOperationContextBuilder copy() {
		throw new NoSuchMethodError("Not implemented.");
	}

	@SuppressWarnings("unchecked")
	@Override /* BeanContextBuilder */
	public RestOperationContext build() {
		try {
			Class<? extends RestOperationContext> ic = (Class<? extends RestOperationContext>) getContextClass().orElse(getDefaultImplClass());
			return BeanStore.of(beanStore).addBean(RestOperationContextBuilder.class, this).createBean(ic);
		} catch (Exception e) {
			throw toHttpException(e, InternalServerError.class);
		}
	}

	/**
	 * Specifies the default implementation class if not specified via {@link #contextClass(Class)}.
	 *
	 * @return The default implementation class if not specified via {@link #contextClass(Class)}.
	 */
	protected Class<? extends RestOperationContext> getDefaultImplClass() {
		return RestOperationContext.class;
	}

	RestOperationContextBuilder(java.lang.reflect.Method method, RestContext context) {

		this.restContext = context;
		this.restMethod = method;
		this.beanStore = context.getRootBeanStore();
		this.defaultFormData = PartList.create();
		this.defaultQueryData = PartList.create();
		this.defaultRequestAttributes = NamedAttributeList.create();
		this.defaultRequestHeaders = HeaderList.create();
		this.defaultResponseHeaders = HeaderList.create();
		this.restMatchers = RestMatcherList.create();

		MethodInfo mi = MethodInfo.of(context.getResourceClass(), method);

		try {

			VarResolver vr = context.getVarResolver();
			VarResolverSession vrs = vr.createSession();

			applyAnnotations(mi.getAnnotationList(ContextApplyFilter.INSTANCE), vrs);

		} catch (Exception e) {
			throw toHttpException(e, InternalServerError.class);
		}
	}

	/**
	 * When enabled, append <js>"/*"</js> to path patterns if not already present.
	 *
	 * @return This object (for method chaining).
	 */
	public RestOperationContextBuilder dotAll() {
		set("RestOperationContext.dotAll.b", true);
		return this;
	}

	/**
	 * Specifies a {@link BeanStore} to use when resolving constructor arguments.
	 *
	 * @param beanStore The bean store to use for resolving constructor arguments.
	 * @return This object (for method chaining).
	 */
	public RestOperationContextBuilder beanStore(BeanStore beanStore) {
		this.beanStore = beanStore;
		return this;
	}

	//----------------------------------------------------------------------------------------------------
	// Properties
	//----------------------------------------------------------------------------------------------------

	/**
	 * Client version pattern matcher.
	 *
	 * <p>
	 * Specifies whether this method can be called based on the client version.
	 *
	 * <p>
	 * The client version is identified via the HTTP request header identified by
	 * {@link Rest#clientVersionHeader() @Rest(clientVersionHeader)} which by default is <js>"Client-Version"</js>.
	 *
	 * <p>
	 * This is a specialized kind of {@link RestMatcher} that allows you to invoke different Java methods for the same
	 * method/path based on the client version.
	 *
	 * <p>
	 * The format of the client version range is similar to that of OSGi versions.
	 *
	 * <p>
	 * In the following example, the Java methods are mapped to the same HTTP method and URL <js>"/foobar"</js>.
	 * <p class='bcode w800'>
	 * 	<jc>// Call this method if Client-Version is at least 2.0.
	 * 	// Note that this also matches 2.0.1.</jc>
	 * 	<ja>@RestGet</ja>(path=<js>"/foobar"</js>, clientVersion=<js>"2.0"</js>)
	 * 	<jk>public</jk> Object method1()  {...}
	 *
	 * 	<jc>// Call this method if Client-Version is at least 1.1, but less than 2.0.</jc>
	 * 	<ja>@RestGet</ja>(path=<js>"/foobar"</js>, clientVersion=<js>"[1.1,2.0)"</js>)
	 * 	<jk>public</jk> Object method2()  {...}
	 *
	 * 	<jc>// Call this method if Client-Version is less than 1.1.</jc>
	 * 	<ja>@RestGet</ja>(path=<js>"/foobar"</js>, clientVersion=<js>"[0,1.1)"</js>)
	 * 	<jk>public</jk> Object method3()  {...}
	 * </p>
	 *
	 * <p>
	 * It's common to combine the client version with transforms that will convert new POJOs into older POJOs for
	 * backwards compatibility.
	 * <p class='bcode w800'>
	 * 	<jc>// Call this method if Client-Version is at least 2.0.</jc>
	 * 	<ja>@RestGet</ja>(path=<js>"/foobar"</js>, clientVersion=<js>"2.0"</js>)
	 * 	<jk>public</jk> NewPojo newMethod()  {...}
	 *
	 * 	<jc>// Call this method if Client-Version is at least 1.1, but less than 2.0.</jc>
	 * 	<ja>@RestGet</ja>(path=<js>"/foobar"</js>, clientVersion=<js>"[1.1,2.0)"</js>, transforms={NewToOldPojoSwap.<jk>class</jk>})
	 * 	<jk>public</jk> NewPojo oldMethod() {
	 * 		<jk>return</jk> newMethod();
	 * 	}
	 *
	 * <p>
	 * Note that in the previous example, we're returning the exact same POJO, but using a transform to convert it into
	 * an older form.
	 * The old method could also just return back a completely different object.
	 * The range can be any of the following:
	 * <ul>
	 * 	<li><js>"[0,1.0)"</js> = Less than 1.0.  1.0 and 1.0.0 does not match.
	 * 	<li><js>"[0,1.0]"</js> = Less than or equal to 1.0.  Note that 1.0.1 will match.
	 * 	<li><js>"1.0"</js> = At least 1.0.  1.0 and 2.0 will match.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link RestOp#clientVersion}
	 * 	<li class='ja'>{@link RestGet#clientVersion}
	 * 	<li class='ja'>{@link RestPut#clientVersion}
	 * 	<li class='ja'>{@link RestPost#clientVersion}
	 * 	<li class='ja'>{@link RestDelete#clientVersion}
	 * 	<li class='jf'>{@link RestContext#REST_clientVersionHeader}
	 * </ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestOperationContextBuilder clientVersion(String value) {
		clientVersion = value;
		return this;
	}

	@Override
	@FluentSetter
	public RestOperationContextBuilder contextClass(Class<? extends Context> value) {
		super.contextClass(value);
		return this;
	}

	/**
	 * Debug mode.
	 *
	 * <p>
	 * Enables the following:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		HTTP request/response bodies are cached in memory for logging purposes.
	 * </ul>
	 *
	 * <p>
	 * If not sppecified, the debug enablement is inherited from the class context.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestOperationContextBuilder debug(Enablement value) {
		debug = value;
		return this;
	}

	/**
	 * Default form data parameters.
	 *
	 * <p>
	 * Sets default values for form data parameters.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestPost</ja>(path=<js>"/*"</js>, defaultFormData={<js>"foo=bar"</js>})
	 * 	<jk>public</jk> String doGet(<ja>@FormData</ja>(<js>"foo"</js>) String <jv>foo</jv>)  {...}
	 * </p>

	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link RestOp#defaultFormData}
	 * 	<li class='ja'>{@link RestPost#defaultFormData}
	 * </ul>
	 *
	 * @param values The form data parameters to add.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestOperationContextBuilder defaultFormData(NameValuePair...values) {
		defaultFormData.setDefault(values);
		return this;
	}

	/**
	 * Default query parameters.
	 *
	 * <p>
	 * Sets default values for query data parameters.
	 *
	 * <p>
	 * Affects values returned by {@link RestRequest#getQueryParam(String)} when the parameter is not present on the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestGet</ja>(path=<js>"/*"</js>, defaultQueryData={<js>"foo=bar"</js>})
	 * 	<jk>public</jk> String doGet(<ja>@Query</ja>(<js>"foo"</js>) String <jv>foo</jv>)  {...}
	 * </p>

	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link RestOp#defaultQueryData}
	 * 	<li class='ja'>{@link RestGet#defaultQueryData}
	 * 	<li class='ja'>{@link RestPut#defaultQueryData}
	 * 	<li class='ja'>{@link RestPost#defaultQueryData}
	 * 	<li class='ja'>{@link RestDelete#defaultQueryData}
	 * </ul>
	 *
	 * @param values The query parameters to add.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestOperationContextBuilder defaultQueryData(NameValuePair...values) {
		defaultQueryData.setDefault(values);
		return this;
	}

	/**
	 * Default request attributes.
	 *
	 * <p>
	 * Specifies default values for request attributes if they are not already set on the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Assume "text/json" Accept value when Accept not specified</jc>
	 * 	<ja>@RestGet</ja>(path=<js>"/*"</js>, defaultRequestAttributes={<js>"Foo=bar"</js>})
	 * 	<jk>public</jk> String doGet()  {...}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link RestOp#defaultRequestAttributes()}
	 * 	<li class='ja'>{@link RestGet#defaultRequestAttributes()}
	 * 	<li class='ja'>{@link RestPut#defaultRequestAttributes()}
	 * 	<li class='ja'>{@link RestPost#defaultRequestAttributes()}
	 * 	<li class='ja'>{@link RestDelete#defaultRequestAttributes()}
	 * </ul>
	 *
	 * @param values The request attributes to add.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestOperationContextBuilder defaultRequestAttributes(NamedAttribute...values) {
		defaultRequestAttributes.append(values);
		return this;
	}

	/**
	 * <i><l>RestOperationContext</l> configuration property:&emsp;</i>  Default request headers.
	 *
	 * <p>
	 * Specifies default values for request headers if they're not passed in through the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Assume "text/json" Accept value when Accept not specified</jc>
	 * 	<ja>@RestGet</ja>(path=<js>"/*"</js>, defaultRequestHeaders={<js>"Accept: text/json"</js>})
	 * 	<jk>public</jk> String doGet()  {...}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link RestOp#defaultRequestHeaders}
	 * 	<li class='ja'>{@link RestGet#defaultRequestHeaders}
	 * 	<li class='ja'>{@link RestPut#defaultRequestHeaders}
	 * 	<li class='ja'>{@link RestPost#defaultRequestHeaders}
	 * 	<li class='ja'>{@link RestDelete#defaultRequestHeaders}
	 * </ul>
	 *
	 * @param values The headers to add.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestOperationContextBuilder defaultRequestHeaders(Header...values) {
		defaultRequestHeaders.setDefault(values);
		return this;
	}

	/**
	 * Default response headers.
	 *
	 * <p>
	 * Specifies default values for response headers if they're not set after the Java REST method is called.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Assume "text/json" Accept value when Accept not specified</jc>
	 * 	<ja>@RestGet</ja>(path=<js>"/*"</js>, defaultResponseHeaders={<js>"Content-Type: text/json"</js>})
	 * 	<jk>public</jk> String doGet()  {...}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link RestOp#defaultResponseHeaders}
	 * 	<li class='ja'>{@link RestGet#defaultResponseHeaders}
	 * 	<li class='ja'>{@link RestPut#defaultResponseHeaders}
	 * 	<li class='ja'>{@link RestPost#defaultResponseHeaders}
	 * 	<li class='ja'>{@link RestDelete#defaultResponseHeaders}
	 * </ul>
	 *
	 * @param values The headers to add.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestOperationContextBuilder defaultResponseHeaders(Header...values) {
		defaultResponseHeaders.setDefault(values);
		return this;
	}

	/**
	 * HTTP method name.
	 *
	 * <p>
	 * Typically <js>"GET"</js>, <js>"PUT"</js>, <js>"POST"</js>, <js>"DELETE"</js>, or <js>"OPTIONS"</js>.
	 *
	 * <p>
	 * Method names are case-insensitive (always folded to upper-case).
	 *
	 * <p>
	 * Note that you can use {@link org.apache.juneau.http.HttpMethod} for constant values.
	 *
	 * <p>
	 * Besides the standard HTTP method names, the following can also be specified:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<js>"*"</js>
	 * 		- Denotes any method.
	 * 		<br>Use this if you want to capture any HTTP methods in a single Java method.
	 * 		<br>The {@link org.apache.juneau.rest.annotation.Method @Method} annotation and/or {@link RestRequest#getMethod()} method can be used to
	 * 		distinguish the actual HTTP method name.
	 * 	<li>
	 * 		<js>""</js>
	 * 		- Auto-detect.
	 * 		<br>The method name is determined based on the Java method name.
	 * 		<br>For example, if the method is <c>doPost(...)</c>, then the method name is automatically detected
	 * 		as <js>"POST"</js>.
	 * 		<br>Otherwise, defaults to <js>"GET"</js>.
	 * 	<li>
	 * 		<js>"RRPC"</js>
	 * 		- Remote-proxy interface.
	 * 		<br>This denotes a Java method that returns an object (usually an interface, often annotated with the
	 * 		{@link Remote @Remote} annotation) to be used as a remote proxy using
	 * 		<c>RestClient.getRemoteInterface(Class&lt;T&gt; interfaceClass, String url)</c>.
	 * 		<br>This allows you to construct client-side interface proxies using REST as a transport medium.
	 * 		<br>Conceptually, this is simply a fancy <c>POST</c> against the url <js>"/{path}/{javaMethodName}"</js>
	 * 		where the arguments are marshalled from the client to the server as an HTTP body containing an array of
	 * 		objects, passed to the method as arguments, and then the resulting object is marshalled back to the client.
	 * 	<li>
	 * 		Anything else
	 * 		- Overloaded non-HTTP-standard names that are passed in through a <c>&amp;method=methodName</c> URL
	 * 		parameter.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link RestOp#method()}
	 * 	<li class='ja'>{@link RestGet}
	 * 	<li class='ja'>{@link RestPut}
	 * 	<li class='ja'>{@link RestPost}
	 * 	<li class='ja'>{@link RestDelete}
	 * </ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestOperationContextBuilder httpMethod(String value) {
		this.httpMethod = value;
		return this;
	}

	/**
	 * Method-level matchers.
	 *
	 * <p>
	 * Associates one or more {@link RestMatcher RestMatchers} with the specified method.
	 *
	 * <p>
	 * If multiple matchers are specified, <b>ONE</b> matcher must pass.
	 * <br>Note that this is different than guards where <b>ALL</b> guards needs to pass.
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link RestOp#matchers()}
	 * 	<li class='ja'>{@link RestGet#matchers()}
	 * 	<li class='ja'>{@link RestPut#matchers()}
	 * 	<li class='ja'>{@link RestPost#matchers()}
	 * 	<li class='ja'>{@link RestDelete#matchers()}
	 * </ul>
	 *
	 * @param values The new values for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestOperationContextBuilder matchers(RestMatcher...values) {
		restMatchers.append(values);
		return this;
	}

	/**
	 * Method-level matchers.
	 *
	 * <p>
	 * Associates one or more {@link RestMatcher RestMatchers} with the specified method.
	 *
	 * <p>
	 * If multiple matchers are specified, <b>ONE</b> matcher must pass.
	 * <br>Note that this is different than guards where <b>ALL</b> guards needs to pass.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		When defined as a class, the implementation must have one of the following constructors:
	 * 		<ul>
	 * 			<li><code><jk>public</jk> T(RestContext)</code>
	 * 			<li><code><jk>public</jk> T()</code>
	 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>(RestContext)</code>
	 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>()</code>
	 * 		</ul>
	 * 	<li>
	 * 		Inner classes of the REST resource class are allowed.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link RestOp#matchers()}
	 * 	<li class='ja'>{@link RestGet#matchers()}
	 * 	<li class='ja'>{@link RestPut#matchers()}
	 * 	<li class='ja'>{@link RestPost#matchers()}
	 * 	<li class='ja'>{@link RestDelete#matchers()}
	 * </ul>
	 *
	 * @param values The new values for this setting.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("unchecked")
	@FluentSetter
	public RestOperationContextBuilder matchers(Class<? extends RestMatcher>...values) {
		restMatchers.append(values);
		return this;
	}

	/**
	 * Configuration property:  Resource method paths.
	 *
	 * <p>
	 * Identifies the URL subpath relative to the servlet class.
	 *
	 * <p>
	 * <ul class='notes'>
	 * 	<li>
	 * 		This method is only applicable for Java methods.
	 * 	<li>
	 * 		Slashes are trimmed from the path ends.
	 * 		<br>As a convention, you may want to start your path with <js>'/'</js> simple because it make it easier to read.
	 * </ul>
	 *
	 * @param values The new values for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestOperationContextBuilder path(String...values) {
		if (path == null)
			path = new ArrayList<>(Arrays.asList(values));
		else
			path.addAll(0, Arrays.asList(values));
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - ContextBuilder */
	public RestOperationContextBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOperationContextBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOperationContextBuilder appendTo(String name, Object value) {
		super.appendTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOperationContextBuilder apply(ContextProperties copyFrom) {
		super.apply(copyFrom);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOperationContextBuilder applyAnnotations(java.lang.Class<?>...fromClasses) {
		super.applyAnnotations(fromClasses);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOperationContextBuilder applyAnnotations(Method...fromMethods) {
		super.applyAnnotations(fromMethods);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOperationContextBuilder applyAnnotations(AnnotationList al, VarResolverSession r) {
		super.applyAnnotations(al, r);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOperationContextBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOperationContextBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOperationContextBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOperationContextBuilder prependTo(String name, Object value) {
		super.prependTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOperationContextBuilder putAllTo(String name, Object value) {
		super.putAllTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOperationContextBuilder putTo(String name, String key, Object value) {
		super.putTo(name, key, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOperationContextBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOperationContextBuilder set(String name) {
		super.set(name);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOperationContextBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOperationContextBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOperationContextBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOperationContextBuilder unset(String name) {
		super.unset(name);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder annotations(Annotation...values) {
		super.annotations(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder beanInterceptor(Class<?> on, Class<? extends org.apache.juneau.transform.BeanInterceptor<?>> value) {
		super.beanInterceptor(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder beanMapPutReturnsOldValue() {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder beanProperties(Map<String,Object> values) {
		super.beanProperties(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder beanProperties(Class<?> beanClass, String properties) {
		super.beanProperties(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder beanProperties(String beanClassName, String properties) {
		super.beanProperties(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder beanPropertiesExcludes(Map<String,Object> values) {
		super.beanPropertiesExcludes(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder beanPropertiesExcludes(Class<?> beanClass, String properties) {
		super.beanPropertiesExcludes(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder beanPropertiesExcludes(String beanClassName, String properties) {
		super.beanPropertiesExcludes(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder beanPropertiesReadOnly(Map<String,Object> values) {
		super.beanPropertiesReadOnly(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder beanPropertiesReadOnly(Class<?> beanClass, String properties) {
		super.beanPropertiesReadOnly(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder beanPropertiesReadOnly(String beanClassName, String properties) {
		super.beanPropertiesReadOnly(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder beanPropertiesWriteOnly(Map<String,Object> values) {
		super.beanPropertiesWriteOnly(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder beanPropertiesWriteOnly(Class<?> beanClass, String properties) {
		super.beanPropertiesWriteOnly(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder beanPropertiesWriteOnly(String beanClassName, String properties) {
		super.beanPropertiesWriteOnly(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder beansRequireDefaultConstructor() {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder beansRequireSerializable() {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder beansRequireSettersForGetters() {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder dictionary(Object...values) {
		super.dictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
		super.dictionaryOn(on, values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder disableBeansRequireSomeProperties() {
		super.disableBeansRequireSomeProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder disableIgnoreMissingSetters() {
		super.disableIgnoreMissingSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder disableIgnoreTransientFields() {
		super.disableIgnoreTransientFields();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder disableIgnoreUnknownNullBeanProperties() {
		super.disableIgnoreUnknownNullBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder disableInterfaceProxies() {
		super.disableInterfaceProxies();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> RestOperationContextBuilder example(Class<T> pojoClass, T o) {
		super.example(pojoClass, o);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> RestOperationContextBuilder example(Class<T> pojoClass, String json) {
		super.example(pojoClass, json);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder findFluentSetters() {
		super.findFluentSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder findFluentSetters(Class<?> on) {
		super.findFluentSetters(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder ignoreInvocationExceptionsOnGetters() {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder ignoreInvocationExceptionsOnSetters() {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder ignoreUnknownBeanProperties() {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder implClass(Class<?> interfaceClass, Class<?> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder implClasses(Map<Class<?>,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder interfaceClass(Class<?> on, Class<?> value) {
		super.interfaceClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder interfaces(java.lang.Class<?>...value) {
		super.interfaces(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder notBeanPackages(Object...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder sortProperties(java.lang.Class<?>...on) {
		super.sortProperties(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder stopClass(Class<?> on, Class<?> value) {
		super.stopClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder swaps(Object...values) {
		super.swaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder typeName(Class<?> on, String value) {
		super.typeName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder typePropertyName(String value) {
		super.typePropertyName(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder typePropertyName(Class<?> on, String value) {
		super.typePropertyName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder useEnumNames() {
		super.useEnumNames();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOperationContextBuilder useJavaBeanIntrospector() {
		super.useJavaBeanIntrospector();
		return this;
	}

	// </FluentSetters>
}