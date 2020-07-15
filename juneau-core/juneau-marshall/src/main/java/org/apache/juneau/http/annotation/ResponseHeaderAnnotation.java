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
 * A concrete implementation of the {@link ResponseHeader} annotation.
 */
public class ResponseHeaderAnnotation implements ResponseHeader {

	private int[] code={};
	private String name="", n="", value="", type="", t="", format="", f="", collectionFormat="", cf="", $ref="", maximum="", max="", minimum="", min="", multipleOf="", mo="", pattern="", p="";
	private Class<? extends HttpPartSerializer> serializer = HttpPartSerializer.Null.class;
	private String[] description={}, d={}, _default={}, df={}, _enum={}, e={}, example={}, ex={}, api={};
	private long maxLength=-1, maxl=-1, minLength=-1, minl=-1, maxItems=-1, maxi=-1, minItems=-1, mini=-1;
	private boolean exclusiveMaximum, emax, exclusiveMinimum, emin, uniqueItems, ui;
	private Items items = new ItemsAnnotation();

	@Override /* ResponseHeader */ /* Annotation */
	public Class<? extends Annotation> annotationType() {
		return ResponseHeader.class;
	}

	@Override /* ResponseHeader */
	public int[] code() {
		return code;
	}

	/**
	 * Sets the <c>code</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation code(int[] value) {
		this.code = value;
		return this;
	}

	@Override /* ResponseHeader */
	public String name() {
		return name;
	}

	/**
	 * Sets the <c>name</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation name(String value) {
		this.name = value;
		return this;
	}

	@Override /* ResponseHeader */
	public String n() {
		return n;
	}

	/**
	 * Sets the <c>n</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation n(String value) {
		this.n = value;
		return this;
	}

	@Override /* ResponseHeader */
	public String value() {
		return value;
	}

	/**
	 * Sets the <c>value</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation value(String value) {
		this.value = value;
		return this;
	}

	@Override /* ResponseHeader */
	public Class<? extends HttpPartSerializer> serializer() {
		return serializer;
	}

	/**
	 * Sets the <c>serializer</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation serializer(Class<? extends HttpPartSerializer> value) {
		this.serializer = value;
		return this;
	}

	@Override /* ResponseHeader */
	public String[] description() {
		return description;
	}

	/**
	 * Sets the <c>description</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation description(String[] value) {
		this.description = value;
		return this;
	}

	@Override /* ResponseHeader */
	public String[] d() {
		return d;
	}

	/**
	 * Sets the <c>d</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation d(String[] value) {
		this.d = value;
		return this;
	}

	@Override /* ResponseHeader */
	public String type() {
		return type;
	}

	/**
	 * Sets the <c>type</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation type(String value) {
		this.type = value;
		return this;
	}

	@Override /* ResponseHeader */
	public String t() {
		return t;
	}

	/**
	 * Sets the <c>t</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation t(String value) {
		this.t = value;
		return this;
	}

	@Override /* ResponseHeader */
	public String format() {
		return format;
	}

	/**
	 * Sets the <c>format</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation format(String value) {
		this.format = value;
		return this;
	}

	@Override /* ResponseHeader */
	public String f() {
		return f;
	}

	/**
	 * Sets the <c>f</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation f(String value) {
		this.f = value;
		return this;
	}

	@Override /* ResponseHeader */
	public String collectionFormat() {
		return collectionFormat;
	}

	/**
	 * Sets the <c>collectionFormat</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation collectionFormat(String value) {
		this.collectionFormat = value;
		return this;
	}

	@Override /* ResponseHeader */
	public String cf() {
		return cf;
	}

	/**
	 * Sets the <c>cf</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation cf(String value) {
		this.cf = value;
		return this;
	}

	@Override /* ResponseHeader */
	public String $ref() {
		return $ref;
	}

	/**
	 * Sets the <c>$ref</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation $ref(String value) {
		this.$ref = value;
		return this;
	}

	@Override /* ResponseHeader */
	public String maximum() {
		return maximum;
	}

	/**
	 * Sets the <c>maximum</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation maximum(String value) {
		this.maximum = value;
		return this;
	}

	@Override /* ResponseHeader */
	public String max() {
		return max;
	}

	/**
	 * Sets the <c>max</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation max(String value) {
		this.max = value;
		return this;
	}

	@Override /* ResponseHeader */
	public String minimum() {
		return minimum;
	}

	/**
	 * Sets the <c>minimum</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation minimum(String value) {
		this.minimum = value;
		return this;
	}

	@Override /* ResponseHeader */
	public String min() {
		return min;
	}

	/**
	 * Sets the <c>min</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation min(String value) {
		this.min = value;
		return this;
	}

	@Override /* ResponseHeader */
	public String multipleOf() {
		return multipleOf;
	}

	/**
	 * Sets the <c>multipleOf</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation multipleOf(String value) {
		this.multipleOf = value;
		return this;
	}

	@Override /* ResponseHeader */
	public String mo() {
		return mo;
	}

	/**
	 * Sets the <c>mo</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation mo(String value) {
		this.mo = value;
		return this;
	}

	@Override /* ResponseHeader */
	public long maxLength() {
		return maxLength;
	}

	/**
	 * Sets the <c>maxLength</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation maxLength(long value) {
		this.maxLength = value;
		return this;
	}

	@Override /* ResponseHeader */
	public long maxl() {
		return maxl;
	}

	/**
	 * Sets the <c>maxl</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation maxl(long value) {
		this.maxl = value;
		return this;
	}

	@Override /* ResponseHeader */
	public long minLength() {
		return minLength;
	}

	/**
	 * Sets the <c>minLength</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation minLength(long value) {
		this.minLength = value;
		return this;
	}

	@Override /* ResponseHeader */
	public long minl() {
		return minl;
	}

	/**
	 * Sets the <c>minl</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation minl(long value) {
		this.minl = value;
		return this;
	}

	@Override /* ResponseHeader */
	public String pattern() {
		return pattern;
	}

	/**
	 * Sets the <c>pattern</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation pattern(String value) {
		this.pattern = value;
		return this;
	}

	@Override /* ResponseHeader */
	public String p() {
		return p;
	}

	/**
	 * Sets the <c>p</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation p(String value) {
		this.p = value;
		return this;
	}

	@Override /* ResponseHeader */
	public long maxItems() {
		return maxItems;
	}

	/**
	 * Sets the <c>maxItems</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation maxItems(long value) {
		this.maxItems = value;
		return this;
	}

	@Override /* ResponseHeader */
	public long maxi() {
		return maxi;
	}

	/**
	 * Sets the <c>maxi</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation maxi(long value) {
		this.maxi = value;
		return this;
	}

	@Override /* ResponseHeader */
	public long minItems() {
		return minItems;
	}

	/**
	 * Sets the <c>minItems</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation minItems(long value) {
		this.minItems = value;
		return this;
	}

	@Override /* ResponseHeader */
	public long mini() {
		return mini;
	}

	/**
	 * Sets the <c>mini</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation mini(long value) {
		this.mini = value;
		return this;
	}

	@Override /* ResponseHeader */
	public boolean exclusiveMaximum() {
		return exclusiveMaximum;
	}

	/**
	 * Sets the <c>exclusiveMaximum</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation exclusiveMaximum(boolean value) {
		this.exclusiveMaximum = value;
		return this;
	}

	@Override /* ResponseHeader */
	public boolean emax() {
		return emax;
	}

	/**
	 * Sets the <c>emax</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation emax(boolean value) {
		this.emax = value;
		return this;
	}

	@Override /* ResponseHeader */
	public boolean exclusiveMinimum() {
		return exclusiveMinimum;
	}

	/**
	 * Sets the <c>exclusiveMinimum</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation exclusiveMinimum(boolean value) {
		this.exclusiveMinimum = value;
		return this;
	}

	@Override /* ResponseHeader */
	public boolean emin() {
		return emin;
	}

	/**
	 * Sets the <c>emin</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation emin(boolean value) {
		this.emin = value;
		return this;
	}

	@Override /* ResponseHeader */
	public boolean uniqueItems() {
		return uniqueItems;
	}

	/**
	 * Sets the <c>uniqueItems</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation uniqueItems(boolean value) {
		this.uniqueItems = value;
		return this;
	}

	@Override /* ResponseHeader */
	public boolean ui() {
		return ui;
	}

	/**
	 * Sets the <c>ui</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation ui(boolean value) {
		this.ui = value;
		return this;
	}

	@Override /* ResponseHeader */
	public Items items() {
		return items;
	}

	/**
	 * Sets the <c>items</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation items(Items value) {
		this.items = value;
		return this;
	}

	@Override /* ResponseHeader */
	public String[] _default() {
		return _default;
	}

	/**
	 * Sets the <c>_default</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation _default(String[] value) {
		this._default = value;
		return this;
	}

	@Override /* ResponseHeader */
	public String[] df() {
		return df;
	}

	/**
	 * Sets the <c>df</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation df(String[] value) {
		this.df = value;
		return this;
	}

	@Override /* ResponseHeader */
	public String[] _enum() {
		return _enum;
	}

	/**
	 * Sets the <c>_enum</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation _enum(String[] value) {
		this._enum = value;
		return this;
	}

	@Override /* ResponseHeader */
	public String[] e() {
		return e;
	}

	/**
	 * Sets the <c>e</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation e(String[] value) {
		this.e = value;
		return this;
	}

	@Override /* ResponseHeader */
	public String[] example() {
		return example;
	}

	/**
	 * Sets the <c>example</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation example(String[] value) {
		this.example = value;
		return this;
	}

	@Override /* ResponseHeader */
	public String[] ex() {
		return ex;
	}

	/**
	 * Sets the <c>ex</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation ex(String[] value) {
		this.ex = value;
		return this;
	}

	@Override /* ResponseHeader */
	public String[] api() {
		return api;
	}

	/**
	 * Sets the <c>api</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderAnnotation api(String[] value) {
		this.api = value;
		return this;
	}
}
