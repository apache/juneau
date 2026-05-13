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
 * Utility classes and methods for the {@link MarshalledProp @MarshalledProp} annotation.
 *
 */
public class MarshalledPropAnnotation {

	/**
	 * Prevents instantiation.
	 */
	private MarshalledPropAnnotation() {}

	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.MarshallingContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends AnnotationObject.Builder {

		private String[] description = {};
		private Class<?>[] dictionary = new Class[0];
		private String format = "";

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(MarshalledProp.class);
		}

		/**
		 * Instantiates a new {@link MarshalledProp @MarshalledProp} object initialized with this builder.
		 *
		 * @return A new {@link MarshalledProp @MarshalledProp} object.
		 */
		public MarshalledProp build() {
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
		 * Sets the {@link MarshalledProp#dictionary()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder dictionary(Class<?>...value) {
			dictionary = value;
			return this;
		}

		/**
		 * Sets the {@link MarshalledProp#format()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder format(String value) {
			format = value;
			return this;
		}

	}

	@SuppressWarnings({
		"java:S2160" // equals() inherited from AnnotationObject compares all annotation interface methods; subclass fields are accessed via those methods
	})
	private static class Object extends AnnotationObject implements MarshalledProp {

		private final String[] description;
		private final Class<?>[] dictionary;
		private final String format;

		Object(MarshalledPropAnnotation.Builder b) {
			super(b);
			description = copyOf(b.description);
			dictionary = copyOf(b.dictionary);
			format = b.format;
		}

		@Override /* Overridden from MarshalledProp */
		public Class<?>[] dictionary() {
			return dictionary;
		}

		@Override /* Overridden from MarshalledProp */
		public String format() {
			return format;
		}

		@Override /* Overridden from annotation */
		public String[] description() {
			return description;
		}
	}

	/** Default value */
	public static final MarshalledProp DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static Builder create() {
		return new Builder();
	}
}
