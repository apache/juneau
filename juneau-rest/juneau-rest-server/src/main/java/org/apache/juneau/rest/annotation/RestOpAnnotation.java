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
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.common.internal.*;
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
 * Utility classes and methods for the {@link RestOp @RestOp} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.RestOpAnnotatedMethods">@RestOp-Annotated Methods</a>
 * </ul>
 */
public class RestOpAnnotation {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/** Default value */
	public static final RestOp DEFAULT = create().build();

	/**
	 * Predicate that can be used with the {@link ClassInfo#getAnnotationList(Predicate)} and {@link MethodInfo#getAnnotationList(Predicate)}
	 */
	public static final Predicate<AnnotationInfo<?>> REST_OP_GROUP = x -> x.isInGroup(RestOp.class);

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
		Class<?>[] parsers=new Class<?>[0];
		OpSwagger swagger = OpSwaggerAnnotation.DEFAULT;
		String clientVersion="", debug="", defaultAccept="", defaultCharset="", defaultContentType="", maxInput="", method="", rolesDeclared="", roleGuard="", summary="", value="";
		String[] consumes={}, defaultRequestFormData={}, defaultRequestQueryData={}, defaultRequestAttributes={}, defaultRequestHeaders={}, defaultResponseHeaders={}, description={}, path={}, produces={};

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(RestOp.class);
		}

		/**
		 * Instantiates a new {@link RestOp @RestOp} object initialized with this builder.
		 *
		 * @return A new {@link RestOp @RestOp} object.
		 */
		public RestOp build() {
			return new Impl(this);
		}

		/**
		 * Sets the {@link RestOp#clientVersion()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder clientVersion(String value) {
			this.clientVersion = value;
			return this;
		}

		/**
		 * Sets the {@link RestOp#consumes()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder consumes(String...value) {
			this.consumes = value;
			return this;
		}

		/**
		 * Sets the {@link RestOp#converters()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder converters(Class<? extends RestConverter>...value) {
			this.converters = value;
			return this;
		}

		/**
		 * Sets the {@link RestOp#debug()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder debug(String value) {
			this.debug = value;
			return this;
		}

		/**
		 * Sets the {@link RestOp#defaultAccept()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultAccept(String value) {
			this.defaultAccept = value;
			return this;
		}

		/**
		 * Sets the {@link RestOp#defaultCharset()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultCharset(String value) {
			this.defaultCharset = value;
			return this;
		}

		/**
		 * Sets the {@link RestOp#defaultContentType()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultContentType(String value) {
			this.defaultContentType = value;
			return this;
		}

		/**
		 * Sets the {@link RestOp#defaultRequestFormData()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultRequestFormData(String...value) {
			this.defaultRequestFormData = value;
			return this;
		}

		/**
		 * Sets the {@link RestOp#defaultRequestQueryData()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultRequestQueryData(String...value) {
			this.defaultRequestQueryData = value;
			return this;
		}

		/**
		 * Sets the {@link RestOp#defaultRequestAttributes()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultRequestAttributes(String...value) {
			this.defaultRequestAttributes = value;
			return this;
		}

		/**
		 * Sets the {@link RestOp#defaultRequestHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultRequestHeaders(String...value) {
			this.defaultRequestHeaders = value;
			return this;
		}

		/**
		 * Sets the {@link RestOp#defaultResponseHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultResponseHeaders(String...value) {
			this.defaultResponseHeaders = value;
			return this;
		}

		/**
		 * Sets the {@link RestOp#description()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder description(String...value) {
			this.description = value;
			return this;
		}

		/**
		 * Sets the {@link RestOp#encoders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder encoders(Class<? extends Encoder>...value) {
			this.encoders = value;
			return this;
		}

		/**
		 * Sets the {@link RestOp#guards()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder guards(Class<? extends RestGuard>...value) {
			this.guards = value;
			return this;
		}

		/**
		 * Sets the {@link RestOp#matchers()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder matchers(Class<? extends RestMatcher>...value) {
			this.matchers = value;
			return this;
		}

		/**
		 * Sets the {@link RestOp#maxInput()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maxInput(String value) {
			this.maxInput = value;
			return this;
		}

		/**
		 * Sets the {@link RestOp#method()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder method(String value) {
			this.method = value;
			return this;
		}

		/**
		 * Sets the {@link RestOp#parsers()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder parsers(Class<?>...value) {
			this.parsers = value;
			return this;
		}

		/**
		 * Sets the {@link RestOp#path()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder path(String...value) {
			this.path = value;
			return this;
		}

		/**
		 * Sets the {@link RestOp#produces()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder produces(String...value) {
			this.produces = value;
			return this;
		}

		/**
		 * Sets the {@link RestOp#roleGuard()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder roleGuard(String value) {
			this.roleGuard = value;
			return this;
		}

		/**
		 * Sets the {@link RestOp#rolesDeclared()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder rolesDeclared(String value) {
			this.rolesDeclared = value;
			return this;
		}

		/**
		 * Sets the {@link RestOp#serializers()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder serializers(Class<? extends Serializer>...value) {
			this.serializers = value;
			return this;
		}

		/**
		 * Sets the {@link RestOp#summary()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder summary(String value) {
			this.summary = value;
			return this;
		}

		/**
		 * Sets the {@link RestOp#swagger()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder swagger(OpSwagger value) {
			this.swagger = value;
			return this;
		}

		/**
		 * Sets the {@link RestOp#value()} property on this annotation.
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

	private static class Impl extends TargetedAnnotationImpl implements RestOp {

		private final Class<? extends RestConverter>[] converters;
		private final Class<? extends RestGuard>[] guards;
		private final Class<? extends RestMatcher>[] matchers;
		private final Class<? extends Encoder>[] encoders;
		private final Class<? extends Serializer>[] serializers;
		private final Class<?>[] parsers;
		private final OpSwagger swagger;
		private final String clientVersion, debug, defaultAccept, defaultCharset, defaultContentType, maxInput, method, rolesDeclared, roleGuard, summary, value;
		private final String[] consumes, defaultRequestFormData, defaultRequestQueryData, defaultRequestAttributes, defaultRequestHeaders, defaultResponseHeaders, description, path, produces;

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
			this.description = copyOf(b.description);
			this.encoders = copyOf(b.encoders);
			this.guards = copyOf(b.guards);
			this.matchers = copyOf(b.matchers);
			this.maxInput = b.maxInput;
			this.method = b.method;
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

		@Override /* RestOp */
		public String clientVersion() {
			return clientVersion;
		}

		@Override /* RestOp */
		public String[] consumes() {
			return consumes;
		}

		@Override /* RestOp */
		public Class<? extends RestConverter>[] converters() {
			return converters;
		}

		@Override /* RestOp */
		public String debug() {
			return debug;
		}

		@Override /* RestOp */
		public String defaultAccept() {
			return defaultAccept;
		}

		@Override /* RestOp */
		public String defaultCharset() {
			return defaultCharset;
		}

		@Override /* RestOp */
		public String defaultContentType() {
			return defaultContentType;
		}

		@Override /* RestOp */
		public String[] defaultRequestFormData() {
			return defaultRequestFormData;
		}

		@Override /* RestOp */
		public String[] defaultRequestQueryData() {
			return defaultRequestQueryData;
		}

		@Override /* RestOp */
		public String[] defaultRequestAttributes() {
			return defaultRequestAttributes;
		}

		@Override /* RestOp */
		public String[] defaultRequestHeaders() {
			return defaultRequestHeaders;
		}

		@Override /* RestOp */
		public String[] defaultResponseHeaders() {
			return defaultResponseHeaders;
		}

		@Override /* RestOp */
		public String[] description() {
			return description;
		}

		@Override /* RestOp */
		public Class<? extends Encoder>[] encoders() {
			return encoders;
		}

		@Override /* RestOp */
		public Class<? extends RestGuard>[] guards() {
			return guards;
		}

		@Override /* RestOp */
		public Class<? extends RestMatcher>[] matchers() {
			return matchers;
		}

		@Override /* RestOp */
		public String maxInput() {
			return maxInput;
		}

		@Override /* RestOp */
		public String method() {
			return method;
		}

		@Override /* RestOp */
		public Class<?>[] parsers() {
			return parsers;
		}

		@Override /* RestOp */
		public String[] path() {
			return path;
		}

		@Override /* RestOp */
		public String[] produces() {
			return produces;
		}

		@Override /* RestOp */
		public String roleGuard() {
			return roleGuard;
		}

		@Override /* RestOp */
		public String rolesDeclared() {
			return rolesDeclared;
		}

		@Override /* RestOp */
		public Class<? extends Serializer>[] serializers() {
			return serializers;
		}

		@Override /* RestOp */
		public String summary() {
			return summary;
		}

		@Override /* RestOp */
		public OpSwagger swagger() {
			return swagger;
		}

		@Override /* RestOp */
		public String value() {
			return value;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Appliers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Applies {@link RestOp} annotations to a {@link org.apache.juneau.rest.RestOpContext.Builder}.
	 */
	public static class RestOpContextApply extends AnnotationApplier<RestOp,RestOpContext.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public RestOpContextApply(VarResolverSession vr) {
			super(RestOp.class, RestOpContext.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<RestOp> ai, RestOpContext.Builder b) {
			RestOp a = ai.inner();

			classes(a.serializers()).ifPresent(x -> b.serializers().set(x));
			classes(a.parsers()).ifPresent(x -> b.parsers().set(x));
			classes(a.encoders()).ifPresent(x -> b.encoders().set(x));
			stream(a.produces()).map(MediaType::of).forEach(x -> b.produces(x));
			stream(a.consumes()).map(MediaType::of).forEach(x -> b.consumes(x));
			stream(a.defaultRequestHeaders()).map(x -> stringHeader(x)).forEach(x -> b.defaultRequestHeaders().setDefault(x));
			stream(a.defaultResponseHeaders()).map(x -> stringHeader(x)).forEach(x -> b.defaultResponseHeaders().setDefault(x));
			stream(a.defaultRequestAttributes()).map(x -> BasicNamedAttribute.ofPair(x)).forEach(x -> b.defaultRequestAttributes().add(x));
			stream(a.defaultRequestQueryData()).map(x -> basicPart(x)).forEach(x -> b.defaultRequestQueryData().setDefault(x));
			stream(a.defaultRequestFormData()).map(x -> basicPart(x)).forEach(x -> b.defaultRequestFormData().setDefault(x));
			string(a.defaultAccept()).map(x -> accept(x)).ifPresent(x -> b.defaultRequestHeaders().setDefault(x));
			string(a.defaultContentType()).map(x -> contentType(x)).ifPresent(x -> b.defaultRequestHeaders().setDefault(x));
			b.converters().append(a.converters());
			b.guards().append(a.guards());
			b.matchers().append(a.matchers());
			string(a.clientVersion()).ifPresent(x -> b.clientVersion(x));
			string(a.defaultCharset()).map(Charset::forName).ifPresent(x -> b.defaultCharset(x));
			string(a.maxInput()).ifPresent(x -> b.maxInput(x));
			stream(a.path()).forEach(x -> b.path(x));
			cdl(a.rolesDeclared()).forEach(x -> b.rolesDeclared(x));
			string(a.roleGuard()).ifPresent(x -> b.roleGuard(x));

			string(a.method()).ifPresent(x -> b.httpMethod(x));
			string(a.debug()).map(Enablement::fromString).ifPresent(x -> b.debug(x));

			String v = StringUtils.trim(string(a.value()).orElse(null));
			if (v != null) {
				int i = v.indexOf(' ');
				if (i == -1) {
					b.httpMethod(v);
				} else {
					b.httpMethod(v.substring(0, i).trim());
					b.path(v.substring(i).trim());
				}
			}
		}
	}
}