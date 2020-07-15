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

import org.apache.juneau.httppart.*;
import org.apache.juneau.jsonschema.annotation.*;

/**
 * A concrete implementation of the {@link Response} annotation.
 */
public class ResponseAnnotation implements Response {

	private Class<? extends HttpPartParser> parser = HttpPartParser.Null.class;
	private Class<? extends HttpPartSerializer> serializer = HttpPartSerializer.Null.class;
	private int[] code={}, value={};
	private String[] description={}, d={}, example={}, ex={}, examples={}, exs={}, api={};
	private Schema schema = new SchemaAnnotation();
	private ResponseHeader[] headers={};

	@Override /* Response */ /* Annotation */
	public Class<? extends Annotation> annotationType() {
		return Response.class;
	}

	@Override /* Response */
	public Class<? extends HttpPartParser> parser() {
		return parser;
	}

	/**
	 * Sets the <c>parser</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseAnnotation parser(Class<? extends HttpPartParser> value) {
		this.parser = value;
		return this;
	}

	@Override /* Response */
	public Class<? extends HttpPartSerializer> serializer() {
		return serializer;
	}

	/**
	 * Sets the <c>serializer</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseAnnotation serializer(Class<? extends HttpPartSerializer> value) {
		this.serializer = value;
		return this;
	}

	@Override /* Response */
	public int[] code() {
		return code;
	}

	/**
	 * Sets the <c>code</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseAnnotation code(int[] value) {
		this.code = value;
		return this;
	}

	@Override /* Response */
	public int[] value() {
		return value;
	}

	/**
	 * Sets the <c>value</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseAnnotation value(int[] value) {
		this.value = value;
		return this;
	}

	@Override /* Response */
	public String[] description() {
		return description;
	}

	/**
	 * Sets the <c>description</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseAnnotation description(String[] value) {
		this.description = value;
		return this;
	}

	@Override /* Response */
	public String[] d() {
		return d;
	}

	/**
	 * Sets the <c>d</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseAnnotation d(String[] value) {
		this.d = value;
		return this;
	}

	@Override /* Response */
	public Schema schema() {
		return schema;
	}

	/**
	 * Sets the <c>schema</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseAnnotation schema(Schema value) {
		this.schema = value;
		return this;
	}

	@Override /* Response */
	public ResponseHeader[] headers() {
		return headers;
	}

	/**
	 * Sets the <c>headers</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseAnnotation headers(ResponseHeader[] value) {
		this.headers = value;
		return this;
	}

	@Override /* Response */
	public String[] example() {
		return example;
	}

	/**
	 * Sets the <c>example</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseAnnotation example(String[] value) {
		this.example = value;
		return this;
	}

	@Override /* Response */
	public String[] ex() {
		return ex;
	}

	/**
	 * Sets the <c>ex</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseAnnotation ex(String[] value) {
		this.ex = value;
		return this;
	}

	@Override /* Response */
	public String[] examples() {
		return examples;
	}

	/**
	 * Sets the <c>examples</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseAnnotation examples(String[] value) {
		this.examples = value;
		return this;
	}

	@Override /* Response */
	public String[] exs() {
		return exs;
	}

	/**
	 * Sets the <c>exs</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseAnnotation exs(String[] value) {
		this.exs = value;
		return this;
	}

	@Override /* Response */
	public String[] api() {
		return api;
	}

	/**
	 * Sets the <c>api</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseAnnotation api(String[] value) {
		this.api = value;
		return this;
	}
}
