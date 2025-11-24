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
package org.apache.juneau.rest.annotation;

import static org.apache.juneau.common.utils.CollectionUtils.*;

import java.lang.annotation.*;
import java.nio.charset.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.common.annotation.*;
import org.apache.juneau.common.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.arg.*;
import org.apache.juneau.rest.converter.*;
import org.apache.juneau.rest.debug.*;
import org.apache.juneau.rest.guard.*;
import org.apache.juneau.rest.httppart.*;
import org.apache.juneau.rest.logger.*;
import org.apache.juneau.rest.processor.*;
import org.apache.juneau.rest.staticfile.*;
import org.apache.juneau.rest.swagger.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link Rest @Rest} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestAnnotatedClassBasics">@Rest-Annotated Class Basics</a>
 * </ul>
 */
public class RestAnnotation {
	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	@SuppressWarnings("unchecked")
	public static class Builder extends AppliedAnnotationObject.BuilderT {

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
		private Class<? extends RestOpArg>[] restOpArgs = new Class[0];
		private Class<? extends BeanStore> beanStore = BeanStore.Void.class;
		private Class<? extends RestChildren> restChildrenClass = RestChildren.Void.class;
		private Class<? extends RestOperations> restOperationsClass = RestOperations.Void.class;
		private Class<? extends DebugEnablement> debugEnablement = DebugEnablement.Void.class;
		private Class<? extends Serializer>[] serializers = new Class[0];
		private Class<?>[] children = {}, parsers = {};
		private Swagger swagger = SwaggerAnnotation.DEFAULT;
		private String disableContentParam = "", allowedHeaderParams = "", allowedMethodHeaders = "", allowedMethodParams = "", clientVersionHeader = "", config = "", debug = "", debugOn = "",
			defaultAccept = "", defaultCharset = "", defaultContentType = "", maxInput = "", messages = "", path = "", renderResponseStackTraces = "", roleGuard = "", rolesDeclared = "",
			siteName = "", uriAuthority = "", uriContext = "", uriRelativity = "", uriResolution = "";
		private String[] consumes = {}, defaultRequestAttributes = {}, defaultRequestHeaders = {}, defaultResponseHeaders = {}, produces = {}, title = {};
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
			this.allowedHeaderParams = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#allowedMethodHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder allowedMethodHeaders(String value) {
			this.allowedMethodHeaders = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#allowedMethodParams()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder allowedMethodParams(String value) {
			this.allowedMethodParams = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#beanStore()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder beanStore(Class<? extends BeanStore> value) {
			this.beanStore = value;
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
			this.description = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#callLogger()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder callLogger(Class<? extends CallLogger> value) {
			this.callLogger = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#children()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder children(Class<?>...value) {
			this.children = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#clientVersionHeader()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder clientVersionHeader(String value) {
			this.clientVersionHeader = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#config()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder config(String value) {
			this.config = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#consumes()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder consumes(String...value) {
			this.consumes = value;
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
			this.converters = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#debug()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder debug(String value) {
			this.debug = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#debugEnablement()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder debugEnablement(Class<? extends DebugEnablement> value) {
			this.debugEnablement = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#debugOn()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder debugOn(String value) {
			this.debugOn = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#defaultAccept()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultAccept(String value) {
			this.defaultAccept = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#defaultCharset()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultCharset(String value) {
			this.defaultCharset = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#defaultContentType()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultContentType(String value) {
			this.defaultContentType = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#defaultRequestAttributes()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultRequestAttributes(String...value) {
			this.defaultRequestAttributes = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#defaultRequestHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultRequestHeaders(String...value) {
			this.defaultRequestHeaders = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#defaultResponseHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultResponseHeaders(String...value) {
			this.defaultResponseHeaders = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#disableContentParam()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder disableContentParam(String value) {
			this.disableContentParam = value;
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
			this.encoders = value;
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
			this.guards = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#maxInput()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maxInput(String value) {
			this.maxInput = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#messages()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder messages(String value) {
			this.messages = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#parsers()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder parsers(Class<?>...value) {
			this.parsers = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#partParser()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder partParser(Class<? extends HttpPartParser> value) {
			this.partParser = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#partSerializer()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder partSerializer(Class<? extends HttpPartSerializer> value) {
			this.partSerializer = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#path()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder path(String value) {
			this.path = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#produces()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder produces(String...value) {
			this.produces = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#renderResponseStackTraces()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder renderResponseStackTraces(String value) {
			this.renderResponseStackTraces = value;
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
			this.responseProcessors = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#restChildrenClass()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder restChildrenClass(Class<? extends RestChildren> value) {
			this.restChildrenClass = value;
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
			this.restOpArgs = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#restOperationsClass()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder restOperationsClass(Class<? extends RestOperations> value) {
			this.restOperationsClass = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#roleGuard()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder roleGuard(String value) {
			this.roleGuard = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#rolesDeclared()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder rolesDeclared(String value) {
			this.rolesDeclared = value;
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
			this.serializers = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#siteName()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder siteName(String value) {
			this.siteName = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#staticFiles()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder staticFiles(Class<? extends StaticFiles> value) {
			this.staticFiles = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#swagger()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder swagger(Swagger value) {
			this.swagger = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#swaggerProvider()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder swaggerProvider(Class<? extends SwaggerProvider> value) {
			this.swaggerProvider = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#title()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder title(String...value) {
			this.title = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#uriAuthority()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder uriAuthority(String value) {
			this.uriAuthority = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#uriContext()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder uriContext(String value) {
			this.uriContext = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#uriRelativity()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder uriRelativity(String value) {
			this.uriRelativity = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#uriResolution()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder uriResolution(String value) {
			this.uriResolution = value;
			return this;
		}

		@Override /* Overridden from AppliedAnnotationObject.Builder */
		public Builder on(String...value) {
			super.on(value);
			return this;
		}
		@Override /* Overridden from AppliedAnnotationObject.BuilderT */
		public Builder on(Class<?>...value) {
			super.on(value);
			return this;
		}

		@Override /* Overridden from AppliedOnClassAnnotationObject.Builder */
		public Builder onClass(Class<?>...value) {
			super.onClass(value);
			return this;
		}
	
		@Override /* Overridden from AppliedAnnotationObject.BuilderT */
		public Builder on(ClassInfo...value) {
			super.on(value);
			return this;
		}

		@Override /* Overridden from AppliedAnnotationObject.BuilderT */
		public Builder onClass(ClassInfo...value) {
			super.onClass(value);
			return this;
		}

	}

	/**
	 * Applies {@link Rest} annotations to a {@link org.apache.juneau.rest.RestContext.Builder}.
	 */
	public static class RestContextApply extends AnnotationApplier<Rest,RestContext.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public RestContextApply(VarResolverSession vr) {
			super(Rest.class, RestContext.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<Rest> ai, RestContext.Builder b) {
			Rest a = ai.inner();

			classes(a.serializers()).ifPresent(x -> b.serializers().add(x));
			classes(a.parsers()).ifPresent(x -> b.parsers().add(x));
			type(a.partSerializer()).ifPresent(x -> b.partSerializer().type(x));
			type(a.partParser()).ifPresent(x -> b.partParser().type(x));
			stream(a.produces()).map(MediaType::of).forEach(x -> b.produces(x));
			stream(a.consumes()).map(MediaType::of).forEach(x -> b.consumes(x));
			stream(a.defaultRequestAttributes()).map(BasicNamedAttribute::ofPair).forEach(x -> b.defaultRequestAttributes(x));
			stream(a.defaultRequestHeaders()).map(HttpHeaders::stringHeader).forEach(x -> b.defaultRequestHeaders(x));
			stream(a.defaultResponseHeaders()).map(HttpHeaders::stringHeader).forEach(x -> b.defaultResponseHeaders(x));
			string(a.defaultAccept()).map(HttpHeaders::accept).ifPresent(x -> b.defaultRequestHeaders(x));
			string(a.defaultContentType()).map(HttpHeaders::contentType).ifPresent(x -> b.defaultRequestHeaders(x));
			b.responseProcessors().add(a.responseProcessors());
			b.children((java.lang.Object[])a.children());
			b.restOpArgs(a.restOpArgs());
			classes(a.encoders()).ifPresent(x -> b.encoders().add(x));
			string(a.uriContext()).ifPresent(x -> b.uriContext(x));
			string(a.uriAuthority()).ifPresent(x -> b.uriAuthority(x));
			string(a.uriRelativity()).map(UriRelativity::valueOf).ifPresent(x -> b.uriRelativity(x));
			string(a.uriResolution()).map(UriResolution::valueOf).ifPresent(x -> b.uriResolution(x));
			b.messages().location(string(a.messages()).orElse(null));
			type(a.staticFiles()).ifPresent(x -> b.staticFiles().type(x));
			string(a.path()).ifPresent(x -> b.path(x));
			string(a.clientVersionHeader()).ifPresent(x -> b.clientVersionHeader(x));
			type(a.callLogger()).ifPresent(x -> b.callLogger().type(x));
			type(a.swaggerProvider()).ifPresent(x -> b.swaggerProvider(x));
			type(a.restChildrenClass()).ifPresent(x -> b.restChildrenClass(x));
			type(a.restOperationsClass()).ifPresent(x -> b.restOperationsClass(x));
			type(a.debugEnablement()).ifPresent(x -> b.debugEnablement().type(x));
			string(a.disableContentParam()).map(Boolean::parseBoolean).ifPresent(x -> b.disableContentParam(x));
			string(a.allowedHeaderParams()).ifPresent(x -> b.allowedHeaderParams(x));
			string(a.allowedMethodHeaders()).ifPresent(x -> b.allowedMethodHeaders(x));
			string(a.allowedMethodParams()).ifPresent(x -> b.allowedMethodParams(x));
			bool(a.renderResponseStackTraces()).ifPresent(x -> b.renderResponseStackTraces(x));
		}
	}

	/**
	 * Applies {@link Rest} annotations to a {@link org.apache.juneau.rest.RestOpContext.Builder}.
	 */
	public static class RestOpContextApply extends AnnotationApplier<Rest,RestOpContext.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public RestOpContextApply(VarResolverSession vr) {
			super(Rest.class, RestOpContext.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<Rest> ai, RestOpContext.Builder b) {
			Rest a = ai.inner();

			stream(a.produces()).map(MediaType::of).forEach(x -> b.produces(x));
			stream(a.consumes()).map(MediaType::of).forEach(x -> b.consumes(x));
			b.converters().append(a.converters());
			b.guards().append(a.guards());
			string(a.defaultCharset()).map(Charset::forName).ifPresent(x -> b.defaultCharset(x));
			string(a.maxInput()).ifPresent(x -> b.maxInput(x));
			cdl(a.rolesDeclared()).forEach(x -> b.rolesDeclared(x));
			string(a.roleGuard()).ifPresent(x -> b.roleGuard(x));
		}
	}

	private static class Object extends AppliedOnClassAnnotationObject implements Rest {

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
		private final Class<? extends RestOpArg>[] restOpArgs;
		private final Class<? extends BeanStore> beanStore;
		private final Class<? extends RestChildren> restChildrenClass;
		private final Class<? extends RestOperations> restOperationsClass;
		private final Class<? extends DebugEnablement> debugEnablement;
		private final Class<? extends Serializer>[] serializers;
		private final Class<?>[] children, parsers;
		private final Swagger swagger;
		private final String disableContentParam, allowedHeaderParams, allowedMethodHeaders, allowedMethodParams, clientVersionHeader, config, debug, debugOn, defaultAccept, defaultCharset,
			defaultContentType, maxInput, messages, path, renderResponseStackTraces, roleGuard, rolesDeclared, siteName, uriAuthority, uriContext, uriRelativity, uriResolution;
		private final String[] consumes, produces, defaultRequestAttributes, defaultRequestHeaders, defaultResponserHeaders, title;
		private final Query[] queryParams;
		private final Header[] headerParams;
		private final Path[] pathParams;
		private final FormData[] formDataParams;

		Object(RestAnnotation.Builder b) {
			super(b);
			this.description = copyOf(b.description);
			this.disableContentParam = b.disableContentParam;
			this.allowedHeaderParams = b.allowedHeaderParams;
			this.allowedMethodHeaders = b.allowedMethodHeaders;
			this.allowedMethodParams = b.allowedMethodParams;
			this.beanStore = b.beanStore;
			this.callLogger = b.callLogger;
			this.children = copyOf(b.children);
			this.clientVersionHeader = b.clientVersionHeader;
			this.config = b.config;
			this.consumes = copyOf(b.consumes);
			this.converters = copyOf(b.converters);
			this.debug = b.debug;
			this.debugEnablement = b.debugEnablement;
			this.debugOn = b.debugOn;
			this.defaultAccept = b.defaultAccept;
			this.defaultCharset = b.defaultCharset;
			this.defaultContentType = b.defaultContentType;
			this.defaultRequestAttributes = copyOf(b.defaultRequestAttributes);
			this.defaultRequestHeaders = copyOf(b.defaultRequestHeaders);
			this.defaultResponserHeaders = copyOf(b.defaultResponseHeaders);
			this.encoders = copyOf(b.encoders);
			this.guards = copyOf(b.guards);
			this.maxInput = b.maxInput;
			this.messages = b.messages;
			this.parsers = copyOf(b.parsers);
			this.partParser = b.partParser;
			this.partSerializer = b.partSerializer;
			this.path = b.path;
			this.produces = copyOf(b.produces);
			this.renderResponseStackTraces = b.renderResponseStackTraces;
			this.responseProcessors = copyOf(b.responseProcessors);
			this.restChildrenClass = b.restChildrenClass;
			this.restOperationsClass = b.restOperationsClass;
			this.restOpArgs = copyOf(b.restOpArgs);
			this.roleGuard = b.roleGuard;
			this.rolesDeclared = b.rolesDeclared;
			this.serializers = copyOf(b.serializers);
			this.siteName = b.siteName;
			this.staticFiles = b.staticFiles;
			this.swagger = b.swagger;
			this.swaggerProvider = b.swaggerProvider;
			this.title = copyOf(b.title);
			this.uriAuthority = b.uriAuthority;
			this.uriContext = b.uriContext;
			this.uriRelativity = b.uriRelativity;
			this.uriResolution = b.uriResolution;
			this.queryParams = copyOf(b.queryParams);
			this.headerParams = copyOf(b.headerParams);
			this.pathParams = copyOf(b.pathParams);
			this.formDataParams = copyOf(b.formDataParams);
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
		public Class<? extends BeanStore> beanStore() {
			return beanStore;
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
		public String clientVersionHeader() {
			return clientVersionHeader;
		}

		@Override /* Overridden from Rest */
		public String config() {
			return config;
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
		public String debug() {
			return debug;
		}

		@Override /* Overridden from Rest */
		public Class<? extends DebugEnablement> debugEnablement() {
			return debugEnablement;
		}

		@Override /* Overridden from Rest */
		public String debugOn() {
			return debugOn;
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
		public String renderResponseStackTraces() {
			return renderResponseStackTraces;
		}

		@Override /* Overridden from Rest */
		public Class<? extends ResponseProcessor>[] responseProcessors() {
			return responseProcessors;
		}

		@Override /* Overridden from Rest */
		public Class<? extends RestChildren> restChildrenClass() {
			return restChildrenClass;
		}

		@Override /* Overridden from Rest */
		public Class<? extends RestOpArg>[] restOpArgs() {
			return restOpArgs;
		}

		@Override /* Overridden from Rest */
		public Class<? extends RestOperations> restOperationsClass() {
			return restOperationsClass;
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

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static Builder create(Class<?>...on) {
		return create().on(on);
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static Builder create(String...on) {
		return create().on(on);
	}
}