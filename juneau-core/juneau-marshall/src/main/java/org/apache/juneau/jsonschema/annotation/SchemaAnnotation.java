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
package org.apache.juneau.jsonschema.annotation;

import java.lang.annotation.*;

import org.apache.juneau.*;

/**
 * A concrete implementation of the {@link Schema} annotation.
 *
 * <p>
 * Annotations can be applied programmatically using {@link BeanContextBuilder#annotations(Annotation...)}.
 */
public class SchemaAnnotation implements Schema {

	private String on = "";

	private String
		$ref = "",
		format = "",
		title = "",
		multipleOf = "",
		maximum = "",
		minimum = "",
		pattern = "",
		type = "",
		collectionFormat = "",
		discriminator = "";

	private String[]
		description = new String[0],
		_default = new String[0],
		_enum = new String[0],
		allOf = new String[0],
		properties = new String[0],
		additionalProperties = new String[0],
		xml = new String[0],
		example = new String[0],
		examples = new String[0],
		value = new String[0];

	private boolean
		exclusiveMaximum = false,
		exclusiveMinimum = false,
		uniqueItems = false,
		required = false,
		readOnly = false,
		ignore = false;

	private long
		maxLength = -1,
		minLength = -1,
		maxItems = -1,
		minItems = -1,
		maxProperties = -1,
		minProperties = -1;

	private Items items = new ItemsAnnotation();
	private ExternalDocs externalDocs = new ExternalDocsAnnotation();


	/**
	 * Constructor.
	 *
	 * @param on The initial value for the <c>on</c> property.
	 * 	<br>See {@link Schema#on()}
	 */
	public SchemaAnnotation(String on) {
		this.on = on;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Schema.class;
	}

	@Override /* Schema */
	public String[] _default() {
		return _default;
	}

	/**
	 * Sets the <c>_default</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation _default(String[] value) {
		this._default = value;
		return this;
	}

	@Override /* Schema */
	public String[] _enum() {
		return _enum;
	}

	/**
	 * Sets the <c>_enum</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation _enum(String[] value) {
		this._enum = value;
		return this;
	}

	@Override /* Schema */
	public String $ref() {
		return $ref;
	}

	/**
	 * Sets the <c>$ref</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation $ref(String value) {
		this.$ref = value;
		return this;
	}

	@Override /* Schema */
	public String[] additionalProperties() {
		return additionalProperties;
	}

	/**
	 * Sets the <c>additionalProperties</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation additionalProperties(String[] value) {
		this.additionalProperties = value;
		return this;
	}

	@Override /* Schema */
	public String[] allOf() {
		return allOf;
	}

	/**
	 * Sets the <c>allOf</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation allOf(String[] value) {
		this.allOf = value;
		return this;
	}

	@Override /* Schema */
	public String collectionFormat() {
		return collectionFormat;
	}

	/**
	 * Sets the <c>collectionFormat</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation collectionFormat(String value) {
		this.collectionFormat = value;
		return this;
	}

	@Override /* Schema */
	public String[] description() {
		return description;
	}

	/**
	 * Sets the <c>description</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation description(String[] value) {
		this.description = value;
		return this;
	}

	@Override /* Schema */
	public String discriminator() {
		return discriminator;
	}

	/**
	 * Sets the <c>discriminator</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation discriminator(String value) {
		this.discriminator = value;
		return this;
	}

	@Override /* Schema */
	public String[] example() {
		return example;
	}

	/**
	 * Sets the <c>example</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation example(String[] value) {
		this.example = value;
		return this;
	}

	@Override /* Schema */
	public String[] examples() {
		return examples;
	}

	/**
	 * Sets the <c>examples</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation examples(String[] value) {
		this.examples = value;
		return this;
	}

	@Override /* Schema */
	public boolean exclusiveMaximum() {
		return exclusiveMaximum;
	}

	/**
	 * Sets the <c>exclusiveMaximum</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation exclusiveMaximum(boolean value) {
		this.exclusiveMaximum = value;
		return this;
	}

	@Override /* Schema */
	public boolean exclusiveMinimum() {
		return exclusiveMinimum;
	}

	/**
	 * Sets the <c>exclusiveMinimum</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation exclusiveMinimum(boolean value) {
		this.exclusiveMinimum = value;
		return this;
	}

	@Override /* Schema */
	public ExternalDocs externalDocs() {
		return externalDocs;
	}

	/**
	 * Sets the <c>externalDocs</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation externalDocs(ExternalDocs value) {
		this.externalDocs = value;
		return this;
	}

	@Override /* Schema */
	public String format() {
		return format;
	}

	/**
	 * Sets the <c>format</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation format(String value) {
		this.format = value;
		return this;
	}

	@Override /* Schema */
	public boolean ignore() {
		return ignore;
	}

	/**
	 * Sets the <c>ignore</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation ignore(boolean value) {
		this.ignore = value;
		return this;
	}

	@Override /* Schema */
	public Items items() {
		return items;
	}

	/**
	 * Sets the <c>items</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation items(Items value) {
		this.items = value;
		return this;
	}

	@Override /* Schema */
	public String maximum() {
		return maximum;
	}

	/**
	 * Sets the <c>maximum</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation maximum(String value) {
		this.maximum = value;
		return this;
	}

	@Override /* Schema */
	public long maxItems() {
		return maxItems;
	}

	/**
	 * Sets the <c>maxItems</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation maxItems(long value) {
		this.maxItems = value;
		return this;
	}

	@Override /* Schema */
	public long maxLength() {
		return maxLength;
	}

	/**
	 * Sets the <c>maxLength</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation maxLength(long value) {
		this.maxLength = value;
		return this;
	}

	@Override /* Schema */
	public long maxProperties() {
		return maxProperties;
	}

	/**
	 * Sets the <c>maxProperties</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation maxProperties(long value) {
		this.maxProperties = value;
		return this;
	}

	@Override /* Schema */
	public String minimum() {
		return minimum;
	}

	/**
	 * Sets the <c>minimum</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation minimum(String value) {
		this.minimum = value;
		return this;
	}

	@Override /* Schema */
	public long minItems() {
		return minItems;
	}

	/**
	 * Sets the <c>minItems</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation minItems(long value) {
		this.minItems = value;
		return this;
	}

	@Override /* Schema */
	public long minLength() {
		return minLength;
	}

	/**
	 * Sets the <c>minLength</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation minLength(long value) {
		this.minLength = value;
		return this;
	}

	@Override /* Schema */
	public long minProperties() {
		return minProperties;
	}

	/**
	 * Sets the <c>minProperties</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation minProperties(long value) {
		this.minProperties = value;
		return this;
	}

	@Override /* Schema */
	public String multipleOf() {
		return multipleOf;
	}

	/**
	 * Sets the <c>multipleOf</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation multipleOf(String value) {
		this.multipleOf = value;
		return this;
	}

	@Override /* Schema */
	public String on() {
		return on;
	}

	/**
	 * Sets the <c>on</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation on(String value) {
		this.on = value;
		return this;
	}

	@Override /* Schema */
	public String pattern() {
		return pattern;
	}

	/**
	 * Sets the <c>pattern</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation pattern(String value) {
		this.pattern = value;
		return this;
	}

	@Override /* Schema */
	public String[] properties() {
		return properties;
	}

	/**
	 * Sets the <c>properties</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation properties(String[] value) {
		this.properties = value;
		return this;
	}

	@Override /* Schema */
	public boolean readOnly() {
		return readOnly;
	}

	/**
	 * Sets the <c>readOnly</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation readOnly(boolean value) {
		this.readOnly = value;
		return this;
	}

	@Override /* Schema */
	public boolean required() {
		return required;
	}

	/**
	 * Sets the <c>required</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation required(boolean value) {
		this.required = value;
		return this;
	}

	@Override /* Schema */
	public String title() {
		return title;
	}

	/**
	 * Sets the <c>title</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation title(String value) {
		this.title = value;
		return this;
	}

	@Override /* Schema */
	public String type() {
		return type;
	}

	/**
	 * Sets the <c>type</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation type(String value) {
		this.type = value;
		return this;
	}

	@Override /* Schema */
	public boolean uniqueItems() {
		return uniqueItems;
	}

	/**
	 * Sets the <c>uniqueItems</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation uniqueItems(boolean value) {
		this.uniqueItems = value;
		return this;
	}

	@Override /* Schema */
	public String[] value() {
		return value;
	}

	/**
	 * Sets the <c>value</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation value(String[] value) {
		this.value = value;
		return this;
	}

	@Override /* Schema */
	public String[] xml() {
		return xml;
	}

	/**
	 * Sets the <c>xml</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaAnnotation xml(String[] value) {
		this.xml = value;
		return this;
	}
}
