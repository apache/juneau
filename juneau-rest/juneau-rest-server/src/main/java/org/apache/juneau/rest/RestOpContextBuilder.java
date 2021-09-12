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
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.rest.HttpRuntimeException.*;
import java.lang.annotation.*;
import java.util.*;
import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.converters.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.uon.*;

import java.lang.reflect.Method;
import java.nio.charset.*;

/**
 * Builder class for {@link RestOpContext} objects.
 */
@FluentSetters
public class RestOpContextBuilder extends BeanContextBuilder {

	RestContext restContext;
	Method restMethod;
	String httpMethod, clientVersion;
	Enablement debug;
	List<String> path;
	PartListBuilder defaultFormData, defaultQueryData;
	NamedAttributeList defaultRequestAttributes;
	HeaderListBuilder defaultRequestHeaders, defaultResponseHeaders;
	RestMatcherList.Builder restMatchers;
	List<MediaType> produces, consumes;
	Set<String> roleGuard, rolesDeclared;
	RestGuardList.Builder guards = RestGuardList.create();
	RestConverterList.Builder converters = RestConverterList.create();
	EncoderGroup.Builder encoders;
	SerializerGroup.Builder serializers;
	ParserGroup.Builder parsers;
	HttpPartSerializer.Creator partSerializer;
	HttpPartParser.Creator partParser;
	boolean dotAll;

	Charset defaultCharset;
	Long maxInput;

	private BeanStore beanStore;

	@Override /* ContextBuilder */
	public RestOpContextBuilder copy() {
		throw new NoSuchMethodError("Not implemented.");
	}

	@SuppressWarnings("unchecked")
	@Override /* BeanContextBuilder */
	public RestOpContext build() {
		try {
			Class<? extends RestOpContext> ic = (Class<? extends RestOpContext>) getContextClass().orElse(getDefaultImplClass());
			return BeanStore.of(beanStore).addBean(RestOpContextBuilder.class, this).createBean(ic);
		} catch (Exception e) {
			throw toHttpException(e, InternalServerError.class);
		}
	}

	/**
	 * Specifies the default implementation class if not specified via {@link #contextClass(Class)}.
	 *
	 * @return The default implementation class if not specified via {@link #contextClass(Class)}.
	 */
	protected Class<? extends RestOpContext> getDefaultImplClass() {
		return RestOpContext.class;
	}

	RestOpContextBuilder(java.lang.reflect.Method method, RestContext context) {

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
			AnnotationWorkList al = mi.getAnnotationList(ContextApplyFilter.INSTANCE).getWork(vrs);

			apply(al);

			if (context.builder.serializers.canApply(al))
				getSerializers().apply(al);
			if (context.builder.parsers.canApply(al))
				getParsers().apply(al);
			if (context.builder.partSerializer().canApply(al))
				getPartSerializer().apply(al);
			if (context.builder.partParser.canApply(al))
				getPartParser().apply(al);

		} catch (Exception e) {
			throw toHttpException(e, InternalServerError.class);
		}
	}

	/**
	 * When enabled, append <js>"/*"</js> to path patterns if not already present.
	 *
	 * @return This object (for method chaining).
	 */
	public RestOpContextBuilder dotAll() {
		dotAll = true;
		return this;
	}

	/**
	 * Specifies a {@link BeanStore} to use when resolving constructor arguments.
	 *
	 * @param beanStore The bean store to use for resolving constructor arguments.
	 * @return This object (for method chaining).
	 */
	public RestOpContextBuilder beanStore(BeanStore beanStore) {
		this.beanStore = beanStore;
		return this;
	}

	/**
	 * Returns the serializer group builder containing the serializers for marshalling POJOs into response bodies.
	 *
	 * <p>
	 * This method can be used to override serializers defined at the class level via {@link RestContextBuilder#getSerializers()}.
	 * On first call, the builder from the class context is copied into a modifiable builder for this method.
	 * If never called, then the builder from the class context is used.
	 *
	 * <p>
	 * The builder is initialized with serializers defined via the {@link RestOp#serializers()} (and related) annotation.
	 * That annotation is applied from parent-to-child order with child entries given priority over parent entries.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestSerializers}
	 * </ul>
	 *
	 * @return The serializer group builder for this context builder.
	 */
	public SerializerGroup.Builder getSerializers() {
		if (serializers == null)
			serializers = restContext.builder.serializers.copy();
		return serializers;
	}

	/**
	 * Returns the parser group builder containing the parsers for converting request bodies into POJOs.
	 *
	 * <p>
	 * This method can be used to override parsers defined at the class level via {@link RestContextBuilder#getParsers()}.
	 * On first call, the builder from the class context is copied into a modifiable builder for this method.
	 * If never called, then the builder from the class context is used.
	 *
	 * <p>
	 * The builder is initialized with parsers defined via the {@link RestOp#parsers()} (and related) annotation.
	 * That annotation is applied from parent-to-child order with child entries given priority over parent entries.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestParsers}
	 * </ul>
	 *
	 * @return The parser group builder for this context builder.
	 */
	public ParserGroup.Builder getParsers() {
		if (parsers == null)
			parsers = restContext.builder.parsers.copy();
		return parsers;
	}

	/**
	 * Returns the HTTP part parser creator containing the part parser for parsing HTTP parts into POJOs.
	 *
	 * <p>
	 * The default value is {@link OpenApiParser} which allows for both plain-text and URL-Encoded-Object-Notation values.
	 * <br>If your parts contain text that can be confused with UON (e.g. <js>"(foo)"</js>), you can switch to
	 * {@link SimplePartParser} which treats everything as plain text.
	 *
	 * @return The HTTP part parser creator.
	 */
	public HttpPartParser.Creator getPartParser() {
		if (partParser == null)
			partParser = restContext.builder.partParser.copy();
		return partParser;
	}

	/**
	 * Returns the HTTP part serializer creator containing the part serializer for serializing POJOs to HTTP parts.
	 *
	 * <p>
	 * The default value is {@link OpenApiSerializer} which serializes based on OpenAPI rules, but defaults to UON notation for beans and maps, and
	 * plain text for everything else.
	 *
	 * <p>
	 * <br>Other options include:
	 * <ul>
	 * 	<li class='jc'>{@link SimplePartSerializer} - Always serializes to plain text.
	 * 	<li class='jc'>{@link UonSerializer} - Always serializers to UON.
	 * </ul>
	 *
	 * @return The HTTP part serializer creator.
	 */
	public HttpPartSerializer.Creator getPartSerializer() {
		if (partSerializer == null)
			partSerializer = restContext.builder.partSerializer().copy();
		return partSerializer;
	}

	/**
	 * Returns the parser group builder containing the parsers for converting HTTP request bodies into POJOs.
	 *
	 * <p>
	 * This method can be used to override encoders defined at the class level via {@link RestContextBuilder#getEncoders()}.
	 * On first call, the builder from the class context is copied into a modifiable builder for this method.
	 * If never called, then the builder from the class context is used.
	 *
	 * <p>
	 * The builder is initialized with encoders defined via the {@link Rest#parsers()} annotation.
	 * That annotation is applied from parent-to-child order with child entries given priority over parent entries.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestEncoders}
	 * </ul>
	 *
	 * @return The encoder group builder for this context builder.
	 */
	public EncoderGroup.Builder getEncoders() {
		if (encoders == null)
			encoders = restContext.builder.encoders.copy();
		return encoders;
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
	 * 	<li class='jm'>{@link RestContextBuilder#clientVersionHeader(String)}
	 * </ul>
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestOpContextBuilder clientVersion(String value) {
		clientVersion = value;
		return this;
	}

	@Override
	@FluentSetter
	public RestOpContextBuilder contextClass(Class<? extends Context> value) {
		super.contextClass(value);
		return this;
	}

	/**
	 * Response converters.
	 *
	 * <p>
	 * Associates one or more {@link RestConverter converters} with a resource class.
	 * <br>These converters get called immediately after execution of the REST method in the same order specified in the
	 * annotation.
	 * <br>The object passed into this converter is the object returned from the Java method or passed into
	 * the {@link RestResponse#setOutput(Object)} method.
	 *
	 * <p>
	 * Can be used for performing post-processing on the response object before serialization.
	 *
	 * <p>
	 * 	When multiple converters are specified, they're executed in the order they're specified in the annotation
	 * 	(e.g. first the results will be traversed, then the resulting node will be searched/sorted).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Our converter.</jc>
	 * 	<jk>public class</jk> MyConverter <jk>implements</jk> RestConverter {
	 * 		<ja>@Override</ja>
	 * 		<jk>public</jk> Object convert(RestRequest <jv>req</jv>, Object <jv>o</jv>) {
	 * 			<jc>// Do something with object and return another object.</jc>
	 * 			<jc>// Or just return the same object for a no-op.</jc>
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Option #1 - Registered via annotation resolving to a config file setting with default value.</jc>
	 * 	<ja>@Rest</ja>(converters={MyConverter.<jk>class</jk>})
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Registered via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			<jv>builder</jv>.converters(MyConverter.<jk>class</jk>);
	 *
	 * 			<jc>// Pass in an instance instead.</jc>
	 * 			<jv>builder</jv>.converters(<jk>new</jk> MyConverter());
	 * 		}
	 *
	 * 		<jc>// Option #3 - Registered via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 * 			<jv>builder</jv>.converters(MyConverter.<jk>class</jk>);
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		When defined as a class, the implementation must have one of the following constructors:
	 * 		<ul>
	 * 			<li><code><jk>public</jk> T(BeanContext)</code>
	 * 			<li><code><jk>public</jk> T()</code>
	 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>(RestContext)</code>
	 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>()</code>
	 * 		</ul>
	 * 	<li>
	 * 		Inner classes of the REST resource class are allowed.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jc'>{@link Traversable} - Allows URL additional path info to address individual elements in a POJO tree.
	 * 	<li class='jc'>{@link Queryable} - Allows query/view/sort functions to be performed on POJOs.
	 * 	<li class='jc'>{@link Introspectable} - Allows Java public methods to be invoked on the returned POJOs.
	 * 	<li class='ja'>{@link Rest#converters()}
	 * 	<li class='link'>{@doc RestConverters}
	 * </ul>
	 *
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException if any class does not extend from {@link RestConverter}.
	 */
	@FluentSetter
	public RestOpContextBuilder converters(Class<?>...values) {
		converters.append(assertClassArrayArgIsType("values", RestConverter.class, values));
		return this;
	}

	/**
	 * Response converters.
	 *
	 * <p>
	 * Same as {@link #converters(Class...)} except input is pre-constructed instances.
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestOpContextBuilder converters(RestConverter...values) {
		converters.append(values);
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
	public RestOpContextBuilder debug(Enablement value) {
		debug = value;
		return this;
	}

	/**
	 * Default character encoding.
	 *
	 * <p>
	 * The default character encoding for the request and response if not specified on the request.
	 *
	 * <p>
	 * This overrides the value defined on the {@link RestContext}.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContextBuilder#defaultCharset(Charset)}
	 * 	<li class='ja'>{@link Rest#defaultCharset}
	 * 	<li class='ja'>{@link RestOp#defaultCharset}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is the first value found:
	 * 	<ul>
	 * 		<li>System property <js>"RestContext.defaultCharset"
	 * 		<li>Environment variable <js>"RESTCONTEXT_defaultCharset"
	 * 		<li><js>"utf-8"</js>
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestOpContextBuilder defaultCharset(Charset value) {
		defaultCharset = value;
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
	public RestOpContextBuilder defaultFormData(NameValuePair...values) {
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
	public RestOpContextBuilder defaultQueryData(NameValuePair...values) {
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
	public RestOpContextBuilder defaultRequestAttributes(NamedAttribute...values) {
		defaultRequestAttributes.append(values);
		return this;
	}

	/**
	 * <i><l>RestOpContext</l> configuration property:&emsp;</i>  Default request headers.
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
	public RestOpContextBuilder defaultRequestHeaders(Header...values) {
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
	public RestOpContextBuilder defaultResponseHeaders(Header...values) {
		defaultResponseHeaders.setDefault(values);
		return this;
	}

	/**
	 * Guards.
	 *
	 * <p>
	 * Associates one or more {@link RestGuard RestGuards} with this method.
	 *
	 * <p>
	 * If multiple guards are specified, <b>ALL</b> guards must pass.
	 * <br>Note that this is different than matchers where only ONE matcher needs to pass.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Define a guard that only lets Billy make a request.</jc>
	 * 	<jk>public</jk> BillyGuard <jk>extends</jk> RestGuard {
	 * 		<ja>@Override</ja>
	 * 		<jk>public boolean</jk> isRequestAllowed(RestRequest <jv>req</jv>) {
	 * 			<jk>return</jk> <jv>req</jv>.getUserPrincipal().getName().equals(<js>"Billy"</js>);
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Option #1 - Registered via annotation.</jc>
	 * 	<ja>@Rest</ja>(guards={BillyGuard.<jk>class</jk>})
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Registered via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			<jv>builder</jv>.guards(BillyGuard.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Registered via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 * 			<jv>builder</jv>.guards(BillyGuard.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Override at the method level.</jc>
	 * 		<ja>@RestGet</ja>(guards={SomeOtherGuard.<jk>class</jk>})
	 * 		<jk>public</jk> Object myMethod() {...}
	 * 	}
	 * </p>
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
	 * 	<li class='link'>{@doc RestGuards}
	 * 	<li class='ja'>{@link Rest#guards()}
	 * 	<li class='ja'>{@link RestOp#guards()}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException if any class does not extend from {@link RestGuard}.
	 */
	@FluentSetter
	public RestOpContextBuilder guards(Class<?>...values) {
		guards.append(assertClassArrayArgIsType("values", RestGuard.class, values));
		return this;
	}

	/**
	 * Guards.
	 *
	 * <p>
	 * Same as {@link #guards(Class...)} except input is pre-constructed instances.
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestOpContextBuilder guards(RestGuard...values) {
		guards.append(values);
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
	public RestOpContextBuilder httpMethod(String value) {
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
	public RestOpContextBuilder matchers(RestMatcher...values) {
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
	 * @throws IllegalArgumentException if any class does not extend from {@link RestMatcher}.
	 */
	@FluentSetter
	public RestOpContextBuilder matchers(Class<?>...values) {
		restMatchers.append(assertClassArrayArgIsType("values", RestMatcher.class, values));
		return this;
	}

	/**
	 * The maximum allowed input size (in bytes) on HTTP requests.
	 *
	 * <p>
	 * Useful for alleviating DoS attacks by throwing an exception when too much input is received instead of resulting
	 * in out-of-memory errors which could affect system stability.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
	 * 	<ja>@Rest</ja>(maxInput=<js>"$C{REST/maxInput,10M}"</js>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			<jv>builder</jv>.maxInput(<js>"10M"</js>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 * 			<jv>builder</jv>.maxInput(<js>"10M"</js>);
	 * 		}
	 *
	 * 		<jc>// Override at the method level.</jc>
	 * 		<ja>@RestPost</ja>(maxInput=<js>"10M"</js>)
	 * 		<jk>public</jk> Object myMethod() {...}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		String value that gets resolved to a <jk>long</jk>.
	 * 	<li>
	 * 		Can be suffixed with any of the following representing kilobytes, megabytes, and gigabytes:
	 * 		<js>'K'</js>, <js>'M'</js>, <js>'G'</js>.
	 * 	<li>
	 * 		A value of <js>"-1"</js> can be used to represent no limit.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Rest#maxInput}
	 * 	<li class='ja'>{@link RestOp#maxInput}
	 * 	<li class='jm'>{@link RestOpContextBuilder#maxInput(String)}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is the first value found:
	 * 	<ul>
	 * 		<li>System property <js>"RestContext.maxInput"
	 * 		<li>Environment variable <js>"RESTCONTEXT_MAXINPUT"
	 * 		<li><js>"100M"</js>
	 * 	</ul>
	 * 	<br>The default is <js>"100M"</js>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestOpContextBuilder maxInput(String value) {
		maxInput = StringUtils.parseLongWithSuffix(value);
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
	public RestOpContextBuilder path(String...values) {
		if (path == null)
			path = new ArrayList<>(Arrays.asList(values));
		else
			path.addAll(0, Arrays.asList(values));
		return this;
	}

	/**
	 * Supported accept media types.
	 *
	 * <p>
	 * Overrides the media types inferred from the serializers that identify what media types can be produced by the resource.
	 * <br>An example where this might be useful if you have serializers registered that handle media types that you
	 * don't want exposed in the Swagger documentation.
	 *
	 * <p>
	 * This affects the returned values from the following:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link RestContext#getProduces() RestContext.getProduces()}
	 * 	<li class='jm'>{@link SwaggerProvider#getSwagger(RestContext,Locale)} - Affects produces field.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Rest#produces}
	 * 	<li class='ja'>{@link RestOp#produces}
	 * 	<li class='ja'>{@link RestGet#produces}
	 * 	<li class='ja'>{@link RestPut#produces}
	 * 	<li class='ja'>{@link RestPost#produces}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestOpContextBuilder produces(MediaType...values) {
		if (produces == null)
			produces = new ArrayList<>(Arrays.asList(values));
		else
			produces.addAll(Arrays.asList(values));
		return this;
	}

	/**
	 * Declared roles.
	 *
	 * <p>
	 * A comma-delimited list of all possible user roles.
	 *
	 * <p>
	 * Used in conjunction with {@link RestOpContextBuilder#roleGuard(String)} is used with patterns.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@Rest</ja>(
	 * 		rolesDeclared=<js>"ROLE_ADMIN,ROLE_READ_WRITE,ROLE_READ_ONLY,ROLE_SPECIAL"</js>,
	 * 		roleGuard=<js>"ROLE_ADMIN || (ROLE_READ_WRITE &amp;&amp; ROLE_SPECIAL)"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet {
	 * 		...
	 * 	}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Rest#rolesDeclared}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestOpContextBuilder rolesDeclared(String...values) {
		if (rolesDeclared == null)
			rolesDeclared = ASet.of(values);
		else
			rolesDeclared.addAll(asList(values));
		return this;
	}

	/**
	 * Role guard.
	 *
	 * <p>
	 * An expression defining if a user with the specified roles are allowed to access methods on this class.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@Rest</ja>(
	 * 		path=<js>"/foo"</js>,
	 * 		roleGuard=<js>"ROLE_ADMIN || (ROLE_READ_WRITE &amp;&amp; ROLE_SPECIAL)"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet {
	 * 		...
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports any of the following expression constructs:
	 * 		<ul>
	 * 			<li><js>"foo"</js> - Single arguments.
	 * 			<li><js>"foo,bar,baz"</js> - Multiple OR'ed arguments.
	 * 			<li><js>"foo | bar | bqz"</js> - Multiple OR'ed arguments, pipe syntax.
	 * 			<li><js>"foo || bar || bqz"</js> - Multiple OR'ed arguments, Java-OR syntax.
	 * 			<li><js>"fo*"</js> - Patterns including <js>'*'</js> and <js>'?'</js>.
	 * 			<li><js>"fo* &amp; *oo"</js> - Multiple AND'ed arguments, ampersand syntax.
	 * 			<li><js>"fo* &amp;&amp; *oo"</js> - Multiple AND'ed arguments, Java-AND syntax.
	 * 			<li><js>"fo* || (*oo || bar)"</js> - Parenthesis.
	 * 		</ul>
	 * 	<li>
	 * 		AND operations take precedence over OR operations (as expected).
	 * 	<li>
	 * 		Whitespace is ignored.
	 * 	<li>
	 * 		<jk>null</jk> or empty expressions always match as <jk>false</jk>.
	 * 	<li>
	 * 		If patterns are used, you must specify the list of declared roles using {@link Rest#rolesDeclared()} or {@link RestOpContextBuilder#rolesDeclared(String...)}.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * @param value The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestOpContextBuilder roleGuard(String value) {
		if (roleGuard == null)
			roleGuard = ASet.of(value);
		else
			roleGuard.add(value);
		return this;
	}

	/**
	 * Supported content media types.
	 *
	 * <p>
	 * Overrides the media types inferred from the parsers that identify what media types can be consumed by the resource.
	 * <br>An example where this might be useful if you have parsers registered that handle media types that you
	 * don't want exposed in the Swagger documentation.
	 *
	 * <p>
	 * This affects the returned values from the following:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link RestContext#getConsumes() RestContext.getConsumes()}
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Rest#consumes}
	 * 	<li class='ja'>{@link RestOp#consumes}
	 * 	<li class='ja'>{@link RestPut#consumes}
	 * 	<li class='ja'>{@link RestPost#consumes}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public RestOpContextBuilder consumes(MediaType...values) {
		if (consumes == null)
			consumes = new ArrayList<>(Arrays.asList(values));
		else
			consumes.addAll(Arrays.asList(values));
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOpContextBuilder apply(AnnotationWorkList work) {
		super.apply(work);
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - ContextBuilder */
	public RestOpContextBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOpContextBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOpContextBuilder appendTo(String name, Object value) {
		super.appendTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOpContextBuilder apply(ContextProperties copyFrom) {
		super.apply(copyFrom);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOpContextBuilder applyAnnotations(java.lang.Class<?>...fromClasses) {
		super.applyAnnotations(fromClasses);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOpContextBuilder applyAnnotations(Method...fromMethods) {
		super.applyAnnotations(fromMethods);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOpContextBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOpContextBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOpContextBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOpContextBuilder prependTo(String name, Object value) {
		super.prependTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOpContextBuilder putAllTo(String name, Object value) {
		super.putAllTo(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOpContextBuilder putTo(String name, String key, Object value) {
		super.putTo(name, key, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOpContextBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOpContextBuilder set(String name) {
		super.set(name);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOpContextBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOpContextBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOpContextBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public RestOpContextBuilder unset(String name) {
		super.unset(name);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder annotations(Annotation...values) {
		super.annotations(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder beanInterceptor(Class<?> on, Class<? extends org.apache.juneau.transform.BeanInterceptor<?>> value) {
		super.beanInterceptor(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder beanMapPutReturnsOldValue() {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder beanProperties(Map<String,Object> values) {
		super.beanProperties(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder beanProperties(Class<?> beanClass, String properties) {
		super.beanProperties(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder beanProperties(String beanClassName, String properties) {
		super.beanProperties(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder beanPropertiesExcludes(Map<String,Object> values) {
		super.beanPropertiesExcludes(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder beanPropertiesExcludes(Class<?> beanClass, String properties) {
		super.beanPropertiesExcludes(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder beanPropertiesExcludes(String beanClassName, String properties) {
		super.beanPropertiesExcludes(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder beanPropertiesReadOnly(Map<String,Object> values) {
		super.beanPropertiesReadOnly(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder beanPropertiesReadOnly(Class<?> beanClass, String properties) {
		super.beanPropertiesReadOnly(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder beanPropertiesReadOnly(String beanClassName, String properties) {
		super.beanPropertiesReadOnly(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder beanPropertiesWriteOnly(Map<String,Object> values) {
		super.beanPropertiesWriteOnly(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder beanPropertiesWriteOnly(Class<?> beanClass, String properties) {
		super.beanPropertiesWriteOnly(beanClass, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder beanPropertiesWriteOnly(String beanClassName, String properties) {
		super.beanPropertiesWriteOnly(beanClassName, properties);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder beansRequireDefaultConstructor() {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder beansRequireSerializable() {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder beansRequireSettersForGetters() {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder dictionary(Object...values) {
		super.dictionary(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
		super.dictionaryOn(on, values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder disableBeansRequireSomeProperties() {
		super.disableBeansRequireSomeProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder disableIgnoreMissingSetters() {
		super.disableIgnoreMissingSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder disableIgnoreTransientFields() {
		super.disableIgnoreTransientFields();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder disableIgnoreUnknownNullBeanProperties() {
		super.disableIgnoreUnknownNullBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder disableInterfaceProxies() {
		super.disableInterfaceProxies();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> RestOpContextBuilder example(Class<T> pojoClass, T o) {
		super.example(pojoClass, o);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public <T> RestOpContextBuilder example(Class<T> pojoClass, String json) {
		super.example(pojoClass, json);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder findFluentSetters() {
		super.findFluentSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder findFluentSetters(Class<?> on) {
		super.findFluentSetters(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder ignoreInvocationExceptionsOnGetters() {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder ignoreInvocationExceptionsOnSetters() {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder ignoreUnknownBeanProperties() {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder implClass(Class<?> interfaceClass, Class<?> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder implClasses(Map<Class<?>,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder interfaceClass(Class<?> on, Class<?> value) {
		super.interfaceClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder interfaces(java.lang.Class<?>...value) {
		super.interfaces(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder notBeanPackages(Object...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
		super.propertyNamer(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder sortProperties(java.lang.Class<?>...on) {
		super.sortProperties(on);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder stopClass(Class<?> on, Class<?> value) {
		super.stopClass(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder swaps(Object...values) {
		super.swaps(values);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder typeName(Class<?> on, String value) {
		super.typeName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder typePropertyName(String value) {
		super.typePropertyName(value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder typePropertyName(Class<?> on, String value) {
		super.typePropertyName(on, value);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder useEnumNames() {
		super.useEnumNames();
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder useJavaBeanIntrospector() {
		super.useJavaBeanIntrospector();
		return this;
	}

	// </FluentSetters>
}