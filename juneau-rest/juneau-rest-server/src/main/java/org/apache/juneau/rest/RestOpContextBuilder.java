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
import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.http.HttpParts.*;
import static org.apache.juneau.internal.ExceptionUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.rest.HttpRuntimeException.*;
import static org.apache.juneau.rest.util.RestUtils.*;
import static java.util.Optional.*;

import java.lang.annotation.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.converters.*;
import org.apache.juneau.rest.guards.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;

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
	private RestGuardList.Builder guards;
	private EncoderGroup.Builder encoders;
	private SerializerGroup.Builder serializers;
	private ParserGroup.Builder parsers;
	private HttpPartSerializer.Creator partSerializer;
	private HttpPartParser.Creator partParser;
	private RestMatcherList.Builder matchers;
	private JsonSchemaGeneratorBuilder jsonSchemaGenerator;

	PartList.Builder defaultRequestFormData, defaultRequestQueryData;
	NamedAttributeList.Builder defaultRequestAttributes;
	HeaderList.Builder defaultRequestHeaders, defaultResponseHeaders;
	RestMatcherList.Builder restMatchers;
	List<MediaType> produces, consumes;
	Set<String> roleGuard, rolesDeclared;
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
			return BeanCreator.of(RestOpContext.class).type(getType().orElse(getDefaultImplClass())).store(beanStore).builder(this).run();
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
				partSerializer().apply(al);
			if (context.builder.partParser().canApply(al))
				partParser().apply(al);
			if (context.builder.jsonSchemaGenerator().canApply(al))
				jsonSchemaGenerator().apply(al);

			processParameterAnnotations();

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
	 * Returns the bean context sub-builder.
	 *
	 * @return The bean context sub-builder.
	 */
	public final BeanContextBuilder beanContext() {
		if (beanContext == null)
			beanContext = createBeanContext(beanStore(), parent, resource());
		return beanContext;
	}

	/**
	 * Applies an operation to the bean context sub-builder.
	 *
	 * <p>
	 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestOpContext <jv>context</jv> = RestOpContext
	 * 		.<jsm>create</jsm>(<jv>method</jv>, <jv>restContext</jv>)
	 * 		.beanContext(<jv>x</jv> -&gt; <jv>x</jv>.ignoreUnknownBeanProperties())
	 * 		.build();
	 * </p>
	 *
	 * @param operation The operation to apply.
	 * @return This object.
	 */
	public final RestOpContextBuilder beanContext(Consumer<BeanContextBuilder> operation) {
		operation.accept(beanContext());
		return this;
	}

	/**
	 * Instantiates the bean context sub-builder.
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param parent
	 * 	The builder for the REST resource class.
	 * @param resource
	 * 	The REST servlet/bean instance that this context is defined against.
	 * @return A new bean context sub-builder.
	 */
	protected BeanContextBuilder createBeanContext(BeanStore beanStore, RestContextBuilder parent, Supplier<?> resource) {

		// Default value.
		Value<BeanContextBuilder> v = Value.of(
			parent.beanContext().copy()
		);

		BeanStore
			.of(beanStore, resource)
			.addBean(BeanContextBuilder.class, v.get())
			.createMethodFinder(BeanContextBuilder.class, resource)
			.find("createBeanContext", Method.class)
			.run(x -> v.set(x));

		BeanStore
			.of(beanStore, resource)
			.addBean(BeanContextBuilder.class, v.get())
			.createMethodFinder(BeanContext.class, resource)
			.find("createBeanContext", Method.class)
			.run(x -> v.get().impl(x));

		return v.get();
	}

	final Optional<BeanContext> getBeanContext() {
		return beanContext == null ? empty() : of(beanContext.build());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// encoders
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the encoder group sub-builder.
	 *
	 * @return The encoder group sub-builder.
	 */
	public final EncoderGroup.Builder encoders() {
		if (encoders == null)
			encoders = createEncoders(beanStore(), parent, resource());
		return encoders;
	}

	/**
	 * Applies an operation to the encoder group sub-builder.
	 *
	 * <p>
	 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestOpContext <jv>context</jv> = RestOpContext
	 * 		.<jsm>create</jsm>(<jv>method</jv>, <jv>restContext</jv>)
	 * 		.encoders(<jv>x</jv> -&gt; <jv>x</jv>.add(MyEncoder.<jk>class</jk>))
	 * 		.build();
	 * </p>
	 *
	 * @param operation The operation to apply.
	 * @return This object.
	 */
	public final RestOpContextBuilder encoders(Consumer<EncoderGroup.Builder> operation) {
		operation.accept(encoders());
		return this;
	}

	/**
	 * Instantiates the encoder group sub-builder.
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param parent
	 * 	The builder for the REST resource class.
	 * @param resource
	 * 	The REST servlet/bean instance that this context is defined against.
	 * @return A new encoder group sub-builder.
	 */
	protected EncoderGroup.Builder createEncoders(BeanStore beanStore, RestContextBuilder parent, Supplier<?> resource) {

		// Default value.
		Value<EncoderGroup.Builder> v = Value.of(
			parent.encoders().copy()
		);

		BeanStore
			.of(beanStore, resource)
			.addBean(EncoderGroup.Builder.class, v.get())
			.createMethodFinder(EncoderGroup.Builder.class, resource)
			.find("createEncoders", Method.class)
			.run(x -> v.set(x));

		BeanStore
			.of(beanStore, resource)
			.addBean(EncoderGroup.Builder.class, v.get())
			.createMethodFinder(EncoderGroup.class, resource)
			.find("createEncoders", Method.class)
			.run(x -> v.get().impl(x));

		return v.get();
	}

	final Optional<EncoderGroup> getEncoders() {
		return encoders == null ? empty() : of(encoders.build());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// serializers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the serializer group sub-builder.
	 *
	 * @return The serializer group sub-builder.
	 */
	public final SerializerGroup.Builder serializers() {
		if (serializers == null)
			serializers = createSerializers(beanStore(), parent, resource());
		return serializers;
	}

	/**
	 * Applies an operation to the serializer group sub-builder.
	 *
	 * <p>
	 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestOpContext <jv>context</jv> = RestOpContext
	 * 		.<jsm>create</jsm>(<jv>method</jv>, <jv>restContext</jv>)
	 * 		.serializers(<jv>x</jv> -&gt; <jv>x</jv>.add(MySerializer.<jk>class</jk>))
	 * 		.build();
	 * </p>
	 *
	 * @param operation The operation to apply.
	 * @return This object.
	 */
	public final RestOpContextBuilder serializers(Consumer<SerializerGroup.Builder> operation) {
		operation.accept(serializers());
		return this;
	}

	/**
	 * Instantiates the serializer group sub-builder.
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param parent
	 * 	The builder for the REST resource class.
	 * @param resource
	 * 	The REST servlet/bean instance that this context is defined against.
	 * @return A new serializer group sub-builder.
	 */
	protected SerializerGroup.Builder createSerializers(BeanStore beanStore, RestContextBuilder parent, Supplier<?> resource) {

		// Default value.
		Value<SerializerGroup.Builder> v = Value.of(
			parent.serializers().copy()
		);

		BeanStore
			.of(beanStore, resource)
			.addBean(SerializerGroup.Builder.class, v.get())
			.createMethodFinder(SerializerGroup.Builder.class, resource)
			.find("createSerializers", Method.class)
			.run(x -> v.set(x));

		BeanStore
			.of(beanStore, resource)
			.addBean(SerializerGroup.Builder.class, v.get())
			.createMethodFinder(SerializerGroup.class, resource)
			.find("createSerializers", Method.class)
			.run(x -> v.get().impl(x));

		return v.get();
	}

	final Optional<SerializerGroup> getSerializers() {
		return serializers == null ? empty() : of(serializers.build());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// parsers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the parser group sub-builder.
	 *
	 * @return The parser group sub-builder.
	 */
	public final ParserGroup.Builder parsers() {
		if (parsers == null)
			parsers = createParsers(beanStore(), parent, resource());
		return parsers;
	}

	/**
	 * Applies an operation to the parser group sub-builder.
	 *
	 * <p>
	 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestOpContext <jv>context</jv> = RestOpContext
	 * 		.<jsm>create</jsm>(<jv>method</jv>, <jv>restContext</jv>)
	 * 		.parsers(<jv>x</jv> -&gt; <jv>x</jv>.add(MyParser.<jk>class</jk>))
	 * 		.build();
	 * </p>
	 *
	 * @param operation The operation to apply.
	 * @return This object.
	 */
	public final RestOpContextBuilder parsers(Consumer<ParserGroup.Builder> operation) {
		operation.accept(parsers());
		return this;
	}

	/**
	 * Instantiates the parser group sub-builder.
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param parent
	 * 	The builder for the REST resource class.
	 * @param resource
	 * 	The REST servlet/bean instance that this context is defined against.
	 * @return A new parser group sub-builder.
	 */
	protected ParserGroup.Builder createParsers(BeanStore beanStore, RestContextBuilder parent, Supplier<?> resource) {

		// Default value.
		Value<ParserGroup.Builder> v = Value.of(
			parent.parsers().copy()
		);

		BeanStore
			.of(beanStore, resource)
			.addBean(ParserGroup.Builder.class, v.get())
			.createMethodFinder(ParserGroup.Builder.class, resource)
			.find("createParsers", Method.class)
			.run(x -> v.set(x));

		BeanStore
			.of(beanStore, resource)
			.addBean(ParserGroup.Builder.class, v.get())
			.createMethodFinder(ParserGroup.class, resource)
			.find("createParsers", Method.class)
			.run(x -> v.get().impl(x));

		return v.get();
	}

	final Optional<ParserGroup> getParsers() {
		return parsers == null ? empty() : of(parsers.build());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// partSerializer
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the part serializer sub-builder.
	 *
	 * @return The part serializer sub-builder.
	 */
	public final HttpPartSerializer.Creator partSerializer() {
		if (partSerializer == null)
			partSerializer = createPartSerializer(beanStore(), parent, resource());
		return partSerializer;
	}

	/**
	 * Applies an operation to the part serializer sub-builder.
	 *
	 * <p>
	 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestOpContext <jv>context</jv> = RestOpContext
	 * 		.<jsm>create</jsm>(<jv>method</jv>, <jv>restContext</jv>)
	 * 		.partSerializer(<jv>x</jv> -&gt; <jv>x</jv>.builder(OpenApiSerializerBuilder.<jk>class</jk>, <jv>y</jv> -&gt; <jv>y</jv>.sortProperties()))
	 * 		.build();
	 * </p>
	 *
	 * @param operation The operation to apply.
	 * @return This object.
	 */
	public final RestOpContextBuilder partSerializer(Consumer<HttpPartSerializer.Creator> operation) {
		operation.accept(partSerializer());
		return this;
	}

	/**
	 * Instantiates the part serializer sub-builder.
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param parent
	 * 	The builder for the REST resource class.
	 * @param resource
	 * 	The REST servlet/bean instance that this context is defined against.
	 * @return A new part serializer sub-builder.
	 */
	protected HttpPartSerializer.Creator createPartSerializer(BeanStore beanStore, RestContextBuilder parent, Supplier<?> resource) {

		// Default value.
		Value<HttpPartSerializer.Creator> v = Value.of(
			parent.partSerializer().copy()
		);

		BeanStore
			.of(beanStore, resource)
			.addBean(HttpPartSerializer.Creator.class, v.get())
			.createMethodFinder(HttpPartSerializer.Creator.class, resource)
			.find("createPartSerializer", Method.class)
			.run(x -> v.set(x));

		BeanStore
			.of(beanStore, resource)
			.addBean(HttpPartSerializer.Creator.class, v.get())
			.createMethodFinder(HttpPartSerializer.class, resource)
			.find("createPartSerializer", Method.class)
			.run(x -> v.get().impl(x));

		return v.get();
	}

	final Optional<HttpPartSerializer> getPartSerializer() {
		return partSerializer == null ? empty() : of(partSerializer.create());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// partParser
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the part parser sub-builder.
	 *
	 * @return The part parser sub-builder.
	 */
	public final HttpPartParser.Creator partParser() {
		if (partParser == null)
			partParser = createPartParser(beanStore(), parent, resource());
		return partParser;
	}

	/**
	 * Applies an operation to the part parser sub-builder.
	 *
	 * <p>
	 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestOpContext <jv>context</jv> = RestOpContext
	 * 		.<jsm>create</jsm>(<jv>method</jv>, <jv>restContext</jv>)
	 * 		.partParser(<jv>x</jv> -&gt; <jv>x</jv>.builder(OpenApiParserBuilder.<jk>class</jk>, <jv>y</jv> -&gt; <jv>y</jv>.ignoreUnknownBeanProperties()))
	 * 		.build();
	 * </p>
	 *
	 * @param operation The operation to apply.
	 * @return This object.
	 */
	public final RestOpContextBuilder partParser(Consumer<HttpPartParser.Creator> operation) {
		operation.accept(partParser());
		return this;
	}

	/**
	 * Instantiates the part parser sub-builder.
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param parent
	 * 	The builder for the REST resource class.
	 * @param resource
	 * 	The REST servlet/bean instance that this context is defined against.
	 * @return A new part parser sub-builder.
	 */
	protected HttpPartParser.Creator createPartParser(BeanStore beanStore, RestContextBuilder parent, Supplier<?> resource) {

		// Default value.
		Value<HttpPartParser.Creator> v = Value.of(
			parent.partParser().copy()
		);

		BeanStore
			.of(beanStore, resource)
			.addBean(HttpPartParser.Creator.class, v.get())
			.createMethodFinder(HttpPartParser.Creator.class, resource)
			.find("createPartParser", Method.class)
			.run(x -> v.set(x));

		BeanStore
			.of(beanStore, resource)
			.addBean(HttpPartParser.Creator.class, v.get())
			.createMethodFinder(HttpPartParser.class, resource)
			.find("createPartParser", Method.class)
			.run(x -> v.get().impl(x));

		return v.get();
	}

	final Optional<HttpPartParser> getPartParser() {
		return partParser == null ? empty() : of(partParser.create());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// jsonSchemaGenerator
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the JSON schema generator sub-builder.
	 *
	 * @return The JSON schema generator sub-builder.
	 */
	public final JsonSchemaGeneratorBuilder jsonSchemaGenerator() {
		if (jsonSchemaGenerator == null)
			jsonSchemaGenerator = createJsonSchemaGenerator(beanStore(), parent, resource());
		return jsonSchemaGenerator;
	}

	/**
	 * Applies an operation to the JSON schema generator sub-builder.
	 *
	 * <p>
	 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestOpContext <jv>context</jv> = RestOpContext
	 * 		.<jsm>create</jsm>(<jv>method</jv>, <jv>restContext</jv>)
	 * 		.jsonSchemaGenerator(<jv>x</jv> -&gt; <jv>x</jv>.allowNestedExamples()))
	 * 		.build();
	 * </p>
	 *
	 * @param operation The operation to apply.
	 * @return This object.
	 */
	public final RestOpContextBuilder jsonSchemaGenerator(Consumer<JsonSchemaGeneratorBuilder> operation) {
		operation.accept(jsonSchemaGenerator());
		return this;
	}

	/**
	 * Instantiates the JSON schema generator sub-builder.
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param parent
	 * 	The builder for the REST resource class.
	 * @param resource
	 * 	The REST servlet/bean instance that this context is defined against.
	 * @return A new JSON schema generator sub-builder.
	 */
	protected JsonSchemaGeneratorBuilder createJsonSchemaGenerator(BeanStore beanStore, RestContextBuilder parent, Supplier<?> resource) {

		// Default value.
		Value<JsonSchemaGeneratorBuilder> v = Value.of(
			parent.jsonSchemaGenerator().copy()
		);

		BeanStore
			.of(beanStore, resource)
			.addBean(JsonSchemaGeneratorBuilder.class, v.get())
			.createMethodFinder(JsonSchemaGeneratorBuilder.class, resource)
			.find("createJsonSchemaGenerator", Method.class)
			.run(x -> v.set(x));

		BeanStore
			.of(beanStore, resource)
			.addBean(JsonSchemaGeneratorBuilder.class, v.get())
			.createMethodFinder(JsonSchemaGenerator.class, resource)
			.find("createJsonSchemaGenerator", Method.class)
			.run(x -> v.get().impl(x));

		return v.get();
	}

	final Optional<JsonSchemaGenerator> getJsonSchemaGenerator() {
		return jsonSchemaGenerator == null ? empty() : of(jsonSchemaGenerator.build());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// converters
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the response converter list sub-builder.
	 *
	 * @return The response converter list sub-builder.
	 */
	public final RestConverterList.Builder converters() {
		if (converters == null)
			converters = createConverters(beanStore(), resource());
		return converters;
	}

	/**
	 * Applies an operation to the response converter list sub-builder.
	 *
	 * <p>
	 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestOpContext <jv>context</jv> = RestOpContext
	 * 		.<jsm>create</jsm>(<jv>method</jv>, <jv>restContext</jv>)
	 * 		.converters(<jv>x</jv> -&gt; <jv>x</jv>.add(MyConverter.<jk>class</jk>)))
	 * 		.build();
	 * </p>
	 *
	 * @param operation The operation to apply.
	 * @return This object.
	 */
	public final RestOpContextBuilder converters(Consumer<RestConverterList.Builder> operation) {
		operation.accept(converters());
		return this;
	}

	/**
	 * Instantiates the response converter list sub-builder.
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
	 * @return A new response converter list sub-builder.
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

	//-----------------------------------------------------------------------------------------------------------------
	// guards
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the guard list sub-builder.
	 *
	 * @return The guard list sub-builder.
	 */
	public final RestGuardList.Builder guards() {
		if (guards == null)
			guards = createGuards(beanStore(), resource());
		return guards;
	}

	/**
	 * Applies an operation to the guard list sub-builder.
	 *
	 * <p>
	 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestOpContext <jv>context</jv> = RestOpContext
	 * 		.<jsm>create</jsm>(<jv>method</jv>, <jv>restContext</jv>)
	 * 		.guards(<jv>x</jv> -&gt; <jv>x</jv>.add(MyGuard.<jk>class</jk>)))
	 * 		.build();
	 * </p>
	 *
	 * @param operation The operation to apply.
	 * @return This object.
	 */
	public final RestOpContextBuilder guards(Consumer<RestGuardList.Builder> operation) {
		operation.accept(guards());
		return this;
	}

	/**
	 * Instantiates the guard list sub-builder.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Looks for guards set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestOpContextBuilder#guards()}}
	 * 			<li>{@link RestOp#guards()}.
	 * 			<li>{@link Rest#guards()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createGuards()</> method that returns <c>{@link RestGuard}[]</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link Method} - The Java method this context belongs to.
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanStore}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean store registered in this context.
	 * 	<li>Instantiates a <c>RestGuard[0]</c>.
	 * </ul>
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param resource
	 * 	The REST servlet/bean instance that this context is defined against.
	 * @return A new guard list sub-builder.
	 */
	protected RestGuardList.Builder createGuards(BeanStore beanStore, Supplier<?> resource) {

		// Default value.
		Value<RestGuardList.Builder> v = Value.of(
			RestGuardList
				.create()
				.beanStore(beanStore)
		);

		// Specify the implementation class if its set as a default.
		defaultClasses()
			.get(RestGuardList.class)
			.ifPresent(x -> v.get().type(x));

		// Replace with builder from bean store.
		beanStore
			.getBean(RestGuardList.Builder.class)
			.map(x -> x.copy())
			.ifPresent(x->v.set(x));

		// Replace with bean from bean store.
		beanStore
			.getBean(RestGuardList.class)
			.ifPresent(x->v.get().impl(x));

		// Replace with builder from:  public [static] RestGuardList.Builder createGuards(<args>)
		beanStore
			.beanCreateMethodFinder(RestGuardList.Builder.class)
			.addBean(RestGuardList.Builder.class, v.get())
			.find("createGuards")
			.run(x -> v.set(x));

		// Replace with bean from:  public [static] RestGuardList createGuards(<args>)
		beanStore
			.beanCreateMethodFinder(RestGuardList.class)
			.addBean(RestGuardList.Builder.class, v.get())
			.find("createGuards")
			.run(x -> v.get().impl(x));

		return v.get();
	}

	final RestGuardList getGuards() {
		RestGuardList.Builder b = guards();
		Set<String> roleGuard = ofNullable(this.roleGuard).orElseGet(()->new LinkedHashSet<>());

		for (String rg : roleGuard) {
			try {
				b.append(new RoleBasedRestGuard(rolesDeclared, rg));
			} catch (java.text.ParseException e1) {
				throw runtimeException(e1);
			}
		}

		return guards.build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// matchers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the matcher list sub-builder.
	 *
	 * @return The matcher list sub-builder.
	 */
	public final RestMatcherList.Builder matchers() {
		if (matchers == null)
			matchers = createMatchers(beanStore(), resource());
		return matchers;
	}

	/**
	 * Applies an operation to the matcher list sub-builder.
	 *
	 * <p>
	 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestOpContext <jv>context</jv> = RestOpContext
	 * 		.<jsm>create</jsm>(<jv>method</jv>, <jv>restContext</jv>)
	 * 		.matchers(<jv>x</jv> -&gt; <jv>x</jv>.add(MyMatcher.<jk>class</jk>)))
	 * 		.build();
	 * </p>
	 *
	 * @param operation The operation to apply.
	 * @return This object.
	 */
	public final RestOpContextBuilder matchers(Consumer<RestMatcherList.Builder> operation) {
		operation.accept(matchers());
		return this;
	}

	/**
	 * Instantiates the matcher list sub-builder.
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
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Looks for matchers set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestOp#matchers()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createMatchers()</> method that returns <c>{@link RestMatcher}[]</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link java.lang.reflect.Method} - The Java method this context belongs to.
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanStore}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean store registered in this context.
	 * 	<li>Instantiates a <c>RestMatcher[0]</c>.
	 * </ul>
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param resource
	 * 	The REST servlet/bean instance that this context is defined against.
	 * @return A new matcher list sub-builder.
	 */
	protected RestMatcherList.Builder createMatchers(BeanStore beanStore, Supplier<?> resource) {

		// Default value.
		Value<RestMatcherList.Builder> v = Value.of(
			RestMatcherList
				.create()
				.beanStore(beanStore)
		);

		// Specify the implementation class if its set as a default.
		defaultClasses()
			.get(RestMatcherList.class)
			.ifPresent(x -> v.get().type(x));

		// Replace with builder from bean store.
		beanStore
			.getBean(RestMatcherList.Builder.class)
			.map(x -> x.copy())
			.ifPresent(x->v.set(x));

		// Replace with bean from bean store.
		beanStore
			.getBean(RestMatcherList.class)
			.ifPresent(x->v.get().impl(x));

		// Replace with builder from:  public [static] RestMatcherList.Builder createMatchers(<args>)
		beanStore
			.beanCreateMethodFinder(RestMatcherList.Builder.class)
			.addBean(RestMatcherList.Builder.class, v.get())
			.find("createMatchers")
			.run(x -> v.set(x));

		// Replace with bean from:  public [static] RestMatcherList createMatchers(<args>)
		beanStore
			.beanCreateMethodFinder(RestMatcherList.class)
			.addBean(RestMatcherList.Builder.class, v.get())
			.find("createMatchers")
			.run(x -> v.get().impl(x));

		return v.get();
	}

	final RestMatcherList getMatchers(RestContext restContext) {
		RestMatcherList.Builder b = matchers();
		if (clientVersion != null)
			b.append(new ClientVersionMatcher(restContext.getClientVersionHeader(), MethodInfo.of(restMethod)));

		return b.build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// pathMatchers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Instantiates the path matchers for this method.
	 *
	 * @return The path matchers for this method.
	 */
	protected UrlPathMatcherList getPathMatchers() {

		Value<UrlPathMatcherList> v = Value.of(
			UrlPathMatcherList.create()
		);

		if (path != null) {
			for (String p : path) {
				if (dotAll && ! p.endsWith("/*"))
					p += "/*";
				v.get().add(UrlPathMatcher.of(p));
			}
		}

		if (v.get().isEmpty()) {
			MethodInfo mi = MethodInfo.of(restMethod);
			String p = null;
			String httpMethod = null;
			if (mi.hasAnnotation(RestGet.class))
				httpMethod = "get";
			else if (mi.hasAnnotation(RestPut.class))
				httpMethod = "put";
			else if (mi.hasAnnotation(RestPost.class))
				httpMethod = "post";
			else if (mi.hasAnnotation(RestDelete.class))
				httpMethod = "delete";
			else if (mi.hasAnnotation(RestOp.class))
				httpMethod = mi.getAnnotations(RestOp.class).stream().map(y -> y.method()).filter(y -> ! y.isEmpty()).findFirst().orElse(null);

			p = HttpUtils.detectHttpPath(restMethod, httpMethod);

			if (dotAll && ! p.endsWith("/*"))
				p += "/*";

			v.get().add(UrlPathMatcher.of(p));
		}

		beanStore
			.createMethodFinder(UrlPathMatcherList.class, resource().get())
			.addBean(UrlPathMatcherList.class, v.get())
			.find("createPathMatchers", Method.class)
			.run(x -> v.set(x));

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

	//-----------------------------------------------------------------------------------------------------------------
	// defaultRequestHeaders
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the default request headers sub-builder.
	 *
	 * @return The default request headers sub-builder.
	 */
	public final HeaderList.Builder defaultRequestHeaders() {
		if (defaultRequestHeaders == null)
			defaultRequestHeaders = createDefaultRequestHeaders(beanStore(), parent, resource());
		return defaultRequestHeaders;
	}

	/**
	 * Applies an operation to the default request headers sub-builder.
	 *
	 * <p>
	 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestOpContext <jv>context</jv> = RestOpContext
	 * 		.<jsm>create</jsm>(<jv>method</jv>, <jv>restContext</jv>)
	 * 		.defaultRequestHeaders(<jv>x</jv> -&gt; <jv>x</jv>.remove(<js>"Foo"</js>)))
	 * 		.build();
	 * </p>
	 *
	 * @param operation The operation to apply.
	 * @return This object.
	 */
	public final RestOpContextBuilder defaultRequestHeaders(Consumer<HeaderList.Builder> operation) {
		operation.accept(defaultRequestHeaders());
		return this;
	}

	/**
	 * Instantiates the default request headers sub-builder.
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param parent
	 * 	The builder for the REST resource class.
	 * @param resource
	 * 	The REST servlet/bean instance that this context is defined against.
	 * @return A new default request headers sub-builder.
	 */
	protected HeaderList.Builder createDefaultRequestHeaders(BeanStore beanStore, RestContextBuilder parent, Supplier<?> resource) {

		Value<HeaderList.Builder> v = Value.of(
			parent.defaultRequestHeaders().copy()
		);

		BeanStore
			.of(beanStore, resource)
			.addBean(HeaderList.Builder.class, v.get())
			.createMethodFinder(HeaderList.Builder.class, resource)
			.find("createDefaultRequestHeaders", Method.class)
			.run(x -> v.set(x));

		BeanStore
			.of(beanStore, resource)
			.addBean(HeaderList.Builder.class, v.get())
			.createMethodFinder(HeaderList.class, resource)
			.find("createDefaultRequestHeaders", Method.class)
			.run(x -> v.get().impl(x));

		return v.get();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// defaultResponseHeaders
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the default response headers sub-builder.
	 *
	 * @return The default response headers sub-builder.
	 */
	public final HeaderList.Builder defaultResponseHeaders() {
		if (defaultResponseHeaders == null)
			defaultResponseHeaders = createDefaultResponseHeaders(beanStore(), parent, resource());
		return defaultResponseHeaders;
	}

	/**
	 * Applies an operation to the default response headers sub-builder.
	 *
	 * <p>
	 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestOpContext <jv>context</jv> = RestOpContext
	 * 		.<jsm>create</jsm>(<jv>method</jv>, <jv>restContext</jv>)
	 * 		.defaultResponseHeaders(<jv>x</jv> -&gt; <jv>x</jv>.remove(<js>"Foo"</js>)))
	 * 		.build();
	 * </p>
	 *
	 * @param operation The operation to apply.
	 * @return This object.
	 */
	public final RestOpContextBuilder defaultResponseHeaders(Consumer<HeaderList.Builder> operation) {
		operation.accept(defaultResponseHeaders());
		return this;
	}

	/**
	 * Instantiates the default response headers sub-builder.
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param parent
	 * 	The builder for the REST resource class.
	 * @param resource
	 * 	The REST servlet/bean instance that this context is defined against.
	 * @return A new default response headers sub-builder.
	 */
	protected HeaderList.Builder createDefaultResponseHeaders(BeanStore beanStore, RestContextBuilder parent, Supplier<?> resource) {

		Value<HeaderList.Builder> v = Value.of(
			parent.defaultResponseHeaders().copy()
		);

		BeanStore
			.of(beanStore, resource)
			.addBean(HeaderList.Builder.class, v.get())
			.createMethodFinder(HeaderList.Builder.class, resource)
			.find("createDefaultResponseHeaders", Method.class)
			.run(x -> v.set(x));

		BeanStore
			.of(beanStore, resource)
			.addBean(HeaderList.Builder.class, v.get())
			.createMethodFinder(HeaderList.class, resource)
			.find("createDefaultResponseHeaders", Method.class)
			.run(x -> v.get().impl(x));

		return v.get();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// defaultRequestAttributes
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the default request attributes sub-builder.
	 *
	 * @return The default request attributes sub-builder.
	 */
	public final NamedAttributeList.Builder defaultRequestAttributes() {
		if (defaultRequestAttributes == null)
			defaultRequestAttributes = createDefaultRequestAttributes(beanStore(), parent, resource());
		return defaultRequestAttributes;
	}

	/**
	 * Applies an operation to the default request attributes sub-builder.
	 *
	 * <p>
	 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestOpContext <jv>context</jv> = RestOpContext
	 * 		.<jsm>create</jsm>(<jv>method</jv>, <jv>restContext</jv>)
	 * 		.defaultRequestAttributes(<jv>x</jv> -&gt; <jv>x</jv>.add(BasicNamedAttribute.<jsm>of</jsm>(<js>"Foo"</js>, ()-&gt;<jsm>getFoo</jsm>()))
	 * 		.build();
	 * </p>
	 *
	 * @param operation The operation to apply.
	 * @return This object.
	 */
	public final RestOpContextBuilder defaultRequestAttributes(Consumer<NamedAttributeList.Builder> operation) {
		operation.accept(defaultRequestAttributes());
		return this;
	}

	/**
	 * Instantiates the default request attributes sub-builder.
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param parent
	 * 	The builder for the REST resource class.
	 * @param resource
	 * 	The REST servlet/bean instance that this context is defined against.
	 * @return A new default request attributes sub-builder.
	 */
	protected NamedAttributeList.Builder createDefaultRequestAttributes(BeanStore beanStore, RestContextBuilder parent, Supplier<?> resource) {

		Value<NamedAttributeList.Builder> v = Value.of(
			parent.defaultRequestAttributes().copy()
		);

		BeanStore
			.of(beanStore, resource)
			.addBean(NamedAttributeList.Builder.class, v.get())
			.createMethodFinder(NamedAttributeList.Builder.class, resource)
			.find("createDefaultRequestAttributes", Method.class)
			.run(x -> v.set(x));

		BeanStore
			.of(beanStore, resource)
			.addBean(NamedAttributeList.Builder.class, v.get())
			.createMethodFinder(NamedAttributeList.class, resource)
			.find("createDefaultRequestAttributes", Method.class)
			.run(x -> v.get().impl(x));

		return v.get();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// defaultRequestQuery
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the default request query data sub-builder.
	 *
	 * @return The default request query data sub-builder.
	 */
	public final PartList.Builder defaultRequestQueryData() {
		if (defaultRequestQueryData == null)
			defaultRequestQueryData = createDefaultRequestQueryData(beanStore(), parent, resource());
		return defaultRequestQueryData;
	}

	/**
	 * Applies an operation to the default request query data sub-builder.
	 *
	 * <p>
	 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestOpContext <jv>context</jv> = RestOpContext
	 * 		.<jsm>create</jsm>(<jv>method</jv>, <jv>restContext</jv>)
	 * 		.defaultRequestQueryData(<jv>x</jv> -&gt; <jv>x</jv>.add(BasicPart.<jsm>of</jsm>(<js>"foo"</js>, ()-&gt;<jsm>getFoo</jsm>()))
	 * 		.build();
	 * </p>
	 *
	 * @param operation The operation to apply.
	 * @return This object.
	 */
	public final RestOpContextBuilder defaultRequestQueryData(Consumer<PartList.Builder> operation) {
		operation.accept(defaultRequestQueryData());
		return this;
	}

	/**
	 * Instantiates the default request query data sub-builder.
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param parent
	 * 	The builder for the REST resource class.
	 * @param resource
	 * 	The REST servlet/bean instance that this context is defined against.
	 * @return A new default request query data sub-builder.
	 */
	protected PartList.Builder createDefaultRequestQueryData(BeanStore beanStore, RestContextBuilder parent, Supplier<?> resource) {

		Value<PartList.Builder> v = Value.of(
			PartList.create()
		);

		BeanStore
			.of(beanStore, resource)
			.addBean(PartList.Builder.class, v.get())
			.createMethodFinder(PartList.Builder.class, resource)
			.find("createDefaultRequestQueryData", Method.class)
			.thenFind("createDefaultRequestQueryData")
			.run(x -> v.set(x));

		BeanStore
			.of(beanStore, resource)
			.addBean(PartList.Builder.class, v.get())
			.createMethodFinder(PartList.class, resource)
			.find("createDefaultRequestQueryData", Method.class)
			.thenFind("createDefaultRequestQueryData")
			.run(x -> v.get().impl(x));

		return v.get();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// defaultRequestFormData
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the default request form data sub-builder.
	 *
	 * @return The default request form data sub-builder.
	 */
	public final PartList.Builder defaultRequestFormData() {
		if (defaultRequestFormData == null)
			defaultRequestFormData = createDefaultRequestFormData(beanStore(), parent, resource());
		return defaultRequestFormData;
	}

	/**
	 * Applies an operation to the default request form data sub-builder.
	 *
	 * <p>
	 * Typically used to allow you to execute operations without breaking the fluent flow of the context builder.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestOpContext <jv>context</jv> = RestOpContext
	 * 		.<jsm>create</jsm>(<jv>method</jv>, <jv>restContext</jv>)
	 * 		.defaultRequestFormData(<jv>x</jv> -&gt; <jv>x</jv>.add(BasicPart.<jsm>of</jsm>(<js>"foo"</js>, ()-&gt;<jsm>getFoo</jsm>()))
	 * 		.build();
	 * </p>
	 *
	 * @param operation The operation to apply.
	 * @return This object.
	 */
	public final RestOpContextBuilder defaultRequestFormData(Consumer<PartList.Builder> operation) {
		operation.accept(defaultRequestFormData());
		return this;
	}

	/**
	 * Instantiates the default request form data sub-builder.
	 *
	 * @param beanStore
	 * 	The factory used for creating beans and retrieving injected beans.
	 * @param parent
	 * 	The builder for the REST resource class.
	 * @param resource
	 * 	The REST servlet/bean instance that this context is defined against.
	 * @return A new default request form data sub-builder.
	 */
	protected PartList.Builder createDefaultRequestFormData(BeanStore beanStore, RestContextBuilder parent, Supplier<?> resource) {

		Value<PartList.Builder> v = Value.of(
			PartList.create()
		);

		BeanStore
			.of(beanStore, resource)
			.addBean(PartList.Builder.class, v.get())
			.createMethodFinder(PartList.Builder.class, resource)
			.find("createDefaultRequestFormData", Method.class)
			.thenFind("createDefaultRequestFormData")
			.run(x -> v.set(x));

		BeanStore
			.of(beanStore, resource)
			.addBean(PartList.Builder.class, v.get())
			.createMethodFinder(PartList.class, resource)
			.find("createDefaultRequestFormData", Method.class)
			.thenFind("createDefaultRequestFormData")
			.run(x -> v.get().impl(x));

		return v.get();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Parameter annotations
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Handles processing of any annotations on parameters.
	 *
	 * <p>
	 * This includes: {@link Header}, {@link Query}, {@link FormData}.
	 */
	protected void processParameterAnnotations() {
		for (Annotation[] aa : restMethod.getParameterAnnotations()) {

			for (Annotation a : aa) {
				if (a instanceof Header) {
					Header h = (Header)a;
					String def = joinnlFirstNonEmptyArray(h._default(), h.df());
					if (def != null) {
						try {
							defaultRequestHeaders().set(basicHeader(firstNonEmpty(h.name(), h.n(), h.value()), parseAnything(def)));
						} catch (ParseException e) {
							throw new ConfigException(e, "Malformed @Header annotation");
						}
					}
				}
				if (a instanceof Query) {
					Query h = (Query)a;
					String def = joinnlFirstNonEmptyArray(h._default(), h.df());
					if (def != null) {
						try {
							defaultRequestQueryData().setDefault(basicPart(firstNonEmpty(h.name(), h.n(), h.value()), parseAnything(def)));
						} catch (ParseException e) {
							throw new ConfigException(e, "Malformed @Query annotation");
						}
					}
				}
				if (a instanceof FormData) {
					FormData h = (FormData)a;
					String def = joinnlFirstNonEmptyArray(h._default(), h.df());
					if (def != null) {
						try {
							defaultRequestFormData().setDefault(basicPart(firstNonEmpty(h.name(), h.n(), h.value()), parseAnything(def)));
						} catch (ParseException e) {
							throw new ConfigException(e, "Malformed @FormData annotation");
						}
					}
				}
			}
		}
	}

	//----------------------------------------------------------------------------------------------------
	// Properties
	//----------------------------------------------------------------------------------------------------

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

	@Override /* GENERATED - BeanContextBuilder */
	public RestOpContextBuilder annotations(Annotation...values) {
		super.annotations(values);
		return this;
	}

	// </FluentSetters>

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//-----------------------------------------------------------------------------------------------------------------

	private String joinnlFirstNonEmptyArray(String[]...s) {
		for (String[] ss : s)
			if (ss.length > 0)
				return joinnl(ss);
		return null;
	}

}