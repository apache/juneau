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

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.jsonschema.annotation.*;

/**
 * Builder class for the {@link MethodSwagger} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class MethodSwaggerBuilder extends AnnotationBuilder {

	/** Default value */
	public static final MethodSwagger DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static MethodSwaggerBuilder create() {
		return new MethodSwaggerBuilder();
	}

	private static class Impl extends AnnotationImpl implements MethodSwagger {

		private final ExternalDocs externalDocs;
		private final String deprecated, operationId;
		private final String[] consumes, description, parameters, produces, responses, schemes, summary, tags, value;

		Impl(MethodSwaggerBuilder b) {
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

		@Override /* MethodSwagger */
		public String[] consumes() {
			return consumes;
		}

		@Override /* MethodSwagger */
		public String deprecated() {
			return deprecated;
		}

		@Override /* MethodSwagger */
		public String[] description() {
			return description;
		}

		@Override /* MethodSwagger */
		public ExternalDocs externalDocs() {
			return externalDocs;
		}

		@Override /* MethodSwagger */
		public String operationId() {
			return operationId;
		}

		@Override /* MethodSwagger */
		public String[] parameters() {
			return parameters;
		}

		@Override /* MethodSwagger */
		public String[] produces() {
			return produces;
		}

		@Override /* MethodSwagger */
		public String[] responses() {
			return responses;
		}

		@Override /* MethodSwagger */
		public String[] schemes() {
			return schemes;
		}

		@Override /* MethodSwagger */
		public String[] summary() {
			return summary;
		}

		@Override /* MethodSwagger */
		public String[] tags() {
			return tags;
		}

		@Override /* MethodSwagger */
		public String[] value() {
			return value;
		}
	}


	ExternalDocs externalDocs = ExternalDocsBuilder.DEFAULT;
	String deprecated="", operationId="";
	String[] consumes={}, description={}, parameters={}, produces={}, responses={}, schemes={}, summary={}, tags={}, value={};

	/**
	 * Constructor.
	 */
	public MethodSwaggerBuilder() {
		super(MethodSwagger.class);
	}

	/**
	 * Instantiates a new {@link MethodSwagger @MethodSwagger} object initialized with this builder.
	 *
	 * @return A new {@link MethodSwagger @MethodSwagger} object.
	 */
	public MethodSwagger build() {
		return new Impl(this);
	}

	/**
	 * Sets the {@link MethodSwagger#consumes()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public MethodSwaggerBuilder consumes(String...value) {
		this.consumes = value;
		return this;
	}

	/**
	 * Sets the {@link MethodSwagger#deprecated()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public MethodSwaggerBuilder deprecated(String value) {
		this.deprecated = value;
		return this;
	}

	/**
	 * Sets the {@link MethodSwagger#description()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public MethodSwaggerBuilder description(String...value) {
		this.description = value;
		return this;
	}

	/**
	 * Sets the {@link MethodSwagger#externalDocs()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public MethodSwaggerBuilder externalDocs(ExternalDocs value) {
		this.externalDocs = value;
		return this;
	}

	/**
	 * Sets the {@link MethodSwagger#operationId()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public MethodSwaggerBuilder operationId(String value) {
		this.operationId = value;
		return this;
	}

	/**
	 * Sets the {@link MethodSwagger#parameters()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public MethodSwaggerBuilder parameters(String...value) {
		this.parameters = value;
		return this;
	}

	/**
	 * Sets the {@link MethodSwagger#produces()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public MethodSwaggerBuilder produces(String...value) {
		this.produces = value;
		return this;
	}

	/**
	 * Sets the {@link MethodSwagger#responses()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public MethodSwaggerBuilder responses(String...value) {
		this.responses = value;
		return this;
	}

	/**
	 * Sets the {@link MethodSwagger#schemes()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public MethodSwaggerBuilder schemes(String...value) {
		this.schemes = value;
		return this;
	}

	/**
	 * Sets the {@link MethodSwagger#summary()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public MethodSwaggerBuilder summary(String...value) {
		this.summary = value;
		return this;
	}

	/**
	 * Sets the {@link MethodSwagger#tags()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public MethodSwaggerBuilder tags(String...value) {
		this.tags = value;
		return this;
	}

	/**
	 * Sets the {@link MethodSwagger#value()} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public MethodSwaggerBuilder value(String...value) {
		this.value = value;
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>
}
