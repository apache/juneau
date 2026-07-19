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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.lang.annotation.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.marshall.encoders.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.rest.server.converter.*;
import org.apache.juneau.rest.server.guard.*;
import org.apache.juneau.rest.server.matcher.*;

/**
 * Utility classes and methods for the {@link RestGet @RestGet} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestOpAnnotatedMethods">@RestOp-Annotated Method Basics</a>
 * </ul>
 */
public class RestGetAnnotation {

	/**
	 * Private constructor to prevent instantiation.
	 */
	private RestGetAnnotation() {
		// Utility class - prevent instantiation
	}

	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.marshall.MarshallingContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	@SuppressWarnings({
		"unchecked" // Type erasure requires cast for Builder inheritance
	})
	public static class Builder extends AnnotationObject.Builder {

		private String[] description = {};
		private Class<? extends RestConverter>[] converters = new Class[0];
		private Class<? extends RestGuard>[] guards = new Class[0];
		private Class<? extends RestMatcher>[] matchers = new Class[0];
		private Class<? extends Encoder>[] encoders = new Class[0];
		private Class<? extends Serializer>[] serializers = new Class[0];
		private OpSwagger swagger = OpSwaggerAnnotation.DEFAULT;
		private String clientVersion = "";
		private String debug = "";
		private String defaultAccept = "";
		private String defaultCharset = "";
		private String observability = "";
		private String asyncCompletionExecutor = "";
		private String metricName = "";
		private String metricTags = "";
		private String problemDetails = "";
		private String rolesDeclared = "";
		private String roleGuard = "";
		private String summary = "";
		private String value = "";
		private String[] defaultRequestQueryData = {};
		private String[] defaultRequestAttributes = {};
		private String[] defaultRequestHeaders = {};
		private String[] defaultResponseHeaders = {};
		private String[] allowedParserOptions = {};
		private String[] allowedSerializerOptions = {};
		private String[] noInherit = {};
		private String[] path = {};
		private String[] produces = {};

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(RestGet.class);
		}

		/**
		 * Instantiates a new {@link RestGet @RestGet} object initialized with this builder.
		 *
		 * @return A new {@link RestGet @RestGet} object.
		 */
		public RestGet build() {
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
		 * Sets the {@link RestGet#clientVersion()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder clientVersion(String value) {
			clientVersion = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#converters()} property on this annotation.
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
		 * Sets the {@link RestGet#debug()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder debug(String value) {
			debug = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#defaultAccept()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultAccept(String value) {
			defaultAccept = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#defaultCharset()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultCharset(String value) {
			defaultCharset = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#defaultRequestAttributes()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultRequestAttributes(String...value) {
			defaultRequestAttributes = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#defaultRequestHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultRequestHeaders(String...value) {
			defaultRequestHeaders = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#defaultRequestQueryData()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultRequestQueryData(String...value) {
			defaultRequestQueryData = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#defaultResponseHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultResponseHeaders(String...value) {
			defaultResponseHeaders = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#encoders()} property on this annotation.
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
		 * Sets the {@link RestGet#guards()} property on this annotation.
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
		 * Sets the {@link RestGet#matchers()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		@SafeVarargs
		public final Builder matchers(Class<? extends RestMatcher>...value) {
			matchers = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#path()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder path(String...value) {
			path = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#problemDetails()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder problemDetails(String value) {
			problemDetails = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#observability()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder observability(String value) {
			observability = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#asyncCompletionExecutor()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder asyncCompletionExecutor(String value) {
			asyncCompletionExecutor = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#metricName()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder metricName(String value) {
			metricName = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#metricTags()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder metricTags(String value) {
			metricTags = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#produces()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder produces(String...value) {
			produces = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#allowedSerializerOptions()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder allowedSerializerOptions(String...value) {
			allowedSerializerOptions = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#allowedParserOptions()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder allowedParserOptions(String...value) {
			allowedParserOptions = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#noInherit()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder noInherit(String...value) {
			noInherit = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#roleGuard()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder roleGuard(String value) {
			roleGuard = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#rolesDeclared()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder rolesDeclared(String value) {
			rolesDeclared = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#serializers()} property on this annotation.
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
		 * Sets the {@link RestGet#summary()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder summary(String value) {
			summary = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#swagger()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder swagger(OpSwagger value) {
			swagger = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#value()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder value(String value) {
			this.value = value;
			return this;
		}
	}

	@SuppressWarnings({
		"java:S2160" // equals() inherited from AnnotationObject compares all annotation interface methods; subclass fields are accessed via those methods
	})
	private static class Object extends AnnotationObject implements RestGet {

		private final String[] description;
		private final Class<? extends RestConverter>[] converters;
		private final Class<? extends RestGuard>[] guards;
		private final Class<? extends RestMatcher>[] matchers;
		private final Class<? extends Encoder>[] encoders;
		private final Class<? extends Serializer>[] serializers;
		private final OpSwagger swagger;
		private final String clientVersion;
		private final String debug;
		private final String defaultAccept;
		private final String defaultCharset;
		private final String observability;
		private final String asyncCompletionExecutor;
		private final String metricName;
		private final String metricTags;
		private final String problemDetails;
		private final String rolesDeclared;
		private final String roleGuard;
		private final String summary;
		private final String value;
		private final String[] defaultRequestQueryData;
		private final String[] defaultRequestAttributes;
		private final String[] defaultRequestHeaders;
		private final String[] defaultResponseHeaders;
		private final String[] allowedParserOptions;
		private final String[] allowedSerializerOptions;
		private final String[] noInherit;
		private final String[] path;
		private final String[] produces;

		Object(RestGetAnnotation.Builder b) {
			super(b);
			description = cp(b.description);
			clientVersion = b.clientVersion;
			converters = cp(b.converters);
			debug = b.debug;
			defaultAccept = b.defaultAccept;
			defaultCharset = b.defaultCharset;
			defaultRequestQueryData = cp(b.defaultRequestQueryData);
			defaultRequestAttributes = cp(b.defaultRequestAttributes);
			defaultRequestHeaders = cp(b.defaultRequestHeaders);
			defaultResponseHeaders = cp(b.defaultResponseHeaders);
			encoders = cp(b.encoders);
			guards = cp(b.guards);
			matchers = cp(b.matchers);
			allowedParserOptions = cp(b.allowedParserOptions);
			allowedSerializerOptions = cp(b.allowedSerializerOptions);
			noInherit = cp(b.noInherit);
			path = cp(b.path);
			observability = b.observability;
			asyncCompletionExecutor = b.asyncCompletionExecutor;
			metricName = b.metricName;
			metricTags = b.metricTags;
			problemDetails = b.problemDetails;
			produces = cp(b.produces);
			roleGuard = b.roleGuard;
			rolesDeclared = b.rolesDeclared;
			serializers = cp(b.serializers);
			summary = b.summary;
			swagger = b.swagger;
			value = b.value;
		}

		@Override /* Overridden from RestGet */
		public String clientVersion() {
			return clientVersion;
		}

		@Override /* Overridden from RestGet */
		public Class<? extends RestConverter>[] converters() {
			return converters;
		}

		@Override /* Overridden from RestGet */
		public String debug() {
			return debug;
		}

		@Override /* Overridden from RestGet */
		public String defaultAccept() {
			return defaultAccept;
		}

		@Override /* Overridden from RestGet */
		public String defaultCharset() {
			return defaultCharset;
		}

		@Override /* Overridden from RestGet */
		public String[] defaultRequestAttributes() {
			return defaultRequestAttributes;
		}

		@Override /* Overridden from RestGet */
		public String[] defaultRequestHeaders() {
			return defaultRequestHeaders;
		}

		@Override /* Overridden from RestGet */
		public String[] defaultRequestQueryData() {
			return defaultRequestQueryData;
		}

		@Override /* Overridden from RestGet */
		public String[] defaultResponseHeaders() {
			return defaultResponseHeaders;
		}

		@Override /* Overridden from RestGet */
		public Class<? extends Encoder>[] encoders() {
			return encoders;
		}

		@Override /* Overridden from RestGet */
		public Class<? extends RestGuard>[] guards() {
			return guards;
		}

		@Override /* Overridden from RestGet */
		public Class<? extends RestMatcher>[] matchers() {
			return matchers;
		}

		@Override /* Overridden from RestGet */
		public String[] allowedParserOptions() {
			return allowedParserOptions;
		}

		@Override /* Overridden from RestGet */
		public String[] allowedSerializerOptions() {
			return allowedSerializerOptions;
		}

		@Override /* Overridden from RestGet */
		public String[] noInherit() {
			return noInherit;
		}

		@Override /* Overridden from RestGet */
		public String[] path() {
			return path;
		}

		@Override /* Overridden from RestGet */
		public String observability() {
			return observability;
		}

		@Override /* Overridden from RestGet */
		public String asyncCompletionExecutor() {
			return asyncCompletionExecutor;
		}

		@Override /* Overridden from RestGet */
		public String metricName() {
			return metricName;
		}

		@Override /* Overridden from RestGet */
		public String metricTags() {
			return metricTags;
		}

		@Override /* Overridden from RestGet */
		public String problemDetails() {
			return problemDetails;
		}

		@Override /* Overridden from RestGet */
		public String[] produces() {
			return produces;
		}

		@Override /* Overridden from RestGet */
		public String roleGuard() {
			return roleGuard;
		}

		@Override /* Overridden from RestGet */
		public String rolesDeclared() {
			return rolesDeclared;
		}

		@Override /* Overridden from RestGet */
		public Class<? extends Serializer>[] serializers() {
			return serializers;
		}

		@Override /* Overridden from RestGet */
		public String summary() {
			return summary;
		}

		@Override /* Overridden from RestGet */
		public OpSwagger swagger() {
			return swagger;
		}

		@Override /* Overridden from RestGet */
		public String value() {
			return value;
		}

		@Override /* Overridden from annotation */
		public String[] description() {
			return description;
		}
	}

	/** Default value */
	public static final RestGet DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static Builder create() {
		return new Builder();
	}
}