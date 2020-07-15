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
 * A concrete implementation of the {@link Path} annotation.
 */
public class PathAnnotation implements Path {

	private Class<? extends HttpPartSerializer> serializer = HttpPartSerializer.Null.class;
	private Class<? extends HttpPartParser> parser = HttpPartParser.Null.class;
	private String name="", n="", value="", type="", t="", format="", f="", collectionFormat="", cf="", maximum="", max="", minimum="", min="", pattern="", p="", multipleOf="", mo="";
	private String[] description={}, d={}, _enum={}, e={}, example={}, ex={}, api={};
	private boolean allowEmptyValue, aev,exclusiveMaximum, emax, exclusiveMinimum, emin, uniqueItems, ui;
	private boolean required=true, r=true;
	private Items items = new ItemsAnnotation();
	private long maxLength=-1, maxl=-1, minLength=-1, minl=-1, maxItems=-1, maxi=-1, minItems=-1, mini=-1;

	@Override /* Path */ /* Annotation */
	public Class<? extends Annotation> annotationType() {
		return Path.class;
	}

	@Override /* Path */ /* Path */
	public Class<? extends HttpPartSerializer> serializer() {
		return serializer;
	}

	/**
	 * Sets the <c>serializer</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation serializer(Class<? extends HttpPartSerializer> value) {
		this.serializer = value;
		return this;
	}

	@Override /* Path */
	public Class<? extends HttpPartParser> parser() {
		return parser;
	}

	/**
	 * Sets the <c>parser</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation parser(Class<? extends HttpPartParser> value) {
		this.parser = value;
		return this;
	}

	@Override /* Path */
	public String name() {
		return name;
	}

	/**
	 * Sets the <c>name</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation name(String value) {
		this.name = value;
		return this;
	}

	@Override /* Path */
	public String n() {
		return n;
	}

	/**
	 * Sets the <c>n</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation n(String value) {
		this.n = value;
		return this;
	}

	@Override /* Path */
	public String value() {
		return value;
	}

	/**
	 * Sets the <c>value</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation value(String value) {
		this.value = value;
		return this;
	}

	@Override /* Path */
	public String[] description() {
		return description;
	}

	/**
	 * Sets the <c>description</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation description(String[] value) {
		this.description = value;
		return this;
	}

	@Override /* Path */
	public String[] d() {
		return d;
	}

	/**
	 * Sets the <c>d</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation d(String[] value) {
		this.d = value;
		return this;
	}

	@Override /* Path */
	public boolean required() {
		return required;
	}

	/**
	 * Sets the <c>required</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation required(boolean value) {
		this.required = value;
		return this;
	}

	@Override /* Path */
	public boolean r() {
		return r;
	}

	/**
	 * Sets the <c>r</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation r(boolean value) {
		this.r = value;
		return this;
	}

	@Override /* Path */
	public String type() {
		return type;
	}

	/**
	 * Sets the <c>type</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation type(String value) {
		this.type = value;
		return this;
	}

	@Override /* Path */
	public String t() {
		return t;
	}

	/**
	 * Sets the <c>t</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation t(String value) {
		this.t = value;
		return this;
	}

	@Override /* Path */
	public String format() {
		return format;
	}

	/**
	 * Sets the <c>format</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation format(String value) {
		this.format = value;
		return this;
	}

	@Override /* Path */
	public String f() {
		return f;
	}

	/**
	 * Sets the <c>f</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation f(String value) {
		this.f = value;
		return this;
	}

	@Override /* Path */
	public boolean allowEmptyValue() {
		return allowEmptyValue;
	}

	/**
	 * Sets the <c>allowEmptyValue</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation allowEmptyValue(boolean value) {
		this.allowEmptyValue = value;
		return this;
	}

	@Override /* Path */
	public boolean aev() {
		return aev;
	}

	/**
	 * Sets the <c>aev</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation aev(boolean value) {
		this.aev = value;
		return this;
	}

	@Override /* Path */
	public Items items() {
		return items;
	}

	/**
	 * Sets the <c>items</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation items(Items value) {
		this.items = value;
		return this;
	}

	@Override /* Path */
	public String collectionFormat() {
		return collectionFormat;
	}

	/**
	 * Sets the <c>collectionFormat</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation collectionFormat(String value) {
		this.collectionFormat = value;
		return this;
	}

	@Override /* Path */
	public String cf() {
		return cf;
	}

	/**
	 * Sets the <c>cf</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation cf(String value) {
		this.cf = value;
		return this;
	}

	@Override /* Path */
	public String maximum() {
		return maximum;
	}

	/**
	 * Sets the <c>maximum</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation maximum(String value) {
		this.maximum = value;
		return this;
	}

	@Override /* Path */
	public String max() {
		return max;
	}

	/**
	 * Sets the <c>max</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation max(String value) {
		this.max = value;
		return this;
	}

	@Override /* Path */
	public boolean exclusiveMaximum() {
		return exclusiveMaximum;
	}

	/**
	 * Sets the <c>exclusiveMaximum</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation exclusiveMaximum(boolean value) {
		this.exclusiveMaximum = value;
		return this;
	}

	@Override /* Path */
	public boolean emax() {
		return emax;
	}

	/**
	 * Sets the <c>emax</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation emax(boolean value) {
		this.emax = value;
		return this;
	}

	@Override /* Path */
	public String minimum() {
		return minimum;
	}

	/**
	 * Sets the <c>minimum</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation minimum(String value) {
		this.minimum = value;
		return this;
	}

	@Override /* Path */
	public String min() {
		return min;
	}

	/**
	 * Sets the <c>min</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation min(String value) {
		this.min = value;
		return this;
	}

	@Override /* Path */
	public boolean exclusiveMinimum() {
		return exclusiveMinimum;
	}

	/**
	 * Sets the <c>exclusiveMinimum</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation exclusiveMinimum(boolean value) {
		this.exclusiveMinimum = value;
		return this;
	}

	@Override /* Path */
	public boolean emin() {
		return emin;
	}

	/**
	 * Sets the <c>emin</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation emin(boolean value) {
		this.emin = value;
		return this;
	}

	@Override /* Path */
	public long maxLength() {
		return maxLength;
	}

	/**
	 * Sets the <c>maxLength</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation maxLength(long value) {
		this.maxLength = value;
		return this;
	}

	@Override /* Path */
	public long maxl() {
		return maxl;
	}

	/**
	 * Sets the <c>maxl</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation maxl(long value) {
		this.maxl = value;
		return this;
	}

	@Override /* Path */
	public long minLength() {
		return minLength;
	}

	/**
	 * Sets the <c>minLength</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation minLength(long value) {
		this.minLength = value;
		return this;
	}

	@Override /* Path */
	public long minl() {
		return minl;
	}

	/**
	 * Sets the <c>minl</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation minl(long value) {
		this.minl = value;
		return this;
	}

	@Override /* Path */
	public String pattern() {
		return pattern;
	}

	/**
	 * Sets the <c>pattern</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation pattern(String value) {
		this.pattern = value;
		return this;
	}

	@Override /* Path */
	public String p() {
		return p;
	}

	/**
	 * Sets the <c>p</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation p(String value) {
		this.p = value;
		return this;
	}

	@Override /* Path */
	public long maxItems() {
		return maxItems;
	}

	/**
	 * Sets the <c>maxItems</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation maxItems(long value) {
		this.maxItems = value;
		return this;
	}

	@Override /* Path */
	public long maxi() {
		return maxi;
	}

	/**
	 * Sets the <c>maxi</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation maxi(long value) {
		this.maxi = value;
		return this;
	}

	@Override /* Path */
	public long minItems() {
		return minItems;
	}

	/**
	 * Sets the <c>minItems</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation minItems(long value) {
		this.minItems = value;
		return this;
	}

	@Override /* Path */
	public long mini() {
		return mini;
	}

	/**
	 * Sets the <c>mini</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation mini(long value) {
		this.mini = value;
		return this;
	}

	@Override /* Path */
	public boolean uniqueItems() {
		return uniqueItems;
	}

	/**
	 * Sets the <c>uniqueItems</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation uniqueItems(boolean value) {
		this.uniqueItems = value;
		return this;
	}

	@Override /* Path */
	public boolean ui() {
		return ui;
	}

	/**
	 * Sets the <c>ui</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation ui(boolean value) {
		this.ui = value;
		return this;
	}

	@Override /* Path */
	public String[] _enum() {
		return _enum;
	}

	/**
	 * Sets the <c>_enum</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation _enum( String[] value) {
		this._enum = value;
		return this;
	}

	@Override /* Path */
	public String[] e() {
		return e;
	}

	/**
	 * Sets the <c>e</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation e( String[] value) {
		this.e = value;
		return this;
	}

	@Override /* Path */
	public String multipleOf() {
		return multipleOf;
	}

	/**
	 * Sets the <c>multipleOf</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation multipleOf(String value) {
		this.multipleOf = value;
		return this;
	}

	@Override /* Path */
	public String mo() {
		return mo;
	}

	/**
	 * Sets the <c>mo</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation mo(String value) {
		this.mo = value;
		return this;
	}

	@Override /* Path */
	public String[] example() {
		return example;
	}

	/**
	 * Sets the <c>example</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation example(String[] value) {
		this.example = value;
		return this;
	}

	@Override /* Path */
	public String[] ex() {
		return ex;
	}

	/**
	 * Sets the <c>ex</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation ex(String[] value) {
		this.ex = value;
		return this;
	}

	@Override /* Path */
	public String[] api() {
		return api;
	}

	/**
	 * Sets the <c>api</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathAnnotation api(String[] value) {
		this.api = value;
		return this;
	}
}
