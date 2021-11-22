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
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link ResponseHeader @ResponseHeader} annotation.
 */
public class ResponseHeaderAnnotation {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/** Default value */
	public static final ResponseHeader DEFAULT = create().build();

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
	public static boolean empty(ResponseHeader a) {
		return a == null || DEFAULT.equals(a);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends TargetedAnnotationTMBuilder {

		Class<? extends HttpPartSerializer> serializer = HttpPartSerializer.Null.class;
		int[] code={};
		Schema schema = SchemaAnnotation.DEFAULT;
		String n="", name="", value="";
		String[] d={}, description={};

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(ResponseHeader.class);
		}

		/**
		 * Instantiates a new {@link ResponseHeader @ResponseHeader} object initialized with this builder.
		 *
		 * @return A new {@link ResponseHeader @ResponseHeader} object.
		 */
		public ResponseHeader build() {
			return new Impl(this);
		}

		/**
		 * Sets the {@link ResponseHeader#code} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder code(int...value) {
			this.code = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#d} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder d(String...value) {
			this.d = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#description} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder description(String...value) {
			this.description = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#n} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder n(String value) {
			this.n = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#name} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder name(String value) {
			this.name = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#schema} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder schema(Schema value) {
			this.schema = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#serializer} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder serializer(Class<? extends HttpPartSerializer> value) {
			this.serializer = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#value} property on this annotation.
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

	private static class Impl extends TargetedAnnotationTImpl implements ResponseHeader {

		private final Class<? extends HttpPartSerializer> serializer;
		private final int[] code;
		private final String n, name, value;
		private final String[] d, description;
		private final Schema schema;

		Impl(Builder b) {
			super(b);
			this.code = Arrays.copyOf(b.code, b.code.length);
			this.d = copyOf(b.d);
			this.description = copyOf(b.description);
			this.n = b.n;
			this.name = b.name;
			this.schema = b.schema;
			this.serializer = b.serializer;
			this.value = b.value;
			postConstruct();
		}

		@Override /* ResponseHeader */
		public int[] code() {
			return code;
		}

		@Override /* ResponseHeader */
		public String[] d() {
			return d;
		}

		@Override /* ResponseHeader */
		public String[] description() {
			return description;
		}

		@Override /* ResponseHeader */
		public String n() {
			return n;
		}

		@Override /* ResponseHeader */
		public String name() {
			return name;
		}

		@Override /* ResponseHeader */
		public Schema schema() {
			return schema;
		}

		@Override /* ResponseHeader */
		public Class<? extends HttpPartSerializer> serializer() {
			return serializer;
		}

		@Override /* ResponseHeader */
		public String value() {
			return value;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Appliers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Applies targeted {@link ResponseHeader} annotations to a {@link org.apache.juneau.BeanContext.Builder}.
	 */
	public static class Applier extends AnnotationApplier<ResponseHeader,BeanContext.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Applier(VarResolverSession vr) {
			super(ResponseHeader.class, BeanContext.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<ResponseHeader> ai, BeanContext.Builder b) {
			ResponseHeader a = ai.getAnnotation();

			if (isEmpty(a.on()) && isEmpty(a.onClass()))
				return;

			b.annotations(a);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * A collection of {@link ResponseHeader @ResponseHeader annotations}.
	 */
	@Documented
	@Target({METHOD,TYPE})
	@Retention(RUNTIME)
	@Inherited
	public static @interface Array {

		/**
		 * The child annotations.
		 */
		ResponseHeader[] value();
	}
}