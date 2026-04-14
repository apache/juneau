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
package org.apache.juneau.jena.annotation;

import java.lang.annotation.*;

import org.apache.juneau.commons.annotation.*;
import org.apache.juneau.jena.*;

/**
 * Utility classes and methods for the {@link Rdf @Rdf} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'>{doc jmr.RdfDetails}
 * </ul>
 */
public class RdfAnnotation {

	private RdfAnnotation() {}

	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends AnnotationObject.Builder {

		private String namespace = "";
		private String prefix = "";
		private boolean beanUri;
		private RdfCollectionFormat collectionFormat = RdfCollectionFormat.DEFAULT;

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(Rdf.class);
		}

		/**
		 * Sets the {@link Rdf#beanUri} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder beanUri(boolean value) {
			beanUri = value;
			return this;
		}

		/**
		 * Instantiates a new {@link Rdf @Rdf} object initialized with this builder.
		 *
		 * @return A new {@link Rdf @Rdf} object.
		 */
		public Rdf build() {
			return new Object(this);
		}

		/**
		 * Sets the {@link Rdf#collectionFormat} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder collectionFormat(RdfCollectionFormat value) {
			collectionFormat = value;
			return this;
		}

		/**
		 * Sets the {@link Rdf#namespace} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder namespace(String value) {
			namespace = value;
			return this;
		}

		/**
		 * Sets the {@link Rdf#prefix} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder prefix(String value) {
			prefix = value;
			return this;
		}
	}

	@SuppressWarnings({
		"java:S2160" // equals() inherited from AnnotationObject compares all annotation interface methods; subclass fields are accessed via those methods
	})
	private static class Object extends AnnotationObject implements Rdf {

		private final boolean beanUri;
		private final RdfCollectionFormat collectionFormat;
		private final String namespace;
		private final String prefix;

		Object(RdfAnnotation.Builder b) {
			super(b);
			beanUri = b.beanUri;
			collectionFormat = b.collectionFormat;
			namespace = b.namespace;
			prefix = b.prefix;
		}

		@Override /* Overridden from Rdf */
		public boolean beanUri() {
			return beanUri;
		}

		@Override /* Overridden from Rdf */
		public RdfCollectionFormat collectionFormat() {
			return collectionFormat;
		}

		@Override /* Overridden from Rdf */
		public String namespace() {
			return namespace;
		}

		@Override /* Overridden from Rdf */
		public String prefix() {
			return prefix;
		}
	}

	/** Default value */
	public static final Rdf DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static Builder create() {
		return new Builder();
	}
}
