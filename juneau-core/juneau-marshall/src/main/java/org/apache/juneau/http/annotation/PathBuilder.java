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
 * Builder class for the {@link Path} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class PathBuilder extends TargetedAnnotationTMFBuilder {

	/** Default value */
	public static final Path DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static PathBuilder create() {
		return new PathBuilder();
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static PathBuilder create(Class<?>...on) {
		return create().on(on);
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static PathBuilder create(String...on) {
		return create().on(on);
	}

	private static class Impl extends TargetedAnnotationTImpl implements Path {

		private final boolean aev, allowEmptyValue, emax, emin, exclusiveMaximum, exclusiveMinimum, ui, uniqueItems;
		private final boolean r, required;
		private final Class<? extends HttpPartParser> parser;
		private final Class<? extends HttpPartSerializer> serializer;
		private final Items items;
		private final long maxi, maxItems, maxl, maxLength, mini, minItems, minl, minLength;
		private final String cf, collectionFormat, f, format, max, maximum, min, minimum, mo, multipleOf, n, name, p, pattern, t, type, value;
		private final String[] _enum, api, d, description, e, ex, example;

		Impl(PathBuilder b) {
			super(b);
			this._enum = copyOf(b._enum);
			this.aev = b.aev;
			this.allowEmptyValue = b.allowEmptyValue;
			this.api = copyOf(b.api);
			this.cf = b.cf;
			this.collectionFormat = b.collectionFormat;
			this.d = copyOf(b.d);
			this.description = copyOf(b.description);
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
			this.multipleOf = b.multipleOf;
			this.n = b.n;
			this.name = b.name;
			this.p = b.p;
			this.parser = b.parser;
			this.pattern = b.pattern;
			this.r = b.r;
			this.required = b.required;
			this.serializer = b.serializer;
			this.t = b.t;
			this.type = b.type;
			this.ui = b.ui;
			this.uniqueItems = b.uniqueItems;
			this.value = b.value;
			postConstruct();
		}

		@Override /* Path */
		public String[] _enum() {
			return _enum;
		}

		@Override /* Path */
		public boolean aev() {
			return aev;
		}

		@Override /* Path */
		public boolean allowEmptyValue() {
			return allowEmptyValue;
		}

		@Override /* Path */
		public String[] api() {
			return api;
		}

		@Override /* Path */
		public String cf() {
			return cf;
		}

		@Override /* Path */
		public String collectionFormat() {
			return collectionFormat;
		}

		@Override /* Path */
		public String[] d() {
			return d;
		}

		@Override /* Path */
		public String[] description() {
			return description;
		}

		@Override /* Path */
		public String[] e() {
			return e;
		}

		@Override /* Path */
		public boolean emax() {
			return emax;
		}

		@Override /* Path */
		public boolean emin() {
			return emin;
		}

		@Override /* Path */
		public String[] ex() {
			return ex;
		}

		@Override /* Path */
		public String[] example() {
			return example;
		}

		@Override /* Path */
		public boolean exclusiveMaximum() {
			return exclusiveMaximum;
		}

		@Override /* Path */
		public boolean exclusiveMinimum() {
			return exclusiveMinimum;
		}

		@Override /* Path */
		public String f() {
			return f;
		}

		@Override /* Path */
		public String format() {
			return format;
		}

		@Override /* Path */
		public Items items() {
			return items;
		}

		@Override /* Path */
		public String max() {
			return max;
		}

		@Override /* Path */
		public long maxi() {
			return maxi;
		}

		@Override /* Path */
		public String maximum() {
			return maximum;
		}

		@Override /* Path */
		public long maxItems() {
			return maxItems;
		}

		@Override /* Path */
		public long maxl() {
			return maxl;
		}

		@Override /* Path */
		public long maxLength() {
			return maxLength;
		}

		@Override /* Path */
		public String min() {
			return min;
		}

		@Override /* Path */
		public long mini() {
			return mini;
		}

		@Override /* Path */
		public String minimum() {
			return minimum;
		}

		@Override /* Path */
		public long minItems() {
			return minItems;
		}

		@Override /* Path */
		public long minl() {
			return minl;
		}

		@Override /* Path */
		public long minLength() {
			return minLength;
		}

		@Override /* Path */
		public String mo() {
			return mo;
		}

		@Override /* Path */
		public String multipleOf() {
			return multipleOf;
		}

		@Override /* Path */
		public String n() {
			return n;
		}

		@Override /* Path */
		public String name() {
			return name;
		}

		@Override /* Path */
		public String p() {
			return p;
		}

		@Override /* Path */
		public Class<? extends HttpPartParser> parser() {
			return parser;
		}

		@Override /* Path */
		public String pattern() {
			return pattern;
		}

		@Override /* Path */
		public boolean r() {
			return r;
		}

		@Override /* Path */
		public boolean required() {
			return required;
		}

		@Override /* Path */ /* Path */
		public Class<? extends HttpPartSerializer> serializer() {
			return serializer;
		}

		@Override /* Path */
		public String t() {
			return t;
		}

		@Override /* Path */
		public String type() {
			return type;
		}

		@Override /* Path */
		public boolean ui() {
			return ui;
		}

		@Override /* Path */
		public boolean uniqueItems() {
			return uniqueItems;
		}

		@Override /* Path */
		public String value() {
			return value;
		}
	}


	boolean allowEmptyValue, aev,exclusiveMaximum, emax, exclusiveMinimum, emin, r=true, required=true, uniqueItems, ui;
	Class<? extends HttpPartParser> parser = HttpPartParser.Null.class;
	Class<? extends HttpPartSerializer> serializer = HttpPartSerializer.Null.class;
	Items items = ItemsBuilder.DEFAULT;
	long maxItems=-1, maxLength=-1, maxi=-1, maxl=-1, minItems=-1, minLength=-1, mini=-1, minl=-1;
	String cf="", collectionFormat="", f="", format="", max="", maximum="", min="", minimum="", mo="", multipleOf="", n="", name="", p="", pattern="", t="", type="", value="";
	String[] _enum={}, api={}, d={}, description={}, e={}, ex={}, example={};

	/**
	 * Constructor.
	 */
	public PathBuilder() {
		super(Path.class);
	}

	/**
	 * Instantiates a new {@link Path @Path} object initialized with this builder.
	 *
	 * @return A new {@link Path @Path} object.
	 */
	public Path build() {
		return new Impl(this);
	}

	/**
	 * Sets the {@link Path#_enum} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder _enum( String...value) {
		this._enum = value;
		return this;
	}

	/**
	 * Sets the {@link Path#aev} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder aev(boolean value) {
		this.aev = value;
		return this;
	}

	/**
	 * Sets the {@link Path#allowEmptyValue} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder allowEmptyValue(boolean value) {
		this.allowEmptyValue = value;
		return this;
	}

	/**
	 * Sets the {@link Path#api} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder api(String...value) {
		this.api = value;
		return this;
	}

	/**
	 * Sets the {@link Path#cf} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder cf(String value) {
		this.cf = value;
		return this;
	}

	/**
	 * Sets the {@link Path#collectionFormat} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder collectionFormat(String value) {
		this.collectionFormat = value;
		return this;
	}

	/**
	 * Sets the {@link Path#d} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder d(String...value) {
		this.d = value;
		return this;
	}

	/**
	 * Sets the {@link Path#description} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder description(String...value) {
		this.description = value;
		return this;
	}

	/**
	 * Sets the {@link Path#e} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder e( String...value) {
		this.e = value;
		return this;
	}

	/**
	 * Sets the {@link Path#emax} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder emax(boolean value) {
		this.emax = value;
		return this;
	}

	/**
	 * Sets the {@link Path#emin} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder emin(boolean value) {
		this.emin = value;
		return this;
	}

	/**
	 * Sets the {@link Path#ex} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder ex(String...value) {
		this.ex = value;
		return this;
	}

	/**
	 * Sets the {@link Path#example} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder example(String...value) {
		this.example = value;
		return this;
	}

	/**
	 * Sets the {@link Path#exclusiveMaximum} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder exclusiveMaximum(boolean value) {
		this.exclusiveMaximum = value;
		return this;
	}

	/**
	 * Sets the {@link Path#exclusiveMinimum} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder exclusiveMinimum(boolean value) {
		this.exclusiveMinimum = value;
		return this;
	}

	/**
	 * Sets the {@link Path#f} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder f(String value) {
		this.f = value;
		return this;
	}

	/**
	 * Sets the {@link Path#format} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder format(String value) {
		this.format = value;
		return this;
	}

	/**
	 * Sets the {@link Path#items} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder items(Items value) {
		this.items = value;
		return this;
	}

	/**
	 * Sets the {@link Path#max} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder max(String value) {
		this.max = value;
		return this;
	}

	/**
	 * Sets the {@link Path#maxi} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder maxi(long value) {
		this.maxi = value;
		return this;
	}

	/**
	 * Sets the {@link Path#maximum} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder maximum(String value) {
		this.maximum = value;
		return this;
	}

	/**
	 * Sets the {@link Path#maxItems} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder maxItems(long value) {
		this.maxItems = value;
		return this;
	}

	/**
	 * Sets the {@link Path#maxl} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder maxl(long value) {
		this.maxl = value;
		return this;
	}

	/**
	 * Sets the {@link Path#maxLength} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder maxLength(long value) {
		this.maxLength = value;
		return this;
	}

	/**
	 * Sets the {@link Path#min} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder min(String value) {
		this.min = value;
		return this;
	}

	/**
	 * Sets the {@link Path#mini} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder mini(long value) {
		this.mini = value;
		return this;
	}

	/**
	 * Sets the {@link Path#minimum} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder minimum(String value) {
		this.minimum = value;
		return this;
	}

	/**
	 * Sets the {@link Path#minItems} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder minItems(long value) {
		this.minItems = value;
		return this;
	}

	/**
	 * Sets the {@link Path#minl} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder minl(long value) {
		this.minl = value;
		return this;
	}

	/**
	 * Sets the {@link Path#minLength} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder minLength(long value) {
		this.minLength = value;
		return this;
	}

	/**
	 * Sets the {@link Path#mo} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder mo(String value) {
		this.mo = value;
		return this;
	}

	/**
	 * Sets the {@link Path#multipleOf} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder multipleOf(String value) {
		this.multipleOf = value;
		return this;
	}

	/**
	 * Sets the {@link Path#n} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder n(String value) {
		this.n = value;
		return this;
	}

	/**
	 * Sets the {@link Path#name} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder name(String value) {
		this.name = value;
		return this;
	}

	/**
	 * Sets the {@link Path#p} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder p(String value) {
		this.p = value;
		return this;
	}

	/**
	 * Sets the {@link Path#parser} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder parser(Class<? extends HttpPartParser> value) {
		this.parser = value;
		return this;
	}

	/**
	 * Sets the {@link Path#pattern} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder pattern(String value) {
		this.pattern = value;
		return this;
	}

	/**
	 * Sets the {@link Path#r} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder r(boolean value) {
		this.r = value;
		return this;
	}

	/**
	 * Sets the {@link Path#required} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder required(boolean value) {
		this.required = value;
		return this;
	}

	/**
	 * Sets the {@link Path#serializer} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder serializer(Class<? extends HttpPartSerializer> value) {
		this.serializer = value;
		return this;
	}

	/**
	 * Sets the {@link Path#t} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder t(String value) {
		this.t = value;
		return this;
	}

	/**
	 * Sets the {@link Path#type} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder type(String value) {
		this.type = value;
		return this;
	}

	/**
	 * Sets the {@link Path#ui} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder ui(boolean value) {
		this.ui = value;
		return this;
	}

	/**
	 * Sets the {@link Path#uniqueItems} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder uniqueItems(boolean value) {
		this.uniqueItems = value;
		return this;
	}

	/**
	 * Sets the {@link Path#value} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public PathBuilder value(String value) {
		this.value = value;
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - TargetedAnnotationBuilder */
	public PathBuilder on(String...values) {
		super.on(values);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTBuilder */
	public PathBuilder on(java.lang.Class<?>...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTBuilder */
	public PathBuilder onClass(java.lang.Class<?>...value) {
		super.onClass(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTMFBuilder */
	public PathBuilder on(Field...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTMFBuilder */
	public PathBuilder on(Method...value) {
		super.on(value);
		return this;
	}

	// </FluentSetters>
}
