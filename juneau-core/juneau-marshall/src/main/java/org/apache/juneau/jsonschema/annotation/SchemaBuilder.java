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
import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;

/**
 * Builder class for the {@link Schema} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class SchemaBuilder extends TargetedAnnotationTMFBuilder {

	/** Default value */
	public static final Schema DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static SchemaBuilder create() {
		return new SchemaBuilder();
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static SchemaBuilder create(Class<?>...on) {
		return create().on(on);
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static SchemaBuilder create(String...on) {
		return create().on(on);
	}

	private static class Impl extends TargetedAnnotationTImpl implements Schema {

		private final boolean exclusiveMaximum, emax, exclusiveMinimum, emin, uniqueItems, ui, required, r, readOnly, ro, ignore;
		private final ExternalDocs externalDocs;
		private final Items items;
		private final long maxLength, maxl, minLength, minl, maxItems, maxi, minItems, mini, maxProperties, maxp, minProperties, minp;
		private final String $ref, format, f, title, multipleOf, mo, maximum, max, minimum, min, pattern, p, type, t, collectionFormat, cf, discriminator;
		private final String[] description, d, _default, df, _enum, e, allOf, properties, additionalProperties, xml, example, ex, examples, exs, value;

		Impl(SchemaBuilder b) {
			super(b);
			this.$ref = b.$ref;
			this._default = copyOf(b._default);
			this._enum = copyOf(b._enum);
			this.additionalProperties = copyOf(b.additionalProperties);
			this.allOf = copyOf(b.allOf);
			this.cf = b.cf;
			this.collectionFormat = b.collectionFormat;
			this.d = copyOf(b.d);
			this.description = copyOf(b.description);
			this.df = copyOf(b.df);
			this.discriminator = b.discriminator;
			this.e = copyOf(b.e);
			this.emax = b.emax;
			this.emin = b.emin;
			this.ex = copyOf(b.ex);
			this.example = copyOf(b.example);
			this.examples = copyOf(b.examples);
			this.exclusiveMaximum = b.exclusiveMaximum;
			this.exclusiveMinimum = b.exclusiveMinimum;
			this.exs = copyOf(b.exs);
			this.externalDocs = b.externalDocs;
			this.f = b.f;
			this.format = b.format;
			this.ignore = b.ignore;
			this.items = b.items;
			this.max = b.max;
			this.maxi = b.maxi;
			this.maximum = b.maximum;
			this.maxItems = b.maxItems;
			this.maxl = b.maxl;
			this.maxLength = b.maxLength;
			this.maxp = b.maxp;
			this.maxProperties = b.maxProperties;
			this.min = b.min;
			this.mini = b.mini;
			this.minimum = b.minimum;
			this.minItems = b.minItems;
			this.minl = b.minl;
			this.minLength = b.minLength;
			this.minp = b.minp;
			this.minProperties = b.minProperties;
			this.mo = b.mo;
			this.multipleOf = b.multipleOf;
			this.p = b.p;
			this.pattern = b.pattern;
			this.properties = copyOf(b.properties);
			this.r = b.r;
			this.readOnly = b.readOnly;
			this.required = b.required;
			this.ro = b.ro;
			this.t = b.t;
			this.title = b.title;
			this.type = b.type;
			this.ui = b.ui;
			this.uniqueItems = b.uniqueItems;
			this.value = copyOf(b.value);
			this.xml = copyOf(b.xml);
			postConstruct();
		}

		@Override /* Schema */
		public String[] _default() {
			return _default;
		}

		@Override /* Schema */
		public String[] _enum() {
			return _enum;
		}

		@Override /* Schema */
		public String $ref() {
			return $ref;
		}

		@Override /* Schema */
		public String[] additionalProperties() {
			return additionalProperties;
		}

		@Override /* Schema */
		public String[] allOf() {
			return allOf;
		}

		@Override /* Schema */
		public String cf() {
			return cf;
		}

		@Override /* Schema */
		public String collectionFormat() {
			return collectionFormat;
		}

		@Override /* Schema */
		public String[] d() {
			return d;
		}

		@Override /* Schema */
		public String[] description() {
			return description;
		}

		@Override /* Schema */
		public String[] df() {
			return df;
		}

		@Override /* Schema */
		public String discriminator() {
			return discriminator;
		}

		@Override /* Schema */
		public String[] e() {
			return e;
		}

		@Override /* Schema */
		public boolean emax() {
			return emax;
		}

		@Override /* Schema */
		public boolean emin() {
			return emin;
		}

		@Override /* Schema */
		public String[] ex() {
			return ex;
		}

		@Override /* Schema */
		public String[] example() {
			return example;
		}

		@Override /* Schema */
		public String[] examples() {
			return examples;
		}

		@Override /* Schema */
		public boolean exclusiveMaximum() {
			return exclusiveMaximum;
		}

		@Override /* Schema */
		public boolean exclusiveMinimum() {
			return exclusiveMinimum;
		}

		@Override /* Schema */
		public String[] exs() {
			return exs;
		}

		@Override /* Schema */
		public ExternalDocs externalDocs() {
			return externalDocs;
		}

		@Override /* Schema */
		public String f() {
			return f;
		}

		@Override /* Schema */
		public String format() {
			return format;
		}

		@Override /* Schema */
		public boolean ignore() {
			return ignore;
		}

		@Override /* Schema */
		public Items items() {
			return items;
		}

		@Override /* Schema */
		public String max() {
			return max;
		}

		@Override /* Schema */
		public long maxi() {
			return maxi;
		}

		@Override /* Schema */
		public String maximum() {
			return maximum;
		}

		@Override /* Schema */
		public long maxItems() {
			return maxItems;
		}

		@Override /* Schema */
		public long maxl() {
			return maxl;
		}

		@Override /* Schema */
		public long maxLength() {
			return maxLength;
		}

		@Override /* Schema */
		public long maxp() {
			return maxp;
		}

		@Override /* Schema */
		public long maxProperties() {
			return maxProperties;
		}

		@Override /* Schema */
		public String min() {
			return min;
		}

		@Override /* Schema */
		public long mini() {
			return mini;
		}

		@Override /* Schema */
		public String minimum() {
			return minimum;
		}

		@Override /* Schema */
		public long minItems() {
			return minItems;
		}

		@Override /* Schema */
		public long minl() {
			return minl;
		}

		@Override /* Schema */
		public long minLength() {
			return minLength;
		}

		@Override /* Schema */
		public long minp() {
			return minp;
		}

		@Override /* Schema */
		public long minProperties() {
			return minProperties;
		}

		@Override /* Schema */
		public String mo() {
			return mo;
		}

		@Override /* Schema */
		public String multipleOf() {
			return multipleOf;
		}

		@Override /* Schema */
		public String p() {
			return p;
		}

		@Override /* Schema */
		public String pattern() {
			return pattern;
		}

		@Override /* Schema */
		public String[] properties() {
			return properties;
		}

		@Override /* Schema */
		public boolean r() {
			return r;
		}

		@Override /* Schema */
		public boolean readOnly() {
			return readOnly;
		}

		@Override /* Schema */
		public boolean required() {
			return required;
		}

		@Override /* Schema */
		public boolean ro() {
			return ro;
		}

		@Override /* Schema */
		public String t() {
			return t;
		}

		@Override /* Schema */
		public String title() {
			return title;
		}

		@Override /* Schema */
		public String type() {
			return type;
		}

		@Override /* Schema */
		public boolean ui() {
			return ui;
		}

		@Override /* Schema */
		public boolean uniqueItems() {
			return uniqueItems;
		}

		@Override /* Schema */
		public String[] value() {
			return value;
		}

		@Override /* Schema */
		public String[] xml() {
			return xml;
		}
	}


	boolean emax, emin, exclusiveMaximum, exclusiveMinimum, ignore, r, readOnly, required, ro, ui, uniqueItems;
	ExternalDocs externalDocs=ExternalDocsBuilder.DEFAULT;
	Items items=ItemsBuilder.DEFAULT;
	long maxi=-1, maxItems=-1, maxl=-1, maxLength=-1, maxp=-1, maxProperties=-1, mini=-1, minItems=-1, minl=-1, minLength=-1, minp=-1, minProperties=-1;
	String $ref="", cf="", collectionFormat="", discriminator="", f="", format="", max="", maximum="", min="", minimum="", mo="", multipleOf="", p="", pattern="", t="", title="", type="";
	String[] _default={}, _enum={}, additionalProperties={}, allOf={}, d={}, description={}, df={}, e={}, ex={}, example={}, examples={}, exs={}, properties={}, value={}, xml={};

	/**
	 * Constructor.
	 */
	public SchemaBuilder() {
		super(Schema.class);
	}

	/**
	 * Instantiates a new {@link Schema @Schema} object initialized with this builder.
	 *
	 * @return A new {@link Schema @Schema} object.
	 */
	public Schema build() {
		return new Impl(this);
	}

	/**
	 * Sets the {@link Schema#_default} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder _default(String...value) {
		this._default = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#_enum} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder _enum(String...value) {
		this._enum = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#$ref} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder $ref(String value) {
		this.$ref = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#additionalProperties} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder additionalProperties(String...value) {
		this.additionalProperties = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#allOf} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder allOf(String...value) {
		this.allOf = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#cf} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder cf(String value) {
		this.cf = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#collectionFormat} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder collectionFormat(String value) {
		this.collectionFormat = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#d} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder d(String...value) {
		this.d = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#description} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder description(String...value) {
		this.description = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#df} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder df(String...value) {
		this.df = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#discriminator} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder discriminator(String value) {
		this.discriminator = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#e} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder e(String...value) {
		this.e = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#emax} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder emax(boolean value) {
		this.emax = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#emin} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder emin(boolean value) {
		this.emin = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#ex} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder ex(String...value) {
		this.ex = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#example} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder example(String...value) {
		this.example = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#examples} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder examples(String...value) {
		this.examples = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#exclusiveMaximum} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder exclusiveMaximum(boolean value) {
		this.exclusiveMaximum = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#exclusiveMinimum} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder exclusiveMinimum(boolean value) {
		this.exclusiveMinimum = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#exs} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder exs(String...value) {
		this.exs = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#externalDocs} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder externalDocs(ExternalDocs value) {
		this.externalDocs = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#f} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder f(String value) {
		this.f = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#format} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder format(String value) {
		this.format = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#ignore} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder ignore(boolean value) {
		this.ignore = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#items} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder items(Items value) {
		this.items = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#max} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder max(String value) {
		this.max = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#maxi} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder maxi(long value) {
		this.maxi = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#maximum} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder maximum(String value) {
		this.maximum = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#maxItems} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder maxItems(long value) {
		this.maxItems = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#maxl} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder maxl(long value) {
		this.maxl = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#maxLength} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder maxLength(long value) {
		this.maxLength = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#maxp} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder maxp(long value) {
		this.maxp = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#maxProperties} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder maxProperties(long value) {
		this.maxProperties = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#min} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder min(String value) {
		this.min = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#mini} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder mini(long value) {
		this.mini = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#minimum} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder minimum(String value) {
		this.minimum = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#minItems} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder minItems(long value) {
		this.minItems = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#minl} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder minl(long value) {
		this.minl = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#minLength} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder minLength(long value) {
		this.minLength = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#minp} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder minp(long value) {
		this.minp = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#minProperties} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder minProperties(long value) {
		this.minProperties = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#mo} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder mo(String value) {
		this.mo = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#multipleOf} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder multipleOf(String value) {
		this.multipleOf = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#p} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder p(String value) {
		this.p = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#pattern} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder pattern(String value) {
		this.pattern = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#properties} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder properties(String...value) {
		this.properties = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#r} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder r(boolean value) {
		this.r = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#readOnly} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder readOnly(boolean value) {
		this.readOnly = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#required} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder required(boolean value) {
		this.required = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#ro} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder ro(boolean value) {
		this.ro = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#t} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder t(String value) {
		this.t = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#title} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder title(String value) {
		this.title = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#type} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder type(String value) {
		this.type = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#ui} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder ui(boolean value) {
		this.ui = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#uniqueItems} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder uniqueItems(boolean value) {
		this.uniqueItems = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#value} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder value(String...value) {
		this.value = value;
		return this;
	}

	/**
	 * Sets the {@link Schema#xml} property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaBuilder xml(String...value) {
		this.xml = value;
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - TargetedAnnotationBuilder */
	public SchemaBuilder on(String...values) {
		super.on(values);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTBuilder */
	public SchemaBuilder on(java.lang.Class<?>...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTBuilder */
	public SchemaBuilder onClass(java.lang.Class<?>...value) {
		super.onClass(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTMFBuilder */
	public SchemaBuilder on(Field...value) {
		super.on(value);
		return this;
	}

	@Override /* GENERATED - TargetedAnnotationTMFBuilder */
	public SchemaBuilder on(Method...value) {
		super.on(value);
		return this;
	}

	// </FluentSetters>
}
