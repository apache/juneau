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
import org.apache.juneau.rest.guard.*;
import org.apache.juneau.rest.httppart.*;
import org.apache.juneau.rest.matcher.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link RestDelete @RestDelete} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.RestOpAnnotatedMethods">@RestOp-Annotated Methods</a>
 * </ul>
 */
public class RestDeleteAnnotation {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/** Default value */
	public static final RestDelete DEFAULT = create().build();

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

		Class<? extends RestGuard>[] guards = new Class[0];
		Class<? extends RestMatcher>[] matchers = new Class[0];
		Class<? extends Encoder>[] encoders = new Class[0];
		OpSwagger swagger = OpSwaggerAnnotation.DEFAULT;
		String clientVersion="", debug="", defaultAccept="", defaultCharset="", rolesDeclared="", roleGuard="", summary="", value="";
		String[] defaultRequestQueryData={}, defaultRequestAttributes={}, defaultRequestHeaders={}, defaultResponseHeaders={}, description={}, path={};

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(RestDelete.class);
		}

		/**
		 * Instantiates a new {@link RestDelete @RestDelete} object initialized with this builder.
		 *
		 * @return A new {@link RestDelete @RestDelete} object.
		 */
		public RestDelete build() {
			return new Impl(this);
		}

		/**
		 * Sets the {@link RestDelete#clientVersion()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder clientVersion(String value) {
			this.clientVersion = value;
			return this;
		}

		/**
		 * Sets the {@link RestDelete#debug()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder debug(String value) {
			this.debug = value;
			return this;
		}

		/**
		 * Sets the {@link RestDelete#defaultAccept()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultAccept(String value) {
			this.defaultAccept = value;
			return this;
		}

		/**
		 * Sets the {@link RestDelete#defaultCharset()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultCharset(String value) {
			this.defaultCharset = value;
			return this;
		}

		/**
		 * Sets the {@link RestDelete#defaultRequestQueryData()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultRequestQueryData(String...value) {
			this.defaultRequestQueryData = value;
			return this;
		}

		/**
		 * Sets the {@link RestDelete#defaultRequestAttributes()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultRequestAttributes(String...value) {
			this.defaultRequestAttributes = value;
			return this;
		}

		/**
		 * Sets the {@link RestDelete#defaultRequestHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultRequestHeaders(String...value) {
			this.defaultRequestHeaders = value;
			return this;
		}

		/**
		 * Sets the {@link RestDelete#defaultResponseHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder defaultResponseHeaders(String...value) {
			this.defaultResponseHeaders = value;
			return this;
		}

		/**
		 * Sets the {@link RestDelete#description()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder description(String...value) {
			this.description = value;
			return this;
		}

		/**
		 * Sets the {@link RestDelete#encoders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder encoders(Class<? extends Encoder>...value) {
			this.encoders = value;
			return this;
		}

		/**
		 * Sets the {@link RestDelete#guards()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder guards(Class<? extends RestGuard>...value) {
			this.guards = value;
			return this;
		}

		/**
		 * Sets the {@link RestDelete#matchers()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder matchers(Class<? extends RestMatcher>...value) {
			this.matchers = value;
			return this;
		}

		/**
		 * Sets the {@link RestDelete#path()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder path(String...value) {
			this.path = value;
			return this;
		}

		/**
		 * Sets the {@link RestDelete#roleGuard()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder roleGuard(String value) {
			this.roleGuard = value;
			return this;
		}

		/**
		 * Sets the {@link RestDelete#rolesDeclared()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder rolesDeclared(String value) {
			this.rolesDeclared = value;
			return this;
		}

		/**
		 * Sets the {@link RestDelete#summary()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder summary(String value) {
			this.summary = value;
			return this;
		}

		/**
		 * Sets the {@link RestDelete#swagger()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder swagger(OpSwagger value) {
			this.swagger = value;
			return this;
		}

		/**
		 * Sets the {@link RestDelete#value()} property on this annotation.
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

	private static class Impl extends TargetedAnnotationImpl implements RestDelete {

		private final Class<? extends RestGuard>[] guards;
		private final Class<? extends RestMatcher>[] matchers;
		private final Class<? extends Encoder>[] encoders;
		private final OpSwagger swagger;
		private final String clientVersion, debug, defaultAccept, defaultCharset, rolesDeclared, roleGuard, summary, value;
		private final String[] defaultRequestQueryData, defaultRequestAttributes, defaultRequestHeaders, defaultResponseHeaders, description, path;

		Impl(Builder b) {
			super(b);
			this.clientVersion = b.clientVersion;
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
			this.roleGuard = b.roleGuard;
			this.rolesDeclared = b.rolesDeclared;
			this.summary = b.summary;
			this.swagger = b.swagger;
			this.value = b.value;
			postConstruct();
		}

		@Override /* RestDelete */
		public String clientVersion() {
			return clientVersion;
		}

		@Override /* RestDelete */
		public String debug() {
			return debug;
		}

		@Override /* RestDelete */
		public String defaultAccept() {
			return defaultAccept;
		}

		@Override /* RestDelete */
		public String defaultCharset() {
			return defaultCharset;
		}

		@Override /* RestDelete */
		public String[] defaultRequestQueryData() {
			return defaultRequestQueryData;
		}

		@Override /* RestDelete */
		public String[] defaultRequestAttributes() {
			return defaultRequestAttributes;
		}

		@Override /* RestDelete */
		public String[] defaultRequestHeaders() {
			return defaultRequestHeaders;
		}

		@Override /* RestDelete */
		public String[] defaultResponseHeaders() {
			return defaultResponseHeaders;
		}

		@Override /* RestDelete */
		public String[] description() {
			return description;
		}

		@Override /* RestDelete */
		public Class<? extends Encoder>[] encoders() {
			return encoders;
		}

		@Override /* RestDelete */
		public Class<? extends RestGuard>[] guards() {
			return guards;
		}

		@Override /* RestDelete */
		public Class<? extends RestMatcher>[] matchers() {
			return matchers;
		}

		@Override /* RestDelete */
		public String[] path() {
			return path;
		}

		@Override /* RestDelete */
		public String roleGuard() {
			return roleGuard;
		}

		@Override /* RestDelete */
		public String rolesDeclared() {
			return rolesDeclared;
		}

		@Override /* RestDelete */
		public String summary() {
			return summary;
		}

		@Override /* RestDelete */
		public OpSwagger swagger() {
			return swagger;
		}

		@Override /* RestDelete */
		public String value() {
			return value;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Appliers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Applies {@link RestDelete} annotations to a {@link org.apache.juneau.rest.RestOpContext.Builder}.
	 */
	public static class RestOpContextApply extends AnnotationApplier<RestDelete,RestOpContext.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public RestOpContextApply(VarResolverSession vr) {
			super(RestDelete.class, RestOpContext.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<RestDelete> ai, RestOpContext.Builder b) {
			RestDelete a = ai.inner();

			b.httpMethod("delete");

			classes(a.encoders()).ifPresent(x -> b.encoders().set(x));
			stream(a.defaultRequestHeaders()).map(x -> stringHeader(x)).forEach(x -> b.defaultRequestHeaders().setDefault(x));
			stream(a.defaultResponseHeaders()).map(x -> stringHeader(x)).forEach(x -> b.defaultResponseHeaders().setDefault(x));
			stream(a.defaultRequestAttributes()).map(x -> BasicNamedAttribute.ofPair(x)).forEach(x -> b.defaultRequestAttributes().add(x));
			stream(a.defaultRequestQueryData()).map(x -> basicPart(x)).forEach(x -> b.defaultRequestQueryData().setDefault(x));
			string(a.defaultAccept()).map(x -> accept(x)).ifPresent(x -> b.defaultRequestHeaders().setDefault(x));
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