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
package org.apache.juneau.bson.annotation;

import static org.apache.juneau.commons.utils.CollectionUtils.*;

import org.apache.juneau.commons.annotation.*;

/**
 * Utility classes and methods for the {@link Bson @Bson} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/BsonBasics">BSON Basics</a>
 * </ul>
 */
public class BsonAnnotation {

	/**
	 * Prevents instantiation.
	 */
	private BsonAnnotation() {}
	/**
	 * Builder class.
	 */
	public static class Builder extends AnnotationObject.Builder {

		private String[] description = {};

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(Bson.class);
		}

		/**
		 * Instantiates a new {@link Bson @Bson} object initialized with this builder.
		 *
		 * @return A new {@link Bson @Bson} object.
		 */
		public Bson build() {
			return new BsonAnnotation.Object(this);
		}

		/**
		 * Sets the description property.
		 *
		 * @param value The new value.
		 * @return This object.
		 */
		public Builder description(String...value) {
			description = value;
			return this;
		}
	}

	@SuppressWarnings({
		"java:S2160" // equals() inherited from AnnotationObject compares all annotation interface methods; subclass fields are accessed via those methods
	})
	private static class Object extends AnnotationObject implements Bson {

		private final String[] description;

		Object(BsonAnnotation.Builder b) {
			super(b);
			description = copyOf(b.description);
		}

		@Override /* Bson */
		public String[] description() {
			return description;
		}
	}

	/** Default value */
	public static final Bson DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static BsonAnnotation.Builder create() {
		return new BsonAnnotation.Builder();
	}
}
