// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.rest.annotation;

import static org.apache.juneau.internal.ArrayUtils.*;

import java.lang.annotation.*;

import org.apache.juneau.annotation.*;

/**
 * Utility classes and methods for the {@link OpSwagger @OpSwagger} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Swagger">Swagger</a>
 * </ul>
 */
public class OpSwaggerAnnotation {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

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

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends AnnotationBuilder {

		ExternalDocs externalDocs = ExternalDocsAnnotation.DEFAULT;
		String deprecated="", operationId="";
		String[] consumes={}, description={}, parameters={}, produces={}, responses={}, schemes={}, summary={}, tags={}, value={};

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
		 * Sets the {@link OpSwagger#description()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder description(String...value) {
			this.description = value;
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

		// <FluentSetters>

		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Implementation
	//-----------------------------------------------------------------------------------------------------------------

	private static class Impl extends AnnotationImpl implements OpSwagger {

		private final ExternalDocs externalDocs;
		private final String deprecated, operationId;
		private final String[] consumes, description, parameters, produces, responses, schemes, summary, tags, value;

		Impl(Builder b) {
			super(b);
			this.consumes = copyOf(b.consumes);
			this.deprecated = b.deprecated;
			this.description = copyOf(b.description);
			this.externalDocs = b.externalDocs;
			this.operationId = b.operationId;
			this.parameters = copyOf(b.parameters);
			this.produces = copyOf(b.produces);
			this.responses = copyOf(b.responses);
			this.schemes = copyOf(b.schemes);
			this.summary = copyOf(b.summary);
			this.tags = copyOf(b.tags);
			this.value = copyOf(b.value);
			postConstruct();
		}

		@Override /* OpSwagger */
		public String[] consumes() {
			return consumes;
		}

		@Override /* OpSwagger */
		public String deprecated() {
			return deprecated;
		}

		@Override /* OpSwagger */
		public String[] description() {
			return description;
		}

		@Override /* OpSwagger */
		public ExternalDocs externalDocs() {
			return externalDocs;
		}

		@Override /* OpSwagger */
		public String operationId() {
			return operationId;
		}

		@Override /* OpSwagger */
		public String[] parameters() {
			return parameters;
		}

		@Override /* OpSwagger */
		public String[] produces() {
			return produces;
		}

		@Override /* OpSwagger */
		public String[] responses() {
			return responses;
		}

		@Override /* OpSwagger */
		public String[] schemes() {
			return schemes;
		}

		@Override /* OpSwagger */
		public String[] summary() {
			return summary;
		}

		@Override /* OpSwagger */
		public String[] tags() {
			return tags;
		}

		@Override /* OpSwagger */
		public String[] value() {
			return value;
		}
	}
}