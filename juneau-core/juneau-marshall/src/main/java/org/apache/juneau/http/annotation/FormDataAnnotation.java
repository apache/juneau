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

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.apache.juneau.BeanContext.*;
import static org.apache.juneau.internal.ArrayUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link FormData @FormData} annotation.
 */
public class FormDataAnnotation {

	/** Default value */
	public static final FormData DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static Builder create(Class<?>...on) {
		return create().on(on);
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static Builder create(String...on) {
		return create().on(on);
	}

	/**
	 * Creates a copy of the specified annotation.
	 *
	 * @param a The annotation to copy.
	 * @param r The var resolver for resolving any variables.
	 * @return A copy of the specified annotation.
	 */
	public static FormData copy(FormData a, VarResolverSession r) {
		return
			create()
			._default(r.resolve(a._default()))
			._enum(r.resolve(a._enum()))
			.aev(a.aev())
			.allowEmptyValue(a.allowEmptyValue())
			.api(r.resolve(a.api()))
			.cf(r.resolve(a.cf()))
			.collectionFormat(r.resolve(a.collectionFormat()))
			.d(r.resolve(a.d()))
			.description(r.resolve(a.description()))
			.df(r.resolve(a.df()))
			.e(r.resolve(a.e()))
			.emax(a.emax())
			.emin(a.emin())
			.ex(r.resolve(a.ex()))
			.example(r.resolve(a.example()))
			.exclusiveMaximum(a.exclusiveMaximum())
			.exclusiveMinimum(a.exclusiveMinimum())
			.f(r.resolve(a.f()))
			.format(r.resolve(a.format()))
			.items(ItemsAnnotation.copy(a.items(), r))
			.max(r.resolve(a.max()))
			.maxi(a.maxi())
			.maximum(r.resolve(a.maximum()))
			.maxItems(a.maxItems())
			.maxl(a.maxl())
			.maxLength(a.maxLength())
			.min(r.resolve(a.min()))
			.mini(a.mini())
			.minimum(r.resolve(a.minimum()))
			.minItems(a.minItems())
			.minl(a.minl())
			.minLength(a.minLength())
			.mo(r.resolve(a.mo()))
			.multi(a.multi())
			.multipleOf(r.resolve(a.multipleOf()))
			.n(r.resolve(a.n()))
			.name(r.resolve(a.name()))
			.on(r.resolve(a.on()))
			.onClass(a.onClass())
			.p(r.resolve(a.p()))
			.parser(a.parser())
			.pattern(r.resolve(a.pattern()))
			.r(a.r())
			.required(a.required())
			.serializer(a.serializer())
			.sie(a.sie())
			.skipIfEmpty(a.skipIfEmpty())
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
	public static boolean empty(FormData a) {
		return a == null || DEFAULT.equals(a);
	}

	/**
	 * Builder class for the {@link FormData} annotation.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends TargetedAnnotationTMFBuilder {

		boolean aev, allowEmptyValue, emax, emin, exclusiveMaximum, exclusiveMinimum, multi, r, required, sie, skipIfEmpty, ui, uniqueItems;
		Class<? extends HttpPartParser> parser = HttpPartParser.Null.class;
		Class<? extends HttpPartSerializer> serializer = HttpPartSerializer.Null.class;
		Items items = ItemsAnnotation.DEFAULT;
		long maxItems=-1, maxLength=-1, maxi=-1, maxl=-1, minItems=-1, minLength=-1, mini=-1, minl=-1;
		String cf="", collectionFormat="", f="", format="", max="", maximum="", min="", minimum="", mo="", multipleOf="", n="", name="", p="", pattern="", t="", type="", value="";
		String[] _default={}, _enum={}, api={}, d={}, description={}, df={}, e={}, ex={}, example={};

		/**
		 * Constructor.
		 */
		public Builder() {
			super(FormData.class);
		}

		/**
		 * Instantiates a new {@link FormData @FormData} object initialized with this builder.
		 *
		 * @return A new {@link FormData @FormData} object.
		 */
		public FormData build() {
			return new Impl(this);
		}

		/**
		 * Sets the {@link FormData#_default} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder _default(String...value) {
			this._default = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#_enum} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder _enum(String...value) {
			this._enum = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#aev} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder aev(boolean value) {
			this.aev = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#allowEmptyValue} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder allowEmptyValue(boolean value) {
			this.allowEmptyValue = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#api} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder api(String...value) {
			this.api = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#cf} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder cf(String value) {
			this.cf = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#collectionFormat} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder collectionFormat(String value) {
			this.collectionFormat = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#d} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder d(String...value) {
			this.d = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#description} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder description(String...value) {
			this.description = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#df} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder df(String...value) {
			this.df = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#e} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder e(String...value) {
			this.e = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#emax} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder emax(boolean value) {
			this.emax = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#emin} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder emin(boolean value) {
			this.emin = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#ex} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder ex(String...value) {
			this.ex = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#example} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder example(String...value) {
			this.example = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#exclusiveMaximum} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder exclusiveMaximum(boolean value) {
			this.exclusiveMaximum = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#exclusiveMinimum} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder exclusiveMinimum(boolean value) {
			this.exclusiveMinimum = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#f} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder f(String value) {
			this.f = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#format} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder format(String value) {
			this.format = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#items} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder items(Items value) {
			this.items = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#max} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder max(String value) {
			this.max = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#maxi} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder maxi(long value) {
			this.maxi = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#maximum} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder maximum(String value) {
			this.maximum = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#maxItems} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder maxItems(long value) {
			this.maxItems = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#maxl} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder maxl(long value) {
			this.maxl = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#maxLength} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder maxLength(long value) {
			this.maxLength = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#min} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder min(String value) {
			this.min = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#mini} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder mini(long value) {
			this.mini = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#minimum} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder minimum(String value) {
			this.minimum = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#minItems} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder minItems(long value) {
			this.minItems = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#minl} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder minl(long value) {
			this.minl = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#minLength} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder minLength(long value) {
			this.minLength = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#mo} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder mo(String value) {
			this.mo = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#multi} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder multi(boolean value) {
			this.multi = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#multipleOf} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder multipleOf(String value) {
			this.multipleOf = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#n} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder n(String value) {
			this.n = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#name} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder name(String value) {
			this.name = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#p} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder p(String value) {
			this.p = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#parser} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder parser(Class<? extends HttpPartParser> value) {
			this.parser = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#pattern} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder pattern(String value) {
			this.pattern = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#r} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder r(boolean value) {
			this.r = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#required} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder required(boolean value) {
			this.required = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#serializer} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder serializer(Class<? extends HttpPartSerializer> value) {
			this.serializer = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#sie} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder sie(boolean value) {
			this.sie = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#skipIfEmpty} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder skipIfEmpty(boolean value) {
			this.skipIfEmpty = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#t} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder t(String value) {
			this.t = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#type} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder type(String value) {
			this.type = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#ui} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder ui(boolean value) {
			this.ui = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#uniqueItems} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder uniqueItems(boolean value) {
			this.uniqueItems = value;
			return this;
		}

		/**
		 * Sets the {@link FormData#value} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder value(String value) {
			this.value = value;
			return this;
		}

		// <FluentSetters>

		@Override /* GENERATED - TargetedAnnotationBuilder */
		public Builder on(String...values) {
			super.on(values);
			return this;
		}

		@Override /* GENERATED - TargetedAnnotationTBuilder */
		public Builder on(java.lang.Class<?>...value) {
			super.on(value);
			return this;
		}

		@Override /* GENERATED - TargetedAnnotationTBuilder */
		public Builder onClass(java.lang.Class<?>...value) {
			super.onClass(value);
			return this;
		}

		@Override /* GENERATED - TargetedAnnotationTMFBuilder */
		public Builder on(Field...value) {
			super.on(value);
			return this;
		}

		@Override /* GENERATED - TargetedAnnotationTMFBuilder */
		public Builder on(Method...value) {
			super.on(value);
			return this;
		}

		// </FluentSetters>
	}

	private static class Impl extends TargetedAnnotationTImpl implements FormData {

		private final boolean aev, allowEmptyValue, emax, emin, exclusiveMaximum, exclusiveMinimum, multi, r, required, sie, skipIfEmpty, ui, uniqueItems;
		private final Class<? extends HttpPartParser> parser;
		private final Class<? extends HttpPartSerializer> serializer;
		private final Items items;
		private final long maxItems, maxLength, maxi, maxl, minItems, minLength, mini, minl;
		private final String cf, collectionFormat, f, format, max, maximum, min, minimum, mo, multipleOf, n, name, p, pattern, t, type, value;
		private final String[] _default, _enum, api, d, description, df, e, ex, example;

		Impl(Builder b) {
			super(b);
			this._default = copyOf(b._default);
			this._enum = copyOf(b._enum);
			this.aev = b.aev;
			this.allowEmptyValue = b.allowEmptyValue;
			this.api = copyOf(b.api);
			this.cf = b.cf;
			this.collectionFormat = b.collectionFormat;
			this.d = copyOf(b.d);
			this.description = copyOf(b.description);
			this.df = copyOf(b.df);
			this.e = copyOf(b.e);
			this.emax = b.emax;
			this.emin = b.emin;
			this.ex = copyOf(b.ex);
			this.example = copyOf(b.example);
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
			this.multi = b.multi;
			this.multipleOf = b.multipleOf;
			this.n = b.n;
			this.name = b.name;
			this.p = b.p;
			this.parser = b.parser;
			this.pattern = b.pattern;
			this.r = b.r;
			this.required = b.required;
			this.serializer = b.serializer;
			this.sie = b.sie;
			this.skipIfEmpty = b.skipIfEmpty;
			this.t = b.t;
			this.type = b.type;
			this.ui = b.ui;
			this.uniqueItems = b.uniqueItems;
			this.value = b.value;
			postConstruct();
		}

		@Override /* FormData */
		public String[] _default() {
			return _default;
		}

		@Override /* FormData */
		public String[] _enum() {
			return _enum;
		}

		@Override /* FormData */
		public boolean aev() {
			return aev;
		}

		@Override /* FormData */
		public boolean allowEmptyValue() {
			return allowEmptyValue;
		}

		@Override /* FormData */
		public String[] api() {
			return api;
		}

		@Override /* FormData */
		public String cf() {
			return cf;
		}

		@Override /* FormData */
		public String collectionFormat() {
			return collectionFormat;
		}

		@Override /* FormData */
		public String[] d() {
			return d;
		}

		@Override /* FormData */
		public String[] description() {
			return description;
		}

		@Override /* FormData */
		public String[] df() {
			return df;
		}

		@Override /* FormData */
		public String[] e() {
			return e;
		}

		@Override /* FormData */
		public boolean emax() {
			return emax;
		}

		@Override /* FormData */
		public boolean emin() {
			return emin;
		}

		@Override /* FormData */
		public String[] ex() {
			return ex;
		}

		@Override /* FormData */
		public String[] example() {
			return example;
		}

		@Override /* FormData */
		public boolean exclusiveMaximum() {
			return exclusiveMaximum;
		}

		@Override /* FormData */
		public boolean exclusiveMinimum() {
			return exclusiveMinimum;
		}

		@Override /* FormData */
		public String f() {
			return f;
		}

		@Override /* FormData */
		public String format() {
			return format;
		}

		@Override /* FormData */
		public Items items() {
			return items;
		}

		@Override /* FormData */
		public String max() {
			return max;
		}

		@Override /* FormData */
		public long maxi() {
			return maxi;
		}

		@Override /* FormData */
		public String maximum() {
			return maximum;
		}

		@Override /* FormData */
		public long maxItems() {
			return maxItems;
		}

		@Override /* FormData */
		public long maxl() {
			return maxl;
		}

		@Override /* FormData */
		public long maxLength() {
			return maxLength;
		}

		@Override /* FormData */
		public String min() {
			return min;
		}

		@Override /* FormData */
		public long mini() {
			return mini;
		}

		@Override /* FormData */
		public String minimum() {
			return minimum;
		}

		@Override /* FormData */
		public long minItems() {
			return minItems;
		}

		@Override /* FormData */
		public long minl() {
			return minl;
		}

		@Override /* FormData */
		public long minLength() {
			return minLength;
		}

		@Override /* FormData */
		public String mo() {
			return mo;
		}

		@Override /* FormData */
		public boolean multi() {
			return multi;
		}

		@Override /* FormData */
		public String multipleOf() {
			return multipleOf;
		}

		@Override /* FormData */
		public String n() {
			return n;
		}

		@Override /* FormData */
		public String name() {
			return name;
		}

		@Override /* FormData */
		public String p() {
			return p;
		}

		@Override /* FormData */
		public Class<? extends HttpPartParser> parser() {
			return parser;
		}

		@Override /* FormData */
		public String pattern() {
			return pattern;
		}

		@Override /* FormData */
		public boolean r() {
			return r;
		}

		@Override /* FormData */
		public boolean required() {
			return required;
		}

		@Override /* FormData */
		public Class<? extends HttpPartSerializer> serializer() {
			return serializer;
		}

		@Override /* FormData */
		public boolean sie() {
			return sie;
		}

		@Override /* FormData */
		public boolean skipIfEmpty() {
			return skipIfEmpty;
		}

		@Override /* FormData */
		public String t() {
			return t;
		}

		@Override /* FormData */
		public String type() {
			return type;
		}

		@Override /* FormData */
		public boolean ui() {
			return ui;
		}

		@Override /* FormData */
		public boolean uniqueItems() {
			return uniqueItems;
		}

		@Override
		public String value() {
			return value;
		}
	}

	/**
	 * Applies targeted {@link FormData} annotations to a {@link ContextPropertiesBuilder}.
	 */
	public static class Apply extends ConfigApply<FormData> {

		/**
		 * Constructor.
		 *
		 * @param c The annotation class.
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Apply(Class<FormData> c, VarResolverSession vr) {
			super(c, vr);
		}

		@Override
		public void apply(AnnotationInfo<FormData> ai, ContextPropertiesBuilder b) {
			FormData a = ai.getAnnotation();

			if (isEmpty(a.on()) && isEmpty(a.onClass()))
				return;

			b.prependTo(BEAN_annotations, copy(a, vr()));
		}
	}

	/**
	 * A collection of {@link FormData @FormData annotations}.
	 */
	@Documented
	@Target({METHOD,TYPE})
	@Retention(RUNTIME)
	@Inherited
	public static @interface Array {

		/**
		 * The child annotations.
		 */
		FormData[] value();
	}
}