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

import java.lang.annotation.*;

import org.apache.juneau.commons.annotation.*;

/**
 * Utility classes and methods for the {@link Items @Items} annotation.
 *
 */
public class ItemsAnnotation {

	/**
	 * Prevents instantiation.
	 */
	private ItemsAnnotation() {}

	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends AnnotationObject.Builder {

		private String[] description = {};
		private boolean emax, emin, exclusiveMaximum, exclusiveMinimum, ui, uniqueItems;
		private long maxItems = -1, maxLength = -1, maxi = -1, maxl = -1, minItems = -1, minLength = -1, mini = -1, minl = -1;
		private String $ref = "", cf = "", collectionFormat = "", f = "", format = "", max = "", maximum = "", min = "", minimum = "", mo = "", multipleOf = "", p = "", pattern = "", t = "",
			type = "";
		private String[] default_ = {}, enum_ = {}, df = {}, e = {};
		private SubItems items = SubItemsAnnotation.DEFAULT;

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(Items.class);
		}

		/**
		 * Sets the {@link Items#default_} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
	public Builder default_(String...value) {
		default_ = value;
		return this;
	}

	/**
	 * Sets the {@link Items#enum_} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder enum_(String...value) {
		enum_ = value;
		return this;
	}

	/**
	 * Sets the {@link Items#$ref} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder $ref(String value) {
		$ref = value;
		return this;
	}

	/**
	 * Instantiates a new {@link Items @Items} object initialized with this builder.
	 *
	 * @return A new {@link Items @Items} object.
	 */
	public Items build() {
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
	 * Sets the {@link Items#cf} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder cf(String value) {
		cf = value;
		return this;
	}

	/**
	 * Sets the {@link Items#collectionFormat} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder collectionFormat(String value) {
		collectionFormat = value;
		return this;
	}

	/**
	 * Sets the {@link Items#df} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder df(String...value) {
		df = value;
		return this;
	}

	/**
	 * Sets the {@link Items#e} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder e(String...value) {
		e = value;
		return this;
	}

	/**
	 * Sets the {@link Items#emax} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder emax(boolean value) {
		emax = value;
		return this;
	}

	/**
	 * Sets the {@link Items#emin} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder emin(boolean value) {
		emin = value;
		return this;
	}

	/**
	 * Sets the {@link Items#exclusiveMaximum} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder exclusiveMaximum(boolean value) {
		exclusiveMaximum = value;
		return this;
	}

	/**
	 * Sets the {@link Items#exclusiveMinimum} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder exclusiveMinimum(boolean value) {
		exclusiveMinimum = value;
		return this;
	}

	/**
	 * Sets the {@link Items#f} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder f(String value) {
		f = value;
		return this;
	}

	/**
	 * Sets the {@link Items#format} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder format(String value) {
		format = value;
		return this;
	}

	/**
	 * Sets the {@link Items#items} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder items(SubItems value) {
		items = value;
		return this;
	}

	/**
	 * Sets the {@link Items#max} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder max(String value) {
		max = value;
		return this;
	}

	/**
	 * Sets the {@link Items#maxi} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder maxi(long value) {
		maxi = value;
		return this;
	}

	/**
	 * Sets the {@link Items#maximum} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder maximum(String value) {
		maximum = value;
		return this;
	}

	/**
	 * Sets the {@link Items#maxItems} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder maxItems(long value) {
		maxItems = value;
		return this;
	}

	/**
	 * Sets the {@link Items#maxl} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder maxl(long value) {
		maxl = value;
		return this;
	}

	/**
	 * Sets the {@link Items#maxLength} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder maxLength(long value) {
		maxLength = value;
		return this;
	}

	/**
	 * Sets the {@link Items#min} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder min(String value) {
		min = value;
		return this;
	}

	/**
	 * Sets the {@link Items#mini} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder mini(long value) {
		mini = value;
		return this;
	}

	/**
	 * Sets the {@link Items#minimum} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder minimum(String value) {
		minimum = value;
		return this;
	}

	/**
	 * Sets the {@link Items#minItems} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder minItems(long value) {
		minItems = value;
		return this;
	}

	/**
	 * Sets the {@link Items#minl} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder minl(long value) {
		minl = value;
		return this;
	}

	/**
	 * Sets the {@link Items#minLength} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder minLength(long value) {
		minLength = value;
		return this;
	}

	/**
	 * Sets the {@link Items#mo} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder mo(String value) {
		mo = value;
		return this;
	}

	/**
	 * Sets the {@link Items#multipleOf} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder multipleOf(String value) {
		multipleOf = value;
		return this;
	}

	/**
	 * Sets the {@link Items#p} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder p(String value) {
		p = value;
		return this;
	}

	/**
	 * Sets the {@link Items#pattern} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder pattern(String value) {
		pattern = value;
		return this;
	}

	/**
	 * Sets the {@link Items#t} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder t(String value) {
		t = value;
		return this;
	}

	/**
	 * Sets the {@link Items#type} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder type(String value) {
		type = value;
		return this;
	}

	/**
	 * Sets the {@link Items#ui} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder ui(boolean value) {
		ui = value;
		return this;
	}

	/**
	 * Sets the {@link Items#uniqueItems} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Builder uniqueItems(boolean value) {
		uniqueItems = value;
		return this;
	}

	}

	private static class Object extends AnnotationObject implements Items {

		private final String[] description;
		private final boolean emax, emin, exclusiveMaximum, exclusiveMinimum, ui, uniqueItems;
		private final long maxi, maxItems, maxl, maxLength, mini, minItems, minl, minLength;
		private final String $ref, cf, collectionFormat, f, format, max, maximum, min, minimum, mo, multipleOf, p, pattern, t, type;
		private final String[] default_, enum_, df, e;
		private final SubItems items;

		Object(ItemsAnnotation.Builder b) {
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
			items = b.items;
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

		@Override /* Overridden from Items */
		public String[] default_() {
			return default_;
		}

		@Override /* Overridden from Items */
		public String[] enum_() {
			return enum_;
		}

		@Override /* Overridden from Items */
		public String $ref() {
			return $ref;
		}

		@Override /* Overridden from Items */
		public String cf() {
			return cf;
		}

		@Override /* Overridden from Items */
		public String collectionFormat() {
			return collectionFormat;
		}

		@Override /* Overridden from Items */
		public String[] df() {
			return df;
		}

		@Override /* Overridden from Items */
		public String[] e() {
			return e;
		}

		@Override /* Overridden from Items */
		public boolean emax() {
			return emax;
		}

		@Override /* Overridden from Items */
		public boolean emin() {
			return emin;
		}

		@Override /* Overridden from Items */
		public boolean exclusiveMaximum() {
			return exclusiveMaximum;
		}

		@Override /* Overridden from Items */
		public boolean exclusiveMinimum() {
			return exclusiveMinimum;
		}

		@Override /* Overridden from Items */
		public String f() {
			return f;
		}

		@Override /* Overridden from Items */
		public String format() {
			return format;
		}

		@Override /* Overridden from Items */
		public SubItems items() {
			return items;
		}

		@Override /* Overridden from Items */
		public String max() {
			return max;
		}

		@Override /* Overridden from Items */
		public long maxi() {
			return maxi;
		}

		@Override /* Overridden from Items */
		public String maximum() {
			return maximum;
		}

		@Override /* Overridden from Items */
		public long maxItems() {
			return maxItems;
		}

		@Override /* Overridden from Items */
		public long maxl() {
			return maxl;
		}

		@Override /* Overridden from Items */
		public long maxLength() {
			return maxLength;
		}

		@Override /* Overridden from Items */
		public String min() {
			return min;
		}

		@Override /* Overridden from Items */
		public long mini() {
			return mini;
		}

		@Override /* Overridden from Items */
		public String minimum() {
			return minimum;
		}

		@Override /* Overridden from Items */
		public long minItems() {
			return minItems;
		}

		@Override /* Overridden from Items */
		public long minl() {
			return minl;
		}

		@Override /* Overridden from Items */
		public long minLength() {
			return minLength;
		}

		@Override /* Overridden from Items */
		public String mo() {
			return mo;
		}

		@Override /* Overridden from Items */
		public String multipleOf() {
			return multipleOf;
		}

		@Override /* Overridden from Items */
		public String p() {
			return p;
		}

		@Override /* Overridden from Items */
		public String pattern() {
			return pattern;
		}

		@Override /* Overridden from Items */
		public String t() {
			return t;
		}

		@Override /* Overridden from Items */
		public String type() {
			return type;
		}

		@Override /* Overridden from Items */
		public boolean ui() {
			return ui;
		}

		@Override /* Overridden from Items */
		public boolean uniqueItems() {
			return uniqueItems;
		}

		@Override /* Overridden from annotation */
		public String[] description() {
			return description;
		}
	}

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
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(org.apache.juneau.annotation.Items a) {
		return a == null || DEFAULT.equals(a);
	}
}