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
 * A concrete implementation of the {@link Header} annotation.
 */
public class HeaderAnnotation implements Header {

	private boolean skipIfEmpty, sie, multi, required, r, allowEmptyValue, aev, exclusiveMaximum, emax, exclusiveMinimum, emin, uniqueItems, ui;
	private Class<? extends HttpPartSerializer> serializer = HttpPartSerializer.Null.class;
	private Class<? extends HttpPartParser> parser = HttpPartParser.Null.class;
	private String name="", n="", value="", type="", t="", format="", f="", collectionFormat="", cf="", maximum="", max="", minimum="", min="", pattern="", p="", multipleOf="", mo="";
	private String[] description={}, d={}, _default={}, df={}, _enum={}, e={}, example={}, ex={}, api={};
	private Items items = new ItemsAnnotation();
	private long maxLength=-1, maxl=-1, minLength=-1, minl=-1, maxItems=-1, maxi=-1, minItems=-1, mini=-1;

	@Override /* Annotation */
	public Class<? extends Annotation> annotationType() {
		return Header.class;
	}

	@Override /* Header */
	public boolean skipIfEmpty() {
		return skipIfEmpty;
	}

	/**
	 * Sets the <c>skipIfEmpty</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation skipIfEmpty(boolean value) {
		this.skipIfEmpty = value;
		return this;
	}

	@Override /* Header */
	public boolean sie() {
		return sie;
	}

	/**
	 * Sets the <c>sie</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation sie(boolean value) {
		this.sie = value;
		return this;
	}

	@Override /* Header */
	public Class<? extends HttpPartSerializer> serializer() {
		return serializer;
	}

	/**
	 * Sets the <c>serializer</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation serializer(Class<? extends HttpPartSerializer> value) {
		this.serializer = value;
		return this;
	}

	@Override /* Header */
	public Class<? extends HttpPartParser> parser() {
		return parser;
	}

	/**
	 * Sets the <c>parser</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation parser(Class<? extends HttpPartParser> value) {
		this.parser = value;
		return this;
	}

	@Override /* Header */
	public boolean multi() {
		return multi;
	}

	/**
	 * Sets the <c>multi</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation multi(boolean value) {
		this.multi = value;
		return this;
	}

	@Override /* Header */
	public String name() {
		return name;
	}

	/**
	 * Sets the <c>name</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation name(String value) {
		this.name = value;
		return this;
	}

	@Override /* Header */
	public String n() {
		return n;
	}

	/**
	 * Sets the <c>n</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation n(String value) {
		this.n = value;
		return this;
	}

	@Override /* Header */
	public String value() {
		return value;
	}

	/**
	 * Sets the <c>value</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation value(String value) {
		this.value = value;
		return this;
	}

	@Override /* Header */
	public String[] description() {
		return description;
	}

	/**
	 * Sets the <c>description</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation description(String[] value) {
		this.description = value;
		return this;
	}

	@Override /* Header */
	public String[] d() {
		return d;
	}

	/**
	 * Sets the <c>d</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation d(String[] value) {
		this.d = value;
		return this;
	}

	@Override /* Header */
	public boolean required() {
		return required;
	}

	/**
	 * Sets the <c>required</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation required(boolean value) {
		this.required = value;
		return this;
	}

	@Override /* Header */
	public boolean r() {
		return r;
	}

	/**
	 * Sets the <c></c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation r(boolean value) {
		this.r = value;
		return this;
	}

	@Override /* Header */
	public String type() {
		return type;
	}

	/**
	 * Sets the <c>type</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation type(String value) {
		this.type = value;
		return this;
	}

	@Override /* Header */
	public String t() {
		return t;
	}

	/**
	 * Sets the <c>t</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation t(String value) {
		this.t = value;
		return this;
	}

	@Override /* Header */
	public String format() {
		return format;
	}

	/**
	 * Sets the <c>format</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation format(String value) {
		this.format = value;
		return this;
	}

	@Override /* Header */
	public String f() {
		return f;
	}

	/**
	 * Sets the <c>f</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation f(String value) {
		this.f = value;
		return this;
	}

	@Override /* Header */
	public boolean allowEmptyValue() {
		return allowEmptyValue;
	}

	/**
	 * Sets the <c>allowEmptyValue</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation allowEmptyValue(boolean value) {
		this.allowEmptyValue = value;
		return this;
	}

	@Override /* Header */
	public boolean aev() {
		return aev;
	}

	/**
	 * Sets the <c>aev</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation aev(boolean value) {
		this.aev = value;
		return this;
	}

	@Override /* Header */
	public Items items() {
		return items;
	}

	/**
	 * Sets the <c>items</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation items(Items value) {
		this.items = value;
		return this;
	}

	@Override /* Header */
	public String collectionFormat() {
		return collectionFormat;
	}

	/**
	 * Sets the <c>collectionFormat</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation collectionFormat(String value) {
		this.collectionFormat = value;
		return this;
	}

	@Override /* Header */
	public String cf() {
		return cf;
	}

	/**
	 * Sets the <c>cf</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation cf(String value) {
		this.cf = value;
		return this;
	}

	@Override /* Header */
	public String[] _default() {
		return _default;
	}

	/**
	 * Sets the <c>_default</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation _default(String[] value) {
		this._default = value;
		return this;
	}

	@Override /* Header */
	public String[] df() {
		return df;
	}

	/**
	 * Sets the <c>df</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation df(String[] value) {
		this.df = value;
		return this;
	}

	@Override /* Header */
	public String maximum() {
		return maximum;
	}

	/**
	 * Sets the <c>maximum</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation maximum(String value) {
		this.maximum = value;
		return this;
	}

	@Override /* Header */
	public String max() {
		return max;
	}

	/**
	 * Sets the <c>max</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation max(String value) {
		this.max = value;
		return this;
	}

	@Override /* Header */
	public boolean exclusiveMaximum() {
		return exclusiveMaximum;
	}

	/**
	 * Sets the <c>exclusiveMaximum</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation exclusiveMaximum(boolean value) {
		this.exclusiveMaximum = value;
		return this;
	}

	@Override /* Header */
	public boolean emax() {
		return emax;
	}

	/**
	 * Sets the <c>emax</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation emax(boolean value) {
		this.emax = value;
		return this;
	}

	@Override /* Header */
	public String minimum() {
		return minimum;
	}

	/**
	 * Sets the <c>minimum</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation minimum(String value) {
		this.minimum = value;
		return this;
	}

	@Override /* Header */
	public String min() {
		return min;
	}

	/**
	 * Sets the <c>min</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation min(String value) {
		this.min = value;
		return this;
	}

	@Override /* Header */
	public boolean exclusiveMinimum() {
		return exclusiveMinimum;
	}

	/**
	 * Sets the <c>exclusiveMinimum</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation exclusiveMinimum(boolean value) {
		this.exclusiveMinimum = value;
		return this;
	}

	@Override /* Header */
	public boolean emin() {
		return emin;
	}

	/**
	 * Sets the <c>emin</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation emin(boolean value) {
		this.emin = value;
		return this;
	}

	@Override /* Header */
	public long maxLength() {
		return maxLength;
	}

	/**
	 * Sets the <c>maxLength</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation maxLength(long value) {
		this.maxLength = value;
		return this;
	}

	@Override /* Header */
	public long maxl() {
		return maxl;
	}

	/**
	 * Sets the <c>maxl</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation maxl(long value) {
		this.maxl = value;
		return this;
	}

	@Override /* Header */
	public long minLength() {
		return minLength;
	}

	/**
	 * Sets the <c>minLength</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation minLength(long value) {
		this.minLength = value;
		return this;
	}

	@Override /* Header */
	public long minl() {
		return minl;
	}

	/**
	 * Sets the <c>minl</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation minl(long value) {
		this.minl = value;
		return this;
	}

	@Override /* Header */
	public String pattern() {
		return pattern;
	}

	/**
	 * Sets the <c>pattern</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation pattern(String value) {
		this.pattern = value;
		return this;
	}

	@Override /* Header */
	public String p() {
		return p;
	}

	/**
	 * Sets the <c>p</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation p(String value) {
		this.p = value;
		return this;
	}

	@Override /* Header */
	public long maxItems() {
		return maxItems;
	}

	/**
	 * Sets the <c>maxItems</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation maxItems(long value) {
		this.maxItems = value;
		return this;
	}

	@Override /* Header */
	public long maxi() {
		return maxi;
	}

	/**
	 * Sets the <c>maxi</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation maxi(long value) {
		this.maxi = value;
		return this;
	}

	@Override /* Header */
	public long minItems() {
		return minItems;
	}

	/**
	 * Sets the <c>minItems</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation minItems(long value) {
		this.minItems = value;
		return this;
	}

	@Override /* Header */
	public long mini() {
		return mini;
	}

	/**
	 * Sets the <c>mini</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation mini(long value) {
		this.mini = value;
		return this;
	}

	@Override /* Header */
	public boolean uniqueItems() {
		return uniqueItems;
	}

	/**
	 * Sets the <c>uniqueItems</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation uniqueItems(boolean value) {
		this.uniqueItems = value;
		return this;
	}

	@Override /* Header */
	public boolean ui() {
		return ui;
	}

	/**
	 * Sets the <c>ui</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation ui(boolean value) {
		this.ui = value;
		return this;
	}

	@Override /* Header */
	public String[] _enum() {
		return _enum;
	}

	/**
	 * Sets the <c>_enum</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation _enum(String[] value) {
		this._enum = value;
		return this;
	}

	@Override /* Header */
	public String[] e() {
		return e;
	}

	/**
	 * Sets the <c>e</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation e(String[] value) {
		this.e = value;
		return this;
	}

	@Override /* Header */
	public String multipleOf() {
		return multipleOf;
	}

	/**
	 * Sets the <c>multipleOf</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation multipleOf(String value) {
		this.multipleOf = value;
		return this;
	}

	@Override /* Header */
	public String mo() {
		return mo;
	}

	/**
	 * Sets the <c>mo</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation mo(String value) {
		this.mo = value;
		return this;
	}

	@Override /* Header */
	public String[] example() {
		return example;
	}

	/**
	 * Sets the <c>example</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation example(String[]  value) {
		this.example = value;
		return this;
	}

	@Override /* Header */
	public String[] ex() {
		return ex;
	}

	/**
	 * Sets the <c>ex</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation ex(String[]  value) {
		this.ex = value;
		return this;
	}

	@Override /* Header */
	public String[] api() {
		return api;
	}

	/**
	 * Sets the <c>api</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderAnnotation api(String[]  value) {
		this.api = value;
		return this;
	}
}
