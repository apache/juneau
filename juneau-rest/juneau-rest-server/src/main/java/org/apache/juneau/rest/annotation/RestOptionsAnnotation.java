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
import static org.apache.juneau.http.HttpParts.*;

import java.lang.annotation.*;
import java.nio.charset.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.converter.*;
import org.apache.juneau.rest.guard.*;
import org.apache.juneau.rest.httppart.*;
import org.apache.juneau.rest.matcher.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link RestOptions @RestOptions} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.RestOpAnnotatedMethods">@RestOp-Annotated Methods</a>
 * </ul>
 */
public class RestOptionsAnnotation {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/** Default value */
	public static final RestOptions DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static Builder create() {
		return new Builder();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	@SuppressWarnings("unchecked")
	public static class Builder extends TargetedAnnotationMBuilder {

		Class<? extends RestConverter>[] converters = new Class[0];
		Class<? extends RestGuard>[] guards = new Class[0];
		Class<? extends RestMatcher>[] matchers = new Class[0];
		Class<? extends Encoder>[] encoders = new Class[0];
		Class<? extends Serializer>[] serializers = new Class[0];
		OpSwagger swagger = OpSwaggerAnnotation.DEFAULT;
		String clientVersion="", debug="", defaultAccept="", defaultCharset="", rolesDeclared="", roleGuard="", summary="", value="";
		String[] defaultRequestQueryData={}, defaultRequestAttributes={}, defaultRequestHeaders={}, defaultResponseHeaders={}, description={}, path={}, produces={};

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(RestOptions.class);
		}

		/**
		 * Instantiates a new {@link RestOptions @RestOptions} object initialized with this builder.
		 *
		 * @return A new {@link RestOptions @RestOptions} object.
		 */
		public RestOptions build() {
			return new Impl(this);
		}

		/**
		 * Sets the {@link RestOptions#clientVersion()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder clientVersion(String value) {
			this.clientVersion = value;
			return this;
		}

		/**
		 * Sets the {@link RestOptions#converters()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder converters(Class<? extends RestConverter>...value) {
			this.converters = value;
			return this;
		}

		/**
		 * Sets the {@link RestOptions#debug()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder debug(String value) {
			this.debug = value;
			return this;
		}

		/**
		 * Sets the {@link RestOptions#defaultAccept()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultAccept(String value) {
			this.defaultAccept = value;
			return this;
		}

		/**
		 * Sets the {@link RestOptions#defaultCharset()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultCharset(String value) {
			this.defaultCharset = value;
			return this;
		}

		/**
		 * Sets the {@link RestOptions#defaultRequestQueryData()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultRequestQueryData(String...value) {
			this.defaultRequestQueryData = value;
			return this;
		}

		/**
		 * Sets the {@link RestOptions#defaultRequestAttributes()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultRequestAttributes(String...value) {
			this.defaultRequestAttributes = value;
			return this;
		}

		/**
		 * Sets the {@link RestOptions#defaultRequestHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultRequestHeaders(String...value) {
			this.defaultRequestHeaders = value;
			return this;
		}

		/**
		 * Sets the {@link RestOptions#defaultResponseHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultResponseHeaders(String...value) {
			this.defaultResponseHeaders = value;
			return this;
		}

		/**
		 * Sets the {@link RestOptions#description()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder description(String...value) {
			this.description = value;
			return this;
		}

		/**
		 * Sets the {@link RestOptions#encoders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder encoders(Class<? extends Encoder>...value) {
			this.encoders = value;
			return this;
		}

		/**
		 * Sets the {@link RestOptions#guards()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder guards(Class<? extends RestGuard>...value) {
			this.guards = value;
			return this;
		}

		/**
		 * Sets the {@link RestOptions#matchers()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder matchers(Class<? extends RestMatcher>...value) {
			this.matchers = value;
			return this;
		}

		/**
		 * Sets the {@link RestOptions#path()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder path(String...value) {
			this.path = value;
			return this;
		}

		/**
		 * Sets the {@link RestOptions#produces()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder produces(String...value) {
			this.produces = value;
			return this;
		}

		/**
		 * Sets the {@link RestOptions#roleGuard()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder roleGuard(String value) {
			this.roleGuard = value;
			return this;
		}

		/**
		 * Sets the {@link RestOptions#rolesDeclared()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder rolesDeclared(String value) {
			this.rolesDeclared = value;
			return this;
		}

		/**
		 * Sets the {@link RestOptions#serializers()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder serializers(Class<? extends Serializer>...value) {
			this.serializers = value;
			return this;
		}

		/**
		 * Sets the {@link RestOptions#summary()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder summary(String value) {
			this.summary = value;
			return this;
		}

		/**
		 * Sets the {@link RestOptions#swagger()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder swagger(OpSwagger value) {
			this.swagger = value;
			return this;
		}

		/**
		 * Sets the {@link RestOptions#value()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
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

	//-----------------------------------------------------------------------------------------------------------------
	// Implementation
	//-----------------------------------------------------------------------------------------------------------------

	private static class Impl extends TargetedAnnotationImpl implements RestOptions {

		private final Class<? extends RestConverter>[] converters;
		private final Class<? extends RestGuard>[] guards;
		private final Class<? extends RestMatcher>[] matchers;
		private final Class<? extends Encoder>[] encoders;
		private final Class<? extends Serializer>[] serializers;
		private final OpSwagger swagger;
		private final String clientVersion, debug, defaultAccept, defaultCharset, rolesDeclared, roleGuard, summary, value;
		private final String[] defaultRequestQueryData, defaultRequestAttributes, defaultRequestHeaders, defaultResponseHeaders, description, path, produces;

		Impl(Builder b) {
			super(b);
			this.clientVersion = b.clientVersion;
			this.converters = copyOf(b.converters);
			this.debug = b.debug;
			this.defaultAccept = b.defaultAccept;
			this.defaultCharset = b.defaultCharset;
			this.defaultRequestQueryData = copyOf(b.defaultRequestQueryData);
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

		@Override /* RestOptions */
		public String clientVersion() {
			return clientVersion;
		}

		@Override /* RestOptions */
		public Class<? extends RestConverter>[] converters() {
			return converters;
		}

		@Override /* RestOptions */
		public String debug() {
			return debug;
		}

		@Override /* RestOptions */
		public String defaultAccept() {
			return defaultAccept;
		}

		@Override /* RestOptions */
		public String defaultCharset() {
			return defaultCharset;
		}

		@Override /* RestOptions */
		public String[] defaultRequestQueryData() {
			return defaultRequestQueryData;
		}

		@Override /* RestOptions */
		public String[] defaultRequestAttributes() {
			return defaultRequestAttributes;
		}

		@Override /* RestOptions */
		public String[] defaultRequestHeaders() {
			return defaultRequestHeaders;
		}

		@Override /* RestOptions */
		public String[] defaultResponseHeaders() {
			return defaultResponseHeaders;
		}

		@Override /* RestOptions */
		public String[] description() {
			return description;
		}

		@Override /* RestOptions */
		public Class<? extends Encoder>[] encoders() {
			return encoders;
		}

		@Override /* RestOptions */
		public Class<? extends RestGuard>[] guards() {
			return guards;
		}

		@Override /* RestOptions */
		public Class<? extends RestMatcher>[] matchers() {
			return matchers;
		}

		@Override /* RestOptions */
		public String[] path() {
			return path;
		}

		@Override /* RestOptions */
		public String[] produces() {
			return produces;
		}

		@Override /* RestOptions */
		public String roleGuard() {
			return roleGuard;
		}

		@Override /* RestOptions */
		public String rolesDeclared() {
			return rolesDeclared;
		}

		@Override /* RestOptions */
		public Class<? extends Serializer>[] serializers() {
			return serializers;
		}

		@Override /* RestOptions */
		public String summary() {
			return summary;
		}

		@Override /* RestOptions */
		public OpSwagger swagger() {
			return swagger;
		}

		@Override /* RestOptions */
		public String value() {
			return value;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Appliers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Applies {@link RestOptions} annotations to a {@link org.apache.juneau.rest.RestOpContext.Builder}.
	 */
	public static class RestOpContextApply extends AnnotationApplier<RestOptions,RestOpContext.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public RestOpContextApply(VarResolverSession vr) {
			super(RestOptions.class, RestOpContext.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<RestOptions> ai, RestOpContext.Builder b) {
			RestOptions a = ai.inner();

			b.httpMethod("options");

			classes(a.serializers()).ifPresent(x -> b.serializers().set(x));
			classes(a.encoders()).ifPresent(x -> b.encoders().set(x));
			stream(a.produces()).map(MediaType::of).forEach(x -> b.produces(x));
			stream(a.defaultRequestHeaders()).map(x -> stringHeader(x)).forEach(x -> b.defaultRequestHeaders().setDefault(x));
			stream(a.defaultResponseHeaders()).map(x -> stringHeader(x)).forEach(x -> b.defaultResponseHeaders().setDefault(x));
			stream(a.defaultRequestAttributes()).map(x -> BasicNamedAttribute.ofPair(x)).forEach(x -> b.defaultRequestAttributes().add(x));
			stream(a.defaultRequestQueryData()).map(x -> basicPart(x)).forEach(x -> b.defaultRequestQueryData().setDefault(x));
			string(a.defaultAccept()).map(x -> accept(x)).ifPresent(x -> b.defaultRequestHeaders().setDefault(x));
			b.converters().append(a.converters());
			b.guards().append(a.guards());
			b.matchers().append(a.matchers());
			string(a.clientVersion()).ifPresent(x -> b.clientVersion(x));
			string(a.defaultCharset()).map(Charset::forName).ifPresent(x -> b.defaultCharset(x));
			stream(a.path()).forEach(x -> b.path(x));
			string(a.value()).ifPresent(x -> b.path(x));
			cdl(a.rolesDeclared()).forEach(x -> b.rolesDeclared(x));
			string(a.roleGuard()).ifPresent(x -> b.roleGuard(x));
			string(a.debug()).map(Enablement::fromString).ifPresent(x -> b.debug(x));
		}
	}
}