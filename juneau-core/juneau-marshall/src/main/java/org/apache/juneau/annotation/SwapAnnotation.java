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
import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.lang.annotation.*;

import org.apache.juneau.commons.annotation.*;

/**
 * Utility classes and methods for the {@link Swap @Swap} annotation.
 *
 */
public class SwapAnnotation {

	/**
	 * Prevents instantiation.
	 */
	private SwapAnnotation() {}

	/**
	 * A collection of {@link Swap @Swap annotations}.
	 */
	@Documented
	@Target({ TYPE, ANNOTATION_TYPE, FIELD, METHOD })
	@Retention(RUNTIME)
	@Inherited
	public static @interface Array {

		/**
		 * The child annotations.
		 *
		 * @return The annotation value.
		 */
		Swap[] value();
	}

	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends AnnotationObject.Builder {

		private String[] description = {};
		private Class<?> impl = void.class;
		private Class<?> value = void.class;
		private String template = "";
		private String[] mediaTypes = {};

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(Swap.class);
		}

		/**
		 * Instantiates a new {@link Swap @Swap} object initialized with this builder.
		 *
		 * @return A new {@link Swap @Swap} object.
		 */
		public Swap build() {
			return new Object(this);
		}

		/**
		 * Sets the description property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder description(String...value) {
			description = value;
			return this;
		}

		/**
		 * Sets the {@link Swap#impl()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder impl(Class<?> value) {
			impl = value;
			return this;
		}

		/**
		 * Sets the {@link Swap#mediaTypes()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder mediaTypes(String...value) {
			mediaTypes = value;
			return this;
		}

		/**
		 * Sets the {@link Swap#template()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder template(String value) {
			template = value;
			return this;
		}

		/**
		 * Sets the {@link Swap#value()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder value(Class<?> value) {
			this.value = value;
			return this;
		}

	}

	@SuppressWarnings({
		"java:S2160" // equals not needed; annotation object identity is sufficient for usage in Sets/Maps
	})
	private static class Object extends AnnotationObject implements Swap {

		private final String[] description;
		private final Class<?> impl;
		private final Class<?> value;
		private final String template;
		private final String[] mediaTypes;

		Object(SwapAnnotation.Builder b) {
			super(b);
			description = copyOf(b.description);
			impl = b.impl;
			mediaTypes = copyOf(b.mediaTypes);
			template = b.template;
			value = b.value;
		}

		@Override /* Overridden from Swap */
		public Class<?> impl() {
			return impl;
		}

		@Override /* Overridden from Swap */
		public String[] mediaTypes() {
			return mediaTypes;
		}

		@Override /* Overridden from Swap */
		public String template() {
			return template;
		}

		@Override /* Overridden from Swap */
		public Class<?> value() {
			return value;
		}

		@Override /* Overridden from annotation */
		public String[] description() {
			return description;
		}
	}

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
}
