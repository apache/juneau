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
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link ResponseHeader @ResponseHeader} annotation.
 */
public class ResponseHeaderAnnotation {

	/** Default value */
	public static final ResponseHeader DEFAULT = create().build();

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
	public static ResponseHeader copy(ResponseHeader a, VarResolverSession r) {
		return
			create()
			._default(r.resolve(a._default()))
			._enum(r.resolve(a._enum()))
			.$ref(r.resolve(a.$ref()))
			.api(r.resolve(a.api()))
			.cf(r.resolve(a.cf()))
			.code(a.code())
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
			.multipleOf(r.resolve(a.multipleOf()))
			.n(r.resolve(a.n()))
			.name(r.resolve(a.name()))
			.on(r.resolve(a.on()))
			.onClass(a.onClass())
			.p(r.resolve(a.p()))
			.pattern(r.resolve(a.pattern()))
			.serializer(a.serializer())
			.t(r.resolve(a.t()))
			.type(r.resolve(a.type()))
			.ui(a.ui())
			.uniqueItems(a.uniqueItems())
			.value(r.resolve(a.value()))
			.build();
	}

	/**
	 * Creates a copy of the specified annotation.
	 *
	 * @param a The annotation to copy.
	 * @param r The var resolver for resolving any variables.
	 * @return A copy of the specified annotation.
	 */
	public static ResponseHeader[] copy(ResponseHeader[] a, VarResolverSession r) {
		ResponseHeader[] b = new ResponseHeader[a.length];
		for (int i = 0; i < a.length; i++)
			b[i] = copy(a[i], r);
		return b;
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(ResponseHeader a) {
		return a == null || DEFAULT.equals(a);
	}

	/**
	 * Builder class for the {@link ResponseHeader} annotation.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends TargetedAnnotationTMBuilder {

		boolean emax, emin, exclusiveMaximum, exclusiveMinimum, ui, uniqueItems;
		Class<? extends HttpPartSerializer> serializer = HttpPartSerializer.Null.class;
		int[] code={};
		Items items = ItemsAnnotation.DEFAULT;
		long maxItems=-1, maxLength=-1, maxi=-1, maxl=-1, minItems=-1, minLength=-1, mini=-1, minl=-1;
		String $ref="", cf="", collectionFormat="", f="", format="", max="", maximum="", min="", minimum="", mo="", multipleOf="", n="", name="", p="", pattern="", t="", type="", value="";
		String[] _default={}, _enum={}, api={}, d={}, description={}, df={}, e={}, ex={}, example={};

		/**
		 * Constructor.
		 */
		public Builder() {
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
		public Builder _default(String...value) {
			this._default = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#_enum} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder _enum(String...value) {
			this._enum = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#$ref} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder $ref(String value) {
			this.$ref = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#api} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder api(String...value) {
			this.api = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#cf} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder cf(String value) {
			this.cf = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#code} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder code(int...value) {
			this.code = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#collectionFormat} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder collectionFormat(String value) {
			this.collectionFormat = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#d} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder d(String...value) {
			this.d = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#description} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder description(String...value) {
			this.description = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#df} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder df(String...value) {
			this.df = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#e} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder e(String...value) {
			this.e = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#emax} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder emax(boolean value) {
			this.emax = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#emin} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder emin(boolean value) {
			this.emin = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#ex} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder ex(String...value) {
			this.ex = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#example} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder example(String...value) {
			this.example = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#exclusiveMaximum} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder exclusiveMaximum(boolean value) {
			this.exclusiveMaximum = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#exclusiveMinimum} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder exclusiveMinimum(boolean value) {
			this.exclusiveMinimum = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#f} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder f(String value) {
			this.f = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#format} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder format(String value) {
			this.format = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#items} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder items(Items value) {
			this.items = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#max} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder max(String value) {
			this.max = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#maxi} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder maxi(long value) {
			this.maxi = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#maximum} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder maximum(String value) {
			this.maximum = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#maxItems} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder maxItems(long value) {
			this.maxItems = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#maxl} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder maxl(long value) {
			this.maxl = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#maxLength} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder maxLength(long value) {
			this.maxLength = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#min} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder min(String value) {
			this.min = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#mini} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder mini(long value) {
			this.mini = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#minimum} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder minimum(String value) {
			this.minimum = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#minItems} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder minItems(long value) {
			this.minItems = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#minl} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder minl(long value) {
			this.minl = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#minLength} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder minLength(long value) {
			this.minLength = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#mo} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder mo(String value) {
			this.mo = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#multipleOf} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder multipleOf(String value) {
			this.multipleOf = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#n} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder n(String value) {
			this.n = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#name} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder name(String value) {
			this.name = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#p} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder p(String value) {
			this.p = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#pattern} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder pattern(String value) {
			this.pattern = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#serializer} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder serializer(Class<? extends HttpPartSerializer> value) {
			this.serializer = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#t} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder t(String value) {
			this.t = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#type} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder type(String value) {
			this.type = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#ui} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder ui(boolean value) {
			this.ui = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#uniqueItems} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder uniqueItems(boolean value) {
			this.uniqueItems = value;
			return this;
		}

		/**
		 * Sets the {@link ResponseHeader#value} property on this annotation.
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

		@Override /* GENERATED - TargetedAnnotationTMBuilder */
		public Builder on(Method...value) {
			super.on(value);
			return this;
		}

		// </FluentSetters>
	}

	private static class Impl extends TargetedAnnotationTImpl implements ResponseHeader {

		private final boolean emax, emin, exclusiveMaximum, exclusiveMinimum, ui, uniqueItems;
		private final Class<? extends HttpPartSerializer> serializer;
		private final int[] code;
		private final Items items;
		private final long maxi, maxItems, maxl, maxLength, mini, minItems, minl, minLength;
		private final String $ref, cf, collectionFormat, f, format, max, maximum, min, minimum, mo, multipleOf, n, name, p, pattern, t, type, value;
		private final String[] _default, _enum, api, d, description, df, e, ex, example;

		Impl(Builder b) {
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

	/**
	 * Applies targeted {@link ResponseHeader} annotations to a {@link ContextPropertiesBuilder}.
	 */
	public static class Apply extends ConfigApply<ResponseHeader,ContextPropertiesBuilder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Apply(VarResolverSession vr) {
			super(ResponseHeader.class, ContextPropertiesBuilder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<ResponseHeader> ai, ContextPropertiesBuilder b) {
			ResponseHeader a = ai.getAnnotation();

			if (isEmpty(a.on()) && isEmpty(a.onClass()))
				return;

			b.prependTo(BEAN_annotations, copy(a, vr()));
		}
	}

	/**
	 * A collection of {@link ResponseHeader @ResponseHeader annotations}.
	 */
	@Documented
	@Target({METHOD,TYPE})
	@Retention(RUNTIME)
	@Inherited
	public static @interface Array {

		/**
		 * The child annotations.
		 */
		ResponseHeader[] value();
	}
}