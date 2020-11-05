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

import static org.apache.juneau.internal.ArrayUtils.*;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;

/**
 * Builder class for the {@link SubItems} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class SubItemsBuilder extends AnnotationBuilder {

	/** Default value */
	public static final SubItems DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static SubItemsBuilder create() {
		return new SubItemsBuilder();
	}

	private static class Impl extends AnnotationImpl implements SubItems {

		private final boolean emax, emin, exclusiveMaximum, exclusiveMinimum, ui, uniqueItems;
		private final long maxi, maxItems, maxl, maxLength, mini, minItems, minl, minLength;
		private final String $ref, cf, collectionFormat, f, format, max, maximum, min, minimum, mo, multipleOf, p, pattern, t, type;
		private final String[] _default, _enum, df, e, items, value;

		Impl(SubItemsBuilder b) {
			super(b);
			this.$ref = b.$ref;
			this._default = copyOf(b._default);
			this._enum = copyOf(b._enum);
			this.cf = b.cf;
			this.collectionFormat = b.collectionFormat;
			this.df = copyOf(b.df);
			this.e = copyOf(b.e);
			this.emax = b.emax;
			this.emin = b.emin;
			this.exclusiveMaximum = b.exclusiveMaximum;
			this.exclusiveMinimum = b.exclusiveMinimum;
			this.f = b.f;
			this.format = b.format;
			this.items = copyOf(b.items);
			this.max = b.max;
			this.maxi = b.maxi;
			this.maximum = b.maximum;
			this.maxItems = b.maxItems;
			this.maxl = b.maxl;
			this.maxLength = b.maxLength;
			this.min = b.min;
			this.mini = b.mini;
			this.minimum = b.minimum;
			this.minItems = b.minItems;
			this.minl = b.minl;
			this.minLength = b.minLength;
			this.mo = b.mo;
			this.multipleOf = b.multipleOf;
			this.p = b.p;
			this.pattern = b.pattern;
			this.t = b.t;
			this.type = b.type;
			this.ui = b.ui;
			this.uniqueItems = b.uniqueItems;
			this.value = copyOf(b.value);
			postConstruct();
		}

		@Override /* SubItems */
		public String[] _default() {
			return _default;
		}

		@Override /* SubItems */
		public String[] _enum() {
			return _enum;
		}

		@Override /* SubItems */
		public String $ref() {
			return $ref;
		}

		@Override /* SubItems */
		public String cf() {
			return cf;
		}

		@Override /* SubItems */
		public String collectionFormat() {
			return collectionFormat;
		}

		@Override /* SubItems */
		public String[] df() {
			return df;
		}

		@Override /* SubItems */
		public String[] e() {
			return e;
		}

		@Override /* SubItems */
		public boolean emax() {
			return emax;
		}

		@Override /* SubItems */
		public boolean emin() {
			return emin;
		}

		@Override /* SubItems */
		public boolean exclusiveMaximum() {
			return exclusiveMaximum;
		}

		@Override /* SubItems */
		public boolean exclusiveMinimum() {
			return exclusiveMinimum;
		}

		@Override /* SubItems */
		public String f() {
			return f;
		}

		@Override /* SubItems */
		public String format() {
			return format;
		}

		@Override /* SubItems */
		public String[] items() {
			return items;
		}

		@Override /* SubItems */
		public String max() {
			return max;
		}

		@Override /* SubItems */
		public long maxi() {
			return maxi;
		}

		@Override /* SubItems */
		public String maximum() {
			return maximum;
		}

		@Override /* SubItems */
		public long maxItems() {
			return maxItems;
		}

		@Override /* SubItems */
		public long maxl() {
			return maxl;
		}

		@Override /* SubItems */
		public long maxLength() {
			return maxLength;
		}

		@Override /* SubItems */
		public String min() {
			return min;
		}

		@Override /* SubItems */
		public long mini() {
			return mini;
		}

		@Override /* SubItems */
		public String minimum() {
			return minimum;
		}

		@Override /* SubItems */
		public long minItems() {
			return minItems;
		}

		@Override /* SubItems */
		public long minl() {
			return minl;
		}

		@Override /* SubItems */
		public long minLength() {
			return minLength;
		}

		@Override /* SubItems */
		public String mo() {
			return mo;
		}

		@Override /* SubItems */
		public String multipleOf() {
			return multipleOf;
		}

		@Override /* SubItems */
		public String p() {
			return p;
		}

		@Override /* SubItems */
		public String pattern() {
			return pattern;
		}

		@Override /* SubItems */
		public String t() {
			return t;
		}

		@Override /* SubItems */
		public String type() {
			return type;
		}

		@Override /* SubItems */
		public boolean ui() {
			return ui;
		}

		@Override /* SubItems */
		public boolean uniqueItems() {
			return uniqueItems;
		}

		@Override /* SubItems */
		public String[] value() {
			return value;
		}
	}


	String $ref="", cf="", collectionFormat="", f="", format="", max="", maximum="", min="", minimum="", mo="", multipleOf="", p="", pattern="", t="", type="";
	long maxItems=-1, maxLength=-1, maxi=-1, maxl=-1, minItems=-1, minLength=-1, mini=-1, minl=-1;
	boolean emax, emin, exclusiveMaximum, exclusiveMinimum, ui, uniqueItems;
	String[] _default={}, _enum={}, df={}, e={}, items={}, value={};

	/**
	 * Constructor.
	 */
	public SubItemsBuilder() {
		super(SubItems.class);
	}

	/**
	 * Instantiates a new {@link SubItems @SubItems} object initialized with this builder.
	 *
	 * @return A new {@link SubItems @SubItems} object.
	 */
	public SubItems build() {
		return new Impl(this);
	}

	/**
	 * Sets the <c>_default</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder _default(String...value) {
		this._default = value;
		return this;
	}

	/**
	 * Sets the <c>_enum</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder _enum(String...value) {
		this._enum = value;
		return this;
	}

	/**
	 * Sets the <c>$ref</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder $ref(String value) {
		this.$ref = value;
		return this;
	}

	/**
	 * Sets the <c>cf</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder cf(String value) {
		this.cf = value;
		return this;
	}

	/**
	 * Sets the <c>collectionFormat</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder collectionFormat(String value) {
		this.collectionFormat = value;
		return this;
	}

	/**
	 * Sets the <c>df</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder df(String...value) {
		this.df = value;
		return this;
	}

	/**
	 * Sets the <c>e</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder e(String...value) {
		this.e = value;
		return this;
	}

	/**
	 * Sets the <c>emax</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder emax(boolean value) {
		this.emax = value;
		return this;
	}

	/**
	 * Sets the <c>emin</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder emin(boolean value) {
		this.emin = value;
		return this;
	}

	/**
	 * Sets the <c>exclusiveMaximum</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder exclusiveMaximum(boolean value) {
		this.exclusiveMaximum = value;
		return this;
	}

	/**
	 * Sets the <c>exclusiveMinimum</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder exclusiveMinimum(boolean value) {
		this.exclusiveMinimum = value;
		return this;
	}

	/**
	 * Sets the <c>f</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder f(String value) {
		this.f = value;
		return this;
	}

	/**
	 * Sets the <c>format</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder format(String value) {
		this.format = value;
		return this;
	}

	/**
	 * Sets the <c>items</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder items(String...value) {
		this.items = value;
		return this;
	}

	/**
	 * Sets the <c>max</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder max(String value) {
		this.max = value;
		return this;
	}

	/**
	 * Sets the <c>maxi</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder maxi(long value) {
		this.maxi = value;
		return this;
	}

	/**
	 * Sets the <c>maximum</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder maximum(String value) {
		this.maximum = value;
		return this;
	}

	/**
	 * Sets the <c>maxItems</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder maxItems(long value) {
		this.maxItems = value;
		return this;
	}

	/**
	 * Sets the <c>maxl</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder maxl(long value) {
		this.maxl = value;
		return this;
	}

	/**
	 * Sets the <c>maxLength</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder maxLength(long value) {
		this.maxLength = value;
		return this;
	}

	/**
	 * Sets the <c>min</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder min(String value) {
		this.min = value;
		return this;
	}

	/**
	 * Sets the <c>mini</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder mini(long value) {
		this.mini = value;
		return this;
	}

	/**
	 * Sets the <c>minimum</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder minimum(String value) {
		this.minimum = value;
		return this;
	}

	/**
	 * Sets the <c>minItems</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder minItems(long value) {
		this.minItems = value;
		return this;
	}

	/**
	 * Sets the <c>minl</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder minl(long value) {
		this.minl = value;
		return this;
	}

	/**
	 * Sets the <c>minLength</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder minLength(long value) {
		this.minLength = value;
		return this;
	}

	/**
	 * Sets the <c>mo</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder mo(String value) {
		this.mo = value;
		return this;
	}

	/**
	 * Sets the <c>multipleOf</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder multipleOf(String value) {
		this.multipleOf = value;
		return this;
	}

	/**
	 * Sets the <c>p</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder p(String value) {
		this.p = value;
		return this;
	}

	/**
	 * Sets the <c>pattern</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder pattern(String value) {
		this.pattern = value;
		return this;
	}

	/**
	 * Sets the <c>t</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder t(String value) {
		this.t = value;
		return this;
	}

	/**
	 * Sets the <c>type</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder type(String value) {
		this.type = value;
		return this;
	}

	/**
	 * Sets the <c>ui</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder ui(boolean value) {
		this.ui = value;
		return this;
	}

	/**
	 * Sets the <c>uniqueItems</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder uniqueItems(boolean value) {
		this.uniqueItems = value;
		return this;
	}

	/**
	 * Sets the <c>value</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SubItemsBuilder value(String...value) {
		this.value = value;
		return this;
	}

	// <FluentSetters>
	// </FluentSetters>
}
