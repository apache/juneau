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
 * A concrete implementation of the {@link SubItems} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class SubItemsAnnotation implements SubItems {

	private String
		type = "",
		format = "",
		collectionFormat = "",
		pattern = "",
		maximum = "",
		minimum = "",
		multipleOf = "",
		$ref = "";
	private long
		maxLength = -1,
		minLength = -1,
		maxItems = -1,
		minItems = -1;
	private boolean
		exclusiveMaximum = false,
		exclusiveMinimum = false,
		uniqueItems = false;
	private String[]
		_default = new String[0],
		_enum = new String[0],
		value = new String[0],
		items = new String[0];

	@Override
	public Class<? extends Annotation> annotationType() {
		return SubItems.class;
	}

	@Override
	public String type() {
		return type;
	}

	/**
	 * Sets the <c>type</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsAnnotation type(String value) {
		this.type = value;
		return this;
	}

	@Override
	public String format() {
		return format;
	}

	/**
	 * Sets the <c>format</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsAnnotation format(String value) {
		this.format = value;
		return this;
	}

	@Override
	public String collectionFormat() {
		return collectionFormat;
	}

	/**
	 * Sets the <c>collectionFormat</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsAnnotation collectionFormat(String value) {
		this.collectionFormat = value;
		return this;
	}

	@Override
	public String pattern() {
		return pattern;
	}

	/**
	 * Sets the <c>pattern</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsAnnotation pattern(String value) {
		this.pattern = value;
		return this;
	}

	@Override
	public String maximum() {
		return maximum;
	}

	/**
	 * Sets the <c>maximum</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsAnnotation maximum(String value) {
		this.maximum = value;
		return this;
	}

	@Override
	public String minimum() {
		return minimum;
	}

	/**
	 * Sets the <c>minimum</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsAnnotation minimum(String value) {
		this.minimum = value;
		return this;
	}

	@Override
	public String multipleOf() {
		return multipleOf;
	}

	/**
	 * Sets the <c>multipleOf</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsAnnotation multipleOf(String value) {
		this.multipleOf = value;
		return this;
	}

	@Override
	public long maxLength() {
		return maxLength;
	}

	/**
	 * Sets the <c>maxLength</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsAnnotation maxLength(long value) {
		this.maxLength = value;
		return this;
	}

	@Override
	public long minLength() {
		return minLength;
	}

	/**
	 * Sets the <c>minLength</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsAnnotation minLength(long value) {
		this.minLength = value;
		return this;
	}

	@Override
	public long maxItems() {
		return maxItems;
	}

	/**
	 * Sets the <c>maxItems</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsAnnotation maxItems(long value) {
		this.maxItems = value;
		return this;
	}

	@Override
	public long minItems() {
		return minItems;
	}

	/**
	 * Sets the <c>minItems</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsAnnotation minItems(long value) {
		this.minItems = value;
		return this;
	}

	@Override
	public boolean exclusiveMaximum() {
		return exclusiveMaximum;
	}

	/**
	 * Sets the <c>exclusiveMaximum</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsAnnotation exclusiveMaximum(boolean value) {
		this.exclusiveMaximum = value;
		return this;
	}

	@Override
	public boolean exclusiveMinimum() {
		return exclusiveMinimum;
	}

	/**
	 * Sets the <c>exclusiveMinimum</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsAnnotation exclusiveMinimum(boolean value) {
		this.exclusiveMinimum = value;
		return this;
	}

	@Override
	public boolean uniqueItems() {
		return uniqueItems;
	}

	/**
	 * Sets the <c>uniqueItems</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsAnnotation uniqueItems(boolean value) {
		this.uniqueItems = value;
		return this;
	}

	@Override
	public String[] _default() {
		return _default;
	}

	/**
	 * Sets the <c>_default</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsAnnotation _default(String[] value) {
		this._default = value;
		return this;
	}

	@Override
	public String[] _enum() {
		return _enum;
	}

	/**
	 * Sets the <c>_enum</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsAnnotation _enum(String[] value) {
		this._enum = value;
		return this;
	}

	@Override
	public String $ref() {
		return $ref;
	}

	/**
	 * Sets the <c>$ref</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsAnnotation $ref(String value) {
		this.$ref = value;
		return this;
	}

	@Override
	public String[] value() {
		return value;
	}

	/**
	 * Sets the <c>value</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsAnnotation value(String[] value) {
		this.value = value;
		return this;
	}

	@Override
	public String[] items() {
		return items;
	}

	/**
	 * Sets the <c>items</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsAnnotation items(String[] value) {
		this.items = value;
		return this;
	}
}
