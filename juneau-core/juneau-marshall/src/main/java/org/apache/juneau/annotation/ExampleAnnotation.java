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

import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.lang.annotation.*;

import org.apache.juneau.commons.annotation.*;

/**
 * Utility classes and methods for the {@link Example @Example} annotation.
 *
 */
public class ExampleAnnotation {

	/**
	 * Prevents instantiation.
	 */
	private ExampleAnnotation() {}

	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends AnnotationObject.Builder {

		private String[] description = {};
		private String value = "";

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(Example.class);
		}

		/**
		 * Instantiates a new {@link Example @Example} object initialized with this builder.
		 *
		 * @return A new {@link Example @Example} object.
		 */
		public Example build() {
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
		 * Sets the <c>value</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder value(String value) {
			this.value = value;
			return this;
		}

	}

	@SuppressWarnings({
		"java:S2160" // equals() inherited from AnnotationObject compares all annotation interface methods; subclass fields are accessed via those methods
	})
	private static class Object extends AnnotationObject implements Example {

		private final String[] description;
		private final String value;

		Object(ExampleAnnotation.Builder b) {
			super(b);
			description = copyOf(b.description);
			value = b.value;
		}

		@Override /* Overridden from Example */
		public String value() {
			return value;
		}

		@Override /* Overridden from annotation */
		public String[] description() {
			return description;
		}
	}

	/** Default value */
	public static final Example DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static Builder create() {
		return new Builder();
	}
}
