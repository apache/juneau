/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.server;

import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.lang.annotation.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.rest.server.arg.*;
import org.apache.juneau.rest.server.converter.*;
import org.apache.juneau.rest.server.guard.*;
import org.apache.juneau.rest.server.logger.*;
import org.apache.juneau.rest.server.openapi.*;
import org.apache.juneau.rest.server.processor.*;
import org.apache.juneau.rest.server.staticfile.*;
import org.apache.juneau.rest.server.swagger.*;
import org.apache.juneau.serializer.*;

/**
 * Utility classes and methods for the {@link Rest @Rest} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestAnnotatedClassBasics">@Rest-Annotated Class Basics</a>
 * </ul>
 */
public class RestAnnotation {

	/**
	 * Prevents instantiation.
	 */
	private RestAnnotation() {}

	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.MarshallingContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	@SuppressWarnings({
		"unchecked" // Type erasure requires cast for Builder inheritance
	})
	public static class Builder extends AnnotationObject.Builder {

		private String[] description = {};
		private Class<? extends Encoder>[] encoders = new Class[0];
		private Class<? extends HttpPartParser> partParser = HttpPartParser.Void.class;
		private Class<? extends HttpPartSerializer> partSerializer = HttpPartSerializer.Void.class;
		private Class<? extends StaticFiles> staticFiles = StaticFiles.Void.class;
		private Class<? extends ResponseProcessor>[] responseProcessors = new Class[0];
		private Class<? extends CallLogger> callLogger = CallLogger.Void.class;
		private Class<? extends RestConverter>[] converters = new Class[0];
		private Class<? extends RestGuard>[] guards = new Class[0];
		private Class<? extends SwaggerProvider> swaggerProvider = SwaggerProvider.Void.class;
		private Class<? extends OpenApiProvider> openApiProvider = OpenApiProvider.Void.class;
		private Class<? extends RestOpArg>[] restOpArgs = new Class[0];
		private Debug debug = DebugAnnotation.DEFAULT;
		private Class<? extends Serializer>[] serializers = new Class[0];
		private Class<?>[] children = {};
		private Class<?>[] mixins = {};
		private Class<?>[] parsers = {};
		private Swagger swagger = SwaggerAnnotation.DEFAULT;
		private String disableContentParam = "";
		private String allowedHeaderParams = "";
		private String allowedMethodHeaders = "";
		private String allowedMethodParams = "";
		private String clientVersionHeader = "";
		private String config = "";
		private String eagerInit = "";
		private String lazyChildren = "";
		private String defaultAccept = "";
		private String defaultCharset = "";
		private String defaultContentType = "";
		private String maxInput = "";
		private String messages = "";
		private String path = "";
		private String[] paths = {};
		private String observability = "";
		private String problemDetails = "";
		private String virtualThreads = "";
		private String asyncTimeoutMillis = "";
		private String asyncCompletionExecutor = "";
		private String renderResponseStackTraces = "";
		private String roleGuard = "";
		private String rolesDeclared = "";
		private String siteName = "";
		private String uriAuthority = "";
		private String uriContext = "";
		private String uriRelativity = "";
		private String uriResolution = "";
		private String[] allowedParserOptions = {};
		private String[] allowedSerializerOptions = {};
		private String[] noInherit = {};
		private String[] consumes = {};
		private String[] defaultRequestAttributes = {};
		private String[] defaultRequestHeaders = {};
		private String[] defaultResponseHeaders = {};
		private String[] produces = {};
		private String[] title = {};
		private Query[] queryParams = new Query[0];
		private Header[] headerParams = new Header[0];
		private Path[] pathParams = new Path[0];
		private FormData[] formDataParams = new FormData[0];

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(Rest.class);
		}

		/**
		 * Sets the {@link Rest#allowedHeaderParams()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder allowedHeaderParams(String value) {
			allowedHeaderParams = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#allowedMethodHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder allowedMethodHeaders(String value) {
			allowedMethodHeaders = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#allowedMethodParams()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder allowedMethodParams(String value) {
			allowedMethodParams = value;
			return this;
		}

		/**
		 * Instantiates a new {@link Rest @Rest} object initialized with this builder.
		 *
		 * @return A new {@link Rest @Rest} object.
		 */
		public Rest build() {
			return new Object(this);
		}

		/**
		 * Sets the description property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder description(String...value) {
			description = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#callLogger()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder callLogger(Class<? extends CallLogger> value) {
			callLogger = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#children()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder children(Class<?>...value) {
			children = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#mixins()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder mixins(Class<?>...value) {
			mixins = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#clientVersionHeader()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder clientVersionHeader(String value) {
			clientVersionHeader = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#config()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder config(String value) {
			config = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#eagerInit()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder eagerInit(String value) {
			eagerInit = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#lazyChildren()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder lazyChildren(String value) {
			lazyChildren = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#consumes()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder consumes(String...value) {
			consumes = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#allowedSerializerOptions()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder allowedSerializerOptions(String...value) {
			allowedSerializerOptions = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#allowedParserOptions()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder allowedParserOptions(String...value) {
			allowedParserOptions = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#noInherit()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder noInherit(String...value) {
			noInherit = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#converters()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		@SafeVarargs
		public final Builder converters(Class<? extends RestConverter>...value) {
			converters = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#debug()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder debug(String value) {
			debug = DebugAnnotation.create().value(value).build();
			return this;
		}

		/**
		 * Sets the {@link Rest#debug()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder debug(Debug value) {
			debug = value == null ? DebugAnnotation.DEFAULT : value;
			return this;
		}

		/**
		 * Sets the {@link Rest#defaultAccept()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultAccept(String value) {
			defaultAccept = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#defaultCharset()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultCharset(String value) {
			defaultCharset = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#defaultContentType()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultContentType(String value) {
			defaultContentType = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#defaultRequestAttributes()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultRequestAttributes(String...value) {
			defaultRequestAttributes = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#defaultRequestHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultRequestHeaders(String...value) {
			defaultRequestHeaders = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#defaultResponseHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultResponseHeaders(String...value) {
			defaultResponseHeaders = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#disableContentParam()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder disableContentParam(String value) {
			disableContentParam = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#encoders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		@SafeVarargs
		public final Builder encoders(Class<? extends Encoder>...value) {
			encoders = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#guards()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		@SafeVarargs
		public final Builder guards(Class<? extends RestGuard>...value) {
			guards = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#maxInput()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maxInput(String value) {
			maxInput = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#messages()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder messages(String value) {
			messages = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#parsers()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder parsers(Class<?>...value) {
			parsers = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#partParser()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder partParser(Class<? extends HttpPartParser> value) {
			partParser = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#partSerializer()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder partSerializer(Class<? extends HttpPartSerializer> value) {
			partSerializer = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#path()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder path(String value) {
			path = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#paths()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder paths(String...value) {
			paths = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#produces()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder produces(String...value) {
			produces = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#problemDetails()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder problemDetails(String value) {
			problemDetails = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#observability()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder observability(String value) {
			observability = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#virtualThreads()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder virtualThreads(String value) {
			virtualThreads = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#asyncTimeoutMillis()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder asyncTimeoutMillis(String value) {
			asyncTimeoutMillis = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#asyncCompletionExecutor()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder asyncCompletionExecutor(String value) {
			asyncCompletionExecutor = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#renderResponseStackTraces()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder renderResponseStackTraces(String value) {
			renderResponseStackTraces = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#responseProcessors()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		@SafeVarargs
		public final Builder responseProcessors(Class<? extends ResponseProcessor>...value) {
			responseProcessors = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#restOpArgs()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		@SafeVarargs
		public final Builder restOpArgs(Class<? extends RestOpArg>...value) {
			restOpArgs = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#roleGuard()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder roleGuard(String value) {
			roleGuard = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#rolesDeclared()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder rolesDeclared(String value) {
			rolesDeclared = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#serializers()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		@SafeVarargs
		public final Builder serializers(Class<? extends Serializer>...value) {
			serializers = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#siteName()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder siteName(String value) {
			siteName = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#staticFiles()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder staticFiles(Class<? extends StaticFiles> value) {
			staticFiles = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#swagger()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder swagger(Swagger value) {
			swagger = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#swaggerProvider()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder swaggerProvider(Class<? extends SwaggerProvider> value) {
			swaggerProvider = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#openApiProvider()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder openApiProvider(Class<? extends OpenApiProvider> value) {
			openApiProvider = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#title()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder title(String...value) {
			title = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#uriAuthority()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder uriAuthority(String value) {
			uriAuthority = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#uriContext()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder uriContext(String value) {
			uriContext = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#uriRelativity()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder uriRelativity(String value) {
			uriRelativity = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#uriResolution()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder uriResolution(String value) {
			uriResolution = value;
			return this;
		}

	}

	@SuppressWarnings({
		"java:S2160" // equals() inherited from AnnotationObject compares all annotation interface methods; subclass fields are accessed via those methods
	})
	private static class Object extends AnnotationObject implements Rest {

		private final String[] description;
		private final Class<? extends Encoder>[] encoders;
		private final Class<? extends HttpPartParser> partParser;
		private final Class<? extends HttpPartSerializer> partSerializer;
		private final Class<? extends StaticFiles> staticFiles;
		private final Class<? extends ResponseProcessor>[] responseProcessors;
		private final Class<? extends CallLogger> callLogger;
		private final Class<? extends RestConverter>[] converters;
		private final Class<? extends RestGuard>[] guards;
		private final Class<? extends SwaggerProvider> swaggerProvider;
		private final Class<? extends OpenApiProvider> openApiProvider;
		private final Class<? extends RestOpArg>[] restOpArgs;
		private final Debug debug;
		private final Class<? extends Serializer>[] serializers;
		private final Class<?>[] children;
		private final Class<?>[] mixins;
		private final Class<?>[] parsers;
		private final Swagger swagger;
		private final String disableContentParam;
		private final String allowedHeaderParams;
		private final String allowedMethodHeaders;
		private final String allowedMethodParams;
		private final String clientVersionHeader;
		private final String config;
		private final String eagerInit;
		private final String lazyChildren;
		private final String defaultAccept;
		private final String defaultCharset;
		private final String defaultContentType;
		private final String maxInput;
		private final String messages;
		private final String path;
		private final String[] paths;
		private final String observability;
		private final String problemDetails;
		private final String virtualThreads;
		private final String asyncTimeoutMillis;
		private final String asyncCompletionExecutor;
		private final String renderResponseStackTraces;
		private final String roleGuard;
		private final String rolesDeclared;
		private final String siteName;
		private final String uriAuthority;
		private final String uriContext;
		private final String uriRelativity;
		private final String uriResolution;
		private final String[] allowedParserOptions;
		private final String[] allowedSerializerOptions;
		private final String[] noInherit;
		private final String[] consumes;
		private final String[] produces;
		private final String[] defaultRequestAttributes;
		private final String[] defaultRequestHeaders;
		private final String[] defaultResponserHeaders;
		private final String[] title;
		private final Query[] queryParams;
		private final Header[] headerParams;
		private final Path[] pathParams;
		private final FormData[] formDataParams;

		Object(RestAnnotation.Builder b) {
			super(b);
			description = copyOf(b.description);
			disableContentParam = b.disableContentParam;
			allowedHeaderParams = b.allowedHeaderParams;
			allowedMethodHeaders = b.allowedMethodHeaders;
			allowedMethodParams = b.allowedMethodParams;
			callLogger = b.callLogger;
			children = copyOf(b.children);
			mixins = copyOf(b.mixins);
			clientVersionHeader = b.clientVersionHeader;
			config = b.config;
			eagerInit = b.eagerInit;
			lazyChildren = b.lazyChildren;
			allowedParserOptions = copyOf(b.allowedParserOptions);
			allowedSerializerOptions = copyOf(b.allowedSerializerOptions);
			noInherit = copyOf(b.noInherit);
			consumes = copyOf(b.consumes);
			converters = copyOf(b.converters);
			debug = b.debug;
			defaultAccept = b.defaultAccept;
			defaultCharset = b.defaultCharset;
			defaultContentType = b.defaultContentType;
			defaultRequestAttributes = copyOf(b.defaultRequestAttributes);
			defaultRequestHeaders = copyOf(b.defaultRequestHeaders);
			defaultResponserHeaders = copyOf(b.defaultResponseHeaders);
			encoders = copyOf(b.encoders);
			guards = copyOf(b.guards);
			maxInput = b.maxInput;
			messages = b.messages;
			parsers = copyOf(b.parsers);
			partParser = b.partParser;
			partSerializer = b.partSerializer;
			path = b.path;
			paths = copyOf(b.paths);
			observability = b.observability;
			problemDetails = b.problemDetails;
			virtualThreads = b.virtualThreads;
			asyncTimeoutMillis = b.asyncTimeoutMillis;
			asyncCompletionExecutor = b.asyncCompletionExecutor;
			produces = copyOf(b.produces);
			renderResponseStackTraces = b.renderResponseStackTraces;
			responseProcessors = copyOf(b.responseProcessors);
			restOpArgs = copyOf(b.restOpArgs);
			roleGuard = b.roleGuard;
			rolesDeclared = b.rolesDeclared;
			serializers = copyOf(b.serializers);
			siteName = b.siteName;
			staticFiles = b.staticFiles;
			swagger = b.swagger;
			swaggerProvider = b.swaggerProvider;
			openApiProvider = b.openApiProvider;
			title = copyOf(b.title);
			uriAuthority = b.uriAuthority;
			uriContext = b.uriContext;
			uriRelativity = b.uriRelativity;
			uriResolution = b.uriResolution;
			queryParams = copyOf(b.queryParams);
			headerParams = copyOf(b.headerParams);
			pathParams = copyOf(b.pathParams);
			formDataParams = copyOf(b.formDataParams);
		}

		@Override /* Overridden from Rest */
		public String allowedHeaderParams() {
			return allowedHeaderParams;
		}

		@Override /* Overridden from Rest */
		public String allowedMethodHeaders() {
			return allowedMethodHeaders;
		}

		@Override /* Overridden from Rest */
		public String allowedMethodParams() {
			return allowedMethodParams;
		}

		@Override /* Overridden from Rest */
		public Class<? extends CallLogger> callLogger() {
			return callLogger;
		}

		@Override /* Overridden from Rest */
		public Class<?>[] children() {
			return children;
		}

		@Override /* Overridden from Rest */
		public Class<?>[] mixins() {
			return mixins;
		}

		@Override /* Overridden from Rest */
		public String clientVersionHeader() {
			return clientVersionHeader;
		}

		@Override /* Overridden from Rest */
		public String config() {
			return config;
		}

		@Override /* Overridden from Rest */
		public String eagerInit() {
			return eagerInit;
		}

		@Override /* Overridden from Rest */
		public String lazyChildren() {
			return lazyChildren;
		}

		@Override /* Overridden from Rest */
		public String[] allowedParserOptions() {
			return allowedParserOptions;
		}

		@Override /* Overridden from Rest */
		public String[] allowedSerializerOptions() {
			return allowedSerializerOptions;
		}

		@Override /* Overridden from Rest */
		public String[] noInherit() {
			return noInherit;
		}

		@Override /* Overridden from Rest */
		public String[] consumes() {
			return consumes;
		}

		@Override /* Overridden from Rest */
		public Class<? extends RestConverter>[] converters() {
			return converters;
		}

		@Override /* Overridden from Rest */
		public Debug debug() {
			return debug;
		}

		@Override /* Overridden from Rest */
		public String defaultAccept() {
			return defaultAccept;
		}

		@Override /* Overridden from Rest */
		public String defaultCharset() {
			return defaultCharset;
		}

		@Override /* Overridden from Rest */
		public String defaultContentType() {
			return defaultContentType;
		}

		@Override /* Overridden from Rest */
		public String[] defaultRequestAttributes() {
			return defaultRequestAttributes;
		}

		@Override /* Overridden from Rest */
		public String[] defaultRequestHeaders() {
			return defaultRequestHeaders;
		}

		@Override /* Overridden from Rest */
		public String[] defaultResponseHeaders() {
			return defaultResponserHeaders;
		}

		@Override /* Overridden from Rest */
		public String disableContentParam() {
			return disableContentParam;
		}

		@Override /* Overridden from Rest */
		public Class<? extends Encoder>[] encoders() {
			return encoders;
		}

		@Override /* Overridden from Rest */
		public FormData[] formDataParams() {
			return formDataParams;
		}

		@Override /* Overridden from Rest */
		public Class<? extends RestGuard>[] guards() {
			return guards;
		}

		@Override /* Overridden from Rest */
		public Header[] headerParams() {
			return headerParams;
		}

		@Override /* Overridden from Rest */
		public String maxInput() {
			return maxInput;
		}

		@Override /* Overridden from Rest */
		public String messages() {
			return messages;
		}

		@Override /* Overridden from Rest */
		public Class<?>[] parsers() {
			return parsers;
		}

		@Override /* Overridden from Rest */
		public Class<? extends HttpPartParser> partParser() {
			return partParser;
		}

		@Override /* Overridden from Rest */
		public Class<? extends HttpPartSerializer> partSerializer() {
			return partSerializer;
		}

		@Override /* Overridden from Rest */
		public String path() {
			return path;
		}

		@Override /* Overridden from Rest */
		public String[] paths() {
			return paths;
		}

		@Override /* Overridden from Rest */
		public Path[] pathParams() {
			return pathParams;
		}

		@Override /* Overridden from Rest */
		public String[] produces() {
			return produces;
		}

		@Override /* Overridden from Rest */
		public Query[] queryParams() {
			return queryParams;
		}

		@Override /* Overridden from Rest */
		public String observability() {
			return observability;
		}

		@Override /* Overridden from Rest */
		public String problemDetails() {
			return problemDetails;
		}

		@Override /* Overridden from Rest */
		public String virtualThreads() {
			return virtualThreads;
		}

		@Override /* Overridden from Rest */
		public String asyncTimeoutMillis() {
			return asyncTimeoutMillis;
		}

		@Override /* Overridden from Rest */
		public String asyncCompletionExecutor() {
			return asyncCompletionExecutor;
		}

		@Override /* Overridden from Rest */
		public String renderResponseStackTraces() {
			return renderResponseStackTraces;
		}

		@Override /* Overridden from Rest */
		public Class<? extends ResponseProcessor>[] responseProcessors() {
			return responseProcessors;
		}

		@Override /* Overridden from Rest */
		public Class<? extends RestOpArg>[] restOpArgs() {
			return restOpArgs;
		}

		@Override /* Overridden from Rest */
		public String roleGuard() {
			return roleGuard;
		}

		@Override /* Overridden from Rest */
		public String rolesDeclared() {
			return rolesDeclared;
		}

		@Override /* Overridden from Rest */
		public Class<? extends Serializer>[] serializers() {
			return serializers;
		}

		@Override /* Overridden from Rest */
		public String siteName() {
			return siteName;
		}

		@Override /* Overridden from Rest */
		public Class<? extends StaticFiles> staticFiles() {
			return staticFiles;
		}

		@Override /* Overridden from Rest */
		public Swagger swagger() {
			return swagger;
		}

		@Override /* Overridden from Rest */
		public Class<? extends SwaggerProvider> swaggerProvider() {
			return swaggerProvider;
		}

		@Override /* Overridden from Rest */
		public Class<? extends OpenApiProvider> openApiProvider() {
			return openApiProvider;
		}

		@Override /* Overridden from Rest */
		public String[] title() {
			return title;
		}

		@Override /* Overridden from Rest */
		public String uriAuthority() {
			return uriAuthority;
		}

		@Override /* Overridden from Rest */
		public String uriContext() {
			return uriContext;
		}

		@Override /* Overridden from Rest */
		public String uriRelativity() {
			return uriRelativity;
		}

		@Override /* Overridden from Rest */
		public String uriResolution() {
			return uriResolution;
		}

		@Override /* Overridden from annotation */
		public String[] description() {
			return description;
		}
	}

	/** Default value */
	public static final Rest DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static Builder create() {
		return new Builder();
	}

}