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

import static org.apache.juneau.internal.ArrayUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.rest.RestContext.*;
import static org.apache.juneau.rest.util.RestUtils.*;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.logging.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.utils.*;

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

		Class<? extends Encoder>[] encoders = new Class[0];
		Class<? extends HttpPartParser> partParser = HttpPartParser.Null.class;
		Class<? extends HttpPartSerializer> partSerializer = HttpPartSerializer.Null.class;
		Class<? extends FileFinder> fileFinder = FileFinder.Null.class;
		Class<? extends StaticFiles> staticFiles = StaticFiles.Null.class;
		Class<? extends ResponseHandler>[] responseHandlers = new Class[0];
		Class<? extends RestLogger> callLogger = RestLogger.Null.class;
		Class<? extends RestContext> contextClass = RestContext.Null.class;
		Class<? extends RestConverter>[] converters = new Class[0];
		Class<? extends RestGuard>[] guards = new Class[0];
		Class<? extends SwaggerProvider> swaggerProvider = SwaggerProvider.Null.class;
		Class<? extends RestOperationArg>[] restOperationArgs = new Class[0];
		Class<? extends BeanFactory> beanFactory = BeanFactory.Null.class;
		Class<? extends RestOperationContext> restOperationContextClass = RestOperationContext.Null.class;
		Class<? extends RestChildren> restChildrenClass = RestChildren.Null.class;
		Class<? extends RestOperations> restOperationsClass = RestOperations.Null.class;
		Class<? extends DebugEnablement> debugEnablement = DebugEnablement.Null.class;
		Class<?>[] children={}, parsers={}, serializers={};
		Swagger swagger = SwaggerAnnotation.DEFAULT;
		String disableAllowBodyParam="", allowedHeaderParams="", allowedMethodHeaders="", allowedMethodParams="", clientVersionHeader="", config="", debug="", debugOn="", defaultAccept="", defaultCharset="", defaultContentType="", maxInput="", messages="", path="", renderResponseStackTraces="", roleGuard="", rolesDeclared="", siteName="", uriAuthority="", uriContext="", uriRelativity="", uriResolution="";
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
		 * Sets the {@link Rest#disableAllowBodyParam()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder disableAllowBodyParam(String value) {
			this.disableAllowBodyParam = value;
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
		 * Sets the {@link Rest#beanFactory()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder beanFactory(Class<? extends BeanFactory> value) {
			this.beanFactory = value;
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
		 * Sets the {@link Rest#responseHandlers()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder responseHandlers(Class<? extends ResponseHandler>...value) {
			this.responseHandlers = value;
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
		 * Sets the {@link Rest#restOperationContextClass()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder restOperationContextClass(Class<? extends RestOperationContext> value) {
			this.restOperationContextClass = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#restOperationArgs()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder restOperationArgs(Class<? extends RestOperationArg>...value) {
			this.restOperationArgs = value;
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
		public Builder serializers(Class<?>...value) {
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

		private final Class<? extends Encoder>[] encoders;
		private final Class<? extends HttpPartParser> partParser;
		private final Class<? extends HttpPartSerializer> partSerializer;
		private final Class<? extends FileFinder> fileFinder;
		private final Class<? extends StaticFiles> staticFiles;
		private final Class<? extends ResponseHandler>[] responseHandlers;
		private final Class<? extends RestLogger> callLogger;
		private final Class<? extends RestContext> contextClass;
		private final Class<? extends RestConverter>[] converters;
		private final Class<? extends RestGuard>[] guards;
		private final Class<? extends SwaggerProvider> swaggerProvider;
		private final Class<? extends RestOperationArg>[] restOperationArgs;
		private final Class<? extends BeanFactory> beanFactory;
		private final Class<? extends RestOperationContext> restOperationContextClass;
		private final Class<? extends RestChildren> restChildrenClass;
		private final Class<? extends RestOperations> restOperationsClass;
		private final Class<? extends DebugEnablement> debugEnablement;
		private final Class<?>[] children, parsers, serializers;
		private final Swagger swagger;
		private final String disableAllowBodyParam, allowedHeaderParams, allowedMethodHeaders, allowedMethodParams, clientVersionHeader, config, debug, debugOn, defaultAccept, defaultCharset, defaultContentType, maxInput, messages, path, renderResponseStackTraces, roleGuard, rolesDeclared, siteName, uriAuthority, uriContext, uriRelativity, uriResolution;
		private final String[] consumes, description, produces, defaultRequestAttributes, defaultRequestHeaders, defaultResponserHeaders, title;

		Impl(Builder b) {
			super(b);
			this.disableAllowBodyParam = b.disableAllowBodyParam;
			this.allowedHeaderParams = b.allowedHeaderParams;
			this.allowedMethodHeaders = b.allowedMethodHeaders;
			this.allowedMethodParams = b.allowedMethodParams;
			this.beanFactory = b.beanFactory;
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
			this.responseHandlers = copyOf(b.responseHandlers);
			this.restChildrenClass = b.restChildrenClass;
			this.restOperationContextClass = b.restOperationContextClass;
			this.restOperationsClass = b.restOperationsClass;
			this.restOperationArgs = copyOf(b.restOperationArgs);
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
		public String disableAllowBodyParam() {
			return disableAllowBodyParam;
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
		public Class<? extends BeanFactory> beanFactory() {
			return beanFactory;
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
		public Class<? extends ResponseHandler>[] responseHandlers() {
			return responseHandlers;
		}

		@Override /* Rest */
		public Class<? extends RestChildren> restChildrenClass() {
			return restChildrenClass;
		}

		@Override /* Rest */
		public Class<? extends RestOperationContext> restOperationContextClass() {
			return restOperationContextClass;
		}

		@Override /* Rest */
		public Class<? extends RestOperationArg>[] restOperationArgs() {
			return restOperationArgs;
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
		public Class<?>[] serializers() {
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
	 * Applies {@link Rest} annotations to a {@link ContextPropertiesBuilder}.
	 */
	public static class Apply extends ConfigApply<Rest> {

		/**
		 * Constructor.
		 *
		 * @param c The annotation class.
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Apply(Class<Rest> c, VarResolverSession vr) {
			super(c, vr);
		}

		@Override
		public void apply(AnnotationInfo<Rest> ai, ContextPropertiesBuilder cpb, VarResolverSession vr) {
			Rest a = ai.getAnnotation();
			ClassInfo c = ai.getClassOn();

			cpb.set(REST_serializers, merge(ConverterUtils.toType(cpb.peek(REST_serializers), Object[].class), a.serializers()));
			cpb.set(REST_parsers, merge(ConverterUtils.toType(cpb.peek(REST_parsers), Object[].class), a.parsers()));
			cpb.setIf(a.partSerializer() != HttpPartSerializer.Null.class, REST_partSerializer, a.partSerializer());
			cpb.setIf(a.partParser() != HttpPartParser.Null.class, REST_partParser, a.partParser());
			cpb.prependTo(REST_encoders, a.encoders());
			cpb.setIfNotEmpty(REST_produces, stringList(a.produces()));
			cpb.setIfNotEmpty(REST_consumes, stringList(a.consumes()));
			stringStream(a.defaultRequestAttributes()).map(x -> BasicNamedAttribute.ofPair(x)).forEach(x -> cpb.appendTo(REST_defaultRequestAttributes, x));
			stringStream(a.defaultRequestHeaders()).map(x -> BasicHeader.ofPair(x)).forEach(x -> cpb.appendTo(REST_defaultRequestHeaders, x));
			stringStream(a.defaultResponseHeaders()).map(x -> BasicHeader.ofPair(x)).forEach(x -> cpb.appendTo(REST_defaultResponseHeaders, x));
			cpb.appendToIfNotEmpty(REST_defaultRequestHeaders, Accept.of(string(a.defaultAccept())));
			cpb.appendToIfNotEmpty(REST_defaultRequestHeaders, ContentType.of(string(a.defaultContentType())));
			cpb.prependTo(REST_responseHandlers, a.responseHandlers());
			cpb.prependTo(REST_converters, a.converters());
			cpb.prependTo(REST_guards, reverse(a.guards()));
			cpb.prependTo(REST_children, a.children());
			cpb.prependTo(REST_restOperationArgs, a.restOperationArgs());
			cpb.setIf(a.contextClass() != RestContext.Null.class, REST_contextClass, a.contextClass());
			cpb.setIfNotEmpty(REST_uriContext, string(a.uriContext()));
			cpb.setIfNotEmpty(REST_uriAuthority, string(a.uriAuthority()));
			cpb.setIfNotEmpty(REST_uriRelativity, string(a.uriRelativity()));
			cpb.setIfNotEmpty(REST_uriResolution, string(a.uriResolution()));
			cpb.prependTo(REST_messages, Tuple2.of(c.inner(), string(a.messages())));
			cpb.setIf(a.fileFinder() != FileFinder.Null.class, REST_fileFinder, a.fileFinder());
			cpb.setIf(a.staticFiles() != StaticFiles.Null.class, REST_staticFiles, a.staticFiles());
			cpb.setIfNotEmpty(REST_path, trimLeadingSlash(string(a.path())));
			cpb.setIfNotEmpty(REST_clientVersionHeader, string(a.clientVersionHeader()));
			cpb.setIf(a.beanFactory() != BeanFactory.Null.class, REST_beanFactory, a.beanFactory());
			cpb.setIf(a.callLogger() != RestLogger.Null.class, REST_callLogger, a.callLogger());
			cpb.setIf(a.swaggerProvider() != SwaggerProvider.Null.class, REST_swaggerProvider, a.swaggerProvider());
			cpb.setIf(a.restOperationContextClass() != RestOperationContext.Null.class, REST_restOperationContextClass, a.restOperationContextClass());
			cpb.setIf(a.restChildrenClass() != RestChildren.Null.class, REST_restChildrenClass, a.restChildrenClass());
			cpb.setIf(a.restOperationsClass() != RestOperations.Null.class, REST_restOperationsClass, a.restOperationsClass());
			cpb.setIf(a.debugEnablement() != DebugEnablement.Null.class, REST_debugEnablement, a.debugEnablement());
			cpb.setIfNotEmpty(REST_disableAllowBodyParam, bool(a.disableAllowBodyParam()));
			cpb.setIfNotEmpty(REST_allowedHeaderParams, string(a.allowedHeaderParams()));
			cpb.setIfNotEmpty(REST_allowedMethodHeaders, string(a.allowedMethodHeaders()));
			cpb.setIfNotEmpty(REST_allowedMethodParams, string(a.allowedMethodParams()));
			cpb.setIfNotEmpty(REST_renderResponseStackTraces, bool(a.renderResponseStackTraces()));
			cpb.setIfNotEmpty(REST_defaultCharset, string(a.defaultCharset()));
			cpb.setIfNotEmpty(REST_maxInput, string(a.maxInput()));
			cpb.setIfNotEmpty(REST_debug, string(a.debug()));
			cpb.setIfNotEmpty(REST_debugOn, string(a.debugOn()));
			cdStream(a.rolesDeclared()).forEach(x -> cpb.addTo(REST_rolesDeclared, x));
			cpb.addToIfNotEmpty(REST_roleGuard, string(a.roleGuard()));
		}

		private String trimLeadingSlash(String value) {
			if (startsWith(value, '/'))
				return value.substring(1);
			return value;
		}
	}
}