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

import java.lang.annotation.*;

import org.apache.juneau.jsonschema.annotation.*;

/**
 * A concrete implementation of the {@link Body} annotation.
 */
public class BodyAnnotation implements Body {

	private String[] description={}, d={}, example={}, ex={}, examples={}, exs={}, value={}, api={};
	private boolean required, r;
	private Schema schema = new SchemaAnnotation();

	@Override /* Annotation */
	public Class<? extends Annotation> annotationType() {
		return Body.class;
	}

	@Override /* Body */
	public String[] description() {
		return description;
	}

	/**
	 * Sets the <c>description</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BodyAnnotation description(String[] value) {
		this.description = value;
		return this;
	}

	@Override /* Body */
	public String[] d() {
		return d;
	}

	/**
	 * Sets the <c>d</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BodyAnnotation d(String[] value) {
		this.d = value;
		return this;
	}

	@Override /* Body */
	public boolean required() {
		return required;
	}

	/**
	 * Sets the <c>required</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BodyAnnotation required(boolean value) {
		this.required = value;
		return this;
	}

	@Override /* Body */
	public boolean r() {
		return r;
	}

	/**
	 * Sets the <c>r</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BodyAnnotation r(boolean value) {
		this.r = value;
		return this;
	}

	@Override /* Body */
	public Schema schema() {
		return schema;
	}

	/**
	 * Sets the <c>schema</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BodyAnnotation schema(Schema value) {
		this.schema = value;
		return this;
	}

	@Override /* Body */
	public String[] example() {
		return example;
	}

	/**
	 * Sets the <c>example</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BodyAnnotation example(String[] value) {
		this.example = value;
		return this;
	}

	@Override /* Body */
	public String[] ex() {
		return ex;
	}

	/**
	 * Sets the <c>ex</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BodyAnnotation ex(String[] value) {
		this.ex = value;
		return this;
	}

	@Override /* Body */
	public String[] examples() {
		return examples;
	}

	/**
	 * Sets the <c>examples</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BodyAnnotation examples(String[] value) {
		this.examples = value;
		return this;
	}

	@Override /* Body */
	public String[] exs() {
		return exs;
	}

	/**
	 * Sets the <c>exs</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BodyAnnotation exs(String[] value) {
		this.exs = value;
		return this;
	}

	@Override /* Body */
	public String[] value() {
		return value;
	}

	/**
	 * Sets the <c>value</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BodyAnnotation value(String[] value) {
		this.value = value;
		return this;
	}

	@Override /* Body */
	public String[] api() {
		return api;
	}

	/**
	 * Sets the <c>api</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public BodyAnnotation api(String[] value) {
		this.api = value;
		return this;
	}
}
