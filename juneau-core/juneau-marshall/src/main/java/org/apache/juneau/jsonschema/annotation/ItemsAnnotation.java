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
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link Items @Items} annotation.
 */
public class ItemsAnnotation {

	/** Default value */
	public static final Items DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Creates a copy of the specified annotation.
	 *
	 * @param a The annotation to copy.
	 * @param r The var resolver for resolving any variables.
	 * @return A copy of the specified annotation.
	 */
	public static Items copy(Items a, VarResolverSession r) {
		return
			create()
			._default(r.resolve(a._default()))
			._enum(r.resolve(a._enum()))
			.$ref(r.resolve(a.$ref()))
			.cf(r.resolve(a.cf()))
			.collectionFormat(r.resolve(a.collectionFormat()))
			.df(r.resolve(a.df()))
			.e(r.resolve(a.e()))
			.emax(a.emax())
			.emin(a.emin())
			.exclusiveMaximum(a.exclusiveMaximum())
			.exclusiveMinimum(a.exclusiveMinimum())
			.f(r.resolve(a.f()))
			.format(r.resolve(a.format()))
			.items(SubItemsAnnotation.copy(a.items(), r))
			.max(r.resolve(a.max()))
			.maxi(a.maxi())
			.maximum(r.resolve(a.maximum()))
			.maxItems(a.maxItems())
			.maxl(a.maxLength())
			.maxLength(a.maxLength())
			.min(r.resolve(a.min()))
			.mini(a.mini())
			.minimum(r.resolve(a.minimum()))
			.minItems(a.minItems())
			.minl(a.minl())
			.minLength(a.minLength())
			.mo(r.resolve(a.mo()))
			.multipleOf(r.resolve(a.mo()))
			.p(r.resolve(a.p()))
			.pattern(r.resolve(a.pattern()))
			.t(r.resolve(a.t()))
			.type(r.resolve(a.type()))
			.ui(a.ui())
			.uniqueItems(a.uniqueItems())
			.value(r.resolve(a.value()))
			.build();
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(org.apache.juneau.jsonschema.annotation.Items a) {
		return a == null || DEFAULT.equals(a);
	}

	/**
	 * Builder class for the {@link Items} annotation.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends AnnotationBuilder {

		boolean emax, emin, exclusiveMaximum, exclusiveMinimum, ui, uniqueItems;
		long maxItems=-1, maxLength=-1, maxi=-1, maxl=-1, minItems=-1, minLength=-1, mini=-1, minl=-1;
		String $ref="", cf="", collectionFormat="", f="", format="", max="", maximum="", min="", minimum="", mo="", multipleOf="", p="", pattern="", t="", type="";
		String[] _default={}, _enum={}, df={}, e={}, value={};
		SubItems items = SubItemsAnnotation.DEFAULT;

		/**
		 * Constructor.
		 */
		public Builder() {
			super(Items.class);
		}

		/**
		 * Instantiates a new {@link Items @Items} object initialized with this builder.
		 *
		 * @return A new {@link Items @Items} object.
		 */
		public Items build() {
			return new Impl(this);
		}

		/**
		 * Sets the {@link Items#_default} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder _default(String...value) {
			this._default = value;
			return this;
		}

		/**
		 * Sets the {@link Items#_enum} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder _enum(String...value) {
			this._enum = value;
			return this;
		}

		/**
		 * Sets the {@link Items#$ref} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder $ref(String value) {
			this.$ref = value;
			return this;
		}

		/**
		 * Sets the {@link Items#cf} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder cf(String value) {
			this.cf = value;
			return this;
		}

		/**
		 * Sets the {@link Items#collectionFormat} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder collectionFormat(String value) {
			this.collectionFormat = value;
			return this;
		}

		/**
		 * Sets the {@link Items#df} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder df(String...value) {
			this.df = value;
			return this;
		}

		/**
		 * Sets the {@link Items#e} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder e(String...value) {
			this.e = value;
			return this;
		}

		/**
		 * Sets the {@link Items#emax} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder emax(boolean value) {
			this.emax = value;
			return this;
		}

		/**
		 * Sets the {@link Items#emin} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder emin(boolean value) {
			this.emin = value;
			return this;
		}

		/**
		 * Sets the {@link Items#exclusiveMaximum} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder exclusiveMaximum(boolean value) {
			this.exclusiveMaximum = value;
			return this;
		}

		/**
		 * Sets the {@link Items#exclusiveMinimum} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder exclusiveMinimum(boolean value) {
			this.exclusiveMinimum = value;
			return this;
		}

		/**
		 * Sets the {@link Items#f} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder f(String value) {
			this.f = value;
			return this;
		}

		/**
		 * Sets the {@link Items#format} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder format(String value) {
			this.format = value;
			return this;
		}

		/**
		 * Sets the {@link Items#items} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder items(SubItems value) {
			this.items = value;
			return this;
		}

		/**
		 * Sets the {@link Items#max} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder max(String value) {
			this.max = value;
			return this;
		}

		/**
		 * Sets the {@link Items#maxi} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder maxi(long value) {
			this.maxi = value;
			return this;
		}

		/**
		 * Sets the {@link Items#maximum} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder maximum(String value) {
			this.maximum = value;
			return this;
		}

		/**
		 * Sets the {@link Items#maxItems} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder maxItems(long value) {
			this.maxItems = value;
			return this;
		}

		/**
		 * Sets the {@link Items#maxl} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder maxl(long value) {
			this.maxl = value;
			return this;
		}

		/**
		 * Sets the {@link Items#maxLength} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder maxLength(long value) {
			this.maxLength = value;
			return this;
		}

		/**
		 * Sets the {@link Items#min} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder min(String value) {
			this.min = value;
			return this;
		}

		/**
		 * Sets the {@link Items#mini} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder mini(long value) {
			this.mini = value;
			return this;
		}

		/**
		 * Sets the {@link Items#minimum} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder minimum(String value) {
			this.minimum = value;
			return this;
		}

		/**
		 * Sets the {@link Items#minItems} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder minItems(long value) {
			this.minItems = value;
			return this;
		}

		/**
		 * Sets the {@link Items#minl} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder minl(long value) {
			this.minl = value;
			return this;
		}

		/**
		 * Sets the {@link Items#minLength} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder minLength(long value) {
			this.minLength = value;
			return this;
		}

		/**
		 * Sets the {@link Items#mo} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder mo(String value) {
			this.mo = value;
			return this;
		}

		/**
		 * Sets the {@link Items#multipleOf} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder multipleOf(String value) {
			this.multipleOf = value;
			return this;
		}

		/**
		 * Sets the {@link Items#p} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder p(String value) {
			this.p = value;
			return this;
		}

		/**
		 * Sets the {@link Items#pattern} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder pattern(String value) {
			this.pattern = value;
			return this;
		}

		/**
		 * Sets the {@link Items#t} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder t(String value) {
			this.t = value;
			return this;
		}

		/**
		 * Sets the {@link Items#type} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder type(String value) {
			this.type = value;
			return this;
		}

		/**
		 * Sets the {@link Items#ui} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder ui(boolean value) {
			this.ui = value;
			return this;
		}

		/**
		 * Sets the {@link Items#uniqueItems} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder uniqueItems(boolean value) {
			this.uniqueItems = value;
			return this;
		}

		/**
		 * Sets the {@link Items#value} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder value(String...value) {
			this.value = value;
			return this;
		}

		// <FluentSetters>
		// </FluentSetters>
	}

	private static class Impl extends AnnotationImpl implements Items {

		private final boolean emax, emin, exclusiveMaximum, exclusiveMinimum, ui, uniqueItems;
		private final long maxi, maxItems, maxl, maxLength, mini, minItems, minl, minLength;
		private final String $ref, cf, collectionFormat, f, format, max, maximum, min, minimum, mo, multipleOf, p, pattern, t, type;
		private final String[] _default, _enum, df, e, value;
		private final SubItems items;

		Impl(Builder b) {
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
			this.items = b.items;
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

		@Override /* Items */
		public String[] _default() {
			return _default;
		}

		@Override /* Items */
		public String[] _enum() {
			return _enum;
		}

		@Override /* Items */
		public String $ref() {
			return $ref;
		}

		@Override /* Items */
		public String cf() {
			return cf;
		}

		@Override /* Items */
		public String collectionFormat() {
			return collectionFormat;
		}

		@Override /* Items */
		public String[] df() {
			return df;
		}

		@Override /* Items */
		public String[] e() {
			return e;
		}

		@Override /* Items */
		public boolean emax() {
			return emax;
		}

		@Override /* Items */
		public boolean emin() {
			return emin;
		}

		@Override /* Items */
		public boolean exclusiveMaximum() {
			return exclusiveMaximum;
		}

		@Override /* Items */
		public boolean exclusiveMinimum() {
			return exclusiveMinimum;
		}

		@Override /* Items */
		public String f() {
			return f;
		}

		@Override /* Items */
		public String format() {
			return format;
		}

		@Override /* Items */
		public SubItems items() {
			return items;
		}

		@Override /* Items */
		public String max() {
			return max;
		}

		@Override /* Items */
		public long maxi() {
			return maxi;
		}

		@Override /* Items */
		public String maximum() {
			return maximum;
		}

		@Override /* Items */
		public long maxItems() {
			return maxItems;
		}

		@Override /* Items */
		public long maxl() {
			return maxl;
		}

		@Override /* Items */
		public long maxLength() {
			return maxLength;
		}

		@Override /* Items */
		public String min() {
			return min;
		}

		@Override /* Items */
		public long mini() {
			return mini;
		}

		@Override /* Items */
		public String minimum() {
			return minimum;
		}

		@Override /* Items */
		public long minItems() {
			return minItems;
		}

		@Override /* Items */
		public long minl() {
			return minl;
		}

		@Override /* Items */
		public long minLength() {
			return minLength;
		}

		@Override /* Items */
		public String mo() {
			return mo;
		}

		@Override /* Items */
		public String multipleOf() {
			return multipleOf;
		}

		@Override /* Items */
		public String p() {
			return p;
		}

		@Override /* Items */
		public String pattern() {
			return pattern;
		}

		@Override /* Items */
		public String t() {
			return t;
		}

		@Override /* Items */
		public String type() {
			return type;
		}

		@Override /* Items */
		public boolean ui() {
			return ui;
		}

		@Override /* Items */
		public boolean uniqueItems() {
			return uniqueItems;
		}

		@Override /* Items */
		public String[] value() {
			return value;
		}
	}
}