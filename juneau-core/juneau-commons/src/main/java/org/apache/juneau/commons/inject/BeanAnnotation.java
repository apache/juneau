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
package org.apache.juneau.commons.inject;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.lang.annotation.*;

import org.apache.juneau.commons.annotation.*;

/**
 * Utility classes and methods for the {@link Bean @Bean} annotation.
 *
 */
public class BeanAnnotation {

	/**
	 * Private constructor to prevent instantiation.
	 */
	private BeanAnnotation() {
		// Utility class - prevent instantiation
	}

	/**
	 * A collection of {@link Bean @Bean} annotations.
	 */
	@Documented
	@Target({ FIELD, METHOD, TYPE })
	@Retention(RUNTIME)
	@Inherited
	public static @interface Array {

		/**
		 * The child annotations.
		 *
		 * @return The annotation value.
		 */
		Bean[] value();
	}

	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@code MarshallingContext.Builder<?>#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends AppliedAnnotationObject.BuilderM {

		private String[] description = {};
		private String name;
		private String value;
		private String[] methodScope;

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(Bean.class);
		}

		/**
		 * Instantiates a new {@link Bean @Bean} object initialized with this builder.
		 *
		 * @return A new {@link Bean @Bean} object.
		 */
		public Bean build() {
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
		 * Sets the {@link Bean#methodScope()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder methodScope(String...value) {
			methodScope = value;
			return this;
		}

		/**
		 * Sets the {@link Bean#name()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder name(String value) {
			name = value;
			return this;
		}

		/**
		 * Sets the {@link Bean#value()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder value(String value) {
			this.value = value;
			return this;
		}

		@Override /* Overridden from AppliedAnnotationObject.Builder */
		public Builder on(String...value) {
			super.on(value);
			return this;
		}
		@Override /* Overridden from AppliedAnnotationObject.BuilderM */
		public Builder on(java.lang.reflect.Method...value) {
			super.on(value);
			return this;
		}


		@Override /* Overridden from AppliedAnnotationObject.BuilderM */
		public Builder on(org.apache.juneau.commons.reflect.MethodInfo...value) {
			super.on(value);
			return this;
		}

	}

	@SuppressWarnings({
		"java:S2160", // equals() inherited from AnnotationObject compares all annotation interface methods; subclass fields are accessed via those methods
		"annotationSuperInterface" // Eclipse JDT: intentional concrete implementation of annotation interface for runtime-built annotation instances
	})
	private static class Object extends AppliedAnnotationObject implements Bean {

		private static final String[] EMPTY_STRINGS = new String[0];

		private final String[] description;
		private final String name;
		private final String value;
		private final String[] methodScope;

		Object(BeanAnnotation.Builder b) {
			super(b);
			// Mirror the compile-time defaults declared on @Bean (empty strings / empty arrays) so
			// callers iterating the runtime instance see annotation-equivalent values instead of nulls.
			description = b.description != null ? copyOf(b.description) : EMPTY_STRINGS;
			name = b.name != null ? b.name : "";
			value = b.value != null ? b.value : "";
			methodScope = b.methodScope != null ? copyOf(b.methodScope) : EMPTY_STRINGS;
		}

		@Override /* Overridden from Bean */
		public String[] methodScope() {
			return methodScope;
		}

		@Override /* Overridden from Bean */
		public String name() {
			return name;
		}

		@Override /* Overridden from Bean */
		public String value() {
			return value;
		}

		@Override /* Overridden from annotation */
		public String[] description() {
			return description;
		}

		@Override /* Overridden from Bean */
		public int priority() {
			return Integer.MAX_VALUE / 2;
		}
	}

	/** Default value */
	public static final Bean DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Pulls the name/value attribute from a {@link Bean} annotation.
	 *
	 * @param a The annotation to check.  Can be <jk>null</jk>.
	 * @return The annotation value, or an empty string if the annotation is <jk>null</jk>.
	 */
	public static String name(Bean a) {
		if (a == null)
			return "";
		if (! a.name().isEmpty())
			return a.name();
		return a.value();
	}
}
