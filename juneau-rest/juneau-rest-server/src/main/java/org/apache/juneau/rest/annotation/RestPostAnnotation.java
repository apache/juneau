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
import static org.apache.juneau.rest.RestContext.*;
import static org.apache.juneau.rest.RestOperationContext.*;
import static org.apache.juneau.rest.util.RestUtils.*;
import static org.apache.juneau.http.HttpParts.*;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link RestPost @RestPost} annotation.
 */
public class RestPostAnnotation {


	/** Default value */
	public static final RestPost DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Builder class for the {@link RestPost} annotation.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
	 * </ul>
	 */
	@SuppressWarnings("unchecked")
	public static class Builder extends TargetedAnnotationMBuilder {

		Class<? extends RestConverter>[] converters = new Class[0];
		Class<? extends RestGuard>[] guards = new Class[0];
		Class<? extends RestMatcher>[] matchers = new Class[0];
		Class<? extends RestOperationContext> contextClass = RestOperationContext.Null.class;
		Class<?>[] encoders=new Class<?>[0], parsers=new Class<?>[0], serializers=new Class<?>[0];
		OpSwagger swagger = OpSwaggerAnnotation.DEFAULT;
		String clientVersion="", debug="", defaultAccept="", defaultCharset="", defaultContentType="", maxInput="", rolesDeclared="", roleGuard="", summary="", value="";
		String[] consumes={}, defaultFormData={}, defaultQuery={}, defaultRequestAttributes={}, defaultRequestHeaders={}, defaultResponseHeaders={}, description={}, path={}, produces={};

		/**
		 * Constructor.
		 */
		public Builder() {
			super(RestPost.class);
		}

		/**
		 * Instantiates a new {@link RestPost @RestPost} object initialized with this builder.
		 *
		 * @return A new {@link RestPost @RestPost} object.
		 */
		public RestPost build() {
			return new Impl(this);
		}

		/**
		 * Sets the {@link RestPost#clientVersion()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder clientVersion(String value) {
			this.clientVersion = value;
			return this;
		}

		/**
		 * Sets the {@link RestPost#consumes()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder consumes(String...value) {
			this.consumes = value;
			return this;
		}

		/**
		 * Sets the {@link RestPost#contextClass()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder contextClass(Class<? extends RestOperationContext> value) {
			this.contextClass = value;
			return this;
		}

		/**
		 * Sets the {@link RestPost#converters()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder converters(Class<? extends RestConverter>...value) {
			this.converters = value;
			return this;
		}

		/**
		 * Sets the {@link RestPost#debug()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder debug(String value) {
			this.debug = value;
			return this;
		}

		/**
		 * Sets the {@link RestPost#defaultAccept()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultAccept(String value) {
			this.defaultAccept = value;
			return this;
		}

		/**
		 * Sets the {@link RestPost#defaultCharset()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultCharset(String value) {
			this.defaultCharset = value;
			return this;
		}

		/**
		 * Sets the {@link RestPost#defaultContentType()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultContentType(String value) {
			this.defaultContentType = value;
			return this;
		}

		/**
		 * Sets the {@link RestPost#defaultFormData()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultFormData(String...value) {
			this.defaultFormData = value;
			return this;
		}

		/**
		 * Sets the {@link RestPost#defaultQuery()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultQuery(String...value) {
			this.defaultQuery = value;
			return this;
		}

		/**
		 * Sets the {@link RestPost#defaultRequestAttributes()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultRequestAttributes(String...value) {
			this.defaultRequestAttributes = value;
			return this;
		}

		/**
		 * Sets the {@link RestPost#defaultRequestHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultRequestHeaders(String...value) {
			this.defaultRequestHeaders = value;
			return this;
		}

		/**
		 * Sets the {@link RestPost#defaultResponseHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultResponseHeaders(String...value) {
			this.defaultResponseHeaders = value;
			return this;
		}

		/**
		 * Sets the {@link RestPost#description()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder description(String...value) {
			this.description = value;
			return this;
		}

		/**
		 * Sets the {@link RestPost#encoders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder encoders(Class<?>...value) {
			this.encoders = value;
			return this;
		}

		/**
		 * Sets the {@link RestPost#guards()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder guards(Class<? extends RestGuard>...value) {
			this.guards = value;
			return this;
		}

		/**
		 * Sets the {@link RestPost#matchers()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder matchers(Class<? extends RestMatcher>...value) {
			this.matchers = value;
			return this;
		}

		/**
		 * Sets the {@link RestPost#maxInput()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder maxInput(String value) {
			this.maxInput = value;
			return this;
		}

		/**
		 * Sets the {@link RestPost#parsers()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder parsers(Class<?>...value) {
			this.parsers = value;
			return this;
		}

		/**
		 * Sets the {@link RestPost#path()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder path(String...value) {
			this.path = value;
			return this;
		}

		/**
		 * Sets the {@link RestPost#produces()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder produces(String...value) {
			this.produces = value;
			return this;
		}

		/**
		 * Sets the {@link RestPost#roleGuard()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder roleGuard(String value) {
			this.roleGuard = value;
			return this;
		}

		/**
		 * Sets the {@link RestPost#rolesDeclared()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder rolesDeclared(String value) {
			this.rolesDeclared = value;
			return this;
		}

		/**
		 * Sets the {@link RestPost#serializers()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder serializers(Class<?>...value) {
			this.serializers = value;
			return this;
		}

		/**
		 * Sets the {@link RestPost#summary()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder summary(String value) {
			this.summary = value;
			return this;
		}

		/**
		 * Sets the {@link RestPost#swagger()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder swagger(OpSwagger value) {
			this.swagger = value;
			return this;
		}

		/**
		 * Sets the {@link RestPost#value()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder value(String value) {
			this.value = value;
			return this;
		}

		// <FluentSetters>

		@Override /* GENERATED - TargetedAnnotationBuilder */
		public Builder on(String...values) {
			super.on(values);
			return this;
		}

		@Override /* GENERATED - TargetedAnnotationTMBuilder */
		public Builder on(java.lang.reflect.Method...value) {
			super.on(value);
			return this;
		}

		// </FluentSetters>
	}

	private static class Impl extends TargetedAnnotationImpl implements RestPost {

		private final Class<? extends RestConverter>[] converters;
		private final Class<? extends RestGuard>[] guards;
		private final Class<? extends RestMatcher>[] matchers;
		private final Class<? extends RestOperationContext> contextClass;
		private final Class<?>[] encoders, parsers, serializers;
		private final OpSwagger swagger;
		private final String clientVersion, debug, defaultAccept, defaultCharset, defaultContentType, maxInput, rolesDeclared, roleGuard, summary, value;
		private final String[] consumes, defaultFormData, defaultQuery, defaultRequestAttributes, defaultRequestHeaders, defaultResponseHeaders, description, path, produces;

		Impl(Builder b) {
			super(b);
			this.clientVersion = b.clientVersion;
			this.consumes = copyOf(b.consumes);
			this.contextClass = b.contextClass;
			this.converters = copyOf(b.converters);
			this.debug = b.debug;
			this.defaultAccept = b.defaultAccept;
			this.defaultCharset = b.defaultCharset;
			this.defaultContentType = b.defaultContentType;
			this.defaultFormData = copyOf(b.defaultFormData);
			this.defaultQuery = copyOf(b.defaultQuery);
			this.defaultRequestAttributes = copyOf(b.defaultRequestAttributes);
			this.defaultRequestHeaders = copyOf(b.defaultRequestHeaders);
			this.defaultResponseHeaders = copyOf(b.defaultResponseHeaders);
			this.description = copyOf(b.description);
			this.encoders = copyOf(b.encoders);
			this.guards = copyOf(b.guards);
			this.matchers = copyOf(b.matchers);
			this.maxInput = b.maxInput;
			this.parsers = copyOf(b.parsers);
			this.path = copyOf(b.path);
			this.produces = copyOf(b.produces);
			this.roleGuard = b.roleGuard;
			this.rolesDeclared = b.rolesDeclared;
			this.serializers = copyOf(b.serializers);
			this.summary = b.summary;
			this.swagger = b.swagger;
			this.value = b.value;
			postConstruct();
		}

		@Override /* RestPost */
		public String clientVersion() {
			return clientVersion;
		}

		@Override /* RestPost */
		public String[] consumes() {
			return consumes;
		}

		@Override /* RestPost */
		public Class<? extends RestOperationContext> contextClass() {
			return contextClass;
		}

		@Override /* RestPost */
		public Class<? extends RestConverter>[] converters() {
			return converters;
		}

		@Override /* RestPost */
		public String debug() {
			return debug;
		}

		@Override /* RestPost */
		public String defaultAccept() {
			return defaultAccept;
		}

		@Override /* RestPost */
		public String defaultCharset() {
			return defaultCharset;
		}

		@Override /* RestPost */
		public String defaultContentType() {
			return defaultContentType;
		}

		@Override /* RestPost */
		public String[] defaultFormData() {
			return defaultFormData;
		}

		@Override /* RestPost */
		public String[] defaultQuery() {
			return defaultQuery;
		}

		@Override /* RestPost */
		public String[] defaultRequestAttributes() {
			return defaultRequestAttributes;
		}

		@Override /* RestPost */
		public String[] defaultRequestHeaders() {
			return defaultRequestHeaders;
		}

		@Override /* RestPost */
		public String[] defaultResponseHeaders() {
			return defaultResponseHeaders;
		}

		@Override /* RestPost */
		public String[] description() {
			return description;
		}

		@Override /* RestPost */
		public Class<?>[] encoders() {
			return encoders;
		}

		@Override /* RestPost */
		public Class<? extends RestGuard>[] guards() {
			return guards;
		}

		@Override /* RestPost */
		public Class<? extends RestMatcher>[] matchers() {
			return matchers;
		}

		@Override /* RestPost */
		public String maxInput() {
			return maxInput;
		}

		@Override /* RestPost */
		public Class<?>[] parsers() {
			return parsers;
		}

		@Override /* RestPost */
		public String[] path() {
			return path;
		}

		@Override /* RestPost */
		public String[] produces() {
			return produces;
		}

		@Override /* RestPost */
		public String roleGuard() {
			return roleGuard;
		}

		@Override /* RestPost */
		public String rolesDeclared() {
			return rolesDeclared;
		}

		@Override /* RestPost */
		public Class<?>[] serializers() {
			return serializers;
		}

		@Override /* RestPost */
		public String summary() {
			return summary;
		}

		@Override /* RestPost */
		public OpSwagger swagger() {
			return swagger;
		}

		@Override /* RestPost */
		public String value() {
			return value;
		}
	}

	/**
	 * Applies {@link RestPost} annotations to a {@link ContextPropertiesBuilder}.
	 */
	public static class Apply extends ConfigApply<RestPost> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Apply(VarResolverSession vr) {
			super(RestPost.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<RestPost> ai, ContextPropertiesBuilder b) {
			RestPost a = ai.getAnnotation();

			b.setIfNotEmpty(RESTOP_httpMethod, "post");

			b.set(REST_serializers, merge(ConverterUtils.toType(b.peek(REST_serializers), Object[].class), a.serializers()));
			b.set(REST_parsers, merge(ConverterUtils.toType(b.peek(REST_parsers), Object[].class), a.parsers()));
			b.set(REST_encoders, merge(ConverterUtils.toType(b.peek(REST_encoders), Object[].class), a.encoders()));
			b.setIf(a.contextClass() != RestOperationContext.Null.class, RESTOP_contextClass, a.contextClass());
			b.setIfNotEmpty(REST_produces, stringList(a.produces()));
			b.setIfNotEmpty(REST_consumes, stringList(a.consumes()));
			stringStream(a.defaultRequestHeaders()).map(x -> stringHeader(x)).forEach(x -> b.appendTo(RESTOP_defaultRequestHeaders, x));
			stringStream(a.defaultResponseHeaders()).map(x -> stringHeader(x)).forEach(x -> b.appendTo(RESTOP_defaultResponseHeaders, x));
			stringStream(a.defaultRequestAttributes()).map(x -> BasicNamedAttribute.ofPair(x)).forEach(x -> b.appendTo(RESTOP_defaultRequestAttributes, x));
			stringStream(a.defaultQuery()).map(x -> basicPart(x)).forEach(x -> b.appendTo(RESTOP_defaultQuery, x));
			stringStream(a.defaultFormData()).map(x -> basicPart(x)).forEach(x -> b.appendTo(RESTOP_defaultFormData, x));
			b.appendToIfNotEmpty(REST_defaultRequestHeaders, accept(string(a.defaultAccept())));
			b.appendToIfNotEmpty(REST_defaultRequestHeaders, contentType(string(a.defaultContentType())));
			b.prependTo(REST_converters, a.converters());
			b.prependTo(REST_guards, reverse(a.guards()));
			b.prependTo(RESTOP_matchers, a.matchers());
			b.setIfNotEmpty(RESTOP_clientVersion, a.clientVersion());
			b.setIfNotEmpty(REST_defaultCharset, string(a.defaultCharset()));
			b.setIfNotEmpty(REST_maxInput, string(a.maxInput()));
			stringStream(a.path()).forEach(x -> b.prependTo(RESTOP_path, x));
			b.setIfNotEmpty(RESTOP_path, a.value());
			cdStream(a.rolesDeclared()).forEach(x -> b.addTo(REST_rolesDeclared, x));
			b.addToIfNotEmpty(REST_roleGuard, string(a.roleGuard()));
			b.setIfNotEmpty(RESTOP_debug, string(a.debug()));
		}
	}
}