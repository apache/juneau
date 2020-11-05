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
package org.apache.juneau.http.annotation;

import static org.apache.juneau.internal.ArrayUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.jsonschema.annotation.*;

/**
 * Builder class for the {@link Body} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class BodyBuilder extends TargetedAnnotationTMBuilder {

	/** Default value */
	public static final Body DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static BodyBuilder create() {
		return new BodyBuilder();
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static BodyBuilder create(Class<?>...on) {
		return create().on(on);
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static BodyBuilder create(String...on) {
		return create().on(on);
	}

	private static class Impl extends TargetedAnnotationTImpl implements Body {

		private final String[] api, d, description, ex, example, examples, exs, value;
		private final boolean r, required;
		private final Schema schema;

		Impl(BodyBuilder b) {
			super(b);
			this.api = copyOf(b.api);
			this.d = copyOf(b.d);
			this.description = copyOf(b.description);
			this.ex = copyOf(b.ex);
			this.example = copyOf(b.example);
			this.examples = copyOf(b.examples);
			this.exs = copyOf(b.exs);
			this.r = b.r;
			this.required = b.required;
			this.schema = b.schema;
			this.value = copyOf(b.value);
			postConstruct();
		}

		@Override /* Body */
		public String[] api() {
			return api;
		}

		@Override /* Body */
		public String[] d() {
			return d;
		}

		@Override /* Body */
		public String[] description() {
			return description;
		}

		@Override /* Body */
		public String[] ex() {
			return ex;
		}

		@Override /* Body */
		public String[] example() {
			return example;
		}

		@Override /* Body */
		public String[] examples() {
			return examples;
		}

		@Override /* Body */
		public String[] exs() {
			return exs;
		}

		@Override /* Body */
		public boolean r() {
			return r;
		}

		@Override /* Body */
		public boolean required() {
			return required;
		}

		@Override /* Body */
		public Schema schema() {
			return schema;
		}

		@Override /* Body */
		public String[] value() {
			return value;
		}
	}


	boolean required, r;
	Schema schema = SchemaBuilder.DEFAULT;
	String[] api={}, d={}, description={}, ex={}, example={}, examples={}, exs={}, value={};

	/**
	 * Constructor.
	 */
	public BodyBuilder() {
		super(Body.class);
	}

	/**
	 * Sets the {@link Body#api} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BodyBuilder api(String...value) {
		this.api = value;
		return this;
	}

	/**
	 * Instantiates a new {@link Body @Body} object initialized with this builder.
	 *
	 * @return A new {@link Body @Body} object.
	 */
	public Body build() {
		return new Impl(this);
	}

	/**
	 * Sets the {@link Body#d} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BodyBuilder d(String...value) {
		this.d = value;
		return this;
	}

	/**
	 * Sets the {@link Body#description} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BodyBuilder description(String...value) {
		this.description = value;
		return this;
	}

	/**
	 * Sets the {@link Body#ex} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BodyBuilder ex(String...value) {
		this.ex = value;
		return this;
	}

	/**
	 * Sets the {@link Body#example} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BodyBuilder example(String...value) {
		this.example = value;
		return this;
	}

	/**
	 * Sets the {@link Body#examples} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BodyBuilder examples(String...value) {
		this.examples = value;
		return this;
	}

	/**
	 * Sets the {@link Body#exs} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BodyBuilder exs(String...value) {
		this.exs = value;
		return this;
	}

	/**
	 * Sets the {@link Body#r} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BodyBuilder r(boolean value) {
		this.r = value;
		return this;
	}

	/**
	 * Sets the {@link Body#required} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BodyBuilder required(boolean value) {
		this.required = value;
		return this;
	}

	/**
	 * Sets the {@link Body#schema} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BodyBuilder schema(Schema value) {
		this.schema = value;
		return this;
	}

	/**
	 * Sets the {@link Body#value} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BodyBuilder value(String...value) {
		this.value = value;
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - TargetedAnnotationBuilder */
	public BodyBuilder on(String...values) {
		super.on(values);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTBuilder */
	public BodyBuilder on(java.lang.Class<?>...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTBuilder */
	public BodyBuilder onClass(java.lang.Class<?>...value) {
		super.onClass(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTMBuilder */
	public BodyBuilder on(Method...value) {
		super.on(value);
		return this;
	}

	// </FluentSetters>
}
