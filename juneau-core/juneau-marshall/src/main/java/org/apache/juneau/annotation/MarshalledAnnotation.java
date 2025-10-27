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
package org.apache.juneau.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link Marshalled @Marshalled} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class MarshalledAnnotation {
	/**
	 * Applies targeted {@link Marshalled} annotations to a {@link org.apache.juneau.BeanContext.Builder}.
	 */
	public static class Applier extends AnnotationApplier<Marshalled,BeanContext.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Applier(VarResolverSession vr) {
			super(Marshalled.class, BeanContext.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<Marshalled> ai, BeanContext.Builder b) {
			Marshalled a = ai.inner();
			if (isEmptyArray(a.on()) && isEmptyArray(a.onClass()))
				return;
			b.annotations(copy(a, vr()));
		}
	}

	/**
	 * A collection of {@link Marshalled @Marshalled annotations}.
	 */
	@Documented
	@Target({ METHOD, TYPE })
	@Retention(RUNTIME)
	@Inherited
	public static @interface Array {

		/**
		 * The child annotations.
		 *
		 * @return The annotation value.
		 */
		Marshalled[] value();
	}

	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends TargetedAnnotationTBuilder<Builder> {

		Class<?> implClass = void.class;
		String example = "";

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(Marshalled.class);
		}

		/**
		 * Instantiates a new {@link Marshalled @Marshalled} object initialized with this builder.
		 *
		 * @return A new {@link Marshalled @Marshalled} object.
		 */
		public Marshalled build() {
			return new Impl(this);
		}

		/**
		 * Sets the {@link Marshalled#example()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder example(String value) {
			this.example = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#implClass()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder implClass(Class<?> value) {
			this.implClass = value;
			return this;
		}

	}

	private static class Impl extends TargetedAnnotationTImpl implements Marshalled {

		private final Class<?> implClass;
		private final String example;

		Impl(Builder b) {
			super(b);
			this.example = b.example;
			this.implClass = b.implClass;
			postConstruct();
		}

		@Override /* Overridden from Marshalled */
		public String example() {
			return example;
		}

		@Override /* Overridden from Marshalled */
		public Class<?> implClass() {
			return implClass;
		}
	}

	/** Default value */
	public static final Marshalled DEFAULT = create().build();

	/**
	 * Creates a copy of the specified annotation.
	 *
	 * @param a The annotation to copy.s
	 * @param r The var resolver for resolving any variables.
	 * @return A copy of the specified annotation.
	 */
	public static Marshalled copy(Marshalled a, VarResolverSession r) {
		// @formatter:off
		return
			create()
			.example(r.resolve(a.example()))
			.implClass(a.implClass())
			.on(r.resolve(a.on()))
			.onClass(a.onClass())
			.build();
		// @formatter:on
	}

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
}