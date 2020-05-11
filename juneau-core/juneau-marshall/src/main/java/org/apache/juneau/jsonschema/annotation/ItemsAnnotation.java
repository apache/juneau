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
 * A concrete implementation of the {@link Items} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class ItemsAnnotation implements Items {

	private String
		type = "",
		t = "",
		format = "",
		f = "",
		collectionFormat = "",
		cf = "",
		pattern = "",
		p = "",
		maximum = "",
		max = "",
		minimum = "",
		min = "",
		multipleOf = "",
		mo = "",
		$ref = "";
	private String[]
		_default = new String[0],
		df = new String[0],
		_enum = new String[0],
		e = new String[0],
		value = new String[0];
	private long
		maxLength = -1,
		maxl = -1,
		minLength = -1,
		minl= -1,
		maxItems = -1,
		maxi = -1,
		minItems = -1,
		mini = -1;
	private boolean
		exclusiveMaximum = false,
		emax = false,
		exclusiveMinimum = false,
		emin = false,
		uniqueItems = false,
		ui = false;
	private SubItems items = new SubItemsAnnotation();

	@Override
	public Class<? extends Annotation> annotationType() {
		return Items.class;
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
	public ItemsAnnotation type(String value) {
		this.type = value;
		return this;
	}

	@Override
	public String t() {
		return t;
	}

	/**
	 * Sets the <c>t</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ItemsAnnotation t(String value) {
		this.t = value;
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
	public ItemsAnnotation format(String value) {
		this.format = value;
		return this;
	}

	@Override
	public String f() {
		return f;
	}

	/**
	 * Sets the <c>f</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ItemsAnnotation f(String value) {
		this.f = value;
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
	public ItemsAnnotation collectionFormat(String value) {
		this.collectionFormat = value;
		return this;
	}

	@Override
	public String cf() {
		return cf;
	}

	/**
	 * Sets the <c>cf</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ItemsAnnotation cf(String value) {
		this.cf = value;
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
	public ItemsAnnotation pattern(String value) {
		this.pattern = value;
		return this;
	}

	@Override
	public String p() {
		return p;
	}

	/**
	 * Sets the <c>p</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ItemsAnnotation p(String value) {
		this.p = value;
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
	public ItemsAnnotation maximum(String value) {
		this.maximum = value;
		return this;
	}

	@Override
	public String max() {
		return max;
	}

	/**
	 * Sets the <c>max</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ItemsAnnotation max(String value) {
		this.max = value;
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
	public ItemsAnnotation minimum(String value) {
		this.minimum = value;
		return this;
	}

	@Override
	public String min() {
		return min;
	}

	/**
	 * Sets the <c>min</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ItemsAnnotation min(String value) {
		this.min = value;
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
	public ItemsAnnotation multipleOf(String value) {
		this.multipleOf = value;
		return this;
	}

	@Override
	public String mo() {
		return mo;
	}

	/**
	 * Sets the <c>mo</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ItemsAnnotation mo(String value) {
		this.mo = value;
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
	public ItemsAnnotation maxLength(long value) {
		this.maxLength = value;
		return this;
	}

	@Override
	public long maxl() {
		return maxl;
	}

	/**
	 * Sets the <c>maxl</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ItemsAnnotation maxl(long value) {
		this.maxl = value;
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
	public ItemsAnnotation minLength(long value) {
		this.minLength = value;
		return this;
	}

	@Override
	public long minl() {
		return minl;
	}

	/**
	 * Sets the <c>minl</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ItemsAnnotation minl(long value) {
		this.minl = value;
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
	public ItemsAnnotation maxItems(long value) {
		this.maxItems = value;
		return this;
	}

	@Override
	public long maxi() {
		return maxi;
	}

	/**
	 * Sets the <c>maxi</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ItemsAnnotation maxi(long value) {
		this.maxi = value;
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
	public ItemsAnnotation minItems(long value) {
		this.minItems = value;
		return this;
	}

	@Override
	public long mini() {
		return mini;
	}

	/**
	 * Sets the <c>mini</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ItemsAnnotation mini(long value) {
		this.mini = value;
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
	public ItemsAnnotation exclusiveMaximum(boolean value) {
		this.exclusiveMaximum = value;
		return this;
	}

	@Override
	public boolean emax() {
		return emax;
	}

	/**
	 * Sets the <c>emax</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ItemsAnnotation emax(boolean value) {
		this.emax = value;
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
	public ItemsAnnotation exclusiveMinimum(boolean value) {
		this.exclusiveMinimum = value;
		return this;
	}

	@Override
	public boolean emin() {
		return emin;
	}

	/**
	 * Sets the <c>emin</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ItemsAnnotation emin(boolean value) {
		this.emin = value;
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
	public ItemsAnnotation uniqueItems(boolean value) {
		this.uniqueItems = value;
		return this;
	}

	@Override
	public boolean ui() {
		return ui;
	}

	/**
	 * Sets the <c>ui</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ItemsAnnotation ui(boolean value) {
		this.ui = value;
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
	public ItemsAnnotation _default(String[] value) {
		this._default = value;
		return this;
	}

	@Override
	public String[] df() {
		return df;
	}

	/**
	 * Sets the <c>df</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ItemsAnnotation df(String[] value) {
		this.df = value;
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
	public ItemsAnnotation _enum(String[] value) {
		this._enum = value;
		return this;
	}

	@Override
	public String[] e() {
		return e;
	}

	/**
	 * Sets the <c>e</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ItemsAnnotation e(String[] value) {
		this.e = value;
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
	public ItemsAnnotation $ref(String value) {
		this.$ref = value;
		return this;
	}

	@Override
	public SubItems items() {
		return items;
	}

	/**
	 * Sets the <c>items</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ItemsAnnotation items(SubItems value) {
		this.items = value;
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
	public ItemsAnnotation value(String[] value) {
		this.value = value;
		return this;
	}
}
