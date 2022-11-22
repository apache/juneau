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
package org.apache.juneau.annotation;

import static org.apache.juneau.internal.ArrayUtils.*;
import static org.apache.juneau.jsonschema.SchemaUtils.*;

import java.lang.annotation.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;

/**
 * Utility classes and methods for the {@link SubItems @SubItems} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class SubItemsAnnotation {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/** Default value */
	public static final SubItems DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(org.apache.juneau.annotation.SubItems a) {
		return a == null || DEFAULT.equals(a);
	}

	/**
	 * Merges the contents of the specified annotation into the specified generic map.
	 *
	 * @param om The map to copy the contents to.
	 * @param a The annotation to apply.
	 * @return The same map with the annotation contents applied.
	 * @throws ParseException Invalid JSON found in value.
	 */
	public static JsonMap merge(JsonMap om, SubItems a) throws ParseException {
		if (SubItemsAnnotation.empty(a))
			return om;
		Predicate<String> ne = StringUtils::isNotEmpty;
		Predicate<Collection<?>> nec = CollectionUtils::isNotEmpty;
		Predicate<Map<?,?>> nem = CollectionUtils::isNotEmpty;
		Predicate<Boolean> nf = ObjectUtils::isTrue;
		Predicate<Long> nm1 = ObjectUtils::isNotMinusOne;
		return om
			.appendFirst(ne, "collectionFormat", a.collectionFormat(), a.cf())
			.appendIf(ne, "default", joinnl(a._default(), a.df()))
			.appendFirst(nec, "enum", parseSet(a._enum()), parseSet(a.e()))
			.appendIf(nf, "exclusiveMaximum", a.exclusiveMaximum() || a.emax())
			.appendIf(nf, "exclusiveMinimum", a.exclusiveMinimum() || a.emin())
			.appendFirst(ne, "format", a.format(), a.f())
			.appendIf(nem, "items", parseMap(a.items()))
			.appendFirst(ne, "maximum", a.maximum(), a.max())
			.appendFirst(nm1, "maxItems", a.maxItems(), a.maxi())
			.appendFirst(nm1, "maxLength", a.maxLength(), a.maxl())
			.appendFirst(ne, "minimum", a.minimum(), a.min())
			.appendFirst(nm1, "minItems", a.minItems(), a.mini())
			.appendFirst(nm1, "minLength", a.minLength(), a.minl())
			.appendFirst(ne, "multipleOf", a.multipleOf(), a.mo())
			.appendFirst(ne, "pattern", a.pattern(), a.p())
			.appendFirst(ne, "type", a.type(), a.t())
			.appendIf(nf, "uniqueItems", a.uniqueItems() || a.ui())
			.appendIf(ne, "$ref", a.$ref())
		;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends AnnotationBuilder {

		String $ref="", cf="", collectionFormat="", f="", format="", max="", maximum="", min="", minimum="", mo="", multipleOf="", p="", pattern="", t="", type="";
		long maxItems=-1, maxLength=-1, maxi=-1, maxl=-1, minItems=-1, minLength=-1, mini=-1, minl=-1;
		boolean emax, emin, exclusiveMaximum, exclusiveMinimum, ui, uniqueItems;
		String[] _default={}, _enum={}, df={}, e={}, items={};

		/**
		 * Constructor.
		 */
		protected Builder() {
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
		 * @return This object.
		 */
		public Builder _default(String...value) {
			this._default = value;
			return this;
		}

		/**
		 * Sets the <c>_enum</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder _enum(String...value) {
			this._enum = value;
			return this;
		}

		/**
		 * Sets the <c>$ref</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder $ref(String value) {
			this.$ref = value;
			return this;
		}

		/**
		 * Sets the <c>cf</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder cf(String value) {
			this.cf = value;
			return this;
		}

		/**
		 * Sets the <c>collectionFormat</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder collectionFormat(String value) {
			this.collectionFormat = value;
			return this;
		}

		/**
		 * Sets the <c>df</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder df(String...value) {
			this.df = value;
			return this;
		}

		/**
		 * Sets the <c>e</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder e(String...value) {
			this.e = value;
			return this;
		}

		/**
		 * Sets the <c>emax</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder emax(boolean value) {
			this.emax = value;
			return this;
		}

		/**
		 * Sets the <c>emin</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder emin(boolean value) {
			this.emin = value;
			return this;
		}

		/**
		 * Sets the <c>exclusiveMaximum</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder exclusiveMaximum(boolean value) {
			this.exclusiveMaximum = value;
			return this;
		}

		/**
		 * Sets the <c>exclusiveMinimum</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder exclusiveMinimum(boolean value) {
			this.exclusiveMinimum = value;
			return this;
		}

		/**
		 * Sets the <c>f</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder f(String value) {
			this.f = value;
			return this;
		}

		/**
		 * Sets the <c>format</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder format(String value) {
			this.format = value;
			return this;
		}

		/**
		 * Sets the <c>items</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder items(String...value) {
			this.items = value;
			return this;
		}

		/**
		 * Sets the <c>max</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder max(String value) {
			this.max = value;
			return this;
		}

		/**
		 * Sets the <c>maxi</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maxi(long value) {
			this.maxi = value;
			return this;
		}

		/**
		 * Sets the <c>maximum</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maximum(String value) {
			this.maximum = value;
			return this;
		}

		/**
		 * Sets the <c>maxItems</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maxItems(long value) {
			this.maxItems = value;
			return this;
		}

		/**
		 * Sets the <c>maxl</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maxl(long value) {
			this.maxl = value;
			return this;
		}

		/**
		 * Sets the <c>maxLength</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maxLength(long value) {
			this.maxLength = value;
			return this;
		}

		/**
		 * Sets the <c>min</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder min(String value) {
			this.min = value;
			return this;
		}

		/**
		 * Sets the <c>mini</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder mini(long value) {
			this.mini = value;
			return this;
		}

		/**
		 * Sets the <c>minimum</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder minimum(String value) {
			this.minimum = value;
			return this;
		}

		/**
		 * Sets the <c>minItems</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder minItems(long value) {
			this.minItems = value;
			return this;
		}

		/**
		 * Sets the <c>minl</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder minl(long value) {
			this.minl = value;
			return this;
		}

		/**
		 * Sets the <c>minLength</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder minLength(long value) {
			this.minLength = value;
			return this;
		}

		/**
		 * Sets the <c>mo</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder mo(String value) {
			this.mo = value;
			return this;
		}

		/**
		 * Sets the <c>multipleOf</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder multipleOf(String value) {
			this.multipleOf = value;
			return this;
		}

		/**
		 * Sets the <c>p</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder p(String value) {
			this.p = value;
			return this;
		}

		/**
		 * Sets the <c>pattern</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder pattern(String value) {
			this.pattern = value;
			return this;
		}

		/**
		 * Sets the <c>t</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder t(String value) {
			this.t = value;
			return this;
		}

		/**
		 * Sets the <c>type</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder type(String value) {
			this.type = value;
			return this;
		}

		/**
		 * Sets the <c>ui</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder ui(boolean value) {
			this.ui = value;
			return this;
		}

		/**
		 * Sets the <c>uniqueItems</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder uniqueItems(boolean value) {
			this.uniqueItems = value;
			return this;
		}

		// <FluentSetters>
		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Implementation
	//-----------------------------------------------------------------------------------------------------------------

	private static class Impl extends AnnotationImpl implements SubItems {

		private final boolean emax, emin, exclusiveMaximum, exclusiveMinimum, ui, uniqueItems;
		private final long maxi, maxItems, maxl, maxLength, mini, minItems, minl, minLength;
		private final String $ref, cf, collectionFormat, f, format, max, maximum, min, minimum, mo, multipleOf, p, pattern, t, type;
		private final String[] _default, _enum, df, e, items;

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
	}
}