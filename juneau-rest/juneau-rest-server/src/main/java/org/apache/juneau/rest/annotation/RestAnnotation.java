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

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
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
		Class<? extends RestInfoProvider> infoProvider=RestInfoProvider.Null.class;
		Class<? extends RestParam>[] restParams = new Class[0];
		Class<? extends BeanFactory> beanFactory = BeanFactory.Null.class;
		Class<? extends RestMethodContext> methodContextClass = RestMethodContext.Null.class;
		Class<? extends RestChildren> restChildrenClass = RestChildren.Null.class;
		Class<? extends RestMethods> restMethodsClass = RestMethods.Null.class;
		Class<?>[] children={}, parsers={}, serializers={};
		ResourceSwagger swagger = ResourceSwaggerAnnotation.DEFAULT;
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
		 * Sets the {@link Rest#infoProvider()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder infoProvider(Class<? extends RestInfoProvider> value) {
			this.infoProvider = value;
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
		 * Sets the {@link Rest#methodContextClass()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder methodContextClass(Class<? extends RestMethodContext> value) {
			this.methodContextClass = value;
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
		 * Sets the {@link Rest#restMethodsClass()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder restMethodsClass(Class<? extends RestMethods> value) {
			this.restMethodsClass = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#restParams()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder restParams(Class<? extends RestParam>...value) {
			this.restParams = value;
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
		public Builder swagger(ResourceSwagger value) {
			this.swagger = value;
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
		private final Class<? extends RestInfoProvider> infoProvider;
		private final Class<? extends RestParam>[] restParams;
		private final Class<? extends BeanFactory> beanFactory;
		private final Class<? extends RestMethodContext> methodContextClass;
		private final Class<? extends RestChildren> restChildrenClass;
		private final Class<? extends RestMethods> restMethodsClass;
		private final Class<?>[] children, parsers, serializers;
		private final ResourceSwagger swagger;
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
			this.infoProvider = b.infoProvider;
			this.maxInput = b.maxInput;
			this.messages = b.messages;
			this.methodContextClass = b.methodContextClass;
			this.restChildrenClass = b.restChildrenClass;
			this.restMethodsClass = b.restMethodsClass;
			this.restParams = copyOf(b.restParams);
			this.parsers = copyOf(b.parsers);
			this.partParser = b.partParser;
			this.partSerializer = b.partSerializer;
			this.path = b.path;
			this.produces = copyOf(b.produces);
			this.renderResponseStackTraces = b.renderResponseStackTraces;
			this.responseHandlers = copyOf(b.responseHandlers);
			this.roleGuard = b.roleGuard;
			this.rolesDeclared = b.rolesDeclared;
			this.serializers = copyOf(b.serializers);
			this.siteName = b.siteName;
			this.staticFiles = b.staticFiles;
			this.swagger = b.swagger;
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
		public Class<? extends RestInfoProvider> infoProvider() {
			return infoProvider;
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
		public Class<? extends RestMethodContext> methodContextClass() {
			return methodContextClass;
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
		public Class<? extends RestMethods> restMethodsClass() {
			return restMethodsClass;
		}

		@Override /* Rest */
		public Class<? extends RestParam>[] restParams() {
			return restParams;
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
		public ResourceSwagger swagger() {
			return swagger;
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
	 * Applies {@link Rest} annotations to a {@link PropertyStoreBuilder}.
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
		public void apply(AnnotationInfo<Rest> ai, PropertyStoreBuilder psb, VarResolverSession vr) {
			Rest a = ai.getAnnotation();
			ClassInfo c = ai.getClassOn();

			psb.set(REST_serializers, merge(ConverterUtils.toType(psb.peek(REST_serializers), Object[].class), a.serializers()));
			psb.set(REST_parsers, merge(ConverterUtils.toType(psb.peek(REST_parsers), Object[].class), a.parsers()));
			psb.setIf(a.partSerializer() != HttpPartSerializer.Null.class, REST_partSerializer, a.partSerializer());
			psb.setIf(a.partParser() != HttpPartParser.Null.class, REST_partParser, a.partParser());
			psb.prependTo(REST_encoders, a.encoders());
			psb.setIfNotEmpty(REST_produces, stringList(a.produces()));
			psb.setIfNotEmpty(REST_consumes, stringList(a.consumes()));
			stringStream(a.defaultRequestAttributes()).map(x -> BasicNamedAttribute.ofPair(x)).forEach(x -> psb.appendTo(REST_defaultRequestAttributes, x));
			stringStream(a.defaultRequestHeaders()).map(x -> BasicHeader.ofPair(x)).forEach(x -> psb.appendTo(REST_defaultRequestHeaders, x));
			stringStream(a.defaultResponseHeaders()).map(x -> BasicHeader.ofPair(x)).forEach(x -> psb.appendTo(REST_defaultResponseHeaders, x));
			psb.appendToIfNotEmpty(REST_defaultRequestHeaders, Accept.of(string(a.defaultAccept())));
			psb.appendToIfNotEmpty(REST_defaultRequestHeaders, ContentType.of(string(a.defaultContentType())));
			psb.prependTo(REST_responseHandlers, a.responseHandlers());
			psb.prependTo(REST_converters, a.converters());
			psb.prependTo(REST_guards, reverse(a.guards()));
			psb.prependTo(REST_children, a.children());
			psb.prependTo(REST_restParams, a.restParams());
			psb.setIf(a.contextClass() != RestContext.Null.class, REST_contextClass, a.contextClass());
			psb.setIfNotEmpty(REST_uriContext, string(a.uriContext()));
			psb.setIfNotEmpty(REST_uriAuthority, string(a.uriAuthority()));
			psb.setIfNotEmpty(REST_uriRelativity, string(a.uriRelativity()));
			psb.setIfNotEmpty(REST_uriResolution, string(a.uriResolution()));
			psb.prependTo(REST_messages, Tuple2.of(c.inner(), string(a.messages())));
			psb.setIf(a.fileFinder() != FileFinder.Null.class, REST_fileFinder, a.fileFinder());
			psb.setIf(a.staticFiles() != StaticFiles.Null.class, REST_staticFiles, a.staticFiles());
			psb.setIfNotEmpty(REST_path, trimLeadingSlash(string(a.path())));
			psb.setIfNotEmpty(REST_clientVersionHeader, string(a.clientVersionHeader()));
			psb.setIf(a.beanFactory() != BeanFactory.Null.class, REST_beanFactory, a.beanFactory());
			psb.setIf(a.callLogger() != RestLogger.Null.class, REST_callLogger, a.callLogger());
			psb.setIf(a.infoProvider() != RestInfoProvider.Null.class, REST_infoProvider, a.infoProvider());
			psb.setIf(a.methodContextClass() != RestMethodContext.Null.class, REST_methodContextClass, a.methodContextClass());
			psb.setIf(a.restChildrenClass() != RestChildren.Null.class, REST_restChildrenClass, a.restChildrenClass());
			psb.setIf(a.restMethodsClass() != RestMethods.Null.class, REST_restMethodsClass, a.restMethodsClass());
			psb.setIfNotEmpty(REST_disableAllowBodyParam, bool(a.disableAllowBodyParam()));
			psb.setIfNotEmpty(REST_allowedHeaderParams, string(a.allowedHeaderParams()));
			psb.setIfNotEmpty(REST_allowedMethodHeaders, string(a.allowedMethodHeaders()));
			psb.setIfNotEmpty(REST_allowedMethodParams, string(a.allowedMethodParams()));
			psb.setIfNotEmpty(REST_renderResponseStackTraces, bool(a.renderResponseStackTraces()));
			psb.setIfNotEmpty(REST_defaultCharset, string(a.defaultCharset()));
			psb.setIfNotEmpty(REST_maxInput, string(a.maxInput()));
			psb.setIfNotEmpty(REST_debug, string(a.debug()));
			psb.setIfNotEmpty(REST_debugOn, string(a.debugOn()));
			cdStream(a.rolesDeclared()).forEach(x -> psb.addTo(REST_rolesDeclared, x));
			psb.addToIfNotEmpty(REST_roleGuard, string(a.roleGuard()));
		}

		private String trimLeadingSlash(String value) {
			if (startsWith(value, '/'))
				return value.substring(1);
			return value;
		}
	}

	/**
	 * A collection of {@link Rest @Rest annotations}.
	 */
	@Documented
	@Target({METHOD,TYPE})
	@Retention(RUNTIME)
	@Inherited
	public static @interface Array {

		/**
		 * The child annotations.
		 */
		Rest[] value();
	}
}