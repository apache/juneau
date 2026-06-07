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
package org.apache.juneau.commons.bean;

import static org.apache.juneau.commons.utils.CollectionUtils.*;

import org.apache.juneau.commons.*;

/**
 * Utility classes and methods for the {@link BeanCtor @BeanCtor} annotation.
 *
 * <p>
 * Provides a {@link Builder} that constructs a synthetic {@link BeanCtor @BeanCtor} annotation instance
 * programmatically without requiring it to be declared on a program element at compile time.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='ja'>{@link BeanCtor}
 * 	<li class='jc'>{@link AnnotationObject}
 * </ul>
 */
public class BeanCtorAnnotation {

	/**
	 * Prevents instantiation.
	 */
	private BeanCtorAnnotation() {}

	/**
	 * Builder class.
	 */
	public static class Builder extends AnnotationObject.Builder {

		private String[] description = {};
		private String properties = "";

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(BeanCtor.class);
		}

		/**
		 * Instantiates a new {@link BeanCtor @BeanCtor} object initialized with this builder.
		 *
		 * @return A new {@link BeanCtor @BeanCtor} object.
		 */
		public BeanCtor build() {
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
		 * Sets the {@link BeanCtor#properties()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder properties(String value) {
			properties = value;
			return this;
		}

	}

	@SuppressWarnings({
		"java:S2160", // equals() inherited from AnnotationObject compares all annotation interface methods; subclass fields are accessed via those methods
		"annotationSuperInterface" // Eclipse JDT: intentional concrete implementation of annotation interface for runtime-built annotation instances
	})
	private static class Object extends AnnotationObject implements BeanCtor {

		private final String[] description;
		private final String properties;

		Object(BeanCtorAnnotation.Builder b) {
			super(b);
			description = copyOf(b.description);
			properties = b.properties;
		}

		@Override /* Overridden from BeanCtor */
		public String[] description() {
			return description;
		}

		@Override /* Overridden from BeanCtor */
		public String properties() {
			return properties;
		}
	}

	/** Default value */
	public static final BeanCtor DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static Builder create() {
		return new Builder();
	}
}
