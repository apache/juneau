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

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link RestGet @RestGet} annotation.
 */
public class RestGetAnnotation {


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

	/**
	 * Builder class for the {@link RestGet} annotation.
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
		Class<?>[] encoders=new Class<?>[0], serializers=new Class<?>[0];
		OpSwagger swagger = OpSwaggerAnnotation.DEFAULT;
		String clientVersion="", debug="", defaultAccept="", defaultCharset="", rolesDeclared="", roleGuard="", summary="", value="";
		String[] defaultQuery={}, defaultRequestAttributes={}, defaultRequestHeaders={}, defaultResponseHeaders={}, description={}, path={}, produces={};

		/**
		 * Constructor.
		 */
		public Builder() {
			super(RestGet.class);
		}

		/**
		 * Instantiates a new {@link RestGet @RestGet} object initialized with this builder.
		 *
		 * @return A new {@link RestGet @RestGet} object.
		 */
		public RestGet build() {
			return new Impl(this);
		}

		/**
		 * Sets the {@link RestGet#clientVersion()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder clientVersion(String value) {
			this.clientVersion = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#contextClass()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder contextClass(Class<? extends RestOperationContext> value) {
			this.contextClass = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#converters()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder converters(Class<? extends RestConverter>...value) {
			this.converters = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#debug()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder debug(String value) {
			this.debug = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#defaultAccept()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultAccept(String value) {
			this.defaultAccept = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#defaultCharset()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultCharset(String value) {
			this.defaultCharset = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#defaultQuery()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultQuery(String...value) {
			this.defaultQuery = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#defaultRequestAttributes()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultRequestAttributes(String...value) {
			this.defaultRequestAttributes = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#defaultRequestHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultRequestHeaders(String...value) {
			this.defaultRequestHeaders = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#defaultResponseHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder defaultResponseHeaders(String...value) {
			this.defaultResponseHeaders = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#description()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder description(String...value) {
			this.description = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#encoders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder encoders(Class<?>...value) {
			this.encoders = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#guards()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder guards(Class<? extends RestGuard>...value) {
			this.guards = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#matchers()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder matchers(Class<? extends RestMatcher>...value) {
			this.matchers = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#path()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder path(String...value) {
			this.path = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#produces()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder produces(String...value) {
			this.produces = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#roleGuard()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder roleGuard(String value) {
			this.roleGuard = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#rolesDeclared()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder rolesDeclared(String value) {
			this.rolesDeclared = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#serializers()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder serializers(Class<?>...value) {
			this.serializers = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#summary()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder summary(String value) {
			this.summary = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#swagger()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder swagger(OpSwagger value) {
			this.swagger = value;
			return this;
		}

		/**
		 * Sets the {@link RestGet#value()} property on this annotation.
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

	private static class Impl extends TargetedAnnotationImpl implements RestGet {

		private final Class<? extends RestConverter>[] converters;
		private final Class<? extends RestGuard>[] guards;
		private final Class<? extends RestMatcher>[] matchers;
		private final Class<? extends RestOperationContext> contextClass;
		private final Class<?>[] encoders, serializers;
		private final OpSwagger swagger;
		private final String clientVersion, debug, defaultAccept, defaultCharset, rolesDeclared, roleGuard, summary, value;
		private final String[] defaultQuery, defaultRequestAttributes, defaultRequestHeaders, defaultResponseHeaders, description, path, produces;

		Impl(Builder b) {
			super(b);
			this.clientVersion = b.clientVersion;
			this.contextClass = b.contextClass;
			this.converters = copyOf(b.converters);
			this.debug = b.debug;
			this.defaultAccept = b.defaultAccept;
			this.defaultCharset = b.defaultCharset;
			this.defaultQuery = copyOf(b.defaultQuery);
			this.defaultRequestAttributes = copyOf(b.defaultRequestAttributes);
			this.defaultRequestHeaders = copyOf(b.defaultRequestHeaders);
			this.defaultResponseHeaders = copyOf(b.defaultResponseHeaders);
			this.description = copyOf(b.description);
			this.encoders = copyOf(b.encoders);
			this.guards = copyOf(b.guards);
			this.matchers = copyOf(b.matchers);
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

		@Override /* RestGet */
		public String clientVersion() {
			return clientVersion;
		}

		@Override /* RestGet */
		public Class<? extends RestOperationContext> contextClass() {
			return contextClass;
		}

		@Override /* RestGet */
		public Class<? extends RestConverter>[] converters() {
			return converters;
		}

		@Override /* RestGet */
		public String debug() {
			return debug;
		}

		@Override /* RestGet */
		public String defaultAccept() {
			return defaultAccept;
		}

		@Override /* RestGet */
		public String defaultCharset() {
			return defaultCharset;
		}

		@Override /* RestGet */
		public String[] defaultQuery() {
			return defaultQuery;
		}

		@Override /* RestGet */
		public String[] defaultRequestAttributes() {
			return defaultRequestAttributes;
		}

		@Override /* RestGet */
		public String[] defaultRequestHeaders() {
			return defaultRequestHeaders;
		}

		@Override /* RestGet */
		public String[] defaultResponseHeaders() {
			return defaultResponseHeaders;
		}

		@Override /* RestGet */
		public String[] description() {
			return description;
		}

		@Override /* RestGet */
		public Class<?>[] encoders() {
			return encoders;
		}

		@Override /* RestGet */
		public Class<? extends RestGuard>[] guards() {
			return guards;
		}

		@Override /* RestGet */
		public Class<? extends RestMatcher>[] matchers() {
			return matchers;
		}

		@Override /* RestGet */
		public String[] path() {
			return path;
		}

		@Override /* RestGet */
		public String[] produces() {
			return produces;
		}

		@Override /* RestGet */
		public String roleGuard() {
			return roleGuard;
		}

		@Override /* RestGet */
		public String rolesDeclared() {
			return rolesDeclared;
		}

		@Override /* RestGet */
		public Class<?>[] serializers() {
			return serializers;
		}

		@Override /* RestGet */
		public String summary() {
			return summary;
		}

		@Override /* RestGet */
		public OpSwagger swagger() {
			return swagger;
		}

		@Override /* RestGet */
		public String value() {
			return value;
		}
	}

	/**
	 * Applies {@link RestGet} annotations to a {@link ContextPropertiesBuilder}.
	 */
	public static class Apply extends ConfigApply<RestGet> {

		/**
		 * Constructor.
		 *
		 * @param c The annotation class.
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Apply(Class<RestGet> c, VarResolverSession vr) {
			super(c, vr);
		}

		@Override
		public void apply(AnnotationInfo<RestGet> ai, ContextPropertiesBuilder cpb, VarResolverSession vr) {
			RestGet a = ai.getAnnotation();

			cpb.setIfNotEmpty(RESTOP_httpMethod, "get");

			cpb.set(REST_serializers, merge(ConverterUtils.toType(cpb.peek(REST_serializers), Object[].class), a.serializers()));
			cpb.set(REST_encoders, merge(ConverterUtils.toType(cpb.peek(REST_encoders), Object[].class), a.encoders()));
			cpb.setIf(a.contextClass() != RestOperationContext.Null.class, RESTOP_contextClass, a.contextClass());
			cpb.setIfNotEmpty(REST_produces, stringList(a.produces()));
			stringStream(a.defaultRequestHeaders()).map(x -> basicHeader(x)).forEach(x -> cpb.appendTo(RESTOP_defaultRequestHeaders, x));
			stringStream(a.defaultResponseHeaders()).map(x -> basicHeader(x)).forEach(x -> cpb.appendTo(RESTOP_defaultResponseHeaders, x));
			stringStream(a.defaultRequestAttributes()).map(x -> BasicNamedAttribute.ofPair(x)).forEach(x -> cpb.appendTo(RESTOP_defaultRequestAttributes, x));
			stringStream(a.defaultQuery()).map(x -> BasicPart.ofPair(x)).forEach(x -> cpb.appendTo(RESTOP_defaultQuery, x));
			cpb.appendToIfNotEmpty(REST_defaultRequestHeaders, accept(string(a.defaultAccept())));
			cpb.prependTo(REST_converters, a.converters());
			cpb.prependTo(REST_guards, reverse(a.guards()));
			cpb.prependTo(RESTOP_matchers, a.matchers());
			cpb.setIfNotEmpty(RESTOP_clientVersion, a.clientVersion());
			cpb.setIfNotEmpty(REST_defaultCharset, string(a.defaultCharset()));
			stringStream(a.path()).forEach(x -> cpb.prependTo(RESTOP_path, x));
			cpb.setIfNotEmpty(RESTOP_path, a.value());
			cdStream(a.rolesDeclared()).forEach(x -> cpb.addTo(REST_rolesDeclared, x));
			cpb.addToIfNotEmpty(REST_roleGuard, string(a.roleGuard()));
			cpb.setIfNotEmpty(RESTOP_debug, string(a.debug()));
		}
	}
}