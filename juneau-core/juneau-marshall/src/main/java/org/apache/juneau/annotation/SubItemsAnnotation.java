/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.annotation;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.jsonschema.SchemaUtils.*;

import java.lang.annotation.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.commons.annotation.*;
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.parser.*;

/**
 * Utility classes and methods for the {@link SubItems @SubItems} annotation.
 *
 */
public class SubItemsAnnotation {

	// Property name constants
	private static final String PROP_collectionFormat = "collectionFormat";
	private static final String PROP_default = "default";
	private static final String PROP_enum = "enum";
	private static final String PROP_exclusiveMaximum = "exclusiveMaximum";
	private static final String PROP_exclusiveMinimum = "exclusiveMinimum";
	private static final String PROP_format = "format";
	private static final String PROP_items = "items";
	private static final String PROP_maximum = "maximum";
	private static final String PROP_maxItems = "maxItems";
	private static final String PROP_maxLength = "maxLength";
	private static final String PROP_minimum = "minimum";
	private static final String PROP_minItems = "minItems";
	private static final String PROP_minLength = "minLength";
	private static final String PROP_multipleOf = "multipleOf";
	private static final String PROP_pattern = "pattern";
	private static final String PROP_ref = "$ref";
	private static final String PROP_type = "type";
	private static final String PROP_uniqueItems = "uniqueItems";

	/**
	 * Prevents instantiation.
	 */
	private SubItemsAnnotation() {}

	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	@SuppressWarnings("java:S116")
	public static class Builder extends AnnotationObject.Builder {

		private String[] description = {};
		private String $ref = "";
		private String cf = "";
		private String collectionFormat = "";
		private String f = "";
		private String format = "";
		private String max = "";
		private String maximum = "";
		private String min = "";
		private String minimum = "";
		private String mo = "";
		private String multipleOf = "";
		private String p = "";
		private String pattern = "";
		private String t = "";
		private String type = "";
		private long maxItems = -1;
		private long maxLength = -1;
		private long maxi = -1;
		private long maxl = -1;
		private long minItems = -1;
		private long minLength = -1;
		private long mini = -1;
		private long minl = -1;
		private boolean emax;
		private boolean emin;
		private boolean exclusiveMaximum;
		private boolean exclusiveMinimum;
		private boolean ui;
		private boolean uniqueItems;
		private String[] default_ = {};
		private String[] enum_ = {};
		private String[] df = {};
		private String[] e = {};
		private String[] items = {};

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(SubItems.class);
		}

		/**
		 * Sets the <c>default_</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder default_(String...value) {
			default_ = value;
			return this;
		}

		/**
		 * Sets the <c>enum_</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder enum_(String...value) {
			enum_ = value;
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
		 * Instantiates a new {@link SubItems @SubItems} object initialized with this builder.
		 *
		 * @return A new {@link SubItems @SubItems} object.
		 */
		public SubItems build() {
			return new Object(this);
		}

		/**
		 * Sets the description property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder description(String...value) {
			description = value;
			return this;
		}

		/**
		 * Sets the <c>cf</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder cf(String value) {
			cf = value;
			return this;
		}

		/**
		 * Sets the <c>collectionFormat</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder collectionFormat(String value) {
			collectionFormat = value;
			return this;
		}

		/**
		 * Sets the <c>df</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder df(String...value) {
			df = value;
			return this;
		}

		/**
		 * Sets the <c>e</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder e(String...value) {
			e = value;
			return this;
		}

		/**
		 * Sets the <c>emax</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder emax(boolean value) {
			emax = value;
			return this;
		}

		/**
		 * Sets the <c>emin</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder emin(boolean value) {
			emin = value;
			return this;
		}

		/**
		 * Sets the <c>exclusiveMaximum</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder exclusiveMaximum(boolean value) {
			exclusiveMaximum = value;
			return this;
		}

		/**
		 * Sets the <c>exclusiveMinimum</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder exclusiveMinimum(boolean value) {
			exclusiveMinimum = value;
			return this;
		}

		/**
		 * Sets the <c>f</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder f(String value) {
			f = value;
			return this;
		}

		/**
		 * Sets the <c>format</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder format(String value) {
			format = value;
			return this;
		}

		/**
		 * Sets the <c>items</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder items(String...value) {
			items = value;
			return this;
		}

		/**
		 * Sets the <c>max</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder max(String value) {
			max = value;
			return this;
		}

		/**
		 * Sets the <c>maxi</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maxi(long value) {
			maxi = value;
			return this;
		}

		/**
		 * Sets the <c>maximum</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maximum(String value) {
			maximum = value;
			return this;
		}

		/**
		 * Sets the <c>maxItems</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maxItems(long value) {
			maxItems = value;
			return this;
		}

		/**
		 * Sets the <c>maxl</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maxl(long value) {
			maxl = value;
			return this;
		}

		/**
		 * Sets the <c>maxLength</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maxLength(long value) {
			maxLength = value;
			return this;
		}

		/**
		 * Sets the <c>min</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder min(String value) {
			min = value;
			return this;
		}

		/**
		 * Sets the <c>mini</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder mini(long value) {
			mini = value;
			return this;
		}

		/**
		 * Sets the <c>minimum</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder minimum(String value) {
			minimum = value;
			return this;
		}

		/**
		 * Sets the <c>minItems</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder minItems(long value) {
			minItems = value;
			return this;
		}

		/**
		 * Sets the <c>minl</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder minl(long value) {
			minl = value;
			return this;
		}

		/**
		 * Sets the <c>minLength</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder minLength(long value) {
			minLength = value;
			return this;
		}

		/**
		 * Sets the <c>mo</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder mo(String value) {
			mo = value;
			return this;
		}

		/**
		 * Sets the <c>multipleOf</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder multipleOf(String value) {
			multipleOf = value;
			return this;
		}

		/**
		 * Sets the <c>p</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder p(String value) {
			p = value;
			return this;
		}

		/**
		 * Sets the <c>pattern</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder pattern(String value) {
			pattern = value;
			return this;
		}

		/**
		 * Sets the <c>t</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder t(String value) {
			t = value;
			return this;
		}

		/**
		 * Sets the <c>type</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder type(String value) {
			type = value;
			return this;
		}

		/**
		 * Sets the <c>ui</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder ui(boolean value) {
			ui = value;
			return this;
		}

		/**
		 * Sets the <c>uniqueItems</c> property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder uniqueItems(boolean value) {
			uniqueItems = value;
			return this;
		}

	}

	@SuppressWarnings("java:S116")
	private static class Object extends AnnotationObject implements SubItems {

		private final String[] description;
		private final boolean emax;
		private final boolean emin;
		private final boolean exclusiveMaximum;
		private final boolean exclusiveMinimum;
		private final boolean ui;
		private final boolean uniqueItems;
		private final long maxi;
		private final long maxItems;
		private final long maxl;
		private final long maxLength;
		private final long mini;
		private final long minItems;
		private final long minl;
		private final long minLength;
		private final String $ref;
		private final String cf;
		private final String collectionFormat;
		private final String f;
		private final String format;
		private final String max;
		private final String maximum;
		private final String min;
		private final String minimum;
		private final String mo;
		private final String multipleOf;
		private final String p;
		private final String pattern;
		private final String t;
		private final String type;
		private final String[] default_;
		private final String[] enum_;
		private final String[] df;
		private final String[] e;
		private final String[] items;

		Object(SubItemsAnnotation.Builder b) {
			super(b);
			description = copyOf(b.description);
			$ref = b.$ref;
			default_ = copyOf(b.default_);
			enum_ = copyOf(b.enum_);
			cf = b.cf;
			collectionFormat = b.collectionFormat;
			df = copyOf(b.df);
			e = copyOf(b.e);
			emax = b.emax;
			emin = b.emin;
			exclusiveMaximum = b.exclusiveMaximum;
			exclusiveMinimum = b.exclusiveMinimum;
			f = b.f;
			format = b.format;
			items = copyOf(b.items);
			max = b.max;
			maxi = b.maxi;
			maximum = b.maximum;
			maxItems = b.maxItems;
			maxl = b.maxl;
			maxLength = b.maxLength;
			min = b.min;
			mini = b.mini;
			minimum = b.minimum;
			minItems = b.minItems;
			minl = b.minl;
			minLength = b.minLength;
			mo = b.mo;
			multipleOf = b.multipleOf;
			p = b.p;
			pattern = b.pattern;
			t = b.t;
			type = b.type;
			ui = b.ui;
			uniqueItems = b.uniqueItems;
		}

		@Override /* Overridden from SubItems */
		public String[] default_() {
			return default_;
		}

		@Override /* Overridden from SubItems */
		public String[] enum_() {
			return enum_;
		}

		@Override /* Overridden from SubItems */
		public String $ref() {
			return $ref;
		}

		@Override /* Overridden from SubItems */
		public String cf() {
			return cf;
		}

		@Override /* Overridden from SubItems */
		public String collectionFormat() {
			return collectionFormat;
		}

		@Override /* Overridden from SubItems */
		public String[] df() {
			return df;
		}

		@Override /* Overridden from SubItems */
		public String[] e() {
			return e;
		}

		@Override /* Overridden from SubItems */
		public boolean emax() {
			return emax;
		}

		@Override /* Overridden from SubItems */
		public boolean emin() {
			return emin;
		}

		@Override /* Overridden from SubItems */
		public boolean exclusiveMaximum() {
			return exclusiveMaximum;
		}

		@Override /* Overridden from SubItems */
		public boolean exclusiveMinimum() {
			return exclusiveMinimum;
		}

		@Override /* Overridden from SubItems */
		public String f() {
			return f;
		}

		@Override /* Overridden from SubItems */
		public String format() {
			return format;
		}

		@Override /* Overridden from SubItems */
		public String[] items() {
			return items;
		}

		@Override /* Overridden from SubItems */
		public String max() {
			return max;
		}

		@Override /* Overridden from SubItems */
		public long maxi() {
			return maxi;
		}

		@Override /* Overridden from SubItems */
		public String maximum() {
			return maximum;
		}

		@Override /* Overridden from SubItems */
		public long maxItems() {
			return maxItems;
		}

		@Override /* Overridden from SubItems */
		public long maxl() {
			return maxl;
		}

		@Override /* Overridden from SubItems */
		public long maxLength() {
			return maxLength;
		}

		@Override /* Overridden from SubItems */
		public String min() {
			return min;
		}

		@Override /* Overridden from SubItems */
		public long mini() {
			return mini;
		}

		@Override /* Overridden from SubItems */
		public String minimum() {
			return minimum;
		}

		@Override /* Overridden from SubItems */
		public long minItems() {
			return minItems;
		}

		@Override /* Overridden from SubItems */
		public long minl() {
			return minl;
		}

		@Override /* Overridden from SubItems */
		public long minLength() {
			return minLength;
		}

		@Override /* Overridden from SubItems */
		public String mo() {
			return mo;
		}

		@Override /* Overridden from SubItems */
		public String multipleOf() {
			return multipleOf;
		}

		@Override /* Overridden from SubItems */
		public String p() {
			return p;
		}

		@Override /* Overridden from SubItems */
		public String pattern() {
			return pattern;
		}

		@Override /* Overridden from SubItems */
		public String t() {
			return t;
		}

		@Override /* Overridden from SubItems */
		public String type() {
			return type;
		}

		@Override /* Overridden from SubItems */
		public boolean ui() {
			return ui;
		}

		@Override /* Overridden from SubItems */
		public boolean uniqueItems() {
			return uniqueItems;
		}

		@Override /* Overridden from annotation */
		public String[] description() {
			return description;
		}
	}

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
		Predicate<String> ne = Utils::ne;
		Predicate<Collection<?>> nec = Utils::ne;
		Predicate<Map<?,?>> nem = Utils::ne;
		Predicate<Boolean> nf = Utils::isTrue;
		Predicate<Long> nm1 = Utils::nm1;
		// @formatter:off
		return om
			.appendFirst(ne, PROP_collectionFormat, a.collectionFormat(), a.cf())
			.appendIf(ne, PROP_default, joinnl(a.default_(), a.df()))
			.appendFirst(nec, PROP_enum, parseSet(a.enum_()), parseSet(a.e()))
			.appendIf(nf, PROP_exclusiveMaximum, a.exclusiveMaximum() || a.emax())
			.appendIf(nf, PROP_exclusiveMinimum, a.exclusiveMinimum() || a.emin())
			.appendFirst(ne, PROP_format, a.format(), a.f())
			.appendIf(nem, PROP_items, parseMap(a.items()))
			.appendFirst(ne, PROP_maximum, a.maximum(), a.max())
			.appendFirst(nm1, PROP_maxItems, a.maxItems(), a.maxi())
			.appendFirst(nm1, PROP_maxLength, a.maxLength(), a.maxl())
			.appendFirst(ne, PROP_minimum, a.minimum(), a.min())
			.appendFirst(nm1, PROP_minItems, a.minItems(), a.mini())
			.appendFirst(nm1, PROP_minLength, a.minLength(), a.minl())
			.appendFirst(ne, PROP_multipleOf, a.multipleOf(), a.mo())
			.appendFirst(ne, PROP_pattern, a.pattern(), a.p())
			.appendFirst(ne, PROP_type, a.type(), a.t())
			.appendIf(nf, PROP_uniqueItems, a.uniqueItems() || a.ui())
			.appendIf(ne, PROP_ref, a.$ref())
		;
		// @formatter:on
	}
}