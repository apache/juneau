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
import org.apache.juneau.common.annotation.*;
import org.apache.juneau.common.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link ParentProperty @ParentProperty} annotation.
 *
 */
public class ParentPropertyAnnotation {
	/**
	 * Applies targeted {@link ParentProperty} annotations to a {@link org.apache.juneau.BeanContext.Builder}.
	 */
	public static class Applier extends AnnotationApplier<ParentProperty,BeanContext.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Applier(VarResolverSession vr) {
			super(ParentProperty.class, BeanContext.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<ParentProperty> ai, BeanContext.Builder b) {
			ParentProperty a = ai.inner();
			if (isEmptyArray(a.on()))
				return;
			b.annotations(copy(a, vr()));
		}
	}

	/**
	 * A collection of {@link ParentProperty @ParentProperty annotations}.
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
		ParentProperty[] value();
	}

	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends AppliedAnnotationObject.BuilderMF<Builder> {

		String[] description = {};

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(ParentProperty.class);
		}

		/**
		 * Instantiates a new {@link ParentProperty @ParentProperty} object initialized with this builder.
		 *
		 * @return A new {@link ParentProperty @ParentProperty} object.
		 */
		public ParentProperty build() {
			return new Object(this);
		}

		/**
		 * Sets the description property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder description(String...value) {
			this.description = value;
			return this;
		}
	}

	private static class Object extends AppliedAnnotationObject implements ParentProperty {

		private final String[] description;

		Object(ParentPropertyAnnotation.Builder b) {
			super(b);
			this.description = copyOf(b.description);
			postConstruct();
		}

		@Override /* Overridden from annotation */
		public String[] description() {
			return description;
		}
	}

	/** Default value */
	public static final ParentProperty DEFAULT = create().build();

	/**
	 * Creates a copy of the specified annotation.
	 *
	 * @param a The annotation to copy.s
	 * @param r The var resolver for resolving any variables.
	 * @return A copy of the specified annotation.
	 */
	public static ParentProperty copy(ParentProperty a, VarResolverSession r) {
		return create().on(r.resolve(a.on())).build();
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
	public static Builder create(String...on) {
		return create().on(on);
	}
}