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
import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.Logging;
import org.apache.juneau.rest.util.*;
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
		Class<? extends ResourceFinder> classpathResourceFinder = ResourceFinder.Null.class;
		Class<? extends ResponseHandler>[] responseHandlers = new Class[0];
		Class<? extends RestCallLogger> callLogger = RestCallLogger.Null.class;
		Class<? extends RestContext> context = RestContext.Null.class;
		Class<? extends RestConverter>[] converters = new Class[0];
		Class<? extends RestGuard>[] guards = new Class[0];
		Class<? extends RestInfoProvider> infoProvider=RestInfoProvider.Null.class;
		Class<? extends RestMethodParam>[] paramResolvers = new Class[0];
		Class<? extends RestResourceResolver> resourceResolver=RestResourceResolver.Null.class;
		Class<?>[] children={}, parsers={}, serializers={};
		Logging logging = LoggingAnnotation.DEFAULT;
		Property[] properties = {};
		ResourceSwagger swagger = ResourceSwaggerAnnotation.DEFAULT;
		String disableAllowBodyParam="", allowedHeaderParams="", allowedMethodHeaders="", allowedMethodParams="", clientVersionHeader="", config="", debug="", debugOn="", defaultAccept="", defaultCharset="", defaultContentType="", maxInput="", messages="", path="", renderResponseStackTraces="", roleGuard="", rolesDeclared="", siteName="", uriAuthority="", uriContext="", uriRelativity="", uriResolution="", disableClasspathResourceCaching="";
		String[] consumes={}, description={}, flags={}, mimeTypes={}, produces={}, reqAttrs={}, reqHeaders={}, resHeaders={}, staticFileResponseHeaders={}, staticFiles={}, title={};

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
		 * Sets the {@link Rest#callLogger()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder callLogger(Class<? extends RestCallLogger> value) {
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
		 * Sets the {@link Rest#classpathResourceFinder()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder classpathResourceFinder(Class<? extends ResourceFinder> value) {
			this.classpathResourceFinder = value;
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
		 * Sets the {@link Rest#context()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder context(Class<? extends RestContext> value) {
			this.context = value;
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
		 * Sets the {@link Rest#flags()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder flags(String...value) {
			this.flags = value;
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
		 * Sets the {@link Rest#logging()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder logging(Logging value) {
			this.logging = value;
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
		 * Sets the {@link Rest#mimeTypes()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder mimeTypes(String...value) {
			this.mimeTypes = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#paramResolvers()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder paramResolvers(Class<? extends RestMethodParam>...value) {
			this.paramResolvers = value;
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
		 * Sets the {@link Rest#properties()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder properties(Property...value) {
			this.properties = value;
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
		 * Sets the {@link Rest#reqAttrs()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder reqAttrs(String...value) {
			this.reqAttrs = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#reqHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder reqHeaders(String...value) {
			this.reqHeaders = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#resHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder resHeaders(String...value) {
			this.resHeaders = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#resourceResolver()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder resourceResolver(Class<? extends RestResourceResolver> value) {
			this.resourceResolver = value;
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
		 * Sets the {@link Rest#staticFileResponseHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder staticFileResponseHeaders(String...value) {
			this.staticFileResponseHeaders = value;
			return this;
		}

		/**
		 * Sets the {@link Rest#staticFiles()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder staticFiles(String...value) {
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

		/**
		 * Sets the {@link Rest#disableClasspathResourceCaching()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder disableClasspathResourceCaching(String value) {
			this.disableClasspathResourceCaching = value;
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
		private final Class<? extends ResourceFinder> classpathResourceFinder;
		private final Class<? extends ResponseHandler>[] responseHandlers;
		private final Class<? extends RestCallLogger> callLogger;
		private final Class<? extends RestContext> context;
		private final Class<? extends RestConverter>[] converters;
		private final Class<? extends RestGuard>[] guards;
		private final Class<? extends RestInfoProvider> infoProvider;
		private final Class<? extends RestMethodParam>[] paramResolvers;
		private final Class<? extends RestResourceResolver> resourceResolver;
		private final Class<?>[] children, parsers, serializers;
		private final Logging logging;
		private final Property[] properties;
		private final ResourceSwagger swagger;
		private final String disableAllowBodyParam, allowedHeaderParams, allowedMethodHeaders, allowedMethodParams, clientVersionHeader, config, debug, debugOn, defaultAccept, defaultCharset, defaultContentType, maxInput, messages, path, renderResponseStackTraces, roleGuard, rolesDeclared, siteName, uriAuthority, uriContext, uriRelativity, uriResolution, disableClasspathResourceCaching;
		private final String[] consumes, description, flags, mimeTypes, produces, reqAttrs, reqHeaders, resHeaders, staticFileResponseHeaders, staticFiles, title;

		Impl(Builder b) {
			super(b);
			this.disableAllowBodyParam = b.disableAllowBodyParam;
			this.allowedHeaderParams = b.allowedHeaderParams;
			this.allowedMethodHeaders = b.allowedMethodHeaders;
			this.allowedMethodParams = b.allowedMethodParams;
			this.callLogger = b.callLogger;
			this.children = copyOf(b.children);
			this.classpathResourceFinder = b.classpathResourceFinder;
			this.clientVersionHeader = b.clientVersionHeader;
			this.config = b.config;
			this.consumes = copyOf(b.consumes);
			this.context = b.context;
			this.converters = copyOf(b.converters);
			this.debug = b.debug;
			this.debugOn = b.debugOn;
			this.defaultAccept = b.defaultAccept;
			this.defaultCharset = b.defaultCharset;
			this.defaultContentType = b.defaultContentType;
			this.description = copyOf(b.description);
			this.encoders = copyOf(b.encoders);
			this.flags = copyOf(b.flags);
			this.guards = copyOf(b.guards);
			this.infoProvider = b.infoProvider;
			this.logging = b.logging;
			this.maxInput = b.maxInput;
			this.messages = b.messages;
			this.mimeTypes = copyOf(b.mimeTypes);
			this.paramResolvers = copyOf(b.paramResolvers);
			this.parsers = copyOf(b.parsers);
			this.partParser = b.partParser;
			this.partSerializer = b.partSerializer;
			this.path = b.path;
			this.produces = copyOf(b.produces);
			this.properties = copyOf(b.properties);
			this.renderResponseStackTraces = b.renderResponseStackTraces;
			this.reqAttrs = copyOf(b.reqAttrs);
			this.reqHeaders = copyOf(b.reqHeaders);
			this.resHeaders = copyOf(b.resHeaders);
			this.resourceResolver = b.resourceResolver;
			this.responseHandlers = copyOf(b.responseHandlers);
			this.roleGuard = b.roleGuard;
			this.rolesDeclared = b.rolesDeclared;
			this.serializers = copyOf(b.serializers);
			this.siteName = b.siteName;
			this.staticFileResponseHeaders = copyOf(b.staticFileResponseHeaders);
			this.staticFiles = copyOf(b.staticFiles);
			this.swagger = b.swagger;
			this.title = copyOf(b.title);
			this.uriAuthority = b.uriAuthority;
			this.uriContext = b.uriContext;
			this.uriRelativity = b.uriRelativity;
			this.uriResolution = b.uriResolution;
			this.disableClasspathResourceCaching = b.disableClasspathResourceCaching;
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
		public Class<? extends RestCallLogger> callLogger() {
			return callLogger;
		}

		@Override /* Rest */
		public Class<?>[] children() {
			return children;
		}

		@Override /* Rest */
		public Class<? extends ResourceFinder> classpathResourceFinder() {
			return classpathResourceFinder;
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
		public Class<? extends RestContext> context() {
			return context;
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
		public String[] description() {
			return description;
		}

		@Override /* Rest */
		public Class<? extends Encoder>[] encoders() {
			return encoders;
		}

		@Override /* Rest */
		public String[] flags() {
			return flags;
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
		public Logging logging() {
			return logging;
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
		public String[] mimeTypes() {
			return mimeTypes;
		}

		@Override /* Rest */
		public Class<? extends RestMethodParam>[] paramResolvers() {
			return paramResolvers;
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
		public Property[] properties() {
			return properties;
		}

		@Override /* Rest */
		public String renderResponseStackTraces() {
			return renderResponseStackTraces;
		}

		@Override /* Rest */
		public String[] reqAttrs() {
			return reqAttrs;
		}

		@Override /* Rest */
		public String[] reqHeaders() {
			return reqHeaders;
		}

		@Override /* Rest */
		public String[] resHeaders() {
			return resHeaders;
		}

		@Override /* Rest */
		public Class<? extends RestResourceResolver> resourceResolver() {
			return resourceResolver;
		}

		@Override /* Rest */
		public Class<? extends ResponseHandler>[] responseHandlers() {
			return responseHandlers;
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
		public String[] staticFileResponseHeaders() {
			return staticFileResponseHeaders;
		}

		@Override /* Rest */
		public String[] staticFiles() {
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

		@Override /* Rest */
		public String disableClasspathResourceCaching() {
			return disableClasspathResourceCaching;
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
			String s = null;
			ClassInfo c = ai.getClassOn();

			for (Property p1 : a.properties()) {
				psb.set(p1.name(), string(p1.value()));  // >>> DEPRECATED - Remove in 9.0 <<<
				psb.putTo(REST_properties, string(p1.name()), string(p1.value()));
			}

			for (String p1 : a.flags()) {
				psb.set(p1, true);  // >>> DEPRECATED - Remove in 9.0 <<<
				psb.putTo(REST_properties, string(p1), true);
			}

			if (a.serializers().length > 0)
				psb.set(REST_serializers, merge(ConverterUtils.toType(psb.peek(REST_serializers), Object[].class), a.serializers()));

			if (a.parsers().length > 0)
				psb.set(REST_parsers, merge(ConverterUtils.toType(psb.peek(REST_parsers), Object[].class), a.parsers()));

			if (a.partSerializer() != HttpPartSerializer.Null.class)
				psb.set(REST_partSerializer, a.partSerializer());

			if (a.partParser() != HttpPartParser.Null.class)
				psb.set(REST_partParser, a.partParser());

			psb.prependTo(REST_encoders, a.encoders());

			if (a.produces().length > 0)
				psb.set(REST_produces, strings(a.produces()));

			if (a.consumes().length > 0)
				psb.set(REST_consumes, strings(a.consumes()));

			for (String ra : strings(a.reqAttrs())) {
				String[] ra2 = RestUtils.parseKeyValuePair(ra);
				if (ra2 == null)
					throw new BasicRuntimeException("Invalid default request attribute specified: ''{0}''.  Must be in the format: ''Name: value''", ra);
				if (isNotEmpty(ra2[1]))
					psb.putTo(REST_reqAttrs, ra2[0], ra2[1]);
			}

			for (String header : strings(a.reqHeaders())) {
				String[] h = RestUtils.parseHeader(header);
				if (h == null)
					throw new BasicRuntimeException("Invalid default request header specified: ''{0}''.  Must be in the format: ''Header-Name: header-value''", header);
				if (isNotEmpty(h[1]))
					psb.putTo(REST_reqHeaders, h[0], h[1]);
			}

			if (a.defaultAccept().length() > 0) {
				s = string(a.defaultAccept());
				if (isNotEmpty(s))
					psb.putTo(REST_reqHeaders, "Accept", s);
			}

			if (a.defaultContentType().length() > 0) {
				s = string(a.defaultContentType());
				if (isNotEmpty(s))
					psb.putTo(REST_reqHeaders, "Content-Type", s);

			}

			for (String header : strings(a.resHeaders())) {
				String[] h = parseHeader(header);
				if (h == null)
					throw new BasicRuntimeException("Invalid default response header specified: ''{0}''.  Must be in the format: ''Header-Name: header-value''", header);
				if (isNotEmpty(h[1]))
					psb.putTo(REST_resHeaders, h[0], h[1]);
			}

			psb.prependTo(REST_responseHandlers, a.responseHandlers());

			psb.prependTo(REST_converters, a.converters());

			psb.prependTo(REST_guards, reverse(a.guards()));

			psb.prependTo(REST_children, a.children());

			psb.prependTo(REST_paramResolvers, a.paramResolvers());

			Class<?> cc = a.context();
			if (! cc.equals(RestContext.Null.class))
				psb.set(REST_context, cc);

			s = string(a.uriContext());
			if (isNotEmpty(s))
				psb.set(REST_uriContext, s);

			s = string(a.uriAuthority());
			if (isNotEmpty(s))
				psb.set(REST_uriAuthority, s);

			s = string(a.uriRelativity());
			if (isNotEmpty(s))
				psb.set(REST_uriRelativity, s);

			s = string(a.uriResolution());
			if (isNotEmpty(s))
				psb.set(REST_uriResolution, s);

			for (String mapping : a.staticFiles()) {
				try {
					for (StaticFileMapping sfm : StaticFileMapping.parse(c.inner(), string(mapping)).riterable())
						psb.prependTo(REST_staticFiles, sfm);
				} catch (ParseException e) {
					throw new ConfigException(e, "Invalid @Resource(staticFiles) value on class ''{0}''", c);
				}
			}

			psb.prependTo(REST_messages, Tuple2.of(c.inner(), string(a.messages())));

			for (String header : strings(a.staticFileResponseHeaders())) {
				String[] h = RestUtils.parseHeader(header);
				if (h == null)
					throw new BasicRuntimeException("Invalid static file response header specified: ''{0}''.  Must be in the format: ''Header-Name: header-value''", header);
				if (isNotEmpty(h[1]))
					psb.putTo(REST_staticFileResponseHeaders, h[0], h[1]);
			}

			if (! a.disableClasspathResourceCaching().isEmpty())
				psb.set(REST_disableClasspathResourceCaching, bool(a.disableClasspathResourceCaching()));

			if (a.classpathResourceFinder() != ResourceFinder.Null.class)
				psb.set(REST_classpathResourceFinder, a.classpathResourceFinder());

			if (! a.path().isEmpty())
				psb.set(REST_path, trimLeadingSlash(string(a.path())));

			if (! a.clientVersionHeader().isEmpty())
				psb.set(REST_clientVersionHeader, string(a.clientVersionHeader()));

			if (a.resourceResolver() != RestResourceResolver.Null.class)
				psb.set(REST_resourceResolver, a.resourceResolver());

			if (a.callLogger() != RestCallLogger.Null.class)
				psb.set(REST_callLogger, a.callLogger());

			if (! LoggingAnnotation.empty(a.logging())) {
				Logging al = a.logging();
				OMap m = new OMap(psb.peek(OMap.class, REST_callLoggerConfig));

				if (! al.useStackTraceHashing().isEmpty())
					m.append("useStackTraceHashing", bool(al.useStackTraceHashing()));

				if (! al.stackTraceHashingTimeout().isEmpty())
					m.append("stackTraceHashingTimeout", integer(al.stackTraceHashingTimeout(), "@Logging(stackTraceHashingTimeout)"));

				if (! al.disabled().isEmpty())
					m.append("disabled", enablement(al.disabled()));

				if (! al.level().isEmpty())
					m.append("level", level(al.level(), "@Logging(level)"));

				if (al.rules().length > 0) {
					OList ol = new OList();
					for (LoggingRule a2 : al.rules()) {
						OMap m2 = new OMap();

						if (! a2.codes().isEmpty())
							m2.append("codes", string(a2.codes()));

						if (! a2.exceptions().isEmpty())
							m2.append("exceptions", string(a2.exceptions()));

						if (! a2.debugOnly().isEmpty())
							 m2.append("debugOnly", bool(a2.debugOnly()));

						if (! a2.level().isEmpty())
							m2.append("level", level(a2.level(), "@LoggingRule(level)"));

						if (! a2.req().isEmpty())
							m2.append("req", string(a2.req()));

						if (! a2.res().isEmpty())
							m2.append("res", string(a2.res()));

						if (! a2.verbose().isEmpty())
							m2.append("verbose", bool(a2.verbose()));

						if (! a2.disabled().isEmpty())
							m2.append("disabled", bool(a2.disabled()));

						ol.add(m2);
					}
					m.put("rules", ol.appendAll(m.getList("rules")));
				}

				psb.set(REST_callLoggerConfig, m);
			}

			if (a.infoProvider() != RestInfoProvider.Null.class)
				psb.set(REST_infoProvider, a.infoProvider());

			if (! a.disableAllowBodyParam().isEmpty())
				psb.set(REST_disableAllowBodyParam, bool(a.disableAllowBodyParam()));

			if (! a.allowedHeaderParams().isEmpty())
				psb.set(REST_allowedHeaderParams, string(a.allowedHeaderParams()));

			if (! a.allowedMethodHeaders().isEmpty())
				psb.set(REST_allowedMethodHeaders, string(a.allowedMethodHeaders()));

			if (! a.allowedMethodParams().isEmpty())
				psb.set(REST_allowedMethodParams, string(a.allowedMethodParams()));

			if (! a.renderResponseStackTraces().isEmpty())
				psb.set(REST_renderResponseStackTraces, bool(a.renderResponseStackTraces()));

			if (! a.defaultCharset().isEmpty())
				psb.set(REST_defaultCharset, string(a.defaultCharset()));

			if (! a.maxInput().isEmpty())
				psb.set(REST_maxInput, string(a.maxInput()));

			if (! a.debug().isEmpty())
				psb.set(REST_debug, string(a.debug()));

			if (! a.debugOn().isEmpty())
				psb.set(REST_debugOn, string(a.debugOn()));

			psb.addTo(REST_mimeTypes, strings(a.mimeTypes()));

			if (! a.rolesDeclared().isEmpty())
				psb.addTo(REST_rolesDeclared, strings(a.rolesDeclared()));

			if (! a.roleGuard().isEmpty())
				psb.addTo(REST_roleGuard, string(a.roleGuard()));
		}

		private String trimLeadingSlash(String value) {
			if (startsWith(value, '/'))
				return value.substring(1);
			return value;
		}

		private Enablement enablement(String in) {
			return Enablement.fromString(string(in));
		}

		private Level level(String in, String loc) {
			try {
				return Level.parse(string(in).toUpperCase());
			} catch (Exception e) {
				throw new ConfigException("Invalid syntax for level on annotation @Rest({0}): {1}", loc, in);
			}
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