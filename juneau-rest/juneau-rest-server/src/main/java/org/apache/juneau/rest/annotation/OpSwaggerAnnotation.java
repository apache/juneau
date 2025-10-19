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
package org.apache.juneau.rest.annotation;

import java.lang.annotation.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.common.utils.*;

/**
 * Utility classes and methods for the {@link OpSwagger @OpSwagger} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanSwagger2">juneau-bean-swagger-v2</a>
 * </ul>
 */
public class OpSwaggerAnnotation {
	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends AnnotationBuilder<Builder> {

		ExternalDocs externalDocs = ExternalDocsAnnotation.DEFAULT;
		String deprecated = "", operationId = "";
		String[] consumes = {}, parameters = {}, produces = {}, responses = {}, schemes = {}, summary = {}, tags = {}, value = {};

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(OpSwagger.class);
		}

		/**
		 * Instantiates a new {@link OpSwagger @OpSwagger} object initialized with this builder.
		 *
		 * @return A new {@link OpSwagger @OpSwagger} object.
		 */
		public OpSwagger build() {
			return new Impl(this);
		}

		/**
		 * Sets the {@link OpSwagger#consumes()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder consumes(String...value) {
			this.consumes = value;
			return this;
		}

		/**
		 * Sets the {@link OpSwagger#deprecated()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder deprecated(String value) {
			this.deprecated = value;
			return this;
		}

		/**
		 * Sets the {@link OpSwagger#externalDocs()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder externalDocs(ExternalDocs value) {
			this.externalDocs = value;
			return this;
		}

		/**
		 * Sets the {@link OpSwagger#operationId()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder operationId(String value) {
			this.operationId = value;
			return this;
		}

		/**
		 * Sets the {@link OpSwagger#parameters()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder parameters(String...value) {
			this.parameters = value;
			return this;
		}

		/**
		 * Sets the {@link OpSwagger#produces()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder produces(String...value) {
			this.produces = value;
			return this;
		}

		/**
		 * Sets the {@link OpSwagger#responses()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder responses(String...value) {
			this.responses = value;
			return this;
		}

		/**
		 * Sets the {@link OpSwagger#schemes()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder schemes(String...value) {
			this.schemes = value;
			return this;
		}

		/**
		 * Sets the {@link OpSwagger#summary()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder summary(String...value) {
			this.summary = value;
			return this;
		}

		/**
		 * Sets the {@link OpSwagger#tags()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder tags(String...value) {
			this.tags = value;
			return this;
		}

		/**
		 * Sets the {@link OpSwagger#value()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder value(String...value) {
			this.value = value;
			return this;
		}

	}

	private static class Impl extends AnnotationImpl implements OpSwagger {

		private final ExternalDocs externalDocs;
		private final String deprecated, operationId;
		private final String[] consumes, parameters, produces, responses, schemes, summary, tags, value;

		Impl(Builder b) {
			super(b);
			this.consumes = ArrayUtils.copyOf(b.consumes);
			this.deprecated = b.deprecated;
			this.externalDocs = b.externalDocs;
			this.operationId = b.operationId;
			this.parameters = ArrayUtils.copyOf(b.parameters);
			this.produces = ArrayUtils.copyOf(b.produces);
			this.responses = ArrayUtils.copyOf(b.responses);
			this.schemes = ArrayUtils.copyOf(b.schemes);
			this.summary = ArrayUtils.copyOf(b.summary);
			this.tags = ArrayUtils.copyOf(b.tags);
			this.value = ArrayUtils.copyOf(b.value);
			postConstruct();
		}

		@Override /* Overridden from OpSwagger */
		public String[] consumes() {
			return consumes;
		}

		@Override /* Overridden from OpSwagger */
		public String deprecated() {
			return deprecated;
		}

		@Override /* Overridden from OpSwagger */
		public ExternalDocs externalDocs() {
			return externalDocs;
		}

		@Override /* Overridden from OpSwagger */
		public String operationId() {
			return operationId;
		}

		@Override /* Overridden from OpSwagger */
		public String[] parameters() {
			return parameters;
		}

		@Override /* Overridden from OpSwagger */
		public String[] produces() {
			return produces;
		}

		@Override /* Overridden from OpSwagger */
		public String[] responses() {
			return responses;
		}

		@Override /* Overridden from OpSwagger */
		public String[] schemes() {
			return schemes;
		}

		@Override /* Overridden from OpSwagger */
		public String[] summary() {
			return summary;
		}

		@Override /* Overridden from OpSwagger */
		public String[] tags() {
			return tags;
		}

		@Override /* Overridden from OpSwagger */
		public String[] value() {
			return value;
		}
	}

	/** Default value */
	public static final OpSwagger DEFAULT = create().build();

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
	public static boolean empty(OpSwagger a) {
		return a == null || DEFAULT.equals(a);
	}

	/**
	 * Returns <jk>false</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>false</jk> if the specified annotation contains all default values.
	 */
	public static boolean notEmpty(OpSwagger a) {
		return ! empty(a);
	}
}