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
 * Utility classes and methods for the {@link Header @Header} annotation.
 */
public class HeaderAnnotation {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/** Default value */
	public static final Header DEFAULT = create().build();

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
	public static Header copy(Header a, VarResolverSession r) {
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
	public static boolean empty(Header a) {
		return a == null || DEFAULT.equals(a);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#annotations(Annotation...)}
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
		protected Builder() {
			super(Header.class);
		}

		/**
		 * Instantiates a new {@link Header @Header} object initialized with this builder.
		 *
		 * @return A new {@link Header @Header} object.
		 */
		public Header build() {
			return new Impl(this);
		}

		/**
		 * Sets the {@link Header#_default} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder _default(String...value) {
			this._default = value;
			return this;
		}


		/**
		 * Sets the {@link Header#_enum} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder _enum(String...value) {
			this._enum = value;
			return this;
		}

		/**
		 * Sets the {@link Header#aev} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder aev(boolean value) {
			this.aev = value;
			return this;
		}

		/**
		 * Sets the {@link Header#allowEmptyValue} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder allowEmptyValue(boolean value) {
			this.allowEmptyValue = value;
			return this;
		}

		/**
		 * Sets the {@link Header#api} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder api(String...value) {
			this.api = value;
			return this;
		}

		/**
		 * Sets the {@link Header#cf} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder cf(String value) {
			this.cf = value;
			return this;
		}

		/**
		 * Sets the {@link Header#collectionFormat} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder collectionFormat(String value) {
			this.collectionFormat = value;
			return this;
		}

		/**
		 * Sets the {@link Header#d} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder d(String...value) {
			this.d = value;
			return this;
		}

		/**
		 * Sets the {@link Header#description} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder description(String...value) {
			this.description = value;
			return this;
		}

		/**
		 * Sets the {@link Header#df} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder df(String...value) {
			this.df = value;
			return this;
		}

		/**
		 * Sets the {@link Header#e} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder e(String...value) {
			this.e = value;
			return this;
		}

		/**
		 * Sets the {@link Header#emax} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder emax(boolean value) {
			this.emax = value;
			return this;
		}

		/**
		 * Sets the {@link Header#emin} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder emin(boolean value) {
			this.emin = value;
			return this;
		}

		/**
		 * Sets the {@link Header#ex} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder ex(String...value) {
			this.ex = value;
			return this;
		}

		/**
		 * Sets the {@link Header#example} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder example(String...value) {
			this.example = value;
			return this;
		}

		/**
		 * Sets the {@link Header#exclusiveMaximum} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder exclusiveMaximum(boolean value) {
			this.exclusiveMaximum = value;
			return this;
		}

		/**
		 * Sets the {@link Header#exclusiveMinimum} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder exclusiveMinimum(boolean value) {
			this.exclusiveMinimum = value;
			return this;
		}

		/**
		 * Sets the {@link Header#f} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder f(String value) {
			this.f = value;
			return this;
		}

		/**
		 * Sets the {@link Header#format} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder format(String value) {
			this.format = value;
			return this;
		}

		/**
		 * Sets the {@link Header#items} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder items(Items value) {
			this.items = value;
			return this;
		}

		/**
		 * Sets the {@link Header#max} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder max(String value) {
			this.max = value;
			return this;
		}

		/**
		 * Sets the {@link Header#maxi} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maxi(long value) {
			this.maxi = value;
			return this;
		}

		/**
		 * Sets the {@link Header#maximum} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maximum(String value) {
			this.maximum = value;
			return this;
		}

		/**
		 * Sets the {@link Header#maxItems} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maxItems(long value) {
			this.maxItems = value;
			return this;
		}

		/**
		 * Sets the {@link Header#maxl} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maxl(long value) {
			this.maxl = value;
			return this;
		}

		/**
		 * Sets the {@link Header#maxLength} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maxLength(long value) {
			this.maxLength = value;
			return this;
		}

		/**
		 * Sets the {@link Header#min} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder min(String value) {
			this.min = value;
			return this;
		}

		/**
		 * Sets the {@link Header#mini} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder mini(long value) {
			this.mini = value;
			return this;
		}

		/**
		 * Sets the {@link Header#minimum} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder minimum(String value) {
			this.minimum = value;
			return this;
		}

		/**
		 * Sets the {@link Header#minItems} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder minItems(long value) {
			this.minItems = value;
			return this;
		}

		/**
		 * Sets the {@link Header#minl} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder minl(long value) {
			this.minl = value;
			return this;
		}

		/**
		 * Sets the {@link Header#minLength} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder minLength(long value) {
			this.minLength = value;
			return this;
		}

		/**
		 * Sets the {@link Header#mo} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder mo(String value) {
			this.mo = value;
			return this;
		}

		/**
		 * Sets the {@link Header#multi} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder multi(boolean value) {
			this.multi = value;
			return this;
		}

		/**
		 * Sets the {@link Header#multipleOf} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder multipleOf(String value) {
			this.multipleOf = value;
			return this;
		}

		/**
		 * Sets the {@link Header#n} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder n(String value) {
			this.n = value;
			return this;
		}

		/**
		 * Sets the {@link Header#name} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder name(String value) {
			this.name = value;
			return this;
		}

		/**
		 * Sets the {@link Header#p} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder p(String value) {
			this.p = value;
			return this;
		}

		/**
		 * Sets the {@link Header#parser} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder parser(Class<? extends HttpPartParser> value) {
			this.parser = value;
			return this;
		}

		/**
		 * Sets the {@link Header#pattern} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder pattern(String value) {
			this.pattern = value;
			return this;
		}

		/**
		 * Sets the {@link Header#value} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder r(boolean value) {
			this.r = value;
			return this;
		}

		/**
		 * Sets the {@link Header#required} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder required(boolean value) {
			this.required = value;
			return this;
		}

		/**
		 * Sets the {@link Header#serializer} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder serializer(Class<? extends HttpPartSerializer> value) {
			this.serializer = value;
			return this;
		}

		/**
		 * Sets the {@link Header#sie} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder sie(boolean value) {
			this.sie = value;
			return this;
		}

		/**
		 * Sets the {@link Header#skipIfEmpty} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder skipIfEmpty(boolean value) {
			this.skipIfEmpty = value;
			return this;
		}

		/**
		 * Sets the {@link Header#t} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder t(String value) {
			this.t = value;
			return this;
		}

		/**
		 * Sets the {@link Header#type} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder type(String value) {
			this.type = value;
			return this;
		}

		/**
		 * Sets the {@link Header#ui} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder ui(boolean value) {
			this.ui = value;
			return this;
		}

		/**
		 * Sets the {@link Header#uniqueItems} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder uniqueItems(boolean value) {
			this.uniqueItems = value;
			return this;
		}

		/**
		 * Sets the {@link Header#value} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
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

	//-----------------------------------------------------------------------------------------------------------------
	// Implementation
	//-----------------------------------------------------------------------------------------------------------------

	private static class Impl extends TargetedAnnotationTImpl implements Header {

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

		@Override /* Header */
		public String[] _default() {
			return _default;
		}

		@Override /* Header */
		public String[] _enum() {
			return _enum;
		}

		@Override /* Header */
		public boolean aev() {
			return aev;
		}

		@Override /* Header */
		public boolean allowEmptyValue() {
			return allowEmptyValue;
		}

		@Override /* Header */
		public String[] api() {
			return api;
		}

		@Override /* Header */
		public String cf() {
			return cf;
		}

		@Override /* Header */
		public String collectionFormat() {
			return collectionFormat;
		}

		@Override /* Header */
		public String[] d() {
			return d;
		}

		@Override /* Header */
		public String[] description() {
			return description;
		}

		@Override /* Header */
		public String[] df() {
			return df;
		}

		@Override /* Header */
		public String[] e() {
			return e;
		}

		@Override /* Header */
		public boolean emax() {
			return emax;
		}

		@Override /* Header */
		public boolean emin() {
			return emin;
		}

		@Override /* Header */
		public String[] ex() {
			return ex;
		}

		@Override /* Header */
		public String[] example() {
			return example;
		}

		@Override /* Header */
		public boolean exclusiveMaximum() {
			return exclusiveMaximum;
		}

		@Override /* Header */
		public boolean exclusiveMinimum() {
			return exclusiveMinimum;
		}

		@Override /* Header */
		public String f() {
			return f;
		}

		@Override /* Header */
		public String format() {
			return format;
		}

		@Override /* Header */
		public Items items() {
			return items;
		}

		@Override /* Header */
		public String max() {
			return max;
		}

		@Override /* Header */
		public long maxi() {
			return maxi;
		}

		@Override /* Header */
		public String maximum() {
			return maximum;
		}

		@Override /* Header */
		public long maxItems() {
			return maxItems;
		}

		@Override /* Header */
		public long maxl() {
			return maxl;
		}

		@Override /* Header */
		public long maxLength() {
			return maxLength;
		}

		@Override /* Header */
		public String min() {
			return min;
		}

		@Override /* Header */
		public long mini() {
			return mini;
		}

		@Override /* Header */
		public String minimum() {
			return minimum;
		}

		@Override /* Header */
		public long minItems() {
			return minItems;
		}

		@Override /* Header */
		public long minl() {
			return minl;
		}

		@Override /* Header */
		public long minLength() {
			return minLength;
		}

		@Override /* Header */
		public String mo() {
			return mo;
		}

		@Override /* Header */
		public boolean multi() {
			return multi;
		}

		@Override /* Header */
		public String multipleOf() {
			return multipleOf;
		}

		@Override /* Header */
		public String n() {
			return n;
		}

		@Override /* Header */
		public String name() {
			return name;
		}

		@Override /* Header */
		public String p() {
			return p;
		}

		@Override /* Header */
		public Class<? extends HttpPartParser> parser() {
			return parser;
		}

		@Override /* Header */
		public String pattern() {
			return pattern;
		}

		@Override /* Header */
		public boolean r() {
			return r;
		}

		@Override /* Header */
		public boolean required() {
			return required;
		}

		@Override /* Header */
		public Class<? extends HttpPartSerializer> serializer() {
			return serializer;
		}

		@Override /* Header */
		public boolean sie() {
			return sie;
		}

		@Override /* Header */
		public boolean skipIfEmpty() {
			return skipIfEmpty;
		}

		@Override /* Header */
		public String t() {
			return t;
		}

		@Override /* Header */
		public String type() {
			return type;
		}

		@Override /* Header */
		public boolean ui() {
			return ui;
		}

		@Override /* Header */
		public boolean uniqueItems() {
			return uniqueItems;
		}

		@Override /* Header */
		public String value() {
			return value;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Appliers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Applies targeted {@link Header} annotations to a {@link org.apache.juneau.BeanContext.Builder}.
	 */
	public static class Applier extends AnnotationApplier<Header,BeanContext.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Applier(VarResolverSession vr) {
			super(Header.class, BeanContext.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<Header> ai, BeanContext.Builder b) {
			Header a = ai.getAnnotation();

			if (isEmpty(a.on()) && isEmpty(a.onClass()))
				return;

			b.annotations(copy(a, vr()));
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * A collection of {@link Header @Header annotations}.
	 */
	@Documented
	@Target({METHOD,TYPE})
	@Retention(RUNTIME)
	@Inherited
	public static @interface Array {

		/**
		 * The child annotations.
		 */
		Header[] value();
	}
}