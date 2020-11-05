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

import static org.apache.juneau.internal.ArrayUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.jsonschema.annotation.*;

/**
 * Builder class for the {@link Query} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class QueryBuilder extends TargetedAnnotationTMFBuilder {

	/** Default value */
	public static final Query DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static QueryBuilder create() {
		return new QueryBuilder();
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static QueryBuilder create(Class<?>...on) {
		return create().on(on);
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static QueryBuilder create(String...on) {
		return create().on(on);
	}

	private static class Impl extends TargetedAnnotationTImpl implements Query {

		private final boolean aev, allowEmptyValue, emax, emin, exclusiveMaximum, exclusiveMinimum, multi, r, required, sie, skipIfEmpty, ui, uniqueItems;
		private final Class<? extends HttpPartParser> parser;
		private final Class<? extends HttpPartSerializer> serializer;
		private final Items items;
		private final long maxi, maxItems, maxl, maxLength, mini, minItems, minl, minLength;
		private final String cf, collectionFormat, f, format, max, maximum, min, minimum, mo, multipleOf, n, name, p, pattern, t, type, value;
		private final String[] _default, _enum, api, d, description, df, e, ex, example;

		Impl(QueryBuilder b) {
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

		@Override /* Query */
		public String[] _default() {
			return _default;
		}

		@Override /* Query */
		public String[] _enum() {
			return _enum;
		}

		@Override /* Query */
		public boolean aev() {
			return aev;
		}

		@Override /* Query */
		public boolean allowEmptyValue() {
			return allowEmptyValue;
		}

		@Override /* Query */
		public String[] api() {
			return api;
		}

		@Override /* Query */
		public String cf() {
			return cf;
		}

		@Override /* Query */
		public String collectionFormat() {
			return collectionFormat;
		}

		@Override /* Query */
		public String[] d() {
			return d;
		}

		@Override /* Query */
		public String[] description() {
			return description;
		}

		@Override /* Query */
		public String[] df() {
			return df;
		}

		@Override /* Query */
		public String[] e() {
			return e;
		}

		@Override /* Query */
		public boolean emax() {
			return emax;
		}

		@Override /* Query */
		public boolean emin() {
			return emin;
		}

		@Override /* Query */
		public String[] ex() {
			return ex;
		}

		@Override /* Query */
		public String[] example() {
			return example;
		}

		@Override /* Query */
		public boolean exclusiveMaximum() {
			return exclusiveMaximum;
		}

		@Override /* Query */
		public boolean exclusiveMinimum() {
			return exclusiveMinimum;
		}

		@Override /* Query */
		public String f() {
			return f;
		}

		@Override /* Query */
		public String format() {
			return format;
		}

		@Override /* Query */
		public Items items() {
			return items;
		}

		@Override /* Query */
		public String max() {
			return max;
		}

		@Override /* Query */
		public long maxi() {
			return maxi;
		}

		@Override /* Query */
		public String maximum() {
			return maximum;
		}

		@Override /* Query */
		public long maxItems() {
			return maxItems;
		}

		@Override /* Query */
		public long maxl() {
			return maxl;
		}

		@Override /* Query */
		public long maxLength() {
			return maxLength;
		}

		@Override /* Query */
		public String min() {
			return min;
		}

		@Override /* Query */
		public long mini() {
			return mini;
		}

		@Override /* Query */
		public String minimum() {
			return minimum;
		}

		@Override /* Query */
		public long minItems() {
			return minItems;
		}

		@Override /* Query */
		public long minl() {
			return minl;
		}

		@Override /* Query */
		public long minLength() {
			return minLength;
		}

		@Override /* Query */
		public String mo() {
			return mo;
		}

		@Override /* Query */
		public boolean multi() {
			return multi;
		}

		@Override /* Query */
		public String multipleOf() {
			return multipleOf;
		}

		@Override /* Query */
		public String n() {
			return n;
		}

		@Override /* Query */
		public String name() {
			return name;
		}

		@Override /* Query */
		public String p() {
			return p;
		}

		@Override /* Query */
		public Class<? extends HttpPartParser> parser() {
			return parser;
		}

		@Override /* Query */
		public String pattern() {
			return pattern;
		}

		@Override /* Query */
		public boolean r() {
			return r;
		}

		@Override /* Query */
		public boolean required() {
			return required;
		}

		@Override /* Query */
		public Class<? extends HttpPartSerializer> serializer() {
			return serializer;
		}

		@Override /* Query */
		public boolean sie() {
			return sie;
		}

		@Override /* Query */
		public boolean skipIfEmpty() {
			return skipIfEmpty;
		}

		@Override /* Query */
		public String t() {
			return t;
		}

		@Override /* Query */
		public String type() {
			return type;
		}

		@Override /* Query */
		public boolean ui() {
			return ui;
		}

		@Override /* Query */
		public boolean uniqueItems() {
			return uniqueItems;
		}

		@Override /* Query */
		public String value() {
			return value;
		}
	}


	boolean aev, allowEmptyValue, emax, emin, exclusiveMaximum, exclusiveMinimum, multi, r, required, sie, skipIfEmpty, ui, uniqueItems;
	Class<? extends HttpPartParser> parser = HttpPartParser.Null.class;
	Class<? extends HttpPartSerializer> serializer = HttpPartSerializer.Null.class;
	Items items = ItemsBuilder.DEFAULT;
	long maxItems=-1, maxLength=-1, maxi=-1, maxl=-1, minItems=-1, minLength=-1, mini=-1, minl=-1;
	String cf="", collectionFormat="", f="", format="", max="", maximum="", min="", minimum="", mo="", multipleOf="", n="", name="", p="", pattern="", t="", type="", value="";
	String[] _default={}, _enum={}, api={}, d={}, description={}, df={}, e={}, ex={}, example={};

	/**
	 * Constructor.
	 */
	public QueryBuilder() {
		super(Query.class);
	}

	/**
	 * Instantiates a new {@link Query @Query} object initialized with this builder.
	 *
	 * @return A new {@link Query @Query} object.
	 */
	public Query build() {
		return new Impl(this);
	}

	/**
	 * Sets the {@link Query#_default} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder _default(String...value) {
		this._default = value;
		return this;
	}

	/**
	 * Sets the {@link Query#_enum} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder _enum(String...value) {
		this._enum = value;
		return this;
	}

	/**
	 * Sets the {@link Query#aev} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder aev(boolean value) {
		this.aev = value;
		return this;
	}

	/**
	 * Sets the {@link Query#allowEmptyValue} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder allowEmptyValue(boolean value) {
		this.allowEmptyValue = value;
		return this;
	}

	/**
	 * Sets the {@link Query#api} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder api(String...value) {
		this.api = value;
		return this;
	}

	/**
	 * Sets the {@link Query#cf} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder cf(String value) {
		this.cf = value;
		return this;
	}

	/**
	 * Sets the {@link Query#collectionFormat} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder collectionFormat(String value) {
		this.collectionFormat = value;
		return this;
	}

	/**
	 * Sets the {@link Query#d} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder d(String...value) {
		this.d = value;
		return this;
	}

	/**
	 * Sets the {@link Query#description} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder description(String...value) {
		this.description = value;
		return this;
	}

	/**
	 * Sets the {@link Query#df} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder df(String...value) {
		this.df = value;
		return this;
	}

	/**
	 * Sets the {@link Query#e} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder e(String...value) {
		this.e = value;
		return this;
	}

	/**
	 * Sets the {@link Query#emax} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder emax(boolean value) {
		this.emax = value;
		return this;
	}

	/**
	 * Sets the {@link Query#emin} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder emin(boolean value) {
		this.emin = value;
		return this;
	}

	/**
	 * Sets the {@link Query#ex} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder ex(String...value) {
		this.ex = value;
		return this;
	}

	/**
	 * Sets the {@link Query#example} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder example(String...value) {
		this.example = value;
		return this;
	}

	/**
	 * Sets the <cexclusiveMaximum>} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder exclusiveMaximum(boolean value) {
		this.exclusiveMaximum = value;
		return this;
	}

	/**
	 * Sets the {@link Query#exclusiveMinimum} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder exclusiveMinimum(boolean value) {
		this.exclusiveMinimum = value;
		return this;
	}

	/**
	 * Sets the {@link Query#f} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder f(String value) {
		this.f = value;
		return this;
	}

	/**
	 * Sets the {@link Query#format} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder format(String value) {
		this.format = value;
		return this;
	}

	/**
	 * Sets the {@link Query#items} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder items(Items value) {
		this.items = value;
		return this;
	}

	/**
	 * Sets the {@link Query#max} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder max(String value) {
		this.max = value;
		return this;
	}

	/**
	 * Sets the {@link Query#maxi} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder maxi(long value) {
		this.maxi = value;
		return this;
	}

	/**
	 * Sets the {@link Query#maximum} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder maximum(String value) {
		this.maximum = value;
		return this;
	}

	/**
	 * Sets the {@link Query#maxItems} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder maxItems(long value) {
		this.maxItems = value;
		return this;
	}

	/**
	 * Sets the {@link Query#maxl} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder maxl(long value) {
		this.maxl = value;
		return this;
	}

	/**
	 * Sets the {@link Query#maxLength} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder maxLength(long value) {
		this.maxLength = value;
		return this;
	}

	/**
	 * Sets the {@link Query#min} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder min(String value) {
		this.min = value;
		return this;
	}

	/**
	 * Sets the {@link Query#mini} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder mini(long value) {
		this.mini = value;
		return this;
	}

	/**
	 * Sets the {@link Query#minimum} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder minimum(String value) {
		this.minimum = value;
		return this;
	}

	/**
	 * Sets the {@link Query#minItems} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder minItems(long value) {
		this.minItems = value;
		return this;
	}

	/**
	 * Sets the {@link Query#minl} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder minl(long value) {
		this.minl = value;
		return this;
	}

	/**
	 * Sets the {@link Query#minLength} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder minLength(long value) {
		this.minLength = value;
		return this;
	}

	/**
	 * Sets the {@link Query#mo} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder mo(String value) {
		this.mo = value;
		return this;
	}

	/**
	 * Sets the {@link Query#multi} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder multi(boolean value) {
		this.multi = value;
		return this;
	}

	/**
	 * Sets the {@link Query#multipleOf} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder multipleOf(String value) {
		this.multipleOf = value;
		return this;
	}

	/**
	 * Sets the {@link Query#n} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder n(String value) {
		this.n = value;
		return this;
	}

	/**
	 * Sets the {@link Query#name} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder name(String value) {
		this.name = value;
		return this;
	}

	/**
	 * Sets the {@link Query#p} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder p(String value) {
		this.p = value;
		return this;
	}

	/**
	 * Sets the {@link Query#parser} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder parser(Class<? extends HttpPartParser> value) {
		this.parser = value;
		return this;
	}

	/**
	 * Sets the {@link Query#pattern} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder pattern(String value) {
		this.pattern = value;
		return this;
	}

	/**
	 * Sets the {@link Query#r} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder r(boolean value) {
		this.r = value;
		return this;
	}

	/**
	 * Sets the {@link Query#required} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder required(boolean value) {
		this.required = value;
		return this;
	}

	/**
	 * Sets the {@link Query#serializer} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder serializer(Class<? extends HttpPartSerializer> value) {
		this.serializer = value;
		return this;
	}

	/**
	 * Sets the <csie>} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder sie(boolean value) {
		this.sie = value;
		return this;
	}

	/**
	 * Sets the {@link Query#skipIfEmpty} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder skipIfEmpty(boolean value) {
		this.skipIfEmpty = value;
		return this;
	}

	/**
	 * Sets the {@link Query#t} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder t(String value) {
		this.t = value;
		return this;
	}

	/**
	 * Sets the {@link Query#type} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder type(String value) {
		this.type = value;
		return this;
	}

	/**
	 * Sets the {@link Query#ui} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder ui(boolean value) {
		this.ui = value;
		return this;
	}

	/**
	 * Sets the {@link Query#uniqueItems} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder uniqueItems(boolean value) {
		this.uniqueItems = value;
		return this;
	}

	/**
	 * Sets the {@link Query#value} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public QueryBuilder value(String value) {
		this.value = value;
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - TargetedAnnotationBuilder */
	public QueryBuilder on(String...values) {
		super.on(values);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTBuilder */
	public QueryBuilder on(java.lang.Class<?>...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTBuilder */
	public QueryBuilder onClass(java.lang.Class<?>...value) {
		super.onClass(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTMFBuilder */
	public QueryBuilder on(Field...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTMFBuilder */
	public QueryBuilder on(Method...value) {
		super.on(value);
		return this;
	}

	// </FluentSetters>
}
