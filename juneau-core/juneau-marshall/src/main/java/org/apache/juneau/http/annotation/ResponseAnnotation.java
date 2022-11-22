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
package org.apache.juneau.http.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.apache.juneau.internal.ArrayUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link Response @Response} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class ResponseAnnotation {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/** Default value */
	public static final Response DEFAULT = create().build();

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
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(Response a) {
		return a == null || DEFAULT.equals(a);
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
	public static class Builder extends TargetedAnnotationTMBuilder {

		Class<? extends HttpPartParser> parser = HttpPartParser.Void.class;
		Class<? extends HttpPartSerializer> serializer = HttpPartSerializer.Void.class;
		Header[] headers={};
		Schema schema = SchemaAnnotation.DEFAULT;
		String[] examples={};

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(Response.class);
		}

		/**
		 * Instantiates a new {@link Response @Response} object initialized with this builder.
		 *
		 * @return A new {@link Response @Response} object.
		 */
		public Response build() {
			return new Impl(this);
		}

		/**
		 * Sets the {@link Response#examples} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder examples(String...value) {
			this.examples = value;
			return this;
		}

		/**
		 * Sets the {@link Response#headers} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder headers(Header...value) {
			this.headers = value;
			return this;
		}

		/**
		 * Sets the {@link Response#parser} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder parser(Class<? extends HttpPartParser> value) {
			this.parser = value;
			return this;
		}

		/**
		 * Sets the {@link Response#schema} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder schema(Schema value) {
			this.schema = value;
			return this;
		}

		/**
		 * Sets the {@link Response#serializer} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder serializer(Class<? extends HttpPartSerializer> value) {
			this.serializer = value;
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

		@Override /* GENERATED - TargetedAnnotationTMBuilder */
		public Builder on(Method...value) {
			super.on(value);
			return this;
		}

		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Implementation
	//-----------------------------------------------------------------------------------------------------------------

	private static class Impl extends TargetedAnnotationTImpl implements Response {

		private final Class<? extends HttpPartParser> parser;
		private final Class<? extends HttpPartSerializer> serializer;
		private final Header[] headers;
		private final Schema schema;
		private final String[] examples;

		Impl(Builder b) {
			super(b);
			this.examples = copyOf(b.examples);
			this.headers = copyOf(b.headers);
			this.parser = b.parser;
			this.schema = b.schema;
			this.serializer = b.serializer;
			postConstruct();
		}

		@Override /* Response */
		public String[] examples() {
			return examples;
		}

		@Override /* Response */
		public Header[] headers() {
			return headers;
		}

		@Override /* Response */
		public Class<? extends HttpPartParser> parser() {
			return parser;
		}

		@Override /* Response */
		public Schema schema() {
			return schema;
		}

		@Override /* Response */
		public Class<? extends HttpPartSerializer> serializer() {
			return serializer;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Appliers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Applies targeted {@link Response} annotations to a {@link org.apache.juneau.BeanContext.Builder}.
	 */
	public static class Applier extends AnnotationApplier<Response,BeanContext.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Applier(VarResolverSession vr) {
			super(Response.class, BeanContext.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<Response> ai, BeanContext.Builder b) {
			Response a = ai.inner();
			if (isEmptyArray(a.on(), a.onClass()))
				return;
			b.annotations(a);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * A collection of {@link Response @Response annotations}.
	 */
	@Documented
	@Target({METHOD,TYPE})
	@Retention(RUNTIME)
	@Inherited
	public static @interface Array {

		/**
		 * The child annotations.
		 *
		 * @return The annotation value.
		 */
		Response[] value();
	}
}