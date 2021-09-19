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
import static java.util.Optional.*;

import java.lang.annotation.*;
import java.util.*;
import java.util.function.*;

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
public class RestOpContextBuilder extends ContextBuilder {

	RestContext restContext;
	RestContextBuilder parent;
	Method restMethod;
	String httpMethod, clientVersion;
	Enablement debug;
	List<String> path;

	private RestConverterList.Builder converters;
	private BeanContextBuilder beanContext;

	PartList.Builder defaultFormData, defaultQueryData;
	NamedAttributeList defaultRequestAttributes;
	HeaderList.Builder defaultRequestHeaders, defaultResponseHeaders;
	RestMatcherList.Builder restMatchers;
	List<MediaType> produces, consumes;
	Set<String> roleGuard, rolesDeclared;
	RestGuardList.Builder guards = RestGuardList.create();
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

	@Override /* BeanContextBuilder */
	public RestOpContext build() {
		try {
			return BeanCreator.create(RestOpContext.class).type(getType().orElse(getDefaultImplClass())).store(beanStore).builder(this).run();
		} catch (Exception e) {
			throw toHttpException(e, InternalServerError.class);
		}
	}

	/**
	 * Specifies the default implementation class if not specified via {@link #type(Class)}.
	 *
	 * @return The default implementation class if not specified via {@link #type(Class)}.
	 */
	protected Class<? extends RestOpContext> getDefaultImplClass() {
		return RestOpContext.class;
	}

	RestOpContextBuilder(java.lang.reflect.Method method, RestContext context) {

		this.restContext = context;
		this.parent = context.builder;
		this.restMethod = method;
		this.beanStore = BeanStore
			.of(context.getRootBeanStore(), context.builder.resource().get())
			.addBean(java.lang.reflect.Method.class, method);

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

			if (context.builder.beanContext().canApply(al))
				beanContext().apply(al);
			if (context.builder.serializers().canApply(al))
				serializers().apply(al);
			if (context.builder.parsers().canApply(al))
				parsers().apply(al);
			if (context.builder.partSerializer().canApply(al))
				getPartSerializer().apply(al);
			if (context.builder.partParser().canApply(al))
				getPartParser().apply(al);

		} catch (Exception e) {
			throw toHttpException(e, InternalServerError.class);
		}
	}

	/**
	 * Returns the REST servlet/bean instance that this context is defined against.
	 *
	 * @return The REST servlet/bean instance that this context is defined against.
	 */
	public final Supplier<?> resource() {
		return restContext.builder.resource();
	}

	/**
	 * Returns the default classes list.
	 *
	 * <p>
	 * This defines the implementation classes for a variety of bean types.
	 *
	 * <p>
	 * Default classes are inherited from the parent REST object.
	 * Typically used on the top-level {@link RestContextBuilder} to affect class types for that REST object and all children.
	 *
	 * <p>
	 * Modifying the default class list on this builder does not affect the default class list on the parent builder, but changes made
	 * here are inherited by child builders.
	 *
	 * @return The default classes list for this builder.
	 */
	public final DefaultClassList defaultClasses() {
		return restContext.builder.defaultClasses();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// beanStore
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns access to the bean store being used by this builder.
	 *
	 * <p>
	 * Can be used to add more beans to the bean store.
	 *
	 * @return The bean store being used by this builder.
	 */
	public final BeanStore beanStore() {
		return beanStore;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// beanContext
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the builder for the {@link BeanContext} object in the REST context.
	 *
	 * @return The builder for the {@link BeanContext} object in the REST context.
	 */
	public final BeanContextBuilder beanContext() {
		if (beanContext == null)
			beanContext = createBeanContext(beanStore(), parent, resource());
		return beanContext;
	}

	/**
	 * Constructs the bean context builder for this REST method.
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param parent
	 * 	The builder for the REST resource class.
	 * @param resource
	 * 	The REST servlet/bean instance that this context is defined against.
	 * @return The bean context builder for this REST resource.
	 */
	protected BeanContextBuilder createBeanContext(BeanStore beanStore, RestContextBuilder parent, Supplier<?> resource) {

		// Default value.
		Value<BeanContextBuilder> v = Value.of(
			parent.beanContext().copy()
		);

		return v.get();
	}

	final Optional<BeanContext> getBeanContext() {
		return beanContext == null ? empty() : of(beanContext.build());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// encoders
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the builder for the {@link EncoderGroup} object in the REST context.
	 *
	 * @return The builder for the {@link EncoderGroup} object in the REST context.
	 */
	public final EncoderGroup.Builder encoders() {
		if (encoders == null)
			encoders = createEncoders(beanStore(), parent, resource());
		return encoders;
	}

	/**
	 * Constructs the encoder group builder for this REST method.
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param parent
	 * 	The builder for the REST resource class.
	 * @param resource
	 * 	The REST servlet/bean instance that this context is defined against.
	 * @return The encoder group builder for this REST resource.
	 */
	protected EncoderGroup.Builder createEncoders(BeanStore beanStore, RestContextBuilder parent, Supplier<?> resource) {

		// Default value.
		Value<EncoderGroup.Builder> v = Value.of(
			parent.encoders().copy()
		);

		return v.get();
	}

	final Optional<EncoderGroup> getEncoders() {
		return encoders == null ? empty() : of(encoders.build());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// serializers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the builder for the {@link SerializerGroup} object in the REST context.
	 *
	 * @return The builder for the {@link SerializerGroup} object in the REST context.
	 */
	public final SerializerGroup.Builder serializers() {
		if (serializers == null)
			serializers = createSerializers(beanStore(), parent, resource());
		return serializers;
	}

	/**
	 * Constructs the serializer group builder for this REST method.
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param parent
	 * 	The builder for the REST resource class.
	 * @param resource
	 * 	The REST servlet/bean instance that this context is defined against.
	 * @return The serializer group builder for this REST resource.
	 */
	protected SerializerGroup.Builder createSerializers(BeanStore beanStore, RestContextBuilder parent, Supplier<?> resource) {

		// Default value.
		Value<SerializerGroup.Builder> v = Value.of(
			parent.serializers().copy()
		);

		return v.get();
	}

	final Optional<SerializerGroup> getSerializers() {
		return serializers == null ? empty() : of(serializers.build());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// parsers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the builder for the {@link ParserGroup} object in the REST context.
	 *
	 * @return The builder for the {@link ParserGroup} object in the REST context.
	 */
	public final ParserGroup.Builder parsers() {
		if (parsers == null)
			parsers = createParsers(beanStore(), parent, resource());
		return parsers;
	}

	/**
	 * Constructs the parser group builder for this REST method.
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param parent
	 * 	The builder for the REST resource class.
	 * @param resource
	 * 	The REST servlet/bean instance that this context is defined against.
	 * @return The serializer group builder for this REST resource.
	 */
	protected ParserGroup.Builder createParsers(BeanStore beanStore, RestContextBuilder parent, Supplier<?> resource) {

		// Default value.
		Value<ParserGroup.Builder> v = Value.of(
			parent.parsers().copy()
		);

		return v.get();
	}

	final Optional<ParserGroup> getParsers() {
		return parsers == null ? empty() : of(parsers.build());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// converters
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the builder for the {@link RestConverterList} object in the REST context.
	 *
	 * @return The builder for the {@link RestConverterList} object in the REST context.
	 */
	public final RestConverterList.Builder converters() {
		if (converters == null)
			converters = createConverters(beanStore(), resource());
		return converters;
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
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param resource
	 * 	The REST servlet/bean instance that this context is defined against.
	 * @return The rest converter list builder for this REST method.
	 */
	protected RestConverterList.Builder createConverters(BeanStore beanStore, Supplier<?> resource) {

		// Default value.
		Value<RestConverterList.Builder> v = Value.of(
			RestConverterList
				.create()
				.beanStore(beanStore)
		);

		// Specify the implementation class if its set as a default.
		defaultClasses()
			.get(RestConverterList.class)
			.ifPresent(x -> v.get().type(x));

		// Replace with builder from bean store.
		beanStore
			.getBean(RestConverterList.Builder.class)
			.map(x -> x.copy())
			.ifPresent(x->v.set(x));

		// Replace with bean from bean store.
		beanStore
			.getBean(RestConverterList.class)
			.ifPresent(x->v.get().impl(x));

		// Replace with builder from:  public [static] RestConverterList.Builder createConverters(<args>)
		beanStore
			.beanCreateMethodFinder(RestConverterList.Builder.class)
			.addBean(RestConverterList.Builder.class, v.get())
			.find("createConverters")
			.run(x -> v.set(x));

		// Replace with bean from:  public [static] RestConverterList createConverters(<args>)
		beanStore
			.beanCreateMethodFinder(RestConverterList.class)
			.addBean(RestConverterList.Builder.class, v.get())
			.find("createConverters")
			.run(x -> v.get().impl(x));

		return v.get();
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
			partParser = restContext.builder.partParser().copy();
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

//	/**
//	 * Returns the parser group builder containing the parsers for converting HTTP request bodies into POJOs.
//	 *
//	 * <p>
//	 * This method can be used to override encoders defined at the class level via {@link RestContextBuilder#getEncoders()}.
//	 * On first call, the builder from the class context is copied into a modifiable builder for this method.
//	 * If never called, then the builder from the class context is used.
//	 *
//	 * <p>
//	 * The builder is initialized with encoders defined via the {@link Rest#parsers()} annotation.
//	 * That annotation is applied from parent-to-child order with child entries given priority over parent entries.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='link'>{@doc RestEncoders}
//	 * </ul>
//	 *
//	 * @return The encoder group builder for this context builder.
//	 */
//	public EncoderGroup.Builder getEncoders() {
//		if (encoders == null)
//			encoders = restContext.builder.encoders().copy();
//		return encoders;
//	}

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
	public RestOpContextBuilder type(Class<? extends Context> value) {
		super.type(value);
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
	public RestOpContextBuilder unset(String name) {
		super.unset(name);
		return this;
	}

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder annotations(Annotation...values) {
		super.annotations(values);
		return this;
	}

	// </FluentSetters>
}