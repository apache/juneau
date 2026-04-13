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
package org.apache.juneau.http.annotation;

import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.lang.annotation.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.commons.annotation.*;

/**
 * Utility classes and methods for the {@link Response @Response} annotation.
 *
 */
public class ResponseAnnotation {

	/**
	 * Prevents instantiation.
	 */
	private ResponseAnnotation() {}

	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends AnnotationObject.Builder {

		private String[] description = {};
		private Header[] headers = {};
		private Schema schema = SchemaAnnotation.DEFAULT;
		private String[] examples = {};

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(Response.class);
		}

		/**
		 * Instantiates a new {@link Response @Response} object initialized with this builder.
		 *
		 * @return A new {@link Response @Response} object.
		 */
		public Response build() {
			return new Instance(this);
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
		 * Sets the {@link Response#examples} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder examples(String...value) {
			examples = value;
			return this;
		}

		/**
		 * Sets the {@link Response#headers} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder headers(Header...value) {
			headers = value;
			return this;
		}

		/**
		 * Sets the {@link Response#schema} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder schema(Schema value) {
			schema = value;
			return this;
		}

	}

	@SuppressWarnings({
		"java:S2160", // equals() inherited from AnnotationObject compares all annotation interface methods; subclass fields are accessed via those methods
		"ClassExplicitlyAnnotation", // IntelliJ / SonarLint: Instance implements @Response (AnnotationObject runtime proxy pattern)
		"all" // Eclipse JDT: AnnotationTypeUsedAsSuperInterface has no dedicated @SuppressWarnings token (JLS 9.6)
	})
	private static class Instance extends AnnotationObject implements Response {

		private final String[] description;
		private final Header[] headers;
		private final Schema schema;
		private final String[] examples;

		Instance(ResponseAnnotation.Builder b) {
			super(b);
			description = copyOf(b.description);
			examples = copyOf(b.examples);
			headers = copyOf(b.headers);
			schema = b.schema;
		}

		@Override /* Overridden from Response */
		public String[] examples() {
			return examples;
		}

		@Override /* Overridden from Response */
		public Header[] headers() {
			return headers;
		}

		@Override /* Overridden from Response */
		public Schema schema() {
			return schema;
		}

		@Override /* Overridden from annotation */
		public String[] description() {
			return description;
		}
	}

	/** Default value */
	public static final Response DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(Response a) {
		return a == null || DEFAULT.equals(a);
	}
}
