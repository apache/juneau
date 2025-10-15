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
package org.apache.juneau.http.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.internal.ArrayUtils.*;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link PathRemainder @PathRemainder} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='ja'>{@link PathRemainder}
 * 	<li class='ja'>{@link Path}
 * </ul>
 *
 * @since 9.2.0
 */
public class PathRemainderAnnotation {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/** Default value */
	public static final PathRemainder DEFAULT = create().build();

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
	public static boolean empty(PathRemainder a) {
		return a == null || DEFAULT.equals(a);
	}

	/**
	 * Finds the default value from the specified list of annotations.
	 *
	 * @param pi The parameter.
	 * @return The last matching default value, or {@link Value#empty()} if not found.
	 */
	public static Value<String> findDef(ParamInfo pi) {
		Value<String> n = Value.empty();
		pi.forEachAnnotation(PathRemainder.class, x -> isNotEmpty(x.def()), x -> n.set(x.def()));
		return n;
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
	public static class Builder extends TargetedAnnotationTMFBuilder<Builder> {

		Class<? extends HttpPartParser> parser = HttpPartParser.Void.class;
		Class<? extends HttpPartSerializer> serializer = HttpPartSerializer.Void.class;
		Schema schema = SchemaAnnotation.DEFAULT;
		String def="";
		String[] description = {};

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(PathRemainder.class);
		}

		/**
		 * Instantiates a new {@link PathRemainder @PathRemainder} object initialized with this builder.
		 *
		 * @return A new {@link PathRemainder @PathRemainder} object.
		 */
		public PathRemainder build() {
			return new Impl(this);
		}

		/**
		 * Sets the {@link PathRemainder#def} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder def(String value) {
			this.def = value;
			return this;
		}

		/**
		 * Sets the {@link PathRemainder#description} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder description(String...value) {
			this.description = value;
			return this;
		}

		/**
		 * Sets the {@link PathRemainder#parser} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder parser(Class<? extends HttpPartParser> value) {
			this.parser = value;
			return this;
		}

		/**
		 * Sets the {@link PathRemainder#schema} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder schema(Schema value) {
			this.schema = value;
			return this;
		}

		/**
		 * Sets the {@link PathRemainder#serializer} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder serializer(Class<? extends HttpPartSerializer> value) {
			this.serializer = value;
			return this;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Implementation
	//-----------------------------------------------------------------------------------------------------------------

	private static class Impl extends TargetedAnnotationTImpl implements PathRemainder {

		private final Class<? extends HttpPartParser> parser;
		private final Class<? extends HttpPartSerializer> serializer;
		private final String def;
		private final String[] description;
		private final Schema schema;

		Impl(Builder b) {
			super(b);
			this.def = b.def;
			this.description = copyOf(b.description);
			this.parser = b.parser;
			this.schema = b.schema;
			this.serializer = b.serializer;
			postConstruct();
		}

		@Override /* PathRemainder */
		public String def() {
			return def;
		}

		@Override /* PathRemainder */
		public String[] description() {
			return description;
		}

		@Override /* PathRemainder */
		public Class<? extends HttpPartParser> parser() {
			return parser;
		}

		@Override /* PathRemainder */
		public Schema schema() {
			return schema;
		}

		@Override /* PathRemainder */
		public Class<? extends HttpPartSerializer> serializer() {
			return serializer;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Appliers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Applies targeted {@link PathRemainder} annotations to a {@link org.apache.juneau.BeanContext.Builder}.
	 */
	public static class Applier extends AnnotationApplier<PathRemainder,BeanContext.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Applier(VarResolverSession vr) {
			super(PathRemainder.class, BeanContext.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<PathRemainder> ai, BeanContext.Builder b) {
			PathRemainder a = ai.inner();
			if (isEmptyArray(a.on(), a.onClass()))
				return;
			b.annotations(a);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * A collection of {@link PathRemainder @PathRemainder annotations}.
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
		PathRemainder[] value();
	}
}