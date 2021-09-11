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
package org.apache.juneau.rest.annotation;

import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.internal.ArrayUtils.*;
import java.lang.annotation.*;
import java.nio.charset.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.logging.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link Rest @Rest} annotation.
 */
public class RestAnnotation {


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

	/**
	 * Builder class for the {@link Rest} annotation.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
	 * </ul>
	 */
	@SuppressWarnings("unchecked")
	public static class Builder extends TargetedAnnotationTBuilder {

		Class<? extends RestContextBuilder> builder = RestContextBuilder.Null.class;
		Class<? extends Encoder>[] encoders = new Class[0];
		Class<? extends HttpPartParser> partParser = HttpPartParser.Null.class;
		Class<? extends HttpPartSerializer> partSerializer = HttpPartSerializer.Null.class;
		Class<? extends FileFinder> fileFinder = FileFinder.Null.class;
		Class<? extends StaticFiles> staticFiles = StaticFiles.Null.class;
		Class<? extends ResponseProcessor>[] responseProcessors = new Class[0];
		Class<? extends RestLogger> callLogger = RestLogger.Null.class;
		Class<? extends RestContext> contextClass = RestContext.Null.class;
		Class<? extends RestConverter>[] converters = new Class[0];
		Class<? extends RestGuard>[] guards = new Class[0];
		Class<? extends SwaggerProvider> swaggerProvider = SwaggerProvider.Null.class;
		Class<? extends RestOpArg>[] restOpArgs = new Class[0];
		Class<? extends BeanStore> beanStore = BeanStore.Null.class;
		Class<? extends RestOpContext> restOpContextClass = RestOpContext.Null.class;
		Class<? extends RestChildren> restChildrenClass = RestChildren.Null.class;
		Class<? extends RestOperations> restOperationsClass = RestOperations.Null.class;
		Class<? extends DebugEnablement> debugEnablement = DebugEnablement.Null.class;
		Class<? extends Serializer>[] serializers = new Class[0];
		Class<?>[] children={}, parsers={};
		Swagger swagger = SwaggerAnnotation.DEFAULT;
		String disableBodyParam="", allowedHeaderParams="", allowedMethodHeaders="", allowedMethodParams="", clientVersionHeader="", config="", debug="", debugOn="", defaultAccept="", defaultCharset="", defaultContentType="", maxInput="", messages="", path="", renderResponseStackTraces="", roleGuard="", rolesDeclared="", siteName="", uriAuthority="", uriContext="", uriRelativity="", uriResolution="";
		String[] consumes={}, defaultRequestAttributes={}, defaultRequestHeaders={}, defaultResponseHeaders={}, description={}, produces={}, title={};

		/**
		 * Constructor.
		 */
		public Builder() {
			super(Rest.class);
		}

		/**
		 * Instantiates a new {@link Rest @Rest} object initialized with this builder.
		 *
		 * @return A new {@link Rest @Rest} object.
		 */
		public Rest build() {
			return new Impl(this);
		}

		/**
		 * Sets the {@link Rest#disableBodyParam()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder disableBodyParam(String value) {
			this.disableBodyParam = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#allowedHeaderParams()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder allowedHeaderParams(String value) {
			this.allowedHeaderParams = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#allowedMethodHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder allowedMethodHeaders(String value) {
			this.allowedMethodHeaders = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#allowedMethodParams()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder allowedMethodParams(String value) {
			this.allowedMethodParams = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#beanStore()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder beanStore(Class<? extends BeanStore> value) {
			this.beanStore = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#callLogger()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder callLogger(Class<? extends RestLogger> value) {
			this.callLogger = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#children()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder children(Class<?>...value) {
			this.children = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#clientVersionHeader()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder clientVersionHeader(String value) {
			this.clientVersionHeader = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#config()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder config(String value) {
			this.config = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#consumes()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder consumes(String...value) {
			this.consumes = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#contextClass()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder contextClass(Class<? extends RestContext> value) {
			this.contextClass = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#converters()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder converters(Class<? extends RestConverter>...value) {
			this.converters = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#debug()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder debug(String value) {
			this.debug = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#debugEnablement()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder debugEnablement(Class<? extends DebugEnablement> value) {
			this.debugEnablement = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#debugOn()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder debugOn(String value) {
			this.debugOn = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#defaultAccept()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultAccept(String value) {
			this.defaultAccept = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#defaultCharset()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultCharset(String value) {
			this.defaultCharset = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#defaultContentType()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultContentType(String value) {
			this.defaultContentType = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#defaultRequestAttributes()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultRequestAttributes(String...value) {
			this.defaultRequestAttributes = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#defaultRequestHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultRequestHeaders(String...value) {
			this.defaultRequestHeaders = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#defaultResponseHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultResponseHeaders(String...value) {
			this.defaultResponseHeaders = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#description()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder description(String...value) {
			this.description = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#encoders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder encoders(Class<? extends Encoder>...value) {
			this.encoders = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#fileFinder()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder fileFinder(Class<? extends FileFinder> value) {
			this.fileFinder = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#guards()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder guards(Class<? extends RestGuard>...value) {
			this.guards = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#maxInput()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder maxInput(String value) {
			this.maxInput = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#messages()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder messages(String value) {
			this.messages = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#parsers()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder parsers(Class<?>...value) {
			this.parsers = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#partParser()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder partParser(Class<? extends HttpPartParser> value) {
			this.partParser = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#partSerializer()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder partSerializer(Class<? extends HttpPartSerializer> value) {
			this.partSerializer = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#path()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder path(String value) {
			this.path = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#produces()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder produces(String...value) {
			this.produces = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#renderResponseStackTraces()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder renderResponseStackTraces(String value) {
			this.renderResponseStackTraces = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#responseProcessors()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder responseProcessors(Class<? extends ResponseProcessor>...value) {
			this.responseProcessors = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#restChildrenClass()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder restChildrenClass(Class<? extends RestChildren> value) {
			this.restChildrenClass = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#restOpContextClass()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder restOpContextClass(Class<? extends RestOpContext> value) {
			this.restOpContextClass = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#restOpArgs()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder restOpArgs(Class<? extends RestOpArg>...value) {
			this.restOpArgs = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#restOperationsClass()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder restOperationsClass(Class<? extends RestOperations> value) {
			this.restOperationsClass = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#roleGuard()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder roleGuard(String value) {
			this.roleGuard = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#rolesDeclared()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder rolesDeclared(String value) {
			this.rolesDeclared = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#serializers()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder serializers(Class<? extends Serializer>...value) {
			this.serializers = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#siteName()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder siteName(String value) {
			this.siteName = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#staticFiles()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder staticFiles(Class<? extends StaticFiles> value) {
			this.staticFiles = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#swagger()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder swagger(Swagger value) {
			this.swagger = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#swaggerProvider()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder swaggerProvider(Class<? extends SwaggerProvider> value) {
			this.swaggerProvider = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#title()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder title(String...value) {
			this.title = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#uriAuthority()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder uriAuthority(String value) {
			this.uriAuthority = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#uriContext()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder uriContext(String value) {
			this.uriContext = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#uriRelativity()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder uriRelativity(String value) {
			this.uriRelativity = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#uriResolution()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder uriResolution(String value) {
			this.uriResolution = value;
			return this;
		}

		// <FluentSetters>

		@Override /* GENERATED - TargetedAnnotationBuilder */
		public Builder on(String...values) {
			super.on(values);
			return this;
		}

		@Override /* GENERATED - TargetedAnnotationTBuilder */
		public Builder on(java.lang.Class<?>...value) {
			super.on(value);
			return this;
		}

		@Override /* GENERATED - TargetedAnnotationTBuilder */
		public Builder onClass(java.lang.Class<?>...value) {
			super.onClass(value);
			return this;
		}

		// </FluentSetters>
	}

	private static class Impl extends TargetedAnnotationTImpl implements Rest {

		private final Class<? extends RestContextBuilder> builder;
		private final Class<? extends Encoder>[] encoders;
		private final Class<? extends HttpPartParser> partParser;
		private final Class<? extends HttpPartSerializer> partSerializer;
		private final Class<? extends FileFinder> fileFinder;
		private final Class<? extends StaticFiles> staticFiles;
		private final Class<? extends ResponseProcessor>[] responseProcessors;
		private final Class<? extends RestLogger> callLogger;
		private final Class<? extends RestContext> contextClass;
		private final Class<? extends RestConverter>[] converters;
		private final Class<? extends RestGuard>[] guards;
		private final Class<? extends SwaggerProvider> swaggerProvider;
		private final Class<? extends RestOpArg>[] restOpArgs;
		private final Class<? extends BeanStore> beanStore;
		private final Class<? extends RestOpContext> restOpContextClass;
		private final Class<? extends RestChildren> restChildrenClass;
		private final Class<? extends RestOperations> restOperationsClass;
		private final Class<? extends DebugEnablement> debugEnablement;
		private final Class<? extends Serializer>[] serializers;
		private final Class<?>[] children, parsers;
		private final Swagger swagger;
		private final String disableBodyParam, allowedHeaderParams, allowedMethodHeaders, allowedMethodParams, clientVersionHeader, config, debug, debugOn, defaultAccept, defaultCharset, defaultContentType, maxInput, messages, path, renderResponseStackTraces, roleGuard, rolesDeclared, siteName, uriAuthority, uriContext, uriRelativity, uriResolution;
		private final String[] consumes, description, produces, defaultRequestAttributes, defaultRequestHeaders, defaultResponserHeaders, title;

		Impl(Builder b) {
			super(b);
			this.builder = b.builder;
			this.disableBodyParam = b.disableBodyParam;
			this.allowedHeaderParams = b.allowedHeaderParams;
			this.allowedMethodHeaders = b.allowedMethodHeaders;
			this.allowedMethodParams = b.allowedMethodParams;
			this.beanStore = b.beanStore;
			this.callLogger = b.callLogger;
			this.children = copyOf(b.children);
			this.clientVersionHeader = b.clientVersionHeader;
			this.config = b.config;
			this.consumes = copyOf(b.consumes);
			this.contextClass = b.contextClass;
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
			this.description = copyOf(b.description);
			this.encoders = copyOf(b.encoders);
			this.fileFinder = b.fileFinder;
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
			this.restOpContextClass = b.restOpContextClass;
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
			postConstruct();
		}

		@Override /* Rest */
		public Class<? extends RestContextBuilder> builder() {
			return builder;
		}

		@Override /* Rest */
		public String disableBodyParam() {
			return disableBodyParam;
		}

		@Override /* Rest */
		public String allowedHeaderParams() {
			return allowedHeaderParams;
		}

		@Override /* Rest */
		public String allowedMethodHeaders() {
			return allowedMethodHeaders;
		}

		@Override /* Rest */
		public String allowedMethodParams() {
			return allowedMethodParams;
		}

		@Override /* Rest */
		public Class<? extends BeanStore> beanStore() {
			return beanStore;
		}

		@Override /* Rest */
		public Class<? extends RestLogger> callLogger() {
			return callLogger;
		}

		@Override /* Rest */
		public Class<?>[] children() {
			return children;
		}

		@Override /* Rest */
		public String clientVersionHeader() {
			return clientVersionHeader;
		}

		@Override /* Rest */
		public String config() {
			return config;
		}

		@Override /* Rest */
		public String[] consumes() {
			return consumes;
		}

		@Override /* Rest */
		public Class<? extends RestContext> contextClass() {
			return contextClass;
		}

		@Override /* Rest */
		public Class<? extends RestConverter>[] converters() {
			return converters;
		}

		@Override /* Rest */
		public String debug() {
			return debug;
		}

		@Override /* Rest */
		public Class<? extends DebugEnablement> debugEnablement() {
			return debugEnablement;
		}

		@Override /* Rest */
		public String debugOn() {
			return debugOn;
		}

		@Override /* Rest */
		public String defaultAccept() {
			return defaultAccept;
		}

		@Override /* Rest */
		public String defaultCharset() {
			return defaultCharset;
		}

		@Override /* Rest */
		public String defaultContentType() {
			return defaultContentType;
		}

		@Override /* Rest */
		public String[] defaultRequestAttributes() {
			return defaultRequestAttributes;
		}

		@Override /* Rest */
		public String[] defaultRequestHeaders() {
			return defaultRequestHeaders;
		}

		@Override /* Rest */
		public String[] defaultResponseHeaders() {
			return defaultResponserHeaders;
		}

		@Override /* Rest */
		public String[] description() {
			return description;
		}

		@Override /* Rest */
		public Class<? extends Encoder>[] encoders() {
			return encoders;
		}

		@Override /* Rest */
		public Class<? extends FileFinder> fileFinder() {
			return fileFinder;
		}

		@Override /* Rest */
		public Class<? extends RestGuard>[] guards() {
			return guards;
		}

		@Override /* Rest */
		public String maxInput() {
			return maxInput;
		}

		@Override /* Rest */
		public String messages() {
			return messages;
		}

		@Override /* Rest */
		public Class<?>[] parsers() {
			return parsers;
		}

		@Override /* Rest */
		public Class<? extends HttpPartParser> partParser() {
			return partParser;
		}

		@Override /* Rest */
		public Class<? extends HttpPartSerializer> partSerializer() {
			return partSerializer;
		}

		@Override /* Rest */
		public String path() {
			return path;
		}

		@Override /* Rest */
		public String[] produces() {
			return produces;
		}

		@Override /* Rest */
		public String renderResponseStackTraces() {
			return renderResponseStackTraces;
		}

		@Override /* Rest */
		public Class<? extends ResponseProcessor>[] responseProcessors() {
			return responseProcessors;
		}

		@Override /* Rest */
		public Class<? extends RestChildren> restChildrenClass() {
			return restChildrenClass;
		}

		@Override /* Rest */
		public Class<? extends RestOpContext> restOpContextClass() {
			return restOpContextClass;
		}

		@Override /* Rest */
		public Class<? extends RestOpArg>[] restOpArgs() {
			return restOpArgs;
		}

		@Override /* Rest */
		public Class<? extends RestOperations> restOperationsClass() {
			return restOperationsClass;
		}

		@Override /* Rest */
		public String roleGuard() {
			return roleGuard;
		}

		@Override /* Rest */
		public String rolesDeclared() {
			return rolesDeclared;
		}

		@Override /* Rest */
		public Class<? extends Serializer>[] serializers() {
			return serializers;
		}

		@Override /* Rest */
		public String siteName() {
			return siteName;
		}

		@Override /* Rest */
		public Class<? extends StaticFiles> staticFiles() {
			return staticFiles;
		}

		@Override /* Rest */
		public Swagger swagger() {
			return swagger;
		}

		@Override /* Rest */
		public Class<? extends SwaggerProvider> swaggerProvider() {
			return swaggerProvider;
		}

		@Override /* Rest */
		public String[] title() {
			return title;
		}

		@Override /* Rest */
		public String uriAuthority() {
			return uriAuthority;
		}

		@Override /* Rest */
		public String uriContext() {
			return uriContext;
		}

		@Override /* Rest */
		public String uriRelativity() {
			return uriRelativity;
		}

		@Override /* Rest */
		public String uriResolution() {
			return uriResolution;
		}
	}

	/**
	 * Applies {@link Rest} annotations to a {@link RestContextBuilder}.
	 */
	public static class RestContextApply extends AnnotationApplier<Rest,RestContextBuilder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public RestContextApply(VarResolverSession vr) {
			super(Rest.class, RestContextBuilder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<Rest> ai, RestContextBuilder b) {
			Rest a = ai.getAnnotation();
			ClassInfo c = ai.getClassOn();

			classes(a.serializers()).ifPresent(x -> b.getSerializers().add(x));
			classes(a.parsers()).ifPresent(x -> b.getParsers().add(x));
			type(a.partSerializer()).ifPresent(x -> b.getPartSerializer().set(x));
			type(a.partParser()).ifPresent(x -> b.getPartParser().set(x));
			strings(a.produces()).map(MediaType::of).forEach(x -> b.produces(x));
			strings(a.consumes()).map(MediaType::of).forEach(x -> b.consumes(x));
			strings(a.defaultRequestAttributes()).map(x -> BasicNamedAttribute.ofPair(x)).forEach(x -> b.defaultRequestAttributes(x));
			strings(a.defaultRequestHeaders()).map(x -> stringHeader(x)).forEach(x -> b.defaultRequestHeaders(x));
			strings(a.defaultResponseHeaders()).map(x -> stringHeader(x)).forEach(x -> b.defaultResponseHeaders(x));
			string(a.defaultAccept()).map(x -> accept(x)).ifPresent(x -> b.defaultRequestHeaders(x));
			string(a.defaultContentType()).map(x -> contentType(x)).ifPresent(x -> b.defaultRequestHeaders(x));
			b.responseProcessors(a.responseProcessors());
			b.children((Object[])a.children());
			b.restOpArgs(a.restOpArgs());
			classes(a.encoders()).ifPresent(x -> b.getEncoders().add(x));
			type(a.contextClass()).ifPresent(x -> b.contextClass(x));
			string(a.uriContext()).ifPresent(x -> b.uriContext(x));
			string(a.uriAuthority()).ifPresent(x -> b.uriAuthority(x));
			string(a.uriRelativity()).map(UriRelativity::valueOf).ifPresent(x -> b.uriRelativity(x));
			string(a.uriResolution()).map(UriResolution::valueOf).ifPresent(x -> b.uriResolution(x));
			b.messages(c.inner(), string(a.messages()).orElse(null));
			type(a.fileFinder()).ifPresent(x -> b.fileFinder(x));
			type(a.staticFiles()).ifPresent(x -> b.staticFiles(x));
			string(a.path()).ifPresent(x -> b.path(x));
			string(a.clientVersionHeader()).ifPresent(x -> b.clientVersionHeader(x));
			type(a.beanStore()).ifPresent(x -> b.beanStore(x));
			type(a.callLogger()).ifPresent(x -> b.callLogger(x));
			type(a.swaggerProvider()).ifPresent(x -> b.swaggerProvider(x));
			type(a.restOpContextClass()).ifPresent(x -> b.restOpContextClass(x));
			type(a.restChildrenClass()).ifPresent(x -> b.restChildrenClass(x));
			type(a.restOperationsClass()).ifPresent(x -> b.restOperationsClass(x));
			type(a.debugEnablement()).ifPresent(x -> b.debugEnablement(x));
			string(a.disableBodyParam()).map(Boolean::parseBoolean).ifPresent(x -> b.disableBodyParam(x));
			string(a.allowedHeaderParams()).ifPresent(x -> b.allowedHeaderParams(x));
			string(a.allowedMethodHeaders()).ifPresent(x -> b.allowedMethodHeaders(x));
			string(a.allowedMethodParams()).ifPresent(x -> b.allowedMethodParams(x));
			bool(a.renderResponseStackTraces()).ifPresent(x -> b.renderResponseStackTraces(x));
			string(a.debug()).map(Enablement::fromString).ifPresent(x -> b.debug(x));
			string(a.debugOn()).ifPresent(x -> b.debugOn(x));
		}
	}

	/**
	 * Applies {@link Rest} annotations to a {@link RestOpContextBuilder}.
	 */
	public static class RestOpContextApply extends AnnotationApplier<Rest,RestOpContextBuilder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public RestOpContextApply(VarResolverSession vr) {
			super(Rest.class, RestOpContextBuilder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<Rest> ai, RestOpContextBuilder b) {
			Rest a = ai.getAnnotation();

			strings(a.produces()).map(MediaType::of).forEach(x -> b.produces(x));
			strings(a.consumes()).map(MediaType::of).forEach(x -> b.consumes(x));
			b.converters(a.converters());
			b.guards(a.guards());
			string(a.defaultCharset()).map(Charset::forName).ifPresent(x -> b.defaultCharset(x));
			string(a.maxInput()).ifPresent(x -> b.maxInput(x));
			strings_cdl(a.rolesDeclared()).forEach(x -> b.rolesDeclared(x));
			string(a.roleGuard()).ifPresent(x -> b.roleGuard(x));
		}
	}
}