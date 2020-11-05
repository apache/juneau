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
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.jsonschema.annotation.*;

/**
 * Builder class for the {@link ResponseHeader} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class ResponseHeaderBuilder extends TargetedAnnotationTMBuilder {

	/** Default value */
	public static final ResponseHeader DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static ResponseHeaderBuilder create() {
		return new ResponseHeaderBuilder();
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static ResponseHeaderBuilder create(Class<?>...on) {
		return create().on(on);
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static ResponseHeaderBuilder create(String...on) {
		return create().on(on);
	}

	private static class Impl extends TargetedAnnotationTImpl implements ResponseHeader {

		private final boolean emax, emin, exclusiveMaximum, exclusiveMinimum, ui, uniqueItems;
		private final Class<? extends HttpPartSerializer> serializer;
		private final int[] code;
		private final Items items;
		private final long maxi, maxItems, maxl, maxLength, mini, minItems, minl, minLength;
		private final String $ref, cf, collectionFormat, f, format, max, maximum, min, minimum, mo, multipleOf, n, name, p, pattern, t, type, value;
		private final String[] _default, _enum, api, d, description, df, e, ex, example;

		Impl(ResponseHeaderBuilder b) {
			super(b);
			this.$ref = b.$ref;
			this._default = copyOf(b._default);
			this._enum = copyOf(b._enum);
			this.api = copyOf(b.api);
			this.cf = b.cf;
			this.code = Arrays.copyOf(b.code, b.code.length);
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
			this.multipleOf = b.multipleOf;
			this.n = b.n;
			this.name = b.name;
			this.p = b.p;
			this.pattern = b.pattern;
			this.serializer = b.serializer;
			this.t = b.t;
			this.type = b.type;
			this.ui = b.ui;
			this.uniqueItems = b.uniqueItems;
			this.value = b.value;
			postConstruct();
		}

		@Override /* ResponseHeader */
		public String[] _default() {
			return _default;
		}

		@Override /* ResponseHeader */
		public String[] _enum() {
			return _enum;
		}

		@Override /* ResponseHeader */
		public String $ref() {
			return $ref;
		}

		@Override /* ResponseHeader */
		public String[] api() {
			return api;
		}

		@Override /* ResponseHeader */
		public String cf() {
			return cf;
		}

		@Override /* ResponseHeader */
		public int[] code() {
			return code;
		}

		@Override /* ResponseHeader */
		public String collectionFormat() {
			return collectionFormat;
		}

		@Override /* ResponseHeader */
		public String[] d() {
			return d;
		}

		@Override /* ResponseHeader */
		public String[] description() {
			return description;
		}

		@Override /* ResponseHeader */
		public String[] df() {
			return df;
		}

		@Override /* ResponseHeader */
		public String[] e() {
			return e;
		}

		@Override /* ResponseHeader */
		public boolean emax() {
			return emax;
		}

		@Override /* ResponseHeader */
		public boolean emin() {
			return emin;
		}

		@Override /* ResponseHeader */
		public String[] ex() {
			return ex;
		}

		@Override /* ResponseHeader */
		public String[] example() {
			return example;
		}

		@Override /* ResponseHeader */
		public boolean exclusiveMaximum() {
			return exclusiveMaximum;
		}

		@Override /* ResponseHeader */
		public boolean exclusiveMinimum() {
			return exclusiveMinimum;
		}

		@Override /* ResponseHeader */
		public String f() {
			return f;
		}

		@Override /* ResponseHeader */
		public String format() {
			return format;
		}

		@Override /* ResponseHeader */
		public Items items() {
			return items;
		}

		@Override /* ResponseHeader */
		public String max() {
			return max;
		}

		@Override /* ResponseHeader */
		public long maxi() {
			return maxi;
		}

		@Override /* ResponseHeader */
		public String maximum() {
			return maximum;
		}

		@Override /* ResponseHeader */
		public long maxItems() {
			return maxItems;
		}

		@Override /* ResponseHeader */
		public long maxl() {
			return maxl;
		}

		@Override /* ResponseHeader */
		public long maxLength() {
			return maxLength;
		}

		@Override /* ResponseHeader */
		public String min() {
			return min;
		}

		@Override /* ResponseHeader */
		public long mini() {
			return mini;
		}

		@Override /* ResponseHeader */
		public String minimum() {
			return minimum;
		}

		@Override /* ResponseHeader */
		public long minItems() {
			return minItems;
		}

		@Override /* ResponseHeader */
		public long minl() {
			return minl;
		}

		@Override /* ResponseHeader */
		public long minLength() {
			return minLength;
		}

		@Override /* ResponseHeader */
		public String mo() {
			return mo;
		}

		@Override /* ResponseHeader */
		public String multipleOf() {
			return multipleOf;
		}

		@Override /* ResponseHeader */
		public String n() {
			return n;
		}

		@Override /* ResponseHeader */
		public String name() {
			return name;
		}

		@Override /* ResponseHeader */
		public String p() {
			return p;
		}

		@Override /* ResponseHeader */
		public String pattern() {
			return pattern;
		}

		@Override /* ResponseHeader */
		public Class<? extends HttpPartSerializer> serializer() {
			return serializer;
		}

		@Override /* ResponseHeader */
		public String t() {
			return t;
		}

		@Override /* ResponseHeader */
		public String type() {
			return type;
		}

		@Override /* ResponseHeader */
		public boolean ui() {
			return ui;
		}

		@Override /* ResponseHeader */
		public boolean uniqueItems() {
			return uniqueItems;
		}

		@Override /* ResponseHeader */
		public String value() {
			return value;
		}
	}


	boolean emax, emin, exclusiveMaximum, exclusiveMinimum, ui, uniqueItems;
	Class<? extends HttpPartSerializer> serializer = HttpPartSerializer.Null.class;
	int[] code={};
	Items items = ItemsBuilder.DEFAULT;
	long maxItems=-1, maxLength=-1, maxi=-1, maxl=-1, minItems=-1, minLength=-1, mini=-1, minl=-1;
	String $ref="", cf="", collectionFormat="", f="", format="", max="", maximum="", min="", minimum="", mo="", multipleOf="", n="", name="", p="", pattern="", t="", type="", value="";
	String[] _default={}, _enum={}, api={}, d={}, description={}, df={}, e={}, ex={}, example={};

	/**
	 * Constructor.
	 */
	public ResponseHeaderBuilder() {
		super(ResponseHeader.class);
	}

	/**
	 * Instantiates a new {@link ResponseHeader @ResponseHeader} object initialized with this builder.
	 *
	 * @return A new {@link ResponseHeader @ResponseHeader} object.
	 */
	public ResponseHeader build() {
		return new Impl(this);
	}

	/**
	 * Sets the {@link ResponseHeader#_default} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder _default(String...value) {
		this._default = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#_enum} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder _enum(String...value) {
		this._enum = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#$ref} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder $ref(String value) {
		this.$ref = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#api} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder api(String...value) {
		this.api = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#cf} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder cf(String value) {
		this.cf = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#code} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder code(int...value) {
		this.code = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#collectionFormat} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder collectionFormat(String value) {
		this.collectionFormat = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#d} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder d(String...value) {
		this.d = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#description} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder description(String...value) {
		this.description = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#df} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder df(String...value) {
		this.df = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#e} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder e(String...value) {
		this.e = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#emax} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder emax(boolean value) {
		this.emax = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#emin} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder emin(boolean value) {
		this.emin = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#ex} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder ex(String...value) {
		this.ex = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#example} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder example(String...value) {
		this.example = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#exclusiveMaximum} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder exclusiveMaximum(boolean value) {
		this.exclusiveMaximum = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#exclusiveMinimum} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder exclusiveMinimum(boolean value) {
		this.exclusiveMinimum = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#f} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder f(String value) {
		this.f = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#format} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder format(String value) {
		this.format = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#items} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder items(Items value) {
		this.items = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#max} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder max(String value) {
		this.max = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#maxi} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder maxi(long value) {
		this.maxi = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#maximum} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder maximum(String value) {
		this.maximum = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#maxItems} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder maxItems(long value) {
		this.maxItems = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#maxl} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder maxl(long value) {
		this.maxl = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#maxLength} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder maxLength(long value) {
		this.maxLength = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#min} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder min(String value) {
		this.min = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#mini} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder mini(long value) {
		this.mini = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#minimum} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder minimum(String value) {
		this.minimum = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#minItems} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder minItems(long value) {
		this.minItems = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#minl} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder minl(long value) {
		this.minl = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#minLength} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder minLength(long value) {
		this.minLength = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#mo} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder mo(String value) {
		this.mo = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#multipleOf} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder multipleOf(String value) {
		this.multipleOf = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#n} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder n(String value) {
		this.n = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#name} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder name(String value) {
		this.name = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#p} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder p(String value) {
		this.p = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#pattern} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder pattern(String value) {
		this.pattern = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#serializer} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder serializer(Class<? extends HttpPartSerializer> value) {
		this.serializer = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#t} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder t(String value) {
		this.t = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#type} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder type(String value) {
		this.type = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#ui} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder ui(boolean value) {
		this.ui = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#uniqueItems} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder uniqueItems(boolean value) {
		this.uniqueItems = value;
		return this;
	}

	/**
	 * Sets the {@link ResponseHeader#value} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ResponseHeaderBuilder value(String value) {
		this.value = value;
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - TargetedAnnotationBuilder */
	public ResponseHeaderBuilder on(String...values) {
		super.on(values);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTBuilder */
	public ResponseHeaderBuilder on(java.lang.Class<?>...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTBuilder */
	public ResponseHeaderBuilder onClass(java.lang.Class<?>...value) {
		super.onClass(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTMBuilder */
	public ResponseHeaderBuilder on(Method...value) {
		super.on(value);
		return this;
	}

	// </FluentSetters>
}
