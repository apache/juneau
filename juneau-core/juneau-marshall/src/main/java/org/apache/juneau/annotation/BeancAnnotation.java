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

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link Beanc @Beanc} annotation.
 */
public class BeancAnnotation {

	/** Default value */
	public static final Beanc DEFAULT = create().build();

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
	public static Builder create(String...on) {
		return create().on(on);
	}

	/**
	 * Creates a copy of the specified annotation.
	 *
	 * @param a The annotation to copy.
	 * @param r The var resolver for resolving any variables.
	 * @return A copy of the specified annotation.
	 */
	public static Beanc copy(Beanc a, VarResolverSession r) {
		return
			create()
			.on(r.resolve(a.on()))
			.properties(r.resolve(a.properties()))
			.build();
	}

	/**
	 * Builder class for the {@link Beanc} annotation.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends TargetedAnnotationCBuilder {

		String properties="";

		/**
		 * Constructor.
		 */
		public Builder() {
			super(Beanc.class);
		}

		/**
		 * Instantiates a new {@link Beanc @Beanc} object initialized with this builder.
		 *
		 * @return A new {@link Beanc @Beanc} object.
		 */
		public Beanc build() {
			return new Impl(this);
		}

		/**
		 * Sets the {@link Beanc#properties()}  property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder properties(String value) {
			this.properties = value;
			return this;
		}

		// <FluentSetters>

		@Override /* GENERATED - TargetedAnnotationBuilder */
		public Builder on(String...values) {
			super.on(values);
			return this;
		}

		@Override /* GENERATED - TargetedAnnotationCBuilder */
		public Builder on(java.lang.reflect.Constructor<?>...value) {
			super.on(value);
			return this;
		}

		// </FluentSetters>
	}

	private static class Impl extends TargetedAnnotationImpl implements Beanc {

		private String properties="";

		Impl(Builder b) {
			super(b);
			this.properties = b.properties;
			postConstruct();
		}

		@Override /* Beanc */
		public String properties() {
			return properties;
		}
	}

	/**
	 * Applies targeted {@link Beanc} annotations to a {@link BeanContextBuilder}.
	 */
	public static class Applier extends AnnotationApplier<Beanc,BeanContextBuilder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Applier(VarResolverSession vr) {
			super(Beanc.class, BeanContextBuilder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<Beanc> ai, BeanContextBuilder b) {
			Beanc a = ai.getAnnotation();

			if (isEmpty(a.on()))
				return;

			b.annotations(copy(a, vr()));
		}
	}

	/**
	 * A collection of {@link Beanc @Beanc annotations}.
	 */
	@Documented
	@Target({METHOD,TYPE})
	@Retention(RUNTIME)
	@Inherited
	public static @interface Array {

		/**
		 * The child annotations.
		 */
		Beanc[] value();
	}
}
