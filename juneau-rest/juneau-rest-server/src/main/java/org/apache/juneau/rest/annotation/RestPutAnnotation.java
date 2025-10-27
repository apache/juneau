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
package org.apache.juneau.rest.annotation;

import static org.apache.juneau.common.utils.CollectionUtils.*;

import java.lang.annotation.*;
import java.nio.charset.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.converter.*;
import org.apache.juneau.rest.guard.*;
import org.apache.juneau.rest.httppart.*;
import org.apache.juneau.rest.matcher.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link RestPut @RestPut} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestOpAnnotatedMethodBasics">@RestOp-Annotated Method Basics</a>
 * </ul>
 */
public class RestPutAnnotation {
	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	@SuppressWarnings("unchecked")
	public static class Builder extends TargetedAnnotationMBuilder<Builder> {

		Class<? extends RestConverter>[] converters = new Class[0];
		Class<? extends RestGuard>[] guards = new Class[0];
		Class<? extends RestMatcher>[] matchers = new Class[0];
		Class<? extends Encoder>[] encoders = new Class[0];
		Class<? extends Serializer>[] serializers = new Class[0];
		Class<?>[] parsers = {};
		OpSwagger swagger = OpSwaggerAnnotation.DEFAULT;
		String clientVersion = "", debug = "", defaultAccept = "", defaultCharset = "", defaultContentType = "", maxInput = "", rolesDeclared = "", roleGuard = "", summary = "", value = "";
		String[] consumes = {}, defaultRequestFormData = {}, defaultRequestQueryData = {}, defaultRequestAttributes = {}, defaultRequestHeaders = {}, defaultResponseHeaders = {}, path = {},
			produces = {};

		/**
		 * Constructor.
		 */
		protected Builder() {
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
		 * @return This object.
		 */
		public Builder clientVersion(String value) {
			this.clientVersion = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#consumes()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder consumes(String...value) {
			this.consumes = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#converters()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		@SafeVarargs
		public final Builder converters(Class<? extends RestConverter>...value) {
			this.converters = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#debug()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder debug(String value) {
			this.debug = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#defaultAccept()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultAccept(String value) {
			this.defaultAccept = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#defaultCharset()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultCharset(String value) {
			this.defaultCharset = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#defaultContentType()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultContentType(String value) {
			this.defaultContentType = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#defaultRequestAttributes()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultRequestAttributes(String...value) {
			this.defaultRequestAttributes = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#defaultRequestFormData()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultRequestFormData(String...value) {
			this.defaultRequestFormData = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#defaultRequestHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultRequestHeaders(String...value) {
			this.defaultRequestHeaders = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#defaultRequestQueryData()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultRequestQueryData(String...value) {
			this.defaultRequestQueryData = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#defaultResponseHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultResponseHeaders(String...value) {
			this.defaultResponseHeaders = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#encoders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		@SafeVarargs
		public final Builder encoders(Class<? extends Encoder>...value) {
			this.encoders = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#guards()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		@SafeVarargs
		public final Builder guards(Class<? extends RestGuard>...value) {
			this.guards = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#matchers()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		@SafeVarargs
		public final Builder matchers(Class<? extends RestMatcher>...value) {
			this.matchers = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#maxInput()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maxInput(String value) {
			this.maxInput = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#parsers()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder parsers(Class<?>...value) {
			this.parsers = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#path()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder path(String...value) {
			this.path = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#produces()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder produces(String...value) {
			this.produces = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#roleGuard()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder roleGuard(String value) {
			this.roleGuard = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#rolesDeclared()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder rolesDeclared(String value) {
			this.rolesDeclared = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#serializers()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		@SafeVarargs
		public final Builder serializers(Class<? extends Serializer>...value) {
			this.serializers = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#summary()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder summary(String value) {
			this.summary = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#swagger()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder swagger(OpSwagger value) {
			this.swagger = value;
			return this;
		}

		/**
		 * Sets the {@link RestPut#value()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder value(String value) {
			this.value = value;
			return this;
		}

	}

	/**
	 * Applies {@link RestPut} annotations to a {@link org.apache.juneau.rest.RestOpContext.Builder}.
	 */
	public static class RestOpContextApply extends AnnotationApplier<RestPut,RestOpContext.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public RestOpContextApply(VarResolverSession vr) {
			super(RestPut.class, RestOpContext.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<RestPut> ai, RestOpContext.Builder b) {
			RestPut a = ai.inner();

			b.httpMethod("put");

			classes(a.serializers()).ifPresent(x -> b.serializers().set(x));
			classes(a.parsers()).ifPresent(x -> b.parsers().set(x));
			classes(a.encoders()).ifPresent(x -> b.encoders().set(x));
			stream(a.produces()).map(MediaType::of).forEach(x -> b.produces(x));
			stream(a.consumes()).map(MediaType::of).forEach(x -> b.consumes(x));
			stream(a.defaultRequestHeaders()).map(HttpHeaders::stringHeader).forEach(x -> b.defaultRequestHeaders().setDefault(x));
			stream(a.defaultResponseHeaders()).map(HttpHeaders::stringHeader).forEach(x -> b.defaultResponseHeaders().setDefault(x));
			stream(a.defaultRequestAttributes()).map(BasicNamedAttribute::ofPair).forEach(x -> b.defaultRequestAttributes().add(x));
			stream(a.defaultRequestQueryData()).map(HttpParts::basicPart).forEach(x -> b.defaultRequestQueryData().setDefault(x));
			stream(a.defaultRequestFormData()).map(HttpParts::basicPart).forEach(x -> b.defaultRequestFormData().setDefault(x));
			string(a.defaultAccept()).map(HttpHeaders::accept).ifPresent(x -> b.defaultRequestHeaders().setDefault(x));
			string(a.defaultContentType()).map(HttpHeaders::contentType).ifPresent(x -> b.defaultRequestHeaders().setDefault(x));
			b.converters().append(a.converters());
			b.guards().append(a.guards());
			b.matchers().append(a.matchers());
			string(a.clientVersion()).ifPresent(x -> b.clientVersion(x));
			string(a.defaultCharset()).map(Charset::forName).ifPresent(x -> b.defaultCharset(x));
			string(a.maxInput()).ifPresent(x -> b.maxInput(x));
			stream(a.path()).forEach(x -> b.path(x));
			string(a.value()).ifPresent(x -> b.path(x));
			cdl(a.rolesDeclared()).forEach(x -> b.rolesDeclared(x));
			string(a.roleGuard()).ifPresent(x -> b.roleGuard(x));
			string(a.debug()).map(Enablement::fromString).ifPresent(x -> b.debug(x));
		}
	}

	private static class Impl extends TargetedAnnotationImpl implements RestPut {

		private final Class<? extends RestConverter>[] converters;
		private final Class<? extends RestGuard>[] guards;
		private final Class<? extends RestMatcher>[] matchers;
		private final Class<? extends Encoder>[] encoders;
		private final Class<? extends Serializer>[] serializers;
		private final Class<?>[] parsers;
		private final OpSwagger swagger;
		private final String clientVersion, debug, defaultAccept, defaultCharset, defaultContentType, maxInput, rolesDeclared, roleGuard, summary, value;
		private final String[] consumes, defaultRequestFormData, defaultRequestQueryData, defaultRequestAttributes, defaultRequestHeaders, defaultResponseHeaders, path, produces;

		Impl(Builder b) {
			super(b);
			this.clientVersion = b.clientVersion;
			this.consumes = copyOf(b.consumes);
			this.converters = copyOf(b.converters);
			this.debug = b.debug;
			this.defaultAccept = b.defaultAccept;
			this.defaultCharset = b.defaultCharset;
			this.defaultContentType = b.defaultContentType;
			this.defaultRequestFormData = copyOf(b.defaultRequestFormData);
			this.defaultRequestQueryData = copyOf(b.defaultRequestQueryData);
			this.defaultRequestAttributes = copyOf(b.defaultRequestAttributes);
			this.defaultRequestHeaders = copyOf(b.defaultRequestHeaders);
			this.defaultResponseHeaders = copyOf(b.defaultResponseHeaders);
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

		@Override /* Overridden from RestPut */
		public String clientVersion() {
			return clientVersion;
		}

		@Override /* Overridden from RestPut */
		public String[] consumes() {
			return consumes;
		}

		@Override /* Overridden from RestPut */
		public Class<? extends RestConverter>[] converters() {
			return converters;
		}

		@Override /* Overridden from RestPut */
		public String debug() {
			return debug;
		}

		@Override /* Overridden from RestPut */
		public String defaultAccept() {
			return defaultAccept;
		}

		@Override /* Overridden from RestPut */
		public String defaultCharset() {
			return defaultCharset;
		}

		@Override /* Overridden from RestPut */
		public String defaultContentType() {
			return defaultContentType;
		}

		@Override /* Overridden from RestPut */
		public String[] defaultRequestAttributes() {
			return defaultRequestAttributes;
		}

		@Override /* Overridden from RestPut */
		public String[] defaultRequestFormData() {
			return defaultRequestFormData;
		}

		@Override /* Overridden from RestPut */
		public String[] defaultRequestHeaders() {
			return defaultRequestHeaders;
		}

		@Override /* Overridden from RestPut */
		public String[] defaultRequestQueryData() {
			return defaultRequestQueryData;
		}

		@Override /* Overridden from RestPut */
		public String[] defaultResponseHeaders() {
			return defaultResponseHeaders;
		}

		@Override /* Overridden from RestPut */
		public Class<? extends Encoder>[] encoders() {
			return encoders;
		}

		@Override /* Overridden from RestPut */
		public Class<? extends RestGuard>[] guards() {
			return guards;
		}

		@Override /* Overridden from RestPut */
		public Class<? extends RestMatcher>[] matchers() {
			return matchers;
		}

		@Override /* Overridden from RestPut */
		public String maxInput() {
			return maxInput;
		}

		@Override /* Overridden from RestPut */
		public Class<?>[] parsers() {
			return parsers;
		}

		@Override /* Overridden from RestPut */
		public String[] path() {
			return path;
		}

		@Override /* Overridden from RestPut */
		public String[] produces() {
			return produces;
		}

		@Override /* Overridden from RestPut */
		public String roleGuard() {
			return roleGuard;
		}

		@Override /* Overridden from RestPut */
		public String rolesDeclared() {
			return rolesDeclared;
		}

		@Override /* Overridden from RestPut */
		public Class<? extends Serializer>[] serializers() {
			return serializers;
		}

		@Override /* Overridden from RestPut */
		public String summary() {
			return summary;
		}

		@Override /* Overridden from RestPut */
		public OpSwagger swagger() {
			return swagger;
		}

		@Override /* Overridden from RestPut */
		public String value() {
			return value;
		}
	}

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
}