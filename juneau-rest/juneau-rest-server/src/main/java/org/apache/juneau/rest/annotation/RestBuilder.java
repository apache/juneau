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

import java.lang.annotation.*;
import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.rest.*;

/**
 * Builder class for the {@link Rest} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
@SuppressWarnings("unchecked")
public class RestBuilder extends TargetedAnnotationTBuilder {

	/** Default value */
	public static final Rest DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static RestBuilder create() {
		return new RestBuilder();
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static RestBuilder create(Class<?>...on) {
		return create().on(on);
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static RestBuilder create(String...on) {
		return create().on(on);
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
		private final String allowBodyParam, allowedHeaderParams, allowedMethodHeaders, allowedMethodParams, clientVersionHeader, config, debug, debugOn, defaultAccept, defaultCharset, defaultContentType, maxInput, messages, path, renderResponseStackTraces, roleGuard, rolesDeclared, siteName, uriAuthority, uriContext, uriRelativity, uriResolution, useClasspathResourceCaching;
		private final String[] consumes, description, flags, mimeTypes, produces, reqAttrs, reqHeaders, resHeaders, staticFileResponseHeaders, staticFiles, title;

		Impl(RestBuilder b) {
			super(b);
			this.allowBodyParam = b.allowBodyParam;
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
			this.useClasspathResourceCaching = b.useClasspathResourceCaching;
			postConstruct();
		}

		@Override /* Rest */
		public String allowBodyParam() {
			return allowBodyParam;
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
		public String useClasspathResourceCaching() {
			return useClasspathResourceCaching;
		}
	}


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
	Logging logging = LoggingBuilder.DEFAULT;
	Property[] properties = {};
	ResourceSwagger swagger = ResourceSwaggerBuilder.DEFAULT;
	String allowBodyParam="", allowedHeaderParams="", allowedMethodHeaders="", allowedMethodParams="", clientVersionHeader="", config="", debug="", debugOn="", defaultAccept="", defaultCharset="", defaultContentType="", maxInput="", messages="", path="", renderResponseStackTraces="", roleGuard="", rolesDeclared="", siteName="", uriAuthority="", uriContext="", uriRelativity="", uriResolution="", useClasspathResourceCaching="";
	String[] consumes={}, description={}, flags={}, mimeTypes={}, produces={}, reqAttrs={}, reqHeaders={}, resHeaders={}, staticFileResponseHeaders={}, staticFiles={}, title={};

	/**
	 * Constructor.
	 */
	public RestBuilder() {
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
	 * Sets the {@link Rest#allowBodyParam()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder allowBodyParam(String value) {
		this.allowBodyParam = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#allowedHeaderParams()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder allowedHeaderParams(String value) {
		this.allowedHeaderParams = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#allowedMethodHeaders()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder allowedMethodHeaders(String value) {
		this.allowedMethodHeaders = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#allowedMethodParams()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder allowedMethodParams(String value) {
		this.allowedMethodParams = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#callLogger()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder callLogger(Class<? extends RestCallLogger> value) {
		this.callLogger = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#children()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder children(Class<?>...value) {
		this.children = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#classpathResourceFinder()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder classpathResourceFinder(Class<? extends ResourceFinder> value) {
		this.classpathResourceFinder = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#clientVersionHeader()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder clientVersionHeader(String value) {
		this.clientVersionHeader = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#config()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder config(String value) {
		this.config = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#consumes()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder consumes(String...value) {
		this.consumes = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#context()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder context(Class<? extends RestContext> value) {
		this.context = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#converters()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder converters(Class<? extends RestConverter>...value) {
		this.converters = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#debug()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder debug(String value) {
		this.debug = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#debugOn()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder debugOn(String value) {
		this.debugOn = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#defaultAccept()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder defaultAccept(String value) {
		this.defaultAccept = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#defaultCharset()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder defaultCharset(String value) {
		this.defaultCharset = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#defaultContentType()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder defaultContentType(String value) {
		this.defaultContentType = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#description()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder description(String...value) {
		this.description = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#encoders()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder encoders(Class<? extends Encoder>...value) {
		this.encoders = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#flags()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder flags(String...value) {
		this.flags = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#guards()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder guards(Class<? extends RestGuard>...value) {
		this.guards = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#infoProvider()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder infoProvider(Class<? extends RestInfoProvider> value) {
		this.infoProvider = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#logging()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder logging(Logging value) {
		this.logging = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#maxInput()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder maxInput(String value) {
		this.maxInput = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#messages()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder messages(String value) {
		this.messages = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#mimeTypes()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder mimeTypes(String...value) {
		this.mimeTypes = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#paramResolvers()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder paramResolvers(Class<? extends RestMethodParam>...value) {
		this.paramResolvers = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#parsers()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder parsers(Class<?>...value) {
		this.parsers = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#partParser()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder partParser(Class<? extends HttpPartParser> value) {
		this.partParser = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#partSerializer()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder partSerializer(Class<? extends HttpPartSerializer> value) {
		this.partSerializer = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#path()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder path(String value) {
		this.path = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#produces()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder produces(String...value) {
		this.produces = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#properties()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder properties(Property...value) {
		this.properties = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#renderResponseStackTraces()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder renderResponseStackTraces(String value) {
		this.renderResponseStackTraces = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#reqAttrs()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder reqAttrs(String...value) {
		this.reqAttrs = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#reqHeaders()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder reqHeaders(String...value) {
		this.reqHeaders = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#resHeaders()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder resHeaders(String...value) {
		this.resHeaders = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#resourceResolver()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder resourceResolver(Class<? extends RestResourceResolver> value) {
		this.resourceResolver = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#responseHandlers()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder responseHandlers(Class<? extends ResponseHandler>...value) {
		this.responseHandlers = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#roleGuard()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder roleGuard(String value) {
		this.roleGuard = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#rolesDeclared()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder rolesDeclared(String value) {
		this.rolesDeclared = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#serializers()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder serializers(Class<?>...value) {
		this.serializers = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#siteName()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder siteName(String value) {
		this.siteName = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#staticFileResponseHeaders()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder staticFileResponseHeaders(String...value) {
		this.staticFileResponseHeaders = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#staticFiles()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder staticFiles(String...value) {
		this.staticFiles = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#swagger()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder swagger(ResourceSwagger value) {
		this.swagger = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#title()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder title(String...value) {
		this.title = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#uriAuthority()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder uriAuthority(String value) {
		this.uriAuthority = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#uriContext()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder uriContext(String value) {
		this.uriContext = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#uriRelativity()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder uriRelativity(String value) {
		this.uriRelativity = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#uriResolution()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder uriResolution(String value) {
		this.uriResolution = value;
		return this;
	}

	/**
	 * Sets the {@link Rest#useClasspathResourceCaching()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestBuilder useClasspathResourceCaching(String value) {
		this.useClasspathResourceCaching = value;
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - TargetedAnnotationBuilder */
	public RestBuilder on(String...values) {
		super.on(values);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTBuilder */
	public RestBuilder on(java.lang.Class<?>...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTBuilder */
	public RestBuilder onClass(java.lang.Class<?>...value) {
		super.onClass(value);
		return this;
	}

	// </FluentSetters>
}
