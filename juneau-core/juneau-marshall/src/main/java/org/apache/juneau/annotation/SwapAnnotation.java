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
package org.apache.juneau.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.apache.juneau.BeanContext.*;
import static org.apache.juneau.internal.ArrayUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link Swap @Swap} annotation.
 */
public class SwapAnnotation {

	/** Default value */
	public static final Swap DEFAULT = create().build();

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
	 * Creates a copy of the specified annotation.
	 *
	 * @param a The annotation to copy.s
	 * @param r The var resolver for resolving any variables.
	 * @return A copy of the specified annotation.
	 */
	public static Swap copy(Swap a, VarResolverSession r) {
		return
			create()
			.impl(a.impl())
			.mediaTypes(r.resolve(a.mediaTypes()))
			.on(r.resolve(a.on()))
			.onClass(a.onClass())
			.template(r.resolve(a.template()))
			.value(a.value())
			.build();
	}

	/**
	 * Builder class for the {@link Swap} annotation.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends TargetedAnnotationTMFBuilder {

		Class<?> impl=Null.class, value=Null.class;
		String template="";
		String[] mediaTypes={};

		/**
		 * Constructor.
		 */
		public Builder() {
			super(Swap.class);
		}

		/**
		 * Instantiates a new {@link Swap @Swap} object initialized with this builder.
		 *
		 * @return A new {@link Swap @Swap} object.
		 */
		public Swap build() {
			return new Impl(this);
		}

		/**
		 * Sets the {@link Swap#impl()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder impl(Class<?> value) {
			this.impl = value;
			return this;
		}

		/**
		 * Sets the {@link Swap#mediaTypes()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder mediaTypes(String...value) {
			this.mediaTypes = value;
			return this;
		}

		/**
		 * Sets the {@link Swap#template()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder template(String value) {
			this.template = value;
			return this;
		}

		/**
		 * Sets the {@link Swap#value()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder value(Class<?> value) {
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

		@Override /* GENERATED - TargetedAnnotationTMFBuilder */
		public Builder on(Field...value) {
			super.on(value);
			return this;
		}

		@Override /* GENERATED - TargetedAnnotationTMFBuilder */
		public Builder on(Method...value) {
			super.on(value);
			return this;
		}

		// </FluentSetters>
	}

	private static class Impl extends TargetedAnnotationTImpl implements Swap {

		private final Class<?> impl, value;
		private final String template;
		private final String[] mediaTypes;

		Impl(Builder b) {
			super(b);
			this.impl = b.impl;
			this.mediaTypes = copyOf(b.mediaTypes);
			this.template = b.template;
			this.value = b.value;
			postConstruct();
		}

		@Override /* Swap */
		public Class<?> impl() {
			return impl;
		}

		@Override /* Swap */
		public String[] mediaTypes() {
			return mediaTypes;
		}

		@Override /* Swap */
		public String template() {
			return template;
		}

		@Override /* Swap */
		public Class<?> value() {
			return value;
		}
	}

	/**
	 * Applies targeted {@link Swap} annotations to a {@link ContextPropertiesBuilder}.
	 */
	public static class Apply extends ConfigApply<Swap,ContextPropertiesBuilder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Apply(VarResolverSession vr) {
			super(Swap.class, ContextPropertiesBuilder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<Swap> ai, ContextPropertiesBuilder b) {
			Swap a = ai.getAnnotation();

			if (isEmpty(a.on()) && isEmpty(a.onClass()))
				return;

			b.prependTo(BEAN_annotations, copy(a, vr()));
		}
	}

	/**
	 * A collection of {@link Swap @Swap annotations}.
	 */
	@Documented
	@Target({METHOD,TYPE})
	@Retention(RUNTIME)
	@Inherited
	public static @interface Array {

		/**
		 * The child annotations.
		 */
		Swap[] value();
	}
}
