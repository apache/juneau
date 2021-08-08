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
 * Utility classes and methods for the {@link RestPut @RestPut} annotation.
 */
public class RestPutAnnotation {


	/** Default value */
	public static final RestPut DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Builder class for the {@link RestPut} annotation.
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
			super(RestPut.class);
		}

		/**
		 * Instantiates a new {@link RestPut @RestPut} object initialized with this builder.
		 *
		 * @return A new {@link RestPut @RestPut} object.
		 */
		public RestPut build() {
			return new Impl(this);
		}

		/**
		 * Sets the {@link RestPut#clientVersion()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder clientVersion(String value) {
			this.clientVersion = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#consumes()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder consumes(String...value) {
			this.consumes = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#contextClass()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder contextClass(Class<? extends RestOperationContext> value) {
			this.contextClass = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#converters()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder converters(Class<? extends RestConverter>...value) {
			this.converters = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#debug()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder debug(String value) {
			this.debug = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#defaultAccept()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultAccept(String value) {
			this.defaultAccept = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#defaultCharset()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultCharset(String value) {
			this.defaultCharset = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#defaultContentType()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultContentType(String value) {
			this.defaultContentType = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#defaultFormData()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultFormData(String...value) {
			this.defaultFormData = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#defaultQuery()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultQuery(String...value) {
			this.defaultQuery = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#defaultRequestAttributes()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultRequestAttributes(String...value) {
			this.defaultRequestAttributes = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#defaultRequestHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultRequestHeaders(String...value) {
			this.defaultRequestHeaders = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#defaultResponseHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultResponseHeaders(String...value) {
			this.defaultResponseHeaders = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#description()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder description(String...value) {
			this.description = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#encoders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder encoders(Class<?>...value) {
			this.encoders = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#guards()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder guards(Class<? extends RestGuard>...value) {
			this.guards = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#matchers()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder matchers(Class<? extends RestMatcher>...value) {
			this.matchers = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#maxInput()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder maxInput(String value) {
			this.maxInput = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#parsers()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder parsers(Class<?>...value) {
			this.parsers = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#path()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder path(String...value) {
			this.path = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#produces()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder produces(String...value) {
			this.produces = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#roleGuard()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder roleGuard(String value) {
			this.roleGuard = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#rolesDeclared()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder rolesDeclared(String value) {
			this.rolesDeclared = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#serializers()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder serializers(Class<?>...value) {
			this.serializers = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#summary()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder summary(String value) {
			this.summary = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#swagger()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder swagger(OpSwagger value) {
			this.swagger = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#value()} property on this annotation.
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

	private static class Impl extends TargetedAnnotationImpl implements RestPut {

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

		@Override /* RestPut */
		public String clientVersion() {
			return clientVersion;
		}

		@Override /* RestPut */
		public String[] consumes() {
			return consumes;
		}

		@Override /* RestPut */
		public Class<? extends RestOperationContext> contextClass() {
			return contextClass;
		}

		@Override /* RestPut */
		public Class<? extends RestConverter>[] converters() {
			return converters;
		}

		@Override /* RestPut */
		public String debug() {
			return debug;
		}

		@Override /* RestPut */
		public String defaultAccept() {
			return defaultAccept;
		}

		@Override /* RestPut */
		public String defaultCharset() {
			return defaultCharset;
		}

		@Override /* RestPut */
		public String defaultContentType() {
			return defaultContentType;
		}

		@Override /* RestPut */
		public String[] defaultFormData() {
			return defaultFormData;
		}

		@Override /* RestPut */
		public String[] defaultQuery() {
			return defaultQuery;
		}

		@Override /* RestPut */
		public String[] defaultRequestAttributes() {
			return defaultRequestAttributes;
		}

		@Override /* RestPut */
		public String[] defaultRequestHeaders() {
			return defaultRequestHeaders;
		}

		@Override /* RestPut */
		public String[] defaultResponseHeaders() {
			return defaultResponseHeaders;
		}

		@Override /* RestPut */
		public String[] description() {
			return description;
		}

		@Override /* RestPut */
		public Class<?>[] encoders() {
			return encoders;
		}

		@Override /* RestPut */
		public Class<? extends RestGuard>[] guards() {
			return guards;
		}

		@Override /* RestPut */
		public Class<? extends RestMatcher>[] matchers() {
			return matchers;
		}

		@Override /* RestPut */
		public String maxInput() {
			return maxInput;
		}

		@Override /* RestPut */
		public Class<?>[] parsers() {
			return parsers;
		}

		@Override /* RestPut */
		public String[] path() {
			return path;
		}

		@Override /* RestPut */
		public String[] produces() {
			return produces;
		}

		@Override /* RestPut */
		public String roleGuard() {
			return roleGuard;
		}

		@Override /* RestPut */
		public String rolesDeclared() {
			return rolesDeclared;
		}

		@Override /* RestPut */
		public Class<?>[] serializers() {
			return serializers;
		}

		@Override /* RestPut */
		public String summary() {
			return summary;
		}

		@Override /* RestPut */
		public OpSwagger swagger() {
			return swagger;
		}

		@Override /* RestPut */
		public String value() {
			return value;
		}
	}

	/**
	 * Applies {@link RestPut} annotations to a {@link RestOperationContextBuilder}.
	 */
	public static class Apply extends AnnotationApplier<RestPut,RestOperationContextBuilder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Apply(VarResolverSession vr) {
			super(RestPut.class, RestOperationContextBuilder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<RestPut> ai, RestOperationContextBuilder b) {
			RestPut a = ai.getAnnotation();

			b.setIfNotEmpty(RESTOP_httpMethod, "put");

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